# 质量保障基础设施 Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 为已修复的 20+ bug 建立回归测试保护 + 强化 CI 静态检查 + 接入生产错误监控（Sentry）。

**Architecture:** 分 4 个阶段，每阶段独立有价值。Phase 1 优先级最高（回归测试），Phase 4 需用户提供 Sentry DSN 后执行。每 Task 是 1-3 个小 commit。

**Tech Stack:** JUnit 5 + Mockito + JaCoCo (后端) / Vitest + Playwright (前端) / GitHub Actions (CI) / Sentry (监控)

**当前状态（无需搭建，直接用）：**
- ✅ Backend: JUnit 5, Mockito, JaCoCo (15% threshold), SpotBugs (code-quality profile)
- ✅ Frontend: Vitest + Playwright, TypeScript strict mode, ESLint
- ✅ CI: `.github/workflows/ci.yml` 已存在，包含后端测试 + 前端 type-check + 安全扫描
- ⚠️ Gap: 测试覆盖集中在部分功能，已修复的 bug 无专门的回归测试

---

## Phase 1: 回归测试 — 核心业务路径

目标：为每个已修的 CRITICAL/HIGH bug 写测试，编译可通过、运行可验证。

### Task 1.1: 组织管理领域模型测试

**Files:**
- Create: `backend/src/test/java/com/school/management/domain/organization/model/OrgUnitTest.java`
- Create: `backend/src/test/java/com/school/management/domain/organization/service/OrgUnitDomainServiceTest.java`

**Step 1: 创建 OrgUnit 领域模型测试文件**

```java
package com.school.management.domain.organization.model;

import com.school.management.domain.organization.model.valueobject.OrgUnitStatus;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class OrgUnitTest {

    @Test
    void create_shouldInitializeWithActiveStatus() {
        OrgUnit unit = OrgUnit.create("DEPT_001", "研发部", "DEPARTMENT", null, 1L);
        assertThat(unit.getStatus()).isEqualTo(OrgUnitStatus.ACTIVE);
        assertThat(unit.isActive()).isTrue();
    }

    @Test
    void setTreePosition_forRootUnit_shouldSetPathCorrectly() {
        OrgUnit unit = OrgUnit.create("DEPT_001", "研发部", "DEPARTMENT", null, 1L);
        unit.setId(100L);
        unit.setTreePosition(null, 0);
        assertThat(unit.getTreePath()).isEqualTo("/100/");
        assertThat(unit.getTreeLevel()).isEqualTo(0);
    }

    @Test
    void setTreePosition_forChildUnit_shouldAppendToParentPath() {
        OrgUnit unit = OrgUnit.create("TEAM_001", "A组", "TEAM", 100L, 1L);
        unit.setId(200L);
        unit.setTreePosition("/100/", 0);
        assertThat(unit.getTreePath()).isEqualTo("/100/200/");
        assertThat(unit.getTreeLevel()).isEqualTo(1);
    }

    @Test
    void freeze_thenUnfreeze_shouldReturnToActive() {
        OrgUnit unit = OrgUnit.create("DEPT_001", "研发部", "DEPARTMENT", null, 1L);
        unit.freeze("暂停运营", 1L);
        assertThat(unit.getStatus()).isEqualTo(OrgUnitStatus.FROZEN);
        unit.unfreeze(1L);
        assertThat(unit.getStatus()).isEqualTo(OrgUnitStatus.ACTIVE);
    }

    @Test
    void markMergedInto_shouldSetDissolvedStatus() {
        OrgUnit unit = OrgUnit.create("DEPT_001", "研发部", "DEPARTMENT", null, 1L);
        unit.markMergedInto(999L, "合并到A部", 1L);
        assertThat(unit.getStatus()).isEqualTo(OrgUnitStatus.DISSOLVED);
        assertThat(unit.getMergedIntoId()).isEqualTo(999L);
    }
}
```

**Step 2: Run tests**
```bash
cd backend && JAVA_HOME="/c/Program Files/Java/jdk-17" PATH="/c/Program Files/Java/jdk-17/bin:/c/Program Files/apache-maven-3.9.11/bin:$PATH" mvn test -Dtest=OrgUnitTest
```
Expected: 5 tests pass.

