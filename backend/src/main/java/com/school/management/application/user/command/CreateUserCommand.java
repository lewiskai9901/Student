package com.school.management.application.user.command;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 创建用户命令
 */
@Data
@Builder
public class CreateUserCommand {

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码（明文，服务层加密）
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
     * 用户类型编码（引用 user_types 表）
     */
    private String userTypeCode;

    /**
     * 关联场所ID
     */
    private Long placeId;

    /**
     * 角色ID列表
     */
    private List<Long> roleIds;

    /**
     * 创建人ID
     */
    private Long createdBy;
}
