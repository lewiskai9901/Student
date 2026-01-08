-- ============================================
-- 版本: V1.0.6
-- 描述: 重构检查记录设计,取消CheckRecord中间层
-- 作者: Claude Code
-- 日期: 2024-10-29
-- ============================================

-- ============================================
-- 第一部分: 增强 daily_checks 表
-- ============================================

-- 1. 添加检查执行相关字段
ALTER TABLE daily_checks
ADD COLUMN checker_id BIGINT COMMENT '检查人ID' AFTER description,
ADD COLUMN checker_name VARCHAR(100) COMMENT '检查人姓名' AFTER checker_id;

-- 2. 添加统计字段(实时汇总)
ALTER TABLE daily_checks
ADD COLUMN total_deduct_score DECIMAL(10,2) DEFAULT 0 COMMENT '总扣分' AFTER checker_name,
ADD COLUMN detail_count INT DEFAULT 0 COMMENT '明细条数' AFTER total_deduct_score;

-- 3. 添加申诉管理字段
ALTER TABLE daily_checks
ADD COLUMN appeal_status TINYINT DEFAULT 0 COMMENT '0=无申诉 1=有待审核 2=已处理' AFTER detail_count,
ADD COLUMN appeal_count INT DEFAULT 0 COMMENT '申诉数量' AFTER appeal_status;

-- 4. 细化状态字段
ALTER TABLE daily_checks
MODIFY COLUMN status TINYINT DEFAULT 0 COMMENT '0=未开始 1=进行中 2=已提交 3=已发布';

-- 5. 添加索引
ALTER TABLE daily_checks
ADD INDEX idx_checker_id (checker_id);

-- ============================================
-- 第二部分: 创建新的扣分明细表
-- ============================================

