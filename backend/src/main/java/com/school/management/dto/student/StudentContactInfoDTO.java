package com.school.management.dto.student;

import lombok.Data;

/**
 * 学生联系信息DTO
 * 包含：手机号、家庭地址、户籍信息等联系方式
 *
 * @author system
 * @version 2.0.0
 * @since 2024-12-31
 */
@Data
public class StudentContactInfoDTO {

    /**
     * 手机号
     */
    private String phone;

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
}
