-- V7__sample_data.sql: Khởi tạo 10 dữ liệu mẫu cho mỗi bảng

DO $SAMPLE_DATA$
BEGIN
    -- Chỉ nạp dữ liệu mẫu nếu không phải môi trường test/local ('Dãy trọ 01') và chưa từng nạp dữ liệu mẫu ('Chung cư Mini Cầu Giấy')
    IF NOT EXISTS (SELECT 1 FROM properties WHERE name = 'Dãy trọ 01')
       AND NOT EXISTS (SELECT 1 FROM properties WHERE name = 'Chung cư Mini Cầu Giấy') THEN

-- 1. properties (10 tòa nhà / khu trọ)
INSERT INTO properties (name, address) VALUES
('Chung cư Mini Cầu Giấy', 'Số 12 Ngõ 68 Cầu Giấy, P. Quan Hoa, Q. Cầu Giấy, Hà Nội'),
('Khu Căn Hộ Dịch Vụ Thanh Xuân', 'Số 45 Ngõ 116 Nhân Hòa, P. Nhân Chính, Q. Thanh Xuân, Hà Nội'),
('Nhà Trọ Cao Cấp Hai Bà Trưng', 'Số 88 Phố Bạch Mai, P. Cầu Dền, Q. Hai Bà Trưng, Hà Nội'),
('Tòa Nhà Harmony Đống Đa', 'Số 26 Ngõ 198 Xã Đàn, P. Nam Đồng, Q. Đống Đa, Hà Nội'),
('Khu Trọ Sinh Viên Tây Hồ', 'Số 15 Ngách 2/1 Võ Chí Công, P. Nghĩa Đô, Q. Tây Hồ, Hà Nội'),
('Căn Hộ Dịch Vụ Bình Thạnh', 'Số 102 Đường D2 (Nguyễn Gia Trí), P. 25, Q. Bình Thạnh, TP.HCM'),
('Chung Cư Mini Tân Bình', 'Số 34 Đường Thăng Long, P. 4, Q. Tân Bình, TP.HCM'),
('Nhà Trọ Cao Cấp Quận 10', 'Số 256/12 Cách Mạng Tháng 8, P. 10, Q. 3, TP.HCM'),
('Tòa Nhà Xanh Thủ Đức', 'Số 18 Đường Số 6, P. Linh Trung, TP. Thủ Đức, TP.HCM'),
('Khu Căn Hộ Sunrise Hải Châu', 'Số 79 Đường Nguyễn Thị Minh Khai, P. Thạch Thang, Q. Hải Châu, Đà Nẵng');

-- 2. rooms (10 phòng trọ)
INSERT INTO rooms (property_id, code, name, area_m2, has_mezzanine, has_air_conditioner, monthly_rent, deposit_amount, description, status) VALUES
(1, 'P101', 'Phòng 101 - Ban công thoáng', 25.00, false, true, 3500000.00, 3500000.00, 'Phòng tầng 1, ban công riêng, đầy đủ máy lạnh tủ lạnh', 'OCCUPIED'),
(1, 'P102', 'Phòng 102 - Studio gác lửng', 30.00, true, true, 4200000.00, 4200000.00, 'Phòng có gác cao 2m, bếp riêng, hút mùi', 'OCCUPIED'),
(2, 'P201', 'Phòng 201 - View phố thoáng mát', 28.00, false, true, 3800000.00, 3800000.00, 'Phòng tầng 2 nhiều ánh sáng tự nhiên, full rèm cửa', 'OCCUPIED'),
(2, 'P202', 'Phòng 202 - Cơ bản tiện nghi', 22.00, false, true, 3200000.00, 3200000.00, 'Phòng gọn gàng, WC khép kín, bình nóng lạnh', 'OCCUPIED'),
(3, 'P301', 'Phòng 301 - Gác lửng cao cấp', 32.00, true, true, 4500000.00, 4500000.00, 'Gác đúc, sàn gỗ, máy giặt riêng trong phòng', 'OCCUPIED'),
(4, 'P401', 'Phòng 401 - Căn hộ 1PN', 35.00, false, true, 5000000.00, 5000000.00, 'Căn hộ 1 phòng ngủ tách biệt, sofa bed, tivi 43 inch', 'OCCUPIED'),
(5, 'P501', 'Phòng 501 - Tiết kiệm sinh viên', 20.00, false, false, 2500000.00, 2500000.00, 'Phòng thoáng mát hướng đông, có quạt trần', 'OCCUPIED'),
(6, 'P101', 'Phòng 101 - Full nội thất', 30.00, true, true, 4800000.00, 4800000.00, 'Gần các trường ĐH HUTECH, Ngoại Thương, khóa vân tay', 'OCCUPIED'),
(7, 'P201', 'Phòng 201 - Yên tĩnh thoáng mát', 26.00, false, true, 3600000.00, 3600000.00, 'Khu vực yên tĩnh, giờ giấc tự do không chung chủ', 'VACANT'),
(8, 'P301', 'Phòng 301 - Đang bảo trì', 24.00, false, true, 3300000.00, 3300000.00, 'Đang sơn sửa lại tường và lắp thêm máy lạnh mới', 'MAINTENANCE');

