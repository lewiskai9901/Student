<template>
  <div class="min-h-screen bg-gray-50/50 p-6">
    <!-- 页面头部 -->
    <div class="mb-6 rounded-xl bg-gradient-to-r from-indigo-600 to-blue-500 p-6 shadow-lg">
      <div class="flex items-center justify-between">
        <div>
          <h1 class="flex items-center gap-3 text-2xl font-bold text-white">
            <Scale class="h-8 w-8" />
            班级加权配置
          </h1>
          <p class="mt-1 text-indigo-100">配置班级人数与扣分加权计算规则</p>
        </div>
        <button
          @click="handleCreate"
          class="flex items-center gap-2 rounded-lg bg-white/20 px-4 py-2 text-white backdrop-blur-sm transition-all hover:bg-white/30"
        >
          <Plus class="h-5 w-5" />
          新增配置
        </button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="mb-6 grid grid-cols-4 gap-4">
      <div class="group relative overflow-hidden rounded-xl bg-white p-5 shadow-sm transition-all hover:-translate-y-1 hover:shadow-md">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-sm font-medium text-gray-500">总配置数</p>
            <p class="mt-1 text-2xl font-bold text-indigo-600">{{ total }}</p>
          </div>
          <div class="flex h-12 w-12 items-center justify-center rounded-xl bg-indigo-100 text-indigo-600">
            <Settings class="h-6 w-6" />
          </div>
        </div>
        <div class="absolute bottom-0 left-0 h-1 w-full bg-gradient-to-r from-indigo-500 to-blue-400 opacity-0 transition-opacity group-hover:opacity-100"></div>
      </div>
      <div class="group relative overflow-hidden rounded-xl bg-white p-5 shadow-sm transition-all hover:-translate-y-1 hover:shadow-md">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-sm font-medium text-gray-500">标准加权</p>
            <p class="mt-1 text-2xl font-bold text-emerald-600">{{ standardCount }}</p>
          </div>
          <div class="flex h-12 w-12 items-center justify-center rounded-xl bg-emerald-100 text-emerald-600">
            <CheckCircle class="h-6 w-6" />
          </div>
        </div>
        <div class="absolute bottom-0 left-0 h-1 w-full bg-gradient-to-r from-emerald-500 to-green-400 opacity-0 transition-opacity group-hover:opacity-100"></div>
      </div>
      <div class="group relative overflow-hidden rounded-xl bg-white p-5 shadow-sm transition-all hover:-translate-y-1 hover:shadow-md">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-sm font-medium text-gray-500">分段加权</p>
            <p class="mt-1 text-2xl font-bold text-amber-600">{{ segmentCount }}</p>
          </div>
          <div class="flex h-12 w-12 items-center justify-center rounded-xl bg-amber-100 text-amber-600">
            <Layers class="h-6 w-6" />
          </div>
        </div>
        <div class="absolute bottom-0 left-0 h-1 w-full bg-gradient-to-r from-amber-500 to-yellow-400 opacity-0 transition-opacity group-hover:opacity-100"></div>
      </div>
      <div class="group relative overflow-hidden rounded-xl bg-white p-5 shadow-sm transition-all hover:-translate-y-1 hover:shadow-md">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-sm font-medium text-gray-500">不加权</p>
            <p class="mt-1 text-2xl font-bold text-gray-600">{{ noneCount }}</p>
          </div>
          <div class="flex h-12 w-12 items-center justify-center rounded-xl bg-gray-100 text-gray-600">
            <XCircle class="h-6 w-6" />
          </div>
        </div>
        <div class="absolute bottom-0 left-0 h-1 w-full bg-gradient-to-r from-gray-400 to-gray-300 opacity-0 transition-opacity group-hover:opacity-100"></div>
      </div>
    </div>

    <!-- 数据表格 -->
    <div class="rounded-xl bg-white shadow-sm">
      <div class="overflow-x-auto">
        <table class="w-full">
          <thead>
            <tr class="border-b border-gray-200 bg-gray-50/50">
              <th class="px-4 py-3 text-left text-sm font-semibold text-gray-600">配置名称</th>
              <th class="px-4 py-3 text-center text-sm font-semibold text-gray-600">加权模式</th>
              <th class="px-4 py-3 text-center text-sm font-semibold text-gray-600">标准人数模式</th>
              <th class="px-4 py-3 text-center text-sm font-semibold text-gray-600">权重范围</th>
              <th class="px-4 py-3 text-left text-sm font-semibold text-gray-600">说明</th>
              <th class="px-4 py-3 text-center text-sm font-semibold text-gray-600">状态</th>
              <th class="px-4 py-3 text-center text-sm font-semibold text-gray-600">操作</th>
            </tr>
          </thead>
          <tbody v-if="loading">
            <tr>
              <td colspan="7" class="px-4 py-12 text-center">
                <div class="flex items-center justify-center gap-2 text-gray-500">
                  <Loader2 class="h-5 w-5 animate-spin" />
                  <span>加载中...</span>
                </div>
              </td>
            </tr>
          </tbody>
          <tbody v-else-if="configList.length === 0">
            <tr>
              <td colspan="7" class="px-4 py-12 text-center text-gray-500">
                <div class="flex flex-col items-center gap-2">
                  <Scale class="h-12 w-12 text-gray-300" />
                  <span>暂无配置数据</span>
                </div>
              </td>
            </tr>
          </tbody>
          <tbody v-else>
            <tr
              v-for="(row, index) in configList"
              :key="row.id"
              class="border-b border-gray-100 transition-colors hover:bg-gray-50/50"
              :style="{ animationDelay: `${index * 30}ms` }"
            >
              <td class="px-4 py-3">
                <div class="flex items-center gap-2">
                  <span class="text-sm font-medium text-gray-900">{{ row.configName }}</span>
                  <span
                    v-if="row.isDefault"
                    class="inline-flex items-center gap-1 rounded-full bg-amber-100 px-2 py-0.5 text-xs font-medium text-amber-700"
                  >
                    <Star class="h-3 w-3" />
                    默认
                  </span>
                </div>
              </td>
              <td class="px-4 py-3 text-center">
                <span
                  class="inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium"
                  :class="getWeightModeClass(row.weightMode)"
                >
                  {{ getWeightModeText(row.weightMode) }}
                </span>
              </td>
              <td class="px-4 py-3 text-center">
                <span v-if="row.standardSizeMode === 'FIXED'" class="text-sm text-gray-600">
                  固定({{ row.standardSize }}人)
                </span>
                <span v-else-if="row.standardSizeMode === 'DYNAMIC'" class="text-sm text-gray-600">动态平均</span>
                <span v-else-if="row.standardSizeMode === 'TARGET_AVERAGE'" class="text-sm text-gray-600">目标平均</span>
                <span v-else-if="row.standardSizeMode === 'RANGE_AVERAGE'" class="text-sm text-gray-600">范围平均</span>
                <span v-else-if="row.standardSizeMode === 'CUSTOM'" class="text-sm text-gray-600">自定义规则</span>
                <span v-else class="text-sm text-gray-400">-</span>
              </td>
              <td class="px-4 py-3 text-center">
                <template v-if="row.enableWeightLimit === 1">
                  <span class="text-sm font-medium text-indigo-600">{{ row.minWeight }} ~ {{ row.maxWeight }}</span>
                </template>
                <template v-else>
                  <span class="inline-flex items-center rounded-full bg-gray-100 px-2 py-0.5 text-xs font-medium text-gray-600">不限制</span>
                </template>
              </td>
              <td class="max-w-xs truncate px-4 py-3 text-sm text-gray-500" :title="row.description">
                {{ row.description || '-' }}
              </td>
              <td class="px-4 py-3 text-center">
                <span
                  class="inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium"
                  :class="row.status === 1 ? 'bg-emerald-100 text-emerald-700' : 'bg-gray-100 text-gray-700'"
                >
                  {{ row.status === 1 ? '启用' : '禁用' }}
                </span>
              </td>
              <td class="px-4 py-3">
                <div class="flex items-center justify-center gap-1">
                  <button
                    @click="handleEdit(row)"
                    class="rounded-lg p-1.5 text-indigo-600 transition-colors hover:bg-indigo-50"
                    title="编辑"
                  >
                    <Pencil class="h-4 w-4" />
                  </button>
                  <button
                    v-if="!row.isDefault"
                    @click="handleSetDefault(row)"
                    class="rounded-lg p-1.5 text-amber-600 transition-colors hover:bg-amber-50"
                    title="设为默认"
                  >
                    <Star class="h-4 w-4" />
                  </button>
                  <button
                    @click="handleDelete(row)"
                    class="rounded-lg p-1.5 text-red-600 transition-colors hover:bg-red-50"
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
      <div class="flex items-center justify-between border-t border-gray-200 px-4 py-3">
        <div class="text-sm text-gray-500">
          共 <span class="font-medium text-gray-900">{{ total }}</span> 条记录
        </div>
        <div class="flex items-center gap-2">
          <select
            v-model="query.pageSize"
            @change="loadConfigs"
            class="rounded-lg border border-gray-300 px-3 py-1.5 text-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
          >
            <option :value="10">10条/页</option>
            <option :value="20">20条/页</option>
            <option :value="50">50条/页</option>
            <option :value="100">100条/页</option>
          </select>
          <div class="flex items-center gap-1">
            <button
              @click="query.pageNum = Math.max(1, query.pageNum - 1); loadConfigs()"
              :disabled="query.pageNum <= 1"
              class="rounded-lg border border-gray-300 px-3 py-1.5 text-sm transition-colors hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-50"
            >
              上一页
            </button>
            <span class="px-3 py-1.5 text-sm text-gray-600">
              {{ query.pageNum }} / {{ Math.ceil(total / query.pageSize) || 1 }}
            </span>
            <button
              @click="query.pageNum = Math.min(Math.ceil(total / query.pageSize), query.pageNum + 1); loadConfigs()"
              :disabled="query.pageNum >= Math.ceil(total / query.pageSize)"
              class="rounded-lg border border-gray-300 px-3 py-1.5 text-sm transition-colors hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-50"
            >
              下一页
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 新增/编辑对话框 -->
    <Teleport to="body">
      <div
        v-if="dialogVisible"
        class="fixed inset-0 z-50 flex items-center justify-center overflow-y-auto bg-black/50 p-4 backdrop-blur-sm"
        @click.self="dialogVisible = false"
      >
        <div class="w-full max-w-3xl animate-modal-in rounded-xl bg-white shadow-2xl">
          <div class="flex items-center justify-between border-b border-gray-200 p-4">
            <h3 class="flex items-center gap-2 text-lg font-semibold text-gray-900">
              <Scale class="h-5 w-5 text-indigo-600" />
              {{ dialogTitle }}
            </h3>
            <button
              @click="dialogVisible = false"
              class="rounded-lg p-1 text-gray-400 transition-colors hover:bg-gray-100 hover:text-gray-600"
            >
              <X class="h-5 w-5" />
            </button>
          </div>

          <div class="max-h-[calc(100vh-200px)] overflow-y-auto p-4">
            <!-- 步骤1: 基本信息 -->
            <div class="mb-4 rounded-lg border border-gray-200 bg-gray-50/50">
              <div class="flex items-center gap-2 border-b border-gray-200 bg-gray-100/80 px-4 py-2">
                <span class="flex h-5 w-5 items-center justify-center rounded-full bg-indigo-600 text-xs font-semibold text-white">1</span>
                <span class="text-sm font-semibold text-gray-700">基本信息</span>
              </div>
              <div class="p-4 space-y-4">
                <div>
                  <label class="mb-1.5 block text-sm font-medium text-gray-700">
                    方案名称 <span class="text-red-500">*</span>
                  </label>
                  <input
                    v-model="form.configName"
                    type="text"
                    placeholder="如：日常检查加权方案"
                    maxlength="50"
                    class="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
                  />
                </div>
                <div>
                  <label class="mb-1.5 block text-sm font-medium text-gray-700">方案说明</label>
                  <textarea
                    v-model="form.description"
                    rows="2"
                    maxlength="200"
                    placeholder="描述该加权方案的用途和适用场景"
                    class="w-full resize-none rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
                  ></textarea>
                </div>
              </div>
            </div>

            <!-- 步骤2: 加权模式 -->
            <div class="mb-4 rounded-lg border border-gray-200 bg-gray-50/50">
              <div class="flex items-center justify-between border-b border-gray-200 bg-gray-100/80 px-4 py-2">
                <div class="flex items-center gap-2">
                  <span class="flex h-5 w-5 items-center justify-center rounded-full bg-indigo-600 text-xs font-semibold text-white">2</span>
                  <span class="text-sm font-semibold text-gray-700">加权模式</span>
                </div>
                <span class="text-xs text-gray-500">选择扣分计算方式</span>
              </div>
              <div class="p-4">
                <div class="grid grid-cols-3 gap-3">
                  <label
                    class="flex cursor-pointer flex-col rounded-lg border-2 p-3 transition-all"
                    :class="form.weightMode === 'STANDARD' ? 'border-indigo-500 bg-indigo-50' : 'border-gray-200 hover:border-gray-300 hover:bg-gray-50'"
                  >
                    <input type="radio" v-model="form.weightMode" value="STANDARD" class="hidden" />
                    <span class="text-sm font-semibold text-gray-900">标准加权</span>
                    <span class="mt-1 text-xs text-gray-500">权重=标准人数÷实际人数</span>
                  </label>
                  <label
                    class="flex cursor-pointer flex-col rounded-lg border-2 p-3 transition-all"
                    :class="form.weightMode === 'SEGMENT' ? 'border-indigo-500 bg-indigo-50' : 'border-gray-200 hover:border-gray-300 hover:bg-gray-50'"
                  >
                    <input type="radio" v-model="form.weightMode" value="SEGMENT" class="hidden" />
                    <span class="text-sm font-semibold text-gray-900">分段加权</span>
                    <span class="mt-1 text-xs text-gray-500">按人数区间设置不同权重</span>
                  </label>
                  <label
                    class="flex cursor-pointer flex-col rounded-lg border-2 p-3 transition-all"
                    :class="form.weightMode === 'NONE' ? 'border-indigo-500 bg-indigo-50' : 'border-gray-200 hover:border-gray-300 hover:bg-gray-50'"
                  >
                    <input type="radio" v-model="form.weightMode" value="NONE" class="hidden" />
                    <span class="text-sm font-semibold text-gray-900">不加权</span>
                    <span class="mt-1 text-xs text-gray-500">使用原始分数,不考虑人数</span>
                  </label>
                </div>
              </div>
            </div>

            <!-- 步骤3: 标准人数配置 (仅STANDARD模式显示) -->
            <div v-if="form.weightMode === 'STANDARD'" class="mb-4 rounded-lg border border-gray-200 bg-gray-50/50">
              <div class="flex items-center justify-between border-b border-gray-200 bg-gray-100/80 px-4 py-2">
                <div class="flex items-center gap-2">
                  <span class="flex h-5 w-5 items-center justify-center rounded-full bg-indigo-600 text-xs font-semibold text-white">3</span>
                  <span class="text-sm font-semibold text-gray-700">标准人数配置</span>
                </div>
                <span class="text-xs text-gray-500">作为加权计算的基准人数</span>
              </div>
              <div class="p-4 space-y-4">
                <div class="grid grid-cols-2 gap-3">
                  <label
                    class="flex cursor-pointer items-start gap-3 rounded-lg border-2 p-3 transition-all"
                    :class="form.standardSizeMode === 'FIXED' ? 'border-indigo-500 bg-indigo-50' : 'border-gray-200 hover:border-gray-300'"
                  >
                    <input type="radio" v-model="form.standardSizeMode" value="FIXED" class="mt-0.5 h-4 w-4 text-indigo-600" />
                    <div>
                      <span class="text-sm font-semibold text-gray-900">固定标准人数</span>
                      <p class="mt-0.5 text-xs text-gray-500">使用手动设置的固定值</p>
                    </div>
                  </label>
                  <label
                    class="flex cursor-pointer items-start gap-3 rounded-lg border-2 p-3 transition-all"
                    :class="form.standardSizeMode === 'TARGET_AVERAGE' ? 'border-indigo-500 bg-indigo-50' : 'border-gray-200 hover:border-gray-300'"
                  >
                    <input type="radio" v-model="form.standardSizeMode" value="TARGET_AVERAGE" class="mt-0.5 h-4 w-4 text-indigo-600" />
                    <div>
                      <span class="text-sm font-semibold text-gray-900">目标平均人数</span>
                      <p class="mt-0.5 text-xs text-gray-500">根据检查目标自动计算</p>
                    </div>
                  </label>
                </div>

                <!-- 固定标准人数输入 -->
                <div v-if="form.standardSizeMode === 'FIXED'" class="flex items-center gap-3">
                  <label class="text-sm font-medium text-gray-700">标准班额</label>
                  <input
                    v-model.number="form.standardSize"
                    type="number"
                    min="1"
                    max="200"
                    class="w-24 rounded-lg border border-gray-300 px-3 py-2 text-center text-sm focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
                  />
                  <span class="text-sm text-gray-500">人</span>
                  <span class="text-xs text-gray-400">（建议设置为学校的标准班额，如40人）</span>
                </div>

                <!-- 目标平均模式说明 -->
                <div v-if="form.standardSizeMode === 'TARGET_AVERAGE'" class="rounded-lg bg-blue-50 p-3">
                  <div class="flex gap-2">
                    <Info class="h-5 w-5 flex-shrink-0 text-blue-600" />
                    <p class="text-sm text-blue-800">系统将根据检查目标自动计算平均班级人数作为标准人数</p>
                  </div>
                </div>
              </div>
            </div>

            <!-- 步骤3: 分段规则配置 (仅SEGMENT模式显示) -->
            <div v-if="form.weightMode === 'SEGMENT'" class="mb-4 rounded-lg border border-gray-200 bg-gray-50/50">
              <div class="flex items-center justify-between border-b border-gray-200 bg-gray-100/80 px-4 py-2">
                <div class="flex items-center gap-2">
                  <span class="flex h-5 w-5 items-center justify-center rounded-full bg-indigo-600 text-xs font-semibold text-white">3</span>
                  <span class="text-sm font-semibold text-gray-700">分段规则配置</span>
                </div>
                <button
                  @click="addSegment"
                  class="flex items-center gap-1 rounded-lg bg-indigo-600 px-3 py-1 text-xs font-medium text-white transition-colors hover:bg-indigo-700"
                >
                  <Plus class="h-3 w-3" />
                  添加分段
                </button>
              </div>
              <div class="p-4">
                <div v-if="segmentRules.length === 0" class="rounded-lg bg-blue-50 p-3">
                  <div class="flex gap-2">
                    <Info class="h-5 w-5 flex-shrink-0 text-blue-600" />
                    <p class="text-sm text-blue-800">暂无分段规则，请点击"添加分段"按钮添加</p>
                  </div>
                </div>
                <div v-else class="space-y-3">
                  <div
                    v-for="(segment, index) in segmentRules"
                    :key="index"
                    class="flex items-center gap-3 rounded-lg border border-gray-200 bg-white p-3"
                  >
                    <span class="text-sm text-gray-500">人数范围:</span>
                    <input
                      v-model.number="segment.minSize"
                      type="number"
                      min="1"
                      class="w-16 rounded-lg border border-gray-300 px-2 py-1 text-center text-sm focus:border-indigo-500 focus:outline-none"
                    />
                    <span class="text-gray-400">至</span>
                    <input
                      v-model.number="segment.maxSize"
                      type="number"
                      min="1"
                      class="w-16 rounded-lg border border-gray-300 px-2 py-1 text-center text-sm focus:border-indigo-500 focus:outline-none"
                    />
                    <span class="text-sm text-gray-500">人</span>
                    <span class="text-sm text-gray-500 ml-2">权重:</span>
                    <input
                      v-model.number="segment.weight"
                      type="number"
                      min="0.1"
                      max="5"
                      step="0.1"
                      class="w-16 rounded-lg border border-gray-300 px-2 py-1 text-center text-sm focus:border-indigo-500 focus:outline-none"
                    />
                    <span class="flex-1 text-xs text-gray-400">{{ getSegmentDescription(segment) }}</span>
                    <button
                      @click="removeSegment(index)"
                      class="rounded-lg p-1 text-red-600 transition-colors hover:bg-red-50"
                    >
                      <Trash2 class="h-4 w-4" />
                    </button>
                  </div>
                </div>
              </div>
            </div>

            <!-- 步骤4: 权重范围限制 (仅STANDARD模式显示) -->
            <div v-if="form.weightMode === 'STANDARD'" class="rounded-lg border border-gray-200 bg-gray-50/50">
              <div class="flex items-center justify-between border-b border-gray-200 bg-gray-100/80 px-4 py-2">
                <div class="flex items-center gap-2">
                  <span class="flex h-5 w-5 items-center justify-center rounded-full bg-indigo-600 text-xs font-semibold text-white">4</span>
                  <span class="text-sm font-semibold text-gray-700">权重范围限制</span>
                </div>
                <span class="text-xs text-gray-500">防止极端情况，控制权重上下限</span>
              </div>
              <div class="p-4 space-y-4">
                <div class="flex items-center gap-4">
                  <label class="text-sm font-medium text-gray-700">启用限制</label>
                  <button
                    @click="form.enableWeightLimit = form.enableWeightLimit === 1 ? 0 : 1"
                    class="relative h-6 w-11 rounded-full transition-colors"
                    :class="form.enableWeightLimit === 1 ? 'bg-indigo-600' : 'bg-gray-200'"
                  >
                    <span
                      class="absolute left-0.5 top-0.5 h-5 w-5 rounded-full bg-white shadow transition-transform"
                      :class="form.enableWeightLimit === 1 ? 'translate-x-5' : 'translate-x-0'"
                    ></span>
                  </button>
                  <span class="text-xs text-gray-400">开启后将限制权重系数在指定范围内</span>
                </div>

                <template v-if="form.enableWeightLimit === 1">
                  <!-- 快速设置 -->
                  <div class="flex items-center gap-2">
                    <span class="text-sm text-gray-600">快速设置:</span>
                    <button
                      @click="applyPreset('conservative')"
                      class="rounded-lg border border-gray-300 px-3 py-1 text-xs font-medium text-gray-700 transition-colors hover:bg-gray-50"
                    >
                      保守方案 (0.7~1.3)
                    </button>
                    <button
                      @click="applyPreset('standard')"
                      class="rounded-lg border border-gray-300 px-3 py-1 text-xs font-medium text-gray-700 transition-colors hover:bg-gray-50"
                    >
                      标准方案 (0.5~2.0)
                    </button>
                    <button
                      @click="applyPreset('aggressive')"
                      class="rounded-lg border border-gray-300 px-3 py-1 text-xs font-medium text-gray-700 transition-colors hover:bg-gray-50"
                    >
                      激进方案 (0.3~3.0)
                    </button>
                  </div>

                  <div class="grid grid-cols-2 gap-4">
                    <div class="flex items-center gap-2">
                      <label class="text-sm font-medium text-gray-700">最小系数</label>
                      <input
                        v-model.number="form.minWeight"
                        type="number"
                        min="0.1"
                        max="5"
                        step="0.1"
                        class="w-20 rounded-lg border border-gray-300 px-2 py-1.5 text-center text-sm focus:border-indigo-500 focus:outline-none"
                      />
                      <span class="text-xs text-gray-400">人数越多扣分越少</span>
                    </div>
                    <div class="flex items-center gap-2">
                      <label class="text-sm font-medium text-gray-700">最大系数</label>
                      <input
                        v-model.number="form.maxWeight"
                        type="number"
                        min="0.1"
                        max="5"
                        step="0.1"
                        class="w-20 rounded-lg border border-gray-300 px-2 py-1.5 text-center text-sm focus:border-indigo-500 focus:outline-none"
                      />
                      <span class="text-xs text-gray-400">人数越少扣分越多</span>
                    </div>
                  </div>
                </template>
              </div>
            </div>
          </div>

          <div class="flex justify-end gap-2 border-t border-gray-200 p-4">
            <button
              @click="dialogVisible = false"
              class="rounded-lg border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 transition-colors hover:bg-gray-50"
            >
              取消
            </button>
            <button
              @click="handleSubmit"
              :disabled="submitting"
              class="flex items-center gap-2 rounded-lg bg-indigo-600 px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-indigo-700 disabled:cursor-not-allowed disabled:opacity-50"
            >
              <Loader2 v-if="submitting" class="h-4 w-4 animate-spin" />
              确定
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import {
  listWeightConfigs,
  createWeightConfig,
  updateWeightConfig,
  deleteWeightConfig,
  setDefaultWeightConfig
} from '@/api/quantification-extra'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Scale,
  Plus,
  Settings,
  CheckCircle,
  Layers,
  XCircle,
  Star,
  Pencil,
  Trash2,
  Loader2,
  X,
  Info
} from 'lucide-vue-next'

