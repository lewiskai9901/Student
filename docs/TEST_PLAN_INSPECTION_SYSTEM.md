# 检查系统多角色功能测试方案

> **版本**: 1.0
> **日期**: 2026-01-31
> **目标**: 完整测试检查系统所有功能，确保每个模块留有测试数据

---

## 一、测试角色与账号

| 角色 | 用户名 | 密码 | 权限范围 | 测试职责 |
|------|--------|------|----------|----------|
| 系统管理员 | admin | admin123 | 全部数据 | 模板管理、计划管理、发布、统计 |
| 检查员 | inspector01 | 123456 | 本部门及以下 | 创建会话、执行检查、记录扣分 |
| 班主任 | teacher01 | 123456 | 仅本班级 | 查看成绩、提交申诉、整改 |
| 学生 | student01 | 123456 | 仅本人 | 查看个人记录、提交申诉 |
| 部门主管 | dept_manager | 123456 | 本部门及以下 | 一级审核申诉 |

---

## 二、测试流程概览

```
阶段1: 管理员 - 基础配置
    ├── 1.1 创建检查模板
    ├── 1.2 配置扣分项和类别
    └── 1.3 发布模板

阶段2: 管理员 - 计划管理
    ├── 2.1 创建检查计划
    └── 2.2 配置计划参数

阶段3: 检查员 - 执行检查
    ├── 3.1 创建检查会话
    ├── 3.2 记录扣分（按空间）
    ├── 3.3 记录扣分（按人员）
    ├── 3.4 提交会话
    └── 3.5 管理员发布会话

阶段4: 班主任 - 查看与申诉
    ├── 4.1 查看班级检查结果
    ├── 4.2 提交申诉
    └── 4.3 跟踪申诉状态

阶段5: 管理员 - 申诉审核
    ├── 5.1 一级审核
    └── 5.2 二级审核（最终审核）

阶段6: 管理员 - 统计分析
    ├── 6.1 查看排名结果
    ├── 6.2 数据分析中心
    ├── 6.3 评级统计
    └── 6.4 导出数据

阶段7: 整改管理
    ├── 7.1 创建整改工单
    └── 7.2 完成整改验收
```

---

## 三、详细测试步骤

### 阶段1: 管理员 - 基础配置

#### 1.1 创建检查模板

**操作路径**: 量化检查 → 量化配置 → 模板管理 → 新建模板

**测试数据**:
```yaml
模板1 - 宿舍卫生检查:
  templateCode: DORM-HYGIENE-2026
  templateName: 2026年宿舍卫生检查模板
  scoringMode: DEDUCTION_ONLY
  baseScore: 100

模板2 - 课堂纪律检查:
  templateCode: CLASS-DISCIPLINE-2026
  templateName: 2026年课堂纪律检查模板
  scoringMode: BASE_SCORE
  baseScore: 100
```

#### 1.2 配置扣分项和类别

**操作路径**: 点击模板 → 编辑 → 添加类别 → 添加扣分项

**测试数据 - 宿舍卫生模板**:
```yaml
类别1 - 卫生:
  categoryCode: HYGIENE
  categoryName: 卫生检查
  weight: 0.6
  扣分项:
    - itemCode: FLOOR
      itemName: 地面不洁
      deductionScore: 2
    - itemCode: BED
      itemName: 床铺不整
      deductionScore: 1
    - itemCode: TRASH
      itemName: 垃圾未清理
      deductionScore: 2

类别2 - 安全:
  categoryCode: SAFETY
  categoryName: 安全检查
  weight: 0.4
  扣分项:
    - itemCode: ELECTRIC
      itemName: 违规电器
      deductionScore: 5
    - itemCode: FIRE
      itemName: 消防隐患
      deductionScore: 10
```

**测试数据 - 课堂纪律模板**:
```yaml
类别1 - 纪律:
  categoryCode: DISCIPLINE
  categoryName: 课堂纪律
  weight: 0.7
  扣分项:
    - itemCode: LATE
      itemName: 迟到
      deductionScore: 1
    - itemCode: ABSENT
      itemName: 缺勤
      deductionScore: 3
    - itemCode: PHONE
      itemName: 使用手机
      deductionScore: 2

类别2 - 仪表:
  categoryCode: APPEARANCE
  categoryName: 仪容仪表
  weight: 0.3
  扣分项:
    - itemCode: UNIFORM
      itemName: 未穿校服
      deductionScore: 1
    - itemCode: BADGE
      itemName: 未佩戴校牌
      deductionScore: 1
```

#### 1.3 发布模板

**操作**: 点击模板 → 发布

**预期结果**: 模板状态变为"已发布"，可在创建计划时选择

---

### 阶段2: 管理员 - 计划管理

#### 2.1 创建检查计划

**操作路径**: 量化检查 → 检查计划 → 新建计划

