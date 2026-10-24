#!/usr/bin/env bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "===================================================================="
echo "             HỆ THỐNG QUẢN LÝ PHÒNG TRỌ (HOME MANAGEMENT)            "
echo "===================================================================="
echo ""

# 1. Kiểm tra Docker CLI
if ! command -v docker &> /dev/null; then
    echo "[LỖI] Không tìm thấy Docker CLI trên hệ thống!"
    echo "Vui lòng cài đặt Docker (hoặc Docker Desktop) rồi thử lại."
    exit 1
fi

# 2. Kiểm tra Docker Engine
echo "[1/3] Đang kiểm tra dịch vụ Docker Engine..."
if ! docker info &> /dev/null; then
    echo "[!] Docker daemon chưa chạy. Vui lòng khởi động Docker daemon / Docker Desktop."
    echo "[*] Đang đợi kết nối tới Docker..."
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

# Kiểm tra nếu cấu hình Neon DB trong .env
USE_NEON=0
if [ -f ".env" ] && grep -qi "neon.tech" .env; then
    USE_NEON=1
fi

if [ "$USE_NEON" = "1" ]; then
    echo "[i] Phát hiện cấu hình Cloud Database (Neon DB) trong file .env!"
    echo "[*] Ứng dụng sẽ kết nối trực tiếp tới Neon DB Cloud."
    echo "[*] Bỏ qua khởi động PostgreSQL local để tiết kiệm tài nguyên."
    echo ""
    echo "[2/3] Đang build và khởi động ứng dụng backend..."
    docker compose up -d --build --wait --no-deps app
else
    # Kiểm tra xung đột cổng 5432
    if netstat -tuln 2>/dev/null | grep -q ":5432 " || ss -tuln 2>/dev/null | grep -q ":5432 " || lsof -i :5432 &>/dev/null; then
        echo "[i] Cổng 5432 đang bận bởi ứng dụng khác. Tự động chuyển cổng PostgreSQL sang 127.0.0.1:5433..."
        export DB_PORT="127.0.0.1:5433"
    else
        export DB_PORT="127.0.0.1:5432"
    fi

    echo "[2/3] Đang tải, build và khởi động hệ thống (PostgreSQL + Spring Boot)..."
    docker compose up -d --build --wait
fi

echo ""
echo "[3/3] Hệ thống đã khởi động thành công!"
echo "===================================================================="
echo "                     THÔNG TIN KẾT NỐI                              "
echo "===================================================================="
echo "  - Giao diện Web (UI):      http://localhost:8080/"
echo "  - Tài liệu API (Swagger):  http://localhost:8080/swagger-ui.html"
if [ "$USE_NEON" = "1" ]; then
echo "  - Cơ sở dữ liệu:           Neon DB (Cloud PostgreSQL)"
else
echo "  - Cơ sở dữ liệu:           PostgreSQL tai $DB_PORT"
fi
echo "  - Tài khoản đăng nhập:     admin (hoặc mật khẩu cấu hình trong .env)"
echo "--------------------------------------------------------------------"
echo "  * Để DỪNG toàn bộ hệ thống: ./stop.sh"
echo "  * Để xem log:               docker compose logs -f"
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
