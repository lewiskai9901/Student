-- V24: Add username, org_unit_name, gender to place_occupants for display
ALTER TABLE place_occupants
    ADD COLUMN username VARCHAR(50) COMMENT '用户名' AFTER occupant_name,
    ADD COLUMN org_unit_name VARCHAR(100) COMMENT '组织名称(冗余)' AFTER username,
    ADD COLUMN gender TINYINT COMMENT '性别: 0-未知 1-男 2-女' AFTER org_unit_name;