**Step 3: 创建 OrgUnitDomainService 测试（核心 bug 回归）**

这个测试覆盖 splitOrgUnit 必须验证 childId 属于 source 的 bug。

```java
package com.school.management.domain.organization.service;

import com.school.management.domain.organization.model.OrgUnit;
import com.school.management.domain.organization.repository.OrgUnitRepository;
import com.school.management.domain.organization.repository.OrgUnitTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrgUnitDomainServiceTest {

    @Mock OrgUnitRepository orgUnitRepository;
    @Mock OrgUnitTypeRepository orgUnitTypeRepository;
    @InjectMocks OrgUnitDomainService service;

    @Test
    void splitOrgUnit_whenChildBelongsToOtherParent_shouldThrow() {
        // 安排：source 是部门 A（id=1），child 是部门 B 下的组（parentId=2，非 1）
        OrgUnit source = OrgUnit.create("A", "部门A", "DEPARTMENT", null, 1L);
        source.setId(1L);
        OrgUnit unrelatedChild = OrgUnit.create("X", "其他组", "TEAM", 2L, 1L);
        unrelatedChild.setId(99L);

        when(orgUnitRepository.findById(1L)).thenReturn(Optional.of(source));
        when(orgUnitRepository.existsByUnitCode("NEW_A")).thenReturn(false);
        when(orgUnitRepository.save(any())).thenAnswer(i -> {
            OrgUnit o = i.getArgument(0);
            if (o.getId() == null) o.setId(100L);
            return o;
        });
        when(orgUnitRepository.findById(99L)).thenReturn(Optional.of(unrelatedChild));

        OrgUnitDomainService.SplitSpec spec = new OrgUnitDomainService.SplitSpec(
            "NEW_A", "新部门A", List.of(99L));

        // 断言：应抛异常拒绝越权移动
        assertThatThrownBy(() -> service.splitOrgUnit(1L, List.of(spec), "拆分", 1L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("不属于源组织");
    }

    @Test
    void mergeOrgUnits_cannotMergeSelfIntoSelf() {
        assertThatThrownBy(() -> service.mergeOrgUnits(1L, 1L, "merge", 1L))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
```

**Step 4: Run tests**
```bash
mvn test -Dtest=OrgUnitDomainServiceTest
```
Expected: 2 tests pass.

**Step 5: Commit**
```bash
git add backend/src/test/java/com/school/management/domain/organization/
git commit -m "test: add OrgUnit domain model + OrgUnitDomainService regression tests"
```

---

### Task 1.2: OrgMemberService 回归测试（removeMember 数据腐败 bug）

**Files:**
- Create: `backend/src/test/java/com/school/management/application/organization/OrgMemberServiceTest.java`

**Step 1: 写测试**

```java
package com.school.management.application.organization;

import com.school.management.domain.access.repository.AccessRelationRepository;
import com.school.management.domain.organization.repository.OrgUnitRepository;
import com.school.management.infrastructure.persistence.user.UserDomainMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrgMemberServiceTest {

    @Mock UserDomainMapper userDomainMapper;
    @Mock OrgUnitRepository orgUnitRepository;
    @Mock AccessRelationRepository accessRelationRepository;
    @InjectMocks OrgMemberService service;

    @Test
    void removeMember_shouldOnlyDeleteSpecificUserRelation_notAll() {
        // 原 bug: 调用了 deleteByResource("org_unit", orgUnitId) 删光所有成员
        // 修复后: 必须调用 deleteByResourceAndSubject(..., "user", userId)
        service.removeMember(100L, 999L);

        // 验证调用了正确的方法
        verify(accessRelationRepository).deleteByResourceAndSubject(
            eq("org_unit"), eq(100L), eq("user"), eq(999L));
        // 验证**没有**调用错误的删除方法
        verify(accessRelationRepository, never()).deleteByResource("org_unit", 100L);
    }

    @Test
    void endAllByOrgUnitId_shouldUseDeleteByResource() {
        // 这个方法是"解散组织时删除所有成员关系"，需要全删
        service.endAllByOrgUnitId(100L, "解散");
        verify(accessRelationRepository).deleteByResource("org_unit", 100L);
    }
}
```

