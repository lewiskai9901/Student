package com.school.management.dto.student;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * 学生基本信息DTO
 * 包含：学号、姓名、性别、生日、身份证等核心个人信息
 *
 * @author system
 * @version 2.0.0
 * @since 2024-12-31
 */
@Data
public class StudentBasicInfoDTO {

    /**
     * 学号（选填，不填则自动生成）
     */
    private String studentNo;

    /**
     * 真实姓名
     */
    @NotBlank(message = "姓名不能为空")
    private String realName;

    /**
     * 性别: 1男 2女
     */
    @NotNull(message = "性别不能为空")
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
     * 证件类型
     */
    private String idCardType;

    /**
     * 民族
     */
    private String ethnicity;

    /**
     * 政治面貌
     */
    private String politicalStatus;

    /**
     * 籍贯
     */
    private String nativePlace;
}
