package com.school.management.interfaces.rest.user;

import com.school.management.domain.user.model.aggregate.User;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户领域响应 DTO
 */
@Data
public class UserDomainResponse {

    private Long id;
    private String username;
    private String realName;
    private String phone;
    private String email;
    private String avatar;
    private String employeeNo;
    private Integer gender;
    private LocalDate birthDate;
    private String idCard;
    private String userType;

    /**
     * 身份证号脱敏：显示前3位和后4位，中间用*代替
     */
    public String getIdCard() {
        if (idCard == null || idCard.length() < 8) {
            return idCard;
        }
        return idCard.substring(0, 3) + "****" + idCard.substring(idCard.length() - 4);
    }
    private Long orgUnitId;
    private String status;
    private LocalDateTime lastLoginTime;
    private String lastLoginIp;
    private LocalDateTime passwordChangedAt;
    private boolean wechatBound;
    private boolean allowMultipleDevices;
    private List<Long> roleIds;
    private String orgUnitName;
    private List<String> roleNames;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 从领域模型转换
     */
    public static UserDomainResponse fromDomain(User user) {
        if (user == null) {
            return null;
        }

        UserDomainResponse response = new UserDomainResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setRealName(user.getRealName());
        response.setPhone(user.getPhone());
        response.setEmail(user.getEmail());
        response.setAvatar(user.getAvatar());
        response.setEmployeeNo(user.getEmployeeNo());
        response.setGender(user.getGender());
        response.setBirthDate(user.getBirthDate());
        response.setIdCard(user.getIdCard());
        response.setUserType(user.getUserTypeCode());
        response.setOrgUnitId(user.getOrgUnitId());
        response.setStatus(user.getStatus() != null ? user.getStatus().getDescription() : null);
        response.setLastLoginTime(user.getLastLoginTime());
        response.setLastLoginIp(user.getLastLoginIp());
        response.setPasswordChangedAt(user.getPasswordChangedAt());
        response.setWechatBound(user.getWechatOpenid() != null && !user.getWechatOpenid().isEmpty());
        response.setAllowMultipleDevices(user.isAllowMultipleDevices());
        response.setRoleIds(user.getRoleIds());
        response.setOrgUnitName(user.getOrgUnitName());
        response.setRoleNames(user.getRoleNames());
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());

        return response;
    }
}
