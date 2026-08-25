# Implementation: Add Transaction Page — Help Panel

Add a small info icon next to the "Add New Transaction" page title. Clicking it opens a
block of tutorial text explaining the page; an `X` in the corner closes it.

**Why this page:** `addTransactionPage.html` is the densest form in the app — a transaction
header, a multi-row card table, copy/paste/clear/delete per row, tag pickers, and `*`
required-field markers — and none of it is explained anywhere. The five transaction types
(`Break`, `Buy`, `Trade`, `Open`, `Giveaway`) in particular have no explanation in the UI or
in `docs/`; a new user has no way to know what to pick.

---

## 1. The Pattern Already Exists on This Page

`addTransactionPage.html` already implements exactly this open/close interaction for the
transaction note field — reuse its shape rather than inventing a second one:

- **Trigger:** `<img class="add-note" ... onclick="showNote()" />` (line ~324)
- **Panel:** `<div id="note">` with a close `<a onclick="hideNote()">x</a>` (line ~382)
- **Functions:** `showNote()` / `hideNote()` — plain globals outside `$(document).ready`,
  since they're called from inline `onclick` attributes (line ~200)
- **CSS:** `#note { display: none; position: fixed; top: 15%; right: 15%; ... }` (line ~253)

The help panel follows the same three pieces: a trigger next to the `<h1>`, a hidden
positioned block, and two global toggle functions.

**One thing to fix rather than copy:** `#note` uses `position: fixed` with `top: 15%; right:
15%` and no `max-width`/`max-height`, which is fragile on a phone. The help panel has more
text than the note textarea, so give it `max-width`, `max-height` with `overflow-y: auto`,
and a mobile-friendly position (see Section 4).

---

## 2. The Icon

No info-icon asset exists — `static/img/` holds only `RGForBanner.png`, `dylt.jpg`,
`im.jpg`, and `notes.png`. Two options:

**Inline SVG (recommended)** — matches `tag/list.html`, which draws all its icons as inline
`<svg>` with `stroke="currentColor"`, so the icon inherits the page's neon palette and needs
no new file or `filter: invert(1)` hack (which is what `.add-note` does today to make the
dark `notes.png` visible):

```html
<h1>
  Add New Transaction
  <button id="guide-toggle" class="guide-icon" aria-label="How to use this page" title="How to use this page">
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
      <circle cx="12" cy="12" r="9" />
      <path stroke-linecap="round" d="M12 11v5" />
      <path stroke-linecap="round" d="M12 7.75h.01" />
    </svg>
  </button>
</h1>
```

Use a `<button>`, not an `<img>` or `<a>` — it's keyboard-focusable and screen-reader
labelled for free, which the existing `.add-note` image is not.

**Alternative:** add an `info.png` to `static/img/` and mirror `.add-note` exactly. Simpler
to drop in, but inherits the invert-filter workaround and won't recolor with the theme.

---

## 3. The Panel Markup

Place the panel next to the existing `#note` div (inside `.page-top`, around line 382) so
both fixed-position overlays live together:

```html
<div id="guide">
  <div class="guide-head">
    <span>如何使用這個頁面</span>
    <a class="guide-close" onclick="hideGuide()">x</a>
  </div>
  <div class="guide-body">
    <!-- content: see Section 5 -->
  </div>
</div>
```

Toggle functions alongside the existing `showNote`/`hideNote` globals (~line 200):

```javascript
function showGuide() {
  $("#guide").show();
}
function hideGuide() {
  $("#guide").hide();
}
```

Bind the trigger inside the existing `$(document).ready(...)` block, next to the other
handlers (`#add-card`, `#add-transaction`, ~line 133) rather than with an inline `onclick`
— new code in this file should use the jQuery binding style the rest of the ready block
already uses:

```javascript
$("#guide-toggle").on("click", function () {
  showGuide();
});
```

Worth adding while you're here (the note popup lacks both, which is part of why it feels
unfinished): close on `Escape`, and close when clicking outside the panel — the same two
affordances `tag/list.html`'s confirm modal already implements (`$('#confirm-modal').on('click', ...)`
checking `e.target.id`, line ~59).

---

## 4. Styling

Add to the page's existing `<style>` block. Reuse the established page palette — the same
purple/gold border and dark panel background used by `#note` and the `#add` tables:

