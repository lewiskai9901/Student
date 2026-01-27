# 「我的班级」工作台实施计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 为班主任和被分配班级的用户提供专属工作台，支持多班级管理、学生信息、宿舍分布和数据分析。

**Architecture:** 前后端分离，后端新增 `/api/v2/my-class` 端点集合，前端新增 `/my-class` 路由和相关组件。复用现有 `TeacherAssignment` 关联实现数据权限控制。

**Tech Stack:**
- Backend: Spring Boot 3.2, MyBatis Plus, DDD架构
- Frontend: Vue 3 + TypeScript, Element Plus, ECharts, Pinia

**设计文档:** `docs/plans/2026-01-18-my-class-workbench-design.md`

---

## 第一期：基础功能

### Task 1: 后端 - 创建「我的班级」API接口

**Files:**
- Create: `backend/src/main/java/com/school/management/interfaces/rest/myclass/MyClassController.java`
- Create: `backend/src/main/java/com/school/management/application/myclass/MyClassApplicationService.java`
- Create: `backend/src/main/java/com/school/management/application/myclass/query/MyClassDTO.java`
- Create: `backend/src/main/java/com/school/management/application/myclass/query/MyClassOverviewDTO.java`

**Step 1: 创建 MyClassDTO 数据传输对象**

```java
// MyClassDTO.java - 我的班级列表项
package com.school.management.application.myclass.query;

import lombok.Data;
import lombok.Builder;
import java.util.List;

@Data
@Builder
public class MyClassDTO {
    private Long id;
    private String classCode;
    private String className;
    private String shortName;
    private Integer currentSize;
    private Integer standardSize;
    private String status;
    private Integer enrollmentYear;
    private String majorName;
    private String orgUnitName;
    // 当前用户在该班级的角色
    private String myRole;
    // 班级排名（本周/本月）
    private Integer weeklyRank;
    private Integer totalClasses;
    // 本周平均分
    private Double weeklyScore;
    // 近5次得分趋势
    private List<Double> scoreTrend;
}
```

**Step 2: 创建 MyClassOverviewDTO 概览数据对象**

```java
// MyClassOverviewDTO.java - 班级概览数据
package com.school.management.application.myclass.query;

import lombok.Data;
import lombok.Builder;
import java.util.List;

@Data
@Builder
public class MyClassOverviewDTO {
    private Long classId;
    private String className;

    // 统计数据
    private Integer studentCount;
    private Integer maleCount;
    private Integer femaleCount;
    private Integer classRank;
    private Integer totalClasses;
    private Double averageScore;
    private Double scoreTrend; // 较上周变化
    private Integer pendingAppeals;

    // 近30天趋势数据
    private List<ScoreTrendItem> scoreTrendList;

    // 最近检查记录
    private List<RecentCheckRecord> recentRecords;

    @Data
    @Builder
    public static class ScoreTrendItem {
        private String date;
        private Double score;
    }

    @Data
    @Builder
    public static class RecentCheckRecord {
        private Long id;
        private String checkDate;
        private String checkType;
        private Double score;
        private Integer rank;
    }
}
```

**Step 3: 创建 MyClassApplicationService 应用服务**

```java
// MyClassApplicationService.java
package com.school.management.application.myclass;

import com.school.management.application.myclass.query.MyClassDTO;
import com.school.management.application.myclass.query.MyClassOverviewDTO;
import com.school.management.domain.organization.model.SchoolClass;
import com.school.management.domain.organization.model.TeacherAssignment;
import com.school.management.domain.organization.repository.SchoolClassRepository;
import com.school.management.domain.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MyClassApplicationService {

    private final SchoolClassRepository schoolClassRepository;
    private final StudentRepository studentRepository;

    /**
     * 获取当前用户管理的班级列表
     */
    public List<MyClassDTO> getMyClasses(Long userId) {
        List<SchoolClass> classes = schoolClassRepository.findByTeacherId(userId);

        return classes.stream()
            .map(c -> toMyClassDTO(c, userId))
            .collect(Collectors.toList());
    }

    /**
     * 获取班级概览数据
     */
    public MyClassOverviewDTO getClassOverview(Long classId, Long userId) {
        SchoolClass schoolClass = schoolClassRepository.findById(classId)
            .orElseThrow(() -> new RuntimeException("班级不存在"));

        // 验证用户有权限访问该班级
        validateAccess(schoolClass, userId);

        // 获取学生统计
        long studentCount = studentRepository.countByClassId(classId);
        long maleCount = studentRepository.countByClassIdAndGender(classId, "男");
        long femaleCount = studentRepository.countByClassIdAndGender(classId, "女");

        return MyClassOverviewDTO.builder()
            .classId(classId)
            .className(schoolClass.getClassName())
            .studentCount((int) studentCount)
            .maleCount((int) maleCount)
            .femaleCount((int) femaleCount)
            .classRank(0) // TODO: 从检查记录服务获取
            .totalClasses(0)
            .averageScore(0.0)
            .scoreTrend(0.0)
            .pendingAppeals(0)
            .scoreTrendList(new ArrayList<>())
            .recentRecords(new ArrayList<>())
            .build();
    }

    private MyClassDTO toMyClassDTO(SchoolClass c, Long userId) {
        String myRole = c.getTeacherAssignments().stream()
            .filter(a -> a.getTeacherId().equals(userId) && a.isCurrent())
            .findFirst()
            .map(a -> a.getRole().name())
            .orElse("UNKNOWN");

        return MyClassDTO.builder()
            .id(c.getId())
            .classCode(c.getClassCode())
            .className(c.getClassName())
            .shortName(c.getShortName())
            .currentSize(c.getCurrentSize())
            .standardSize(c.getStandardSize())
            .status(c.getStatus().name())
            .enrollmentYear(c.getEnrollmentYear())
            .myRole(myRole)
            .weeklyRank(0)
            .totalClasses(0)
            .weeklyScore(0.0)
            .scoreTrend(new ArrayList<>())
            .build();
    }

    private void validateAccess(SchoolClass schoolClass, Long userId) {
        boolean hasAccess = schoolClass.getTeacherAssignments().stream()
            .anyMatch(a -> a.getTeacherId().equals(userId) && a.isCurrent());
        if (!hasAccess) {
            throw new RuntimeException("无权访问该班级");
        }
    }
}
```

**Step 4: 创建 MyClassController REST控制器**

