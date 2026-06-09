-- dev_custom_scope_fixtures.sql — CUSTOM data-scope dev/test fixtures (2026-06-09)
--
-- Purpose: deterministic org tree (dept → grades → classes) + students wired to their
-- class ONLY via access_relations `member` tuples (the Zanzibar substrate documented in
-- docs/plans/2026-06-07-model-findings.md). Lets CUSTOM data-scope grants on a CLASS/GRADE
-- org_unit be exercised end-to-end.
--
-- Idempotent: re-runnable, 0 rows changed on 2nd run (INSERT ... ON DUPLICATE KEY UPDATE
-- on natural/unique keys; access_relations honors uk_relation + generated uk_membership_unique).
-- All rows tenant_id=1. Sentinel ids in a fixed high block so cleanup is trivial.
-- NOTE: ids stay below signed BIGINT max (9223372036854775807) — access_relations.id is a
-- signed bigint, so the "94e17" scheme would overflow; we use a 9.0e18-based block instead:
--   org_units         9000000000000001001..9000000000000001006
--   users             9000000000000002001..9000000000000002006
--   user_student      9000000000000003001..9000000000000003006
--   access_relations  9000000000000004001..9000000000000004006
--
-- Hierarchy:
--   DPTEST 学院 (DEPARTMENT, root, id ...1001)
--   ├─ DPTEST 2024级 (GRADE, ...1002)
--   │  ├─ DPTEST 一班 (CLASS, ...1004)  → 2 students
--   │  └─ DPTEST 二班 (CLASS, ...1005)  → 2 students
--   └─ DPTEST 2025级 (GRADE, ...1003)
--      └─ DPTEST 三班 (CLASS, ...1006)  → 2 students
--
-- Cleanup (if ever needed):
--   DELETE FROM access_relations WHERE id BETWEEN 9000000000000004001 AND 9000000000000004006;
--   DELETE FROM user_student     WHERE id BETWEEN 9000000000000003001 AND 9000000000000003006;
--   DELETE FROM users            WHERE id BETWEEN 9000000000000002001 AND 9000000000000002006;
--   DELETE FROM org_units        WHERE id BETWEEN 9000000000000001001 AND 9000000000000001006;

SET NAMES utf8mb4;
SET @OLD_FK := @@FOREIGN_KEY_CHECKS;
SET FOREIGN_KEY_CHECKS = 0;

-- ---------------------------------------------------------------------------
-- 1. org_units — 1 root DEPARTMENT + 2 GRADE + 3 CLASS
--    tree_path convention = '/<ancestor>/.../<self>/' (leading+trailing slash, self included),
--    matching e2e_seed_v3.sql:38-41.
-- ---------------------------------------------------------------------------
INSERT INTO `org_units`
  (`id`,`unit_code`,`unit_name`,`unit_type`,`type_code`,`unit_category`,`parent_id`,`tree_path`,`tree_level`,`sort_order`,`deleted`,`tenant_id`,`attributes`,`version`,`status`,`created_by`)
VALUES
  -- root department
  (9000000000000001001,'DPTEST-ROOT','DPTEST 学院','DEPARTMENT','DEPARTMENT','academic',NULL,
   '/9000000000000001001/',1,0,0,1,NULL,1,'ACTIVE',1),
  -- grades (children of root)
  (9000000000000001002,'DPTEST-G2024','DPTEST 2024级','GRADE','GRADE','academic',9000000000000001001,
   '/9000000000000001001/9000000000000001002/',2,0,0,1,'{"enrollmentYear": 2024}',1,'ACTIVE',1),
  (9000000000000001003,'DPTEST-G2025','DPTEST 2025级','GRADE','GRADE','academic',9000000000000001001,
   '/9000000000000001001/9000000000000001003/',2,1,0,1,'{"enrollmentYear": 2025}',1,'ACTIVE',1),
  -- classes under 2024级
  (9000000000000001004,'DPTEST-C1','DPTEST 一班','CLASS','CLASS','academic',9000000000000001002,
   '/9000000000000001001/9000000000000001002/9000000000000001004/',3,0,0,1,
   '{"classType": 1, "gradeId": 9000000000000001002, "enrollmentYear": 2024}',1,'ACTIVE',1),
  (9000000000000001005,'DPTEST-C2','DPTEST 二班','CLASS','CLASS','academic',9000000000000001002,
   '/9000000000000001001/9000000000000001002/9000000000000001005/',3,1,0,1,
   '{"classType": 1, "gradeId": 9000000000000001002, "enrollmentYear": 2024}',1,'ACTIVE',1),
  -- class under 2025级
  (9000000000000001006,'DPTEST-C3','DPTEST 三班','CLASS','CLASS','academic',9000000000000001003,
   '/9000000000000001001/9000000000000001003/9000000000000001006/',3,0,0,1,
   '{"classType": 1, "gradeId": 9000000000000001003, "enrollmentYear": 2025}',1,'ACTIVE',1)
ON DUPLICATE KEY UPDATE
  `unit_name`=VALUES(`unit_name`),`unit_type`=VALUES(`unit_type`),`type_code`=VALUES(`type_code`),
  `unit_category`=VALUES(`unit_category`),`parent_id`=VALUES(`parent_id`),`tree_path`=VALUES(`tree_path`),
  `tree_level`=VALUES(`tree_level`),`sort_order`=VALUES(`sort_order`),`attributes`=VALUES(`attributes`),
  `status`=VALUES(`status`),`deleted`=VALUES(`deleted`);

