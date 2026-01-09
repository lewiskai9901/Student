# 学生管理系统健康度改进计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 将项目健康度从 60/100 提升至 85/100，解决安全隐患、架构混乱、测试不足等核心问题

**Architecture:** 分四阶段推进：紧急安全修复 → 架构规范化 → 测试覆盖提升 → 代码质量改进。采用 TDD 方式，每个任务完成后提交代码。

**Tech Stack:** Spring Boot 3.2, MyBatis Plus 3.5.7, Vue 3, TypeScript, JUnit 5, Mockito

---

## 阶段概览

| 阶段 | 名称 | 时长 | 任务数 | 预期收益 |
|------|------|------|--------|---------|
| 1 | 紧急安全修复 | 3天 | 8个 | 消除高危安全风险 |
| 2 | 架构规范化 | 2周 | 12个 | V1/V2边界清晰，Bean冲突解决 |
| 3 | 测试覆盖提升 | 3周 | 15个 | 覆盖率从2%提升到50% |
| 4 | 代码质量改进 | 2周 | 10个 | 消除代码坏味道 |

---

# 阶段一：紧急安全修复（3天）

## Task 1.1: 移除硬编码数据库默认密码

**Files:**
- Modify: `backend/src/main/resources/application.yml:19`
- Create: `backend/src/main/java/com/school/management/config/SecurityValidationConfig.java`
- Test: `backend/src/test/java/com/school/management/config/SecurityValidationConfigTest.java`

**Step 1: 写失败测试**

```java
// backend/src/test/java/com/school/management/config/SecurityValidationConfigTest.java
package com.school.management.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

class SecurityValidationConfigTest {

    @Test
    @DisplayName("当数据库密码为默认值时应抛出异常")
    void shouldRejectDefaultPassword() {
        SecurityValidationConfig config = new SecurityValidationConfig();

        assertThrows(IllegalStateException.class, () -> {
            config.validateDatabasePassword("123456");
        });
    }

    @Test
    @DisplayName("当数据库密码为安全值时应通过验证")
    void shouldAcceptSecurePassword() {
        SecurityValidationConfig config = new SecurityValidationConfig();

        assertDoesNotThrow(() -> {
            config.validateDatabasePassword("MySecureP@ssw0rd!");
        });
    }

    @Test
    @DisplayName("当数据库密码为空时应抛出异常")
    void shouldRejectEmptyPassword() {
        SecurityValidationConfig config = new SecurityValidationConfig();

        assertThrows(IllegalStateException.class, () -> {
            config.validateDatabasePassword("");
        });
    }
}
```

**Step 2: 运行测试验证失败**

Run: `cd backend && mvn test -Dtest=SecurityValidationConfigTest -q`
Expected: FAIL with "cannot find symbol: class SecurityValidationConfig"

**Step 3: 实现安全验证配置类**

```java
// backend/src/main/java/com/school/management/config/SecurityValidationConfig.java
package com.school.management.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

@Slf4j
@Configuration
public class SecurityValidationConfig {

    private static final Set<String> FORBIDDEN_PASSWORDS = Set.of(
        "123456", "password", "admin", "root", "test", "demo",
        "123456789", "12345678", "1234567890", "qwerty"
    );

    @Value("${spring.datasource.password:}")
    private String dbPassword;

    @PostConstruct
    public void validateOnStartup() {
        validateDatabasePassword(dbPassword);
        log.info("Security validation passed: database password is not a default value");
    }

    public void validateDatabasePassword(String password) {
        if (password == null || password.isBlank()) {
            throw new IllegalStateException(
                "Database password is not configured. " +
                "Please set DB_PASSWORD environment variable."
            );
        }

        if (FORBIDDEN_PASSWORDS.contains(password.toLowerCase())) {
            throw new IllegalStateException(
                "SECURITY RISK: Database password is set to a common default value. " +
                "Please change DB_PASSWORD to a secure password before starting the application."
            );
        }

        if (password.length() < 8) {
            throw new IllegalStateException(
                "Database password is too short. " +
                "Please use a password with at least 8 characters."
            );
        }
    }
}
```

**Step 4: 修改 application.yml 移除默认密码**

```yaml
# backend/src/main/resources/application.yml Line 19
# 修改前:
#   password: ${DB_PASSWORD:123456}
# 修改后:
    password: ${DB_PASSWORD}
```

**Step 5: 运行测试验证通过**

Run: `cd backend && mvn test -Dtest=SecurityValidationConfigTest -q`
Expected: PASS (3 tests)

**Step 6: 提交**

```bash
git add backend/src/main/java/com/school/management/config/SecurityValidationConfig.java
git add backend/src/test/java/com/school/management/config/SecurityValidationConfigTest.java
git add backend/src/main/resources/application.yml
git commit -m "security: reject default database passwords on startup

- Add SecurityValidationConfig to validate DB password
- Remove default password from application.yml
- Add unit tests for password validation"
```

---

## Task 1.2: 禁用生产环境 Druid 监控

**Files:**
- Modify: `backend/src/main/resources/application.yml:49-54`
- Create: `backend/src/main/resources/application-prod.yml` (如不存在)

