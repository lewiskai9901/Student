# 考试与成绩模块增强 Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 补全考试与成绩模块的三个关键缺口：成绩 Excel 导入、加权总评计算、考场冲突检测。

**Architecture:** 在现有 DDD 结构上增量开发。成绩导入复用 GradeApplicationService + Apache POI；加权总评引入 grade_weight_configs 表 + 计算服务；考场冲突在 ExamApplicationService 中新增检测方法。所有新接口使用 @CasbinAccess 门控。

**Tech Stack:** Spring Boot 3.2, MyBatis-Plus, Apache POI (already in deps), Vue 3 + Element Plus, TypeScript

---

## Phase 1: 成绩 Excel 导入（Task 1-4）

### 现状
- 前端 GradeEntryPanel.vue 已有导入弹窗 UI（下载模板 → 上传文件）
- 前端 API 已定义 `gradeApi.getImportTemplate(batchId)` 和 `gradeApi.importGrades(batchId, file)`
- **后端缺少这两个端点的实现**

### Task 1: 后端 — 导入模板生成接口

**Files:**
- Modify: `backend/src/main/java/com/school/management/application/teaching/GradeApplicationService.java`
- Modify: `backend/src/main/java/com/school/management/interfaces/rest/teaching/GradeController.java`

**Step 1: 在 GradeApplicationService 中添加模板生成方法**

```java
/**
 * 生成成绩导入模板 Excel
 * 预填学生信息（学号、姓名、班级），教师只需填成绩列
 */
public void generateImportTemplate(Long batchId, HttpServletResponse response) throws IOException {
    GradeBatchPO batch = gradeBatchMapper.selectById(batchId);
    if (batch == null) throw new RuntimeException("批次不存在");

    // 查询批次关联的学生列表（通过 task_id → class_id → students）
    String sql = "SELECT s.id as student_id, s.student_no, s.name as student_name, " +
            "c.class_name FROM students s " +
            "JOIN school_classes c ON s.class_id = c.id " +
            "WHERE s.class_id = ? AND s.deleted = 0 ORDER BY s.student_no";
    List<Map<String, Object>> students = jdbcTemplate.queryForList(sql, batch.getOrgUnitId());

    try (Workbook wb = new XSSFWorkbook()) {
        Sheet sheet = wb.createSheet("成绩导入");

        // 表头样式
        CellStyle headerStyle = wb.createCellStyle();
        Font headerFont = wb.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        // 锁定样式（不可编辑列）
        CellStyle lockedStyle = wb.createCellStyle();
        lockedStyle.setLocked(true);
        Font grayFont = wb.createFont();
        grayFont.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
        lockedStyle.setFont(grayFont);

        // 表头
        String[] headers = {"学号", "姓名", "班级", "成绩", "备注"};
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // 学生数据
        int rowNum = 1;
        for (Map<String, Object> stu : students) {
            Row row = sheet.createRow(rowNum++);
            Cell c0 = row.createCell(0);
            c0.setCellValue(str(stu.get("student_no")));
            c0.setCellStyle(lockedStyle);
            Cell c1 = row.createCell(1);
            c1.setCellValue(str(stu.get("student_name")));
            c1.setCellStyle(lockedStyle);
            Cell c2 = row.createCell(2);
            c2.setCellValue(str(stu.get("class_name")));
            c2.setCellStyle(lockedStyle);
            row.createCell(3); // 成绩 — 留空
            row.createCell(4); // 备注 — 留空
        }

        // 隐藏列：student_id（用于回写时匹配）
        // 放在 F 列（index=5），前端不可见
        for (int i = 1; i <= students.size(); i++) {
            Row row = sheet.getRow(i);
            Cell idCell = row.createCell(5);
            idCell.setCellValue(toLong(students.get(i - 1).get("student_id")));
        }
        sheet.setColumnHidden(5, true);

        // 自适应列宽
        for (int i = 0; i < 5; i++) sheet.autoSizeColumn(i);

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition",
                "attachment; filename=grade_template_" + batch.getBatchCode() + ".xlsx");
        wb.write(response.getOutputStream());
    }
}
```

**Step 2: 在 GradeController 中添加端点**

```java
@GetMapping("/batches/{batchId}/import-template")
@CasbinAccess(resource = "teaching", action = "view")
public void getImportTemplate(@PathVariable Long batchId, HttpServletResponse response) throws IOException {
    gradeApplicationService.generateImportTemplate(batchId, response);
}
```

**Step 3: 验证**

