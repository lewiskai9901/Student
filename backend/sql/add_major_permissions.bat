@echo off
chcp 65001 > nul
echo 正在添加专业管理权限...

"C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u root -p123456 student_management --default-character-set=utf8mb4 < major_permissions.sql

if %ERRORLEVEL% == 0 (
    echo 专业管理权限添加成功!
) else (
    echo 添加失败,请检查错误信息
)

pause
