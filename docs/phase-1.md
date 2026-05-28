# Phase 1 — Pre-Deploy Hardening

Work to complete before the app can go online. Tasks 1–2 map to the blocking section of [`pre-deploy-checklist.md`](pre-deploy-checklist.md). Task 3 maps to the Crawler Strategy section. Task 4 is a blocking must-have.

## Tasks

### 1. Add Spring Security (Authentication)
- Add `spring-boot-starter-security` to `pom.xml`
- Create a login page (`login.html`) with email/password form
- Configure a `SecurityFilterChain` bean:
  - Permit `/login`, `/css/**`, `/js/**`, `/images/**`
  - Require authentication for all other routes
  - Call `.usernameParameter("email")` — Spring Security reads `username` by default; without this, all logins fail silently
- Back users by the local PostgreSQL DB (a `users` table with email, hashed password, role)
- Build a register page (`/register`) with email/password/role form
- Controlled by a feature flag in `application.properties`: `app.registration.enabled=false` (default off)
- When disabled, `/register` returns 403 or redirects to login
- Enable locally to seed accounts, then leave off for production
- When deploying to the cloud, do a one-time pg_dump of the local DB and restore it to the cloud DB

### 2. Data Ownership
- See [`data-ownership.md`](data-ownership.md) for the full strategy
- Add `user_id` FK to `Card`, `Transaction`, and `SearchKeyword`
- Write a Flyway migration that:
  1. Creates the `users` table
  2. Inserts the admin user with a known ID (e.g. `id = 1`)
  3. Adds `user_id` to the three root entities and backfills all existing rows to `id = 1`
- Scope all repository queries to the current logged-in user (`findByUserId(currentUserId)`)

### 3. Add Scheduled Cron Job for Crawler
- Create a scheduler class that calls `CrawlerService.getResultAsync()` on a schedule
- Enable scheduling with `@EnableScheduling` on the main class or a config class
- Hardcode the cron expression directly in the scheduler class (e.g. `@Scheduled(cron = "0 0 8 * * *")`)
- The existing `GET /crawler/search-all-async` endpoint stays untouched — manual trigger still works as before

### 4. Externalize Secrets
- Move all credentials out of `application.properties` into environment variables
- Keys to externalize: DB password, Gmail app password, YouTube API key, Gemini API key
- Replace values with `${ENV_VAR}` references in `application.properties`
- Confirm `.gitignore` excludes the real `application.properties` (only `.example` committed)

## Order of Work

- Task 2 depends on task 1 (needs the `users` table defined first)
- Tasks 3 and 4 are independent — can be done in any order, in parallel with 1 and 2

> **Note:** Removing `spring-boot-devtools` is not a hard requirement — the Maven plugin excludes it from the JAR automatically and devtools disables itself when running packaged. Good hygiene but not blocking.

> **Note:** User role enforcement is deferred to a later phase — only one user for now.
