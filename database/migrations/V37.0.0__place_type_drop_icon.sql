-- V37.0.0: Remove icon column from place_types (no longer used in UI)
ALTER TABLE place_types DROP COLUMN IF EXISTS icon;
