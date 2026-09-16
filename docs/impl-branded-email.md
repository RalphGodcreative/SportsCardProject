# Implementation: Branded Email on `rgsportscards.com`

Move outbound mail off Ralph's personal Gmail and onto a `@rgsportscards.com` address, and lift
the sending ceiling from Gmail's ~500/day to something that survives public signups.

Mail rides on the **same** Cloudflare zone that already serves the site — the one holding the
`A` record from [GCP_MIGRATION.md](GCP_MIGRATION.md) Part 6. Nothing here touches the `A`
record, the nginx `server_name`, or the OAuth2 redirect URI, so the site and Google login are
unaffected throughout.

---

## Current State

Outbound mail is Gmail SMTP, configured in `application-prod.properties:14-19`:

```properties
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=${GMAIL_USERNAME}
spring.mail.password=${GMAIL_APP_PASSWORD}
```

Two senders exist, both in `EmailService.java`:

| Method | Line | Sets `From`? | Used by |
|---|---|---|---|
| `sendHtmlEmail` | 92 | Yes — **derived from the SMTP username** | the crawler digest, via `sendSearchResultEmail` |
| `sendSimpleEmail` | 75 | **No** | **nothing — zero callers** |

The blocking line is `EmailService.java:97`:

```java
String fromAddress = env.getProperty("spring.mail.username");
helper.setFrom(fromAddress, "RG Sports Cards");
```

This works **only with Gmail**, where the SMTP username happens to be an email address. Every
ESP uses an API key or generated handle as the SMTP username — Resend's is the literal string
`resend`, SES's looks like `AKIA...`. So this line must stop reading `spring.mail.username`
before any provider switch.

The display name `"RG Sports Cards"` is already correct, so mail currently renders as
`RG Sports Cards <you@gmail.com>` — only the address gives it away.

`sendSimpleEmail` sets no `From` at all. Gmail silently rewrites it to the authenticated user;
most ESPs reject the message outright. It currently has **no callers** (and its
`templates/mail/defaultMail.html` is likewise unused), so this is a trap waiting for whoever
wires up password reset — not a live bug. Fix it or delete it; don't leave it as-is.

---

## Architecture — Two Independent Halves

The most common mistake here is expecting Cloudflare to do both jobs. **Cloudflare Email
Routing cannot send mail.** It is inbound forwarding only. You need both halves:

```
INBOUND (free, Cloudflare)
  someone@example.com
        |  writes to support@rgsportscards.com
        v
  Cloudflare Email Routing  --forwards-->  you@gmail.com

OUTBOUND (an ESP - Cloudflare cannot do this)
  Spring Boot (JavaMailSender)
        |  SMTP :587
        v
  Brevo / Resend / SES  --signed with DKIM-->  the user's inbox
        From: RG Sports Cards <alerts@rgsportscards.com>
```