-- 3. tenants (10 khách thuê)
INSERT INTO tenants (full_name, phone, identity_number, date_of_birth, address, gender, vehicle_plate, note) VALUES
('Nguyễn Văn An', '0912345678', '001200001234', '2000-05-15', 'P. Dịch Vọng Hậu, Q. Cầu Giấy, Hà Nội', 'MALE', '29-B1 123.45', 'Khách thuê dài hạn, làm việc tại FPT'),
('Trần Thị Bích', '0987654321', '036199002345', '1999-08-20', 'P. Thanh Xuân Bắc, Q. Thanh Xuân, Hà Nội', 'FEMALE', '36-B2 678.90', 'Nhân viên kế toán ngân hàng'),
('Lê Hoàng Cường', '0903112233', '024198003456', '1998-11-03', 'P. Khâm Thiên, Q. Đống Đa, Hà Nội', 'MALE', '29-E1 999.88', 'Kỹ sư phần mềm công ty công nghệ'),
('Phạm Minh Đức', '0934556677', '017197004567', '1997-02-28', 'P. Hàng Mã, Q. Hoàn Kiếm, Hà Nội', 'MALE', '29-C1 543.21', 'Kinh doanh thời trang online'),
('Vũ Thùy Dung', '0978998877', '031201005678', '2001-09-12', 'P. Lê Chân, Q. Ngô Quyền, Hải Phòng', 'FEMALE', '15-B1 135.79', 'Sinh viên năm cuối ĐH Quốc Gia'),
('Đỗ Quang Hải', '0918776655', '079196006789', '1996-07-07', 'P. 12, Q. Bình Thạnh, TP.HCM', 'MALE', '59-S2 246.80', 'Designer agency quảng cáo'),
('Hoàng Mai Lan', '0965443322', '075200007890', '2000-12-25', 'P. Thảo Điền, TP. Thủ Đức, TP.HCM', 'FEMALE', '60-F3 888.66', 'Giáo viên tiếng Anh trung tâm ILA'),
('Bùi Tấn Phát', '0945667788', '080195008901', '1995-04-18', 'P. 6, Q. Tân Bình, TP.HCM', 'MALE', '51-K1 456.78', 'Quản trị chuỗi cung ứng Shopee'),
('Ngô Bảo Ngọc', '0922334455', '048199009012', '1999-01-30', 'P. Thuận Phước, Q. Hải Châu, Đà Nẵng', 'FEMALE', '43-D1 369.25', 'Khách cũ đã trả phòng đầu năm'),
('Đinh Trọng Nghĩa', '0971223344', '038198010123', '1998-10-10', 'P. Tân Phong, TP. Biên Hòa, Đồng Nai', 'MALE', '39-F2 777.99', 'Khách mới đặt cọc giữ chỗ phòng');

