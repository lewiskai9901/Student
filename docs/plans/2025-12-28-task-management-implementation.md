# 任务管理系统重构实施计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 重构任务管理系统，支持多人任务只显示1条记录，卡片式进度展示，按系部两级审批

**Architecture:** 修改数据模型将执行人信息移到子表，每个执行人独立审批流程。创建审批配置表支持按系部分别指定审批人。前端实现卡片式网格展示，按系部分组。

**Tech Stack:** Spring Boot 3.2, MyBatis Plus 3.5.7, Vue 3, TypeScript, Element Plus

---

## Phase 1: 数据库重构

### Task 1.1: 创建审批配置表DDL

**Files:**
- Create: `database/schema/task_approval_config.sql`

**Step 1: 创建审批配置表SQL**

创建文件 `database/schema/task_approval_config.sql`:

```sql
-- 任务审批配置表（按系部分别配置审批人）
CREATE TABLE task_approval_configs (
    id BIGINT PRIMARY KEY COMMENT '主键',
    task_id BIGINT NOT NULL COMMENT '任务ID',
    department_id BIGINT NOT NULL COMMENT '系部ID',
    department_name VARCHAR(100) NOT NULL COMMENT '系部名称',

    approval_level TINYINT NOT NULL COMMENT '审批级别: 1-第一级, 2-第二级, 3-第三级...',

    approver_id BIGINT NOT NULL COMMENT '审批人ID',
    approver_name VARCHAR(50) NOT NULL COMMENT '审批人姓名',
    approver_role VARCHAR(50) COMMENT '审批人角色（如：系领导、学工处领导）',

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    UNIQUE KEY uk_task_dept_level (task_id, department_id, approval_level),
    INDEX idx_task (task_id),
    INDEX idx_department (department_id),
    INDEX idx_approver (approver_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务审批配置表';
```

**Step 2: 执行SQL创建表**

运行命令:
```bash
mysql -u root -p student_management < database/schema/task_approval_config.sql
```

预期: 表创建成功

**Step 3: 提交**

```bash
git add database/schema/task_approval_config.sql
git commit -m "feat(db): 添加任务审批配置表"
```

---

### Task 1.2: 修改Task表和TaskAssignee表

**Files:**
- Create: `database/schema/alter_task_tables.sql`

**Step 1: 编写修改表结构SQL**

创建文件 `database/schema/alter_task_tables.sql`:

```sql
-- 1. 修改tasks表
ALTER TABLE tasks
    -- 移除执行人字段（单人任务时使用）
    DROP COLUMN IF EXISTS assignee_id,
    DROP COLUMN IF EXISTS assignee_name,

    -- 移除批量分配相关字段（移到子表和配置表）
    DROP COLUMN IF EXISTS department_id,
    DROP COLUMN IF EXISTS department_name,
    DROP COLUMN IF EXISTS target_ids,

    -- 移除流程相关字段（每个执行人独立流程）
    DROP COLUMN IF EXISTS workflow_template_id,
    DROP COLUMN IF EXISTS process_instance_id,
    DROP COLUMN IF EXISTS current_node,
    DROP COLUMN IF EXISTS current_approvers,

    -- 移除时间字段（移到子表）
    DROP COLUMN IF EXISTS accepted_at,
    DROP COLUMN IF EXISTS submitted_at,
    DROP COLUMN IF EXISTS completed_at;

-- 2. 修改task_assignees表
ALTER TABLE task_assignees
    -- 添加部门信息
    ADD COLUMN department_id BIGINT COMMENT '执行人所属系部ID' AFTER assignee_name,
    ADD COLUMN department_name VARCHAR(100) COMMENT '执行人所属系部名称' AFTER department_id,

    -- 添加审批配置JSON字段
    ADD COLUMN approval_config JSON COMMENT '审批配置: [{"level":1, "approverId":123, "approverName":"张主任"}, ...]' AFTER department_name,

    -- 添加当前审批级别
    ADD COLUMN current_approval_level TINYINT COMMENT '当前审批级别: 0-未提交, 1-第一级, 2-第二级...' AFTER process_instance_id;

-- 3. 添加索引
ALTER TABLE task_assignees
    ADD INDEX idx_department (department_id);

-- 4. 清空测试数据
TRUNCATE TABLE task_approval_records;
TRUNCATE TABLE task_submissions;
TRUNCATE TABLE task_assignees;
TRUNCATE TABLE tasks;
TRUNCATE TABLE task_logs;
```

**Step 2: 执行SQL修改表**

运行命令:
```bash
mysql -u root -p student_management < database/schema/alter_task_tables.sql
```

预期: 表结构修改成功，数据已清空

**Step 3: 验证表结构**

运行命令:
```bash
mysql -u root -p student_management -e "DESC tasks; DESC task_assignees; DESC task_approval_configs;"
```

预期: 显示新的表结构

**Step 4: 提交**

```bash
git add database/schema/alter_task_tables.sql
git commit -m "feat(db): 重构Task和TaskAssignee表结构，清空测试数据"
```

---

## Phase 2: 后端实体类修改

### Task 2.1: 修改Task实体类

**Files:**
- Modify: `backend/src/main/java/com/school/management/entity/task/Task.java`

**Step 1: 移除不需要的字段**

修改 `Task.java`，移除以下字段及其getter/setter:
- `assigneeId`
- `assigneeName`
- `departmentId`
- `departmentName`
- `targetIds`
- `acceptedAt`
- `submittedAt`
- `completedAt`
- `workflowTemplateId`
- `processInstanceId`
- `currentNode`
- `currentApprovers`

保留字段:
```java
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "tasks", autoResultMap = true)
public class Task extends BaseEntity {

    private String taskCode;
    private String title;
    private String description;
    private Integer priority;
    private Integer status;

    // 发布人信息
    private Long assignerId;
    private String assignerName;

    // 分配类型
    private Integer assignType;

    // 时间信息
    private LocalDateTime dueDate;

    // 附件
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Long> attachmentIds;

    // 非数据库字段
    @TableField(exist = false)
    private List<TaskAssignee> assignees;

    @TableField(exist = false)
    private List<TaskApprovalRecord> approvalRecords;
}
```

**Step 2: 编译验证**

运行命令:
```bash
cd backend
mvn clean compile -DskipTests
```

预期: 编译成功

**Step 3: 提交**

```bash
git add backend/src/main/java/com/school/management/entity/task/Task.java
git commit -m "refactor(entity): 简化Task实体类，移除执行人和流程字段"
```

---

### Task 2.2: 修改TaskAssignee实体类

**Files:**
- Modify: `backend/src/main/java/com/school/management/entity/task/TaskAssignee.java`

**Step 1: 添加新字段**

修改 `TaskAssignee.java`，添加新字段:

```java
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "task_assignees", autoResultMap = true)
public class TaskAssignee extends BaseEntity {

    private Long taskId;

    private Long assigneeId;
    private String assigneeName;

    // 新增：部门信息
    private Long departmentId;
    private String departmentName;

    // 新增：审批配置JSON
    @TableField(typeHandler = JacksonTypeHandler.class)
    private String approvalConfig;

    private Integer status;

    private LocalDateTime acceptedAt;
    private LocalDateTime submittedAt;
    private LocalDateTime completedAt;

    private String processInstanceId;

    // 新增：当前审批级别
    private Integer currentApprovalLevel;
}
```

**Step 2: 编译验证**

运行命令:
```bash
cd backend
mvn clean compile -DskipTests
```

预期: 编译成功

**Step 3: 提交**

```bash
git add backend/src/main/java/com/school/management/entity/task/TaskAssignee.java
git commit -m "refactor(entity): TaskAssignee添加部门和审批配置字段"
```

