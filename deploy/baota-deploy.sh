#!/bin/bash

# ============================================
#    学生管理系统 - 宝塔面板一键部署脚本
#    适用于: 腾讯云 + 宝塔面板
# ============================================

set -e

# 颜色
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# 配置
INSTALL_DIR="/www/wwwroot/student-system"
APP_NAME="student-management-system-1.0.0.jar"

echo -e "${BLUE}"
echo "============================================"
echo "   学生管理系统 - 宝塔一键部署"
echo "============================================"
echo -e "${NC}"

# 检查root
if [ "$EUID" -ne 0 ]; then
    echo -e "${RED}请使用 root 用户运行${NC}"
    exit 1
fi

# 获取脚本目录
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

# ============================================
# 收集信息
# ============================================
echo -e "${YELLOW}请输入配置信息:${NC}"
read -p "MySQL root密码: " -s MYSQL_PWD
echo ""
read -p "域名或IP (回车自动获取): " DOMAIN
DOMAIN=${DOMAIN:-$(curl -s ifconfig.me 2>/dev/null || hostname -I | awk '{print $1}')}

echo ""
echo -e "${GREEN}配置确认:${NC}"
echo "  安装目录: $INSTALL_DIR"
echo "  访问地址: http://$DOMAIN"
read -p "继续? (y/n): " CONFIRM
[ "$CONFIRM" != "y" ] && exit 0

# ============================================
# 1. 检查/安装 JDK 17
# ============================================
echo ""
echo -e "${BLUE}[1/6] 检查 Java 环境...${NC}"

install_jdk() {
    echo "安装 JDK 17..."
    cd /usr/local
    if [ ! -f "jdk-17_linux-x64_bin.tar.gz" ]; then
        wget -q --show-progress https://download.oracle.com/java/17/latest/jdk-17_linux-x64_bin.tar.gz || {
            echo "下载失败，尝试备用地址..."
            wget -q --show-progress https://mirrors.huaweicloud.com/java/jdk/17.0.2+8/jdk-17.0.2_linux-x64_bin.tar.gz -O jdk-17_linux-x64_bin.tar.gz
        }
    fi
    tar -zxf jdk-17_linux-x64_bin.tar.gz
    JDK_DIR=$(ls -d jdk-17* 2>/dev/null | grep -v tar | head -1)

    grep -q "JAVA_HOME" /etc/profile || {
        echo "export JAVA_HOME=/usr/local/$JDK_DIR" >> /etc/profile
        echo 'export PATH=$JAVA_HOME/bin:$PATH' >> /etc/profile
    }
    source /etc/profile
    export JAVA_HOME=/usr/local/$JDK_DIR
    export PATH=$JAVA_HOME/bin:$PATH
}

if ! java -version 2>&1 | grep -q "17"; then
    install_jdk
fi
echo -e "${GREEN}Java 17 已就绪${NC}"

# ============================================
# 2. 创建目录
# ============================================
echo ""
echo -e "${BLUE}[2/6] 创建目录结构...${NC}"
mkdir -p $INSTALL_DIR/{backend,frontend,uploads,logs}
echo -e "${GREEN}完成${NC}"

# ============================================
# 3. 配置数据库
# ============================================
echo ""
echo -e "${BLUE}[3/6] 配置数据库...${NC}"

mysql -uroot -p"$MYSQL_PWD" -e "CREATE DATABASE IF NOT EXISTS student_management CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;" 2>/dev/null

if [ -f "$SCRIPT_DIR/database/complete_schema.sql" ]; then
    mysql -uroot -p"$MYSQL_PWD" student_management < "$SCRIPT_DIR/database/complete_schema.sql"
    echo "数据库结构已导入"
fi
if [ -f "$SCRIPT_DIR/database/init_data.sql" ]; then
    mysql -uroot -p"$MYSQL_PWD" student_management < "$SCRIPT_DIR/database/init_data.sql"
    echo "初始数据已导入"
fi
echo -e "${GREEN}完成${NC}"

# ============================================
# 4. 部署后端
# ============================================
echo ""
echo -e "${BLUE}[4/6] 部署后端...${NC}"

# 停止旧进程
pkill -f "$APP_NAME" 2>/dev/null || true
sleep 2

# 复制文件
cp "$SCRIPT_DIR/backend/$APP_NAME" $INSTALL_DIR/backend/ 2>/dev/null || {
    echo -e "${RED}未找到 $APP_NAME，请确保文件存在${NC}"
    exit 1
}

# 生成配置
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

# 启动
cd $INSTALL_DIR/backend
source /etc/profile
nohup java -jar -Xms256m -Xmx512m -Dspring.profiles.active=prod -Dfile.encoding=UTF-8 $APP_NAME > $INSTALL_DIR/logs/app.log 2>&1 &
sleep 5

ps -ef | grep -v grep | grep "$APP_NAME" > /dev/null && echo -e "${GREEN}后端启动成功${NC}" || echo -e "${RED}启动失败，查看日志${NC}"

# ============================================
# 5. 部署前端
# ============================================
echo ""
echo -e "${BLUE}[5/6] 部署前端...${NC}"
rm -rf $INSTALL_DIR/frontend/*
cp -r "$SCRIPT_DIR/frontend/"* $INSTALL_DIR/frontend/ 2>/dev/null || {
    echo -e "${RED}未找到前端文件${NC}"
    exit 1
}
echo -e "${GREEN}完成${NC}"

# ============================================
# 6. 配置 Nginx
# ============================================
echo ""
echo -e "${BLUE}[6/6] 配置 Nginx...${NC}"

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
cat > $INSTALL_DIR/ctl.sh << 'CTLEOF'
#!/bin/bash
DIR="/www/wwwroot/student-system"
JAR="student-management-system-1.0.0.jar"
case "$1" in
    start)
        cd $DIR/backend && source /etc/profile
        nohup java -jar -Xms256m -Xmx512m -Dspring.profiles.active=prod $JAR > $DIR/logs/app.log 2>&1 &
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
CTLEOF
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
echo -e "账号: ${YELLOW}admin${NC}  密码: ${YELLOW}admin123${NC}"
echo ""
echo -e "管理命令:"
echo -e "  $INSTALL_DIR/ctl.sh start   # 启动"
echo -e "  $INSTALL_DIR/ctl.sh stop    # 停止"
echo -e "  $INSTALL_DIR/ctl.sh restart # 重启"
echo -e "  $INSTALL_DIR/ctl.sh log     # 日志"
echo ""
echo -e "${YELLOW}记得在腾讯云安全组放行 80 端口！${NC}"
