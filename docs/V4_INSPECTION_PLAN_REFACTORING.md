# V4 检查计划模块重构方案

> 版本: 1.0
> 日期: 2026-01-30
> 状态: 待评审

---

## 一、重构背景与目标

### 1.1 当前问题

| 问题 | 影响 | 严重程度 |
|------|------|----------|
| 工作流程过长 | 创建一次检查需4步操作 | 高 |
| Plan与Session概念混淆 | 用户理解成本高 | 中 |
| 临时检查无法快速执行 | 限制使用场景 | 高 |
| UI交互繁琐 | 创建Session需配置5+选项 | 中 |

### 1.2 重构目标

```
目标1: 简化工作流程
  - 快速检查: 2步完成 (选模板 → 开始检查)
  - 计划检查: 保留完整流程,但优化体验

目标2: 保持功能完整性
  - 兼容现有排名、申诉、整改、分析等模块
  - 保留周期性计划管理能力

目标3: 渐进式改进
  - 不破坏现有数据结构
  - 分阶段实施,可回滚
```

---

## 二、整体架构设计

### 2.1 新的模块结构

```
┌─────────────────────────────────────────────────────────────────┐
│                     检查模块入口 (新)                            │
│  /inspection/dashboard                                          │
├─────────────────────┬───────────────────────────────────────────┤
│                     │                                           │
│   ┌─────────────┐   │   ┌─────────────────────────────────┐    │
│   │  快速检查    │   │   │        计划管理                  │    │
│   │  (新增)     │   │   │      (优化现有)                  │    │
│   │             │   │   │                                  │    │
│   │ 选模板      │   │   │  检查计划列表                    │    │
│   │   ↓         │   │   │       ↓                         │    │
│   │ 一键开始    │   │   │  计划详情 (统计+会话列表)        │    │
│   │   ↓         │   │   │       ↓                         │    │
│   │ 执行打分    │   │   │  新建检查会话                    │    │
│   │             │   │   │       ↓                         │    │
│   └─────────────┘   │   │  执行打分                        │    │
│                     │   └─────────────────────────────────┘    │
└─────────────────────┴───────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                   检查执行 (InspectionExecute)                   │
│                   (统一入口,支持多种模式)                        │
└─────────────────────────────────────────────────────────────────┘
                              │
           ┌──────────────────┼──────────────────┐
           ▼                  ▼                  ▼
    ┌────────────┐    ┌────────────┐    ┌────────────┐
    │  排名结果   │    │  申诉管理   │    │  数据分析   │
    └────────────┘    └────────────┘    └────────────┘
```

### 2.2 数据模型调整

**保持现有模型不变,新增关联字段:**

```sql
-- InspectionSession 新增字段
ALTER TABLE inspection_session ADD COLUMN quick_mode TINYINT DEFAULT 0 COMMENT '是否快速检查模式';
ALTER TABLE inspection_session ADD COLUMN plan_id BIGINT NULL COMMENT '关联的计划ID(快速模式为null)';

-- 索引优化
CREATE INDEX idx_session_plan ON inspection_session(plan_id);
CREATE INDEX idx_session_quick ON inspection_session(quick_mode, inspection_date);
```

**关键设计原则:**
- 快速检查: `plan_id = NULL, quick_mode = 1`
- 计划检查: `plan_id = {计划ID}, quick_mode = 0`

---

## 三、功能详细设计

### 3.1 检查入口页面 (新增)

**路由:** `/inspection/dashboard`

