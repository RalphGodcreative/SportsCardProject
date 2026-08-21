# Implementation Plan: Tags (replaces Storages)

## Summary

The `storages` feature becomes **tags**. Same idea — user-defined labels for organizing
cards — but the relationship changes from **one storage → many cards** to
**many cards ↔ many tags**. A card can carry any number of tags ("Box 1", "Rookie",
"PSA 10", "For Sale"); a tag can be on any number of cards.

Storages are already fully implemented (entity, repo, service, `StorageController`,
`V7` migration, `/storage` pages, and a storage column in 9 templates). This is a
**refactor of working code**, not a greenfield feature, so the plan is written as a
rename + remodel.

### Decisions taken (2026-08-19)

| Question | Decision |
|---|---|
| Existing storage data | **Migrate in place.** `storages` is renamed to `tags`; every `cards.storage_id` becomes one `card_tags` row. Nothing is lost. |
| Multi-tag picker UI | **Checkbox dropdown.** A button showing the selected tag names, opening a panel of checkboxes. |
| Join-table mapping | **JPA `@ManyToMany` with `@JoinTable`.** No explicit `CardTag` entity — see §2.2. |
| Table stays minimal | `tags` keeps `id / name / user_id` only — no `created_at`, no description, matching the storage decision. |

---

## 1. Schema

### `V8__replace_storages_with_tags.sql` — New Flyway Migration

```sql
-- 1. storages becomes tags (keeps the rows, the ids, and the identity sequence)
ALTER TABLE storages RENAME TO tags;
ALTER TABLE tags RENAME CONSTRAINT fk_storages_user TO fk_tags_user;

-- 2. the join table
CREATE TABLE card_tags (
    card_id INTEGER NOT NULL,
    tag_id  BIGINT  NOT NULL,
    PRIMARY KEY (card_id, tag_id),
    CONSTRAINT fk_card_tags_card FOREIGN KEY (card_id) REFERENCES cards(id) ON DELETE CASCADE,
    CONSTRAINT fk_card_tags_tag  FOREIGN KEY (tag_id)  REFERENCES tags(id)  ON DELETE CASCADE
);

CREATE INDEX idx_card_tags_tag ON card_tags (tag_id);

-- 3. carry every existing assignment over
INSERT INTO card_tags (card_id, tag_id)
SELECT id, storage_id FROM cards WHERE storage_id IS NOT NULL;

-- 4. drop the old single-valued column
ALTER TABLE cards DROP CONSTRAINT fk_cards_storage;
ALTER TABLE cards DROP COLUMN storage_id;
```

Notes:

- `cards.id` is `INTEGER` (see `V1__baseline_schema.sql`), `storages.id` is `BIGINT` —
  the join-table column types above match each side exactly.
- Both FKs are `ON DELETE CASCADE`: deleting a tag drops its assignments (cards survive,
  they just lose that tag); deleting a card drops its assignments. This replaces the old
  `ON DELETE SET NULL` behaviour and means `CardService.deleteCard` /
  `deleteTransactionAndAllRef` need no extra cleanup.
- `spring.jpa.hibernate.ddl-auto=validate` is set, so Hibernate will check the
  `@JoinTable` mapping against this schema at boot. The column names `card_id` / `tag_id`
  must match the annotation exactly.
- **Optional, not included:** `UNIQUE (user_id, name)` on `tags`. Storages allowed
  duplicate names; keeping the same looseness unless asked otherwise.

---

## 2. Backend

