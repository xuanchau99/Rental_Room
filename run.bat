@echo off
setlocal EnableDelayedExpansion
chcp 65001 >nul

title Quản lý phòng trọ - Home Management
cd /d "%~dp0"

echo ====================================================================
echo             HỆ THỐNG QUẢN LÝ PHÒNG TRỌ (HOME MANAGEMENT)            
echo ====================================================================
echo.

REM 1. Kiem tra docker CLI trong PATH
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
echo Sau khi cai dat xong, hay mo lai file run.bat nay.
echo.
pause
exit /b 1

:docker_cli_found
REM 2. Kiem tra Docker Daemon
echo [1/3] Kiem tra ket noi Docker Engine...
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
echo Sau do hay chay lai file run.bat nay.
echo.
pause
exit /b 1

:docker_daemon_ready
echo.
echo [OK] Docker Engine da san sang.
echo.

REM Kiem tra neu co cau hinh Cloud Database (Neon DB) trong file .env
set "USE_NEON=0"
if exist ".env" (
    findstr /i "neon.tech" .env >nul 2>&1
    if !ERRORLEVEL! equ 0 set "USE_NEON=1"
)

if "!USE_NEON!"=="1" (
    echo [i] Phat hien cau hinh Cloud Database Neon DB trong file .env!
    echo [*] Ung dung se ket noi truc tiep toi Cloud Database tren Internet.
    echo [*] Bo qua khoi dong PostgreSQL local de tiet kiem tai nguyen.
    echo.
    echo [2/3] Dang build va khoi dong ung dung backend...
    docker compose up -d --build --wait --no-deps app
) else (
    REM Kiem tra xung dot cong 5432
    netstat -ano | findstr /C:":5432 " | findstr "LISTENING" >nul 2>&1
    if !ERRORLEVEL! equ 0 (
        echo [i] Cong 5432 tren may dang duoc su dung boi ung dung khac.
        echo [*] Tu dong chuyen cong PostgreSQL sang 127.0.0.1:5433 de tranh xung dot.
        set "DB_PORT=127.0.0.1:5433"
    ) else (
        set "DB_PORT=127.0.0.1:5432"
    )

    echo [2/3] Dang build va khoi dong he thong (PostgreSQL va Spring Boot Backend)...
    echo Qua trinh nay se tu dong khoi tao co so du lieu va du lieu mau.
    echo.
    docker compose up -d --build --wait
)

if %ERRORLEVEL% neq 0 (
    echo.
    echo [LOI] Docker Compose khong the khoi dong thanh cong!
    echo Kiem tra log chi tiet bang lenh: docker compose logs
    echo.
    pause
    exit /b 1
)

echo.
echo [3/3] He thong da khoi dong thanh cong!
echo ====================================================================
echo                      THONG TIN KET NOI                             
echo ====================================================================
echo   - Giao dien Web (UI):      http://localhost:8080/
echo   - Tai lieu API (Swagger):  http://localhost:8080/swagger-ui.html
if "!USE_NEON!"=="1" (
echo   - Co so du lieu:           Neon DB (Cloud PostgreSQL)
) else (
echo   - Co so du lieu:           PostgreSQL local tai !DB_PORT!
)
echo   - Tai khoan mac dinh:      admin (hoac mat khau trong .env)
echo --------------------------------------------------------------------
echo   * De DUNG he thong:        Nhay dup vao file stop.bat
echo   * Xem log he thong:        Mo terminal go: docker compose logs -f
echo ====================================================================
echo.
echo Dang tu dong mo trinh duyet...
ping 127.0.0.1 -n 3 >nul
start http://localhost:8080/

echo.
echo Ung dung dang chay ngam. Ban co the dong cua so nay bat ky luc nao.
echo Nhan phim bat ky de thoat...
pause >nul
