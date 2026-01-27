<template>
  <div class="check-plan-detail">
    <!-- 顶部导航栏 -->
    <header class="top-header">
      <div class="header-left">
        <button class="back-btn" @click="goBack">
          <ArrowLeft class="icon" />
        </button>
        <div class="header-info">
          <h1 class="header-title">{{ planDetail?.planName || '计划详情' }}</h1>
          <p class="header-subtitle">
            {{ planDetail?.planCode }} · {{ formatDateRange(planDetail?.startDate, planDetail?.endDate) }}
          </p>
        </div>
      </div>

      <!-- 状态和操作 -->
      <div class="header-right">
        <span class="status-badge" :class="getStatusClass(planDetail?.status)">
          {{ getStatusText(planDetail?.status) }}
        </span>
        <div class="action-buttons">
          <button v-if="planDetail?.status === 0" class="btn btn-success" @click="handleStart">
            <Play class="icon-sm" />
            开始计划
          </button>
          <button v-if="planDetail?.status === 1" class="btn btn-warning" @click="handleFinish">
            <Square class="icon-sm" />
            结束计划
          </button>
          <button v-if="planDetail?.status === 2" class="btn btn-default" @click="handleArchive">
            <Archive class="icon-sm" />
            归档
          </button>
          <button v-if="planDetail?.status === 0" class="btn btn-default" @click="handleEdit">
            <Pencil class="icon-sm" />
            编辑
          </button>
          <button v-if="planDetail?.status === 0" class="btn btn-danger" @click="handleDelete">
            <Trash2 class="icon-sm" />
            删除
          </button>
        </div>
      </div>
    </header>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-state">
      <Loader2 class="spinner" />
      <span>加载中...</span>
    </div>

    <!-- 主内容 -->
    <div v-else class="main-content">
      <!-- Tab导航 -->
      <div class="tab-nav">
        <button
          v-for="tab in tabs"
          :key="tab.key"
          class="tab-btn"
          :class="{ active: activeTab === tab.key }"
          @click="activeTab = tab.key"
        >
          <component :is="tab.icon" class="icon-sm" />
          {{ tab.label }}
        </button>
      </div>

      <!-- 概览Tab -->
      <div v-if="activeTab === 'overview'" class="tab-content">
        <div class="content-grid">
          <!-- 基本信息卡片 -->
          <div class="info-card">
            <h3 class="card-title">
              <FileText class="icon-sm" />
              基本信息
            </h3>
            <div class="info-list">
              <div class="info-row">
                <span class="label">计划编号</span>
                <span class="value code">{{ planDetail?.planCode }}</span>
              </div>
              <div class="info-row">
                <span class="label">计划名称</span>
                <span class="value">{{ planDetail?.planName }}</span>
              </div>
              <div class="info-row">
                <span class="label">计划描述</span>
                <span class="value">{{ planDetail?.description || '无' }}</span>
              </div>
              <div class="info-row">
                <span class="label">时间范围</span>
                <span class="value">{{ planDetail?.startDate }} ~ {{ planDetail?.endDate }}</span>
              </div>
              <div class="info-row">
                <span class="label">创建人</span>
                <span class="value">{{ planDetail?.creatorName || '-' }}</span>
              </div>
              <div class="info-row">
                <span class="label">创建时间</span>
                <span class="value">{{ formatDateTime(planDetail?.createdAt) }}</span>
              </div>
            </div>
          </div>

          <!-- 统计卡片 -->
          <div class="info-card">
            <h3 class="card-title">
              <BarChart3 class="icon-sm" />
              检查统计
            </h3>
            <div class="stat-boxes">
              <div class="stat-box">
                <span class="stat-value">{{ planDetail?.totalChecks || 0 }}</span>
                <span class="stat-label">检查次数</span>
              </div>
              <div class="stat-box">
                <span class="stat-value">{{ planDetail?.totalRecords || 0 }}</span>
                <span class="stat-label">检查记录</span>
              </div>
              <div class="stat-box">
                <span class="stat-value deduct">{{ formatScore(planDetail?.totalDeductionScore) }}</span>
                <span class="stat-label">总扣分</span>
              </div>
            </div>
          </div>

        </div>

        <!-- 功能卡片区域 -->
        <div class="feature-cards-section">
          <!-- 模板结构卡片 -->
          <div class="feature-card clickable" @click="showTemplateDialog = true" v-if="templateSnapshot?.categories?.length">
            <ClipboardList class="feature-icon template" />
            <h3>模板结构</h3>
            <p>{{ templateSnapshot?.categories?.length || 0 }} 个类别 · {{ getTotalItemCount() }} 个扣分项</p>
          </div>
          <!-- 统计分析入口（已移至独立Tab） -->
          <div class="feature-card clickable" @click="activeTab = 'analysis'">
            <BarChart3 class="feature-icon statistics" />
            <h3>统计分析</h3>
            <p>配置分析方案，查看班级排名与趋势</p>
          </div>
          <!-- 智能统计分析入口 -->
          <div class="feature-card clickable" @click="goToSmartStatistics">
            <TrendingUp class="feature-icon smart" />
            <h3>智能统计</h3>
            <p>多维度分析，自动适应不同检查目标与轮次</p>
          </div>
          <!-- 评级管理卡片 -->
          <div class="feature-card clickable" @click="activeTab = 'rating'">
            <Award class="feature-icon rating" />
            <h3>评级管理</h3>
            <p>{{ ratingRules.length > 0 ? `${ratingRules.length} 条评级规则` : '配置评级规则，自动计算班级评级' }}</p>
          </div>
          <!-- 汇总评级入口 - 待迁移到V4.4评级系统 -->
          <!-- <div class="feature-card clickable" @click="showRatingSummaryDialog = true" v-if="ratingRules.some(r => r.ruleType === 'SUMMARY')">
            <BarChart3 class="feature-icon summary" />
            <h3>汇总评级</h3>
            <p>查看指定周期内的班级综合评级结果</p>
          </div> -->
          <!-- 评级频次统计入口 -->
          <div class="feature-card clickable" @click="goToRatingFrequency" v-if="ratingRules.length > 0">
            <Trophy class="feature-icon frequency" />
            <h3>评级频次统计</h3>
            <p>统计班级获得各等级的频次，如月度优秀班级次数</p>
          </div>
          <!-- 评级审核管理入口 -->
          <div class="feature-card clickable" @click="goToRatingAudit" v-if="ratingRules.some(r => r.enabled === 1)">
            <ClipboardCheck class="feature-icon audit" />
            <h3>评级审核管理</h3>
            <p>审核评级结果，批量通过或驳回，发布评级公告</p>
          </div>
        </div>

        <!-- 模板结构弹窗 -->
        <el-dialog
          v-model="showTemplateDialog"
          title="模板结构与加权配置"
          width="800px"
          :close-on-click-modal="true"
          class="template-dialog"
        >
          <div class="dialog-template-header">
            <span class="dialog-template-name">{{ templateSnapshot?.templateName }}</span>
            <div class="dialog-template-badges">
              <span class="info-badge">
                <FolderOpen class="icon-xs" />
                {{ templateSnapshot?.categories?.length || 0 }} 个类别
              </span>
              <span class="info-badge">
                <FileText class="icon-xs" />
                {{ getTotalItemCount() }} 个扣分项
              </span>
              <span v-if="planDetail?.enableWeight === 1" class="info-badge success">
                <Scale class="icon-xs" />
                已启用加权
              </span>
            </div>
          </div>

          <!-- 类别和扣分项展示 -->
          <div class="dialog-template-categories">
            <div v-for="category in templateSnapshot?.categories" :key="category.categoryId" class="template-category">
              <!-- 类别头部 -->
              <div class="category-header" @click="toggleDetailCategory(category.categoryId)">
                <div class="category-left">
                  <ChevronRight
                    :size="14"
                    class="expand-icon"
                    :class="{ expanded: expandedDetailCategories.has(String(category.categoryId)) }"
                  />
                  <FolderOpen :size="16" class="folder-icon" />
                  <span class="category-name">{{ category.categoryName }}</span>
                  <span v-if="category.isRequired === 1" class="required-tag">必检</span>
                </div>
                <div class="category-right">
                  <span class="item-count">{{ category.deductionItems?.length || 0 }} 项</span>
                </div>
              </div>

              <!-- 扣分项卡片网格 -->
              <div v-show="expandedDetailCategories.has(String(category.categoryId))" class="detail-items-grid">
                <div
                  v-for="item in category.deductionItems"
                  :key="item.itemId"
                  class="detail-item-card"
                  :class="{ 'has-weight': planDetail?.enableWeight === 1 }"
                >
                  <div class="detail-item-header" :class="{ 'weighted': planDetail?.enableWeight === 1 }">
                    <span class="detail-item-name">{{ item.itemName }}</span>
                    <span class="detail-item-score" :class="getDetailScoreClass(item)">
                      {{ formatDetailScore(item) }}
                    </span>
                  </div>
                  <div class="detail-item-footer">
                    <div v-if="planDetail?.enableWeight === 1 && planDetail?.weightConfigName" class="detail-weight-tag" :style="{ background: '#6366f1' }">
                      <Gauge :size="10" />
                      {{ planDetail.weightConfigName }}
                    </div>
                    <div v-else-if="planDetail?.enableWeight === 1" class="detail-weight-tag" :style="{ background: '#6366f1' }">
                      <Gauge :size="10" />
                      已加权
                    </div>
                    <div v-else class="detail-no-weight">
                      <Target :size="10" />
                      无加权
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </el-dialog>
      </div>

      <!-- 检查管理Tab -->
      <div v-if="activeTab === 'checks'" class="tab-content">
        <ChecksManagementTab
          :plan-status="planDetail?.status || 0"
          :daily-checks="dailyChecks"
          :check-records="checkRecords"
          :inspectors="inspectors"
          :loading-daily-checks="loadingDailyChecks"
          :loading-records="loadingRecords"
          :loading-inspectors="loadingInspectors"
          @create-daily-check="handleCreateDailyCheck"
          @view-daily-check="handleViewDailyCheck"
          @start-daily-check="handleStartDailyCheck"
          @finish-daily-check="handleFinishDailyCheck"
          @scoring="handleScoring"
          @delete-daily-check="handleDeleteDailyCheck"
          @view-record="handleViewRecord"
          @add-inspector="showInspectorDialog = true"
          @remove-inspector="handleRemoveInspector"
        />
      </div>

      <!-- 通报中心Tab -->
      <div v-if="activeTab === 'notification'" class="tab-content">
        <NotificationCenterTab
          :plan-id="planId"
          :daily-checks="dailyChecks"
          :check-records="checkRecords"
          :deduction-items="allDeductionItems"
        />
      </div>

      <!-- 统计分析Tab -->
      <div v-if="activeTab === 'analysis'" class="tab-content">
        <PlanAnalysisTab :plan-id="planId" />
      </div>

      <!-- 评级配置Tab -->
      <div v-if="activeTab === 'rating'" class="tab-content">
        <el-tabs v-model="ratingSubTab" type="card">
          <el-tab-pane label="评级配置" name="config">
            <RatingConfigManagement :check-plan-id="planId" />
          </el-tab-pane>
          <el-tab-pane label="评级结果" name="results">
            <RatingResultView :check-plan-id="planId" />
          </el-tab-pane>
        </el-tabs>
      </div>

    </div>

    <!-- 添加/编辑打分人员弹窗 -->
    <el-dialog
      v-model="showInspectorDialog"
      :title="editingInspector ? '编辑打分人员' : '添加打分人员'"
      width="800px"
      :close-on-click-modal="false"
      class="inspector-dialog"
    >
      <div class="inspector-form">
        <!-- 第一步：选择用户 -->
        <div class="form-section">
          <div class="section-label">
            <span class="required">*</span> 选择人员
          </div>
          <div v-if="editingInspector" class="selected-user-card">
            <div class="user-avatar">{{ editingInspector.userName?.charAt(0) || '?' }}</div>
            <div class="user-details">
              <span class="user-name">{{ editingInspector.userName }}</span>
              <span class="user-meta">@{{ editingInspector.username }}</span>
            </div>
          </div>
          <div v-else class="user-search-section">
            <el-input
              v-model="userSearchKeyword"
              placeholder="输入姓名或账号搜索人员..."
              :prefix-icon="Search"
              clearable
              @input="debouncedSearchUsers"
            />
            <div class="user-list-container">
              <div v-if="userSearchLoading" class="user-list-loading">
                <el-icon class="is-loading"><Loading /></el-icon>
                搜索中...
              </div>
              <div v-else-if="userList.length === 0 && userSearchKeyword" class="user-list-empty">
                未找到匹配的用户
              </div>
              <div v-else-if="userList.length === 0" class="user-list-hint">
                请输入关键词搜索用户
              </div>
              <div v-else class="user-list">
                <div
                  v-for="user in userList"
                  :key="user.id"
                  class="user-card"
                  :class="{ selected: inspectorForm.userId === user.id }"
                  @click="selectUser(user)"
                >
                  <div class="user-avatar">{{ user.realName?.charAt(0) || '?' }}</div>
                  <div class="user-details">
                    <span class="user-name">{{ user.realName }}</span>
                    <span class="user-meta">@{{ user.username }}</span>
                    <span v-if="user.orgUnitName" class="user-dept">{{ user.orgUnitName }}</span>
                  </div>
                  <div v-if="inspectorForm.userId === user.id" class="check-icon">
                    <Check :size="18" />
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 第二步：选择检查类别权限 -->
        <div class="form-section">
          <div class="section-label">
            <span class="required">*</span> 检查类别权限
            <span class="section-hint">勾选该打分人员可以打分的检查类别</span>
          </div>
          <div class="category-checkbox-grid">
            <div
              v-for="cat in templateSnapshot?.categories || []"
              :key="cat.categoryId"
              class="category-checkbox-item"
              :class="{ checked: isCategorySelected(cat.categoryId) }"
              @click="toggleCategory(cat)"
            >
              <div class="checkbox-indicator">
                <Check v-if="isCategorySelected(cat.categoryId)" :size="14" />
              </div>
              <span class="category-label">{{ cat.categoryName }}</span>
            </div>
          </div>
        </div>

        <!-- 第三步：配置班级范围 -->
        <div v-if="inspectorForm.permissions.length > 0" class="form-section">
          <div class="section-label">
            班级范围
            <span class="section-hint">为每个类别设置可打分的班级，不选则默认全部班级</span>
          </div>
          <div class="class-permission-list">
            <div
              v-for="perm in inspectorForm.permissions"
              :key="perm.categoryId"
              class="class-permission-item"
            >
              <div class="permission-header">
                <span class="category-tag">{{ perm.categoryName }}</span>
                <span class="class-scope-text">
                  {{ !perm.classIds || perm.classIds.length === 0 ? '全部班级' : `已选 ${perm.classIds.length} 个班级` }}
                </span>
                <button
                  type="button"
                  class="toggle-expand-btn"
                  @click="toggleClassExpand(perm.categoryId)"
                >
                  {{ expandedClassConfigs.has(perm.categoryId) ? '收起' : '展开选择' }}
                </button>
              </div>
              <div v-if="expandedClassConfigs.has(perm.categoryId)" class="class-checkbox-grid">
                <label
                  v-for="cls in availableClasses"
                  :key="cls.id"
                  class="class-checkbox-item"
                >
                  <input
                    type="checkbox"
                    :checked="perm.classIds?.includes(cls.id)"
                    @change="toggleClassInPermission(perm, cls.id)"
                  />
                  <span>{{ cls.className }}</span>
                </label>
              </div>
            </div>
          </div>
        </div>

        <!-- 备注 -->
        <div class="form-section">
          <div class="section-label">备注</div>
          <el-input
            v-model="inspectorForm.remark"
            type="textarea"
            :rows="2"
            placeholder="可选，添加备注信息"
          />
        </div>
      </div>

      <template #footer>
        <el-button @click="showInspectorDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSaveInspector">保存</el-button>
      </template>
    </el-dialog>

    <!-- 汇总评级弹窗 - 待迁移到V4.4评级系统 -->
    <!-- <RatingSummaryDialog
      v-model:visible="showRatingSummaryDialog"
      :check-plan-id="planId"
      :plan-start-date="planDetail?.startDate"
      :plan-end-date="planDetail?.endDate"
      :rules="ratingRules"
    /> -->

    <!-- 模板快照弹窗 -->
    <Teleport to="body">
      <div v-if="showTemplateSnapshot" class="modal-overlay" @click.self="showTemplateSnapshot = false">
        <div class="modal-content modal-xl">
          <div class="modal-header">
            <div class="modal-header-left">
              <h3>模板快照</h3>
              <span class="template-name-tag">{{ templateSnapshot?.templateName }}</span>
            </div>
            <div class="modal-header-right">
              <span class="snapshot-time">
                <Clock class="icon-xs" />
                快照时间: {{ formatSnapshotTime(templateSnapshot?.snapshotTime) }}
              </span>
              <button class="close-btn" @click="showTemplateSnapshot = false">
                <X class="icon" />
              </button>
            </div>
          </div>
          <div class="modal-body snapshot-modal-body">
            <div v-if="templateSnapshot?.categories?.length" class="snapshot-categories-grid">
              <!-- 卡片式展示每个检查类别 -->
              <div v-for="category in templateSnapshot.categories" :key="category.categoryId" class="category-card">
                <!-- 卡片头部 -->
                <div class="category-card-header">
                  <div class="category-title">
                    <span class="category-name">{{ category.categoryName }}</span>
                    <span v-if="category.categoryCode" class="category-code">{{ category.categoryCode }}</span>
                  </div>
                  <div class="category-badges">
                    <span v-if="category.isRequired === 1" class="cat-badge required">必检</span>
                    <span v-if="category.linkType === 1" class="cat-badge link">关联宿舍</span>
                    <span v-else-if="category.linkType === 2" class="cat-badge link">关联教室</span>
                  </div>
                </div>

                <!-- 扣分项列表 -->
                <div class="deduction-items-container">
                  <div v-if="category.deductionItems && category.deductionItems.length > 0" class="deduction-items-list">
                    <div v-for="item in category.deductionItems" :key="item.itemId" class="deduction-item-card">
                      <div class="item-main">
                        <span class="item-name">{{ item.itemName }}</span>
                        <div class="item-tags">
                          <span v-if="item.allowPhoto === 1" class="item-tag photo" title="允许上传照片">
                            <Camera class="icon-xs" />
                          </span>
                          <span v-if="item.allowRemark === 1" class="item-tag remark" title="允许添加备注">
                            <MessageSquare class="icon-xs" />
                          </span>
                          <span v-if="item.allowStudents === 1" class="item-tag students" title="允许添加学生">
                            <Users class="icon-xs" />
                          </span>
                        </div>
                      </div>
                      <div class="item-score-info">
                        <!-- 模式1: 固定扣分 -->
                        <template v-if="item.deductMode === 1">
                          <span class="score-value fixed">-{{ item.fixedScore || 0 }}</span>
                          <span class="score-type">固定扣分</span>
                        </template>
                        <!-- 模式2: 按人数扣分 -->
                        <template v-else-if="item.deductMode === 2">
                          <span class="score-value per-person">
                            {{ item.baseScore || 0 }}+{{ item.perPersonScore || 0 }}/人
                          </span>
                          <span class="score-type">按人扣分</span>
                        </template>
                        <!-- 模式3: 区间扣分 -->
                        <template v-else-if="item.deductMode === 3">
                          <span class="score-value range">{{ formatRangeConfig(item.rangeConfig) }}</span>
                          <span class="score-type">区间扣分</span>
                        </template>
                      </div>
                      <div v-if="item.description" class="item-description">
                        {{ item.description }}
                      </div>
                    </div>
                  </div>
                  <div v-else class="no-items">
                    <AlertCircle class="icon-sm" />
                    <span>暂无扣分项</span>
                  </div>
                </div>

                <!-- 卡片底部统计 -->
                <div class="category-card-footer">
                  <span class="items-count">
                    共 {{ category.deductionItems?.length || 0 }} 个扣分项
                  </span>
                </div>
              </div>
            </div>
            <div v-else class="empty-inline">
              <ClipboardList class="empty-icon" />
              <p>暂无检查类别数据</p>
            </div>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- 编辑弹窗 -->
    <Teleport to="body">
      <div v-if="showEditDialog" class="modal-overlay" @click.self="showEditDialog = false">
        <div class="modal-content">
          <div class="modal-header">
            <h3>编辑计划</h3>
            <button class="close-btn" @click="showEditDialog = false">
              <X class="icon" />
            </button>
          </div>
          <div class="modal-body">
            <div class="form-item">
              <label>计划名称</label>
              <input v-model="editForm.planName" type="text" />
            </div>
            <div class="form-item">
              <label>计划描述</label>
              <textarea v-model="editForm.description" rows="3"></textarea>
            </div>
            <div class="form-row">
              <div class="form-item">
                <label>开始日期</label>
                <input v-model="editForm.startDate" type="date" />
              </div>
              <div class="form-item">
                <label>结束日期</label>
                <input v-model="editForm.endDate" type="date" />
              </div>
            </div>
          </div>
          <div class="modal-footer">
            <button class="btn btn-default" @click="showEditDialog = false">取消</button>
            <button class="btn btn-primary" @click="saveEdit" :disabled="saving">
              {{ saving ? '保存中...' : '保存' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- 创建日常检查弹窗 - 重构版 -->
    <Teleport to="body">
      <div v-if="showCreateDailyCheckDialog" class="create-check-overlay" @click.self="showCreateDailyCheckDialog = false">
        <div class="create-check-dialog">
          <!-- 头部 -->
          <div class="dialog-header">
            <div class="header-title">
              <Calendar :size="20" />
              <span>新建日常检查</span>
            </div>
            <button class="header-close" @click="showCreateDailyCheckDialog = false">
              <X :size="18" />
            </button>
          </div>

          <!-- 内容区 -->
          <div class="dialog-content">
            <!-- 第一行：日期 + 类型 + 名称 -->
            <div class="form-row-inline">
              <div class="inline-field date-field">
                <label>日期</label>
                <input v-model="dailyCheckForm.checkDate" @change="autoGenerateCheckName" type="date" />
              </div>
              <div class="inline-field type-field">
                <label>类型</label>
                <div class="type-switch">
                  <span
                    class="type-option"
                    :class="{ active: dailyCheckForm.checkType === 1 }"
                    @click="dailyCheckForm.checkType = 1; autoGenerateCheckName()"
                  >日常</span>
                  <span
                    class="type-option"
                    :class="{ active: dailyCheckForm.checkType === 2 }"
                    @click="dailyCheckForm.checkType = 2; autoGenerateCheckName()"
                  >专项</span>
                </div>
              </div>
              <div class="inline-field name-field">
                <label>名称</label>
                <input v-model="dailyCheckForm.checkName" type="text" placeholder="自动生成，可修改" />
              </div>
            </div>

            <!-- 检查范围 -->
            <div class="scope-section">
              <div class="scope-header">
                <div class="scope-info">
                  <Users :size="16" />
                  <span>检查 <strong>{{ actualCheckClassCount }}</strong> 个班级</span>
                  <span v-if="dailyCheckForm.excludedTargets.length > 0" class="exclude-count">
                    (已排除{{ dailyCheckForm.excludedTargets.length }}个)
                  </span>
                </div>
                <button type="button" class="scope-toggle" @click="showExcludeSection = !showExcludeSection">
                  <span>{{ showExcludeSection ? '收起' : '排除班级' }}</span>
                  <ChevronRight :size="14" :class="{ rotated: showExcludeSection }" />
                </button>
              </div>

              <!-- 排除班级展开区 -->
              <div v-if="showExcludeSection" class="scope-expand">
                <div class="class-chips">
                  <div
                    v-for="cls in planTargetClasses"
                    :key="cls.id"
                    class="class-chip"
                    :class="{ excluded: isClassExcluded(cls.id) }"
                    @click="toggleExcludeClass(cls)"
                  >
                    {{ cls.className }}
                    <X v-if="isClassExcluded(cls.id)" :size="12" class="chip-x" />
                  </div>
                </div>
                <div v-if="dailyCheckForm.excludedTargets.length > 0" class="exclude-reasons">
                  <div v-for="(item, index) in dailyCheckForm.excludedTargets" :key="item.classId" class="reason-row">
                    <span class="reason-class">{{ item.className }}</span>
                    <input v-model="item.reason" type="text" placeholder="排除原因" class="reason-input" />
                    <button type="button" class="reason-remove" @click="removeExcludedClass(index)">
                      <X :size="12" />
                    </button>
                  </div>
                </div>
              </div>
            </div>

            <!-- 检查类别 -->
            <div class="category-section">
              <div class="section-header">
                <span class="section-title">检查类别</span>
                <!-- 添加轮次按钮 - 始终显示在标题右侧 -->
                <button type="button" class="add-round-btn-fixed" @click="handleAddRound" title="添加轮次">
                  <Plus :size="14" />
                </button>
              </div>

              <!-- 类别/轮次配置 - 统一风格 -->
              <div class="category-panel">
                <!-- 轮次标签页（始终显示） -->
                <div class="round-tabs">
                  <div
                    v-for="round in dailyCheckForm.totalRounds"
                    :key="round"
                    class="round-tab"
                    :class="{ active: currentEditRound === round }"
                    @click="currentEditRound = round"
                  >
                    <span class="tab-num">{{ round }}</span>
                    <input
                      v-model="dailyCheckForm.roundNames[round - 1]"
                      type="text"
                      class="tab-name"
                      :placeholder="`第${round}轮`"
                      @click.stop
                    />
                    <!-- 只有多轮次时才显示删除按钮 -->
                    <button
                      v-if="dailyCheckForm.totalRounds > 1"
                      type="button"
                      class="tab-del"
                      @click.stop="handleDeleteRound(round)"
                    >
                      <X :size="12" />
                    </button>
                  </div>
                </div>

                <!-- 类别标签区域 -->
                <div class="category-chips">
                  <span
                    v-for="cat in getCategoriesForRound(currentEditRound)"
                    :key="`${currentEditRound}-${cat.categoryId}`"
                    class="cat-chip"
                  >
                    {{ cat.categoryName }}
                    <span v-if="cat.linkType" class="chip-badge">{{ cat.linkType === 1 ? '宿舍' : '教室' }}</span>
                    <button
                      v-if="getCategoriesForRound(currentEditRound).length > 1"
                      type="button"
                      class="chip-remove"
                      @click="handleRemoveCategoryFromRound(cat, currentEditRound)"
                    >
                      <X :size="12" />
                    </button>
                  </span>
                  <!-- 添加类别按钮（多轮次时显示） -->
                  <button
                    v-if="dailyCheckForm.totalRounds > 1 && getAvailableCategoriesForRound(currentEditRound).length > 0"
                    type="button"
                    class="chip-add"
                    @click="showAddCategoryMenu(currentEditRound, $event)"
                  >
                    <Plus :size="14" />
                  </button>
                  <span v-if="dailyCheckForm.categories.length === 0" class="no-cat">暂无类别</span>
                </div>
              </div>
            </div>

            <!-- 备注 -->
            <div class="remark-section">
              <label>备注</label>
              <textarea v-model="dailyCheckForm.description" rows="2" placeholder="可选，填写特殊说明"></textarea>
            </div>
          </div>

          <!-- 底部 -->
          <div class="dialog-footer">
            <button class="btn-cancel" @click="showCreateDailyCheckDialog = false">取消</button>
            <button
              class="btn-submit"
              @click="handleSubmitDailyCheck"
              :disabled="submitLoading || !dailyCheckForm.checkName || !dailyCheckForm.checkDate || actualCheckClassCount === 0"
            >
              <Loader2 v-if="submitLoading" :size="16" class="spin" />
              {{ submitLoading ? '创建中...' : '创建检查' }}
            </button>
          </div>
        </div>

        <!-- 添加类别下拉 -->
        <div v-if="addCategoryMenuVisible" class="cat-dropdown" :style="addCategoryMenuStyle">
          <div
            v-for="cat in getAvailableCategoriesForRound(addCategoryMenuRound)"
            :key="cat.categoryId"
            class="cat-dropdown-item"
            @click="handleAddCategoryToRound(cat, addCategoryMenuRound)"
          >
            {{ cat.categoryName }}
          </div>
        </div>
      </div>
    </Teleport>
    <!-- 旧版统计分析弹窗（保留兼容，可在概览页快速查看基础统计） -->
    <StatisticsDialog
      v-model:visible="showStatisticsDialog"
      :plan-id="planId"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch, markRaw } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowLeft,
  Play,
  Square,
  Archive,
  Pencil,
  Trash2,
  Loader2,
  FileText,
  BarChart3,
  ClipboardList,
  Scale,
  Eye,
  X,
  Plus,
  Award,
  LayoutGrid,
  Calendar,
  FileCheck,
  Star,
  Info,
  AlertCircle,
  Check,
  ChevronLeft,
  ChevronRight,
  Download,
  Clock,
  Camera,
  MessageSquare,
  Users,
  FolderOpen,
  Gauge,
  Target,
  TrendingUp,
  Search,
  FileOutput,
  Trophy,
  ClipboardCheck
} from 'lucide-vue-next'
import { Loading } from '@element-plus/icons-vue'
import {
  getCheckPlanDetail,
  updateCheckPlan,
  deleteCheckPlan,
  startPlan,
  finishPlan,
  archivePlan,
  parseTemplateSnapshot,
  getPlanTargetClasses,
  PLAN_STATUS,
  PLAN_STATUS_LABELS
} from '@/api/quantification'
import type { CheckPlanVO, TemplateSnapshot, SnapshotCategory } from '@/api/quantification'
import { http } from '@/utils/request'
import {
  getCheckRecordsList,
  deleteCheckRecord,
  publishCheckRecord,
  exportCheckRecord
} from '@/api/quantification'
import type { CheckRecord } from '@/api/quantification'
import { getClassList, type Class } from '@/api/organization'
import { getAllEnabledDepartments, type DepartmentResponse } from '@/api/organization'
import { getAllGrades, type Grade } from '@/api/organization'
import { useAuthStore } from '@/stores/auth'
import StatisticsDialog from './components/statistics/StatisticsDialog.vue'
import PlanAnalysisTab from './components/PlanAnalysisTab.vue'
import RatingConfigManagement from './components/rating/RatingConfigManagement.vue'
import RatingResultView from './components/rating/RatingResultView.vue'
import ExportTemplateTab from './components/ExportTemplateTab.vue'
import ChecksManagementTab from './components/ChecksManagementTab.vue'
import NotificationCenterTab from './components/NotificationCenterTab.vue'
import {
  getInspectorsByPlanId,
  addInspector,
  updateInspector,
  deleteInspector,
  type InspectorDTO,
  type InspectorCreateRequest,
  type InspectorUpdateRequest,
  type PermissionConfig
} from '@/api/quantification-extra'
import { getSimpleUserList, type SimpleUser } from '@/api/user'

