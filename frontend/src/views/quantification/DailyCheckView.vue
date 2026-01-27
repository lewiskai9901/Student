<template>
  <div class="min-h-screen bg-gray-50 p-6">
    <!-- 统计卡片 -->
    <div class="mb-6 grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
      <StatCard
        title="检查总数"
        :value="stats.total"
        :icon="ClipboardList"
        subtitle="全部检查"
        color="blue"
      />
      <StatCard
        title="进行中"
        :value="stats.inProgress"
        :icon="Clock"
        subtitle="正在检查"
        color="orange"
      />
      <StatCard
        title="已完成"
        :value="stats.completed"
        :icon="CheckCircle2"
        subtitle="检查完成"
        color="emerald"
      />
      <StatCard
        title="已发布"
        :value="stats.published"
        :icon="Send"
        subtitle="结果发布"
        color="purple"
      />
    </div>

    <!-- 搜索栏 -->
    <div class="mb-6 rounded-lg border border-gray-200 bg-white p-4">
      <div class="flex flex-wrap items-end gap-4">
        <div class="w-40">
          <label class="mb-1 block text-sm text-gray-600">检查日期</label>
          <input
            v-model="searchForm.checkDate"
            type="date"
            class="h-9 w-full rounded-lg border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none"
          />
        </div>
        <div class="w-44">
          <label class="mb-1 block text-sm text-gray-600">检查名称</label>
          <input
            v-model="searchForm.checkName"
            type="text"
            placeholder="请输入检查名称"
            class="h-9 w-full rounded-lg border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none"
            @keyup.enter="handleSearch"
          />
        </div>
        <div class="w-32">
          <label class="mb-1 block text-sm text-gray-600">状态</label>
          <select
            v-model="searchForm.status"
            class="h-9 w-full rounded-lg border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none"
          >
            <option :value="undefined">全部</option>
            <option :value="0">未开始</option>
            <option :value="1">进行中</option>
            <option :value="2">已完成</option>
            <option :value="3">已发布</option>
          </select>
        </div>
        <div class="flex gap-2">
          <button
            @click="handleSearch"
            class="inline-flex h-9 items-center gap-1.5 rounded-lg bg-blue-600 px-4 text-sm font-medium text-white hover:bg-blue-700"
          >
            <Search class="h-4 w-4" />
            查询
          </button>
          <button
            @click="handleReset"
            class="inline-flex h-9 items-center gap-1.5 rounded-lg border border-gray-300 px-4 text-sm font-medium text-gray-700 hover:bg-gray-50"
          >
            <RotateCcw class="h-4 w-4" />
            重置
          </button>
        </div>
        <div class="ml-auto">
          <button
            v-if="hasPermission('quantification:check:add')"
            @click="handleAdd"
            class="inline-flex h-9 items-center gap-1.5 rounded-lg bg-blue-600 px-4 text-sm font-medium text-white hover:bg-blue-700"
          >
            <Plus class="h-4 w-4" />
            新增检查
          </button>
        </div>
      </div>
    </div>

    <!-- 表格 -->
    <div class="rounded-lg border border-gray-200 bg-white">
      <div class="flex items-center justify-between border-b border-gray-200 px-4 py-3">
        <div class="flex items-center gap-2">
          <span class="font-medium text-gray-900">检查列表</span>
          <span class="rounded bg-gray-100 px-2 py-0.5 text-xs text-gray-600">{{ pagination.total }} 条</span>
        </div>
      </div>

      <table class="w-full">
        <thead>
          <tr class="border-b border-gray-200 bg-gray-50">
            <th class="px-4 py-3 text-left text-sm font-medium text-gray-900">ID</th>
            <th class="px-4 py-3 text-left text-sm font-medium text-gray-900">检查日期</th>
            <th class="px-4 py-3 text-left text-sm font-medium text-gray-900">检查名称</th>
            <th class="px-4 py-3 text-left text-sm font-medium text-gray-900">使用模板</th>
            <th class="px-4 py-3 text-center text-sm font-medium text-gray-900">类型</th>
            <th class="px-4 py-3 text-center text-sm font-medium text-gray-900">状态</th>
            <th class="px-4 py-3 text-left text-sm font-medium text-gray-900">创建时间</th>
            <th class="px-4 py-3 text-center text-sm font-medium text-gray-900">操作</th>
          </tr>
        </thead>
        <tbody v-if="loading">
          <tr>
            <td colspan="8" class="py-16 text-center">
              <Loader2 class="mx-auto h-8 w-8 animate-spin text-blue-600" />
              <div class="mt-2 text-sm text-gray-500">加载中...</div>
            </td>
          </tr>
        </tbody>
        <tbody v-else-if="checkList.length === 0">
          <tr>
            <td colspan="8" class="py-16 text-center">
              <ClipboardCheck class="mx-auto h-12 w-12 text-gray-300" />
              <div class="mt-2 text-sm text-gray-500">暂无数据</div>
            </td>
          </tr>
        </tbody>
        <tbody v-else>
          <tr v-for="row in checkList" :key="row.id" class="border-b border-gray-100 hover:bg-gray-50">
            <td class="px-4 py-3 text-gray-600">{{ row.id }}</td>
            <td class="px-4 py-3 text-gray-900">{{ row.checkDate }}</td>
            <td class="px-4 py-3">
              <div class="flex items-center gap-2">
                <div class="flex h-7 w-7 items-center justify-center rounded-lg bg-blue-100">
                  <ClipboardCheck class="h-4 w-4 text-blue-600" />
                </div>
                <span class="font-medium text-gray-900">{{ row.checkName }}</span>
              </div>
            </td>
            <td class="px-4 py-3 text-gray-600">{{ row.templateName || '自定义' }}</td>
            <td class="px-4 py-3 text-center">
              <span :class="['rounded-full px-2 py-0.5 text-xs font-medium', row.checkType === 1 ? 'bg-blue-100 text-blue-700' : 'bg-purple-100 text-purple-700']">
                {{ row.checkType === 1 ? '日常检查' : '专项检查' }}
              </span>
            </td>
            <td class="px-4 py-3 text-center">
              <span :class="['rounded-full px-2 py-0.5 text-xs font-medium', getStatusClass(row.status)]">
                {{ getStatusText(row.status) }}
              </span>
            </td>
            <td class="px-4 py-3 text-sm text-gray-500">{{ row.createdAt }}</td>
            <td class="px-4 py-3">
              <div class="flex items-center justify-center gap-1">
                <button v-if="hasPermission('quantification:check:view')" @click="handleView(row)" class="rounded p-1.5 text-gray-500 hover:bg-blue-50 hover:text-blue-600" title="查看">
                  <Eye class="h-4 w-4" />
                </button>
                <button v-if="hasPermission('quantification:record:add') && row.status === 1" @click="handleScoring(row)" class="rounded p-1.5 text-gray-500 hover:bg-amber-50 hover:text-amber-600" title="打分">
                  <Star class="h-4 w-4" />
                </button>
                <button v-if="hasPermission('quantification:check:edit') && row.status !== 3" @click="handleEdit(row)" class="rounded p-1.5 text-gray-500 hover:bg-blue-50 hover:text-blue-600" title="编辑">
                  <Pencil class="h-4 w-4" />
                </button>
                <button v-if="hasPermission('quantification:check:edit') && row.status === 0" @click="handleStart(row)" class="rounded p-1.5 text-gray-500 hover:bg-green-50 hover:text-green-600" title="开始">
                  <Play class="h-4 w-4" />
                </button>
                <button v-if="hasPermission('quantification:check:edit') && row.status === 1" @click="handleFinish(row)" class="rounded p-1.5 text-gray-500 hover:bg-orange-50 hover:text-orange-600" title="结束">
                  <Square class="h-4 w-4" />
                </button>
                <button v-if="hasPermission('quantification:check:delete') && row.status !== 3" @click="handleDelete(row)" class="rounded p-1.5 text-gray-500 hover:bg-red-50 hover:text-red-600" title="删除">
                  <Trash2 class="h-4 w-4" />
                </button>
                <button v-if="hasPermission('quantification:check:view') && row.planId" @click="handleExport(row)" class="rounded p-1.5 text-gray-500 hover:bg-purple-50 hover:text-purple-600" title="导出">
                  <Download class="h-4 w-4" />
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>

      <!-- 分页 -->
      <div class="flex items-center justify-between border-t border-gray-200 px-4 py-3">
        <div class="text-sm text-gray-500">
          共 {{ pagination.total }} 条，第 {{ pagination.pageNum }} / {{ Math.ceil(pagination.total / pagination.pageSize) || 1 }} 页
        </div>
        <div class="flex items-center gap-2">
          <select v-model="pagination.pageSize" @change="handleSizeChange" class="h-8 rounded border border-gray-300 px-2 text-sm">
            <option v-for="size in [10, 20, 50, 100]" :key="size" :value="size">{{ size }}条/页</option>
          </select>
          <div class="flex gap-1">
            <button @click="pagination.pageNum = 1; loadCheckList()" :disabled="pagination.pageNum <= 1" class="rounded border border-gray-300 p-1.5 disabled:opacity-50 hover:bg-gray-50">
              <ChevronsLeft class="h-4 w-4" />
            </button>
            <button @click="pagination.pageNum--; loadCheckList()" :disabled="pagination.pageNum <= 1" class="rounded border border-gray-300 p-1.5 disabled:opacity-50 hover:bg-gray-50">
              <ChevronLeft class="h-4 w-4" />
            </button>
            <button @click="pagination.pageNum++; loadCheckList()" :disabled="pagination.pageNum >= Math.ceil(pagination.total / pagination.pageSize)" class="rounded border border-gray-300 p-1.5 disabled:opacity-50 hover:bg-gray-50">
              <ChevronRight class="h-4 w-4" />
            </button>
            <button @click="pagination.pageNum = Math.ceil(pagination.total / pagination.pageSize); loadCheckList()" :disabled="pagination.pageNum >= Math.ceil(pagination.total / pagination.pageSize)" class="rounded border border-gray-300 p-1.5 disabled:opacity-50 hover:bg-gray-50">
              <ChevronsRight class="h-4 w-4" />
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 新增/编辑对话框 -->
    <Teleport to="body">
      <Transition name="modal">
        <div v-if="dialogVisible" class="fixed inset-0 z-50 flex items-center justify-center">
          <div class="fixed inset-0 bg-black/50" @click="dialogVisible = false"></div>
          <div class="relative w-full max-w-4xl rounded-lg bg-white shadow-xl">
            <div class="flex items-center justify-between border-b border-gray-200 px-6 py-4">
              <h3 class="text-lg font-medium text-gray-900">{{ dialogTitle }}</h3>
              <button @click="dialogVisible = false" class="rounded p-1 hover:bg-gray-100">
                <X class="h-5 w-5 text-gray-500" />
              </button>
            </div>
            <div class="max-h-[70vh] overflow-y-auto p-6">
              <!-- 步骤指示器 -->
              <div class="mb-6 flex items-center justify-center gap-4">
                <div class="flex items-center gap-2">
                  <div :class="['flex h-7 w-7 items-center justify-center rounded-full text-sm font-medium', formStep === 1 ? 'bg-blue-600 text-white' : 'bg-blue-100 text-blue-600']">1</div>
                  <span :class="['text-sm', formStep === 1 ? 'font-medium text-gray-900' : 'text-gray-500']">选择模板</span>
                </div>
                <div class="h-px w-12 bg-gray-300"></div>
                <div class="flex items-center gap-2">
                  <div :class="['flex h-7 w-7 items-center justify-center rounded-full text-sm font-medium', formStep === 2 ? 'bg-blue-600 text-white' : formStep > 2 ? 'bg-blue-100 text-blue-600' : 'bg-gray-200 text-gray-500']">2</div>
                  <span :class="['text-sm', formStep === 2 ? 'font-medium text-gray-900' : 'text-gray-500']">设置检查</span>
                </div>
                <div class="h-px w-12 bg-gray-300"></div>
                <div class="flex items-center gap-2">
                  <div :class="['flex h-7 w-7 items-center justify-center rounded-full text-sm font-medium', formStep === 3 ? 'bg-blue-600 text-white' : 'bg-gray-200 text-gray-500']">3</div>
                  <span :class="['text-sm', formStep === 3 ? 'font-medium text-gray-900' : 'text-gray-500']">选择目标</span>
                </div>
              </div>

              <!-- 步骤1：选择计划和模板 -->
              <div v-if="formStep === 1" class="space-y-4">
                <!-- 选择检查计划（可选） -->
                <div class="rounded-lg border border-blue-200 bg-blue-50 p-3">
                  <div class="flex items-start gap-2 text-sm text-blue-700">
                    <Info class="mt-0.5 h-4 w-4 flex-shrink-0" />
                    <span>可选择关联的检查计划，选择后将限制检查目标为计划范围内的班级</span>
                  </div>
                </div>

                <div>
                  <label class="mb-1.5 block text-sm font-medium text-gray-700">检查计划（可选）</label>
                  <div v-if="planList.length === 0" class="rounded-lg border border-dashed border-gray-300 py-4 text-center">
                    <p class="text-sm text-gray-500">暂无进行中的检查计划</p>
                    <p class="mt-1 text-xs text-gray-400">可跳过此步骤，直接选择模板创建检查</p>
                  </div>
                  <div v-else class="grid grid-cols-2 gap-2">
                    <div
                      @click="handleSelectPlan(null)"
                      :class="['cursor-pointer rounded-lg border-2 p-3 transition-all hover:shadow-md', !selectedPlanId ? 'border-gray-500 bg-gray-50' : 'border-gray-200 hover:border-gray-300']"
                    >
                      <div class="text-sm font-medium text-gray-700">不关联计划</div>
                      <div class="text-xs text-gray-500">可选择任意班级作为检查目标</div>
                    </div>
                    <div
                      v-for="plan in planList"
                      :key="plan.id"
                      @click="handleSelectPlan(plan)"
                      :class="['cursor-pointer rounded-lg border-2 p-3 transition-all hover:shadow-md', selectedPlanId === plan.id ? 'border-blue-500 bg-blue-50' : 'border-gray-200 hover:border-blue-300']"
                    >
                      <div class="flex items-center justify-between">
                        <span class="text-sm font-medium text-gray-900">{{ plan.planName }}</span>
                        <span class="rounded bg-green-100 px-1.5 py-0.5 text-xs text-green-700">进行中</span>
                      </div>
                      <div class="mt-1 text-xs text-gray-500">{{ plan.startDate }} ~ {{ plan.endDate }}</div>
                    </div>
                  </div>
                </div>

                <div class="border-t border-gray-200 pt-4">
                  <div class="rounded-lg border border-amber-200 bg-amber-50 p-3">
                    <div class="flex items-start gap-2 text-sm text-amber-700">
                      <AlertCircle class="mt-0.5 h-4 w-4 flex-shrink-0" />
                      <span>请选择检查模板，模板定义了检查类别、扣分项和加权方案</span>
                    </div>
                  </div>
                </div>

                <div v-if="templateList.length === 0" class="rounded-lg border border-dashed border-gray-300 py-12 text-center">
                  <FileText class="mx-auto h-12 w-12 text-gray-300" />
                  <p class="mt-2 text-sm text-gray-500">暂无可用模板</p>
                  <p class="mt-1 text-xs text-gray-400">请先在"检查模板"中创建模板</p>
                </div>

                <div v-else class="grid grid-cols-2 gap-3">
                  <div
                    v-for="tpl in templateList"
                    :key="tpl.id"
                    @click="handleSelectTemplate(tpl)"
                    :class="['cursor-pointer rounded-lg border-2 p-4 transition-all hover:shadow-md', formData.templateId === tpl.id ? 'border-blue-500 bg-blue-50' : 'border-gray-200 hover:border-blue-300']"
                  >
                    <div class="flex items-start justify-between">
                      <div class="flex items-center gap-2">
                        <div :class="['flex h-8 w-8 items-center justify-center rounded-lg', formData.templateId === tpl.id ? 'bg-blue-600' : 'bg-gray-100']">
                          <FileText :class="['h-4 w-4', formData.templateId === tpl.id ? 'text-white' : 'text-gray-500']" />
                        </div>
                        <div>
                          <div class="font-medium text-gray-900">{{ tpl.templateName }}</div>
                          <div class="text-xs text-gray-500">{{ tpl.templateCode }}</div>
                        </div>
                      </div>
                      <div v-if="tpl.isDefault === 1" class="rounded bg-green-100 px-1.5 py-0.5 text-xs text-green-700">默认</div>
                    </div>
                    <p v-if="tpl.description" class="mt-2 text-xs text-gray-500 line-clamp-2">{{ tpl.description }}</p>
                    <div v-if="tpl.categories && tpl.categories.length > 0" class="mt-3 flex flex-wrap gap-1">
                      <span v-for="cat in tpl.categories.slice(0, 4)" :key="cat.categoryId" class="rounded bg-gray-100 px-1.5 py-0.5 text-xs text-gray-600">
                        {{ cat.categoryName }}
                      </span>
                      <span v-if="tpl.categories.length > 4" class="rounded bg-gray-100 px-1.5 py-0.5 text-xs text-gray-600">+{{ tpl.categories.length - 4 }}</span>
                    </div>
                  </div>
                </div>
              </div>

              <!-- 步骤2：设置检查信息 -->
              <div v-if="formStep === 2" class="space-y-4">
                <!-- 已选模板信息 -->
                <div class="rounded-lg border border-gray-200 bg-gray-50 p-4">
                  <div class="flex items-center justify-between">
                    <div class="flex items-center gap-3">
                      <div class="flex h-10 w-10 items-center justify-center rounded-lg bg-blue-600">
                        <FileText class="h-5 w-5 text-white" />
                      </div>
                      <div>
                        <div class="font-medium text-gray-900">{{ selectedTemplate?.templateName }}</div>
                        <div class="text-xs text-gray-500">包含 {{ selectedTemplate?.categories?.length || 0 }} 个检查类别</div>
                      </div>
                    </div>
                    <button @click="formStep = 1" class="text-sm text-blue-600 hover:text-blue-700">更换模板</button>
                  </div>
                </div>

                <div class="grid grid-cols-2 gap-4">
                  <div>
                    <label class="mb-1.5 block text-sm font-medium text-gray-700">检查日期 <span class="text-red-500">*</span></label>
                    <input v-model="formData.checkDate" @change="autoGenerateCheckName" type="date" class="h-10 w-full rounded-lg border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500" />
                  </div>
                  <div>
                    <label class="mb-1.5 block text-sm font-medium text-gray-700">检查类型</label>
                    <div class="flex h-10 items-center gap-6">
                      <label class="flex cursor-pointer items-center gap-2">
                        <input v-model="formData.checkType" @change="autoGenerateCheckName" type="radio" :value="1" class="h-4 w-4 text-blue-600" />
                        <span class="text-sm text-gray-700">日常检查</span>
                      </label>
                      <label class="flex cursor-pointer items-center gap-2">
                        <input v-model="formData.checkType" @change="autoGenerateCheckName" type="radio" :value="2" class="h-4 w-4 text-blue-600" />
                        <span class="text-sm text-gray-700">专项检查</span>
                      </label>
                    </div>
                  </div>
                </div>

                <div>
                  <label class="mb-1.5 block text-sm font-medium text-gray-700">检查名称 <span class="text-red-500">*</span></label>
                  <input v-model="formData.checkName" type="text" placeholder="系统自动生成，可手动修改" class="h-10 w-full rounded-lg border border-gray-300 px-3 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500" />
                </div>

                <div>
                  <label class="mb-1.5 block text-sm font-medium text-gray-700">检查说明</label>
                  <textarea v-model="formData.description" rows="2" placeholder="可选，填写本次检查的特殊说明" class="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"></textarea>
                </div>

                <!-- 模板检查类别配置 -->
                <div>
                  <label class="mb-1.5 block text-sm font-medium text-gray-700">检查类别配置</label>
                  <div class="rounded-lg border border-gray-200 bg-white">
                    <div v-if="formData.categories && formData.categories.length > 0" class="divide-y divide-gray-100">
                      <div v-for="(cat, idx) in formData.categories" :key="cat.categoryId" class="flex items-center justify-between px-4 py-2.5">
                        <div class="flex items-center gap-3">
                          <span class="flex h-6 w-6 items-center justify-center rounded bg-blue-100 text-xs font-medium text-blue-700">{{ idx + 1 }}</span>
                          <span class="text-sm text-gray-900">{{ cat.categoryName }}</span>
                        </div>
                        <div class="flex items-center gap-2">
                          <span v-if="cat.linkType === 1" class="rounded bg-purple-100 px-1.5 py-0.5 text-xs text-purple-700">关联宿舍</span>
                          <span v-else-if="cat.linkType === 2" class="rounded bg-green-100 px-1.5 py-0.5 text-xs text-green-700">关联教室</span>
                          <span v-if="cat.isRequired === 1" class="rounded bg-red-100 px-1.5 py-0.5 text-xs text-red-700">必检</span>
                        </div>
                      </div>
                    </div>
                    <div v-else class="py-6 text-center text-sm text-gray-500">请先选择检查模板</div>
                  </div>
                </div>
              </div>

              <!-- 步骤3：选择检查目标 -->
              <div v-if="formStep === 3" class="space-y-4">
                <div class="rounded-lg border border-blue-200 bg-blue-50 p-3">
                  <div class="flex items-start gap-2 text-sm text-blue-700">
                    <Info class="mt-0.5 h-4 w-4 flex-shrink-0" />
                    <span>选择本次检查的目标范围，可以是具体班级、某个年级的所有班级或某个院系的所有班级</span>
                  </div>
                </div>

                <!-- 目标类型选择 -->
                <div>
                  <label class="mb-1.5 block text-sm font-medium text-gray-700">目标类型</label>
                  <div class="flex gap-2">
                    <button
                      v-for="type in targetTypes"
                      :key="type.value"
                      @click="currentTargetType = type.value"
                      :class="['rounded-lg border px-4 py-2 text-sm font-medium transition-colors', currentTargetType === type.value ? 'border-blue-500 bg-blue-50 text-blue-700' : 'border-gray-300 text-gray-700 hover:bg-gray-50']"
                    >
                      {{ type.label }}
                    </button>
                  </div>
                </div>

                <!-- 计划目标范围提示 -->
                <div v-if="selectedPlanId && planTargetClasses.length > 0" class="rounded-lg border border-green-200 bg-green-50 p-3">
                  <div class="flex items-start gap-2 text-sm text-green-700">
                    <Check class="mt-0.5 h-4 w-4 flex-shrink-0" />
                    <span>已关联检查计划，可选目标已限制在计划范围内（{{ planTargetClasses.length }} 个班级）</span>
                  </div>
                </div>

                <!-- 目标选择列表 -->
                <div class="rounded-lg border border-gray-200">
                  <div class="border-b border-gray-200 bg-gray-50 px-4 py-2">
                    <span class="text-sm font-medium text-gray-700">
                      {{ currentTargetType === 1 ? '班级列表' : currentTargetType === 2 ? '年级列表' : '院系列表' }}
                      <span v-if="selectedPlanId" class="ml-2 text-xs text-blue-600">（计划范围内）</span>
                    </span>
                  </div>
                  <div class="max-h-48 overflow-y-auto p-2">
                    <!-- 班级选择 -->
                    <div v-if="currentTargetType === 1" class="grid grid-cols-3 gap-2">
                      <label
                        v-for="cls in availableClasses"
                        :key="cls.id"
                        :class="['flex cursor-pointer items-center gap-2 rounded-lg border p-2 text-sm transition-colors', isTargetSelected(1, cls.id) ? 'border-blue-500 bg-blue-50' : 'border-gray-200 hover:bg-gray-50']"
                      >
                        <input type="checkbox" :checked="isTargetSelected(1, cls.id)" @change="toggleTarget(1, cls.id, cls.className)" class="h-4 w-4 rounded text-blue-600" />
                        <span class="truncate">{{ cls.className }}</span>
                      </label>
                      <div v-if="availableClasses.length === 0" class="col-span-3 py-4 text-center text-sm text-gray-500">
                        暂无可选班级
                      </div>
                    </div>
                    <!-- 年级选择 -->
                    <div v-else-if="currentTargetType === 2" class="grid grid-cols-4 gap-2">
                      <label
                        v-for="grade in availableGrades"
                        :key="grade.id"
                        :class="['flex cursor-pointer items-center gap-2 rounded-lg border p-2 text-sm transition-colors', isTargetSelected(2, grade.id) ? 'border-blue-500 bg-blue-50' : 'border-gray-200 hover:bg-gray-50']"
                      >
                        <input type="checkbox" :checked="isTargetSelected(2, grade.id)" @change="toggleTarget(2, grade.id, grade.gradeName)" class="h-4 w-4 rounded text-blue-600" />
                        <span>{{ grade.gradeName }}</span>
                      </label>
                      <div v-if="availableGrades.length === 0" class="col-span-4 py-4 text-center text-sm text-gray-500">
                        暂无可选年级
                      </div>
                    </div>
                    <!-- 院系选择 -->
                    <div v-else class="grid grid-cols-2 gap-2">
                      <label
                        v-for="dept in availableDepartments"
                        :key="dept.id"
                        :class="['flex cursor-pointer items-center gap-2 rounded-lg border p-2 text-sm transition-colors', isTargetSelected(3, dept.id) ? 'border-blue-500 bg-blue-50' : 'border-gray-200 hover:bg-gray-50']"
                      >
                        <input type="checkbox" :checked="isTargetSelected(3, dept.id)" @change="toggleTarget(3, dept.id, dept.deptName)" class="h-4 w-4 rounded text-blue-600" />
                        <span class="truncate">{{ dept.deptName }}</span>
                      </label>
                      <div v-if="availableDepartments.length === 0" class="col-span-2 py-4 text-center text-sm text-gray-500">
                        暂无可选院系
                      </div>
                    </div>
                  </div>
                </div>

                <!-- 已选目标 -->
                <div>
                  <div class="mb-1.5 flex items-center justify-between">
                    <label class="text-sm font-medium text-gray-700">已选目标 <span class="text-red-500">*</span></label>
                    <span class="text-xs text-gray-500">共 {{ formData.targets.length }} 个目标</span>
                  </div>
                  <div v-if="formData.targets.length === 0" class="rounded-lg border border-dashed border-gray-300 py-6 text-center text-sm text-gray-500">
                    请从上方选择检查目标
                  </div>
                  <div v-else class="space-y-3">
                    <div class="flex flex-wrap gap-2">
                      <div
                        v-for="(target, index) in formData.targets"
                        :key="index"
                        class="flex items-center gap-1.5 rounded-full border border-gray-200 bg-gray-50 py-1 pl-3 pr-1.5"
                      >
                        <span :class="['h-1.5 w-1.5 rounded-full', target.targetType === 1 ? 'bg-blue-500' : target.targetType === 2 ? 'bg-green-500' : 'bg-purple-500']"></span>
                        <span class="text-sm text-gray-700">{{ target.targetName }}</span>
                        <button @click="handleRemoveTarget(index)" class="rounded-full p-0.5 text-gray-400 hover:bg-gray-200 hover:text-gray-600">
                          <X class="h-3.5 w-3.5" />
                        </button>
                      </div>
                    </div>
                    <!-- 班级数量统计 -->
                    <div class="rounded-lg border border-green-200 bg-green-50 px-4 py-2">
                      <div class="flex items-center gap-2 text-sm text-green-700">
                        <Check class="h-4 w-4" />
                        <span>实际涉及 <strong>{{ selectedClassCount }}</strong> 个班级（已去重）</span>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <!-- 底部按钮 -->
            <div class="flex items-center justify-between border-t border-gray-200 px-6 py-4">
              <div>
                <button v-if="formStep > 1" @click="formStep--" class="inline-flex h-9 items-center gap-1.5 rounded-lg border border-gray-300 px-4 text-sm font-medium text-gray-700 hover:bg-gray-50">
                  <ChevronLeft class="h-4 w-4" />
                  上一步
                </button>
              </div>
              <div class="flex gap-3">
                <button @click="dialogVisible = false" class="h-9 rounded-lg border border-gray-300 px-4 text-sm font-medium text-gray-700 hover:bg-gray-50">取消</button>
                <button v-if="formStep < 3" @click="handleNextStep" :disabled="!canNextStep" class="inline-flex h-9 items-center gap-1.5 rounded-lg bg-blue-600 px-4 text-sm font-medium text-white hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-50">
                  下一步
                  <ChevronRight class="h-4 w-4" />
                </button>
                <button v-else @click="handleSubmit" :disabled="submitLoading || formData.targets.length === 0" class="inline-flex h-9 items-center gap-1.5 rounded-lg bg-blue-600 px-4 text-sm font-medium text-white hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-50">
                  <Loader2 v-if="submitLoading" class="h-4 w-4 animate-spin" />
                  <Check v-else class="h-4 w-4" />
                  {{ currentEditId ? '保存修改' : '创建检查' }}
                </button>
              </div>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- 查看详情对话框 -->
    <Teleport to="body">
      <Transition name="modal">
        <div v-if="detailVisible" class="fixed inset-0 z-50 flex items-center justify-center">
          <div class="fixed inset-0 bg-black/50" @click="detailVisible = false"></div>
          <div class="relative w-full max-w-2xl rounded-lg bg-white shadow-xl">
            <div class="flex items-center justify-between border-b border-gray-200 px-6 py-4">
              <h3 class="text-lg font-medium text-gray-900">检查详情</h3>
              <button @click="detailVisible = false" class="rounded p-1 hover:bg-gray-100">
                <X class="h-5 w-5 text-gray-500" />
              </button>
            </div>
            <div class="max-h-[60vh] overflow-y-auto p-6">
              <div class="grid grid-cols-2 gap-4">
                <div class="rounded-lg bg-gray-50 p-3">
                  <div class="text-sm text-gray-500">检查名称</div>
                  <div class="mt-1 font-medium text-gray-900">{{ detailData.checkName }}</div>
                </div>
                <div class="rounded-lg bg-gray-50 p-3">
                  <div class="text-sm text-gray-500">检查日期</div>
                  <div class="mt-1 font-medium text-gray-900">{{ detailData.checkDate }}</div>
                </div>
                <div class="rounded-lg bg-gray-50 p-3">
                  <div class="text-sm text-gray-500">检查类型</div>
                  <div class="mt-1 font-medium text-gray-900">{{ detailData.checkType === 1 ? '日常检查' : '专项检查' }}</div>
                </div>
                <div class="rounded-lg bg-gray-50 p-3">
                  <div class="text-sm text-gray-500">状态</div>
                  <div class="mt-1">
                    <span :class="['rounded-full px-2 py-0.5 text-xs font-medium', getStatusClass(detailData.status)]">{{ getStatusText(detailData.status) }}</span>
                  </div>
                </div>
                <div class="col-span-2 rounded-lg bg-gray-50 p-3">
                  <div class="text-sm text-gray-500">使用模板</div>
                  <div class="mt-1 font-medium text-gray-900">{{ detailData.templateName || '自定义' }}</div>
                </div>
                <div class="col-span-2 rounded-lg bg-gray-50 p-3">
                  <div class="text-sm text-gray-500">检查说明</div>
                  <div class="mt-1 font-medium text-gray-900">{{ detailData.description || '无' }}</div>
                </div>
              </div>
              <div class="mt-6">
                <h4 class="mb-3 font-medium text-gray-900">检查目标</h4>
                <table class="w-full rounded-lg border border-gray-200">
                  <thead>
                    <tr class="border-b border-gray-200 bg-gray-50">
                      <th class="px-4 py-2 text-left text-sm font-medium text-gray-900">目标类型</th>
                      <th class="px-4 py-2 text-left text-sm font-medium text-gray-900">目标名称</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="target in detailData.targets" :key="target.targetId" class="border-b border-gray-100">
                      <td class="px-4 py-2 text-gray-600">{{ target.targetType === 1 ? '班级' : target.targetType === 2 ? '年级' : '院系' }}</td>
                      <td class="px-4 py-2 text-gray-900">{{ target.targetName }}</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- 导出对话框 -->
    <ExportDialog
      v-model="showExportDialog"
      :check-id="exportCheckId"
      @createTemplate="goToCreateTemplate"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ClipboardCheck, Plus, Search, RotateCcw, Loader2, ChevronLeft, ChevronRight, ChevronsLeft, ChevronsRight,
  Eye, Star, Pencil, Play, Square, Trash2, X, Info, AlertCircle, FileText, Check, Download
} from 'lucide-vue-next'
import ExportDialog from './components/ExportDialog.vue'
import {
  getDailyCheckPage, createDailyCheck, updateDailyCheck, deleteDailyCheck, getDailyCheckById, updateCheckStatus,
  type DailyCheckResponse, type DailyCheckCreateRequest, type CheckTargetItem, type CheckCategoryItem
} from '@/api/v2/quantification'
import { getAllCheckTemplates, getCheckTemplateById, type CheckTemplateResponse } from '@/api/v2/quantification'
import { getClassList, type Class } from '@/api/v2/organization'
import { getAllEnabledDepartments, type DepartmentResponse } from '@/api/v2/organization'
import { getAllGrades, type Grade } from '@/api/v2/organization'
import { getCheckPlanPage, getPlanTargetClasses, type CheckPlanListVO } from '@/api/v2/quantification'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const hasPermission = (permission: string) => authStore.hasPermission(permission)