---

### Task 2.3: 创建TaskApprovalConfig实体类

**Files:**
- Create: `backend/src/main/java/com/school/management/entity/task/TaskApprovalConfig.java`

**Step 1: 创建实体类**

创建文件并写入:

```java
package com.school.management.entity.task;

import com.baomidou.mybatisplus.annotation.TableName;
import com.school.management.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 任务审批配置实体（按系部分别配置审批人）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("task_approval_configs")
public class TaskApprovalConfig extends BaseEntity {

    /**
     * 任务ID
     */
    private Long taskId;

    /**
     * 系部ID
     */
    private Long departmentId;

    /**
     * 系部名称
     */
    private String departmentName;

    /**
     * 审批级别: 1-第一级, 2-第二级, 3-第三级...
     */
    private Integer approvalLevel;

    /**
     * 审批人ID
     */
    private Long approverId;

    /**
     * 审批人姓名
     */
    private String approverName;

    /**
     * 审批人角色（如：系领导、学工处领导）
     */
    private String approverRole;
}
```

**Step 2: 编译验证**

运行命令:
```bash
cd backend
mvn clean compile -DskipTests
```

预期: 编译成功

**Step 3: 提交**

```bash
git add backend/src/main/java/com/school/management/entity/task/TaskApprovalConfig.java
git commit -m "feat(entity): 创建TaskApprovalConfig审批配置实体类"
```

---

## Phase 3: Mapper和DTO开发

### Task 3.1: 创建TaskApprovalConfigMapper

**Files:**
- Create: `backend/src/main/java/com/school/management/mapper/task/TaskApprovalConfigMapper.java`
- Create: `backend/src/main/resources/mapper/task/TaskApprovalConfigMapper.xml`

**Step 1: 创建Mapper接口**

创建文件 `TaskApprovalConfigMapper.java`:

```java
package com.school.management.mapper.task;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.management.entity.task.TaskApprovalConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 任务审批配置Mapper
 */
@Mapper
public interface TaskApprovalConfigMapper extends BaseMapper<TaskApprovalConfig> {

    /**
     * 根据任务ID和系部ID查询审批配置
     */
    List<TaskApprovalConfig> selectByTaskAndDept(@Param("taskId") Long taskId,
                                                  @Param("departmentId") Long departmentId);

    /**
     * 根据任务ID查询所有审批配置（按系部、级别分组）
     */
    List<TaskApprovalConfig> selectByTaskId(@Param("taskId") Long taskId);

    /**
     * 批量插入审批配置
     */
    int batchInsert(@Param("configs") List<TaskApprovalConfig> configs);
}
```

**Step 2: 创建XML映射文件**

创建文件 `backend/src/main/resources/mapper/task/TaskApprovalConfigMapper.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.school.management.mapper.task.TaskApprovalConfigMapper">

    <resultMap id="BaseResultMap" type="com.school.management.entity.task.TaskApprovalConfig">
        <id column="id" property="id"/>
        <result column="task_id" property="taskId"/>
        <result column="department_id" property="departmentId"/>
        <result column="department_name" property="departmentName"/>
        <result column="approval_level" property="approvalLevel"/>
        <result column="approver_id" property="approverId"/>
        <result column="approver_name" property="approverName"/>
        <result column="approver_role" property="approverRole"/>
        <result column="created_at" property="createdAt"/>
    </resultMap>

    <select id="selectByTaskAndDept" resultMap="BaseResultMap">
        SELECT * FROM task_approval_configs
        WHERE task_id = #{taskId} AND department_id = #{departmentId}
        ORDER BY approval_level ASC
    </select>

    <select id="selectByTaskId" resultMap="BaseResultMap">
        SELECT * FROM task_approval_configs
        WHERE task_id = #{taskId}
        ORDER BY department_id ASC, approval_level ASC
    </select>

    <insert id="batchInsert">
        INSERT INTO task_approval_configs
        (id, task_id, department_id, department_name, approval_level, approver_id, approver_name, approver_role, created_at)
        VALUES
        <foreach collection="configs" item="item" separator=",">
            (#{item.id}, #{item.taskId}, #{item.departmentId}, #{item.departmentName},
             #{item.approvalLevel}, #{item.approverId}, #{item.approverName}, #{item.approverRole}, NOW())
        </foreach>
    </insert>

</mapper>
```

**Step 3: 编译验证**

运行命令:
```bash
cd backend
mvn clean compile -DskipTests
```

预期: 编译成功

**Step 4: 提交**

```bash
git add backend/src/main/java/com/school/management/mapper/task/TaskApprovalConfigMapper.java
git add backend/src/main/resources/mapper/task/TaskApprovalConfigMapper.xml
git commit -m "feat(mapper): 创建TaskApprovalConfig Mapper和XML"
```

---

### Task 3.2: 创建DTO类

**Files:**
- Create: `backend/src/main/java/com/school/management/dto/task/ApprovalConfigDTO.java`
- Create: `backend/src/main/java/com/school/management/dto/task/DepartmentApprovalConfigDTO.java`
- Create: `backend/src/main/java/com/school/management/dto/task/TaskDetailDTO.java`

**Step 1: 创建ApprovalConfigDTO**

创建文件 `ApprovalConfigDTO.java`:

```java
package com.school.management.dto.task;

import lombok.Data;

/**
 * 审批配置DTO（单个审批级别）
 */
@Data
public class ApprovalConfigDTO {

    /**
     * 审批级别
     */
    private Integer level;

    /**
     * 审批人ID
     */
    private Long approverId;

    /**
     * 审批人姓名
     */
    private String approverName;

    /**
     * 审批人角色
     */
    private String approverRole;
}
```

**Step 2: 创建DepartmentApprovalConfigDTO**

创建文件 `DepartmentApprovalConfigDTO.java`:

```java
package com.school.management.dto.task;

import lombok.Data;

import java.util.List;

/**
 * 部门审批配置DTO（一个系部的所有审批级别）
 */
@Data
public class DepartmentApprovalConfigDTO {

    /**
     * 系部ID
     */
    private Long departmentId;

    /**
     * 系部名称
     */
    private String departmentName;

    /**
     * 审批级别列表
     */
    private List<ApprovalConfigDTO> levels;
}
```

**Step 3: 创建TaskDetailDTO**

创建文件 `TaskDetailDTO.java`:

```java
package com.school.management.dto.task;

import com.school.management.entity.task.Task;
import com.school.management.entity.task.TaskAssignee;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 任务详情DTO（含执行人卡片数据）
 */
@Data
public class TaskDetailDTO {

    /**
     * 任务基本信息
     */
    private Task task;

    /**
     * 审批流程配置（按系部）
     */
    private List<DepartmentApprovalFlowDTO> approvalFlows;

    /**
     * 执行人列表（按系部分组）
     */
    private List<DepartmentAssigneesDTO> assigneesByDepartment;

    /**
     * 统计信息
     */
    private TaskStatisticsDTO statistics;
}

/**
 * 部门审批流程DTO
 */
@Data
class DepartmentApprovalFlowDTO {
    private Long departmentId;
    private String departmentName;
    private String flowChain; // 如: "班主任提交 → 张主任(系领导) → 王处长(学工处)"
}

/**
 * 部门执行人列表DTO
 */
@Data
class DepartmentAssigneesDTO {
    private Long departmentId;
    private String departmentName;
    private Integer totalCount;
    private Integer completedCount;
    private List<TaskAssigneeDetailDTO> assignees;
}

/**
 * 执行人详情DTO（卡片数据）
 */
@Data
class TaskAssigneeDetailDTO {
    private Long assigneeId;
    private String assigneeName;
    private Integer status;
    private String statusText;
    private Integer currentApprovalLevel;
    private List<ApprovalConfigDTO> approvalConfig;
    private List<ApprovalRecordDTO> approvalRecords;
    private String submittedAt;
    private String completedAt;
}

/**
 * 审批记录DTO
 */
@Data
class ApprovalRecordDTO {
    private Integer approvalLevel;
    private Integer approvalStatus;
    private String approvalTime;
}
```

