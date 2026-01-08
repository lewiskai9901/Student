-- 修复测试账号密码
-- 将所有测试账号密码设置为 admin123 (与admin账号相同)

UPDATE users
SET password = '$2a$10$CTSYb4i8PaYdk3cR8W0Db.rIGZJ5EfIDbLMij2VL1qvLtAFUTsZQS'
WHERE id IN (1001, 2001, 3001, 1101, 1102, 1103, 1104);

-- 验证更新
SELECT id, username, real_name,
       SUBSTRING(password, 1, 20) as password_prefix
FROM users
WHERE id IN (1001, 2001, 3001, 1101, 1102, 1103, 1104)
ORDER BY id;
