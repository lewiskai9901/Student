package com.school.management.infrastructure.persistence.user;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户持久化对象
 */
@Data
@TableName("users")
public class UserPO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

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
     * 部门ID
     */
    private Long departmentId;

    /**
     * 班级ID
     */
    private Long classId;

    /**
     * 管理的班级ID
     */
    private Long managedClassId;

    /**
     * 用户类型:1-管理员,2-教师,3-学生
     */
    private Integer userType;

    /**
     * 状态: 1启用 0禁用
     */
    private Integer status;

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
    private Integer allowMultipleDevices;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 创建人
     */
    private Long createdBy;

    /**
     * 更新人
     */
    private Long updatedBy;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer deleted;

    // ==================== 关联字段（非数据库字段） ====================

    /**
     * 部门名称（JOIN 查询时使用）
     */
    @TableField(exist = false)
    private String departmentName;
}
