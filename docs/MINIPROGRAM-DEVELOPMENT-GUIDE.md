# 学生管理系统 - 微信小程序开发文档

> 版本: 1.0
> 创建日期: 2025-12-07
> 基于项目分析报告设计

---

## 一、项目概述

### 1.1 小程序定位

基于现有Web端学生管理系统，开发配套的微信小程序，主要服务于：
- **学生**: 查看量化成绩、综测结果、提交荣誉申报
- **班主任**: 快速查看班级检查情况、审核申报
- **检查员**: 现场量化检查打分

### 1.2 技术选型

| 项目 | 技术方案 | 说明 |
|------|----------|------|
| 开发框架 | **uni-app** | 跨平台，可复用Vue技术栈 |
| UI组件库 | **uView UI 2.0** | 丰富的小程序组件 |
| 状态管理 | **Pinia** | 与Web端保持一致 |
| 请求库 | **uni.request封装** | 统一请求/响应处理 |
| 构建工具 | **Vite** | 快速构建 |

### 1.3 目录结构

```
miniprogram/
├── src/
│   ├── api/                    # API接口
│   │   ├── auth.ts            # 认证接口
│   │   ├── student.ts         # 学生接口
│   │   ├── quantification.ts  # 量化检查接口
│   │   ├── evaluation.ts      # 综测接口
│   │   └── index.ts           # 统一导出
│   │
│   ├── components/             # 公共组件
│   │   ├── ScoreCard/         # 成绩卡片
│   │   ├── CheckItem/         # 检查项组件
│   │   ├── StatusTag/         # 状态标签
│   │   └── EmptyState/        # 空状态组件
│   │
│   ├── pages/                  # 页面
│   │   ├── login/             # 登录页
│   │   ├── index/             # 首页(TabBar)
│   │   ├── student/           # 学生模块
│   │   │   ├── profile/       # 个人信息
│   │   │   ├── score/         # 量化成绩
│   │   │   └── evaluation/    # 综测结果
│   │   ├── check/             # 量化检查模块
│   │   │   ├── plan/          # 检查计划
│   │   │   ├── scoring/       # 现场打分
│   │   │   └── record/        # 检查记录
│   │   ├── honor/             # 荣誉申报模块
│   │   │   ├── list/          # 申报列表
│   │   │   ├── create/        # 提交申报
│   │   │   └── detail/        # 申报详情
│   │   └── class/             # 班级模块(班主任)
│   │       ├── students/      # 班级学生
│   │       ├── check-result/  # 检查结果
│   │       └── review/        # 审核管理
│   │
│   ├── stores/                 # 状态管理
│   │   ├── user.ts            # 用户状态
│   │   ├── check.ts           # 检查状态
│   │   └── app.ts             # 应用状态
│   │
│   ├── utils/                  # 工具函数
│   │   ├── request.ts         # 请求封装
│   │   ├── auth.ts            # 认证工具
│   │   ├── storage.ts         # 存储工具
│   │   └── format.ts          # 格式化工具
│   │
│   ├── types/                  # 类型定义
│   │   └── index.ts
│   │
│   ├── static/                 # 静态资源
│   │   ├── images/
│   │   └── icons/
│   │
│   ├── App.vue                # 应用入口
│   ├── main.ts                # 主入口
│   ├── pages.json             # 页面配置
│   ├── manifest.json          # 应用配置
│   └── uni.scss               # 全局样式
│
├── package.json
├── tsconfig.json
├── vite.config.ts
└── README.md
```

---

## 二、功能模块设计

### 2.1 用户角色与功能矩阵

| 功能模块 | 学生 | 班主任 | 检查员 | 管理员 |
|----------|------|--------|--------|--------|
| 个人信息查看 | ✅ | ✅ | ✅ | ✅ |
| 量化成绩查看 | ✅(本人) | ✅(班级) | ❌ | ✅ |
| 综测结果查看 | ✅(本人) | ✅(班级) | ❌ | ✅ |
| 荣誉申报提交 | ✅ | ✅(代提交) | ❌ | ❌ |
| 荣誉申报审核 | ❌ | ✅ | ❌ | ✅ |
| 量化检查打分 | ❌ | ❌ | ✅ | ✅ |
| 检查记录查看 | ✅(本班) | ✅(班级) | ✅ | ✅ |
| 扣分申诉 | ❌ | ✅ | ❌ | ❌ |

### 2.2 页面流程图

```
┌─────────────────────────────────────────────────────────────────────┐
│                           小程序页面流程                             │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│                        ┌──────────────┐                            │
│                        │   启动页     │                            │
│                        │ (检查登录态)  │                            │
│                        └──────┬───────┘                            │
│                               │                                     │
│              ┌────────────────┼────────────────┐                   │
│              │                │                │                   │
│              ▼                ▼                ▼                   │
│      ┌──────────────┐  ┌──────────────┐  ┌──────────────┐         │
│      │ 微信一键登录 │  │  账号密码登录 │  │   自动进入    │         │
│      │  (首次使用)   │  │   (可选)     │  │  (已登录)    │         │
│      └──────┬───────┘  └──────┬───────┘  └──────┬───────┘         │
│             │                 │                 │                   │
│             └─────────────────┼─────────────────┘                   │
│                               ▼                                     │
│                        ┌──────────────┐                            │
│                        │    首页      │                            │
│                        │  (TabBar)    │                            │
│                        └──────┬───────┘                            │
│                               │                                     │
│     ┌─────────────┬───────────┼───────────┬─────────────┐         │
│     ▼             ▼           ▼           ▼             ▼         │
│ ┌────────┐  ┌────────┐  ┌────────┐  ┌────────┐  ┌────────┐       │
│ │  首页  │  │  成绩  │  │  检查  │  │  荣誉  │  │  我的  │       │
│ │ (动态) │  │ (量化) │  │ (打分) │  │ (申报) │  │ (个人) │       │
│ └────────┘  └────────┘  └────────┘  └────────┘  └────────┘       │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

### 2.3 TabBar设计

```json
{
  "tabBar": {
    "color": "#999999",
    "selectedColor": "#409EFF",
    "backgroundColor": "#ffffff",
    "borderStyle": "black",
    "list": [
      {
        "pagePath": "pages/index/index",
        "text": "首页",
        "iconPath": "static/icons/home.png",
        "selectedIconPath": "static/icons/home-active.png"
      },
      {
        "pagePath": "pages/student/score/index",
        "text": "成绩",
        "iconPath": "static/icons/score.png",
        "selectedIconPath": "static/icons/score-active.png"
      },
      {
        "pagePath": "pages/check/plan/index",
        "text": "检查",
        "iconPath": "static/icons/check.png",
        "selectedIconPath": "static/icons/check-active.png"
      },
      {
        "pagePath": "pages/honor/list/index",
        "text": "荣誉",
        "iconPath": "static/icons/honor.png",
        "selectedIconPath": "static/icons/honor-active.png"
      },
      {
        "pagePath": "pages/mine/index",
        "text": "我的",
        "iconPath": "static/icons/mine.png",
        "selectedIconPath": "static/icons/mine-active.png"
      }
    ]
  }
}
```

---

## 三、API接口设计

### 3.1 基础请求封装

```typescript
// src/utils/request.ts
import { useUserStore } from '@/stores/user'

const BASE_URL = 'https://api.yourschool.com/api'

interface RequestConfig {
  url: string
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE'
  data?: any
  header?: Record<string, string>
  showLoading?: boolean
  showError?: boolean
}

interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
}