const authStore = useAuthStore()
const hasPermission = (permission: string) => authStore.hasPermission(permission)

const router = useRouter()
const route = useRoute()

const planId = computed(() => route.params.id as string)

// Tab配置
const tabs = [
  { key: 'overview', label: '概览', icon: markRaw(LayoutGrid) },
  { key: 'checks', label: '检查管理', icon: markRaw(Calendar) },
  { key: 'notification', label: '通报中心', icon: markRaw(FileOutput) },
  { key: 'rating', label: '评级管理', icon: markRaw(Award) },
  { key: 'analysis', label: '统计分析', icon: markRaw(BarChart3) }
]
const activeTab = ref('overview')
const ratingSubTab = ref('config')

// 数据状态
const loading = ref(false)
const planDetail = ref<CheckPlanVO | null>(null)
const templateSnapshot = ref<TemplateSnapshot | null>(null)

// 日常检查
const loadingDailyChecks = ref(false)
const dailyChecks = ref<any[]>([])

// 检查记录
const loadingRecords = ref(false)
const checkRecords = ref<CheckRecord[]>([])

// 弹窗状态
const showTemplateSnapshot = ref(false)
const showEditDialog = ref(false)
const saving = ref(false)
const showTemplateDialog = ref(false)
const showStatisticsDialog = ref(false)
const showRatingSummaryDialog = ref(false)

