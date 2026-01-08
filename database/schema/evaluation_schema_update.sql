-- =====================================================
-- 学生综合素质测评模块 - 数据库表结构更新
-- 版本: 1.1
-- 更新日期: 2025-11-28
-- =====================================================

SET NAMES utf8mb4;

-- ---------------------------------------------------
-- 1. 为 semesters 表添加 description 和 deleted 字段
-- ---------------------------------------------------
ALTER TABLE `semesters`
ADD COLUMN `description` VARCHAR(500) COMMENT '描述' AFTER `status`,
ADD COLUMN `deleted` TINYINT DEFAULT 0 COMMENT '是否删除' AFTER `updated_at`;

-- ---------------------------------------------------
-- 2. 为 courses 表添加 semester_id 和 hours 字段（如果不存在）
-- ---------------------------------------------------
-- 检查是否有 semester_id 字段
-- ALTER TABLE `courses` ADD COLUMN `semester_id` BIGINT COMMENT '学期ID' AFTER `department_id`;
-- ALTER TABLE `courses` ADD COLUMN `hours` INT COMMENT '学时' AFTER `total_hours`;
-- ALTER TABLE `courses` ADD COLUMN `teacher_name` VARCHAR(100) COMMENT '授课教师' AFTER `department_id`;
-- ALTER TABLE `courses` ADD INDEX `idx_semester` (`semester_id`);

-- ---------------------------------------------------
-- 3. 初始化综测维度配置数据
-- ---------------------------------------------------
INSERT INTO `evaluation_dimensions` (`dimension_code`, `dimension_name`, `weight`, `base_score`, `max_bonus_score`, `min_total_score`, `max_total_score`, `calculation_formula`, `sort_order`, `status`) VALUES
('MORAL', '德育', 25.00, 60.00, 40.00, 0.00, 100.00, '德育素质分 = 基础分(60) + 奖励分(≤40) - 扣分，范围0-100', 1, 1),
('INTELLECTUAL', '智育', 40.00, 60.00, 40.00, 0.00, 100.00, '智育素质分 = 学业成绩转换分，以加权平均分为基准', 2, 1),
('PHYSICAL', '体育', 10.00, 60.00, 40.00, 0.00, 100.00, '体育素质分 = 基础分(60) + 奖励分(≤40) - 扣分', 3, 1),
('AESTHETIC', '美育', 10.00, 60.00, 40.00, 0.00, 100.00, '美育素质分 = 基础分(60) + 奖励分(≤40) - 扣分', 4, 1),
('LABOR', '劳育', 10.00, 60.00, 40.00, 0.00, 100.00, '劳育素质分 = 基础分(60) + 奖励分(≤40) - 扣分', 5, 1),
('DEVELOPMENT', '发展素质', 5.00, 0.00, 100.00, 0.00, 100.00, '发展素质分 = 纯奖励分，上限100分', 6, 1)
ON DUPLICATE KEY UPDATE
`dimension_name` = VALUES(`dimension_name`),
`weight` = VALUES(`weight`);

-- ---------------------------------------------------
-- 4. 初始化荣誉类型数据
-- ---------------------------------------------------
INSERT INTO `honor_types` (`type_code`, `type_name`, `category`, `evaluation_dimension`, `description`, `required_attachments`, `sort_order`, `status`) VALUES
-- 竞赛类
('COMPETITION_TECH', '科技创新竞赛', 'COMPETITION', 'DEVELOPMENT', '各类科技创新、创业大赛等', 1, 1, 1),
('COMPETITION_SKILL', '技能竞赛', 'COMPETITION', 'INTELLECTUAL', '职业技能竞赛', 1, 2, 1),
('COMPETITION_SPORTS', '体育竞赛', 'COMPETITION', 'PHYSICAL', '各级体育比赛', 1, 3, 1),
('COMPETITION_ART', '文艺竞赛', 'COMPETITION', 'AESTHETIC', '各类文艺比赛', 1, 4, 1),
-- 证书类
('CERTIFICATE_LANGUAGE', '语言证书', 'CERTIFICATE', 'INTELLECTUAL', '英语四六级、普通话等证书', 1, 10, 1),
('CERTIFICATE_COMPUTER', '计算机证书', 'CERTIFICATE', 'INTELLECTUAL', '计算机等级考试证书', 1, 11, 1),
('CERTIFICATE_PROFESSIONAL', '专业资格证书', 'CERTIFICATE', 'INTELLECTUAL', '专业技术资格证书', 1, 12, 1),
-- 荣誉称号类
('TITLE_OUTSTANDING', '优秀学生', 'TITLE', 'MORAL', '三好学生、优秀学生干部等', 1, 20, 1),
('TITLE_SCHOLARSHIP', '奖学金', 'TITLE', 'INTELLECTUAL', '各类奖学金', 1, 21, 1),
-- 活动类
('ACTIVITY_VOLUNTEER', '志愿服务', 'ACTIVITY', 'MORAL', '志愿服务活动', 0, 30, 1),
('ACTIVITY_SOCIAL', '社会实践', 'ACTIVITY', 'LABOR', '社会实践活动', 0, 31, 1),
-- 论文成果类
('PUBLICATION_PAPER', '论文发表', 'PUBLICATION', 'DEVELOPMENT', '学术论文发表', 1, 40, 1),
('PUBLICATION_PATENT', '专利发明', 'PUBLICATION', 'DEVELOPMENT', '专利发明', 1, 41, 1)
ON DUPLICATE KEY UPDATE
`type_name` = VALUES(`type_name`),
`category` = VALUES(`category`);

