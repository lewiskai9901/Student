# V7 检查平台全面升级计划

> 生成日期: 2026-03-11
> 基于 V7 模块 133 个后端类 + 106 个前端文件的完整代码审计

---

## 现有系统能力总结

| 模块 | 已实现功能 |
|------|-----------|
| 模板 | 22种字段类型、分区层级、条件逻辑V2(AND/OR嵌套)、版本快照、响应集、验证规则、模板目录树、导入导出、版本Diff |
| 评分 | 13种评分模式、维度加权、4种规则(VETO/PROGRESSIVE/BONUS/CUSTOM)、等级映射、4种归一化、公式引擎、评分模拟器 |
| 执行 | 多种周期调度、检查员轮转(ROUND_ROBIN/RANDOM/LOAD_BALANCED)、离线同步(IndexedDB)、证据采集(照片/视频/文档/签名/GPS)、批量操作、移动端打分 |
| 纠正 | CAPA工作流、RCA(鱼骨图/5-Why/故障树)、有效性验证、自动创建、SLA自动升级 |
| 分析 | 日/周/月/季/年汇总、趋势分析、帕累托图、雷达图、检查员绩效、归一化影响图、导出对话框 |
| 平台 | 审计追踪、假日日历、通知规则、报告模板、Webhook、22项权限定义 |

---

## 升级功能清单 (32项)

### 第一阶段: 评分引擎深度增强 (P0 - 核心竞争力)

#### 1.1 检查项权重 (Item Weight)

**目标**: 同一维度内的检查项可设置不同权重，而不是等权平均。

**数据库**:
```sql
-- V41.0.0__item_weight.sql
ALTER TABLE insp_template_items ADD COLUMN weight DECIMAL(5,2) DEFAULT 1.00 COMMENT '项目权重(维度内)';
ALTER TABLE insp_submission_details ADD COLUMN item_weight DECIMAL(5,2) DEFAULT 1.00 COMMENT '快照:提交时的项目权重';
```

**后端**:
- `TemplateItem.java` 新增 `weight` 字段 + `setWeight(BigDecimal)` 方法
- `TemplateItemPO.java` / `TemplateItemRepositoryImpl.java` 映射
- `TemplateItemApplicationService.java` createItem/updateItem 参数增加 weight
- `TemplateItemController.java` CreateItemRequest/UpdateItemRequest 增加 weight

**前端**:
- `types/insp/template.ts` TemplateItem 增加 `weight: number`
- `ItemEditor.vue` 增加权重输入框(在评分配置面板内，仅 isScored=true 时显示)
- `ScoreSimulator.vue` 计算逻辑改为加权: `dimensionScore = Σ(itemScore × itemWeight) / Σ(itemWeight)`
- `ScoringItemRow.vue` 显示权重标签

**计算公式变更**:
```
当前: dimensionScore = SUM(itemScores) / itemCount
改后: dimensionScore = Σ(itemScore × weight) / Σ(weight)
```

---

#### 1.2 跨维度规则 (Cross-Dimension Rules)

**目标**: 创建涉及多个维度得分的复合规则。例如"若卫生维度<60且安全维度<60，总分直接判定F"。

**数据库**:
```sql
-- V41.0.0 (同批)
ALTER TABLE insp_calculation_rules ADD COLUMN scope_type VARCHAR(20) DEFAULT 'GLOBAL' COMMENT 'GLOBAL|DIMENSION|CROSS_DIMENSION';
ALTER TABLE insp_calculation_rules ADD COLUMN target_dimension_ids JSON COMMENT '跨维度规则关联的维度ID列表';
```

**后端**:
- `CalculationRuleV7.java` 新增 `scopeType`(枚举: GLOBAL/DIMENSION/CROSS_DIMENSION), `targetDimensionIds`(List<Long>)
- 评分计算引擎修改: 在维度汇总后、全局规则前插入跨维度规则执行阶段

**前端**:
- `CalcRuleChain.vue` 创建/编辑对话框增加 scopeType 选择
- 当 scopeType=CROSS_DIMENSION 时显示维度多选组件
- `RuleConfigForm.vue` 条件表达式支持引用 `dim[dimensionId].score`

**执行管线变更**:
```
Item → Dimension(SUM) → [跨维度规则] → Total(WEIGHTED_AVG) → [全局规则] → Clamp → Grade
```

---

#### 1.3 重复违规递增扣分 (Repeat Violation Escalation)

**目标**: 同一对象在周期内重复出现同类问题时，扣分递增(如首次-2，再次-4，三次-8)。

**数据库**:
```sql
-- V42.0.0__repeat_violation_escalation.sql
CREATE TABLE insp_escalation_policies (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  profile_id BIGINT NOT NULL COMMENT '关联评分配置',
  policy_name VARCHAR(100) NOT NULL,
  lookup_period_days INT NOT NULL DEFAULT 30 COMMENT '回溯天数',
  escalation_mode VARCHAR(20) NOT NULL DEFAULT 'MULTIPLY' COMMENT 'MULTIPLY|ADD|FIXED_TABLE',
  multiplier DECIMAL(4,2) DEFAULT 2.0 COMMENT '乘数(MULTIPLY模式)',
  adder DECIMAL(6,2) DEFAULT NULL COMMENT '增加值(ADD模式)',
  fixed_table JSON DEFAULT NULL COMMENT '固定表(FIXED_TABLE模式): [{occurrence:1,factor:1.0},{occurrence:2,factor:2.0},...]',
  max_escalation_factor DECIMAL(4,2) DEFAULT 5.0 COMMENT '最大放大倍数',
  match_by VARCHAR(50) NOT NULL DEFAULT 'ITEM_CODE' COMMENT 'ITEM_CODE|DIMENSION|SECTION|CATEGORY',
  is_enabled TINYINT DEFAULT 1,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  INDEX idx_profile (profile_id)
) COMMENT='重复违规递增策略';
```

**后端**:
- 新建 `domain/inspection/model/v7/scoring/EscalationPolicy.java`
- 新建 `EscalationPolicyRepository.java` + 基础设施实现
- `ScoringProfileApplicationService.java` 增加 escalation CRUD
- 打分时: 查询 `insp_submission_details` 按 target+itemCode 统计回溯期内出现次数 → 应用倍数

**前端**:
- `ScoringProfileEditor.vue` 左栏新增 "递增策略" 卡片
- 新建 `components/EscalationPolicyEditor.vue`
  - 策略名称、回溯天数、匹配维度、递增模式(乘法/加法/固定表)
  - 固定表模式支持动态行: [次数, 倍率]
- `ScoreSimulator.vue` 增加"模拟第N次违规"输入

