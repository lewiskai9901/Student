# Section Tree Refactoring: 统一树形分区 + 内嵌评分配置

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 将模板分区从扁平结构重构为树形结构（文件夹模式），合并"子模板引用"功能到分区中，并将评分配置内嵌到每个分区节点，替代独立的 ScoringProfile 系统。

**Architecture:** 分区(TemplateSection)成为树的节点，类似文件系统：
- 分区 = 文件夹（可包含子分区和字段）
- 字段(TemplateItem) = 文件（叶子节点）
- 引用分区 = 快捷方式（只读链接到另一个模板，可克隆为本地副本）
- 每个分区节点独立配置评分（汇总方式 + 等级划分），替代 ScoringProfile

**Tech Stack:** Spring Boot 3.2 + MyBatis Plus, Vue 3 + TypeScript + Element Plus, MySQL

---

## 概览

### 改动范围

| 层 | 新增 | 修改 | 废弃 |
|---|---|---|---|
| DB | 1 migration | - | - |
| Domain | - | TemplateSection | TemplateModuleRef (保留代码，停止使用) |
| PO/Mapper | - | TemplateSectionPO, TemplateSectionMapper | TemplateModuleRefPO/Mapper (保留) |
| Repository | - | TemplateSectionRepository + Impl | - |
| Application | - | TemplateSectionApplicationService, InspProjectApplicationService, InspTemplateApplicationService | TemplateModuleRefApplicationService (保留) |
| Controller | - | TemplateSectionController | TemplateModuleRefController (保留) |
| Frontend Types | - | template.ts, enums.ts | - |
| Frontend API | - | template.ts (section functions) | templateModuleRef.ts (停止使用) |
| Frontend Composable | - | useTemplateEditor.ts | - |
| Frontend Views | - | TemplateEditorView.vue, SectionTree.vue | ModuleRefPanel.vue (停止使用) |
| Scoring | - | - | ScoringProfile 相关组件 (Phase 2 后续) |

### 不改动

- TemplateItem（字段）模型不变
- InspSubmission / SubmissionDetail 不变
- 选项集(ResponseSet) 不变
- 整改系统不变
- 场所管理不变

---

## Task 1: DB Migration — 分区表加列

**Files:**
- Create: `database/schema/V61.0.0__section_tree_and_ref.sql`
- Reference: `database/schema/V30.0.0__insp_v7_template_schema.sql` (原始建表)
- Reference: `database/schema/V40.0.0__drop_parent_section_id.sql` (V40 删了 parent_section_id)

**Step 1: Write migration SQL**

```sql
-- V61.0.0: 分区树形结构 + 引用模板 + 内嵌评分配置
-- parent_section_id 在 V40 被删除，现在重新添加以支持树形分区

ALTER TABLE `insp_template_sections`
  ADD COLUMN `parent_section_id` BIGINT NULL AFTER `template_id`,
  ADD COLUMN `ref_template_id` BIGINT NULL AFTER `parent_section_id`,
  ADD COLUMN `scoring_config` JSON NULL AFTER `condition_logic`;

ALTER TABLE `insp_template_sections`
  ADD INDEX `idx_parent` (`parent_section_id`),
  ADD INDEX `idx_ref_template` (`ref_template_id`);
```

字段说明：
- `parent_section_id`: 父分区 ID，NULL 表示顶层分区
- `ref_template_id`: 引用的模板 ID，NULL 表示本地分区；非 NULL 表示该分区是对另一模板的只读引用
- `scoring_config`: JSON 格式的评分配置，示例：
  ```json
  {
    "aggregation": "WEIGHTED_AVERAGE",
    "maxScore": 100,
    "minScore": 0,
    "gradeBands": [
      {"grade": "A", "label": "优秀", "minScore": 90, "maxScore": 100, "color": "#22c55e"},
      {"grade": "B", "label": "良好", "minScore": 75, "maxScore": 89.99, "color": "#3b82f6"},
      {"grade": "C", "label": "合格", "minScore": 60, "maxScore": 74.99, "color": "#f59e0b"},
      {"grade": "D", "label": "不合格", "minScore": 0, "maxScore": 59.99, "color": "#ef4444"}
    ]
  }
  ```

**Step 2: Apply migration**

```bash
cd "/d/学生管理系统/backend"
mysql -u root -p123456 student_management < ../database/schema/V61.0.0__section_tree_and_ref.sql
```

**Step 3: Verify columns exist**

```bash
mysql -u root -p123456 student_management -e "DESCRIBE insp_template_sections;"
```

Expected: `parent_section_id`, `ref_template_id`, `scoring_config` columns present.

**Step 4: Commit**

```bash
git add database/schema/V61.0.0__section_tree_and_ref.sql
git commit -m "feat: add parent_section_id, ref_template_id, scoring_config to sections"
```

---

## Task 2: Domain Model — TemplateSection 加字段

**Files:**
- Modify: `backend/src/main/java/com/school/management/domain/inspection/model/v7/template/TemplateSection.java`

**Step 1: Add fields to TemplateSection**

在现有字段后新增三个字段：

```java
// 在 private String conditionLogic; 之后添加
private Long parentSectionId;    // 父分区ID, null=顶层
private Long refTemplateId;      // 引用模板ID, null=本地分区
private String scoringConfig;    // JSON: {aggregation, maxScore, minScore, gradeBands[]}
```

**Step 2: Update Builder**

Builder 类新增三个字段和对应链式方法：

```java
private Long parentSectionId;
private Long refTemplateId;
private String scoringConfig;

public Builder parentSectionId(Long parentSectionId) { this.parentSectionId = parentSectionId; return this; }
public Builder refTemplateId(Long refTemplateId) { this.refTemplateId = refTemplateId; return this; }
public Builder scoringConfig(String scoringConfig) { this.scoringConfig = scoringConfig; return this; }
```

**Step 3: Update constructor**

在 `private TemplateSection(Builder builder)` 中新增：

```java
this.parentSectionId = builder.parentSectionId;
this.refTemplateId = builder.refTemplateId;
this.scoringConfig = builder.scoringConfig;
```

**Step 4: Add factory method for reference section**

