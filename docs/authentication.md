# Authentication Plan

## Stack
- Spring Security 6 (comes with Spring Boot 3.x)
- BCrypt password hashing
- Thymeleaf + Spring Security integration (`thymeleaf-extras-springsecurity6`)
- Session-based authentication (no JWT needed for a server-rendered app)

## What Needs to Be Built

### 1. Dependency
Add to `pom.xml`:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
    <groupId>org.thymeleaf.extras</groupId>
    <artifactId>thymeleaf-extras-springsecurity6</artifactId>
</dependency>
```

### 2. User Entity
Create a `User` entity with fields:
- `id`
- `email` (used as login identifier — must be unique)
- `password` (BCrypt hashed — never store plain text)
- `role` (`ADMIN` or `USER`)
- `enabled`

Implement `UserDetails` interface on this entity or a separate `UserDetailsService`.
Configure `UserDetailsService` to load users by email instead of username.

### 3. Security Config
Create a `SecurityConfig` class (`@Configuration`, `@EnableWebSecurity`) with a `SecurityFilterChain` bean:
- Public routes: `/login`, `/css/**`, `/js/**`, static assets
- `ADMIN` only: `/card/saveCard`, `/card/addTransaction`, `/card/sellTransaction`, `/cardRest/deleteCard`, `/transactions/delete/**`, `/crawler/**`
- `USER` and `ADMIN`: all read-only pages
- Set custom login page: `/login`
- Set logout URL: `/logout`, redirect to `/login` after logout
- Enable CSRF (Spring Security enables it by default — do not disable)

### 4. Login Page
Create `templates/login.html`:
- Email and password form
- POST to `/login` (handled automatically by Spring Security)
- Show error message on failed login

### 5. Password Encoding
Register a `BCryptPasswordEncoder` bean in config. Use it when saving new users.

### 6. Thymeleaf Integration
Use Spring Security dialect in templates to show/hide UI elements by role:
```html
<!-- Show only to ADMIN -->
<div sec:authorize="hasRole('ADMIN')">
    <a href="/card/addNewCard">Add Card</a>
</div>

<!-- Show to any logged-in user -->
<div sec:authorize="isAuthenticated()">
    ...
</div>
```

## User Registration Strategy
- No self-registration for now — admin creates users manually or via a seeded SQL script.
- Can revisit if the app is opened to more users later.

## Password Reset Flow
Since the app already has a mail service (`spring-boot-starter-mail`), password reset can be added with minimal effort:

1. User clicks "Forgot password?" on the login page
2. User enters their email
3. App generates a secure random token, stores it in a `password_reset_tokens` table with an expiry (e.g. 15 minutes)
4. App sends an email with a reset link: `/reset-password?token=<token>`
5. User clicks the link, enters a new password
6. App validates the token (exists + not expired), updates the password, invalidates the token

### What needs to be built
- `PasswordResetToken` entity — fields: `id`, `token`, `user_id`, `expiresAt`
- `POST /forgot-password` — generates and emails the token
- `GET /reset-password?token=` — shows the new password form
- `POST /reset-password` — validates token and updates password
- Email template for the reset link

## Session Management
- Default Spring Security session handling is sufficient.
- Set session timeout in `application.properties`: `server.servlet.session.timeout=30m`
