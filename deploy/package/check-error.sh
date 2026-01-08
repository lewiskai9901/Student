#!/bin/bash

# ============================================
#    学生管理系统 - 错误排查脚本
# ============================================

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

INSTALL_DIR="/www/wwwroot/student-system"

echo -e "${BLUE}"
echo "============================================"
echo "   学生管理系统 - 错误排查"
echo "============================================"
echo -e "${NC}"

# ============================================
# 1. 检查8080端口占用
# ============================================
echo -e "${BLUE}[1/4] 检查8080端口占用${NC}"
echo ""
if command -v ss &> /dev/null; then
    ss -tlnp | grep ":8080" || echo "8080端口未被占用"
elif command -v netstat &> /dev/null; then
    netstat -tlnp | grep ":8080" || echo "8080端口未被占用"
fi
echo ""

# ============================================
# 2. 查看配置文件
# ============================================
echo -e "${BLUE}[2/4] 当前配置文件内容${NC}"
echo ""
if [ -f "$INSTALL_DIR/backend/application-prod.yml" ]; then
    echo -e "${YELLOW}--- application-prod.yml ---${NC}"
    cat "$INSTALL_DIR/backend/application-prod.yml"
else
    echo -e "${RED}配置文件不存在！${NC}"
fi
echo ""

# ============================================
# 3. 查看启动错误
# ============================================
echo -e "${BLUE}[3/4] 启动错误日志${NC}"
echo ""
if [ -f "$INSTALL_DIR/logs/app.log" ]; then
    # 查找占位符错误
    echo -e "${YELLOW}--- 占位符解析错误 ---${NC}"
    grep -i "Could not resolve placeholder" "$INSTALL_DIR/logs/app.log" | tail -5
    echo ""

    # 查找其他常见错误
    echo -e "${YELLOW}--- 其他错误信息 ---${NC}"
    grep -E "(Error|Exception|Failed|错误)" "$INSTALL_DIR/logs/app.log" | grep -v "at " | tail -20
    echo ""

    # 查看日志开头（启动信息）
    echo -e "${YELLOW}--- 启动日志前50行 ---${NC}"
    head -50 "$INSTALL_DIR/logs/app.log"
else
    echo -e "${RED}日志文件不存在！${NC}"
fi
echo ""

# ============================================
# 4. 尝试修复并重启
# ============================================
echo -e "${BLUE}[4/4] 修复建议${NC}"
echo ""

# 检查常见问题
CONFIG_FILE="$INSTALL_DIR/backend/application-prod.yml"
if [ -f "$CONFIG_FILE" ]; then
    # 检查JWT密钥
    JWT_SECRET=$(grep "secret:" "$CONFIG_FILE" | head -1 | awk '{print $2}')
    if [ -z "$JWT_SECRET" ] || [ ${#JWT_SECRET} -lt 32 ]; then
        echo -e "${RED}问题: JWT密钥为空或太短${NC}"
        echo "  修复: 需要重新生成JWT密钥"
    fi

    # 检查MySQL密码是否包含特殊字符
    if grep -q "password:.*[&!@#\$%^*()]" "$CONFIG_FILE"; then
        echo -e "${YELLOW}警告: MySQL密码包含特殊字符，可能需要用引号包裹${NC}"
    fi
fi

echo ""
read -p "是否尝试修复配置并重启? (y/n): " FIX

if [ "$FIX" = "y" ]; then
    echo ""
    echo -e "${BLUE}开始修复...${NC}"

    # 停止可能占用8080的进程
    echo "1. 停止占用8080端口的进程..."
    fuser -k 8080/tcp 2>/dev/null || true
    pkill -f "student-management" 2>/dev/null || true
    sleep 2

    # 重新生成配置文件
    echo "2. 请输入配置信息..."
    read -p "   MySQL密码: " -s MYSQL_PWD
    echo ""

    # 生成新的JWT密钥
    JWT_KEY=$(head -c 64 /dev/urandom 2>/dev/null | base64 | tr -d '\n' | head -c 64)
    if [ -z "$JWT_KEY" ]; then
        JWT_KEY=$(date +%s%N | sha256sum | base64 | head -c 64)
    fi

    echo "3. 生成新配置文件..."
    cat > "$CONFIG_FILE" << EOF
server:
  port: 8080
  servlet:
    context-path: /api

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/student_management?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
    username: root
    password: "${MYSQL_PWD}"
    driver-class-name: com.mysql.cj.jdbc.Driver
  redis:
    host: localhost
    port: 6379
    password: ""

jwt:
  secret: ${JWT_KEY}
  access-token-expiration: 7200000
  refresh-token-expiration: 2592000000

file:
  upload-path: ${INSTALL_DIR}/uploads/

logging:
  file:
    path: ${INSTALL_DIR}/logs
  level:
    root: INFO
    com.school: DEBUG
EOF

    echo "4. 启动后端服务..."
    cd "$INSTALL_DIR/backend"

    # 清空旧日志
    > "$INSTALL_DIR/logs/app.log"

    # 启动
    nohup java -jar -Xms256m -Xmx512m -Dspring.profiles.active=prod -Dfile.encoding=UTF-8 student-management-system-1.0.0.jar > "$INSTALL_DIR/logs/app.log" 2>&1 &

    echo "5. 等待启动（10秒）..."
    sleep 10

    # 检查是否启动成功
    if ps -ef | grep -v grep | grep "student-management-system" > /dev/null; then
        echo -e "${GREEN}后端启动成功！${NC}"

        # 测试接口
        echo ""
        echo "6. 测试接口..."
        RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" http://127.0.0.1:8080/api/auth/login --max-time 5 2>/dev/null)
        if [ "$RESPONSE" = "405" ] || [ "$RESPONSE" = "400" ] || [ "$RESPONSE" = "401" ]; then
            echo -e "${GREEN}接口响应正常 (HTTP $RESPONSE)${NC}"
        else
            echo -e "${YELLOW}接口响应: HTTP $RESPONSE${NC}"
            echo "查看日志: tail -50 $INSTALL_DIR/logs/app.log"
        fi
    else
        echo -e "${RED}后端启动失败！${NC}"
        echo ""
        echo "错误日志:"
        tail -30 "$INSTALL_DIR/logs/app.log"
    fi

    echo ""
    echo -e "${GREEN}修复完成！请刷新浏览器访问网站${NC}"
fi