-- ---------------------------------------------------
-- 5. 初始化行为类型数据（与量化系统对接）
-- ---------------------------------------------------
INSERT INTO `behavior_types` (`behavior_code`, `behavior_name`, `behavior_category`, `behavior_nature`, `default_affect_scope`, `description`, `sort_order`, `status`) VALUES
-- 考勤类
('LATE', '迟到', 'ATTENDANCE', 2, 1, '上课迟到', 1, 1),
('LEAVE_EARLY', '早退', 'ATTENDANCE', 2, 1, '上课早退', 2, 1),
('ABSENT', '旷课', 'ATTENDANCE', 2, 1, '无故缺课', 3, 1),
('SICK_LEAVE', '病假', 'ATTENDANCE', 3, 1, '因病请假', 4, 1),
('PERSONAL_LEAVE', '事假', 'ATTENDANCE', 3, 1, '因事请假', 5, 1),
-- 纪律类
('CLASSROOM_VIOLATION', '课堂纪律违规', 'DISCIPLINE', 2, 1, '上课玩手机、睡觉等', 10, 1),
('DORM_LATE_RETURN', '宿舍晚归', 'DISCIPLINE', 2, 1, '超过规定时间回宿舍', 11, 1),
('DORM_NOT_RETURN', '夜不归宿', 'DISCIPLINE', 2, 1, '未经批准在外过夜', 12, 1),
('DORM_ELECTRICAL', '违规用电', 'DISCIPLINE', 2, 2, '使用违规电器', 13, 1),
-- 卫生类
('HYGIENE_PERSONAL', '个人卫生不合格', 'HYGIENE', 2, 1, '个人卫生检查不合格', 20, 1),
('HYGIENE_DORM', '宿舍卫生不合格', 'HYGIENE', 2, 2, '宿舍卫生检查不合格', 21, 1),
('HYGIENE_CLASS', '教室卫生不合格', 'HYGIENE', 2, 3, '教室卫生检查不合格', 22, 1),
-- 正向行为
('VOLUNTEER', '志愿服务', 'ACTIVITY', 1, 1, '参与志愿服务活动', 30, 1),
('ACTIVITY_ORGANIZE', '活动组织', 'ACTIVITY', 1, 1, '组织班级或社团活动', 31, 1),
('STUDY_HELP', '学习互助', 'STUDY', 1, 1, '帮助同学学习进步', 32, 1)
ON DUPLICATE KEY UPDATE
`behavior_name` = VALUES(`behavior_name`),
`behavior_category` = VALUES(`behavior_category`);

-- ---------------------------------------------------
-- 6. 初始化荣誉等级配置数据
-- ---------------------------------------------------
INSERT INTO `honor_level_configs` (`honor_type_id`, `level_code`, `level_name`, `rank_code`, `rank_name`, `score`, `max_count`, `priority`, `sort_order`)
SELECT
    ht.id, 'NATIONAL', '国家级', 'FIRST', '一等奖', 10.00, NULL, 100, 1
FROM `honor_types` ht WHERE ht.type_code = 'COMPETITION_TECH'
ON DUPLICATE KEY UPDATE `score` = VALUES(`score`);

INSERT INTO `honor_level_configs` (`honor_type_id`, `level_code`, `level_name`, `rank_code`, `rank_name`, `score`, `max_count`, `priority`, `sort_order`)
SELECT
    ht.id, 'NATIONAL', '国家级', 'SECOND', '二等奖', 8.00, NULL, 99, 2
FROM `honor_types` ht WHERE ht.type_code = 'COMPETITION_TECH'
ON DUPLICATE KEY UPDATE `score` = VALUES(`score`);

INSERT INTO `honor_level_configs` (`honor_type_id`, `level_code`, `level_name`, `rank_code`, `rank_name`, `score`, `max_count`, `priority`, `sort_order`)
SELECT
    ht.id, 'PROVINCIAL', '省级', 'FIRST', '一等奖', 6.00, NULL, 80, 3
FROM `honor_types` ht WHERE ht.type_code = 'COMPETITION_TECH'
ON DUPLICATE KEY UPDATE `score` = VALUES(`score`);

INSERT INTO `honor_level_configs` (`honor_type_id`, `level_code`, `level_name`, `rank_code`, `rank_name`, `score`, `max_count`, `priority`, `sort_order`)
SELECT
    ht.id, 'SCHOOL', '校级', 'FIRST', '一等奖', 3.00, NULL, 50, 4
FROM `honor_types` ht WHERE ht.type_code = 'COMPETITION_TECH'
ON DUPLICATE KEY UPDATE `score` = VALUES(`score`);

-- 完成
