-- V26.3.0: 为所有教学任务分配教师
DELETE FROM teaching_task_teachers;

-- Step 1: 创建映射表 (dept_id, course_id) → teacher_id
CREATE TEMPORARY TABLE tmp_course_teacher (
  dept_id BIGINT, course_id BIGINT, teacher_id BIGINT,
  PRIMARY KEY (dept_id, course_id)
);

-- Step 2: 用存储过程风格的逐行插入
-- 经济与信息技术系 (dept_id=2040636269108707330, 6个教师, 6门课+1门2024独有)
INSERT INTO tmp_course_teacher VALUES
(2040636269108707330, 1775537914102, (SELECT id FROM users WHERE primary_org_unit_id=2040636269108707330 AND user_type_code='TEACHER' AND deleted=0 ORDER BY id LIMIT 1 OFFSET 0)),
(2040636269108707330, 1775537914105, (SELECT id FROM users WHERE primary_org_unit_id=2040636269108707330 AND user_type_code='TEACHER' AND deleted=0 ORDER BY id LIMIT 1 OFFSET 1)),
(2040636269108707330, 1775537914106, (SELECT id FROM users WHERE primary_org_unit_id=2040636269108707330 AND user_type_code='TEACHER' AND deleted=0 ORDER BY id LIMIT 1 OFFSET 2)),
(2040636269108707330, 1775537914101, (SELECT id FROM users WHERE primary_org_unit_id=2040636269108707330 AND user_type_code='TEACHER' AND deleted=0 ORDER BY id LIMIT 1 OFFSET 3)),
(2040636269108707330, 1775537914104, (SELECT id FROM users WHERE primary_org_unit_id=2040636269108707330 AND user_type_code='TEACHER' AND deleted=0 ORDER BY id LIMIT 1 OFFSET 4)),
(2040636269108707330, 2040360775176011777, (SELECT id FROM users WHERE primary_org_unit_id=2040636269108707330 AND user_type_code='TEACHER' AND deleted=0 ORDER BY id LIMIT 1 OFFSET 5));

-- 汽车工程系 (dept_id=2041864691411632129, 6个教师, 5门课)
INSERT INTO tmp_course_teacher VALUES
(2041864691411632129, 1775537914102, (SELECT id FROM users WHERE primary_org_unit_id=2041864691411632129 AND user_type_code='TEACHER' AND deleted=0 ORDER BY id LIMIT 1 OFFSET 0)),
(2041864691411632129, 1775537914105, (SELECT id FROM users WHERE primary_org_unit_id=2041864691411632129 AND user_type_code='TEACHER' AND deleted=0 ORDER BY id LIMIT 1 OFFSET 1)),
(2041864691411632129, 1775537914106, (SELECT id FROM users WHERE primary_org_unit_id=2041864691411632129 AND user_type_code='TEACHER' AND deleted=0 ORDER BY id LIMIT 1 OFFSET 2)),
(2041864691411632129, 1775537914101, (SELECT id FROM users WHERE primary_org_unit_id=2041864691411632129 AND user_type_code='TEACHER' AND deleted=0 ORDER BY id LIMIT 1 OFFSET 3)),
(2041864691411632129, 1775537914103, (SELECT id FROM users WHERE primary_org_unit_id=2041864691411632129 AND user_type_code='TEACHER' AND deleted=0 ORDER BY id LIMIT 1 OFFSET 4));

-- 艺术教育系 (dept_id=2041864692741226497, 5个教师, 4门课)
INSERT INTO tmp_course_teacher VALUES
(2041864692741226497, 1775537914102, (SELECT id FROM users WHERE primary_org_unit_id=2041864692741226497 AND user_type_code='TEACHER' AND deleted=0 ORDER BY id LIMIT 1 OFFSET 0)),
(2041864692741226497, 1775537914105, (SELECT id FROM users WHERE primary_org_unit_id=2041864692741226497 AND user_type_code='TEACHER' AND deleted=0 ORDER BY id LIMIT 1 OFFSET 1)),
(2041864692741226497, 1775537914106, (SELECT id FROM users WHERE primary_org_unit_id=2041864692741226497 AND user_type_code='TEACHER' AND deleted=0 ORDER BY id LIMIT 1 OFFSET 2)),
(2041864692741226497, 1775537914104, (SELECT id FROM users WHERE primary_org_unit_id=2041864692741226497 AND user_type_code='TEACHER' AND deleted=0 ORDER BY id LIMIT 1 OFFSET 3));

-- 康养系 (dept_id=2041870341340401665, 5个教师, 4门课)
INSERT INTO tmp_course_teacher VALUES
(2041870341340401665, 1775537914102, (SELECT id FROM users WHERE primary_org_unit_id=2041870341340401665 AND user_type_code='TEACHER' AND deleted=0 ORDER BY id LIMIT 1 OFFSET 0)),
(2041870341340401665, 1775537914105, (SELECT id FROM users WHERE primary_org_unit_id=2041870341340401665 AND user_type_code='TEACHER' AND deleted=0 ORDER BY id LIMIT 1 OFFSET 1)),
(2041870341340401665, 1775537914106, (SELECT id FROM users WHERE primary_org_unit_id=2041870341340401665 AND user_type_code='TEACHER' AND deleted=0 ORDER BY id LIMIT 1 OFFSET 2)),
(2041870341340401665, 1775537914103, (SELECT id FROM users WHERE primary_org_unit_id=2041870341340401665 AND user_type_code='TEACHER' AND deleted=0 ORDER BY id LIMIT 1 OFFSET 3));

-- Step 3: 批量插入教师分配
INSERT INTO teaching_task_teachers (task_id, teacher_id, teacher_role, weekly_hours)
SELECT t.id, ct.teacher_id, 1, t.weekly_hours
FROM teaching_tasks t
JOIN org_units cls ON cls.id = t.org_unit_id
JOIN org_units grade ON grade.id = cls.parent_id
JOIN tmp_course_teacher ct ON ct.dept_id = grade.parent_id AND ct.course_id = t.course_id
WHERE t.deleted = 0;

-- Step 4: 更新状态
UPDATE teaching_tasks SET task_status = 1 WHERE deleted = 0;
DELETE FROM schedule_entries;
UPDATE teaching_tasks SET scheduling_status = 0 WHERE deleted = 0;

DROP TEMPORARY TABLE tmp_course_teacher;
