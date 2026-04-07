# 校历 × 排课一体化重构设计

> **状态**: 待实施
> **日期**: 2026-04-07
> **范围**: 校历重构 + 排课联动 + 实况课表 + 课时统计

---

## 一、核心模型：三层架构

```
┌────────────────────────────────────────────────────────┐
│  基准层 (Master)                                        │
│  schedule_entries — 排课引擎生成的理论课表                 │
│  "如果不发生任何意外，每周应该怎么上课"                      │
│  学期内不变，只有重新排课才会修改                           │
└────────────────────────┬───────────────────────────────┘
                         │ 学期开始时自动展开
                         ▼
┌────────────────────────────────────────────────────────┐
│  实况层 (Live)                                          │
│  schedule_instances — 每一天每一节的具体实例               │
│  "4月7日周一第1-2节，实际上什么课"                         │
│  自动计算 = 基准展开 + 校历事件 + 调课覆盖                 │
└────────────────────────┬───────────────────────────────┘
                         ▲ 叠加影响
┌────────────────────────┴───────────────────────────────┐
│  事件层 (Events)                                        │
│  校历事件 — 放假/运动会/考试周/补课日/停课                  │
│  调课记录 — 调课/停课/补课/代课                            │
│  作息表 — 节次时间配置（全校/学期维度）                     │
└────────────────────────────────────────────────────────┘
```

## 二、数据库设计

### 2.1 新增表: schedule_instances（实况课表）

```sql
CREATE TABLE schedule_instances (
  id BIGINT NOT NULL PRIMARY KEY,
  entry_id BIGINT COMMENT '来源基准条目(补课/临时课无基准时为空)',
  semester_id BIGINT NOT NULL,
  actual_date DATE NOT NULL COMMENT '实际日期',
  weekday TINYINT NOT NULL COMMENT '实际周几 1-7',
  week_number INT COMMENT '第几教学周',
  start_slot INT NOT NULL,
  end_slot INT NOT NULL,
  
  course_id BIGINT NOT NULL,
  class_id BIGINT NOT NULL,
  teacher_id BIGINT COMMENT '实际授课教师(代课时与基准不同)',
  original_teacher_id BIGINT COMMENT '原教师(代课时记录)',
  classroom_id BIGINT COMMENT '实际场所(换教室时与基准不同)',
  
  status TINYINT NOT NULL DEFAULT 0 COMMENT '0正常 1已取消 2已调走 3补课 4代课',
  cancel_reason VARCHAR(200) COMMENT '取消/变更原因',
  source_type TINYINT NOT NULL DEFAULT 0 COMMENT '0基准展开 1调课生成 2补课日生成 3临时加课',
  source_id BIGINT COMMENT '来源ID(调课记录ID/校历事件ID)',
  
  actual_hours DECIMAL(3,1) DEFAULT 1.0 COMMENT '实际课时数',
  remark VARCHAR(500),
  
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  
  INDEX idx_semester_date (semester_id, actual_date),
  INDEX idx_teacher (teacher_id, actual_date),
  INDEX idx_class (class_id, actual_date),
  INDEX idx_classroom (classroom_id, actual_date),
  INDEX idx_course (course_id, semester_id),
  INDEX idx_entry (entry_id),
  INDEX idx_week (semester_id, week_number)
) COMMENT '实况课表(每日每节实例)';
```

### 2.2 扩展表: academic_event

```sql
-- 新增字段
ALTER TABLE academic_event ADD COLUMN affect_type TINYINT DEFAULT 0 
  COMMENT '0无影响 1全天停课 2半天停课 3按指定日课表补课 4考试周';
ALTER TABLE academic_event ADD COLUMN affect_scope VARCHAR(50) DEFAULT 'all'
  COMMENT '影响范围: all/grade:2025/class:123';
ALTER TABLE academic_event ADD COLUMN substitute_weekday TINYINT
  COMMENT '补课日按周几课表上课(affect_type=3时有效)';
ALTER TABLE academic_event ADD COLUMN affect_slots VARCHAR(50)
  COMMENT '影响节次范围(半天停课时: 1-4上午, 5-8下午)';
```

### 2.3 新增表: period_configs（作息表/节次配置）

```sql
CREATE TABLE period_configs (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  semester_id BIGINT NOT NULL,
  config_name VARCHAR(50) NOT NULL DEFAULT '默认作息表',
  periods_per_day INT NOT NULL DEFAULT 8,
  schedule_days JSON NOT NULL DEFAULT '[1,2,3,4,5]' COMMENT '排课日',
  periods JSON NOT NULL COMMENT '节次详情[{period,name,startTime,endTime}]',
  is_default TINYINT DEFAULT 1,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  INDEX idx_semester (semester_id)
) COMMENT '作息表/节次配置(按学期)';
```

