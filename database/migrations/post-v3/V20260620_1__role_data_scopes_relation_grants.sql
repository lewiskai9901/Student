-- ============================================================================
-- V20260620_1: 统一锚定 R3a-2 — role_data_scopes 加 relation_grants 列 + 回填现有行
--
-- R3 把"单组织锚点(M1 三轴)"升级为"N 条关系授予 OR"。本迁移加 relation_grants JSON 列,
-- 并把现有行的 M1 轴① (org_anchor/anchor_param/include_subtree/custom_org_ids) 翻译成等价
-- 单 grant (字节等价, 见 RelationGrant.fromM1Axes)。轴②/③ 现零数据, 不处理。
--
-- SELF 按资源 storage 分流: 成员型资源 (owner_org=SUBJECT_GRAPH, 即 student/user) → member-self
-- (owner_org, SELF); 列锚资源 → creator (creator, SELF) —— 与引擎 meta.viaMembership 一致。
-- subtree 用 CAST(... AS JSON) 产真 JSON 布尔 (避免 Jackson 数字→布尔强制的歧义)。
--
-- 注: M1 轴①②列暂留 (R3a-2b 删); getScopeSpec 改读 relation_grants, relation_grants 为空的行
-- (新建/未回填) 仍由引擎轴① bridge 兜底。
-- ============================================================================

ALTER TABLE `role_data_scopes` ADD COLUMN `relation_grants` json DEFAULT NULL
  COMMENT 'R3 关系授予 [{relation,subject,subjectParam?,subtree?,orgIds?}]; NULL=引擎轴① bridge 派生';

UPDATE `role_data_scopes` SET `relation_grants` = CASE
  WHEN `org_anchor` = 'ALL'
       THEN JSON_ARRAY(JSON_OBJECT('relation','owner_org','subject','ALL'))
  WHEN `org_anchor` = 'SELF' AND `resource_code` IN ('student','user')
       THEN JSON_ARRAY(JSON_OBJECT('relation','owner_org','subject','SELF'))
  WHEN `org_anchor` = 'SELF'
       THEN JSON_ARRAY(JSON_OBJECT('relation','creator','subject','SELF'))
  WHEN `org_anchor` = 'PRIMARY_ORG'
       THEN JSON_ARRAY(JSON_OBJECT('relation','owner_org','subject','MY_ORG',
              'subtree', IF(`include_subtree` = 1, CAST('true' AS JSON), CAST('false' AS JSON))))
  WHEN `org_anchor` = 'PLUGIN_DIM'
       THEN JSON_ARRAY(JSON_OBJECT('relation','owner_org','subject','PLUGIN_DIM','subjectParam', `anchor_param`))
  WHEN `org_anchor` = 'RELATION'
       THEN JSON_ARRAY(JSON_OBJECT('relation','owner_org','subject','RELATION','subjectParam', `anchor_param`,
              'subtree', IF(`include_subtree` = 1, CAST('true' AS JSON), CAST('false' AS JSON))))
  WHEN `org_anchor` = 'CUSTOM_ORG'
       THEN JSON_ARRAY(JSON_OBJECT('relation','owner_org','subject','CUSTOM',
              'subtree', IF(`include_subtree` = 1, CAST('true' AS JSON), CAST('false' AS JSON)),
              'orgIds', `custom_org_ids`))
  ELSE NULL END
WHERE `deleted` = 0 AND `org_anchor` IS NOT NULL;
