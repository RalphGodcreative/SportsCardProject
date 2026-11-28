# Implementation Plan: Sell Signals

## Summary

A logged-in user opens **Sell Signals** and gets a short ranked list of the cards they own
that look like a good time to sell, each with:

- what they paid (cost basis from their transactions),
- what the card is **currently listed for** on eBay (asking prices, not sold prices),
- a sample of the listings used, so they can check the comps,
- a 1–2 line AI reason grounded in web search (hot streak, award race, playoff run,
  injury risk, hype fading).

The ranking is computed **in code from real numbers**. The AI only explains the top few.
It never supplies prices.

| Route | Who | What |
|---|---|---|
| `GET /sell-signals` | `ROLE_ADMIN` (v1) | latest saved run for the current user (or an empty state with a "Run" button) |
| `POST /sell-signals/run` | `ROLE_ADMIN` (v1) | runs a new scan, saves it, redirects to `GET /sell-signals` |

### Admin-only first

v1 is **admin-only** so the match quality and costs can be checked on real data before
members see it. The route stays at `/sell-signals` (not `/admin/...`), so opening it later is
a one-line change rather than a URL move:

```java
// SecurityConfig.java, add "/sell-signals/**" to the existing admin matcher
.requestMatchers("/admin/**", "/card/allCard", "/crawler/search-all",
                 "/crawler/search-all-async", "/sell-signals/**").hasAuthority("ROLE_ADMIN")
```

The nav link is wrapped in `sec:authorize="hasAuthority('ROLE_ADMIN')"` (same pattern as the
admin entry in `banner.html`). All data stays **per-user** (`user_id` on runs, scoped
queries), so when it opens up, each member only sees their own cards and nothing in the schema
changes. See §7 for the steps to open it up.

### Why prices come from asking listings

eBay retired the Finding API (`findCompletedItems`) in Feb 2025, so `EbayFindingService`
returns nothing, and nothing calls it. The sold-data replacement (Marketplace Insights API)
is gated. The only working eBay source is `EbayBrowseService.searchItems`, which returns
**active listings**. v1 uses those and **labels them as asking prices everywhere in the UI**.
Real sold data comes later from the Yahoo TW sold-price history (see *Later*).

### Decisions

