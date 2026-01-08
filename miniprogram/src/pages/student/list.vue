<template>
  <view class="page-container">
    <!-- 自定义导航栏 -->
    <view class="nav-bar" :style="{ paddingTop: statusBarHeight + 'px' }">
      <view class="nav-content">
        <text class="nav-title">学生管理</text>
        <view class="nav-actions">
          <view class="action-btn" @click="goToAdd" v-if="canCreate">
            <u-icon name="plus" size="22" color="#fff" />
          </view>
        </view>
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

    <!-- 筛选栏 -->
    <FilterBar
      :filters="filterItems"
      @change="onFilterChange"
      @clear="onFilterClear"
    />

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
        <!-- 加载中 -->
        <view v-if="loading && !students.length" class="loading-wrapper">
          <u-loading-icon size="40" color="#667eea" />
          <text class="loading-text">加载中...</text>
        </view>

        <!-- 学生列表 -->
        <StudentCard
          v-for="student in students"
          :key="student.id"
          :student="student"
          :show-actions="canEdit"
          @click="goToDetail"
          @edit="goToEdit"
          @more="showActionSheet"
        />

        <!-- 空状态 -->
        <EmptyState
          v-if="!loading && !students.length"
          :type="searchKeyword ? 'search' : 'default'"
          :text="searchKeyword ? '未找到匹配的学生' : '暂无学生数据'"
        />

        <!-- 加载更多 -->
        <LoadMore
          v-if="students.length"
          :status="loadMoreStatus"
          @loadmore="loadMore"
        />
      </view>
    </scroll-view>

    <!-- 操作菜单 -->
    <u-action-sheet
      :show="showSheet"
      :actions="sheetActions"
      @close="showSheet = false"
      @select="onSheetSelect"
    />
  </view>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { getStudentPage, deleteStudent, type Student } from '@/api/student'
import { getAllClasses } from '@/api/class'
import { getEnabledDepartments } from '@/api/department'
import { hasPermission, PermissionCode } from '@/utils/permission'
import SearchBar from '@/components/common/SearchBar.vue'
import FilterBar from '@/components/common/FilterBar.vue'
import StudentCard from '@/components/student/StudentCard.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import LoadMore from '@/components/common/LoadMore.vue'

// 系统信息
const statusBarHeight = ref(0)
const navBarHeight = ref(88)
const listHeight = ref(500)

// 权限
const canCreate = computed(() => hasPermission(PermissionCode.STUDENT_CREATE))
const canEdit = computed(() => hasPermission(PermissionCode.STUDENT_UPDATE))
const canDelete = computed(() => hasPermission(PermissionCode.STUDENT_DELETE))

// 数据
const students = ref<Student[]>([])
const loading = ref(false)
const refreshing = ref(false)
const loadMoreStatus = ref<'loading' | 'loadmore' | 'nomore'>('loadmore')

// 搜索和筛选
const searchKeyword = ref('')
const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  classId: undefined as number | undefined,
  departmentId: undefined as number | undefined,
  status: undefined as number | undefined,
})

// 筛选选项
const classList = ref<any[]>([])
const departmentList = ref<any[]>([])
const statusOptions = [
  { label: '全部状态', value: '' },
  { label: '在籍', value: 1 },
  { label: '已毕业', value: 2 },
  { label: '休学', value: 3 },
  { label: '退学', value: 4 },
]

const filterItems = computed(() => [
  {
    key: 'classId',
    label: '班级',
    value: queryParams.classId,
    displayValue: classList.value.find(c => c.id === queryParams.classId)?.className,
    options: [
      { label: '全部班级', value: '' },
      ...classList.value.map(c => ({ label: c.className, value: c.id }))
    ]
  },
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
    key: 'status',
    label: '状态',
    value: queryParams.status,
    displayValue: statusOptions.find(s => s.value === queryParams.status)?.label,
    options: statusOptions
  }
])

// 操作菜单
const showSheet = ref(false)
const currentStudent = ref<Student | null>(null)
const sheetActions = computed(() => {
  const actions = []
  if (canEdit.value) {
    actions.push({ name: '编辑', value: 'edit' })
  }
  actions.push({ name: '查看详情', value: 'detail' })
  if (canDelete.value) {
    actions.push({ name: '删除', value: 'delete', color: '#EF4444' })
  }
  return actions
})

// 初始化
onMounted(() => {
  initSystemInfo()
  loadFilterOptions()
  loadData()
})

const initSystemInfo = () => {
  const sysInfo = uni.getSystemInfoSync()
  statusBarHeight.value = sysInfo.statusBarHeight || 20
  navBarHeight.value = statusBarHeight.value + 44

  // 计算列表高度
  const windowHeight = sysInfo.windowHeight
  const searchHeight = 60
  const filterHeight = 50
  listHeight.value = windowHeight - navBarHeight.value - searchHeight - filterHeight
}

const loadFilterOptions = async () => {
  try {
    const [classRes, deptRes] = await Promise.all([
      getAllClasses(),
      getEnabledDepartments()
    ])
    classList.value = classRes || []
    departmentList.value = deptRes || []
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
    const res = await getStudentPage({
      ...queryParams,
      keyword: searchKeyword.value
    })

    const newData = res.records || []
    if (isRefresh) {
      students.value = newData
    } else {
      students.value = [...students.value, ...newData]
    }

    // 更新加载状态
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

const onFilterChange = (filter: { key: string; value: any }) => {
  (queryParams as any)[filter.key] = filter.value || undefined
  loadData(true)
}

const onFilterClear = () => {
  queryParams.classId = undefined
  queryParams.departmentId = undefined
  queryParams.status = undefined
  loadData(true)
}

const goToDetail = (student: Student) => {
  uni.navigateTo({
    url: `/pages/student/detail?id=${student.id}`
  })
}

const goToEdit = (student: Student) => {
  uni.navigateTo({
    url: `/pages/student/edit?id=${student.id}`
  })
}

const goToAdd = () => {
  uni.navigateTo({
    url: '/pages/student/edit'
  })
}

const showActionSheet = (student: Student) => {
  currentStudent.value = student
  showSheet.value = true
}

const onSheetSelect = async (action: any) => {
  if (!currentStudent.value) return

  switch (action.value) {
    case 'edit':
      goToEdit(currentStudent.value)
      break
    case 'detail':
      goToDetail(currentStudent.value)
      break
    case 'delete':
      confirmDelete(currentStudent.value)
      break
  }
}

const confirmDelete = (student: Student) => {
  uni.showModal({
    title: '确认删除',
    content: `确定要删除学生"${student.realName}"吗？`,
    confirmColor: '#EF4444',
    success: async (res) => {
      if (res.confirm) {
        try {
          await deleteStudent(student.id)
          uni.showToast({ title: '删除成功', icon: 'success' })
          loadData(true)
        } catch (error: any) {
          uni.showToast({ title: error.message || '删除失败', icon: 'none' })
        }
      }
    }
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
    justify-content: space-between;
    height: 88rpx;
    padding: 0 24rpx;

    .nav-title {
      font-size: 36rpx;
      font-weight: 600;
      color: #fff;
    }

    .nav-actions {
      display: flex;
      gap: 24rpx;

      .action-btn {
        width: 64rpx;
        height: 64rpx;
        display: flex;
        align-items: center;
        justify-content: center;
        background: rgba(255, 255, 255, 0.2);
        border-radius: 50%;
      }
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
