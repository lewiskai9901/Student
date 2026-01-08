-- ====================================================================
-- 创建优化视图替代冗余字段
--
-- 目的: 减少数据冗余,通过视图动态关联查询相关数据
-- 作者: Claude Code
-- 日期: 2025-11-23
-- 优先级: 🟡 中 - 数据规范化
-- ====================================================================

-- 1. 检查记录班级统计增强视图
-- 目的: 替代 check_record_class_stats 表中的冗余字段
-- 说明: 动态关联班级、年级、教师等信息,避免数据重复存储

DROP VIEW IF EXISTS v_check_record_class_stats_enhanced;

CREATE VIEW v_check_record_class_stats_enhanced AS
SELECT
    -- 统计表原始字段
    s.id,
    s.record_id,
    s.class_id,

    -- 从classes表动态获取班级信息 (替代冗余字段: class_name)
    c.class_name,
    c.class_code,

    -- 从classes表动态获取年级信息 (替代冗余字段: grade_id, grade_name, enrollment_year)
    g.id AS grade_id,
    g.grade_name,
    g.enrollment_year,
    g.education_system,
    g.skill_level,

    -- 从users表动态获取班主任信息 (替代冗余字段: teacher_id, teacher_name)
    c.teacher_id,
    u.real_name AS teacher_name,

    -- 专业方向信息
    s.major_direction_id,

    -- 分数信息 (保留原表字段)
    s.total_score,
    s.hygiene_score,
    s.discipline_score,
    s.attendance_score,
    s.other_score,

    -- 排名信息 (保留原表字段)
    s.ranking,
    s.grade_ranking,
    s.score_level,

    -- 动态计算扣分明细统计 (替代冗余字段: total_items, appeal_count)
    (SELECT COUNT(*)
     FROM check_record_items_v3 i
     WHERE i.class_stat_id = s.id) AS total_items,

    (SELECT COUNT(*)
     FROM check_record_items_v3 i
     WHERE i.class_stat_id = s.id
     AND i.appeal_status > 0) AS appeal_count,

    (SELECT COUNT(*)
     FROM check_record_items_v3 i
     WHERE i.class_stat_id = s.id
     AND i.appeal_status = 2) AS appeal_passed,

    (SELECT COUNT(*)
     FROM check_record_items_v3 i
     WHERE i.class_stat_id = s.id
     AND i.appeal_status = 1) AS appeal_pending,

    -- 对比数据 (保留原表字段)
    s.vs_avg_score,
    s.vs_last_score,

    -- 审计字段
    s.created_at,
    s.updated_at

FROM check_record_class_stats s
LEFT JOIN classes c ON s.class_id = c.id
LEFT JOIN grades g ON c.grade_id = g.id
LEFT JOIN users u ON c.teacher_id = u.id;


-- 2. 检查记录完整信息视图
-- 目的: 提供检查记录的完整信息,包括检查人、模板、配置等
-- 说明: 便于查询时一次性获取所有关联信息

DROP VIEW IF EXISTS v_check_records_v3_full;

CREATE VIEW v_check_records_v3_full AS
SELECT
    -- 检查记录基本信息
    r.id,
    r.record_code,
    r.check_name,
    r.check_date,
    r.check_type,

    -- 检查人信息 (动态关联)
    r.checker_id,
    u1.real_name AS checker_name,
    u1.username AS checker_username,

    -- 检查模板信息 (动态关联)
    r.template_id,
    t.template_name,
    t.template_code,

    -- 加权配置信息 (动态关联)
    r.weight_config_id,
    w.config_name AS weight_config_name,
    w.config_code AS weight_config_code,
    w.weight_mode,

    -- 申诉配置信息 (动态关联)
    r.appeal_config_id,
    ac.config_name AS appeal_config_name,

    -- 统计数据 (保留原表)
    r.total_score,
    r.avg_score,
    r.max_score,
    r.min_score,
    r.class_count,

    -- 发布信息
    r.publish_status,
    r.publish_time,
    r.publisher_id,
    u2.real_name AS publisher_name,

    -- 状态信息
    r.status,
    r.deleted,
    r.remark,

    -- 审计字段
    r.created_at,
    r.updated_at,
    r.created_by,
    u3.real_name AS creator_name

FROM check_records_v3 r
LEFT JOIN users u1 ON r.checker_id = u1.id
LEFT JOIN users u2 ON r.publisher_id = u2.id
LEFT JOIN users u3 ON r.created_by = u3.id
LEFT JOIN check_templates t ON r.template_id = t.id
LEFT JOIN class_weight_configs w ON r.weight_config_id = w.id
LEFT JOIN appeal_configs ac ON r.appeal_config_id = ac.id;


-- 3. 扣分明细增强视图
-- 目的: 关联类别、申诉等信息
-- 说明: 便于查询扣分明细的完整信息

DROP VIEW IF EXISTS v_check_record_items_enhanced;

CREATE VIEW v_check_record_items_enhanced AS
SELECT
    -- 扣分明细基本信息
    i.id,
    i.record_id,
    i.class_stat_id,

    -- 班级信息 (从统计表获取)
    s.class_id,
    s.class_name,

    -- 分类信息 (动态关联)
    i.category_id,
    i.category_name,

    -- 扣分项信息
    i.item_id,
    i.item_name,
    i.deduct_mode,
    i.deduct_score,
    i.person_count,

    -- 关联信息
    i.link_type,
    i.link_id,
    i.link_no,

    -- 证据和备注
    i.photo_urls,
    i.remark,

    -- 申诉信息 (动态关联)
    i.appeal_status,
    i.appeal_id,
    CASE i.appeal_status
        WHEN 0 THEN '未申诉'
        WHEN 1 THEN '待处理'
        WHEN 2 THEN '已通过'
        WHEN 3 THEN '已驳回'
        ELSE '未知'
    END AS appeal_status_text,

    -- 审计字段
    i.created_at

