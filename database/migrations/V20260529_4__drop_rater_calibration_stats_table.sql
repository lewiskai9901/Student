-- V20260529_4: 删除死表 insp_rater_calibration_stats
--
-- 背景: V45 建的 insp_rater_calibration_stats 服务于 ScoringProfile 的 calibration/multiRater
-- 高级算法. 这些字段已在 V20260529_3 连同 Java 代码彻底删除 (引擎 0 引用、概念错位),
-- 该表随之成为死表 (无任何 PO/Repository/Mapper 消费). 按"彻底删除"原则一并 DROP.
--
-- DROP TABLE IF EXISTS 本身幂等, 可重复执行. 全新建库: V45 (schema/) 先建, 本迁移
-- (migrations/) 后 DROP, 最终不存在 — 与升级库一致.
DROP TABLE IF EXISTS insp_rater_calibration_stats;