```bash
curl -s -o template.xlsx -w "HTTP %{http_code}" \
  "http://localhost:8080/api/teaching/grades/batches/{batchId}/import-template" \
  -H "Authorization: Bearer $TOKEN"
# 期望：HTTP 200，生成 .xlsx 文件
```

**Step 4: Commit**

```bash
git add backend/src/main/java/com/school/management/application/teaching/GradeApplicationService.java \
       backend/src/main/java/com/school/management/interfaces/rest/teaching/GradeController.java
git commit -m "feat(teaching): grade import template generation endpoint"
```

---

### Task 2: 后端 — Excel 导入解析接口

**Files:**
- Modify: `backend/src/main/java/com/school/management/application/teaching/GradeApplicationService.java`
- Modify: `backend/src/main/java/com/school/management/interfaces/rest/teaching/GradeController.java`

**Step 1: 在 GradeApplicationService 中添加导入方法**

```java
/**
 * 导入 Excel 成绩
 * @return 导入结果：成功数、失败数、错误明细
 */
@Transactional
public Map<String, Object> importGrades(Long batchId, MultipartFile file, Long currentUserId) throws IOException {
    GradeBatchPO batch = gradeBatchMapper.selectById(batchId);
    if (batch == null) throw new RuntimeException("批次不存在");

    List<Map<String, Object>> errors = new ArrayList<>();
    int successCount = 0;

    try (Workbook wb = WorkbookFactory.create(file.getInputStream())) {
        Sheet sheet = wb.getSheetAt(0);
        int lastRow = sheet.getLastRowNum();

        for (int i = 1; i <= lastRow; i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            try {
                String studentNo = getCellString(row.getCell(0));
                BigDecimal score = getCellDecimal(row.getCell(3));
                String remark = getCellString(row.getCell(4));

                // 尝试从隐藏列读 student_id
                Long studentId = getCellLong(row.getCell(5));

                // 若隐藏列为空，通过学号查找
                if (studentId == null && studentNo != null && !studentNo.isBlank()) {
                    studentId = findStudentIdByNo(studentNo);
                }

                if (studentId == null) {
                    errors.add(Map.of("row", i + 1, "error", "找不到学生: " + studentNo));
                    continue;
                }

                if (score == null) {
                    errors.add(Map.of("row", i + 1, "error", "成绩为空"));
                    continue;
                }

                if (score.compareTo(BigDecimal.ZERO) < 0 || score.compareTo(new BigDecimal("100")) > 0) {
                    errors.add(Map.of("row", i + 1, "error", "成绩不在 0-100 范围: " + score));
                    continue;
                }

                // 计算等级和绩点
                String gradeLevel = calcGradeLevel(score);
                BigDecimal gradePoint = calcGradePoint(score);
                boolean passed = score.compareTo(new BigDecimal("60")) >= 0;

                // 查找已有记录 → 更新；无 → 插入
                LambdaQueryWrapper<StudentGradePO> qw = new LambdaQueryWrapper<StudentGradePO>()
                        .eq(StudentGradePO::getBatchId, batchId)
                        .eq(StudentGradePO::getStudentId, studentId);
                StudentGradePO existing = studentGradeMapper.selectOne(qw);

                if (existing != null) {
                    existing.setTotalScore(score);
                    existing.setGradeLevel(gradeLevel);
                    existing.setGradePoint(gradePoint);
                    existing.setPassed(passed ? 1 : 0);
                    existing.setGradeStatus(1);
                    existing.setRemark(remark);
                    existing.setUpdatedAt(LocalDateTime.now());
                    studentGradeMapper.updateById(existing);
                } else {
                    StudentGradePO po = new StudentGradePO();
                    po.setId(IdWorker.getId());
                    po.setBatchId(batchId);
                    po.setSemesterId(batch.getSemesterId());
                    po.setCourseId(batch.getCourseId());
                    po.setStudentId(studentId);
                    po.setOrgUnitId(batch.getOrgUnitId());
                    po.setTotalScore(score);
                    po.setGradeLevel(gradeLevel);
                    po.setGradePoint(gradePoint);
                    po.setPassed(passed ? 1 : 0);
                    po.setGradeStatus(1);
                    po.setRemark(remark);
                    po.setCreatedAt(LocalDateTime.now());
                    po.setUpdatedAt(LocalDateTime.now());
                    studentGradeMapper.insert(po);
                }
                successCount++;
            } catch (Exception e) {
                errors.add(Map.of("row", i + 1, "error", e.getMessage()));
            }
        }
    }

    return Map.of("successCount", successCount, "errorCount", errors.size(), "errors", errors);
}
```

