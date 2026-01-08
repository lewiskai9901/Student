package com.school.management.dto.student;

import lombok.Data;

/**
 * 学生补充信息DTO
 * 包含：健康状况、资助信息、特殊备注等补充信息
 *
 * @author system
 * @version 2.0.0
 * @since 2024-12-31
 */
@Data
public class StudentSupplementaryInfoDTO {

    /**
     * 健康状况
     */
    private String healthStatus;

    /**
     * 过敏史
     */
    private String allergies;

    /**
     * 是否建档立卡(0否1是)
     */
    private Integer isPovertyRegistered;

    /**
     * 资助申请类型
     */
    private String financialAidType;

    /**
     * 特殊备注
     */
    private String specialNotes;

    /**
     * 备注
     */
    private String remark;
}
