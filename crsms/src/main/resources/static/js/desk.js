async function bootDesk(title, intro) {
  const me = await mountNav();
  if (!me) return;
  document.getElementById("desk-title").textContent = title;
  document.getElementById("desk-intro").textContent = intro;
  const summary = await api("/api/users/me/dashboard");
  const cards = [
    ["Open reservations", summary.myOpenReservations],
    ["Out on loan", summary.myActiveBorrowings],
    ["Unread alerts", summary.unreadNotifications],
    ["My listings", summary.myListings],
    ["Incoming requests", summary.incomingRequests],
    ["Penalties due", money(summary.outstandingPenalties)]
  ];
  document.getElementById("desk-stats").innerHTML = cards.map(([label, value]) =>
    `<article class="card stat"><strong>${value}</strong><span>${label}</span></article>`).join("");
  const reservations = await api("/api/reservations/my");
  const borrowings = await api("/api/borrowings/my");
  document.getElementById("desk-reservations").innerHTML = reservations.slice(0, 5).map((row) =>
    `<tr><td>${escapeHtml(row.listingTitle)}</td><td>${escapeHtml(row.assetCode)}</td><td>${when(row.startAt)}</td><td>${badge(row.status)}</td></tr>`
  ).join("") || emptyRow(4, "No reservations yet. Browse the catalogue to request one.");
  document.getElementById("desk-borrowings").innerHTML = borrowings.slice(0, 5).map((row) =>
    `<tr><td>${escapeHtml(row.listingTitle)}</td><td>${when(row.dueAt)}</td><td>${money(row.chargeAmount)}</td><td>${badge(row.status)}</td></tr>`
  ).join("") || emptyRow(4, "Nothing is currently on loan.");
}