**API**:
```
GET    /api/v7/scoring-profiles/{id}/escalation-policies
POST   /api/v7/scoring-profiles/{id}/escalation-policies
PUT    /api/v7/scoring-profiles/{id}/escalation-policies/{policyId}
DELETE /api/v7/scoring-profiles/{id}/escalation-policies/{policyId}
```

---

#### 1.4 条件触发规则 (Conditional Rules)

**目标**: 规则仅在特定条件满足时激活。例如"周末检查不执行扣分规则R3"、"仅对一年级执行纪律加倍规则"。

**数据库**:
```sql
-- V42.0.0 (同批)
ALTER TABLE insp_calculation_rules ADD COLUMN activation_condition JSON COMMENT '激活条件(条件逻辑V2格式)';
ALTER TABLE insp_calculation_rules ADD COLUMN applies_to JSON COMMENT '适用范围: {targetTypes:[], orgUnitIds:[], userTypes:[]}';
```

**后端**:
- `CalculationRuleV7.java` 新增 `activationCondition`(String JSON), `appliesTo`(String JSON)
- 规则执行引擎: 先评估 activation_condition → 符合才应用规则
- 复用已有的条件逻辑V2引擎 (`conditionLogicEngine.ts` 后端镜像)

**前端**:
- `RuleConfigForm.vue` 增加"激活条件"折叠面板
  - 复用条件逻辑编辑器(useConditionLogic composable)
- 增加"适用范围"多选: 目标类型/组织单元/用户类型

---

#### 1.5 规则有效期 (Rule Effective Period)

**目标**: 规则仅在指定日期范围内生效。如"学期末加分规则仅12月生效"。

**数据库**:
```sql
-- V42.0.0 (同批)
ALTER TABLE insp_calculation_rules ADD COLUMN effective_from DATE DEFAULT NULL COMMENT '生效起始日';
ALTER TABLE insp_calculation_rules ADD COLUMN effective_until DATE DEFAULT NULL COMMENT '生效截止日';
```

**后端/前端**:
- 字段映射到 domain/PO/DTO/Request 全栈
- 规则执行时检查 `LocalDate.now()` 是否在区间内
- `CalcRuleChain.vue` 规则卡片显示有效期标签(已过期/即将生效/生效中)
- 编辑对话框增加日期范围选择器

---

#### 1.6 规则互斥 (Rule Mutual Exclusion)

**目标**: 防止冲突规则同时生效。如VETO规则和BONUS规则互斥。

**数据库**:
```sql
-- V42.0.0 (同批)
ALTER TABLE insp_calculation_rules ADD COLUMN exclusion_group VARCHAR(50) DEFAULT NULL COMMENT '互斥组名';
```

**后端**: 同一 exclusion_group 内，只有优先级最高(priority最小)的生效规则被执行。
**前端**: `CalcRuleChain.vue` 互斥组可视化: 同组规则用相同颜色左边框标记。

---

#### 1.7 评分配置版本化 (Scoring Profile Versioning)

**目标**: 评分配置随模板发布一起生成版本快照，历史检查结果可追溯使用当时的评分配置。

**数据库**:
```sql
-- V43.0.0__scoring_profile_versioning.sql
ALTER TABLE insp_scoring_profiles ADD COLUMN current_version INT DEFAULT 0;

CREATE TABLE insp_scoring_profile_versions (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  profile_id BIGINT NOT NULL,
  version INT NOT NULL,
  snapshot JSON NOT NULL COMMENT '完整配置快照(dimensions+rules+bands+escalation)',
  published_at DATETIME NOT NULL,
  published_by BIGINT,
  change_summary VARCHAR(500),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  UNIQUE KEY uk_profile_version (profile_id, version)
) COMMENT='评分配置版本快照';

ALTER TABLE insp_submissions ADD COLUMN scoring_profile_version INT DEFAULT NULL COMMENT '使用的评分配置版本';
```

**后端**:
- 模板发布时(`InspTemplateApplicationService.publishTemplate`)同步生成评分配置快照
- `ScoringProfileVersion.java` 领域模型
- 打分时记录使用的配置版本号

**前端**:
- `ScoringProfileEditor.vue` 右侧栏增加"版本历史"面板
- 支持查看历史版本配置详情(只读)

---

#### 1.8 基准对标 / 百分位 (Benchmarking & Percentile)

**目标**: 计算得分在同类目标中的百分位排名，支持同比/环比。

**数据库**:
```sql
-- V44.0.0__benchmarking.sql
ALTER TABLE insp_daily_summaries ADD COLUMN percentile DECIMAL(5,2) DEFAULT NULL COMMENT '百分位(0-100)';
ALTER TABLE insp_daily_summaries ADD COLUMN rank_in_group INT DEFAULT NULL COMMENT '组内排名';
ALTER TABLE insp_daily_summaries ADD COLUMN group_total INT DEFAULT NULL COMMENT '组内总数';
ALTER TABLE insp_daily_summaries ADD COLUMN group_avg DECIMAL(8,2) DEFAULT NULL COMMENT '组平均分';
ALTER TABLE insp_daily_summaries ADD COLUMN group_median DECIMAL(8,2) DEFAULT NULL COMMENT '组中位数';
```

**后端**:
- `AnalyticsProjectionService.java` 在日汇总计算后追加百分位/排名计算
- 分组依据: 同项目 + 同target_type + 同日期

**前端**:
- `AnalyticsDashboardView.vue` 新增百分位分布图组件
- `RankingTable.vue` 增加百分位列

---

#### 1.9 趋势因子 (Trend Factor)

**目标**: 得分计算可加入趋势权重 — 持续改善的给奖励分，持续恶化的施加惩罚。

**数据库**:
```sql
-- V44.0.0 (同批)
ALTER TABLE insp_scoring_profiles ADD COLUMN trend_factor_enabled TINYINT DEFAULT 0;
ALTER TABLE insp_scoring_profiles ADD COLUMN trend_lookback_days INT DEFAULT 7;
ALTER TABLE insp_scoring_profiles ADD COLUMN trend_bonus_per_percent DECIMAL(4,2) DEFAULT 0.5 COMMENT '每1%进步奖励分';
ALTER TABLE insp_scoring_profiles ADD COLUMN trend_penalty_per_percent DECIMAL(4,2) DEFAULT 0.3 COMMENT '每1%退步惩罚分';
ALTER TABLE insp_scoring_profiles ADD COLUMN trend_max_adjustment DECIMAL(6,2) DEFAULT 10.0 COMMENT '趋势最大调整值';
```

**后端**:
- 打分后处理阶段: 查询该目标最近N天得分 → 计算趋势方向和幅度 → 应用加/减分
- 在规则链之后、Clamp之前执行

**前端**:
- `ScoringProfileEditor.vue` 基础设置增加"趋势因子"开关及参数配置
- `ScoreSimulator.vue` 增加"模拟历史趋势"输入(上升/下降/持平)

---

