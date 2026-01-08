#!/bin/bash

# ============================================
#    修复配置并启动 (最终版)
# ============================================

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

INSTALL_DIR="/www/wwwroot/student-system"
APP_NAME="student-management-system-1.0.0.jar"
JAVA_HOME="/www/server/java/jdk-17.0.8"

echo -e "${BLUE}"
echo "============================================"
echo "   修复配置并启动 (最终版)"
echo "============================================"
echo -e "${NC}"

# 获取服务器IP
SERVER_IP=$(curl -s ifconfig.me --max-time 5 2>/dev/null || echo "43.140.251.18")

# 1. 停止进程
echo -e "${BLUE}[1/4] 停止进程${NC}"
pkill -f "$APP_NAME" 2>/dev/null || true
fuser -k 8080/tcp 2>/dev/null || true
sleep 2
echo -e "${GREEN}完成${NC}"

# 2. 生成正确的配置文件
echo ""
echo -e "${BLUE}[2/4] 生成配置文件${NC}"

cat > "$INSTALL_DIR/backend/application-prod.yml" << EOF
server:
  port: 8080
  servlet:
    context-path: /api

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/student_management?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
    username: root
    password: "123456"
    driver-class-name: com.mysql.cj.jdbc.Driver
    hikari:
      minimum-idle: 5
      maximum-pool-size: 20
      idle-timeout: 30000
      max-lifetime: 1800000
      connection-timeout: 30000

  data:
    redis:
      host: localhost
      port: 6379
      password: ""
      timeout: 10000ms

  servlet:
    multipart:
      max-file-size: 50MB
      max-request-size: 50MB

jwt:
  secret-key: student-management-system-jwt-secret-key-2024-base64-encoded-long-enough-for-hs512-algorithm

file:
  upload:
    path: ${INSTALL_DIR}/uploads
    url-prefix: http://${SERVER_IP}/uploads

logging:
  file:
    path: ${INSTALL_DIR}/logs
  level:
    root: INFO
    com.school: DEBUG

mybatis-plus:
  mapper-locations: classpath*:/mapper/**/*.xml
  configuration:
    map-underscore-to-camel-case: true
EOF

echo -e "${GREEN}配置文件已生成${NC}"
echo ""
cat "$INSTALL_DIR/backend/application-prod.yml"
echo ""

# 3. 创建目录
echo -e "${BLUE}[3/4] 创建目录${NC}"
mkdir -p "$INSTALL_DIR/uploads"
mkdir -p "$INSTALL_DIR/logs"
echo -e "${GREEN}完成${NC}"

# 4. 启动后端
echo ""
echo -e "${BLUE}[4/4] 启动后端${NC}"

export JAVA_HOME="$JAVA_HOME"
export PATH="$JAVA_HOME/bin:$PATH"

> "$INSTALL_DIR/logs/app.log"

cd "$INSTALL_DIR/backend"
nohup "$JAVA_HOME/bin/java" -jar \
    -Xms256m -Xmx512m \
    -Dspring.profiles.active=prod \
    -Dfile.encoding=UTF-8 \
    "$APP_NAME" > "$INSTALL_DIR/logs/app.log" 2>&1 &

echo "等待启动 (最多60秒)..."

for i in {1..60}; do
    sleep 1

    # 检查日志是否有错误
    if grep -q "Could not resolve placeholder" "$INSTALL_DIR/logs/app.log" 2>/dev/null; then
        echo ""
        echo -e "${RED}配置错误:${NC}"
        grep "Could not resolve placeholder" "$INSTALL_DIR/logs/app.log"
        exit 1
    fi

    # 检查是否启动成功
    if grep -q "Started StudentManagementApplication" "$INSTALL_DIR/logs/app.log" 2>/dev/null; then
        echo ""
        echo -e "${GREEN}============================================${NC}"
        echo -e "${GREEN}        启动成功！${NC}"
        echo -e "${GREEN}============================================${NC}"
        echo ""
        echo -e "访问地址: ${BLUE}http://${SERVER_IP}${NC}"
        echo -e "账号: ${YELLOW}admin${NC}"
        echo -e "密码: ${YELLOW}admin123${NC}"
        exit 0
    fi

    # 检查进程是否还在
    if ! ps -ef | grep -v grep | grep "$APP_NAME" > /dev/null; then
        echo ""
        echo -e "${RED}进程退出，错误日志:${NC}"
        tail -50 "$INSTALL_DIR/logs/app.log"
        exit 1
    fi

    echo -n "."
done

echo ""
echo -e "${YELLOW}启动超时，查看日志:${NC}"
tail -30 "$INSTALL_DIR/logs/app.log"
