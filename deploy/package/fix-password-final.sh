#!/bin/bash

# ============================================
#    最终密码修复
# ============================================

GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}更新密码...${NC}"

mysql -uroot -p123456 student_management <<'EOF'
UPDATE users SET password='$2a$10$w7zuZzW50gCkY/0P9sJQzOd/UeV4.8tyx.Z3iB2LghnS0mGsT369O' WHERE username='admin';
EOF

echo -e "${BLUE}测试登录...${NC}"

RESPONSE=$(curl -s -X POST http://127.0.0.1:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}')

if echo "$RESPONSE" | grep -q "accessToken"; then
    echo -e "${GREEN}============================================${NC}"
    echo -e "${GREEN}   登录成功！${NC}"
    echo -e "${GREEN}============================================${NC}"
    echo ""
    echo "访问地址: http://43.140.251.18"
    echo "账号: admin"
    echo "密码: admin123"
else
    echo "登录结果: $RESPONSE"
fi
