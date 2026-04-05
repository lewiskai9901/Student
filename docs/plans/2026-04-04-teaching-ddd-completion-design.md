# 教务模块 DDD 补全 & 工作流串联 设计文档

> **状态**: 待实施  
> **日期**: 2026-04-04  
> **范围**: 考试/成绩/排课领域模型补全 + 工作流编排 + 前端大文件拆分

---

## 一、问题回顾

### 1.1 考试/成绩/排课缺领域模型

当前这三个子领域退化为事务脚本:

| 子领域 | 应用服务行数 | 有聚合根？ | 有领域事件？ |
|--------|-------------|-----------|------------|
| 排课 (Schedule) | AutoSchedulingService 798行 | 无 | 无 |
| 考试 (Exam) | ExamApplicationService 255行 | 无 | 无 |
| 成绩 (StudentGrade) | GradeApplicationService 511行 | 无 | 无 |

业务规则全部堆在 Application Service 中，无法通过领域模型保护不变量。

### 1.2 工作流断裂

教务核心链路 `培养方案 → 开课 → 教学任务 → 排课 → 考试 → 成绩` 中:
- 开课 → 任务: 无直接外键，靠 courseId+classId+semesterId 碰撞
- 排课 → 考试: 完全断裂
- 考试 → 成绩: 仅靠 semesterId+courseId 松散关联

### 1.3 前端大文件

| 文件 | 行数 |
|------|------|
| ScheduleView.vue | 1537 |
| OfferingManagementView.vue | 1025 |
| AcademicCalendarView.vue | 795 |
| ExaminationView.vue | 783 |
| GradeView.vue | 811 |

---

## 二、领域模型补全设计

### 2.1 Schedule (排课) 聚合

```
domain/teaching/model/scheduling/
├── ScheduleEntry.java          ← 聚合根 (对应 schedule_entries 表)
│   - 业务方法: move(), checkConflict(), cancel()
│   - 不变量: 同一 teacher+weekday+slot 不能重叠
├── ScheduleEntryStatus.java    ← 值对象
└── WeekType.java               ← 值对象 (EVERY/ODD/EVEN)

domain/teaching/model/scheduling/
├── ScheduleAdjustment.java     ← 聚合根 (调课)
│   - 业务方法: approve(), reject(), execute()
│   - 状态机: PENDING → APPROVED → EXECUTED / REJECTED

domain/teaching/repository/
├── ScheduleEntryRepository.java
└── ScheduleAdjustmentRepository.java
```

`AutoSchedulingService` 保留为**领域服务** (不是应用服务)，因为排课算法跨越多个聚合。但冲突检测逻辑移入 ScheduleEntry 聚合。

### 2.2 Exam (考试) 聚合

```
domain/teaching/model/exam/
├── ExamBatch.java              ← 聚合根
│   - 包含 List<ExamArrangement> 子实体
│   - 业务方法: publish(), addArrangement(), close()
│   - 状态机: DRAFT → PUBLISHED → ONGOING → FINISHED
├── ExamArrangement.java        ← 实体 (属于 ExamBatch)
│   - 包含 List<ExamRoom> 子实体
│   - assignRoom(), removeRoom()
├── ExamRoom.java               ← 实体
│   - assignInvigilator(), removeInvigilator()
├── ExamInvigilator.java        ← 值对象
└── ExamType.java               ← 值对象 (MIDTERM/FINAL/MAKEUP/RETAKE)

domain/teaching/repository/
└── ExamBatchRepository.java
```

### 2.3 StudentGrade (成绩) 聚合

```
domain/teaching/model/grade/
├── GradeBatch.java             ← 聚合根
│   - 业务方法: submit(), approve(), publish()
│   - 状态机: DRAFT → SUBMITTED → APPROVED → PUBLISHED
├── StudentScore.java           ← 实体 (改名避免与教学 Grade 混淆)
│   - 业务方法: record(), confirm()
│   - 计算逻辑: calculateGradePoint(), calculatePassed()
└── GradeType.java              ← 值对象

domain/teaching/repository/
└── GradeBatchRepository.java
```

### 2.4 领域事件

