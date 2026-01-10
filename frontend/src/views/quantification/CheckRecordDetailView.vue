<template>
  <div class="min-h-screen bg-slate-50">
    <!-- 顶部导航栏 - 检查记录详情视图 -->
    <header class="sticky top-0 z-50 bg-white border-b border-slate-200 shadow-sm">
      <div class="flex items-center justify-between px-6 py-4">
        <div class="flex items-center gap-4">
          <button
            @click="goBack"
            class="flex items-center justify-center w-10 h-10 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-600 transition-colors"
          >
            <ArrowLeft class="w-5 h-5" />
          </button>
          <div>
            <h1 class="text-lg font-semibold text-slate-900">{{ recordInfo.checkName || '检查记录详情' }}</h1>
            <p class="text-sm text-slate-500">{{ formatDate(recordInfo.checkDate) }} · {{ recordInfo.checkerName || '未知' }}</p>
          </div>
        </div>
        <!-- 视图切换Tab -->
        <div class="flex items-center gap-2 bg-slate-100 rounded-xl p-1">
          <button
            @click="activeView = 'detail'"
            :class="[
              'flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-medium transition-all',
              activeView === 'detail'
                ? 'bg-white text-blue-600 shadow-sm'
                : 'text-slate-500 hover:text-slate-700'
            ]"
          >
            <LayoutList class="w-4 h-4" />
            班级详情
          </button>
          <button
            @click="switchToRankingView"
            :class="[
              'flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-medium transition-all',
              activeView === 'ranking'
                ? 'bg-white text-blue-600 shadow-sm'
                : 'text-slate-500 hover:text-slate-700'
            ]"
          >
            <Trophy class="w-4 h-4" />
            排名视图
          </button>
          <button
            v-if="ratingRules.length > 0"
            @click="activeView = 'rating'"
            :class="[
              'flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-medium transition-all',
              activeView === 'rating'
                ? 'bg-white text-blue-600 shadow-sm'
                : 'text-slate-500 hover:text-slate-700'
            ]"
          >
            <BarChart3 class="w-4 h-4" />
            评级结果
          </button>
        </div>
        <div class="flex items-center gap-6">
          <div class="text-center">
            <div class="text-2xl font-bold text-slate-800">{{ classList.length }}</div>
            <div class="text-xs text-slate-400">参检班级</div>
          </div>
          <div class="text-center">
            <div class="text-2xl font-bold text-rose-500">-{{ formatScore(totalDeduction) }}</div>
            <div class="text-xs text-slate-400">总扣分</div>
          </div>
        </div>
      </div>
    </header>

    <!-- 加载状态 -->
    <div v-if="loading" class="flex flex-col items-center justify-center h-96 gap-4 text-slate-500">
      <Loader2 class="w-10 h-10 animate-spin" />
      <span>正在加载...</span>
    </div>

    <!-- 主内容区 - 班级详情视图 -->
    <div v-else-if="activeView === 'detail'" class="flex h-[calc(100vh-73px)]">
      <!-- 左侧班级列表 -->
      <aside class="w-64 flex-shrink-0 bg-white border-r border-slate-200 overflow-y-auto">
        <div class="p-4">
          <h3 class="text-xs font-medium text-slate-400 uppercase tracking-wider mb-3">班级列表</h3>

          <!-- 筛选区域 -->
          <div class="space-y-2 mb-4">
            <el-select
              v-model="filterDepartment"
              placeholder="选择部门"
              clearable
              size="small"
              class="w-full"
            >
              <el-option
                v-for="dept in departmentOptions"
                :key="dept.value"
                :label="dept.label"
                :value="dept.value"
              />
            </el-select>

            <el-select
              v-model="filterGrade"
              placeholder="选择年级"
              clearable
              size="small"
              class="w-full"
            >
              <el-option
                v-for="grade in gradeOptions"
                :key="grade.value"
                :label="grade.label"
                :value="grade.value"
              />
            </el-select>

            <div v-if="filterDepartment || filterGrade" class="text-xs text-slate-500 px-1">
              共 {{ filteredClassList.length }} 个班级
            </div>
          </div>

          <!-- 无筛选结果提示 -->
          <div v-if="filteredClassList.length === 0" class="text-center py-8">
            <div class="text-slate-400 text-sm">暂无符合条件的班级</div>
          </div>

          <div v-else class="space-y-1.5">
            <button
              v-for="cls in filteredClassList"
              :key="cls.classId"
              @click="activeClassId = String(cls.classId)"
              :class="[
                'w-full flex items-center justify-between px-4 py-3 rounded-xl text-left transition-all duration-200',
                activeClassId === String(cls.classId)
                  ? 'bg-gradient-to-r from-blue-500 to-blue-600 text-white shadow-lg shadow-blue-500/25'
                  : 'bg-slate-50 hover:bg-slate-100 text-slate-700'
              ]"
            >
              <div class="flex-1 min-w-0">
                <div class="font-medium truncate">{{ cls.className }}</div>
                <div class="text-xs opacity-75 truncate">{{ cls.gradeName || cls.departmentName || '-' }}</div>
              </div>
              <span
                v-if="cls.totalScore > 0"
                :class="[
                  'text-sm font-semibold px-2 py-0.5 rounded-lg whitespace-nowrap flex-shrink-0 ml-2',
                  activeClassId === String(cls.classId)
                    ? 'bg-white/20 text-white'
                    : 'bg-rose-100 text-rose-600'
                ]"
              >
                -{{ formatScore(cls.totalScore) }}
                <template v-if="cls.weightEnabled && cls.weightedTotalScore != null">
                  <span :class="activeClassId === String(cls.classId) ? 'text-white/60' : 'text-slate-400'">/</span>
                  <span :class="activeClassId === String(cls.classId) ? 'text-sky-200' : 'text-indigo-500'">-{{ formatScore(cls.weightedTotalScore) }}</span>
                </template>
              </span>
              <span
                v-else
                :class="[
                  'text-xs px-2 py-0.5 rounded-lg',
                  activeClassId === String(cls.classId) ? 'bg-white/20 text-white' : 'bg-emerald-100 text-emerald-600'
                ]"
              >
                满分
              </span>
            </button>
          </div>
        </div>
      </aside>

      <!-- 右侧内容 -->
      <main class="flex-1 overflow-y-auto p-6">
        <template v-if="activeClass">
          <!-- 班级信息卡片 -->
          <div class="bg-white rounded-2xl shadow-sm border border-slate-200 p-5 mb-6">
            <div class="flex items-center justify-between">
              <div class="flex items-center gap-4">
                <div class="w-12 h-12 rounded-xl bg-gradient-to-br from-blue-500 to-indigo-600 flex items-center justify-center text-white font-bold text-lg">
                  {{ activeClass.className?.charAt(0) || '班' }}
                </div>
                <div>
                  <h2 class="text-xl font-bold text-slate-900">{{ activeClass.className }}</h2>
                  <p class="text-sm text-slate-500">
                    {{ activeClass.teacherName || '暂无班主任信息' }}
                    <span v-if="activeClass.classSize" class="ml-2 text-slate-400">· {{ activeClass.classSize }}人</span>
                  </p>
                </div>
              </div>
              <div class="flex items-center gap-8">
                <div class="text-center">
                  <div class="text-3xl font-bold text-rose-500">{{ formatScore(activeClass.totalScore) }}</div>
                  <div class="text-xs text-slate-400 mt-1">扣分合计</div>
                </div>
                <div v-if="activeClass.weightEnabled && activeClass.weightedTotalScore != null" class="text-center">
                  <div class="text-3xl font-bold text-indigo-500">{{ formatScore(activeClass.weightedTotalScore) }}</div>
                  <div class="text-xs text-slate-400 mt-1">加权后</div>
                </div>
                <div class="text-center">
                  <div class="text-3xl font-bold text-slate-700">{{ getCategoryStats(activeClass).length }}</div>
                  <div class="text-xs text-slate-400 mt-1">扣分类别</div>
                </div>
                <div class="text-center">
                  <div class="text-3xl font-bold text-slate-700">{{ getDeductionCount(activeClass) }}</div>
                  <div class="text-xs text-slate-400 mt-1">扣分项数</div>
                </div>
                <button
                  @click="openAppealHistoryDialog"
                  class="flex items-center gap-2 px-4 py-2 rounded-xl bg-amber-50 text-amber-700 hover:bg-amber-100 transition-colors border border-amber-200"
                >
                  <FileText class="w-4 h-4" />
                  <span class="text-sm font-medium">申诉记录</span>
                </button>
              </div>
            </div>
          </div>

          <!-- 加权配置区域（可折叠） -->
          <div v-if="activeClass?.weightEnabled" class="bg-white rounded-2xl shadow-sm border border-slate-200 overflow-hidden mb-6">
            <!-- 折叠头部 -->
            <div
              class="flex items-center justify-between px-5 py-4 cursor-pointer hover:bg-slate-50 transition-colors border-b border-slate-100"
              @click="weightConfigExpanded = !weightConfigExpanded"
            >
              <div class="flex items-center gap-3">
                <div class="w-10 h-10 rounded-xl bg-gradient-to-br from-indigo-500 to-purple-600 flex items-center justify-center">
                  <Scale class="w-5 h-5 text-white" />
                </div>
                <div>
                  <h3 class="text-base font-semibold text-slate-800">加权配置详情</h3>
                  <p class="text-xs text-slate-400">
                    班级人数 {{ activeClass.classSize || '-' }} 人，加权系数 x{{ formatWeightFactor(activeClass.weightFactor) }}
                  </p>
                </div>
              </div>
              <div class="flex items-center gap-3">
                <div class="text-right mr-2">
                  <div class="text-xs text-slate-400">原始 → 加权</div>
                  <div class="text-sm font-semibold">
                    <span class="text-rose-500">-{{ formatScore(activeClass.totalScore) }}</span>
                    <span class="text-slate-400 mx-1">→</span>
                    <span class="text-indigo-500">-{{ formatScore(activeClass.weightedTotalScore) }}</span>
                  </div>
                </div>
                <ChevronDown
                  :class="[
                    'w-5 h-5 text-slate-400 transition-transform duration-200',
                    weightConfigExpanded ? 'rotate-180' : ''
                  ]"
                />
              </div>
            </div>
            <!-- 展开内容 -->
            <div v-show="weightConfigExpanded" class="p-5 bg-slate-50/50">
              <ClassWeightConfigView
                :class-info="activeClass"
                :category-stats="getCategoryStats(activeClass)"
                :show-deduction-details="true"
                :default-expanded="true"
              />
            </div>
          </div>

          <!-- 筛选Tab区域 -->
          <div class="bg-white rounded-2xl shadow-sm border border-slate-200 p-4 mb-6">
            <div class="flex flex-wrap items-center gap-6">
              <!-- 关联位置 Tab -->
              <div class="flex items-center gap-2">
                <span class="text-xs text-slate-400 font-medium">关联位置:</span>
                <div class="flex rounded-lg bg-slate-100 p-1">
                  <button
                    v-for="tab in locationTabs"
                    :key="tab.value"
                    @click="activeLocationTab = tab.value"
                    :class="[
                      'flex items-center gap-1.5 px-3 py-1.5 rounded-md text-xs font-medium transition-all',
                      activeLocationTab === tab.value
                        ? 'bg-white text-blue-600 shadow-sm'
                        : 'text-slate-500 hover:text-slate-700'
                    ]"
                  >
                    <component :is="tab.icon" class="w-3.5 h-3.5" />
                    {{ tab.label }}
                    <span
                      v-if="getLocationCount(tab.value) > 0"
                      :class="[
                        'px-1.5 py-0.5 rounded text-[10px] font-semibold',
                        activeLocationTab === tab.value ? 'bg-blue-100 text-blue-600' : 'bg-slate-200 text-slate-600'
                      ]"
                    >
                      {{ getLocationCount(tab.value) }}
                    </span>
                  </button>
                </div>
              </div>

              <!-- 检查轮次 Tab -->
              <div v-if="availableRounds.length > 1" class="flex items-center gap-2">
                <span class="text-xs text-slate-400 font-medium">检查轮次:</span>
                <div class="flex rounded-lg bg-slate-100 p-1">
                  <button
                    v-for="round in roundTabs"
                    :key="round.value"
                    @click="activeRoundTab = round.value"
                    :class="[
                      'flex items-center gap-1.5 px-3 py-1.5 rounded-md text-xs font-medium transition-all',
                      activeRoundTab === round.value
                        ? 'bg-white text-indigo-600 shadow-sm'
                        : 'text-slate-500 hover:text-slate-700'
                    ]"
                  >
                    {{ round.label }}
                    <span
                      v-if="getRoundCount(round.value) > 0"
                      :class="[
                        'px-1.5 py-0.5 rounded text-[10px] font-semibold',
                        activeRoundTab === round.value ? 'bg-indigo-100 text-indigo-600' : 'bg-slate-200 text-slate-600'
                      ]"
                    >
                      {{ getRoundCount(round.value) }}
                    </span>
                  </button>
                </div>
              </div>

              <!-- 筛选结果统计 -->
              <div class="ml-auto flex items-center gap-2 text-xs text-slate-500">
                <Filter class="w-3.5 h-3.5" />
                <span>显示 <span class="font-semibold text-slate-700">{{ filteredDeductionsCount }}</span> 项扣分</span>
              </div>
            </div>
          </div>

          <!-- 扣分详情（按类别分组） -->
          <div v-if="filteredCategoryStats.length" class="space-y-4">
            <div
              v-for="cat in filteredCategoryStats"
              :key="cat.categoryId || cat.categoryName"
              class="bg-white rounded-2xl shadow-sm border border-slate-200 overflow-hidden"
            >
              <!-- 类别头部 -->
              <div
                @click="toggleCategory(cat.categoryId || cat.categoryName)"
                class="flex items-center justify-between px-5 py-4 cursor-pointer hover:bg-slate-50 transition-colors"
              >
                <div class="flex items-center gap-3">
                  <div class="w-10 h-10 rounded-xl bg-gradient-to-br from-slate-100 to-slate-200 flex items-center justify-center">
                    <component :is="getCategoryIcon(cat.categoryName)" class="w-5 h-5 text-slate-600" />
                  </div>
                  <div>
                    <h3 class="text-base font-semibold text-slate-800">{{ cat.categoryName }}</h3>
                    <p class="text-xs text-slate-400">{{ cat.deductions?.length || 0 }} 项扣分</p>
                  </div>
                </div>
                <div class="flex items-center gap-4">
                  <span class="text-lg font-bold">
                    <span class="text-rose-500">-{{ formatScore(cat.totalScore) }}</span>
                    <template v-if="activeClass?.weightEnabled && cat.weightedTotalScore != null">
                      <span class="text-slate-300 mx-1">/</span>
                      <span class="text-indigo-500">-{{ formatScore(cat.weightedTotalScore) }}</span>
                    </template>
                  </span>
                  <ChevronDown
                    :class="[
                      'w-5 h-5 text-slate-400 transition-transform duration-200',
                      expandedCategories.includes(cat.categoryId || cat.categoryName) ? 'rotate-180' : ''
                    ]"
                  />
                </div>
              </div>

              <!-- 扣分明细卡片网格 -->
              <div
                v-show="expandedCategories.includes(cat.categoryId || cat.categoryName)"
                class="border-t border-slate-100 p-3 bg-slate-50/50"
              >
                <!-- 筛选Tab区域（轮次 + 关联位置） -->
                <div
                  v-if="hasLinkedLocationsForCategory(cat) || hasMultipleRoundsForCategory(cat)"
                  class="mb-3 space-y-2"
                >
                  <!-- 轮次Tab -->
                  <div
                    v-if="hasMultipleRoundsForCategory(cat)"
                    class="flex flex-wrap items-center gap-2 p-2.5 bg-white rounded-xl border border-slate-200 shadow-sm"
                  >
                    <span class="text-xs text-slate-400 font-medium flex items-center gap-1">
                      <RefreshCw class="w-3.5 h-3.5" />轮次
                    </span>
                    <button
                      @click="setCategoryRoundTab(cat.categoryId || cat.categoryName, 0)"
                      :class="[
                        'px-2.5 py-1.5 rounded-lg text-xs font-medium transition-all',
                        getCategoryRoundTab(cat.categoryId || cat.categoryName) === 0
                          ? 'bg-slate-700 text-white shadow-sm'
                          : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
                      ]"
                    >
                      全部
                      <span class="ml-1 px-1.5 py-0.5 rounded text-[10px]" :class="getCategoryRoundTab(cat.categoryId || cat.categoryName) === 0 ? 'bg-white/20' : 'bg-slate-200'">
                        {{ cat.deductions?.length || 0 }}
                      </span>
                    </button>
                    <button
                      v-for="roundGroup in getRoundGroupsForCategory(cat)"
                      :key="'round-' + roundGroup.round"
                      @click="setCategoryRoundTab(cat.categoryId || cat.categoryName, roundGroup.round)"
                      :class="[
                        'px-2.5 py-1.5 rounded-lg text-xs font-medium transition-all',
                        getCategoryRoundTab(cat.categoryId || cat.categoryName) === roundGroup.round
                          ? 'bg-indigo-500 text-white shadow-sm'
                          : 'bg-indigo-50 text-indigo-700 hover:bg-indigo-100 border border-indigo-200'
                      ]"
                    >
                      {{ roundGroup.label }}
                      <span class="ml-1 px-1.5 py-0.5 rounded text-[10px]" :class="getCategoryRoundTab(cat.categoryId || cat.categoryName) === roundGroup.round ? 'bg-white/20' : 'bg-indigo-100'">
                        {{ roundGroup.count }}
                      </span>
                    </button>
                  </div>

                  <!-- 关联位置Tab（宿舍/教室） -->
                  <div
                    v-if="hasLinkedLocationsForCategory(cat)"
                    class="flex flex-wrap items-center gap-2 p-2.5 bg-white rounded-xl border border-slate-200 shadow-sm"
                  >
                    <span class="text-xs text-slate-400 font-medium flex items-center gap-1">
                      <MapPin class="w-3.5 h-3.5" />位置
                    </span>
                    <!-- 全部按钮 -->
                    <button
                      @click="setCategoryDormitoryTab(cat.categoryId || cat.categoryName, 'all')"
                      :class="[
                        'px-2.5 py-1.5 rounded-lg text-xs font-medium transition-all',
                        getCategoryDormitoryTab(cat.categoryId || cat.categoryName) === 'all'
                          ? 'bg-slate-700 text-white shadow-sm'
                          : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
                      ]"
                    >
                      全部
                      <span class="ml-1 px-1.5 py-0.5 rounded text-[10px]" :class="getCategoryDormitoryTab(cat.categoryId || cat.categoryName) === 'all' ? 'bg-white/20' : 'bg-slate-200'">
                        {{ cat.deductions?.length || 0 }}
                      </span>
                    </button>

                    <!-- 宿舍分组 -->
                    <template v-if="getDormitoryGroupsForCategory(cat).length > 0">
                      <span class="text-slate-300 mx-1">|</span>
                      <span class="text-xs text-slate-400 font-medium flex items-center gap-1">
                        <Home class="w-3.5 h-3.5" />宿舍
                      </span>
                      <button
                        v-for="dormGroup in getDormitoryGroupsForCategory(cat)"
                        :key="'dorm-' + dormGroup.linkId"
                        @click="setCategoryDormitoryTab(cat.categoryId || cat.categoryName, dormGroup.linkId)"
                        :class="[
                          'px-2.5 py-1.5 rounded-lg text-xs font-medium transition-all',
                          getCategoryDormitoryTab(cat.categoryId || cat.categoryName) === dormGroup.linkId
                            ? 'bg-sky-500 text-white shadow-sm'
                            : 'bg-sky-50 text-sky-700 hover:bg-sky-100 border border-sky-200'
                        ]"
                      >
                        {{ dormGroup.linkName }}
                        <span class="ml-1 px-1.5 py-0.5 rounded text-[10px]" :class="getCategoryDormitoryTab(cat.categoryId || cat.categoryName) === dormGroup.linkId ? 'bg-white/20' : 'bg-sky-100'">
                          {{ dormGroup.count }}
                        </span>
                      </button>
                    </template>

                    <!-- 教室分组 -->
                    <template v-if="getClassroomGroupsForCategory(cat).length > 0">
                      <span class="text-slate-300 mx-1">|</span>
                      <span class="text-xs text-slate-400 font-medium flex items-center gap-1">
                        <Building class="w-3.5 h-3.5" />教室
                      </span>
                      <button
                        v-for="classGroup in getClassroomGroupsForCategory(cat)"
                        :key="'class-' + classGroup.linkId"
                        @click="setCategoryDormitoryTab(cat.categoryId || cat.categoryName, classGroup.linkId)"
                        :class="[
                          'px-2.5 py-1.5 rounded-lg text-xs font-medium transition-all',
                          getCategoryDormitoryTab(cat.categoryId || cat.categoryName) === classGroup.linkId
                            ? 'bg-emerald-500 text-white shadow-sm'
                            : 'bg-emerald-50 text-emerald-700 hover:bg-emerald-100 border border-emerald-200'
                        ]"
                      >
                        {{ classGroup.linkName }}
                        <span class="ml-1 px-1.5 py-0.5 rounded text-[10px]" :class="getCategoryDormitoryTab(cat.categoryId || cat.categoryName) === classGroup.linkId ? 'bg-white/20' : 'bg-emerald-100'">
                          {{ classGroup.count }}
                        </span>
                      </button>
                    </template>
                  </div>
                </div>
                <div v-if="getFilteredDeductionsForCategory(cat).length" class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5 gap-2">
                  <div
                    v-for="(item, itemIdx) in getFilteredDeductionsForCategory(cat)"
                    :key="item.id"
                    class="deduction-card bg-white rounded-lg border border-slate-200 overflow-hidden hover:shadow-md hover:border-slate-300 transition-all"
                    :class="{ 'border-l-2 border-l-amber-400': item.appealStatus > 0 }"
                    :style="activeClass?.weightEnabled && weightSchemes.length > 0 ? { borderTopWidth: '3px', borderTopStyle: 'solid', borderTopColor: getItemSchemeColor(cat, itemIdx) } : {}"
                  >
                    <!-- 卡片头部：名称+分数 -->
                    <div class="flex items-center justify-between gap-2 px-2.5 py-2 bg-slate-50/80">
                      <span class="text-xs font-medium text-slate-800 truncate flex-1" :title="item.deductionItemName">
                        {{ item.deductionItemName }}
                      </span>
                      <div class="flex-shrink-0 flex items-center gap-1">
                        <span class="px-1.5 py-0.5 rounded bg-rose-500 text-white text-xs font-bold">
                          -{{ formatScore(item.actualScore) }}
                        </span>
                        <template v-if="activeClass?.weightEnabled && (activeClass?.weightFactor || activeClass?.multiConfigEnabled)">
                          <span class="text-[10px] text-indigo-500">→</span>
                          <span class="px-1.5 py-0.5 rounded bg-indigo-500 text-white text-xs font-bold">
                            -{{ formatScore(calculateWeightedItemScore(item.actualScore, cat.categoryName)) }}
                          </span>
                        </template>
                      </div>
                    </div>

                    <!-- 卡片内容 -->
                    <div class="px-2.5 py-2 space-y-1.5 text-[11px]">
                      <!-- 标签行：轮次+位置+申诉 -->
                      <div class="flex flex-wrap items-center gap-1">
                        <span v-if="item.checkRound > 1" class="px-1 py-0.5 rounded bg-indigo-50 text-indigo-600">
                          第{{ item.checkRound }}轮
                        </span>
                        <span v-if="item.linkType === 1" class="inline-flex items-center gap-0.5 px-1 py-0.5 rounded bg-sky-50 text-sky-600">
                          <Home class="w-2.5 h-2.5" />{{ item.linkName || item.linkCode }}
                        </span>
                        <span v-else-if="item.linkType === 2" class="inline-flex items-center gap-0.5 px-1 py-0.5 rounded bg-emerald-50 text-emerald-600">
                          <Building class="w-2.5 h-2.5" />{{ item.linkName || item.linkCode }}
                        </span>
                        <span v-if="item.appealStatus === 1" class="px-1 py-0.5 rounded bg-amber-50 text-amber-600">申诉中</span>
                        <span v-else-if="item.appealStatus === 2" class="px-1 py-0.5 rounded bg-green-50 text-green-600">已通过</span>
                        <span v-else-if="item.appealStatus === 3" class="px-1 py-0.5 rounded bg-rose-50 text-rose-600">已驳回</span>
                      </div>

                      <!-- 操作按钮行 -->
                      <div class="flex items-center gap-1 pt-1 border-t border-slate-100">
                        <!-- 学生按钮 -->
                        <button
                          v-if="item.personCount > 0"
                          @click.stop="showStudentList(item)"
                          class="flex items-center gap-1 px-1.5 py-1 rounded bg-purple-50 text-purple-600 hover:bg-purple-100 transition-colors"
                        >
                          <Users class="w-3 h-3" />
                          <span>{{ item.personCount }}人</span>
                        </button>

                        <!-- 备注按钮 -->
                        <button
                          v-if="item.remark"
                          @click.stop="showRemark(item)"
                          class="flex items-center gap-1 px-1.5 py-1 rounded bg-amber-50 text-amber-600 hover:bg-amber-100 transition-colors"
                        >
                          <MessageSquare class="w-3 h-3" />
                          <span>备注</span>
                        </button>

                        <!-- 照片按钮 -->
                        <button
                          v-if="item.photoUrlList?.length"
                          @click.stop="viewPhotos(item.photoUrlList, 0)"
                          class="flex items-center gap-1 px-1.5 py-1 rounded bg-violet-50 text-violet-600 hover:bg-violet-100 transition-colors"
                        >
                          <Camera class="w-3 h-3" />
                          <span>{{ item.photoUrlList.length }}张</span>
                        </button>

                        <!-- 申诉按钮 -->
                        <button
                          v-if="item.appealStatus === 0 || item.appealStatus === undefined"
                          @click.stop="openAppealDialog(item)"
                          class="flex items-center gap-1 px-1.5 py-1 rounded bg-orange-50 text-orange-600 hover:bg-orange-100 transition-colors ml-auto"
                        >
                          <AlertTriangle class="w-3 h-3" />
                          <span>申诉</span>
                        </button>

                        <!-- 无额外信息时显示占位 -->
                        <span v-if="!item.personCount && !item.remark && !item.photoUrlList?.length && (item.appealStatus > 0)" class="text-slate-400">
                          无附加信息
                        </span>
                      </div>
                    </div>
                  </div>
                </div>
                <div v-else class="text-center py-8 text-slate-400 text-sm">
                  {{ getCategoryDormitoryTab(cat.categoryId || cat.categoryName) !== 'all' ? '当前宿舍暂无扣分记录' : '该类别暂无扣分记录' }}
                </div>
              </div>
            </div>
          </div>

          <!-- 无扣分记录 -->
          <div v-else class="flex flex-col items-center justify-center py-20 text-slate-400">
            <CheckCircle class="w-16 h-16 text-emerald-400 mb-4" />
            <p class="text-lg font-medium text-slate-600">{{ hasAnyDeductions ? '当前筛选条件下无扣分记录' : '该班级本次检查无扣分' }}</p>
            <p class="text-sm text-slate-400 mt-1">{{ hasAnyDeductions ? '请调整筛选条件' : '表现优秀！' }}</p>
          </div>
        </template>

        <!-- 未选择班级 -->
        <div v-else class="flex flex-col items-center justify-center h-full text-slate-400">
          <Users class="w-16 h-16 mb-4" />
          <p>请从左侧选择班级查看详情</p>
        </div>
      </main>
    </div>

    <!-- 主内容区 - 排名视图 -->
    <div v-else-if="activeView === 'ranking'" class="h-[calc(100vh-73px)] overflow-auto bg-gradient-to-br from-slate-50 to-slate-100">
      <div class="max-w-7xl mx-auto p-6">
        <!-- 排名头部信息 -->
        <div class="bg-white rounded-2xl shadow-sm border border-slate-200/60 p-5 mb-6">
          <div class="flex flex-wrap items-center justify-between gap-4">
            <!-- 左侧标题和排序 -->
            <div class="flex items-center gap-4">
              <div class="flex items-center gap-3">
                <div class="w-10 h-10 rounded-xl bg-gradient-to-br from-blue-500 to-indigo-600 flex items-center justify-center">
                  <Trophy class="w-5 h-5 text-white" />
                </div>
                <div>
                  <h2 class="font-bold text-lg text-slate-800">班级排名</h2>
                  <p class="text-xs text-slate-400">{{ recordInfo.checkName || '检查记录' }}</p>
                </div>
              </div>

              <!-- 排序方式 -->
              <div class="flex items-center gap-2 ml-4 pl-4 border-l border-slate-200">
                <span class="text-xs text-slate-400">排序依据:</span>
                <div class="flex rounded-xl bg-slate-100 p-1">
                  <button
                    @click="rankingSortBy = 'weighted'"
                    :class="[
                      'px-3 py-1.5 rounded-lg text-xs font-medium transition-all',
                      rankingSortBy === 'weighted'
                        ? 'bg-white text-indigo-600 shadow-sm'
                        : 'text-slate-500 hover:text-slate-700'
                    ]"
                  >
                    加权分数
                  </button>
                  <button
                    @click="rankingSortBy = 'original'"
                    :class="[
                      'px-3 py-1.5 rounded-lg text-xs font-medium transition-all',
                      rankingSortBy === 'original'
                        ? 'bg-white text-indigo-600 shadow-sm'
                        : 'text-slate-500 hover:text-slate-700'
                    ]"
                  >
                    原始分数
                  </button>
                </div>
              </div>
            </div>

            <!-- 右侧统计 -->
            <div class="flex items-center gap-3">
              <div class="text-center px-4 py-2 bg-slate-50 rounded-xl">
                <div class="text-xl font-bold text-slate-700">{{ rankingList.length }}</div>
                <div class="text-[10px] text-slate-400">参检班级</div>
              </div>
              <div v-if="hasWeightEnabled" class="flex items-center gap-1.5 px-3 py-2 rounded-xl bg-emerald-50 border border-emerald-100">
                <Scale class="w-4 h-4 text-emerald-500" />
                <span class="text-xs font-medium text-emerald-600">加权已启用</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 加载中 -->
        <div v-if="rankingLoading" class="bg-white rounded-2xl shadow-sm border border-slate-200/60 flex flex-col items-center justify-center py-20 text-slate-500">
          <Loader2 class="w-10 h-10 animate-spin mb-4 text-indigo-500" />
          <span>正在加载排名数据...</span>
        </div>

        <!-- 排名列表 -->
        <div v-else class="space-y-3">
          <!-- 表头 -->
          <div class="bg-white/80 backdrop-blur-sm rounded-xl px-4 py-3 border border-slate-200/60 sticky top-0 z-10">
            <div class="grid items-center gap-4" :style="{ gridTemplateColumns: '60px minmax(180px, 250px) 80px 60px repeat(' + dynamicCategories.length + ', 90px) 100px' }">
              <div class="text-xs font-semibold text-slate-500 uppercase">排名</div>
              <div class="text-xs font-semibold text-slate-500 uppercase">班级</div>
              <div class="text-xs font-semibold text-slate-500 uppercase">班主任</div>
              <div class="text-xs font-semibold text-slate-500 uppercase text-center">人数</div>
              <div
                v-for="cat in dynamicCategories"
                :key="'header-' + (cat.categoryId || cat.categoryName)"
                class="text-xs font-semibold text-slate-500 uppercase text-center"
              >
                {{ cat.categoryName }}
              </div>
              <div class="text-xs font-semibold text-slate-500 uppercase text-center">总计</div>
            </div>
          </div>

          <!-- 排名卡片列表 -->
          <div
            v-for="(item, index) in rankingList"
            :key="item.classId"
            class="bg-white rounded-xl border border-slate-200/60 hover:shadow-md hover:border-slate-300/60 transition-all duration-200 overflow-hidden"
            :class="{
              'ring-2 ring-amber-400/50 border-amber-200': index === 0,
              'ring-1 ring-slate-300/50': index === 1,
              'ring-1 ring-amber-600/30': index === 2
            }"
          >
            <div class="px-4 py-3 grid items-center gap-4" :style="{ gridTemplateColumns: '60px minmax(180px, 250px) 80px 60px repeat(' + dynamicCategories.length + ', 90px) 100px' }">
              <!-- 排名 -->
              <div class="flex justify-center">
                <div
                  :class="[
                    'w-9 h-9 rounded-xl flex items-center justify-center font-bold text-sm transition-transform hover:scale-105',
                    index === 0 ? 'bg-gradient-to-br from-amber-400 to-yellow-500 text-white shadow-lg shadow-amber-200/50' :
                    index === 1 ? 'bg-gradient-to-br from-slate-300 to-slate-400 text-white shadow-md' :
                    index === 2 ? 'bg-gradient-to-br from-amber-600 to-orange-500 text-white shadow-md' :
                    'bg-slate-100 text-slate-600'
                  ]"
                >
                  {{ (rankingSortBy === 'weighted' ? item.weightedOverallRanking : item.overallRanking) || index + 1 }}
                </div>
              </div>

              <!-- 班级 -->
              <div class="flex items-center gap-3">
                <div class="w-10 h-10 rounded-xl bg-gradient-to-br from-blue-500 to-indigo-600 flex items-center justify-center text-white font-bold text-sm flex-shrink-0">
                  {{ item.className?.charAt(0) || '班' }}
                </div>
                <div class="min-w-0">
                  <div class="font-semibold text-slate-800 truncate">{{ item.className }}</div>
                  <div class="text-xs text-slate-400 truncate">{{ item.gradeName || item.departmentName || '-' }}</div>
                </div>
              </div>

              <!-- 班主任 -->
              <div class="text-sm text-slate-600 truncate">{{ item.teacherName || '-' }}</div>

              <!-- 人数 -->
              <div class="text-center">
                <span class="inline-flex items-center justify-center min-w-[2rem] px-2 py-1 rounded-lg bg-slate-100 text-slate-700 text-sm font-medium">
                  {{ item.classSize || '-' }}
                </span>
              </div>

              <!-- 动态类别分数 (扣分/加权扣分) -->
              <div
                v-for="cat in dynamicCategories"
                :key="`${item.classId}-${cat.categoryId || cat.categoryName}`"
                class="text-center"
              >
                <span class="text-sm font-medium">
                  <span
                    :class="[
                      getCategoryScore(item, cat.categoryName, false) === 0 ? 'text-emerald-500' :
                      getCategoryScore(item, cat.categoryName, false) < 3 ? 'text-amber-500' : 'text-rose-500'
                    ]"
                  >{{ getCategoryScore(item, cat.categoryName, false) === 0 ? '0' : '-' + formatScore(getCategoryScore(item, cat.categoryName, false)) }}</span>
                  <template v-if="hasWeightEnabled && item.weightEnabled">
                    <span class="text-slate-300">/</span>
                    <span class="text-indigo-500">{{ getRankingWeightedCategoryScore(item, cat.categoryName) === 0 ? '0' : '-' + formatScore(getRankingWeightedCategoryScore(item, cat.categoryName)) }}</span>
                  </template>
                </span>
              </div>

              <!-- 总分 (扣分/加权扣分) -->
              <div class="text-center">
                <span class="text-sm font-bold">
                  <span class="text-rose-600">-{{ formatScore(item.totalScore) }}</span>
                  <template v-if="hasWeightEnabled && item.weightEnabled && item.weightedTotalScore != null">
                    <span class="text-slate-300">/</span>
                    <span class="text-indigo-600">-{{ formatScore(item.weightedTotalScore) }}</span>
                  </template>
                </span>
              </div>
            </div>
          </div>

          <!-- 空状态 -->
          <div v-if="!rankingList.length" class="bg-white rounded-2xl shadow-sm border border-slate-200/60 flex flex-col items-center justify-center py-20 text-slate-400">
            <Trophy class="w-16 h-16 mb-4 text-slate-300" />
            <p class="text-lg font-medium text-slate-500">暂无排名数据</p>
          </div>
        </div>

        <!-- 加权说明 -->
        <div v-if="hasWeightEnabled" class="mt-6 p-4 bg-gradient-to-r from-indigo-50 to-purple-50 border border-indigo-100/60 rounded-2xl">
          <div class="flex items-start gap-3">
            <div class="w-8 h-8 rounded-lg bg-indigo-100 flex items-center justify-center flex-shrink-0">
              <Info class="w-4 h-4 text-indigo-500" />
            </div>
            <div class="text-sm">
              <p class="font-semibold text-indigo-700 mb-1">加权分数说明</p>
              <p class="text-indigo-600/80 leading-relaxed">加权分数根据班级人数与标准人数的比例进行调整，使得不同规模班级的扣分可以公平比较。系数小于1表示有利于该班级（人数少），大于1表示不利（人数多）。</p>
            </div>
          </div>
        </div>

      </div>
    </div>

    <!-- 主内容区 - 评级结果视图 -->
    <div v-else-if="activeView === 'rating'" class="h-[calc(100vh-73px)] overflow-auto bg-slate-50 p-4">
      <!-- 评级结果网格 -->
      <div class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4" v-if="ratingRules.length > 0">
        <div v-for="rule in ratingRules" :key="rule.id">
          <RatingResultCard
            :check-record-id="String(route.params.id)"
            :rule-id="rule.id"
            :rule-name="rule.ruleName"
            compact
          />
        </div>
      </div>

      <!-- 空状态 -->
      <div v-else class="flex flex-col items-center justify-center h-full text-slate-400">
        <BarChart3 class="w-12 h-12 mb-3 text-slate-300" />
        <p class="text-sm text-slate-500">暂无评级规则</p>
      </div>
    </div>

    <!-- 照片预览弹窗 -->
    <Teleport to="body">
      <div
        v-if="photoVisible"
        class="fixed inset-0 z-[1000] flex items-center justify-center bg-black/90"
        @click="photoVisible = false"
      >
        <button
          class="absolute top-5 right-5 w-12 h-12 rounded-full bg-white/10 hover:bg-white/20 text-white flex items-center justify-center transition-colors"
          @click.stop="photoVisible = false"
        >
          <X class="w-6 h-6" />
        </button>
        <div class="relative max-w-[90vw] max-h-[85vh]" @click.stop>
          <button
            v-if="currentPhotoIndex > 0"
            class="absolute left-[-60px] top-1/2 -translate-y-1/2 w-12 h-12 rounded-full bg-white/10 hover:bg-white/20 text-white flex items-center justify-center transition-colors"
            @click="currentPhotoIndex--"
          >
            <ChevronLeft class="w-8 h-8" />
          </button>
          <img :src="currentPhotos[currentPhotoIndex]" class="max-w-full max-h-[85vh] rounded-lg" />
          <button
            v-if="currentPhotoIndex < currentPhotos.length - 1"
            class="absolute right-[-60px] top-1/2 -translate-y-1/2 w-12 h-12 rounded-full bg-white/10 hover:bg-white/20 text-white flex items-center justify-center transition-colors"
            @click="currentPhotoIndex++"
          >
            <ChevronRight class="w-8 h-8" />
          </button>
        </div>
        <div class="absolute bottom-5 left-1/2 -translate-x-1/2 px-4 py-2 rounded-full bg-black/60 text-white text-sm">
          {{ currentPhotoIndex + 1 }} / {{ currentPhotos.length }}
        </div>
      </div>
    </Teleport>

    <!-- 学生列表弹窗 -->
    <Teleport to="body">
      <div
        v-if="studentDialogVisible"
        class="fixed inset-0 z-[1000] flex items-center justify-center bg-black/50"
        @click="studentDialogVisible = false"
      >
        <div
          class="relative w-full max-w-md bg-white rounded-2xl shadow-2xl overflow-hidden"
          @click.stop
        >
          <div class="flex items-center justify-between px-6 py-4 border-b border-slate-200 bg-gradient-to-r from-purple-500 to-indigo-600">
            <div class="flex items-center gap-3 text-white">
              <Users class="w-5 h-5" />
              <span class="font-semibold">关联学生</span>
            </div>
            <button
              @click="studentDialogVisible = false"
              class="w-8 h-8 rounded-lg bg-white/20 hover:bg-white/30 flex items-center justify-center transition-colors"
            >
              <X class="w-4 h-4 text-white" />
            </button>
          </div>
          <div class="p-4">
            <div class="text-sm text-slate-500 mb-3">
              扣分项: <span class="font-medium text-slate-700">{{ currentDeductionItem?.deductionItemName }}</span>
            </div>
            <div class="space-y-2 max-h-64 overflow-y-auto">
              <div
                v-for="(name, idx) in currentStudentList"
                :key="idx"
                class="flex items-center gap-3 px-4 py-3 rounded-xl bg-slate-50 hover:bg-slate-100 transition-colors"
              >
                <div class="w-8 h-8 rounded-full bg-gradient-to-br from-purple-400 to-indigo-500 flex items-center justify-center text-white text-sm font-medium">
                  {{ name.charAt(0) }}
                </div>
                <span class="text-sm text-slate-700">{{ name }}</span>
              </div>
            </div>
            <div v-if="!currentStudentList.length" class="text-center py-8 text-slate-400 text-sm">
              暂无学生信息
            </div>
          </div>
          <div class="px-6 py-4 border-t border-slate-200 bg-slate-50">
            <button
              @click="studentDialogVisible = false"
              class="w-full py-2.5 rounded-xl bg-slate-200 hover:bg-slate-300 text-slate-700 font-medium transition-colors"
            >
              关闭
            </button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- 备注弹窗 -->
    <Teleport to="body">
      <div
        v-if="remarkDialogVisible"
        class="fixed inset-0 z-[1000] flex items-center justify-center bg-black/50"
        @click="remarkDialogVisible = false"
      >
        <div
          class="relative w-full max-w-md bg-white rounded-2xl shadow-2xl overflow-hidden"
          @click.stop
        >
          <div class="flex items-center justify-between px-6 py-4 border-b border-slate-200 bg-gradient-to-r from-amber-500 to-orange-500">
            <div class="flex items-center gap-3 text-white">
              <MessageSquare class="w-5 h-5" />
              <span class="font-semibold">扣分备注</span>
            </div>
            <button
              @click="remarkDialogVisible = false"
              class="w-8 h-8 rounded-lg bg-white/20 hover:bg-white/30 flex items-center justify-center transition-colors"
            >
              <X class="w-4 h-4 text-white" />
            </button>
          </div>
          <div class="p-6">
            <div class="text-sm text-slate-500 mb-3">
              扣分项: <span class="font-medium text-slate-700">{{ currentDeductionItem?.deductionItemName }}</span>
            </div>
            <div class="p-4 rounded-xl bg-amber-50 border border-amber-100">
              <p class="text-sm text-slate-700 leading-relaxed whitespace-pre-wrap">{{ currentRemark }}</p>
            </div>
          </div>
          <div class="px-6 py-4 border-t border-slate-200 bg-slate-50">
            <button
              @click="remarkDialogVisible = false"
              class="w-full py-2.5 rounded-xl bg-slate-200 hover:bg-slate-300 text-slate-700 font-medium transition-colors"
            >
              关闭
            </button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- 申诉弹窗 -->
    <Teleport to="body">
      <Transition
        enter-active-class="transition duration-300 ease-out"
        enter-from-class="opacity-0"
        enter-to-class="opacity-100"
        leave-active-class="transition duration-200 ease-in"
        leave-from-class="opacity-100"
        leave-to-class="opacity-0"
      >
        <div
          v-if="appealDialogVisible"
          class="fixed inset-0 z-[1000] flex items-center justify-center p-4 bg-slate-900/50 backdrop-blur-sm"
          @click="appealDialogVisible = false"
        >
          <Transition
            enter-active-class="transition duration-300 ease-out"
            enter-from-class="opacity-0 scale-95"
            enter-to-class="opacity-100 scale-100"
            leave-active-class="transition duration-200 ease-in"
            leave-from-class="opacity-100 scale-100"
            leave-to-class="opacity-0 scale-95"
          >
            <div
              v-if="appealDialogVisible"
              class="relative w-full max-w-lg bg-white rounded-2xl shadow-xl overflow-hidden"
              @click.stop
            >
              <!-- 头部 -->
              <div class="flex items-center justify-between px-6 py-4 border-b border-slate-100">
                <div class="flex items-center gap-3">
                  <div class="w-10 h-10 rounded-xl bg-indigo-50 flex items-center justify-center">
                    <Send class="w-5 h-5 text-indigo-600" />
                  </div>
                  <div>
                    <h2 class="text-lg font-semibold text-slate-800">提交申诉</h2>
                    <p class="text-xs text-slate-400">对扣分项提出异议申请</p>
                  </div>
                </div>
                <button
                  @click="appealDialogVisible = false"
                  class="w-8 h-8 rounded-lg hover:bg-slate-100 flex items-center justify-center transition-colors"
                >
                  <X class="w-5 h-5 text-slate-400" />
                </button>
              </div>

              <!-- 内容区域 -->
              <div class="p-6 space-y-5 max-h-[70vh] overflow-y-auto">
                <!-- 扣分项信息 -->
                <div class="flex items-center gap-4 p-4 rounded-xl bg-slate-50 border border-slate-100">
                  <div class="flex-1 min-w-0">
                    <div class="text-xs text-slate-400 mb-1">申诉项目</div>
                    <div class="font-medium text-slate-800 truncate">{{ appealDeductionItem?.deductionItemName }}</div>
                  </div>
                  <div class="flex items-center gap-4 flex-shrink-0">
                    <div class="text-center">
                      <div class="text-xs text-slate-400 mb-1">扣分</div>
                      <div class="text-lg font-bold text-rose-500">-{{ formatScore(appealDeductionItem?.actualScore) }}</div>
                    </div>
                    <div class="w-px h-8 bg-slate-200"></div>
                    <div class="text-center">
                      <div class="text-xs text-slate-400 mb-1">班级</div>
                      <div class="text-sm font-medium text-slate-700">{{ activeClass?.className }}</div>
                    </div>
                  </div>
                </div>

                <!-- 申诉类型 -->
                <div>
                  <label class="block text-sm font-medium text-slate-700 mb-2">申诉类型</label>
                  <div class="grid grid-cols-4 gap-2">
                    <button
                      v-for="type in appealTypes"
                      :key="type.value"
                      @click="appealForm.appealType = type.value"
                      :class="[
                        'px-3 py-2 rounded-lg text-sm font-medium transition-all border',
                        appealForm.appealType === type.value
                          ? 'bg-indigo-600 border-indigo-600 text-white'
                          : 'bg-white border-slate-200 text-slate-600 hover:border-indigo-300 hover:text-indigo-600'
                      ]"
                    >
                      {{ type.label }}
                    </button>
                  </div>
                </div>

                <!-- 申诉理由 -->
                <div>
                  <label class="block text-sm font-medium text-slate-700 mb-2">
                    申诉理由 <span class="text-rose-500">*</span>
                  </label>
                  <textarea
                    v-model="appealForm.reason"
                    rows="4"
                    maxlength="200"
                    class="w-full px-4 py-3 rounded-lg border border-slate-200 text-sm text-slate-800 placeholder-slate-400 focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500/20 outline-none transition-all resize-none"
                    placeholder="请详细说明您的申诉理由，以便审核人员了解情况..."
                  ></textarea>
                  <div class="flex justify-end mt-1">
                    <span class="text-xs text-slate-400">{{ appealForm.reason.length }}/200</span>
                  </div>
                </div>

                <!-- 期望分数和联系电话 -->
                <div class="grid grid-cols-2 gap-4">
                  <div>
                    <label class="block text-sm font-medium text-slate-700 mb-2">期望扣分</label>
                    <div class="relative">
                      <input
                        v-model.number="appealForm.expectedScore"
                        type="number"
                        step="0.5"
                        min="0"
                        class="w-full px-4 py-2.5 pr-10 rounded-lg border border-slate-200 text-sm text-slate-800 placeholder-slate-400 focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500/20 outline-none transition-all"
                        placeholder="0"
                      />
                      <span class="absolute right-4 top-1/2 -translate-y-1/2 text-sm text-slate-400">分</span>
                    </div>
                    <p class="text-xs text-slate-400 mt-1">您认为合理的扣分分数</p>
                  </div>
                  <div>
                    <label class="block text-sm font-medium text-slate-700 mb-2">联系电话</label>
                    <input
                      v-model="appealForm.contactPhone"
                      type="tel"
                      class="w-full px-4 py-2.5 rounded-lg border border-slate-200 text-sm text-slate-800 placeholder-slate-400 focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500/20 outline-none transition-all"
                      placeholder="选填"
                    />
                    <p class="text-xs text-slate-400 mt-1">方便工作人员联系您</p>
                  </div>
                </div>
              </div>

              <!-- 底部按钮 -->
              <div class="flex items-center gap-3 px-6 py-4 border-t border-slate-100 bg-slate-50/50">
                <button
                  @click="appealDialogVisible = false"
                  class="flex-1 px-4 py-2.5 rounded-lg border border-slate-200 bg-white text-slate-600 font-medium hover:bg-slate-50 transition-colors"
                >
                  取消
                </button>
                <button
                  @click="submitAppeal"
                  :disabled="appealSubmitting || !appealForm.reason.trim()"
                  class="flex-1 px-4 py-2.5 rounded-lg bg-indigo-600 hover:bg-indigo-700 disabled:bg-slate-300 text-white font-medium transition-colors flex items-center justify-center gap-2"
                >
                  <Loader2 v-if="appealSubmitting" class="w-4 h-4 animate-spin" />
                  <Send v-else class="w-4 h-4" />
                  <span>提交申诉</span>
                </button>
              </div>
            </div>
          </Transition>
        </div>
      </Transition>
    </Teleport>

    <!-- 加权配置详情弹窗 -->
    <WeightConfigDetailDialog
      v-model="weightConfigDialogVisible"
      :record-id="String(route.params.id)"
    />

    <!-- 加权配置树形结构弹窗 -->
    <WeightConfigTreeDialog
      v-model="weightConfigTreeDialogVisible"
      :record-id="String(route.params.id)"
    />

    <!-- 班级申诉记录弹窗 -->
    <Teleport to="body">
      <div
        v-if="appealHistoryDialogVisible"
        class="fixed inset-0 z-[1000] flex items-center justify-center p-4 bg-slate-900/50 backdrop-blur-sm"
        @click="appealHistoryDialogVisible = false"
      >
        <div
          class="relative w-full max-w-3xl bg-white rounded-2xl shadow-xl overflow-hidden"
          @click.stop
        >
          <!-- 头部 -->
          <div class="flex items-center justify-between px-6 py-4 border-b border-slate-100 bg-gradient-to-r from-amber-500 to-orange-500">
            <div class="flex items-center gap-3 text-white">
              <FileText class="w-5 h-5" />
              <span class="font-semibold">{{ activeClass?.className }} - 申诉记录</span>
            </div>
            <button
              @click="appealHistoryDialogVisible = false"
              class="w-8 h-8 rounded-lg bg-white/20 hover:bg-white/30 flex items-center justify-center transition-colors"
            >
              <X class="w-4 h-4 text-white" />
            </button>
          </div>

          <!-- 内容区域 -->
          <div class="p-6 max-h-[70vh] overflow-y-auto">
            <!-- 加载中 -->
            <div v-if="appealHistoryLoading" class="flex items-center justify-center py-12">
              <Loader2 class="w-8 h-8 animate-spin text-amber-500" />
            </div>

            <!-- 申诉列表 -->
            <div v-else-if="appealHistoryList.length > 0" class="space-y-4">
              <div
                v-for="appeal in appealHistoryList"
                :key="appeal.id"
                class="p-4 rounded-xl border border-slate-200 hover:border-slate-300 transition-colors"
              >
                <div class="flex items-start justify-between gap-4">
                  <div class="flex-1 min-w-0">
                    <div class="flex items-center gap-2 mb-2">
                      <span class="text-sm font-medium text-slate-800">{{ appeal.itemName }}</span>
                      <span
                        :class="[
                          'px-2 py-0.5 rounded-full text-xs font-medium',
                          appeal.status === 1 ? 'bg-amber-100 text-amber-700' :
                          appeal.status === 2 ? 'bg-green-100 text-green-700' :
                          appeal.status === 3 ? 'bg-rose-100 text-rose-700' :
                          appeal.status === 4 ? 'bg-slate-100 text-slate-600' :
                          'bg-slate-100 text-slate-600'
                        ]"
                      >
                        {{ appeal.statusDesc || getAppealStatusText(appeal.status) }}
                      </span>
                    </div>
                    <p class="text-sm text-slate-500 line-clamp-2 mb-2">{{ appeal.reason }}</p>
                    <div class="flex items-center gap-4 text-xs text-slate-400">
                      <span>申诉编号: {{ appeal.appealCode }}</span>
                      <span>申诉人: {{ appeal.applicantName }}</span>
                      <span>{{ appeal.createdAt }}</span>
                    </div>
                  </div>
                  <div class="flex-shrink-0 text-right">
                    <div class="text-sm">
                      <span class="text-slate-400">原始扣分:</span>
                      <span class="text-rose-500 font-medium ml-1">-{{ appeal.originalScore }}</span>
                    </div>
                    <div v-if="appeal.adjustedScore !== null && appeal.adjustedScore !== undefined" class="text-sm mt-1">
                      <span class="text-slate-400">调整后:</span>
                      <span class="text-green-600 font-medium ml-1">-{{ appeal.adjustedScore }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <!-- 空状态 -->
            <div v-else class="flex flex-col items-center justify-center py-12 text-slate-400">
              <FileText class="w-12 h-12 mb-4 text-slate-300" />
              <p class="text-sm">该班级暂无申诉记录</p>
            </div>
          </div>

          <!-- 底部 -->
          <div class="px-6 py-4 border-t border-slate-100 bg-slate-50">
            <button
              @click="appealHistoryDialogVisible = false"
              class="w-full py-2.5 rounded-xl bg-slate-200 hover:bg-slate-300 text-slate-700 font-medium transition-colors"
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
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  ArrowLeft, Loader2, Home, Building, Camera, MessageSquare,
  CheckCircle, X, ChevronLeft, ChevronRight, ChevronDown, Users,
  AlertCircle, ClipboardCheck, Sparkles, BookOpen, Moon, Filter, Layers,
  LayoutList, Trophy, Scale, Info, AlertTriangle, Send, BarChart3, Eye,
  RefreshCw, MapPin, FileText
} from 'lucide-vue-next'
import { getCheckRecordDetail, getClassRanking, getWeightConfigTree } from '@/api/v2/quantification'
import { createAppeal, getClassAppealHistory } from '@/api/v2/appeal'
import type { ClassStats, CheckRecordCategoryStatsDTO, DeductionDetail } from '@/api/v2/quantification'
import WeightConfigDetailDialog from '@/components/quantification/WeightConfigDetailDialog.vue'
import WeightConfigTreeDialog from '@/components/quantification/WeightConfigTreeDialog.vue'
import ClassWeightConfigView from '@/components/quantification/ClassWeightConfigView.vue'
import RatingResultCard from './components/rating/RatingResultCard.vue'
import { getRatingConfigsByPlan, type RatingConfigVO } from '@/api/v2/rating'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const recordInfo = ref<Record<string, any>>({})
const classList = ref<ClassStats[]>([])
const activeClassId = ref('')
const expandedCategories = ref<(string | number)[]>([])

