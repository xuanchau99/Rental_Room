#!/usr/bin/env bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "===================================================================="
echo "             KẾT NỐI HỆ THỐNG VỚI NEON CLOUD DATABASE                "
echo "===================================================================="
echo ""

# 1. Khởi tạo file .env nếu chưa có
if [ ! -f ".env" ]; then
    echo "[*] Chưa tìm thấy file .env, đang khởi tạo từ cấu hình Neon DB..."
    cat << 'EOF' > .env
# Cấu hình Neon DB
SPRING_PROFILES_ACTIVE=local
DB_URL=jdbc:postgresql://ep-lively-shape-az8oo4id.c-3.ap-southeast-1.aws.neon.tech/neondb?sslmode=require&channel_binding=require
DB_USERNAME=neondb_owner
DB_PASSWORD=npg_7QKjWxeSc3hf
APP_ADMIN_PASSWORD=chau12345
JWT_SECRET=1cea4c48e03765074d15dc39a97a2d11
SERVER_PORT=10000
EOF
    echo "[OK] Đã tạo file .env thành công!"
    echo ""
fi

# 2. Kiểm tra Docker CLI
if ! command -v docker &> /dev/null; then
    echo "[LỖI] Không tìm thấy Docker CLI trên hệ thống!"
    echo "Vui lòng cài đặt Docker rồi thử lại."
    exit 1
fi

# 3. Kiểm tra Docker Engine
echo "[1/2] Đang kiểm tra dịch vụ Docker Engine..."
if ! docker info &> /dev/null; then
    echo "[!] Docker daemon chưa chạy. Vui lòng khởi động Docker daemon / Docker Desktop."
    for i in {1..30}; do
        if docker info &> /dev/null; then
            break
        fi
        printf "."
        sleep 2
    done
    echo ""
    if ! docker info &> /dev/null; then
        echo "[LỖI] Không thể kết nối tới Docker Engine. Vui lòng bật Docker và thử lại."
        exit 1
    fi
fi
echo "[OK] Docker Engine đã sẵn sàng."
echo ""

# 4. Khởi động app kết nối Neon DB
echo "[2/2] Đang build và khởi động ứng dụng kết nối tới Neon DB Cloud..."
docker compose up -d --build --wait app

echo ""
echo "===================================================================="
echo "             KẾT NỐI NEON DB THÀNH CÔNG!                            "
echo "===================================================================="
echo "  - Giao diện Web (UI):      http://localhost:8080/"
echo "  - Tài liệu API (Swagger):  http://localhost:8080/swagger-ui.html"
echo "  - Cơ sở dữ liệu:           Neon DB (ep-lively-shape-az8oo4id...)"
echo "  - Tài khoản đăng nhập:     admin / chau12345"
echo "--------------------------------------------------------------------"
echo "  * Để DỪNG ứng dụng:        ./stop.sh"
echo "  * Xem log trực tiếp:       docker compose logs -f app"
echo "===================================================================="
echo ""

URL="http://localhost:8080/"
if command -v xdg-open &> /dev/null; then
    xdg-open "$URL" &> /dev/null || true
elif command -v open &> /dev/null; then
    open "$URL" &> /dev/null || true
elif command -v powershell.exe &> /dev/null; then
    powershell.exe -NoProfile -Command "Start-Process '$URL'" &> /dev/null || true
fi

echo "Ứng dụng đang chạy nền tại $URL"
