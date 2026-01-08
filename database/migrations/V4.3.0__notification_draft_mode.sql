-- =============================================
-- V4.3.0 通报草稿模式
-- 创建日期: 2024-12-20
-- 说明: 修改通报记录表，支持草稿编辑模式
-- =============================================

-- 1. 添加发布状态字段
ALTER TABLE notification_records
    ADD COLUMN publish_status TINYINT DEFAULT 0 COMMENT '发布状态: 0-草稿, 1-已发布' AFTER status;

-- 2. 添加更新时间字段
ALTER TABLE notification_records
    ADD COLUMN updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间' AFTER created_at;

-- 3. 修改 file_format 为可空（草稿时不需要指定格式）
ALTER TABLE notification_records
    MODIFY COLUMN file_format VARCHAR(20) NULL COMMENT '文件格式: PDF/WORD/EXCEL（下载时指定）';

-- 4. 修改 status 字段说明
-- status: 0-草稿编辑中, 1-已完成, 2-生成失败
-- 保持现有值不变，但含义更新

-- 5. 添加发布状态索引
ALTER TABLE notification_records
    ADD INDEX idx_publish_status (publish_status);
