package com.school.management.domain.inspection.event.v7;

import com.school.management.domain.shared.event.BaseDomainEvent;

/**
 * 模板发布事件 — 当模板从 DRAFT 转为 PUBLISHED 并创建版本快照时触发
 */
public class TemplatePublishedEvent extends BaseDomainEvent implements InspV7DomainEvent {

    private final Long templateId;
    private final Integer version;
    private final String templateCode;

    public TemplatePublishedEvent(Long templateId, Integer version, String templateCode) {
        super("InspTemplate", templateId);
        this.templateId = templateId;
        this.version = version;
        this.templateCode = templateCode;
    }

    public Long getTemplateId() {
        return templateId;
    }

    public Integer getVersion() {
        return version;
    }

    public String getTemplateCode() {
        return templateCode;
    }
}