**页面布局:**
```
┌─────────────────────────────────────────────────────────────────┐
│  量化检查                                          [设置] [帮助] │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌──────────────────────┐  ┌──────────────────────────────────┐│
│  │     快速开始检查      │  │        今日检查概览              ││
│  │                      │  │                                  ││
│  │  [卫生检查]          │  │   已完成: 3    进行中: 1         ││
│  │  [纪律检查]          │  │   待发布: 2    申诉中: 5         ││
│  │  [安全检查]          │  │                                  ││
│  │  ...                 │  │   [查看全部检查记录 →]           ││
│  │                      │  │                                  ││
│  │  [+ 选择其他模板]     │  └──────────────────────────────────┘│
│  └──────────────────────┘                                       │
│                                                                 │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  功能入口                                                 │  │
│  │                                                          │  │
│  │  [检查计划]  [排名结果]  [申诉管理]  [数据分析]  [导出中心]│  │
│  │  [整改工单]  [学生行为]  [评级统计]  [班主任台]  [配置中心]│  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                 │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  最近检查记录                                              │  │
│  │  ┌────────────────────────────────────────────────────┐  │  │
│  │  │ 1月30日 卫生检查 | 已发布 | 32个班级 | [查看] [继续] │  │  │
│  │  │ 1月29日 纪律检查 | 进行中 | 28个班级 | [查看] [继续] │  │  │
│  │  │ 1月28日 安全检查 | 已提交 | 30个班级 | [查看] [发布] │  │  │
│  │  └────────────────────────────────────────────────────┘  │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

**交互设计:**

1. **快速开始检查**
   - 显示最常用的3-5个模板(按使用频率排序)
   - 点击模板 → 弹出简化配置框 → 确认后直接进入打分页面

2. **简化配置框**
   ```
   ┌─────────────────────────────────────┐
   │  开始卫生检查                    [×] │
   ├─────────────────────────────────────┤
   │                                     │
   │  检查日期: [2026-01-30]  (默认今天)  │
   │                                     │
   │  检查名称: [1月30日 卫生检查]        │
   │            (自动生成,可修改)         │
   │                                     │
   │  ─────────────────────────────────  │
   │  [▼ 高级选项]                       │
   │    录入模式: ○物理空间 ●按班级 ○搜索 │
   │    计分模式: ●扣分制 ○基准分制       │
   │    基准分:   [100]                  │
   │                                     │
   ├─────────────────────────────────────┤
   │        [取消]    [开始检查]         │
   └─────────────────────────────────────┘
   ```

3. **智能默认值**
   - 检查日期: 默认今天
   - 检查名称: `{日期} {模板名称}`
   - 录入模式: 记住用户上次选择
   - 计分模式: 从模板继承
   - 基准分: 从模板继承

### 3.2 检查计划列表 (优化现有)

**路由:** `/inspection/check-plan` (保持不变)

**优化点:**

1. **卡片视图增强**
   ```
   ┌─────────────────────────────────────────┐
   │  2024学年第一学期卫生检查计划    [进行中] │
   │                                         │
   │  模板: 宿舍卫生检查                       │
   │  周期: 2024-09-01 ~ 2025-01-31          │
   │                                         │
   │  ┌─────────────────────────────────┐   │
   │  │ ████████████░░░░  75%           │   │
   │  │ 已完成 15次 / 计划 20次          │   │
   │  └─────────────────────────────────┘   │
   │                                         │
   │  下次检查: 2026-02-01 (自动排班)         │
   │                                         │
   │  [查看详情]  [新建检查]  [⋮]            │
   └─────────────────────────────────────────┘
   ```

2. **新增"新建检查"快捷按钮**
   - 直接在列表页可为任意计划创建新检查
   - 点击后进入简化配置框

3. **增加筛选功能**
   - 按状态筛选: 全部/进行中/已完成/已归档
   - 按模板筛选
   - 按时间范围筛选

### 3.3 检查计划详情 (优化现有)

**路由:** `/inspection/check-plan/:id` (保持不变)

**优化点:**

1. **统计卡片增强**
   ```
   ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐
   │ 检查次数  │ │ 已完成   │ │ 平均分   │ │ 申诉数   │
   │    18    │ │    15    │ │   92.5   │ │    3     │
   │  +2本周  │ │  83.3%   │ │  ↑1.2    │ │  待处理1 │
   └──────────┘ └──────────┘ └──────────┘ └──────────┘
   ```

2. **检查记录时间线视图**
   ```
   ┌─────────────────────────────────────────────────────────┐
   │  检查记录                              [新建检查(V4)]   │
   ├─────────────────────────────────────────────────────────┤
   │                                                         │
   │  2026年1月                                              │
   │  ├─ 30日 周四  卫生检查  [已发布] 32班 均分93.2 [查看]  │
   │  ├─ 28日 周二  卫生检查  [已发布] 32班 均分91.8 [查看]  │
   │  └─ 25日 周六  卫生检查  [已发布] 32班 均分92.5 [查看]  │
   │                                                         │
   │  2026年12月                                             │
   │  ├─ 28日 周六  卫生检查  [已发布] 32班 均分90.1 [查看]  │
   │  └─ ...                                                 │
   │                                                         │
   │  [加载更多]                                              │
   └─────────────────────────────────────────────────────────┘
   ```

3. **去除V3旧版检查显示**
   - 移除 `dailyChecks` 相关逻辑
   - 仅显示V4 Session记录

### 3.4 创建检查会话 (简化)

**当前流程:**
```
打开弹窗 → 填写6个字段 → 确认 → 跳转执行页
```

**优化后流程:**
```
点击"新建检查" → 简化弹窗(2-3个核心字段) → 确认 → 跳转执行页
                          ↓
                   [展开高级选项] (可选)
