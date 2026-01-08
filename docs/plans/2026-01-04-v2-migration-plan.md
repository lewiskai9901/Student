# V2 完整迁移计划

## 一、当前状态总览

### 迁移进度统计
| 指标 | 完成度 | 说明 |
|------|--------|------|
| 后端API控制器 | 9/68 (13%) | V2仅实现9个控制器 |
| 后端应用服务 | 8个框架 (30%) | 大部分仅有框架 |
| 后端领域模型 | 6/12模块 (50%) | 核心领域已建立 |
| 前端视图 | 6/16模块 (37%) | 基础视图已完成 |
| **整体完成度** | **~25%** | |

### 各模块状态
| 模块 | 后端 | 前端 | 总体 | 优先级 |
|------|------|------|------|--------|
| 组织管理 | ✅ 100% | ✅ 100% | ✅ 完成 | - |
| 权限管理 | ✅ 100% | ✅ 100% | ✅ 完成 | - |
| 量化检查 | 🟡 70% | 🟡 60% | 🟡 65% | P0 |
| 资产/宿舍 | 🟡 30% | 🟡 30% | 🟡 30% | P1 |
| 学生管理 | 🔴 20% | 🟡 40% | 🔴 30% | P0 |
| 任务管理 | 🔴 20% | 🟡 40% | 🔴 30% | P1 |
| 评估评分 | ❌ 0% | ❌ 0% | ❌ 0% | P2 |
| 教学管理 | ❌ 0% | ❌ 0% | ❌ 0% | P2 |
| 通知公告 | ❌ 0% | ❌ 0% | ❌ 0% | P3 |
| 文件管理 | ❌ 0% | ❌ 0% | ❌ 0% | P3 |

---

## 二、分阶段迁移计划

### 第一阶段：核心功能完善 (P0) - 预计3周

#### Phase 1.1: 学生管理模块完善 (5天)

**后端任务：**

1. **完善Student聚合根** (`domain/student/`)
   - [ ] Student实体完整属性
   - [ ] StudentStatus值对象
   - [ ] StudentDormitoryAssignment值对象
   - [ ] 领域事件: StudentEnrolled, StudentGraduated, DormitoryAssigned

2. **完善StudentRepository** (`infrastructure/persistence/student/`)
   - [ ] 分页查询实现
   - [ ] 条件查询（按班级、状态、宿舍等）
   - [ ] 批量操作支持

3. **扩展StudentApplicationService**
   ```java
   // 需要实现的命令
   - EnrollStudentCommand      // 学生入学
   - UpdateStudentCommand      // 更新信息
   - GraduateStudentCommand    // 毕业处理
   - AssignDormitoryCommand    // 分配宿舍
   - TransferClassCommand      // 转班
   - ImportStudentsCommand     // 批量导入
   - ExportStudentsCommand     // 导出
   ```

4. **创建StudentController** (`interfaces/rest/student/`)
   ```
   POST   /v2/students              创建学生
   GET    /v2/students              分页查询
   GET    /v2/students/{id}         获取详情
   PUT    /v2/students/{id}         更新学生
   DELETE /v2/students/{id}         删除学生
   POST   /v2/students/import       批量导入
   GET    /v2/students/export       导出
   POST   /v2/students/{id}/dormitory  分配宿舍
   POST   /v2/students/{id}/graduate   毕业处理
   GET    /v2/students/statistics   统计信息
   ```

**前端任务：**

1. **完善StudentListView.vue**
   - [ ] 高级搜索（学号、姓名、班级、状态）
   - [ ] 批量操作（删除、导出）
   - [ ] 导入Excel功能

2. **新建StudentDetailView.vue**
   - [ ] 学生详情展示
   - [ ] 信息编辑表单
   - [ ] 宿舍分配操作

3. **完善student.ts API**
   - [ ] 所有CRUD方法
   - [ ] 导入导出API
   - [ ] 统计API

---

#### Phase 1.2: 量化检查模块补完 (7天)

**后端任务：**

1. **创建CheckPlan聚合根** (`domain/inspection/`)
   - [ ] CheckPlan实体
   - [ ] CheckPlanStatus值对象
   - [ ] Inspector值对象（检查员）
   - [ ] 领域事件: PlanCreated, PlanStarted, PlanCompleted