#### 1.10 分数衰减 (Score Decay)

**目标**: 长期未检查的目标，历史得分按时间衰减，防止"查一次吃老本"。

**数据库**:
```sql
-- V44.0.0 (同批)
ALTER TABLE insp_scoring_profiles ADD COLUMN decay_enabled TINYINT DEFAULT 0;
ALTER TABLE insp_scoring_profiles ADD COLUMN decay_mode VARCHAR(20) DEFAULT 'LINEAR' COMMENT 'LINEAR|EXPONENTIAL';
ALTER TABLE insp_scoring_profiles ADD COLUMN decay_rate_per_day DECIMAL(6,4) DEFAULT 0.5 COMMENT '每天衰减值/率';
ALTER TABLE insp_scoring_profiles ADD COLUMN decay_floor DECIMAL(6,2) DEFAULT 60.0 COMMENT '衰减下限';
```

**后端**:
- 定时任务: 每日检查哪些目标超过N天未检查 → 按衰减公式调整 daily_summary 的 total_score
- `LINEAR`: score = max(floor, lastScore - rate × daysSince)
- `EXPONENTIAL`: score = max(floor, lastScore × (1-rate)^daysSince)

**前端**:
- `ScoringProfileEditor.vue` 基础设置增加"分数衰减"开关及参数

---

#### 1.11 多评审员聚合 (Multi-Rater Aggregation)

**目标**: 同一目标被多个检查员检查时，如何聚合多份打分结果。

**数据库**:
```sql
-- V45.0.0__multi_rater.sql
ALTER TABLE insp_scoring_profiles ADD COLUMN multi_rater_mode VARCHAR(30) DEFAULT 'LATEST'
  COMMENT 'LATEST|AVERAGE|WEIGHTED_AVERAGE|MEDIAN|MAX|MIN|CONSENSUS';
ALTER TABLE insp_scoring_profiles ADD COLUMN rater_weight_by VARCHAR(30) DEFAULT NULL
  COMMENT 'EQUAL|BY_ROLE|BY_EXPERIENCE|CUSTOM';
ALTER TABLE insp_scoring_profiles ADD COLUMN consensus_threshold DECIMAL(4,2) DEFAULT 0.8
  COMMENT '共识阈值(0-1)，偏差超过时标记需复核';
```

**后端**:
- 提交后: 检测同目标同日期是否有多份 submission → 按模式聚合
- CONSENSUS模式: 计算标准差，超过阈值时发送复核通知

**前端**:
- `ScoringProfileEditor.vue` 基础设置增加"多评审聚合"配置
- `TaskReviewWorkbenchView.vue` 显示多份打分对比视图

---

#### 1.12 分布校准 (Distribution Calibration)

**目标**: 不同检查员的打分尺度不一，通过统计方法校准消除系统性偏差。

**数据库**:
```sql
-- V45.0.0 (同批)
ALTER TABLE insp_scoring_profiles ADD COLUMN calibration_enabled TINYINT DEFAULT 0;
ALTER TABLE insp_scoring_profiles ADD COLUMN calibration_method VARCHAR(30) DEFAULT 'Z_SCORE'
  COMMENT 'Z_SCORE|MIN_MAX|PERCENTILE_RANK|IRT';
ALTER TABLE insp_scoring_profiles ADD COLUMN calibration_period_days INT DEFAULT 30
  COMMENT '校准样本回溯天数';
ALTER TABLE insp_scoring_profiles ADD COLUMN calibration_min_samples INT DEFAULT 10
  COMMENT '最少样本数才启用校准';

CREATE TABLE insp_rater_calibration_stats (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  profile_id BIGINT NOT NULL,
  inspector_id BIGINT NOT NULL,
  period_start DATE NOT NULL,
  period_end DATE NOT NULL,
  sample_count INT NOT NULL,
  mean_score DECIMAL(8,4) NOT NULL,
  std_dev DECIMAL(8,4) NOT NULL,
  skewness DECIMAL(8,4) DEFAULT NULL,
  calibration_offset DECIMAL(8,4) DEFAULT NULL COMMENT '校准偏移量',
  calibration_scale DECIMAL(8,4) DEFAULT NULL COMMENT '校准缩放因子',
  calculated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  UNIQUE KEY uk_inspector_period (profile_id, inspector_id, period_start, period_end)
) COMMENT='评审员校准统计';
```

**后端**:
- 定时任务: 每日计算各检查员的统计特征(均值、标准差、偏度)
- Z_SCORE校准: calibrated = (raw - inspector_mean) / inspector_stddev × global_stddev + global_mean
- `RaterCalibrationJob.java` 定时计算
- `ScoringProfileApplicationService` 增加手动触发校准 API

**前端**:
- `ScoringProfileEditor.vue` 增加"分布校准"卡片
- 新增 `components/CalibrationDashboard.vue` 显示各检查员的统计分布

---

### 第二阶段: 模板能力增强 (P1 - 模板复用与灵活性)

#### 2.1 检查项库 (Item Library)

**目标**: 全局共享的检查项库，模板可从库中引用项目，修改库项目可选择性同步到引用模板。

**数据库**:
```sql
-- V46.0.0__item_library.sql
CREATE TABLE insp_library_items (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  item_code VARCHAR(50) NOT NULL,
  item_name VARCHAR(200) NOT NULL,
  description TEXT,
  item_type VARCHAR(30) NOT NULL,
  category VARCHAR(100) COMMENT '库内分类',
  tags VARCHAR(500) COMMENT '标签(逗号分隔)',
  default_config JSON COMMENT '默认配置',
  default_validation_rules JSON,
  default_scoring_config JSON,
  default_help_content TEXT,
  usage_count INT DEFAULT 0 COMMENT '引用次数',
  is_standard TINYINT DEFAULT 0 COMMENT '是否标准项(不可删除)',
  created_by BIGINT,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  UNIQUE KEY uk_code (tenant_id, item_code)
) COMMENT='检查项库';

ALTER TABLE insp_template_items ADD COLUMN library_item_id BIGINT DEFAULT NULL COMMENT '来源库项目ID';
ALTER TABLE insp_template_items ADD COLUMN sync_with_library TINYINT DEFAULT 0 COMMENT '是否与库同步';
```

**后端**:
- 新建 `domain/inspection/model/v7/template/LibraryItem.java`
- 新建 `LibraryItemRepository` + 基础设施实现
- 新建 `LibraryItemApplicationService.java` CRUD + 搜索 + 同步推送
- `TemplateItemApplicationService.java` 增加 `createFromLibrary(sectionId, libraryItemId)` 方法

**前端**:
- 新建 `views/inspection/v7/templates/ItemLibraryView.vue` 列表页
- `ItemEditor.vue` 增加"从库中选择"按钮 → 弹出库搜索对话框
- 库项目编辑器支持标签/分类/搜索/排序