```

**简化弹窗设计:**

```typescript
interface QuickCreateSessionRequest {
  // 必填项
  templateId: number       // 从计划继承或用户选择
  inspectionDate: string   // 默认今天

  // 可选项 (高级选项,默认折叠)
  inspectionPeriod?: string  // 默认自动生成
  inputMode?: InputMode      // 默认从模板/用户偏好
  scoringMode?: ScoringMode  // 默认从模板
  baseScore?: number         // 默认100
  inspectionLevel?: string   // 默认CLASS
}
```

---

## 四、后端API设计

### 4.1 新增API端点

```java
// 快速检查相关
@RestController
@RequestMapping("/inspection/quick")
public class QuickInspectionController {

    /**
     * 获取常用模板列表 (按使用频率排序)
     */
    @GetMapping("/frequent-templates")
    public Result<List<TemplateQuickVO>> getFrequentTemplates(
        @RequestParam(defaultValue = "5") int limit
    );

    /**
     * 快速创建检查会话 (简化参数)
     */
    @PostMapping("/start")
    public Result<InspectionSession> quickStart(
        @RequestBody QuickStartRequest request
    );

    /**
     * 获取今日检查概览
     */
    @GetMapping("/today-overview")
    public Result<TodayOverviewVO> getTodayOverview();

    /**
     * 获取最近检查记录
     */
    @GetMapping("/recent-sessions")
    public Result<List<SessionBriefVO>> getRecentSessions(
        @RequestParam(defaultValue = "10") int limit
    );
}

// DTO定义
@Data
public class QuickStartRequest {
    @NotNull
    private Long templateId;

    private String inspectionDate;  // 默认今天
    private String inspectionPeriod; // 默认自动生成

    // 高级选项
    private InputMode inputMode;     // 默认SPACE_FIRST
    private ScoringMode scoringMode; // 默认DEDUCTION_ONLY
    private Integer baseScore;       // 默认100
}

@Data
public class TodayOverviewVO {
    private int completedCount;
    private int inProgressCount;
    private int pendingPublishCount;
    private int appealPendingCount;
    private LocalDate date;
}
```

### 4.2 现有API优化

```java
// InspectionSessionController 优化
@RestController
@RequestMapping("/inspection/sessions")
public class InspectionSessionController {

    /**
     * 创建会话 - 支持简化模式
     *
     * 变更:
     * - planId 变为可选 (快速检查模式不需要)
     * - 自动填充默认值
     */
    @PostMapping
    public Result<InspectionSession> createSession(
        @RequestBody CreateSessionRequest request
    ) {
        // 如果未指定planId,标记为快速检查模式
        if (request.getPlanId() == null) {
            request.setQuickMode(true);
        }

        // 自动填充默认值
        if (request.getInspectionDate() == null) {
            request.setInspectionDate(LocalDate.now());
        }
        if (request.getInspectionPeriod() == null) {
            request.setInspectionPeriod(generatePeriodName(request));
        }

        return sessionService.createSession(request);
    }

    /**
     * 获取会话列表 - 支持多种查询模式
     */
    @GetMapping
    public Result<Page<SessionVO>> listSessions(
        @RequestParam(required = false) Long planId,      // 按计划筛选
        @RequestParam(required = false) Boolean quickMode, // 快速检查筛选
        @RequestParam(required = false) String startDate,
        @RequestParam(required = false) String endDate,
        @RequestParam(required = false) String status,
        Pageable pageable
    );
}
```

### 4.3 用户偏好存储

```java
// 用户检查偏好 (存储在Redis或数据库)
@Data
public class UserInspectionPreference {
    private Long userId;
    private InputMode preferredInputMode;
    private ScoringMode preferredScoringMode;
    private Integer preferredBaseScore;
    private List<Long> frequentTemplateIds;  // 常用模板
    private LocalDateTime lastUpdated;
}

// 偏好服务
@Service
public class InspectionPreferenceService {

    /**
     * 获取用户偏好
     */
    public UserInspectionPreference getPreference(Long userId);

    /**
     * 更新用户偏好 (每次创建会话后自动更新)
     */
    public void updatePreference(Long userId, CreateSessionRequest request);

