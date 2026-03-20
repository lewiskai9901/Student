package com.school.management.application.organization.query;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO for organization unit query results.
 */
@Data
public class OrgUnitDTO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String unitCode;
    private String unitName;
    private String unitType;           // typeCode
    private String typeName;           // display name from org_unit_types
    private String typeIcon;           // icon from org_unit_types
    private String typeColor;          // color from org_unit_types
    @JsonSerialize(using = ToStringSerializer.class)
    private Long parentId;
    private String treePath;
    private Integer treeLevel;
    private Integer sortOrder;
    private String status;             // DRAFT/ACTIVE/FROZEN/MERGING/DISSOLVED
    private String statusLabel;        // human-readable status label
    private Integer headcount;
    private Map<String, Object> attributes;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long mergedIntoId;
    private LocalDateTime dissolvedAt;
    private String dissolvedReason;
}
