# Miniprogram Scaffold Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Initialize the miniprogram project with uni-app Vue 3 + TS, implement login, home, message, mine pages with steel-blue professional theme, and permission-driven module system.

**Architecture:** uni-app Vue 3 + Vite + TypeScript as framework, wot-design-uni as UI library, Pinia for state management. Request layer wraps uni.request with JWT dual-token (access+refresh) auto-management. Home page renders function modules dynamically based on user permissions returned from backend `POST /api/auth/login`.

**Tech Stack:** uni-app 3.x, Vue 3, TypeScript, Vite, wot-design-uni, Pinia, z-paging

**Design reference:** `miniprogram/style-preview.html` Style #10 (钢蓝专业) and `docs/plans/2026-04-04-miniprogram-design.md`

**Backend API (already exists):**
- `POST /api/auth/login` → `{ username, password }` → `{ accessToken, refreshToken, tokenType, expiresIn, userInfo: { userId, username, realName, phone, email, avatar, gender, status, roles, permissions, orgUnitId, tenantId } }`
- `POST /api/auth/refresh` → `{ refreshToken }` → same as login response
- `POST /api/auth/logout` → `{ refreshToken?, logoutAll? }`
- `GET /api/auth/me` → `{ userInfo }`

---

### Task 1: Project Initialization

**Files:**
- Create: `miniprogram/package.json`
- Create: `miniprogram/vite.config.ts`
- Create: `miniprogram/tsconfig.json`
- Create: `miniprogram/src/main.ts`
- Create: `miniprogram/src/App.vue`
- Create: `miniprogram/src/pages.json`
- Create: `miniprogram/src/manifest.json`
- Create: `miniprogram/src/env.d.ts`
- Create: `miniprogram/src/uni.scss`
- Create: `miniprogram/index.html`
- Create: `miniprogram/src/pages/index/index.vue` (placeholder)

**Step 1: Scaffold uni-app project**

```bash
cd "/d/学生管理系统"
npx degit dcloudio/uni-preset-vue#vite-ts miniprogram-tmp
# Move contents into miniprogram/ (keep style-preview.html)
cp -r miniprogram-tmp/* miniprogram/
cp miniprogram-tmp/.gitignore miniprogram/ 2>/dev/null
rm -rf miniprogram-tmp
```

**Step 2: Install dependencies**

```bash
cd "/d/学生管理系统/miniprogram"
npm install
npm i wot-design-uni
npm i sass@1.78.0 -D
npm i pinia
npm i z-paging
```

**Step 3: Configure vite.config.ts**

Replace `miniprogram/vite.config.ts`:

```typescript
import { defineConfig } from "vite";
import uni from "@dcloudio/vite-plugin-uni";

export default defineConfig({
  plugins: [uni()],
});
```

**Step 4: Configure pages.json with easycom**

Replace `miniprogram/src/pages.json`:

```json
{
  "easycom": {
    "autoscan": true,
    "custom": {
      "^wd-(.*)": "wot-design-uni/components/wd-$1/wd-$1.vue",
      "^(?!z-paging-refresh|z-paging-load-more)z-paging(.*)": "z-paging/components/z-paging$1/z-paging$1.vue"
    }
  },
  "pages": [
    { "path": "pages/index/index", "style": { "navigationBarTitleText": "", "navigationStyle": "custom" } }
  ],
  "tabBar": {
    "color": "#c0c8d0",
    "selectedColor": "#3a7bd5",
    "backgroundColor": "#ffffff",
    "borderStyle": "white",
    "list": [
      { "pagePath": "pages/index/index", "text": "首页", "iconPath": "static/tabbar/home.png", "selectedIconPath": "static/tabbar/home-active.png" },
      { "pagePath": "pages/message/index", "text": "消息", "iconPath": "static/tabbar/message.png", "selectedIconPath": "static/tabbar/message-active.png" },
      { "pagePath": "pages/mine/index", "text": "我的", "iconPath": "static/tabbar/mine.png", "selectedIconPath": "static/tabbar/mine-active.png" }
    ]
  },
  "globalStyle": {
    "navigationBarTextStyle": "black",
    "navigationBarBackgroundColor": "#f3f5f8",
    "backgroundColor": "#f3f5f8"
  }
}
```

**Step 5: Configure main.ts with Pinia**

Replace `miniprogram/src/main.ts`:

```typescript
import { createSSRApp } from 'vue'
import * as Pinia from 'pinia'
import App from './App.vue'

export function createApp() {
  const app = createSSRApp(App)
  app.use(Pinia.createPinia())
  return { app, Pinia }
}
```

**Step 6: Add tsconfig types for wot-design-uni**

Ensure `miniprogram/tsconfig.json` compilerOptions.types includes:

```json
{
  "compilerOptions": {
    "types": ["@dcloudio/types", "wot-design-uni/global"]
  }
}
```

**Step 7: Verify build**

```bash
cd "/d/学生管理系统/miniprogram"
npm run build:mp-weixin
```

Expected: Build succeeds without errors.

**Step 8: Commit**

