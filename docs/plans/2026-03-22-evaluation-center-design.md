# 评级中心（Evaluation Center）完整设计

## 定位

独立一级模块，不依附于检查平台。通用多条件评选引擎，支持跨系统数据源。

## 核心概念

```
评选活动 (EvaluationCampaign)
  "星级宿舍评选" — 长期运行，按周期自动/手动执行

级别定义 (EvaluationLevel)
  五星/四星/三星 — 从高到低逐级判定，首个满足即为结果

条件 (EvaluationCondition)
  每个级别 N 个条件，AND/OR 逻辑组合
  每个条件绑定一个数据源，有具体的指标+阈值

执行批次 (EvaluationBatch)
  每次执行产生一个批次，包含所有目标的结果

批次结果 (EvaluationResult)
  每个目标: 级别 + 排名 + 各条件达标明细 + 升级提示
```

## 数据库设计

```sql
-- 评选活动
CREATE TABLE eval_campaigns (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  campaign_name VARCHAR(100) NOT NULL,
  campaign_description TEXT NULL,
  target_type VARCHAR(20) NOT NULL COMMENT 'ORG/PLACE/USER',
  scope_org_ids TEXT NULL COMMENT 'JSON: 范围组织ID列表',
  evaluation_period VARCHAR(20) DEFAULT 'MONTHLY',
  status VARCHAR(20) DEFAULT 'DRAFT' COMMENT 'DRAFT/ACTIVE/PAUSED/ARCHIVED',
  is_auto_execute TINYINT DEFAULT 0 COMMENT '是否按周期自动执行',
  last_executed_at DATETIME NULL,
  next_execute_at DATETIME NULL,
  sort_order INT DEFAULT 0,
  created_by BIGINT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT NULL,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  INDEX idx_tenant (tenant_id),
  INDEX idx_status (status)
);

-- 评选级别
CREATE TABLE eval_levels (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  campaign_id BIGINT NOT NULL,
  level_num INT NOT NULL COMMENT '1=最高',
  level_name VARCHAR(50) NOT NULL,
  condition_logic VARCHAR(10) DEFAULT 'AND',
  sort_order INT DEFAULT 0,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_campaign (campaign_id),
  UNIQUE KEY uk_campaign_level (campaign_id, level_num)
);

-- 评选条件
CREATE TABLE eval_conditions (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  level_id BIGINT NOT NULL,
  source_type VARCHAR(20) NOT NULL COMMENT 'INSPECTION/EVENT/HISTORY',
  source_config TEXT NOT NULL COMMENT 'JSON: 数据源参数',
  metric VARCHAR(30) NOT NULL COMMENT '指标类型',
  operator VARCHAR(10) NOT NULL COMMENT '>=/<=/=/!=/IN',
  threshold TEXT NOT NULL COMMENT '阈值（数字或JSON数组）',
  scope VARCHAR(20) DEFAULT 'SELF' COMMENT 'SELF/MEMBERS/SPECIFIC_ROLE',
  scope_role VARCHAR(50) NULL COMMENT 'SPECIFIC_ROLE时的角色编码',
  time_range VARCHAR(20) DEFAULT 'CYCLE' COMMENT 'CYCLE/CUSTOM',
  time_range_days INT NULL COMMENT 'CUSTOM时的天数',
  description VARCHAR(200) NULL COMMENT '自然语言描述（自动生成）',
  sort_order INT DEFAULT 0,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_level (level_id)
);

-- 执行批次
CREATE TABLE eval_batches (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  campaign_id BIGINT NOT NULL,
  cycle_start DATE NOT NULL,
  cycle_end DATE NOT NULL,
  total_targets INT DEFAULT 0,
  executed_at DATETIME NOT NULL,
  executed_by BIGINT NULL,
  status VARCHAR(20) DEFAULT 'COMPLETED',
  summary TEXT NULL COMMENT 'JSON: {level1Count:3, level2Count:12, ...}',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_campaign (campaign_id, cycle_start)
);

-- 批次结果
CREATE TABLE eval_results (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  batch_id BIGINT NOT NULL,
  campaign_id BIGINT NOT NULL,
  target_type VARCHAR(20) NOT NULL,
  target_id BIGINT NOT NULL,
  target_name VARCHAR(100) NULL,
  level_num INT NULL COMMENT 'NULL=未达任何级别',
  level_name VARCHAR(50) NULL,
  rank_no INT NULL,
  score DECIMAL(10,2) NULL COMMENT '综合分（用于排名）',
  condition_details TEXT NULL COMMENT 'JSON: [{conditionId, passed, actual, threshold, description}]',
  upgrade_hint TEXT NULL COMMENT '升级提示文本',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_batch (batch_id),
  INDEX idx_campaign_target (campaign_id, target_type, target_id),
  INDEX idx_rank (batch_id, rank_no)
);
```

## 条件数据源详细定义

### INSPECTION（检查分数）

source_config:
```json
{
  "projectId": 51,        // 可选，不填=所有项目
  "sectionId": 100057     // 必填，指定分区
}
```

可用指标:
| metric | 说明 | operator | threshold |
|--------|------|----------|-----------|
| SCORE_AVG | 平均分 | >= <= | 数字 |
| SCORE_MIN | 最低分 | >= | 数字 |
| SCORE_MAX | 最高分 | <= | 数字 |
| SCORE_EVERY | 每次都达标 | >= | 数字 |
| GRADE_EVERY | 每次评级达标 | IN | ["优秀","良好"] |
| GRADE_COUNT | 达标次数 | >= | 数字 |
| FAIL_COUNT | 不通过次数 | = <= | 数字 |

