# GCP Account Migration — Moving Off the Expiring Free Trial

## Overview

This covers moving the app from a GCP account whose free trial is about to end to a
**brand-new** GCP account, on a new always-free e2-micro VM. It assumes you're already
running the setup from [GCP_MIGRATION.md](GCP_MIGRATION.md) (Nginx + systemd + Cloudflare in
front of an e2-micro VM) and just need to relocate it.

**Good news:** your database is on Neon (not on the GCP VM), so there's no data migration —
you just point the new VM at the same Neon connection string. The whole cutover is: stand up
a twin VM on the new account, verify it works, flip DNS, then decommission the old one.

> Last run successfully 2026-08-29 (old account's trial → new account, same domain, zero
> downtime, ~1 day of Yahoo crawler downtime skipped since it wasn't needed immediately).

> **Before you start — do you actually need a new account?** GCP's "free trial" ($300 credit,
> 90 days) is separate from the **Always Free** tier (1 e2-micro VM/month in
> us-central1/us-east1/us-west1, forever, no credit involved). If your VM has stayed within
> Always Free limits, you can usually just **add a billing account / upgrade out of trial
> mode on your current project** and keep the same VM running with no changes at all —
> Google will ask you to confirm you want to move to a paid account, but an Always Free
> e2-micro keeps costing $0. Do this instead if you're not trying to get a fresh $300 credit
> or leave the current Google account behind entirely. If you *do* want a clean account
> (e.g. different Google login, want another trial credit, or the old account has billing
> issues), continue below.

---

## Part 1 — Create the New GCP Account & Project

1. Sign up at https://cloud.google.com with a **different Google account** than the one on
   the expiring trial (reusing the same account/payment method usually won't grant a new
   trial, and can get flagged).
2. Attach a billing account (required even for free-tier use — you won't be charged as long
   as you stay within Always Free limits, same as before).
3. Create a new **Project** (e.g. `sports-cards-project-2`).