```bash
git add miniprogram/
git commit -m "feat(miniprogram): initialize uni-app Vue 3 + TS project with wot-design-uni, pinia, z-paging"
```

---

### Task 2: Global Theme & Styles

**Files:**
- Create: `miniprogram/src/styles/theme.scss` — design tokens
- Modify: `miniprogram/src/uni.scss` — import theme
- Modify: `miniprogram/src/App.vue` — global styles

**Step 1: Create theme.scss**

```scss
// ===== 钢蓝专业 Design Tokens =====

// Colors
$color-primary: #3a7bd5;
$color-primary-light: #e8edf4;
$color-primary-dark: #2a5fa8;
$color-danger: #e0592a;
$color-warning: #d4a030;
$color-success: #15a87e;
$color-info: #9a5cc6;

// Backgrounds
$bg-page: #f3f5f8;
$bg-card: #ffffff;
$bg-input: #ffffff;

// Text
$text-primary: #1a2840;
$text-secondary: #5a6a7a;
$text-placeholder: #a0aab4;
$text-disabled: #c0c8d0;

// Border
$border-color: #e8ecf0;
$border-light: #f0f2f5;

// Radius
$radius-card: 28rpx;
$radius-btn: 20rpx;
$radius-icon: 28rpx;
$radius-input: 20rpx;
$radius-tag: 12rpx;

// Shadows
$shadow-card: 0 4rpx 12rpx rgba(58, 123, 213, 0.06);
$shadow-search: 0 2rpx 4rpx rgba(0, 0, 0, 0.03);
$shadow-tabbar: 0 -2rpx 6rpx rgba(0, 0, 0, 0.03);

// Spacing
$spacing-xs: 8rpx;
$spacing-sm: 16rpx;
$spacing-md: 24rpx;
$spacing-lg: 32rpx;
$spacing-xl: 40rpx;
$spacing-page: 40rpx;  // page horizontal padding

// Font
$font-xs: 20rpx;
$font-sm: 22rpx;
$font-base: 26rpx;
$font-md: 28rpx;
$font-lg: 34rpx;
$font-xl: 38rpx;
$font-xxl: 44rpx;
```

**Step 2: Import in uni.scss**

Replace `miniprogram/src/uni.scss`:

```scss
@import './styles/theme.scss';

// wot-design-uni theme overrides
:root {
  --wot-color-theme: #{$color-primary};
  --wot-color-danger: #{$color-danger};
  --wot-color-warning: #{$color-warning};
  --wot-color-success: #{$color-success};
}
```

**Step 3: Set global styles in App.vue**

Replace `miniprogram/src/App.vue`:

```vue
<script setup lang="ts">
import { onLaunch } from '@dcloudio/uni-app'

onLaunch(() => {
  console.log('App Launch')
})
</script>

<style lang="scss">
page {
  background-color: $bg-page;
  color: $text-primary;
  font-family: -apple-system, BlinkMacSystemFont, 'Helvetica Neue', 'PingFang SC',
    'Microsoft YaHei', sans-serif;
  font-size: $font-md;
  line-height: 1.5;
}

// Utility classes
.text-primary { color: $text-primary; }
.text-secondary { color: $text-secondary; }
.text-placeholder { color: $text-placeholder; }
.text-brand { color: $color-primary; }
.text-danger { color: $color-danger; }
.text-success { color: $color-success; }

.bg-page { background-color: $bg-page; }
.bg-card { background-color: $bg-card; }

.card {
  background: $bg-card;
  border-radius: $radius-card;
  box-shadow: $shadow-card;
}
</style>
```

**Step 4: Commit**

```bash
git add miniprogram/src/styles/ miniprogram/src/uni.scss miniprogram/src/App.vue
git commit -m "feat(miniprogram): add steel-blue professional theme tokens and global styles"
```

---

### Task 3: TabBar Icons

**Files:**
- Create: `miniprogram/src/static/tabbar/home.png`
- Create: `miniprogram/src/static/tabbar/home-active.png`
- Create: `miniprogram/src/static/tabbar/message.png`
- Create: `miniprogram/src/static/tabbar/message-active.png`
- Create: `miniprogram/src/static/tabbar/mine.png`
- Create: `miniprogram/src/static/tabbar/mine-active.png`

**Step 1: Generate 81x81 PNG tabbar icons**

Use a script or tool to generate simple SVG-to-PNG tabbar icons. Colors: inactive `#c0c8d0`, active `#3a7bd5`. Size: 81x81px.

For quick implementation, create them as single-color SVG embedded in a generation script, or use a free icon set and convert. Alternatively, use wot-design-uni's icon font in a custom tabbar later — for now use simple PNG placeholders.

**Step 2: Commit**

```bash
git add miniprogram/src/static/
git commit -m "feat(miniprogram): add tabbar icons"
```

---

### Task 4: Types & API Layer

**Files:**
- Create: `miniprogram/src/types/auth.ts`
- Create: `miniprogram/src/types/common.ts`
- Create: `miniprogram/src/types/module.ts`
- Create: `miniprogram/src/utils/request.ts`
- Create: `miniprogram/src/api/auth.ts`

**Step 1: Create common types**

