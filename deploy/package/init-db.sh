#!/bin/bash

# ============================================
#    初始化完整数据库
# ============================================

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
INSTALL_DIR="/www/wwwroot/student-system"

echo -e "${BLUE}"
echo "============================================"
echo "   初始化完整数据库"
echo "============================================"
echo -e "${NC}"

read -p "MySQL密码 [默认: 123456]: " MYSQL_PWD
MYSQL_PWD=${MYSQL_PWD:-123456}

echo ""
echo -e "${BLUE}[1/5] 创建数据库${NC}"
mysql -uroot -p"$MYSQL_PWD" -e "CREATE DATABASE IF NOT EXISTS student_management CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;" 2>/dev/null
echo -e "${GREEN}完成${NC}"

echo ""
echo -e "${BLUE}[2/5] 导入核心表结构${NC}"
if [ -f "$SCRIPT_DIR/database/core_schema.sql" ]; then
    mysql -uroot -p"$MYSQL_PWD" student_management < "$SCRIPT_DIR/database/core_schema.sql" 2>&1
    echo -e "${GREEN}完成${NC}"
else
    echo -e "${RED}core_schema.sql 不存在${NC}"
fi

echo ""
echo -e "${BLUE}[3/5] 导入核心初始数据${NC}"
if [ -f "$SCRIPT_DIR/database/core_init_data.sql" ]; then
    mysql -uroot -p"$MYSQL_PWD" student_management < "$SCRIPT_DIR/database/core_init_data.sql" 2>&1
    echo -e "${GREEN}完成${NC}"
else
    echo -e "${RED}core_init_data.sql 不存在${NC}"
fi

echo ""
echo -e "${BLUE}[4/5] 导入评价系统表${NC}"
if [ -f "$SCRIPT_DIR/database/evaluation_schema.sql" ]; then
    mysql -uroot -p"$MYSQL_PWD" student_management < "$SCRIPT_DIR/database/evaluation_schema.sql" 2>&1
    echo -e "${GREEN}完成${NC}"
fi
if [ -f "$SCRIPT_DIR/database/evaluation_init_data.sql" ]; then
    mysql -uroot -p"$MYSQL_PWD" student_management < "$SCRIPT_DIR/database/evaluation_init_data.sql" 2>&1
    echo -e "${GREEN}完成${NC}"
fi

echo ""
echo -e "${BLUE}[5/5] 导入其他表${NC}"
# 系统配置表
if [ -f "$SCRIPT_DIR/../database/system_config_schema.sql" ]; then
    mysql -uroot -p"$MYSQL_PWD" student_management < "$SCRIPT_DIR/../database/system_config_schema.sql" 2>&1
fi
# 公告表
if [ -f "$SCRIPT_DIR/../database/announcements_schema.sql" ]; then
    mysql -uroot -p"$MYSQL_PWD" student_management < "$SCRIPT_DIR/../database/announcements_schema.sql" 2>&1
fi
# 操作日志
if [ -f "$SCRIPT_DIR/../database/operation_logs_schema.sql" ]; then
    mysql -uroot -p"$MYSQL_PWD" student_management < "$SCRIPT_DIR/../database/operation_logs_schema.sql" 2>&1
fi
echo -e "${GREEN}完成${NC}"

echo ""
echo -e "${BLUE}检查表数量:${NC}"
TABLE_COUNT=$(mysql -uroot -p"$MYSQL_PWD" -N -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='student_management';" 2>/dev/null)
echo "表数量: $TABLE_COUNT"

echo ""
echo -e "${BLUE}检查关键表:${NC}"
mysql -uroot -p"$MYSQL_PWD" -e "SELECT 'users' as tbl, COUNT(*) as cnt FROM student_management.users UNION ALL SELECT 'roles', COUNT(*) FROM student_management.roles UNION ALL SELECT 'system_configs', COUNT(*) FROM student_management.system_configs;" 2>/dev/null

echo ""
read -p "是否重启后端? (y/n): " RESTART
if [ "$RESTART" = "y" ]; then
    pkill -f "student-management-system" 2>/dev/null || true
    sleep 2

    cd "$INSTALL_DIR/backend"
    export JAVA_HOME="/www/server/java/jdk-17.0.8"
    nohup $JAVA_HOME/bin/java -jar -Xms256m -Xmx512m \
        -Dspring.profiles.active=prod \
        -Dfile.encoding=UTF-8 \
        student-management-system-1.0.0.jar > "$INSTALL_DIR/logs/app.log" 2>&1 &

    echo "等待启动..."
    sleep 15

    RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" http://127.0.0.1:8080/api/system/configs/public/system --max-time 5 2>/dev/null)
    if [ "$RESPONSE" = "200" ]; then
        echo -e "${GREEN}============================================${NC}"
        echo -e "${GREEN}   启动成功！${NC}"
        echo -e "${GREEN}============================================${NC}"
        echo ""
        echo "访问地址: http://$(curl -s ifconfig.me)"
        echo "账号: admin"
        echo "密码: admin123"
    else
        echo -e "${YELLOW}接口返回: HTTP $RESPONSE${NC}"
        echo "查看日志: tail -50 $INSTALL_DIR/logs/app.log"
    fi
fi

echo ""
echo "============================================"
echo "数据库初始化完成"
echo "============================================"