**Step 4: 编译验证**

运行命令:
```bash
cd backend
mvn clean compile -DskipTests
```

预期: 编译成功

**Step 5: 提交**

```bash
git add backend/src/main/java/com/school/management/dto/task/ApprovalConfigDTO.java
git add backend/src/main/java/com/school/management/dto/task/DepartmentApprovalConfigDTO.java
git add backend/src/main/java/com/school/management/dto/task/TaskDetailDTO.java
git commit -m "feat(dto): 创建任务审批相关DTO类"
```

---

### Task 3.3: 修改TaskCreateRequest

**Files:**
- Modify: `backend/src/main/java/com/school/management/dto/task/TaskCreateRequest.java`

**Step 1: 添加审批配置字段**

修改 `TaskCreateRequest.java`，添加审批配置字段:

```java
package com.school.management.dto.task;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 创建任务请求DTO
 */
@Data
public class TaskCreateRequest {

    @NotBlank(message = "任务标题不能为空")
    private String title;

    private String description;

    @NotNull(message = "优先级不能为空")
    private Integer priority;

    @NotNull(message = "截止时间不能为空")
    private LocalDateTime dueDate;

    @NotEmpty(message = "执行人列表不能为空")
    private List<Long> assigneeIds;

    /**
     * 审批配置（按系部）
     */
    @NotEmpty(message = "审批配置不能为空")
    private List<DepartmentApprovalConfigDTO> approvalConfigs;

    private List<Long> attachmentIds;
}
```

**Step 2: 编译验证**

运行命令:
```bash
cd backend
mvn clean compile -DskipTests
```

预期: 编译成功

**Step 3: 提交**

```bash
git add backend/src/main/java/com/school/management/dto/task/TaskCreateRequest.java
git commit -m "refactor(dto): TaskCreateRequest添加审批配置字段"
```

---

## Phase 4: Service层重构

### Task 4.1: 重构TaskService.createTask方法

**Files:**
- Modify: `backend/src/main/java/com/school/management/service/task/impl/TaskServiceImpl.java`

**Step 1: 备份原方法**

在修改前，先注释掉原来的 `createTask` 方法，保留备份。

**Step 2: 实现新的createTask方法**

在 `TaskServiceImpl.java` 中实现新的 `createTask` 方法:

```java
@Override
@Transactional(rollbackFor = Exception.class)
public Long createTask(TaskCreateRequest request) {
    // 1. 获取当前用户信息
    Long currentUserId = SecurityUtils.getCurrentUserId();
    String currentUserName = SecurityUtils.getCurrentUserName();

    // 2. 创建Task主记录
    Task task = new Task();
    task.setTaskCode(generateTaskCode());
    task.setTitle(request.getTitle());
    task.setDescription(request.getDescription());
    task.setPriority(request.getPriority());
    task.setDueDate(request.getDueDate());
    task.setAssignerId(currentUserId);
    task.setAssignerName(currentUserName);
    task.setAssignType(request.getAssigneeIds().size() > 1 ? 2 : 1);
    task.setStatus(0); // 待接收
    task.setAttachmentIds(request.getAttachmentIds());
    taskMapper.insert(task);

    // 3. 按系部分组执行人
    Map<Long, List<User>> deptAssigneeMap = groupAssigneesByDepartment(request.getAssigneeIds());

    // 4. 保存审批配置到task_approval_configs表
    List<TaskApprovalConfig> approvalConfigs = new ArrayList<>();
    for (DepartmentApprovalConfigDTO deptConfig : request.getApprovalConfigs()) {
        for (ApprovalConfigDTO level : deptConfig.getLevels()) {
            TaskApprovalConfig config = new TaskApprovalConfig();
            config.setTaskId(task.getId());
            config.setDepartmentId(deptConfig.getDepartmentId());
            config.setDepartmentName(deptConfig.getDepartmentName());
            config.setApprovalLevel(level.getLevel());
            config.setApproverId(level.getApproverId());
            config.setApproverName(level.getApproverName());
            config.setApproverRole(level.getApproverRole());
            approvalConfigs.add(config);
        }
    }
    if (!approvalConfigs.isEmpty()) {
        approvalConfigMapper.batchInsert(approvalConfigs);
    }

    // 5. 创建TaskAssignee记录
    List<TaskAssignee> assignees = new ArrayList<>();
    for (Map.Entry<Long, List<User>> entry : deptAssigneeMap.entrySet()) {
        Long deptId = entry.getKey();
        List<User> users = entry.getValue();

        // 获取该系部的审批配置
        List<TaskApprovalConfig> deptApprovalConfigs = approvalConfigMapper
            .selectByTaskAndDept(task.getId(), deptId);

        // 转换为JSON字符串
        String approvalConfigJson = convertApprovalConfigToJson(deptApprovalConfigs);

        for (User user : users) {
            TaskAssignee assignee = new TaskAssignee();
            assignee.setTaskId(task.getId());
            assignee.setAssigneeId(user.getId());
            assignee.setAssigneeName(user.getName());
            assignee.setDepartmentId(deptId);
            assignee.setDepartmentName(user.getDepartmentName());
            assignee.setApprovalConfig(approvalConfigJson);
            assignee.setStatus(0); // 待接收
            assignee.setCurrentApprovalLevel(0); // 未提交
            assignees.add(assignee);
        }
    }

    // 批量插入
    if (!assignees.isEmpty()) {
        assignees.forEach(taskAssigneeMapper::insert);
    }

    // 6. 发送通知给所有执行人
    sendTaskNotification(task, request.getAssigneeIds());

    // 7. 记录操作日志
    logTaskOperation(task.getId(), "创建任务", "创建任务: " + task.getTitle());

    return task.getId();
}

/**
 * 按系部分组执行人
 */
private Map<Long, List<User>> groupAssigneesByDepartment(List<Long> assigneeIds) {
    List<User> users = userService.listByIds(assigneeIds);
    return users.stream()
        .collect(Collectors.groupingBy(User::getDepartmentId));
}

/**
 * 将审批配置转换为JSON
 */
private String convertApprovalConfigToJson(List<TaskApprovalConfig> configs) {
    List<Map<String, Object>> jsonList = configs.stream()
        .map(config -> {
            Map<String, Object> map = new HashMap<>();
            map.put("level", config.getApprovalLevel());
            map.put("approverId", config.getApproverId());
            map.put("approverName", config.getApproverName());
            map.put("approverRole", config.getApproverRole());
            return map;
        })
        .collect(Collectors.toList());

    return JSON.toJSONString(jsonList);
}
```

**Step 3: 添加必要的依赖注入**

在 `TaskServiceImpl` 类中添加:

```java
@Autowired
private TaskApprovalConfigMapper approvalConfigMapper;

@Autowired
private UserService userService;
```

**Step 4: 编译验证**

运行命令:
```bash
cd backend
mvn clean compile -DskipTests
```

预期: 编译成功

**Step 5: 提交**

```bash
git add backend/src/main/java/com/school/management/service/task/impl/TaskServiceImpl.java
git commit -m "refactor(service): 重构TaskService.createTask支持审批配置"
```

---

### Task 4.2: 创建TaskApprovalService

