# 教务管理 Phase 2: 自动排课引擎 + 拖拽排课 + 导出打印

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Implement auto-scheduling engine (CSP + genetic algorithm), drag-and-drop manual scheduling, and schedule export/print functionality.

**Architecture:** Backend auto-scheduling service with async execution + WebSocket-style progress polling. Frontend drag-and-drop on TimetableGrid using HTML5 DnD API. Export via Apache POI (Excel) and HTML-to-PDF.

**Tech Stack:** Java 17, Spring Boot 3.2, MyBatis Plus, Jackson, Apache POI. Vue 3, TypeScript, Element Plus, Tailwind CSS, HTML5 Drag and Drop API.

---

## Task 1: Backend Auto-Scheduling Engine — Core Algorithm

**Files:**
- Create: `backend/src/main/java/com/school/management/application/teaching/AutoSchedulingService.java`

This is the core scheduling algorithm. It implements a hybrid approach:
1. **Phase 1 (CSP)**: Generate feasible initial solution via constraint propagation + backtracking
2. **Phase 2 (GA)**: Optimize soft constraint satisfaction via genetic algorithm

The service works with these data structures:
- **ScheduleSlot**: (teachingClassId, teacherId, dayOfWeek, periodStart, periodEnd, weekStart, weekEnd, weekType, classroomId)
- **TimeSlot**: (day 1-5, period 1-10) — a single cell in the weekly grid
- **TaskRequirement**: (taskId, teachingClassId, courseId, teacherId, classId, weeklyHours, startWeek, endWeek, consecutivePeriods, timesPerWeek)

**Step 1: Create AutoSchedulingService.java**

Key methods:

```java
@Service
@RequiredArgsConstructor
public class AutoSchedulingService {
    private final JdbcTemplate jdbcTemplate;
    private final SchedulingConstraintRepository constraintRepo;
    private final ConstraintApplicationService constraintService;

    /**
     * Main entry point. Runs synchronously (caller should run async if needed).
     * Returns: { success, entriesGenerated, conflicts, executionTime, entries }
     */
    public Map<String, Object> autoSchedule(Long semesterId, Map<String, Object> params) {
        long start = System.currentTimeMillis();
        int maxIterations = (int) params.getOrDefault("maxIterations", 500);
        int populationSize = (int) params.getOrDefault("populationSize", 30);

        // 1. Load all task requirements
        List<TaskRequirement> requirements = loadRequirements(semesterId);

        // 2. Load all constraints
        List<SchedulingConstraint> constraints = constraintRepo.findEnabledBySemesterId(semesterId);

        // 3. Load available classrooms
        List<Map<String, Object>> classrooms = loadClassrooms();

        // 4. Build forbidden time slots per teacher/class/global
        Map<Long, Set<String>> teacherForbidden = buildForbiddenSlots(constraints, ConstraintLevel.TEACHER);
        Map<Long, Set<String>> classForbidden = buildForbiddenSlots(constraints, ConstraintLevel.CLASS);
        Set<String> globalForbidden = buildGlobalForbidden(constraints);

        // 5. Load already-fixed entries (manual placements)
        List<Map<String, Object>> fixedEntries = loadFixedEntries(semesterId);

        // 6. Phase 1: CSP — Generate initial feasible solution
        List<ScheduleSlot> solution = cspSolve(requirements, globalForbidden,
            teacherForbidden, classForbidden, classrooms, fixedEntries);

        // 7. Phase 2: GA — Optimize soft constraints
        if (solution != null && !solution.isEmpty()) {
            solution = gaOptimize(solution, requirements, constraints,
                maxIterations, populationSize);
        }

        // 8. Write solution to schedule_entries
        int entriesGenerated = 0;
        List<Map<String, Object>> conflicts = new ArrayList<>();
        if (solution != null) {
            entriesGenerated = writeSolution(semesterId, solution);
        }

        long elapsed = System.currentTimeMillis() - start;
        Map<String, Object> result = new HashMap<>();
        result.put("success", solution != null && !solution.isEmpty());
        result.put("entriesGenerated", entriesGenerated);
        result.put("conflicts", conflicts);
        result.put("executionTime", elapsed);
        return result;
    }
}
```

Core algorithm methods:

```java
/**
 * CSP Solver: Constraint propagation + backtracking.
 * Assigns each task to a (day, period, classroom) respecting hard constraints.
 */
private List<ScheduleSlot> cspSolve(
        List<TaskRequirement> requirements,
        Set<String> globalForbidden,
        Map<Long, Set<String>> teacherForbidden,
        Map<Long, Set<String>> classForbidden,
        List<Map<String, Object>> classrooms,
        List<Map<String, Object>> fixedEntries) {

    // Sort requirements by constraint tightness (most constrained first)
    requirements.sort((a, b) -> {
        int aSlots = countAvailableSlots(a, globalForbidden, teacherForbidden, classForbidden);
        int bSlots = countAvailableSlots(b, globalForbidden, teacherForbidden, classForbidden);
        return aSlots - bSlots; // Most constrained first
    });

    List<ScheduleSlot> solution = new ArrayList<>();
    // Add fixed entries as already-assigned
    Set<String> teacherOccupied = new HashSet<>(); // "teacherId_day_period"
    Set<String> classOccupied = new HashSet<>();    // "classId_day_period"
    Set<String> roomOccupied = new HashSet<>();     // "roomId_day_period"

    // Mark fixed entries as occupied
    for (Map<String, Object> fixed : fixedEntries) {
        markOccupied(fixed, teacherOccupied, classOccupied, roomOccupied);
    }

    // Try to assign each requirement
    return backtrack(requirements, 0, solution,
        globalForbidden, teacherForbidden, classForbidden,
        teacherOccupied, classOccupied, roomOccupied, classrooms);
}

/**
 * Backtracking search with forward checking.
 */
private List<ScheduleSlot> backtrack(
        List<TaskRequirement> requirements, int index,
        List<ScheduleSlot> current,
        Set<String> globalForbidden,
        Map<Long, Set<String>> teacherForbidden,
        Map<Long, Set<String>> classForbidden,
        Set<String> teacherOccupied, Set<String> classOccupied,
        Set<String> roomOccupied,
        List<Map<String, Object>> classrooms) {

    if (index >= requirements.size()) return current; // All assigned

    TaskRequirement req = requirements.get(index);
    // Generate all valid (day, periodStart) combinations for this requirement
    List<int[]> candidates = generateCandidates(req, globalForbidden,
        teacherForbidden.getOrDefault(req.teacherId, Collections.emptySet()),
        classForbidden.getOrDefault(req.classId, Collections.emptySet()),
        teacherOccupied, classOccupied);

    // Shuffle for randomness (different initial solutions)
    Collections.shuffle(candidates);

    for (int[] candidate : candidates) {
        int day = candidate[0], periodStart = candidate[1];
        int periodEnd = periodStart + req.consecutivePeriods - 1;

        // Find available classroom
        Long roomId = findAvailableRoom(day, periodStart, periodEnd,
            roomOccupied, classrooms, req);

        if (roomId != null) {
            // Assign
            ScheduleSlot slot = new ScheduleSlot(req, day, periodStart, periodEnd, roomId);
            current.add(slot);
            markSlotOccupied(slot, teacherOccupied, classOccupied, roomOccupied);

            // Recurse
            List<ScheduleSlot> result = backtrack(requirements, index + 1, current,
                globalForbidden, teacherForbidden, classForbidden,
                teacherOccupied, classOccupied, roomOccupied, classrooms);

            if (result != null) return result;

            // Backtrack
            current.remove(current.size() - 1);
            unmarkSlotOccupied(slot, teacherOccupied, classOccupied, roomOccupied);
        }
    }

    // Try without room assignment (room can be assigned later)
    // ... fallback logic

    return null; // No valid assignment found
}

/**
 * GA Optimizer: Improve soft constraint satisfaction.
 * Chromosome = permutation of time slot assignments.
 * Fitness = weighted sum of soft constraint satisfaction.
 */
private List<ScheduleSlot> gaOptimize(
        List<ScheduleSlot> initialSolution,
        List<TaskRequirement> requirements,
        List<SchedulingConstraint> constraints,
        int maxIterations, int populationSize) {

    // Extract soft constraints
    List<SchedulingConstraint> softConstraints = constraints.stream()
        .filter(c -> !c.getIsHard()).collect(Collectors.toList());

    if (softConstraints.isEmpty()) return initialSolution;

    // Generate initial population by random swaps
    List<List<ScheduleSlot>> population = new ArrayList<>();
    population.add(new ArrayList<>(initialSolution));
    for (int i = 1; i < populationSize; i++) {
        population.add(mutate(new ArrayList<>(initialSolution), requirements, constraints));
    }

    List<ScheduleSlot> best = initialSolution;
    double bestFitness = evaluateFitness(initialSolution, softConstraints);

    for (int gen = 0; gen < maxIterations; gen++) {
        // Evaluate fitness
        List<double[]> fitnesses = new ArrayList<>();
        for (int i = 0; i < population.size(); i++) {
            double f = evaluateFitness(population.get(i), softConstraints);
            fitnesses.add(new double[]{i, f});
        }
        fitnesses.sort((a, b) -> Double.compare(b[1], a[1]));

        // Track best
        if (fitnesses.get(0)[1] > bestFitness) {
            bestFitness = fitnesses.get(0)[1];
            best = new ArrayList<>(population.get((int) fitnesses.get(0)[0]));
        }

        // Selection + crossover + mutation
        List<List<ScheduleSlot>> nextGen = new ArrayList<>();
        // Elitism: keep top 10%
        int eliteCount = Math.max(1, populationSize / 10);
        for (int i = 0; i < eliteCount; i++) {
            nextGen.add(new ArrayList<>(population.get((int) fitnesses.get(i)[0])));
        }

        // Fill rest with crossover + mutation
        Random rand = new Random();
        while (nextGen.size() < populationSize) {
            int p1 = (int) fitnesses.get(rand.nextInt(populationSize / 2))[0];
            int p2 = (int) fitnesses.get(rand.nextInt(populationSize / 2))[0];
            List<ScheduleSlot> child = crossover(population.get(p1), population.get(p2));
            if (rand.nextDouble() < 0.3) {
                child = mutate(child, requirements, constraints);
            }
            // Validate hard constraints
            if (isValid(child, constraints)) {
                nextGen.add(child);
            } else {
                nextGen.add(new ArrayList<>(population.get(p1))); // fallback
            }
        }

        population = nextGen;
    }

    return best;
}

/**
 * Fitness function: weighted sum of soft constraint satisfaction.
 */
private double evaluateFitness(List<ScheduleSlot> solution,
        List<SchedulingConstraint> softConstraints) {
    double score = 100.0;
    for (SchedulingConstraint c : softConstraints) {
        double weight = c.getPriority() / 100.0;
        double violation = measureViolation(solution, c);
        score -= violation * weight;
    }
    // Add evenness bonus
    score += measureEvenness(solution) * 5;
    return Math.max(0, score);
}
```

