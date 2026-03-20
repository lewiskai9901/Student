package com.school.management.infrastructure.persistence.user;

import com.baomidou.mybatisplus.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户持久化对象
 */
@TableName("users")
public class UserPO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String username;
    private String password;
    private String realName;
    private String phone;
    private String email;
    private String avatar;
    private String employeeNo;
    private Integer gender;
    private LocalDate birthDate;
    @TableField("identity_card")
    private String idCard;
    private Long primaryOrgUnitId;
    private String userTypeCode;
    private Integer status;
    private LocalDateTime lastLoginTime;
    private String lastLoginIp;
    private LocalDateTime passwordChangedAt;
    private String wechatOpenid;
    private Integer allowMultipleDevices;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    private Long tenantId;
    private Long createdBy;
    private Long updatedBy;
    @TableLogic
    private Long deleted;
    @TableField(exist = false)
    private String orgUnitName;
    @TableField(exist = false)
    private Long orgUnitId;

    // Getters
    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getRealName() { return realName; }
    public String getPhone() { return phone; }
    public String getEmail() { return email; }
    public String getAvatar() { return avatar; }
    public String getEmployeeNo() { return employeeNo; }
    public Integer getGender() { return gender; }
    public LocalDate getBirthDate() { return birthDate; }
    public String getIdCard() { return idCard; }
    public Long getPrimaryOrgUnitId() { return primaryOrgUnitId; }
    public String getUserTypeCode() { return userTypeCode; }
    public Integer getStatus() { return status; }
    public LocalDateTime getLastLoginTime() { return lastLoginTime; }
    public String getLastLoginIp() { return lastLoginIp; }
    public LocalDateTime getPasswordChangedAt() { return passwordChangedAt; }
    public String getWechatOpenid() { return wechatOpenid; }
    public Integer getAllowMultipleDevices() { return allowMultipleDevices; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public Long getTenantId() { return tenantId; }
    public Long getCreatedBy() { return createdBy; }
    public Long getUpdatedBy() { return updatedBy; }
    public Long getDeleted() { return deleted; }
    public String getOrgUnitName() { return orgUnitName; }
    public Long getOrgUnitId() { return orgUnitId; }

    // Setters
    public void setId(Long id) { this.id = id; }
    public void setUsername(String username) { this.username = username; }
    public void setPassword(String password) { this.password = password; }
    public void setRealName(String realName) { this.realName = realName; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setEmail(String email) { this.email = email; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
    public void setEmployeeNo(String employeeNo) { this.employeeNo = employeeNo; }
    public void setGender(Integer gender) { this.gender = gender; }
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }
    public void setIdCard(String idCard) { this.idCard = idCard; }
    public void setPrimaryOrgUnitId(Long primaryOrgUnitId) { this.primaryOrgUnitId = primaryOrgUnitId; }
    public void setUserTypeCode(String userTypeCode) { this.userTypeCode = userTypeCode; }
    public void setStatus(Integer status) { this.status = status; }
    public void setLastLoginTime(LocalDateTime lastLoginTime) { this.lastLoginTime = lastLoginTime; }
    public void setLastLoginIp(String lastLoginIp) { this.lastLoginIp = lastLoginIp; }
    public void setPasswordChangedAt(LocalDateTime passwordChangedAt) { this.passwordChangedAt = passwordChangedAt; }
    public void setWechatOpenid(String wechatOpenid) { this.wechatOpenid = wechatOpenid; }
    public void setAllowMultipleDevices(Integer allowMultipleDevices) { this.allowMultipleDevices = allowMultipleDevices; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    public void setUpdatedBy(Long updatedBy) { this.updatedBy = updatedBy; }
    public void setDeleted(Long deleted) { this.deleted = deleted; }
    public void setOrgUnitName(String orgUnitName) { this.orgUnitName = orgUnitName; }
    public void setOrgUnitId(Long orgUnitId) { this.orgUnitId = orgUnitId; }
}
