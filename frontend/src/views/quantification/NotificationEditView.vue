<template>
  <div class="notification-edit-view">
    <!-- 顶部工具栏 -->
    <div class="edit-header">
      <div class="header-left">
        <button class="btn btn-ghost" @click="goBack">
          <ArrowLeft :size="18" />
          返回
        </button>
        <div class="title-section">
          <input
            v-model="title"
            type="text"
            class="title-input"
            placeholder="请输入通报标题"
            :disabled="isPublished"
          />
          <span :class="['status-badge', publishStatus === 1 ? 'published' : 'draft']">
            {{ publishStatus === 1 ? '已发布' : '草稿' }}
          </span>
        </div>
      </div>
      <div class="header-actions">
        <button
          v-if="!isPublished"
          class="btn btn-outline"
          @click="saveContent"
          :disabled="saving"
        >
          <Save :size="16" />
          {{ saving ? '保存中...' : '保存' }}
        </button>
        <button
          v-if="!isPublished"
          class="btn btn-primary"
          @click="confirmPublish"
        >
          <Send :size="16" />
          发布
        </button>
        <el-dropdown @command="handleDownload" trigger="click">
          <button class="btn btn-outline">
            <Download :size="16" />
            下载
            <ChevronDown :size="14" />
          </button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="PDF">
                <FileText :size="14" class="dropdown-icon" />
                下载 PDF
              </el-dropdown-item>
              <el-dropdown-item command="WORD">
                <FileType :size="14" class="dropdown-icon" />
                下载 Word
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        <el-dropdown trigger="click" @command="handleShare">
          <button class="btn btn-outline">
            <Share2 :size="16" />
            分享
            <ChevronDown :size="14" />
          </button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="wechat">
                <MessageSquare :size="14" class="dropdown-icon" />
                分享到微信
              </el-dropdown-item>
              <el-dropdown-item command="copy">
                <Link2 :size="14" class="dropdown-icon" />
                复制分享链接
              </el-dropdown-item>
              <el-dropdown-item command="qrcode">
                <QrCode :size="14" class="dropdown-icon" />
                生成二维码
              </el-dropdown-item>
              <el-dropdown-item disabled divider>
                <Bell :size="14" class="dropdown-icon" />
                公众号推送（开发中）
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>

    <!-- 编辑器区域 -->
    <div class="edit-content" v-loading="loading">
      <div v-if="!loading" class="editor-wrapper">
        <Editor
          v-model="content"
          :init="editorConfig"
          :disabled="isPublished"
        />
      </div>
    </div>

    <!-- 底部信息栏 -->
    <div class="edit-footer">
      <div class="meta-info">
        <span v-if="record">
          创建时间：{{ formatDate(record.createdAt) }}
        </span>
        <span v-if="record?.updatedAt">
          | 最后修改：{{ formatDate(record.updatedAt) }}
        </span>
      </div>
      <div class="meta-stats" v-if="record">
        <span>涉及 {{ record.totalCount }} 人</span>
        <span>| {{ record.totalClasses }} 个班级</span>
        <span>| {{ record.totalDeductionCount }} 条扣分</span>
      </div>
    </div>

    <!-- 二维码对话框 -->
    <el-dialog
      v-model="showQrcodeDialog"
      title="扫码分享"
      width="320px"
      center
    >
      <div class="qrcode-container">
        <img :src="qrcodeUrl" alt="分享二维码" class="qrcode-img" />
        <p class="qrcode-tip">使用微信扫一扫分享</p>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowLeft, Save, Send, Download, Share2, ChevronDown,
  FileText, FileType, MessageSquare, Bell, Link2, QrCode
} from 'lucide-vue-next'
import Editor from '@tinymce/tinymce-vue'
// TinyMCE 核心 - 必须在组件之前导入
import tinymce from 'tinymce/tinymce'
import 'tinymce/themes/silver/theme'
import 'tinymce/icons/default/icons'
import 'tinymce/models/dom/model'

// 在导入插件之前设置全局默认配置（重要！）
tinymce.overrideDefaults({
  license_key: 'gpl',
  promotion: false,
  branding: false
})

// TinyMCE 插件
import 'tinymce/plugins/advlist'
import 'tinymce/plugins/autolink'
import 'tinymce/plugins/lists'
import 'tinymce/plugins/link'
import 'tinymce/plugins/image'
import 'tinymce/plugins/charmap'
import 'tinymce/plugins/preview'
import 'tinymce/plugins/anchor'
import 'tinymce/plugins/searchreplace'
import 'tinymce/plugins/visualblocks'
import 'tinymce/plugins/code'
import 'tinymce/plugins/fullscreen'
import 'tinymce/plugins/insertdatetime'
import 'tinymce/plugins/media'
import 'tinymce/plugins/table'
import 'tinymce/plugins/help'
import 'tinymce/plugins/wordcount'
// TinyMCE 皮肤
import 'tinymce/skins/ui/oxide/skin.min.css'
import contentCss from 'tinymce/skins/content/default/content.min.css?raw'
import contentUiCss from 'tinymce/skins/ui/oxide/content.min.css?raw'
import {
  getNotificationById,
  updateNotificationContent,
  publishNotification,
  downloadNotification,
  type NotificationRecord
} from '@/api/notification'
import {
  initWechatShare,
  isWechatBrowser,
  copyShareLink,
  getShareInfo
} from '@/api/wechat'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const saving = ref(false)
const record = ref<NotificationRecord | null>(null)
const title = ref('')
const content = ref('')
const publishStatus = ref(0)

