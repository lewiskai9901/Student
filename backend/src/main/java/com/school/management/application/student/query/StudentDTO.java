package com.school.management.application.student.query;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 学生查询DTO
 */
@Data
@Builder
public class StudentDTO {

    private Long id;
    private String studentNo;
    private String name;
    private Integer gender;
    private String genderText;
    private String idCard;
    private String phone;
    private String email;
    private LocalDate birthDate;
    private LocalDate enrollmentDate;
    private LocalDate expectedGraduationDate;
    private Long classId;
    private String className;
    private Long dormitoryId;
    private String dormitoryName;
    private Integer bedNumber;
    private Integer status;
    private String statusText;
    private String avatarUrl;
    private String homeAddress;
    private String emergencyContact;
    private String emergencyPhone;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 扩展字段
    private String idCardType;
    private String ethnicity;
    private String politicalStatus;
    private String educationLevel;
    private String studyLength;
    private String degreeType;
    private String graduatedSchool;
    private String entryLevel;
    private String educationSystem;
    private String guardianName;
    private String guardianPhone;
    private String guardianRelation;
    private String guardianIdCard;
    private String fatherName;
    private String fatherIdCard;
    private String fatherPhone;
    private String motherName;
    private String motherIdCard;
    private String motherPhone;
    private String hukouProvince;
    private String hukouCity;
    private String hukouDistrict;
    private String hukouAddress;
    private String hukouType;
    private String postalCode;
    private Integer isPovertyRegistered;
    private String financialAidType;
    private String healthStatus;
    private String allergies;
    private String specialNotes;
    private String nativePlace;

    // ==================== 兼容性方法 (供前端V1字段使用) ====================

    /**
     * 姓名别名 (兼容前端realName字段)
     */
    public String getRealName() {
        return name;
    }

    /**
     * 学籍状态别名 (兼容前端studentStatus字段)
     */
    public Integer getStudentStatus() {
        return status;
    }

    /**
     * 学籍状态名称别名 (兼容前端studentStatusName字段)
     */
    public String getStudentStatusName() {
        return statusText;
    }

    /**
     * 入学日期别名 (兼容前端admissionDate字段)
     */
    public LocalDate getAdmissionDate() {
        return enrollmentDate;
    }
}
