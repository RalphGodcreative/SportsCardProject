# Deployment Plan — Sports Card Project

**Goal:** Get the app live on a custom domain with free hosting.  
**Stack:** Cloudflare (domain + DNS + SSL) · Render (app hosting, free) · Neon (PostgreSQL, free)

---

## Architecture Overview

```
Browser
  └─▶ Cloudflare (DNS + SSL proxy)
        └─▶ Render Web Service (Spring Boot JAR)
              └─▶ Neon PostgreSQL (hosted DB)
```

---

## Step 1 — Buy Your Domain on Cloudflare

1. Go to [cloudflare.com](https://cloudflare.com) and create an account (or log in).
2. In the left sidebar click **Domain Registration → Register Domains**.
3. Search for your domain name (e.g., `rgcards.com`). Cloudflare sells at cost — no markup (~$9–11/year for `.com`).
4. Complete checkout. Your domain will appear under **Websites** in your dashboard.
5. Leave DNS empty for now — you'll add records in Step 4.

---

## Step 2 — Set Up Free PostgreSQL on Neon

1. Go to [neon.tech](https://neon.tech) and sign up with GitHub.
2. Click **New Project**, name it `sportscard-db`, choose region closest to you.
3. Neon creates a database. In the dashboard, click **Connection Details**.
4. Copy the **Connection String** — it looks like:
   ```
   postgresql://user:password@host.neon.tech/neondb?sslmode=require
   ```
5. Save this string securely — you'll need it in Step 3.

> **Neon free tier:** 0.5 GB storage, auto-suspend after inactivity (wakes on first request — cold start ~1-2 sec). Free forever.

---

## Step 3 — Set Up Render (Free App Hosting)

### 3a. Create a Render account

1. Go to [render.com](https://render.com) and sign up with GitHub.
2. Connect your GitHub account and authorize Render to access your `SportsCardProject` repo.

### 3b. Add a Dockerfile to your project

Render needs a `Dockerfile` to build and run your Spring Boot app. Create this file in the project root:

```dockerfile
# Build stage
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Run stage
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/SportsCardProject-0.0.1-SNAPSHOT.jar app.jar

# Install Chrome for Selenium crawler
RUN apt-get update && apt-get install -y \
  chromium \
  chromium-driver \
  && rm -rf /var/lib/apt/lists/*

ENV CHROME_BIN=/usr/bin/chromium
ENV CHROMEDRIVER_PATH=/usr/bin/chromedriver

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

Commit and push this file to GitHub.

### 3c. Create a Web Service on Render

1. In Render dashboard, click **New → Web Service**.
2. Connect your `SportsCardProject` GitHub repo.
3. Configure:
   - **Name:** `sportscard-app` (or anything you like)
   - **Region:** Oregon (or closest to you)
   - **Branch:** `master`
   - **Runtime:** Docker
   - **Instance Type:** Free

4. Under **Environment Variables**, add all your secrets (do NOT hardcode them):

   | Key | Value |
   |-----|-------|
   | `DB_URL` | your Neon connection string |
   | `DB_USERNAME` | your Neon username |
   | `DB_PASSWORD` | your Neon password |
   | `MAIL_USERNAME` | your Gmail address |
   | `MAIL_PASSWORD` | your Gmail app password |
   | `YOUTUBE_API_KEY` | your YouTube API key |
   | `GEMINI_API_KEY` | your Gemini API key |
   | `SPRING_PROFILES_ACTIVE` | `prod` |

5. Click **Create Web Service**. Render will build and deploy — takes 3–8 minutes first time.
6. Copy the URL Render gives you (e.g., `https://sportscard-app.onrender.com`). You'll need it for Step 4.

> **Render free tier:** 750 hours/month, spins down after 15 min of inactivity (cold start ~30 sec on next request). Free forever.

---

## Step 4 — Point Your Domain to Render via Cloudflare

1. In Cloudflare dashboard, click your domain → **DNS → Records**.
2. Click **Add record**:
   - **Type:** `CNAME`
   - **Name:** `@` (or `www`)
   - **Target:** your Render URL without `https://` (e.g., `sportscard-app.onrender.com`)
   - **Proxy status:** Proxied (orange cloud ON) — this enables Cloudflare SSL and CDN

3. Add a second record to handle `www` redirect:
   - **Type:** `CNAME`
   - **Name:** `www`
   - **Target:** `@`

4. In Render, go to your service → **Settings → Custom Domains** → Add your domain (e.g., `rgcards.com` and `www.rgcards.com`).

5. SSL is handled automatically by Cloudflare (free). No cert setup needed.

6. Wait 5–15 minutes for DNS to propagate. Test by visiting your domain in a browser.

---

## Step 5 — Configure a Production Spring Profile

Create `src/main/resources/application-prod.properties`:

```properties
# Database
spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
spring.jpa.hibernate.ddl-auto=update

# Mail
spring.mail.username=${MAIL_USERNAME}
spring.mail.password=${MAIL_PASSWORD}

# APIs
youtube.api.key=${YOUTUBE_API_KEY}
gemini.api.key=${GEMINI_API_KEY}

# Performance
spring.thymeleaf.cache=true

# Crawler — headless Chrome in container
webdriver.chrome.driver=${CHROMEDRIVER_PATH:/usr/bin/chromedriver}
```

Commit and push. Render will redeploy automatically.

---

## Step 6 — Verify the Live App

- [ ] Visit `https://yourdomain.com` — app loads
- [ ] Log in with your admin account
- [ ] Add a test card — saves correctly to Neon DB
- [ ] Trigger the Yahoo Auction crawler — runs without errors
- [ ] Check email notifications arrive
- [ ] Visit `https://yourdomain.com/swagger-ui.html` — **block this in prod** (see security note below)

---

## Security Notes Before Going Live

- [ ] Confirm `/swagger-ui.html` and `/v3/api-docs` are blocked or require admin login
- [ ] Confirm `/crawler/*` endpoints are not publicly accessible
- [ ] Confirm all secrets are only in Render environment variables, not in code
- [ ] Confirm Spring Security is active (login page appears on first visit)

See [`pre-deploy-checklist.md`](pre-deploy-checklist.md) for the full checklist.

---

## Cost Summary

| Service | Cost |
|---------|------|
| Domain (Cloudflare, .com) | ~$10/year |
| Cloudflare DNS + SSL + CDN | Free |
| Render Web Service | Free (750 hr/month) |
| Neon PostgreSQL | Free (0.5 GB) |
| **Total** | **~$10/year** |

---

## When You Outgrow the Free Tier

| Need | Upgrade path |
|------|-------------|
| No cold starts | Render Starter ($7/month) |
| More DB storage | Neon Launch ($19/month) or migrate to Railway |
| Better crawler reliability | Add a dedicated VPS (DigitalOcean $6/month) for Selenium |