辅助方法：

```java
private String getCellString(Cell cell) {
    if (cell == null) return null;
    cell.setCellType(CellType.STRING);
    return cell.getStringCellValue().trim();
}

private BigDecimal getCellDecimal(Cell cell) {
    if (cell == null) return null;
    if (cell.getCellType() == CellType.NUMERIC) return BigDecimal.valueOf(cell.getNumericCellValue());
    String s = cell.getStringCellValue().trim();
    return s.isEmpty() ? null : new BigDecimal(s);
}

private Long getCellLong(Cell cell) {
    if (cell == null) return null;
    if (cell.getCellType() == CellType.NUMERIC) return (long) cell.getNumericCellValue();
    return null;
}

private Long findStudentIdByNo(String studentNo) {
    try {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM students WHERE student_no = ? AND deleted = 0", Long.class, studentNo);
    } catch (Exception e) { return null; }
}

private String calcGradeLevel(BigDecimal score) {
    int s = score.intValue();
    if (s >= 90) return "A";
    if (s >= 80) return "B";
    if (s >= 70) return "C";
    if (s >= 60) return "D";
    return "F";
}

private BigDecimal calcGradePoint(BigDecimal score) {
    int s = score.intValue();
    if (s >= 90) return new BigDecimal("4.0");
    if (s >= 85) return new BigDecimal("3.7");
    if (s >= 82) return new BigDecimal("3.3");
    if (s >= 78) return new BigDecimal("3.0");
    if (s >= 75) return new BigDecimal("2.7");
    if (s >= 72) return new BigDecimal("2.3");
    if (s >= 68) return new BigDecimal("2.0");
    if (s >= 64) return new BigDecimal("1.5");
    if (s >= 60) return new BigDecimal("1.0");
    return BigDecimal.ZERO;
}
```

**Step 2: 在 GradeController 中添加端点**

```java
@PostMapping("/batches/{batchId}/import")
@CasbinAccess(resource = "teaching", action = "edit")
public Result<Map<String, Object>> importGrades(
        @PathVariable Long batchId,
        @RequestParam("file") MultipartFile file) throws IOException {
    Long userId = UserContextHolder.getCurrentUserId();
    Map<String, Object> result = gradeApplicationService.importGrades(batchId, file, userId);
    return Result.success(result);
}
```

**Step 3: 验证**

```bash
# 先下载模板，填入成绩，再上传
curl -X POST "http://localhost:8080/api/teaching/grades/batches/{batchId}/import" \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@filled_template.xlsx"
# 期望：{"code":200,"data":{"successCount":30,"errorCount":0,"errors":[]}}
```

**Step 4: Commit**

```bash
git commit -m "feat(teaching): grade Excel import with validation"
```

---

### Task 3: 前端 — 验证导入流程端到端

**Files:**
- 可能修改: `frontend/src/views/teaching/grade/GradeEntryPanel.vue`（若弹窗调用需修复）

**Step 1:** 启动前后端，打开成绩录入页面，选择一个批次，点击"批量导入"
**Step 2:** 下载模板 → 确认模板有学生列表预填
**Step 3:** 在模板中填入几个成绩 → 上传
**Step 4:** 确认导入结果显示成功数量，表格刷新后显示新成绩
**Step 5:** Commit（如有前端修改）

---

### Task 4: GradeBatchPO 补充 courseId / orgUnitId 字段

**注意：** Task 1-2 中模板生成和导入都依赖 `batch.getCourseId()` 和 `batch.getOrgUnitId()`。
需确认 grade_batches 表和 GradeBatchPO 是否已有这两个字段。若缺失：

**Files:**
- Modify: `database/schema/teaching_academic_complete_schema.sql`（添加列）
- Create: `database/migrations/V20260416_4__grade_batch_add_course_orgunit.sql`
- Modify: `backend/.../GradeBatchPO.java`（添加字段）

```sql
ALTER TABLE grade_batches ADD COLUMN IF NOT EXISTS course_id BIGINT AFTER semester_id;
ALTER TABLE grade_batches ADD COLUMN IF NOT EXISTS org_unit_id BIGINT AFTER course_id;
ALTER TABLE grade_batches ADD INDEX idx_course (course_id);
```

---

## Phase 2: 加权总评计算（Task 5-8）