```java
// MyClassController.java
package com.school.management.interfaces.rest.myclass;

import com.school.management.application.myclass.MyClassApplicationService;
import com.school.management.application.myclass.query.MyClassDTO;
import com.school.management.application.myclass.query.MyClassOverviewDTO;
import com.school.management.common.Result;
import com.school.management.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v2/my-class")
@RequiredArgsConstructor
@Tag(name = "我的班级", description = "班主任/教师班级管理工作台")
public class MyClassController {

    private final MyClassApplicationService myClassService;

    @GetMapping("/classes")
    @Operation(summary = "获取我管理的班级列表")
    public Result<List<MyClassDTO>> getMyClasses(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<MyClassDTO> classes = myClassService.getMyClasses(userDetails.getUserId());
        return Result.success(classes);
    }

    @GetMapping("/classes/{classId}/overview")
    @Operation(summary = "获取班级概览数据")
    public Result<MyClassOverviewDTO> getClassOverview(
            @PathVariable Long classId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        MyClassOverviewDTO overview = myClassService.getClassOverview(classId, userDetails.getUserId());
        return Result.success(overview);
    }
}
```

**Step 5: 在 SchoolClassRepository 添加查询方法**

修改 `backend/src/main/java/com/school/management/domain/organization/repository/SchoolClassRepository.java`：

```java
// 添加方法
List<SchoolClass> findByTeacherId(Long teacherId);
```

**Step 6: 在 SchoolClassRepositoryImpl 实现查询**

修改 `backend/src/main/java/com/school/management/infrastructure/persistence/organization/SchoolClassRepositoryImpl.java`：

```java
@Override
public List<SchoolClass> findByTeacherId(Long teacherId) {
    // 使用 Mapper 查询
    List<SchoolClassPO> poList = schoolClassMapper.findByTeacherId(teacherId);
    return poList.stream()
        .map(this::toDomain)
        .collect(Collectors.toList());
}
```

**Step 7: 在 Mapper 添加 SQL 查询**

修改 `backend/src/main/resources/mapper/organization/SchoolClassMapper.xml`：

```xml
<select id="findByTeacherId" resultMap="schoolClassResultMap">
    SELECT DISTINCT c.*
    FROM school_class c
    INNER JOIN class_teacher_assignment cta ON c.id = cta.class_id
    WHERE cta.teacher_id = #{teacherId}
      AND cta.is_current = 1
      AND c.deleted = 0
    ORDER BY c.enrollment_year DESC, c.sort_order ASC
</select>
```

**Step 8: 验证接口可用**

```bash
# 启动后端
cd backend && mvn spring-boot:run -DskipTests

# 测试接口（需要先登录获取token）
curl -X GET http://localhost:8080/api/v2/my-class/classes \
  -H "Authorization: Bearer {token}"
```

---

### Task 2: 前端 - 创建API和类型定义

**Files:**
- Create: `frontend/src/api/v2/myClass.ts`
- Create: `frontend/src/types/v2/myClass.ts`

**Step 1: 创建类型定义**

```typescript
// frontend/src/types/v2/myClass.ts

/**
 * 我的班级 - 列表项
 */
export interface MyClassItem {
  id: number
  classCode: string
  className: string
  shortName?: string
  currentSize: number
  standardSize: number
  status: 'PREPARING' | 'ACTIVE' | 'GRADUATED' | 'DISSOLVED'
  enrollmentYear: number
  majorName?: string
  orgUnitName?: string
  myRole: 'HEAD_TEACHER' | 'DEPUTY_HEAD_TEACHER' | 'SUBJECT_TEACHER' | 'COUNSELOR'
  weeklyRank?: number
  totalClasses?: number
  weeklyScore?: number
  scoreTrend?: number[]
}

/**
 * 成绩趋势项
 */
export interface ScoreTrendItem {
  date: string
  score: number
}

/**
 * 最近检查记录
 */
export interface RecentCheckRecord {
  id: number
  checkDate: string
  checkType: string
  score: number
  rank: number
}

/**
 * 班级概览数据
 */
export interface MyClassOverview {
  classId: number
  className: string
  studentCount: number
  maleCount: number
  femaleCount: number
  classRank: number
  totalClasses: number
  averageScore: number
  scoreTrend: number
  pendingAppeals: number
  scoreTrendList: ScoreTrendItem[]
  recentRecords: RecentCheckRecord[]
}

/**
 * 班级学生列表项
 */
export interface MyClassStudent {
  id: number
  studentNo: string
  name: string
  gender: '男' | '女'
  phone?: string
  dormitoryName?: string
  bedNo?: string
  status: 'ENROLLED' | 'SUSPENDED' | 'GRADUATED' | 'DROPPED'
}

/**
 * 宿舍分布信息
 */
export interface DormitoryDistribution {
  buildingId: number
  buildingName: string
  buildingType: 'MALE' | 'FEMALE' | 'MIXED'
  rooms: DormitoryRoom[]
  studentCount: number
}

export interface DormitoryRoom {
  dormitoryId: number
  roomNo: string
  floor: number
  studentCount: number
  students: { id: number; name: string; bedNo: string }[]
}

/**
 * 角色显示配置
 */
export const MyRoleConfig: Record<string, { label: string; color: string }> = {
  HEAD_TEACHER: { label: '班主任', color: '#409EFF' },
  DEPUTY_HEAD_TEACHER: { label: '副班主任', color: '#67C23A' },
  SUBJECT_TEACHER: { label: '任课教师', color: '#909399' },
  COUNSELOR: { label: '辅导员', color: '#E6A23C' }
}
```

**Step 2: 创建API模块**

```typescript
// frontend/src/api/v2/myClass.ts

import { http } from '@/utils/request'
import type {
  MyClassItem,
  MyClassOverview,
  MyClassStudent,
  DormitoryDistribution
} from '@/types/v2/myClass'

const BASE_URL = '/v2/my-class'

/**
 * 获取我管理的班级列表
 */
export function getMyClasses(): Promise<MyClassItem[]> {
  return http.get<MyClassItem[]>(`${BASE_URL}/classes`)
}

/**
 * 获取班级概览数据
 */
export function getClassOverview(classId: number): Promise<MyClassOverview> {
  return http.get<MyClassOverview>(`${BASE_URL}/classes/${classId}/overview`)
}

/**
 * 获取班级学生列表
 */
export function getClassStudents(classId: number, params?: {
  keyword?: string
  status?: string
}): Promise<MyClassStudent[]> {
  return http.get<MyClassStudent[]>(`${BASE_URL}/classes/${classId}/students`, { params })
}

/**
 * 获取班级宿舍分布
 */
export function getClassDormitoryDistribution(classId: number): Promise<DormitoryDistribution[]> {
  return http.get<DormitoryDistribution[]>(`${BASE_URL}/classes/${classId}/dormitory-distribution`)
}

/**
 * 我的班级 API 对象
 */
export const myClassApi = {
  getMyClasses,
  getClassOverview,
  getClassStudents,
  getClassDormitoryDistribution
}

export default myClassApi
```

