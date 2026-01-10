<template>
  <div class="min-h-screen bg-gray-50/50 p-6">
    <!-- 页面头部 -->
    <div class="mb-6 rounded-xl bg-gradient-to-r from-emerald-600 to-green-500 p-6 shadow-lg">
      <div class="flex items-center justify-between">
        <div>
          <h1 class="flex items-center gap-3 text-2xl font-bold text-white">
            <ClipboardList class="h-8 w-8" />
            检查模板管理
          </h1>
          <p class="mt-1 text-emerald-100">管理日常检查模板及其关联的检查类别</p>
        </div>
        <div class="flex gap-3">
          <button
            v-if="hasPermission('quantification:template:add')"
            @click="handleAdd"
            class="flex items-center gap-2 rounded-lg bg-white/95 px-4 py-2 font-medium text-emerald-600 shadow-md transition-all hover:-translate-y-0.5 hover:bg-white hover:shadow-lg"
          >
            <Plus class="h-5 w-5" />
            新增模板
          </button>
        </div>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="mb-6 grid grid-cols-4 gap-4">
      <div class="group relative overflow-hidden rounded-xl bg-white p-5 shadow-sm transition-all hover:-translate-y-1 hover:shadow-md">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-sm font-medium text-gray-500">模板总数</p>
            <p class="mt-1 text-2xl font-bold text-gray-900">{{ stats.total }}</p>
          </div>
          <div class="flex h-12 w-12 items-center justify-center rounded-xl bg-emerald-100 text-emerald-600">
            <ClipboardList class="h-6 w-6" />
          </div>
        </div>
        <div class="absolute bottom-0 left-0 h-1 w-full bg-gradient-to-r from-emerald-500 to-green-400 opacity-0 transition-opacity group-hover:opacity-100"></div>
      </div>
      <div class="group relative overflow-hidden rounded-xl bg-white p-5 shadow-sm transition-all hover:-translate-y-1 hover:shadow-md">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-sm font-medium text-gray-500">已启用</p>
            <p class="mt-1 text-2xl font-bold text-green-600">{{ stats.enabled }}</p>
          </div>
          <div class="flex h-12 w-12 items-center justify-center rounded-xl bg-green-100 text-green-600">
            <CheckCircle class="h-6 w-6" />
          </div>
        </div>
        <div class="absolute bottom-0 left-0 h-1 w-full bg-gradient-to-r from-green-500 to-emerald-400 opacity-0 transition-opacity group-hover:opacity-100"></div>
      </div>
      <div class="group relative overflow-hidden rounded-xl bg-white p-5 shadow-sm transition-all hover:-translate-y-1 hover:shadow-md">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-sm font-medium text-gray-500">已禁用</p>
            <p class="mt-1 text-2xl font-bold text-gray-500">{{ stats.disabled }}</p>
          </div>
          <div class="flex h-12 w-12 items-center justify-center rounded-xl bg-gray-100 text-gray-500">
            <XCircle class="h-6 w-6" />
          </div>
        </div>
        <div class="absolute bottom-0 left-0 h-1 w-full bg-gradient-to-r from-gray-400 to-gray-300 opacity-0 transition-opacity group-hover:opacity-100"></div>
      </div>
      <div class="group relative overflow-hidden rounded-xl bg-white p-5 shadow-sm transition-all hover:-translate-y-1 hover:shadow-md">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-sm font-medium text-gray-500">默认模板</p>
            <p class="mt-1 text-2xl font-bold text-amber-600">{{ stats.defaultCount }}</p>
          </div>
          <div class="flex h-12 w-12 items-center justify-center rounded-xl bg-amber-100 text-amber-600">
            <Star class="h-6 w-6" />
          </div>
        </div>
        <div class="absolute bottom-0 left-0 h-1 w-full bg-gradient-to-r from-amber-500 to-yellow-400 opacity-0 transition-opacity group-hover:opacity-100"></div>
      </div>
    </div>

    <!-- 搜索栏 -->
    <div class="mb-6 rounded-xl bg-white p-5 shadow-sm">
      <div class="flex items-center gap-4">
        <div class="relative flex-1 max-w-md">
          <Search class="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
          <input
            v-model="queryForm.templateName"
            type="text"
            placeholder="搜索模板名称"
            class="w-full rounded-lg border border-gray-300 py-2 pl-10 pr-4 text-sm transition-colors focus:border-emerald-500 focus:outline-none focus:ring-2 focus:ring-emerald-500/20"
            @keyup.enter="handleQuery"
          />
        </div>
        <select
          v-model="queryForm.status"
          class="rounded-lg border border-gray-300 px-3 py-2 text-sm transition-colors focus:border-emerald-500 focus:outline-none focus:ring-2 focus:ring-emerald-500/20"
        >
          <option :value="undefined">全部状态</option>
          <option :value="1">启用</option>
          <option :value="0">禁用</option>
        </select>
        <button
          @click="handleQuery"
          class="flex items-center gap-2 rounded-lg bg-gradient-to-r from-emerald-600 to-green-500 px-4 py-2 text-sm font-medium text-white shadow-sm transition-all hover:-translate-y-0.5 hover:shadow-md"
        >
          <Search class="h-4 w-4" />
          搜索
        </button>
        <button
          @click="handleReset"
          class="flex items-center gap-2 rounded-lg border border-gray-300 bg-white px-4 py-2 text-sm font-medium text-gray-700 transition-all hover:bg-gray-50"
        >
          <RotateCcw class="h-4 w-4" />
          重置
        </button>
      </div>
    </div>

    <!-- 表格卡片 -->
    <div class="rounded-xl bg-white shadow-sm">
      <div class="flex items-center justify-between border-b border-gray-100 px-5 py-4">
        <div class="flex items-center gap-3">
          <h3 class="font-semibold text-gray-900">模板列表</h3>
          <span class="rounded-full bg-emerald-100 px-2.5 py-0.5 text-xs font-medium text-emerald-700">
            {{ total }} 条记录
          </span>
        </div>
      </div>

      <div class="overflow-x-auto">
        <table class="w-full">
          <thead>
            <tr class="border-b border-gray-100 bg-gray-50/50">
              <th class="whitespace-nowrap px-4 py-3 text-left text-sm font-semibold text-gray-900">模板名称</th>
              <th class="whitespace-nowrap px-4 py-3 text-left text-sm font-semibold text-gray-900">模板编码</th>
              <th class="whitespace-nowrap px-4 py-3 text-left text-sm font-semibold text-gray-900">描述</th>
              <th class="whitespace-nowrap px-4 py-3 text-center text-sm font-semibold text-gray-900">是否默认</th>
              <th class="whitespace-nowrap px-4 py-3 text-center text-sm font-semibold text-gray-900">状态</th>
              <th class="whitespace-nowrap px-4 py-3 text-left text-sm font-semibold text-gray-900">创建时间</th>
              <th class="whitespace-nowrap px-4 py-3 text-center text-sm font-semibold text-gray-900">操作</th>
            </tr>
          </thead>
          <tbody v-if="loading">
            <tr>
              <td colspan="7" class="py-20 text-center">
                <div class="flex flex-col items-center gap-3">
                  <Loader2 class="h-8 w-8 animate-spin text-emerald-600" />
                  <span class="text-sm text-gray-500">加载中...</span>
                </div>
              </td>
            </tr>
          </tbody>
          <tbody v-else-if="templateList.length === 0">
            <tr>
              <td colspan="7" class="py-20 text-center">
                <div class="flex flex-col items-center gap-3">
                  <ClipboardList class="h-12 w-12 text-gray-300" />
                  <span class="text-sm text-gray-500">暂无模板数据</span>
                </div>
              </td>
            </tr>
          </tbody>
          <tbody v-else>
            <tr
              v-for="(row, index) in templateList"
              :key="row.id"
              class="animate-fade-in border-b border-gray-50 transition-colors hover:bg-emerald-50/30"
              :style="{ animationDelay: `${index * 50}ms` }"
            >
              <td class="whitespace-nowrap px-4 py-3">
                <div class="flex items-center gap-3">
                  <div class="flex h-9 w-9 items-center justify-center rounded-lg bg-emerald-100">
                    <ClipboardList class="h-5 w-5 text-emerald-600" />
                  </div>
                  <span class="font-medium text-gray-900">{{ row.templateName }}</span>
                </div>
              </td>
              <td class="whitespace-nowrap px-4 py-3">
                <span class="rounded-md bg-gray-100 px-2 py-0.5 text-xs font-mono text-gray-600">
                  {{ row.templateCode }}
                </span>
              </td>
              <td class="max-w-xs truncate px-4 py-3 text-gray-600" :title="row.description">
                {{ row.description || '-' }}
              </td>
              <td class="whitespace-nowrap px-4 py-3 text-center">
                <span
                  v-if="row.isDefault === 1"
                  class="inline-flex items-center gap-1 rounded-full bg-amber-100 px-2.5 py-0.5 text-xs font-medium text-amber-700"
                >
                  <Star class="h-3 w-3" />
                  默认
                </span>
                <span v-else class="text-gray-400">-</span>
              </td>
              <td class="whitespace-nowrap px-4 py-3 text-center">
                <span
                  :class="[
                    'rounded-full px-2.5 py-0.5 text-xs font-medium',
                    row.status === 1 ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-600'
                  ]"
                >
                  {{ row.status === 1 ? '启用' : '禁用' }}
                </span>
              </td>
              <td class="whitespace-nowrap px-4 py-3 text-sm text-gray-500">
                {{ row.createdAt }}
              </td>
              <td class="whitespace-nowrap px-4 py-3">
                <div class="flex items-center justify-center gap-1">
                  <button
                    v-if="hasPermission('quantification:template:view')"
                    @click="handleView(row)"
                    class="rounded-lg p-1.5 text-gray-500 transition-colors hover:bg-blue-100 hover:text-blue-600"
                    title="查看"
                  >
                    <Eye class="h-4 w-4" />
                  </button>
                  <button
                    v-if="hasPermission('quantification:template:edit')"
                    @click="handleEdit(row)"
                    class="rounded-lg p-1.5 text-gray-500 transition-colors hover:bg-emerald-100 hover:text-emerald-600"
                    title="编辑"
                  >
                    <Pencil class="h-4 w-4" />
                  </button>
                  <button
                    v-if="hasPermission('quantification:template:delete')"
                    @click="handleDelete(row)"
                    class="rounded-lg p-1.5 text-gray-500 transition-colors hover:bg-red-100 hover:text-red-600"
                    title="删除"
                  >
                    <Trash2 class="h-4 w-4" />
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- 分页 -->
      <div v-if="total > 0" class="flex items-center justify-between border-t border-gray-100 px-5 py-4">
        <div class="text-sm text-gray-500">
          共 {{ total }} 条记录，第 {{ queryForm.pageNum }} / {{ Math.ceil(total / queryForm.pageSize) }} 页
        </div>
        <div class="flex items-center gap-2">
          <button
            @click="queryForm.pageNum = 1; handleQuery()"
            :disabled="queryForm.pageNum <= 1"
            class="rounded-lg border border-gray-300 px-3 py-1.5 text-sm transition-colors hover:bg-gray-50 disabled:opacity-50"
          >
            首页
          </button>
          <button
            @click="queryForm.pageNum--; handleQuery()"
            :disabled="queryForm.pageNum <= 1"
            class="rounded-lg border border-gray-300 px-3 py-1.5 text-sm transition-colors hover:bg-gray-50 disabled:opacity-50"
          >
            <ChevronLeft class="h-4 w-4" />
          </button>
          <template v-for="page in visiblePages" :key="page">
            <button
              v-if="page !== '...'"
              @click="queryForm.pageNum = page as number; handleQuery()"
              :class="[
                'min-w-[36px] rounded-lg px-3 py-1.5 text-sm transition-colors',
                queryForm.pageNum === page
                  ? 'bg-emerald-600 text-white'
                  : 'border border-gray-300 hover:bg-gray-50'
              ]"
            >
              {{ page }}
            </button>
            <span v-else class="px-2 text-gray-400">...</span>
          </template>
          <button
            @click="queryForm.pageNum++; handleQuery()"
            :disabled="queryForm.pageNum >= Math.ceil(total / queryForm.pageSize)"
            class="rounded-lg border border-gray-300 px-3 py-1.5 text-sm transition-colors hover:bg-gray-50 disabled:opacity-50"
          >
            <ChevronRight class="h-4 w-4" />
          </button>
          <button
            @click="queryForm.pageNum = Math.ceil(total / queryForm.pageSize); handleQuery()"
            :disabled="queryForm.pageNum >= Math.ceil(total / queryForm.pageSize)"
            class="rounded-lg border border-gray-300 px-3 py-1.5 text-sm transition-colors hover:bg-gray-50 disabled:opacity-50"
          >
            末页
          </button>
          <select
            v-model="queryForm.pageSize"
            @change="queryForm.pageNum = 1; handleQuery()"
            class="rounded-lg border border-gray-300 px-2 py-1.5 text-sm"
          >
            <option :value="10">10条/页</option>
            <option :value="20">20条/页</option>
            <option :value="50">50条/页</option>
            <option :value="100">100条/页</option>
          </select>
        </div>
      </div>
    </div>

    <!-- 新增/编辑对话框 - 全新现代简约设计 -->
    <Teleport to="body">
      <div v-if="dialogVisible" class="fixed inset-0 z-50 flex items-center justify-center">
        <div class="fixed inset-0 bg-black/60 backdrop-blur-sm" @click="dialogVisible = false"></div>
        <div class="relative z-10 w-full max-w-5xl rounded-2xl bg-white shadow-2xl">
          <!-- 对话框头部 -->
          <div class="flex items-center justify-between border-b border-gray-100 px-8 py-5">
            <div>
              <h3 class="text-xl font-semibold text-gray-900">{{ dialogTitle }}</h3>
              <p class="mt-1 text-sm text-gray-500">配置检查模板的基本信息和检查类别</p>
            </div>
            <button @click="dialogVisible = false" class="rounded-full p-2 text-gray-400 transition-colors hover:bg-gray-100 hover:text-gray-600">
              <X class="h-5 w-5" />
            </button>
          </div>

          <!-- 步骤指示器 -->
          <div class="border-b border-gray-100 px-8 py-4">
            <div class="flex items-center gap-8">
              <button
                @click="dialogStep = 1"
                :class="[
                  'flex items-center gap-3 pb-2 transition-all',
                  dialogStep === 1 ? 'border-b-2 border-emerald-500 text-emerald-600' : 'text-gray-500 hover:text-gray-700'
                ]"
              >
                <span :class="[
                  'flex h-7 w-7 items-center justify-center rounded-full text-sm font-medium',
                  dialogStep === 1 ? 'bg-emerald-500 text-white' : 'bg-gray-200 text-gray-600'
                ]">1</span>
                <span class="font-medium">基本信息</span>
              </button>
              <button
                @click="dialogStep = 2"
                :class="[
                  'flex items-center gap-3 pb-2 transition-all',
                  dialogStep === 2 ? 'border-b-2 border-emerald-500 text-emerald-600' : 'text-gray-500 hover:text-gray-700'
                ]"
              >
                <span :class="[
                  'flex h-7 w-7 items-center justify-center rounded-full text-sm font-medium',
                  dialogStep === 2 ? 'bg-emerald-500 text-white' : 'bg-gray-200 text-gray-600'
                ]">2</span>
                <span class="font-medium">检查类别</span>
                <span v-if="formData.categories.length > 0" class="rounded-full bg-emerald-100 px-2 py-0.5 text-xs font-medium text-emerald-700">
                  {{ formData.categories.length }}
                </span>
              </button>
            </div>
          </div>

          <div class="max-h-[60vh] overflow-y-auto">
            <!-- 步骤1: 基本信息 -->
            <div v-show="dialogStep === 1" class="p-8">
              <!-- 名称和编码 -->
              <div class="grid grid-cols-2 gap-6 mb-8">
                <div>
                  <label class="mb-2 block text-sm font-medium text-gray-700">
                    模板名称 <span class="text-red-500">*</span>
                  </label>
                  <input
                    v-model="formData.templateName"
                    type="text"
                    placeholder="例如：宿舍日常检查模板"
                    maxlength="100"
                    class="w-full rounded-xl border-2 border-gray-200 px-4 py-3 text-sm transition-all focus:border-emerald-500 focus:outline-none focus:ring-4 focus:ring-emerald-500/10"
                  />
                </div>
                <div>
                  <label class="mb-2 block text-sm font-medium text-gray-700">
                    模板编码 <span class="text-red-500">*</span>
                  </label>
                  <input
                    v-model="formData.templateCode"
                    type="text"
                    placeholder="例如：DORM_DAILY"
                    maxlength="50"
                    :disabled="editMode === 'edit'"
                    class="w-full rounded-xl border-2 border-gray-200 px-4 py-3 text-sm font-mono transition-all focus:border-emerald-500 focus:outline-none focus:ring-4 focus:ring-emerald-500/10 disabled:bg-gray-50 disabled:text-gray-500"
                  />
                </div>
              </div>

              <!-- 描述 -->
              <div class="mb-8">
                <label class="mb-2 block text-sm font-medium text-gray-700">模板描述</label>
                <textarea
                  v-model="formData.description"
                  rows="2"
                  placeholder="简要描述模板用途..."
                  maxlength="500"
                  class="w-full rounded-xl border-2 border-gray-200 px-4 py-3 text-sm transition-all focus:border-emerald-500 focus:outline-none focus:ring-4 focus:ring-emerald-500/10"
                ></textarea>
              </div>

              <!-- 快捷设置 -->
              <div class="mb-8 grid grid-cols-2 gap-4">
                <button
                  @click="formData.isDefault = formData.isDefault === 1 ? 0 : 1"
                  :class="[
                    'flex items-center gap-4 rounded-xl border-2 p-4 transition-all',
                    formData.isDefault === 1
                      ? 'border-amber-400 bg-amber-50'
                      : 'border-gray-200 hover:border-gray-300 hover:bg-gray-50'
                  ]"
                >
                  <div :class="[
                    'flex h-10 w-10 items-center justify-center rounded-lg',
                    formData.isDefault === 1 ? 'bg-amber-500 text-white' : 'bg-gray-100 text-gray-400'
                  ]">
                    <Star class="h-5 w-5" />
                  </div>
                  <div class="flex-1 text-left">
                    <div class="font-medium text-gray-900">设为默认模板</div>
                    <div class="text-xs text-gray-500">创建检查时自动选择此模板</div>
                  </div>
                  <div :class="[
                    'h-5 w-5 rounded-full border-2 flex items-center justify-center',
                    formData.isDefault === 1 ? 'border-amber-500 bg-amber-500' : 'border-gray-300'
                  ]">
                    <Check v-if="formData.isDefault === 1" class="h-3 w-3 text-white" />
                  </div>
                </button>

                <button
                  @click="formData.status = formData.status === 1 ? 0 : 1"
                  :class="[
                    'flex items-center gap-4 rounded-xl border-2 p-4 transition-all',
                    formData.status === 1
                      ? 'border-emerald-400 bg-emerald-50'
                      : 'border-gray-200 hover:border-gray-300 hover:bg-gray-50'
                  ]"
                >
                  <div :class="[
                    'flex h-10 w-10 items-center justify-center rounded-lg',
                    formData.status === 1 ? 'bg-emerald-500 text-white' : 'bg-gray-100 text-gray-400'
                  ]">
                    <CheckCircle class="h-5 w-5" />
                  </div>
                  <div class="flex-1 text-left">
                    <div class="font-medium text-gray-900">启用模板</div>
                    <div class="text-xs text-gray-500">禁用后无法用于创建检查</div>
                  </div>
                  <div :class="[
                    'h-5 w-5 rounded-full border-2 flex items-center justify-center',
                    formData.status === 1 ? 'border-emerald-500 bg-emerald-500' : 'border-gray-300'
                  ]">
                    <Check v-if="formData.status === 1" class="h-3 w-3 text-white" />
                  </div>
                </button>
              </div>

            </div>

            <!-- 步骤2: 检查类别 -->
            <div v-show="dialogStep === 2" class="p-8">
              <!-- 类别选择区域 -->
              <div class="grid grid-cols-2 gap-6">
                <!-- 可选类别 -->
                <div>
                  <div class="mb-4 flex items-center justify-between">
                    <h4 class="font-medium text-gray-900">可选类别</h4>
                    <span class="rounded-full bg-gray-100 px-2.5 py-1 text-xs font-medium text-gray-600">
                      {{ availableCategoriesFiltered.length }} 项
                    </span>
                  </div>
                  <div class="max-h-[400px] space-y-2 overflow-y-auto rounded-xl border-2 border-dashed border-gray-200 p-4">
                    <div
                      v-for="cat in availableCategoriesFiltered"
                      :key="cat.id"
                      @click="handleAddCategoryClick(cat)"
                      class="group flex cursor-pointer items-center gap-3 rounded-xl border border-gray-200 bg-white p-4 transition-all hover:border-emerald-400 hover:bg-emerald-50 hover:shadow-sm"
                    >
                      <div class="flex h-10 w-10 items-center justify-center rounded-lg bg-gray-100 text-gray-500 transition-colors group-hover:bg-emerald-500 group-hover:text-white">
                        <Folder class="h-5 w-5" />
                      </div>
                      <div class="flex-1 min-w-0">
                        <div class="font-medium text-gray-900 truncate">{{ cat.categoryName }}</div>
                        <div class="text-xs text-gray-500">{{ cat.categoryCode }}</div>
                      </div>
                      <Plus class="h-5 w-5 text-gray-400 transition-colors group-hover:text-emerald-600" />
                    </div>
                    <div v-if="availableCategoriesFiltered.length === 0" class="flex flex-col items-center justify-center py-12 text-gray-400">
                      <Folder class="mb-2 h-10 w-10" />
                      <span class="text-sm">所有类别已添加</span>
                    </div>
                  </div>
                </div>

                <!-- 已选类别 -->
                <div>
                  <div class="mb-4 flex items-center justify-between">
                    <h4 class="font-medium text-gray-900">已选类别</h4>
                    <span class="rounded-full bg-emerald-100 px-2.5 py-1 text-xs font-medium text-emerald-700">
                      {{ formData.categories.length }} 项
                    </span>
                  </div>
                  <div class="max-h-[400px] space-y-3 overflow-y-auto overflow-x-visible rounded-xl border-2 border-emerald-200 bg-emerald-50/30 p-4">
                    <div
                      v-for="(item, index) in formData.categories"
                      :key="item.categoryId || index"
                      class="rounded-xl border border-gray-200 bg-white shadow-sm"
                    >
                      <!-- 类别头部 -->
                      <div class="flex items-center gap-3 border-b border-gray-100 bg-gray-50/50 px-4 py-3">
                        <GripVertical class="h-4 w-4 cursor-move text-gray-300" />
                        <div class="flex h-8 w-8 items-center justify-center rounded-lg bg-emerald-500 text-white text-sm font-medium">
                          {{ index + 1 }}
                        </div>
                        <div class="flex-1 min-w-0">
                          <span class="font-medium text-gray-900">{{ getCategoryName(item.categoryId) }}</span>
                        </div>
                        <button
                          @click="handleRemoveCategory(index)"
                          class="rounded-lg p-1.5 text-gray-400 transition-colors hover:bg-red-100 hover:text-red-500"
                        >
                          <X class="h-4 w-4" />
                        </button>
                      </div>

                      <!-- 类别配置 -->
                      <div class="p-4 space-y-3">
                        <!-- 快捷配置按钮组 -->
                        <div class="flex flex-wrap items-center gap-2">
                          <!-- 关联类型 -->
                          <div class="flex items-center rounded-lg bg-gray-100 p-0.5">
                            <button
                              v-for="(lt, ltIdx) in linkTypes"
                              :key="ltIdx"
                              @click="item.linkType = ltIdx"
                              :class="[
                                'rounded-md px-3 py-1.5 text-xs font-medium transition-all',
                                item.linkType === ltIdx
                                  ? 'bg-white text-gray-900 shadow-sm'
                                  : 'text-gray-500 hover:text-gray-700'
                              ]"
                            >
                              {{ lt === '无' ? '不关联' : lt }}
                            </button>
                          </div>
                          <!-- 必检 -->
                          <button
                            @click="item.isRequired = item.isRequired === 1 ? 0 : 1"
                            :class="[
                              'flex items-center gap-1.5 rounded-lg px-3 py-1.5 text-xs font-medium transition-all',
                              item.isRequired === 1
                                ? 'bg-emerald-100 text-emerald-700'
                                : 'bg-gray-100 text-gray-500 hover:bg-gray-200'
                            ]"
                          >
                            <Check v-if="item.isRequired === 1" class="h-3.5 w-3.5" />
                            必检
                          </button>
                        </div>

                        <!-- 扣分项折叠区域 -->
                        <div class="border-t border-gray-100 pt-3">
                          <button
                            @click="toggleCategoryExpand(item.categoryId)"
                            class="flex w-full items-center justify-between text-sm text-gray-600 hover:text-gray-900"
                          >
                            <span class="flex items-center gap-2">
                              <FileText class="h-4 w-4" />
                              <span>扣分项</span>
                              <span class="rounded bg-gray-100 px-1.5 py-0.5 text-xs">
                                {{ deductionItemsCache[item.categoryId]?.length || 0 }}
                              </span>
                            </span>
                            <ChevronDown
                              :class="[
                                'h-4 w-4 transition-transform',
                                expandedCategories.has(item.categoryId) ? 'rotate-180' : ''
                              ]"
                            />
                          </button>

                          <!-- 扣分项列表 -->
                          <div v-if="expandedCategories.has(item.categoryId)" class="mt-3 space-y-2">
                            <div
                              v-for="deductItem in (deductionItemsCache[item.categoryId] || [])"
                              :key="deductItem.id"
                              class="flex items-center justify-between rounded-lg bg-gray-50 px-3 py-2"
                            >
                              <div class="flex-1 min-w-0">
                                <div class="flex items-center gap-2">
                                  <span class="text-sm text-gray-700 truncate">{{ deductItem.itemName }}</span>
                                  <span class="rounded bg-gray-200 px-1.5 py-0.5 text-xs text-gray-500">
                                    {{ getDeductModeText(deductItem.deductMode) }}
                                  </span>
                                </div>
                                <div class="text-xs text-gray-400 mt-0.5">
                                  <span v-if="deductItem.deductMode === 1">{{ deductItem.fixedScore }}分</span>
                                  <span v-else-if="deductItem.deductMode === 2">{{ deductItem.minScore }}-{{ deductItem.maxScore }}分</span>
                                  <span v-else-if="deductItem.deductMode === 3">{{ deductItem.perPersonScore || deductItem.scorePerPerson }}分/人</span>
                                </div>
                              </div>
                            </div>
                            <div v-if="!deductionItemsCache[item.categoryId]?.length" class="py-4 text-center text-xs text-gray-400">
                              暂无扣分项
                            </div>
                          </div>
                        </div>
                      </div>
                    </div>

                    <!-- 空状态 -->
                    <div v-if="formData.categories.length === 0" class="flex flex-col items-center justify-center py-12 text-gray-400">
                      <ArrowLeft class="mb-2 h-10 w-10" />
                      <span class="text-sm">点击左侧添加检查类别</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 底部操作栏 -->
          <div class="flex items-center justify-between border-t border-gray-100 px-8 py-5">
            <div>
              <button
                v-if="dialogStep === 2"
                @click="dialogStep = 1"
                class="flex items-center gap-2 text-sm text-gray-600 hover:text-gray-900"
              >
                <ChevronLeft class="h-4 w-4" />
                上一步
              </button>
            </div>
            <div class="flex items-center gap-3">
              <button
                @click="dialogVisible = false"
                class="rounded-xl border border-gray-300 px-5 py-2.5 text-sm font-medium text-gray-700 transition-colors hover:bg-gray-50"
              >
                取消
              </button>
              <button
                v-if="dialogStep === 1"
                @click="handleNextStep"
                class="flex items-center gap-2 rounded-xl bg-emerald-600 px-5 py-2.5 text-sm font-medium text-white transition-all hover:bg-emerald-700"
              >
                下一步
                <ChevronRight class="h-4 w-4" />
              </button>
              <button
                v-else
                @click="handleSubmit"
                :disabled="submitLoading"
                class="flex items-center gap-2 rounded-xl bg-emerald-600 px-6 py-2.5 text-sm font-medium text-white transition-all hover:bg-emerald-700 disabled:opacity-50"
              >
                <Loader2 v-if="submitLoading" class="h-4 w-4 animate-spin" />
                <Check v-else class="h-4 w-4" />
                保存模板
              </button>
            </div>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- 查看详情对话框 -->
    <Teleport to="body">
      <div v-if="viewDialogVisible" class="fixed inset-0 z-50 flex items-center justify-center">
        <div class="fixed inset-0 bg-black/50 backdrop-blur-sm" @click="viewDialogVisible = false"></div>
        <div class="relative z-10 w-full max-w-2xl rounded-xl bg-white shadow-2xl">
          <div class="flex items-center justify-between border-b border-gray-100 px-6 py-4">
            <h3 class="text-lg font-semibold text-gray-900">模板详情</h3>
            <button @click="viewDialogVisible = false" class="rounded-lg p-1 hover:bg-gray-100">
              <X class="h-5 w-5 text-gray-500" />
            </button>
          </div>
          <div class="max-h-[70vh] overflow-y-auto p-6">
            <div class="grid grid-cols-2 gap-4 mb-6">
              <div class="rounded-lg bg-gray-50 p-4">
                <div class="text-sm text-gray-500 mb-1">模板名称</div>
                <div class="font-medium text-gray-900">{{ viewData.templateName }}</div>
              </div>
              <div class="rounded-lg bg-gray-50 p-4">
                <div class="text-sm text-gray-500 mb-1">模板编码</div>
                <div class="font-mono text-gray-900">{{ viewData.templateCode }}</div>
              </div>
              <div class="rounded-lg bg-gray-50 p-4">
                <div class="text-sm text-gray-500 mb-1">是否默认</div>
                <span
                  :class="[
                    'inline-flex items-center gap-1 rounded-full px-2.5 py-0.5 text-xs font-medium',
                    viewData.isDefault === 1 ? 'bg-amber-100 text-amber-700' : 'bg-gray-100 text-gray-600'
                  ]"
                >
                  <Star v-if="viewData.isDefault === 1" class="h-3 w-3" />
                  {{ viewData.isDefault === 1 ? '是' : '否' }}
                </span>
              </div>
              <div class="rounded-lg bg-gray-50 p-4">
                <div class="text-sm text-gray-500 mb-1">状态</div>
                <span
                  :class="[
                    'rounded-full px-2.5 py-0.5 text-xs font-medium',
                    viewData.status === 1 ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-600'
                  ]"
                >
                  {{ viewData.status === 1 ? '启用' : '禁用' }}
                </span>
              </div>
              <div class="col-span-2 rounded-lg bg-gray-50 p-4">
                <div class="text-sm text-gray-500 mb-1">模板描述</div>
                <div class="text-gray-900">{{ viewData.description || '-' }}</div>
              </div>
              <div class="rounded-lg bg-gray-50 p-4">
                <div class="text-sm text-gray-500 mb-1">创建时间</div>
                <div class="text-gray-900">{{ viewData.createdAt }}</div>
              </div>
              <div class="rounded-lg bg-gray-50 p-4">
                <div class="text-sm text-gray-500 mb-1">更新时间</div>
                <div class="text-gray-900">{{ viewData.updatedAt }}</div>
              </div>
            </div>

            <div>
              <h4 class="mb-3 font-semibold text-gray-900">检查类别列表</h4>
              <div class="space-y-2">
                <div
                  v-for="(cat, index) in viewData.categories"
                  :key="index"
                  class="flex items-center gap-3 rounded-lg bg-gray-50 p-3"
                >
                  <div class="flex h-8 w-8 items-center justify-center rounded-full bg-emerald-600 text-sm font-bold text-white">
                    {{ index + 1 }}
                  </div>
                  <div class="flex-1">
                    <div class="font-medium text-gray-900">{{ cat.categoryName }}</div>
                    <div class="mt-1 flex flex-wrap gap-2">
                      <span class="rounded bg-gray-200 px-1.5 py-0.5 text-xs text-gray-600">{{ cat.categoryCode }}</span>
                      <span :class="['rounded px-1.5 py-0.5 text-xs', getLinkTypeClass(cat.linkType)]">
                        {{ getLinkTypeText(cat.linkType) }}
                      </span>
                      <span :class="['rounded px-1.5 py-0.5 text-xs', cat.isRequired === 1 ? 'bg-green-100 text-green-700' : 'bg-orange-100 text-orange-700']">
                        {{ cat.isRequired === 1 ? '必检' : '选检' }}
                      </span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
          <div class="flex justify-end border-t border-gray-100 px-6 py-4">
            <button
              @click="viewDialogVisible = false"
              class="rounded-lg border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 transition-colors hover:bg-gray-50"
            >
              关闭
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ClipboardList,
  Plus,
  Search,
  RotateCcw,
  Eye,
  Pencil,
  Trash2,
  X,
  Loader2,
  CheckCircle,
  XCircle,
  Star,
  ChevronLeft,
  ChevronRight,
  ChevronDown,
  Folder,
  GripVertical,
  FileText,
  Check,
  ArrowLeft,
  RefreshCw
} from 'lucide-vue-next'
import {
  getCheckTemplateById,
  createCheckTemplate,
  updateCheckTemplate,
  deleteCheckTemplate,
  getCheckTemplatePage,
  type CheckTemplateResponse,
  type CheckTemplateCreateRequest
} from '@/api/v2/quantification'
import { getAllEnabledTypes } from '@/api/v2/quantification'
import { getEnabledDeductionItemsByTypeId } from '@/api/v2/quantification'
import { useAuthStore } from '@/stores/auth'