const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const detailVisible = ref(false)
const dialogTitle = ref('')
const currentEditId = ref<number>()
const formStep = ref(1)
const currentTargetType = ref(1)

const targetTypes = [
  { value: 1, label: '按班级' },
  { value: 2, label: '按年级' },
  { value: 3, label: '按院系' }
]

const searchForm = reactive({ checkDate: '', checkName: '', status: undefined as number | undefined })
const pagination = reactive({ pageNum: 1, pageSize: 10, total: 0 })

const checkList = ref<DailyCheckResponse[]>([])
const templateList = ref<CheckTemplateResponse[]>([])
const classList = ref<Class[]>([])
const gradeList = ref<Grade[]>([])
const departmentList = ref<DepartmentResponse[]>([])
const detailData = ref<DailyCheckResponse>({} as DailyCheckResponse)
// 检查计划相关
const planList = ref<CheckPlanListVO[]>([])
const selectedPlanId = ref<number | string | undefined>(undefined)
const planTargetClasses = ref<Class[]>([]) // 计划目标范围内的班级

const stats = computed(() => ({
  total: pagination.total,
  inProgress: checkList.value.filter(c => c.status === 1).length,
  completed: checkList.value.filter(c => c.status === 2).length,
  published: checkList.value.filter(c => c.status === 3).length
}))

