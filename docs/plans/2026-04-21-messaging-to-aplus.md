# 消息与事件子系统 → A+ Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 把消息与事件子系统从 B- (67/100) 推到真 A+, 补齐 3 Critical + 4 Important + 3 Minor + 前端 UI 升级, 并与 A+ core 三大新扩展点 (Policy SPI / AccessRelation implied / DataScope 动态维度) 完整对齐。

**Architecture:**
- `TriggerService.fire(code, ctx)` 为唯一事件入口, 启动期自检保证 `trigger_points`/`entity_event_types`/`event_triggers`/`msg_subscription_rules` 4 表配置就绪。
- BY_RELATION 目标解析走 `AuthorizationService.expandSubjects()` (使用 W4 BFS implied 展开), 而非裸 SQL。
- target_mode 从硬编码 switch 改为 `TargetModeResolver` SPI, 允许插件贡献。
- 旧 `MessagingDomainPlugin` 彻底拆成 3 个 sealed `Contribution` permit, 消除双轨制 (之前 Phase 7.2 跳过, 本计划补)。

**Tech Stack:** Spring Boot 3.2, MyBatis Plus, Java 17 sealed/records, ArchUnit, JUnit 5, Vue 3, Element Plus。

**预估工作量**: 5 tracks × 1-3d = **10-13d** 全职 / 3-4 周 × 2-3d/周。

---

## Track 总览

| Track | 内容 | 天数 | 关键 commit |
|---|---|---:|---|
| M1 | 3 Critical 修 (启动自检 + BY_RELATION 走 AuthService + 消息查询加 tenant/DataScope) | 2d | `fix(messaging): critical protections` |
| M2 | `target_mode` SPI 化 (TargetModeResolver sealed permit 新增) | 2d | `feat(messaging): TargetModeResolver SPI` |
| M3 | 旧 `MessagingDomainPlugin` → 3 Contribution 拆分 (Phase 7.2 补) | 3d | `refactor(messaging): 拆旧 SPI 为 3 Contribution` |
| M4 | `ConditionMatcher` 接入 SpEL + DLQ 死信 + 自动幂等 key + Policy→Notification 接线 + 集成测试 | 3d | 多 commit |
| M5 | 前端 UI 升级: 触发点目录 / 事件类型浏览 / MessageConfigView 增强 | 2d | `feat(frontend/messaging)` |

**执行顺序**: M1 → M2 → M3 → M4 → M5。M1 是止血, M5 是收尾。

---

# Track M1 — 3 Critical 修 (2d)

## Task M1.1: TriggerService 启动自检

**Files:**
- Create: `backend/src/main/java/com/school/management/application/event/TriggerPipelineHealthCheck.java`
- Modify: `backend/src/main/java/com/school/management/application/event/TriggerService.java` (加 `triggerPipelineReady` 标志)

**Step 1: 写失败测试**

Create `backend/src/test/java/com/school/management/application/event/TriggerPipelineHealthCheckTest.java`:

```java
@Test
void reportsMissingWhenTablesEmpty() {
    // mock JdbcTemplate 返回各表 count=0
    // 期望: health.check() 返回 UNHEALTHY + 列出缺失表
}

@Test
void reportsHealthyWhenAllTablesPopulated() {
    // mock 各表 count > 0
    // 期望: health.check() 返回 HEALTHY
}

@Test
void logsErrorOnStartupWhenUnhealthy() {
    // 触发 ApplicationReadyEvent, 验证 log.error 被调
}
```

**Step 2: 实现**

