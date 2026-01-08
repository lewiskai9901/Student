<template>
  <div class="checks-management-tab">
    <!-- 子Tab导航 -->
    <div class="sub-tab-nav">
      <button
        v-for="tab in subTabs"
        :key="tab.key"
        class="sub-tab-btn"
        :class="{ active: activeSubTab === tab.key }"
        @click="activeSubTab = tab.key"
      >
        {{ tab.label }}
        <span v-if="tab.count !== undefined" class="tab-count">{{ tab.count }}</span>
      </button>
    </div>

    <!-- 日常检查 -->
    <div v-if="activeSubTab === 'dailyChecks'" class="sub-tab-content">
      <div class="section-header">
        <h3>日常检查列表</h3>
        <button v-if="planStatus === 1" class="btn btn-primary btn-sm" @click="$emit('createDailyCheck')">
          <Plus :size="14" />
          新建检查
        </button>
      </div>

      <div v-if="loadingDailyChecks" class="loading-inline">
        <Loader2 class="spinner-sm" />
        <span>加载中...</span>
      </div>

      <div v-else-if="dailyChecks.length === 0" class="empty-inline">
        <ClipboardList :size="40" class="empty-icon" />
        <p>暂无日常检查</p>
        <span v-if="planStatus === 0" class="empty-hint">请先开始计划后再创建日常检查</span>
        <span v-else-if="planStatus === 1" class="empty-hint">点击上方按钮创建第一个日常检查</span>
      </div>

      <div v-else class="data-table">
        <table>
          <thead>
            <tr>
              <th>检查日期</th>
              <th>检查名称</th>
              <th>轮次</th>
              <th>状态</th>
              <th>创建时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="check in dailyChecks" :key="check.id">
              <td>{{ check.checkDate }}</td>
              <td>{{ check.checkName }}</td>
              <td>
                <span class="round-info">{{ check.totalRounds }}轮</span>
              </td>
              <td>
                <span class="status-badge sm" :class="getDailyCheckStatusClass(check.status)">
                  {{ getDailyCheckStatusText(check.status) }}
                </span>
              </td>
              <td>{{ formatDateTime(check.createdAt) }}</td>
              <td>
                <div class="action-btn-group">
                  <button class="action-btn" @click="$emit('viewDailyCheck', check)" title="查看">
                    <Eye :size="14" />
                  </button>
                  <button v-if="check.status === 0" class="action-btn start" @click="$emit('startDailyCheck', check)" title="开始检查">
                    <Play :size="14" />
                  </button>
                  <button v-if="check.status === 1" class="action-btn scoring" @click="$emit('scoring', check)" title="打分">
                    <Star :size="14" />
                  </button>
                  <button v-if="check.status === 1" class="action-btn finish" @click="$emit('finishDailyCheck', check)" title="结束检查">
                    <CheckCircle :size="14" />
                  </button>
                  <button v-if="check.status !== 3" class="action-btn delete" @click="$emit('deleteDailyCheck', check)" title="删除">
                    <Trash2 :size="14" />
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- 检查记录 -->
    <div v-if="activeSubTab === 'records'" class="sub-tab-content">
      <div class="section-header">
        <h3>检查记录列表</h3>
        <div class="section-stats" v-if="checkRecords.length > 0">
          <span class="stat-tag">共 {{ checkRecords.length }} 条</span>
        </div>
      </div>

      <div v-if="loadingRecords" class="loading-inline">
        <Loader2 class="spinner-sm" />
        <span>加载中...</span>
      </div>

      <div v-else-if="checkRecords.length === 0" class="empty-inline">
        <FileCheck :size="40" class="empty-icon" />
        <p>暂无检查记录</p>
        <span class="empty-hint">完成日常检查后会自动生成检查记录</span>
      </div>

      <div v-else class="data-table">
        <table>
          <thead>
            <tr>
              <th>记录编号</th>
              <th>检查名称</th>
              <th>检查日期</th>
              <th>扣分条数</th>
              <th>总扣分</th>
              <th>状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="record in checkRecords" :key="record.id">
              <td class="code">{{ record.recordCode }}</td>
              <td>{{ record.checkName }}</td>
              <td>{{ record.checkDate }}</td>
              <td>{{ record.totalDeductionCount }}</td>
              <td class="deduct">{{ record.totalDeductionScore }}</td>
              <td>
                <span class="status-badge sm" :class="record.status === 1 ? 'success' : 'default'">
                  {{ record.status === 1 ? '已发布' : '已归档' }}
                </span>
              </td>
              <td>
                <div class="action-btn-group">
                  <button class="action-btn" @click="$emit('viewRecord', record)" title="查看详情">
                    <Eye :size="14" />
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- 打分人员 -->
    <div v-if="activeSubTab === 'inspectors'" class="sub-tab-content">
      <div class="section-header">
        <h3>打分人员管理</h3>
        <button class="btn btn-primary btn-sm" @click="$emit('addInspector')">
          <Plus :size="14" />
          添加人员
        </button>
      </div>

      <div v-if="loadingInspectors" class="loading-inline">
        <Loader2 class="spinner-sm" />
        <span>加载中...</span>
      </div>

      <div v-else-if="inspectors.length === 0" class="empty-inline">
        <Users :size="40" class="empty-icon" />
        <p>暂无打分人员</p>
        <span class="empty-hint">添加打分人员后，他们可以对日常检查进行打分</span>
      </div>

      <div v-else class="inspector-grid">
        <div v-for="inspector in inspectors" :key="inspector.id" class="inspector-card">
          <div class="inspector-avatar">
            <User :size="24" />
          </div>
          <div class="inspector-info">
            <div class="inspector-name">{{ inspector.userName }}</div>
            <div class="inspector-role">{{ inspector.roleName || '检查员' }}</div>
          </div>
          <button class="btn-icon remove" @click="$emit('removeInspector', inspector)" title="移除">
            <X :size="14" />
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { Plus, Loader2, ClipboardList, Eye, Star, Trash2, FileCheck, Users, User, X, Play, CheckCircle } from 'lucide-vue-next'

