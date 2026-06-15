-- T8: data_resources 新增"②结果关系过滤"能力声明列。
-- 成员型资源 (如 user/system_user — 通过 access_relations 与用户挂钩) 支持轴②按关系过滤;
-- 普通组织锚定资源不支持, 默认 0。前端范围生成器据此决定是否渲染关系过滤入口。
ALTER TABLE data_resources
  ADD COLUMN subject_relation_filterable TINYINT(1) NOT NULL DEFAULT 0
  COMMENT '是否支持②结果关系过滤(成员型资源, 如 user)' AFTER type_entity;

UPDATE data_resources SET subject_relation_filterable = 1
  WHERE resource_code IN ('user', 'system_user');
