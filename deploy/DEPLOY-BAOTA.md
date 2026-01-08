# 学生管理系统 - 宝塔面板部署指南

## 一、宝塔面板安装软件

登录宝塔面板后，进入 **软件商店**，安装以下软件：

| 软件 | 版本 | 说明 |
|------|------|------|
| Nginx | 1.24+ | Web服务器 |
| MySQL | 8.0 | 数据库 |
| Redis | 7.0+ | 缓存（可选但推荐） |
| Java项目管理器 | 最新版 | **重要！用于运行SpringBoot** |

> **注意**：一定要安装 **Java项目管理器**，在软件商店搜索 "Java" 即可找到

---

## 二、配置 Java 环境

1. 打开 **Java项目管理器**
2. 点击 **版本管理** → **安装JDK**
3. 选择 **JDK 17** 进行安装
4. 等待安装完成

---

## 三、创建数据库

1. 进入 **数据库** 菜单
2. 点击 **添加数据库**
3. 填写信息：
   - 数据库名：`student_management`
   - 用户名：`student_admin`
   - 密码：自己设置一个强密码（记住它！）
   - 访问权限：本地服务器
4. 点击 **提交**

---

## 四、导入数据库

1. 在数据库列表找到 `student_management`
2. 点击 **导入** 按钮
3. 依次导入以下SQL文件（需要先上传到服务器）：
   - `database/schema/complete_schema.sql`
   - `database/init/init_data.sql`

**或者使用 phpMyAdmin：**
1. 点击数据库旁边的 **管理** 按钮
2. 进入 phpMyAdmin
3. 选择 `student_management` 数据库
4. 点击 **导入** → 选择SQL文件 → 执行

---

## 五、上传项目文件

### 5.1 创建项目目录

1. 进入 **文件** 菜单
2. 进入 `/www/wwwroot/` 目录
3. 新建文件夹：`student-management`
4. 在里面再创建：`backend`、`frontend`、`uploads` 三个子目录

目录结构：
```
/www/wwwroot/student-management/
├── backend/          # 后端JAR文件
├── frontend/         # 前端静态文件
└── uploads/          # 上传文件存储
```

### 5.2 上传后端文件

1. 在本地先构建后端（如果还没构建）：
   ```bash
   cd D:\学生管理系统\backend
   mvn clean package -DskipTests
   ```

2. 在宝塔面板进入 `/www/wwwroot/student-management/backend/`
3. 点击 **上传**，选择 `backend/target/student-management-0.0.1-SNAPSHOT.jar`

### 5.3 上传前端文件

1. 在本地先构建前端（如果还没构建）：
   ```bash
   cd D:\学生管理系统\frontend
   npm install
   npm run build
   ```

2. 将 `frontend/dist` 文件夹打包成 zip
3. 在宝塔面板进入 `/www/wwwroot/student-management/frontend/`
4. 上传 zip 文件并解压

### 5.4 上传数据库文件

将 `database` 文件夹上传到 `/www/wwwroot/student-management/`

---

## 六、配置并启动后端

### 6.1 添加 Java 项目

1. 打开 **Java项目管理器**
2. 点击 **添加项目**
3. 填写配置：

| 配置项 | 值 |
|--------|-----|
| 项目名称 | student-management |
| 项目路径 | /www/wwwroot/student-management/backend |
| 项目JAR | student-management-0.0.1-SNAPSHOT.jar |
| 项目端口 | 8080 |
| JDK版本 | JDK 17 |

4. **项目参数** 填写：
```
-Xms512m -Xmx1024m -Dspring.profiles.active=prod -Dfile.encoding=UTF-8
```

5. **项目环境变量** 填写（点击展开）：
```
DB_USERNAME=student_admin
DB_PASSWORD=你的数据库密码
JWT_SECRET=student-management-jwt-secret-key-2024-production-environment-random-string-64bytes
FILE_UPLOAD_PATH=/www/wwwroot/student-management/uploads/
```