**Files:**
- Create: `backend/src/main/java/com/school/management/service/task/TaskApprovalService.java`
- Create: `backend/src/main/java/com/school/management/service/task/impl/TaskApprovalServiceImpl.java`

**Step 1: 创建Service接口**

创建文件 `TaskApprovalService.java`:

```java
package com.school.management.service.task;

import com.school.management.dto.task.TaskApproveRequest;
import com.school.management.dto.task.TaskSubmitRequest;

import java.util.List;

/**
 * 任务审批服务
 */
public interface TaskApprovalService {

    /**
     * 班主任提交任务
     */
    void submitTask(Long taskId, TaskSubmitRequest request);

    /**
     * 审批人审批任务
     */
    void approveTask(Long recordId, TaskApproveRequest request);

    /**
     * 获取我的待审批列表
     */
    List<TaskApprovalDTO> getMyPendingApprovals(Long userId);
}
```

**Step 2: 创建Service实现类**

创建文件 `TaskApprovalServiceImpl.java`:

```java
package com.school.management.service.task.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.management.dto.task.ApprovalConfigDTO;
import com.school.management.dto.task.TaskApproveRequest;
import com.school.management.dto.task.TaskSubmitRequest;
import com.school.management.entity.task.*;
import com.school.management.exception.BusinessException;
import com.school.management.mapper.task.*;
import com.school.management.service.task.TaskApprovalService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class TaskApprovalServiceImpl implements TaskApprovalService {

    @Autowired
    private TaskMapper taskMapper;

    @Autowired
    private TaskAssigneeMapper taskAssigneeMapper;

    @Autowired
    private TaskSubmissionMapper submissionMapper;

    @Autowired
    private TaskApprovalRecordMapper approvalRecordMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitTask(Long taskId, TaskSubmitRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        // 1. 查找执行人记录
        TaskAssignee assignee = taskAssigneeMapper.selectOne(
            new LambdaQueryWrapper<TaskAssignee>()
                .eq(TaskAssignee::getTaskId, taskId)
                .eq(TaskAssignee::getAssigneeId, currentUserId)
        );

        if (assignee == null) {
            throw new BusinessException("您不是该任务的执行人");
        }

        if (assignee.getStatus() != 0 && assignee.getStatus() != 1 && assignee.getStatus() != 4) {
            throw new BusinessException("当前状态不允许提交");
        }

        // 2. 创建提交记录
        TaskSubmission submission = new TaskSubmission();
        submission.setTaskId(taskId);
        submission.setAssigneeId(currentUserId);
        submission.setContent(request.getContent());
        submission.setAttachmentIds(request.getAttachmentIds());
        submission.setSubmittedAt(LocalDateTime.now());
        submissionMapper.insert(submission);

        // 3. 更新执行人状态
        assignee.setStatus(2); // 待审核
        assignee.setSubmittedAt(LocalDateTime.now());
        assignee.setCurrentApprovalLevel(1); // 进入第一级审批
        taskAssigneeMapper.updateById(assignee);

        // 4. 解析审批配置，获取第一级审批人
        List<ApprovalConfigDTO> approvalConfigs = JSON.parseArray(
            assignee.getApprovalConfig(),
            ApprovalConfigDTO.class
        );

        ApprovalConfigDTO firstLevel = approvalConfigs.stream()
            .filter(c -> c.getLevel() == 1)
            .findFirst()
            .orElseThrow(() -> new BusinessException("未找到第一级审批人配置"));

        // 5. 创建待审批记录
        TaskApprovalRecord record = new TaskApprovalRecord();
        record.setTaskId(taskId);
        record.setAssigneeId(currentUserId);
        record.setSubmissionId(submission.getId());
        record.setApprovalLevel(1);
        record.setApproverId(firstLevel.getApproverId());
        record.setApproverName(firstLevel.getApproverName());
        record.setApproverRole(firstLevel.getApproverRole());
        record.setApprovalStatus(0); // 待审批
        approvalRecordMapper.insert(record);

        // 6. 更新任务整体状态
        updateTaskOverallStatus(taskId);

        // 7. 发送审批通知
        // TODO: 发送通知给第一级审批人
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveTask(Long recordId, TaskApproveRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        // 1. 查找待审批记录
        TaskApprovalRecord record = approvalRecordMapper.selectById(recordId);
        if (record == null) {
            throw new BusinessException("审批记录不存在");
        }

        if (!record.getApproverId().equals(currentUserId)) {
            throw new BusinessException("您不是该任务的审批人");
        }

        if (record.getApprovalStatus() != 0) {
            throw new BusinessException("该任务已审批");
        }

        // 2. 更新审批记录
        record.setApprovalStatus(request.getApproved() ? 1 : 2);
        record.setApprovalComment(request.getComment());
        record.setApprovalTime(LocalDateTime.now());
        approvalRecordMapper.updateById(record);

        // 3. 查找执行人记录
        TaskAssignee assignee = taskAssigneeMapper.selectOne(
            new LambdaQueryWrapper<TaskAssignee>()
                .eq(TaskAssignee::getTaskId, record.getTaskId())
                .eq(TaskAssignee::getAssigneeId, record.getAssigneeId())
        );

        if (request.getApproved()) {
            // 审批通过
            handleApprovalPassed(assignee, record);
        } else {
            // 审批打回
            assignee.setStatus(4); // 已打回
            assignee.setCurrentApprovalLevel(0);
            taskAssigneeMapper.updateById(assignee);

            // TODO: 发送打回通知给执行人
        }

        // 4. 更新任务整体状态
        updateTaskOverallStatus(record.getTaskId());
    }

    /**
     * 处理审批通过
     */
    private void handleApprovalPassed(TaskAssignee assignee, TaskApprovalRecord currentRecord) {
        // 解析审批配置
        List<ApprovalConfigDTO> approvalConfigs = JSON.parseArray(
            assignee.getApprovalConfig(),
            ApprovalConfigDTO.class
        );

        int maxLevel = approvalConfigs.stream()
            .mapToInt(ApprovalConfigDTO::getLevel)
            .max()
            .orElse(1);

        if (currentRecord.getApprovalLevel() < maxLevel) {
            // 还有下一级审批
            int nextLevel = currentRecord.getApprovalLevel() + 1;
            assignee.setCurrentApprovalLevel(nextLevel);
            taskAssigneeMapper.updateById(assignee);

            // 获取下一级审批人配置
            ApprovalConfigDTO nextConfig = approvalConfigs.stream()
                .filter(c -> c.getLevel() == nextLevel)
                .findFirst()
                .orElseThrow(() -> new BusinessException("未找到下一级审批人配置"));

            // 创建下一级待审批记录
            TaskApprovalRecord nextRecord = new TaskApprovalRecord();
            nextRecord.setTaskId(assignee.getTaskId());
            nextRecord.setAssigneeId(assignee.getAssigneeId());
            nextRecord.setSubmissionId(currentRecord.getSubmissionId());
            nextRecord.setApprovalLevel(nextLevel);
            nextRecord.setApproverId(nextConfig.getApproverId());
            nextRecord.setApproverName(nextConfig.getApproverName());
            nextRecord.setApproverRole(nextConfig.getApproverRole());
            nextRecord.setApprovalStatus(0);
            approvalRecordMapper.insert(nextRecord);

            // TODO: 发送通知给下一级审批人
        } else {
            // 所有审批通过，任务完成
            assignee.setStatus(3); // 已完成
            assignee.setCompletedAt(LocalDateTime.now());
            taskAssigneeMapper.updateById(assignee);

            // TODO: 发送完成通知给执行人
        }
    }

    /**
     * 更新任务整体状态
     */
    private void updateTaskOverallStatus(Long taskId) {
        List<TaskAssignee> assignees = taskAssigneeMapper.selectList(
            new LambdaQueryWrapper<TaskAssignee>()
                .eq(TaskAssignee::getTaskId, taskId)
        );

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

    @Override
    public List<TaskApprovalDTO> getMyPendingApprovals(Long userId) {
        // TODO: 实现查询逻辑
        return null;
    }
}
```

