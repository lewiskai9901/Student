<template>
  <view class="page-container">
    <!-- 自定义导航栏 -->
    <view class="nav-bar" :style="{ paddingTop: statusBarHeight + 'px' }">
      <view class="nav-content">
        <view class="nav-back" @click="goBack">
          <u-icon name="arrow-left" size="20" color="#fff" />
        </view>
        <text class="nav-title">用户管理</text>
        <view class="nav-placeholder"></view>
      </view>
    </view>

    <!-- 搜索栏 -->
    <view class="search-section" :style="{ marginTop: navBarHeight + 'px' }">
      <view class="search-input-wrap">
        <u-icon name="search" size="18" color="#9CA3AF" />
        <input
          type="text"
          v-model="keyword"
          placeholder="搜索用户名/姓名"
          confirm-type="search"
          @confirm="handleSearch"
        />
        <u-icon v-if="keyword" name="close-circle-fill" size="18" color="#9CA3AF" @click="clearSearch" />
      </view>
    </view>

    <!-- 角色筛选 -->
    <view class="filter-bar">
      <scroll-view scroll-x class="filter-scroll" :show-scrollbar="false">
        <view class="filter-list">
          <view
            class="filter-item"
            :class="{ active: selectedRole === '' }"
            @click="filterByRole('')"
          >
            <text>全部</text>
          </view>
          <view
            v-for="role in roles"
            :key="role.id"
            class="filter-item"
            :class="{ active: selectedRole === role.roleCode }"
            @click="filterByRole(role.roleCode)"
          >
            <text>{{ role.roleName }}</text>
          </view>
        </view>
      </scroll-view>
    </view>

    <!-- 用户列表 -->
    <scroll-view
      scroll-y
      class="user-list"
      :style="{ height: listHeight + 'px' }"
      @scrolltolower="loadMore"
      refresher-enabled
      :refresher-triggered="refreshing"
      @refresherrefresh="onRefresh"
    >
      <view class="list-content">
        <!-- 加载中 -->
        <view v-if="loading && !users.length" class="loading-wrapper">
          <u-loading-icon size="40" color="#667eea" />
          <text class="loading-text">加载中...</text>
        </view>

        <!-- 用户卡片 -->
        <view
          v-for="item in users"
          :key="item.id"
          class="user-card"
          @click="goToDetail(item)"
        >
          <view class="user-avatar">
            <image v-if="item.avatar" :src="item.avatar" mode="aspectFill" />
            <view v-else class="avatar-placeholder">
              {{ (item.realName || item.username || '').substring(0, 1) }}
            </view>
          </view>
          <view class="user-info">
            <view class="user-name-row">
              <text class="user-name">{{ item.realName || item.username }}</text>
              <view class="status-badge" :class="item.status === 1 ? 'active' : 'inactive'">
                {{ item.status === 1 ? '正常' : '禁用' }}
              </view>
            </view>
            <text class="user-account">{{ item.username }}</text>
            <view class="user-meta">
              <text class="role-tag">{{ item.roleName || '未分配角色' }}</text>
              <text v-if="item.departmentName" class="dept-text">{{ item.departmentName }}</text>
            </view>
          </view>
          <view class="card-arrow">
            <u-icon name="arrow-right" size="16" color="#9CA3AF" />
          </view>
        </view>

        <!-- 空状态 -->
        <EmptyState
          v-if="!loading && !users.length"
          type="search"
          text="暂无用户数据"
        />

        <!-- 加载更多 -->
        <LoadMore
          v-if="users.length"
          :status="loadMoreStatus"
          @loadmore="loadMore"
        />
      </view>
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { getUserList, type User } from '@/api/user'
import { getRoleList, type Role } from '@/api/role'
import EmptyState from '@/components/common/EmptyState.vue'
import LoadMore from '@/components/common/LoadMore.vue'

// 系统信息
const statusBarHeight = ref(0)
const navBarHeight = ref(88)
const listHeight = ref(500)

// 数据
const users = ref<User[]>([])
const roles = ref<Role[]>([])
const loading = ref(false)
const refreshing = ref(false)
const loadMoreStatus = ref<'loading' | 'loadmore' | 'nomore'>('loadmore')

// 搜索和筛选
const keyword = ref('')
const selectedRole = ref('')

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
})

onMounted(() => {
  initSystemInfo()
  loadRoles()
  loadData()
})

const initSystemInfo = () => {
  const sysInfo = uni.getSystemInfoSync()
  statusBarHeight.value = sysInfo.statusBarHeight || 20
  navBarHeight.value = statusBarHeight.value + 44
  const windowHeight = sysInfo.windowHeight
  listHeight.value = windowHeight - navBarHeight.value - 140
}

const loadRoles = async () => {
  try {
    const data = await getRoleList()
    roles.value = data || []
  } catch (error) {
    console.error('加载角色列表失败', error)
    roles.value = [
      { id: 1, roleCode: 'SUPER_ADMIN', roleName: '超级管理员' },
      { id: 2, roleCode: 'SCHOOL_ADMIN', roleName: '学校管理员' },
      { id: 3, roleCode: 'CLASS_TEACHER', roleName: '班主任' },
      { id: 4, roleCode: 'STUDENT', roleName: '学生' }
    ]
  }
}

