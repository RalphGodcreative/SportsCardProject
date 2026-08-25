# Implementation: Runtime Registration Toggle

Turn `app.registration.enabled` from a deploy-time property into a setting an admin can
flip from `/admin/settings` while the app is running.

## Current State

The flag is read via `@Value("${app.registration.enabled:false}")` in two places:
- `AuthController.java:20` — gates `GET /register` and `POST /register` (both
  `return "redirect:/login"` when off)
- `GlobalModelAttributes.java:13` — exposes `registrationEnabled` to every template;
  `login.html:47` uses it to show/hide the "Don't have an account? Register" line

It's set to `false` in both `application-prod.properties:30` and
`application.properties.example:39`. Changing it means editing the properties file and
redeploying.

---

## 1. Fix First: Google OAuth2 Bypasses the Flag Entirely

**This is a real gap in the current system, independent of the toggle work.**
`CustomOAuth2UserService.loadUser` (`service/CustomOAuth2UserService.java:26-33`)
auto-creates a local `User` on first Google login via `orElseGet(...)` and never checks
`app.registration.enabled`. So with the flag `false` today, `/register` is closed but
**anyone with a Google account can still sign up** just by clicking "Login with Google" —
the account is created silently, with `ROLE_USER`.

Whatever the flag is called, it should mean "no new accounts." Gate the auto-create too:

```java
private final AppSettingService appSettingService;

User user = userRepository.findByEmailIgnoreCase(email)
        .orElseGet(() -> {
            if (!appSettingService.isRegistrationEnabled()) {
                throw new OAuth2AuthenticationException(
                        new OAuth2Error("registration_disabled"), "Registration is currently closed");
            }
            User newUser = new User();
            // ... unchanged ...
        });
```

Existing Google users still log in fine — the guard only fires on the create path. Spring
redirects a thrown `OAuth2AuthenticationException` back to `/login?error`; if a clearer
message is wanted, that needs a custom `AuthenticationFailureHandler` on the
`oauth2Login(...)` block in `SecurityConfig` (not covered here).

---

## 2. `app_settings` Table — Key/Value Store

A general key/value table rather than a one-off boolean column, so later toggles (there
are already candidates: crawler on/off, AI features on/off) don't each need a migration:

```sql
CREATE TABLE app_settings (
    key   VARCHAR(64)  PRIMARY KEY,
    value VARCHAR(255) NOT NULL
);

INSERT INTO app_settings (key, value) VALUES ('registration.enabled', 'false');
```

Pick the next migration version at implementation time (latest checked in is
`V8__replace_storages_with_tags.sql`, and both `docs/impl-usage-limits.md` and
`docs/impl-admin-user-management.md` also add migrations — order them by whichever lands
first).

Note `key` is a reserved word in some SQL dialects; it's fine in Postgres unquoted here,
but name the entity field `settingKey` to avoid any ambiguity in JPQL.

**`AppSetting.java`**:
```java
@Entity
@Table(name = "app_settings")
@Data
@NoArgsConstructor
public class AppSetting {

    @Id
    @Column(name = "key")
    private String settingKey;

    @Column(nullable = false)
    private String value;
}
```

**`AppSettingRepository.java`**: plain `JpaRepository<AppSetting, String>` — no custom
methods needed, `findById` is enough.

---

## 3. `AppSettingService.java` — Cached Read, Write-Through