**Step 3: 导出类型**

修改 `frontend/src/types/v2/index.ts`，添加：

```typescript
export * from './myClass'
```

---

### Task 3: 前端 - 创建「我的班级」路由和入口页面

**Files:**
- Create: `frontend/src/views/myclass/MyClassListView.vue`
- Modify: `frontend/src/router/index.ts`

**Step 1: 创建班级列表页面**

```vue
<!-- frontend/src/views/myclass/MyClassListView.vue -->
<template>
  <div class="my-class-list">
    <!-- 页面标题 -->
    <div class="page-header">
      <h2>我的班级</h2>
      <p class="subtitle">管理您负责的班级，查看学生和数据统计</p>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-container">
      <el-skeleton :rows="3" animated />
    </div>

    <!-- 空状态 -->
    <el-empty
      v-else-if="classes.length === 0"
      description="您暂未分配任何班级"
    >
      <template #image>
        <el-icon :size="64" color="#909399"><School /></el-icon>
      </template>
    </el-empty>

    <!-- 班级卡片网格 -->
    <div v-else class="class-grid">
      <div
        v-for="item in classes"
        :key="item.id"
        class="class-card"
        @click="goToClassDetail(item.id)"
      >
        <div class="card-header">
          <div class="class-info">
            <span class="class-name">{{ item.className }}</span>
            <el-tag size="small" :type="getStatusType(item.status)">
              {{ getStatusLabel(item.status) }}
            </el-tag>
          </div>
          <el-tag size="small" :color="getRoleColor(item.myRole)">
            {{ getRoleLabel(item.myRole) }}
          </el-tag>
        </div>

        <div class="card-stats">
          <div class="stat-item">
            <el-icon><User /></el-icon>
            <span>{{ item.currentSize }}人</span>
          </div>
          <div v-if="item.weeklyRank" class="stat-item">
            <el-icon><Trophy /></el-icon>
            <span :class="getRankClass(item.weeklyRank, item.totalClasses)">
              第{{ item.weeklyRank }}/{{ item.totalClasses }}名
            </span>
          </div>
          <div v-if="item.weeklyScore" class="stat-item">
            <el-icon><DataAnalysis /></el-icon>
            <span>{{ item.weeklyScore?.toFixed(1) }}分</span>
          </div>
        </div>

        <!-- 趋势迷你图 -->
        <div v-if="item.scoreTrend?.length" class="trend-mini">
          <MiniTrendChart :data="item.scoreTrend" />
        </div>

        <div class="card-footer">
          <span class="enrollment">{{ item.enrollmentYear }}级</span>
          <el-button type="primary" text>
            进入 <el-icon><ArrowRight /></el-icon>
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Trophy, DataAnalysis, ArrowRight, School } from '@element-plus/icons-vue'
import { getMyClasses } from '@/api/v2/myClass'
import type { MyClassItem } from '@/types/v2/myClass'
import MiniTrendChart from './components/MiniTrendChart.vue'

const router = useRouter()
const loading = ref(true)
const classes = ref<MyClassItem[]>([])

const fetchClasses = async () => {
  try {
    loading.value = true
    classes.value = await getMyClasses()

    // 如果只有一个班级，直接跳转详情页
    if (classes.value.length === 1) {
      goToClassDetail(classes.value[0].id)
    }
  } catch (error: any) {
    ElMessage.error(error.message || '获取班级列表失败')
  } finally {
    loading.value = false
  }
}

const goToClassDetail = (classId: number) => {
  router.push(`/my-class/${classId}`)
}

const getStatusType = (status: string) => {
  const map: Record<string, string> = {
    PREPARING: 'info',
    ACTIVE: 'success',
    GRADUATED: 'warning',
    DISSOLVED: 'danger'
  }
  return map[status] || 'info'
}

const getStatusLabel = (status: string) => {
  const map: Record<string, string> = {
    PREPARING: '筹建中',
    ACTIVE: '在读中',
    GRADUATED: '已毕业',
    DISSOLVED: '已撤销'
  }
  return map[status] || status
}

const getRoleColor = (role: string) => {
  const map: Record<string, string> = {
    HEAD_TEACHER: '#409EFF',
    DEPUTY_HEAD_TEACHER: '#67C23A',
    SUBJECT_TEACHER: '#909399',
    COUNSELOR: '#E6A23C'
  }
  return map[role] || '#909399'
}

const getRoleLabel = (role: string) => {
  const map: Record<string, string> = {
    HEAD_TEACHER: '班主任',
    DEPUTY_HEAD_TEACHER: '副班主任',
    SUBJECT_TEACHER: '任课教师',
    COUNSELOR: '辅导员'
  }
  return map[role] || role
}

const getRankClass = (rank: number, total: number) => {
  if (rank <= 3) return 'rank-top'
  if (rank > total * 0.7) return 'rank-warning'
  return ''
}

onMounted(fetchClasses)
</script>

<style scoped lang="scss">
.my-class-list {
  padding: 24px;
}

.page-header {
  margin-bottom: 24px;

  h2 {
    margin: 0 0 8px 0;
    font-size: 24px;
    font-weight: 600;
  }

  .subtitle {
    margin: 0;
    color: #909399;
  }
}

.loading-container {
  padding: 40px;
}

.class-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 20px;
}

.class-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  cursor: pointer;
  transition: all 0.3s ease;

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  }
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;

  .class-info {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .class-name {
    font-size: 18px;
    font-weight: 600;
  }
}

.card-stats {
  display: flex;
  gap: 20px;
  margin-bottom: 16px;

  .stat-item {
    display: flex;
    align-items: center;
    gap: 4px;
    color: #606266;
    font-size: 14px;

    .el-icon {
      color: #909399;
    }
  }

  .rank-top {
    color: #E6A23C;
    font-weight: 600;
  }

  .rank-warning {
    color: #F56C6C;
  }
}

.trend-mini {
  height: 40px;
  margin-bottom: 16px;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 12px;
  border-top: 1px solid #EBEEF5;

  .enrollment {
    color: #909399;
    font-size: 14px;
  }
}
</style>
```