| Question | Decision |
|---|---|
| Price source | eBay Browse API active listings (`EbayBrowseService`). Shown as "listed from", never as "worth" or "sold for". |
| Which price to use | The **25th percentile** of matched asking prices (`low_ask`), not the median. Asking prices skew high because overpriced listings sit unsold, so the low end is closer to what actually sells. Median is stored and shown too. |
| Which cards are scanned | Cards the user still owns: no `transaction_infos` row with `move = 'OUT'`. **`PC` cards are excluded** (personal collection means "not for sale"). `INVESTMENT` cards go first, then `NEUTRAL`. |
| Cards per run | Capped at **20** (`MAX_CARDS_PER_RUN`). If a user has more, take `INVESTMENT` first, then the highest cost basis. Bounds eBay calls and request time. |
| Cost basis | From the card's `IN` transaction: `transactions.amount / (number of cards in that transaction)`. A bulk buy is split evenly, which is approximate and shown as "≈". No `IN` transaction means cost unknown (see next row). |
| Cards with unknown cost | Still priced and shown in a separate "No cost data" list, not ranked. |
| Currency | eBay prices are USD. Transaction amounts are **TWD** (confirmed 2026-11-25). Convert using an admin-editable `sell_signals.usd_to_twd` value in `app_settings` (default `32.0`), saved on each run so old runs stay reproducible. No live FX API in v1. |
| Minimum data | A card needs **≥ 3 matched listings** to be ranked. Fewer means "Not enough listings" and the card is not ranked. |
| Score | `gain = low_ask_twd / cost - 1`, then `score = gain * min(1, listings / 5)`. Thin markets are discounted. Pure function, unit-tested. |
| AI | **One** Gemini call per run (`gemini-2.5-flash`, `useSearch = true`) for the **top 5** ranked cards **that are above cost** (a card at a loss isn't a sell candidate). No such cards means `SKIPPED`. The prompt passes in the numbers and asks only for reasons. |
| AI usage limit | One run = one AI call against `users.ai_call_count` / `UsageLimits.maxAiCalls`, same check-and-increment as `CardAiService`. At the limit, the run still works but saves no AI reasons (`ai_status = SKIPPED`). |
| Cooldown | One run per user per **24 h** for non-admins. **Admins bypass it**, which is all users in v1, so you can re-run freely while tuning the match filters. Within the cooldown, `POST /run` just redirects to the latest run with a flash message. Bounds eBay quota once members get access (Browse default is 5,000 calls/day app-wide; 20 cards × users adds up). |
| History | Every run is saved and nothing is overwritten (same approach as `recommendations`). The page shows the latest run. Older runs are kept for later trend features. |
| Failure handling | One card's eBay call failing skips that card (logged). A Gemini failure still saves the ranking, with `ai_status = FAILED`. The page never 500s. |
| Wording | "Worth considering selling", never "Sell now". A fixed footer reads: *Based on current eBay asking prices, not completed sales. Not financial advice.* |

---

## 0. Cleanup first

Delete the dead sold-comps code so nobody builds on it:

- `service/EbayFindingService.java`
- `dto/EbaySoldItemSummary.java`

Grep for any remaining references (none found in `src/main` as of 2026-11-25). Keep
`ebay.client.id`, which `EbayTokenService` still uses.

---

## 1. Schema

### `V20__add_sell_signals_tables.sql` — New Flyway Migration

(Latest existing migration is `V19__add_sport_to_sets.sql`; re-check the next free number at
implementation time.)

```sql
CREATE TABLE sell_signal_runs (
    id                BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    user_id           BIGINT       NOT NULL,
    created_at        TIMESTAMP    NOT NULL DEFAULT now(),
    cards_considered  INTEGER      NOT NULL,
    usd_to_twd        NUMERIC(10,4) NOT NULL,
    ai_status         VARCHAR(10)  NOT NULL,
    CONSTRAINT chk_sell_signal_ai_status CHECK (ai_status IN ('OK', 'FAILED', 'SKIPPED')),
    CONSTRAINT fk_sell_signal_run_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_sell_signal_runs_user_created ON sell_signal_runs (user_id, created_at DESC);

CREATE TABLE sell_signal_items (
    id               BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    run_id           BIGINT       NOT NULL,
    card_id          INTEGER      NOT NULL,
    rank             SMALLINT,              -- null = not ranked (no cost / too few listings)
    status           VARCHAR(20)  NOT NULL,
    cost_basis_twd   NUMERIC,
    listing_count    INTEGER      NOT NULL,
    low_ask_usd      NUMERIC,
    median_ask_usd   NUMERIC,
    gain_pct         NUMERIC,
    score            NUMERIC,
    ebay_query       VARCHAR(255) NOT NULL,
    sample_listings  VARCHAR(4000),         -- JSON array, max 3: {title, price, url}
    ai_reason        VARCHAR(2000),
    CONSTRAINT chk_sell_signal_item_status
        CHECK (status IN ('RANKED', 'NO_COST', 'TOO_FEW_LISTINGS', 'EBAY_ERROR')),
    CONSTRAINT fk_sell_signal_item_run  FOREIGN KEY (run_id)  REFERENCES sell_signal_runs(id) ON DELETE CASCADE,
    CONSTRAINT fk_sell_signal_item_card FOREIGN KEY (card_id) REFERENCES cards(id) ON DELETE CASCADE
);

CREATE INDEX idx_sell_signal_items_run  ON sell_signal_items (run_id);
CREATE INDEX idx_sell_signal_items_card ON sell_signal_items (card_id);
```

Notes:

- `cards.id` is `int` in `Card.java`, so `card_id` is `INTEGER`. `users.id` is `BIGINT`.
- `ON DELETE CASCADE` on both: deleting a card or a user removes their signals. Check
  `AccountDeletionService`. If it deletes rows manually in order, add the two tables there
  before `cards` / `users`, or rely on the cascade.
- `sample_listings` is `VARCHAR` holding JSON, not `jsonb`, so it maps to a plain `String` under
  `ddl-auto=validate` without a custom Hibernate type. `VARCHAR` rather than `TEXT` matches the
  rest of the schema (`feedback.content`); the service drops samples until the JSON fits.
- `rank` is a reserved-ish word in some SQL dialects. It is fine in Postgres, but quote it or
  rename it to `rank_no` if Hibernate complains.

### `app_settings` row

Add key `sell_signals.usd_to_twd` (default `32.0`), read through the same mechanism
`UsageLimits` uses. Either insert it in the migration or fall back to the default in code when
it's missing (match whatever `UsageLimits` does).

---

## 2. Backend

### 2.1 Enums

- `enums/SellSignalAiStatus.java`: `OK, FAILED, SKIPPED`
- `enums/SellSignalItemStatus.java`: `RANKED, NO_COST, TOO_FEW_LISTINGS, EBAY_ERROR`

### 2.2 Entities + repositories

- `entity/SellSignalRun.java` with a `@OneToMany(mappedBy = "run", cascade = ALL)
  List<SellSignalItem> items` ordered by `rank` (nulls last).
- `entity/SellSignalItem.java` with `@ManyToOne SellSignalRun run`, `int cardId` (plain column,
  card loaded separately and scoped by user).
- `dao/SellSignalRunRepository`:
  - `Optional<SellSignalRun> findFirstByUserIdOrderByCreatedAtDesc(Long userId)`. Always
    scoped by `userId`; never add an unscoped finder (see the cross-user risk in the project
    overview).

### 2.3 Owned cards + cost basis query

Add to `CardRepository` (native, scoped by user):

```sql
-- owned, non-PC cards with their cost basis (null if no IN transaction)
SELECT c.id AS card_id,
       t.amount / NULLIF(cnt.n, 0) AS cost_basis
FROM cards c
LEFT JOIN transaction_infos ti_in ON ti_in.card_id = c.id AND ti_in.move = 'IN'
LEFT JOIN transactions t          ON t.id = ti_in.transaction_id AND t.user_id = :userId
LEFT JOIN (SELECT transaction_id, COUNT(*) AS n
           FROM transaction_infos WHERE move = 'IN'
           GROUP BY transaction_id) cnt ON cnt.transaction_id = ti_in.transaction_id
WHERE c.user_id = :userId
  AND (c.type IS NULL OR c.type <> 'PC')
  AND NOT EXISTS (SELECT 1 FROM transaction_infos o WHERE o.card_id = c.id AND o.move = 'OUT')
```

Return it as an interface projection (`OwnedCardCost { int getCardId(); Double getCostBasis(); }`).
If a card has more than one `IN` row (data error), take the first and log it.

### 2.4 `service/CardAskPriceService.java`

Turns one `Card` into asking-price stats.

1. **Build the query** from the card's fields, skipping nulls/blanks:
   `year + set + player + parallel + numbered + (auto ? "auto") + grade`.
   Example: `2018 Topps Chrome Shohei Ohtani Refractor PSA 10`. Store it on the item
   (`ebay_query`) so a bad match can be debugged.
2. Call `ebayBrowseService.searchItems(query, 50)`.
3. **Filter titles** (case-insensitive):
   - must contain the player's last name,
   - if `grade` is set, must contain the grade string (e.g. `PSA 10`),
   - if `grade` is blank, drop titles containing `PSA|BGS|SGC|CGC` (raw vs graded),
   - drop `lot`, `reprint`, `custom`, `you pick`, `choose`.
4. Parse prices as `BigDecimal`. Keep `USD` only.
5. Return `AskStats { int count; BigDecimal lowAsk (p25); BigDecimal median; List<Sample> top3Cheapest }`.

Keep the filter and percentile logic in package-private static methods so they can be
unit-tested without eBay.

### 2.5 `service/SellSignalScorer.java`

Pure and stateless:

```java
static Result score(BigDecimal costTwd, AskStats stats, BigDecimal usdToTwd)
```

- `costTwd == null` → `NO_COST`
- `stats.count < 3` → `TOO_FEW_LISTINGS`
- otherwise `gain = lowAsk * fx / cost - 1`, `score = gain * min(1, count / 5)`, `RANKED`

Ranking = sort `RANKED` items by `score` desc, assign `rank` 1..n.

### 2.6 `service/SellSignalAiService.java`

One Gemini call for the top 5 ranked items:

```java
Map<Integer, String> explain(List<RankedCard> top5)   // cardId -> reason
```

Prompt outline:

> You are helping a sports card collector decide which cards may be worth selling now.
> Prices below are current eBay **asking** prices and the collector's cost; do not invent or
> restate prices. For each card, search for the player's recent news (performance, awards,
> injuries, team situation, upcoming events) and give one or two sentences on whether demand
> is likely near a high or likely to fade. Reply with exactly one line per card:
> `CARD <id>: <reason>`

