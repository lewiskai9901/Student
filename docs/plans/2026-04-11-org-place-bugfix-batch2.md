# 组织管理 & 场所管理 Bug修复 Batch 2 Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Fix remaining HIGH/CRITICAL bugs: checkIn/checkOut race condition, splitOrgUnit security hole, tree endpoint missing depth limit.

**Architecture:** Atomic SQL for concurrency, parentId validation for security, parameter-based depth control for performance. Minimal, targeted changes.

**Tech Stack:** Spring Boot 3.2 / MyBatis Plus / Vue 3

---

## Issues

| # | Issue | Severity | Module |
|---|-------|----------|--------|
| 1 | checkIn/checkOut race condition — read-then-check-then-write allows overbooking | **CRITICAL** | Place Backend |
| 2 | splitOrgUnit() doesn't verify child belongs to source — can move arbitrary nodes | **HIGH** | Org Backend |
| 3 | `/v9/places/tree` loads entire tree with no depth limit — OOM risk with large data | **HIGH** | Place Backend |

---

### Task 1: Fix checkIn/checkOut Race Condition with Atomic SQL

**Context:** `UniversalPlaceApplicationService.checkIn()` reads `currentOccupancy`, checks `< capacity`, then increments. Two concurrent requests can both pass the check and cause overbooking. Fix: use atomic SQL UPDATE with a WHERE condition so the DB itself enforces the capacity constraint.

**Files:**
- Modify: `backend/src/main/java/com/school/management/infrastructure/persistence/place/UniversalPlaceMapper.java`
- Modify: `backend/src/main/java/com/school/management/infrastructure/persistence/place/UniversalPlaceRepositoryImpl.java`
- Modify: `backend/src/main/java/com/school/management/domain/place/repository/UniversalPlaceRepository.java`
- Modify: `backend/src/main/java/com/school/management/application/place/UniversalPlaceApplicationService.java`

**Step 1: Add atomic increment/decrement SQL to mapper**

In `UniversalPlaceMapper.java`, add two methods:

```java
import org.apache.ibatis.annotations.Update;

/**
 * 原子递增占用数（带容量检查），返回受影响行数。
 * 如果返回 0，说明场所已满或不存在。
 */
@Update("UPDATE places SET current_occupancy = COALESCE(current_occupancy, 0) + 1 " +
        "WHERE id = #{id} AND deleted = 0 " +
        "AND (capacity IS NULL OR capacity = 0 OR COALESCE(current_occupancy, 0) < capacity)")
int atomicIncrementOccupancy(@Param("id") Long id);

/**
 * 原子递减占用数（不低于0），返回受影响行数。
 * 如果返回 0，说明已经为0或不存在。
 */
@Update("UPDATE places SET current_occupancy = COALESCE(current_occupancy, 0) - 1 " +
        "WHERE id = #{id} AND deleted = 0 " +
        "AND COALESCE(current_occupancy, 0) > 0")
int atomicDecrementOccupancy(@Param("id") Long id);
```

**Step 2: Add methods to repository interface**

In `UniversalPlaceRepository.java`, add:

```java
/**
 * 原子递增占用数，成功返回 true，场所已满返回 false
 */
boolean atomicIncrementOccupancy(Long placeId);

/**
 * 原子递减占用数，成功返回 true，已为0返回 false
 */
boolean atomicDecrementOccupancy(Long placeId);
```

**Step 3: Implement in repository impl**

In `UniversalPlaceRepositoryImpl.java`, add:

```java
@Override
public boolean atomicIncrementOccupancy(Long placeId) {
    return mapper.atomicIncrementOccupancy(placeId) > 0;
}

@Override
public boolean atomicDecrementOccupancy(Long placeId) {
    return mapper.atomicDecrementOccupancy(placeId) > 0;
}
```

**Step 4: Refactor checkIn() to use atomic operation**

In `UniversalPlaceApplicationService.java`, in the `checkIn()` method (around line 628-714):

Replace the capacity check + domain checkIn block (lines 634-687):
```java
// 验证容量
if (place.getCapacity() != null && place.getCapacity() > 0) {
    int current = place.getCurrentOccupancy() != null ? place.getCurrentOccupancy() : 0;
    if (current >= place.getCapacity()) {
        throw new IllegalStateException("场所已满，无法入住");
    }
}
```
...and later:
```java
place.checkIn();
placeRepository.save(place);
```

With: Remove the manual capacity check (lines 634-639). After saving the occupant (line 683), replace `place.checkIn(); placeRepository.save(place);` with:

```java
// 原子递增占用数（数据库级并发安全）
if (!placeRepository.atomicIncrementOccupancy(placeId)) {
    // 回滚占用记录
    occupantRepository.deleteById(occupant.getId());
    throw new IllegalStateException("场所已满，无法入住");
}
```

**Step 5: Refactor checkOut() similarly**