```java
public static TemplateSection createRef(Long templateId, Long parentSectionId,
                                         String sectionCode, String sectionName,
                                         Long refTemplateId, Long createdBy) {
    return builder()
            .templateId(templateId)
            .parentSectionId(parentSectionId)
            .sectionCode(sectionCode)
            .sectionName(sectionName)
            .refTemplateId(refTemplateId)
            .createdBy(createdBy)
            .build();
}
```

**Step 5: Update existing `create` method to accept parentSectionId**

```java
public static TemplateSection create(Long templateId, String sectionCode,
                                      String sectionName, Long createdBy) {
    // 保持不变，parentSectionId 默认 null（顶层分区）
    return builder()
            .templateId(templateId)
            .sectionCode(sectionCode)
            .sectionName(sectionName)
            .createdBy(createdBy)
            .build();
}
```

**Step 6: Add update method for scoringConfig**

```java
public void updateScoringConfig(String scoringConfig, Long updatedBy) {
    this.scoringConfig = scoringConfig;
    this.updatedBy = updatedBy;
    this.updatedAt = LocalDateTime.now();
}
```

**Step 7: Add isRef helper**

```java
public boolean isRef() {
    return refTemplateId != null;
}
```

**Step 8: Add getters**

```java
public Long getParentSectionId() { return parentSectionId; }
public Long getRefTemplateId() { return refTemplateId; }
public String getScoringConfig() { return scoringConfig; }
public boolean isRef() { return refTemplateId != null; }
```

**Step 9: Verify compile**

```bash
cd "/d/学生管理系统/backend"
JAVA_HOME="/c/Program Files/Java/jdk-17" PATH="/c/Program Files/Java/jdk-17/bin:$PATH" mvn compile -DskipTests -pl . 2>&1 | tail -5
```

**Step 10: Commit**

```bash
git add backend/src/main/java/com/school/management/domain/inspection/model/v7/template/TemplateSection.java
git commit -m "feat: add parentSectionId, refTemplateId, scoringConfig to TemplateSection domain"
```

---

## Task 3: PO + Mapper — 持久化层加字段

**Files:**
- Modify: `backend/src/main/java/com/school/management/infrastructure/persistence/inspection/v7/template/TemplateSectionPO.java`
- Modify: `backend/src/main/java/com/school/management/infrastructure/persistence/inspection/v7/template/TemplateSectionRepositoryImpl.java`

**Step 1: Add fields to TemplateSectionPO**

在 `private Long templateId;` 之后添加：

```java
private Long parentSectionId;
private Long refTemplateId;
private String scoringConfig;
```

**Step 2: Update TemplateSectionRepositoryImpl toDomain / toPO mapping**

在 `toDomain` 方法中添加：

```java
.parentSectionId(po.getParentSectionId())
.refTemplateId(po.getRefTemplateId())
.scoringConfig(po.getScoringConfig())
```

在 `toPO` 方法中添加：

```java
po.setParentSectionId(section.getParentSectionId());
po.setRefTemplateId(section.getRefTemplateId());
po.setScoringConfig(section.getScoringConfig());
```

**Step 3: Add repository methods for tree queries**

在 TemplateSectionRepository 接口新增：

```java
List<TemplateSection> findByParentSectionId(Long parentSectionId);
List<TemplateSection> findRootSections(Long templateId); // parentSectionId IS NULL
```

在 TemplateSectionRepositoryImpl 实现：

```java
@Override
public List<TemplateSection> findByParentSectionId(Long parentSectionId) {
    LambdaQueryWrapper<TemplateSectionPO> qw = new LambdaQueryWrapper<>();
    qw.eq(TemplateSectionPO::getParentSectionId, parentSectionId)
      .orderByAsc(TemplateSectionPO::getSortOrder);
    return mapper.selectList(qw).stream().map(this::toDomain).collect(Collectors.toList());
}

@Override
public List<TemplateSection> findRootSections(Long templateId) {
    LambdaQueryWrapper<TemplateSectionPO> qw = new LambdaQueryWrapper<>();
    qw.eq(TemplateSectionPO::getTemplateId, templateId)
      .isNull(TemplateSectionPO::getParentSectionId)
      .orderByAsc(TemplateSectionPO::getSortOrder);
    return mapper.selectList(qw).stream().map(this::toDomain).collect(Collectors.toList());
}
```

**Step 4: Update TemplateSectionRepository interface**

**File:** `backend/src/main/java/com/school/management/domain/inspection/repository/v7/TemplateSectionRepository.java`

Add methods:

```java
List<TemplateSection> findByParentSectionId(Long parentSectionId);
List<TemplateSection> findRootSections(Long templateId);
```

**Step 5: Verify compile**

```bash
cd "/d/学生管理系统/backend"
JAVA_HOME="/c/Program Files/Java/jdk-17" PATH="/c/Program Files/Java/jdk-17/bin:$PATH" mvn compile -DskipTests 2>&1 | tail -5
```

**Step 6: Commit**

```bash
git add backend/src/main/java/com/school/management/infrastructure/persistence/inspection/v7/template/TemplateSectionPO.java \
       backend/src/main/java/com/school/management/infrastructure/persistence/inspection/v7/template/TemplateSectionRepositoryImpl.java \
       backend/src/main/java/com/school/management/domain/inspection/repository/v7/TemplateSectionRepository.java
git commit -m "feat: add tree and ref fields to TemplateSectionPO and repository"
```

---

## Task 4: Application Service — 树形分区 CRUD + 引用 + 克隆

**Files:**
- Modify: `backend/src/main/java/com/school/management/application/inspection/v7/TemplateSectionApplicationService.java`

**Step 1: Add dependencies**

```java
private final InspTemplateRepository templateRepository;  // 新增
```

**Step 2: Update createSection to accept parentSectionId**