**Step 1: 检查当前配置**

```yaml
# 当前 application.yml 配置 (Line 49-54)
druid:
  stat-view-servlet:
    enabled: ${DRUID_MONITOR_ENABLED:true}  # 危险：默认启用
    login-username: ${DRUID_MONITOR_USER:admin}
    login-password: ${DRUID_MONITOR_PWD:DruidMonitor@2024!Secure}
```

**Step 2: 修改默认配置为禁用**

```yaml
# backend/src/main/resources/application.yml Line 49-54
# 修改后:
druid:
  stat-view-servlet:
    enabled: ${DRUID_MONITOR_ENABLED:false}  # 默认禁用
    login-username: ${DRUID_MONITOR_USER:}
    login-password: ${DRUID_MONITOR_PWD:}
    allow: 127.0.0.1  # 仅允许本地访问
    deny:   # 黑名单为空
```

**Step 3: 确保 application-prod.yml 明确禁用**

```yaml
# backend/src/main/resources/application-prod.yml (添加或确认)
spring:
  datasource:
    druid:
      stat-view-servlet:
        enabled: false  # 生产环境强制禁用
```

**Step 4: 提交**

```bash
git add backend/src/main/resources/application.yml
git add backend/src/main/resources/application-prod.yml
git commit -m "security: disable Druid monitor by default

- Change default DRUID_MONITOR_ENABLED to false
- Remove default credentials
- Restrict access to localhost only
- Force disable in production profile"
```

---

## Task 1.3: 修复 CSP 安全策略

**Files:**
- Modify: `backend/src/main/java/com/school/management/config/SecurityHeadersConfig.java:44-48`

**Step 1: 查看当前 CSP 配置**

当前问题：允许 `'unsafe-inline'` 和 `'unsafe-eval'`

**Step 2: 更新 CSP 策略**

```java
// backend/src/main/java/com/school/management/config/SecurityHeadersConfig.java
// 找到 contentSecurityPolicy 方法或配置，修改为:

// 修改前 (约 Line 44-48):
// .contentSecurityPolicy("default-src 'self'; script-src 'self' 'unsafe-inline' 'unsafe-eval'; ...")

// 修改后:
.headers(headers -> headers
    .contentSecurityPolicy(csp -> csp
        .policyDirectives(
            "default-src 'self'; " +
            "script-src 'self'; " +  // 移除 unsafe-inline 和 unsafe-eval
            "style-src 'self' 'unsafe-inline'; " +  // 样式保留 unsafe-inline (Element Plus 需要)
            "img-src 'self' data: https:; " +
            "font-src 'self' data:; " +
            "connect-src 'self' https://api.weixin.qq.com; " +
            "frame-ancestors 'none'; " +
            "form-action 'self'"
        )
    )
    .frameOptions(frame -> frame.deny())
    .xssProtection(xss -> xss.headerValue(XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK))
)
```

**Step 3: 提交**

```bash
git add backend/src/main/java/com/school/management/config/SecurityHeadersConfig.java
git commit -m "security: strengthen Content-Security-Policy

- Remove unsafe-inline and unsafe-eval from script-src
- Keep unsafe-inline for style-src (required by Element Plus)
- Add frame-ancestors 'none' for clickjacking protection"
```

---

## Task 1.4: 前端 Token 存储安全加固

**Files:**
- Modify: `frontend/src/stores/auth.ts`
- Modify: `frontend/src/utils/request.ts`

**Step 1: 理解当前实现**

当前问题：Refresh Token 直接存储在 localStorage，易受 XSS 攻击

**Step 2: 实现 Token 存储封装**

```typescript
// frontend/src/utils/tokenStorage.ts (新建)
/**
 * Token 安全存储工具
 * Access Token: 存储在内存 + sessionStorage (短期)
 * Refresh Token: 存储在 localStorage (加密 - 未来改为 HttpOnly Cookie)
 */

const ACCESS_TOKEN_KEY = 'access_token'
const REFRESH_TOKEN_KEY = 'refresh_token'

// 内存中的 Access Token (XSS 更安全)
let memoryAccessToken: string | null = null

export const tokenStorage = {
  getAccessToken(): string | null {
    // 优先从内存获取
    if (memoryAccessToken) {
      return memoryAccessToken
    }
    // 页面刷新后从 sessionStorage 恢复
    return sessionStorage.getItem(ACCESS_TOKEN_KEY)
  },

  setAccessToken(token: string): void {
    memoryAccessToken = token
    // 备份到 sessionStorage (页面刷新后恢复)
    sessionStorage.setItem(ACCESS_TOKEN_KEY, token)
  },

  getRefreshToken(): string | null {
    return localStorage.getItem(REFRESH_TOKEN_KEY)
  },

  setRefreshToken(token: string): void {
    // TODO: 未来应改为 HttpOnly Cookie
    localStorage.setItem(REFRESH_TOKEN_KEY, token)
  },

  clearAll(): void {
    memoryAccessToken = null
    sessionStorage.removeItem(ACCESS_TOKEN_KEY)
    localStorage.removeItem(REFRESH_TOKEN_KEY)
    localStorage.removeItem('userInfo')
  },

  // 检测是否有有效 Token
  hasTokens(): boolean {
    return !!(this.getAccessToken() || this.getRefreshToken())
  }
}
```