`miniprogram/src/types/common.ts`:

```typescript
/** Backend standard response wrapper */
export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
}

/** Pagination request */
export interface PageQuery {
  page?: number
  size?: number
}

/** Pagination response */
export interface PageResult<T> {
  records: T[]
  total: number
  pages: number
  current: number
  size: number
}
```

**Step 2: Create auth types**

`miniprogram/src/types/auth.ts`:

```typescript
export interface LoginRequest {
  username: string
  password: string
}

export interface LoginResponse {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
  userInfo: UserInfo
}

export interface UserInfo {
  userId: number
  username: string
  realName: string
  phone: string | null
  email: string | null
  avatar: string | null
  gender: number | null
  status: number
  roles: string[]
  permissions: string[]
  orgUnitId: number | null
  tenantId: number | null
}

export interface RefreshTokenRequest {
  refreshToken: string
}
```

**Step 3: Create module types**

`miniprogram/src/types/module.ts`:

```typescript
export interface AppModule {
  /** Permission code required to see this module */
  key: string
  /** Display label */
  label: string
  /** Icon name (wot-design-uni icon) */
  icon: string
  /** Icon color for the container background */
  iconColor: string
  /** Navigation path */
  path: string
}
```

**Step 4: Create request.ts**

`miniprogram/src/utils/request.ts`:

```typescript
import type { ApiResponse } from '@/types/common'

const BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'

let isRefreshing = false
let pendingRequests: Array<(token: string) => void> = []

function getToken(): string | null {
  return uni.getStorageSync('accessToken') || null
}

function getRefreshToken(): string | null {
  return uni.getStorageSync('refreshToken') || null
}

function setTokens(access: string, refresh: string) {
  uni.setStorageSync('accessToken', access)
  uni.setStorageSync('refreshToken', refresh)
}

function clearTokens() {
  uni.removeStorageSync('accessToken')
  uni.removeStorageSync('refreshToken')
  uni.removeStorageSync('userInfo')
}

function redirectToLogin() {
  clearTokens()
  uni.reLaunch({ url: '/pages/login/index' })
}

async function refreshToken(): Promise<string> {
  const refresh = getRefreshToken()
  if (!refresh) throw new Error('No refresh token')

  const res = await uni.request({
    url: `${BASE_URL}/auth/refresh`,
    method: 'POST',
    data: { refreshToken: refresh },
    header: { 'Content-Type': 'application/json' },
  })

  const body = res.data as ApiResponse<{ accessToken: string; refreshToken: string }>
  if (body.code !== 200 || !body.data) throw new Error('Refresh failed')

  setTokens(body.data.accessToken, body.data.refreshToken)
  return body.data.accessToken
}

function request<T = any>(options: {
  url: string
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE'
  data?: any
  header?: Record<string, string>
}): Promise<T> {
  return new Promise((resolve, reject) => {
    const token = getToken()
    const header: Record<string, string> = {
      'Content-Type': 'application/json',
      ...options.header,
    }
    if (token) header['Authorization'] = `Bearer ${token}`

    uni.request({
      url: `${BASE_URL}${options.url}`,
      method: options.method || 'GET',
      data: options.data,
      header,
      success: async (res) => {
        const statusCode = res.statusCode
        const body = res.data as ApiResponse<T>

        if (statusCode === 401) {
          // Try refresh
          if (!isRefreshing) {
            isRefreshing = true
            try {
              const newToken = await refreshToken()
              isRefreshing = false
              pendingRequests.forEach((cb) => cb(newToken))
              pendingRequests = []
              // Retry original request
              header['Authorization'] = `Bearer ${newToken}`
              uni.request({
                url: `${BASE_URL}${options.url}`,
                method: options.method || 'GET',
                data: options.data,
                header,
                success: (retryRes) => {
                  const retryBody = retryRes.data as ApiResponse<T>
                  if (retryBody.code === 200) resolve(retryBody.data)
                  else reject(new Error(retryBody.message || '请求失败'))
                },
                fail: (err) => reject(err),
              })
            } catch {
              isRefreshing = false
              pendingRequests = []
              redirectToLogin()
              reject(new Error('登录已过期'))
            }
          } else {
            // Queue this request
            pendingRequests.push((newToken: string) => {
              header['Authorization'] = `Bearer ${newToken}`
              uni.request({
                url: `${BASE_URL}${options.url}`,
                method: options.method || 'GET',
                data: options.data,
                header,
                success: (retryRes) => {
                  const retryBody = retryRes.data as ApiResponse<T>
                  if (retryBody.code === 200) resolve(retryBody.data)
                  else reject(new Error(retryBody.message || '请求失败'))
                },
                fail: (err) => reject(err),
              })
            })
          }
          return
        }

        if (statusCode >= 200 && statusCode < 300 && body.code === 200) {
          resolve(body.data)
        } else {
          const msg = body.message || `请求失败 (${statusCode})`
          uni.showToast({ title: msg, icon: 'none', duration: 2000 })
          reject(new Error(msg))
        }
      },
      fail: (err) => {
        uni.showToast({ title: '网络异常', icon: 'none', duration: 2000 })
        reject(err)
      },
    })
  })
}

export const http = {
  get: <T = any>(url: string, data?: any) => request<T>({ url, method: 'GET', data }),
  post: <T = any>(url: string, data?: any) => request<T>({ url, method: 'POST', data }),
  put: <T = any>(url: string, data?: any) => request<T>({ url, method: 'PUT', data }),
  delete: <T = any>(url: string, data?: any) => request<T>({ url, method: 'DELETE', data }),
}

export { clearTokens, setTokens, getToken }
```

