-- 将高级评分设置从模板级(scoring_profiles)移到项目级(projects)
-- 模板定义"怎么算分"，项目定义"怎么执行"

ALTER TABLE insp_projects ADD COLUMN evaluation_mode VARCHAR(20) DEFAULT 'SINGLE' COMMENT '单评/多评: SINGLE, MULTI';
ALTER TABLE insp_projects ADD COLUMN multi_rater_mode VARCHAR(30) DEFAULT 'AVERAGE' COMMENT '多人合并: LATEST,AVERAGE,WEIGHTED_AVERAGE,MEDIAN,MAX,MIN,CONSENSUS';
ALTER TABLE insp_projects ADD COLUMN rater_weight_by VARCHAR(20) DEFAULT 'EQUAL' COMMENT '评分人权重: EQUAL,BY_ROLE,BY_EXPERIENCE,CUSTOM';
ALTER TABLE insp_projects ADD COLUMN consensus_threshold DECIMAL(5,2) NULL COMMENT '共识阈值';
ALTER TABLE insp_projects ADD COLUMN trend_enabled TINYINT DEFAULT 0 COMMENT '启用趋势因子';
ALTER TABLE insp_projects ADD COLUMN trend_lookback_days INT DEFAULT 7 COMMENT '趋势回溯天数';
ALTER TABLE insp_projects ADD COLUMN decay_enabled TINYINT DEFAULT 0 COMMENT '启用分数衰减';
ALTER TABLE insp_projects ADD COLUMN decay_mode VARCHAR(20) DEFAULT 'LINEAR' COMMENT 'LINEAR,EXPONENTIAL';
ALTER TABLE insp_projects ADD COLUMN calibration_enabled TINYINT DEFAULT 0 COMMENT '启用校准';
ALTER TABLE insp_projects ADD COLUMN calibration_method VARCHAR(20) DEFAULT 'Z_SCORE' COMMENT 'Z_SCORE,MIN_MAX,PERCENTILE_RANK,IRT';
