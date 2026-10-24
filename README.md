# Quản lý phòng trọ

Backend Java 21, Spring Boot 3.5.16, PostgreSQL, Flyway, JWT và Swagger.
Đặc tả: [docs/requirements.md](docs/requirements.md).

## Chạy nhanh (1-Click)

### Cách 1: Chạy với PostgreSQL Local (Mặc định trong Docker)
- **Windows**: Nhấp đúp vào file `run.bat`.
- **Linux / macOS**: `./run.sh`
- Tự động kiểm tra Docker Desktop, khởi động PostgreSQL và backend, mở trình duyệt tại `http://localhost:8080/`.

### Cách 2: Chạy với Neon DB Cloud (Không cần sửa code)
- **Windows**: Nhấp đúp vào file `run-neon.bat`.
- **Linux / macOS**: `./run-neon.sh`
- Tự động kết nối trực tiếp tới cơ sở dữ liệu Neon DB trên đám mây (không cần chạy container database trên máy).
- Bạn có thể tùy chỉnh thông tin Neon DB bất kỳ lúc nào trong file `.env` (xem mẫu tại `.env.example`).

### Dừng hệ thống
- Chạy `stop.bat` (Windows) hoặc `./stop.sh` (Linux / macOS).

## Chạy thủ công

Cần Docker Desktop. Mở PowerShell thường tại thư mục project (không cần quyền Administrator):

```powershell
docker compose up -d --build --wait
```

Compose chạy PostgreSQL và backend. Mở giao diện HTML tại
http://localhost:8080/; mở Swagger tại http://localhost:8080/swagger-ui.html.
Đăng nhập trên giao diện bằng tài khoản OWNER ở dưới. Trang HTML có dashboard,
quản lý dãy/phòng/khách/dịch vụ, hợp đồng và người ở, tiền cọc, chỉ số điện nước,
ảnh đồng hồ, hóa đơn, thanh toán và nhắc nợ. Trong tab Hóa đơn có nút tải PDF
từng hóa đơn, xuất Excel theo kỳ, xem QR chuyển khoản và lịch sử thanh toán.
Khi tạo hợp đồng, nhập giá Wi-Fi/tháng nếu có (0 nghĩa là không thu). Với hợp
đồng đã tồn tại, dùng mục “Cập nhật Wi-Fi” tại tab Phòng & khách; giá mới chỉ
áp dụng cho hóa đơn chưa lập. Trong tab Hóa đơn, bấm “Chi tiết” để xem dòng Wi-Fi.
QR cần cấu hình `BANK_BIN`, `BANK_ACCOUNT` và `BANK_NAME`; nếu chưa cấu hình,
giao diện sẽ báo rõ khi bấm QR. Swagger vẫn dùng để kiểm tra API. Nếu chạy
backend từ IntelliJ hoặc Maven,
chỉ chạy database trong Docker để tránh trùng cổng 8080:

