-- V26.2.0: courses.exam_type → assessment_method (考核方式)
-- 1=考试 2=考查 3=技能考试 4=考试+考查

ALTER TABLE courses
    CHANGE COLUMN exam_type assessment_method TINYINT DEFAULT 1 COMMENT '考核方式(1=考试,2=考查,3=技能考试,4=考试+考查)';

-- 更新现有数据：体育改为考查
UPDATE courses SET assessment_method = 2 WHERE course_code = 'PE101';
-- 计算机组装改为技能考试
UPDATE courses SET assessment_method = 3 WHERE course_code = '213';

-- curriculum_plan_courses 也有 exam_type
ALTER TABLE curriculum_plan_courses
    CHANGE COLUMN exam_type assessment_method TINYINT DEFAULT 1 COMMENT '考核方式(1=考试,2=考查,3=技能考试,4=考试+考查)';
