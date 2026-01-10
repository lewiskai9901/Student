<template>
  <div class="min-h-screen bg-gray-50">
    <!-- 加载状态 -->
    <div v-if="pageLoading" class="flex h-screen items-center justify-center">
      <div class="flex flex-col items-center gap-4">
        <div class="relative">
          <div class="h-16 w-16 animate-spin rounded-full border-4 border-blue-200 border-t-blue-600"></div>
          <ClipboardCheck class="absolute left-1/2 top-1/2 h-6 w-6 -translate-x-1/2 -translate-y-1/2 text-blue-600" />
        </div>
        <span class="text-gray-500">正在加载检查数据...</span>
      </div>
    </div>

    <template v-else>
      <!-- 顶部导航栏 - 固定 -->
      <div class="sticky top-0 z-40 border-b border-gray-200 bg-white/95 backdrop-blur-sm">
        <div class="flex items-center justify-between px-6 py-3">
          <!-- 左侧：返回 + 检查信息 -->
          <div class="flex items-center gap-4">
            <button
              @click="goBack"
              class="flex items-center gap-2 rounded-lg px-3 py-2 text-gray-600 transition-colors hover:bg-gray-100 hover:text-gray-900"
            >
              <ArrowLeft class="h-5 w-5" />
              <span class="text-sm font-medium">返回</span>
            </button>
            <div class="h-6 w-px bg-gray-200"></div>
            <div class="flex items-center gap-3">
              <div class="flex h-10 w-10 items-center justify-center rounded-xl bg-gradient-to-br from-blue-500 to-blue-600 shadow-lg shadow-blue-500/30">
                <ClipboardCheck class="h-5 w-5 text-white" />
              </div>
              <div>
                <h1 class="font-semibold text-gray-900">{{ initData.checkName || '日常检查' }}</h1>
                <div class="flex items-center gap-2 text-xs text-gray-500">
                  <Calendar class="h-3.5 w-3.5" />
                  <span>{{ initData.checkDate }}</span>
                  <span class="text-gray-300">|</span>
                  <span>{{ initData.templateName || '自定义模板' }}</span>
                </div>
              </div>
            </div>
          </div>

          <!-- 中间：快捷录入 + 保存状态 -->
          <div class="flex items-center gap-4">
            <!-- 快捷录入按钮 -->
            <button
              @click="showQuickEntryDialog = true"
              class="flex items-center gap-2 rounded-lg bg-gradient-to-r from-amber-500 to-orange-500 px-4 py-2 text-sm font-medium text-white shadow-md shadow-orange-500/30 transition-all hover:shadow-lg hover:shadow-orange-500/40"
            >
              <Zap class="h-4 w-4" />
              <span>快捷录入</span>
            </button>
            <div
              v-if="autoSaveStatus !== 'idle'"
              :class="[
                'flex items-center gap-2 rounded-full px-4 py-1.5 text-sm font-medium transition-all',
                autoSaveStatus === 'saving' ? 'bg-blue-50 text-blue-600' : 'bg-green-50 text-green-600'
              ]"
            >
              <Loader2 v-if="autoSaveStatus === 'saving'" class="h-4 w-4 animate-spin" />
              <CheckCircle v-else class="h-4 w-4" />
              <span>{{ autoSaveStatus === 'saving' ? '保存中...' : '已保存' }}</span>
            </div>
          </div>

          <!-- 右侧：扣分统计 -->
          <div class="flex items-center gap-6">
            <!-- 加权配置信息 -->
            <div v-if="hasWeightConfig" class="group relative">
              <div class="flex items-center gap-2 rounded-lg bg-gradient-to-r from-amber-50 to-orange-50 px-4 py-2 border border-amber-200 cursor-help">
                <Scale class="h-4 w-4 text-amber-600" />
                <span class="text-sm font-medium text-amber-700">{{ weightConfigName }}</span>
                <Info class="h-3.5 w-3.5 text-amber-400" />
              </div>
              <!-- 悬停显示加权详情 -->
              <div class="absolute right-0 top-full z-50 mt-2 hidden w-72 rounded-xl border border-gray-200 bg-white p-4 shadow-xl group-hover:block">
                <h4 class="mb-2 font-semibold text-gray-900">加权配置详情</h4>
                <div class="space-y-2 text-sm">
                  <div class="flex justify-between">
                    <span class="text-gray-500">配置名称</span>
                    <span class="font-medium text-gray-900">{{ weightConfigName }}</span>
                  </div>
                  <div class="flex justify-between">
                    <span class="text-gray-500">加权模式</span>
                    <span class="font-medium text-gray-900">{{ weightModeDescription }}</span>
                  </div>
                  <div v-if="weightConfig?.minWeight || weightConfig?.maxWeight" class="flex justify-between">
                    <span class="text-gray-500">权重限制</span>
                    <span class="font-medium text-gray-900">{{ weightConfig?.minWeight || '无' }} ~ {{ weightConfig?.maxWeight || '无' }}</span>
                  </div>
                </div>
              </div>
            </div>
            <!-- 原始扣分 -->
            <div class="text-center px-4 py-1 rounded-lg bg-red-50">
              <div class="text-xs text-gray-500">原始扣分</div>
              <div class="text-xl font-bold text-red-600">-{{ totalScore }}</div>
            </div>
            <!-- 加权后扣分 -->
            <div v-if="hasWeightConfig" class="text-center px-4 py-1 rounded-lg bg-orange-50">
              <div class="text-xs text-gray-500">加权后</div>
              <div class="text-xl font-bold text-orange-600">-{{ weightedTotalScore }}</div>
            </div>
          </div>
        </div>
      </div>

      <!-- 主内容区域 -->
      <div class="flex" style="height: calc(100vh - 72px);">
        <!-- 左侧边栏：类别优先模式 -->
        <div class="w-96 flex-shrink-0 border-r border-gray-200 bg-white flex flex-col">
          <!-- 全局轮次选择（始终显示） -->
          <div class="border-b border-gray-100 p-4 flex-shrink-0">
            <div class="flex items-center gap-2 mb-2">
              <RefreshCw class="h-4 w-4 text-indigo-500" />
              <span class="text-sm font-semibold text-gray-700">检查轮次</span>
              <span class="text-xs text-gray-400">({{ availableCategoriesForRound.length }}个类别)</span>
            </div>
            <div class="flex flex-wrap gap-2">
              <button
                v-for="round in globalTotalRounds"
                :key="round"
                @click="handleRoundChange(round)"
                :class="[
                  'rounded-lg px-4 py-2 text-sm font-semibold transition-all',
                  activeCheckRound === round
                    ? 'bg-gradient-to-r from-indigo-600 to-purple-600 text-white shadow-md'
                    : 'bg-gray-100 text-indigo-600 hover:bg-indigo-50'
                ]"
              >
                {{ getRoundName(round) }}
              </button>
            </div>
          </div>

          <!-- 检查类别（根据当前轮次筛选） -->
          <div class="border-b border-gray-100 p-4 flex-shrink-0">
            <h3 class="mb-3 text-sm font-semibold text-gray-700 flex items-center gap-2">
              <FolderOpen class="h-4 w-4 text-emerald-500" />
              检查类别
              <span class="text-xs text-gray-400 font-normal">
                ({{ getRoundName(activeCheckRound) }})
              </span>
            </h3>
            <div v-if="availableCategoriesForRound.length > 0" class="grid grid-cols-2 gap-2">
              <button
                v-for="category in availableCategoriesForRound"
                :key="category.categoryId"
                @click="handleCategoryChange(String(category.categoryId))"
                :class="[
                  'relative flex flex-col items-start rounded-lg px-3 py-2 text-left transition-all',
                  activeCategoryId === String(category.categoryId)
                    ? 'bg-gradient-to-r from-emerald-500 to-green-500 text-white shadow-md'
                    : 'bg-gray-50 text-gray-700 hover:bg-gray-100'
                ]"
              >
                <div class="flex items-center gap-1.5">
                  <span class="text-xs font-medium truncate max-w-[100px]">{{ category.categoryName }}</span>
                  <Home v-if="category.linkType === 1" :class="['h-3 w-3', activeCategoryId === String(category.categoryId) ? 'text-white/70' : 'text-purple-500']" />
                  <Building v-else-if="category.linkType === 2" :class="['h-3 w-3', activeCategoryId === String(category.categoryId) ? 'text-white/70' : 'text-purple-500']" />
                </div>
                <span v-if="getCategoryAllScore(category.categoryId) > 0" :class="[
                  'absolute -top-1 -right-1 rounded-full px-1.5 py-0.5 text-[10px] font-bold',
                  activeCategoryId === String(category.categoryId) ? 'bg-white text-emerald-600' : 'bg-red-500 text-white'
                ]">
                  -{{ getCategoryAllScore(category.categoryId) }}
                </span>
              </button>
            </div>
            <div v-else class="text-center py-4 text-gray-400 text-sm">
              当前轮次无可用类别
            </div>
          </div>

          <!-- 目标选择：根据类别类型显示不同内容 -->
          <div class="flex-1 overflow-y-auto p-4">
            <!-- 有关联资源的类别：直接显示宿舍/教室列表 -->
            <template v-if="currentCategory?.linkType > 0">
              <div class="flex items-center justify-between mb-3">
                <h3 class="text-sm font-semibold text-gray-700 flex items-center gap-2">
                  <Home v-if="currentCategory.linkType === 1" class="h-4 w-4 text-purple-500" />
                  <Building v-else class="h-4 w-4 text-purple-500" />
                  {{ currentCategory.linkType === 1 ? '选择宿舍' : '选择教室' }}
                </h3>
                <span class="rounded bg-purple-100 px-2 py-0.5 text-xs font-medium text-purple-700">
                  {{ filteredLinkResourcesCount }}/{{ allLinkResources.length }}
                </span>
              </div>

              <!-- 筛选条件 -->
              <div class="mb-3 space-y-2">
                <!-- 年级筛选 -->
                <div class="flex items-center gap-2">
                  <span class="text-xs text-gray-500 w-10 flex-shrink-0">年级:</span>
                  <div class="flex flex-wrap gap-1 flex-1">
                    <button
                      @click="filterLinkGrade = ''"
                      :class="[
                        'rounded px-2 py-0.5 text-xs transition-all',
                        filterLinkGrade === '' ? 'bg-purple-500 text-white' : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                      ]"
                    >全部</button>
                    <button
                      v-for="grade in linkGradeOptions"
                      :key="grade.id"
                      @click="filterLinkGrade = String(grade.id)"
                      :class="[
                        'rounded px-2 py-0.5 text-xs transition-all',
                        filterLinkGrade === String(grade.id) ? 'bg-purple-500 text-white' : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                      ]"
                    >{{ grade.name }}</button>
                  </div>
                </div>
                <!-- 部门筛选 -->
                <div v-if="linkDepartmentOptions.length > 1" class="flex items-center gap-2">
                  <span class="text-xs text-gray-500 w-10 flex-shrink-0">部门:</span>
                  <div class="flex flex-wrap gap-1 flex-1">
                    <button
                      @click="filterLinkDepartment = ''"
                      :class="[
                        'rounded px-2 py-0.5 text-xs transition-all',
                        filterLinkDepartment === '' ? 'bg-indigo-500 text-white' : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                      ]"
                    >全部</button>
                    <button
                      v-for="dept in linkDepartmentOptions"
                      :key="dept.id"
                      @click="filterLinkDepartment = String(dept.id)"
                      :class="[
                        'rounded px-2 py-0.5 text-xs transition-all truncate max-w-[80px]',
                        filterLinkDepartment === String(dept.id) ? 'bg-indigo-500 text-white' : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                      ]"
                      :title="dept.name"
                    >{{ dept.name }}</button>
                  </div>
                </div>
                <!-- 班级筛选 -->
                <div v-if="linkClassOptions.length > 1" class="flex items-center gap-2">
                  <span class="text-xs text-gray-500 w-10 flex-shrink-0">班级:</span>
                  <div class="flex flex-wrap gap-1 flex-1 max-h-20 overflow-y-auto">
                    <button
                      @click="filterLinkClass = ''"
                      :class="[
                        'rounded px-2 py-0.5 text-xs transition-all',
                        filterLinkClass === '' ? 'bg-blue-500 text-white' : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                      ]"
                    >全部</button>
                    <button
                      v-for="cls in linkClassOptions"
                      :key="cls.id"
                      @click="filterLinkClass = String(cls.id)"
                      :class="[
                        'rounded px-2 py-0.5 text-xs transition-all truncate max-w-[100px]',
                        filterLinkClass === String(cls.id) ? 'bg-blue-500 text-white' : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                      ]"
                      :title="cls.name"
                    >{{ cls.name }}</button>
                  </div>
                </div>
                <!-- 楼栋筛选 -->
                <div v-if="linkBuildingOptions.length > 1" class="flex items-center gap-2">
                  <span class="text-xs text-gray-500 w-10 flex-shrink-0">楼栋:</span>
                  <div class="flex flex-wrap gap-1 flex-1">
                    <button
                      @click="filterLinkBuilding = ''"
                      :class="[
                        'rounded px-2 py-0.5 text-xs transition-all',
                        filterLinkBuilding === '' ? 'bg-orange-500 text-white' : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                      ]"
                    >全部</button>
                    <button
                      v-for="building in linkBuildingOptions"
                      :key="building"
                      @click="filterLinkBuilding = building"
                      :class="[
                        'rounded px-2 py-0.5 text-xs transition-all',
                        filterLinkBuilding === building ? 'bg-orange-500 text-white' : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                      ]"
                    >{{ building }}</button>
                  </div>
                </div>
              </div>

              <!-- 按楼栋分组显示（网格布局） -->
              <div class="space-y-4">
                <div v-for="building in filteredLinkResourcesByBuilding" :key="building.buildingName" class="space-y-2">
                  <!-- 楼栋标题 -->
                  <div class="flex items-center gap-2 text-xs font-semibold text-gray-600 bg-gray-100 rounded px-2 py-1">
                    <Building class="h-3.5 w-3.5 text-gray-500" />
                    <span>{{ building.buildingName }}</span>
                    <span class="text-gray-400 font-normal">({{ building.resources.length }})</span>
                  </div>
                  <!-- 宿舍/教室网格 -->
                  <div class="grid grid-cols-5 gap-1">
                    <button
                      v-for="link in building.resources"
                      :key="`${link.classId}-${link.id}`"
                      @click="handleLinkSelect(link.classId, link)"
                      :class="[
                        'relative rounded px-2 py-1.5 text-center transition-all text-xs font-medium',
                        activeLinkId === String(link.id) && activeClassId === String(link.classId)
                          ? 'bg-purple-600 text-white shadow-md'
                          : 'bg-gray-50 text-gray-700 hover:bg-purple-50 hover:text-purple-700'
                      ]"
                    >
                      {{ link.no }}
                      <!-- 扣分标记 -->
                      <div v-if="getLinkTotalScoreAll(link.id, link.classId) > 0" :class="[
                        'absolute -top-1 -right-1 rounded-full px-1 py-0 text-[10px] font-bold',
                        activeLinkId === String(link.id) && activeClassId === String(link.classId) ? 'bg-white text-purple-600' : 'bg-red-500 text-white'
                      ]">
                        -{{ getLinkTotalScoreAll(link.id, link.classId) }}
                      </div>
                    </button>
                  </div>
                </div>
              </div>

              <!-- 无资源提示 -->
              <div v-if="allLinkResources.length === 0" class="py-8 text-center">
                <Home v-if="currentCategory.linkType === 1" class="mx-auto h-10 w-10 text-gray-300" />
                <Building v-else class="mx-auto h-10 w-10 text-gray-300" />
                <p class="mt-2 text-sm text-gray-400">暂无{{ currentCategory.linkType === 1 ? '宿舍' : '教室' }}数据</p>
              </div>
            </template>

            <!-- 无关联资源的类别：显示班级列表 -->
            <template v-else>
              <div class="flex items-center justify-between mb-3">
                <h3 class="text-sm font-semibold text-gray-700 flex items-center gap-2">
                  <School class="h-4 w-4 text-blue-500" />
                  选择班级
                </h3>
                <span class="rounded bg-blue-100 px-2 py-0.5 text-xs font-medium text-blue-700">
                  {{ filteredClasses.length }}/{{ initData.targetClasses?.length || 0 }}
                </span>
              </div>

              <!-- 筛选条件 -->
              <div class="mb-3 space-y-2">
                <!-- 年级筛选 -->
                <div class="flex items-center gap-2">
                  <span class="text-xs text-gray-500 w-10 flex-shrink-0">年级:</span>
                  <div class="flex flex-wrap gap-1 flex-1">
                    <button
                      @click="filterGrade = ''"
                      :class="[
                        'rounded px-2 py-0.5 text-xs transition-all',
                        filterGrade === '' ? 'bg-blue-500 text-white' : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                      ]"
                    >全部</button>
                    <button
                      v-for="grade in gradeOptions"
                      :key="grade.id"
                      @click="filterGrade = String(grade.id)"
                      :class="[
                        'rounded px-2 py-0.5 text-xs transition-all',
                        filterGrade === String(grade.id) ? 'bg-blue-500 text-white' : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                      ]"
                    >{{ grade.name }}</button>
                  </div>
                </div>
                <!-- 部门筛选 -->
                <div v-if="departmentOptions.length > 1" class="flex items-center gap-2">
                  <span class="text-xs text-gray-500 w-10 flex-shrink-0">部门:</span>
                  <div class="flex flex-wrap gap-1 flex-1">
                    <button
                      @click="filterDepartment = ''"
                      :class="[
                        'rounded px-2 py-0.5 text-xs transition-all',
                        filterDepartment === '' ? 'bg-indigo-500 text-white' : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                      ]"
                    >全部</button>
                    <button
                      v-for="dept in departmentOptions"
                      :key="dept.id"
                      @click="filterDepartment = String(dept.id)"
                      :class="[
                        'rounded px-2 py-0.5 text-xs transition-all truncate max-w-[80px]',
                        filterDepartment === String(dept.id) ? 'bg-indigo-500 text-white' : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                      ]"
                      :title="dept.name"
                    >{{ dept.name }}</button>
                  </div>
                </div>
              </div>

              <!-- 班级网格 -->
              <div class="grid grid-cols-2 gap-2">
                <button
                  v-for="classItem in filteredClasses"
                  :key="classItem.classId"
                  @click="handleClassChange(String(classItem.classId))"
                  :class="[
                    'relative group flex flex-col rounded-lg px-2.5 py-2 text-left transition-all',
                    activeClassId === String(classItem.classId)
                      ? 'bg-blue-600 text-white shadow-lg shadow-blue-600/30'
                      : 'bg-gray-50 text-gray-700 hover:bg-gray-100'
                  ]"
                >
                  <div class="flex items-center gap-1.5">
                    <School :class="['h-3.5 w-3.5 flex-shrink-0', activeClassId === String(classItem.classId) ? 'text-blue-200' : 'text-gray-400']" />
                    <span class="text-xs font-medium truncate">{{ classItem.className }}</span>
                  </div>
                  <!-- 年级+人数 -->
                  <div :class="[
                    'mt-1 flex items-center gap-2 text-[10px]',
                    activeClassId === String(classItem.classId) ? 'text-blue-200' : 'text-gray-400'
                  ]">
                    <span>{{ classItem.gradeName || '' }}</span>
                    <span v-if="classItem.studentCount" class="flex items-center gap-0.5">
                      <Users class="h-2.5 w-2.5" />{{ classItem.studentCount }}
                    </span>
                  </div>
                  <!-- 扣分标记 -->
                  <span v-if="getClassCategoryScore(classItem.classId) > 0" :class="[
                    'absolute -top-1 -right-1 rounded-full px-1.5 py-0.5 text-[10px] font-bold',
                    activeClassId === String(classItem.classId) ? 'bg-white text-blue-600' : 'bg-red-500 text-white'
                  ]">
                    -{{ getClassCategoryScore(classItem.classId) }}
                  </span>
                </button>
              </div>

              <!-- 无数据提示 -->
              <div v-if="filteredClasses.length === 0" class="py-8 text-center">
                <School class="mx-auto h-10 w-10 text-gray-300" />
                <p class="mt-2 text-sm text-gray-400">无匹配班级</p>
              </div>
            </template>
          </div>
        </div>

        <!-- 右侧主区域：扣分项 -->
        <div class="flex-1 overflow-hidden bg-gray-50">
          <!-- 当前位置指示器 -->
          <div class="flex items-center gap-2 border-b border-gray-200 bg-white px-6 py-3">
            <span class="rounded bg-blue-100 px-2 py-1 text-xs font-medium text-blue-700">{{ currentClassName }}</span>
            <ChevronRight class="h-4 w-4 text-gray-300" />
            <span class="rounded bg-emerald-100 px-2 py-1 text-xs font-medium text-emerald-700">{{ currentCategoryName }}</span>
            <template v-if="currentLinkNo">
              <ChevronRight class="h-4 w-4 text-gray-300" />
              <span class="rounded bg-purple-100 px-2 py-1 text-xs font-medium text-purple-700">{{ currentLinkNo }}</span>
            </template>
            <!-- 轮次显示 - 始终显示 -->
            <ChevronRight class="h-4 w-4 text-gray-300" />
            <span class="rounded bg-indigo-100 px-2 py-1 text-xs font-medium text-indigo-700">{{ getRoundName(activeCheckRound) }}</span>
            <div class="ml-auto flex items-center gap-4 text-sm">
              <span class="text-gray-500">扣分项: <span class="font-semibold text-gray-900">{{ deductionItems.length }}</span></span>
              <span class="text-gray-500">已扣分: <span class="font-semibold text-red-600">{{ currentDeductedCount }}</span></span>
              <!-- 当前班级加权信息 -->
              <template v-if="hasWeightConfig">
                <span class="h-4 w-px bg-gray-200"></span>
                <span class="text-gray-500">
                  本班原始: <span class="font-semibold text-red-600">-{{ currentClassScore }}</span>
                </span>
                <span class="text-gray-500">
                  加权后: <span class="font-semibold text-orange-600">-{{ currentClassWeightedScore }}</span>
                </span>
                <span class="text-amber-600 text-xs">
                  (×{{ currentClassWeightFactor.toFixed(2) }})
                </span>
              </template>
            </div>
          </div>

          <!-- 扣分项网格 -->
          <div class="h-full overflow-y-auto p-6">
            <div v-if="deductionItemsLoading" class="flex items-center justify-center py-20">
              <Loader2 class="h-8 w-8 animate-spin text-blue-600" />
            </div>
            <!-- 需要关联资源但没有关联资源时，显示提示 -->
            <div v-else-if="requiresLinkResource && !hasLinkResource" class="flex flex-col items-center justify-center py-20">
              <div class="flex h-20 w-20 items-center justify-center rounded-full bg-amber-100">
                <AlertCircle class="h-10 w-10 text-amber-500" />
              </div>
              <p class="mt-4 text-lg font-medium text-gray-700">该班级暂无关联资源</p>
              <p class="mt-2 text-sm text-gray-500">
                当前检查类别需要关联{{ currentCategory?.linkType === 1 ? '宿舍' : '教室' }}，但该班级未配置相关资源
              </p>
              <p class="mt-1 text-xs text-gray-400">请先在系统中为该班级关联{{ currentCategory?.linkType === 1 ? '宿舍' : '教室' }}后再进行检查</p>
            </div>
            <div v-else-if="deductionItems.length > 0" class="grid grid-cols-1 gap-4 lg:grid-cols-2 xl:grid-cols-3 2xl:grid-cols-4">
              <DeductionCard
                v-for="item in deductionItems"
                :key="item.id"
                :item="item"
                :is-deducted="isItemDeducted(item.id)"
                :deducted-data="getDeductedData(item.id)"
                :category-id="activeCategoryId"
                :class-id="activeClassId"
                :link-id="activeLinkId"
                :link-type="currentCategory?.linkType || 0"
                :link-no="currentLinkNo"
                :has-weight="hasWeightConfig && currentCategory?.enableWeight === 1"
                :weight-config-name="weightConfigName"
                :weight-factor="currentClassWeightFactor"
                :check-round="activeCheckRound"
                @toggle="handleToggleDeduct"
                @update="handleUpdateDeduct"
              />
            </div>
            <div v-else class="flex flex-col items-center justify-center py-20">
              <AlertCircle class="h-16 w-16 text-gray-300" />
              <p class="mt-4 text-gray-500">该类别暂无扣分项</p>
            </div>
          </div>
        </div>
      </div>
    </template>

    <!-- 快捷录入弹窗 -->
    <QuickEntryDialog
      v-model="showQuickEntryDialog"
      :check-id="checkId"
      :total-rounds="globalTotalRounds"
      :round-names="globalRoundNames"
      :current-round="activeCheckRound"
      @refresh="loadInitData"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  ArrowLeft, ClipboardCheck, Calendar, Loader2, CheckCircle, School, FolderOpen,
  Home, Building, ChevronRight, Scale, AlertCircle, Info, Users, TrendingUp, RefreshCw,
  Zap
} from 'lucide-vue-next'
import http from '@/utils/request'
import { useAuthStore } from '@/stores/auth'
import { getEnabledDeductionItemsByTypeId, type DeductionItem } from '@/api/v2/quantification'
import { getWeightConfig, calculateWeightedScore, type ClassWeightConfig, type ClassWeightResult } from '@/api/v2/quantification-extra'
import DeductionCard from './components/DeductionCard.vue'
import QuickEntryDialog from '@/components/quantification/QuickEntryDialog.vue'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const checkId = computed(() => {
  const id = route.query.checkId
  return id ? String(id) : ''
})

