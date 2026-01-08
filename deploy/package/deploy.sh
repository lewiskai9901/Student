#!/bin/bash

# ============================================
#    学生管理系统 - 宝塔一键部署脚本
#    前提：已安装 Nginx、MySQL、JDK17
# ============================================

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

INSTALL_DIR="/www/wwwroot/student-system"
APP_NAME="student-management-system-1.0.0.jar"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo -e "${BLUE}"
echo "============================================"
echo "   学生管理系统 - 一键部署"
echo "============================================"
echo -e "${NC}"

# 检查并配置Java
echo -e "${BLUE}[检查环境]${NC}"

# 尝试找到JDK 17
if ! java -version 2>&1 | grep -q "17"; then
    # 检查宝塔常见JDK路径
    JAVA_PATHS=(
        "/www/server/java/jdk-17.0.8"
        "/www/server/java/jdk-17"
        "/usr/local/jdk-17"
        "/usr/local/jdk-17.0.2"
    )

    for path in "${JAVA_PATHS[@]}"; do
        if [ -f "$path/bin/java" ]; then
            export JAVA_HOME="$path"
            export PATH="$JAVA_HOME/bin:$PATH"
            echo "export JAVA_HOME=$path" >> /etc/profile
            echo 'export PATH=$JAVA_HOME/bin:$PATH' >> /etc/profile
            source /etc/profile
            break
        fi
    done
fi

if ! java -version 2>&1 | grep -q "17"; then
    echo -e "${RED}未检测到 JDK 17${NC}"
    echo "请手动配置: export JAVA_HOME=/www/server/java/jdk-17.0.8"
    exit 1
fi
echo -e "${GREEN}JDK 17 ✓${NC}"

# 检查MySQL
if ! command -v mysql &> /dev/null; then
    echo -e "${RED}未检测到 MySQL，请先在宝塔安装${NC}"
    exit 1
fi
echo -e "${GREEN}MySQL ✓${NC}"

# 检查Nginx
if ! command -v nginx &> /dev/null; then
    echo -e "${RED}未检测到 Nginx，请先在宝塔安装${NC}"
    exit 1
fi
echo -e "${GREEN}Nginx ✓${NC}"

echo ""
echo -e "${YELLOW}请输入配置信息:${NC}"
read -p "MySQL root密码: " -s MYSQL_PWD
echo ""
read -p "域名或IP (回车自动获取): " DOMAIN
DOMAIN=${DOMAIN:-$(curl -s ifconfig.me 2>/dev/null || hostname -I | awk '{print $1}')}

echo ""
echo -e "域名/IP: ${GREEN}$DOMAIN${NC}"
read -p "确认开始部署? (y/n): " CONFIRM
[ "$CONFIRM" != "y" ] && exit 0

# ============================================
# 1. 创建目录
# ============================================
echo ""
echo -e "${BLUE}[1/5] 创建目录...${NC}"
mkdir -p $INSTALL_DIR/{backend,frontend,uploads,logs}
echo -e "${GREEN}完成${NC}"

# ============================================
# 2. 配置数据库
# ============================================
echo ""
echo -e "${BLUE}[2/5] 配置数据库...${NC}"
mysql -uroot -p"$MYSQL_PWD" -e "CREATE DATABASE IF NOT EXISTS student_management CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;" 2>/dev/null || {
    echo -e "${RED}数据库连接失败，请检查密码${NC}"
    exit 1
}

# 导入SQL（如果存在）
if [ -f "$SCRIPT_DIR/database/evaluation_schema.sql" ]; then
    mysql -uroot -p"$MYSQL_PWD" student_management < "$SCRIPT_DIR/database/evaluation_schema.sql" 2>/dev/null || true
fi
if [ -f "$SCRIPT_DIR/database/evaluation_init_data.sql" ]; then
    mysql -uroot -p"$MYSQL_PWD" student_management < "$SCRIPT_DIR/database/evaluation_init_data.sql" 2>/dev/null || true
fi
echo -e "${GREEN}完成${NC}"

# ============================================
# 3. 部署后端
# ============================================
echo ""
echo -e "${BLUE}[3/5] 部署后端...${NC}"

# 停止旧进程
pkill -f "$APP_NAME" 2>/dev/null || true
sleep 2

# 复制JAR
if [ -f "$SCRIPT_DIR/backend/$APP_NAME" ]; then
    cp "$SCRIPT_DIR/backend/$APP_NAME" $INSTALL_DIR/backend/
