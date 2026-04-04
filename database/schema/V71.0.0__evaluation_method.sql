-- V71.0.0: Move evaluation method and grade thresholds from GradeScheme to Indicator
-- GradeDefinition: minValue/maxValue columns kept for backward compat but no longer used by new code.

-- Indicator: add evaluation method and thresholds
ALTER TABLE insp_indicators
    ADD COLUMN evaluation_method VARCHAR(20) DEFAULT 'PERCENT_RANGE' COMMENT 'SCORE_RANGE|PERCENT_RANGE|RANK_COUNT|RANK_PERCENT',
    ADD COLUMN grade_thresholds TEXT DEFAULT NULL COMMENT 'JSON: [{gradeCode, value}]';
