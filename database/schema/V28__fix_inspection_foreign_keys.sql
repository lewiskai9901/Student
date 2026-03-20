-- 修复检查模块外键：从旧备份表 inspection_targets_v4_backup 改为正确的 inspection_targets
-- 问题：项目清理时遗留了指向旧表的外键约束，导致新增检查明细时报外键约束失败

-- 1. inspection_details.target_id → inspection_targets(id)
ALTER TABLE inspection_details DROP FOREIGN KEY inspection_details_ibfk_1;
ALTER TABLE inspection_details ADD CONSTRAINT inspection_details_fk_target
    FOREIGN KEY (target_id) REFERENCES inspection_targets(id) ON DELETE CASCADE;

-- 2. inspection_deduction_items.target_id → inspection_targets(id)
--    先清理孤儿数据（引用已不存在的 target_id）
DELETE FROM inspection_deduction_items
WHERE target_id NOT IN (SELECT id FROM inspection_targets);

ALTER TABLE inspection_deduction_items DROP FOREIGN KEY inspection_deduction_items_ibfk_2;
ALTER TABLE inspection_deduction_items ADD CONSTRAINT inspection_deduction_items_fk_target
    FOREIGN KEY (target_id) REFERENCES inspection_targets(id) ON DELETE CASCADE;
