# 量化检查系统 V4.0 全面重构方案

> **文档版本**: 1.0
> **创建日期**: 2026-01-27
> **技术栈**: Spring Boot 3.2 + Vue 3 + TypeScript + Element Plus + Tailwind CSS
> **适用规模**: 3个系部30个班级 ~ 15+系部200+班级（弹性架构）

---

## 目录

1. [项目概述与重构目标](#1-项目概述与重构目标)
2. [业界大厂方案深度分析](#2-业界大厂方案深度分析)
3. [核心设计理念与推翻清单](#3-核心设计理念与推翻清单)
4. [系统架构设计](#4-系统架构设计)
5. [限界上下文与领域模型](#5-限界上下文与领域模型)
6. [数据库设计](#6-数据库设计)
7. [API接口设计](#7-api接口设计)
8. [前端架构设计](#8-前端架构设计)
9. [前端UI页面详细设计](#9-前端ui页面详细设计)
10. [实施路线图](#10-实施路线图)

---

## 1. 项目概述与重构目标

### 1.1 业务背景

学校量化检查系统是教育信息化的核心模块，承载着对班级日常管理质量的客观评价。现有系统（V3）已实现：
- 自定义扣分项与三种扣分模式（固定/按人/区间）
- 权重系统V5.0（方案库模式，四种权重算法）
- 自定义评级标题与三种划分方式
- 荣誉徽章自动授予
- 7状态申诉流程
- 混合宿舍公平分摊

### 1.2 重构目标

| 目标 | 说明 |
|------|------|
| **检查范式升级** | 从"自由扣分录入"升级为"逐项核验+自由扣分"双模式 |
| **物理空间优先** | 检查员按物理动线录入，系统自动解析组织归属 |
| **多级检查体系** | 支持系部→班级、校级→系部两级独立检查 |
| **闭环改善** | 发现问题→创建整改→跟踪验证→确认关闭 |
| **学生行为独立** | 违纪行为作为独立领域，支持画像与预警 |
| **正向激励引入** | 在扣分体系基础上增加加分维度（可配置开关） |
| **自动调度** | 周期性+随机+触发式三类自动排检 |
| **班主任工作台** | 专属视角看班级量化、问题、学生、趋势 |
| **架构升级** | 事件驱动、CQRS读写分离、聚合拆分 |

### 1.3 适用角色

| 角色 | 核心场景 |
|------|----------|
| **系统管理员** | 配置模板、权重方案、评级规则、调度策略 |
| **检查计划管理员** | 创建/管理检查计划，分配检查员 |
| **检查员** | 现场执行检查，录入扣分/加分，拍照取证 |
| **系部管理员** | 查看本系部班级排名、评级、趋势 |
| **班主任** | 查看本班量化、问题明细、学生违纪、申诉 |
| **校级领导** | 查看全校数据、系部排名、分析报告 |

---

## 2. 业界大厂方案深度分析

### 2.1 SafetyCulture (iAuditor) — 全球第一检查平台

**用户**: 丰田、麦当劳、壳牌、希尔顿等28000+企业

**核心设计理念**:

**A. 逐项核验模式 (Item-by-Item Verification)**
```
传统模式: 检查员发现问题 → 记录扣分 → 没记录的默认无问题
iAuditor: 检查员逐项回答 → Pass/Fail/N/A → 每项都有明确状态
```
- 每个检查项必须给出响应，不允许跳过
- 未回答的项目显示为"未完成"，检查无法提交
- 评分自动计算：Score = Passed / (Total - N/A) × 100%
- **核心优势**: 完整的覆盖审计，知道"什么被检查了"而非猜测

**B. 问题→行动闭环 (Issue → Action Pipeline)**
```
发现Fail → 自动创建Action → 分配责任人 → 设置截止日期
→ 责任人整改 → 上传整改证据 → 检查员复核验证 → 关闭
```
- 不只是打分，更是发现问题并推动解决
- Action有独立的状态流：Open → In Progress → Review → Closed
- 逾期未处理自动升级（通知上级）

**C. 证据优先 (Evidence-First)**
- 照片是核心证据，不是可选附件
- Fail项必须附照片（可配置）
- 支持拍照标注（在照片上画圈/箭头指出问题）
- GPS定位自动记录检查位置

**D. 分析仪表盘**
- 按站点/区域/团队多维度对比
- 常见问题TOP10
- 改善趋势追踪
- 检查完成率监控

### 2.2 钉钉智能巡检 — 阿里企业巡检方案

**核心设计**:

**A. 巡检点扫码/NFC**
- 每个巡检点（宿舍门口/教室门口）贴二维码或NFC标签
- 检查员到达后扫码确认"已到达此位置"
- 系统记录到达时间+GPS坐标
- **防作弊**: 不到现场无法录入该点的检查结果

**B. 路线规划**
- 系统根据巡检点位置自动规划最优路线
- 显示已检查/未检查的点位地图
- 预估剩余检查时间

**C. 自动派发**
- 根据排班表自动生成每日巡检任务
- 支持轮换、替班、请假
- 工作量均衡分配

**D. 异常升级**
- 严重问题即时推送给管理层
- 超时未检查自动提醒
- 问题整改超期自动升级

### 2.3 ClassDojo — 教育行为管理标杆

**用户**: 全球95%的K-8学校，1.5亿用户

**核心设计**:

**A. 实时双向反馈**
```
正向: +勤奋 +参与 +团队合作 +坚持 +创造力  (自定义)
负向: -违纪 -迟到 -未完成作业 -不尊重      (自定义)
```
- 一键式操作：点击学生头像 → 选择行为 → 完成
- 全班视图和个人视图随时切换
- 实时音效反馈（叮=加分，嗡=扣分）

**B. 可视化身份**
- 每个学生有可爱的怪物头像
- 积分可视化为进度条
- 排行榜以友好方式展示（不公开分数，只显示等级）

**C. 家校沟通**
- 自动生成学生行为周报
- 家长APP实时查看孩子表现
- 班级故事分享（正向展示）

**D. 数据驱动**
- 行为趋势图（个人/班级/全校）
- 高频问题分析
- 干预效果评估

### 2.4 PBIS Rewards — 正向行为支持系统

**核心理念**: 5:1正负比（每给1次负面反馈应搭配5次正面反馈）

**三级干预模型**:
```
Tier 1（全体 80%学生）: 通用正向行为教育，积分激励
Tier 2（目标 15%学生）: 针对性行为干预小组，行为契约
Tier 3（个别  5%学生）: 个别化行为支持计划，多专业协作
```

**积分经济**:
- 学生赚取行为积分（正向行为）
- 积分可在"学校商店"兑换奖励（特权/物品）
- 创造内在动机而非外在惩罚

### 2.5 综合对比与启示

| 维度 | SafetyCulture | 钉钉巡检 | ClassDojo | PBIS | **V4方案** |
|------|--------------|----------|-----------|------|-----------|
| 检查模式 | 逐项核验 | 巡检点扫码 | 即时行为记录 | 行为观察 | **逐项核验+自由扣分双模** |
| 评分方式 | 通过率% | 自定义评分 | 正负积分 | 正向积分 | **基准分+扣分+加分可配置** |
| 物理定位 | GPS | 二维码/NFC | 教室选择 | 无 | **物理空间优先+楼层导航** |
| 问题闭环 | Action管道 | 工单系统 | 无 | 干预计划 | **整改工单+验证闭环** |
| 移动体验 | 离线优先 | 钉钉集成 | 移动APP | Web为主 | **Web优先+未来小程序** |
| 分析能力 | 多维对比 | 基础统计 | 趋势分析 | 三级数据 | **多维分析+预警+归因** |
| 正向激励 | 无 | 无 | 核心 | 核心 | **可配置加分维度** |

**V4核心创新**: 将工业巡检的专业性（SafetyCulture）、企业巡检的自动化（钉钉）、教育场景的正向激励（ClassDojo/PBIS）、以及现有系统的权重/评级/申诉精细化设计，融合为一个统一平台。

---

## 3. 核心设计理念与推翻清单

### 3.1 推翻总览

| # | 推翻内容 | 现有方案 | V4方案 | 参考来源 |
|---|----------|----------|--------|----------|
| 1 | 检查范式 | 自由扣分录入 | 逐项核验+自由扣分双模 | SafetyCulture |
| 2 | 评分模型 | 纯扣分累加(0起) | 基准分+扣分+加分(可配置) | ClassDojo/PBIS |
| 3 | 数据模型 | 双层数据复制 | 事件驱动+CQRS投影 | 业界DDD实践 |
| 4 | 申诉重算 | 同步+Redis锁 | 事件驱动Saga | 阿里对账系统 |
| 5 | 聚合设计 | DailyCheck巨型聚合 | Session+ClassRecord双聚合 | DDD最佳实践 |
| 6 | 权重计算 | 生成时固化 | 快照+实时双模式 | 华为绩效系统 |
| 7 | 检查调度 | 手动创建 | 策略化自动排检引擎 | 钉钉智能巡检 |
| 8 | 学生违纪 | 文本字段附加 | 独立Behavior上下文 | PBIS三级模型 |
| 9 | 检查层级 | 扁平(检查员→班级) | 三级(班级/系部/专项) | 组织管理实践 |
| 10 | 录入范式 | 组织优先(选班级→看宿舍) | 物理空间优先(选楼层→看房间) | 钉钉/SafetyCulture |
| 11 | 问题处理 | 记录扣分即结束 | 发现→整改→验证→关闭闭环 | SafetyCulture Action |

### 3.2 推翻#1 详解: 检查范式升级

**逐项核验模式 (Checklist Mode)**:
```
适用场景: 宿舍卫生、教室卫生等周期性例行检查
工作方式:
  检查员打开检查清单 → 每个宿舍/教室逐项确认:
    □ 地面清洁 → ✅通过 / ❌不通过(-2分) / ➖不适用
    □ 被褥整齐 → ✅通过 / ❌不通过(-1分) / ➖不适用
    □ 物品摆放 → ✅通过 / ❌不通过(-1分) / ➖不适用
    ...

  自动计算:
    扣分总和 = Σ(不通过项的扣分)
    通过率 = 通过数 / (总数 - 不适用数) × 100%

  核心价值:
    - 确保每项都被检查（不会遗漏）
    - 清楚知道哪些项被标记为"通过"（非假设无问题）
    - 支持通过率和扣分两种维度的分析
```

**自由扣分模式 (Quick Deduction Mode)**:
```
适用场景: 迟到检查、吸烟/违纪抓现行、随机抽查
工作方式:
  检查员发现问题 → 选扣分项 → 输入学生/位置 → 确认

  保留现有三种扣分模式:
    - FIXED_DEDUCT: 固定扣分
    - PER_PERSON_DEDUCT: 按人扣分
    - SCORE_RANGE: 区间扣分

  核心价值:
    - 快速录入，适合现场抓违纪
    - 灵活，不受检查清单限制
    - 兼容现有所有业务逻辑
```

**模式配置**:
- 每个检查类别(Category)可配置默认检查模式
- 宿舍卫生类 → 默认逐项核验模式
- 纪律检查类 → 默认自由扣分模式
- 检查员可在录入时手动切换模式
- 两种模式的扣分数据统一进入相同的评分/排名/评级管线

### 3.3 推翻#2 详解: 双轨积分制

```
可配置的评分公式:

模式A（纯扣分制 - 兼容现有）:
  最终得分 = Σ扣分  (越小越好)
  排名: 扣分升序

模式B（基准分制）:
  最终得分 = 基准分(可配置,默认100) - Σ扣分  (越大越好)
  排名: 得分降序

模式C（双轨制）:
  最终得分 = 基准分(100) - Σ扣分 + Σ加分  (越大越好)
  排名: 得分降序

加分项类型（仅模式C）:
  - BONUS_FIXED: 固定加分 (如: 主动整改+2)
  - BONUS_PROGRESSIVE: 递进加分 (连续N周无扣分, 1→2→3递增)
  - BONUS_IMPROVEMENT: 改善加分 (环比改善幅度×系数)

配置位置: 检查计划级别
  checkPlan.scoringMode = "DEDUCTION_ONLY" | "BASE_SCORE" | "DUAL_TRACK"
  checkPlan.baseScore = 100 (仅模式B/C)
```

### 3.4 推翻#10 详解: 物理空间优先录入

**三种录入入口**:

```
入口A: 物理空间入口 (宿舍/教室巡查)
  选楼栋 → 选楼层 → 看到该层所有房间号（按号排序）
  → 逐间检查 → 系统自动解析房间→班级映射

入口B: 人员入口 (迟到/违纪)
  搜索学生姓名/学号 → 系统自动解析学生→班级映射
  支持批量输入多个学生 → 自动按班级拆分为多条记录

入口C: 组织入口 (传统模式, 保留)
  选班级 → 看该班关联的宿舍/教室 → 录入扣分
  适合: 课堂纪律检查等明确知道班级的场景
```

### 3.5 推翻#11 详解: 整改闭环

```
问题发现 → 整改工单创建 → 责任人接收 → 执行整改
→ 上传整改证据 → 原检查员复核 → 确认关闭/驳回重做

CorrectiveAction（整改工单）:
  - actionId, sourceType(INSPECTION/APPEAL/MANUAL)
  - sourceRef: { sessionId, deductionId }
  - title: "301宿舍地面不洁"
  - severity: MINOR / MODERATE / SEVERE
  - assigneeId: 班主任或宿舍长
  - dueDate: 整改截止日期
  - status: OPEN → IN_PROGRESS → REVIEW → CLOSED / OVERDUE
  - evidence: 整改后照片
  - verifierId: 复核人
  - verifyResult: PASS / FAIL

自动创建规则:
  - 扣分≥阈值(可配置) → 自动创建整改工单
  - 严重扣分(SEVERE) → 整改工单+即时通知班主任
  - 连续同一问题 → 升级通知系部管理员
```

---

## 4. 系统架构设计

### 4.1 整体架构

```
┌─────────────────────────────────────────────────────────────┐
│                     前端层 (Frontend)                         │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐   │
│  │ 配置中心  │  │ 检查执行  │  │ 结果查看  │  │ 分析中心  │   │
│  │ 模板/权重 │  │ 录入/审核 │  │ 排名/评级 │  │ 仪表盘   │   │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘   │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐   │
│  │ 计划管理  │  │ 申诉管理  │  │ 班主任台  │  │ 行为管理  │   │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘   │
└─────────────────────────────────────────────────────────────┘
                           │ REST API
┌─────────────────────────────────────────────────────────────┐
│                   接口层 (Interface)                          │
│  /api/v2/inspection-*                                        │
│  Controller → Command/Query → Application Service            │
└─────────────────────────────────────────────────────────────┘
                           │
┌─────────────────────────────────────────────────────────────┐
│                  应用层 (Application)                         │
│  ┌────────────┐  ┌────────────┐  ┌────────────┐            │
│  │ Command处理 │  │ Query处理   │  │ Event处理   │            │
│  │ 写操作编排  │  │ 读操作查询  │  │ 事件响应    │            │
│  └────────────┘  └────────────┘  └────────────┘            │
└─────────────────────────────────────────────────────────────┘
                           │
┌─────────────────────────────────────────────────────────────┐
│                  领域层 (Domain)                              │
│  ┌─────────┐ ┌──────────┐ ┌────────┐ ┌──────────┐         │
│  │配置上下文│ │检查执行   │ │结果上下文│ │行为上下文 │         │
│  │Template │ │Session   │ │Record  │ │Behavior  │         │
│  │Category │ │Deduction │ │Ranking │ │Profile   │         │
│  │Weight   │ │Evidence  │ │Rating  │ │Alert     │         │
│  └─────────┘ └──────────┘ └────────┘ └──────────┘         │
│  ┌─────────┐ ┌──────────┐ ┌────────┐ ┌──────────┐         │
│  │计划上下文│ │申诉上下文 │ │整改上下文│ │调度上下文 │         │
│  │Plan     │ │Appeal    │ │Action  │ │Schedule  │         │
│  │Inspector│ │Approval  │ │Verify  │ │Policy    │         │
│  └─────────┘ └──────────┘ └────────┘ └──────────┘         │
└─────────────────────────────────────────────────────────────┘
                           │
┌─────────────────────────────────────────────────────────────┐
│                基础设施层 (Infrastructure)                     │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐      │
│  │MyBatis   │ │Redis     │ │事件总线   │ │文件存储   │      │
│  │持久化    │ │缓存/锁   │ │Spring    │ │照片/文档  │      │
│  └──────────┘ └──────────┘ │Events   │ └──────────┘      │
│                            └──────────┘                     │
└─────────────────────────────────────────────────────────────┘
```

### 4.2 限界上下文划分

```
┌─────────────────────────────────────────────────────────────┐
│                    Inspection Platform                        │
│                                                              │
│  ┌─────────────────┐        ┌─────────────────┐            │
│  │ Configuration    │───────→│ Planning         │            │
│  │ 配置上下文       │ 模板引用│ 计划上下文       │            │
│  │                 │        │                  │            │
│  │ • Template      │        │ • CheckPlan      │            │
│  │ • Category      │        │ • Inspector      │            │
│  │ • DeductionItem │        │ • SchedulePolicy │            │
│  │ • WeightConfig  │        │ • InspectorRotation │         │
│  │ • RatingRule    │        └────────┬─────────┘            │
│  └─────────────────┘                 │ 创建检查会话           │
│                                      ▼                      │
│  ┌─────────────────┐        ┌─────────────────┐            │
│  │ Corrective       │◄──────│ Execution        │            │
│  │ 整改上下文       │ 问题发现│ 检查执行上下文    │            │
│  │                 │        │                  │            │
│  │ • Action        │        │ • Session        │            │
│  │ • Verification  │        │ • ClassRecord    │            │
│  │ • Escalation    │        │ • Deduction      │            │
│  └─────────────────┘        │ • ChecklistResponse │         │
│                             │ • Evidence       │            │
│                             └────────┬─────────┘            │
│                                      │ 提交生成结果          │
│                                      ▼                      │
│  ┌─────────────────┐        ┌─────────────────┐            │
│  │ Appeal           │◄──────│ Result            │            │
│  │ 申诉上下文       │ 结果申诉│ 结果上下文        │            │
│  │                 │        │                  │            │
│  │ • Appeal        │        │ • RecordSnapshot │            │
│  │ • ApprovalFlow  │        │ • ClassStats     │            │
│  │ • Publicity     │        │ • Ranking        │            │
│  └─────────────────┘        │ • Rating         │            │
│                             │ • Badge          │            │
│         ┌───────────────────│ • Trend          │            │
│         │ 违纪关联          └─────────────────┘            │
│         ▼                                                   │
│  ┌─────────────────┐        ┌─────────────────┐            │
│  │ Behavior         │        │ Analytics        │            │
│  │ 学生行为上下文    │        │ 分析上下文        │            │
│  │                 │        │                  │            │
│  │ • BehaviorRecord│        │ • AnalysisConfig │            │
│  │ • BehaviorProfile│       │ • TrendReport    │            │
│  │ • Alert         │        │ • ComparisonReport│           │
│  │ • Intervention  │        │ • PredictiveAlert │           │
│  └─────────────────┘        └─────────────────┘            │
└─────────────────────────────────────────────────────────────┘
```

### 4.3 事件流设计

```
Configuration Context:
  TemplateUpdatedEvent → Planning重新关联
  WeightConfigUpdatedEvent → Result重新计算(可选)

Planning Context:
  PlanActivatedEvent → Scheduling开始排检
  InspectorAssignedEvent → 通知检查员

Execution Context:
  SessionStartedEvent → 记录检查开始
  DeductionRecordedEvent → 实时分数投影
  ChecklistRespondedEvent → 实时通过率投影
  SessionSubmittedEvent → Result生成快照
  SessionPublishedEvent → 通知班主任/触发评级/触发整改

Result Context:
  RankingCalculatedEvent → Rating评级计算
  RatingAssignedEvent → Badge徽章判断
  BadgeGrantedEvent → 通知班主任

Appeal Context:
  AppealSubmittedEvent → 通知审批人
  AppealApprovedEvent → Result重新计算(异步Saga)
  AppealEffectiveEvent → 最终排名更新

Behavior Context:
  BehaviorRecordedEvent → Profile更新
  AlertTriggeredEvent → 通知班主任/辅导员

Corrective Context:
  ActionCreatedEvent → 通知责任人
  ActionOverdueEvent → 升级通知
  ActionVerifiedEvent → 关闭问题
```

---

## 5. 限界上下文与领域模型

### 5.1 Configuration Context (配置上下文)

```
CheckTemplate（检查模板 - 聚合根）
  ├── templateId: Long
  ├── templateName: String
  ├── templateCode: String
  ├── totalRounds: Integer
  ├── roundNames: List<String>
  ├── scoringMode: DEDUCTION_ONLY | BASE_SCORE | DUAL_TRACK
  ├── baseScore: BigDecimal (仅BASE_SCORE/DUAL_TRACK)
  ├── status: ENABLED | DISABLED
  └── categories: List<CheckCategory>
       ├── categoryId
       ├── categoryName
       ├── categoryType: HYGIENE | DISCIPLINE | ATTENDANCE | DORMITORY | OTHER
       ├── inspectionMode: CHECKLIST | FREE_DEDUCTION | BOTH
       ├── linkType: NONE | DORMITORY | CLASSROOM
       ├── participatedRounds: List<Integer>
       └── deductionItems: List<DeductionItem>
            ├── itemId
            ├── itemName
            ├── deductMode: FIXED | PER_PERSON | RANGE
            ├── fixedScore / baseScore+perPersonScore / rangeConfig
            ├── allowPhoto: Boolean
            ├── allowStudents: Boolean
            ├── requirePhotoOnFail: Boolean (逐项核验模式-Fail时必须拍照)
            └── bonusEnabled: Boolean (是否支持加分, 仅DUAL_TRACK)

BonusItem（加分项 - 实体, 仅DUAL_TRACK模式）
  ├── itemId
  ├── categoryId
  ├── itemName: String
  ├── bonusMode: FIXED | PROGRESSIVE | IMPROVEMENT
  ├── fixedBonus: BigDecimal
  ├── progressiveConfig: JSON
  └── improvementCoefficient: BigDecimal

WeightConfig（权重配置 - 聚合根, 保留现有V5.0设计）
  └── [保留现有全部字段和逻辑]

RatingRule（评级规则 - 聚合根, 保留现有设计）
  └── [保留现有全部字段和逻辑]
```

### 5.2 Planning Context (计划上下文)

```
CheckPlan（检查计划 - 聚合根）
  ├── planId
  ├── planCode, planName
  ├── inspectionLevel: CLASS | DEPARTMENT | SPECIAL
  │   CLASS: 系部对班级的检查
  │   DEPARTMENT: 校级对系部的检查
  │   SPECIAL: 专项检查（跨层级）
  ├── templateId, templateSnapshot
  ├── targetScope: ALL | DEPARTMENT | GRADE | CUSTOM
  ├── targetConfig: JSON
  ├── weightConfigId, enableWeight
  ├── scoringMode: 从模板继承或覆盖
  ├── status: DRAFT | ACTIVE | FINISHED | ARCHIVED
  ├── dateRange: [startDate, endDate]
  └── inspectors: List<PlanInspector>
       ├── inspectorId, inspectorName
       ├── permissions: List<InspectorPermission>
       │    ├── categoryId
       │    └── targetClassIds/targetOrgUnitIds
       └── assignmentMode: FIXED | ROTATION | RANDOM

SchedulePolicy（调度策略 - 聚合根）
  ├── policyId
  ├── planId
  ├── policyType: PERIODIC | RANDOM | TRIGGER
  ├── periodicConfig:
  │    ├── cronExpression
  │    ├── timeWindow: [startTime, endTime]
  │    └── skipHolidays: Boolean
  ├── randomConfig:
  │    ├── frequencyPerWeek: Integer
  │    ├── minInterval: Duration
  │    └── samplingRate: 0.5 (随机抽取50%目标)
  ├── triggerConfig:
  │    ├── condition: "consecutive_low_score >= 3"
  │    └── action: "auto_create_special_check"
  └── inspectorRotation:
       ├── rotationMode: ROUND_ROBIN | RANDOM | LOAD_BALANCED
       ├── excludeSelfDepartment: Boolean
       └── maxDailyWorkload: Integer
```

### 5.3 Execution Context (检查执行上下文)

```
InspectionSession（检查会话 - 聚合根）
  ├── sessionId
  ├── planId
  ├── checkDate
  ├── inspectorId, inspectorName
  ├── inputMode: SPACE_FIRST | PERSON_FIRST | ORG_FIRST
  ├── status: CREATED → IN_PROGRESS → SUBMITTED → PUBLISHED
  ├── version: Integer (乐观锁)
  └── 不包含具体扣分数据（引用关系）

ClassInspectionRecord（班级检查记录 - 聚合根）
  ├── recordId
  ├── sessionId
  ├── classId, className (自动解析或手动选择)
  ├── status: PENDING | RECORDING | COMPLETED
  ├── version: Integer (独立乐观锁)
  ├── deductions: List<InspectionDeduction>
  │    ├── deductionId
  │    ├── categoryId
  │    ├── deductionItemId
  │    ├── deductMode, deductScore
  │    ├── personCount, studentIds, studentNames
  │    ├── spaceType: DORMITORY | CLASSROOM | NONE
  │    ├── spaceId, spaceNo
  │    ├── photoUrls: List<String>
  │    ├── remark
  │    └── inputSource: CHECKLIST_FAIL | FREE_DEDUCTION | SPACE_RESOLVED | PERSON_RESOLVED
  ├── checklistResponses: List<ChecklistResponse> (逐项核验模式)
  │    ├── responseId
  │    ├── deductionItemId
  │    ├── spaceId (哪个房间的这一项)
  │    ├── result: PASS | FAIL | NA
  │    ├── failDeductionId (如果FAIL, 关联到deductions)
  │    └── respondedAt: LocalDateTime
  └── bonuses: List<InspectionBonus> (仅DUAL_TRACK模式)
       ├── bonusId
       ├── bonusItemId
       ├── bonusScore
       └── reason

SpaceToOrgResolver（物理空间→组织解析 - 领域服务）
  resolveDormitory(dormitoryId) → List<ClassAllocation>
  resolveClassroom(classroomId) → ClassAllocation
  resolveStudent(studentId) → StudentOrgInfo
  resolveStudentBatch(studentIds) → Map<ClassId, List<StudentInfo>>
```

### 5.4 Result Context (结果上下文)

```
InspectionResult（检查结果 - 聚合根）
  ├── resultId
  ├── sessionId
  ├── planId
  ├── inspectionLevel: CLASS | DEPARTMENT
  ├── checkDate
  ├── templateSnapshot: JSON
  ├── weightConfigSnapshot: JSON
  ├── scoringMode
  ├── baseScore (如适用)
  ├── status: GENERATED | PUBLISHED | ARCHIVED
  ├── snapshotVersion: Integer
  └── classStats: List<ClassResultStats>
       ├── classId, className, gradeId, orgUnitId
       ├── classSize
       ├── deductionTotal: BigDecimal
       ├── bonusTotal: BigDecimal (如适用)
       ├── finalScore: BigDecimal (=baseScore - deductionTotal + bonusTotal)
       ├── passRate: BigDecimal (逐项核验模式的通过率)
       ├── categoryScores: Map<CategoryType, BigDecimal>
       ├── overallRanking, gradeRanking, orgUnitRanking
       ├── weightedScore, weightedRanking (如启用权重)
       ├── scoreLevel: String (评级名称)
       ├── vsAvgDiff, vsLastDiff, trend
       └── checklistCoverage: BigDecimal (核验模式的完成率)

DepartmentResultStats（系部结果 - 仅DEPARTMENT级检查）
  ├── orgUnitId, orgUnitName
  ├── avgClassScore: BigDecimal
  ├── classCount: Integer
  ├── departmentSpecificScore: BigDecimal (系部独立检查项得分)
  ├── compositeScore: BigDecimal (班级汇总×权重 + 独立项)
  ├── departmentRanking: Integer
  └── departmentLevel: String

RatingResult（评级结果 - 实体）
  ├── ruleId, levelId, levelName
  ├── classId / orgUnitId
  ├── score, ranking
  └── publishStatus

BadgeRecord（徽章记录 - 实体）
  └── [保留现有设计]
```

### 5.5 Corrective Context (整改上下文) — 新增

```
CorrectiveAction（整改工单 - 聚合根）
  ├── actionId
  ├── actionCode: String "CA-20260127-001"
  ├── sourceType: INSPECTION | APPEAL | MANUAL
  ├── sourceRef: { sessionId, deductionId, classId }
  ├── title: String "301宿舍地面不洁"
  ├── description: String
  ├── severity: MINOR | MODERATE | SEVERE | CRITICAL
  ├── category: HYGIENE | DISCIPLINE | SAFETY | OTHER
  ├── photoUrls: List<String> (问题照片)
  ├── assigneeId: Long (责任人, 通常是班主任)
  ├── assigneeName: String
  ├── dueDate: LocalDate
  ├── status: OPEN → IN_PROGRESS → REVIEW → CLOSED | OVERDUE | ESCALATED
  ├── resolution: String (整改措施描述)
  ├── resolutionPhotos: List<String> (整改后照片)
  ├── resolvedAt: LocalDateTime
  ├── verifierId: Long (复核人, 通常是原检查员)
  ├── verifyResult: PENDING | PASS | FAIL
  ├── verifyComment: String
  ├── verifiedAt: LocalDateTime
  └── escalationLevel: Integer (升级次数)

自动创建规则:
  AutoActionRule
  ├── ruleId
  ├── triggerCondition: "deductScore >= {threshold}" | "severity >= SEVERE"
  ├── autoAssignTo: CLASS_TEACHER | DORMITORY_HEAD | CUSTOM
  ├── defaultDueDays: Integer
  └── enabled: Boolean
```

### 5.6 Behavior Context (学生行为上下文) — 新增

```
BehaviorRecord（行为记录 - 聚合根）
  ├── recordId
  ├── studentId, studentName
  ├── classId, className, orgUnitId
  ├── behaviorType: VIOLATION | COMMENDATION
  ├── category: DISCIPLINE | HYGIENE | SAFETY | ATTENDANCE | OTHER
  ├── severity: MINOR | MODERATE | SEVERE | CRITICAL
  ├── description: String
  ├── source: INSPECTION | TEACHER_REPORT | SELF_REPORT
  ├── sourceRef: { inspectionSessionId, deductionId }
  ├── status: RECORDED → NOTIFIED → ACKNOWLEDGED → RESOLVED
  ├── handlerId: Long (班主任/辅导员)
  ├── resolution: String (教育谈话/家长通知/纪律处分)
  └── resolvedAt: LocalDateTime

BehaviorProfile（行为画像 - 投影/读模型）
  ├── studentId
  ├── violationCount, commendationCount
  ├── recentViolations: List (近30天)
  ├── frequentCategories: Map<Category, Integer>
  ├── trend: IMPROVING | STABLE | DETERIORATING
  ├── riskLevel: LOW | MEDIUM | HIGH | CRITICAL
  └── lastUpdated: LocalDateTime

BehaviorAlert（行为预警 - 实体）
  ├── alertId
  ├── studentId
  ├── alertType: FREQUENCY | SEVERITY | TREND
  ├── condition: "violations >= 3 in 30 days"
  ├── notifyTargets: List<UserId>
  └── status: ACTIVE | ACKNOWLEDGED | RESOLVED
```

---

## 6. 数据库设计

### 6.1 新增/修改表清单

以下为V4新增的核心表，保留现有表不变（标注[保留]的不修改）。

#### 6.1.1 检查会话表（拆分自daily_checks）

```sql
-- 检查会话（轻量级，管理生命周期）
CREATE TABLE inspection_sessions (
    id              BIGINT PRIMARY KEY COMMENT '雪花ID',
    plan_id         BIGINT NOT NULL COMMENT '检查计划ID',
    check_date      DATE NOT NULL COMMENT '检查日期',
    check_name      VARCHAR(100) COMMENT '检查名称',
    check_type      TINYINT DEFAULT 1 COMMENT '1=日常, 2=专项',
    inspection_level VARCHAR(20) DEFAULT 'CLASS' COMMENT 'CLASS/DEPARTMENT/SPECIAL',
    scoring_mode    VARCHAR(20) DEFAULT 'DEDUCTION_ONLY' COMMENT '评分模式',
    base_score      DECIMAL(10,2) DEFAULT 0 COMMENT '基准分(仅BASE_SCORE/DUAL_TRACK)',
    inspector_id    BIGINT COMMENT '检查员ID',
    inspector_name  VARCHAR(100) COMMENT '检查员姓名',
    input_mode      VARCHAR(20) DEFAULT 'SPACE_FIRST' COMMENT '录入模式: SPACE_FIRST/PERSON_FIRST/ORG_FIRST',
    template_id     BIGINT COMMENT '模板ID',
    template_snapshot LONGTEXT COMMENT '模板快照JSON',
    enable_weight   TINYINT DEFAULT 0,
    weight_config_id BIGINT,
    weight_config_snapshot LONGTEXT,
    total_rounds    INT DEFAULT 1,
    round_names     JSON COMMENT '轮次名称',
    status          TINYINT DEFAULT 0 COMMENT '0=未开始,1=进行中,2=已提交,3=已发布',
    version         INT DEFAULT 0 COMMENT '乐观锁',
    submitted_at    DATETIME COMMENT '提交时间',
    published_at    DATETIME COMMENT '发布时间',
    created_by      BIGINT,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT DEFAULT 0,
    INDEX idx_plan_id (plan_id),
    INDEX idx_check_date (check_date),
    INDEX idx_inspector (inspector_id),
    INDEX idx_status (status)
) COMMENT '检查会话';

-- 班级检查记录（独立聚合，独立并发控制）
CREATE TABLE class_inspection_records (
    id              BIGINT PRIMARY KEY,
    session_id      BIGINT NOT NULL COMMENT '会话ID',
    class_id        BIGINT NOT NULL COMMENT '班级ID',
    class_name      VARCHAR(100) COMMENT '班级名称快照',
    grade_id        BIGINT,
    grade_name      VARCHAR(50),
    org_unit_id     BIGINT,
    org_unit_name   VARCHAR(100),
    status          TINYINT DEFAULT 0 COMMENT '0=待检查,1=检查中,2=已完成',
    version         INT DEFAULT 0 COMMENT '独立乐观锁',
    input_source    VARCHAR(20) COMMENT '录入来源: SPACE/PERSON/ORG/CHECKLIST',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_session (session_id),
    INDEX idx_class (class_id),
    UNIQUE KEY uk_session_class (session_id, class_id)
) COMMENT '班级检查记录';
```

#### 6.1.2 逐项核验响应表（新增）

```sql
-- 逐项核验模式的每项响应记录
CREATE TABLE checklist_responses (
    id                  BIGINT PRIMARY KEY,
    class_record_id     BIGINT NOT NULL COMMENT '班级检查记录ID',
    session_id          BIGINT NOT NULL,
    class_id            BIGINT NOT NULL,
    category_id         BIGINT NOT NULL,
    deduction_item_id   BIGINT NOT NULL COMMENT '检查项ID',
    item_name           VARCHAR(100) COMMENT '项目名称快照',
    check_round         INT DEFAULT 1 COMMENT '检查轮次',
    space_type          VARCHAR(20) COMMENT 'DORMITORY/CLASSROOM/NONE',
    space_id            BIGINT COMMENT '宿舍/教室ID',
    space_no            VARCHAR(50) COMMENT '房间号',
    result              VARCHAR(10) NOT NULL COMMENT 'PASS/FAIL/NA',
    fail_deduction_id   BIGINT COMMENT 'FAIL时关联的扣分记录ID',
    photo_urls          JSON COMMENT '检查照片',
    remark              VARCHAR(500),
    responded_by        BIGINT COMMENT '响应人ID',
    responded_at        DATETIME,
    created_at          DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_class_record (class_record_id),
    INDEX idx_session (session_id),
    INDEX idx_result (result)
) COMMENT '逐项核验响应';
```

#### 6.1.3 加分记录表（新增，仅DUAL_TRACK模式）

```sql
-- 加分项配置
CREATE TABLE bonus_items (
    id              BIGINT PRIMARY KEY,
    category_id     BIGINT NOT NULL COMMENT '所属检查类别',
    item_name       VARCHAR(100) NOT NULL,
    bonus_mode      VARCHAR(20) NOT NULL COMMENT 'FIXED/PROGRESSIVE/IMPROVEMENT',
    fixed_bonus     DECIMAL(10,2) COMMENT '固定加分值',
    progressive_config JSON COMMENT '递进加分配置',
    improvement_coefficient DECIMAL(5,2) COMMENT '改善系数',
    description     VARCHAR(500),
    sort_order      INT DEFAULT 0,
    status          TINYINT DEFAULT 1,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT DEFAULT 0
) COMMENT '加分项配置';

-- 加分记录
CREATE TABLE inspection_bonuses (
    id              BIGINT PRIMARY KEY,
    class_record_id BIGINT NOT NULL,
    session_id      BIGINT NOT NULL,
    class_id        BIGINT NOT NULL,
    bonus_item_id   BIGINT NOT NULL,
    bonus_score     DECIMAL(10,2) NOT NULL,
    reason          VARCHAR(500),
    recorded_by     BIGINT,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_class_record (class_record_id),
    INDEX idx_session (session_id)
) COMMENT '加分记录';
```

#### 6.1.4 整改工单表（新增）

```sql
CREATE TABLE corrective_actions (
    id              BIGINT PRIMARY KEY,
    action_code     VARCHAR(50) NOT NULL COMMENT '工单编号',
    source_type     VARCHAR(20) NOT NULL COMMENT 'INSPECTION/APPEAL/MANUAL',
    source_session_id BIGINT COMMENT '来源检查会话ID',
    source_deduction_id BIGINT COMMENT '来源扣分记录ID',
    class_id        BIGINT NOT NULL,
    class_name      VARCHAR(100),
    title           VARCHAR(200) NOT NULL COMMENT '问题标题',
    description     TEXT COMMENT '问题详细描述',
    severity        VARCHAR(20) NOT NULL COMMENT 'MINOR/MODERATE/SEVERE/CRITICAL',
    category        VARCHAR(20) COMMENT 'HYGIENE/DISCIPLINE/SAFETY/OTHER',
    photo_urls      JSON COMMENT '问题照片',
    assignee_id     BIGINT COMMENT '责任人ID',
    assignee_name   VARCHAR(100),
    due_date        DATE COMMENT '整改截止日',
    status          VARCHAR(20) DEFAULT 'OPEN' COMMENT 'OPEN/IN_PROGRESS/REVIEW/CLOSED/OVERDUE/ESCALATED',
    resolution      TEXT COMMENT '整改措施',
    resolution_photos JSON COMMENT '整改后照片',
    resolved_at     DATETIME,
    verifier_id     BIGINT COMMENT '复核人ID',
    verify_result   VARCHAR(10) COMMENT 'PENDING/PASS/FAIL',
    verify_comment  VARCHAR(500),
    verified_at     DATETIME,
    escalation_level INT DEFAULT 0 COMMENT '升级次数',
    created_by      BIGINT,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT DEFAULT 0,
    INDEX idx_class (class_id),
    INDEX idx_assignee (assignee_id),
    INDEX idx_status (status),
    INDEX idx_due_date (due_date),
    INDEX idx_source (source_session_id)
) COMMENT '整改工单';

-- 自动创建规则
CREATE TABLE auto_action_rules (
    id              BIGINT PRIMARY KEY,
    rule_name       VARCHAR(100),
    trigger_type    VARCHAR(20) COMMENT 'SCORE_THRESHOLD/SEVERITY/CONSECUTIVE',
    trigger_config  JSON COMMENT '触发条件配置',
    auto_assign_to  VARCHAR(20) COMMENT 'CLASS_TEACHER/DORMITORY_HEAD/CUSTOM',
    custom_assignee_id BIGINT,
    default_due_days INT DEFAULT 3,
    enabled         TINYINT DEFAULT 1,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '整改自动创建规则';
```

#### 6.1.5 学生行为表（新增）

```sql
-- 学生行为记录
CREATE TABLE student_behavior_records (
    id              BIGINT PRIMARY KEY,
    student_id      BIGINT NOT NULL,
    student_name    VARCHAR(50),
    class_id        BIGINT NOT NULL,
    class_name      VARCHAR(100),
    org_unit_id     BIGINT,
    behavior_type   VARCHAR(20) NOT NULL COMMENT 'VIOLATION/COMMENDATION',
    category        VARCHAR(20) COMMENT 'DISCIPLINE/HYGIENE/SAFETY/ATTENDANCE/OTHER',
    severity        VARCHAR(20) COMMENT 'MINOR/MODERATE/SEVERE/CRITICAL',
    description     TEXT,
    source          VARCHAR(20) COMMENT 'INSPECTION/TEACHER_REPORT/SELF_REPORT',
    source_session_id BIGINT,
    source_deduction_id BIGINT,
    status          VARCHAR(20) DEFAULT 'RECORDED' COMMENT 'RECORDED/NOTIFIED/ACKNOWLEDGED/RESOLVED',
    handler_id      BIGINT COMMENT '处理人(班主任/辅导员)',
    handler_name    VARCHAR(100),
    resolution      VARCHAR(500) COMMENT '处理措施',
    resolved_at     DATETIME,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT DEFAULT 0,
    INDEX idx_student (student_id),
    INDEX idx_class (class_id),
    INDEX idx_type (behavior_type),
    INDEX idx_source (source_session_id)
) COMMENT '学生行为记录';

-- 学生行为预警
CREATE TABLE student_behavior_alerts (
    id              BIGINT PRIMARY KEY,
    student_id      BIGINT NOT NULL,
    student_name    VARCHAR(50),
    class_id        BIGINT NOT NULL,
    alert_type      VARCHAR(20) COMMENT 'FREQUENCY/SEVERITY/TREND',
    alert_condition VARCHAR(200) COMMENT '触发条件描述',
    risk_level      VARCHAR(20) COMMENT 'LOW/MEDIUM/HIGH/CRITICAL',
    notify_targets  JSON COMMENT '通知人员IDs',
    status          VARCHAR(20) DEFAULT 'ACTIVE' COMMENT 'ACTIVE/ACKNOWLEDGED/RESOLVED',
    acknowledged_by BIGINT,
    acknowledged_at DATETIME,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_student (student_id),
    INDEX idx_status (status),
    INDEX idx_risk (risk_level)
) COMMENT '学生行为预警';
```

#### 6.1.6 调度策略表（新增）

```sql
CREATE TABLE schedule_policies (
    id              BIGINT PRIMARY KEY,
    plan_id         BIGINT NOT NULL COMMENT '关联检查计划',
    policy_name     VARCHAR(100),
    policy_type     VARCHAR(20) NOT NULL COMMENT 'PERIODIC/RANDOM/TRIGGER',
    -- 周期性配置
    cron_expression VARCHAR(100) COMMENT 'cron表达式',
    time_window_start TIME COMMENT '时间窗口开始',
    time_window_end TIME COMMENT '时间窗口结束',
    skip_holidays   TINYINT DEFAULT 1,
    -- 随机配置
    frequency_per_week INT COMMENT '每周次数',
    min_interval_days INT COMMENT '最小间隔天数',
    sampling_rate   DECIMAL(3,2) COMMENT '抽样比例 0.5=50%',
    -- 触发配置
    trigger_condition JSON COMMENT '触发条件',
    -- 检查员轮转
    rotation_mode   VARCHAR(20) COMMENT 'ROUND_ROBIN/RANDOM/LOAD_BALANCED',
    exclude_self_dept TINYINT DEFAULT 1 COMMENT '排除检查自己系部',
    max_daily_workload INT DEFAULT 5 COMMENT '每人每天最大检查数',
    -- 通用
    enabled         TINYINT DEFAULT 1,
    last_executed_at DATETIME,
    next_execute_at DATETIME,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT DEFAULT 0,
    INDEX idx_plan (plan_id),
    INDEX idx_next (next_execute_at)
) COMMENT '检查调度策略';
```

### 6.2 现有表修改

```sql
-- check_plans: 新增字段
ALTER TABLE check_plans ADD COLUMN inspection_level VARCHAR(20) DEFAULT 'CLASS'
    COMMENT 'CLASS/DEPARTMENT/SPECIAL' AFTER plan_name;
ALTER TABLE check_plans ADD COLUMN scoring_mode VARCHAR(20) DEFAULT 'DEDUCTION_ONLY'
    COMMENT '评分模式' AFTER inspection_level;
ALTER TABLE check_plans ADD COLUMN base_score DECIMAL(10,2) DEFAULT 0
    COMMENT '基准分' AFTER scoring_mode;

-- check_record_class_stats_new: 新增字段
ALTER TABLE check_record_class_stats_new ADD COLUMN bonus_total DECIMAL(10,2) DEFAULT 0
    COMMENT '加分总计' AFTER total_score;
ALTER TABLE check_record_class_stats_new ADD COLUMN final_score DECIMAL(10,2) DEFAULT 0
    COMMENT '最终得分(基准-扣分+加分)' AFTER bonus_total;
ALTER TABLE check_record_class_stats_new ADD COLUMN pass_rate DECIMAL(5,2)
    COMMENT '逐项核验通过率' AFTER final_score;
ALTER TABLE check_record_class_stats_new ADD COLUMN checklist_coverage DECIMAL(5,2)
    COMMENT '核验完成率' AFTER pass_rate;
```

---

## 7. API接口设计

### 7.1 检查执行API（核心重设计）

```
=== 物理空间优先录入 ===

GET  /api/v2/inspection/spaces/buildings
     查询可检查的楼栋列表
     Response: [{ buildingId, buildingName, buildingType, totalFloors }]

GET  /api/v2/inspection/spaces/rooms?buildingId={id}&floor={n}
     查询指定楼层的所有房间（按房间号排序）
     Response: {
       building: { id, name, floor },
       rooms: [{
         spaceId, spaceNo, spaceType: "DORMITORY|CLASSROOM",
         classes: [{ classId, className, studentCount }],  // 辅助信息
         lastCheckDate, lastCheckResult  // 上次检查信息
       }],
       orderedBy: "ROOM_NUMBER"
     }

=== 学生快速搜索 ===

GET  /api/v2/students/quick-search?keyword={name}&limit=10
     根据姓名/学号模糊搜索学生
     Response: [{
       studentId, name, studentNo,
       className, gradeName, orgUnitName,
       photoUrl
     }]

=== 检查会话管理 ===

POST /api/v2/inspection/sessions
     创建检查会话
     Body: { planId, checkDate, inputMode, inspectorId }

GET  /api/v2/inspection/sessions/{sessionId}
     获取会话详情（含所有班级记录）

GET  /api/v2/inspection/sessions/{sessionId}/scoring-init
     获取录入初始化数据（类别、扣分项、关联资源）

PATCH /api/v2/inspection/sessions/{sessionId}/status
      更新会话状态（提交/发布）

=== 物理空间模式录入 ===

POST /api/v2/inspection/sessions/{sessionId}/space-deductions
     按物理空间录入扣分（系统自动解析班级）
     Body: {
       spaceType: "DORMITORY",
       spaceId: 12345,
       categoryId: 1,
       deductionItemId: 15,
       deductScore: 2.0,
       checkRound: 1,
       remark: "...",
       photoUrls: [...]
     }
     Response: {
       deductionId: 99999,
       resolvedClasses: [
         { classId: 1, className: "计算机2班", allocatedScore: 1.67, ratio: 0.833 },
         { classId: 2, className: "软件1班", allocatedScore: 0.33, ratio: 0.167 }
       ]
     }

=== 人员模式录入 ===

POST /api/v2/inspection/sessions/{sessionId}/person-deductions
     按人员录入扣分（系统自动解析班级）
     Body: {
       studentIds: [10001, 10025],
       categoryId: 2,
       deductionItemId: 8,
       deductScore: 2.0,
       checkRound: 1
     }
     Response: {
       deductions: [
         { deductionId: 100, classId: 1, className: "计算机2班",
           students: ["张三"], score: 2.0 },
         { deductionId: 101, classId: 3, className: "软件1班",
           students: ["李四"], score: 2.0 }
       ]
     }

=== 逐项核验模式 ===

POST /api/v2/inspection/sessions/{sessionId}/checklist-responses
     批量提交核验响应
     Body: {
       classRecordId: 12345,
       responses: [
         { deductionItemId: 1, spaceId: 301, result: "PASS" },
         { deductionItemId: 2, spaceId: 301, result: "FAIL",
           deductScore: 2.0, photoUrls: [...], remark: "..." },
         { deductionItemId: 3, spaceId: 301, result: "NA" }
       ]
     }

GET  /api/v2/inspection/sessions/{sessionId}/checklist-progress
     获取核验进度
     Response: {
       totalItems: 120,
       respondedItems: 85,
       passCount: 70, failCount: 10, naCount: 5,
       progress: 70.8,
       bySpace: [
         { spaceNo: "301", total: 10, responded: 10, status: "COMPLETE" },
         { spaceNo: "302", total: 10, responded: 5, status: "IN_PROGRESS" },
         { spaceNo: "303", total: 10, responded: 0, status: "PENDING" }
       ]
     }

=== 传统组织模式（保留兼容）===

POST /api/v2/inspection/sessions/{sessionId}/deductions
     传统按班级录入扣分
     Body: { classId, categoryId, deductionItemId, deductScore, ... }
```

### 7.2 整改工单API（新增）

```
POST /api/v2/corrective-actions
     创建整改工单

GET  /api/v2/corrective-actions?status={s}&classId={id}&assigneeId={id}
     查询整改工单列表

GET  /api/v2/corrective-actions/{actionId}
     获取工单详情

PATCH /api/v2/corrective-actions/{actionId}/resolve
      提交整改结果 Body: { resolution, photos }

PATCH /api/v2/corrective-actions/{actionId}/verify
      复核整改结果 Body: { verifyResult: "PASS|FAIL", comment }

GET  /api/v2/corrective-actions/statistics
     整改工单统计（完成率、超期率、按类别分布）
```

### 7.3 学生行为API（新增）

```
GET  /api/v2/student-behaviors?studentId={id}&classId={id}&type={type}
     查询行为记录

GET  /api/v2/student-behaviors/profile/{studentId}
     获取学生行为画像

GET  /api/v2/student-behaviors/alerts?status=ACTIVE
     查询活跃预警

PATCH /api/v2/student-behaviors/alerts/{alertId}/acknowledge
      确认预警
```

### 7.4 班主任工作台API（新增）

```
GET  /api/v2/teacher-dashboard/overview
     班主任首页概览（本班本周/月量化、排名、评级、趋势）

GET  /api/v2/teacher-dashboard/deductions?dateRange={range}
     本班扣分明细（时间倒序）

GET  /api/v2/teacher-dashboard/top-issues
     本班高频问题TOP5

GET  /api/v2/teacher-dashboard/students/violations
     本班学生违纪排行

GET  /api/v2/teacher-dashboard/actions
     本班待处理整改工单

GET  /api/v2/teacher-dashboard/improvement
     改善追踪（环比数据）
```

### 7.5 调度引擎API（新增）

```
POST /api/v2/schedule-policies
     创建调度策略

GET  /api/v2/schedule-policies?planId={id}
     查询计划的调度策略

PATCH /api/v2/schedule-policies/{policyId}/toggle
      启用/停用策略

GET  /api/v2/schedule-policies/upcoming
     查看即将执行的调度任务
```

---

## 8. 前端架构设计

### 8.1 路由结构

```
/inspection (量化检查)
├── /inspection/config                    检查配置中心
│   ├── /inspection/config/templates      模板管理
│   ├── /inspection/config/weights        权重方案管理
│   └── /inspection/config/ratings        评级规则管理
├── /inspection/plans                     检查计划管理
│   ├── /inspection/plans/list            计划列表
│   ├── /inspection/plans/create          创建计划
│   └── /inspection/plans/:id             计划详情
├── /inspection/execute                   检查执行
│   ├── /inspection/execute/my-tasks      我的检查任务
│   ├── /inspection/execute/:sessionId    执行录入页面 ★核心页面
│   └── /inspection/execute/history       检查历史
├── /inspection/results                   检查结果
│   ├── /inspection/results/records       检查记录列表
│   ├── /inspection/results/:recordId     记录详情
│   ├── /inspection/results/ranking       排名总览
│   └── /inspection/results/rating        评级结果
├── /inspection/appeals                   申诉管理
├── /inspection/actions                   整改管理（新增）
├── /inspection/schedule                  调度配置（新增）
└── /inspection/analytics                 数据分析中心

/behavior (学生行为 - 新增)
├── /behavior/records                     行为记录
├── /behavior/profile/:studentId          学生画像
└── /behavior/alerts                      行为预警

/teacher-dashboard (班主任工作台 - 新增)
├── /teacher-dashboard/overview           概览
├── /teacher-dashboard/deductions         扣分明细
├── /teacher-dashboard/students           学生管理
└── /teacher-dashboard/actions            整改追踪
```

### 8.2 组件架构

```
src/modules/inspection/
├── api/                          API层
│   ├── session.ts                检查会话API
│   ├── space.ts                  物理空间API
│   ├── checklist.ts              逐项核验API
│   ├── result.ts                 结果API
│   ├── appeal.ts                 申诉API
│   ├── action.ts                 整改API
│   └── schedule.ts               调度API
├── types/                        类型定义
│   ├── session.ts
│   ├── checklist.ts
│   ├── result.ts
│   └── action.ts
├── stores/                       Pinia状态
│   ├── inspectionSession.ts      检查会话状态
│   ├── checklistProgress.ts      核验进度状态
│   └── teacherDashboard.ts       班主任工作台状态
├── views/                        页面
│   ├── config/                   配置页面
│   ├── plan/                     计划页面
│   ├── execute/                  执行页面 ★
│   │   ├── InspectionExecute.vue 主执行页面
│   │   ├── SpaceMode.vue         物理空间模式
│   │   ├── PersonMode.vue        人员模式
│   │   ├── ChecklistMode.vue     逐项核验模式
│   │   └── QuickDeductionMode.vue 快速扣分模式
│   ├── result/                   结果页面
│   ├── appeal/                   申诉页面
│   ├── action/                   整改页面
│   └── analytics/                分析页面
└── components/                   可复用组件
    ├── FloorPlanGrid.vue         楼层平面图网格
    ├── RoomStatusCard.vue        房间状态卡片
    ├── StudentSearchInput.vue    学生搜索输入
    ├── DeductionItemSelector.vue 扣分项选择器
    ├── ChecklistItemRow.vue      核验项行
    ├── EvidenceCapture.vue       证据采集
    ├── ScoreSummaryPanel.vue     评分汇总面板
    ├── RankingTable.vue          排名表格
    ├── TrendChart.vue            趋势图表
    └── ActionStatusBadge.vue     整改状态徽章
```

---

## 9. 前端UI页面详细设计

### 9.1 检查执行页面 — 物理空间模式 (核心页面)

**页面路径**: `/inspection/execute/:sessionId`

**布局结构**:
```
┌─────────────────────────────────────────────────────────────┐
│ 顶部工具栏                                                    │
│ [← 返回] 2026年1月27日 宿舍卫生检查  [暂存] [提交]             │
│ 录入模式: (●)物理空间  ( )人员搜索  ( )按班级  ( )逐项核验      │
│ 进度: ████████░░ 80% (16/20间完成)                           │
└─────────────────────────────────────────────────────────────┘
┌──────────────────┬──────────────────────────────────────────┐
│ 左侧楼层导航      │ 右侧检查区域                               │
│                  │                                          │
│ ┌──────────────┐ │ 1号宿舍楼 - 3层                           │
│ │ 1号宿舍楼    │ │ ┌────┐ ┌────┐ ┌────┐ ┌────┐ ┌────┐     │
│ │  3层 ●       │ │ │301 │ │302 │ │303 │ │304 │ │305 │     │
│ │  4层 ○       │ │ │✓   │ │✓   │ │!-2 │ │    │ │    │     │
│ │  5层 ○       │ │ └────┘ └────┘ └────┘ └────┘ └────┘     │
│ │              │ │ ┌────┐ ┌────┐ ┌────┐ ┌────┐ ┌────┐     │
│ │ 2号宿舍楼    │ │ │306 │ │307 │ │308 │ │309 │ │310 │     │
│ │  1层 ○       │ │ │    │ │    │ │    │ │    │ │    │     │
│ │  2层 ○       │ │ └────┘ └────┘ └────┘ └────┘ └────┘     │
│ └──────────────┘ │                                          │
│                  │ ─────────────────────────────────────── │
│ 已检查: 16间     │ 303号扣分详情:                              │
│ 有问题: 3间      │ ┌──────────────────────────────────────┐ │
│ 合格: 13间       │ │ 扣分类别: 卫生检查                      │ │
│ 未检查: 4间      │ │ 扣分项: 地面不洁 (-2.0分)              │ │
│                  │ │ 所属班级: 计算机2班(0.83) 软件1班(0.17)│ │
│                  │ │ 证据照片: [📷]                         │ │
│                  │ │ [编辑] [删除]                          │ │
│                  │ └──────────────────────────────────────┘ │
└──────────────────┴──────────────────────────────────────────┘
```

**交互流程**:
1. 左侧选择楼栋和楼层
2. 右侧网格展示该层所有房间（按号排序）
3. 点击房间卡片 → 弹出快捷扣分面板:
   - 常用扣分项一键选择（地面不洁-2、物品乱放-1、被褥未叠-1）
   - 拍照取证按钮
   - "无问题"一键标记
   - 下方显示该房间关联班级信息（自动解析）
4. 操作后房间变色: 绿色=合格, 红色=有问题, 灰色=未检查
5. 自动跳转下一间房

**房间卡片状态**:
```
未检查: bg-gray-100 border-gray-200
合格:   bg-emerald-50 border-emerald-300 + ✓图标
有问题: bg-red-50 border-red-300 + 扣分数字
当前:   ring-2 ring-blue-500 (选中高亮)
```

### 9.2 检查执行页面 — 人员搜索模式

**布局结构**:
```
┌─────────────────────────────────────────────────────────────┐
│ 顶部工具栏 (同上)                                             │
│ 录入模式: ( )物理空间  (●)人员搜索  ( )按班级  ( )逐项核验      │
└─────────────────────────────────────────────────────────────┘
┌─────────────────────────────────────────────────────────────┐
│ 搜索区域                                                      │
│ ┌─────────────────────────────────────────┐                  │
│ │ 🔍 输入学生姓名或学号搜索...               │ [扫码录入]       │
│ └─────────────────────────────────────────┘                  │
│                                                              │
│ 搜索结果:                                                     │
│ ┌──────────────────────────────────────────────────────────┐ │
│ │ □ 张三  |  2024010001  |  计算机2班  |  信息工程系        │ │
│ │ □ 张伟  |  2024020015  |  电子商务1班 |  经济管理系       │ │
│ │ □ 张丽  |  2023030008  |  会计3班    |  财经系           │ │
│ └──────────────────────────────────────────────────────────┘ │
│                                                              │
│ 已选学生 (2人):                                               │
│ ┌──────────────────────────────────────────────────────────┐ │
│ │ ■ 张三 (计算机2班)  [×]                                   │ │
│ │ ■ 李四 (软件1班)    [×]                                   │ │
│ └──────────────────────────────────────────────────────────┘ │
│                                                              │
│ 扣分信息:                                                     │
│ 检查类别: [纪律检查 ▼]                                        │
│ 扣分项:   [迟到 -2分 ▼]                                      │
│ 备注:     [                           ]                      │
│                                                              │
│ 预览: 系统将自动生成2条扣分记录                                  │
│   → 计算机2班: 迟到 -2分 (张三)                                │
│   → 软件1班:   迟到 -2分 (李四)                                │
│                                                              │
│ [确认录入]                                                    │
└─────────────────────────────────────────────────────────────┘
│                                                              │
│ 本次已录入记录:                                                │
│ ┌──────────────────────────────────────────────────────────┐ │
│ │ 08:05  张三(计算机2班)  迟到  -2分                        │ │
│ │ 08:05  李四(软件1班)    迟到  -2分                        │ │
│ │ 08:12  王五(电商1班)    吸烟  -5分                        │ │
│ └──────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

### 9.3 检查执行页面 — 逐项核验模式

**布局结构**:
```
┌─────────────────────────────────────────────────────────────┐
│ 顶部工具栏                                                    │
│ 录入模式: ( )物理空间  ( )人员搜索  ( )按班级  (●)逐项核验      │
│ 进度: ████░░░░░░ 35% (42/120项)  通过率: 87.5%              │
└─────────────────────────────────────────────────────────────┘
┌──────────────────┬──────────────────────────────────────────┐
│ 左侧房间/班级导航  │ 右侧核验清单                               │
│                  │                                          │
│ 当前: 301号宿舍   │ === 301号宿舍 (计算机2班 15人) ===        │
│                  │                                          │
│ ┌──────────────┐ │ 卫生检查项:                                │
│ │ 301  10/10 ✓ │ │ ┌────────────────────────────────────┐   │
│ │ 302  8/10    │ │ │ 1. 地面清洁                         │   │
│ │ 303  0/10    │ │ │    [✅通过]  [❌不通过]  [➖不适用]   │   │
│ │ 304  0/10    │ │ └────────────────────────────────────┘   │
│ │ 305  0/10    │ │ ┌────────────────────────────────────┐   │
│ │ ...          │ │ │ 2. 被褥整齐        ❌ 不通过 -1分   │   │
│ └──────────────┘ │ │    备注: 2张床被褥未叠               │   │
│                  │ │    照片: [📷已拍1张]                  │   │
│ 完成率:          │ │    [修改]                             │   │
│ ■■■■□□□□□□ 40%  │ └────────────────────────────────────┘   │
│                  │ ┌────────────────────────────────────┐   │
│                  │ │ 3. 物品摆放                         │   │
│                  │ │    [✅通过]  [❌不通过]  [➖不适用]   │   │
│                  │ └────────────────────────────────────┘   │
│                  │ ┌────────────────────────────────────┐   │
│                  │ │ 4. 窗户干净                         │   │
│                  │ │    [✅通过]  [❌不通过]  [➖不适用]   │   │
│                  │ └────────────────────────────────────┘   │
│                  │                                          │
│                  │ 纪律检查项:                                │
│                  │ ┌────────────────────────────────────┐   │
│                  │ │ 5. 用电安全                         │   │
│                  │ │    [✅通过]  [❌不通过]  [➖不适用]   │   │
│                  │ └────────────────────────────────────┘   │
│                  │                                          │
│                  │ [全部标记为通过]    [下一间 →]             │
└──────────────────┴──────────────────────────────────────────┘
```

### 9.4 班主任工作台

**布局结构**:
```
┌─────────────────────────────────────────────────────────────┐
│ 班主任工作台 - 2024级计算机2班                                  │
│ 班主任: 李老师                                                │
└─────────────────────────────────────────────────────────────┘
┌─────────────────────────────────────────────────────────────┐
│ 统计卡片区 (4列)                                              │
│ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐       │
│ │ 本周扣分  │ │ 年级排名  │ │ 当前评级  │ │ 待处理    │       │
│ │   8.5    │ │  3/42    │ │  良好    │ │   2项    │       │
│ │ ↓2.5(好转)│ │ ↑2(上升) │ │ ★★★★☆  │ │ 整改+申诉 │       │
│ └──────────┘ └──────────┘ └──────────┘ └──────────┘       │
└─────────────────────────────────────────────────────────────┘
┌────────────────────────────┬────────────────────────────────┐
│ 扣分趋势图 (近4周)          │ 扣分类别分布 (雷达图)            │
│                            │          卫生                  │
│  12 ─                      │      ╱───●───╲               │
│  10 ─  ●                   │   纪律●       ●考勤           │
│   8 ─     ●                │      ╲───●───╱               │
│   6 ─        ●    ●        │         宿舍                  │
│   4 ─                      │                               │
│     第1周 第2周 第3周 第4周   │  我的班级 ── 年级平均 ----     │
└────────────────────────────┴────────────────────────────────┘
┌─────────────────────────────────────────────────────────────┐
│ 高频问题TOP5                                                  │
│ ┌───┬─────────────┬──────┬───────┬────────────────────┐    │
│ │ # │ 问题项       │ 次数  │ 累计扣分 │ 趋势              │    │
│ ├───┼─────────────┼──────┼───────┼────────────────────┤    │
│ │ 1 │ 宿舍地面不洁  │ 5次  │ -10分  │ ████████ 持续问题   │    │
│ │ 2 │ 迟到         │ 3次  │ -6分   │ ██████░░ 有好转     │    │
│ │ 3 │ 物品乱放     │ 3次  │ -3分   │ ██████░░ 有好转     │    │
│ │ 4 │ 被褥未叠     │ 2次  │ -2分   │ ████░░░░ 明显改善   │    │
│ │ 5 │ 课间打闹     │ 1次  │ -2分   │ ██░░░░░░ 新出现     │    │
│ └───┴─────────────┴──────┴───────┴────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
┌────────────────────────────┬────────────────────────────────┐
│ 待处理整改工单               │ 学生违纪关注                    │
│                            │                               │
│ ● 301宿舍地面不洁    紧急    │ ⚠ 张三 - 3次违纪(30天内)       │
│   截止: 1月28日             │   迟到×2, 宿舍卫生×1           │
│   [查看详情] [处理]         │   趋势: 恶化 →                │
│                            │   [查看画像] [记录处理]         │
│ ● 教室黑板未擦       一般   │                               │
│   截止: 1月29日             │ ● 王五 - 2次违纪(30天内)       │
│   [查看详情] [处理]         │   吸烟×1, 打闹×1              │
│                            │   趋势: 新增 ↑                │
│                            │   [查看画像] [记录处理]         │
└────────────────────────────┴────────────────────────────────┘
```

### 9.5 排名结果页面

**布局结构**:
```
┌─────────────────────────────────────────────────────────────┐
│ 检查结果 - 排名总览                                           │
│ 检查计划: 2025-2026学年第二学期日常检查  检查日期: 2026-01-27   │
│ [筛选: 年级▼] [筛选: 系部▼] [排名维度: 总排名▼]  [导出Excel]   │
└─────────────────────────────────────────────────────────────┘
┌─────────────────────────────────────────────────────────────┐
│ 排名前三展示                                                  │
│ ┌──────────────┐ ┌──────────────┐ ┌──────────────┐         │
│ │    🥈 第2名   │ │    🥇 第1名   │ │    🥉 第3名   │         │
│ │  电商1班      │ │  计算机3班    │ │  会计2班      │         │
│ │  扣分: 3.5    │ │  扣分: 2.0    │ │  扣分: 4.0    │         │
│ │  评级: 优秀   │ │  评级: 优秀   │ │  评级: 优秀   │         │
│ │  趋势: →      │ │  趋势: ↑      │ │  趋势: ↑      │         │
│ └──────────────┘ └──────────────┘ └──────────────┘         │
└─────────────────────────────────────────────────────────────┘
┌─────────────────────────────────────────────────────────────┐
│ 完整排名表                                                    │
│ ┌───┬────────────┬──────┬──────┬──────┬──────┬─────┬─────┐│
│ │排名│ 班级       │ 系部  │ 扣分  │ 加分  │最终分│ 评级 │ 趋势││
│ ├───┼────────────┼──────┼──────┼──────┼──────┼─────┼─────┤│
│ │ 1 │ 计算机3班   │信工系│ -2.0 │ +1.0 │ 99.0│ 优秀 │ ↑   ││
│ │ 2 │ 电商1班     │经管系│ -3.5 │ +0.0 │ 96.5│ 优秀 │ →   ││
│ │ 3 │ 会计2班     │财经系│ -4.0 │ +2.0 │ 98.0│ 优秀 │ ↑   ││
│ │...│ ...         │ ...  │ ...  │ ...  │ ... │ ...  │ ... ││
│ │42│ 物流3班      │经管系│-28.5 │ +0.0 │71.5 │ 较差 │ ↓   ││
│ └───┴────────────┴──────┴──────┴──────┴──────┴─────┴─────┘│
│                                                              │
│ 分页: ◀ 1 2 3 ▶   共42个班级                                 │
└─────────────────────────────────────────────────────────────┘
```

### 9.6 数据分析中心

**布局结构**:
```
┌─────────────────────────────────────────────────────────────┐
│ 数据分析中心                                                  │
│ [时间范围: 本月▼] [范围: 全校▼] [对比: 上月▼]                  │
└─────────────────────────────────────────────────────────────┘
┌──────────────┬──────────────┬──────────────┬──────────────┐
│ 检查覆盖率    │ 平均扣分      │ 整改完成率    │ 优秀率        │
│   95.2%      │   12.3分     │   78.5%      │   35.7%      │
│   ↑2.1%      │   ↓1.5分     │   ↑5.2%      │   ↑3.1%      │
└──────────────┴──────────────┴──────────────┴──────────────┘
┌────────────────────────────────────────────────────────────┐
│ 扣分趋势对比图 (折线图)                                       │
│                                                            │
│  全校平均 ── 信工系 ── 经管系 ── 财经系                        │
│  30 ─                                                      │
│  20 ─  ╲                                                   │
│  15 ─    ╲──────●                                          │
│  10 ─           ╲───●───●                                  │
│   5 ─                                                      │
│      W1   W2   W3   W4                                     │
└────────────────────────────────────────────────────────────┘
┌──────────────────────────┬─────────────────────────────────┐
│ 扣分类别分布 (饼图)        │ 高频问题项TOP10 (横向柱状图)       │
│                          │                                 │
│     ┌───┐ 卫生 45%       │ 地面不洁     ████████████ 23次   │
│   ╱     ╲ 纪律 25%       │ 迟到         ████████░░░ 18次   │
│  │  PIE  │ 考勤 15%      │ 物品乱放     ██████░░░░░ 12次   │
│   ╲     ╱ 宿舍 10%       │ 被褥未叠     █████░░░░░░ 10次   │
│     └───┘ 其他  5%       │ 课间打闹     ████░░░░░░░  8次   │
│                          │ ...                              │
└──────────────────────────┴─────────────────────────────────┘
┌────────────────────────────────────────────────────────────┐
│ 系部对比排名 (仅DEPARTMENT级检查)                              │
│ ┌───┬────────────┬──────────┬──────────┬──────────┬──────┐│
│ │排名│ 系部        │ 班级平均分│ 系部独立分│ 综合分    │ 评级 ││
│ ├───┼────────────┼──────────┼──────────┼──────────┼──────┤│
│ │ 1 │ 信息工程系   │ 92.5     │ 95.0     │ 93.3     │ 优秀 ││
│ │ 2 │ 财经系       │ 88.2     │ 90.0     │ 88.8     │ 良好 ││
│ │ 3 │ 经济管理系   │ 85.1     │ 82.0     │ 84.1     │ 良好 ││
│ └───┴────────────┴──────────┴──────────┴──────────┴──────┘│
└────────────────────────────────────────────────────────────┘
┌────────────────────────────────────────────────────────────┐
│ 预警信息                                                     │
│ ⚠ 物流3班 连续3周排名末位，建议关注                             │
│ ⚠ 宿舍卫生类扣分本月上升23%，建议加强检查                       │
│ ⚠ 2号宿舍楼3层检查覆盖率仅60%，存在检查盲区                     │
│ ℹ 整改工单超期5项，涉及3个班级                                  │
└────────────────────────────────────────────────────────────┘
```

### 9.7 整改管理页面（新增）

**布局结构**:
```
┌─────────────────────────────────────────────────────────────┐
│ 整改工单管理                                                  │
│ [全部] [待处理(5)] [进行中(3)] [待复核(2)] [已完成] [已超期(1)] │
│ [筛选: 班级▼] [筛选: 严重程度▼] [筛选: 类别▼]                  │
└─────────────────────────────────────────────────────────────┘
┌─────────────────────────────────────────────────────────────┐
│ ┌──────────────────────────────────────────────────────────┐│
│ │ CA-20260127-001                      🔴 紧急 | 待处理    ││
│ │ 301宿舍地面不洁                                          ││
│ │ 班级: 计算机2班  |  责任人: 李老师  |  截止: 2026-01-28   ││
│ │ 来源: 日常检查 2026-01-27  |  检查员: 张检查员             ││
│ │ ┌──────────────────────────────────────────────────────┐││
│ │ │ 问题照片: [🖼] [🖼]                                  │││
│ │ └──────────────────────────────────────────────────────┘││
│ │ [查看详情] [开始处理] [转派]                              ││
│ └──────────────────────────────────────────────────────────┘│
│                                                              │
│ ┌──────────────────────────────────────────────────────────┐│
│ │ CA-20260126-003                      🟡 一般 | 待复核    ││
│ │ 教室黑板未擦                                              ││
│ │ 班级: 电商1班  |  责任人: 王老师  |  已整改                 ││
│ │ 整改措施: 已安排值日生重新清扫                               ││
│ │ ┌────────────────────────────────────────────────────┐  ││
│ │ │ 整改前: [🖼]        整改后: [🖼]                    │  ││
│ │ └────────────────────────────────────────────────────┘  ││
│ │ [通过复核] [驳回重做]                                     ││
│ └──────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────┘
```

### 9.8 检查配置中心

**布局结构**:
```
┌─────────────────────────────────────────────────────────────┐
│ 检查配置中心                                                  │
│ [模板管理] [权重方案] [评级规则] [调度策略] [整改规则]           │
└─────────────────────────────────────────────────────────────┘

=== 模板管理 Tab ===
┌─────────────────────────────────────────────────────────────┐
│ 检查模板列表                                    [+ 新建模板]   │
│ ┌──────────────────────────────────────────────────────────┐│
│ │ 日常宿舍卫生检查模板                          [启用] [编辑]││
│ │ 编码: TPL-001  |  评分模式: 基准分制(100分)                ││
│ │ 类别: 卫生检查(核验模式), 纪律检查(自由扣分)               ││
│ │ 扣分项: 12项  |  加分项: 3项  |  使用次数: 45              ││
│ └──────────────────────────────────────────────────────────┘│
│ ┌──────────────────────────────────────────────────────────┐│
│ │ 课堂纪律检查模板                              [启用] [编辑]││
│ │ 编码: TPL-002  |  评分模式: 纯扣分制                       ││
│ │ 类别: 课堂纪律(自由扣分), 考勤(自由扣分)                   ││
│ │ 扣分项: 8项  |  使用次数: 23                               ││
│ └──────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────┘

=== 模板编辑 ===
┌─────────────────────────────────────────────────────────────┐
│ 编辑模板: 日常宿舍卫生检查模板                                  │
│                                                              │
│ 模板名称: [日常宿舍卫生检查模板           ]                     │
│ 评分模式: (●)基准分制  ( )纯扣分制  ( )双轨制                   │
│ 基准分:   [100    ]                                          │
│ 检查轮次: [3] 轮   轮次名称: [早检] [午检] [晚检]              │
│                                                              │
│ ─── 检查类别 ──────────────────────────────── [+ 添加类别] ── │
│                                                              │
│ ▼ 卫生检查                                                   │
│   检查模式: (●)逐项核验  ( )自由扣分  ( )两者都支持              │
│   关联空间: (●)宿舍  ( )教室  ( )无                            │
│   参与轮次: [✓]早检 [✓]午检 [ ]晚检                           │
│                                                              │
│   扣分项:                                              [+]   │
│   ┌──┬────────────┬─────────┬──────┬──────────────────────┐ │
│   │# │ 名称        │ 扣分模式 │ 分值  │ 选项                 │ │
│   ├──┼────────────┼─────────┼──────┼──────────────────────┤ │
│   │1 │ 地面不洁     │ 固定扣分 │ -2.0 │ ☑拍照 ☑备注 ☐学生   │ │
│   │2 │ 被褥未叠     │ 按人扣分 │ -0.5/人│☑拍照 ☑备注 ☑学生  │ │
│   │3 │ 违规电器     │ 固定扣分 │ -5.0 │ ☑拍照 ☑备注 ☑学生   │ │
│   │4 │ 物品摆放     │ 区间扣分 │ 0~3  │ ☑拍照 ☑备注 ☐学生   │ │
│   └──┴────────────┴─────────┴──────┴──────────────────────┘ │
│                                                              │
│   加分项 (仅双轨制):                                    [+]   │
│   ┌──┬────────────┬─────────┬──────────────────────────────┐│
│   │1 │ 主动整改     │ 固定加分 │ +2.0                        ││
│   │2 │ 连续优秀     │ 递进加分 │ 连续3周+1, 5周+2, 8周+3     ││
│   └──┴────────────┴─────────┴──────────────────────────────┘│
│                                                              │
│ ▶ 纪律检查 (折叠)                                            │
│ ▶ 考勤检查 (折叠)                                            │
│                                                              │
│ [保存草稿]  [发布模板]  [取消]                                 │
└─────────────────────────────────────────────────────────────┘
```

---

## 10. 实施路线图

### Phase 1: 架构基础 + 核心执行重构

**目标**: 聚合拆分、物理空间录入、逐项核验模式

**后端**:
- [ ] 创建 `inspection_sessions` 表和领域模型
- [ ] 创建 `class_inspection_records` 表（独立聚合）
- [ ] 创建 `checklist_responses` 表
- [ ] 实现 `SpaceToOrgResolver` 物理空间解析服务
- [ ] 实现物理空间录入API (`/space-deductions`)
- [ ] 实现人员搜索录入API (`/person-deductions`)
- [ ] 实现逐项核验API (`/checklist-responses`)
- [ ] 保留现有组织模式录入API（兼容）

**前端**:
- [ ] 检查执行主页面（四种录入模式切换）
- [ ] 物理空间模式 — 楼层网格导航
- [ ] 人员搜索模式 — 学生搜索+批量选择
- [ ] 逐项核验模式 — 检查清单响应
- [ ] 快速扣分面板组件

### Phase 2: 多级检查 + 评分升级

**目标**: 系部级检查、双轨积分制、班主任工作台

**后端**:
- [ ] `check_plans` 增加 `inspection_level` 字段
- [ ] 实现系部级检查结果聚合（班级平均→系部得分）
- [ ] 实现双轨积分制（基准分+扣分+加分）
- [ ] `bonus_items` 和 `inspection_bonuses` 表
- [ ] 班主任工作台API（overview, deductions, top-issues等）
- [ ] 系部排名API

**前端**:
- [ ] 班主任工作台全部页面
- [ ] 排名结果页面（支持多维度切换）
- [ ] 系部排名视图
- [ ] 模板编辑器（支持评分模式、加分项配置）

### Phase 3: 整改闭环 + 学生行为

**目标**: 整改工单系统、学生行为独立管理

**后端**:
- [ ] `corrective_actions` 表和领域模型
- [ ] `auto_action_rules` 自动创建规则
- [ ] 整改工单完整生命周期API
- [ ] `student_behavior_records` 表和领域模型
- [ ] `student_behavior_alerts` 预警系统
- [ ] 检查扣分→学生行为自动关联事件

**前端**:
- [ ] 整改工单管理页面
- [ ] 学生行为记录页面
- [ ] 学生行为画像页面
- [ ] 行为预警仪表盘

### Phase 4: 自动调度 + 数据分析

**目标**: 自动排检引擎、高级分析

**后端**:
- [ ] `schedule_policies` 表和调度引擎
- [ ] 周期性调度（Spring Scheduler + cron）
- [ ] 随机调度（带约束的随机算法）
- [ ] 触发式调度（条件监控）
- [ ] 检查员轮转算法
- [ ] 高级分析API（趋势、归因、对比、预警）

**前端**:
- [ ] 调度策略配置页面
- [ ] 数据分析中心全部图表
- [ ] 预警信息面板
- [ ] 系部对比分析视图

### Phase 5: 事件驱动 + 性能优化

**目标**: 架构升级、性能优化

**后端**:
- [ ] Spring Events 领域事件基础设施
- [ ] 申诉重算改为事件驱动Saga
- [ ] CQRS读模型投影（排名/统计异步更新）
- [ ] 权重双模式（快照+实时）
- [ ] 大规模数据优化（分区表、读写分离考虑）

---

## 附录A: 设计决策记录

### A.1 为什么选择逐项核验+自由扣分双模式而非纯核验

纯核验模式（SafetyCulture风格）要求每项都回答，适合标准化流程但不适合灵活的纪律检查场景。中国学校量化检查场景多样（宿舍卫生=标准化 vs 迟到抓违纪=灵活），双模式覆盖两种场景。

### A.2 为什么保留纯扣分制作为可选项

部分学校已习惯纯扣分制且运行良好，强制切换基准分制会造成使用习惯断裂。通过`scoringMode`配置让学校自行选择，渐进式迁移。

### A.3 为什么整改工单是独立上下文而非检查执行的子模块

整改的生命周期（可能持续数天）与检查的生命周期（通常当天完成）不同，责任人（班主任）与检查执行者（检查员）不同，且整改可能来自多个来源（检查发现、申诉结果、管理员手动创建），独立上下文更符合DDD的限界上下文原则。

### A.4 为什么学生行为是独立限界上下文

学生行为数据有独立的消费者（班主任、辅导员、家长），独立的生命周期（从记录到干预到解决），独立的分析维度（个人画像、预警、干预效果评估）。将其耦合在检查上下文中会导致职责过重。

### A.5 关于移动端的设计预留

虽然本期只做Web端，但API设计已考虑移动端需求:
- 物理空间API返回的数据结构适配移动端网格布局
- 学生搜索API支持增量搜索（输入一个字即返回候选）
- 逐项核验API支持单项提交（离线场景下可逐条同步）
- 照片上传API支持分片上传（移动网络不稳定）

---

## 附录B: 与现有系统的兼容性

### B.1 数据迁移策略

| 现有表 | V4变化 | 迁移策略 |
|--------|--------|----------|
| daily_checks | 拆分为 inspection_sessions | 新数据写新表，历史数据保留在旧表，提供统一查询视图 |
| daily_check_details | 拆分为 class_inspection_records + deductions | 同上 |
| check_records_new | 保留，新增字段 | ALTER TABLE 新增 bonus_total, final_score, pass_rate |
| check_record_class_stats_new | 保留，新增字段 | ALTER TABLE 新增相关字段 |
| check_plans | 保留，新增字段 | ALTER TABLE 新增 inspection_level, scoring_mode, base_score |
| 所有配置表 | 保留不变 | 无需迁移 |

### B.2 API兼容策略

- V1 API（`/quantification/*`）保留，继续使用旧表
- V2 API（`/api/v2/inspection-*`）使用新架构
- 前端渐进式切换：新页面用V2 API，旧页面保持V1 API
- 统一查询层可同时查询新旧数据
