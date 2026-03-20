-- V14.0.0: 场所类型系统重构 — 两层架构（基础分类 + 具体类型树）
-- 1. place_types 增加 base_category 和 parent_type_id 列
-- 2. STATION → POINT 重命名
-- 3. places 增加 gender 列
-- 4. placeCode 唯一约束调整为父节点内唯一
SET NAMES utf8mb4;

-- ==================== 1. place_types 表加列 ====================

ALTER TABLE place_types ADD COLUMN base_category VARCHAR(20) NULL
  COMMENT '基础分类: SITE/BUILDING/FLOOR/ROOM/AREA/POINT（具体类型引用基础分类，基础分类自身为NULL）';

ALTER TABLE place_types ADD COLUMN parent_type_id BIGINT NULL
  COMMENT '父类型ID（具体类型树结构）';

CREATE INDEX idx_pt_base_category ON place_types (base_category);
CREATE INDEX idx_pt_parent_type_id ON place_types (parent_type_id);

-- ==================== 2. STATION → POINT ====================

UPDATE place_types SET type_code = 'POINT', type_name = '点位'
WHERE type_code = 'STATION' AND deleted = 0;

-- 同步更新 places 表中引用的 type_code
UPDATE places SET type_code = 'POINT'
WHERE type_code = 'STATION' AND deleted = 0;

-- ==================== 3. places 表加 gender ====================

ALTER TABLE places ADD COLUMN gender VARCHAR(10) NULL
  COMMENT '性别限制: MALE/FEMALE/MIXED (NULL=继承父节点)';

CREATE INDEX idx_places_gender ON places (gender);

-- ==================== 4. placeCode 唯一约束变更 ====================
-- 从全局唯一 → 父节点内唯一（应用层校验）
-- 先检查索引是否存在再删除

-- 尝试删除全局唯一约束（如果存在）
SET @idx_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'places' AND INDEX_NAME = 'uk_place_code');

SET @sql = IF(@idx_exists > 0, 'ALTER TABLE places DROP INDEX uk_place_code', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 创建复合索引用于父节点内唯一性查询
CREATE INDEX idx_parent_place_code ON places (parent_id, place_code, deleted);