**Step 3: 更新 auth store 使用新的存储工具**

```typescript
// frontend/src/stores/auth.ts
// 修改 import 和相关调用

import { tokenStorage } from '@/utils/tokenStorage'

// 修改 setToken 方法
const setTokens = (accessToken: string, refreshToken: string) => {
  tokenStorage.setAccessToken(accessToken)
  tokenStorage.setRefreshToken(refreshToken)
}

// 修改 getToken 方法
const getAccessToken = () => tokenStorage.getAccessToken()
const getRefreshToken = () => tokenStorage.getRefreshToken()

// 修改 logout 方法
const logout = () => {
  tokenStorage.clearAll()
  // ... 其他清理逻辑
}
```

**Step 4: 提交**

```bash
git add frontend/src/utils/tokenStorage.ts
git add frontend/src/stores/auth.ts
git commit -m "security: improve token storage security

- Create tokenStorage utility with memory + sessionStorage for access token
- Keep refresh token in localStorage (TODO: migrate to HttpOnly Cookie)
- Add clearAll method for secure logout"
```

---

## Task 1.5: 添加登录失败次数限制测试

**Files:**
- Test: `backend/src/test/java/com/school/management/service/AuthServiceLoginLimitTest.java`

**Step 1: 写测试验证登录限制功能**

```java
// backend/src/test/java/com/school/management/service/AuthServiceLoginLimitTest.java
package com.school.management.service;

import com.school.management.dto.auth.LoginRequest;
import com.school.management.exception.BusinessException;
import com.school.management.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.authentication.AuthenticationManager;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("登录失败次数限制测试")
class AuthServiceLoginLimitTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("连续5次失败后应锁定账户")
    void shouldLockAccountAfter5FailedAttempts() {
        // Given: 已有4次失败记录
        when(valueOperations.get(anyString())).thenReturn("4");

        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("wrongpassword");

        // When & Then: 第5次失败应抛出账户锁定异常
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            authService.checkLoginAttempts("testuser");
        });

        assertTrue(exception.getMessage().contains("锁定") ||
                   exception.getMessage().contains("locked"));
    }

    @Test
    @DisplayName("锁定期内应拒绝登录")
    void shouldRejectLoginDuringLockPeriod() {
        // Given: 账户已锁定
        when(valueOperations.get(contains("lock"))).thenReturn("true");

        // When & Then
        assertThrows(BusinessException.class, () -> {
            authService.checkLoginAttempts("lockeduser");
        });
    }

    @Test
    @DisplayName("登录成功后应重置失败计数")
    void shouldResetCounterOnSuccessfulLogin() {
        // Given: 有之前的失败记录
        when(valueOperations.get(anyString())).thenReturn("3");

        // When: 登录成功
        authService.resetLoginAttempts("testuser");

        // Then: 应删除失败计数
        verify(redisTemplate).delete(contains("testuser"));
    }
}
```

**Step 2: 运行测试**

Run: `cd backend && mvn test -Dtest=AuthServiceLoginLimitTest -q`
Expected: 根据实现情况可能需要调整

**Step 3: 提交**

```bash
git add backend/src/test/java/com/school/management/service/AuthServiceLoginLimitTest.java
git commit -m "test: add login attempt limit tests

- Test account lockout after 5 failed attempts
- Test rejection during lock period
- Test counter reset on successful login"
```

---

## Task 1.6: 修复 JWT 密钥配置警告

**Files:**
- Modify: `backend/src/main/resources/application.yml:127`

**Step 1: 移除默认 JWT 密钥**

```yaml
# backend/src/main/resources/application.yml Line 127
# 修改前:
# jwt:
#   secret: ${JWT_SECRET:student-management-system-jwt-secret-key-2024-base64-encoded-long-enough-for-hs512-algorithm-requirement}

# 修改后:
jwt:
  secret: ${JWT_SECRET}  # 强制要求环境变量配置
  access-token-expiration: 7200000   # 2小时
  refresh-token-expiration: 2592000000  # 30天
```

**Step 2: 更新 JwtTokenService 添加启动验证**

```java
// 在 JwtTokenService.java 的 @PostConstruct 方法中添加:

@PostConstruct
public void validateConfiguration() {
    if (jwtSecret == null || jwtSecret.isBlank()) {
        throw new IllegalStateException(
            "JWT_SECRET environment variable is not configured. " +
            "Please set a secure secret key (minimum 64 bytes for HS512)."
        );
    }

    if (jwtSecret.getBytes(StandardCharsets.UTF_8).length < 64) {
        throw new IllegalStateException(
            "JWT secret key is too short. HS512 requires at least 64 bytes."
        );
    }

    log.info("JWT密钥配置验证通过，Token服务已就绪");
}
```

**Step 3: 提交**

```bash
git add backend/src/main/resources/application.yml
git add backend/src/main/java/com/school/management/security/JwtTokenService.java
git commit -m "security: enforce JWT secret configuration

- Remove default JWT secret from application.yml
- Add startup validation for JWT secret presence and length
- Require minimum 64 bytes for HS512 algorithm"
```

