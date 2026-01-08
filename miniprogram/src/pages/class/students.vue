<template>
  <view class="page-container">
    <!-- 自定义导航栏 -->
    <view class="nav-bar" :style="{ paddingTop: statusBarHeight + 'px' }">
      <view class="nav-content">
        <view class="nav-back" @click="goBack">
          <u-icon name="arrow-left" size="20" color="#fff" />
        </view>
        <text class="nav-title">{{ className || '班级学生' }}</text>
        <view class="nav-placeholder"></view>
      </view>
    </view>

    <!-- 搜索栏 -->
    <view class="search-section" :style="{ marginTop: navBarHeight + 'px' }">
      <SearchBar
        v-model="searchKeyword"
        placeholder="搜索学号/姓名"
        @search="onSearch"
        @clear="onSearchClear"
      />
    </view>

    <!-- 学生列表 -->
    <scroll-view
      scroll-y
      class="student-list"
      :style="{ height: listHeight + 'px' }"
      @scrolltolower="loadMore"
      refresher-enabled
      :refresher-triggered="refreshing"
      @refresherrefresh="onRefresh"
    >
      <view class="list-content">
        <!-- 统计信息 -->
        <view class="stats-bar" v-if="students.length">
          <text class="stats-text">共 {{ total }} 名学生</text>
        </view>

        <!-- 加载中 -->
        <view v-if="loading && !students.length" class="loading-wrapper">
          <u-loading-icon size="40" color="#667eea" />
          <text class="loading-text">加载中...</text>
        </view>

        <!-- 学生卡片 -->
        <StudentCard
          v-for="student in students"
          :key="student.id"
          :student="student"
          :show-actions="false"
          @click="goToDetail"
        />

        <!-- 空状态 -->
        <EmptyState
          v-if="!loading && !students.length"
          :type="searchKeyword ? 'search' : 'default'"
          :text="searchKeyword ? '未找到匹配的学生' : '该班级暂无学生'"
        />

        <!-- 加载更多 -->
        <LoadMore
          v-if="students.length"
          :status="loadMoreStatus"
          @loadmore="loadMore"
        />
      </view>
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { getStudentPage, type Student } from '@/api/student'
import SearchBar from '@/components/common/SearchBar.vue'
import StudentCard from '@/components/student/StudentCard.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import LoadMore from '@/components/common/LoadMore.vue'

// 系统信息
const statusBarHeight = ref(0)
const navBarHeight = ref(88)
const listHeight = ref(500)

// 页面参数
const classId = ref<string>('')
const className = ref<string>('')

// 数据
const students = ref<Student[]>([])
const total = ref(0)
const loading = ref(false)
const refreshing = ref(false)
const loadMoreStatus = ref<'loading' | 'loadmore' | 'nomore'>('loadmore')

// 搜索
const searchKeyword = ref('')
const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
})

onMounted(() => {
  initSystemInfo()
  loadPageParams()
  loadData()
})

const initSystemInfo = () => {
  const sysInfo = uni.getSystemInfoSync()
  statusBarHeight.value = sysInfo.statusBarHeight || 20
  navBarHeight.value = statusBarHeight.value + 44
  const windowHeight = sysInfo.windowHeight
  listHeight.value = windowHeight - navBarHeight.value - 60
}

const loadPageParams = () => {
  const pages = getCurrentPages()
  const currentPage = pages[pages.length - 1]
  classId.value = (currentPage as any).options?.id || ''
  className.value = decodeURIComponent((currentPage as any).options?.name || '')
}

const loadData = async (isRefresh = false) => {
  if (loading.value || !classId.value) return

  if (isRefresh) {
    queryParams.pageNum = 1
    loadMoreStatus.value = 'loadmore'
  }

  loading.value = true
  try {
    const res = await getStudentPage({
      ...queryParams,
      classId: Number(classId.value),
      keyword: searchKeyword.value
    })

    const newData = res.records || []
    total.value = res.total || 0

    if (isRefresh) {
      students.value = newData
    } else {
      students.value = [...students.value, ...newData]
    }

    if (newData.length < queryParams.pageSize) {
      loadMoreStatus.value = 'nomore'
    } else {
      loadMoreStatus.value = 'loadmore'
    }
  } catch (error: any) {
    console.error('加载学生数据失败', error)
    uni.showToast({ title: error.message || '加载失败', icon: 'none' })
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

const onSearch = () => {
  loadData(true)
}

const onSearchClear = () => {
  searchKeyword.value = ''
  loadData(true)
}

const goBack = () => {
  uni.navigateBack()
}

const goToDetail = (student: Student) => {
  uni.navigateTo({
    url: `/pages/student/detail?id=${student.id}`
  })
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
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }
}

.search-section {
  background: #fff;
}

.student-list {
  .list-content {
    padding: 20rpx 24rpx;
  }

  .stats-bar {
    margin-bottom: 16rpx;

    .stats-text {
      font-size: 26rpx;
      color: #6B7280;
    }
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
</style>
