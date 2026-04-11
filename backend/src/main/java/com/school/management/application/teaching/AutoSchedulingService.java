package com.school.management.application.teaching;

import com.school.management.domain.teaching.model.scheduling.ConstraintLevel;
import com.school.management.domain.teaching.model.scheduling.ConstraintType;
import com.school.management.domain.teaching.model.scheduling.SchedulingConstraint;
import com.school.management.domain.teaching.repository.SchedulingConstraintRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AutoSchedulingService {
    private final JdbcTemplate jdbcTemplate;
    private final SchedulingConstraintRepository constraintRepo;
    private final ObjectMapper objectMapper;

    // Runtime config (set per scheduling run)
    private int periodsPerDay = 10;

    // Inner data classes
    static class TaskRequirement {
        Long taskId;
        Long courseId;
        Long teacherId;
        Long orgUnitId;
        Long teachingClassId;
        int weeklyHours;
        int startWeek;
        int endWeek;
        int consecutivePeriods; // default 2 for double-period classes
        int timesPerWeek;      // number of sessions per week
        int weekType;          // 0=每周 1=单周 2=双周
        int studentCount;      // 学生人数(合堂时为合计)
        String courseName;
        List<Long> combinedClassIds;
        List<Long> combinedTaskIds;
    }

    static class ScheduleSlot {
        Long taskId;
        Long courseId;
        Long teacherId;
        Long orgUnitId;
        Long teachingClassId;
        int dayOfWeek;    // 1-5
        int periodStart;  // 1-10
        int periodEnd;    // 1-10
        int weekStart;
        int weekEnd;
        int weekType;     // 0=all
        Long classroomId;
        List<Long> combinedClassIds;  // 合堂涉及的所有班级
        List<Long> combinedTaskIds;   // 合堂涉及的所有任务
    }

    /**
     * Main auto-scheduling entry point.
     */
    @Transactional
    public Map<String, Object> autoSchedule(Long semesterId, Map<String, Object> params) {
        long startTime = System.currentTimeMillis();
        int maxIterations = toInt(params.getOrDefault("maxIterations", 500));
        int populationSize = toInt(params.getOrDefault("populationSize", 30));

        log.info("开始自动排课: semesterId={}, maxIterations={}, populationSize={}",
            semesterId, maxIterations, populationSize);

        // 0. Load period config from calendar (period_configs table)
        int maxPeriodsPerDay = 10; // default
        try {
            List<Map<String, Object>> pconfigs = jdbcTemplate.queryForList(
                "SELECT periods_per_day FROM period_configs WHERE semester_id = ? ORDER BY is_default DESC LIMIT 1", semesterId);
            if (!pconfigs.isEmpty() && pconfigs.get(0).get("periods_per_day") != null) {
                maxPeriodsPerDay = ((Number) pconfigs.get(0).get("periods_per_day")).intValue();
            }
        } catch (Exception e) {
            log.warn("无法加载节次配置，使用默认值 {}", maxPeriodsPerDay);
        }
        this.periodsPerDay = maxPeriodsPerDay;
        log.info("每日节次: {}", periodsPerDay);

        // 1. Load unscheduled task requirements
        List<TaskRequirement> requirements = loadRequirements(semesterId);
        // Count tasks without teachers (skipped)
        long totalUnscheduled = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM teaching_tasks WHERE semester_id = ? AND deleted = 0 AND scheduling_status != 2",
            Long.class, semesterId);
        int skippedNoTeacher = (int)(totalUnscheduled - requirements.size());
        log.info("加载 {} 个待排教学任务 (跳过 {} 个未分配教师的任务)", requirements.size(), skippedNoTeacher);

        if (requirements.isEmpty()) {
            Map<String, Object> result = buildResult(true, 0, Collections.emptyList(), System.currentTimeMillis() - startTime);
            if (skippedNoTeacher > 0) {
                result.put("skippedNoTeacher", skippedNoTeacher);
                result.put("message", "有 " + skippedNoTeacher + " 个任务未分配教师，无法排课。请先在任务落实中分配教师。");
            }
            return result;
        }

        // 2. Load constraints
        List<SchedulingConstraint> constraints = constraintRepo.findEnabledBySemesterId(semesterId);

        // 3. Build forbidden slot sets
        Set<String> globalForbidden = new HashSet<>();
        Map<Long, Set<String>> teacherForbidden = new HashMap<>();
        Map<Long, Set<String>> classForbidden = new HashMap<>();
        Map<Long, Set<String>> courseForbidden = new HashMap<>();
        buildForbiddenSets(constraints, globalForbidden, teacherForbidden, classForbidden, courseForbidden);

        // 4. Load classrooms
        List<Map<String, Object>> classrooms = loadClassrooms();
        log.info("加载 {} 个可用教室", classrooms.size());

        // 5. Load fixed entries (already manually scheduled)
        Set<String> teacherOccupied = new HashSet<>();
        Set<String> classOccupied = new HashSet<>();
        Set<String> roomOccupied = new HashSet<>();
        loadFixedEntries(semesterId, teacherOccupied, classOccupied, roomOccupied);

        // 6. Expand requirements into per-session items
        // e.g., 4 hours/week with 2-hour sessions = 2 sessions
        List<TaskRequirement> sessions = expandToSessions(requirements);
        log.info("展开为 {} 个排课会话", sessions.size());

        // 7. Sort by constraint tightness (most constrained first - MRV heuristic)
        sessions.sort((a, b) -> {
            int aSlots = countAvailable(a, globalForbidden, teacherForbidden, classForbidden, courseForbidden);
            int bSlots = countAvailable(b, globalForbidden, teacherForbidden, classForbidden, courseForbidden);
            return aSlots - bSlots;
        });

        // 8. Phase 1: CSP solve
        List<ScheduleSlot> solution = cspSolve(sessions, globalForbidden,
            teacherForbidden, classForbidden, courseForbidden,
            teacherOccupied, classOccupied, roomOccupied, classrooms);

        if (solution == null || solution.isEmpty()) {
            log.warn("CSP求解失败，尝试贪心分配");
            solution = greedySolve(sessions, globalForbidden,
                teacherForbidden, classForbidden, courseForbidden,
                teacherOccupied, classOccupied, roomOccupied, classrooms);
        }

        // 9. Phase 2: GA optimize soft constraints
        if (solution != null && !solution.isEmpty()) {
            List<SchedulingConstraint> softConstraints = constraints.stream()
                .filter(c -> !c.getIsHard()).collect(Collectors.toList());
            if (!softConstraints.isEmpty()) {
                solution = gaOptimize(solution, sessions, softConstraints,
                    globalForbidden, teacherForbidden, classForbidden,
                    maxIterations, populationSize);
            }
        }

        // 10. Write to database
        int entriesGenerated = 0;
        if (solution != null) {
            entriesGenerated = writeSolution(semesterId, solution);
            updateTaskStatuses(semesterId);
        }

        // 11. 容量警告检测
        List<Map<String, Object>> capacityWarnings = new ArrayList<>();
        if (solution != null) {
            Map<Long, Integer> roomCapMap = new HashMap<>();
            for (Map<String, Object> r : classrooms) {
                roomCapMap.put(toLong(r.get("id")), r.get("capacity") != null ? toInt(r.get("capacity")) : 0);
            }
            for (ScheduleSlot slot : solution) {
                if (slot.classroomId != null) {
                    int cap = roomCapMap.getOrDefault(slot.classroomId, 0);
                    int students = slot.combinedClassIds != null ? sessions.stream()
                        .filter(s -> s.taskId.equals(slot.taskId)).findFirst().map(s -> s.studentCount).orElse(0) : 0;
                    if (students == 0) {
                        students = requirements.stream().filter(r -> r.taskId.equals(slot.taskId))
                            .findFirst().map(r -> r.studentCount).orElse(0);
                    }
                    if (cap > 0 && students > cap) {
                        capacityWarnings.add(Map.of(
                            "taskId", slot.taskId, "classroomId", slot.classroomId,
                            "capacity", cap, "studentCount", students,
                            "message", String.format("教室容量%d不足，学生%d人", cap, students)));
                    }
                }
            }
        }

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("排课完成: 生成 {} 条记录, 容量警告 {} 个, 耗时 {}ms", entriesGenerated, capacityWarnings.size(), elapsed);

        Map<String, Object> result = new HashMap<>(buildResult(solution != null && !solution.isEmpty(), entriesGenerated,
            Collections.emptyList(), elapsed));
        result.put("capacityWarnings", capacityWarnings);
        if (skippedNoTeacher > 0) {
            result.put("skippedNoTeacher", skippedNoTeacher);
        }
        return result;
    }

    // ==================== Data Loading ====================

    private List<TaskRequirement> loadRequirements(Long semesterId) {
        // Load tasks that are not fully scheduled
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT t.id as task_id, t.course_id, t.org_unit_id, t.weekly_hours, " +
            "t.start_week, t.end_week, t.teaching_class_id, " +
            "t.consecutive_periods, " +
            "c.course_name, " +
            "tt.teacher_id " +
            "FROM teaching_tasks t " +
            "LEFT JOIN courses c ON c.id = t.course_id " +
            "INNER JOIN teaching_task_teachers tt ON tt.task_id = t.id AND tt.teacher_role = 1 " +
            "WHERE t.semester_id = ? AND t.deleted = 0 AND t.scheduling_status != 2",
            semesterId);

        List<TaskRequirement> reqs = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            TaskRequirement req = new TaskRequirement();
            req.taskId = toLong(row.get("task_id"));
            req.courseId = toLong(row.get("course_id"));
            req.orgUnitId = toLong(row.get("org_unit_id"));
            req.teacherId = row.get("teacher_id") != null ? toLong(row.get("teacher_id")) : null;
            req.teachingClassId = row.get("teaching_class_id") != null ? toLong(row.get("teaching_class_id")) : null;
            req.weeklyHours = toInt(row.get("weekly_hours"));
            req.startWeek = row.get("start_week") != null ? toInt(row.get("start_week")) : 1;
            req.endWeek = row.get("end_week") != null ? toInt(row.get("end_week")) : 16;
            req.courseName = (String) row.get("course_name");
            req.studentCount = row.get("student_count") != null ? toInt(row.get("student_count")) : 0;
            req.weekType = 0;
            // Read consecutive_periods from task, default 2
            int cp = row.get("consecutive_periods") != null ? toInt(row.get("consecutive_periods")) : 2;
            req.consecutivePeriods = Math.min(cp, req.weeklyHours);
            req.timesPerWeek = (int) Math.ceil((double) req.weeklyHours / req.consecutivePeriods);
            reqs.add(req);
        }
        return reqs;
    }

    private List<Map<String, Object>> loadClassrooms() {
        try {
            // 从 places 表查教室类型的场所
            return jdbcTemplate.queryForList(
                "SELECT id, place_name AS name, " +
                "COALESCE(capacity, CAST(JSON_UNQUOTE(JSON_EXTRACT(attributes, '$.capacity')) AS UNSIGNED), 50) AS capacity " +
                "FROM places WHERE deleted = 0 AND type_code IN ('TYPE_CLASSROOM','CLASSROOM','TYPE_MULTIMEDIA','TYPE_COMPUTER_LAB','TYPE_LAB','TYPE_SMART_CLASS','TYPE_TRAINING') " +
                "ORDER BY capacity DESC");
        } catch (Exception e) {
            log.warn("无法加载教室数据: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private void loadFixedEntries(Long semesterId,
            Set<String> teacherOcc, Set<String> classOcc, Set<String> roomOcc) {
        List<Map<String, Object>> fixed = jdbcTemplate.queryForList(
            "SELECT teacher_id, org_unit_id, classroom_id, weekday, start_slot, end_slot " +
            "FROM schedule_entries WHERE semester_id = ? AND deleted = 0", semesterId);
        for (Map<String, Object> f : fixed) {
            int day = toInt(f.get("weekday"));
            int startSlot = toInt(f.get("start_slot"));
            int endSlot = toInt(f.get("end_slot"));
            for (int p = startSlot; p <= endSlot; p++) {
                String key = day + "_" + p;
                if (f.get("teacher_id") != null) teacherOcc.add(toLong(f.get("teacher_id")) + "_" + key);
                if (f.get("org_unit_id") != null) classOcc.add(toLong(f.get("org_unit_id")) + "_" + key);
                if (f.get("classroom_id") != null) roomOcc.add(toLong(f.get("classroom_id")) + "_" + key);
            }
        }
    }

    // ==================== Constraint Processing ====================

    @SuppressWarnings("unchecked")
    private void buildForbiddenSets(List<SchedulingConstraint> constraints,
            Set<String> globalForbidden,
            Map<Long, Set<String>> teacherForbidden,
            Map<Long, Set<String>> classForbidden,
            Map<Long, Set<String>> courseForbidden) {
        for (SchedulingConstraint c : constraints) {
            if (c.getConstraintType() != ConstraintType.TIME_FORBIDDEN || !c.getIsHard()) continue;
            try {
                Map<String, Object> params = objectMapper.readValue(c.getParams(), Map.class);
                List<Integer> days = (List<Integer>) params.getOrDefault("days", Collections.emptyList());
                List<Integer> periods = (List<Integer>) params.getOrDefault("periods", Collections.emptyList());

                Set<String> slots = new HashSet<>();
                List<Integer> effectiveDays = days.isEmpty() ? List.of(1, 2, 3, 4, 5) : days;
                List<Integer> effectivePeriods = periods.isEmpty() ? List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10) : periods;

                for (int d : effectiveDays) {
                    for (int p : effectivePeriods) {
                        slots.add(d + "_" + p);
                    }
                }

                switch (c.getConstraintLevel()) {
                    case GLOBAL:
                        globalForbidden.addAll(slots);
                        break;
                    case TEACHER:
                        if (c.getTargetId() != null)
                            teacherForbidden.computeIfAbsent(c.getTargetId(), k -> new HashSet<>()).addAll(slots);
                        break;
                    case CLASS:
                        if (c.getTargetId() != null)
                            classForbidden.computeIfAbsent(c.getTargetId(), k -> new HashSet<>()).addAll(slots);
                        break;
                    case COURSE:
                        if (c.getTargetId() != null)
                            courseForbidden.computeIfAbsent(c.getTargetId(), k -> new HashSet<>()).addAll(slots);
                        break;
                }
            } catch (Exception e) {
                log.warn("解析约束参数失败: constraint={}", c.getId(), e);
            }
        }
    }

    private boolean isForbidden(int day, int periodStart, int periodEnd,
            TaskRequirement req,
            Set<String> globalForbidden,
            Map<Long, Set<String>> teacherForbidden,
            Map<Long, Set<String>> classForbidden,
            Map<Long, Set<String>> courseForbidden) {
        for (int p = periodStart; p <= periodEnd; p++) {
            String key = day + "_" + p;
            if (globalForbidden.contains(key)) return true;
            if (req.teacherId != null && teacherForbidden.getOrDefault(req.teacherId, Collections.emptySet()).contains(key)) return true;
            if (req.orgUnitId != null && classForbidden.getOrDefault(req.orgUnitId, Collections.emptySet()).contains(key)) return true;
            if (req.courseId != null && courseForbidden.getOrDefault(req.courseId, Collections.emptySet()).contains(key)) return true;
        }
        return false;
    }

    private int countAvailable(TaskRequirement req,
            Set<String> globalForbidden,
            Map<Long, Set<String>> teacherForbidden,
            Map<Long, Set<String>> classForbidden,
            Map<Long, Set<String>> courseForbidden) {
        int count = 0;
        for (int day = 1; day <= 5; day++) {
            for (int p = 1; p <= periodsPerDay; p++) {
                String key = day + "_" + p;
                boolean forbidden = globalForbidden.contains(key);
                if (!forbidden && req.teacherId != null)
                    forbidden = teacherForbidden.getOrDefault(req.teacherId, Collections.emptySet()).contains(key);
                if (!forbidden && req.orgUnitId != null)
                    forbidden = classForbidden.getOrDefault(req.orgUnitId, Collections.emptySet()).contains(key);
                if (!forbidden) count++;
            }
        }
        return count;
    }

    // ==================== Session Expansion ====================

    private List<TaskRequirement> expandToSessions(List<TaskRequirement> requirements) {
        // 1. 合堂合并: 同一 teachingClassId + courseId 的 task 合并为一组
        //    合堂组只排一次, 但占用所有涉及班级的时段
        Map<String, List<TaskRequirement>> combinedGroups = new LinkedHashMap<>();
        List<TaskRequirement> normalTasks = new ArrayList<>();

        for (TaskRequirement req : requirements) {
            if (req.teachingClassId != null) {
                String groupKey = req.teachingClassId + "_" + req.courseId;
                combinedGroups.computeIfAbsent(groupKey, k -> new ArrayList<>()).add(req);
            } else {
                normalTasks.add(req);
            }
        }

        List<TaskRequirement> sessions = new ArrayList<>();

        // 展开普通课程
        for (TaskRequirement req : normalTasks) {
            for (int i = 0; i < req.timesPerWeek; i++) {
                TaskRequirement session = new TaskRequirement();
                session.taskId = req.taskId;
                session.courseId = req.courseId;
                session.orgUnitId = req.orgUnitId;
                session.teacherId = req.teacherId;
                session.teachingClassId = req.teachingClassId;
                session.startWeek = req.startWeek;
                session.endWeek = req.endWeek;
                session.courseName = req.courseName;
                int remainingHours = req.weeklyHours - i * req.consecutivePeriods;
                session.consecutivePeriods = Math.min(req.consecutivePeriods, remainingHours);
                session.weeklyHours = session.consecutivePeriods;
                session.timesPerWeek = 1;
                session.weekType = req.weekType;
                session.studentCount = req.studentCount;
                sessions.add(session);
            }
        }

        // 展开合堂课程: 取组内第一个 task 的参数, 但记录所有 classId
        for (List<TaskRequirement> group : combinedGroups.values()) {
            TaskRequirement primary = group.get(0);
            List<Long> allClassIds = new ArrayList<>();
            List<Long> allTaskIds = new ArrayList<>();
            int totalStudents = 0;
            for (TaskRequirement t : group) {
                allClassIds.add(t.orgUnitId);
                allTaskIds.add(t.taskId);
                totalStudents += t.studentCount;
            }

            for (int i = 0; i < primary.timesPerWeek; i++) {
                TaskRequirement session = new TaskRequirement();
                session.taskId = primary.taskId;
                session.courseId = primary.courseId;
                session.orgUnitId = primary.orgUnitId; // 主班级
                session.teacherId = primary.teacherId;
                session.teachingClassId = primary.teachingClassId;
                session.startWeek = primary.startWeek;
                session.endWeek = primary.endWeek;
                session.courseName = primary.courseName + "(合堂)";
                session.combinedClassIds = allClassIds;
                session.combinedTaskIds = allTaskIds;
                session.studentCount = totalStudents;
                session.weekType = primary.weekType;
                int remainingHours = primary.weeklyHours - i * primary.consecutivePeriods;
                session.consecutivePeriods = Math.min(primary.consecutivePeriods, remainingHours);
                session.weeklyHours = session.consecutivePeriods;
                session.timesPerWeek = 1;
                sessions.add(session);
            }
        }

        return sessions;
    }

    // ==================== CSP Solver ====================

    private List<ScheduleSlot> cspSolve(List<TaskRequirement> sessions,
            Set<String> globalForbidden,
            Map<Long, Set<String>> teacherForbidden,
            Map<Long, Set<String>> classForbidden,
            Map<Long, Set<String>> courseForbidden,
            Set<String> teacherOcc, Set<String> classOcc, Set<String> roomOcc,
            List<Map<String, Object>> classrooms) {

        List<ScheduleSlot> solution = new ArrayList<>();
        boolean success = backtrack(sessions, 0, solution,
            globalForbidden, teacherForbidden, classForbidden, courseForbidden,
            new HashSet<>(teacherOcc), new HashSet<>(classOcc), new HashSet<>(roomOcc),
            classrooms, 0);

        return success ? solution : null;
    }

    private boolean backtrack(List<TaskRequirement> sessions, int idx,
            List<ScheduleSlot> solution,
            Set<String> globalForbidden,
            Map<Long, Set<String>> teacherForbidden,
            Map<Long, Set<String>> classForbidden,
            Map<Long, Set<String>> courseForbidden,
            Set<String> teacherOcc, Set<String> classOcc, Set<String> roomOcc,
            List<Map<String, Object>> classrooms,
            int backtrackCount) {

        if (idx >= sessions.size()) return true;
        if (backtrackCount > 10000) return false; // Prevent infinite search

        TaskRequirement session = sessions.get(idx);
        List<int[]> candidates = generateCandidates(session, globalForbidden,
            teacherForbidden, classForbidden, courseForbidden,
            teacherOcc, classOcc);

        Collections.shuffle(candidates); // Randomize for diversity

        for (int[] c : candidates) {
            int day = c[0], periodStart = c[1];
            int periodEnd = periodStart + session.consecutivePeriods - 1;

            // Find a room (with capacity check)
            Long roomId = findRoom(day, periodStart, periodEnd, roomOcc, classrooms,
                session.studentCount, session.weekType);

            // Create slot
            ScheduleSlot slot = new ScheduleSlot();
            slot.taskId = session.taskId;
            slot.courseId = session.courseId;
            slot.teacherId = session.teacherId;
            slot.orgUnitId = session.orgUnitId;
            slot.teachingClassId = session.teachingClassId;
            slot.dayOfWeek = day;
            slot.periodStart = periodStart;
            slot.periodEnd = periodEnd;
            slot.weekStart = session.startWeek;
            slot.weekEnd = session.endWeek;
            slot.weekType = session.weekType;
            slot.classroomId = roomId;
            slot.combinedClassIds = session.combinedClassIds;
            slot.combinedTaskIds = session.combinedTaskIds;

            // Mark occupied
            Set<String> addedTeacher = new HashSet<>();
            Set<String> addedClass = new HashSet<>();
            Set<String> addedRoom = new HashSet<>();
            markOccupied(slot, teacherOcc, classOcc, roomOcc, addedTeacher, addedClass, addedRoom);

            solution.add(slot);

            if (backtrack(sessions, idx + 1, solution,
                    globalForbidden, teacherForbidden, classForbidden, courseForbidden,
                    teacherOcc, classOcc, roomOcc, classrooms, backtrackCount + 1)) {
                return true;
            }

            // Backtrack
            solution.remove(solution.size() - 1);
            teacherOcc.removeAll(addedTeacher);
            classOcc.removeAll(addedClass);
            roomOcc.removeAll(addedRoom);
        }

        return false;
    }

    private List<int[]> generateCandidates(TaskRequirement session,
            Set<String> globalForbidden,
            Map<Long, Set<String>> teacherForbidden,
            Map<Long, Set<String>> classForbidden,
            Map<Long, Set<String>> courseForbidden,
            Set<String> teacherOcc, Set<String> classOcc) {

        List<int[]> candidates = new ArrayList<>();
        for (int day = 1; day <= 5; day++) {
            for (int p = 1; p <= periodsPerDay - session.consecutivePeriods + 1; p++) {
                int pEnd = p + session.consecutivePeriods - 1;

                // Check forbidden
                if (isForbidden(day, p, pEnd, session,
                    globalForbidden, teacherForbidden, classForbidden, courseForbidden)) continue;

                // Check occupied (with weekType awareness)
                // weekType: 0=每周, 1=单周, 2=双周
                // 冲突规则: 0与0/1/2都冲突; 1只与0和1冲突; 2只与0和2冲突
                boolean occupied = false;
                int wt = session.weekType;
                for (int pp = p; pp <= pEnd; pp++) {
                    String keyBase = day + "_" + pp;
                    if (session.teacherId != null) {
                        String tid = session.teacherId + "_" + keyBase;
                        if (teacherOcc.contains(tid + "_0") || teacherOcc.contains(tid + "_" + wt)) { occupied = true; break; }
                        if (wt == 0 && (teacherOcc.contains(tid + "_1") || teacherOcc.contains(tid + "_2"))) { occupied = true; break; }
                    }
                    // 检查所有涉及班级(合堂)
                    List<Long> classIds = session.combinedClassIds != null ? session.combinedClassIds : (session.orgUnitId != null ? List.of(session.orgUnitId) : List.of());
                    for (Long cid : classIds) {
                        String cidKey = cid + "_" + keyBase;
                        if (classOcc.contains(cidKey + "_0") || classOcc.contains(cidKey + "_" + wt)) { occupied = true; break; }
                        if (wt == 0 && (classOcc.contains(cidKey + "_1") || classOcc.contains(cidKey + "_2"))) { occupied = true; break; }
                    }
                    if (occupied) break;
                }
                if (occupied) continue;

                candidates.add(new int[]{day, p});
            }
        }
        return candidates;
    }

    // ==================== Greedy Fallback ====================

    private List<ScheduleSlot> greedySolve(List<TaskRequirement> sessions,
            Set<String> globalForbidden,
            Map<Long, Set<String>> teacherForbidden,
            Map<Long, Set<String>> classForbidden,
            Map<Long, Set<String>> courseForbidden,
            Set<String> teacherOcc, Set<String> classOcc, Set<String> roomOcc,
            List<Map<String, Object>> classrooms) {

        Set<String> tOcc = new HashSet<>(teacherOcc);
        Set<String> cOcc = new HashSet<>(classOcc);
        Set<String> rOcc = new HashSet<>(roomOcc);
        List<ScheduleSlot> solution = new ArrayList<>();

        for (TaskRequirement session : sessions) {
            List<int[]> candidates = generateCandidates(session,
                globalForbidden, teacherForbidden, classForbidden, courseForbidden,
                tOcc, cOcc);

            if (!candidates.isEmpty()) {
                int[] best = candidates.get(0);
                int day = best[0], periodStart = best[1];
                int periodEnd = periodStart + session.consecutivePeriods - 1;

                ScheduleSlot slot = new ScheduleSlot();
                slot.taskId = session.taskId;
                slot.courseId = session.courseId;
                slot.teacherId = session.teacherId;
                slot.orgUnitId = session.orgUnitId;
                slot.teachingClassId = session.teachingClassId;
                slot.dayOfWeek = day;
                slot.periodStart = periodStart;
                slot.periodEnd = periodEnd;
                slot.weekStart = session.startWeek;
                slot.weekEnd = session.endWeek;
                slot.weekType = 0;
                slot.classroomId = findRoom(day, periodStart, periodEnd, rOcc, classrooms);

                Set<String> at = new HashSet<>(), ac = new HashSet<>(), ar = new HashSet<>();
                markOccupied(slot, tOcc, cOcc, rOcc, at, ac, ar);
                solution.add(slot);
            } else {
                log.warn("无法为任务 {} 找到可用时间槽", session.taskId);
            }
        }
        return solution;
    }

    // ==================== GA Optimizer ====================

    private List<ScheduleSlot> gaOptimize(List<ScheduleSlot> initial,
            List<TaskRequirement> sessions,
            List<SchedulingConstraint> softConstraints,
            Set<String> globalForbidden,
            Map<Long, Set<String>> teacherForbidden,
            Map<Long, Set<String>> classForbidden,
            int maxIterations, int populationSize) {

        List<List<ScheduleSlot>> population = new ArrayList<>();
        population.add(new ArrayList<>(initial));

        // Generate diverse initial population by random swaps
        Random rand = new Random();
        for (int i = 1; i < populationSize; i++) {
            List<ScheduleSlot> variant = deepCopySolution(initial);
            // Random swap mutation
            int swaps = rand.nextInt(5) + 1;
            for (int s = 0; s < swaps; s++) {
                if (variant.size() >= 2) {
                    int a = rand.nextInt(variant.size());
                    int b = rand.nextInt(variant.size());
                    if (a != b) {
                        // Swap time slots
                        int tempDay = variant.get(a).dayOfWeek;
                        int tempStart = variant.get(a).periodStart;
                        int tempEnd = variant.get(a).periodEnd;
                        variant.get(a).dayOfWeek = variant.get(b).dayOfWeek;
                        variant.get(a).periodStart = variant.get(b).periodStart;
                        variant.get(a).periodEnd = variant.get(b).periodEnd;
                        variant.get(b).dayOfWeek = tempDay;
                        variant.get(b).periodStart = tempStart;
                        variant.get(b).periodEnd = tempEnd;
                    }
                }
            }
            population.add(variant);
        }

        List<ScheduleSlot> best = deepCopySolution(initial);
        double bestFitness = evaluateFitness(initial, softConstraints);

        for (int gen = 0; gen < maxIterations; gen++) {
            // Evaluate all
            double[] fitnesses = new double[population.size()];
            for (int i = 0; i < population.size(); i++) {
                fitnesses[i] = evaluateFitness(population.get(i), softConstraints);
                if (fitnesses[i] > bestFitness) {
                    bestFitness = fitnesses[i];
                    best = deepCopySolution(population.get(i));
                }
            }

            // Selection + mutation for next generation
            List<List<ScheduleSlot>> nextGen = new ArrayList<>();
            // Elitism: keep top 10%
            Integer[] indices = new Integer[population.size()];
            for (int i = 0; i < indices.length; i++) indices[i] = i;
            Arrays.sort(indices, (a, b) -> Double.compare(fitnesses[b], fitnesses[a]));

            int eliteCount = Math.max(2, populationSize / 10);
            for (int i = 0; i < eliteCount; i++) {
                nextGen.add(deepCopySolution(population.get(indices[i])));
            }

            // Fill rest
            while (nextGen.size() < populationSize) {
                int parent = indices[rand.nextInt(Math.max(1, populationSize / 3))];
                List<ScheduleSlot> child = deepCopySolution(population.get(parent));
                // Mutate: random re-assign one session
                if (!child.isEmpty()) {
                    int mutIdx = rand.nextInt(child.size());
                    ScheduleSlot m = child.get(mutIdx);
                    int span = m.periodEnd - m.periodStart;
                    int newDay = rand.nextInt(5) + 1;
                    int maxStart = periodsPerDay - span;
                    int newStart = rand.nextInt(Math.max(1, maxStart)) + 1;
                    m.dayOfWeek = newDay;
                    m.periodStart = newStart;
                    m.periodEnd = newStart + span;
                }
                nextGen.add(child);
            }

            population = nextGen;

            // Early termination if fitness is very high
            if (bestFitness >= 98.0) break;
        }

        log.info("GA优化完成: bestFitness={}", bestFitness);
        return best;
    }

    private double evaluateFitness(List<ScheduleSlot> solution,
            List<SchedulingConstraint> softConstraints) {
        double score = 100.0;

        for (SchedulingConstraint c : softConstraints) {
            double weight = c.getPriority() / 100.0;
            double penalty = evaluateSoftConstraintPenalty(solution, c);
            score -= penalty * weight;
        }

        // Bonus for even distribution across days
        score += measureEvenness(solution) * 2;

        return Math.max(0, score);
    }

    @SuppressWarnings("unchecked")
    private double evaluateSoftConstraintPenalty(List<ScheduleSlot> solution, SchedulingConstraint c) {
        try {
            Map<String, Object> params = objectMapper.readValue(c.getParams(), Map.class);

            switch (c.getConstraintType()) {
                case TIME_PREFERRED: {
                    List<Integer> days = (List<Integer>) params.getOrDefault("days", Collections.emptyList());
                    List<Integer> periods = (List<Integer>) params.getOrDefault("periods", Collections.emptyList());
                    long violations = solution.stream().filter(s -> {
                        if (c.getTargetId() != null) {
                            boolean matchesTarget = false;
                            switch (c.getConstraintLevel()) {
                                case TEACHER: matchesTarget = c.getTargetId().equals(s.teacherId); break;
                                case CLASS: matchesTarget = c.getTargetId().equals(s.orgUnitId); break;
                                case COURSE: matchesTarget = c.getTargetId().equals(s.courseId); break;
                                default: matchesTarget = true;
                            }
                            if (!matchesTarget) return false;
                        }
                        boolean dayOk = days.isEmpty() || days.contains(s.dayOfWeek);
                        boolean periodOk = periods.isEmpty() || periods.contains(s.periodStart);
                        return !(dayOk && periodOk); // violation if NOT in preferred
                    }).count();
                    return violations * 5;
                }
                case SPREAD_EVEN: {
                    // Penalty for same course on consecutive days
                    // Group by taskId, check day spread
                    Map<Long, List<Integer>> taskDays = new HashMap<>();
                    for (ScheduleSlot s : solution) {
                        taskDays.computeIfAbsent(s.taskId, k -> new ArrayList<>()).add(s.dayOfWeek);
                    }
                    double penalty = 0;
                    for (List<Integer> daysList : taskDays.values()) {
                        daysList.sort(Integer::compareTo);
                        for (int i = 1; i < daysList.size(); i++) {
                            if (daysList.get(i) - daysList.get(i - 1) <= 1) {
                                penalty += 5; // consecutive days penalty
                            }
                        }
                    }
                    return penalty;
                }
                case COMPACT_SCHEDULE: {
                    int maxDays = params.get("maxDays") != null ? toInt(params.get("maxDays")) : 4;
                    if (c.getTargetId() != null && c.getConstraintLevel() == ConstraintLevel.TEACHER) {
                        long distinctDays = solution.stream()
                            .filter(s -> c.getTargetId().equals(s.teacherId))
                            .map(s -> s.dayOfWeek)
                            .distinct().count();
                        return Math.max(0, distinctDays - maxDays) * 10;
                    }
                    return 0;
                }
                default:
                    return 0;
            }
        } catch (Exception e) {
            return 0;
        }
    }

    private double measureEvenness(List<ScheduleSlot> solution) {
        // Measure how evenly distributed sessions are across days
        Map<Long, int[]> teacherDayCounts = new HashMap<>(); // teacherId -> [day1Count, ..., day5Count]
        for (ScheduleSlot s : solution) {
            if (s.teacherId != null) {
                int[] counts = teacherDayCounts.computeIfAbsent(s.teacherId, k -> new int[5]);
                if (s.dayOfWeek >= 1 && s.dayOfWeek <= 5) counts[s.dayOfWeek - 1]++;
            }
        }

        double totalEvenness = 0;
        for (int[] counts : teacherDayCounts.values()) {
            double mean = Arrays.stream(counts).average().orElse(0);
            double variance = Arrays.stream(counts).mapToDouble(cnt -> Math.pow(cnt - mean, 2)).average().orElse(0);
            totalEvenness += Math.max(0, 10 - variance * 3); // Higher is better
        }
        return teacherDayCounts.isEmpty() ? 0 : totalEvenness / teacherDayCounts.size();
    }

    // ==================== Helper Methods ====================

    /**
     * 查找可用教室，优先匹配容量
     * @param studentCount 需要容纳的学生数(0=不限)
     * @param weekType 周类型(用于冲突检测: 单双周不冲突)
     */
    private Long findRoom(int day, int periodStart, int periodEnd,
            Set<String> roomOcc, List<Map<String, Object>> classrooms,
            int studentCount, int weekType) {
        Long fallbackRoom = null; // 容量不足但时间可用的教室(作为兜底)
        for (Map<String, Object> room : classrooms) {
            Long roomId = toLong(room.get("id"));
            int capacity = room.get("capacity") != null ? toInt(room.get("capacity")) : 0;
            boolean occupied = false;
            for (int p = periodStart; p <= periodEnd; p++) {
                String keyEvery = roomId + "_" + day + "_" + p + "_0";
                String keyThis = roomId + "_" + day + "_" + p + "_" + weekType;
                // 检查冲突: 每周课(0)与任何课冲突; 单周(1)只与每周(0)和单周(1)冲突
                if (roomOcc.contains(keyEvery) || roomOcc.contains(keyThis)) {
                    occupied = true;
                    break;
                }
                // 如果当前是每周课，还要检查单双周是否有占用
                if (weekType == 0) {
                    if (roomOcc.contains(roomId + "_" + day + "_" + p + "_1") ||
                        roomOcc.contains(roomId + "_" + day + "_" + p + "_2")) {
                        occupied = true;
                        break;
                    }
                }
            }
            if (!occupied) {
                if (studentCount <= 0 || capacity >= studentCount) {
                    return roomId; // 容量匹配
                }
                if (fallbackRoom == null) fallbackRoom = roomId; // 容量不足但可用
            }
        }
        return fallbackRoom; // 返回兜底(可能容量不足，后续标警告)
    }

    // 兼容旧调用
    private Long findRoom(int day, int periodStart, int periodEnd,
            Set<String> roomOcc, List<Map<String, Object>> classrooms) {
        return findRoom(day, periodStart, periodEnd, roomOcc, classrooms, 0, 0);
    }

    private void markOccupied(ScheduleSlot slot,
            Set<String> teacherOcc, Set<String> classOcc, Set<String> roomOcc,
            Set<String> addedTeacher, Set<String> addedClass, Set<String> addedRoom) {
        int wt = slot.weekType;
        for (int p = slot.periodStart; p <= slot.periodEnd; p++) {
            String key = slot.dayOfWeek + "_" + p;
            String keyWt = key + "_" + wt;
            if (slot.teacherId != null) {
                String tk = slot.teacherId + "_" + keyWt;
                teacherOcc.add(tk);
                addedTeacher.add(tk);
            }
            // 合堂: 占用所有涉及班级的时段
            if (slot.combinedClassIds != null && !slot.combinedClassIds.isEmpty()) {
                for (Long cid : slot.combinedClassIds) {
                    String ck = cid + "_" + keyWt;
                    classOcc.add(ck);
                    addedClass.add(ck);
                }
            } else if (slot.orgUnitId != null) {
                String ck = slot.orgUnitId + "_" + keyWt;
                classOcc.add(ck);
                addedClass.add(ck);
            }
            if (slot.classroomId != null) {
                String rk = slot.classroomId + "_" + keyWt;
                roomOcc.add(rk);
                addedRoom.add(rk);
            }
        }
    }

    private List<ScheduleSlot> deepCopySolution(List<ScheduleSlot> source) {
        List<ScheduleSlot> copy = new ArrayList<>(source.size());
        for (ScheduleSlot s : source) {
            ScheduleSlot c = new ScheduleSlot();
            c.taskId = s.taskId;
            c.courseId = s.courseId;
            c.teacherId = s.teacherId;
            c.orgUnitId = s.orgUnitId;
            c.teachingClassId = s.teachingClassId;
            c.dayOfWeek = s.dayOfWeek;
            c.periodStart = s.periodStart;
            c.periodEnd = s.periodEnd;
            c.weekStart = s.weekStart;
            c.weekEnd = s.weekEnd;
            c.weekType = s.weekType;
            c.classroomId = s.classroomId;
            copy.add(c);
        }
        return copy;
    }

    private int writeSolution(Long semesterId, List<ScheduleSlot> solution) {
        int count = 0;
        for (ScheduleSlot slot : solution) {
            if (slot.combinedTaskIds != null && slot.combinedTaskIds.size() > 1) {
                // 合堂: 为每个 task(每个班级) 生成一条 entry, 共享相同时间和教室
                for (int i = 0; i < slot.combinedTaskIds.size(); i++) {
                    Long taskId = slot.combinedTaskIds.get(i);
                    Long orgUnitId = slot.combinedClassIds.get(i);
                    jdbcTemplate.update(
                        "INSERT INTO schedule_entries (id, semester_id, task_id, course_id, org_unit_id, teacher_id, " +
                        "classroom_id, teaching_class_id, weekday, start_slot, end_slot, start_week, end_week, " +
                        "week_type, schedule_type, entry_status, conflict_flag, deleted) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, 1, 0, 0)",
                        com.baomidou.mybatisplus.core.toolkit.IdWorker.getId(),
                        semesterId, taskId, slot.courseId, orgUnitId, slot.teacherId,
                        slot.classroomId, slot.teachingClassId,
                        slot.dayOfWeek, slot.periodStart, slot.periodEnd,
                        slot.weekStart, slot.weekEnd, slot.weekType);
                    count++;
                }
            } else {
                // 普通课: 一条 entry
                jdbcTemplate.update(
                    "INSERT INTO schedule_entries (id, semester_id, task_id, course_id, org_unit_id, teacher_id, " +
                    "classroom_id, teaching_class_id, weekday, start_slot, end_slot, start_week, end_week, " +
                    "week_type, schedule_type, entry_status, conflict_flag, deleted) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, 1, 0, 0)",
                    com.baomidou.mybatisplus.core.toolkit.IdWorker.getId(),
                    semesterId, slot.taskId, slot.courseId, slot.orgUnitId, slot.teacherId,
                    slot.classroomId, slot.teachingClassId,
                    slot.dayOfWeek, slot.periodStart, slot.periodEnd,
                    slot.weekStart, slot.weekEnd, slot.weekType);
                count++;
            }
        }
        return count;
    }

    private void updateTaskStatuses(Long semesterId) {
        // Update scheduling_status for tasks that are now fully scheduled
        jdbcTemplate.update(
            "UPDATE teaching_tasks t SET scheduling_status = 2 WHERE semester_id = ? AND deleted = 0 " +
            "AND (SELECT COALESCE(SUM(e.end_slot - e.start_slot + 1), 0) " +
            "     FROM schedule_entries e WHERE e.task_id = t.id AND e.deleted = 0) >= t.weekly_hours",
            semesterId);
        // Partial
        jdbcTemplate.update(
            "UPDATE teaching_tasks t SET scheduling_status = 1 WHERE semester_id = ? AND deleted = 0 " +
            "AND scheduling_status != 2 " +
            "AND (SELECT COUNT(*) FROM schedule_entries e WHERE e.task_id = t.id AND e.deleted = 0) > 0",
            semesterId);
    }

    private Map<String, Object> buildResult(boolean success, int entriesGenerated,
            List<Map<String, Object>> conflicts, long executionTime) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("entriesGenerated", entriesGenerated);
        result.put("conflicts", conflicts);
        result.put("executionTime", executionTime);
        return result;
    }

    private Long toLong(Object val) {
        if (val == null) return null;
        if (val instanceof Number) return ((Number) val).longValue();
        return Long.parseLong(val.toString());
    }

    private int toInt(Object val) {
        if (val == null) return 0;
        if (val instanceof Number) return ((Number) val).intValue();
        return Integer.parseInt(val.toString());
    }
}
