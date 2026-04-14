# /my/dashboard 教师工作台 — 设计文档

**日期**: 2026-04-14
**作者**: Claude + Owner
**状态**: 已定稿，待实现
**依赖**: Phase 0-2 Access Control Refactor (已完成)

## 1. 背景

Phase 0-2 完成了三层访问控制 + PermissionScope 分层 + UserType 激活（defaultRoleCodes 自动分配、features capability 标志、my:* SELF 权限种子）。基础设施已就位，但**无消费者**—老师登录后无可用首页。本设计兑现业务价值：教师登录后看到今日课表、我的班级、代课待办，端到端验证 Phase 0-2 的整条链路。

## 2. 目标与非目标

**目标**
- 老师登录自动落地 `/my/dashboard`，三个核心信息一眼可见
- 消费 Phase 2.3 种下的 5 条 SELF 权限
- 端到端验证 Phase 1 的 scope 分层 + Phase 2 的 capability 映射

**非目标**
- 不做作业批改、消息中心、公告系统
- 不做 widget 拖拽 / 自定义布局
- 不做学生端 / 辅导员端 dashboard（未来另做）

## 3. 架构决策

### 3.1 页面定位

- 组件名 `MyDashboard.vue`（通用名，非 TeacherDashboard），路由 `/my/dashboard`，所有登录用户可访问
- 本期内部仅实现 TEACHER 视图：`if (userType === 'TEACHER')` 渲染 3 张卡片；其它用户看空态 + 跳原 Dashboard 链接
- **不**做 widget 注册表 / 插件槽位—YAGNI。学生 / 辅导员页未来很可能非卡片式，强抽象救不了他们

### 3.2 视觉风格

参考 Linear "My Issues" + GitHub Dashboard + 钉钉工作台。项目既定紧凑美学：
- 无彩色图标方块、无 emoji 装饰
- 统计条用竖线分隔 + 纯文字
- 列表行无装饰图标
- 当前时段用 `●` 实心圆 + 高亮色；未开始用 `○` 空心圆

### 3.3 布局

```
┌────────────────────────────────────────────────────┐
│ 早上好，张老师 │ 今天 周三 4月15日 · 第8周            │
│ 今日 3节课 │ 待批 12份 │ 代课 1 │ 本周课时 12/12     │
├────────────────────────────────────────────────────┤
│ 今日课表                              查看完整 →    │
│ ● 08:00─09:40  高数·计算机21-1  教A-301    [进入]  │
│ ○ 10:00─11:40  高数·计算机21-2  教A-302    [签到]  │
│ ○ 14:00─15:40  线代·信管22-1    教B-205    [签到]  │
├────────────────────────┬───────────────────────────┤
│ 我的班级         查看→ │ 待办                       │
│ 计算机21-1 48人·班主任 │ 代课·数学 周四3-4         │
│ 计算机21-2 46人        │ 李老师 · 2小时前           │
│ 信管22-1   42人        │ [接受] [拒绝]              │
│ 信管22-2   44人        │                            │
└────────────────────────┴───────────────────────────┘
```

## 4. 后端接口

新增 4 个接口，全部 `/my/*` 前缀，全部 `@CasbinAccess(scope=SELF)`。

| 接口 | 权限码 | 数据源 | 用途 |
|------|--------|--------|------|
| `GET /my/dashboard/summary` | `my:schedule:view` | 聚合 SQL | 头部统计条 |
| `GET /my/schedule/today?date=` | `my:schedule:view` | `teaching_instances` | 今日课表 |
| `GET /my/classes` | `my:students:view` | `teacher_assignments` + `school_classes` | 我的班级 |
| `GET /my/tasks/substitute` | `my:substitute:view` | 代课任务表 | 代课待办 |

### 4.1 关键设计

- **不依赖 `@DataPermission`，接口内硬过滤**：SELF 语义是 `user_id = currentUser`，接口实现直接用 `SecurityUtils.requireCurrentUserId()`，绕开 DataPermission 的 org 维度
- **优先查 `teaching_instances`**：物化每天一行，原生包含代课/调课。若当日无数据则回退 `schedule_entries` 周模板
- **`teacher_id = User.id`**：现 schema 老师的教学主体 ID 就是 User.id，不另设 Teacher 聚合
- **扁平化 DTO**：一次请求够渲染，不让前端再 join 课程名/班级名/教室名
- **Summary 不编造指标**：无作业批改模块则不出"待批"数

