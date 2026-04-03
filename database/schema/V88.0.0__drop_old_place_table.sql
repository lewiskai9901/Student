-- V88.0.0: Drop legacy `place` table
-- The old `place` table (147 records) is replaced by `places` (256 records)
-- used by UniversalPlace domain and PlaceMapper.
-- All queries now reference `places` consistently.

DROP TABLE IF EXISTS `place`;