// 班级列表筛选
const filterDepartment = ref<string>('')
const filterGrade = ref<string>('')

// 部门选项（从classList提取）
const departmentOptions = computed(() => {
  const depts = new Map<string, string>()
  classList.value.forEach(cls => {
    if (cls.departmentId && cls.departmentName) {
      depts.set(String(cls.departmentId), cls.departmentName)
    }
  })
  return Array.from(depts.entries()).map(([value, label]) => ({ value, label }))
})

// 年级选项（从classList提取）
const gradeOptions = computed(() => {
  const grades = new Map<string, string>()
  classList.value.forEach(cls => {
    if (cls.gradeId && cls.gradeName) {
      grades.set(String(cls.gradeId), cls.gradeName)
    }
  })
  return Array.from(grades.entries()).map(([value, label]) => ({ value, label }))
})

// 筛选后的班级列表
const filteredClassList = computed(() => {
  let filtered = classList.value

  if (filterDepartment.value) {
    filtered = filtered.filter(cls => String(cls.departmentId) === filterDepartment.value)
  }

  if (filterGrade.value) {
    filtered = filtered.filter(cls => String(cls.gradeId) === filterGrade.value)
  }

  return filtered
})

// 视图切换状态
const activeView = ref<'detail' | 'ranking' | 'rating'>('detail')