```powershell
docker compose up -d postgres
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

Profile `local` tạo một dãy P01–P09 (20 m², có gác, điều hòa); tiền thuê
chưa được đặt. Tài khoản OWNER ban đầu: `admin / admin`.
Nó được tạo khi bảng người dùng trống; đổi biến môi trường về sau không
tự đổi mật khẩu đã lưu.

Trong profile `local`, mật khẩu khởi tạo có thể ngắn từ 5 ký tự để thử nghiệm;
các môi trường khác yêu cầu ít nhất 12 ký tự. Nếu database đã có tài khoản,
`APP_ADMIN_PASSWORD` không phải chức năng đặt lại mật khẩu. Đăng nhập bằng mật
khẩu đã dùng khi tạo tài khoản, rồi bấm **Đổi mật khẩu** ở góc trên. Đổi mật khẩu
cần nhập mật khẩu hiện tại. Tài khoản OWNER thêm người dùng ở **Danh mục & tài
khoản → Thêm tài khoản**; có thể chọn OWNER hoặc STAFF. Mật khẩu của tài khoản
mới phải có ít nhất 12 ký tự. Nếu quên mật khẩu OWNER và không còn tài khoản
OWNER nào khác, cần khôi phục trong database từ bản sao lưu hoặc bằng quy trình
quản trị riêng; không xóa volume database để đặt lại mật khẩu.

```powershell
$login = Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/v1/auth/login -ContentType 'application/json' -Body '{"username":"admin","password":"admin"}'
$headers = @{ Authorization = "Bearer $($login.accessToken)" }
Invoke-RestMethod -Uri http://localhost:8080/api/v1/rooms -Headers $headers
```

Access token có hiệu lực 15 phút; refresh token 7 ngày, luân chuyển khi refresh
và thu hồi qua logout. Swagger có nút Authorize để nhập access token.
OWNER quản lý cấu hình/hợp đồng/người dùng; STAFF nhập chỉ số và thanh toán.

## Quy trình tháng

1. Tạo khách thuê và hợp đồng ACTIVE cho phòng VACANT; đặt giá điện, nước,
   tiền thuê và dịch vụ.
2. Xem `GET /api/v1/meter-workflow?month=10&year=2026`, nhập điện tại
   `POST /api/v1/electricity-readings`, nước tại `POST /api/v1/water-readings`.
3. Tạo từng hóa đơn tại `POST /api/v1/invoices/generate` hoặc tạo hàng loạt
   tại `POST /api/v1/invoices/generate-monthly`.
4. Ghi thanh toán tại `POST /api/v1/invoices/{id}/payments`; xem
   `GET /api/v1/dashboard?month=10&year=2026`.

Chỉ số kỳ đầu bắt buộc nhập `previousReading`; kỳ sau tự lấy số cuối kỳ trước,
không bỏ tháng giữa. Nước hỗ trợ METER và PER_PERSON. Dịch vụ METER cần
`POST /api/v1/service-usages`. Tạo hàng loạt báo phòng nào tạo được,
đã có hoặc thiếu dữ liệu; gọi lại không tạo trùng. Hóa đơn giữ từng dòng
InvoiceItem và giá đã chốt.
Tổng hóa đơn = phòng + điện + nước + dịch vụ (gồm Wi-Fi) + khoản khác - giảm giá.

Tiền thuê tính theo số ngày hợp đồng trong tháng; ngày thu vượt ngày cuối
tháng được đưa về ngày cuối. Khi hai hợp đồng cùng thuộc một tháng, backend
báo cần quyết toán thủ công. Nước theo người tính trọn suất cho người có mặt
bất kỳ ngày nào trong tháng. Thanh toán vượt công nợ bị từ chối. Tiền cọc
theo dõi riêng, không cộng vào doanh thu.

## Tiện ích và cấu hình

- `GET /api/v1/invoices/{id}/pdf`: PDF; `GET /api/v1/invoices/export.xlsx?month=10&year=2026`: Excel.
- `POST /api/v1/electricity-readings/{id}/image`: ảnh JPEG/PNG tối đa 2 MB; GET cùng URL để xem.
- `PUT /api/v1/electricity-readings/{id}`: chủ trọ sửa chỉ số chưa lập hóa đơn; GET `/{id}/history` để xem lịch sử.
- `GET /api/v1/invoices/{id}/payment-qr.png`: QR chuyển khoản cho số tiền còn nợ; cần `BANK_BIN`, `BANK_ACCOUNT`, `BANK_NAME`. QR không tự xác nhận đã thu tiền.
- `GET /api/v1/reminders?dueWithinDays=7`: danh sách cần nhắc; chưa tự gửi Zalo/tin nhắn.
- `GET /v3/api-docs`: mô tả API OpenAPI.

Ngoài local, cấp `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`,
`APP_ADMIN_USERNAME`, `APP_ADMIN_PASSWORD` (ít nhất 12 ký tự) và
`JWT_SECRET` (ít nhất 32 byte). Có thể đặt `METER_IMAGES_ROOT`,
`APP_PDF_FONT`, `SERVER_PORT` và thông tin ngân hàng.
Compose mặc định là cấu hình phát triển; dùng file `.env` (đã nằm trong
.gitignore) cho giá trị riêng. Ảnh dùng volume `meter_images`, database
dùng volume `postgres_data`; không chạy `docker compose down -v`
nếu cần giữ dữ liệu.

Flyway V1/V2 từ bộ khung; V3–V6 nâng cấp schema và thêm dịch vụ Wi-Fi. Không sửa
migration đã chạy. Dữ liệu chỉ số trùng phòng/tháng cần xử lý trước khi
migration áp unique constraint. Sao lưu database trước khi nâng dữ liệu thật.

## Kiểm tra

```powershell
.\mvnw.cmd verify
docker compose ps
```

Maven kiểm tra định dạng Java ở bước `validate` và `verify`. Chạy
`.\mvnw.cmd spotless:apply` trước khi commit thay đổi Java.

Testcontainers chạy test PostgreSQL. JaCoCo tạo báo cáo
`target/site/jacoco/index.html` và `verify` yêu cầu tối thiểu 60% line coverage.
CI chạy build, test và Docker Compose.
Giao diện HTML nằm trong `src/main/resources/static` và được Spring Boot phục vụ
cùng cổng 8080. Phiên đăng nhập lưu trong tab trình duyệt và mất khi đóng tab.

Trên macOS/Linux, chạy `./mvnw verify` và
`./mvnw spring-boot:run -Dspring-boot.run.profiles=local` sau khi khởi động PostgreSQL. Maven wrapper có
quyền thực thi trong Git; nếu filesystem không giữ quyền này, dùng `sh ./mvnw`.
PDF dùng font DejaVu đóng gói cùng ứng dụng trên mọi hệ điều hành. Chỉ cần đặt
`APP_PDF_FONT` khi muốn dùng font khác.

Tài khoản `admin / admin` chỉ dành cho profile `local`. Khi triển khai
thật, không dùng profile này: đặt `APP_ADMIN_PASSWORD`, `JWT_SECRET`, thông tin
PostgreSQL riêng. Swagger mặc định tắt ngoài `local`; đặt `APP_API_DOCS_ENABLED=true`
nếu cần mở tài liệu API ở môi trường khác.
