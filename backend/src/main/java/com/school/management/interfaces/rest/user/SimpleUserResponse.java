package com.school.management.interfaces.rest.user;

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
    private String departmentName;

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
        response.setDepartmentName(user.getDepartmentName());

        return response;
    }
}