```java
@Transactional
public TemplateSection createSection(Long templateId, String sectionCode, String sectionName,
                                      Long parentSectionId,     // 新增
                                      Integer weight,
                                      Boolean isRepeatable, String conditionLogic,
                                      Integer sortOrder, Long createdBy) {
    TemplateSection section = TemplateSection.create(templateId, sectionCode, sectionName, createdBy);
    section.update(sectionName, weight, isRepeatable, conditionLogic, createdBy);
    if (parentSectionId != null) {
        // 验证父分区存在且属于同一模板
        TemplateSection parent = sectionRepository.findById(parentSectionId)
                .orElseThrow(() -> new IllegalArgumentException("父分区不存在: " + parentSectionId));
        if (!parent.getTemplateId().equals(templateId)) {
            throw new IllegalArgumentException("父分区不属于当前模板");
        }
    }
    // Use builder to set parentSectionId since create() doesn't support it
    TemplateSection withParent = TemplateSection.reconstruct(
        TemplateSection.builder()
            .templateId(templateId)
            .parentSectionId(parentSectionId)
            .sectionCode(sectionCode)
            .sectionName(sectionName)
            .sortOrder(sortOrder != null ? sortOrder : 0)
            .weight(weight != null ? weight : 100)
            .isRepeatable(isRepeatable != null ? isRepeatable : false)
            .conditionLogic(conditionLogic)
            .createdBy(createdBy)
    );
    return sectionRepository.save(withParent);
}
```

**Step 3: Add createRefSection method**

```java
@Transactional
public TemplateSection createRefSection(Long templateId, Long parentSectionId,
                                         Long refTemplateId, Integer weight,
                                         Integer sortOrder, Long createdBy) {
    // 验证引用模板存在
    InspTemplate refTemplate = templateRepository.findById(refTemplateId)
            .orElseThrow(() -> new IllegalArgumentException("引用模板不存在: " + refTemplateId));

    // 防止自引用
    if (refTemplateId.equals(templateId)) {
        throw new IllegalArgumentException("不能引用自身模板");
    }

    // 循环引用检测
    if (hasCircularReference(templateId, refTemplateId)) {
        throw new IllegalArgumentException("检测到循环引用");
    }

    String sectionCode = "REF-" + System.currentTimeMillis();
    TemplateSection refSection = TemplateSection.createRef(
            templateId, parentSectionId, sectionCode,
            refTemplate.getTemplateName(), refTemplateId, createdBy);
    if (weight != null) refSection.update(refSection.getSectionName(), weight,
            false, null, createdBy);
    if (sortOrder != null) refSection.reorder(sortOrder);
    return sectionRepository.save(refSection);
}

private boolean hasCircularReference(Long templateId, Long refTemplateId) {
    // BFS: 从 refTemplateId 的分区中找引用，看是否回到 templateId
    Set<Long> visited = new HashSet<>();
    Queue<Long> queue = new LinkedList<>();
    queue.add(refTemplateId);
    while (!queue.isEmpty()) {
        Long current = queue.poll();
        if (current.equals(templateId)) return true;
        if (!visited.add(current)) continue;
        List<TemplateSection> sections = sectionRepository.findByTemplateId(current);
        for (TemplateSection s : sections) {
            if (s.getRefTemplateId() != null) {
                queue.add(s.getRefTemplateId());
            }
        }
    }
    return false;
}
```

**Step 4: Add cloneRefSection method**

```java
@Transactional
public TemplateSection cloneRefSection(Long sectionId, Long operatorId) {
    TemplateSection refSection = sectionRepository.findById(sectionId)
            .orElseThrow(() -> new IllegalArgumentException("分区不存在: " + sectionId));
    if (refSection.getRefTemplateId() == null) {
        throw new IllegalArgumentException("非引用分区不需要克隆");
    }

    Long refTemplateId = refSection.getRefTemplateId();

    // 获取引用模板的所有分区和字段
    List<TemplateSection> refSections = sectionRepository.findByTemplateId(refTemplateId);
    List<TemplateItem> refItems = itemRepository.findByTemplateId(refTemplateId);

    // 将引用分区转为本地分区（清除 refTemplateId）
    refSection.update(refSection.getSectionName(), refSection.getWeight(),
            refSection.getIsRepeatable(), refSection.getConditionLogic(), operatorId);
    // Need to clear refTemplateId — use reconstruct
    TemplateSection cloned = TemplateSection.reconstruct(
        TemplateSection.builder()
            .id(refSection.getId())
            .templateId(refSection.getTemplateId())
            .parentSectionId(refSection.getParentSectionId())
            .refTemplateId(null)  // 清除引用，变为本地
            .sectionCode(refSection.getSectionCode())
            .sectionName(refSection.getSectionName())
            .sortOrder(refSection.getSortOrder())
            .weight(refSection.getWeight())
            .isRepeatable(refSection.getIsRepeatable())
            .conditionLogic(refSection.getConditionLogic())
            .scoringConfig(refSection.getScoringConfig())
            .createdBy(refSection.getCreatedBy())
            .createdAt(refSection.getCreatedAt())
            .updatedBy(operatorId)
            .updatedAt(LocalDateTime.now())
    );
    sectionRepository.save(cloned);

    // 复制引用模板的分区作为子分区
    Map<Long, Long> sectionIdMap = new HashMap<>();  // oldId -> newId
    // 先处理顶层分区（parentSectionId is null）
    List<TemplateSection> rootRefSections = refSections.stream()
            .filter(s -> s.getParentSectionId() == null)
            .sorted(Comparator.comparingInt(TemplateSection::getSortOrder))
            .collect(Collectors.toList());
    copySubTree(rootRefSections, refSections, refSection.getTemplateId(),
                refSection.getId(), sectionIdMap, operatorId);

    // 复制字段
    for (TemplateSection srcSection : refSections) {
        Long newSectionId = sectionIdMap.get(srcSection.getId());
        if (newSectionId == null) continue;
        List<TemplateItem> srcItems = refItems.stream()
                .filter(i -> i.getSectionId().equals(srcSection.getId()))
                .sorted(Comparator.comparingInt(TemplateItem::getSortOrder))
                .collect(Collectors.toList());
        for (TemplateItem srcItem : srcItems) {
            TemplateItem newItem = TemplateItem.create(
                    newSectionId, "I-" + System.currentTimeMillis() + "-" + ThreadLocalRandom.current().nextInt(1000),
                    srcItem.getItemName(), srcItem.getItemType(), operatorId);
            newItem.update(srcItem.getItemName(), srcItem.getDescription(), srcItem.getItemType(),
                    srcItem.getConfig(), srcItem.getValidationRules(), srcItem.getResponseSetId(),
                    srcItem.getScoringConfig(), srcItem.getDimensionId(), srcItem.getHelpContent(),
                    srcItem.getIsRequired(), srcItem.getIsScored(), srcItem.getRequireEvidence(),
                    srcItem.getItemWeight(), srcItem.getConditionLogic(), operatorId);
            newItem.reorder(srcItem.getSortOrder());
            itemRepository.save(newItem);
        }
    }

    return cloned;
}

private void copySubTree(List<TemplateSection> currentLevel,
                          List<TemplateSection> allSections,
                          Long targetTemplateId, Long targetParentId,
                          Map<Long, Long> sectionIdMap, Long operatorId) {
    for (TemplateSection src : currentLevel) {
        String code = "S-" + System.currentTimeMillis() + "-" + ThreadLocalRandom.current().nextInt(1000);
        TemplateSection copy = TemplateSection.reconstruct(
            TemplateSection.builder()
                .templateId(targetTemplateId)
                .parentSectionId(targetParentId)
                .sectionCode(code)
                .sectionName(src.getSectionName())
                .sortOrder(src.getSortOrder())
                .weight(src.getWeight())
                .isRepeatable(src.getIsRepeatable())
                .conditionLogic(src.getConditionLogic())
                .scoringConfig(src.getScoringConfig())
                .createdBy(operatorId)
        );
        copy = sectionRepository.save(copy);
        sectionIdMap.put(src.getId(), copy.getId());

        // 递归处理子分区
        List<TemplateSection> children = allSections.stream()
                .filter(s -> src.getId().equals(s.getParentSectionId()))
                .sorted(Comparator.comparingInt(TemplateSection::getSortOrder))
                .collect(Collectors.toList());
        if (!children.isEmpty()) {
            copySubTree(children, allSections, targetTemplateId, copy.getId(), sectionIdMap, operatorId);
        }
    }
}
```

