# GradeScheme 等级方案 — 完整设计文档

> **日期**: 2026-03-27
> **状态**: 设计中
> **目标**: 将等级映射从技术配置提升为一等公民业务概念，支持自定义方案名称和等级名称

---

## 1. 需求概述

### 1.1 核心需求

- 等级映射的**方案名称**支持自定义（如"卫生流动红旗"、"安全星级"、"文明班级"）
- 等级映射的**等级名称**支持自定义（如"红旗/蓝旗/黄旗"而非固定的"A/B/C"）
- 在**模板分区级别**配置（每个分区可绑定不同方案）
- 在**项目级别**汇总展示（用自定义方案名称作为标题）
- 支持全局**库级别复用**（跨模板共享方案）

### 1.2 业务场景示例

```
模板: "校园综合检查"
├── 根分区: "全面检查"       → 等级方案"综合之星"  (金星/银星/铜星)
│   总分 = 卫生 + 纪律 + 安全
│   ├── 子分区: "卫生"       → 等级方案"卫生流动红旗" (红旗/蓝旗/黄旗)
│   ├── 子分区: "纪律"       → 等级方案"纪律之星"    (⭐⭐⭐/⭐⭐/⭐)
│   └── 子分区: "安全"       → 等级方案"安全等级"    (优秀/合格/不合格)

项目汇总 (某班级):
  卫生流动红旗: 蓝旗 (85分)
  纪律之星: ⭐⭐⭐ (95分)
  安全等级: 优秀 (92分)
  ─────────────────
  综合之星: 金星 (总分 272)
```

### 1.3 核心抽象

**每个分区（无论根/子）= 一个分数 + 一个可选的等级方案。**
- 子分区产出分数（通过检查打分）
- 根分区汇总子分区分数（求和/平均/加权等）
- 两者都可以独立挂载 GradeScheme 来把分数映射成自定义名称的等级

---

## 2. 数据模型

### 2.1 新增实体：GradeScheme（等级方案）

```
GradeScheme (聚合根)
├── id: Long                          PK
├── tenantId: Long                    租户隔离
├── displayName: String(100)          "卫生流动红旗" — 方案展示名称
├── description: String(500)          方案描述（可选）
├── schemeType: SchemeType            SCORE_RANGE | PERCENT_RANGE
│   - SCORE_RANGE: 用绝对分数映射 (min=90, max=100)
│   - PERCENT_RANGE: 用百分比映射 (min=90%, max=100% of maxScore)
├── isSystem: Boolean                 系统预设不可删除/修改
├── createdBy: Long                   创建者
├── createdAt: LocalDateTime
├── updatedAt: LocalDateTime
├── deleted: Integer                  逻辑删除
│
└── grades: List<GradeDefinition>     等级定义列表 (1:N)
```

### 2.2 新增实体：GradeDefinition（等级定义）

```
GradeDefinition (实体，GradeScheme 子对象)
├── id: Long                          PK
├── gradeSchemeId: Long               FK → GradeScheme
├── code: String(50)                  等级编码 "RED_FLAG" (程序内部用)
├── name: String(100)                 等级名称 "红旗" (显示用)
├── minValue: BigDecimal              下限值 (含)
├── maxValue: BigDecimal              上限值 (含)
├── color: String(20)                 颜色 "#FF0000"
├── icon: String(50)                  图标 "flag"
├── sortOrder: Integer                排序序号
```

### 2.3 现有实体变更

#### TemplateSection — 新增 gradeSchemeId

```diff
TemplateSection
  ... (existing fields)
+ gradeSchemeId: Long                  FK → GradeScheme (可选)
```

**设计理由**：GradeScheme 挂在 TemplateSection 而非 ScoringProfile 上，因为：
1. 等级方案是**业务概念**（"这个分区的结果叫什么"），不是技术评分配置
2. 分区树决定聚合结构，等级方案跟着分区走最自然
3. 没有 ScoringProfile 的分区也可能需要等级方案（纯汇总的根分区）

#### ProjectScore — 增强快照

