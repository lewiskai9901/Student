package com.school.management.infrastructure.persistence.student;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 学生持久化对象
 */
@Data
@TableName("students")
public class StudentPO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 学号
     */
    private String studentNo;

    /**
     * 用户ID (关联users表)
     */
    private Long userId;

    /**
     * 姓名 (来自users表，非students表字段)
     */
    @TableField(exist = false)
    private String name;

    /**
     * 性别: 1-男, 2-女 (来自users表，非students表字段)
     */
    @TableField(exist = false)
    private Integer gender;

    /**
     * 身份证号 (来自users表，非students表字段)
     */
    @TableField(exist = false)
    private String idCard;

    /**
     * 手机号 (来自users表，非students表字段)
     */
    @TableField(exist = false)
    private String phone;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 出生日期
     */
    private LocalDate birthDate;

    /**
     * 入学日期
     */
    private LocalDate enrollmentDate;

    /**
     * 预计毕业日期
     */
    private LocalDate expectedGraduationDate;

    /**
     * 班级ID
     */
    private Long classId;

    /**
     * 学籍状态: 0-在读, 1-休学, 2-退学, 3-毕业, 4-转学, 5-开除
     */
    @TableField("student_status")
    private Integer status;

    /**
     * 头像URL
     */
    private String avatarUrl;

    /**
     * 家庭住址
     */
    private String homeAddress;

    /**
     * 紧急联系人
     */
    private String emergencyContact;

    /**
     * 紧急联系电话
     */
    private String emergencyPhone;

    /**
     * 备注
     */
    private String remark;

    private Long tenantId;

    /**
     * 逻辑删除
     */
    @TableLogic
    private Integer deleted;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
