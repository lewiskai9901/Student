-- V61.0.0: 分区树形结构 + 引用模板 + 内嵌评分配置
-- parent_section_id 已存在（未被 V40 删除），仅添加 ref_template_id 和 scoring_config

ALTER TABLE `insp_template_sections`
  ADD COLUMN `ref_template_id` BIGINT NULL AFTER `parent_section_id`,
  ADD COLUMN `scoring_config` JSON NULL AFTER `condition_logic`;

ALTER TABLE `insp_template_sections`
  ADD INDEX `idx_ref_template` (`ref_template_id`);