export async function request<T = any>(config: RequestConfig): Promise<T> {
  const userStore = useUserStore()

  // 显示加载
  if (config.showLoading !== false) {
    uni.showLoading({ title: '加载中...' })
  }

  try {
    const response = await uni.request({
      url: BASE_URL + config.url,
      method: config.method || 'GET',
      data: config.data,
      header: {
        'Content-Type': 'application/json',
        'Authorization': userStore.token ? `Bearer ${userStore.token}` : '',
        ...config.header
      }
    })

    const result = response.data as ApiResponse<T>

    // 隐藏加载
    uni.hideLoading()

    // 处理业务错误
    if (result.code !== 200) {
      // Token过期
      if (result.code === 401) {
        await handleTokenExpired()
        return Promise.reject(new Error('登录已过期'))
      }

      if (config.showError !== false) {
        uni.showToast({
          title: result.message || '请求失败',
          icon: 'none'
        })
      }
      return Promise.reject(new Error(result.message))
    }

    return result.data

  } catch (error: any) {
    uni.hideLoading()

    if (config.showError !== false) {
      uni.showToast({
        title: error.message || '网络请求失败',
        icon: 'none'
      })
    }

    return Promise.reject(error)
  }
}

// Token过期处理
async function handleTokenExpired() {
  const userStore = useUserStore()

  // 尝试刷新Token
  if (userStore.refreshToken) {
    try {
      const newToken = await refreshToken(userStore.refreshToken)
      userStore.setToken(newToken.accessToken, newToken.refreshToken)
      return true
    } catch {
      // 刷新失败，跳转登录
    }
  }

  userStore.logout()
  uni.reLaunch({ url: '/pages/login/index' })
  return false
}

// 刷新Token
async function refreshToken(refreshToken: string) {
  const response = await uni.request({
    url: BASE_URL + '/auth/refresh',
    method: 'POST',
    data: { refreshToken }
  })
  return (response.data as ApiResponse).data
}

// 快捷方法
export const get = <T>(url: string, data?: any) =>
  request<T>({ url, method: 'GET', data })

export const post = <T>(url: string, data?: any) =>
  request<T>({ url, method: 'POST', data })

export const put = <T>(url: string, data?: any) =>
  request<T>({ url, method: 'PUT', data })

export const del = <T>(url: string, data?: any) =>
  request<T>({ url, method: 'DELETE', data })
```

### 3.2 认证接口

```typescript
// src/api/auth.ts
import { post, get } from '@/utils/request'

export interface LoginRequest {
  username: string
  password: string
}

export interface WxLoginRequest {
  code: string        // 微信登录code
  encryptedData?: string
  iv?: string
}

export interface LoginResponse {
  accessToken: string
  refreshToken: string
  expiresIn: number
  user: {
    id: number
    username: string
    realName: string
    avatar: string
    roles: string[]
    permissions: string[]
  }
}

// 账号密码登录
export function login(data: LoginRequest) {
  return post<LoginResponse>('/auth/login', data)
}

// 微信一键登录
export function wxLogin(data: WxLoginRequest) {
  return post<LoginResponse>('/miniapp/auth/wx-login', data)
}

// 绑定已有账号
export function bindAccount(data: { code: string; username: string; password: string }) {
  return post<LoginResponse>('/miniapp/auth/bind', data)
}

// 获取当前用户信息
export function getCurrentUser() {
  return get<LoginResponse['user']>('/auth/current')
}

// 退出登录
export function logout() {
  return post('/auth/logout')
}

// 刷新Token
export function refreshToken(token: string) {
  return post<{ accessToken: string; refreshToken: string }>('/auth/refresh', {
    refreshToken: token
  })
}
```

### 3.3 学生信息接口

```typescript
// src/api/student.ts
import { get } from '@/utils/request'

export interface StudentInfo {
  id: number
  studentNo: string
  realName: string
  gender: number
  phone: string
  idCard: string
  className: string
  classId: number
  gradeName: string
  gradeId: number
  majorName: string
  departmentName: string
  dormitoryNo: string
  bedNumber: string
  studentStatus: number
  admissionDate: string
  avatar?: string
}

// 获取当前学生信息
export function getMyStudentInfo() {
  return get<StudentInfo>('/students/my')
}

// 获取学生详情
export function getStudentDetail(id: number) {
  return get<StudentInfo>(`/students/${id}`)
}
```

### 3.4 量化检查接口

```typescript
// src/api/quantification.ts
import { get, post } from '@/utils/request'

// ============ 类型定义 ============

export interface CheckPlan {
  id: number
  planName: string
  planCode: string
  templateId: number
  templateName: string
  startDate: string
  endDate: string
  status: number // 0-未开始 1-进行中 2-已结束
  description: string
}

export interface DailyCheck {
  id: number
  checkName: string
  checkDate: string
  templateId: number
  templateName: string
  checkType: number // 1-宿舍 2-教室 3-纪律
  status: number // 0-未开始 1-进行中 2-已完成 3-已发布
  checkerId: number
  checkerName: string
}

export interface ScoringInitData {
  checkId: number
  checkName: string
  checkDate: string
  checkType: number
  targetClasses: Array<{
    classId: number
    className: string
  }>
  categories: Array<{
    id: number
    categoryId: number
    categoryName: string
    linkType: number // 0-无 1-宿舍 2-教室
    checkRounds: number
    deductionItems: Array<{
      id: number
      itemName: string
      deductMode: number // 1-固定扣分 2-按人扣分 3-范围扣分
      fixedScore: number
      perPersonScore: number
      rangeConfig: string
      description: string
      allowPhoto: number
      allowRemark: number
      allowStudents: number
    }>
  }>
  linkResources: Record<number, {
    linkType: number
    classResources: Array<{
      classId: number
      className: string
      dormitories?: Array<{
        id: number
        dormitoryNo: string
        floor: number
        buildingName: string
      }>
      classrooms?: Array<{
        id: number
        classroomNo: string
        floor: number
        buildingName: string
      }>
    }>
  }>
  existingDetails: ScoringDetail[]
}

export interface ScoringDetail {
  id?: number
  checkRound: number
  categoryId: number
  classId: number
  deductionItemId: number
  deductionItemName?: string
  deductMode?: number
  linkType?: number
  linkId?: number
  linkNo?: string
  deductScore: number
  personCount?: number
  studentIds?: string
  studentNames?: string
  description?: string
  remark?: string
  photoUrls?: string
}

export interface CheckRecord {
  id: number
  recordCode: string
  checkName: string
  checkDate: string
  checkType: number
  status: number
  checkerName: string
  totalClasses: number
  totalDeduct: number
  avgDeduct: number
  publishedAt: string
}

export interface ClassCheckResult {
  classId: number
  className: string
  originalDeduct: number
  weightedDeduct: number
  finalScore: number
  ranking: number
  ratingLevel: string
  ratingColor: string
  items: Array<{
    categoryName: string
    itemName: string
    deductScore: number
    linkNo: string
    remark: string
  }>
}

export interface StudentCheckScore {
  checkDate: string
  checkName: string
  categoryName: string
  itemName: string
  deductScore: number
  linkNo: string
  remark: string
  appealStatus: number
}

// ============ API方法 ============

// 获取进行中的检查计划
export function getActivePlans() {
  return get<CheckPlan[]>('/check-plans/active')
}

// 获取检查计划详情
export function getPlanDetail(id: number) {
  return get<CheckPlan>(`/check-plans/${id}`)
}

// 获取计划下的日常检查列表
export function getDailyChecksByPlan(planId: number, params?: {
  pageNum?: number
  pageSize?: number
  status?: number
}) {
  return get<{ records: DailyCheck[]; total: number }>('/daily-checks', {
    planId,
    ...params
  })
}

