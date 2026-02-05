package com.school.management.domain.user.model.aggregate;

import com.school.management.domain.shared.AggregateRoot;
import com.school.management.domain.user.event.UserCreatedEvent;
import com.school.management.domain.user.event.UserStatusChangedEvent;
import com.school.management.domain.user.event.UserUpdatedEvent;
import com.school.management.domain.user.event.UserPasswordResetEvent;
import com.school.management.domain.user.model.valueobject.UserStatus;
import com.school.management.domain.user.model.valueobject.UserType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 用户聚合根
 */
public class User extends AggregateRoot<Long> {

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码（加密后）
     */
    private String password;

    /**
     * 真实姓名
     */
    private String realName;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 头像URL
     */
    private String avatar;

    /**
     * 工号
     */
    private String employeeNo;

    /**
     * 性别: 1男 2女
     */
    private Integer gender;

    /**
     * 出生日期
     */
    private LocalDate birthDate;

    /**
     * 身份证号
     */
    private String idCard;

    /**
     * 组织单元ID
     */
    private Long orgUnitId;

    /**
     * 班级ID
     */
    private Long classId;

    /**
     * 用户类型（旧）
     */
    private UserType userType;

    /**
     * 用户类型编码（新，引用 user_types 表）
     */
    private String userTypeCode;

    /**
     * 主组织关系ID
     */
    private Long primaryOrgRelationId;

    /**
     * 状态
     */
    private UserStatus status;

    /**
     * 最后登录时间
     */
    private LocalDateTime lastLoginTime;

    /**
     * 最后登录IP
     */
    private String lastLoginIp;

    /**
     * 密码修改时间
     */
    private LocalDateTime passwordChangedAt;

    /**
     * 微信OpenID
     */
    private String wechatOpenid;

    /**
     * 是否允许多设备登录
     */
    private boolean allowMultipleDevices;

    /**
     * 角色ID列表
     */
    private List<Long> roleIds = new ArrayList<>();

    /**
     * 组织单元名称（查询时填充，非持久化字段）
     */
    private String orgUnitName;

    /**
     * 角色名称列表（查询时填充，非持久化字段）
     */
    private List<String> roleNames = new ArrayList<>();

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    // 私有构造函数
    private User() {}

    /**
     * 创建新用户
     */
    public static User create(
            String username,
            String encodedPassword,
            String realName,
            UserType userType,
            Long orgUnitId
    ) {
        User user = new User();
        user.username = username;
        user.password = encodedPassword;
        user.realName = realName;
        user.userType = userType;
        user.orgUnitId = orgUnitId;
        user.status = UserStatus.ENABLED;
        user.allowMultipleDevices = false;
        user.createdAt = LocalDateTime.now();
        user.updatedAt = LocalDateTime.now();
        user.passwordChangedAt = LocalDateTime.now();

        user.registerEvent(new UserCreatedEvent(
                "NEW",
                username,
                realName,
                userType
        ));

        return user;
    }

    /**
     * 重建用户（从持久化层恢复）
     */
    public static User reconstruct(
            Long id,
            String username,
            String password,
            String realName,
            String phone,
            String email,
            String avatar,
            String employeeNo,
            Integer gender,
            LocalDate birthDate,
            String idCard,
            Long orgUnitId,
            Long primaryOrgRelationId,
            Long classId,
            UserType userType,
            String userTypeCode,
            UserStatus status,
            LocalDateTime lastLoginTime,
            String lastLoginIp,
            LocalDateTime passwordChangedAt,
            String wechatOpenid,
            boolean allowMultipleDevices,
            List<Long> roleIds,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        User user = new User();
        user.setId(id);
        user.username = username;
        user.password = password;
        user.realName = realName;
        user.phone = phone;
        user.email = email;
        user.avatar = avatar;
        user.employeeNo = employeeNo;
        user.gender = gender;
        user.birthDate = birthDate;
        user.idCard = idCard;
        user.orgUnitId = orgUnitId;
        user.primaryOrgRelationId = primaryOrgRelationId;
        user.classId = classId;
        user.userType = userType;
        user.userTypeCode = userTypeCode;
        user.status = status;
        user.lastLoginTime = lastLoginTime;
        user.lastLoginIp = lastLoginIp;
        user.passwordChangedAt = passwordChangedAt;
        user.wechatOpenid = wechatOpenid;
        user.allowMultipleDevices = allowMultipleDevices;
        user.roleIds = roleIds != null ? new ArrayList<>(roleIds) : new ArrayList<>();
        user.createdAt = createdAt;
        user.updatedAt = updatedAt;
        return user;
    }

