-- =====================================================
-- 学生综合素质测评模块 - 初始化数据
-- 版本: 1.0
-- 创建日期: 2025-11-28
-- =====================================================

SET NAMES utf8mb4;

-- =====================================================
-- 1. 综测维度配置初始化
-- =====================================================
INSERT INTO `evaluation_dimensions` (`dimension_code`, `dimension_name`, `weight`, `base_score`, `max_bonus_score`, `min_total_score`, `max_total_score`, `calculation_formula`, `sort_order`) VALUES
('MORAL', '德育', 25.00, 60.00, 40.00, 0.00, 100.00, '德育总分 = min(100, max(0, 基础分60 + 奖励分 - 扣分))，有处分时上限受限', 1),
('INTELLECTUAL', '智育', 40.00, 60.00, 40.00, 0.00, 100.00, '智育基础分由学业成绩转换，奖励分来自学业竞赛和学术成果', 2),
('PHYSICAL', '体育', 10.00, 60.00, 40.00, 0.00, 100.00, '体育基础分60分，体育竞赛、达标测试可获得奖励分', 3),
('AESTHETIC', '美育', 10.00, 60.00, 40.00, 0.00, 100.00, '美育基础分60分，文艺活动、艺术竞赛可获得奖励分', 4),
('LABOR', '劳育', 10.00, 60.00, 40.00, 0.00, 100.00, '劳育基础分60分，劳动实践、志愿服务可获得奖励分', 5),
('DEVELOPMENT', '发展素质', 5.00, 60.00, 40.00, 0.00, 100.00, '发展素质基础分60分，创新创业、社会实践可获得奖励分', 6);

-- =====================================================
-- 2. 行为类型字典初始化
-- =====================================================

-- 考勤类行为
INSERT INTO `behavior_types` (`behavior_code`, `behavior_name`, `behavior_category`, `behavior_nature`, `default_affect_scope`, `description`, `sort_order`) VALUES
('ABSENCE_CLASS', '旷课', 'ATTENDANCE', 2, 1, '无故缺席课程', 1),
('LATE_CLASS', '上课迟到', 'ATTENDANCE', 2, 1, '上课迟到', 2),
('EARLY_LEAVE_CLASS', '上课早退', 'ATTENDANCE', 2, 1, '上课早退', 3),
('ABSENCE_ACTIVITY', '缺席活动', 'ATTENDANCE', 2, 1, '无故缺席集体活动', 4),
('LATE_ACTIVITY', '活动迟到', 'ATTENDANCE', 2, 1, '集体活动迟到', 5),
('ABSENCE_MEETING', '缺席会议', 'ATTENDANCE', 2, 1, '无故缺席会议', 6),
('FULL_ATTENDANCE', '全勤', 'ATTENDANCE', 1, 1, '学期内无迟到早退旷课', 7);

-- 纪律类行为
INSERT INTO `behavior_types` (`behavior_code`, `behavior_name`, `behavior_category`, `behavior_nature`, `default_affect_scope`, `description`, `sort_order`) VALUES
('DISCIPLINE_VIOLATION', '违反纪律', 'DISCIPLINE', 2, 1, '违反校规校纪', 10),
('CLASSROOM_DISORDER', '课堂纪律差', 'DISCIPLINE', 2, 1, '扰乱课堂秩序', 11),
('EXAM_CHEATING', '考试作弊', 'DISCIPLINE', 2, 1, '考试中作弊行为', 12),
('FIGHTING', '打架斗殴', 'DISCIPLINE', 2, 1, '参与打架斗殴', 13),
('SMOKING', '吸烟', 'DISCIPLINE', 2, 1, '在校内吸烟', 14),
('DRINKING', '饮酒', 'DISCIPLINE', 2, 1, '在校内饮酒', 15),
('GOOD_BEHAVIOR', '行为表现优秀', 'DISCIPLINE', 1, 1, '表现优秀受表扬', 16);