### 4.2 DTO 形态（示意）

```java
record TodayLessonDTO(Long instanceId, LocalTime startTime, LocalTime endTime,
                      String courseName, String className, String classroomName,
                      LessonStatus status, boolean canSign) {}

record MyClassDTO(Long classId, String className, Integer studentCount,
                  boolean isHeadTeacher, List<String> subjects) {}

record SubstituteTaskDTO(Long taskId, String courseName, LocalDate scheduledDate,
                         Integer period, String requesterName, Instant requestedAt) {}

record DashboardSummaryDTO(int todayLessons, int weeklyHoursCurrent,
                           int weeklyHoursTotal, int substituteRequests) {}
```

## 5. 权限链路（SELF 硬保障）

**三道门，任何一道漏就是漏洞。**

### 门 1：Casbin 前置（`CasbinAccessInterceptor`）
`@CasbinAccess(resource="my:schedule", action="view")` 要求用户有该 code；SELF 权限码跳过"任一 MANAGEMENT 权限"要求

### 门 2：方法内硬过滤
- 每个 `/my/*` 方法首行：`Long userId = SecurityUtils.requireCurrentUserId();`
- 所有查询带 `WHERE teacher_id = :userId`
- **禁止**参数列表接收 `teacherId`

### 门 3：ArchUnit 兜底
新增 `ArchUnitMyEndpointTest`：
- 路径以 `/my/` 开头的方法必须调用 `SecurityUtils.requireCurrentUserId()`
- 这些方法不得有 `@RequestParam Long teacherId` 等身份参数

## 6. 前端架构

```
frontend/src/views/my/
├── MyDashboard.vue                # 路由组件
├── components/
│   ├── DashboardHeader.vue
│   ├── TodayScheduleCard.vue
│   ├── MyClassesCard.vue
│   └── PendingTasksCard.vue
frontend/src/api/my.ts
frontend/src/types/my.ts
```

**路由**
```ts
{ path: '/my/dashboard', component: MyDashboard,
  meta: { title: '工作台', requiresAuth: true, order: 1, group: 'daily' } }
```

**登录跳转**：`LoginView.vue` 成功后 `userType === 'TEACHER'` → `/my/dashboard`，否则 `/dashboard`

**退化策略**（4 种空态）
1. 非老师用户：三卡片空态 + 跳原 Dashboard 链接
2. 无今日课（周末/假期）：课表卡显示"今天没有课"
3. 无班级：班级卡显示"尚未被分配班级·联系教务"
4. 接口失败：每卡独立错误态 + 重试，不让单接口把整页打黑

**不做**：骨架屏、widget 拖拽、Service Worker

## 7. 测试

**后端**
- 4 个 Controller 单测：SELF 拒绝非本人数据（Mock 他人 userId 入 SecurityUtils，期空）
- 1 个 ArchUnit 测试（门 3）
- 集成测试：真 DB，两 TEACHER 账号互访对方 endpoint 应空

**前端**
- Playwright E2E 1 条：登录 TEACHER → `/my/dashboard` → 断言 3 卡片 DOM + 统计数字

**手工验收**
1. TEACHER 登录自动跳 `/my/dashboard`
2. 头部统计条显示正确
3. 当前节高亮 + 其它按时间升序
4. 班级数量与 DB 一致
5. 无代课时显示空态
6. DB 删掉课表数据页面不崩
7. ADMIN 访问 `/my/dashboard` 空态不报 403

## 8. 落地节奏

约 2 天：

| 时段 | 任务 | 估时 |
|------|------|------|
| D1 上 | 后端 4 接口 + `@CasbinAccess` + ArchUnit | 4h |
| D1 下 | 接口联调 + 单测 + 集成测试 | 3h |
| D2 上 | 前端 5 组件 + 路由 + 登录跳转 | 4h |
| D2 下 | E2E + 手工验收 + bug 修复 | 3h |

## 9. 验收矩阵

| 维度 | 通过条件 |
|------|----------|
| 业务 | TEACHER 登录一眼看到课表/班级/待办 |
| 权限 | SELF scope 三道门全开，无横向越权 |
| 性能 | 首屏 ≤ 1.5s（局部内网） |
| 退化 | 4 种空态均友好不崩 |
| 架构 | ArchUnit CI 通过，Phase 1-2 链路被消费 |
