-- V20260418_3: 归一化 entity_type_configs.category —— 统一到后端 enum 定义值
--
-- 背景：P5 收敛后 OrgCategory/UserCategory/BaseCategory 枚举被精简，但 DB 历史数据仍用
-- 老语义（ACADEMIC/LEARNER/SPACE）。前端分类下拉找不到就回退显示英文 code。
-- 本迁移将 DB 值映射到枚举定义：
--   ORG_UNIT: ACADEMIC → CONTAINER  (OrgCategory.CONTAINER 注释即"年级/学部")
--   USER:     LEARNER  → MEMBER     (features 完全一致)
--   PLACE:    SPACE    → 每类型对应的结构角色

-- 1. ORG_UNIT: ACADEMIC → CONTAINER
UPDATE entity_type_configs
SET category = 'CONTAINER'
WHERE entity_type = 'ORG_UNIT' AND category = 'ACADEMIC' AND deleted = 0;

-- 2. USER: LEARNER → MEMBER
UPDATE entity_type_configs
SET category = 'MEMBER'
WHERE entity_type = 'USER' AND category = 'LEARNER' AND deleted = 0;

-- 3. PLACE: 基础结构类型自指
UPDATE entity_type_configs SET category = 'SITE'     WHERE entity_type='PLACE' AND type_code='SITE'     AND deleted=0;
UPDATE entity_type_configs SET category = 'BUILDING' WHERE entity_type='PLACE' AND type_code='BUILDING' AND deleted=0;
UPDATE entity_type_configs SET category = 'FLOOR'    WHERE entity_type='PLACE' AND type_code='FLOOR'    AND deleted=0;
UPDATE entity_type_configs SET category = 'ROOM'     WHERE entity_type='PLACE' AND type_code='ROOM'     AND deleted=0;
UPDATE entity_type_configs SET category = 'AREA'     WHERE entity_type='PLACE' AND type_code='AREA'     AND deleted=0;
UPDATE entity_type_configs SET category = 'POINT'    WHERE entity_type='PLACE' AND type_code='POINT'    AND deleted=0;

-- 4. PLACE: 楼栋变体 → BUILDING
UPDATE entity_type_configs
SET category = 'BUILDING'
WHERE entity_type = 'PLACE'
  AND type_code IN ('TYPE_TEACH_BLDG', 'TYPE_TRAIN_BLDG', 'DORM_BUILDING')
  AND deleted = 0;

-- 5. PLACE: 楼层变体 → FLOOR
UPDATE entity_type_configs
SET category = 'FLOOR'
WHERE entity_type = 'PLACE'
  AND type_code IN ('TYPE_TEACH_FLOOR', 'TYPE_TRAIN_FLOOR', 'DORM_FLOOR')
  AND deleted = 0;

-- 6. PLACE: 房间/实验室变体 → ROOM
UPDATE entity_type_configs
SET category = 'ROOM'
WHERE entity_type = 'PLACE'
  AND type_code IN ('TYPE_LAB', 'TYPE_COMPUTER_LAB', 'TYPE_TRAINING')
  AND deleted = 0;

-- 校验：应返回 0 行
SELECT entity_type, type_code, category
FROM entity_type_configs
WHERE deleted = 0
  AND (
    (entity_type = 'ORG_UNIT'  AND category NOT IN ('ROOT','BRANCH','FUNCTIONAL','GROUP','CONTAINER')) OR
    (entity_type = 'USER'      AND category NOT IN ('ADMIN','STAFF','MEMBER','EXTERNAL')) OR
    (entity_type = 'PLACE'     AND category NOT IN ('SITE','BUILDING','FLOOR','ROOM','AREA','POINT'))
  );
