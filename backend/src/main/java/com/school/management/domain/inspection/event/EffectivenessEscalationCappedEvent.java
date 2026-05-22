package com.school.management.domain.inspection.event;

import com.school.management.domain.shared.event.BaseDomainEvent;

/**
 * 效果验证不达标且已达自动升级上限 — 案例转人工接管 (P0#1).
 *
 * <p>语义: failEffectiveness 时 escalationLevel 已达上限, 不再自动加级别,
 * 但案例不能卡在不可达状态 — 重新打开为 OPEN 等待上级人工重新分派处理.
 * 下游 listener 可据此触发"升级到管理层"通知, 与普通 {@link EffectivenessFailedEvent}
 * (自动重开+升级) 区分.
 */
public class EffectivenessEscalationCappedEvent extends BaseDomainEvent implements InspDomainEvent {

    private final Long caseId;
    private final String caseCode;
    private final Integer escalationLevel;

    public EffectivenessEscalationCappedEvent(Long caseId, String caseCode, Integer escalationLevel) {
        super("CorrectiveCase", caseId != null ? caseId.toString() : null);
        this.caseId = caseId;
        this.caseCode = caseCode;
        this.escalationLevel = escalationLevel;
    }

    public Long getCaseId() { return caseId; }
    public String getCaseCode() { return caseCode; }
    public Integer getEscalationLevel() { return escalationLevel; }
}