-- 卫生类行为
INSERT INTO `behavior_types` (`behavior_code`, `behavior_name`, `behavior_category`, `behavior_nature`, `default_affect_scope`, `description`, `sort_order`) VALUES
('DORM_HYGIENE_BAD', '宿舍卫生差', 'HYGIENE', 2, 2, '宿舍卫生检查不合格', 20),
('DORM_HYGIENE_GOOD', '宿舍卫生优秀', 'HYGIENE', 1, 2, '宿舍卫生检查优秀', 21),
('CLASSROOM_HYGIENE_BAD', '教室卫生差', 'HYGIENE', 2, 3, '教室值日不合格', 22),
('CLASSROOM_HYGIENE_GOOD', '教室卫生优秀', 'HYGIENE', 1, 3, '教室卫生检查优秀', 23),
('PERSONAL_HYGIENE_BAD', '个人卫生差', 'HYGIENE', 2, 1, '个人卫生不合格', 24);

-- 学习类行为
INSERT INTO `behavior_types` (`behavior_code`, `behavior_name`, `behavior_category`, `behavior_nature`, `default_affect_scope`, `description`, `sort_order`) VALUES
('HOMEWORK_MISSING', '作业缺交', 'STUDY', 2, 1, '未按时提交作业', 30),
('HOMEWORK_EXCELLENT', '作业优秀', 'STUDY', 1, 1, '作业被评为优秀', 31),
('ACADEMIC_PROGRESS', '学业进步', 'STUDY', 1, 1, '学业成绩明显进步', 32),
('ACADEMIC_DECLINE', '学业退步', 'STUDY', 2, 1, '学业成绩明显退步', 33),
('HELP_CLASSMATE', '帮助同学学习', 'STUDY', 1, 1, '主动帮助同学学习', 34);

-- 活动类行为
INSERT INTO `behavior_types` (`behavior_code`, `behavior_name`, `behavior_category`, `behavior_nature`, `default_affect_scope`, `description`, `sort_order`) VALUES
('ACTIVITY_ORGANIZE', '组织活动', 'ACTIVITY', 1, 1, '组织或参与组织活动', 40),
('ACTIVITY_PARTICIPATE', '参与活动', 'ACTIVITY', 1, 1, '积极参与集体活动', 41),
('VOLUNTEER_SERVICE', '志愿服务', 'ACTIVITY', 1, 1, '参与志愿服务活动', 42),
('SOCIAL_PRACTICE', '社会实践', 'ACTIVITY', 1, 1, '参与社会实践活动', 43),
('REFUSE_ACTIVITY', '拒绝参与活动', 'ACTIVITY', 2, 1, '无故拒绝参与集体活动', 44);

-- 荣誉类行为
INSERT INTO `behavior_types` (`behavior_code`, `behavior_name`, `behavior_category`, `behavior_nature`, `default_affect_scope`, `description`, `sort_order`) VALUES
('HONOR_NATIONAL', '国家级荣誉', 'HONOR', 1, 1, '获得国家级荣誉或奖项', 50),
('HONOR_PROVINCIAL', '省级荣誉', 'HONOR', 1, 1, '获得省级荣誉或奖项', 51),
('HONOR_CITY', '市级荣誉', 'HONOR', 1, 1, '获得市级荣誉或奖项', 52),
('HONOR_SCHOOL', '校级荣誉', 'HONOR', 1, 1, '获得校级荣誉或奖项', 53),
('HONOR_DEPARTMENT', '系级荣誉', 'HONOR', 1, 1, '获得系级荣誉或奖项', 54);

-- =====================================================
-- 3. 行为-综测映射初始化
-- =====================================================

-- 考勤类行为映射
INSERT INTO `behavior_evaluation_effects` (`behavior_type_id`, `evaluation_dimension`, `score_type`, `base_score`, `min_score`, `trigger_condition`) VALUES
((SELECT id FROM behavior_types WHERE behavior_code = 'ABSENCE_CLASS'), 'MORAL', 2, -2.00, -20.00, '{"description": "每次旷课扣2分，累计上限20分"}'),
((SELECT id FROM behavior_types WHERE behavior_code = 'LATE_CLASS'), 'MORAL', 2, -0.50, -10.00, '{"description": "每次迟到扣0.5分，累计上限10分"}'),
((SELECT id FROM behavior_types WHERE behavior_code = 'EARLY_LEAVE_CLASS'), 'MORAL', 2, -0.50, -10.00, '{"description": "每次早退扣0.5分，累计上限10分"}'),
((SELECT id FROM behavior_types WHERE behavior_code = 'ABSENCE_ACTIVITY'), 'MORAL', 2, -1.00, -10.00, '{"description": "每次缺席活动扣1分，累计上限10分"}'),
((SELECT id FROM behavior_types WHERE behavior_code = 'FULL_ATTENDANCE'), 'MORAL', 1, 5.00, NULL, '{"description": "学期全勤加5分"}');