**Step 5: Add updateScoringConfig method**

```java
@Transactional
public TemplateSection updateScoringConfig(Long sectionId, String scoringConfig, Long updatedBy) {
    TemplateSection section = sectionRepository.findById(sectionId)
            .orElseThrow(() -> new IllegalArgumentException("分区不存在: " + sectionId));
    if (section.isRef()) {
        throw new IllegalArgumentException("引用分区不可修改评分配置");
    }
    section.updateScoringConfig(scoringConfig, updatedBy);
    return sectionRepository.save(section);
}
```

**Step 6: Update deleteSection to cascade children**

```java
@Transactional
public void deleteSection(Long id) {
    // 递归删除子分区
    List<TemplateSection> children = sectionRepository.findByParentSectionId(id);
    for (TemplateSection child : children) {
        deleteSection(child.getId());
    }
    // 删除本分区的字段
    itemRepository.deleteBySectionId(id);
    sectionRepository.deleteById(id);
}
```

**Step 7: Add listSectionsTree method**

```java
@Transactional(readOnly = true)
public List<TemplateSection> listSectionsFlat(Long templateId) {
    // 返回所有分区（扁平列表），前端自行组装树
    return sectionRepository.findByTemplateId(templateId);
}
```

**Step 8: Verify compile**

```bash
cd "/d/学生管理系统/backend"
JAVA_HOME="/c/Program Files/Java/jdk-17" PATH="/c/Program Files/Java/jdk-17/bin:$PATH" mvn compile -DskipTests 2>&1 | tail -5
```

**Step 9: Commit**

```bash
git add backend/src/main/java/com/school/management/application/inspection/v7/TemplateSectionApplicationService.java
git commit -m "feat: tree-aware section CRUD with ref, clone, scoring config"
```

---

## Task 5: Controller — 新增引用/克隆/评分配置端点

**Files:**
- Modify: `backend/src/main/java/com/school/management/interfaces/rest/inspection/v7/TemplateSectionController.java`

**Step 1: Update CreateSectionRequest to include parentSectionId**

```java
@lombok.Data
public static class CreateSectionRequest {
    private String sectionCode;
    private String sectionName;
    private Long parentSectionId;    // 新增
    private Integer weight;
    private Boolean isRepeatable;
    private String conditionLogic;
    private Integer sortOrder;
}
```

**Step 2: Update createSection endpoint**

传入 `request.getParentSectionId()` 到 service 调用。

**Step 3: Add createRefSection endpoint**

```java
@PostMapping("/ref")
@CasbinAccess(resource = "insp:template", action = "edit")
public Result<TemplateSection> createRefSection(@PathVariable Long templateId,
                                                 @RequestBody CreateRefSectionRequest request) {
    Long userId = SecurityUtils.getCurrentUserId();
    return Result.success(sectionService.createRefSection(
            templateId, request.getParentSectionId(),
            request.getRefTemplateId(), request.getWeight(),
            request.getSortOrder(), userId));
}

@lombok.Data
public static class CreateRefSectionRequest {
    private Long parentSectionId;
    private Long refTemplateId;
    private Integer weight;
    private Integer sortOrder;
}
```

**Step 4: Add cloneRefSection endpoint**

```java
@PostMapping("/{sectionId}/clone")
@CasbinAccess(resource = "insp:template", action = "edit")
public Result<TemplateSection> cloneRefSection(@PathVariable Long templateId,
                                                @PathVariable Long sectionId) {
    Long userId = SecurityUtils.getCurrentUserId();
    return Result.success(sectionService.cloneRefSection(sectionId, userId));
}
```

**Step 5: Add updateScoringConfig endpoint**

```java
@PutMapping("/{sectionId}/scoring-config")
@CasbinAccess(resource = "insp:template", action = "edit")
public Result<TemplateSection> updateScoringConfig(@PathVariable Long templateId,
                                                     @PathVariable Long sectionId,
                                                     @RequestBody UpdateScoringConfigRequest request) {
    Long userId = SecurityUtils.getCurrentUserId();
    return Result.success(sectionService.updateScoringConfig(sectionId,
            request.getScoringConfig(), userId));
}

@lombok.Data
public static class UpdateScoringConfigRequest {
    private String scoringConfig;
}
```

**Step 6: Verify compile**

```bash
cd "/d/学生管理系统/backend"
JAVA_HOME="/c/Program Files/Java/jdk-17" PATH="/c/Program Files/Java/jdk-17/bin:$PATH" mvn compile -DskipTests 2>&1 | tail -5
```

**Step 7: Commit**

```bash
git add backend/src/main/java/com/school/management/interfaces/rest/inspection/v7/TemplateSectionController.java
git commit -m "feat: add ref section, clone, scoring config REST endpoints"
```

