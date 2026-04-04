# Dual-Mode Scoring Interface Redesign

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Redesign TaskExecutionView.vue to support two scoring modes: by-section (with full hierarchy) and by-target (inspector-friendly workflow).

**Architecture:** Single-file rewrite of `TaskExecutionView.vue`. Add `viewMode` state toggle. Left sidebar adapts: section-tree or target-list. Right panel adapts: single-section scoring or all-sections-for-target card stack. No backend changes needed. No new files — all in one SFC.

**Tech Stack:** Vue 3 Composition API, Element Plus, Lucide icons, existing store/API.

---

## Data Model

```
submissions[]: { id, sectionId, targetId, targetName, status, ... }
```

**Section mode** groups by `sectionId` → for each section, show targets.
**Target mode** groups by `targetId` → for each target, show all sections with details.

Key computed for target mode:
```typescript
// Unique targets from submissions
uniqueTargets = dedupe(submissions, 'targetId')

// For selected target, all submissions grouped by section
targetSectionGroups = submissions
  .filter(s => s.targetId === selectedTargetId)
  .map(sub => ({ section, submission: sub, details: [] }))
```

---

### Task 1: Add dual-mode state and computed properties

**Files:**
- Modify: `src/views/inspection/v7/tasks/TaskExecutionView.vue` (script section, lines 37-120)

**What to add:**

```typescript
// View mode
const viewMode = ref<'section' | 'target'>('target') // default: target mode

// ===== Target Mode computeds =====

// Unique target list (deduped from submissions)
const uniqueTargets = computed(() => {
  const map = new Map<string, { targetId: number; targetName: string; targetType: string }>()
  for (const s of submissions.value) {
    const key = String(s.targetId)
    if (!map.has(key)) {
      map.set(key, { targetId: s.targetId, targetName: s.targetName, targetType: s.targetType })
    }
  }
  return Array.from(map.values())
})

// Selected target ID (for target mode)
const selectedTargetId = ref<number | null>(null)

// Target search/filter
const targetSearch = ref('')
const targetFilter = ref<'all' | 'pending' | 'completed'>('all')

// Filtered targets
const filteredTargets = computed(() => {
  let list = uniqueTargets.value
  if (targetSearch.value) {
    const q = targetSearch.value.toLowerCase()
    list = list.filter(t => t.targetName.toLowerCase().includes(q))
  }
  if (targetFilter.value === 'pending') {
    list = list.filter(t => getTargetStatus(t.targetId) !== 'COMPLETED')
  } else if (targetFilter.value === 'completed') {
    list = list.filter(t => getTargetStatus(t.targetId) === 'COMPLETED')
  }
  return list
})

// Get aggregate status for a target across all its submissions
function getTargetStatus(targetId: number): string {
  const subs = submissions.value.filter(s => s.targetId === targetId)
  if (subs.length === 0) return 'PENDING'
  if (subs.every(s => s.status === 'COMPLETED' || s.status === 'SKIPPED')) return 'COMPLETED'
  if (subs.some(s => s.status === 'IN_PROGRESS')) return 'IN_PROGRESS'
  return 'PENDING'
}

// Get target progress
function getTargetProgress(targetId: number): { done: number; total: number } {
  const subs = submissions.value.filter(s => s.targetId === targetId)
  const done = subs.filter(s => s.status === 'COMPLETED' || s.status === 'SKIPPED').length
  return { done, total: subs.length }
}

// Submissions for selected target, grouped by section
interface TargetSectionGroup {
  section: TemplateSection | null
  submission: InspSubmission
  details: SubmissionDetail[]
  collapsed: boolean
}
const targetSectionGroups = ref<TargetSectionGroup[]>([])

// Load all details for target's submissions
async function loadTargetDetails(targetId: number) {
  const subs = submissions.value.filter(s => s.targetId === targetId)
  const groups: TargetSectionGroup[] = []
  for (const sub of subs) {
    const section = allSections.value.find(s => s.id === sub.sectionId) || null
    let details: SubmissionDetail[] = []
    try { details = await store.loadDetails(sub.id) } catch {}
    groups.push({ section, submission: sub, details, collapsed: false })
  }
  targetSectionGroups.value = groups
}

// Select target in target mode
async function selectTarget(targetId: number) {
  selectedTargetId.value = targetId
  detailLoading.value = true
  try {
    // Auto-transition all PENDING submissions for this target
    const subs = submissions.value.filter(s => s.targetId === targetId)
    for (const sub of subs) {
      if (sub.status === 'PENDING' || sub.status === 'LOCKED') {
        try {
          const updated = await store.startFillingSubmission(sub.id)
          const idx = submissions.value.findIndex(s => s.id === sub.id)
          if (idx >= 0) submissions.value[idx] = updated
        } catch {}
      }
    }
    await loadTargetDetails(targetId)
  } finally {
    detailLoading.value = false
  }
}
```