**Step 3: 编译验证**

运行命令:
```bash
cd backend
mvn clean compile -DskipTests
```

预期: 编译成功

**Step 4: 提交**

```bash
git add backend/src/main/java/com/school/management/service/task/TaskApprovalService.java
git add backend/src/main/java/com/school/management/service/task/impl/TaskApprovalServiceImpl.java
git commit -m "feat(service): 创建TaskApprovalService实现审批流程"
```

---

## Phase 5: API接口重构

### Task 5.1: 修改TaskController.getTaskDetail

**Files:**
- Modify: `backend/src/main/java/com/school/management/controller/task/TaskController.java`

**Step 1: 实现新的getTaskDetail方法**

在 `TaskController.java` 中修改 `getTaskDetail` 方法:

```java
/**
 * 获取任务详情（含卡片式执行人数据）
 */
@GetMapping("/{id}/detail")
public Result<TaskDetailDTO> getTaskDetail(@PathVariable Long id) {
    TaskDetailDTO detail = taskService.getTaskDetail(id);
    return Result.success(detail);
}
```

**Step 2: 在TaskService中实现getTaskDetail**

在 `TaskServiceImpl.java` 中添加方法:

```java
@Override
public TaskDetailDTO getTaskDetail(Long taskId) {
    // 1. 查询任务基本信息
    Task task = taskMapper.selectById(taskId);
    if (task == null) {
        throw new BusinessException("任务不存在");
    }

    // 2. 查询所有执行人
    List<TaskAssignee> assignees = taskAssigneeMapper.selectList(
        new LambdaQueryWrapper<TaskAssignee>()
            .eq(TaskAssignee::getTaskId, taskId)
    );

    // 3. 按系部分组
    Map<Long, List<TaskAssignee>> deptMap = assignees.stream()
        .collect(Collectors.groupingBy(TaskAssignee::getDepartmentId));

    // 4. 构建返回数据
    TaskDetailDTO detail = new TaskDetailDTO();
    detail.setTask(task);

    // 5. 构建审批流程配置
    List<DepartmentApprovalFlowDTO> flows = new ArrayList<>();
    for (Map.Entry<Long, List<TaskAssignee>> entry : deptMap.entrySet()) {
        TaskAssignee firstAssignee = entry.getValue().get(0);
        List<ApprovalConfigDTO> configs = JSON.parseArray(
            firstAssignee.getApprovalConfig(),
            ApprovalConfigDTO.class
        );

        String flowChain = buildFlowChain(configs);

        DepartmentApprovalFlowDTO flow = new DepartmentApprovalFlowDTO();
        flow.setDepartmentId(entry.getKey());
        flow.setDepartmentName(firstAssignee.getDepartmentName());
        flow.setFlowChain(flowChain);
        flows.add(flow);
    }
    detail.setApprovalFlows(flows);

    // 6. 构建执行人列表（按系部）
    List<DepartmentAssigneesDTO> deptAssignees = new ArrayList<>();
    for (Map.Entry<Long, List<TaskAssignee>> entry : deptMap.entrySet()) {
        DepartmentAssigneesDTO deptDto = new DepartmentAssigneesDTO();
        deptDto.setDepartmentId(entry.getKey());
        deptDto.setDepartmentName(entry.getValue().get(0).getDepartmentName());
        deptDto.setTotalCount(entry.getValue().size());
        deptDto.setCompletedCount((int) entry.getValue().stream()
            .filter(a -> a.getStatus() == 3)
            .count());

        // 构建执行人详情列表
        List<TaskAssigneeDetailDTO> assigneeDetails = entry.getValue().stream()
            .map(this::buildAssigneeDetail)
            .collect(Collectors.toList());
        deptDto.setAssignees(assigneeDetails);

        deptAssignees.add(deptDto);
    }
    detail.setAssigneesByDepartment(deptAssignees);

    // 7. 统计信息
    TaskStatisticsDTO stats = new TaskStatisticsDTO();
    stats.setTotalAssignees(assignees.size());
    stats.setCompletedCount((int) assignees.stream().filter(a -> a.getStatus() == 3).count());
    stats.setInProgressCount((int) assignees.stream().filter(a -> a.getStatus() == 1 || a.getStatus() == 2).count());
    stats.setPendingCount((int) assignees.stream().filter(a -> a.getStatus() == 0).count());
    detail.setStatistics(stats);

    return detail;
}

/**
 * 构建流程链描述
 */
private String buildFlowChain(List<ApprovalConfigDTO> configs) {
    return "班主任提交 → " + configs.stream()
        .sorted(Comparator.comparing(ApprovalConfigDTO::getLevel))
        .map(c -> c.getApproverName() + "(" + c.getApproverRole() + ")")
        .collect(Collectors.joining(" → "));
}

/**
 * 构建执行人详情
 */
private TaskAssigneeDetailDTO buildAssigneeDetail(TaskAssignee assignee) {
    TaskAssigneeDetailDTO detail = new TaskAssigneeDetailDTO();
    detail.setAssigneeId(assignee.getAssigneeId());
    detail.setAssigneeName(assignee.getAssigneeName());
    detail.setStatus(assignee.getStatus());
    detail.setStatusText(getStatusText(assignee.getStatus()));
    detail.setCurrentApprovalLevel(assignee.getCurrentApprovalLevel());

    // 解析审批配置
    List<ApprovalConfigDTO> configs = JSON.parseArray(
        assignee.getApprovalConfig(),
        ApprovalConfigDTO.class
    );
    detail.setApprovalConfig(configs);

    // 查询审批记录
    List<TaskApprovalRecord> records = approvalRecordMapper.selectList(
        new LambdaQueryWrapper<TaskApprovalRecord>()
            .eq(TaskApprovalRecord::getTaskId, assignee.getTaskId())
            .eq(TaskApprovalRecord::getAssigneeId, assignee.getAssigneeId())
    );

    List<ApprovalRecordDTO> recordDtos = records.stream()
        .map(r -> {
            ApprovalRecordDTO dto = new ApprovalRecordDTO();
            dto.setApprovalLevel(r.getApprovalLevel());
            dto.setApprovalStatus(r.getApprovalStatus());
            dto.setApprovalTime(r.getApprovalTime() != null ? r.getApprovalTime().toString() : null);
            return dto;
        })
        .collect(Collectors.toList());
    detail.setApprovalRecords(recordDtos);

    detail.setSubmittedAt(assignee.getSubmittedAt() != null ? assignee.getSubmittedAt().toString() : null);
    detail.setCompletedAt(assignee.getCompletedAt() != null ? assignee.getCompletedAt().toString() : null);

    return detail;
}

private String getStatusText(Integer status) {
    Map<Integer, String> map = Map.of(
        0, "待接收",
        1, "进行中",
        2, "待审核",
        3, "已完成",
        4, "已打回"
    );
    return map.getOrDefault(status, "未知");
}
```

**Step 3: 编译验证**

运行命令:
```bash
cd backend
mvn clean compile -DskipTests
```

预期: 编译成功

**Step 4: 提交**

```bash
git add backend/src/main/java/com/school/management/controller/task/TaskController.java
git add backend/src/main/java/com/school/management/service/task/impl/TaskServiceImpl.java
git commit -m "feat(api): 实现任务详情接口，返回卡片式数据"
```