---

## Task 1.7: 添加 CORS 生产环境配置检查

**Files:**
- Modify: `backend/src/main/java/com/school/management/config/SecurityConfig.java`

**Step 1: 添加 CORS 配置验证**

```java
// backend/src/main/java/com/school/management/config/SecurityConfig.java
// 在 CORS 配置方法中添加:

@Value("${cors.allowed-origins:}")
private String allowedOrigins;

@Value("${spring.profiles.active:dev}")
private String activeProfile;

@PostConstruct
public void validateCorsConfiguration() {
    if ("prod".equals(activeProfile)) {
        if (allowedOrigins == null || allowedOrigins.isBlank()) {
            log.warn("SECURITY WARNING: CORS_ALLOWED_ORIGINS is not configured for production!");
        }

        if (allowedOrigins != null && (
            allowedOrigins.contains("localhost") ||
            allowedOrigins.contains("127.0.0.1"))) {
            log.warn("SECURITY WARNING: CORS allows localhost in production environment!");
        }
    }
}
```

**Step 2: 提交**

```bash
git add backend/src/main/java/com/school/management/config/SecurityConfig.java
git commit -m "security: add CORS configuration validation for production

- Warn if CORS_ALLOWED_ORIGINS is not configured in production
- Warn if localhost is allowed in production"
```

---

## Task 1.8: 创建安全配置检查清单文档

**Files:**
- Create: `docs/security/security-checklist.md`

**Step 1: 创建安全检查清单**

```markdown
# 安全配置检查清单

## 部署前必须检查

### 环境变量配置

- [ ] `DB_PASSWORD` - 数据库密码（禁止使用默认值）
- [ ] `JWT_SECRET` - JWT 密钥（至少64字节）
- [ ] `CORS_ALLOWED_ORIGINS` - 允许的跨域来源（生产环境必须配置实际域名）
- [ ] `REDIS_PASSWORD` - Redis 密码

### 功能开关

- [ ] `DRUID_MONITOR_ENABLED=false` - Druid 监控必须禁用
- [ ] `springdoc.swagger-ui.enabled=false` - Swagger UI 必须禁用
- [ ] `spring.profiles.active=prod` - 使用生产配置文件

### 安全配置

- [ ] HTTPS 已启用
- [ ] 防火墙规则已配置
- [ ] 数据库仅允许内网访问
- [ ] Redis 仅允许内网访问

## 定期检查

- [ ] 检查依赖漏洞 (`mvn dependency-check:check`)
- [ ] 检查日志中是否有敏感信息
- [ ] 检查用户权限分配是否合理
- [ ] 检查 Token 黑名单清理策略

## 应急响应

如发现安全问题：
1. 立即禁用相关功能
2. 重置所有用户 Token
3. 更换数据库密码和 JWT 密钥
4. 检查审计日志
```

**Step 2: 提交**

```bash
git add docs/security/security-checklist.md
git commit -m "docs: add security checklist for deployment

- Environment variables checklist
- Feature toggles checklist
- Security configuration checklist
- Emergency response guide"
```

---

# 阶段二：架构规范化（2周）

## Task 2.1: 统一 Domain Event 基类

**Files:**
- Create: `backend/src/main/java/com/school/management/domain/shared/event/AbstractDomainEvent.java`
- Modify: `backend/src/main/java/com/school/management/domain/task/event/TaskDomainEvent.java`
- Modify: `backend/src/main/java/com/school/management/domain/rating/event/RatingDomainEvent.java`
- Test: `backend/src/test/java/com/school/management/domain/shared/event/AbstractDomainEventTest.java`

**Step 1: 写测试**

```java
// backend/src/test/java/com/school/management/domain/shared/event/AbstractDomainEventTest.java
package com.school.management.domain.shared.event;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class AbstractDomainEventTest {

    @Test
    @DisplayName("事件ID应该是唯一的UUID")
    void eventIdShouldBeUniqueUUID() {
        TestEvent event1 = new TestEvent("aggregate-1");
        TestEvent event2 = new TestEvent("aggregate-1");

        assertNotNull(event1.getEventId());
        assertNotNull(event2.getEventId());
        assertNotEquals(event1.getEventId(), event2.getEventId());
    }

    @Test
    @DisplayName("occurredOn应该接近当前时间")
    void occurredOnShouldBeNearCurrentTime() {
        LocalDateTime before = LocalDateTime.now().minusSeconds(1);
        TestEvent event = new TestEvent("aggregate-1");
        LocalDateTime after = LocalDateTime.now().plusSeconds(1);

        assertTrue(event.occurredOn().isAfter(before));
        assertTrue(event.occurredOn().isBefore(after));
    }

    @Test
    @DisplayName("aggregateId应该正确返回")
    void aggregateIdShouldBeCorrect() {
        TestEvent event = new TestEvent("my-aggregate-123");
        assertEquals("my-aggregate-123", event.aggregateId());
    }

    @Test
    @DisplayName("eventType应该返回类名")
    void eventTypeShouldReturnClassName() {
        TestEvent event = new TestEvent("aggregate-1");
        assertEquals("TestEvent", event.eventType());
    }

    // 测试用的具体实现类
    static class TestEvent extends AbstractDomainEvent {
        public TestEvent(String aggregateId) {
            super("TestAggregate", aggregateId);
        }
    }
}
```

