"use strict";

(function() {
  function renderContracts() {
    const container = $("contracts-body");
    if (!container) return;
    container.replaceChildren();
    if (!state.contracts || !state.contracts.length) {
      container.append(node("p", "empty", "Chưa có hợp đồng nào."));
      return;
    }
    const wrap = node("div", "table-wrap");
    const table = node("table");
    const thead = node("thead");
    const tr = node("tr");
    ["Phòng", "Khách đứng tên", "Giá thuê / tháng", "Tiền cọc", "Ngày bắt đầu", "Ngày kết thúc", "Trạng thái", "Ghi chú"].forEach(h => tr.append(node("th", "", h)));
    thead.append(tr);
    table.append(thead);
    const tbody = node("tbody");
    state.contracts.forEach(c => {
      const row = node("tr");
      const tenant = state.tenants.find(t => t.id === c.tenantId);
      addCells(row, [
        roomName(c.roomId),
        tenant ? tenant.fullName : `#${c.tenantId}`,
        money(c.monthlyRent),
        money(c.depositAmount),
        c.startDate || "—",
        c.endDate || "Dài hạn"
      ]);
      const statusCell = node("td");
      statusCell.append(badge(c.status));
      row.append(statusCell);
      row.append(node("td", "", c.note || "—"));
      tbody.append(row);
    });
    table.append(tbody);
    wrap.append(table);
    container.append(wrap);
  }

  function renderCatalog() {
    // 1. Properties
    const propList = $("properties-list");
    if (propList) {
      propList.replaceChildren();
      if (!state.properties || !state.properties.length) {
        propList.append(node("p", "empty", "Chưa có dãy trọ nào. Hãy nhấn 'Thêm dãy' để tạo."));
      } else {
        const wrap = node("div", "table-wrap");
        const table = node("table");
        const thead = node("thead");
        const tr = node("tr");
        ["ID", "Tên dãy trọ / Tòa nhà", "Địa chỉ"].forEach(h => tr.append(node("th", "", h)));
        thead.append(tr);
        table.append(thead);
        const tbody = node("tbody");
        state.properties.forEach(p => {
          const row = node("tr");
          addCells(row, [p.id, p.name, p.address || "—"]);
          tbody.append(row);
        });
        table.append(tbody);
        wrap.append(table);
        propList.append(wrap);
      }
    }

    // 2. Rooms
    const roomsList = $("catalog-rooms");
    if (roomsList) {
      roomsList.replaceChildren();
      if (!state.rooms || !state.rooms.length) {
        roomsList.append(node("p", "empty", "Chưa có phòng nào."));
      } else {
        const wrap = node("div", "table-wrap");
        const table = node("table");
        const thead = node("thead");
        const tr = node("tr");
        ["Mã phòng", "Tên mô tả", "Diện tích", "Tiện nghi", "Giá thuê niêm yết", "Tiền cọc", "Trạng thái"].forEach(h => tr.append(node("th", "", h)));
        thead.append(tr);
        table.append(thead);
        const tbody = node("tbody");
        state.rooms.forEach(r => {
          const row = node("tr");
          addCells(row, [
            r.roomNumber,
            r.name || "—",
            `${r.area || "—"} m²`,
            [r.hasLoft && "Gác lửng", r.hasAirConditioner && "Điều hòa"].filter(Boolean).join(", ") || "Cơ bản",
            r.monthlyRent == null ? "Chưa đặt" : money(r.monthlyRent),
            r.depositAmount == null ? "0 đ" : money(r.depositAmount)
          ]);
          const statusCell = node("td");
          statusCell.append(badge(r.status));
          row.append(statusCell);
          tbody.append(row);
        });
        table.append(tbody);
        wrap.append(table);
        roomsList.append(wrap);
      }
    }

    // 3. Tenants
    const tenantsList = $("catalog-tenants");
    if (tenantsList) {
      tenantsList.replaceChildren();
      if (!state.tenants || !state.tenants.length) {
        tenantsList.append(node("p", "empty", "Chưa có khách thuê nào."));
      } else {
        const wrap = node("div", "table-wrap");
        const table = node("table");
        const thead = node("thead");
        const tr = node("tr");
        ["Họ tên", "Số điện thoại", "Số CCCD/CMND", "Biển số xe", "Ghi chú"].forEach(h => tr.append(node("th", "", h)));
        thead.append(tr);
        table.append(thead);
        const tbody = node("tbody");
        state.tenants.forEach(t => {
          const row = node("tr");
          addCells(row, [t.fullName, t.phone || "—", t.citizenId || "—", t.vehiclePlate || "—", t.note || "—"]);
          tbody.append(row);
        });
        table.append(tbody);
        wrap.append(table);
        tenantsList.append(wrap);
      }
    }

    // 4. Services
    const servicesList = $("catalog-services");
    if (servicesList) {
      servicesList.replaceChildren();
      if (!state.services || !state.services.length) {
        servicesList.append(node("p", "empty", "Chưa có danh mục dịch vụ."));
      } else {
        const wrap = node("div", "table-wrap");
        const table = node("table");
        const thead = node("thead");
        const tr = node("tr");
        ["Tên dịch vụ", "Cách tính", "Đơn giá", "Trạng thái"].forEach(h => tr.append(node("th", "", h)));
        thead.append(tr);
        table.append(thead);
        const tbody = node("tbody");
        const calcMap = {FIXED: "Cố định", PER_PERSON: "Theo người", PER_VEHICLE: "Theo xe", METER: "Theo chỉ số đồng hồ", USAGE: "Theo mức sử dụng"};
        state.services.forEach(s => {
          const row = node("tr");
          addCells(row, [s.name, calcMap[s.calculationType] || s.calculationType, money(s.price)]);
          const statusCell = node("td");
          statusCell.append(badge(s.active ? "Đang dùng" : "Ngừng dùng"));
          row.append(statusCell);
          tbody.append(row);
        });
        table.append(tbody);
        wrap.append(table);
        servicesList.append(wrap);
      }
    }

    // 5. Users
    const usersList = $("users-list");
    if (usersList) {
      usersList.replaceChildren();
      if (!state.users || !state.users.length) {
        usersList.append(node("p", "empty", "Chưa có tài khoản nào hoặc bạn không có quyền xem."));
      } else {
        const wrap = node("div", "table-wrap");
        const table = node("table");
        const thead = node("thead");
        const tr = node("tr");
        ["Tên đăng nhập", "Họ tên", "Vai trò", "Số điện thoại", "Trạng thái"].forEach(h => tr.append(node("th", "", h)));
        thead.append(tr);
        table.append(thead);
        const tbody = node("tbody");
        const roleMap = {SUPER_ADMIN: "Quản trị viên hệ thống", OWNER: "Chủ trọ", STAFF: "Nhân viên quản lý"};
        state.users.forEach(u => {
          const row = node("tr");
          addCells(row, [u.username, u.fullName || "—", roleMap[u.role] || u.role, u.phone || "—"]);
          const statusCell = node("td");
          statusCell.append(badge(u.enabled ? "Hoạt động" : "Bị khóa"));
          row.append(statusCell);
          tbody.append(row);
        });
        table.append(tbody);
        wrap.append(table);
        usersList.append(wrap);
      }
    }
  }

  window.renderExtras = function() {
    renderContracts();
    renderCatalog();
  };

  // Dialog helpers
  function openEditor(title, fields, onSubmit) {
    const dialog = $("editor-dialog");
    if (!dialog) return;
    $("editor-title").textContent = title;
    const formFields = $("editor-fields");
    formFields.replaceChildren();
    fields.forEach(f => {
      const label = node("label", "", f.label);
      let input;
      if (f.type === "select") {
        input = node("select");
        f.options.forEach(([text, val]) => input.add(new Option(text, val)));
      } else {
        input = node("input");
        input.type = f.type || "text";
        if (f.step) input.step = f.step;
        if (f.min !== undefined) input.min = f.min;
      }
      input.id = `edit-${f.name}`;
      input.name = f.name;
      if (f.value !== undefined) input.value = f.value;
      if (f.required) input.required = true;
      label.append(input);
      formFields.append(label);
    });
    const form = $("editor-form");
    form.onsubmit = async (e) => {
      e.preventDefault();
      const data = {};
      fields.forEach(f => {
        const el = $(`edit-${f.name}`);
        if (!el) return;
        if (f.type === "number") data[f.name] = el.value === "" ? null : Number(el.value);
        else if (f.type === "checkbox") data[f.name] = el.checked;
        else data[f.name] = el.value.trim() || null;
      });
      const submitBtn = form.querySelector("button[type=submit]");
      submitBtn.disabled = true;
      try {
        await onSubmit(data);
        dialog.close();
        await reload();
      } catch (err) {
        setNotice(err.message, true);
      } finally {
        submitBtn.disabled = false;
      }
    };
    dialog.showModal();
  }

  $("editor-close")?.addEventListener("click", () => $("editor-dialog")?.close());
  $("viewer-close")?.addEventListener("click", () => $("viewer-dialog")?.close());

  // Thêm dãy trọ
  $("add-property")?.addEventListener("click", () => {
    openEditor("Thêm dãy trọ / Tòa nhà mới", [
      {name: "name", label: "Tên dãy trọ / Tòa nhà *", required: true},
      {name: "address", label: "Địa chỉ cụ thể"}
    ], async (data) => {
      await api("/properties", {method: "POST", body: data});
      setNotice("Đã thêm dãy trọ mới thành công.");
    });
  });

  // Thêm phòng
  function openAddRoom() {
    if (!state.properties.length) {
      setNotice("Bạn cần tạo ít nhất 1 dãy trọ trước khi thêm phòng.", true);
      return;
    }
    const propOptions = state.properties.map(p => [p.name, p.id]);
    openEditor("Thêm phòng trọ mới", [
      {name: "propertyId", label: "Thuộc dãy trọ *", type: "select", options: propOptions, required: true},
      {name: "roomNumber", label: "Mã / Số phòng (vd: P101) *", required: true},
      {name: "name", label: "Tên mô tả phòng (vd: Phòng 101 - Ban công)"},
      {name: "area", label: "Diện tích (m²) *", type: "number", step: "0.1", min: "1", value: 20, required: true},
      {name: "monthlyRent", label: "Giá thuê niêm yết (đ)", type: "number", step: "1000", min: "0", value: 3000000},
      {name: "depositAmount", label: "Tiền cọc mặc định (đ)", type: "number", step: "1000", min: "0", value: 3000000},
      {name: "hasLoft", label: "Có gác lửng?", type: "select", options: [["Không", false], ["Có gác lửng", true]]},
      {name: "hasAirConditioner", label: "Có điều hòa / máy lạnh?", type: "select", options: [["Có điều hòa", true], ["Không có", false]]},
      {name: "status", label: "Trạng thái ban đầu", type: "select", options: [["Phòng trống (VACANT)", "VACANT"], ["Đang bảo trì (MAINTENANCE)", "MAINTENANCE"]]}
    ], async (data) => {
      data.hasLoft = String(data.hasLoft) === "true";
      data.hasAirConditioner = String(data.hasAirConditioner) === "true";
      await api("/rooms", {method: "POST", body: data});
      setNotice("Đã thêm phòng mới thành công.");
    });
  }
  $("add-room")?.addEventListener("click", openAddRoom);
  $("add-room-people")?.addEventListener("click", openAddRoom);

  // Thêm khách thuê
  $("add-tenant")?.addEventListener("click", () => {
    openEditor("Thêm khách thuê mới", [
      {name: "fullName", label: "Họ và tên *", required: true},
      {name: "phone", label: "Số điện thoại (10 chữ số) *", required: true},
      {name: "citizenId", label: "Số CCCD / CMND (12 chữ số)"},
      {name: "dateOfBirth", label: "Ngày sinh", type: "date"},
      {name: "vehiclePlate", label: "Biển số xe (nếu có)"},
      {name: "address", label: "Địa chỉ thường trú / Quê quán"},
      {name: "gender", label: "Giới tính", type: "select", options: [["Nam", "MALE"], ["Nữ", "FEMALE"], ["Khác", "OTHER"]]},
      {name: "note", label: "Ghi chú"}
    ], async (data) => {
      await api("/tenants", {method: "POST", body: data});
      setNotice("Đã thêm khách thuê thành công.");
    });
  });

  // Thêm dịch vụ
  $("add-service")?.addEventListener("click", () => {
    openEditor("Thêm danh mục dịch vụ", [
      {name: "name", label: "Tên dịch vụ *", required: true},
      {name: "calculationType", label: "Cách tính phí *", type: "select", options: [
        ["Cố định theo phòng (FIXED)", "FIXED"],
        ["Theo đầu người (PER_PERSON)", "PER_PERSON"],
        ["Theo số lượng xe (PER_VEHICLE)", "PER_VEHICLE"],
        ["Theo chỉ số công tơ (METER)", "METER"],
        ["Theo mức sử dụng phát sinh (USAGE)", "USAGE"]
      ]},
      {name: "price", label: "Đơn giá (đ) *", type: "number", step: "1000", min: "0", value: 50000, required: true},
      {name: "active", label: "Trạng thái", type: "select", options: [["Đang áp dụng", true], ["Tạm ngưng", false]]}
    ], async (data) => {
      data.active = String(data.active) === "true";
      await api("/services", {method: "POST", body: data});
      setNotice("Đã thêm dịch vụ mới thành công.");
    });
  });

  // Thêm tài khoản người dùng
  $("add-user")?.addEventListener("click", () => {
    const isSuper = state.tokens && JSON.parse(atob(state.tokens.accessToken.split(".")[1].replace(/-/g, "+").replace(/_/g, "/"))).role === "SUPER_ADMIN";
    const roleOptions = isSuper
      ? [["Chủ trọ mới (OWNER)", "OWNER"], ["Nhân viên hỗ trợ (STAFF)", "STAFF"], ["Quản trị viên (SUPER_ADMIN)", "SUPER_ADMIN"]]
      : [["Nhân viên hỗ trợ (STAFF)", "STAFF"]];

    openEditor("Tạo tài khoản người dùng mới", [
      {name: "username", label: "Tên đăng nhập *", required: true},
      {name: "password", label: "Mật khẩu ban đầu (ít nhất 6 ký tự) *", type: "password", required: true},
      {name: "fullName", label: "Họ và tên người dùng"},
      {name: "phone", label: "Số điện thoại liên hệ"},
      {name: "role", label: "Vai trò tài khoản *", type: "select", options: roleOptions},
      {name: "note", label: "Ghi chú"}
    ], async (data) => {
      await api("/users", {method: "POST", body: data});
      setNotice("Đã tạo tài khoản người dùng mới thành công.");
    });
  });

  // Đổi mật khẩu tài khoản
  $("change-password-button")?.addEventListener("click", () => {
    openEditor("Đổi mật khẩu tài khoản", [
      {name: "currentPassword", label: "Mật khẩu hiện tại *", type: "password", required: true},
      {name: "newPassword", label: "Mật khẩu mới (ít nhất 5 ký tự) *", type: "password", required: true}
    ], async (data) => {
      const tokens = await api("/auth/change-password", {method: "POST", body: data});
      if (tokens) saveSession(tokens);
      setNotice("Đã đổi mật khẩu thành công.");
    });
  });

  // Xuất file Excel
  $("export-excel")?.addEventListener("click", async () => {
    const {month, year} = period();
    const token = state.tokens?.accessToken;
    if (!token) return;
    try {
      setNotice("Đang tạo và tải file báo cáo Excel...");
      const res = await fetch(`/api/v1/invoices/export.xlsx?month=${month}&year=${year}`, {
        headers: {Authorization: `Bearer ${token}`}
      });
      if (!res.ok) throw new Error("Không thể tải file báo cáo Excel.");
      const blob = await res.blob();
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = `Bao_cao_hoa_don_${month}_${year}.xlsx`;
      document.body.appendChild(a);
      a.click();
      a.remove();
      window.URL.revokeObjectURL(url);
      setNotice("Đã tải xong file Excel!");
    } catch (err) {
      setNotice(err.message, true);
    }
  });

  // Tạo hóa đơn cho một phòng đơn lẻ
  $("single-invoice-form")?.addEventListener("submit", async (e) => {
    e.preventDefault();
    const submitBtn = e.currentTarget.querySelector("button");
    const roomId = Number($("single-invoice-room").value);
    if (!roomId) {
      setNotice("Vui lòng chọn một phòng cần tạo hóa đơn.", true);
      return;
    }
    const {month, year} = period();
    const otherAmount = Number($("single-invoice-other").value || 0);
    const discount = Number($("single-invoice-discount").value || 0);
    await performMutation(submitBtn, () => api("/invoices", {
      method: "POST",
      body: {roomId, month, year, otherAmount, discount}
    }), "Đã lập hóa đơn thành công.");
  });

})();
