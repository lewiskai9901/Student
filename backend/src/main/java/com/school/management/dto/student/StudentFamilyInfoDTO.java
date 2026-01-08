package com.school.management.dto.student;

import lombok.Data;

/**
 * 学生家庭信息DTO
 * 包含：监护人、父母、紧急联系人等家庭成员信息
 *
 * @author system
 * @version 2.0.0
 * @since 2024-12-31
 */
@Data
public class StudentFamilyInfoDTO {

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
     * 监护人身份证号
     */
    private String guardianIdCard;

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
     * 紧急联系人
     */
    private String emergencyContact;

    /**
     * 紧急联系电话
     */
    private String emergencyPhone;
}
