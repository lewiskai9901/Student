-- =============================================
-- V4.2.0 通报中心相关表
-- 创建日期: 2024-12-20
-- 说明: 创建通报记录表，更新导出模板表
-- =============================================

-- 1. 添加自定义变量字段到导出模板表（如果不存在）
-- 注意: 如果字段已存在会报错，可忽略
SET @exist_col = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'check_export_templates'
    AND COLUMN_NAME = 'custom_variables');

SET @sql = IF(@exist_col = 0,
    'ALTER TABLE check_export_templates ADD COLUMN custom_variables TEXT COMMENT ''自定义变量配置（JSON格式）''',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. 创建通报记录表
CREATE TABLE IF NOT EXISTS notification_records (
    id BIGINT PRIMARY KEY COMMENT '主键ID',

    -- 关联信息
    plan_id BIGINT NOT NULL COMMENT '检查计划ID',
    template_id BIGINT NOT NULL COMMENT '使用的模板ID',

    -- 通报类型
    notification_type VARCHAR(30) NOT NULL COMMENT '通报类型: SINGLE_CHECK-单次检查, MULTI_ROUND-多轮次, MULTI_CHECK-多检查汇总, RATING-评级通报',

    -- 数据范围
    daily_check_ids TEXT COMMENT '关联的日常检查ID列表（JSON数组）',
    check_record_ids TEXT COMMENT '关联的检查记录ID列表（JSON数组）',
    check_rounds TEXT COMMENT '选择的轮次（JSON数组）',
    rating_result_ids TEXT COMMENT '评级结果ID列表（JSON数组，评级通报时使用）',

    -- 通报内容快照
    title VARCHAR(200) COMMENT '通报标题',
    content_snapshot MEDIUMTEXT COMMENT '通报内容快照（渲染后的HTML）',
    variable_values TEXT COMMENT '使用的变量值（JSON格式）',

    -- 统计信息
    total_count INT DEFAULT 0 COMMENT '涉及人数/班级数',
    total_classes INT DEFAULT 0 COMMENT '涉及班级数',
    total_deduction_count INT DEFAULT 0 COMMENT '扣分条目数',

    -- 文件信息
    file_format VARCHAR(20) NOT NULL COMMENT '文件格式: PDF/WORD/EXCEL',
    file_name VARCHAR(200) COMMENT '文件名',
    file_path VARCHAR(500) COMMENT '文件存储路径',
    file_size BIGINT COMMENT '文件大小（字节）',

    -- 状态
    status TINYINT DEFAULT 0 COMMENT '状态: 0-生成中, 1-已完成, 2-生成失败',
    error_message VARCHAR(500) COMMENT '错误信息',

    -- 时间戳
    generated_at DATETIME COMMENT '生成完成时间',
    created_by BIGINT COMMENT '创建人ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    -- 索引
    INDEX idx_plan_id (plan_id),
    INDEX idx_template_id (template_id),
    INDEX idx_notification_type (notification_type),
    INDEX idx_created_at (created_at),
    INDEX idx_created_by (created_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通报记录表';
