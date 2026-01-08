package com.school.management.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 学生响应DTO
 *
 * @author system
 * @since 1.0.0
 */
@Data
public class StudentResponse {

    /**
     * 学生ID
     */
    private Long id;

    /**
     * 学号
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
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 真实姓名
     */
    private String realName;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 性别: 1男 2女
     */
    private Integer gender;

    /**
     * 性别名称
     */
    private String genderName;

    /**
     * 身份证号
     */
    private String idCard;

    /**
     * 班级ID
     */
    private Long classId;

    /**
     * 班级名称
     */
    private String className;

    /**
     * 年级ID
     */
    private Long gradeId;

    /**
     * 年级名称
     */
    private String gradeName;

    /**
     * 年级等级(如10=高一,11=高二,12=高三)
     */
    private Integer gradeLevel;

    /**
     * 专业ID
     */
    private Long majorId;

    /**
     * 专业名称
     */
    private String majorName;

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
     * 入学日期
     */
    private LocalDate admissionDate;

    /**
     * 毕业日期
     */
    private LocalDate graduationDate;

    /**
     * 学生状态: 1在读 2休学 3退学 4毕业 5转学
     */
    private Integer studentStatus;

    /**
     * 学生状态名称
     */
    private String studentStatusName;

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
     * 出生日期
     */
    private LocalDate birthDate;

    /**
     * 籍贯
     */
    private String nativePlace;

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
     * 备注
     */
    private String remark;

    /**
     * 宿舍ID
     */
    private Long dormitoryId;

    /**
     * 楼号
     */
    private String buildingNo;

    /**
     * 楼宇名称
     */
    private String buildingName;

    /**
     * 房间号
     */
    private String roomNo;

    /**
     * 床位号
     */
    private String bedNumber;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    public Integer getStatus() {
        return studentStatus;
    }

    public void setStatus(Integer status) {
        this.studentStatus = status;
    }

    public LocalDate getEnrollmentDate() {
        return admissionDate;
    }

    public void setEnrollmentDate(LocalDate enrollmentDate) {
        this.admissionDate = enrollmentDate;
    }

    // Explicit getters for fields that Lombok might not generate properly
    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public String getNativePlace() {
        return nativePlace;
    }

    public void setNativePlace(String nativePlace) {
        this.nativePlace = nativePlace;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}