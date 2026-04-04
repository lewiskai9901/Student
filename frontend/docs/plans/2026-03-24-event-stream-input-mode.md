# EVENT_STREAM 快速事件记录模式

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 为 VIOLATION_RECORD 类型检查项添加 EVENT_STREAM 输入模式——搜索学生一键记录事件，自动按班级聚合计分。

**Architecture:** 在现有 VIOLATION_RECORD 基础上扩展，不新增评分模式。后端加 `input_mode` 字段区分录入方式；前端创建独立的 EventStreamRecorder 组件，在打分界面中根据 inputMode 选择渲染哪个组件。事件记录复用现有 `insp_violation_records` 表和 API。

**Tech Stack:** Spring Boot 3.2 + MyBatis Plus (backend), Vue 3 + TypeScript + Element Plus (frontend)

---

### Task 1: 数据库迁移 — 添加 input_mode 列

**Files:**
- Create: `database/schema/V65.0.0__template_item_input_mode.sql`
- Modify: `database/schema/V30.0.0__insp_v7_template_schema.sql` (在建表 SQL 中同步加列，保持一致)

**Step 1: 创建迁移文件**

```sql
-- V65.0.0__template_item_input_mode.sql
ALTER TABLE insp_template_items
  ADD COLUMN input_mode VARCHAR(30) DEFAULT 'INLINE' COMMENT '输入模式: INLINE(默认内嵌) | EVENT_STREAM(事件流快速录入)';
```

**Step 2: 执行迁移**

Run: `mysql -u root -p123456 student_management < database/schema/V65.0.0__template_item_input_mode.sql`
Expected: Query OK

**Step 3: 验证**

Run: `mysql -u root -p123456 student_management -e "DESCRIBE insp_template_items" | grep input_mode`
Expected: `input_mode	varchar(30)	YES		INLINE`

---

### Task 2: 后端 — TemplateItem 领域模型和 PO 添加 inputMode

**Files:**
- Modify: `backend/src/main/java/com/school/management/domain/inspection/model/v7/template/TemplateItem.java`
- Modify: `backend/src/main/java/com/school/management/infrastructure/persistence/inspection/v7/template/TemplateItemPO.java` (如果是独立 PO)

**Step 1: 领域模型加字段**

在 TemplateItem.java 中找到字段声明区，添加：
```java
private String inputMode; // INLINE | EVENT_STREAM
```

在 Builder 中添加对应的 builder 方法。
在 update() 方法参数中加入 inputMode。

**Step 2: PO 加字段（如果 PO 独立于领域模型）**

在 TemplateItemPO 中添加：
```java
private String inputMode;
```

MyBatis Plus 会自动映射 `input_mode` 列。

**Step 3: 验证编译**

Run: `cd backend && JAVA_HOME="/c/Program Files/Java/jdk-17" PATH="/c/Program Files/Java/jdk-17/bin:/c/Program Files/apache-maven-3.9.11/bin:$PATH" mvn compile -DskipTests -q`
Expected: BUILD SUCCESS

---

### Task 3: 前端类型定义 — 添加 inputMode

**Files:**
- Modify: `frontend/src/types/insp/template.ts`

**Step 1: 在 TemplateItem interface 中添加**

```typescript
inputMode?: 'INLINE' | 'EVENT_STREAM'
```

**Step 2: 验证编译**

Run: `cd frontend && npx vite build 2>&1 | grep -E "error|ERROR|✓ built"`
Expected: `✓ built`

---

### Task 4: 模板编辑器 — 配置 inputMode

**Files:**
- Modify: `frontend/src/views/inspection/v7/templates/components/ItemEditor.vue`

**Step 1: 在 VIOLATION_RECORD 类型的编辑面板中添加 inputMode 选择**

找到 ItemEditor 中处理 item 属性编辑的区域。当 `itemType === 'VIOLATION_RECORD'` 时，显示一个额外的 radio group：

```html
<el-form-item v-if="item.itemType === 'VIOLATION_RECORD'" label="录入模式">
  <el-radio-group v-model="item.inputMode" @change="handleInputModeChange">
    <el-radio value="INLINE">内嵌录入（在打分卡片内逐条添加）</el-radio>
    <el-radio value="EVENT_STREAM">快速记录（独立搜索界面，适合门口查迟到等高频场景）</el-radio>
  </el-radio-group>
</el-form-item>
```

**Step 2: 确保保存时 inputMode 被提交到后端**

检查 item 保存 API 调用，确认 inputMode 字段会被包含在 request body 中。

**Step 3: 验证编译**

Run: `cd frontend && npx vite build 2>&1 | grep -E "error|ERROR|✓ built"`

---

### Task 5: 创建 EventStreamRecorder 组件

**Files:**
- Create: `frontend/src/views/inspection/v7/tasks/components/EventStreamRecorder.vue`

**这是核心组件，实现 mockup (`style-4-event-log.html`) 的功能。**

**Props:**
```typescript
interface Props {
  submissionId: number       // 当前 submission
  detailId: number           // 当前 submission detail
  sectionId?: number         // 可选 section 引用
  itemId?: number            // 模板 item ID
  targetId: number           // 当前检查目标 ID（用于过滤）
  targetName: string         // 目标名称
  disabled?: boolean         // 是否只读
  scoringConfig?: string     // 评分配置 JSON
}
```