---

### Task 5.2: 添加审批接口

**Files:**
- Create: `backend/src/main/java/com/school/management/controller/task/TaskApprovalController.java`

**Step 1: 创建Controller**

创建文件 `TaskApprovalController.java`:

```java
package com.school.management.controller.task;

import com.school.management.common.Result;
import com.school.management.dto.task.TaskApprovalDTO;
import com.school.management.dto.task.TaskApproveRequest;
import com.school.management.service.task.TaskApprovalService;
import com.school.management.util.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 任务审批控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/tasks/approvals")
public class TaskApprovalController {

    @Autowired
    private TaskApprovalService approvalService;

    /**
     * 获取我的待审批列表
     */
    @GetMapping("/pending")
    public Result<List<TaskApprovalDTO>> getMyPendingApprovals() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        List<TaskApprovalDTO> approvals = approvalService.getMyPendingApprovals(currentUserId);
        return Result.success(approvals);
    }

    /**
     * 审批任务
     */
    @PostMapping("/{recordId}/approve")
    public Result<Void> approveTask(@PathVariable Long recordId,
                                      @Valid @RequestBody TaskApproveRequest request) {
        approvalService.approveTask(recordId, request);
        return Result.success("审批成功");
    }
}
```

**Step 2: 编译验证**

运行命令:
```bash
cd backend
mvn clean compile -DskipTests
```

预期: 编译成功

**Step 3: 提交**

```bash
git add backend/src/main/java/com/school/management/controller/task/TaskApprovalController.java
git commit -m "feat(api): 添加任务审批API接口"
```

---

## Phase 6: 前端UI重构

### Task 6.1: 修改创建任务对话框

**Files:**
- Modify: `frontend/src/views/task/components/TaskCreateDialog.vue`

**Step 1: 添加审批配置表单**

在 `TaskCreateDialog.vue` 的表单中添加审批配置部分:

```vue
<template>
  <el-dialog
    v-model="dialogVisible"
    title="创建任务"
    width="800px"
    @close="handleClose"
  >
    <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
      <!-- 现有字段 -->
      <el-form-item label="任务标题" prop="title">
        <el-input v-model="form.title" placeholder="请输入任务标题" />
      </el-form-item>

      <el-form-item label="任务描述" prop="description">
        <el-input
          v-model="form.description"
          type="textarea"
          :rows="3"
          placeholder="请输入任务描述"
        />
      </el-form-item>

      <el-form-item label="优先级" prop="priority">
        <el-radio-group v-model="form.priority">
          <el-radio :label="1">紧急</el-radio>
          <el-radio :label="2">普通</el-radio>
          <el-radio :label="3">低</el-radio>
        </el-radio-group>
      </el-form-item>

      <el-form-item label="截止时间" prop="dueDate">
        <el-date-picker
          v-model="form.dueDate"
          type="datetime"
          placeholder="选择截止时间"
          value-format="YYYY-MM-DD HH:mm:ss"
        />
      </el-form-item>

      <!-- 选择执行人 -->
      <el-form-item label="执行人" prop="assigneeIds">
        <el-button @click="openUserSelector">选择执行人 ({{ form.assigneeIds.length }})</el-button>
        <div v-if="selectedUsers.length > 0" class="mt-2">
          <el-tag
            v-for="user in selectedUsers"
            :key="user.id"
            closable
            @close="removeUser(user.id)"
            class="mr-2 mb-2"
          >
            {{ user.name }} ({{ user.departmentName }})
          </el-tag>
        </div>
      </el-form-item>

      <!-- 审批配置 -->
      <el-divider>审批流程配置（按系部）</el-divider>

      <div v-for="deptConfig in departmentConfigs" :key="deptConfig.departmentId" class="mb-4">
        <h4 class="text-base font-medium mb-2">【{{ deptConfig.departmentName }}】({{ deptConfig.userCount }}人)</h4>

        <div v-for="(level, index) in deptConfig.levels" :key="index" class="ml-4 mb-2 flex items-center gap-2">
          <span class="w-20">第{{ level.level }}级审批:</span>
          <el-select
            v-model="level.approverId"
            placeholder="选择审批人"
            @change="handleApproverChange(deptConfig.departmentId, level)"
            class="w-48"
          >
            <el-option
              v-for="user in approverOptions"
              :key="user.id"
              :label="user.name"
              :value="user.id"
            />
          </el-select>
          <span v-if="level.approverName">{{ level.approverName }}</span>
          <el-button
            v-if="index > 0"
            type="danger"
            size="small"
            @click="removeApprovalLevel(deptConfig.departmentId, index)"
          >
            删除
          </el-button>
        </div>

        <el-button
          size="small"
          @click="addApprovalLevel(deptConfig.departmentId)"
          class="ml-24"
        >
          + 添加审批级别
        </el-button>
      </div>
    </el-form>

    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button type="primary" @click="handleSubmit" :loading="loading">创建任务</el-button>
    </template>

    <!-- 用户选择对话框 -->
    <UserSelectorDialog
      v-model:visible="userSelectorVisible"
      :selected-ids="form.assigneeIds"
      @confirm="handleUsersSelected"
    />
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { createTask } from '@/api/task'
import { getUserList } from '@/api/user'
import UserSelectorDialog from './UserSelectorDialog.vue'

const dialogVisible = defineModel('visible', { type: Boolean, default: false })

const formRef = ref()
const loading = ref(false)
const userSelectorVisible = ref(false)

const form = reactive({
  title: '',
  description: '',
  priority: 2,
  dueDate: '',
  assigneeIds: [] as number[],
  approvalConfigs: [] as any[]
})

const selectedUsers = ref<any[]>([])
const approverOptions = ref<any[]>([])

// 按系部分组的审批配置
const departmentConfigs = ref<any[]>([])

// 监听执行人变化，重新分组系部
watch(() => form.assigneeIds, async (newIds) => {
  if (newIds.length === 0) {
    departmentConfigs.value = []
    return
  }

  // 按系部分组
  const deptMap = new Map()
  selectedUsers.value.forEach(user => {
    if (!deptMap.has(user.departmentId)) {
      deptMap.set(user.departmentId, {
        departmentId: user.departmentId,
        departmentName: user.departmentName,
        userCount: 0,
        levels: [
          { level: 1, approverId: null, approverName: '', approverRole: '系领导' },
          { level: 2, approverId: null, approverName: '', approverRole: '学工处领导' }
        ]
      })
    }
    const dept = deptMap.get(user.departmentId)
    dept.userCount++
  })

  departmentConfigs.value = Array.from(deptMap.values())
})

const openUserSelector = () => {
  userSelectorVisible.value = true
}

const handleUsersSelected = (users: any[]) => {
  selectedUsers.value = users
  form.assigneeIds = users.map(u => u.id)
}

const removeUser = (userId: number) => {
  selectedUsers.value = selectedUsers.value.filter(u => u.id !== userId)
  form.assigneeIds = selectedUsers.value.map(u => u.id)
}

const handleApproverChange = (deptId: number, level: any) => {
  const approver = approverOptions.value.find(u => u.id === level.approverId)
  if (approver) {
    level.approverName = approver.name
  }
}

const addApprovalLevel = (deptId: number) => {
  const dept = departmentConfigs.value.find(d => d.departmentId === deptId)
  if (dept) {
    const nextLevel = dept.levels.length + 1
    dept.levels.push({
      level: nextLevel,
      approverId: null,
      approverName: '',
      approverRole: `第${nextLevel}级审批人`
    })
  }
}

const removeApprovalLevel = (deptId: number, index: number) => {
  const dept = departmentConfigs.value.find(d => d.departmentId === deptId)
  if (dept) {
    dept.levels.splice(index, 1)
    // 重新编号
    dept.levels.forEach((level, i) => {
      level.level = i + 1
    })
  }
}

const handleSubmit = async () => {
  await formRef.value?.validate()

  // 构建审批配置
  form.approvalConfigs = departmentConfigs.value.map(dept => ({
    departmentId: dept.departmentId,
    departmentName: dept.departmentName,
    levels: dept.levels.filter(l => l.approverId)
  }))

  // 验证审批配置
  for (const config of form.approvalConfigs) {
    if (config.levels.length === 0) {
      ElMessage.error(`请为【${config.departmentName}】配置审批人`)
      return
    }
  }

  loading.value = true
  try {
    await createTask(form)
    ElMessage.success('任务创建成功')
    dialogVisible.value = false
    emit('success')
  } catch (error) {
    console.error('创建任务失败', error)
  } finally {
    loading.value = false
  }
}

const handleClose = () => {
  formRef.value?.resetFields()
  selectedUsers.value = []
  departmentConfigs.value = []
  dialogVisible.value = false
}

// 加载审批人选项（管理员和系领导）
const loadApproverOptions = async () => {
  try {
    const res = await getUserList({ roleIds: [1, 2] }) // 假设1=管理员，2=系领导
    approverOptions.value = res.records
  } catch (error) {
    console.error('加载审批人失败', error)
  }
}

loadApproverOptions()
</script>
```

