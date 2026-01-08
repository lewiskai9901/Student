-- ====================================================================
-- 创建V3检查模板（迁移V1量化类型配置）
--
-- 目的: 将V1的3条量化类型配置迁移到V3检查模板系统
-- 作者: Claude Code
-- 日期: 2025-11-23
-- 优先级: 🔴 高 - 数据迁移
-- ====================================================================

-- V1配置数据参考:
-- 1. 宿舍卫生检查 (DORMITORY_HYGIENE) - 满分100, 权重1.0
-- 2. 教室卫生检查 (CLASSROOM_HYGIENE) - 满分100, 权重1.0
-- 3. 纪律检查 (DISCIPLINE_CHECK) - 满分100, 权重0.8

-- ====================================================================
-- 模板1: 宿舍卫生检查模板
-- ====================================================================

INSERT INTO check_templates (
    template_name,
    template_code,
    template_desc,
    check_frequency,
    max_score,
    categories,
    status,
    created_at,
    created_by,
    deleted
)
VALUES (
    '宿舍卫生检查',
    'DORMITORY_HYGIENE',
    '宿舍卫生日常检查模板，包含地面整洁、物品摆放、床铺整理、垃圾清理、窗户清洁、安全隐患等检查项目。每项按实际扣分计算。',
    1,  -- 检查频率: 1=每日
    100.00,  -- 满分100分
    JSON_ARRAY(
        JSON_OBJECT(
            'categoryName', '地面整洁',
            'categoryCode', 'FLOOR_CLEAN',
            'maxScore', 20,
            'weight', 1.0,
            'items', JSON_ARRAY(
                JSON_OBJECT('itemName', '地面是否扫拭干净', 'maxScore', 10),
                JSON_OBJECT('itemName', '是否有垃圾杂物', 'maxScore', 10)
            )
        ),
        JSON_OBJECT(
            'categoryName', '物品摆放',
            'categoryCode', 'ITEMS_PLACEMENT',
            'maxScore', 20,
            'weight', 1.0,
            'items', JSON_ARRAY(
                JSON_OBJECT('itemName', '物品摆放是否整齐规范', 'maxScore', 20)
            )
        ),
        JSON_OBJECT(
            'categoryName', '床铺整理',
            'categoryCode', 'BED_MAKING',
            'maxScore', 20,
            'weight', 1.0,
            'items', JSON_ARRAY(
                JSON_OBJECT('itemName', '床铺是否整理规范', 'maxScore', 20)
            )
        ),
        JSON_OBJECT(
            'categoryName', '垃圾清理',
            'categoryCode', 'GARBAGE_DISPOSAL',
            'maxScore', 15,
            'weight', 1.0,
            'items', JSON_ARRAY(
                JSON_OBJECT('itemName', '垃圾是否及时清理', 'maxScore', 15)
            )
        ),
        JSON_OBJECT(
            'categoryName', '窗户清洁',
            'categoryCode', 'WINDOW_CLEAN',
            'maxScore', 15,
            'weight', 1.0,
            'items', JSON_ARRAY(
                JSON_OBJECT('itemName', '窗户玻璃是否干净', 'maxScore', 15)
            )
        ),
        JSON_OBJECT(
            'categoryName', '安全隐患',
            'categoryCode', 'SAFETY_HAZARD',
            'maxScore', 10,
            'weight', 1.0,
            'items', JSON_ARRAY(
                JSON_OBJECT('itemName', '是否存在安全隐患', 'maxScore', 10)
            )
        )
    ),
    1,  -- status: 1=启用
    NOW(),
    1,  -- created_by: 系统管理员
    0   -- deleted: 0=未删除
);

-- ====================================================================
-- 模板2: 教室卫生检查模板
-- ====================================================================

INSERT INTO check_templates (
    template_name,
    template_code,
    template_desc,
    check_frequency,
    max_score,
    categories,
    status,
    created_at,
    created_by,
    deleted
)
VALUES (
    '教室卫生检查',
    'CLASSROOM_HYGIENE',
    '教室卫生日常检查模板，包含黑板清洁、地面整洁、桌椅整理、垃圾清理、窗户清洁等检查项目。每项按实际扣分计算。',
    1,  -- 检查频率: 1=每日
    100.00,  -- 满分100分
    JSON_ARRAY(
        JSON_OBJECT(
            'categoryName', '黑板清洁',
            'categoryCode', 'BLACKBOARD_CLEAN',
            'maxScore', 15,
            'weight', 1.0,
            'items', JSON_ARRAY(
                JSON_OBJECT('itemName', '黑板是否擦拭干净', 'maxScore', 15)
            )
        ),
        JSON_OBJECT(
            'categoryName', '地面整洁',
            'categoryCode', 'FLOOR_CLEAN',
            'maxScore', 25,
            'weight', 1.0,
            'items', JSON_ARRAY(
                JSON_OBJECT('itemName', '地面是否清扫干净', 'maxScore', 25)
            )
        ),
        JSON_OBJECT(
            'categoryName', '桌椅整理',
            'categoryCode', 'DESK_ARRANGEMENT',
            'maxScore', 20,
            'weight', 1.0,
            'items', JSON_ARRAY(
                JSON_OBJECT('itemName', '桌椅是否摆放整齐', 'maxScore', 20)
            )
        ),
        JSON_OBJECT(
            'categoryName', '垃圾清理',
            'categoryCode', 'GARBAGE_DISPOSAL',
            'maxScore', 20,
            'weight', 1.0,
            'items', JSON_ARRAY(
                JSON_OBJECT('itemName', '垃圾是否及时清理', 'maxScore', 20)
            )
        ),
        JSON_OBJECT(
            'categoryName', '窗户清洁',
            'categoryCode', 'WINDOW_CLEAN',
            'maxScore', 20,
            'weight', 1.0,
            'items', JSON_ARRAY(
                JSON_OBJECT('itemName', '窗户是否保持干净', 'maxScore', 20)
            )
        )
    ),
    1,  -- status: 1=启用
    NOW(),
    1,  -- created_by: 系统管理员
    0   -- deleted: 0=未删除
);

