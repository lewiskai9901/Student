-- V113.0.0 重建被迁移误删/未建成的 4 个对象 (归属统一重构无关, 修 fresh-init 债)
-- 背景: 线上库丢失后从迁移链重建, 发现应用仍依赖但库中缺失的 4 个对象:
--   1. org_units.type_code: 反范式辅助列(=unit_type), classes 视图/类型 seed 依赖,
--      OrgUnitPO 不经 ORM 读它, 故列级对账未覆盖。源: V35.0.1 UPDATE org_units SET type_code=unit_type。
--   2. data_modules: 被 V20260419_6 删除, 但 DataModuleController/DataPermissionInterceptor 等活代码仍引用。源: V26.0.0。
--   3. schedule_policies / schedule_executions: 被 V74 第50-51行误删, core schedule 模块 PO 仍在用。源: baseline。
--   4. classes 视图: org_units(type_code='CLASS') 的视图, 因 type_code 缺失而建失败。源: V20260408_2。
-- 全部 IF NOT EXISTS / OR REPLACE, 幂等。

SET NAMES utf8mb4;

-- 1. org_units.type_code -------------------------------------------------
SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'org_units' AND COLUMN_NAME = 'type_code');
SET @s := IF(@c = 0,
    "ALTER TABLE org_units ADD COLUMN type_code VARCHAR(50) NULL COMMENT '类型编码(=unit_type 反范式, 视图/seed 用)' AFTER unit_type",
    "SELECT 'org_units.type_code exists' AS msg");
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @i := (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'org_units' AND INDEX_NAME = 'idx_org_units_type_code');
SET @s := IF(@i = 0,
    "ALTER TABLE org_units ADD INDEX idx_org_units_type_code (type_code)",
    "SELECT 'idx_org_units_type_code exists' AS msg");
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

UPDATE org_units SET type_code = unit_type
WHERE (type_code IS NULL OR type_code = '') AND unit_type IS NOT NULL AND unit_type != '';

-- 2. data_modules --------------------------------------------------------
CREATE TABLE IF NOT EXISTS data_modules (
    id             BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id      BIGINT       NOT NULL DEFAULT 1,
    module_code    VARCHAR(50)  NOT NULL,
    module_name    VARCHAR(100) NOT NULL,
    domain_code    VARCHAR(50)  NOT NULL COMMENT '所属领域标识',
    domain_name    VARCHAR(100) NOT NULL COMMENT '领域显示名',
    resource_type  VARCHAR(50)  DEFAULT NULL COMMENT '对应 access_relations.resource_type',
    org_unit_field VARCHAR(50)  DEFAULT 'org_unit_id' COMMENT '表中的组织字段名',
    creator_field  VARCHAR(50)  DEFAULT 'created_by',
    sort_order     INT          DEFAULT 0,
    enabled        TINYINT(1)   DEFAULT 1,
    industry       VARCHAR(50)  DEFAULT NULL COMMENT '所属行业插件(NULL=通用核心)',
    plugin_enabled TINYINT(1)   DEFAULT 1 COMMENT '插件是否启用',
    UNIQUE KEY uk_tenant_code (tenant_id, module_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='动态数据模块配置';

-- 既有 data_modules 补列 (表已存在时)
SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='data_modules' AND COLUMN_NAME='industry');
SET @s := IF(@c=0, "ALTER TABLE data_modules ADD COLUMN industry VARCHAR(50) DEFAULT NULL COMMENT '所属行业插件(NULL=通用核心)'", "SELECT 1");
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='data_modules' AND COLUMN_NAME='plugin_enabled');
SET @s := IF(@c=0, "ALTER TABLE data_modules ADD COLUMN plugin_enabled TINYINT(1) DEFAULT 1 COMMENT '插件是否启用'", "SELECT 1");
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

INSERT IGNORE INTO data_modules (tenant_id, module_code, module_name, domain_code, domain_name, resource_type, org_unit_field, creator_field, sort_order) VALUES
(1, 'org_unit',             '组织单元',   'organization', '组织管理', 'org_unit',             'parent_id',   'created_by', 1),
(1, 'student',              '学生信息',   'student',      '学生管理', 'student',              'org_unit_id', 'created_by', 10),
(1, 'school_class',         '班级管理',   'student',      '学生管理', 'school_class',         'org_unit_id', 'created_by', 11),
(1, 'attendance',           '考勤记录',   'student',      '学生管理', 'attendance',           'class_id',    'recorded_by', 12),
(1, 'teaching_task',        '教学任务',   'teaching',     '教学管理', 'teaching_task',        'org_unit_id', 'created_by', 20),
(1, 'exam_batch',           '考试管理',   'teaching',     '教学管理', 'exam_batch',           '',            'created_by', 21),
(1, 'student_grade',        '学生成绩',   'teaching',     '教学管理', 'student_grade',        'class_id',    '',           22),
(1, 'inspection_template',  '检查模板',   'inspection',   '检查平台', 'inspection_template',  'org_unit_id', 'created_by', 30),
(1, 'inspection_record',    '检查记录',   'inspection',   '检查平台', 'inspection_record',    'org_unit_id', 'created_by', 31),
(1, 'place',                '场所管理',   'place',        '场所管理', 'place',                'org_unit_id', 'created_by', 40);