6. 点击 **提交**

### 6.2 启动项目

1. 在项目列表找到 `student-management`
2. 点击 **启动** 按钮
3. 等待启动完成（约30秒-1分钟）
4. 点击 **日志** 查看是否有错误

**验证后端是否启动成功：**
- 点击项目的 **状态**，确认为 "运行中"
- 或在服务器终端执行：`curl http://localhost:8080/api/actuator/health`

---

## 七、创建网站并配置

### 7.1 创建网站

1. 进入 **网站** 菜单
2. 点击 **添加站点**
3. 填写信息：
   - 域名：填写你的服务器IP（如：`123.456.789.0`）
   - 根目录：`/www/wwwroot/student-management/frontend/dist`
   - PHP版本：纯静态
4. 点击 **提交**

### 7.2 配置反向代理

1. 找到刚创建的网站，点击 **设置**
2. 进入 **反向代理** 菜单
3. 点击 **添加反向代理**
4. 填写：
   - 代理名称：`api`
   - 目标URL：`http://127.0.0.1:8080`
   - 发送域名：`$host`
5. 点击 **提交**
6. 找到刚添加的代理，点击 **配置文件**，修改为：

```nginx
#PROXY-START/api

location /api/ {
    proxy_pass http://127.0.0.1:8080/api/;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
    proxy_connect_timeout 60s;
    proxy_send_timeout 60s;
    proxy_read_timeout 60s;
    client_max_body_size 50M;
}

#PROXY-END/api
```

7. 点击 **保存**

### 7.3 配置上传文件访问

1. 在网站设置中，进入 **配置文件**
2. 在 `server { }` 块内添加：

```nginx
# 上传文件访问
location /uploads/ {
    alias /www/wwwroot/student-management/uploads/;
    expires 30d;
}
```

3. 保存配置

### 7.4 配置伪静态（Vue History模式）

1. 在网站设置中，进入 **伪静态**
2. 填写以下内容：

```nginx
location / {
    try_files $uri $uri/ /index.html;
}
```

3. 保存

---

## 八、配置安全组/防火墙

### 8.1 宝塔防火墙

1. 进入 **安全** 菜单
2. 确保以下端口已放行：
   - 80 (HTTP)
   - 443 (HTTPS)
   - 8080 (后端，可选)

### 8.2 云服务器安全组

登录你的云服务器控制台（阿里云/腾讯云等），在安全组中放行：
- 80 端口
- 443 端口

---

## 九、访问测试

打开浏览器访问：

```
http://你的服务器IP/
```

**默认登录账号：**
- 用户名：`admin`
- 密码：`admin123`

---

## 十、常见问题排查

### Q1: 后端启动失败

1. 在 Java项目管理器 中查看 **日志**
2. 常见原因：
   - 数据库连接失败：检查环境变量中的密码
   - 端口被占用：`lsof -i:8080`
   - 内存不足：减小 JVM 参数

### Q2: 前端页面空白

1. 检查文件是否正确上传到 `frontend/dist/`
2. 检查网站根目录配置是否正确
3. 检查伪静态是否配置

### Q3: API 请求失败 (502/504)

1. 检查后端是否正在运行
2. 检查反向代理配置
3. 查看 Nginx 错误日志

### Q4: 上传文件失败

1. 检查 `uploads` 目录权限：`chmod 755 /www/wwwroot/student-management/uploads`
2. 检查 Nginx 配置中的 `client_max_body_size`

---

## 十一、更新部署

1. 上传新的 JAR 文件覆盖旧的
2. 在 Java项目管理器 中 **重启** 项目
3. 清理浏览器缓存测试

---

## 十二、备份建议

1. **数据库备份**：宝塔面板 → 数据库 → 备份
2. **设置定时备份**：宝塔面板 → 计划任务 → 添加数据库备份任务