-- 纪律类行为映射
INSERT INTO `behavior_evaluation_effects` (`behavior_type_id`, `evaluation_dimension`, `score_type`, `base_score`, `min_score`, `trigger_condition`) VALUES
((SELECT id FROM behavior_types WHERE behavior_code = 'DISCIPLINE_VIOLATION'), 'MORAL', 2, -3.00, -30.00, '{"description": "每次违纪扣3分"}'),
((SELECT id FROM behavior_types WHERE behavior_code = 'CLASSROOM_DISORDER'), 'MORAL', 2, -2.00, -10.00, '{"description": "每次扰乱课堂扣2分"}'),
((SELECT id FROM behavior_types WHERE behavior_code = 'EXAM_CHEATING'), 'MORAL', 1, -20.00, NULL, '{"description": "考试作弊扣20分"}'),
((SELECT id FROM behavior_types WHERE behavior_code = 'EXAM_CHEATING'), 'INTELLECTUAL', 1, -10.00, NULL, '{"description": "考试作弊智育扣10分"}'),
((SELECT id FROM behavior_types WHERE behavior_code = 'GOOD_BEHAVIOR'), 'MORAL', 1, 2.00, NULL, '{"description": "行为优秀加2分"}');

-- 卫生类行为映射
INSERT INTO `behavior_evaluation_effects` (`behavior_type_id`, `evaluation_dimension`, `score_type`, `base_score`, `min_score`, `max_score`, `trigger_condition`) VALUES
((SELECT id FROM behavior_types WHERE behavior_code = 'DORM_HYGIENE_BAD'), 'LABOR', 2, -1.00, -15.00, NULL, '{"description": "宿舍卫生差每次扣1分"}'),
((SELECT id FROM behavior_types WHERE behavior_code = 'DORM_HYGIENE_GOOD'), 'LABOR', 2, 1.00, NULL, 10.00, '{"description": "宿舍卫生优秀每次加1分"}'),
((SELECT id FROM behavior_types WHERE behavior_code = 'CLASSROOM_HYGIENE_BAD'), 'LABOR', 2, -1.00, -10.00, NULL, '{"description": "教室卫生差每次扣1分"}'),
((SELECT id FROM behavior_types WHERE behavior_code = 'CLASSROOM_HYGIENE_GOOD'), 'LABOR', 2, 1.00, NULL, 10.00, '{"description": "教室卫生优秀每次加1分"}');

-- 活动类行为映射
INSERT INTO `behavior_evaluation_effects` (`behavior_type_id`, `evaluation_dimension`, `score_type`, `base_score`, `max_score`, `trigger_condition`) VALUES
((SELECT id FROM behavior_types WHERE behavior_code = 'ACTIVITY_ORGANIZE'), 'DEVELOPMENT', 1, 3.00, 15.00, '{"description": "组织活动每次加3分"}'),
((SELECT id FROM behavior_types WHERE behavior_code = 'ACTIVITY_PARTICIPATE'), 'MORAL', 1, 1.00, 10.00, '{"description": "参与活动每次加1分"}'),
((SELECT id FROM behavior_types WHERE behavior_code = 'VOLUNTEER_SERVICE'), 'LABOR', 2, 2.00, 20.00, '{"description": "志愿服务每次加2分"}'),
((SELECT id FROM behavior_types WHERE behavior_code = 'SOCIAL_PRACTICE'), 'DEVELOPMENT', 2, 2.00, 15.00, '{"description": "社会实践每次加2分"}');