- `geminiService.generateContent(prompt, "gemini-2.5-flash", true)`.
- **Don't use JSON mode.** Gemini doesn't allow `response_mime_type: application/json`
  together with the `google_search` tool. Parse `CARD <id>:` lines with a regex and ignore
  anything else. A card with no parsed line gets `ai_reason = null`.
- Usage limit: copy the check-and-increment from `CardAiService.analyzeCardPotential`,
  including reloading the real `User` entity (the principal may be an `OAuth2UserPrincipal`).
  Better: extract that into a small shared helper (e.g. `AiCallGuard`) and have both services
  use it.

### 2.7 `service/SellSignalService.java` (orchestrator)

```java
SellSignalRun run(User principal)
Optional<SellSignalRun> latest(User principal)
```

`run`:

1. Cooldown: if the user is not an admin and `latest` is younger than 24 h, return it
   (controller shows a flash message).
2. Load owned cards + cost (§2.3), order `INVESTMENT` first then cost desc, take 20.
3. For each card: `CardAskPriceService` → `SellSignalScorer`. Catch `RestClientException`
   per card → `EBAY_ERROR`.
4. Rank, then call `SellSignalAiService` for the top 5. Set `ai_status` (`OK` / `FAILED` on
   exception / `SKIPPED` at the usage limit or when nothing is ranked).