// 获取打分初始化数据
export function getScoringInitData(checkId: number) {
  return get<ScoringInitData>(`/daily-checks/${checkId}/scoring-init`)
}

// 保存打分数据
export function saveScoring(checkId: number, data: {
  checkerId: number
  checkerName: string
  details: ScoringDetail[]
}) {
  return post(`/daily-checks/${checkId}/scoring`, data)
}

// 结束检查
export function finishCheck(checkId: number) {
  return post(`/daily-checks/${checkId}/finish`)
}

// 获取检查记录列表
export function getCheckRecords(params?: {
  pageNum?: number
  pageSize?: number
  checkDate?: string
  classId?: number
}) {
  return get<{ records: CheckRecord[]; total: number }>('/check-records', params)
}

// 获取检查记录详情
export function getCheckRecordDetail(id: number) {
  return get<CheckRecord>(`/check-records/${id}`)
}

// 获取班级检查结果
export function getClassCheckResults(recordId: number, classId?: number) {
  return get<ClassCheckResult[]>(`/check-records/${recordId}/class-results`, { classId })
}

// 获取学生量化扣分记录
export function getMyCheckScores(params?: {
  pageNum?: number
  pageSize?: number
  startDate?: string
  endDate?: string
}) {
  return get<{ records: StudentCheckScore[]; total: number }>('/check-records/my-scores', params)
}

// 获取班级量化排名
export function getClassRanking(recordId: number) {
  return get<ClassCheckResult[]>(`/check-records/${recordId}/ranking`)
}
```

### 3.5 综合测评接口

```typescript
// src/api/evaluation.ts
import { get, post } from '@/utils/request'

// ============ 类型定义 ============

export interface EvaluationPeriod {
  id: number
  periodCode: string
  periodName: string
  semesterId: number
  semesterName: string
  academicYear: string
  status: number // 0-未开始 1-数据采集 2-荣誉申报 3-审核 4-公示 5-申诉 6-已结束
  applicationStartTime: string
  applicationEndTime: string
  isLocked: number
}

export interface EvaluationResult {
  id: number
  periodId: number
  periodName: string
  studentId: number
  studentNo: string
  studentName: string
  className: string
  // 六维分数
  moralBaseScore: number
  moralBonusScore: number
  moralDeductScore: number
  moralTotalScore: number
  intellectualTotalScore: number
  physicalTotalScore: number
  aestheticTotalScore: number
  laborTotalScore: number
  developmentTotalScore: number
  // 总分和排名
  totalScore: number
  classRank: number
  classTotal: number
  gradeRank: number
  gradeTotal: number
  status: number
}

export interface EvaluationDetail {
  id: number
  detailType: string // QUANTIFICATION/RATING/HONOR/PUNISHMENT/SCORE
  evaluationDimension: string // MORAL/INTELLECTUAL/PHYSICAL/AESTHETIC/LABOR/DEVELOPMENT
  scoreCategory: string // BASE/BONUS/DEDUCT
  sourceName: string
  sourceDate: string
  score: number
}

export interface HonorType {
  id: number
  typeCode: string
  typeName: string
  category: string // COMPETITION/CERTIFICATE/TITLE/ACTIVITY/PUBLICATION/OTHER
  evaluationDimension: string
  description: string
  requiredAttachments: number
  levelConfigs: Array<{
    id: number
    levelCode: string
    levelName: string
    rankCode: string
    rankName: string
    score: number
  }>
}

export interface HonorApplication {
  id: number
  applicationCode: string
  periodId: number
  periodName: string
  studentId: number
  studentNo: string
  studentName: string
  className: string
  honorTypeId: number
  honorTypeName: string
  honorName: string
  honorLevel: string
  honorRank: string
  honorDate: string
  description: string
  attachments: string // JSON数组
  expectedScore: number
  actualScore: number
  status: number // 0-待提交 1-待班级审核 2-待系部审核 3-已通过 4-已驳回 5-已撤销
  classReviewComment: string
  deptReviewComment: string
  createdAt: string
}

// ============ API方法 ============

// 获取当前测评周期
export function getCurrentPeriod() {
  return get<EvaluationPeriod>('/evaluation/periods/current')
}

// 获取测评周期列表
export function getPeriods(params?: {
  academicYear?: string
  status?: number
}) {
  return get<EvaluationPeriod[]>('/evaluation/periods/list', params)
}

// 获取我的综测结果
export function getMyEvaluationResult(periodId: number) {
  return get<EvaluationResult>('/evaluation/results/my', { periodId })
}

// 获取综测结果明细
export function getEvaluationDetails(resultId: number) {
  return get<EvaluationDetail[]>(`/evaluation/results/${resultId}/details`)
}

// 获取班级综测排名
export function getClassEvaluationRanking(periodId: number, classId: number) {
  return get<EvaluationResult[]>('/evaluation/results/class-ranking', { periodId, classId })
}

// 获取可申报的荣誉类型
export function getAvailableHonorTypes(periodId: number) {
  return get<HonorType[]>('/evaluation/honor-types/available', { periodId })
}

// 获取荣誉类型详情
export function getHonorTypeDetail(id: number) {
  return get<HonorType>(`/evaluation/honor-types/${id}`)
}

// 获取我的荣誉申报列表
export function getMyHonorApplications(periodId?: number) {
  return get<HonorApplication[]>('/evaluation/honor-applications/my', { periodId })
}

// 提交荣誉申报
export function submitHonorApplication(data: {
  periodId: number
  honorTypeId: number
  honorLevelConfigId: number
  honorName: string
  honorDate: string
  issuingAuthority?: string
  certificateNo?: string
  description?: string
  attachments?: string[]
}) {
  return post<number>('/evaluation/honor-applications', data)
}

// 获取申报详情
export function getHonorApplicationDetail(id: number) {
  return get<HonorApplication>(`/evaluation/honor-applications/${id}`)
}

// 撤回申报
export function withdrawHonorApplication(id: number) {
  return post(`/evaluation/honor-applications/${id}/withdraw`)
}

// 获取待审核列表(班主任)
export function getPendingReviewList(level: 'class' | 'department') {
  return get<HonorApplication[]>('/evaluation/honor-applications/pending-review', { level })
}

// 审核荣誉申报(班主任)
export function reviewHonorApplication(id: number, data: {
  approved: boolean
  comment?: string
}) {
  return post(`/evaluation/honor-applications/${id}/class-review`, data)
}
```

### 3.6 文件上传接口

```typescript
// src/api/file.ts

// 上传图片
export function uploadImage(filePath: string): Promise<string> {
  return new Promise((resolve, reject) => {
    const userStore = useUserStore()

    uni.uploadFile({
      url: BASE_URL + '/files/upload',
      filePath: filePath,
      name: 'file',
      header: {
        'Authorization': `Bearer ${userStore.token}`
      },
      success: (res) => {
        const data = JSON.parse(res.data)
        if (data.code === 200) {
          resolve(data.data.url)
        } else {
          reject(new Error(data.message))
        }
      },
      fail: (err) => {
        reject(err)
      }
    })
  })
}

// 批量上传图片
export async function uploadImages(filePaths: string[]): Promise<string[]> {
  const urls: string[] = []
  for (const path of filePaths) {
    const url = await uploadImage(path)
    urls.push(url)
  }
  return urls
}
```

---

## 四、状态管理设计

### 4.1 用户状态

```typescript
// src/stores/user.ts
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getCurrentUser, logout as apiLogout } from '@/api/auth'