**API**:
```
GET    /api/v7/library-items?category=&keyword=&page=&size=
POST   /api/v7/library-items
PUT    /api/v7/library-items/{id}
DELETE /api/v7/library-items/{id}
POST   /api/v7/library-items/{id}/sync  (推送更新到所有引用)
```

---

#### 2.2 模板组合 (Template Composition)

**目标**: 将模板拆分为可独立管理的"模块"，大模板通过组合多个模块构建。

**数据库**:
```sql
-- V47.0.0__template_composition.sql
ALTER TABLE insp_templates ADD COLUMN is_module TINYINT DEFAULT 0 COMMENT '是否为模块(可被其他模板引用)';
ALTER TABLE insp_templates ADD COLUMN is_composite TINYINT DEFAULT 0 COMMENT '是否为组合模板';

CREATE TABLE insp_template_module_refs (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  composite_template_id BIGINT NOT NULL COMMENT '组合模板ID',
  module_template_id BIGINT NOT NULL COMMENT '被引用的模块模板ID',
  sort_order INT DEFAULT 0,
  override_config JSON COMMENT '覆盖配置(可选)',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  INDEX idx_composite (composite_template_id),
  INDEX idx_module (module_template_id)
) COMMENT='模板模块引用关系';
```

**后端**:
- `InspTemplate.java` 增加 `isModule`, `isComposite` 字段
- 新建 `TemplateModuleRef.java` 实体
- 组合模板发布时: 展开所有模块为完整的sections+items快照

**前端**:
- `TemplateEditorView.vue` 增加"插入模块"操作
- 模块引用在 SectionTree 中以不同颜色标记
- `TemplateListView.vue` 增加 is_module 筛选标签

---

#### 2.3 动态检查项 (Dynamic Items)

**目标**: 基于前面的回答动态显示/隐藏后续项目。已有条件逻辑V2基础，但仅控制"是否必填"，需要扩展为控制"是否显示"和"是否计分"。

**数据库**:
```sql
-- V47.0.0 (同批)
ALTER TABLE insp_template_items ADD COLUMN visibility_logic JSON DEFAULT NULL COMMENT '显示条件(条件逻辑V2格式)';
ALTER TABLE insp_template_items ADD COLUMN scoring_logic JSON DEFAULT NULL COMMENT '计分条件(条件逻辑V2格式)';
```

**后端**:
- `TemplateItem.java` 增加 `visibilityLogic`, `scoringLogic` 字段
- 打分时: 条件逻辑引擎评估 → 不满足 visibility 的项目不计入得分

**前端**:
- `ItemEditor.vue` 增加"显示条件"和"计分条件"配置区域
- 复用 `useConditionLogic` composable
- `InspectionForm.vue` 实时根据已回答项目评估后续项目的可见性
- `ScoringItemRow.vue` 动态隐藏/禁用不满足条件的项目

---

#### 2.4 合规映射 (Compliance Mapping)

**目标**: 检查项映射到外部标准/法规条款，自动生成合规报告。

**数据库**:
```sql
-- V48.0.0__compliance_mapping.sql
CREATE TABLE insp_compliance_standards (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  standard_code VARCHAR(50) NOT NULL COMMENT '标准编号',
  standard_name VARCHAR(200) NOT NULL COMMENT '标准名称',
  issuing_body VARCHAR(200) COMMENT '发布机构',
  effective_date DATE,
  version VARCHAR(50),
  description TEXT,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  UNIQUE KEY uk_code (tenant_id, standard_code)
) COMMENT='合规标准';

CREATE TABLE insp_compliance_clauses (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  standard_id BIGINT NOT NULL,
  clause_number VARCHAR(50) NOT NULL COMMENT '条款编号(如3.2.1)',
  clause_title VARCHAR(200) NOT NULL,
  clause_content TEXT,
  parent_clause_id BIGINT DEFAULT NULL COMMENT '上级条款',
  sort_order INT DEFAULT 0,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  INDEX idx_standard (standard_id)
) COMMENT='合规条款';

CREATE TABLE insp_item_compliance_mappings (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  template_item_id BIGINT NOT NULL,
  clause_id BIGINT NOT NULL,
  coverage_level VARCHAR(20) DEFAULT 'FULL' COMMENT 'FULL|PARTIAL|REFERENCE',
  notes VARCHAR(500),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  INDEX idx_item (template_item_id),
  INDEX idx_clause (clause_id)
) COMMENT='检查项-条款映射';
```

**后端**:
- 新建 `domain/inspection/model/v7/compliance/` 包: `ComplianceStandard`, `ComplianceClause`, `ItemComplianceMapping`
- 新建 Application Service + REST Controller
- `ReportGenerationService.java` 增加合规覆盖率报告

**前端**:
- 新建 `views/inspection/v7/compliance/` 目录
  - `ComplianceStandardListView.vue` 标准管理
  - `ComplianceMappingView.vue` 映射编辑器(矩阵视图: 行=条款，列=检查项)
  - `ComplianceReportView.vue` 合规覆盖率报告

**API**:
```
GET/POST/PUT/DELETE /api/v7/compliance-standards
GET/POST/PUT/DELETE /api/v7/compliance-standards/{id}/clauses
GET/POST/DELETE     /api/v7/template-items/{id}/compliance-mappings
GET                 /api/v7/compliance-reports?standardId=&templateId=
```

---

#### 2.5 模板市场 (Template Marketplace)

**目标**: 跨租户共享和发现模板，支持模板评分/评论/下载计数。

**数据库**:
```sql
-- V49.0.0__template_marketplace.sql
CREATE TABLE insp_marketplace_listings (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  template_id BIGINT NOT NULL,
  publisher_tenant_id BIGINT NOT NULL,
  title VARCHAR(200) NOT NULL,
  summary VARCHAR(500),
  cover_image_url VARCHAR(500),
  category VARCHAR(100),
  tags VARCHAR(500),
  is_free TINYINT DEFAULT 1,
  price DECIMAL(10,2) DEFAULT 0,
  download_count INT DEFAULT 0,
  avg_rating DECIMAL(3,2) DEFAULT 0,
  rating_count INT DEFAULT 0,
  status VARCHAR(20) DEFAULT 'PENDING' COMMENT 'PENDING|APPROVED|REJECTED|REMOVED',
  published_at DATETIME,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0
) COMMENT='模板市场上架';

CREATE TABLE insp_marketplace_reviews (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  listing_id BIGINT NOT NULL,
  reviewer_tenant_id BIGINT NOT NULL,
  reviewer_user_id BIGINT NOT NULL,
  rating INT NOT NULL COMMENT '1-5星',
  comment TEXT,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  INDEX idx_listing (listing_id)
) COMMENT='模板市场评价';

CREATE TABLE insp_marketplace_downloads (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  listing_id BIGINT NOT NULL,
  downloader_tenant_id BIGINT NOT NULL,
  downloaded_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_listing (listing_id)
) COMMENT='模板下载记录';
```

