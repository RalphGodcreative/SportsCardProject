/* Tag picker — a checkbox dropdown for assigning multiple tags to a card.
 *
 * Markup a template emits:
 *     <div class="tag-picker" data-selected="1,3"></div>
 *
 * The available tags come from window.ALL_TAGS, which each page sets once via
 * th:inline="javascript" from the ${tags} model attribute.
 *
 * The open menu is rendered into ONE body-level container positioned with
 * position:fixed rather than inside the picker itself. That is deliberate:
 * .table-center sets overflow-x:auto, so an absolutely positioned panel inside a
 * table cell gets clipped by the scroll container.
 */
(function (window, document) {
  "use strict";

  var menuEl = null;
  var activePicker = null;

  function allTags() {
    return window.ALL_TAGS || [];
  }

  function unwrap(el) {
    if (!el) return null;
    if (el.jquery) return el.length ? el[0] : null;
    if (el.nodeType) return el;
    return null;
  }

  function readIds(el) {
    var raw = el.getAttribute("data-selected") || "";
    var seen = {};
    return raw.split(",")
      .map(function (s) { return parseInt(s, 10); })
      .filter(function (n) {
        if (isNaN(n) || seen[n]) return false;
        seen[n] = true;
        return true;
      });
  }

  function writeIds(el, ids) {
    el.setAttribute("data-selected", ids.join(","));
  }

  function nameOf(id) {
    var tags = allTags();
    for (var i = 0; i < tags.length; i++) {
      if (Number(tags[i].id) === Number(id)) return tags[i].name;
    }
    return null;
  }

  function escapeHtml(s) {
    return String(s).replace(/[&<>"']/g, function (c) {
      return { "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c];
    });
  }

  /* Chip markup for a list of ids or of {id,name} objects. Used for read-only
     display in JS-built rows too, hence the tolerant input. */
  function chipsHtml(items) {
    if (!items || !items.length) return "";
    return items.map(function (item) {
      var name = (item && typeof item === "object") ? item.name : nameOf(item);
      if (!name) return "";
      return '<span class="tag-chip">' + escapeHtml(name) + "</span>";
    }).join("");
  }

  function renderToggle(el) {
    var ids = readIds(el);
    var btn = el.querySelector(".tag-picker-toggle");
    if (!btn) return;
    var label = btn.querySelector(".tag-picker-label");
    if (ids.length) {
      label.innerHTML = chipsHtml(ids);
      btn.classList.remove("is-empty");
    } else {
      label.textContent = "-- none --";
      btn.classList.add("is-empty");
    }
  }

  function ensureMenu() {
    if (menuEl) return menuEl;
    menuEl = document.createElement("div");
    menuEl.className = "tag-picker-menu";
    menuEl.setAttribute("role", "listbox");
    document.body.appendChild(menuEl);

    menuEl.addEventListener("change", function (e) {
      if (!activePicker || !e.target.matches("input[type=checkbox]")) return;
      var id = parseInt(e.target.value, 10);
      var ids = readIds(activePicker);
      var at = ids.indexOf(id);
      if (e.target.checked && at === -1) ids.push(id);
      if (!e.target.checked && at !== -1) ids.splice(at, 1);
      writeIds(activePicker, ids);
      renderToggle(activePicker);
      syncHidden(activePicker);
    });

    /* Keep clicks inside the menu from reaching the document handler. */
    menuEl.addEventListener("mousedown", function (e) { e.stopPropagation(); });

    return menuEl;
  }

  function position(picker) {
    var rect = picker.getBoundingClientRect();
    var narrow = window.innerWidth < 700;
    if (narrow) {
      menuEl.style.left = "8px";
      menuEl.style.right = "8px";
      menuEl.style.width = "auto";
      menuEl.style.minWidth = "0";
    } else {
      menuEl.style.right = "auto";
      menuEl.style.width = "auto";
      menuEl.style.minWidth = Math.max(rect.width, 160) + "px";
      var left = Math.min(rect.left, window.innerWidth - 220);
      menuEl.style.left = Math.max(8, left) + "px";
    }

    /* Flip above the control when there isn't room below. */
    var below = window.innerHeight - rect.bottom;
    if (below < 200 && rect.top > below) {
      menuEl.style.top = "auto";
      menuEl.style.bottom = (window.innerHeight - rect.top + 4) + "px";
    } else {
      menuEl.style.bottom = "auto";
      menuEl.style.top = (rect.bottom + 4) + "px";
    }
  }

  function open(picker) {
    ensureMenu();
    activePicker = picker;
    var selected = readIds(picker);
    var tags = allTags();

    if (!tags.length) {
      menuEl.innerHTML = '<div class="tag-picker-empty">No tags yet — create some on the Tags page.</div>';
    } else {
      menuEl.innerHTML = tags.map(function (t) {
        var checked = selected.indexOf(Number(t.id)) !== -1 ? " checked" : "";
        return '<label class="tag-picker-option">' +
          '<input type="checkbox" value="' + t.id + '"' + checked + " />" +
          "<span>" + escapeHtml(t.name) + "</span></label>";
      }).join("");
    }

    menuEl.classList.add("show");
    position(picker);
    picker.classList.add("is-open");
  }

  function close() {
    if (!menuEl) return;
    menuEl.classList.remove("show");
    if (activePicker) activePicker.classList.remove("is-open");
    activePicker = null;
  }

  /* Mirror the selection into hidden inputs, for pickers inside a plain form
     submit (data-name="tagIds"). AJAX pages leave data-name off and read
     TagPicker.get() instead. */
  function syncHidden(picker) {
    var name = picker.dataset.name;
    if (!name) return;
    var old = picker.querySelectorAll("input[type=hidden]");
    for (var i = 0; i < old.length; i++) old[i].parentNode.removeChild(old[i]);
    readIds(picker).forEach(function (id) {
      var input = document.createElement("input");
      input.type = "hidden";
      input.name = name;
      input.value = id;
      picker.appendChild(input);
    });
  }

  function init(picker) {
    if (picker.dataset.tagPickerReady === "1") return;

    /* Seed from data-selected and/or from <input> children a template rendered
       with th:each — read both BEFORE the markup is replaced below. */
    var ids = readIds(picker);
    var seeds = picker.querySelectorAll("input");
    for (var i = 0; i < seeds.length; i++) {
      var n = parseInt(seeds[i].value, 10);
      if (!isNaN(n) && ids.indexOf(n) === -1) ids.push(n);
    }
    writeIds(picker, ids);

    picker.dataset.tagPickerReady = "1";
    picker.innerHTML =
      '<button type="button" class="tag-picker-toggle">' +
      '<span class="tag-picker-label"></span>' +
      '<span class="tag-picker-caret">&#9662;</span></button>';
    renderToggle(picker);
    syncHidden(picker);

    picker.querySelector(".tag-picker-toggle").addEventListener("mousedown", function (e) {
      e.preventDefault();
      e.stopPropagation();
      if (activePicker === picker) close();
      else open(picker);
    });
  }

  document.addEventListener("mousedown", close);
  document.addEventListener("keydown", function (e) {
    if (e.key === "Escape") close();
  });
  /* Capture phase so scrolling any ancestor (including .table-center) closes it —
     a fixed menu would otherwise detach from its control. */
  window.addEventListener("scroll", close, true);
  window.addEventListener("resize", close);

  window.TagPicker = {
    initAll: function (root) {
      var scope = unwrap(root) || document;
      var list = scope.querySelectorAll(".tag-picker");
      for (var i = 0; i < list.length; i++) init(list[i]);
      /* querySelectorAll misses the root itself when the root IS a picker. */
      if (scope.classList && scope.classList.contains("tag-picker")) init(scope);
    },

    get: function (el) {
      var picker = unwrap(el);
      return picker ? readIds(picker) : [];
    },

    set: function (el, ids) {
      var picker = unwrap(el);
      if (!picker) return;
      writeIds(picker, (ids || []).map(Number));
      renderToggle(picker);
      syncHidden(picker);
      if (activePicker === picker) open(picker);
    },

    chipsHtml: chipsHtml,

    names: function (items) {
      if (!items || !items.length) return "";
      return items.map(function (item) {
        return (item && typeof item === "object") ? item.name : nameOf(item);
      }).filter(Boolean).join(", ");
    }
  };

  document.addEventListener("DOMContentLoaded", function () {
    window.TagPicker.initAll(document);
  });
})(window, document);