**Step 2: Create inner helper class TaskRequirement**

```java
// Inner class or separate file
static class TaskRequirement {
    Long taskId;
    Long teachingClassId;
    Long courseId;
    Long teacherId;
    Long classId;
    int weeklyHours;
    int startWeek;
    int endWeek;
    int consecutivePeriods; // e.g., 2 for double-period
    int timesPerWeek; // sessions per week
}

static class ScheduleSlot {
    Long taskId;
    Long teachingClassId;
    Long courseId;
    Long teacherId;
    Long classId;
    int dayOfWeek;
    int periodStart;
    int periodEnd;
    int weekStart;
    int weekEnd;
    int weekType; // 0=all, 1=odd, 2=even
    Long classroomId;
}
```

**Step 3: Commit**

---

## Task 2: Backend Auto-Scheduling REST Endpoint

**Files:**
- Modify: `backend/src/main/java/com/school/management/interfaces/rest/teaching/TeachingScheduleController.java`

**Step 1: Add auto-schedule endpoint**

Add to TeachingScheduleController:

```java
@Autowired
private AutoSchedulingService autoSchedulingService;

@PostMapping("/auto-schedule")
public Result<Map<String, Object>> autoSchedule(@RequestBody Map<String, Object> params) {
    Long semesterId = Long.parseLong(params.get("semesterId").toString());
    Map<String, Object> result = autoSchedulingService.autoSchedule(semesterId, params);
    return Result.success(result);
}
```

Also add a manual entry move endpoint (for drag-and-drop):

