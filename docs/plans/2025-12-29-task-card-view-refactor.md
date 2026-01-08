# 任务卡片式展示重构实施计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 实现任务管理的卡片式展示，支持多系部独立审批流程

**Architecture:**
- 1个任务显示1条记录，通过卡片展示多个执行人状态
- 每个班主任独立走审批流程（班主任提交 → 系领导审核 → 学工处审核）
- 系领导只能看到自己系部的待审批任务

**Tech Stack:** Spring Boot 3.2, MyBatis Plus, Vue 3, TypeScript, Tailwind CSS

---

## Task 1: 修复数据库表结构

**Files:**
- Create: `database/migrations/2025-12-29-fix-tasks-table.sql`

**Step 1: 创建迁移脚本**

```sql
-- 为tasks表添加缺失的status字段
ALTER TABLE tasks
ADD COLUMN status TINYINT DEFAULT 0 COMMENT '状态: 0-待接收, 1-进行中, 2-待审核, 3-已完成, 4-已打回, 5-已取消, 6-审批中' AFTER assign_type;

-- 添加索引
ALTER TABLE tasks ADD INDEX idx_status (status);
```

**Step 2: 执行迁移**

Run: `mysql -u root -p123456 student_management < database/migrations/2025-12-29-fix-tasks-table.sql`

**Step 3: 验证**

Run: `mysql -u root -p123456 -e "DESCRIBE student_management.tasks;" | grep status`
Expected: 显示status字段

---

## Task 2: 优化Task实体和DTO

**Files:**
- Modify: `backend/src/main/java/com/school/management/entity/task/Task.java`
- Modify: `backend/src/main/java/com/school/management/dto/task/TaskDTO.java`

**Step 1: 确认Task实体有status字段**

Task实体应该包含:
```java
private Integer status;
```

**Step 2: 确认TaskDTO包含卡片展示所需字段**

```java
// 批量任务进度
private Integer totalAssignees;
private Integer submittedAssignees;
private Integer completedAssignees;
private List<TaskAssigneeDTO> assignees;
```

---

## Task 3: 实现任务详情卡片式API

**Files:**
- Modify: `backend/src/main/java/com/school/management/service/task/impl/TaskServiceImpl.java`
- Modify: `backend/src/main/java/com/school/management/dto/task/TaskDetailDTO.java`

**Step 1: 创建卡片数据结构**

TaskDetailDTO应包含:
- 任务基本信息
- 执行人卡片列表（按系部分组）
- 流程进度节点

**Step 2: 实现getTaskDetail方法**

返回完整的任务详情，包含所有执行人的状态卡片

---

## Task 4: 优化待审批查询（按系部过滤）

**Files:**
- Modify: `backend/src/main/java/com/school/management/service/task/impl/TaskApprovalServiceImpl.java`

**Step 1: 修改getMyPendingApprovals方法**

系领导只能看到自己系部的待审批任务:
- 获取当前用户的系部ID
- 过滤task_approval_records中department_id匹配的记录

---

## Task 5: 前端任务列表优化

**Files:**
- Modify: `frontend/src/views/task/TaskList.vue`

**Step 1: 任务列表显示优化**

- 1个任务1条记录
- 显示执行进度条：已提交X/总Y人
- 点击进入详情页

---

## Task 6: 前端卡片式详情页

**Files:**
- Create: `frontend/src/views/task/TaskDetail.vue`
- Create: `frontend/src/components/task/AssigneeCardGrid.vue`

**Step 1: 创建执行人卡片网格组件**

类似电影院选座:
- 按系部分组显示
- 每个卡片显示：姓名、状态颜色
- 悬停显示详情

**Step 2: 创建任务详情页**

- 任务基本信息
- 卡片网格
- 流程进度时间线

---

## Task 7: 测试验证

**Step 1: 启动后端服务**

Run: `cd backend && mvn spring-boot:run -DskipTests`

**Step 2: 启动前端服务**

Run: `cd frontend && npm run dev`

**Step 3: 功能测试**

- 创建批量任务
- 验证任务列表显示1条
- 验证卡片展示
- 验证系领导只看到本系部待审批

---

## 执行顺序

1. Task 1: 修复数据库 (必须先完成)
2. Task 2-4: 后端API优化 (可并行)
3. Task 5-6: 前端组件 (依赖后端完成)
4. Task 7: 测试验证