**测试数据**:
```yaml
计划1 - 宿舍日常检查:
  planCode: PLAN-DORM-202602
  planName: 2026年2月宿舍日常检查计划
  templateId: (选择宿舍卫生模板)
  startDate: 2026-02-01
  endDate: 2026-02-28
  description: 2月份宿舍卫生日常检查

计划2 - 课堂纪律抽查:
  planCode: PLAN-CLASS-202602
  planName: 2026年2月课堂纪律检查计划
  templateId: (选择课堂纪律模板)
  startDate: 2026-02-01
  endDate: 2026-02-28
  description: 2月份课堂纪律抽查
```

#### 2.2 配置计划参数

**操作**: 进入计划详情，配置检查参数

---

### 阶段3: 检查员 - 执行检查

#### 3.1 创建检查会话

**切换账号**: 登录 inspector01

**操作路径**: 量化检查 → 检查计划 → 选择计划 → 新建检查(V4)

**测试数据 - 宿舍检查会话**:
```yaml
会话1:
  checkDate: 2026-02-01
  checkName: 2月1日宿舍卫生检查
  inputMode: SPACE_FIRST (按空间优先)
  scoringMode: DEDUCTION_ONLY
  baseScore: 100
  inspectionLevel: CLASS
```

**测试数据 - 课堂检查会话**:
```yaml
会话2:
  checkDate: 2026-02-01
  checkName: 2月1日课堂纪律检查
  inputMode: ORG_FIRST (按班级优先)
  scoringMode: BASE_SCORE
  baseScore: 100
  inspectionLevel: CLASS
```

#### 3.2 记录扣分（按空间 - 宿舍检查）

**操作路径**: 进入会话 → 执行检查

**测试数据**:
```yaml
扣分记录1:
  spaceType: DORMITORY
  spaceName: 1号楼101室
  扣分项:
    - itemName: 地面不洁
      deductionScore: 2
      remark: 地面有纸屑
    - itemName: 床铺不整
      deductionScore: 1
      remark: 3床被子未叠

扣分记录2:
  spaceType: DORMITORY
  spaceName: 1号楼102室
  扣分项:
    - itemName: 垃圾未清理
      deductionScore: 2
      remark: 垃圾桶已满
    - itemName: 违规电器
      deductionScore: 5
      remark: 发现电热毯

扣分记录3:
  spaceType: DORMITORY
  spaceName: 2号楼201室
  扣分项:
    - itemName: 地面不洁
      deductionScore: 2
      remark: 有水渍
```

#### 3.3 记录扣分（按班级 - 课堂检查）

**测试数据**:
```yaml
班级扣分1:
  className: 2024级软件1班
  扣分项:
    - itemName: 迟到
      deductionScore: 1
      personCount: 2
      remark: 2人迟到5分钟
    - itemName: 使用手机
      deductionScore: 2
      personCount: 1
      remark: 上课玩手机

班级扣分2:
  className: 2024级软件2班
  扣分项:
    - itemName: 缺勤
      deductionScore: 3
      personCount: 1
      remark: 无故缺勤
    - itemName: 未穿校服
      deductionScore: 1
      personCount: 3
      remark: 3人未穿校服
```

#### 3.4 提交会话

**操作**: 检查员点击"提交会话"

**预期结果**: 会话状态变为 SUBMITTED

#### 3.5 管理员发布会话

**切换账号**: 登录 admin

**操作路径**: 进入会话详情 → 发布

**预期结果**:
- 会话状态变为 PUBLISHED
- 班级分数计算完成
- 生成排名数据

---

### 阶段4: 班主任 - 查看与申诉

#### 4.1 查看班级检查结果

**切换账号**: 登录 teacher01

**操作路径**: 量化检查 → 班主任工作台

**验证内容**:
- 查看所管班级的检查记录
- 查看扣分明细
- 查看班级排名

#### 4.2 提交申诉

**操作路径**: 班主任工作台 → 检查记录 → 申诉

**测试数据**:
```yaml
申诉1:
  appealType: DEDUCTION (扣分申诉)
  targetDeduction: 违规电器扣分
  reason: 该电器为学校统一配发的电热水壶，非违规电器
  attachments: (可选上传证明图片)

申诉2:
  appealType: DEDUCTION
  targetDeduction: 迟到扣分
  reason: 当天有班级活动，学生经辅导员批准晚到
  attachments: (可选上传批准单)
```

#### 4.3 跟踪申诉状态

**操作路径**: 量化检查 → 申诉管理 → 我的申诉

**验证内容**: 申诉状态显示为"待审核"

---

### 阶段5: 管理员 - 申诉审核

#### 5.1 一级审核

**切换账号**: 登录 admin (或 dept_manager)

**操作路径**: 量化检查 → 申诉管理 → 待审核

**测试操作**:
```yaml
申诉1处理:
  action: 通过
  comment: 经核实确为学校配发设备，同意申诉

申诉2处理:
  action: 驳回
  comment: 未提供有效的批准证明
```

#### 5.2 二级审核（最终审核）

**操作路径**: 申诉管理 → 待二级审核

