-- P1#9: 当事人通知触发点 — 让 整改/申诉/任务驳回 关键时刻能配置订阅规则推送通知
-- 不 seed event_trigger / subscription_rule: 订阅规则由 admin 按业务需求配置.

INSERT IGNORE INTO trigger_points
    (module_code, module_name, point_code, point_name, description, context_schema, tenant_id)
VALUES
('inspection', '检查平台', 'INSP_CORRECTIVE_CREATED', '整改单创建',
 '整改案例创建时触发, 通知当事人 (subject) 有新整改要求.',
 '{"caseId":{"type":"Long","label":"整改单ID"},"caseCode":{"type":"String","label":"整改单号"},"subjectType":{"type":"String","label":"主体类型"},"subjectId":{"type":"Long","label":"主体ID"},"subjectName":{"type":"String","label":"主体名称"},"priority":{"type":"String","label":"优先级"},"deadline":{"type":"String","label":"整改期限"},"issueDescription":{"type":"String","label":"问题描述"}}',
 1),
('inspection', '检查平台', 'INSP_CORRECTIVE_ASSIGNED', '整改责任人分配',
 '整改案例指派责任人时触发, 通知 assignee.',
 '{"caseId":{"type":"Long","label":"整改单ID"},"caseCode":{"type":"String","label":"整改单号"},"assigneeId":{"type":"Long","label":"责任人ID"},"assigneeName":{"type":"String","label":"责任人姓名"},"deadline":{"type":"String","label":"整改期限"}}',
 1),
('inspection', '检查平台', 'INSP_CORRECTIVE_VERIFIED', '整改验证完成',
 '整改案例验证 (通过/不通过) 时触发, 通知当事人和责任人.',
 '{"caseId":{"type":"Long","label":"整改单ID"},"caseCode":{"type":"String","label":"整改单号"},"verificationResult":{"type":"String","label":"验证结果"},"subjectType":{"type":"String","label":"主体类型"},"subjectId":{"type":"Long","label":"主体ID"},"assigneeId":{"type":"Long","label":"责任人ID"},"verifierComment":{"type":"String","label":"验证备注"}}',
 1),
('inspection', '检查平台', 'INSP_APPEAL_SUBMITTED', '申诉提交',
 '申诉提交时触发, 通知申诉人 (确认收到) 和审核员 (有待审).',
 '{"appealId":{"type":"Long","label":"申诉ID"},"appealCode":{"type":"String","label":"申诉编号"},"submitterUserId":{"type":"Long","label":"提交人ID"},"submitterName":{"type":"String","label":"提交人姓名"},"reason":{"type":"String","label":"申诉理由"},"submissionDetailId":{"type":"Long","label":"扣分项ID"}}',
 1),
('inspection', '检查平台', 'INSP_APPEAL_REVIEWED', '申诉审核完成',
 '申诉审核完成 (APPROVED/REJECTED) 时触发, 通知申诉人.',
 '{"appealId":{"type":"Long","label":"申诉ID"},"appealCode":{"type":"String","label":"申诉编号"},"submitterUserId":{"type":"Long","label":"提交人ID"},"reviewResult":{"type":"String","label":"审核结果"},"reviewerId":{"type":"Long","label":"审核员ID"},"reviewerComment":{"type":"String","label":"审核备注"},"finalAdjustment":{"type":"Decimal","label":"实际调整"}}',
 1),
('inspection', '检查平台', 'INSP_TASK_REJECTED', '任务驳回',
 '任务被审核驳回时触发, 通知检查员重新提交.',
 '{"taskId":{"type":"Long","label":"任务ID"},"taskCode":{"type":"String","label":"任务编号"},"inspectorId":{"type":"Long","label":"检查员ID"},"rejectionCount":{"type":"Long","label":"累计驳回次数"},"extendedTo":{"type":"String","label":"延期到的日期"},"comment":{"type":"String","label":"驳回原因"}}',
 1);
