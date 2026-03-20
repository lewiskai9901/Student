-- 修复: 软删除的用户仍占用 username 唯一约束
-- 将唯一索引从 (username) 改为 (username, deleted)，允许已删除用户的 username 被重新使用

ALTER TABLE users DROP INDEX uk_username;
ALTER TABLE users ADD UNIQUE KEY uk_username_deleted (username, deleted);
