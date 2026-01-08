#!/bin/bash

# ============================================
#    学生管理系统 - 部署诊断脚本
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
echo "   学生管理系统 - 部署诊断"
echo "============================================"
echo -e "${NC}"

ERRORS=0

# ============================================
# 1. 检查目录结构
# ============================================
echo -e "${BLUE}[1/6] 检查目录结构${NC}"

if [ -d "$INSTALL_DIR" ]; then
    echo -e "  安装目录: ${GREEN}存在${NC}"
else
    echo -e "  安装目录: ${RED}不存在${NC}"
    ((ERRORS++))
fi

if [ -d "$INSTALL_DIR/frontend" ]; then
    FILE_COUNT=$(ls -1 "$INSTALL_DIR/frontend" 2>/dev/null | wc -l)
    echo -e "  前端目录: ${GREEN}存在 ($FILE_COUNT 个文件/目录)${NC}"
else
    echo -e "  前端目录: ${RED}不存在${NC}"
    ((ERRORS++))
fi

if [ -f "$INSTALL_DIR/frontend/index.html" ]; then
    echo -e "  index.html: ${GREEN}存在${NC}"
else
    echo -e "  index.html: ${RED}不存在${NC}"
    ((ERRORS++))
fi

if [ -d "$INSTALL_DIR/frontend/static/js" ]; then
    JS_COUNT=$(ls -1 "$INSTALL_DIR/frontend/static/js"/*.js 2>/dev/null | wc -l)
    echo -e "  JS文件: ${GREEN}$JS_COUNT 个${NC}"
else
    echo -e "  JS目录: ${RED}不存在${NC}"
    ((ERRORS++))
fi

if [ -d "$INSTALL_DIR/frontend/static/css" ]; then
    CSS_COUNT=$(ls -1 "$INSTALL_DIR/frontend/static/css"/*.css 2>/dev/null | wc -l)
    echo -e "  CSS文件: ${GREEN}$CSS_COUNT 个${NC}"
else
    echo -e "  CSS目录: ${RED}不存在${NC}"
    ((ERRORS++))
fi

# ============================================
# 2. 检查后端
# ============================================
echo ""
echo -e "${BLUE}[2/6] 检查后端服务${NC}"

if [ -f "$INSTALL_DIR/backend/$APP_NAME" ]; then
    echo -e "  JAR文件: ${GREEN}存在${NC}"
else
    echo -e "  JAR文件: ${RED}不存在${NC}"
    ((ERRORS++))
fi

if ps -ef | grep -v grep | grep "$APP_NAME" > /dev/null; then
    PID=$(ps -ef | grep -v grep | grep "$APP_NAME" | awk '{print $2}')
    echo -e "  后端进程: ${GREEN}运行中 (PID: $PID)${NC}"
else
    echo -e "  后端进程: ${RED}未运行${NC}"
    ((ERRORS++))
fi

# 检查8080端口
if netstat -tlnp 2>/dev/null | grep ":8080" > /dev/null || ss -tlnp 2>/dev/null | grep ":8080" > /dev/null; then
    echo -e "  8080端口: ${GREEN}已监听${NC}"
else
    echo -e "  8080端口: ${RED}未监听${NC}"
    ((ERRORS++))
fi

# 测试后端接口
echo -n "  后端响应: "
RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" http://127.0.0.1:8080/api/auth/login --max-time 5 2>/dev/null)
if [ "$RESPONSE" = "405" ] || [ "$RESPONSE" = "200" ] || [ "$RESPONSE" = "401" ] || [ "$RESPONSE" = "400" ]; then
    echo -e "${GREEN}正常 (HTTP $RESPONSE)${NC}"
else
    echo -e "${RED}异常 (HTTP $RESPONSE)${NC}"
    ((ERRORS++))
fi

# ============================================
# 3. 检查Nginx
# ============================================
echo ""
echo -e "${BLUE}[3/6] 检查Nginx${NC}"

if command -v nginx &> /dev/null; then
    echo -e "  Nginx: ${GREEN}已安装${NC}"
else
    echo -e "  Nginx: ${RED}未安装${NC}"
    ((ERRORS++))
fi

if systemctl is-active --quiet nginx 2>/dev/null || ps -ef | grep -v grep | grep "nginx: master" > /dev/null; then
    echo -e "  Nginx状态: ${GREEN}运行中${NC}"
else
    echo -e "  Nginx状态: ${RED}未运行${NC}"
    ((ERRORS++))
fi

# 检查配置文件
NGINX_CONF="/www/server/panel/vhost/nginx/student-system.conf"
if [ -f "$NGINX_CONF" ]; then
    echo -e "  站点配置: ${GREEN}存在${NC}"

    # 检查配置内容
    if grep -q "root $INSTALL_DIR/frontend" "$NGINX_CONF"; then
        echo -e "  前端路径: ${GREEN}正确${NC}"
    else
        echo -e "  前端路径: ${YELLOW}可能不正确${NC}"
    fi

    if grep -q "proxy_pass.*8080" "$NGINX_CONF"; then
        echo -e "  API代理: ${GREEN}已配置${NC}"
    else
        echo -e "  API代理: ${RED}未配置${NC}"
        ((ERRORS++))
    fi
else
    echo -e "  站点配置: ${RED}不存在${NC}"
    ((ERRORS++))
fi

# 测试Nginx配置
echo -n "  配置语法: "
if nginx -t 2>&1 | grep -q "successful"; then
    echo -e "${GREEN}正确${NC}"
else
    echo -e "${RED}错误${NC}"
    ((ERRORS++))
fi

# ============================================
# 4. 检查数据库
# ============================================
echo ""
echo -e "${BLUE}[4/6] 检查MySQL${NC}"

if command -v mysql &> /dev/null; then
    echo -e "  MySQL: ${GREEN}已安装${NC}"
else
    echo -e "  MySQL: ${RED}未安装${NC}"
    ((ERRORS++))
fi

if systemctl is-active --quiet mysql 2>/dev/null || systemctl is-active --quiet mysqld 2>/dev/null || ps -ef | grep -v grep | grep mysqld > /dev/null; then
    echo -e "  MySQL状态: ${GREEN}运行中${NC}"
else
    echo -e "  MySQL状态: ${RED}未运行${NC}"
    ((ERRORS++))
fi

# ============================================
# 5. 检查端口
# ============================================
echo ""
echo -e "${BLUE}[5/6] 检查端口监听${NC}"

check_port() {
    local port=$1
    local name=$2
    if netstat -tlnp 2>/dev/null | grep ":$port" > /dev/null || ss -tlnp 2>/dev/null | grep ":$port" > /dev/null; then
        echo -e "  $port ($name): ${GREEN}已监听${NC}"
    else
        echo -e "  $port ($name): ${YELLOW}未监听${NC}"
    fi
}

check_port 80 "HTTP"
check_port 443 "HTTPS"
check_port 8080 "后端"
check_port 3306 "MySQL"
check_port 6379 "Redis"

# ============================================
# 6. 查看最近日志
# ============================================
echo ""
echo -e "${BLUE}[6/6] 最近错误日志${NC}"

if [ -f "$INSTALL_DIR/logs/app.log" ]; then
    echo -e "  后端日志最后10行:"
    echo -e "${YELLOW}"
    tail -10 "$INSTALL_DIR/logs/app.log" 2>/dev/null | sed 's/^/    /'
    echo -e "${NC}"
else
    echo -e "  ${YELLOW}后端日志文件不存在${NC}"
fi

# ============================================
# 诊断结果
# ============================================
echo ""
echo "============================================"
if [ $ERRORS -eq 0 ]; then
    echo -e "${GREEN}诊断完成: 未发现明显问题${NC}"
    echo ""
    echo "如果仍无法访问，请检查:"
    echo "  1. 云服务器安全组是否放行 80 端口"
    echo "  2. 防火墙是否放行 80 端口: firewall-cmd --list-ports"
    echo "  3. 浏览器缓存，尝试 Ctrl+F5 强制刷新"
else
    echo -e "${RED}诊断完成: 发现 $ERRORS 个问题${NC}"
    echo ""
    echo "建议操作:"

    if ! ps -ef | grep -v grep | grep "$APP_NAME" > /dev/null; then
        echo "  - 启动后端: $INSTALL_DIR/ctl.sh start"
    fi

    if [ ! -f "$INSTALL_DIR/frontend/index.html" ]; then
        echo "  - 重新部署前端文件"
    fi

    if [ ! -f "$NGINX_CONF" ]; then
        echo "  - 重新运行部署脚本或手动创建Nginx配置"
    fi
fi
echo "============================================"

# 提供快速修复选项
echo ""
read -p "是否尝试自动修复? (y/n): " FIX
if [ "$FIX" = "y" ]; then
    echo ""
    echo -e "${BLUE}尝试修复...${NC}"

    # 重启后端
    if ! ps -ef | grep -v grep | grep "$APP_NAME" > /dev/null; then
        echo "启动后端服务..."
        if [ -f "$INSTALL_DIR/ctl.sh" ]; then
            $INSTALL_DIR/ctl.sh start
        fi
    fi

    # 重载Nginx
    echo "重载Nginx配置..."
    nginx -s reload 2>/dev/null || systemctl reload nginx 2>/dev/null

    echo -e "${GREEN}修复完成，请重新访问网站${NC}"
fi
