#!/bin/bash

# ============================================
#    完整诊断脚本
# ============================================

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

INSTALL_DIR="/www/wwwroot/student-system"

echo -e "${BLUE}"
echo "============================================"
echo "   完整诊断"
echo "============================================"
echo -e "${NC}"

# 1. 后端进程
echo -e "${BLUE}[1] 后端进程${NC}"
ps -ef | grep -v grep | grep student-management || echo -e "${RED}未运行${NC}"
echo ""

# 2. 端口监听
echo -e "${BLUE}[2] 端口监听${NC}"
ss -tlnp | grep -E ":80|:8080" || netstat -tlnp | grep -E ":80|:8080"
echo ""

# 3. 测试后端
echo -e "${BLUE}[3] 测试后端接口${NC}"
BACKEND=$(curl -s -o /dev/null -w "%{http_code}" http://127.0.0.1:8080/api/auth/login --max-time 5 2>/dev/null)
echo "后端响应: HTTP $BACKEND"
if [ "$BACKEND" = "000" ]; then
    echo -e "${RED}后端未响应，查看日志:${NC}"
    tail -20 "$INSTALL_DIR/logs/app.log" 2>/dev/null
fi
echo ""

# 4. 测试前端（通过Nginx）
echo -e "${BLUE}[4] 测试 Nginx 前端${NC}"
FRONTEND=$(curl -s -o /dev/null -w "%{http_code}" http://127.0.0.1/ --max-time 5 2>/dev/null)
echo "前端响应: HTTP $FRONTEND"
if [ "$FRONTEND" = "000" ] || [ "$FRONTEND" = "502" ] || [ "$FRONTEND" = "404" ]; then
    echo -e "${RED}前端异常${NC}"
fi
echo ""

# 5. 检查前端文件
echo -e "${BLUE}[5] 前端文件${NC}"
if [ -f "$INSTALL_DIR/frontend/index.html" ]; then
    echo -e "${GREEN}index.html 存在${NC}"
    echo "内容预览:"
    head -15 "$INSTALL_DIR/frontend/index.html"
else
    echo -e "${RED}index.html 不存在!${NC}"
fi
echo ""

# 6. Nginx 配置
echo -e "${BLUE}[6] Nginx 站点配置${NC}"
NGINX_CONF="/www/server/panel/vhost/nginx/student-system.conf"
if [ -f "$NGINX_CONF" ]; then
    echo -e "${GREEN}配置存在${NC}"
    cat "$NGINX_CONF"
else
    echo -e "${RED}配置不存在!${NC}"
    echo "查找其他配置..."
    ls -la /www/server/panel/vhost/nginx/
fi
echo ""

# 7. Nginx 错误日志
echo -e "${BLUE}[7] Nginx 错误日志${NC}"
if [ -f "/www/wwwlogs/student-system.error.log" ]; then
    tail -15 /www/wwwlogs/student-system.error.log
elif [ -f "/var/log/nginx/error.log" ]; then
    tail -15 /var/log/nginx/error.log
else
    echo "未找到错误日志"
fi
echo ""

# 8. 后端日志最后几行
echo -e "${BLUE}[8] 后端日志${NC}"
if [ -f "$INSTALL_DIR/logs/app.log" ]; then
    echo "最后30行:"
    tail -30 "$INSTALL_DIR/logs/app.log"
else
    echo "日志文件不存在"
fi
echo ""

# 9. 外部访问测试
echo -e "${BLUE}[9] 外部访问测试${NC}"
PUBLIC_IP=$(curl -s ifconfig.me --max-time 5 2>/dev/null || hostname -I | awk '{print $1}')
echo "服务器IP: $PUBLIC_IP"
echo "测试命令: curl -I http://$PUBLIC_IP/"
echo ""

# 10. 防火墙
echo -e "${BLUE}[10] 防火墙状态${NC}"
if command -v firewall-cmd &> /dev/null; then
    echo "firewalld 端口:"
    firewall-cmd --list-ports 2>/dev/null || echo "无法获取"
elif command -v ufw &> /dev/null; then
    echo "ufw 状态:"
    ufw status 2>/dev/null || echo "无法获取"
fi
echo ""

echo "============================================"
echo "诊断完成，请将以上输出发给我分析"
echo "============================================"
