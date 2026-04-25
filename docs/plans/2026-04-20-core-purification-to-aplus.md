# Core Purification → A+ Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 把 7 个通用核心模块 (Organization / Place / User / Permission / Role / DataScope / EntityType) 从 A- 推到真·A+ — 彻底剥离行业污染, 补齐 Policy/Validator SPI, 让 DataScope 可插件扩展, 让 AccessRelation 支持关系链推导。

**Architecture:**
- 在既有的 `sealed Contribution` 扩展点家族 (当前 9 permits) 上**新增 2 个**: `PolicyContribution`, 扩展 `DataScopeContribution` 语义变为动态维度。
- User/Permission 的行业污染**迁移而非兼容**: 按项目内约定"彻底删除, 不保留旧列"。
- AccessRelation 加 `RelationTypeDef.impliedRelations()` 声明传递关系。

**Tech Stack:** Spring Boot 3.2, MyBatis Plus, Flyway V104+ (当前最大 V103), Java 17 sealed/records, ArchUnit, JUnit 5, Vue 3 + TypeScript。

**参考规范**: 既有 [插件架构 Phase 2 完成状态](../../CLAUDE.md#插件架构-phase-2-完成) / [docs/adr/001-phase2-unified-spi.md](../adr/001-phase2-unified-spi.md)。

**预估工作量**: 4 tracks × 2-4 天 = **10-13 天** (主职不间断) / 3-4 周 (每周 2-3 天)。

---

## Track 总览

| Track | 目标 | 天数 | 触达级别 |
|---|---|---:|:---:|
| W1 | Policy/Validator SPI (最大扩展性缺口) | 3-4d | **A** |
| W2 | 清理 3 处行业污染 (User.employeeNo / 权限 seed / 前端 type 分支) | 2d | 仍 A |
| W3 | DataScope 动态维度化 (DataScopePlugin 半成品收口) | 2-3d | 仍 A |
| W4 | AccessRelation 关系链 (implied permissions) | 3-4d | **A+** |

**执行顺序建议**: W2 → W1 → W3 → W4 (先拿简单分消顾虑, 再做 SPI 深改, DataScope/Relation 依赖 W1 的模式)。

---

# Track W2 — 清理行业污染

**优先级**: 最高 (工作量最小, ROI 立竿见影)
**原则**: 按 memory 记录 "彻底删除, 不考虑旧数据兼容" — 同步删 DB 列 + 代码 + 前端。

---

### Task W2.1: 审计前确认影响面

**Files:**
- Read: `backend/src/main/java/com/school/management/domain/user/model/aggregate/User.java`
- Grep 所有 `employeeNo` 引用 (已知 14 个文件)
- Read: `database/init/task_basic_permissions.sql`
- Grep 所有前端 `type === 'CLASS'` / `type === 'GRADE'` 硬编码

**Step 1**: Grep 全仓 employeeNo
```bash
cd backend && grep -rn "employeeNo" src/main --include='*.java' | wc -l
```
Expected: ~20-30 行

**Step 2**: 列出每个引用及其分类 (读: 展示 / 写: 创建更新 / 持久化 PO)

**Step 3**: 无 commit, 只是输出审计清单写入 `docs/plans/2026-04-20-core-purification-to-aplus.md.audit-W2.md`

---

### Task W2.2: User.employeeNo 迁移到 TeacherProfile

**策略**: `employeeNo` 业务上只对 TEACHER 类型用户有意义, 而 `user_teacher` 表已经存在 (V89)。把该字段彻底从 `User` 聚合移除, 搬到 `TeacherProfile`。

**Files:**
- Modify: `backend/src/main/java/com/school/management/domain/user/model/aggregate/User.java` (删除 L62-63 `@Deprecated employeeNo` 字段 + getter/setter + 构造用法)
- Modify: `backend/src/main/java/com/school/management/infrastructure/persistence/user/UserPO.java` (删除 employeeNo 列映射)
- Modify: `backend/src/main/java/com/school/management/domain/user/repository/UserRepository.java` (删除 findByEmployeeNo 方法, 若有)
- Modify: `backend/src/main/java/com/school/management/interfaces/rest/user/CreateUserRequest.java` / `UpdateUserRequest.java` / `UserDomainResponse.java` (删除 employeeNo 字段, DTO 层)
- Modify: `backend/src/main/java/com/school/management/application/user/command/CreateUserCommand.java` / `UpdateUserCommand.java`
- Modify: `backend/src/main/java/com/school/management/interfaces/rest/user/UserController.java` / `UserDomainController.java` / `TeacherProfileController.java` (若查询 employeeNo 改走 TeacherProfile)
- Create: `database/schema/V104.0.0__drop_user_employee_no.sql`

**Step 1: 写 Flyway migration**

```sql
-- V104.0.0__drop_user_employee_no.sql
-- 把 users.employee_no 数据迁移到 user_teacher.employee_no, 然后删除 users 列
-- 前置: V89 已创建 user_teacher 表, employee_no 列存在

-- 1. 迁移数据 (只迁 teacher 类型 user)
UPDATE user_teacher ut
INNER JOIN users u ON ut.user_id = u.id
SET ut.employee_no = u.employee_no
WHERE u.employee_no IS NOT NULL AND (ut.employee_no IS NULL OR ut.employee_no = '');

-- 2. 确认 teacher 表已持有, 再从 users 删列
ALTER TABLE users DROP COLUMN employee_no;
```

**Step 2: 从 User aggregate 删字段**

```java
// User.java L54-63 整段删除 (包括注释 + @Deprecated + private String employeeNo;)
```

**Step 3: 从所有 13 个 DTO / service 删字段** (逐个文件编辑)

**Step 4: 编译验证**

```bash
cd backend && mvn compile -DskipTests
```
Expected: BUILD SUCCESS, 无 symbol not found

**Step 5: 单元/集成测试**

```bash
mvn test -Dtest='*UserApplicationService*'
```
Expected: PASS

**Step 6: Commit**

```bash
git add backend/src/main/java/com/school/management/domain/user \
        backend/src/main/java/com/school/management/infrastructure/persistence/user \
        backend/src/main/java/com/school/management/interfaces/rest/user \
        backend/src/main/java/com/school/management/application/user \
        database/schema/V104.0.0__drop_user_employee_no.sql
git commit -m "refactor(user): 彻底删除 User.employeeNo (迁至 user_teacher) — core 去教师特化字段"
```

---

### Task W2.3: 拆分 task_basic_permissions.sql

**问题**: 文件里 `student:*` / `quantification:grade:view` 属于 EDU 插件权限, 不该由 core 初始化。

**Files:**
- Modify: `database/init/task_basic_permissions.sql` (只保留 system:* 和 organization 相关)
- Create: `database/init/plugins/education/edu_basic_permissions.sql` (移出的 student:* / quantification:* 行)
- Modify: 插件注册逻辑 — 在 `EducationPermissionProvider` 或 `RolePresetPlugin` 里声明这些默认权限绑定, 不依赖 seed SQL

**Step 1: 在 core seed 只保留通用权限**

```sql
-- task_basic_permissions.sql (清理后)
-- 只保留 core 通用权限: system:department, system:user, system:role
-- student:* / quantification:* 已迁至 EDU 插件
```

**Step 2: EDU 插件 seed 新文件**

```sql
-- database/init/plugins/education/edu_basic_permissions.sql
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT 2, id FROM permissions WHERE permission_code IN (
    'student:department', 'student:department:view',
    'student:class', 'student:class:view',
    'quantification:grade:view',
    'student:info', 'student:info:view'
);
-- 同理 role 3/4/5
```

**Step 3: 修改 init 流程** — 确认 `database/init/` 的执行入口 (CLAUDE.md 提到 `init_data.sql`), 把 edu seed 改为条件执行 (仅当 EDU 启用)

Run:
```bash
grep -rn "task_basic_permissions" backend/ database/
```

**Step 4: 验证 DB 初始化**

```bash
mysql -u root -p student_management < database/schema/complete_schema_v2.sql
mysql -u root -p student_management < database/init/init_data.sql
# 检查: core 角色 2/3/4/5 无 student:* 权限 (除非 EDU 启用)
```

**Step 5: Commit**

```bash
git add database/init/task_basic_permissions.sql database/init/plugins/education/
git commit -m "refactor(permissions): 拆分 core vs EDU 权限 seed — task_basic_permissions 去教育特化"
```

---

### Task W2.4: 处理前端 `type === 'CLASS'` / `'GRADE'` 硬编码

**Files:**
- Modify: `frontend/src/views/teaching/offering/OfferingListTab.vue:274,278`

**观察**: `views/teaching/` 整个目录是 EDU 插件内容 (router/plugins/edu.ts 里加载)。但文件内硬编码类型字符串不利于后续把 `views/teaching/` 整体搬到 `src/modules/education/` 之类的插件隔离目录。当前 Phase 仅把"判定"通用化:

**Step 1**: 引入类型码常量

Create: `frontend/src/types/plugins/education-types.ts`

```typescript
/** EDU 插件专属的 org_unit 类型码 — 不该被 core 直接引用 */
export const EduOrgTypes = {
  GRADE: 'GRADE',
  CLASS: 'CLASS',
  MAJOR: 'MAJOR',
} as const;
```

**Step 2**: 重构 OfferingListTab.vue

```vue
<!-- L274-278 改为: -->
import { EduOrgTypes } from '@/types/plugins/education-types';
// ...
if (org.type === EduOrgTypes.GRADE) { ... }
if (org.type === EduOrgTypes.CLASS) { ... }
```

**Step 3: 构建验证**

```bash
cd frontend && npm run build 2>&1 | tail -10
```
Expected: 成功 + dist 产物完整

**Step 4: Commit**

```bash
git add frontend/src/types/plugins/education-types.ts \
        frontend/src/views/teaching/offering/OfferingListTab.vue
git commit -m "refactor(frontend): EDU 类型码常量化 — 为后续 teaching 整体搬到插件目录铺路"
```

---

### Task W2.5: ArchUnit 守护 — core 包禁止出现行业类型字符串

**Files:**
- Create: `backend/src/test/java/com/school/management/architecture/NoIndustryTypeInCoreTest.java`

**Step 1: 写测试**

```java
package com.school.management.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * 守护 core 包 (非 plugins.* / 非 inspection.*) 里不能出现硬编码的行业类型字符串.
 * EDU/HEALTH 特有的类型码必须由插件携带, core 不认识.
 */
class NoIndustryTypeInCoreTest {

    private static final String[] FORBIDDEN = {
        "\"STUDENT\"", "\"TEACHER\"", "\"CLASS\"", "\"GRADE\"",
        "\"MAJOR\"", "\"DORMITORY\"", "\"CLASSROOM\"",
        "\"PATIENT\"", "\"WARD\""
    };

    @Test
    void coreCodeHasNoIndustryStringLiterals() {
        JavaClasses core = new ClassFileImporter()
            .withImportOption(loc -> {
                String p = loc.asURI().toString();
                return !p.contains("/plugins/") && !p.contains("/inspection/");
            })
            .importPackages("com.school.management.domain",
                            "com.school.management.application",
                            "com.school.management.infrastructure",
                            "com.school.management.interfaces");

        ArchRule rule = noClasses()
            .should(new ArchCondition<>("contain forbidden industry type literals") {
                @Override
                public void check(com.tngtech.archunit.core.domain.JavaClass item,
                                  ConditionEvents events) {
                    // ArchUnit 没有直接的 literal-in-source 检查, 退而读字节码字符串常量池
                    item.getFieldAccesses().forEach(access -> {
                        // placeholder; 真正检查用 source code scan (见 step2 的替代方案)
                    });
                }
            });
        // 测试主体见 step2 实际实现
    }
}
```

**注意**: ArchUnit 不易做 string literal 检查。替代方案用 grep-based `@Test` + ProcessBuilder:

```java
@Test
void coreSourceFilesHaveNoIndustryTypeLiterals() throws Exception {
    Path srcRoot = Path.of("src/main/java/com/school/management");
    List<Path> offenders = new ArrayList<>();
    try (Stream<Path> files = Files.walk(srcRoot)) {
        files.filter(p -> p.toString().endsWith(".java"))
             .filter(p -> !p.toString().contains("plugins"))
             .filter(p -> !p.toString().contains("inspection"))
             .forEach(p -> {
                 try {
                     String content = Files.readString(p);
                     for (String token : FORBIDDEN) {
                         if (content.contains(token)) {
                             offenders.add(p);
                             break;
                         }
                     }
                 } catch (IOException ignored) {}
             });
    }
    assertThat(offenders)
        .as("core packages must not contain industry type literals")
        .isEmpty();
}
```

**Step 2: 运行**

```bash
mvn test -Dtest=NoIndustryTypeInCoreTest
```
Expected: PASS (上面 3 个清理完成后)

**Step 3: Commit**

```bash
git add backend/src/test/java/com/school/management/architecture/NoIndustryTypeInCoreTest.java
git commit -m "test(arch): NoIndustryTypeInCoreTest — 永久守护 core 不含行业类型字符串"
```

---

# Track W1 — Policy / Validator SPI

**目的**: core 暴露"业务规则检查点", 让插件注入跨类型规则 (例: "CLASS 删除前必须无归属学生", "宿舍入住 <4 人警告")。

---

### Task W1.1: 定义 Policy SPI 骨架

**Files:**
- Create: `backend/src/main/java/com/school/management/infrastructure/extension/Policy.java`
- Create: `backend/src/main/java/com/school/management/infrastructure/extension/PolicyContext.java`
- Create: `backend/src/main/java/com/school/management/infrastructure/extension/Violation.java`

**Step 1: 写 Violation record**

```java
// Violation.java
package com.school.management.infrastructure.extension;

public record Violation(Severity severity, String code, String message, Object detail) {
    public enum Severity { BLOCK, WARN, INFO }

    public static Violation block(String code, String msg) {
        return new Violation(Severity.BLOCK, code, msg, null);
    }
    public static Violation warn(String code, String msg) {
        return new Violation(Severity.WARN, code, msg, null);
    }
}
```

**Step 2: 写 PolicyContext (泛型上下文)**

```java
// PolicyContext.java
package com.school.management.infrastructure.extension;

/**
 * 策略检查上下文 — 封装被检查的实体类型 + 操作阶段 + payload.
 *
 * @param entityType 如 "place" / "org_unit" / "user"
 * @param phase      如 "BEFORE_CHECKIN" / "AFTER_CREATE" / "BEFORE_DELETE"
 * @param payload    操作上下文数据
 */
public record PolicyContext<T>(String entityType, String phase, T payload) {}
```

**Step 3: 写 Policy 接口**

```java
// Policy.java
package com.school.management.infrastructure.extension;

import java.util.List;

/**
 * 策略 SPI — 插件实现此接口, 通过 PolicyContribution 贡献给 core.
 *
 * 实现类应:
 *   1. 实现 supports(ctx) 判断是否处理这个 (entityType, phase) 组合
 *   2. 实现 check(ctx) 返回违规列表, 空=通过
 *
 * core 在变更边界 (入住/出宿/加入组织/删除实体) 调用 PolicyRegistry.check()
 * 集中执行所有支持的策略, 按 BLOCK/WARN/INFO 分级处理.
 */
public interface Policy<T> {
    /** 策略代号, 全局唯一 */
    String code();

    /** 人类可读名称 */
    String name();

    /** 判断此策略是否适用于给定上下文 */
    boolean supports(PolicyContext<?> ctx);

    /** 执行检查 */
    List<Violation> check(PolicyContext<T> ctx);
}
```

**Step 4: 扩展 Contribution sealed**

Modify `Contribution.java`:

```java
// 添加到 permits 列表末尾:
public sealed interface Contribution permits
    Contribution.EntityTypeContribution,
    Contribution.RelationTypeContribution,
    Contribution.EventDomainContribution,
    Contribution.PermissionContribution,
    Contribution.RoleContribution,
    Contribution.MenuContribution,
    Contribution.DataScopeContribution,
    Contribution.RouteContribution,
    Contribution.PolicyContribution,   // ← 新增
    Contribution.DomainContribution {

    // ... 既有 records ...

    /** 策略贡献 (Track W1) */
    record PolicyContribution(Policy<?> policy) implements Contribution {
        @Override public String uniqueKey() { return "policy:" + policy.code(); }
    }
}
```

**Step 5: 更新 UnifiedPluginPackageTest**

Modify `backend/src/test/java/com/school/management/architecture/UnifiedPluginPackageTest.java`:

```java
// 找到断言 Contribution 有 9 permits 的测试, 改为 10
@Test
void contributionSealedHas10Permits() {
    Class<?>[] permits = Contribution.class.getPermittedSubclasses();
    assertThat(permits).hasSize(10);
}
```

**Step 6: 编译 + 测试**

```bash
mvn test -Dtest=UnifiedPluginPackageTest
```
Expected: PASS

**Step 7: Commit**

```bash
git add backend/src/main/java/com/school/management/infrastructure/extension/Policy.java \
        backend/src/main/java/com/school/management/infrastructure/extension/PolicyContext.java \
        backend/src/main/java/com/school/management/infrastructure/extension/Violation.java \
        backend/src/main/java/com/school/management/infrastructure/extension/Contribution.java \
        backend/src/test/java/com/school/management/architecture/UnifiedPluginPackageTest.java
git commit -m "feat(extension): 新增 Policy SPI — Contribution sealed 扩至 10 permits (W1.1)"
```

---

### Task W1.2: 实现 PolicyRegistry

**Files:**
- Create: `backend/src/main/java/com/school/management/infrastructure/extension/PolicyRegistry.java`
- Create: `backend/src/test/java/com/school/management/infrastructure/extension/PolicyRegistryTest.java`

**Step 1: 写失败测试**

```java
// PolicyRegistryTest.java
package com.school.management.infrastructure.extension;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

class PolicyRegistryTest {

    static class AlwaysPass implements Policy<Void> {
        public String code() { return "pass"; }
        public String name() { return "pass"; }
        public boolean supports(PolicyContext<?> c) { return true; }
        public List<Violation> check(PolicyContext<Void> c) { return List.of(); }
    }

    static class BlockOnPlace implements Policy<Void> {
        public String code() { return "block-place"; }
        public String name() { return "block"; }
        public boolean supports(PolicyContext<?> c) { return "place".equals(c.entityType()); }
        public List<Violation> check(PolicyContext<Void> c) {
            return List.of(Violation.block("X", "nope"));
        }
    }

    @Test
    void registryRoutesByEntityTypeAndPhase() {
        PolicyRegistry reg = new PolicyRegistry(List.of(new AlwaysPass(), new BlockOnPlace()));
        List<Violation> r1 = reg.check(new PolicyContext<>("place", "BEFORE_CHECKIN", null));
        assertThat(r1).hasSize(1);
        assertThat(r1.get(0).severity()).isEqualTo(Violation.Severity.BLOCK);

        List<Violation> r2 = reg.check(new PolicyContext<>("user", "BEFORE_DELETE", null));
        assertThat(r2).isEmpty();
    }

    @Test
    void throwIfAnyBlockViolation() {
        PolicyRegistry reg = new PolicyRegistry(List.of(new BlockOnPlace()));
        assertThatThrownBy(() -> reg.enforce(new PolicyContext<>("place", "X", null)))
            .isInstanceOf(PolicyViolationException.class);
    }
}
```

**Step 2: Run, see it fail**

```bash
mvn test -Dtest=PolicyRegistryTest
```
Expected: FAIL with "cannot find PolicyRegistry"

**Step 3: 实现**

```java
// PolicyRegistry.java
package com.school.management.infrastructure.extension;

import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class PolicyRegistry {

    private final List<Policy<?>> policies;

    public PolicyRegistry(List<Policy<?>> policies) {
        this.policies = policies;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public List<Violation> check(PolicyContext<?> ctx) {
        return policies.stream()
            .filter(p -> p.supports(ctx))
            .flatMap(p -> ((Policy) p).check(ctx).stream())
            .map(v -> (Violation) v)
            .toList();
    }

    /** 调用 check 并对 BLOCK 违规抛异常, WARN/INFO 仅返回 */
    public List<Violation> enforce(PolicyContext<?> ctx) {
        List<Violation> all = check(ctx);
        all.stream()
            .filter(v -> v.severity() == Violation.Severity.BLOCK)
            .findFirst()
            .ifPresent(v -> { throw new PolicyViolationException(all); });
        return all;
    }
}
```

```java
// PolicyViolationException.java
package com.school.management.infrastructure.extension;

import java.util.List;

public class PolicyViolationException extends RuntimeException {
    private final List<Violation> violations;
    public PolicyViolationException(List<Violation> v) {
        super("Policy violation: " + v);
        this.violations = v;
    }
    public List<Violation> getViolations() { return violations; }
}
```

**Step 4: PASS**

```bash
mvn test -Dtest=PolicyRegistryTest
```
Expected: PASS

**Step 5: Commit**

```bash
git add backend/src/main/java/com/school/management/infrastructure/extension/PolicyRegistry.java \
        backend/src/main/java/com/school/management/infrastructure/extension/PolicyViolationException.java \
        backend/src/test/java/com/school/management/infrastructure/extension/PolicyRegistryTest.java
git commit -m "feat(extension): PolicyRegistry + enforce API (W1.2)"
```

---

### Task W1.3: ContributionDispatcher 接入 PolicyContribution

**Files:**
- Modify: `backend/src/main/java/com/school/management/infrastructure/extension/ContributionDispatcher.java` (在 switch 里加 PolicyContribution 分支)

**Step 1: 找现有 dispatch switch**

```bash
grep -n "switch.*Contribution\|case.*Contribution" backend/src/main/java/com/school/management/infrastructure/extension/ContributionDispatcher.java
```

**Step 2: 添加 case**

```java
case Contribution.PolicyContribution pc ->
    log.info("[ContributionDispatcher] registered Policy: {}", pc.policy().code());
// PolicyRegistry 自己通过 Spring DI 收集 Policy<?> bean, 不需 dispatcher 写表
```

**Step 3: 测试**

```bash
mvn test -Dtest=UnifiedPluginPackageTest
```
Expected: PASS

**Step 4: Commit**

```bash
git add backend/src/main/java/com/school/management/infrastructure/extension/ContributionDispatcher.java
git commit -m "feat(extension): ContributionDispatcher 处理 PolicyContribution (W1.3)"
```

---

### Task W1.4: Place 入住流程接入 Policy hook

**定位关键方法**: 需要先找到 Place 的 checkIn / addOccupant 应用服务。

**Step 1: Grep 现有入住方法**

```bash
grep -rn "checkIn\|addOccupant\|assignPlace" backend/src/main/java/com/school/management/application/place
```

**Step 2: 在最合适的入住应用服务里注入 PolicyRegistry**

```java
// 假设是 PlaceApplicationService.checkIn(...)
private final PolicyRegistry policyRegistry;

public void checkIn(CheckInCommand cmd) {
    PolicyContext<CheckInCommand> ctx = new PolicyContext<>("place", "BEFORE_CHECKIN", cmd);
    List<Violation> violations = policyRegistry.enforce(ctx);  // BLOCK 抛异常

    // ...既有业务逻辑...

    PolicyContext<CheckInCommand> after = new PolicyContext<>("place", "AFTER_CHECKIN", cmd);
    List<Violation> warns = policyRegistry.check(after);
    warns.stream()
        .filter(v -> v.severity() == Violation.Severity.WARN)
        .forEach(v -> notificationService.warn(v.message()));  // 非阻断
}
```

**Step 3: 集成测试**

写一个 `PlaceCheckInPolicyIntegrationTest`, 注册一个 mock Policy 抛 BLOCK, 验证 checkIn 抛 PolicyViolationException。

**Step 4: Commit**

```bash
git commit -m "feat(place): checkIn 接入 PolicyRegistry.enforce (W1.4)"
```

---

### Task W1.5: OrgUnit 成员变更接入 Policy hook

**同 W1.4 模式**, 在 `OrgUnitApplicationService.addMember` / `removeMember` / `delete` 三个变更点加 PolicyRegistry 调用。

**Commit**: `feat(org): addMember/removeMember/delete 接入 PolicyRegistry (W1.5)`

---

### Task W1.6: 示例实现 — MinOccupantsPolicy (core 内置)

**验证 SPI 可用 + 作为 reference impl 给外部开发者参考。**

**Files:**
- Create: `backend/src/main/java/com/school/management/infrastructure/extension/plugins/core/MinOccupantsPolicy.java`
- Create: `backend/src/test/java/com/school/management/infrastructure/extension/plugins/core/MinOccupantsPolicyTest.java`

**Step 1: 实现**

```java
@Component
public class MinOccupantsPolicy implements Policy<CheckInCommand> {

    private final PlaceRepository placeRepo;

    public String code() { return "MIN_OCCUPANTS_WARN"; }
    public String name() { return "最小入住人数警告"; }

    public boolean supports(PolicyContext<?> ctx) {
        return "place".equals(ctx.entityType()) && "AFTER_CHECKIN".equals(ctx.phase());
    }

    public List<Violation> check(PolicyContext<CheckInCommand> ctx) {
        Place place = placeRepo.findById(ctx.payload().placeId());
        int min = readMinOccupantsConfig(place);  // 从 place.metadata 或配置表读
        if (place.currentOccupancy() < min) {
            return List.of(Violation.warn("MIN_OCCUPANTS",
                "场所 " + place.name() + " 当前 " + place.currentOccupancy() +
                " 人, 低于期望 " + min + " 人"));
        }
        return List.of();
    }
}
```

**Step 2: 测试 + Commit**

```bash
git commit -m "feat(core): MinOccupantsPolicy — 策略 SPI reference impl (W1.6)"
```

---

### Task W1.7: 文档 — plugin-extension-catalog 增加 Policy 章节

**Files:**
- Modify: `docs/plugin-extension-catalog.md`

添加 Policy SPI 的使用示例 + 何时用 vs EntityTypePlugin.validate() vs Spring @Valid 的对比表。

**Commit**: `docs(plugin): Policy SPI 章节 + 与其他校验手段对比 (W1.7)`

---

# Track W3 — DataScope 动态维度化

**目标**: 当前 DataScopePlugin SPI 存在但 `DataScope` enum 硬编码 5 值, 插件贡献的维度被架空。让 DataScope 支持动态维度 + enum 部分保持为 hardcoded base。

---

### Task W3.1: 建 data_scope_dimensions 表

**Files:**
- Create: `database/schema/V105.0.0__data_scope_dimensions.sql`

```sql
-- V105: 数据权限动态维度注册表
CREATE TABLE IF NOT EXISTS data_scope_dimensions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    dimension_code VARCHAR(50) NOT NULL UNIQUE COMMENT '维度代号, 如 BY_MAJOR / BY_WARD',
    display_name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    resolver_class VARCHAR(200) NOT NULL COMMENT 'DataScopeResolver 实现类全限定名',
    source_plugin VARCHAR(100) COMMENT '注册来源插件码, 如 EDU / HEALTH',
    is_enabled TINYINT DEFAULT 1,
    sort_order INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_plugin (source_plugin)
) COMMENT='数据权限维度注册表 (动态, 由 DataScopeContribution 启动 UPSERT)';
```

**Commit**: `feat(schema): V105 data_scope_dimensions 表 (W3.1)`

---

### Task W3.2: DataScopeRegistrar 启动 UPSERT + 加载

修改现有 `DataScopeRegistrar` (路径: `backend/src/main/java/com/school/management/infrastructure/extension/DataScopeRegistrar.java`), 将 ContributionDispatcher 分发来的 DataScopeContribution 写入新表。

**Step 1: 测试写**
**Step 2: 修改 Registrar**
**Step 3: 运行 + Commit**

`feat(datascope): Registrar 启动 UPSERT data_scope_dimensions 表 (W3.2)`

---

### Task W3.3: Role 数据权限 UI 动态加载维度

前端 `RoleDataPermissionDialog.vue` (或等效文件) 改为从 `/api/roles/data-permissions/scopes` 读维度列表, 不再硬编码 5 个 enum 值。

**Step 1: 后端 endpoint 返回 hardcoded base (5 enum) + dynamic (表数据) 合并**
**Step 2: 前端下拉动态渲染**
**Step 3: Commit**

`feat(ui): Role 数据权限下拉动态加载维度 (W3.3)`

---

### Task W3.4: DataScopeResolver 路由到插件实现

**Files:**
- Modify: `backend/src/main/java/com/school/management/infrastructure/access/DataPermissionInterceptor.java` 或 `DataScopeResolver` 实现链

**核心**: 当 role 的 data_scope 是 `BY_MAJOR` (插件维度) 时, 从 `data_scope_dimensions.resolver_class` 读出类名, 经 ApplicationContext.getBean() 拿到实例执行。

**Commit**: `feat(datascope): 拦截器路由插件维度 resolver (W3.4)`

---

# Track W4 — AccessRelation 关系链

**目标**: 声明"A relation → implied B relation", 授权时自动展开。解决"宿舍长 → 本宿舍所有 user viewer"这类场景。

---

### Task W4.1: 扩展 RelationTypeDef — impliedRelations 字段

**Files:**
- Modify: `backend/src/main/java/com/school/management/infrastructure/extension/RelationTypePlugin.java`

**Step 1**: 在 RelationTypeDef record 增加字段

```java
public record RelationTypeDef(
    String relationCode,
    String fromType, String toType,
    String relationName,
    boolean isTransitive,
    String category,
    String description,
    boolean capacityBound,
    Integer maxPerResource,
    Map<String, Integer> maxBySubtype,
    /** 关系推导规则: 拥有本关系时, 自动拥有以下 implied 关系 */
    List<Implied> impliedRelations  // ← 新增
) {
    public record Implied(
        /** 推导关系的目标资源类型, 如 "user" */
        String targetType,
        /** 推导得到的 relation, 如 "viewer" */
        String relation,
        /** 如何从原 resource 找到目标资源. 例: "OCCUPANTS_OF_PLACE" */
        String discoveryRule
    ) {}
    // ... factory 方法同步增加一个 .withImplied() builder
}
```

**Step 2**: 老 factory 保持兼容
**Step 3**: Commit

`feat(relation): RelationTypeDef 加 impliedRelations 字段 (W4.1)`

---

### Task W4.2: Schema — relation_types 表加 implied_relations 列

**Files:**
- Create: `database/schema/V106.0.0__relation_implied.sql`

```sql
ALTER TABLE relation_types
ADD COLUMN implied_relations JSON COMMENT '关系链推导规则';
```

---

### Task W4.3: AuthorizationService 展开 implied 检查

**定位 core 检查方法**: `authorize(user, resource, permission)` — 在比对关系时加入 implied 展开逻辑。

**Step 1**: 写失败测试

```java
@Test
void dormHeadImpliesViewerOnOccupants() {
    // 给 user A 建 dorm_head_of → place 101 关系
    // RelationTypeDef dorm_head_of 配置 impliedRelations:
    //   Implied("user", "viewer", "OCCUPANTS_OF_PLACE")
    // authorizationService.canRead(A, user=student-in-101) 应返回 true
    assertThat(authz.canRead(userA, studentInDorm101)).isTrue();
}
```

**Step 2**: 实现展开逻辑
**Step 3**: 缓存 (展开开销大, 用 Caffeine 或 Redis 缓存 implied edges)
**Step 4**: Commit

`feat(authz): 关系链 implied 推导 + 缓存 (W4.3)`

---

### Task W4.4: 迁移 CoreRelationsPlugin — 为 occupant/member 加 implied

把"场所 occupant 的 place manager → 对 occupant viewer"这种语义从散落在各处的 hack 改为 RelationTypeDef 声明。

**Commit**: `feat(relations): occupant/member 关系加 implied 声明 (W4.4)`

---

### Task W4.5: 架构测试 — 禁止绕过 AuthorizationService

**Files:**
- Create: `backend/src/test/java/com/school/management/architecture/NoBypassAuthServiceTest.java`

规则: `application/**` 下的查询方法不得直接访问 AccessRelationMapper, 必须走 AuthorizationService。

**Commit**: `test(arch): NoBypassAuthServiceTest 守护 (W4.5)`

---

# Phase 完成 Gate

每个 track 结束, 验证对应 gate:

### W2 完成 Gate
- [ ] `NoIndustryTypeInCoreTest` 绿
- [ ] User 无 employeeNo 字段 / getter / setter
- [ ] `task_basic_permissions.sql` 不含 student:* / quantification:*
- [ ] 前端 build 绿
- [ ] `mvn test` 全绿

### W1 完成 Gate
- [ ] Contribution 有 10 permits (UnifiedPluginPackageTest 确认)
- [ ] PolicyRegistry + PolicyViolationException 就绪
- [ ] PlaceApplicationService.checkIn 接入 policy.enforce
- [ ] OrgUnit 变更 3 处接入
- [ ] 至少 1 个 reference Policy (MinOccupantsPolicy)
- [ ] docs/plugin-extension-catalog.md 含 Policy 章节

### W3 完成 Gate
- [ ] V105 migration 跑成功
- [ ] `/api/roles/data-permissions/scopes` 返回 hardcoded + dynamic 合并
- [ ] 前端 RoleDataPermissionDialog 动态渲染
- [ ] 插件贡献的 BY_MAJOR resolver 工作

### W4 完成 Gate
- [ ] V106 migration
- [ ] RelationTypeDef.impliedRelations 字段
- [ ] AuthorizationService 展开 + 缓存
- [ ] `dormHeadImpliesViewerOnOccupants` 集成测试绿

### Phase 完成 = A+
所有 4 个 Gate + 以下最终测试:
- [ ] `mvn verify` 全绿
- [ ] `npm run build` + `npm run type-check:ceiling` 绿
- [ ] Chrome 手工跑一遍创建角色 / 角色选 BY_MAJOR / 看到 implied 权限生效的完整链路

---

# 风险与 pushback 点

1. **W1 的 Policy hook 点选择**: Place/OrgUnit 变更边界可能比想象的多, 初版先覆盖 3-5 个最关键的 (checkIn, addMember, delete); 后续补。
2. **W3 数据迁移**: 如果已有角色数据权限配置里存着硬编码 `BY_MAJOR` 而表未建, 必须在 V105 migration 前先归档。
3. **W4 缓存一致性**: implied 展开用缓存, AccessRelation 写操作要失效缓存; Event sourcing 化是后置优化不纳入本 plan。
4. **backward compat**: 本计划遵循项目 memory 里"彻底删除, 不兼容旧数据"原则, 所有 deprecation 直接物理移除。

---

# 当前 session 不在本 plan

- 插件 marketplace / PF4J runtime isolation (S 级) — 见 [Road to A+ 完成](../../CLAUDE.md) 明确跳过
- inspection 迁 plugin — 不在本计划内 (ROI 未到)
- 微服务拆分 — S+ 级, 需商业信号