-- 4. contracts (10 hợp đồng)
INSERT INTO contracts (room_id, tenant_id, start_date, end_date, monthly_rent, deposit_amount, status, payment_due_day, note, electricity_price, water_calculation_type, water_price) VALUES
(1, 1, '2026-01-01', '2026-12-31', 3500000.00, 3500000.00, 'ACTIVE', 5, 'Hợp đồng thuê 1 năm, đóng tiền ngày 5', 3500.00, 'METER', 25000.00),
(2, 2, '2026-02-15', '2027-02-14', 4200000.00, 4200000.00, 'ACTIVE', 5, 'Hợp đồng 1 năm, nước tính theo đầu người (2 người)', 3500.00, 'PER_PERSON', 100000.00),
(3, 3, '2026-03-01', '2026-12-01', 3800000.00, 3800000.00, 'ACTIVE', 10, 'Hợp đồng 9 tháng, thanh toán ngày 10', 3800.00, 'METER', 28000.00),
(4, 4, '2026-04-01', '2027-04-01', 3200000.00, 3200000.00, 'ACTIVE', 5, 'Hợp đồng 1 năm, ưu đãi tháng đầu', 3500.00, 'METER', 25000.00),
(5, 5, '2026-05-01', '2026-11-01', 4500000.00, 4500000.00, 'ACTIVE', 5, 'Gia đình nhỏ 2 vợ chồng', 3500.00, 'METER', 25000.00),
(6, 6, '2026-06-01', '2027-06-01', 5000000.00, 5000000.00, 'ACTIVE', 1, 'Căn hộ 1PN, thanh toán đầu tháng', 4000.00, 'PER_PERSON', 120000.00),
(7, 7, '2026-07-01', '2027-01-01', 2500000.00, 2500000.00, 'ACTIVE', 5, 'Hợp đồng sinh viên 6 tháng', 3500.00, 'PER_PERSON', 80000.00),
(8, 8, '2026-08-01', '2027-08-01', 4800000.00, 4800000.00, 'ACTIVE', 5, 'Hợp đồng căn hộ dịch vụ Bình Thạnh', 3800.00, 'METER', 30000.00),
(9, 9, '2025-01-01', '2026-01-01', 3600000.00, 3600000.00, 'EXPIRED', 5, 'Hợp đồng đã kết thúc và thanh lý xong', 3500.00, 'METER', 25000.00),
(9, 10, '2026-11-01', '2027-11-01', 3600000.00, 3600000.00, 'PENDING', 5, 'Hợp đồng giữ chỗ, nhận phòng từ tháng 11', 3500.00, 'METER', 25000.00);

-- 5. room_tenants (10 người ở phòng)
INSERT INTO room_tenants (room_id, tenant_id, contract_id, role, move_in_date, move_out_date) VALUES
(1, 1, 1, 'PRIMARY_TENANT', '2026-01-01', NULL),
(2, 2, 2, 'PRIMARY_TENANT', '2026-02-15', NULL),
(3, 3, 3, 'PRIMARY_TENANT', '2026-03-01', NULL),
(4, 4, 4, 'PRIMARY_TENANT', '2026-04-01', NULL),
(5, 5, 5, 'PRIMARY_TENANT', '2026-05-01', NULL),
(6, 6, 6, 'PRIMARY_TENANT', '2026-06-01', NULL),
(7, 7, 7, 'PRIMARY_TENANT', '2026-07-01', NULL),
(8, 8, 8, 'PRIMARY_TENANT', '2026-08-01', NULL),
(9, 9, 9, 'PRIMARY_TENANT', '2025-01-01', '2026-01-01'),
(2, 10, 2, 'MEMBER', '2026-02-15', NULL);

-- 6. services (10 dịch vụ: cập nhật giá Wi-Fi và thêm 9 dịch vụ phổ biến)
UPDATE services SET price = 100000.00 WHERE lower(name) = 'wi-fi';
INSERT INTO services (name, calculation_type, price, active) VALUES
('Vệ sinh rác thải', 'FIXED', 50000.00, true),
('Gửi xe máy', 'PER_VEHICLE', 120000.00, true),
('Gửi xe đạp điện', 'PER_VEHICLE', 80000.00, true),
('Quản lý tòa nhà', 'FIXED', 150000.00, true),
('Giặt sấy chung', 'PER_PERSON', 100000.00, true),
('Thang máy', 'PER_PERSON', 50000.00, true),
('Nước uống Lavie', 'METER', 60000.00, true),
('Bảo vệ 24/7 & Camera', 'FIXED', 80000.00, true),
('Dọn phòng theo tuần', 'FIXED', 200000.00, false);

