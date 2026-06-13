package com.school.management.application.user.command;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

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
     * 用户类型编码
     */
    private String userTypeCode;

    /**
     * 角色ID列表
     */
    private List<Long> roleIds;

    /**
     * 扩展属性 (schema 驱动的用户类型扩展字段值)
     */
    private Map<String, Object> attributes;

    /**
     * 更新人ID
     */
    private Long updatedBy;
}