---

## Task 6: 更新项目创建逻辑 — 读取 refTemplateId 而非 moduleRefs

**Files:**
- Modify: `backend/src/main/java/com/school/management/application/inspection/v7/InspProjectApplicationService.java`

**Step 1: Update expandModuleRefsToChildProjects**

将现有的 `expandModuleRefsToChildProjects` 方法改为从 sections 的 `refTemplateId` 读取，而非从 `TemplateModuleRefRepository`：

```java
private void expandRefSectionsToChildProjects(InspProject parentProject, Long createdBy) {
    Long templateId = parentProject.getTemplateId();
    List<TemplateSection> allSections = sectionRepository.findByTemplateId(templateId);

    // 找所有引用分区
    List<TemplateSection> refSections = allSections.stream()
            .filter(s -> s.getRefTemplateId() != null)
            .sorted(Comparator.comparingInt(TemplateSection::getSortOrder))
            .collect(Collectors.toList());

    for (TemplateSection refSection : refSections) {
        InspTemplate childTemplate = templateRepository.findById(refSection.getRefTemplateId())
                .orElse(null);
        if (childTemplate == null) continue;

        InspProject childProject = InspProject.create(
                generateProjectCode(),
                refSection.getSectionName(),  // 用分区名作为子项目名
                childTemplate.getId(),
                createdBy);
        childProject.setParentProjectId(parentProject.getId());
        // 继承父项目的 scope 和 target 配置
        if (childTemplate.getTargetType() != null) {
            childProject.updateTargetType(childTemplate.getTargetType());
        }
        childProject = projectRepository.save(childProject);

        // 递归展开子模板的引用分区
        expandRefSectionsToChildProjects(childProject, createdBy);
    }
}
```

**Step 2: Update createProject to call new method**

Replace call to `expandModuleRefsToChildProjects` with `expandRefSectionsToChildProjects`.

**Step 3: Add TemplateSectionRepository dependency**

```java
private final TemplateSectionRepository sectionRepository;
```

**Step 4: Verify compile**

```bash
cd "/d/学生管理系统/backend"
JAVA_HOME="/c/Program Files/Java/jdk-17" PATH="/c/Program Files/Java/jdk-17/bin:$PATH" mvn compile -DskipTests 2>&1 | tail -5
```

**Step 5: Commit**

```bash
git add backend/src/main/java/com/school/management/application/inspection/v7/InspProjectApplicationService.java
git commit -m "feat: project creation reads refTemplateId from sections instead of moduleRefs"
```

---

## Task 7: Frontend 类型 + API

**Files:**
- Modify: `frontend/src/types/insp/template.ts`
- Modify: `frontend/src/api/insp/template.ts`

**Step 1: Update TemplateSection type**

```typescript
export interface TemplateSection {
  id: number
  templateId: number
  parentSectionId: number | null    // 新增
  refTemplateId: number | null      // 新增
  sectionCode: string
  sectionName: string
  sortOrder: number
  weight: number
  isRepeatable: boolean
  scoringConfig: string | null      // 新增: JSON
  createdAt: string
  updatedAt: string
}
```

**Step 2: Update CreateSectionRequest**

```typescript
export interface CreateSectionRequest {
  sectionCode: string
  sectionName: string
  parentSectionId?: number | null    // 新增
  weight?: number
  isRepeatable?: boolean
  sortOrder?: number
}
```

**Step 3: Add new request types**

```typescript
export interface CreateRefSectionRequest {
  parentSectionId?: number | null
  refTemplateId: number
  weight?: number
  sortOrder?: number
}

export interface ScoringConfig {
  aggregation: 'WEIGHTED_AVERAGE' | 'SUM' | 'AVERAGE' | 'MAX' | 'MIN'
  maxScore: number
  minScore: number
  gradeBands: GradeBand[]
}

export interface GradeBand {
  grade: string
  label: string
  minScore: number
  maxScore: number
  color: string
}
```

**Step 4: Add API functions to template.ts**

```typescript
// 引用分区
export function createRefSection(templateId: number, data: CreateRefSectionRequest): Promise<TemplateSection> {
  return http.post<TemplateSection>(`${BASE}/${templateId}/sections/ref`, data)
}

// 克隆引用分区
export function cloneRefSection(templateId: number, sectionId: number): Promise<TemplateSection> {
  return http.post<TemplateSection>(`${BASE}/${templateId}/sections/${sectionId}/clone`)
}

// 更新分区评分配置
export function updateSectionScoringConfig(templateId: number, sectionId: number, scoringConfig: string): Promise<TemplateSection> {
  return http.put<TemplateSection>(`${BASE}/${templateId}/sections/${sectionId}/scoring-config`, { scoringConfig })
}
```

**Step 5: Add to inspTemplateApi object**

```typescript
export const inspTemplateApi = {
  // ... existing
  createRefSection,
  cloneRefSection,
  updateSectionScoringConfig,
}
```

**Step 6: Verify build**

```bash
cd "/d/学生管理系统/frontend"
npm run build 2>&1 | tail -5
```

**Step 7: Commit**

```bash
git add frontend/src/types/insp/template.ts frontend/src/api/insp/template.ts
git commit -m "feat: add ref section, clone, scoring config to frontend types and API"
```

---

## Task 8: Frontend Composable — useTemplateEditor 支持树形

**Files:**
- Modify: `frontend/src/composables/insp/useTemplateEditor.ts`

**Step 1: Update addSection to accept parentSectionId**

```typescript
async function addSection(parentSectionId?: number | null): Promise<TemplateSection> {
  const code = `S${Date.now().toString(36)}`
  const data: CreateSectionRequest = {
    sectionCode: code,
    sectionName: `新分区`,
    parentSectionId: parentSectionId ?? null,
    sortOrder: sections.value.filter(s =>
      (parentSectionId ? s.parentSectionId === parentSectionId : !s.parentSectionId)
    ).length,
  }
  const section = await inspTemplateApi.createSection(templateId.value, data)
  sections.value.push(section)
  return section
}
```

**Step 2: Add addRefSection method**

