-- 给 semesters 加 academic_year_id 显式外键，替代之前的隐式关系
ALTER TABLE `semesters`
    ADD COLUMN `academic_year_id` BIGINT NULL COMMENT '所属学年ID' AFTER `id`;

-- 回填：根据 start_year 匹配 academic_years.year_code 前4位
UPDATE `semesters` s
    JOIN `academic_years` ay ON ay.`year_code` LIKE CONCAT(s.`start_year`, '-%')
SET s.`academic_year_id` = ay.`id`
WHERE s.`academic_year_id` IS NULL AND ay.`deleted` = 0;

-- 加索引
ALTER TABLE `semesters`
    ADD INDEX `idx_semester_academic_year` (`academic_year_id`);