```java
@PostMapping("/{id}/move")
public Result<Map<String, Object>> moveEntry(
        @PathVariable Long id,
        @RequestBody Map<String, Object> body) {
    // Validate new position doesn't conflict
    int newDay = Integer.parseInt(body.get("dayOfWeek").toString());
    int newPeriod = Integer.parseInt(body.get("periodStart").toString());
    Long newClassroomId = body.get("classroomId") != null ?
        Long.parseLong(body.get("classroomId").toString()) : null;

    // Check conflicts at new position
    Long semesterId = Long.parseLong(body.get("semesterId").toString());
    // ... conflict check SQL
    // If no conflict, update the entry
    jdbcTemplate.update(
        "UPDATE schedule_entries SET weekday=?, start_slot=?, end_slot=?, classroom_id=? WHERE id=?",
        newDay, newPeriod, newPeriod + getSpan(id) - 1, newClassroomId, id);

    return Result.success(Map.of("success", true));
}

// Quick conflict check for a single move
@PostMapping("/check-move-conflict")
public Result<Map<String, Object>> checkMoveConflict(@RequestBody Map<String, Object> body) {
    Long entryId = toLong(body.get("entryId"));
    Long semesterId = toLong(body.get("semesterId"));
    int newDay = toInt(body.get("dayOfWeek"));
    int newPeriod = toInt(body.get("periodStart"));

    // Get the entry's teacher, class info
    Map<String, Object> entry = jdbcTemplate.queryForMap(
        "SELECT teacher_id, class_id, end_slot - start_slot + 1 as span FROM schedule_entries WHERE id = ?", entryId);

    int span = ((Number) entry.get("span")).intValue();
    int newEndPeriod = newPeriod + span - 1;
    Long teacherId = ((Number) entry.get("teacher_id")).longValue();
    Long classId = entry.get("class_id") != null ? ((Number) entry.get("class_id")).longValue() : null;

    // Check teacher conflict
    List<Map<String, Object>> teacherConflicts = jdbcTemplate.queryForList(
        "SELECT id, course_id FROM schedule_entries WHERE semester_id=? AND teacher_id=? AND weekday=? " +
        "AND start_slot <= ? AND end_slot >= ? AND id != ? AND deleted=0",
        semesterId, teacherId, newDay, newEndPeriod, newPeriod, entryId);

    // Check class conflict
    List<Map<String, Object>> classConflicts = classId != null ? jdbcTemplate.queryForList(
        "SELECT id, course_id FROM schedule_entries WHERE semester_id=? AND class_id=? AND weekday=? " +
        "AND start_slot <= ? AND end_slot >= ? AND id != ? AND deleted=0",
        semesterId, classId, newDay, newEndPeriod, newPeriod, entryId) : Collections.emptyList();

    boolean hasConflict = !teacherConflicts.isEmpty() || !classConflicts.isEmpty();

    Map<String, Object> result = new HashMap<>();
    result.put("hasConflict", hasConflict);
    result.put("teacherConflicts", teacherConflicts);
    result.put("classConflicts", classConflicts);
    return Result.success(result);
}
```

**Step 2: Commit**

---

## Task 3: Frontend Auto-Schedule Dialog + Progress

**Files:**
- Modify: `frontend/src/views/teaching/ScheduleView.vue` — enhance Tab 1 (排课总览)
- Modify: `frontend/src/api/teaching.ts` — fix auto-schedule API path

**Step 1: Fix scheduleApi.autoSchedule path**

The current API sends to `/schedules/{scheduleId}/auto-schedule` which doesn't match the new endpoint. Fix it:

```typescript
// In scheduleApi object, replace autoSchedule:
autoSchedule: (params: { semesterId: number | string; maxIterations?: number; populationSize?: number }) =>
  request.post<AutoScheduleResult>(`${BASE_URL}/schedules/auto-schedule`, params),

// Add new move/conflict-check methods:
moveEntry: (id: number | string, data: { semesterId: number; dayOfWeek: number; periodStart: number; classroomId?: number }) =>
  request.post(`${BASE_URL}/schedules/${id}/move`, data),

checkMoveConflict: (data: { entryId: number; semesterId: number; dayOfWeek: number; periodStart: number }) =>
  request.post(`${BASE_URL}/schedules/check-move-conflict`, data),
```

**Step 2: Add auto-schedule dialog to ScheduleView Tab 1**