### 设计思路
现有 `grade_type` 已区分平时(1)、期中(2)、期末(3)、总评(4)。
加权总评 = 平时分数 × 平时权重 + 期中分数 × 期中权重 + 期末分数 × 期末权重。
只需：一张权重配置表 + 一个计算接口。

### Task 5: 数据库 — 加权配置表

**Files:**
- Create: `database/migrations/V20260416_5__grade_weight_configs.sql`
- Modify: `database/schema/teaching_academic_complete_schema.sql`（追加）

```sql
CREATE TABLE IF NOT EXISTS `grade_weight_configs` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `semester_id` BIGINT NOT NULL,
    `course_id` BIGINT NOT NULL,
    `component_type` TINYINT NOT NULL COMMENT '1=平时 2=期中 3=期末',
    `weight_percent` INT NOT NULL COMMENT '权重百分比，如 20 表示 20%',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_semester_course_component` (`semester_id`, `course_id`, `component_type`),
    INDEX `idx_semester_course` (`semester_id`, `course_id`)
) COMMENT '成绩加权配置表';
```

---

### Task 6: 后端 — 权重配置 CRUD + 总评计算

**Files:**
- Modify: `backend/.../GradeApplicationService.java`
- Modify: `backend/.../GradeController.java`

**新增方法：**

```java
// ---- 权重配置 ----

public List<Map<String, Object>> getWeightConfigs(Long semesterId, Long courseId) {
    return jdbcTemplate.queryForList(
        "SELECT * FROM grade_weight_configs WHERE semester_id = ? AND course_id = ? ORDER BY component_type",
        semesterId, courseId);
}

@Transactional
public void saveWeightConfigs(Long semesterId, Long courseId, List<Map<String, Object>> configs) {
    // 校验权重之和 = 100
    int totalWeight = configs.stream()
        .mapToInt(c -> toInt(c.get("weightPercent"), 0))
        .sum();
    if (totalWeight != 100) throw new RuntimeException("权重之和必须为100%，当前为" + totalWeight + "%");

    jdbcTemplate.update("DELETE FROM grade_weight_configs WHERE semester_id = ? AND course_id = ?",
        semesterId, courseId);

    for (Map<String, Object> c : configs) {
        jdbcTemplate.update(
            "INSERT INTO grade_weight_configs (semester_id, course_id, component_type, weight_percent) VALUES (?, ?, ?, ?)",
            semesterId, courseId, toInt(c.get("componentType"), 0), toInt(c.get("weightPercent"), 0));
    }
}

// ---- 总评计算 ----

@Transactional
public Map<String, Object> calculateOverallGrades(Long semesterId, Long courseId, Long currentUserId) {
    // 1. 读取权重配置
    List<Map<String, Object>> weights = getWeightConfigs(semesterId, courseId);
    if (weights.isEmpty()) throw new RuntimeException("请先配置成绩权重");

    Map<Integer, Integer> weightMap = new HashMap<>(); // componentType → percent
    for (Map<String, Object> w : weights) {
        weightMap.put(toInt(w.get("component_type"), 0), toInt(w.get("weight_percent"), 0));
    }

    // 2. 查询各类型成绩（平时/期中/期末）
    String sql = "SELECT sg.student_id, gb.grade_type, sg.total_score " +
            "FROM student_grades sg JOIN grade_batches gb ON sg.batch_id = gb.id " +
            "WHERE gb.semester_id = ? AND sg.course_id = ? AND sg.deleted = 0 AND gb.deleted = 0 " +
            "AND gb.grade_type IN (1,2,3)";
    List<Map<String, Object>> scores = jdbcTemplate.queryForList(sql, semesterId, courseId);

    // 按学生分组: studentId → {gradeType → score}
    Map<Long, Map<Integer, BigDecimal>> studentScores = new HashMap<>();
    for (Map<String, Object> row : scores) {
        Long sid = toLong(row.get("student_id"));
        int type = toInt(row.get("grade_type"), 0);
        BigDecimal score = toBigDecimal(row.get("total_score"));
        studentScores.computeIfAbsent(sid, k -> new HashMap<>()).put(type, score);
    }

    // 3. 创建或获取总评批次
    GradeBatchPO overallBatch = getOrCreateOverallBatch(semesterId, courseId, currentUserId);

    // 4. 计算每个学生的加权总评
    int calculated = 0;
    int skipped = 0;
    for (Map.Entry<Long, Map<Integer, BigDecimal>> entry : studentScores.entrySet()) {
        Long studentId = entry.getKey();
        Map<Integer, BigDecimal> typeScores = entry.getValue();

        BigDecimal overall = BigDecimal.ZERO;
        boolean complete = true;
        for (Map.Entry<Integer, Integer> wEntry : weightMap.entrySet()) {
            BigDecimal score = typeScores.get(wEntry.getKey());
            if (score == null) { complete = false; break; }
            overall = overall.add(score.multiply(BigDecimal.valueOf(wEntry.getValue()))
                    .divide(BigDecimal.valueOf(100), 1, RoundingMode.HALF_UP));
        }

        if (!complete) { skipped++; continue; }

        // Upsert
        overall = overall.setScale(1, RoundingMode.HALF_UP);
        saveOverallGrade(overallBatch, studentId, courseId, overall);
        calculated++;
    }

    return Map.of("batchId", overallBatch.getId(), "calculated", calculated, "skipped", skipped);
}
```

**新增端点：**

```java
@GetMapping("/weight-configs")
@CasbinAccess(resource = "teaching", action = "view")
public Result<List<Map<String, Object>>> getWeightConfigs(
        @RequestParam Long semesterId, @RequestParam Long courseId) {
    return Result.success(gradeApplicationService.getWeightConfigs(semesterId, courseId));
}