> **You'll likely see the project created under an "Organization"** (e.g. `yourname-org`)
> instead of "No organization" — this is normal. Google auto-provisions a Cloud Identity
> organization for personal Gmail-based Cloud accounts now; it's not a company/school org you
> don't control, and it doesn't add cost or extra restrictions by default. The one thing worth
> a quick sanity check: **IAM & Admin → Organization Policies**, search "external IP" — the
> constraint `compute.vmExternalIpAccess` should not be restrictive (it wasn't, by default, in
> the 2026-08-29 run). If it is restrictive, you'd get an explicit error creating the VM's
> external IP, and you (as the org's implicit owner) can loosen it yourself.

## Part 2 — Stand Up the Twin VM

### 2.1 Create the VM

Compute Engine → VM Instances → Create Instance (enable the Compute Engine API first if
prompted — first use in a fresh project):

- **Name:** `sports-card-app`
- **Region:** `us-central1` / `us-east1` / `us-west1`
- **Machine type:** `e2-micro`
- **Provisioning model:** leave as **Standard** (not Spot — Spot/preemptible VMs can be
  killed anytime, which would randomly take a 24/7 app down)
- **Boot disk** — click "Change":
  - OS: **Debian GNU/Linux 12 (bookworm)**
  - **Disk type: "Standard persistent disk"** — the console's current default is "Balanced
    persistent disk," which is **not** covered by Always Free and will show a nonzero cost.
    You must switch it manually.
  - Size: up to **30 GB** is free
  - Snapshot schedule: if one is auto-attached (e.g. `default-schedule-1`), consider removing
    it — automatic snapshots aren't Always-Free and add a small ongoing cost
- **Firewall:** check both **Allow HTTP traffic** and **Allow HTTPS traffic**
- Click **Create**

> **The cost estimate sidebar during creation will show a nonzero number (e.g. "$6.11/mo")
> even with a fully correct e2-micro/Standard-disk/free-region config.** This is a known UI
> quirk — the estimator shows list price and doesn't apply the Always Free discount, which is
> only reflected in actual billing. As long as machine type is `e2-micro`, region is one of
> the three free regions, and disk is Standard ≤30GB, the real bill is $0.

Create the `allow-http` firewall rule manually — checking the box during VM creation doesn't
reliably create it:

VPC Network → Firewall → Create Firewall Rule:
- Name: `allow-http`, Direction: Ingress, Action: Allow
- Targets: All instances in the network
- Source IPv4 ranges: `0.0.0.0/0`
- Protocols and ports: TCP, port `80`

### 2.2 Install Java, Git, Maven

SSH in (Console → SSH button), then:

```bash
sudo apt update && sudo apt upgrade -y
sudo apt install -y openjdk-17-jdk git maven
java -version
```

No swap file needed — the old VM runs fine on e2-micro's 1GB RAM with 0B swap.

### 2.3 `/etc/sportscard/env`

```bash
sudo mkdir -p /etc/sportscard
sudo nano /etc/sportscard/env
```

**Copy the values straight from the old VM's `/etc/sportscard/env`**
(`sudo cat /etc/sportscard/env` on the old VM) for everything except the OAuth2 vars:
`DB_URL`, `DB_USERNAME`, `DB_PASSWORD` (same Neon DB), `GMAIL_USERNAME`, `GMAIL_APP_PASSWORD`,
`YOUTUBE_API_KEY`, `GEMINI_API_KEY`, `EBAY_CLIENT_ID`, `EBAY_CLIENT_SECRET` — none of these
need to change, since they're not tied to the GCP account.

`GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` need **new** values — see 2.5 below. Add them to
this same file once generated.

Then lock it down:
```bash
sudo chmod 600 /etc/sportscard/env
sudo chown root:root /etc/sportscard/env
```

> Diff `/etc/sportscard/env` against the current `src/main/resources/application-prod.properties`
> in the repo before finishing — that file is the source of truth for what env vars exist, and
> it may have grown since this doc was last updated.

### 2.4 Clone & Build

Check how the old VM authenticates to GitHub first (`cd ~/SportsCardProject && git remote -v`
on the old VM) — on the 2026-08-29 run it turned out to be a **public repo with no credential
at all**. If yours is public too, you can skip auth entirely. If you want a credential anyway
(e.g. planning to go private, or want push access), SSH deploy key is cleanest:

```bash
ssh-keygen -t ed25519 -C "sports-card-app-2"    # accept defaults, empty passphrase
cat ~/.ssh/id_ed25519.pub                        # paste into github.com/settings/keys
ssh -T git@github.com                            # confirm "successfully authenticated"
```

Then clone and build:
```bash
cd ~
git clone git@github.com:RalphGodcreative/SportsCardProject.git   # or https:// if public/no key
cd SportsCardProject
chmod +x mvnw          # git doesn't always preserve the exec bit on a fresh clone
./mvnw clean package -Dmaven.test.skip=true -Pprod
```

Building on the e2-micro is slow (1 vCPU, 1 GB RAM) — expect several minutes.

### 2.5 New Google OAuth2 Client

The OAuth2 credential is scoped to a GCP project, not to the VM — since this is a new
project, you need a new client. Google's console UI here is now called **"Google Auth
Platform"** and is split across tabs (Overview / Branding / Audience / Data Access / Clients),
not the older single wizard:

1. **Audience** tab — choose **External** (not Internal — Internal restricts login to
   accounts inside your auto-created org, which for a personal account means basically only
   yourself; External lets any Google account attempt login, same as before). Fill in app
   name / support email. Add your own account under **Test users** while in Testing mode.
2. **Data Access** tab — Add or Remove Scopes → select `.../auth/userinfo.email` and
   `.../auth/userinfo.profile` (matches `scope=email,profile` in `application-prod.properties`).
3. **Clients** tab — Create Client:
   - Type: **Web application**
   - **Authorized JavaScript origins:** `https://rgsportscards.com`
   - **Authorized redirect URIs:** `https://rgsportscards.com/login/oauth2/code/google`
   - Copy the **Client ID** and **Client secret** into `/etc/sportscard/env` as
     `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET`.

You can sanity-check the new client works by trying it against a local dev instance before
relying on it in prod.

### 2.6 Nginx

```bash
sudo apt install -y nginx
sudo nano /etc/nginx/sites-available/sportscard
```

Paste (identical to the old VM's config — confirm with `cat /etc/nginx/sites-available/sportscard`
on the old VM first if you want to double check nothing custom was added):

```nginx
server {
    listen 80;
    server_name rgsportscards.com www.rgsportscards.com;

    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto https;
    }
}
```

```bash
sudo ln -s /etc/nginx/sites-available/sportscard /etc/nginx/sites-enabled/
sudo rm -f /etc/nginx/sites-enabled/default
sudo nginx -t
sudo systemctl restart nginx
```

### 2.7 systemd Service

**Check the new VM's actual username first** (`whoami`) — it will likely differ from the old
VM's username, so don't copy the old service file verbatim.

```bash
sudo nano /etc/systemd/system/sportscard.service
```

```ini
[Unit]
Description=SportsCardProject Spring Boot App
After=network.target

[Service]
User=<your_new_vm_username>
EnvironmentFile=/etc/sportscard/env
ExecStart=/usr/bin/java -jar -Dspring.profiles.active=prod /home/<your_new_vm_username>/SportsCardProject/target/SportsCardProject-0.0.1-SNAPSHOT.jar
SuccessExitStatus=143
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
```

```bash
sudo systemctl daemon-reload
sudo systemctl enable sportscard
sudo systemctl start sportscard
sudo journalctl -u sportscard -f
```

Confirm it boots clean: Flyway/Hibernate init against Neon, security filter chain loads
(including the OAuth2 filters), Tomcat started on port 8080.

## Part 3 — Verify Before Cutover

```bash
curl -I http://localhost/login
curl -I http://localhost/assets/style.css
```

Both should return `200`. Note: `curl -I http://localhost` on `/` now correctly returns `200`
with the homepage HTML (not a redirect to `/login`) — the app has had a public homepage since
phase 2, so a `302` is *not* the expected result anymore.

**You can't fully verify Google OAuth2 login or outbound email pre-cutover** — Cloudflare
terminates TLS and the VM only listens on port 80, so a browser can't complete the
`https://rgsportscards.com/...` OAuth2 redirect against the new VM directly. This is fine:
skip deep pre-cutover verification for those two and test them live immediately after the DNS
flip instead — rollback (flipping the Cloudflare A record back) is instant if anything's wrong.

## Part 4 — Cutover

1. In Cloudflare DNS, update the `A` record to the **new** VM's external IP (see
   [GCP_MIGRATION.md Part 6](GCP_MIGRATION.md#part-6--cloudflare-dns-update) — keep Proxy
   status **Proxied**).
2. Verify: `curl -I https://rgsportscards.com` (should show `server: cloudflare`, `HTTP/2 200`).
3. In an actual browser: load the site, try **Login with Google** end-to-end, try a form
   login, and trigger anything that sends email.
4. Watch `sudo journalctl -u sportscard -f` on the new VM while doing this.

## Part 5 — Decommission the Old Account

Once the new VM has been serving traffic cleanly and Google login / email are confirmed
working live:

1. In the **old** project: **IAM & Admin → Manage Resources**, select the project, click
   **Shut Down**, type the project ID to confirm.
   - This immediately stops all billing for everything in it (VM, disk, firewall rules, the
     old OAuth client) — the VM's status flips to **Stopped** right away.
   - The project disappears from the active Manage Resources list immediately, but is
     recoverable for **30 days** before permanent deletion — no charges accrue during that
     window either way.
   - **This does not touch your Google account** — Gmail, the App Password used for SMTP,
     etc. are all account-level, not project-level, and are completely unaffected.
2. Double-check Cloudflare no longer has any DNS record pointing at the old VM's IP.
3. Optional: if you want zero GCP billing account tied to your card at all (not just $0
   usage), you can also close the old billing account once the project shutdown is confirmed
   — not necessary, since an empty billing account with no projects doesn't charge anything.

## Rollback

If the new VM misbehaves after DNS cutover, just flip the Cloudflare `A` record back to the
old VM's IP — instant, as long as you haven't completed Part 5 yet.
