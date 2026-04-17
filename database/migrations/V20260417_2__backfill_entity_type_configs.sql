-- =============================================================================
-- V20260417_2 从 org_unit_types / user_types / place_types 回填数据到 entity_type_configs
--
-- 前置: V20260417_1 已执行（entity_type_configs 已补充新列）
-- 策略:
--   1) 补搬 org_unit_types 全量（V20260408_5 漏了这张表）
--   2) UPDATE JOIN 把三张老表新列值填到 entity_type_configs 已有行（只填 NULL 位）
--   3) 场所特有字段合并进 ui_config JSON
--   4) 组织特有 isAcademic 合并进 features JSON
-- =============================================================================

-- 1-3. ORG_UNIT 部分：跳过。
-- V20260408_4 已 INSERT 过 ORG_UNIT 行且 org_unit_types 表随后被彻底删除。
-- 当前 entity_type_configs 已含 9 行 ORG_UNIT（插件注入 SCHOOL/DEPARTMENT/GRADE/CLASS 等）。
-- 新增列 description/icon/max_depth/default_* 对 ORG_UNIT 行保持 NULL 是可接受的初始状态，
-- 不需要从已不存在的 org_unit_types 回填。

-- 4. 回填 USER 新列
UPDATE entity_type_configs etc
JOIN user_types ut ON ut.type_code = etc.type_code AND ut.deleted = 0
SET etc.description              = COALESCE(etc.description, ut.description),
    etc.default_role_codes       = COALESCE(
        etc.default_role_codes,
        CASE WHEN ut.default_role_codes IS NOT NULL AND JSON_VALID(ut.default_role_codes)
             THEN CAST(ut.default_role_codes AS JSON)
             ELSE NULL END
    ),
    etc.default_org_type_codes   = COALESCE(etc.default_org_type_codes, ut.default_org_type_codes),
    etc.default_place_type_codes = COALESCE(etc.default_place_type_codes, ut.default_place_type_codes)
WHERE etc.entity_type = 'USER' AND etc.deleted = 0;

-- 5. 回填 PLACE 新列
UPDATE entity_type_configs etc
JOIN place_types pt ON pt.type_code = etc.type_code AND pt.deleted = 0
SET etc.description              = COALESCE(etc.description, pt.description),
    etc.max_depth                = COALESCE(etc.max_depth, pt.max_depth),
    etc.default_user_type_codes  = COALESCE(etc.default_user_type_codes, pt.default_user_type_codes),
    etc.default_org_type_codes   = COALESCE(etc.default_org_type_codes, pt.default_org_type_codes)
WHERE etc.entity_type = 'PLACE' AND etc.deleted = 0;

-- 6. PLACE 特有字段合并进 ui_config JSON
UPDATE entity_type_configs etc
JOIN place_types pt ON pt.type_code = etc.type_code AND pt.deleted = 0
SET etc.ui_config = JSON_MERGE_PATCH(
    COALESCE(etc.ui_config, JSON_OBJECT()),
    JSON_OBJECT(
        'isRootType',      COALESCE(pt.is_root_type, 0) = 1,
        'capacityUnit',    pt.capacity_unit,
        'defaultCapacity', pt.default_capacity
    )
)
WHERE etc.entity_type = 'PLACE' AND etc.deleted = 0;
