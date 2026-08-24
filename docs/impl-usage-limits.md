# Implementation: Per-User Usage Limits

Limits are role-based. Only two roles exist today — `ROLE_USER` (assigned at signup,
both in `AuthController.java:58` and `CustomOAuth2UserService.java:31`) and `ROLE_ADMIN`
(never auto-granted; promoted manually via SQL, see `docs/impl-player-recommendations.md`).
This adds a third: `ROLE_TEST`, for internal/beta accounts that need higher caps than a
normal user without going fully unlimited. Nothing grants `ROLE_TEST` automatically either
— promote an account manually the same way `ROLE_ADMIN` is promoted:

```sql
UPDATE users SET role = 'ROLE_TEST' WHERE id = <id>;
```

| Role         | Cards | Keywords | AI calls / month |
|--------------|-------|----------|-------------------|
| `ROLE_ADMIN` | unlimited | unlimited | unlimited |
| `ROLE_TEST`  | 300   | 20       | 30                |
| `ROLE_USER`  | 50    | 5        | 0                 |

(`ROLE_USER` getting `0` AI calls means the check blocks immediately — no special-casing
needed, see Section 7.)

---

## 1. Externalize Limit Values

There is no checked-in `application.properties` — add the block below to both
`src/main/resources/application.properties.example` (local-dev template) and
`src/main/resources/application-prod.properties` (real prod config). `ROLE_ADMIN` has no
properties — it's handled in code as "no limit," not as a very large configured number:

```properties
app.limits.test.max-cards=300
app.limits.test.max-keywords=20
app.limits.test.max-ai-calls-per-month=30

app.limits.user.max-cards=50
app.limits.user.max-keywords=5
app.limits.user.max-ai-calls-per-month=0
```

---

## 2. `UsageLimits.java` — Role-Aware Limit Resolver + Monthly Reset

New `@Component` (e.g. `service` package) that centralizes the role → limit mapping, so
none of the three enforcement sites (Sections 5-7) need their own role branching. It also
owns the monthly reset operation (Section 9) — one place for everything "usage limits":

```java
@Component
@RequiredArgsConstructor
public class UsageLimits {

    private final UserRepository userRepository;

    @Value("${app.limits.test.max-cards}")
    private int testMaxCards;
    @Value("${app.limits.test.max-keywords}")
    private int testMaxKeywords;
    @Value("${app.limits.test.max-ai-calls-per-month}")
    private int testMaxAiCalls;

    @Value("${app.limits.user.max-cards}")
    private int userMaxCards;
    @Value("${app.limits.user.max-keywords}")
    private int userMaxKeywords;
    @Value("${app.limits.user.max-ai-calls-per-month}")
    private int userMaxAiCalls;

    public int maxCards(User user)    { return forRole(user, testMaxCards, userMaxCards); }
    public int maxKeywords(User user) { return forRole(user, testMaxKeywords, userMaxKeywords); }
    public int maxAiCalls(User user)  { return forRole(user, testMaxAiCalls, userMaxAiCalls); }

    private int forRole(User user, int testValue, int userValue) {
        return switch (user.getRole()) {
            case "ROLE_ADMIN" -> Integer.MAX_VALUE;
            case "ROLE_TEST" -> testValue;
            default -> userValue;
        };
    }

    @Transactional
    public void resetAllAiCallCounts() {
        userRepository.resetAllAiCallCounts();
    }
}
```

Because `ROLE_ADMIN` resolves to `Integer.MAX_VALUE`, the existing `current + incoming >
maxCards`-style checks in Sections 5-7 never trip for admins — no `if (isAdmin) skip`
branch needed at each call site.

---

## 3. `User.java` — Add AI Call Tracking Field

The count resets via a scheduled job (Section 9), not by comparing against a stored
month — so only one new column is needed, not two:

```java
@Column(nullable = false)
private int aiCallCount = 0;
```

Write a new Flyway migration (latest checked in is `V8__replace_storages_with_tags.sql`,
so pick the next version number at implementation time) in `src/main/resources/db/migration/`:
```sql
ALTER TABLE users ADD COLUMN ai_call_count INTEGER NOT NULL DEFAULT 0;
```

---

## 4. `SearchKeywordRepository.java` — Add Count Query

```java
int countByUserId(Long userId);
```

---

