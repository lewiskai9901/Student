package com.school.management.interfaces.rest.student.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

/**
 * 学生入学请求
 */
@Data
@Schema(description = "学生入学请求")
public class EnrollStudentRequest {

    @Schema(description = "学号")
    private String studentNo;

    @Schema(description = "姓名", required = true)
    private String name;

    @Schema(description = "性别 1男 2女")
    private Integer gender;

    @Schema(description = "身份证号")
    private String idCard;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "出生日期")
    private LocalDate birthDate;

    @Schema(description = "入学日期")
    private LocalDate enrollmentDate;

    @Schema(description = "班级ID", required = true)
    private Long orgUnitId;

    @Schema(description = "家庭住址")
    private String homeAddress;

    @Schema(description = "紧急联系人")
    private String emergencyContact;

    @Schema(description = "紧急联系电话")
    private String emergencyPhone;

    @Schema(description = "备注")
    private String remark;
}
