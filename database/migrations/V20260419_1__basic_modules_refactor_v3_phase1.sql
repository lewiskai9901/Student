-- ============================================================
-- Basic Modules Refactor v3 — Phase 1: 纯新增，不破坏现有代码
--   1) 新增关系字典 + 历史表 + 关系扩展表
--   2) 新增字段注册 + EAV 扩展值表
--   3) 新增事件契约表 + 通道投递表
--   4) 新增统一数据权限表 + 资源注册表
--   5) 新增用户/场所扩展表 (user_parent/user_staff/user_counselor/place_dorm_room 等)
--
-- 本文件只 CREATE 与 INSERT，**不 DROP 任何表、不 ALTER 已有表**，
-- 保证执行后应用继续可跑。Phase 2 再做 DROP + ALTER + RENAME。
-- ============================================================

-- ------------------------------------------------------------
-- 1. relation_types 关系字典
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS relation_types (
  relation_code          VARCHAR(50)  NOT NULL,
  from_type              VARCHAR(30)  NOT NULL,
  to_type                VARCHAR(30)  NOT NULL,
  relation_name          VARCHAR(50)  NOT NULL,
  is_transitive          TINYINT      NOT NULL DEFAULT 0,
  category               VARCHAR(20)  NOT NULL COMMENT 'OWNERSHIP/MEMBERSHIP/ASSOCIATION/DELEGATION/SUBSCRIPTION',
  allowed_from_type_codes JSON        NULL,
  allowed_to_type_codes  JSON         NULL,
  extension_table        VARCHAR(100) NULL,
  tier                   VARCHAR(20)  NOT NULL DEFAULT 'CORE' COMMENT 'CORE/COMMON_EXT/DOMAIN',
  registered_by          VARCHAR(100) NOT NULL DEFAULT 'CORE',
  description            VARCHAR(200),
  is_enabled             TINYINT      NOT NULL DEFAULT 1,
  tenant_id              BIGINT       NOT NULL DEFAULT 1,
  created_at             DATETIME     DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (relation_code, from_type, to_type, tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='关系类型字典';

-- 预置 9 个 CORE + 3 个 COMMON_EXT + 教育 DOMAIN
INSERT INTO relation_types
  (relation_code, from_type, to_type, relation_name, is_transitive, category, allowed_from_type_codes, allowed_to_type_codes, extension_table, tier, registered_by, description)
VALUES
  -- === CORE 9 个 ===
  ('member',       'user',  'org_unit', '成员',       0, 'MEMBERSHIP',   NULL, NULL, NULL,                                  'CORE', 'CORE', '用户属于该组织'),
  ('admin',        'user',  'org_unit', '主管理员',   1, 'OWNERSHIP',    JSON_ARRAY('TEACHER','COUNSELOR','STAFF','ADMIN','SUPER_ADMIN'), NULL, NULL, 'CORE', 'CORE', '组织的主负责人,沿子组织传递'),
  ('admin',        'user',  'place',    '场所负责人', 0, 'OWNERSHIP',    JSON_ARRAY('TEACHER','STAFF','ADMIN','SUPER_ADMIN'), NULL, NULL,  'CORE', 'CORE', '场所的主负责人'),
  ('deputy',       'user',  'org_unit', '副管理员',   1, 'OWNERSHIP',    JSON_ARRAY('TEACHER','STAFF','ADMIN'), NULL, NULL,               'CORE', 'CORE', '组织副负责人'),
  ('manages',      'user',  'place',    '管理者',     0, 'OWNERSHIP',    NULL, NULL, NULL,                                  'CORE', 'CORE', '非主责的管理者,如保洁/物业'),
  ('belongs_to',   'place', 'org_unit', '归属',       0, 'ASSOCIATION',  NULL, NULL, NULL,                                  'CORE', 'CORE', '场所归属某组织'),
  ('occupies',     'user',  'place',    '占用',       0, 'MEMBERSHIP',   NULL, NULL, 'access_relations_occupancy_ext',       'CORE', 'CORE', '宿舍入住/工位使用'),
  ('delegated_to', 'user',  'user',     '委托',       0, 'DELEGATION',   NULL, NULL, 'access_relations_delegation_ext',      'CORE', 'CORE', '权限临时委托'),
  ('watches',      'user',  'org_unit', '关注',       0, 'SUBSCRIPTION', NULL, NULL, NULL,                                  'CORE', 'CORE', '订阅组织动态(用于消息)'),

  -- === COMMON_EXT 通用扩展 (教育/医疗/养老共享) ===
  ('guardian_of',  'user',  'user',     '监护',       0, 'ASSOCIATION',  JSON_ARRAY('PARENT','STAFF'), JSON_ARRAY('STUDENT'), NULL, 'COMMON_EXT', 'CommonExtPlugin', '家长监护学生/护工监护病人'),
  ('supervisor_of','user',  'user',     '上级',       0, 'ASSOCIATION',  NULL, NULL, NULL,                                  'COMMON_EXT', 'CommonExtPlugin', '人员上下级关系'),
  ('mentor_of',    'user',  'user',     '导师',       0, 'ASSOCIATION',  NULL, NULL, NULL,                                  'COMMON_EXT', 'CommonExtPlugin', '指导关系'),

  -- === DOMAIN 教育插件 ===
  ('teaches',      'user',  'org_unit', '任课',       0, 'ASSOCIATION',  JSON_ARRAY('TEACHER'), JSON_ARRAY('CLASS'), 'relation_teach_ext', 'DOMAIN', 'EducationPlugin', '教师任教班级'),
  ('advisor_of',   'user',  'org_unit', '辅导员',     1, 'OWNERSHIP',    JSON_ARRAY('COUNSELOR'), JSON_ARRAY('GRADE','CLASS'), NULL, 'DOMAIN', 'EducationPlugin', '辅导员负责年级或班级');

-- ------------------------------------------------------------
-- 2. access_relations 加时间维度 + 软删字段
-- ------------------------------------------------------------
ALTER TABLE access_relations
  ADD COLUMN valid_from DATETIME NULL DEFAULT CURRENT_TIMESTAMP AFTER access_level,
  ADD COLUMN valid_to   DATETIME NULL                          AFTER valid_from,
  ADD COLUMN deleted_at DATETIME NULL                          AFTER deleted,
  ADD COLUMN deleted_by BIGINT   NULL                          AFTER deleted_at;

-- 新索引(针对时间点查询)
ALTER TABLE access_relations
  ADD INDEX idx_validity (valid_from, valid_to),
  ADD INDEX idx_relation_validity (relation, valid_to, deleted);

-- ------------------------------------------------------------
-- 3. access_relations_history 历史归档
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS access_relations_history (
  id                   BIGINT PRIMARY KEY AUTO_INCREMENT,
  original_id          BIGINT,
  resource_type        VARCHAR(30) NOT NULL,
  resource_id          BIGINT      NOT NULL,
  relation             VARCHAR(50) NOT NULL,
  subject_type         VARCHAR(30) NOT NULL,
  subject_id           BIGINT      NOT NULL,
  include_children     TINYINT,
  access_level         VARCHAR(20),
  valid_from           DATETIME,
  valid_to             DATETIME,
  metadata             JSON,
  remark               VARCHAR(500),
  archived_at          DATETIME    DEFAULT CURRENT_TIMESTAMP,
  archived_reason      VARCHAR(100),
  archived_by          BIGINT,
  tenant_id            BIGINT      NOT NULL DEFAULT 1,
  created_by           BIGINT,
  INDEX idx_original (original_id),
  INDEX idx_resource_subject (resource_type, resource_id, subject_id),
  INDEX idx_archived (archived_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='关系归档表';

-- ------------------------------------------------------------
-- 4. 关系扩展表(强结构关系)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS access_relations_occupancy_ext (
  relation_id          BIGINT PRIMARY KEY COMMENT 'FK -> access_relations.id',
  seat_no              VARCHAR(20),
  check_in_time        DATETIME NOT NULL,
  check_out_time       DATETIME NULL,
  notes                VARCHAR(500)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='占用关系扩展(床位号/时段)';

CREATE TABLE IF NOT EXISTS access_relations_delegation_ext (
  relation_id          BIGINT PRIMARY KEY,
  delegated_permissions JSON NULL COMMENT '委托的具体权限列表(子集)',
  delegation_reason    VARCHAR(200)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='委托关系扩展';

CREATE TABLE IF NOT EXISTS relation_teach_ext (
  relation_id          BIGINT PRIMARY KEY,
  course_id            BIGINT NOT NULL,
  semester_id          BIGINT,
  teaching_hours       INT,
  INDEX idx_course (course_id),
  INDEX idx_semester (semester_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任课关系扩展(教育插件)';

-- ------------------------------------------------------------
-- 5. entity_field_definitions 字段注册
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS entity_field_definitions (
  id                   BIGINT PRIMARY KEY AUTO_INCREMENT,
  entity_type          VARCHAR(30) NOT NULL,
  type_code            VARCHAR(50) NOT NULL,
  field_code           VARCHAR(50) NOT NULL,
  field_name           VARCHAR(50),
  field_type           VARCHAR(20) NOT NULL COMMENT 'text/number/date/relation/bool/enum',
  category             VARCHAR(50),
  is_required          TINYINT DEFAULT 0,
  is_system            TINYINT DEFAULT 0 COMMENT '1=插件声明,0=管理员自定义',
  options              JSON,
  storage_kind         VARCHAR(20) NOT NULL DEFAULT 'ATTR' COMMENT 'COLUMN/ATTR',
  storage_table        VARCHAR(100),
  storage_column       VARCHAR(100),
  sort_order           INT DEFAULT 0,
  tenant_id            BIGINT NOT NULL DEFAULT 1,
  created_at           DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at           DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted              TINYINT DEFAULT 0,
  UNIQUE KEY uk_field (entity_type, type_code, field_code, tenant_id),
  INDEX idx_storage (storage_kind, storage_table)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='实体字段定义注册';

-- ------------------------------------------------------------
-- 6. entity_attribute_values 统一 EAV 兜底
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS entity_attribute_values (
  id                   BIGINT PRIMARY KEY AUTO_INCREMENT,
  entity_type          VARCHAR(30) NOT NULL,
  entity_id            BIGINT      NOT NULL,
  field_code           VARCHAR(50) NOT NULL,
  value_text           VARCHAR(1000) NULL,
  value_number         DECIMAL(20,6) NULL,
  value_date           DATETIME      NULL,
  value_bool           TINYINT       NULL,
  value_ref_id         BIGINT        NULL,
  tenant_id            BIGINT NOT NULL DEFAULT 1,
  updated_at           DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_entity_field (entity_type, entity_id, field_code, tenant_id),
  INDEX idx_field_text (field_code, value_text(100)),
  INDEX idx_field_num (field_code, value_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='实体属性值(EAV 兜底)';

-- ------------------------------------------------------------
-- 7. event_schemas 事件契约
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS event_schemas (
  event_type             VARCHAR(50) PRIMARY KEY,
  event_category         VARCHAR(30),
  required_payload_keys  JSON NOT NULL COMMENT '["batchId","createdBy"]',
  optional_payload_keys  JSON NULL,
  description            TEXT,
  registered_by          VARCHAR(100),
  created_at             DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='事件契约注册';

-- 预置 CORE + 教育事件
INSERT INTO event_schemas (event_type, event_category, required_payload_keys, description, registered_by) VALUES
  ('GRADE_SUBMITTED',     'TEACHING', JSON_ARRAY('batchId','courseId','createdBy'),     '成绩提交,主体=提交教师',  'EducationPlugin'),
  ('GRADE_APPROVED',      'TEACHING', JSON_ARRAY('batchId','courseId','createdBy','approvedBy'), '成绩审核通过',  'EducationPlugin'),
  ('GRADE_PUBLISHED',     'TEACHING', JSON_ARRAY('batchId','courseId','orgUnitId'),     '成绩发布',            'EducationPlugin'),
  ('EXAM_PUBLISHED',      'TEACHING', JSON_ARRAY('examId','orgUnitId'),                 '考试发布',            'EducationPlugin'),
  ('ATTENDANCE_RECORDED', 'TEACHING', JSON_ARRAY('studentId','classId','status'),       '考勤记录',            'EducationPlugin'),
  ('INSP_ITEM_RESULT',    'INSPECTION', JSON_ARRAY('studentId','classId','itemId'),     '检查项结果',          'InspectionPlugin');

-- ------------------------------------------------------------
-- 8. channel_deliveries 通道投递
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS channel_deliveries (
  id                   BIGINT PRIMARY KEY AUTO_INCREMENT,
  notification_id      BIGINT NOT NULL COMMENT 'FK -> msg_notifications.id',
  channel              VARCHAR(20) NOT NULL COMMENT 'IN_APP/SMS/WX_MP/EMAIL/APP_PUSH',
  status               VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/SENDING/SENT/DELIVERED/FAILED/RETRYING/DEAD',
  provider_msg_id      VARCHAR(100),
  retry_count          INT         NOT NULL DEFAULT 0,
  next_retry_at        DATETIME,
  sent_at              DATETIME,
  delivered_at         DATETIME COMMENT '第三方回执',
  read_at              DATETIME,
  clicked_at           DATETIME,
  last_error           VARCHAR(500),
  tenant_id            BIGINT NOT NULL DEFAULT 1,
  created_at           DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_notification (notification_id),
  INDEX idx_status_retry (status, next_retry_at),
  INDEX idx_channel (channel, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通道投递记录';

-- ------------------------------------------------------------
-- 9. data_resources 通用资源注册 + role_data_scopes 统一权限
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS data_resources (
  resource_code        VARCHAR(50) PRIMARY KEY,
  resource_name        VARCHAR(50) NOT NULL,
  domain_code          VARCHAR(50) NOT NULL DEFAULT 'CORE',
  domain_name          VARCHAR(50),
  org_unit_field       VARCHAR(50) NULL COMMENT '业务表里指向组织的字段',
  creator_field        VARCHAR(50) NULL,
  registered_by        VARCHAR(100) NOT NULL DEFAULT 'CORE',
  sort_order           INT DEFAULT 0,
  enabled              TINYINT DEFAULT 1,
  tenant_id            BIGINT NOT NULL DEFAULT 1,
  created_at           DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据资源注册';

INSERT INTO data_resources VALUES
  ('user',         '用户',      'CORE',      '核心',       NULL,            'created_by', 'CORE', 1, 1, 1, NOW()),
  ('org_unit',     '组织',      'CORE',      '核心',       'parent_id',     'created_by', 'CORE', 2, 1, 1, NOW()),
  ('place',        '场所',      'CORE',      '核心',       NULL,            'created_by', 'CORE', 3, 1, 1, NOW()),
  ('role',         '角色',      'CORE',      '核心',       NULL,            'created_by', 'CORE', 4, 1, 1, NOW()),
  ('notification', '消息',      'CORE',      '核心',       NULL,            'user_id',    'CORE', 5, 1, 1, NOW()),
  ('student',      '学生',      'education', '教育',       'org_unit_id',   'created_by', 'EducationPlugin',   10, 1, 1, NOW()),
  ('grade_batch',  '成绩批次',  'education', '教育',       'org_unit_id',   'created_by', 'EducationPlugin',   20, 1, 1, NOW()),
  ('exam',         '考试',      'education', '教育',       NULL,            'created_by', 'EducationPlugin',   21, 1, 1, NOW()),
  ('attendance',   '考勤',      'education', '教育',       'class_id',      'recorded_by','EducationPlugin',   22, 1, 1, NOW()),
  ('inspection',   '检查记录',  'inspection','检查平台',   'org_unit_id',   'created_by', 'InspectionPlugin',  30, 1, 1, NOW());

CREATE TABLE IF NOT EXISTS role_data_scopes (
  id                   BIGINT PRIMARY KEY AUTO_INCREMENT,
  role_id              BIGINT      NOT NULL,
  resource_code        VARCHAR(50) NOT NULL,
  scope_type           VARCHAR(30) NOT NULL COMMENT 'ALL/OWN_AND_BELOW/OWN/SELF/CUSTOM',
  custom_org_unit_ids  JSON        NULL COMMENT 'CUSTOM 时用',
  priority             INT         DEFAULT 0,
  tenant_id            BIGINT      NOT NULL DEFAULT 1,
  created_at           DATETIME    DEFAULT CURRENT_TIMESTAMP,
  updated_at           DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted              TINYINT     DEFAULT 0,
  UNIQUE KEY uk_role_res (role_id, resource_code, tenant_id),
  INDEX idx_role (role_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色数据权限(单轨,替代多代并存)';

-- ------------------------------------------------------------
-- 10. 用户类型扩展表(规范命名 user_{type})
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS user_parent (
  id                   BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id              BIGINT NOT NULL UNIQUE,
  occupation           VARCHAR(100),
  id_card              VARCHAR(18),
  workplace            VARCHAR(200),
  emergency_contact    TINYINT DEFAULT 0,
  tenant_id            BIGINT NOT NULL DEFAULT 1,
  created_at           DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at           DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted              TINYINT DEFAULT 0,
  CONSTRAINT fk_user_parent_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='家长用户扩展';

CREATE TABLE IF NOT EXISTS user_staff (
  id                   BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id              BIGINT NOT NULL UNIQUE,
  position             VARCHAR(50),
  department_rank      VARCHAR(30),
  hire_date            DATE,
  tenant_id            BIGINT NOT NULL DEFAULT 1,
  created_at           DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at           DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted              TINYINT DEFAULT 0,
  CONSTRAINT fk_user_staff_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='职工扩展';

CREATE TABLE IF NOT EXISTS user_counselor (
  id                   BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id              BIGINT NOT NULL UNIQUE,
  hire_date            DATE,
  responsibility_area  VARCHAR(200),
  tenant_id            BIGINT NOT NULL DEFAULT 1,
  created_at           DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at           DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted              TINYINT DEFAULT 0,
  CONSTRAINT fk_user_counselor_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='辅导员扩展';

-- ------------------------------------------------------------
-- 11. 场所类型扩展表(规范命名 place_{type}) — 新建待业务填
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS place_classroom (
  id                   BIGINT PRIMARY KEY AUTO_INCREMENT,
  place_id             BIGINT NOT NULL UNIQUE,
  classroom_category   VARCHAR(32),
  has_projector        TINYINT DEFAULT 0,
  has_air_conditioner  TINYINT DEFAULT 0,
  has_computer         TINYINT DEFAULT 0,
  equipment_info       VARCHAR(500),
  tenant_id            BIGINT NOT NULL DEFAULT 1,
  created_at           DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at           DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted              TINYINT DEFAULT 0,
  CONSTRAINT fk_place_classroom_place FOREIGN KEY (place_id) REFERENCES places(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教室扩展';

CREATE TABLE IF NOT EXISTS place_dorm_room (
  id                   BIGINT PRIMARY KEY AUTO_INCREMENT,
  place_id             BIGINT NOT NULL UNIQUE,
  gender_type          TINYINT NOT NULL,
  bed_count            INT,
  facilities           VARCHAR(500),
  tenant_id            BIGINT NOT NULL DEFAULT 1,
  created_at           DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at           DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted              TINYINT DEFAULT 0,
  CONSTRAINT fk_place_dormroom_place FOREIGN KEY (place_id) REFERENCES places(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='宿舍扩展(正规命名,替代 place_dormitory_ext)';

CREATE TABLE IF NOT EXISTS place_lab (
  id                   BIGINT PRIMARY KEY AUTO_INCREMENT,
  place_id             BIGINT NOT NULL UNIQUE,
  lab_type             VARCHAR(50),
  safety_level         VARCHAR(20),
  equipment_inventory  JSON,
  tenant_id            BIGINT NOT NULL DEFAULT 1,
  created_at           DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at           DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted              TINYINT DEFAULT 0,
  CONSTRAINT fk_place_lab_place FOREIGN KEY (place_id) REFERENCES places(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='实验室扩展';

CREATE TABLE IF NOT EXISTS place_office (
  id                   BIGINT PRIMARY KEY AUTO_INCREMENT,
  place_id             BIGINT NOT NULL UNIQUE,
  office_type          VARCHAR(50),
  workstation_count    INT,
  tenant_id            BIGINT NOT NULL DEFAULT 1,
  created_at           DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at           DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted              TINYINT DEFAULT 0,
  CONSTRAINT fk_place_office_place FOREIGN KEY (place_id) REFERENCES places(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='办公室扩展';

-- ============================================================
-- Phase 1 结束。下一 Phase (V20260419_2) 做破坏性变更:
--   - DROP 18 张 zombie 表
--   - RENAME students -> user_student, teacher_profiles -> user_teacher
--   - DROP 直连字段 (users.primary_org_unit_id 等)
-- 必须先修代码,再执行 Phase 2。
-- ============================================================
