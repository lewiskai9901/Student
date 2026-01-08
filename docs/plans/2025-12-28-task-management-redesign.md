# 任务管理系统重构设计文档

**日期**: 2025-12-28
**版本**: 1.0
**设计目标**: 重构任务管理系统，支持多人任务、卡片式进度展示、按系部两级审批

---

## 📋 需求概述

### 核心需求

1. **任务列表显示优化**
   - 多人任务在列表中只显示1条记录（不是每个执行人1条）
   - 显示任务发布人信息
   - 显示流程配置概览

2. **卡片式进度展示**
   - 类似"电影院选座"的视觉效果
   - 按系部分组显示执行人卡片
   - 卡片显示：执行人姓名、任务状态、审批进度链路

3. **两级审批流程**
   - 班主任提交 → 系领导审核 → 学工处审核
   - 每个系部的领导只审核自己系部的任务
   - 每个班主任的审批流程独立运行（互不阻塞）

4. **灵活的审批配置**
   - 发布任务时按系部分别指定审批人
   - 支持不同系部配置不同的审批人
   - 支持动态添加审批级别

### 使用场景示例

**场景**：学工处领导向2个系部的班主任发布"班会任务"
- **执行人**：计算机系10个班主任 + 机械系10个班主任
- **任务列表**：只显示1条"班会任务"
- **审批配置**：
  - 计算机系：班主任 → 张主任（计算机系领导）→ 王处长（学工处）
  - 机械系：班主任 → 李主任（机械系领导）→ 王处长（学工处）
- **执行过程**：
  - 计算机系班主任A提交后，立即进入张主任审批队列
  - 不需要等其他班主任提交
  - 张主任只能看到计算机系的待审批任务
  - 李主任只能看到机械系的待审批任务

---

## 🗄️ 数据库设计

### 1. Task（任务主表）- 修改

**设计思路**：1个任务对应1条记录，执行人信息移到子表

```sql
CREATE TABLE tasks (
    id BIGINT PRIMARY KEY COMMENT '主键',
    task_code VARCHAR(50) NOT NULL COMMENT '任务编号(TASK-20251228-0001)',
    title VARCHAR(200) NOT NULL COMMENT '任务标题',
    description TEXT COMMENT '任务描述',
    priority TINYINT NOT NULL DEFAULT 2 COMMENT '优先级: 1-紧急, 2-普通, 3-低',

    -- 发布人信息
    assigner_id BIGINT NOT NULL COMMENT '发布人ID',
    assigner_name VARCHAR(50) NOT NULL COMMENT '发布人姓名',

    -- 任务类型
    assign_type TINYINT NOT NULL DEFAULT 1 COMMENT '分配类型: 1-单人任务, 2-多人任务',

    -- 整体状态（根据所有执行人状态计算）
    status TINYINT NOT NULL DEFAULT 0 COMMENT '任务整体状态: 0-待接收, 1-进行中, 2-待审核, 3-已完成, 4-部分打回',

    -- 时间信息
    due_date DATETIME COMMENT '截止时间',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    updated_at DATETIME NOT NULL COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',

    -- 附件（保留）
    attachment_ids JSON COMMENT '任务附件ID列表',

    INDEX idx_task_code (task_code),
    INDEX idx_assigner (assigner_id),
    INDEX idx_status (status),
    INDEX idx_created (created_at)
) COMMENT='任务主表';
```

**关键变更**：
- ❌ 移除：`assigneeId`、`assigneeName`（执行人移到子表）
- ❌ 移除：`departmentId`、`departmentName`（批量任务按系部存储在配置表）
- ❌ 移除：`targetIds`（执行人列表在子表）
- ❌ 移除：`workflowTemplateId`、`processInstanceId`、`currentNode`、`currentApprovers`（流程信息在子表）
- ✅ 新增：`assign_type`（区分单人/多人任务）
- ✅ 调整：`status`语义为任务整体状态

### 2. TaskAssignee（执行人子表）- 扩展

**设计思路**：每个执行人1条记录，记录该执行人的审批配置和状态

```sql
CREATE TABLE task_assignees (
    id BIGINT PRIMARY KEY COMMENT '主键',
    task_id BIGINT NOT NULL COMMENT '任务ID',

    -- 执行人信息
    assignee_id BIGINT NOT NULL COMMENT '执行人ID',
    assignee_name VARCHAR(50) NOT NULL COMMENT '执行人姓名',
    department_id BIGINT COMMENT '执行人所属系部ID',
    department_name VARCHAR(100) COMMENT '执行人所属系部名称',

    -- 该执行人的审批配置（冗余存储，避免关联查询）
    approval_config JSON COMMENT '审批配置: [{level:1, approverId:123, approverName:"张主任"}, ...]',

    -- 执行人状态
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态: 0-待接收, 1-进行中, 2-待审核, 3-已完成, 4-已打回',

    -- 时间信息
    accepted_at DATETIME COMMENT '接收时间',
    submitted_at DATETIME COMMENT '提交时间',
    completed_at DATETIME COMMENT '完成时间（所有审批通过）',

    -- Flowable流程实例ID（每个执行人独立流程）
    process_instance_id VARCHAR(100) COMMENT 'Flowable流程实例ID',
    current_approval_level TINYINT COMMENT '当前审批级别: 1-第一级, 2-第二级...',

    created_at DATETIME NOT NULL COMMENT '创建时间',
    updated_at DATETIME NOT NULL COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',

    INDEX idx_task (task_id),
    INDEX idx_assignee (assignee_id),
    INDEX idx_department (department_id),
    INDEX idx_status (status),
    INDEX idx_process (process_instance_id)
) COMMENT='任务执行人表（支持一对多）';
```

