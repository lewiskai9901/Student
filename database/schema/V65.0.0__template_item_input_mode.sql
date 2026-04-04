ALTER TABLE insp_template_items
  ADD COLUMN input_mode VARCHAR(30) DEFAULT 'INLINE' COMMENT '输入模式: INLINE(默认内嵌) | EVENT_STREAM(事件流快速录入)';