```diff
ProjectScore
  ... (existing fields)
+ gradeSchemeDisplayName: String(100)  快照方案名称，展示用
+ gradeName: String(100)               快照等级名称（"红旗"），补充现有 grade 字段
```

**设计理由**：ProjectScore 快照方案名称和等级名称，使得展示不依赖模板/方案的实时数据，历史记录不会因方案修改而变化。

### 2.4 与现有 GradeBand / PolicyGradeBand 的关系

| 现有概念 | 去向 | 说明 |
|---------|------|------|
| `GradeBand` (ScoringProfile 子表) | **逐步替代** | 新分区使用 GradeScheme，旧数据迁移 |
| `PolicyGradeBand` (ScoringPolicy 子表) | **保留** | ScoringPolicy 的计算策略级别的等级映射，与 GradeScheme 不同层级 |
| `RatingDimension.gradeBands` (JSON) | **引用 GradeScheme** | 改为 `gradeSchemeId`，不再内嵌 JSON |

迁移策略：
1. 现有 GradeBand 数据迁移为 GradeScheme（displayName 默认"等级映射"）
2. 现有 RatingDimension.gradeBands JSON 迁移为独立 GradeScheme
3. 旧字段标记 deprecated，保留兼容期

---

## 3. 数据库 Schema

### 3.1 新表：insp_grade_schemes

```sql
CREATE TABLE insp_grade_schemes (
    id              BIGINT          PRIMARY KEY COMMENT '主键',
    tenant_id       BIGINT          NOT NULL COMMENT '租户ID',
    display_name    VARCHAR(100)    NOT NULL COMMENT '方案显示名称，如"卫生流动红旗"',
    description     VARCHAR(500)    DEFAULT NULL COMMENT '方案描述',
    scheme_type     VARCHAR(20)     NOT NULL DEFAULT 'SCORE_RANGE' COMMENT '映射类型: SCORE_RANGE, PERCENT_RANGE',
    is_system       TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '是否系统预设',
    created_by      BIGINT          DEFAULT NULL COMMENT '创建者ID',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT(1)      NOT NULL DEFAULT 0,
    INDEX idx_tenant (tenant_id),
    INDEX idx_display_name (tenant_id, display_name)
) COMMENT '等级方案';
```

### 3.2 新表：insp_grade_definitions

```sql
CREATE TABLE insp_grade_definitions (
    id                BIGINT          PRIMARY KEY COMMENT '主键',
    grade_scheme_id   BIGINT          NOT NULL COMMENT 'FK → insp_grade_schemes.id',
    code              VARCHAR(50)     NOT NULL COMMENT '等级编码，如 RED_FLAG',
    name              VARCHAR(100)    NOT NULL COMMENT '等级名称，如 红旗',
    min_value         DECIMAL(10,2)   NOT NULL COMMENT '下限值（含）',
    max_value         DECIMAL(10,2)   NOT NULL COMMENT '上限值（含）',
    color             VARCHAR(20)     DEFAULT NULL COMMENT '颜色代码',
    icon              VARCHAR(50)     DEFAULT NULL COMMENT '图标标识',
    sort_order        INT             NOT NULL DEFAULT 0 COMMENT '排序',
    INDEX idx_scheme (grade_scheme_id)
) COMMENT '等级定义';
```

### 3.3 现有表变更

```sql
-- TemplateSection 新增字段
ALTER TABLE insp_template_sections
    ADD COLUMN grade_scheme_id BIGINT DEFAULT NULL COMMENT 'FK → insp_grade_schemes.id';

-- ProjectScore 增强
ALTER TABLE insp_project_scores
    ADD COLUMN grade_scheme_display_name VARCHAR(100) DEFAULT NULL COMMENT '等级方案名称快照',
    ADD COLUMN grade_name VARCHAR(100) DEFAULT NULL COMMENT '等级名称快照';
```

### 3.4 系统预设数据