// 排名视图状态
const rankingLoading = ref(false)
const rankingList = ref<ClassStats[]>([])
const rankingSortBy = ref<'weighted' | 'original'>('weighted')

// 评级规则
const ratingRules = ref<RatingConfigVO[]>([])

// 是否有班级启用了加权
const hasWeightEnabled = computed(() => rankingList.value.some(r => r.weightEnabled))

// Tab 筛选状态
const activeLocationTab = ref<'all' | 'dormitory' | 'classroom'>('all')
const activeRoundTab = ref<number>(0) // 0 表示全部

// 每个类别的宿舍分组Tab状态（按类别ID/名称存储当前选中的宿舍）
const categoryDormitoryTabs = ref<Record<string, string>>({})

// 获取某个类别当前选中的宿舍Tab
const getCategoryDormitoryTab = (categoryKey: string | number): string => {
  return categoryDormitoryTabs.value[String(categoryKey)] || 'all'
}

// 设置某个类别的宿舍Tab
const setCategoryDormitoryTab = (categoryKey: string | number, dormitoryId: string) => {
  categoryDormitoryTabs.value[String(categoryKey)] = dormitoryId
}

// 获取某个类别下的所有宿舍分组（按当前轮次过滤）
const getDormitoryGroupsForCategory = (cat: CheckRecordCategoryStatsDTO): { linkId: string; linkName: string; count: number }[] => {
  const deductions = cat.deductions || []
  const categoryKey = String(cat.categoryId || cat.categoryName)
  const selectedRound = getCategoryRoundTab(categoryKey)

  // 只统计关联宿舍的扣分项（linkType === 1），并按轮次过滤
  let dormitoryDeductions = deductions.filter(d => d.linkType === 1 && d.linkId)
  if (selectedRound !== 0) {
    dormitoryDeductions = dormitoryDeductions.filter(d => (d.checkRound || 1) === selectedRound)
  }

  if (dormitoryDeductions.length === 0) return []

  const dormitoryMap = new Map<string, { linkId: string; linkName: string; count: number }>()

  dormitoryDeductions.forEach(d => {
    const linkId = String(d.linkId)
    if (!dormitoryMap.has(linkId)) {
      dormitoryMap.set(linkId, {
        linkId,
        linkName: d.linkName || d.linkCode || `宿舍${linkId}`,
        count: 0
      })
    }
    dormitoryMap.get(linkId)!.count++
  })

  return Array.from(dormitoryMap.values()).sort((a, b) => a.linkName.localeCompare(b.linkName))
}