@PutMapping("/weight-configs")
@CasbinAccess(resource = "teaching", action = "edit")
public Result<Void> saveWeightConfigs(@RequestBody Map<String, Object> body) {
    Long semesterId = toLong(body.get("semesterId"));
    Long courseId = toLong(body.get("courseId"));
    List<Map<String, Object>> configs = (List<Map<String, Object>>) body.get("configs");
    gradeApplicationService.saveWeightConfigs(semesterId, courseId, configs);
    return Result.success(null);
}

@PostMapping("/calculate-overall")
@CasbinAccess(resource = "teaching", action = "edit")
public Result<Map<String, Object>> calculateOverall(@RequestBody Map<String, Object> body) {
    Long semesterId = toLong(body.get("semesterId"));
    Long courseId = toLong(body.get("courseId"));
    Long userId = UserContextHolder.getCurrentUserId();
    return Result.success(gradeApplicationService.calculateOverallGrades(semesterId, courseId, userId));
}
```

---

### Task 7: 前端 — 权重配置 + 总评计算 UI

**Files:**
- Create: `frontend/src/views/teaching/grade/WeightConfigDialog.vue`
- Modify: `frontend/src/views/teaching/GradeView.vue`（添加按钮）
- Modify: `frontend/src/api/teaching.ts`（添加 API）

**WeightConfigDialog.vue 核心功能：**
- 弹窗：选择学期+课程 → 显示三行权重输入（平时%、期中%、期末%）
- 实时校验三项之和 = 100
- "保存配置"按钮 → `gradeApi.saveWeightConfigs()`
- "计算总评"按钮 → `gradeApi.calculateOverall()` → 显示结果（计算了 N 人，跳过 M 人）

**GradeView.vue 修改：**
- 在过滤栏右侧添加"加权总评"按钮
- 点击打开 WeightConfigDialog

**API 新增：**

```typescript
// 在 gradeApi 中添加
getWeightConfigs: (semesterId: number, courseId: number) =>
  request.get('/teaching/grades/weight-configs', { params: { semesterId, courseId } }),
saveWeightConfigs: (data: { semesterId: number; courseId: number; configs: Array<{componentType: number; weightPercent: number}> }) =>
  request.put('/teaching/grades/weight-configs', data),
calculateOverall: (data: { semesterId: number; courseId: number }) =>
  request.post('/teaching/grades/calculate-overall', data),
```

---

### Task 8: 端到端验证 — 加权总评

**Step 1:** 创建平时/期中/期末三个批次，各录入几个学生成绩
**Step 2:** 打开加权配置弹窗，设置 20% / 20% / 60%
**Step 3:** 点击"计算总评"→ 确认生成总评批次
**Step 4:** 在批次列表中选中总评批次，查看学生成绩是否正确
**Step 5:** Commit

---

## Phase 3: 考场冲突检测（Task 9-11）

### Task 9: 后端 — 考场冲突检测服务

**Files:**
- Modify: `backend/.../ExamApplicationService.java`

**新增方法：**

```java
/**
 * 检测考试安排中的冲突
 * 返回: 教室冲突、监考教师冲突
 */