```sql
-- 预设1: 标准五级 (A-F)
INSERT INTO insp_grade_schemes (id, tenant_id, display_name, scheme_type, is_system)
VALUES (1, 0, '标准五级评定', 'PERCENT_RANGE', 1);

INSERT INTO insp_grade_definitions (id, grade_scheme_id, code, name, min_value, max_value, color, sort_order) VALUES
(101, 1, 'A', '优秀', 90, 100, '#22C55E', 1),
(102, 1, 'B', '良好', 80, 89, '#3B82F6', 2),
(103, 1, 'C', '中等', 70, 79, '#F59E0B', 3),
(104, 1, 'D', '及格', 60, 69, '#F97316', 4),
(105, 1, 'F', '不及格', 0, 59, '#EF4444', 5);

-- 预设2: 二级评定 (合格/不合格)
INSERT INTO insp_grade_schemes (id, tenant_id, display_name, scheme_type, is_system)
VALUES (2, 0, '合格评定', 'PERCENT_RANGE', 1);

INSERT INTO insp_grade_definitions (id, grade_scheme_id, code, name, min_value, max_value, color, sort_order) VALUES
(201, 2, 'PASS', '合格', 60, 100, '#22C55E', 1),
(202, 2, 'FAIL', '不合格', 0, 59, '#EF4444', 2);

-- 预设3: 流动红旗
INSERT INTO insp_grade_schemes (id, tenant_id, display_name, scheme_type, is_system)
VALUES (3, 0, '流动红旗', 'PERCENT_RANGE', 1);

INSERT INTO insp_grade_definitions (id, grade_scheme_id, code, name, min_value, max_value, color, icon, sort_order) VALUES
(301, 3, 'RED',    '红旗', 90, 100, '#EF4444', 'flag', 1),
(302, 3, 'BLUE',   '蓝旗', 70, 89,  '#3B82F6', 'flag', 2),
(303, 3, 'YELLOW', '黄旗', 0,  69,  '#F59E0B', 'flag', 3);

-- 预设4: 星级评定
INSERT INTO insp_grade_schemes (id, tenant_id, display_name, scheme_type, is_system)
VALUES (4, 0, '星级评定', 'PERCENT_RANGE', 1);

INSERT INTO insp_grade_definitions (id, grade_scheme_id, code, name, min_value, max_value, color, icon, sort_order) VALUES
(401, 4, '5STAR', '⭐⭐⭐⭐⭐', 90, 100, '#FFD700', 'star', 1),
(402, 4, '4STAR', '⭐⭐⭐⭐',   80, 89,  '#FFA500', 'star', 2),
(403, 4, '3STAR', '⭐⭐⭐',     70, 79,  '#C0C0C0', 'star', 3),
(404, 4, '2STAR', '⭐⭐',       60, 69,  '#CD7F32', 'star', 4),
(405, 4, '1STAR', '⭐',         0,  59,  '#808080', 'star', 5);
```

---

## 4. 后端领域模型

### 4.1 聚合根：GradeScheme