```java
package com.school.management.application.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 启动时检查事件/消息配置表完整性.
 *
 * 4 张表任意为空都会让 triggerService.fire() 静默 no-op:
 *   - trigger_points          (插件声明的触发点码)
 *   - entity_event_types      (事件类型目录)
 *   - event_triggers          (触发器配置, 把触发点连到事件类型)
 *   - msg_subscription_rules  (订阅规则, 决定谁收通知)
 *
 * 本类 @EventListener(ApplicationReadyEvent) 启动末尾跑一遍, 缺就 ERROR 日志 + 设置
 * triggerPipelineReady=false, 业务查询可以读这个标志决定是否退化.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class TriggerPipelineHealthCheck {

    private final JdbcTemplate jdbc;
    private volatile boolean healthy = false;
    private volatile List<String> missingTables = List.of();

    public boolean isHealthy() { return healthy; }
    public List<String> getMissingTables() { return missingTables; }

    @EventListener(ApplicationReadyEvent.class)
    public void checkOnStartup() {
        Map<String, String> checks = Map.of(
            "trigger_points",         "SELECT COUNT(*) FROM trigger_points WHERE is_enabled=1",
            "entity_event_types",     "SELECT COUNT(*) FROM entity_event_types WHERE is_enabled=1 AND deleted=0",
            "event_triggers",         "SELECT COUNT(*) FROM event_triggers WHERE is_enabled=1 AND deleted=0",
            "msg_subscription_rules", "SELECT COUNT(*) FROM msg_subscription_rules WHERE is_enabled=1"
        );

        List<String> missing = new ArrayList<>();
        for (var e : checks.entrySet()) {
            try {
                Long n = jdbc.queryForObject(e.getValue(), Long.class);
                if (n == null || n == 0) {
                    missing.add(e.getKey());
                }
            } catch (Exception ex) {
                missing.add(e.getKey() + " (query failed: " + ex.getMessage() + ")");
            }
        }

        this.missingTables = List.copyOf(missing);
        this.healthy = missing.isEmpty();

        if (!healthy) {
            log.error("[TriggerPipelineHealthCheck] ❌ 消息/事件管道不完整, 以下表空或缺失: {}" +
                     " — triggerService.fire() 将静默 no-op, 业务消息推送全盲! " +
                     "检查 migration (V97/V98/...) 是否 apply", missingTables);
        } else {
            log.info("[TriggerPipelineHealthCheck] ✅ 消息/事件管道就绪");
        }
    }
}
```

**Step 3: TriggerService.fire() 增加 health 检查分支**

Modify `TriggerService.java:54-80` (fire 方法入口):
```java
if (!healthCheck.isHealthy()) {
    log.warn("[TriggerService] fire({}) skipped — pipeline unhealthy, missing: {}",
        triggerPointCode, healthCheck.getMissingTables());
    return 0;
}
```

**Step 4: 运行**

```bash
cd backend
mvn test -Dtest=TriggerPipelineHealthCheckTest
```
Expected: 3/3 PASS

**Step 5: Commit**

```bash
git commit -m "fix(messaging): TriggerPipelineHealthCheck — 启动自检 4 表就绪 (M1.1)"
```

---

## Task M1.2: BY_RELATION 走 AuthorizationService

**Files:**
- Modify: `backend/src/main/java/com/school/management/application/message/MessageDispatcher.java:216-270` (resolveByRelation 方法)
- Modify: 构造注入 `AuthorizationService`

**Step 1: 定位现有 resolveByRelation**

```bash
grep -n "resolveByRelation\|BY_RELATION" backend/src/main/java/com/school/management/application/message/MessageDispatcher.java
```

Read 该方法全文, 看它当前怎么查 access_relations。

**Step 2: 加 AuthorizationService 依赖注入**

```java
@Component
@RequiredArgsConstructor
public class MessageDispatcher {
    private final AuthorizationService authorizationService;
    // ... existing
}
```

**Step 3: 改 resolveByRelation 调用 authService API**