    /**
     * 获取用户常用模板
     */
    public List<InspectionTemplate> getFrequentTemplates(Long userId, int limit);
}
```

---

## 五、前端实现方案

### 5.1 新增文件

```
frontend/src/views/inspection/
├── InspectionDashboard.vue      # 新增: 检查入口页面
├── components/
│   ├── QuickStartCard.vue       # 新增: 快速开始卡片
│   ├── TodayOverview.vue        # 新增: 今日概览
│   ├── RecentSessions.vue       # 新增: 最近检查记录
│   └── QuickCreateDialog.vue    # 新增: 快速创建弹窗
└── ...
```

### 5.2 路由配置

```typescript
// router/index.ts 新增路由
{
  path: '/inspection',
  children: [
    {
      path: '',
      redirect: '/inspection/dashboard'  // 默认跳转到入口页
    },
    {
      path: 'dashboard',
      name: 'InspectionDashboard',
      component: () => import('@/views/inspection/InspectionDashboard.vue'),
      meta: {
        title: '量化检查',
        icon: 'ClipboardCheck',
        order: 1  // 放在第一位
      }
    },
    // ... 其他路由保持不变
  ]
}
```

### 5.3 API模块

```typescript
// api/inspectionQuick.ts 新增
import { http } from '@/utils/request'

export interface QuickStartRequest {
  templateId: number
  inspectionDate?: string
  inspectionPeriod?: string
  inputMode?: string
  scoringMode?: string
  baseScore?: number
}

export interface TodayOverview {
  completedCount: number
  inProgressCount: number
  pendingPublishCount: number
  appealPendingCount: number
  date: string
}

export interface SessionBrief {
  id: number
  sessionCode: string
  inspectionDate: string
  inspectionPeriod: string
  templateName: string
  status: string
  classCount: number
  averageScore: number
}

// 获取常用模板
export function getFrequentTemplates(limit = 5) {
  return http.get('/inspection/quick/frequent-templates', { params: { limit } })
}

// 快速开始检查
export function quickStart(request: QuickStartRequest) {
  return http.post('/inspection/quick/start', request)
}

// 获取今日概览
export function getTodayOverview(): Promise<TodayOverview> {
  return http.get('/inspection/quick/today-overview')
}

// 获取最近检查记录
export function getRecentSessions(limit = 10): Promise<SessionBrief[]> {
  return http.get('/inspection/quick/recent-sessions', { params: { limit } })
}
```

### 5.4 状态管理

```typescript
// stores/inspectionPreference.ts 新增
import { defineStore } from 'pinia'

interface InspectionPreference {
  inputMode: string
  scoringMode: string
  baseScore: number
  frequentTemplateIds: number[]
}

