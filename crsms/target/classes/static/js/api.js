async function api(path, options = {}) {
  const opts = { credentials: "same-origin", headers: {}, ...options };
  if (opts.body && !(opts.body instanceof FormData)) {
    opts.headers["Content-Type"] = "application/json";
    if (typeof opts.body !== "string") opts.body = JSON.stringify(opts.body);
  }
  const response = await fetch(path, opts);
  const text = await response.text();
  let data = null;
  if (text) {
    try { data = JSON.parse(text); } catch { data = { error: text }; }
  }
  if (!response.ok) throw new Error((data && data.error) || response.statusText || "Request failed");
  return data;
}

function toast(message, isError) {
  let el = document.querySelector(".toast");
  if (!el) {
    el = document.createElement("div");
    el.className = "toast";
    document.body.appendChild(el);
  }
  el.style.background = isError ? "#9b2c2c" : "#10243f";
  el.textContent = message;
  clearTimeout(el._t);
  el._t = setTimeout(() => el.remove(), 4200);
}

function badge(status) {
  if (!status) return "";
  const bad = ["REJECTED", "CANCELLED", "EXPIRED", "DAMAGED", "LOST", "DEACTIVATED", "OVERDUE", "UNPAID", "UNDER_MAINTENANCE"];
  const ok = ["APPROVED", "ACTIVE", "AVAILABLE", "RETURNED", "RESOLVED", "GOOD", "NEW", "PAID", "WAIVED", "SIMULATED"];
  const cls = bad.includes(status) ? "bad" : ok.includes(status) ? "ok" : "warn";
  return `<span class="badge ${cls}">${escapeHtml(status)}</span>`;
}

function escapeHtml(value) {
  return String(value ?? "").replace(/[&<>"']/g, (ch) => {
    if (ch === "&") return "\u0026amp;";
    if (ch === "<") return "\u0026lt;";
    if (ch === ">") return "\u0026gt;";
    if (ch === '"') return "\u0026quot;";
    return "\u0026#39;";
  });
}

function money(value) {
  const n = Number(value || 0);
  return n.toLocaleString(undefined, { style: "currency", currency: "USD" });
}

function when(value) {
  if (!value) return "—";
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return escapeHtml(value);
  return d.toLocaleString();
}

function stars(rating) {
  if (rating == null) return `<span class="muted">No reviews yet</span>`;
  const full = Math.round(Number(rating));
  return `<span class="stars">${"●".repeat(full)}${"○".repeat(5 - full)}</span> ${Number(rating).toFixed(1)}`;
}

function emptyRow(cols, text) {
  return `<tr><td colspan="${cols}" class="empty">${escapeHtml(text)}</td></tr>`;
}

const DASH = {
  STUDENT: ["/student.html", "Student desk"],
  FACULTY: ["/faculty.html", "Faculty desk"],
  STAFF: ["/staff.html", "Staff desk"],
  ADMIN: ["/admin.html", "Administration"]
};

async function mountNav() {
  const slot = document.querySelector("[data-nav]");
  let me = null;
  try { me = await api("/api/users/me"); } catch { me = null; }
  const links = [["Home", "/index.html"], ["Browse", "/browse.html"]];
  if (!me) {
    links.push(["Sign in", "/login.html"], ["Register", "/register.html"]);
  } else {
    links.push(
      ["Desk", DASH[me.role]?.[0] || "/student.html"],
      ["Listings", "/my-listings.html"],
      ["Reservations", "/reservations.html"],
      ["Borrowings", "/borrowings.html"],
      ["Penalties", "/penalties.html"],
      ["Alerts", "/notifications.html"],
      ["Profile", "/profile.html"]
    );
    if (me.role === "ADMIN") {
      links.push(["Analytics", "/admin.html"], ["People", "/admin-users.html"], ["Audit", "/admin-audit.html"]);
    }
  }
  if (slot) {
    slot.innerHTML = links.map(([label, href]) => `<a href="${href}">${label}</a>`).join("")
      + (me ? `<button class="btn secondary small" id="logout" type="button">Sign out</button>` : "");
    const btn = document.getElementById("logout");
    if (btn) btn.onclick = async () => {
      await api("/api/auth/logout", { method: "POST" });
      location.href = "/login.html";
    };
  }
  const side = document.querySelector("[data-side]");
  if (side) {
    const sideLinks = me ? links.slice(2) : links;
    side.innerHTML = `<h2>${me ? escapeHtml(me.fullName) : "Explore"}</h2>`
      + sideLinks.map(([label, href]) => {
        const active = location.pathname.endsWith(href) ? "active" : "";
        return `<a class="${active}" href="${href}">${label}</a>`;
      }).join("")
      + (me && me.role === "ADMIN" ? `<h2>Manage</h2>
        <a href="/admin-resources.html">Resources</a>
        <a href="/admin-reservations.html">All reservations</a>
        <a href="/admin-borrowings.html">All borrowings</a>
        <a href="/admin-maintenance.html">Maintenance</a>
        <a href="/admin-audit.html">Audit log</a>
        <a href="/admin-settings.html">Settings</a>
        <a href="/maintenance.html">Report damage</a>
        <a href="/reviews.html">Reviews</a>` : `<a href="/maintenance.html">Report damage</a><a href="/reviews.html">Reviews</a>`);
  }
  const gate = document.body.dataset.auth;
  if (gate === "user" && !me) location.href = "/login.html?next=" + encodeURIComponent(location.pathname);
  if (gate === "admin" && (!me || me.role !== "ADMIN")) {
    toast("Administration is restricted", true);
    location.href = me ? (DASH[me.role]?.[0] || "/browse.html") : "/login.html";
  }
  const expect = document.body.dataset.role;
  if (expect && me && me.role !== expect) location.href = DASH[me.role]?.[0] || "/browse.html";
  return me;
}

document.addEventListener("DOMContentLoaded", () => { mountNav(); });