**Step 2: 创建迷你趋势图组件**

```vue
<!-- frontend/src/views/myclass/components/MiniTrendChart.vue -->
<template>
  <div ref="chartRef" class="mini-trend-chart"></div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import * as echarts from 'echarts/core'
import { LineChart } from 'echarts/charts'
import { GridComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

echarts.use([LineChart, GridComponent, CanvasRenderer])

const props = defineProps<{
  data: number[]
}>()

const chartRef = ref<HTMLElement>()
let chart: echarts.ECharts | null = null

const initChart = () => {
  if (!chartRef.value) return

  chart = echarts.init(chartRef.value)
  updateChart()
}

const updateChart = () => {
  if (!chart) return

  chart.setOption({
    grid: {
      top: 5,
      right: 5,
      bottom: 5,
      left: 5
    },
    xAxis: {
      type: 'category',
      show: false,
      data: props.data.map((_, i) => i)
    },
    yAxis: {
      type: 'value',
      show: false,
      min: (value: { min: number }) => value.min * 0.95,
      max: (value: { max: number }) => value.max * 1.05
    },
    series: [{
      type: 'line',
      data: props.data,
      smooth: true,
      symbol: 'none',
      lineStyle: {
        color: '#409EFF',
        width: 2
      },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(64, 158, 255, 0.3)' },
          { offset: 1, color: 'rgba(64, 158, 255, 0.05)' }
        ])
      }
    }]
  })
}

watch(() => props.data, updateChart)

onMounted(initChart)
</script>

<style scoped>
.mini-trend-chart {
  width: 100%;
  height: 100%;
}
</style>
```

**Step 3: 添加路由配置**

修改 `frontend/src/router/index.ts`，在 children 数组中（Dashboard 之后）添加：

```typescript
// ==================== 我的班级 /my-class (order: 1.5) ====================
{
  path: '/my-class',
  name: 'MyClass',
  redirect: '/my-class/list',
  meta: {
    title: '我的班级',
    icon: 'School',
    requiresAuth: true,
    requiresClass: true,  // 需要有分配班级才显示
    order: 1.5,  // 在首页之后、组织管理之前
    group: 'main'
  },
  children: [
    {
      path: '/my-class/list',
      name: 'MyClassList',
      component: () => import('@/views/myclass/MyClassListView.vue'),
      meta: {
        title: '班级列表',
        requiresAuth: true,
        hidden: true  // 不显示在子菜单
      }
    },
    {
      path: '/my-class/:classId',
      name: 'MyClassDetail',
      component: () => import('@/views/myclass/MyClassDetailView.vue'),
      meta: {
        title: '班级详情',
        requiresAuth: true,
        hidden: true
      }
    }
  ]
},
```

---

### Task 4: 前端 - 创建班级详情页面（Tab结构）

**Files:**
- Create: `frontend/src/views/myclass/MyClassDetailView.vue`
- Create: `frontend/src/views/myclass/tabs/OverviewTab.vue`
- Create: `frontend/src/views/myclass/tabs/StudentsTab.vue`

**Step 1: 创建班级详情页面框架**

```vue
<!-- frontend/src/views/myclass/MyClassDetailView.vue -->
<template>
  <div class="my-class-detail">
    <!-- 顶部导航 -->
    <div class="detail-header">
      <div class="header-left">
        <el-button text @click="goBack">
          <el-icon><ArrowLeft /></el-icon>
          返回
        </el-button>

        <!-- 班级切换器 -->
        <el-select
          v-if="allClasses.length > 1"
          v-model="currentClassId"
          class="class-selector"
          @change="onClassChange"
        >
          <el-option
            v-for="c in allClasses"
            :key="c.id"
            :label="c.className"
            :value="c.id"
          >
            <span>{{ c.className }}</span>
            <span class="class-size">{{ c.currentSize }}人</span>
          </el-option>
        </el-select>

        <h3 v-else class="class-title">{{ currentClass?.className }}</h3>
      </div>
    </div>

    <!-- Tab 导航 -->
    <el-tabs v-model="activeTab" class="detail-tabs">
      <el-tab-pane label="概览" name="overview">
        <template #label>
          <span><el-icon><DataLine /></el-icon> 概览</span>
        </template>
      </el-tab-pane>
      <el-tab-pane label="学生" name="students">
        <template #label>
          <span><el-icon><User /></el-icon> 学生</span>
        </template>
      </el-tab-pane>
      <el-tab-pane label="宿舍" name="dormitory">
        <template #label>
          <span><el-icon><House /></el-icon> 宿舍</span>
        </template>
      </el-tab-pane>
      <el-tab-pane label="分析" name="analytics">
        <template #label>
          <span><el-icon><TrendCharts /></el-icon> 分析</span>
        </template>
      </el-tab-pane>
    </el-tabs>

    <!-- Tab 内容 -->
    <div class="tab-content">
      <OverviewTab v-if="activeTab === 'overview'" :class-id="currentClassId" />
      <StudentsTab v-else-if="activeTab === 'students'" :class-id="currentClassId" />
      <DormitoryTab v-else-if="activeTab === 'dormitory'" :class-id="currentClassId" />
      <AnalyticsTab v-else-if="activeTab === 'analytics'" :class-id="currentClassId" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, DataLine, User, House, TrendCharts } from '@element-plus/icons-vue'
import { getMyClasses } from '@/api/v2/myClass'
import type { MyClassItem } from '@/types/v2/myClass'
import OverviewTab from './tabs/OverviewTab.vue'
import StudentsTab from './tabs/StudentsTab.vue'
import DormitoryTab from './tabs/DormitoryTab.vue'
import AnalyticsTab from './tabs/AnalyticsTab.vue'

const route = useRoute()
const router = useRouter()

const activeTab = ref('overview')
const allClasses = ref<MyClassItem[]>([])
const currentClassId = ref<number>(0)

const currentClass = computed(() =>
  allClasses.value.find(c => c.id === currentClassId.value)
)

const fetchClasses = async () => {
  allClasses.value = await getMyClasses()

  // 从路由参数获取classId
  const classId = Number(route.params.classId)
  if (classId && allClasses.value.some(c => c.id === classId)) {
    currentClassId.value = classId
  } else if (allClasses.value.length > 0) {
    currentClassId.value = allClasses.value[0].id
    router.replace(`/my-class/${currentClassId.value}`)
  }
}

const onClassChange = (classId: number) => {
  router.push(`/my-class/${classId}`)
}

const goBack = () => {
  if (allClasses.value.length > 1) {
    router.push('/my-class/list')
  } else {
    router.push('/dashboard')
  }
}

watch(() => route.params.classId, (newId) => {
  if (newId) {
    currentClassId.value = Number(newId)
  }
})

onMounted(fetchClasses)
</script>

<style scoped lang="scss">
.my-class-detail {
  min-height: 100vh;
  background: #f5f7fa;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  background: #fff;
  border-bottom: 1px solid #EBEEF5;

  .header-left {
    display: flex;
    align-items: center;
    gap: 16px;
  }

  .class-selector {
    width: 200px;
  }

  .class-title {
    margin: 0;
    font-size: 18px;
    font-weight: 600;
  }
}

.class-size {
  margin-left: 8px;
  color: #909399;
  font-size: 12px;
}

.detail-tabs {
  background: #fff;
  padding: 0 24px;

  :deep(.el-tabs__header) {
    margin-bottom: 0;
  }

  :deep(.el-tabs__item) {
    height: 56px;
    line-height: 56px;

    .el-icon {
      margin-right: 4px;
    }
  }
}

.tab-content {
  padding: 24px;
}
</style>
```

