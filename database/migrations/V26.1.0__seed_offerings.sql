-- V26.1.0: Seed semester offerings and class assignments for 2025-2026 S1
-- Each department gets different courses

SET @SEM = 2042273577180405761;

-- Course IDs
SET @MATH   = 1775537914101;  -- 高等数学 wh=4
SET @ENG    = 1775537914102;  -- 大学英语 wh=3
SET @PHY    = 1775537914103;  -- 大学物理 wh=3
SET @CS     = 1775537914104;  -- 计算机基础 wh=2
SET @CHN    = 1775537914105;  -- 语文 wh=3
SET @PE     = 1775537914106;  -- 体育 wh=2
SET @CSASM  = 2040360775176011777; -- 计算机组装 wh=5

-- Grade parent IDs
SET @ECO24 = 2041867773310668802;  -- 经济2024级
SET @ECO25 = 2041867774556377090;  -- 经济2025级
SET @CAR24 = 2041867775017750529;  -- 汽车2024级
SET @CAR25 = 2041867775277797377;  -- 汽车2025级
SET @ART24 = 2041867775604953089;  -- 艺术2024级
SET @ART25 = 2041867776007606274;  -- 艺术2025级
SET @KY24  = 2041870390619279362;  -- 康养2024级
SET @KY25  = 2041870393899225089;  -- 康养2025级

-- ================================================================
-- 1. Semester Course Offerings (每门课每个年级一条,唯一键)
-- ================================================================

INSERT INTO semester_course_offerings
  (semester_id, course_id, applicable_grade, weekly_hours, start_week, end_week, course_category, course_type, status, tenant_id)
VALUES
  -- 2024级课程
  (@SEM, @ENG,   '2024级', 3, 1, 16, 1, 1, 1, 1),
  (@SEM, @CHN,   '2024级', 3, 1, 16, 1, 1, 1, 1),
  (@SEM, @PE,    '2024级', 2, 1, 16, 1, 1, 1, 1),
  (@SEM, @MATH,  '2024级', 4, 1, 16, 1, 1, 1, 1),
  (@SEM, @PHY,   '2024级', 3, 1, 16, 2, 1, 1, 1),
  (@SEM, @CS,    '2024级', 2, 1, 16, 2, 1, 1, 1),
  (@SEM, @CSASM, '2024级', 5, 1, 9,  2, 3, 1, 1),
  -- 2025级课程
  (@SEM, @ENG,   '2025级', 3, 1, 16, 1, 1, 1, 1),
  (@SEM, @CHN,   '2025级', 3, 1, 16, 1, 1, 1, 1),
  (@SEM, @PE,    '2025级', 2, 1, 16, 1, 1, 1, 1),
  (@SEM, @MATH,  '2025级', 4, 1, 16, 1, 1, 1, 1),
  (@SEM, @PHY,   '2025级', 3, 1, 16, 2, 1, 1, 1),
  (@SEM, @CS,    '2025级', 2, 1, 16, 2, 1, 1, 1);

-- ================================================================
-- 2. Class Course Assignments
--    经济系: 英语+语文+体育+高数+计算机基础+(2024级加计算机组装)
--    汽车系: 英语+语文+体育+高数+物理
--    艺术系: 英语+语文+体育+计算机基础
--    康养系: 英语+语文+体育+物理
-- ================================================================

-- 经济2024级 (3班 x 6门课)
INSERT INTO class_course_assignments (semester_id, org_unit_id, offering_id, course_id, weekly_hours, student_count, status, tenant_id)
SELECT @SEM, c.id, o.id, o.course_id, o.weekly_hours, 36, 1, 1
FROM org_units c
CROSS JOIN semester_course_offerings o
WHERE c.parent_id = @ECO24 AND c.unit_type = 'CLASS' AND c.deleted = 0
  AND o.semester_id = @SEM AND o.applicable_grade = '2024级'
  AND o.course_id IN (@ENG, @CHN, @PE, @MATH, @CS, @CSASM);