// 获取某个类别下的所有教室分组（按当前轮次过滤）
const getClassroomGroupsForCategory = (cat: CheckRecordCategoryStatsDTO): { linkId: string; linkName: string; count: number }[] => {
  const deductions = cat.deductions || []
  const categoryKey = String(cat.categoryId || cat.categoryName)
  const selectedRound = getCategoryRoundTab(categoryKey)

  // 只统计关联教室的扣分项（linkType === 2），并按轮次过滤
  let classroomDeductions = deductions.filter(d => d.linkType === 2 && d.linkId)
  if (selectedRound !== 0) {
    classroomDeductions = classroomDeductions.filter(d => (d.checkRound || 1) === selectedRound)
  }

  if (classroomDeductions.length === 0) return []

  const classroomMap = new Map<string, { linkId: string; linkName: string; count: number }>()

  classroomDeductions.forEach(d => {
    const linkId = String(d.linkId)
    if (!classroomMap.has(linkId)) {
      classroomMap.set(linkId, {
        linkId,
        linkName: d.linkName || d.linkCode || `教室${linkId}`,
        count: 0
      })
    }
    classroomMap.get(linkId)!.count++
  })

  return Array.from(classroomMap.values()).sort((a, b) => a.linkName.localeCompare(b.linkName))
}