### EVENT（事件记录）

source_config:
```json
{
  "eventType": "INSP_VIOLATION"  // 必填，事件类型编码
}
```

可用指标:
| metric | 说明 | operator | threshold |
|--------|------|----------|-----------|
| COUNT | 事件次数 | = >= <= | 数字 |
| SCORE_SUM | 事件分值总和 | >= <= | 数字 |

### HISTORY（评选历史）

source_config:
```json
{
  "campaignId": null  // 可选，null=本活动
}
```

可用指标:
| metric | 说明 | operator | threshold |
|--------|------|----------|-----------|
| PREV_LEVEL | 上期级别 | >= <= | 级别序号 |
| CONSECUTIVE | 连续达标期数 | >= | 数字 |
| RANK_PERCENTILE | 排名百分位 | <= | 百分比 |
| TREND | 趋势（分差） | >= | 数字 |

## API 设计

```
# 评选活动 CRUD
GET    /eval/campaigns                    列表（支持 status 筛选）
POST   /eval/campaigns                    创建
GET    /eval/campaigns/:id                详情（含级别+条件）
PUT    /eval/campaigns/:id                更新基本信息
DELETE /eval/campaigns/:id                删除

# 级别管理（批量保存）
GET    /eval/campaigns/:id/levels         获取级别列表（含条件）
PUT    /eval/campaigns/:id/levels         批量保存级别+条件

# 执行
POST   /eval/campaigns/:id/execute        执行评选（传 cycleStart/cycleEnd）
GET    /eval/campaigns/:id/batches        执行历史

# 结果
GET    /eval/batches/:batchId/results     批次结果列表（含条件明细）
GET    /eval/results/target/:type/:id     某目标的评选历史

# 辅助（条件编辑器需要的选项数据）
GET    /eval/options/projects             可选的检查项目列表
GET    /eval/options/sections/:projectId  项目下的分区列表
GET    /eval/options/grade-bands/:sectionId  分区的等级映射
GET    /eval/options/event-types          事件类型列表
```

## 后端架构

```
domain/evaluation/
├── model/
│   ├── EvalCampaign.java         聚合根
│   ├── EvalLevel.java            级别实体
│   ├── EvalCondition.java        条件值对象
│   ├── EvalBatch.java            批次实体
│   └── EvalResult.java           结果实体
├── repository/
│   ├── EvalCampaignRepository.java
│   ├── EvalBatchRepository.java
│   └── EvalResultRepository.java
└── engine/
    ├── ConditionEvaluator.java        接口
    ├── InspectionEvaluator.java       检查分数评估器
    ├── EventEvaluator.java            事件记录评估器
    ├── HistoryEvaluator.java          评选历史评估器
    └── EvaluationEngine.java          引擎（调度所有评估器）

application/evaluation/
├── EvalCampaignApplicationService.java   活动CRUD+执行
└── EvalConditionOptionsService.java      条件编辑器选项数据

interfaces/rest/evaluation/
├── EvalCampaignController.java
└── EvalResultController.java

infrastructure/persistence/evaluation/
├── EvalCampaignPO/Mapper/RepositoryImpl
├── EvalLevelPO/Mapper (嵌入Campaign查询)
├── EvalConditionPO/Mapper (嵌入Level查询)
├── EvalBatchPO/Mapper/RepositoryImpl
└── EvalResultPO/Mapper/RepositoryImpl
```

评估引擎流程:
```
execute(campaignId, cycleStart, cycleEnd):
  1. 加载活动配置 + 所有级别 + 所有条件
  2. 解析范围 → 找到所有目标（scopeOrgIds + targetType）
  3. 对每个目标:
     a. 从最高级别开始
     b. 评估该级别的所有条件
        - 根据 sourceType 分发到对应 Evaluator
        - Evaluator 查询数据并返回 {passed, actualValue}
     c. 根据 conditionLogic (AND/OR) 组合判定
     d. 首个满足的级别即为结果
     e. 记录每个条件的达标明细
     f. 生成升级提示（分析第一个未达标级别的差距）
  4. 按综合分排名
  5. 保存批次 + 结果
  6. 发布荣誉事件到实体事件系统
```

## 前端路由

```
/evaluation                          评级中心首页（活动列表）
/evaluation/campaigns/create         创建评选活动
/evaluation/campaigns/:id            编辑活动（两栏：左配置 右预览）
/evaluation/batches/:batchId         评选结果详情页
```

侧边栏：与检查平台平级的一级菜单「评级中心」

## 迁移策略

1. 新建 eval_* 表，不动旧的 insp_evaluation_* 表
2. 前端新建 /evaluation 路由和页面
3. 检查平台项目详情的"评级规则"Tab 改为"关联评选"入口（跳转到评级中心）
4. 旧的 EvaluationRule/RatingDimension 代码保留不删，逐步废弃
5. 数据迁移：将现有 insp_evaluation_rules 转换为 eval_campaigns（手动或脚本）

## 设计原则

- 评级中心不依赖检查平台的任何代码，只通过 Repository 查询 InspSubmission 数据
- 条件评估器用适配器模式，新增数据源只需加一个 Evaluator 实现类
- 条件描述自动生成自然语言（"宿舍检查 每次评级都达到 优秀或良好"）
- 结果详情包含每个条件的达标明细和升级提示