-- =====================================================
-- 4. 荣誉类型初始化
-- =====================================================
INSERT INTO `honor_types` (`type_code`, `type_name`, `category`, `evaluation_dimension`, `description`, `required_attachments`, `sort_order`) VALUES
-- 竞赛类
('ACADEMIC_COMPETITION', '学科竞赛', 'COMPETITION', 'INTELLECTUAL', '各类学科竞赛获奖', 1, 1),
('SPORTS_COMPETITION', '体育竞赛', 'COMPETITION', 'PHYSICAL', '各类体育竞赛获奖', 1, 2),
('ART_COMPETITION', '文艺竞赛', 'COMPETITION', 'AESTHETIC', '各类文艺竞赛获奖', 1, 3),
('INNOVATION_COMPETITION', '创新创业竞赛', 'COMPETITION', 'DEVELOPMENT', '创新创业类竞赛获奖', 1, 4),
('SKILL_COMPETITION', '技能竞赛', 'COMPETITION', 'INTELLECTUAL', '职业技能竞赛获奖', 1, 5),
-- 证书类
('PROFESSIONAL_CERT', '职业资格证书', 'CERTIFICATE', 'INTELLECTUAL', '获得职业资格证书', 1, 10),
('LANGUAGE_CERT', '语言等级证书', 'CERTIFICATE', 'INTELLECTUAL', '获得语言等级证书', 1, 11),
('COMPUTER_CERT', '计算机等级证书', 'CERTIFICATE', 'INTELLECTUAL', '获得计算机等级证书', 1, 12),
-- 称号类
('OUTSTANDING_STUDENT', '优秀学生', 'TITLE', 'MORAL', '获得优秀学生称号', 1, 20),
('OUTSTANDING_CADRE', '优秀学生干部', 'TITLE', 'MORAL', '获得优秀学生干部称号', 1, 21),
('OUTSTANDING_LEAGUE', '优秀团员', 'TITLE', 'MORAL', '获得优秀团员称号', 1, 22),
('SCHOLARSHIP', '奖学金', 'TITLE', 'INTELLECTUAL', '获得各类奖学金', 1, 23),
-- 活动类
('VOLUNTEER_AWARD', '志愿服务表彰', 'ACTIVITY', 'LABOR', '志愿服务获表彰', 1, 30),
('SOCIAL_PRACTICE_AWARD', '社会实践表彰', 'ACTIVITY', 'DEVELOPMENT', '社会实践获表彰', 1, 31),
-- 学术成果类
('PAPER_PUBLICATION', '论文发表', 'PUBLICATION', 'INTELLECTUAL', '公开发表学术论文', 1, 40),
('PATENT', '专利', 'PUBLICATION', 'DEVELOPMENT', '获得专利授权', 1, 41),
('SOFTWARE_COPYRIGHT', '软件著作权', 'PUBLICATION', 'DEVELOPMENT', '获得软件著作权', 1, 42);

-- =====================================================
-- 5. 荣誉等级配置初始化
-- =====================================================

-- 学科竞赛等级配置
INSERT INTO `honor_level_configs` (`honor_type_id`, `level_code`, `level_name`, `rank_code`, `rank_name`, `score`, `priority`, `sort_order`) VALUES
((SELECT id FROM honor_types WHERE type_code = 'ACADEMIC_COMPETITION'), 'NATIONAL', '国家级', 'FIRST', '一等奖', 20.00, 100, 1),
((SELECT id FROM honor_types WHERE type_code = 'ACADEMIC_COMPETITION'), 'NATIONAL', '国家级', 'SECOND', '二等奖', 15.00, 99, 2),
((SELECT id FROM honor_types WHERE type_code = 'ACADEMIC_COMPETITION'), 'NATIONAL', '国家级', 'THIRD', '三等奖', 10.00, 98, 3),
((SELECT id FROM honor_types WHERE type_code = 'ACADEMIC_COMPETITION'), 'PROVINCIAL', '省级', 'FIRST', '一等奖', 10.00, 90, 4),
((SELECT id FROM honor_types WHERE type_code = 'ACADEMIC_COMPETITION'), 'PROVINCIAL', '省级', 'SECOND', '二等奖', 8.00, 89, 5),
((SELECT id FROM honor_types WHERE type_code = 'ACADEMIC_COMPETITION'), 'PROVINCIAL', '省级', 'THIRD', '三等奖', 5.00, 88, 6),
((SELECT id FROM honor_types WHERE type_code = 'ACADEMIC_COMPETITION'), 'CITY', '市级', 'FIRST', '一等奖', 5.00, 80, 7),
((SELECT id FROM honor_types WHERE type_code = 'ACADEMIC_COMPETITION'), 'CITY', '市级', 'SECOND', '二等奖', 3.00, 79, 8),
((SELECT id FROM honor_types WHERE type_code = 'ACADEMIC_COMPETITION'), 'CITY', '市级', 'THIRD', '三等奖', 2.00, 78, 9),
((SELECT id FROM honor_types WHERE type_code = 'ACADEMIC_COMPETITION'), 'SCHOOL', '校级', 'FIRST', '一等奖', 3.00, 70, 10),
((SELECT id FROM honor_types WHERE type_code = 'ACADEMIC_COMPETITION'), 'SCHOOL', '校级', 'SECOND', '二等奖', 2.00, 69, 11),
((SELECT id FROM honor_types WHERE type_code = 'ACADEMIC_COMPETITION'), 'SCHOOL', '校级', 'THIRD', '三等奖', 1.00, 68, 12);