const props = defineProps<{
  planStatus: number
  dailyChecks: any[]
  checkRecords: any[]
  inspectors: any[]
  loadingDailyChecks: boolean
  loadingRecords: boolean
  loadingInspectors: boolean
}>()

defineEmits([
  'createDailyCheck',
  'viewDailyCheck',
  'startDailyCheck',
  'finishDailyCheck',
  'scoring',
  'deleteDailyCheck',
  'viewRecord',
  'addInspector',
  'removeInspector'
])

const activeSubTab = ref('dailyChecks')

const subTabs = computed(() => [
  { key: 'dailyChecks', label: '日常检查', count: props.dailyChecks.length },
  { key: 'records', label: '检查记录', count: props.checkRecords.length },
  { key: 'inspectors', label: '打分人员', count: props.inspectors.length }
])

function getDailyCheckStatusClass(status: number) {
  const classes: Record<number, string> = {
    0: 'default',
    1: 'progress',
    2: 'warning',
    3: 'success'
  }
  return classes[status] || 'default'
}

function getDailyCheckStatusText(status: number) {
  const texts: Record<number, string> = {
    0: '未开始',
    1: '进行中',
    2: '已提交',
    3: '已发布'
  }
  return texts[status] || '未知'
}

function formatDateTime(dateStr?: string) {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN')
}
</script>

<style scoped lang="scss">
.checks-management-tab {
  padding: 0;
}

.sub-tab-nav {
  display: flex;
  gap: 4px;
  padding: 4px;
  background: #f3f4f6;
  border-radius: 8px;
  margin-bottom: 20px;
}

.sub-tab-btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 10px 16px;
  border: none;
  background: transparent;
  color: #6b7280;
  font-size: 14px;
  font-weight: 500;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    color: #374151;
  }

  &.active {
    background: #fff;
    color: #1f2937;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  }

  .tab-count {
    padding: 2px 8px;
    background: #e5e7eb;
    border-radius: 10px;
    font-size: 12px;
  }

  &.active .tab-count {
    background: #3b82f6;
    color: #fff;
  }
}

