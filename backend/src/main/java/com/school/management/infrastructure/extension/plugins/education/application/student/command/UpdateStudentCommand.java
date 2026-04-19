package com.school.management.application.student.command;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

/**
 * 更新学生信息命令
 */
@Data
@Builder
public class UpdateStudentCommand {

    private Long id;
    private String studentNo;
    private String name;
    private Integer gender;
    private String idCard;
    private String phone;
    private String email;
    private LocalDate birthDate;
    private LocalDate enrollmentDate;
    private LocalDate expectedGraduationDate;
    private Long orgUnitId;
    private String avatarUrl;
    private String homeAddress;
    private String emergencyContact;
    private String emergencyPhone;
    private String remark;

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
}