const selectedTemplate = computed(() => templateList.value.find(t => t.id === formData.templateId))

const canNextStep = computed(() => {
  if (formStep.value === 1) return !!formData.templateId
  if (formStep.value === 2) return !!formData.checkName && !!formData.checkDate
  return true
})

// 计算选中目标对应的班级（去重取并集）
const selectedClassIds = computed(() => {
  const classIdSet = new Set<number>()
  // 使用过滤后的班级列表
  const classesToUse = selectedPlanId.value && planTargetClasses.value.length > 0
    ? planTargetClasses.value
    : classList.value

  for (const target of formData.targets) {
    if (target.targetType === 1) {
      // 直接选择的班级
      classIdSet.add(target.targetId)
    } else if (target.targetType === 2) {
      // 选择的年级 - 找出该年级下的所有班级（使用gradeId匹配）
      const gradeClasses = classesToUse.filter(c => c.gradeId === target.targetId)
      gradeClasses.forEach(c => classIdSet.add(c.id))
    } else if (target.targetType === 3) {
      // 选择的院系 - 找出该院系下的所有班级
      const deptClasses = classesToUse.filter(c => c.orgUnitId === target.targetId)
      deptClasses.forEach(c => classIdSet.add(c.id))
    }
  }

  return Array.from(classIdSet)
})

