# Implementation: Admin User Management Page

Today `/admin/recommendations` (`AdminRecommendationController`) is the only page under a
dedicated `controller/admin` package. `SecurityConfig.java:46` also restricts three other
routes to `ROLE_ADMIN` piecemeal (`/card/allCard`, `/crawler/search-all`,
`/crawler/search-all-async`), but those live in general controllers, not `/admin/**`. This
page follows the `AdminRecommendationController` pattern — new controller, `service` layer
call, page under `/admin/**` — since `/admin/**` is already `hasAuthority("ROLE_ADMIN")` in
`SecurityConfig`, so no new security rule is needed.

**Page:** `GET /admin/users` — lists every user with role, enabled/disabled state, and
per-user usage stats (cards / keywords / AI calls this month), each row with inline
controls for the actions below.

**Actions:**
1. Change a user's role (`ROLE_USER` / `ROLE_TEST` / `ROLE_ADMIN`)
2. Enable / disable a user's account
3. Set (or clear) a per-user override for their monthly AI-call limit
4. Delete a user and cascade-delete all their data

---

## 1. Safety Guards

Enforced server-side in `AdminUserService` (Section 6), not just hidden in the UI:
- An admin can't change their own role, disable their own account, or delete themselves.
- The last remaining `ROLE_ADMIN` account can't be demoted or deleted (would lock everyone
  out of `/admin/**`).

Both checks need `UserRepository.countByRole(String role)` (Section 5) and the acting
admin's own `User` (already available via `@AuthenticationPrincipal` in the controller,
same as everywhere else in this app).

---

## 2. `User.java` — New Fields

```java
@Column(nullable = false)
private boolean enabled = true;

@Column(name = "max_ai_calls_override")
private Integer maxAiCallsOverride; // null = use the role default from UsageLimits
```

Wire the existing `UserDetails.isEnabled()` override to the new field instead of the
hardcoded `true` it returns today:

```java
@Override public boolean isEnabled() { return enabled; }
```

`DaoAuthenticationProvider` (already configured in `SecurityConfig`) checks `isEnabled()`
on every login attempt automatically — no extra wiring needed for a disabled account to be
rejected at sign-in. **Known limitation:** this is only checked at login, not on every
request, so an already-authenticated session isn't killed immediately when an admin
disables the account — it just can't log back in once the session ends. Sessions last up
to 2 days (`server.servlet.session.timeout=2d` in `application-prod.properties`), so a
disabled user's existing session could remain active that long. Not addressed here; would
need a session-invalidation filter if that gap matters.

Migration (pick the next version number at implementation time):
```sql
ALTER TABLE users ADD COLUMN enabled BOOLEAN NOT NULL DEFAULT true;
ALTER TABLE users ADD COLUMN max_ai_calls_override INTEGER;
```

---

## 3. `UsageLimits.java` — Per-User AI-Call Override

Extends the resolver from `docs/impl-usage-limits.md` Section 2. Only `maxAiCalls` gets an
override — cards and keywords stay strictly role-based:

```java
public int maxAiCalls(User user) {
    if (user.getMaxAiCallsOverride() != null) {
        return user.getMaxAiCallsOverride();
    }
    return forRole(user, testMaxAiCalls, userMaxAiCalls);
}
```

Since the check in `CardAiService` is `user.getAiCallCount() >= maxAiCalls`, raising a
user's override immediately un-blocks them for the rest of the current month — no need to
also reset `aiCallCount` when granting extra calls.

---

## 4. Repository Additions

**`UserRepository.java`** — needed for the last-admin guard (Section 1):
```java
long countByRole(String role);
```

**`TransactionRepository.java`** — needed for the cascade delete (Section 6); doesn't
exist yet (only `getTransactionsSortByDate`, `countByUserId`, and a date-range query):
```java
List<Transaction> findByUserId(Long userId);
```

