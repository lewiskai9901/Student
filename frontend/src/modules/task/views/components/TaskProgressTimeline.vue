<template>
  <div class="task-progress-timeline">
    <el-timeline>
      <el-timeline-item
        v-for="node in progressNodes"
        :key="node.order"
        :timestamp="formatTimestamp(node.handledAt)"
        placement="top"
        :type="getNodeType(node.status)"
        :hollow="node.status === 'PENDING'"
        :icon="getNodeIcon(node)"
      >
        <el-card>
          <div class="node-header">
            <div class="node-title">
              <span class="node-type-badge" :class="'badge-' + node.nodeType.toLowerCase()">
                {{ getNodeTypeText(node.nodeType) }}
              </span>
              <span class="node-name">{{ node.nodeName }}</span>
              <el-tag
                :type="getStatusTagType(node.status)"
                size="small"
                class="status-tag"
              >
                {{ getStatusText(node.status) }}
              </el-tag>
            </div>
          </div>

          <!-- 单人处理节点 -->
          <div v-if="node.handlerName" class="node-handler">
            <el-icon><User /></el-icon>
            <span>{{ node.handlerName }}</span>
          </div>

          <!-- 多人处理节点（执行阶段） -->
          <div v-if="node.handlers && node.handlers.length > 0" class="multi-handler-node">
            <div class="progress-summary">
              <el-progress
                :percentage="node.progressPercent || 0"
                :status="node.status === 'COMPLETED' ? 'success' : undefined"
              >
                <span class="progress-text">
                  {{ node.completedCount }}/{{ node.totalCount }} 已提交
                </span>
              </el-progress>
            </div>

            <el-collapse accordion class="handler-list">
              <el-collapse-item title="查看执行人详情" name="1">
                <div class="handler-grid">
                  <div
                    v-for="handler in node.handlers"
                    :key="handler.id"
                    class="handler-item"
                  >
                    <div class="handler-info">
                      <el-avatar :size="32">{{ handler.assigneeName?.charAt(0) }}</el-avatar>
                      <div class="handler-details">
                        <div class="handler-name">{{ handler.assigneeName }}</div>
                        <div class="handler-status">
                          <el-tag
                            :type="getStatusTagType(mapAssigneeStatus(handler.status))"
                            size="small"
                          >
                            {{ handler.statusText }}
                          </el-tag>
                        </div>
                      </div>
                    </div>
                    <div v-if="handler.submittedAt" class="handler-time">
                      提交于: {{ formatTime(handler.submittedAt) }}
                    </div>
                  </div>
                </div>
              </el-collapse-item>
            </el-collapse>
          </div>

          <!-- 审批意见 -->
          <div v-if="node.comment" class="node-comment">
            <div class="comment-label">
              <el-icon><ChatLineRound /></el-icon>
              <span>审批意见：</span>
            </div>
            <div class="comment-content">{{ node.comment }}</div>
          </div>
        </el-card>
      </el-timeline-item>
    </el-timeline>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { User, ChatLineRound, CircleCheck, Clock, CircleClose, Finished } from '@element-plus/icons-vue'

interface TaskProgressNode {
  order: number
  nodeName: string
  nodeType: 'CREATE' | 'EXECUTE' | 'APPROVE' | 'END'
  status: 'COMPLETED' | 'PROCESSING' | 'PENDING' | 'REJECTED'
  handlerName?: string
  handlerId?: number
  handledAt?: string
  comment?: string
  handlers?: Array<{
    id: number
    assigneeName: string
    status: number
    statusText: string
    acceptedAt?: string
    submittedAt?: string
    completedAt?: string
  }>
  totalCount?: number
  completedCount?: number
  progressPercent?: number
}

const props = defineProps<{
  progressNodes: TaskProgressNode[]
}>()

const getNodeTypeText = (type: string): string => {
  const typeMap: Record<string, string> = {
    CREATE: '创建',
    EXECUTE: '执行',
    APPROVE: '审批',
    END: '完成'
  }
  return typeMap[type] || type
}

const getStatusText = (status: string): string => {
  const statusMap: Record<string, string> = {
    COMPLETED: '已完成',
    PROCESSING: '进行中',
    PENDING: '待处理',
    REJECTED: '已拒绝'
  }
  return statusMap[status] || status
}

