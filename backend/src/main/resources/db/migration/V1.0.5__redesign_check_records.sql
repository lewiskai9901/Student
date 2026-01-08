-- 重新设计检查记录表结构
-- 将 check_records 从"每个班级+类别一条记录"改为"每个日常检查一条记录"

-- 1. 备份现有数据
CREATE TABLE IF NOT EXISTS check_records_backup AS SELECT * FROM check_records;
CREATE TABLE IF NOT EXISTS check_record_details_backup AS SELECT * FROM check_record_details;

-- 2. 删除现有数据（因为结构变化较大）
TRUNCATE TABLE check_record_details;
TRUNCATE TABLE check_records;

-- 3. 修改 check_records 表结构
ALTER TABLE check_records
  DROP COLUMN IF EXISTS category_id,
  DROP COLUMN IF EXISTS class_id,
  DROP COLUMN IF EXISTS class_name;

-- 4. 添加新字段
ALTER TABLE check_records
  ADD COLUMN IF NOT EXISTS check_name VARCHAR(200) COMMENT '检查名称' AFTER check_id,
  ADD COLUMN IF NOT EXISTS check_date DATE COMMENT '检查日期' AFTER check_name,
  ADD COLUMN IF NOT EXISTS checker_id BIGINT COMMENT '检查员ID' AFTER check_date,
  ADD COLUMN IF NOT EXISTS checker_name VARCHAR(100) COMMENT '检查员姓名' AFTER checker_id,
  ADD COLUMN IF NOT EXISTS status INT DEFAULT 0 COMMENT '状态: 0进行中 1已完成' AFTER checker_name;

-- 5. 添加索引
ALTER TABLE check_records
  ADD INDEX IF NOT EXISTS idx_check_date (check_date),
  ADD INDEX IF NOT EXISTS idx_status (status),
  ADD INDEX IF NOT EXISTS idx_checker_id (checker_id);

-- 6. 更新注释
ALTER TABLE check_records COMMENT = '检查记录主表：每个日常检查对应一条记录，包含所有类别和班级的汇总';
ALTER TABLE check_record_details COMMENT = '检查记录详情表：记录具体的班级+类别+宿舍/教室的扣分明细';
