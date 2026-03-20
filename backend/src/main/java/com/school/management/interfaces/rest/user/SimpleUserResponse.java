package com.school.management.interfaces.rest.user;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.school.management.domain.user.model.aggregate.User;
import lombok.Data;

/**
 * 简单用户响应 DTO（用于选择器）
 */
@Data
public class SimpleUserResponse {

    private Long id;
    private String username;
    private String realName;
    private String orgUnitName;
    private String userType;
    private Integer gender;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long primaryOrgUnitId;
    private String primaryOrgUnitName;

    /**
     * 从领域模型转换
     */
    public static SimpleUserResponse fromDomain(User user) {
        if (user == null) {
            return null;
        }

        SimpleUserResponse response = new SimpleUserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setRealName(user.getRealName());
        response.setOrgUnitName(user.getOrgUnitName());
        response.setUserType(user.getUserTypeCode());
        response.setGender(user.getGender());
        response.setPrimaryOrgUnitId(user.getPrimaryOrgUnitId());
        response.setPrimaryOrgUnitName(user.getOrgUnitName());

        return response;
    }
}