```typescript
async function addRefSection(refTemplateId: number, parentSectionId?: number | null): Promise<TemplateSection> {
  const section = await inspTemplateApi.createRefSection(templateId.value, {
    refTemplateId,
    parentSectionId: parentSectionId ?? null,
    sortOrder: sections.value.filter(s =>
      (parentSectionId ? s.parentSectionId === parentSectionId : !s.parentSectionId)
    ).length,
  })
  sections.value.push(section)
  return section
}
```

**Step 3: Add cloneRefSection method**

```typescript
async function cloneSection(sectionId: number): Promise<TemplateSection> {
  const cloned = await inspTemplateApi.cloneRefSection(templateId.value, sectionId)
  // 重新加载所有分区（克隆会创建子分区）
  await loadSections()
  return cloned
}
```

**Step 4: Add updateScoringConfig method**

```typescript
async function updateScoringConfig(sectionId: number, config: string): Promise<void> {
  await inspTemplateApi.updateSectionScoringConfig(templateId.value, sectionId, config)
  const idx = sections.value.findIndex(s => s.id === sectionId)
  if (idx >= 0) sections.value[idx].scoringConfig = config
}
```

**Step 5: Expose new methods in return**

Add `addRefSection`, `cloneSection`, `updateScoringConfig` to the composable's return object.

**Step 6: Add tree helper computed**

```typescript
// 树形结构：按 parentSectionId 组织
const sectionTree = computed(() => {
  const map = new Map<number | null, TemplateSection[]>()
  for (const s of sections.value) {
    const key = s.parentSectionId ?? null
    if (!map.has(key)) map.set(key, [])
    map.get(key)!.push(s)
  }
  // 每层按 sortOrder 排序
  for (const list of map.values()) {
    list.sort((a, b) => a.sortOrder - b.sortOrder)
  }
  return map
})
```

Expose `sectionTree` in return.

**Step 7: Verify build**

```bash
cd "/d/学生管理系统/frontend"
npm run build 2>&1 | tail -5
```

**Step 8: Commit**

```bash
git add frontend/src/composables/insp/useTemplateEditor.ts
git commit -m "feat: useTemplateEditor supports tree sections, refs, clone, scoring"
```

---

## Task 9: Frontend — SectionTree.vue 改造为树形 + 引用显示

**Files:**
- Modify: `frontend/src/views/inspection/v7/templates/components/SectionTree.vue`

**Step 1: Update props to accept tree data**

```typescript
const props = defineProps<{
  sections: TemplateSection[]
  selectedSectionId: number | null
  readonly?: boolean
}>()
```

**Step 2: Build tree structure in component**

```typescript
interface SectionNode {
  section: TemplateSection
  children: SectionNode[]
}

const tree = computed<SectionNode[]>(() => {
  const map = new Map<number | null, TemplateSection[]>()
  for (const s of props.sections) {
    const key = s.parentSectionId ?? null
    if (!map.has(key)) map.set(key, [])
    map.get(key)!.push(s)
  }
  for (const list of map.values()) {
    list.sort((a, b) => a.sortOrder - b.sortOrder)
  }

  function buildNodes(parentId: number | null): SectionNode[] {
    return (map.get(parentId) ?? []).map(s => ({
      section: s,
      children: buildNodes(s.id),
    }))
  }
  return buildNodes(null)
})
```

**Step 3: Recursive tree rendering template**

使用递归组件或 `v-for` + 缩进渲染树。引用分区显示特殊标记（链接图标 + "引用"标签）。

关键 UI 元素：
- 本地分区：普通文件夹图标，可编辑
- 引用分区：链接图标 + 蓝色"引用"标签，不可编辑字段，显示"克隆"按钮
- 每个分区右键/hover 操作：添加子分区、添加引用分区、删除、克隆（仅引用）

**Step 4: Emit events**

```typescript
const emit = defineEmits<{
  select: [sectionId: number]
  add: [parentSectionId?: number]
  addRef: [parentSectionId?: number]
  remove: [sectionId: number]
  clone: [sectionId: number]
}>()
```

**Step 5: Verify build**

```bash
cd "/d/学生管理系统/frontend"
npm run build 2>&1 | tail -5
```

**Step 6: Commit**

```bash
git add frontend/src/views/inspection/v7/templates/components/SectionTree.vue
git commit -m "feat: SectionTree supports tree hierarchy and ref section display"
```

---

## Task 10: Frontend — TemplateEditorView.vue 合并 tabs + 接入树

**Files:**
- Modify: `frontend/src/views/inspection/v7/templates/TemplateEditorView.vue`

**Step 1: Remove "子模板引用" tab**

删除 `activeView` 中的 `'modules'` 选项，以及对应的 tab 按钮和 `<ModuleRefPanel>` 使用。

Tab 从三个变为两个：
- `structure` → "模板结构"（包含树形分区 + 引用分区）
- `scoring` → "评分配置"（后续 Phase 2 会把评分内嵌到分区，但目前保留独立 tab）

```typescript
const activeView = ref<'structure' | 'scoring'>('structure')
```

**Step 2: Update SectionTree events**

```html
<SectionTree
  :sections="editor.sections.value"
  :selected-section-id="selectedSectionId"
  :readonly="isReadonly"
  @select="handleSelectSection"
  @add="handleAddSection"
  @add-ref="handleAddRefSection"
  @remove="handleRemoveSection"
  @clone="handleCloneSection"
/>
```

**Step 3: Add handler functions**

```typescript
async function handleAddSection(parentSectionId?: number) {
  if (isReadonly.value) return
  try {
    const section = await editor.addSection(parentSectionId)
    selectedSectionId.value = section.id
    selectedItem.value = null
  } catch (e: any) {
    ElMessage.error(e.message || '添加分区失败')
  }
}

async function handleAddRefSection(parentSectionId?: number) {
  if (isReadonly.value) return
  // 打开模板选择对话框
  refParentSectionId.value = parentSectionId ?? null
  showRefPicker.value = true
}

async function handleCloneSection(sectionId: number) {
  if (isReadonly.value) return
  try {
    await editor.cloneSection(sectionId)
    ElMessage.success('克隆成功，引用分区已转为本地分区')
  } catch (e: any) {
    ElMessage.error(e.message || '克隆失败')
  }
}
```

**Step 4: Add reference template picker dialog**

类似 ModuleRefPanel 中的 picker，但更简化：选择一个模板后调用 `editor.addRefSection()`。

