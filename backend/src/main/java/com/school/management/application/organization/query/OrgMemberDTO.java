package com.school.management.application.organization.query;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

@Data
public class OrgMemberDTO {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userPositionId;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;
    private String userName;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long positionId;
    private String positionName;
    private String appointmentType;
    private boolean isPrimary;
    private String startDate;
    private String jobLevel;
    private boolean isKeyPosition;
    private String userTypeCode;
    /** belonging = 归属成员, staff = 岗位人员 */
    private String membershipType;

    /** 用户归属组织ID（primaryOrgUnitId） */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long primaryOrgUnitId;
    /** 用户归属组织名称 */
    private String primaryOrgUnitName;

    /** 岗位所在组织ID（递归查询时用） */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long orgUnitId;
    /** 岗位所在组织名称 */
    private String orgUnitName;
}
