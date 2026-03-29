<template>
  <div class="flex h-full flex-col bg-gray-50">
    <!-- Top Header Bar -->
    <div class="flex items-center justify-between border-b border-gray-200 bg-white px-6 py-4">
      <div>
        <h1 class="text-lg font-semibold text-gray-900">排课中心</h1>
        <p class="mt-0.5 text-sm text-gray-500">排课管理、课表查看、冲突检测与调课</p>
      </div>
      <div class="flex items-center gap-3">
        <el-select
          v-model="semesterId"
          placeholder="选择学期"
          class="w-48"
          @change="onSemesterChange"
        >
          <el-option v-for="s in semesters" :key="s.id" :value="s.id" :label="s.name" />
        </el-select>
      </div>
    </div>

    <!-- Tabs -->
    <div class="border-b border-gray-200 bg-white px-6">
      <nav class="-mb-px flex gap-6">
        <button
          v-for="tab in tabs"
          :key="tab.key"
          class="border-b-2 px-1 py-3 text-sm font-medium transition-colors"
          :class="
            activeTab === tab.key
              ? 'border-blue-500 text-blue-600'
              : 'border-transparent text-gray-500 hover:border-gray-300 hover:text-gray-700'
          "
          @click="activeTab = tab.key"
        >
          {{ tab.label }}
        </button>
      </nav>
    </div>

    <!-- Tab Content -->
    <div class="flex-1 overflow-y-auto px-6 pt-5 pb-6">
      <!-- Loading -->
      <div v-if="globalLoading" class="flex items-center justify-center py-20">
        <div class="h-8 w-8 animate-spin rounded-full border-2 border-blue-600 border-t-transparent" />
      </div>

      <template v-else>
        <!-- ==================== Tab 1: Overview ==================== -->
        <div v-if="activeTab === 'overview'">
          <!-- Progress bar -->
          <div class="mb-5 rounded-xl border border-gray-200 bg-white p-5">
            <div class="mb-3 flex items-center justify-between">
              <span class="text-sm font-semibold text-gray-700">排课进度</span>
              <span class="text-sm font-semibold text-blue-600">{{ overviewProgress }}%</span>
            </div>
            <div class="mb-4 h-2.5 w-full overflow-hidden rounded-full bg-gray-100">
              <div
                class="h-full rounded-full bg-blue-500 transition-all duration-500"
                :style="{ width: overviewProgress + '%' }"
              />
            </div>

            <!-- Stat cards -->
            <div class="grid grid-cols-4 gap-4">
              <div class="rounded-lg border border-gray-100 bg-gray-50 px-4 py-3 text-center">
                <div class="text-xl font-bold text-gray-900">{{ taskStats.total }}</div>
                <div class="mt-0.5 text-xs text-gray-500">教学任务</div>
              </div>
              <div class="rounded-lg border border-emerald-100 bg-emerald-50 px-4 py-3 text-center">
                <div class="text-xl font-bold text-emerald-600">{{ taskStats.scheduled }}</div>
                <div class="mt-0.5 text-xs text-gray-500">已排完</div>
              </div>
              <div class="rounded-lg border border-amber-100 bg-amber-50 px-4 py-3 text-center">
                <div class="text-xl font-bold text-amber-600">{{ taskStats.partial }}</div>
                <div class="mt-0.5 text-xs text-gray-500">部分排</div>
              </div>
              <div class="rounded-lg border border-rose-100 bg-rose-50 px-4 py-3 text-center">
                <div class="text-xl font-bold text-rose-600">{{ taskStats.unscheduled }}</div>
                <div class="mt-0.5 text-xs text-gray-500">未排课</div>
              </div>
            </div>
          </div>

          <!-- Action buttons -->
          <div class="mb-5 flex items-center gap-3">
            <el-button type="primary" @click="handleAutoSchedule">
              <Play class="mr-1 h-4 w-4" /> 智能排课
            </el-button>
            <el-button @click="handleFeasibilityCheck">
              <ClipboardCheck class="mr-1 h-4 w-4" /> 可行性检测
            </el-button>
            <el-button @click="handlePublish">
              <Megaphone class="mr-1 h-4 w-4" /> 发布课表
            </el-button>
          </div>

          <!-- Teaching task table -->
          <div class="rounded-xl border border-gray-200 bg-white">
            <div class="flex items-center justify-between px-5 py-3">
              <h2 class="text-sm font-semibold text-gray-700">教学任务列表</h2>
              <div class="flex items-center gap-2">
                <el-input
                  v-model="taskFilter.keyword"
                  placeholder="搜索课程/教师"
                  class="w-48"
                  size="small"
                  clearable
                  @clear="loadTasks"
                  @keyup.enter="loadTasks"
                >
                  <template #prefix><Search class="h-3.5 w-3.5 text-gray-400" /></template>
                </el-input>
                <el-select v-model="taskFilter.status" placeholder="状态" class="w-28" size="small" clearable @change="loadTasks">
                  <el-option :value="0" label="待分配" />
                  <el-option :value="1" label="已分配" />
                  <el-option :value="2" label="已排课" />
                  <el-option :value="3" label="进行中" />
                </el-select>
              </div>
            </div>
            <div class="border-t border-gray-100">
              <el-table :data="tasks" v-loading="taskLoading" stripe>
                <el-table-column prop="className" label="教学班" min-width="120" />
                <el-table-column prop="courseName" label="课程" min-width="140" />
                <el-table-column label="教师" min-width="100">
                  <template #default="{ row }">
                    {{ getMainTeacher(row) || '-' }}
                  </template>
                </el-table-column>
                <el-table-column prop="weeklyHours" label="周课时" width="80" align="center" />
                <el-table-column label="已排/总需" width="100" align="center">
                  <template #default="{ row }">
                    {{ getScheduledHours(row) }}/{{ row.weeklyHours }}
                  </template>
                </el-table-column>
                <el-table-column label="状态" width="90" align="center">
                  <template #default="{ row }">
                    <el-tag size="small" :type="getTaskStatusTag(row.status)">{{ getTaskStatusName(row.status) }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="140" fixed="right">
                  <template #default="{ row }">
                    <el-button size="small" text @click="viewTaskTimetable(row)">查看</el-button>
                    <el-button size="small" text type="primary" @click="showManualEntryDialog(row)">手动排</el-button>
                  </template>
                </el-table-column>
              </el-table>
              <div class="flex justify-end px-4 py-3">
                <el-pagination
                  v-model:current-page="taskPagination.page"
                  v-model:page-size="taskPagination.size"
                  :total="taskPagination.total"
                  :page-sizes="[20, 50, 100]"
                  layout="total, sizes, prev, pager, next"
                  small
                  @size-change="loadTasks"
                  @current-change="loadTasks"
                />
              </div>
            </div>
          </div>
        </div>

        <!-- ==================== Tab 2: Timetable View ==================== -->
        <div v-if="activeTab === 'timetable'">
          <div class="mb-4 rounded-xl border border-gray-200 bg-white p-4">
            <div class="flex flex-wrap items-center gap-3">
              <!-- View type radio -->
              <el-radio-group v-model="timetableViewType" size="small" @change="onTimetableTypeChange">
                <el-radio-button value="class">班级课表</el-radio-button>
                <el-radio-button value="teacher">教师课表</el-radio-button>
                <el-radio-button value="classroom">教室课表</el-radio-button>
              </el-radio-group>

              <!-- Target selector -->
              <el-select
                v-model="timetableTargetId"
                :placeholder="timetableTargetPlaceholder"
                class="w-48"
                size="small"
                filterable
                clearable
                @change="loadTimetable"
              >
                <el-option
                  v-for="opt in timetableOptions"
                  :key="opt.id"
                  :value="opt.id"
                  :label="opt.name"
                />
              </el-select>

              <div class="h-5 w-px bg-gray-200" />

              <!-- Week selector -->
              <el-select v-model="timetableWeek" placeholder="周次" class="w-28" size="small" clearable>
                <el-option v-for="w in 20" :key="w" :value="w" :label="'第' + w + '周'" />
              </el-select>

              <!-- Odd/even week filter -->
              <el-radio-group v-model="timetableWeekType" size="small">
                <el-radio-button :value="0">全部</el-radio-button>
                <el-radio-button :value="1">仅单周</el-radio-button>
                <el-radio-button :value="2">仅双周</el-radio-button>
              </el-radio-group>
            </div>
          </div>

          <!-- Timetable grid -->
          <div class="rounded-xl border border-gray-200 bg-white">
            <TimetableGrid
              :entries="filteredTimetableEntries"
              :periods="periods"
              @entry-click="showEntryDetail"
            />
          </div>

          <!-- Bottom stats -->
          <div v-if="timetableTargetId" class="mt-3 flex items-center gap-4 text-xs text-gray-500">
            <span>本周 <span class="font-semibold text-gray-700">{{ timetableWeeklyCount }}</span> 节课</span>
            <div class="h-3 w-px bg-gray-200" />
            <span><span class="font-semibold text-gray-700">{{ timetableDayCount }}</span> 天有课</span>
          </div>
        </div>

        <!-- ==================== Tab 3: Constraints ==================== -->
        <div v-if="activeTab === 'constraints'">
          <div class="rounded-xl border border-gray-200 bg-white p-8 text-center">
            <Settings class="mx-auto mb-3 h-10 w-10 text-gray-300" />
            <p class="mb-1 text-sm font-medium text-gray-600">约束配置</p>
            <p class="mb-4 text-xs text-gray-400">约束规则管理已移至独立页面，点击下方按钮前往配置。</p>
            <router-link
              v-if="constraintRouteExists"
              :to="{ name: 'ConstraintConfig' }"
              class="inline-flex items-center gap-1.5 rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-blue-700"
            >
              <ExternalLink class="h-4 w-4" />
              前往约束配置
            </router-link>
            <div v-else class="text-xs text-gray-400">约束配置页面尚未创建，敬请期待。</div>
          </div>
        </div>

        <!-- ==================== Tab 4: Conflict Center ==================== -->
        <div v-if="activeTab === 'conflicts'">
          <!-- Action buttons -->
          <div class="mb-4 flex items-center gap-3">
            <el-button type="primary" :loading="feasibilityLoading" @click="runFeasibilityCheck">
              <ShieldCheck class="mr-1 h-4 w-4" /> 运行可行性检测
            </el-button>
            <el-button :loading="conflictDetecting" @click="runConflictDetection">
              <SearchCheck class="mr-1 h-4 w-4" /> 检测排课冲突
            </el-button>
          </div>

          <!-- Feasibility report -->
          <div v-if="feasibilityReport" class="mb-5 rounded-xl border border-gray-200 bg-white p-5">
            <h3 class="mb-3 text-sm font-semibold text-gray-700">可行性报告</h3>
            <div class="mb-4 flex items-center gap-6 text-sm">
              <span class="flex items-center gap-1.5">
                <CheckCircle2 class="h-4 w-4 text-emerald-500" />
                通过 <span class="font-semibold text-emerald-600">{{ feasibilityReport.passedChecks }}</span> 项
              </span>
              <span class="flex items-center gap-1.5">
                <XCircle class="h-4 w-4 text-rose-500" />
                阻塞 <span class="font-semibold text-rose-600">{{ feasibilityReport.blockingIssues.length }}</span> 个
              </span>
              <span class="flex items-center gap-1.5">
                <AlertTriangle class="h-4 w-4 text-amber-500" />
                警告 <span class="font-semibold text-amber-600">{{ feasibilityReport.warnings.length }}</span> 个
              </span>
            </div>

            <!-- Blocking issues -->
            <div v-if="feasibilityReport.blockingIssues.length > 0" class="mb-3">
              <div class="mb-1.5 text-xs font-medium text-rose-600">阻塞项</div>
              <div
                v-for="(issue, idx) in feasibilityReport.blockingIssues"
                :key="'block-' + idx"
                class="mb-1.5 rounded-lg border border-rose-100 bg-rose-50 px-3 py-2 text-xs text-rose-700"
              >
                <span class="font-medium">[{{ issue.type }}]</span> {{ issue.target }}: {{ issue.description }}
                <span v-if="issue.suggestion" class="ml-2 text-rose-500">-- {{ issue.suggestion }}</span>
              </div>
            </div>

            <!-- Warnings -->
            <div v-if="feasibilityReport.warnings.length > 0">
              <div class="mb-1.5 text-xs font-medium text-amber-600">警告项</div>
              <div
                v-for="(issue, idx) in feasibilityReport.warnings"
                :key="'warn-' + idx"
                class="mb-1.5 rounded-lg border border-amber-100 bg-amber-50 px-3 py-2 text-xs text-amber-700"
              >
                <span class="font-medium">[{{ issue.type }}]</span> {{ issue.target }}: {{ issue.description }}
              </div>
            </div>
          </div>

          <!-- Conflict list -->
          <div class="rounded-xl border border-gray-200 bg-white">
            <div class="flex items-center justify-between px-5 py-3">
              <h2 class="text-sm font-semibold text-gray-700">冲突列表</h2>
              <el-select v-model="conflictStatusFilter" placeholder="状态" class="w-28" size="small" clearable @change="loadConflicts">
                <el-option :value="0" label="未处理" />
                <el-option :value="1" label="已解决" />
                <el-option :value="2" label="已忽略" />
              </el-select>
            </div>
            <div class="border-t border-gray-100">
              <el-table :data="conflicts" v-loading="conflictsLoading" stripe>
                <el-table-column label="类型" width="100" align="center">
                  <template #default="{ row }">
                    <el-tag size="small" :type="getConflictCategoryTag(row.conflictCategory)">
                      {{ getConflictCategoryName(row.conflictCategory) }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="严重度" width="90" align="center">
                  <template #default="{ row }">
                    <el-tag size="small" :type="getSeverityTag(row.severity)">
                      {{ getSeverityName(row.severity) }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="conflictType" label="冲突类别" width="120" />
                <el-table-column prop="description" label="描述" min-width="240" show-overflow-tooltip />
                <el-table-column label="状态" width="90" align="center">
                  <template #default="{ row }">
                    <el-tag size="small" :type="getResolutionStatusTag(row.resolutionStatus)">
                      {{ getResolutionStatusName(row.resolutionStatus) }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="140" fixed="right">
                  <template #default="{ row }">
                    <template v-if="row.resolutionStatus === 0">
                      <el-button size="small" text type="success" @click="resolveConflict(row)">解决</el-button>
                      <el-button size="small" text type="warning" @click="ignoreConflict(row)">忽略</el-button>
                    </template>
                    <span v-else class="text-xs text-gray-400">{{ row.resolutionNote || '-' }}</span>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </div>
        </div>

        <!-- ==================== Tab 5: Adjustments ==================== -->
        <div v-if="activeTab === 'adjustments'">
          <!-- Action bar -->
          <div class="mb-4 flex items-center gap-3">
            <el-button type="primary" @click="showAdjustmentDialog()">
              <Plus class="mr-1 h-4 w-4" /> 申请调课
            </el-button>
            <el-radio-group v-model="adjustmentView" size="small">
              <el-radio-button value="my">我的申请</el-radio-button>
              <el-radio-button value="pending">待审批</el-radio-button>
              <el-radio-button value="all">全部记录</el-radio-button>
            </el-radio-group>
          </div>

          <!-- Adjustment table -->
          <div class="rounded-xl border border-gray-200 bg-white">
            <div class="border-t border-gray-100">
              <el-table :data="adjustments" v-loading="adjustmentLoading" stripe>
                <el-table-column label="类型" width="80" align="center">
                  <template #default="{ row }">
                    <el-tag size="small" :type="getAdjTypeTag(row.adjustmentType)">
                      {{ getAdjTypeName(row.adjustmentType) }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="applicantName" label="申请人" width="100" />
                <el-table-column prop="reason" label="原因" min-width="200" show-overflow-tooltip />
                <el-table-column label="新时间" width="160">
                  <template #default="{ row }">
                    <template v-if="row.newDayOfWeek">
                      {{ getWeekdayName(row.newDayOfWeek) }}
                      第{{ row.newPeriodStart }}-{{ row.newPeriodEnd }}节
                    </template>
                    <span v-else class="text-gray-400">-</span>
                  </template>
                </el-table-column>
                <el-table-column prop="newClassroomName" label="新教室" width="100" />
                <el-table-column label="状态" width="90" align="center">
                  <template #default="{ row }">
                    <el-tag size="small" :type="getAdjStatusTag(row.status)">
                      {{ getAdjStatusName(row.status) }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="appliedAt" label="申请时间" width="160" />
                <el-table-column label="操作" width="180" fixed="right">
                  <template #default="{ row }">
                    <template v-if="row.status === 0 && adjustmentView === 'pending'">
                      <el-button size="small" text type="success" @click="approveAdjustment(row)">批准</el-button>
                      <el-button size="small" text type="danger" @click="rejectAdjustment(row)">驳回</el-button>
                    </template>
                    <template v-else-if="row.status === 1">
                      <el-button size="small" text type="primary" @click="executeAdjustment(row)">执行</el-button>
                    </template>
                    <template v-else-if="row.status === 0 && adjustmentView === 'my'">
                      <el-button size="small" text type="danger" @click="cancelAdjustment(row)">撤回</el-button>
                    </template>
                    <span v-else class="text-xs text-gray-400">-</span>
                  </template>
                </el-table-column>
              </el-table>
              <div class="flex justify-end px-4 py-3">
                <el-pagination
                  v-model:current-page="adjustmentPagination.page"
                  v-model:page-size="adjustmentPagination.size"
                  :total="adjustmentPagination.total"
                  :page-sizes="[20, 50]"
                  layout="total, prev, pager, next"
                  small
                  @current-change="loadAdjustments"
                />
              </div>
            </div>
          </div>
        </div>

        <!-- ==================== Tab 6: Export ==================== -->
        <div v-if="activeTab === 'export'">
          <div class="rounded-xl border border-gray-200 bg-white p-8 text-center">
            <Printer class="mx-auto mb-3 h-10 w-10 text-gray-300" />
            <p class="mb-1 text-sm font-medium text-gray-600">导出与打印</p>
            <p class="mb-4 text-xs text-gray-400">选择课表维度后可导出 Excel 或生成打印版课表。</p>
            <div class="mx-auto flex max-w-md items-center gap-3">
              <el-select v-model="exportType" placeholder="导出维度" class="flex-1" size="default">
                <el-option value="class" label="按班级导出" />
                <el-option value="teacher" label="按教师导出" />
                <el-option value="classroom" label="按教室导出" />
                <el-option value="all" label="全校总课表" />
              </el-select>
              <el-button type="primary" @click="handleExport">
                <Download class="mr-1 h-4 w-4" /> 导出
              </el-button>
              <el-button @click="handlePrint">
                <Printer class="mr-1 h-4 w-4" /> 打印预览
              </el-button>
            </div>
          </div>
        </div>
      </template>
    </div>

    <!-- ==================== Dialogs ==================== -->

    <!-- Auto Schedule Dialog -->
    <el-dialog v-model="autoScheduleDialogVisible" title="智能排课" width="500px">
      <div class="mb-4 rounded-lg bg-blue-50 p-3 text-xs text-blue-700">
        智能排课将根据教学任务和约束条件自动生成最优课表，过程可能需要几分钟。
      </div>
      <el-form :model="autoScheduleParams" label-width="120px">
        <el-form-item label="排课方案">
          <el-select v-model="autoScheduleParams.scheduleId" style="width: 100%">
            <el-option v-for="s in scheduleList" :key="s.id" :value="s.id" :label="s.name" />
          </el-select>
        </el-form-item>
        <el-form-item label="最大迭代次数">
          <el-input-number v-model="autoScheduleParams.maxIterations" :min="100" :max="5000" :step="100" />
        </el-form-item>
        <el-form-item label="种群大小">
          <el-input-number v-model="autoScheduleParams.populationSize" :min="20" :max="500" :step="10" />
        </el-form-item>
        <el-form-item label="变异率">
          <el-slider v-model="autoScheduleParams.mutationRate" :min="0.01" :max="0.5" :step="0.01" show-input />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="autoScheduleDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="scheduling" @click="runAutoSchedule">开始排课</el-button>
      </template>
    </el-dialog>

    <!-- Manual Entry Dialog -->
    <el-dialog
      v-model="entryDialogVisible"
      :title="entryForm.id ? '编辑排课' : '手动排课'"
      width="600px"
    >
      <el-form ref="entryFormRef" :model="entryForm" :rules="entryRules" label-width="100px">
        <el-form-item label="教学任务" prop="taskId">
          <el-select v-model="entryForm.taskId" placeholder="选择教学任务" style="width: 100%" filterable disabled>
            <el-option
              v-for="t in taskOptionsForEntry"
              :key="t.id"
              :value="t.id"
              :label="`${t.courseName} - ${t.className}`"
            />
          </el-select>
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="星期" prop="dayOfWeek">
              <el-select v-model="entryForm.dayOfWeek" style="width: 100%">
                <el-option v-for="d in weekdays" :key="d.value" :value="d.value" :label="d.label" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="教室" prop="classroomId">
              <el-select v-model="entryForm.classroomId" placeholder="选择教室" style="width: 100%" filterable>
                <el-option v-for="c in classrooms" :key="c.id" :value="c.id" :label="c.name" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="开始节次" prop="periodStart">
              <el-select v-model="entryForm.periodStart" style="width: 100%">
                <el-option v-for="p in periods" :key="p.period" :value="p.period" :label="p.name" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="结束节次" prop="periodEnd">
              <el-select v-model="entryForm.periodEnd" style="width: 100%">
                <el-option v-for="p in periods" :key="p.period" :value="p.period" :label="p.name" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="开始周" prop="weekStart">
              <el-input-number v-model="entryForm.weekStart" :min="1" :max="20" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="结束周" prop="weekEnd">
              <el-input-number v-model="entryForm.weekEnd" :min="1" :max="20" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="周类型">
              <el-select v-model="entryForm.weekType" style="width: 100%">
                <el-option :value="0" label="每周" />
                <el-option :value="1" label="单周" />
                <el-option :value="2" label="双周" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="entryDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="entrySaving" @click="saveEntry">保存</el-button>
      </template>
    </el-dialog>

    <!-- Adjustment Apply Dialog -->
    <el-dialog v-model="adjustmentDialogVisible" title="申请调课" width="560px">
      <el-form ref="adjFormRef" :model="adjForm" :rules="adjRules" label-width="100px">
        <el-form-item label="调课类型" prop="adjustmentType">
          <el-radio-group v-model="adjForm.adjustmentType">
            <el-radio :value="1">调课</el-radio>
            <el-radio :value="2">停课</el-radio>
            <el-radio :value="3">补课</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="原课程" prop="entryId">
          <el-select v-model="adjForm.entryId" placeholder="选择要调整的课程" style="width: 100%" filterable>
            <el-option
              v-for="entry in adjEntryOptions"
              :key="entry.id"
              :value="entry.id"
              :label="`${entry.courseName} - ${getWeekdayName(entry.dayOfWeek)} 第${entry.periodStart}-${entry.periodEnd}节`"
            />
          </el-select>
        </el-form-item>
        <template v-if="adjForm.adjustmentType !== 2">
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="新星期">
                <el-select v-model="adjForm.newDayOfWeek" style="width: 100%" clearable>
                  <el-option v-for="d in weekdays" :key="d.value" :value="d.value" :label="d.label" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="新教室">
                <el-select v-model="adjForm.newClassroomId" placeholder="选择教室" style="width: 100%" filterable clearable>
                  <el-option v-for="c in classrooms" :key="c.id" :value="c.id" :label="c.name" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="8">
              <el-form-item label="新开始节">
                <el-select v-model="adjForm.newPeriodStart" style="width: 100%" clearable>
                  <el-option v-for="p in periods" :key="p.period" :value="p.period" :label="p.name" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="新结束节">
                <el-select v-model="adjForm.newPeriodEnd" style="width: 100%" clearable>
                  <el-option v-for="p in periods" :key="p.period" :value="p.period" :label="p.name" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="新周次">
                <el-input-number v-model="adjForm.newWeek" :min="1" :max="20" style="width: 100%" />
              </el-form-item>
            </el-col>
          </el-row>
        </template>
        <el-form-item label="原因" prop="reason">
          <el-input v-model="adjForm.reason" type="textarea" rows="2" placeholder="请填写调课原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="adjustmentDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="adjSaving" @click="submitAdjustment">提交</el-button>
      </template>
    </el-dialog>

    <!-- Entry detail popover (simple dialog) -->
    <el-dialog v-model="entryDetailVisible" title="课程详情" width="400px">
      <div v-if="selectedEntry" class="space-y-2 text-sm">
        <div class="flex"><span class="w-20 text-gray-500">课程:</span><span class="font-medium">{{ selectedEntry.courseName }}</span></div>
        <div class="flex"><span class="w-20 text-gray-500">教师:</span><span>{{ selectedEntry.teacherName || '-' }}</span></div>
        <div class="flex"><span class="w-20 text-gray-500">班级:</span><span>{{ selectedEntry.className || '-' }}</span></div>
        <div class="flex"><span class="w-20 text-gray-500">教室:</span><span>{{ selectedEntry.classroomName || '-' }}</span></div>
        <div class="flex"><span class="w-20 text-gray-500">时间:</span><span>{{ getWeekdayName(selectedEntry.dayOfWeek) }} 第{{ selectedEntry.periodStart }}-{{ selectedEntry.periodEnd }}节</span></div>
        <div class="flex"><span class="w-20 text-gray-500">周次:</span><span>{{ formatWeeks(selectedEntry) }}</span></div>
      </div>
    </el-dialog>

    <!-- Resolve conflict dialog -->
    <el-dialog v-model="resolveDialogVisible" title="处理冲突" width="400px">
      <el-input v-model="resolveNote" type="textarea" rows="3" :placeholder="resolveAction === 'resolve' ? '请描述解决方案' : '请说明忽略原因'" />
      <template #footer>
        <el-button @click="resolveDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="resolveSaving" @click="confirmResolve">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  Search, Play, ClipboardCheck, Megaphone, Settings, ExternalLink,
  ShieldCheck, SearchCheck, CheckCircle2, XCircle, AlertTriangle,
  Plus, Download, Printer,
} from 'lucide-vue-next'
import request from '@/utils/request'
import {
  scheduleApi, semesterApi, teachingTaskApi, conflictApi, adjustmentApi,
} from '@/api/teaching'
import type {
  CourseSchedule, ScheduleEntry, Semester, TeachingTask,
  DetectedConflict, FeasibilityReport, ScheduleAdjustment,
} from '@/types/teaching'
import { WEEKDAYS, DEFAULT_PERIODS } from '@/types/teaching'
import TimetableGrid from './scheduling/TimetableGrid.vue'

// ==================== Constants ====================

const weekdays = WEEKDAYS
const periods = DEFAULT_PERIODS

const tabs = [
  { key: 'overview', label: '排课总览' },
  { key: 'timetable', label: '课表视图' },
  { key: 'constraints', label: '约束配置' },
  { key: 'conflicts', label: '冲突中心' },
  { key: 'adjustments', label: '调课管理' },
  { key: 'export', label: '导出打印' },
] as const

type TabKey = (typeof tabs)[number]['key']

// ==================== Core State ====================

const router = useRouter()
const globalLoading = ref(false)
const activeTab = ref<TabKey>('overview')
const semesterId = ref<number | string>()
const semesters = ref<Semester[]>([])
const scheduleList = ref<CourseSchedule[]>([])

// Options data
const classrooms = ref<{ id: number; name: string }[]>([])
const classList = ref<{ id: number; name: string }[]>([])
const teacherList = ref<{ id: number; name: string }[]>([])

// ==================== Tab 1: Overview ====================

const tasks = ref<TeachingTask[]>([])
const taskLoading = ref(false)
const taskFilter = reactive({ keyword: '', status: undefined as number | undefined })
const taskPagination = reactive({ page: 1, size: 20, total: 0 })

const taskStats = computed(() => {
  const total = taskPagination.total
  const scheduled = tasks.value.filter(t => t.status >= 2).length
  const partial = tasks.value.filter(t => t.status === 1).length
  const unscheduled = tasks.value.filter(t => t.status === 0).length
  return { total, scheduled, partial, unscheduled }
})

const overviewProgress = computed(() => {
  if (taskStats.value.total === 0) return 0
  return Math.round((taskStats.value.scheduled / taskStats.value.total) * 100)
})

// ==================== Tab 2: Timetable ====================

const timetableViewType = ref<'class' | 'teacher' | 'classroom'>('class')
const timetableTargetId = ref<number | string>()
const timetableEntries = ref<ScheduleEntry[]>([])
const timetableWeek = ref<number>()
const timetableWeekType = ref<number>(0)

const timetableTargetPlaceholder = computed(() => {
  const m: Record<string, string> = { class: '选择班级', teacher: '选择教师', classroom: '选择教室' }
  return m[timetableViewType.value]
})

const timetableOptions = computed(() => {
  if (timetableViewType.value === 'class') return classList.value
  if (timetableViewType.value === 'teacher') return teacherList.value
  return classrooms.value
})

const filteredTimetableEntries = computed(() => {
  let entries = timetableEntries.value
  // Filter by week
  if (timetableWeek.value) {
    const w = timetableWeek.value
    entries = entries.filter(e => e.weekStart <= w && e.weekEnd >= w)
    // Filter by odd/even
    if (timetableWeekType.value === 1) {
      entries = entries.filter(e => e.weekType !== 2) // not even-only
    } else if (timetableWeekType.value === 2) {
      entries = entries.filter(e => e.weekType !== 1) // not odd-only
    }
  } else if (timetableWeekType.value === 1) {
    entries = entries.filter(e => e.weekType !== 2)
  } else if (timetableWeekType.value === 2) {
    entries = entries.filter(e => e.weekType !== 1)
  }
  return entries
})

const timetableWeeklyCount = computed(() => filteredTimetableEntries.value.length)

const timetableDayCount = computed(() => {
  const days = new Set(filteredTimetableEntries.value.map(e => e.dayOfWeek))
  return days.size
})

// ==================== Tab 3: Constraints ====================

const constraintRouteExists = computed(() => {
  return router.getRoutes().some(r => r.name === 'ConstraintConfig')
})

// ==================== Tab 4: Conflicts ====================

const conflicts = ref<DetectedConflict[]>([])
const conflictsLoading = ref(false)
const conflictDetecting = ref(false)
const feasibilityLoading = ref(false)
const feasibilityReport = ref<FeasibilityReport | null>(null)
const conflictStatusFilter = ref<number | undefined>(undefined)

const resolveDialogVisible = ref(false)
const resolveNote = ref('')
const resolveAction = ref<'resolve' | 'ignore'>('resolve')
const resolveTargetId = ref<number>()
const resolveSaving = ref(false)

// ==================== Tab 5: Adjustments ====================

const adjustments = ref<ScheduleAdjustment[]>([])
const adjustmentLoading = ref(false)
const adjustmentView = ref<'my' | 'pending' | 'all'>('my')
const adjustmentPagination = reactive({ page: 1, size: 20, total: 0 })
const adjustmentDialogVisible = ref(false)
const adjSaving = ref(false)
const adjFormRef = ref<FormInstance>()
const adjForm = ref<Partial<ScheduleAdjustment & { newWeek?: number }>>({
  adjustmentType: 1,
  reason: '',
})
const adjEntryOptions = ref<ScheduleEntry[]>([])

const adjRules: FormRules = {
  adjustmentType: [{ required: true, message: '请选择调课类型', trigger: 'change' }],
  entryId: [{ required: true, message: '请选择课程', trigger: 'change' }],
  reason: [{ required: true, message: '请填写原因', trigger: 'blur' }],
}

// ==================== Tab 6: Export ====================

const exportType = ref<string>('class')

// ==================== Dialogs State ====================

const autoScheduleDialogVisible = ref(false)
const scheduling = ref(false)
const autoScheduleParams = ref({
  scheduleId: undefined as number | string | undefined,
  maxIterations: 1000,
  populationSize: 100,
  mutationRate: 0.1,
})

const entryDialogVisible = ref(false)
const entrySaving = ref(false)
const entryFormRef = ref<FormInstance>()
const entryForm = ref<Partial<ScheduleEntry>>({})
const taskOptionsForEntry = ref<{ id: number | string; courseName: string; className: string }[]>([])

const entryRules: FormRules = {
  taskId: [{ required: true, message: '请选择教学任务', trigger: 'change' }],
  dayOfWeek: [{ required: true, message: '请选择星期', trigger: 'change' }],
  classroomId: [{ required: true, message: '请选择教室', trigger: 'change' }],
  periodStart: [{ required: true, message: '请选择开始节次', trigger: 'change' }],
  periodEnd: [{ required: true, message: '请选择结束节次', trigger: 'change' }],
  weekStart: [{ required: true, message: '请输入开始周', trigger: 'blur' }],
  weekEnd: [{ required: true, message: '请输入结束周', trigger: 'blur' }],
}

const entryDetailVisible = ref(false)
const selectedEntry = ref<ScheduleEntry | null>(null)

// ==================== Data Loading ====================

async function loadSemesters() {
  try {
    const res = await semesterApi.list()
    semesters.value = (res as any).data || res
    if (Array.isArray(semesters.value) && semesters.value.length > 0) {
      const current = semesters.value.find(s => s.isCurrent)
      semesterId.value = current ? current.id : semesters.value[0].id
    }
  } catch (e) {
    console.error('Failed to load semesters:', e)
  }
}

async function loadScheduleList() {
  if (!semesterId.value) return
  try {
    const res = await scheduleApi.list({ semesterId: semesterId.value })
    scheduleList.value = (res as any).data || res
    if (Array.isArray(scheduleList.value) && scheduleList.value.length > 0 && !autoScheduleParams.value.scheduleId) {
      autoScheduleParams.value.scheduleId = scheduleList.value[0].id
    }
  } catch (e) {
    console.error('Failed to load schedules:', e)
  }
}

async function loadTasks() {
  if (!semesterId.value) return
  taskLoading.value = true
  try {
    const res = await teachingTaskApi.list({
      semesterId: semesterId.value,
      status: taskFilter.status,
      page: taskPagination.page,
      size: taskPagination.size,
    })
    const data = (res as any).data || res
    if (data.records) {
      tasks.value = data.records
      taskPagination.total = data.total || 0
    } else if (Array.isArray(data)) {
      tasks.value = data
      taskPagination.total = data.length
    }
  } catch (e) {
    console.error('Failed to load tasks:', e)
  } finally {
    taskLoading.value = false
  }
}

async function loadClassrooms() {
  try {
    const res = await request.get('/v9/places', { params: { typeCode: 'CLASSROOM' } })
    const data = (res as any).data || res
    const items = Array.isArray(data) ? data : data.records || []
    classrooms.value = items.map((p: any) => ({ id: p.id, name: p.placeName || p.name }))
  } catch (e) {
    console.error('Failed to load classrooms:', e)
  }
}

async function loadClassList() {
  try {
    const res = await request.get('/organization/classes/list')
    const data = (res as any).data || res
    const items = Array.isArray(data) ? data : data.records || []
    classList.value = items.map((c: any) => ({ id: c.id, name: c.className || c.name }))
  } catch (e) {
    console.error('Failed to load class list:', e)
  }
}

async function loadTeacherList() {
  try {
    const res = await request.get('/users', { params: { role: 'TEACHER' } })
    const data = (res as any).data || res
    const items = Array.isArray(data) ? data : data.records || []
    teacherList.value = items.map((t: any) => ({ id: t.id, name: t.realName || t.username || t.name }))
  } catch (e) {
    console.error('Failed to load teacher list:', e)
  }
}

async function loadTimetable() {
  if (!timetableTargetId.value || !semesterId.value) return
  try {
    let res
    if (timetableViewType.value === 'class') {
      res = await scheduleApi.getByClass(timetableTargetId.value, semesterId.value)
    } else if (timetableViewType.value === 'teacher') {
      res = await scheduleApi.getByTeacher(timetableTargetId.value, semesterId.value)
    } else {
      res = await scheduleApi.getByClassroom(timetableTargetId.value, semesterId.value)
    }
    timetableEntries.value = (res as any).data || res
  } catch (e) {
    console.error('Failed to load timetable:', e)
  }
}

async function loadConflicts() {
  if (!semesterId.value) return
  conflictsLoading.value = true
  try {
    const res = await conflictApi.list({
      semesterId: semesterId.value,
      status: conflictStatusFilter.value,
    })
    conflicts.value = (res as any).data || res
  } catch (e) {
    console.error('Failed to load conflicts:', e)
  } finally {
    conflictsLoading.value = false
  }
}

async function loadAdjustments() {
  adjustmentLoading.value = true
  try {
    let res
    const params = { page: adjustmentPagination.page, size: adjustmentPagination.size }
    if (adjustmentView.value === 'my') {
      res = await adjustmentApi.getMyApplications(params)
    } else if (adjustmentView.value === 'pending') {
      res = await adjustmentApi.getPendingApprovals(params)
    } else {
      res = await adjustmentApi.list(params)
    }
    const data = (res as any).data || res
    if (data.records) {
      adjustments.value = data.records
      adjustmentPagination.total = data.total || 0
    } else if (Array.isArray(data)) {
      adjustments.value = data
      adjustmentPagination.total = data.length
    }
  } catch (e) {
    console.error('Failed to load adjustments:', e)
  } finally {
    adjustmentLoading.value = false
  }
}

async function loadAdjEntryOptions() {
  if (!semesterId.value) return
  try {
    // Load entries from all schedule plans for this semester
    if (scheduleList.value.length > 0) {
      const res = await scheduleApi.getEntries(scheduleList.value[0].id)
      adjEntryOptions.value = (res as any).data || res
    }
  } catch (e) {
    console.error('Failed to load entry options:', e)
  }
}

// ==================== Semester Change ====================

function onSemesterChange() {
  loadScheduleList()
  loadTasks()
  if (activeTab.value === 'conflicts') loadConflicts()
  if (activeTab.value === 'adjustments') loadAdjustments()
  timetableEntries.value = []
  timetableTargetId.value = undefined
}

// ==================== Tab 1 Actions ====================

function handleAutoSchedule() {
  if (scheduleList.value.length === 0) {
    ElMessage.warning('当前学期暂无排课方案，请先创建排课方案')
    return
  }
  autoScheduleParams.value = {
    scheduleId: scheduleList.value[0].id,
    maxIterations: 1000,
    populationSize: 100,
    mutationRate: 0.1,
  }
  autoScheduleDialogVisible.value = true
}

async function runAutoSchedule() {
  if (!autoScheduleParams.value.scheduleId) {
    ElMessage.warning('请选择排课方案')
    return
  }
  scheduling.value = true
  try {
    const result = await scheduleApi.autoSchedule({
      scheduleId: autoScheduleParams.value.scheduleId,
      maxIterations: autoScheduleParams.value.maxIterations,
      populationSize: autoScheduleParams.value.populationSize,
      mutationRate: autoScheduleParams.value.mutationRate,
    })
    const data = (result as any).data || result
    if (data.success) {
      ElMessage.success(`排课完成，生成 ${data.entriesGenerated} 条排课记录`)
      if (data.conflicts?.length > 0) {
        ElMessage.warning(`存在 ${data.conflicts.length} 个冲突，请检查`)
      }
    } else {
      ElMessage.error('排课失败')
    }
    autoScheduleDialogVisible.value = false
    loadTasks()
    loadScheduleList()
  } catch (e) {
    ElMessage.error('排课失败，请检查约束配置')
  } finally {
    scheduling.value = false
  }
}

async function handleFeasibilityCheck() {
  if (!semesterId.value) return
  feasibilityLoading.value = true
  try {
    const res = await conflictApi.feasibilityCheck(semesterId.value)
    feasibilityReport.value = (res as any).data || res
    activeTab.value = 'conflicts'
    ElMessage.success('可行性检测完成')
  } catch (e) {
    ElMessage.error('可行性检测失败')
  } finally {
    feasibilityLoading.value = false
  }
}

async function handlePublish() {
  const draftSchedules = scheduleList.value.filter(s => s.status === 0)
  if (draftSchedules.length === 0) {
    ElMessage.info('暂无待发布的排课方案')
    return
  }
  await ElMessageBox.confirm(
    `将发布 ${draftSchedules.length} 个排课方案，发布后课表对所有用户可见，确定发布吗？`,
    '发布确认',
    { type: 'warning' },
  )
  try {
    for (const s of draftSchedules) {
      await scheduleApi.publish(s.id)
    }
    ElMessage.success('发布成功')
    loadScheduleList()
  } catch (e) {
    ElMessage.error('发布失败')
  }
}

function getMainTeacher(task: TeachingTask): string {
  if (task.teachers && task.teachers.length > 0) {
    const main = task.teachers.find(t => t.isMain)
    return main?.teacherName || task.teachers[0]?.teacherName || ''
  }
  return ''
}

function getScheduledHours(task: TeachingTask): number {
  // Estimate based on status; exact data would come from API
  if (task.status >= 2) return task.weeklyHours
  if (task.status === 1) return Math.floor(task.weeklyHours / 2)
  return 0
}

function viewTaskTimetable(task: TeachingTask) {
  activeTab.value = 'timetable'
  timetableViewType.value = 'class'
  timetableTargetId.value = task.classId
  loadTimetable()
}

function showManualEntryDialog(task: TeachingTask) {
  if (scheduleList.value.length === 0) {
    ElMessage.warning('请先创建排课方案')
    return
  }
  taskOptionsForEntry.value = [{
    id: task.id,
    courseName: task.courseName || '',
    className: task.className || '',
  }]
  entryForm.value = {
    scheduleId: scheduleList.value[0].id,
    taskId: task.id,
    weekStart: task.startWeek || 1,
    weekEnd: task.endWeek || 16,
    weekType: 0,
  }
  entryDialogVisible.value = true
}

async function saveEntry() {
  await entryFormRef.value?.validate()
  if (!entryForm.value.scheduleId) return
  entrySaving.value = true
  try {
    if (entryForm.value.id) {
      await scheduleApi.updateEntry(entryForm.value.scheduleId, entryForm.value.id, entryForm.value)
    } else {
      await scheduleApi.addEntry(entryForm.value.scheduleId, entryForm.value)
    }
    ElMessage.success('保存成功')
    entryDialogVisible.value = false
    loadTasks()
  } catch (e) {
    ElMessage.error('保存失败')
  } finally {
    entrySaving.value = false
  }
}

// ==================== Tab 2 Actions ====================

function onTimetableTypeChange() {
  timetableTargetId.value = undefined
  timetableEntries.value = []
}

function showEntryDetail(entry: ScheduleEntry) {
  selectedEntry.value = entry
  entryDetailVisible.value = true
}

// ==================== Tab 4 Actions ====================

async function runFeasibilityCheck() {
  if (!semesterId.value) return
  feasibilityLoading.value = true
  try {
    const res = await conflictApi.feasibilityCheck(semesterId.value)
    feasibilityReport.value = (res as any).data || res
    ElMessage.success('可行性检测完成')
  } catch (e) {
    ElMessage.error('检测失败')
  } finally {
    feasibilityLoading.value = false
  }
}

async function runConflictDetection() {
  if (!semesterId.value) return
  conflictDetecting.value = true
  try {
    const res = await conflictApi.detect(semesterId.value)
    const detected = (res as any).data || res
    if (Array.isArray(detected)) {
      ElMessage.success(`检测完成，发现 ${detected.length} 个冲突`)
    }
    loadConflicts()
  } catch (e) {
    ElMessage.error('检测失败')
  } finally {
    conflictDetecting.value = false
  }
}

function resolveConflict(conflict: DetectedConflict) {
  resolveAction.value = 'resolve'
  resolveTargetId.value = conflict.id
  resolveNote.value = ''
  resolveDialogVisible.value = true
}

function ignoreConflict(conflict: DetectedConflict) {
  resolveAction.value = 'ignore'
  resolveTargetId.value = conflict.id
  resolveNote.value = ''
  resolveDialogVisible.value = true
}

async function confirmResolve() {
  if (!resolveTargetId.value) return
  if (!resolveNote.value.trim()) {
    ElMessage.warning('请填写备注')
    return
  }
  resolveSaving.value = true
  try {
    if (resolveAction.value === 'resolve') {
      await conflictApi.resolve(resolveTargetId.value, resolveNote.value)
    } else {
      await conflictApi.ignore(resolveTargetId.value, resolveNote.value)
    }
    ElMessage.success('操作成功')
    resolveDialogVisible.value = false
    loadConflicts()
  } catch (e) {
    ElMessage.error('操作失败')
  } finally {
    resolveSaving.value = false
  }
}

// ==================== Tab 5 Actions ====================

function showAdjustmentDialog() {
  adjForm.value = { adjustmentType: 1, reason: '' }
  loadAdjEntryOptions()
  adjustmentDialogVisible.value = true
}

async function submitAdjustment() {
  await adjFormRef.value?.validate()
  adjSaving.value = true
  try {
    await adjustmentApi.apply({
      entryId: adjForm.value.entryId!,
      adjustmentType: adjForm.value.adjustmentType!,
      newClassroomId: adjForm.value.newClassroomId,
      newDayOfWeek: adjForm.value.newDayOfWeek,
      newPeriodStart: adjForm.value.newPeriodStart,
      newPeriodEnd: adjForm.value.newPeriodEnd,
      newWeek: adjForm.value.newWeek,
      reason: adjForm.value.reason!,
    })
    ElMessage.success('申请已提交')
    adjustmentDialogVisible.value = false
    loadAdjustments()
  } catch (e) {
    ElMessage.error('提交失败')
  } finally {
    adjSaving.value = false
  }
}

async function approveAdjustment(adj: ScheduleAdjustment) {
  await ElMessageBox.confirm('确定批准该调课申请？', '审批确认')
  try {
    await adjustmentApi.approve(adj.id)
    ElMessage.success('已批准')
    loadAdjustments()
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

async function rejectAdjustment(adj: ScheduleAdjustment) {
  const { value } = await ElMessageBox.prompt('请填写驳回原因', '驳回确认', {
    inputType: 'textarea',
    inputValidator: (v) => (v && v.trim() ? true : '请填写驳回原因'),
  })
  try {
    await adjustmentApi.reject(adj.id, value)
    ElMessage.success('已驳回')
    loadAdjustments()
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

async function executeAdjustment(adj: ScheduleAdjustment) {
  await ElMessageBox.confirm('执行后将正式修改课表，确定执行？', '执行确认', { type: 'warning' })
  try {
    await adjustmentApi.execute(adj.id)
    ElMessage.success('已执行')
    loadAdjustments()
  } catch (e) {
    ElMessage.error('执行失败')
  }
}

async function cancelAdjustment(adj: ScheduleAdjustment) {
  await ElMessageBox.confirm('确定撤回该申请？', '撤回确认')
  try {
    await adjustmentApi.cancel(adj.id)
    ElMessage.success('已撤回')
    loadAdjustments()
  } catch (e) {
    ElMessage.error('撤回失败')
  }
}

// ==================== Tab 6 Actions ====================

function handleExport() {
  ElMessage.info('导出功能开发中...')
}

function handlePrint() {
  ElMessage.info('打印功能开发中...')
}

// ==================== Helpers ====================

function getWeekdayName(day: number) {
  return weekdays.find(w => w.value === day)?.label || ''
}

function formatWeeks(entry: ScheduleEntry) {
  const weekTypeText = entry.weekType === 1 ? '(单)' : entry.weekType === 2 ? '(双)' : ''
  return `${entry.weekStart}-${entry.weekEnd}周${weekTypeText}`
}

function getTaskStatusName(status: number) {
  const m: Record<number, string> = { 0: '待分配', 1: '已分配', 2: '已排课', 3: '进行中', 4: '已结束' }
  return m[status] || '未知'
}

function getTaskStatusTag(status: number) {
  const m: Record<number, '' | 'success' | 'warning' | 'info' | 'danger'> = {
    0: 'info', 1: 'warning', 2: 'success', 3: '', 4: 'info',
  }
  return m[status] || 'info'
}

function getConflictCategoryName(cat: number) {
  const m: Record<number, string> = { 1: '资源冲突', 2: '约束违反', 3: '时间冲突' }
  return m[cat] || '未知'
}

function getConflictCategoryTag(cat: number) {
  const m: Record<number, '' | 'success' | 'warning' | 'info' | 'danger'> = {
    1: 'danger', 2: 'warning', 3: '',
  }
  return m[cat] || 'info'
}

function getSeverityName(s: number) {
  const m: Record<number, string> = { 1: '低', 2: '中', 3: '高' }
  return m[s] || '-'
}

function getSeverityTag(s: number) {
  const m: Record<number, '' | 'success' | 'warning' | 'info' | 'danger'> = {
    1: 'info', 2: 'warning', 3: 'danger',
  }
  return m[s] || 'info'
}

function getResolutionStatusName(s: number) {
  const m: Record<number, string> = { 0: '未处理', 1: '已解决', 2: '已忽略' }
  return m[s] || '-'
}

function getResolutionStatusTag(s: number) {
  const m: Record<number, '' | 'success' | 'warning' | 'info' | 'danger'> = {
    0: 'danger', 1: 'success', 2: 'info',
  }
  return m[s] || 'info'
}

function getAdjTypeName(t: number) {
  const m: Record<number, string> = { 1: '调课', 2: '停课', 3: '补课' }
  return m[t] || '未知'
}

function getAdjTypeTag(t: number) {
  const m: Record<number, '' | 'success' | 'warning' | 'info' | 'danger'> = {
    1: '', 2: 'danger', 3: 'success',
  }
  return m[t] || 'info'
}

function getAdjStatusName(s: number) {
  const m: Record<number, string> = { 0: '待审批', 1: '已批准', 2: '已驳回', 3: '已执行', 4: '已取消' }
  return m[s] || '未知'
}

function getAdjStatusTag(s: number) {
  const m: Record<number, '' | 'success' | 'warning' | 'info' | 'danger'> = {
    0: 'warning', 1: 'success', 2: 'danger', 3: '', 4: 'info',
  }
  return m[s] || 'info'
}

// ==================== Tab Change Watcher ====================

watch(activeTab, (tab) => {
  if (tab === 'conflicts') {
    loadConflicts()
  } else if (tab === 'adjustments') {
    loadAdjustments()
  }
})

watch(adjustmentView, () => {
  adjustmentPagination.page = 1
  loadAdjustments()
})

// ==================== Init ====================

onMounted(async () => {
  globalLoading.value = true
  try {
    await loadSemesters()
    await Promise.all([
      loadScheduleList(),
      loadTasks(),
      loadClassrooms(),
      loadClassList(),
      loadTeacherList(),
    ])
  } finally {
    globalLoading.value = false
  }
})
</script>