-- 体育竞赛等级配置
INSERT INTO `honor_level_configs` (`honor_type_id`, `level_code`, `level_name`, `rank_code`, `rank_name`, `score`, `priority`, `sort_order`) VALUES
((SELECT id FROM honor_types WHERE type_code = 'SPORTS_COMPETITION'), 'NATIONAL', '国家级', 'FIRST', '一等奖/金牌', 20.00, 100, 1),
((SELECT id FROM honor_types WHERE type_code = 'SPORTS_COMPETITION'), 'NATIONAL', '国家级', 'SECOND', '二等奖/银牌', 15.00, 99, 2),
((SELECT id FROM honor_types WHERE type_code = 'SPORTS_COMPETITION'), 'NATIONAL', '国家级', 'THIRD', '三等奖/铜牌', 10.00, 98, 3),
((SELECT id FROM honor_types WHERE type_code = 'SPORTS_COMPETITION'), 'PROVINCIAL', '省级', 'FIRST', '一等奖/金牌', 10.00, 90, 4),
((SELECT id FROM honor_types WHERE type_code = 'SPORTS_COMPETITION'), 'PROVINCIAL', '省级', 'SECOND', '二等奖/银牌', 8.00, 89, 5),
((SELECT id FROM honor_types WHERE type_code = 'SPORTS_COMPETITION'), 'PROVINCIAL', '省级', 'THIRD', '三等奖/铜牌', 5.00, 88, 6),
((SELECT id FROM honor_types WHERE type_code = 'SPORTS_COMPETITION'), 'SCHOOL', '校级', 'FIRST', '一等奖/金牌', 3.00, 70, 7),
((SELECT id FROM honor_types WHERE type_code = 'SPORTS_COMPETITION'), 'SCHOOL', '校级', 'SECOND', '二等奖/银牌', 2.00, 69, 8),
((SELECT id FROM honor_types WHERE type_code = 'SPORTS_COMPETITION'), 'SCHOOL', '校级', 'THIRD', '三等奖/铜牌', 1.00, 68, 9);

-- 优秀学生称号等级配置
INSERT INTO `honor_level_configs` (`honor_type_id`, `level_code`, `level_name`, `rank_code`, `rank_name`, `score`, `priority`, `sort_order`) VALUES
((SELECT id FROM honor_types WHERE type_code = 'OUTSTANDING_STUDENT'), 'NATIONAL', '国家级', NULL, NULL, 15.00, 100, 1),
((SELECT id FROM honor_types WHERE type_code = 'OUTSTANDING_STUDENT'), 'PROVINCIAL', '省级', NULL, NULL, 10.00, 90, 2),
((SELECT id FROM honor_types WHERE type_code = 'OUTSTANDING_STUDENT'), 'CITY', '市级', NULL, NULL, 5.00, 80, 3),
((SELECT id FROM honor_types WHERE type_code = 'OUTSTANDING_STUDENT'), 'SCHOOL', '校级', NULL, NULL, 3.00, 70, 4),
((SELECT id FROM honor_types WHERE type_code = 'OUTSTANDING_STUDENT'), 'DEPARTMENT', '系级', NULL, NULL, 1.00, 60, 5);