// 判断某个类别是否有关联的宿舍或教室
const hasLinkedLocationsForCategory = (cat: CheckRecordCategoryStatsDTO): boolean => {
  return getDormitoryGroupsForCategory(cat).length > 0 || getClassroomGroupsForCategory(cat).length > 0
}

// 获取某个类别下的所有轮次分组
const getRoundGroupsForCategory = (cat: CheckRecordCategoryStatsDTO): { round: number; label: string; count: number }[] => {
  const deductions = cat.deductions || []
  if (deductions.length === 0) return []

  const roundMap = new Map<number, number>()

  deductions.forEach(d => {
    const round = d.checkRound || 1
    roundMap.set(round, (roundMap.get(round) || 0) + 1)
  })

  return Array.from(roundMap.entries())
    .map(([round, count]) => ({
      round,
      label: `第${round}轮`,
      count
    }))
    .sort((a, b) => a.round - b.round)
}

// 判断某个类别是否有多个轮次
const hasMultipleRoundsForCategory = (cat: CheckRecordCategoryStatsDTO): boolean => {
  return getRoundGroupsForCategory(cat).length > 1
}

// 每个类别的轮次Tab状态
const categoryRoundTabs = ref<Record<string, number>>({})

// 获取某个类别当前选中的轮次Tab
const getCategoryRoundTab = (categoryKey: string | number): number => {
  return categoryRoundTabs.value[String(categoryKey)] ?? 0 // 0 表示全部
}