### 2.1 `entity/Tag.java` — renamed from `Storage.java`

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tags")
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private String name;

    @Column(name = "user_id")
    private Long userId;
}
```

Identical to `Storage` apart from the name. Delete `Storage.java`.

### 2.2 Why there is no `CardTag` entity

The `card_tags` **table** is required — that is simply how a many-to-many is stored. A
`CardTag` **entity class** is not: JPA's `@ManyToMany` + `@JoinTable` manages those rows
itself, and taking that route deletes a large amount of otherwise-necessary code.

| Explicit `CardTag` entity would need | `@ManyToMany` |
|---|---|
| `CardTag`, `CardTagId`, `CardTagRepository` | nothing — JPA owns the join table |
| `setCardTags` / `findTagIdsForCards` / `findCardIdsWithTag` in the service | nothing |
| a `withTags()` hydration call wrapped around 8 read methods | collection loads with the card |
| a `tagNames` map passed as a model attribute in 8 controller spots | `card.tags` already carries `Tag` objects with names |

That last row matters most. Today every controller passes a `storageNames` map purely so
templates can do `storageNames.get(card.storageId)`. With a real association the map
disappears from the controllers *and* the templates — `card.tags` is enough.

The one argument against `@ManyToMany` here would be
`CardRestController.searchCard`, which calls `om.writeValueAsString(cards)` on entities
returned by the service — a lazy collection touched after the transaction closed. That is
a non-issue in this app: `spring.jpa.open-in-view` is unset, so Spring Boot's default
(`true`) keeps the persistence context open for the whole request, and §2.6 specifies
`FetchType.EAGER` anyway, which removes the dependency on OSIV entirely.

**Revisit this only if the join row ever needs its own columns** (`added_at`, `added_by`,
a per-card ordering). At that point promote it to an entity with `@OneToMany` on both
sides. Nothing plans that today.

### 2.3 `dao/TagRepository.java` — renamed from `StorageRepository.java`

```java
public interface TagRepository extends JpaRepository<Tag, Long> {

    List<Tag> findByUserIdOrderByIdAsc(Long userId);

    Optional<Tag> findByIdAndUserId(long id, Long userId);

