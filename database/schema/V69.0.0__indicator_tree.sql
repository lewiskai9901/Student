-- Indicator table
CREATE TABLE insp_indicators (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id             BIGINT NOT NULL DEFAULT 0,
    project_id            BIGINT NOT NULL,
    parent_indicator_id   BIGINT DEFAULT NULL,
    name                  VARCHAR(100) NOT NULL COMMENT '指标名称',
    indicator_type        VARCHAR(20) NOT NULL DEFAULT 'LEAF' COMMENT 'LEAF|COMPOSITE',
    source_section_id     BIGINT DEFAULT NULL COMMENT '数据来源分区ID (LEAF)',
    source_aggregation    VARCHAR(20) DEFAULT 'AVG' COMMENT 'AVG|MAX|MIN|LATEST|SUM (LEAF)',
    composite_aggregation VARCHAR(20) DEFAULT 'WEIGHTED_AVG' COMMENT 'WEIGHTED_AVG|SUM|AVG|MIN|MAX (COMPOSITE)',
    missing_policy        VARCHAR(20) DEFAULT 'SKIP' COMMENT 'SKIP|CARRY_FORWARD|MARK_INCOMPLETE (COMPOSITE)',
    evaluation_period     VARCHAR(20) NOT NULL DEFAULT 'PER_TASK' COMMENT 'PER_TASK|DAILY|WEEKLY|MONTHLY',
    grade_scheme_id       BIGINT DEFAULT NULL COMMENT 'FK -> insp_grade_schemes',
    sort_order            INT NOT NULL DEFAULT 0,
    created_at            DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted               TINYINT(1) NOT NULL DEFAULT 0,
    INDEX idx_project (project_id),
    INDEX idx_parent (parent_indicator_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评价指标';

-- IndicatorScore table
CREATE TABLE insp_indicator_scores (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id       BIGINT NOT NULL DEFAULT 0,
    indicator_id    BIGINT NOT NULL,
    target_id       BIGINT NOT NULL,
    target_name     VARCHAR(200) DEFAULT NULL,
    target_type     VARCHAR(20) DEFAULT NULL,
    period_start    DATE NOT NULL,
    period_end      DATE NOT NULL,
    score           DECIMAL(10,2) DEFAULT NULL,
    grade_code      VARCHAR(50) DEFAULT NULL,
    grade_name      VARCHAR(100) DEFAULT NULL,
    grade_color     VARCHAR(20) DEFAULT NULL,
    source_count    INT DEFAULT 0,
    detail          TEXT DEFAULT NULL COMMENT 'JSON',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT(1) NOT NULL DEFAULT 0,
    INDEX idx_indicator (indicator_id),
    INDEX idx_target (target_id),
    INDEX idx_period (period_start, period_end),
    UNIQUE KEY uk_indicator_target_period (indicator_id, target_id, period_start)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='指标得分记录';
