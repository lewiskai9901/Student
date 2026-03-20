-- =====================================================
-- 场所继承模型重构 - NULL语义继承
-- 对标: Kubernetes ConfigMap, AWS Resource Groups Tag Inheritance
-- 创建时间: 2026-02-13
-- 解决缺陷: P0.1 继承逻辑缺陷（值拷贝 → NULL继承）
-- =====================================================

-- 设置字符集为utf8mb4
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
SET CHARACTER SET utf8mb4;

-- 1. 修改 places 表：允许 org_unit_id 为 NULL
ALTER TABLE places
MODIFY COLUMN org_unit_id BIGINT DEFAULT NULL COMMENT '所属组织ID（NULL=继承父级）';

-- 2. 创建递归函数：计算有效组织ID
DROP FUNCTION IF EXISTS get_effective_org_unit_id;

DELIMITER $$
CREATE FUNCTION get_effective_org_unit_id(p_place_id BIGINT)
RETURNS BIGINT
DETERMINISTIC
READS SQL DATA
BEGIN
  DECLARE effective_org_id BIGINT;
  DECLARE current_org_id BIGINT;
  DECLARE parent_id BIGINT;
  DECLARE max_depth INT DEFAULT 10;  -- 防止无限递归
  DECLARE current_depth INT DEFAULT 0;

  -- 读取当前场所的 org_unit_id 和 parent_id
  SELECT org_unit_id, parent_id
  INTO current_org_id, parent_id
  FROM places
  WHERE id = p_place_id AND deleted = 0;

  -- 如果场所不存在，返回 NULL
  IF current_org_id IS NULL AND parent_id IS NULL THEN
    RETURN NULL;
  END IF;

  -- 如果当前场所有显式设置 org_unit_id，直接返回
  IF current_org_id IS NOT NULL THEN
    RETURN current_org_id;
  END IF;

  -- 递归向上查找父级的有效 org_unit_id
  SET effective_org_id = NULL;
  SET parent_id = (SELECT parent_id FROM places WHERE id = p_place_id);

  WHILE parent_id IS NOT NULL AND current_depth < max_depth DO
    SELECT org_unit_id, parent_id
    INTO current_org_id, parent_id
    FROM places
    WHERE id = parent_id AND deleted = 0;

    -- 如果找到显式设置的 org_unit_id，终止递归
    IF current_org_id IS NOT NULL THEN
      SET effective_org_id = current_org_id;
      SET parent_id = NULL;
    END IF;

    SET current_depth = current_depth + 1;
  END WHILE;

  RETURN effective_org_id;
END$$
DELIMITER ;

-- 3. 创建视图：显示有效（解析后）的组织归属
CREATE OR REPLACE VIEW v_places_effective_org AS
SELECT
  up.id AS place_id,
  up.place_name,
  up.org_unit_id AS declared_org_id,
  get_effective_org_unit_id(up.id) AS effective_org_id,
  CASE
    WHEN up.org_unit_id IS NULL THEN 'INHERITED'
    ELSE 'EXPLICIT'
  END AS org_assignment_type,
  ou.unit_name AS effective_org_name,
  up.parent_id,
  parent.place_name AS parent_place_name
FROM places up
LEFT JOIN org_units ou ON get_effective_org_unit_id(up.id) = ou.id
LEFT JOIN places parent ON up.parent_id = parent.id
WHERE up.deleted = 0;

-- 4. 数据迁移：识别并转换继承关系
-- 策略：如果子场所的 org_unit_id 与父场所完全相同，则设置为 NULL（表示继承）

UPDATE places child
INNER JOIN places parent ON child.parent_id = parent.id
SET child.org_unit_id = NULL
WHERE child.org_unit_id IS NOT NULL
  AND parent.org_unit_id IS NOT NULL
  AND child.org_unit_id = parent.org_unit_id
  AND child.deleted = 0
  AND parent.deleted = 0;

-- 记录迁移日志
INSERT INTO place_audit_logs (
  event_id,
  request_id,
  resource_type,
  resource_id,
  event_name,
  event_type,
  event_source,
  event_time,
  user_type,
  source_ip,
  api_endpoint,
  reason
) VALUES (
  UUID(),
  UUID(),
  'SYSTEM',
  0,
  'MigrateInheritanceModel',
  'SystemAction',
  'inheritance-migration-job',
  NOW(6),
  'SYSTEM',
  '127.0.0.1',
  '/system/migration/inheritance',
  CONCAT('迁移继承模型: 将与父级相同的 org_unit_id 设置为 NULL，共影响 ', ROW_COUNT(), ' 条记录')
);

-- 5. 创建触发器：级联更新继承关系（父级变更时通知子级）
DROP TRIGGER IF EXISTS trg_cascade_org_unit_update;