**Step 2: 运行测试验证失败**

Run: `cd backend && mvn test -Dtest=AbstractDomainEventTest -q`
Expected: FAIL

**Step 3: 实现统一基类**

```java
// backend/src/main/java/com/school/management/domain/shared/event/AbstractDomainEvent.java
package com.school.management.domain.shared.event;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

/**
 * 领域事件抽象基类
 * 所有领域事件都应该继承此类，避免重复代码
 */
public abstract class AbstractDomainEvent implements DomainEvent {

    private final String eventId;
    private final LocalDateTime occurredOn;
    private final String aggregateType;
    private final String aggregateId;

    protected AbstractDomainEvent(String aggregateType, String aggregateId) {
        this.eventId = UUID.randomUUID().toString();
        this.occurredOn = LocalDateTime.now();
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
    }

    protected AbstractDomainEvent(String aggregateType, Long aggregateId) {
        this(aggregateType, aggregateId != null ? aggregateId.toString() : null);
    }

    @Override
    public String getEventId() {
        return eventId;
    }

    @Override
    public LocalDateTime occurredOn() {
        return occurredOn;
    }

    @Override
    public String aggregateId() {
        return aggregateId;
    }

    @Override
    public String getAggregateType() {
        return aggregateType;
    }

    @Override
    public String eventType() {
        return this.getClass().getSimpleName();
    }

    @Override
    public Instant getOccurredAt() {
        return occurredOn.atZone(ZoneId.systemDefault()).toInstant();
    }
}
```

**Step 4: 运行测试验证通过**

Run: `cd backend && mvn test -Dtest=AbstractDomainEventTest -q`
Expected: PASS

**Step 5: 重构 TaskDomainEvent 继承新基类**

```java
// backend/src/main/java/com/school/management/domain/task/event/TaskDomainEvent.java
package com.school.management.domain.task.event;

import com.school.management.domain.shared.event.AbstractDomainEvent;
import lombok.Getter;

/**
 * 任务领域事件基类
 */
@Getter
public abstract class TaskDomainEvent extends AbstractDomainEvent {

    private final Long taskId;
    private final String taskCode;

    protected TaskDomainEvent(Long taskId, String taskCode) {
        super("Task", taskId);
        this.taskId = taskId;
        this.taskCode = taskCode;
    }
}
```

**Step 6: 重构 RatingDomainEvent 继承新基类**

```java
// backend/src/main/java/com/school/management/domain/rating/event/RatingDomainEvent.java
package com.school.management.domain.rating.event;

import com.school.management.domain.shared.event.AbstractDomainEvent;
import lombok.Getter;

/**
 * 评比领域事件基类
 */
@Getter
public abstract class RatingDomainEvent extends AbstractDomainEvent {

    private final Long resultId;
    private final Long ratingConfigId;
    private final Long classId;

    protected RatingDomainEvent(Long resultId, Long ratingConfigId, Long classId) {
        super("RatingResult", resultId);
        this.resultId = resultId;
        this.ratingConfigId = ratingConfigId;
        this.classId = classId;
    }
}
```

**Step 7: 运行所有事件相关测试**

Run: `cd backend && mvn test -Dtest="*Event*Test" -q`
Expected: PASS

**Step 8: 提交**

```bash
git add backend/src/main/java/com/school/management/domain/shared/event/AbstractDomainEvent.java
git add backend/src/main/java/com/school/management/domain/task/event/TaskDomainEvent.java
git add backend/src/main/java/com/school/management/domain/rating/event/RatingDomainEvent.java
git add backend/src/test/java/com/school/management/domain/shared/event/AbstractDomainEventTest.java
git commit -m "refactor: unify domain event base class

- Create AbstractDomainEvent as single base class
- Refactor TaskDomainEvent to extend AbstractDomainEvent
- Refactor RatingDomainEvent to extend AbstractDomainEvent
- Eliminate duplicate event ID and timestamp logic"
```

---

## Task 2.2: 规范 V2 Controller Bean 命名

**Files:**
- Modify: `backend/src/main/java/com/school/management/interfaces/rest/access/RoleController.java`
- Modify: `backend/src/main/java/com/school/management/interfaces/rest/access/PermissionController.java`
- Modify: `backend/src/main/java/com/school/management/interfaces/rest/organization/OrgUnitController.java`
- Modify: `backend/src/main/java/com/school/management/interfaces/rest/organization/GradeController.java`
- Modify: `backend/src/main/java/com/school/management/interfaces/rest/organization/SchoolClassController.java`

**Step 1: 为所有 V2 Controller 添加显式 Bean 名称**