-- 奖学金等级配置
INSERT INTO `honor_level_configs` (`honor_type_id`, `level_code`, `level_name`, `rank_code`, `rank_name`, `score`, `priority`, `sort_order`) VALUES
((SELECT id FROM honor_types WHERE type_code = 'SCHOLARSHIP'), 'NATIONAL', '国家级', 'FIRST', '国家奖学金', 15.00, 100, 1),
((SELECT id FROM honor_types WHERE type_code = 'SCHOLARSHIP'), 'NATIONAL', '国家级', 'SECOND', '国家励志奖学金', 10.00, 99, 2),
((SELECT id FROM honor_types WHERE type_code = 'SCHOLARSHIP'), 'SCHOOL', '校级', 'FIRST', '一等奖学金', 5.00, 70, 3),
((SELECT id FROM honor_types WHERE type_code = 'SCHOLARSHIP'), 'SCHOOL', '校级', 'SECOND', '二等奖学金', 3.00, 69, 4),
((SELECT id FROM honor_types WHERE type_code = 'SCHOLARSHIP'), 'SCHOOL', '校级', 'THIRD', '三等奖学金', 2.00, 68, 5);

-- 证书类等级配置
INSERT INTO `honor_level_configs` (`honor_type_id`, `level_code`, `level_name`, `rank_code`, `rank_name`, `score`, `priority`, `sort_order`) VALUES
((SELECT id FROM honor_types WHERE type_code = 'LANGUAGE_CERT'), 'NATIONAL', '国家级', 'CET6', '英语六级', 3.00, 80, 1),
((SELECT id FROM honor_types WHERE type_code = 'LANGUAGE_CERT'), 'NATIONAL', '国家级', 'CET4', '英语四级', 2.00, 70, 2),
((SELECT id FROM honor_types WHERE type_code = 'COMPUTER_CERT'), 'NATIONAL', '国家级', 'NCRE2', '计算机二级', 2.00, 70, 1),
((SELECT id FROM honor_types WHERE type_code = 'COMPUTER_CERT'), 'NATIONAL', '国家级', 'NCRE3', '计算机三级', 3.00, 80, 2),
((SELECT id FROM honor_types WHERE type_code = 'COMPUTER_CERT'), 'NATIONAL', '国家级', 'NCRE4', '计算机四级', 5.00, 90, 3);

-- =====================================================
-- 6. 学期初始化
-- =====================================================
INSERT INTO `semesters` (`semester_code`, `semester_name`, `academic_year`, `semester_type`, `start_date`, `end_date`, `is_current`) VALUES
('2024-2025-1', '2024-2025学年第一学期', '2024-2025', 1, '2024-09-01', '2025-01-15', 0),
('2024-2025-2', '2024-2025学年第二学期', '2024-2025', 2, '2025-02-17', '2025-07-10', 1);

-- =====================================================
-- 7. 权限数据初始化
-- =====================================================

-- 获取父权限ID的函数式查询
SET @parent_system_id = (SELECT id FROM permissions WHERE permission_code = 'system' LIMIT 1);

-- 插入综测模块权限
INSERT INTO `permissions` (`permission_code`, `permission_name`, `permission_type`, `parent_id`, `path`, `icon`, `sort_order`, `status`, `created_at`) VALUES
-- 一级菜单
('evaluation', '综合测评', 1, 0, '/evaluation', 'Trophy', 50, 1, NOW()),

-- 二级菜单 - 配置管理
('evaluation:config', '测评配置', 1, (SELECT id FROM (SELECT id FROM permissions WHERE permission_code = 'evaluation') t), '/evaluation/config', 'Settings', 1, 1, NOW()),
('evaluation:config:behavior', '行为类型管理', 1, (SELECT id FROM (SELECT id FROM permissions WHERE permission_code = 'evaluation:config') t), '/evaluation/config/behavior', NULL, 1, 1, NOW()),
('evaluation:config:dimension', '维度配置', 1, (SELECT id FROM (SELECT id FROM permissions WHERE permission_code = 'evaluation:config') t), '/evaluation/config/dimension', NULL, 2, 1, NOW()),
('evaluation:config:honor', '荣誉类型配置', 1, (SELECT id FROM (SELECT id FROM permissions WHERE permission_code = 'evaluation:config') t), '/evaluation/config/honor', NULL, 3, 1, NOW()),

