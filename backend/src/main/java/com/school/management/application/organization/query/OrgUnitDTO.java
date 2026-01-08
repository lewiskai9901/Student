package com.school.management.application.organization.query;

import com.school.management.domain.organization.model.OrgUnitType;
import lombok.Data;

import java.util.List;

/**
 * DTO for organization unit query results.
 */
@Data
public class OrgUnitDTO {

    private Long id;
    private String unitCode;
    private String unitName;
    private OrgUnitType unitType;
    private Long parentId;
    private String treePath;
    private Integer treeLevel;
    private Long leaderId;
    private String leaderName;
    private List<Long> deputyLeaderIds;
    private Integer sortOrder;
    private Boolean enabled;
}