```
event/
├── ExamBatchPublishedEvent.java
├── GradeBatchPublishedEvent.java
├── ScheduleEntryCreatedEvent.java
└── ScheduleConflictDetectedEvent.java
```

---

## 三、工作流串联设计

### 3.1 显式引用链

```sql
-- 开课 → 教学任务 (新增字段)
ALTER TABLE teaching_tasks ADD COLUMN offering_id BIGINT COMMENT '关联开课计划';
ALTER TABLE teaching_tasks ADD CONSTRAINT fk_task_offering 
    FOREIGN KEY (offering_id) REFERENCES semester_course_offerings(id);

-- 教学任务 → 考试 (新增字段)  
ALTER TABLE exam_arrangements ADD COLUMN task_id BIGINT COMMENT '关联教学任务';

-- 教学任务 → 成绩 (已有 task_id)
-- student_grades.task_id 已存在，确保有外键
```

### 3.2 应用层编排

在 `application/teaching/` 中新增 `TeachingWorkflowService`:

```java
@Service
public class TeachingWorkflowService {
    
    // 从培养方案一键生成开课计划
    public void generateOfferingsFromPlan(Long semesterId, Long planId);
    
    // 从开课计划批量创建教学任务
    public void generateTasksFromOfferings(Long semesterId);
    
    // 从教学任务创建考试安排
    public void generateExamFromTasks(Long batchId, List<Long> taskIds);
    
    // 从考试批次创建成绩批次
    public void generateGradeBatchFromExam(Long examBatchId);
}
```

---

## 四、前端拆分设计

### 4.1 ScheduleView.vue (1537行) 拆分为:

```
views/teaching/schedule/
├── ScheduleOverview.vue        ← Tab 1: 排课概览统计
├── ScheduleManager.vue         ← Tab 2: 课表管理
├── TimetableViewer.vue         ← Tab 3: 课表查看 (含 TimetableGrid)
├── ConflictPanel.vue           ← Tab 4: 冲突检测
├── AdjustmentPanel.vue         ← Tab 5: 调课管理
└── ScheduleView.vue            ← 外壳: Tab 路由 + 学期选择器
```

### 4.2 OfferingManagementView.vue (1025行) 拆分为:

```
views/teaching/offering/
├── OfferingListTab.vue         ← Tab 1: 开课列表
├── ClassAssignmentTab.vue      ← Tab 2: 班级分配
├── TeachingClassTab.vue        ← Tab 3: 教学班管理
└── OfferingManagementView.vue  ← 外壳
```

### 4.3 ExaminationView.vue (783行) 拆分为:

```
views/teaching/exam/
├── ExamBatchList.vue           ← 批次列表
├── ExamArrangementPanel.vue    ← 考试安排
├── ExamRoomPanel.vue           ← 考场分配
└── ExaminationView.vue         ← 外壳
```

### 4.4 GradeView.vue (811行) 拆分为:

```
views/teaching/grade/
├── GradeBatchList.vue          ← 批次管理
├── GradeEntryPanel.vue         ← 成绩录入
├── GradeStatisticsPanel.vue    ← 统计分析
└── GradeView.vue               ← 外壳
```

---

## 五、实施计划

### Phase 1: 领域模型 (预估 2-3 天)
1. 创建 Exam 聚合根 + Repository + Persistence
2. 创建 GradeBatch 聚合根 + Repository + Persistence
3. 创建 ScheduleEntry 聚合根 + Repository + Persistence
4. 迁移 ExamApplicationService → 使用领域模型
5. 迁移 GradeApplicationService → 使用领域模型
6. AutoSchedulingService 重构为领域服务

### Phase 2: 工作流 (预估 1-2 天)
1. 添加 offering_id 到 teaching_tasks 表
2. 实现 TeachingWorkflowService
3. 补充领域事件

### Phase 3: 前端拆分 (预估 2-3 天)
1. ScheduleView 拆分
2. OfferingManagementView 拆分
3. ExaminationView 拆分
4. GradeView 拆分

### 依赖关系
Phase 1 → Phase 2 (领域模型是工作流的基础)
Phase 3 可与 Phase 1/2 并行
