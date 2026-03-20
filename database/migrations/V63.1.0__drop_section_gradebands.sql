-- Phase 1.3: Drop redundant grade_bands column from insp_template_sections
-- gradeBands on TemplateSection was superseded by scoringConfig (JSON) on ScoringProfile
ALTER TABLE insp_template_sections DROP COLUMN grade_bands;