**Step 5: Create auth API**

`miniprogram/src/api/auth.ts`:

```typescript
import { http, setTokens } from '@/utils/request'
import type { LoginRequest, LoginResponse, UserInfo } from '@/types/auth'

export const authApi = {
  login(data: LoginRequest) {
    return http.post<LoginResponse>('/auth/login', data)
  },

  refresh(refreshToken: string) {
    return http.post<LoginResponse>('/auth/refresh', { refreshToken })
  },

  logout(refreshToken?: string) {
    return http.post<void>('/auth/logout', { refreshToken })
  },

  getMe() {
    return http.get<UserInfo>('/auth/me')
  },
}
```

**Step 6: Create .env file**

`miniprogram/.env`:

```
VITE_API_BASE_URL=http://localhost:8080/api
```

**Step 7: Commit**

```bash
git add miniprogram/src/types/ miniprogram/src/utils/ miniprogram/src/api/ miniprogram/.env
git commit -m "feat(miniprogram): add types, request layer with JWT auto-refresh, and auth API"
```

---

### Task 5: Auth Store (Pinia)

**Files:**
- Create: `miniprogram/src/stores/auth.ts`

**Step 1: Create auth store**

`miniprogram/src/stores/auth.ts`:

```typescript
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '@/api/auth'
import { setTokens, clearTokens } from '@/utils/request'
import type { UserInfo, LoginRequest } from '@/types/auth'

export const useAuthStore = defineStore('auth', () => {
  const userInfo = ref<UserInfo | null>(null)
  const isLoggedIn = computed(() => !!userInfo.value)
  const permissions = computed(() => userInfo.value?.permissions || [])
  const roles = computed(() => userInfo.value?.roles || [])
  const realName = computed(() => userInfo.value?.realName || '')

  function hasPermission(code: string): boolean {
    return permissions.value.includes(code)
  }

  function init() {
    const cached = uni.getStorageSync('userInfo')
    if (cached) {
      try {
        userInfo.value = JSON.parse(cached)
      } catch {
        userInfo.value = null
      }
    }
  }

  async function login(data: LoginRequest) {
    const res = await authApi.login(data)
    setTokens(res.accessToken, res.refreshToken)
    userInfo.value = res.userInfo
    uni.setStorageSync('userInfo', JSON.stringify(res.userInfo))
    return res
  }

  async function logout() {
    try {
      const refreshToken = uni.getStorageSync('refreshToken')
      await authApi.logout(refreshToken)
    } catch { /* ignore */ }
    userInfo.value = null
    clearTokens()
    uni.reLaunch({ url: '/pages/login/index' })
  }

  async function fetchUserInfo() {
    const info = await authApi.getMe()
    userInfo.value = info
    uni.setStorageSync('userInfo', JSON.stringify(info))
    return info
  }

  return {
    userInfo, isLoggedIn, permissions, roles, realName,
    hasPermission, init, login, logout, fetchUserInfo,
  }
})
```

**Step 2: Initialize store on app launch**

Update `miniprogram/src/App.vue` script:

```vue
<script setup lang="ts">
import { onLaunch } from '@dcloudio/uni-app'
import { useAuthStore } from '@/stores/auth'

onLaunch(() => {
  const authStore = useAuthStore()
  authStore.init()
})
</script>
```

**Step 3: Commit**

```bash
git add miniprogram/src/stores/ miniprogram/src/App.vue
git commit -m "feat(miniprogram): add auth store with login/logout/permission management"
```

---

### Task 6: Login Page

**Files:**
- Create: `miniprogram/src/pages/login/index.vue`
- Modify: `miniprogram/src/pages.json` — add login page route

**Step 1: Add login route to pages.json**

Add to the `pages` array (NOT inside tabBar):

```json
{ "path": "pages/login/index", "style": { "navigationBarTitleText": "登录", "navigationStyle": "custom" } }
```

**Step 2: Create login page**

`miniprogram/src/pages/login/index.vue`:

