-- V60: 项目层级 + 分数汇总表
-- 支持组合模板展开为父子项目树，分数逐层向上汇总

-- 项目增加父项目ID
ALTER TABLE insp_projects ADD COLUMN parent_project_id BIGINT NULL;
ALTER TABLE insp_projects ADD INDEX idx_parent_project (parent_project_id);

-- 项目分数汇总表
CREATE TABLE insp_project_scores (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    project_id BIGINT NOT NULL,
    cycle_date DATE NOT NULL,
    score DECIMAL(10,2) NULL,
    grade VARCHAR(10) NULL,
    target_count INT DEFAULT 0,
    detail JSON NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    INDEX idx_project_date (project_id, cycle_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 组织树得分 roll-up 表 (规模公平性 Stage 5)
-- 对 (project, cycle_date) 的已完成 submission 按所属 orgUnit 分组, 沿组织树
-- 自底向上用 MEAN (均值) 逐层滚动. org_unit_id 为业务主键, RepositoryImpl 显式 set.
CREATE TABLE org_unit_scores (
    id            BIGINT       PRIMARY KEY,
    tenant_id     BIGINT       NOT NULL DEFAULT 1,
    project_id    BIGINT       NOT NULL,
    org_unit_id   BIGINT       NOT NULL,
    cycle_date    DATE         NOT NULL,
    score         DECIMAL(8,2) NOT NULL,
    grade         VARCHAR(32)  NULL,
    child_count   INT          NOT NULL DEFAULT 0,
    source_count  INT          NOT NULL DEFAULT 0,
    deleted       TINYINT      NOT NULL DEFAULT 0,
    created_at    DATETIME     NOT NULL,
    updated_at    DATETIME     NULL,
    UNIQUE KEY uk_org_score (tenant_id, project_id, org_unit_id, cycle_date),
    KEY idx_org_score_project_date (project_id, cycle_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