**Step 2: 创建概览 Tab**

```vue
<!-- frontend/src/views/myclass/tabs/OverviewTab.vue -->
<template>
  <div class="overview-tab">
    <!-- 统计卡片 -->
    <div class="stat-cards">
      <div class="stat-card">
        <div class="stat-icon student">
          <el-icon><User /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ overview?.studentCount || 0 }}</div>
          <div class="stat-label">学生人数</div>
          <div class="stat-extra">
            男 {{ overview?.maleCount || 0 }} / 女 {{ overview?.femaleCount || 0 }}
          </div>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon rank">
          <el-icon><Trophy /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-value">
            {{ overview?.classRank || '-' }}
            <span class="stat-total">/ {{ overview?.totalClasses || '-' }}</span>
          </div>
          <div class="stat-label">班级排名</div>
          <div class="stat-extra" :class="getTrendClass(overview?.scoreTrend)">
            {{ formatTrend(overview?.scoreTrend) }}
          </div>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon score">
          <el-icon><DataAnalysis /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ overview?.averageScore?.toFixed(1) || '-' }}</div>
          <div class="stat-label">平均分</div>
          <div class="stat-extra">本周</div>
        </div>
      </div>

      <div class="stat-card clickable" @click="goToAppeals">
        <div class="stat-icon appeal">
          <el-icon><ChatDotRound /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ overview?.pendingAppeals || 0 }}</div>
          <div class="stat-label">待处理申诉</div>
          <div class="stat-extra">点击查看</div>
        </div>
      </div>
    </div>

    <!-- 趋势图和快捷操作 -->
    <div class="content-row">
      <div class="trend-chart-card">
        <div class="card-header">
          <h4>成绩趋势（近30天）</h4>
        </div>
        <div class="chart-container">
          <TrendChart :data="overview?.scoreTrendList || []" />
        </div>
      </div>

      <div class="quick-actions-card">
        <div class="card-header">
          <h4>快捷操作</h4>
        </div>
        <div class="actions-grid">
          <div class="action-item" @click="goToStudents">
            <el-icon><User /></el-icon>
            <span>查看学生</span>
          </div>
          <div class="action-item" @click="exportStudentList">
            <el-icon><Download /></el-icon>
            <span>导出名单</span>
          </div>
          <div class="action-item" @click="goToDormitory">
            <el-icon><House /></el-icon>
            <span>宿舍分布</span>
          </div>
          <div class="action-item" @click="goToAnalytics">
            <el-icon><TrendCharts /></el-icon>
            <span>数据分析</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 最近检查记录 -->
    <div class="recent-records-card">
      <div class="card-header">
        <h4>最近检查记录</h4>
        <el-button text type="primary">查看全部</el-button>
      </div>
      <el-table :data="overview?.recentRecords || []" style="width: 100%">
        <el-table-column prop="checkDate" label="日期" width="120" />
        <el-table-column prop="checkType" label="检查类型" width="150" />
        <el-table-column prop="score" label="得分" width="100">
          <template #default="{ row }">
            <span :class="getScoreClass(row.score)">{{ row.score?.toFixed(1) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="rank" label="排名" width="100">
          <template #default="{ row }">
            <span :class="getRankClass(row.rank)">第{{ row.rank }}名</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click="viewRecord(row.id)">
              详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Trophy, DataAnalysis, ChatDotRound, Download, House, TrendCharts } from '@element-plus/icons-vue'
import { getClassOverview } from '@/api/v2/myClass'
import type { MyClassOverview } from '@/types/v2/myClass'
import TrendChart from '../components/TrendChart.vue'

const props = defineProps<{
  classId: number
}>()

const emit = defineEmits<{
  (e: 'change-tab', tab: string): void
}>()

const router = useRouter()
const loading = ref(false)
const overview = ref<MyClassOverview | null>(null)

const fetchOverview = async () => {
  if (!props.classId) return

  try {
    loading.value = true
    overview.value = await getClassOverview(props.classId)
  } catch (error: any) {
    ElMessage.error(error.message || '获取概览数据失败')
  } finally {
    loading.value = false
  }
}

const getTrendClass = (trend?: number) => {
  if (!trend) return ''
  return trend > 0 ? 'trend-up' : trend < 0 ? 'trend-down' : ''
}

const formatTrend = (trend?: number) => {
  if (!trend) return '-'
  const arrow = trend > 0 ? '↑' : trend < 0 ? '↓' : ''
  return `${arrow}${Math.abs(trend)} 较上周`
}

const getScoreClass = (score: number) => {
  if (score >= 95) return 'score-excellent'
  if (score >= 80) return 'score-good'
  if (score >= 60) return 'score-pass'
  return 'score-fail'
}

const getRankClass = (rank: number) => {
  if (rank <= 3) return 'rank-top'
  return ''
}

const goToStudents = () => emit('change-tab', 'students')
const goToDormitory = () => emit('change-tab', 'dormitory')
const goToAnalytics = () => emit('change-tab', 'analytics')
const goToAppeals = () => router.push('/inspection/appeals-v3')
const exportStudentList = () => ElMessage.info('导出功能开发中')
const viewRecord = (id: number) => router.push(`/inspection/check-record/${id}`)

watch(() => props.classId, fetchOverview, { immediate: true })

onMounted(fetchOverview)
</script>

<style scoped lang="scss">
.overview-tab {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.stat-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.stat-card {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  display: flex;
  gap: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);

  &.clickable {
    cursor: pointer;
    transition: all 0.3s;

    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
    }
  }
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;

  .el-icon {
    font-size: 28px;
    color: #fff;
  }

  &.student { background: linear-gradient(135deg, #409EFF, #66b1ff); }
  &.rank { background: linear-gradient(135deg, #E6A23C, #f5c270); }
  &.score { background: linear-gradient(135deg, #67C23A, #95d475); }
  &.appeal { background: linear-gradient(135deg, #909399, #b4b4b4); }
}

.stat-content {
  flex: 1;

  .stat-value {
    font-size: 28px;
    font-weight: 700;
    color: #303133;

    .stat-total {
      font-size: 16px;
      font-weight: 400;
      color: #909399;
    }
  }

  .stat-label {
    font-size: 14px;
    color: #909399;
    margin-top: 4px;
  }

  .stat-extra {
    font-size: 12px;
    color: #909399;
    margin-top: 8px;

    &.trend-up { color: #67C23A; }
    &.trend-down { color: #F56C6C; }
  }
}

.content-row {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 20px;
}

.trend-chart-card,
.quick-actions-card,
.recent-records-card {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;

    h4 {
      margin: 0;
      font-size: 16px;
      font-weight: 600;
    }
  }
}

.chart-container {
  height: 240px;
}

.actions-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.action-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 20px;
  background: #f5f7fa;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;

  .el-icon {
    font-size: 24px;
    color: #409EFF;
    margin-bottom: 8px;
  }

  span {
    font-size: 14px;
    color: #606266;
  }

  &:hover {
    background: #e6f7ff;
  }
}

.score-excellent { color: #67C23A; font-weight: 600; }
.score-good { color: #409EFF; }
.score-pass { color: #E6A23C; }
.score-fail { color: #F56C6C; }

.rank-top { color: #E6A23C; font-weight: 600; }
</style>
```

