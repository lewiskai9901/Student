-- Demo observations for project 2 — drive BigScreen 实时动态 feed
INSERT INTO insp_submission_observations (
  tenant_id, submission_id, detail_id, project_id, task_id,
  item_code, item_name, item_type, section_name,
  subject_type, subject_id, subject_name,
  org_unit_id, org_unit_name,
  score, is_negative, severity, is_flagged,
  description, observed_at
)
SELECT
  0, s.id, d.id, 2, s.task_id,
  d.item_code, d.item_name, d.item_type, d.section_name,
  'ORG_UNIT', s.target_id, s.target_name,
  s.org_unit_id, s.org_unit_name,
  d.score - 5,
  1,
  CASE WHEN d.score < 1 THEN 'CRITICAL' WHEN d.score < 2 THEN 'HIGH' ELSE 'MEDIUM' END,
  1,
  CONCAT(d.item_name, ' 现场不达标, 已拍照存证'),
  DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 720) MINUTE)
FROM insp_submissions s
JOIN insp_submission_details d ON d.submission_id = s.id
WHERE s.task_id IN (200, 202, 203, 205)
  AND d.is_flagged = 1
  AND NOT EXISTS (
    SELECT 1 FROM insp_submission_observations o
    WHERE o.detail_id = d.id AND o.project_id = 2
  )
LIMIT 20;
