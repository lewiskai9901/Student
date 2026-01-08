#!/bin/bash

# ============================================
#    诊断500错误并修复数据库
# ============================================

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

INSTALL_DIR="/www/wwwroot/student-system"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo -e "${BLUE}"
echo "============================================"
echo "   诊断500错误并修复数据库"
echo "============================================"
echo -e "${NC}"

# 1. 查看后端错误日志
echo -e "${BLUE}[1/4] 后端错误日志${NC}"
echo ""
if [ -f "$INSTALL_DIR/logs/app.log" ]; then
    echo "最近的错误信息:"
    grep -i "error\|exception" "$INSTALL_DIR/logs/app.log" | tail -30
    echo ""
    echo "--- 最后50行日志 ---"
    tail -50 "$INSTALL_DIR/logs/app.log"
else
    echo "日志文件不存在"
fi
echo ""

# 2. 检查数据库
echo -e "${BLUE}[2/4] 检查数据库${NC}"
read -p "MySQL密码 [默认: 123456]: " MYSQL_PWD
MYSQL_PWD=${MYSQL_PWD:-123456}

echo ""
echo "数据库列表:"
mysql -uroot -p"$MYSQL_PWD" -e "SHOW DATABASES;" 2>/dev/null || {
    echo -e "${RED}数据库连接失败${NC}"
    exit 1
}

echo ""
echo "student_management 表列表:"
TABLES=$(mysql -uroot -p"$MYSQL_PWD" -e "USE student_management; SHOW TABLES;" 2>/dev/null)
if [ -z "$TABLES" ]; then
    echo -e "${RED}数据库为空或不存在${NC}"
else
    echo "$TABLES"
fi

echo ""
TABLE_COUNT=$(mysql -uroot -p"$MYSQL_PWD" -N -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='student_management';" 2>/dev/null)
echo "表数量: $TABLE_COUNT"

# 3. 检查关键表
echo ""
echo -e "${BLUE}[3/4] 检查关键数据${NC}"

echo "系统配置表:"
mysql -uroot -p"$MYSQL_PWD" -e "SELECT * FROM student_management.system_config LIMIT 5;" 2>/dev/null || echo "表不存在"

echo ""
echo "用户表:"
mysql -uroot -p"$MYSQL_PWD" -e "SELECT id, username, real_name FROM student_management.sys_user LIMIT 5;" 2>/dev/null || echo "表不存在"

# 4. 修复数据库
echo ""
echo -e "${BLUE}[4/4] 修复数据库${NC}"
read -p "是否导入/重新导入数据库? (y/n): " FIX

if [ "$FIX" = "y" ]; then
    echo ""

    # 创建数据库
    echo "创建数据库..."
    mysql -uroot -p"$MYSQL_PWD" -e "CREATE DATABASE IF NOT EXISTS student_management CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;" 2>/dev/null

    # 查找SQL文件
    SQL_DIR="$SCRIPT_DIR/database"
    if [ ! -d "$SQL_DIR" ]; then
        SQL_DIR="$INSTALL_DIR/database"
    fi

    echo "SQL目录: $SQL_DIR"
    ls -la "$SQL_DIR" 2>/dev/null || echo "目录不存在"

    # 导入schema
    if [ -f "$SQL_DIR/evaluation_schema.sql" ]; then
        echo "导入 evaluation_schema.sql..."
        mysql -uroot -p"$MYSQL_PWD" student_management < "$SQL_DIR/evaluation_schema.sql" 2>&1
        echo -e "${GREEN}完成${NC}"
    else
        echo -e "${YELLOW}evaluation_schema.sql 不存在${NC}"
    fi

    # 导入初始数据
    if [ -f "$SQL_DIR/evaluation_init_data.sql" ]; then
        echo "导入 evaluation_init_data.sql..."
        mysql -uroot -p"$MYSQL_PWD" student_management < "$SQL_DIR/evaluation_init_data.sql" 2>&1
        echo -e "${GREEN}完成${NC}"
    else
        echo -e "${YELLOW}evaluation_init_data.sql 不存在${NC}"
    fi

    # 检查是否还有其他SQL文件
    echo ""
    echo "查找其他SQL文件..."
    find "$SCRIPT_DIR" -name "*.sql" -type f 2>/dev/null

    echo ""
    echo "导入完成，重新检查表数量:"
    mysql -uroot -p"$MYSQL_PWD" -e "SELECT COUNT(*) as '表数量' FROM information_schema.tables WHERE table_schema='student_management';" 2>/dev/null

    # 重启后端
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

        echo "后端已重启，等待10秒..."
        sleep 10

        # 测试
        RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" http://127.0.0.1:8080/api/system/configs/public/system --max-time 5 2>/dev/null)
        echo "接口测试: HTTP $RESPONSE"

        if [ "$RESPONSE" = "200" ]; then
            echo -e "${GREEN}修复成功！${NC}"
        else
            echo -e "${YELLOW}接口返回 $RESPONSE，查看日志:${NC}"
            tail -20 "$INSTALL_DIR/logs/app.log"
        fi
    fi
fi

echo ""
echo "============================================"
echo "诊断完成"
echo "============================================"
