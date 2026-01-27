-- 校历事件表
CREATE TABLE IF NOT EXISTS `academic_event` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `year_id` BIGINT NOT NULL COMMENT '学年ID',
    `semester_id` BIGINT DEFAULT NULL COMMENT '学期ID（可选）',
    `event_name` VARCHAR(100) NOT NULL COMMENT '事件名称',
    `event_type` TINYINT NOT NULL DEFAULT 5 COMMENT '事件类型：1-开学, 2-放假, 3-考试, 4-活动, 5-其他',
    `start_date` DATE NOT NULL COMMENT '开始日期',
    `end_date` DATE DEFAULT NULL COMMENT '结束日期',
    `all_day` TINYINT(1) DEFAULT 1 COMMENT '是否全天事件',
    `description` VARCHAR(500) DEFAULT NULL COMMENT '事件描述',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT(1) DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_year_id` (`year_id`),
    KEY `idx_semester_id` (`semester_id`),
    KEY `idx_start_date` (`start_date`),
    KEY `idx_event_type` (`event_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='校历事件表';

-- 插入初始示例数据
INSERT INTO `academic_event` (`year_id`, `semester_id`, `event_name`, `event_type`, `start_date`, `end_date`, `description`) VALUES
(1, 2, '开学典礼', 1, '2026-02-17', '2026-02-17', '新学期开学典礼'),
(1, 2, '期中考试', 3, '2026-04-13', '2026-04-17', '期中考试周'),
(1, 2, '五一假期', 2, '2026-05-01', '2026-05-05', '劳动节假期'),
(1, 2, '期末考试', 3, '2026-06-22', '2026-06-28', '期末考试周'),
(1, 2, '暑假开始', 2, '2026-07-01', '2026-08-31', '暑假');
