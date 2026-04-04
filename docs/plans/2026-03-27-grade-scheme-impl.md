# GradeScheme 等级方案 Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Introduce GradeScheme as a first-class business concept — customizable grade mapping names and grade names per template section, displayed in project summaries.

**Architecture:** New `GradeScheme` aggregate root with `GradeDefinition` children. TemplateSection references a scheme via `gradeSchemeId`. ScoreAggregationService prefers GradeScheme over legacy GradeBand. ProjectScore snapshots scheme display name.

**Tech Stack:** Spring Boot 3.2, MyBatis Plus 3.5.7, Vue 3 + TypeScript, Element Plus

**Design doc:** `docs/plans/2026-03-27-grade-scheme-design.md`

---

### Task 1: Database Migration

**Files:**
- Create: `database/schema/V67.0.0__grade_scheme.sql`

**Step 1: Write the migration SQL**

```sql
-- V67.0.0 等级方案
CREATE TABLE IF NOT EXISTS `insp_grade_schemes` (
    `id`              BIGINT          NOT NULL AUTO_INCREMENT,
    `tenant_id`       BIGINT          NOT NULL DEFAULT 0,
    `display_name`    VARCHAR(100)    NOT NULL COMMENT '方案显示名称',
    `description`     VARCHAR(500)    DEFAULT NULL COMMENT '方案描述',
    `scheme_type`     VARCHAR(20)     NOT NULL DEFAULT 'SCORE_RANGE' COMMENT 'SCORE_RANGE|PERCENT_RANGE',
    `is_system`       TINYINT(1)      NOT NULL DEFAULT 0,
    `created_by`      BIGINT          DEFAULT NULL,
    `created_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`         TINYINT(1)      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    INDEX `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='等级方案';