// 来源路由（用于返回）
const fromRoute = computed(() => route.query.from as string || '')

const pageLoading = ref(false)
const deductionItemsLoading = ref(false)
const autoSaveStatus = ref<'idle' | 'saving' | 'saved'>('idle')
let autoSaveTimer: NodeJS.Timeout | null = null

// 快捷录入弹窗
const showQuickEntryDialog = ref(false)

const initData = reactive<any>({
  checkId: null,
  checkName: '',
  checkDate: '',
  templateName: '',
  targetClasses: [],
  categories: [],
  linkResources: {},
  existingDetails: [],
  weightConfigId: null,
  weightConfigName: '',
  totalRounds: 1,           // 全局总轮次数
  roundNames: [] as string[] // 全局轮次名称列表
})

// 加权配置详情
const weightConfig = ref<ClassWeightConfig | null>(null)
// 班级加权结果缓存
const classWeightResults = ref<Map<string, ClassWeightResult>>(new Map())

const activeClassId = ref('')
const activeCategoryId = ref('')
const activeLinkId = ref('')
const activeCheckRound = ref(1)  // 当前选中的检查轮次
const scoringDetails = ref<any[]>([])
const deductionItems = ref<DeductionItem[]>([])

// 班级筛选
const filterGrade = ref('')
const filterDepartment = ref('')

