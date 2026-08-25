# Phase 2 — Open Beta / User Testing

Open the app to a small group of external users for real-world testing and feedback.

## Tasks

### 1. Public Homepage
- [x] Make the homepage (`/`) accessible without login
  - Update `SecurityFilterChain` to permit `/` alongside existing public routes
  - Homepage should display useful content for non-logged-in visitors (**Content TBD**)

### 2. Banner Auth Panel
- [x] Add a login/register auth panel to the site banner (visible on all pages)
  - When logged out: show **Login** and **Register** buttons
  - **Register** button must respect the `app.registration.enabled` flag — hide when off
  - When logged in: show the **username** with a dropdown containing at minimum a **Logout** option

### 3. Google OAuth2 Login
- [x] Implement Google login alongside existing username/password auth
  - Register the app in Google Cloud Console (separate from the GCP VM — this is an OAuth2 credential)
  - Auto-create a local user record on first Google login
  - See [impl-google-oauth2.md](impl-google-oauth2.md) for detailed code changes

### 4. Redesign Login & Register Pages
- [x] Rewrite login and register pages with a cleaner, more polished design
  - Include a **Login with Google** button wired to the OAuth2 flow from Task 3
  - Should feel consistent with the public homepage and banner from Tasks 1 and 2

### 5. Per-User Usage Limits (Free Tier Protection)
- [ ] Enforce per-user soft caps to protect free-tier infrastructure, role-based via new `ROLE_TEST` tier
  - `ROLE_ADMIN`: unlimited
  - `ROLE_TEST`: 300 cards / 20 keywords / 30 AI calls per month
  - `ROLE_USER` (normal user): 50 cards / 5 keywords / 0 AI calls per month
  - See [impl-usage-limits.md](impl-usage-limits.md) for detailed code changes

### 6. Admin User Management Page
- [ ] Add a `/admin/users` page to manage external testers once the beta is open
  - List all users with role, enabled/disabled state, and per-user usage stats
  - Change a user's role, enable/disable an account, delete a user (cascades all their data)
  - Set or clear a per-user override for their monthly AI-call limit (Task 5)
  - Guard rails: an admin can't change their own role, disable, or delete themselves, and
    the last remaining `ROLE_ADMIN` can't be demoted or deleted
  - See [impl-admin-user-management.md](impl-admin-user-management.md) for detailed code changes

### 7. Runtime Registration Toggle
- [x] Make `app.registration.enabled` flippable from `/admin/settings` without a redeploy
  - Backed by a new `app_settings` key/value table, cached in memory, property kept as seed default
  - **Fixes an existing gap:** Google OAuth2 login auto-creates accounts regardless of the
    flag today, so registration is never truly closed — gate `CustomOAuth2UserService` too
  - See [impl-registration-toggle.md](impl-registration-toggle.md) for detailed code changes

### 8. Add Transaction Page Help Panel
- [ ] Add an info icon next to the "Add New Transaction" title that opens a tutorial block, closed by an `X`
  - Explains the five transaction types (`Break` / `Buy` / `Trade` / `Open` / `Giveaway`),
    which are documented nowhere in the UI today
  - Walks through every card column with a filled-in example, plus the copy/paste/clear/delete row actions
  - Clarifies that `amount` is the transaction total while `value` is per-card — the easiest
    thing for a new user to get backwards
  - Reuses the existing show/hide popup pattern already on this page (the note field)
  - See [impl-add-transaction-guide.md](impl-add-transaction-guide.md) for detailed code changes

## Order of Work

- Task 1 (public homepage) first — it's the entry point for all external users
- Task 2 (banner) builds on Task 1 and the existing auth setup from phase 1
- Task 3 (Google OAuth2) before Task 4 — the redesigned login page needs the Google button wired up
- Task 4 (login/register redesign) alongside or after Task 3
- Task 5 (usage limits) must be in place before opening to any external users
- Task 6 (admin user management) depends on Task 5's `UsageLimits`/`ROLE_TEST` work and
  should land around the same time — it's how an admin actually manages testers (adjust
  limits, disable, remove) once the beta is live
- Task 7's OAuth2 fix should land before Tasks 1/2 put a public homepage and visible auth
  panel in front of external users — until then, "registration off" doesn't hold. The
  toggle itself can follow whenever
- Task 8 (help panel) should land before external testers arrive — they'll hit the add
  transaction page with no idea what the transaction types mean. Independent of Tasks 5-7,
  so it can be done at any point
