-- V37: Add scoring_config, validation_rules, condition_logic to submission details
-- These fields store template config copies so execution is self-contained

ALTER TABLE insp_submission_details
  ADD COLUMN scoring_config VARCHAR(1000) NULL AFTER scoring_mode,
  ADD COLUMN validation_rules VARCHAR(2000) NULL AFTER scoring_config,
  ADD COLUMN condition_logic VARCHAR(1000) NULL AFTER validation_rules;