CREATE TABLE IF NOT EXISTS `insp_grade_definitions` (
    `id`                BIGINT          NOT NULL AUTO_INCREMENT,
    `grade_scheme_id`   BIGINT          NOT NULL,
    `code`              VARCHAR(50)     NOT NULL COMMENT '等级编码',
    `name`              VARCHAR(100)    NOT NULL COMMENT '等级名称',
    `min_value`         DECIMAL(10,2)   NOT NULL,
    `max_value`         DECIMAL(10,2)   NOT NULL,
    `color`             VARCHAR(20)     DEFAULT NULL,
    `icon`              VARCHAR(50)     DEFAULT NULL,
    `sort_order`        INT             NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    INDEX `idx_scheme` (`grade_scheme_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='等级定义';

-- TemplateSection 新增字段
ALTER TABLE `insp_template_sections`
    ADD COLUMN `grade_scheme_id` BIGINT DEFAULT NULL COMMENT 'FK → insp_grade_schemes.id';

-- ProjectScore 新增快照字段
ALTER TABLE `insp_project_scores`
    ADD COLUMN `grade_scheme_display_name` VARCHAR(100) DEFAULT NULL COMMENT '等级方案名称快照',
    ADD COLUMN `grade_name` VARCHAR(100) DEFAULT NULL COMMENT '等级名称快照';

-- 系统预设: 标准五级
INSERT INTO `insp_grade_schemes` (`tenant_id`, `display_name`, `scheme_type`, `is_system`) VALUES
(0, '标准五级评定', 'PERCENT_RANGE', 1),
(0, '合格评定', 'PERCENT_RANGE', 1),
(0, '流动红旗', 'PERCENT_RANGE', 1),
(0, '星级评定', 'PERCENT_RANGE', 1);

SET @five_id = (SELECT id FROM insp_grade_schemes WHERE display_name = '标准五级评定' AND is_system = 1 LIMIT 1);
SET @pass_id = (SELECT id FROM insp_grade_schemes WHERE display_name = '合格评定' AND is_system = 1 LIMIT 1);
SET @flag_id = (SELECT id FROM insp_grade_schemes WHERE display_name = '流动红旗' AND is_system = 1 LIMIT 1);
SET @star_id = (SELECT id FROM insp_grade_schemes WHERE display_name = '星级评定' AND is_system = 1 LIMIT 1);

INSERT INTO `insp_grade_definitions` (`grade_scheme_id`, `code`, `name`, `min_value`, `max_value`, `color`, `sort_order`) VALUES
(@five_id, 'A', '优秀', 90, 100, '#22C55E', 1),
(@five_id, 'B', '良好', 80, 89.99, '#3B82F6', 2),
(@five_id, 'C', '中等', 70, 79.99, '#F59E0B', 3),
(@five_id, 'D', '及格', 60, 69.99, '#F97316', 4),
(@five_id, 'F', '不及格', 0, 59.99, '#EF4444', 5);

INSERT INTO `insp_grade_definitions` (`grade_scheme_id`, `code`, `name`, `min_value`, `max_value`, `color`, `sort_order`) VALUES
(@pass_id, 'PASS', '合格', 60, 100, '#22C55E', 1),
(@pass_id, 'FAIL', '不合格', 0, 59.99, '#EF4444', 2);

INSERT INTO `insp_grade_definitions` (`grade_scheme_id`, `code`, `name`, `min_value`, `max_value`, `color`, `icon`, `sort_order`) VALUES
(@flag_id, 'RED',    '红旗', 90, 100,   '#EF4444', 'flag', 1),
(@flag_id, 'BLUE',   '蓝旗', 70, 89.99, '#3B82F6', 'flag', 2),
(@flag_id, 'YELLOW', '黄旗', 0,  69.99, '#F59E0B', 'flag', 3);

INSERT INTO `insp_grade_definitions` (`grade_scheme_id`, `code`, `name`, `min_value`, `max_value`, `color`, `icon`, `sort_order`) VALUES
(@star_id, '5STAR', '五星', 90, 100,   '#FFD700', 'star', 1),
(@star_id, '4STAR', '四星', 80, 89.99, '#FFA500', 'star', 2),
(@star_id, '3STAR', '三星', 70, 79.99, '#C0C0C0', 'star', 3),
(@star_id, '2STAR', '二星', 60, 69.99, '#CD7F32', 'star', 4),
(@star_id, '1STAR', '一星', 0,  59.99, '#808080', 'star', 5);
```

**Step 2: Apply migration**

Run: `mysql -u root -p123456 student_management < database/schema/V67.0.0__grade_scheme.sql`

**Step 3: Commit**

```
feat: add grade scheme database tables and system presets
```

---

### Task 2: Domain Models — GradeScheme + GradeDefinition

**Files:**
- Create: `backend/src/main/java/com/school/management/domain/inspection/model/v7/scoring/GradeScheme.java`
- Create: `backend/src/main/java/com/school/management/domain/inspection/model/v7/scoring/GradeDefinition.java`
- Create: `backend/src/main/java/com/school/management/domain/inspection/model/v7/scoring/GradeSchemeSnapshot.java`

**Reference pattern:** `GradeBand.java` (same package — builder + reconstruct + Entity interface)

**Step 1: Create GradeDefinition entity**

Follow the exact same pattern as `GradeBand.java`:
- `implements Entity<Long>`
- Private constructor with Builder
- `reconstruct(Builder)` factory
- Manual getters (no Lombok)
- Fields: id, gradeSchemeId, code, name, minValue(BigDecimal), maxValue(BigDecimal), color, icon, sortOrder

**Step 2: Create GradeScheme aggregate root**

Follow the exact same pattern as `GradeBand.java` but as aggregate root:
- `implements Entity<Long>` (note: existing AggregateRoot in this project is abstract generic, but GradeBand/ProjectScore use Entity<Long> — follow that pattern)
- Fields: id, tenantId, displayName, description, schemeType(String), isSystem(Boolean), createdBy, createdAt, updatedAt
- Transient field: `grades: List<GradeDefinition>` (loaded separately)
- Factory: `create(tenantId, displayName, schemeType, createdBy)`
- Methods: `updateInfo(displayName, description)`, `setGradeSchemeId(Long)` on definitions, `matchGrade(BigDecimal score)`, `matchGrade(BigDecimal score, BigDecimal maxScore)`
- Guard: `guardSystemPreset()` throws on system presets

**Step 3: Create GradeSchemeSnapshot record**

```java
package com.school.management.domain.inspection.model.v7.scoring;

public record GradeSchemeSnapshot(String displayName, String gradeCode, String gradeName, String color, String icon) {}
```

**Step 4: Commit**

```
feat: add GradeScheme and GradeDefinition domain models
```

---

### Task 3: Repository Interface + Infrastructure (PO + Mapper + Impl)

**Files:**
- Create: `backend/src/main/java/com/school/management/domain/inspection/repository/v7/GradeSchemeRepository.java`
- Create: `backend/src/main/java/com/school/management/domain/inspection/repository/v7/GradeDefinitionRepository.java`
- Create: `backend/src/main/java/com/school/management/infrastructure/persistence/inspection/v7/scoring/GradeSchemePO.java`
- Create: `backend/src/main/java/com/school/management/infrastructure/persistence/inspection/v7/scoring/GradeDefinitionPO.java`
- Create: `backend/src/main/java/com/school/management/infrastructure/persistence/inspection/v7/scoring/GradeSchemeMapper.java`
- Create: `backend/src/main/java/com/school/management/infrastructure/persistence/inspection/v7/scoring/GradeDefinitionMapper.java`
- Create: `backend/src/main/java/com/school/management/infrastructure/persistence/inspection/v7/scoring/GradeSchemeRepositoryImpl.java`
- Create: `backend/src/main/java/com/school/management/infrastructure/persistence/inspection/v7/scoring/GradeDefinitionRepositoryImpl.java`

**Reference pattern:** `GradeBandRepository.java` → `GradeBandPO.java` → `GradeBandMapper.java` → `GradeBandRepositoryImpl.java`

**Step 1: Create GradeSchemeRepository interface**

```java
package com.school.management.domain.inspection.repository.v7;

import com.school.management.domain.inspection.model.v7.scoring.GradeScheme;
import java.util.List;
import java.util.Optional;

public interface GradeSchemeRepository {
    GradeScheme save(GradeScheme scheme);
    Optional<GradeScheme> findById(Long id);
    List<GradeScheme> findByTenantIdOrSystem(Long tenantId);
    void deleteById(Long id);
}
```

**Step 2: Create GradeDefinitionRepository interface**

```java
public interface GradeDefinitionRepository {
    GradeDefinition save(GradeDefinition def);
    List<GradeDefinition> findByGradeSchemeId(Long gradeSchemeId);
    void deleteByGradeSchemeId(Long gradeSchemeId);
}
```

**Step 3: Create PO classes**

`GradeSchemePO`:
- `@TableName("insp_grade_schemes")`, `@TableId(type = IdType.AUTO)`
- Fields mirror DB columns: id, tenantId, displayName, description, schemeType, isSystem, createdBy, createdAt, updatedAt
- `@TableLogic private Integer deleted;`

`GradeDefinitionPO`:
- `@TableName("insp_grade_definitions")`, `@TableId(type = IdType.AUTO)`
- Fields: id, gradeSchemeId, code, name, minValue, maxValue, color, icon, sortOrder

**Step 4: Create Mapper interfaces**

`GradeSchemeMapper extends BaseMapper<GradeSchemePO>`:
- `@Select("SELECT * FROM insp_grade_schemes WHERE (tenant_id = #{tenantId} OR is_system = 1) AND deleted = 0 ORDER BY is_system DESC, id")` → `findByTenantIdOrSystem`

`GradeDefinitionMapper extends BaseMapper<GradeDefinitionPO>`:
- `@Select("SELECT * FROM insp_grade_definitions WHERE grade_scheme_id = #{gradeSchemeId} ORDER BY sort_order")` → `findByGradeSchemeId`
- `@Delete("DELETE FROM insp_grade_definitions WHERE grade_scheme_id = #{gradeSchemeId}")` → `deleteByGradeSchemeId`

**Step 5: Create RepositoryImpl classes**

Follow `GradeBandRepositoryImpl.java` pattern exactly:
- Constructor injection of Mapper
- `save()`: insert if id null, updateById otherwise
- `toPO()` / `toDomain()` conversion methods

`GradeSchemeRepositoryImpl`:
- On `save()`, after insert/update, also save grades if present: delete existing definitions, re-insert all
- On `findById()`, also load grades via `GradeDefinitionRepository`

**Step 6: Commit**

```
feat: add GradeScheme repository infrastructure (PO, Mapper, Impl)
```

---

### Task 4: TemplateSection — Add gradeSchemeId Field

**Files:**
- Modify: `backend/src/main/java/com/school/management/domain/inspection/model/v7/template/TemplateSection.java`
  - Add field `private Long gradeSchemeId;` after line 39 (after `scoringConfig`)
  - Add `this.gradeSchemeId = builder.gradeSchemeId;` in constructor after line 70
  - Add getter `public Long getGradeSchemeId() { return gradeSchemeId; }` in getters section
  - Add setter `public void setGradeSchemeId(Long gradeSchemeId) { this.gradeSchemeId = gradeSchemeId; this.updatedAt = LocalDateTime.now(); }` in operations section
  - Add builder field + method in Builder class
- Modify: `backend/src/main/java/com/school/management/infrastructure/persistence/inspection/v7/template/TemplateSectionPO.java`
  - Add field: `private Long gradeSchemeId;`
- Modify: `backend/src/main/java/com/school/management/infrastructure/persistence/inspection/v7/template/TemplateSectionRepositoryImpl.java`
  - Add `gradeSchemeId` to `toPO()` and `toDomain()` conversion methods

**Step 1: Add field to TemplateSection domain model**

In `TemplateSection.java` after line 39 (`private String scoringConfig;`):
```java
private Long gradeSchemeId;           // FK → GradeScheme (等级方案)
```

In constructor after line 70 (`this.scoringConfig = builder.scoringConfig;`):
```java
this.gradeSchemeId = builder.gradeSchemeId;
```

Add setter in operations section (after `updateScoringConfig` method ~line 208):
```java
public void setGradeSchemeId(Long gradeSchemeId) {
    this.gradeSchemeId = gradeSchemeId;
    this.updatedAt = LocalDateTime.now();
}
```

Add getter in getters section (after `getScoringConfig` ~line 247):
```java
public Long getGradeSchemeId() { return gradeSchemeId; }
```

In Builder class add field (after `scoringConfig` ~line 279):
```java
private Long gradeSchemeId;
```

Add builder method (after `scoringConfig` method ~line 308):
```java
public Builder gradeSchemeId(Long gradeSchemeId) { this.gradeSchemeId = gradeSchemeId; return this; }
```

**Step 2: Add field to TemplateSectionPO**

Add after `scoringConfig` field:
```java
@TableField(updateStrategy = FieldStrategy.ALWAYS)
private Long gradeSchemeId;
```

**Step 3: Update TemplateSectionRepositoryImpl toPO/toDomain**

In `toPO()` add:
```java
po.setGradeSchemeId(domain.getGradeSchemeId());
```

In `toDomain()` builder chain add:
```java
.gradeSchemeId(po.getGradeSchemeId())
```

**Step 4: Commit**

```
feat: add gradeSchemeId field to TemplateSection
```

---

### Task 5: ProjectScore — Add Snapshot Fields

**Files:**
- Modify: `backend/src/main/java/com/school/management/domain/inspection/model/v7/execution/ProjectScore.java`
  - Add fields: `gradeSchemeDisplayName`, `gradeName`
  - Add to builder, constructor, getters
  - Add `updateGradeSchemeInfo(displayName, gradeName)` method
- Modify: corresponding `ProjectScorePO.java` (find in infrastructure)
- Modify: corresponding `ProjectScoreRepositoryImpl.java` toPO/toDomain

**Step 1: Add fields to ProjectScore domain**

After line 22 (`private String detail;`):
```java
private String gradeSchemeDisplayName;
private String gradeName;
```

In constructor after `this.detail = builder.detail;`:
```java
this.gradeSchemeDisplayName = builder.gradeSchemeDisplayName;
this.gradeName = builder.gradeName;
```

Add method after `updateScore`:
```java
public void updateGradeInfo(String gradeSchemeDisplayName, String gradeName) {
    this.gradeSchemeDisplayName = gradeSchemeDisplayName;
    this.gradeName = gradeName;
    this.updatedAt = LocalDateTime.now();
}
```

Add getters, builder fields, and builder methods.

**Step 2: Update PO and RepositoryImpl**

Add matching fields to `ProjectScorePO` and update `toPO()`/`toDomain()` in the RepositoryImpl.

**Step 3: Commit**

```
feat: add grade scheme snapshot fields to ProjectScore
```

---

### Task 6: Application Service — GradeSchemeApplicationService

**Files:**
- Create: `backend/src/main/java/com/school/management/application/inspection/v7/GradeSchemeApplicationService.java`

**Step 1: Create the service**

```java
package com.school.management.application.inspection.v7;

import com.school.management.domain.inspection.model.v7.scoring.GradeDefinition;
import com.school.management.domain.inspection.model.v7.scoring.GradeScheme;
import com.school.management.domain.inspection.model.v7.template.TemplateSection;
import com.school.management.domain.inspection.repository.v7.GradeDefinitionRepository;
import com.school.management.domain.inspection.repository.v7.GradeSchemeRepository;
import com.school.management.domain.inspection.repository.v7.TemplateSectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GradeSchemeApplicationService {

    private final GradeSchemeRepository schemeRepository;
    private final GradeDefinitionRepository definitionRepository;
    private final TemplateSectionRepository sectionRepository;

    public List<GradeScheme> listSchemes(Long tenantId) {
        List<GradeScheme> schemes = schemeRepository.findByTenantIdOrSystem(tenantId);
        schemes.forEach(s -> s.setGrades(definitionRepository.findByGradeSchemeId(s.getId())));
        return schemes;
    }

    public GradeScheme getScheme(Long id) {
        GradeScheme scheme = schemeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("等级方案不存在: " + id));
        scheme.setGrades(definitionRepository.findByGradeSchemeId(id));
        return scheme;
    }

    @Transactional
    public GradeScheme createScheme(Long tenantId, String displayName, String description,
                                     String schemeType, List<GradeDefinition> grades, Long createdBy) {
        GradeScheme scheme = GradeScheme.create(tenantId, displayName, description, schemeType, createdBy);
        scheme = schemeRepository.save(scheme);
        saveGrades(scheme.getId(), grades);
        scheme.setGrades(definitionRepository.findByGradeSchemeId(scheme.getId()));
        return scheme;
    }

    @Transactional
    public GradeScheme cloneFromPreset(Long sourceId, String displayName, Long tenantId, Long createdBy) {
        GradeScheme source = getScheme(sourceId);
        GradeScheme clone = GradeScheme.create(tenantId, displayName, source.getDescription(),
                source.getSchemeType(), createdBy);
        clone = schemeRepository.save(clone);
        // Copy definitions
        for (GradeDefinition def : source.getGrades()) {
            GradeDefinition copy = GradeDefinition.create(
                    clone.getId(), def.getCode(), def.getName(),
                    def.getMinValue(), def.getMaxValue(),
                    def.getColor(), def.getIcon(), def.getSortOrder());
            definitionRepository.save(copy);
        }
        clone.setGrades(definitionRepository.findByGradeSchemeId(clone.getId()));
        return clone;
    }

    @Transactional
    public GradeScheme updateScheme(Long id, String displayName, String description,
                                     List<GradeDefinition> grades) {
        GradeScheme scheme = schemeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("等级方案不存在: " + id));
        scheme.updateInfo(displayName, description);
        schemeRepository.save(scheme);
        // Replace all definitions
        definitionRepository.deleteByGradeSchemeId(id);
        saveGrades(id, grades);
        scheme.setGrades(definitionRepository.findByGradeSchemeId(id));
        return scheme;
    }

    @Transactional
    public void deleteScheme(Long id) {
        GradeScheme scheme = schemeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("等级方案不存在: " + id));
        if (Boolean.TRUE.equals(scheme.getIsSystem())) {
            throw new IllegalStateException("系统预设方案不可删除");
        }
        definitionRepository.deleteByGradeSchemeId(id);
        schemeRepository.deleteById(id);
    }

    // === Section Binding ===

    @Transactional
    public void bindToSection(Long sectionId, Long gradeSchemeId) {
        TemplateSection section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new IllegalArgumentException("分区不存在: " + sectionId));
        // Validate scheme exists
        schemeRepository.findById(gradeSchemeId)
                .orElseThrow(() -> new IllegalArgumentException("等级方案不存在: " + gradeSchemeId));
        section.setGradeSchemeId(gradeSchemeId);
        sectionRepository.save(section);
    }

    @Transactional
    public void unbindFromSection(Long sectionId) {
        TemplateSection section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new IllegalArgumentException("分区不存在: " + sectionId));
        section.setGradeSchemeId(null);
        sectionRepository.save(section);
    }

    public GradeScheme getSchemeForSection(Long sectionId) {
        TemplateSection section = sectionRepository.findById(sectionId).orElse(null);
        if (section == null || section.getGradeSchemeId() == null) return null;
        return getScheme(section.getGradeSchemeId());
    }

    private void saveGrades(Long schemeId, List<GradeDefinition> grades) {
        if (grades == null) return;
        int order = 0;
        for (GradeDefinition def : grades) {
            GradeDefinition toSave = GradeDefinition.create(
                    schemeId, def.getCode(), def.getName(),
                    def.getMinValue(), def.getMaxValue(),
                    def.getColor(), def.getIcon(), order++);
            definitionRepository.save(toSave);
        }
    }
}
```

**Step 2: Commit**

```
feat: add GradeSchemeApplicationService with CRUD and section binding
```

---

### Task 7: REST Controller — GradeSchemeController

**Files:**
- Create: `backend/src/main/java/com/school/management/interfaces/rest/inspection/v7/GradeSchemeController.java`

**Reference pattern:** `TemplateSectionController.java`

**Step 1: Create the controller**

```java
package com.school.management.interfaces.rest.inspection.v7;

import com.school.management.application.inspection.v7.GradeSchemeApplicationService;
import com.school.management.common.result.Result;
import com.school.management.common.util.SecurityUtils;
import com.school.management.domain.inspection.model.v7.scoring.GradeDefinition;
import com.school.management.domain.inspection.model.v7.scoring.GradeScheme;
import com.school.management.infrastructure.casbin.CasbinAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/v7/insp/grade-schemes")
@RequiredArgsConstructor
public class GradeSchemeController {

    private final GradeSchemeApplicationService gradeSchemeService;

    @GetMapping
    @CasbinAccess(resource = "insp:template", action = "view")
    public Result<List<GradeScheme>> listSchemes() {
        Long tenantId = 0L; // TODO: from context
        return Result.success(gradeSchemeService.listSchemes(tenantId));
    }

    @GetMapping("/{id}")
    @CasbinAccess(resource = "insp:template", action = "view")
    public Result<GradeScheme> getScheme(@PathVariable Long id) {
        return Result.success(gradeSchemeService.getScheme(id));
    }

    @PostMapping
    @CasbinAccess(resource = "insp:template", action = "edit")
    public Result<GradeScheme> createScheme(@RequestBody CreateGradeSchemeRequest req) {
        Long userId = SecurityUtils.getCurrentUserId();
        List<GradeDefinition> grades = req.grades() != null
                ? req.grades().stream().map(g -> GradeDefinition.create(
                    null, g.code(), g.name(), g.minValue(), g.maxValue(),
                    g.color(), g.icon(), g.sortOrder() != null ? g.sortOrder() : 0)).toList()
                : List.of();
        return Result.success(gradeSchemeService.createScheme(
                0L, req.displayName(), req.description(), req.schemeType(), grades, userId));
    }

    @PostMapping("/clone")
    @CasbinAccess(resource = "insp:template", action = "edit")
    public Result<GradeScheme> cloneScheme(@RequestBody CloneGradeSchemeRequest req) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(gradeSchemeService.cloneFromPreset(
                req.sourceSchemeId(), req.displayName(), 0L, userId));
    }

    @PutMapping("/{id}")
    @CasbinAccess(resource = "insp:template", action = "edit")
    public Result<GradeScheme> updateScheme(@PathVariable Long id,
                                             @RequestBody UpdateGradeSchemeRequest req) {
        List<GradeDefinition> grades = req.grades() != null
                ? req.grades().stream().map(g -> GradeDefinition.create(
                    null, g.code(), g.name(), g.minValue(), g.maxValue(),
                    g.color(), g.icon(), g.sortOrder() != null ? g.sortOrder() : 0)).toList()
                : null;
        return Result.success(gradeSchemeService.updateScheme(
                id, req.displayName(), req.description(), grades));
    }

    @DeleteMapping("/{id}")
    @CasbinAccess(resource = "insp:template", action = "edit")
    public Result<Void> deleteScheme(@PathVariable Long id) {
        gradeSchemeService.deleteScheme(id);
        return Result.success(null);
    }

    // === Section Binding ===

    @PutMapping("/sections/{sectionId}/bind")
    @CasbinAccess(resource = "insp:template", action = "edit")
    public Result<Void> bindToSection(@PathVariable Long sectionId,
                                       @RequestBody BindGradeSchemeRequest req) {
        gradeSchemeService.bindToSection(sectionId, req.gradeSchemeId());
        return Result.success(null);
    }

    @DeleteMapping("/sections/{sectionId}/bind")
    @CasbinAccess(resource = "insp:template", action = "edit")
    public Result<Void> unbindFromSection(@PathVariable Long sectionId) {
        gradeSchemeService.unbindFromSection(sectionId);
        return Result.success(null);
    }

    @GetMapping("/sections/{sectionId}")
    @CasbinAccess(resource = "insp:template", action = "view")
    public Result<GradeScheme> getSchemeForSection(@PathVariable Long sectionId) {
        GradeScheme scheme = gradeSchemeService.getSchemeForSection(sectionId);
        return Result.success(scheme);
    }

    // === Request Records ===

    record CreateGradeSchemeRequest(String displayName, String description, String schemeType,
                                     List<GradeDefRequest> grades) {}
    record CloneGradeSchemeRequest(Long sourceSchemeId, String displayName) {}
    record UpdateGradeSchemeRequest(String displayName, String description,
                                     List<GradeDefRequest> grades) {}
    record BindGradeSchemeRequest(Long gradeSchemeId) {}
    record GradeDefRequest(String code, String name, BigDecimal minValue, BigDecimal maxValue,
                           String color, String icon, Integer sortOrder) {}
}
```

**Step 2: Commit**

```
feat: add GradeSchemeController REST API
```

---

### Task 8: ScoreAggregationService — Integrate GradeScheme

**Files:**
- Modify: `backend/src/main/java/com/school/management/application/inspection/v7/ScoreAggregationService.java`
  - Add `GradeSchemeRepository` + `GradeDefinitionRepository` dependencies
  - Add new `determineGradeFromScheme(Long sectionId, BigDecimal score)` method
  - Modify `computeScoreFields` to prefer GradeScheme
  - Modify `gradeProjectScore` to snapshot scheme info

**Step 1: Add GradeScheme-aware grade determination**

In the dependency injection (after line 41):
```java
private final GradeSchemeRepository gradeSchemeRepository;
private final GradeDefinitionRepository gradeDefinitionRepository;
```

Add new method before `determineGrade`:
```java
/**
 * 优先从分区的 GradeScheme 确定等级，回退到旧 GradeBand 路径
 */
private GradeSchemeSnapshot determineGradeWithScheme(Long sectionId, BigDecimal score, BigDecimal maxScore) {
    TemplateSection section = sectionRepository.findById(sectionId).orElse(null);
    if (section != null && section.getGradeSchemeId() != null) {
        GradeScheme scheme = gradeSchemeRepository.findById(section.getGradeSchemeId()).orElse(null);
        if (scheme != null) {
            List<GradeDefinition> grades = gradeDefinitionRepository.findByGradeSchemeId(scheme.getId());
            scheme.setGrades(grades);
            var matched = "PERCENT_RANGE".equals(scheme.getSchemeType()) && maxScore != null
                    ? scheme.matchGrade(score, maxScore)
                    : scheme.matchGrade(score);
            if (matched.isPresent()) {
                GradeDefinition g = matched.get();
                return new GradeSchemeSnapshot(scheme.getDisplayName(), g.getCode(), g.getName(), g.getColor(), g.getIcon());
            }
            return new GradeSchemeSnapshot(scheme.getDisplayName(), null, null, null, null);
        }
    }
    return null;
}
```

**Step 2: Modify gradeProjectScore to use snapshot**

In `gradeProjectScore` method (~line 62-73), after determining grade:
```java
// After existing grade determination, try GradeScheme
GradeSchemeSnapshot schemeInfo = determineGradeWithScheme(
        project.getRootSectionId(), score.getScore(), null);
if (schemeInfo != null && schemeInfo.gradeName() != null) {
    grade = schemeInfo.gradeCode();
}
// ... existing updateScore call ...
if (schemeInfo != null) {
    score.updateGradeInfo(schemeInfo.displayName(), schemeInfo.gradeName());
}
```

**Step 3: Modify computeScoreFields to prefer GradeScheme**

At the start of the `sectionId != null` block in `computeScoreFields` (~line 167), add GradeScheme check before the existing ScoringProfile check:
```java
// Try GradeScheme first
GradeSchemeSnapshot schemeResult = determineGradeWithScheme(sectionId, sum, maxPossible);
if (schemeResult != null && schemeResult.gradeName() != null) {
    grade = schemeResult.gradeName();
    if (maxPossible.compareTo(BigDecimal.ZERO) > 0) {
        BigDecimal pct = sum.multiply(BigDecimal.valueOf(100))
                .divide(maxPossible, 2, RoundingMode.HALF_UP);
        passed = pct.compareTo(BigDecimal.valueOf(60)) >= 0;
    }
} else {
    // existing ScoringProfile → GradeBand fallback logic
}
```

**Step 4: Commit**

```
feat: ScoreAggregationService prefers GradeScheme over legacy GradeBand
```

---

### Task 9: Frontend — Types + API

**Files:**
- Create: `frontend/src/types/insp/gradeScheme.ts`
- Create: `frontend/src/api/insp/gradeScheme.ts`

**Step 1: Create TypeScript types**

`frontend/src/types/insp/gradeScheme.ts`:
```typescript
export interface GradeScheme {
  id: number
  tenantId: number
  displayName: string
  description: string | null
  schemeType: 'SCORE_RANGE' | 'PERCENT_RANGE'
  isSystem: boolean
  grades: GradeDefinition[]
  createdBy: number | null
  createdAt: string
  updatedAt: string | null
}

export interface GradeDefinition {
  id?: number
  gradeSchemeId?: number
  code: string
  name: string
  minValue: number
  maxValue: number
  color: string | null
  icon: string | null
  sortOrder: number
}

export interface CreateGradeSchemeRequest {
  displayName: string
  description?: string
  schemeType: string
  grades: Omit<GradeDefinition, 'id' | 'gradeSchemeId'>[]
}

export interface UpdateGradeSchemeRequest {
  displayName: string
  description?: string
  grades: Omit<GradeDefinition, 'id' | 'gradeSchemeId'>[]
}

export interface CloneGradeSchemeRequest {
  sourceSchemeId: number
  displayName: string
}
```

**Step 2: Create API module**

`frontend/src/api/insp/gradeScheme.ts`:
```typescript
import { http } from '@/utils/request'
import type {
  GradeScheme,
  CreateGradeSchemeRequest,
  UpdateGradeSchemeRequest,
  CloneGradeSchemeRequest,
} from '@/types/insp/gradeScheme'

const BASE = '/v7/insp/grade-schemes'

export function getGradeSchemes(): Promise<GradeScheme[]> {
  return http.get<GradeScheme[]>(BASE)
}

export function getGradeScheme(id: number): Promise<GradeScheme> {
  return http.get<GradeScheme>(`${BASE}/${id}`)
}

export function createGradeScheme(data: CreateGradeSchemeRequest): Promise<GradeScheme> {
  return http.post<GradeScheme>(BASE, data)
}

export function cloneGradeScheme(data: CloneGradeSchemeRequest): Promise<GradeScheme> {
  return http.post<GradeScheme>(`${BASE}/clone`, data)
}

export function updateGradeScheme(id: number, data: UpdateGradeSchemeRequest): Promise<GradeScheme> {
  return http.put<GradeScheme>(`${BASE}/${id}`, data)
}

export function deleteGradeScheme(id: number): Promise<void> {
  return http.delete(`${BASE}/${id}`)
}

// Section binding
export function bindSchemeToSection(sectionId: number, gradeSchemeId: number): Promise<void> {
  return http.put(`${BASE}/sections/${sectionId}/bind`, { gradeSchemeId })
}

export function unbindSchemeFromSection(sectionId: number): Promise<void> {
  return http.delete(`${BASE}/sections/${sectionId}/bind`)
}

export function getSchemeForSection(sectionId: number): Promise<GradeScheme | null> {
  return http.get<GradeScheme | null>(`${BASE}/sections/${sectionId}`)
}

export const gradeSchemeApi = {
  getList: getGradeSchemes,
  getById: getGradeScheme,
  create: createGradeScheme,
  clone: cloneGradeScheme,
  update: updateGradeScheme,
  delete: deleteGradeScheme,
  bindToSection: bindSchemeToSection,
  unbindFromSection: unbindSchemeFromSection,
  getForSection: getSchemeForSection,
}
```

**Step 3: Commit**

```
feat: add GradeScheme frontend types and API module
```

---

### Task 10: Frontend — GradeSchemeEditor Component

**Files:**
- Create: `frontend/src/views/inspection/v7/templates/components/GradeSchemeEditor.vue`

**Reference:** `GradeBandEditor.vue` (existing grade band editor)

**Step 1: Create the editor component**

A dialog/drawer component that allows:
- Setting `displayName` (方案名称)
- Setting `description` (方案描述)
- Setting `schemeType` (映射类型)
- Quick-apply from presets (五级/二级/红旗/星级)
- Add/edit/delete grade definitions in a table
- Validation: overlap check, gap check
- Save → emits `saved` with the GradeScheme

Props:
- `modelValue: boolean` (dialog visible)
- `scheme: GradeScheme | null` (existing scheme to edit, null for create)

Emits:
- `update:modelValue`
- `saved(scheme: GradeScheme)`

Template structure:
- ElDialog with form
- displayName input, description textarea, schemeType select
- Preset buttons row
- ElTable with inline-editable grade definitions (code, name, minValue, maxValue, color picker)
- Add/remove grade buttons
- Overlap/gap validation warnings
- Save/Cancel buttons

**Step 2: Commit**

```
feat: add GradeSchemeEditor Vue component
```

---

### Task 11: Frontend — Section Grade Scheme Selector

**Files:**
- Create: `frontend/src/views/inspection/v7/templates/components/GradeSchemeSelector.vue`
- Modify: `frontend/src/views/inspection/v7/templates/components/SectionTree.vue` or `SectionEditDialog.vue`
  - Add GradeSchemeSelector to section editing UI

**Step 1: Create GradeSchemeSelector component**

A section-level component that shows:
- Current bound scheme (if any) with preview
- Dropdown to select from available schemes
- "新建方案" button → opens GradeSchemeEditor
- "编辑方案" button → opens GradeSchemeEditor with current scheme
- "解绑" button → unbinds

Props:
- `sectionId: number`

Loads schemes on mount via `getGradeSchemes()`, shows current binding via `getSchemeForSection()`.

**Step 2: Integrate into section editing UI**

In the section editing dialog/panel (SectionEditDialog.vue or SectionPropsCard.vue), add the GradeSchemeSelector component after the scoring config section.

**Step 3: Commit**

```
feat: add GradeSchemeSelector and integrate into template section editor
```

---

### Task 12: Frontend — Project Detail Grade Scheme Display

**Files:**
- Modify: `frontend/src/views/inspection/v7/projects/ProjectDetailView.vue`
  - Replace hardcoded grade column with dynamic scheme-aware columns
  - Table headers use `scheme.displayName` instead of fixed "等级"
  - Grade cells show `gradeName` with color/icon from scheme

**Step 1: Load grade schemes for all sections**

In the project detail data loading, after loading section tree, for each section that has a `gradeSchemeId`, load the scheme. Store as a Map<sectionId, GradeScheme>.

**Step 2: Update score table columns**

Replace the single "等级" column with dynamic columns:
- For each section with a grade scheme, add a column with header = `scheme.displayName`
- Cell content: matched grade name with color dot

**Step 3: Update grade matching logic**

Replace the existing `rootGradeBands`-based matching with GradeScheme-based matching:
```typescript
function matchGrade(scheme: GradeScheme, score: number, maxScore?: number): GradeDefinition | null {
  const value = scheme.schemeType === 'PERCENT_RANGE' && maxScore
    ? (score / maxScore) * 100
    : score
  return scheme.grades.find(g => value >= g.minValue && value <= g.maxValue) ?? null
}
```

**Step 4: Commit**

```
feat: project detail displays grade scheme names as column headers
```

---

### Task 13: Backend Build Verification

**Step 1: Run backend build**

```bash
cd backend && JAVA_HOME="/c/Program Files/Java/jdk-17" PATH="/c/Program Files/Java/jdk-17/bin:$PATH" mvn compile -DskipTests
```

Expected: BUILD SUCCESS

**Step 2: Run frontend type check**

```bash
cd frontend && npm run type-check
```

Expected: No errors

**Step 3: Fix any compilation issues**

**Step 4: Commit any fixes**

```
fix: resolve compilation issues from GradeScheme integration
```

---

### Task 14: Start Backend and Smoke Test API

**Step 1: Start backend**

```bash
cd backend && JAVA_HOME="/c/Program Files/Java/jdk-17" PATH="/c/Program Files/Java/jdk-17/bin:$PATH" mvn spring-boot:run -DskipTests
```

**Step 2: Smoke test via curl**

```bash
# List schemes (should include 4 system presets)
curl -s http://localhost:8080/api/v7/insp/grade-schemes -H "Authorization: Bearer $TOKEN" | jq '.data | length'

# Get a specific scheme
curl -s http://localhost:8080/api/v7/insp/grade-schemes/1 -H "Authorization: Bearer $TOKEN" | jq '.data.displayName'

# Create custom scheme
curl -s -X POST http://localhost:8080/api/v7/insp/grade-schemes \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"displayName":"测试方案","schemeType":"SCORE_RANGE","grades":[{"code":"A","name":"优","minValue":80,"maxValue":100},{"code":"B","name":"良","minValue":0,"maxValue":79.99}]}'
```

**Step 3: Fix any runtime issues**

**Step 4: Commit any fixes**

---

## Execution Notes

- **DB migration first** (Task 1) — everything else depends on it
- **Tasks 2-5** are backend domain/infra, can be done in sequence quickly
- **Tasks 6-8** are backend service/controller layer, depend on 2-5
- **Tasks 9-12** are frontend, depend on Task 7 (API must exist)
- **Task 13-14** are verification
- Each task should be committed independently for clean history
