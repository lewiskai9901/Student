-- =========================================================================
-- Inspection System V4 - Phase 2: Bonus Items + Multi-level Inspection
-- Version: V20260127_2
-- Description:
--   1. bonus_items              - Bonus item configuration
--   2. inspection_bonuses       - Inspection bonus detail records
--   3. ALTER inspection_sessions - Add inspection_level column
-- =========================================================================

-- =====================================================
-- 1. Bonus Items (加分项配置表)
-- =====================================================
CREATE TABLE IF NOT EXISTS bonus_items (
    id                      BIGINT          PRIMARY KEY                          COMMENT '雪花ID',
    category_id             BIGINT          NOT NULL                            COMMENT '所属分类ID',
    item_name               VARCHAR(100)    NOT NULL                            COMMENT '加分项名称',
    bonus_mode              VARCHAR(20)     NOT NULL                            COMMENT '加分模式: FIXED/PROGRESSIVE/IMPROVEMENT',
    fixed_bonus             DECIMAL(10,2)                                       COMMENT '固定加分值(FIXED模式)',
    progressive_config      JSON                                                COMMENT '递进加分配置(PROGRESSIVE模式)',
    improvement_coefficient DECIMAL(5,2)                                        COMMENT '改善系数(IMPROVEMENT模式)',
    description             VARCHAR(500)                                        COMMENT '描述',
    sort_order              INT             DEFAULT 0                           COMMENT '排序',
    status                  TINYINT         DEFAULT 1                           COMMENT '状态: 1-启用 0-禁用',
    created_at              DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP  COMMENT '创建时间',
    updated_at              DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted                 INT             DEFAULT 0                           COMMENT '逻辑删除: 0-未删除 1-已删除',

    INDEX idx_category_id (category_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='加分项配置表(V4 Phase2)';

-- =====================================================
-- 2. Inspection Bonuses (检查加分明细表)
-- =====================================================
CREATE TABLE IF NOT EXISTS inspection_bonuses (
    id                      BIGINT          PRIMARY KEY                          COMMENT '雪花ID',
    class_record_id         BIGINT          NOT NULL                            COMMENT '班级检查记录ID',
    session_id              BIGINT          NOT NULL                            COMMENT '检查会话ID',
    class_id                BIGINT          NOT NULL                            COMMENT '班级ID',
    bonus_item_id           BIGINT          NOT NULL                            COMMENT '加分项ID',
    bonus_score             DECIMAL(10,2)   NOT NULL                            COMMENT '加分分值',
    reason                  VARCHAR(500)                                        COMMENT '加分原因',
    recorded_by             BIGINT                                              COMMENT '记录人ID',
    created_at              DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP  COMMENT '创建时间',
    updated_at              DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted                 INT             DEFAULT 0                           COMMENT '逻辑删除: 0-未删除 1-已删除',

    INDEX idx_class_record_id (class_record_id),
    INDEX idx_session_id (session_id),
    INDEX idx_class_id (class_id),
    INDEX idx_bonus_item_id (bonus_item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='检查加分明细表(V4 Phase2)';

-- =====================================================
-- 3. ALTER inspection_sessions - Add inspection_level
-- =====================================================
ALTER TABLE inspection_sessions
    ADD COLUMN inspection_level VARCHAR(32) DEFAULT 'CLASS' COMMENT '检查级别: CLASS/DEPARTMENT/SPECIAL'
    AFTER base_score;
