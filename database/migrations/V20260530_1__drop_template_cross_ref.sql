-- ============================================================
-- Migration: V20260530_1__drop_template_cross_ref.sql
-- Description: 彻底删除两套已死的"模板互相引用"机制.
--
-- 死物 1 — 孤儿表 insp_template_module_refs:
--   V47 建 (配合 insp_templates.is_module / is_composite 的模板组合特性).
--   V58 重构删了 is_module/is_composite 后此表无任何后端代码消费
--   (无 PO / Mapper / Repository / Service, 旧 /module-refs 控制器已删).
--   纯孤儿表, DROP.
--
-- 死物 2 — insp_template_sections 上的分区引用列 ref_section_id / ref_template_id:
--   ref_template_id 由 V61 ADD, ref_section_id 由 V62 ADD (替代前者).
--   全链路从不解析: 发布快照不读 / 任务生成不读 / 前端无 UI,
--   唯一用到是 duplicate 时原样透传 ref_section_id, 纯死透传.
--   按"删字段同步删 DB 列"原则一并 DROP (含 V61 的 idx_ref_template 索引).
--
-- 全新建库一致性: V47 (建表) / V61 (ref_template_id+idx) / V62 (ref_section_id+idx)
--   在历史顺序里 CREATE/ADD 这些对象, 本迁移 (序号 V20260530_1) 排在它们之后,
--   叠加执行末尾把表与列删掉 — 全新库与升级库最终一致, 均不含这张表与两列.
--   保留历史 CREATE/ALTER 不动 (已应用迁移不可改), 靠本 DROP 兜底.
--
-- 幂等: 表 DROP IF EXISTS; 列/索引 information_schema 条件化 (存在才 DROP),
--   可重复执行, 缺对象不报错 (与 V97/V104/V20260529_3 风格一致).
-- ============================================================

-- ---------- 死物 1: 孤儿表 ----------
DROP TABLE IF EXISTS insp_template_module_refs;

-- ---------- 死物 2: insp_template_sections 引用列 + 索引 ----------

-- 先删 V61 建的 idx_ref_template 索引 (依赖 ref_template_id 列)
SET @c = (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='insp_template_sections' AND index_name='idx_ref_template');
SET @s = IF(@c>0,'ALTER TABLE insp_template_sections DROP INDEX idx_ref_template','SELECT 1'); PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- 再删 V62 建的 idx_sections_ref 索引 (依赖 ref_section_id 列)
SET @c = (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='insp_template_sections' AND index_name='idx_sections_ref');
SET @s = IF(@c>0,'ALTER TABLE insp_template_sections DROP INDEX idx_sections_ref','SELECT 1'); PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- 删 ref_template_id 列 (V61)
SET @c = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='insp_template_sections' AND column_name='ref_template_id');
SET @s = IF(@c>0,'ALTER TABLE insp_template_sections DROP COLUMN ref_template_id','SELECT 1'); PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- 删 ref_section_id 列 (V62)
SET @c = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='insp_template_sections' AND column_name='ref_section_id');
SET @s = IF(@c>0,'ALTER TABLE insp_template_sections DROP COLUMN ref_section_id','SELECT 1'); PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