**关键字段**：
- `approval_config`：JSON存储该执行人的审批链路，避免多表关联
  ```json
  [
    {"level": 1, "approverId": 123, "approverName": "张主任"},
    {"level": 2, "approverId": 456, "approverName": "王处长"}
  ]
  ```
- `process_instance_id`：每个执行人启动独立的Flowable流程实例
- `current_approval_level`：快速知道当前审批到第几级

### 3. TaskApprovalConfig（审批配置表）- 新增

**设计思路**：记录任务在不同系部的审批人配置

```sql
CREATE TABLE task_approval_configs (
    id BIGINT PRIMARY KEY COMMENT '主键',
    task_id BIGINT NOT NULL COMMENT '任务ID',
    department_id BIGINT NOT NULL COMMENT '系部ID',
    department_name VARCHAR(100) NOT NULL COMMENT '系部名称',

    -- 审批级别
    approval_level TINYINT NOT NULL COMMENT '审批级别: 1-第一级, 2-第二级, 3-第三级...',

    -- 审批人信息
    approver_id BIGINT NOT NULL COMMENT '审批人ID',
    approver_name VARCHAR(50) NOT NULL COMMENT '审批人姓名',
    approver_role VARCHAR(50) COMMENT '审批人角色（如：系领导、学工处领导）',

    created_at DATETIME NOT NULL COMMENT '创建时间',

    UNIQUE KEY uk_task_dept_level (task_id, department_id, approval_level),
    INDEX idx_task (task_id),
    INDEX idx_department (department_id),
    INDEX idx_approver (approver_id)
) COMMENT='任务审批配置表（按系部分别配置）';
```

**使用示例**：
```
task_id=1, department_id=10(计算机系), level=1, approver_id=123(张主任)
task_id=1, department_id=10(计算机系), level=2, approver_id=456(王处长)
task_id=1, department_id=20(机械系), level=1, approver_id=789(李主任)
task_id=1, department_id=20(机械系), level=2, approver_id=456(王处长)
```

### 4. TaskSubmission（提交记录表）- 保持不变

```sql
-- 无需修改，每个执行人提交1条记录
CREATE TABLE task_submissions (
    id BIGINT PRIMARY KEY,
    task_id BIGINT NOT NULL COMMENT '任务ID',
    assignee_id BIGINT NOT NULL COMMENT '执行人ID（关联TaskAssignee.assignee_id）',
    content TEXT COMMENT '提交内容',
    attachment_ids JSON COMMENT '附件ID列表',
    submitted_at DATETIME NOT NULL COMMENT '提交时间',
    ...
);
```

### 5. TaskApprovalRecord（审批记录表）- 保持不变

```sql
-- 无需修改，每次审批1条记录
CREATE TABLE task_approval_records (
    id BIGINT PRIMARY KEY,
    task_id BIGINT NOT NULL COMMENT '任务ID',
    assignee_id BIGINT NOT NULL COMMENT '执行人ID',
    submission_id BIGINT COMMENT '提交记录ID',

    approval_level TINYINT NOT NULL COMMENT '审批级别',
    approver_id BIGINT NOT NULL COMMENT '审批人ID',
    approver_name VARCHAR(50) NOT NULL COMMENT '审批人姓名',

    approval_status TINYINT NOT NULL COMMENT '审批状态: 0-待审批, 1-通过, 2-打回',
    approval_comment TEXT COMMENT '审批意见',
    approval_time DATETIME COMMENT '审批时间',

    process_instance_id VARCHAR(100) COMMENT 'Flowable流程实例ID',
    flowable_task_id VARCHAR(100) COMMENT 'Flowable任务ID',
    ...
);
```

---

## 🔄 任务发布流程设计

### 创建任务对话框UI设计

