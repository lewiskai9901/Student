-- =============================================
-- V8.5.0: V6检查系统完善 - 检查执行与核心表
-- =============================================

-- 1. 创建实体类型配置表
CREATE TABLE IF NOT EXISTS entity_types (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    category ENUM('ORGANIZATION', 'SPACE', 'USER') NOT NULL COMMENT '分类',
    type_code VARCHAR(50) NOT NULL COMMENT '类型代码',
    type_name VARCHAR(100) NOT NULL COMMENT '类型名称',
    parent_type_code VARCHAR(50) COMMENT '父类型代码',
    is_leaf TINYINT(1) DEFAULT 0 COMMENT '是否叶子节点',
    icon VARCHAR(50) COMMENT '图标',
    color VARCHAR(20) COMMENT '颜色',
    attributes_schema JSON COMMENT '属性定义(JSON Schema)',
    weight_attribute VARCHAR(50) COMMENT '加权属性',
    sort_order INT DEFAULT 0 COMMENT '排序',
    is_enabled TINYINT(1) DEFAULT 1 COMMENT '是否启用',
    is_system TINYINT(1) DEFAULT 0 COMMENT '是否系统预置',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_type_code (type_code),
    INDEX idx_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='实体类型配置表';

-- 预置实体类型
INSERT INTO entity_types (category, type_code, type_name, parent_type_code, is_leaf, icon, weight_attribute, sort_order, is_system) VALUES
-- 组织类型
('ORGANIZATION', 'SCHOOL', '学校', NULL, 0, 'school', NULL, 1, 1),
('ORGANIZATION', 'DEPARTMENT', '系部', 'SCHOOL', 0, 'building', 'member_count', 2, 1),
('ORGANIZATION', 'CLASS', '班级', 'DEPARTMENT', 1, 'users', 'member_count', 3, 1),
-- 场所类型
('SPACE', 'BUILDING', '楼栋', NULL, 0, 'building', NULL, 10, 1),
('SPACE', 'DORMITORY', '宿舍', 'BUILDING', 1, 'home', 'capacity', 11, 1),
('SPACE', 'CLASSROOM', '教室', 'BUILDING', 1, 'book', 'capacity', 12, 1),
('SPACE', 'OFFICE', '办公室', 'BUILDING', 1, 'briefcase', 'area', 13, 1),
-- 用户类型
('USER', 'STUDENT', '学生', NULL, 1, 'user', NULL, 20, 1),
('USER', 'TEACHER', '教师', NULL, 1, 'user-tie', NULL, 21, 1);

-- 2. 创建动态分组表
CREATE TABLE IF NOT EXISTS entity_groups (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    group_code VARCHAR(50) NOT NULL COMMENT '分组代码',
    group_name VARCHAR(100) NOT NULL COMMENT '分组名称',
    description VARCHAR(500) COMMENT '描述',
    group_type ENUM('DYNAMIC', 'STATIC') DEFAULT 'DYNAMIC' COMMENT '分组类型',
    entity_category ENUM('ORGANIZATION', 'SPACE', 'USER') NOT NULL COMMENT '实体分类',
    entity_type_code VARCHAR(50) COMMENT '实体类型代码',
    filter_conditions JSON COMMENT '筛选条件(DYNAMIC)',
    static_member_ids JSON COMMENT '静态成员ID(STATIC)',
    cached_member_ids JSON COMMENT '缓存的成员ID',
    cached_member_count INT DEFAULT 0 COMMENT '缓存的成员数量',
    cached_at DATETIME COMMENT '缓存时间',
    auto_refresh TINYINT(1) DEFAULT 1 COMMENT '自动刷新',
    refresh_interval ENUM('HOURLY', 'DAILY', 'WEEKLY') DEFAULT 'DAILY' COMMENT '刷新间隔',
    org_unit_id BIGINT COMMENT '所属组织',
    created_by BIGINT COMMENT '创建人',
    is_enabled TINYINT(1) DEFAULT 1 COMMENT '是否启用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_group_code (group_code),
    INDEX idx_entity_category (entity_category),
    INDEX idx_org_unit (org_unit_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='动态分组表';

-- 3. 创建打分策略配置表
CREATE TABLE IF NOT EXISTS scoring_strategies (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    strategy_code VARCHAR(50) NOT NULL COMMENT '策略代码',
    strategy_name VARCHAR(100) NOT NULL COMMENT '策略名称',
    description VARCHAR(500) COMMENT '描述',
    strategy_type ENUM('DEDUCTION', 'ADDITION', 'BASE_SCORE', 'RATING', 'GRADE', 'PASS_FAIL', 'CHECKLIST') NOT NULL COMMENT '策略类型',
    config JSON COMMENT '配置',
    completion_rules JSON COMMENT '完成规则',
    result_format VARCHAR(100) COMMENT '结果格式',
    is_system TINYINT(1) DEFAULT 0 COMMENT '是否系统预置',
    is_enabled TINYINT(1) DEFAULT 1 COMMENT '是否启用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_strategy_code (strategy_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='打分策略配置表';

-- 预置打分策略
INSERT INTO scoring_strategies (strategy_code, strategy_name, strategy_type, config, result_format, is_system) VALUES
('PURE_DEDUCTION', '纯扣分制', 'DEDUCTION', '{"accumulate": true}', '累计扣{total}分', 1),
('BASE_100', '百分制', 'BASE_SCORE', '{"base": 100, "min": 0, "allow_negative": false}', '{score}分', 1),
('RATING_5', '五分制', 'RATING', '{"min": 1, "max": 5, "step": 1}', '{score}/5分', 1),
('RATING_10', '十分制', 'RATING', '{"min": 1, "max": 10, "step": 1}', '{score}/10分', 1),
('GRADE_ABCD', '四级评级', 'GRADE', '{"grades": [{"code":"A","name":"优秀","min_score":90},{"code":"B","name":"良好","min_score":75},{"code":"C","name":"合格","min_score":60},{"code":"D","name":"不合格","min_score":0}]}', '{grade}({grade_name})', 1),
('PASS_FAIL', '通过制', 'PASS_FAIL', '{}', '{result}', 1),
('CHECKLIST', '清单制', 'CHECKLIST', '{}', '{checked}/{total}项({rate}%)', 1);

-- 4. 创建检查明细表（增强版，支持多种打分模式）
CREATE TABLE IF NOT EXISTS inspection_details (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    target_id BIGINT NOT NULL COMMENT '检查目标ID',

    -- 检查项信息
    category_id BIGINT COMMENT '类别ID',
    category_code VARCHAR(50) COMMENT '类别代码',
    category_name VARCHAR(100) COMMENT '类别名称',
    item_id BIGINT COMMENT '检查项ID',
    item_code VARCHAR(50) COMMENT '检查项代码',
    item_name VARCHAR(100) NOT NULL COMMENT '检查项名称',

    -- 作用范围
    scope ENUM('WHOLE', 'INDIVIDUAL') NOT NULL DEFAULT 'WHOLE' COMMENT '作用范围',
    individual_type VARCHAR(50) COMMENT '个体类型(USER/SPACE)',
    individual_id BIGINT COMMENT '个体ID',
    individual_name VARCHAR(100) COMMENT '个体名称',

    -- 打分
    scoring_mode VARCHAR(20) NOT NULL COMMENT '打分模式',
    score DECIMAL(6,2) NOT NULL COMMENT '分值(扣分为负,加分为正)',
    quantity INT DEFAULT 1 COMMENT '数量',
    total_score DECIMAL(6,2) COMMENT '总分(score*quantity)',

    -- 评级结果(GRADE模式)
    grade_code VARCHAR(10) COMMENT '等级代码',
    grade_name VARCHAR(50) COMMENT '等级名称',

    -- 清单结果(CHECKLIST模式)
    checklist_checked TINYINT(1) COMMENT '是否勾选',

    -- 备注和证据
    remark VARCHAR(500) COMMENT '备注',
    evidence_ids JSON COMMENT '证据ID列表',

    -- 审计
    created_by BIGINT COMMENT '创建人',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_target_id (target_id),
    INDEX idx_item_id (item_id),
    INDEX idx_individual (individual_type, individual_id),
    FOREIGN KEY (target_id) REFERENCES inspection_targets(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='V6检查明细表';

-- 5. 创建证据附件表
CREATE TABLE IF NOT EXISTS inspection_evidences (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    detail_id BIGINT COMMENT '检查明细ID',
    target_id BIGINT COMMENT '检查目标ID(整体证据)',

    -- 文件信息
    file_name VARCHAR(200) NOT NULL COMMENT '文件名',
    file_path VARCHAR(500) NOT NULL COMMENT '文件路径',
    file_url VARCHAR(500) COMMENT '文件URL',
    file_size BIGINT COMMENT '文件大小(字节)',
    file_type VARCHAR(50) COMMENT '文件类型',

    -- 元数据
    latitude DECIMAL(10,7) COMMENT '纬度',
    longitude DECIMAL(10,7) COMMENT '经度',

    -- 审计
    upload_by BIGINT COMMENT '上传人',
    upload_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_detail_id (detail_id),
    INDEX idx_target_id (target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='V6证据附件表';

-- 6. 为inspection_targets添加检查深度相关字段
ALTER TABLE inspection_targets
ADD COLUMN IF NOT EXISTS target_path JSON COMMENT '目标路径(层级名称)',
ADD COLUMN IF NOT EXISTS target_path_ids JSON COMMENT '目标路径(层级ID)',
ADD COLUMN IF NOT EXISTS depth_level INT DEFAULT 0 COMMENT '层级深度',
ADD COLUMN IF NOT EXISTS scoring_mode VARCHAR(20) COMMENT '打分模式',
ADD COLUMN IF NOT EXISTS result_data JSON COMMENT '检查结果数据';

-- 7. 为inspection_projects添加检查深度配置
ALTER TABLE inspection_projects
ADD COLUMN IF NOT EXISTS inspection_depth JSON COMMENT '检查深度配置',
ADD COLUMN IF NOT EXISTS base_score DECIMAL(6,2) DEFAULT 100.00 COMMENT '基础分',
ADD COLUMN IF NOT EXISTS min_score DECIMAL(6,2) DEFAULT 0.00 COMMENT '最低分',
ADD COLUMN IF NOT EXISTS allow_negative TINYINT(1) DEFAULT 0 COMMENT '允许负分',
ADD COLUMN IF NOT EXISTS score_formula VARCHAR(500) COMMENT '分数计算公式',
ADD COLUMN IF NOT EXISTS category_weights JSON COMMENT '类别权重配置';

-- 8. 创建索引优化查询
CREATE INDEX IF NOT EXISTS idx_details_scoring_mode ON inspection_details(scoring_mode);
CREATE INDEX IF NOT EXISTS idx_targets_depth ON inspection_targets(depth_level);
