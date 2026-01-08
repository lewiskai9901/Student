# 部署问题总结与解决方案

## 概述

本文档记录了学生管理系统部署到云服务器时遇到的所有问题及解决方案，供后续部署参考。

---

## 问题 1：Java 版本不兼容

### 错误信息
```
UnsupportedClassVersionError: org/springframework/boot/loader/launch/JarLauncher : Unsupported major.minor version 61.0
```

### 原因
- JAR 包使用 Java 17 编译（version 61.0）
- 服务器默认 Java 版本是 1.7 或 1.8

### 解决方案
```bash
# 1. 安装 JDK 17（宝塔面板：软件商店 → Java项目管理器 → 安装 JDK 17）

# 2. 启动时指定 JDK 17 路径
/www/server/java/jdk-17.0.8/bin/java -jar xxx.jar

# 3. 更新启动脚本，确保使用正确的 JAVA_HOME
export JAVA_HOME="/www/server/java/jdk-17.0.8"
export PATH="$JAVA_HOME/bin:$PATH"
```

### 预防措施
- 部署脚本中添加 Java 版本检查
- 在 deploy.sh 中明确指定 JAVA_HOME

---

## 问题 2：配置文件占位符无法解析

### 错误信息
```
Could not resolve placeholder 'JWT_SECRET' in value "${JWT_SECRET}"
Could not resolve placeholder 'FILE_URL_PREFIX' in value "${FILE_URL_PREFIX}"
```

### 原因
- application-prod.yml 使用了环境变量占位符 `${XXX}`
- 但服务器上没有设置这些环境变量

### 解决方案

**方案一：直接在配置文件中写入值（推荐用于简单部署）**
```yaml
jwt:
  secret-key: your-actual-secret-key-here

file:
  upload:
    path: /www/wwwroot/student-system/uploads
    url-prefix: http://your-server-ip/uploads
```

**方案二：设置环境变量**
```bash
export JWT_SECRET="your-secret-key"
export FILE_URL_PREFIX="http://your-server-ip/uploads"
```

### 预防措施
- 部署脚本中自动生成 application-prod.yml
- 配置文件使用默认值语法：`${JWT_SECRET:default-value}`

---

## 问题 3：配置项名称不匹配

### 错误信息
```
Could not resolve placeholder 'FILE_URL_PREFIX'
```

### 原因
代码中使用的配置键与配置文件中的不一致：

| 代码期望 | 配置文件写的 |
|---------|-------------|
| `file.upload.path` | `file.upload-path` |
| `file.upload.url-prefix` | `file.url-prefix` |

### 解决方案
确保配置文件的键名与代码中的 `@Value` 注解完全匹配：
```yaml
file:
  upload:
    path: /path/to/uploads
    url-prefix: http://server-ip/uploads
```

### 预防措施
- 在代码中使用默认值：`@Value("${file.upload.path:default}")`
- 创建标准的 application-prod.yml 模板

---

## 问题 4：数据库表不存在

### 错误信息
```
Table 'student_management.system_configs' doesn't exist
Table 'student_management.users' doesn't exist
```

### 原因
- 只导入了部分 SQL 文件（evaluation_schema.sql）
- 缺少核心基础表（users, roles, permissions, system_configs 等）

### 解决方案
创建并导入完整的数据库初始化脚本：

```sql
-- core_schema.sql 包含以下表：
-- users, roles, permissions, user_roles, role_permissions
-- departments, classes, system_configs, operation_logs
```

### 预防措施
- 创建一个完整的 `complete_schema.sql` 包含所有表
- 部署脚本中按顺序导入所有必需的 SQL 文件

---

## 问题 5：密码哈希不匹配

### 错误信息
```
Bad credentials
用户名或密码错误
```

### 原因
- 手动生成的 BCrypt 密码哈希与 Spring Security 不兼容
- 或者密码哈希在 SQL 插入时被 shell 特殊字符处理破坏

### 解决方案
使用后端 JAR 包中的 Spring Security 库生成密码：

```bash
# 提取依赖
cd /tmp
jar -xf /path/to/app.jar BOOT-INF/lib

# 编译并运行密码生成器
echo 'import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
public class G{public static void main(String[]a){
System.out.println(new BCryptPasswordEncoder().encode("admin123"));}}' > G.java

javac -cp "BOOT-INF/lib/*" G.java
java -cp ".:BOOT-INF/lib/*" G

# 使用生成的哈希更新数据库（注意用 heredoc 避免 $ 转义问题）
mysql -uroot -pXXX database <<'EOF'
UPDATE users SET password='生成的哈希' WHERE username='admin';
EOF
```