In the 排课总览 tab, the "智能排课" button should open a dialog:

```vue
<!-- Auto Schedule Dialog -->
<el-dialog v-model="autoScheduleDialogVisible" title="智能排课" width="520px">
  <div class="space-y-4">
    <el-alert type="info" :closable="false" class="mb-4">
      <p>系统将基于已配置的约束规则，自动为未排课的教学任务生成课表。</p>
      <p class="mt-1 text-xs text-gray-500">已手动排定的课程不会被调整。</p>
    </el-alert>

    <el-form label-width="120px">
      <el-form-item label="最大迭代次数">
        <el-input-number v-model="autoScheduleForm.maxIterations" :min="100" :max="5000" :step="100" />
      </el-form-item>
      <el-form-item label="种群大小">
        <el-input-number v-model="autoScheduleForm.populationSize" :min="10" :max="100" :step="10" />
      </el-form-item>
    </el-form>

    <!-- Progress display (shown during scheduling) -->
    <div v-if="autoScheduling" class="rounded-lg bg-gray-50 p-4">
      <div class="mb-2 flex items-center justify-between text-sm">
        <span>正在排课...</span>
        <span class="text-gray-500">请稍候</span>
      </div>
      <el-progress :percentage="autoScheduleProgress" :status="autoScheduleProgress >= 100 ? 'success' : ''" />
    </div>

    <!-- Result display -->
    <div v-if="autoScheduleResult" class="rounded-lg border p-4" :class="autoScheduleResult.success ? 'border-green-200 bg-green-50' : 'border-red-200 bg-red-50'">
      <div class="flex items-center gap-2">
        <span v-if="autoScheduleResult.success" class="text-green-600 font-medium">排课完成</span>
        <span v-else class="text-red-600 font-medium">排课未能完成</span>
      </div>
      <div class="mt-2 text-sm text-gray-600 space-y-1">
        <p>生成课程条目: {{ autoScheduleResult.entriesGenerated }} 条</p>
        <p>耗时: {{ (autoScheduleResult.executionTime / 1000).toFixed(1) }} 秒</p>
        <p v-if="autoScheduleResult.conflicts?.length">冲突数: {{ autoScheduleResult.conflicts.length }}</p>
      </div>
    </div>
  </div>
  <template #footer>
    <el-button @click="autoScheduleDialogVisible = false" :disabled="autoScheduling">取消</el-button>
    <el-button type="primary" @click="runAutoSchedule" :loading="autoScheduling" :disabled="autoScheduleResult?.success">
      {{ autoScheduling ? '排课中...' : '开始排课' }}
    </el-button>
  </template>
</el-dialog>
```

Script section additions:

```typescript
const autoScheduleDialogVisible = ref(false)
const autoScheduling = ref(false)
const autoScheduleProgress = ref(0)
const autoScheduleResult = ref<AutoScheduleResult | null>(null)
const autoScheduleForm = ref({
  maxIterations: 500,
  populationSize: 30,
})

async function runAutoSchedule() {
  autoScheduling.value = true
  autoScheduleProgress.value = 0
  autoScheduleResult.value = null

  // Simulate progress (since backend runs synchronously)
  const progressInterval = setInterval(() => {
    if (autoScheduleProgress.value < 90) {
      autoScheduleProgress.value += Math.random() * 15
    }
  }, 500)

  try {
    const result = await scheduleApi.autoSchedule({
      semesterId: semesterId.value,
      ...autoScheduleForm.value,
    })
    autoScheduleProgress.value = 100
    autoScheduleResult.value = result
    if (result.success) {
      ElMessage.success(`排课完成！共生成 ${result.entriesGenerated} 条排课记录`)
      loadData() // Refresh task list
    }
  } catch (e: any) {
    ElMessage.error('排课失败: ' + (e.message || '未知错误'))
  } finally {
    clearInterval(progressInterval)
    autoScheduling.value = false
  }
}
```

**Step 3: Commit**

---

## Task 4: Frontend Drag-and-Drop on TimetableGrid

**Files:**
- Modify: `frontend/src/views/teaching/scheduling/TimetableGrid.vue` — add drag-and-drop support

**Step 1: Enhance TimetableGrid with HTML5 DnD**

Add new props and emits:

```typescript
interface Props {
  entries: ScheduleEntry[]
  periods: PeriodConfig[]
  weekdays?: { value: number; label: string }[]
  editable?: boolean
  constraintMatrix?: any[][] // optional: forbidden slots from time matrix
}

const emit = defineEmits<{
  'entry-click': [entry: ScheduleEntry]
  'cell-click': [day: number, period: number]
  'entry-drop': [entryId: number, newDay: number, newPeriod: number]
}>()
```

Add drag state:

```typescript
const draggedEntry = ref<ScheduleEntry | null>(null)
const dragOverCell = ref<{ day: number; period: number } | null>(null)

function onDragStart(e: DragEvent, entry: ScheduleEntry) {
  if (!props.editable) return
  draggedEntry.value = entry
  e.dataTransfer!.effectAllowed = 'move'
  e.dataTransfer!.setData('text/plain', String(entry.id))
}

function onDragOver(e: DragEvent, day: number, period: number) {
  if (!props.editable || !draggedEntry.value) return
  e.preventDefault()
  e.dataTransfer!.dropEffect = 'move'
  dragOverCell.value = { day, period }
}

function onDragLeave() {
  dragOverCell.value = null
}

function onDrop(e: DragEvent, day: number, period: number) {
  e.preventDefault()
  if (!draggedEntry.value) return
  emit('entry-drop', draggedEntry.value.id, day, period)
  draggedEntry.value = null
  dragOverCell.value = null
}

function onDragEnd() {
  draggedEntry.value = null
  dragOverCell.value = null
}

function isForbiddenSlot(day: number, period: number): boolean {
  if (!props.constraintMatrix) return false
  const dayRow = props.constraintMatrix[day - 1]
  if (!dayRow) return false
  const slot = dayRow[period - 1]
  return slot?.status === 'forbidden'
}
```

Template changes for course card (add draggable):

```html
<div v-for="entry in getEntriesForCell(day.value, period.period)"
     :key="entry.id"
     :draggable="editable"
     class="mb-1 cursor-pointer rounded-md p-1.5 text-white transition-shadow"
     :class="[getEntryColor(entry), editable ? 'cursor-grab active:cursor-grabbing hover:shadow-md' : '']"
     @dragstart="onDragStart($event, entry)"
     @dragend="onDragEnd"
     @click="emit('entry-click', entry)">
  <!-- content -->
</div>
```

Template changes for cells (add drop zones):

```html
<td v-for="day in displayWeekdays" :key="day.value"
    class="border border-gray-200 p-1 align-top transition-colors"
    :class="{
      'bg-gray-100': isForbiddenSlot(day.value, period.period),
      'bg-blue-50 ring-2 ring-inset ring-blue-400': dragOverCell?.day === day.value && dragOverCell?.period === period.period,
      'min-h-[60px]': true
    }"
    @dragover="onDragOver($event, day.value, period.period)"
    @dragleave="onDragLeave"
    @drop="onDrop($event, day.value, period.period)"
    @click="!getEntriesForCell(day.value, period.period).length && emit('cell-click', day.value, period.period)">
```

**Step 2: Handle drop events in ScheduleView**

In ScheduleView.vue, add the handler:

```typescript
async function onEntryDrop(entryId: number, newDay: number, newPeriod: number) {
  // First check for conflicts
  try {
    const check = await scheduleApi.checkMoveConflict({
      entryId,
      semesterId: semesterId.value,
      dayOfWeek: newDay,
      periodStart: newPeriod,
    })

    if (check.hasConflict) {
      const teacherMsg = check.teacherConflicts?.length ? '教师时间冲突' : ''
      const classMsg = check.classConflicts?.length ? '班级时间冲突' : ''
      ElMessage.warning(`无法移动: ${[teacherMsg, classMsg].filter(Boolean).join('、')}`)
      return
    }

    // Move the entry
    await scheduleApi.moveEntry(entryId, {
      semesterId: semesterId.value,
      dayOfWeek: newDay,
      periodStart: newPeriod,
    })
    ElMessage.success('课程已移动')
    loadTimetableData() // Refresh
  } catch (e: any) {
    ElMessage.error('移动失败: ' + (e.message || '未知错误'))
  }
}
```

Pass to TimetableGrid:

```html
<TimetableGrid
  :entries="timetableEntries"
  :periods="periods"
  :editable="true"
  :constraint-matrix="constraintMatrix"
  @entry-click="onEntryClick"
  @cell-click="onCellClick"
  @entry-drop="onEntryDrop"
/>
```

**Step 3: Commit**

---

## Task 5: Backend Schedule Export (Excel)

**Files:**
- Create: `backend/src/main/java/com/school/management/application/teaching/ScheduleExportService.java`
- Modify: `backend/src/main/java/com/school/management/interfaces/rest/teaching/TeachingScheduleController.java`

**Step 1: Add Apache POI dependency check**

Check if `pom.xml` already has POI. If not, the service should use a simple CSV approach or JdbcTemplate + manual Excel generation.

**Step 2: Create ScheduleExportService**

```java
@Service
@RequiredArgsConstructor
public class ScheduleExportService {
    private final JdbcTemplate jdbcTemplate;

    /**
     * Export class schedule to Excel (one sheet per class).
     */
    public byte[] exportClassSchedule(Long semesterId, Long classId) throws IOException {
        // Load entries for the class
        List<Map<String, Object>> entries = jdbcTemplate.queryForList(
            "SELECT se.*, c.course_name, u.real_name as teacher_name, " +
            "cr.name as classroom_name " +
            "FROM schedule_entries se " +
            "LEFT JOIN courses c ON c.id = se.course_id " +
            "LEFT JOIN users u ON u.id = se.teacher_id " +
            "LEFT JOIN classrooms cr ON cr.id = se.classroom_id " +
            "WHERE se.semester_id = ? AND se.class_id = ? AND se.deleted = 0 " +
            "ORDER BY se.weekday, se.start_slot",
            semesterId, classId);

        // Build 10-row × 5-col grid
        String[][] grid = new String[10][5]; // periods × weekdays

        for (Map<String, Object> entry : entries) {
            int day = ((Number) entry.get("weekday")).intValue();
            int startSlot = ((Number) entry.get("start_slot")).intValue();
            int endSlot = ((Number) entry.get("end_slot")).intValue();

            String cellText = String.format("%s\n%s\n%s",
                entry.get("course_name"),
                entry.get("teacher_name"),
                entry.getOrDefault("classroom_name", ""));

            for (int slot = startSlot; slot <= endSlot && slot <= 10; slot++) {
                if (day >= 1 && day <= 5) {
                    grid[slot - 1][day - 1] = cellText;
                }
            }
        }

        // Generate Excel using Apache POI
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("课表");

            // Header row
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("节次");
            String[] dayNames = {"周一", "周二", "周三", "周四", "周五"};
            for (int i = 0; i < 5; i++) {
                headerRow.createCell(i + 1).setCellValue(dayNames[i]);
            }

            // Data rows
            for (int period = 0; period < 10; period++) {
                Row row = sheet.createRow(period + 1);
                row.createCell(0).setCellValue("第" + (period + 1) + "节");
                for (int day = 0; day < 5; day++) {
                    String cellValue = grid[period][day];
                    row.createCell(day + 1).setCellValue(cellValue != null ? cellValue : "");
                }
            }

            // Auto-size columns
            for (int i = 0; i <= 5; i++) {
                sheet.setColumnWidth(i, 5000);
            }

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            workbook.write(bos);
            return bos.toByteArray();
        }
    }

    /**
     * Export teacher schedule to Excel.
     */
    public byte[] exportTeacherSchedule(Long semesterId, Long teacherId) throws IOException {
        // Same pattern but filtered by teacher_id
        // ... similar to above
    }
}
```

**Step 3: Add export endpoints**

```java
@GetMapping("/export/class/{classId}")
public void exportClassSchedule(
        @PathVariable Long classId,
        @RequestParam Long semesterId,
        HttpServletResponse response) throws IOException {
    byte[] data = exportService.exportClassSchedule(semesterId, classId);
    response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    response.setHeader("Content-Disposition", "attachment; filename=class_schedule.xlsx");
    response.getOutputStream().write(data);
}

@GetMapping("/export/teacher/{teacherId}")
public void exportTeacherSchedule(
        @PathVariable Long teacherId,
        @RequestParam Long semesterId,
        HttpServletResponse response) throws IOException {
    byte[] data = exportService.exportTeacherSchedule(semesterId, teacherId);
    response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    response.setHeader("Content-Disposition", "attachment; filename=teacher_schedule.xlsx");
    response.getOutputStream().write(data);
}
```