// 权限检查
const authStore = useAuthStore()
const hasPermission = (permission: string) => authStore.hasPermission(permission)

const linkTypes = ['无', '宿舍', '教室']

// 查询表单
const queryForm = reactive({
  templateName: '',
  status: undefined as number | undefined,
  pageNum: 1,
  pageSize: 10
})

// 数据列表
const loading = ref(false)
const templateList = ref<CheckTemplateResponse[]>([])
const total = ref(0)

// 统计数据
const stats = computed(() => {
  return {
    total: total.value,
    enabled: templateList.value.filter(t => t.status === 1).length,
    disabled: templateList.value.filter(t => t.status === 0).length,
    defaultCount: templateList.value.filter(t => t.isDefault === 1).length
  }
})

// 可用的检查类别列表
const availableCategories = ref<any[]>([])

// 扣分项缓存 (按类别ID)
const deductionItemsCache = ref<Record<number, any[]>>({})
// 已展开的类别
const expandedCategories = ref<Set<number>>(new Set())

// 过滤已选择的类别
const availableCategoriesFiltered = computed(() => {
  const selectedIds = formData.categories.map(c => c.categoryId)
  return availableCategories.value.filter(cat => !selectedIds.includes(cat.id))
})

// 对话框
const dialogVisible = ref(false)
const dialogTitle = ref('')
const editMode = ref<'add' | 'edit'>('add')
const submitLoading = ref(false)
const dialogStep = ref(1)

