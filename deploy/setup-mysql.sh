#!/bin/bash
# ============================================
# MySQL 数据库配置脚本
# ============================================

echo "=========================================="
echo "  MySQL 数据库配置"
echo "=========================================="

# 提示用户输入
read -p "请输入 MySQL root 新密码 (至少8位，含大小写字母和数字): " MYSQL_ROOT_PWD
read -p "请输入项目数据库密码 (将创建用户 student_admin): " DB_PWD

# 创建 SQL 文件
cat > /tmp/init_db.sql << EOF
-- 修改 root 密码（如果是首次登录）
-- ALTER USER 'root'@'localhost' IDENTIFIED BY '${MYSQL_ROOT_PWD}';

-- 创建数据库
CREATE DATABASE IF NOT EXISTS student_management
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

-- 创建用户
CREATE USER IF NOT EXISTS 'student_admin'@'localhost' IDENTIFIED BY '${DB_PWD}';
CREATE USER IF NOT EXISTS 'student_admin'@'%' IDENTIFIED BY '${DB_PWD}';

-- 授权
GRANT ALL PRIVILEGES ON student_management.* TO 'student_admin'@'localhost';
GRANT ALL PRIVILEGES ON student_management.* TO 'student_admin'@'%';
FLUSH PRIVILEGES;

-- 显示结果
SELECT 'Database and user created successfully!' as Result;
EOF

echo ""
echo "请执行以下命令配置 MySQL:"
echo ""
echo "1. 首先修改 root 密码（使用临时密码登录）:"
echo "   mysql -u root -p"
echo "   ALTER USER 'root'@'localhost' IDENTIFIED BY '你的新密码';"
echo ""
echo "2. 然后执行初始化脚本:"
echo "   mysql -u root -p < /tmp/init_db.sql"
echo ""
echo "3. 导入数据库结构:"
echo "   mysql -u root -p student_management < /opt/student-management/database/schema/complete_schema.sql"
echo "   mysql -u root -p student_management < /opt/student-management/database/init/init_data.sql"
echo ""
echo "SQL 脚本已生成: /tmp/init_db.sql"