// 年级选项（从班级数据中提取）
const gradeOptions = computed(() => {
  const gradeMap = new Map<string, { id: string; name: string }>()
  initData.targetClasses?.forEach((cls: any) => {
    if (cls.gradeId && cls.gradeName) {
      gradeMap.set(String(cls.gradeId), { id: String(cls.gradeId), name: cls.gradeName })
    }
  })
  return Array.from(gradeMap.values()).sort((a, b) => b.name.localeCompare(a.name, 'zh-CN'))
})

// 部门选项（从班级数据中提取）
const departmentOptions = computed(() => {
  const deptMap = new Map<string, { id: string; name: string }>()
  initData.targetClasses?.forEach((cls: any) => {
    if (cls.departmentId && cls.departmentName) {
      deptMap.set(String(cls.departmentId), { id: String(cls.departmentId), name: cls.departmentName })
    }
  })
  return Array.from(deptMap.values()).sort((a, b) => a.name.localeCompare(b.name, 'zh-CN'))
})

// 筛选后的班级列表
const filteredClasses = computed(() => {
  let result = initData.targetClasses || []
  if (filterGrade.value) {
    result = result.filter((cls: any) => String(cls.gradeId) === filterGrade.value)
  }
  if (filterDepartment.value) {
    result = result.filter((cls: any) => String(cls.departmentId) === filterDepartment.value)
  }
  return result
})