**优先级说明**: 此功能依赖多租户基础设施成熟度，可后期实现。

---

### 第三阶段: 执行体验提升 (P1 - 移动端提效)

#### 3.1 快捷评分 (Quick Scoring Shortcuts)

**目标**: 高频操作一键完成。如"全部合格"、"复制上次评分"、"应用预设"。

**数据库**:
```sql
-- V50.0.0__quick_scoring.sql
CREATE TABLE insp_scoring_presets (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  template_id BIGINT NOT NULL,
  preset_name VARCHAR(100) NOT NULL,
  preset_type VARCHAR(20) NOT NULL COMMENT 'FULL_PASS|FULL_FAIL|CUSTOM',
  item_values JSON NOT NULL COMMENT '[{itemId:1,value:"合格",score:10},...]',
  usage_count INT DEFAULT 0,
  created_by BIGINT,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  INDEX idx_template (template_id)
) COMMENT='评分预设';
```

**后端**:
- 新建 `ScoringPreset.java` + Repository + ApplicationService + Controller
- 预设应用: 批量填充 submission_details

**前端**:
- `InspectionForm.vue` 顶部增加预设快捷栏
  - "全部合格" (一键填充所有项目为满分)
  - "复制上次" (查询该目标最近一次提交的值)
  - 自定义预设列表
- `TaskExecutionMobileView.vue` 底部浮动快捷按钮

---

#### 3.2 协同检查 (Collaborative Inspection)

**目标**: 多个检查员同时对同一目标检查不同部分(分区分工)。

**数据库**:
```sql
-- V51.0.0__collaborative_inspection.sql
ALTER TABLE insp_tasks ADD COLUMN collaboration_mode VARCHAR(20) DEFAULT 'SINGLE'
  COMMENT 'SINGLE|SECTION_SPLIT|FULL_PARALLEL';

CREATE TABLE insp_task_section_assignments (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  task_id BIGINT NOT NULL,
  section_id BIGINT NOT NULL,
  inspector_id BIGINT NOT NULL,
  status VARCHAR(20) DEFAULT 'PENDING',
  started_at DATETIME,
  completed_at DATETIME,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  INDEX idx_task (task_id)
) COMMENT='任务分区分配';
```

**后端**:
- `InspTask.java` 增加 `collaborationMode` 字段
- 新建 `TaskSectionAssignment.java` 实体
- 提交时: 所有分区完成后合并为最终 submission

**前端**:
- `TaskExecutionView.vue` 协同模式下仅显示自己负责的分区
- 实时进度指示器: 显示其他检查员的完成情况
- 使用 WebSocket / SSE 推送进度更新

---

#### 3.3 AI 图片识别 (AI Photo Recognition)

**目标**: 拍照后 AI 自动识别问题类型并预填扣分项。

**技术方案**:
- 不内置模型，通过 Webhook 对接外部 AI 服务(OpenAI Vision / 百度AI / 阿里云视觉)
- 配置: `WebhookSubscription` 增加 `AI_RECOGNITION` 事件类型
- 前端: `EvidenceCapture.vue` 拍照后调用 webhook → 返回识别结果 → 预填建议

**数据库**:
```sql
-- V51.0.0 (同批)
ALTER TABLE insp_evidences ADD COLUMN ai_analysis JSON DEFAULT NULL COMMENT 'AI识别结果';
ALTER TABLE insp_evidences ADD COLUMN ai_confidence DECIMAL(4,2) DEFAULT NULL;
```

**实现要点**:
- 异步处理: 上传照片 → 返回占位 → Webhook回调写入 ai_analysis → 前端轮询/SSE 获取结果
- 结果仅作为建议，检查员确认后才生效

---

#### 3.4 语音转文字 (Voice-to-Text)

**目标**: 检查员可以语音输入备注和描述。

**技术方案**:
- 前端使用 Web Speech API (SpeechRecognition) — 纯浏览器端实现，无需后端
- 回退方案: 通过 Webhook 对接讯飞/百度语音识别

**前端**:
- `FormItemRenderer.vue` TEXT/TEXTAREA 类型增加麦克风按钮
- 新建 `composables/useSpeechRecognition.ts`
  - `startListening()` / `stopListening()` / `transcript` / `isListening`
  - 支持连续识别 + 标点自动插入
- 移动端优先: 仅在移动视图(`TaskExecutionMobileView.vue`)默认显示

---

#### 3.5 计时分析 (Timing Analytics)

**目标**: 追踪每个检查项/分区/任务的耗时，发现效率瓶颈。

**数据库**:
```sql
-- V52.0.0__timing_analytics.sql
ALTER TABLE insp_submission_details ADD COLUMN time_spent_seconds INT DEFAULT NULL COMMENT '该项耗时(秒)';
ALTER TABLE insp_submissions ADD COLUMN total_time_seconds INT DEFAULT NULL COMMENT '总耗时(秒)';
ALTER TABLE insp_tasks ADD COLUMN execution_started_at DATETIME DEFAULT NULL;
ALTER TABLE insp_tasks ADD COLUMN execution_ended_at DATETIME DEFAULT NULL;
```

**前端**:
- `InspectionForm.vue` 进入分区/项目时记录时间戳
- 提交时计算每项耗时，写入 submission_details
- 新建 `composables/useTimingTracker.ts`

**后端**:
- `AnalyticsProjectionService.java` 汇总平均耗时
- 新增 API: `GET /api/v7/analytics/timing?projectId=&dateFrom=&dateTo=`

**分析视图**:
- `AnalyticsDashboardView.vue` 增加"效率分析"标签页
  - 每项平均耗时柱状图
  - 检查员效率对比
  - 异常耗时(过快/过慢)标记

---

### 第四阶段: 分析看板增强 (P2 - 数据可视化)

#### 4.1 热力图 (Heatmap Visualization)

**目标**: 时间×目标 矩阵热力图，直观展示问题集中区域。

**前端**:
- 新建 `analytics/components/InspectionHeatmap.vue`
- 使用 ECharts heatmap 图表
- X轴: 日期, Y轴: 目标(宿舍/班级/区域), 颜色深浅: 得分
- 支持按维度筛选

**后端**:
- 新增 API: `GET /api/v7/analytics/heatmap?projectId=&dateFrom=&dateTo=&targetType=&dimensionId=`
- 从 `insp_daily_summaries` 查询并按日期×目标分组

---

#### 4.2 桑基图 (Sankey Diagram)

**目标**: 展示 问题来源 → 问题分类 → 纠正状态 的流向关系。