```java
package com.school.management.domain.inspection.model.v7.scoring;

@Getter
public class GradeScheme extends AggregateRoot {
    private Long tenantId;
    private String displayName;       // "卫生流动红旗"
    private String description;
    private SchemeType schemeType;     // SCORE_RANGE, PERCENT_RANGE
    private Boolean isSystem;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 子实体
    private List<GradeDefinition> grades;

    public enum SchemeType {
        SCORE_RANGE,      // 绝对分数: min=90, max=100
        PERCENT_RANGE     // 百分比: min=90%, max=100% (需结合 maxScore 计算)
    }

    // 工厂方法
    public static GradeScheme create(Long tenantId, String displayName,
                                      SchemeType schemeType, Long createdBy) { ... }

    // 从系统预设克隆（修改 displayName）
    public GradeScheme cloneAsCustom(String newDisplayName, Long tenantId, Long createdBy) { ... }

    // 更新方案信息
    public void updateInfo(String displayName, String description) {
        guardSystemPreset();
        this.displayName = displayName;
        this.description = description;
        this.updatedAt = LocalDateTime.now();
    }

    // 设置等级定义（整体替换）
    public void setGrades(List<GradeDefinition> grades) {
        guardSystemPreset();
        validateNoOverlap(grades);
        validateNoCoverage(grades);   // 可选: 检查是否覆盖完整区间
        this.grades = new ArrayList<>(grades);
        this.updatedAt = LocalDateTime.now();
    }

    // 根据分数匹配等级
    public Optional<GradeDefinition> matchGrade(BigDecimal score) {
        return grades.stream()
            .filter(g -> score.compareTo(g.getMinValue()) >= 0
                      && score.compareTo(g.getMaxValue()) <= 0)
            .findFirst();
    }

    // 根据分数和满分匹配等级 (PERCENT_RANGE 模式)
    public Optional<GradeDefinition> matchGrade(BigDecimal score, BigDecimal maxScore) {
        if (schemeType == SchemeType.SCORE_RANGE) {
            return matchGrade(score);
        }
        // 百分比模式: 先算百分比再匹配
        BigDecimal percent = score.multiply(BigDecimal.valueOf(100))
                                  .divide(maxScore, 2, RoundingMode.HALF_UP);
        return matchGrade(percent);
    }

    // 生成快照 (用于 ProjectScore)
    public GradeSchemeSnapshot toSnapshot() {
        return new GradeSchemeSnapshot(displayName,
            grades.stream().map(GradeDefinition::toSnapshot).toList());
    }

    private void guardSystemPreset() {
        if (Boolean.TRUE.equals(isSystem)) {
            throw new DomainException("系统预设方案不可修改");
        }
    }

    private void validateNoOverlap(List<GradeDefinition> grades) { ... }
}
```

### 4.2 实体：GradeDefinition

```java
@Getter
public class GradeDefinition extends Entity {
    private Long gradeSchemeId;
    private String code;          // "RED_FLAG"
    private String name;          // "红旗"
    private BigDecimal minValue;
    private BigDecimal maxValue;
    private String color;
    private String icon;
    private Integer sortOrder;

    public static GradeDefinition create(String code, String name,
            BigDecimal minValue, BigDecimal maxValue,
            String color, String icon, Integer sortOrder) { ... }

    public void update(String name, BigDecimal minValue, BigDecimal maxValue,
                       String color, String icon) { ... }

    public GradeDefinitionSnapshot toSnapshot() {
        return new GradeDefinitionSnapshot(code, name, minValue, maxValue, color, icon);
    }
}
```

### 4.3 值对象：GradeSchemeSnapshot

```java
// 快照，嵌入 ProjectScore.detail JSON 中，不可变
public record GradeSchemeSnapshot(
    String displayName,
    List<GradeDefinitionSnapshot> grades
) {}

public record GradeDefinitionSnapshot(
    String code, String name,
    BigDecimal minValue, BigDecimal maxValue,
    String color, String icon
) {}
```

### 4.4 仓储接口

```java
public interface GradeSchemeRepository {
    GradeScheme save(GradeScheme scheme);
    Optional<GradeScheme> findById(Long id);
    List<GradeScheme> findByTenantId(Long tenantId);            // 该租户所有方案
    List<GradeScheme> findSystemPresets();                       // 系统预设
    List<GradeScheme> findByTenantIdOrSystem(Long tenantId);    // 租户 + 系统预设
    void deleteById(Long id);
}
```

---

## 5. API 设计

### 5.1 等级方案 CRUD