```java
private List<Long> resolveByRelation(String relation, String resourceType, Long resourceId,
                                      String direction /* INWARD/OUTWARD */) {
    // 旧: jdbc.queryForList("SELECT subject_id FROM access_relations WHERE ...")
    // 新: authService 展开 implied, 含级联 admin/viewer
    if ("INWARD".equals(direction)) {
        // 找谁对此 resource 有 relation 关系 (含 implied)
        return authorizationService.findSubjectsWithRelation(
            resourceType, resourceId, relation, /* expandImplied= */ true);
    } else {
        // OUTWARD: 给定 subject 找它有关系的 resources
        // ...
    }
}
```

**注意**: 如果 `AuthorizationService` 没有 `findSubjectsWithRelation(resourceType, resourceId, relation, expandImplied)` 方法, 加一个。它应该:
1. 基础: `SELECT subject_id FROM access_relations WHERE resource_type=? AND resource_id=? AND relation=?`
2. implied 展开: 对每个 `(X.resourceType, relation) → [ImpliedTarget]` 反向查, 找哪些 source 关系能派生到本 relation, 递归往上找 subject

**Step 4: 集成测试**

写一个测试: 用户 A 是 place=1 的 `manages`, place=1 里 occupant 是 user=2/3/4. 发消息 target=BY_RELATION(viewer, user=2), 预期 user A 收到 (因为 manages→viewer on user via OCCUPANTS_OF_PLACE).

```bash
mvn test -Dtest='MessageDispatcherImpliedTest'
```

**Step 5: Commit**

```bash
git commit -m "fix(messaging): BY_RELATION 走 AuthorizationService implied 展开 (M1.2)"
```

---

## Task M1.3: 消息查询加 tenant_id + DataScope

**Files:**
- Modify: `backend/src/main/java/com/school/management/infrastructure/persistence/message/MsgNotificationMapper.java` (或 XML 等效)
- Modify: MessageQueryService (或等效的查询应用服务), 加 `@DataPermission` 注解

**Step 1: 查现有 findByUserId**

```bash
grep -rn "findByUserId\|selectByUser\|msg_notifications" backend/src/main/java/com/school/management/infrastructure/persistence/message/ backend/src/main/resources/mapper/ 2>&1
```

**Step 2: 加 tenant_id 过滤到所有 msg_notifications 查询**

所有 msg_notifications 表查询必须带 `AND tenant_id = ?`, 从 `TenantContext.current()` 读取。

如果没有 `tenant_id` 列? 看 DDL。如果缺, 加 migration V20260422_1 加列:
```sql
ALTER TABLE msg_notifications ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1;
CREATE INDEX idx_tenant ON msg_notifications(tenant_id, user_id);
```

**Step 3: ApplicationService 方法加 `@DataPermission`**

例:
```java
@DataPermission(resourceType = "msg_notification")
public Page<MsgNotification> listMyNotifications(Long userId, Page<?> page) { ... }
```

需要在 `data_resources` 表加 `msg_notification` 条目 (如果还没)。

**Step 4: 回归测试**

保证既有 `MessageQueryServiceTest` 不破。

**Step 5: Commit**

```bash
git commit -m "fix(messaging): msg_notifications 加 tenant_id 过滤 + @DataPermission (M1.3)"
```

---

# Track M2 — TargetModeResolver SPI (2d)

## Task M2.1: 定义 SPI 接口

**Files:**
- Create: `backend/src/main/java/com/school/management/infrastructure/extension/TargetModeResolver.java`
- Modify: `backend/src/main/java/com/school/management/infrastructure/extension/Contribution.java` (sealed 10→11 permits)

```java
// TargetModeResolver.java
package com.school.management.infrastructure.extension;

import java.util.List;
import java.util.Map;

/**
 * 消息推送目标模式解析 SPI — 插件可贡献新的 target_mode.
 *
 * 当前 core 内置 4 种: BY_SUBJECT / BY_ROLE / BY_RELATION / BY_FEATURE.
 * 行业可加如 BY_DEPARTMENT / BY_GRADE / BY_WARD 等, 无需改 MessageDispatcher.
 */
public interface TargetModeResolver {
    /** 模式码 (全局唯一, 如 BY_DEPARTMENT) */
    String modeCode();
    /** 人类可读名 */
    String displayName();
    /**
     * 根据 rule 配置解析到用户 id 列表.
     * @param config  规则配置 JSON (e.g. {"deptId": 123})
     * @param event   触发事件上下文 (含 payload)
     * @return user id list
     */
    List<Long> resolve(Map<String, Object> config, Map<String, Object> event);
}
```

