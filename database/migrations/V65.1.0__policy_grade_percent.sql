-- 评分方案删除 maxScore/minScore（移到分区级别，分区有自己的 max_score/min_score）
ALTER TABLE insp_scoring_policies DROP COLUMN max_score;
ALTER TABLE insp_scoring_policies DROP COLUMN min_score;

-- 等级映射改为百分比（语义变化：90 表示 90%，而不是 90 分）
ALTER TABLE insp_policy_grade_bands RENAME COLUMN min_score TO min_percent;
ALTER TABLE insp_policy_grade_bands RENAME COLUMN max_score TO max_percent;

-- 注：现有数据无需转换
-- 原来基于满分=100的绝对分数，与百分比值相同（如 90分 = 90%）