// 宿舍/教室筛选
const filterLinkGrade = ref('')
const filterLinkDepartment = ref('')
const filterLinkClass = ref('')
const filterLinkBuilding = ref('')

// 宿舍年级选项（从关联资源的班级中提取）
const linkGradeOptions = computed(() => {
  if (!currentCategory.value?.linkType) return []
  const linkResource = initData.linkResources[activeCategoryId.value]
  if (!linkResource?.classResources) return []

  const gradeMap = new Map<string, { id: string; name: string }>()
  linkResource.classResources.forEach((cr: any) => {
    const classItem = initData.targetClasses?.find((c: any) => c.classId === cr.classId)
    if (classItem?.gradeId && classItem?.gradeName) {
      gradeMap.set(String(classItem.gradeId), { id: String(classItem.gradeId), name: classItem.gradeName })
    }
  })
  return Array.from(gradeMap.values()).sort((a, b) => b.name.localeCompare(a.name, 'zh-CN'))
})

// 宿舍部门选项
const linkDepartmentOptions = computed(() => {
  if (!currentCategory.value?.linkType) return []
  const linkResource = initData.linkResources[activeCategoryId.value]
  if (!linkResource?.classResources) return []

  const deptMap = new Map<string, { id: string; name: string }>()
  linkResource.classResources.forEach((cr: any) => {
    const classItem = initData.targetClasses?.find((c: any) => c.classId === cr.classId)
    if (classItem?.departmentId && classItem?.departmentName) {
      deptMap.set(String(classItem.departmentId), { id: String(classItem.departmentId), name: classItem.departmentName })
    }
  })
  return Array.from(deptMap.values()).sort((a, b) => a.name.localeCompare(b.name, 'zh-CN'))
})

// 宿舍班级选项（根据年级和部门筛选后的班级）
const linkClassOptions = computed(() => {
  if (!currentCategory.value?.linkType) return []
  const linkResource = initData.linkResources[activeCategoryId.value]
  if (!linkResource?.classResources) return []

  const classMap = new Map<string, { id: string; name: string }>()
  linkResource.classResources.forEach((cr: any) => {
    const classItem = initData.targetClasses?.find((c: any) => c.classId === cr.classId)
    if (!classItem) return

    // 应用年级和部门筛选
    if (filterLinkGrade.value && String(classItem.gradeId) !== filterLinkGrade.value) return
    if (filterLinkDepartment.value && String(classItem.departmentId) !== filterLinkDepartment.value) return

    classMap.set(String(cr.classId), { id: String(cr.classId), name: classItem.className })
  })
  return Array.from(classMap.values()).sort((a, b) => a.name.localeCompare(b.name, 'zh-CN'))
})

// 宿舍楼栋选项
const linkBuildingOptions = computed(() => {
  if (!currentCategory.value?.linkType) return []
  const linkResource = initData.linkResources[activeCategoryId.value]
  if (!linkResource?.classResources) return []

  const buildingSet = new Set<string>()
  linkResource.classResources.forEach((cr: any) => {
    const classItem = initData.targetClasses?.find((c: any) => c.classId === cr.classId)
    if (!classItem) return

    // 应用年级、部门、班级筛选
    if (filterLinkGrade.value && String(classItem.gradeId) !== filterLinkGrade.value) return
    if (filterLinkDepartment.value && String(classItem.departmentId) !== filterLinkDepartment.value) return
    if (filterLinkClass.value && String(cr.classId) !== filterLinkClass.value) return

    if (linkResource.linkType === 1) {
      (cr.dormitories || []).forEach((d: any) => {
        buildingSet.add(d.buildingName || '未知楼栋')
      })
    } else {
      (cr.classrooms || []).forEach((c: any) => {
        buildingSet.add(c.buildingName || '未知楼栋')
      })
    }
  })
  return Array.from(buildingSet).sort((a, b) => a.localeCompare(b, 'zh-CN', { numeric: true }))
})