**核心功能:**

1. **搜索学生** — 使用 `getSimpleUserList(keyword)` API（已存在）
2. **一键记录** — 调用 `inspViolationApi.create()` 创建 violation record
3. **实时流水** — 调用 `inspViolationApi.listBySubmission()` 加载已有记录
4. **撤销/删除** — 调用 `inspViolationApi.delete()`
5. **事件类型** — 从 item 的 config JSON 读取可用事件类型，默认 [迟到, 早退, 缺勤, 违纪]
6. **自动计分** — 根据 scoringConfig.deductPerViolation 自动计算每条记录的扣分值

**模板结构（参照 mockup）:**
```
<div class="event-recorder">
  <!-- 事件类型选择 -->
  <div class="event-type-pills">...</div>
  <!-- 搜索输入 -->
  <div class="event-search">
    <el-input ref="searchRef" v-model="searchKeyword" ... />
    <!-- 搜索结果下拉 -->
    <div v-if="searchResults.length" class="search-results">
      <div v-for="user in searchResults" @click="recordEvent(user)">...</div>
    </div>
  </div>
  <!-- 记录流水 -->
  <div class="event-log">
    <div class="log-header">今日记录 ({{ records.length }}人)</div>
    <div v-for="record in filteredRecords" class="log-item">
      <span class="log-time">{{ formatTime(record.occurredAt) }}</span>
      <span class="log-name">{{ record.userName }}</span>
      <span class="log-class">{{ record.classInfo }}</span>
      <span class="log-type">{{ record.description }}</span>
      <button v-if="!disabled" @click="revokeRecord(record)">撤销</button>
    </div>
  </div>
  <!-- 统计 -->
  <div class="event-stats">...</div>
</div>
```

**关键实现细节:**
- `recordEvent(user)` 调用后自动清空搜索、重新聚焦搜索框
- `searchKeyword` 的 watch 防抖 300ms 调用 API
- 记录按时间倒序显示
- 统计区汇总各事件类型数量

**Step 1: 创建组件文件**（完整代码）

**Step 2: 验证编译**

Run: `cd frontend && npx vite build 2>&1 | grep -E "error|ERROR|✓ built"`

---

### Task 6: 打分界面集成 — 根据 inputMode 渲染不同组件

**Files:**
- Modify: `frontend/src/views/inspection/v7/tasks/TaskExecutionView.vue`

**Step 1: 导入 EventStreamRecorder 组件**

```typescript
import EventStreamRecorder from './components/EventStreamRecorder.vue'
```

**Step 2: 在评分卡片中，检测 VIOLATION_RECORD + EVENT_STREAM 时渲染新组件**

在 targetSectionGroups 的 field card v-for 循环中，找到现有的 ViolationRecordInput 渲染位置（或添加新的条件分支）。

在 scoring controls 的 v-if/v-else-if 链末尾，修改 VIOLATION_RECORD 的处理：

```html
<!-- VIOLATION_RECORD items -->
<template v-if="detail.itemType === 'VIOLATION_RECORD'">
  <!-- EVENT_STREAM mode: full-width recorder -->
  <EventStreamRecorder
    v-if="detail.inputMode === 'EVENT_STREAM'"
    :submission-id="group.submission.id"
    :detail-id="detail.id"
    :section-id="detail.sectionId"
    :item-id="detail.templateItemId"
    :target-id="selectedTargetId!"
    :target-name="currentTargetName"
    :disabled="!isGroupEditable(group)"
    :scoring-config="detail.scoringConfig"
  />
  <!-- INLINE mode: existing component -->
  <ViolationRecordInput
    v-else
    :submission-id="group.submission.id"
    :detail-id="detail.id"
    :section-id="detail.sectionId"
    :item-id="detail.templateItemId"
    :disabled="!isGroupEditable(group)"
  />
</template>
```

注意：EVENT_STREAM 模式的卡片应该占满整行（跳出 grid），可用 CSS：
```css
.score-card--fullwidth {
  grid-column: 1 / -1;
}
```

给包含 EventStreamRecorder 的 score-card 添加此 class。

**Step 3: 确保 SubmissionDetail 类型包含 inputMode 和 itemType**

检查 `SubmissionDetail` 类型定义，确认有 `inputMode` 和 `itemType` 字段。如果缺少，在类型定义中添加。

**Step 4: 验证编译**

Run: `cd frontend && npx vite build 2>&1 | grep -E "error|ERROR|✓ built"`

**Step 5: Commit**

```bash
git add -A
git commit -m "feat: EVENT_STREAM input mode for VIOLATION_RECORD items"
```

---

## 验收标准

1. **模板配置** — VIOLATION_RECORD 类型检查项可选择 INLINE 或 EVENT_STREAM 模式
2. **打分界面** — EVENT_STREAM 模式显示独立的搜索+记录组件，不是打分卡片内嵌
3. **搜索** — 输入学生姓名实时搜索，显示匹配结果
4. **一键记录** — 点击记录按钮，自动创建 violation record，清空搜索
5. **实时流水** — 已记录事件按时间倒序显示，支持撤销
6. **自动计分** — 根据 scoringConfig.deductPerViolation 自动计算每条记录扣分
7. **数据复用** — 使用现有 violation_records 表和 API，不创建新表
