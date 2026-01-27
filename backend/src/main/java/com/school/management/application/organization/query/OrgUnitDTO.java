package com.school.management.application.organization.query;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.school.management.domain.organization.model.OrgUnitType;
import com.school.management.domain.organization.model.UnitCategory;
import lombok.Data;

import java.util.List;

/**
 * DTO for organization unit query results.
 */
@Data
public class OrgUnitDTO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String unitCode;
    private String unitName;
    private OrgUnitType unitType;
    private UnitCategory unitCategory;  // 组织类别
    @JsonSerialize(using = ToStringSerializer.class)
    private Long parentId;
    private String treePath;
    private Integer treeLevel;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long leaderId;
    private String leaderName;
    @JsonSerialize(contentUsing = ToStringSerializer.class)
    private List<Long> deputyLeaderIds;
    private Integer sortOrder;
    private Boolean enabled;
}
