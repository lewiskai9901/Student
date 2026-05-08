# 关系管理 A+ 提升 Phase 1 — 阻断级 bug 修复

> Phase 1/6,目标:从 75/100 → 82/100(B-→B)
> 工作量:~2 天
> 不影响小程序测试(只动后端,后端重启即可)

## 背景

审计发现 3 处问题,Phase 1 全部修掉:

| 编号 | 问题 | 严重度 |
|---|---|---|
| W1.1 | `access_level` 表是 VARCHAR(20)(已修),实体是 String(无类型安全) | 中 — 隐性 bug |
| W1.2 | 实体不暴露 `validFrom`/`validTo`,业务设有效期得开后门 SQL | 高 — 功能阙失 |
| W1.3 | `AuthorizationService` domain 层 + application 层重名,application 层直 jdbc 绕仓储 | 高 — 维护混乱 |

## Task W1.1: AccessLevel enum + TypeHandler

**目标**:`AccessRelation.accessLevel` 从 `String` 改为 `enum AccessLevel`,编译期防 typo / 错误值。

### Files

- Create: `domain/access/model/valueobject/AccessLevel.java`
- Create: `infrastructure/persistence/access/AccessLevelTypeHandler.java`(可选,看 PO 是否走 MyBatis)
- Modify: `domain/access/model/entity/AccessRelation.java`
- Modify: `infrastructure/persistence/access/AccessRelationPO.java`
- Modify: `infrastructure/persistence/access/AccessRelationRepositoryImpl.java`
- Modify: `application/access/AccessRelationApplicationService.java`(CreateCommand / UpdateCommand)
- Modify: `application/access/AuthorizationService.java`(SubjectRef record,grant 调用)
- Modify: `application/place/UniversalPlaceApplicationService.java`(`.accessLevel("FULL")`)
- Modify: `application/user/UserApplicationService.java`(4 处 `.accessLevel("FULL")`)

### Step-by-step

#### Step 1: 创建 enum

`domain/access/model/valueobject/AccessLevel.java`:

```java
package com.school.management.domain.access.model.valueobject;

/**
 * 关系访问等级.
 *
 * 表达 subject 对 resource 的操作授权深度,与 relation 类型独立 —
 * relation 描述"是什么关系",accessLevel 描述"该关系下能做多少".
 *
 * 例:
 *   member(user→org) accessLevel=READ_ONLY → 只能查组织信息
 *   member(user→org) accessLevel=FULL      → 能查 + 能编辑成员名单
 *   admin(user→org)  accessLevel=OWNER     → 完全控制 + 能授权他人
 */
public enum AccessLevel {
    /** 只读 — 仅查询,不改 */
    READ_ONLY,
    /** 读写 — 大部分业务关系默认 */
    FULL,
    /** 拥有 — 包含 FULL + 能再授权(grant/revoke 子权限) */
    OWNER;

    /**
     * 容错解析:大小写不敏感,空值/非法值返回 FULL(默认值).
     * 历史数据可能是小写或老格式,此处兜底.
     */
    public static AccessLevel parse(String s) {
        if (s == null || s.isBlank()) return FULL;
        try {
            return AccessLevel.valueOf(s.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return FULL;  // 兜底
        }
    }

    /** 当前等级是否包含写权限. */
    public boolean isReadWrite() {
        return this == FULL || this == OWNER;
    }

    /** 当前等级是否包含 OWNER 级别(可再授权) */
    public boolean canDelegate() {
        return this == OWNER;
    }
}
```

#### Step 2: 改实体

`AccessRelation.java`(L40,L60-62):

```java
// before
@Builder.Default
private String accessLevel = "FULL";
public boolean isReadWrite() {
    return "FULL".equals(accessLevel) || "OWNER".equals(accessLevel);
}

// after
@Builder.Default
private AccessLevel accessLevel = AccessLevel.FULL;
public boolean isReadWrite() {
    return accessLevel != null && accessLevel.isReadWrite();
}
```

加 import: `import com.school.management.domain.access.model.valueobject.AccessLevel;`

#### Step 3: 改 PO + Repository

`AccessRelationPO.java`(L26):
- 保持 `String accessLevel`(因为 DB 是 VARCHAR,且 PO 直接和 SQL 对接最简单)
- 不引入 MyBatis TypeHandler(避免增加 4-5 个 PO 改动 + handler 注册)
- **领域和持久化的 enum/string 转换在 Repository 实现里做**

`AccessRelationRepositoryImpl.java`(L173):

```java
// before
.accessLevel(po.getAccessLevel() != null ? po.getAccessLevel() : "FULL")

// after
.accessLevel(AccessLevel.parse(po.getAccessLevel()))
```

并在 toPO/save 路径上:
```java
po.setAccessLevel(rel.getAccessLevel() != null ? rel.getAccessLevel().name() : AccessLevel.FULL.name());
```

#### Step 4: 改 ApplicationService 命令对象