-- 3. schedule_policies / schedule_executions -----------------------------
CREATE TABLE IF NOT EXISTS `schedule_policies` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `policy_code` VARCHAR(50) NOT NULL COMMENT '策略编码',
    `policy_name` VARCHAR(100) NOT NULL COMMENT '策略名称',
    `policy_type` VARCHAR(20) COMMENT '策略类型',
    `rotation_algorithm` VARCHAR(20) COMMENT '轮换算法',
    `template_id` BIGINT COMMENT '关联模板ID',
    `inspector_pool` JSON COMMENT '检查员池',
    `schedule_config` JSON COMMENT '排程配置',
    `excluded_dates` JSON COMMENT '排除日期',
    `is_enabled` TINYINT(1) DEFAULT 1 COMMENT '是否启用',
    `created_by` BIGINT COMMENT '创建人',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_policy_code` (`policy_code`),
    INDEX `idx_template_id` (`template_id`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='排程策略表';

CREATE TABLE IF NOT EXISTS `schedule_executions` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `policy_id` BIGINT NOT NULL COMMENT '策略ID',
    `execution_date` DATE NOT NULL COMMENT '执行日期',
    `assigned_inspectors` JSON COMMENT '分配的检查员',
    `session_id` BIGINT COMMENT '关联会话ID',
    `status` VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态',
    `failure_reason` VARCHAR(500) COMMENT '失败原因',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    INDEX `idx_policy_id` (`policy_id`),
    INDEX `idx_execution_date` (`execution_date`),
    INDEX `idx_status` (`status`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='排程执行表';

-- 4. classes 视图 --------------------------------------------------------
CREATE OR REPLACE VIEW classes AS
SELECT
  o.id,
  o.unit_name AS class_name,
  o.unit_code AS class_code,
  COALESCE(CAST(JSON_UNQUOTE(JSON_EXTRACT(o.attributes, '$.gradeLevel')) AS UNSIGNED), 1) AS grade_level,
  o.parent_id AS org_unit_id,
  CAST(JSON_UNQUOTE(JSON_EXTRACT(o.attributes, '$.gradeId')) AS UNSIGNED) AS grade_id,
  CAST(JSON_UNQUOTE(JSON_EXTRACT(o.attributes, '$.majorId')) AS UNSIGNED) AS major_id,
  CAST(JSON_UNQUOTE(JSON_EXTRACT(o.attributes, '$.majorDirectionId')) AS UNSIGNED) AS major_direction_id,
  NULL AS class_sequence,
  CAST(JSON_UNQUOTE(JSON_EXTRACT(o.attributes, '$.headTeacher')) AS UNSIGNED) AS teacher_id,
  CAST(JSON_UNQUOTE(JSON_EXTRACT(o.attributes, '$.assistantTeacher')) AS UNSIGNED) AS assistant_teacher_id,
  COALESCE(CAST(JSON_UNQUOTE(JSON_EXTRACT(o.attributes, '$.studentCount')) AS UNSIGNED), 0) AS student_count,
  JSON_UNQUOTE(JSON_EXTRACT(o.attributes, '$.classroomLocation')) AS classroom_location,
  YEAR(NOW()) AS enrollment_year,
  JSON_UNQUOTE(JSON_EXTRACT(o.attributes, '$.educationSystem')) AS education_system,
  NULL AS skill_level,
  CAST(JSON_UNQUOTE(JSON_EXTRACT(o.attributes, '$.duration')) AS UNSIGNED) AS duration,
  CAST(JSON_UNQUOTE(JSON_EXTRACT(o.attributes, '$.graduationYear')) AS UNSIGNED) AS graduation_year,
  COALESCE(CAST(JSON_UNQUOTE(JSON_EXTRACT(o.attributes, '$.classType')) AS UNSIGNED), 1) AS class_type,
  CASE o.status WHEN 'ACTIVE' THEN 1 WHEN 'FROZEN' THEN 0 ELSE 1 END AS status,
  0 AS is_international,
  0 AS is_experimental,
  0 AS is_oriented,
  o.created_at,
  o.updated_at,
  o.created_by,
  o.updated_by,
  o.deleted,
  o.tenant_id
FROM org_units o
WHERE o.type_code = 'CLASS';
