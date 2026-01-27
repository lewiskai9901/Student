<template>
  <div class="calendar-page">
    <!-- ========== 第一级：学年列表界面 ========== -->
    <template v-if="!selectedYear">
      <div class="page-toolbar">
        <div class="toolbar-left">
          <h2 class="page-title">校历管理</h2>
        </div>
        <div class="toolbar-right">
          <el-button type="primary" @click="showYearDialog()">
            <el-icon><Plus /></el-icon>新建学年
          </el-button>
        </div>
      </div>

      <div class="year-grid">
        <div
          v-for="year in academicYears"
          :key="year.id"
          class="year-card-large"
          :class="{ current: year.isCurrent }"
          @click="enterYear(year)"
        >
          <div class="year-badge" v-if="year.isCurrent">当前学年</div>
          <div class="year-icon">📅</div>
          <div class="year-name">{{ year.name }}</div>
          <div class="year-period">{{ year.startDate }} ~ {{ year.endDate }}</div>
          <div class="year-stats-row">
            <div class="stat-item">
              <span class="stat-num">{{ getYearSemesterCount(year.id) }}</span>
              <span class="stat-label">学期</span>
            </div>
            <div class="stat-item">
              <span class="stat-num">{{ getYearEventCount(year.id) }}</span>
              <span class="stat-label">事件</span>
            </div>
          </div>
          <div class="year-action">
            <span>点击进入</span>
            <el-icon><ArrowRight /></el-icon>
          </div>
        </div>

        <!-- 空状态 -->
        <div v-if="!academicYears.length" class="empty-state">
          <div class="empty-icon">📆</div>
          <div class="empty-text">暂无学年数据</div>
          <el-button type="primary" @click="showYearDialog()">创建第一个学年</el-button>
        </div>
      </div>
    </template>

    <!-- ========== 第二级：校历详情界面（Tab布局） ========== -->
    <template v-else>
      <!-- 顶部工具栏 -->
      <div class="page-toolbar">
        <div class="toolbar-left">
          <div class="back-button" @click="selectedYear = null">
            <el-icon><ArrowLeft /></el-icon>
            <span>返回学年列表</span>
          </div>
          <div class="current-year-title">
            <h2 class="page-title">{{ selectedYear.name }}</h2>
            <el-tag v-if="selectedYear.isCurrent" type="success" size="small">当前</el-tag>
          </div>
        </div>
        <div class="toolbar-right">
          <el-button @click="showYearDialog(selectedYear)">编辑学年</el-button>
          <el-dropdown trigger="click" style="margin-left: 8px">
            <el-button type="primary">
              快捷操作 <el-icon class="el-icon--right"><ArrowRight /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="showSemesterDialog()">新建学期</el-dropdown-item>
                <el-dropdown-item @click="showEventDialog()">添加事件</el-dropdown-item>
                <el-dropdown-item @click="showPeriodConfigDialog()">配置作息时间</el-dropdown-item>
                <el-dropdown-item @click="showGradeActivityDialog()">添加年级活动</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>

      <!-- 统计概览 -->
      <div class="stats-row">
      <div class="stat-card">
        <div class="stat-label">当前学期</div>
        <div class="stat-value">{{ currentSemester?.name || '未设置' }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">当前周次</div>
        <div class="stat-value">{{ currentWeekNumber ? `第${currentWeekNumber}周` : '-' }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">剩余天数</div>
        <div class="stat-value">{{ daysRemaining }}天</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">学期进度</div>
        <div class="stat-value">
          <el-progress :percentage="semesterProgress" :stroke-width="8" :show-text="false" style="width: 80px" />
          <span style="margin-left: 8px; font-size: 13px">{{ semesterProgress }}%</span>
        </div>
      </div>
    </div>

    <!-- 主体内容：Tab布局 -->
    <el-tabs v-model="activeTab" class="calendar-tabs">
      <!-- =============== Tab 1: 学期管理 =============== -->
      <el-tab-pane label="学期管理" name="semester">
        <el-row :gutter="16">
          <!-- 左侧：学期列表 -->
          <el-col :span="8">
            <div class="panel">
              <div class="panel-header">
                <span class="panel-title">学期列表</span>
                <el-button type="primary" size="small" @click="showSemesterDialog()">
                  <el-icon><Plus /></el-icon>新建学期
                </el-button>
              </div>
              <div class="semester-list-panel">
                <div
                  v-for="sem in semesters"
                  :key="sem.id"
                  class="semester-item"
                  :class="{ active: selectedSemester?.id === sem.id, current: sem.isCurrent }"
                  @click="selectSemester(sem)"
                >
                  <div class="sem-main">
                    <div class="sem-type-badge" :class="'type-' + sem.termType">
                      {{ sem.termType === 1 ? '秋季' : sem.termType === 2 ? '春季' : '夏季' }}
                    </div>
                    <div class="sem-info">
                      <div class="sem-name">
                        {{ sem.name }}
                        <el-tag v-if="sem.isCurrent" size="small" type="success">当前</el-tag>
                      </div>
                      <div class="sem-meta">{{ sem.startDate }} ~ {{ sem.endDate }}</div>
                    </div>
                  </div>
                  <div class="sem-stats">
                    <span>{{ sem.teachingWeeks }}教学周</span>
                  </div>
                </div>
                <el-empty v-if="!semesters.length" description="暂无学期，请先创建" :image-size="60" />
              </div>
            </div>
          </el-col>

          <!-- 右侧：学期详情 & 教学周管理 -->
          <el-col :span="16">
            <div v-if="selectedSemester" class="panel">
              <div class="panel-header">
                <span class="panel-title">{{ selectedSemester.name }} - 教学周管理</span>
                <div class="header-actions">
                  <el-button size="small" @click="showSemesterDialog(selectedSemester)">编辑学期</el-button>
                  <el-button type="primary" size="small" @click="generateWeeks">自动生成周次</el-button>
                </div>
              </div>
              <div class="semester-detail-content">
                <!-- 学期基本信息 -->
                <div class="semester-info-card">
                  <div class="info-grid">
                    <div class="info-item">
                      <span class="label">开始日期</span>
                      <span class="value">{{ selectedSemester.startDate }}</span>
                    </div>
                    <div class="info-item">
                      <span class="label">结束日期</span>
                      <span class="value">{{ selectedSemester.endDate }}</span>
                    </div>
                    <div class="info-item">
                      <span class="label">教学周数</span>
                      <span class="value">{{ selectedSemester.teachingWeeks }}周</span>
                    </div>
                    <div class="info-item">
                      <span class="label">状态</span>
                      <span class="value">
                        <el-tag :type="selectedSemester.isCurrent ? 'success' : 'info'" size="small">
                          {{ selectedSemester.isCurrent ? '当前学期' : '非当前' }}
                        </el-tag>
                      </span>
                    </div>
                  </div>
                  <el-button v-if="!selectedSemester.isCurrent" type="primary" text @click="setCurrentSemester(selectedSemester.id)">
                    设为当前学期
                  </el-button>
                </div>

                <!-- 教学周列表 -->
                <div class="weeks-table-container">
                  <el-table :data="teachingWeeks" border size="small" max-height="400">
                    <el-table-column prop="weekNumber" label="周次" width="80" align="center">
                      <template #default="{ row }">
                        <span class="week-number">第{{ row.weekNumber }}周</span>
                      </template>
                    </el-table-column>
                    <el-table-column prop="startDate" label="开始日期" width="110" align="center" />
                    <el-table-column prop="endDate" label="结束日期" width="110" align="center" />
                    <el-table-column prop="weekType" label="周类型" width="100" align="center">
                      <template #default="{ row }">
                        <el-tag size="small" :type="getWeekTypeTag(row.weekType)">
                          {{ getWeekTypeName(row.weekType) }}
                        </el-tag>
                      </template>
                    </el-table-column>
                    <el-table-column prop="remark" label="备注/标签" min-width="120">
                      <template #default="{ row }">
                        <span class="week-remark">{{ row.remark || '-' }}</span>
                      </template>
                    </el-table-column>
                    <el-table-column label="操作" width="80" align="center">
                      <template #default="{ row }">
                        <el-button text size="small" @click="editWeek(row)">编辑</el-button>
                      </template>
                    </el-table-column>
                  </el-table>
                </div>
              </div>
            </div>
            <div v-else class="panel empty-detail">
              <el-empty description="请在左侧选择一个学期查看详情" :image-size="80" />
            </div>
          </el-col>
        </el-row>
      </el-tab-pane>

      <!-- =============== Tab 2: 校历事件 =============== -->
      <el-tab-pane label="校历事件" name="events">
        <el-row :gutter="16">
          <!-- 左侧：日历视图 -->
          <el-col :span="14">
            <div class="panel calendar-panel">
              <div class="panel-header">
                <div class="month-nav">
                  <el-button text size="small" @click="prevMonth"><el-icon><ArrowLeft /></el-icon></el-button>
                  <span class="month-text">{{ currentMonthText }}</span>
                  <el-button text size="small" @click="nextMonth"><el-icon><ArrowRight /></el-icon></el-button>
                </div>
                <div class="header-actions">
                  <el-button text type="primary" size="small" @click="goToday">今天</el-button>
                  <el-radio-group v-model="calendarViewMode" size="small">
                    <el-radio-button label="month">月历</el-radio-button>
                    <el-radio-button label="list">周历</el-radio-button>
                  </el-radio-group>
                </div>
              </div>
              <!-- 月视图 -->
              <div
                v-if="calendarViewMode === 'month'"
                class="mini-calendar large-calendar"
                :class="{ 'is-dragging': isDragging }"
                @mouseup="handleDragEnd"
                @mouseleave="handleDragEnd"
              >
                <div class="cal-weekdays">
                  <span class="week-col">周</span>
                  <span v-for="d in ['日','一','二','三','四','五','六']" :key="d">{{ d }}</span>
                </div>
                <div class="cal-weeks">
                  <div v-for="(week, weekIdx) in calendarWeeks" :key="weekIdx" class="cal-week-row">
                    <div class="week-num">{{ week.weekNumber || '-' }}</div>
                    <div
                      v-for="(day, dayIdx) in week.days"
                      :key="dayIdx"
                      class="cal-day"
                      :class="[
                        {
                          'other': !day.isCurrentMonth,
                          'today': day.isToday,
                          'weekend': day.isWeekend,
                          'drag-selected': isDayInDragSelection(day.date),
                        },
                        day.events.length ? 'has-event event-type-' + day.events[0].eventType : ''
                      ]"
                      @mousedown.prevent="handleCalendarDragStart(day, $event)"
                      @mouseenter="handleCalendarDragMove(day)"
                      @click.prevent="handleDayClick(day)"
                    >
                      <span class="day-num">{{ day.day }}</span>
                      <span v-if="day.events.length" class="event-label">
                        {{ day.events[0].title?.substring(0, 4) || getShortEventName(day.events[0].eventType) }}
                      </span>
                    </div>
                  </div>
                </div>
                <div class="drag-hint-bar" :class="{ 'hidden': isDragging }">拖拽选择日期范围快速添加事件</div>
              </div>
              <!-- 学期周历视图 -->
              <div v-else class="semester-overview" :class="{ 'is-dragging': isDragging }" @mouseup="handleDragEnd" @mouseleave="handleDragEnd">
                <div class="overview-header">
                  <span>{{ currentSemester?.name || '本学期' }} 校历总览</span>
                </div>
                <div class="overview-table-wrapper">
                  <table class="overview-table" @selectstart.prevent>
                    <thead>
                      <tr>
                        <th class="col-month">月</th>
                        <th class="col-week">周</th>
                        <th v-for="d in ['日','一','二','三','四','五','六']" :key="d" class="col-day">{{ d }}</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr v-for="week in semesterWeeksData" :key="week.weekNumber" :class="{ 'month-last-row': week.isMonthLastRow }">
                        <td v-if="week.monthRowSpan > 0" class="col-month" :rowspan="week.monthRowSpan">{{ week.monthLabel }}</td>
                        <td class="col-week">{{ week.weekNumber }}</td>
                        <td
                          v-for="(day, idx) in week.days"
                          :key="idx"
                          class="col-day"
                          :class="[
                            { 'today': day.isToday, 'has-event': day.events.length > 0, 'drag-selected': isDayInDragSelection(day.date), 'empty-cell': !day.date },
                            day.events.length ? 'event-type-' + day.events[0].eventType : ''
                          ]"
                          @mousedown.prevent="handleDragStart(day, $event)"
                          @mouseenter="handleDragMove(day)"
                          @click.prevent="handleOverviewDayClick(day)"
                        >
                          <span class="day-num">{{ day.day || '' }}</span>
                          <span v-if="day.events.length" class="day-event">{{ day.events[0].title?.substring(0, 2) }}</span>
                        </td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              </div>
              <div class="cal-legend">
                <span class="legend-item t1"><i class="legend-bar"></i>开学</span>
                <span class="legend-item t2"><i class="legend-bar"></i>放假</span>
                <span class="legend-item t3"><i class="legend-bar"></i>考试</span>
                <span class="legend-item t4"><i class="legend-bar"></i>活动</span>
                <span class="legend-item t6"><i class="legend-bar"></i>补课</span>
              </div>
            </div>
          </el-col>

          <!-- 右侧：事件列表 -->
          <el-col :span="10">
            <div class="panel full-height">
              <div class="panel-header">
                <span class="panel-title">事件列表</span>
                <div class="header-actions">
                  <el-radio-group v-model="eventFilter" size="small">
                    <el-radio-button label="all">全部</el-radio-button>
                    <el-radio-button label="upcoming">近期</el-radio-button>
                  </el-radio-group>
                  <el-button type="primary" size="small" @click="showEventDialog()" style="margin-left: 8px">
                    <el-icon><Plus /></el-icon>添加
                  </el-button>
                </div>
              </div>
              <div class="event-list">
                <div v-for="event in filteredEvents" :key="event.id" class="event-card" @click="showEventDialog(event)">
                  <div class="event-date" :class="'type-' + event.eventType">
                    <span class="day">{{ getEventDay(event.startDate) }}</span>
                    <span class="month">{{ getEventMonth(event.startDate) }}</span>
                  </div>
                  <div class="event-body">
                    <div class="event-title">{{ event.title }}</div>
                    <div class="event-desc">
                      <el-tag size="small" :type="getEventTagType(event.eventType)">{{ getEventTypeName(event.eventType) }}</el-tag>
                      <span v-if="event.endDate && event.endDate !== event.startDate" class="date-range">
                        {{ formatEventDate(event.startDate) }} ~ {{ formatEventDate(event.endDate) }}
                      </span>
                      <span v-else class="countdown">{{ getEventCountdown(event.startDate) }}</span>
                    </div>
                  </div>
                </div>
                <el-empty v-if="!filteredEvents.length" description="暂无事件" :image-size="50" />
              </div>
            </div>
          </el-col>
        </el-row>
      </el-tab-pane>

      <!-- =============== Tab 3: 作息时间 =============== -->
      <el-tab-pane label="作息时间" name="periods">
        <div class="panel">
          <div class="panel-header">
            <span class="panel-title">节次时间配置</span>
            <div class="header-actions">
              <el-select v-model="periodConfigSemester" placeholder="选择学期" size="small" style="width: 200px; margin-right: 8px">
                <el-option label="全校默认" :value="0" />
                <el-option v-for="sem in semesters" :key="sem.id" :label="sem.name" :value="sem.id" />
              </el-select>
              <el-button type="primary" size="small" @click="showPeriodConfigDialog()">
                <el-icon><Plus /></el-icon>添加节次
              </el-button>
            </div>
          </div>
          <div class="period-config-content">
            <el-table :data="periodConfigs" border>
              <el-table-column prop="period" label="序号" width="80" align="center" />
              <el-table-column prop="name" label="名称" width="120" align="center" />
              <el-table-column prop="startTime" label="开始时间" width="120" align="center" />
              <el-table-column prop="endTime" label="结束时间" width="120" align="center" />
              <el-table-column label="时段" width="100" align="center">
                <template #default="{ row }">
                  <el-tag size="small" :type="row.period <= 4 ? 'success' : row.period <= 8 ? 'warning' : 'info'">
                    {{ row.period <= 4 ? '上午' : row.period <= 8 ? '下午' : '晚上' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="120" align="center">
                <template #default="{ row }">
                  <el-button text size="small" @click="editPeriodConfig(row)">编辑</el-button>
                  <el-button text size="small" type="danger" @click="deletePeriodConfig(row)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
            <div class="period-tips">
              <el-icon><InfoFilled /></el-icon>
              <span>作息时间是排课的基础配置，可以为不同学期设置不同的作息时间（如夏季/冬季作息）</span>
            </div>
          </div>
        </div>
      </el-tab-pane>

      <!-- =============== Tab 4: 年级活动 =============== -->
      <el-tab-pane label="年级活动" name="gradeActivities">
        <el-row :gutter="16">
          <!-- 左侧：年级筛选 -->
          <el-col :span="6">
            <div class="panel">
              <div class="panel-header">
                <span class="panel-title">年级筛选</span>
              </div>
              <div class="grade-filter-list">
                <div
                  class="grade-filter-item"
                  :class="{ active: selectedGradeFilter === 'all' }"
                  @click="selectedGradeFilter = 'all'"
                >
                  <span>全部年级</span>
                  <span class="count">{{ gradeActivities.length }}</span>
                </div>
                <div
                  v-for="grade in grades"
                  :key="grade.id"
                  class="grade-filter-item"
                  :class="{ active: selectedGradeFilter === grade.id }"
                  @click="selectedGradeFilter = grade.id"
                >
                  <span>{{ grade.name }}</span>
                  <span class="count">{{ getGradeActivityCount(grade.id) }}</span>
                </div>
              </div>
            </div>
          </el-col>

          <!-- 右侧：活动列表 -->
          <el-col :span="18">
            <div class="panel">
              <div class="panel-header">
                <span class="panel-title">年级活动列表</span>
                <div class="header-actions">
                  <el-select v-model="activityTypeFilter" placeholder="活动类型" size="small" clearable style="width: 120px; margin-right: 8px">
                    <el-option label="军训" :value="1" />
                    <el-option label="入学教育" :value="2" />
                    <el-option label="专业实习" :value="3" />
                    <el-option label="社会实践" :value="4" />
                    <el-option label="毕业实习" :value="5" />
                    <el-option label="毕业设计" :value="6" />
                    <el-option label="毕业答辩" :value="7" />
                    <el-option label="其他" :value="10" />
                  </el-select>
                  <el-button type="primary" size="small" @click="showGradeActivityDialog()">
                    <el-icon><Plus /></el-icon>添加活动
                  </el-button>
                </div>
              </div>
              <el-table :data="filteredGradeActivities" border>
                <el-table-column prop="gradeName" label="年级" width="100" />
                <el-table-column prop="activityName" label="活动名称" min-width="150" />
                <el-table-column label="活动类型" width="100">
                  <template #default="{ row }">
                    <el-tag size="small">{{ getActivityTypeName(row.activityType) }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="时间" width="200">
                  <template #default="{ row }">
                    {{ row.startDate }} ~ {{ row.endDate || row.startDate }}
                  </template>
                </el-table-column>
                <el-table-column label="周次" width="100">
                  <template #default="{ row }">
                    <span v-if="row.startWeek">第{{ row.startWeek }}-{{ row.endWeek || row.startWeek }}周</span>
                    <span v-else>-</span>
                  </template>
                </el-table-column>
                <el-table-column label="影响教学" width="100" align="center">
                  <template #default="{ row }">
                    <el-tag :type="row.affectTeaching ? 'warning' : 'info'" size="small">
                      {{ row.affectTeaching ? '是' : '否' }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="120" align="center">
                  <template #default="{ row }">
                    <el-button text size="small" @click="editGradeActivity(row)">编辑</el-button>
                    <el-button text size="small" type="danger" @click="deleteGradeActivity(row)">删除</el-button>
                  </template>
                </el-table-column>
              </el-table>
              <el-empty v-if="!filteredGradeActivities.length" description="暂无年级活动" />
            </div>
          </el-col>
        </el-row>
      </el-tab-pane>
    </el-tabs>
    </template>

    <!-- 新建学年对话框 -->
    <el-dialog
      v-model="yearDialogVisible"
      :title="yearForm.id ? '编辑学年' : '新建学年'"
      width="500px"
      class="custom-dialog"
      :close-on-click-modal="false"
    >
      <div class="dialog-form">
        <el-form ref="yearFormRef" :model="yearForm" :rules="yearRules" label-position="top">
          <el-form-item label="学年名称" prop="name">
            <el-input v-model="yearForm.name" placeholder="请输入学年名称，如：2025-2026学年" size="large" />
          </el-form-item>
          <div class="form-row">
            <el-form-item label="开始日期" prop="startDate" class="flex-1">
              <el-date-picker
                v-model="yearForm.startDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="选择开始日期"
                size="large"
                style="width: 100%"
              />
            </el-form-item>
            <span class="date-separator">至</span>
            <el-form-item label="结束日期" prop="endDate" class="flex-1">
              <el-date-picker
                v-model="yearForm.endDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="选择结束日期"
                size="large"
                style="width: 100%"
              />
            </el-form-item>
          </div>
          <div class="form-tip">
            <el-icon><InfoFilled /></el-icon>
            <span>学年通常从9月开始，到次年8月结束</span>
          </div>
        </el-form>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="yearDialogVisible = false" size="large">取消</el-button>
          <el-button type="primary" :loading="saving" @click="saveYear" size="large">
            {{ yearForm.id ? '保存修改' : '创建学年' }}
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 新建学期对话框 -->
    <el-dialog
      v-model="semesterDialogVisible"
      :title="semesterForm.id ? '编辑学期' : '新建学期'"
      width="520px"
      class="custom-dialog"
      :close-on-click-modal="false"
    >
      <div class="dialog-form">
        <el-form ref="semesterFormRef" :model="semesterForm" :rules="semesterRules" label-position="top">
          <el-form-item label="学期名称" prop="name">
            <el-input v-model="semesterForm.name" placeholder="请输入学期名称" size="large" />
          </el-form-item>
          <el-form-item label="学期类型" prop="termType">
            <div class="term-type-selector">
              <div
                v-for="term in [{value: 1, label: '第一学期', icon: '①'}, {value: 2, label: '第二学期', icon: '②'}, {value: 3, label: '短学期', icon: '☀'}]"
                :key="term.value"
                class="term-type-item"
                :class="{ active: semesterForm.termType === term.value }"
                @click="semesterForm.termType = term.value"
              >
                <span class="term-icon">{{ term.icon }}</span>
                <span class="term-label">{{ term.label }}</span>
              </div>
            </div>
          </el-form-item>
          <div class="form-row">
            <el-form-item label="开始日期" prop="startDate" class="flex-1">
              <el-date-picker
                v-model="semesterForm.startDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="选择开始日期"
                size="large"
                style="width: 100%"
              />
            </el-form-item>
            <span class="date-separator">至</span>
            <el-form-item label="结束日期" prop="endDate" class="flex-1">
              <el-date-picker
                v-model="semesterForm.endDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="选择结束日期"
                size="large"
                style="width: 100%"
              />
            </el-form-item>
          </div>
          <el-form-item label="教学周数" prop="teachingWeeks">
            <div class="weeks-input">
              <el-input-number
                v-model="semesterForm.teachingWeeks"
                :min="1"
                :max="30"
                size="large"
                controls-position="right"
              />
              <span class="weeks-unit">周</span>
              <span class="weeks-hint">（一般为16-20周）</span>
            </div>
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="semesterDialogVisible = false" size="large">取消</el-button>
          <el-button type="primary" :loading="saving" @click="saveSemester" size="large">
            {{ semesterForm.id ? '保存修改' : '创建学期' }}
          </el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="weeksDialogVisible" title="教学周历" width="650px">
      <div style="display: flex; justify-content: space-between; margin-bottom: 12px">
        <span style="font-weight: 500">{{ currentSemesterForWeeks?.name }}</span>
        <el-button type="primary" size="small" @click="generateWeeks">自动生成</el-button>
      </div>
      <el-table :data="teachingWeeks" border size="small" max-height="400">
        <el-table-column prop="weekNumber" label="周次" width="70" align="center">
          <template #default="{ row }">第{{ row.weekNumber }}周</template>
        </el-table-column>
        <el-table-column prop="startDate" label="开始" width="100" align="center" />
        <el-table-column prop="endDate" label="结束" width="100" align="center" />
        <el-table-column prop="weekType" label="类型" width="80" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="getWeekTypeTag(row.weekType)">{{ getWeekTypeName(row.weekType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" />
      </el-table>
    </el-dialog>

    <el-dialog v-model="eventDialogVisible" :title="eventForm.id ? '编辑事件' : '添加事件'" width="450px">
      <el-form ref="eventFormRef" :model="eventForm" :rules="eventRules" label-width="80px">
        <el-form-item label="事件标题" prop="title">
          <el-input v-model="eventForm.title" />
        </el-form-item>
        <el-form-item label="事件类型" prop="eventType">
          <el-select v-model="eventForm.eventType" style="width: 100%">
            <el-option :value="1" label="开学" />
            <el-option :value="2" label="放假" />
            <el-option :value="3" label="考试" />
            <el-option :value="4" label="活动" />
            <el-option :value="5" label="其他" />
            <el-option :value="6" label="补课" />
          </el-select>
        </el-form-item>
        <el-form-item label="持续事件">
          <el-switch v-model="eventForm.isRange" />
          <span style="margin-left: 8px; color: #86909c; font-size: 12px">开启后可设置结束日期</span>
        </el-form-item>
        <el-form-item v-if="!eventForm.isRange" label="日期" prop="startDate">
          <el-date-picker v-model="eventForm.startDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item v-if="eventForm.isRange" label="开始日期" prop="startDate">
          <el-date-picker v-model="eventForm.startDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item v-if="eventForm.isRange" label="结束日期" prop="endDate">
          <el-date-picker v-model="eventForm.endDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="eventForm.description" type="textarea" rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div style="display: flex; justify-content: space-between">
          <el-button v-if="eventForm.id" type="danger" text @click="deleteEvent">删除</el-button>
          <div>
            <el-button @click="eventDialogVisible = false">取消</el-button>
            <el-button type="primary" :loading="saving" @click="saveEvent">保存</el-button>
          </div>
        </div>
      </template>
    </el-dialog>

    <!-- 作息时间配置对话框 -->
    <el-dialog
      v-model="periodConfigDialogVisible"
      :title="periodConfigForm.id ? '编辑节次' : '添加节次'"
      width="420px"
      class="custom-dialog"
      :close-on-click-modal="false"
    >
      <div class="dialog-form">
        <el-form :model="periodConfigForm" label-position="top">
          <el-row :gutter="16">
            <el-col :span="8">
              <el-form-item label="节次">
                <el-input-number
                  v-model="periodConfigForm.period"
                  :min="1"
                  :max="15"
                  controls-position="right"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>
            <el-col :span="16">
              <el-form-item label="名称">
                <el-input v-model="periodConfigForm.name" placeholder="如：第一节、早读" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="开始时间">
                <el-time-picker
                  v-model="periodConfigForm.startTime"
                  format="HH:mm"
                  value-format="HH:mm"
                  placeholder="选择开始时间"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="结束时间">
                <el-time-picker
                  v-model="periodConfigForm.endTime"
                  format="HH:mm"
                  value-format="HH:mm"
                  placeholder="选择结束时间"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>
          </el-row>
          <div class="period-preview" v-if="periodConfigForm.startTime && periodConfigForm.endTime">
            <el-icon><Clock /></el-icon>
            <span>{{ periodConfigForm.name }}: {{ periodConfigForm.startTime }} - {{ periodConfigForm.endTime }}</span>
            <span class="duration">
              ({{ calculateDuration(periodConfigForm.startTime, periodConfigForm.endTime) }}分钟)
            </span>
          </div>
        </el-form>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="periodConfigDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="savePeriodConfig">
            {{ periodConfigForm.id ? '保存修改' : '添加' }}
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { ArrowLeft, ArrowRight, Setting, List, Calendar, InfoFilled, Plus, Clock } from '@element-plus/icons-vue'
import { academicYearApi, semesterApi, academicEventApi } from '@/api/teaching'
import type { AcademicYear, Semester, TeachingWeek, AcademicEvent } from '@/types/teaching'

const academicYears = ref<AcademicYear[]>([])
const currentYearId = ref<number>()
const semesters = ref<Semester[]>([])
const events = ref<AcademicEvent[]>([])
const selectedYear = ref<AcademicYear | null>(null)
const yearSemesterCounts = ref<Map<number, number>>(new Map())
const yearEventCounts = ref<Map<number, number>>(new Map())
const teachingWeeks = ref<TeachingWeek[]>([])
const calendarDate = ref(new Date())
const saving = ref(false)
const eventFilter = ref('all')

// Tab布局相关
const activeTab = ref('semester')
const selectedSemester = ref<Semester | null>(null)

// 作息时间相关
const periodConfigSemester = ref(0)
const periodConfigs = ref<any[]>([])

// 年级活动相关
const grades = ref<any[]>([])
const gradeActivities = ref<any[]>([])
const selectedGradeFilter = ref<string | number>('all')
const activityTypeFilter = ref<number | null>(null)
const calendarViewMode = ref<'month' | 'list'>('month')

// 拖拽选择日期范围
const isDragging = ref(false)
const dragStartDate = ref<Date | null>(null)
const dragEndDate = ref<Date | null>(null)
const dragSelectedDates = ref<Set<string>>(new Set())

const yearDialogVisible = ref(false)
const semesterDialogVisible = ref(false)
const weeksDialogVisible = ref(false)
const eventDialogVisible = ref(false)
const periodConfigDialogVisible = ref(false)

// 作息时间表单
const periodConfigForm = ref({
  id: null as number | null,
  period: 1,
  name: '',
  startTime: '',
  endTime: '',
  semesterId: 0
})
const currentSemesterForWeeks = ref<Semester>()

const yearFormRef = ref<FormInstance>()
const semesterFormRef = ref<FormInstance>()
const eventFormRef = ref<FormInstance>()

const yearForm = ref<Partial<AcademicYear>>({})
const semesterForm = ref<Partial<Semester>>({})
// 事件表单扩展类型，添加前端专用的 isRange 字段
interface EventFormData extends Partial<AcademicEvent> {
  isRange?: boolean
}
const eventForm = ref<EventFormData>({})

// 模拟年级校历数据
const gradeCalendars = ref([
  {
    id: 1,
    name: '2024级',
    status: 'active',
    startDate: '2026-02-17',
    endDate: '2026-07-10',
    specialNote: '军训9月1-15日',
    events: [
      { id: 1, date: '02-17', name: '新生报到' },
      { id: 2, date: '09-01', name: '军训开始' },
    ]
  },
  {
    id: 2,
    name: '2023级',
    status: 'active',
    startDate: null,
    endDate: null,
    specialNote: null,
    events: [
      { id: 3, date: '06-20', name: '实习动员' },
    ]
  },
  {
    id: 3,
    name: '2022级',
    status: 'active',
    startDate: '2026-02-24',
    endDate: '2026-06-15',
    specialNote: '毕业设计答辩',
    events: [
      { id: 4, date: '05-20', name: '论文答辩' },
      { id: 5, date: '06-20', name: '毕业典礼' },
    ]
  },
])

// 事件类型简称
const getShortEventName = (type: number) => {
  const map: Record<number, string> = { 1: '开', 2: '假', 3: '考', 4: '活', 5: '他', 6: '补' }
  return map[type] || ''
}

const yearRules: FormRules = {
  name: [{ required: true, message: '请输入学年名称', trigger: 'blur' }],
  startDate: [{ required: true, message: '请选择开始日期', trigger: 'change' }],
  endDate: [{ required: true, message: '请选择结束日期', trigger: 'change' }],
}

const semesterRules: FormRules = {
  name: [{ required: true, message: '请输入学期名称', trigger: 'blur' }],
  termType: [{ required: true, message: '请选择学期类型', trigger: 'change' }],
  startDate: [{ required: true, message: '请选择开始日期', trigger: 'change' }],
  endDate: [{ required: true, message: '请选择结束日期', trigger: 'change' }],
  teachingWeeks: [{ required: true, message: '请输入教学周数', trigger: 'blur' }],
}

const eventRules: FormRules = {
  title: [{ required: true, message: '请输入事件标题', trigger: 'blur' }],
  eventType: [{ required: true, message: '请选择事件类型', trigger: 'change' }],
  startDate: [{ required: true, message: '请选择日期', trigger: 'change' }],
}

const currentSemester = computed(() => semesters.value.find(s => s.isCurrent))

// 获取学年的学期数量
const getYearSemesterCount = (yearId: number) => {
  return yearSemesterCounts.value.get(yearId) || 0
}

// 获取学年的事件数量
const getYearEventCount = (yearId: number) => {
  return yearEventCounts.value.get(yearId) || 0
}

// 进入学年详情
const enterYear = (year: AcademicYear) => {
  selectedYear.value = year
  currentYearId.value = year.id
  loadSemesters()
  loadEvents()
}

// 预加载学年统计数据
const loadYearStats = async () => {
  for (const year of academicYears.value) {
    try {
      // 加载学期数量
      const semRes = await semesterApi.list(year.id)
      const semData = semRes.data || semRes
      yearSemesterCounts.value.set(year.id, Array.isArray(semData) ? semData.length : 0)

      // 加载事件数量
      const eventRes = await academicEventApi.list({ yearId: year.id })
      let eventData: any[] = []
      if (Array.isArray(eventRes)) {
        eventData = eventRes
      } else if (eventRes && typeof eventRes === 'object') {
        eventData = (eventRes as any).data || (eventRes as any).records || []
      }
      yearEventCounts.value.set(year.id, eventData.length)
    } catch (error) {
      console.error(`Failed to load stats for year ${year.id}:`, error)
    }
  }
}

// ========== Tab 1: 学期管理相关函数 ==========
const selectSemester = (sem: Semester) => {
  selectedSemester.value = sem
  loadTeachingWeeks(sem.id)
}

const loadTeachingWeeks = async (semesterId: number) => {
  try {
    // TODO: 调用真实API
    // const res = await teachingWeekApi.list(semesterId)
    // teachingWeeks.value = res.data || res
    // 模拟数据
    teachingWeeks.value = generateMockWeeks(semesterId)
  } catch (error) {
    console.error('Failed to load teaching weeks:', error)
  }
}

const generateMockWeeks = (semesterId: number) => {
  const sem = semesters.value.find(s => s.id === semesterId)
  if (!sem) return []
  const weeks: TeachingWeek[] = []
  const start = new Date(sem.startDate)
  for (let i = 1; i <= sem.teachingWeeks; i++) {
    const weekStart = new Date(start)
    weekStart.setDate(start.getDate() + (i - 1) * 7)
    const weekEnd = new Date(weekStart)
    weekEnd.setDate(weekStart.getDate() + 6)
    weeks.push({
      id: i,
      semesterId,
      weekNumber: i,
      startDate: formatDateStr(weekStart),
      endDate: formatDateStr(weekEnd),
      weekType: i <= sem.teachingWeeks - 1 ? 1 : 2, // 最后一周是考试周
      remark: i === sem.teachingWeeks ? '考试周' : ''
    })
  }
  return weeks
}

const generateWeeks = () => {
  if (!selectedSemester.value) return
  ElMessageBox.confirm('是否自动生成教学周次？这将覆盖已有的周次数据。', '确认').then(() => {
    teachingWeeks.value = generateMockWeeks(selectedSemester.value!.id)
    ElMessage.success('教学周次已生成')
  }).catch(() => {})
}

const editWeek = (week: TeachingWeek) => {
  // TODO: 打开编辑周次对话框
  ElMessage.info('编辑周次功能开发中')
}

const getWeekTypeName = (type: number) => {
  const map: Record<number, string> = { 1: '教学周', 2: '考试周', 3: '假期', 4: '机动周' }
  return map[type] || '未知'
}

const getWeekTypeTag = (type: number) => {
  const map: Record<number, string> = { 1: 'success', 2: 'warning', 3: 'info', 4: '' }
  return map[type] || ''
}

// ========== Tab 3: 作息时间相关函数 ==========
const loadPeriodConfigs = () => {
  // 使用默认作息时间配置
  periodConfigs.value = [
    { period: 1, name: '第一节', startTime: '08:00', endTime: '08:45' },
    { period: 2, name: '第二节', startTime: '08:55', endTime: '09:40' },
    { period: 3, name: '第三节', startTime: '10:00', endTime: '10:45' },
    { period: 4, name: '第四节', startTime: '10:55', endTime: '11:40' },
    { period: 5, name: '第五节', startTime: '14:00', endTime: '14:45' },
    { period: 6, name: '第六节', startTime: '14:55', endTime: '15:40' },
    { period: 7, name: '第七节', startTime: '16:00', endTime: '16:45' },
    { period: 8, name: '第八节', startTime: '16:55', endTime: '17:40' },
    { period: 9, name: '第九节', startTime: '19:00', endTime: '19:45' },
    { period: 10, name: '第十节', startTime: '19:55', endTime: '20:40' },
  ]
}

const showPeriodConfigDialog = (row?: any) => {
  if (row) {
    // 编辑模式
    periodConfigForm.value = { ...row, semesterId: periodConfigSemester.value }
  } else {
    // 新增模式 - 计算下一个节次
    const maxPeriod = periodConfigs.value.length > 0
      ? Math.max(...periodConfigs.value.map(p => p.period))
      : 0
    periodConfigForm.value = {
      id: null,
      period: maxPeriod + 1,
      name: `第${maxPeriod + 1}节`,
      startTime: '',
      endTime: '',
      semesterId: periodConfigSemester.value
    }
  }
  periodConfigDialogVisible.value = true
}

const editPeriodConfig = (row: any) => {
  showPeriodConfigDialog(row)
}

const savePeriodConfig = () => {
  const form = periodConfigForm.value
  if (!form.startTime || !form.endTime) {
    ElMessage.warning('请填写开始时间和结束时间')
    return
  }

  if (form.id) {
    // 编辑模式 - 更新现有记录
    const index = periodConfigs.value.findIndex(p => p.id === form.id)
    if (index !== -1) {
      periodConfigs.value[index] = { ...form }
    }
  } else {
    // 新增模式
    periodConfigs.value.push({
      ...form,
      id: Date.now() // 临时ID
    })
    // 按节次排序
    periodConfigs.value.sort((a, b) => a.period - b.period)
  }

  periodConfigDialogVisible.value = false
  ElMessage.success(form.id ? '修改成功' : '添加成功')
}

const deletePeriodConfig = (row: any) => {
  ElMessageBox.confirm(`确定要删除"${row.name}"吗？`, '确认删除', {
    type: 'warning'
  }).then(() => {
    periodConfigs.value = periodConfigs.value.filter(p => p.id !== row.id)
    ElMessage.success('删除成功')
  }).catch(() => {})
}

// 计算时长（分钟）
const calculateDuration = (startTime: string, endTime: string) => {
  if (!startTime || !endTime) return 0
  const [sh, sm] = startTime.split(':').map(Number)
  const [eh, em] = endTime.split(':').map(Number)
  return (eh * 60 + em) - (sh * 60 + sm)
}

// ========== Tab 4: 年级活动相关函数 ==========
const loadGrades = () => {
  // 模拟年级数据
  grades.value = [
    { id: 1, name: '2024级' },
    { id: 2, name: '2023级' },
    { id: 3, name: '2022级' },
    { id: 4, name: '2021级' },
  ]
}

const loadGradeActivities = () => {
  // 模拟年级活动数据
  gradeActivities.value = [
    { id: 1, gradeId: 1, gradeName: '2024级', activityName: '新生军训', activityType: 1, startDate: '2024-09-01', endDate: '2024-09-14', startWeek: 1, endWeek: 2, affectTeaching: true },
    { id: 2, gradeId: 1, gradeName: '2024级', activityName: '入学教育', activityType: 2, startDate: '2024-09-15', endDate: '2024-09-15', startWeek: 3, endWeek: 3, affectTeaching: false },
    { id: 3, gradeId: 3, gradeName: '2022级', activityName: '毕业实习', activityType: 5, startDate: '2025-03-01', endDate: '2025-05-31', startWeek: 1, endWeek: 12, affectTeaching: true },
    { id: 4, gradeId: 3, gradeName: '2022级', activityName: '毕业答辩', activityType: 7, startDate: '2025-06-01', endDate: '2025-06-15', startWeek: 15, endWeek: 16, affectTeaching: true },
  ]
}

const getGradeActivityCount = (gradeId: number) => {
  return gradeActivities.value.filter(a => a.gradeId === gradeId).length
}

const filteredGradeActivities = computed(() => {
  let result = gradeActivities.value
  if (selectedGradeFilter.value !== 'all') {
    result = result.filter(a => a.gradeId === selectedGradeFilter.value)
  }
  if (activityTypeFilter.value) {
    result = result.filter(a => a.activityType === activityTypeFilter.value)
  }
  return result
})

const getActivityTypeName = (type: number) => {
  const map: Record<number, string> = {
    1: '军训', 2: '入学教育', 3: '专业实习', 4: '社会实践',
    5: '毕业实习', 6: '毕业设计', 7: '毕业答辩', 8: '毕业典礼',
    9: '就业指导', 10: '其他'
  }
  return map[type] || '其他'
}

const showGradeActivityDialog = () => {
  ElMessage.info('添加年级活动对话框开发中')
}

const editGradeActivity = (row: any) => {
  ElMessage.info('编辑年级活动开发中')
}

const deleteGradeActivity = (row: any) => {
  ElMessageBox.confirm('确定要删除该年级活动吗？', '确认').then(() => {
    gradeActivities.value = gradeActivities.value.filter(a => a.id !== row.id)
    ElMessage.success('删除成功')
  }).catch(() => {})
}

const currentWeekNumber = computed(() => {
  if (!currentSemester.value) return null
  const start = new Date(currentSemester.value.startDate)
  const today = new Date()
  const diff = Math.floor((today.getTime() - start.getTime()) / (7 * 24 * 60 * 60 * 1000))
  return diff >= 0 && diff < (currentSemester.value.teachingWeeks || 18) ? diff + 1 : null
})

const daysRemaining = computed(() => {
  if (!currentSemester.value) return 0
  const end = new Date(currentSemester.value.endDate)
  const diff = Math.ceil((end.getTime() - Date.now()) / 86400000)
  return diff > 0 ? diff : 0
})

const semesterProgress = computed(() => {
  if (!currentSemester.value) return 0
  const start = new Date(currentSemester.value.startDate).getTime()
  const end = new Date(currentSemester.value.endDate).getTime()
  const now = Date.now()
  const progress = Math.round(((now - start) / (end - start)) * 100)
  return Math.max(0, Math.min(100, progress))
})

const currentMonthText = computed(() => {
  const y = calendarDate.value.getFullYear()
  const m = calendarDate.value.getMonth() + 1
  return `${y}年${m}月`
})

const calendarDays = computed(() => {
  const year = calendarDate.value.getFullYear()
  const month = calendarDate.value.getMonth()
  const firstDay = new Date(year, month, 1)
  const startDate = new Date(firstDay)
  startDate.setDate(startDate.getDate() - firstDay.getDay())

  const days = []
  const today = new Date()
  today.setHours(0, 0, 0, 0)

  for (let i = 0; i < 42; i++) {
    const date = new Date(startDate)
    date.setDate(startDate.getDate() + i)
    const dateStr = formatDateStr(date)
    // 支持跨天事件：检查日期是否在事件的开始和结束日期之间
    const dayEvents = events.value.filter(e => {
      const eventStart = e.startDate?.substring(0, 10)
      const eventEnd = e.endDate?.substring(0, 10) || eventStart
      return dateStr >= eventStart && dateStr <= eventEnd
    })

    days.push({
      date,
      day: date.getDate(),
      isCurrentMonth: date.getMonth() === month,
      isToday: date.getTime() === today.getTime(),
      isWeekend: date.getDay() === 0 || date.getDay() === 6,
      events: dayEvents,
    })
  }
  return days
})

// 月视图按周分组，带周次
const calendarWeeks = computed(() => {
  const days = calendarDays.value
  const weeks = []

  // 计算学期开始日期用于计算周次
  let semesterStart: Date | null = null
  let semesterEnd: Date | null = null
  let semesterStartWeekSunday: Date | null = null

  if (currentSemester.value?.startDate) {
    semesterStart = new Date(currentSemester.value.startDate)
    semesterStart.setHours(0, 0, 0, 0)
    // 计算学期开始那周的周日
    semesterStartWeekSunday = new Date(semesterStart)
    semesterStartWeekSunday.setDate(semesterStartWeekSunday.getDate() - semesterStartWeekSunday.getDay())
  }
  if (currentSemester.value?.endDate) {
    semesterEnd = new Date(currentSemester.value.endDate)
    semesterEnd.setHours(23, 59, 59, 999)
  }

  for (let i = 0; i < 6; i++) {
    const weekDays = days.slice(i * 7, (i + 1) * 7)
    let weekNumber: number | null = null

    // 计算这一周的周次
    if (semesterStart && semesterEnd && semesterStartWeekSunday && weekDays.length > 0) {
      // 取这周的周日作为基准
      const thisWeekSunday = weekDays[0].date

      // 检查这周是否与学期有交集
      const weekEnd = weekDays[6].date
      const weekOverlapsSemester = thisWeekSunday <= semesterEnd && weekEnd >= semesterStart

      if (weekOverlapsSemester) {
        // 计算周次：当前周日与学期开始周日的差距
        const weeksDiff = Math.round((thisWeekSunday.getTime() - semesterStartWeekSunday.getTime()) / (7 * 24 * 60 * 60 * 1000))
        weekNumber = weeksDiff + 1
        // 确保周次为正数
        if (weekNumber < 1) weekNumber = null
      }
    }

    weeks.push({
      weekNumber,
      days: weekDays,
    })
  }

  return weeks
})

const filteredEvents = computed(() => {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  let list = [...events.value]
  if (eventFilter.value === 'upcoming') {
    list = list.filter(e => new Date(e.startDate) >= today)
  }
  return list.sort((a, b) => new Date(a.startDate).getTime() - new Date(b.startDate).getTime())
})

// 学期事件列表（按日期排序，用于列表视图）
const semesterEvents = computed(() => {
  return [...events.value].sort((a, b) => new Date(a.startDate).getTime() - new Date(b.startDate).getTime())
})

// 学期周次表格数据 - 按月份正确分行
const semesterWeeksData = computed(() => {
  if (!currentSemester.value) return []

  const startDate = new Date(currentSemester.value.startDate)
  const endDate = new Date(currentSemester.value.endDate)
  const today = new Date()
  today.setHours(0, 0, 0, 0)

  const rows: Array<{
    weekNumber: number | string
    monthLabel: string
    monthRowSpan: number
    isMonthLastRow: boolean
    days: Array<{ date: Date | null; day: number | null; isToday: boolean; events: AcademicEvent[] }>
  }> = []

  // 计算学期第一周的周日
  const semesterWeekStart = new Date(startDate)
  semesterWeekStart.setDate(semesterWeekStart.getDate() - semesterWeekStart.getDay())

  let currentDate = new Date(startDate)

  while (currentDate <= endDate) {
    const rowMonth = currentDate.getMonth()
    const dayOfWeek = currentDate.getDay() // 0=周日

    // 计算当前是学期第几周
    const daysSinceStart = Math.floor((currentDate.getTime() - semesterWeekStart.getTime()) / (24 * 60 * 60 * 1000))
    const weekNum = Math.floor(daysSinceStart / 7) + 1

    // 创建新行
    const days = []

    // 如果不是从周日开始，前面填充空白
    for (let d = 0; d < dayOfWeek; d++) {
      days.push({ date: null, day: null, isToday: false, events: [] as AcademicEvent[] })
    }

    // 记录这一行的周次范围
    let rowWeekStart = weekNum
    let rowWeekEnd = weekNum

    // 填充本月剩余日期（同一行内只显示同一个月）
    while (days.length < 7 && currentDate <= endDate) {
      const date = new Date(currentDate)
      const dateStr = formatDateStr(date)

      // 如果月份变了，停止当前行
      if (date.getMonth() !== rowMonth) {
        break
      }

      // 更新周次范围
      const currDaysSince = Math.floor((date.getTime() - semesterWeekStart.getTime()) / (24 * 60 * 60 * 1000))
      rowWeekEnd = Math.floor(currDaysSince / 7) + 1

      // 获取当天事件
      const dayEvents = events.value.filter(e => {
        const eventStart = e.startDate?.substring(0, 10)
        const eventEnd = e.endDate?.substring(0, 10) || eventStart
        return dateStr >= eventStart && dateStr <= eventEnd
      })

      days.push({
        date,
        day: date.getDate(),
        isToday: date.getTime() === today.getTime(),
        events: dayEvents,
      })

      currentDate.setDate(currentDate.getDate() + 1)
    }

    // 如果行未满，后面填充空白
    while (days.length < 7) {
      days.push({ date: null, day: null, isToday: false, events: [] as AcademicEvent[] })
    }

    // 添加行，显示周次范围
    rows.push({
      weekNumber: rowWeekStart === rowWeekEnd ? rowWeekStart : `${rowWeekStart}-${rowWeekEnd}`,
      monthLabel: (rowMonth + 1) + '月',
      monthRowSpan: 0,
      isMonthLastRow: false,
      days,
    })
  }

  // 第二遍：计算月份合并行数
  let i = 0
  while (i < rows.length) {
    const month = rows[i].monthLabel
    let span = 1

    while (i + span < rows.length && rows[i + span].monthLabel === month) {
      rows[i + span].monthRowSpan = 0
      span++
    }

    rows[i].monthRowSpan = span
    rows[i + span - 1].isMonthLastRow = true
    i += span
  }

  return rows
})

// 月视图拖拽选择开始
const handleCalendarDragStart = (day: { date: Date; events: AcademicEvent[] }, e: MouseEvent) => {
  if (!day.date) return
  isDragging.value = true
  dragStartDate.value = new Date(day.date)
  dragEndDate.value = new Date(day.date)
  updateDragSelection()
}

// 月视图拖拽选择中
const handleCalendarDragMove = (day: { date: Date }) => {
  if (!isDragging.value || !day.date) return
  dragEndDate.value = new Date(day.date)
  updateDragSelection()
}

// 学期总览拖拽选择开始
const handleDragStart = (day: { date: Date | null; events: AcademicEvent[] }, e: MouseEvent) => {
  if (!day.date) return
  isDragging.value = true
  dragStartDate.value = new Date(day.date)
  dragEndDate.value = new Date(day.date)
  updateDragSelection()
}

// 学期总览拖拽选择中
const handleDragMove = (day: { date: Date | null }) => {
  if (!isDragging.value || !day.date) return
  dragEndDate.value = new Date(day.date)
  updateDragSelection()
}

// 拖拽选择结束
const handleDragEnd = () => {
  if (!isDragging.value) return
  isDragging.value = false

  if (dragStartDate.value && dragEndDate.value) {
    const start = dragStartDate.value.getTime() < dragEndDate.value.getTime() ? dragStartDate.value : dragEndDate.value
    const end = dragStartDate.value.getTime() < dragEndDate.value.getTime() ? dragEndDate.value : dragStartDate.value

    // 打开表单并自动填充日期
    eventForm.value = {
      yearId: currentYearId.value,
      startDate: formatDateStr(start),
      endDate: formatDateStr(end),
      allDay: true,
      eventType: 4,
      isRange: start.getTime() !== end.getTime(),
    }
    eventDialogVisible.value = true
  }

  // 清除选择状态
  dragStartDate.value = null
  dragEndDate.value = null
  dragSelectedDates.value = new Set()
}

// 更新拖拽选中的日期集合
const updateDragSelection = () => {
  if (!dragStartDate.value || !dragEndDate.value) {
    dragSelectedDates.value = new Set()
    return
  }

  const start = dragStartDate.value.getTime() < dragEndDate.value.getTime() ? dragStartDate.value : dragEndDate.value
  const end = dragStartDate.value.getTime() < dragEndDate.value.getTime() ? dragEndDate.value : dragStartDate.value

  const newSet = new Set<string>()
  const current = new Date(start)
  while (current.getTime() <= end.getTime()) {
    newSet.add(formatDateStr(current))
    current.setDate(current.getDate() + 1)
  }
  dragSelectedDates.value = newSet
}

// 检查日期是否在拖拽选择范围内
const isDayInDragSelection = (date: Date | null) => {
  if (!date) return false
  return dragSelectedDates.value.has(formatDateStr(date))
}

// 总览视图点击处理（单击查看已有事件）
const handleOverviewDayClick = (day: { date: Date | null; events: AcademicEvent[] }) => {
  if (!day.date || isDragging.value) return
  if (day.events.length === 1) {
    showEventDialog(day.events[0])
  }
}

// 格式化事件日期显示
const formatEventDate = (dateStr: string) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return `${date.getMonth() + 1}月${date.getDate()}日`
}

const formatDateStr = (date: Date) => {
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

const loadAcademicYears = async () => {
  try {
    const res = await academicYearApi.list()
    const data = Array.isArray(res) ? res : (res.data || [])
    // 后端返回 yearName，前端类型定义用 name，做字段映射
    academicYears.value = data.map((y: any) => ({
      ...y,
      name: y.yearName || y.name,
    }))

    // 预加载学年统计数据（用于学年列表界面显示）
    loadYearStats()
  } catch (error) {
    console.error('Failed to load academic years:', error)
  }
}

const loadSemesters = async () => {
  if (!currentYearId.value) return
  try {
    const res = await semesterApi.list(currentYearId.value)
    const data = res.data || res
    // 后端字段映射：semesterName→name, semesterType→termType, weekCount→teachingWeeks
    semesters.value = data.map((s: any) => ({
      ...s,
      name: s.semesterName || s.name,
      termType: s.semesterType || s.termType,
      teachingWeeks: s.weekCount || s.teachingWeeks,
    }))
  } catch (error) {
    console.error('Failed to load semesters:', error)
  }
}

const loadEvents = async () => {
  if (!currentYearId.value) return
  try {
    const res = await academicEventApi.list({ yearId: currentYearId.value })

    // 处理响应数据
    let data: any[] = []
    if (Array.isArray(res)) {
      data = res
    } else if (res && typeof res === 'object') {
      data = (res as any).data || (res as any).records || []
    }

    if (!data || data.length === 0) {
      events.value = []
      return
    }

    // 后端返回 eventName，前端使用 title，做字段映射
    events.value = data.map((e: any) => ({
      id: Number(e.id) || 0,
      yearId: Number(e.yearId),
      semesterId: Number(e.semesterId),
      title: String(e.eventName || e.title || '未命名事件'),
      eventType: Number(e.eventType) || 5,
      startDate: String(e.startDate || ''),
      endDate: e.endDate ? String(e.endDate) : undefined,
      allDay: true,
      description: e.description ? String(e.description) : undefined,
    }))
  } catch (error) {
    console.error('Failed to load events:', error)
  }
}

const prevMonth = () => {
  const d = new Date(calendarDate.value)
  d.setMonth(d.getMonth() - 1)
  calendarDate.value = d
}

const nextMonth = () => {
  const d = new Date(calendarDate.value)
  d.setMonth(d.getMonth() + 1)
  calendarDate.value = d
}

const goToday = () => {
  calendarDate.value = new Date()
}

const handleDayClick = (day: { date: Date; events: AcademicEvent[] }) => {
  // 如果刚刚完成拖拽选择，不处理点击
  if (dragSelectedDates.value.size > 1) return

  if (day.events.length === 1) {
    showEventDialog(day.events[0])
  } else if (day.events.length === 0 && dragSelectedDates.value.size === 0) {
    eventForm.value = {
      yearId: currentYearId.value,
      startDate: formatDateStr(day.date),
      allDay: true,
      eventType: 4,
      isRange: false,
    }
    eventDialogVisible.value = true
  }
}

const showYearDialog = (year?: AcademicYear) => {
  yearForm.value = year ? { ...year } : {}
  yearDialogVisible.value = true
}

const saveYear = async () => {
  try {
    await yearFormRef.value?.validate()
  } catch {
    ElMessage.warning('请填写完整的学年信息')
    return
  }
  saving.value = true
  try {
    // 前端使用 name，后端使用 yearName，做字段映射
    const payload = {
      ...yearForm.value,
      yearName: yearForm.value.name,
    }
    if (yearForm.value.id) {
      await academicYearApi.update(yearForm.value.id, payload)
    } else {
      await academicYearApi.create(payload)
    }
    ElMessage.success('保存成功')
    yearDialogVisible.value = false
    loadAcademicYears()
  } catch (error) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

const showSemesterDialog = (semester?: Semester) => {
  semesterForm.value = semester ? { ...semester } : { yearId: currentYearId.value, teachingWeeks: 18 }
  semesterDialogVisible.value = true
}

const saveSemester = async () => {
  try {
    await semesterFormRef.value?.validate()
  } catch {
    ElMessage.warning('请填写完整的学期信息')
    return
  }
  saving.value = true
  try {
    // 前端字段映射到后端：name→semesterName, termType→semesterType, teachingWeeks→weekCount
    const payload = {
      ...semesterForm.value,
      semesterName: semesterForm.value.name,
      semesterType: semesterForm.value.termType,
      weekCount: semesterForm.value.teachingWeeks,
    }
    if (semesterForm.value.id) {
      await semesterApi.update(semesterForm.value.id, payload)
    } else {
      await semesterApi.create(payload)
    }
    ElMessage.success('保存成功')
    semesterDialogVisible.value = false
    loadSemesters()
  } catch (error) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

const setCurrentSemester = async (id: number) => {
  try {
    await semesterApi.setCurrent(id)
    ElMessage.success('设置成功')
    loadSemesters()
  } catch (error) {
    ElMessage.error('设置失败')
  }
}

const showWeeksDialog = async (semester: Semester) => {
  currentSemesterForWeeks.value = semester
  try {
    const res = await semesterApi.getWeeks(semester.id)
    teachingWeeks.value = res.data || res
  } catch (error) {
    console.error('Failed to load weeks:', error)
  }
  weeksDialogVisible.value = true
}

const showEventDialog = (event?: AcademicEvent) => {
  if (event) {
    eventForm.value = {
      ...event,
      isRange: !!(event.endDate && event.endDate !== event.startDate)
    }
  } else {
    eventForm.value = { yearId: currentYearId.value, allDay: true, eventType: 4, isRange: false }
  }
  eventDialogVisible.value = true
}

const saveEvent = async () => {
  try {
    await eventFormRef.value?.validate()
  } catch {
    ElMessage.warning('请填写完整的事件信息')
    return
  }

  saving.value = true
  try {
    // 前端使用 title，后端使用 eventName，做字段映射
    const payload = {
      ...eventForm.value,
      eventName: eventForm.value.title,
      // 如果不是持续事件，清空结束日期
      endDate: eventForm.value.isRange ? eventForm.value.endDate : eventForm.value.startDate,
    }
    // 移除前端专用的 isRange 字段
    delete (payload as any).isRange

    if (eventForm.value.id) {
      await academicEventApi.update(eventForm.value.id, payload)
    } else {
      await academicEventApi.create(payload)
    }
    ElMessage.success('保存成功')
    eventDialogVisible.value = false
    loadEvents()
  } catch (error) {
    console.error('Save event error:', error)
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

const deleteEvent = async () => {
  if (!eventForm.value.id) return
  await ElMessageBox.confirm('确定删除该事件吗？', '提示', { type: 'warning' })
  try {
    await academicEventApi.delete(eventForm.value.id)
    ElMessage.success('删除成功')
    eventDialogVisible.value = false
    loadEvents()
  } catch (error) {
    ElMessage.error('删除失败')
  }
}

const showGradeCalendarDialog = () => {
  ElMessage.info('年级校历配置功能开发中')
}

const getEventDay = (dateStr: string) => new Date(dateStr).getDate()
const getEventMonth = (dateStr: string) => (new Date(dateStr).getMonth() + 1) + '月'
const getEventTagType = (type: number) => {
  const map: Record<number, '' | 'success' | 'warning' | 'danger' | 'info'> = {
    1: 'success', 2: 'warning', 3: 'danger', 4: '', 5: 'info', 6: 'warning'
  }
  return map[type] || 'info'
}
const getEventTypeName = (type: number) => {
  const map: Record<number, string> = { 1: '开学', 2: '放假', 3: '考试', 4: '活动', 5: '其他', 6: '补课' }
  return map[type] || '其他'
}
const getEventCountdown = (dateStr: string) => {
  const diff = Math.ceil((new Date(dateStr).getTime() - Date.now()) / 86400000)
  if (diff < 0) return '已过'
  if (diff === 0) return '今天'
  if (diff === 1) return '明天'
  return `${diff}天后`
}

onMounted(() => {
  loadAcademicYears()
  loadPeriodConfigs()
  loadGrades()
  loadGradeActivities()
})
</script>

<style scoped lang="scss">
.calendar-page {
  padding: 16px;
  background: #f0f2f5;
  min-height: calc(100vh - 60px);
}

.page-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;

  .toolbar-left {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  .page-title {
    margin: 0;
    font-size: 18px;
    font-weight: 600;
  }

  .toolbar-right {
    display: flex;
    gap: 8px;
  }
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-bottom: 12px;

  .stat-card {
    background: #fff;
    border-radius: 8px;
    padding: 12px 16px;
    border: 1px solid #e5e6e8;

    .stat-label {
      font-size: 12px;
      color: #86909c;
      margin-bottom: 4px;
    }

    .stat-value {
      font-size: 16px;
      font-weight: 600;
      color: #1d2129;
      display: flex;
      align-items: center;
    }
  }
}

.panel {
  background: #fff;
  border-radius: 8px;
  border: 1px solid #e5e6e8;

  &.full-height {
    height: calc(100vh - 220px);
    display: flex;
    flex-direction: column;
  }

  .panel-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 10px 12px;
    border-bottom: 1px solid #e5e6e8;

    .panel-title {
      font-size: 14px;
      font-weight: 600;
    }

    .month-nav {
      display: flex;
      align-items: center;
      gap: 4px;

      .month-text {
        font-size: 14px;
        font-weight: 500;
        min-width: 80px;
        text-align: center;
      }
    }
  }
}

// 紧凑日历
.mini-calendar {
  padding: 8px;
  user-select: none;

  &.is-dragging {
    cursor: crosshair;

    .cal-day {
      cursor: crosshair;
    }
  }

  .cal-weekdays {
    display: grid;
    grid-template-columns: 28px repeat(7, 1fr);
    margin-bottom: 4px;

    span {
      text-align: center;
      font-size: 11px;
      color: #86909c;
      padding: 4px 0;
    }

    .week-col {
      font-size: 10px;
      color: #c0c4cc;
    }
  }

  .cal-weeks {
    display: flex;
    flex-direction: column;
    gap: 3px;
  }

  .cal-week-row {
    display: grid;
    grid-template-columns: 28px repeat(7, 1fr);
    gap: 3px;
  }

  .week-num {
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 10px;
    color: #c0c4cc;
    background: #fafafa;
    border-radius: 4px;
  }

  .drag-hint-bar {
    text-align: center;
    font-size: 11px;
    color: #999;
    padding: 6px 0 2px;
    transition: opacity 0.2s;

    &.hidden {
      opacity: 0;
    }
  }

  .cal-day {
    aspect-ratio: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    font-size: 12px;
    cursor: pointer;
    border-radius: 4px;
    position: relative;
    background: #fafafa;
    transition: all 0.15s;

    &:hover {
      background: #e8f4ff;
    }

    &.other {
      color: #c9cdd4;
      background: transparent;
    }

    &.today {
      background: #165dff;
      color: #fff;

      .event-label {
        color: rgba(255,255,255,0.9);
      }
    }

    &.weekend:not(.today):not(.has-event):not(.drag-selected) {
      color: #f53f3f;
    }

    // 拖拽选中样式
    &.drag-selected {
      background: linear-gradient(135deg, #69b1ff 0%, #4096ff 100%) !important;
      color: #fff !important;
      transform: scale(1.02);
      box-shadow: 0 2px 8px rgba(64, 150, 255, 0.4);
      z-index: 1;

      .day-num {
        color: #fff;
        font-weight: 600;
      }

      .event-label {
        color: rgba(255,255,255,0.9);
      }
    }

    // 有事件的日期 - 使用明显的背景色
    &.has-event {
      font-weight: 600;

      .day-num {
        margin-bottom: 1px;
      }
    }

    &.event-type-1 { // 开学 - 绿色
      background: linear-gradient(135deg, #d4f5e0 0%, #b8ecc8 100%);
      border-left: 3px solid #00b42a;
      color: #0a6b22;
    }

    &.event-type-2 { // 放假 - 橙色
      background: linear-gradient(135deg, #fff0e0 0%, #ffe4c7 100%);
      border-left: 3px solid #ff7d00;
      color: #a35000;
    }

    &.event-type-3 { // 考试 - 红色
      background: linear-gradient(135deg, #ffe8e8 0%, #ffcfcf 100%);
      border-left: 3px solid #f53f3f;
      color: #a31515;
    }

    &.event-type-4 { // 活动 - 蓝色
      background: linear-gradient(135deg, #e8f3ff 0%, #d1e6ff 100%);
      border-left: 3px solid #165dff;
      color: #0d4eb3;
    }

    &.event-type-5 { // 其他 - 灰色
      background: linear-gradient(135deg, #f2f3f5 0%, #e5e6e8 100%);
      border-left: 3px solid #86909c;
      color: #4e5969;
    }

    &.event-type-6 { // 补课 - 紫色
      background: linear-gradient(135deg, #f5e8ff 0%, #e8d4ff 100%);
      border-left: 3px solid #722ed1;
      color: #531dab;
    }

    .day-num {
      line-height: 1;
    }

    .event-label {
      font-size: 9px;
      line-height: 1;
      font-weight: 500;
    }
  }
}

// 学期总览视图样式 - 简洁现代风格
.semester-overview {
  &.is-dragging {
    cursor: crosshair;

    .col-day {
      cursor: crosshair;
    }
  }

  .overview-header {
    padding: 12px 16px;
    font-size: 13px;
    font-weight: 500;
    color: #333;
    background: #fff;
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;

    > span:first-child {
      display: flex;
      align-items: center;
      gap: 8px;

      &::before {
        content: '';
        width: 3px;
        height: 14px;
        background: #1890ff;
        border-radius: 2px;
      }
    }

    .drag-hint {
      font-size: 11px;
      font-weight: 400;
      color: #999;
      background: #fafafa;
      padding: 4px 8px;
      border-radius: 4px;
    }
  }

  .overview-table-wrapper {
    max-height: 340px;
    overflow: auto;
    position: relative;
    margin: 0 8px 8px;
    border-radius: 6px;
    border: 1px solid #f0f0f0;
  }

  .overview-table {
    width: 100%;
    border-collapse: collapse;
    font-size: 12px;
    background: #fff;
    user-select: none;
    -webkit-user-select: none;
    -moz-user-select: none;
    -ms-user-select: none;

    th, td {
      padding: 0;
      text-align: center;
      vertical-align: middle;
    }

    thead {
      position: sticky;
      top: 0;
      z-index: 10;

      th {
        background: #fafafa;
        font-weight: 500;
        color: #666;
        padding: 10px 4px;
        border-bottom: 1px solid #e8e8e8;
        font-size: 12px;
      }
    }

    tbody tr {
      border-bottom: 1px solid #f5f5f5;

      &:hover {
        background: #fafafa;
      }
    }

    .col-month {
      width: 40px;
      font-size: 12px;
      font-weight: 600;
      color: #1890ff;
      background: #f0f7ff;
      padding: 8px 4px;
      border-right: 1px solid #e8e8e8;
    }

    .col-week {
      width: 36px;
      font-weight: 400;
      font-size: 11px;
      color: #999;
      background: #fafafa;
      padding: 8px 4px;
    }

    .col-day {
      width: calc((100% - 76px) / 7);
      height: 44px;
      cursor: pointer;
      position: relative;
      transition: all 0.2s;
      background: #fff;
      color: #333;
      padding: 4px 2px;
      user-select: none;

      &:hover {
        background: #e6f7ff;
      }

      &.drag-selected {
        background: linear-gradient(135deg, #69b1ff 0%, #4096ff 100%) !important;

        .day-num {
          color: #fff !important;
          font-weight: 600;
        }

        .day-event {
          background: rgba(255,255,255,0.3) !important;
          color: #fff !important;
        }
      }

      &.empty-cell {
        cursor: default;
        background: #fafafa;

        &:hover {
          background: #fafafa;
        }
      }

      .day-num {
        display: block;
        font-size: 13px;
        font-weight: 400;
        line-height: 1.5;
        color: #333;
      }

      .day-event {
        display: block;
        font-size: 9px;
        line-height: 1.2;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
        padding: 1px 2px;
        border-radius: 2px;
        margin-top: 2px;
      }

      &.today {
        .day-num {
          background: #1890ff;
          color: #fff;
          width: 24px;
          height: 24px;
          line-height: 24px;
          border-radius: 50%;
          margin: 0 auto;
        }
      }

      // 事件类型 - 使用小圆点+柔和背景
      &.event-type-1 { // 开学 - 薄荷绿
        background: #f6ffed;
        .day-event { background: #b7eb8f; color: #389e0d; }
      }

      &.event-type-2 { // 放假 - 暖橙
        background: #fff7e6;
        .day-event { background: #ffd591; color: #d46b08; }
      }

      &.event-type-3 { // 考试 - 珊瑚红
        background: #fff1f0;
        .day-event { background: #ffa39e; color: #cf1322; }
      }

      &.event-type-4 { // 活动 - 天蓝
        background: #e6f7ff;
        .day-event { background: #91d5ff; color: #096dd9; }
      }

      &.event-type-5 { // 其他 - 淡灰
        background: #f5f5f5;
        .day-event { background: #d9d9d9; color: #595959; }
      }

      &.event-type-6 { // 补课 - 淡紫
        background: #f9f0ff;
        .day-event { background: #d3adf7; color: #531dab; }
      }
    }

    // 月份分隔 - 使用更明显的间距
    tr.month-last-row {
      td {
        border-bottom: 3px solid #f0f0f0;
      }
    }
  }
}

// 标题滚动动画 - 鼠标悬停时滚动
.event-label {
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.cal-day:hover .scroll-text {
  animation: scroll-left 2s linear infinite;
  overflow: visible;
}

@keyframes scroll-left {
  0%, 20% { transform: translateX(0); }
  80%, 100% { transform: translateX(-30%); }
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 4px;
}

.cal-legend {
  display: flex;
  justify-content: center;
  gap: 16px;
  padding: 8px;
  border-top: 1px solid #e5e6e8;
  font-size: 11px;
  color: #4e5969;

  .legend-item {
    display: flex;
    align-items: center;
    gap: 4px;

    .legend-bar {
      display: inline-block;
      width: 12px;
      height: 12px;
      border-radius: 2px;
      border-left: 3px solid;
    }

    &.t1 .legend-bar {
      background: linear-gradient(135deg, #d4f5e0 0%, #b8ecc8 100%);
      border-color: #00b42a;
    }

    &.t2 .legend-bar {
      background: linear-gradient(135deg, #fff0e0 0%, #ffe4c7 100%);
      border-color: #ff7d00;
    }

    &.t3 .legend-bar {
      background: linear-gradient(135deg, #ffe8e8 0%, #ffcfcf 100%);
      border-color: #f53f3f;
    }

    &.t4 .legend-bar {
      background: linear-gradient(135deg, #e8f3ff 0%, #d1e6ff 100%);
      border-color: #165dff;
    }

    &.t6 .legend-bar {
      background: linear-gradient(135deg, #f5e8ff 0%, #e8d4ff 100%);
      border-color: #722ed1;
    }
  }
}


// 事件列表
.event-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.event-card {
  display: flex;
  gap: 10px;
  padding: 10px;
  border-radius: 6px;
  margin-bottom: 8px;
  cursor: pointer;
  transition: background 0.2s;

  &:hover {
    background: #f7f8fa;
  }

  .event-date {
    width: 40px;
    height: 40px;
    border-radius: 6px;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    color: #fff;
    flex-shrink: 0;

    &.type-1 { background: #00b42a; }
    &.type-2 { background: #ff7d00; }
    &.type-3 { background: #f53f3f; }
    &.type-4 { background: #165dff; }
    &.type-5 { background: #86909c; }
    &.type-6 { background: #722ed1; }

    .day {
      font-size: 14px;
      font-weight: 600;
      line-height: 1;
    }

    .month {
      font-size: 10px;
    }
  }

  .event-body {
    flex: 1;
    min-width: 0;
  }

  .event-title {
    font-size: 13px;
    font-weight: 500;
    margin-bottom: 4px;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .event-desc {
    display: flex;
    align-items: center;
    gap: 8px;

    .countdown, .date-range {
      font-size: 11px;
      color: #86909c;
    }

    .date-range {
      color: #165dff;
    }
  }
}

// 年级校历
.grade-calendar-list {
  padding: 8px;
  max-height: 280px;
  overflow-y: auto;
}

.grade-card {
  background: #f7f8fa;
  border-radius: 6px;
  padding: 10px;
  margin-bottom: 8px;

  &:last-child {
    margin-bottom: 0;
  }

  .grade-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 8px;

    .grade-name {
      font-size: 13px;
      font-weight: 600;
    }
  }

  .grade-info {
    .info-item {
      display: flex;
      justify-content: space-between;
      font-size: 11px;
      padding: 2px 0;

      .label {
        color: #86909c;
      }

      .value {
        color: #1d2129;
      }
    }
  }

  .grade-events {
    margin-top: 8px;
    padding-top: 8px;
    border-top: 1px dashed #e5e6e8;

    .events-title {
      font-size: 11px;
      color: #86909c;
      margin-bottom: 4px;
    }

    .mini-event {
      display: flex;
      gap: 8px;
      font-size: 11px;
      padding: 2px 0;

      .evt-date {
        color: #165dff;
        font-weight: 500;
      }

      .evt-name {
        color: #1d2129;
      }
    }
  }
}

// 自定义对话框样式
:deep(.custom-dialog) {
  .el-dialog__header {
    padding: 20px 24px 16px;
    margin: 0;
    border-bottom: 1px solid #f0f0f0;
  }

  .el-dialog__title {
    font-size: 16px;
    font-weight: 600;
    color: #1d2129;
  }

  .el-dialog__body {
    padding: 20px 24px;
  }

  .el-dialog__footer {
    padding: 16px 24px 20px;
    border-top: 1px solid #f0f0f0;
  }
}

.dialog-form {
  .el-form-item {
    margin-bottom: 20px;

    &:last-child {
      margin-bottom: 0;
    }
  }

  .el-form-item__label {
    font-weight: 500;
    color: #4e5969;
    padding-bottom: 8px;
  }

  .form-row {
    display: flex;
    align-items: flex-start;
    gap: 12px;

    .flex-1 {
      flex: 1;
    }
  }

  .date-separator {
    display: flex;
    align-items: center;
    padding-top: 32px;
    color: #86909c;
    font-size: 14px;
  }

  .form-tip {
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 12px;
    background: #f7f8fa;
    border-radius: 6px;
    margin-top: 8px;

    .el-icon {
      color: #165dff;
      font-size: 16px;
    }

    span {
      font-size: 13px;
      color: #86909c;
    }
  }
}

// 学期类型选择器
.term-type-selector {
  display: flex;
  gap: 12px;

  .term-type-item {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 6px;
    padding: 16px 12px;
    border: 2px solid #e5e6e8;
    border-radius: 8px;
    cursor: pointer;
    transition: all 0.2s;

    &:hover {
      border-color: #165dff;
      background: #f7f8fa;
    }

    &.active {
      border-color: #165dff;
      background: #e8f3ff;

      .term-icon {
        color: #165dff;
      }

      .term-label {
        color: #165dff;
        font-weight: 500;
      }
    }

    .term-icon {
      font-size: 24px;
      color: #86909c;
    }

    .term-label {
      font-size: 13px;
      color: #4e5969;
    }
  }
}

// 教学周数输入
.weeks-input {
  display: flex;
  align-items: center;
  gap: 8px;

  .weeks-unit {
    font-size: 14px;
    color: #4e5969;
  }

  .weeks-hint {
    font-size: 12px;
    color: #c0c4cc;
  }
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

// ========== 第一级：学年列表界面 ==========
.year-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
  padding: 8px;
}

.year-card-large {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  border: 1px solid #e5e6e8;
  cursor: pointer;
  transition: all 0.3s;
  position: relative;
  overflow: hidden;

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 12px 24px rgba(0, 0, 0, 0.1);
    border-color: #165dff;

    .year-action {
      color: #165dff;
    }
  }

  &.current {
    background: linear-gradient(135deg, #e8f3ff 0%, #f5faff 100%);
    border-color: #165dff;
  }

  .year-badge {
    position: absolute;
    top: 12px;
    right: -24px;
    background: #165dff;
    color: #fff;
    font-size: 11px;
    padding: 4px 32px;
    transform: rotate(45deg);
  }

  .year-icon {
    font-size: 36px;
    margin-bottom: 12px;
  }

  .year-name {
    font-size: 18px;
    font-weight: 600;
    color: #1d2129;
    margin-bottom: 8px;
  }

  .year-period {
    font-size: 13px;
    color: #86909c;
    margin-bottom: 16px;
  }

  .year-stats-row {
    display: flex;
    gap: 24px;
    margin-bottom: 16px;
    padding: 12px 0;
    border-top: 1px dashed #e5e6e8;
    border-bottom: 1px dashed #e5e6e8;

    .stat-item {
      display: flex;
      flex-direction: column;
      align-items: center;

      .stat-num {
        font-size: 24px;
        font-weight: 600;
        color: #165dff;
      }

      .stat-label {
        font-size: 12px;
        color: #86909c;
      }
    }
  }

  .year-action {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 4px;
    font-size: 13px;
    color: #86909c;
    transition: color 0.2s;

    .el-icon {
      font-size: 14px;
    }
  }
}

.empty-state {
  grid-column: 1 / -1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  background: #fff;
  border-radius: 12px;
  border: 1px dashed #e5e6e8;

  .empty-icon {
    font-size: 48px;
    margin-bottom: 16px;
  }

  .empty-text {
    font-size: 14px;
    color: #86909c;
    margin-bottom: 20px;
  }
}

// ========== 第二级：校历详情界面 ==========
.back-button {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  color: #86909c;
  font-size: 14px;
  padding: 6px 12px;
  border-radius: 6px;
  transition: all 0.2s;
  margin-right: 16px;

  &:hover {
    color: #165dff;
    background: #e8f3ff;
  }

  .el-icon {
    font-size: 16px;
  }
}

.current-year-title {
  display: flex;
  align-items: center;
  gap: 8px;

  .page-title {
    margin: 0;
  }
}

// ========== Tab布局样式 ==========
.calendar-tabs {
  :deep(.el-tabs__header) {
    margin-bottom: 16px;
    background: #fff;
    padding: 0 16px;
    border-radius: 8px;
  }

  :deep(.el-tabs__item) {
    font-size: 14px;
    padding: 0 20px;
    height: 48px;
    line-height: 48px;
  }

  :deep(.el-tabs__content) {
    overflow: visible;
  }
}

// Tab 1: 学期管理样式
.semester-list-panel {
  padding: 8px;
  max-height: 500px;
  overflow-y: auto;
}

.semester-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  background: #f7f8fa;
  border-radius: 8px;
  margin-bottom: 8px;
  cursor: pointer;
  border: 2px solid transparent;
  transition: all 0.2s;

  &:hover {
    background: #e8f3ff;
  }

  &.active {
    border-color: #165dff;
    background: #e8f3ff;
  }

  &.current {
    background: linear-gradient(135deg, #e8f3ff 0%, #f0f7ff 100%);
  }

  .sem-main {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  .sem-type-badge {
    padding: 4px 8px;
    border-radius: 4px;
    font-size: 12px;
    font-weight: 500;

    &.type-1 { background: #fff7e6; color: #fa8c16; }
    &.type-2 { background: #f6ffed; color: #52c41a; }
    &.type-3 { background: #e6f7ff; color: #1890ff; }
  }

  .sem-name {
    font-size: 14px;
    font-weight: 500;
    display: flex;
    align-items: center;
    gap: 6px;
  }

  .sem-meta {
    font-size: 12px;
    color: #86909c;
    margin-top: 2px;
  }

  .sem-stats {
    font-size: 12px;
    color: #86909c;
    background: #fff;
    padding: 4px 8px;
    border-radius: 4px;
  }
}

.semester-detail-content {
  padding: 16px;
}

.semester-info-card {
  background: linear-gradient(135deg, #f0f7ff 0%, #fff 100%);
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;

  .info-grid {
    display: flex;
    gap: 32px;
  }

  .info-item {
    .label {
      font-size: 12px;
      color: #86909c;
      margin-bottom: 4px;
    }

    .value {
      font-size: 14px;
      font-weight: 500;
      color: #1d2129;
    }
  }
}

.weeks-table-container {
  .week-number {
    font-weight: 500;
    color: #165dff;
  }

  .week-remark {
    color: #86909c;
  }
}

.empty-detail {
  min-height: 400px;
  display: flex;
  align-items: center;
  justify-content: center;
}

// Tab 3: 作息时间样式
.period-config-content {
  padding: 16px;
}

.period-tips {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 16px;
  padding: 12px;
  background: #f0f7ff;
  border-radius: 6px;
  font-size: 13px;
  color: #4e5969;

  .el-icon {
    color: #165dff;
  }
}

.period-preview {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 16px;
  padding: 12px 16px;
  background: linear-gradient(135deg, #f0f7ff 0%, #e8f4ff 100%);
  border-radius: 8px;
  font-size: 14px;
  color: #1d2129;
  border: 1px solid #d4e5ff;

  .el-icon {
    color: #165dff;
    font-size: 16px;
  }

  .duration {
    color: #86909c;
    font-size: 13px;
  }
}

// Tab 4: 年级活动样式
.grade-filter-list {
  padding: 8px;
}

.grade-filter-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 12px;
  border-radius: 6px;
  cursor: pointer;
  margin-bottom: 4px;
  transition: all 0.2s;

  &:hover {
    background: #f7f8fa;
  }

  &.active {
    background: #e8f3ff;
    color: #165dff;
    font-weight: 500;
  }

  .count {
    font-size: 12px;
    color: #86909c;
    background: #f2f3f5;
    padding: 2px 8px;
    border-radius: 10px;
  }

  &.active .count {
    background: #bedaff;
    color: #165dff;
  }
}

// 大日历样式
.large-calendar {
  .cal-day {
    min-height: 60px;
    padding: 4px;
  }
}

// 日历面板
.calendar-panel {
  .panel-header {
    padding: 8px 12px;
  }
}

// 学期概览（紧凑）
.semester-brief {
  .panel-header {
    padding: 8px 12px;
  }

  .semester-chips {
    padding: 8px;
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
  }

  .semester-chip {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 8px 12px;
    background: #f7f8fa;
    border-radius: 6px;
    cursor: pointer;
    transition: all 0.2s;
    border: 1px solid transparent;

    &:hover {
      background: #e8f3ff;
      border-color: #bedaff;
    }

    &.active {
      background: linear-gradient(135deg, #e8f3ff 0%, #f0f7ff 100%);
      border-color: #165dff;

      .chip-name {
        color: #165dff;
        font-weight: 600;
      }
    }

    .chip-name {
      font-size: 13px;
      color: #1d2129;
    }

    .chip-weeks {
      font-size: 11px;
      color: #86909c;
      background: #fff;
      padding: 2px 6px;
      border-radius: 4px;
    }
  }

  .no-semester {
    font-size: 12px;
    color: #c0c4cc;
    padding: 8px;
  }
}

</style>
