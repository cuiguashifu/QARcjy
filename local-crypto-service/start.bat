@echo off
chcp 65001 >nul
echo ========================================
echo   QAR 本地加密服务启动器
echo ========================================
echo.

cd /d "%~dp0"

if not exist "target\local-crypto-service-1.0.0.jar" (
    echo [错误] 未找到服务JAR文件
    echo [提示] 请先运行: mvn clean package
    echo.
    pause
    exit /b 1
)

echo [启动] 正在启动本地加密服务...
echo [端口] 18234
echo [地址] http://127.0.0.1:18234
echo.
echo 按 Ctrl+C 停止服务
echo ========================================
echo.

java -jar target\local-crypto-service-1.0.0.jar

pause