-- 7. contract_services (10 dịch vụ đăng ký theo hợp đồng)
INSERT INTO contract_services (contract_id, service_id, price, quantity) VALUES
(1, 1, 100000.00, 1.000),
(1, 2, 50000.00, 1.000),
(1, 3, 120000.00, 1.000),
(2, 1, 100000.00, 1.000),
(2, 3, 120000.00, 2.000),
(3, 1, 100000.00, 1.000),
(4, 2, 50000.00, 1.000),
(5, 5, 150000.00, 1.000),
(6, 7, 50000.00, 2.000),
(8, 3, 120000.00, 1.000);

-- 8. service_usages (10 ghi nhận sử dụng dịch vụ trong kỳ)
INSERT INTO service_usages (contract_id, service_id, billing_month, billing_year, quantity) VALUES
(1, 1, 10, 2026, 1.000),
(1, 2, 10, 2026, 1.000),
(1, 3, 10, 2026, 1.000),
(2, 1, 10, 2026, 1.000),
(2, 3, 10, 2026, 2.000),
(3, 1, 10, 2026, 1.000),
(4, 2, 10, 2026, 1.000),
(5, 5, 10, 2026, 1.000),
(6, 7, 10, 2026, 2.000),
(8, 3, 10, 2026, 1.000);

-- 9. electricity_meters (10 chỉ số điện)
INSERT INTO electricity_meters (room_id, reading_date, reading, billing_month, billing_year, previous_reading, current_reading, usage, unit_price, amount, image_url) VALUES
(1, '2026-10-20', 1385.000, 10, 2026, 1250.000, 1385.000, 135.000, 3500.00, 472500.00, NULL),
(2, '2026-10-20', 2280.000, 10, 2026, 2100.000, 2280.000, 180.000, 3500.00, 630000.00, NULL),
(3, '2026-10-20', 970.000, 10, 2026, 850.000, 970.000, 120.000, 3800.00, 456000.00, NULL),
(4, '2026-10-20', 1715.000, 10, 2026, 1620.000, 1715.000, 95.000, 3500.00, 332500.00, NULL),
(5, '2026-10-20', 3310.000, 10, 2026, 3100.000, 3310.000, 210.000, 3500.00, 735000.00, NULL),
(6, '2026-10-20', 2230.000, 10, 2026, 1980.000, 2230.000, 250.000, 4000.00, 1000000.00, NULL),
(7, '2026-10-20', 560.000, 10, 2026, 500.000, 560.000, 60.000, 3500.00, 210000.00, NULL),
(8, '2026-10-20', 1060.000, 10, 2026, 920.000, 1060.000, 140.000, 3800.00, 532000.00, NULL),
(9, '2026-10-20', 450.000, 10, 2026, 450.000, 450.000, 0.000, 3500.00, 0.00, NULL),
(10, '2026-10-20', 310.000, 10, 2026, 310.000, 310.000, 0.000, 3500.00, 0.00, NULL);

-- 10. water_meters (10 chỉ số nước)
INSERT INTO water_meters (room_id, reading_date, reading, billing_month, billing_year, previous_reading, current_reading, usage, unit_price, amount, calculation_type, person_count) VALUES
(1, '2026-10-20', 120.000, 10, 2026, 112.000, 120.000, 8.000, 25000.00, 200000.00, 'METER', 0),
(2, '2026-10-20', 0.000, 10, 2026, 0.000, 0.000, 0.000, 100000.00, 200000.00, 'PER_PERSON', 2),
(3, '2026-10-20', 91.000, 10, 2026, 85.000, 91.000, 6.000, 28000.00, 168000.00, 'METER', 0),
(4, '2026-10-20', 69.000, 10, 2026, 64.000, 69.000, 5.000, 25000.00, 125000.00, 'METER', 0),
(5, '2026-10-20', 142.000, 10, 2026, 130.000, 142.000, 12.000, 25000.00, 300000.00, 'METER', 0),
(6, '2026-10-20', 0.000, 10, 2026, 0.000, 0.000, 0.000, 120000.00, 240000.00, 'PER_PERSON', 2),
(7, '2026-10-20', 0.000, 10, 2026, 0.000, 0.000, 0.000, 80000.00, 80000.00, 'PER_PERSON', 1),
(8, '2026-10-20', 52.000, 10, 2026, 45.000, 52.000, 7.000, 30000.00, 210000.00, 'METER', 0),
(9, '2026-10-20', 30.000, 10, 2026, 30.000, 30.000, 0.000, 25000.00, 0.00, 'METER', 0),
(10, '2026-10-20', 22.000, 10, 2026, 22.000, 22.000, 0.000, 25000.00, 0.00, 'METER', 0);