    /**
     * 更新基本信息
     */
    public void updateBasicInfo(
            String realName,
            String phone,
            String email,
            String employeeNo,
            Integer gender,
            LocalDate birthDate,
            String idCard,
            Long orgUnitId
    ) {
        this.realName = realName;
        this.phone = phone;
        this.email = email;
        this.employeeNo = employeeNo;
        this.gender = gender;
        this.birthDate = birthDate;
        this.idCard = idCard;
        this.orgUnitId = orgUnitId;
        this.updatedAt = LocalDateTime.now();

        // 只有在已持久化（有ID）时才注册更新事件
        if (this.getId() != null) {
            registerEvent(new UserUpdatedEvent(
                    this.getId().toString(),
                    this.username,
                    this.realName
            ));
        }
    }

    /**
     * 启用用户
     */
    public void enable() {
        if (this.status == UserStatus.ENABLED) {
            return;
        }
        UserStatus oldStatus = this.status;
        this.status = UserStatus.ENABLED;
        this.updatedAt = LocalDateTime.now();

        registerEvent(new UserStatusChangedEvent(
                this.getId().toString(),
                this.username,
                oldStatus,
                this.status
        ));
    }

    /**
     * 禁用用户
     */
    public void disable() {
        if (this.status == UserStatus.DISABLED) {
            return;
        }
        UserStatus oldStatus = this.status;
        this.status = UserStatus.DISABLED;
        this.updatedAt = LocalDateTime.now();

        registerEvent(new UserStatusChangedEvent(
                this.getId().toString(),
                this.username,
                oldStatus,
                this.status
        ));
    }

    /**
     * 重置密码
     */
    public void resetPassword(String newEncodedPassword) {
        this.password = newEncodedPassword;
        this.passwordChangedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();

        registerEvent(new UserPasswordResetEvent(
                this.getId().toString(),
                this.username
        ));
    }

    /**
     * 修改密码
     */
    public void changePassword(String newEncodedPassword) {
        this.password = newEncodedPassword;
        this.passwordChangedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 记录登录信息
     */
    public void recordLogin(String ip) {
        this.lastLoginTime = LocalDateTime.now();
        this.lastLoginIp = ip;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 绑定微信
     */
    public void bindWechat(String openid) {
        this.wechatOpenid = openid;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 解绑微信
     */
    public void unbindWechat() {
        this.wechatOpenid = null;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 分配角色
     */
    public void assignRoles(List<Long> roleIds) {
        this.roleIds = roleIds != null ? new ArrayList<>(roleIds) : new ArrayList<>();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 设置头像
     */
    public void setAvatar(String avatarUrl) {
        this.avatar = avatarUrl;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 设置多设备登录
     */
    public void setAllowMultipleDevices(boolean allow) {
        this.allowMultipleDevices = allow;
        this.updatedAt = LocalDateTime.now();
    }

    // Getters
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
    public Long getOrgUnitId() { return orgUnitId; }
    public Long getClassId() { return classId; }
    public UserType getUserType() { return userType; }
    public String getUserTypeCode() { return userTypeCode; }
    public Long getPrimaryOrgRelationId() { return primaryOrgRelationId; }
    public UserStatus getStatus() { return status; }
    public LocalDateTime getLastLoginTime() { return lastLoginTime; }
    public String getLastLoginIp() { return lastLoginIp; }
    public LocalDateTime getPasswordChangedAt() { return passwordChangedAt; }
    public String getWechatOpenid() { return wechatOpenid; }
    public boolean isAllowMultipleDevices() { return allowMultipleDevices; }
    public List<Long> getRoleIds() { return Collections.unmodifiableList(roleIds); }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public boolean isEnabled() {
        return status != null && status.isEnabled();
    }

    // 关联字段的 Getter 和 Setter
    public String getOrgUnitName() { return orgUnitName; }
    public void setOrgUnitName(String orgUnitName) { this.orgUnitName = orgUnitName; }
    public List<String> getRoleNames() { return roleNames; }
    public void setRoleNames(List<String> roleNames) { this.roleNames = roleNames != null ? roleNames : new ArrayList<>(); }
}
