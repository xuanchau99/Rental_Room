@echo off
setlocal
chcp 65001 >nul

title Quản lý phòng trọ - Dừng hệ thống
cd /d "%~dp0"

echo ====================================================================
echo             DỪNG HỆ THỐNG QUẢN LÝ PHÒNG TRỌ                         
echo ====================================================================
echo.

where docker >nul 2>&1
if %ERRORLEVEL% neq 0 (
    if exist "%ProgramFiles%\Docker\Docker\resources\bin\docker.exe" (
        set "PATH=%ProgramFiles%\Docker\Docker\resources\bin;!PATH!"
    ) else if exist "%LOCALAPPDATA%\Programs\DockerDesktop\resources\bin\docker.exe" (
        set "PATH=%LOCALAPPDATA%\Programs\DockerDesktop\resources\bin;!PATH!"
    )
)

echo [*] Đang dừng các container dịch vụ (PostgreSQL, App)...
docker compose down

if %ERRORLEVEL% equ 0 (
    echo.
    echo [OK] Toàn bộ dịch vụ đã được dừng an toàn.
    echo Dữ liệu cơ sở dữ liệu và hình ảnh đồng hồ vẫn được lưu giữ an toàn.
    echo Khi muốn sử dụng lại, hãy chạy file 'run.bat'.
) else (
    echo.
    echo [!] Có lỗi xảy ra khi dừng dịch vụ hoặc dịch vụ chưa từng chạy.
)

echo.
echo Bấm phím bất kỳ để thoát...
pause >nul