## 5. `CardService.java` — Enforce Card Limit

`saveTransactionWithCard` currently takes `Long userId`; change it to take the full
`User` (needed to resolve the role-based limit via `UsageLimits`). The one call site,
`CardController.saveTransaction` (`cardService.saveTransactionWithCard(transactionWithCard,
currentUser.getId())`), already has `currentUser` available via `@AuthenticationPrincipal`
— just pass `currentUser` instead of `currentUser.getId()`.

`CardService` currently uses `@Autowired` field injection throughout, not constructor
injection — add `usageLimits` the same way to stay consistent with the rest of the class:

```java
@Autowired
private UsageLimits usageLimits;

public void saveTransactionWithCard(TransactionWithCard transactionWithCard, User user) {
    int maxCards = usageLimits.maxCards(user);
    int current = findCardsCount(user.getId());
    int incoming = transactionWithCard.getCards().size();
    if (current + incoming > maxCards) {
        throw new LimitExceededException("Card limit reached (" + maxCards + ")");
    }
    // ... existing save logic unchanged (still uses user.getId() internally) ...
}
```

---

## 6. Keyword Limit — Enforce in `CrawlerService.addKeyword`

The single place a new `SearchKeyword` gets created is
`CrawlerService.addKeyword(String keyword, Long userId)`
(`src/main/java/RGcards/SportsCardProject/service/CrawlerService.java`). Same as Section 5,
change the param from `Long userId` to `User user` — the call site,
`CrawlerController.addKeyword` (`crawlerService.addKeyword(keyword, currentUser.getId())`),
already has `currentUser` available. Add the check right after the existing
duplicate-keyword check, before the new `SearchKeyword` is built:

```java
@Autowired
private UsageLimits usageLimits;

public SearchKeyword addKeyword(String keyword, User user) {
    if (searchKeywordRepository.findByKeywordAndUserId(keyword, user.getId()) != null) {
        return null;
    }
    int maxKeywords = usageLimits.maxKeywords(user);
    int current = searchKeywordRepository.countByUserId(user.getId());
    if (current >= maxKeywords) {
        throw new LimitExceededException("Keyword limit reached (" + maxKeywords + ")");
    }
    // ... existing save logic unchanged (still uses user.getId() internally) ...
}
```

---

## 7. `CardAiService.java` — Enforce Monthly AI Call Limit

Current signature is `analyzeCardPotential(Card card)` with no `User` param. Adding
`User user` also requires updating the one call site,
`CardRestController.getCardPotential` (`cardAiService.analyzeCardPotential(card)` →
`analyzeCardPotential(card, currentUser)`), which already has `currentUser` available
via `@AuthenticationPrincipal`.

```java
private final UsageLimits usageLimits;
private final UserRepository userRepository;

public String analyzeCardPotential(Card card, User user) throws JsonProcessingException {
    int maxAiCalls = usageLimits.maxAiCalls(user);
    if (user.getAiCallCount() >= maxAiCalls) {
        throw new LimitExceededException("Monthly AI call limit reached (" + maxAiCalls + ")");
    }

    String result = geminiService.generateContent(buildPrompt(card), "gemini-2.5-flash", true);

    user.setAiCallCount(user.getAiCallCount() + 1);
    userRepository.save(user);

    return result;
}
```

Extract the existing prompt into a private `buildPrompt(Card card)` method to keep it clean.
`usageLimits` and `userRepository` slot in as `private final` fields the same way
`geminiService` already is — this class uses `@RequiredArgsConstructor`, so both get
constructor-injected automatically.

For a `ROLE_USER` account, `maxAiCalls` resolves to `0` and `aiCallCount` starts at `0`,
so `0 >= 0` is true and the very first call is blocked — no separate "0 means disabled"
branch needed.

---

## 8. `LimitExceededException.java` — New Exception Class

```java
@ResponseStatus(HttpStatus.FORBIDDEN)
public class LimitExceededException extends RuntimeException {
    public LimitExceededException(String message) {
        super(message);
    }
}
```

