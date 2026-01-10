<template>
  <div class="seat-grid-container">
    <!-- 统计条 -->
    <div class="stats-bar">
      <span class="stat-item total">共 <b>{{ totalCount }}</b> 人</span>
      <span class="stat-item completed">已完成 <b>{{ completedCount }}</b></span>
      <span class="stat-item pending">进行中 <b>{{ pendingCount }}</b></span>
      <span class="stat-item waiting">待接收 <b>{{ waitingCount }}</b></span>
    </div>

    <!-- 按部门分组的紧凑座位 -->
    <div class="departments-wrapper">
      <div
        v-for="dept in groupedByDepartment"
        :key="dept.departmentId"
        class="department-row"
      >
        <span class="department-label">{{ dept.departmentName }}</span>
        <div class="seats-row">
          <button
            v-for="assignee in dept.assignees"
            :key="assignee.id"
            class="seat-tag"
            :class="[getSeatClass(assignee.status), { 'selected': selectedId === assignee.id }]"
            @click="selectAssignee(assignee)"
          >
            {{ assignee.assigneeName }}
          </button>
        </div>
      </div>
    </div>

    <!-- 图例 -->
    <div class="legend">
      <span class="legend-item"><i class="dot waiting"></i>待接收</span>
      <span class="legend-item"><i class="dot progress"></i>进行中</span>
      <span class="legend-item"><i class="dot review"></i>待审核</span>
      <span class="legend-item"><i class="dot completed"></i>已完成</span>
      <span class="legend-item"><i class="dot rejected"></i>已打回</span>
    </div>

    <!-- 选中执行人的详细流程 -->
    <Transition name="detail-slide">
      <div v-if="selectedAssignee" class="detail-panel">
        <!-- 头部信息 -->
        <div class="detail-header">
          <div class="assignee-info">
            <span class="assignee-avatar" :class="getAvatarClass(selectedAssignee.status)">
              {{ selectedAssignee.assigneeName.charAt(0) }}
            </span>
            <div class="assignee-meta">
              <span class="assignee-name">{{ selectedAssignee.assigneeName }}</span>
              <span class="assignee-dept">{{ selectedAssignee.departmentName || '未知部门' }}</span>
            </div>
            <span class="status-tag" :class="getStatusTagClass(selectedAssignee.status)">
              {{ selectedAssignee.statusText }}
            </span>
          </div>
          <button class="close-btn" @click="selectedId = null">
            <XMarkIcon class="w-4 h-4" />
          </button>
        </div>

        <!-- 详细内容区域 -->
        <div class="detail-body">
          <!-- 左侧：流程进度 -->
          <div class="flow-section">
            <h5 class="section-title">
              <ClipboardDocumentListIcon class="w-4 h-4" />
              流程进度
            </h5>
            <div class="flow-steps">
              <div class="flow-step" :class="{ 'completed': selectedAssignee.status >= 1, 'active': selectedAssignee.status === 0 }">
                <div class="step-dot"></div>
                <div class="step-info">
                  <span class="step-name">接收任务</span>
                  <span class="step-time">{{ selectedAssignee.acceptedAt ? formatDate(selectedAssignee.acceptedAt) : '待接收' }}</span>
                </div>
              </div>
              <div class="flow-step" :class="{ 'completed': selectedAssignee.status >= 2, 'active': selectedAssignee.status === 1 }">
                <div class="step-dot"></div>
                <div class="step-info">
                  <span class="step-name">提交成果</span>
                  <span class="step-time">{{ selectedAssignee.submittedAt ? formatDate(selectedAssignee.submittedAt) : '待提交' }}</span>
                </div>
              </div>
              <div class="flow-step" :class="{ 'completed': selectedAssignee.status === 3, 'active': selectedAssignee.status === 2 || selectedAssignee.status === 6 }">
                <div class="step-dot"></div>
                <div class="step-info">
                  <span class="step-name">审批流程</span>
                  <span v-if="selectedAssignee.currentApprovalLevel" class="step-level">第{{ selectedAssignee.currentApprovalLevel }}级</span>
                  <span v-else class="step-time">{{ selectedAssignee.status >= 2 ? '审批中' : '待审批' }}</span>
                </div>
              </div>
              <div class="flow-step" :class="{ 'completed': selectedAssignee.status === 3 }">
                <div class="step-dot"></div>
                <div class="step-info">
                  <span class="step-name">完成</span>
                  <span class="step-time">{{ selectedAssignee.completedAt ? formatDate(selectedAssignee.completedAt) : '待完成' }}</span>
                </div>
              </div>
            </div>
          </div>

          <!-- 右侧：提交和审批详情 -->
          <div class="details-section">
            <!-- 提交详情 -->
            <div class="detail-card">
              <h5 class="card-title">
                <DocumentTextIcon class="w-4 h-4" />
                提交详情
              </h5>
              <div v-if="selectedAssignee.submission" class="card-content">
                <div class="info-row">
                  <span class="info-label">提交人</span>
                  <span class="info-value">{{ selectedAssignee.submission.submitterName || selectedAssignee.assigneeName }}</span>
                </div>
                <div class="info-row">
                  <span class="info-label">提交时间</span>
                  <span class="info-value">{{ formatDate(selectedAssignee.submission.submittedAt) }}</span>
                </div>
                <div class="info-row">
                  <span class="info-label">审核状态</span>
                  <span class="info-value">
                    <span class="review-badge" :class="getReviewBadgeClass(selectedAssignee.submission.reviewStatus)">
                      {{ selectedAssignee.submission.reviewStatusText || '待审核' }}
                    </span>
                  </span>
                </div>
                <div class="content-row">
                  <span class="info-label">提交内容</span>
                  <p class="content-text">{{ selectedAssignee.submission.content || '无提交内容' }}</p>
                </div>
                <!-- 附件列表 -->
                <div v-if="selectedAssignee.submission.attachmentUrls?.length" class="attachments-row">
                  <span class="info-label">附件 ({{ selectedAssignee.submission.attachmentUrls.length }}个)</span>
                  <div class="attachments-list">
                    <a
                      v-for="(url, idx) in selectedAssignee.submission.attachmentUrls"
                      :key="idx"
                      :href="url"
                      target="_blank"
                      class="attachment-item"
                    >
                      <PaperClipIcon class="w-4 h-4" />
                      <span>附件{{ idx + 1 }}</span>
                      <ArrowTopRightOnSquareIcon class="w-3 h-3 ml-auto" />
                    </a>
                  </div>
                </div>
              </div>
              <div v-else class="card-empty">
                <InboxIcon class="w-8 h-8" />
                <span>暂未提交</span>
              </div>
            </div>

            <!-- 审批详情 -->
            <div class="detail-card">
              <h5 class="card-title">
                <ClipboardDocumentCheckIcon class="w-4 h-4" />
                审批详情
                <span v-if="selectedAssignee.approvalRecords?.length" class="record-count">
                  {{ selectedAssignee.approvalRecords.length }}条记录
                </span>
              </h5>
              <div v-if="selectedAssignee.approvalRecords?.length" class="card-content">
                <div
                  v-for="(record, index) in selectedAssignee.approvalRecords"
                  :key="record.id"
                  class="approval-record"
                  :class="getApprovalRecordClass(record.approvalStatus)"
                >
                  <div class="record-header">
                    <span class="record-index">{{ index + 1 }}</span>
                    <div class="record-info">
                      <div class="record-top">
                        <span class="approver-name">{{ record.approverName }}</span>
                        <span v-if="record.approverRole" class="approver-role">{{ record.approverRole }}</span>
                        <span class="node-name">{{ record.nodeName }}</span>
                      </div>
                      <div class="record-bottom">
                        <span class="approval-result" :class="getApprovalResultClass(record.approvalStatus)">
                          {{ record.approvalStatusText }}
                        </span>
                        <span v-if="record.approvalTime" class="approval-time">
                          {{ formatDate(record.approvalTime) }}
                        </span>
                      </div>
                    </div>
                  </div>
                  <!-- 审批意见 -->
                  <div v-if="record.approvalComment" class="record-comment">
                    <span class="comment-label">审批意见：</span>
                    <span class="comment-text">"{{ record.approvalComment }}"</span>
                  </div>
                  <!-- 打回原因 -->
                  <div v-if="record.rejectReason" class="record-reject">
                    <span class="reject-label">打回原因：</span>
                    <span class="reject-text">{{ record.rejectReason }}</span>
                  </div>
                </div>
              </div>
              <div v-else class="card-empty">
                <ClockIcon class="w-8 h-8" />
                <span>{{ selectedAssignee.status >= 2 ? '等待审批中...' : '暂无审批记录' }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import {
  XMarkIcon,
  CheckIcon,
  PaperClipIcon,
  DocumentTextIcon,
  ClipboardDocumentCheckIcon,
  ClipboardDocumentListIcon,
  InboxIcon,
  ClockIcon,
  ArrowTopRightOnSquareIcon
} from '@heroicons/vue/24/outline'
import type { TaskAssigneeDTO } from '@/api/v2/task'

const props = defineProps<{
  assignees: TaskAssigneeDTO[]
}>()

const selectedId = ref<string | null>(null)

const selectedAssignee = computed(() => {
  if (!selectedId.value) return null
  return props.assignees.find(a => a.id === selectedId.value) || null
})

const selectAssignee = (assignee: TaskAssigneeDTO) => {
  selectedId.value = selectedId.value === assignee.id ? null : assignee.id
}

// 按部门分组
const groupedByDepartment = computed(() => {
  const groups: Record<string, { departmentId: string; departmentName: string; assignees: TaskAssigneeDTO[] }> = {}

  props.assignees.forEach(assignee => {
    const deptId = assignee.departmentId || 'unknown'
    const deptName = assignee.departmentName || '未分配部门'

    if (!groups[deptId]) {
      groups[deptId] = { departmentId: deptId, departmentName: deptName, assignees: [] }
    }
    groups[deptId].assignees.push(assignee)
  })

  return Object.values(groups).sort((a, b) => a.departmentName.localeCompare(b.departmentName))
})

// 统计
const totalCount = computed(() => props.assignees.length)
const completedCount = computed(() => props.assignees.filter(a => a.status === 3).length)
const pendingCount = computed(() => props.assignees.filter(a => a.status === 1 || a.status === 2 || a.status === 6).length)
const waitingCount = computed(() => props.assignees.filter(a => a.status === 0).length)

const formatDate = (dateStr?: string) => {
  if (!dateStr) return ''
  return dateStr.replace('T', ' ').substring(0, 16)
}

// 座位样式
const getSeatClass = (status: number) => {
  const classes: Record<number, string> = {
    0: 'seat-waiting',
    1: 'seat-progress',
    2: 'seat-review',
    3: 'seat-completed',
    4: 'seat-rejected',
    5: 'seat-cancelled',
    6: 'seat-approving'
  }
  return classes[status] || 'seat-default'
}

const getAvatarClass = (status: number) => {
  const classes: Record<number, string> = {
    0: 'avatar-waiting',
    1: 'avatar-progress',
    2: 'avatar-review',
    3: 'avatar-completed',
    4: 'avatar-rejected',
    6: 'avatar-approving'
  }
  return classes[status] || ''
}

const getStatusTagClass = (status: number) => {
  const classes: Record<number, string> = {
    0: 'tag-waiting',
    1: 'tag-progress',
    2: 'tag-review',
    3: 'tag-completed',
    4: 'tag-rejected',
    6: 'tag-approving'
  }
  return classes[status] || ''
}

const getReviewBadgeClass = (status?: number) => {
  const classes: Record<number, string> = {
    0: 'review-pending',
    1: 'review-reviewing',
    2: 'review-passed',
    3: 'review-rejected'
  }
  return classes[status ?? 0] || 'review-pending'
}

const getApprovalRecordClass = (status: number) => {
  const classes: Record<number, string> = {
    0: 'record-pending',
    1: 'record-passed',
    2: 'record-rejected'
  }
  return classes[status] || 'record-pending'
}

const getApprovalResultClass = (status: number) => {
  const classes: Record<number, string> = {
    0: 'result-pending',
    1: 'result-passed',
    2: 'result-rejected'
  }
  return classes[status] || 'result-pending'
}
</script>

<style scoped>
.seat-grid-container {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

/* 统计条 */
.stats-bar {
  display: flex;
  align-items: center;
  gap: 16px;
  font-size: 13px;
  color: #64748b;
}

.stat-item b {
  font-weight: 600;
  margin-left: 2px;
}

.stat-item.total b { color: #1e293b; }
.stat-item.completed b { color: #10b981; }
.stat-item.pending b { color: #3b82f6; }
.stat-item.waiting b { color: #f97316; }

/* 部门行 */
.departments-wrapper {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.department-row {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.department-label {
  flex-shrink: 0;
  width: 80px;
  font-size: 12px;
  font-weight: 500;
  color: #64748b;
  padding-top: 4px;
  text-align: right;
}

.seats-row {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  flex: 1;
}

/* 紧凑座位标签 */
.seat-tag {
  padding: 4px 10px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
  border: 1px solid transparent;
  cursor: pointer;
  transition: all 0.15s ease;
  outline: none;
}

.seat-tag:hover {
  transform: translateY(-1px);
  box-shadow: 0 2px 6px rgba(0,0,0,0.1);
}

.seat-tag.selected {
  box-shadow: 0 0 0 2px #3b82f6;
}

.seat-waiting { background: #ffedd5; color: #c2410c; border-color: #fdba74; }
.seat-progress { background: #dbeafe; color: #1d4ed8; border-color: #93c5fd; }
.seat-review { background: #fef3c7; color: #b45309; border-color: #fcd34d; }
.seat-completed { background: #d1fae5; color: #047857; border-color: #6ee7b7; }
.seat-rejected { background: #fee2e2; color: #b91c1c; border-color: #fca5a5; }
.seat-approving { background: #f3e8ff; color: #7c3aed; border-color: #c4b5fd; }

/* 图例 */
.legend {
  display: flex;
  gap: 14px;
  font-size: 11px;
  color: #64748b;
  padding-top: 4px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.legend .dot {
  width: 8px;
  height: 8px;
  border-radius: 2px;
}

.dot.waiting { background: #fb923c; }
.dot.progress { background: #3b82f6; }
.dot.review { background: #f59e0b; }
.dot.completed { background: #10b981; }
.dot.rejected { background: #ef4444; }

/* 详情面板 */
.detail-panel {
  margin-top: 16px;
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.detail-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  border-bottom: 1px solid #e2e8f0;
}

.assignee-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.assignee-avatar {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-weight: 700;
  color: white;
}

.avatar-waiting { background: linear-gradient(135deg, #fb923c, #f97316); }
.avatar-progress { background: linear-gradient(135deg, #60a5fa, #3b82f6); }
.avatar-review { background: linear-gradient(135deg, #fbbf24, #f59e0b); }
.avatar-completed { background: linear-gradient(135deg, #34d399, #10b981); }
.avatar-rejected { background: linear-gradient(135deg, #f87171, #ef4444); }
.avatar-approving { background: linear-gradient(135deg, #a78bfa, #8b5cf6); }

.assignee-meta {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.assignee-name {
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
}

.assignee-dept {
  font-size: 12px;
  color: #64748b;
}

.status-tag {
  font-size: 12px;
  padding: 4px 12px;
  border-radius: 12px;
  font-weight: 500;
}

.tag-waiting { background: #ffedd5; color: #c2410c; }
.tag-progress { background: #dbeafe; color: #1d4ed8; }
.tag-review { background: #fef3c7; color: #b45309; }
.tag-completed { background: #d1fae5; color: #047857; }
.tag-rejected { background: #fee2e2; color: #b91c1c; }
.tag-approving { background: #f3e8ff; color: #7c3aed; }

.close-btn {
  width: 32px;
  height: 32px;
  border: none;
  background: white;
  border-radius: 8px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #64748b;
  transition: all 0.15s;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.close-btn:hover {
  background: #f1f5f9;
  color: #1e293b;
}

/* 详情内容区域 */
.detail-body {
  display: grid;
  grid-template-columns: 200px 1fr;
  gap: 0;
}

/* 左侧流程进度 */
.flow-section {
  padding: 20px;
  background: #f8fafc;
  border-right: 1px solid #e2e8f0;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 600;
  color: #374151;
  margin: 0 0 16px 0;
}

.flow-steps {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.flow-step {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 10px 0;
  position: relative;
}

.flow-step::before {
  content: '';
  position: absolute;
  left: 5px;
  top: 24px;
  width: 2px;
  height: calc(100% - 14px);
  background: #e2e8f0;
}

.flow-step:last-child::before {
  display: none;
}

.flow-step.completed::before {
  background: #10b981;
}

.step-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: #e2e8f0;
  flex-shrink: 0;
  margin-top: 2px;
  position: relative;
  z-index: 1;
}

.flow-step.active .step-dot {
  background: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.2);
}

.flow-step.completed .step-dot {
  background: #10b981;
}

.step-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.step-name {
  font-size: 12px;
  font-weight: 500;
  color: #374151;
}

.step-time {
  font-size: 11px;
  color: #9ca3af;
}

.step-level {
  font-size: 10px;
  padding: 1px 6px;
  background: #f3e8ff;
  color: #7c3aed;
  border-radius: 6px;
}

/* 右侧详情区域 */
.details-section {
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  max-height: 400px;
  overflow-y: auto;
}

.detail-card {
  background: #f9fafb;
  border: 1px solid #f3f4f6;
  border-radius: 10px;
  overflow: hidden;
}

.card-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 600;
  color: #374151;
  margin: 0;
  padding: 12px 14px;
  background: white;
  border-bottom: 1px solid #f3f4f6;
}

.record-count {
  margin-left: auto;
  font-size: 11px;
  font-weight: 400;
  color: #9ca3af;
}

.card-content {
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.card-empty {
  padding: 30px 14px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  color: #9ca3af;
  font-size: 12px;
}

/* 信息行 */
.info-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.info-label {
  font-size: 12px;
  color: #6b7280;
  flex-shrink: 0;
  min-width: 60px;
}

.info-value {
  font-size: 12px;
  color: #1e293b;
  font-weight: 500;
}

.content-row {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.content-text {
  font-size: 13px;
  color: #374151;
  line-height: 1.6;
  margin: 0;
  padding: 10px 12px;
  background: white;
  border-radius: 6px;
  border: 1px solid #e5e7eb;
}

/* 审核状态徽章 */
.review-badge {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 8px;
  font-weight: 500;
}

.review-pending { background: #fef3c7; color: #b45309; }
.review-reviewing { background: #dbeafe; color: #1d4ed8; }
.review-passed { background: #d1fae5; color: #047857; }
.review-rejected { background: #fee2e2; color: #b91c1c; }

/* 附件列表 */
.attachments-row {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.attachments-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.attachment-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  text-decoration: none;
  color: #3b82f6;
  font-size: 12px;
  transition: all 0.15s;
}

.attachment-item:hover {
  background: #eff6ff;
  border-color: #93c5fd;
}

/* 审批记录 */
.approval-record {
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 12px;
  border-left: 3px solid #e5e7eb;
}

.approval-record.record-passed {
  border-left-color: #10b981;
}

.approval-record.record-rejected {
  border-left-color: #ef4444;
}

.approval-record.record-pending {
  border-left-color: #f59e0b;
}

.record-header {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.record-index {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: #f1f5f9;
  color: #64748b;
  font-size: 11px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.record-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.record-top {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.approver-name {
  font-size: 13px;
  font-weight: 600;
  color: #1e293b;
}

.approver-role {
  font-size: 11px;
  color: #6b7280;
  padding: 1px 6px;
  background: #f1f5f9;
  border-radius: 4px;
}

.node-name {
  font-size: 11px;
  color: #9ca3af;
}

.record-bottom {
  display: flex;
  align-items: center;
  gap: 10px;
}

.approval-result {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 8px;
  font-weight: 500;
}

.result-pending { background: #fef3c7; color: #b45309; }
.result-passed { background: #d1fae5; color: #047857; }
.result-rejected { background: #fee2e2; color: #b91c1c; }

.approval-time {
  font-size: 11px;
  color: #9ca3af;
}

.record-comment {
  margin-top: 10px;
  padding: 10px 12px;
  background: #f8fafc;
  border-radius: 6px;
  border-left: 2px solid #cbd5e1;
}

.comment-label {
  font-size: 11px;
  color: #6b7280;
  display: block;
  margin-bottom: 4px;
}

.comment-text {
  font-size: 13px;
  color: #374151;
  font-style: italic;
}

.record-reject {
  margin-top: 10px;
  padding: 10px 12px;
  background: #fef2f2;
  border-radius: 6px;
  border-left: 2px solid #fca5a5;
}

.reject-label {
  font-size: 11px;
  color: #b91c1c;
  display: block;
  margin-bottom: 4px;
}

.reject-text {
  font-size: 13px;
  color: #dc2626;
}

/* 展开动画 */
.detail-slide-enter-active,
.detail-slide-leave-active {
  transition: all 0.3s ease;
}

.detail-slide-enter-from,
.detail-slide-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

@media (max-width: 768px) {
  .detail-body {
    grid-template-columns: 1fr;
  }

  .flow-section {
    border-right: none;
    border-bottom: 1px solid #e2e8f0;
  }

  .flow-steps {
    flex-direction: row;
    gap: 0;
    justify-content: space-between;
  }

  .flow-step {
    flex-direction: column;
    align-items: center;
    text-align: center;
    padding: 0;
  }

  .flow-step::before {
    left: 50%;
    top: 6px;
    width: calc(100% - 12px);
    height: 2px;
    transform: translateX(6px);
  }

  .department-row {
    flex-direction: column;
    gap: 4px;
  }

  .department-label {
    width: auto;
    text-align: left;
  }

  .stats-bar {
    flex-wrap: wrap;
    gap: 10px;
  }
}
</style>