export interface UserInfo {
  id: number
  username: string
  realName: string
  avatar: string
  roles: string[]
  permissions: string[]
  // 学生信息
  studentId?: number
  studentNo?: string
  classId?: number
  className?: string
}

export const useUserStore = defineStore('user', () => {
  // 状态
  const token = ref<string>('')
  const refreshToken = ref<string>('')
  const userInfo = ref<UserInfo | null>(null)

  // 计算属性
  const isLogin = computed(() => !!token.value)

  const isStudent = computed(() =>
    userInfo.value?.roles.includes('student') ?? false
  )

  const isClassTeacher = computed(() =>
    userInfo.value?.roles.includes('class_teacher') ?? false
  )

  const isChecker = computed(() =>
    userInfo.value?.roles.includes('checker') ?? false
  )

  // 检查权限
  const hasPermission = (permission: string) => {
    return userInfo.value?.permissions.includes(permission) ?? false
  }

  // 设置Token
  const setToken = (accessToken: string, refresh: string) => {
    token.value = accessToken
    refreshToken.value = refresh
    uni.setStorageSync('token', accessToken)
    uni.setStorageSync('refreshToken', refresh)
  }

  // 设置用户信息
  const setUserInfo = (info: UserInfo) => {
    userInfo.value = info
    uni.setStorageSync('userInfo', JSON.stringify(info))
  }

  // 获取用户信息
  const fetchUserInfo = async () => {
    try {
      const info = await getCurrentUser()
      setUserInfo(info as UserInfo)
      return info
    } catch (error) {
      console.error('获取用户信息失败', error)
      throw error
    }
  }

  // 初始化(从本地存储恢复)
  const init = () => {
    token.value = uni.getStorageSync('token') || ''
    refreshToken.value = uni.getStorageSync('refreshToken') || ''
    const storedInfo = uni.getStorageSync('userInfo')
    if (storedInfo) {
      try {
        userInfo.value = JSON.parse(storedInfo)
      } catch {}
    }
  }

  // 退出登录
  const logout = async () => {
    try {
      await apiLogout()
    } catch {}

    token.value = ''
    refreshToken.value = ''
    userInfo.value = null
    uni.removeStorageSync('token')
    uni.removeStorageSync('refreshToken')
    uni.removeStorageSync('userInfo')
  }

  return {
    token,
    refreshToken,
    userInfo,
    isLogin,
    isStudent,
    isClassTeacher,
    isChecker,
    hasPermission,
    setToken,
    setUserInfo,
    fetchUserInfo,
    init,
    logout
  }
})
```

### 4.2 检查状态(现场打分)

```typescript
// src/stores/check.ts
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { ScoringInitData, ScoringDetail } from '@/api/quantification'

export const useCheckStore = defineStore('check', () => {
  // 当前检查数据
  const currentCheck = ref<ScoringInitData | null>(null)

  // 打分明细(本地编辑中)
  const scoringDetails = ref<ScoringDetail[]>([])

  // 当前选中的班级
  const currentClassId = ref<number | null>(null)

  // 当前选中的类别
  const currentCategoryId = ref<number | null>(null)

  // 当前检查轮次
  const currentRound = ref<number>(1)

  // 计算当前班级的扣分项
  const currentClassDetails = computed(() => {
    if (!currentClassId.value) return []
    return scoringDetails.value.filter(d => d.classId === currentClassId.value)
  })

  // 计算当前班级总扣分
  const currentClassTotalDeduct = computed(() => {
    return currentClassDetails.value.reduce((sum, d) => sum + (d.deductScore || 0), 0)
  })

  // 初始化检查数据
  const initCheck = (data: ScoringInitData) => {
    currentCheck.value = data
    scoringDetails.value = [...data.existingDetails]

    // 默认选中第一个班级
    if (data.targetClasses.length > 0) {
      currentClassId.value = data.targetClasses[0].classId
    }

    // 默认选中第一个类别
    if (data.categories.length > 0) {
      currentCategoryId.value = data.categories[0].categoryId
    }
  }

  // 添加扣分项
  const addScoringDetail = (detail: ScoringDetail) => {
    scoringDetails.value.push(detail)
  }

  // 更新扣分项
  const updateScoringDetail = (index: number, detail: Partial<ScoringDetail>) => {
    const target = scoringDetails.value[index]
    if (target) {
      Object.assign(target, detail)
    }
  }

  // 删除扣分项
  const removeScoringDetail = (index: number) => {
    scoringDetails.value.splice(index, 1)
  }

  // 切换班级
  const selectClass = (classId: number) => {
    currentClassId.value = classId
  }

  // 切换类别
  const selectCategory = (categoryId: number) => {
    currentCategoryId.value = categoryId
  }

  // 设置检查轮次
  const setRound = (round: number) => {
    currentRound.value = round
  }

  // 重置状态
  const reset = () => {
    currentCheck.value = null
    scoringDetails.value = []
    currentClassId.value = null
    currentCategoryId.value = null
    currentRound.value = 1
  }

  // 本地持久化(防止中断丢失)
  const saveToLocal = () => {
    if (currentCheck.value) {
      uni.setStorageSync('check_draft', JSON.stringify({
        checkId: currentCheck.value.checkId,
        details: scoringDetails.value,
        currentClassId: currentClassId.value,
        currentCategoryId: currentCategoryId.value,
        currentRound: currentRound.value,
        timestamp: Date.now()
      }))
    }
  }

  // 从本地恢复
  const loadFromLocal = (checkId: number): boolean => {
    const draft = uni.getStorageSync('check_draft')
    if (draft) {
      try {
        const data = JSON.parse(draft)
        // 检查是否是同一个检查且未过期(24小时)
        if (data.checkId === checkId && Date.now() - data.timestamp < 24 * 60 * 60 * 1000) {
          scoringDetails.value = data.details
          currentClassId.value = data.currentClassId
          currentCategoryId.value = data.currentCategoryId
          currentRound.value = data.currentRound
          return true
        }
      } catch {}
    }
    return false
  }

  // 清除本地缓存
  const clearLocal = () => {
    uni.removeStorageSync('check_draft')
  }

  return {
    currentCheck,
    scoringDetails,
    currentClassId,
    currentCategoryId,
    currentRound,
    currentClassDetails,
    currentClassTotalDeduct,
    initCheck,
    addScoringDetail,
    updateScoringDetail,
    removeScoringDetail,
    selectClass,
    selectCategory,
    setRound,
    reset,
    saveToLocal,
    loadFromLocal,
    clearLocal
  }
})
```

---

## 五、核心页面设计

### 5.1 登录页

```vue
<!-- src/pages/login/index.vue -->
<template>
  <view class="login-page">
    <view class="logo-section">
      <image class="logo" src="/static/images/logo.png" mode="aspectFit" />
      <text class="title">学生管理系统</text>
      <text class="subtitle">量化检查 · 综合测评</text>
    </view>

    <!-- 登录方式切换 -->
    <view class="login-form" v-if="loginType === 'account'">
      <u-form ref="formRef" :model="form" :rules="rules">
        <u-form-item prop="username">
          <u-input
            v-model="form.username"
            placeholder="请输入用户名/学号"
            prefix-icon="account"
            clearable
          />
        </u-form-item>
        <u-form-item prop="password">
          <u-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            prefix-icon="lock"
            password
          />
        </u-form-item>
      </u-form>

      <u-button
        type="primary"
        :loading="loading"
        @click="handleLogin"
        class="login-btn"
      >
        登 录
      </u-button>

      <view class="switch-login" @click="loginType = 'wechat'">
        <text>使用微信登录</text>
      </view>
    </view>

    <!-- 微信登录 -->
    <view class="wechat-login" v-else>
      <u-button
        type="success"
        :loading="loading"
        open-type="getPhoneNumber"
        @getphonenumber="handleWxLogin"
        class="wx-btn"
      >
        <u-icon name="weixin-fill" size="40" color="#fff" />
        <text>微信一键登录</text>
      </u-button>

      <view class="switch-login" @click="loginType = 'account'">
        <text>使用账号密码登录</text>
      </view>

      <view class="tip">
        首次使用需绑定学校账号
      </view>
    </view>

    <!-- 协议 -->
    <view class="agreement">
      <u-checkbox v-model="agreed" shape="circle" />
      <text>登录即表示同意</text>
      <text class="link" @click="showAgreement('user')">《用户协议》</text>
      <text>和</text>
      <text class="link" @click="showAgreement('privacy')">《隐私政策》</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { login, wxLogin } from '@/api/auth'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

