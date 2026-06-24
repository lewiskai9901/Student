-- 有向关系链 P-M1: relation_types 加 reverse_name (resource→subject 反向读名),
-- 供有向链 UI 显示反向边名 + 反向走跳。baseline_v3 已含该列(新库直接有), 本迁移供已建库追上。
-- ⚠ apply 必带 --default-character-set=utf8mb4 (含中文, 否则 mojibake)。

ALTER TABLE `relation_types`
  ADD COLUMN `reverse_name` varchar(50) DEFAULT NULL
  COMMENT '反向读名 (resource→subject 视角, 如 belongs_to 反向"组织下辖场所")'
  AFTER `relation_name`;

-- 种子: 反向名 = 该关系从 resource 侧读到的 subject 角色 (人话, 非杜撰)。
-- user → org_unit
UPDATE relation_types SET reverse_name='管理员'    WHERE relation_code='admin'           AND from_type='user'  AND to_type='org_unit';
UPDATE relation_types SET reverse_name='辅导员'    WHERE relation_code='advisor_of'      AND from_type='user'  AND to_type='org_unit';
UPDATE relation_types SET reverse_name='副管理员'  WHERE relation_code='deputy'          AND from_type='user'  AND to_type='org_unit';
UPDATE relation_types SET reverse_name='成员'      WHERE relation_code='member'          AND from_type='user'  AND to_type='org_unit';
UPDATE relation_types SET reverse_name='责任人'    WHERE relation_code='responsible_for' AND from_type='user'  AND to_type='org_unit';
UPDATE relation_types SET reverse_name='任课教师'  WHERE relation_code='teaches'         AND from_type='user'  AND to_type='org_unit';
UPDATE relation_types SET reverse_name='查阅者'    WHERE relation_code='viewer'          AND from_type='user'  AND to_type='org_unit';
UPDATE relation_types SET reverse_name='关注者'    WHERE relation_code='watches'         AND from_type='user'  AND to_type='org_unit';
-- user → place
UPDATE relation_types SET reverse_name='负责人'    WHERE relation_code='admin'           AND from_type='user'  AND to_type='place';
UPDATE relation_types SET reverse_name='管理者'    WHERE relation_code='manages'         AND from_type='user'  AND to_type='place';
UPDATE relation_types SET reverse_name='占用人'    WHERE relation_code='occupies'        AND from_type='user'  AND to_type='place';
UPDATE relation_types SET reverse_name='责任人'    WHERE relation_code='responsible_for' AND from_type='user'  AND to_type='place';
UPDATE relation_types SET reverse_name='查阅者'    WHERE relation_code='viewer'          AND from_type='user'  AND to_type='place';
-- user → user
UPDATE relation_types SET reverse_name='委托方'    WHERE relation_code='delegated_to'     AND from_type='user' AND to_type='user';
UPDATE relation_types SET reverse_name='紧急联系人' WHERE relation_code='emergency_contact' AND from_type='user' AND to_type='user';
UPDATE relation_types SET reverse_name='亲属'      WHERE relation_code='family_of'        AND from_type='user' AND to_type='user';
UPDATE relation_types SET reverse_name='监护人'    WHERE relation_code='guardian_of'      AND from_type='user' AND to_type='user';
UPDATE relation_types SET reverse_name='导师'      WHERE relation_code='mentor_of'        AND from_type='user' AND to_type='user';
UPDATE relation_types SET reverse_name='责任人'    WHERE relation_code='responsible_for'  AND from_type='user' AND to_type='user';
UPDATE relation_types SET reverse_name='上级'      WHERE relation_code='supervisor_of'    AND from_type='user' AND to_type='user';
UPDATE relation_types SET reverse_name='查阅者'    WHERE relation_code='viewer'           AND from_type='user' AND to_type='user';
-- place → org_unit
UPDATE relation_types SET reverse_name='下辖场所'  WHERE relation_code='belongs_to'       AND from_type='place' AND to_type='org_unit';
