<template>
  <div class="min-h-screen bg-gray-50 p-6">
    <!-- Header -->
    <div class="mb-4">
      <h1 class="text-xl font-bold text-gray-800">专业管理</h1>
      <p class="mt-0.5 text-sm text-gray-400">管理专业设置、方向规划与培养方案</p>
    </div>

    <!-- Stats bar -->
    <div class="mb-4 flex items-center gap-2 text-sm text-gray-600">
      <span>专业总数 <b class="text-gray-900">{{ stats.total }}</b></span>
      <span class="text-gray-300">|</span>
      <span>招生中 <b class="text-emerald-600">{{ stats.enrolling }}</b></span>
      <span class="text-gray-300">|</span>
      <span>暂停 <b class="text-amber-600">{{ stats.suspended }}</b></span>
      <span class="text-gray-300">|</span>
      <span>筹建 <b class="text-blue-600">{{ stats.preparing }}</b></span>
      <span class="text-gray-300">|</span>
      <span>方向总数 <b class="text-gray-900">{{ stats.directionCount }}</b></span>
    </div>

    <!-- Filter bar -->
    <div class="mb-4 flex flex-wrap items-center gap-3">
      <el-input
        v-model="filters.keyword"
        placeholder="搜索专业名称/代码"
        clearable
        class="!w-52"
        @keyup.enter="loadMajors"
        @clear="loadMajors"
      >
        <template #prefix><Search class="h-4 w-4 text-gray-400" /></template>
      </el-input>

      <el-tree-select
        v-model="filters.orgUnitId"
        :data="orgTree"
        :props="{ label: 'unitName', value: 'id', children: 'children' }"
        placeholder="所属系部"
        clearable
        filterable
        check-strictly
        class="!w-44"
        @change="loadMajors"
      />

      <el-select v-model="filters.majorStatus" placeholder="专业状态" clearable class="!w-32" @change="loadMajors">
        <el-option v-for="s in STATUSES" :key="s.value" :label="s.label" :value="s.value" />
      </el-select>

      <!-- View mode switcher -->
      <div class="ml-auto flex items-center gap-3">
        <div class="flex items-center rounded-lg border border-gray-200 bg-white p-0.5">
          <button
            v-for="v in VIEW_MODES" :key="v.key"
            @click="viewMode = v.key"
            class="rounded-md px-3 py-1.5 text-xs font-medium transition-colors"
            :class="viewMode === v.key ? 'bg-blue-600 text-white' : 'text-gray-500 hover:text-gray-700'"
          >
            {{ v.label }}
          </button>
        </div>
        <button
          @click="openMajorDialog()"
          class="inline-flex items-center gap-1.5 rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white shadow-sm hover:bg-blue-700 transition-colors"
        >
          <Plus class="h-4 w-4" />
          新增专业
        </button>
      </div>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="flex items-center justify-center py-20">
      <div class="h-8 w-8 animate-spin rounded-full border-4 border-blue-200 border-t-blue-600" />
    </div>

    <!-- Empty -->
    <div v-else-if="filteredMajors.length === 0" class="py-20 text-center text-gray-400">
      暂无专业数据
    </div>

    <!-- ═══════════ View 1: Category View (default) ═══════════ -->
    <div v-else-if="viewMode === 'category'" class="space-y-4">
      <div
        v-for="group in categoryGroups" :key="group.code"
        class="rounded-xl border border-gray-200 bg-white overflow-hidden"
      >
        <!-- Category header -->
        <div
          @click="toggleCategory(group.code)"
          class="flex cursor-pointer items-center gap-2 px-5 py-3 bg-gray-50 hover:bg-gray-100 transition-colors border-b border-gray-100"
        >
          <ChevronRight
            class="h-4 w-4 text-gray-400 transition-transform"
            :class="{ 'rotate-90': expandedCategories.has(group.code) }"
          />
          <span class="font-semibold text-gray-800">{{ group.name }}</span>
          <span class="text-xs text-gray-400">({{ group.majors.length }}个专业)</span>
        </div>

        <!-- Majors in category -->
        <div v-show="expandedCategories.has(group.code)" class="p-4 space-y-3">
          <div
            v-for="major in group.majors" :key="major.id"
            class="rounded-lg border border-gray-100 bg-gray-50/50 p-4 hover:border-blue-200 transition-colors"
          >
            <div class="flex items-start justify-between">
              <div class="flex-1 min-w-0">
                <!-- Major name + status -->
                <div class="flex items-center gap-3 mb-2">
                  <span class="text-base font-semibold text-gray-900">{{ major.majorName }}</span>
                  <span
                    v-if="getStatusDef(major.majorStatus)"
                    class="inline-block rounded px-2 py-0.5 text-xs font-medium"
                    :class="getStatusDef(major.majorStatus)!.class"
                  >{{ getStatusDef(major.majorStatus)!.label }}</span>
                </div>
                <!-- Meta row -->
                <div class="flex flex-wrap items-center gap-x-5 gap-y-1 text-sm text-gray-500">
                  <span>代码: <b class="text-gray-700">{{ major.majorCode }}</b></span>
                  <span>系部: {{ major.orgUnitName || '-' }}</span>
                  <span v-if="major.leadTeacherName">带头人: {{ major.leadTeacherName }}</span>
                  <span v-if="major.enrollmentTarget">招生: {{ major.enrollmentTarget }}</span>
                  <span v-if="major.educationForm">形式: {{ major.educationForm }}</span>
                </div>
                <!-- Directions list -->
                <div v-if="major.directions && major.directions.length > 0" class="mt-3 space-y-1.5">
                  <div class="text-xs font-medium text-gray-400 mb-1">方向:</div>
                  <div
                    v-for="dir in major.directions" :key="dir.id"
                    class="flex items-center gap-3 text-sm text-gray-600 pl-1"
                  >
                    <span class="text-blue-400 text-xs">&#9670;</span>
                    <span class="font-medium text-gray-700">{{ dir.directionName }}</span>
                    <span v-if="dir.isSegmented" class="text-gray-500">
                      {{ dir.phase1Level }}&rarr;{{ dir.phase2Level }}
                      {{ dir.phase1Years }}+{{ dir.phase2Years }}年
                    </span>
                    <span v-else class="text-gray-500">{{ dir.level }} {{ dir.years }}年</span>
                    <span v-if="dir.certificateNames && dir.certificateNames.length" class="text-gray-400 text-xs">
                      证书{{ dir.certificateNames.length }}项
                    </span>
                  </div>
                </div>
              </div>
              <!-- Actions -->
              <div class="flex items-center gap-1 ml-4 shrink-0">
                <button @click="openMajorDialog(major)" class="text-xs text-blue-600 hover:text-blue-800 px-2 py-1 rounded hover:bg-blue-50">编辑</button>
                <button @click="openDirectionDialog(major)" class="text-xs text-emerald-600 hover:text-emerald-800 px-2 py-1 rounded hover:bg-emerald-50">添加方向</button>
                <button @click="openDetailDrawer(major)" class="text-xs text-gray-600 hover:text-gray-800 px-2 py-1 rounded hover:bg-gray-100">详情</button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- ═══════════ View 2: List View ═══════════ -->
    <div v-else-if="viewMode === 'list'" class="rounded-xl border border-gray-200 bg-white overflow-hidden">
      <el-table :data="filteredMajors" stripe class="w-full" row-key="id">
        <el-table-column prop="majorCode" label="代码" width="110" />
        <el-table-column prop="majorName" label="专业名称" min-width="160">
          <template #default="{ row }">
            <span class="font-medium text-gray-800 cursor-pointer hover:text-blue-600" @click="openDetailDrawer(row)">
              {{ row.majorName }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="大类" width="120">
          <template #default="{ row }">{{ getCategoryName(row.majorCategoryCode) }}</template>
        </el-table-column>
        <el-table-column prop="orgUnitName" label="系部" width="130">
          <template #default="{ row }">{{ row.orgUnitName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="leadTeacherName" label="带头人" width="100">
          <template #default="{ row }">{{ row.leadTeacherName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="enrollmentTarget" label="招生对象" width="110">
          <template #default="{ row }">{{ row.enrollmentTarget || '-' }}</template>
        </el-table-column>
        <el-table-column label="方向数" width="80" align="center">
          <template #default="{ row }">
            <span class="text-gray-700">{{ row.directions?.length || 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <span
              v-if="getStatusDef(row.majorStatus)"
              class="inline-block rounded px-2 py-0.5 text-xs font-medium"
              :class="getStatusDef(row.majorStatus)!.class"
            >{{ getStatusDef(row.majorStatus)!.label }}</span>
            <span v-else class="text-gray-400">-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <div class="flex items-center gap-1">
              <button @click="openDetailDrawer(row)" class="text-xs text-gray-600 hover:text-blue-600 px-2 py-1">详情</button>
              <button @click="openMajorDialog(row)" class="text-xs text-blue-600 hover:text-blue-800 px-2 py-1">编辑</button>
              <button @click="handleDeleteMajor(row)" class="text-xs text-red-500 hover:text-red-700 px-2 py-1">删除</button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- ═══════════ View 3: Card View ═══════════ -->
    <div v-else-if="viewMode === 'card'" class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
      <div
        v-for="major in filteredMajors" :key="major.id"
        @click="openDetailDrawer(major)"
        class="rounded-xl border border-gray-200 bg-white p-4 hover:shadow-md hover:border-blue-200 transition-all cursor-pointer"
      >
        <div class="flex items-start justify-between mb-3">
          <div class="min-w-0 flex-1">
            <div class="text-base font-semibold text-gray-900 truncate">{{ major.majorName }}</div>
            <div class="text-xs text-gray-400 mt-0.5">{{ major.majorCode }}</div>
          </div>
          <span
            v-if="getStatusDef(major.majorStatus)"
            class="inline-block rounded px-2 py-0.5 text-xs font-medium shrink-0 ml-2"
            :class="getStatusDef(major.majorStatus)!.class"
          >{{ getStatusDef(major.majorStatus)!.label }}</span>
        </div>
        <div class="space-y-1.5 text-sm text-gray-500">
          <div class="flex items-center justify-between">
            <span>系部</span>
            <span class="text-gray-700">{{ major.orgUnitName || '-' }}</span>
          </div>
          <div class="flex items-center justify-between">
            <span>大类</span>
            <span class="text-gray-700">{{ getCategoryName(major.majorCategoryCode) }}</span>
          </div>
          <div class="flex items-center justify-between">
            <span>方向数</span>
            <span class="font-medium text-blue-600">{{ major.directions?.length || 0 }}</span>
          </div>
          <div v-if="major.enrollmentTarget" class="flex items-center justify-between">
            <span>招生对象</span>
            <span class="text-gray-700">{{ major.enrollmentTarget }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- ═══════════ Detail Drawer ═══════════ -->
    <el-drawer
      v-model="detailDrawerVisible"
      :title="detailMajor?.majorName || '专业详情'"
      size="600px"
      :destroy-on-close="false"
    >
      <template v-if="detailMajor">
        <el-tabs v-model="detailTab" class="detail-tabs">
          <!-- Tab 1: Basic Info -->
          <el-tab-pane label="基本信息" name="info">
            <div class="space-y-0">
              <InfoRow label="专业代码" :value="detailMajor.majorCode" />
              <InfoRow label="专业名称" :value="detailMajor.majorName" />
              <InfoRow label="专业大类" :value="getCategoryName(detailMajor.majorCategoryCode)" />
              <InfoRow label="所属系部" :value="detailMajor.orgUnitName || '-'" />
              <InfoRow label="专业带头人" :value="detailMajor.leadTeacherName || '-'" />
              <InfoRow label="招生对象" :value="detailMajor.enrollmentTarget || '-'" />
              <InfoRow label="办学形式" :value="detailMajor.educationForm || '-'" />
              <InfoRow label="审批年份" :value="detailMajor.approvalYear ? String(detailMajor.approvalYear) : '-'" />
              <InfoRow label="专业状态">
                <template #value>
                  <span
                    v-if="getStatusDef(detailMajor.majorStatus)"
                    class="inline-block rounded px-2 py-0.5 text-xs font-medium"
                    :class="getStatusDef(detailMajor.majorStatus)!.class"
                  >{{ getStatusDef(detailMajor.majorStatus)!.label }}</span>
                  <span v-else class="text-gray-400">-</span>
                </template>
              </InfoRow>
              <InfoRow label="描述" :value="detailMajor.description || '-'" />
            </div>
          </el-tab-pane>

          <!-- Tab 2: Directions -->
          <el-tab-pane label="专业方向" name="directions">
            <div class="mb-3 flex items-center justify-between">
              <span class="text-sm text-gray-500">共 {{ detailDirections.length }} 个方向</span>
              <button
                @click="openDirectionDialog(detailMajor!)"
                class="inline-flex items-center gap-1 text-sm text-blue-600 hover:text-blue-800"
              >
                <Plus class="h-4 w-4" />
                添加方向
              </button>
            </div>
            <div v-if="directionsLoading" class="flex justify-center py-10">
              <div class="h-6 w-6 animate-spin rounded-full border-2 border-blue-200 border-t-blue-600" />
            </div>
            <div v-else-if="detailDirections.length === 0" class="py-10 text-center text-gray-400 text-sm">
              暂无方向
            </div>
            <div v-else class="space-y-3">
              <div
                v-for="dir in detailDirections" :key="dir.id"
                class="rounded-lg border border-gray-100 bg-gray-50/60 p-4"
              >
                <div class="flex items-start justify-between mb-2">
                  <div>
                    <div class="font-semibold text-gray-800">{{ dir.directionName }}</div>
                    <div class="text-xs text-gray-400 mt-0.5">{{ dir.directionCode }}</div>
                  </div>
                  <div class="flex items-center gap-1 shrink-0">
                    <button @click="openDirectionDialog(detailMajor!, dir)" class="text-xs text-blue-600 hover:text-blue-800 px-2 py-1 rounded hover:bg-blue-50">编辑</button>
                    <button @click="handleDeleteDirection(dir)" class="text-xs text-red-500 hover:text-red-700 px-2 py-1 rounded hover:bg-red-50">删除</button>
                  </div>
                </div>
                <!-- Level progression -->
                <div class="text-sm text-gray-600 space-y-1">
                  <div v-if="dir.isSegmented" class="flex items-center gap-2">
                    <span class="text-gray-400">培养:</span>
                    <span class="font-medium">{{ dir.phase1Level }} {{ dir.phase1Years }}年</span>
                    <span class="text-gray-300">&rarr;</span>
                    <span class="font-medium">{{ dir.phase2Level }} {{ dir.phase2Years }}年</span>
                    <span class="text-gray-400">(共{{ (dir.phase1Years || 0) + (dir.phase2Years || 0) }}年)</span>
                  </div>
                  <div v-else class="flex items-center gap-2">
                    <span class="text-gray-400">培养:</span>
                    <span class="font-medium">{{ dir.level }} {{ dir.years }}年</span>
                  </div>
                  <div v-if="dir.enrollmentTarget || dir.educationForm" class="flex items-center gap-4">
                    <span v-if="dir.enrollmentTarget"><span class="text-gray-400">招生:</span> {{ dir.enrollmentTarget }}</span>
                    <span v-if="dir.educationForm"><span class="text-gray-400">形式:</span> {{ dir.educationForm }}</span>
                    <span v-if="dir.maxEnrollment"><span class="text-gray-400">最大人数:</span> {{ dir.maxEnrollment }}</span>
                  </div>
                  <div v-if="dir.certificateNames && dir.certificateNames.length" class="flex items-start gap-2">
                    <span class="text-gray-400 shrink-0">证书:</span>
                    <div class="flex flex-wrap gap-1">
                      <el-tag v-for="(cert, ci) in dir.certificateNames" :key="ci" size="small" type="info">{{ cert }}</el-tag>
                    </div>
                  </div>
                  <div v-if="dir.cooperationEnterprise">
                    <span class="text-gray-400">合作企业:</span> {{ dir.cooperationEnterprise }}
                  </div>
                  <div v-if="dir.trainingStandard">
                    <span class="text-gray-400">培养标准:</span> {{ dir.trainingStandard }}
                  </div>
                  <div v-if="dir.remarks" class="text-gray-400 text-xs mt-1">备注: {{ dir.remarks }}</div>
                </div>
              </div>
            </div>
          </el-tab-pane>

          <!-- Tab 3: Curriculum Plans -->
          <el-tab-pane label="培养方案" name="plans">
            <div v-if="plansLoading" class="flex justify-center py-10">
              <div class="h-6 w-6 animate-spin rounded-full border-2 border-blue-200 border-t-blue-600" />
            </div>
            <div v-else-if="detailPlans.length === 0" class="py-10 text-center text-gray-400 text-sm">
              暂无培养方案
            </div>
            <div v-else class="space-y-3">
              <div
                v-for="plan in detailPlans" :key="plan.id"
                class="rounded-lg border border-gray-100 bg-gray-50/60 p-4"
              >
                <div class="flex items-center justify-between mb-1">
                  <span class="font-medium text-gray-800">{{ plan.name }}</span>
                  <el-tag v-if="plan.status === 1" size="small" type="success">已发布</el-tag>
                  <el-tag v-else-if="plan.status === 0" size="small" type="info">草稿</el-tag>
                  <el-tag v-else size="small" type="warning">已废弃</el-tag>
                </div>
                <div class="text-sm text-gray-500 space-y-0.5">
                  <div>版本: {{ plan.version }} | 年级: {{ plan.gradeYear }} | 总学分: {{ plan.totalCredits }}</div>
                  <div v-if="plan.majorDirectionName">方向: {{ plan.majorDirectionName }}</div>
                  <div v-if="plan.description" class="text-xs text-gray-400">{{ plan.description }}</div>
                </div>
              </div>
            </div>
          </el-tab-pane>
        </el-tabs>
      </template>
    </el-drawer>

    <!-- ═══════════ Major Create/Edit Dialog ═══════════ -->
    <el-dialog
      v-model="majorDialogVisible"
      :title="majorForm.id ? '编辑专业' : '新增专业'"
      width="620px"
      :close-on-click-modal="false"
      @closed="resetMajorForm"
    >
      <el-form
        ref="majorFormRef"
        :model="majorForm"
        :rules="majorRules"
        label-position="top"
        class="px-2"
      >
        <!-- Section: Basic Info -->
        <div class="text-sm font-semibold text-gray-500 mb-3">基本信息</div>
        <div class="grid grid-cols-2 gap-x-4">
          <el-form-item label="专业大类" prop="majorCategoryCode">
            <el-select v-model="majorForm.majorCategoryCode" placeholder="选择大类" class="w-full">
              <el-option v-for="c in CATEGORIES" :key="c.code" :label="c.name" :value="c.code" />
            </el-select>
          </el-form-item>
          <el-form-item label="专业代码" prop="majorCode">
            <el-input v-model="majorForm.majorCode" placeholder="如 0301" />
          </el-form-item>
        </div>
        <el-form-item label="专业名称" prop="majorName">
          <el-input v-model="majorForm.majorName" placeholder="专业全称" />
        </el-form-item>
        <el-form-item label="所属系部" prop="orgUnitId">
          <el-tree-select
            v-model="majorForm.orgUnitId"
            :data="orgTree"
            :props="{ label: 'unitName', value: 'id', children: 'children' }"
            placeholder="选择系部"
            clearable
            filterable
            check-strictly
            class="w-full"
          />
        </el-form-item>

        <!-- Section: School Info -->
        <div class="text-sm font-semibold text-gray-500 mb-3 mt-4">办学信息</div>
        <el-form-item label="招生对象">
          <el-radio-group v-model="majorForm.enrollmentTarget">
            <el-radio value="初中毕业生">初中毕业生</el-radio>
            <el-radio value="高中毕业生">高中毕业生</el-radio>
            <el-radio value="社会人员">社会人员</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="办学形式">
          <el-radio-group v-model="majorForm.educationForm">
            <el-radio value="全日制">全日制</el-radio>
            <el-radio value="非全日制">非全日制</el-radio>
            <el-radio value="企业新型学徒制">企业新型学徒制</el-radio>
          </el-radio-group>
        </el-form-item>
        <div class="grid grid-cols-2 gap-x-4">
          <el-form-item label="审批年份">
            <el-input-number v-model="majorForm.approvalYear" :min="2000" :max="2099" :controls="false" class="w-full" placeholder="如 2024" />
          </el-form-item>
          <el-form-item label="专业状态">
            <el-select v-model="majorForm.majorStatus" placeholder="选择状态" class="w-full">
              <el-option v-for="s in STATUSES" :key="s.value" :label="s.label" :value="s.value" />
            </el-select>
          </el-form-item>
        </div>

        <!-- Section: Lead Teacher -->
        <div class="text-sm font-semibold text-gray-500 mb-3 mt-4">负责人</div>
        <el-form-item label="专业带头人">
          <el-input v-model="majorForm.leadTeacherName" placeholder="带头人姓名" />
        </el-form-item>

        <!-- Section: Description -->
        <div class="text-sm font-semibold text-gray-500 mb-3 mt-4">描述</div>
        <el-form-item label="专业描述">
          <el-input v-model="majorForm.description" type="textarea" :rows="3" placeholder="专业简介" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="majorDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="majorSaving" @click="saveMajor">保存</el-button>
      </template>
    </el-dialog>

    <!-- ═══════════ Direction Create/Edit Dialog ═══════════ -->
    <el-dialog
      v-model="directionDialogVisible"
      :title="directionForm.id ? '编辑方向' : '添加方向'"
      width="620px"
      :close-on-click-modal="false"
      @closed="resetDirectionForm"
    >
      <div v-if="directionParentMajor" class="mb-3 text-sm text-gray-400">
        所属专业: <b class="text-gray-600">{{ directionParentMajor.majorName }}</b>
      </div>
      <el-form
        ref="directionFormRef"
        :model="directionForm"
        :rules="directionRules"
        label-position="top"
        class="px-2"
      >
        <!-- Code & Name -->
        <div class="grid grid-cols-2 gap-x-4">
          <el-form-item label="方向代码" prop="directionCode">
            <div class="flex gap-2 w-full">
              <el-input v-model="directionForm.directionCode" placeholder="方向代码" class="flex-1" />
              <el-button size="small" @click="autoGenerateDirectionCode">自动生成</el-button>
            </div>
          </el-form-item>
          <el-form-item label="方向名称" prop="directionName">
            <el-input v-model="directionForm.directionName" placeholder="方向名称" />
          </el-form-item>
        </div>

        <!-- Training Mode -->
        <div class="text-sm font-semibold text-gray-500 mb-2 mt-3">培养模式</div>
        <el-form-item>
          <el-radio-group v-model="directionForm.trainingMode" @change="onTrainingModeChange">
            <el-radio value="direct">直通培养</el-radio>
            <el-radio value="segmented">分段培养</el-radio>
          </el-radio-group>
        </el-form-item>

        <!-- Direct mode -->
        <template v-if="directionForm.trainingMode === 'direct'">
          <div class="grid grid-cols-2 gap-x-4">
            <el-form-item label="培养层次" prop="level">
              <el-select v-model="directionForm.level" placeholder="选择层次" class="w-full">
                <el-option v-for="l in LEVELS" :key="l" :label="l" :value="l" />
              </el-select>
            </el-form-item>
            <el-form-item label="学制(年)" prop="years">
              <el-input-number v-model="directionForm.years" :min="1" :max="8" :controls="false" class="w-full" />
            </el-form-item>
          </div>
        </template>

        <!-- Segmented mode -->
        <template v-else>
          <div class="grid grid-cols-4 gap-x-3">
            <el-form-item label="阶段一层次" prop="phase1Level">
              <el-select v-model="directionForm.phase1Level" placeholder="层次" class="w-full" size="small">
                <el-option v-for="l in LEVELS" :key="l" :label="l" :value="l" />
              </el-select>
            </el-form-item>
            <el-form-item label="阶段一年限" prop="phase1Years">
              <el-input-number v-model="directionForm.phase1Years" :min="1" :max="6" :controls="false" class="w-full" size="small" />
            </el-form-item>
            <el-form-item label="阶段二层次" prop="phase2Level">
              <el-select v-model="directionForm.phase2Level" placeholder="层次" class="w-full" size="small">
                <el-option v-for="l in LEVELS" :key="l" :label="l" :value="l" />
              </el-select>
            </el-form-item>
            <el-form-item label="阶段二年限" prop="phase2Years">
              <el-input-number v-model="directionForm.phase2Years" :min="1" :max="6" :controls="false" class="w-full" size="small" />
            </el-form-item>
          </div>
          <div class="text-xs text-gray-400 -mt-1 mb-2">
            总学制: {{ (directionForm.phase1Years || 0) + (directionForm.phase2Years || 0) }}年
            ({{ directionForm.phase1Level || '?' }} &rarr; {{ directionForm.phase2Level || '?' }})
          </div>
        </template>

        <!-- Enrollment -->
        <div class="text-sm font-semibold text-gray-500 mb-2 mt-3">招生信息</div>
        <div class="grid grid-cols-3 gap-x-4">
          <el-form-item label="招生对象">
            <el-select v-model="directionForm.enrollmentTarget" placeholder="使用专业默认" clearable class="w-full">
              <el-option label="初中毕业生" value="初中毕业生" />
              <el-option label="高中毕业生" value="高中毕业生" />
              <el-option label="社会人员" value="社会人员" />
            </el-select>
          </el-form-item>
          <el-form-item label="办学形式">
            <el-select v-model="directionForm.educationForm" placeholder="使用专业默认" clearable class="w-full">
              <el-option label="全日制" value="全日制" />
              <el-option label="非全日制" value="非全日制" />
              <el-option label="企业新型学徒制" value="企业新型学徒制" />
            </el-select>
          </el-form-item>
          <el-form-item label="最大招生数">
            <el-input-number v-model="directionForm.maxEnrollment" :min="0" :controls="false" class="w-full" placeholder="人数" />
          </el-form-item>
        </div>

        <!-- Certificates -->
        <div class="text-sm font-semibold text-gray-500 mb-2 mt-3">证书</div>
        <el-form-item label="可获证书">
          <div class="space-y-2 w-full">
            <div class="flex flex-wrap gap-2">
              <el-tag
                v-for="(cert, i) in directionForm.certificateNames"
                :key="i"
                closable
                @close="directionForm.certificateNames.splice(i, 1)"
                type="info"
              >{{ cert }}</el-tag>
            </div>
            <div class="flex items-center gap-2">
              <el-input
                v-model="newCertName"
                placeholder="输入证书名称"
                size="small"
                class="!w-56"
                @keyup.enter="addCertificate"
              />
              <el-button size="small" @click="addCertificate" :disabled="!newCertName.trim()">
                <Plus class="h-3.5 w-3.5 mr-1" />
                添加
              </el-button>
            </div>
          </div>
        </el-form-item>

        <!-- Training & Cooperation -->
        <div class="text-sm font-semibold text-gray-500 mb-2 mt-3">培养与合作</div>
        <el-form-item label="培养标准">
          <el-input v-model="directionForm.trainingStandard" placeholder="国家/行业/企业标准名称" />
        </el-form-item>
        <el-form-item label="合作企业">
          <el-input v-model="directionForm.cooperationEnterprise" placeholder="校企合作企业名称" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="directionForm.remarks" type="textarea" :rows="2" placeholder="其他说明" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="directionDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="directionSaving" @click="saveDirection">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, h } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Plus, Search, ChevronRight } from 'lucide-vue-next'
import {
  majorApi, majorDirectionApi, curriculumPlanApi,
  type Major, type MajorDirection, type CurriculumPlan,
} from '@/api/academic'
import { getOrgUnitTree } from '@/api/organization'
import type { OrgUnitTreeNode } from '@/types/organization'

// ──────────────────────────────────────────────
// Constants
// ──────────────────────────────────────────────

const CATEGORIES = [
  { code: '01', name: '机械类' }, { code: '02', name: '电工电子类' },
  { code: '03', name: '信息类' }, { code: '04', name: '交通类' },
  { code: '05', name: '服务类' }, { code: '06', name: '财经商贸类' },
  { code: '07', name: '文化艺术类' }, { code: '08', name: '医药卫生类' },
  { code: '09', name: '农业类' }, { code: '10', name: '建筑类' },
  { code: '11', name: '化工类' }, { code: '12', name: '轻工类' },
  { code: '13', name: '教育类' }, { code: '14', name: '其他' },
]

const LEVELS = ['中级工', '高级工', '预备技师', '技师']

const STATUSES = [
  { value: 'PREPARING', label: '筹建中', class: 'text-blue-600 bg-blue-50' },
  { value: 'ENROLLING', label: '招生中', class: 'text-emerald-600 bg-emerald-50' },
  { value: 'SUSPENDED', label: '暂停招生', class: 'text-amber-600 bg-amber-50' },
  { value: 'REVOKED', label: '已撤销', class: 'text-gray-500 bg-gray-100' },
]

const VIEW_MODES = [
  { key: 'category' as const, label: '大类视图' },
  { key: 'list' as const, label: '列表视图' },
  { key: 'card' as const, label: '卡片视图' },
]

// ──────────────────────────────────────────────
// State
// ──────────────────────────────────────────────

const loading = ref(false)
const allMajors = ref<Major[]>([])
const orgTree = ref<OrgUnitTreeNode[]>([])
const viewMode = ref<'category' | 'list' | 'card'>('category')
const expandedCategories = ref<Set<string>>(new Set())

const filters = reactive({
  keyword: '',
  orgUnitId: null as number | string | null,
  majorStatus: '' as string,
})

// ──────────────────────────────────────────────
// Computed
// ──────────────────────────────────────────────

const stats = computed(() => {
  const list = allMajors.value
  let dirCount = 0
  let enrolling = 0, suspended = 0, preparing = 0
  for (const m of list) {
    dirCount += m.directions?.length || 0
    if (m.majorStatus === 'ENROLLING') enrolling++
    else if (m.majorStatus === 'SUSPENDED') suspended++
    else if (m.majorStatus === 'PREPARING') preparing++
  }
  return { total: list.length, enrolling, suspended, preparing, directionCount: dirCount }
})

const filteredMajors = computed(() => {
  let list = allMajors.value
  if (filters.keyword) {
    const kw = filters.keyword.toLowerCase()
    list = list.filter(m =>
      m.majorName.toLowerCase().includes(kw) || m.majorCode.toLowerCase().includes(kw)
    )
  }
  if (filters.orgUnitId) {
    list = list.filter(m => String(m.orgUnitId) === String(filters.orgUnitId))
  }
  if (filters.majorStatus) {
    list = list.filter(m => m.majorStatus === filters.majorStatus)
  }
  return list
})

const categoryGroups = computed(() => {
  const map = new Map<string, { code: string; name: string; majors: Major[] }>()
  for (const m of filteredMajors.value) {
    const code = m.majorCategoryCode || '14'
    if (!map.has(code)) {
      const cat = CATEGORIES.find(c => c.code === code)
      map.set(code, { code, name: cat?.name || '其他', majors: [] })
    }
    map.get(code)!.majors.push(m)
  }
  return Array.from(map.values()).sort((a, b) => a.code.localeCompare(b.code))
})

// ──────────────────────────────────────────────
// Helpers
// ──────────────────────────────────────────────

function getStatusDef(status?: string) {
  if (!status) return null
  return STATUSES.find(s => s.value === status) || null
}

function getCategoryName(code?: string): string {
  if (!code) return '-'
  return CATEGORIES.find(c => c.code === code)?.name || '未知'
}

function toggleCategory(code: string) {
  if (expandedCategories.value.has(code)) {
    expandedCategories.value.delete(code)
  } else {
    expandedCategories.value.add(code)
  }
}

// ──────────────────────────────────────────────
// Detail Drawer
// ──────────────────────────────────────────────

const detailDrawerVisible = ref(false)
const detailMajor = ref<Major | null>(null)
const detailTab = ref('info')
const detailDirections = ref<MajorDirection[]>([])
const detailPlans = ref<CurriculumPlan[]>([])
const directionsLoading = ref(false)
const plansLoading = ref(false)

async function openDetailDrawer(major: Major) {
  detailMajor.value = major
  detailTab.value = 'info'
  detailDrawerVisible.value = true
  loadDetailDirections(major.id)
  loadDetailPlans(major.id)
}

async function loadDetailDirections(majorId: number | string) {
  directionsLoading.value = true
  try {
    const res = await majorDirectionApi.getByMajor(majorId)
    detailDirections.value = Array.isArray(res) ? res : (res as any)?.records || []
  } catch {
    detailDirections.value = []
  } finally {
    directionsLoading.value = false
  }
}

async function loadDetailPlans(majorId: number | string) {
  plansLoading.value = true
  try {
    const res = await curriculumPlanApi.list({ majorId })
    detailPlans.value = (res as any)?.records || []
  } catch {
    detailPlans.value = []
  } finally {
    plansLoading.value = false
  }
}

// ──────────────────────────────────────────────
// Major CRUD
// ──────────────────────────────────────────────

const majorDialogVisible = ref(false)
const majorSaving = ref(false)
const majorFormRef = ref<FormInstance>()
const majorForm = reactive({
  id: null as number | string | null,
  majorCode: '',
  majorName: '',
  majorCategoryCode: '',
  orgUnitId: null as number | string | null,
  enrollmentTarget: '初中毕业生',
  educationForm: '全日制',
  approvalYear: undefined as number | undefined,
  majorStatus: 'ENROLLING',
  leadTeacherName: '',
  description: '',
  status: 1,
})

const majorRules: FormRules = {
  majorCode: [{ required: true, message: '请输入专业代码', trigger: 'blur' }],
  majorName: [{ required: true, message: '请输入专业名称', trigger: 'blur' }],
  majorCategoryCode: [{ required: true, message: '请选择专业大类', trigger: 'change' }],
  orgUnitId: [{ required: true, message: '请选择所属系部', trigger: 'change' }],
}

function openMajorDialog(major?: Major) {
  if (major) {
    majorForm.id = major.id
    majorForm.majorCode = major.majorCode
    majorForm.majorName = major.majorName
    majorForm.majorCategoryCode = major.majorCategoryCode || ''
    majorForm.orgUnitId = major.orgUnitId
    majorForm.enrollmentTarget = major.enrollmentTarget || '初中毕业生'
    majorForm.educationForm = major.educationForm || '全日制'
    majorForm.approvalYear = major.approvalYear
    majorForm.majorStatus = major.majorStatus || 'ENROLLING'
    majorForm.leadTeacherName = major.leadTeacherName || ''
    majorForm.description = major.description || ''
    majorForm.status = major.status
  } else {
    resetMajorForm()
  }
  majorDialogVisible.value = true
}

function resetMajorForm() {
  majorForm.id = null
  majorForm.majorCode = ''
  majorForm.majorName = ''
  majorForm.majorCategoryCode = ''
  majorForm.orgUnitId = null
  majorForm.enrollmentTarget = '初中毕业生'
  majorForm.educationForm = '全日制'
  majorForm.approvalYear = undefined
  majorForm.majorStatus = 'ENROLLING'
  majorForm.leadTeacherName = ''
  majorForm.description = ''
  majorForm.status = 1
  majorFormRef.value?.resetFields()
}

async function saveMajor() {
  const valid = await majorFormRef.value?.validate().catch(() => false)
  if (!valid) return

  majorSaving.value = true
  try {
    const data: any = {
      majorCode: majorForm.majorCode,
      majorName: majorForm.majorName,
      majorCategoryCode: majorForm.majorCategoryCode || null,
      orgUnitId: majorForm.orgUnitId,
      enrollmentTarget: majorForm.enrollmentTarget || null,
      educationForm: majorForm.educationForm || null,
      approvalYear: majorForm.approvalYear || null,
      majorStatus: majorForm.majorStatus || null,
      leadTeacherName: majorForm.leadTeacherName || null,
      description: majorForm.description || null,
      status: majorForm.status,
    }
    if (majorForm.id) {
      await majorApi.update(majorForm.id, data)
      ElMessage.success('专业已更新')
    } else {
      await majorApi.create(data)
      ElMessage.success('专业已创建')
    }
    majorDialogVisible.value = false
    await loadMajors()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e?.message || '保存失败')
  } finally {
    majorSaving.value = false
  }
}

async function handleDeleteMajor(major: Major) {
  try {
    await ElMessageBox.confirm(
      `确定删除专业「${major.majorName}」？关联的方向和培养方案也将受影响。`,
      '删除确认',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
    await majorApi.delete(major.id)
    ElMessage.success('已删除')
    await loadMajors()
  } catch { /* cancelled */ }
}

// ──────────────────────────────────────────────
// Direction CRUD
// ──────────────────────────────────────────────

const directionDialogVisible = ref(false)
const directionSaving = ref(false)
const directionFormRef = ref<FormInstance>()
const directionParentMajor = ref<Major | null>(null)
const newCertName = ref('')

const directionForm = reactive({
  id: null as number | undefined | null,
  majorId: null as number | null,
  directionCode: '',
  directionName: '',
  trainingMode: 'direct' as 'direct' | 'segmented',
  level: '中级工',
  years: 3,
  isSegmented: 0,
  phase1Level: '中级工',
  phase1Years: 3,
  phase2Level: '高级工',
  phase2Years: 2,
  enrollmentTarget: '',
  educationForm: '',
  maxEnrollment: undefined as number | undefined,
  certificateNames: [] as string[],
  trainingStandard: '',
  cooperationEnterprise: '',
  remarks: '',
})

const directionRules: FormRules = {
  directionCode: [{ required: true, message: '请输入方向代码', trigger: 'blur' }],
  directionName: [{ required: true, message: '请输入方向名称', trigger: 'blur' }],
  level: [{ required: true, message: '请选择培养层次', trigger: 'change' }],
  years: [{ required: true, message: '请输入学制', trigger: 'blur' }],
  phase1Level: [{ required: true, message: '请选择阶段一层次', trigger: 'change' }],
  phase1Years: [{ required: true, message: '请输入阶段一年限', trigger: 'blur' }],
  phase2Level: [{ required: true, message: '请选择阶段二层次', trigger: 'change' }],
  phase2Years: [{ required: true, message: '请输入阶段二年限', trigger: 'blur' }],
}

function openDirectionDialog(major: Major, dir?: MajorDirection) {
  directionParentMajor.value = major
  newCertName.value = ''
  if (dir) {
    directionForm.id = dir.id
    directionForm.majorId = Number(major.id)
    directionForm.directionCode = dir.directionCode
    directionForm.directionName = dir.directionName
    directionForm.trainingMode = dir.isSegmented ? 'segmented' : 'direct'
    directionForm.level = dir.level || '中级工'
    directionForm.years = dir.years || 3
    directionForm.isSegmented = dir.isSegmented || 0
    directionForm.phase1Level = dir.phase1Level || '中级工'
    directionForm.phase1Years = dir.phase1Years || 3
    directionForm.phase2Level = dir.phase2Level || '高级工'
    directionForm.phase2Years = dir.phase2Years || 2
    directionForm.enrollmentTarget = dir.enrollmentTarget || ''
    directionForm.educationForm = dir.educationForm || ''
    directionForm.maxEnrollment = dir.maxEnrollment
    directionForm.certificateNames = dir.certificateNames ? [...dir.certificateNames] : []
    directionForm.trainingStandard = dir.trainingStandard || ''
    directionForm.cooperationEnterprise = dir.cooperationEnterprise || ''
    directionForm.remarks = dir.remarks || ''
  } else {
    resetDirectionForm()
    directionForm.majorId = Number(major.id)
  }
  directionDialogVisible.value = true
}

function resetDirectionForm() {
  directionForm.id = null
  directionForm.majorId = null
  directionForm.directionCode = ''
  directionForm.directionName = ''
  directionForm.trainingMode = 'direct'
  directionForm.level = '中级工'
  directionForm.years = 3
  directionForm.isSegmented = 0
  directionForm.phase1Level = '中级工'
  directionForm.phase1Years = 3
  directionForm.phase2Level = '高级工'
  directionForm.phase2Years = 2
  directionForm.enrollmentTarget = ''
  directionForm.educationForm = ''
  directionForm.maxEnrollment = undefined
  directionForm.certificateNames = []
  directionForm.trainingStandard = ''
  directionForm.cooperationEnterprise = ''
  directionForm.remarks = ''
  newCertName.value = ''
  directionFormRef.value?.resetFields()
}

function onTrainingModeChange(mode: string | number | boolean | undefined) {
  directionForm.isSegmented = mode === 'segmented' ? 1 : 0
}

function autoGenerateDirectionCode() {
  if (!directionParentMajor.value) return
  const base = directionParentMajor.value.majorCode || '0000'
  const existingCount = detailDirections.value.length
    || allMajors.value.find(m => m.id === directionParentMajor.value?.id)?.directions?.length
    || 0
  directionForm.directionCode = `${base}-${String(existingCount + 1).padStart(2, '0')}`
}

function addCertificate() {
  const name = newCertName.value.trim()
  if (!name) return
  if (!directionForm.certificateNames.includes(name)) {
    directionForm.certificateNames.push(name)
  }
  newCertName.value = ''
}

async function saveDirection() {
  const valid = await directionFormRef.value?.validate().catch(() => false)
  if (!valid) return

  directionSaving.value = true
  try {
    const isSegmented = directionForm.trainingMode === 'segmented'
    const data: any = {
      majorId: directionForm.majorId,
      directionCode: directionForm.directionCode,
      directionName: directionForm.directionName,
      level: isSegmented ? directionForm.phase2Level : directionForm.level,
      years: isSegmented ? (directionForm.phase1Years || 0) + (directionForm.phase2Years || 0) : directionForm.years,
      isSegmented: isSegmented ? 1 : 0,
      phase1Level: isSegmented ? directionForm.phase1Level : null,
      phase1Years: isSegmented ? directionForm.phase1Years : null,
      phase2Level: isSegmented ? directionForm.phase2Level : null,
      phase2Years: isSegmented ? directionForm.phase2Years : null,
      enrollmentTarget: directionForm.enrollmentTarget || null,
      educationForm: directionForm.educationForm || null,
      maxEnrollment: directionForm.maxEnrollment || null,
      certificateNames: directionForm.certificateNames.filter(c => c.trim()),
      trainingStandard: directionForm.trainingStandard || null,
      cooperationEnterprise: directionForm.cooperationEnterprise || null,
      remarks: directionForm.remarks || null,
    }

    if (directionForm.id) {
      await majorDirectionApi.update(directionForm.id, data)
      ElMessage.success('方向已更新')
    } else {
      await majorDirectionApi.create(data)
      ElMessage.success('方向已创建')
    }
    directionDialogVisible.value = false
    await loadMajors()
    if (detailDrawerVisible.value && detailMajor.value) {
      loadDetailDirections(detailMajor.value.id)
    }
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e?.message || '保存失败')
  } finally {
    directionSaving.value = false
  }
}

async function handleDeleteDirection(dir: MajorDirection) {
  if (!dir.id) return
  try {
    await ElMessageBox.confirm(
      `确定删除方向「${dir.directionName}」？`,
      '删除确认',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
    await majorDirectionApi.delete(dir.id)
    ElMessage.success('已删除')
    await loadMajors()
    if (detailMajor.value) {
      loadDetailDirections(detailMajor.value.id)
    }
  } catch { /* cancelled */ }
}

// ──────────────────────────────────────────────
// Data Loading
// ──────────────────────────────────────────────

async function loadMajors() {
  loading.value = true
  try {
    const params: any = { pageNum: 1, pageSize: 500 }
    if (filters.keyword) params.majorName = filters.keyword
    if (filters.orgUnitId) params.orgUnitId = filters.orgUnitId
    if (filters.majorStatus) params.majorStatus = filters.majorStatus

    const res = await majorApi.list(params)
    const records = (res as any)?.records || (Array.isArray(res) ? res : [])
    allMajors.value = records

    // Load directions for each major
    await loadAllDirections(records)

    // Auto-expand all categories that have data
    for (const m of records) {
      if (m.majorCategoryCode) {
        expandedCategories.value.add(m.majorCategoryCode)
      }
    }
  } catch {
    ElMessage.error('加载专业列表失败')
    allMajors.value = []
  } finally {
    loading.value = false
  }
}

async function loadAllDirections(majors: Major[]) {
  const promises = majors.map(async (m) => {
    try {
      const dirs = await majorDirectionApi.getByMajor(m.id)
      m.directions = Array.isArray(dirs) ? dirs : (dirs as any)?.records || []
    } catch {
      m.directions = []
    }
  })
  await Promise.all(promises)
}

async function loadOrgTree() {
  try {
    orgTree.value = await getOrgUnitTree()
  } catch {
    orgTree.value = []
  }
}

// ──────────────────────────────────────────────
// Init
// ──────────────────────────────────────────────

onMounted(() => {
  loadOrgTree()
  loadMajors()
})

// ──────────────────────────────────────────────
// Inline sub-components
// ──────────────────────────────────────────────

const InfoRow = {
  props: { label: String, value: String },
  setup(props: { label?: string; value?: string }, { slots }: any) {
    return () => h('div', { class: 'flex items-center gap-2 py-2 border-b border-gray-100 text-sm' }, [
      h('span', { class: 'w-24 shrink-0 text-gray-400' }, props.label),
      slots?.value ? slots.value() : h('span', { class: 'text-gray-700' }, props.value || '-'),
    ])
  },
}
</script>

<script lang="ts">
export default { name: 'MajorListView' }
</script>

<style scoped>
.detail-tabs :deep(.el-tabs__header) {
  margin-bottom: 16px;
}
.detail-tabs :deep(.el-tabs__item) {
  font-size: 14px;
}
</style>