**Step 2: Run + Step 3: Commit**
```bash
mvn test -Dtest=OrgMemberServiceTest
git add backend/src/test/java/com/school/management/application/organization/OrgMemberServiceTest.java
git commit -m "test: add removeMember regression test for data corruption bug"
```

---

### Task 1.3: 场所 checkIn/checkOut 并发测试

**Files:**
- Create: `backend/src/test/java/com/school/management/application/place/UniversalPlaceCheckInConcurrencyTest.java`

这个是集成测试，使用 Spring Boot + MySQL + Redis 真实环境。如果环境配置复杂，可降级为单元测试通过 Mockito 验证 `atomicIncrementOccupancy` 返回 false 时的回滚路径。

**Step 1: 写单元测试版本（更快、不依赖 DB）**

```java
package com.school.management.application.place;

import com.school.management.domain.place.model.aggregate.UniversalPlace;
import com.school.management.domain.place.repository.UniversalPlaceRepository;
import com.school.management.domain.place.repository.UniversalPlaceOccupantRepository;
// ... 其他 imports

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UniversalPlaceCheckInConcurrencyTest {

    @Mock UniversalPlaceRepository placeRepository;
    @Mock UniversalPlaceOccupantRepository occupantRepository;
    // ... 其他 mock（根据实际构造函数参数）
    
    @InjectMocks UniversalPlaceApplicationService service;

    @Test
    void checkIn_whenAtomicIncrementFails_shouldRollbackOccupant() {
        // 模拟场所已满（atomicIncrementOccupancy 返回 false）
        UniversalPlace place = /* ... 构造容量=1，已满的场所 ... */;
        when(placeRepository.findById(100L)).thenReturn(Optional.of(place));
        when(placeRepository.atomicIncrementOccupancy(100L)).thenReturn(false);
        // ... 其他 stub
        
        var command = new UniversalPlaceApplicationService.CheckInCommand();
        command.setOccupantId(999L);
        command.setOccupantType("user");
        
        // 执行 + 断言：应抛"场所已满"异常
        assertThatThrownBy(() -> service.checkIn(100L, command))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("场所已满");
        
        // 验证：占用记录被回滚删除
        verify(occupantRepository).deleteById(anyLong());
    }
}
```

**注意**：由于 `UniversalPlaceApplicationService` 依赖较多，如果 Mockito 配置复杂，可以简化为只测关键逻辑。如果仍觉繁琐，跳过单元测试，写 E2E 测试（见 Phase 2）覆盖。

**Step 2-3: Run + Commit**
```bash
mvn test -Dtest=UniversalPlaceCheckInConcurrencyTest
git add backend/src/test/java/com/school/management/application/place/UniversalPlaceCheckInConcurrencyTest.java
git commit -m "test: add checkIn rollback regression test for overbooking fix"
```

---

### Task 1.4: 占用数对账 Job 测试

**Files:**
- Create: `backend/src/test/java/com/school/management/infrastructure/scheduler/PlaceOccupancyReconciliationJobTest.java`

**Step 1: 写测试**

```java
package com.school.management.infrastructure.scheduler;

import com.school.management.infrastructure.persistence.place.UniversalPlaceMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlaceOccupancyReconciliationJobTest {

    @Mock UniversalPlaceMapper placeMapper;
    @InjectMocks PlaceOccupancyReconciliationJob job;

    @Test
    void reconcile_whenMismatchFound_shouldFixCounter() {
        when(placeMapper.findOccupancyMismatches()).thenReturn(List.of(
            Map.of("id", 100L, "storedCount", 5, "actualCount", 3)
        ));

        job.reconcile();

        verify(placeMapper).fixOccupancy(100L, 3);
    }

    @Test
    void reconcile_whenNoMismatch_shouldDoNothing() {
        when(placeMapper.findOccupancyMismatches()).thenReturn(List.of());
        job.reconcile();
        verify(placeMapper, never()).fixOccupancy(anyLong(), anyInt());
    }

    @Test
    void reconcile_withNullValuesInRow_shouldSkipGracefully() {
        when(placeMapper.findOccupancyMismatches()).thenReturn(List.of(
            Map.of("id", 100L) // missing storedCount, actualCount
        ));
        // 不应抛 NPE
        job.reconcile();
        verify(placeMapper, never()).fixOccupancy(anyLong(), anyInt());
    }
}
```

