# Place 领域架构重构 Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 彻底清理 Place 双聚合根死代码、修复领域事件丢失架构缺陷、增加占用数对账机制。

**Architecture:** 三层重构 — (1) 删除全部旧 Place 生态链 ~50 文件 (2) 在 Repository.save() 中原子化发布领域事件 (3) 添加定时对账 Job 校正 currentOccupancy。

**Tech Stack:** Spring Boot 3.2 / MyBatis Plus / Spring Scheduling

---

## Task 1: 删除旧 Place 死代码

**Context:** 旧 Place 系统（PlaceController `/places`、PlaceApplicationService、Place.java 聚合根等）已被 UniversalPlace 体系完全替代。前端零调用旧端点。两套代码共用同一张 `places` 表。约 50 个文件、~5500 行死代码。

**删除清单：**

**域层 (domain/place/):**
- `model/aggregate/Place.java`
- `model/valueobject/Capacity.java`
- `model/valueobject/PlacePath.java`
- `model/valueobject/BuildingType.java`
- `model/valueobject/RoomType.java`
- `model/entity/PlaceClassAssignment.java`
- `repository/PlaceRepository.java`
- `repository/PlaceClassAssignmentRepository.java`

**应用层 (application/place/):**
- `PlaceApplicationService.java`
- `PlaceBookingApplicationService.java`
- `PlaceCapacityAlertService.java`
- `PlaceCategoryApplicationService.java`
- `PlaceBatchJobService.java`
- `PlaceBatchJobExecutor.java`
- `PlaceBatchItemProcessor.java`
- `command/CreatePlaceCommand.java`
- `command/UpdatePlaceCommand.java`
- `command/CreatePlaceCategoryCommand.java`
- `command/UpdatePlaceCategoryCommand.java`
- `command/BatchAssignOrgCommand.java`
- `command/BatchAssignResponsibleCommand.java`
- `command/BatchChangeStatusCommand.java`
- `command/CheckInCommand.java` (旧版，注意 UniversalPlaceApplicationService 内部有自己的 CheckInCommand 内部类)
- `query/PlaceDTO.java` (旧版)
- `query/PlaceOccupantDTO.java`
- `query/PlaceQueryCriteria.java`
- `query/PlaceStatisticsDTO.java`
- `query/PlaceCategoryDTO.java`
- `query/CapacityAlertDTO.java`

**基础设施层 (infrastructure/persistence/place/):**
- `PlaceRepositoryImpl.java`
- `PlaceMapper.java`
- `PlacePO.java`
- `PlaceClassAssignmentRepositoryImpl.java`
- `PlaceClassAssignmentMapper.java`
- `PlaceClassAssignmentPO.java`
- `PlaceCategoryRepositoryImpl.java`
- `PlaceCategoryMapper.java`
- `PlaceCategoryPO.java`
- `PlaceBatchJobMapper.java`
- `PlaceBatchJobPO.java`
- `PlaceBatchJobItemMapper.java`
- `PlaceBatchJobItemPO.java`

**资源文件:**
- `resources/mapper/PlaceMapper.xml`

**接口层 (interfaces/rest/place/):**
- `PlaceController.java`
- `PlaceBookingController.java`
- `PlaceCapacityAlertController.java`
- `PlaceCategoryController.java`
- `PlaceBatchJobController.java`
- `dto/CreatePlaceRequest.java`
- `dto/UpdatePlaceRequest.java`

**Step 1:** 删除上述所有文件。注意：
- 保留 `PlaceStatus.java`（被 UniversalPlace 使用）
- 保留 `PlaceType.java` 枚举（如果 UniversalPlace 引用了它）
- 保留 `GenderType.java`（如果被共用）
- 删除前先 grep 每个文件名确认没有被 Universal* 代码引用

**Step 2:** 编译检查 — 运行 `mvn compile -DskipTests`。如果有编译错误，说明有遗漏的引用需要清理。

**Step 3:** 检查前端是否有任何对旧路径 `/places/` (非 `/v9/places/`) 的引用，清理。

**Step 4:** Commit
```
refactor: remove entire legacy Place system (~50 files, ~5500 lines of dead code)
```

---

## Task 2: Repository 层原子化领域事件发布

**Context:** `UniversalPlaceRepositoryImpl.save()` 在第 54 行通过 `toDomain(saved)` 返回新对象，原始聚合根上注册的领域事件丢失。目前的 workaround 是应用层在 save() 前手动调用 `publishEvents()`，但这是脆弱的约定（忘调就丢事件）。

**对比：** `OrgUnitRepositoryImpl.save()` 直接返回原始对象，不存在此问题。

**根本修复：** 让 `save()` 方法自动提取并发布事件，在返回新对象前完成。应用层的 `publishEvents()` 全部移除。

**核心原则：** 持久化 = 保存状态 + 发布事件（一个原子操作，同一事务）。

**Files:**
- Modify: `backend/src/main/java/com/school/management/infrastructure/persistence/place/UniversalPlaceRepositoryImpl.java`
- Modify: `backend/src/main/java/com/school/management/application/place/UniversalPlaceApplicationService.java` (删除所有 publishEvents 调用)

**Step 1: 修改 UniversalPlaceRepositoryImpl.save()**

在 `save()` 方法中，在执行 `toDomain(saved)` 之前，提取并发布事件：

