-- ============================================================================
-- V20260613_1: 删除未建成的预订子系统表
--
-- 背景 (场所模块深审, 2026-06-13): 预订子系统是"前端有完整 UI + 后端零端点"的
-- 半成品孤岛 —— 前端 7 个 /place-bookings API 全部 404, 后端只有领域/持久化层
-- (实体/PO/Mapper/Repository) + 一个失效的 BookingTimeConflictPolicy (status 用
-- 字符串比 tinyint 列恒匹配 0 行, 且其 phase 从无处 fire)。前端预订 tab 仅当
-- node.bookable=true 显示, 而无任何 place 类型开启 bookable → 实际不可达。
--
-- 决策: 删死代码 (而非从零补建后端)。6 个后端 Java 文件已删, 前端 UI 同步移除。
-- 将来若真要预订, 是个明确的功能项目, 届时重新建模建表。
--
-- 三张表均空 (0 行) 且零代码引用:
--   place_bookings           — 预订主表
--   booking_seat_assignments — 排座表 (前端 /seating API 目标, 后端从无实体)
--   space_bookings           — Space→Place 改名前的残留死表
--
-- 幂等: DROP TABLE IF EXISTS。
-- ============================================================================

DROP TABLE IF EXISTS booking_seat_assignments;
DROP TABLE IF EXISTS place_bookings;
DROP TABLE IF EXISTS space_bookings;