**Step 2-3: Run + Commit**
```bash
mvn test -Dtest=PlaceOccupancyReconciliationJobTest
git commit -m "test: add reconciliation job tests covering null-safety fix"
```

---

### Task 1.5: UniversalPlace 聚合根领域测试

**Files:**
- Create: `backend/src/test/java/com/school/management/domain/place/model/aggregate/UniversalPlaceTest.java`

覆盖：checkIn/checkOut 容量约束、status 变更、isAncestorOf 环路检测。

**Step 1: 写测试（简化，关键场景）**

```java
package com.school.management.domain.place.model.aggregate;

import com.school.management.domain.place.model.valueobject.PlaceStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class UniversalPlaceTest {

    @Test
    void checkIn_whenAtCapacity_shouldThrow() {
        UniversalPlace place = UniversalPlace.builder()
            .id(1L)
            .placeCode("R101")
            .placeName("101宿舍")
            .typeCode("DORM_ROOM")
            .capacity(2)
            .currentOccupancy(2)
            .status(PlaceStatus.NORMAL)
            .build();

        assertThatThrownBy(place::checkIn)
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void checkOut_whenAlreadyZero_shouldThrow() {
        UniversalPlace place = UniversalPlace.builder()
            .id(1L).placeCode("R").placeName("N").typeCode("T")
            .capacity(2).currentOccupancy(0).status(PlaceStatus.NORMAL)
            .build();

        assertThatThrownBy(place::checkOut)
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void isAncestorOf_shouldDetectCycle() {
        UniversalPlace a = UniversalPlace.builder().id(1L).path("/1/").build();
        UniversalPlace b = UniversalPlace.builder().id(2L).path("/1/2/").build();
        
        assertThat(a.isAncestorOf(b)).isTrue();
        assertThat(b.isAncestorOf(a)).isFalse();
    }
}
```

**Step 2-3: Run + Commit**
```bash
mvn test -Dtest=UniversalPlaceTest
git commit -m "test: add UniversalPlace aggregate tests for capacity + cycle detection"
```

---

### Task 1.6: AccessRelation 仓储层新方法集成测试

验证 `deleteByResourceAndSubject` 的 SQL 正确性。需要 Spring Boot 测试上下文。

**Files:**
- Create: `backend/src/test/java/com/school/management/infrastructure/persistence/access/AccessRelationRepositoryImplIT.java`

**Step 1: 写集成测试（使用 @SpringBootTest + MySQL in CI）**

```java
package com.school.management.infrastructure.persistence.access;

import com.school.management.domain.access.model.entity.AccessRelation;
import com.school.management.domain.access.repository.AccessRelationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
class AccessRelationRepositoryImplIT {

    @Autowired AccessRelationRepository repo;

    @Test
    void deleteByResourceAndSubject_shouldOnlyDeleteMatchingTuple() {
        // 准备：同一 org_unit 下两个 user 的关系
        AccessRelation r1 = AccessRelation.builder()
            .resourceType("org_unit").resourceId(100L)
            .relation("member").subjectType("user").subjectId(1L)
            .build();
        AccessRelation r2 = AccessRelation.builder()
            .resourceType("org_unit").resourceId(100L)
            .relation("member").subjectType("user").subjectId(2L)
            .build();
        repo.save(r1);
        repo.save(r2);

        // 只删 user 1
        repo.deleteByResourceAndSubject("org_unit", 100L, "user", 1L);

        // 验证：user 2 的关系还在，user 1 的被删
        assertThat(repo.exists("org_unit", 100L, "member", "user", 1L)).isFalse();
        assertThat(repo.exists("org_unit", 100L, "member", "user", 2L)).isTrue();
    }
}
```