**Step 3: 创建学生列表 Tab**

```vue
<!-- frontend/src/views/myclass/tabs/StudentsTab.vue -->
<template>
  <div class="students-tab">
    <!-- 搜索和筛选 -->
    <div class="toolbar">
      <el-input
        v-model="keyword"
        placeholder="搜索学生姓名/学号"
        prefix-icon="Search"
        clearable
        style="width: 240px"
        @input="debouncedSearch"
      />
      <el-select v-model="statusFilter" placeholder="学籍状态" clearable style="width: 120px">
        <el-option label="在校" value="ENROLLED" />
        <el-option label="休学" value="SUSPENDED" />
        <el-option label="退学" value="DROPPED" />
        <el-option label="毕业" value="GRADUATED" />
      </el-select>
      <el-button type="primary" @click="exportList">
        <el-icon><Download /></el-icon>
        导出名单
      </el-button>
    </div>

    <!-- 学生列表 -->
    <el-table v-loading="loading" :data="students" style="width: 100%">
      <el-table-column label="状态" width="60">
        <template #default="{ row }">
          <span :class="['status-dot', `status-${row.status.toLowerCase()}`]"></span>
        </template>
      </el-table-column>
      <el-table-column prop="studentNo" label="学号" width="120" />
      <el-table-column prop="name" label="姓名" width="100" />
      <el-table-column prop="gender" label="性别" width="60" />
      <el-table-column label="宿舍" min-width="120">
        <template #default="{ row }">
          {{ row.dormitoryName || '-' }}
          <span v-if="row.bedNo" class="bed-no">{{ row.bedNo }}号床</span>
        </template>
      </el-table-column>
      <el-table-column prop="phone" label="联系电话" width="130" />
      <el-table-column label="学籍状态" width="100">
        <template #default="{ row }">
          <el-tag :type="getStatusType(row.status)" size="small">
            {{ getStatusLabel(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button text type="primary" size="small" @click="viewStudent(row.id)">
            详情
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 统计信息 -->
    <div class="stats-footer">
      共 {{ students.length }} 人 |
      在校 {{ enrolledCount }} |
      休学 {{ suspendedCount }} |
      退学 {{ droppedCount }}
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Download } from '@element-plus/icons-vue'
import { getClassStudents } from '@/api/v2/myClass'
import type { MyClassStudent } from '@/types/v2/myClass'
import { useDebounceFn } from '@vueuse/core'

const props = defineProps<{
  classId: number
}>()

const router = useRouter()
const loading = ref(false)
const students = ref<MyClassStudent[]>([])
const keyword = ref('')
const statusFilter = ref('')

const enrolledCount = computed(() => students.value.filter(s => s.status === 'ENROLLED').length)
const suspendedCount = computed(() => students.value.filter(s => s.status === 'SUSPENDED').length)
const droppedCount = computed(() => students.value.filter(s => s.status === 'DROPPED').length)

const fetchStudents = async () => {
  if (!props.classId) return

  try {
    loading.value = true
    students.value = await getClassStudents(props.classId, {
      keyword: keyword.value || undefined,
      status: statusFilter.value || undefined
    })
  } catch (error: any) {
    ElMessage.error(error.message || '获取学生列表失败')
  } finally {
    loading.value = false
  }
}

const debouncedSearch = useDebounceFn(fetchStudents, 300)

const getStatusType = (status: string) => {
  const map: Record<string, string> = {
    ENROLLED: 'success',
    SUSPENDED: 'warning',
    DROPPED: 'danger',
    GRADUATED: 'info'
  }
  return map[status] || 'info'
}

const getStatusLabel = (status: string) => {
  const map: Record<string, string> = {
    ENROLLED: '在校',
    SUSPENDED: '休学',
    DROPPED: '退学',
    GRADUATED: '毕业'
  }
  return map[status] || status
}

const viewStudent = (id: number) => {
  router.push(`/organization/students?id=${id}`)
}

const exportList = () => {
  ElMessage.info('导出功能开发中')
}

watch([() => props.classId, statusFilter], fetchStudents)

onMounted(fetchStudents)
</script>

<style scoped lang="scss">
.students-tab {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
}

.toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}

.status-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;

  &.status-enrolled { background: #67C23A; }
  &.status-suspended { background: #E6A23C; }
  &.status-dropped { background: #F56C6C; }
  &.status-graduated { background: #909399; }
}

.bed-no {
  color: #909399;
  font-size: 12px;
  margin-left: 4px;
}

.stats-footer {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #EBEEF5;
  color: #909399;
  font-size: 14px;
}
</style>
```