**前端**:
- 新建 `analytics/components/IssueSankeyChart.vue`
- ECharts sankey 图表
- 节点层: [来源维度] → [问题类别] → [处理状态(已关闭/进行中/超期)]

**后端**:
- 新增 API: `GET /api/v7/analytics/issue-flow?projectId=&dateFrom=&dateTo=`
- 联查 `insp_submission_details` + `insp_corrective_cases` 构建流向数据

---

#### 4.3 预警看板 (Alert Dashboard)

**目标**: 实时监控异常指标，自动告警。

**数据库**:
```sql
-- V53.0.0__alert_dashboard.sql
CREATE TABLE insp_alert_rules (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  rule_name VARCHAR(100) NOT NULL,
  metric_type VARCHAR(50) NOT NULL COMMENT 'SCORE_DROP|CONSECUTIVE_FAIL|HIGH_DEVIATION|LOW_COMPLIANCE|OVERDUE_CORRECTION',
  threshold_config JSON NOT NULL COMMENT '阈值配置',
  severity VARCHAR(20) NOT NULL DEFAULT 'WARNING' COMMENT 'INFO|WARNING|CRITICAL',
  notification_channels JSON COMMENT '通知渠道配置',
  is_enabled TINYINT DEFAULT 1,
  project_id BIGINT DEFAULT NULL COMMENT 'NULL=全局',
  created_by BIGINT,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0
) COMMENT='预警规则';

CREATE TABLE insp_alerts (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  alert_rule_id BIGINT NOT NULL,
  target_id BIGINT,
  target_type VARCHAR(30),
  target_name VARCHAR(200),
  metric_value DECIMAL(10,2) COMMENT '触发值',
  threshold_value DECIMAL(10,2) COMMENT '阈值',
  severity VARCHAR(20) NOT NULL,
  message TEXT NOT NULL,
  status VARCHAR(20) DEFAULT 'OPEN' COMMENT 'OPEN|ACKNOWLEDGED|RESOLVED|DISMISSED',
  acknowledged_by BIGINT DEFAULT NULL,
  acknowledged_at DATETIME DEFAULT NULL,
  resolved_at DATETIME DEFAULT NULL,
  triggered_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  INDEX idx_status (status),
  INDEX idx_triggered (triggered_at)
) COMMENT='预警记录';
```

**后端**:
- 新建 `domain/inspection/model/v7/analytics/AlertRule.java`, `Alert.java`
- 定时任务: 每小时扫描预警规则 → 评估指标 → 触发告警 → 发送通知
- 预置5种指标类型:
  - `SCORE_DROP`: 得分环比下降超过阈值
  - `CONSECUTIVE_FAIL`: 连续N次不合格
  - `HIGH_DEVIATION`: 检查员间偏差过大
  - `LOW_COMPLIANCE`: 合规覆盖率过低
  - `OVERDUE_CORRECTION`: 纠正措施超期

**前端**:
- 新建 `views/inspection/v7/analytics/AlertDashboardView.vue`
  - 告警列表(按严重程度排序)
  - 告警确认/忽略/解决操作
  - 告警趋势图
- `V7DashboardView.vue` 增加告警摘要卡片

---

#### 4.4 大屏展示 (Big Screen Display)

**目标**: 专为大屏幕/电视墙设计的展示界面，实时滚动数据。

**前端**:
- 新建 `views/inspection/v7/analytics/BigScreenView.vue`
  - 全屏深色主题
  - 6宫格布局: 今日总览 / 实时排名 / 趋势折线 / 热力图 / 告警滚动 / 纠正进度
  - 自动刷新(30秒间隔)
  - 数据翻页动画
- 路由: `/inspection/v7/big-screen` (无需主布局)

---

#### 4.5 同比/环比计算 (YoY/MoM Calculation)

**目标**: 在所有分析视图中提供同比(YoY)和环比(MoM)指标。

**数据库**:
```sql
-- V53.0.0 (同批)
ALTER TABLE insp_period_summaries ADD COLUMN prev_period_score DECIMAL(8,2) DEFAULT NULL COMMENT '上期分数';
ALTER TABLE insp_period_summaries ADD COLUMN mom_change DECIMAL(6,2) DEFAULT NULL COMMENT '环比变化(%)';
ALTER TABLE insp_period_summaries ADD COLUMN yoy_score DECIMAL(8,2) DEFAULT NULL COMMENT '去年同期分数';
ALTER TABLE insp_period_summaries ADD COLUMN yoy_change DECIMAL(6,2) DEFAULT NULL COMMENT '同比变化(%)';
```

**后端**:
- `AnalyticsProjectionService.java` 在 period summary 计算时同步计算环比/同比
- `AnalyticsQueryService.java` 查询接口返回同比/环比字段

**前端**:
- `ScoreTrendChart.vue` 增加同比/环比切换
- 所有数值卡片增加变化箭头(↑绿 ↓红)

---

### 第五阶段: 纠正措施增强 (P2)

#### 5.1 子任务分解 (Sub-task Decomposition)

**目标**: 将大型纠正案例分解为多个子任务，支持并行处理和进度追踪。

**数据库**:
```sql
-- V54.0.0__corrective_subtasks.sql
CREATE TABLE insp_corrective_subtasks (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  case_id BIGINT NOT NULL COMMENT '所属纠正案例',
  subtask_name VARCHAR(200) NOT NULL,
  description TEXT,
  assignee_id BIGINT NOT NULL,
  status VARCHAR(20) DEFAULT 'PENDING' COMMENT 'PENDING|IN_PROGRESS|COMPLETED|BLOCKED',
  priority INT DEFAULT 0,
  due_date DATE,
  completed_at DATETIME,
  sort_order INT DEFAULT 0,
  created_by BIGINT,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  INDEX idx_case (case_id)
) COMMENT='纠正案例子任务';
```

**后端**:
- 新建 `CorrectiveSubtask.java` 实体 (归属 CorrectiveCase 聚合)
- `CorrectiveCaseApplicationService.java` 增加子任务 CRUD
- Case 完成条件: 所有子任务 COMPLETED

**前端**:
- `CorrectiveCaseDetailView.vue` 增加"子任务"标签
- 子任务列表支持拖拽排序、状态切换、负责人分配

---

#### 5.2 知识库 (Knowledge Base)

**目标**: 积累常见问题和解决方案，辅助纠正措施制定。

**数据库**:
```sql
-- V55.0.0__knowledge_base.sql
CREATE TABLE insp_knowledge_articles (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  title VARCHAR(200) NOT NULL,
  content TEXT NOT NULL,
  category VARCHAR(100),
  tags VARCHAR(500),
  related_item_codes JSON COMMENT '关联检查项编码列表',
  source_case_id BIGINT DEFAULT NULL COMMENT '来源纠正案例',
  view_count INT DEFAULT 0,
  helpful_count INT DEFAULT 0,
  is_pinned TINYINT DEFAULT 0,
  created_by BIGINT,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  FULLTEXT KEY ft_search (title, content, tags)
) COMMENT='知识库文章';
```

