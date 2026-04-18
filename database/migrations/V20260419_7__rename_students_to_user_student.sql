-- v3 规范化扩展表命名:
--   students -> user_student
--   teacher_profiles -> user_teacher
-- 命名规范 {entity_type}_{type_code_lower}

SET FOREIGN_KEY_CHECKS = 0;

-- 同时 DROP Phase 1 准备的空壳 user_student/user_teacher (避免重名)
DROP TABLE IF EXISTS user_student;
DROP TABLE IF EXISTS user_teacher;

RENAME TABLE students TO user_student;
RENAME TABLE teacher_profiles TO user_teacher;

SET FOREIGN_KEY_CHECKS = 1;
