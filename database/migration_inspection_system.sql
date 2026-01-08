-- =====================================================
-- 量化检查系统数据库迁移脚本
-- 创建新的四层结构表
-- =====================================================

-- 1. 备份旧表
-- RENAME TABLE quantification_records TO quantification_records_backup;

-- 2. 创建检查批次表
CREATE TABLE IF NOT EXISTS inspection_sessions (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  session_code VARCHAR(50) NOT NULL COMMENT '检查批次编号',
  inspection_date DATE NOT NULL COMMENT '检查日期',
  inspection_time TIME COMMENT '检查时间',
  inspector_id BIGINT NOT NULL COMMENT '检查人ID',
  inspector_name VARCHAR(50) NOT NULL COMMENT '检查人姓名',
  grade_id BIGINT COMMENT '年级ID(可选,用于筛选)',
  grade_name VARCHAR(50) COMMENT '年级名称',
  total_targets INT DEFAULT 0 COMMENT '检查目标总数',
  total_deductions DECIMAL(10,2) DEFAULT 0 COMMENT '总扣分',
  status TINYINT DEFAULT 1 COMMENT '状态:1草稿 2待审核 3已发布',
  reviewer_id BIGINT COMMENT '审核人ID',
  reviewer_name VARCHAR(50) COMMENT '审核人姓名',
  reviewed_at TIMESTAMP NULL COMMENT '审核时间',
  published_at TIMESTAMP NULL COMMENT '发布时间',
  remarks TEXT COMMENT '备注',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX idx_date (inspection_date),
  INDEX idx_inspector (inspector_id),
  INDEX idx_status (status),
  INDEX idx_grade (grade_id),
  UNIQUE KEY uk_session_code (session_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='检查批次表';

-- 3. 创建检查目标表
CREATE TABLE IF NOT EXISTS inspection_targets (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  session_id BIGINT NOT NULL COMMENT '检查批次ID',
  target_type VARCHAR(20) DEFAULT 'class' COMMENT '目标类型:class-班级,dorm-宿舍',
  target_id BIGINT NOT NULL COMMENT '目标ID(班级ID或宿舍ID)',
  target_name VARCHAR(100) NOT NULL COMMENT '目标名称',
  total_deductions DECIMAL(10,2) DEFAULT 0 COMMENT '该目标总扣分',
  category_count INT DEFAULT 0 COMMENT '检查类别数',
  item_count INT DEFAULT 0 COMMENT '扣分项数量',
  remarks VARCHAR(500) COMMENT '备注',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX idx_session (session_id),
  INDEX idx_target (target_id),
  INDEX idx_target_type (target_type),
  FOREIGN KEY (session_id) REFERENCES inspection_sessions(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='检查目标表';

-- 4. 创建检查类别表
CREATE TABLE IF NOT EXISTS inspection_categories (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  target_id BIGINT NOT NULL COMMENT '检查目标ID',
  session_id BIGINT NOT NULL COMMENT '检查批次ID',
  type_id BIGINT NOT NULL COMMENT '量化类型ID',
  type_name VARCHAR(50) NOT NULL COMMENT '类别名称',
  type_code VARCHAR(30) NOT NULL COMMENT '类别代码',
  category_deductions DECIMAL(10,2) DEFAULT 0 COMMENT '该类别总扣分',
  item_count INT DEFAULT 0 COMMENT '扣分项数量',
  evidence_images JSON COMMENT '证据图片',
  remarks VARCHAR(500) COMMENT '备注',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX idx_target (target_id),
  INDEX idx_session (session_id),
  INDEX idx_type (type_id),
  FOREIGN KEY (target_id) REFERENCES inspection_targets(id) ON DELETE CASCADE,
  FOREIGN KEY (session_id) REFERENCES inspection_sessions(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='检查类别表';

-- 5. 创建扣分项明细表
CREATE TABLE IF NOT EXISTS inspection_deduction_items (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  category_id BIGINT NOT NULL COMMENT '检查类别ID',
  target_id BIGINT NOT NULL COMMENT '检查目标ID',
  session_id BIGINT NOT NULL COMMENT '检查批次ID',
  item_name VARCHAR(100) NOT NULL COMMENT '扣分项名称',
  deduct_score DECIMAL(10,2) NOT NULL COMMENT '扣分分值',
  person_count INT DEFAULT 0 COMMENT '涉及人数(适用于按人头扣分)',
  deduct_reason VARCHAR(500) COMMENT '扣分原因详情',
  evidence_images JSON COMMENT '证据图片',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX idx_category (category_id),
  INDEX idx_target (target_id),
  INDEX idx_session (session_id),
  FOREIGN KEY (category_id) REFERENCES inspection_categories(id) ON DELETE CASCADE,
  FOREIGN KEY (target_id) REFERENCES inspection_targets(id) ON DELETE CASCADE,
  FOREIGN KEY (session_id) REFERENCES inspection_sessions(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='扣分项明细表';

-- 6. 创建批次编号生成序列(用于生成唯一的检查批次编号)
CREATE TABLE IF NOT EXISTS inspection_session_sequence (
  id INT PRIMARY KEY AUTO_INCREMENT,
  stub CHAR(1) NOT NULL DEFAULT '',
  UNIQUE KEY uk_stub (stub)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='检查批次编号序列表';

-- 初始化序列
INSERT IGNORE INTO inspection_session_sequence (stub) VALUES ('');

-- =====================================================
-- 插入测试数据
-- =====================================================

-- 测试数据:创建一次检查批次
INSERT INTO inspection_sessions (
  session_code, inspection_date, inspection_time,
  inspector_id, inspector_name, grade_id, grade_name,
  total_targets, total_deductions, status
) VALUES (
  'INS-20251031-001', '2025-10-31', '08:30:00',
  1, 'Zhang Teacher', NULL, 'Grade 1',
  3, 16.00, 2
);

SET @session_id = LAST_INSERT_ID();

-- 测试数据:添加检查目标(班级)
INSERT INTO inspection_targets (
  session_id, target_type, target_id, target_name,
  total_deductions, category_count, item_count
) VALUES
  (@session_id, 'class', 1, 'Class 1-1', 8.00, 2, 3),
  (@session_id, 'class', 2, 'Class 1-2', 5.00, 1, 2),
  (@session_id, 'class', 3, 'Class 1-3', 3.00, 1, 1);

SET @target1_id = LAST_INSERT_ID();
SET @target2_id = @target1_id + 1;
SET @target3_id = @target1_id + 2;

-- 测试数据:高一(1)班的检查类别
INSERT INTO inspection_categories (
  target_id, session_id, type_id, type_name, type_code,
  category_deductions, item_count
) VALUES
  (@target1_id, @session_id, 1, 'Classroom Hygiene', 'classroom_hygiene', 5.00, 2),
  (@target1_id, @session_id, 3, 'Discipline Check', 'discipline_check', 3.00, 1);

SET @category1_id = LAST_INSERT_ID();
SET @category2_id = @category1_id + 1;

-- 测试数据:教室卫生检查的扣分项
INSERT INTO inspection_deduction_items (
  category_id, target_id, session_id,
  item_name, deduct_score, person_count, deduct_reason
) VALUES
  (@category1_id, @target1_id, @session_id, 'Trash not cleaned', 3.00, 0, 'Garbage on classroom floor'),
  (@category1_id, @target1_id, @session_id, 'Blackboard not wiped', 2.00, 0, 'Writing remains on blackboard');

-- 测试数据:纪律检查的扣分项
INSERT INTO inspection_deduction_items (
  category_id, target_id, session_id,
  item_name, deduct_score, person_count, deduct_reason
) VALUES
  (@category2_id, @target1_id, @session_id, 'Sleeping in class', 3.00, 1, 'Found 1 student sleeping');

-- 测试数据:高一(2)班的检查类别和扣分项
INSERT INTO inspection_categories (
  target_id, session_id, type_id, type_name, type_code,
  category_deductions, item_count
) VALUES
  (@target2_id, @session_id, 2, 'Morning Exercise', 'morning_exercise', 5.00, 2);

SET @category3_id = LAST_INSERT_ID();

INSERT INTO inspection_deduction_items (
  category_id, target_id, session_id,
  item_name, deduct_score, person_count, deduct_reason
) VALUES
  (@category3_id, @target2_id, @session_id, 'Absent', 4.00, 2, '2 students absent from morning exercise'),
  (@category3_id, @target2_id, @session_id, 'Messy line', 1.00, 0, 'Formation not neat');

-- 测试数据:高一(3)班的检查类别和扣分项
INSERT INTO inspection_categories (
  target_id, session_id, type_id, type_name, type_code,
  category_deductions, item_count
) VALUES
  (@target3_id, @session_id, 1, 'Classroom Hygiene', 'classroom_hygiene', 3.00, 1);

SET @category4_id = LAST_INSERT_ID();

INSERT INTO inspection_deduction_items (
  category_id, target_id, session_id,
  item_name, deduct_score, person_count, deduct_reason
) VALUES
  (@category4_id, @target3_id, @session_id, 'Messy podium', 3.00, 0, 'Items on podium not organized');

-- =====================================================
-- 查询验证
-- =====================================================

-- 查询检查批次列表
SELECT
  id, session_code, inspection_date, inspector_name,
  total_targets, total_deductions, status
FROM inspection_sessions
ORDER BY inspection_date DESC;

-- 查询某个批次的完整信息
SELECT
  s.session_code,
  s.inspection_date,
  s.inspector_name,
  t.target_name,
  c.type_name,
  i.item_name,
  i.deduct_score,
  i.deduct_reason
FROM inspection_sessions s
JOIN inspection_targets t ON s.id = t.session_id
JOIN inspection_categories c ON t.id = c.target_id
JOIN inspection_deduction_items i ON c.id = i.category_id
WHERE s.session_code = 'INS-20251031-001'
ORDER BY t.id, c.id, i.id;
