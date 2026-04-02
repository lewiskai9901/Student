<template>
  <!-- CLASS type: render ClassDetailPanel -->
  <ClassDetailPanel
    v-if="node.category === 'GROUP'"
    :node="node"
    :tree-data="treeData"
    @refresh="emit('refresh')"
    @edit="emit('edit', node)"
    @delete="emit('delete', node)"
    @toggle-status="emit('toggleStatus', node)"
  />

  <div v-else class="space-y-4">
    <!-- Header Card -->
    <div class="rounded-xl border border-gray-200 bg-white">
      <div class="flex items-center justify-between px-5 py-3">
        <div class="flex items-center gap-2 min-w-0">
          <h2 class="truncate text-[15px] font-semibold text-gray-900">{{ node.unitName }}</h2>
          <code class="rounded bg-gray-100 px-1.5 py-0.5 font-mono text-[10px] text-gray-500 flex-shrink-0">{{ node.unitCode }}</code>
          <span
            class="inline-flex items-center rounded px-1.5 py-0.5 text-[10px] font-medium flex-shrink-0"
            :style="typeBadgeStyle"
          >
            {{ node.typeName || node.unitType }}
          </span>
          <span class="rounded px-1.5 py-0.5 text-[10px] font-medium flex-shrink-0" :class="node.status === 'ACTIVE' ? 'bg-green-50 text-green-600' : node.status === 'FROZEN' ? 'bg-orange-50 text-orange-500' : 'bg-red-50 text-red-500'">
            {{ node.statusLabel || node.status }}
          </span>
        </div>
        <div class="flex items-center gap-1.5 flex-shrink-0">
          <button
            class="inline-flex items-center gap-1 rounded-md border border-gray-200 px-2.5 py-1 text-xs font-medium text-gray-600 hover:bg-gray-50"
            @click="emit('addChild', node)"
          >
            <Plus class="h-3 w-3" />
            子组织
          </button>
          <button
            class="inline-flex items-center gap-1 rounded-md border border-gray-200 px-2.5 py-1 text-xs font-medium text-gray-600 hover:bg-gray-50"
            @click="emit('edit', node)"
          >
            <Pencil class="h-3 w-3" />
            编辑
          </button>
          <el-dropdown v-if="node.status === 'ACTIVE' || node.status === 'FROZEN'" trigger="click">
            <button class="inline-flex items-center justify-center h-6 w-6 rounded-md border border-gray-200 text-gray-400 hover:bg-gray-50">
              <MoreHorizontal class="h-3.5 w-3.5" />
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item v-if="node.status === 'ACTIVE'" @click="emit('toggleStatus', node)">
                  <Ban class="mr-2 h-4 w-4 text-orange-500" />冻结
                </el-dropdown-item>
                <el-dropdown-item v-if="node.status === 'FROZEN'" @click="emit('toggleStatus', node)">
                  <Check class="mr-2 h-4 w-4 text-green-500" />解冻
                </el-dropdown-item>
                <el-dropdown-item v-if="node.status === 'ACTIVE'" divided @click="emit('merge', node)">
                  <Merge class="mr-2 h-4 w-4 text-blue-500" />合并到...
                </el-dropdown-item>
                <el-dropdown-item v-if="node.status === 'ACTIVE'" @click="emit('split', node)">
                  <Split class="mr-2 h-4 w-4 text-indigo-500" />拆分
                </el-dropdown-item>
                <el-dropdown-item v-if="node.status === 'ACTIVE'" divided @click="emit('dissolve', node)">
                  <XCircle class="mr-2 h-4 w-4 text-red-500" />解散
                </el-dropdown-item>
                <el-dropdown-item divided @click="showExportDialog = true">
                  <Download class="mr-2 h-4 w-4 text-gray-500" />导出数据
                </el-dropdown-item>
                <el-dropdown-item @click="showImportDialog = true">
                  <Upload class="mr-2 h-4 w-4 text-gray-500" />导入数据
                </el-dropdown-item>
                <el-dropdown-item divided @click="emit('delete', node)">
                  <Trash2 class="mr-2 h-4 w-4 text-red-500" />删除
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <button
            v-else
            class="inline-flex items-center gap-1 rounded-md border border-red-200 bg-red-50 px-2.5 py-1 text-xs font-medium text-red-600 transition-colors hover:bg-red-100"
            @click="emit('delete', node)"
          >
            <Trash2 class="h-3 w-3" />
            删除
          </button>
        </div>
      </div>

      <!-- Statistics Bar -->
      <div v-if="stats" class="flex items-center gap-4 border-t border-gray-100 px-5 py-2 text-xs text-gray-600">
        <span>成员 <strong class="text-gray-900">{{ stats.belongingCount }}</strong></span>
        <template v-if="Object.keys(stats.countByUserType).length > 0">
          <span class="text-gray-300">—</span>
          <template v-for="(cnt, typeCode, idx) in stats.countByUserType" :key="typeCode">
            <span v-if="idx > 0" class="text-gray-200">|</span>
            <span>{{ userTypeNameMap[typeCode as string] || typeCode }} <strong class="text-gray-900">{{ cnt }}</strong></span>
          </template>
        </template>
      </div>
    </div>

    <!-- Tabs -->
    <div class="rounded-xl border border-gray-200 bg-white">
      <div class="flex border-b border-gray-200">
        <button
          v-for="tab in tabs"
          :key="tab.key"
          :class="[
            'relative px-4 py-2.5 text-xs font-medium transition-colors',
            activeTab === tab.key
              ? 'text-blue-600'
              : 'text-gray-500 hover:text-gray-700'
          ]"
          @click="activeTab = tab.key"
        >
          {{ tab.label }}
          <span
            v-if="tab.count !== undefined"
            class="ml-1.5 rounded-full bg-gray-100 px-1.5 py-0.5 text-[10px] font-medium text-gray-600"
          >{{ tab.count }}</span>
          <div
            v-if="activeTab === tab.key"
            class="absolute bottom-0 left-0 right-0 h-0.5 bg-blue-600"
          ></div>
        </button>
      </div>

      <!-- Tab: 下级组织 -->
      <div v-if="activeTab === 'children'">
        <div class="flex items-center justify-between border-b border-gray-50 px-5 py-2.5">
          <span class="text-xs text-gray-500">共 {{ children.length }} 个下级组织</span>
          <button
            class="inline-flex items-center gap-1 rounded-md bg-blue-600 px-2.5 py-1 text-[11px] font-medium text-white transition-colors hover:bg-blue-700"
            @click="emit('addChild', node)"
          >
            <Plus class="h-3 w-3" />
            添加
          </button>
        </div>
        <div v-if="children.length > 0" class="overflow-x-auto">
          <table class="w-full">
            <thead>
              <tr class="border-b border-gray-100 bg-gray-50/50">
                <th class="px-4 py-2 text-left text-xs font-medium text-gray-500">名称</th>
                <th class="px-4 py-2 text-left text-xs font-medium text-gray-500">编码</th>
                <th class="px-4 py-2 text-left text-xs font-medium text-gray-500">类型</th>
                <th class="px-4 py-2 text-center text-xs font-medium text-gray-500">状态</th>
                <th class="px-4 py-2 text-right text-xs font-medium text-gray-500">操作</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-gray-50">
              <tr
                v-for="child in children"
                :key="child.id"
                class="transition-colors hover:bg-gray-50"
              >
                <td class="px-4 py-2">
                  <div class="flex items-center gap-2">
                    <span class="text-xs font-medium text-gray-900">{{ child.unitName }}</span>
                    <span
                      v-if="child.children?.length"
                      class="rounded bg-gray-100 px-1 py-px text-[10px] text-gray-500"
                    >
                      {{ child.children.length }} 子级
                    </span>
                  </div>
                </td>
                <td class="px-4 py-2">
                  <code class="rounded bg-gray-100 px-1.5 py-0.5 font-mono text-[10px] text-gray-600">
                    {{ child.unitCode }}
                  </code>
                </td>
                <td class="px-4 py-2">
                  <span class="text-xs text-gray-600">{{ child.typeName || child.unitType }}</span>
                </td>
                <td class="px-4 py-2 text-center">
                  <span
                    :class="[
                      'inline-flex items-center rounded px-1.5 py-0.5 text-[10px] font-medium',
                      child.status === 'ACTIVE' ? 'bg-green-50 text-green-700' :
                      child.status === 'FROZEN' ? 'bg-orange-50 text-orange-700' :
                      'bg-red-50 text-red-700'
                    ]"
                  >
                    {{ child.statusLabel || child.status }}
                  </span>
                </td>
                <td class="px-4 py-2 text-right">
                  <div class="flex items-center justify-end gap-1">
                    <button
                      class="rounded p-1.5 text-gray-400 hover:bg-gray-100 hover:text-blue-600"
                      title="编辑"
                      @click="emit('edit', child)"
                    >
                      <Pencil class="h-3.5 w-3.5" />
                    </button>
                    <button
                      v-if="child.status === 'ACTIVE' || child.status === 'FROZEN'"
                      :class="[
                        'rounded p-1.5 text-gray-400',
                        child.status === 'ACTIVE'
                          ? 'hover:bg-orange-50 hover:text-orange-600'
                          : 'hover:bg-green-50 hover:text-green-600'
                      ]"
                      :title="child.status === 'ACTIVE' ? '冻结' : '解冻'"
                      @click="emit('toggleStatus', child)"
                    >
                      <Ban v-if="child.status === 'ACTIVE'" class="h-3.5 w-3.5" />
                      <Check v-else class="h-3.5 w-3.5" />
                    </button>
                    <button
                      class="rounded p-1.5 text-gray-400 hover:bg-red-50 hover:text-red-600"
                      title="删除"
                      @click="emit('delete', child)"
                    >
                      <Trash2 class="h-3.5 w-3.5" />
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <div v-else class="flex flex-col items-center py-10">
          <FolderOpen class="h-10 w-10 text-gray-300" />
          <p class="mt-2 text-sm text-gray-500">暂无下级组织</p>
          <button
            class="mt-3 inline-flex items-center gap-1.5 rounded-lg border border-gray-300 bg-white px-3 py-1.5 text-xs font-medium text-gray-700 transition-colors hover:bg-gray-50"
            @click="emit('addChild', node)"
          >
            <Plus class="h-3.5 w-3.5" />
            添加子组织
          </button>
        </div>
      </div>

      <!-- Tab: 成员 (仅本组织归属成员) -->
      <div v-if="activeTab === 'members'">
        <div class="flex items-center justify-between border-b border-gray-50 px-6 py-3">
          <span class="text-xs text-gray-500">共 {{ belongingMembers.length }} 人</span>
          <button
            class="inline-flex items-center gap-1.5 rounded-lg border border-gray-300 bg-white px-3 py-1.5 text-xs font-medium text-gray-700 transition-colors hover:bg-gray-50"
            @click="showAddMember = true"
          >
            <Plus class="h-3.5 w-3.5" />
            添加成员
          </button>
        </div>
        <div v-if="membersLoading" class="flex items-center justify-center py-10">
          <Loader2 class="h-5 w-5 animate-spin text-gray-400" />
          <span class="ml-2 text-sm text-gray-500">加载中...</span>
        </div>
        <div v-else-if="mergedMembers.length > 0" class="overflow-x-auto">
          <table class="w-full">
            <thead>
              <tr class="border-b border-gray-100 bg-gray-50/60">
                <th class="px-6 py-2.5 text-left text-xs font-medium text-gray-500">姓名</th>
                <th class="px-6 py-2.5 text-left text-xs font-medium text-gray-500">身份</th>
                <th class="px-6 py-2.5 text-left text-xs font-medium text-gray-500">岗位</th>
                <th class="px-6 py-2.5 text-left text-xs font-medium text-gray-500">任职方式</th>
                <th class="px-6 py-2.5 text-right text-xs font-medium text-gray-500">操作</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-gray-50">
              <tr
                v-for="m in mergedMembers"
                :key="`${m.userId}-${m.userPositionId || 'no-pos'}`"
                class="transition-colors hover:bg-gray-50"
              >
                <td class="px-6 py-3">
                  <button
                    class="text-sm font-medium text-blue-600 hover:text-blue-800 hover:underline"
                    @click="openUserRelation(m)"
                  >{{ m.userName }}</button>
                </td>
                <td class="px-6 py-3">
                  <span class="text-sm text-gray-600">{{ userTypeNameMap[m.userTypeCode || ''] || m.userTypeCode || '-' }}</span>
                </td>
                <td class="px-6 py-3">
                  <div v-if="m.localPositionName" class="flex items-center gap-1.5">
                    <span class="text-sm text-gray-900">{{ m.localPositionName }}</span>
                    <span
                      v-if="m.isKeyPosition"
                      class="rounded bg-amber-50 px-1 py-0.5 text-[10px] font-medium text-amber-600"
                    >关键</span>
                  </div>
                  <span v-else class="text-sm text-gray-400">—</span>
                </td>
                <td class="px-6 py-3">
                  <span
                    v-if="m.appointmentType"
                    class="inline-flex rounded-full px-2 py-0.5 text-xs font-medium"
                    :class="appointmentTagClass(m.appointmentType)"
                  >
                    {{ AppointmentTypeLabels[m.appointmentType] || m.appointmentType }}
                  </span>
                  <span v-else class="text-sm text-gray-400">—</span>
                </td>
                <td class="px-6 py-3 text-right">
                  <div class="flex items-center justify-end gap-1">
                    <button
                      v-if="m.userPositionId"
                      class="rounded p-1.5 text-gray-400 hover:bg-orange-50 hover:text-orange-600"
                      title="移除岗位"
                      @click="handleRemoveStaff(m)"
                    >
                      <UserMinus class="h-3.5 w-3.5" />
                    </button>
                    <button
                      class="rounded p-1.5 text-gray-400 hover:bg-red-50 hover:text-red-600"
                      title="移除成员"
                      @click="handleRemoveMember(m)"
                    >
                      <Trash2 class="h-3.5 w-3.5" />
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <div v-else class="flex flex-col items-center py-10">
          <Users class="h-10 w-10 text-gray-300" />
          <p class="mt-2 text-sm text-gray-500">暂无成员</p>
        </div>
      </div>

      <!-- Tab: 关联场所 -->
      <div v-if="activeTab === 'places'">
        <div class="flex items-center justify-between border-b border-gray-50 px-6 py-3">
          <span class="text-xs text-gray-500">共 {{ places.length }} 个关联场所</span>
          <button
            class="inline-flex items-center gap-1.5 rounded-lg border border-gray-300 bg-white px-3 py-1.5 text-xs font-medium text-gray-700 transition-colors hover:bg-gray-50"
            @click="showAddPlace = true"
          >
            <Plus class="h-3.5 w-3.5" />
            关联场所
          </button>
        </div>
        <div v-if="placesLoading" class="flex items-center justify-center py-10">
          <Loader2 class="h-5 w-5 animate-spin text-gray-400" />
          <span class="ml-2 text-sm text-gray-500">加载中...</span>
        </div>
        <div v-else-if="places.length > 0" class="overflow-x-auto">
          <table class="w-full">
            <thead>
              <tr class="border-b border-gray-100 bg-gray-50/50">
                <th class="px-4 py-2 text-left text-xs font-medium text-gray-500">场所名称</th>
                <th class="px-4 py-2 text-left text-xs font-medium text-gray-500">编码</th>
                <th class="px-4 py-2 text-left text-xs font-medium text-gray-500">关系类型</th>
                <th class="w-12 px-2 py-2 text-right font-medium text-gray-400"></th>
              </tr>
            </thead>
            <tbody class="divide-y divide-gray-50">
              <tr
                v-for="s in places"
                :key="s.id"
                class="transition-colors hover:bg-gray-50/50"
              >
                <td class="px-4 py-2">
                  <span class="text-xs font-medium text-gray-900">{{ s.metadata?.placeName || `场所#${s.resourceId}` }}</span>
                </td>
                <td class="px-4 py-2">
                  <code class="rounded bg-gray-100 px-1.5 py-0.5 font-mono text-[10px] text-gray-600">
                    {{ s.metadata?.placeCode || '-' }}
                  </code>
                </td>
                <td class="px-4 py-2">
                  <span class="rounded px-1.5 py-0.5 text-[10px] font-medium bg-purple-50 text-purple-700">
                    {{ RelationLabels[s.relation] || s.relation }}
                  </span>
                </td>
                <td class="whitespace-nowrap px-2 py-2 text-right">
                  <button
                    class="rounded px-1.5 py-0.5 text-[10px] font-medium text-red-400 transition-colors hover:bg-red-50 hover:text-red-500"
                    @click="handleRemovePlace(s)"
                  >移除</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <div v-else class="px-5 py-4 text-center text-xs text-gray-400">
          暂无关联场所
        </div>
      </div>

      <!-- Tab: 岗位编制 -->
      <div v-if="activeTab === 'positions'">
        <!-- 本级岗位编制 -->
        <div class="flex items-center justify-between border-b border-gray-50 px-6 py-3">
          <span class="text-xs text-gray-500">本级岗位 {{ positions.length }} 个</span>
        </div>
        <div v-if="positionsLoading" class="flex items-center justify-center py-10">
          <Loader2 class="h-5 w-5 animate-spin text-gray-400" />
          <span class="ml-2 text-sm text-gray-500">加载中...</span>
        </div>
        <div v-else-if="positions.length > 0" class="overflow-x-auto">
          <table class="w-full">
            <thead>
              <tr class="border-b border-gray-100 bg-gray-50/60">
                <th class="px-6 py-2.5 text-left text-xs font-medium text-gray-500">岗位名称</th>
                <th class="px-6 py-2.5 text-center text-xs font-medium text-gray-500">编制/在岗</th>
                <th class="px-6 py-2.5 text-left text-xs font-medium text-gray-500">在岗人员</th>
                <th class="px-6 py-2.5 text-center text-xs font-medium text-gray-500">状态</th>
                <th class="px-6 py-2.5 text-right text-xs font-medium text-gray-500">操作</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-gray-50">
              <tr
                v-for="p in positions"
                :key="p.id"
                class="transition-colors hover:bg-gray-50"
              >
                <td class="px-6 py-3">
                  <div class="flex items-center gap-2">
                    <span class="text-sm font-medium text-gray-900">{{ p.positionName }}</span>
                    <span
                      v-if="p.isKeyPosition"
                      class="rounded bg-amber-50 px-1.5 py-0.5 text-[10px] font-medium text-amber-600"
                    >关键</span>
                  </div>
                </td>
                <td class="px-6 py-3 text-center">
                  <span
                    class="text-sm font-medium"
                    :class="staffingColor(p)"
                  >
                    {{ p.currentCount ?? 0 }}/{{ p.headcount }}
                  </span>
                </td>
                <td class="px-6 py-3">
                  <template v-if="getPositionHolders(p.id).length > 0">
                    <span
                      v-for="(h, i) in getPositionHolders(p.id)"
                      :key="h.userPositionId"
                      class="text-sm text-gray-600"
                    >{{ i > 0 ? '、' : '' }}{{ h.userName }}<span
                        v-if="!belongingUserIds.has(String(h.userId)) && h.primaryOrgUnitName"
                        class="ml-0.5 rounded bg-orange-50 px-1 py-0.5 text-[10px] font-medium text-orange-600"
                        :title="'归属: ' + h.primaryOrgUnitName"
                      >来自{{ h.primaryOrgUnitName }}</span><span
                        v-else-if="!belongingUserIds.has(String(h.userId))"
                        class="ml-0.5 rounded bg-orange-50 px-1 py-0.5 text-[10px] font-medium text-orange-600"
                      >外部</span></span>
                  </template>
                  <span v-else class="text-sm text-orange-500">(空缺)</span>
                </td>
                <td class="px-6 py-3 text-center">
                  <span
                    :class="[
                      'inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium',
                      p.enabled ? 'bg-green-50 text-green-700' : 'bg-red-50 text-red-700'
                    ]"
                  >
                    {{ p.enabled ? '启用' : '禁用' }}
                  </span>
                </td>
                <td class="px-6 py-3 text-right">
                  <button
                    class="rounded p-1.5 text-gray-400 hover:bg-blue-50 hover:text-blue-600"
                    title="任命人员"
                    @click="openAppointDialog(p)"
                  >
                    <UserPlus class="h-3.5 w-3.5" />
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <div v-else class="flex flex-col items-center py-10">
          <Users class="h-10 w-10 text-gray-300" />
          <p class="mt-2 text-sm text-gray-500">暂无岗位</p>
        </div>

        <!-- 子组织岗位汇总（可折叠） -->
        <div v-if="children.length > 0" class="border-t border-gray-100">
          <button
            class="flex w-full items-center justify-between px-6 py-3 text-left transition-colors hover:bg-gray-50"
            @click="toggleChildPositions"
          >
            <span class="text-xs font-medium text-gray-600">
              子组织岗位汇总
              <span v-if="recursiveMembers.length > 0" class="ml-1 text-gray-400">({{ childOnlyMembers.length }}人)</span>
            </span>
            <ChevronDown
              class="h-3.5 w-3.5 text-gray-400 transition-transform"
              :class="{ 'rotate-180': showChildPositions }"
            />
          </button>

          <template v-if="showChildPositions">
            <div v-if="recursiveLoading" class="flex items-center justify-center py-6">
              <Loader2 class="h-4 w-4 animate-spin text-gray-400" />
              <span class="ml-2 text-xs text-gray-500">加载中...</span>
            </div>
            <template v-else-if="childOnlyMembers.length > 0">
              <!-- 汇总统计条 -->
              <div class="flex flex-wrap gap-x-4 gap-y-1 border-b border-gray-50 bg-gray-50/40 px-6 py-2">
                <span
                  v-for="s in childPositionSummary"
                  :key="s.positionName"
                  class="text-xs text-gray-500"
                >{{ s.positionName }} <span class="font-medium text-gray-700">{{ s.count }}</span></span>
              </div>
              <!-- 明细表 -->
              <div class="overflow-x-auto">
                <table class="w-full">
                  <thead>
                    <tr class="border-b border-gray-100 bg-gray-50/30">
                      <th class="px-6 py-2 text-left text-xs font-medium text-gray-400">人员</th>
                      <th class="px-6 py-2 text-left text-xs font-medium text-gray-400">岗位</th>
                      <th class="px-6 py-2 text-left text-xs font-medium text-gray-400">所在组织</th>
                      <th class="px-6 py-2 text-center text-xs font-medium text-gray-400">任职方式</th>
                    </tr>
                  </thead>
                  <tbody class="divide-y divide-gray-50">
                    <tr
                      v-for="m in childOnlyMembers"
                      :key="m.userPositionId"
                      class="transition-colors hover:bg-gray-50"
                    >
                      <td class="px-6 py-2.5 text-sm text-gray-900">{{ m.userName }}</td>
                      <td class="px-6 py-2.5 text-sm text-gray-600">{{ m.positionName }}</td>
                      <td class="px-6 py-2.5 text-sm text-gray-500">{{ m.orgUnitName }}</td>
                      <td class="px-6 py-2.5 text-center">
                        <span
                          v-if="m.appointmentType"
                          :class="['inline-block rounded px-1.5 py-0.5 text-[10px] font-medium', appointmentTagClass(m.appointmentType)]"
                        >{{ AppointmentTypeLabels[m.appointmentType] || m.appointmentType }}</span>
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </template>
            <div v-else class="px-6 py-6 text-center text-xs text-gray-400">
              子组织中暂无岗位人员
            </div>
          </template>
        </div>
      </div>

      <!-- Tab: 操作记录 -->
      <div v-if="activeTab === 'changelog'">
        <ActivityTimeline
          resourceType="ORG_UNIT"
          :resourceId="node.id"
          :limit="50"
          title="操作记录"
        />
      </div>

      <!-- Tab: 基本信息 -->
      <div v-if="activeTab === 'info'" class="grid grid-cols-2 gap-x-6 gap-y-3 px-5 py-4 lg:grid-cols-3">
        <div>
          <dt class="text-xs font-medium text-gray-500">组织编码</dt>
          <dd class="mt-1 text-sm font-medium text-gray-900">
            <code class="rounded bg-gray-100 px-2 py-0.5 font-mono text-xs">{{ node.unitCode }}</code>
          </dd>
        </div>
        <div>
          <dt class="text-xs font-medium text-gray-500">组织类型</dt>
          <dd class="mt-1">
            <span
              class="inline-flex items-center gap-1 rounded-full px-2.5 py-0.5 text-xs font-medium"
              :style="typeBadgeStyle"
            >
              <span class="h-1.5 w-1.5 rounded-full" :style="{ backgroundColor: typeColor }"></span>
              {{ node.typeName || node.unitType }}
            </span>
          </dd>
        </div>
        <div>
          <dt class="text-xs font-medium text-gray-500">编制人数</dt>
          <dd class="mt-1 text-sm text-gray-900">{{ node.headcount ?? '-' }}</dd>
        </div>
        <div>
          <dt class="text-xs font-medium text-gray-500">上级组织</dt>
          <dd class="mt-1 text-sm text-gray-900">
            {{ parentName || '顶级组织' }}
          </dd>
        </div>
        <div>
          <dt class="text-xs font-medium text-gray-500">当前状态</dt>
          <dd class="mt-1">
            <span
              :class="[
                'inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-xs font-medium',
                node.status === 'ACTIVE' ? 'bg-green-50 text-green-700' :
                node.status === 'FROZEN' ? 'bg-orange-50 text-orange-700' :
                node.status === 'DISSOLVED' ? 'bg-red-50 text-red-700' :
                'bg-gray-50 text-gray-700'
              ]"
            >
              <CheckCircle v-if="node.status === 'ACTIVE'" class="h-3 w-3" />
              <XCircle v-else class="h-3 w-3" />
              {{ node.statusLabel || node.status }}
            </span>
          </dd>
        </div>
        <div>
          <dt class="text-xs font-medium text-gray-500">排序号</dt>
          <dd class="mt-1 text-sm text-gray-900">{{ node.sortOrder }}</dd>
        </div>
      </div>
    </div>

    <!-- 添加成员 — 用户选择器弹窗 (Step 4) -->
    <UserSelectorDialog
      v-model:visible="showAddMember"
      title="添加成员到组织"
      :exclude-user-ids="belongingMembers.map(m => m.userId)"
      @confirm="handleAddMemberFromSelector"
    />

    <!-- 任命到岗位弹窗 (从岗位 Tab 触发) -->
    <div
      v-if="showAppointDialog"
      class="fixed inset-0 z-50 flex items-center justify-center bg-black/40"
      @click.self="showAppointDialog = false"
    >
      <div class="w-[480px] rounded-xl bg-white p-6 shadow-xl">
        <h3 class="mb-4 text-base font-semibold text-gray-900">
          任命人员到「{{ appointForm.positionName }}」
        </h3>
        <div class="space-y-4">
          <div>
            <label class="mb-1 block text-xs font-medium text-gray-600">选择用户</label>
            <div
              class="flex min-h-[36px] cursor-pointer items-center rounded-lg border border-gray-300 px-3 py-1.5 text-sm transition-colors hover:border-blue-400"
              @click="showAppointUserSelector = true"
            >
              <span v-if="appointSelectedUser" class="text-gray-900">
                {{ appointSelectedUser.realName }} ({{ appointSelectedUser.username }})
              </span>
              <span v-else class="text-gray-400">点击选择用户</span>
            </div>
          </div>
          <div>
            <label class="mb-1 block text-xs font-medium text-gray-600">任命类型</label>
            <el-select v-model="appointForm.appointmentType" style="width: 100%">
              <el-option label="正式" value="FORMAL" />
              <el-option label="代理" value="ACTING" />
              <el-option label="兼职" value="CONCURRENT" />
              <el-option label="试用" value="PROBATION" />
            </el-select>
          </div>
          <div>
            <label class="mb-1 block text-xs font-medium text-gray-600">任职日期</label>
            <el-date-picker
              v-model="appointForm.startDate"
              type="date"
              placeholder="选择日期"
              style="width: 100%"
              value-format="YYYY-MM-DD"
            />
          </div>
          <div class="flex items-center gap-4">
            <label class="flex items-center gap-2 text-sm text-gray-700">
              <input v-model="appointForm.isPrimary" type="checkbox" class="rounded" />
              主岗
            </label>
          </div>
        </div>
        <div class="mt-6 flex justify-end gap-3">
          <button
            class="rounded-lg border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50"
            @click="showAppointDialog = false"
          >
            取消
          </button>
          <button
            :disabled="!appointForm.userId || appointSubmitting"
            class="rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-50"
            @click="handleAppoint"
          >
            {{ appointSubmitting ? '任命中...' : '确定' }}
          </button>
        </div>
      </div>
    </div>
    <!-- 任命用户选择器 -->
    <UserSelectorDialog
      v-model:visible="showAppointUserSelector"
      title="选择要任命的用户"
      @confirm="handleAppointUserSelected"
    />

    <!-- 导出/导入弹窗 (Step 5) -->
    <OrgExportDialog
      v-model:visible="showExportDialog"
      :org-unit="node"
    />
    <OrgImportDialog
      v-model:visible="showImportDialog"
      :org-unit-id="node.id"
      @imported="Promise.all([loadMembers(), loadPositions(), loadStats()])"
    />

    <!-- 用户关系抽屉 (Step 3) -->
    <UserRelationDrawer
      v-model:visible="showUserRelation"
      :user-id="userRelationUserId"
      :user-name="userRelationUserName"
      :tree-data="treeData"
      @changed="onUserRelationChanged"
    />

    <!-- 关联场所弹窗 -->
    <div
      v-if="showAddPlace"
      class="fixed inset-0 z-50 flex items-center justify-center bg-black/40"
      @click.self="showAddPlace = false"
    >
      <div class="w-[480px] rounded-xl bg-white p-6 shadow-xl">
        <h3 class="mb-4 text-base font-semibold text-gray-900">关联场所到「{{ node.unitName }}」</h3>
        <div class="space-y-4">
          <div>
            <label class="mb-1 block text-xs font-medium text-gray-600">选择场所</label>
            <select
              v-model="addPlaceForm.placeId"
              class="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
            >
              <option :value="0" disabled>请选择场所</option>
              <option v-for="s in placeOptions" :key="s.id" :value="s.id">
                {{ s.placeName }} ({{ s.placeCode }})
              </option>
            </select>
          </div>
          <div>
            <label class="mb-1 block text-xs font-medium text-gray-600">关系类型</label>
            <select
              v-model="addPlaceForm.relationType"
              class="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
            >
              <option value="PRIMARY">主管</option>
              <option value="SHARED">共享</option>
              <option value="MANAGED">托管</option>
            </select>
          </div>
          <div class="flex items-center gap-4">
            <label class="flex items-center gap-2 text-sm text-gray-700">
              <input v-model="addPlaceForm.isPrimary" type="checkbox" class="rounded" />
              主归属
            </label>
            <label class="flex items-center gap-2 text-sm text-gray-700">
              <input v-model="addPlaceForm.canInspect" type="checkbox" class="rounded" />
              可检查
            </label>
          </div>
        </div>
        <div class="mt-6 flex justify-end gap-3">
          <button
            class="rounded-lg border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50"
            @click="showAddPlace = false"
          >
            取消
          </button>
          <button
            :disabled="!addPlaceForm.placeId || addPlaceSubmitting"
            class="rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-50"
            @click="handleAddPlace"
          >
            {{ addPlaceSubmitting ? '关联中...' : '确定' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch, reactive } from 'vue'
import {
  Building2,
  Plus,
  Pencil,
  Trash2,
  Ban,
  Check,
  CheckCircle,
  XCircle,
  Users,
  FolderOpen,
  MapPin,
  Loader2,
  MoreHorizontal,
  Merge,
  Split,
  UserPlus,
  UserMinus,
  Download,
  Upload,
  ChevronDown,
} from 'lucide-vue-next'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { DepartmentResponse } from '@/api/organization'
import ClassDetailPanel from './ClassDetailPanel.vue'
import UserRelationDrawer from './UserRelationDrawer.vue'
import UserSelectorDialog from '@/components/common/UserSelectorDialog.vue'
import OrgExportDialog from './OrgExportDialog.vue'
import OrgImportDialog from './OrgImportDialog.vue'
import type { AccessRelation } from '@/types/accessRelation'
import { RelationLabels } from '@/types/accessRelation'
import { accessRelationApi } from '@/api/accessRelation'
import type { SimpleUser } from '@/types/user'
import type { UniversalPlace, PlaceTreeNode } from '@/types/universalPlace'
import { getSimpleUserList } from '@/api/user'
import { universalPlaceApi } from '@/api/universalPlace'
import { positionApi, userPositionApi } from '@/api/position'
import type { Position, OrgStatistics } from '@/types/position'
import ActivityTimeline from '@/components/activity/ActivityTimeline.vue'
import { AppointmentTypeLabels } from '@/types/position'
import type { OrgMember } from '@/types/position'
import { getEnabledUserTypes } from '@/api/userType'

interface Props {
  node: DepartmentResponse
  treeData: DepartmentResponse[]
}

const props = defineProps<Props>()

const emit = defineEmits<{
  refresh: []
  addChild: [node: DepartmentResponse]
  edit: [node: DepartmentResponse]
  delete: [node: DepartmentResponse]
  toggleStatus: [node: DepartmentResponse]
  merge: [node: DepartmentResponse]
  split: [node: DepartmentResponse]
  dissolve: [node: DepartmentResponse]
}>()

// ==================== Tab state ====================
const activeTab = ref<'children' | 'members' | 'places' | 'positions' | 'changelog' | 'info'>('children')

// ==================== Color mapping by category (OrgCategory enum) ====================
const OrgCategoryColorMap: Record<string, string> = {
  ROOT: '#3b82f6',         // 蓝 - 根组织
  BRANCH: '#8b5cf6',       // 紫 - 分支机构
  FUNCTIONAL: '#10b981',   // 绿 - 职能部门
  GROUP: '#ef4444',        // 红 - 成员组（班级/小组）
  CONTAINER: '#f59e0b',    // 橙 - 容器（年级/学部）
}

const typeColor = computed(() => OrgCategoryColorMap[props.node.category || ''] || '#6b7280')

const typeIconStyle = computed(() => ({
  backgroundColor: `${typeColor.value}18`,
  color: typeColor.value
}))

const typeBadgeStyle = computed(() => ({
  backgroundColor: `${typeColor.value}12`,
  color: typeColor.value
}))

// Find parent name
const findParentName = (nodes: DepartmentResponse[], parentId: number | null): string => {
  if (!parentId) return ''
  for (const n of nodes) {
    if (n.id === parentId) return n.unitName
    if (n.children) {
      const found = findParentName(n.children, parentId)
      if (found) return found
    }
  }
  return ''
}

const parentName = computed(() => findParentName(props.treeData, props.node.parentId))

// Children
const children = computed(() => props.node.children || [])

const getChildIconStyle = (child: DepartmentResponse) => {
  const color = OrgCategoryColorMap[child.category || ''] || '#6b7280'
  return {
    backgroundColor: `${color}18`,
    color: color
  }
}

// ==================== Statistics ====================
const stats = ref<OrgStatistics | null>(null)

const loadStats = async () => {
  try {
    stats.value = await userPositionApi.getOrgStatistics(props.node.id)
  } catch (e: any) {
    console.error('Failed to load stats', e)
  }
}

// ==================== Members data ====================
const belongingMembers = ref<OrgMember[]>([])
const staffMembers = ref<OrgMember[]>([])
const membersLoading = ref(false)

const loadMembers = async () => {
  membersLoading.value = true
  try {
    const [belonging, staff] = await Promise.all([
      userPositionApi.getBelongingMembers(props.node.id),
      userPositionApi.getOrgMembers(props.node.id),
    ])
    belongingMembers.value = belonging
    staffMembers.value = staff
  } catch (e: any) {
    console.error('Failed to load members', e)
  } finally {
    membersLoading.value = false
  }
}

// ==================== User type name map ====================
const userTypeNameMap = ref<Record<string, string>>({})

const loadUserTypes = async () => {
  try {
    const types = await getEnabledUserTypes()
    const map: Record<string, string> = {}
    for (const t of types) {
      map[t.typeCode] = t.typeName
    }
    userTypeNameMap.value = map
  } catch (e) {
    console.error('Failed to load user types', e)
  }
}
loadUserTypes()

// ==================== Merged members (unified list) ====================
interface MergedMember {
  userId: string | number
  userName: string
  userTypeCode?: string
  localPositionName?: string     // position name in THIS org
  appointmentType?: string
  startDate?: string
  isPrimary: boolean
  isKeyPosition?: boolean
  isExternal: boolean            // belongs to another org
  userPositionId?: string | number  // for remove action
}

const mergedMembers = computed<MergedMember[]>(() => {
  const result: MergedMember[] = []

  // Group staff by userId for quick lookup
  const staffByUser = new Map<string, OrgMember[]>()
  for (const s of staffMembers.value) {
    const key = String(s.userId)
    if (!staffByUser.has(key)) staffByUser.set(key, [])
    staffByUser.get(key)!.push(s)
  }

  // Only belonging members (primary_org_unit_id = this org)
  for (const m of belongingMembers.value) {
    const staffEntries = staffByUser.get(String(m.userId)) || []
    if (staffEntries.length > 0) {
      // 用户在当前组织有岗位 → 显示当前组织的岗位信息
      for (const s of staffEntries) {
        result.push({
          userId: m.userId,
          userName: m.userName,
          userTypeCode: m.userTypeCode,
          localPositionName: s.positionName,
          appointmentType: s.appointmentType,
          startDate: s.startDate,
          isPrimary: s.isPrimary,
          isKeyPosition: s.isKeyPosition,
          isExternal: false,
          userPositionId: s.userPositionId,
        })
      }
    } else {
      // 用户在当前组织无岗位 → 使用 getBelongingMembers 返回的主岗信息
      result.push({
        userId: m.userId,
        userName: m.userName,
        userTypeCode: m.userTypeCode,
        localPositionName: m.positionName,
        appointmentType: m.appointmentType,
        startDate: m.startDate,
        isPrimary: m.isPrimary,
        isKeyPosition: m.isKeyPosition,
        isExternal: false,
        userPositionId: m.userPositionId,
      })
    }
  }

  return result
})

// Belonging user IDs set (for positions tab "外部" tag)
const belongingUserIds = computed(() => new Set(belongingMembers.value.map(m => String(m.userId))))

// Appointment type tag color
const appointmentTagClass = (type: string) => {
  switch (type) {
    case 'FORMAL': return 'bg-blue-50 text-blue-700'
    case 'ACTING': return 'bg-amber-50 text-amber-700'
    case 'CONCURRENT': return 'bg-purple-50 text-purple-700'
    case 'PROBATION': return 'bg-gray-100 text-gray-600'
    default: return 'bg-gray-100 text-gray-600'
  }
}

// ==================== Child org positions (collapsible panel) ====================
const showChildPositions = ref(false)
const recursiveMembers = ref<OrgMember[]>([])
const recursiveLoading = ref(false)
const recursiveLoaded = ref(false)

// 排除本级，只留子组织的成员
const childOnlyMembers = computed(() => {
  const currentOrgId = String(props.node.id)
  return recursiveMembers.value.filter(m => String(m.orgUnitId) !== currentOrgId)
})

// 子组织岗位汇总统计
const childPositionSummary = computed(() => {
  const countMap = new Map<string, number>()
  for (const m of childOnlyMembers.value) {
    const name = m.positionName || '未知岗位'
    countMap.set(name, (countMap.get(name) || 0) + 1)
  }
  return Array.from(countMap.entries())
    .map(([positionName, count]) => ({ positionName, count }))
    .sort((a, b) => b.count - a.count)
})

const loadRecursiveMembers = async () => {
  recursiveLoading.value = true
  try {
    recursiveMembers.value = await userPositionApi.getOrgMembersRecursive(props.node.id)
    recursiveLoaded.value = true
  } catch (e: any) {
    console.error('Failed to load recursive members', e)
  } finally {
    recursiveLoading.value = false
  }
}

const toggleChildPositions = () => {
  showChildPositions.value = !showChildPositions.value
  if (showChildPositions.value && !recursiveLoaded.value) {
    loadRecursiveMembers()
  }
}

// ==================== Places data ====================
const places = ref<AccessRelation[]>([])
const placesLoading = ref(false)

const loadPlaces = async () => {
  placesLoading.value = true
  try {
    places.value = await accessRelationApi.getBySubject('org_unit', props.node.id, 'place')
  } catch (e: any) {
    console.error('Failed to load places', e)
  } finally {
    placesLoading.value = false
  }
}

// ==================== Tab counts ====================
const tabs = computed(() => [
  { key: 'children' as const, label: '下级组织', count: children.value.length },
  { key: 'members' as const, label: '成员', count: belongingMembers.value.length || undefined },
  { key: 'positions' as const, label: '岗位编制', count: positions.value.length },
  { key: 'places' as const, label: '关联场所', count: places.value.length },
  { key: 'changelog' as const, label: '操作记录', count: undefined },
  { key: 'info' as const, label: '基本信息', count: undefined }
])

// ==================== Positions data ====================
const positions = ref<Position[]>([])
const positionsLoading = ref(false)

const loadPositions = async () => {
  positionsLoading.value = true
  try {
    positions.value = await positionApi.getByOrgUnit(props.node.id)
  } catch (e: any) {
    console.error('Failed to load positions', e)
  } finally {
    positionsLoading.value = false
  }
}

// Position tab helpers
const getPositionHolders = (positionId: number | string) => {
  return staffMembers.value.filter(m => String(m.positionId) === String(positionId))
}

const staffingColor = (p: Position) => {
  const current = p.currentCount ?? 0
  if (current > p.headcount) return 'text-red-600'
  if (current === p.headcount) return 'text-green-600'
  return 'text-orange-500'
}

// ==================== Load data on node change ====================
watch(
  () => props.node.id,
  () => {
    belongingMembers.value = []
    staffMembers.value = []
    places.value = []
    positions.value = []
    stats.value = null
    showChildPositions.value = false
    recursiveMembers.value = []
    recursiveLoaded.value = false
    loadMembers()
    loadPlaces()
    loadPositions()
    loadStats()
  },
  { immediate: true }
)

// ==================== Add Member (直接添加到组织, using UserSelectorDialog) ====================
const showAddMember = ref(false)
const userOptions = ref<SimpleUser[]>([])

const handleAddMemberFromSelector = async (users: SimpleUser[]) => {
  if (users.length === 0) return
  const user = users[0]
  // Step 2: Check if user already belongs to another org
  if (user.primaryOrgUnitId && String(user.primaryOrgUnitId) !== String(props.node.id)) {
    try {
      await ElMessageBox.confirm(
        `该用户当前归属于「${user.primaryOrgUnitName || '其他组织'}」，确定要变更到本组织吗？`,
        '变更组织提示',
        { type: 'warning' }
      )
    } catch {
      return // user cancelled
    }
  }
  try {
    await userPositionApi.addMember(props.node.id, user.id)
    ElMessage.success('成员添加成功')
    await Promise.all([loadMembers(), loadPositions(), loadStats()])
  } catch (e: any) {
    ElMessage.error(e.message || '添加失败')
  }
}

const handleRemoveMember = async (m: MergedMember) => {
  try {
    await ElMessageBox.confirm(
      `确定要将「${m.userName}」从本组织移除吗？${m.userPositionId ? '同时会移除其所有岗位。' : ''}`,
      '确认移除成员',
      { type: 'warning' }
    )
    await userPositionApi.removeMember(props.node.id, m.userId)
    ElMessage.success('已移除成员')
    await Promise.all([loadMembers(), loadPositions(), loadStats()])
  } catch {
    // cancelled
  }
}

const handleRemoveStaff = async (m: MergedMember) => {
  if (!m.userPositionId) return
  try {
    await ElMessageBox.confirm(
      `确定要移除「${m.userName}」的岗位「${m.localPositionName}」吗？`,
      '确认移除岗位',
      { type: 'warning' }
    )
    await userPositionApi.endAppointment(m.userPositionId, {
      endDate: new Date().toISOString().slice(0, 10),
      reason: '管理员移除',
    })
    ElMessage.success('已移除岗位')
    await Promise.all([loadMembers(), loadPositions(), loadStats()])
  } catch {
    // cancelled
  }
}

// ==================== Appoint to Position (从岗位 Tab 触发) ====================
const showAppointDialog = ref(false)
const showAppointUserSelector = ref(false)
const appointSubmitting = ref(false)
const appointSelectedUser = ref<SimpleUser | null>(null)
const appointForm = reactive({
  positionId: null as number | string | null,
  positionName: '',
  userId: null as number | string | null,
  appointmentType: 'FORMAL',
  isPrimary: true,
  startDate: new Date().toISOString().slice(0, 10),
})

const openAppointDialog = (p: Position) => {
  appointForm.positionId = p.id
  appointForm.positionName = p.positionName
  appointForm.userId = null
  appointSelectedUser.value = null
  appointForm.appointmentType = 'FORMAL'
  appointForm.isPrimary = true
  appointForm.startDate = new Date().toISOString().slice(0, 10)
  showAppointDialog.value = true
}

const handleAppointUserSelected = (users: SimpleUser[]) => {
  if (users.length > 0) {
    const u = users[0]
    appointSelectedUser.value = u
    appointForm.userId = u.id
  }
}

const handleAppoint = async () => {
  if (!appointForm.userId || !appointForm.positionId) return
  appointSubmitting.value = true
  try {
    // 查询用户现有岗位，如有则提示"从哪里到哪里"
    const existingPositions = await userPositionApi.getByUser(appointForm.userId)
    const currentPositions = existingPositions.filter(p => p.isCurrent)
    if (currentPositions.length > 0) {
      const fromList = currentPositions
        .map(p => `「${p.orgUnitName || '?'}」${p.positionName || ''}(${AppointmentTypeLabels[p.appointmentType || ''] || p.appointmentType || ''})`)
        .join('、')
      const toLabel = `「${props.node.unitName}」${appointForm.positionName}`
      await ElMessageBox.confirm(
        `该用户当前任职于 ${fromList}，确定要任命到 ${toLabel} 吗？`,
        '任命确认',
        { type: 'warning' }
      )
    }

    await userPositionApi.appoint({
      userId: appointForm.userId,
      positionId: appointForm.positionId,
      isPrimary: appointForm.isPrimary,
      appointmentType: appointForm.appointmentType,
      startDate: appointForm.startDate,
    })
    ElMessage.success('任命成功')
    showAppointDialog.value = false
    await Promise.all([loadMembers(), loadPositions(), loadStats()])
  } catch (e: any) {
    if (e !== 'cancel' && e?.toString() !== 'cancel') {
      ElMessage.error(e.message || '任命失败')
    }
  } finally {
    appointSubmitting.value = false
  }
}

// ==================== Add Place ====================
const showAddPlace = ref(false)
const addPlaceSubmitting = ref(false)
const placeOptions = ref<UniversalPlace[]>([])
const addPlaceForm = reactive({
  placeId: 0,
  relationType: 'PRIMARY',
  isPrimary: false,
  canInspect: false
})

const flattenTree = (nodes: PlaceTreeNode[]): UniversalPlace[] => {
  const result: UniversalPlace[] = []
  const walk = (list: PlaceTreeNode[]) => {
    for (const n of list) {
      result.push(n)
      if (n.children?.length) walk(n.children)
    }
  }
  walk(nodes)
  return result
}

watch(showAddPlace, async (show) => {
  if (show && placeOptions.value.length === 0) {
    try {
      const tree = await universalPlaceApi.getTree()
      placeOptions.value = flattenTree(tree)
    } catch (e) {
      console.error('Failed to load places', e)
    }
  }
  if (show) {
    addPlaceForm.placeId = 0
    addPlaceForm.relationType = 'PRIMARY'
    addPlaceForm.isPrimary = false
    addPlaceForm.canInspect = false
  }
})

const handleAddPlace = async () => {
  if (!addPlaceForm.placeId) return
  addPlaceSubmitting.value = true
  try {
    await accessRelationApi.create({
      resourceType: 'place',
      resourceId: addPlaceForm.placeId,
      subjectType: 'org_unit',
      subjectId: props.node.id,
      relation: addPlaceForm.relationType,
      metadata: {
        isPrimary: addPlaceForm.isPrimary,
        canInspect: addPlaceForm.canInspect
      }
    })
    ElMessage.success('场所关联成功')
    showAddPlace.value = false
    await loadPlaces()
  } catch (e: any) {
    ElMessage.error(e.message || '关联失败')
  } finally {
    addPlaceSubmitting.value = false
  }
}

const handleRemovePlace = async (s: AccessRelation) => {
  try {
    await ElMessageBox.confirm(
      `确定要移除场所「${s.metadata?.placeName || '场所#' + s.resourceId}」的关联吗？`,
      '确认移除',
      { type: 'warning' }
    )
    await accessRelationApi.delete(s.id)
    ElMessage.success('已移除关联')
    await loadPlaces()
  } catch {
    // cancelled
  }
}

// ==================== Export / Import Dialogs (Step 5) ====================
const showExportDialog = ref(false)
const showImportDialog = ref(false)

// ==================== User Relation Drawer (Step 3) ====================
const showUserRelation = ref(false)
const userRelationUserId = ref<number | string | null>(null)
const userRelationUserName = ref('')

const openUserRelation = (m: MergedMember) => {
  userRelationUserId.value = m.userId
  userRelationUserName.value = m.userName
  showUserRelation.value = true
}

const onUserRelationChanged = () => {
  loadMembers()
  loadPositions()
  loadStats()
}
</script>
