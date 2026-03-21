-- ==================== 评分方案（从 ScoringProfile 演化） ====================
CREATE TABLE IF NOT EXISTS insp_scoring_policies (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    policy_code VARCHAR(100) NOT NULL,
    policy_name VARCHAR(100) NOT NULL,
    description TEXT NULL,
    max_score DECIMAL(10,2) NOT NULL DEFAULT 100.00,
    min_score DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    precision_digits INT NOT NULL DEFAULT 2,
    is_system TINYINT DEFAULT 0,
    is_enabled TINYINT DEFAULT 1,
    sort_order INT DEFAULT 0,
    created_by BIGINT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT NULL,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    UNIQUE KEY uk_tenant_code (tenant_id, policy_code)
);

-- 评分方案的等级映射
CREATE TABLE IF NOT EXISTS insp_policy_grade_bands (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    policy_id BIGINT NOT NULL,
    grade_code VARCHAR(20) NOT NULL,
    grade_name VARCHAR(50) NOT NULL,
    min_score DECIMAL(10,2) NOT NULL,
    max_score DECIMAL(10,2) NOT NULL,
    sort_order INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    INDEX idx_policy (policy_id)
);

-- 评分方案的即时规则
CREATE TABLE IF NOT EXISTS insp_policy_calc_rules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    policy_id BIGINT NOT NULL,
    rule_code VARCHAR(50) NOT NULL,
    rule_name VARCHAR(100) NOT NULL,
    rule_type VARCHAR(30) NOT NULL COMMENT 'VETO/PENALTY/BONUS/PROGRESSIVE/CUSTOM',
    priority INT DEFAULT 0,
    config TEXT NULL COMMENT 'JSON: 规则参数',
    is_enabled TINYINT DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    INDEX idx_policy (policy_id)
);

-- 分区引用评分方案
ALTER TABLE insp_template_sections ADD COLUMN IF NOT EXISTS scoring_policy_id BIGINT NULL COMMENT '引用的评分方案ID';

-- ==================== 评选规则（替代 RatingDimension） ====================
CREATE TABLE IF NOT EXISTS insp_evaluation_rules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    project_id BIGINT NOT NULL,
    rule_name VARCHAR(100) NOT NULL,
    rule_description TEXT NULL,
    target_type VARCHAR(20) NOT NULL COMMENT 'ORG/PLACE/USER',
    evaluation_period VARCHAR(20) DEFAULT 'MONTHLY' COMMENT 'WEEKLY/MONTHLY/QUARTERLY/CUSTOM',
    award_name VARCHAR(100) NULL,
    ranking_enabled TINYINT DEFAULT 1,
    sort_order INT DEFAULT 0,
    is_enabled TINYINT DEFAULT 1,
    created_by BIGINT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT NULL,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    INDEX idx_project (project_id)
);

-- 评选等级（每个规则多个等级，从高到低判定）
CREATE TABLE IF NOT EXISTS insp_evaluation_levels (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rule_id BIGINT NOT NULL,
    level_num INT NOT NULL COMMENT '等级序号，1=最高',
    level_name VARCHAR(50) NOT NULL COMMENT '等级名称，如五星/四星',
    level_icon VARCHAR(50) NULL COMMENT '图标',
    level_color VARCHAR(20) NULL COMMENT '颜色',
    condition_logic VARCHAR(10) DEFAULT 'AND' COMMENT 'AND/OR',
    conditions TEXT NOT NULL COMMENT 'JSON: 条件列表',
    sort_order INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_rule (rule_id),
    UNIQUE KEY uk_rule_level (rule_id, level_num)
);

-- 评选结果
CREATE TABLE IF NOT EXISTS insp_evaluation_results (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    rule_id BIGINT NOT NULL,
    target_type VARCHAR(20) NOT NULL,
    target_id BIGINT NOT NULL,
    target_name VARCHAR(100) NULL,
    cycle_date DATE NOT NULL,
    level_num INT NULL,
    level_name VARCHAR(50) NULL,
    score DECIMAL(10,2) NULL,
    rank_no INT NULL,
    details TEXT NULL COMMENT 'JSON: 各条件的判定详情',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    INDEX idx_rule_cycle (rule_id, cycle_date),
    INDEX idx_target (target_type, target_id)
);

-- 预置评分方案
INSERT IGNORE INTO insp_scoring_policies (id, policy_code, policy_name, description, max_score, min_score, is_system) VALUES
(1, 'STANDARD_FIVE', '标准五级评分', '≥90优秀 ≥80良好 ≥70中等 ≥60及格 <60不及格', 100, 0, 1),
(2, 'PASS_FAIL', '通过/不通过', '≥60通过 <60不通过', 100, 0, 1),
(3, 'PERCENTAGE', '百分制', '直接分数，无等级映射', 100, 0, 1);

-- 标准五级的等级映射
INSERT IGNORE INTO insp_policy_grade_bands (policy_id, grade_code, grade_name, min_score, max_score, sort_order) VALUES
(1, 'A', '优秀', 90, 100, 1),
(1, 'B', '良好', 80, 89.99, 2),
(1, 'C', '中等', 70, 79.99, 3),
(1, 'D', '及格', 60, 69.99, 4),
(1, 'F', '不及格', 0, 59.99, 5);

-- 通过/不通过的等级映射
INSERT IGNORE INTO insp_policy_grade_bands (policy_id, grade_code, grade_name, min_score, max_score, sort_order) VALUES
(2, 'P', '通过', 60, 100, 1),
(2, 'F', '不通过', 0, 59.99, 2);