const loginType = ref<'account' | 'wechat'>('wechat')
const loading = ref(false)
const agreed = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名' }],
  password: [{ required: true, message: '请输入密码' }]
}

// 账号密码登录
const handleLogin = async () => {
  if (!agreed.value) {
    uni.showToast({ title: '请先同意用户协议', icon: 'none' })
    return
  }

  loading.value = true
  try {
    const result = await login(form)
    userStore.setToken(result.accessToken, result.refreshToken)
    userStore.setUserInfo(result.user)

    uni.showToast({ title: '登录成功', icon: 'success' })

    setTimeout(() => {
      uni.switchTab({ url: '/pages/index/index' })
    }, 500)
  } catch (error) {
    console.error('登录失败', error)
  } finally {
    loading.value = false
  }
}

// 微信登录
const handleWxLogin = async (e: any) => {
  if (!agreed.value) {
    uni.showToast({ title: '请先同意用户协议', icon: 'none' })
    return
  }

  if (e.detail.errMsg !== 'getPhoneNumber:ok') {
    return
  }

  loading.value = true
  try {
    // 获取微信登录code
    const loginRes = await uni.login({ provider: 'weixin' })

    const result = await wxLogin({
      code: loginRes.code,
      encryptedData: e.detail.encryptedData,
      iv: e.detail.iv
    })

    userStore.setToken(result.accessToken, result.refreshToken)
    userStore.setUserInfo(result.user)

    uni.showToast({ title: '登录成功', icon: 'success' })

    setTimeout(() => {
      uni.switchTab({ url: '/pages/index/index' })
    }, 500)
  } catch (error: any) {
    // 如果是未绑定账号，跳转绑定页
    if (error.message?.includes('未绑定')) {
      uni.navigateTo({ url: '/pages/login/bind' })
    }
  } finally {
    loading.value = false
  }
}

const showAgreement = (type: string) => {
  uni.navigateTo({ url: `/pages/agreement/index?type=${type}` })
}
</script>

<style lang="scss" scoped>
.login-page {
  min-height: 100vh;
  padding: 100rpx 60rpx;
  background: linear-gradient(180deg, #409EFF 0%, #fff 50%);
}

.logo-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 80rpx;

  .logo {
    width: 160rpx;
    height: 160rpx;
    margin-bottom: 30rpx;
  }

  .title {
    font-size: 48rpx;
    font-weight: bold;
    color: #fff;
  }

  .subtitle {
    font-size: 28rpx;
    color: rgba(255, 255, 255, 0.8);
    margin-top: 10rpx;
  }
}

.login-form {
  background: #fff;
  border-radius: 20rpx;
  padding: 40rpx;
  box-shadow: 0 10rpx 40rpx rgba(0, 0, 0, 0.1);
}

.login-btn {
  margin-top: 40rpx;
  height: 90rpx;
  font-size: 32rpx;
}

.wechat-login {
  display: flex;
  flex-direction: column;
  align-items: center;

  .wx-btn {
    width: 80%;
    height: 100rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 20rpx;
    font-size: 32rpx;
  }

  .tip {
    margin-top: 30rpx;
    font-size: 24rpx;
    color: #999;
  }
}

.switch-login {
  text-align: center;
  margin-top: 40rpx;
  color: #409EFF;
  font-size: 28rpx;
}

.agreement {
  position: fixed;
  bottom: 60rpx;
  left: 0;
  right: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24rpx;
  color: #666;

  .link {
    color: #409EFF;
  }
}
</style>
```

### 5.2 首页

```vue
<!-- src/pages/index/index.vue -->
<template>
  <view class="index-page">
    <!-- 顶部用户信息 -->
    <view class="header">
      <view class="user-info">
        <u-avatar :src="userStore.userInfo?.avatar" size="100" />
        <view class="info">
          <text class="name">{{ userStore.userInfo?.realName }}</text>
          <text class="class">{{ studentInfo?.className || '加载中...' }}</text>
        </view>
      </view>
      <view class="date">
        <text>{{ currentDate }}</text>
      </view>
    </view>

    <!-- 快捷入口 -->
    <view class="quick-entry">
      <view class="entry-item" @click="navigateTo('/pages/student/score/index')">
        <view class="icon" style="background: #409EFF;">
          <u-icon name="order" size="50" color="#fff" />
        </view>
        <text>量化成绩</text>
      </view>
      <view class="entry-item" @click="navigateTo('/pages/student/evaluation/index')">
        <view class="icon" style="background: #67C23A;">
          <u-icon name="star" size="50" color="#fff" />
        </view>
        <text>综测结果</text>
      </view>
      <view class="entry-item" @click="navigateTo('/pages/honor/list/index')">
        <view class="icon" style="background: #E6A23C;">
          <u-icon name="award" size="50" color="#fff" />
        </view>
        <text>荣誉申报</text>
      </view>
      <view
        class="entry-item"
        v-if="userStore.isChecker"
        @click="navigateTo('/pages/check/plan/index')"
      >
        <view class="icon" style="background: #F56C6C;">
          <u-icon name="checkmark-circle" size="50" color="#fff" />
        </view>
        <text>量化检查</text>
      </view>
    </view>

    <!-- 最新动态 -->
    <view class="section">
      <view class="section-header">
        <text class="title">最新动态</text>
        <text class="more" @click="navigateTo('/pages/notice/list')">查看更多</text>
      </view>

      <view class="notice-list">
        <view
          class="notice-item"
          v-for="item in notices"
          :key="item.id"
          @click="viewNotice(item)"
        >
          <u-tag :text="item.typeText" size="mini" :type="item.tagType" />
          <text class="content">{{ item.title }}</text>
          <text class="time">{{ item.time }}</text>
        </view>

        <u-empty v-if="notices.length === 0" text="暂无动态" />
      </view>
    </view>

    <!-- 本周量化概览 (学生) -->
    <view class="section" v-if="userStore.isStudent">
      <view class="section-header">
        <text class="title">本周量化</text>
      </view>

      <view class="score-overview">
        <view class="score-card">
          <text class="label">本周扣分</text>
          <text class="value negative">-{{ weekScore.deduct }}</text>
        </view>
        <view class="score-card">
          <text class="label">班级排名</text>
          <text class="value">{{ weekScore.rank }}/{{ weekScore.total }}</text>
        </view>
        <view class="score-card">
          <text class="label">班级评级</text>
          <u-tag :text="weekScore.rating" :color="weekScore.ratingColor" />
        </view>
      </view>
    </view>

    <!-- 待办事项 (班主任/检查员) -->
    <view class="section" v-if="userStore.isClassTeacher || userStore.isChecker">
      <view class="section-header">
        <text class="title">待办事项</text>
      </view>

      <view class="todo-list">
        <view
          class="todo-item"
          v-for="item in todos"
          :key="item.id"
          @click="handleTodo(item)"
        >
          <u-badge :value="item.count" :max="99" />
          <text class="text">{{ item.title }}</text>
          <u-icon name="arrow-right" size="28" color="#999" />
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useUserStore } from '@/stores/user'
import { getMyStudentInfo } from '@/api/student'
import { formatDate } from '@/utils/format'