**Step 2-3: Run + Commit**
```bash
# 集成测试通常命名为 *IT 而非 *Test，需要 Failsafe 插件。可改名为 *Test 让 Surefire 跑
mvn test -Dtest=AccessRelationRepositoryImplIT
git commit -m "test: add integration test for deleteByResourceAndSubject correctness"
```

---

## Phase 2: 前端 E2E 测试 — 关键用户流程

目标：Playwright 覆盖核心业务路径，任何回归立即暴露。

### Task 2.1: 登录流程 E2E

**Files:**
- Create: `frontend/tests/e2e/auth-critical.spec.ts`

**Step 1: 写测试**

```typescript
import { test, expect } from '@playwright/test'

test.describe('Critical auth flows', () => {
  test('admin login + access dashboard', async ({ page }) => {
    await page.goto('/login')
    await page.fill('input[placeholder="请输入账号"]', 'admin')
    await page.fill('input[placeholder="请输入密码"]', 'admin123')
    await page.click('button:has-text("登录")')
    
    await expect(page).toHaveURL(/\/dashboard/)
    // 验证侧边栏显示
    await expect(page.locator('text=组织管理')).toBeVisible()
  })

  test('invalid credentials should show error', async ({ page }) => {
    await page.goto('/login')
    await page.fill('input[placeholder="请输入账号"]', 'wrong')
    await page.fill('input[placeholder="请输入密码"]', 'wrong')
    await page.click('button:has-text("登录")')
    
    await expect(page.locator('.el-message--error')).toBeVisible()
  })

  test('logout clears session', async ({ page }) => {
    // login first
    await page.goto('/login')
    await page.fill('input[placeholder="请输入账号"]', 'admin')
    await page.fill('input[placeholder="请输入密码"]', 'admin123')
    await page.click('button:has-text("登录")')
    await page.waitForURL(/\/dashboard/)
    
    // logout
    await page.click('text=系统管理员') // 用户菜单
    await page.click('text=退出登录')
    await expect(page).toHaveURL(/\/login/)
  })
})
```

**Step 2: Run**
```bash
cd frontend && npx playwright test tests/e2e/auth-critical.spec.ts
```

**Step 3: Commit**
```bash
git add frontend/tests/e2e/auth-critical.spec.ts
git commit -m "test: add critical auth flow E2E tests"
```

---

### Task 2.2: 组织管理 E2E

**Files:**
- Create: `frontend/tests/e2e/organization-critical.spec.ts`

**Step 1: 写测试**

```typescript
import { test, expect } from '@playwright/test'

test.describe('Organization management critical flows', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/login')
    await page.fill('input[placeholder="请输入账号"]', 'admin')
    await page.fill('input[placeholder="请输入密码"]', 'admin123')
    await page.click('button:has-text("登录")')
    await page.waitForURL(/\/dashboard/)
  })

  test('org tree loads and clicking node shows detail', async ({ page }) => {
    await page.goto('/organization/units')
    await expect(page.locator('text=组织树')).toBeVisible()
    // 等待树加载
    await expect(page.locator('text=枣庄技师学院')).toBeVisible({ timeout: 5000 })
    
    // 点击树节点
    await page.click('text=枣庄技师学院')
    
    // 验证右侧详情面板出现
    await expect(page.locator('text=下级组织')).toBeVisible()
  })

  test('sidebar should not show raw paths like /system/org-types', async ({ page }) => {
    await page.goto('/dashboard')
    // 展开系统管理
    await page.click('text=系统管理')
    // 验证没有原始路径显示
    await expect(page.locator('text=/system/org-types')).not.toBeVisible()
    await expect(page.locator('text=/system/place-types')).not.toBeVisible()
  })
})
```

**Step 2-3: Run + Commit**

---

### Task 2.3: 场所入住/退住 E2E

**Files:**
- Create: `frontend/tests/e2e/place-checkin.spec.ts`

覆盖关键业务流：创建场所 → 入住 → 退住。可能需要 seed data 或测试账号权限。

**Step 1: 写测试（保守版 — 只验证 UI 路径可达）**

