ALTER TABLE insp_template_sections
  ADD COLUMN input_mode VARCHAR(30) DEFAULT 'INLINE' COMMENT '输入模式: INLINE(结构化检查) | EVENT_STREAM(巡查快速记录)';