5. Save the run and its items in **one short transaction at the end**. Don't hold a DB
   transaction open across the eBay and Gemini calls (the same mistake as
   `CrawlerService.runCrawlerForAllUsers`).

v1 runs synchronously: 20 sequential Browse calls plus one Gemini call is roughly 10–30 s.
Show a spinner on submit. If it's too slow, run the eBay calls on a small fixed thread pool
(4) in a follow-up, not v1.

### 2.8 `controller/SellSignalController.java`

```java
@Controller
@RequestMapping("/sell-signals")
GET  ""      -> model: run (nullable), cards by id (scoped findByIdAndUserId), nextRunAt
POST "/run"  -> sellSignalService.run(user); redirect:/sell-signals (+ flash on cooldown / limit)
```

Load the `Card`s for display with `findByIdAndUserId` (or one scoped `IN` query), never an
unscoped find.

---

## 3. Frontend

### `templates/sellSignals.html` (uses `template.html` layout like the other pages)

- Header: "Sell Signals" + last run time + **Run** button.
- **Ranked table** (top 5 highlighted, rest below): card (player / year / set / parallel /
  grade), cost ≈ TWD, listed from (low ask, USD + TWD), median ask, gain %, # listings, AI
  reason, expandable "See listings" with the ≤ 3 sample links.
