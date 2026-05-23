package com.school.management.domain.inspection.exception;

/**
 * 拒绝移除最后一个项目负责人 (LEAD).
 *
 * <p>不变量: 每个项目至少 1 个 LEAD. 如果当前只剩 1 个 LEAD, 则不允许移除/降级.
 * 必须先指派另一个 LEAD, 再释放当前 LEAD.
 */
public class LastLeadRemovalException extends RuntimeException {

    public LastLeadRemovalException(Long projectId) {
        super("项目 " + projectId + " 至少需要 1 个负责人, 请先指定其他负责人再移除当前负责人.");
    }

    public LastLeadRemovalException(String message) {
        super(message);
    }
}
