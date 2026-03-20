-- Phase 1.1: Rename template_id to section_id in insp_scoring_profiles
-- ScoringProfile.templateId was actually storing a section ID (root section), rename to clarify.
ALTER TABLE insp_scoring_profiles RENAME COLUMN template_id TO section_id;
