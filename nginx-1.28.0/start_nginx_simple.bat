@echo off
echo ========================================
echo 启动Nginx（使用极简配置）
echo ========================================
echo.

echo [1/3] 停止现有Nginx进程...
taskkill /F /IM nginx.exe >nul 2>&1
timeout /t 2 /nobreak >nul

echo [2/3] 验证配置文件...
cd /d "%~dp0"
nginx.exe -t
if %errorlevel% neq 0 (
    echo.
    echo 配置验证失败！请检查配置文件。
    pause
    exit /b 1
)

echo [3/3] 启动Nginx...
start /B nginx.exe
timeout /t 2 /nobreak >nul

echo.
echo ========================================
echo 检查Nginx状态...
netstat -ano | findstr ":80"
echo.
echo ========================================
echo.
echo Nginx已启动！
echo 请访问 http://localhost/admin/ 测试
echo.
pause