// 表单数据
const formData = reactive<CheckTemplateCreateRequest & { id?: string | number }>({
  templateName: '',
  templateCode: '',
  description: '',
  isDefault: 0,
  status: 1,
  categories: []
})

// 查看详情
const viewDialogVisible = ref(false)
const viewData = ref<CheckTemplateResponse>({} as CheckTemplateResponse)

// 分页计算
const visiblePages = computed(() => {
  const totalPages = Math.ceil(total.value / queryForm.pageSize)
  const current = queryForm.pageNum
  const pages: (number | string)[] = []

  if (totalPages <= 7) {
    for (let i = 1; i <= totalPages; i++) pages.push(i)
  } else {
    if (current <= 4) {
      for (let i = 1; i <= 5; i++) pages.push(i)
      pages.push('...')
      pages.push(totalPages)
    } else if (current >= totalPages - 3) {
      pages.push(1)
      pages.push('...')
      for (let i = totalPages - 4; i <= totalPages; i++) pages.push(i)
    } else {
      pages.push(1)
      pages.push('...')
      for (let i = current - 1; i <= current + 1; i++) pages.push(i)
      pages.push('...')
      pages.push(totalPages)
    }
  }

  return pages
})

// 点击添加类别
const handleAddCategoryClick = async (cat: any) => {
  if (formData.categories.some(c => c.categoryId === cat.id)) {
    ElMessage.warning('该类别已添加')
    return
  }

  formData.categories.push({
    categoryId: cat.id,
    linkType: 0,
    sortOrder: formData.categories.length + 1,
    isRequired: 1
  })

  // 立即加载该类别的扣分项数量
  if (!deductionItemsCache.value[cat.id]) {
    try {
      const items = await getEnabledDeductionItemsByTypeId(cat.id)
      deductionItemsCache.value[cat.id] = items || []
    } catch (error) {
      console.error('加载扣分项失败', error)
      deductionItemsCache.value[cat.id] = []
    }
  }
}