// 评级规则数据
const ratingRules = ref<RatingRuleVO[]>([])

// 打分人员
const loadingInspectors = ref(false)
const inspectors = ref<InspectorDTO[]>([])
const showInspectorDialog = ref(false)
const editingInspector = ref<InspectorDTO | null>(null)
const inspectorForm = reactive<{
  userId: number | null
  permissions: PermissionConfig[]
  remark: string
}>({
  userId: null,
  permissions: [],
  remark: ''
})
const userList = ref<SimpleUser[]>([])
const userSearchLoading = ref(false)
const userSearchKeyword = ref('')
const expandedClassConfigs = ref<Set<string>>(new Set())

// 防抖搜索用户
let searchDebounceTimer: ReturnType<typeof setTimeout> | null = null
const debouncedSearchUsers = () => {
  if (searchDebounceTimer) {
    clearTimeout(searchDebounceTimer)
  }
  searchDebounceTimer = setTimeout(() => {
    searchUsers(userSearchKeyword.value)
  }, 300)
}

// 选择用户
const selectUser = (user: SimpleUser) => {
  inspectorForm.userId = user.id
}

// 检查类别是否被选中
const isCategorySelected = (categoryId: string | number) => {
  return inspectorForm.permissions.some(p => p.categoryId === String(categoryId))
}

// 切换类别选中状态
const toggleCategory = (cat: { categoryId: string | number; categoryName: string }) => {
  const categoryIdStr = String(cat.categoryId)
  const index = inspectorForm.permissions.findIndex(p => p.categoryId === categoryIdStr)
  if (index >= 0) {
    inspectorForm.permissions.splice(index, 1)
    expandedClassConfigs.value.delete(categoryIdStr)
  } else {
    inspectorForm.permissions.push({
      categoryId: categoryIdStr,
      categoryName: cat.categoryName,
      classIds: []
    })
  }
}

// 切换班级配置展开状态
const toggleClassExpand = (categoryId: string) => {
  if (expandedClassConfigs.value.has(categoryId)) {
    expandedClassConfigs.value.delete(categoryId)
  } else {
    expandedClassConfigs.value.add(categoryId)
  }
}

// 切换班级选中状态
const toggleClassInPermission = (perm: PermissionConfig, classId: number) => {
  if (!perm.classIds) {
    perm.classIds = []
  }
  const index = perm.classIds.indexOf(classId)
  if (index >= 0) {
    perm.classIds.splice(index, 1)
  } else {
    perm.classIds.push(classId)
  }
}

// 详情页模板展开状态
const expandedDetailCategories = ref<Set<string | number>>(new Set())

// 加权配置颜色池
const WEIGHT_COLORS = [
  '#6366f1', '#8b5cf6', '#ec4899', '#f43f5e', '#f97316',
  '#eab308', '#22c55e', '#14b8a6', '#06b6d4', '#3b82f6'
]

// 加权配置映射（从planDetail中解析）
const itemWeightConfigMap = ref<Map<string | number, any>>(new Map())

// 编辑表单
const editForm = reactive({
  planName: '',
  description: '',
  startDate: '',
  endDate: ''
})

// 日常检查创建相关
const showCreateDailyCheckDialog = ref(false)
const dailyCheckFormStep = ref(1)
const submitLoading = ref(false)
const currentTargetType = ref(1)

// 检查目标类型
const targetTypes = [
  { value: 1, label: '按班级' },
  { value: 2, label: '按年级' },
  { value: 3, label: '按院系' }
]

// 班级、年级、院系列表
const classList = ref<Class[]>([])
const gradeList = ref<Grade[]>([])
const departmentList = ref<DepartmentResponse[]>([])
// 计划目标班级列表（根据计划的目标范围配置过滤后的班级）
const planTargetClasses = ref<Class[]>([])

// 计算属性：根据计划目标范围过滤可选的班级
const availableClasses = computed(() => {
  if (planTargetClasses.value.length > 0) {
    return planTargetClasses.value
  }
  return classList.value
})

// 计算属性：根据计划目标范围过滤可选的年级
const availableGrades = computed(() => {
  if (planTargetClasses.value.length > 0) {
    // 从目标班级中提取年级
    const gradeNames = new Set(planTargetClasses.value.map(c => c.grade))
    return gradeList.value.filter(g => gradeNames.has(g.gradeName))
  }
  return gradeList.value
})

// 计算属性：根据计划目标范围过滤可选的院系
const availableDepartments = computed(() => {
  if (planTargetClasses.value.length > 0) {
    // 从目标班级中提取院系ID
    const deptIds = new Set(planTargetClasses.value.map(c => c.orgUnitId))
    return departmentList.value.filter(d => deptIds.has(d.id))
  }
  return departmentList.value
})

// 检查目标项接口
interface CheckTargetItem {
  targetType: number
  targetId: number
  targetName: string
}

// 检查类别项接口
interface CheckCategoryItem {
  categoryId: string | number
  categoryName: string
  linkType?: number
  isRequired?: number
  sortOrder?: number
  checkRounds?: number               // 旧字段，保留兼容
  participatedRounds?: string        // 参与的轮次，如 "1,3"
}

// 排除目标项接口
interface ExcludedTargetItem {
  classId: number
  className: string
  reason: string
}

// 日常检查创建表单
const dailyCheckForm = reactive<{
  checkDate: string
  checkName: string
  checkType: number
  description: string
  targets: CheckTargetItem[]
  categories: CheckCategoryItem[]
  excludedTargets: ExcludedTargetItem[]
  totalRounds: number                // 总轮次数
  roundNames: string[]               // 轮次名称数组
}>({
  checkDate: '',
  checkName: '',
  checkType: 1,
  description: '',
  targets: [],
  categories: [],
  excludedTargets: [],
  totalRounds: 1,
  roundNames: []
})

// 是否展开排除目标区域
const showExcludeSection = ref(false)

// 计算计划的全部目标班级
const allPlanTargetClassIds = computed(() => {
  return planTargetClasses.value.map(c => c.id)
})

// 计算排除后实际检查的班级ID列表
const actualCheckClassIds = computed(() => {
  const excludedIds = new Set(dailyCheckForm.excludedTargets.map(e => e.classId))
  return allPlanTargetClassIds.value.filter(id => !excludedIds.has(id))
})

// 实际检查的班级数量
const actualCheckClassCount = computed(() => actualCheckClassIds.value.length)

// 全部目标班级数量
const totalTargetClassCount = computed(() => planTargetClasses.value.length)

// 是否可以进入下一步
const canNextStep = computed(() => {
  if (dailyCheckFormStep.value === 1) return !!dailyCheckForm.checkName && !!dailyCheckForm.checkDate
  return true
})

// 格式化日期范围
const formatDateRange = (start?: string, end?: string) => {
  if (!start || !end) return '-'
  return `${start} ~ ${end}`
}

// 格式化日期时间
const formatDateTime = (dateTime?: string) => {
  if (!dateTime) return '-'
  return dateTime.replace('T', ' ').substring(0, 16)
}

// 格式化分数
const formatScore = (score?: number | null) => {
  if (score === undefined || score === null) return '0'
  return Number(score).toFixed(1)
}

// 格式化快照时间
const formatSnapshotTime = (time?: string) => {
  if (!time) return '-'
  return time.replace('T', ' ')
}

// 格式化区间配置
const formatRangeConfig = (rangeConfig?: string | null) => {
  if (!rangeConfig) return '0~0'
  try {
    const ranges = JSON.parse(rangeConfig)
    if (Array.isArray(ranges) && ranges.length > 0) {
      const range = ranges[0]
      return `${range.min || 0}~${range.max || 0}`
    }
    return '0~0'
  } catch {
    return '0~0'
  }
}

// 详情页模板相关方法
const toggleDetailCategory = (categoryId: string | number) => {
  const id = String(categoryId)
  if (expandedDetailCategories.value.has(id)) {
    expandedDetailCategories.value.delete(id)
  } else {
    expandedDetailCategories.value.add(id)
  }
}

const getTotalItemCount = () => {
  return templateSnapshot.value?.categories?.reduce((sum, cat) => sum + (cat.deductionItems?.length || 0), 0) || 0
}

const getItemWeightConfig = (item: any) => {
  return itemWeightConfigMap.value.get(item.itemId)
}

const getDetailScoreClass = (item: any) => {
  if (item.deductMode === 1) return 'fixed'
  if (item.deductMode === 2) return 'dynamic'
  return 'range'
}

const formatDetailScore = (item: any) => {
  if (item.deductMode === 1) return `-${item.fixedScore || 0}`
  if (item.deductMode === 2) return `${item.baseScore || 0}+${item.perPersonScore || 0}/人`
  return '区间'
}

const getDetailItemStyle = (item: any) => {
  const weight = getItemWeightConfig(item)
  if (weight?.color) {
    return {
      borderColor: weight.color,
      borderLeftWidth: '3px'
    }
  }
  return {}
}

const getDetailItemHeaderStyle = (item: any) => {
  const weight = getItemWeightConfig(item)
  if (weight?.color) {
    return {
      background: `${weight.color}15`,
      borderBottom: `1px solid ${weight.color}30`
    }
  }
  return {}
}

// 初始化：默认折叠所有类别
const initExpandedCategories = () => {
  expandedDetailCategories.value.clear()
  // 默认不展开任何类别，用户点击时才展开
}

