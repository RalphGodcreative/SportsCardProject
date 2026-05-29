# Pre-Deployment Checklist

## Must-Haves (Blocking)

- [ ] **Authentication** — Add Spring Security with login page. All endpoints are currently public. See [`authentication.md`](authentication.md) for full plan.
- [ ] **User roles** — See [`user-roles.md`](user-roles.md) for role definitions. Restrict destructive endpoints to `ADMIN` only.
- [ ] **Data ownership** — Add `user_id` to `Card`, `Transaction`, `SearchKeyword`. See [`data-ownership.md`](data-ownership.md) for full plan and migration strategy.
- [ ] **Externalize secrets** — Replace hardcoded API keys and credentials in `application.properties` with environment variable references (`${ENV_VAR}`).
- [ ] **Remove spring-boot-devtools** — Development-only tool, must not run in production.

## Should-Haves

- [ ] **Production database** — Set up a hosted PostgreSQL instance (Railway, Neon, or Supabase). Current DB is localhost only.
- [ ] **Enable Thymeleaf cache** — Set `spring.thymeleaf.cache=true` in production profile for performance.
- [ ] **Error pages** — Add a basic `error.html` to replace Spring's default whitelabel error page.

## Nice-to-Haves

- [ ] **Input validation** — Add `@Valid` constraints on form submissions.
- [ ] **Password reset** — Email-based reset flow using existing mail service. See [`authentication.md`](authentication.md) for full plan.

## Crawler Strategy (Online Deployment)

- Close all public crawler endpoints (`/crawler/*`).
- Replace with a scheduled cron job running internally on the server.
- Crawler should never be triggerable by an external HTTP request in production.