**测试操作**:
```yaml
申诉1最终处理:
  action: 最终通过
  adjustedScore: 0 (取消该扣分)
  comment: 申诉有效，恢复班级分数
```

**预期结果**:
- 申诉状态变为 APPROVED
- 班级分数自动更新
- 排名重新计算

---

### 阶段6: 管理员 - 统计分析

#### 6.1 查看排名结果

**切换账号**: 登录 admin

**操作路径**: 量化检查 → 排名结果

**验证内容**:
- 班级排名列表
- 分数变化趋势
- 申诉影响标记

#### 6.2 数据分析中心

**操作路径**: 量化检查 → 数据分析

**验证内容**:
- 扣分类型分布图
- 部门对比分析
- 时间趋势分析

#### 6.3 评级统计

**操作路径**: 量化检查 → 评级统计中心

**验证内容**:
- 评级规则应用
- 班级评级结果
- 评优名单

#### 6.4 导出数据

**操作路径**: 量化检查 → 导出中心

**测试操作**:
```yaml
导出任务1:
  exportType: 检查记录导出
  dateRange: 2026-02-01 ~ 2026-02-28
  format: Excel

导出任务2:
  exportType: 排名结果导出
  dateRange: 2026-02-01 ~ 2026-02-28
  format: PDF
```

---

### 阶段7: 整改管理

#### 7.1 创建整改工单

**操作路径**: 量化检查 → 整改工单 → 新建工单

**测试数据**:
```yaml
整改工单1:
  source: 检查发现
  relatedSession: 2月1日宿舍卫生检查
  issueDescription: 1号楼102室发现违规电器
  responsibleClass: 2024级软件1班
  severity: HIGH
  deadline: 2026-02-03
  requiredAction: 收缴违规电器，提交整改报告
```

#### 7.2 完成整改验收

**操作**:
1. 班主任提交整改报告
2. 管理员验收确认

---

## 四、测试数据汇总

测试完成后，系统中应存在以下数据：

### 4.1 模板数据
| 模板编码 | 模板名称 | 状态 |
|---------|---------|------|
| DORM-HYGIENE-2026 | 2026年宿舍卫生检查模板 | 已发布 |
| CLASS-DISCIPLINE-2026 | 2026年课堂纪律检查模板 | 已发布 |

### 4.2 计划数据
| 计划编码 | 计划名称 | 状态 |
|---------|---------|------|
| PLAN-DORM-202602 | 2026年2月宿舍日常检查计划 | 进行中 |
| PLAN-CLASS-202602 | 2026年2月课堂纪律检查计划 | 进行中 |

### 4.3 会话数据
| 会话名称 | 检查日期 | 状态 | 记录数 |
|---------|---------|------|--------|
| 2月1日宿舍卫生检查 | 2026-02-01 | 已发布 | 3条扣分 |
| 2月1日课堂纪律检查 | 2026-02-01 | 已发布 | 2条扣分 |

### 4.4 申诉数据
| 申诉内容 | 申诉人 | 状态 |
|---------|--------|------|
| 违规电器扣分申诉 | teacher01 | 已通过 |
| 迟到扣分申诉 | teacher01 | 已驳回 |

### 4.5 整改工单
| 工单内容 | 责任班级 | 状态 |
|---------|---------|------|
| 违规电器整改 | 2024级软件1班 | 已完成 |

### 4.6 导出任务
| 任务类型 | 格式 | 状态 |
|---------|------|------|
| 检查记录导出 | Excel | 已完成 |
| 排名结果导出 | PDF | 已完成 |

---

## 五、验收标准

### 5.1 功能验收
- [ ] 模板创建、编辑、发布功能正常
- [ ] 计划创建、配置功能正常
- [ ] 会话创建、执行、提交、发布流程完整
- [ ] 扣分记录（按空间/按班级）功能正常
- [ ] 申诉创建、审核流程完整
- [ ] 排名计算正确
- [ ] 数据分析图表正常显示
- [ ] 导出功能正常

### 5.2 权限验收
- [ ] 检查员只能执行检查，不能发布
- [ ] 班主任只能查看本班数据
- [ ] 学生只能查看个人相关数据
- [ ] 管理员拥有全部权限

### 5.3 数据验收
- [ ] 每个模块都有测试数据
- [ ] 数据状态流转正确
- [ ] 分数计算准确
- [ ] 申诉影响正确反映

---

## 六、测试执行顺序

```
1. 管理员登录 → 创建模板 → 配置扣分项 → 发布模板
2. 管理员 → 创建计划
3. 切换检查员 → 创建会话 → 执行检查 → 提交
4. 切换管理员 → 发布会话
5. 切换班主任 → 查看结果 → 提交申诉
6. 切换管理员 → 审核申诉
7. 管理员 → 查看统计 → 导出数据
8. 管理员 → 创建整改工单 → 验收整改
```

---

**文档版本**: 1.0
**最后更新**: 2026-01-31