// 获取类别名称
const getCategoryName = (categoryId: number) => {
  const cat = availableCategories.value.find(c => c.id === categoryId)
  return cat?.categoryName || '未知类别'
}

// 获取关联类型样式
const getLinkTypeClass = (linkType: number) => {
  const classes = ['bg-gray-100 text-gray-600', 'bg-blue-100 text-blue-700', 'bg-purple-100 text-purple-700']
  return classes[linkType] || classes[0]
}

// 获取关联类型文本
const getLinkTypeText = (linkType: number) => {
  const texts = ['不关联', '关联宿舍', '关联教室']
  return texts[linkType] || texts[0]
}

// 切换类别展开状态
const toggleCategoryExpand = async (categoryId: number) => {
  if (expandedCategories.value.has(categoryId)) {
    expandedCategories.value.delete(categoryId)
  } else {
    expandedCategories.value.add(categoryId)
    // 加载扣分项
    if (!deductionItemsCache.value[categoryId]) {
      try {
        const items = await getEnabledDeductionItemsByTypeId(categoryId)
        deductionItemsCache.value[categoryId] = items || []
      } catch (error) {
        console.error('加载扣分项失败', error)
        deductionItemsCache.value[categoryId] = []
      }
    }
  }
}

// 获取扣分模式文本
const getDeductModeText = (mode: number) => {
  const modeMap: Record<number, string> = {
    1: '固定扣分',
    2: '区间扣分',
    3: '按人次扣分'
  }
  return modeMap[mode] || '未知'
}

