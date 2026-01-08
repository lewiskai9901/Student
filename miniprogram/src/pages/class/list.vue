<template>
  <view class="page-container">
    <!-- 自定义导航栏 -->
    <view class="nav-bar" :style="{ paddingTop: statusBarHeight + 'px' }">
      <view class="nav-content">
        <text class="nav-title">班级管理</text>
      </view>
    </view>

    <!-- 搜索和筛选 -->
    <view class="search-section" :style="{ marginTop: navBarHeight + 'px' }">
      <SearchBar
        v-model="searchKeyword"
        placeholder="搜索班级名称"
        @search="onSearch"
        @clear="onSearchClear"
      />
    </view>

    <FilterBar
      :filters="filterItems"
      @change="onFilterChange"
      @clear="onFilterClear"
    />

    <!-- 班级列表 -->
    <scroll-view
      scroll-y
      class="class-list"
      :style="{ height: listHeight + 'px' }"
      @scrolltolower="loadMore"
      refresher-enabled
      :refresher-triggered="refreshing"
      @refresherrefresh="onRefresh"
    >
      <view class="list-content">
        <!-- 加载中 -->
        <view v-if="loading && !classes.length" class="loading-wrapper">
          <u-loading-icon size="40" color="#667eea" />
          <text class="loading-text">加载中...</text>
        </view>

        <!-- 班级卡片 -->
        <view
          v-for="cls in classes"
          :key="cls.id"
          class="class-card"
          @click="goToDetail(cls)"
        >
          <view class="card-header">
            <view class="class-info">
              <text class="class-name">{{ cls.className }}</text>
              <StatusTag :type="cls.status === 1 ? 'success' : 'default'" :text="cls.status === 1 ? '正常' : '停用'" />
            </view>
            <view class="class-code">{{ cls.classCode }}</view>
          </view>

          <view class="card-body">
            <view class="info-row">
              <view class="info-item">
                <u-icon name="account" size="14" color="#9CA3AF" />
                <text class="label">班主任：</text>
                <text class="value">{{ cls.headTeacherName || '未分配' }}</text>
              </view>
            </view>
            <view class="info-row">
              <view class="info-item">
                <u-icon name="home" size="14" color="#9CA3AF" />
                <text class="label">院系：</text>
                <text class="value">{{ cls.departmentName || '-' }}</text>
              </view>
            </view>
            <view class="info-row">
              <view class="info-item">
                <u-icon name="calendar" size="14" color="#9CA3AF" />
                <text class="label">年级：</text>
                <text class="value">{{ cls.gradeName || '-' }}</text>
              </view>
            </view>
          </view>

          <view class="card-footer">
            <view class="stat-item">
              <text class="stat-value">{{ cls.studentCount || 0 }}</text>
              <text class="stat-label">学生总数</text>
            </view>
            <view class="divider"></view>
            <view class="stat-item">
              <text class="stat-value">{{ cls.maleCount || 0 }}</text>
              <text class="stat-label">男生</text>
            </view>
            <view class="divider"></view>
            <view class="stat-item">
              <text class="stat-value">{{ cls.femaleCount || 0 }}</text>
              <text class="stat-label">女生</text>
            </view>
          </view>
        </view>

        <!-- 空状态 -->
        <EmptyState
          v-if="!loading && !classes.length"
          :type="searchKeyword ? 'search' : 'default'"
          :text="searchKeyword ? '未找到匹配的班级' : '暂无班级数据'"
        />

        <!-- 加载更多 -->
        <LoadMore
          v-if="classes.length"
          :status="loadMoreStatus"
          @loadmore="loadMore"
        />
      </view>
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { getClassList, type Class } from '@/api/class'
import { getEnabledDepartments } from '@/api/department'
import { getEnabledGrades } from '@/api/grade'
import SearchBar from '@/components/common/SearchBar.vue'
import FilterBar from '@/components/common/FilterBar.vue'
import StatusTag from '@/components/common/StatusTag.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import LoadMore from '@/components/common/LoadMore.vue'

// 系统信息
const statusBarHeight = ref(0)
const navBarHeight = ref(88)
const listHeight = ref(500)

// 数据
const classes = ref<Class[]>([])
const loading = ref(false)
const refreshing = ref(false)
const loadMoreStatus = ref<'loading' | 'loadmore' | 'nomore'>('loadmore')