// 计算选中的班级数量
const selectedClassCount = computed(() => selectedClassIds.value.length)

const formData = reactive<DailyCheckCreateRequest>({
  planId: undefined, checkDate: '', checkName: '', checkType: 1, templateId: undefined, description: '', targets: [], categories: []
})

const getStatusClass = (status: number) => ({ 0: 'bg-gray-100 text-gray-700', 1: 'bg-amber-100 text-amber-700', 2: 'bg-green-100 text-green-700', 3: 'bg-blue-100 text-blue-700' }[status] || 'bg-gray-100 text-gray-700')
const getStatusText = (status: number) => ({ 0: '未开始', 1: '进行中', 2: '已完成', 3: '已发布' }[status] || '未知')

const loadCheckList = async () => {
  loading.value = true
  try {
    const res = await getDailyCheckPage({ pageNum: pagination.pageNum, pageSize: pagination.pageSize, ...searchForm })
    checkList.value = res.records
    pagination.total = Number(res.total)
  } catch { ElMessage.error('加载失败') } finally { loading.value = false }
}

const loadTemplateList = async () => { try { templateList.value = await getAllCheckTemplates() || [] } catch {} }
const loadClassList = async () => {
  try {
    const res = await getClassList({ pageNum: 1, pageSize: 10000 })
    classList.value = res.records || []
  } catch {}
}
const loadGradeList = async () => {
  try {
    const res = await getAllGrades()
    gradeList.value = (res || []).filter(g => g.status === 1).sort((a, b) => (b.enrollmentYear || 0) - (a.enrollmentYear || 0))
  } catch {
    // 加载失败，使用空列表
  }
}
const loadDepartmentList = async () => { try { departmentList.value = await getAllEnabledDepartments() || [] } catch {} }