const userStore = useUserStore()

const studentInfo = ref<any>(null)
const notices = ref<any[]>([])
const weekScore = ref({
  deduct: 0,
  rank: 0,
  total: 0,
  rating: '-',
  ratingColor: '#999'
})
const todos = ref<any[]>([])

const currentDate = computed(() => formatDate(new Date(), 'MM月DD日 dddd'))

onMounted(async () => {
  // 获取学生信息
  if (userStore.isStudent) {
    studentInfo.value = await getMyStudentInfo()
  }

  // 加载数据...
  loadNotices()
  loadWeekScore()
  loadTodos()
})

const loadNotices = async () => {
  // TODO: 调用API
  notices.value = [
    { id: 1, type: 'check', typeText: '量化', tagType: 'primary', title: '本周宿舍检查结果已发布', time: '2小时前' },
    { id: 2, type: 'honor', typeText: '荣誉', tagType: 'success', title: '2024年度综测荣誉申报开始', time: '1天前' },
  ]
}

const loadWeekScore = async () => {
  // TODO: 调用API
  weekScore.value = {
    deduct: 5,
    rank: 12,
    total: 45,
    rating: '良好',
    ratingColor: '#67C23A'
  }
}

const loadTodos = async () => {
  if (userStore.isClassTeacher) {
    todos.value = [
      { id: 1, type: 'review', title: '待审核荣誉申报', count: 3 },
      { id: 2, type: 'appeal', title: '待处理扣分申诉', count: 1 },
    ]
  }
  if (userStore.isChecker) {
    todos.value.push(
      { id: 3, type: 'check', title: '进行中的检查', count: 2 }
    )
  }
}

const navigateTo = (url: string) => {
  uni.navigateTo({ url })
}

const viewNotice = (item: any) => {
  // TODO
}

const handleTodo = (item: any) => {
  // TODO: 根据类型跳转
}
</script>

<style lang="scss" scoped>
.index-page {
  min-height: 100vh;
  background: #f5f5f5;
}

.header {
  background: linear-gradient(135deg, #409EFF, #67C23A);
  padding: 60rpx 30rpx 80rpx;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;

  .user-info {
    display: flex;
    align-items: center;
    gap: 20rpx;

    .info {
      display: flex;
      flex-direction: column;

      .name {
        font-size: 36rpx;
        font-weight: bold;
        color: #fff;
      }

      .class {
        font-size: 26rpx;
        color: rgba(255, 255, 255, 0.8);
        margin-top: 8rpx;
      }
    }
  }

  .date {
    font-size: 26rpx;
    color: rgba(255, 255, 255, 0.8);
  }
}

.quick-entry {
  display: flex;
  justify-content: space-around;
  background: #fff;
  margin: -40rpx 20rpx 20rpx;
  padding: 30rpx;
  border-radius: 20rpx;
  box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.05);

  .entry-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 16rpx;

    .icon {
      width: 100rpx;
      height: 100rpx;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
    }

    text {
      font-size: 26rpx;
      color: #333;
    }
  }
}

.section {
  background: #fff;
  margin: 20rpx;
  padding: 30rpx;
  border-radius: 20rpx;

  .section-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20rpx;

    .title {
      font-size: 32rpx;
      font-weight: bold;
      color: #333;
    }

    .more {
      font-size: 26rpx;
      color: #409EFF;
    }
  }
}

.notice-list {
  .notice-item {
    display: flex;
    align-items: center;
    gap: 16rpx;
    padding: 20rpx 0;
    border-bottom: 1rpx solid #f0f0f0;

    &:last-child {
      border-bottom: none;
    }

    .content {
      flex: 1;
      font-size: 28rpx;
      color: #333;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .time {
      font-size: 24rpx;
      color: #999;
    }
  }
}

.score-overview {
  display: flex;
  justify-content: space-around;

  .score-card {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 10rpx;

    .label {
      font-size: 24rpx;
      color: #999;
    }

    .value {
      font-size: 36rpx;
      font-weight: bold;
      color: #333;

      &.negative {
        color: #F56C6C;
      }
    }
  }
}