```typescript
import { test, expect } from '@playwright/test'

test.describe('Place management critical flows', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/login')
    await page.fill('input[placeholder="请输入账号"]', 'admin')
    await page.fill('input[placeholder="请输入密码"]', 'admin123')
    await page.click('button:has-text("登录")')
    await page.waitForURL(/\/dashboard/)
  })

  test('place management loads tree and statistics', async ({ page }) => {
    await page.goto('/place/management')
    await expect(page.locator('text=场所树')).toBeVisible()
    // 验证统计条
    await expect(page.locator('text=总数')).toBeVisible()
  })

  test('place type config page accessible', async ({ page }) => {
    await page.goto('/system/entity-types')
    await expect(page.locator('text=实体类型')).toBeVisible({ timeout: 5000 })
  })
})
```

**Step 2-3: Run + Commit**

---

## Phase 3: CI 硬化

目标：把质量检查从"可选"变成"强制"，防止新 bug 合入。

### Task 3.1: 启用 code-quality profile 为默认构建的一部分

**Files:**
- Modify: `.github/workflows/ci.yml`

**Step 1: 修改 CI 启用 SpotBugs + Checkstyle 阻断构建**

找到 `backend-quality` job，移除 `continue-on-error: true`（如果有），让 SpotBugs/Checkstyle 失败时 PR 无法合入。

**注意：** 第一次启用时现有代码可能有大量 SpotBugs 警告。策略：
- 先跑一次本地看看有多少警告
- 如果 < 20 个，直接修复
- 如果 > 100 个，先用 `spotbugs-exclude.xml` 排除现有问题（基线），只让新问题阻断

**Step 2: 创建 spotbugs-exclude.xml 基线（如需要）**

**Step 3: Commit**
```bash
git commit -m "chore(ci): enforce SpotBugs + Checkstyle as blocking quality gate"
```

---

### Task 3.2: ArchUnit 测试 DDD 分层

**Files:**
- Modify: `backend/pom.xml` — 添加 archunit 依赖
- Create: `backend/src/test/java/com/school/management/architecture/DddLayerTest.java`

**Step 1: 添加依赖**

```xml
<dependency>
    <groupId>com.tngtech.archunit</groupId>
    <artifactId>archunit-junit5</artifactId>
    <version>1.2.1</version>
    <scope>test</scope>
</dependency>
```

**Step 2: 写架构测试**

```java
package com.school.management.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.library.Architectures;
import org.junit.jupiter.api.Test;

class DddLayerTest {

    @Test
    void dddLayerDependenciesAreRespected() {
        JavaClasses classes = new ClassFileImporter()
            .importPackages("com.school.management");

        Architectures.layeredArchitecture()
            .consideringAllDependencies()
            .layer("Interfaces").definedBy("..interfaces..")
            .layer("Application").definedBy("..application..")
            .layer("Domain").definedBy("..domain..")
            .layer("Infrastructure").definedBy("..infrastructure..")
            .whereLayer("Interfaces").mayNotBeAccessedByAnyLayer()
            .whereLayer("Application").mayOnlyBeAccessedByLayers("Interfaces")
            .whereLayer("Infrastructure").mayNotBeAccessedByAnyLayer()
            // Domain 不能依赖 Infrastructure
            .whereLayer("Domain").mayOnlyBeAccessedByLayers("Application", "Interfaces", "Infrastructure")
            .check(classes);
    }
}
```

**Step 3: Run**
```bash
mvn test -Dtest=DddLayerTest
```

预期：可能会失败！因为现有代码可能已有分层违反。先观察结果，如果违反少（< 5 个），修复后合入。

**Step 4: Commit**
```bash
git commit -m "test: add ArchUnit DDD layer dependency enforcement"
```

---

### Task 3.3: E2E 测试纳入 CI

**Files:**
- Modify: `.github/workflows/ci.yml` — 添加 e2e job

**Step 1: 在 workflow 中添加 E2E job**