```
┌─────────────────────────────────────────────────┐
│ 创建任务                                    [×] │
├─────────────────────────────────────────────────┤
│ 任务标题: [_____________________________]      │
│ 任务描述: [_____________________________]      │
│           [_____________________________]      │
│ 优先级:   ● 紧急  ○ 普通  ○ 低              │
│ 截止时间: [2025-12-30 18:00] 📅                │
│                                                 │
│ ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ │
│ 选择执行人                                      │
│ ┌─────────────────────────────────────────┐   │
│ │ [按部门批量] [手动选择]                  │   │
│ │                                           │   │
│ │ 部门: [计算机系 ▼]  [加载人员]           │   │
│ │                                           │   │
│ │ 已选择: (20人)                            │   │
│ │ ┌─────────────────────────────────────┐ │   │
│ │ │ ☑ 张三 (计算机系)            [×]      │ │   │
│ │ │ ☑ 李四 (计算机系)            [×]      │ │   │
│ │ │ ... (计算机系8人)                     │ │   │
│ │ │ ☑ 孙八 (机械系)              [×]      │ │   │
│ │ │ ... (机械系10人)                      │ │   │
│ │ └─────────────────────────────────────┘ │   │
│ └─────────────────────────────────────────┘   │
│                                                 │
│ ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ │
│ 配置审批流程（按系部）                          │
│                                                 │
│ 【计算机系】(10人)                              │
│ ┌─────────────────────────────────────────┐   │
│ │ 第一级审批: [选择用户▼] 张主任        [×]│   │
│ │ 第二级审批: [选择用户▼] 王处长        [×]│   │
│ │ [+ 添加审批级别]                          │   │
│ └─────────────────────────────────────────┘   │
│                                                 │
│ 【机械系】(10人)                                │
│ ┌─────────────────────────────────────────┐   │
│ │ 第一级审批: [选择用户▼] 李主任        [×]│   │
│ │ 第二级审批: [选择用户▼] 王处长        [×]│   │
│ │ [+ 添加审批级别]                          │   │
│ └─────────────────────────────────────────┘   │
│                                                 │
├─────────────────────────────────────────────────┤
│                    [取消]  [创建任务]           │
└─────────────────────────────────────────────────┘
```

### 后端处理流程

```java
// TaskService.createTask() 逻辑

public void createTask(TaskCreateRequest request) {
    // 1. 创建Task主记录
    Task task = new Task();
    task.setTitle(request.getTitle());
    task.setAssignerId(currentUserId);
    task.setAssignerName(currentUserName);
    task.setAssignType(request.getAssigneeIds().size() > 1 ? 2 : 1);
    task.setStatus(0); // 待接收
    taskMapper.insert(task);

    // 2. 按系部分组执行人
    Map<Long, List<Long>> deptAssigneeMap = groupAssigneesByDepartment(request.getAssigneeIds());

    // 3. 为每个系部保存审批配置
    for (DepartmentApprovalConfig deptConfig : request.getApprovalConfigs()) {
        for (ApprovalLevel level : deptConfig.getLevels()) {
            TaskApprovalConfig config = new TaskApprovalConfig();
            config.setTaskId(task.getId());
            config.setDepartmentId(deptConfig.getDepartmentId());
            config.setDepartmentName(deptConfig.getDepartmentName());
            config.setApprovalLevel(level.getLevel());
            config.setApproverId(level.getApproverId());
            config.setApproverName(level.getApproverName());
            approvalConfigMapper.insert(config);
        }
    }

    // 4. 创建TaskAssignee记录
    for (Map.Entry<Long, List<Long>> entry : deptAssigneeMap.entrySet()) {
        Long deptId = entry.getKey();
        List<Long> assigneeIds = entry.getValue();

        // 获取该系部的审批配置
        List<TaskApprovalConfig> approvalConfigs = approvalConfigMapper.selectByTaskAndDept(task.getId(), deptId);

        for (Long assigneeId : assigneeIds) {
            User user = userService.getById(assigneeId);

            TaskAssignee assignee = new TaskAssignee();
            assignee.setTaskId(task.getId());
            assignee.setAssigneeId(assigneeId);
            assignee.setAssigneeName(user.getName());
            assignee.setDepartmentId(deptId);
            assignee.setDepartmentName(user.getDepartmentName());

            // 将审批配置存储为JSON
            assignee.setApprovalConfig(convertToJson(approvalConfigs));
            assignee.setStatus(0); // 待接收

            taskAssigneeMapper.insert(assignee);
        }
    }

    // 5. 发送通知给所有执行人
    sendTaskNotification(task, request.getAssigneeIds());
}
```

---

## 🎨 任务详情页UI设计

### 整体布局

