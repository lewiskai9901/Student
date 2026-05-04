-- V109: 移除检查平台未真正落地功能 (P0)
-- 删除内容: NFC / IoT / Webhook / NotificationRule / Knowledge / Compliance / ReportTemplate
-- 全部表无业务数据 (项目 1+2 / demo 数据均为 0 行)
DROP TABLE IF EXISTS insp_nfc_tags;
DROP TABLE IF EXISTS insp_iot_sensors;
DROP TABLE IF EXISTS insp_item_sensor_bindings;
DROP TABLE IF EXISTS insp_webhook_subscriptions;
DROP TABLE IF EXISTS insp_notification_rules;
DROP TABLE IF EXISTS insp_knowledge_articles;
DROP TABLE IF EXISTS insp_compliance_standards;
DROP TABLE IF EXISTS insp_compliance_clauses;
DROP TABLE IF EXISTS insp_item_compliance_mappings;
DROP TABLE IF EXISTS insp_report_templates;
SELECT 'V109 done' AS msg;