// 设置某个类别的轮次Tab
const setCategoryRoundTab = (categoryKey: string | number, round: number) => {
  categoryRoundTabs.value[String(categoryKey)] = round
}

// 获取过滤后的扣分项（考虑宿舍/教室Tab和轮次Tab选择）
const getFilteredDeductionsForCategory = (cat: CheckRecordCategoryStatsDTO): DeductionDetail[] => {
  let deductions = cat.deductions || []
  const categoryKey = String(cat.categoryId || cat.categoryName)

  // 1. 按位置筛选（宿舍/教室）
  const selectedLocation = getCategoryDormitoryTab(categoryKey)
  if (selectedLocation !== 'all') {
    deductions = deductions.filter(d => String(d.linkId) === selectedLocation)
  }

  // 2. 按轮次筛选
  const selectedRound = getCategoryRoundTab(categoryKey)
  if (selectedRound !== 0) {
    deductions = deductions.filter(d => (d.checkRound || 1) === selectedRound)
  }

  return deductions
}

// 照片预览
const photoVisible = ref(false)
const currentPhotos = ref<string[]>([])
const currentPhotoIndex = ref(0)

// 学生列表弹窗
const studentDialogVisible = ref(false)
const currentStudentList = ref<string[]>([])
const currentDeductionItem = ref<DeductionDetail | null>(null)