Modify Contribution.java 加 permit:
```java
record TargetModeResolverContribution(TargetModeResolver resolver) implements Contribution {
    @Override public String uniqueKey() { return "target-mode:" + resolver.modeCode(); }
}
```

更新 `UnifiedPluginPackageTest` 断言 10→11。

**Commit**: `feat(extension): TargetModeResolver SPI (M2.1)`

---

## Task M2.2: MessageDispatcher 改用 SPI dispatch

**Files:**
- Modify: `backend/src/main/java/com/school/management/application/message/MessageDispatcher.java` (switch → Map 查找)

**Step 1: 注入 `List<TargetModeResolver> resolvers` + 构造 Map**

```java
@PostConstruct
void initResolverMap() {
    resolverByMode = resolvers.stream()
        .collect(Collectors.toMap(TargetModeResolver::modeCode, Function.identity()));
    log.info("[MessageDispatcher] 加载 target mode resolvers: {}", resolverByMode.keySet());
}
```

**Step 2: 替换硬编码 switch**

```java
// 旧
switch (targetMode) {
    case "BY_SUBJECT": return resolveBySubject(...);
    case "BY_ROLE":    return resolveByRole(...);
    case "BY_RELATION":return resolveByRelation(...);
    case "BY_FEATURE": return resolveByFeature(...);
}

// 新
TargetModeResolver r = resolverByMode.get(targetMode);
if (r == null) {
    log.warn("[MessageDispatcher] unknown target_mode {}, skip", targetMode);
    return List.of();
}
return r.resolve(rule.getTargetConfig(), event);
```

**Step 3: 把 core 的 4 模式拆成 4 个 @Component 实现 TargetModeResolver**

Create:
- `BySubjectTargetMode.java`
- `ByRoleTargetMode.java`
- `ByRelationTargetMode.java` (内部调 AuthorizationService 走 implied 展开, M1.2 已接入)
- `ByFeatureTargetMode.java`

全部放在 `application/message/targetmode/` 目录。

**Commit**: `refactor(messaging): MessageDispatcher 用 TargetModeResolver SPI (M2.2)`

---

## Task M2.3: ArchUnit 守护 — 禁止 core MessageDispatcher 硬编码 target_mode

**Create**: `backend/src/test/java/com/school/management/architecture/NoHardcodedTargetModeTest.java`

扫 `MessageDispatcher.java` 源码, 禁止出现 `"BY_SUBJECT"` / `"BY_ROLE"` / `"BY_RELATION"` / `"BY_FEATURE"` 字符串 literal (必须通过 SPI 查)。

**Commit**: `test(arch): NoHardcodedTargetModeTest 守护 (M2.3)`

---

# Track M3 — 拆 MessagingDomainPlugin (3d)

**背景**: `MessagingDomainPlugin` 一 SPI 声明 (触发点 + 事件类型 + 默认触发器) 3 种东西, 耦合高。Phase 7.2 曾计划拆 3 个 Contribution 但跳过了。本 track 补。

## Task M3.1: 新增 3 个 Contribution permits

**Files:**
- Modify: `Contribution.java` (11 → 13 permits 加 2 个)

```java
record TriggerPointContribution(String domainCode, MessagingDomainPlugin.TriggerPointDef def) implements Contribution {
    @Override public String uniqueKey() { return "trigger-point:" + def.code(); }
}

record EventTypeContribution(String domainCode, MessagingDomainPlugin.EventTypeDef def) implements Contribution {
    @Override public String uniqueKey() { return "event-type:" + def.code(); }
}

// EventDomainContribution (旧的) 保留 — 它代表一个"域包", 含 N 个 TriggerPoint + N 个 EventType + N 个 Default Trigger
// 但插件可以只用细粒度的 TriggerPointContribution + EventTypeContribution, 不必全打包
```