```java
@Override
public UniversalPlace save(UniversalPlace place) {
    // 1. 提取领域事件（在持久化之前，因为 toDomain 会创建新对象）
    List<DomainEvent> events = place.getDomainEvents();
    
    // 2. 持久化状态
    UniversalPlacePO po = toPO(place);
    if (po.getId() == null) {
        mapper.insert(po);
        place.setId(po.getId());
        if (place.getParentId() == null) {
            place.setPath("/" + po.getId() + "/");
            place.setLevel(0);
        }
        po = toPO(place);
        mapper.updateById(po);
    } else {
        int rows = mapper.updateById(po);
        if (rows == 0) {
            throw new IllegalStateException("更新场所失败：未找到 ID=" + po.getId() + " 的记录");
        }
    }
    
    // 3. 发布领域事件（同一事务内，使用 outbox 模式）
    if (!events.isEmpty()) {
        events.forEach(eventPublisher::publish);
        place.clearDomainEvents();
    }
    
    // 4. 重新查询返回最新数据
    UniversalPlacePO saved = mapper.selectById(po.getId());
    return saved != null ? toDomain(saved) : toDomain(po);
}
```

需要在 `UniversalPlaceRepositoryImpl` 中注入 `DomainEventPublisher`。找到该类的构造函数或字段注入，添加：
```java
private final DomainEventPublisher eventPublisher;
```

**Step 2: 删除 UniversalPlaceApplicationService 中的所有 publishEvents 调用**

在 `UniversalPlaceApplicationService.java` 中：
- 删除 `publishEvents()` 私有方法（约 lines 395-398）
- 删除所有 `publishEvents(place)` 调用点（约 lines 288, 342, 358，以及其他位置）
- 因为 save() 现在自动发布事件，应用层不再需要关心

**Step 3:** 编译检查 `mvn compile -DskipTests`

**Step 4:** Commit
```
refactor: move domain event publishing into repository save() for atomic state+event persistence
```

---

## Task 3: 占用数定时对账 Job

**Context:** `places.current_occupancy` 是反范式缓存字段，通过 atomic SQL 维护。但如果有直接 SQL 修改、数据迁移、或极端异常导致 occupant 记录和 counter 不同步，就会出现数据偏差。`countActiveByPlaceId()` 方法已存在。

**Files:**
- Create: `backend/src/main/java/com/school/management/infrastructure/scheduler/PlaceOccupancyReconciliationJob.java`
- Modify: `backend/src/main/java/com/school/management/infrastructure/persistence/place/UniversalPlaceMapper.java` (添加批量对账 SQL)

**Step 1: 添加对账查询 SQL**

在 `UniversalPlaceMapper.java` 中添加一条查询，返回所有 currentOccupancy 与实际占用数不匹配的场所：

```java
@Select("SELECT p.id, p.current_occupancy AS storedCount, " +
        "COALESCE(oc.actual_count, 0) AS actualCount " +
        "FROM places p " +
        "LEFT JOIN (" +
        "  SELECT place_id, COUNT(*) AS actual_count " +
        "  FROM place_occupants " +
        "  WHERE status = 1 AND deleted = 0 " +
        "  GROUP BY place_id" +
        ") oc ON p.id = oc.place_id " +
        "WHERE p.deleted = 0 " +
        "AND COALESCE(p.current_occupancy, 0) != COALESCE(oc.actual_count, 0)")
List<Map<String, Object>> findOccupancyMismatches();
```

添加修正 SQL:
```java
@Update("UPDATE places SET current_occupancy = #{actualCount} WHERE id = #{placeId} AND deleted = 0")
int fixOccupancy(@Param("placeId") Long placeId, @Param("actualCount") int actualCount);
```

**Step 2: 创建对账 Job**

```java
package com.school.management.infrastructure.scheduler;

import com.school.management.infrastructure.persistence.place.UniversalPlaceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 场所占用数定时对账任务
 * 每小时检查 current_occupancy 与 place_occupants 实际记录数是否一致，不一致则修正。
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class PlaceOccupancyReconciliationJob {

    private final UniversalPlaceMapper placeMapper;

    @Scheduled(cron = "0 0 * * * *")  // 每小时整点执行
    @Transactional
    public void reconcile() {
        List<Map<String, Object>> mismatches = placeMapper.findOccupancyMismatches();
        if (mismatches.isEmpty()) {
            log.debug("场所占用数对账：全部一致");
            return;
        }

        log.warn("场所占用数对账：发现 {} 处偏差，开始修正", mismatches.size());
        int fixed = 0;
        for (Map<String, Object> row : mismatches) {
            Long placeId = ((Number) row.get("id")).longValue();
            int stored = ((Number) row.get("storedCount")).intValue();
            int actual = ((Number) row.get("actualCount")).intValue();
            
            placeMapper.fixOccupancy(placeId, actual);
            log.info("修正场所 {} 占用数: {} → {}", placeId, stored, actual);
            fixed++;
        }
        log.warn("场所占用数对账完成：修正 {} 处", fixed);
    }
}
```

**Step 3:** 确保 `@EnableScheduling` 已在主应用类上启用（检查 `Application.java`）。

**Step 4:** Commit
```
feat: add hourly occupancy reconciliation job to prevent counter drift
```

---

## 执行顺序

Task 1 → Task 2 → Task 3（严格顺序，因为 Task 1 删除旧代码减少干扰，Task 2 改事件架构，Task 3 加安全网）。

Task 1 最大（大量文件删除），Task 2 最关键（架构修复），Task 3 最独立。
