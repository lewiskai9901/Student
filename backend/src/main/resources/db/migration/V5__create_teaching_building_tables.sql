-- 教学楼表
CREATE TABLE IF NOT EXISTS `teaching_buildings` (
    `id` BIGINT NOT NULL PRIMARY KEY COMMENT '教学楼ID',
    `building_name` VARCHAR(100) NOT NULL COMMENT '教学楼名称',
    `building_code` VARCHAR(50) NOT NULL UNIQUE COMMENT '教学楼编号',
    `floor_count` INT NOT NULL DEFAULT 0 COMMENT '楼层数',
    `location` VARCHAR(200) COMMENT '位置',
    `description` TEXT COMMENT '描述',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态(0-停用,1-启用)',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除(0-未删除,1-已删除)',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_building_code` (`building_code`),
    INDEX `idx_status` (`status`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教学楼表';

-- 教室表
CREATE TABLE IF NOT EXISTS `classrooms` (
    `id` BIGINT NOT NULL PRIMARY KEY COMMENT '教室ID',
    `building_id` BIGINT NOT NULL COMMENT '教学楼ID',
    `classroom_name` VARCHAR(100) NOT NULL COMMENT '教室名称',
    `classroom_code` VARCHAR(50) NOT NULL UNIQUE COMMENT '教室编号',
    `floor` INT NOT NULL COMMENT '楼层',
    `room_number` VARCHAR(50) NOT NULL COMMENT '房间号',
    `capacity` INT NOT NULL DEFAULT 0 COMMENT '容纳人数',
    `class_id` BIGINT COMMENT '关联班级ID',
    `class_name` VARCHAR(100) COMMENT '班级名称(冗余)',
    `head_teacher_id` BIGINT COMMENT '班主任ID',
    `head_teacher_name` VARCHAR(100) COMMENT '班主任姓名(冗余)',
    `student_count` INT NOT NULL DEFAULT 0 COMMENT '班级人数',
    `classroom_type` VARCHAR(50) COMMENT '教室类型(普通教室/实验室/多媒体教室等)',
    `facilities` TEXT COMMENT '设施设备',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态(0-停用,1-启用)',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除(0-未删除,1-已删除)',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_building_id` (`building_id`),
    INDEX `idx_classroom_code` (`classroom_code`),
    INDEX `idx_class_id` (`class_id`),
    INDEX `idx_floor` (`floor`),
    INDEX `idx_status` (`status`),
    INDEX `idx_deleted` (`deleted`),
    FOREIGN KEY (`building_id`) REFERENCES `teaching_buildings`(`id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教室表';

-- 插入示例数据
INSERT INTO `teaching_buildings` (`id`, `building_name`, `building_code`, `floor_count`, `location`, `description`, `status`) VALUES
(1, '第一教学楼', 'A', 5, '校区东侧', '主教学楼,配备现代化教学设施', 1),
(2, '第二教学楼', 'B', 4, '校区西侧', '综合教学楼', 1),
(3, '实验楼', 'C', 3, '校区北侧', '各类实验室', 1);

-- 插入示例教室数据
INSERT INTO `classrooms` (`id`, `building_id`, `classroom_name`, `classroom_code`, `floor`, `room_number`, `capacity`, `classroom_type`, `facilities`, `status`) VALUES
(1, 1, 'A101教室', 'A101', 1, '101', 50, '普通教室', '多媒体投影、空调', 1),
(2, 1, 'A102教室', 'A102', 1, '102', 50, '普通教室', '多媒体投影、空调', 1),
(3, 1, 'A201教室', 'A201', 2, '201', 45, '多媒体教室', '智能黑板、投影、音响', 1),
(4, 2, 'B101教室', 'B101', 1, '101', 60, '普通教室', '多媒体投影', 1),
(5, 3, 'C101实验室', 'C101', 1, '101', 30, '实验室', '实验设备、通风系统', 1);
