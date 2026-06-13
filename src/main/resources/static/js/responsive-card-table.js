/* ==========================================================================
   responsive-card-table.js
   --------------------------------------------------------------------------
   Drives the mobile "stacked card" behaviour for wide tables. A table opts in
   with a class:
     .mobile-cards  -> collapsible data list (tap a card to show/hide details)
     .mobile-form   -> stacked input form (all fields shown)

   Responsibilities:
     1. Copy each table's <th> header text onto its body cells as data-label,
        so the CSS can print labels regardless of column order. A
        MutationObserver re-labels rows added later (AJAX search, "add card").
     2. Toggle .expanded on a .mobile-cards row when it's tapped (collapsed
        detail fields show/hide). Works for static and AJAX rows via delegation.
   The layout itself lives in responsive-tables.css.
   ========================================================================== */
(function () {
  "use strict";

  var MOBILE_MAX = 768;
  var SELECTOR = ".mobile-cards, .mobile-form";

  function isMobile() {
    return window.innerWidth <= MOBILE_MAX;
  }

  /* Copy header labels onto every body cell of one table. */
  function labelTable(table) {
    var rows = table.rows;
    if (rows.length < 2) return;

    var headerCells = rows[0].cells;
    var labels = [];
    for (var c = 0; c < headerCells.length; c++) {
      labels.push(headerCells[c].textContent.replace(/\*/g, "").trim());
    }

    for (var r = 1; r < rows.length; r++) {
      var cells = rows[r].cells;
      for (var k = 0; k < cells.length; k++) {
        if (labels[k]) cells[k].setAttribute("data-label", labels[k]);
      }
    }
  }

  function init() {
    var tables = document.querySelectorAll(SELECTOR);
    for (var i = 0; i < tables.length; i++) {
      labelTable(tables[i]);
      /* Re-label when rows are added/removed later. setAttribute only touches
         attributes, so labelling won't re-trigger this childList observer. */
      (function (t) {
        new MutationObserver(function () {
          labelTable(t);
        }).observe(t, { childList: true, subtree: true });
      })(tables[i]);
    }
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", init);
  } else {
    init();
  }

  /* Return the collapsible card row owning this element, or null. */
  function cardRowFor(el) {
    var row = el.closest("tr");
    if (!row) return null;
    if (!row.closest("table.mobile-cards")) return null;
    if (row.querySelector("th")) return null; // header row
    return row;
  }

  document.addEventListener("click", function (e) {
    if (!isMobile()) return;

    /* Entering edit mode: force the card open so hidden fields are editable.
       The page's own ".modify" handler still runs. */
    if (e.target.closest(".modify")) {
      var editRow = cardRowFor(e.target);
      if (editRow) editRow.classList.add("expanded");
      return;
    }

    /* Don't treat clicks on real controls as a toggle. */
    if (e.target.closest("button, input, select, textarea")) return;

    var row = cardRowFor(e.target);
    if (row) row.classList.toggle("expanded");
  });

  /* Once back on a desktop width the cards revert to a table — drop any
     leftover expanded state. */
  var resizeTimer;
  window.addEventListener("resize", function () {
    clearTimeout(resizeTimer);
    resizeTimer = setTimeout(function () {
      if (isMobile()) return;
      var expanded = document.querySelectorAll("tr.expanded");
      for (var i = 0; i < expanded.length; i++) {
        expanded[i].classList.remove("expanded");
      }
    }, 150);
  });
})();
