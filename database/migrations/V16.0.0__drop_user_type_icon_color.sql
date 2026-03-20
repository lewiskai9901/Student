-- V16.0.0: 删除 user_types 表的 icon 和 color 列（不再使用）
ALTER TABLE user_types DROP COLUMN icon;
ALTER TABLE user_types DROP COLUMN color;