export const useInspectionPreferenceStore = defineStore('inspectionPreference', {
  state: (): InspectionPreference => ({
    inputMode: 'SPACE_FIRST',
    scoringMode: 'DEDUCTION_ONLY',
    baseScore: 100,
    frequentTemplateIds: []
  }),

  actions: {
    // 从localStorage恢复偏好
    loadFromStorage() {
      const saved = localStorage.getItem('inspection_preference')
      if (saved) {
        Object.assign(this, JSON.parse(saved))
      }
    },

    // 保存偏好到localStorage
    saveToStorage() {
      localStorage.setItem('inspection_preference', JSON.stringify(this.$state))
    },

    // 更新偏好
    updatePreference(partial: Partial<InspectionPreference>) {
      Object.assign(this, partial)
      this.saveToStorage()
    }
  }
})
```

---

## 六、与其他模块的兼容性

### 6.1 排名模块 (RankingResults)

**现有逻辑:**
```typescript
// 当前: 从session获取classRecords计算排名
const rankings = computed(() => {
  return classRecords.value
    .sort((a, b) => b.finalScore - a.finalScore)
    .map((record, idx) => ({ ranking: idx + 1, ...record }))
})
```

**兼容方案:**
- 无需修改，排名基于 `ClassInspectionRecord`
- 快速检查和计划检查的记录结构完全相同
- 新增 `quickMode` 字段可用于筛选统计

### 6.2 申诉模块 (AppealManagement)

**现有逻辑:**
```typescript
// 申诉关联到 inspectionRecordId 和 deductionDetailId
interface Appeal {
  inspectionRecordId: number  // 关联ClassInspectionRecord
  deductionDetailId: number   // 关联具体扣分项
  // ...
}
```

**兼容方案:**
- 无需修改，申诉基于 `ClassInspectionRecord` 和 `InspectionDeduction`
- 快速检查产生的记录同样支持申诉流程

### 6.3 整改模块 (CorrectiveActions)

**现有逻辑:**
```typescript
// 整改关联到班级检查记录
interface CorrectiveAction {
  sourceRecordId: number     // 关联ClassInspectionRecord
  classId: number
  // ...
}
```

**兼容方案:**
- 无需修改，整改基于 `ClassInspectionRecord`
- 整改工单的创建、追踪流程不受影响

### 6.4 数据分析模块 (DataAnalyticsCenter)

**现有逻辑:**
```typescript
// 统计基于时间范围和模板筛选
async function loadViolationData() {
  const resp = await getViolationDistribution({ startDate, endDate })
  // ...
}
```

**兼容方案:**
- 无需修改，分析基于 `InspectionSession` 和 `ClassInspectionRecord`
- 可新增 `quickMode` 筛选维度（可选）

### 6.5 学生行为模块 (StudentBehaviorProfile)

**现有逻辑:**
```typescript
// 行为记录关联到扣分明细
interface BehaviorRecord {
  inspectionSessionId: number
  deductionId: number
  studentId: number
  // ...
}
```

**兼容方案:**
- 无需修改，行为记录基于 `InspectionDeduction`
- `SessionPublishedEvent` 触发行为记录同步

### 6.6 兼容性矩阵

| 模块 | 依赖的数据 | 是否需要修改 | 说明 |
|------|-----------|-------------|------|
| 排名结果 | ClassInspectionRecord | 否 | 基于finalScore排序 |
| 申诉管理 | ClassInspectionRecord, InspectionDeduction | 否 | 申诉流程不变 |
| 整改工单 | ClassInspectionRecord | 否 | 整改关联不变 |
| 数据分析 | InspectionSession, ClassInspectionRecord | 否 | 统计逻辑不变 |
| 学生行为 | InspectionDeduction | 否 | 事件触发不变 |
| 班主任台 | InspectionSession | 否 | 仪表板数据不变 |
| 评级统计 | ClassInspectionRecord | 否 | 评级计算不变 |
| 导出中心 | InspectionSession, ClassInspectionRecord | 可选 | 可新增quickMode筛选 |

---

## 七、实施计划

### 7.1 阶段划分

```
Phase 1: 基础改造 (1-2天)
├─ 后端: 新增QuickInspectionController
├─ 后端: CreateSessionRequest支持planId可选
├─ 数据库: 新增quick_mode和plan_id字段
└─ 单元测试

Phase 2: 入口页面 (2-3天)
├─ 前端: InspectionDashboard.vue
├─ 前端: QuickStartCard, TodayOverview组件
├─ 前端: QuickCreateDialog组件
├─ 路由配置调整
└─ 集成测试

Phase 3: 现有页面优化 (1-2天)
├─ InspectionPlanList.vue 优化
├─ InspectionPlanDetail.vue 优化
├─ 移除V3旧版检查逻辑
└─ 回归测试

Phase 4: 用户偏好与体验 (1天)
├─ 用户偏好存储
├─ 智能默认值填充
├─ 常用模板排序
└─ 端到端测试
```

### 7.2 风险评估

| 风险 | 概率 | 影响 | 缓解措施 |
|------|------|------|----------|
| 数据库迁移失败 | 低 | 高 | 先在测试环境验证,准备回滚脚本 |
| 与现有模块不兼容 | 低 | 中 | 兼容性测试覆盖所有模块 |
| 用户不适应新流程 | 中 | 低 | 保留原有入口,渐进式引导 |
| 性能下降 | 低 | 中 | 添加必要索引,监控查询性能 |

### 7.3 回滚方案

```sql
-- 如需回滚,执行以下SQL
ALTER TABLE inspection_session DROP COLUMN quick_mode;
ALTER TABLE inspection_session DROP COLUMN plan_id;
DROP INDEX idx_session_plan ON inspection_session;
DROP INDEX idx_session_quick ON inspection_session;

-- 前端回滚: 恢复路由默认指向 /inspection/check-plan
```

---

## 八、核心代码示例

### 8.1 InspectionDashboard.vue

```vue
<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  ClipboardCheck, Plus, ArrowRight, Calendar,
  TrendingUp, AlertCircle, FileText, Users
} from 'lucide-vue-next'
import {
  getFrequentTemplates,
  getTodayOverview,
  getRecentSessions,
  quickStart,
  type TodayOverview,
  type SessionBrief
} from '@/api/inspectionQuick'
import { useInspectionPreferenceStore } from '@/stores/inspectionPreference'
import QuickCreateDialog from './components/QuickCreateDialog.vue'