**后端**:
- 新建 `domain/inspection/model/v7/knowledge/KnowledgeArticle.java`
- Application Service + REST Controller
- 纠正案例关闭时: 提示是否将解决方案沉淀为知识库文章

**前端**:
- 新建 `views/inspection/v7/knowledge/KnowledgeBaseView.vue` 列表+搜索
- 新建 `views/inspection/v7/knowledge/ArticleEditorView.vue` 富文本编辑
- `CorrectionForm.vue` 增加"推荐方案"面板 → 搜索知识库

---

### 第六阶段: 平台集成 (P3 - 生态扩展)

#### 6.1 NFC 打卡 (NFC Check-in)

**目标**: 通过 NFC 标签验证检查员到达指定地点。

**数据库**:
```sql
-- V56.0.0__nfc_checkin.sql
CREATE TABLE insp_nfc_tags (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  tag_uid VARCHAR(100) NOT NULL COMMENT 'NFC标签UID',
  location_name VARCHAR(200) NOT NULL,
  place_id BIGINT DEFAULT NULL COMMENT '关联场所',
  org_unit_id BIGINT DEFAULT NULL COMMENT '关联组织',
  is_active TINYINT DEFAULT 1,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  UNIQUE KEY uk_uid (tenant_id, tag_uid)
) COMMENT='NFC标签注册';

ALTER TABLE insp_submissions ADD COLUMN nfc_tag_uid VARCHAR(100) DEFAULT NULL;
ALTER TABLE insp_submissions ADD COLUMN checkin_time DATETIME DEFAULT NULL;
```

**前端** (移动端/小程序):
- 使用 Web NFC API (Android Chrome) 或微信小程序 NFC 能力
- `TaskExecutionMobileView.vue` 增加"NFC打卡"按钮
- 打卡后自动记录 tag_uid + 时间

---

#### 6.2 IM 平台集成 (DingTalk/WeCom/Feishu)

**目标**: 通过 Webhook + 机器人推送检查通知/告警到IM平台。

**技术方案**:
- 复用已有 `WebhookSubscription` 机制
- 新增 Webhook 模板: 钉钉/企微/飞书的消息格式适配器
- 消息类型: 任务分配通知、SLA预警、纠正措施提醒、日报摘要

**数据库**:
```sql
-- V56.0.0 (同批)
ALTER TABLE insp_webhook_subscriptions ADD COLUMN platform VARCHAR(30) DEFAULT 'GENERIC'
  COMMENT 'GENERIC|DINGTALK|WECOM|FEISHU|SLACK';
ALTER TABLE insp_webhook_subscriptions ADD COLUMN message_template TEXT DEFAULT NULL
  COMMENT '消息模板(支持变量替换)';
```

**后端**:
- 新建 `infrastructure/integration/` 包
  - `DingTalkMessageAdapter.java`
  - `WeComMessageAdapter.java`
  - `FeishuMessageAdapter.java`
- 每个适配器将通用事件转为平台特定的 Markdown/Card 消息格式

**前端**:
- `WebhookListView.vue` 增加平台选择器
- 平台选择后自动填充 URL 格式和消息模板

---

#### 6.3 IoT 传感器 (IoT Sensor Integration)

**目标**: 对接温湿度、空气质量等传感器，自动采集数据填充检查项。

**数据库**:
```sql
-- V57.0.0__iot_integration.sql
CREATE TABLE insp_iot_sensors (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  sensor_code VARCHAR(100) NOT NULL,
  sensor_name VARCHAR(200) NOT NULL,
  sensor_type VARCHAR(50) NOT NULL COMMENT 'TEMPERATURE|HUMIDITY|AIR_QUALITY|NOISE|LIGHT|SMOKE|WATER',
  location_name VARCHAR(200),
  place_id BIGINT DEFAULT NULL,
  mqtt_topic VARCHAR(200) COMMENT 'MQTT订阅主题',
  data_unit VARCHAR(20) COMMENT '数据单位(℃, %, dB等)',
  is_active TINYINT DEFAULT 1,
  last_reading DECIMAL(10,2) DEFAULT NULL,
  last_reading_at DATETIME DEFAULT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  UNIQUE KEY uk_code (tenant_id, sensor_code)
) COMMENT='IoT传感器注册';

CREATE TABLE insp_sensor_readings (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  sensor_id BIGINT NOT NULL,
  reading_value DECIMAL(10,2) NOT NULL,
  reading_unit VARCHAR(20),
  recorded_at DATETIME NOT NULL,
  INDEX idx_sensor_time (sensor_id, recorded_at)
) COMMENT='传感器读数(时序)';

CREATE TABLE insp_item_sensor_bindings (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  template_item_id BIGINT NOT NULL,
  sensor_id BIGINT NOT NULL,
  auto_fill TINYINT DEFAULT 1 COMMENT '是否自动填充',
  auto_score TINYINT DEFAULT 0 COMMENT '是否自动评分',
  scoring_thresholds JSON COMMENT '自动评分阈值配置',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  INDEX idx_item (template_item_id)
) COMMENT='检查项-传感器绑定';
```

**后端**:
- 可选 MQTT 客户端接收传感器数据
- REST API 接收传感器推送数据
- 打分时: 自动查询绑定传感器的最新读数 → 预填 + 自动评分

**优先级说明**: 依赖硬件部署，通常最后实施。

---

## 实施路线图

```
Phase 1 (评分引擎)     ██████████████████████████████████ 6-8 周
  ├─ 1.1 检查项权重                    1周
  ├─ 1.2 跨维度规则                    1周
  ├─ 1.3 重复违规递增                   1.5周
  ├─ 1.4 条件触发规则                   1周 (复用条件逻辑引擎)
  ├─ 1.5 规则有效期                    0.5周 (简单字段)
  ├─ 1.6 规则互斥                     0.5周 (简单字段)
  ├─ 1.7 评分配置版本化                  1周
  ├─ 1.8 基准对标/百分位                 1周
  ├─ 1.9 趋势因子                     0.5周
  ├─ 1.10 分数衰减                    0.5周
  ├─ 1.11 多评审员聚合                  1周
  └─ 1.12 分布校准                    1.5周

Phase 2 (模板增强)     ██████████████████████████ 5-6 周
  ├─ 2.1 检查项库                     2周
  ├─ 2.2 模板组合                     1.5周
  ├─ 2.3 动态检查项                    1周
  ├─ 2.4 合规映射                     2周
  └─ 2.5 模板市场                     2周 (可延后)

Phase 3 (执行体验)     ████████████████████████ 4-5 周
  ├─ 3.1 快捷评分                     1周
  ├─ 3.2 协同检查                     2周
  ├─ 3.3 AI图片识别                   1周 (Webhook对接)
  ├─ 3.4 语音转文字                    0.5周 (浏览器API)
  └─ 3.5 计时分析                     1周

Phase 4 (分析看板)     ████████████████████████ 4-5 周
  ├─ 4.1 热力图                      1周
  ├─ 4.2 桑基图                      1周
  ├─ 4.3 预警看板                     2周
  ├─ 4.4 大屏展示                     1.5周
  └─ 4.5 同比/环比                    0.5周

Phase 5 (纠正增强)     ██████████ 2 周
  ├─ 5.1 子任务分解                    1周
  └─ 5.2 知识库                      1.5周

Phase 6 (平台集成)     ████████████████ 3-4 周
  ├─ 6.1 NFC打卡                    1周
  ├─ 6.2 IM集成                     1.5周
  └─ 6.3 IoT传感器                   2周 (可延后)
```

