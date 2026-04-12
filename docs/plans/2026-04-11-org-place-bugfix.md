# 组织管理 & 场所管理 Bug修复 Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Fix critical bugs in org management and place management modules identified through deep code + UI analysis.

**Architecture:** Fix focused on 4 areas: (1) data corruption in removeMember, (2) sidebar label rendering, (3) N+1 queries, (4) redirect route visibility in menu. Minimal, targeted changes — no refactoring beyond what's needed.

**Tech Stack:** Spring Boot 3.2 / MyBatis Plus / Vue 3 + TypeScript

---

## Verified Issues Summary

| # | Issue | Severity | Module |
|---|-------|----------|--------|
| 1 | `removeMember()` deletes ALL org relations, not just the specific user | **CRITICAL** | Backend |
| 2 | Sidebar shows raw path for redirect routes (e.g. `/system/org-types`) | **MEDIUM** | Frontend |
| 3 | N+1 query in `getMembersRecursive()` — loops findById per descendant | **MEDIUM** | Backend |
| 4 | N+1 query in `findAncestors()` — loops findById per ancestor | **MEDIUM** | Backend |

**NOT a bug (verified):** Tree node click in org/place management — works correctly; previous test failure was Chrome extension automation artifact.

---

### Task 1: Fix `removeMember()` Data Corruption Bug

**Context:** When removing a single user from an org unit, `OrgMemberService.removeMember()` at line 141 calls `accessRelationRepository.deleteByResource("org_unit", orgUnitId)` which deletes **ALL** access relations for that org unit — removing every user, not just the target one. The `AccessRelationRepository` interface lacks a method to delete by both resource AND subject.

**Files:**
- Modify: `backend/src/main/java/com/school/management/domain/access/repository/AccessRelationRepository.java`
- Modify: `backend/src/main/java/com/school/management/infrastructure/persistence/access/AccessRelationRepositoryImpl.java`
- Modify: `backend/src/main/java/com/school/management/infrastructure/persistence/access/AccessRelationMapper.java`
- Modify: `backend/src/main/java/com/school/management/application/organization/OrgMemberService.java:141`

**Step 1: Add `deleteByResourceAndSubject` to repository interface**

In `AccessRelationRepository.java`, add after `deleteBySubject`:

```java
/**
 * 删除指定资源与指定主体之间的所有关系记录
 */
void deleteByResourceAndSubject(String resourceType, Long resourceId, String subjectType, Long subjectId);
```

**Step 2: Implement in `AccessRelationRepositoryImpl.java`**

Add method:

```java
@Override
public void deleteByResourceAndSubject(String resourceType, Long resourceId, String subjectType, Long subjectId) {
    LambdaQueryWrapper<AccessRelationPO> wrapper = new LambdaQueryWrapper<AccessRelationPO>()
            .eq(AccessRelationPO::getResourceType, resourceType)
            .eq(AccessRelationPO::getResourceId, resourceId)
            .eq(AccessRelationPO::getSubjectType, subjectType)
            .eq(AccessRelationPO::getSubjectId, subjectId);
    mapper.delete(wrapper);
}
```

**Step 3: Fix `OrgMemberService.removeMember()`**

Change line 141 from:
```java
accessRelationRepository.deleteByResource("org_unit", orgUnitId);
```
To:
```java
accessRelationRepository.deleteByResourceAndSubject("org_unit", orgUnitId, "user", userId);
```

**Step 4: Verify no other callers misuse `deleteByResource`**

Check: `endAllByOrgUnitId()` at line 152 correctly uses `deleteByResource` because it **intends** to remove all relations when dissolving an org. That usage is correct.

**Step 5: Commit**

```bash
git add backend/src/main/java/com/school/management/domain/access/repository/AccessRelationRepository.java \
       backend/src/main/java/com/school/management/infrastructure/persistence/access/AccessRelationRepositoryImpl.java \
       backend/src/main/java/com/school/management/application/organization/OrgMemberService.java
git commit -m "fix: removeMember() now only deletes the specific user's relation, not all org relations"
```

---

### Task 2: Fix Sidebar Showing Raw Paths for Redirect Routes

**Context:** `menu-generator.ts:60` falls back to `route.path` when `route.meta?.title` is undefined. Redirect routes at `router/index.ts:919-921` have no `meta`, so `/system/org-types` appears as the sidebar label. Fix: add `meta: { hidden: true }` to redirect routes.

**Files:**
- Modify: `frontend/src/router/index.ts:919-921`

**Step 1: Add hidden meta to redirect routes**

