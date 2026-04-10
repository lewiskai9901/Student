package com.school.management.application.organization.query;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * DTO for organization unit tree structure.
 * Generic — no industry-specific fields.
 */
@Data
public class OrgUnitTreeDTO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long parentId;
    private String unitCode;
    private String unitName;
    private String unitType;           // typeCode
    private String category;           // OrgCategory enum value (ROOT/BRANCH/FUNCTIONAL/GROUP/CONTAINER)
    private String typeName;           // display name
    private String typeIcon;           // icon
    private String typeColor;          // color
    private String status;             // DRAFT/ACTIVE/FROZEN/MERGING/DISSOLVED
    private String statusLabel;
    private Integer headcount;
    private Map<String, Object> attributes; // generic extension attributes from org_units.attributes JSON
    private List<OrgUnitTreeDTO> children;
}