```
┌─────────────────────────────────────────────────────────┐
│ 任务详情                                          [×]    │
├─────────────────────────────────────────────────────────┤
│ ┌─────────────────────────────────────────────────────┐ │
│ │ 📋 任务信息                                          │ │
│ │ ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ │ │
│ │ 任务标题: 班会任务                                   │ │
│ │ 任务编号: TASK-20251228-0001                         │ │
│ │ 发布人:   王处长 (学工处)                            │ │
│ │ 发布时间: 2025-12-28 09:00                           │ │
│ │ 截止时间: 2025-12-30 18:00                           │ │
│ │ 优先级:   🔴 紧急                                    │ │
│ │ 任务描述: 召开"安全教育"主题班会，提交班会记录...   │ │
│ └─────────────────────────────────────────────────────┘ │
│                                                           │
│ ┌─────────────────────────────────────────────────────┐ │
│ │ 🔄 审批流程配置                                      │ │
│ │ ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ │ │
│ │ 【计算机系】                                         │ │
│ │   班主任提交 → 张主任(系领导) → 王处长(学工处)      │ │
│ │                                                       │ │
│ │ 【机械系】                                           │ │
│ │   班主任提交 → 李主任(系领导) → 王处长(学工处)      │ │
│ └─────────────────────────────────────────────────────┘ │
│                                                           │
│ ┌─────────────────────────────────────────────────────┐ │
│ │ 👥 执行人完成情况                                    │ │
│ │ ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ │ │
│ │                                                       │ │
│ │ 【计算机系】(8/10人完成)                             │ │
│ │ ┌────┬────┬────┬────┬────┬────┬────┬────┬────┬────┐ │ │
│ │ │张三│李四│王五│赵六│钱七│孙八│周九│吴十│郑一│冯二│ │ │
│ │ │✅  │✅  │✅  │✅  │✅  │✅  │⏱  │❌  │✅  │✅  │ │ │
│ │ │完成│完成│完成│完成│完成│完成│审核│待接│完成│完成│ │ │
│ │ │────│────│────│────│────│────│────│────│────│────│ │ │
│ │ │✓提交│✓提交│✓提交│✓提交│✓提交│✓提交│✓提交│    │✓提交│✓提交│ │ │
│ │ │✓系审│✓系审│✓系审│✓系审│✓系审│✓系审│⏱系审│    │✓系审│✓系审│ │ │
│ │ │✓学工│✓学工│✓学工│✓学工│✓学工│✓学工│    │    │✓学工│✓学工│ │ │
│ │ └────┴────┴────┴────┴────┴────┴────┴────┴────┴────┘ │ │
│ │                                                       │ │
│ │ 【机械系】(6/10人完成)                               │ │
│ │ ┌────┬────┬────┬────┬────┬────┬────┬────┬────┬────┐ │ │
│ │ │田三│何四│罗五│萧六│谢七│邹八│柏九│水十│窦一│章二│ │ │
│ │ │✅  │✅  │✅  │✅  │⏱  │❌  │❌  │✅  │⏱  │✅  │ │ │
│ │ │完成│完成│完成│完成│审核│待接│待接│完成│进行│完成│ │ │
│ │ │────│────│────│────│────│────│────│────│────│────│ │ │
│ │ │✓提交│✓提交│✓提交│✓提交│✓提交│    │    │✓提交│✓提交│✓提交│ │ │
│ │ │✓系审│✓系审│✓系审│✓系审│⏱系审│    │    │✓系审│⏱系审│✓系审│ │ │
│ │ │✓学工│✓学工│✓学工│✓学工│    │    │    │✓学工│    │✓学工│ │ │
│ │ └────┴────┴────┴────┴────┴────┴────┴────┴────┴────┘ │ │
│ └─────────────────────────────────────────────────────┘ │
│                                                           │
│ 统计: 总计20人, 已完成14人, 进行中4人, 待接收2人         │
│                                                           │
├─────────────────────────────────────────────────────────┤
│                              [关闭]                       │
└─────────────────────────────────────────────────────────┘
```

### 卡片样式设计（Vue组件）

```vue
<template>
  <div class="assignee-card" :class="statusClass">
    <!-- 执行人姓名 -->
    <div class="assignee-name">{{ assignee.assigneeName }}</div>

    <!-- 状态图标 -->
    <div class="status-icon">
      <CheckCircle v-if="assignee.status === 3" class="text-green-500" />
      <Clock v-else-if="assignee.status === 2" class="text-yellow-500" />
      <Play v-else-if="assignee.status === 1" class="text-blue-500" />
      <Circle v-else class="text-gray-400" />
    </div>

    <!-- 状态文本 -->
    <div class="status-text">{{ statusText }}</div>

    <!-- 审批进度 -->
    <div class="approval-progress">
      <div
        v-for="level in approvalLevels"
        :key="level.level"
        class="approval-step"
        :class="getStepClass(level)"
      >
        <Check v-if="level.approved" class="w-3 h-3" />
        <Clock v-else-if="level.current" class="w-3 h-3" />
        {{ level.name }}
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { CheckCircle, Clock, Play, Circle, Check } from 'lucide-vue-next'

interface AssigneeCardProps {
  assignee: {
    assigneeId: number
    assigneeName: string
    status: number // 0-待接收, 1-进行中, 2-待审核, 3-已完成
    currentApprovalLevel: number
    approvalConfig: Array<{
      level: number
      approverId: number
      approverName: string
    }>
  }
  approvalRecords: Array<{
    approvalLevel: number
    approvalStatus: number
  }>
}

const props = defineProps<AssigneeCardProps>()

const statusText = computed(() => {
  const map = {
    0: '待接收',
    1: '进行中',
    2: '审批中',
    3: '已完成',
    4: '已打回'
  }
  return map[props.assignee.status] || '未知'
})

const statusClass = computed(() => {
  const map = {
    0: 'border-gray-300 bg-gray-50',
    1: 'border-blue-400 bg-blue-50',
    2: 'border-yellow-400 bg-yellow-50',
    3: 'border-green-400 bg-green-50',
    4: 'border-red-400 bg-red-50'
  }
  return map[props.assignee.status] || ''
})

const approvalLevels = computed(() => {
  return props.assignee.approvalConfig.map(config => {
    const record = props.approvalRecords.find(r => r.approvalLevel === config.level)
    return {
      level: config.level,
      name: config.level === 1 ? '系审' : '学工',
      approved: record?.approvalStatus === 1,
      current: props.assignee.currentApprovalLevel === config.level,
      rejected: record?.approvalStatus === 2
    }
  })
})

const getStepClass = (level: any) => {
  if (level.approved) return 'text-green-600'
  if (level.current) return 'text-yellow-600'
  return 'text-gray-400'
}
</script>

<style scoped>
.assignee-card {
  @apply border-2 rounded-lg p-3 text-center transition-all cursor-pointer;
  @apply hover:shadow-md;
  width: 100px;
  min-height: 120px;
}

.assignee-name {
  @apply font-medium text-sm mb-2;
}

.status-icon {
  @apply flex justify-center mb-1;
}

.status-text {
  @apply text-xs text-gray-600 mb-2;
}

.approval-progress {
  @apply text-xs space-y-0.5 border-t pt-2 mt-2;
}

.approval-step {
  @apply flex items-center justify-center gap-1;
}
</style>
```

