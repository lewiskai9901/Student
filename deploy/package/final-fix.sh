#!/bin/bash

# ============================================
#    最终修复脚本 - 解决所有配置问题
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
echo "   最终修复 - 解决所有问题"
echo "============================================"
echo -e "${NC}"

# 获取服务器IP
SERVER_IP=$(curl -s ifconfig.me --max-time 5 2>/dev/null || hostname -I | awk '{print $1}')

# 1. 停止所有相关进程
echo -e "${BLUE}[1/5] 停止相关进程${NC}"
echo "停止后端进程..."
pkill -f "$APP_NAME" 2>/dev/null || true

echo "停止占用8080端口的进程 (jsvc/Tomcat)..."
fuser -k 8080/tcp 2>/dev/null || true
pkill -f jsvc 2>/dev/null || true
sleep 3

# 确认8080已释放
if ss -tlnp | grep ":8080" > /dev/null; then
    echo -e "${YELLOW}8080端口仍被占用，强制关闭...${NC}"
    PID=$(ss -tlnp | grep ":8080" | grep -oP 'pid=\K\d+')
    if [ -n "$PID" ]; then
        kill -9 $PID 2>/dev/null || true
    fi
    sleep 2
fi
echo -e "${GREEN}完成${NC}"

# 2. 获取配置信息
echo ""
echo -e "${BLUE}[2/5] 配置信息${NC}"
read -p "MySQL密码 [默认: 123456]: " MYSQL_PWD
MYSQL_PWD=${MYSQL_PWD:-123456}

read -p "Redis密码 [无密码直接回车]: " REDIS_PWD

# 生成JWT密钥
JWT_KEY=$(cat /dev/urandom 2>/dev/null | tr -dc 'a-zA-Z0-9' | fold -w 80 | head -n 1)
if [ -z "$JWT_KEY" ] || [ ${#JWT_KEY} -lt 64 ]; then
    JWT_KEY="student-management-system-jwt-secret-key-2024-base64-encoded-long-enough-for-hs512-algorithm-requirement"
fi

# 3. 生成完整配置文件
echo ""
echo -e "${BLUE}[3/5] 生成配置文件${NC}"

cat > "$INSTALL_DIR/backend/application-prod.yml" << EOF
# ======================================
# 学生管理系统 - 生产环境配置
# ======================================

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

  data:
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
  url-prefix: http://${SERVER_IP}/uploads

logging:
  file:
    path: ${INSTALL_DIR}/logs
  level:
    root: INFO
    com.school: DEBUG

mybatis-plus:
  mapper-locations: classpath*:/mapper/**/*.xml
  type-aliases-package: com.school.management.entity
  configuration:
    map-underscore-to-camel-case: true
    cache-enabled: false
EOF

echo -e "${GREEN}配置文件已生成${NC}"

# 4. 更新启动脚本
echo ""
echo -e "${BLUE}[4/5] 更新管理脚本${NC}"

cat > "$INSTALL_DIR/ctl.sh" << 'CTLEOF'
#!/bin/bash
export JAVA_HOME="/www/server/java/jdk-17.0.8"
export PATH="$JAVA_HOME/bin:$PATH"

DIR="/www/wwwroot/student-system"
JAR="student-management-system-1.0.0.jar"

case "$1" in
    start)
        # 检查是否已运行
        if ps -ef | grep -v grep | grep "$JAR" > /dev/null; then
            echo "已在运行"
            exit 0
        fi
        # 检查8080端口
        if ss -tlnp | grep ":8080" > /dev/null; then
            echo "8080端口被占用，尝试释放..."
            fuser -k 8080/tcp 2>/dev/null || true
            sleep 2
        fi
        cd $DIR/backend
        nohup $JAVA_HOME/bin/java -jar -Xms256m -Xmx512m \
            -Dspring.profiles.active=prod \
            -Dfile.encoding=UTF-8 \
            $JAR > $DIR/logs/app.log 2>&1 &
        echo "已启动 (PID: $!)"
        ;;
    stop)
        pkill -f "$JAR" && echo "已停止" || echo "未运行"
        ;;
    restart)
        $0 stop
        sleep 2
        $0 start
        ;;
    status)
        if ps -ef | grep -v grep | grep "$JAR" > /dev/null; then
            echo "运行中"
            ps -ef | grep -v grep | grep "$JAR"
        else
            echo "未运行"
        fi
        ;;
    log)
        tail -f $DIR/logs/app.log
        ;;
    *)
        echo "用法: $0 {start|stop|restart|status|log}"
        ;;
esac
CTLEOF

chmod +x "$INSTALL_DIR/ctl.sh"
echo -e "${GREEN}完成${NC}"

# 5. 启动后端
echo ""
echo -e "${BLUE}[5/5] 启动后端服务${NC}"

export JAVA_HOME="$JAVA17_HOME"
export PATH="$JAVA_HOME/bin:$PATH"

# 清空旧日志
mkdir -p "$INSTALL_DIR/logs"
> "$INSTALL_DIR/logs/app.log"

cd "$INSTALL_DIR/backend"
nohup "$JAVA17_HOME/bin/java" -jar \
    -Xms256m -Xmx512m \
    -Dspring.profiles.active=prod \
    -Dfile.encoding=UTF-8 \
    "$APP_NAME" > "$INSTALL_DIR/logs/app.log" 2>&1 &

BACKEND_PID=$!
echo "后端 PID: $BACKEND_PID"
echo "等待启动..."

# 等待启动
SUCCESS=false
for i in {1..30}; do
    sleep 1
    echo -n "."

    # 检查进程是否还在
    if ! ps -p $BACKEND_PID > /dev/null 2>&1; then
        echo ""
        echo -e "${RED}进程已退出，查看错误:${NC}"
        tail -50 "$INSTALL_DIR/logs/app.log"
        exit 1
    fi

    # 测试接口
    RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" http://127.0.0.1:8080/api/auth/login --max-time 2 2>/dev/null)
    if [ "$RESPONSE" = "405" ] || [ "$RESPONSE" = "400" ] || [ "$RESPONSE" = "401" ] || [ "$RESPONSE" = "200" ]; then
        SUCCESS=true
        break
    fi
done

echo ""

if [ "$SUCCESS" = true ]; then
    echo ""
    echo -e "${GREEN}============================================${NC}"
    echo -e "${GREEN}        启动成功！${NC}"
    echo -e "${GREEN}============================================${NC}"
    echo ""
    echo -e "访问地址: ${BLUE}http://${SERVER_IP}${NC}"
    echo -e "账号: ${YELLOW}admin${NC}"
    echo -e "密码: ${YELLOW}admin123${NC}"
    echo ""
    echo "管理命令:"
    echo "  $INSTALL_DIR/ctl.sh start   - 启动"
    echo "  $INSTALL_DIR/ctl.sh stop    - 停止"
    echo "  $INSTALL_DIR/ctl.sh restart - 重启"
    echo "  $INSTALL_DIR/ctl.sh log     - 查看日志"
else
    echo -e "${RED}启动超时，查看日志:${NC}"
    tail -50 "$INSTALL_DIR/logs/app.log"
    echo ""
    echo "可能的问题:"
    echo "1. 数据库连接失败 - 检查MySQL是否运行"
    echo "2. 配置错误 - 检查日志中的具体错误"
    echo "3. 端口冲突 - 运行: ss -tlnp | grep 8080"
fi
