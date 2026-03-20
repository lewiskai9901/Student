<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Pencil, Trash2, Pin, PinOff, ThumbsUp, Eye } from 'lucide-vue-next'
import {
  getArticles,
  createArticle,
  updateArticle,
  deleteArticle,
  getPinnedArticles,
  markHelpful,
  pinArticle,
  unpinArticle,
} from '@/api/insp/knowledgeArticle'
import type {
  KnowledgeArticle,
  CreateArticleRequest,
  UpdateArticleRequest,
} from '@/types/insp/knowledge'

// ==================== State ====================

const loading = ref(false)
const articles = ref<KnowledgeArticle[]>([])
const pinnedArticles = ref<KnowledgeArticle[]>([])
const keyword = ref('')
const categoryFilter = ref('')

// Dialog
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const dialogTitle = computed(() => (editingId.value ? '编辑文章' : '新建文章'))

const form = ref({
  title: '',
  content: '',
  category: '',
  tags: '',
})

// ==================== Categories ====================

const categories = computed(() => {
  const cats = new Set<string>()
  articles.value.forEach(a => {
    if (a.category) cats.add(a.category)
  })
  return [...cats].sort()
})

// ==================== Data Loading ====================

async function loadData() {
  loading.value = true
  try {
    const params: { keyword?: string; category?: string } = {}
    if (keyword.value.trim()) params.keyword = keyword.value.trim()
    if (categoryFilter.value) params.category = categoryFilter.value

    const [articleList, pinned] = await Promise.all([
      getArticles(params),
      getPinnedArticles(),
    ])
    articles.value = articleList
    pinnedArticles.value = pinned
  } catch (e: any) {
    ElMessage.error(e.message || '加载知识库失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  loadData()
}

// ==================== CRUD ====================

function openCreate() {
  editingId.value = null
  form.value = { title: '', content: '', category: '', tags: '' }
  dialogVisible.value = true
}

function openEdit(article: KnowledgeArticle) {
  editingId.value = article.id
  form.value = {
    title: article.title,
    content: article.content,
    category: article.category ?? '',
    tags: article.tags ?? '',
  }
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!form.value.title.trim()) {
    ElMessage.warning('请输入文章标题')
    return
  }
  if (!form.value.content.trim()) {
    ElMessage.warning('请输入文章内容')
    return
  }
  try {
    if (editingId.value) {
      const req: UpdateArticleRequest = {
        title: form.value.title,
        content: form.value.content,
        category: form.value.category || undefined,
        tags: form.value.tags || undefined,
      }
      await updateArticle(editingId.value, req)
      ElMessage.success('更新成功')
    } else {
      const req: CreateArticleRequest = {
        title: form.value.title,
        content: form.value.content,
        category: form.value.category || undefined,
        tags: form.value.tags || undefined,
      }
      await createArticle(req)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    await loadData()
  } catch (e: any) {
    ElMessage.error(e.message || '保存失败')
  }
}

async function handleDelete(article: KnowledgeArticle) {
  try {
    await ElMessageBox.confirm(`确认删除文章「${article.title}」？`, '确认删除', { type: 'warning' })
    await deleteArticle(article.id)
    ElMessage.success('删除成功')
    await loadData()
  } catch { /* cancelled */ }
}

// ==================== Actions ====================

async function handleTogglePin(article: KnowledgeArticle) {
  try {
    if (article.isPinned) {
      await unpinArticle(article.id)
      ElMessage.success('已取消置顶')
    } else {
      await pinArticle(article.id)
      ElMessage.success('已置顶')
    }
    await loadData()
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  }
}

async function handleMarkHelpful(article: KnowledgeArticle) {
  try {
    await markHelpful(article.id)
    ElMessage.success('感谢反馈')
    await loadData()
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  }
}

function parseTags(tags: string | null): string[] {
  if (!tags) return []
  try {
    const arr = JSON.parse(tags)
    return Array.isArray(arr) ? arr : [tags]
  } catch {
    return tags.split(',').map(t => t.trim()).filter(Boolean)
  }
}

// ==================== Lifecycle ====================

onMounted(() => loadData())
</script>

<template>
  <div class="p-5 space-y-4">
    <div class="flex items-center justify-between">
      <h2 class="text-lg font-semibold">知识库</h2>
      <el-button type="primary" @click="openCreate">
        <Plus class="w-4 h-4 mr-1" />新建文章
      </el-button>
    </div>

    <!-- Search & Filter -->
    <el-card shadow="never">
      <div class="flex items-center gap-4">
        <el-input
          v-model="keyword"
          placeholder="搜索关键词"
          clearable
          class="w-60"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        />
        <el-select v-model="categoryFilter" clearable placeholder="分类筛选" class="w-40" @change="handleSearch">
          <el-option v-for="cat in categories" :key="cat" :label="cat" :value="cat" />
        </el-select>
        <el-button @click="handleSearch">搜索</el-button>
      </div>
    </el-card>

    <!-- Pinned Articles -->
    <template v-if="pinnedArticles.length > 0">
      <div class="text-sm font-medium text-gray-500 flex items-center gap-1">
        <Pin class="w-3.5 h-3.5" /> 置顶文章
      </div>
      <div class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-3">
        <el-card
          v-for="article in pinnedArticles"
          :key="'pin-' + article.id"
          shadow="hover"
          class="cursor-pointer"
        >
          <div class="flex items-start justify-between mb-2">
            <h3 class="font-medium text-sm line-clamp-1">{{ article.title }}</h3>
            <el-tag v-if="article.category" size="small" type="info">{{ article.category }}</el-tag>
          </div>
          <p class="text-xs text-gray-500 line-clamp-2 mb-3">{{ article.content }}</p>
          <div class="flex items-center gap-2 flex-wrap mb-2">
            <el-tag
              v-for="tag in parseTags(article.tags)"
              :key="tag"
              size="small"
              type="info"
              effect="plain"
            >{{ tag }}</el-tag>
          </div>
          <div class="flex items-center justify-between text-xs text-gray-400">
            <div class="flex items-center gap-3">
              <span class="flex items-center gap-0.5"><Eye class="w-3 h-3" /> {{ article.viewCount }}</span>
              <span class="flex items-center gap-0.5"><ThumbsUp class="w-3 h-3" /> {{ article.helpfulCount }}</span>
            </div>
            <div class="flex items-center gap-1">
              <el-button link size="small" @click.stop="handleMarkHelpful(article)">
                <ThumbsUp class="w-3.5 h-3.5 text-gray-400" />
              </el-button>
              <el-button link size="small" @click.stop="handleTogglePin(article)">
                <PinOff class="w-3.5 h-3.5 text-gray-400" />
              </el-button>
              <el-button link type="primary" size="small" @click.stop="openEdit(article)">
                <Pencil class="w-3.5 h-3.5" />
              </el-button>
              <el-button link type="danger" size="small" @click.stop="handleDelete(article)">
                <Trash2 class="w-3.5 h-3.5" />
              </el-button>
            </div>
          </div>
        </el-card>
      </div>
    </template>

    <!-- All Articles -->
    <div class="text-sm font-medium text-gray-500">全部文章 ({{ articles.length }})</div>
    <div v-loading="loading" class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-3">
      <el-card
        v-for="article in articles"
        :key="article.id"
        shadow="hover"
        class="cursor-pointer"
      >
        <div class="flex items-start justify-between mb-2">
          <div class="flex items-center gap-1.5">
            <Pin v-if="article.isPinned" class="w-3 h-3 text-warning flex-shrink-0" />
            <h3 class="font-medium text-sm line-clamp-1">{{ article.title }}</h3>
          </div>
          <el-tag v-if="article.category" size="small" type="info" class="flex-shrink-0 ml-2">{{ article.category }}</el-tag>
        </div>
        <p class="text-xs text-gray-500 line-clamp-2 mb-3">{{ article.content }}</p>
        <div class="flex items-center gap-2 flex-wrap mb-2">
          <el-tag
            v-for="tag in parseTags(article.tags)"
            :key="tag"
            size="small"
            type="info"
            effect="plain"
          >{{ tag }}</el-tag>
        </div>
        <div class="flex items-center justify-between text-xs text-gray-400">
          <div class="flex items-center gap-3">
            <span class="flex items-center gap-0.5"><Eye class="w-3 h-3" /> {{ article.viewCount }}</span>
            <span class="flex items-center gap-0.5"><ThumbsUp class="w-3 h-3" /> {{ article.helpfulCount }}</span>
          </div>
          <div class="flex items-center gap-1">
            <el-button link size="small" @click.stop="handleMarkHelpful(article)">
              <ThumbsUp class="w-3.5 h-3.5 text-gray-400" />
            </el-button>
            <el-button link size="small" @click.stop="handleTogglePin(article)">
              <component :is="article.isPinned ? PinOff : Pin" class="w-3.5 h-3.5 text-gray-400" />
            </el-button>
            <el-button link type="primary" size="small" @click.stop="openEdit(article)">
              <Pencil class="w-3.5 h-3.5" />
            </el-button>
            <el-button link type="danger" size="small" @click.stop="handleDelete(article)">
              <Trash2 class="w-3.5 h-3.5" />
            </el-button>
          </div>
        </div>
      </el-card>

      <!-- Empty State -->
      <div v-if="!loading && articles.length === 0" class="col-span-full text-center py-12 text-gray-400">
        暂无文章
      </div>
    </div>

    <!-- ==================== Create/Edit Dialog ==================== -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="标题" required>
          <el-input v-model="form.title" placeholder="输入文章标题" />
        </el-form-item>
        <el-form-item label="内容" required>
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="8"
            placeholder="输入文章内容"
          />
        </el-form-item>
        <el-form-item label="分类">
          <el-input v-model="form.category" placeholder="例如：卫生检查、安全规范" />
        </el-form-item>
        <el-form-item label="标签">
          <el-input v-model="form.tags" placeholder="逗号分隔，例如：清洁,消毒,规范" />
          <div class="text-xs text-gray-400 mt-1">多个标签用逗号分隔</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.text-warning { color: #E6A23C; }
</style>
