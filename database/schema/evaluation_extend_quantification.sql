-- =====================================================
-- 扩展量化系统表结构 - 支持综测对接
-- 版本: 1.0
-- 创建日期: 2025-11-28
-- 说明: 为现有量化系统表添加综测相关字段
-- =====================================================

SET NAMES utf8mb4;

-- =====================================================
-- 1. 扩展 check_items 表 - 添加行为类型关联
-- =====================================================
ALTER TABLE `check_items`
ADD COLUMN IF NOT EXISTS `behavior_type_id` BIGINT NULL COMMENT '行为类型ID(关联综测)' AFTER `status`,
ADD INDEX IF NOT EXISTS `idx_behavior_type` (`behavior_type_id`);

-- =====================================================
-- 2. 扩展 rating_levels 表 - 添加综测影响配置
-- =====================================================
ALTER TABLE `rating_levels`
ADD COLUMN IF NOT EXISTS `evaluation_effect_type` TINYINT DEFAULT 0 COMMENT '综测影响:0无影响,1加分,2扣分' AFTER `color`,
ADD COLUMN IF NOT EXISTS `evaluation_dimension` VARCHAR(20) NULL COMMENT '影响维度:MORAL/LABOR等' AFTER `evaluation_effect_type`,
ADD COLUMN IF NOT EXISTS `evaluation_score` DECIMAL(5,2) NULL COMMENT '影响分数' AFTER `evaluation_dimension`,
ADD COLUMN IF NOT EXISTS `special_role_effects` JSON NULL COMMENT '特殊角色配置:[{role:"DORM_LEADER",extraScore:1}]' AFTER `evaluation_score`;

-- =====================================================
-- 3. 扩展 check_record_items_v3 表 - 添加学生关联支持
-- =====================================================
-- 原表的 link_type 字段已支持: 0无 1宿舍 2教室
-- 需要扩展支持学生关联，添加 link_type=3 表示学生

-- 添加字段以支持直接关联学生
ALTER TABLE `check_record_items_v3`
ADD COLUMN IF NOT EXISTS `linked_student_ids` JSON NULL COMMENT '关联学生ID列表' AFTER `link_no`,
ADD COLUMN IF NOT EXISTS `behavior_type_id` BIGINT NULL COMMENT '行为类型ID' AFTER `linked_student_ids`,
ADD COLUMN IF NOT EXISTS `behavior_type_code` VARCHAR(50) NULL COMMENT '行为类型编码' AFTER `behavior_type_id`;

-- 更新 link_type 注释
ALTER TABLE `check_record_items_v3`
MODIFY COLUMN `link_type` TINYINT DEFAULT 0 COMMENT '关联类型(0无1宿舍2学生3教室)';

-- =====================================================
-- 4. 扩展 check_record_rating_results 表 - 添加综测影响快照
-- =====================================================
-- 检查是否存在该表，如果存在则扩展
ALTER TABLE `check_record_rating_results`
ADD COLUMN IF NOT EXISTS `evaluation_effect_type` TINYINT DEFAULT 0 COMMENT '综测影响类型(快照)' AFTER `score`,
ADD COLUMN IF NOT EXISTS `evaluation_dimension` VARCHAR(20) NULL COMMENT '影响维度(快照)' AFTER `evaluation_effect_type`,
ADD COLUMN IF NOT EXISTS `evaluation_score` DECIMAL(5,2) NULL COMMENT '影响分数(快照)' AFTER `evaluation_dimension`,
ADD COLUMN IF NOT EXISTS `special_role_effects` JSON NULL COMMENT '特殊角色配置(快照)' AFTER `evaluation_score`,
ADD COLUMN IF NOT EXISTS `student_effects_generated` TINYINT DEFAULT 0 COMMENT '是否已生成学生影响' AFTER `special_role_effects`;

-- =====================================================
-- 5. 为 check_records 主表添加综测同步状态
-- =====================================================
-- 检查不同版本的主表
-- V3版本
ALTER TABLE `check_records_v3`
ADD COLUMN IF NOT EXISTS `evaluation_synced` TINYINT DEFAULT 0 COMMENT '综测数据是否已同步' AFTER `remarks`,
ADD COLUMN IF NOT EXISTS `evaluation_sync_time` DATETIME NULL COMMENT '综测同步时间' AFTER `evaluation_synced`;

-- 如果存在新版本check_records表
-- ALTER TABLE `check_records`
-- ADD COLUMN IF NOT EXISTS `evaluation_synced` TINYINT DEFAULT 0 COMMENT '综测数据是否已同步' AFTER `remarks`,
-- ADD COLUMN IF NOT EXISTS `evaluation_sync_time` DATETIME NULL COMMENT '综测同步时间' AFTER `evaluation_synced`;

-- =====================================================
-- 完成
-- =====================================================