-- ---------------------------------------------------------------------------
-- 2. users — 6 student accounts (the join subject for member tuples)
-- ---------------------------------------------------------------------------
INSERT INTO `users`
  (`id`,`username`,`password`,`real_name`,`user_type_code`,`status`,`deleted`,`tenant_id`)
VALUES
  (9000000000000002001,'dptest_stu1','$2a$10$NBILuC13J0f71JBhs8sr5uiB02XDpR4BF9uhki3dDsXEaIGanmR6e','DPTEST 学生一','STUDENT',1,0,1),
  (9000000000000002002,'dptest_stu2','$2a$10$NBILuC13J0f71JBhs8sr5uiB02XDpR4BF9uhki3dDsXEaIGanmR6e','DPTEST 学生二','STUDENT',1,0,1),
  (9000000000000002003,'dptest_stu3','$2a$10$NBILuC13J0f71JBhs8sr5uiB02XDpR4BF9uhki3dDsXEaIGanmR6e','DPTEST 学生三','STUDENT',1,0,1),
  (9000000000000002004,'dptest_stu4','$2a$10$NBILuC13J0f71JBhs8sr5uiB02XDpR4BF9uhki3dDsXEaIGanmR6e','DPTEST 学生四','STUDENT',1,0,1),
  (9000000000000002005,'dptest_stu5','$2a$10$NBILuC13J0f71JBhs8sr5uiB02XDpR4BF9uhki3dDsXEaIGanmR6e','DPTEST 学生五','STUDENT',1,0,1),
  (9000000000000002006,'dptest_stu6','$2a$10$NBILuC13J0f71JBhs8sr5uiB02XDpR4BF9uhki3dDsXEaIGanmR6e','DPTEST 学生六','STUDENT',1,0,1)
ON DUPLICATE KEY UPDATE
  `real_name`=VALUES(`real_name`),`user_type_code`=VALUES(`user_type_code`),
  `status`=VALUES(`status`),`deleted`=VALUES(`deleted`);

-- ---------------------------------------------------------------------------
-- 3. user_student — 6 student rows, user_id = the matching users.id (the join key).
--    No class/grade column exists; membership lives in access_relations (step 4).
-- ---------------------------------------------------------------------------
INSERT INTO `user_student`
  (`id`,`student_no`,`name`,`gender`,`status`,`deleted`,`tenant_id`,`user_id`)
VALUES
  (9000000000000003001,'DPTEST-2024001','DPTEST 学生一',1,1,0,1,9000000000000002001),
  (9000000000000003002,'DPTEST-2024002','DPTEST 学生二',2,1,0,1,9000000000000002002),
  (9000000000000003003,'DPTEST-2024003','DPTEST 学生三',1,1,0,1,9000000000000002003),
  (9000000000000003004,'DPTEST-2024004','DPTEST 学生四',2,1,0,1,9000000000000002004),
  (9000000000000003005,'DPTEST-2025001','DPTEST 学生五',1,1,0,1,9000000000000002005),
  (9000000000000003006,'DPTEST-2025002','DPTEST 学生六',2,1,0,1,9000000000000002006)
ON DUPLICATE KEY UPDATE
  `name`=VALUES(`name`),`gender`=VALUES(`gender`),`status`=VALUES(`status`),
  `deleted`=VALUES(`deleted`),`user_id`=VALUES(`user_id`);

-- ---------------------------------------------------------------------------
-- 4. access_relations — 6 member tuples (student user_id → class org_unit id).
--    This is what makes each class non-empty for CUSTOM-scope class/grade filters.
--    一班(1004): stu1,stu2 | 二班(1005): stu3,stu4 | 三班(1006): stu5,stu6
--    Explicit ids keep ON DUPLICATE on PK stable; the natural uk_relation
--    (resource_type,resource_id,relation,subject_type,subject_id,deleted) + the
--    generated uk_membership_unique are both honored (each user has exactly one
--    active member tuple). Do NOT insert is_primary/membership_lock_key — generated.
-- ---------------------------------------------------------------------------
INSERT INTO `access_relations`
  (`id`,`resource_type`,`resource_id`,`relation`,`subject_type`,`subject_id`,`access_level`,`deleted`,`tenant_id`)
VALUES
  (9000000000000004001,'org_unit',9000000000000001004,'member','user',9000000000000002001,'READ',0,1),
  (9000000000000004002,'org_unit',9000000000000001004,'member','user',9000000000000002002,'READ',0,1),
  (9000000000000004003,'org_unit',9000000000000001005,'member','user',9000000000000002003,'READ',0,1),
  (9000000000000004004,'org_unit',9000000000000001005,'member','user',9000000000000002004,'READ',0,1),
  (9000000000000004005,'org_unit',9000000000000001006,'member','user',9000000000000002005,'READ',0,1),
  (9000000000000004006,'org_unit',9000000000000001006,'member','user',9000000000000002006,'READ',0,1)
ON DUPLICATE KEY UPDATE
  `resource_id`=VALUES(`resource_id`),`access_level`=VALUES(`access_level`),`deleted`=VALUES(`deleted`);

SET FOREIGN_KEY_CHECKS = @OLD_FK;
