# Render → GCP Free Tier Migration Guide

## Overview

This guide covers moving the SportsCardProject (Spring Boot 3.1, Java 17, PostgreSQL) from Render to a GCP e2-micro VM (always-free tier), and updating Cloudflare DNS to point to the new server.

**What this solves vs Render:**
- App runs 24/7 (no sleep on inactivity)
- Cron job (`CrawlerScheduler`) fires reliably at 9 PM every night
- Outbound SMTP port 587 is open — Gmail email works

---

## Part 1 — GCP Account & VM Setup

### 1.1 Create a GCP Account

1. Go to https://cloud.google.com and sign up
2. A billing account is required even for free-tier use (you won't be charged for e2-micro within limits)
3. Create a new **Project** (e.g., `sports-card-project`)

### 1.2 Create the Free-Tier VM

1. Go to **Compute Engine → VM Instances → Create Instance**
2. Set the following:

| Field | Value |
|---|---|
| Name | `sports-card-app` |
| Region | `us-central1` (or `us-east1` / `us-west1`) |
| Zone | any zone in that region |
| Machine type | `e2-micro` |
| Boot disk | Debian 12 (Bookworm), 30 GB standard persistent disk |
| Firewall | Check **Allow HTTP traffic** and **Allow HTTPS traffic** |

3. Click **Create**

> The e2-micro in us-central1/us-east1/us-west1 is free: 1 instance per billing account, up to 30 GB disk, 1 GB egress/month. Stay within these limits to avoid charges.

### 1.3 Open Port 80 in GCP Firewall

> **Note:** Checking "Allow HTTP traffic" during VM creation does NOT always create the firewall rule. Create it manually to be safe.

In **VPC Network → Firewall → Create Firewall Rule**:
- Name: `allow-http`
- Direction: Ingress
- Targets: All instances in the network
- Source IPv4 ranges: `0.0.0.0/0`
- Protocols and ports: TCP `80`

Click **Create**.

---

## Part 2 — Configure the VM

SSH into the VM from the GCP Console (click **SSH** button on the VM list).

### 2.1 Install Java 17

```bash
sudo apt update && sudo apt upgrade -y
sudo apt install -y openjdk-17-jdk
java -version   # should print openjdk 17
```

### 2.2 Set Environment Variables

Your database is already on Neon — just use the connection string from the Neon dashboard directly.

Create an environment file that systemd will load:

```bash
sudo mkdir -p /etc/sportscard
sudo nano /etc/sportscard/env
```

Paste your environment variables:

```
DB_URL=jdbc:postgresql://<neon-host>/<dbname>?sslmode=require
DB_USERNAME=your_neon_username
DB_PASSWORD=your_neon_password
GMAIL_USERNAME=your_gmail@gmail.com
GMAIL_APP_PASSWORD=your_app_password
YOUTUBE_API_KEY=your_key
GEMINI_API_KEY=your_key
EBAY_CLIENT_ID=your_id
EBAY_CLIENT_SECRET=your_secret
```

> Copy the exact connection string from **Neon Dashboard → your project → Connection Details → Java (JDBC)**. It already includes `sslmode=require`.

Secure the file:

```bash
sudo chmod 600 /etc/sportscard/env
sudo chown root:root /etc/sportscard/env
```

---

## Part 3 — Clone & Build on the VM

SSH into the VM, then install Git and Maven:

```bash
sudo apt install -y git maven
```

### 3.1 Clone the Repository

```bash
cd ~
git clone https://github.com/your_username/SportsCardProject.git
cd SportsCardProject
```

### 3.2 Build the JAR

```bash
./mvnw clean package -DskipTests -Pprod
```

The JAR will be at:
```
~/SportsCardProject/target/SportsCardProject-0.0.1-SNAPSHOT.jar
```

> Building on the e2-micro is slow (1 vCPU, 1 GB RAM) — expect a few minutes per build.

---

## Part 4 — Set Up Nginx Reverse Proxy

Cloudflare connects to your VM on port 80. The app runs on port 8080. Nginx bridges the two.

### 4.1 Install Nginx

```bash
sudo apt install -y nginx
```

### 4.2 Create the Site Config

```bash
sudo nano /etc/nginx/sites-available/sportscard
```

Paste:

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

> **Why hardcode `https` instead of `$scheme`:** with Cloudflare SSL/TLS mode set to **Flexible** (see Part 6), Cloudflare always connects to this VM over plain HTTP, so `$scheme` here is always `http` — even when the visitor's browser used HTTPS. Forwarding `$scheme` as-is tells Spring Boot every request is `http`, which makes it generate `http://` redirect URLs and triggers mixed-content errors in the browser. Hardcoding `https` is safe here because Cloudflare (proxied/orange-cloud) is the only thing that reaches this VM on port 80, and it only proxies requests it received from the visitor over HTTPS as long as Cloudflare's **Always Use HTTPS** setting is enabled.

### 4.3 Enable the Site

```bash
sudo ln -s /etc/nginx/sites-available/sportscard /etc/nginx/sites-enabled/
sudo rm /etc/nginx/sites-enabled/default
sudo nginx -t
sudo systemctl restart nginx
```

Verify nginx is proxying to the app:

```bash
curl -v http://localhost
# Should return HTTP 302 redirect to /login
```

> Spring Boot must also be told to trust the `X-Forwarded-*` headers nginx sends, otherwise it still builds redirect/absolute URLs using the scheme of the nginx→app connection (`http`) instead of the original visitor scheme. Set `server.forward-headers-strategy=framework` in `application-prod.properties`.

---

## Part 5 — Run as a systemd Service (24/7)

### 4.1 Create the Service File

```bash
sudo nano /etc/systemd/system/sportscard.service
```

Paste:

```ini
[Unit]
Description=SportsCardProject Spring Boot App
After=network.target

[Service]
User=your_username
EnvironmentFile=/etc/sportscard/env
ExecStart=/usr/bin/java -jar -Dspring.profiles.active=prod /home/your_username/SportsCardProject/target/SportsCardProject-0.0.1-SNAPSHOT.jar
SuccessExitStatus=143
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
```

Replace `your_username` with your actual Linux username on the VM.

### 4.2 Enable & Start

```bash
sudo systemctl daemon-reload
sudo systemctl enable sportscard
sudo systemctl start sportscard
```

Check logs:

```bash
sudo journalctl -u sportscard -f
```

The app will now:
- Start automatically on VM reboot
- Restart automatically if it crashes
- Run the `CrawlerScheduler` cron at 9 PM every night

---

## Part 6 — Cloudflare DNS Update

Once the app is running and confirmed healthy on GCP:

1. Log in to your **Cloudflare dashboard**
2. Select your domain
3. Go to **DNS → Records**
4. Find the **A record** that currently points to your Render IP
5. Update the **IPv4 address** to your GCP VM's **External IP**
   - Keep **Proxy status** as **Proxied** (orange cloud) — this keeps DDoS protection active
   - TTL can be `Auto`
6. Click **Save**

DNS propagation is instant through Cloudflare's proxy. Verify with:

```bash
curl -I https://yourdomain.com
```

### Cloudflare SSL/TLS Setting

Make sure SSL/TLS mode is set correctly:
- Go to **SSL/TLS → Overview**
- If you're running the app on HTTP (port 8080/80) behind Cloudflare: set to **Flexible**
- If you install a certificate on the VM (e.g., Certbot): set to **Full (strict)**

For simplicity with Cloudflare proxying, **Flexible** mode works without any certificate setup on the VM — Cloudflare handles HTTPS to your visitors and connects to your VM over HTTP.

---

## Part 7 — Deploying Updates

When you push new code, the update flow is:

1. Push your changes to GitHub from your local machine (normal `git push`)
2. SSH into the VM
3. Pull and rebuild:

```bash
cd ~/SportsCardProject
git pull
./mvnw clean package -DskipTests -Pprod
sudo systemctl restart sportscard
```

---

## Checklist

- [ ] GCP account created with billing account attached
- [ ] e2-micro VM created in us-central1/us-east1/us-west1 with Standard persistent disk (30 GB)
- [ ] GCP firewall rule `allow-http` created for TCP port 80
- [ ] Java 17, Git, Maven installed on VM
- [ ] `/etc/sportscard/env` created with all environment variables (Neon JDBC URL included)
- [ ] Repo cloned and JAR built (`./mvnw clean package -DskipTests -Pprod`)
- [ ] Nginx installed, site config created, default site removed
- [ ] `sportscard.service` created and enabled
- [ ] App starts and logs look healthy (`journalctl -u sportscard -f`)
- [ ] Cloudflare A record (`@`) updated to GCP external IP, CNAME `www` pointing to root
- [ ] Cloudflare SSL/TLS mode set to **Flexible**
- [ ] Email (SMTP) tested and working
- [ ] Yahoo Auction search tested and working
- [ ] Cron job fires at scheduled time (9 PM Taiwan time)