```vue
<template>
  <view class="login-page">
    <view class="login-header">
      <view class="login-logo">
        <wd-icon name="user" size="60rpx" color="#3a7bd5" />
      </view>
      <text class="login-title">通用管理平台</text>
      <text class="login-subtitle">登录以继续使用</text>
    </view>

    <view class="login-form">
      <view class="form-item">
        <wd-input
          v-model="form.username"
          placeholder="请输入用户名"
          clearable
          :prefix-icon="'user'"
        />
      </view>
      <view class="form-item">
        <wd-input
          v-model="form.password"
          placeholder="请输入密码"
          show-password
          clearable
          :prefix-icon="'lock'"
        />
      </view>

      <wd-button
        type="primary"
        block
        :loading="loading"
        :disabled="!canSubmit"
        custom-class="login-btn"
        @click="handleLogin"
      >
        登录
      </wd-button>

      <view class="login-divider">
        <view class="divider-line" />
        <text class="divider-text">其他登录方式</text>
        <view class="divider-line" />
      </view>

      <wd-button
        block
        plain
        custom-class="wechat-btn"
        @click="handleWechatLogin"
      >
        微信快捷登录
      </wd-button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { reactive, ref, computed } from 'vue'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const loading = ref(false)
const form = reactive({ username: '', password: '' })
const canSubmit = computed(() => form.username.trim() && form.password.trim())

async function handleLogin() {
  if (!canSubmit.value || loading.value) return
  loading.value = true
  try {
    await authStore.login({ username: form.username.trim(), password: form.password })
    uni.switchTab({ url: '/pages/index/index' })
  } catch (e: any) {
    // Error toast handled by request layer
  } finally {
    loading.value = false
  }
}

function handleWechatLogin() {
  uni.showToast({ title: '微信登录开发中', icon: 'none' })
}
</script>

<style lang="scss" scoped>
.login-page {
  min-height: 100vh;
  background: $bg-page;
  padding: $spacing-page;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.login-header {
  text-align: center;
  margin-bottom: 80rpx;
}

.login-logo {
  width: 120rpx;
  height: 120rpx;
  border-radius: 30rpx;
  background: $color-primary-light;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 24rpx;
}

.login-title {
  display: block;
  font-size: $font-xl;
  font-weight: 700;
  color: $text-primary;
}

.login-subtitle {
  display: block;
  font-size: $font-sm;
  color: $text-placeholder;
  margin-top: 8rpx;
}

.login-form {
  background: $bg-card;
  border-radius: $radius-card;
  padding: $spacing-lg;
  box-shadow: $shadow-card;
}

.form-item {
  margin-bottom: $spacing-md;
}

.login-btn {
  margin-top: $spacing-lg;
  height: 88rpx;
  border-radius: $radius-btn !important;
}

.login-divider {
  display: flex;
  align-items: center;
  margin: $spacing-xl 0;
}

.divider-line {
  flex: 1;
  height: 1px;
  background: $border-light;
}

.divider-text {
  font-size: $font-xs;
  color: $text-placeholder;
  padding: 0 $spacing-md;
}

.wechat-btn {
  border-radius: $radius-btn !important;
}
</style>
```

**Step 3: Commit**

```bash
git add miniprogram/src/pages/login/ miniprogram/src/pages.json
git commit -m "feat(miniprogram): add login page with account/password form and wechat placeholder"
```

---

### Task 7: Module Registry & Home Page

**Files:**
- Create: `miniprogram/src/config/modules.ts`
- Modify: `miniprogram/src/pages/index/index.vue`

**Step 1: Create module registry**

`miniprogram/src/config/modules.ts`:

```typescript
import type { AppModule } from '@/types/module'

/**
 * Module registry — permission-driven.
 * Home page filters this list based on user's permissions.
 * To add a new module: add one entry here + create the page + add the route.
 */
export const moduleRegistry: AppModule[] = [
  { key: 'inspection:task:execute', label: '检查任务', icon: 'edit-outline', iconColor: '#3a7bd5', path: '/pages/inspection/task/list' },
  { key: 'inspection:task:review', label: '审核管理', icon: 'check-outline', iconColor: '#e0592a', path: '/pages/inspection/review/list' },
  { key: 'inspection:corrective:handle', label: '整改处理', icon: 'warning', iconColor: '#15a87e', path: '/pages/inspection/corrective/list' },
  { key: 'inspection:stats:view', label: '数据统计', icon: 'chart', iconColor: '#9a5cc6', path: '/pages/inspection/stats/dashboard' },
  { key: 'org:unit:view', label: '组织架构', icon: 'office-building', iconColor: '#d4a030', path: '/pages/organization/units' },
  { key: 'org:member:view', label: '人员管理', icon: 'friends', iconColor: '#4a90d9', path: '/pages/organization/members' },
  { key: 'schedule:view', label: '日程安排', icon: 'calendar', iconColor: '#e07070', path: '/pages/schedule/index' },
  { key: 'common:scan', label: '扫码', icon: 'scan', iconColor: '#50a0a0', path: '' },
]
```

**Step 2: Create home page**

Replace `miniprogram/src/pages/index/index.vue`:

