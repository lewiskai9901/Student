#!/bin/bash

# ============================================
#    调试登录问题
# ============================================

RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

INSTALL_DIR="/www/wwwroot/student-system"

echo -e "${BLUE}"
echo "============================================"
echo "   调试登录问题"
echo "============================================"
echo -e "${NC}"

read -p "MySQL密码 [默认: 123456]: " MYSQL_PWD
MYSQL_PWD=${MYSQL_PWD:-123456}

# 1. 检查用户表
echo -e "${BLUE}[1] 用户表数据${NC}"
mysql -uroot -p"$MYSQL_PWD" -e "SELECT id, username, password, real_name, user_type, status, deleted FROM student_management.users LIMIT 5;" 2>/dev/null
echo ""

# 2. 检查角色关联
echo -e "${BLUE}[2] 用户角色关联${NC}"
mysql -uroot -p"$MYSQL_PWD" -e "SELECT ur.*, r.role_name, r.role_code FROM student_management.user_roles ur LEFT JOIN student_management.roles r ON ur.role_id = r.id WHERE ur.user_id = 1;" 2>/dev/null
echo ""

# 3. 检查角色表
echo -e "${BLUE}[3] 角色表${NC}"
mysql -uroot -p"$MYSQL_PWD" -e "SELECT * FROM student_management.roles LIMIT 5;" 2>/dev/null
echo ""

# 4. 测试登录接口
echo -e "${BLUE}[4] 测试登录接口${NC}"
RESPONSE=$(curl -s -X POST http://127.0.0.1:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' 2>/dev/null)
echo "响应: $RESPONSE"
echo ""

# 5. 查看后端日志
echo -e "${BLUE}[5] 后端日志 (最近登录相关)${NC}"
if [ -f "$INSTALL_DIR/logs/app.log" ]; then
    grep -i "login\|password\|auth\|用户名\|密码" "$INSTALL_DIR/logs/app.log" | tail -20
fi
echo ""

# 6. 检查密码格式
echo -e "${BLUE}[6] 密码格式检查${NC}"
PWD_HASH=$(mysql -uroot -p"$MYSQL_PWD" -N -e "SELECT password FROM student_management.users WHERE username='admin';" 2>/dev/null)
echo "当前密码哈希: $PWD_HASH"
echo "哈希长度: ${#PWD_HASH}"

if [[ "$PWD_HASH" == \$2a\$* ]] || [[ "$PWD_HASH" == \$2b\$* ]]; then
    echo -e "${GREEN}密码格式正确 (BCrypt)${NC}"
else
    echo -e "${RED}密码格式可能不对${NC}"
fi
echo ""

# 7. 修复尝试
echo -e "${BLUE}[7] 尝试修复${NC}"
read -p "是否尝试用后端生成的密码更新? (y/n): " FIX

if [ "$FIX" = "y" ]; then
    # 调用后端接口生成密码（如果有这样的接口）
    # 或者使用已知正确的哈希

    # 这个哈希是 "admin123" 用 BCryptPasswordEncoder 生成的标准哈希
    NEW_HASH='$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36WQoeG6Lruj3vjPGga31lW'

    echo "使用新密码哈希..."
    mysql -uroot -p"$MYSQL_PWD" -e "UPDATE student_management.users SET password='$NEW_HASH' WHERE username='admin';" 2>/dev/null

    echo "重新测试登录..."
    sleep 1
    RESPONSE=$(curl -s -X POST http://127.0.0.1:8080/api/auth/login \
      -H "Content-Type: application/json" \
      -d '{"username":"admin","password":"admin123"}' 2>/dev/null)
    echo "响应: $RESPONSE"

    if echo "$RESPONSE" | grep -q "token"; then
        echo -e "${GREEN}登录成功！${NC}"
    else
        echo -e "${YELLOW}仍然失败，查看完整日志...${NC}"
        tail -30 "$INSTALL_DIR/logs/app.log"
    fi
fi

echo ""
echo "============================================"
echo "诊断完成"
echo "============================================"
