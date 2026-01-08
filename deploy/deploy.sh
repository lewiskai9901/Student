#!/bin/bash
# ============================================
# 学生管理系统 - 部署脚本
# ============================================

set -e

APP_NAME="student-management"
APP_DIR="/opt/student-management"
BACKEND_DIR="$APP_DIR/backend"
FRONTEND_DIR="$APP_DIR/frontend"
LOG_DIR="/var/log/student-management"

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

info() { echo -e "${GREEN}[INFO]${NC} $1"; }
warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
error() { echo -e "${RED}[ERROR]${NC} $1"; }

# ============================================
# 部署后端
# ============================================
deploy_backend() {
  info "正在部署后端..."

  # 检查 JAR 文件
  JAR_FILE=$(ls $BACKEND_DIR/*.jar 2>/dev/null | head -1)
  if [ -z "$JAR_FILE" ]; then
    error "未找到 JAR 文件，请先上传到 $BACKEND_DIR"
    exit 1
  fi

  info "找到 JAR 文件: $JAR_FILE"

  # 停止旧进程
  PID=$(pgrep -f "student-management.*\.jar" || true)
  if [ -n "$PID" ]; then
    info "停止旧进程 PID: $PID"
    kill $PID
    sleep 3
  fi

  # 启动新进程
  info "启动后端服务..."
  nohup java -jar \
    -Xms512m -Xmx1024m \
    -Dspring.profiles.active=prod \
    -Dfile.encoding=UTF-8 \
    "$JAR_FILE" \
    > "$LOG_DIR/backend.log" 2>&1 &

  NEW_PID=$!
  info "后端服务已启动，PID: $NEW_PID"

  # 等待启动
  info "等待服务启动..."
  sleep 10

  # 检查是否启动成功
  if curl -s http://localhost:8080/api/actuator/health > /dev/null 2>&1; then
    info "后端服务启动成功！"
  else
    warn "后端服务可能还在启动中，请查看日志: tail -f $LOG_DIR/backend.log"
  fi
}

# ============================================
# 部署前端
# ============================================
deploy_frontend() {
  info "正在部署前端..."

  # 检查 dist 目录
  if [ ! -d "$FRONTEND_DIR/dist" ]; then
    error "未找到前端构建文件，请先上传 dist 目录到 $FRONTEND_DIR"
    exit 1
  fi

  # 复制到 Nginx 目录
  rm -rf /usr/share/nginx/html/student-management
  cp -r $FRONTEND_DIR/dist /usr/share/nginx/html/student-management

  # 重新加载 Nginx
  nginx -t && nginx -s reload

  info "前端部署完成！"
}

# ============================================
# 查看状态
# ============================================
status() {
  echo ""
  echo "=========================================="
  echo "  服务状态"
  echo "=========================================="

  # 后端状态
  PID=$(pgrep -f "student-management.*\.jar" || true)
  if [ -n "$PID" ]; then
    echo -e "后端服务: ${GREEN}运行中${NC} (PID: $PID)"
  else
    echo -e "后端服务: ${RED}已停止${NC}"
  fi

  # MySQL 状态
  if systemctl is-active --quiet mysqld; then
    echo -e "MySQL:     ${GREEN}运行中${NC}"
  else
    echo -e "MySQL:     ${RED}已停止${NC}"
  fi

  # Redis 状态
  if systemctl is-active --quiet redis; then
    echo -e "Redis:     ${GREEN}运行中${NC}"
  else
    echo -e "Redis:     ${RED}已停止${NC}"
  fi

  # Nginx 状态
  if systemctl is-active --quiet nginx; then
    echo -e "Nginx:     ${GREEN}运行中${NC}"
  else
    echo -e "Nginx:     ${RED}已停止${NC}"
  fi

  echo ""
}

# ============================================
# 查看日志
# ============================================
logs() {
  tail -f $LOG_DIR/backend.log
}

# ============================================
# 停止后端
# ============================================
stop_backend() {
  PID=$(pgrep -f "student-management.*\.jar" || true)
  if [ -n "$PID" ]; then
    info "停止后端服务 PID: $PID"
    kill $PID
    info "后端服务已停止"
  else
    warn "后端服务未运行"
  fi
}

# ============================================
# 主菜单
# ============================================
case "$1" in
  backend)
    deploy_backend
    ;;
  frontend)
    deploy_frontend
    ;;
  all)
    deploy_backend
    deploy_frontend
    ;;
  status)
    status
    ;;
  logs)
    logs
    ;;
  stop)
    stop_backend
    ;;
  *)
    echo "用法: $0 {backend|frontend|all|status|logs|stop}"
    echo ""
    echo "命令说明:"
    echo "  backend   - 部署后端服务"
    echo "  frontend  - 部署前端服务"
    echo "  all       - 部署全部"
    echo "  status    - 查看服务状态"
    echo "  logs      - 查看后端日志"
    echo "  stop      - 停止后端服务"
    exit 1
    ;;
esac