## 三、联动机制

### 3.1 实况课表生成（学期初）

```
触发: 排课完成 或 手动点击"生成实况课表"
流程:
  1. 读取基准课表 schedule_entries (WHERE semester_id=?)
  2. 读取学期的教学周列表 academic_weeks
  3. 对每条 entry × 每个教学周:
     - 计算 actual_date = 该周的 weekday 对应日期
     - 检查 week_type (单/双周过滤)
     - 生成 schedule_instance, status=0, source_type=0
  4. 读取校历事件, 叠加影响:
     - 放假日: 对应日期的实例 status=1, cancel_reason=事件名
     - 补课日: 生成新实例, substitute_weekday的课表
```

### 3.2 校历事件变更时

```
事件创建/修改 → CalendarEventListener:
  
  affect_type=1(全天停课):
    UPDATE schedule_instances SET status=1, cancel_reason=?, source_id=?
    WHERE actual_date BETWEEN ? AND ? AND status=0
  
  affect_type=2(半天停课):
    同上但加 start_slot/end_slot 过滤
    
  affect_type=3(补课日):
    1. 查基准课表 WHERE weekday=substitute_weekday
    2. 为补课日期生成新实例, source_type=2
    
  affect_type=4(考试周):
    取消常规课, 考试安排另计
```

### 3.3 调课申请执行时

```
调课审批通过并执行 → AdjustmentExecuteListener:

  调课(type=1):
    1. 原实例 status=2(已调走)
    2. 生成新实例: 新日期+新节次+新教室, source_type=1
    
  停课(type=2):
    原实例 status=1, cancel_reason=调课原因
    
  补课(type=3):
    生成新实例, source_type=1
    
  代课:
    原实例 status=4, teacher_id=代课教师, original_teacher_id=原教师
```

## 四、课时统计 API

```
GET /teaching/statistics/hours?semesterId=X&groupBy=teacher
GET /teaching/statistics/hours?semesterId=X&groupBy=class
GET /teaching/statistics/hours?semesterId=X&groupBy=course
GET /teaching/statistics/hours?semesterId=X&groupBy=classroom
GET /teaching/statistics/hours?semesterId=X&groupBy=teacher&period=week&weekNumber=5
GET /teaching/statistics/hours?semesterId=X&groupBy=teacher&period=month&month=3

响应:
{
  "items": [
    {
      "id": 123, "name": "张老师",
      "totalHours": 40,           // 理论课时(基准)
      "actualHours": 38,          // 实际课时(正常+补课+代课)
      "cancelledHours": 4,        // 被取消课时(放假/停课)
      "substituteHours": 2,       // 补课课时
      "proxyHours": 1,            // 代课课时(替别人上)
      "proxiedHours": 1,          // 被代课课时(别人替自己)
      "adjustedHours": 1,         // 调课课时
      "attendanceRate": "95.0%"   // 实际/理论
    }
  ],
  "summary": {
    "totalTeachers": 20,
    "totalActualHours": 760,
    "avgHoursPerTeacher": 38
  }
}
```

## 五、页面结构

### 5.1 校历管理（重构，tm- 风格）

```
校历中心 CalendarCenter.vue
├── 头部: 学年切换 + 学期切换
│
├── 日常模式 (3 tabs)
│   ├── Tab 1: 学期总览 ← 默认
│   │   ├── 时间轴: 教学周进度条(当前第N周高亮)
│   │   ├── 本周概览: 本周有什么事件/放假
│   │   └── 统计卡片: 总教学周/已过/剩余/放假天数
│   │
│   ├── Tab 2: 校历事件
│   │   ├── 月历视图(可切周视图)
│   │   ├── 事件类型筛选: 放假/考试/活动/补课
│   │   ├── 事件列表(点击编辑)
│   │   └── 事件对排课影响预览
│   │
│   └── Tab 3: 作息表
│       ├── 当前学期的节次配置
│       ├── 预设模板: 6节/8节/10节
│       └── 自定义编辑
│
└── 学年设置 (右上角按钮,抽屉)
    ├── 学年 CRUD
    ├── 学期 CRUD
    └── 教学周生成
```

### 5.2 排课中心（在已重构基础上增强）

