package com.school.management.application.user.command;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 更新用户命令
 */
@Data
@Builder
public class UpdateUserCommand {

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
     * 角色ID列表
     */
    private List<Long> roleIds;

    /**
     * 更新人ID
     */
    private Long updatedBy;
}