FROM check_record_items_v3 i
LEFT JOIN check_record_class_stats s ON i.class_stat_id = s.id;


-- 4. 班级检查统计汇总视图
-- 目的: 提供班级维度的检查统计数据
-- 说明: 用于班级检查历史查询和分析

DROP VIEW IF EXISTS v_class_check_summary;

CREATE VIEW v_class_check_summary AS
SELECT
    c.id AS class_id,
    c.class_name,
    c.class_code,

    -- 年级信息
    g.grade_name,
    g.enrollment_year,

    -- 班主任信息
    c.teacher_id,
    u.real_name AS teacher_name,

    -- 检查次数统计
    COUNT(DISTINCT s.record_id) AS total_check_count,

    -- 最近一次检查
    MAX(r.check_date) AS last_check_date,

    -- 平均分数
    AVG(s.total_score) AS avg_total_score,

    -- 最好排名
    MIN(s.ranking) AS best_ranking,

    -- 总扣分项数
    SUM((SELECT COUNT(*) FROM check_record_items_v3 i WHERE i.class_stat_id = s.id)) AS total_deduct_items,

    -- 总申诉数
    SUM((SELECT COUNT(*) FROM check_record_items_v3 i WHERE i.class_stat_id = s.id AND i.appeal_status > 0)) AS total_appeals

FROM classes c
LEFT JOIN grades g ON c.grade_id = g.id
LEFT JOIN users u ON c.teacher_id = u.id
LEFT JOIN check_record_class_stats s ON c.id = s.class_id
LEFT JOIN check_records_v3 r ON s.record_id = r.id AND r.deleted = 0
WHERE c.deleted = 0
GROUP BY c.id, c.class_name, c.class_code, g.grade_name, g.enrollment_year, c.teacher_id, u.real_name;


-- 5. 加权配置使用统计视图
-- 目的: 统计各个加权配置的使用情况
-- 说明: 用于分析配置使用频率和效果

DROP VIEW IF EXISTS v_weight_config_usage;

CREATE VIEW v_weight_config_usage AS
SELECT
    w.id AS config_id,
    w.config_name,
    w.config_code,
    w.weight_mode,
    w.is_default,
    w.status,

    -- 使用次数
    COUNT(r.id) AS usage_count,

    -- 最近使用时间
    MAX(r.check_date) AS last_used_date,

    -- 首次使用时间
    MIN(r.check_date) AS first_used_date,

    -- 平均分数影响
    AVG(r.avg_score) AS avg_result_score

FROM class_weight_configs w
LEFT JOIN check_records_v3 r ON w.id = r.weight_config_id AND r.deleted = 0
WHERE w.deleted = 0
GROUP BY w.id, w.config_name, w.config_code, w.weight_mode, w.is_default, w.status;


-- ====================================================================
-- 视图使用示例:
-- ====================================================================

-- 查询检查记录的完整信息(包括检查人、模板、配置)
-- SELECT * FROM v_check_records_v3_full
-- WHERE check_date >= '2024-01-01'
-- ORDER BY check_date DESC;

-- 查询班级统计(包括班级、年级、教师信息)
-- SELECT * FROM v_check_record_class_stats_enhanced
-- WHERE record_id = 1
-- ORDER BY ranking;

-- 查询扣分明细(包括班级、分类、申诉信息)
-- SELECT * FROM v_check_record_items_enhanced
-- WHERE class_stat_id = 1;

-- 查询班级检查历史统计
-- SELECT * FROM v_class_check_summary
-- WHERE grade_name LIKE '%2023%'
-- ORDER BY avg_total_score DESC;

-- 查询加权配置使用情况
-- SELECT * FROM v_weight_config_usage
-- ORDER BY usage_count DESC;


-- ====================================================================
-- 执行说明:
-- 1. 视图创建后不占用额外存储空间
-- 2. 视图数据是动态查询生成的,始终保持最新
-- 3. 使用DROP VIEW IF EXISTS确保可重复执行
-- 4. 视图可以像普通表一样进行SELECT查询
-- 5. 某些复杂视图可能影响查询性能,建议在生产环境测试
-- ====================================================================

-- 验证视图创建
SELECT
    TABLE_NAME as view_name,
    TABLE_COMMENT as comment
FROM INFORMATION_SCHEMA.TABLES
WHERE TABLE_SCHEMA = 'student_management'
  AND TABLE_TYPE = 'VIEW'
  AND TABLE_NAME LIKE 'v_%check%'
ORDER BY TABLE_NAME;

-- ====================================================================
-- 性能优化建议:
-- 1. 视图查询会实时关联多张表,对于高频查询可能影响性能
-- 2. 可以考虑创建物化视图(MySQL 8.0不原生支持,需要手动刷新表)
-- 3. 对于报表查询,建议异步生成汇总表
-- 4. 生产环境使用前需要进行性能测试
-- ====================================================================

-- ====================================================================
-- 回滚脚本 (如果需要)
-- DROP VIEW IF EXISTS v_check_record_class_stats_enhanced;
-- DROP VIEW IF EXISTS v_check_records_v3_full;
-- DROP VIEW IF EXISTS v_check_record_items_enhanced;
-- DROP VIEW IF EXISTS v_class_check_summary;
-- DROP VIEW IF EXISTS v_weight_config_usage;
-- ====================================================================
