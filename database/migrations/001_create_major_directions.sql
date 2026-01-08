-- ================================================================
-- 专业方向体系重构 - 数据库迁移脚本
-- 创建时间: 2025-01-15
-- 说明: 支持一个专业多个培养层次(学制+技能等级)
-- ================================================================

-- 1. 创建专业方向表
CREATE TABLE IF NOT EXISTS major_directions (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  major_id BIGINT NOT NULL COMMENT '所属专业ID',
  direction_name VARCHAR(100) NOT NULL COMMENT '方向名称(如:3+2高级工)',
  direction_code VARCHAR(50) NOT NULL COMMENT '方向编码(如:CN_3P2_ADV)',

  -- 培养层次信息(灵活组合)
  education_system VARCHAR(20) NOT NULL COMMENT '学制类型(3+2/3年/5年/2+2)',
  skill_level VARCHAR(20) NOT NULL COMMENT '技能等级(中级工/高级工/预备技师/技师)',
  duration INT NOT NULL COMMENT '学制年数(3/4/5)',

  -- 特殊标记(不体现在名称中,只作属性)
  is_international TINYINT DEFAULT 0 COMMENT '是否国际班(0=否,1=是)',
  is_experimental TINYINT DEFAULT 0 COMMENT '是否实验班(0=否,1=是)',
  is_oriented TINYINT DEFAULT 0 COMMENT '是否定向培养(0=否,1=是)',
  oriented_company VARCHAR(200) COMMENT '定向企业名称',

  -- 教学信息
  curriculum_plan TEXT COMMENT '课程计划',
  training_goal TEXT COMMENT '培养目标',
  certificate_type VARCHAR(50) COMMENT '证书类型',
  employment_direction TEXT COMMENT '就业方向',

  -- 招生信息
  tuition_fee DECIMAL(10,2) COMMENT '学费/年',
  default_class_size INT DEFAULT 45 COMMENT '默认班级人数',
  min_enrollment INT COMMENT '最低招生人数',
  max_enrollment INT COMMENT '最高招生人数',

  -- 排序和状态
  sort_order INT DEFAULT 0 COMMENT '排序序号',
  status TINYINT DEFAULT 1 COMMENT '状态(0=停招,1=招生中)',
  remarks TEXT COMMENT '备注',

  -- 审计字段
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  created_by BIGINT COMMENT '创建人ID',
  updated_by BIGINT COMMENT '更新人ID',
  deleted TINYINT DEFAULT 0 COMMENT '删除标志(0=未删除,1=已删除)',

  -- 索引
  UNIQUE KEY uk_direction_code (direction_code),
  UNIQUE KEY uk_major_direction (major_id, education_system, skill_level, deleted),
  INDEX idx_major (major_id),
  INDEX idx_status (status, deleted),

  -- 外键
  CONSTRAINT fk_major_direction_major
    FOREIGN KEY (major_id) REFERENCES majors(id) ON DELETE CASCADE

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='专业方向表-同一专业的不同培养层次';

-- 2. 创建年级-专业方向关联表
CREATE TABLE IF NOT EXISTS grade_major_directions (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  grade_id BIGINT NOT NULL COMMENT '年级ID',
  major_direction_id BIGINT NOT NULL COMMENT '专业方向ID',

  -- 冗余字段(便于查询和统计,从major_direction继承)
  major_id BIGINT NOT NULL COMMENT '专业ID',
  major_name VARCHAR(100) COMMENT '专业名称',
  education_system VARCHAR(20) COMMENT '学制类型',
  skill_level VARCHAR(20) COMMENT '技能等级',
  duration INT COMMENT '学制年数',

  -- 招生计划数据
  planned_class_count INT DEFAULT 0 COMMENT '计划班级数',
  planned_student_count INT DEFAULT 0 COMMENT '计划招生人数',
  planned_start_date DATE COMMENT '计划开班日期',
  enrollment_deadline DATE COMMENT '招生截止日期',

  -- 实际数据(自动统计,触发器更新)
  actual_class_count INT DEFAULT 0 COMMENT '实际班级数',
  actual_student_count INT DEFAULT 0 COMMENT '实际学生人数',
  first_class_date DATE COMMENT '首个班级开班日期',

  -- 招生进度
  enrollment_progress DECIMAL(5,2) COMMENT '招生进度百分比',
  is_full TINYINT DEFAULT 0 COMMENT '是否招满(0=未满,1=已满)',
  is_new_direction TINYINT DEFAULT 0 COMMENT '是否新增方向(0=否,1=是)',

  -- 备注
  remarks TEXT COMMENT '备注',

  -- 审计字段
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  created_by BIGINT COMMENT '创建人ID',
  updated_by BIGINT COMMENT '更新人ID',
  deleted TINYINT DEFAULT 0 COMMENT '删除标志',

  -- 索引
  UNIQUE KEY uk_grade_direction (grade_id, major_direction_id, deleted),
  INDEX idx_grade (grade_id),
  INDEX idx_major_direction (major_direction_id),
  INDEX idx_major (major_id),

  -- 外键
  CONSTRAINT fk_gmd_grade
    FOREIGN KEY (grade_id) REFERENCES grades(id) ON DELETE CASCADE,
  CONSTRAINT fk_gmd_direction
    FOREIGN KEY (major_direction_id) REFERENCES major_directions(id) ON DELETE CASCADE

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='年级专业方向关联表-每年开设的专业方向';

-- 3. 修改班级表结构
-- 3.1 添加新字段
ALTER TABLE classes
  ADD COLUMN major_direction_id BIGINT COMMENT '专业方向ID' AFTER major_id,
  ADD COLUMN class_sequence INT COMMENT '班级序号(同年级同方向内唯一)' AFTER major_direction_id,
  ADD COLUMN education_system VARCHAR(20) COMMENT '学制类型' AFTER enrollment_year,
  ADD COLUMN skill_level VARCHAR(20) COMMENT '技能等级' AFTER education_system,
  ADD COLUMN duration INT COMMENT '学制年数' AFTER skill_level,
  ADD COLUMN is_international TINYINT DEFAULT 0 COMMENT '是否国际班' AFTER status,
  ADD COLUMN is_experimental TINYINT DEFAULT 0 COMMENT '是否实验班' AFTER is_international,
  ADD COLUMN is_oriented TINYINT DEFAULT 0 COMMENT '是否定向班' AFTER is_experimental;

-- 3.2 添加索引
ALTER TABLE classes
  ADD INDEX idx_major_direction (major_direction_id),
  ADD INDEX idx_grade_major_direction (grade_id, major_direction_id),
  ADD UNIQUE KEY uk_class_sequence (grade_id, major_direction_id, class_sequence, deleted);

-- 3.3 添加外键
ALTER TABLE classes
  ADD CONSTRAINT fk_class_major_direction
    FOREIGN KEY (major_direction_id) REFERENCES major_directions(id);

-- 4. 修改grades表(优化)
ALTER TABLE grades
  MODIFY COLUMN grade_level INT COMMENT '年级等级[已废弃-技工院校不使用]';

-- 5. 修改majors表(优化)
ALTER TABLE majors
  ADD COLUMN major_category VARCHAR(50) COMMENT '专业大类(计算机类/汽修类/幼教类)' AFTER major_code,
  ADD COLUMN is_core_major TINYINT DEFAULT 0 COMMENT '是否核心专业(0=否,1=是)' AFTER status;

-- 6. 修改class_size_standards表(支持专业方向维度)
ALTER TABLE class_size_standards
  ADD COLUMN major_direction_id BIGINT COMMENT '专业方向ID(可选,优先级最高)' AFTER major_id,
  ADD COLUMN enrollment_year YEAR COMMENT '入学年份(可选,替代grade_level)' AFTER department_id,
  ADD INDEX idx_major_direction (major_direction_id);

-- 7. 修改量化管理相关表
-- 7.1 检查记录班级统计表
ALTER TABLE check_record_class_stats
  ADD COLUMN major_direction_id BIGINT COMMENT '专业方向ID' AFTER major_id,
  ADD COLUMN enrollment_year YEAR COMMENT '入学年份' AFTER grade_name,
  ADD COLUMN education_system VARCHAR(20) COMMENT '学制类型' AFTER enrollment_year,
  ADD COLUMN skill_level VARCHAR(20) COMMENT '技能等级' AFTER education_system,
  ADD INDEX idx_major_direction (major_direction_id),
  ADD INDEX idx_enrollment_year (enrollment_year);

-- 8. 创建触发器:自动更新统计数据
DELIMITER $$

-- 8.1 班级创建时,更新年级-专业方向统计
CREATE TRIGGER tr_class_insert_update_gmd
AFTER INSERT ON classes
FOR EACH ROW
BEGIN
  IF NEW.deleted = 0 AND NEW.grade_id IS NOT NULL AND NEW.major_direction_id IS NOT NULL THEN
    UPDATE grade_major_directions SET
      actual_class_count = actual_class_count + 1,
      actual_student_count = actual_student_count + IFNULL(NEW.student_count, 0),
      first_class_date = IFNULL(first_class_date, NOW()),
      enrollment_progress = CASE
        WHEN planned_student_count > 0
        THEN ROUND((actual_student_count + IFNULL(NEW.student_count, 0)) * 100.0 / planned_student_count, 2)
        ELSE 0
      END
    WHERE grade_id = NEW.grade_id
      AND major_direction_id = NEW.major_direction_id
      AND deleted = 0;
  END IF;
END$$

-- 8.2 班级删除时,更新统计
CREATE TRIGGER tr_class_delete_update_gmd
AFTER UPDATE ON classes
FOR EACH ROW
BEGIN
  IF OLD.deleted = 0 AND NEW.deleted = 1
    AND NEW.grade_id IS NOT NULL
    AND NEW.major_direction_id IS NOT NULL THEN
    UPDATE grade_major_directions SET
      actual_class_count = GREATEST(0, actual_class_count - 1),
      actual_student_count = GREATEST(0, actual_student_count - IFNULL(OLD.student_count, 0)),
      enrollment_progress = CASE
        WHEN planned_student_count > 0
        THEN ROUND((actual_student_count - IFNULL(OLD.student_count, 0)) * 100.0 / planned_student_count, 2)
        ELSE 0
      END
    WHERE grade_id = NEW.grade_id
      AND major_direction_id = NEW.major_direction_id
      AND deleted = 0;
  END IF;
END$$

-- 8.3 班级学生数变化时,更新统计
CREATE TRIGGER tr_class_student_count_update_gmd
AFTER UPDATE ON classes
FOR EACH ROW
BEGIN
  IF NEW.deleted = 0
    AND NEW.grade_id IS NOT NULL
    AND NEW.major_direction_id IS NOT NULL
    AND (OLD.student_count != NEW.student_count OR OLD.student_count IS NULL) THEN
    UPDATE grade_major_directions SET
      actual_student_count = actual_student_count - IFNULL(OLD.student_count, 0) + IFNULL(NEW.student_count, 0),
      enrollment_progress = CASE
        WHEN planned_student_count > 0
        THEN ROUND((actual_student_count - IFNULL(OLD.student_count, 0) + IFNULL(NEW.student_count, 0)) * 100.0 / planned_student_count, 2)
        ELSE 0
      END
    WHERE grade_id = NEW.grade_id
      AND major_direction_id = NEW.major_direction_id
      AND deleted = 0;
  END IF;
END$$

DELIMITER ;

-- 9. 创建视图:便于查询
CREATE OR REPLACE VIEW v_class_full_info AS
SELECT
  c.id,
  c.class_name,
  c.class_code,
  c.class_sequence,

  -- 年级信息
  g.id AS grade_id,
  g.grade_name,
  g.enrollment_year,

  -- 专业信息
  m.id AS major_id,
  m.major_name,
  m.major_code,
  m.major_category,

  -- 专业方向信息
  md.id AS major_direction_id,
  md.direction_name,
  md.education_system,
  md.skill_level,
  md.duration,
  md.is_international,
  md.is_experimental,
  md.is_oriented,

  -- 部门信息
  d.id AS department_id,
  d.dept_name AS department_name,

  -- 班级详细信息
  c.teacher_id,
  c.student_count,
  c.classroom_location,
  c.status,
  c.graduation_year,
  c.created_at

FROM classes c
LEFT JOIN grades g ON c.grade_id = g.id
LEFT JOIN majors m ON c.major_id = m.id
LEFT JOIN major_directions md ON c.major_direction_id = md.id
LEFT JOIN departments d ON c.department_id = d.id
WHERE c.deleted = 0;

-- 10. 创建初始数据视图:年级-专业方向汇总
CREATE OR REPLACE VIEW v_grade_major_direction_summary AS
SELECT
  gmd.id,
  g.grade_name,
  g.enrollment_year,
  d.dept_name AS department_name,
  m.major_name,
  md.direction_name,
  md.education_system,
  md.skill_level,
  md.duration,
  gmd.planned_class_count,
  gmd.planned_student_count,
  gmd.actual_class_count,
  gmd.actual_student_count,
  gmd.enrollment_progress,
  gmd.is_full,
  gmd.is_new_direction,
  CONCAT(gmd.actual_class_count, '/', gmd.planned_class_count) AS class_progress,
  CONCAT(gmd.actual_student_count, '/', gmd.planned_student_count) AS student_progress
FROM grade_major_directions gmd
LEFT JOIN grades g ON gmd.grade_id = g.id
LEFT JOIN majors m ON gmd.major_id = m.id
LEFT JOIN major_directions md ON gmd.major_direction_id = md.id
LEFT JOIN departments d ON g.department_id = d.id
WHERE gmd.deleted = 0
ORDER BY g.enrollment_year DESC, m.major_name, md.sort_order;

-- 完成
SELECT 'Database migration completed successfully!' AS status;
