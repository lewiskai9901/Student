package com.school.management.interfaces.rest.student.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

/**
 * 更新学生请求
 */
@Data
@Schema(description = "更新学生请求")
public class UpdateStudentRequest {

    @Schema(description = "学号")
    private String studentNo;

    @Schema(description = "姓名")
    private String name;

    @Schema(description = "性别 1男 2女")
    private Integer gender;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "出生日期")
    private LocalDate birthDate;

    @Schema(description = "家庭住址")
    private String homeAddress;

    @Schema(description = "紧急联系人")
    private String emergencyContact;

    @Schema(description = "紧急联系电话")
    private String emergencyPhone;

    @Schema(description = "备注")
    private String remark;
}