### 预防措施
- 在项目中提供密码生成工具类
- 部署脚本中使用 Java 生成密码而不是手写哈希
- SQL 中使用 heredoc (`<<'EOF'`) 避免特殊字符问题

---

## 问题 6：8080 端口被占用

### 错误信息
```
8080端口: 已监听
后端进程: 未运行
```

### 原因
- 宝塔安装的 Tomcat 或其他 Java 服务占用了 8080 端口
- jsvc 进程（Tomcat）正在运行

### 解决方案
```bash
# 查看占用进程
ss -tlnp | grep 8080

# 停止占用进程
fuser -k 8080/tcp
# 或
pkill -f jsvc
```

### 预防措施
- 部署脚本中添加端口检查和清理
- 或使用其他端口（如 8081）

---

## 问题 7：mysqldump 导出包含警告信息

### 错误信息
```
ERROR 1064 (42000) at line 1: You have an error in your SQL syntax;
check the manual... near 'mysqldump: [Warning]...'
```

### 原因
- mysqldump 的警告信息被写入了 SQL 文件
- 警告：`Using a password on the command line interface can be insecure`

### 解决方案
```bash
# Windows - 将错误输出重定向到 nul
mysqldump -uroot -pXXX database 2>nul > backup.sql

# Linux - 将错误输出重定向到 /dev/null
mysqldump -uroot -pXXX database 2>/dev/null > backup.sql
```

### 预防措施
- 导出后检查 SQL 文件第一行是否为 `-- MySQL dump`
- 使用配置文件存储密码而不是命令行参数

---

## 问题 8：视图引用无效

### 错误信息
```
View 'student_management.v_check_record_class_stats_enhanced'
references invalid table(s) or column(s)
```

### 原因
- 数据库中存在引用已删除表/列的视图

### 解决方案
```bash
# 导出时跳过有问题的视图
mysqldump -uroot -pXXX database \
  --ignore-table=database.problematic_view \
  > backup.sql
```

### 预防措施
- 定期清理无效视图
- 修改表结构时同步更新相关视图

---

## 完整部署检查清单

### 部署前检查
- [ ] JDK 17 已安装
- [ ] MySQL 已安装并运行
- [ ] Redis 已安装并运行（如需要）
- [ ] Nginx 已安装并运行
- [ ] 80 端口已在云服务器安全组开放
- [ ] 8080 端口未被其他服务占用

### 数据库准备
- [ ] 创建数据库：`CREATE DATABASE student_management CHARACTER SET utf8mb4`
- [ ] 导入核心表结构（core_schema.sql）
- [ ] 导入核心初始数据（core_init_data.sql）
- [ ] 导入业务表结构（evaluation_schema.sql 等）
- [ ] 使用 Java 生成正确的管理员密码

### 后端配置
- [ ] application-prod.yml 配置正确的数据库连接
- [ ] application-prod.yml 配置正确的 Redis 连接
- [ ] jwt.secret-key 已设置（64字符以上）
- [ ] file.upload.path 和 file.upload.url-prefix 已设置
- [ ] 启动脚本使用 JDK 17

### Nginx 配置
- [ ] 前端静态文件目录正确
- [ ] /api 代理到 127.0.0.1:8080
- [ ] /uploads 配置为文件目录别名

### 部署后验证
- [ ] 访问首页能加载登录页
- [ ] 能正常登录系统
- [ ] API 接口正常响应

---

## 推荐的部署流程

```bash
# 1. 上传部署包到服务器
scp -r package/ root@server:/root/

# 2. 执行部署脚本
cd /root/package
chmod +x *.sh
./deploy.sh

# 3. 初始化数据库
./init-db.sh

# 4. 生成并更新密码
./fix-password-final.sh

# 5. 验证部署
curl http://localhost:8080/api/auth/login
```

---

## 常用诊断命令

```bash
# 检查 Java 版本
java -version
/www/server/java/jdk-17.0.8/bin/java -version

# 检查端口占用
ss -tlnp | grep -E "80|8080"

# 检查后端进程
ps -ef | grep student-management

# 查看后端日志
tail -100 /www/wwwroot/student-system/logs/app.log

# 测试后端接口
curl http://127.0.0.1:8080/api/auth/login

# 检查 Nginx 配置
nginx -t

# 重载 Nginx
nginx -s reload
```
