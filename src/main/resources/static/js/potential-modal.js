/* The AI "potential" dialog, shared by every page with a card table.
   Markup lives in templates/fragments/potentialModal.html; pages only call
   PotentialModal.open(cardId) from their own click handler, because some bind
   directly and some delegate from #result. */
(function ($) {
  "use strict";

  var SELECTOR = "#potential-modal";

  function $modal() {
    return $(SELECTOR);
  }

  // The AI answers in light markdown: **bold** and a Low/Medium/High rating.
  function formatAnalysis(text) {
    var escaped = $("<div>").text(text).html();
    var formatted = escaped.replace(
      /\*\*Potential Rating:\*\*\s*(Low|Medium|High)/gi,
      function (match, rating) {
        return '<strong>Potential Rating:</strong> <span class="rating-' +
          rating.toLowerCase() + '">' + rating + "</span>";
      }
    );
    formatted = formatted.replace(/\*\*(.*?)\*\*/g, "<strong>$1</strong>");
    formatted = formatted.replace(/\n/g, "<br>");
    return formatted;
  }

  function open(cardId) {
    var $m = $modal();
    if (!$m.length) {
      return;
    }

    $("#potential-modal-body").html(
      '<span class="rg-modal__loading">' +
        '<span class="loading loading-dots loading-sm"></span>' +
        "Asking the AI about this card…" +
      "</span>"
    );

    $m.addClass("modal-open").attr("aria-hidden", "false");
    $("body").addClass("rg-modal-lock");

    $.ajax({
      url: "/cardRest/" + cardId + "/potential",
      type: "GET",
      success: function (response) {
        var data = typeof response === "string" ? JSON.parse(response) : response;
        $("#potential-modal-body").html(
          formatAnalysis(data.analysis || data.error || "No response.")
        );
      },
      error: function () {
        $("#potential-modal-body").text("Failed to fetch analysis.");
      }
    });
  }

  function close() {
    $modal().removeClass("modal-open").attr("aria-hidden", "true");
    $("body").removeClass("rg-modal-lock");
  }

  $(function () {
    $(document).on(
      "click",
      "#potential-modal-close, #potential-modal-x, #potential-modal-backdrop",
      close
    );

    $(document).on("keydown", function (e) {
      if (e.key === "Escape" && $modal().hasClass("modal-open")) {
        close();
      }
    });
  });

  window.PotentialModal = { open: open, close: close, format: formatAnalysis };
})(jQuery);