```yaml
  e2e:
    runs-on: ubuntu-latest
    needs: [backend-build]
    services:
      mysql:
        image: mysql:8.0
        env:
          MYSQL_ROOT_PASSWORD: test123
          MYSQL_DATABASE: student_management
        ports: ['3306:3306']
      redis:
        image: redis:7
        ports: ['6379:6379']
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { distribution: 'temurin', java-version: '17' }
      - uses: actions/setup-node@v4
        with: { node-version: '20' }
      
      - name: Install frontend deps
        run: cd frontend && npm ci
      
      - name: Install Playwright browsers
        run: cd frontend && npx playwright install --with-deps chromium
      
      - name: Start backend
        run: |
          cd backend
          mvn spring-boot:run -DskipTests &
          # 等待后端起来
          timeout 60 bash -c 'until curl -f http://localhost:8080/api/actuator/health; do sleep 2; done'
        env:
          DB_HOST: localhost
          DB_PASSWORD: test123
      
      - name: Start frontend
        run: |
          cd frontend
          npm run dev &
          sleep 10
      
      - name: Run E2E tests
        run: cd frontend && npx playwright test
      
      - name: Upload test results
        if: failure()
        uses: actions/upload-artifact@v4
        with:
          name: playwright-report
          path: frontend/playwright-report/
```

**Step 2: Commit**
```bash
git commit -m "chore(ci): add E2E test job with Playwright + backend services"
```

---

### Task 3.4: 前端 lint 阻断构建

**Files:**
- Modify: `.github/workflows/ci.yml` — 修改 frontend job

把 lint 和 type-check 从非阻断改为阻断。如果有现存 warnings 要先修复或忽略。

**Step 1: 调整 workflow**
- 移除 `continue-on-error` 标志
- 确保 `npm run lint` 和 `npm run type-check` 失败时 CI 失败

**Step 2: Commit**
```bash
git commit -m "chore(ci): enforce lint and type-check as blocking"
```

---

## Phase 4: Sentry 生产监控

**前置：用户需要先完成以下步骤并提供信息：**
1. 访问 sentry.io 注册免费账号（或使用已有实例）
2. 创建 2 个 projects：
   - `student-management-backend` (Platform: Java/Spring Boot)
   - `student-management-frontend` (Platform: Vue)
3. 从每个 project 的 Settings → Client Keys (DSN) 复制 DSN 字符串
4. 把两个 DSN 提供给我

### Task 4.1: 后端 Sentry 集成

**Files:**
- Modify: `backend/pom.xml`
- Modify: `backend/src/main/resources/application.yml`
- Create: `backend/src/main/java/com/school/management/config/SentryConfig.java`

**Step 1: 添加依赖**

```xml
<dependency>
    <groupId>io.sentry</groupId>
    <artifactId>sentry-spring-boot-starter-jakarta</artifactId>
    <version>7.17.0</version>
</dependency>
```

**Step 2: 配置 application.yml**

```yaml
sentry:
  dsn: ${SENTRY_DSN_BACKEND:}  # 从环境变量读取，dev 环境空值
  environment: ${SPRING_PROFILES_ACTIVE:dev}
  traces-sample-rate: 0.1  # 10% 性能采样
  send-default-pii: false  # 不自动发送个人信息
  in-app-includes:
    - com.school.management
  # 敏感信息过滤
  before-send: sentryBeforeSendCallback
```

**Step 3: 实现 PII 过滤 callback**

```java
package com.school.management.config;

import io.sentry.SentryEvent;
import io.sentry.SentryOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SentryConfig {

    @Bean
    public SentryOptions.BeforeSendCallback beforeSendCallback() {
        return (event, hint) -> {
            // 过滤敏感字段
            if (event.getRequest() != null) {
                var request = event.getRequest();
                if (request.getHeaders() != null) {
                    request.getHeaders().remove("Authorization");
                    request.getHeaders().remove("Cookie");
                }
                // URL 参数中删除 token/password
                if (request.getQueryString() != null) {
                    String qs = request.getQueryString()
                        .replaceAll("(password|token)=[^&]*", "$1=***");
                    request.setQueryString(qs);
                }
            }
            return event;
        };
    }
}
```

**Step 4: 配置环境变量**