const router = useRouter()
const preferenceStore = useInspectionPreferenceStore()

// State
const loading = ref(false)
const templates = ref<any[]>([])
const overview = ref<TodayOverview | null>(null)
const recentSessions = ref<SessionBrief[]>([])

// Quick create dialog
const showQuickDialog = ref(false)
const selectedTemplate = ref<any>(null)

// Load data
async function loadData() {
  loading.value = true
  try {
    const [tpls, ov, sessions] = await Promise.all([
      getFrequentTemplates(5),
      getTodayOverview(),
      getRecentSessions(5)
    ])
    templates.value = tpls
    overview.value = ov
    recentSessions.value = sessions
  } catch (e: any) {
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

// Quick start
function openQuickStart(template: any) {
  selectedTemplate.value = template
  showQuickDialog.value = true
}

async function handleQuickStart(request: any) {
  try {
    const session = await quickStart(request)
    ElMessage.success('检查创建成功')
    showQuickDialog.value = false
    router.push(`/inspection/execute/${session.id}`)
  } catch (e: any) {
    ElMessage.error('创建失败: ' + (e.message || '未知错误'))
  }
}

// Navigation
function goToSession(session: SessionBrief) {
  router.push(`/inspection/execute/${session.id}`)
}

function goToModule(path: string) {
  router.push(path)
}

onMounted(() => {
  preferenceStore.loadFromStorage()
  loadData()
})
</script>

<template>
  <div class="min-h-screen bg-gray-50 p-6">
    <!-- Header -->
    <div class="flex items-center justify-between mb-6">
      <div>
        <h1 class="text-2xl font-bold text-gray-900 mb-1">量化检查</h1>
        <p class="text-sm text-gray-500">快速开始检查或管理检查计划</p>
      </div>
    </div>

    <div class="grid grid-cols-3 gap-6">
      <!-- Left: Quick Start -->
      <div class="col-span-1">
        <div class="bg-white rounded-xl p-5 shadow-sm border border-gray-100">
          <h2 class="text-sm font-semibold text-gray-800 mb-4 flex items-center gap-2">
            <ClipboardCheck class="w-4 h-4 text-emerald-500" />
            快速开始检查
          </h2>

          <div class="space-y-2">
            <button
              v-for="tpl in templates"
              :key="tpl.id"
              @click="openQuickStart(tpl)"
              class="w-full flex items-center justify-between p-3 rounded-lg border border-gray-100 hover:border-emerald-200 hover:bg-emerald-50/50 transition-all group"
            >
              <span class="text-sm text-gray-700 group-hover:text-emerald-700">{{ tpl.templateName }}</span>
              <ArrowRight class="w-4 h-4 text-gray-300 group-hover:text-emerald-500 transition-colors" />
            </button>
          </div>

          <button
            @click="goToModule('/inspection/config')"
            class="mt-4 w-full flex items-center justify-center gap-2 p-2 text-sm text-gray-500 hover:text-gray-700 transition-colors"
          >
            <Plus class="w-4 h-4" />
            选择其他模板
          </button>
        </div>
      </div>

      <!-- Middle: Today Overview + Recent -->
      <div class="col-span-2 space-y-6">
        <!-- Today Overview -->
        <div class="bg-white rounded-xl p-5 shadow-sm border border-gray-100">
          <h2 class="text-sm font-semibold text-gray-800 mb-4 flex items-center gap-2">
            <Calendar class="w-4 h-4 text-blue-500" />
            今日检查概览
          </h2>

          <div class="grid grid-cols-4 gap-4" v-if="overview">
            <div class="text-center p-3 rounded-lg bg-emerald-50">
              <div class="text-2xl font-bold text-emerald-600">{{ overview.completedCount }}</div>
              <div class="text-xs text-gray-500 mt-1">已完成</div>
            </div>
            <div class="text-center p-3 rounded-lg bg-amber-50">
              <div class="text-2xl font-bold text-amber-600">{{ overview.inProgressCount }}</div>
              <div class="text-xs text-gray-500 mt-1">进行中</div>
            </div>
            <div class="text-center p-3 rounded-lg bg-blue-50">
              <div class="text-2xl font-bold text-blue-600">{{ overview.pendingPublishCount }}</div>
              <div class="text-xs text-gray-500 mt-1">待发布</div>
            </div>
            <div class="text-center p-3 rounded-lg bg-red-50">
              <div class="text-2xl font-bold text-red-600">{{ overview.appealPendingCount }}</div>
              <div class="text-xs text-gray-500 mt-1">申诉待处理</div>
            </div>
          </div>
        </div>

        <!-- Recent Sessions -->
        <div class="bg-white rounded-xl p-5 shadow-sm border border-gray-100">
          <div class="flex items-center justify-between mb-4">
            <h2 class="text-sm font-semibold text-gray-800 flex items-center gap-2">
              <FileText class="w-4 h-4 text-violet-500" />
              最近检查记录
            </h2>
            <button
              @click="goToModule('/inspection/check-plan')"
              class="text-xs text-gray-500 hover:text-gray-700"
            >
              查看全部 →
            </button>
          </div>

          <div class="space-y-2">
            <div
              v-for="session in recentSessions"
              :key="session.id"
              @click="goToSession(session)"
              class="flex items-center justify-between p-3 rounded-lg border border-gray-100 hover:border-gray-200 hover:bg-gray-50 transition-all cursor-pointer"
            >
              <div class="flex items-center gap-3">
                <div class="text-sm">
                  <span class="font-medium text-gray-800">{{ session.inspectionPeriod }}</span>
                  <span class="text-gray-400 ml-2">{{ session.inspectionDate }}</span>
                </div>
              </div>
              <div class="flex items-center gap-3">
                <span class="text-xs text-gray-500">{{ session.classCount }}个班级</span>
                <span
                  class="text-xs px-2 py-0.5 rounded-full"
                  :class="{
                    'bg-emerald-50 text-emerald-600': session.status === 'PUBLISHED',
                    'bg-amber-50 text-amber-600': session.status === 'IN_PROGRESS',
                    'bg-blue-50 text-blue-600': session.status === 'SUBMITTED',
                    'bg-gray-100 text-gray-500': session.status === 'CREATED'
                  }"
                >
                  {{ session.status === 'PUBLISHED' ? '已发布' :
                     session.status === 'IN_PROGRESS' ? '进行中' :
                     session.status === 'SUBMITTED' ? '已提交' : '已创建' }}
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Function Modules -->
    <div class="mt-6 bg-white rounded-xl p-5 shadow-sm border border-gray-100">
      <h2 class="text-sm font-semibold text-gray-800 mb-4">功能入口</h2>
      <div class="grid grid-cols-5 gap-3">
        <button
          v-for="item in [
            { path: '/inspection/check-plan', label: '检查计划', icon: Calendar },
            { path: '/inspection/ranking-results', label: '排名结果', icon: TrendingUp },
            { path: '/inspection/appeals', label: '申诉管理', icon: AlertCircle },
            { path: '/inspection/data-analytics', label: '数据分析', icon: FileText },
            { path: '/inspection/teacher-dashboard', label: '班主任台', icon: Users },
          ]"
          :key="item.path"
          @click="goToModule(item.path)"
          class="flex flex-col items-center gap-2 p-4 rounded-lg border border-gray-100 hover:border-gray-200 hover:bg-gray-50 transition-all"
        >
          <component :is="item.icon" class="w-5 h-5 text-gray-400" />
          <span class="text-xs text-gray-600">{{ item.label }}</span>
        </button>
      </div>
    </div>

    <!-- Quick Create Dialog -->
    <QuickCreateDialog
      v-model:visible="showQuickDialog"
      :template="selectedTemplate"
      :default-input-mode="preferenceStore.inputMode"
      :default-scoring-mode="preferenceStore.scoringMode"
      :default-base-score="preferenceStore.baseScore"
      @confirm="handleQuickStart"
    />
  </div>
