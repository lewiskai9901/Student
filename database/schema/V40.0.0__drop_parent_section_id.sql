-- Remove parent_section_id: sections are now flat, no hierarchy
ALTER TABLE `insp_template_sections` DROP INDEX `idx_parent`;
ALTER TABLE `insp_template_sections` DROP COLUMN `parent_section_id`;