---

## ⚙️ 审批流程引擎集成

### 方案选择：简化版（不使用Flowable）

**考虑因素**：
- 当前系统已有Flowable集成，但审批流程相对固定（2-3级）
- 使用数据库+状态机可以简化实现
- 降低系统复杂度和维护成本

**推荐方案：基于数据库的审批流程管理**

```java
// 审批流程服务
@Service
public class TaskApprovalService {

    /**
     * 班主任提交任务
     */
    public void submitTask(Long taskId, Long assigneeId, TaskSubmitRequest request) {
        // 1. 查找执行人记录
        TaskAssignee assignee = taskAssigneeMapper.selectByTaskAndAssignee(taskId, assigneeId);

        // 2. 创建提交记录
        TaskSubmission submission = new TaskSubmission();
        submission.setTaskId(taskId);
        submission.setAssigneeId(assigneeId);
        submission.setContent(request.getContent());
        submission.setAttachmentIds(request.getAttachmentIds());
        submissionMapper.insert(submission);

        // 3. 更新执行人状态
        assignee.setStatus(2); // 待审核
        assignee.setSubmittedAt(LocalDateTime.now());
        assignee.setCurrentApprovalLevel(1); // 进入第一级审批
        taskAssigneeMapper.updateById(assignee);

        // 4. 获取第一级审批人
        List<ApprovalConfig> approvalConfigs = JSON.parseArray(assignee.getApprovalConfig(), ApprovalConfig.class);
        ApprovalConfig firstLevel = approvalConfigs.stream()
            .filter(c -> c.getLevel() == 1)
            .findFirst()
            .orElseThrow();

        // 5. 创建待审批记录
        TaskApprovalRecord record = new TaskApprovalRecord();
        record.setTaskId(taskId);
        record.setAssigneeId(assigneeId);
        record.setSubmissionId(submission.getId());
        record.setApprovalLevel(1);
        record.setApproverId(firstLevel.getApproverId());
        record.setApproverName(firstLevel.getApproverName());
        record.setApprovalStatus(0); // 待审批
        approvalRecordMapper.insert(record);

        // 6. 发送审批通知
        sendApprovalNotification(firstLevel.getApproverId(), task, assignee);
    }

    /**
     * 审批人审批
     */
    public void approveTask(Long taskId, Long assigneeId, TaskApproveRequest request) {
        // 1. 查找待审批记录
        TaskApprovalRecord record = approvalRecordMapper.selectPendingRecord(taskId, assigneeId, request.getCurrentUserId());
        if (record == null) {
            throw new BusinessException("没有找到待审批记录");
        }

        // 2. 更新审批记录
        record.setApprovalStatus(request.getApproved() ? 1 : 2); // 1-通过, 2-打回
        record.setApprovalComment(request.getComment());
        record.setApprovalTime(LocalDateTime.now());
        approvalRecordMapper.updateById(record);

        TaskAssignee assignee = taskAssigneeMapper.selectByTaskAndAssignee(taskId, assigneeId);

        if (request.getApproved()) {
            // 通过：进入下一级审批或完成
            handleApprovalPassed(assignee, record);
        } else {
            // 打回：重置到进行中状态
            assignee.setStatus(4); // 已打回
            assignee.setCurrentApprovalLevel(0);
            taskAssigneeMapper.updateById(assignee);

            // 通知执行人
            sendRejectNotification(assigneeId, request.getComment());
        }
    }

    /**
     * 处理审批通过
     */
    private void handleApprovalPassed(TaskAssignee assignee, TaskApprovalRecord currentRecord) {
        // 获取审批配置
        List<ApprovalConfig> approvalConfigs = JSON.parseArray(assignee.getApprovalConfig(), ApprovalConfig.class);
        int maxLevel = approvalConfigs.stream().mapToInt(ApprovalConfig::getLevel).max().orElse(1);

        if (currentRecord.getApprovalLevel() < maxLevel) {
            // 还有下一级审批
            int nextLevel = currentRecord.getApprovalLevel() + 1;
            assignee.setCurrentApprovalLevel(nextLevel);
            taskAssigneeMapper.updateById(assignee);

            // 创建下一级待审批记录
            ApprovalConfig nextConfig = approvalConfigs.stream()
                .filter(c -> c.getLevel() == nextLevel)
                .findFirst()
                .orElseThrow();

            TaskApprovalRecord nextRecord = new TaskApprovalRecord();
            nextRecord.setTaskId(assignee.getTaskId());
            nextRecord.setAssigneeId(assignee.getAssigneeId());
            nextRecord.setSubmissionId(currentRecord.getSubmissionId());
            nextRecord.setApprovalLevel(nextLevel);
            nextRecord.setApproverId(nextConfig.getApproverId());
            nextRecord.setApproverName(nextConfig.getApproverName());
            nextRecord.setApprovalStatus(0);
            approvalRecordMapper.insert(nextRecord);

            // 通知下一级审批人
            sendApprovalNotification(nextConfig.getApproverId(), assignee);
        } else {
            // 所有审批通过，任务完成
            assignee.setStatus(3); // 已完成
            assignee.setCompletedAt(LocalDateTime.now());
            taskAssigneeMapper.updateById(assignee);

            // 检查任务整体状态
            updateTaskOverallStatus(assignee.getTaskId());

            // 通知执行人
            sendCompletionNotification(assignee.getAssigneeId());
        }
    }

    /**
     * 更新任务整体状态
     */
    private void updateTaskOverallStatus(Long taskId) {
        List<TaskAssignee> assignees = taskAssigneeMapper.selectByTaskId(taskId);

        boolean allCompleted = assignees.stream().allMatch(a -> a.getStatus() == 3);
        boolean anyInProgress = assignees.stream().anyMatch(a -> a.getStatus() > 0);

        Task task = taskMapper.selectById(taskId);
        if (allCompleted) {
            task.setStatus(3); // 已完成
        } else if (anyInProgress) {
            task.setStatus(1); // 进行中
        }
        taskMapper.updateById(task);
    }
}
```

