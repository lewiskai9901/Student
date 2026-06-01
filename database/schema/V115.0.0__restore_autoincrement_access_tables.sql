-- V115.0.0 恢复核心访问表 id 的 AUTO_INCREMENT (修 fresh-init 债, 归属重构无关)
-- 背景: baseline 漂移导致 permissions/roles/role_permissions/user_roles 的 id 丢了 AUTO_INCREMENT。
--   启动期 PermissionRegistrar / RoleRegistrar 用裸 JdbcTemplate INSERT 不带 id, 以及 seed 同样,
--   触发 ERROR 1364 Field 'id' doesn't have a default value → 启动崩。线上库这些表本是自增。
-- MODIFY 幂等(已是 auto_increment 重复执行无害)。

SET NAMES utf8mb4;

ALTER TABLE `permissions`      MODIFY `id` BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE `roles`            MODIFY `id` BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE `role_permissions` MODIFY `id` BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE `user_roles`       MODIFY `id` BIGINT NOT NULL AUTO_INCREMENT;
