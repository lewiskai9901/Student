-- 去教育化: DEPT_ADMIN 角色描述"本部门及下级" -> "本组织及下级"
UPDATE roles SET role_desc = '系/院级管理角色，可查看本组织及下级数据'
WHERE role_code = 'DEPT_ADMIN'
  AND role_desc LIKE '%本部门及下级%';
