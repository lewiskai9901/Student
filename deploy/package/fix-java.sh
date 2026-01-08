#!/bin/bash

# ============================================
#    修复 Java 版本问题
# ============================================

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

INSTALL_DIR="/www/wwwroot/student-system"
APP_NAME="student-management-system-1.0.0.jar"

echo -e "${BLUE}"
echo "============================================"
echo "   修复 Java 版本问题"
echo "============================================"
echo -e "${NC}"

# 检查当前 Java 版本
echo -e "${BLUE}[1/4] 当前 Java 版本${NC}"
java -version 2>&1
echo ""

# 查找 JDK 17
echo -e "${BLUE}[2/4] 查找 JDK 17${NC}"

JAVA17_PATHS=(
    "/www/server/java/jdk-17.0.8"
    "/www/server/java/jdk-17.0.9"
    "/www/server/java/jdk-17.0.10"
    "/www/server/java/jdk-17.0.11"
    "/www/server/java/jdk-17.0.12"
    "/www/server/java/jdk-17"
    "/usr/local/jdk-17"
    "/usr/lib/jvm/java-17-openjdk"
    "/usr/lib/jvm/java-17-openjdk-amd64"
)

JAVA17_HOME=""
for path in "${JAVA17_PATHS[@]}"; do
    if [ -f "$path/bin/java" ]; then
        VERSION=$("$path/bin/java" -version 2>&1 | head -1)
        if echo "$VERSION" | grep -q "17"; then
            JAVA17_HOME="$path"
            echo -e "${GREEN}找到 JDK 17: $path${NC}"
            break
        fi
    fi
done

# 列出宝塔 Java 目录
echo ""
echo "宝塔 Java 目录:"
ls -la /www/server/java/ 2>/dev/null || echo "目录不存在"
echo ""

if [ -z "$JAVA17_HOME" ]; then
    echo -e "${RED}未找到 JDK 17！${NC}"
    echo ""
    echo "请在宝塔面板安装 JDK 17："
    echo "  1. 打开宝塔面板"
    echo "  2. 软件商店 → 搜索 'Java项目管理器'"
    echo "  3. 安装后，在设置中安装 JDK 17"
    echo ""
    echo "或手动安装："
    echo "  cd /www/server/java"
    echo "  wget https://download.oracle.com/java/17/latest/jdk-17_linux-x64_bin.tar.gz"
    echo "  tar -xzf jdk-17_linux-x64_bin.tar.gz"
    echo ""
    exit 1
fi

# 停止旧进程
echo -e "${BLUE}[3/4] 停止旧进程${NC}"
pkill -f "$APP_NAME" 2>/dev/null || true
fuser -k 8080/tcp 2>/dev/null || true
sleep 2
echo -e "${GREEN}完成${NC}"

# 使用 JDK 17 启动
echo ""
echo -e "${BLUE}[4/4] 使用 JDK 17 启动后端${NC}"

export JAVA_HOME="$JAVA17_HOME"
export PATH="$JAVA_HOME/bin:$PATH"

echo "JAVA_HOME: $JAVA_HOME"
echo "Java 版本:"
java -version 2>&1
echo ""

# 更新启动脚本
cat > "$INSTALL_DIR/ctl.sh" << EOF
#!/bin/bash
export JAVA_HOME="$JAVA17_HOME"
export PATH="\$JAVA_HOME/bin:\$PATH"

DIR="/www/wwwroot/student-system"
JAR="student-management-system-1.0.0.jar"

case "\$1" in
    start)
        cd \$DIR/backend
        nohup \$JAVA_HOME/bin/java -jar -Xms256m -Xmx512m -Dspring.profiles.active=prod -Dfile.encoding=UTF-8 \$JAR > \$DIR/logs/app.log 2>&1 &
        echo "已启动 (使用 JDK 17)";;
    stop)
        pkill -f "\$JAR" && echo "已停止" || echo "未运行";;
    restart)
        \$0 stop; sleep 2; \$0 start;;
    log)
        tail -f \$DIR/logs/app.log;;
    *)
        echo "用法: \$0 {start|stop|restart|log}";;
esac
EOF
chmod +x "$INSTALL_DIR/ctl.sh"

# 清空日志
> "$INSTALL_DIR/logs/app.log"

# 启动
cd "$INSTALL_DIR/backend"
nohup "$JAVA17_HOME/bin/java" -jar -Xms256m -Xmx512m -Dspring.profiles.active=prod -Dfile.encoding=UTF-8 "$APP_NAME" > "$INSTALL_DIR/logs/app.log" 2>&1 &

echo "等待启动（15秒）..."
sleep 15

# 检查结果
if ps -ef | grep -v grep | grep "$APP_NAME" > /dev/null; then
    echo ""
    echo -e "${GREEN}============================================${NC}"
    echo -e "${GREEN}   后端启动成功！${NC}"
    echo -e "${GREEN}============================================${NC}"
    echo ""

    # 测试接口
    RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" http://127.0.0.1:8080/api/auth/login --max-time 5 2>/dev/null)
    echo "接口测试: HTTP $RESPONSE"

    if [ "$RESPONSE" = "405" ] || [ "$RESPONSE" = "400" ] || [ "$RESPONSE" = "401" ]; then
        echo -e "${GREEN}接口响应正常！${NC}"
        echo ""
        echo "现在可以访问网站了！"
    fi
else
    echo ""
    echo -e "${RED}启动失败，查看日志:${NC}"
    tail -30 "$INSTALL_DIR/logs/app.log"
fi