// 加载进行中的检查计划列表
const loadPlanList = async () => {
  try {
    const res = await getCheckPlanPage({ pageNum: 1, pageSize: 100, status: 1 }) // 只加载进行中的计划
    planList.value = res?.records || []
  } catch {}
}

// 获取计划的目标班级列表
const loadPlanTargetClasses = async (planId: number | string) => {
  try {
    const res = await getPlanTargetClasses(planId)
    planTargetClasses.value = res || []
  } catch {
    planTargetClasses.value = []
  }
}

// 选择检查计划时
const handleSelectPlan = async (plan: CheckPlanListVO | null) => {
  if (plan) {
    selectedPlanId.value = plan.id
    formData.planId = plan.id
    // 加载计划的目标班级
    await loadPlanTargetClasses(plan.id)
  } else {
    selectedPlanId.value = undefined
    formData.planId = undefined
    planTargetClasses.value = []
  }
  // 清空已选目标
  formData.targets = []
}

// 计算属性：根据计划目标范围过滤可选的班级
const availableClasses = computed(() => {
  if (selectedPlanId.value && planTargetClasses.value.length > 0) {
    return planTargetClasses.value
  }
  return classList.value
})

// 计算属性：根据计划目标范围过滤可选的年级
const availableGrades = computed(() => {
  if (selectedPlanId.value && planTargetClasses.value.length > 0) {
    // 从目标班级中提取年级ID
    const gradeIdSet = new Set(planTargetClasses.value.map(c => c.gradeId))
    return gradeList.value.filter(g => gradeIdSet.has(g.id))
  }
  return gradeList.value
})

