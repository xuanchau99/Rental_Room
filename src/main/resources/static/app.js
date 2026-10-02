"use strict";

const $ = (id) => document.getElementById(id);
const state = {tokens: null, rooms: [], tenants: [], contracts: [], services: [], worksheet: [], electricity: [], water: [], invoices: [], dashboard: null};
const money = (value) => `${new Intl.NumberFormat("vi-VN", {maximumFractionDigits: 2}).format(Number(value || 0))} đ`;
const today = () => {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
};
const roomName = (id) => state.rooms.find((room) => room.id === id)?.roomNumber || `#${id}`;
const period = () => ({month: Number($("period-month").value), year: Number($("period-year").value)});
const query = (values) => new URLSearchParams(values).toString();

function node(tag, className, value) {
  const element = document.createElement(tag);
  if (className) element.className = className;
  if (value !== undefined) element.textContent = String(value);
  return element;
}
function setNotice(message, error = false) {
  const element = $("notice");
  element.textContent = message;
  element.classList.toggle("error", error);
  element.hidden = !message;
  if (message) window.scrollTo({top: 0, behavior: "smooth"});
}
function clearSession() {
  state.tokens = null;
  sessionStorage.removeItem("rentalTokens");
  $("app").hidden = true;
  $("login-screen").hidden = false;
  $("password").value = "";
}
function saveSession(tokens) {
  state.tokens = tokens;
  sessionStorage.setItem("rentalTokens", JSON.stringify(tokens));
  $("login-screen").hidden = true;
  $("app").hidden = false;
}
async function refreshToken() {
  if (!state.tokens?.refreshToken) return false;
  try {
    const response = await fetch("/api/v1/auth/refresh", {
      method: "POST", headers: {"Content-Type": "application/json"},
      body: JSON.stringify({refreshToken: state.tokens.refreshToken})
    });
    if (!response.ok) return false;
    saveSession(await response.json());
    return true;
  } catch { return false; }
}
async function api(path, {method = "GET", body, auth = true, retry = true} = {}) {
  const headers = {"Accept": "application/json"};
  if (body !== undefined) headers["Content-Type"] = "application/json";
  if (auth && state.tokens?.accessToken) headers.Authorization = `Bearer ${state.tokens.accessToken}`;
  const response = await fetch(`/api/v1${path}`, {method, headers, body: body === undefined ? undefined : JSON.stringify(body)});
  if (response.status === 401 && auth && retry) {
    if (await refreshToken()) return api(path, {method, body, auth, retry: false});
    clearSession();
    throw new Error("Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.");
  }
  if (!response.ok) {
    const problem = await response.json().catch(() => ({}));
    throw new Error([problem.detail || `Yêu cầu thất bại (${response.status})`, ...(problem.errors || [])].join(" · "));
  }
  return response.status === 204 ? null : response.json();
}
async function perform(button, action, success) {
  button.disabled = true;
  try {
    const result = await action();
    if (success) setNotice(typeof success === "function" ? success(result) : success);
    return result;
  } catch (error) {
    setNotice(error.message || "Không thể hoàn tất thao tác.", true);
    return null;
  } finally { button.disabled = false; }
}
function assertPeriod() {
  const selected = period();
  const current = new Date();
  if (selected.year < 2000 || selected.year > 9999 || selected.month < 1 || selected.month > 12)
    throw new Error("Tháng hoặc năm không hợp lệ.");
  if (selected.year * 12 + selected.month > current.getFullYear() * 12 + current.getMonth() + 1)
    throw new Error("Chưa thể lập hóa đơn cho tháng trong tương lai.");
  return selected;
}
async function reload() {
  const selected = assertPeriod();
  const params = query({...selected, size: 100});
  const [rooms, tenants, contracts, services, dashboard, worksheet, electricity, water, invoices] = await Promise.all([
    api("/rooms?size=100"), api("/tenants?size=100"), api("/contracts?size=100"), api("/services?size=100"),
    api(`/dashboard?${query(selected)}`), api(`/meter-workflow?${query(selected)}`),
    api(`/electricity-readings?${params}`), api(`/water-readings?${params}`), api(`/invoices?${params}`)
  ]);
  Object.assign(state, {rooms, tenants, contracts, services, dashboard, worksheet, electricity, water, invoices});
  render();
}
function emptyRow(body, columns, message) {
  const row = node("tr");
  const cell = node("td", "empty", message);
  cell.colSpan = columns;
  row.append(cell);
  body.append(row);
}
function addCells(row, values) { values.forEach((value) => row.append(node("td", "", value))); }
function badge(status) {
  const label = {VACANT: "Trống", OCCUPIED: "Đang thuê", MAINTENANCE: "Bảo trì",
    UNPAID: "Chưa trả", PARTIALLY_PAID: "Trả một phần", PAID: "Đã trả", OVERDUE: "Quá hạn", CANCELLED: "Đã hủy"}[status] || status;
  return node("span", `badge ${["VACANT", "PAID"].includes(status) ? "good" : ["OVERDUE", "MAINTENANCE"].includes(status) ? "bad" : "warn"}`, label);
}
function renderOverview() {
  const summary = state.dashboard;
  const stats = [
    ["Tổng phòng", summary.totalRooms], ["Đang thuê", summary.occupied], ["Phòng trống", summary.vacant],
    ["Đã lập hóa đơn", money(summary.billed)], ["Đã thu", money(summary.paid)], ["Còn nợ", money(summary.outstanding)]
  ];
  const target = $("stats"); target.replaceChildren();
  stats.forEach(([label, value]) => {
    const card = node("div", "stat"); card.append(node("span", "", label), node("strong", "", value)); target.append(card);
  });
  const debts = $("debt-list"); debts.replaceChildren();
  if (!summary.debts.length) debts.append(node("div", "empty", "Không có hóa đơn còn nợ trong kỳ."));
  summary.debts.forEach((debt) => {
    const row = node("div", "kv"); row.append(node("span", "", debt.roomNumber), node("strong", "", money(debt.outstanding))); debts.append(row);
  });
  const revenue = $("revenue-list"); revenue.replaceChildren();
  [["Tiền phòng", summary.rent], ["Tiền điện", summary.electricity], ["Tiền nước", summary.water],
    ["Dịch vụ (gồm Wi-Fi)", summary.services], ["Khoản khác", summary.other], ["Giảm giá", -Number(summary.discounts || 0)]]
    .forEach(([label, amount]) => {
      const row = node("div", "kv"); row.append(node("span", "", label), node("strong", "", money(amount))); revenue.append(row);
    });
}
function fillSelect(select, entries, placeholder) {
  select.replaceChildren(new Option(placeholder, ""));
  entries.forEach(([label, value]) => select.add(new Option(label, value)));
}
const isWifiName = (name) => /^(wi[\s-]?fi|internet)$/i.test((name || "").trim());
const wifiCatalog = () => state.services.find((service) => service.active && service.calculationType === "FIXED" && service.name.toLowerCase() === "wi-fi");
function wifiSubscription(price) {
  if (price === 0) return null;
  const service = wifiCatalog();
  if (!service) throw new Error("Chưa có dịch vụ Wi-Fi đang hoạt động. Hãy tải lại trang.");
  return {serviceId: service.id, price, quantity: 1};
}
async function loadWifiPrice() {
  const id = $("wifi-contract").value;
  if (!id) { $("wifi-price").value = ""; return; }
  const subscriptions = await api(`/contracts/${id}/services`);
  if ($("wifi-contract").value === id) {
    const wifi = subscriptions.find((item) => isWifiName(item.name));
    $("wifi-price").value = wifi ? wifi.price : 0;
  }
}
function renderPeople() {
  const roomsBody = $("rooms-body"); roomsBody.replaceChildren();
  if (!state.rooms.length) emptyRow(roomsBody, 5, "Chưa có phòng.");
  state.rooms.forEach((room) => {
    const row = node("tr");
    addCells(row, [room.roomNumber, `${room.area || "—"} m²`, [room.hasLoft && "Có gác", room.hasAirConditioner && "Điều hòa"].filter(Boolean).join(", ") || "—"]);
    const status = node("td"); status.append(badge(room.status)); row.append(status);
    row.append(node("td", "", room.monthlyRent == null ? "Chưa đặt" : money(room.monthlyRent)));
    roomsBody.append(row);
  });
  const tenantsBody = $("tenants-body"); tenantsBody.replaceChildren();
  if (!state.tenants.length) emptyRow(tenantsBody, 3, "Chưa có khách thuê.");
  state.tenants.forEach((tenant) => { const row = node("tr"); addCells(row, [tenant.fullName, tenant.phone || "—", tenant.id]); tenantsBody.append(row); });
  fillSelect($("contract-room"), state.rooms.filter((room) => room.status === "VACANT").map((room) => [room.roomNumber, room.id]), "Chọn phòng trống");
  fillSelect($("contract-tenant"), state.tenants.map((tenant) => [tenant.fullName, tenant.id]), "Chọn khách thuê");
  const currentWifiContract = $("wifi-contract").value;
  fillSelect($("wifi-contract"), state.contracts.filter((contract) => contract.status === "ACTIVE")
    .map((contract) => [roomName(contract.roomId), contract.id]), "Chọn phòng đang thuê");
  if (state.contracts.some((contract) => String(contract.id) === currentWifiContract && contract.status === "ACTIVE")) {
    $("wifi-contract").value = currentWifiContract;
    loadWifiPrice().catch((error) => setNotice(error.message, true));
  } else $("wifi-price").value = "";
}
function applicableContract(roomId) {
  const {month, year} = period();
  const first = `${year}-${String(month).padStart(2, "0")}-01`;
  const last = `${year}-${String(month).padStart(2, "0")}-${String(new Date(year, month, 0).getDate()).padStart(2, "0")}`;
  return state.contracts.find((contract) => contract.roomId === roomId && contract.status !== "PENDING" &&
    contract.startDate <= last && (!contract.endDate || contract.endDate >= first));
}
function meterInput(label, value, disabled = false) {
  const wrapper = node("label", "", label);
  const input = node("input"); input.type = "number"; input.min = "0"; input.step = "0.001";
  input.value = value ?? ""; input.disabled = disabled; input.required = true;
  wrapper.append(input); return {wrapper, input};
}
function meterBlock(title, existing, oldReading, mode, roomId, kind) {
  const block = node("form", "meter-block"); block.append(node("h4", "", title));
  if (existing) {
    block.append(node("div", "saved", `Đã lưu · ${kind === "water" && mode === "PER_PERSON" ? `${existing.personCount} người` : `${existing.usage} đơn vị`} · ${money(existing.amount)}`));
    return block;
  }
  let old;
  if (kind === "water" && mode === "PER_PERSON") {
    const people = meterInput("Số người", 1); people.input.min = "1"; people.input.step = "1";
    block.append(people.wrapper); old = people.input;
  } else {
    const previous = meterInput("Chỉ số cũ", oldReading, oldReading != null);
    const current = meterInput("Chỉ số mới", "");
    block.append(previous.wrapper, current.wrapper);
    old = previous.input;
    block.dataset.currentInput = "true";
  }
  const submit = node("button", "secondary small-button", "Lưu chỉ số"); submit.type = "submit"; block.append(submit);
  block.addEventListener("submit", async (event) => {
    event.preventDefault();
    const {month, year} = period();
    const payload = {roomId, month, year};
    if (kind === "water" && mode === "PER_PERSON") payload.personCount = Number(old.value);
    else {
      const current = block.querySelectorAll("input")[1];
      payload.currentReading = Number(current.value);
      payload.previousReading = old.value === "" ? null : Number(old.value);
    }
    const result = await perform(submit, () => api(kind === "water" ? "/water-readings" : "/electricity-readings", {method: "POST", body: payload}), "Đã lưu chỉ số.");
    if (result) await perform(submit, reload);
  });
  return block;
}
function renderMeters() {
  const target = $("meter-list"); target.replaceChildren();
  state.worksheet.forEach((entry) => {
    const card = node("article", "panel meter-card");
    card.append(node("h3", "", entry.roomNumber));
    const contract = applicableContract(entry.roomId);
    if (!contract) { card.append(node("p", "muted", "Không có hợp đồng cho kỳ này.")); target.append(card); return; }
    card.append(node("p", "room-meta", `Nước: ${contract.waterCalculationType === "METER" ? "theo đồng hồ" : "theo người"} · Giá phòng ${money(contract.monthlyRent)}/tháng`));
    const columns = node("div", "meter-columns");
    columns.append(
      meterBlock("Điện (kWh)", state.electricity.find((item) => item.roomId === entry.roomId), entry.previousElectricityReading, "METER", entry.roomId, "electricity"),
      meterBlock("Nước", state.water.find((item) => item.roomId === entry.roomId), entry.previousWaterReading, contract.waterCalculationType, entry.roomId, "water")
    );
    card.append(columns); target.append(card);
  });
  if (!state.worksheet.length) target.append(node("p", "empty", "Chưa có phòng để nhập chỉ số."));
}
function renderInvoices() {
  const body = $("invoices-body"); body.replaceChildren();
  $("invoice-detail").hidden = true;
  if (!state.invoices.length) emptyRow(body, 8, "Chưa có hóa đơn cho tháng này.");
  state.invoices.forEach((invoice) => {
    const row = node("tr");
    addCells(row, [roomName(invoice.roomId), invoice.number]);
    [invoice.serviceAmount, invoice.totalAmount, invoice.paidAmount, invoice.outstandingAmount]
      .forEach((amount) => row.append(node("td", "money", money(amount))));
    const status = node("td"); status.append(badge(invoice.status)); row.append(status);
    const actions = node("td");
    const detailButton = node("button", "ghost small-button", "Chi tiết"); detailButton.type = "button";
    detailButton.addEventListener("click", async () => {
      const detail = await perform(detailButton, () => api(`/invoices/${invoice.id}`));
      if (!detail) return;
      $("invoice-detail-title").textContent = `Chi tiết hóa đơn · ${roomName(detail.roomId)} · ${detail.month}/${detail.year}`;
      const items = $("invoice-items"); items.replaceChildren();
      detail.items.forEach((item) => {
        const line = node("div", "kv");
        line.append(node("span", "", `${item.description} (${item.quantity} × ${money(item.unitPrice)})`),
          node("strong", "", money(item.amount)));
        items.append(line);
      });
      const total = node("div", "kv"); total.append(node("strong", "", "Tổng hóa đơn"), node("strong", "", money(detail.totalAmount))); items.append(total);
      $("invoice-detail").hidden = false;
      $("invoice-detail").scrollIntoView({behavior: "smooth", block: "center"});
    });
    actions.append(detailButton, " ");
    if (Number(invoice.outstandingAmount) > 0 && invoice.status !== "CANCELLED") {
      const button = node("button", "secondary small-button", "Ghi thanh toán"); button.type = "button";
      button.addEventListener("click", () => {
        $("payment-form").hidden = false;
        $("payment-form").dataset.invoiceId = invoice.id;
        $("payment-title").textContent = `Ghi thanh toán · ${roomName(invoice.roomId)}`;
        $("payment-amount").value = invoice.outstandingAmount;
        $("payment-amount").max = invoice.outstandingAmount;
        $("payment-form").scrollIntoView({behavior: "smooth", block: "center"});
      }); actions.append(button);
    }
    row.append(actions); body.append(row);
  });
}
function render() { renderOverview(); renderPeople(); renderMeters(); renderInvoices(); }