.todo-list {
  .todo-item {
    display: flex;
    align-items: center;
    gap: 20rpx;
    padding: 24rpx 0;
    border-bottom: 1rpx solid #f0f0f0;

    &:last-child {
      border-bottom: none;
    }

    .text {
      flex: 1;
      font-size: 28rpx;
      color: #333;
    }
  }
}
</style>
```

### 5.3 量化检查打分页

```vue
<!-- src/pages/check/scoring/index.vue -->
<template>
  <view class="scoring-page">
    <!-- 顶部检查信息 -->
    <view class="check-info">
      <text class="name">{{ checkStore.currentCheck?.checkName }}</text>
      <text class="date">{{ checkStore.currentCheck?.checkDate }}</text>
    </view>

    <!-- 班级选择器 -->
    <scroll-view class="class-tabs" scroll-x>
      <view
        class="tab-item"
        :class="{ active: checkStore.currentClassId === cls.classId }"
        v-for="cls in checkStore.currentCheck?.targetClasses"
        :key="cls.classId"
        @click="checkStore.selectClass(cls.classId)"
      >
        <text>{{ cls.className }}</text>
        <text class="deduct" v-if="getClassDeduct(cls.classId)">
          {{ getClassDeduct(cls.classId) }}
        </text>
      </view>
    </scroll-view>

    <!-- 类别选择器 -->
    <view class="category-selector">
      <view
        class="category-item"
        :class="{ active: checkStore.currentCategoryId === cat.categoryId }"
        v-for="cat in checkStore.currentCheck?.categories"
        :key="cat.categoryId"
        @click="checkStore.selectCategory(cat.categoryId)"
      >
        {{ cat.categoryName }}
      </view>
    </view>

    <!-- 轮次选择 (如果多轮) -->
    <view class="round-selector" v-if="currentCategory?.checkRounds > 1">
      <text class="label">检查轮次:</text>
      <view class="rounds">
        <view
          class="round-item"
          :class="{ active: checkStore.currentRound === i }"
          v-for="i in currentCategory.checkRounds"
          :key="i"
          @click="checkStore.setRound(i)"
        >
          第{{ i }}轮
        </view>
      </view>
    </view>

    <!-- 关联资源选择 (宿舍/教室) -->
    <view class="link-selector" v-if="currentCategory?.linkType > 0">
      <text class="label">
        {{ currentCategory.linkType === 1 ? '选择宿舍' : '选择教室' }}:
      </text>
      <scroll-view class="link-list" scroll-x>
        <view
          class="link-item"
          :class="{ active: selectedLinkId === item.id }"
          v-for="item in linkResources"
          :key="item.id"
          @click="selectedLinkId = item.id"
        >
          {{ item.dormitoryNo || item.classroomNo }}
        </view>
      </scroll-view>
    </view>

    <!-- 扣分项列表 -->
    <view class="deduction-list">
      <view
        class="deduction-item"
        v-for="item in currentCategory?.deductionItems"
        :key="item.id"
        @click="showDeductionDialog(item)"
      >
        <view class="item-info">
          <text class="name">{{ item.itemName }}</text>
          <text class="desc" v-if="item.description">{{ item.description }}</text>
        </view>
        <view class="item-score">
          <template v-if="item.deductMode === 1">
            <text class="score">-{{ item.fixedScore }}</text>
          </template>
          <template v-else-if="item.deductMode === 2">
            <text class="score">-{{ item.perPersonScore }}/人</text>
          </template>
          <template v-else>
            <text class="score">范围扣分</text>
          </template>
        </view>
        <u-icon name="plus-circle" size="50" color="#F56C6C" />
      </view>
    </view>

    <!-- 已添加的扣分明细 -->
    <view class="added-details" v-if="filteredDetails.length > 0">
      <view class="header">
        <text>已添加扣分 ({{ filteredDetails.length }}项)</text>
        <text class="total">共计: {{ totalDeduct }}分</text>
      </view>

      <view
        class="detail-item"
        v-for="(detail, index) in filteredDetails"
        :key="index"
      >
        <view class="info">
          <text class="name">{{ detail.deductionItemName }}</text>
          <text class="link" v-if="detail.linkNo">{{ detail.linkNo }}</text>
          <text class="remark" v-if="detail.remark">{{ detail.remark }}</text>
        </view>
        <text class="score">-{{ detail.deductScore }}</text>
        <u-icon
          name="close-circle"
          size="40"
          color="#999"
          @click="removeDetail(index)"
        />
      </view>
    </view>

    <!-- 底部操作栏 -->
    <view class="bottom-bar">
      <view class="summary">
        <text>当前班级扣分: </text>
        <text class="value">{{ checkStore.currentClassTotalDeduct }}</text>
      </view>
      <view class="actions">
        <u-button size="small" @click="saveDraft">保存草稿</u-button>
        <u-button type="primary" size="small" @click="submitScoring">提交打分</u-button>
      </view>
    </view>

    <!-- 扣分弹窗 -->
    <u-popup v-model="showPopup" mode="bottom" :border-radius="20">
      <view class="deduction-popup">
        <view class="popup-header">
          <text class="title">添加扣分</text>
          <u-icon name="close" @click="showPopup = false" />
        </view>

        <view class="popup-content" v-if="selectedItem">
          <view class="item-name">{{ selectedItem.itemName }}</view>

          <!-- 固定扣分 -->
          <view class="form-item" v-if="selectedItem.deductMode === 1">
            <text class="label">扣分</text>
            <text class="value">-{{ selectedItem.fixedScore }}</text>
          </view>

          <!-- 按人扣分 -->
          <view class="form-item" v-if="selectedItem.deductMode === 2">
            <text class="label">人数</text>
            <u-number-box v-model="deductForm.personCount" :min="1" :max="20" />
            <text class="calc">= -{{ selectedItem.perPersonScore * deductForm.personCount }}分</text>
          </view>

          <!-- 范围扣分 -->
          <view class="form-item" v-if="selectedItem.deductMode === 3">
            <text class="label">扣分</text>
            <u-slider
              v-model="deductForm.deductScore"
              :min="selectedItem.minScore || 0"
              :max="selectedItem.maxScore || 10"
              :step="0.5"
              show-value
            />
          </view>

          <!-- 备注 -->
          <view class="form-item" v-if="selectedItem.allowRemark">
            <text class="label">备注</text>
            <u-input
              v-model="deductForm.remark"
              type="textarea"
              placeholder="请输入备注"
              maxlength="200"
            />
          </view>

          <!-- 拍照 -->
          <view class="form-item" v-if="selectedItem.allowPhoto">
            <text class="label">照片</text>
            <u-upload
              :file-list="deductForm.photos"
              @afterRead="afterPhotoRead"
              @delete="deletePhoto"
              :max-count="3"
            />
          </view>
        </view>

        <view class="popup-footer">
          <u-button @click="showPopup = false">取消</u-button>
          <u-button type="primary" @click="confirmDeduction">确认添加</u-button>
        </view>
      </view>
    </u-popup>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useCheckStore } from '@/stores/check'
import { useUserStore } from '@/stores/user'
import { getScoringInitData, saveScoring, finishCheck } from '@/api/quantification'
import { uploadImage } from '@/api/file'

const props = defineProps<{
  checkId: number
}>()

const checkStore = useCheckStore()
const userStore = useUserStore()

const showPopup = ref(false)
const selectedItem = ref<any>(null)
const selectedLinkId = ref<number | null>(null)

const deductForm = ref({
  personCount: 1,
  deductScore: 0,
  remark: '',
  photos: [] as any[]
})

// 当前类别
const currentCategory = computed(() => {
  return checkStore.currentCheck?.categories.find(
    c => c.categoryId === checkStore.currentCategoryId
  )
})

// 当前类别的关联资源
const linkResources = computed(() => {
  if (!currentCategory.value || currentCategory.value.linkType === 0) return []

  const linkData = checkStore.currentCheck?.linkResources[currentCategory.value.categoryId]
  if (!linkData) return []

  const classResource = linkData.classResources.find(
    r => r.classId === checkStore.currentClassId
  )
  if (!classResource) return []

  return currentCategory.value.linkType === 1
    ? classResource.dormitories
    : classResource.classrooms
})

// 过滤后的扣分明细
const filteredDetails = computed(() => {
  return checkStore.scoringDetails.filter(d =>
    d.classId === checkStore.currentClassId &&
    d.categoryId === checkStore.currentCategoryId &&
    d.checkRound === checkStore.currentRound
  )
})

// 总扣分
const totalDeduct = computed(() => {
  return filteredDetails.value.reduce((sum, d) => sum + (d.deductScore || 0), 0)
})

// 获取班级扣分
const getClassDeduct = (classId: number) => {
  const deduct = checkStore.scoringDetails
    .filter(d => d.classId === classId)
    .reduce((sum, d) => sum + (d.deductScore || 0), 0)
  return deduct ? `-${deduct}` : ''
}

// 初始化
onMounted(async () => {
  try {
    const data = await getScoringInitData(props.checkId)
    checkStore.initCheck(data)

    // 尝试恢复本地草稿
    checkStore.loadFromLocal(props.checkId)
  } catch (error) {
    uni.showToast({ title: '加载失败', icon: 'none' })
    setTimeout(() => uni.navigateBack(), 1500)
  }
})

// 自动保存
const autoSaveTimer = setInterval(() => {
  checkStore.saveToLocal()
}, 30000) // 30秒自动保存

onUnmounted(() => {
  clearInterval(autoSaveTimer)
})

// 显示扣分弹窗
const showDeductionDialog = (item: any) => {
  selectedItem.value = item
  deductForm.value = {
    personCount: 1,
    deductScore: item.fixedScore || 0,
    remark: '',
    photos: []
  }
  showPopup.value = true
}

// 确认添加扣分
const confirmDeduction = async () => {
  if (!selectedItem.value) return

  const item = selectedItem.value
  let score = 0

  if (item.deductMode === 1) {
    score = item.fixedScore
  } else if (item.deductMode === 2) {
    score = item.perPersonScore * deductForm.value.personCount
  } else {
    score = deductForm.value.deductScore
  }

  // 上传图片
  let photoUrls = ''
  if (deductForm.value.photos.length > 0) {
    const urls = await Promise.all(
      deductForm.value.photos.map(p => uploadImage(p.url))
    )
    photoUrls = JSON.stringify(urls)
  }

  checkStore.addScoringDetail({
    checkRound: checkStore.currentRound,
    categoryId: checkStore.currentCategoryId!,
    classId: checkStore.currentClassId!,
    deductionItemId: item.id,
    deductionItemName: item.itemName,
    deductMode: item.deductMode,
    linkType: currentCategory.value?.linkType || 0,
    linkId: selectedLinkId.value || undefined,
    linkNo: getLinkNo(),
    deductScore: score,
    personCount: item.deductMode === 2 ? deductForm.value.personCount : undefined,
    remark: deductForm.value.remark || undefined,
    photoUrls: photoUrls || undefined
  })

  showPopup.value = false
  selectedLinkId.value = null

  // 自动保存
  checkStore.saveToLocal()
}