// 计算属性：根据计划目标范围过滤可选的院系
const availableDepartments = computed(() => {
  if (selectedPlanId.value && planTargetClasses.value.length > 0) {
    // 从目标班级中提取院系ID
    const deptIdSet = new Set(planTargetClasses.value.map(c => c.orgUnitId))
    return departmentList.value.filter(d => deptIdSet.has(d.id))
  }
  return departmentList.value
})

const handleSearch = () => { pagination.pageNum = 1; loadCheckList() }
const handleReset = () => { searchForm.checkDate = ''; searchForm.checkName = ''; searchForm.status = undefined; handleSearch() }
const handleSizeChange = () => loadCheckList()

const handleAdd = () => {
  dialogTitle.value = '新增检查'
  currentEditId.value = undefined
  resetFormData()
  formStep.value = 1
  currentTargetType.value = 1
  // 设置默认日期为今天
  formData.checkDate = new Date().toISOString().split('T')[0]
  autoGenerateCheckName()
  dialogVisible.value = true
}
const handleEdit = async (row: DailyCheckResponse) => {
  dialogTitle.value = '编辑检查'
  currentEditId.value = row.id
  try {
    const data = await getDailyCheckById(row.id)
    Object.assign(formData, {
      checkDate: data.checkDate,
      checkName: data.checkName,
      checkType: data.checkType,
      templateId: data.templateId,
      description: data.description,
      targets: data.targets?.map(t => ({ targetType: t.targetType, targetId: t.targetId, targetName: t.targetName })) || [],
      categories: data.categories?.map(c => ({
        categoryId: c.categoryId,
        categoryName: c.categoryName,
        linkType: c.linkType,
        isRequired: c.isRequired,
        sortOrder: c.sortOrder
      })) || []
    })
    formStep.value = data.templateId ? 2 : 1
    currentTargetType.value = 1
    dialogVisible.value = true
  } catch { ElMessage.error('加载失败') }
}
const handleView = async (row: DailyCheckResponse) => { try { detailData.value = await getDailyCheckById(row.id); detailVisible.value = true } catch { ElMessage.error('加载失败') } }
const handleScoring = (row: DailyCheckResponse) => router.push({ path: '/quantification/check-record-scoring', query: { checkId: String(row.id), from: '/quantification/daily-check' } })
const handleDelete = (row: DailyCheckResponse) => ElMessageBox.confirm(`确定删除"${row.checkName}"吗?`, '提示', { type: 'warning' }).then(async () => { await deleteDailyCheck(row.id); ElMessage.success('删除成功'); loadCheckList() })
const handleStart = async (row: DailyCheckResponse) => { await ElMessageBox.confirm('确认开始?', '提示'); await updateCheckStatus(row.id, 1); ElMessage.success('已开始'); loadCheckList() }
const handleFinish = async (row: DailyCheckResponse) => { await ElMessageBox.confirm('确认结束?', '提示', { type: 'warning' }); await updateCheckStatus(row.id, 2); ElMessage.success('已结束'); loadCheckList() }