---

### Task 5: 前端 - 创建宿舍分布和数据分析 Tab

**Files:**
- Create: `frontend/src/views/myclass/tabs/DormitoryTab.vue`
- Create: `frontend/src/views/myclass/tabs/AnalyticsTab.vue`

**Step 1: 创建宿舍分布 Tab**

```vue
<!-- frontend/src/views/myclass/tabs/DormitoryTab.vue -->
<template>
  <div class="dormitory-tab">
    <!-- 统计概览 -->
    <div class="summary">
      <span>本班学生分布在 <strong>{{ buildingCount }}</strong> 栋楼 / <strong>{{ roomCount }}</strong> 间宿舍</span>
      <el-radio-group v-model="viewMode" size="small">
        <el-radio-button label="visual">可视化</el-radio-button>
        <el-radio-button label="list">列表</el-radio-button>
      </el-radio-group>
    </div>

    <!-- 可视化视图 -->
    <div v-if="viewMode === 'visual'" class="visual-view">
      <div v-for="building in distribution" :key="building.buildingId" class="building-card">
        <div class="building-header">
          <el-icon><OfficeBuilding /></el-icon>
          <span class="building-name">{{ building.buildingName }}</span>
          <el-tag size="small" :type="getBuildingTypeTag(building.buildingType)">
            {{ getBuildingTypeLabel(building.buildingType) }}
          </el-tag>
          <span class="student-count">本班 {{ building.studentCount }} 人</span>
        </div>

        <div class="rooms-grid">
          <div
            v-for="room in building.rooms"
            :key="room.dormitoryId"
            class="room-item"
            @click="showRoomDetail(room)"
          >
            <div class="room-no">{{ room.roomNo }}</div>
            <div class="room-count">{{ room.studentCount }}人</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 列表视图 -->
    <div v-else class="list-view">
      <el-table :data="flatRoomList">
        <el-table-column prop="buildingName" label="楼栋" width="120" />
        <el-table-column prop="roomNo" label="房间号" width="100" />
        <el-table-column prop="floor" label="楼层" width="80" />
        <el-table-column label="入住学生">
          <template #default="{ row }">
            <span v-for="(s, i) in row.students" :key="s.id">
              {{ s.name }}<span v-if="i < row.students.length - 1">、</span>
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="studentCount" label="人数" width="80" />
      </el-table>
    </div>

    <!-- 宿舍详情抽屉 -->
    <el-drawer v-model="drawerVisible" title="宿舍成员" direction="rtl" size="400px">
      <div v-if="selectedRoom" class="room-detail">
        <div class="room-info">
          <h4>{{ selectedRoom.roomNo }}</h4>
        </div>
        <el-table :data="selectedRoom.students" style="width: 100%">
          <el-table-column prop="name" label="姓名" />
          <el-table-column prop="bedNo" label="床位" width="80" />
        </el-table>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { OfficeBuilding } from '@element-plus/icons-vue'
import { getClassDormitoryDistribution } from '@/api/v2/myClass'
import type { DormitoryDistribution, DormitoryRoom } from '@/types/v2/myClass'

const props = defineProps<{
  classId: number
}>()

const loading = ref(false)
const distribution = ref<DormitoryDistribution[]>([])
const viewMode = ref('visual')
const drawerVisible = ref(false)
const selectedRoom = ref<DormitoryRoom | null>(null)

const buildingCount = computed(() => distribution.value.length)
const roomCount = computed(() =>
  distribution.value.reduce((sum, b) => sum + b.rooms.length, 0)
)

const flatRoomList = computed(() => {
  const list: any[] = []
  distribution.value.forEach(b => {
    b.rooms.forEach(r => {
      list.push({
        buildingName: b.buildingName,
        ...r
      })
    })
  })
  return list
})

const fetchDistribution = async () => {
  if (!props.classId) return

  try {
    loading.value = true
    distribution.value = await getClassDormitoryDistribution(props.classId)
  } catch (error: any) {
    ElMessage.error(error.message || '获取宿舍分布失败')
  } finally {
    loading.value = false
  }
}

const getBuildingTypeTag = (type: string) => {
  const map: Record<string, string> = {
    MALE: 'primary',
    FEMALE: 'danger',
    MIXED: 'warning'
  }
  return map[type] || 'info'
}

const getBuildingTypeLabel = (type: string) => {
  const map: Record<string, string> = {
    MALE: '男生',
    FEMALE: '女生',
    MIXED: '混合'
  }
  return map[type] || type
}

const showRoomDetail = (room: DormitoryRoom) => {
  selectedRoom.value = room
  drawerVisible.value = true
}

watch(() => props.classId, fetchDistribution, { immediate: true })

onMounted(fetchDistribution)
</script>

<style scoped lang="scss">
.dormitory-tab {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
}

.summary {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;

  strong {
    color: #409EFF;
    font-size: 18px;
  }
}

.visual-view {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(400px, 1fr));
  gap: 20px;
}

.building-card {
  background: #f5f7fa;
  border-radius: 12px;
  padding: 20px;
}

.building-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;

  .building-name {
    font-size: 16px;
    font-weight: 600;
  }

  .student-count {
    margin-left: auto;
    color: #909399;
    font-size: 14px;
  }
}

.rooms-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(80px, 1fr));
  gap: 12px;
}

.room-item {
  background: #fff;
  border-radius: 8px;
  padding: 12px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s;

  &:hover {
    background: #e6f7ff;
    transform: translateY(-2px);
  }

  .room-no {
    font-weight: 600;
    color: #303133;
  }

  .room-count {
    font-size: 12px;
    color: #909399;
    margin-top: 4px;
  }
}

.room-detail {
  .room-info {
    margin-bottom: 16px;

    h4 {
      margin: 0;
      font-size: 18px;
    }
  }
}
</style>
```

**Step 2: 创建数据分析 Tab (占位)**