2. **创建检查计划相关Controller**
   ```
   # CheckPlanController
   POST   /v2/inspection/plans           创建计划
   GET    /v2/inspection/plans           计划列表
   GET    /v2/inspection/plans/{id}      计划详情
   PUT    /v2/inspection/plans/{id}      更新计划
   POST   /v2/inspection/plans/{id}/start    启动计划
   POST   /v2/inspection/plans/{id}/complete 完成计划
   GET    /v2/inspection/plans/{id}/statistics 统计

   # DailyCheckController
   POST   /v2/inspection/daily-checks    创建日常检查
   GET    /v2/inspection/daily-checks    检查列表
   POST   /v2/inspection/daily-checks/{id}/score  打分
   ```

3. **扩展InspectionApplicationService**
   - [ ] 检查计划管理逻辑
   - [ ] 日常检查逻辑
   - [ ] 统计分析逻辑

4. **创建权重配置服务**
   ```
   GET    /v2/inspection/weight-configs          获取配置
   PUT    /v2/inspection/weight-configs          更新配置
   POST   /v2/inspection/weight-configs/calculate 计算权重
   ```

**前端任务：**

1. **新建检查计划视图** (`views/v2/inspection/`)
   - [ ] CheckPlanListView.vue - 计划列表
   - [ ] CheckPlanCreateView.vue - 创建计划
   - [ ] CheckPlanDetailView.vue - 计划详情

2. **新建日常检查视图**
   - [ ] DailyCheckView.vue - 日常检查
   - [ ] CheckScoringView.vue - 打分界面

3. **新建统计分析视图**
   - [ ] StatisticsView.vue - 统计分析
   - [ ] WeightConfigView.vue - 权重配置

4. **扩展inspection.ts API**
   - [ ] 检查计划API (10+方法)
   - [ ] 日常检查API (8+方法)
   - [ ] 权重配置API (5+方法)

---

#### Phase 1.3: 任务管理模块完善 (4天)

**后端任务：**

1. **完善Task聚合根**
   - [ ] Task实体完整实现
   - [ ] TaskStatus状态机
   - [ ] ApprovalRecord审批记录
   - [ ] 领域事件完善

2. **创建TaskController** (`interfaces/rest/task/`)
   ```
   POST   /v2/tasks                    创建任务
   GET    /v2/tasks                    任务列表
   GET    /v2/tasks/{id}               任务详情
   PUT    /v2/tasks/{id}               更新任务
   POST   /v2/tasks/{id}/assign        分配任务
   POST   /v2/tasks/{id}/accept        接受任务
   POST   /v2/tasks/{id}/submit        提交任务
   POST   /v2/tasks/{id}/approve       审批任务
   POST   /v2/tasks/{id}/reject        驳回任务
   GET    /v2/tasks/my                 我的任务
   GET    /v2/tasks/pending-approval   待审批
   GET    /v2/tasks/statistics         统计
   ```

3. **创建WorkflowController**
   ```
   POST   /v2/workflows                创建流程
   GET    /v2/workflows                流程列表
   GET    /v2/workflows/{id}           流程详情
   PUT    /v2/workflows/{id}           更新流程
   ```

**前端任务：**

1. **完善TaskListView.vue**
   - [ ] 高级筛选
   - [ ] 状态标签
   - [ ] 操作按钮

2. **新建任务详情视图**
   - [ ] TaskDetailView.vue
   - [ ] TaskApprovalView.vue
   - [ ] WorkflowDesignerView.vue

---

### 第二阶段：重要功能迁移 (P1) - 预计3周

#### Phase 2.1: 资产/宿舍管理完善 (5天)

**后端：**
```
# BuildingController
/v2/buildings/*

# DormitoryController (扩展)
/v2/dormitories/overview      宿舍总览
/v2/dormitories/allocation    分配管理
/v2/dormitories/statistics    统计

# ClassroomController
/v2/classrooms/*
```

**前端：**
- [ ] BuildingListView.vue
- [ ] DormitoryOverviewView.vue
- [ ] DormitoryAllocationView.vue
- [ ] ClassroomListView.vue

---

#### Phase 2.2: 教学管理模块 (5天)

**后端：**
1. 创建Teaching领域模块
   ```
   domain/teaching/
   ├── model/
   │   ├── Grade.java
   │   ├── Major.java
   │   ├── MajorDirection.java
   │   └── Semester.java
   ├── repository/
   └── service/
   ```

