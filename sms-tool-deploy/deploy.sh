#!/bin/bash
# ============================================
#  短信验证码 + 小红书粉丝查询 - 一键部署脚本
#  适用于: Ubuntu/Debian/CentOS (x64/arm64)
# ============================================

set -e

APP_DIR="/opt/sms-tool"
SMS_PORT=3333
XHS_PORT=3457

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

log()  { echo -e "${GREEN}[OK]${NC} $1"; }
warn() { echo -e "${YELLOW}[!]${NC} $1"; }
err()  { echo -e "${RED}[X]${NC} $1"; exit 1; }

echo ""
echo "=========================================="
echo "   短信验证码 + 小红书粉丝查询 - 一键部署"
echo "=========================================="
echo ""

# ---- 1. Check root ----
if [ "$(id -u)" -ne 0 ]; then
  err "请使用 root 用户运行: sudo bash deploy.sh"
fi

# ---- 2. Install Node.js ----
NODE_VER_REQUIRED="v18.20.8"

if command -v node &>/dev/null; then
  NODE_VER=$(node -v)
  log "Node.js 已安装: $NODE_VER"
else
  warn "未检测到 Node.js，正在安装..."

  ARCH=$(uname -m)
  case "$ARCH" in
    x86_64|amd64)  NODE_ARCH="x64" ;;
    aarch64|arm64) NODE_ARCH="arm64" ;;
    armv7l)        NODE_ARCH="armv7l" ;;
    *)             err "不支持的 CPU 架构: $ARCH" ;;
  esac

  NODE_FILENAME="node-${NODE_VER_REQUIRED}-linux-${NODE_ARCH}"
  NODE_URL="https://nodejs.org/dist/${NODE_VER_REQUIRED}/${NODE_FILENAME}.tar.xz"
  NODE_INSTALL_DIR="/usr/local/lib/nodejs"

  warn "下载 Node.js ${NODE_VER_REQUIRED} (${NODE_ARCH})..."
  cd /tmp
  curl -fsSL -o "${NODE_FILENAME}.tar.xz" "$NODE_URL" || err "下载失败"

  mkdir -p "$NODE_INSTALL_DIR"
  tar -xJf "${NODE_FILENAME}.tar.xz" -C "$NODE_INSTALL_DIR"
  rm -f "${NODE_FILENAME}.tar.xz"

  ln -sf "${NODE_INSTALL_DIR}/${NODE_FILENAME}/bin/node" /usr/local/bin/node
  ln -sf "${NODE_INSTALL_DIR}/${NODE_FILENAME}/bin/npm" /usr/local/bin/npm
  ln -sf "${NODE_INSTALL_DIR}/${NODE_FILENAME}/bin/npx" /usr/local/bin/npx

  command -v node &>/dev/null || err "Node.js 安装失败"
  log "Node.js 安装完成: $(node -v)"
fi

# ---- 3. Install Chromium dependencies ----
warn "安装 Chromium 运行所需的系统依赖库..."

if command -v apt-get &>/dev/null; then
  # Debian/Ubuntu
  apt-get update -qq
  apt-get install -y -qq \
    libnss3 libatk1.0-0 libatk-bridge2.0-0 libcups2 libdrm2 \
    libxkbcommon0 libxcomposite1 libxdamage1 libxrandr2 libgbm1 \
    libpango-1.0-0 libcairo2 libasound2 libxshmfence1 \
    fonts-liberation fonts-noto-cjk wget ca-certificates \
    2>/dev/null && log "系统依赖安装完成" || warn "部分依赖安装失败，继续..."

elif command -v yum &>/dev/null || command -v dnf &>/dev/null; then
  # CentOS/RHEL/Fedora
  PKG_MGR=$(command -v dnf 2>/dev/null || echo "yum")
  $PKG_MGR install -y -q \
    nss atk at-spi2-atk cups-libs libdrm libxkbcommon \
    libXcomposite libXdamage libXrandr mesa-libgbm pango cairo \
    alsa-lib libxshmfence wget ca-certificates \
    2>/dev/null && log "系统依赖安装完成" || warn "部分依赖安装失败，继续..."
fi

# Check if system Chrome/Chromium exists (optional, Puppeteer has its own)
CHROME_BIN=$(which google-chrome-stable 2>/dev/null || which google-chrome 2>/dev/null || which chromium-browser 2>/dev/null || which chromium 2>/dev/null || echo "")
if [ -n "$CHROME_BIN" ]; then
  log "检测到系统浏览器: $CHROME_BIN"
