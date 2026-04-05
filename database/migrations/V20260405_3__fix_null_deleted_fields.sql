-- 修复: 旧 JdbcTemplate 代码创建的记录可能 deleted 为 NULL
-- MyBatis Plus @TableLogic 要求 deleted = 0 才能查到
UPDATE academic_years SET deleted = 0 WHERE deleted IS NULL;
UPDATE semesters SET deleted = 0 WHERE deleted IS NULL;
UPDATE academic_event SET deleted = 0 WHERE deleted IS NULL;
UPDATE courses SET deleted = 0 WHERE deleted IS NULL;
UPDATE curriculum_plans SET deleted = 0 WHERE deleted IS NULL;
UPDATE majors SET deleted = 0 WHERE deleted IS NULL;
UPDATE major_directions SET deleted = 0 WHERE deleted IS NULL;
UPDATE teaching_tasks SET deleted = 0 WHERE deleted IS NULL;
UPDATE teaching_classes SET deleted = 0 WHERE deleted IS NULL;
UPDATE semester_course_offerings SET deleted = 0 WHERE deleted IS NULL;
UPDATE class_course_assignments SET deleted = 0 WHERE deleted IS NULL;
UPDATE scheduling_constraints SET deleted = 0 WHERE deleted IS NULL;
UPDATE schedule_entries SET deleted = 0 WHERE deleted IS NULL;
UPDATE schedule_adjustments SET deleted = 0 WHERE deleted IS NULL;
UPDATE exam_batches SET deleted = 0 WHERE deleted IS NULL;
UPDATE student_grades SET deleted = 0 WHERE deleted IS NULL;