Change:
```typescript
{ path: '/system/org-types', redirect: '/system/entity-types' },
{ path: '/system/place-types', redirect: '/system/entity-types' },
{ path: '/system/user-types', redirect: '/system/entity-types' },
```
To:
```typescript
{ path: '/system/org-types', redirect: '/system/entity-types', meta: { hidden: true } },
{ path: '/system/place-types', redirect: '/system/entity-types', meta: { hidden: true } },
{ path: '/system/user-types', redirect: '/system/entity-types', meta: { hidden: true } },
```

**Step 2: Verify in browser**

Open sidebar → System Management section. The three raw-path entries should no longer appear.

**Step 3: Commit**

```bash
git add frontend/src/router/index.ts
git commit -m "fix: hide redirect routes from sidebar menu"
```

---

### Task 3: Fix N+1 Query in `getMembersRecursive()`

**Context:** `OrgMemberService.getMembersRecursive()` lines 66-70 loops `findById(descId)` for each descendant to build a name map. With 100 descendants, this creates 100 queries.

**Files:**
- Modify: `backend/src/main/java/com/school/management/domain/organization/repository/OrgUnitRepository.java`
- Modify: `backend/src/main/java/com/school/management/infrastructure/persistence/organization/OrgUnitRepositoryImpl.java`
- Modify: `backend/src/main/java/com/school/management/infrastructure/persistence/organization/OrgUnitMapper.java`
- Modify: `backend/src/main/java/com/school/management/application/organization/OrgMemberService.java:66-70`

**Step 1: Add `findByIds` to OrgUnitRepository interface**

```java
List<OrgUnit> findByIds(Collection<Long> ids);
```

**Step 2: Implement in OrgUnitRepositoryImpl**

```java
@Override
public List<OrgUnit> findByIds(Collection<Long> ids) {
    if (ids == null || ids.isEmpty()) return Collections.emptyList();
    List<OrgUnitPO> poList = mapper.selectBatchIds(ids);
    return poList.stream().map(this::toDomain).collect(Collectors.toList());
}
```

(`selectBatchIds` is built-in MyBatis Plus — no mapper XML needed.)

**Step 3: Replace N+1 loop in OrgMemberService**

Replace lines 64-70:
```java
Map<Long, String> orgNameMap = new HashMap<>();
orgNameMap.put(orgUnitId, orgUnit.getUnitName());
for (Long descId : allOrgIds) {
    if (!orgNameMap.containsKey(descId)) {
        orgUnitRepository.findById(descId).ifPresent(o -> orgNameMap.put(o.getId(), o.getUnitName()));
    }
}
```

With:
```java
List<OrgUnit> allOrgs = orgUnitRepository.findByIds(allOrgIds);
Map<Long, String> orgNameMap = allOrgs.stream()
        .collect(Collectors.toMap(OrgUnit::getId, OrgUnit::getUnitName, (a, b) -> a));
```

**Step 4: Commit**

```bash
git add backend/src/main/java/com/school/management/domain/organization/repository/OrgUnitRepository.java \
       backend/src/main/java/com/school/management/infrastructure/persistence/organization/OrgUnitRepositoryImpl.java \
       backend/src/main/java/com/school/management/application/organization/OrgMemberService.java
git commit -m "perf: replace N+1 findById loop with batch findByIds in getMembersRecursive"
```

---

### Task 4: Fix N+1 Query in `findAncestors()`

**Context:** `OrgUnitRepositoryImpl.findAncestors()` lines 109-131 parses `treePath` then loops `findById` for each ancestor ID. Same N+1 pattern.

**Files:**
- Modify: `backend/src/main/java/com/school/management/infrastructure/persistence/organization/OrgUnitRepositoryImpl.java:109-131`

**Step 1: Replace N+1 with batch query**

Replace the method body:

```java
@Override
public List<OrgUnit> findAncestors(Long orgUnitId) {
    Optional<OrgUnit> orgUnit = findById(orgUnitId);
    if (orgUnit.isEmpty() || orgUnit.get().getTreePath() == null) {
        return new ArrayList<>();
    }

    String treePath = orgUnit.get().getTreePath();
    List<Long> ancestorIds = Arrays.stream(treePath.split("/"))
            .filter(s -> !s.isEmpty())
            .map(Long::parseLong)
            .filter(id -> !id.equals(orgUnitId))
            .collect(Collectors.toList());

    if (ancestorIds.isEmpty()) {
        return new ArrayList<>();
    }

    return findByIds(ancestorIds);
}
```

**Step 2: Commit**

```bash
git add backend/src/main/java/com/school/management/infrastructure/persistence/organization/OrgUnitRepositoryImpl.java
git commit -m "perf: replace N+1 findById loop with batch query in findAncestors"
```

---

## Execution Notes

- **Backend restart required** after Tasks 1, 3, 4. Auto-restart per project convention.
- **Frontend hot-reload** handles Task 2 automatically.
- Tasks 1-4 are independent and can be executed in parallel by separate agents.
