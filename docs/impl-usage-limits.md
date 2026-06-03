# Implementation: Per-User Usage Limits

## 1. `application.properties` — Externalize Limit Values

```properties
app.limits.max-cards=100
app.limits.max-keywords=20
app.limits.max-ai-calls-per-month=30
```

---

## 2. `User.java` — Add AI Call Tracking Fields

```java
@Column(nullable = false)
private int aiCallCount = 0;

@Column(nullable = true)
private String aiCallResetMonth; // stored as "YYYY-MM", e.g. "2026-06"
```

Write a Flyway migration:
```sql
ALTER TABLE users ADD COLUMN ai_call_count INTEGER NOT NULL DEFAULT 0;
ALTER TABLE users ADD COLUMN ai_call_reset_month VARCHAR(7);
```

---

## 3. `SearchKeywordRepository.java` — Add Count Query

```java
int countByUserId(Long userId);
```

---

## 4. `CardService.java` — Enforce Card Limit

Inject the limit value and check before saving in `saveTransactionWithCard`:

```java
@Value("${app.limits.max-cards}")
private int maxCards;

public void saveTransactionWithCard(TransactionWithCard transactionWithCard, Long userId) {
    int current = findCardsCount(userId);
    int incoming = transactionWithCard.getCards().size();
    if (current + incoming > maxCards) {
        throw new LimitExceededException("Card limit reached (" + maxCards + ")");
    }
    // ... existing save logic unchanged ...
}
```

---

## 5. Keyword Limit — Enforce in the Controller or a New `KeywordService`

Wherever a new `SearchKeyword` is saved, add a check:

```java
@Value("${app.limits.max-keywords}")
private int maxKeywords;

// before saving:
int current = searchKeywordRepository.countByUserId(userId);
if (current >= maxKeywords) {
    throw new LimitExceededException("Keyword limit reached (" + maxKeywords + ")");
}
```

---

## 6. `CardAiService.java` — Enforce Monthly AI Call Limit

```java
@Value("${app.limits.max-ai-calls-per-month}")
private int maxAiCalls;

private final UserRepository userRepository;

public String analyzeCardPotential(Card card, User user) throws JsonProcessingException {
    String currentMonth = YearMonth.now().toString(); // e.g. "2026-06"

    if (!currentMonth.equals(user.getAiCallResetMonth())) {
        user.setAiCallCount(0);
        user.setAiCallResetMonth(currentMonth);
    }

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

---

## 7. `LimitExceededException.java` — New Exception Class

```java
@ResponseStatus(HttpStatus.FORBIDDEN)
public class LimitExceededException extends RuntimeException {
    public LimitExceededException(String message) {
        super(message);
    }
}
```

---

## 8. UI — Show Usage vs Limit (Thymeleaf)

Pass counts to the model in the relevant controllers and display them in the UI:

```java
// in controller
model.addAttribute("cardCount", cardService.findCardsCount(userId));
model.addAttribute("maxCards", maxCards);
```

```html
<!-- in template -->
<small th:text="${cardCount + ' / ' + maxCards + ' cards'}"></small>
```