In `checkOut()` method (around line 726-771):

Replace:
```java
place.checkOut();
placeRepository.save(place);
```

With:
```java
// 原子递减占用数
placeRepository.atomicDecrementOccupancy(placeId);
```

**Step 6: Commit**

```
fix: use atomic SQL for checkIn/checkOut to prevent race condition overbooking
```

---

### Task 2: Fix splitOrgUnit() Missing Child Validation

**Context:** `OrgUnitDomainService.splitOrgUnit()` at line 249-255 moves children by ID without verifying they belong to the source org. An attacker could pass arbitrary childIds to steal nodes from other branches.

**Files:**
- Modify: `backend/src/main/java/com/school/management/domain/organization/service/OrgUnitDomainService.java:248-255`

**Step 1: Add parentId validation**

In `splitOrgUnit()`, replace the child-moving block (lines 248-255):

```java
if (spec.childIds != null) {
    for (Long childId : spec.childIds) {
        orgUnitRepository.findById(childId).ifPresent(child -> {
            child.moveToParent(savedUnit.getId(), createdBy);
            child.setTreePosition(savedUnit.getTreePath(), savedUnit.getTreeLevel());
            orgUnitRepository.save(child);
        });
    }
}
```

With:

```java
if (spec.childIds != null) {
    for (Long childId : spec.childIds) {
        orgUnitRepository.findById(childId).ifPresent(child -> {
            if (!sourceId.equals(child.getParentId())) {
                throw new IllegalArgumentException(
                    "子组织 " + childId + " 不属于源组织 " + sourceId + "，无法拆分");
            }
            child.moveToParent(savedUnit.getId(), createdBy);
            child.setTreePosition(savedUnit.getTreePath(), savedUnit.getTreeLevel());
            orgUnitRepository.save(child);
        });
    }
}
```

**Step 2: Commit**

```
fix: validate child belongs to source org before split to prevent unauthorized moves
```

---

### Task 3: Add Depth Limit to Place Tree Endpoint

**Context:** `GET /v9/places/tree` loads the entire place tree into memory. With 100k+ nodes, this causes OOM. Fix: add optional `maxDepth` parameter (default 3) to limit tree depth. Children beyond the depth are not loaded.

**Files:**
- Modify: `backend/src/main/java/com/school/management/interfaces/rest/place/UniversalPlaceController.java:32-37`
- Modify: `backend/src/main/java/com/school/management/application/place/UniversalPlaceApplicationService.java` (getPlaceTree method)

**Step 1: Add maxDepth parameter to controller**

In `UniversalPlaceController.java`, change:

```java
@GetMapping("/tree")
@Operation(summary = "获取空间树")
@CasbinAccess(resource = "place", action = "view")
public Result<List<PlaceTreeNode>> getPlaceTree() {
    return Result.success(placeService.getPlaceTree());
}
```

To:

```java
@GetMapping("/tree")
@Operation(summary = "获取空间树")
@CasbinAccess(resource = "place", action = "view")
public Result<List<PlaceTreeNode>> getPlaceTree(
        @RequestParam(defaultValue = "0") int maxDepth) {
    return Result.success(placeService.getPlaceTree(maxDepth));
}
```

`maxDepth=0` means unlimited (backward compatible default).

**Step 2: Add overloaded method in application service**

In `UniversalPlaceApplicationService.java`, find the `getPlaceTree()` method and add an overload:

```java
public List<PlaceTreeNode> getPlaceTree(int maxDepth) {
    if (maxDepth <= 0) {
        return getPlaceTree(); // unlimited, original behavior
    }
    List<UniversalPlace> roots = placeRepository.findRoots();
    return roots.stream()
            .map(r -> buildTreeNodeWithDepth(r, 0, maxDepth))
            .collect(Collectors.toList());
}
```

And add the depth-limited tree builder:

```java
private PlaceTreeNode buildTreeNodeWithDepth(UniversalPlace place, int currentDepth, int maxDepth) {
    PlaceTreeNode node = buildTreeNodeBase(place);
    if (currentDepth < maxDepth) {
        List<UniversalPlace> children = placeRepository.findChildren(place.getId());
        if (!children.isEmpty()) {
            node.setChildren(children.stream()
                    .map(c -> buildTreeNodeWithDepth(c, currentDepth + 1, maxDepth))
                    .collect(Collectors.toList()));
        }
    }
    // When at maxDepth, node.children stays null (signals "has more" to frontend)
    return node;
}
```

Note: `buildTreeNodeBase` should be extracted from the existing `buildTreeNode` method — it does all the field mapping without the recursive children loading. If the existing `buildTreeNode` already has a base portion, reuse that. If not, extract the non-recursive field mapping into `buildTreeNodeBase`.

**Step 3: Commit**

```
perf: add maxDepth parameter to place tree endpoint to prevent OOM on large datasets
```