// 加载检查类别列表
const loadCategories = async () => {
  try {
    const res = await getAllEnabledTypes()
    availableCategories.value = res || []
  } catch (error: any) {
    ElMessage.error(error.message || '加载检查类别失败')
  }
}

// 查询模板列表
const handleQuery = async () => {
  loading.value = true
  try {
    const res = await getCheckTemplatePage(queryForm)
    templateList.value = res.records || []
    total.value = Number(res.total) || 0
  } catch (error: any) {
    ElMessage.error(error.message || '查询失败')
  } finally {
    loading.value = false
  }
}

// 重置查询
const handleReset = () => {
  queryForm.templateName = ''
  queryForm.status = undefined
  queryForm.pageNum = 1
  handleQuery()
}

// 新增
const handleAdd = () => {
  editMode.value = 'add'
  dialogTitle.value = '新增检查模板'
  resetForm()
  dialogVisible.value = true
}

// 编辑
const handleEdit = async (row: CheckTemplateResponse) => {
  editMode.value = 'edit'
  dialogTitle.value = '编辑检查模板'

  try {
    const data = await getCheckTemplateById(row.id)
    formData.id = data.id
    formData.templateName = data.templateName
    formData.templateCode = data.templateCode
    formData.description = data.description || ''
    formData.isDefault = data.isDefault
    formData.status = data.status
    formData.categories = (data.categories || []).map(cat => ({
      categoryId: cat.categoryId,
      linkType: cat.linkType,
      sortOrder: cat.sortOrder,
      isRequired: cat.isRequired
    }))
    dialogVisible.value = true
  } catch (error: any) {
    ElMessage.error(error.message || '加载模板详情失败')
  }
}

