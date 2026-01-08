#!/bin/bash
# ============================================
# 学生管理系统 - 服务器环境安装脚本
# 适用于 CentOS 7/8
# ============================================

set -e

echo "=========================================="
echo "  学生管理系统 - 环境安装脚本"
echo "=========================================="

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 打印带颜色的消息
info() { echo -e "${GREEN}[INFO]${NC} $1"; }
warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
error() { echo -e "${RED}[ERROR]${NC} $1"; }

# 检查是否为root用户
if [ "$EUID" -ne 0 ]; then
  error "请使用 root 用户执行此脚本"
  exit 1
fi

# ============================================
# 1. 系统更新和基础工具
# ============================================
info "正在更新系统和安装基础工具..."
yum update -y
yum install -y wget curl vim git unzip lsof net-tools firewalld

# ============================================
# 2. 安装 JDK 17
# ============================================
info "正在安装 JDK 17..."
if ! command -v java &> /dev/null || ! java -version 2>&1 | grep -q "17"; then
  # 下载并安装 OpenJDK 17
  yum install -y java-17-openjdk java-17-openjdk-devel

  # 设置 JAVA_HOME
  JAVA_HOME=$(dirname $(dirname $(readlink -f $(which java))))
  echo "export JAVA_HOME=$JAVA_HOME" >> /etc/profile
  echo "export PATH=\$PATH:\$JAVA_HOME/bin" >> /etc/profile
  source /etc/profile

  info "JDK 17 安装完成"
  java -version
else
  info "JDK 17 已安装"
fi

# ============================================
# 3. 安装 MySQL 8
# ============================================
info "正在安装 MySQL 8..."
if ! command -v mysql &> /dev/null; then
  # 添加 MySQL 仓库
  rpm -Uvh https://dev.mysql.com/get/mysql80-community-release-el7-7.noarch.rpm || true

  # 禁用默认模块（CentOS 8）
  yum module disable mysql -y 2>/dev/null || true

  # 安装 MySQL
  yum install -y mysql-community-server

  # 启动 MySQL
  systemctl start mysqld
  systemctl enable mysqld

  # 获取临时密码
  TEMP_PWD=$(grep 'temporary password' /var/log/mysqld.log | awk '{print $NF}')
  info "MySQL 临时密码: $TEMP_PWD"
  info "请记录此密码，稍后需要用它来设置新密码"

  info "MySQL 8 安装完成"
else
  info "MySQL 已安装"
fi

# ============================================
# 4. 安装 Redis
# ============================================
info "正在安装 Redis..."
if ! command -v redis-server &> /dev/null; then
  yum install -y epel-release
  yum install -y redis

  # 修改配置允许远程连接（可选）
  # sed -i 's/bind 127.0.0.1/bind 0.0.0.0/' /etc/redis.conf

  # 启动 Redis
  systemctl start redis
  systemctl enable redis

  info "Redis 安装完成"
else
  info "Redis 已安装"
fi

# ============================================
# 5. 安装 Nginx
# ============================================
info "正在安装 Nginx..."
if ! command -v nginx &> /dev/null; then
  yum install -y nginx

  # 启动 Nginx
  systemctl start nginx
  systemctl enable nginx

  info "Nginx 安装完成"
else
  info "Nginx 已安装"
fi

# ============================================
# 6. 配置防火墙
# ============================================
info "正在配置防火墙..."
systemctl start firewalld
systemctl enable firewalld

# 开放必要端口
firewall-cmd --permanent --add-port=80/tcp      # HTTP
firewall-cmd --permanent --add-port=443/tcp     # HTTPS
firewall-cmd --permanent --add-port=8080/tcp    # 后端API
firewall-cmd --permanent --add-port=3000/tcp    # 前端开发
firewall-cmd --permanent --add-port=3306/tcp    # MySQL（生产环境建议关闭）
firewall-cmd --reload

info "防火墙配置完成"

# ============================================
# 7. 创建项目目录
# ============================================
info "正在创建项目目录..."
mkdir -p /opt/student-management/{backend,frontend,logs,uploads}
mkdir -p /var/log/student-management
chown -R root:root /opt/student-management

info "项目目录创建完成: /opt/student-management"

# ============================================
# 完成
# ============================================
echo ""
echo "=========================================="
echo "  环境安装完成！"
echo "=========================================="
echo ""
echo "已安装的服务:"
echo "  - JDK 17:  $(java -version 2>&1 | head -1)"
echo "  - MySQL 8: $(mysql --version)"
echo "  - Redis:   $(redis-server --version | head -1)"
echo "  - Nginx:   $(nginx -v 2>&1)"
echo ""
echo "项目目录: /opt/student-management"
echo ""
echo "下一步操作:"
echo "  1. 配置 MySQL 密码: mysql_secure_installation"
echo "  2. 创建数据库和用户"
echo "  3. 上传并部署项目"
echo ""
warn "MySQL 临时密码已显示在上方，请先修改密码！"
