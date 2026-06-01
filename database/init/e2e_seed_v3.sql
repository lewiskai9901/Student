-- e2e_seed_v3.sql — CI E2E 最小业务数据 (对齐 baseline_v3 当前 schema, 2026-06-01)
--
-- 配合 baseline_v3.sql 使用: baseline 已含完整 schema + bootstrap 配置 (admin/角色/权限/类型),
-- 本文件只补 E2E 测试依赖的最小业务数据, 全部幂等 (INSERT IGNORE / NOT EXISTS):
--   - admin / teacher01 / teacher02 三账号 (密码均 admin123, teacher02 不同 hash)
--   - 角色绑定: admin→SUPER_ADMIN, teacher01/02→TEACHER
--   - 一个班级 org_unit (经济2024-1班, type_code=CLASS) + teacher01 班主任 assignment
--   - 组织归属 member 关系 (teacher01/02 → 各自系)
--
-- 与旧 ci_e2e_seed.sql 的区别 (旧文件对旧 schema, 已废弃):
--   * 删 user_types 插入 (表已 DROP, 类型走 entity_type_configs, app 启动自举)
--   * 删 ~777 条 permissions/role_permissions (registrar 启动自举, seed 冗余)
--   * org_units 去掉已删列 leader_id/deputy_leader_ids
--   * teacher_assignments 去掉已删列 is_primary/workload_hours/remark
--   * user_roles 去掉不存在列 created_by

SET NAMES utf8mb4;
SET @OLD_FK := @@FOREIGN_KEY_CHECKS;
SET FOREIGN_KEY_CHECKS = 0;

-- 1. 账号 (baseline_v3 已含, IGNORE 兜底独立运行) -------------------------
INSERT IGNORE INTO `users`
  (`id`,`username`,`password`,`real_name`,`phone`,`user_type_code`,`status`,`password_changed_at`,`allow_multiple_devices`,`created_at`,`updated_at`,`deleted`,`tenant_id`) VALUES
  (1,'admin','$2a$10$NBILuC13J0f71JBhs8sr5uiB02XDpR4BF9uhki3dDsXEaIGanmR6e','超级管理员','13800000000','SUPER_ADMIN',1,'2026-04-04 19:25:03',0,'2025-11-05 22:49:34','2026-04-04 19:25:03',0,1),
  (2041870507300622337,'teacher01','$2a$10$NBILuC13J0f71JBhs8sr5uiB02XDpR4BF9uhki3dDsXEaIGanmR6e','张明','13810000001','TEACHER',1,'2026-04-08 21:27:20',0,'2026-04-08 21:27:20','2026-04-08 21:27:20',0,1),
  (2041870508646993922,'teacher02','$2a$10$x7vfQ9F.3zCV1WOM8QlxJuGmIm.cNiN3wt1aE4dSN/hE2/bYC3BTC','李华','13810000002','TEACHER',1,'2026-04-08 21:27:20',0,'2026-04-08 21:27:20','2026-04-08 21:27:20',0,1);

-- 2. 角色绑定 (SUPER_ADMIN=1, TEACHER=2022900002094850049) -----------------
INSERT IGNORE INTO `user_roles`
  (`id`,`user_id`,`role_id`,`tenant_id`,`scope_type`,`scope_id`,`assigned_at`,`is_active`,`created_at`) VALUES
  (2040422302595776514,1,1,1,'ALL',0,'2026-04-04 21:32:41',1,'2026-04-04 21:32:40'),
  (2040422302667079683,2041870507300622337,2022900002094850049,1,'ALL',0,'2026-04-08 21:27:19',1,'2026-04-08 21:27:19'),
  (2040422302667079684,2041870508646993922,2022900002094850049,1,'ALL',0,'2026-04-08 21:27:20',1,'2026-04-08 21:27:20');

-- 3. 班级 org_unit (type_code=CLASS → classes 视图可见) ---------------------
INSERT IGNORE INTO `org_units`
  (`id`,`unit_code`,`unit_name`,`unit_type`,`type_code`,`parent_id`,`tree_path`,`tree_level`,`sort_order`,`created_at`,`updated_at`,`created_by`,`deleted`,`unit_category`,`tenant_id`,`attributes`,`version`,`status`) VALUES
  (2041867776338956290,'CLS-0001','经济2024-1班','CLASS','CLASS',2041867773310668802,
   '/2040636119007150081/2040636269108707330/2041867773310668802/2041867776338956290/',4,0,
   '2026-04-08 21:16:29','2026-04-08 21:16:29',1,0,'academic',1,
   '{"classType": 1, "headTeacher": "2041870507300622337", "enrollmentYear": 2024}',1,'ACTIVE');

-- 4. 班主任 assignment -----------------------------------------------------
INSERT IGNORE INTO `teacher_assignments`
  (`id`,`org_unit_id`,`teacher_id`,`teacher_name`,`role_type`,`start_date`,`status`,`created_at`,`updated_at`,`tenant_id`,`role`,`is_current`,`deleted`) VALUES
  (2041867776339055290,2041867776338956290,2041870507300622337,'张明','HEAD_TEACHER','2024-09-01','ACTIVE',
   '2026-04-08 21:36:11','2026-04-08 21:36:11',1,'HEAD_TEACHER',1,0);

-- 5. 组织归属 member 关系 (每用户唯一, is_primary=1; NOT EXISTS 防与 uk_membership_unique 冲突) ---
INSERT INTO `access_relations`
    (`resource_type`,`resource_id`,`relation`,`subject_type`,`subject_id`,
     `is_primary`,`access_level`,`deleted`,`tenant_id`,`created_at`,`updated_at`)
SELECT 'org_unit', m.org_unit_id, 'member', 'user', m.user_id,
       1,'READ',0,1,NOW(),NOW()
FROM (
    SELECT 2041870507300622337 AS user_id, 2040636269108707330 AS org_unit_id  -- teacher01 → 经济与信息技术系
    UNION ALL
    SELECT 2041870508646993922, 2041864691411632129                            -- teacher02 → 汽车工程系
) m
JOIN `users` u ON u.`id` = m.user_id AND u.`deleted` = 0
WHERE NOT EXISTS (
      SELECT 1 FROM `access_relations` ar
      WHERE ar.`relation`='member' AND ar.`subject_type`='user'
        AND ar.`subject_id`=m.user_id AND ar.`resource_type`='org_unit' AND ar.`deleted`=0);

SET FOREIGN_KEY_CHECKS = @OLD_FK;
