UPDATE user_types SET is_system = 0 WHERE type_code <> 'SUPER_ADMIN' AND deleted = 0;
SELECT id, type_code, type_name, parent_type_code, is_system FROM user_types WHERE deleted = 0 ORDER BY sort_order;