- **No cost data** list: card + listed-from price + "add a buy transaction to rank this card"
  link to the add-transaction page.
- **Not enough listings / eBay error** list, collapsed.
- `ai_status = SKIPPED` → banner "AI reasons skipped: monthly AI limit reached".
  `FAILED` → "AI reasons unavailable for this run".
- Footer disclaimer (fixed text from *Decisions*).
- Nav (`banner.html`): add a "Sell Signals" link inside a
  `sec:authorize="hasAuthority('ROLE_ADMIN')"` element for v1. Change it to
  `isAuthenticated()` when opening to members (§7).
- During the cooldown (non-admins only), the Run button is disabled with "next run available at …".

---

## 4. Admin

- `adminSettings.html`: add a field for `sell_signals.usd_to_twd`.
- Not in v1: an admin report of runs. Add one if eBay quota becomes a concern.

---

## 5. Tests

`src/test` currently has only the context-load test, so add:

- `SellSignalScorerTest`: no cost, < 3 listings, gain math, thin-market discount, ranking order.
- `CardAskPriceServiceTest` (static helpers): query building with null fields, title filters
  (graded vs raw, lots, reprints), p25/median on odd and even counts.
- `SellSignalAiServiceTest`: parsing `CARD <id>:` lines with extra chatter, missing ids, and
  ids that weren't in the prompt (ignored).
- Repository test for §2.3 (needs a test DB; skip if there's no Testcontainers setup and
  verify manually instead): a bulk buy split, a sold card excluded, a `PC` card excluded,
  another user's card never returned.

---

## 6. Build order

1. §0 cleanup (separate small PR).
2. Migration + enums + entities + repositories.
3. §2.3 query, verified against real data in the dev DB.
4. `CardAskPriceService` + `SellSignalScorer` + tests. **Check the match quality by hand on
   about 10 of your own cards before going further.** Comp matching is the riskiest part.
5. Orchestrator + controller + page **without AI** (ranking only). Ship this to yourself.
6. `SellSignalAiService` + shared `AiCallGuard`.
7. Admin FX setting, admin-only nav link, disclaimer polish.
8. Use it yourself for a while, then open it to members (§7).

---

## 7. Opening to members (later, separate PR)

1. `SecurityConfig`: remove `"/sell-signals/**"` from the admin matcher (it falls back to
   `anyRequest().authenticated()`).
2. `banner.html`: change the nav link's `sec:authorize` to `isAuthenticated()`.
3. Confirm the 24 h cooldown and AI usage limit apply to `USER` / `TEST` roles.
4. Check eBay Browse quota headroom: members × 20 calls/day against the 5,000/day app limit.
5. Optionally gate by role with a `UsageLimits` toggle instead of opening to everyone at once.

---

## Open questions

- **Use `cards.value` as a fallback cost?** It's user-entered "current value", not cost, so
  the plan doesn't use it. Revisit if most cards lack a buy transaction.
- **eBay marketplace.** Browse is called with `EBAY_US`. Fine for US-market cards; Japanese or
  Taiwan-only cards will match poorly.

## Later

- **Yahoo TW sold prices** (the long-term moat): once the crawler records final sold prices,
  add a second price source and prefer sold data over asking prices. First check whether ended
  Yahoo auctions still show the final price.
- **Trend signal**: with saved run history, compare `low_ask` for the same card across runs
  (price rising = stronger sell signal).
- **Weekly digest email**: reuse the crawler email pipeline, but only after the ESP move
  (Gmail's ~500/day cap).
- **Paid sold data** (SportsCardsPro / PriceCharting API) if Yahoo TW coverage isn't enough
  for US cards.
- **Link to `recommendations`**: if an admin/AI recommendation exists for the player, show it
  beside the signal.
