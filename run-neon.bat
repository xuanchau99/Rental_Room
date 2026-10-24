@echo off
setlocal EnableDelayedExpansion
chcp 65001 >nul

title Quản lý phòng trọ - Kết nối Neon DB
cd /d "%~dp0"

echo ====================================================================
echo             KẾT NỐI HỆ THỐNG VỚI NEON CLOUD DATABASE                
echo ====================================================================
echo.

REM 1. Kiem tra file .env, neu chua co thi tao mau tu cau hinh Neon
if not exist ".env" (
    echo [*] Chua tim thay file .env, dang khoi tao file .env voi cau hinh Neon DB cua ban...
    (
        echo # Cau hinh Neon DB
        echo SPRING_PROFILES_ACTIVE=local
        echo DB_URL=jdbc:postgresql://ep-lively-shape-az8oo4id.c-3.ap-southeast-1.aws.neon.tech/neondb?sslmode=require^&channel_binding=require
        echo DB_USERNAME=neondb_owner
        echo DB_PASSWORD=npg_7QKjWxeSc3hf
        echo APP_ADMIN_PASSWORD=chau12345
        echo JWT_SECRET=1cea4c48e03765074d15dc39a97a2d11
        echo SERVER_PORT=10000
    ) > .env
    echo [OK] Da tao file .env thanh cong!
    echo.
)

REM 2. Kiem tra docker CLI trong PATH
where docker >nul 2>&1
if %ERRORLEVEL% equ 0 goto :docker_cli_found

if exist "%ProgramFiles%\Docker\Docker\resources\bin\docker.exe" (
    set "PATH=%ProgramFiles%\Docker\Docker\resources\bin;!PATH!"
    goto :docker_cli_found
)
if exist "%LOCALAPPDATA%\Programs\DockerDesktop\resources\bin\docker.exe" (
    set "PATH=%LOCALAPPDATA%\Programs\DockerDesktop\resources\bin;!PATH!"
    goto :docker_cli_found
)

echo [LOI] Khong tim thay Docker tren he thong!
echo Vui long cai dat Docker Desktop tai: https://www.docker.com/products/docker-desktop
echo Sau khi cai dat xong, hay mo lai file nay.
echo.
pause
exit /b 1

:docker_cli_found
REM 3. Kiem tra Docker Daemon
echo [1/2] Kiem tra ket noi Docker Engine...
docker info >nul 2>&1
if %ERRORLEVEL% equ 0 goto :docker_daemon_ready

echo [*] Docker Desktop chua chay. Dang tien hanh bat Docker Desktop...
set "DOCKER_EXE="
if exist "%ProgramFiles%\Docker\Docker\Docker Desktop.exe" (
    set "DOCKER_EXE=%ProgramFiles%\Docker\Docker\Docker Desktop.exe"
)
if not defined DOCKER_EXE if exist "%LOCALAPPDATA%\Programs\Docker\Docker\Docker Desktop.exe" (
    set "DOCKER_EXE=%LOCALAPPDATA%\Programs\Docker\Docker\Docker Desktop.exe"
)

if defined DOCKER_EXE (
    start "" "!DOCKER_EXE!"
    echo [*] Da gui lenh mo Docker Desktop. Vui long doi khoang 30-60 giay de engine khoi dong.
) else (
    echo [!] Vui long mo thu cong ung dung Docker Desktop tren may tinh cua ban.
    echo [*] Dang doi Docker Engine khoi dong...
)

set "DOCKER_WAIT_COUNT=0"

:wait_docker_loop
set /a DOCKER_WAIT_COUNT+=1
if !DOCKER_WAIT_COUNT! gtr 40 goto :docker_timeout

docker info >nul 2>&1
if %ERRORLEVEL% equ 0 goto :docker_daemon_ready

echo Dang doi Docker khoi dong... (!DOCKER_WAIT_COUNT!/40)
ping 127.0.0.1 -n 4 >nul
goto :wait_docker_loop

:docker_timeout
echo.
echo [CANH BAO] Docker Engine khoi dong lau hon du kien.
echo Hay dam bao ung dung Docker Desktop dang chay va co bieu tuong mau xanh.
echo Sau do hay chay lai file run-neon.bat nay.
echo.
pause
exit /b 1

:docker_daemon_ready
echo.
echo [OK] Docker Engine da san sang.
echo.

REM 4. Chay Backend ket noi truc tiep toi Neon DB
echo [2/2] Dang build va khoi dong ung dung ket noi toi Neon DB Cloud...
echo.

docker compose up -d --build --wait --no-deps app
if %ERRORLEVEL% neq 0 (
    echo.
    echo [LOI] Khoi dong ung dung khong thanh cong!
    echo Kiem tra log chi tiet bang lenh: docker compose logs app
    echo.
    pause
    exit /b 1
)

echo.
echo ====================================================================
echo             KẾT NỐI NEON DB THÀNH CÔNG!                            
echo ====================================================================
echo   - Giao dien Web (UI):      http://localhost:8080/
echo   - Tai lieu API (Swagger):  http://localhost:8080/swagger-ui.html
echo   - Co so du lieu:           Neon DB (ep-lively-shape-az8oo4id...)
echo   - Tai khoan mac dinh:      admin / chau12345
echo --------------------------------------------------------------------
echo   * De DUNG ung dung:        Nhay dup vao file stop.bat
echo   * Xem log truc tiep:       Mo terminal go: docker compose logs -f app
echo ====================================================================
echo.
echo Dang tu dong mo trinh duyet...
ping 127.0.0.1 -n 3 >nul
start http://localhost:8080/

echo.
echo Ung dung dang chay ngam ket noi toi Neon DB.
echo Nhan phim bat ky de thoat...
pause >nul
