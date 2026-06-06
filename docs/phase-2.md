# Phase 2 — Open Beta / User Testing

Open the app to a small group of external users for real-world testing and feedback.

## Tasks

### 1. Public Homepage
- [ ] Make the homepage (`/`) accessible without login
  - Update `SecurityFilterChain` to permit `/` alongside existing public routes
  - Homepage should display useful content for non-logged-in visitors (**Content TBD**)

### 2. Banner Auth Panel
- [ ] Add a login/register auth panel to the site banner (visible on all pages)
  - When logged out: show **Login** and **Register** buttons
  - **Register** button must respect the `app.registration.enabled` flag — hide when off
  - When logged in: show the **username** with a dropdown containing at minimum a **Logout** option

### 3. Google OAuth2 Login
- [ ] Implement Google login alongside existing username/password auth
  - Register the app in Google Cloud Console (separate from the GCP VM — this is an OAuth2 credential)
  - Auto-create a local user record on first Google login
  - See [impl-google-oauth2.md](impl-google-oauth2.md) for detailed code changes

### 4. Redesign Login & Register Pages
- [ ] Rewrite login and register pages with a cleaner, more polished design
  - Include a **Login with Google** button wired to the OAuth2 flow from Task 3
  - Should feel consistent with the public homepage and banner from Tasks 1 and 2

### 5. Per-User Usage Limits (Free Tier Protection)
- [ ] Enforce per-user soft caps to protect free-tier infrastructure
  - Cards per user: **100**
  - SearchKeywords per user: **20**
  - AI feature calls per user per month: **30** (`analyzeCardPotential`)
  - See [impl-usage-limits.md](impl-usage-limits.md) for detailed code changes

## Order of Work

- Task 1 (public homepage) first — it's the entry point for all external users
- Task 2 (banner) builds on Task 1 and the existing auth setup from phase 1
- Task 3 (Google OAuth2) before Task 4 — the redesigned login page needs the Google button wired up
- Task 4 (login/register redesign) alongside or after Task 3
- Task 5 (usage limits) must be in place before opening to any external users