```vue
<template>
  <view class="home-page">
    <!-- Status bar placeholder -->
    <view class="status-bar-placeholder" :style="{ height: statusBarHeight + 'px' }" />

    <!-- Header -->
    <view class="home-header">
      <view class="header-left">
        <text class="header-org">{{ orgName }}</text>
        <text class="header-name">Hi, {{ authStore.realName || '用户' }}</text>
      </view>
      <view class="header-right">
        <view class="header-icon-btn" @tap="onNotificationTap">
          <wd-icon name="bell" size="36rpx" color="#8a95a5" />
        </view>
        <view class="header-icon-btn" @tap="onScanTap">
          <wd-icon name="scan" size="36rpx" color="#8a95a5" />
        </view>
      </view>
    </view>

    <!-- Search bar -->
    <view class="search-bar" @tap="onSearchTap">
      <wd-icon name="search" size="28rpx" color="#b0bac4" />
      <text class="search-placeholder">搜索功能、人员、通知...</text>
    </view>

    <!-- Module grid -->
    <view class="section-row">
      <text class="section-title">功能</text>
      <text class="section-more" @tap="onAllModulesTap">全部 ></text>
    </view>
    <view class="module-grid">
      <view
        v-for="mod in visibleModules"
        :key="mod.key"
        class="module-item"
        @tap="onModuleTap(mod)"
      >
        <view class="module-icon" :style="{ background: mod.iconColor + '12' }">
          <wd-icon :name="mod.icon" size="40rpx" :color="mod.iconColor" />
        </view>
        <text class="module-name">{{ mod.label }}</text>
      </view>
    </view>

    <!-- Notices -->
    <view class="section-row">
      <text class="section-title">通知</text>
      <text class="section-more" @tap="onMoreNotices">更多 ></text>
    </view>
    <view class="notice-card">
      <view v-if="notices.length === 0" class="notice-empty">
        <text class="notice-empty-text">暂无通知</text>
      </view>
      <view
        v-for="(notice, idx) in notices"
        :key="idx"
        class="notice-item"
      >
        <view class="notice-dot" :class="{ warn: notice.type === 'warn' }" />
        <text class="notice-text">{{ notice.title }}</text>
        <text class="notice-time">{{ notice.time }}</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { moduleRegistry } from '@/config/modules'
import type { AppModule } from '@/types/module'

const authStore = useAuthStore()
const statusBarHeight = ref(0)
const orgName = ref('通用管理平台')

// Get status bar height
uni.getSystemInfo({
  success: (info) => {
    statusBarHeight.value = info.statusBarHeight || 44
  },
})

// Permission-driven module filtering
const visibleModules = computed(() => {
  if (!authStore.permissions.length) return moduleRegistry // Show all if no permissions loaded (dev mode)
  return moduleRegistry.filter((m) => authStore.hasPermission(m.key))
})

// Placeholder notices
const notices = ref([
  { title: '系统升级通知：v2.1 版本已发布', time: '10分钟', type: 'info' },
  { title: '新功能上线：支持扫码快捷操作', time: '1小时', type: 'warn' },
  { title: '4月份工作安排已发布，请查阅', time: '3小时', type: 'info' },
])

function onModuleTap(mod: AppModule) {
  if (!mod.path) {
    uni.showToast({ title: '功能开发中', icon: 'none' })
    return
  }
  uni.navigateTo({ url: mod.path })
}

function onNotificationTap() {
  uni.switchTab({ url: '/pages/message/index' })
}

function onScanTap() {
  uni.scanCode({
    success: (res) => {
      uni.showToast({ title: `扫码结果: ${res.result}`, icon: 'none' })
    },
  })
}

function onSearchTap() {
  uni.showToast({ title: '搜索功能开发中', icon: 'none' })
}

function onAllModulesTap() {
  uni.showToast({ title: '全部功能页开发中', icon: 'none' })
}

function onMoreNotices() {
  uni.switchTab({ url: '/pages/message/index' })
}
</script>

<style lang="scss" scoped>
.home-page {
  min-height: 100vh;
  background: $bg-page;
  padding-bottom: 20rpx;
}

.status-bar-placeholder {
  width: 100%;
}

// Header
.home-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16rpx $spacing-page 0;
}

.header-org {
  display: block;
  font-size: $font-xs;
  color: $text-placeholder;
  letter-spacing: 2rpx;
}

.header-name {
  display: block;
  font-size: $font-lg;
  font-weight: 700;
  color: $text-primary;
  margin-top: 4rpx;
}

.header-right {
  display: flex;
  gap: 16rpx;
}

.header-icon-btn {
  width: 72rpx;
  height: 72rpx;
  border-radius: $radius-btn;
  background: $bg-card;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: $shadow-search;
}

// Search
.search-bar {
  margin: 24rpx $spacing-page;
  height: 80rpx;
  background: $bg-card;
  border-radius: $radius-btn;
  display: flex;
  align-items: center;
  padding: 0 24rpx;
  gap: 16rpx;
  box-shadow: $shadow-search;
}

.search-placeholder {
  font-size: $font-sm;
  color: $text-placeholder;
}

// Section
.section-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24rpx $spacing-page 16rpx;
}

.section-title {
  font-size: $font-md;
  font-weight: 700;
  color: $text-primary;
}

.section-more {
  font-size: $font-sm;
  color: $color-primary;
}

// Module grid
.module-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20rpx;
  padding: 0 $spacing-page;
}

.module-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12rpx;
  padding: 16rpx 0;
}

.module-icon {
  width: 92rpx;
  height: 92rpx;
  border-radius: $radius-icon;
  display: flex;
  align-items: center;
  justify-content: center;
  background: $bg-card;
  box-shadow: $shadow-card;
}

.module-name {
  font-size: $font-sm;
  color: $text-secondary;
  font-weight: 500;
}

// Notices
.notice-card {
  margin: 0 $spacing-page;
  background: $bg-card;
  border-radius: $radius-card;
  overflow: hidden;
  box-shadow: $shadow-card;
}

.notice-empty {
  padding: 40rpx;
  text-align: center;
}

.notice-empty-text {
  font-size: $font-sm;
  color: $text-placeholder;
}

.notice-item {
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 24rpx 28rpx;

  & + & {
    border-top: 1rpx solid $border-light;
  }
}

.notice-dot {
  width: 12rpx;
  height: 12rpx;
  border-radius: 4rpx;
  background: $color-primary;
  flex-shrink: 0;

  &.warn {
    background: $color-danger;
  }
}

.notice-text {
  flex: 1;
  font-size: $font-sm;
  color: $text-secondary;
}

.notice-time {
  font-size: $font-xs;
  color: $text-placeholder;
  flex-shrink: 0;
}
</style>
```

