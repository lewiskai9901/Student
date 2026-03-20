-- 修复座位分配唯一键冲突：改为物理删除
-- 问题：逻辑删除(deleted=1)的记录累积导致唯一键 uk_booking_position 冲突

-- 1. 清除所有已逻辑删除的脏数据
DELETE FROM booking_seat_assignments WHERE deleted = 1;

-- 2. 重建唯一键（去掉 deleted 列，因为不再需要逻辑删除）
ALTER TABLE booking_seat_assignments DROP INDEX uk_booking_position;
ALTER TABLE booking_seat_assignments ADD UNIQUE KEY uk_booking_position (booking_id, position_no);