Both halves authenticate against the same domain's DNS, which is why the SPF record has to be
shared between them — see [Part 4](#part-4--dns-records-on-cloudflare).

---

## Rollout Sequence

The Parts below are organized by topic, not by the order you actually do them — local testing
is Part 10 but comes first in practice, and the code change in Part 5 is the one people assume
they can skip. This is the chronological path, using **Brevo** as the example provider.

The short version is *three* things, not two: **DNS + environment variables + a code change.**
Doing only the first two gets you a fully configured ESP that still sends from your Gmail
address.

### Stage 1 — Local (nothing external touched, fully reversible)

1. Start Mailpit and point local `application.properties` at `localhost:1025`, with **auth and
   starttls off** → [10.2](#102-mailpit--a-local-smtp-sink-recommended-default)
2. Make the code changes: `app.mail.from` in `EmailService`, `MAIL_*` placeholders in **both**
   properties files → [5.1](#51-decouple-from-from-the-smtp-username), [5.2](#52-properties)
3. Add the empty-digest guard → [5.4](#54-stop-sending-empty-digests)
4. Confirm in Mailpit's UI that `From:` reads `alerts@rgsportscards.com` and the digest renders,
   including the empty-keyword section

### Stage 2 — Provider (external, but nothing live changes yet)

5. Create the Brevo account and generate an **SMTP key** — not your account password
6. Add `rgsportscards.com` as a sending domain in Brevo; it generates the DNS records you'll
   paste in Stage 3

   > **⚠️ Brevo's SMTP username is your account login email.** This is exactly why Stage 1
   > step 2 is mandatory: with the old code, `From:` is read from `spring.mail.username`, so
   > it would become your Brevo login address — most likely the same Gmail you're trying to
   > get away from.

7. Optional but recommended: send one real message from localhost through Brevo to yourself →
   [10.3](#103-the-real-esp-from-localhost). This is the only way to prove the `From:` fix works
   against a real ESP, since Mailpit accepts any sender.

### Stage 3 — DNS (live, but reversible in seconds)

Order matters here — Email Routing creates the SPF record that Brevo's include gets merged
into, so enable it first or you'll merge and then find a second record appear.

8. Enable Cloudflare Email Routing for `support@` and `dmarc@` → [Part 2](#part-2--cloudflare-email-routing-inbound-free).
   This adds the MX records and the initial SPF record.
9. Paste Brevo's ownership (`brevo-code`) and DKIM records into Cloudflare →
   [4.2](#42-dkim--mind-the-orange-cloud). **Any CNAME must be grey cloud / DNS only.**
10. **Merge** `include:spf.brevo.com` into that single existing SPF record →
    [4.1](#41-spf--merge-do-not-add-a-second-record). Do not add a second TXT record.
11. Add DMARC at `_dmarc` with `p=none` → [4.3](#43-dmarc--start-permissive)

### Stage 4 — Production deploy (the only step that can cause downtime)

12. Update `/etc/sportscard/env` with `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`,
    `MAIL_FROM`, `MAIL_REPLY_TO` → [5.3](#53-vm-environment-file)

    > **⚠️ Env file before JAR.** Spring fails fast on an unresolvable `${MAIL_HOST}`. Deploy
    > the new JAR against the old env file and the app will not start.

13. Build and deploy the JAR. **A rebuild is required, not just a restart** — the mail block
    lives in `application-prod.properties`, which ships *inside* the JAR.
14. `sudo systemctl restart sportscard`, then watch `sudo journalctl -u sportscard -f` for a
    clean boot.

> **Shortcut for a connectivity-only test:** Spring env vars override properties, so setting
> `SPRING_MAIL_HOST=smtp-relay.brevo.com` in the env file redirects the relay with no rebuild.
> It cannot fix the `From:` address, so it's a smoke test only — you still need the Stage 1
> code change.

### Stage 5 — Verify

Full commands and expected output in [Part 7](#part-7--verify).

15. `dig` shows exactly one SPF record, plus DKIM and DMARC
16. A real digest shows SPF / DKIM / DMARC all `PASS` in Gmail's **Show original**
17. mail-tester.com scores ≥ 9/10
18. Keep the Gmail App Password active until all of the above passes →
    [Part 8](#part-8--rollback)

### What's reversible, and how fast

| Stage | Rollback |
|---|---|
| 1–2 | Nothing live has changed; discard the branch |
| 3 | Edit or delete the Cloudflare records — DNS only, the site's `A` record is untouched |
| 4 | Restore the Gmail values in `/etc/sportscard/env` and restart → [Part 8](#part-8--rollback) |

---

## Part 1 — Decide the Addresses

| Address | Purpose | Handled by |
|---|---|---|
| `alerts@rgsportscards.com` | `From:` on the daily crawler digest | ESP (outbound) |
| `support@rgsportscards.com` | `Reply-To:`, and the address users write to | Cloudflare Email Routing (inbound) |
| `dmarc@rgsportscards.com` | DMARC aggregate report destination | Cloudflare Email Routing (inbound) |

**Do not use `noreply@`.** It blocks replies, reads as cold to users, and engagement signals
(including replies) feed inbox-placement reputation. `alerts@` with a working `Reply-To:` is
strictly better and costs nothing.

---

## Part 2 — Cloudflare Email Routing (Inbound, Free)

1. Open the existing `rgsportscards.com` zone in Cloudflare — the same one serving the site's
   `A` record. No new zone is needed.
2. In that zone: **Email → Email Routing → Get started**.
3. Cloudflare offers to add the required **MX records** plus an **SPF TXT record**
   automatically. Accept — but note what the SPF record says, you will edit it in Part 4:
   ```
   v=spf1 include:_spf.mx.cloudflare.net ~all
   ```
4. Under **Destination addresses**, add `you@gmail.com` and click the verification link
   Google delivers. Nothing forwards until this is verified.
5. Under **Routing rules**, create:
   - `support@rgsportscards.com` → `you@gmail.com`
   - `dmarc@rgsportscards.com` → `you@gmail.com`
   - Optionally enable **catch-all** → same destination, so a typo'd address isn't lost.
6. Send a test from any outside account to `support@rgsportscards.com` and confirm it lands in
   Gmail.

That's the professional-looking inbound address done, for $0/month, forever.

---

## Part 3 — Pick a Sending Provider

The digest is one email per user per day, so the **daily cap** is the number that binds — not
the monthly one.

| Provider | Free tier | Users it supports | Paid entry | SMTP username is… |
|---|---|---|---|---|
| **Resend** | 100/day, 3k/mo | ~100 | ~$20/mo → 50k | the literal string `resend` |
| **Mailjet** | 200/day, 6k/mo | ~200 | — | an API key |
| **Brevo** | 300/day, 9k/mo | ~300 | — | your login email |
| **Amazon SES** | none useful here | unlimited | **$0.10 / 1,000** | IAM SMTP cred (`AKIA…`) |

> ⚠️ **Verify these numbers before committing.** Free tiers get cut regularly; the figures
> above were accurate to the best of our knowledge in 2026 and should be re-checked on each
> provider's pricing page.

### The cost reality

At SES pricing, **100 users × 30 days = 3,000 emails = $0.30/month**. Email is not a cost
problem at any plausible scale for this project — it's a setup-effort problem. Two caveats on
SES: you must apply to leave the sandbox (short form, usually approved in a day or two, until
then you can only send to verified addresses), and the AWS console is considerably less
pleasant than Resend's.

The **62,000/month free SES tier does not apply to us** — that is only for senders running on
EC2, and this app runs on a GCP e2-micro.

### Recommendation

Start on **Brevo** (most daily headroom on a free tier) or **Resend** (best docs). Because the
app already uses Spring's `JavaMailSender`, switching is an env-var change plus the code fix in
Part 5 — no rewrite. Move to SES when you outgrow the free daily cap.

**Port note:** GCP blocks outbound port **25** on all VMs. Use **587** (or 2525 as a fallback);
this is the same constraint already documented for Gmail SMTP.

---

## Part 4 — DNS Records on Cloudflare

Three records make the difference between "lands in inbox" and "lands in spam". Skipping these
wastes the provider switch entirely.

### 4.1 SPF — merge, do not add a second record

**This is the step people break.** Email Routing (Part 2) already created an SPF record. Adding
your ESP's SPF as a *second* TXT record produces two SPF records on one name, which is a
permanent error (`permerror`) — mail fails authentication worse than having no SPF at all.

Edit the **existing** record to include both:

```
Type: TXT   Name: @   Content:
v=spf1 include:_spf.mx.cloudflare.net include:<provider-token> ~all
```

Take `<provider-token>` from your ESP's domain wizard — e.g. `spf.brevo.com` for Brevo, or
`amazonses.com` for Resend/SES. SPF allows 10 DNS lookups; two includes is comfortable.

### 4.2 DKIM — mind the orange cloud

Your provider's wizard will give you either TXT records (Brevo, Resend) or three CNAMEs (SES
Easy DKIM). Add them exactly as given.

- **TXT records are never proxied** by Cloudflare — nothing to watch for.
- **CNAME records must be set to DNS only (grey cloud).** Cloudflare defaults new CNAMEs to
  proxied (orange), which rewrites the answer and silently breaks DKIM validation. This is the
  single most common Cloudflare-specific mail failure.

### 4.3 DMARC — start permissive

```
Type: TXT   Name: _dmarc   Content:
v=DMARC1; p=none; rua=mailto:dmarc@rgsportscards.com; pct=100
```

`p=none` means "monitor, don't reject" — it collects reports without risking legitimate mail
while SPF/DKIM settle. Once reports show consistent passes for a week or two, tighten to
`p=quarantine`, then `p=reject`.

### 4.4 Optional hardening — a sending subdomain

Sending from `alerts@send.rgsportscards.com` instead of `alerts@rgsportscards.com` isolates bulk-send
reputation from the root domain, and **sidesteps the SPF merge in 4.1 entirely** (the subdomain
gets its own SPF record, so there is no collision with Email Routing).

The trade-off is a slightly uglier `From:` address. For a consumer-facing product the prettier
root address is usually worth the merge step — but if the digest ever grows into anything
marketing-shaped, move it to a subdomain.

---

## Part 5 — Code Changes

### 5.1 Decouple `From` from the SMTP username

`EmailService.java:97` — replace:

```java
String fromAddress = env.getProperty("spring.mail.username");
helper.setFrom(fromAddress, "RG Sports Cards");
```

with:

```java
String fromAddress = env.getProperty("app.mail.from");
String fromName    = env.getProperty("app.mail.from-name", "RG Sports Cards");
helper.setFrom(fromAddress, fromName);
helper.setReplyTo(env.getProperty("app.mail.reply-to", fromAddress));
```

And give `sendSimpleEmail` (line 75) the `From` it currently lacks:

```java
message.setFrom(env.getProperty("app.mail.from"));
```

### 5.2 Properties

In **both** `application-prod.properties` and `application.properties.example`, replace the
Gmail-specific block:

```properties
spring.mail.host=${MAIL_HOST}
spring.mail.port=${MAIL_PORT:587}
spring.mail.username=${MAIL_USERNAME}
spring.mail.password=${MAIL_PASSWORD}
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true

app.mail.from=${MAIL_FROM}
app.mail.from-name=${MAIL_FROM_NAME:RG Sports Cards}
app.mail.reply-to=${MAIL_REPLY_TO:support@rgsportscards.com}
```

Renaming `GMAIL_USERNAME`/`GMAIL_APP_PASSWORD` to `MAIL_*` keeps the provider out of the
variable names, so the next switch touches no code.

### 5.3 VM environment file

On the VM, update `/etc/sportscard/env` (see [GCP_MIGRATION.md](GCP_MIGRATION.md) 2.2):

```
MAIL_HOST=smtp-relay.brevo.com
MAIL_PORT=587
MAIL_USERNAME=<provider smtp username>
MAIL_PASSWORD=<provider smtp key>
MAIL_FROM=alerts@rgsportscards.com
MAIL_REPLY_TO=support@rgsportscards.com
```

Then remove the now-unused `GMAIL_USERNAME` / `GMAIL_APP_PASSWORD` lines.

> **⚠️ Order matters.** Spring fails fast on an unresolvable `${…}` placeholder — if you deploy
> the new JAR before updating `/etc/sportscard/env`, the app **will not start**. Update the env
> file first, then `sudo systemctl restart sportscard`.

### 5.4 Stop sending empty digests

This matters more than the provider choice. `CrawlerService.java:111-115` mails every user who
has *any* keyword, whether or not anything was found — `moveEmptyListsToEnd` exists precisely
to push empty keyword sections to the bottom of the template. So a user with five keywords and
a quiet day still receives a "Yahoo Auction Search Result" email containing nothing.

At one user that's harmless. At a hundred it is the fastest way to teach Gmail that
`rgsportscards.com` sends spam — and domain reputation takes weeks to repair. No ESP protects you
from this.

In `runCrawlerForAllUsers`, after building `resultList`:

```java
boolean hasAnyResult = resultList.values().stream()
        .anyMatch(list -> list != null && !list.isEmpty());
if (!hasAnyResult) {
    continue;
}
emailService.sendSearchResultEmail(resultList, user.getEmail());
```

Apply the same guard to `getResultAsync` only if the manual "search now" flow should stay
silent on no results — arguably a user who clicked the button *wants* confirmation either way,
so leaving that one alone is defensible.

> `CrawlerService` has no `@Slf4j` (it uses `e.printStackTrace()` at line 68). Add the
> annotation if you want to log skipped digests.

### 5.5 Unsubscribe (phase 2, not a one-liner)

Gmail and Yahoo's bulk-sender rules make one-click unsubscribe effectively mandatory. Current
volume is below the 5,000/day threshold where it's strictly enforced, but it converts an angry
spam-report into a quiet opt-out, which is the difference that protects the domain.

This needs more than a header — budget it as a small feature:

1. A migration adding `users.digest_enabled BOOLEAN NOT NULL DEFAULT true` and
   `users.unsubscribe_token VARCHAR` (random UUID per user).
2. A `GET`/`POST /unsubscribe?token=…` endpoint, `permitAll()` in `SecurityConfig` — it must
   work without a login session.
3. `runCrawlerForAllUsers` skipping users where `digest_enabled` is false.
4. Headers on the digest, in `sendHtmlEmail`:
   ```java
   message.addHeader("List-Unsubscribe", "<https://rgsportscards.com/unsubscribe?token=" + token + ">");
   message.addHeader("List-Unsubscribe-Post", "List-Unsubscribe=One-Click");
   ```
5. A visible unsubscribe link in the footer of `templates/mail/email-result.html` — the header
   alone satisfies the machines, not the humans.

---

## Part 6 — Reply as `support@` from Gmail

Email Routing delivers replies *to* you, but replying from Gmail would still show your personal
address. To fix that:

1. Gmail → **Settings → Accounts and Import → Send mail as → Add another email address**.
2. Address: `support@rgsportscards.com`. Uncheck "Treat as an alias" if you want replies to thread
   back to the branded address.
3. Enter your **ESP's SMTP credentials** (same values as `MAIL_USERNAME`/`MAIL_PASSWORD`).
4. Google mails a verification code to `support@rgsportscards.com` — Email Routing forwards it right
   back to your inbox. Paste it in.

You can now pick `support@rgsportscards.com` in Gmail's From dropdown.

**Why not Google Workspace?** ~$7/user/month gets a real mailbox on the domain and a 2,000/day
sending limit. It's a reasonable upgrade once there's revenue and real support volume, but it's
priced per human user and isn't built for app-transactional sending — you'd still want an ESP
for the digest. Cloudflare Routing + ESP covers both jobs at $0.

---

## Part 7 — Verify

From any machine:

```bash
dig TXT rgsportscards.com +short            # exactly ONE v=spf1 line, with both includes
dig TXT _dmarc.rgsportscards.com +short     # the DMARC policy
dig MX rgsportscards.com +short             # Cloudflare's route*.mx.cloudflare.net
```

DKIM lives at a provider-specific name, e.g.:

```bash
dig TXT resend._domainkey.rgsportscards.com +short
dig TXT mail._domainkey.rgsportscards.com +short
```

Then send a real one:

1. Trigger a digest — log in and hit the manual crawler search, or wait for the 9 PM cron.
2. In Gmail, open the message → **⋮ → Show original**. You want all three:
   ```
   SPF:   PASS
   DKIM:  PASS
   DMARC: PASS
   ```
3. Send a digest to a [mail-tester.com](https://www.mail-tester.com) address. Aim for 9/10 or
   better; it names exactly what's missing.
4. Confirm the `From:` reads `RG Sports Cards <alerts@rgsportscards.com>` and that replying lands at
   `support@rgsportscards.com`.

---

## Part 8 — Rollback

The old path stays available as long as the Gmail App Password hasn't been revoked. To revert,
put the Gmail values back in `/etc/sportscard/env`:

```
MAIL_HOST=smtp.gmail.com
MAIL_USERNAME=you@gmail.com
MAIL_PASSWORD=<gmail app password>
MAIL_FROM=you@gmail.com
```

then `sudo systemctl restart sportscard`. After the Part 5 code change, `MAIL_FROM` **must**
match the Gmail account — Gmail rejects a `From` it doesn't own. Keep the App Password active
until a branded send has been verified end to end.

DNS changes are independently reversible in Cloudflare and don't affect the site (the `A`
record and Email Routing records are separate concerns).

---

## Part 9 — When to Move to SES

Switch when you cross the free daily cap — i.e. roughly when paying users exist:

- **Trigger:** daily sends approaching the provider's cap (100/200/300 depending on Part 3).
- **Work:** verify the domain in SES, request sandbox removal, swap `MAIL_HOST` to
  `email-smtp.<region>.amazonaws.com`, generate SMTP credentials, update the SPF include and
  add SES's three DKIM CNAMEs (**grey cloud** — see 4.2).
- **No code change** — that's the point of `MAIL_*` and `app.mail.from`.
- **Cost:** ~$0.10 per 1,000. A thousand users on a daily digest is ~$3/month.

---

## Part 10 — Testing Locally

Do this **before** touching DNS. Four options, in increasing fidelity — pick by what you're
actually trying to learn.

Local config lives in `application.properties`, which is gitignored (created from
`application.properties.example`). Note that `.gitignore` excludes `application-*.properties`
with a single exception for `application-prod.properties`, so a new `application-local.properties`
would **not** be committed — fine for secrets, but don't expect to share it.

### 10.1 Iterating on the template — don't send anything

The fastest loop for working on `mail/email-result.html` involves no SMTP at all.
`buildEmailContent` (line 62) is public and, like `sendSimpleEmail`, currently has no callers.
Local config already sets `spring.thymeleaf.cache=false`, so a dev-only preview endpoint plus a
browser refresh gives you instant iteration:

```java
@Controller
@Profile("!prod")   // the bean does not exist in prod, so the URL 404s there
@RequiredArgsConstructor
public class MailPreviewController {

    private final EmailService emailService;

    @GetMapping(value = "/dev/mail-preview", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String preview() {
        SearchKeyword hit = new SearchKeyword();
        hit.setKeyword("大谷翔平 2018 Topps");
        SearchKeyword quiet = new SearchKeyword();
        quiet.setKeyword("Trout Auto");

        SearchProduct p = new SearchProduct();
        p.setTitle("2018 Topps Chrome Shohei Ohtani RC PSA 10");
        p.setPrice(48000);
        p.setLink("https://tw.bid.yahoo.com/item/example");
        p.setImage("https://via.placeholder.com/80");

        Map<SearchKeyword, List<SearchProduct>> data = new LinkedHashMap<>();
        data.put(hit, List.of(p));
        data.put(quiet, List.of());   // exercises the empty-section path

        return emailService.buildEmailContent("mail/email-result",
                Map.of("resultList", data, "formattedDate", "2026-09-16"));
    }
}
```

`@Profile("!prod")` is the right guard — production runs with `-Dspring.profiles.active=prod`,
so the controller is never instantiated there. Don't instead add `/dev/**` to `permitAll()` in
`SecurityConfig`, since that rule would apply in prod too. Locally you'll just need to be
logged in to reach it.

Include an empty keyword list in the fake data, as above — that path is what
[5.4](#54-stop-sending-empty-digests) is about, and it's easy to forget it renders at all.

### 10.2 Mailpit — a local SMTP sink (recommended default)

Catches every message, delivers none, shows them in a web UI. Nothing can escape to a real
user, which makes it the safe place to test the *send* path.

```bash
docker run -d --name mailpit -p 1025:1025 -p 8025:8025 axllent/mailpit
```

In your local `application.properties`:

```properties
spring.mail.host=localhost
spring.mail.port=1025
spring.mail.username=
spring.mail.password=
spring.mail.properties.mail.smtp.auth=false
spring.mail.properties.mail.smtp.starttls.enable=false

app.mail.from=alerts@rgsportscards.com
app.mail.reply-to=support@rgsportscards.com
```

> **Both `auth` and `starttls` must be `false`.** Mailpit requires neither, and leaving the
> prod values at `true` produces a connection failure that looks like a code bug.

Open <http://localhost:8025> to read what the app sent. Mailpit also renders an HTML
compatibility check, which is useful given how many email clients mangle the dark-themed table
layout in `email-result.html`.

MailHog is the older equivalent and is no longer maintained — prefer Mailpit.

### 10.3 The real ESP, from localhost

Both candidate providers let you send genuinely from a dev machine, which is how you verify the
SMTP-username-is-not-an-email-address problem is actually fixed:

- **Resend** — sending from `onboarding@resend.dev` works with **no domain verification at
  all**, but will only deliver to the email address you signed up with. Ideal for a solo dev
  loop before `rgsportscards.com` exists in Cloudflare.
- **Brevo** — issue an SMTP key and point `MAIL_HOST` at `smtp-relay.brevo.com`. Free-tier
  sends from a dev box count against the same daily cap as production.

This is the only way to confirm the Part 5 code change works against a real ESP, because the
failure mode it fixes (`spring.mail.username` not being an address) cannot reproduce against
Mailpit, which accepts anything.

### 10.4 Mailtrap — a catcher that also scores deliverability

Hosted fake inbox: mail is captured rather than delivered, but you also get a spam-score and
authentication report. Useful as a dress rehearsal for `mail-tester.com` without burning a real
send. Free tier is a single inbox with a modest monthly cap.

### 10.5 Gmail, exactly as it is today

Your current setup already works from localhost — the App Password isn't tied to the VM. Zero
setup, and a fine smoke test that the digest renders and sends. It tells you **nothing** about
the branded-sender work, since the `From` will be your Gmail no matter what `app.mail.from`
says (Gmail rejects a `From` it doesn't own).

### 10.6 The gotcha that will waste your afternoon

`sendHtmlEmail` is `@Async` (line 91) and swallows `MessagingException` into a `log.error` (line
106). So **a failed send looks exactly like a successful one** from the caller's side — the HTTP
request returns fine and the UI shows no error. Two consequences when testing locally:

- **Watch the log, not the browser.** A successful attempt prints `sending email {} to {}` at
  line 103. If you see that line and nothing after it, delivery worked; if an error follows, it
  didn't.
- **Authentication and connection failures take a different path.** `mailSender.send()` throws
  `MailAuthenticationException` / `MailSendException`, which are *runtime* exceptions and so are
  not caught by the `catch (MessagingException e)` block. They still get logged, but by Spring's
  async uncaught-exception handler rather than by `EmailService` — so the message looks
  unfamiliar and doesn't mention email. Wrong SMTP password shows up there, not at line 107.
- To debug synchronously, temporarily comment out `@Async` on line 91 so failures surface on the
  calling thread.

### 10.7 Triggering a digest locally

With keywords saved against your local user, hit the crawler UI's search action
(`/crawler/search-all` is `ROLE_ADMIN`-only, so use an admin account). The Yahoo fetch is plain
HTTPS and works from a dev machine. Alternatively call `getResultAsync` — but note it sends
whether or not there were results, so for template work prefer the 10.1 preview endpoint.

---

## Checklist

**Local testing (do first)**
- [ ] Mailpit running; local `application.properties` points at `localhost:1025` with auth and
      starttls **off**
- [ ] A digest renders correctly in Mailpit's UI, including the empty-keyword section
- [ ] One real send through the chosen ESP from localhost, confirming `app.mail.from` is
      honoured
- [ ] Optional: `/dev/mail-preview` endpoint behind `@Profile("!prod")`

**Inbound (Cloudflare, free)**
- [ ] `rgsportscards.com` added as a Cloudflare zone, nameservers pointed
- [ ] Email Routing enabled; MX + SPF records accepted
- [ ] `you@gmail.com` verified as a destination address
- [ ] Routing rules for `support@` and `dmarc@` (+ optional catch-all)
- [ ] Test mail to `support@rgsportscards.com` arrives in Gmail

**Outbound (ESP)**
- [ ] Provider chosen, free-tier daily cap re-verified against current pricing
- [ ] Domain verified in the provider's dashboard
- [ ] SPF **merged into the single existing record** (both includes, one TXT)
- [ ] DKIM records added — CNAMEs set to **DNS only / grey cloud**
- [ ] DMARC record added at `_dmarc` with `p=none`

**Code**
- [ ] `EmailService.java:97` reads `app.mail.from`, not `spring.mail.username`
- [ ] `sendSimpleEmail` sets a `From`
- [ ] `app.mail.*` + `MAIL_*` added to `application-prod.properties` **and**
      `application.properties.example`
- [ ] `/etc/sportscard/env` updated **before** deploying the new JAR
- [ ] Empty-digest guard added to `runCrawlerForAllUsers`
- [ ] Unsubscribe token + endpoint + headers + footer link (phase 2)

**Verify**
- [ ] `dig` shows exactly one SPF record, plus DKIM and DMARC
- [ ] A real digest shows SPF/DKIM/DMARC all `PASS` in Gmail's "Show original"
- [ ] mail-tester.com score ≥ 9/10
- [ ] `From:` is `alerts@rgsportscards.com`; replies reach `support@rgsportscards.com`
- [ ] Gmail App Password kept active until the above passes
