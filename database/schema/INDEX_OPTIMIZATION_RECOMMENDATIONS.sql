-- =============================================================================
-- 量化系统数据库索引优化建议
-- 生成日期: 2025-11-26
-- 基于代码分析自动生成
-- =============================================================================

-- =============================================================================
-- 1. 申诉相关表索引 (check_item_appeals)
-- =============================================================================

-- 按班级和时间查询申诉历史
CREATE INDEX IF NOT EXISTS idx_cia_class_created
ON check_item_appeals(class_id, created_at DESC);

-- 按检查记录查询申诉
CREATE INDEX IF NOT EXISTS idx_cia_record_created
ON check_item_appeals(record_id, created_at DESC);

-- 按年级统计申诉
CREATE INDEX IF NOT EXISTS idx_cia_grade_created
ON check_item_appeals(grade_id, created_at);

-- 按状态查询
CREATE INDEX IF NOT EXISTS idx_cia_status
ON check_item_appeals(status);

-- 按申诉编码查询
CREATE INDEX IF NOT EXISTS idx_cia_appeal_code
ON check_item_appeals(appeal_code);

-- =============================================================================
-- 2. 检查记录班级统计表索引 (check_record_class_stats)
-- =============================================================================

-- 按记录ID和班级ID查询
CREATE INDEX IF NOT EXISTS idx_crcs_record_class
ON check_record_class_stats(record_id, class_id);

-- 按记录ID和年级ID排序查询(用于排名)
CREATE INDEX IF NOT EXISTS idx_crcs_record_grade_score
ON check_record_class_stats(record_id, grade_id, total_score DESC);

-- 按记录ID排序查询(用于全校排名)
CREATE INDEX IF NOT EXISTS idx_crcs_record_score
ON check_record_class_stats(record_id, total_score DESC);

-- 按年级ID统计
CREATE INDEX IF NOT EXISTS idx_crcs_grade
ON check_record_class_stats(grade_id);

-- =============================================================================
-- 3. 检查记录明细表索引 (check_record_items_v3)
-- =============================================================================

-- 按记录ID和班级ID查询
CREATE INDEX IF NOT EXISTS idx_cri3_record_class
ON check_record_items_v3(record_id, class_id);

-- 按申诉状态查询
CREATE INDEX IF NOT EXISTS idx_cri3_appeal_status
ON check_record_items_v3(appeal_status);

-- =============================================================================
-- 4. 日常检查表索引 (daily_checks)
-- =============================================================================

-- 按检查日期查询
CREATE INDEX IF NOT EXISTS idx_dc_check_date
ON daily_checks(check_date);

-- =============================================================================
-- 5. 班级人数快照表索引 (class_size_snapshots)
-- =============================================================================

-- 按记录ID查询
CREATE INDEX IF NOT EXISTS idx_css_record
ON class_size_snapshots(record_id);

-- 按班级ID和快照日期查询
CREATE INDEX IF NOT EXISTS idx_css_class_date
ON class_size_snapshots(class_id, snapshot_date DESC);

-- =============================================================================
-- 6. 检查记录加权配置表索引 (check_record_weight_configs)
-- =============================================================================

-- 按记录ID和配置级别查询
CREATE INDEX IF NOT EXISTS idx_crwc_record_level
ON check_record_weight_configs(record_id, config_level);

-- 按记录ID查询所有配置
CREATE INDEX IF NOT EXISTS idx_crwc_record
ON check_record_weight_configs(record_id);

-- =============================================================================
-- 7. 评级结果表索引 (check_rating_results)
-- =============================================================================

-- 按记录ID和排名查询
CREATE INDEX IF NOT EXISTS idx_crr_record_ranking
ON check_rating_results(record_id, ranking);

-- 按记录ID和班级ID查询
CREATE INDEX IF NOT EXISTS idx_crr_record_class
ON check_rating_results(record_id, class_id);

-- 按等级名称统计
CREATE INDEX IF NOT EXISTS idx_crr_level_name
ON check_rating_results(record_id, level_name);

-- =============================================================================
-- 8. 班级表索引优化 (classes)
-- =============================================================================

-- 按年级等级和入学年份查询(用于关联grades表)
CREATE INDEX IF NOT EXISTS idx_c_grade_enrollment
ON classes(grade_level, enrollment_year);

-- 按专业ID统计
CREATE INDEX IF NOT EXISTS idx_c_major
ON classes(major_id);

-- =============================================================================
-- 9. 年级专业方向表索引 (grade_major_directions)
-- =============================================================================

-- 按年级ID查询
CREATE INDEX IF NOT EXISTS idx_gmd_grade
ON grade_major_directions(grade_id);

-- 按专业方向ID查询
CREATE INDEX IF NOT EXISTS idx_gmd_direction
ON grade_major_directions(major_direction_id);

-- =============================================================================
-- 注意事项:
-- 1. 在生产环境执行前，请先在测试环境验证
-- 2. 建议在低峰期执行索引创建
-- 3. 创建索引后可使用 EXPLAIN 验证查询效率
-- 4. 定期使用 ANALYZE TABLE 更新索引统计信息
-- =============================================================================