-- 二级菜单 - 课程成绩
('evaluation:academic', '课程成绩', 1, (SELECT id FROM (SELECT id FROM permissions WHERE permission_code = 'evaluation') t), '/evaluation/academic', 'BookOpen', 2, 1, NOW()),
('evaluation:academic:semester', '学期管理', 1, (SELECT id FROM (SELECT id FROM permissions WHERE permission_code = 'evaluation:academic') t), '/evaluation/academic/semester', NULL, 1, 1, NOW()),
('evaluation:academic:course', '课程管理', 1, (SELECT id FROM (SELECT id FROM permissions WHERE permission_code = 'evaluation:academic') t), '/evaluation/academic/course', NULL, 2, 1, NOW()),
('evaluation:academic:score', '成绩管理', 1, (SELECT id FROM (SELECT id FROM permissions WHERE permission_code = 'evaluation:academic') t), '/evaluation/academic/score', NULL, 3, 1, NOW()),

-- 二级菜单 - 荣誉管理
('evaluation:honor', '荣誉管理', 1, (SELECT id FROM (SELECT id FROM permissions WHERE permission_code = 'evaluation') t), '/evaluation/honor', 'Award', 3, 1, NOW()),
('evaluation:honor:apply', '荣誉申报', 1, (SELECT id FROM (SELECT id FROM permissions WHERE permission_code = 'evaluation:honor') t), '/evaluation/honor/apply', NULL, 1, 1, NOW()),
('evaluation:honor:review', '荣誉审核', 1, (SELECT id FROM (SELECT id FROM permissions WHERE permission_code = 'evaluation:honor') t), '/evaluation/honor/review', NULL, 2, 1, NOW()),

-- 二级菜单 - 处分管理
('evaluation:punishment', '处分管理', 1, (SELECT id FROM (SELECT id FROM permissions WHERE permission_code = 'evaluation') t), '/evaluation/punishment', 'AlertTriangle', 4, 1, NOW()),

-- 二级菜单 - 评定管理
('evaluation:period', '评定周期', 1, (SELECT id FROM (SELECT id FROM permissions WHERE permission_code = 'evaluation') t), '/evaluation/period', 'Calendar', 5, 1, NOW()),

-- 二级菜单 - 数据确认
('evaluation:data', '数据确认', 1, (SELECT id FROM (SELECT id FROM permissions WHERE permission_code = 'evaluation') t), '/evaluation/data', 'CheckSquare', 6, 1, NOW()),

-- 二级菜单 - 综测结果
('evaluation:result', '综测结果', 1, (SELECT id FROM (SELECT id FROM permissions WHERE permission_code = 'evaluation') t), '/evaluation/result', 'FileText', 7, 1, NOW()),
('evaluation:result:my', '我的综测', 1, (SELECT id FROM (SELECT id FROM permissions WHERE permission_code = 'evaluation:result') t), '/evaluation/result/my', NULL, 1, 1, NOW()),
('evaluation:result:class', '班级综测', 1, (SELECT id FROM (SELECT id FROM permissions WHERE permission_code = 'evaluation:result') t), '/evaluation/result/class', NULL, 2, 1, NOW()),
('evaluation:result:all', '全部综测', 1, (SELECT id FROM (SELECT id FROM permissions WHERE permission_code = 'evaluation:result') t), '/evaluation/result/all', NULL, 3, 1, NOW()),

-- 二级菜单 - 统计报表
('evaluation:statistics', '统计报表', 1, (SELECT id FROM (SELECT id FROM permissions WHERE permission_code = 'evaluation') t), '/evaluation/statistics', 'BarChart2', 8, 1, NOW()),

-- 二级菜单 - 综测申诉
('evaluation:appeal', '综测申诉', 1, (SELECT id FROM (SELECT id FROM permissions WHERE permission_code = 'evaluation') t), '/evaluation/appeal', 'MessageSquare', 9, 1, NOW());