const getStatusTagType = (status: string): string => {
  const typeMap: Record<string, string> = {
    COMPLETED: 'success',
    PROCESSING: 'warning',
    PENDING: 'info',
    REJECTED: 'danger'
  }
  return typeMap[status] || 'info'
}

const getNodeType = (status: string): string => {
  const typeMap: Record<string, string> = {
    COMPLETED: 'success',
    PROCESSING: 'primary',
    PENDING: 'info',
    REJECTED: 'danger'
  }
  return typeMap[status] || 'info'
}

const getNodeIcon = (node: TaskProgressNode) => {
  if (node.status === 'COMPLETED') return CircleCheck
  if (node.status === 'PROCESSING') return Clock
  if (node.status === 'REJECTED') return CircleClose
  if (node.nodeType === 'END') return Finished
  return Clock
}

const mapAssigneeStatus = (status: number): string => {
  const statusMap: Record<number, string> = {
    0: 'PENDING',
    1: 'PROCESSING',
    2: 'PROCESSING',
    3: 'COMPLETED',
    4: 'REJECTED'
  }
  return statusMap[status] || 'PENDING'
}

const formatTimestamp = (datetime?: string): string => {
  if (!datetime) return ''
  return new Date(datetime).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const formatTime = (datetime?: string): string => {
  if (!datetime) return '未提交'
  return new Date(datetime).toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}
</script>

<style scoped lang="scss">
.task-progress-timeline {
  padding: 20px;

  :deep(.el-timeline-item__timestamp) {
    color: #909399;
    font-size: 13px;
  }

  .node-header {
    margin-bottom: 12px;

    .node-title {
      display: flex;
      align-items: center;
      gap: 8px;
      flex-wrap: wrap;

      .node-type-badge {
        padding: 2px 8px;
        border-radius: 4px;
        font-size: 12px;
        font-weight: 500;

        &.badge-create {
          background: #ecf5ff;
          color: #409eff;
        }

        &.badge-execute {
          background: #f0f9ff;
          color: #67c23a;
        }

        &.badge-approve {
          background: #fef0f0;
          color: #f56c6c;
        }

        &.badge-end {
          background: #f4f4f5;
          color: #909399;
        }
      }

      .node-name {
        font-size: 15px;
        font-weight: 500;
        color: #303133;
      }

      .status-tag {
        margin-left: auto;
      }
    }
  }

  .node-handler {
    display: flex;
    align-items: center;
    gap: 6px;
    color: #606266;
    font-size: 14px;
    margin-top: 8px;

    .el-icon {
      color: #909399;
    }
  }

  .multi-handler-node {
    margin-top: 12px;

    .progress-summary {
      margin-bottom: 12px;

      .progress-text {
        font-size: 13px;
        color: #606266;
      }
    }

    .handler-list {
      margin-top: 12px;
      border: none;

      :deep(.el-collapse-item__header) {
        background: #f5f7fa;
        border: none;
        padding: 8px 12px;
        border-radius: 4px;
        font-size: 13px;
      }

      :deep(.el-collapse-item__wrap) {
        border: none;
      }

      :deep(.el-collapse-item__content) {
        padding: 12px 0 0 0;
      }
    }

    .handler-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
      gap: 12px;

      .handler-item {
        padding: 12px;
        background: #fafafa;
        border-radius: 6px;
        border: 1px solid #ebeef5;

        .handler-info {
          display: flex;
          align-items: center;
          gap: 10px;
          margin-bottom: 8px;

          .handler-details {
            flex: 1;

            .handler-name {
              font-size: 14px;
              font-weight: 500;
              color: #303133;
              margin-bottom: 4px;
            }

            .handler-status {
              font-size: 12px;
            }
          }
        }

        .handler-time {
          font-size: 12px;
          color: #909399;
          margin-top: 6px;
        }
      }
    }
  }

  .node-comment {
    margin-top: 12px;
    padding: 10px;
    background: #f9f9f9;
    border-left: 3px solid #409eff;
    border-radius: 4px;

    .comment-label {
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 13px;
      font-weight: 500;
      color: #606266;
      margin-bottom: 6px;

      .el-icon {
        color: #409eff;
      }
    }

    .comment-content {
      font-size: 14px;
      color: #303133;
      line-height: 1.6;
      white-space: pre-wrap;
    }
  }

  :deep(.el-card) {
    border: none;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  }

  :deep(.el-card__body) {
    padding: 16px;
  }
}
</style>
