# 学生管理系统 - 服务器部署指南

## 一、服务器要求

- **操作系统**: CentOS 7/8 (本指南基于此系统)
- **配置**: 最低 2核4G，推荐 4核8G
- **磁盘**: 至少 40GB 可用空间

## 二、快速部署步骤

### 2.1 上传部署文件到服务器

在本地 Windows 打开命令行，执行：

```bash
# 使用 scp 上传 deploy 目录到服务器
scp -r D:\学生管理系统\deploy root@你的服务器IP:/opt/
```

或者使用 SFTP 工具（如 WinSCP、FileZilla）上传。

### 2.2 安装服务器环境

SSH 登录到服务器：

```bash
ssh root@你的服务器IP
```

执行环境安装脚本：

```bash
cd /opt/deploy
chmod +x *.sh
./install-env.sh
```

等待安装完成（约 5-10 分钟）。

### 2.3 配置 MySQL

1. **修改 root 密码**（使用安装时显示的临时密码）：

```bash
mysql -u root -p
# 输入临时密码后执行：
ALTER USER 'root'@'localhost' IDENTIFIED BY '你的新密码';
exit;
```

> **密码要求**: 至少8位，包含大小写字母、数字和特殊字符，例如：`Student@2024`

2. **创建数据库和用户**：

```bash
mysql -u root -p

# 执行以下 SQL：
CREATE DATABASE student_management CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'student_admin'@'localhost' IDENTIFIED BY '你的数据库密码';
GRANT ALL PRIVILEGES ON student_management.* TO 'student_admin'@'localhost';
FLUSH PRIVILEGES;
exit;
```

3. **导入数据库结构**（需要先上传 database 目录）：

```bash
# 上传数据库文件后执行
mysql -u root -p student_management < /opt/student-management/database/schema/complete_schema.sql
mysql -u root -p student_management < /opt/student-management/database/init/init_data.sql
```

### 2.4 构建并上传项目

#### 在本地 Windows 执行：

**1. 构建后端**

```bash
cd D:\学生管理系统\backend
mvn clean package -DskipTests
```

构建完成后，JAR 文件在 `target/student-management-0.0.1-SNAPSHOT.jar`

**2. 构建前端**

```bash
cd D:\学生管理系统\frontend
npm install
npm run build
```

构建完成后，静态文件在 `dist/` 目录

**3. 上传到服务器**

```bash
# 上传后端 JAR
scp D:\学生管理系统\backend\target\student-management-*.jar root@服务器IP:/opt/student-management/backend/

# 上传前端 dist
scp -r D:\学生管理系统\frontend\dist root@服务器IP:/opt/student-management/frontend/

# 上传数据库脚本
scp -r D:\学生管理系统\database root@服务器IP:/opt/student-management/
```

### 2.5 配置生产环境

**1. 创建环境变量文件**：

```bash
cat > /opt/student-management/backend/env.sh << 'EOF'
export DB_USERNAME=student_admin
export DB_PASSWORD=你的数据库密码
export JWT_SECRET=这里填写一个至少64字节的随机字符串作为JWT密钥
export FILE_URL_PREFIX=http://你的服务器IP/uploads
EOF
```

**2. 配置 Nginx**：

```bash
cp /opt/deploy/nginx.conf /etc/nginx/conf.d/student-management.conf

# 编辑配置，将 server_name 改为你的服务器 IP
vim /etc/nginx/conf.d/student-management.conf

# 测试配置
nginx -t

# 重新加载
nginx -s reload
```

### 2.6 启动服务

```bash
# 复制部署脚本
cp /opt/deploy/deploy.sh /opt/student-management/
chmod +x /opt/student-management/deploy.sh

# 部署所有服务
cd /opt/student-management
source backend/env.sh
./deploy.sh all

# 查看状态
./deploy.sh status
```

### 2.7 访问测试

打开浏览器访问：

- **前端页面**: `http://你的服务器IP/`
- **后端API**: `http://你的服务器IP/api/`
- **Swagger文档**: `http://你的服务器IP/api/swagger-ui.html` (如果启用)

**默认管理员账号**:
- 用户名: `admin`
- 密码: `admin123`

---

## 三、常用运维命令

### 3.1 服务管理

```bash
# 查看服务状态
./deploy.sh status

# 查看后端日志
./deploy.sh logs

# 停止后端
./deploy.sh stop

# 重启后端
./deploy.sh backend

# 重启 Nginx
systemctl restart nginx

# 重启 MySQL
systemctl restart mysqld

# 重启 Redis
systemctl restart redis
```

### 3.2 日志查看

```bash
# 后端日志
tail -f /var/log/student-management/backend.log

# Nginx 访问日志
tail -f /var/log/nginx/student-management.access.log

# Nginx 错误日志
tail -f /var/log/nginx/student-management.error.log

# MySQL 日志
tail -f /var/log/mysqld.log
```

### 3.3 数据库备份

```bash
# 创建备份目录
mkdir -p /opt/backups

# 备份数据库
mysqldump -u root -p student_management > /opt/backups/db_$(date +%Y%m%d_%H%M%S).sql

# 定时备份（添加到 crontab）
crontab -e
# 添加：每天凌晨3点备份
0 3 * * * mysqldump -u root -pYourPassword student_management > /opt/backups/db_$(date +\%Y\%m\%d).sql
```

---

## 四、常见问题

### Q1: 后端启动失败

```bash
# 查看日志
tail -100 /var/log/student-management/backend.log

# 常见原因：
# 1. 数据库连接失败 - 检查密码和用户权限
# 2. 端口被占用 - lsof -i:8080
# 3. 内存不足 - free -m
```

### Q2: 前端页面空白

```bash
# 检查文件是否上传
ls -la /usr/share/nginx/html/student-management/

# 检查 Nginx 配置
nginx -t

# 查看 Nginx 错误日志
tail -f /var/log/nginx/student-management.error.log
```

### Q3: API 请求 502 错误

```bash
# 检查后端是否运行
./deploy.sh status

# 检查后端日志
./deploy.sh logs

# 检查端口
curl http://localhost:8080/api/actuator/health
```

### Q4: MySQL 连接失败

```bash
# 检查 MySQL 服务
systemctl status mysqld

# 测试连接
mysql -u student_admin -p -h localhost student_management

# 检查用户权限
mysql -u root -p -e "SELECT user, host FROM mysql.user;"
```

---

## 五、安全建议

1. **修改默认密码**: 部署后立即修改 admin 账号密码
2. **配置防火墙**: 仅开放必要端口 (80, 443)
3. **关闭调试接口**: 生产环境关闭 Swagger 和 Druid 监控
4. **定期备份**: 设置数据库自动备份
5. **更新系统**: 定期更新系统和软件包
6. **使用 HTTPS**: 如有域名，配置 SSL 证书

---

## 六、升级部署

```bash
# 1. 备份当前版本
cp /opt/student-management/backend/*.jar /opt/backups/

# 2. 上传新版本 JAR

# 3. 重启服务
./deploy.sh backend

# 4. 验证
./deploy.sh status
curl http://localhost:8080/api/actuator/health
```