// 筛选后的宿舍/教室（按楼栋分组）
const filteredLinkResourcesByBuilding = computed(() => {
  if (!currentCategory.value?.linkType) return []
  const linkResource = initData.linkResources[activeCategoryId.value]
  if (!linkResource?.classResources) return []

  // 收集所有资源并添加班级信息
  const allResources: any[] = []
  linkResource.classResources.forEach((cr: any) => {
    const classItem = initData.targetClasses?.find((c: any) => c.classId === cr.classId)
    if (!classItem) return

    // 应用筛选
    if (filterLinkGrade.value && String(classItem.gradeId) !== filterLinkGrade.value) return
    if (filterLinkDepartment.value && String(classItem.departmentId) !== filterLinkDepartment.value) return
    if (filterLinkClass.value && String(cr.classId) !== filterLinkClass.value) return

    const className = classItem.className || `班级${cr.classId}`

    if (linkResource.linkType === 1) {
      // 宿舍
      (cr.dormitories || []).forEach((d: any) => {
        const buildingName = d.buildingName || '未知楼栋'
        // 楼栋筛选
        if (filterLinkBuilding.value && buildingName !== filterLinkBuilding.value) return
        allResources.push({
          id: d.id,
          no: d.dormitoryNo,
          floor: d.floor,
          buildingName,
          classId: cr.classId,
          className,
          type: 1
        })
      })
    } else {
      // 教室
      (cr.classrooms || []).forEach((c: any) => {
        const buildingName = c.buildingName || '未知楼栋'
        // 楼栋筛选
        if (filterLinkBuilding.value && buildingName !== filterLinkBuilding.value) return
        allResources.push({
          id: c.id,
          no: c.classroomNo || c.classroomCode,
          buildingName,
          classId: cr.classId,
          className,
          type: 2
        })
      })
    }
  })

  // 按楼栋分组
  const buildingMap = new Map<string, any[]>()
  allResources.forEach(res => {
    const buildingName = res.buildingName
    if (!buildingMap.has(buildingName)) {
      buildingMap.set(buildingName, [])
    }
    buildingMap.get(buildingName)!.push(res)
  })

  // 转换为数组并排序
  return Array.from(buildingMap.entries())
    .map(([buildingName, resources]) => ({
      buildingName,
      resources: resources.sort((a, b) => {
        const floorA = a.floor || 0
        const floorB = b.floor || 0
        if (floorA !== floorB) return floorA - floorB
        return String(a.no).localeCompare(String(b.no), 'zh-CN', { numeric: true })
      })
    }))
    .sort((a, b) => a.buildingName.localeCompare(b.buildingName, 'zh-CN', { numeric: true }))
})

// 筛选后的宿舍数量
const filteredLinkResourcesCount = computed(() => {
  return filteredLinkResourcesByBuilding.value.reduce((sum, b) => sum + b.resources.length, 0)
})

// 加权相关计算
const hasWeightConfig = computed(() => !!initData.weightConfigId && !!weightConfig.value)
const weightConfigName = computed(() => initData.weightConfigName || weightConfig.value?.configName || '标准加权')

// 加权模式描述
const weightModeDescription = computed(() => {
  if (!weightConfig.value) return ''
  const modeMap: Record<string, string> = {
    'STANDARD': '标准人数模式 (标准人数÷实际人数)',
    'PER_CAPITA': '人均模式 (实际人数÷标准人数)',
    'SEGMENT': '分段模式',
    'NONE': '无加权'
  }
  return modeMap[weightConfig.value.weightMode] || weightConfig.value.weightMode
})

// 原始总扣分
const totalScore = computed(() => {
  return scoringDetails.value.reduce((sum, item) => sum + (item.deductScore || 0), 0).toFixed(1)
})

// 当前班级原始扣分
const currentClassScore = computed(() => {
  return scoringDetails.value
    .filter((d: any) => String(d.classId) === activeClassId.value)
    .reduce((sum, item) => sum + (item.deductScore || 0), 0)
    .toFixed(1)
})

// 当前班级加权系数
const currentClassWeightFactor = computed(() => {
  if (!hasWeightConfig.value || !activeClassId.value) return 1
  const result = classWeightResults.value.get(activeClassId.value)
  return result?.weightFactor || 1
})

// 当前班级加权后扣分
const currentClassWeightedScore = computed(() => {
  if (!hasWeightConfig.value) return currentClassScore.value
  const originalScore = parseFloat(currentClassScore.value)
  return (originalScore * currentClassWeightFactor.value).toFixed(1)
})

// 加权后总扣分
const weightedTotalScore = computed(() => {
  if (!hasWeightConfig.value) return totalScore.value

  // 按班级分组计算加权分数
  const classScores = new Map<string, number>()
  scoringDetails.value.forEach((item: any) => {
    const classId = String(item.classId)
    const current = classScores.get(classId) || 0
    classScores.set(classId, current + (item.deductScore || 0))
  })

  let totalWeighted = 0
  classScores.forEach((score, classId) => {
    const result = classWeightResults.value.get(classId)
    const factor = result?.weightFactor || 1
    totalWeighted += score * factor
  })

  return totalWeighted.toFixed(1)
})

// 获取班级加权信息
const getClassWeightInfo = (classId: string | number) => {
  const result = classWeightResults.value.get(String(classId))
  if (!result) return null
  return {
    actualSize: result.actualSize,
    standardSize: result.standardSize,
    weightFactor: result.weightFactor,
    weightMode: result.weightMode
  }
}

// 当前类别
const currentCategory = computed(() => {
  return initData.categories.find((c: any) => String(c.categoryId) === activeCategoryId.value)
})

// 当前班级名称
const currentClassName = computed(() => {
  const cls = initData.targetClasses?.find((c: any) => String(c.classId) === activeClassId.value)
  return cls?.className || '-'
})

// 当前类别名称
const currentCategoryName = computed(() => {
  return currentCategory.value?.categoryName || '-'
})

// 当前关联资源
const currentLinkResources = computed(() => {
  if (!currentCategory.value || !currentCategory.value.linkType) return []
  return getLinkResources(activeCategoryId.value, activeClassId.value)
})

// 当前关联资源编号
const currentLinkNo = computed(() => {
  const link = currentLinkResources.value.find((l: any) => String(l.id) === activeLinkId.value)
  return link?.no || ''
})

// 当前类别是否需要关联资源（宿舍或教室）
const requiresLinkResource = computed(() => {
  if (!currentCategory.value) return false
  return currentCategory.value.linkType && currentCategory.value.linkType > 0
})

// 当前班级是否有关联资源
const hasLinkResource = computed(() => {
  if (!requiresLinkResource.value) return true
  return currentLinkResources.value.length > 0
})

// 全局检查轮次数
const globalTotalRounds = computed(() => {
  return initData.totalRounds || 1
})

// 全局轮次名称
const globalRoundNames = computed(() => {
  return initData.roundNames || []
})

// 获取轮次显示名称
const getRoundName = (round: number) => {
  if (globalRoundNames.value.length >= round && globalRoundNames.value[round - 1]) {
    return globalRoundNames.value[round - 1]
  }
  return `第${round}轮`
}

// 当前轮次可用的类别（根据 participatedRoundsList 筛选）
const availableCategoriesForRound = computed(() => {
  if (!initData.categories || initData.categories.length === 0) return []
  const currentRound = activeCheckRound.value
  return initData.categories.filter((cat: any) => {
    // 如果有 participatedRoundsList，检查当前轮次是否在列表中
    if (cat.participatedRoundsList && cat.participatedRoundsList.length > 0) {
      return cat.participatedRoundsList.includes(currentRound)
    }
    // 兼容旧数据：如果没有 participatedRoundsList，检查 participatedRounds 字符串
    if (cat.participatedRounds) {
      const rounds = cat.participatedRounds.split(',').map((s: string) => parseInt(s.trim()))
      return rounds.includes(currentRound)
    }
    // 默认参与所有轮次
    return true
  })
})

