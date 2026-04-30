-- review #13: 注册 insp:audit:view 权限 + inspection_audit 数据资源
-- 让审计日志查询 API 走 Casbin 鉴权

-- 数据资源 (允许 ALL/SELF 范围)
INSERT IGNORE INTO data_resources (module_code, resource_code, resource_name, allowed_scopes, tenant_id)
VALUES ('inspection', 'inspection_audit', '检查审计日志', 'ALL,SELF', 1);

-- Casbin 权限点
INSERT IGNORE INTO casbin_rule (ptype, v0, v1, v2, v3, v4, v5)
VALUES ('p', 'role:1', 'insp:audit', 'view', '', '', ''),
       ('p', 'role:2', 'insp:audit', 'view', '', '', '');
