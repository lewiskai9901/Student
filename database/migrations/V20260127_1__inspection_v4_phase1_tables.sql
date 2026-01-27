-- =========================================================================
-- Inspection System V4 - Phase 1: Core Tables
-- Version: V20260127_1
-- Description: Create the 4 core tables for the V4 inspection system:
--   1. inspection_sessions        - Inspection session (aggregate root)
--   2. class_inspection_records   - Per-class inspection record (aggregate root)
--   3. checklist_responses        - Per-item checklist response
--   4. inspection_deductions      - Deduction detail records
--
-- Key design decisions:
--   - Snowflake IDs (BIGINT, not auto-increment) for all primary keys
--   - Support for 3 input modes: SPACE_FIRST, PERSON_FIRST, ORG_FIRST
--   - Support for 3 scoring modes: DEDUCTION_ONLY, BASE_SCORE, DUAL_TRACK
--   - JSON columns for student lists and evidence URLs
--   - Logical delete via `deleted` column
-- =========================================================================

-- =====================================================
-- 1. Inspection Sessions (检查会话表 - 聚合根)
-- =====================================================
CREATE TABLE IF NOT EXISTS inspection_sessions (
    id                  BIGINT          PRIMARY KEY                          COMMENT '雪花ID',
    session_code        VARCHAR(64)     NOT NULL                            COMMENT '会话编码，如 SES-20260127-XXXX',
    template_id         BIGINT          NOT NULL                            COMMENT '检查模板ID',
    template_version    INT             DEFAULT 1                           COMMENT '模板版本号',
    inspection_date     DATE            NOT NULL                            COMMENT '检查日期',
    inspection_period   VARCHAR(32)                                         COMMENT '检查时段: morning/afternoon/evening',
    input_mode          VARCHAR(32)     NOT NULL DEFAULT 'SPACE_FIRST'      COMMENT '录入模式: SPACE_FIRST/PERSON_FIRST/ORG_FIRST',
    scoring_mode        VARCHAR(32)     NOT NULL DEFAULT 'DEDUCTION_ONLY'   COMMENT '评分模式: DEDUCTION_ONLY/BASE_SCORE/DUAL_TRACK',
    base_score          INT             DEFAULT 100                         COMMENT '基础分(BASE_SCORE/DUAL_TRACK模式使用)',
    status              VARCHAR(32)     NOT NULL DEFAULT 'CREATED'          COMMENT '状态: CREATED/IN_PROGRESS/SUBMITTED/PUBLISHED',
    inspector_id        BIGINT          NOT NULL                            COMMENT '检查人ID',
    inspector_name      VARCHAR(64)                                         COMMENT '检查人姓名',
    submitted_at        DATETIME                                            COMMENT '提交时间',
    published_at        DATETIME                                            COMMENT '发布时间',
    remarks             TEXT                                                COMMENT '备注',
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP  COMMENT '创建时间',
    created_by          BIGINT                                              COMMENT '创建人ID',
    updated_at          DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted             INT             DEFAULT 0                           COMMENT '逻辑删除: 0-未删除 1-已删除',

    UNIQUE KEY uk_session_code (session_code),
    INDEX idx_inspection_date (inspection_date),
    INDEX idx_status (status),
    INDEX idx_template_id (template_id),
    INDEX idx_inspector_id (inspector_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='检查会话表(V4聚合根)';

-- =====================================================
-- 2. Class Inspection Records (班级检查记录表 - 独立聚合根)
-- =====================================================
CREATE TABLE IF NOT EXISTS class_inspection_records (
    id                  BIGINT          PRIMARY KEY                          COMMENT '雪花ID',
    session_id          BIGINT          NOT NULL                            COMMENT '检查会话ID',
    class_id            BIGINT          NOT NULL                            COMMENT '班级ID',
    class_name          VARCHAR(64)                                         COMMENT '班级名称',
    org_unit_id         BIGINT                                              COMMENT '所属组织单元ID(系部)',
    org_unit_name       VARCHAR(64)                                         COMMENT '组织单元名称',
    base_score          INT             DEFAULT 100                         COMMENT '基础分',
    total_deduction     DECIMAL(10,2)   DEFAULT 0                           COMMENT '总扣分',
    bonus_score         DECIMAL(10,2)   DEFAULT 0                           COMMENT '加分',
    final_score         DECIMAL(10,2)   DEFAULT 0                           COMMENT '最终得分',
    status              VARCHAR(32)     NOT NULL DEFAULT 'PENDING'          COMMENT '状态: PENDING/RECORDING/COMPLETED',
    remarks             TEXT                                                COMMENT '备注',
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP  COMMENT '创建时间',
    updated_at          DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted             INT             DEFAULT 0                           COMMENT '逻辑删除: 0-未删除 1-已删除',

    UNIQUE KEY uk_session_class (session_id, class_id, deleted),
    INDEX idx_session_id (session_id),
    INDEX idx_class_id (class_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='班级检查记录表(V4独立聚合根)';

-- =====================================================
-- 3. Checklist Responses (检查清单响应表)
-- =====================================================
CREATE TABLE IF NOT EXISTS checklist_responses (
    id                  BIGINT          PRIMARY KEY                          COMMENT '雪花ID',
    session_id          BIGINT          NOT NULL                            COMMENT '检查会话ID',
    class_record_id     BIGINT          NOT NULL                            COMMENT '班级检查记录ID',
    checklist_item_id   BIGINT          NOT NULL                            COMMENT '模板检查项ID',
    item_name           VARCHAR(128)                                        COMMENT '检查项名称',
    category_name       VARCHAR(64)                                         COMMENT '所属分类名称',
    result              VARCHAR(16)     NOT NULL DEFAULT 'NA'               COMMENT '检查结果: PASS/FAIL/NA',
    auto_deduction      DECIMAL(10,2)   DEFAULT 0                           COMMENT 'FAIL时自动计算的扣分',
    inspector_note      VARCHAR(512)                                        COMMENT '检查人备注',
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP  COMMENT '创建时间',
    updated_at          DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX idx_session_id (session_id),
    INDEX idx_class_record_id (class_record_id),
    INDEX idx_checklist_item_id (checklist_item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='检查清单响应表(V4)';

-- =====================================================
-- 4. Inspection Deductions (扣分明细表 - V4独立表)
-- =====================================================
CREATE TABLE IF NOT EXISTS inspection_deductions (
    id                  BIGINT          PRIMARY KEY                          COMMENT '雪花ID',
    session_id          BIGINT          NOT NULL                            COMMENT '检查会话ID',
    class_record_id     BIGINT          NOT NULL                            COMMENT '班级检查记录ID',
    deduction_item_id   BIGINT                                              COMMENT '扣分项ID(引用模板扣分项，自由扣分时可为空)',
    item_name           VARCHAR(128)    NOT NULL                            COMMENT '扣分项名称',
    category_name       VARCHAR(64)                                         COMMENT '所属分类名称',
    space_type          VARCHAR(32)                                         COMMENT '空间类型: DORMITORY/CLASSROOM/NONE',
    space_id            BIGINT                                              COMMENT '物理空间ID(宿舍/教室)',
    space_name          VARCHAR(128)                                        COMMENT '空间显示名称',
    student_ids         JSON                                                COMMENT '涉及学生ID数组',
    student_names       JSON                                                COMMENT '涉及学生姓名数组',
    person_count        INT             DEFAULT 0                           COMMENT '涉及人数',
    deduction_amount    DECIMAL(10,2)   NOT NULL DEFAULT 0                  COMMENT '扣分数值',
    input_source        VARCHAR(32)                                         COMMENT '录入来源: CHECKLIST_FAIL/FREE_DEDUCTION/SPACE_RESOLVED/PERSON_RESOLVED',
    remark              VARCHAR(512)                                        COMMENT '备注说明',
    evidence_urls       JSON                                                COMMENT '佐证照片URL数组',
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP  COMMENT '创建时间',
    updated_at          DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted             INT             DEFAULT 0                           COMMENT '逻辑删除: 0-未删除 1-已删除',

    INDEX idx_session_id (session_id),
    INDEX idx_class_record_id (class_record_id),
    INDEX idx_space_id (space_id),
    INDEX idx_deduction_item_id (deduction_item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='扣分明细表(V4独立表)';
