-- V20260619_3: 统一数据归属 R2.2 前置① — 激活 8 个"裸奔"教务/排课模块的数据权限
-- 这些 mapper 声明了 @DataPermission 但无 data_resources 行 → interceptor 此前跳过 = 完全不过滤(越权面)。
-- 补行后按注解锚点(org_unit_id + creator)激活 org 范围过滤, 实现注解本意。
-- ⚠ 这是访问控制行为变更: org 范围角色看到的数据变少; ALL 范围角色因短路不受影响; 无配置默认 SELF。
-- semester_offering 不在此列(表 semester_offerings 不存在=死 mapper, 其注解已删)。
-- teacher_preference 无 org 维度(表无 org_unit_id), 仅按 teacher_id(creator)过滤。
-- 幂等。

INSERT IGNORE INTO `data_resources`
  (`resource_code`,`resource_name`,`domain_code`,`industry`,`domain_name`,`org_unit_field`,`creator_field`,`registered_by`,`sort_order`,`enabled`,`tenant_id`,`plugin_enabled`,`allowed_scopes`,`resource_kind`)
VALUES
  ('teaching_progress','教学进度','education','EDU','教育','org_unit_id','recorded_by','EducationPlugin',60,1,1,1,'["ALL","DEPARTMENT_AND_BELOW","DEPARTMENT","SELF","CUSTOM"]','PLAIN'),
  ('class_course_assignment','排课分配','education','EDU','教育','org_unit_id','created_by','EducationPlugin',61,1,1,1,'["ALL","DEPARTMENT_AND_BELOW","DEPARTMENT","SELF","CUSTOM"]','PLAIN'),
  ('course_evaluation','课程评教','education','EDU','教育','org_unit_id','created_by','EducationPlugin',62,1,1,1,'["ALL","DEPARTMENT_AND_BELOW","DEPARTMENT","SELF","CUSTOM"]','PLAIN'),
  ('evaluation_response','评教作答','education','EDU','教育','org_unit_id','student_id','EducationPlugin',63,1,1,1,'["ALL","DEPARTMENT_AND_BELOW","DEPARTMENT","SELF","CUSTOM"]','PLAIN'),
  ('scheduling_constraint','排课约束','education','EDU','教育','org_unit_id','created_by','EducationPlugin',64,1,1,1,'["ALL","DEPARTMENT_AND_BELOW","DEPARTMENT","SELF","CUSTOM"]','PLAIN'),
  ('schedule_entry','课表条目','education','EDU','教育','org_unit_id','created_by','EducationPlugin',65,1,1,1,'["ALL","DEPARTMENT_AND_BELOW","DEPARTMENT","SELF","CUSTOM"]','PLAIN'),
  ('schedule_conflict_record','排课冲突','education','EDU','教育','org_unit_id','created_by','EducationPlugin',66,1,1,1,'["ALL","DEPARTMENT_AND_BELOW","DEPARTMENT","SELF","CUSTOM"]','PLAIN'),
  ('teacher_preference','教师偏好','education','EDU','教育',NULL,'teacher_id','EducationPlugin',67,1,1,1,'["ALL","SELF"]','PLAIN');