```typescript
const showRefPicker = ref(false)
const refParentSectionId = ref<number | null>(null)
const refPickerTemplates = ref<InspTemplate[]>([])
const refPickerLoading = ref(false)

async function openRefPicker() {
  refPickerLoading.value = true
  try {
    const result = await inspTemplateApi.getList({ page: 1, size: 100 })
    refPickerTemplates.value = result.records.filter(t => t.id !== templateId.value)
  } finally {
    refPickerLoading.value = false
  }
}

async function handlePickRef(tpl: InspTemplate) {
  try {
    await editor.addRefSection(tpl.id, refParentSectionId.value)
    showRefPicker.value = false
    ElMessage.success(`已添加引用「${tpl.templateName}」`)
  } catch (e: any) {
    ElMessage.error(e.message || '添加引用失败')
  }
}
```

Watch `showRefPicker` to trigger load:
```typescript
watch(showRefPicker, (v) => { if (v) openRefPicker() })
```

**Step 5: Update section editor panel for ref sections**

当选中引用分区时，右侧面板显示"此为引用分区（只读）"提示 + "克隆为本地"按钮。

**Step 6: Remove ModuleRefPanel import**

```typescript
// 删除: import ModuleRefPanel from './components/ModuleRefPanel.vue'
```

**Step 7: Verify build**

```bash
cd "/d/学生管理系统/frontend"
npm run build 2>&1 | tail -5
```

**Step 8: Commit**

```bash
git add frontend/src/views/inspection/v7/templates/TemplateEditorView.vue
git commit -m "feat: merge module ref into section tree, remove modules tab"
```

---

## Task 11: Frontend — 分区评分配置 UI

**Files:**
- Modify: `frontend/src/views/inspection/v7/templates/TemplateEditorView.vue` (section editor panel)

**Step 1: Add scoring config form in section editor**

在右侧分区编辑面板（`te-sec-editor-body`）中，新增"评分配置"折叠区：

```html
<!-- 评分配置 (仅非引用分区) -->
<div v-if="!selectedSection?.refTemplateId" class="te-sec-scoring">
  <div class="te-sec-cond-head">
    <span class="te-sec-cond-title">评分配置</span>
    <span v-if="sectionScoringForm.aggregation" class="te-sec-cond-badge">已配置</span>
  </div>
  <p class="te-sec-cond-desc">配置该分区下字段/子分区的分数汇总方式和等级划分。</p>

  <div class="te-sec-fld">
    <label>汇总方式</label>
    <select v-model="sectionScoringForm.aggregation" @change="markScoringDirty">
      <option value="WEIGHTED_AVERAGE">加权平均</option>
      <option value="SUM">求和</option>
      <option value="AVERAGE">简单平均</option>
      <option value="MAX">取最高</option>
      <option value="MIN">取最低</option>
    </select>
  </div>

  <!-- 等级区间简化编辑 -->
  <div class="te-sec-fld">
    <label>等级区间</label>
    <div v-for="(band, idx) in sectionScoringForm.gradeBands" :key="idx" class="flex items-center gap-2 mb-1">
      <input v-model="band.grade" class="w-12" placeholder="等级" @input="markScoringDirty" />
      <input v-model="band.label" class="flex-1" placeholder="标签" @input="markScoringDirty" />
      <input v-model.number="band.minScore" type="number" class="w-16" placeholder="最低" @input="markScoringDirty" />
      <span>-</span>
      <input v-model.number="band.maxScore" type="number" class="w-16" placeholder="最高" @input="markScoringDirty" />
      <button @click="removeScoringBand(idx)">×</button>
    </div>
    <button class="te-btn-ghost te-btn-sm" @click="addScoringBand">+ 添加等级</button>
  </div>

  <button
    v-if="scoringConfigDirty"
    class="te-btn-primary te-btn-sm mt-2"
    @click="handleSaveScoringConfig"
  >保存评分配置</button>
</div>
```

**Step 2: Add scoring config state**

```typescript
const sectionScoringForm = reactive({
  aggregation: 'WEIGHTED_AVERAGE',
  maxScore: 100,
  minScore: 0,
  gradeBands: [] as Array<{ grade: string; label: string; minScore: number; maxScore: number; color: string }>,
})
const scoringConfigDirty = ref(false)

function markScoringDirty() { scoringConfigDirty.value = true }

function addScoringBand() {
  sectionScoringForm.gradeBands.push({ grade: '', label: '', minScore: 0, maxScore: 100, color: '#3b82f6' })
  markScoringDirty()
}

function removeScoringBand(idx: number) {
  sectionScoringForm.gradeBands.splice(idx, 1)
  markScoringDirty()
}

// 切换分区时同步评分配置
watch(selectedSection, (s) => {
  if (s?.scoringConfig) {
    try {
      const config = JSON.parse(s.scoringConfig)
      sectionScoringForm.aggregation = config.aggregation || 'WEIGHTED_AVERAGE'
      sectionScoringForm.maxScore = config.maxScore ?? 100
      sectionScoringForm.minScore = config.minScore ?? 0
      sectionScoringForm.gradeBands = config.gradeBands || []
    } catch {
      Object.assign(sectionScoringForm, { aggregation: 'WEIGHTED_AVERAGE', maxScore: 100, minScore: 0, gradeBands: [] })
    }
  } else {
    Object.assign(sectionScoringForm, { aggregation: 'WEIGHTED_AVERAGE', maxScore: 100, minScore: 0, gradeBands: [] })
  }
  scoringConfigDirty.value = false
})

async function handleSaveScoringConfig() {
  if (!selectedSection.value) return
  try {
    await editor.updateScoringConfig(selectedSection.value.id, JSON.stringify(sectionScoringForm))
    scoringConfigDirty.value = false
    ElMessage.success('评分配置已保存')
  } catch (e: any) {
    ElMessage.error(e.message || '保存失败')
  }
}
```

**Step 3: Verify build**

```bash
cd "/d/学生管理系统/frontend"
npm run build 2>&1 | tail -5
```

**Step 4: Commit**

```bash
git add frontend/src/views/inspection/v7/templates/TemplateEditorView.vue
git commit -m "feat: per-section scoring config UI in template editor"
```

---

## Task 12: 更新模板发布逻辑 — 使用 refTemplateId

