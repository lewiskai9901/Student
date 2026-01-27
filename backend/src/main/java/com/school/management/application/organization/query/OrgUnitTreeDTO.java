package com.school.management.application.organization.query;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.school.management.domain.organization.model.OrgUnitType;
import com.school.management.domain.organization.model.UnitCategory;
import lombok.Data;

import java.util.List;

/**
 * DTO for organization unit tree structure.
 */
@Data
public class OrgUnitTreeDTO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String unitCode;
    private String unitName;
    private OrgUnitType unitType;
    private UnitCategory unitCategory;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long leaderId;
    private Boolean enabled;
    private List<OrgUnitTreeDTO> children;
}