```java
// 为每个 V2 Controller 添加 @RestController("v2XxxController") 注解

// RoleController.java
@RestController("v2RoleController")
@RequestMapping("/v2/roles")
public class RoleController { ... }

// PermissionController.java
@RestController("v2PermissionController")
@RequestMapping("/v2/permissions")
public class PermissionController { ... }

// OrgUnitController.java
@RestController("v2OrgUnitController")
@RequestMapping("/v2/org-units")
public class OrgUnitController { ... }

// GradeController.java
@RestController("v2GradeController")
@RequestMapping("/v2/grades")
public class GradeController { ... }

// SchoolClassController.java
@RestController("v2SchoolClassController")
@RequestMapping("/v2/organization/classes")
public class SchoolClassController { ... }
```

**Step 2: 编译验证无冲突**

Run: `cd backend && mvn compile -q`
Expected: BUILD SUCCESS

**Step 3: 提交**

```bash
git add backend/src/main/java/com/school/management/interfaces/rest/
git commit -m "refactor: standardize V2 controller bean names

- Add explicit bean names to all V2 controllers
- Use 'v2XxxController' naming convention
- Prevent bean naming conflicts with V1 controllers"
```

---

## Task 2.3: 规范 V2 Mapper Bean 命名

**Files:**
- Modify: All mappers in `backend/src/main/java/com/school/management/infrastructure/persistence/`

**Step 1: 为所有 DDD Mapper 添加 @Repository 注解**

```java
// 示例：DddRoleMapper.java
@Mapper
@Repository("v2RoleMapper")
public interface DddRoleMapper extends BaseMapper<RolePO> { ... }

// 示例：DddPermissionMapper.java
@Mapper
@Repository("v2PermissionMapper")
public interface DddPermissionMapper extends BaseMapper<PermissionPO> { ... }

// 对所有 infrastructure/persistence/ 下的 Mapper 执行相同操作
```

**Step 2: 验证编译通过**

Run: `cd backend && mvn compile -q`
Expected: BUILD SUCCESS

**Step 3: 提交**

```bash
git add backend/src/main/java/com/school/management/infrastructure/persistence/
git commit -m "refactor: standardize V2 mapper bean names

- Add @Repository annotation with explicit names to all V2 mappers
- Use 'v2XxxMapper' naming convention
- Prevent bean naming conflicts with V1 mappers"
```

---

## Task 2.4: 为 V1 代码添加 @Deprecated 标注

**Files:**
- Modify: All controllers in `backend/src/main/java/com/school/management/controller/`

**Step 1: 为 V1 Controller 添加 @Deprecated 注解和迁移说明**

```java
// 示例：controller/RoleController.java

/**
 * @deprecated 此控制器将在 v2.0 版本移除
 * 请迁移至 {@link com.school.management.interfaces.rest.access.RoleController}
 * 新 API 路径: /v2/roles
 */
@Deprecated(since = "1.5.0", forRemoval = true)
@RestController
@RequestMapping("/roles")
public class RoleController { ... }
```

**Step 2: 对所有 V1 Controller 执行相同操作**

需要标记的控制器列表:
- `RoleController.java`
- `PermissionController.java`
- `DepartmentController.java`
- `ClassController.java`
- 其他已有 V2 替代版本的控制器

**Step 3: 提交**

```bash
git add backend/src/main/java/com/school/management/controller/
git commit -m "deprecate: mark V1 controllers for removal

- Add @Deprecated annotation to all V1 controllers with V2 replacements
- Include migration path in Javadoc
- Set forRemoval=true for v2.0 release"
```

---

## Task 2.5: 创建 V1 到 V2 API 映射文档

**Files:**
- Create: `docs/migration/v1-to-v2-api-mapping.md`

**Step 1: 创建 API 映射文档**

```markdown
# V1 到 V2 API 迁移指南

## 概述

本项目正在从 V1 (传统分层架构) 迁移到 V2 (DDD 架构)。
V1 API 将在 v2.0.0 版本完全移除。

## API 映射表

### 角色管理

| V1 路径 | V2 路径 | 状态 |
|---------|---------|------|
| `GET /roles` | `GET /v2/roles` | ✅ 已迁移 |
| `POST /roles` | `POST /v2/roles` | ✅ 已迁移 |
| `PUT /roles/{id}` | `PUT /v2/roles/{id}` | ✅ 已迁移 |
| `DELETE /roles/{id}` | `DELETE /v2/roles/{id}` | ✅ 已迁移 |

### 权限管理

| V1 路径 | V2 路径 | 状态 |
|---------|---------|------|
| `GET /permissions` | `GET /v2/permissions` | ✅ 已迁移 |
| `POST /permissions` | `POST /v2/permissions` | ✅ 已迁移 |

### 组织架构

| V1 路径 | V2 路径 | 状态 |
|---------|---------|------|
| `GET /departments` | `GET /v2/org-units` | ✅ 已迁移 |
| `GET /classes` | `GET /v2/organization/classes` | ✅ 已迁移 |

### 量化检查

| V1 路径 | V2 路径 | 状态 |
|---------|---------|------|
| `GET /check/templates` | `GET /v2/inspection-templates` | ✅ 已迁移 |
| `GET /check/records` | `GET /v2/inspection-records` | ✅ 已迁移 |

## 迁移步骤

### 后端迁移

1. 确认 V2 API 功能完整性
2. 更新前端 API 调用
3. 验证所有功能正常
4. 移除 V1 API 代码

### 前端迁移

1. 更新 `src/api/v2/` 目录下的 API 定义
2. 修改组件中的 API 调用
3. 运行测试验证功能

## 时间线

- **v1.5.0** (当前): V1 和 V2 并行运行，V1 标记为 @Deprecated
- **v1.8.0**: V2 功能完整，开始迁移前端
- **v2.0.0**: 移除所有 V1 代码
```