`SearchKeywordRepository.countByUserId` and `TagRepository.findByUserIdOrderByIdAsc` /
`CardRepository.findByUserIdOrderByIdDesc` already exist (the former is also added by
`docs/impl-usage-limits.md` Section 4 — same method, don't add it twice).

---

## 5. Cascade Delete — Why It's Not a Single `deleteById`

None of `cards.user_id`, `transactions.user_id`, `tags.user_id`, or
`search_keywords.user_id` have `ON DELETE CASCADE` (checked every migration — all four are
plain `FOREIGN KEY (user_id) REFERENCES users(id)`), so deleting a `users` row directly
would fail on the first FK it hits. `transaction_infos.card_id` / `.transaction_id` have no
FK at all (per `V1__baseline_schema.sql`), so those need explicit cleanup too, same as the
existing `CardService.deleteCard` already does per-card. `card_tags` is the one exception —
it has `ON DELETE CASCADE` to both `cards` and `tags` (`V8__replace_storages_with_tags.sql`),
so deleting a `Card` or `Tag` row cleans up its `card_tags` rows automatically; nothing to
do for that table explicitly.

Order: `transaction_infos` (via each card) → `cards` → `transactions` → `tags` →
`search_keywords` → `users`.

---

## 6. `AdminUserService.java` — New Service

New class in the `service` package (there's no general `UserService` yet — `UserController`
talks to `UserRepository` directly for its own narrower concern of editing your own profile;
this is enough new logic to warrant its own service):

```java
@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;
    private final CardRepository cardRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionInfoRepository transactionInfoRepository;
    private final TagRepository tagRepository;
    private final SearchKeywordRepository searchKeywordRepository;

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public void changeRole(Long targetId, String newRole, User actingAdmin) {
        requireNotSelf(targetId, actingAdmin, "change your own role");
        User target = userRepository.findById(targetId).orElseThrow();
        requireNotLastAdmin(target, newRole);
        target.setRole(newRole);
        userRepository.save(target);
    }

    public void setEnabled(Long targetId, boolean enabled, User actingAdmin) {
        requireNotSelf(targetId, actingAdmin, "disable your own account");
        User target = userRepository.findById(targetId).orElseThrow();
        target.setEnabled(enabled);
        userRepository.save(target);
    }

    public void setAiCallLimitOverride(Long targetId, Integer override) {
        User target = userRepository.findById(targetId).orElseThrow();
        target.setMaxAiCallsOverride(override); // null clears it
        userRepository.save(target);
    }

    @Transactional
    public void deleteUserAndAllData(Long targetId, User actingAdmin) {
        requireNotSelf(targetId, actingAdmin, "delete your own account");
        User target = userRepository.findById(targetId).orElseThrow();
        requireNotLastAdmin(target, "ROLE_USER"); // any non-admin role trips the same guard

        List<Card> cards = cardRepository.findByUserIdOrderByIdDesc(targetId);
        for (Card card : cards) {
            transactionInfoRepository.deleteAll(transactionInfoRepository.findByCardId(card.getId()));
        }
        cardRepository.deleteAll(cards); // card_tags cascades automatically (DB-level ON DELETE CASCADE)
        transactionRepository.deleteAll(transactionRepository.findByUserId(targetId));
        tagRepository.deleteAll(tagRepository.findByUserIdOrderByIdAsc(targetId));
        searchKeywordRepository.deleteAll(searchKeywordRepository.findByUserId(targetId));
        userRepository.deleteById(targetId);
    }

    private void requireNotSelf(Long targetId, User actingAdmin, String action) {
        if (targetId.equals(actingAdmin.getId())) {
            throw new IllegalStateException("Cannot " + action);
        }
    }

    private void requireNotLastAdmin(User target, String newRole) {
        boolean losingAdminStatus = "ROLE_ADMIN".equals(target.getRole()) && !"ROLE_ADMIN".equals(newRole);
        if (losingAdminStatus && userRepository.countByRole("ROLE_ADMIN") <= 1) {
            throw new IllegalStateException("Cannot remove the last admin");
        }
    }
}
```

---

## 7. `AdminUserController.java` — New Controller

New class in `controller/admin`, alongside `AdminRecommendationController`. There's still
no `@ControllerAdvice` anywhere in this app (same gap noted in
`docs/impl-usage-limits.md` Section 8) — each action catches `IllegalStateException` itself
and redirects with a flash message via `RedirectAttributes.addFlashAttribute`, which isn't
used anywhere else in the codebase yet (existing controllers that render forms just add
`model.addAttribute("error", ...)` and re-render the same view — see
`UserController.edit` — rather than redirecting); flash attributes are the right tool here
specifically because these actions redirect back to the same list page rather than
re-rendering in place. Same overall shape as `AdminRecommendationController`'s
redirect-after-POST pattern:

```java
@Controller
@RequestMapping("/admin/users")
@RequiredArgsConstructor
@Slf4j
public class AdminUserController {

    private final AdminUserService adminUserService;
    private final CardService cardService;
    private final SearchKeywordRepository searchKeywordRepository;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("users", adminUserService.findAll());
        // per-row stats — cardService.findCardsCount(user.getId()), searchKeywordRepository.countByUserId(user.getId()),
        // user.getAiCallCount() — resolved in the template or pre-built into a small DTO, admin's choice
        return "admin/users";
    }

    @PostMapping("/{id}/role")
    public String changeRole(@PathVariable Long id, @RequestParam String role,
                              @AuthenticationPrincipal User currentUser,
                              RedirectAttributes redirectAttributes) {
        try {
            adminUserService.changeRole(id, role, currentUser);
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/enabled")
    public String setEnabled(@PathVariable Long id, @RequestParam boolean enabled,
                              @AuthenticationPrincipal User currentUser,
                              RedirectAttributes redirectAttributes) {
        try {
            adminUserService.setEnabled(id, enabled, currentUser);
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/ai-limit")
    public String setAiLimit(@PathVariable Long id,
                              @RequestParam(required = false) Integer maxAiCalls) {
        adminUserService.setAiCallLimitOverride(id, maxAiCalls); // blank field -> null -> clears override
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, @AuthenticationPrincipal User currentUser,
                          RedirectAttributes redirectAttributes) {
        try {
            adminUserService.deleteUserAndAllData(id, currentUser);
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }
}
```

---

## 8. Template — `admin/users.html`

New template, own inline `<style>` block (this app doesn't share component CSS across
pages — `recommendationAdmin.html` and every other page each carries their own `<style>`).
One row per user: username/email, a role `<select>` + small submit form, an enabled
toggle, cards/keywords/AI-calls-this-month stats, a number input for the AI-call override
(empty = cleared/default), and a delete button behind a JS confirm dialog. Render `error`
(the flash attribute from Section 7) at the top if present. Hide/disable the role, enable,
and delete controls on the admin's own row (`th:if="${user.id != #authentication.principal.id}"`
or equivalent) — client-side reinforcement of the server-side guard in Section 1, not a
replacement for it.

---

## 9. Nav Link — `banner.html`

Add alongside the existing `/admin/recommendations` link, both desktop (line 56) and
mobile (line 82) nav, same `sec:authorize="hasAuthority('ROLE_ADMIN')"` gate:

```html
<a href="/admin/users" class="user-menu-item" sec:authorize="hasAuthority('ROLE_ADMIN')">Users</a>
```

```html
<a href="/admin/users" class="mobile-sub" sec:authorize="hasAuthority('ROLE_ADMIN')">Users</a>
```
