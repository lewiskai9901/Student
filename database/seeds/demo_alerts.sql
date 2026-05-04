-- Demo 预警规则 + 预警实例 (让 AlertDashboard 看板有数据)

-- 3 条规则
INSERT INTO insp_alert_rules (id, rule_name, metric_type, threshold_config, severity, notification_channels, is_enabled, project_id)
VALUES
  (1001, '日均分低于 70', 'DAILY_AVG_SCORE', JSON_OBJECT('op', '<', 'value', 70), 'WARNING', JSON_ARRAY('SYSTEM'), 1, 2),
  (1002, '日均分低于 60', 'DAILY_AVG_SCORE', JSON_OBJECT('op', '<', 'value', 60), 'CRITICAL', JSON_ARRAY('SYSTEM','EMAIL'), 1, 2),
  (1003, '整改逾期 7 天', 'CORRECTIVE_OVERDUE_DAYS', JSON_OBJECT('op', '>', 'value', 7), 'WARNING', JSON_ARRAY('SYSTEM'), 1, 2)
ON DUPLICATE KEY UPDATE rule_name = VALUES(rule_name);

-- 5 条预警实例 — 从低分 daily_summary 派生
INSERT INTO insp_alerts (tenant_id, alert_rule_id, target_id, target_type, target_name, metric_value, threshold_value, severity, message, status, triggered_at, org_unit_id)
SELECT
  0,
  CASE WHEN ds.avg_score < 60 THEN 1002 ELSE 1001 END,
  ds.target_id,
  ds.target_type,
  ds.target_name,
  ds.avg_score,
  CASE WHEN ds.avg_score < 60 THEN 60 ELSE 70 END,
  CASE WHEN ds.avg_score < 60 THEN 'CRITICAL' ELSE 'WARNING' END,
  CONCAT(ds.target_name, ' ', ds.summary_date, ' 日均分 ', ds.avg_score, ' 低于阈值'),
  CASE (ds.id % 4)
    WHEN 0 THEN 'OPEN'
    WHEN 1 THEN 'OPEN'
    WHEN 2 THEN 'ACKNOWLEDGED'
    WHEN 3 THEN 'RESOLVED'
  END,
  CONCAT(ds.summary_date, ' 16:30:00'),
  ds.org_unit_id
FROM insp_daily_summaries ds
WHERE ds.project_id = 2 AND ds.avg_score < 80
  AND NOT EXISTS (SELECT 1 FROM insp_alerts a WHERE a.target_id = ds.target_id AND a.message LIKE CONCAT('%', ds.summary_date, '%'))
LIMIT 12;
