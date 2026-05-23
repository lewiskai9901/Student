-- ============================================================
-- V20260523_3: ScoringProfile 项目-owned 收尾 (Phase 5)
--
-- Phase 2 Java @PostConstruct 迁移已为所有引用的 profile 填充 project_id
-- + 清理 orphan profiles. 此处:
--   1) ALTER COLUMN project_id NOT NULL — 强制每个 profile 必属一项目
--   2) CREATE UNIQUE INDEX uk_scoring_profile_project_section — 每项目每分区
--      至多一套评分方案
--   3) 旧的 idx_scoring_profile_project 索引可删 (复合唯一索引覆盖单列查询)
--
-- 全部 information_schema 条件化, 可重复执行.
-- ============================================================

-- 1) project_id NOT NULL
SET @nullable := (SELECT IS_NULLABLE FROM information_schema.columns
                  WHERE table_schema=DATABASE() AND table_name='insp_scoring_profiles'
                    AND column_name='project_id');
SET @sql := IF(@nullable='YES',
  'ALTER TABLE insp_scoring_profiles MODIFY COLUMN project_id BIGINT NOT NULL COMMENT ''项目-owned: profile 必属一项目, 跨项目不共享''',
  'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- 2) (project_id, section_id) 唯一索引
SET @uk := (SELECT COUNT(*) FROM information_schema.statistics
            WHERE table_schema=DATABASE() AND table_name='insp_scoring_profiles'
              AND index_name='uk_scoring_profile_project_section');
SET @sql := IF(@uk=0,
  'CREATE UNIQUE INDEX uk_scoring_profile_project_section ON insp_scoring_profiles (project_id, section_id)',
  'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- 3) 删除旧的单列索引 idx_scoring_profile_project (复合唯一索引已覆盖单列查询)
SET @idx := (SELECT COUNT(*) FROM information_schema.statistics
             WHERE table_schema=DATABASE() AND table_name='insp_scoring_profiles'
               AND index_name='idx_scoring_profile_project');
SET @sql := IF(@idx>0,
  'ALTER TABLE insp_scoring_profiles DROP INDEX idx_scoring_profile_project',
  'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