const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('新增配置')
const total = ref(0)
const configList = ref<any[]>([])

// 统计数据
const standardCount = computed(() => configList.value.filter(c => c.weightMode === 'STANDARD').length)
const segmentCount = computed(() => configList.value.filter(c => c.weightMode === 'SEGMENT').length)
const noneCount = computed(() => configList.value.filter(c => c.weightMode === 'NONE').length)

const query = reactive({
  pageNum: 1,
  pageSize: 10
})

const form = reactive({
  id: undefined as number | undefined,
  configName: '',
  weightMode: 'STANDARD',
  minWeight: 0.5,
  maxWeight: 2.0,
  enableWeight: 1,
  enableWeightLimit: 1,
  description: '',
  segmentRules: '',
  standardSizeMode: 'FIXED',
  standardSize: 40,
  customStandardRules: '',
  rangeDepartments: '',
  rangeGrades: '',
  rangeClasses: ''
})

// 分段规则数组
const segmentRules = ref<Array<{ minSize: number; maxSize: number; weight: number }>>([])

// 获取加权模式样式
const getWeightModeClass = (mode: string) => {
  const map: Record<string, string> = {
    STANDARD: 'bg-emerald-100 text-emerald-700',
    SEGMENT: 'bg-amber-100 text-amber-700',
    NONE: 'bg-gray-100 text-gray-700'
  }
  return map[mode] || 'bg-gray-100 text-gray-700'
}