CREATE TABLE daily_check_details (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,

  -- 关联关系
  check_id BIGINT NOT NULL COMMENT '日常检查ID',
  category_id BIGINT NOT NULL COMMENT '检查类别ID',
  class_id BIGINT NOT NULL COMMENT '班级ID',

  -- 扣分项信息
  deduction_item_id BIGINT NOT NULL COMMENT '扣分项ID',
  deduction_item_name VARCHAR(200) COMMENT '扣分项名称(冗余)',
  deduct_mode TINYINT COMMENT '扣分模式 1=固定 2=按人数 3=区间',

  -- 关联对象(宿舍/教室)
  link_type TINYINT DEFAULT 0 COMMENT '0=无关联 1=宿舍 2=教室',
  link_id BIGINT COMMENT '关联对象ID(宿舍ID或教室ID)',
  link_no VARCHAR(100) COMMENT '关联对象编号',

  -- 扣分计算
  deduct_score DECIMAL(10,2) NOT NULL COMMENT '实际扣分',
  person_count INT COMMENT '违规人数(模式2)',

  -- 混寝分摊(仅宿舍关联时使用)
  total_students INT COMMENT '宿舍总人数',
  class_students INT COMMENT '该班在宿舍的人数',
  score_ratio DECIMAL(5,4) COMMENT '分数分摊比例',

  -- 检查信息
  checker_id BIGINT COMMENT '检查人ID',
  check_time DATETIME COMMENT '检查时间',
  description VARCHAR(1000) COMMENT '扣分说明',
  images JSON COMMENT '证据图片JSON数组',

  -- 申诉修订
  is_revised TINYINT DEFAULT 0 COMMENT '是否已修订 0=否 1=是',
  original_score DECIMAL(10,2) COMMENT '原始扣分',
  revised_score DECIMAL(10,2) COMMENT '修订后扣分',
  revision_reason VARCHAR(500) COMMENT '修订原因',

  -- 时间戳
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,

  -- 索引优化
  KEY idx_check_id (check_id),
  KEY idx_category_class (category_id, class_id),
  KEY idx_class_id (class_id),
  KEY idx_link (link_type, link_id),
  KEY idx_deduction_item (deduction_item_id),
  KEY idx_check_time (check_time),

  -- 外键约束
  CONSTRAINT fk_detail_check FOREIGN KEY (check_id)
    REFERENCES daily_checks(id) ON DELETE CASCADE,
  CONSTRAINT fk_detail_deduction FOREIGN KEY (deduction_item_id)
    REFERENCES deduction_items(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='检查扣分明细表(重构版)';

-- ============================================
-- 第三部分: 创建申诉表
-- ============================================

CREATE TABLE daily_check_appeals (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,

  -- 关联
  check_id BIGINT NOT NULL COMMENT '日常检查ID',
  detail_id BIGINT NOT NULL COMMENT '明细ID',
  class_id BIGINT NOT NULL COMMENT '班级ID',

  -- 申诉内容
  appeal_reason VARCHAR(2000) NOT NULL COMMENT '申诉理由',
  appeal_images JSON COMMENT '申诉证据图片',

  -- 申诉人信息
  appellant_id BIGINT NOT NULL COMMENT '申诉人ID',
  appellant_name VARCHAR(100) COMMENT '申诉人姓名',
  appellant_role VARCHAR(50) COMMENT '申诉人角色(班长/学生)',
  appeal_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '申诉时间',

  -- 审核信息
  status TINYINT DEFAULT 0 COMMENT '0=待审核 1=通过 2=驳回',
  reviewer_id BIGINT COMMENT '审核人ID',
  reviewer_name VARCHAR(100) COMMENT '审核人姓名',
  review_time DATETIME COMMENT '审核时间',
  review_result VARCHAR(2000) COMMENT '审核意见',

  -- 修订信息
  original_score DECIMAL(10,2) COMMENT '原扣分',
  revised_score DECIMAL(10,2) COMMENT '修订后扣分',

  -- 时间戳
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,

  KEY idx_check_id (check_id),
  KEY idx_detail_id (detail_id),
  KEY idx_status (status),
  KEY idx_class_id (class_id),
  KEY idx_appeal_time (appeal_time),

  CONSTRAINT fk_appeal_check FOREIGN KEY (check_id)
    REFERENCES daily_checks(id) ON DELETE CASCADE,
  CONSTRAINT fk_appeal_detail FOREIGN KEY (detail_id)
    REFERENCES daily_check_details(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='检查申诉表';

-- ============================================
-- 第四部分: 数据迁移(从旧表迁移到新表)
-- ============================================

-- 1. 迁移 check_record_details 到 daily_check_details
INSERT INTO daily_check_details (
  check_id,
  category_id,
  class_id,
  deduction_item_id,
  deduction_item_name,
  deduct_mode,
  link_type,
  link_id,
  link_no,
  deduct_score,
  person_count,
  total_students,
  class_students,
  score_ratio,
  checker_id,
  check_time,
  description,
  images,
  created_at,
  updated_at
)
SELECT
  crd.check_id,
  crd.category_id,
  crd.class_id,
  crd.deduction_item_id,
  crd.deduction_item_name,
  crd.deduct_mode,
  crd.link_type,
  CASE
    WHEN crd.link_type = 1 THEN crd.dormitory_id
    WHEN crd.link_type = 2 THEN crd.classroom_id
    ELSE NULL
  END AS link_id,
  CASE
    WHEN crd.link_type = 1 THEN crd.dormitory_no
    WHEN crd.link_type = 2 THEN crd.classroom_no
    ELSE NULL
  END AS link_no,
  crd.deduct_score,
  crd.person_count,
  crd.total_students,
  crd.class_students,
  crd.score_ratio,
  crd.checker_id,
  crd.check_time,
  crd.description,
  crd.images,
  crd.created_at,
  crd.updated_at
FROM check_record_details crd
WHERE crd.deleted = 0;

-- 2. 更新 daily_checks 的统计信息
UPDATE daily_checks dc
SET
  total_deduct_score = (
    SELECT COALESCE(SUM(deduct_score), 0)
    FROM daily_check_details dcd
    WHERE dcd.check_id = dc.id AND dcd.deleted = 0
  ),
  detail_count = (
    SELECT COUNT(*)
    FROM daily_check_details dcd
    WHERE dcd.check_id = dc.id AND dcd.deleted = 0
  ),
  checker_id = (
    SELECT checker_id
    FROM check_records cr
    WHERE cr.check_id = dc.id
    LIMIT 1
  ),
  checker_name = (
    SELECT checker_name
    FROM check_records cr
    WHERE cr.check_id = dc.id
    LIMIT 1
  ),
  appeal_status = (
    SELECT CASE
      WHEN MAX(appeal_status) = 2 THEN 2
      WHEN MAX(appeal_status) = 1 THEN 1
      ELSE 0
    END
    FROM check_records cr
    WHERE cr.check_id = dc.id
  )
WHERE EXISTS (SELECT 1 FROM check_records cr WHERE cr.check_id = dc.id);

-- ============================================
-- 第五部分: 创建触发器(自动维护统计信息)
-- ============================================

DELIMITER $$

-- 触发器1: 插入明细时自动更新汇总
CREATE TRIGGER trg_insert_detail_update_check
AFTER INSERT ON daily_check_details
FOR EACH ROW
BEGIN
  IF NEW.deleted = 0 THEN
    UPDATE daily_checks
    SET
      total_deduct_score = total_deduct_score + NEW.deduct_score,
      detail_count = detail_count + 1,
      updated_at = NOW()
    WHERE id = NEW.check_id;
  END IF;
END$$

-- 触发器2: 更新明细时自动调整汇总
CREATE TRIGGER trg_update_detail_update_check
AFTER UPDATE ON daily_check_details
FOR EACH ROW
BEGIN
  IF OLD.deleted = 0 AND NEW.deleted = 0 THEN
    -- 正常更新
    UPDATE daily_checks
    SET
      total_deduct_score = total_deduct_score - OLD.deduct_score + NEW.deduct_score,
      updated_at = NOW()
    WHERE id = NEW.check_id;
  ELSEIF OLD.deleted = 0 AND NEW.deleted = 1 THEN
    -- 软删除
    UPDATE daily_checks
    SET
      total_deduct_score = total_deduct_score - OLD.deduct_score,
      detail_count = detail_count - 1,
      updated_at = NOW()
    WHERE id = NEW.check_id;
  ELSEIF OLD.deleted = 1 AND NEW.deleted = 0 THEN
    -- 恢复
    UPDATE daily_checks
    SET
      total_deduct_score = total_deduct_score + NEW.deduct_score,
      detail_count = detail_count + 1,
      updated_at = NOW()
    WHERE id = NEW.check_id;
  END IF;
END$$

-- 触发器3: 删除明细时自动调整汇总
CREATE TRIGGER trg_delete_detail_update_check
AFTER DELETE ON daily_check_details
FOR EACH ROW
BEGIN
  IF OLD.deleted = 0 THEN
    UPDATE daily_checks
    SET
      total_deduct_score = total_deduct_score - OLD.deduct_score,
      detail_count = detail_count - 1,
      updated_at = NOW()
    WHERE id = OLD.check_id;
  END IF;
END$$

-- 触发器4: 申诉通过时自动更新明细
CREATE TRIGGER trg_appeal_approved_update_detail
AFTER UPDATE ON daily_check_appeals
FOR EACH ROW
BEGIN
  IF NEW.status = 1 AND OLD.status = 0 AND NEW.revised_score IS NOT NULL THEN
    -- 更新明细
    UPDATE daily_check_details
    SET
      is_revised = 1,
      original_score = deduct_score,
      revised_score = NEW.revised_score,
      deduct_score = NEW.revised_score,
      revision_reason = NEW.review_result,
      updated_at = NOW()
    WHERE id = NEW.detail_id AND deleted = 0;

    -- 更新检查申诉状态
    UPDATE daily_checks
    SET appeal_count = appeal_count + 1,
        updated_at = NOW()
    WHERE id = NEW.check_id;
  END IF;
END$$

DELIMITER ;

-- ============================================
-- 第六部分: 备份和标记旧表
-- ============================================

-- 备份旧表(保留数据以备回滚)
CREATE TABLE check_records_backup AS SELECT * FROM check_records;
CREATE TABLE check_record_details_backup AS SELECT * FROM check_record_details;

-- 标记旧表(添加注释说明已废弃)
ALTER TABLE check_records COMMENT='检查记录表(已废弃,V1.0.6后使用daily_checks)';
ALTER TABLE check_record_details COMMENT='检查明细表(已废弃,V1.0.6后使用daily_check_details)';

-- ============================================
-- 第七部分: 验证数据完整性
-- ============================================

-- 验证迁移数据量
SELECT
  'check_record_details' AS table_name,
  COUNT(*) AS old_count,
  (SELECT COUNT(*) FROM daily_check_details) AS new_count,
  CASE
    WHEN COUNT(*) = (SELECT COUNT(*) FROM daily_check_details WHERE deleted = 0)
    THEN 'OK'
    ELSE 'ERROR'
  END AS migration_status
FROM check_record_details
WHERE deleted = 0;

-- 验证统计数据准确性
SELECT
  dc.id,
  dc.check_name,
  dc.total_deduct_score AS computed_score,
  (SELECT SUM(deduct_score) FROM daily_check_details dcd WHERE dcd.check_id = dc.id AND dcd.deleted = 0) AS actual_score,
  dc.detail_count AS computed_count,
  (SELECT COUNT(*) FROM daily_check_details dcd WHERE dcd.check_id = dc.id AND dcd.deleted = 0) AS actual_count,
  CASE
    WHEN dc.total_deduct_score = (SELECT SUM(deduct_score) FROM daily_check_details dcd WHERE dcd.check_id = dc.id AND dcd.deleted = 0)
    THEN 'OK'
    ELSE 'ERROR'
  END AS validation_status
FROM daily_checks dc
WHERE EXISTS (SELECT 1 FROM daily_check_details dcd WHERE dcd.check_id = dc.id)
LIMIT 10;

-- ============================================
-- 完成
-- ============================================
