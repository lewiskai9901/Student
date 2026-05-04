-- Demo corrective cases for Analytics dashboard pie chart
-- 30 cases distributed across 7 状态 (OPEN/ASSIGNED/IN_PROGRESS/SUBMITTED/VERIFIED/CLOSED/REJECTED)
-- Seeded from is_flagged=1 details on tasks 200/202/203/205

INSERT INTO insp_corrective_cases (case_code, submission_id, project_id, task_id, target_type, target_id, target_name, issue_description, priority, deadline, status)
SELECT
  CONCAT('CASE-', s.id, '-', d.id),
  s.id, 2, s.task_id, 'ORG', s.target_id, s.org_unit_name,
  CONCAT(d.item_name, ' 不达标'),
  CASE
    WHEN d.score < 1 THEN 'CRITICAL'
    WHEN d.score < 2 THEN 'HIGH'
    WHEN s.final_score < 60 THEN 'HIGH'
    WHEN s.final_score < 75 THEN 'MEDIUM'
    ELSE 'LOW'
  END,
  DATE_ADD(NOW(), INTERVAL 7 DAY),
  ELT((d.id % 8) + 1, 'OPEN', 'ASSIGNED', 'IN_PROGRESS', 'IN_PROGRESS', 'SUBMITTED', 'VERIFIED', 'CLOSED', 'REJECTED')
FROM insp_submissions s
JOIN insp_submission_details d ON d.submission_id = s.id
WHERE s.task_id IN (200, 202, 203, 205)
  AND d.is_flagged = 1
  AND NOT EXISTS (SELECT 1 FROM insp_corrective_cases c WHERE c.case_code = CONCAT('CASE-', s.id, '-', d.id))
LIMIT 30;
