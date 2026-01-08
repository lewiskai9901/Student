package com.school.management.application.organization.query;

import com.school.management.domain.organization.model.OrgUnitType;
import lombok.Data;

import java.util.List;

/**
 * DTO for organization unit tree structure.
 */
@Data
public class OrgUnitTreeDTO {

    private Long id;
    private String unitCode;
    private String unitName;
    private OrgUnitType unitType;
    private Long leaderId;
    private Boolean enabled;
    private List<OrgUnitTreeDTO> children;
}