// 当前类别是否参与当前轮次
const isCategoryAvailableForRound = (categoryId: string | number) => {
  return availableCategoriesForRound.value.some((cat: any) => String(cat.categoryId) === String(categoryId))
}

// 当前类别的检查轮次数（已废弃，保持兼容）
const currentCheckRounds = computed(() => {
  // 现在使用全局轮次
  return globalTotalRounds.value
})

// 轮次选项数组
const checkRoundOptions = computed(() => {
  const rounds = globalTotalRounds.value
  const options = []
  for (let i = 1; i <= rounds; i++) {
    options.push({
      value: i,
      label: getRoundName(i)
    })
  }
  return options
})

// 当前已扣分数量
const currentDeductedCount = computed(() => {
  return Object.keys(currentDeductedItems.value).length
})

// 当前扣分项Map（按轮次过滤）
const currentDeductedItems = computed(() => {
  const key = `${String(activeCategoryId.value)}_${String(activeClassId.value)}_${String(activeLinkId.value || 0)}_${activeCheckRound.value}`
  return getDeductedItemsMap.value.get(key) || {}
})

// 扣分项Map（包含轮次维度）- 聚合同一扣分项的多条学生记录
const getDeductedItemsMap = computed(() => {
  const map = new Map<string, Record<string, any>>()
  const makeKey = (categoryId: string | number, classId: string | number, linkId: string | number, checkRound: number) =>
    `${String(categoryId)}_${String(classId)}_${String(linkId)}_${checkRound}`

  scoringDetails.value.forEach((detail: any) => {
    const linkId = detail.dormitoryId || detail.classroomId || detail.linkId || 0
    const round = detail.checkRound || 1
    const key = makeKey(detail.categoryId, detail.classId, linkId, round)
    if (!map.has(key)) map.set(key, {})
    const itemsMap = map.get(key)!
    const itemId = String(detail.deductionItemId)

    // 对于按人次扣分模式(deductMode=2)，聚合多条记录
    if (detail.deductMode === 2) {
      if (!itemsMap[itemId]) {
        // 第一条记录，初始化聚合对象
        itemsMap[itemId] = {
          ...detail,
          // 解析学生ID和姓名
          _studentIds: detail.studentIds ? detail.studentIds.split(',').map((id: string) => id.trim()).filter((id: string) => id) : [],
          _studentNames: detail.studentNames ? detail.studentNames.split(',').map((name: string) => name.trim()) : [],
          _records: [detail] // 保存原始记录用于后续处理
        }
      } else {
        // 已有记录，合并学生信息
        const existing = itemsMap[itemId]
        const newStudentIds = detail.studentIds ? detail.studentIds.split(',').map((id: string) => id.trim()).filter((id: string) => id) : []
        const newStudentNames = detail.studentNames ? detail.studentNames.split(',').map((name: string) => name.trim()) : []

        // 合并学生ID（去重）
        newStudentIds.forEach((id: string, idx: number) => {
          if (!existing._studentIds.includes(id)) {
            existing._studentIds.push(id)
            existing._studentNames.push(newStudentNames[idx] || '')
          }
        })
        existing._records.push(detail)

        // 更新聚合字段
        existing.personCount = existing._studentIds.length
        existing.studentIds = existing._studentIds.join(',')
        existing.studentNames = existing._studentNames.join(',')
        // 累加扣分（直接累加各记录的分数，不重新计算）
        existing.deductScore = existing._records.reduce((sum: number, r: any) => sum + (r.deductScore || 0), 0)
      }
    } else {
      // 非按人次模式，直接覆盖
      itemsMap[itemId] = detail
    }
  })
  return map
})

// 获取关联资源
const getLinkResources = (categoryId: string | number, classId: string | number) => {
  const linkResource = initData.linkResources[String(categoryId)]
  if (!linkResource || !linkResource.classResources) return []
  const classResource = linkResource.classResources.find((cr: any) => String(cr.classId) === String(classId))
  if (!classResource) return []
  if (linkResource.linkType === 1) {
    return (classResource.dormitories || []).map((d: any) => ({ id: d.id, no: d.dormitoryNo, type: 1 }))
  } else if (linkResource.linkType === 2) {
    return (classResource.classrooms || []).map((c: any) => ({ id: c.id, no: c.classroomNo, type: 2 }))
  }
  return []
}

// 班级扣分
const getClassTotalScore = (classId: string | number) => {
  const score = scoringDetails.value
    .filter((d: any) => String(d.classId) === String(classId))
    .reduce((sum, item) => sum + (item.deductScore || 0), 0)
  return Number(score.toFixed(1))
}

// 类别扣分
const getCategoryTotalScore = (categoryId: string | number, classId: string | number) => {
  const score = scoringDetails.value
    .filter((d: any) => String(d.categoryId) === String(categoryId) && String(d.classId) === String(classId))
    .reduce((sum, item) => sum + (item.deductScore || 0), 0)
  return Number(score.toFixed(1))
}

// 关联资源扣分（按当前轮次过滤）
const getLinkTotalScore = (categoryId: string | number, classId: string | number, linkId: string | number) => {
  const score = scoringDetails.value.filter((d: any) => {
    const detailLinkId = String(d.dormitoryId || d.classroomId || d.linkId || 0)
    return String(d.categoryId) === String(categoryId) &&
           String(d.classId) === String(classId) &&
           detailLinkId === String(linkId) &&
           (d.checkRound || 1) === activeCheckRound.value
  }).reduce((sum, item) => sum + (item.deductScore || 0), 0)
  return Number(score.toFixed(1))
}

// 轮次扣分统计
const getRoundTotalScore = (categoryId: string | number, classId: string | number, round: number) => {
  const score = scoringDetails.value
    .filter((d: any) =>
      String(d.categoryId) === String(categoryId) &&
      String(d.classId) === String(classId) &&
      (d.checkRound || 1) === round
    )
    .reduce((sum, item) => sum + (item.deductScore || 0), 0)
  return Number(score.toFixed(1))
}

// 获取类别的总扣分（所有班级）
const getCategoryAllScore = (categoryId: string | number) => {
  const score = scoringDetails.value
    .filter((d: any) => String(d.categoryId) === String(categoryId))
    .reduce((sum, item) => sum + (item.deductScore || 0), 0)
  return Number(score.toFixed(1))
}

// 获取班级在当前类别的扣分
const getClassCategoryScore = (classId: string | number) => {
  if (!activeCategoryId.value) return 0
  const score = scoringDetails.value
    .filter((d: any) =>
      String(d.categoryId) === activeCategoryId.value &&
      String(d.classId) === String(classId) &&
      (d.checkRound || 1) === activeCheckRound.value
    )
    .reduce((sum, item) => sum + (item.deductScore || 0), 0)
  return Number(score.toFixed(1))
}

// 获取宿舍/教室的扣分（带班级和轮次）
const getLinkTotalScoreAll = (linkId: string | number, classId: string | number) => {
  if (!activeCategoryId.value) return 0
  const score = scoringDetails.value
    .filter((d: any) => {
      const detailLinkId = String(d.dormitoryId || d.classroomId || d.linkId || 0)
      return String(d.categoryId) === activeCategoryId.value &&
             String(d.classId) === String(classId) &&
             detailLinkId === String(linkId) &&
             (d.checkRound || 1) === activeCheckRound.value
    })
    .reduce((sum, item) => sum + (item.deductScore || 0), 0)
  return Number(score.toFixed(1))
}

// 所有关联资源列表（当前类别）
const allLinkResources = computed(() => {
  if (!currentCategory.value?.linkType) return []
  const result: any[] = []
  const linkResource = initData.linkResources[activeCategoryId.value]
  if (!linkResource?.classResources) return []

  linkResource.classResources.forEach((cr: any) => {
    const resources = linkResource.linkType === 1
      ? (cr.dormitories || []).map((d: any) => ({ ...d, no: d.dormitoryNo, type: 1, classId: cr.classId }))
      : (cr.classrooms || []).map((c: any) => ({ ...c, no: c.classroomNo, type: 2, classId: cr.classId }))
    result.push(...resources)
  })
  return result
})