There is no `@ControllerAdvice`/`@ExceptionHandler` anywhere in the codebase yet. The
`@ResponseStatus` alone is enough for the REST endpoints (`CardRestController`, which
already returns a JSON error body via its own try/catch), but `CardController.saveTransaction`
(`src/main/java/RGcards/SportsCardProject/controller/CardController.java`, calls
`cardService.saveTransactionWithCard(...)`) is a plain form POST that just redirects —
an uncaught `LimitExceededException` there would surface as Spring's default whitelabel
error page. Wrap that call in a try/catch and pass the message back via a flash attribute
(or similar) if a friendlier message on the form is wanted.

---

## 9. Monthly Reset — Scheduled Job + Admin Fallback

One reset function, two callers: a monthly cron job (primary) and an admin-triggered
endpoint (fallback if the cron misses a run — e.g. a deploy landing right at midnight).
Loosely coupled the same way `CrawlerScheduler` triggers `CrawlerService`, and the admin
endpoint mirrors the existing `CrawlerController.resetAll`-style manual-trigger pattern.

**`UserRepository.java`** — bulk reset query (the one place both callers go through, via
`UsageLimits.resetAllAiCallCounts()` in Section 2):

```java
@Modifying
@Query("UPDATE User u SET u.aiCallCount = 0")
void resetAllAiCallCounts();
```

**`AiCallResetScheduler.java`** — new class in the `scheduler` package, same shape as the
existing `CrawlerScheduler` (`@EnableScheduling` is already on
`SportsCardProjectApplication`, no new config needed):

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class AiCallResetScheduler {

    private final UsageLimits usageLimits;

    @Scheduled(cron = "0 0 0 1 * *")
    public void scheduledReset() {
        log.info("Scheduled AI call count reset started");
        usageLimits.resetAllAiCallCounts();
        log.info("Scheduled AI call count reset finished");
    }
}
```

No timezone offset to account for: `SportsCardProjectApplication.main` calls
`TimeZone.setDefault(TimeZone.getTimeZone("Asia/Taipei"))` before `SpringApplication.run(...)`,
and `@Scheduled(cron=...)` resolves against the JVM default zone unless a `zone=` attribute
overrides it — so `"0 0 0 1 * *"` fires at midnight Taipei time on the 1st, same as
`CrawlerScheduler`'s cron resolves against Taipei time. (`docs/GCP_MIGRATION.md` says
`CrawlerScheduler`'s `"0 0 8 * * *"` fires at 9 PM Taiwan time, which doesn't match 8 AM —
that line is stale/wrong, not evidence of a real offset.)

**`AdminUsageLimitsController.java`** — new class in `controller/admin`, alongside the
existing `AdminRecommendationController`. `/admin/**` is already restricted to
`hasAuthority("ROLE_ADMIN")` in `SecurityConfig.java:46`, so no new security rule is
needed:

```java
@Controller
@RequestMapping("/admin/usage-limits")
@RequiredArgsConstructor
@Slf4j
public class AdminUsageLimitsController {

    private final UsageLimits usageLimits;

    @PostMapping("/reset-ai-calls")
    @ResponseBody
    public Boolean resetAiCalls() {
        try {
            log.info("Admin manually triggered AI call count reset");
            usageLimits.resetAllAiCallCounts();
            return true;
        } catch (Exception e) {
            log.error("Manual AI call count reset failed", e);
            return false;
        }
    }
}
```

---

## 10. UI — Show Usage vs Limit (Thymeleaf)

`CardController` already adds a `cardCounts` (plural) model attribute — populated via
`cardService.findCardsCount(...)` — in both `allCard` (`/card/allCard`) and
`allCardsByPage` (`/card/cards`), and both `allCard.html:139` and `pagingAllCard.html:187`
already render `total cards: {{cardCounts}}`. Reuse that existing attribute rather than
introducing a new `cardCount` one — just add `maxCards` alongside it. Since `ROLE_ADMIN`
resolves to `Integer.MAX_VALUE` (Section 2), guard the display so admins see "unlimited"
instead of that raw number:

```java
// in CardController, alongside the existing cardCounts line
model.addAttribute("maxCards", usageLimits.maxCards(currentUser));
```

```html
<!-- allCard.html / pagingAllCard.html, replacing the existing "total cards" line -->
<span th:if="${maxCards == T(Integer).MAX_VALUE}">total cards: <span th:text="${cardCounts}"></span></span>
<span th:unless="${maxCards == T(Integer).MAX_VALUE}">total cards: <span th:text="${cardCounts + ' / ' + maxCards}"></span></span>
```