```css
.guide-icon {
  background: none;
  border: none;
  cursor: pointer;
  color: #38C9FA;
  width: 26px;
  height: 26px;
  padding: 0;
  vertical-align: middle;
  margin-left: 10px;
}
.guide-icon svg { width: 100%; height: 100%; }

#guide {
  display: none;
  position: fixed;
  top: 10%;
  left: 50%;
  transform: translateX(-50%);
  width: min(560px, 92vw);
  max-height: 75vh;
  overflow-y: auto;
  z-index: 50;
  text-align: left;
  background: #1e1e2e;
  border: 1px solid rgba(246, 76, 189, 0.3);
  box-shadow: 0 0 40px rgba(246, 76, 189, 0.1);
  border-radius: 4px;
  padding: 18px 22px;
  color: #e2e8f0;
}
```

`width: min(560px, 92vw)` + `max-height` + `overflow-y: auto` is the part that keeps it
usable on a phone — the page is already mobile-aware (`responsive-card-table.js`,
`responsive-tables.css`, the `.mobile-form` class on `#forCards`), so the panel shouldn't
be the one thing that breaks there.

`z-index: 50` matters: `banner.html`'s header and the mobile menu also stack, so the panel
needs to sit above them while open.

---

## 5. The Content

Written in Traditional Chinese, matching the existing precedent for explanatory copy in this
app — `tag/list.html:132`'s subtitle (`自訂標籤 方便管理 — 存放位置、隊伍、特殊標記等...`)
and the `Yahoo拍賣 追蹤` nav link. Field labels and buttons stay English, exactly as they are
today. (Easy to swap to English if preferred — it's one block of markup, no logic depends on it.)

```
交易資料（上方表格）
  date / type / amount 為必填，note 可選填（點筆記圖示開啟）。
  amount 填整筆交易的總金額，不是單張卡的價格。

交易類型
  Break     — 團拆獲得
  Buy       — 直接購買
  Trade     — 交換所得
  Open      — 自行開卡獲得
  Giveaway  — 抽獎、贈送取得

卡片列表（下方表格）
  按 add card 逐張加入這筆交易的卡片。標 * 為必填。

  * year       卡片年份 — 例：2026、2024-25
    sports     運動項目 — 例：Baseball、Basketball
    publisher  發行商 — 例：Topps、Panini
  * set        系列名稱 — 例：Chrome、Prizm、Select
  * player     球員名稱 — 例：Aaron Judge、Luka Doncic
    auto       是否為簽名卡，勾選即可
    insert     特卡種類 — 例：Home Field Advantage、Kaboom
    parallel   平行卡版本 — 例：Gold Refractor、Green
    numbered   限量編號 — 例：021/199、3/5
    grade      鑑定分數 — 例：PSA 10、BGS 9.5
    value      這張卡的個別價值，可紀錄大筆交易中單卡的價格 — 例：299
    tags       標籤，一張卡可有多個（詳見Tag頁面）

  建議每個字的第一個字母都大寫，方便日後搜尋。

快速輸入
  copy   — 複製這一列的欄位內容
  paste  — 將複製的欄位內容貼到這一列
  clear  — 清空這一列
  delete — 刪除這一列

完成後按 Add Transaction 儲存整筆交易。
```

Two content notes worth getting right, since both are easy to state wrong:
- **`amount` is the transaction total, `value` is per-card.** Nothing in the UI says this,
  and it's the most likely thing for a user to get backwards.
- **The five types are all "cards coming in."** Selling is a separate page
  (`sellTransactionPage.html`, types `Sell` / `Trade`) — worth one line if the panel should
  prevent users hunting for a "Sell" option that isn't on this page.

---

## 6. Optional Follow-on

The same icon + panel could go on the other dense pages, reusing whatever CSS class names
land here (`.guide-icon` / `#guide`):
- `sellTransactionPage.html` — the `Sell` vs `Trade` distinction, and that cards are picked
  from existing inventory rather than typed in
- `searchCardPage.html` — which fields do partial matching
- `crawler/keywords.html` — what the nightly crawler actually does and when it runs

If it spreads past two pages, lift the markup into a Thymeleaf fragment (the app already
does this for `banner.html :: header` and `fragments/tagCell :: assets`) with the body
passed in, rather than copying the block into each template.
