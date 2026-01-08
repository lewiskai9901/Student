package com.school.management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * 学生更新请求DTO
 *
 * @author system
 * @since 1.0.0
 */
@Data
public class StudentUpdateRequest {

    /**
     * 学生ID
     */
    @NotNull(message = "学生ID不能为空")
    private Long id;

    /**
     * 学号（选填）
     */
    private String studentNo;

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
     * 真实姓名
     */
    @NotBlank(message = "姓名不能为空")
    private String realName;

    /**
     * 手机号
     */
    private String phone;

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
     * 班级ID（选填）
     */
    private Long classId;

    /**
     * 年级ID
     */
    private Long gradeId;

    /**
     * 专业ID
     */
    private Long majorId;

    /**
     * 专业方向ID
     */
    private Long majorDirectionId;

    /**
     * 专业级别/层次(如:中专/大专)
     */
    private String educationLevel;

    /**
     * 学制(如:3年)
     */
    private String studyLength;

    /**
     * 学历(如:中专/大专)
     */
    private String degreeType;

    /**
     * 毕业学校
     */
    private String graduatedSchool;

    /**
     * 入学层次(如:本科、专科等)
     */
    private String entryLevel;

    /**
     * 学制(如:4年制、3年制等)
     */
    private String educationSystem;

    /**
     * 入学日期（选填）
     */
    private LocalDate admissionDate;

    /**
     * 毕业日期
     */
    private LocalDate graduationDate;

    /**
     * 学生状态: 1在读 2休学 3退学 4毕业 5转学（选填）
     */
    private Integer studentStatus;

    /**
     * 监护人姓名
     */
    private String guardianName;

    /**
     * 监护人电话
     */
    private String guardianPhone;

    /**
     * 监护人关系
     */
    private String guardianRelation;

    /**
     * 父亲姓名
     */
    private String fatherName;

    /**
     * 父亲身份证号
     */
    private String fatherIdCard;

    /**
     * 父亲电话
     */
    private String fatherPhone;

    /**
     * 母亲姓名
     */
    private String motherName;

    /**
     * 母亲身份证号
     */
    private String motherIdCard;

    /**
     * 母亲电话
     */
    private String motherPhone;

    /**
     * 监护人身份证号
     */
    private String guardianIdCard;

    /**
     * 紧急联系人
     */
    private String emergencyContact;

    /**
     * 紧急联系电话
     */
    private String emergencyPhone;

    /**
     * 家庭住址
     */
    private String homeAddress;

    /**
     * 户口所在地-省
     */
    private String hukouProvince;

    /**
     * 户口所在地-市
     */
    private String hukouCity;

    /**
     * 户口所在地-区
     */
    private String hukouDistrict;

    /**
     * 户口详细地址
     */
    private String hukouAddress;

    /**
     * 户口性质(农业/非农业)
     */
    private String hukouType;

    /**
     * 邮政编码
     */
    private String postalCode;

    /**
     * 是否建档立卡(0否1是)
     */
    private Integer isPovertyRegistered;

    /**
     * 资助申请类型
     */
    private String financialAidType;

    /**
     * 宿舍ID
     */
    private Long dormitoryId;

    /**
     * 床位号
     */
    private String bedNumber;

    /**
     * 健康状况
     */
    private String healthStatus;

    /**
     * 过敏史
     */
    private String allergies;

    /**
     * 特殊备注
     */
    private String specialNotes;

    public LocalDate getEnrollmentDate() {
        return admissionDate;
    }

    public void setEnrollmentDate(LocalDate enrollmentDate) {
        this.admissionDate = enrollmentDate;
    }
}