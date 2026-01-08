-- ============================================
-- 学生管理系统 - 部署前安全脚本
-- ============================================
-- 在生产环境部署前执行此脚本
--
-- 步骤1: 生成新密码的BCrypt哈希
--   可使用在线工具: https://bcrypt-generator.com/
--   或使用Java: BCryptPasswordEncoder().encode("你的新密码")
--
-- 步骤2: 替换下面的 'YOUR_BCRYPT_HASH' 为实际的哈希值
--
-- 步骤3: 执行此SQL脚本
-- ============================================

-- ==================== 1. 更新管理员密码 ====================
-- 请将 'YOUR_BCRYPT_HASH' 替换为新密码的BCrypt哈希
-- 示例: 密码 "Admin@2024!Secure" 的BCrypt哈希
-- UPDATE users SET password = '$2a$10$XXXXXXXXXXXXXXXXXXXXXX',
--        password_changed_at = NOW(), updated_at = NOW()
-- WHERE username = 'admin';

-- ==================== 2. 禁用测试账号 ====================
-- 禁用所有测试账号
UPDATE users SET status = 0, updated_at = NOW()
WHERE username IN ('testuser', 'teacher001', 'inspector001')
  AND status = 1;

-- ==================== 3. 清理测试数据 (可选) ====================
-- 如果需要，取消下面的注释以删除测试数据
-- DELETE FROM students WHERE student_no LIKE 'TEST%';
-- DELETE FROM classes WHERE class_code LIKE 'TEST%';

-- ==================== 4. 验证安全配置 ====================
-- 检查是否还有使用默认密码的账号
SELECT username, real_name, status,
       CASE WHEN password = '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3NL1w3lFvXHSgUBVr.S2'
            THEN '⚠️ 默认密码未修改'
            ELSE '✓ 密码已修改'
       END as password_status
FROM users
WHERE user_type = 1 AND deleted = 0;

-- ==================== 5. 检查禁用的测试账号 ====================
SELECT username, real_name, status,
       CASE status WHEN 0 THEN '✓ 已禁用' ELSE '⚠️ 仍然启用' END as account_status
FROM users
WHERE username IN ('testuser', 'teacher001', 'inspector001');

-- ============================================
-- 部署后首次登录提示
-- ============================================
-- 建议: 部署后立即登录管理后台修改admin密码
-- 密码要求: 至少8位，包含大小写字母、数字和特殊字符
-- ============================================