DELIMITER $$
CREATE TRIGGER trg_cascade_org_unit_update
AFTER UPDATE ON places
FOR EACH ROW
BEGIN
  -- 仅当 org_unit_id 发生变化时触发
  IF NEW.org_unit_id <> OLD.org_unit_id OR (NEW.org_unit_id IS NULL AND OLD.org_unit_id IS NOT NULL) OR (NEW.org_unit_id IS NOT NULL AND OLD.org_unit_id IS NULL) THEN
    -- 记录级联事件日志（不自动更新子级，由应用层处理）
    INSERT INTO place_audit_logs (
      event_id,
      request_id,
      resource_type,
      resource_id,
      resource_name,
      event_name,
      event_type,
      event_source,
      event_time,
      user_type,
      source_ip,
      api_endpoint,
      changed_fields,
      reason
    ) VALUES (
      UUID(),
      UUID(),
      'PLACE',
      NEW.id,
      NEW.place_name,
      'OrgUnitInheritanceChanged',
      'SystemAction',
      'inheritance-trigger',
      NOW(6),
      'SYSTEM',
      '127.0.0.1',
      '/system/inheritance/cascade',
      JSON_ARRAY(
        JSON_OBJECT(
          'fieldName', 'org_unit_id',
          'oldValue', OLD.org_unit_id,
          'newValue', NEW.org_unit_id
        )
      ),
      CONCAT('场所 ', NEW.id, ' 的组织归属变更，可能影响 ', (SELECT COUNT(*) FROM places WHERE parent_id = NEW.id AND org_unit_id IS NULL), ' 个子场所')
    );
  END IF;
END$$
DELIMITER ;

-- 6. 创建存储过程：查询受继承影响的子场所
DROP PROCEDURE IF EXISTS get_affected_children;

DELIMITER $$
CREATE PROCEDURE get_affected_children(
  IN p_parent_id BIGINT
)
BEGIN
  -- 递归查询所有继承该父场所组织归属的子孙场所
  WITH RECURSIVE place_tree AS (
    -- 初始节点：直接子级且使用继承（org_unit_id IS NULL）
    SELECT
      id,
      place_name,
      parent_id,
      org_unit_id,
      1 AS depth
    FROM places
    WHERE parent_id = p_parent_id
      AND org_unit_id IS NULL
      AND deleted = 0

    UNION ALL

    -- 递归查找更深层级的继承子级
    SELECT
      child.id,
      child.place_name,
      child.parent_id,
      child.org_unit_id,
      parent.depth + 1 AS depth
    FROM places child
    INNER JOIN place_tree parent ON child.parent_id = parent.id
    WHERE child.org_unit_id IS NULL
      AND child.deleted = 0
      AND parent.depth < 10  -- 最大递归深度
  )
  SELECT
    id,
    place_name,
    parent_id,
    get_effective_org_unit_id(id) AS effective_org_id,
    depth
  FROM place_tree
  ORDER BY depth, id;
END$$
DELIMITER ;

-- 7. 创建索引：优化继承查询
CREATE INDEX idx_parent_inheritance ON places (
  parent_id,
  org_unit_id
) COMMENT '父级继承关系索引（org_unit_id IS NULL 表示继承）';

-- 8. 创建视图：继承关系可视化
CREATE OR REPLACE VIEW v_inheritance_tree AS
SELECT
  child.id AS child_place_id,
  child.place_name AS child_place_name,
  child.org_unit_id AS child_declared_org,
  parent.id AS parent_id,
  parent.place_name AS parent_place_name,
  parent.org_unit_id AS parent_declared_org,
  get_effective_org_unit_id(child.id) AS child_effective_org,
  get_effective_org_unit_id(parent.id) AS parent_effective_org,
  CASE
    WHEN child.org_unit_id IS NULL THEN 'YES'
    ELSE 'NO'
  END AS inherits_from_parent
FROM places child
LEFT JOIN places parent ON child.parent_id = parent.id
WHERE child.deleted = 0;

COMMIT;

-- 验证脚本
-- 查询所有继承关系
-- SELECT * FROM v_inheritance_tree WHERE inherits_from_parent = 'YES' LIMIT 20;

-- 查询有效组织归属
-- SELECT * FROM v_places_effective_org WHERE org_assignment_type = 'INHERITED' LIMIT 20;

-- 测试递归函数
-- SELECT id, name, org_unit_id, get_effective_org_unit_id(id) AS effective_org FROM places WHERE parent_id IS NOT NULL LIMIT 10;

-- 查询受父级变更影响的子场所
-- CALL get_affected_children(1);  -- 替换1为实际的父场所ID