-- ====================================================================
-- 模板3: 纪律检查模板
-- ====================================================================

INSERT INTO check_templates (
    template_name,
    template_code,
    template_desc,
    check_frequency,
    max_score,
    categories,
    status,
    created_at,
    created_by,
    deleted
)
VALUES (
    '纪律检查',
    'DISCIPLINE_CHECK',
    '学生纪律日常检查模板，包含迟到早退、课堂纪律、作业完成、校牌佩戴等检查项目。每项按实际扣分计算。权重0.8。',
    4,  -- 检查频率: 4=每周多次
    100.00,  -- 满分100分
    JSON_ARRAY(
        JSON_OBJECT(
            'categoryName', '迟到早退',
            'categoryCode', 'ATTENDANCE',
            'maxScore', 30,
            'weight', 0.8,
            'items', JSON_ARRAY(
                JSON_OBJECT('itemName', '学生迟到情况', 'maxScore', 15),
                JSON_OBJECT('itemName', '学生早退情况', 'maxScore', 15)
            )
        ),
        JSON_OBJECT(
            'categoryName', '课堂纪律',
            'categoryCode', 'CLASS_DISCIPLINE',
            'maxScore', 25,
            'weight', 0.8,
            'items', JSON_ARRAY(
                JSON_OBJECT('itemName', '课堂秩序是否良好', 'maxScore', 25)
            )
        ),
        JSON_OBJECT(
            'categoryName', '作业完成',
            'categoryCode', 'HOMEWORK',
            'maxScore', 25,
            'weight', 0.8,
            'items', JSON_ARRAY(
                JSON_OBJECT('itemName', '作业完成情况', 'maxScore', 25)
            )
        ),
        JSON_OBJECT(
            'categoryName', '校牌佩戴',
            'categoryCode', 'BADGE_WEARING',
            'maxScore', 20,
            'weight', 0.8,
            'items', JSON_ARRAY(
                JSON_OBJECT('itemName', '学生校牌佩戴是否规范', 'maxScore', 20)
            )
        )
    ),
    1,  -- status: 1=启用
    NOW(),
    1,  -- created_by: 系统管理员
    0   -- deleted: 0=未删除
);

-- ====================================================================
-- 验证插入结果
-- ====================================================================

SELECT
    id,
    template_name,
    template_code,
    max_score,
    check_frequency,
    status,
    created_at
FROM check_templates
WHERE template_code IN ('DORMITORY_HYGIENE', 'CLASSROOM_HYGIENE', 'DISCIPLINE_CHECK')
ORDER BY id DESC
LIMIT 3;

-- ====================================================================
-- 执行说明:
-- 1. 此脚本将V1的3条量化类型配置迁移到V3检查模板
-- 2. 使用JSON格式存储类别和检查项
-- 3. 保持了原有的评分标准和权重
-- 4. 执行后会显示新创建的3个模板
-- 5. 如果模板已存在(template_code唯一约束),会报错,请先检查
-- ====================================================================

-- 检查是否已存在相同code的模板
SELECT
    'ALREADY_EXISTS' as status,
    template_code,
    template_name
FROM check_templates
WHERE template_code IN ('DORMITORY_HYGIENE', 'CLASSROOM_HYGIENE', 'DISCIPLINE_CHECK')
  AND deleted = 0;

-- ====================================================================
-- 注意事项:
-- 1. 如果check_templates表结构不同,需要调整字段映射
-- 2. categories字段如果不是JSON类型,需要调整数据格式
-- 3. check_frequency字段含义需要确认(1=每日, 4=每周多次)
-- 4. created_by字段需要使用实际的管理员用户ID
-- ====================================================================

-- ====================================================================
-- 回滚脚本 (如果需要)
-- DELETE FROM check_templates
-- WHERE template_code IN ('DORMITORY_HYGIENE', 'CLASSROOM_HYGIENE', 'DISCIPLINE_CHECK')
--   AND created_at >= '2025-11-23 00:00:00';
-- ====================================================================