// 解析加权配置
const parseWeightConfigs = () => {
  itemWeightConfigMap.value.clear()
  if (planDetail.value?.itemWeightConfigs) {
    try {
      const configs = typeof planDetail.value.itemWeightConfigs === 'string'
        ? JSON.parse(planDetail.value.itemWeightConfigs)
        : planDetail.value.itemWeightConfigs
      if (Array.isArray(configs)) {
        configs.forEach((cfg: any, idx: number) => {
          itemWeightConfigMap.value.set(cfg.itemId, {
            ...cfg,
            color: WEIGHT_COLORS[idx % WEIGHT_COLORS.length]
          })
        })
      }
    } catch (e) {
      console.error('解析加权配置失败', e)
    }
  }
}

// 计划状态样式
const getStatusClass = (status?: number) => {
  const map: Record<number, string> = {
    [PLAN_STATUS.DRAFT]: 'draft',
    [PLAN_STATUS.IN_PROGRESS]: 'progress',
    [PLAN_STATUS.FINISHED]: 'finished',
    [PLAN_STATUS.ARCHIVED]: 'archived'
  }
  return map[status ?? 0] || 'draft'
}

const getStatusText = (status?: number) => {
  return PLAN_STATUS_LABELS[status ?? 0] || '未知'
}

// 日常检查状态
const getDailyCheckStatusClass = (status: number) => {
  const map: Record<number, string> = {
    0: 'draft',
    1: 'progress',
    2: 'finished',
    3: 'published'
  }
  return map[status] || 'draft'
}

const getDailyCheckStatusText = (status: number) => {
  const map: Record<number, string> = {
    0: '未开始',
    1: '进行中',
    2: '已完成',
    3: '已发布'
  }
  return map[status] || '未知'
}

// 加载计划详情
const loadPlanDetail = async () => {
  loading.value = true
  try {
    const res = await getCheckPlanDetail(planId.value)
    planDetail.value = res
    if (res?.templateSnapshot) {
      templateSnapshot.value = parseTemplateSnapshot(res.templateSnapshot)
      initExpandedCategories()
    }
    parseWeightConfigs()
  } catch (error: any) {
    ElMessage.error(error.message || '加载失败')
    router.push('/quantification/check-plan')
  } finally {
    loading.value = false
  }
}

// 加载日常检查
const loadDailyChecks = async () => {
  loadingDailyChecks.value = true
  try {
    const res = await http.get<any>('/quantification/daily-checks', {
      params: { planId: planId.value, pageNum: 1, pageSize: 100 }
    })
    dailyChecks.value = res?.records || []
  } catch (error) {
    console.error('加载日常检查失败', error)
  } finally {
    loadingDailyChecks.value = false
  }
}

// 加载检查记录
const loadCheckRecords = async () => {
  loadingRecords.value = true
  try {
    const res = await getCheckRecordsList({
      pageNum: 1,
      pageSize: 100,
      planId: planId.value
    })
    if (res && res.records !== undefined) {
      checkRecords.value = res.records || []
    } else if (Array.isArray(res)) {
      checkRecords.value = res
    } else {
      checkRecords.value = []
    }
  } catch (error) {
    console.error('加载检查记录失败', error)
    checkRecords.value = []
  } finally {
    loadingRecords.value = false
  }
}

// 加载评级规则（已废弃，使用新的评级系统）
// const loadRatingRules = async () => {
//   try {
//     const res = await getRatingRulesByPlanId(planId.value)
//     ratingRules.value = res || []
//   } catch (error) {
//     console.error('加载评级规则失败', error)
//     ratingRules.value = []
//   }
// }

// ========== 打分人员管理 ==========
const loadInspectors = async () => {
  loadingInspectors.value = true
  try {
    const res = await getInspectorsByPlanId(planId.value)
    inspectors.value = res || []
  } catch (error) {
    console.error('加载打分人员失败', error)
    inspectors.value = []
  } finally {
    loadingInspectors.value = false
  }
}

const searchUsers = async (keyword: string) => {
  if (!keyword) {
    userList.value = []
    return
  }
  userSearchLoading.value = true
  try {
    const res = await getSimpleUserList(keyword)
    userList.value = res || []
  } catch (error) {
    console.error('搜索用户失败', error)
    userList.value = []
  } finally {
    userSearchLoading.value = false
  }
}

const openAddInspectorDialog = () => {
  editingInspector.value = null
  inspectorForm.userId = null
  inspectorForm.permissions = []
  inspectorForm.remark = ''
  userList.value = []
  userSearchKeyword.value = ''
  expandedClassConfigs.value.clear()
  showInspectorDialog.value = true
}

const openEditInspectorDialog = (inspector: InspectorDTO) => {
  editingInspector.value = inspector
  inspectorForm.userId = inspector.userId
  inspectorForm.permissions = inspector.permissions.map(p => ({
    categoryId: p.categoryId,
    categoryName: p.categoryName,
    classIds: p.classIds ? [...p.classIds] : []
  }))
  inspectorForm.remark = inspector.remark || ''
  expandedClassConfigs.value.clear()
  showInspectorDialog.value = true
}

const handleSaveInspector = async () => {
  if (!inspectorForm.userId) {
    ElMessage.warning('请选择打分人员')
    return
  }
  if (inspectorForm.permissions.length === 0) {
    ElMessage.warning('请至少配置一个检查类别的权限')
    return
  }

  try {
    if (editingInspector.value) {
      // 更新
      await updateInspector(planId.value, editingInspector.value.id, {
        id: editingInspector.value.id,
        permissions: inspectorForm.permissions,
        remark: inspectorForm.remark
      })
      ElMessage.success('更新成功')
    } else {
      // 新增
      await addInspector(planId.value, {
        userId: inspectorForm.userId,
        permissions: inspectorForm.permissions,
        remark: inspectorForm.remark
      })
      ElMessage.success('添加成功')
    }
    showInspectorDialog.value = false
    loadInspectors()
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  }
}