```
# 列表（含系统预设 + 租户自定义）
GET /v7/insp/grade-schemes
Response: [{id, displayName, description, schemeType, isSystem, grades: [...]}]

# 创建自定义方案
POST /v7/insp/grade-schemes
Body: {
    displayName: "卫生流动红旗",
    description: "根据卫生检查分数评定流动红旗",
    schemeType: "SCORE_RANGE",
    grades: [
        { code: "RED", name: "红旗", minValue: 90, maxValue: 100, color: "#EF4444", icon: "flag" },
        { code: "BLUE", name: "蓝旗", minValue: 70, maxValue: 89, color: "#3B82F6", icon: "flag" },
        { code: "YELLOW", name: "黄旗", minValue: 0, maxValue: 69, color: "#F59E0B", icon: "flag" }
    ]
}
Response: { id: 12345, ... }

# 从系统预设克隆
POST /v7/insp/grade-schemes/clone
Body: { sourceSchemeId: 3, displayName: "卫生流动红旗" }
Response: { id: 12346, ... }

# 更新
PUT /v7/insp/grade-schemes/{id}
Body: { displayName, description, grades: [...] }

# 删除（仅自定义方案）
DELETE /v7/insp/grade-schemes/{id}
```

### 5.2 分区绑定等级方案

```
# 绑定/更换方案
PUT /v7/insp/sections/{sectionId}/grade-scheme
Body: { gradeSchemeId: 12345 }

# 解绑方案
DELETE /v7/insp/sections/{sectionId}/grade-scheme

# 查询分区的等级方案
GET /v7/insp/sections/{sectionId}/grade-scheme
Response: { id, displayName, schemeType, grades: [...] }
```

### 5.3 项目汇总展示

```
# 项目评分汇总（已有接口增强）
GET /v7/insp/projects/{projectId}/scores?cycleDate=2026-03-27
Response: {
    projectId: 1,
    cycleDate: "2026-03-27",
    sections: [
        {
            sectionId: 10,
            sectionName: "卫生",
            score: 85.00,
            gradeScheme: {                    ← 新增
                displayName: "卫生流动红旗",
                matchedGrade: {
                    code: "BLUE",
                    name: "蓝旗",
                    color: "#3B82F6",
                    icon: "flag"
                }
            }
        },
        {
            sectionId: 20,
            sectionName: "纪律",
            score: 95.00,
            gradeScheme: {
                displayName: "纪律之星",
                matchedGrade: { code: "5STAR", name: "⭐⭐⭐⭐⭐", color: "#FFD700" }
            }
        },
        {
            sectionId: 1,
            sectionName: "全面检查",
            score: 272.00,
            isRoot: true,
            gradeScheme: {
                displayName: "综合之星",
                matchedGrade: { code: "GOLD", name: "金星", color: "#FFD700" }
            }
        }
    ]
}
```

---

## 6. 前端设计

### 6.1 类型定义

```typescript
// types/insp/gradeScheme.ts

export interface GradeScheme {
    id: number
    displayName: string          // "卫生流动红旗"
    description?: string
    schemeType: 'SCORE_RANGE' | 'PERCENT_RANGE'
    isSystem: boolean
    grades: GradeDefinition[]
}

export interface GradeDefinition {
    id?: number
    code: string                 // "RED_FLAG"
    name: string                 // "红旗"
    minValue: number
    maxValue: number
    color?: string
    icon?: string
    sortOrder: number
}

export interface GradeMatchResult {
    code: string
    name: string
    color?: string
    icon?: string
}

export interface SectionScoreWithGrade {
    sectionId: number
    sectionName: string
    score: number
    isRoot?: boolean
    gradeScheme?: {
        displayName: string
        matchedGrade?: GradeMatchResult
    }
}
```

### 6.2 模板编辑器 — 分区等级方案配置

在模板编辑器的分区设置面板中新增"等级方案"配置区域：

```
┌─────────────────────────────────────────────────┐
│ 分区: 卫生检查                                    │
│                                                  │
│ 基本信息  评分配置  [等级方案]  检查项              │
│ ─────────────────────────────────────────        │
│                                                  │
│ 等级方案                                          │
│ ┌─── 选择方案 ──────────────────────────┐        │
│ │  ○ 无等级映射                          │        │
│ │  ● 选择已有方案  [卫生流动红旗 ▾]       │        │
│ │  ○ 新建方案                            │        │
│ └────────────────────────────────────────┘        │
│                                                  │
│ 方案预览: 卫生流动红旗                             │
│ ┌────────┬──────┬──────┬───────┬───────┐         │
│ │ 等级   │ 名称 │ 下限 │ 上限  │ 颜色  │         │
│ ├────────┼──────┼──────┼───────┼───────┤         │
│ │ 🚩     │ 红旗 │ 90   │ 100   │ 🔴   │         │
│ │ 🚩     │ 蓝旗 │ 70   │ 89    │ 🔵   │         │
│ │ 🚩     │ 黄旗 │ 0    │ 69    │ 🟡   │         │
│ └────────┴──────┴──────┴───────┴───────┘         │
│                              [编辑方案] [解绑]    │
└─────────────────────────────────────────────────┘
```

