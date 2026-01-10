<template>
  <div class="notification-public-view">
    <!-- 加载中 -->
    <div v-if="loading" class="loading-container">
      <div class="loading-spinner"></div>
      <p>加载中...</p>
    </div>

    <!-- 错误状态 -->
    <div v-else-if="error" class="error-container">
      <div class="error-icon">!</div>
      <h2>无法加载通报</h2>
      <p>{{ error }}</p>
      <button class="btn-retry" @click="loadNotification">重试</button>
    </div>

    <!-- 通报内容 -->
    <div v-else class="notification-container">
      <!-- 头部 -->
      <header class="notification-header">
        <h1 class="notification-title">{{ record?.title || '量化检查通报' }}</h1>
        <div class="notification-meta">
          <span v-if="record?.createdAt">发布时间：{{ formatDate(record.createdAt) }}</span>
        </div>
      </header>

      <!-- 内容区域 -->
      <article class="notification-content" v-html="contentHtml"></article>

      <!-- 底部信息 -->
      <footer class="notification-footer">
        <div class="stats-info" v-if="record">
          <span>涉及 {{ record.totalCount }} 人</span>
          <span>{{ record.totalClasses }} 个班级</span>
        </div>
        <div class="share-tip">
          <p>长按识别二维码或点击右上角分享给好友</p>
        </div>
      </footer>
    </div>

    <!-- 微信分享提示（仅在微信中显示） -->
    <div v-if="isWechat && !loading && !error" class="wechat-share-tip">
      点击右上角 ··· 分享给好友
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getNotificationPublic } from '@/api/v2/notification'
import { initWechatShare, isWechatBrowser } from '@/api/v2/wechat'
import DOMPurify from 'dompurify'

const route = useRoute()

const loading = ref(true)
const error = ref('')
const record = ref<any>(null)

const isWechat = computed(() => isWechatBrowser())

// 提取HTML内容并进行XSS消毒
const contentHtml = computed(() => {
  if (!record.value?.contentSnapshot) return ''
  let html = record.value.contentSnapshot
  // 尝试提取body内容
  const bodyMatch = html.match(/<body[^>]*>([\s\S]*?)<\/body>/i)
  if (bodyMatch) {
    html = bodyMatch[1]
  }
  // 使用DOMPurify消毒HTML，防止XSS攻击
  return DOMPurify.sanitize(html, {
    ALLOWED_TAGS: ['p', 'br', 'strong', 'em', 'u', 'h1', 'h2', 'h3', 'h4', 'h5', 'h6',
                   'table', 'thead', 'tbody', 'tr', 'th', 'td', 'ul', 'ol', 'li',
                   'span', 'div', 'a', 'img'],
    ALLOWED_ATTR: ['class', 'style', 'href', 'src', 'alt', 'title', 'colspan', 'rowspan'],
    ALLOW_DATA_ATTR: false
  })
})

// 格式化日期
const formatDate = (dateStr?: string) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  })
}

// 加载通报数据
const loadNotification = async () => {
  const id = route.params.id as string
  if (!id) {
    error.value = '无效的通报ID'
    loading.value = false
    return
  }

  loading.value = true
  error.value = ''

  try {
    const data = await getNotificationPublic(id)
    record.value = data

    // 设置页面标题
    document.title = data.title || '量化检查通报'

    // 如果在微信中，初始化分享
    if (isWechatBrowser()) {
      await initWechatShare(id)
    }
  } catch (err: any) {
    error.value = err.message || '加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadNotification()
})
</script>

<style scoped lang="scss">
.notification-public-view {
  min-height: 100vh;
  background: #f5f5f5;
}

// 加载状态
.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 60vh;
  color: #666;

  .loading-spinner {
    width: 40px;
    height: 40px;
    border: 3px solid #e5e5e5;
    border-top-color: #3b82f6;
    border-radius: 50%;
    animation: spin 0.8s linear infinite;
  }

  p {
    margin-top: 16px;
    font-size: 14px;
  }
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

// 错误状态
.error-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 60vh;
  padding: 20px;
  text-align: center;

  .error-icon {
    width: 60px;
    height: 60px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: #fee2e2;
    color: #dc2626;
    border-radius: 50%;
    font-size: 32px;
    font-weight: bold;
    margin-bottom: 16px;
  }

  h2 {
    font-size: 18px;
    color: #1f2937;
    margin-bottom: 8px;
  }

  p {
    font-size: 14px;
    color: #6b7280;
    margin-bottom: 20px;
  }

  .btn-retry {
    padding: 10px 24px;
    background: #3b82f6;
    color: #fff;
    border: none;
    border-radius: 6px;
    font-size: 14px;
    cursor: pointer;

    &:active {
      background: #2563eb;
    }
  }
}

// 通报容器
.notification-container {
  max-width: 800px;
  margin: 0 auto;
  background: #fff;
  min-height: 100vh;
}

// 头部
.notification-header {
  padding: 24px 20px;
  border-bottom: 1px solid #e5e7eb;
  text-align: center;

  .notification-title {
    font-size: 20px;
    font-weight: 600;
    color: #1f2937;
    margin: 0 0 12px 0;
    line-height: 1.4;
  }

  .notification-meta {
    font-size: 13px;
    color: #9ca3af;
  }
}

// 内容区域
.notification-content {
  padding: 20px;
  font-size: 15px;
  line-height: 1.8;
  color: #374151;

  :deep(table) {
    width: 100%;
    border-collapse: collapse;
    margin: 16px 0;
    font-size: 13px;
  }

  :deep(th),
  :deep(td) {
    border: 1px solid #333;
    padding: 8px;
    text-align: center;
  }

  :deep(th) {
    background: #f0f0f0;
    font-weight: 600;
  }

  :deep(p) {
    margin: 0.8em 0;
  }

  :deep(h1),
  :deep(h2),
  :deep(h3) {
    margin: 1em 0 0.5em;
    color: #1f2937;
  }
}

// 底部
.notification-footer {
  padding: 20px;
  border-top: 1px solid #e5e7eb;
  text-align: center;

  .stats-info {
    display: flex;
    justify-content: center;
    gap: 20px;
    font-size: 13px;
    color: #6b7280;
    margin-bottom: 16px;
  }

  .share-tip {
    font-size: 12px;
    color: #9ca3af;
  }
}

// 微信分享提示
.wechat-share-tip {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  padding: 10px;
  background: rgba(0, 0, 0, 0.7);
  color: #fff;
  text-align: center;
  font-size: 13px;
  z-index: 100;
  animation: fadeInDown 0.3s ease;
}

@keyframes fadeInDown {
  from {
    opacity: 0;
    transform: translateY(-100%);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

// 移动端适配
@media (max-width: 640px) {
  .notification-header {
    padding: 20px 16px;

    .notification-title {
      font-size: 18px;
    }
  }

  .notification-content {
    padding: 16px;
    font-size: 14px;

    :deep(table) {
      font-size: 12px;
    }

    :deep(th),
    :deep(td) {
      padding: 6px 4px;
    }
  }
}
</style>