const handleDeleteInspector = async (inspector: InspectorDTO) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除打分人员 ${inspector.userName} 吗？`,
      '确认删除',
      { type: 'warning' }
    )
    await deleteInspector(planId.value, inspector.id)
    ElMessage.success('删除成功')
    loadInspectors()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

// 移除打分人员（别名）
const handleRemoveInspector = handleDeleteInspector

const addPermissionConfig = () => {
  inspectorForm.permissions.push({
    categoryId: '',
    categoryName: '',
    classIds: undefined
  })
}

const removePermissionConfig = (index: number) => {
  inspectorForm.permissions.splice(index, 1)
}

const onCategoryChange = (index: number, categoryId: string) => {
  const category = templateSnapshot.value?.categories?.find(c => c.categoryId === categoryId)
  if (category) {
    inspectorForm.permissions[index].categoryName = category.categoryName
  }
}

// 获取可选的检查类别（排除已选的）
const getAvailableCategories = (currentIndex: number) => {
  const selectedIds = inspectorForm.permissions
    .map((p, i) => i !== currentIndex ? p.categoryId : null)
    .filter(Boolean)
  return (templateSnapshot.value?.categories || []).filter(c => !selectedIds.includes(c.categoryId))
}

// 获取模板类别列表（用于评级规则配置）
const templateCategories = computed(() => {
  if (!templateSnapshot.value?.categories) return []
  return templateSnapshot.value.categories.map(cat => ({
    id: cat.categoryId,
    categoryName: cat.categoryName
  }))
})

// 所有扣分项（用于导出模板配置）
const allDeductionItems = computed(() => {
  if (!templateSnapshot.value?.categories) return []
  const items: Array<{ id: string | number; itemName: string; name: string; categoryName: string }> = []
  templateSnapshot.value.categories.forEach(cat => {
    if (cat.deductionItems) {
      cat.deductionItems.forEach(item => {
        items.push({
          id: item.itemId,
          name: item.itemName,
          categoryName: cat.categoryName || '其他',
          itemName: item.itemName
        })
      })
    }
  })
  return items
})

// 返回
const goBack = () => {
  router.push('/quantification/check-plan')
}

// 跳转到智能统计分析页面
const goToSmartStatistics = () => {
  router.push(`/quantification/check-plan/${planId.value}/smart-statistics`)
}

// 跳转到评级频次统计页面
const goToRatingFrequency = () => {
  router.push({
    path: `/quantification/check-plan/${planId.value}/rating-frequency`,
    query: { planName: planDetail.value?.planName }
  })
}

// 跳转到评级审核管理页面
const goToRatingAudit = () => {
  router.push({
    path: `/quantification/check-plan/${planId.value}/rating-audit`,
    query: { planName: planDetail.value?.planName }
  })
}

// 开始计划
const handleStart = async () => {
  try {
    await ElMessageBox.confirm('确定要开始此计划吗？开始后可以创建日常检查。', '开始计划', {
      confirmButtonText: '确定开始',
      cancelButtonText: '取消'
    })
    await startPlan(planId.value)
    ElMessage.success('计划已开始')
    loadPlanDetail()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '操作失败')
    }
  }
}

// 结束计划
const handleFinish = async () => {
  try {
    await ElMessageBox.confirm('确定要结束此计划吗？结束后将无法再添加检查。', '结束计划', {
      confirmButtonText: '确定结束',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await finishPlan(planId.value)
    ElMessage.success('计划已结束')
    loadPlanDetail()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '操作失败')
    }
  }
}

// 归档计划
const handleArchive = async () => {
  try {
    await ElMessageBox.confirm('确定要归档此计划吗？', '归档确认')
    await archivePlan(planId.value)
    ElMessage.success('归档成功')
    loadPlanDetail()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '归档失败')
    }
  }
}

// 编辑计划
const handleEdit = () => {
  editForm.planName = planDetail.value?.planName || ''
  editForm.description = planDetail.value?.description || ''
  editForm.startDate = planDetail.value?.startDate || ''
  editForm.endDate = planDetail.value?.endDate || ''
  showEditDialog.value = true
}

// 保存编辑
const saveEdit = async () => {
  if (!editForm.planName) {
    ElMessage.warning('请输入计划名称')
    return
  }
  saving.value = true
  try {
    await updateCheckPlan({
      id: planId.value,
      planName: editForm.planName,
      description: editForm.description,
      startDate: editForm.startDate,
      endDate: editForm.endDate
    })
    ElMessage.success('保存成功')
    showEditDialog.value = false
    loadPlanDetail()
  } catch (error: any) {
    ElMessage.error(error.message || '保存失败')
  } finally {
    saving.value = false
  }
}

// 删除计划
const handleDelete = async () => {
  try {
    await ElMessageBox.confirm('确定要删除此计划吗？此操作不可恢复！', '删除确认', {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteCheckPlan(planId.value)
    ElMessage.success('删除成功')
    router.push('/quantification/check-plan')
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

// 加载班级列表
const loadClassList = async () => {
  try {
    const res = await getClassList({ pageNum: 1, pageSize: 10000 })
    classList.value = res.records || []
  } catch (e) {
    console.error('加载班级失败', e)
  }
}

// 加载计划目标班级列表（根据计划的目标范围配置）
const loadPlanTargetClasses = async () => {
  try {
    const res = await getPlanTargetClasses(planId.value)
    // API返回的班级数据，需要转换ID类型以匹配Class接口
    planTargetClasses.value = (res || []).map((c: any) => ({
      ...c,
      id: typeof c.id === 'string' ? parseInt(c.id, 10) : c.id,
      departmentId: typeof c.orgUnitId === 'string' ? parseInt(c.orgUnitId, 10) : c.orgUnitId,
      gradeId: typeof c.gradeId === 'string' ? parseInt(c.gradeId, 10) : c.gradeId
    }))
  } catch (e) {
    console.error('加载计划目标班级失败', e)
    planTargetClasses.value = []
  }
}

// 加载年级列表
const loadGradeList = async () => {
  try {
    gradeList.value = await getAllGrades() || []
  } catch (e) {
    console.error('加载年级失败', e)
  }
}

// 加载院系列表
const loadDepartmentList = async () => {
  try {
    departmentList.value = await getAllEnabledDepartments() || []
  } catch (e) {
    console.error('加载院系失败', e)
  }
}

// 自动生成检查名称
const autoGenerateCheckName = () => {
  if (!dailyCheckForm.checkDate) return
  const date = dailyCheckForm.checkDate
  const typeText = dailyCheckForm.checkType === 1 ? '日常检查' : '专项检查'
  const templateName = planDetail.value?.templateName || ''
  dailyCheckForm.checkName = `${date} ${templateName || typeText}`
}

// 重置日常检查表单
const resetDailyCheckForm = () => {
  dailyCheckForm.checkDate = ''
  dailyCheckForm.checkName = ''
  dailyCheckForm.checkType = 1
  dailyCheckForm.description = ''
  dailyCheckForm.targets = []
  dailyCheckForm.categories = []
  dailyCheckForm.excludedTargets = []
  dailyCheckForm.totalRounds = 1
  dailyCheckForm.roundNames = []
  showExcludeSection.value = false
  currentEditRound.value = 1
}

// ============ 轮次管理相关 ============

// 添加类别菜单状态
const addCategoryMenuVisible = ref(false)
const addCategoryMenuRound = ref(1)
const addCategoryMenuStyle = ref<{ top: string; left: string }>({ top: '0px', left: '0px' })

// 当前编辑的轮次
const currentEditRound = ref(1)

// 解析类别的参与轮次
const parseCategoryRounds = (cat: CheckCategoryItem): number[] => {
  if (!cat.participatedRounds) {
    // 默认参与所有轮次
    return Array.from({ length: dailyCheckForm.totalRounds }, (_, i) => i + 1)
  }
  return cat.participatedRounds.split(',').map(s => parseInt(s.trim())).filter(n => !isNaN(n))
}

// 获取某轮次包含的类别
const getCategoriesForRound = (round: number): CheckCategoryItem[] => {
  return dailyCheckForm.categories.filter(cat => {
    const rounds = parseCategoryRounds(cat)
    return rounds.includes(round)
  })
}

// 获取某轮次可添加的类别（不在该轮次中的类别）
const getAvailableCategoriesForRound = (round: number): CheckCategoryItem[] => {
  return dailyCheckForm.categories.filter(cat => {
    const rounds = parseCategoryRounds(cat)
    return !rounds.includes(round)
  })
}

// 添加轮次
const handleAddRound = () => {
  dailyCheckForm.totalRounds++
  // 为新轮次添加空的名称占位
  while (dailyCheckForm.roundNames.length < dailyCheckForm.totalRounds) {
    dailyCheckForm.roundNames.push('')
  }
  // 新轮次自动包含所有类别
  const newRound = dailyCheckForm.totalRounds
  dailyCheckForm.categories.forEach(cat => {
    const rounds = parseCategoryRounds(cat)
    if (!rounds.includes(newRound)) {
      rounds.push(newRound)
      rounds.sort((a, b) => a - b)
      cat.participatedRounds = rounds.join(',')
    }
  })
  // 自动切换到新轮次
  currentEditRound.value = newRound
}

// 删除轮次
const handleDeleteRound = (round: number) => {
  if (dailyCheckForm.totalRounds <= 1) return

  // 从所有类别中移除该轮次
  dailyCheckForm.categories.forEach(cat => {
    const rounds = parseCategoryRounds(cat)
    const newRounds = rounds.filter(r => r !== round).map(r => r > round ? r - 1 : r)
    cat.participatedRounds = newRounds.length > 0 ? newRounds.join(',') : '1'
  })

  // 移除轮次名称
  dailyCheckForm.roundNames.splice(round - 1, 1)
  dailyCheckForm.totalRounds--

  // 调整当前编辑轮次
  if (currentEditRound.value > dailyCheckForm.totalRounds) {
    currentEditRound.value = dailyCheckForm.totalRounds
  } else if (currentEditRound.value > round) {
    currentEditRound.value--
  }
}

// 从轮次中移除类别
const handleRemoveCategoryFromRound = (cat: CheckCategoryItem, round: number) => {
  // 单轮次模式：直接从列表中删除
  if (dailyCheckForm.totalRounds === 1) {
    const index = dailyCheckForm.categories.findIndex(c => c.categoryId === cat.categoryId)
    if (index > -1) {
      if (dailyCheckForm.categories.length <= 1) {
        ElMessage.warning('至少保留一个检查类别')
        return
      }
      dailyCheckForm.categories.splice(index, 1)
    }
    return
  }

  // 多轮次模式：从指定轮次中移除
  const rounds = parseCategoryRounds(cat)
  const newRounds = rounds.filter(r => r !== round)

  // 如果移除后该类别不参与任何轮次，则从列表中删除
  if (newRounds.length === 0) {
    const index = dailyCheckForm.categories.findIndex(c => c.categoryId === cat.categoryId)
    if (index > -1) {
      if (dailyCheckForm.categories.length <= 1) {
        ElMessage.warning('至少保留一个检查类别')
        return
      }
      dailyCheckForm.categories.splice(index, 1)
    }
    return
  }

  cat.participatedRounds = newRounds.join(',')
}

// 显示添加类别菜单
const showAddCategoryMenu = (round: number, event: MouseEvent) => {
  const target = event.target as HTMLElement
  const rect = target.getBoundingClientRect()
  addCategoryMenuStyle.value = {
    top: `${rect.bottom + 4}px`,
    left: `${rect.left}px`
  }
  addCategoryMenuRound.value = round
  addCategoryMenuVisible.value = true

  // 点击其他地方关闭菜单
  const closeMenu = () => {
    addCategoryMenuVisible.value = false
    document.removeEventListener('click', closeMenu)
  }
  setTimeout(() => {
    document.addEventListener('click', closeMenu)
  }, 0)
}

// 添加类别到轮次
const handleAddCategoryToRound = (cat: CheckCategoryItem, round: number) => {
  const rounds = parseCategoryRounds(cat)
  if (!rounds.includes(round)) {
    rounds.push(round)
    rounds.sort((a, b) => a - b)
    cat.participatedRounds = rounds.join(',')
  }
  addCategoryMenuVisible.value = false
}

// 检查班级是否被排除
const isClassExcluded = (classId: number) => {
  return dailyCheckForm.excludedTargets.some(e => e.classId === classId)
}

// 切换班级排除状态
const toggleExcludeClass = (cls: Class) => {
  const index = dailyCheckForm.excludedTargets.findIndex(e => e.classId === cls.id)
  if (index >= 0) {
    dailyCheckForm.excludedTargets.splice(index, 1)
  } else {
    dailyCheckForm.excludedTargets.push({
      classId: cls.id,
      className: cls.className,
      reason: ''
    })
  }
}

// 移除排除的班级
const removeExcludedClass = (index: number) => {
  dailyCheckForm.excludedTargets.splice(index, 1)
}

// 创建日常检查 - 打开对话框
const handleCreateDailyCheck = () => {
  if (!planDetail.value || planDetail.value.status !== 1) {
    ElMessage.warning('只有进行中的计划才能创建日常检查')
    return
  }

  // 重置表单
  resetDailyCheckForm()

  // 设置默认日期为今天
  dailyCheckForm.checkDate = new Date().toISOString().split('T')[0]
  autoGenerateCheckName()

  // 从计划的模板快照中获取轮次配置
  if (templateSnapshot.value) {
    // 默认1轮次
    dailyCheckForm.totalRounds = 1
    dailyCheckForm.roundNames = ['']

    // 获取类别（包含关联类型和轮次配置）
    if (templateSnapshot.value.categories) {
      dailyCheckForm.categories = templateSnapshot.value.categories.map(cat => ({
        categoryId: cat.categoryId,
        categoryName: cat.categoryName,
        linkType: cat.linkType ?? 0,
        isRequired: cat.isRequired ?? 1,
        sortOrder: cat.sortOrder,
        // 默认所有类别都参与第1轮
        participatedRounds: '1'
      }))
    }
  }

  // 重置当前编辑轮次
  currentEditRound.value = 1

  showCreateDailyCheckDialog.value = true
}

// 下一步
const handleDailyCheckNextStep = () => {
  if (dailyCheckFormStep.value === 1) {
    if (!dailyCheckForm.checkName) {
      ElMessage.warning('请输入检查名称')
      return
    }
    if (!dailyCheckForm.checkDate) {
      ElMessage.warning('请选择检查日期')
      return
    }
  }
  dailyCheckFormStep.value++
}

// 检查目标是否已选择
const isTargetSelected = (type: number, id: number, name?: string) => {
  if (type === 2) return dailyCheckForm.targets.some(t => t.targetType === type && t.targetName === name)
  return dailyCheckForm.targets.some(t => t.targetType === type && t.targetId === id)
}

// 切换目标选择
const toggleTarget = (type: number, id: number, name: string) => {
  const index = type === 2
    ? dailyCheckForm.targets.findIndex(t => t.targetType === type && t.targetName === name)
    : dailyCheckForm.targets.findIndex(t => t.targetType === type && t.targetId === id)

  if (index > -1) {
    dailyCheckForm.targets.splice(index, 1)
  } else {
    dailyCheckForm.targets.push({ targetType: type, targetId: id, targetName: name })
  }
}

// 移除目标
const handleRemoveTarget = (index: number) => {
  dailyCheckForm.targets.splice(index, 1)
}

// 提交创建日常检查
const handleSubmitDailyCheck = async () => {
  if (!dailyCheckForm.checkName || !dailyCheckForm.checkDate) {
    ElMessage.error('请填写检查名称和日期')
    return
  }
  if (actualCheckClassCount.value === 0) {
    ElMessage.error('检查范围不能为空')
    return
  }

  // 构建目标列表：使用实际检查的班级ID
  const targets: CheckTargetItem[] = actualCheckClassIds.value.map(classId => {
    const cls = planTargetClasses.value.find(c => c.id === classId)
    return {
      targetType: 1, // 按班级
      targetId: classId,
      targetName: cls?.className || ''
    }
  })

  submitLoading.value = true
  try {
    // 构建轮次名称数组（空字符串用默认名称替代）
    const processedRoundNames = dailyCheckForm.roundNames.map((name, index) =>
      name.trim() || `第${index + 1}轮`
    )

    // 构建类别数据，确保每个类别都有有效的 participatedRounds
    const processedCategories = dailyCheckForm.categories.map(cat => ({
      ...cat,
      // 如果没有设置 participatedRounds，默认参与所有轮次
      participatedRounds: cat.participatedRounds ||
        Array.from({ length: dailyCheckForm.totalRounds }, (_, i) => i + 1).join(',')
    }))

    await http.post('/quantification/daily-checks', {
      planId: planId.value,
      templateId: planDetail.value?.templateId,
      checkDate: dailyCheckForm.checkDate,
      checkName: dailyCheckForm.checkName,
      checkType: dailyCheckForm.checkType,
      description: dailyCheckForm.description,
      targets: targets,
      categories: processedCategories,
      // 传递轮次配置
      totalRounds: dailyCheckForm.totalRounds,
      roundNames: processedRoundNames,
      // 传递排除信息，后端可以记录
      excludedTargets: dailyCheckForm.excludedTargets.length > 0 ? dailyCheckForm.excludedTargets : undefined
    })
    ElMessage.success('日常检查创建成功')
    showCreateDailyCheckDialog.value = false
    loadDailyChecks()
    loadPlanDetail() // 刷新统计数据
  } catch (error: any) {
    ElMessage.error(error.message || '创建失败')
  } finally {
    submitLoading.value = false
  }
}

// 查看日常检查详情
const handleViewDailyCheck = (check: any) => {
  router.push(`/quantification/daily-check/${check.id}`)
}

// 开始日常检查
const handleStartDailyCheck = async (check: any) => {
  try {
    await ElMessageBox.confirm('确认开始此检查吗？', '开始检查')
    await http.patch(`/quantification/daily-checks/${check.id}/status`, null, { params: { status: 1 } })
    ElMessage.success('已开始')
    loadDailyChecks()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '操作失败')
    }
  }
}

// 结束日常检查
const handleFinishDailyCheck = async (check: any) => {
  try {
    await ElMessageBox.confirm('确认结束此检查吗？', '结束检查', { type: 'warning' })
    await http.patch(`/quantification/daily-checks/${check.id}/status`, null, { params: { status: 2 } })
    ElMessage.success('已结束')
    loadDailyChecks()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '操作失败')
    }
  }
}

// 打分
const handleScoring = (check: any) => {
  router.push({ path: '/quantification/check-record-scoring', query: { checkId: String(check.id), from: route.fullPath } })
}

// 删除日常检查
const handleDeleteDailyCheck = async (check: any) => {
  try {
    await ElMessageBox.confirm(`确定删除"${check.checkName}"吗？`, '删除确认', { type: 'warning' })
    await http.delete(`/quantification/daily-checks/${check.id}`)
    ElMessage.success('删除成功')
    loadDailyChecks()
    loadPlanDetail() // 刷新统计
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

// 查看检查记录详情
const viewRecord = (id: string | number) => {
  router.push(`/quantification/check-record/${id}`)
}

// 处理查看检查记录事件
const handleViewRecord = (record: any) => {
  viewRecord(record.id)
}

// 计算检查记录总扣分
const totalRecordDeduction = computed(() => {
  return checkRecords.value.reduce((sum, record) => {
    const score = record.totalScore || record.totalDeductionScore || 0
    return sum + Number(score)
  }, 0)
})

// 检查记录状态样式
const getRecordStatusClass = (status: number) => {
  const map: Record<number, string> = {
    1: 'published',
    2: 'archived'
  }
  return map[status] || 'published'
}

// 检查记录状态文本
const getRecordStatusText = (status: number) => {
  const map: Record<number, string> = {
    1: '已发布',
    2: '已归档'
  }
  return map[status] || '已发布'
}

// 导出检查记录
const handleExportRecord = async (record: any) => {
  try {
    ElMessage.info('正在导出...')
    const blob = await exportCheckRecord(record.id)
    const url = window.URL.createObjectURL(blob as Blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `检查记录_${record.recordCode || record.checkName}_${record.checkDate}.xlsx`
    link.click()
    window.URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch (error: any) {
    ElMessage.error(error.message || '导出失败')
  }
}

// 归档检查记录
const handleArchiveRecord = async (record: any) => {
  try {
    await ElMessageBox.confirm(`确定要归档"${record.checkName}"吗？`, '归档确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'info'
    })
    await publishCheckRecord(record.id)
    ElMessage.success('归档成功')
    loadCheckRecords()
    loadPlanDetail() // 刷新统计
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '归档失败')
    }
  }
}

// 删除检查记录
const handleDeleteRecord = async (record: any) => {
  try {
    await ElMessageBox.confirm(`确定要删除"${record.checkName}"吗？此操作不可恢复！`, '删除确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteCheckRecord(record.id)
    ElMessage.success('删除成功')
    loadCheckRecords()
    loadPlanDetail() // 刷新统计
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

onMounted(() => {
  loadPlanDetail()
  loadDailyChecks()
  loadCheckRecords()
  // loadRatingRules() // 已废弃，使用新的评级系统
  loadInspectors()
  loadClassList()
  loadGradeList()
  loadDepartmentList()
  loadPlanTargetClasses()
})
</script>

<style scoped>
.check-plan-detail {
  min-height: 100vh;
  background: #f8fafc;
}

/* 顶部导航栏 */
.top-header {
  position: sticky;
  top: 0;
  z-index: 50;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  background: white;
  border-bottom: 1px solid #e5e7eb;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.back-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: #f1f5f9;
  border: none;
  color: #64748b;
  cursor: pointer;
  transition: all 0.15s;
}

.back-btn:hover {
  background: #e2e8f0;
  color: #334155;
}

.header-title {
  font-size: 18px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0;
}

.header-subtitle {
  font-size: 13px;
  color: #64748b;
  margin: 4px 0 0;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.action-buttons {
  display: flex;
  gap: 8px;
}

/* 状态徽章 */
.status-badge {
  padding: 6px 12px;
  border-radius: 16px;
  font-size: 13px;
  font-weight: 500;
}

.status-badge.sm {
  padding: 4px 10px;
  font-size: 12px;
}

.status-badge.draft { background: #f3f4f6; color: #6b7280; }
.status-badge.progress { background: #fef3c7; color: #d97706; }
.status-badge.finished { background: #d1fae5; color: #059669; }
.status-badge.archived { background: #e5e7eb; color: #6b7280; }
.status-badge.published { background: #dbeafe; color: #1d4ed8; }

/* 按钮 */
.btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  border: none;
  transition: all 0.2s;
}

.btn-sm { padding: 6px 12px; font-size: 13px; }

.btn-primary { background: #2563eb; color: white; }
.btn-primary:hover { background: #1d4ed8; }

.btn-success { background: #059669; color: white; }
.btn-success:hover { background: #047857; }

.btn-warning { background: #d97706; color: white; }
.btn-warning:hover { background: #b45309; }

.btn-danger { background: #dc2626; color: white; }
.btn-danger:hover { background: #b91c1c; }

.btn-default { background: white; color: #374151; border: 1px solid #d1d5db; }
.btn-default:hover { background: #f9fafb; }

.btn:disabled { opacity: 0.5; cursor: not-allowed; }

.icon { width: 18px; height: 18px; }
.icon-sm { width: 16px; height: 16px; }

/* 加载状态 */
.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 100px 20px;
  color: #9ca3af;
}

.spinner {
  width: 32px;
  height: 32px;
  animation: spin 1s linear infinite;
  margin-bottom: 12px;
}

.spinner-sm {
  width: 20px;
  height: 20px;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* 主内容 */
.main-content {
  padding: 24px;
  max-width: 1400px;
  margin: 0 auto;
}

/* Tab导航 */
.tab-nav {
  display: flex;
  gap: 4px;
  background: white;
  padding: 4px;
  border-radius: 10px;
  margin-bottom: 24px;
  border: 1px solid #e5e7eb;
}

.tab-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  color: #64748b;
  background: transparent;
  border: none;
  cursor: pointer;
  transition: all 0.15s;
}

.tab-btn:hover {
  color: #334155;
  background: #f1f5f9;
}

.tab-btn.active {
  color: #2563eb;
  background: #eff6ff;
}

/* Tab内容 */
.tab-content {
  background: white;
  border-radius: 12px;
  padding: 24px;
  border: 1px solid #e5e7eb;
}

/* 内容网格 */
.content-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
  margin-bottom: 24px;
}

/* 信息卡片 */
.info-card {
  background: #f9fafb;
  border-radius: 10px;
  padding: 20px;
}

.card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: #374151;
  margin: 0 0 16px;
}

.info-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.info-row .label {
  font-size: 13px;
  color: #6b7280;
}

.info-row .value {
  font-size: 14px;
  color: #1a1a1a;
  font-weight: 500;
}

.info-row .value.code {
  font-family: monospace;
  background: #e5e7eb;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 13px;
}

/* 统计盒子 */
.stat-boxes {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.stat-box {
  text-align: center;
  padding: 16px;
  background: white;
  border-radius: 8px;
}

.stat-box .stat-value {
  display: block;
  font-size: 24px;
  font-weight: 600;
  color: #1a1a1a;
}

.stat-box .stat-value.deduct {
  color: #dc2626;
}

.stat-box .stat-label {
  font-size: 12px;
  color: #9ca3af;
  margin-top: 4px;
}

/* 徽章 */
.badge {
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.badge.success { background: #d1fae5; color: #059669; }
.badge.default { background: #f3f4f6; color: #6b7280; }

/* 查看模板按钮 */
.view-template-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  width: 100%;
  padding: 10px;
  margin-top: 16px;
  background: white;
  border: 1px dashed #d1d5db;
  border-radius: 6px;
  font-size: 14px;
  color: #6b7280;
  cursor: pointer;
  transition: all 0.15s;
}

.view-template-btn:hover {
  border-color: #2563eb;
  color: #2563eb;
}

/* 功能卡片区域 */
.feature-cards-section {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  margin-bottom: 24px;
}

.feature-card {
  padding: 32px 24px;
  background: #f9fafb;
  border: 1px dashed #d1d5db;
  border-radius: 10px;
  text-align: center;
  transition: all 0.2s;
}

.feature-card.clickable {
  cursor: pointer;
  background: #fefefe;
  border: 1px solid #e5e7eb;
}

.feature-card.clickable:hover {
  border-color: #3b82f6;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.1);
}

.feature-card.disabled {
  opacity: 0.7;
}

.feature-icon {
  width: 40px;
  height: 40px;
  color: #d1d5db;
  margin: 0 auto 14px;
}

.feature-icon.template {
  color: #3b82f6;
}

.feature-icon.statistics {
  color: #10b981;
}

.feature-icon.smart {
  color: #8b5cf6;
}

.feature-icon.analysis {
  color: #8b5cf6;
}

.feature-icon.rating {
  color: #f59e0b;
}

.feature-icon.summary {
  color: #ec4899;
}

.feature-icon.frequency {
  color: #14b8a6;
}

.feature-icon.audit {
  color: #8b5cf6;
}

.feature-card h3 {
  font-size: 15px;
  font-weight: 600;
  color: #374151;
  margin: 0 0 6px;
}

.feature-card.disabled h3 {
  color: #6b7280;
}

.feature-card p {
  font-size: 13px;
  color: #9ca3af;
  margin: 0;
}

.feature-card.clickable p {
  color: #6b7280;
}

/* 模板弹窗样式 */
.dialog-template-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 16px;
  border-bottom: 1px solid #e5e7eb;
  margin-bottom: 16px;
}

.dialog-template-name {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
}

.dialog-template-badges {
  display: flex;
  gap: 10px;
}

.info-badge {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 5px 12px;
  background: #f3f4f6;
  color: #6b7280;
  border-radius: 16px;
  font-size: 12px;
}

.info-badge.success {
  background: #d1fae5;
  color: #059669;
}

.dialog-template-categories {
  max-height: 500px;
  overflow-y: auto;
}

.template-category {
  margin-bottom: 12px;
}

.template-category:last-child {
  margin-bottom: 0;
}

.category-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 14px;
  background: #f9fafb;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.15s;
}

.category-header:hover {
  background: #f1f5f9;
}

.category-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.category-left .expand-icon {
  color: #94a3b8;
  transition: transform 0.2s;
}

.category-left .expand-icon.expanded {
  transform: rotate(90deg);
}

.category-left .folder-icon {
  color: #f59e0b;
}

.category-left .category-name {
  font-size: 14px;
  font-weight: 500;
  color: #1a1a1a;
}

.category-left .required-tag {
  padding: 2px 6px;
  background: #fef2f2;
  color: #dc2626;
  border-radius: 4px;
  font-size: 10px;
  font-weight: 600;
}

.category-right .item-count {
  font-size: 12px;
  color: #94a3b8;
  background: #e5e7eb;
  padding: 3px 10px;
  border-radius: 12px;
}

/* 详情页扣分项卡片网格 */
.detail-items-grid {
  position: relative;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 12px;
  padding: 12px;
  margin-top: 8px;
  margin-left: 20px;
  background: #fafbfc;
  border-left: 2px solid #e5e7eb;
  border-radius: 0 8px 8px 0;
}

.detail-item-card {
  position: relative;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  overflow: hidden;
}

.detail-item-card.has-weight {
  border-color: #c7d2fe;
  border-left: 3px solid #6366f1;
}

.detail-item-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.detail-item-header {
  position: relative;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
  padding: 10px;
  background: #f8fafc;
  border-bottom: 1px solid #f1f5f9;
}

.detail-item-header.weighted {
  background: #eef2ff;
  border-bottom-color: #c7d2fe;
}

.detail-item-name {
  position: relative;
  flex: 1;
  min-width: 0;
  order: 1;
  font-size: 12px;
  font-weight: 500;
  color: #334155;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.detail-item-score {
  position: relative;
  order: 2;
  font-size: 10px;
  font-weight: 700;
  padding: 2px 6px;
  border-radius: 10px;
  flex-shrink: 0;
  white-space: nowrap;
}

.detail-item-score.fixed {
  background: #fef2f2;
  color: #dc2626;
}

.detail-item-score.dynamic {
  background: #fffbeb;
  color: #d97706;
}

.detail-item-score.range {
  background: #eff6ff;
  color: #2563eb;
}

.detail-item-footer {
  padding: 8px 10px;
  background: #fff;
}

.detail-weight-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 8px;
  border-radius: 12px;
  font-size: 10px;
  font-weight: 600;
  color: #fff;
}

.detail-no-weight {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 8px;
  border-radius: 12px;
  font-size: 10px;
  font-weight: 500;
  background: #f3f4f6;
  color: #9ca3af;
}

/* 区块头部 */
.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.section-header h3 {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0;
}

/* 内联加载/空状态 */
.loading-inline,
.empty-inline {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  color: #9ca3af;
}

.empty-icon {
  width: 48px;
  height: 48px;
  color: #d1d5db;
  margin-bottom: 12px;
}

.empty-hint {
  font-size: 13px;
  color: #9ca3af;
  margin-top: 8px;
}

/* 检查记录卡片网格 */
.record-cards-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 16px;
}

.record-card {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.record-card:hover {
  border-color: #3b82f6;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.15);
  transform: translateY(-2px);
}

.record-card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 16px 16px 12px;
  border-bottom: 1px solid #f3f4f6;
}

.record-card-header .check-info {
  flex: 1;
}

.record-card-header .check-name {
  font-size: 15px;
  font-weight: 600;
  color: #1f2937;
  margin: 0 0 4px;
}

.record-card-header .check-date {
  font-size: 13px;
  color: #6b7280;
}

.record-card-body {
  padding: 16px;
}

.stat-grid-4 {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
}

.stat-grid-4 .stat-item {
  text-align: center;
  padding: 8px 4px;
  background: #f9fafb;
  border-radius: 8px;
}

.stat-grid-4 .stat-value {
  display: block;
  font-size: 18px;
  font-weight: 700;
  color: #1f2937;
}

.stat-grid-4 .stat-value.deduct {
  color: #ef4444;
}

.stat-grid-4 .stat-label {
  display: block;
  font-size: 11px;
  color: #9ca3af;
  margin-top: 4px;
}

.record-card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-top: 1px solid #f3f4f6;
  background: #fafafa;
  border-radius: 0 0 12px 12px;
}

.record-card-footer .meta-info {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 12px;
  color: #6b7280;
}

.record-card-footer .checker {
  display: flex;
  align-items: center;
  gap: 4px;
}

.record-card-footer .code {
  font-family: monospace;
  color: #9ca3af;
}

.record-card-footer .card-actions {
  display: flex;
  gap: 4px;
}

.record-card-footer .action-btn {
  padding: 6px;
  background: transparent;
  border: none;
  color: #6b7280;
  cursor: pointer;
  border-radius: 6px;
  transition: all 0.15s ease;
}

.record-card-footer .action-btn:hover {
  background: #e5e7eb;
  color: #3b82f6;
}

.record-card-footer .action-btn.danger:hover {
  background: #fee2e2;
  color: #ef4444;
}

.icon-xs {
  width: 12px;
  height: 12px;
}

/* 数据表格 */
.data-table {
  overflow-x: auto;
}

.data-table table {
  width: 100%;
  border-collapse: collapse;
}

.data-table th,
.data-table td {
  padding: 12px 16px;
  text-align: left;
  border-bottom: 1px solid #e5e7eb;
}

.data-table th {
  font-size: 13px;
  font-weight: 600;
  color: #6b7280;
  background: #f9fafb;
}

.data-table td {
  font-size: 14px;
  color: #374151;
}

.data-table td.code {
  font-family: monospace;
  font-size: 13px;
  color: #6b7280;
}

.data-table td.deduct {
  color: #dc2626;
  font-weight: 500;
}

.data-table tr:hover {
  background: #f9fafb;
}

.action-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 6px;
  border: none;
  background: transparent;
  color: #6b7280;
  cursor: pointer;
  transition: all 0.15s;
}

.action-btn:hover {
  background: #eff6ff;
  color: #2563eb;
}

/* 模态框 */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background: white;
  border-radius: 12px;
  width: 100%;
  max-width: 500px;
  max-height: 90vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.modal-content.modal-lg {
  max-width: 700px;
}

.modal-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 20px;
  border-bottom: 1px solid #e5e7eb;
}

.modal-header h3 {
  font-size: 18px;
  font-weight: 600;
  margin: 0;
  flex: 1;
}

.snapshot-time {
  font-size: 12px;
  color: #9ca3af;
}

.close-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 6px;
  border: none;
  background: transparent;
  color: #9ca3af;
  cursor: pointer;
}

.close-btn:hover {
  background: #f3f4f6;
  color: #1a1a1a;
}

.modal-body {
  padding: 20px;
  overflow-y: auto;
  flex: 1;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 16px 20px;
  border-top: 1px solid #e5e7eb;
}

/* 表单样式 */
.form-item {
  margin-bottom: 16px;
}

.form-item label {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: #374151;
  margin-bottom: 6px;
}

.form-item input,
.form-item textarea {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 14px;
}

.form-item input:focus,
.form-item textarea:focus {
  outline: none;
  border-color: #2563eb;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

/* 模板快照弹窗 - 加大尺寸 */
.modal-content.modal-xl {
  max-width: 900px;
}

.modal-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
}

.modal-header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.template-name-tag {
  padding: 4px 10px;
  background: #eff6ff;
  color: #2563eb;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 500;
}

.snapshot-time {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #9ca3af;
}

.snapshot-modal-body {
  max-height: 60vh;
  overflow-y: auto;
}

/* 卡片式类别网格 */
.snapshot-categories-grid {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* 类别卡片 */
.category-card {
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
}

.category-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.category-title {
  display: flex;
  align-items: center;
  gap: 10px;
}

.category-card-header .category-name {
  font-size: 16px;
  font-weight: 600;
  color: white;
}

.category-card-header .category-code {
  padding: 2px 8px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 4px;
  font-size: 12px;
  font-family: monospace;
  color: rgba(255, 255, 255, 0.9);
}

.category-badges {
  display: flex;
  gap: 8px;
}

.cat-badge {
  padding: 4px 10px;
  border-radius: 20px;
  font-size: 11px;
  font-weight: 500;
}

.cat-badge.required {
  background: rgba(255, 255, 255, 0.25);
  color: white;
}

.cat-badge.link {
  background: rgba(255, 255, 255, 0.25);
  color: white;
}

.cat-badge.rounds {
  background: rgba(255, 255, 255, 0.25);
  color: white;
}

/* 扣分项容器 */
.deduction-items-container {
  padding: 16px 20px;
}

.deduction-items-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

/* 扣分项卡片 */
.deduction-item-card {
  background: #f9fafb;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 14px 16px;
  transition: all 0.15s ease;
}

.deduction-item-card:hover {
  border-color: #c7d2fe;
  background: #f5f7ff;
}

.item-main {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.deduction-item-card .item-name {
  font-size: 14px;
  font-weight: 500;
  color: #1a1a1a;
}

.item-tags {
  display: flex;
  gap: 6px;
}

.item-tag {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 6px;
  color: #6b7280;
}

.item-tag.photo {
  background: #fef3c7;
  color: #d97706;
}

.item-tag.remark {
  background: #dbeafe;
  color: #2563eb;
}

.item-tag.students {
  background: #d1fae5;
  color: #059669;
}

.item-score-info {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 4px;
}

.score-value {
  font-size: 15px;
  font-weight: 600;
  padding: 4px 12px;
  border-radius: 6px;
}

.score-value.fixed {
  background: #fee2e2;
  color: #dc2626;
}

.score-value.per-person {
  background: #fef3c7;
  color: #d97706;
}

.score-value.range {
  background: #dbeafe;
  color: #2563eb;
}

.score-type {
  font-size: 12px;
  color: #9ca3af;
}

.item-description {
  font-size: 12px;
  color: #6b7280;
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px dashed #e5e7eb;
}

.no-items {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 40px;
  color: #9ca3af;
  font-size: 14px;
}

/* 卡片底部 */
.category-card-footer {
  padding: 12px 20px;
  background: #f9fafb;
  border-top: 1px solid #e5e7eb;
}

.items-count {
  font-size: 13px;
  color: #6b7280;
}

/* 响应式 */
@media (max-width: 768px) {
  .top-header {
    flex-direction: column;
    gap: 16px;
    align-items: flex-start;
  }

  .header-right {
    width: 100%;
    justify-content: space-between;
  }

  .action-buttons {
    flex-wrap: wrap;
  }

  .content-grid {
    grid-template-columns: 1fr;
  }

  .feature-cards-section {
    grid-template-columns: 1fr;
  }

  .form-row {
    grid-template-columns: 1fr;
  }
}

/* Action button group */
.action-btn-group {
  display: flex;
  align-items: center;
  gap: 2px;
}

.action-btn.scoring:hover {
  background: #fef3c7;
  color: #d97706;
}

.action-btn.start:hover {
  background: #d1fae5;
  color: #059669;
}

.action-btn.finish:hover {
  background: #fed7aa;
  color: #c2410c;
}

.action-btn.delete:hover {
  background: #fee2e2;
  color: #dc2626;
}

.action-btn.export:hover {
  background: #e0e7ff;
  color: #4f46e5;
}

.action-btn.archive:hover {
  background: #f3f4f6;
  color: #374151;
}

/* Section stats */
.section-stats {
  display: flex;
  align-items: center;
  gap: 12px;
}

.stat-tag {
  padding: 4px 12px;
  background: #f3f4f6;
  border-radius: 16px;
  font-size: 13px;
  color: #6b7280;
}

.stat-tag.deduct {
  background: #fee2e2;
  color: #dc2626;
}

/* Deduct light color */
.data-table td.deduct-light {
  color: #f97316;
  font-weight: 500;
}

/* Icons */
.icon-xs { width: 12px; height: 12px; }

/* Step indicator for daily check dialog */
.step-indicator {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 24px;
}

.step-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.step-num {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 600;
  background: #e5e7eb;
  color: #6b7280;
  transition: all 0.2s;
}

.step-item.active .step-num {
  background: #2563eb;
  color: white;
}

.step-item.completed .step-num {
  background: #059669;
  color: white;
}

.step-item span {
  font-size: 13px;
  color: #6b7280;
}

.step-item.active span {
  color: #1a1a1a;
  font-weight: 500;
}

.step-line {
  width: 60px;
  height: 2px;
  background: #e5e7eb;
  margin: 0 12px;
  margin-bottom: 24px;
}

.step-line.completed {
  background: #059669;
}

.step-content-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* Info banner */
.info-banner {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  border-radius: 8px;
  font-size: 13px;
  color: #1e40af;
}

.info-banner strong {
  color: #1d4ed8;
}

/* 检查范围摘要横幅 */
.target-summary-banner {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 18px;
  background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%);
  border: 1px solid #7dd3fc;
  border-radius: 10px;
  margin-bottom: 16px;
}

.target-summary-banner .summary-content {
  flex: 1;
  font-size: 14px;
  color: #0369a1;
}

.target-summary-banner .exclude-info {
  color: #ea580c;
  font-weight: 500;
}

.toggle-exclude-btn {
  padding: 6px 14px;
  border: 1px solid #0ea5e9;
  border-radius: 6px;
  background: white;
  color: #0284c7;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.toggle-exclude-btn:hover {
  background: #0ea5e9;
  color: white;
}

/* 排除目标区域 */
.exclude-section {
  background: #fffbeb;
  border: 1px solid #fcd34d;
  border-radius: 10px;
  padding: 16px;
  margin-bottom: 16px;
}

.exclude-header {
  margin-bottom: 12px;
}

.exclude-title {
  font-size: 14px;
  font-weight: 600;
  color: #92400e;
}

.exclude-hint {
  display: block;
  font-size: 12px;
  color: #b45309;
  margin-top: 4px;
}

.exclude-class-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: 8px;
  margin-bottom: 12px;
}

.exclude-class-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 13px;
}

.exclude-class-item:hover {
  border-color: #f59e0b;
  background: #fef3c7;
}

.exclude-class-item.excluded {
  background: #fef3c7;
  border-color: #f59e0b;
}

.exclude-checkbox {
  width: 18px;
  height: 18px;
  border: 2px solid #d1d5db;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.exclude-class-item.excluded .exclude-checkbox {
  background: #f59e0b;
  border-color: #f59e0b;
  color: white;
}

/* 已排除列表 */
.excluded-list {
  border-top: 1px dashed #fcd34d;
  padding-top: 12px;
}

.excluded-list-header {
  font-size: 13px;
  font-weight: 500;
  color: #92400e;
  margin-bottom: 10px;
}

.excluded-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  background: white;
  border: 1px solid #fcd34d;
  border-radius: 6px;
  margin-bottom: 8px;
}

.excluded-class-name {
  font-size: 13px;
  font-weight: 500;
  color: #92400e;
  min-width: 100px;
}

.reason-input {
  flex: 1;
  padding: 6px 10px;
  border: 1px solid #d1d5db;
  border-radius: 4px;
  font-size: 13px;
}

.reason-input:focus {
  outline: none;
  border-color: #f59e0b;
}

.remove-exclude-btn {
  padding: 4px;
  border: none;
  background: transparent;
  color: #9ca3af;
  cursor: pointer;
  border-radius: 4px;
}

.remove-exclude-btn:hover {
  background: #fee2e2;
  color: #dc2626;
}

/* 日常检查表单 */
.daily-check-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* Form grid */
.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.form-item.required label::after {
  content: '*';
  color: #dc2626;
  margin-left: 4px;
}

/* Radio group */
.radio-group {
  display: flex;
  align-items: center;
  gap: 24px;
  height: 42px;
}

.radio-item {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.radio-item input {
  width: 16px;
  height: 16px;
  cursor: pointer;
}

.radio-item span {
  font-size: 14px;
  color: #374151;
}

/* Category config */
.category-config {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  overflow: hidden;
}

.category-list {
  background: white;
}

.category-row {
  display: flex;
  align-items: center;
  padding: 10px 16px;
  border-bottom: 1px solid #f3f4f6;
}

.category-row:last-child {
  border-bottom: none;
}

.cat-index {
  width: 24px;
  height: 24px;
  border-radius: 6px;
  background: #eff6ff;
  color: #2563eb;
  font-size: 12px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 12px;
}

.cat-name {
  flex: 1;
  font-size: 14px;
  color: #374151;
}

.cat-rounds {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 12px;
  background: #f9fafb;
  border-radius: 6px;
}

.cat-rounds span {
  font-size: 12px;
  color: #6b7280;
}

.cat-rounds select {
  padding: 4px 8px;
  border: 1px solid #d1d5db;
  border-radius: 4px;
  font-size: 13px;
  background: white;
  cursor: pointer;
}

.empty-categories {
  padding: 40px;
  text-align: center;
  font-size: 14px;
  color: #9ca3af;
}

/* ========== 单轮次视图 ========== */
.single-round-view {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.category-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.category-tag {
  display: inline-flex;
  align-items: center;
  padding: 8px 14px;
  background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
  border: 1px solid #bfdbfe;
  border-radius: 20px;
  font-size: 13px;
  color: #1e40af;
  font-weight: 500;
}

.tag-suffix {
  margin-left: 4px;
  font-size: 11px;
  color: #60a5fa;
  font-weight: 400;
}

.empty-hint {
  padding: 20px;
  text-align: center;
  color: #9ca3af;
  font-size: 13px;
}

.add-round-hint {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 10px;
  border: 1px dashed #d1d5db;
  border-radius: 8px;
  color: #9ca3af;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.add-round-hint:hover {
  border-color: #2563eb;
  color: #2563eb;
  background: #f8fafc;
}

/* ========== 多轮次视图 ========== */
.multi-round-view {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.round-card {
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  background: #fafafa;
  overflow: hidden;
}

.round-card-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  background: white;
  border-bottom: 1px solid #f3f4f6;
}

.round-badge {
  width: 24px;
  height: 24px;
  border-radius: 6px;
  background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%);
  color: white;
  font-size: 12px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
}

.round-name-edit {
  flex: 1;
  padding: 4px 8px;
  border: none;
  background: transparent;
  font-size: 14px;
  color: #374151;
}

.round-name-edit::placeholder {
  color: #9ca3af;
}

.round-name-edit:focus {
  outline: none;
  background: #f9fafb;
  border-radius: 4px;
}

.round-delete-btn {
  width: 26px;
  height: 26px;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: #d1d5db;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s;
}

.round-delete-btn:hover {
  background: #fef2f2;
  color: #ef4444;
}

.round-card-body {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  padding: 10px 12px;
  min-height: 44px;
}

.round-cat-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 5px 10px;
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  font-size: 13px;
  color: #374151;
}

.tag-remove {
  width: 16px;
  height: 16px;
  border: none;
  border-radius: 50%;
  background: transparent;
  color: #d1d5db;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s;
  margin-left: 2px;
}

.tag-remove:hover {
  background: #fee2e2;
  color: #ef4444;
}

.add-cat-tag {
  width: 28px;
  height: 28px;
  border: 1px dashed #d1d5db;
  border-radius: 6px;
  background: white;
  color: #9ca3af;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s;
}

.add-cat-tag:hover {
  border-color: #2563eb;
  color: #2563eb;
  background: #eff6ff;
}

.no-cat-hint {
  font-size: 12px;
  color: #9ca3af;
}

.add-round-card {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 12px;
  border: 1px dashed #d1d5db;
  border-radius: 10px;
  background: white;
  color: #6b7280;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.add-round-card:hover {
  border-color: #2563eb;
  color: #2563eb;
  background: #f8fafc;
}

/* ========== 类别下拉菜单 ========== */
.cat-dropdown {
  position: fixed;
  z-index: 9999;
  min-width: 140px;
  max-width: 200px;
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
  padding: 4px;
}

.cat-dropdown-item {
  padding: 8px 12px;
  font-size: 13px;
  color: #374151;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.15s;
}

.cat-dropdown-item:hover {
  background: #f3f4f6;
}

/* Target type buttons */
.target-type-btns {
  display: flex;
  gap: 8px;
}

.type-btn {
  padding: 8px 16px;
  border-radius: 6px;
  border: 1px solid #d1d5db;
  background: white;
  font-size: 14px;
  color: #374151;
  cursor: pointer;
  transition: all 0.15s;
}

.type-btn:hover {
  border-color: #93c5fd;
  background: #f9fafb;
}

.type-btn.active {
  border-color: #2563eb;
  background: #eff6ff;
  color: #2563eb;
}

/* Target selector */
.target-selector {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  overflow: hidden;
}

.selector-header {
  padding: 10px 16px;
  background: #f9fafb;
  border-bottom: 1px solid #e5e7eb;
  font-size: 13px;
  font-weight: 500;
  color: #374151;
}

.selector-body {
  max-height: 200px;
  overflow-y: auto;
  padding: 12px;
}

.target-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
}

.target-grid.cols-2 {
  grid-template-columns: repeat(2, 1fr);
}

.target-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  font-size: 13px;
  color: #374151;
  cursor: pointer;
  transition: all 0.15s;
}

.target-item:hover {
  border-color: #93c5fd;
  background: #f9fafb;
}

.target-item.selected {
  border-color: #2563eb;
  background: #eff6ff;
}

.target-item input {
  width: 14px;
  height: 14px;
  cursor: pointer;
}

.target-item span {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* Selected header */
.selected-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.selected-header .count {
  font-size: 12px;
  color: #9ca3af;
}

.empty-targets {
  padding: 40px;
  text-align: center;
  font-size: 14px;
  color: #9ca3af;
  background: #f9fafb;
  border: 1px dashed #d1d5db;
  border-radius: 8px;
}

.selected-targets {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.target-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.target-tag {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 8px 6px 12px;
  background: #f9fafb;
  border: 1px solid #e5e7eb;
  border-radius: 20px;
  font-size: 13px;
  color: #374151;
}

.target-tag .dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
}

.target-tag .dot.type-1 { background: #2563eb; }
.target-tag .dot.type-2 { background: #059669; }
.target-tag .dot.type-3 { background: #7c3aed; }

.target-tag .remove-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  border: none;
  background: transparent;
  color: #9ca3af;
  cursor: pointer;
  transition: all 0.15s;
}

.target-tag .remove-btn:hover {
  background: #fee2e2;
  color: #dc2626;
}

.class-count-info {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  background: #d1fae5;
  border-radius: 8px;
  font-size: 13px;
  color: #059669;
}

.class-count-info strong {
  font-weight: 600;
}

/* Modal footer modifications */
.modal-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  padding: 16px 20px;
  border-top: 1px solid #e5e7eb;
}

.footer-left {
  flex: 1;
}

.footer-right {
  display: flex;
  gap: 10px;
}

.spinner {
  animation: spin 1s linear infinite;
}

/* 分析配置弹窗样式 */
.analysis-dialog-content {
  min-height: 200px;
}

.analysis-header-info {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: #f0f9ff;
  border-radius: 8px;
  color: #0369a1;
  font-size: 14px;
  margin-bottom: 20px;
}

.analysis-config-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.analysis-config-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  transition: all 0.2s;
}

.analysis-config-card:hover {
  border-color: #3b82f6;
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.1);
}

.config-info h4 {
  margin: 0 0 6px 0;
  font-size: 15px;
  font-weight: 600;
  color: #1f2937;
}

.config-desc {
  margin: 0 0 8px 0;
  font-size: 13px;
  color: #6b7280;
}

.config-meta {
  display: flex;
  gap: 16px;
}

.config-meta .meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #9ca3af;
}

.config-actions {
  display: flex;
  gap: 8px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

/* 打分人员管理样式 */
.inspector-tab {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.inspector-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.inspector-header .section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  margin: 0;
}

.inspector-description {
  padding: 12px 16px;
  background: #f0f9ff;
  border: 1px solid #bae6fd;
  border-radius: 8px;
  font-size: 13px;
  color: #0369a1;
}

.inspector-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.inspector-card {
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  padding: 16px;
}

.inspector-main {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.inspector-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.inspector-name {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
}

.inspector-username {
  font-size: 13px;
  color: #6b7280;
}

.inspector-dept {
  padding: 2px 8px;
  background: #f3f4f6;
  border-radius: 4px;
  font-size: 12px;
  color: #6b7280;
}

.inspector-actions {
  display: flex;
  gap: 8px;
}

.inspector-permissions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  align-items: center;
}

.permission-label {
  font-size: 13px;
  color: #6b7280;
}

.permission-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.permission-tag {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  border-radius: 6px;
  font-size: 13px;
}

.permission-tag .category-name {
  color: #1d4ed8;
  font-weight: 500;
}

.permission-tag .class-scope {
  color: #6b7280;
  font-size: 12px;
}

.inspector-remark {
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px dashed #e5e7eb;
  font-size: 13px;
  color: #6b7280;
}

/* 新版打分人员弹窗样式 */
.inspector-form {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.form-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.section-label {
  font-size: 14px;
  font-weight: 600;
  color: #374151;
  display: flex;
  align-items: center;
  gap: 8px;
}

.section-label .required {
  color: #ef4444;
}

.section-hint {
  font-size: 12px;
  font-weight: 400;
  color: #9ca3af;
}

/* 用户搜索区域 */
.user-search-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.user-list-container {
  max-height: 200px;
  overflow-y: auto;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fafafa;
}

.user-list-loading,
.user-list-empty,
.user-list-hint {
  padding: 24px;
  text-align: center;
  color: #9ca3af;
  font-size: 14px;
}

.user-list-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.user-list {
  display: flex;
  flex-direction: column;
}

.user-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  cursor: pointer;
  transition: background 0.2s;
  border-bottom: 1px solid #f3f4f6;
}

.user-card:last-child {
  border-bottom: none;
}

.user-card:hover {
  background: #f0f9ff;
}

.user-card.selected {
  background: #eff6ff;
  border-left: 3px solid #3b82f6;
}

.user-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
  flex-shrink: 0;
}

.user-details {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.user-name {
  font-size: 14px;
  font-weight: 500;
  color: #1a1a1a;
}

.user-meta {
  font-size: 12px;
  color: #9ca3af;
}

.user-dept {
  font-size: 12px;
  color: #6b7280;
  padding: 2px 6px;
  background: #f3f4f6;
  border-radius: 4px;
  width: fit-content;
}

.check-icon {
  color: #3b82f6;
}

.selected-user-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: #f0f9ff;
  border: 1px solid #bfdbfe;
  border-radius: 8px;
}

/* 检查类别复选框网格 */
.category-checkbox-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: 10px;
}

.category-checkbox-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  background: white;
}

.category-checkbox-item:hover {
  border-color: #3b82f6;
  background: #f0f9ff;
}

.category-checkbox-item.checked {
  border-color: #3b82f6;
  background: #eff6ff;
}

.checkbox-indicator {
  width: 20px;
  height: 20px;
  border: 2px solid #d1d5db;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: all 0.2s;
}

.category-checkbox-item.checked .checkbox-indicator {
  background: #3b82f6;
  border-color: #3b82f6;
  color: white;
}

.category-label {
  font-size: 14px;
  color: #374151;
}

/* 班级权限配置 */
.class-permission-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.class-permission-item {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  overflow: hidden;
}

.permission-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 14px;
  background: #f9fafb;
  border-bottom: 1px solid #e5e7eb;
}

.category-tag {
  padding: 4px 10px;
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 500;
  color: #1d4ed8;
}

.class-scope-text {
  flex: 1;
  font-size: 13px;
  color: #6b7280;
}

.toggle-expand-btn {
  padding: 4px 10px;
  border: 1px solid #d1d5db;
  border-radius: 4px;
  background: white;
  font-size: 12px;
  color: #6b7280;
  cursor: pointer;
  transition: all 0.2s;
}

.toggle-expand-btn:hover {
  background: #f3f4f6;
  border-color: #9ca3af;
}

.class-checkbox-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 8px;
  padding: 12px;
  max-height: 200px;
  overflow-y: auto;
}

.class-checkbox-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 10px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  cursor: pointer;
  font-size: 13px;
  color: #374151;
  transition: all 0.2s;
}

.class-checkbox-item:hover {
  background: #f9fafb;
}

.class-checkbox-item input[type="checkbox"] {
  accent-color: #3b82f6;
}

/* ==================== 创建检查弹窗重构 ==================== */
.create-check-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 20px;
}

.create-check-dialog {
  width: 100%;
  max-width: 560px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.2);
  overflow: hidden;
}

.dialog-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%);
  color: white;
}

.header-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 16px;
  font-weight: 600;
}

.header-close {
  width: 32px;
  height: 32px;
  border: none;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.15);
  color: white;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s;
}

.header-close:hover {
  background: rgba(255, 255, 255, 0.25);
}

.dialog-content {
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  max-height: 60vh;
  overflow-y: auto;
}

/* 第一行内联表单 */
.form-row-inline {
  display: flex;
  gap: 12px;
}

.inline-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.inline-field label {
  font-size: 12px;
  color: #6b7280;
  font-weight: 500;
}

.inline-field input {
  padding: 8px 12px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  font-size: 14px;
  transition: border-color 0.2s;
}

.inline-field input:focus {
  outline: none;
  border-color: #2563eb;
}

.date-field { width: 140px; }
.type-field { width: 100px; }
.name-field { flex: 1; }

.type-switch {
  display: flex;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  overflow: hidden;
}

.type-option {
  flex: 1;
  padding: 8px 0;
  text-align: center;
  font-size: 13px;
  color: #6b7280;
  cursor: pointer;
  transition: all 0.2s;
}

.type-option:first-child {
  border-right: 1px solid #e5e7eb;
}

.type-option.active {
  background: #2563eb;
  color: white;
}

/* 检查范围 */
.scope-section {
  background: #f9fafb;
  border-radius: 10px;
  padding: 12px;
}

.scope-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.scope-info {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #374151;
}

.scope-info strong {
  color: #2563eb;
  font-size: 16px;
}

.exclude-count {
  color: #ef4444;
  font-size: 12px;
}

.scope-toggle {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 10px;
  border: none;
  border-radius: 6px;
  background: white;
  font-size: 12px;
  color: #6b7280;
  cursor: pointer;
  transition: all 0.2s;
}

.scope-toggle:hover {
  background: #e5e7eb;
}

.scope-toggle svg.rotated {
  transform: rotate(90deg);
}

.scope-expand {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #e5e7eb;
}

.class-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.class-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 6px 10px;
  border: 1px solid #e5e7eb;
  border-radius: 16px;
  background: white;
  font-size: 12px;
  color: #374151;
  cursor: pointer;
  transition: all 0.2s;
}

.class-chip:hover {
  border-color: #fca5a5;
  background: #fef2f2;
}

.class-chip.excluded {
  border-color: #ef4444;
  background: #fef2f2;
  color: #dc2626;
}

.chip-x {
  color: #ef4444;
}

.exclude-reasons {
  margin-top: 10px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.reason-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.reason-class {
  width: 80px;
  font-size: 12px;
  color: #dc2626;
  font-weight: 500;
  flex-shrink: 0;
}

.reason-row .reason-input {
  flex: 1;
  padding: 6px 10px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  font-size: 12px;
}

.reason-remove {
  width: 24px;
  height: 24px;
  border: none;
  border-radius: 4px;
  background: transparent;
  color: #9ca3af;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}

.reason-remove:hover {
  background: #fee2e2;
  color: #ef4444;
}

/* 检查类别区域 */
.category-section {
  background: #f9fafb;
  border-radius: 10px;
  padding: 12px;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.section-title {
  font-size: 13px;
  font-weight: 600;
  color: #374151;
}

/* 固定位置的添加轮次按钮 */
.add-round-btn-fixed {
  width: 28px;
  height: 28px;
  border: 1px dashed #d1d5db;
  border-radius: 6px;
  background: white;
  color: #9ca3af;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s;
  flex-shrink: 0;
}

.add-round-btn-fixed:hover {
  border-color: #2563eb;
  color: #2563eb;
  background: #eff6ff;
}

.category-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.cat-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 8px 12px;
  background: white;
  border: 1px solid #dbeafe;
  border-radius: 8px;
  font-size: 13px;
  color: #1e40af;
  font-weight: 500;
}

.chip-remove {
  width: 18px;
  height: 18px;
  border: none;
  border-radius: 50%;
  background: transparent;
  color: #93c5fd;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-left: 2px;
  transition: all 0.15s;
}

.chip-remove:hover {
  background: #fee2e2;
  color: #ef4444;
}

.chip-badge {
  font-size: 10px;
  padding: 2px 5px;
  background: #dbeafe;
  border-radius: 4px;
  color: #3b82f6;
}

.no-cat {
  color: #9ca3af;
  font-size: 13px;
}

/* 类别面板 */
.category-panel {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

/* 轮次标签页 */
.round-tabs {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.round-tab {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 10px;
  background: #f3f4f6;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.round-tab:hover {
  background: #e5e7eb;
}

.round-tab.active {
  background: #2563eb;
  border-color: #2563eb;
}

.round-tab.active .tab-num {
  background: white;
  color: #2563eb;
}

.round-tab.active .tab-name {
  color: white;
}

.round-tab.active .tab-name::placeholder {
  color: rgba(255, 255, 255, 0.6);
}

.round-tab.active .tab-del {
  color: rgba(255, 255, 255, 0.6);
}

.round-tab.active .tab-del:hover {
  color: white;
  background: rgba(255, 255, 255, 0.2);
}

.tab-num {
  width: 20px;
  height: 20px;
  border-radius: 5px;
  background: #2563eb;
  color: white;
  font-size: 11px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.tab-name {
  width: 60px;
  padding: 2px 4px;
  border: none;
  background: transparent;
  font-size: 12px;
  color: #374151;
}

.tab-name:focus {
  outline: none;
}

.tab-del {
  width: 18px;
  height: 18px;
  border: none;
  border-radius: 4px;
  background: transparent;
  color: #9ca3af;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}

.tab-del:hover {
  background: #fee2e2;
  color: #ef4444;
}

.tab-add {
  width: 32px;
  height: 32px;
  border: 1px dashed #d1d5db;
  border-radius: 8px;
  background: white;
  color: #9ca3af;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s;
}

.tab-add:hover {
  border-color: #2563eb;
  color: #2563eb;
  background: #eff6ff;
}

/* 添加类别按钮 */
.chip-add {
  width: 32px;
  height: 32px;
  border: 1px dashed #d1d5db;
  border-radius: 8px;
  background: white;
  color: #9ca3af;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s;
}

.chip-add:hover {
  border-color: #2563eb;
  color: #2563eb;
  background: #eff6ff;
}

/* 备注 */
.remark-section {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.remark-section label {
  font-size: 12px;
  color: #6b7280;
  font-weight: 500;
}

.remark-section textarea {
  padding: 10px 12px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  font-size: 13px;
  resize: none;
}

.remark-section textarea:focus {
  outline: none;
  border-color: #2563eb;
}

/* 底部按钮 */
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 16px 20px;
  background: #f9fafb;
  border-top: 1px solid #e5e7eb;
}

.btn-cancel {
  padding: 10px 20px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  background: white;
  font-size: 14px;
  color: #374151;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-cancel:hover {
  background: #f3f4f6;
}

.btn-submit {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 24px;
  border: none;
  border-radius: 8px;
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  font-size: 14px;
  font-weight: 500;
  color: white;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-submit:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(16, 185, 129, 0.4);
}

.btn-submit:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-submit .spin {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
</style>