**Files:**
- Modify: `backend/src/main/java/com/school/management/application/inspection/v7/InspTemplateApplicationService.java`

**Step 1: Update publishTemplate validation**

替换之前的 moduleRef 检查为 refTemplateId 检查：

```java
List<TemplateSection> refSections = sections.stream()
        .filter(s -> s.getRefTemplateId() != null)
        .collect(Collectors.toList());

if (sections.isEmpty() && refSections.isEmpty()) {
    throw new IllegalArgumentException("模板至少需要一个分区才能发布");
}
```

实际上原来的逻辑检查 sections 是否为空就足够了——引用分区也是分区，所以只要 `sections.isEmpty()` 就行。简化为：

```java
if (sections.isEmpty()) {
    throw new IllegalArgumentException("模板至少需要一个分区才能发布");
}
```

**Step 2: Update structure snapshot to include tree info**

sections 已经包含 `parentSectionId` 和 `refTemplateId`，序列化时自然带上，无需额外处理。

可以移除 snapshot 中的 `moduleRefs` 字段（但保持向后兼容也行）。

**Step 3: Update duplicateTemplate to copy tree structure**

当前 `duplicateTemplate` 方法只复制扁平分区。需要更新为保持树形关系：

```java
// 在复制分区时维护 parentSectionId 映射
for (TemplateSection srcSection : sourceSections) {
    Long newParentId = srcSection.getParentSectionId() != null
            ? sectionIdMap.get(srcSection.getParentSectionId())
            : null;
    TemplateSection newSection = TemplateSection.reconstruct(
        TemplateSection.builder()
            .templateId(copy.getId())
            .parentSectionId(newParentId)
            .refTemplateId(srcSection.getRefTemplateId())
            .sectionCode(srcSection.getSectionCode() + "-COPY")
            .sectionName(srcSection.getSectionName())
            .sortOrder(srcSection.getSortOrder())
            .weight(srcSection.getWeight())
            .isRepeatable(srcSection.getIsRepeatable())
            .conditionLogic(srcSection.getConditionLogic())
            .scoringConfig(srcSection.getScoringConfig())
            .createdBy(operatorId)
    );
    newSection = sectionRepository.save(newSection);
    sectionIdMap.put(srcSection.getId(), newSection.getId());
}
```

注意：需要先排序确保父分区在子分区之前被复制（按拓扑排序或先处理 parentSectionId=null 的）。

**Step 4: Verify compile**

```bash
cd "/d/学生管理系统/backend"
JAVA_HOME="/c/Program Files/Java/jdk-17" PATH="/c/Program Files/Java/jdk-17/bin:$PATH" mvn compile -DskipTests 2>&1 | tail -5
```

**Step 5: Commit**

```bash
git add backend/src/main/java/com/school/management/application/inspection/v7/InspTemplateApplicationService.java
git commit -m "feat: update publish and duplicate to support section tree with refs"
```

---

## Task 13: 更新 ProjectWizardView — 展示分区树预览

**Files:**
- Modify: `frontend/src/views/inspection/v7/projects/ProjectWizardView.vue`

**Step 1: Replace moduleRef preview with section tree preview**

在 Step 1 (模板选择) 中，选择模板后展示分区树（包含引用分区），替代之前的子模板列表。

```typescript
// 替换 moduleRefs 和 moduleTemplateCache
const templateSections = ref<TemplateSection[]>([])
const loadingSections = ref(false)

watch(() => form.templateId, async (newId) => {
  templateSections.value = []
  if (!newId) return
  loadingSections.value = true
  try {
    templateSections.value = await inspTemplateApi.getSections(newId)
  } catch { /* ignore */ }
  loadingSections.value = false
})
```

**Step 2: Build tree display**

```typescript
const sectionTree = computed(() => {
  const map = new Map<number | null, TemplateSection[]>()
  for (const s of templateSections.value) {
    const key = s.parentSectionId ?? null
    if (!map.has(key)) map.set(key, [])
    map.get(key)!.push(s)
  }
  for (const list of map.values()) {
    list.sort((a, b) => a.sortOrder - b.sortOrder)
  }
  return map
})
```

**Step 3: Update template**

将 moduleRefs 预览替换为递归的分区树显示，引用分区标注"引用"标签。

**Step 4: Remove moduleRef imports and state**

删除 `templateModuleRefApi` 导入、`moduleRefs` ref、`moduleTemplateCache` ref、`getTemplateName` 函数等。

**Step 5: Update confirm step (Step 4)**

将"含 X 个子模板"改为"含 X 个分区（Y 个引用）"。

**Step 6: Verify build + Commit**

---

## Task 14: 全面编译验证

**Step 1: Backend compile**

```bash
cd "/d/学生管理系统/backend"
JAVA_HOME="/c/Program Files/Java/jdk-17" PATH="/c/Program Files/Java/jdk-17/bin:$PATH" mvn compile -DskipTests 2>&1 | tail -10
```

**Step 2: Frontend build**

```bash
cd "/d/学生管理系统/frontend"
npm run build 2>&1 | tail -10
```

**Step 3: Fix any compilation errors**

逐个修复直到两端都通过。

**Step 4: Final commit**

```bash
git add -A
git commit -m "feat: section tree refactoring - unified tree sections with ref and scoring"
```

---

## 后续 Phase 2（不在本次实施范围）

1. **废弃 ScoringProfile 系统**：评分完全内嵌到分区后，可以移除 `insp_scoring_profiles`, `insp_score_dimensions`, `insp_grade_bands`, `insp_calc_rules` 表和相关代码
2. **分数汇总引擎**：根据分区树的 scoringConfig 递归汇总分数
3. **拖拽排序**：支持分区树的拖拽重排
4. **批量导入**：从 Excel/JSON 导入分区树结构

---

## 验证方式

1. 创建模板 → 添加根分区 → 添加子分区 → 添加字段 → 树形显示正确
2. 引用另一模板作为分区 → 显示"引用"标签 → 分区内容只读
3. 克隆引用分区 → 内容复制为本地 → 可编辑
4. 每个分区独立配置评分（汇总方式 + 等级区间）→ 保存成功
5. 发布模板 → 版本快照包含树形结构和评分配置
6. 创建项目（含引用分区的模板）→ 自动展开为子项目
7. `mvn compile -DskipTests` + `npm run build` 通过