// 获取关联编号
const getLinkNo = () => {
  if (!selectedLinkId.value) return ''
  const resource = linkResources.value.find((r: any) => r.id === selectedLinkId.value)
  return resource?.dormitoryNo || resource?.classroomNo || ''
}

// 删除扣分项
const removeDetail = (index: number) => {
  const realIndex = checkStore.scoringDetails.findIndex(d =>
    d.classId === checkStore.currentClassId &&
    d.categoryId === checkStore.currentCategoryId &&
    d.checkRound === checkStore.currentRound
  ) + index

  checkStore.removeScoringDetail(realIndex)
}

// 图片上传
const afterPhotoRead = (event: any) => {
  deductForm.value.photos.push({
    url: event.file.url || event.file.path
  })
}

const deletePhoto = (event: any) => {
  deductForm.value.photos.splice(event.index, 1)
}

// 保存草稿
const saveDraft = () => {
  checkStore.saveToLocal()
  uni.showToast({ title: '草稿已保存', icon: 'success' })
}

// 提交打分
const submitScoring = async () => {
  if (checkStore.scoringDetails.length === 0) {
    uni.showToast({ title: '请添加扣分项', icon: 'none' })
    return
  }

  uni.showModal({
    title: '确认提交',
    content: `共${checkStore.scoringDetails.length}条扣分记录，确认提交？`,
    success: async (res) => {
      if (res.confirm) {
        try {
          uni.showLoading({ title: '提交中...' })

          await saveScoring(props.checkId, {
            checkerId: userStore.userInfo!.id,
            checkerName: userStore.userInfo!.realName,
            details: checkStore.scoringDetails
          })

          // 清除本地缓存
          checkStore.clearLocal()

          uni.hideLoading()
          uni.showToast({ title: '提交成功', icon: 'success' })

          setTimeout(() => {
            uni.navigateBack()
          }, 1500)
        } catch (error) {
          uni.hideLoading()
        }
      }
    }
  })
}
</script>

<style lang="scss" scoped>
// 样式省略，根据实际UI设计实现
</style>
```

---

## 六、后端接口补充

### 6.1 新增小程序认证Controller

```java
// backend/src/main/java/com/school/management/controller/MiniappAuthController.java

@RestController
@RequestMapping("/miniapp/auth")
@RequiredArgsConstructor
@Tag(name = "小程序认证", description = "微信小程序登录相关接口")
public class MiniappAuthController {

    private final MiniappAuthService miniappAuthService;

    @PostMapping("/wx-login")
    @Operation(summary = "微信登录")
    public Result<LoginResponse> wxLogin(@RequestBody WxLoginRequest request) {
        return Result.success(miniappAuthService.wxLogin(request));
    }

    @PostMapping("/bind")
    @Operation(summary = "绑定已有账号")
    public Result<LoginResponse> bindAccount(@RequestBody BindAccountRequest request) {
        return Result.success(miniappAuthService.bindAccount(request));
    }

    @GetMapping("/check-bindink")
    @Operation(summary = "检查是否已绑定")
    public Result<Boolean> checkBinding(@RequestParam String openId) {
        return Result.success(miniappAuthService.checkBinding(openId));
    }
}
```

### 6.2 新增小程序专用接口

需要在现有Controller中添加以下接口:

```java
// StudentController 补充
@GetMapping("/my")
@Operation(summary = "获取当前学生信息")
public Result<StudentResponse> getMyStudentInfo() {
    Long userId = SecurityUtils.getCurrentUserId();
    return Result.success(studentService.getStudentByUserId(userId));
}

// CheckRecordController 补充
@GetMapping("/my-scores")
@Operation(summary = "获取我的量化扣分记录")
public Result<PageResult<StudentCheckScoreResponse>> getMyCheckScores(
    @RequestParam(required = false) String startDate,
    @RequestParam(required = false) String endDate,
    @RequestParam(defaultValue = "1") Integer pageNum,
    @RequestParam(defaultValue = "20") Integer pageSize
) {
    Long userId = SecurityUtils.getCurrentUserId();
    return Result.success(checkRecordService.getStudentScores(userId, startDate, endDate, pageNum, pageSize));
}

// EvaluationResultController 补充
@GetMapping("/my")
@Operation(summary = "获取我的综测结果")
public Result<EvaluationResultResponse> getMyEvaluationResult(@RequestParam Long periodId) {
    Long userId = SecurityUtils.getCurrentUserId();
    return Result.success(evaluationResultService.getByUserIdAndPeriod(userId, periodId));
}
```

---

## 七、部署与发布

### 7.1 环境配置

```javascript
// src/config/index.ts
const ENV = {
  development: {
    BASE_URL: 'http://localhost:8080/api',
    WX_APPID: 'wx_dev_appid'
  },
  production: {
    BASE_URL: 'https://api.yourschool.com/api',
    WX_APPID: 'wx_prod_appid'
  }
}

export const config = ENV[process.env.NODE_ENV || 'development']
```

### 7.2 发布流程

1. **开发环境测试**
   ```bash
   npm run dev:mp-weixin
   ```

2. **生产构建**
   ```bash
   npm run build:mp-weixin
   ```

3. **上传代码**
   - 使用微信开发者工具上传
   - 填写版本号和描述

4. **提交审核**
   - 在微信公众平台提交审核
   - 填写功能介绍、类目等信息

5. **发布上线**
   - 审核通过后发布

---

## 八、附录

### A. 权限配置

需要在 `app.json` 或 `manifest.json` 中配置:

```json
{
  "mp-weixin": {
    "appid": "your-appid",
    "permission": {
      "scope.userLocation": {
        "desc": "用于定位检查位置"
      }
    },
    "requiredPrivateInfos": [
      "getLocation"
    ]
  }
}
```

### B. 数据字典

| 字段 | 值 | 说明 |
|------|-----|------|
| 检查状态 | 0 | 未开始 |
| | 1 | 进行中 |
| | 2 | 已完成 |
| | 3 | 已发布 |
| 扣分模式 | 1 | 固定扣分 |
| | 2 | 按人扣分 |
| | 3 | 范围扣分 |
| 关联类型 | 0 | 无关联 |
| | 1 | 关联宿舍 |
| | 2 | 关联教室 |
| 荣誉申报状态 | 0 | 待提交 |
| | 1 | 待班级审核 |
| | 2 | 待系部审核 |
| | 3 | 已通过 |
| | 4 | 已驳回 |
| | 5 | 已撤销 |

### C. 错误码

| 错误码 | 说明 |
|--------|------|
| 200 | 成功 |
| 401 | 未登录或Token过期 |
| 403 | 无权限 |
| 404 | 资源不存在 |
| 500 | 服务器错误 |
| 1001 | 用户名或密码错误 |
| 1002 | 账号已被禁用 |
| 1003 | 微信账号未绑定 |
| 2001 | 检查记录不存在 |
| 2002 | 检查已结束,无法打分 |
| 3001 | 荣誉申报不存在 |
| 3002 | 不在申报时间范围内 |

---

**文档版本**: 1.0
**最后更新**: 2025-12-07
**编写者**: AI Assistant