// 获取加权模式文本
const getWeightModeText = (mode: string) => {
  const map: Record<string, string> = {
    STANDARD: '标准加权',
    PER_CAPITA: '人均加权',
    SEGMENT: '分段加权',
    NONE: '不加权'
  }
  return map[mode] || mode
}

// 添加分段
const addSegment = () => {
  const lastSegment = segmentRules.value[segmentRules.value.length - 1]
  const newMinSize = lastSegment ? lastSegment.maxSize + 1 : 1

  segmentRules.value.push({
    minSize: newMinSize,
    maxSize: newMinSize + 19,
    weight: 1.0
  })
}

// 删除分段
const removeSegment = (index: number) => {
  segmentRules.value.splice(index, 1)
}

// 获取分段描述
const getSegmentDescription = (segment: any) => {
  const { weight } = segment
  if (weight < 1) {
    const percent = ((1 - weight) * 100).toFixed(0)
    return `(扣分减少${percent}%)`
  } else if (weight > 1) {
    const percent = ((weight - 1) * 100).toFixed(0)
    return `(扣分增加${percent}%)`
  }
  return '(正常扣分)'
}

// 应用预设方案
const applyPreset = (preset: string) => {
  const presets = {
    conservative: { min: 0.7, max: 1.3 },
    standard: { min: 0.5, max: 2.0 },
    aggressive: { min: 0.3, max: 3.0 }
  }

  const selected = presets[preset as keyof typeof presets]
  if (selected) {
    form.minWeight = selected.min
    form.maxWeight = selected.max
    ElMessage.success(`已应用${preset === 'conservative' ? '保守' : preset === 'standard' ? '标准' : '激进'}方案`)
  }
}