Update `UnifiedPluginPackageTest` assert count.

**Commit**: `feat(extension): TriggerPointContribution + EventTypeContribution permits (M3.1)`

---

## Task M3.2: ContributionDispatcher 处理新 permits

Modify `ContributionDispatcher.java` instanceof 链加 2 case, 分别 UPSERT 到 `trigger_points` / `entity_event_types` 表。与既有 `MessagingRegistrar` 共用 upsert 方法。

**Commit**: `feat(extension): ContributionDispatcher 接入新 messaging permits (M3.2)`

---

## Task M3.3: 挑 2 个插件迁移做 reference

选 `PatientAdmissionMessagingPlugin` + `DormitoryMessagingPlugin` 作迁移示范: 从 `implements MessagingDomainPlugin` 改为 `implements PluginPackage` 返回 `Stream<Contribution>`。

其他 11 个插件保留旧 API (@Deprecated 能工作不变), **但不再允许新写旧 API** (ArchUnit 守护)。

**Files:** 2 个 plugin + 1 ArchUnit test

**Commit**: `refactor(messaging): HEALTH+EDU 2 插件迁 Contribution 模式 (M3.3)`

---

## Task M3.4: ArchUnit — 禁止新增 MessagingDomainPlugin 实现

Create `NoNewMessagingDomainPluginTest`: 扫 `plugins/` 下 `implements MessagingDomainPlugin` 的类数量, 超过基线 (如 12, 基线固化) 就 fail。

**Commit**: `test(arch): 禁止新增 MessagingDomainPlugin 实现 (M3.4)`

---

# Track M4 — 条件表达式 + DLQ + 幂等 + Policy 接线 + 集成测试 (3d)

## Task M4.1: ConditionMatcher 接入 SpEL

**Files:**
- Modify: `backend/src/main/java/com/school/management/application/event/ConditionMatcher.java`

加入 Spring SpEL 支持作为可选分支 (保留既有 JSON schema, 新增 `expression` 字段):

```java
if (rule.hasSpelExpression()) {
    ExpressionParser parser = new SpelExpressionParser();
    StandardEvaluationContext ctx = new StandardEvaluationContext();
    ctx.setVariable("event", event);
    Boolean result = parser.parseExpression(rule.getSpelExpression()).getValue(ctx, Boolean.class);
    return Boolean.TRUE.equals(result);
}
```

**安全**: SpEL 用 `SimpleEvaluationContext.forReadOnlyDataBinding()` 沙箱化, 防任意代码执行。

**Commit**: `feat(messaging): ConditionMatcher 接入 SpEL 表达式 (M4.1)`

---

## Task M4.2: DLQ + 自动幂等 key

**Files:**
- Create: `database/schema/V108.0.0__msg_notification_failures.sql` (死信表)
- Modify: `EntityEventDispatchListener` 异常捕获写 DLQ
- Modify: `TriggerService.fire` 若 `_idempotencyKey` 未传, 自动 hash(triggerPointCode + payload 关键字段) 生成

```sql
CREATE TABLE IF NOT EXISTS msg_notification_failures (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    event_id BIGINT,
    rule_id BIGINT,
    target_user_id BIGINT,
    error_message TEXT,
    retry_count INT DEFAULT 0,
    next_retry_at DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_retry (next_retry_at, retry_count)
);
```

**Commit**: `feat(messaging): DLQ 死信 + 自动幂等 key (M4.2)`

---

## Task M4.3: Policy WARN → Notification 接线

**思路**: Policy SPI 产生的 `Violation(severity=WARN)` 应该可以作为事件源, 触发消息。

