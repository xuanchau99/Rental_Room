"use strict";

const $ = (id) => document.getElementById(id);
const state = {tokens: null, properties: [], rooms: [], tenants: [], contracts: [], services: [], serviceUsages: [], users: [], worksheet: [], electricity: [], water: [], invoices: [], reminders: [], dashboard: null};
const owner = () => {
  try {
    const role = JSON.parse(atob(state.tokens.accessToken.split(".")[1].replace(/-/g, "+").replace(/_/g, "/"))).role;
    return role === "OWNER" || role === "SUPER_ADMIN";
  }
  catch { return false; }
};
const money = (value) => {
  const amount = Number(value || 0);
  return `${new Intl.NumberFormat("vi-VN", {maximumFractionDigits: 0}).format(Math.sign(amount) * Math.ceil(Math.abs(amount)))} đ`;
};
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
function decodeJwt(token) {
  if (!token) return null;
  try {
    const base64Url = token.split(".")[1];
    const base64 = base64Url.replace(/-/g, "+").replace(/_/g, "/");
    const jsonPayload = decodeURIComponent(
      atob(base64)
        .split("")
        .map((c) => "%" + ("00" + c.charCodeAt(0).toString(16)).slice(-2))
        .join("")
    );
    return JSON.parse(jsonPayload);
  } catch {
    try {
      return JSON.parse(atob(token.split(".")[1].replace(/-/g, "+").replace(/_/g, "/")));
    } catch {
      return null;
    }
  }
}
function updateUserBadge() {
  const payload = decodeJwt(state.tokens?.accessToken);
  const nameEl = $("user-display-name");
  const roleEl = $("user-display-role");
  const badgeEl = $("user-badge");
  if (!nameEl || !roleEl) return;
  if (!payload) {
    nameEl.textContent = "---";
    roleEl.textContent = "---";
    return;
  }
  const roleMap = {
    SUPER_ADMIN: "Quản trị viên",
    OWNER: "Chủ trọ",
    STAFF: "Nhân viên"
  };
  const roleText = roleMap[payload.role] || payload.role || "Người dùng";
  const displayName = payload.fullName || payload.sub || "Người dùng";
  nameEl.textContent = displayName;
  roleEl.textContent = roleText;
  if (badgeEl) {
    badgeEl.title = `Tài khoản: ${displayName} (${roleText})`;
  }
}
function clearSession() {
  state.tokens = null;
  sessionStorage.removeItem("rentalTokens");
  $("app").hidden = true;
  $("login-screen").hidden = false;
  $("password").value = "";
  updateUserBadge();
}
function saveSession(tokens) {
  state.tokens = tokens;
  sessionStorage.setItem("rentalTokens", JSON.stringify(tokens));
  $("login-screen").hidden = true;
  $("app").hidden = false;
  updateUserBadge();
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
function performMutation(button, action, success) {
  return perform(button, async () => {
    const result = await action();
    await reload();
    return result;
  }, success);
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
let reloadVersion = 0;
async function reload() {
  const version = ++reloadVersion;
  const selected = assertPeriod();
  const params = query({...selected, size: 100});
  const [properties, rooms, tenants, contracts, services, dashboard, worksheet, electricity, water, invoices, reminders, serviceUsages, users] = await Promise.all([
    api("/properties").catch(() => []),
    api("/rooms?size=100").catch(() => []),
    api("/tenants?size=100").catch(() => []),
    api("/contracts?size=100").catch(() => []),
    api("/services?size=100").catch(() => []),
    api(`/dashboard?${query(selected)}`).catch(() => ({
      totalRooms: 0, occupied: 0, vacant: 0, billed: 0, paid: 0, outstanding: 0,
      debts: [], rent: 0, electricity: 0, water: 0, services: 0, other: 0, discounts: 0
    })),
    api(`/meter-workflow?${query(selected)}`).catch(() => []),
    api(`/electricity-readings?${params}`).catch(() => []),
    api(`/water-readings?${params}`).catch(() => []),
    api(`/invoices?${params}`).catch(() => []),
    api("/reminders?dueWithinDays=7").catch(() => []),
    api("/service-usages?size=100").catch(() => []),
    owner() ? api("/users").catch(() => []) : Promise.resolve([])
  ]);
  if (version !== reloadVersion) return;
  Object.assign(state, {properties, rooms, tenants, contracts, services, dashboard, worksheet, electricity, water, invoices, reminders, serviceUsages, users});
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
  const summary = state.dashboard || {totalRooms: 0, occupied: 0, vacant: 0, maintenance: 0, billed: 0, paid: 0, outstanding: 0, debts: [], rent: 0, electricity: 0, water: 0, services: 0, other: 0, discounts: 0};
  const stats = [
    {label: "Tổng phòng", value: summary.totalRooms || 0, group: "room", color: "slate", icon: `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="m3 9 9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/><polyline points="9 22 9 12 15 12 15 22"/></svg>`},
    {label: "Đang thuê", value: summary.occupied || 0, group: "room", color: "emerald", icon: `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><polyline points="16 11 18 13 22 9"/></svg>`},
    {label: "Phòng trống", value: summary.vacant || 0, group: "room", color: "amber", icon: `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="M13 4h3a2 2 0 0 1 2 2v14"/><path d="M2 20h20"/><path d="M13 20V4a2 2 0 0 0-2-2H5a2 2 0 0 0-2 2v16"/><circle cx="9" cy="12" r="1"/></svg>`},
    {label: "Đang bảo trì", value: summary.maintenance || 0, group: "room", color: "purple", icon: `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="M14.7 6.3a1 1 0 0 0 0 1.4l1.6 1.6a1 1 0 0 0 1.4 0l3.77-3.77a6 6 0 0 1-7.94 7.94l-6.91 6.91a2.12 2.12 0 0 1-3-3l6.91-6.91a6 6 0 0 1 7.94-7.94l-3.76 3.76z"/></svg>`},
    {label: "Đã lập hóa đơn", value: money(summary.billed), group: "finance", color: "blue", icon: `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="M4 2v20l2-1 2 1 2-1 2 1 2-1 2 1 2-1 2 1V2l-2 1-2-1-2 1-2-1-2 1-2-1-2 1Z"/><path d="M16 8h-8"/><path d="M16 12h-8"/><path d="M12 16h-4"/></svg>`},
    {label: "Đã thu", value: money(summary.paid), group: "finance", color: "emerald", icon: `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="M19 7V4a1 1 0 0 0-1-1H5a2 2 0 0 0 0 4h15a1 1 0 0 1 1 1v4h-3a2 2 0 0 0 0 4h3a1 1 0 0 0 1-1v-2a1 1 0 0 0-1-1"/><path d="M3 5v14a2 2 0 0 0 2 2h15a1 1 0 0 0 1-1v-4"/></svg>`},
    {label: "Còn nợ", value: money(summary.outstanding), group: "finance", color: "rose", icon: `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>`}
  ];
  const target = $("stats"); target.replaceChildren();
  stats.forEach(({label, value, group, color, icon}) => {
    const card = node("div", `stat stat-${group} stat-${color}`);
    const header = node("div", "stat-header");
    header.append(node("span", "stat-label", label));
    const iconWrapper = node("div", `stat-icon stat-icon-${color}`);
    iconWrapper.innerHTML = icon;
    header.append(iconWrapper);
    card.append(header, node("strong", "stat-value", value));
    target.append(card);
  });
  const debts = $("debt-list"); debts.replaceChildren();
  if (!summary.debts || !summary.debts.length) debts.append(node("div", "empty", "Không có hóa đơn còn nợ trong kỳ."));
  (summary.debts || []).forEach((debt) => {
    const row = node("div", "kv kv-with-icon");
    const labelSpan = node("span", "kv-label");
    const iconEl = node("span", "kv-icon icon-rose");
    iconEl.innerHTML = `<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>`;
    labelSpan.append(iconEl, document.createTextNode(`Phòng ${debt.roomNumber}`));
    row.append(labelSpan, node("strong", "text-bad", money(debt.outstanding)));
    debts.append(row);
  });
  const revenue = $("revenue-list"); revenue.replaceChildren();
  const revenueItems = [
    ["Tiền phòng", summary.rent, `<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="m3 9 9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/></svg>`],
    ["Tiền điện", summary.electricity, `<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/></svg>`],
    ["Tiền nước", summary.water, `<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 2.69l5.66 5.66a8 8 0 1 1-11.31 0z"/></svg>`],
    ["Dịch vụ (Wi-Fi)", summary.services, `<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M5 12.55a11 11 0 0 1 14.08 0"/><path d="M1.42 9a16 16 0 0 1 21.16 0"/><path d="M8.53 16.11a6 6 0 0 1 6.95 0"/><line x1="12" y1="20" x2="12.01" y2="20"/></svg>`],
    ["Khoản khác", summary.other, `<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="16"/><line x1="8" x2="12" y1="12" y2="12"/></svg>`],
    ["Giảm giá", -Number(summary.discounts || 0), `<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="m19 5-7 7-7-7"/></svg>`]
  ];
  revenueItems.forEach(([label, amount, iconSvg]) => {
    const row = node("div", "kv kv-with-icon");
    const labelSpan = node("span", "kv-label");
    const iconEl = node("span", "kv-icon");
    iconEl.innerHTML = iconSvg;
    labelSpan.append(iconEl, document.createTextNode(label));
    row.append(labelSpan, node("strong", amount < 0 ? "text-good" : "", money(amount)));
    revenue.append(row);
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
  if (!state.tenants.length) emptyRow(tenantsBody, 4, "Chưa có khách thuê.");
  state.tenants.forEach((tenant) => {
    const row = node("tr");
    addCells(row, [tenant.fullName, tenant.phone || "—", tenant.id]);
    const cell = node("td");
    const button = node("button", "ghost small-button", "Sửa");
    button.type = "button";
    button.setAttribute("aria-label", `Sửa thông tin ${tenant.fullName}`);
    button.addEventListener("click", () => tenantEditor(tenant));
    cell.append(button); row.append(cell); tenantsBody.append(row);
  });
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
    await performMutation(submit, () => api(kind === "water" ? "/water-readings" : "/electricity-readings", {method: "POST", body: payload}), "Đã lưu chỉ số.");
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
  const remindersList = $("reminders-list");
  if (remindersList) {
    remindersList.replaceChildren();
    if (!state.reminders || !state.reminders.length) {
      remindersList.className = "empty";
      remindersList.textContent = "Chưa có dữ liệu.";
    } else {
      remindersList.className = "";
      state.reminders.forEach((r) => {
        const item = node("div", "kv");
        item.append(node("span", "", `Phòng ${roomName(r.roomId)} (${r.number}) · Hạn ${r.dueDate}`), node("strong", "", money(r.outstanding)));
        remindersList.append(item);
      });
    }
  }
  const singleRoom = $("single-invoice-room");
  if (singleRoom) {
    fillSelect(singleRoom, state.rooms.map(r => [r.roomNumber, r.id]), "Chọn phòng");
  }
}
function render() { renderOverview(); renderPeople(); renderMeters(); renderInvoices(); window.renderExtras?.(); }

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
  const result = await performMutation(button, () => api("/tenants", {method: "POST", body: {
    fullName: $("tenant-name").value.trim(), phone: $("tenant-phone").value.trim() || null
  }}), "Đã thêm khách thuê.");
  if (result) event.currentTarget.reset();
});
$("wifi-contract").addEventListener("change", () => loadWifiPrice().catch((error) => setNotice(error.message, true)));
$("wifi-form").addEventListener("submit", async (event) => {
  event.preventDefault();
  const button = event.currentTarget.querySelector("button[type=submit]");
  const contractId = $("wifi-contract").value;
  const price = Number($("wifi-price").value);
  await performMutation(button, async () => {
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
});
$("contract-form").addEventListener("submit", async (event) => {
  event.preventDefault(); const button = event.currentTarget.querySelector("button");
  const result = await performMutation(button, () => {
    const wifi = wifiSubscription(Number($("contract-wifi").value));
    return api("/contracts", {method: "POST", body: {
      roomId: Number($("contract-room").value), tenantId: Number($("contract-tenant").value),
      startDate: $("contract-start").value, endDate: $("contract-end").value || null, monthlyRent: Number($("contract-rent").value),
      depositAmount: Number($("contract-deposit").value), status: $("contract-status").value, paymentDueDay: Number($("contract-due").value),
      note: $("contract-note").value.trim() || null, electricityPrice: Number($("contract-electric").value),
      waterCalculationType: $("contract-water-mode").value, waterPrice: Number($("contract-water-price").value),
      services: wifi ? [wifi] : []
    }});
  }, () => $("contract-status").value === "PENDING"
    ? "Đã tạo hợp đồng chờ; phòng sẽ được thuê khi kích hoạt."
    : "Đã tạo hợp đồng và chuyển phòng sang Đang thuê.");
  if (result) { event.currentTarget.reset(); $("contract-start").value = today(); }
});
$("generate-button").addEventListener("click", async (event) => {
  const button = event.currentTarget;
  await performMutation(button, () => api("/invoices/generate-monthly", {method: "POST", body: {...period(), propertyId: null}}),
    (value) => `Đã tạo ${value.created.length} hóa đơn; đã có ${value.existing.length}.` +
      (Object.keys(value.failed).length ? ` Cần kiểm tra: ${Object.entries(value.failed).map(([id, reason]) => `${roomName(Number(id))}: ${reason}`).join("; ")}` : ""));
});
$("payment-form").addEventListener("submit", async (event) => {
  event.preventDefault(); const button = event.currentTarget.querySelector("button[type=submit]");
  const id = event.currentTarget.dataset.invoiceId;
  const result = await performMutation(button, () => api(`/invoices/${id}/payments`, {method: "POST", body: {
    amount: Number($("payment-amount").value), paymentMethod: $("payment-method").value,
    paymentDate: new Date().toISOString(), referenceCode: $("payment-reference").value.trim() || null,
    note: $("payment-note").value.trim() || null
  }}), "Đã ghi nhận thanh toán.");
  if (result) { event.currentTarget.reset(); event.currentTarget.hidden = true; }
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
  updateUserBadge();
  reload().catch((error) => setNotice(error.message, true));
}