const isPublished = computed(() => publishStatus.value === 1)

// TinyMCE 编辑器配置（本地版本 + GPL 许可）
const editorConfig = {
  license_key: 'gpl',
  height: '100%',
  min_height: 500,
  language: 'zh_CN',
  language_url: '/tinymce/langs/zh_CN.js',
  skin: false,
  content_css: false,
  content_style: [contentCss, contentUiCss, `
    body {
      font-family: 'Microsoft YaHei', SimSun, serif;
      font-size: 14px;
      line-height: 1.8;
      padding: 20px 25px;
    }
    p { margin: 0.5em 0; }
    table { border-collapse: collapse; width: 100%; margin: 10px 0; }
    table td, table th { border: 1px solid #333; padding: 8px; }
    table th { background: #f0f0f0; font-weight: bold; }
  `].join('\n'),
  plugins: [
    'advlist', 'autolink', 'lists', 'link', 'image', 'charmap', 'preview',
    'anchor', 'searchreplace', 'visualblocks', 'code', 'fullscreen',
    'insertdatetime', 'media', 'table', 'help', 'wordcount'
  ],
  toolbar: 'undo redo | blocks fontfamily fontsize | bold italic underline strikethrough | ' +
    'alignleft aligncenter alignright alignjustify | ' +
    'bullist numlist outdent indent | forecolor backcolor | table | removeformat | fullscreen preview help',
  menubar: 'file edit view insert format tools table help',
  branding: false,
  promotion: false
}

// 加载通报数据
const loadNotification = async () => {
  const id = route.params.id as string
  if (!id) {
    ElMessage.error('缺少通报ID')
    goBack()
    return
  }

  loading.value = true
  try {
    const data = await getNotificationById(id)
    record.value = data
    title.value = data.title || ''
    publishStatus.value = data.publishStatus || 0

    // 提取HTML内容（去掉外层包装）
    let htmlContent = data.contentSnapshot || ''
    // 尝试提取body内容
    const bodyMatch = htmlContent.match(/<body[^>]*>([\s\S]*?)<\/body>/i)
    if (bodyMatch) {
      htmlContent = bodyMatch[1]
    }
    content.value = htmlContent
  } catch (error: any) {
    ElMessage.error(error.message || '加载通报失败')
    goBack()
  } finally {
    loading.value = false
  }
}

// 保存内容
const saveContent = async () => {
  if (!record.value) return
  if (isPublished.value) {
    ElMessage.warning('已发布的通报不能编辑')
    return
  }

  saving.value = true
  try {
    await updateNotificationContent(record.value.id, {
      title: title.value,
      contentHtml: content.value
    })
    ElMessage.success('保存成功')
  } catch (error: any) {
    ElMessage.error(error.message || '保存失败')
  } finally {
    saving.value = false
  }
}

// 确认发布
const confirmPublish = async () => {
  try {
    await ElMessageBox.confirm(
      '发布后通报将不能再编辑，确定要发布吗？',
      '发布确认',
      {
        confirmButtonText: '确定发布',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    // 先保存当前内容
    await saveContent()

    // 发布
    if (record.value) {
      await publishNotification(record.value.id)
      publishStatus.value = 1
      ElMessage.success('发布成功')
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '发布失败')
    }
  }
}

// 下载文件
const handleDownload = async (format: 'PDF' | 'WORD') => {
  if (!record.value) return

  try {
    ElMessage.info(`正在生成${format}文件...`)
    const blob = await downloadNotification(record.value.id, format)

    // 创建下载链接
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    const ext = format === 'WORD' ? '.docx' : '.pdf'
    link.download = (title.value || '通报') + ext
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)

    ElMessage.success('下载成功')
  } catch (error: any) {
    ElMessage.error(error.message || '下载失败')
  }
}

// 分享处理
const showQrcodeDialog = ref(false)
const qrcodeUrl = ref('')

