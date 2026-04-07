# 排课中心重构设计

> **状态**: 实施中
> **日期**: 2026-04-07
> **目标**: 重构排课中心为"日常模式+向导模式"双模架构

---

## 一、核心设计原则

1. **日常模式为主** — 打开就是课表，最高频操作零点击
2. **向导模式引导** — 学期初排课有清晰步骤，阻断式流程防止遗漏
3. **节次可配置** — 每个学期独立配置几节课、每节什么时间
4. **就地完成** — 向导内每一步都能直接操作，不跳转到其他页面

## 二、页面结构

```
排课中心 ScheduleCenter.vue
├── 头部: 学期选择器 + [排课设置] 按钮
│
├── 日常模式 (默认)
│   ├── Tab 1: 课表视图 (默认选中)
│   ├── Tab 2: 调课管理
│   └── Tab 3: 导出打印
│
└── 排课设置 (全屏 Drawer)
    ├── 左侧: 步骤导航 (vertical stepper)
    │   ├── Step 1: 节次时间 ← 首次必须配置
    │   ├── Step 2: 数据准备 ← 检查+一键生成
    │   ├── Step 3: 约束规则 ← 可选但建议
    │   ├── Step 4: 执行排课 ← 核心操作
    │   └── Step 5: 检查发布 ← 最终确认
    └── 右侧: 当前步骤内容区
```

## 三、节次配置

存储: `system_configs` 表
- key: `schedule.periods`
- value: JSON
- 按学期隔离: 创建学期时继承上一学期配置

```json
{
  "periodsPerDay": 8,
  "scheduleDays": [1, 2, 3, 4, 5],
  "periods": [
    { "period": 1, "name": "第一节", "startTime": "08:00", "endTime": "08:45" },
    { "period": 2, "name": "第二节", "startTime": "08:55", "endTime": "09:40" },
    { "period": 3, "name": "第三节", "startTime": "10:00", "endTime": "10:45" },
    { "period": 4, "name": "第四节", "startTime": "10:55", "endTime": "11:40" },
    { "period": 5, "name": "第五节", "startTime": "14:00", "endTime": "14:45" },
    { "period": 6, "name": "第六节", "startTime": "14:55", "endTime": "15:40" },
    { "period": 7, "name": "第七节", "startTime": "16:00", "endTime": "16:45" },
    { "period": 8, "name": "第八节", "startTime": "16:55", "endTime": "17:40" }
  ]
}
```

## 四、向导步骤详细设计

### Step 1: 节次时间配置
- 可视化表格编辑: 增删节次、修改时间
- 预设模板: "标准8节制"、"10节制(含晚自习)"、"6节制(小学)"
- 配置排课日: 默认周一到周五, 可勾选周六

### Step 2: 数据准备检查
- 自动检测清单:
  - ✅/❌ 开课计划 (N门课)
  - ✅/❌ 教学任务 (N条, M条未分配教师)
  - ✅/❌ 教室/场所 (N间可用)
- 每项旁边有快捷按钮:
  - "从培养方案导入开课" → 调 workflowApi
  - "生成教学任务" → 调 workflowApi
  - "批量分配教师" → 弹出简易分配面板
- 全部 ✅ 后下一步解锁

### Step 3: 约束规则
- 直接内嵌现有 ConstraintConfig 组件
- 提供"推荐约束"一键添加(教师每天不超过6节等常见规则)

### Step 4: 执行排课
- 选择/创建排课方案
- 配置参数(迭代次数、种群大小、变异率)
- 执行 + 进度条
- 结果预览: 迷你课表网格 + 统计(排课率、冲突数)

### Step 5: 检查发布
- 冲突列表(如有)
- 课表预览(班级维度切换)
- [发布课表] 按钮

## 五、文件结构

```
views/teaching/
├── ScheduleCenter.vue              ← 主入口(替代 ScheduleView.vue)
├── schedule/
│   ├── DailyTimetable.vue          ← 日常课表视图(合并原 TimetableViewer)
│   ├── DailyAdjustment.vue         ← 日常调课(复用原 AdjustmentPanel)
│   ├── DailyExport.vue             ← 日常导出(简化)
│   ├── SetupDrawer.vue             ← 排课设置全屏抽屉
│   ├── setup/
│   │   ├── StepPeriodConfig.vue    ← Step 1: 节次配置
│   │   ├── StepDataReady.vue       ← Step 2: 数据准备
│   │   ├── StepConstraints.vue     ← Step 3: 约束(包装 ConstraintConfig)
│   │   ├── StepExecute.vue         ← Step 4: 执行排课
│   │   └── StepPublish.vue         ← Step 5: 检查发布
│   ├── ConstraintConfig.vue        ← 保留(被 Step 3 引用)
│   └── TimetableGrid.vue           ← 保留(被多处引用)
```

## 六、后端新增

### API: 节次配置
```
GET  /teaching/schedule-config?semesterId=X     → 获取节次配置
PUT  /teaching/schedule-config                  → 保存节次配置
POST /teaching/schedule-config/init?semesterId=X → 从上一学期继承
```

### API: 数据就绪检查
```
GET  /teaching/schedule-readiness?semesterId=X  → 返回各项数据状态
{
  offerings: { count: 6, status: 'ready' },
  tasks: { count: 12, withoutTeacher: 3, status: 'warning' },
  classrooms: { count: 15, status: 'ready' },
  constraints: { count: 0, status: 'empty' }
}
```

## 七、实施计划

1. 后端: 节次配置 API + 数据就绪检查 API
2. 前端: ScheduleCenter.vue 主框架 + 日常3个tab
3. 前端: SetupDrawer + 5个步骤组件
4. 清理: 删除旧文件, 更新路由
