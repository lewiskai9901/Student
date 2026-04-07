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
      <OverviewTab 
        v-if="activeTab === 'overview'" 
        :class-id="currentClassId" 
        @change-tab="changeTab"
      />
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
import { getMyClasses } from '@/api/myClass'
import type { MyClassItem } from '@/types/myClass'
import OverviewTab from './tabs/OverviewTab.vue'
import StudentsTab from './tabs/StudentsTab.vue'
import DormitoryTab from './tabs/DormitoryTab.vue'
import AnalyticsTab from './tabs/AnalyticsTab.vue'

const route = useRoute()
const router = useRouter()

const activeTab = ref('overview')
const allClasses = ref<MyClassItem[]>([])
const currentClassId = ref<string | number>('')

const currentClass = computed(() =>
  allClasses.value.find(c => String(c.id) === String(currentClassId.value))
)

const fetchClasses = async () => {
  allClasses.value = await getMyClasses()

  // 从路由参数获取orgUnitId (保持字符串，避免大整数精度丢失)
  const orgUnitId = route.params.orgUnitId as string
  if (orgUnitId && allClasses.value.some(c => String(c.id) === orgUnitId)) {
    currentClassId.value = orgUnitId
  } else if (allClasses.value.length > 0) {
    currentClassId.value = allClasses.value[0].id
    router.replace(`/my-class/${currentClassId.value}`)
  }
}

const onClassChange = (orgUnitId: string | number) => {
  router.push(`/my-class/${orgUnitId}`)
}

const goBack = () => {
  if (allClasses.value.length > 1) {
    router.push('/my-class/list')
  } else {
    router.push('/dashboard')
  }
}

const changeTab = (tab: string) => {
  activeTab.value = tab
}

watch(() => route.params.orgUnitId, (newId) => {
  if (newId) {
    currentClassId.value = newId as string
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