// 按班级分组的关联资源
const linkResourcesByClass = computed(() => {
  if (!currentCategory.value?.linkType) return []
  const linkResource = initData.linkResources[activeCategoryId.value]
  if (!linkResource?.classResources) return []

  return linkResource.classResources.map((cr: any) => {
    const classItem = initData.targetClasses?.find((c: any) => c.classId === cr.classId)
    const resources = linkResource.linkType === 1
      ? (cr.dormitories || []).map((d: any) => ({ id: d.id, no: d.dormitoryNo, type: 1 }))
      : (cr.classrooms || []).map((c: any) => ({ id: c.id, no: c.classroomNo, type: 2 }))
    return {
      classId: cr.classId,
      className: classItem?.className || `班级${cr.classId}`,
      resources
    }
  }).filter((g: any) => g.resources.length > 0)
})

// 按楼栋分组的关联资源
const linkResourcesByBuilding = computed(() => {
  if (!currentCategory.value?.linkType) return []
  const linkResource = initData.linkResources[activeCategoryId.value]
  if (!linkResource?.classResources) return []

  // 收集所有资源并添加班级信息
  const allResources: any[] = []
  linkResource.classResources.forEach((cr: any) => {
    const classItem = initData.targetClasses?.find((c: any) => c.classId === cr.classId)
    const className = classItem?.className || `班级${cr.classId}`

    if (linkResource.linkType === 1) {
      // 宿舍
      (cr.dormitories || []).forEach((d: any) => {
        allResources.push({
          id: d.id,
          no: d.dormitoryNo,
          floor: d.floor,
          buildingName: d.buildingName || '未知楼栋',
          classId: cr.classId,
          className,
          type: 1
        })
      })
    } else {
      // 教室
      (cr.classrooms || []).forEach((c: any) => {
        allResources.push({
          id: c.id,
          no: c.classroomNo || c.classroomCode,
          buildingName: c.buildingName || '未知楼栋',
          classId: cr.classId,
          className,
          type: 2
        })
      })
    }
  })

  // 按楼栋分组
  const buildingMap = new Map<string, any[]>()
  allResources.forEach(res => {
    const buildingName = res.buildingName
    if (!buildingMap.has(buildingName)) {
      buildingMap.set(buildingName, [])
    }
    buildingMap.get(buildingName)!.push(res)
  })

  // 转换为数组并排序（按楼栋名称）
  return Array.from(buildingMap.entries())
    .map(([buildingName, resources]) => ({
      buildingName,
      resources: resources.sort((a, b) => {
        // 先按楼层排序，再按房间号排序
        const floorA = a.floor || 0
        const floorB = b.floor || 0
        if (floorA !== floorB) return floorA - floorB
        return String(a.no).localeCompare(String(b.no), 'zh-CN', { numeric: true })
      })
    }))
    .sort((a, b) => a.buildingName.localeCompare(b.buildingName, 'zh-CN', { numeric: true }))
})

// 判断扣分项是否已扣分
const isItemDeducted = (itemId: number | string) => {
  return !!currentDeductedItems.value[String(itemId)]
}

// 获取扣分数据
const getDeductedData = (itemId: number | string) => {
  return currentDeductedItems.value[String(itemId)] || null
}

// 返回
const goBack = () => {
  if (fromRoute.value) {
    router.push(fromRoute.value)
  } else {
    router.back()
  }
}

// 加载加权配置
const loadWeightConfig = async () => {
  if (!initData.weightConfigId) return
  try {
    const config = await getWeightConfig(initData.weightConfigId)
    weightConfig.value = config
    // 预计算所有班级的加权系数
    await preCalculateClassWeights()
  } catch (error) {
    console.error('加载加权配置失败:', error)
  }
}

// 预计算所有班级的加权系数
const preCalculateClassWeights = async () => {
  if (!initData.targetClasses || !initData.checkDate) return

  const promises = initData.targetClasses.map(async (classItem: any) => {
    try {
      const result = await calculateWeightedScore(
        classItem.classId,
        1, // 使用1分作为基准计算权重系数
        initData.checkDate
      )
      classWeightResults.value.set(String(classItem.classId), result)
    } catch (error) {
      console.error(`计算班级 ${classItem.classId} 加权系数失败:`, error)
    }
  })

  await Promise.all(promises)
}

// 加载初始数据
const loadInitData = async () => {
  try {
    pageLoading.value = true
    const response = await http.get(`/quantification/daily-checks/${checkId.value}/scoring/init`)
    Object.assign(initData, response)

    if (initData.existingDetails && initData.existingDetails.length > 0) {
      scoringDetails.value = initData.existingDetails.map((detail: any) => ({
        categoryId: detail.categoryId,
        classId: detail.classId,
        deductionItemId: detail.deductionItemId,
        deductionItemName: detail.deductionItemName,
        deductMode: detail.deductMode,
        linkType: detail.linkType || 0,
        linkId: detail.linkId || 0,
        deductScore: detail.deductScore,
        personCount: detail.personCount,
        studentIds: detail.studentIds,
        studentNames: detail.studentNames,
        description: detail.description,
        remark: detail.remark,
        photoUrls: detail.photoUrls,
        students: detail.students,
        dormitoryId: detail.dormitoryId,
        dormitoryNo: detail.dormitoryNo,
        classroomId: detail.classroomId,
        classroomNo: detail.classroomNo,
        checkRound: detail.checkRound || 1
      }))
    }

    // 设置默认选中
    if (initData.targetClasses?.length > 0) {
      activeClassId.value = String(initData.targetClasses[0].classId)
    }
    // 默认选择第1轮及其可用的类别
    activeCheckRound.value = 1
    if (initData.categories?.length > 0) {
      // 找到第1轮可用的第一个类别
      const firstRoundCategories = initData.categories.filter((cat: any) => {
        if (cat.participatedRoundsList && cat.participatedRoundsList.length > 0) {
          return cat.participatedRoundsList.includes(1)
        }
        if (cat.participatedRounds) {
          const rounds = cat.participatedRounds.split(',').map((s: string) => parseInt(s.trim()))
          return rounds.includes(1)
        }
        return true
      })
      const firstCategory = firstRoundCategories.length > 0 ? firstRoundCategories[0] : initData.categories[0]
      activeCategoryId.value = String(firstCategory.categoryId)
      if (firstCategory.linkType && firstCategory.linkType > 0) {
        const links = getLinkResources(firstCategory.categoryId, activeClassId.value)
        if (links?.length > 0) activeLinkId.value = String(links[0].id)
      }
    }

    // 加载加权配置
    await loadWeightConfig()
  } catch (error: any) {
    ElMessage.error('加载打分数据失败: ' + (error.message || ''))
  } finally {
    pageLoading.value = false
  }
}

// 加载扣分项
const loadDeductionItems = async () => {
  if (!activeCategoryId.value) return
  deductionItemsLoading.value = true
  try {
    const data = await getEnabledDeductionItemsByTypeId(activeCategoryId.value)
    deductionItems.value = data || []
  } catch (error) {
    console.error('加载扣分项失败:', error)
  } finally {
    deductionItemsLoading.value = false
  }
}

// 班级切换
const handleClassChange = (classId: string) => {
  activeClassId.value = classId
  // 保持当前类别不变，只更新关联资源
  if (currentCategory.value?.linkType && currentCategory.value.linkType > 0) {
    const links = getLinkResources(activeCategoryId.value, classId)
    activeLinkId.value = links?.length > 0 ? String(links[0].id) : ''
  } else {
    activeLinkId.value = ''
  }
}

