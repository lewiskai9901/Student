-- V67.0.0: 等级方案 — 可自定义等级命名映射

CREATE TABLE insp_grade_schemes (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  display_name VARCHAR(100) NOT NULL COMMENT '方案显示名称',
  description VARCHAR(500) DEFAULT NULL COMMENT '方案描述',
  scheme_type VARCHAR(20) NOT NULL DEFAULT 'SCORE_RANGE' COMMENT 'SCORE_RANGE|PERCENT_RANGE',
  is_system TINYINT(1) NOT NULL DEFAULT 0,
  created_by BIGINT DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  INDEX idx_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='等级方案';

CREATE TABLE insp_grade_definitions (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  grade_scheme_id BIGINT NOT NULL,
  code VARCHAR(50) NOT NULL COMMENT '等级编码',
  name VARCHAR(100) NOT NULL COMMENT '等级名称',
  min_value DECIMAL(10,2) NOT NULL,
  max_value DECIMAL(10,2) NOT NULL,
  color VARCHAR(20) DEFAULT NULL,
  icon VARCHAR(50) DEFAULT NULL,
  sort_order INT NOT NULL DEFAULT 0,
  INDEX idx_scheme (grade_scheme_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='等级定义';

-- ============================================================
-- ALTER existing tables
-- ============================================================

ALTER TABLE insp_template_sections
  ADD COLUMN grade_scheme_id BIGINT DEFAULT NULL COMMENT 'FK → insp_grade_schemes.id';

ALTER TABLE insp_project_scores
  ADD COLUMN grade_scheme_display_name VARCHAR(100) DEFAULT NULL COMMENT '等级方案名称快照',
  ADD COLUMN grade_name VARCHAR(100) DEFAULT NULL COMMENT '等级名称快照';

-- ============================================================
-- System presets
-- ============================================================

-- 1. 标准五级评定
INSERT INTO insp_grade_schemes (tenant_id, display_name, description, scheme_type, is_system)
VALUES (0, '标准五级评定', '按百分比划分A/B/C/D/F五个等级', 'PERCENT_RANGE', 1);
SET @scheme1 = LAST_INSERT_ID();

INSERT INTO insp_grade_definitions (grade_scheme_id, code, name, min_value, max_value, color, icon, sort_order) VALUES
(@scheme1, 'A', '优秀', 90.00, 100.00, '#22C55E', NULL, 1),
(@scheme1, 'B', '良好', 80.00,  89.99, '#3B82F6', NULL, 2),
(@scheme1, 'C', '中等', 70.00,  79.99, '#F59E0B', NULL, 3),
(@scheme1, 'D', '及格', 60.00,  69.99, '#F97316', NULL, 4),
(@scheme1, 'F', '不及格', 0.00,  59.99, '#EF4444', NULL, 5);

-- 2. 合格评定
INSERT INTO insp_grade_schemes (tenant_id, display_name, description, scheme_type, is_system)
VALUES (0, '合格评定', '简单的合格/不合格二级评定', 'PERCENT_RANGE', 1);
SET @scheme2 = LAST_INSERT_ID();

INSERT INTO insp_grade_definitions (grade_scheme_id, code, name, min_value, max_value, color, icon, sort_order) VALUES
(@scheme2, 'PASS', '合格',   60.00, 100.00, '#22C55E', NULL, 1),
(@scheme2, 'FAIL', '不合格',  0.00,  59.99, '#EF4444', NULL, 2);

-- 3. 流动红旗
INSERT INTO insp_grade_schemes (tenant_id, display_name, description, scheme_type, is_system)
VALUES (0, '流动红旗', '红旗/蓝旗/黄旗三级流动旗评定', 'PERCENT_RANGE', 1);
SET @scheme3 = LAST_INSERT_ID();

INSERT INTO insp_grade_definitions (grade_scheme_id, code, name, min_value, max_value, color, icon, sort_order) VALUES
(@scheme3, 'RED',    '红旗', 90.00, 100.00, '#EF4444', 'flag', 1),
(@scheme3, 'BLUE',   '蓝旗', 70.00,  89.99, '#3B82F6', 'flag', 2),
(@scheme3, 'YELLOW', '黄旗',  0.00,  69.99, '#F59E0B', 'flag', 3);

-- 4. 星级评定
INSERT INTO insp_grade_schemes (tenant_id, display_name, description, scheme_type, is_system)
VALUES (0, '星级评定', '五星到一星五级星级评定', 'PERCENT_RANGE', 1);
SET @scheme4 = LAST_INSERT_ID();

INSERT INTO insp_grade_definitions (grade_scheme_id, code, name, min_value, max_value, color, icon, sort_order) VALUES
(@scheme4, '5STAR', '五星', 90.00, 100.00, '#FFD700', 'star', 1),
(@scheme4, '4STAR', '四星', 80.00,  89.99, '#C0C0C0', 'star', 2),
(@scheme4, '3STAR', '三星', 70.00,  79.99, '#CD7F32', 'star', 3),
(@scheme4, '2STAR', '二星', 60.00,  69.99, '#F97316', 'star', 4),
(@scheme4, '1STAR', '一星',  0.00,  59.99, '#808080', 'star', 5);
