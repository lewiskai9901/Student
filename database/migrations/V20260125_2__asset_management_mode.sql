-- =====================================================
-- V20260125_2: Asset Management Mode Enhancement
-- Description: Add management mode support for assets
--              (single item vs batch management)
-- =====================================================

-- 1. Add management_mode column to asset table
ALTER TABLE asset ADD COLUMN IF NOT EXISTS management_mode INT DEFAULT 1 COMMENT '管理模式: 1-单品管理, 2-批量管理';

-- 2. Add default_management_mode column to asset_category table
ALTER TABLE asset_category ADD COLUMN IF NOT EXISTS default_management_mode INT DEFAULT 1 COMMENT '默认管理模式: 1-单品管理, 2-批量管理';

-- 3. Update existing categories based on category_type
-- category_type: 1-固定资产, 2-低值易耗品, 3-消耗品
-- Fixed assets default to single item management (1)
-- Low-value consumables default to single item management (1)
-- Consumables default to batch management (2)
UPDATE asset_category
SET default_management_mode = CASE
    WHEN category_type = 1 THEN 1  -- 固定资产 -> 单品管理
    WHEN category_type = 2 THEN 1  -- 低值易耗品 -> 单品管理
    WHEN category_type = 3 THEN 2  -- 消耗品 -> 批量管理
    ELSE 1                         -- 默认单品管理
END
WHERE default_management_mode IS NULL;

-- 4. Update existing assets based on their category's default mode
UPDATE asset a
JOIN asset_category c ON a.category_id = c.id
SET a.management_mode = c.default_management_mode
WHERE a.management_mode IS NULL;

-- 5. Set default for any remaining assets without management_mode
UPDATE asset SET management_mode = 1 WHERE management_mode IS NULL;

-- 6. Add index for efficient queries
CREATE INDEX IF NOT EXISTS idx_asset_management_mode ON asset(management_mode);
CREATE INDEX IF NOT EXISTS idx_asset_category_default_mode ON asset_category(default_management_mode);

-- 7. Add comment for documentation
-- Management Mode Explanation:
-- SINGLE_ITEM (1): Each asset has a unique code, quantity is always 1
--                  Examples: computers, projectors, printers
--                  Borrowing changes asset status, no quantity deduction
--
-- BATCH (2): One record represents multiple identical assets
--            Examples: pens, paper, cleaning supplies
--            Use (领用) deducts quantity from stock
--            Borrow (借用) temporarily reserves but doesn't deduct