// 查看详情
const handleView = async (row: CheckTemplateResponse) => {
  try {
    viewData.value = await getCheckTemplateById(row.id)
    viewDialogVisible.value = true
  } catch (error: any) {
    ElMessage.error(error.message || '加载模板详情失败')
  }
}

// 删除
const handleDelete = (row: CheckTemplateResponse) => {
  ElMessageBox.confirm(
    `确定要删除模板"${row.templateName}"吗?`,
    '提示',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      await deleteCheckTemplate(row.id)
      ElMessage.success('删除成功')
      handleQuery()
    } catch (error: any) {
      ElMessage.error(error.message || '删除失败')
    }
  }).catch(() => {})
}

// 下一步 - 验证第一步必填项
const handleNextStep = () => {
  if (!formData.templateName?.trim()) {
    ElMessage.error('请输入模板名称')
    return
  }
  if (!formData.templateCode?.trim()) {
    ElMessage.error('请输入模板编码')
    return
  }
  dialogStep.value = 2
}

// 移除类别
const handleRemoveCategory = (index: number) => {
  formData.categories.splice(index, 1)
  formData.categories.forEach((item, i) => {
    item.sortOrder = i + 1
  })
}

// 提交表单
const handleSubmit = async () => {
  if (!formData.templateName) {
    ElMessage.error('请输入模板名称')
    return
  }
  if (!formData.templateCode) {
    ElMessage.error('请输入模板编码')
    return
  }
  if (formData.categories.length === 0) {
    ElMessage.error('请至少添加一个检查类别')
    return
  }

  submitLoading.value = true
  try {
    const submitData = {
      templateName: formData.templateName,
      templateCode: formData.templateCode,
      description: formData.description,
      isDefault: formData.isDefault,
      status: formData.status,
      categories: formData.categories
    }

    if (editMode.value === 'add') {
      await createCheckTemplate(submitData)
      ElMessage.success('新增成功')
    } else {
      await updateCheckTemplate(formData.id!, { ...submitData, id: formData.id! })
      ElMessage.success('更新成功')
    }

    dialogVisible.value = false
    handleQuery()
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  } finally {
    submitLoading.value = false
  }
}

// 重置表单
const resetForm = () => {
  formData.id = undefined
  formData.templateName = ''
  formData.templateCode = ''
  formData.description = ''
  formData.isDefault = 0
  formData.status = 1
  formData.categories = []
  // 清理扣分项相关状态
  expandedCategories.value.clear()
  deductionItemsCache.value = {}
  // 重置对话框状态
  dialogStep.value = 1
}

// 初始化
onMounted(() => {
  loadCategories()
  handleQuery()
})
</script>

<style scoped>
@keyframes fade-in {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.animate-fade-in {
  animation: fade-in 0.3s ease-out forwards;
  opacity: 0;
}
</style>