-- 11. deposit_transactions (10 giao dịch tiền cọc)
INSERT INTO deposit_transactions (contract_id, type, amount, transaction_date, note) VALUES
(1, 'RECEIVED', 3500000.00, '2026-01-01', 'Tiền cọc phòng 101 Cầu Giấy'),
(2, 'RECEIVED', 4200000.00, '2026-02-15', 'Tiền cọc phòng 102 Cầu Giấy'),
(3, 'RECEIVED', 3800000.00, '2026-03-01', 'Tiền cọc phòng 201 Thanh Xuân'),
(4, 'RECEIVED', 3200000.00, '2026-04-01', 'Tiền cọc phòng 202 Thanh Xuân'),
(5, 'RECEIVED', 4500000.00, '2026-05-01', 'Tiền cọc phòng 301 Bạch Mai'),
(6, 'RECEIVED', 5000000.00, '2026-06-01', 'Tiền cọc phòng 401 Xã Đàn'),
(7, 'RECEIVED', 2500000.00, '2026-07-01', 'Tiền cọc phòng 501 Tây Hồ'),
(8, 'RECEIVED', 4800000.00, '2026-08-01', 'Tiền cọc phòng 101 Bình Thạnh'),
(9, 'RECEIVED', 3600000.00, '2025-01-01', 'Tiền cọc khi ký hợp đồng năm 2025'),
(9, 'REFUNDED', 3600000.00, '2026-01-02', 'Hoàn cọc đủ khi trả phòng');

-- 12. invoices (10 hóa đơn thanh toán)
INSERT INTO invoices (contract_id, room_id, number, period_start, period_end, due_date, billing_month, billing_year, room_amount, electric_amount, water_amount, service_amount, other_amount, discount, total_amount, status, paid_at) VALUES
(1, 1, 'INV-202610-001', '2026-10-01', '2026-10-31', '2026-11-05', 10, 2026, 3500000.00, 472500.00, 200000.00, 270000.00, 0.00, 0.00, 4442500.00, 'PAID', '2026-10-21 09:30:00+07'),
(2, 2, 'INV-202610-002', '2026-10-01', '2026-10-31', '2026-11-05', 10, 2026, 4200000.00, 630000.00, 200000.00, 340000.00, 0.00, 100000.00, 5270000.00, 'PAID', '2026-10-21 14:15:00+07'),
(3, 3, 'INV-202610-003', '2026-10-01', '2026-10-31', '2026-11-10', 10, 2026, 3800000.00, 456000.00, 168000.00, 100000.00, 0.00, 0.00, 4524000.00, 'PARTIALLY_PAID', NULL),
(4, 4, 'INV-202610-004', '2026-10-01', '2026-10-31', '2026-11-05', 10, 2026, 3200000.00, 332500.00, 125000.00, 50000.00, 0.00, 0.00, 3707500.00, 'UNPAID', NULL),
(5, 5, 'INV-202610-005', '2026-10-01', '2026-10-31', '2026-11-05', 10, 2026, 4500000.00, 735000.00, 300000.00, 150000.00, 50000.00, 0.00, 5735000.00, 'UNPAID', NULL),
(6, 6, 'INV-202610-006', '2026-10-01', '2026-10-31', '2026-11-01', 10, 2026, 5000000.00, 1000000.00, 240000.00, 100000.00, 0.00, 200000.00, 6140000.00, 'PAID', '2026-10-22 10:00:00+07'),
(7, 7, 'INV-202610-007', '2026-10-01', '2026-10-31', '2026-11-05', 10, 2026, 2500000.00, 210000.00, 80000.00, 0.00, 0.00, 0.00, 2790000.00, 'UNPAID', NULL),
(8, 8, 'INV-202610-008', '2026-10-01', '2026-10-31', '2026-11-05', 10, 2026, 4800000.00, 532000.00, 210000.00, 120000.00, 0.00, 0.00, 5662000.00, 'PAID', '2026-10-22 08:30:00+07'),
(1, 1, 'INV-202609-001', '2026-09-01', '2026-09-30', '2026-10-05', 9, 2026, 3500000.00, 420000.00, 200000.00, 270000.00, 0.00, 0.00, 4390000.00, 'PAID', '2026-09-25 11:20:00+07'),
(2, 2, 'INV-202609-002', '2026-09-01', '2026-09-30', '2026-10-05', 9, 2026, 4200000.00, 590000.00, 200000.00, 340000.00, 0.00, 0.00, 5330000.00, 'PAID', '2026-09-26 15:45:00+07');