-- 经济2025级 (3班 x 5门课)
INSERT INTO class_course_assignments (semester_id, org_unit_id, offering_id, course_id, weekly_hours, student_count, status, tenant_id)
SELECT @SEM, c.id, o.id, o.course_id, o.weekly_hours, 35, 1, 1
FROM org_units c
CROSS JOIN semester_course_offerings o
WHERE c.parent_id = @ECO25 AND c.unit_type = 'CLASS' AND c.deleted = 0
  AND o.semester_id = @SEM AND o.applicable_grade = '2025级'
  AND o.course_id IN (@ENG, @CHN, @PE, @MATH, @CS);

-- 汽车2024级 (2班 x 5门课)
INSERT INTO class_course_assignments (semester_id, org_unit_id, offering_id, course_id, weekly_hours, student_count, status, tenant_id)
SELECT @SEM, c.id, o.id, o.course_id, o.weekly_hours, 38, 1, 1
FROM org_units c
CROSS JOIN semester_course_offerings o
WHERE c.parent_id = @CAR24 AND c.unit_type = 'CLASS' AND c.deleted = 0
  AND o.semester_id = @SEM AND o.applicable_grade = '2024级'
  AND o.course_id IN (@ENG, @CHN, @PE, @MATH, @PHY);

-- 汽车2025级 (2班 x 5门课)
INSERT INTO class_course_assignments (semester_id, org_unit_id, offering_id, course_id, weekly_hours, student_count, status, tenant_id)
SELECT @SEM, c.id, o.id, o.course_id, o.weekly_hours, 37, 1, 1
FROM org_units c
CROSS JOIN semester_course_offerings o
WHERE c.parent_id = @CAR25 AND c.unit_type = 'CLASS' AND c.deleted = 0
  AND o.semester_id = @SEM AND o.applicable_grade = '2025级'
  AND o.course_id IN (@ENG, @CHN, @PE, @MATH, @PHY);

-- 艺术2024级 (2班 x 4门课)
INSERT INTO class_course_assignments (semester_id, org_unit_id, offering_id, course_id, weekly_hours, student_count, status, tenant_id)
SELECT @SEM, c.id, o.id, o.course_id, o.weekly_hours, 34, 1, 1
FROM org_units c
CROSS JOIN semester_course_offerings o
WHERE c.parent_id = @ART24 AND c.unit_type = 'CLASS' AND c.deleted = 0
  AND o.semester_id = @SEM AND o.applicable_grade = '2024级'
  AND o.course_id IN (@ENG, @CHN, @PE, @CS);

-- 艺术2025级 (2班 x 4门课)
INSERT INTO class_course_assignments (semester_id, org_unit_id, offering_id, course_id, weekly_hours, student_count, status, tenant_id)
SELECT @SEM, c.id, o.id, o.course_id, o.weekly_hours, 33, 1, 1
FROM org_units c
CROSS JOIN semester_course_offerings o
WHERE c.parent_id = @ART25 AND c.unit_type = 'CLASS' AND c.deleted = 0
  AND o.semester_id = @SEM AND o.applicable_grade = '2025级'
  AND o.course_id IN (@ENG, @CHN, @PE, @CS);

-- 康养2024级 (2班 x 4门课)
INSERT INTO class_course_assignments (semester_id, org_unit_id, offering_id, course_id, weekly_hours, student_count, status, tenant_id)
SELECT @SEM, c.id, o.id, o.course_id, o.weekly_hours, 30, 1, 1
FROM org_units c
CROSS JOIN semester_course_offerings o
WHERE c.parent_id = @KY24 AND c.unit_type = 'CLASS' AND c.deleted = 0
  AND o.semester_id = @SEM AND o.applicable_grade = '2024级'
  AND o.course_id IN (@ENG, @CHN, @PE, @PHY);

-- 康养2025级 (2班 x 4门课)
INSERT INTO class_course_assignments (semester_id, org_unit_id, offering_id, course_id, weekly_hours, student_count, status, tenant_id)
SELECT @SEM, c.id, o.id, o.course_id, o.weekly_hours, 32, 1, 1
FROM org_units c
CROSS JOIN semester_course_offerings o
WHERE c.parent_id = @KY25 AND c.unit_type = 'CLASS' AND c.deleted = 0
  AND o.semester_id = @SEM AND o.applicable_grade = '2025级'
  AND o.course_id IN (@ENG, @CHN, @PE, @PHY);

-- Clear test teaching tasks
DELETE FROM teaching_task_teachers;
DELETE FROM teaching_tasks;
