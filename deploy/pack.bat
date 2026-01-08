@echo off
chcp 65001 >nul
echo ============================================
echo    学生管理系统 - 本地打包脚本
echo ============================================
echo.

cd /d %~dp0..

echo [1/4] 打包后端...
cd backend
call mvn clean package -DskipTests -Dfile.encoding=UTF-8
if %errorlevel% neq 0 (
    echo 后端打包失败！
    pause
    exit /b 1
)
cd ..

echo.
echo [2/4] 打包前端...
cd frontend
call npm run build
if %errorlevel% neq 0 (
    echo 前端打包失败！
    pause
    exit /b 1
)
cd ..

echo.
echo [3/4] 创建部署包目录...
if exist deploy\package rmdir /s /q deploy\package
mkdir deploy\package
mkdir deploy\package\backend
mkdir deploy\package\frontend
mkdir deploy\package\database

echo.
echo [4/4] 复制文件到部署包...
copy backend\target\student-management-system-1.0.0.jar deploy\package\backend\
xcopy frontend\dist\* deploy\package\frontend\ /s /e /q
copy database\schema\complete_schema.sql deploy\package\database\
copy database\init\init_data.sql deploy\package\database\
copy deploy\baota-deploy.sh deploy\package\deploy.sh

echo.
echo ============================================
echo    打包完成！
echo ============================================
echo.
echo 部署包位置: %cd%\deploy\package
echo.
echo ============================================
echo    部署步骤:
echo ============================================
echo 1. 将 deploy\package 整个目录上传到服务器 /root/
echo 2. SSH连接服务器，执行:
echo    cd /root/package
echo    chmod +x deploy.sh
echo    ./deploy.sh
echo.
echo 脚本会自动完成: 安装JDK、配置数据库、部署前后端、配置Nginx
echo.
pause