`GlobalModelAttributes.registrationEnabled()` runs on **every page render**, so this must
not hit the DB per request. Cache in a `volatile` field, load once at startup, update on
write:

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class AppSettingService {

    public static final String REGISTRATION_ENABLED = "registration.enabled";

    private final AppSettingRepository appSettingRepository;

    @Value("${app.registration.enabled:false}")
    private boolean registrationEnabledDefault;

    private volatile boolean registrationEnabled;

    @PostConstruct
    void load() {
        registrationEnabled = appSettingRepository.findById(REGISTRATION_ENABLED)
                .map(s -> Boolean.parseBoolean(s.getValue()))
                .orElse(registrationEnabledDefault);
        log.info("Registration enabled: {}", registrationEnabled);
    }

    public boolean isRegistrationEnabled() {
        return registrationEnabled;
    }

    public void setRegistrationEnabled(boolean enabled) {
        AppSetting setting = appSettingRepository.findById(REGISTRATION_ENABLED)
                .orElseGet(() -> {
                    AppSetting s = new AppSetting();
                    s.setSettingKey(REGISTRATION_ENABLED);
                    return s;
                });
        setting.setValue(Boolean.toString(enabled));
        appSettingRepository.save(setting);
        registrationEnabled = enabled;
        log.info("Admin set registration enabled: {}", enabled);
    }
}
```

The `@Value` property is kept as the **seed default only** — used when the row is missing
(e.g. a fresh DB before the migration's `INSERT`, or local dev). Once the row exists, the
DB wins and the property is ignored. Worth a comment in both properties files saying so,
since a stale property that no longer controls anything is a trap for later.

**Caveat — single instance only:** the cache is per-JVM, so with more than one app instance
a toggle would only affect the instance that served the request until the others restart.
Fine today (one GCP e2-micro VM per `docs/GCP_MIGRATION.md`), but it's the thing that
breaks first if the app is ever scaled horizontally.

---

## 4. Replace the `@Value` Reads

**`AuthController.java`** — drop the `@Value` field, inject the service:

```java
private final AppSettingService appSettingService;

@GetMapping("/register")
public String registerPage() {
    if (!appSettingService.isRegistrationEnabled()) return "redirect:/login";
    return "register";
}
```
Same one-line change in `POST /register` (line 44). Keeping the check in **both** methods
matters — the POST guard is the real enforcement; the GET guard just avoids showing a dead
form.

**`GlobalModelAttributes.java`** — same swap, so `login.html:47` keeps working unchanged:

```java
private final AppSettingService appSettingService;

@ModelAttribute("registrationEnabled")
public boolean registrationEnabled() {
    return appSettingService.isRegistrationEnabled();
}
```
This class currently has no constructor injection (it only has the `@Value` field), so add
`@RequiredArgsConstructor` alongside its existing `@ControllerAdvice`.

---

## 5. `/admin/settings` Page

New `AdminSettingsController` in `controller/admin`, alongside `AdminRecommendationController`
and the planned `AdminUserController` (`docs/impl-admin-user-management.md`). `/admin/**` is
already `hasAuthority("ROLE_ADMIN")` in `SecurityConfig.java:46` — no new security rule:

```java
@Controller
@RequestMapping("/admin/settings")
@RequiredArgsConstructor
@Slf4j
public class AdminSettingsController {

    private final AppSettingService appSettingService;

    @GetMapping
    public String view(Model model) {
        model.addAttribute("registrationEnabled", appSettingService.isRegistrationEnabled());
        return "admin/settings";
    }

    @PostMapping("/registration")
    public String setRegistration(@RequestParam boolean enabled) {
        appSettingService.setRegistrationEnabled(enabled);
        return "redirect:/admin/settings";
    }
}
```

Note the `registrationEnabled` model attribute here is redundant with the global one from
`GlobalModelAttributes` (which is on every page anyway) — harmless, and being explicit
reads better on a page whose whole job is that setting.

**Template `admin/settings.html`**: own inline `<style>` block, matching how
`recommendationAdmin.html` and every other page in this app carry their own CSS. One row
per setting: label, current state, and a toggle form posting to `/admin/settings/registration`.
Built as a list from the start so the next toggle is a new row, not a redesign.

**Nav link** in `banner.html`, both desktop (near line 56) and mobile (near line 82), same
gate as the existing admin links:
```html
<a href="/admin/settings" class="user-menu-item" sec:authorize="hasAuthority('ROLE_ADMIN')">Settings</a>
```

---

## 6. Order of Work

1. Section 1 (OAuth2 gap) — worth doing on its own even if the toggle is deferred; it's a
   correctness fix to a flag that already exists and is currently half-effective.
2. Sections 2-4 (table, service, swap the reads) — no user-visible change yet.
3. Section 5 (admin page) — the payoff.