-- 13. invoice_items (10 chi tiết dòng hóa đơn)
INSERT INTO invoice_items (invoice_id, type, description, quantity, unit_price, amount) VALUES
(1, 'RENT', 'Tiền thuê phòng tháng 10/2026', 1.000, 3500000.00, 3500000.00),
(1, 'ELECTRICITY', 'Tiền điện sinh hoạt (135 kWh x 3,500đ)', 135.000, 3500.00, 472500.00),
(1, 'WATER', 'Tiền nước sinh hoạt (8 m3 x 25,000đ)', 8.000, 25000.00, 200000.00),
(1, 'SERVICE', 'Phí Internet Wi-Fi tốc độ cao', 1.000, 100000.00, 100000.00),
(1, 'SERVICE', 'Phí vệ sinh & thu gom rác', 1.000, 50000.00, 50000.00),
(1, 'SERVICE', 'Phí gửi xe máy (1 xe)', 1.000, 120000.00, 120000.00),
(2, 'RENT', 'Tiền thuê phòng tháng 10/2026', 1.000, 4200000.00, 4200000.00),
(2, 'ELECTRICITY', 'Tiền điện (180 kWh x 3,500đ)', 180.000, 3500.00, 630000.00),
(2, 'WATER', 'Tiền nước theo đầu người (2 người x 100,000đ)', 2.000, 100000.00, 200000.00),
(2, 'DISCOUNT', 'Ưu đãi thanh toán đúng hạn', 1.000, -100000.00, -100000.00);

-- 14. payments (10 lượt thanh toán)
INSERT INTO payments (invoice_id, amount, paid_at, method, reference, note) VALUES
(1, 4442500.00, '2026-10-21 09:30:00+07', 'BANK_TRANSFER', 'FT26102100123', 'Chuyển khoản qua Vietcombank'),
(2, 5270000.00, '2026-10-21 14:15:00+07', 'BANK_TRANSFER', 'FT26102100456', 'Chuyển khoản qua MB Bank'),
(3, 2500000.00, '2026-10-22 11:00:00+07', 'CASH', NULL, 'Thanh toán đợt 1 bằng tiền mặt cho quản lý'),
(6, 6140000.00, '2026-10-22 10:00:00+07', 'BANK_TRANSFER', 'FT26102200789', 'Chuyển khoản Techcombank đầy đủ'),
(8, 5662000.00, '2026-10-22 08:30:00+07', 'BANK_TRANSFER', 'FT26102200999', 'Chuyển khoản ACB qua QR code'),
(9, 4390000.00, '2026-09-25 11:20:00+07', 'BANK_TRANSFER', 'FT26092500111', 'Thanh toán tiền phòng kỳ tháng 09/2026'),
(10, 5330000.00, '2026-09-26 15:45:00+07', 'BANK_TRANSFER', 'FT26092600222', 'Thanh toán tiền phòng kỳ tháng 09/2026'),
(3, 1000000.00, '2026-10-22 15:30:00+07', 'BANK_TRANSFER', 'FT26102200333', 'Thanh toán đợt 2 qua QR Vietinbank'),
(5, 2000000.00, '2026-10-22 16:00:00+07', 'CASH', NULL, 'Đặt trước một phần tiền mặt'),
(7, 1000000.00, '2026-10-22 17:00:00+07', 'OTHER', 'MOMO26102299', 'Thanh toán tạm ứng qua ví MoMo');