### 6.3 等级方案编辑器（弹窗/抽屉）

```
┌────────────────────────────────────────────────────┐
│ 编辑等级方案                                 [×]   │
│                                                    │
│ 方案名称  [卫生流动红旗          ]                   │
│ 方案描述  [根据卫生检查分数评定... ]                   │
│ 映射类型  [绝对分数 ▾]                               │
│                                                    │
│ 快速模板:  [五级] [二级] [流动红旗] [星级] [自定义]    │
│                                                    │
│ 等级定义                                            │
│ ┌────┬────────┬──────┬──────┬────────┬─────┬───┐   │
│ │ #  │ 编码   │ 名称 │ 下限 │ 上限   │ 颜色│ ⋮ │   │
│ ├────┼────────┼──────┼──────┼────────┼─────┼───┤   │
│ │ 1  │ RED    │ 红旗 │ 90   │ 100    │ 🔴 │ ✕ │   │
│ │ 2  │ BLUE   │ 蓝旗 │ 70   │ 89     │ 🔵 │ ✕ │   │
│ │ 3  │ YELLOW │ 黄旗 │ 0    │ 69     │ 🟡 │ ✕ │   │
│ └────┴────────┴──────┴──────┴────────┴─────┴───┘   │
│                                    [+ 添加等级]     │
│                                                    │
│ ⚠ 区间覆盖检查: ✅ 完整覆盖 0-100                    │
│                                                    │
│                        [取消]  [保存]               │
└────────────────────────────────────────────────────┘
```

### 6.4 项目汇总展示

```
┌──────────────────────────────────────────────────────────┐
│ 项目: 第12周校园综合检查          2026-03-27             │
│                                                          │
│ 成绩汇总                                                 │
│ ┌─────────────────────────────────────────────────────┐  │
│ │ 班级    │ 卫生流动红旗 │ 纪律之星  │ 安全等级 │ 综合之星 │  │
│ ├─────────┼────────────┼──────────┼─────────┼─────────┤  │
│ │ 高一(1) │ 🔴 红旗 95 │ ⭐⭐⭐ 92 │ 优秀 88 │ 🥇金星  │  │
│ │ 高一(2) │ 🔵 蓝旗 82 │ ⭐⭐  75 │ 合格 71 │ 🥈银星  │  │
│ │ 高一(3) │ 🟡 黄旗 58 │ ⭐   55 │ 不合格60│ 🥉铜星  │  │
│ └─────────┴────────────┴──────────┴─────────┴─────────┘  │
│                                                          │
│ 表头即为等级方案的 displayName，自动从分区配置读取          │
└──────────────────────────────────────────────────────────┘
```

---

## 7. 服务层变更

### 7.1 GradeSchemeApplicationService（新增）

```java
@Service
public class GradeSchemeApplicationService {

    // CRUD
    public GradeScheme createScheme(CreateGradeSchemeCmd cmd) { ... }
    public GradeScheme cloneFromPreset(Long sourceId, String displayName) { ... }
    public void updateScheme(Long id, UpdateGradeSchemeCmd cmd) { ... }
    public void deleteScheme(Long id) { ... }
    public List<GradeScheme> listSchemes(Long tenantId) { ... }

    // 绑定
    public void bindToSection(Long sectionId, Long gradeSchemeId) { ... }
    public void unbindFromSection(Long sectionId) { ... }
}
```

### 7.2 ScoreAggregationService 变更