### 审批权限控制

```java
// 审批列表查询：系领导只能看到自己系部的待审批任务
public List<TaskApprovalDTO> getMyApprovalList(Long currentUserId) {
    // 1. 查找当前用户作为审批人的待审批记录
    List<TaskApprovalRecord> records = approvalRecordMapper.selectList(
        new LambdaQueryWrapper<TaskApprovalRecord>()
            .eq(TaskApprovalRecord::getApproverId, currentUserId)
            .eq(TaskApprovalRecord::getApprovalStatus, 0) // 待审批
    );

    // 2. 关联查询任务和执行人信息
    return records.stream().map(record -> {
        Task task = taskMapper.selectById(record.getTaskId());
        TaskAssignee assignee = taskAssigneeMapper.selectByTaskAndAssignee(
            record.getTaskId(),
            record.getAssigneeId()
        );

        TaskApprovalDTO dto = new TaskApprovalDTO();
        dto.setTask(task);
        dto.setAssignee(assignee);
        dto.setApprovalRecord(record);
        return dto;
    }).collect(Collectors.toList());
}
```

---

## 🔌 API接口设计

### 1. 创建任务

**请求**：
```http
POST /api/tasks
Authorization: Bearer {token}
Content-Type: application/json

{
  "title": "班会任务",
  "description": "召开安全教育主题班会",
  "priority": 1,
  "dueDate": "2025-12-30T18:00:00",
  "assigneeIds": [101, 102, 103, ..., 120],  // 20个班主任ID
  "approvalConfigs": [
    {
      "departmentId": 10,
      "departmentName": "计算机系",
      "levels": [
        {"level": 1, "approverId": 123, "approverName": "张主任"},
        {"level": 2, "approverId": 456, "approverName": "王处长"}
      ]
    },
    {
      "departmentId": 20,
      "departmentName": "机械系",
      "levels": [
        {"level": 1, "approverId": 789, "approverName": "李主任"},
        {"level": 2, "approverId": 456, "approverName": "王处长"}
      ]
    }
  ],
  "attachmentIds": [1, 2, 3]
}
```

**响应**：
```json
{
  "code": 200,
  "message": "任务创建成功",
  "data": {
    "taskId": 1001,
    "taskCode": "TASK-20251228-0001"
  }
}
```

### 2. 任务列表（只显示1条多人任务）

**请求**：
```http
GET /api/tasks?pageNum=1&pageSize=10&status=1
```