```vue
<!-- frontend/src/views/myclass/tabs/AnalyticsTab.vue -->
<template>
  <div class="analytics-tab">
    <el-empty description="数据分析功能开发中，敬请期待">
      <template #image>
        <el-icon :size="64" color="#409EFF"><TrendCharts /></el-icon>
      </template>
    </el-empty>
  </div>
</template>

<script setup lang="ts">
import { TrendCharts } from '@element-plus/icons-vue'

defineProps<{
  classId: number
}>()
</script>

<style scoped>
.analytics-tab {
  background: #fff;
  border-radius: 12px;
  padding: 48px;
  min-height: 400px;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
```

---

### Task 6: 前端 - 创建趋势图组件

**Files:**
- Create: `frontend/src/views/myclass/components/TrendChart.vue`

```vue
<!-- frontend/src/views/myclass/components/TrendChart.vue -->
<template>
  <div ref="chartRef" class="trend-chart"></div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch, onUnmounted } from 'vue'
import * as echarts from 'echarts/core'
import { LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import type { ScoreTrendItem } from '@/types/v2/myClass'

echarts.use([LineChart, GridComponent, TooltipComponent, LegendComponent, CanvasRenderer])

const props = defineProps<{
  data: ScoreTrendItem[]
}>()

const chartRef = ref<HTMLElement>()
let chart: echarts.ECharts | null = null

const initChart = () => {
  if (!chartRef.value) return

  chart = echarts.init(chartRef.value)
  updateChart()

  window.addEventListener('resize', handleResize)
}

const handleResize = () => {
  chart?.resize()
}

const updateChart = () => {
  if (!chart) return

  const dates = props.data.map(d => d.date)
  const scores = props.data.map(d => d.score)

  chart.setOption({
    tooltip: {
      trigger: 'axis',
      formatter: (params: any) => {
        const p = params[0]
        return `${p.axisValue}<br/>得分: ${p.value.toFixed(1)}`
      }
    },
    grid: {
      top: 20,
      right: 20,
      bottom: 30,
      left: 50
    },
    xAxis: {
      type: 'category',
      data: dates,
      axisLine: { lineStyle: { color: '#E4E7ED' } },
      axisLabel: { color: '#909399', fontSize: 11 }
    },
    yAxis: {
      type: 'value',
      min: (value: { min: number }) => Math.floor(value.min * 0.95),
      max: 100,
      axisLine: { show: false },
      axisTick: { show: false },
      splitLine: { lineStyle: { color: '#EBEEF5' } },
      axisLabel: { color: '#909399' }
    },
    series: [{
      type: 'line',
      data: scores,
      smooth: true,
      symbol: 'circle',
      symbolSize: 6,
      lineStyle: {
        color: '#409EFF',
        width: 2
      },
      itemStyle: {
        color: '#409EFF',
        borderColor: '#fff',
        borderWidth: 2
      },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(64, 158, 255, 0.25)' },
          { offset: 1, color: 'rgba(64, 158, 255, 0.05)' }
        ])
      }
    }]
  })
}

watch(() => props.data, updateChart, { deep: true })

onMounted(initChart)

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  chart?.dispose()
})
</script>

<style scoped>
.trend-chart {
  width: 100%;
  height: 100%;
}
</style>
```

---

### Task 7: 菜单权限控制

**Files:**
- Modify: `frontend/src/utils/menu-generator.ts`
- Modify: `frontend/src/stores/auth.ts`

**Step 1: 扩展用户信息类型**

修改 `frontend/src/types/auth.ts`：

```typescript
// 添加字段
export interface UserInfo {
  // ... 现有字段
  assignedClasses?: {
    id: number
    className: string
    role: string
  }[]
}
```

**Step 2: 修改菜单生成逻辑**

在 `frontend/src/utils/menu-generator.ts` 中添加过滤逻辑：

```typescript
// 在过滤函数中添加
if (route.meta?.requiresClass) {
  const user = useAuthStore().user
  if (!user?.assignedClasses?.length) {
    return false
  }
}
```

**Step 3: 后端返回用户班级信息**

修改登录/用户信息接口，返回 assignedClasses 字段。

---

### Task 8: 后端 - 补充学生和宿舍查询接口

**Files:**
- Modify: `MyClassController.java`
- Modify: `MyClassApplicationService.java`

**Step 1: 添加学生查询接口**

```java
@GetMapping("/classes/{classId}/students")
@Operation(summary = "获取班级学生列表")
public Result<List<MyClassStudentDTO>> getClassStudents(
        @PathVariable Long classId,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String status,
        @AuthenticationPrincipal CustomUserDetails userDetails) {
    List<MyClassStudentDTO> students = myClassService.getClassStudents(
        classId, userDetails.getUserId(), keyword, status);
    return Result.success(students);
}
```

**Step 2: 添加宿舍分布查询接口**

```java
@GetMapping("/classes/{classId}/dormitory-distribution")
@Operation(summary = "获取班级宿舍分布")
public Result<List<DormitoryDistributionDTO>> getDormitoryDistribution(
        @PathVariable Long classId,
        @AuthenticationPrincipal CustomUserDetails userDetails) {
    List<DormitoryDistributionDTO> distribution = myClassService.getDormitoryDistribution(
        classId, userDetails.getUserId());
    return Result.success(distribution);
}
```

---

## 验收清单

- [ ] 后端 `/v2/my-class/classes` 接口返回用户管理的班级列表
- [ ] 后端 `/v2/my-class/classes/{id}/overview` 接口返回班级概览数据
- [ ] 后端 `/v2/my-class/classes/{id}/students` 接口返回班级学生列表
- [ ] 后端 `/v2/my-class/classes/{id}/dormitory-distribution` 接口返回宿舍分布
- [ ] 前端「我的班级」菜单仅对有分配班级的用户显示
- [ ] 单班级用户自动进入班级详情页
- [ ] 多班级用户看到班级卡片列表
- [ ] 班级详情页四个 Tab 功能完整
- [ ] UI 符合设计稿，交互流畅
- [ ] 数据权限正确，只能访问自己的班级

---

## 执行方式

**Plan complete and saved to `docs/plans/2026-01-18-my-class-implementation-plan.md`**

Two execution options:

**1. Subagent-Driven (this session)** - I dispatch fresh subagent per task, review between tasks, fast iteration

**2. Parallel Session (separate)** - Open new session with executing-plans, batch execution with checkpoints

**Which approach?**