**Step 2: 提交**

```bash
git add frontend/src/views/task/components/TaskCreateDialog.vue
git commit -m "feat(ui): 创建任务对话框添加审批配置表单"
```

---

### Task 6.2: 创建AssigneeCard组件

**Files:**
- Create: `frontend/src/views/task/components/AssigneeCard.vue`

**Step 1: 创建卡片组件**

创建文件 `AssigneeCard.vue`:

```vue
<template>
  <div class="assignee-card" :class="statusClass">
    <!-- 执行人姓名 -->
    <div class="assignee-name">{{ assignee.assigneeName }}</div>

    <!-- 状态图标 -->
    <div class="status-icon">
      <CheckCircle v-if="assignee.status === 3" :size="24" class="text-green-500" />
      <Clock v-else-if="assignee.status === 2" :size="24" class="text-yellow-500" />
      <Play v-else-if="assignee.status === 1" :size="24" class="text-blue-500" />
      <Circle v-else :size="24" class="text-gray-400" />
    </div>

    <!-- 状态文本 -->
    <div class="status-text">{{ assignee.statusText }}</div>

    <!-- 分隔线 -->
    <div class="divider"></div>

    <!-- 审批进度 -->
    <div class="approval-progress">
      <div
        v-for="config in assignee.approvalConfig"
        :key="config.level"
        class="approval-step"
        :class="getStepClass(config.level)"
      >
        <Check v-if="isApproved(config.level)" :size="12" />
        <Clock v-else-if="isCurrent(config.level)" :size="12" />
        <span class="ml-1">{{ getLevelName(config.level) }}</span>
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
    status: number
    statusText: string
    currentApprovalLevel: number
    approvalConfig: Array<{
      level: number
      approverName: string
      approverRole: string
    }>
    approvalRecords: Array<{
      approvalLevel: number
      approvalStatus: number
    }>
  }
}

const props = defineProps<AssigneeCardProps>()

const statusClass = computed(() => {
  const map: Record<number, string> = {
    0: 'border-gray-300 bg-gray-50',
    1: 'border-blue-400 bg-blue-50',
    2: 'border-yellow-400 bg-yellow-50',
    3: 'border-green-400 bg-green-50',
    4: 'border-red-400 bg-red-50'
  }
  return map[props.assignee.status] || 'border-gray-300 bg-gray-50'
})

const isApproved = (level: number) => {
  const record = props.assignee.approvalRecords?.find(r => r.approvalLevel === level)
  return record?.approvalStatus === 1
}

const isCurrent = (level: number) => {
  return props.assignee.currentApprovalLevel === level
}

const getStepClass = (level: number) => {
  if (isApproved(level)) return 'text-green-600 font-medium'
  if (isCurrent(level)) return 'text-yellow-600 font-medium'
  return 'text-gray-400'
}

const getLevelName = (level: number) => {
  if (level === 1) return '系审'
  if (level === 2) return '学工'
  return `L${level}`
}
</script>

<style scoped>
.assignee-card {
  @apply border-2 rounded-lg p-3 text-center transition-all cursor-pointer;
  @apply hover:shadow-md hover:scale-105;
  width: 110px;
  min-height: 140px;
  display: flex;
  flex-direction: column;
  align-items: center;
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

.divider {
  @apply w-full border-t border-gray-300 my-2;
}

.approval-progress {
  @apply text-xs space-y-1 w-full;
}

.approval-step {
  @apply flex items-center justify-center gap-1;
}
</style>
```

**Step 2: 提交**

```bash
git add frontend/src/views/task/components/AssigneeCard.vue
git commit -m "feat(ui): 创建AssigneeCard执行人卡片组件"
```

---

### Task 6.3: 创建任务详情对话框

**Files:**
- Create: `frontend/src/views/task/components/TaskDetailDialog.vue`

**Step 1: 创建详情对话框**

创建文件 `TaskDetailDialog.vue`:

```vue
<template>
  <el-dialog
    v-model="dialogVisible"
    title="任务详情"
    width="90%"
    @open="loadTaskDetail"
  >
    <div v-if="loading" class="flex justify-center py-8">
      <el-icon class="is-loading" :size="32"><Loading /></el-icon>
    </div>

    <div v-else-if="detail" class="space-y-6">
      <!-- 任务信息 -->
      <div class="border rounded-lg p-4 bg-gray-50">
        <h3 class="text-lg font-semibold mb-3 flex items-center gap-2">
          <FileText :size="20" />
          任务信息
        </h3>
        <div class="grid grid-cols-2 gap-4 text-sm">
          <div><span class="text-gray-600">任务标题:</span> {{ detail.task.title }}</div>
          <div><span class="text-gray-600">任务编号:</span> {{ detail.task.taskCode }}</div>
          <div><span class="text-gray-600">发布人:</span> {{ detail.task.assignerName }}</div>
          <div><span class="text-gray-600">发布时间:</span> {{ formatDate(detail.task.createdAt) }}</div>
          <div><span class="text-gray-600">截止时间:</span> {{ formatDate(detail.task.dueDate) }}</div>
          <div><span class="text-gray-600">优先级:</span> <el-tag :type="getPriorityType(detail.task.priority)" size="small">{{ getPriorityText(detail.task.priority) }}</el-tag></div>
          <div class="col-span-2"><span class="text-gray-600">任务描述:</span> {{ detail.task.description }}</div>
        </div>
      </div>

      <!-- 审批流程配置 -->
      <div class="border rounded-lg p-4 bg-blue-50">
        <h3 class="text-lg font-semibold mb-3 flex items-center gap-2">
          <GitBranch :size="20" />
          审批流程配置
        </h3>
        <div class="space-y-2">
          <div v-for="flow in detail.approvalFlows" :key="flow.departmentId" class="text-sm">
            <span class="font-medium">【{{ flow.departmentName }}】</span>
            <span class="text-gray-600 ml-2">{{ flow.flowChain }}</span>
          </div>
        </div>
      </div>

      <!-- 执行人完成情况 -->
      <div class="border rounded-lg p-4">
        <h3 class="text-lg font-semibold mb-4 flex items-center gap-2">
          <Users :size="20" />
          执行人完成情况
        </h3>

        <!-- 按系部分组显示 -->
        <div v-for="dept in detail.assigneesByDepartment" :key="dept.departmentId" class="mb-6">
          <h4 class="text-base font-medium mb-3">
            【{{ dept.departmentName }}】
            <span class="text-sm text-gray-600 ml-2">({{ dept.completedCount }}/{{ dept.totalCount }}人完成)</span>
          </h4>

          <!-- 卡片网格 -->
          <div class="grid grid-cols-5 xl:grid-cols-8 gap-3">
            <AssigneeCard
              v-for="assignee in dept.assignees"
              :key="assignee.assigneeId"
              :assignee="assignee"
            />
          </div>
        </div>
      </div>

      <!-- 统计信息 -->
      <div class="border rounded-lg p-4 bg-green-50">
        <div class="flex gap-6 text-sm">
          <div><span class="text-gray-600">总计:</span> <span class="font-medium">{{ detail.statistics.totalAssignees }}人</span></div>
          <div><span class="text-gray-600">已完成:</span> <span class="font-medium text-green-600">{{ detail.statistics.completedCount }}人</span></div>
          <div><span class="text-gray-600">进行中:</span> <span class="font-medium text-blue-600">{{ detail.statistics.inProgressCount }}人</span></div>
          <div><span class="text-gray-600">待接收:</span> <span class="font-medium text-gray-600">{{ detail.statistics.pendingCount }}人</span></div>
        </div>
      </div>
    </div>

    <template #footer>
      <el-button @click="dialogVisible = false">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { FileText, GitBranch, Users, Loading } from 'lucide-vue-next'
import { getTaskDetail } from '@/api/task'
import AssigneeCard from './AssigneeCard.vue'

const props = defineProps<{
  taskId?: number | string
}>()

const dialogVisible = defineModel('visible', { type: Boolean, default: false })

const loading = ref(false)
const detail = ref<any>(null)

const loadTaskDetail = async () => {
  if (!props.taskId) return

  loading.value = true
  try {
    const res = await getTaskDetail(props.taskId)
    detail.value = res
  } catch (error) {
    console.error('加载任务详情失败', error)
  } finally {
    loading.value = false
  }
}

const formatDate = (dateStr?: string) => {
  if (!dateStr) return '-'
  return dateStr.substring(0, 16).replace('T', ' ')
}

const getPriorityText = (priority: number) => {
  const map: Record<number, string> = { 1: '紧急', 2: '普通', 3: '低' }
  return map[priority] || '未知'
}

const getPriorityType = (priority: number) => {
  const map: Record<number, any> = { 1: 'danger', 2: 'primary', 3: 'info' }
  return map[priority] || 'info'
}
</script>
```

**Step 2: 提交**

```bash
git add frontend/src/views/task/components/TaskDetailDialog.vue
git commit -m "feat(ui): 创建任务详情对话框，显示卡片式执行人"
```

---

## Phase 7: 测试和验证

### Task 7.1: 后端启动测试

**Step 1: 启动后端服务**

运行命令:
```bash
cd backend
JAVA_HOME="/c/Program Files/Java/jdk-17" PATH="/c/Program Files/Java/jdk-17/bin:$PATH" mvn spring-boot:run -DskipTests
```

预期: 服务启动成功，无报错

**Step 2: 验证数据库表**

运行命令:
```bash
mysql -u root -p student_management -e "SELECT COUNT(*) FROM task_approval_configs; SELECT COUNT(*) FROM task_assignees;"
```

预期: 表可以正常查询

---

### Task 7.2: 前端启动测试

**Step 1: 启动前端服务**

运行命令:
```bash
cd frontend
npm run dev
```

预期: 服务启动成功，访问 http://localhost:3000

**Step 2: 测试创建任务流程**

1. 登录系统
2. 进入任务管理页面
3. 点击"创建任务"
4. 选择多个执行人（来自不同系部）
5. 为每个系部配置审批人
6. 提交创建

预期:
- 任务创建成功
- 任务列表只显示1条记录
- 可以点击详情查看卡片式展示

---

### Task 7.3: 端到端测试

**Step 1: 创建测试任务**

使用Postman或curl测试:
```bash
curl -X POST http://localhost:8080/api/tasks \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "测试班会任务",
    "description": "测试多人任务和审批流程",
    "priority": 1,
    "dueDate": "2025-12-30T18:00:00",
    "assigneeIds": [101, 102, 103],
    "approvalConfigs": [
      {
        "departmentId": 10,
        "departmentName": "计算机系",
        "levels": [
          {"level": 1, "approverId": 123, "approverName": "张主任", "approverRole": "系领导"},
          {"level": 2, "approverId": 456, "approverName": "王处长", "approverRole": "学工处"}
        ]
      }
    ]
  }'
```

预期: 返回200，任务创建成功

**Step 2: 查询任务详情**

```bash
curl http://localhost:8080/api/tasks/1/detail \
  -H "Authorization: Bearer YOUR_TOKEN"
```

预期: 返回完整的任务详情，包含执行人卡片数据

**Step 3: 提交任务**

班主任登录后提交:
```bash
curl -X POST http://localhost:8080/api/tasks/1/submit \
  -H "Authorization: Bearer ASSIGNEE_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "content": "已完成任务",
    "attachmentIds": []
  }'
```

预期: 提交成功，状态变为"待审核"

**Step 4: 审批任务**

系领导登录后审批:
```bash
curl -X POST http://localhost:8080/api/tasks/approvals/1/approve \
  -H "Authorization: Bearer APPROVER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "approved": true,
    "comment": "同意"
  }'
```

预期: 审批成功，进入下一级审批

---

## Phase 8: 最终提交

### Task 8.1: 最终代码审查

**Step 1: 检查代码质量**

运行命令:
```bash
cd backend
mvn checkstyle:check

cd ../frontend
npm run lint
```

预期: 无严重错误

**Step 2: 运行所有测试**

```bash
cd backend
mvn test
```

预期: 所有测试通过（如果有测试）

---

### Task 8.2: 创建最终提交

**Step 1: 查看所有变更**

```bash
git status
git diff --stat
```

**Step 2: 创建最终提交**

```bash
git add .
git commit -m "$(cat <<'EOF'
feat: 重构任务管理系统支持多人任务和卡片式展示

数据库变更:
- 创建task_approval_configs表存储审批配置
- 修改tasks表移除执行人字段
- 修改task_assignees表添加部门和审批配置

后端变更:
- 重构Task和TaskAssignee实体类
- 创建TaskApprovalConfig实体和Mapper
- 重构TaskService.createTask支持审批配置
- 创建TaskApprovalService实现审批流程
- 修改任务详情API返回卡片数据

前端变更:
- 修改创建任务对话框添加审批配置表单
- 创建AssigneeCard执行人卡片组件
- 创建任务详情对话框显示卡片式布局

功能特性:
- 多人任务在列表中只显示1条记录
- 按系部分组显示执行人卡片
- 卡片显示审批进度链路
- 每个系部可配置不同的审批人
- 每个执行人独立审批流程

🤖 Generated with [Claude Code](https://claude.com/claude-code)

Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>
EOF
)"
```

---

## 实施完成！

**总结:**
- ✅ 数据库重构完成（2个DDL脚本）
- ✅ 后端实体类修改（3个实体类）
- ✅ Mapper和DTO开发（1个Mapper + 3个DTO）
- ✅ Service层重构（TaskService + TaskApprovalService）
- ✅ API接口重构（2个Controller方法）
- ✅ 前端UI重构（3个Vue组件）
- ✅ 测试和验证（端到端测试）

**预计工作量:** 6-8小时（包含测试和调试）

**下一步:** 根据实际使用情况优化性能和用户体验