**Also modify:**
- `firstLevelSections` to include root section
- Add `rootSection` computed

```typescript
const rootSection = computed(() =>
  allSections.value.find(s => s.id === rootSectionId.value) ?? null,
)

// Section tree: root + first-level children (for section mode left nav)
const sectionTree = computed(() => {
  const root = rootSection.value
  if (!root) return []
  const children = allSections.value.filter(s => Number(s.parentSectionId) === Number(rootSectionId.value))
  return [{ ...root, isRoot: true, children }]
})
```

**Step 1:** Add all the above state/computed/functions to the script section.
**Step 2:** Verify build: `cd frontend && npx vite build`

---

### Task 2: Rewrite left sidebar template

**Files:**
- Modify: `src/views/inspection/v7/tasks/TaskExecutionView.vue` (template lines 700-731)

**Replace** the current `section-nav` div with:

```html
<div class="left-nav">
  <!-- Mode toggle -->
  <div class="mode-toggle">
    <button :class="{ active: viewMode === 'target' }" @click="viewMode = 'target'">按目标</button>
    <button :class="{ active: viewMode === 'section' }" @click="viewMode = 'section'">按分区</button>
  </div>

  <!-- TARGET MODE: target list -->
  <template v-if="viewMode === 'target'">
    <div class="target-search">
      <el-input v-model="targetSearch" placeholder="搜索..." size="small" clearable prefix-icon="Search" />
    </div>
    <div class="target-filters">
      <button :class="{ active: targetFilter === 'all' }" @click="targetFilter = 'all'">全部</button>
      <button :class="{ active: targetFilter === 'pending' }" @click="targetFilter = 'pending'">待检</button>
      <button :class="{ active: targetFilter === 'completed' }" @click="targetFilter = 'completed'">已完成</button>
    </div>
    <div class="target-list">
      <div
        v-for="t in filteredTargets" :key="t.targetId"
        class="target-item"
        :class="{ active: selectedTargetId === t.targetId }"
        @click="selectTarget(t.targetId)"
      >
        <span class="t-dot" :class="'t-dot--' + getTargetStatus(t.targetId)" />
        <div class="t-label">
          <span class="t-name">{{ t.targetName }}</span>
          <span class="t-prog">{{ getTargetProgress(t.targetId).done }}/{{ getTargetProgress(t.targetId).total }}</span>
        </div>
      </div>
      <div v-if="filteredTargets.length === 0" class="list-empty">无匹配目标</div>
    </div>
  </template>

  <!-- SECTION MODE: section tree -->
  <template v-else>
    <div class="section-nav-header">分区导航</div>
    <!-- Root section -->
    <div v-if="rootSection"
      class="section-item section-item--root"
      :class="{ active: activeSectionId === rootSection.id }"
      @click="activeSectionId = rootSection.id"
    >
      <span class="sec-dot" :class="'sec-dot--' + getSectionIcon(rootSection.id)" />
      <div class="sec-label-wrap">
        <span class="sec-name">{{ rootSection.sectionName }}</span>
        <span class="sec-prog">{{ getSectionProgress(rootSection.id).done }}/{{ getSectionProgress(rootSection.id).total }}</span>
      </div>
    </div>
    <!-- Child sections (indented) -->
    <div
      v-for="sec in firstLevelSections" :key="sec.id"
      class="section-item section-item--child"
      :class="{ active: activeSectionId === sec.id }"
      @click="activeSectionId = sec.id"
    >
      <span class="sec-dot" :class="'sec-dot--' + getSectionIcon(sec.id)" />
      <div class="sec-label-wrap">
        <span class="sec-name">{{ sec.sectionName }}</span>
        <span class="sec-prog">{{ getSectionProgress(sec.id).done }}/{{ getSectionProgress(sec.id).total }}</span>
      </div>
    </div>
    <div v-if="!rootSection && firstLevelSections.length === 0" class="list-empty">暂无分区</div>
  </template>
</div>
```

**Step 1:** Replace left sidebar template.
**Step 2:** Verify build.

---

### Task 3: Rewrite right panel for dual mode

**Files:**
- Modify: `src/views/inspection/v7/tasks/TaskExecutionView.vue` (template lines 733-1035)

**Section mode (right panel):** Keep largely the same, but remove the el-select dropdown. Instead use a compact horizontal scroll row of target pills (single line, overflow-x auto, no wrap).

**Target mode (right panel):** Replace entire scoring-panel content:

```html
<div class="scoring-panel">
  <template v-if="viewMode === 'target'">
    <!-- Target header -->
    <div class="target-header-row" v-if="selectedTargetId">
      <span class="target-header-name">
        {{ uniqueTargets.find(t => t.targetId === selectedTargetId)?.targetName }}
      </span>
      <div class="target-nav">
        <!-- prev/next target buttons + counter -->
      </div>
    </div>

    <!-- Section cards stack -->
    <div class="form-area" v-loading="detailLoading">
      <div v-if="!selectedTargetId" class="form-placeholder">
        <p>请在左侧选择检查目标</p>
      </div>
      <div v-else class="section-cards">
        <div
          v-for="(group, gi) in targetSectionGroups" :key="gi"
          class="section-card"
        >
          <div class="section-card-header" @click="group.collapsed = !group.collapsed">
            <ChevronRight :size="14" :class="{ 'rotate-90': !group.collapsed }" />
            <span class="sc-name">{{ group.section?.sectionName || '未知分区' }}</span>
            <span class="sc-status">{{ getSubmissionStatusLabel(group.submission) }}</span>
          </div>
          <div v-show="!group.collapsed" class="section-card-body">
            <!-- Reuse existing scoring controls for each detail -->
            <div v-for="detail in group.details" :key="detail.id" class="field-card">
              <!-- Same field-card content as current (DEDUCTION, ADDITION, PASS_FAIL, etc.) -->
            </div>
            <div v-if="group.details.length === 0" class="form-placeholder"><p>暂无检查项</p></div>
          </div>
        </div>
      </div>
    </div>

    <!-- Bottom bar: aggregate score + complete-all button -->
  </template>

  <template v-else>
    <!-- Section mode: same as current (with improvements) -->
  </template>
</div>
```

**Key challenge:** The scoring controls (DEDUCTION, ADDITION, etc.) currently reference `numberInputs[detail.id]`, `selectInputs[detail.id]` etc. These are keyed by detail ID, so they'll work across both modes without change — detail IDs are unique per submission.

**For target mode inputs:** Need to `initInputs` for ALL details across all groups, not just one submission's details. Modify `selectTarget()` to call `initInputs(allDetails)` after loading.

**Step 1:** Add target mode template to right panel.
**Step 2:** Extract scoring field rendering into a reusable template fragment (or just duplicate with v-for).
**Step 3:** Adapt initInputs and isEditable for multi-submission target mode.
**Step 4:** Verify build.

---

### Task 4: Target-mode score bar and completion

**Files:**
- Modify: `src/views/inspection/v7/tasks/TaskExecutionView.vue` (template + script)

**Target mode score bar:** Shows aggregate across all sections for the target:
- Total items scored / total items
- Combined deductions/bonuses
- "Complete All" button that calls `handleComplete` for each IN_PROGRESS submission
- Auto-advance to next target after completing all

```typescript
// Target-mode score summary
const targetScoreSummary = computed(() => {
  let scored = 0, total = 0, deductions = 0, bonuses = 0
  for (const group of targetSectionGroups.value) {
    for (const d of group.details) {
      if (d.scoringMode) total++
      if (d.responseValue != null && d.responseValue !== '') scored++
      // ... accumulate scores
    }
  }
  return { scored, total, deductions, bonuses }
})

async function handleCompleteTarget() {
  // Complete each IN_PROGRESS submission for the selected target
  for (const group of targetSectionGroups.value) {
    if (group.submission.status === 'IN_PROGRESS') {
      // calculate scores for this group, call store.completeSubmission()
    }
  }
  // Reload and auto-advance to next target
  await reloadAll()
  advanceToNextTarget()
}
```

**Step 1:** Add target-mode score summary computed and complete handler.
**Step 2:** Add target-mode score bar template.
**Step 3:** Verify build.

---

### Task 5: CSS for new elements

**Files:**
- Modify: `src/views/inspection/v7/tasks/TaskExecutionView.vue` (style section)

**New styles needed:**
- `.left-nav` — replaces `.section-nav`, same dimensions
- `.mode-toggle` — segmented control
- `.target-search`, `.target-filters` — search + filter bar
- `.target-list`, `.target-item` — scrollable target list (like chat list)
- `.section-item--root`, `.section-item--child` — tree indentation
- `.section-card`, `.section-card-header`, `.section-card-body` — collapsible cards
- `.target-header-row` — target name header in right panel
- `.t-dot`, `.t-dot--COMPLETED`, `.t-dot--IN_PROGRESS`, `.t-dot--PENDING` — status dots

**Step 1:** Write all new CSS.
**Step 2:** Remove unused old CSS.
**Step 3:** Final build verification: `cd frontend && npx vite build`

---

### Task 6: Final verification

**Step 1:** Full build: `cd frontend && npx vite build`
**Step 2:** API smoke test: Verify task 68 data loads correctly via curl
**Step 3:** Review total file line count (should stay under ~2000 lines)
