# REQ: Ứng dụng Quản lý Nhà Trọ

|Field|Nội dung|
|-|-|
|Tác giả|ThanhHP4|
|Ngày|2026-10-02|
|Trạng thái|Approved|

## 1\. Bối cảnh

* Quản lý dãy trọ gồm 9 phòng.
* Mỗi phòng khoảng 20m², có gác, có điều hòa.
* Quản lý phòng, người thuê, hợp đồng, điện nước, hóa đơn và thanh toán.
* Mục tiêu: thay thế việc quản lý thủ công bằng Excel/sổ tay.

## 2\. Mục tiêu

* \[ ] Quản lý phòng và trạng thái phòng.
* \[ ] Quản lý khách thuê và hợp đồng.
* \[ ] Ghi chỉ số điện, nước hàng tháng.
* \[ ] Tự động tính tiền phòng, điện, nước, dịch vụ.
* \[ ] Tạo hóa đơn và theo dõi công nợ.
* \[ ] Dashboard doanh thu và tình trạng phòng.

## 3\. Techstack

* Java 21 / Spring Boot 3.x
* PostgreSQL + Flyway
* Spring Data JPA
* Spring Security + JWT
* Maven
* Swagger / OpenAPI
* Docker + docker-compose

## 4\. Phạm vi

* Must: Auth, CRUD phòng, khách thuê, hợp đồng, điện nước, hóa đơn, thanh toán.
* Should: xuất PDF/Excel, QR chuyển khoản, upload ảnh đồng hồ điện.
* Out-scope: mobile app, IoT, microservices, Kubernetes.

## 5\. Domain / Entities

|Entity|Field chính|Quan hệ|
|-|-|-|
|Room|id, roomNumber, area, rentPrice, status|1-N Contract|
|Tenant|id, fullName, phone, citizenId|1-N Contract|
|Contract|id, roomId, tenantId, startDate, deposit, status|N-1 Room, Tenant|
|ElectricityReading|roomId, month, oldIndex, newIndex, amount|N-1 Room|
|WaterReading|roomId, month, usage, amount|N-1 Room|
|Invoice|roomId, month, totalAmount, status|1-N Payment|
|Payment|invoiceId, amount, paymentDate, method|N-1 Invoice|

## 6\. API tối thiểu

* `POST /api/v1/auth/login`
* `GET/POST/PUT/DELETE /api/v1/rooms`
* `GET/POST/PUT /api/v1/tenants`
* `GET/POST/PUT /api/v1/contracts`
* `POST /api/v1/electricity-readings`
* `POST /api/v1/water-readings`
* `POST /api/v1/invoices/generate`
* `POST /api/v1/invoices/{id}/payments`
* `GET /api/v1/dashboard/summary`

## 7\. Business Rule

* BR-01: Một phòng chỉ có 1 hợp đồng ACTIVE tại một thời điểm.
* BR-02: Chỉ số điện/nước mới không được nhỏ hơn chỉ số cũ.
* BR-03: Không tạo 2 hóa đơn cho cùng phòng trong cùng tháng.
* BR-04: Hóa đơn = tiền phòng + điện + nước + dịch vụ - giảm giá.
* BR-05: Thanh toán đủ thì hóa đơn chuyển sang PAID.
* BR-06: Khi kết thúc hợp đồng, phòng chuyển về VACANT.

## 8\. Yêu cầu kỹ thuật

* JWT access/refresh token.
* Phân quyền ADMIN/STAFF.
* Validation request.
* Pagination cho danh sách.
* Dùng `BigDecimal` cho tiền.
* Flyway migration.
* Swagger UI.
* Docker Compose chạy app + PostgreSQL.
* Tránh N+1 query.

## 9\. Tiêu chí Pass / Fail

* \[ ] 9 phòng được seed sẵn.
* \[ ] CRUD phòng, khách thuê, hợp đồng chạy được.
* \[ ] Nhập điện nước và tính tiền đúng.
* \[ ] Tạo hóa đơn và thanh toán được.
* \[ ] Theo dõi công nợ được.
* \[ ] Dashboard hiển thị đúng dữ liệu.
* \[ ] Swagger/Postman test được toàn bộ API chính.
* \[ ] `docker compose up` chạy thành công.

