-- ================================================================
-- 专业方向体系重构 - 安全迁移脚本(只创建新表和必要修改)
-- 创建时间: 2025-01-15
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

  -- 特殊标记
  is_international TINYINT DEFAULT 0 COMMENT '是否国际班',
  is_experimental TINYINT DEFAULT 0 COMMENT '是否实验班',
  is_oriented TINYINT DEFAULT 0 COMMENT '是否定向培养',
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
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by BIGINT,
  updated_by BIGINT,
  deleted TINYINT DEFAULT 0,

  -- 索引
  UNIQUE KEY uk_direction_code (direction_code),
  UNIQUE KEY uk_major_direction (major_id, education_system, skill_level, deleted),
  INDEX idx_major (major_id),
  INDEX idx_status (status, deleted),

  -- 外键
  CONSTRAINT fk_major_direction_major
    FOREIGN KEY (major_id) REFERENCES majors(id) ON DELETE CASCADE

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='专业方向表';

-- 2. 创建年级-专业方向关联表
CREATE TABLE IF NOT EXISTS grade_major_directions (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  grade_id BIGINT NOT NULL,
  major_direction_id BIGINT NOT NULL,

  -- 冗余字段
  major_id BIGINT NOT NULL,
  major_name VARCHAR(100),
  education_system VARCHAR(20),
  skill_level VARCHAR(20),
  duration INT,

  -- 计划数据
  planned_class_count INT DEFAULT 0,
  planned_student_count INT DEFAULT 0,
  planned_start_date DATE,
  enrollment_deadline DATE,

  -- 实际数据
  actual_class_count INT DEFAULT 0,
  actual_student_count INT DEFAULT 0,
  first_class_date DATE,

  -- 招生进度
  enrollment_progress DECIMAL(5,2),
  is_full TINYINT DEFAULT 0,
  is_new_direction TINYINT DEFAULT 0,

  remarks TEXT,

  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  created_by BIGINT,
  updated_by BIGINT,
  deleted TINYINT DEFAULT 0,

  UNIQUE KEY uk_grade_direction (grade_id, major_direction_id, deleted),
  INDEX idx_grade (grade_id),
  INDEX idx_major_direction (major_direction_id),
  INDEX idx_major (major_id),

  CONSTRAINT fk_gmd_grade
    FOREIGN KEY (grade_id) REFERENCES grades(id) ON DELETE CASCADE,
  CONSTRAINT fk_gmd_direction
    FOREIGN KEY (major_direction_id) REFERENCES major_directions(id) ON DELETE CASCADE

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='年级专业方向关联表';

-- 3. 给majors表添加字段(如果不存在)
SET @sql_major_category = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = 'student_management'
   AND TABLE_NAME = 'majors'
   AND COLUMN_NAME = 'major_category') = 0,
  'ALTER TABLE majors ADD COLUMN major_category VARCHAR(50) COMMENT ''专业大类'' AFTER major_code',
  'SELECT ''Column major_category already exists'''
);
PREPARE stmt FROM @sql_major_category;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql_is_core = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = 'student_management'
   AND TABLE_NAME = 'majors'
   AND COLUMN_NAME = 'is_core_major') = 0,
  'ALTER TABLE majors ADD COLUMN is_core_major TINYINT DEFAULT 0 COMMENT ''是否核心专业'' AFTER status',
  'SELECT ''Column is_core_major already exists'''
);
PREPARE stmt FROM @sql_is_core;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 4. 给class_size_standards表添加字段
SET @sql_css_md = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = 'student_management'
   AND TABLE_NAME = 'class_size_standards'
   AND COLUMN_NAME = 'major_direction_id') = 0,
  'ALTER TABLE class_size_standards ADD COLUMN major_direction_id BIGINT COMMENT ''专业方向ID'' AFTER major_id, ADD INDEX idx_major_direction (major_direction_id)',
  'SELECT ''Column major_direction_id already exists'''
);
PREPARE stmt FROM @sql_css_md;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql_css_ey = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = 'student_management'
   AND TABLE_NAME = 'class_size_standards'
   AND COLUMN_NAME = 'enrollment_year') = 0,
  'ALTER TABLE class_size_standards ADD COLUMN enrollment_year YEAR COMMENT ''入学年份'' AFTER department_id',
  'SELECT ''Column enrollment_year already exists'''
);
PREPARE stmt FROM @sql_css_ey;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 5. 给check_record_class_stats表添加字段
SET @sql_crcs_md = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = 'student_management'
   AND TABLE_NAME = 'check_record_class_stats'
   AND COLUMN_NAME = 'major_direction_id') = 0,
  'ALTER TABLE check_record_class_stats ADD COLUMN major_direction_id BIGINT COMMENT ''专业方向ID'' AFTER total_score, ADD INDEX idx_major_direction (major_direction_id)',
  'SELECT ''Column major_direction_id already exists in check_record_class_stats'''
);
PREPARE stmt FROM @sql_crcs_md;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql_crcs_ey = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = 'student_management'
   AND TABLE_NAME = 'check_record_class_stats'
   AND COLUMN_NAME = 'enrollment_year') = 0,
  'ALTER TABLE check_record_class_stats ADD COLUMN enrollment_year YEAR COMMENT ''入学年份'' AFTER grade_name',
  'SELECT ''Column enrollment_year already exists in check_record_class_stats'''
);
PREPARE stmt FROM @sql_crcs_ey;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql_crcs_es = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = 'student_management'
   AND TABLE_NAME = 'check_record_class_stats'
   AND COLUMN_NAME = 'education_system') = 0,
  'ALTER TABLE check_record_class_stats ADD COLUMN education_system VARCHAR(20) COMMENT ''学制类型'' AFTER enrollment_year',
  'SELECT ''Column education_system already exists in check_record_class_stats'''
);
PREPARE stmt FROM @sql_crcs_es;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql_crcs_sl = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = 'student_management'
   AND TABLE_NAME = 'check_record_class_stats'
   AND COLUMN_NAME = 'skill_level') = 0,
  'ALTER TABLE check_record_class_stats ADD COLUMN skill_level VARCHAR(20) COMMENT ''技能等级'' AFTER education_system',
  'SELECT ''Column skill_level already exists in check_record_class_stats'''
);
PREPARE stmt FROM @sql_crcs_sl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 6. 创建视图
CREATE OR REPLACE VIEW v_class_full_info AS
SELECT
  c.id,
  c.class_name,
  c.class_code,
  c.class_sequence,
  c.grade_id,
  g.grade_name,
  g.enrollment_year,
  c.major_id,
  m.major_name,
  m.major_code,
  m.major_category,
  c.major_direction_id,
  md.direction_name,
  COALESCE(md.education_system, c.education_system) AS education_system,
  COALESCE(md.skill_level, c.skill_level) AS skill_level,
  COALESCE(md.duration, c.duration) AS duration,
  c.department_id,
  d.dept_name AS department_name,
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

-- 7. 创建年级-专业方向汇总视图
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

SELECT 'Safe migration completed successfully!' AS status;