**Step 2: 提交**

```bash
git add docs/migration/v1-to-v2-api-mapping.md
git commit -m "docs: add V1 to V2 API migration guide

- Document all API endpoint mappings
- Include migration steps for backend and frontend
- Define migration timeline"
```

---

## Task 2.6-2.12: 其他架构规范化任务

[由于篇幅限制，以下任务采用简化格式]

### Task 2.6: 创建 DTO 转换器基础框架
- 使用 MapStruct 创建统一的 DTO 转换器
- 替换手写的 toResponse/toDTO 方法

### Task 2.7: 分离 InspectionApplicationService
- 拆分为 InspectionTemplateService、InspectionRecordService、InspectionAppealService

### Task 2.8: 统一 Repository 接口设计
- 确保所有 Repository 方法都有高效实现
- 移除内存过滤的低效实现

### Task 2.9: 修复 Role 三模型问题
- 明确 V1 Entity 只读用途
- 确保新功能只使用 V2 Domain Model

### Task 2.10: 规范 Response DTO 不暴露 Domain Model
- 将 RoleType、DataScope 等转换为基本类型

### Task 2.11: 添加架构测试 (ArchUnit)
- 确保层级依赖正确
- 确保命名规范

### Task 2.12: 更新前端 API 版本
- 将关键模块切换到 V2 API

---

# 阶段三：测试覆盖提升（3周）

## Task 3.1: 配置测试覆盖率门槛

**Files:**
- Modify: `backend/pom.xml`

**Step 1: 提高 JaCoCo 覆盖率要求**

```xml
<!-- backend/pom.xml 约 Line 340 -->
<limit>
    <counter>LINE</counter>
    <value>COVEREDRATIO</value>
    <minimum>0.50</minimum>  <!-- 从 0.10 提升到 0.50 -->
</limit>

<!-- 添加分支覆盖率要求 -->
<limit>
    <counter>BRANCH</counter>
    <value>COVEREDRATIO</value>
    <minimum>0.40</minimum>
</limit>
```

**Step 2: 添加排除规则 (配置类、DTO等不需要测试)**

```xml
<configuration>
    <excludes>
        <exclude>**/config/**</exclude>
        <exclude>**/dto/**</exclude>
        <exclude>**/entity/**</exclude>
        <exclude>**/*PO.class</exclude>
        <exclude>**/*DTO.class</exclude>
        <exclude>**/*Request.class</exclude>
        <exclude>**/*Response.class</exclude>
    </excludes>
</configuration>
```

**Step 3: 提交**

```bash
git add backend/pom.xml
git commit -m "test: raise code coverage threshold to 50%

- Increase line coverage minimum from 10% to 50%
- Add branch coverage requirement of 40%
- Exclude config, DTO, entity classes from coverage"
```

---

## Task 3.2: 为 Application Services 添加测试 (重点)

### Task 3.2.1: InspectionApplicationService 测试

**Files:**
- Create: `backend/src/test/java/com/school/management/application/inspection/InspectionApplicationServiceTest.java`

**测试要点:**
- createTemplate() - 创建检查模板
- publishTemplate() - 发布模板
- createRecord() - 创建检查记录
- submitAppeal() - 提交申诉
- approveAppeal() - 审批申诉

### Task 3.2.2: GradeApplicationService 测试
### Task 3.2.3: StudentApplicationService 测试
### Task 3.2.4: SemesterApplicationService 测试

---

## Task 3.3: 为 Event Handlers 添加测试

**Files:**
- Create: `backend/src/test/java/com/school/management/application/events/InspectionEventHandlerTest.java`
- Create: `backend/src/test/java/com/school/management/application/events/TaskEventHandlerTest.java`

**测试要点:**
- 事件正确处理
- 异常情况不影响主流程
- 通知正确发送

---

## Task 3.4: 为 REST Controllers 添加集成测试

**Files:**
- Create: `backend/src/test/java/com/school/management/interfaces/rest/organization/OrgUnitControllerIntegrationTest.java`
- Create: `backend/src/test/java/com/school/management/interfaces/rest/access/RoleControllerIntegrationTest.java`

**测试要点:**
- 使用 @WebMvcTest 或 @SpringBootTest
- 测试权限验证 (@PreAuthorize)
- 测试请求参数验证
- 测试错误响应

---

## Task 3.5: 添加 Repository 集成测试

**Files:**
- Create: `backend/src/test/java/com/school/management/infrastructure/persistence/access/RoleRepositoryImplIntegrationTest.java`

**测试要点:**
- 使用 @DataJpaTest 或嵌入式数据库
- 测试 CRUD 操作
- 测试复杂查询

---

## Task 3.6-3.15: 其他测试任务

