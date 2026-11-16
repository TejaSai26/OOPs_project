import { chromium } from "playwright";
import { mkdirSync } from "node:fs";

const base = "http://127.0.0.1:8080";
mkdirSync("/workspace/screenshots", { recursive: true });
const browser = await chromium.launch({ headless: true });
const page = await browser.newPage({ viewport: { width: 1280, height: 800 } });
const errors = [];
page.on("pageerror", (err) => errors.push("PAGE " + page.url() + " " + err.message));
page.on("console", (msg) => {
  if (msg.type() === "error") errors.push("CONSOLE " + page.url() + " " + msg.text());
});

async function shot(name) {
  await page.screenshot({ path: `/workspace/screenshots/${name}.png`, fullPage: true });
}

async function login(email, password) {
  await page.goto(base + "/login.html", { waitUntil: "networkidle" });
  await page.fill('input[name="email"]', email);
  await page.fill('input[name="password"]', password);
  await page.click('button[type="submit"]');
  await page.waitForURL((url) => !url.pathname.endsWith("/login.html"), { timeout: 10000 });
}

const publicPages = ["/", "/browse.html", "/listing.html?id=1", "/register.html", "/login.html"];
for (const path of publicPages) {
  await page.goto(base + path, { waitUntil: "networkidle" });
  const text = await page.locator("body").innerText();
  if (text.trim().length < 40) errors.push("THIN " + path);
}
await shot("01-home");
await page.goto(base + "/browse.html", { waitUntil: "networkidle" });
await page.waitForTimeout(400);
await shot("02-browse");
await page.goto(base + "/listing.html?id=1", { waitUntil: "networkidle" });
await page.waitForTimeout(400);
await shot("03-listing");

await login("bob@campus.edu", "Student@123");
await shot("04-student-bob");
const userPages = ["/my-listings.html", "/reservations.html", "/borrowings.html", "/penalties.html", "/notifications.html", "/maintenance.html", "/reviews.html", "/profile.html", "/listing-form.html"];
for (const path of userPages) {
  await page.goto(base + path, { waitUntil: "networkidle" });
  const text = await page.locator("body").innerText();
  if (/Sign in/.test(await page.title()) && path !== "/login.html") errors.push("BOUNCED " + path);
  if (text.trim().length < 20) errors.push("THIN " + path);
}
await page.setViewportSize({ width: 390, height: 844 });
await page.goto(base + "/browse.html", { waitUntil: "networkidle" });
await shot("05-browse-mobile");
await page.setViewportSize({ width: 1280, height: 800 });

await page.goto(base + "/listing.html?id=1", { waitUntil: "networkidle" });
const start = new Date(Date.now() + 2 * 3600 * 1000);
const end = new Date(Date.now() + 4 * 3600 * 1000);
const local = (d) => {
  const pad = (n) => String(n).padStart(2, "0");
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
};
await page.selectOption("#unit", { index: 0 });
await page.fill("#start", local(start));
await page.fill("#end", local(end));
await page.click("#check");
await page.waitForTimeout(500);
await page.click("#book");
await page.waitForURL("**/reservations.html", { timeout: 10000 });
const mine = await page.locator("#mine").innerText();
if (!/REQUESTED|Campus cruiser/.test(mine)) errors.push("RESERVATION MISSING " + mine);

await login("alice@campus.edu", "Student@123");
await page.goto(base + "/reservations.html", { waitUntil: "networkidle" });
await page.waitForTimeout(300);
const approve = page.locator("#incoming button[data-act='approve']").first();
if (await approve.count()) {
  await approve.click();
  await page.waitForTimeout(600);
} else errors.push("NO APPROVE BUTTON");
await shot("06-alice-reservations");

await login("bob@campus.edu", "Student@123");
await page.goto(base + "/reservations.html", { waitUntil: "networkidle" });
const pickup = page.locator("button[data-act='pickup']").first();
if (await pickup.count()) {
  await pickup.click();
  await page.waitForURL("**/borrowings.html", { timeout: 10000 });
} else errors.push("NO PICKUP");
await page.locator("button.ret").first().click();
await page.selectOption('select[name="condition"]', "GOOD");
await page.click('#return-form button[type="submit"]');
await page.waitForTimeout(800);
const loans = await page.locator("#rows").innerText();
if (!/RETURNED/.test(loans)) errors.push("RETURN FAILED " + loans);
await shot("07-returned");

await login("admin@campus.edu", "changeme");
const adminPages = ["/admin.html", "/admin-users.html", "/admin-resources.html", "/admin-reservations.html", "/admin-borrowings.html", "/admin-maintenance.html", "/admin-audit.html", "/admin-settings.html"];
for (const path of adminPages) {
  await page.goto(base + path, { waitUntil: "networkidle" });
  await page.waitForTimeout(400);
  const text = await page.locator("body").innerText();
  if (text.includes("Administration is restricted") || text.length < 30) errors.push("ADMIN THIN " + path + " " + text.slice(0, 80));
}
await page.goto(base + "/admin.html", { waitUntil: "networkidle" });
await page.waitForTimeout(800);
await shot("08-admin");
await page.goto(base + "/admin-audit.html", { waitUntil: "networkidle" });
await shot("09-audit");

await browser.close();
const interesting = errors.filter((line) => !line.includes("favicon") && !line.includes("Failed to load resource") || line.includes("PAGE") || line.includes("THIN") || line.includes("FAILED") || line.includes("MISSING") || line.includes("BOUNCED") || line.includes("ADMIN"));
console.log(JSON.stringify({ errors, interestingCount: interesting.length }, null, 2));
if (errors.some((line) => /THIN|FAILED|MISSING|BOUNCED|ADMIN THIN|PAGE /.test(line))) process.exit(1);