// 导出相关
const showExportDialog = ref(false)
const exportCheckId = ref<string | number>('')
const exportingRow = ref<DailyCheckResponse | null>(null)
const handleExport = (row: DailyCheckResponse) => {
  exportingRow.value = row
  exportCheckId.value = row.id
  showExportDialog.value = true
}
const goToCreateTemplate = () => {
  if (exportingRow.value?.planId) {
    router.push(`/quantification/check-plan/${exportingRow.value.planId}?tab=exportTemplates`)
  }
}

// 选择模板
const handleSelectTemplate = async (tpl: CheckTemplateResponse) => {
  formData.templateId = tpl.id

  // 如果模板没有加载categories，则从API获取详细信息
  let categories = tpl.categories
  if (!categories || categories.length === 0) {
    try {
      const fullTemplate = await getCheckTemplateById(tpl.id)
      categories = fullTemplate.categories
    } catch {
      // 获取模板详情失败
    }
  }

  // 从模板中复制类别信息
  formData.categories = (categories || []).map(cat => ({
    categoryId: cat.categoryId,
    categoryName: cat.categoryName,
    linkType: cat.linkType,
    isRequired: cat.isRequired,
    sortOrder: cat.sortOrder
  }))
  autoGenerateCheckName()
}

// 自动生成检查名称
const autoGenerateCheckName = () => {
  if (!formData.checkDate) return
  const date = formData.checkDate
  const typeText = formData.checkType === 1 ? '日常检查' : '专项检查'
  const templateName = selectedTemplate.value?.templateName || ''
  formData.checkName = `${date} ${templateName || typeText}`
}

