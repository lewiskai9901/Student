-- Generate submission_details for tasks 200/202/203/205 (24 submissions × 25 items = 600 rows)
-- Score / response derived from submission.final_score

INSERT INTO insp_submission_details (
  tenant_id, submission_id, template_item_id, item_code, item_name,
  section_id, section_name, item_type, response_value, scoring_mode,
  score, item_weight, is_flagged, flag_reason, remark
)
SELECT
  0,
  s.id,
  i.id,
  i.item_code,
  i.item_name,
  sec.id,
  sec.section_name,
  i.item_type,
  CASE
    WHEN i.item_type = 'PASS_FAIL' THEN
      CASE WHEN s.final_score >= 75 THEN 'PASS'
           WHEN s.final_score >= 60 AND (i.id % 3) > 0 THEN 'PASS'
           ELSE 'FAIL' END
    WHEN i.item_type = 'NUMBER' THEN
      CAST(GREATEST(40, LEAST(100, s.final_score + ((i.id % 21) - 10))) AS CHAR)
    WHEN i.item_type = 'SELECT' THEN
      CASE WHEN s.final_score >= 80 THEN 'GOOD'
           WHEN s.final_score >= 60 THEN 'AVG'
           ELSE 'POOR' END
    ELSE NULL
  END AS response_value,
  CASE WHEN i.is_scored = 1 THEN 'AUTO' ELSE 'NONE' END AS scoring_mode,
  ROUND(i.item_weight * 5 * (s.final_score / 100.0) + ((i.id % 5) - 2) * 0.2, 2) AS score,
  i.item_weight,
  CASE
    WHEN i.item_type = 'PASS_FAIL' AND s.final_score < 60 THEN 1
    WHEN i.item_type = 'NUMBER' AND s.final_score < 60 AND (i.id % 4) = 0 THEN 1
    ELSE 0
  END AS is_flagged,
  CASE
    WHEN i.item_type = 'PASS_FAIL' AND s.final_score < 60 THEN '现场未达标, 已拍照存证'
    WHEN i.item_type = 'NUMBER' AND s.final_score < 60 AND (i.id % 4) = 0 THEN '低于基准, 需整改'
    ELSE NULL
  END AS flag_reason,
  CASE
    WHEN s.final_score >= 90 THEN '执行良好'
    WHEN s.final_score >= 75 THEN ''
    WHEN s.final_score >= 60 THEN '基本达标, 个别细节需改进'
    ELSE '存在多处不达标, 建议复查'
  END AS remark
FROM insp_submissions s
CROSS JOIN insp_template_items i
JOIN insp_template_sections sec ON sec.id = i.section_id
WHERE s.task_id IN (200, 202, 203, 205)
  AND sec.id IN (110, 111, 112, 120, 121, 130, 131, 140, 141)
  AND NOT EXISTS (
    SELECT 1 FROM insp_submission_details d
    WHERE d.submission_id = s.id AND d.template_item_id = i.id
  );

SELECT
  s.task_id,
  COUNT(DISTINCT s.id) AS submissions,
  COUNT(d.id) AS detail_rows,
  SUM(d.is_flagged) AS flagged
FROM insp_submissions s
LEFT JOIN insp_submission_details d ON d.submission_id = s.id
WHERE s.task_id IN (200,202,203,205)
GROUP BY s.task_id;
