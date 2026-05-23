-- ============================================================
-- V20260523_2: ScoringProfile 项目-owned (Phase 1/2)
--
-- 第一步: insp_scoring_profiles 加 project_id 列 (NULL allowed for now).
-- Phase 2 Java @PostConstruct 迁移代码会按"每个项目 × 它引用的 profile"
-- 写新副本并填 project_id; Phase 5 再补 NOT NULL 约束 + 唯一索引.
--
-- 配套同步:
--   1) insp_grade_bands           — 关联表 (scoring_profile_id FK), 不动结构
--   2) insp_calculation_rules     — 同上
--   3) insp_score_dimensions      — 同上
--   迁移时这三张表的行会随 profile 一起复制 (Java 端处理).
-- ============================================================

SET @c := (SELECT COUNT(*) FROM information_schema.columns
           WHERE table_schema=DATABASE() AND table_name='insp_scoring_profiles'
             AND column_name='project_id');
SET @sql := IF(@c=0,
  'ALTER TABLE insp_scoring_profiles ADD COLUMN project_id BIGINT NULL COMMENT ''项目-owned: profile 归属项目, 跨项目不共享'' AFTER section_id',
  'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- 加索引便于按项目过滤查询 (NOT NULL + UNIQUE 留给 Phase 5).
SET @ix := (SELECT COUNT(*) FROM information_schema.statistics
            WHERE table_schema=DATABASE() AND table_name='insp_scoring_profiles'
              AND index_name='idx_scoring_profile_project');
SET @sql := IF(@ix=0,
  'CREATE INDEX idx_scoring_profile_project ON insp_scoring_profiles (project_id)',
  'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- migration_locks 表: 用于 Java 端 @PostConstruct 迁移幂等标记
SET @t := (SELECT COUNT(*) FROM information_schema.tables
           WHERE table_schema=DATABASE() AND table_name='migration_locks');
SET @sql := IF(@t=0,
  'CREATE TABLE migration_locks (
     lock_key VARCHAR(100) NOT NULL PRIMARY KEY,
     locked_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
     note VARCHAR(500)
   ) COMMENT=''一次性数据迁移幂等标记''',
  'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