**Step 3: Commit**

```bash
git add miniprogram/src/config/ miniprogram/src/pages/index/
git commit -m "feat(miniprogram): add permission-driven home page with module registry and notice list"
```

---

### Task 8: Message Page (TabBar Placeholder)

**Files:**
- Create: `miniprogram/src/pages/message/index.vue`
- Modify: `miniprogram/src/pages.json` — add message page route

**Step 1: Add message route**

Add to pages.json `pages` array:

```json
{ "path": "pages/message/index", "style": { "navigationBarTitleText": "消息", "navigationStyle": "custom" } }
```

**Step 2: Create message page**

`miniprogram/src/pages/message/index.vue`:

```vue
<template>
  <view class="message-page">
    <view class="status-bar-placeholder" :style="{ height: statusBarHeight + 'px' }" />
    <view class="page-title">
      <text class="title-text">消息</text>
    </view>
    <view class="empty-state">
      <wd-icon name="chat" size="80rpx" color="#c0c8d0" />
      <text class="empty-text">暂无消息</text>
      <text class="empty-sub">新的通知和消息将显示在这里</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const statusBarHeight = ref(0)
uni.getSystemInfo({ success: (info) => { statusBarHeight.value = info.statusBarHeight || 44 } })
</script>

<style lang="scss" scoped>
.message-page {
  min-height: 100vh;
  background: $bg-page;
}

.status-bar-placeholder { width: 100%; }

.page-title {
  padding: 16rpx $spacing-page;
}

.title-text {
  font-size: $font-xl;
  font-weight: 700;
  color: $text-primary;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding-top: 200rpx;
  gap: 16rpx;
}

.empty-text {
  font-size: $font-md;
  color: $text-secondary;
  font-weight: 500;
}

.empty-sub {
  font-size: $font-sm;
  color: $text-placeholder;
}
</style>
```

**Step 3: Commit**

```bash
git add miniprogram/src/pages/message/ miniprogram/src/pages.json
git commit -m "feat(miniprogram): add message page placeholder"
```

---

### Task 9: Mine Page

**Files:**
- Create: `miniprogram/src/pages/mine/index.vue`
- Modify: `miniprogram/src/pages.json` — add mine page route

**Step 1: Add mine route**

Add to pages.json `pages` array:

```json
{ "path": "pages/mine/index", "style": { "navigationBarTitleText": "我的", "navigationStyle": "custom" } }
```

**Step 2: Create mine page**

`miniprogram/src/pages/mine/index.vue`:

```vue
<template>
  <view class="mine-page">
    <view class="status-bar-placeholder" :style="{ height: statusBarHeight + 'px' }" />

    <!-- Profile card -->
    <view class="profile-card">
      <view class="profile-avatar">
        <text class="avatar-text">{{ avatarLetter }}</text>
      </view>
      <view class="profile-info">
        <text class="profile-name">{{ authStore.realName || '未登录' }}</text>
        <text class="profile-role">{{ roleDisplay }}</text>
      </view>
      <wd-icon name="arrow-right" size="32rpx" color="#c0c8d0" />
    </view>

    <!-- Menu sections -->
    <view class="menu-card">
      <view class="menu-item" @tap="onMenuTap('account')">
        <wd-icon name="user" size="36rpx" color="#3a7bd5" />
        <text class="menu-label">账号信息</text>
        <wd-icon name="arrow-right" size="28rpx" color="#c0c8d0" />
      </view>
      <view class="menu-item" @tap="onMenuTap('settings')">
        <wd-icon name="setting" size="36rpx" color="#5a6a7a" />
        <text class="menu-label">设置</text>
        <wd-icon name="arrow-right" size="28rpx" color="#c0c8d0" />
      </view>
      <view class="menu-item" @tap="onMenuTap('about')">
        <wd-icon name="info-outline" size="36rpx" color="#5a6a7a" />
        <text class="menu-label">关于</text>
        <wd-icon name="arrow-right" size="28rpx" color="#c0c8d0" />
      </view>
    </view>

    <!-- Logout -->
    <view class="logout-area" v-if="authStore.isLoggedIn">
      <wd-button type="error" plain block custom-class="logout-btn" @click="handleLogout">
        退出登录
      </wd-button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const statusBarHeight = ref(0)
uni.getSystemInfo({ success: (info) => { statusBarHeight.value = info.statusBarHeight || 44 } })

const avatarLetter = computed(() => {
  const name = authStore.realName
  return name ? name.charAt(0) : '?'
})

const roleDisplay = computed(() => {
  const roles = authStore.roles
  return roles.length > 0 ? roles.join(', ') : '暂无角色'
})

function onMenuTap(key: string) {
  uni.showToast({ title: `${key} 功能开发中`, icon: 'none' })
}

function handleLogout() {
  uni.showModal({
    title: '确认退出',
    content: '确定要退出登录吗？',
    success: (res) => {
      if (res.confirm) authStore.logout()
    },
  })
}
</script>

<style lang="scss" scoped>
.mine-page {
  min-height: 100vh;
  background: $bg-page;
}

.status-bar-placeholder { width: 100%; }

// Profile card
.profile-card {
  margin: 16rpx $spacing-page;
  background: $bg-card;
  border-radius: $radius-card;
  padding: 32rpx 28rpx;
  display: flex;
  align-items: center;
  gap: 20rpx;
  box-shadow: $shadow-card;
}

.profile-avatar {
  width: 96rpx;
  height: 96rpx;
  border-radius: 50%;
  background: $color-primary-light;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.avatar-text {
  font-size: $font-xl;
  font-weight: 700;
  color: $color-primary;
}

.profile-info {
  flex: 1;
}

.profile-name {
  display: block;
  font-size: $font-lg;
  font-weight: 700;
  color: $text-primary;
}

.profile-role {
  display: block;
  font-size: $font-sm;
  color: $text-placeholder;
  margin-top: 4rpx;
}

// Menu
.menu-card {
  margin: 20rpx $spacing-page;
  background: $bg-card;
  border-radius: $radius-card;
  overflow: hidden;
  box-shadow: $shadow-card;
}

.menu-item {
  display: flex;
  align-items: center;
  padding: 28rpx;
  gap: 20rpx;

  & + & {
    border-top: 1rpx solid $border-light;
  }
}

.menu-label {
  flex: 1;
  font-size: $font-md;
  color: $text-primary;
}

// Logout
.logout-area {
  margin: 40rpx $spacing-page;
}

.logout-btn {
  border-radius: $radius-btn !important;
}
</style>
```

**Step 3: Commit**

```bash
git add miniprogram/src/pages/mine/ miniprogram/src/pages.json
git commit -m "feat(miniprogram): add mine page with profile card, menu, and logout"
```

---

### Task 10: Route Guard & Final Integration

**Files:**
- Modify: `miniprogram/src/App.vue` — add route interceptor
- Modify: `miniprogram/src/pages.json` — verify all routes correct

**Step 1: Add route guard in App.vue**

Add to `miniprogram/src/App.vue` script:

```typescript
import { onLaunch } from '@dcloudio/uni-app'
import { useAuthStore } from '@/stores/auth'
import { getToken } from '@/utils/request'

const LOGIN_PAGE = '/pages/login/index'
const WHITE_LIST = [LOGIN_PAGE]

onLaunch(() => {
  const authStore = useAuthStore()
  authStore.init()

  // Route guard
  const interceptor = {
    invoke(args: { url: string }) {
      const path = args.url.split('?')[0]
      if (WHITE_LIST.includes(path)) return
      if (!getToken()) {
        uni.reLaunch({ url: LOGIN_PAGE })
        return false
      }
    },
  }

  uni.addInterceptor('navigateTo', interceptor)
  uni.addInterceptor('redirectTo', interceptor)
  uni.addInterceptor('reLaunch', interceptor)
  uni.addInterceptor('switchTab', {
    invoke(args: { url: string }) {
      if (!getToken()) {
        uni.reLaunch({ url: LOGIN_PAGE })
        return false
      }
    },
  })

  // Check login on launch
  if (!getToken()) {
    uni.reLaunch({ url: LOGIN_PAGE })
  }
})
```

**Step 2: Verify final pages.json**

Ensure `pages` array contains all pages in this order:

```json
{
  "pages": [
    { "path": "pages/index/index", "style": { "navigationBarTitleText": "", "navigationStyle": "custom" } },
    { "path": "pages/message/index", "style": { "navigationBarTitleText": "消息", "navigationStyle": "custom" } },
    { "path": "pages/mine/index", "style": { "navigationBarTitleText": "我的", "navigationStyle": "custom" } },
    { "path": "pages/login/index", "style": { "navigationBarTitleText": "登录", "navigationStyle": "custom" } }
  ]
}
```

**Step 3: Build and verify**

```bash
cd "/d/学生管理系统/miniprogram"
npm run build:mp-weixin
```

Expected: Build succeeds. Output in `dist/build/mp-weixin/`.

**Step 4: Commit**

```bash
git add miniprogram/
git commit -m "feat(miniprogram): add route guard and finalize page routing"
```
