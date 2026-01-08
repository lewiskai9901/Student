-- ====================================================================
-- 创建V3检查模板（简化版 - 仅创建模板主记录）
--
-- 目的: 将V1的3条量化类型配置迁移到V3检查模板系统
-- 说明: 此脚本仅创建模板主记录,类别和检查项请在系统UI中手动配置
-- 作者: Claude Code
-- 日期: 2025-11-23
-- 优先级: 🔴 高
-- ====================================================================

-- 检查是否已存在相同code的模板
SELECT
    '检查已存在的模板' as info,
    template_code,
    template_name,
    status
FROM check_templates
WHERE template_code IN ('DORMITORY_HYGIENE', 'CLASSROOM_HYGIENE', 'DISCIPLINE_CHECK')
  AND deleted = 0;

-- ====================================================================
-- 模板1: 宿舍卫生检查
-- ====================================================================

INSERT INTO check_templates (
    template_name,
    template_code,
    description,
    is_default,
    status,
    created_by,
    created_at,
    deleted
)
SELECT
    '宿舍卫生检查',
    'DORMITORY_HYGIENE',
    '宿舍卫生日常检查模板。包含地面整洁、物品摆放、床铺整理、垃圾清理、窗户清洁、安全隐患等检查项目。满分100分,权重1.0。',
    0,  -- is_default: 0=非默认
    1,  -- status: 1=启用
    1,  -- created_by: 系统管理员
    NOW(),
    0   -- deleted: 0=未删除
WHERE NOT EXISTS (
    SELECT 1 FROM check_templates
    WHERE template_code = 'DORMITORY_HYGIENE' AND deleted = 0
);

-- ====================================================================
-- 模板2: 教室卫生检查
-- ====================================================================

INSERT INTO check_templates (
    template_name,
    template_code,
    description,
    is_default,
    status,
    created_by,
    created_at,
    deleted
)
SELECT
    '教室卫生检查',
    'CLASSROOM_HYGIENE',
    '教室卫生日常检查模板。包含黑板清洁、地面整洁、桌椅整理、垃圾清理、窗户清洁等检查项目。满分100分,权重1.0。',
    0,  -- is_default: 0=非默认
    1,  -- status: 1=启用
    1,  -- created_by: 系统管理员
    NOW(),
    0   -- deleted: 0=未删除
WHERE NOT EXISTS (
    SELECT 1 FROM check_templates
    WHERE template_code = 'CLASSROOM_HYGIENE' AND deleted = 0
);

-- ====================================================================
-- 模板3: 纪律检查
-- ====================================================================

INSERT INTO check_templates (
    template_name,
    template_code,
    description,
    is_default,
    status,
    created_by,
    created_at,
    deleted
)
SELECT
    '纪律检查',
    'DISCIPLINE_CHECK',
    '学生纪律日常检查模板。包含迟到早退、课堂纪律、作业完成、校牌佩戴等检查项目。满分100分,权重0.8。',
    0,  -- is_default: 0=非默认
    1,  -- status: 1=启用
    1,  -- created_by: 系统管理员
    NOW(),
    0   -- deleted: 0=未删除
WHERE NOT EXISTS (
    SELECT 1 FROM check_templates
    WHERE template_code = 'DISCIPLINE_CHECK' AND deleted = 0
);

-- ====================================================================
-- 验证插入结果
-- ====================================================================

SELECT
    id,
    template_name,
    template_code,
    description,
    is_default,
    status,
    created_at
FROM check_templates
WHERE template_code IN ('DORMITORY_HYGIENE', 'CLASSROOM_HYGIENE', 'DISCIPLINE_CHECK')
  AND deleted = 0
ORDER BY id DESC;

-- ====================================================================
-- 后续手动操作说明:
-- ====================================================================
-- 1. 登录系统管理后台
-- 2. 进入 "量化管理" -> "检查模板管理"
-- 3. 编辑每个模板,添加类别和检查项:
--
-- 【宿舍卫生检查】:
--   - 地面整洁 (20分): 地面是否扫拭干净(10分), 是否有垃圾杂物(10分)
--   - 物品摆放 (20分): 物品摆放是否整齐规范(20分)
--   - 床铺整理 (20分): 床铺是否整理规范(20分)
--   - 垃圾清理 (15分): 垃圾是否及时清理(15分)
--   - 窗户清洁 (15分): 窗户玻璃是否干净(15分)
--   - 安全隐患 (10分): 是否存在安全隐患(10分)
--
-- 【教室卫生检查】:
--   - 黑板清洁 (15分): 黑板是否擦拭干净(15分)
--   - 地面整洁 (25分): 地面是否清扫干净(25分)
--   - 桌椅整理 (20分): 桌椅是否摆放整齐(20分)
--   - 垃圾清理 (20分): 垃圾是否及时清理(20分)
--   - 窗户清洁 (20分): 窗户是否保持干净(20分)
--
-- 【纪律检查】:
--   - 迟到早退 (30分): 学生迟到情况(15分), 学生早退情况(15分)
--   - 课堂纪律 (25分): 课堂秩序是否良好(25分)
--   - 作业完成 (25分): 作业完成情况(25分)
--   - 校牌佩戴 (20分): 学生校牌佩戴是否规范(20分)
--
-- 4. 设置检查频率:
--    - 宿舍卫生检查: 每日
--    - 教室卫生检查: 每日
--    - 纪律检查: 每周多次
--
-- 5. 保存并启用模板
-- ====================================================================

-- ====================================================================
-- 执行说明:
-- 1. 此脚本使用NOT EXISTS避免重复插入
-- 2. 如果模板已存在,不会报错,会跳过插入
-- 3. 执行后请在UI中完善类别和检查项配置
-- 4. created_by字段使用1(系统管理员ID),如需修改请调整
-- ====================================================================

-- ====================================================================
-- 回滚脚本 (如果需要)
-- ====================================================================
-- DELETE FROM check_templates
-- WHERE template_code IN ('DORMITORY_HYGIENE', 'CLASSROOM_HYGIENE', 'DISCIPLINE_CHECK')
--   AND created_at >= '2025-11-23 00:00:00';
-- ====================================================================