else
  log "未检测到系统浏览器，将使用 Puppeteer 内置 Chromium"
fi

# ---- 4. Install pm2 ----
if command -v pm2 &>/dev/null; then
  log "pm2 已安装"
else
  warn "正在安装 pm2..."
  npm install -g pm2

  if ! command -v pm2 &>/dev/null; then
    PM2_BIN=$(find /usr/local/lib/nodejs -name "pm2" -path "*/bin/pm2" 2>/dev/null | head -1)
    [ -n "$PM2_BIN" ] && ln -sf "$PM2_BIN" /usr/local/bin/pm2
  fi

  command -v pm2 &>/dev/null || err "pm2 安装失败"
  log "pm2 安装完成"
fi

# ---- 5. Deploy files ----
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

mkdir -p "$APP_DIR"
cp "$SCRIPT_DIR/sms-tool-server.js" "$APP_DIR/"
cp "$SCRIPT_DIR/xhs-fans-server.js" "$APP_DIR/"
cp "$SCRIPT_DIR/sms-code-tool.html" "$APP_DIR/"
cp "$SCRIPT_DIR/sms-admin.html" "$APP_DIR/"
cp "$SCRIPT_DIR/xhs-fans-checker.html" "$APP_DIR/"
cp "$SCRIPT_DIR/package.json" "$APP_DIR/"
mkdir -p "$APP_DIR/sms-tool-data"
mkdir -p "$APP_DIR/.xhs-data"

log "文件部署到 $APP_DIR"

# ---- 6. Install npm dependencies (puppeteer) ----
cd "$APP_DIR"
warn "安装依赖 (puppeteer)，可能需要几分钟..."
npm install --production 2>&1 | tail -3
log "依赖安装完成"

# ---- 7. Set Chrome path for Puppeteer ----
if [ -n "$CHROME_BIN" ]; then
  export CHROME_PATH="$CHROME_BIN"
fi

# ---- 8. Stop old instances ----
pm2 describe "sms-tool" &>/dev/null && pm2 delete "sms-tool" 2>/dev/null
pm2 describe "xhs-fans" &>/dev/null && pm2 delete "xhs-fans" 2>/dev/null
log "已清理旧实例"

# ---- 9. Start services ----
cd "$APP_DIR"
PORT=$SMS_PORT pm2 start sms-tool-server.js --name "sms-tool"
PORT=$XHS_PORT CHROME_PATH="${CHROME_BIN:-}" pm2 start xhs-fans-server.js --name "xhs-fans"
pm2 save

log "服务已启动"

# ---- 10. Auto-start on boot ----
pm2 startup -u root --hp /root 2>/dev/null || true
pm2 save
log "已设置开机自启"

# ---- 11. Firewall ----
for P in $SMS_PORT $XHS_PORT; do
  if command -v firewall-cmd &>/dev/null; then
    firewall-cmd --permanent --add-port=${P}/tcp 2>/dev/null
  fi
  if command -v ufw &>/dev/null; then
    ufw allow $P/tcp 2>/dev/null
  fi
done
command -v firewall-cmd &>/dev/null && firewall-cmd --reload 2>/dev/null
log "防火墙已放行端口 $SMS_PORT, $XHS_PORT"

# ---- 12. Get public IP ----
PUBLIC_IP=$(curl -s --max-time 5 ifconfig.me 2>/dev/null || curl -s --max-time 5 ip.sb 2>/dev/null || echo "获取失败")

echo ""
echo "=========================================="
echo -e "  ${GREEN}部署完成！${NC}"
echo "=========================================="
echo ""
echo "  短信工具:     http://${PUBLIC_IP}:${SMS_PORT}"
echo "  粉丝查询:     http://${PUBLIC_IP}:${XHS_PORT}"
echo "  粉丝管理后台: http://${PUBLIC_IP}:${XHS_PORT}/admin"
echo ""
echo -e "  ${YELLOW}重要: 请确认云服务器安全组已放行端口 ${SMS_PORT} 和 ${XHS_PORT}${NC}"
echo ""
echo "  常用命令:"
echo "    pm2 logs         # 查看所有日志"
echo "    pm2 restart all  # 重启所有服务"
echo "    pm2 status       # 查看状态"
echo "    pm2 monit        # 监控面板"
echo ""
