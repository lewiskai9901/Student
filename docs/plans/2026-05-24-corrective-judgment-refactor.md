# 整改判定体系彻底重构 — 分层模型

> **For Claude:** 这是用户验收的设计方向，本会话内分 Phase 推完。

**Goal:** 把"全项目一刀切的整改判定"重构为"题目层 + 项目层"分层模型，消除当前 7 大缺陷。

**Architecture:**
- **题目层 (TemplateItem.itemRule)** — 业务专家配："这道题答错有多严重、是否红线、是否排除"
- **项目层 (ProjectCorrectivePolicy)** — 项目运营配："本项目整体松紧 + 自动建单门槛"，**解耦"严格度"和"建单流程"**
- **复发增强** — 多信号融合
- **三态错误** — N/A / 没填 / 脏数据 分开报警

**Tech Stack:** Spring Boot + MyBatis Plus + MySQL + Vue 3 + Element Plus

---

## 数据模型变更

### 1. `insp_template_items` 加 `item_rule` JSON 列

```json
{
  "criticality": "NORMAL|RED",       // RED=红线题, 一旦不通过强制 HIGH
  "neverCorrect": false,             // true=该题永不建整改单 (e.g. 备注题)
  "baseSeverityMap": {                // 响应值→严重度的显式映射 (优先于 normalizer)
    "FAIL": "HIGH",
    "D": "HIGH",
    "C": "MEDIUM"
  },
  "deadlineOverrideDays": null       // 该题的整改时限覆盖 (null=用项目默认)
}
```

### 2. `insp_projects` 整改字段重构

旧（删除）:
```
corrective_strictness  STRICT/NORMAL/LENIENT/OFF   ← 耦合判定+流程
corrective_threshold_high/medium/low                ← 项目级单一阈值
corrective_deadline_high/medium/low                 ← OK 保留
```

新（追加）:
```
corrective_enabled            TINYINT       — 总开关 (取代 OFF)
corrective_strictness_adj     INT          — 整体严格度偏移 -2~+2, 默认 0 (升降一档)
corrective_auto_create_level  VARCHAR(10)  — 'HIGH'/'MEDIUM'/'LOW'/'NONE' 自动建单门槛
```

回填策略:
| 旧 strictness | enabled | strictness_adj | auto_create_level |
|---|---|---|---|
| STRICT | 1 | +1 | 'LOW' (LOW 及以上都自动) |
| NORMAL | 1 | 0 | 'NONE' (一律候选) |
| LENIENT | 1 | -1 | 'NONE' |
| OFF | 0 | 0 | 'NONE' |

### 3. (跳过) 分区级 baseline

P3 阶段做。当前 P1+P2 足够覆盖 80% 场景。

---

## 判定算法（新）

```java
CorrectionVerdict judge(detail, project, item, recurrenceCount):
    // 1. 总开关
    if !project.correctiveEnabled: return NONE("disabled")

    // 2. 题目硬规则
    if item.itemRule.neverCorrect: return NONE("neverCorrect")

    // 3. baseSeverity 来源 (优先级):
    //    a. itemRule.baseSeverityMap[responseValue]
    //    b. normalizer 算 sev → 项目默认阈值 classify
    Severity baseSev = ...

    // 4. 红线强制
    if item.itemRule.criticality == RED && baseSev != NONE:
        baseSev = HIGH

    // 5. 复发增强 (有 recurrenceCount 输入)
    if recurrenceCount >= 3: baseSev = baseSev.escalateOne()

    // 6. 项目级强度调档
    finalSev = baseSev.shift(project.strictnessAdjustment)

    // 7. 自动建单门槛
    autoCreate = finalSev >= project.autoCreateLevel

    // 8. deadline (item override > project preset)
    deadline = item.itemRule.deadlineOverride OR project.deadlines.forSeverity(finalSev)

    return verdict(finalSev, autoCreate, deadline, trace)
```

---

## Phase 推进顺序

- **P1**: DB 迁移 + Domain 改造 (ItemRule 持久化 + ProjectCorrectivePolicy 字段)
- **P2**: CorrectionEngine 重写按新算法 + 测试
- **P3**: 前端 UI: ItemEditor 加「整改规则」+ 项目设置「整改判定策略」拆字段
- **P4**: 启动验证 + 单测全绿

不变量守护:
- enabled=false 时其他字段忽略 (UI 灰)
- auto_create_level='LOW' 蕴含 LOW/MEDIUM/HIGH 都自动建单
- itemRule.baseSeverityMap 优先于 normalizer
- 旧数据迁移幂等可重跑

兼容性: 完全 backward-compat — 旧 STRICT/NORMAL/LENIENT 通过迁移自动映射到新字段, 老 API 仍接受 strictness 字符串.
