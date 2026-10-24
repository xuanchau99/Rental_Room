#!/usr/bin/env bash

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "===================================================================="
echo "             DỪNG HỆ THỐNG QUẢN LÝ PHÒNG TRỌ                        "
echo "===================================================================="
echo ""
echo "[*] Đang dừng các container dịch vụ (PostgreSQL, App)..."

docker compose down

if [ $? -eq 0 ]; then
    echo ""
    echo "[OK] Toàn bộ dịch vụ đã được dừng an toàn."
    echo "Dữ liệu cơ sở dữ liệu và hình ảnh đồng hồ vẫn được lưu giữ an toàn."
    echo "Khi muốn sử dụng lại, hãy chạy './run.sh'."
else
    echo ""
    echo "[!] Có lỗi xảy ra khi dừng dịch vụ hoặc dịch vụ chưa từng chạy."
fi
