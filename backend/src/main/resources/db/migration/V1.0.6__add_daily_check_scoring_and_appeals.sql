-- ================================================================
-- 数据库迁移脚本 V1.0.6
-- 功能: 添加日常检查打分和申诉功能
-- 日期: 2025-10-30
-- 作者: Claude Code
-- ================================================================

-- ================================================================
-- 1. 修改 daily_checks 表 - 添加检查员和统计字段
-- ================================================================

-- 添加检查员相关字段
ALTER TABLE daily_checks
ADD COLUMN IF NOT EXISTS checker_id BIGINT NULL COMMENT '检查员ID' AFTER status,
ADD COLUMN IF NOT EXISTS checker_name VARCHAR(100) NULL COMMENT '检查员姓名' AFTER checker_id;

-- 添加统计字段
ALTER TABLE daily_checks
ADD COLUMN IF NOT EXISTS total_deduct_score DECIMAL(10,2) NULL DEFAULT 0.00 COMMENT '总扣分' AFTER checker_name,
ADD COLUMN IF NOT EXISTS detail_count INT NULL DEFAULT 0 COMMENT '明细条数' AFTER total_deduct_score,
ADD COLUMN IF NOT EXISTS appeal_status TINYINT NULL DEFAULT 0 COMMENT '申诉状态:0=无申诉,1=有待审核,2=已处理' AFTER detail_count,
ADD COLUMN IF NOT EXISTS appeal_count INT NULL DEFAULT 0 COMMENT '申诉数量' AFTER appeal_status;

-- 添加索引
ALTER TABLE daily_checks ADD INDEX IF NOT EXISTS idx_checker_id (checker_id);
ALTER TABLE daily_checks ADD INDEX IF NOT EXISTS idx_appeal_status (appeal_status);

-- ================================================================
-- 2. 创建 daily_check_details 表 - 日常检查扣分明细表
-- ================================================================

CREATE TABLE IF NOT EXISTS daily_check_details (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  check_id BIGINT NOT NULL COMMENT '日常检查ID',
  category_id BIGINT NOT NULL COMMENT '检查类别ID',
  class_id BIGINT NOT NULL COMMENT '班级ID',
  deduction_item_id BIGINT NOT NULL COMMENT '扣分项ID',
  deduction_item_name VARCHAR(200) NULL COMMENT '扣分项名称',
  deduct_mode TINYINT NULL DEFAULT 1 COMMENT '扣分模式:1=固定,2=按人数,3=区间',
  link_type TINYINT NULL DEFAULT 0 COMMENT '关联类型:0=无关联,1=学生,2=宿舍,3=教室',
  link_id BIGINT NULL COMMENT '关联对象ID',
  link_no VARCHAR(50) NULL COMMENT '关联对象编号',
  deduct_score DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '实际扣分',
  person_count INT NULL COMMENT '违规人数',
  total_students INT NULL COMMENT '宿舍总人数',
  class_students INT NULL COMMENT '该班在宿舍的人数',
  score_ratio DECIMAL(10,4) NULL COMMENT '分数分摊比例',
  checker_id BIGINT NULL COMMENT '检查人ID',
  check_time DATETIME NULL COMMENT '检查时间',
  description TEXT NULL COMMENT '扣分说明',
  images TEXT NULL COMMENT '证据图片JSON数组',
  is_revised TINYINT NULL DEFAULT 0 COMMENT '是否已修订:0=否,1=是',
  original_score DECIMAL(10,2) NULL COMMENT '原始扣分',
  revised_score DECIMAL(10,2) NULL COMMENT '修订后扣分',
  revision_reason VARCHAR(500) NULL COMMENT '修订原因',
  remark TEXT NULL COMMENT '备注',
  photo_urls TEXT NULL COMMENT '图片URLs',
  appeal_status TINYINT NULL DEFAULT 0 COMMENT '申诉状态:0=未申诉,1=申诉中,2=申诉通过,3=申诉驳回',
  created_by BIGINT NULL COMMENT '创建人ID',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT DEFAULT 0 COMMENT '逻辑删除:0=未删除,1=已删除',
  PRIMARY KEY (id),
  INDEX idx_check_id (check_id),
  INDEX idx_category_id (category_id),
  INDEX idx_class_id (class_id),
  INDEX idx_link (link_type, link_id),
  INDEX idx_appeal_status (appeal_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='日常检查扣分明细表';

-- ================================================================
-- 3. 创建 daily_check_appeals 表 - 日常检查申诉表
-- ================================================================

CREATE TABLE IF NOT EXISTS daily_check_appeals (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  check_id BIGINT NOT NULL COMMENT '检查ID',
  detail_id BIGINT NOT NULL COMMENT '明细ID',
  category_id BIGINT NULL COMMENT '检查类别ID',
  class_id BIGINT NOT NULL COMMENT '班级ID',
  original_score DECIMAL(10,2) NOT NULL COMMENT '原始扣分',
  appeal_reason TEXT NOT NULL COMMENT '申诉理由',
  appeal_user_id BIGINT NOT NULL COMMENT '申诉人ID',
  appeal_user_name VARCHAR(100) NOT NULL COMMENT '申诉人姓名',
  appeal_time DATETIME NOT NULL COMMENT '申诉时间',
  appeal_photo_urls TEXT NULL COMMENT '申诉图片URLs',
  status TINYINT NOT NULL DEFAULT 0 COMMENT '状态:0=待处理,1=处理中,2=通过,3=驳回',
  revised_score DECIMAL(10,2) NULL COMMENT '修订后扣分',
  review_opinion TEXT NULL COMMENT '审核意见',
  reviewer_id BIGINT NULL COMMENT '审核人ID',
  reviewer_name VARCHAR(100) NULL COMMENT '审核人姓名',
  review_time DATETIME NULL COMMENT '审核时间',
  created_by BIGINT NULL COMMENT '创建人ID',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT DEFAULT 0 COMMENT '逻辑删除:0=未删除,1=已删除',
  PRIMARY KEY (id),
  INDEX idx_check_id (check_id),
  INDEX idx_detail_id (detail_id),
  INDEX idx_class_id (class_id),
  INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='日常检查申诉表';

-- ================================================================
-- 4. 添加新的权限
-- ================================================================

-- 插入打分和申诉相关权限
INSERT INTO permissions (permission_code, permission_name, parent_id, permission_type, sort_order, status, permission_desc)
VALUES
('quantification:check:score', 'Daily Check Scoring', NULL, 2, 24, 1, 'Save and modify daily check scoring data'),
('quantification:appeal:view', 'View Appeals', NULL, 2, 30, 1, 'View daily check appeal information'),
('quantification:appeal:create', 'Create Appeal', NULL, 2, 31, 1, 'Create new check appeal'),
('quantification:appeal:review', 'Review Appeal', NULL, 2, 32, 1, 'Review check appeals'),
('quantification:appeal:withdraw', 'Withdraw Appeal', NULL, 2, 33, 1, 'Withdraw own submitted appeal')
ON DUPLICATE KEY UPDATE
permission_name = VALUES(permission_name),
permission_desc = VALUES(permission_desc);

-- ================================================================
-- 5. 将新权限分配给管理员角色
-- ================================================================

-- 将新权限分配给管理员角色(假设role_id=1是管理员)
INSERT INTO role_permissions (role_id, permission_id)
SELECT 1, id FROM permissions
WHERE permission_code IN (
  'quantification:check:score',
  'quantification:appeal:view',
  'quantification:appeal:create',
  'quantification:appeal:review',
  'quantification:appeal:withdraw'
)
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

-- ================================================================
-- 迁移完成
-- ================================================================