// 下一步
const handleNextStep = () => {
  if (formStep.value === 1 && !formData.templateId) {
    ElMessage.warning('请选择检查模板')
    return
  }
  if (formStep.value === 2) {
    if (!formData.checkName) { ElMessage.warning('请输入检查名称'); return }
    if (!formData.checkDate) { ElMessage.warning('请选择检查日期'); return }
  }
  formStep.value++
}

// 检查目标是否已选择
const isTargetSelected = (type: number, id: number) => {
  return formData.targets.some(t => t.targetType === type && t.targetId === id)
}

// 切换目标选择
const toggleTarget = (type: number, id: number, name: string) => {
  const index = formData.targets.findIndex(t => t.targetType === type && t.targetId === id)

  if (index > -1) {
    formData.targets.splice(index, 1)
  } else {
    formData.targets.push({ targetType: type, targetId: id, targetName: name })
  }
}

const handleRemoveTarget = (index: number) => formData.targets.splice(index, 1)

const handleSubmit = async () => {
  if (!formData.templateId) { ElMessage.error('请选择检查模板'); return }
  if (!formData.checkName || !formData.checkDate) { ElMessage.error('请填写检查名称和日期'); return }
  if (formData.targets.length === 0) { ElMessage.error('请选择检查目标'); return }
  submitLoading.value = true
  try {
    if (currentEditId.value) { await updateDailyCheck(currentEditId.value, formData); ElMessage.success('更新成功') }
    else { await createDailyCheck(formData); ElMessage.success('创建成功') }
    dialogVisible.value = false; loadCheckList()
  } catch { ElMessage.error('操作失败') } finally { submitLoading.value = false }
}

const resetFormData = () => {
  Object.assign(formData, { planId: undefined, checkDate: '', checkName: '', checkType: 1, templateId: undefined, description: '', targets: [], categories: [] })
  selectedPlanId.value = undefined
  planTargetClasses.value = []
}

onMounted(() => { loadCheckList(); loadTemplateList(); loadClassList(); loadGradeList(); loadDepartmentList(); loadPlanList() })
</script>

<style scoped>
.modal-enter-active, .modal-leave-active { transition: opacity 0.2s ease; }
.modal-enter-from, .modal-leave-to { opacity: 0; }
</style>
