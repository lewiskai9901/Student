package com.school.management.domain.inspection.event;

import com.school.management.domain.inspection.model.InspectionTemplate;
import com.school.management.domain.inspection.model.TemplateScope;
import com.school.management.domain.shared.event.BaseDomainEvent;

/**
 * Domain event raised when an inspection template is created.
 */
public class InspectionTemplateCreatedEvent extends BaseDomainEvent {

    private final Long templateId;
    private final String templateCode;
    private final String templateName;
    private final TemplateScope scope;
    private final Long createdBy;

    public InspectionTemplateCreatedEvent(InspectionTemplate template) {
        super("InspectionTemplate", String.valueOf(template.getId()));
        this.templateId = template.getId();
        this.templateCode = template.getTemplateCode();
        this.templateName = template.getTemplateName();
        this.scope = template.getScope();
        this.createdBy = template.getCreatedBy();
    }

    public Long getTemplateId() {
        return templateId;
    }

    public String getTemplateCode() {
        return templateCode;
    }

    public String getTemplateName() {
        return templateName;
    }

    public TemplateScope getScope() {
        return scope;
    }

    public Long getCreatedBy() {
        return createdBy;
    }
}