// 加载配置列表
const loadConfigs = async () => {
  loading.value = true
  try {
    const res = await listWeightConfigs(query)
    configList.value = res.records || []
    total.value = Number(res.total) || 0
  } catch (error) {
    console.error('加载配置列表失败:', error)
    ElMessage.error('加载配置列表失败')
    configList.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

// 新增
const handleCreate = () => {
  dialogTitle.value = '新增配置'
  dialogVisible.value = true

  Object.assign(form, {
    id: undefined,
    configName: '',
    weightMode: 'STANDARD',
    minWeight: 0.5,
    maxWeight: 2.0,
    enableWeight: 1,
    enableWeightLimit: 1,
    description: '',
    segmentRules: '',
    standardSizeMode: 'FIXED',
    standardSize: 40,
    customStandardRules: '',
    rangeDepartments: '',
    rangeGrades: '',
    rangeClasses: ''
  })

  segmentRules.value = []
}

// 编辑
const handleEdit = (row: any) => {
  dialogTitle.value = '编辑配置'
  dialogVisible.value = true

  Object.assign(form, {
    id: row.id,
    configName: row.configName,
    weightMode: row.weightMode,
    minWeight: row.minWeight,
    maxWeight: row.maxWeight,
    enableWeight: row.enableWeight,
    enableWeightLimit: row.enableWeightLimit ?? 1,
    description: row.description,
    segmentRules: row.segmentRules,
    standardSizeMode: row.standardSizeMode || 'FIXED',
    standardSize: row.standardSize || 40,
    customStandardRules: row.customStandardRules || '',
    rangeDepartments: row.rangeDepartments || '',
    rangeGrades: row.rangeGrades || '',
    rangeClasses: row.rangeClasses || ''
  })

  if (row.weightMode === 'SEGMENT' && row.segmentRules) {
    try {
      segmentRules.value = JSON.parse(row.segmentRules)
    } catch (error) {
      segmentRules.value = []
    }
  } else {
    segmentRules.value = []
  }
}

// 设置默认
const handleSetDefault = async (row: any) => {
  try {
    await setDefaultWeightConfig(row.id)
    ElMessage.success('设置默认成功')
    loadConfigs()
  } catch (error) {
    console.error('设置默认失败:', error)
    ElMessage.error('设置默认失败')
  }
}

// 删除
const handleDelete = async (row: any) => {
  try {
    await ElMessageBox.confirm('确定要删除该配置吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteWeightConfig(row.id)
    ElMessage.success('删除成功')
    loadConfigs()
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

// 提交
const handleSubmit = async () => {
  if (!form.configName || form.configName.trim() === '') {
    ElMessage.warning('请输入配置名称')
    return
  }

  if (form.weightMode === 'SEGMENT') {
    if (segmentRules.value.length === 0) {
      ElMessage.warning('请至少添加一个分段规则')
      return
    }
    form.segmentRules = JSON.stringify(segmentRules.value)
  } else {
    form.segmentRules = ''
  }

  submitting.value = true
  try {
    if (form.id) {
      await updateWeightConfig(form.id, form)
      ElMessage.success('更新成功')
    } else {
      await createWeightConfig(form as any)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadConfigs()
  } catch (error) {
    console.error('操作失败:', error)
    ElMessage.error('操作失败')
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadConfigs()
})
</script>

<style scoped>
@keyframes modal-in {
  from {
    opacity: 0;
    transform: scale(0.95);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

.animate-modal-in {
  animation: modal-in 0.2s ease-out;
}

tbody tr {
  animation: row-fade-in 0.3s ease-out forwards;
  opacity: 0;
}

@keyframes row-fade-in {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
