package com.school.management.application.inspection;

import com.school.management.domain.inspection.model.TemplateScope;
import lombok.Builder;
import lombok.Data;

/**
 * Command to create a new inspection template.
 */
@Data
@Builder
public class CreateTemplateCommand {
    private String templateCode;
    private String templateName;
    private String description;
    private TemplateScope scope;
    private Long applicableOrgUnitId;
    private Long createdBy;
}