</template>
```

### 8.2 QuickCreateDialog.vue

```vue
<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ElDialog, ElForm, ElFormItem, ElInput, ElSelect, ElOption, ElDatePicker } from 'element-plus'
import { ChevronDown, ChevronUp } from 'lucide-vue-next'

const props = defineProps<{
  visible: boolean
  template: any
  defaultInputMode: string
  defaultScoringMode: string
  defaultBaseScore: number
}>()

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'confirm', request: any): void
}>()

const dialogVisible = computed({
  get: () => props.visible,
  set: (val) => emit('update:visible', val)
})

// Form state
const form = ref({
  inspectionDate: new Date().toISOString().split('T')[0],
  inspectionPeriod: '',
  inputMode: 'SPACE_FIRST',
  scoringMode: 'DEDUCTION_ONLY',
  baseScore: 100
})

const showAdvanced = ref(false)

// Auto-generate period name
watch(() => [props.template, form.value.inspectionDate], () => {
  if (props.template && form.value.inspectionDate) {
    const date = new Date(form.value.inspectionDate)
    const dateStr = `${date.getMonth() + 1}月${date.getDate()}日`
    form.value.inspectionPeriod = `${dateStr} ${props.template.templateName}`
  }
}, { immediate: true })

// Initialize with defaults
watch(() => props.visible, (visible) => {
  if (visible) {
    form.value.inputMode = props.defaultInputMode
    form.value.scoringMode = props.defaultScoringMode
    form.value.baseScore = props.defaultBaseScore
  }
})