告知用户在生产环境设置：
```bash
export SENTRY_DSN_BACKEND=https://...@sentry.io/...
```

**Step 5: Commit**
```bash
git commit -m "feat: integrate Sentry for backend error monitoring with PII filtering"
```

---

### Task 4.2: 前端 Sentry 集成

**Files:**
- Modify: `frontend/package.json` — 添加 @sentry/vue
- Modify: `frontend/src/main.ts` — 初始化 Sentry
- Modify: `frontend/.env.example` — 示例 DSN 变量

**Step 1: 安装**

```bash
cd frontend && npm install @sentry/vue
```

**Step 2: 在 main.ts 初始化**

```typescript
import * as Sentry from '@sentry/vue'

const dsn = import.meta.env.VITE_SENTRY_DSN
if (dsn) {
  Sentry.init({
    app,
    dsn,
    environment: import.meta.env.MODE,
    integrations: [
      Sentry.browserTracingIntegration(),
      Sentry.replayIntegration({
        maskAllText: true,  // 遮罩所有文本（隐私）
        blockAllMedia: true,
      }),
    ],
    tracesSampleRate: 0.1,
    replaysSessionSampleRate: 0,  // 不录制普通会话
    replaysOnErrorSampleRate: 1.0,  // 只在错误时录制
    // 过滤敏感数据
    beforeSend(event) {
      // 移除 cookie、auth headers
      if (event.request?.headers) {
        delete event.request.headers['Authorization']
        delete event.request.headers['Cookie']
      }
      return event
    },
  })
}
```

**Step 3: 配置 .env**

```env
VITE_SENTRY_DSN=
```

**Step 4: Commit**
```bash
git commit -m "feat: integrate Sentry for frontend error monitoring with privacy protections"
```

---

### Task 4.3: 用户上下文关联

让 Sentry 能按用户追踪错误（但不泄露 PII）。

**Files:**
- Modify: `frontend/src/stores/auth.ts` — 登录成功时设置 Sentry 用户
- Modify: `backend/src/main/java/com/school/management/security/JwtAuthenticationFilter.java` — 每次请求设置 Sentry 用户

**Step 1: 前端 — 登录后设置用户上下文**

```typescript
// 在 auth store 的 login 成功后
import * as Sentry from '@sentry/vue'

// 只用 userId，不用 username 或 email（隐私）
Sentry.setUser({ id: String(userInfo.id) })

// 登出时清除
Sentry.setUser(null)
```

**Step 2: 后端 — 认证过滤器中设置**

```java
import io.sentry.Sentry;
import io.sentry.protocol.User;

// 在 JwtAuthenticationFilter 成功解析 token 后
Long userId = ... // from token
User user = new User();
user.setId(userId.toString());
Sentry.setUser(user);
```

**Step 3: Commit**
```bash
git commit -m "feat: link user context to Sentry events (ID only, no PII)"
```

---

## 执行顺序建议

```
Phase 1 (1.1 → 1.6)   — 2-4 小时，最高 ROI，立即做
Phase 3.1 + 3.2       — 1-2 小时，加强现有 CI
Phase 2 (2.1 → 2.3)   — 2-3 小时，E2E 覆盖
Phase 3.3 + 3.4       — 1 小时，CI 完整化
Phase 4 (4.1 → 4.3)   — 1-2 小时，等用户提供 DSN 后做
```

**建议先做 Phase 1 完整 + Phase 3.1 + 3.2**，让测试和质量门禁先到位。然后再铺开 E2E 和 Sentry。

---

## 注意事项

1. **现有 24 个后端测试 + 9 个前端单元测试**不要动，保持兼容
2. **JaCoCo 目标 15%** 不要立即提升到 80%，渐进式提升（每月 +5%）
3. **ArchUnit 测试首次可能失败** — 需要修现有违反，或加临时例外规则
4. **SpotBugs 阻断** 需要先建立基线（spotbugs-exclude.xml），再逐步收紧
5. **E2E 测试依赖环境** — 需要 seed data 或幂等的测试账号（admin/admin123）
6. **Sentry 免费套餐限制** — 5k events/month，个人项目够用