**响应**：
```json
{
  "code": 200,
  "data": {
    "records": [
      {
        "id": 1001,
        "taskCode": "TASK-20251228-0001",
        "title": "班会任务",
        "priority": 1,
        "priorityText": "紧急",
        "status": 1,
        "statusText": "进行中",
        "assignType": 2,
        "assignerName": "王处长",
        "dueDate": "2025-12-30T18:00:00",
        "createdAt": "2025-12-28T09:00:00",

        // 统计信息
        "totalAssignees": 20,
        "completedCount": 14,
        "inProgressCount": 4,
        "pendingCount": 2,
        "completionRate": 70.0
      }
    ],
    "total": 1,
    "pageNum": 1,
    "pageSize": 10
  }
}
```

### 3. 任务详情（含卡片式人员列表）

**请求**：
```http
GET /api/tasks/1001/detail
```

**响应**：
```json
{
  "code": 200,
  "data": {
    "task": {
      "id": 1001,
      "taskCode": "TASK-20251228-0001",
      "title": "班会任务",
      "description": "召开安全教育主题班会",
      "priority": 1,
      "assignerName": "王处长",
      "assignerId": 1,
      "dueDate": "2025-12-30T18:00:00",
      "createdAt": "2025-12-28T09:00:00"
    },

    "approvalFlows": [
      {
        "departmentId": 10,
        "departmentName": "计算机系",
        "flowChain": "班主任提交 → 张主任(系领导) → 王处长(学工处)"
      },
      {
        "departmentId": 20,
        "departmentName": "机械系",
        "flowChain": "班主任提交 → 李主任(系领导) → 王处长(学工处)"
      }
    ],

    "assigneesByDepartment": [
      {
        "departmentId": 10,
        "departmentName": "计算机系",
        "totalCount": 10,
        "completedCount": 8,
        "assignees": [
          {
            "assigneeId": 101,
            "assigneeName": "张三",
            "status": 3,
            "statusText": "已完成",
            "currentApprovalLevel": 0,
            "approvalConfig": [
              {"level": 1, "approverId": 123, "approverName": "张主任"},
              {"level": 2, "approverId": 456, "approverName": "王处长"}
            ],
            "approvalRecords": [
              {"approvalLevel": 1, "approvalStatus": 1, "approvalTime": "2025-12-28T14:00:00"},
              {"approvalLevel": 2, "approvalStatus": 1, "approvalTime": "2025-12-28T16:00:00"}
            ],
            "submittedAt": "2025-12-28T10:00:00",
            "completedAt": "2025-12-28T16:00:00"
          },
          // ... 其他9个班主任
        ]
      },
      {
        "departmentId": 20,
        "departmentName": "机械系",
        "totalCount": 10,
        "completedCount": 6,
        "assignees": [
          // ... 10个班主任
        ]
      }
    ],

    "statistics": {
      "totalAssignees": 20,
      "completedCount": 14,
      "inProgressCount": 4,
      "pendingCount": 2,
      "rejectedCount": 0
    }
  }
}
```

### 4. 提交任务

**请求**：
```http
POST /api/tasks/1001/submit
Authorization: Bearer {token}

{
  "content": "已召开班会，学生参与度高...",
  "attachmentIds": [10, 11]
}
```

**响应**：
```json
{
  "code": 200,
  "message": "任务提交成功，等待系领导审核"
}
```

### 5. 我的待审批列表（系领导视角）

**请求**：
```http
GET /api/tasks/approvals/pending?pageNum=1&pageSize=10
Authorization: Bearer {token}  // 张主任（计算机系领导）
```

**响应**：
```json
{
  "code": 200,
  "data": {
    "records": [
      {
        "taskId": 1001,
        "taskTitle": "班会任务",
        "taskCode": "TASK-20251228-0001",
        "assigneeId": 103,
        "assigneeName": "王五",
        "departmentName": "计算机系",
        "submittedAt": "2025-12-28T11:00:00",
        "content": "已召开班会...",
        "attachmentIds": [10, 11],

        "approvalLevel": 1,
        "myRole": "系领导",

        "recordId": 5001  // 审批记录ID
      },
      // 只能看到计算机系的待审批任务
    ],
    "total": 3
  }
}
```

### 6. 审批任务

**请求**：
```http
POST /api/tasks/approvals/5001/approve
Authorization: Bearer {token}

{
  "approved": true,
  "comment": "班会开展良好，同意通过"
}
```

**响应**：
```json
{
  "code": 200,
  "message": "审批成功，已提交至学工处审核"
}
```

---

## 📊 数据流转示例

### 完整流程演示

**1. 学工处领导创建任务**
```
POST /api/tasks
→ 生成1条Task记录 (id=1001)
→ 生成4条TaskApprovalConfig记录（2个系部 × 2级审批）
→ 生成20条TaskAssignee记录（20个班主任）
→ 发送通知给20个班主任
```

**2. 计算机系班主任张三接收并提交**
```
POST /api/tasks/1001/submit
→ 更新TaskAssignee状态: status=2(待审核), currentApprovalLevel=1
→ 创建TaskSubmission记录
→ 创建TaskApprovalRecord记录: approvalLevel=1, approverId=123(张主任), status=0(待审批)
→ 通知张主任审批
```

