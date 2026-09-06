/* A second horizontal scrollbar above any table that scrolls sideways, so the
   right-hand columns are reachable without first scrolling to the bottom of
   the page to find the real one.

   Applies to every .rg-table-card on the page and needs no markup: the bar is
   inserted, kept in sync both ways, and hidden again whenever the table fits.
   Under 768px the card tables stack into rows and stop scrolling sideways, so
   nothing appears there. Vanilla JS - this is loaded by fragments/head, which
   some pages include without jQuery. */
(function () {
  "use strict";

  function attach(container) {
    if (container.dataset.topScroll) {
      return;
    }
    container.dataset.topScroll = "1";

    var bar = document.createElement("div");
    bar.className = "rg-table-topscroll";
    bar.setAttribute("aria-hidden", "true");
    var filler = document.createElement("div");
    bar.appendChild(filler);
    container.parentNode.insertBefore(bar, container);

    // Without this the two elements fight each other over every scroll event.
    var syncing = null;

    function follow(from, to) {
      return function () {
        if (syncing && syncing !== from) {
          return;
        }
        syncing = from;
        to.scrollLeft = from.scrollLeft;
        window.requestAnimationFrame(function () {
          syncing = null;
        });
      };
    }

    bar.addEventListener("scroll", follow(bar, container), { passive: true });
    container.addEventListener("scroll", follow(container, bar), { passive: true });

    function measure() {
      var overflows = container.scrollWidth - container.clientWidth > 1;
      bar.hidden = !overflows;
      if (overflows) {
        // Match the container's own viewport width (it has a border, the bar
        // does not), so both end up with exactly the same maximum scroll and
        // the bar cannot be dragged past where the table stops.
        bar.style.width = container.clientWidth + "px";
        filler.style.width = container.scrollWidth + "px";
        bar.scrollLeft = container.scrollLeft;
      }
    }

    measure();

    var pending = false;
    function remeasure() {
      if (pending) {
        return;
      }
      pending = true;
      window.requestAnimationFrame(function () {
        pending = false;
        measure();
      });
    }

    window.addEventListener("resize", remeasure);

    if (window.ResizeObserver) {
      var ro = new ResizeObserver(remeasure);
      ro.observe(container);
      // The container is full width whatever the table does, so watch the
      // table itself too - it is what changes width when a column wraps
      // differently or the webfont finishes loading.
      if (container.firstElementChild) {
        ro.observe(container.firstElementChild);
      }
    }

    if (document.fonts && document.fonts.ready) {
      document.fonts.ready.then(remeasure);
    }
    // Search results and inline editing rewrite the table after load.
    if (window.MutationObserver) {
      new MutationObserver(remeasure).observe(container, {
        childList: true,
        subtree: true,
        characterData: true
      });
    }
  }

  function scan() {
    var containers = document.querySelectorAll(".rg-table-card");
    for (var i = 0; i < containers.length; i++) {
      attach(containers[i]);
    }
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", scan);
  } else {
    scan();
  }

  window.TableTopScroll = { scan: scan };
})();