const loadData = async (isRefresh = false) => {
  if (loading.value) return

  if (isRefresh) {
    queryParams.pageNum = 1
    loadMoreStatus.value = 'loadmore'
  }

  loading.value = true
  try {
    const params: any = {
      ...queryParams
    }
    if (keyword.value) {
      params.keyword = keyword.value
    }
    if (selectedRole.value) {
      params.roleCode = selectedRole.value
    }

    const res = await getUserList(params)

    const newData = res.records || []
    if (isRefresh) {
      users.value = newData
    } else {
      users.value = [...users.value, ...newData]
    }

    if (newData.length < queryParams.pageSize) {
      loadMoreStatus.value = 'nomore'
    } else {
      loadMoreStatus.value = 'loadmore'
    }
  } catch (error: any) {
    console.error('加载用户列表失败', error)
    // 模拟数据
    users.value = [
      {
        id: 1,
        username: 'admin',
        realName: '系统管理员',
        roleName: '超级管理员',
        departmentName: '信息中心',
        status: 1,
        createdAt: '2024-01-01 10:00:00'
      },
      {
        id: 2,
        username: 'teacher001',
        realName: '张老师',
        roleName: '班主任',
        departmentName: '信息工程学院',
        status: 1,
        createdAt: '2024-06-15 14:30:00'
      },
      {
        id: 3,
        username: 'teacher002',
        realName: '李老师',
        roleName: '班主任',
        departmentName: '机械工程学院',
        status: 1,
        createdAt: '2024-06-20 09:00:00'
      },
      {
        id: 4,
        username: 'student001',
        realName: '王同学',
        roleName: '学生',
        departmentName: '信息工程学院',
        status: 0,
        createdAt: '2024-09-01 08:00:00'
      }
    ]
    loadMoreStatus.value = 'nomore'
  } finally {
    loading.value = false
    refreshing.value = false
  }
}

const loadMore = () => {
  if (loadMoreStatus.value !== 'loadmore') return
  queryParams.pageNum++
  loadMoreStatus.value = 'loading'
  loadData()
}

const onRefresh = () => {
  refreshing.value = true
  loadData(true)
}

const handleSearch = () => {
  loadData(true)
}

const clearSearch = () => {
  keyword.value = ''
  loadData(true)
}

const filterByRole = (roleCode: string) => {
  selectedRole.value = roleCode
  loadData(true)
}

const goBack = () => {
  uni.navigateBack()
}

const goToDetail = (item: User) => {
  uni.showToast({ title: '用户详情页开发中', icon: 'none' })
}
</script>

<style lang="scss" scoped>
.page-container {
  min-height: 100vh;
  background: #F3F4F6;
}

.nav-bar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);

  .nav-content {
    display: flex;
    align-items: center;
    height: 88rpx;
    padding: 0 24rpx;

    .nav-back, .nav-placeholder {
      width: 64rpx;
      height: 64rpx;
      display: flex;
      align-items: center;
      justify-content: center;
    }

    .nav-title {
      flex: 1;
      font-size: 36rpx;
      font-weight: 600;
      color: #fff;
      text-align: center;
    }
  }
}

.search-section {
  background: #fff;
  padding: 16rpx 24rpx;
  border-bottom: 1rpx solid #E5E7EB;

  .search-input-wrap {
    display: flex;
    align-items: center;
    gap: 12rpx;
    background: #F3F4F6;
    border-radius: 8rpx;
    padding: 16rpx 20rpx;

    input {
      flex: 1;
      font-size: 28rpx;
      color: #111827;
    }
  }
}

.filter-bar {
  background: #fff;
  border-bottom: 1rpx solid #E5E7EB;
}

.filter-scroll {
  white-space: nowrap;
}

.filter-list {
  display: inline-flex;
  padding: 16rpx 24rpx;
  gap: 12rpx;
}

.filter-item {
  display: inline-flex;
  align-items: center;
  padding: 12rpx 24rpx;
  border-radius: 8rpx;
  background: #F3F4F6;
  font-size: 26rpx;
  color: #6B7280;
  transition: all 0.2s;

  &.active {
    background: #667eea;
    color: #fff;
  }
}

.user-list {
  .list-content {
    padding: 20rpx 24rpx;
  }

  .loading-wrapper {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 100rpx 0;

    .loading-text {
      margin-top: 20rpx;
      font-size: 28rpx;
      color: #9CA3AF;
    }
  }
}

.user-card {
  display: flex;
  align-items: center;
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx;
  margin-bottom: 16rpx;

  .user-avatar {
    width: 96rpx;
    height: 96rpx;
    margin-right: 20rpx;
    flex-shrink: 0;

    image {
      width: 100%;
      height: 100%;
      border-radius: 50%;
    }

    .avatar-placeholder {
      width: 100%;
      height: 100%;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 36rpx;
      font-weight: 600;
      color: #fff;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      border-radius: 50%;
    }
  }

  .user-info {
    flex: 1;
    min-width: 0;

    .user-name-row {
      display: flex;
      align-items: center;
      gap: 12rpx;
      margin-bottom: 4rpx;

      .user-name {
        font-size: 30rpx;
        font-weight: 600;
        color: #1F2937;
      }

      .status-badge {
        font-size: 20rpx;
        padding: 4rpx 12rpx;
        border-radius: 4rpx;

        &.active {
          background: #D1FAE5;
          color: #059669;
        }

        &.inactive {
          background: #FEE2E2;
          color: #DC2626;
        }
      }
    }

    .user-account {
      font-size: 24rpx;
      color: #9CA3AF;
      margin-bottom: 8rpx;
    }

    .user-meta {
      display: flex;
      align-items: center;
      gap: 12rpx;

      .role-tag {
        font-size: 22rpx;
        padding: 4rpx 12rpx;
        border-radius: 4rpx;
        background: rgba(102, 126, 234, 0.1);
        color: #667eea;
      }

      .dept-text {
        font-size: 22rpx;
        color: #9CA3AF;
      }
    }
  }

  .card-arrow {
    width: 40rpx;
    height: 40rpx;
    display: flex;
    align-items: center;
    justify-content: center;
  }
}
</style>