// 类别切换
const handleCategoryChange = (categoryId: string) => {
  activeCategoryId.value = categoryId
  const category = initData.categories.find((c: any) => String(c.categoryId) === categoryId)
  // 不再重置轮次（轮次现在是全局的）
  // 重置宿舍筛选条件
  filterLinkGrade.value = ''
  filterLinkDepartment.value = ''
  filterLinkClass.value = ''
  filterLinkBuilding.value = ''

  if (category?.linkType && category.linkType > 0) {
    // 有关联资源的类别：自动选择第一个资源
    const linkResource = initData.linkResources[categoryId]
    if (linkResource?.classResources?.length > 0) {
      const firstClassResource = linkResource.classResources[0]
      activeClassId.value = String(firstClassResource.classId)
      const resources = linkResource.linkType === 1
        ? firstClassResource.dormitories
        : firstClassResource.classrooms
      if (resources?.length > 0) {
        activeLinkId.value = String(resources[0].id)
      } else {
        activeLinkId.value = ''
      }
    } else {
      activeLinkId.value = ''
    }
  } else {
    // 无关联资源的类别：保持当前班级或选择第一个班级
    activeLinkId.value = ''
    if (!activeClassId.value && initData.targetClasses?.length > 0) {
      activeClassId.value = String(initData.targetClasses[0].classId)
    }
  }
}

// 轮次切换
const handleRoundChange = (round: number) => {
  activeCheckRound.value = round
  // 检查当前类别是否在新轮次可用
  const newAvailableCategories = initData.categories.filter((cat: any) => {
    if (cat.participatedRoundsList && cat.participatedRoundsList.length > 0) {
      return cat.participatedRoundsList.includes(round)
    }
    if (cat.participatedRounds) {
      const rounds = cat.participatedRounds.split(',').map((s: string) => parseInt(s.trim()))
      return rounds.includes(round)
    }
    return true
  })
  // 如果当前类别不在新轮次可用，自动选择第一个可用类别
  const currentCatAvailable = newAvailableCategories.some(
    (cat: any) => String(cat.categoryId) === activeCategoryId.value
  )
  if (!currentCatAvailable && newAvailableCategories.length > 0) {
    handleCategoryChange(String(newAvailableCategories[0].categoryId))
  }
}

// 关联资源切换
const handleLinkChange = (linkId: string) => {
  activeLinkId.value = linkId
}

// 选择宿舍/教室（同时设置班级和资源ID）
const handleLinkSelect = (classId: number, link: any) => {
  activeClassId.value = String(classId)
  activeLinkId.value = String(link.id)
}

// 扣分切换 - 支持按人次扣分模式的学生单独记录
const handleToggleDeduct = (detail: any, isAdd: boolean) => {
  const linkId = detail.dormitoryId || detail.classroomId || detail.linkId || 0
  const checkRound = detail.checkRound || activeCheckRound.value

  // 对于按人次扣分模式(deductMode=2)，按学生拆分记录
  if (detail.deductMode === 2) {
    // 先删除该扣分项的所有记录
    scoringDetails.value = scoringDetails.value.filter((d) =>
      !(String(d.categoryId) === String(detail.categoryId) &&
        String(d.classId) === String(detail.classId) &&
        String(d.deductionItemId) === String(detail.deductionItemId) &&
        String(d.linkId || d.dormitoryId || d.classroomId || 0) === String(linkId) &&
        (d.checkRound || 1) === checkRound)
    )

    if (isAdd && detail.studentIds) {
      // 解析学生信息
      const studentIdList = detail.studentIds.split(',').map((id: string) => id.trim()).filter((id: string) => id)
      const studentNameList = detail.studentNames ? detail.studentNames.split(',').map((name: string) => name.trim()) : []

      // 计算单人扣分
      const baseScore = detail.baseScore || 0
      const perPersonScore = detail.perPersonScore || detail.scorePerPerson || 0
      const singleStudentScore = baseScore + perPersonScore

      // 为每个学生创建单独的记录
      studentIdList.forEach((studentId: string, idx: number) => {
        const studentName = studentNameList[idx] || ''
        const singleDetail = {
          categoryId: detail.categoryId,
          classId: detail.classId,
          deductionItemId: detail.deductionItemId,
          deductionItemName: detail.deductionItemName,
          deductMode: detail.deductMode,
          linkType: detail.linkType || 0,
          linkId: linkId,
          dormitoryId: detail.dormitoryId,
          dormitoryNo: detail.dormitoryNo,
          classroomId: detail.classroomId,
          classroomNo: detail.classroomNo,
          deductScore: singleStudentScore,
          personCount: 1,
          studentIds: studentId,
          studentNames: studentName,
          checkRound: checkRound,
          remark: detail.remark || null,
          photoUrls: detail.photoUrls || null,
          // 保存扣分项参数供后续计算
          baseScore: baseScore,
          perPersonScore: perPersonScore
        }
        scoringDetails.value.push(singleDetail)
      })
    }
  } else {
    // 非按人次模式，保持原有逻辑
    const index = scoringDetails.value.findIndex((d) =>
      String(d.categoryId) === String(detail.categoryId) &&
      String(d.classId) === String(detail.classId) &&
      String(d.deductionItemId) === String(detail.deductionItemId) &&
      String(d.linkId || 0) === String(linkId) &&
      (d.checkRound || 1) === checkRound
    )

    if (!isAdd) {
      if (index !== -1) scoringDetails.value.splice(index, 1)
    } else {
      const detailWithRound = { ...detail, checkRound: checkRound }
      if (index !== -1) {
        scoringDetails.value[index] = detailWithRound
      } else {
        scoringDetails.value.push(detailWithRound)
      }
    }
  }
  debouncedAutoSave()
}

// 更新扣分数据 - 支持按人次扣分模式的学生单独记录
const handleUpdateDeduct = (detail: any) => {
  const linkId = detail.dormitoryId || detail.classroomId || detail.linkId || 0
  const checkRound = detail.checkRound || activeCheckRound.value

  // 对于按人次扣分模式(deductMode=2)，重新创建所有学生记录
  if (detail.deductMode === 2) {
    // 复用 handleToggleDeduct 的逻辑
    handleToggleDeduct(detail, true)
  } else {
    // 非按人次模式，保持原有逻辑
    const index = scoringDetails.value.findIndex((d) =>
      String(d.categoryId) === String(detail.categoryId) &&
      String(d.classId) === String(detail.classId) &&
      String(d.deductionItemId) === String(detail.deductionItemId) &&
      String(d.linkId || 0) === String(linkId) &&
      (d.checkRound || 1) === checkRound
    )
    if (index !== -1) {
      scoringDetails.value[index] = { ...detail, checkRound: checkRound }
      debouncedAutoSave()
    }
  }
}

// 防抖自动保存
const debouncedAutoSave = () => {
  if (autoSaveTimer) clearTimeout(autoSaveTimer)
  autoSaveStatus.value = 'idle'
  autoSaveTimer = setTimeout(async () => {
    await autoSave()
  }, 1500)
}

// 自动保存
const autoSave = async () => {
  try {
    autoSaveStatus.value = 'saving'
    const requestData = {
      checkId: checkId.value,
      checkerId: authStore.user?.id || 1,
      checkerName: authStore.user?.realName || '系统管理员',
      details: scoringDetails.value
    }
    await http.post(`/quantification/daily-checks/${checkId.value}/scoring`, requestData)
    autoSaveStatus.value = 'saved'
    setTimeout(() => {
      if (autoSaveStatus.value === 'saved') autoSaveStatus.value = 'idle'
    }, 3000)
  } catch (error) {
    console.error('保存失败:', error)
    autoSaveStatus.value = 'idle'
    ElMessage.error('自动保存失败')
  }
}

// 监听类别变化，加载扣分项
watch(activeCategoryId, () => {
  loadDeductionItems()
}, { immediate: false })

onMounted(async () => {
  if (!checkId.value) {
    ElMessage.error('缺少检查ID参数')
    router.back()
    return
  }
  await loadInitData()
  await loadDeductionItems()
})
</script>

<style scoped>
/* 自定义滚动条样式 */
.scrollbar-thin::-webkit-scrollbar {
  width: 6px;
}
.scrollbar-thin::-webkit-scrollbar-track {
  background: #f1f5f9;
  border-radius: 3px;
}
.scrollbar-thin::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 3px;
}
.scrollbar-thin::-webkit-scrollbar-thumb:hover {
  background: #94a3b8;
}
</style>