// 备注弹窗
const remarkDialogVisible = ref(false)
const currentRemark = ref('')

// 申诉弹窗
const appealDialogVisible = ref(false)

// 加权配置详情弹窗
const weightConfigDialogVisible = ref(false)
// 加权配置树形结构弹窗
const weightConfigTreeDialogVisible = ref(false)
// 班级加权配置展开状态
const weightConfigExpanded = ref(false)
const appealDeductionItem = ref<DeductionDetail | null>(null)
const appealSubmitting = ref(false)
const appealForm = ref({
  appealType: 1,
  reason: '',
  expectedScore: null as number | null,
  contactPhone: ''
})

// 申诉类型选项
const appealTypes = [
  { value: 1, label: '分数异议' },
  { value: 2, label: '事实不符' },
  { value: 3, label: '程序不当' },
  { value: 4, label: '其他原因' }
]

// 班级申诉记录弹窗
const appealHistoryDialogVisible = ref(false)
const appealHistoryLoading = ref(false)
const appealHistoryList = ref<any[]>([])

// 获取申诉状态文本
const getAppealStatusText = (status: number): string => {
  const statusMap: Record<number, string> = {
    1: '待审核',
    2: '已通过',
    3: '已驳回',
    4: '已撤销',
    5: '已过期',
    6: '公示中',
    7: '已生效'
  }
  return statusMap[status] || '未知'
}

// 打开申诉记录弹窗
const openAppealHistoryDialog = async () => {
  if (!activeClass.value) return

  appealHistoryDialogVisible.value = true
  appealHistoryLoading.value = true
  appealHistoryList.value = []

  try {
    const res = await getClassAppealHistory(activeClass.value.classId, 20)
    appealHistoryList.value = res || []
  } catch (error) {
    console.error('加载申诉记录失败:', error)
    ElMessage.error('加载申诉记录失败')
  } finally {
    appealHistoryLoading.value = false
  }
}

// 位置 Tab 配置
const locationTabs = [
  { value: 'all' as const, label: '全部', icon: Layers },
  { value: 'dormitory' as const, label: '宿舍', icon: Home },
  { value: 'classroom' as const, label: '教室', icon: Building }
]

// 加权方案相关
interface WeightScheme {
  id?: string
  name: string
  categoryIds?: string[]
}

// 加权方案颜色
const schemeColors = [
  '#667eea',  // 紫蓝
  '#f5576c',  // 玫红
  '#43e97b',  // 翠绿
  '#4facfe',  // 天蓝
  '#fa709a',  // 粉红
  '#a18cd1',  // 淡紫
  '#ffc107',  // 琥珀
  '#17a2b8',  // 青色
]

// 加权方案列表
const weightSchemes = ref<WeightScheme[]>([])
const weightTreeData = ref<any>({})

// 加载加权配置树数据
const loadWeightConfigTree = async () => {
  if (!route.params.id) return
  try {
    const res = await getWeightConfigTree(String(route.params.id))
    weightTreeData.value = res || {}

    // 从树数据中提取唯一的加权方案
    const schemeMap = new Map<string, WeightScheme>()

    // 添加全局配置
    if (weightTreeData.value.globalConfig) {
      const globalConfig = weightTreeData.value.globalConfig
      const key = globalConfig.configId || 'global'
      schemeMap.set(key, {
        id: key,
        name: globalConfig.configName || '默认配置',
        categoryIds: []
      })
    }

    // 遍历类别收集配置
    weightTreeData.value.categories?.forEach((category: any) => {
      const config = category.effectiveConfig
      if (config) {
        const key = config.configId || config.configName || 'default'
        if (!schemeMap.has(key)) {
          schemeMap.set(key, {
            id: key,
            name: config.configName || '默认配置',
            categoryIds: [category.categoryId]
          })
        } else {
          schemeMap.get(key)!.categoryIds?.push(category.categoryId)
        }
      }
    })

    weightSchemes.value = Array.from(schemeMap.values())
  } catch (error) {
    console.error('加载加权配置树失败:', error)
  }
}

// 获取扣分项的方案颜色（基于类别）
const getItemSchemeColor = (category: CheckRecordCategoryStatsDTO, itemIdx: number): string => {
  const categoryName = category.categoryName

  // 优先使用 multiWeightConfigs 中的颜色（直接从 classStats 获取）
  if (activeClass.value?.multiConfigEnabled && activeClass.value?.multiWeightConfigs?.length) {
    const configs = activeClass.value.multiWeightConfigs

    // 查找该分类对应的加权配置
    const matchedConfig = configs.find(config =>
      config.applyCategoryNames?.includes(categoryName)
    )

    if (matchedConfig?.colorCode) {
      return matchedConfig.colorCode
    }

    // 如果没有匹配的分类，使用默认配置的颜色
    const defaultConfig = configs.find(config => config.isDefault)
    if (defaultConfig?.colorCode) {
      return defaultConfig.colorCode
    }
  }

  // 回退：使用旧的 weightTreeData 逻辑
  let schemeIndex = 0
  const treeCategory = weightTreeData.value.categories?.find((c: any) => c.categoryName === categoryName)
  if (treeCategory?.effectiveConfig) {
    const configId = treeCategory.effectiveConfig.configId || treeCategory.effectiveConfig.configName || 'default'
    schemeIndex = weightSchemes.value.findIndex(s => s.id === configId || s.name === configId)
    if (schemeIndex < 0) schemeIndex = 0
  }

  return schemeColors[schemeIndex % schemeColors.length]
}

// 计算属性
const totalDeduction = computed(() =>
  classList.value.reduce((sum, c) => sum + Number(c.totalScore || 0), 0)
)

const activeClass = computed(() =>
  classList.value.find(c => String(c.classId) === activeClassId.value)
)

// 获取所有扣分项（未筛选）
const allDeductions = computed(() => {
  if (!activeClass.value) return []
  const stats = getCategoryStats(activeClass.value)
  return stats.flatMap(cat => cat.deductions || [])
})

// 是否有任何扣分
const hasAnyDeductions = computed(() => allDeductions.value.length > 0)

// 可用的检查轮次
const availableRounds = computed(() => {
  const rounds = new Set<number>()
  allDeductions.value.forEach(d => {
    rounds.add(d.checkRound || 1)
  })
  return Array.from(rounds).sort((a, b) => a - b)
})

// 轮次 Tab 配置
const roundTabs = computed(() => {
  const tabs = [{ value: 0, label: '全部轮次' }]
  availableRounds.value.forEach(round => {
    tabs.push({ value: round, label: `第${round}轮` })
  })
  return tabs
})

// 获取位置筛选后的扣分数量
const getLocationCount = (locationType: 'all' | 'dormitory' | 'classroom') => {
  if (locationType === 'all') return allDeductions.value.length
  if (locationType === 'dormitory') return allDeductions.value.filter(d => d.linkType === 1).length
  if (locationType === 'classroom') return allDeductions.value.filter(d => d.linkType === 2).length
  return 0
}

// 获取轮次筛选后的扣分数量
const getRoundCount = (round: number) => {
  if (round === 0) return allDeductions.value.length
  return allDeductions.value.filter(d => (d.checkRound || 1) === round).length
}

// 筛选后的扣分项总数
const filteredDeductionsCount = computed(() => {
  return filteredCategoryStats.value.reduce((sum, cat) => sum + (cat.deductions?.length || 0), 0)
})

// 筛选后的类别统计
const filteredCategoryStats = computed(() => {
  if (!activeClass.value) return []

  const stats = getCategoryStats(activeClass.value)

  // 对每个类别的扣分项进行筛选
  return stats.map(cat => {
    let filteredDeductions = cat.deductions || []

    // 位置筛选
    if (activeLocationTab.value === 'dormitory') {
      filteredDeductions = filteredDeductions.filter(d => d.linkType === 1)
    } else if (activeLocationTab.value === 'classroom') {
      filteredDeductions = filteredDeductions.filter(d => d.linkType === 2)
    }

    // 轮次筛选
    if (activeRoundTab.value > 0) {
      filteredDeductions = filteredDeductions.filter(d => (d.checkRound || 1) === activeRoundTab.value)
    }

    return {
      ...cat,
      deductions: filteredDeductions,
      totalScore: filteredDeductions.reduce((sum, d) => sum + Number(d.actualScore || 0), 0)
    }
  }).filter(cat => cat.deductions.length > 0)
})

// 工具方法
const formatDate = (d: string) => d ? new Date(d).toLocaleDateString('zh-CN') : '-'

const formatScore = (s: number | null | undefined) => {
  if (s === null || s === undefined) return '0'
  const n = Number(s)
  return n % 1 === 0 ? String(n) : n.toFixed(1)
}

// 计算扣分项的加权后分数（根据分类对应的加权系数）
const calculateWeightedItemScore = (originalScore: number | null | undefined, categoryName?: string): number => {
  if (!originalScore) return 0

  // 多配置模式：根据分类查找对应的加权系数
  if (activeClass.value?.multiConfigEnabled && activeClass.value?.multiWeightConfigs?.length && categoryName) {
    const configs = activeClass.value.multiWeightConfigs

    // 查找该分类对应的加权配置
    const matchedConfig = configs.find(config =>
      config.applyCategoryNames?.includes(categoryName)
    )

    if (matchedConfig?.weightFactor) {
      return Number(originalScore) * Number(matchedConfig.weightFactor)
    }

    // 如果没有匹配的分类，使用默认配置
    const defaultConfig = configs.find(config => config.isDefault)
    if (defaultConfig?.weightFactor) {
      return Number(originalScore) * Number(defaultConfig.weightFactor)
    }
  }

  // 单配置模式：使用全局加权系数
  if (!activeClass.value?.weightFactor) return Number(originalScore) || 0
  return Number(originalScore) * Number(activeClass.value.weightFactor)
}

const goBack = () => router.back()