`AccessRelationApplicationService.java`:
- L212: `private String accessLevel = "FULL"` → `private AccessLevel accessLevel = AccessLevel.FULL`
- L220: 同上
- L121, L177: `.accessLevel(cmd.getAccessLevel())` 不用改(类型自动跟着)

`AuthorizationService.java`:
- L477: `r.accessLevel != null ? r.accessLevel : "FULL"` → 改成 `r.accessLevel != null ? r.accessLevel.name() : AccessLevel.FULL.name()`
- L486: 类似改
- L547: `public record SubjectRef(... String accessLevel ...)` → 改 `AccessLevel`(通过 `AccessLevel.parse()` 从 SQL 结果转)
- L279, L386, L423: `rs.getString("access_level")` → `AccessLevel.parse(rs.getString("access_level"))`
- L568: `public String accessLevel` → `public AccessLevel accessLevel`(SubjectRef 字段)

#### Step 5: 改散落的 .accessLevel("FULL") 调用

```bash
# 共 5 处:
grep -n '.accessLevel("FULL")' application/place/ application/user/
```

改成 `.accessLevel(AccessLevel.FULL)` + 加 import。

#### Step 6: 编译 + 跑测

```bash
cd backend
JAVA_HOME=... PATH=... mvn compile -DskipTests
JAVA_HOME=... PATH=... mvn test -Dtest='*AccessRelation*,*Authorization*'
```

**期待**:
- `mvn compile` BUILD SUCCESS
- 现有测试不挂
- 如果某个测试硬编码 String "FULL" 等比较,改成 enum

#### Step 7: 加单测

`domain/access/model/valueobject/AccessLevelTest.java`:

```java
@Test void parse_handlesNull() { assertThat(AccessLevel.parse(null)).isEqualTo(AccessLevel.FULL); }
@Test void parse_handlesBlank() { assertThat(AccessLevel.parse("  ")).isEqualTo(AccessLevel.FULL); }
@Test void parse_caseInsensitive() {
    assertThat(AccessLevel.parse("read_only")).isEqualTo(AccessLevel.READ_ONLY);
    assertThat(AccessLevel.parse("OWNER")).isEqualTo(AccessLevel.OWNER);
}
@Test void parse_invalidFallsBackToFull() { assertThat(AccessLevel.parse("WHATEVER")).isEqualTo(AccessLevel.FULL); }
@Test void isReadWrite_correctness() {
    assertThat(AccessLevel.READ_ONLY.isReadWrite()).isFalse();
    assertThat(AccessLevel.FULL.isReadWrite()).isTrue();
    assertThat(AccessLevel.OWNER.isReadWrite()).isTrue();
}
@Test void canDelegate_onlyOwner() {
    assertThat(AccessLevel.OWNER.canDelegate()).isTrue();
    assertThat(AccessLevel.FULL.canDelegate()).isFalse();
}
```

#### Step 8: Commit

```bash
git add domain/access/model/valueobject/AccessLevel.java \
        domain/access/model/entity/AccessRelation.java \
        infrastructure/persistence/access/AccessRelationRepositoryImpl.java \
        application/access/AccessRelationApplicationService.java \
        application/access/AuthorizationService.java \
        application/place/UniversalPlaceApplicationService.java \
        application/user/UserApplicationService.java \
        ../src/test/java/com/school/management/domain/access/model/valueobject/AccessLevelTest.java
git commit -m "refactor(access): AccessLevel enum 替代 String 字面量(Phase 1 W1.1)"
```

---

## Task W1.2: 实体补 validFrom/validTo + Service 暴露

(Phase 1 W1.2 完整 spec — Task 1.1 完成后单独写)

要点:
- AccessRelation 实体加 `LocalDateTime validFrom`, `LocalDateTime validTo`
- PO 同步加(DB 已有列)
- Repository toPO/fromPO 处理
- AccessRelationApplicationService.CreateCommand 加这俩
- AuthorizationService.grant() 加重载 `grant(..., validFrom, validTo)`
- 默认 validFrom=now, validTo=null

---

## Task W1.3: AuthorizationService 重命名 + 收回仓储

(Phase 1 W1.3 完整 spec — W1.2 完成后单独写)

要点:
- `application/access/AuthorizationService.java` → `AccessRelationService.java`
- 所有 jdbcTemplate.* 调用挪到 `infrastructure/persistence/access/AccessRelationRepositoryImpl.java`
- Repository 接口扩 `check / expand / lookup / findSubjectsWithRelation` 等方法
- 调用方 import 全部改名
- domain 层 AuthorizationService 接口保留(只管 RBAC 决策)

---

## 验收

Phase 1 全部完成时:
- [ ] mvn compile BUILD SUCCESS
- [ ] mvn test 全绿(含新增 AccessLevelTest)
- [ ] grep `.accessLevel("` 在 application/ infrastructure/ 下 0 命中
- [ ] grep `String accessLevel` 在 domain/ application/ 下 0 命中(只允许 PO 层)
- [ ] AccessRelation 实体能设 validFrom/validTo
- [ ] application 层 grep `JdbcTemplate.* FROM access_relations` 0 命中(全在 repository)
- [ ] 评分:75 → 82
