-- review #B: 新增 INSP_CORRECTIVE_REJECTED 触发点 — 与 INSP_CORRECTIVE_VERIFIED 拆分
-- 避免同 case 反复 verify/reject 时自动幂等 hash 命中导致漏发通知

INSERT IGNORE INTO trigger_points
    (module_code, module_name, point_code, point_name, description, context_schema, tenant_id)
VALUES
('inspection', '检查平台', 'INSP_CORRECTIVE_REJECTED', '整改验证驳回',
 '整改案例验证不通过 (验证驳回) 时触发, 通知当事人和责任人重新整改.',
 '{"caseId":{"type":"Long","label":"整改单ID"},"caseCode":{"type":"String","label":"整改单号"},"verificationResult":{"type":"String","label":"验证结果"},"subjectType":{"type":"String","label":"主体类型"},"subjectId":{"type":"Long","label":"主体ID"},"assigneeId":{"type":"Long","label":"责任人ID"},"verifierComment":{"type":"String","label":"驳回理由"}}',
 1);

-- 同时更新原 INSP_CORRECTIVE_VERIFIED 描述, 仅表示通过场景
UPDATE trigger_points
SET point_name = '整改验证通过',
    description = '整改案例验证通过时触发, 通知当事人和责任人案例闭环.'
WHERE point_code = 'INSP_CORRECTIVE_VERIFIED' AND deleted = 0;