$("login-form").addEventListener("submit", async (event) => {
  event.preventDefault();
  const button = event.currentTarget.querySelector("button"); button.disabled = true;
  $("login-error").hidden = true;
  try {
    const tokens = await api("/auth/login", {method: "POST", auth: false,
      body: {username: $("username").value.trim(), password: $("password").value}});
    saveSession(tokens);
    try { await reload(); } catch (error) { setNotice(error.message, true); }
  } catch (error) {
    $("login-error").textContent = error.message;
    $("login-error").hidden = false;
    if (!state.tokens) clearSession();
  } finally { button.disabled = false; }
});
$("logout-button").addEventListener("click", async () => {
  const token = state.tokens?.refreshToken;
  if (token) await api("/auth/logout", {method: "POST", body: {refreshToken: token}, auth: false}).catch(() => {});
  clearSession(); setNotice("");
});
$("refresh-button").addEventListener("click", (event) => perform(event.currentTarget, reload, "Dữ liệu đã được làm mới."));
$("period-month").addEventListener("change", () => perform($("refresh-button"), reload));
$("period-year").addEventListener("change", () => perform($("refresh-button"), reload));
document.querySelectorAll(".tab").forEach((tab) => tab.addEventListener("click", () => {
  document.querySelectorAll(".tab").forEach((item) => item.classList.toggle("active", item === tab));
  document.querySelectorAll(".page").forEach((page) => page.classList.toggle("active", page.id === tab.dataset.tab));
  setNotice("");
}));
$("tenant-form").addEventListener("submit", async (event) => {
  event.preventDefault();
  const button = event.currentTarget.querySelector("button");
  const result = await perform(button, () => api("/tenants", {method: "POST", body: {
    fullName: $("tenant-name").value.trim(), phone: $("tenant-phone").value.trim() || null
  }}), "Đã thêm khách thuê.");
  if (result) { event.currentTarget.reset(); await perform(button, reload); }
});
$("wifi-contract").addEventListener("change", () => loadWifiPrice().catch((error) => setNotice(error.message, true)));
$("wifi-form").addEventListener("submit", async (event) => {
  event.preventDefault();
  const button = event.currentTarget.querySelector("button[type=submit]");
  const contractId = $("wifi-contract").value;
  const price = Number($("wifi-price").value);
  const result = await perform(button, async () => {
    const current = await api(`/contracts/${contractId}/services`);
    const existingWifi = current.filter((item) => isWifiName(item.name));
    if (existingWifi.length > 1) throw new Error("Hợp đồng có nhiều dịch vụ Internet/Wi-Fi. Hãy kiểm tra trong Swagger trước khi sửa.");
    const others = current.filter((item) => !isWifiName(item.name))
      .map((item) => ({serviceId: item.serviceId, price: item.price, quantity: item.quantity}));
    if (price > 0) {
      const serviceId = existingWifi[0]?.serviceId || wifiSubscription(price).serviceId;
      others.push({serviceId, price, quantity: 1});
    }
    return api(`/contracts/${contractId}/services`, {method: "PUT", body: others});
  }, "Đã cập nhật Wi-Fi cho các hóa đơn chưa lập.");
  if (result) await perform(button, reload);
});
$("contract-form").addEventListener("submit", async (event) => {
  event.preventDefault(); const button = event.currentTarget.querySelector("button");
  const result = await perform(button, () => {
    const wifi = wifiSubscription(Number($("contract-wifi").value));
    return api("/contracts", {method: "POST", body: {
      roomId: Number($("contract-room").value), tenantId: Number($("contract-tenant").value),
      startDate: $("contract-start").value, endDate: null, monthlyRent: Number($("contract-rent").value),
      depositAmount: Number($("contract-deposit").value), status: "ACTIVE", paymentDueDay: Number($("contract-due").value),
      note: null, electricityPrice: Number($("contract-electric").value),
      waterCalculationType: $("contract-water-mode").value, waterPrice: Number($("contract-water-price").value),
      services: wifi ? [wifi] : []
    }});
  }, "Đã tạo hợp đồng cùng khoản Wi-Fi và chuyển phòng sang Đang thuê.");
  if (result) { event.currentTarget.reset(); $("contract-start").value = today(); await perform(button, reload); }
});
$("generate-button").addEventListener("click", async (event) => {
  const button = event.currentTarget;
  const result = await perform(button, () => api("/invoices/generate-monthly", {method: "POST", body: {...period(), propertyId: null}}),
    (value) => `Đã tạo ${value.created.length} hóa đơn; đã có ${value.existing.length}.` +
      (Object.keys(value.failed).length ? ` Cần kiểm tra: ${Object.entries(value.failed).map(([id, reason]) => `${roomName(Number(id))}: ${reason}`).join("; ")}` : ""));
  if (result) await perform(button, reload);
});
$("payment-form").addEventListener("submit", async (event) => {
  event.preventDefault(); const button = event.currentTarget.querySelector("button[type=submit]");
  const id = event.currentTarget.dataset.invoiceId;
  const result = await perform(button, () => api(`/invoices/${id}/payments`, {method: "POST", body: {
    amount: Number($("payment-amount").value), paymentMethod: $("payment-method").value,
    paymentDate: new Date().toISOString(), referenceCode: $("payment-reference").value.trim() || null, note: null
  }}), "Đã ghi nhận thanh toán.");
  if (result) { event.currentTarget.reset(); event.currentTarget.hidden = true; await perform(button, reload); }
});
$("payment-cancel").addEventListener("click", () => { $("payment-form").hidden = true; });

const now = new Date();
for (let month = 1; month <= 12; month++) $("period-month").add(new Option(`Tháng ${month}`, month));
$("period-month").value = String(now.getMonth() + 1);
$("period-year").value = String(now.getFullYear());
$("contract-start").value = today();
try { state.tokens = JSON.parse(sessionStorage.getItem("rentalTokens") || "null"); } catch { state.tokens = null; }
if (state.tokens) {
  $("login-screen").hidden = true; $("app").hidden = false;
  reload().catch((error) => setNotice(error.message, true));
}
