-- V18.0.0 用户类型去除系统内置标记
-- 只保留 SUPER_ADMIN 为系统内置，其他类型均可由用户自由增删改

UPDATE user_types SET is_system = 0 WHERE type_code != 'SUPER_ADMIN' AND deleted = 0;
