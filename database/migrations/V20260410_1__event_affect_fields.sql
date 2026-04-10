ALTER TABLE `academic_event`
    ADD COLUMN `affect_type` TINYINT DEFAULT 0 COMMENT '排课影响: 0=无 1=全天停课 2=半天停课 3=补课日 4=考试周' AFTER `description`,
    ADD COLUMN `substitute_weekday` TINYINT NULL COMMENT '补课日按周几课表(1=周一...5=周五)' AFTER `affect_type`,
    ADD COLUMN `affect_slots` VARCHAR(20) NULL COMMENT '半天停课节次范围(如1-4)' AFTER `substitute_weekday`;
