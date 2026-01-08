#!/bin/bash

# ============================================
#    修复配置并启动
# ============================================

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

INSTALL_DIR="/www/wwwroot/student-system"
APP_NAME="student-management-system-1.0.0.jar"
JAVA17_HOME="/www/server/java/jdk-17.0.8"

echo -e "${BLUE}"
echo "============================================"
echo "   修复配置并启动"
echo "============================================"
echo -e "${NC}"

# 停止旧进程
echo -e "${BLUE}[1/4] 停止旧进程${NC}"
pkill -f "$APP_NAME" 2>/dev/null || true
fuser -k 8080/tcp 2>/dev/null || true
sleep 2
echo -e "${GREEN}完成${NC}"

# 获取配置信息
echo ""
echo -e "${BLUE}[2/4] 配置信息${NC}"
read -p "MySQL密码 [默认: 123456]: " MYSQL_PWD
MYSQL_PWD=${MYSQL_PWD:-123456}

read -p "Redis密码 [默认: 无密码，直接回车]: " REDIS_PWD

# 生成 JWT 密钥（64字节以上）
JWT_KEY=$(cat /dev/urandom | tr -dc 'a-zA-Z0-9' | fold -w 80 | head -n 1)
if [ -z "$JWT_KEY" ]; then
    JWT_KEY="student-management-system-jwt-secret-key-2024-base64-encoded-long-enough-for-hs512-algorithm-requirement"
fi

echo ""
echo -e "${BLUE}[3/4] 生成配置文件${NC}"

# 生成正确的配置文件
cat > "$INSTALL_DIR/backend/application-prod.yml" << EOF
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
    hikari:
      minimum-idle: 5
      maximum-pool-size: 20
      idle-timeout: 30000
      max-lifetime: 1800000
      connection-timeout: 30000
  redis:
    host: localhost
    port: 6379
    password: "${REDIS_PWD}"
    timeout: 10000ms
  servlet:
    multipart:
      max-file-size: 50MB
      max-request-size: 50MB

jwt:
  secret-key: ${JWT_KEY}
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

mybatis-plus:
  configuration:
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
EOF

echo -e "${GREEN}配置文件已生成${NC}"
echo ""
cat "$INSTALL_DIR/backend/application-prod.yml"
echo ""

# 启动后端
echo -e "${BLUE}[4/4] 启动后端${NC}"

export JAVA_HOME="$JAVA17_HOME"
export PATH="$JAVA_HOME/bin:$PATH"

# 清空旧日志
> "$INSTALL_DIR/logs/app.log"

cd "$INSTALL_DIR/backend"
nohup "$JAVA17_HOME/bin/java" -jar \
    -Xms256m -Xmx512m \
    -Dspring.profiles.active=prod \
    -Dfile.encoding=UTF-8 \
    "$APP_NAME" > "$INSTALL_DIR/logs/app.log" 2>&1 &

echo "等待启动（20秒）..."

# 等待并检查
for i in {1..20}; do
    sleep 1
    if curl -s http://127.0.0.1:8080/api/auth/login --max-time 2 > /dev/null 2>&1; then
        echo ""
        echo -e "${GREEN}============================================${NC}"
        echo -e "${GREEN}   启动成功！${NC}"
        echo -e "${GREEN}============================================${NC}"
        echo ""
        echo "访问地址: http://你的服务器IP"
        echo "账号: admin"
        echo "密码: admin123"
        echo ""
        echo "管理命令:"
        echo "  启动: $INSTALL_DIR/ctl.sh start"
        echo "  停止: $INSTALL_DIR/ctl.sh stop"
        echo "  日志: $INSTALL_DIR/ctl.sh log"
        exit 0
    fi
    echo -n "."
done

echo ""

# 检查进程
if ps -ef | grep -v grep | grep "$APP_NAME" > /dev/null; then
    echo -e "${YELLOW}进程已启动，但接口未响应，继续等待...${NC}"
    sleep 10

    RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" http://127.0.0.1:8080/api/auth/login --max-time 5 2>/dev/null)
    if [ "$RESPONSE" = "405" ] || [ "$RESPONSE" = "400" ] || [ "$RESPONSE" = "401" ]; then
        echo -e "${GREEN}启动成功！HTTP $RESPONSE${NC}"
        exit 0
    fi
fi

echo -e "${RED}启动失败，查看日志:${NC}"
echo ""
tail -50 "$INSTALL_DIR/logs/app.log"
