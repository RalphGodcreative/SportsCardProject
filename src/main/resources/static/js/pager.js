/*
 * Shared windowed pager (daisyUI .join + .btn, styled by .rg-pager__* in rgneon.css).
 *
 *   RGPager.render({
 *     control: "#page-control",   // empty .join container for the buttons
 *     range:   "#pager-range",    // optional: element that gets "51–60"
 *     total:   118,               // total rows
 *     page:    6,                 // current page, 1-based
 *     pageSize: 10,
 *     href: function (p) { return "/card/cards?page=" + p; }
 *   });
 *
 * Shows first/prev, 1 … c-1 c c+1 … last, next/last. On phones (CSS) the
 * numbers collapse into a "c / N" counter.
 */
(function (global) {
  var ICONS = {
    first: '<path d="M11 17l-5-5 5-5M18 17l-5-5 5-5"/>',
    prev: '<path d="M15 18l-6-6 6-6"/>',
    next: '<path d="M9 18l6-6-6-6"/>',
    last: '<path d="M13 17l5-5-5-5M6 17l5-5-5-5"/>'
  };

  function svg(name) {
    return '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor"' +
      ' stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">' +
      ICONS[name] + '</svg>';
  }

  function render(opts) {
    var pageSize = opts.pageSize || 10;
    var total = Math.max(0, parseInt(opts.total, 10) || 0);
    var totalPages = Math.max(1, Math.ceil(total / pageSize));
    var current = Math.min(Math.max(1, parseInt(opts.page, 10) || 1), totalPages);
    var href = opts.href;

    var rangeEl = opts.range && document.querySelector(opts.range);
    if (rangeEl) {
      var from = total === 0 ? 0 : (current - 1) * pageSize + 1;
      var to = Math.min(current * pageSize, total);
      rangeEl.textContent = from + "–" + to;
    }

    var ctrl = document.querySelector(opts.control);
    if (!ctrl) return;
    if (totalPages <= 1) { ctrl.innerHTML = ""; ctrl.hidden = true; return; }

    function step(name, target, label, disabled, extra) {
      var cls = "join-item btn btn-sm rg-pager__btn " + (extra || "");
      if (disabled) {
        return '<button type="button" class="' + cls + ' btn-disabled" disabled aria-label="' + label + '">' + svg(name) + '</button>';
      }
      return '<a class="' + cls + '" href="' + href(target) + '" aria-label="' + label + '">' + svg(name) + '</a>';
    }

    function num(p) {
      var cur = p === current;
      return '<a class="join-item btn btn-sm rg-pager__btn rg-pager__num' + (cur ? ' btn-primary' : '') +
        '" href="' + href(p) + '"' + (cur ? ' aria-current="page"' : '') +
        ' aria-label="Page ' + p + (cur ? ', current' : '') + '">' + p + '</a>';
    }

    var gap = '<span class="join-item btn btn-sm btn-disabled rg-pager__btn rg-pager__num rg-pager__gap" aria-hidden="true">…</span>';

    // Always 1 and last, ±1 around current; near either end show 4 in a row
    // so the window never shrinks to a lone number next to an ellipsis.
    var pages = [];
    for (var p = 1; p <= totalPages; p++) {
      if (p === 1 || p === totalPages || Math.abs(p - current) <= 1 ||
          (current <= 3 && p <= 4) || (current >= totalPages - 2 && p >= totalPages - 3)) {
        pages.push(p);
      }
    }

    var atStart = current <= 1, atEnd = current >= totalPages;
    var html = step("first", 1, "First page", atStart, "rg-pager__edge") +
               step("prev", current - 1, "Previous page", atStart);
    for (var i = 0; i < pages.length; i++) {
      if (i > 0 && pages[i] - pages[i - 1] > 1) html += gap;
      html += num(pages[i]);
    }
    html += '<span class="join-item btn btn-sm rg-pager__btn rg-pager__compact" aria-hidden="true">' +
            current + ' / ' + totalPages + '</span>';
    html += step("next", current + 1, "Next page", atEnd) +
            step("last", totalPages, "Last page", atEnd, "rg-pager__edge");

    ctrl.hidden = false;
    ctrl.innerHTML = html;
  }

  global.RGPager = { render: render };
})(window);
