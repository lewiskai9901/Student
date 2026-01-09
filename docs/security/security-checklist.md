# 安全配置检查清单

本文档用于部署前的安全配置检查，确保生产环境安全。

---

## 部署前必须检查

### 1. 环境变量配置

| 环境变量 | 要求 | 说明 |
|---------|------|------|
| `DB_PASSWORD` | **必须设置** | 数据库密码，禁止使用默认值 |
| `JWT_SECRET` | **必须设置** | JWT 密钥，至少64字节随机字符串 |
| `CORS_ALLOWED_ORIGINS` | **必须设置** | 允许的跨域来源，逗号分隔的域名列表 |
| `REDIS_PASSWORD` | 建议设置 | Redis 密码 |
| `DRUID_MONITOR_ENABLED` | 必须为 `false` | Druid 监控必须禁用 |

#### 环境变量示例

```bash
# 生产环境 .env 文件示例
DB_PASSWORD=YourSecureDbPassword!2024
JWT_SECRET=your-random-64-byte-string-for-jwt-signing-must-be-secure-and-random
CORS_ALLOWED_ORIGINS=https://www.your-domain.com,https://admin.your-domain.com
REDIS_PASSWORD=YourSecureRedisPassword
DRUID_MONITOR_ENABLED=false
```

---

### 2. 功能开关检查

| 配置项 | 生产值 | 开发值 | 说明 |
|-------|--------|--------|------|
| `DRUID_MONITOR_ENABLED` | `false` | `true` | Druid 监控页面 |
| `springdoc.swagger-ui.enabled` | `false` | `true` | Swagger UI |
| `springdoc.api-docs.enabled` | `false` | `true` | API 文档 |
| `spring.profiles.active` | `prod` | `dev` | 环境配置文件 |

---

### 3. 安全配置验证

启动应用后，检查日志确认：

```
✓ "Security validation passed: database password is not a default value"
✓ "JWT密钥配置验证通过，Token加密已启用"
✓ "CORS 配置验证通过，允许的来源: ..."
```

**警告日志（生产环境不应出现）：**
```
✗ "【安全警告】正在使用默认JWT密钥"
✗ "【安全警告】生产环境未配置 CORS_ALLOWED_ORIGINS"
✗ "【安全警告】生产环境 CORS 配置包含 localhost"
```

---

### 4. 网络安全配置

- [ ] HTTPS 已启用（反向代理或 SSL 证书）
- [ ] 防火墙规则已配置
- [ ] 数据库仅允许内网访问（3306 端口）
- [ ] Redis 仅允许内网访问（6379 端口）
- [ ] 后端服务端口（8080）仅允许反向代理访问

---

### 5. 应用安全配置

- [ ] CSP 头已配置（已移除 `unsafe-eval`）
- [ ] X-Frame-Options 设置为 DENY
- [ ] X-XSS-Protection 已启用
- [ ] 登录失败锁定已启用（5次失败锁定30分钟）
- [ ] Token 黑名单机制已启用

---

## 定期检查项

### 每周检查

- [ ] 检查审计日志是否有异常登录尝试
- [ ] 检查 Token 黑名单 Redis 键数量
- [ ] 检查系统日志是否有安全警告

### 每月检查

- [ ] 运行依赖漏洞扫描：`mvn dependency-check:check`
- [ ] 检查用户权限分配是否合理
- [ ] 检查是否有长期未使用的账户
- [ ] 更新依赖到最新安全版本

### 每季度检查

- [ ] 轮换 JWT 密钥（需要协调用户重新登录）
- [ ] 审查和更新 CORS 白名单
- [ ] 审查 API 访问日志
- [ ] 进行渗透测试

---

## 应急响应

### 发现安全漏洞时

1. **立即禁用相关功能**
   ```bash
   # 紧急关闭服务
   systemctl stop student-management
   ```

2. **重置所有用户 Token**
   ```bash
   # 清空 Redis 中的 Token
   redis-cli KEYS "token:*" | xargs redis-cli DEL
   ```

3. **更换密钥**
   - 更换 `DB_PASSWORD`
   - 更换 `JWT_SECRET`
   - 更换 `REDIS_PASSWORD`

4. **检查审计日志**
   ```bash
   # 查看近期登录日志
   grep "登录" /var/log/student-management/app.log | tail -100
   ```

5. **通知相关人员**
   - 技术负责人
   - 安全负责人
   - 如涉及数据泄露，通知数据保护官

---

## 安全配置代码位置

| 配置项 | 文件位置 |
|-------|---------|
| 数据库密码验证 | `config/SecurityValidationConfig.java` |
| JWT 密钥验证 | `security/JwtTokenService.java` |
| CORS 配置 | `config/SecurityConfig.java` |
| CSP 头配置 | `config/SecurityHeadersConfig.java` |
| 登录限制 | `service/impl/AuthServiceImpl.java` |
| Token 存储 | `frontend/src/utils/tokenStorage.ts` |

---

## 联系方式

如有安全问题，请联系：
- 技术支持：tech@example.com
- 安全团队：security@example.com

---

*最后更新：2026-01-09*