**Files:**
- Modify: `PolicyRegistry.check()` 发布 `PolicyWarningEvent(policyCode, ctx, violations)` Spring event
- Create: `PolicyWarningToNotificationListener` — 监听上面 event, 调 `triggerService.fire("POLICY_WARNING", Map.of("policyCode",..., "message",...))`
- Seed: `trigger_points` 表加 `POLICY_WARNING` 一行
- 可选: seed `event_triggers` + default `msg_subscription_rules` — 管理员接收所有 policy warning

**Commit**: `feat(messaging): Policy WARN 自动转成事件触发消息推送 (M4.3)`

---

## Task M4.4: 集成测试全链路

**Files:**
- Create: `backend/src/test/java/com/school/management/application/message/FullPipelineIntegrationTest.java`

**Step 1: 用 @SpringBootTest + @Transactional 写 5 用例**

```java
@Test void firePointMatchRuleCreatesNotification() {
    // 1. triggerService.fire("INSP_ITEM_RESULT", {"studentId": 1, "score": 60})
    // 2. 断言: 对应 event_trigger 匹配, 产生 entity_event
    // 3. 对应 msg_subscription_rule (target=BY_RELATION(family_of, user)) 解析
    // 4. msg_notifications 表插入 N 条 (学生的家属们)
}

@Test void spelConditionFiltersNonMatching() {
    // rule.spel: #event.score < 60
    // fire 传 score=70, 不产生通知
}

@Test void implied 权限链产生额外收件人() {
    // manages → viewer via OCCUPANTS_OF_PLACE 应该让 place manager 收到场所相关通知
}

@Test void tenantIsolation_worksOnQuery() { ... }

@Test void dlqCapturesFailures() {
    // mock notification save throw, 验证 msg_notification_failures 插入
}
```

**Commit**: `test(messaging): 全链路 5 集成测试 (M4.4)`

---

# Track M5 — 前端 UI 升级 (2d)

## Task M5.1: 插件平台加 "触发点" + "事件类型" Tab

**Files:**
- Modify: `frontend/src/views/system/PluginManagementView.vue`
- Modify: `backend/src/main/java/com/school/management/interfaces/rest/system/PluginPlatformController.java` (加 `/trigger-points` endpoint)

**后端 endpoint**:

```java
@GetMapping("/trigger-points")
public Result<List<Map<String, Object>>> triggerPoints() {
    return Result.success(jdbc.queryForList(
        "SELECT module_code, module_name, point_code, point_name, description, context_schema, " +
        "is_enabled, (SELECT COUNT(*) FROM event_triggers et WHERE et.trigger_point_code=tp.point_code AND et.is_enabled=1) AS trigger_count " +
        "FROM trigger_points tp WHERE is_enabled=1 ORDER BY module_code, sort_order"));
}
```

**前端 stats row**: 9 → 11 卡片, 加 "触发点" + "订阅规则" (后者是消息配置数量 — 用既有 endpoint 也可)

**新 Tab "触发点"**: 表格显示 module / code / 触发器数 / context_schema 缩略, 点击展开详情

**新 Tab "订阅规则"** (可选): 列出 msg_subscription_rules 所有规则, 按目标模式分组

**Commit**: `feat(frontend/plugin-platform): 触发点 + 订阅规则 Tab (M5.1)`

---

## Task M5.2: MessageConfigView 升级 — 触发器/事件类型可视化管理

**Files:**
- Modify: `frontend/src/views/system/messaging/` 下相关 Vue

增强:
- 触发器创建时从 `/trigger-points` 下拉选 trigger_point_code (不再裸输入字符串)
- 条件表达式: 加 SpEL 模式切换 + 语法校验提示
- 目标模式: 从 `/api/plugin-platform/target-modes` (新 endpoint) 动态加载 resolver 列表 (M2 新增)
- 健康指示: 在页顶显示 TriggerPipelineHealthCheck 状态 (若 unhealthy 红色 banner)