**3. 计算机系领导张主任审批**
```
GET /api/tasks/approvals/pending
→ 只查询到approverId=123的待审批记录（计算机系的）
→ 看不到机械系的待审批任务

POST /api/tasks/approvals/5001/approve (approved=true)
→ 更新TaskApprovalRecord: approvalStatus=1(通过)
→ 更新TaskAssignee: currentApprovalLevel=2
→ 创建新的TaskApprovalRecord: approvalLevel=2, approverId=456(王处长)
→ 通知王处长审批
```

**4. 学工处领导王处长审批**
```
GET /api/tasks/approvals/pending
→ 查询到所有approverId=456的待审批记录（所有系部的第二级审批）

POST /api/tasks/approvals/5002/approve (approved=true)
→ 更新TaskApprovalRecord: approvalStatus=1(通过)
→ 更新TaskAssignee: status=3(已完成), completedAt=now
→ 检查Task整体状态，如果所有人完成则更新Task.status=3
→ 通知张三任务已完成
```

---

## 🎯 关键技术点总结

### 1. 一对多关系的设计
- Task : TaskAssignee = 1 : N
- 任务列表只查询Task表，显示1条记录
- 任务详情关联查询TaskAssignee，按系部分组展示

### 2. 审批配置的存储策略
- TaskApprovalConfig表：存储任务在不同系部的审批配置
- TaskAssignee.approvalConfig字段：JSON冗余存储，避免每次都关联查询
- 优点：查询快速，自包含完整审批链路

### 3. 权限控制
- 系领导审批列表：`WHERE approverId = currentUserId AND approvalStatus = 0`
- 自动实现"只看自己系部"的效果
- 无需额外的部门权限判断

### 4. 状态管理
- TaskAssignee.status：单个执行人的状态
- Task.status：任务整体状态（根据所有执行人计算）
- TaskAssignee.currentApprovalLevel：快速定位当前审批节点

### 5. 性能优化
- 审批配置JSON存储，减少关联查询
- 任务详情接口按系部分组返回，前端直接渲染
- 使用索引：task_id, assignee_id, department_id, approver_id

---

## 📝 实施建议

### 第一阶段：数据库迁移
1. 创建新表：TaskApprovalConfig
2. 修改TaskAssignee表：添加department_id, department_name, approval_config字段
3. 数据迁移脚本：将现有任务数据迁移到新结构

### 第二阶段：后端重构
1. 修改TaskService.createTask方法，支持按系部配置审批
2. 实现TaskApprovalService审批流程逻辑
3. 修改任务详情API，按系部分组返回执行人

### 第三阶段：前端重构
1. 修改创建任务对话框，添加审批配置面板
2. 实现任务详情页卡片式布局
3. 实现AssigneeCard组件
4. 修改任务列表，移除执行人列（多人任务只显示1条）

### 第四阶段：测试和优化
1. 功能测试：创建、提交、审批、打回全流程
2. 权限测试：不同角色看到的数据范围
3. 性能测试：大批量任务（100+执行人）的展示性能

---

## ❓ 潜在问题和解决方案

### Q1: 如果某个系部需要3级审批怎么办？
**A**: 审批配置支持动态添加级别，TaskApprovalConfig表存储任意级数的审批人。审批逻辑根据max(approvalLevel)判断是否完成。

### Q2: 任务整体状态如何计算？
**A**: 每次TaskAssignee状态变更时，触发updateTaskOverallStatus方法，统计所有执行人状态，更新Task.status。

### Q3: 如何防止误操作（删除任务/撤回审批）？
**A**:
- 任务创建后只能取消，不能删除
- 审批通过后不可撤回，只能由下一级打回
- 所有操作记录到TaskLog表

### Q4: 卡片网格如何响应式适配？
**A**: 使用CSS Grid，每行显示5-10个卡片，根据屏幕宽度自动调整。移动端使用列表模式替代网格。

### Q5: 大批量任务（500+执行人）性能如何？
**A**:
- 任务详情接口分页加载执行人
- 前端虚拟滚动渲染卡片
- 使用Redis缓存任务详情数据

---

## 🎉 预期效果

### 任务列表
- ✅ 多人任务只显示1条记录
- ✅ 显示发布人和完成进度统计
- ✅ 点击详情查看完整卡片视图

### 任务详情页
- ✅ 完整的任务信息
- ✅ 清晰的审批流程配置展示
- ✅ 电影院式卡片网格，一眼看清所有人的完成情况
- ✅ 卡片显示审批进度链路

### 审批流程
- ✅ 每个班主任提交后立即进入审批
- ✅ 系领导只看到自己系部的待审批任务
- ✅ 审批通过自动流转到下一级
- ✅ 打回后重新提交从第一级开始

---

**设计完成时间**: 2025-12-28
**设计师**: Claude (superpowers:brainstorming)
