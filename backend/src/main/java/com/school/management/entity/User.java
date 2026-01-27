package com.school.management.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户实体
 *
 * @author system
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("users")
public class User extends BaseEntity {

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码
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
     * 头像
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
    @TableField("identity_card")
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
     * 用户类型:1-管理员,2-教师,3-学生
     */
    private Integer userType;

    /**
     * 状态: 1启用 0禁用
     */
    private Integer status;

    /**
     * 最后登录时间(数据库字段)
     */
    private LocalDateTime lastLoginTime;

    /**
     * 最后登录IP(数据库字段)
     */
    private String lastLoginIp;

    /**
     * 密码修改时间(数据库字段)
     */
    private LocalDateTime passwordChangedAt;

    /**
     * 微信OpenID
     */
    private String wechatOpenid;

    /**
     * 是否允许多设备登录
     */
    private Integer allowMultipleDevices;

    // ========== 关联字段（非数据库字段） ==========

    /**
     * 用户角色列表
     */
    @TableField(exist = false)
    private java.util.List<Role> roles;

    /**
     * 角色名称列表(逗号分隔)
     */
    @TableField(exist = false)
    private String roleNames;
}