**Commit**: `feat(frontend/messaging): MessageConfigView 配置化升级 (M5.2)`

---

## Task M5.3: 消息中心 — 类型筛选 + 来源展示

**Files:**
- Modify: `frontend/src/views/message/MessageCenter.vue` 或等效

增强:
- 消息列表加"事件类型"筛选器 (dropdown 从 entity_event_types 加载)
- 消息卡片展示"来源": event_id → 可点击跳转事件详情
- 消息操作: 已读 / 忽略 / 转发

**Commit**: `feat(frontend/message): 消息中心类型筛选 + 来源追溯 (M5.3)`

---

# Phase 完成 Gate

### M1 Gate (止血)
- [ ] `TriggerPipelineHealthCheck` 启动时打日志 + `isHealthy()` API
- [ ] BY_RELATION 走 AuthorizationService.findSubjectsWithRelation(implied=true)
- [ ] msg_notifications 所有查询含 tenant_id 过滤
- [ ] `mvn test -Dtest='*Trigger*,*Dispatch*,*Notification*'` 全绿

### M2 Gate (解耦)
- [ ] Contribution sealed 10 → 11 permits
- [ ] 4 core target mode 各为独立 @Component
- [ ] MessageDispatcher 零硬编码 target_mode 字符串 (ArchUnit 守护)

### M3 Gate (SPI 拆分)
- [ ] Contribution sealed 11 → 13 permits (加 TriggerPoint + EventType)
- [ ] 2 reference plugin 迁 Contribution 模式
- [ ] 禁止新增 MessagingDomainPlugin 实现

### M4 Gate (质量)
- [ ] SpEL 表达式支持 + 沙箱化
- [ ] DLQ 表 + retry 机制
- [ ] Policy WARN 可通过 trigger_points 转消息
- [ ] 5 集成测试绿

### M5 Gate (UI)
- [ ] 插件平台 11 卡片 (触发点 / 订阅规则)
- [ ] MessageConfigView 触发点/事件类型下拉 + SpEL 编辑器
- [ ] 消息中心类型筛选 + 来源追溯

### 总 Gate = 消息事件 A+
- [ ] `mvn verify` 全量绿 (新测试 + 既有 462 无回归)
- [ ] `npm run build` + `type-check:ceiling` 绿
- [ ] 浏览器过一遍: 触发点浏览 / 配置触发器 / 触发消息 / 用户收到

---

# 风险 + 回滚

1. **拆 MessagingDomainPlugin 风险**: 13 插件迁移工作量大 (本计划只迁 2 个 reference), 剩下 11 个留 @Deprecated 能用; 后续业务新功能逐步迁
2. **SpEL 安全**: SimpleEvaluationContext.forReadOnlyDataBinding() 必须用, 不能用 StandardEvaluationContext 允许执行任意 Java 代码
3. **DLQ retry 策略**: MVP 只记录失败, 不自动重试; 后续加 @Scheduled job
4. **Policy → Notification 接线**: 只处理 WARN, 不处理 BLOCK (BLOCK 抛异常事务回滚, 发通知已晚)

---

# 预期成果对标

| 维度 | 当前 | 完成后 |
|---|:---:|:---:|
| 架构分层 | 78 | 95 (补启动自检) |
| 扩展点完整度 | 65 | 95 (TargetMode SPI + 拆 3 Contribution) |
| 行业污染度 | 92 | 95 (ArchUnit 守护加强) |
| 与 A+ 模块协作 | 55 | 95 (implied + DataScope + Policy 都接) |
| 前端 UX 覆盖 | 75 | 92 (触发点浏览 + SpEL 编辑 + 来源追溯) |
| 测试覆盖 | 40 | 88 (5 集成测试 + 多 unit) |
| **总分** | **67/100** | **~93/100** |

**评级: B- → A+** (消息/事件子系统 A+) 。
