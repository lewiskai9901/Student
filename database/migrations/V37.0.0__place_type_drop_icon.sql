-- V37.0.0: Remove icon column from place_types (no longer used in UI) — MySQL 8.0 条件化
SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='place_types' AND COLUMN_NAME='icon');
SET @s := IF(@c>0, "ALTER TABLE place_types DROP COLUMN icon", "SELECT 1");
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
