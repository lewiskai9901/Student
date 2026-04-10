package com.school.management.application.organization.query;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

@Data
public class OrgMemberDTO {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;
    private String userName;
    private String userTypeCode;
    private String userTypeName;
    /** belonging = 归属成员 */
    private String membershipType;

    /** 用户归属组织ID（primaryOrgUnitId） */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long primaryOrgUnitId;
    /** 用户归属组织名称 */
    private String primaryOrgUnitName;
}