.sub-tab-content {
  min-height: 300px;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;

  h3 {
    font-size: 16px;
    font-weight: 600;
    color: #1f2937;
    margin: 0;
  }
}

.section-stats {
  display: flex;
  gap: 8px;

  .stat-tag {
    padding: 4px 10px;
    background: #f3f4f6;
    border-radius: 4px;
    font-size: 13px;
    color: #6b7280;

    &.deduct {
      background: #fef2f2;
      color: #dc2626;
    }
  }
}

.loading-inline,
.empty-inline {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  color: #9ca3af;

  .spinner-sm {
    width: 24px;
    height: 24px;
    animation: spin 1s linear infinite;
  }

  .empty-icon {
    color: #d1d5db;
    margin-bottom: 12px;
  }

  p {
    margin: 0 0 4px 0;
    color: #6b7280;
  }

  .empty-hint {
    font-size: 13px;
  }
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.data-table {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  overflow: hidden;

  table {
    width: 100%;
    border-collapse: collapse;

    th, td {
      padding: 12px 16px;
      text-align: left;
      border-bottom: 1px solid #f3f4f6;
    }

    th {
      background: #f9fafb;
      font-size: 13px;
      font-weight: 600;
      color: #6b7280;
    }

    td {
      font-size: 14px;
      color: #374151;

      &.code {
        font-family: monospace;
        font-size: 13px;
        color: #6b7280;
      }

      &.deduct {
        color: #dc2626;
        font-weight: 500;
      }
    }

    tbody tr:hover {
      background: #f9fafb;
    }

    tbody tr:last-child td {
      border-bottom: none;
    }
  }
}

.round-info {
  padding: 2px 8px;
  background: #eff6ff;
  color: #3b82f6;
  border-radius: 4px;
  font-size: 12px;
}

.status-badge {
  display: inline-block;
  padding: 4px 10px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;

  &.sm {
    padding: 2px 8px;
  }

  &.default {
    background: #f3f4f6;
    color: #6b7280;
  }

  &.progress {
    background: #fef3c7;
    color: #d97706;
  }

  &.warning {
    background: #fef9c3;
    color: #ca8a04;
  }

  &.success {
    background: #dcfce7;
    color: #16a34a;
  }
}

.action-btn-group {
  display: flex;
  gap: 4px;
}

.action-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border: none;
  border-radius: 4px;
  background: transparent;
  color: #6b7280;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    background: #f3f4f6;
    color: #374151;
  }

  &.start:hover {
    background: #dcfce7;
    color: #16a34a;
  }

  &.scoring:hover {
    background: #fef3c7;
    color: #d97706;
  }

  &.finish:hover {
    background: #dbeafe;
    color: #2563eb;
  }

  &.delete:hover {
    background: #fef2f2;
    color: #dc2626;
  }
}

.inspector-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 12px;
}

.inspector-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  transition: all 0.2s;

  &:hover {
    border-color: #3b82f6;
    box-shadow: 0 2px 8px rgba(59, 130, 246, 0.1);
  }

  .inspector-avatar {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 40px;
    height: 40px;
    background: #eff6ff;
    color: #3b82f6;
    border-radius: 50%;
  }

  .inspector-info {
    flex: 1;

    .inspector-name {
      font-size: 14px;
      font-weight: 500;
      color: #1f2937;
    }

    .inspector-role {
      font-size: 12px;
      color: #9ca3af;
    }
  }

  .btn-icon {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 24px;
    height: 24px;
    border: none;
    border-radius: 4px;
    background: transparent;
    color: #9ca3af;
    cursor: pointer;
    opacity: 0;
    transition: all 0.2s;

    &:hover {
      background: #fef2f2;
      color: #dc2626;
    }
  }

  &:hover .btn-icon {
    opacity: 1;
  }
}

.btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;

  &.btn-primary {
    background: #3b82f6;
    color: #fff;

    &:hover {
      background: #2563eb;
    }
  }

  &.btn-sm {
    padding: 6px 12px;
    font-size: 13px;
  }
}
</style>
