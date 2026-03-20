package com.school.management.application.organization.query;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.util.List;

/**
 * DTO for organization unit tree structure.
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
    private List<OrgUnitTreeDTO> children;

    // ===== Class extension fields (only populated for CLASS type nodes) =====
    private Integer studentCount;      // current student count
    private Integer standardSize;      // standard capacity
    private String headTeacherName;    // head teacher name
    private Integer enrollmentYear;    // enrollment year
    private String classStatus;        // PREPARING/ACTIVE/GRADUATED/DISSOLVED
}
