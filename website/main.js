(function () {
  var toggle = document.querySelector(".nav-toggle");
  var links = document.querySelector(".nav-links");
  if (!toggle || !links) return;

  toggle.addEventListener("click", function () {
    var open = links.classList.toggle("is-open");
    toggle.setAttribute("aria-expanded", open ? "true" : "false");
  });

  links.querySelectorAll("a").forEach(function (a) {
    a.addEventListener("click", function () {
      links.classList.remove("is-open");
      toggle.setAttribute("aria-expanded", "false");
    });
  });
})();

(function () {
  var form = document.getElementById("delete-account-form");
  if (!form) return;

  var panel = document.getElementById("delete-form-panel");
  var success = document.getElementById("delete-success-panel");
  var errEl = document.getElementById("delete-form-error");
  var supportEmail = "support@soundamplifier.app";

  var reasonLabels = {
    "": "",
    no_longer_using: "No longer using the app",
    privacy: "Privacy concerns",
    alternative: "Found a better alternative",
    other: "Other",
  };

  form.addEventListener("submit", function (e) {
    e.preventDefault();
    if (errEl) {
      errEl.hidden = true;
      errEl.textContent = "";
    }

    var email = (document.getElementById("delete-email") || {}).value || "";
    email = email.trim();
    var reasonSel = document.getElementById("delete-reason");
    var reason = reasonSel ? reasonSel.value : "";
    var comments = (document.getElementById("delete-comments") || {}).value || "";
    var confirmCb = document.getElementById("delete-confirm");

    if (!email || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      if (errEl) {
        errEl.textContent = "Please enter a valid email address associated with your account.";
        errEl.hidden = false;
      }
      return;
    }

    if (!confirmCb || !confirmCb.checked) {
      if (errEl) {
        errEl.textContent = "Please confirm that you understand this action is irreversible.";
        errEl.hidden = false;
      }
      return;
    }

    var body =
      "SunoX — Account deletion request\r\n\r\n" +
      "Email: " +
      email +
      "\r\n" +
      "Reason: " +
      (reasonLabels[reason] || reason || "(not specified)") +
      "\r\n\r\n" +
      "Comments:\r\n" +
      (comments.trim() || "(none)") +
      "\r\n";

    var subject = encodeURIComponent("SunoX account deletion request");
    var mailto =
      "mailto:" +
      supportEmail +
      "?subject=" +
      subject +
      "&body=" +
      encodeURIComponent(body);

    try {
      window.location.href = mailto;
    } catch (ignore) {}

    if (panel) panel.hidden = true;
    if (success) success.hidden = false;
    success.scrollIntoView({ behavior: "smooth", block: "nearest" });
  });
})();