-- 按钮权限
INSERT INTO `permissions` (`permission_code`, `permission_name`, `permission_type`, `parent_id`, `sort_order`, `status`, `created_at`) VALUES
-- 行为类型管理按钮
('evaluation:behavior:list', '查看行为类型', 2, (SELECT id FROM (SELECT id FROM permissions WHERE permission_code = 'evaluation:config:behavior') t), 1, 1, NOW()),
('evaluation:behavior:create', '创建行为类型', 2, (SELECT id FROM (SELECT id FROM permissions WHERE permission_code = 'evaluation:config:behavior') t), 2, 1, NOW()),
('evaluation:behavior:update', '编辑行为类型', 2, (SELECT id FROM (SELECT id FROM permissions WHERE permission_code = 'evaluation:config:behavior') t), 3, 1, NOW()),
('evaluation:behavior:delete', '删除行为类型', 2, (SELECT id FROM (SELECT id FROM permissions WHERE permission_code = 'evaluation:config:behavior') t), 4, 1, NOW()),

-- 课程管理按钮
('evaluation:course:list', '查看课程', 2, (SELECT id FROM (SELECT id FROM permissions WHERE permission_code = 'evaluation:academic:course') t), 1, 1, NOW()),
('evaluation:course:create', '创建课程', 2, (SELECT id FROM (SELECT id FROM permissions WHERE permission_code = 'evaluation:academic:course') t), 2, 1, NOW()),
('evaluation:course:update', '编辑课程', 2, (SELECT id FROM (SELECT id FROM permissions WHERE permission_code = 'evaluation:academic:course') t), 3, 1, NOW()),
('evaluation:course:delete', '删除课程', 2, (SELECT id FROM (SELECT id FROM permissions WHERE permission_code = 'evaluation:academic:course') t), 4, 1, NOW()),

-- 成绩管理按钮
('evaluation:score:list', '查看成绩', 2, (SELECT id FROM (SELECT id FROM permissions WHERE permission_code = 'evaluation:academic:score') t), 1, 1, NOW()),
('evaluation:score:input', '录入成绩', 2, (SELECT id FROM (SELECT id FROM permissions WHERE permission_code = 'evaluation:academic:score') t), 2, 1, NOW()),
('evaluation:score:import', '导入成绩', 2, (SELECT id FROM (SELECT id FROM permissions WHERE permission_code = 'evaluation:academic:score') t), 3, 1, NOW()),
('evaluation:score:export', '导出成绩', 2, (SELECT id FROM (SELECT id FROM permissions WHERE permission_code = 'evaluation:academic:score') t), 4, 1, NOW()),

-- 荣誉管理按钮
('evaluation:honor:apply:create', '提交申报', 2, (SELECT id FROM (SELECT id FROM permissions WHERE permission_code = 'evaluation:honor:apply') t), 1, 1, NOW()),
('evaluation:honor:review:class', '班级审核', 2, (SELECT id FROM (SELECT id FROM permissions WHERE permission_code = 'evaluation:honor:review') t), 1, 1, NOW()),
('evaluation:honor:review:dept', '系部审核', 2, (SELECT id FROM (SELECT id FROM permissions WHERE permission_code = 'evaluation:honor:review') t), 2, 1, NOW()),

-- 综测结果按钮
('evaluation:result:view', '查看结果', 2, (SELECT id FROM (SELECT id FROM permissions WHERE permission_code = 'evaluation:result') t), 1, 1, NOW()),
('evaluation:result:calculate', '计算综测', 2, (SELECT id FROM (SELECT id FROM permissions WHERE permission_code = 'evaluation:result') t), 2, 1, NOW()),
('evaluation:result:adjust', '调整结果', 2, (SELECT id FROM (SELECT id FROM permissions WHERE permission_code = 'evaluation:result') t), 3, 1, NOW()),
('evaluation:result:export', '导出结果', 2, (SELECT id FROM (SELECT id FROM permissions WHERE permission_code = 'evaluation:result') t), 4, 1, NOW());

-- =====================================================
-- 完成
-- =====================================================