// 搜索和筛选
const searchKeyword = ref('')
const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  departmentId: undefined as number | undefined,
  gradeId: undefined as number | undefined,
})

// 筛选选项
const departmentList = ref<any[]>([])
const gradeList = ref<any[]>([])

const filterItems = computed(() => [
  {
    key: 'departmentId',
    label: '院系',
    value: queryParams.departmentId,
    displayValue: departmentList.value.find(d => d.id === queryParams.departmentId)?.deptName,
    options: [
      { label: '全部院系', value: '' },
      ...departmentList.value.map(d => ({ label: d.deptName, value: d.id }))
    ]
  },
  {
    key: 'gradeId',
    label: '年级',
    value: queryParams.gradeId,
    displayValue: gradeList.value.find(g => g.id === queryParams.gradeId)?.gradeName,
    options: [
      { label: '全部年级', value: '' },
      ...gradeList.value.map(g => ({ label: g.gradeName, value: g.id }))
    ]
  }
])

onMounted(() => {
  initSystemInfo()
  loadFilterOptions()
  loadData()
})

const initSystemInfo = () => {
  const sysInfo = uni.getSystemInfoSync()
  statusBarHeight.value = sysInfo.statusBarHeight || 20
  navBarHeight.value = statusBarHeight.value + 44
  const windowHeight = sysInfo.windowHeight
  listHeight.value = windowHeight - navBarHeight.value - 60 - 50
}

const loadFilterOptions = async () => {
  try {
    const [deptRes, gradeRes] = await Promise.all([
      getEnabledDepartments(),
      getEnabledGrades()
    ])
    departmentList.value = deptRes || []
    gradeList.value = gradeRes || []
  } catch (error) {
    console.error('加载筛选选项失败', error)
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
    const res = await getClassList({
      ...queryParams,
      keyword: searchKeyword.value
    })

    const newData = res.records || []
    if (isRefresh) {
      classes.value = newData
    } else {
      classes.value = [...classes.value, ...newData]
    }

    if (newData.length < queryParams.pageSize) {
      loadMoreStatus.value = 'nomore'
    } else {
      loadMoreStatus.value = 'loadmore'
    }
  } catch (error: any) {
    console.error('加载班级数据失败', error)
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

const onFilterChange = (filter: { key: string; value: any }) => {
  (queryParams as any)[filter.key] = filter.value || undefined
  loadData(true)
}

const onFilterClear = () => {
  queryParams.departmentId = undefined
  queryParams.gradeId = undefined
  loadData(true)
}

const goToDetail = (cls: Class) => {
  uni.navigateTo({
    url: `/pages/class/detail?id=${cls.id}`
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
    justify-content: center;
    height: 88rpx;
    padding: 0 24rpx;

    .nav-title {
      font-size: 36rpx;
      font-weight: 600;
      color: #fff;
    }
  }
}

.search-section {
  background: #fff;
}

.class-list {
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

.class-card {
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.04);

  .card-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 20rpx;

    .class-info {
      display: flex;
      align-items: center;
      gap: 12rpx;

      .class-name {
        font-size: 32rpx;
        font-weight: 600;
        color: #1F2937;
      }
    }

    .class-code {
      font-size: 24rpx;
      color: #9CA3AF;
    }
  }

  .card-body {
    margin-bottom: 20rpx;

    .info-row {
      display: flex;
      align-items: center;
      margin-bottom: 12rpx;

      &:last-child {
        margin-bottom: 0;
      }

      .info-item {
        display: flex;
        align-items: center;
        gap: 8rpx;

        .label {
          font-size: 24rpx;
          color: #9CA3AF;
        }

        .value {
          font-size: 24rpx;
          color: #4B5563;
        }
      }
    }
  }

  .card-footer {
    display: flex;
    align-items: center;
    padding-top: 20rpx;
    border-top: 1rpx solid #F3F4F6;

    .stat-item {
      flex: 1;
      display: flex;
      flex-direction: column;
      align-items: center;

      .stat-value {
        font-size: 36rpx;
        font-weight: 600;
        color: #667eea;
      }

      .stat-label {
        font-size: 22rpx;
        color: #9CA3AF;
        margin-top: 4rpx;
      }
    }

    .divider {
      width: 1rpx;
      height: 48rpx;
      background: #E5E7EB;
    }
  }
}
</style>
