-- 专业表
CREATE TABLE IF NOT EXISTS `majors` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `major_name` VARCHAR(100) NOT NULL COMMENT '专业名称',
    `major_code` VARCHAR(50) DEFAULT NULL COMMENT '专业代码',
    `education_system` VARCHAR(10) NOT NULL COMMENT '学制(3+2, 3+3, 3, 4)',
    `training_level` TINYINT(1) NOT NULL COMMENT '培养层次(1-中级工, 2-高级工, 3-预备技师)',
    `department_id` BIGINT(20) DEFAULT NULL COMMENT '所属院系ID',
    `description` TEXT COMMENT '专业描述',
    `status` TINYINT(1) DEFAULT 1 COMMENT '状态(0-停用, 1-启用)',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `created_by` BIGINT(20) DEFAULT NULL COMMENT '创建人',
    `updated_by` BIGINT(20) DEFAULT NULL COMMENT '更新人',
    `deleted` TINYINT(1) DEFAULT 0 COMMENT '逻辑删除(0-未删除, 1-已删除)',
    PRIMARY KEY (`id`),
    KEY `idx_major_name` (`major_name`),
    KEY `idx_major_code` (`major_code`),
    KEY `idx_department_id` (`department_id`),
    KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='专业表';

-- 插入示例数据
INSERT INTO `majors` (`major_name`, `major_code`, `education_system`, `training_level`, `department_id`, `description`, `status`) VALUES
('计算机网络', 'CS001', '3', 1, NULL, '培养计算机网络技术中级工', 1),
('软件技术', 'CS002', '3+2', 2, NULL, '培养软件开发高级工', 1),
('人工智能', 'CS003', '4', 3, NULL, '培养人工智能预备技师', 1),
('电子商务', 'EC001', '3', 1, NULL, '培养电子商务中级工', 1),
('会计', 'AC001', '3+3', 2, NULL, '培养会计高级工', 1);