const handleShare = async (command: string) => {
  if (!record.value) return

  switch (command) {
    case 'wechat':
      if (isWechatBrowser()) {
        // 在微信浏览器中，初始化分享
        const success = await initWechatShare(record.value.id)
        if (success) {
          ElMessage.success('已设置分享内容，点击右上角分享按钮即可分享')
        } else {
          ElMessage.warning('微信分享功能初始化失败，请复制链接分享')
        }
      } else {
        // 不在微信浏览器中，提示用户
        ElMessage.info('请在微信中打开此页面进行分享，或复制链接分享')
      }
      break

    case 'copy':
      try {
        // 直接构建分享链接
        const shareUrl = `${window.location.origin}/notification/view/${record.value.id}`
        await navigator.clipboard.writeText(shareUrl)
        ElMessage.success('链接已复制到剪贴板')
      } catch (error) {
        ElMessage.error('复制失败，请手动复制')
      }
      break

    case 'qrcode':
      try {
        // 直接使用当前页面URL生成二维码（无需调用后端API）
        const shareUrl = `${window.location.origin}/notification/view/${record.value.id}`
        // 使用第三方API生成二维码
        qrcodeUrl.value = `https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=${encodeURIComponent(shareUrl)}`
        showQrcodeDialog.value = true
      } catch (error) {
        ElMessage.error('生成二维码失败')
      }
      break
  }
}

// 格式化日期
const formatDate = (dateStr?: string) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN')
}

// 返回上一页
const goBack = () => {
  router.back()
}

// 页面离开前提示
const handleBeforeUnload = (e: BeforeUnloadEvent) => {
  if (!isPublished.value) {
    e.preventDefault()
    e.returnValue = ''
  }
}

onMounted(() => {
  loadNotification()
  window.addEventListener('beforeunload', handleBeforeUnload)
})

onBeforeUnmount(() => {
  window.removeEventListener('beforeunload', handleBeforeUnload)
})
</script>

<style scoped lang="scss">
.notification-edit-view {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #f5f7fa;
}

.edit-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 24px;
  background: #fff;
  border-bottom: 1px solid #e5e7eb;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);

  .header-left {
    display: flex;
    align-items: center;
    gap: 16px;
    flex: 1;
  }

  .title-section {
    display: flex;
    align-items: center;
    gap: 12px;
    flex: 1;
    max-width: 600px;
  }

  .title-input {
    flex: 1;
    padding: 8px 12px;
    font-size: 16px;
    font-weight: 600;
    border: 1px solid transparent;
    border-radius: 6px;
    outline: none;
    transition: all 0.2s;

    &:hover:not(:disabled) {
      border-color: #e5e7eb;
    }

    &:focus:not(:disabled) {
      border-color: #3b82f6;
      box-shadow: 0 0 0 2px rgba(59, 130, 246, 0.1);
    }

    &:disabled {
      background: transparent;
      color: #1f2937;
    }
  }

  .status-badge {
    padding: 4px 10px;
    border-radius: 4px;
    font-size: 12px;
    font-weight: 500;
    white-space: nowrap;

    &.draft {
      background: #fef3c7;
      color: #d97706;
    }

    &.published {
      background: #d1fae5;
      color: #059669;
    }
  }

  .header-actions {
    display: flex;
    gap: 8px;
  }
}

.edit-content {
  flex: 1;
  padding: 24px;
  overflow: hidden;

  .editor-wrapper {
    display: flex;
    flex-direction: column;
    height: 100%;
    background: #fff;
    border-radius: 8px;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
    overflow: hidden;
  }
}

.edit-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 24px;
  background: #fff;
  border-top: 1px solid #e5e7eb;
  font-size: 13px;
  color: #6b7280;

  .meta-info, .meta-stats {
    display: flex;
    gap: 8px;
  }
}

// 按钮样式
.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 8px 16px;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;

  &:disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }

  &.btn-primary {
    background: #3b82f6;
    color: #fff;

    &:hover:not(:disabled) {
      background: #2563eb;
    }
  }

  &.btn-outline {
    background: #fff;
    border: 1px solid #d1d5db;
    color: #374151;

    &:hover:not(:disabled) {
      background: #f9fafb;
      border-color: #9ca3af;
    }
  }

  &.btn-ghost {
    background: transparent;
    color: #6b7280;

    &:hover:not(:disabled) {
      background: #f3f4f6;
      color: #1f2937;
    }
  }
}

.dropdown-icon {
  margin-right: 8px;
}

// TinyMCE 样式
:deep(.tox-tinymce) {
  border: none !important;
  border-radius: 0 !important;
}

:deep(.tox-editor-header) {
  border-bottom: 1px solid #e5e7eb !important;
}

:deep(.tox-statusbar) {
  border-top: 1px solid #e5e7eb !important;
}

// 二维码容器
.qrcode-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px;

  .qrcode-img {
    width: 200px;
    height: 200px;
    border: 1px solid #e5e7eb;
    border-radius: 8px;
  }

  .qrcode-tip {
    margin-top: 16px;
    color: #6b7280;
    font-size: 14px;
  }
}
</style>