**Step 4: Commit**

---

## Task 6: Frontend Export Tab Implementation

**Files:**
- Modify: `frontend/src/views/teaching/ScheduleView.vue` — implement Tab 6 (导出打印)
- Modify: `frontend/src/api/teaching.ts` — add export methods

**Step 1: Add export API methods**

```typescript
// Add to scheduleApi:
exportClassSchedule: (semesterId: number | string, classId: number | string) =>
  request.get(`${BASE_URL}/schedules/export/class/${classId}`, {
    params: { semesterId },
    responseType: 'blob',
  }),

exportTeacherSchedule: (semesterId: number | string, teacherId: number | string) =>
  request.get(`${BASE_URL}/schedules/export/teacher/${teacherId}`, {
    params: { semesterId },
    responseType: 'blob',
  }),
```

**Step 2: Implement export tab in ScheduleView**

Replace the placeholder export tab content:

```vue
<div v-if="activeTab === 'export'" class="space-y-5">
  <div class="rounded-xl border border-gray-200 bg-white p-5">
    <h3 class="mb-4 text-sm font-semibold text-gray-900">导出课表</h3>
    <el-form label-width="100px">
      <el-form-item label="导出维度">
        <el-radio-group v-model="exportDimension">
          <el-radio value="class">班级课表</el-radio>
          <el-radio value="teacher">教师课表</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="选择对象">
        <el-select v-model="exportTargetId" placeholder="请选择" class="w-64" filterable>
          <el-option v-for="item in exportTargetOptions" :key="item.value" :value="item.value" :label="item.label" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="exporting" @click="doExport" :disabled="!exportTargetId">
          下载 Excel
        </el-button>
        <el-button @click="doPrint" :disabled="!exportTargetId">
          打印预览
        </el-button>
      </el-form-item>
    </el-form>
  </div>
</div>
```

```typescript
const exportDimension = ref('class')
const exportTargetId = ref<number | null>(null)
const exporting = ref(false)

const exportTargetOptions = computed(() => {
  return exportDimension.value === 'class' ? classList.value : teacherList.value
})

async function doExport() {
  if (!exportTargetId.value) return
  exporting.value = true
  try {
    const blob = exportDimension.value === 'class'
      ? await scheduleApi.exportClassSchedule(semesterId.value, exportTargetId.value)
      : await scheduleApi.exportTeacherSchedule(semesterId.value, exportTargetId.value)

    const url = window.URL.createObjectURL(new Blob([blob]))
    const a = document.createElement('a')
    a.href = url
    a.download = `${exportDimension.value}_schedule.xlsx`
    a.click()
    window.URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch (e: any) {
    ElMessage.error('导出失败')
  } finally {
    exporting.value = false
  }
}

function doPrint() {
  // Open timetable in new window for printing
  // Switch to the timetable tab with the selected target, then use window.print()
  activeTab.value = 'timetable'
  timetableViewMode.value = exportDimension.value
  timetableTargetId.value = exportTargetId.value
  nextTick(() => {
    window.print()
  })
}
```

**Step 3: Commit**

---

## Task 7: Backend Compile + Frontend Type-Check

**Step 1: Backend compile**
```bash
cd backend && JAVA_HOME="/c/Program Files/Java/jdk-17" PATH="/c/Program Files/Java/jdk-17/bin:$PATH" mvn compile -DskipTests
```

**Step 2: Fix any compilation errors**

**Step 3: Frontend check**
```bash
cd frontend && npx vue-tsc --noEmit 2>&1 | grep "views/teaching" | head -20
```

**Step 4: Fix any TypeScript errors in new files**

**Step 5: Final commit**

---

## Summary

| Task | Feature | Estimated Complexity |
|------|---------|---------------------|
| 1 | Auto-scheduling engine (CSP + GA) | High |
| 2 | Auto-schedule + move/conflict-check endpoints | Medium |
| 3 | Auto-schedule dialog + progress UI | Medium |
| 4 | Drag-and-drop on TimetableGrid | Medium |
| 5 | Excel export service | Medium |
| 6 | Export tab UI | Low |
| 7 | Compile verification + fixes | Low |