- Task 3.6: 添加前端单元测试 (Vitest)
- Task 3.7: 添加前端 E2E 测试 (Playwright)
- Task 3.8: 添加工作流集成测试 (Flowable)
- Task 3.9: 添加 Redis 集成测试
- Task 3.10: 添加安全测试 (权限绕过)
- Task 3.11: 添加性能测试基准
- Task 3.12: 配置 CI 测试流水线
- Task 3.13: 添加测试数据工厂
- Task 3.14: 添加测试覆盖率报告
- Task 3.15: 编写测试指南文档

---

# 阶段四：代码质量改进（2周）

## Task 4.1: 消除通配符导入

**Files:**
- Modify: 所有使用 `import xxx.*` 的文件

**工具:**
```bash
# 查找所有通配符导入
grep -r "import .*\.\*;" backend/src/main/java/ --include="*.java"
```

**Step 1: 使用 IDE 自动优化导入**

在 IntelliJ IDEA 中:
- Settings → Editor → Code Style → Java → Imports
- 设置 "Class count to use import with '*'" 为 999
- 对每个文件执行 "Optimize Imports" (Ctrl+Alt+O)

**Step 2: 提交**

```bash
git add backend/src/main/java/
git commit -m "style: replace wildcard imports with explicit imports"
```

---

## Task 4.2: 提取共享 ObjectMapper Bean

**Files:**
- Create: `backend/src/main/java/com/school/management/config/JacksonConfig.java`
- Modify: 所有 `new ObjectMapper()` 的地方

**Step 1: 创建配置类**

```java
@Configuration
public class JacksonConfig {

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }
}
```

**Step 2: 替换所有 new ObjectMapper()**

```java
// 修改前
ObjectMapper mapper = new ObjectMapper();

// 修改后
@Autowired
private ObjectMapper objectMapper;
```

---

## Task 4.3: 修复异常处理

**Files:**
- Modify: 所有 `catch (Exception e)` 的地方

**规范:**
```java
// 修改前
catch (Exception e) {
    log.warn("Error: {}", e.getMessage());
}

// 修改后
catch (IOException | DataAccessException e) {
    log.warn("Failed to process: {}", context, e);  // 包含堆栈
    throw new BusinessException("Operation failed", e);
}
```

---

## Task 4.4: 添加领域层日志

**Files:**
- Modify: Domain model 中的关键业务方法

**规范:**
```java
@Slf4j
public class InspectionTemplate {

    public void publish() {
        log.info("Publishing inspection template: id={}, code={}", id, templateCode);
        // 业务逻辑
        log.debug("Template published successfully: version={}", currentVersion);
    }
}
```

---

## Task 4.5: 提取魔法数字为常量

**Files:**
- Create: `backend/src/main/java/com/school/management/domain/shared/DomainConstants.java`
- Modify: 所有硬编码数字的地方

**Step 1: 创建常量类**

```java
public final class DomainConstants {

    private DomainConstants() {}

    // 系统用户ID
    public static final Long SYSTEM_ADMIN_ID = 1L;

    // 字段长度限制
    public static final int CODE_MAX_LENGTH = 50;
    public static final int NAME_MAX_LENGTH = 100;
    public static final int DESCRIPTION_MAX_LENGTH = 500;

    // 业务规则
    public static final int MAX_LOGIN_ATTEMPTS = 5;
    public static final int LOCK_DURATION_MINUTES = 30;
}
```

---

## Task 4.6-4.10: 其他代码质量任务

- Task 4.6: 完善 Javadoc 注释
- Task 4.7: 添加 null 检查 (@NonNull, Objects.requireNonNull)
- Task 4.8: 分解过长方法 (>100行)
- Task 4.9: 添加 SonarQube 配置
- Task 4.10: 编写代码规范文档

---

# 执行检查点

## 阶段一完成检查

- [ ] 启动应用无安全警告
- [ ] Druid 监控页面无法访问
- [ ] 使用默认密码启动失败
- [ ] 所有安全测试通过

## 阶段二完成检查

- [ ] 无 Bean 冲突警告
- [ ] V1 API 标记为 @Deprecated
- [ ] V2 API 功能完整
- [ ] 架构测试通过

## 阶段三完成检查

- [ ] 测试覆盖率 ≥50%
- [ ] 所有 Application Service 有测试
- [ ] 所有 Event Handler 有测试
- [ ] CI 流水线测试通过

## 阶段四完成检查

- [ ] 无 Checkstyle 警告
- [ ] 无 SpotBugs 警告
- [ ] SonarQube 质量门通过
- [ ] 代码审查通过

---

# 附录

## A. 命令速查

```bash
# 运行所有测试
cd backend && mvn test

# 生成覆盖率报告
cd backend && mvn jacoco:report

# 代码质量检查
cd backend && mvn verify -Pcode-quality

# 查看依赖漏洞
cd backend && mvn dependency-check:check
```

## B. 相关文档

- [安全检查清单](../security/security-checklist.md)
- [V1 到 V2 迁移指南](../migration/v1-to-v2-api-mapping.md)
- [测试规范](../development/testing-guide.md)
- [代码规范](../development/coding-standards.md)