2. 创建控制器
   ```
   /v2/teaching/grades/*
   /v2/teaching/majors/*
   /v2/teaching/major-directions/*
   /v2/teaching/semesters/*
   ```

**前端：**
- [ ] GradeListView.vue
- [ ] MajorListView.vue
- [ ] SemesterListView.vue

---

#### Phase 2.3: 系统管理增强 (4天)

**后端：**
```
# UserController (V2)
/v2/users/*

# OperationLogController (V2)
/v2/operation-logs/*

# SystemConfigController (V2)
/v2/system/configs/*
```

**前端：**
- [ ] UserListView.vue (V2)
- [ ] OperationLogView.vue (V2)
- [ ] SystemConfigView.vue (V2)

---

### 第三阶段：辅助功能迁移 (P2) - 预计2周

#### Phase 3.1: 评估评分系统 (7天)

**后端：创建Evaluation领域**
```
domain/evaluation/
├── model/
│   ├── EvaluationPeriod.java    评估周期
│   ├── EvaluationResult.java    评估结果
│   ├── StudentScore.java        学生成绩
│   ├── HonorApplication.java    荣誉申报
│   └── Course.java              课程
├── repository/
└── service/
```

**控制器：**
```
/v2/evaluation/periods/*         评估周期
/v2/evaluation/results/*         评估结果
/v2/evaluation/scores/*          成绩管理
/v2/evaluation/honors/*          荣誉申报
/v2/evaluation/courses/*         课程管理
```

**前端：**
- [ ] EvaluationPeriodView.vue
- [ ] EvaluationResultView.vue
- [ ] StudentScoreView.vue
- [ ] HonorApplicationView.vue
- [ ] CourseManagementView.vue

---

#### Phase 3.2: 通知公告系统 (3天)

**后端：**
```
domain/notification/
/v2/notifications/*
/v2/announcements/*
```

**前端：**
- [ ] NotificationListView.vue
- [ ] AnnouncementListView.vue

---

### 第四阶段：扩展功能迁移 (P3) - 预计1周

#### Phase 4.1: 文件管理 (2天)
```
/v2/files/upload
/v2/files/download/{id}
/v2/files/list
```

#### Phase 4.2: 统计分析 (2天)
```
/v2/statistics/dashboard
/v2/statistics/analysis-configs
/v2/statistics/reports
```

#### Phase 4.3: 微信集成 (2天)
```
/v2/wechat/config
/v2/wechat/push
/v2/miniapp/auth
```

---

## 三、技术规范

### 3.1 后端开发规范

**Controller层：**
```java
@Tag(name = "模块名 V2", description = "描述")
@RestController("controllerNameV2")  // 避免Bean冲突
@RequestMapping("/v2/xxx")
@RequiredArgsConstructor
public class XxxController {

    private final XxxApplicationService applicationService;
    private final XxxQueryService queryService;

    @Operation(summary = "操作描述")
    @PostMapping
    @PreAuthorize("hasAuthority('xxx:create')")
    public Result<Long> create(@Valid @RequestBody CreateXxxRequest request) {
        // 调用应用服务
    }
}
```

**Application层：**
```java
@Service
@RequiredArgsConstructor
@Transactional
public class XxxApplicationService {

    private final XxxRepository repository;
    private final DomainEventPublisher eventPublisher;

    public Long create(CreateXxxCommand command) {
        // 1. 验证
        // 2. 创建聚合根
        // 3. 保存
        // 4. 发布事件
    }
}
```

**Domain层：**
```java
public class Xxx extends AggregateRoot<Long> {
    // 属性
    private XxxId id;
    private XxxStatus status;

    // 工厂方法
    public static Xxx create(...) { }

    // 业务方法
    public void doSomething() {
        // 业务逻辑
        registerEvent(new XxxEvent(...));
    }
}
```

### 3.2 前端开发规范

