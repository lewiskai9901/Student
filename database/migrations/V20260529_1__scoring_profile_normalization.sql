-- ============================================================
-- Migration: V20260529_1__scoring_profile_normalization.sql
-- Description: 章节级评分方案 (insp_scoring_profiles) 新增归一化配置字段
--              (规模公平性重构 Phase 1B). 供装配层 (Phase 1C) 解析归一化分母时读取.
--
-- 新增列:
--   normalize_by          VARCHAR(20)   NOT NULL DEFAULT 'NONE'  归一化分母维度 NONE|PER_MEMBER|PER_PLACE|PER_SUB_ORG
--   normalization_mode    VARCHAR(20)   NOT NULL DEFAULT 'NONE'  归一化模式 NONE|PER_CAPITA|SQRT_ADJUSTED
--   baseline_population   INT           NOT NULL DEFAULT 1       归一化基准人口/规模
--   norm_floor            DECIMAL(10,4) NULL                     归一化后下限 (NULL=不限)
--   norm_cap              DECIMAL(10,4) NULL                     归一化后上限 (NULL=不限)
--
-- 幂等: 通过 information_schema 条件化执行 (检测 normalize_by 列是否存在),
--       重复运行不会失败 (与 V97/V104 风格一致).
-- ============================================================

-- 以 normalize_by 列作为本次迁移是否已应用的标志 (5 列一次性添加)
SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'insp_scoring_profiles'
      AND column_name = 'normalize_by');

SET @sql = IF(@col_exists = 0,
    'ALTER TABLE insp_scoring_profiles
        ADD COLUMN normalize_by        VARCHAR(20)   NOT NULL DEFAULT ''NONE'' COMMENT ''归一化分母维度 NONE|PER_MEMBER|PER_PLACE|PER_SUB_ORG'',
        ADD COLUMN normalization_mode  VARCHAR(20)   NOT NULL DEFAULT ''NONE'' COMMENT ''归一化模式 NONE|PER_CAPITA|SQRT_ADJUSTED'',
        ADD COLUMN baseline_population INT           NOT NULL DEFAULT 1        COMMENT ''归一化基准人口/规模'',
        ADD COLUMN norm_floor          DECIMAL(10,4) NULL                      COMMENT ''归一化后下限(NULL=不限)'',
        ADD COLUMN norm_cap            DECIMAL(10,4) NULL                      COMMENT ''归一化后上限(NULL=不限)''',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