// 获取类别统计（支持categoryStats或从deductions中提取）
const getCategoryStats = (cls: ClassStats) => {
  // 如果有categoryStats直接使用
  if (cls.categoryStats?.length) {
    return cls.categoryStats
  }

  // 否则从deductions中按categoryName分组
  if (cls.deductions?.length) {
    const categoryMap = new Map<string, { categoryId: string; categoryName: string; totalScore: number; deductions: DeductionDetail[] }>()

    cls.deductions.forEach(d => {
      const catName = d.categoryName || '其他'
      if (!categoryMap.has(catName)) {
        categoryMap.set(catName, {
          categoryId: catName,
          categoryName: catName,
          totalScore: 0,
          deductions: []
        })
      }
      const cat = categoryMap.get(catName)!
      cat.totalScore += Number(d.actualScore || 0)
      cat.deductions.push(d)
    })

    return Array.from(categoryMap.values())
  }

  return []
}

// 获取扣分项总数
const getDeductionCount = (cls: ClassStats) => {
  const stats = getCategoryStats(cls)
  return stats.reduce((sum, cat) => sum + (cat.deductions?.length || 0), 0)
}

// 切换类别展开/收起
const toggleCategory = (catId: string | number) => {
  const idx = expandedCategories.value.indexOf(catId)
  if (idx === -1) {
    expandedCategories.value.push(catId)
  } else {
    expandedCategories.value.splice(idx, 1)
  }
}

// 获取类别图标
const getCategoryIcon = (name: string) => {
  const lower = name?.toLowerCase() || ''
  if (lower.includes('卫生') || lower.includes('清洁')) return Sparkles
  if (lower.includes('纪律') || lower.includes('行为')) return ClipboardCheck
  if (lower.includes('考勤') || lower.includes('出勤')) return BookOpen
  if (lower.includes('宿舍') || lower.includes('寝室')) return Moon
  return ClipboardCheck
}

// 动态类别列表（从排名数据中提取所有唯一的类别）
const dynamicCategories = computed(() => {
  const categoryMap = new Map<string, { categoryId?: string; categoryName: string }>()

  rankingList.value.forEach(item => {
    if (item.categoryStats?.length) {
      item.categoryStats.forEach(cat => {
        const key = cat.categoryId || cat.categoryName
        if (!categoryMap.has(key)) {
          categoryMap.set(key, {
            categoryId: cat.categoryId,
            categoryName: cat.categoryName
          })
        }
      })
    }
  })

  return Array.from(categoryMap.values())
})

// 获取类别颜色样式（根据索引循环使用颜色）
const getCategoryColorClass = (index: number) => {
  const colors = [
    'text-rose-600',
    'text-amber-600',
    'text-blue-600',
    'text-purple-600',
    'text-emerald-600',
    'text-cyan-600',
    'text-pink-600',
    'text-indigo-600'
  ]
  return colors[index % colors.length]
}

// 获取某个班级某个类别的分数（原始或加权）
const getCategoryScore = (item: ClassStats, categoryName: string, weighted: boolean): number | null => {
  if (!item.categoryStats?.length) return null

  const catStat = item.categoryStats.find(c => c.categoryName === categoryName)
  if (!catStat) return null

  if (weighted && catStat.weightedTotalScore !== undefined && catStat.weightedTotalScore !== null) {
    return catStat.weightedTotalScore
  }
  return catStat.totalScore
}

// 获取排名列表中某个班级某个类别的加权分数
const getRankingWeightedCategoryScore = (item: ClassStats, categoryName: string): number => {
  // 1. 先尝试从 categoryStats 获取加权分数
  if (item.categoryStats?.length) {
    const catStat = item.categoryStats.find(c => c.categoryName === categoryName)
    if (catStat?.weightedTotalScore !== undefined && catStat?.weightedTotalScore !== null) {
      return catStat.weightedTotalScore
    }
  }

  // 2. 如果没有加权分数，用原始分数乘以权值计算
  const originalScore = getCategoryScore(item, categoryName, false) || 0

  // 多配置模式：查找对应类别的配置
  if (item.multiConfigEnabled && item.multiWeightConfigs?.length) {
    const matchedConfig = item.multiWeightConfigs.find(config =>
      config.applyCategoryNames?.includes(categoryName)
    )
    if (matchedConfig?.weightFactor) {
      return originalScore * matchedConfig.weightFactor
    }
  }

  // 单配置模式：使用全局权值
  if (item.weightFactor) {
    return originalScore * item.weightFactor
  }

  return originalScore
}

// 查看照片
const viewPhotos = (urls: string[], startIdx = 0) => {
  currentPhotos.value = urls
  currentPhotoIndex.value = startIdx
  photoVisible.value = true
}

// 显示学生列表
const showStudentList = (item: DeductionDetail) => {
  currentDeductionItem.value = item
  currentStudentList.value = item.studentNameList || []
  studentDialogVisible.value = true
}

// 显示备注
const showRemark = (item: DeductionDetail) => {
  currentDeductionItem.value = item
  currentRemark.value = item.remark || ''
  remarkDialogVisible.value = true
}

// 打开申诉弹窗
const openAppealDialog = (item: DeductionDetail) => {
  appealDeductionItem.value = item
  appealForm.value = {
    appealType: 1,
    reason: '',
    expectedScore: null,
    contactPhone: ''
  }
  appealDialogVisible.value = true
}

// 提交申诉
const submitAppeal = async () => {
  if (!appealForm.value.reason.trim()) {
    ElMessage.warning('请填写申诉理由')
    return
  }
  if (!appealDeductionItem.value || !activeClass.value) {
    ElMessage.error('数据异常，请刷新页面重试')
    return
  }

  appealSubmitting.value = true
  try {
    await createAppeal({
      recordId: String(route.params.id),
      itemId: String(appealDeductionItem.value.id),
      reason: appealForm.value.reason,
      expectedScore: appealForm.value.expectedScore || undefined,
      appealType: appealForm.value.appealType,
      contactPhone: appealForm.value.contactPhone || undefined
    })
    ElMessage.success('申诉提交成功，请等待审核')
    appealDialogVisible.value = false
    // 刷新数据
    loadData()
  } catch (error: any) {
    ElMessage.error(error.message || '申诉提交失败')
  } finally {
    appealSubmitting.value = false
  }
}

// 切换班级时重置筛选和展开状态
watch(activeClassId, () => {
  activeLocationTab.value = 'all'
  activeRoundTab.value = 0
  // 重置宿舍Tab状态
  categoryDormitoryTabs.value = {}
  // 默认展开所有类别
  if (activeClass.value) {
    const cats = getCategoryStats(activeClass.value)
    expandedCategories.value = cats.map(c => c.categoryId || c.categoryName)
  }
})

// 加载数据
const loadData = async () => {
  loading.value = true
  try {
    const res = await getCheckRecordDetail(route.params.id as string)
    // http.ts已解包响应数据,res就是实际数据
    const data = res || {}
    recordInfo.value = data
    classList.value = data.classStats || []

    if (classList.value.length > 0) {
      activeClassId.value = String(classList.value[0].classId)
      // 默认展开所有类别
      const firstClass = classList.value[0]
      const cats = getCategoryStats(firstClass)
      expandedCategories.value = cats.map(c => c.categoryId || c.categoryName)
    }

    // 加载加权配置树（用于颜色标记）
    loadWeightConfigTree()

    // 加载评级规则（如果有planId）
    if (data.planId) {
      loadRatingRules(data.planId)
    }
  } catch (e) {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

// 加载评级规则
const loadRatingRules = async (planId: string | number) => {
  try {
    const res = await getRatingConfigsByPlan(Number(planId))
    ratingRules.value = (res || []).filter(r => r.ratingType === 'DAILY' && r.enabled === 1)
  } catch (e) {
    console.error('加载评级规则失败', e)
    ratingRules.value = []
  }
}

// 加载排名数据
const loadRankingData = async () => {
  rankingLoading.value = true
  try {
    const res = await getClassRanking(route.params.id as string, {
      sortBy: rankingSortBy.value
    })
    // http.ts已解包响应数据
    rankingList.value = res || []
  } catch (e) {
    console.error('加载排名数据失败', e)
    ElMessage.error('加载排名数据失败')
  } finally {
    rankingLoading.value = false
  }
}

// 切换到排名视图
const switchToRankingView = () => {
  activeView.value = 'ranking'
  if (rankingList.value.length === 0) {
    loadRankingData()
  }
}

// 监听排序方式变化，重新加载排名数据
watch(rankingSortBy, () => {
  if (activeView.value === 'ranking') {
    loadRankingData()
  }
})

// 监听筛选条件变化，自动选择第一个班级
watch([filterDepartment, filterGrade], () => {
  if (filteredClassList.value.length > 0) {
    // 检查当前选中的班级是否在筛选结果中
    const currentClassInList = filteredClassList.value.some(
      cls => String(cls.classId) === activeClassId.value
    )
    // 如果不在，自动选择第一个
    if (!currentClassInList) {
      activeClassId.value = String(filteredClassList.value[0].classId)
    }
  }
})

// 获取分数显示样式
const getScoreClass = (score: number | null | undefined) => {
  if (!score || score === 0) return 'text-sm text-emerald-600 font-medium'
  if (score < 2) return 'text-sm text-amber-600 font-medium'
  return 'text-sm text-rose-600 font-medium'
}

// 格式化分数显示（带负号）
const formatScoreDisplay = (score: number | null | undefined) => {
  if (!score || score === 0) return '0'
  const n = Number(score)
  const formatted = n % 1 === 0 ? String(n) : n.toFixed(1)
  return `-${formatted}`
}

// 格式化加权系数
const formatWeightFactor = (factor: number | null | undefined) => {
  if (!factor) return '1.00'
  return Number(factor).toFixed(2)
}

onMounted(loadData)
</script>

<style scoped>
/* 滚动条美化 */
aside::-webkit-scrollbar,
main::-webkit-scrollbar {
  width: 6px;
}

aside::-webkit-scrollbar-track,
main::-webkit-scrollbar-track {
  background: transparent;
}

aside::-webkit-scrollbar-thumb,
main::-webkit-scrollbar-thumb {
  background: #e2e8f0;
  border-radius: 3px;
}

aside::-webkit-scrollbar-thumb:hover,
main::-webkit-scrollbar-thumb:hover {
  background: #cbd5e1;
}

/* 扣分卡片统一高度 */
.deduction-card {
  display: flex;
  flex-direction: column;
  min-height: 100px;
}

.deduction-card > div:last-child {
  flex: 1;
}
</style>
