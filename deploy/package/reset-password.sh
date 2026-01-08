#!/bin/bash

# ============================================
#    重置管理员密码
# ============================================

RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}"
echo "============================================"
echo "   重置管理员密码"
echo "============================================"
echo -e "${NC}"

read -p "MySQL密码 [默认: 123456]: " MYSQL_PWD
MYSQL_PWD=${MYSQL_PWD:-123456}

# admin123 的 BCrypt 哈希
# 使用 Spring Security 的 BCryptPasswordEncoder 生成
BCRYPT_HASH='$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKh1O2eq'

echo ""
echo "当前 admin 用户信息:"
mysql -uroot -p"$MYSQL_PWD" -e "SELECT id, username, LEFT(password, 30) as password_prefix, status, deleted FROM student_management.users WHERE username='admin';" 2>/dev/null

echo ""
echo "重置密码为: admin123"
mysql -uroot -p"$MYSQL_PWD" -e "UPDATE student_management.users SET password='$BCRYPT_HASH', status=1, deleted=0 WHERE username='admin';" 2>/dev/null

if [ $? -eq 0 ]; then
    echo -e "${GREEN}密码重置成功！${NC}"
else
    echo -e "${RED}重置失败，尝试插入新用户...${NC}"
    mysql -uroot -p"$MYSQL_PWD" -e "INSERT INTO student_management.users (id, username, password, real_name, user_type, status, deleted, created_at, updated_at) VALUES (1, 'admin', '$BCRYPT_HASH', '系统管理员', 1, 1, 0, NOW(), NOW()) ON DUPLICATE KEY UPDATE password='$BCRYPT_HASH', status=1, deleted=0;" 2>/dev/null
fi

echo ""
echo "更新后的 admin 用户信息:"
mysql -uroot -p"$MYSQL_PWD" -e "SELECT id, username, LEFT(password, 30) as password_prefix, status, deleted FROM student_management.users WHERE username='admin';" 2>/dev/null

echo ""
echo "检查 user_roles 关联:"
mysql -uroot -p"$MYSQL_PWD" -e "SELECT * FROM student_management.user_roles WHERE user_id=1;" 2>/dev/null

# 确保有角色关联
mysql -uroot -p"$MYSQL_PWD" -e "INSERT IGNORE INTO student_management.user_roles (id, user_id, role_id, deleted, created_at, updated_at) VALUES (1, 1, 1, 0, NOW(), NOW());" 2>/dev/null

echo ""
echo -e "${GREEN}============================================${NC}"
echo -e "${GREEN}   完成！${NC}"
echo -e "${GREEN}============================================${NC}"
echo ""
echo "登录信息:"
echo "  用户名: admin"
echo "  密码:   admin123"