public List<Map<String, Object>> detectExamConflicts(Long batchId) {
    List<Map<String, Object>> conflicts = new ArrayList<>();

    // 1. 教室冲突：同一教室、同一日期、时间重叠
    String roomConflictSql =
        "SELECT a1.id as arr1_id, a2.id as arr2_id, " +
        "       r1.classroom_id, r1.room_code, a1.exam_date, " +
        "       a1.start_time as start1, a1.end_time as end1, " +
        "       a2.start_time as start2, a2.end_time as end2 " +
        "FROM exam_arrangements a1 " +
        "JOIN exam_rooms r1 ON r1.arrangement_id = a1.id " +
        "JOIN exam_rooms r2 ON r2.classroom_id = r1.classroom_id AND r2.id != r1.id " +
        "JOIN exam_arrangements a2 ON a2.id = r2.arrangement_id AND a2.batch_id = ? " +
        "WHERE a1.batch_id = ? AND a1.id < a2.id " +
        "  AND a1.exam_date = a2.exam_date " +
        "  AND a1.start_time < a2.end_time AND a2.start_time < a1.end_time";

    List<Map<String, Object>> roomConflicts = jdbcTemplate.queryForList(roomConflictSql, batchId, batchId);
    for (Map<String, Object> c : roomConflicts) {
        conflicts.add(Map.of(
            "type", "ROOM",
            "description", "教室 " + c.get("room_code") + " 在 " + c.get("exam_date") +
                " " + c.get("start1") + "-" + c.get("end1") + " 与 " +
                c.get("start2") + "-" + c.get("end2") + " 冲突",
            "arrangement1Id", c.get("arr1_id"),
            "arrangement2Id", c.get("arr2_id")
        ));
    }

    // 2. 监考教师冲突：同一教师、同一日期、时间重叠
    String teacherConflictSql =
        "SELECT i1.teacher_id, a1.id as arr1_id, a2.id as arr2_id, " +
        "       a1.exam_date, a1.start_time as start1, a1.end_time as end1, " +
        "       a2.start_time as start2, a2.end_time as end2 " +
        "FROM exam_invigilators i1 " +
        "JOIN exam_rooms r1 ON r1.id = i1.room_id " +
        "JOIN exam_arrangements a1 ON a1.id = r1.arrangement_id AND a1.batch_id = ? " +
        "JOIN exam_invigilators i2 ON i2.teacher_id = i1.teacher_id AND i2.id != i1.id " +
        "JOIN exam_rooms r2 ON r2.id = i2.room_id " +
        "JOIN exam_arrangements a2 ON a2.id = r2.arrangement_id AND a2.batch_id = ? " +
        "WHERE a1.id < a2.id " +
        "  AND a1.exam_date = a2.exam_date " +
        "  AND a1.start_time < a2.end_time AND a2.start_time < a1.end_time";

    List<Map<String, Object>> teacherConflicts = jdbcTemplate.queryForList(teacherConflictSql, batchId, batchId);
    for (Map<String, Object> c : teacherConflicts) {
        conflicts.add(Map.of(
            "type", "TEACHER",
            "description", "教师ID " + c.get("teacher_id") + " 在 " + c.get("exam_date") +
                " 有时间冲突",
            "arrangement1Id", c.get("arr1_id"),
            "arrangement2Id", c.get("arr2_id")
        ));
    }

    return conflicts;
}
```

**新增端点：**

```java
@GetMapping("/batches/{batchId}/conflicts")
@CasbinAccess(resource = "teaching", action = "view")
public Result<List<Map<String, Object>>> detectConflicts(@PathVariable Long batchId) {
    return Result.success(examApplicationService.detectExamConflicts(batchId));
}
```

---

### Task 10: 前端 — 冲突检测 UI

**Files:**
- Modify: `frontend/src/views/teaching/ExaminationView.vue` 或 `exam/ExamBatchList.vue`

**功能：**
- 在考试批次操作栏添加"冲突检测"按钮
- 点击后调用 `examApi.detectConflicts(batchId)`
- 若无冲突：绿色成功提示
- 若有冲突：弹窗显示冲突列表（类型 + 描述 + 涉及安排）

**API 新增：**

```typescript
// 在 examApi 中添加
detectConflicts: (batchId: number) =>
  request.get(`/teaching/examinations/batches/${batchId}/conflicts`),
```

---

### Task 11: 端到端验证 — 冲突检测

**Step 1:** 创建一个考试批次，添加两个安排使用同一教室、同一时段
**Step 2:** 点击"冲突检测"→ 应显示教室冲突
**Step 3:** 修改时间避开冲突 → 再次检测 → 应显示无冲突
**Step 4:** Commit + push