```diff
  // 现有: determineGrade 走 ScoringProfile → GradeBand
  // 新增: 优先走 TemplateSection → GradeScheme

  public String determineGrade(Long sectionId, BigDecimal score) {
+     // 1. 优先: 分区绑定的 GradeScheme
+     TemplateSection section = sectionRepository.findById(sectionId);
+     if (section.getGradeSchemeId() != null) {
+         GradeScheme scheme = gradeSchemeRepository.findById(section.getGradeSchemeId());
+         BigDecimal maxScore = getMaxScore(sectionId);
+         return scheme.matchGrade(score, maxScore)
+                      .map(GradeDefinition::getName)
+                      .orElse(null);
+     }
-     // 2. 回退: 旧的 ScoringProfile → GradeBand 路径
+     // 2. 回退: 旧的 ScoringProfile → GradeBand 路径 (兼容)
      return determineGradeFromProfile(sectionId, score);
  }
```

### 7.3 ProjectScore 写入增强

```diff
  public void gradeProjectScore(Long projectId, LocalDate cycleDate) {
      BigDecimal score = computeProjectScore(projectId, cycleDate);
      String grade = determineGrade(rootSectionId, score);
+     // 快照方案信息
+     GradeScheme scheme = getSchemeForSection(rootSectionId);
+     String schemeDisplayName = scheme != null ? scheme.getDisplayName() : null;
+     String gradeName = scheme != null
+         ? scheme.matchGrade(score, maxScore).map(GradeDefinition::getName).orElse(grade)
+         : grade;

      projectScore.updateScore(score, grade, targetCount, detail);
+     projectScore.setGradeSchemeDisplayName(schemeDisplayName);
+     projectScore.setGradeName(gradeName);
  }
```

---

## 8. 数据迁移方案

### 8.1 迁移脚本

```sql
-- Step 1: 为现有 GradeBand 数据生成 GradeScheme
-- 每个有 GradeBand 的 ScoringProfile 生成一个 GradeScheme
INSERT INTO insp_grade_schemes (id, tenant_id, display_name, scheme_type, is_system, created_at, updated_at)
SELECT
    sp.id + 100000,                              -- offset 避免 ID 冲突
    sp.tenant_id,
    CONCAT('等级映射-', sp.id),                   -- 默认名称，用户可后续修改
    'SCORE_RANGE',
    0,
    NOW(), NOW()
FROM insp_scoring_profiles sp
WHERE EXISTS (SELECT 1 FROM insp_grade_bands gb WHERE gb.scoring_profile_id = sp.id);

-- Step 2: 迁移 GradeBand → GradeDefinition
INSERT INTO insp_grade_definitions (id, grade_scheme_id, code, name, min_value, max_value, color, icon, sort_order)
SELECT
    gb.id + 100000,
    gb.scoring_profile_id + 100000,              -- 对应新 scheme
    gb.grade_code,
    gb.grade_name,
    gb.min_score,
    gb.max_score,
    gb.color,
    gb.icon,
    gb.sort_order
FROM insp_grade_bands gb;

-- Step 3: 关联 Section → GradeScheme
UPDATE insp_template_sections ts
JOIN insp_scoring_profiles sp ON sp.section_id = ts.id
SET ts.grade_scheme_id = sp.id + 100000
WHERE EXISTS (SELECT 1 FROM insp_grade_bands gb WHERE gb.scoring_profile_id = sp.id);
```

---

## 9. 实现优先级

| 阶段 | 内容 | 依赖 |
|------|------|------|
| P0 | DB Schema + 领域模型 + 仓储 | 无 |
| P1 | GradeScheme CRUD API | P0 |
| P2 | 分区绑定 API + ScoreAggregation 适配 | P0, P1 |
| P3 | 模板编辑器 — 等级方案选择/编辑 UI | P1 |
| P4 | 项目汇总展示增强 | P2 |
| P5 | 数据迁移脚本 | P0 |
| P6 | 旧 GradeBand 路径标记 deprecated | P5 |