**总计**: ~25-30 周 (约 6-7 个月)

---

## 数据库迁移文件规划

| 版本号 | 文件名 | 内容 |
|-------|-------|------|
| V41.0.0 | item_weight_and_cross_dim.sql | 检查项权重 + 跨维度规则字段 |
| V42.0.0 | escalation_and_conditional_rules.sql | 递增策略表 + 条件触发/有效期/互斥字段 |
| V43.0.0 | scoring_profile_versioning.sql | 评分配置版本表 |
| V44.0.0 | benchmarking_and_advanced_scoring.sql | 百分位字段 + 趋势/衰减字段 |
| V45.0.0 | multi_rater_calibration.sql | 多评审聚合字段 + 校准统计表 |
| V46.0.0 | item_library.sql | 检查项库表 + template_items关联字段 |
| V47.0.0 | template_composition_dynamic.sql | 模块引用表 + 动态项字段 |
| V48.0.0 | compliance_mapping.sql | 合规标准/条款/映射3张表 |
| V49.0.0 | template_marketplace.sql | 市场上架/评价/下载3张表 |
| V50.0.0 | quick_scoring.sql | 评分预设表 |
| V51.0.0 | collaborative_inspection.sql | 协同检查字段 + 分区分配表 + AI字段 |
| V52.0.0 | timing_analytics.sql | 计时字段 |
| V53.0.0 | alert_dashboard_yoy.sql | 预警规则/记录表 + 同比环比字段 |
| V54.0.0 | corrective_subtasks.sql | 子任务表 |
| V55.0.0 | knowledge_base.sql | 知识库表 |
| V56.0.0 | nfc_im_integration.sql | NFC标签表 + Webhook平台字段 |
| V57.0.0 | iot_integration.sql | 传感器/读数/绑定3张表 |

---

## 新增文件清单

### 后端 (预计 ~60 个新文件)

**Domain 层 (~15)**:
- `scoring/EscalationPolicy.java`
- `scoring/ScoringProfileVersion.java`
- `scoring/ScoringPreset.java`
- `scoring/RaterCalibrationStats.java`
- `template/LibraryItem.java`
- `template/TemplateModuleRef.java`
- `compliance/ComplianceStandard.java`
- `compliance/ComplianceClause.java`
- `compliance/ItemComplianceMapping.java`
- `execution/TaskSectionAssignment.java`
- `corrective/CorrectiveSubtask.java`
- `analytics/AlertRule.java`
- `analytics/Alert.java`
- `knowledge/KnowledgeArticle.java`
- `platform/NfcTag.java`
- `platform/IoTSensor.java`

**Repository 接口 (~15)**: 每个 Domain 实体对应

**Infrastructure 持久化 (~30)**: Mapper + PO + RepositoryImpl × 15

**Application Service (~10)**:
- `EscalationPolicyApplicationService.java`
- `LibraryItemApplicationService.java`
- `ComplianceApplicationService.java`
- `ScoringPresetApplicationService.java`
- `AlertApplicationService.java`
- `KnowledgeArticleApplicationService.java`
- `NfcTagApplicationService.java`
- `IoTSensorApplicationService.java`
- `RaterCalibrationJob.java`
- `AlertEvaluationJob.java`

**REST Controller (~8)**:
- `EscalationPolicyController.java`
- `LibraryItemController.java`
- `ComplianceController.java`
- `ScoringPresetController.java`
- `AlertController.java`
- `KnowledgeArticleController.java`
- `NfcTagController.java`
- `IoTSensorController.java`

### 前端 (预计 ~25 个新文件)

**Views**:
- `scoring/components/EscalationPolicyEditor.vue`
- `scoring/components/CalibrationDashboard.vue`
- `templates/ItemLibraryView.vue`
- `templates/components/LibraryItemPicker.vue`
- `compliance/ComplianceStandardListView.vue`
- `compliance/ComplianceMappingView.vue`
- `compliance/ComplianceReportView.vue`
- `analytics/AlertDashboardView.vue`
- `analytics/BigScreenView.vue`
- `analytics/components/InspectionHeatmap.vue`
- `analytics/components/IssueSankeyChart.vue`
- `analytics/components/YoYIndicator.vue`
- `knowledge/KnowledgeBaseView.vue`
- `knowledge/ArticleEditorView.vue`

**Composables**:
- `useSpeechRecognition.ts`
- `useTimingTracker.ts`
- `useNfcReader.ts`

**API**:
- `api/insp/escalationPolicy.ts`
- `api/insp/libraryItem.ts`
- `api/insp/compliance.ts`
- `api/insp/scoringPreset.ts`
- `api/insp/alert.ts`
- `api/insp/knowledgeArticle.ts`

**Types**:
- `types/insp/compliance.ts`
- `types/insp/alert.ts`
- `types/insp/knowledge.ts`

**Stores**:
- `stores/insp/inspAlertStore.ts`

---

## 推荐实施优先级

如果资源有限，建议按以下优先级精简实施:

### MVP (最小可行增强 - 4周)
1. **1.1 检查项权重** — 评分精度立即提升
2. **1.4 条件触发规则** — 复用现有引擎，投入产出比高
3. **1.5 规则有效期** — 极简实现，半天工作量
4. **1.6 规则互斥** — 极简实现
5. **2.3 动态检查项** — 智能表单核心能力
6. **3.1 快捷评分** — 移动端效率翻倍
7. **3.5 计时分析** — 零成本数据采集
8. **4.5 同比/环比** — 分析视图立刻有深度

### 进阶 (再投入 6周)
9. **1.3 重复违规递增**
10. **1.7 评分配置版本化**
11. **2.1 检查项库**
12. **4.1 热力图**
13. **4.3 预警看板**
14. **5.1 子任务分解**

### 高级 (按需)
15-32. 其余功能按业务需求优先级排序