-- 15. electricity_revisions (10 lịch sử điều chỉnh số điện)
INSERT INTO electricity_revisions (reading_id, old_reading, new_reading, changed_by, reason) VALUES
(1, 1375.000, 1385.000, 'admin', 'Ghi nhầm số 7 thành 8, chỉnh lại theo ảnh chụp công tơ thực tế'),
(2, 2250.000, 2280.000, 'admin', 'Khách chụp ảnh gửi xác nhận chỉ số cuối tháng'),
(3, 960.000, 970.000, 'admin', 'Kiểm tra lại đồng hồ công tơ phụ sau phản ánh của khách'),
(4, 1700.000, 1715.000, 'admin', 'Bù số điện 1 ngày ghi trễ do lễ'),
(5, 3300.000, 3310.000, 'admin', 'Chốt số điện ngày đổi người thuê phòng'),
(6, 2200.000, 2230.000, 'admin', 'Điều chỉnh theo số đo thực tế đồng hồ điện tử'),
(7, 550.000, 560.000, 'admin', 'Khách phản ánh số công tơ chưa chính xác'),
(8, 1050.000, 1060.000, 'admin', 'Chốt lại chỉ số trước kỳ thanh toán'),
(1, 1380.000, 1385.000, 'admin', 'Cập nhật lần 2 sau khi đối soát đồng hồ tổng'),
(2, 2270.000, 2280.000, 'admin', 'Đối chiếu biên bản bàn giao đầu vào');

-- 16. user_accounts (10 tài khoản người dùng: owner_chau + 9 staff/owner khác với mật khẩu staff12345)
INSERT INTO user_accounts (username, password_hash, role, enabled) VALUES
('owner_chau', '$2a$10$E5le9Qerw5y3MNql04StC.GxOIjVEyrwNBYYPIP/mEaWPIXNUowPG', 'OWNER', true),
('staff_caugiay', '$2a$10$cwKFWJG52k1TsT72/E.5X.tM.pu.3Ze.F.kdxJxjGAIpBPjJl.u8u', 'STAFF', true),
('staff_thanhxuan', '$2a$10$cwKFWJG52k1TsT72/E.5X.tM.pu.3Ze.F.kdxJxjGAIpBPjJl.u8u', 'STAFF', true),
('staff_dongda', '$2a$10$cwKFWJG52k1TsT72/E.5X.tM.pu.3Ze.F.kdxJxjGAIpBPjJl.u8u', 'STAFF', true),
('staff_binhthanh', '$2a$10$cwKFWJG52k1TsT72/E.5X.tM.pu.3Ze.F.kdxJxjGAIpBPjJl.u8u', 'STAFF', true),
('staff_tanbinh', '$2a$10$cwKFWJG52k1TsT72/E.5X.tM.pu.3Ze.F.kdxJxjGAIpBPjJl.u8u', 'STAFF', true),
('staff_quan10', '$2a$10$cwKFWJG52k1TsT72/E.5X.tM.pu.3Ze.F.kdxJxjGAIpBPjJl.u8u', 'STAFF', true),
('staff_thuduc', '$2a$10$cwKFWJG52k1TsT72/E.5X.tM.pu.3Ze.F.kdxJxjGAIpBPjJl.u8u', 'STAFF', true),
('staff_danang', '$2a$10$cwKFWJG52k1TsT72/E.5X.tM.pu.3Ze.F.kdxJxjGAIpBPjJl.u8u', 'STAFF', true),
('staff_haichau', '$2a$10$cwKFWJG52k1TsT72/E.5X.tM.pu.3Ze.F.kdxJxjGAIpBPjJl.u8u', 'STAFF', true)
ON CONFLICT (username) DO NOTHING;

    END IF;
END $SAMPLE_DATA$;