    /** Ownership-enforcing bulk lookup — the security boundary for tag assignment. */
    List<Tag> findByIdInAndUserId(Collection<Long> ids, Long userId);
}
```

`findByIdInAndUserId` is the new one. It resolves posted tag ids into `Tag` entities and
silently drops anything not owned by the caller, in a single query — so a crafted
`tagIds` param cannot attach another user's tag to your card. `data-ownership.md` rules apply.

### 2.4 `service/TagService.java` — renamed from `StorageService.java`

Keeps `findAllForUser`, `create`, `findByIdAndUserId`, `delete`, `rename` verbatim
(s/Storage/Tag/). `findNameMapForUser` can be **deleted** — nothing needs an id→name map
any more now that templates read `card.tags` directly.

One method is added:

```java
/** Resolve posted tag ids to entities, dropping any the user doesn't own. */
public Set<Tag> resolveOwnedTags(List<Long> tagIds, Long userId) {
    if (tagIds == null || tagIds.isEmpty()) return new LinkedHashSet<>();
    return new LinkedHashSet<>(tagRepository.findByIdInAndUserId(tagIds, userId));
}
```

Returning an empty set (never `null`) matters: an empty collection on a saved card is what
clears its tags.

`delete(id, userId)` needs no change — the `ON DELETE CASCADE` on `fk_card_tags_tag`
removes the assignments. If you would rather not rely on the DB, add
`@Modifying @Query("DELETE FROM ...")` — but the cascade is simpler and already correct.

### 2.5 `entity/Card.java`

Remove:

```java
@Column(name = "storage_id")
private Long storageId;
```

Add:

```java
@ManyToMany(fetch = FetchType.EAGER)
@JoinTable(
        name = "card_tags",
        joinColumns = @JoinColumn(name = "card_id"),
        inverseJoinColumns = @JoinColumn(name = "tag_id")
)
@OrderBy("name")
@BatchSize(size = 50)
@ToString.Exclude
@EqualsAndHashCode.Exclude
private Set<Tag> tags = new LinkedHashSet<>();
```

Each annotation is load-bearing:

- **`EAGER`** — every page that lists cards also shows their tags, so there is no read path
  that would benefit from lazy. Being eager also means the code does not depend on
  open-in-view staying enabled, and `ObjectMapper` serialization in `CardRestController`
  can never hit a lazy-init error.
- **`@BatchSize(size = 50)`** — without it, an eager collection over a 100-card list is
  100 extra selects. With it, Hibernate batches them ~2. This is the N+1 fix.
- **`@OrderBy("name")`** — a `Set` has no order; this makes chip rendering stable between
  page loads.
- **`@ToString.Exclude` / `@EqualsAndHashCode.Exclude`** — `Card` is a Lombok `@Data`
  class. Without these, generated `toString`/`hashCode` walk the collection, which is the
  classic Lombok-plus-JPA footgun (recursion and surprise queries). Non-optional.
- **Initialized to an empty set** — so `card.getTags()` is never `null` in a template.

`@AllArgsConstructor` gains one parameter; the three hand-written constructors are
unchanged. Grep for positional `new Card(` calls before compiling.

Note the JSON shape this produces for `searchCard`:
`"tags":[{"id":1,"name":"Rookie","userId":1}]` — see §3.2 for how the JS reads it.

### 2.6 `dao/CardRepository.java`

Delete `findByStorageIdAndUserId` and `countByStorageIdAndUserId`. Add the derived
equivalents that traverse the new association:

```java
List<Card> findByTags_IdAndUserIdOrderByIdDesc(Long tagId, Long userId);

long countByTags_IdAndUserId(Long tagId, Long userId);
```

Spring Data resolves `Tags_Id` through the collection and generates the join. Nothing else
in the repository changes.

### 2.7 `dao/CardSpec.java`

Replace `equalsStorageId` with a tag filter. The search picker is multi-select, so the
semantics are **AND** — a card matches only if it carries *every* selected tag. (OR would
be a single `IN`; AND is the more useful default for narrowing a collection.)

```java
public static Specification<Card> build(Card card, List<Long> tagIds, Long userId) {
    return Specification
            .where(forUser(userId))
            // ... all existing predicates unchanged ...
            .and(hasAllTags(tagIds));
}

private static Specification<Card> hasAllTags(List<Long> tagIds) {
    if (tagIds == null || tagIds.isEmpty()) return null;
    Specification<Card> spec = null;
    for (Long tagId : tagIds) {
        spec = spec == null ? hasTag(tagId) : spec.and(hasTag(tagId));
    }
    return spec;
}

private static Specification<Card> hasTag(Long tagId) {
    return (root, q, cb) -> {
        Join<Card, Tag> t = root.join("tags", JoinType.INNER);
        return cb.equal(t.get("id"), tagId);
    };
}
```

The trick is that each `hasTag` call issues its **own** `root.join`, so AND-ing three of
them produces three independent joins — exactly "has tag A *and* tag B *and* tag C". And
because every join is pinned to one specific tag id, a matching card yields exactly one
row, so no `distinct` is needed.

This spec is only used by `findCardsWithParam`, which calls `findAll(spec, Sort)` — no
count query, so the joins cannot corrupt a pagination count.

### 2.8 `service/CardService.java`

Much smaller change than the join-entity approach would have needed — **no hydration
helper and no `TagService` dependency**, because JPA loads `card.tags` with the card.

Replace the two storage methods:

```java
// was getCardsByStorage / countCardsInStorage
public List<Card> getCardsByTag(Long tagId, Long userId) {
    return cardRepo.findByTags_IdAndUserIdOrderByIdDesc(tagId, userId);
}

public long countCardsWithTag(Long tagId, Long userId) {
    return cardRepo.countByTags_IdAndUserId(tagId, userId);
}
```

Update the one signature that gained a parameter:

```java
public List<Card> findCardsWithParam(Card card, List<Long> tagIds, Long userId) {
    return cardRepo.findAll(CardSpec.build(card, tagIds, userId), Sort.by(DESC, "id"));
}
```

`saveCard`, `saveTransactionWithCard`, `addCardsToTransaction`, `deleteCard` and
`deleteTransactionAndAllRef` are **unchanged**. Saving a `Card` whose `tags` collection is
populated syncs the join rows automatically, including on the detached-merge path that
`updateCard` uses (`new Card(id, ...)` → `save()`). Deleting a card removes its join rows.

> **Watch the merge semantics:** because `updateCard` builds a fresh `Card` and merges it,
> whatever is in `tags` at save time *becomes* the card's full tag set. An empty set clears
> the card's tags — which is the behaviour we want when the user unchecks everything, but
> it means any code path that saves a `Card` without populating `tags` will silently wipe
> them. `CardController.updateCard` and `saveCard` are the only such paths and both set it
> explicitly (§2.9).

### 2.9 Controllers

**`StorageController.java` → `TagController.java`**, `@RequestMapping("/tag")`. Same
shape: `GET /tag` (list page), `GET /tag/{id}/cards`, `PUT /tag/add`, `PUT /tag/rename`,
`DELETE /tag/delete`. Params rename `storageId` → `tagId`. The card-count loop calls
`cardService.countCardsWithTag`.

**`CardController.java`** — swap `StorageService` for `TagService`. Every
`model.addAttribute("storageNames", storageService.findNameMapForUser(...))` is **deleted
outright**, not renamed — templates read `card.tags` now. The pages that host a picker
still need the full tag list, so those keep one attribute:

```java
model.addAttribute("tags", tagService.findAllForUser(currentUser.getId()));
```

That is every card-listing and card-form page (`cardMain`, `allCard`, `pagingAllCard`,
`addCardPage`, `addTransactionPage`, `searchCardPage`, `sellTransactionPage`,
`transactionList`), because each one can edit tags inline.

In `saveCard` and `updateCard`:

```java
@RequestParam(name = "tagIds", required = false) List<Long> tagIds,
// ...
card.setTags(tagService.resolveOwnedTags(tagIds, currentUser.getId()));
```

`required = false` matters: when no boxes are checked the param is absent, `tagIds` is
`null`, `resolveOwnedTags` returns an empty set, and the card's tags are cleared — correct.

**`CardRestController.searchCard`** — same `tagIds` param, then
`component.findCardsWithParam(card, tagIds, currentUser.getId())`.

**`CardController.saveTransaction` / `saveSale`** — the JSON body path. Jackson will
deserialize `tags: [{"id":1}]` into detached `Tag` instances. Since the `@ManyToMany` has
**no cascade**, Hibernate writes join rows from the ids and never tries to insert a `Tag`.
But Jackson-supplied ids are unvalidated, so `saveTransactionWithCard` must re-resolve
them through `resolveOwnedTags` before saving — do not trust the posted objects. Simplest
form: have the JS post `tagIds: [1,3]` (a plain id array on a `@Transient` setter or a DTO
field) rather than whole tag objects, and resolve server-side like the form paths do.

**`TransactionController`** — drop the `storageNames` attribute, add `tags`.

**`SecurityConfig`** — no change needed. `/storage` was never listed explicitly; it fell
under `.anyRequest().authenticated()`, and `/tag` will too. Grep to confirm no `/storage`
string survives.

---

## 3. Frontend

### 3.1 The tag picker — `static/js/tag-picker.js` + `static/assets/tag-pages.css`

One reusable component, used everywhere a storage `<select>` is today.

Markup a template emits:

```html
<div class="tag-picker" data-selected="1,3"></div>
```

JS contract:

```js
TagPicker.initAll(rootEl)          // build every .tag-picker inside rootEl
TagPicker.get(el)                  // -> [1, 3]  (array of tag ids as numbers)
TagPicker.set(el, [1, 3])          // set selection
```

Every page defines the tag list once via `th:inline="javascript"`:

```html
<script th:inline="javascript">
  window.ALL_TAGS = /*[[${tags}]]*/ [];
</script>
```

> **Critical layout constraint:** `.table-center` in `style.css` sets
> `overflow-x: auto`. An absolutely-positioned dropdown panel inside a table cell **will
> be clipped** by that scroll container. The menu must therefore be rendered into a
> single body-level container with `position: fixed`, positioned from the toggle button's
> `getBoundingClientRect()` on open, and closed on outside-click, `Esc`, scroll, and
> resize. This is the one genuinely fiddly part of the whole change — build and eyeball it
> on `pagingAllCard.html` first, before touching the other eight templates.

Read-only display (the `.show` element that currently prints the storage name) becomes a
row of small chips rendered straight from the association — no lookup map:

```html
<span class="show show-tags">
  <span class="tag-chip" th:each="t : ${card.tags}" th:text="${t.name}"></span>
</span>
```

And the picker seeds itself from the same collection:

```html
<div class="tag-picker" th:data-selected="${#strings.listJoin(card.tags.![id], ',')}"></div>
```

(SpEL projection over a `Set` returns a `List`, so `listJoin` is right — worth confirming
on the first template rather than assuming.)

### 3.2 Template-by-template

`storage-pages.css` → `tag-pages.css` (rename classes `.storage-*` → `.tag-*`), plus the
picker and chip styles appended.

| File | Change |
|---|---|
| `templates/storage/list.html` → `templates/tag/list.html` | Rename throughout: `/storage/*` fetch URLs → `/tag/*`, `storageId` param → `tagId`, `#new-storage-name` → `#new-tag-name`, copy changes ("Storage Locations" → "Tags", the box/binder blurb → something tag-flavoured). The delete-confirm text "Cards inside it won't be deleted" still holds — reword to "they'll just lose this tag". Swap the box SVG for a tag SVG. |
| `templates/storage/cards.html` → `templates/tag/cards.html` | Same rename; header shows the tag name; the `storage` column in its card table becomes the `tags` picker; breadcrumb → `/tag`. |
| `banner.html` (2 spots) | `<a href="/storage">manage Storages</a>` → `<a href="/tag">manage Tags</a>`, desktop + `.mobile-sub`. |
| `addCardPage.html` | `<th>storage</th>` → `<th>tags</th>`; the `<select name="storageId">` becomes a `.tag-picker`. Because this form is a plain GET submit, the picker must also emit `<input type="hidden" name="tagIds" value="...">` per checked box (or serialize on submit) so `@RequestParam List<Long> tagIds` binds. |
| `addTransactionPage.html` | `storages` / `storageOptionsHtml` splicing disappears; the JS row template gets `<td><div class="tag-picker"></div></td>` and calls `TagPicker.initAll(newRow)` after append. `getCardList()` sends `tagIds: TagPicker.get($(this).find(".tag-picker"))` (an id array — see §2.9). The clipboard copy/paste (`clipboardCard.storage`) becomes an array — `.slice()` it on copy so paste doesn't alias. Header `<th>storage</th>` → `<th>tags</th>` (2 spots). |
| `allCard.html`, `cardMain.html`, `pagingAllCard.html`, `tag/cards.html` | Three edits each: header cell; the `.show-storage` anchor → chips from `${card.tags}` (§3.1); the `<select class="storage">` → `.tag-picker`. In the save handler, `storageId: row.find(".storage").val() \|\| null` → `tagIds: TagPicker.get(row.find(".tag-picker"))`. The `storageNames` model attribute is gone, so any `storageNames.get(...)` reference must go with it. |
| `searchCardPage.html` | Two places: the search filter (`#storage` select → `#tag-filter` picker, sent as `tagIds`) and the JS-built result rows. The JS previously did `storageNames[card.storageId]`; it now maps the embedded objects — `card.tags.map(function (t) { return t.name; }).join(", ")`. Also drop the `var storageNames = /*[[${storageNames}]]*/ {};` line. Headers in both the filter table and the results table. |
| `sellTransactionPage.html` | Same two places, same `storageNames` removal. Note line ~302: `if (cardData.storage != "") info += "[" + cardData.storage + "]"` — this builds the sale description; with multiple tags decide whether to join them (`[Box 1, Rookie]`) or drop tags from the description. **Recommend dropping it** — a sale note listing every tag is noise. |
| `transactionList.html` | Header, chips, picker, and the hidden `<input class="storage-id">` at line ~597 → a `.tag-picker` (or hidden inputs) feeding the row's save payload. |

### 3.3 The jQuery serialization gotcha

Every table row save posts `$.param(cardData)` to `/card/updateCard` as
form-urlencoded. With an array value, jQuery's default emits `tagIds%5B%5D=1&tagIds%5B%5D=2`
(`tagIds[]=`), which Spring's `@RequestParam List<Long> tagIds` **will not bind**.

Fix: pass `traditional = true`.

```js
var formData = $.param(cardData, true);   // -> tagIds=1&tagIds=2
```

This applies to the save handler in `allCard.html`, `cardMain.html`, `pagingAllCard.html`,
`searchCardPage.html`, `sellTransactionPage.html`, `transactionList.html`, and
`tag/cards.html` — seven copies of the same handler. All other fields in the object are
scalars, so `traditional` changes nothing else.

The transaction pages post JSON bodies and need no such change.

### 3.4 Mobile / responsive

`responsive-tables.css` addresses columns by `nth-child`, and the tags column sits in the
same 14th slot the storage column occupied — **the column count and ordering are
unchanged**, so no `nth-child` edits. Only the comment at line ~77 ("note + storage were
added after this was first written") needs updating. The picker's fixed-position menu
needs a mobile check: on a narrow screen it should be full-width rather than
button-width.

---

## 4. Docs to update

- `docs/data-ownership.md` line 13 — the `Storage` row becomes `Tag`. `card_tags` needs a
  note: it has no `user_id` of its own, so ownership is enforced by resolving posted tag
  ids through `TagRepository.findByIdInAndUserId` (§2.3) rather than by a column.
- `docs/impl-storages.md` — delete, or leave with a one-line "superseded by
  `impl-tags.md`" header.

---

## 5. Suggested sequencing

Each step compiles and runs on its own.

1. **Migration + `Tag`** — `V8`, rename `Storage` → `Tag`, `TagRepository`. Boot the app,
   confirm Flyway applies cleanly and `card_tags` carries the old assignments
   (`SELECT count(*) FROM card_tags` should equal the old count of non-null `storage_id`).
2. **`Card.tags` mapping** — the `@ManyToMany` block (§2.5). With `ddl-auto=validate`, a
   clean boot is itself the proof the mapping matches the migration.
3. **Service + spec** — `TagService.resolveOwnedTags`, `CardService` method swaps,
   `CardRepository` derived queries, `CardSpec.hasAllTags`.
4. **Controllers** — `TagController`, and the param/attribute swaps in `CardController`,
   `CardRestController`, `TransactionController`. Backend done; templates now broken.
5. **Tag picker component** — `tag-picker.js` + `tag-pages.css`, wired into
   `pagingAllCard.html` only. Get the clipping, positioning, and save round-trip right
   here before repeating.
6. **Management pages** — `tag/list.html`, `tag/cards.html`, `banner.html`.
7. **Remaining templates** — `allCard`, `cardMain`, `addCardPage`, `addTransactionPage`,
   `searchCardPage`, `sellTransactionPage`, `transactionList`.
8. **Cleanup** — delete `Storage.java`, `StorageRepository`, `StorageService`,
   `StorageController`, `templates/storage/`, `storage-pages.css`; update the two docs;
   `grep -ri storage src/` should come back empty apart from unrelated hits.

---

## 6. Known risks

| Risk | Mitigation |
|---|---|
| Dropdown clipped by `.table-center { overflow-x: auto }` | Body-level fixed-position menu layer (§3.1). Prove it in step 5. |
| `tagIds[]` vs `tagIds` form encoding | `$.param(cardData, true)` in all seven save handlers (§3.3). |
| Lombok `@Data` on `Card` recursing into `tags` in `toString`/`hashCode` | `@ToString.Exclude` + `@EqualsAndHashCode.Exclude` (§2.5). |
| N+1 selects from the eager collection over a card list | `@BatchSize(size = 50)` (§2.5). Watch the SQL log on `/card/allCard` once. |
| A save path that forgets to populate `tags` silently wipes a card's tags | Only `saveCard` / `updateCard` merge detached cards; both set it explicitly (§2.8). |
| Posted tag ids referencing another user's tags | `resolveOwnedTags` → `findByIdInAndUserId` filters in SQL (§2.3), applied on the JSON path too (§2.9). |
| `@AllArgsConstructor` arity changes when `tags` is added to `Card` | Grep for positional `new Card(` calls with the full field list before compiling. |
| `V8` fails if `storages` was never applied somewhere | It follows `V7` in the same Flyway chain, so ordering is guaranteed. No `IF EXISTS` guards needed. |

---

## 7. As built — where the implementation diverged from this plan

Implemented 2026-08-19 on branch `docs/impl-tags`. The schema, the `@ManyToMany`
decision, the AND-semantics filter, and the fixed-position menu all landed as written
above. Seven things ended up different or more specific:

1. **`Card.tagIds` exists after all** — but only as an inbound field:

   ```java
   @Transient
   @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
   private List<Long> tagIds;
   ```

   The two JSON write paths (`/card/saveTransaction`, `/transactions/{id}/addCards`) post
   bare tag ids, and this is where Jackson puts them. `WRITE_ONLY` keeps it out of
   outbound JSON, so responses still expose `tags` only. Both controllers resolve it
   through `resolveOwnedTags` before saving — posted ids are never trusted.

2. **A shared Thymeleaf fragment, not per-template markup.**
   `templates/fragments/tagCell.html` defines two fragments:
   - `cell(card)` — the whole `<td>` (chips + picker), used by `pagingAllCard`,
     `allCard`, `cardMain`, `transactionList`, `tag/cards`.
   - `assets` — sets `window.ALL_TAGS` from `${tags}` and pulls in `tag-picker.js` +
     `tag-picker.css`. Included once per page inside `<head>`.

   This replaced five near-identical copies of the same markup.

3. **The picker seeds from hidden `<input>` children, not a SpEL projection.** The plan
   proposed `th:data-selected="${#strings.listJoin(card.tags.![id], ',')}"`. Projection
   over a `Set` is exactly the kind of thing that works or doesn't depending on the
   Thymeleaf/SpEL version, so the fragment renders

   ```html
   <div class="update-element tag-picker">
     <input type="hidden" th:each="t : ${card.tags}" th:value="${t.id}" />
   </div>
   ```

   and `tag-picker.js` reads those values (plus any `data-selected`) before replacing the
   markup with its own. Plain `th:each`, no projection.

4. **`data-name` drives the plain-form case.** A picker marked
   `<div class="tag-picker" data-name="tagIds">` keeps hidden inputs in sync with its
   selection on every change, so `addCardPage`'s plain GET form submits
   `tagIds=1&tagIds=2` with no submit hook. AJAX pages omit `data-name` and call
   `TagPicker.get()` instead.

5. **`traditional: true` was needed in more places than §3.3 said.** Not just
   `$.param(cardData, true)` on the row saves — the two search pages pass the criteria to
   `$.ajax({ data: formData })` directly, which needs `traditional: true` in the ajax
   options for `tagIds` to bind. That is `searchCardPage.html` and
   `sellTransactionPage.html`. `transactionList.html` also has a *second* `$.param`
   call (the bulk existing-card update inside the transaction editor) that needed it.

6. **`transactionList.html`'s hidden carry-through was the real trap.** That page kept
   `<input class="storage-id" th:value="${card.storageId}">` purely so a transaction edit
   wouldn't wipe the card's storage — precisely the hazard called out in §2.8. It is now

   ```html
   <span class="tag-ids" style="display: none">
     <input type="hidden" th:each="t : ${card.tags}" th:value="${t.id}" />
   </span>
   ```

   read back into `tagIds` on submit. The "add new card" rows in that same table never
   had a storage column and still have no tag column — unchanged behaviour, not a
   regression.

7. **Small naming details.** `TagController` exposes `totalTagged` (not `totalCards`) for
   the header stat, and the sale-description `[storage]` fragment in
   `sellTransactionPage.html` was dropped entirely, as §3.2 recommended.

### Not yet verified

`mvn compile` passes and no `storage` reference survives in `src/`. The application was
**not booted**, because this checkout has no local `application.properties` (only
`application.properties.example` and `application-prod.properties`), so there is no
datasource to run Flyway against. Still to confirm on a machine with the dev DB:

- `V8` applies, and `SELECT count(*) FROM card_tags` matches the old count of non-null
  `cards.storage_id`.
- `ddl-auto=validate` accepts the `@JoinTable` mapping at boot.
- The two derived queries `findByTags_IdAndUserIdOrderByIdDesc` /
  `countByTags_IdAndUserId` resolve — these are checked at context startup, not compile
  time, so a bad property path fails on boot rather than in the build.
- The picker menu clears `.table-center`'s `overflow-x: auto` in a real browser.