function handleConfirm() {
  emit('confirm', {
    templateId: props.template?.id,
    inspectionDate: form.value.inspectionDate,
    inspectionPeriod: form.value.inspectionPeriod,
    inputMode: form.value.inputMode,
    scoringMode: form.value.scoringMode,
    baseScore: form.value.baseScore
  })
}
</script>

<template>
  <ElDialog
    v-model="dialogVisible"
    :title="`开始${template?.templateName || '检查'}`"
    width="420px"
    :close-on-click-modal="false"
  >
    <ElForm :model="form" label-width="80px">
      <ElFormItem label="检查日期">
        <ElDatePicker
          v-model="form.inspectionDate"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="选择日期"
          class="w-full"
        />
      </ElFormItem>

      <ElFormItem label="检查名称">
        <ElInput v-model="form.inspectionPeriod" placeholder="自动生成,可修改" />
      </ElFormItem>

      <!-- Advanced Options Toggle -->
      <div
        class="flex items-center gap-2 py-2 text-sm text-gray-500 cursor-pointer hover:text-gray-700"
        @click="showAdvanced = !showAdvanced"
      >
        <component :is="showAdvanced ? ChevronUp : ChevronDown" class="w-4 h-4" />
        高级选项
      </div>

      <template v-if="showAdvanced">
        <ElFormItem label="录入模式">
          <ElSelect v-model="form.inputMode" class="w-full">
            <ElOption value="SPACE_FIRST" label="物理空间优先" />
            <ElOption value="ORG_FIRST" label="按班级模式" />
            <ElOption value="PERSON_FIRST" label="人员搜索模式" />
          </ElSelect>
        </ElFormItem>

        <ElFormItem label="计分模式">
          <ElSelect v-model="form.scoringMode" class="w-full">
            <ElOption value="DEDUCTION_ONLY" label="纯扣分制" />
            <ElOption value="BASE_SCORE" label="基准分制" />
            <ElOption value="DUAL_TRACK" label="双轨制" />
          </ElSelect>
        </ElFormItem>

        <ElFormItem v-if="form.scoringMode !== 'DEDUCTION_ONLY'" label="基准分">
          <ElInput v-model.number="form.baseScore" type="number" />
        </ElFormItem>
      </template>
    </ElForm>

    <template #footer>
      <button
        @click="dialogVisible = false"
        class="px-4 py-2 text-sm text-gray-600 hover:text-gray-800 transition-colors"
      >
        取消
      </button>
      <button
        @click="handleConfirm"
        class="ml-3 px-5 py-2 bg-emerald-500 text-white text-sm font-medium rounded-lg hover:bg-emerald-600 transition-colors"
      >
        开始检查
      </button>
    </template>
  </ElDialog>
</template>
```

---

## 九、总结

### 9.1 方案核心要点

1. **双轨模式**: 快速检查(无需Plan) + 计划检查(完整流程)
2. **最小侵入**: 保持现有数据结构,仅新增字段
3. **完全兼容**: 不影响排名、申诉、整改等模块
4. **渐进实施**: 分4个阶段,每阶段可独立验证

### 9.2 预期效果

| 指标 | 当前 | 优化后 |
|------|------|--------|
| 开始检查步骤 | 4步 | 2步 |
| 创建会话配置项 | 6个 | 2个(+4可选) |
| 用户理解成本 | 高 | 低 |
| 临时检查支持 | 无 | 有 |

### 9.3 下一步行动

1. 评审本方案
2. 确认实施优先级
3. 开始Phase 1开发