```
排课中心 ScheduleCenter.vue (已有，增强)
├── 头部: 学期选择 + [排课设置]
│
├── 日常模式 (4 tabs，新增实况课表)
│   ├── Tab 1: 基准课表 ← 理论课表(原课表视图)
│   ├── Tab 2: 实况课表 ← 新增!
│   │   ├── 日历周视图: 按日期查看，显示每天实际课表
│   │   ├── 状态色块: 正常(白)/取消(红)/补课(绿)/代课(橙)
│   │   └── 点击实例查看详情/变更记录
│   ├── Tab 3: 调课管理
│   └── Tab 4: 课时统计 ← 新增!
│       ├── 维度切换: 教师/班级/课程/场所
│       ├── 周期切换: 全学期/按月/按周
│       └── 导出Excel
│
└── 排课设置 (向导，已有)
    ├── Step 1: 确认作息表 ← 改为读取校历作息表
    ├── Step 2: 数据准备
    ├── Step 3: 约束规则
    ├── Step 4: 执行排课
    └── Step 5: 生成实况 + 发布 ← 增强
```

### 5.3 文件结构

```
views/teaching/
├── CalendarCenter.vue              ← 校历中心(新，替代 AcademicCalendarView)
├── calendar/
│   ├── SemesterOverview.vue        ← Tab1: 学期总览(时间轴+统计)
│   ├── CalendarEvents.vue          ← Tab2: 校历事件(月历+列表)
│   ├── PeriodSettings.vue          ← Tab3: 作息表配置
│   └── YearSetupDrawer.vue         ← 学年设置抽屉
│
├── ScheduleCenter.vue              ← 排课中心(已有，增强)
├── schedule/
│   ├── MasterTimetable.vue         ← Tab1: 基准课表(改名)
│   ├── LiveTimetable.vue           ← Tab2: 实况课表(新)
│   ├── AdjustmentPanel.vue         ← Tab3: 调课(已有)
│   ├── HoursStatistics.vue         ← Tab4: 课时统计(新)
│   ├── DailyExport.vue             ← 导出(已有)
│   ├── setup/                      ← 排课设置向导(已有)
│   └── ...
```

## 六、关键交互设计

### 6.1 校历事件创建时的排课影响预览

```
用户在校历创建"国庆放假 10/1-10/7"
  → 系统自动显示:
    "此事件将影响 42 节课（涉及 12 位教师、6 个班级）"
    [预览受影响课程列表]
    [确认创建]
```

### 6.2 实况课表的周视图

```
┌──────┬─────────┬─────────┬─────────┬─────────┬─────────┐
│      │ 4/7 周一 │ 4/8 周二 │ 4/9 周三 │ 4/10周四 │ 4/11周五│
├──────┼─────────┼─────────┼─────────┼─────────┼─────────┤
│ 1-2  │ 🟢数学  │ 🟢英语  │ 🔴停课  │ 🟢物理  │ 🟠代课  │
│      │ 张老师  │ 李老师  │ 运动会  │ 王老师  │ 赵老师  │
├──────┼─────────┼─────────┼─────────┼─────────┼─────────┤
│ 3-4  │ 🟢英语  │ 🟡补课  │ 🔴停课  │ 🟢语文  │ 🟢计算  │
│      │ 李老师  │ 周老师  │ 运动会  │ 孙老师  │ 钱老师  │
└──────┴─────────┴─────────┴─────────┴─────────┴─────────┘

颜色: 🟢正常 🔴已取消 🟡补课 🟠代课 ⚪已调走
```

### 6.3 课时统计卡片

```
教师课时统计 — 2025-2026 第二学期

┌─────────┬────────┬────────┬──────┬──────┬──────┬────────┐
│ 教师    │ 理论   │ 实际   │ 取消 │ 补课 │ 代课 │ 出勤率 │
├─────────┼────────┼────────┼──────┼──────┼──────┼────────┤
│ 张老师  │ 80     │ 76     │ 6    │ 2    │ 0    │ 95.0%  │
│ 李老师  │ 64     │ 62     │ 4    │ 1    │ 1    │ 96.9%  │
│ 王老师  │ 48     │ 44     │ 6    │ 2    │ 0    │ 91.7%  │
└─────────┴────────┴────────┴──────┴──────┴──────┴────────┘
```

## 七、实施顺序

### Phase 1: 基础设施
1. 创建 schedule_instances 表 + period_configs 表
2. 扩展 academic_event 表字段
3. 后端: 实况课表生成服务 (InstanceGenerationService)
4. 后端: 课时统计 API

### Phase 2: 校历重构
5. CalendarCenter.vue 主框架 (tm- 风格)
6. SemesterOverview — 学期总览时间轴
7. CalendarEvents — 月历事件视图
8. PeriodSettings — 作息表(从排课设置迁移)
9. YearSetupDrawer — 学年/学期管理

### Phase 3: 排课增强
10. 排课设置 Step 1 改为"确认作息表"(读取校历)
11. 排课设置 Step 5 增加"生成实况课表"
12. LiveTimetable — 实况课表周视图
13. HoursStatistics — 课时统计面板

### Phase 4: 联动
14. 校历事件创建时的影响预览
15. 校历事件变更时自动更新实况
16. 调课执行时自动更新实况
17. 代课流程