else
    echo -e "${RED}未找到 $APP_NAME${NC}"
    exit 1
fi

# 生成配置文件
cat > $INSTALL_DIR/backend/application-prod.yml << EOF
server:
  port: 8080
  servlet:
    context-path: /api

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/student_management?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
    username: root
    password: $MYSQL_PWD
    driver-class-name: com.mysql.cj.jdbc.Driver
  redis:
    host: localhost
    port: 6379

jwt:
  secret: $(cat /dev/urandom | tr -dc 'a-zA-Z0-9' | fold -w 64 | head -n 1)
  access-token-expiration: 7200000
  refresh-token-expiration: 2592000000

file:
  upload-path: $INSTALL_DIR/uploads/

logging:
  file:
    path: $INSTALL_DIR/logs
EOF

# 启动后端
cd $INSTALL_DIR/backend
nohup java -jar -Xms256m -Xmx512m -Dspring.profiles.active=prod -Dfile.encoding=UTF-8 $APP_NAME > $INSTALL_DIR/logs/app.log 2>&1 &
sleep 5

if ps -ef | grep -v grep | grep "$APP_NAME" > /dev/null; then
    echo -e "${GREEN}后端启动成功${NC}"
else
    echo -e "${RED}后端启动失败，查看日志: tail -f $INSTALL_DIR/logs/app.log${NC}"
fi

# ============================================
# 4. 部署前端
# ============================================
echo ""
echo -e "${BLUE}[4/5] 部署前端...${NC}"
rm -rf $INSTALL_DIR/frontend/*
cp -r "$SCRIPT_DIR/frontend/"* $INSTALL_DIR/frontend/
echo -e "${GREEN}完成${NC}"

# ============================================
# 5. 配置Nginx
# ============================================
echo ""
echo -e "${BLUE}[5/5] 配置Nginx...${NC}"

cat > /www/server/panel/vhost/nginx/student-system.conf << EOF
server {
    listen 80;
    server_name $DOMAIN;
    root $INSTALL_DIR/frontend;
    index index.html;

    location / {
        try_files \$uri \$uri/ /index.html;
    }

    location /api {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_connect_timeout 60;
        proxy_read_timeout 120;
        client_max_body_size 50m;
    }

    location /uploads {
        alias $INSTALL_DIR/uploads;
    }
}
EOF

nginx -t && nginx -s reload
echo -e "${GREEN}完成${NC}"

# ============================================
# 创建管理脚本
# ============================================
cat > $INSTALL_DIR/ctl.sh << 'EOF'
#!/bin/bash
DIR="/www/wwwroot/student-system"
JAR="student-management-system-1.0.0.jar"
case "$1" in
    start)
        cd $DIR/backend
        nohup java -jar -Xms256m -Xmx512m -Dspring.profiles.active=prod -Dfile.encoding=UTF-8 $JAR > $DIR/logs/app.log 2>&1 &
        echo "已启动";;
    stop)
        pkill -f "$JAR" && echo "已停止" || echo "未运行";;
    restart)
        $0 stop; sleep 2; $0 start;;
    log)
        tail -f $DIR/logs/app.log;;
    *)
        echo "用法: $0 {start|stop|restart|log}";;
esac
EOF
chmod +x $INSTALL_DIR/ctl.sh

# ============================================
# 完成
# ============================================
echo ""
echo -e "${GREEN}============================================${NC}"
echo -e "${GREEN}        部署完成！${NC}"
echo -e "${GREEN}============================================${NC}"
echo ""
echo -e "访问地址: ${BLUE}http://$DOMAIN${NC}"
echo -e "账号: ${YELLOW}admin${NC}"
echo -e "密码: ${YELLOW}admin123${NC}"
echo ""
echo -e "管理命令:"
echo -e "  ${BLUE}$INSTALL_DIR/ctl.sh start${NC}   - 启动"
echo -e "  ${BLUE}$INSTALL_DIR/ctl.sh stop${NC}    - 停止"
echo -e "  ${BLUE}$INSTALL_DIR/ctl.sh restart${NC} - 重启"
echo -e "  ${BLUE}$INSTALL_DIR/ctl.sh log${NC}     - 查看日志"
echo ""
echo -e "${YELLOW}注意: 确保腾讯云安全组已放行 80 端口${NC}"