**视图组件：**
```vue
<template>
  <div class="xxx-view">
    <!-- 搜索区域 -->
    <!-- 操作按钮 -->
    <!-- 数据表格 -->
    <!-- 分页 -->
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { xxxApi } from '@/api/v2/xxx'
import type { XxxDTO, XxxQueryRequest } from '@/types/v2/xxx'

// 状态
const loading = ref(false)
const dataList = ref<XxxDTO[]>([])

// 方法
const fetchData = async () => {
  loading.value = true
  try {
    const res = await xxxApi.getList(queryParams.value)
    dataList.value = res.data.records
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchData()
})
</script>
```

**API模块：**
```typescript
// api/v2/xxx.ts
import request from '@/utils/request'
import type { XxxDTO, CreateXxxRequest } from '@/types/v2/xxx'

const BASE_URL = '/v2/xxx'

export const xxxApi = {
  getList: (params: XxxQueryRequest) =>
    request.get<PageResult<XxxDTO>>(BASE_URL, { params }),

  getById: (id: number) =>
    request.get<XxxDTO>(`${BASE_URL}/${id}`),

  create: (data: CreateXxxRequest) =>
    request.post<number>(BASE_URL, data),

  update: (id: number, data: UpdateXxxRequest) =>
    request.put(`${BASE_URL}/${id}`, data),

  delete: (id: number) =>
    request.delete(`${BASE_URL}/${id}`)
}
```

---

## 四、迁移检查清单

### 每个模块迁移完成标准：

- [ ] **后端**
  - [ ] 领域模型完整（实体、值对象、事件）
  - [ ] Repository接口和实现
  - [ ] Application Service（Command/Query分离）
  - [ ] REST Controller（完整CRUD + 业务操作）
  - [ ] DTO定义完整
  - [ ] 权限注解配置
  - [ ] 单元测试覆盖

- [ ] **前端**
  - [ ] 列表视图（搜索、分页、操作）
  - [ ] 详情/编辑视图
  - [ ] API模块完整
  - [ ] TypeScript类型定义
  - [ ] 路由配置
  - [ ] 权限控制

- [ ] **集成**
  - [ ] 前后端联调通过
  - [ ] 功能测试通过
  - [ ] 与V1功能对等

---

## 五、时间规划

| 阶段 | 内容 | 预计时间 | 里程碑 |
|------|------|----------|--------|
| Phase 1.1 | 学生管理 | 5天 | 学生CRUD完整 |
| Phase 1.2 | 量化检查 | 7天 | 检查计划完整 |
| Phase 1.3 | 任务管理 | 4天 | 任务流程完整 |
| **P0总计** | | **16天 (~3周)** | **核心功能完成** |
| Phase 2.1 | 资产/宿舍 | 5天 | |
| Phase 2.2 | 教学管理 | 5天 | |
| Phase 2.3 | 系统管理 | 4天 | |
| **P1总计** | | **14天 (~3周)** | **重要功能完成** |
| Phase 3.1 | 评估评分 | 7天 | |
| Phase 3.2 | 通知公告 | 3天 | |
| **P2总计** | | **10天 (~2周)** | **辅助功能完成** |
| Phase 4 | 扩展功能 | 6天 | |
| **P3总计** | | **6天 (~1周)** | **全部完成** |
| **总计** | | **46天 (~9周)** | |

---

## 六、风险与注意事项

### 6.1 技术风险
1. **数据兼容性** - V1/V2数据结构差异，需要数据迁移脚本
2. **事务一致性** - 跨聚合根操作需要Saga模式
3. **性能问题** - DDD可能带来N+1查询，需优化

### 6.2 迁移策略
1. **渐进式迁移** - 每完成一个模块立即上线使用
2. **双轨运行** - V1/V2并行，逐步切换
3. **回滚方案** - 保留V1代码，遇问题可快速回滚

### 6.3 注意事项
1. V2 Controller必须添加唯一Bean名称
2. 前端路由需要配置hidden:false启用
3. 权限码需要与V1保持一致或做映射
4. 接口返回格式需要统一

---

## 七、立即开始的任务

### 今日任务 (Day 1)

1. **后端：完善Student聚合根**
   - 文件：`domain/student/model/aggregate/Student.java`
   - 添加完整属性和业务方法

2. **后端：创建StudentController**
   - 文件：`interfaces/rest/student/StudentController.java`
   - 实现基础CRUD端点

3. **前端：启用V2学生管理路由**
   - 修改：`router/v2.ts`
   - 移除hidden:true

4. **前端：完善StudentListView**
   - 添加搜索、分页、操作按钮
