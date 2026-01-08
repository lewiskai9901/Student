package com.school.management.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 学生实体
 *
 * @author system
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("students")
public class Student extends BaseEntity {

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
     * 真实姓名(来自users表,非students表字段)
     */
    @TableField(exist = false)
    private String realName;

    /**
     * 性别: 1男 2女 (来自users表,非students表字段)
     */
    @TableField(exist = false)
    private Integer gender;

    /**
     * 手机号(来自users表,非students表字段)
     */
    @TableField(exist = false)
    private String phone;

    /**
     * 身份证号(来自users表,非students表字段)
     */
    @TableField(exist = false)
    private String idCard;

    /**
     * 毕业学校
     */
    @TableField(exist = false)
    private String graduatedSchool;

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
     * 班级ID
     */
    private Long classId;

    /**
     * 年级ID(关联grades表)
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
     * 入学日期
     */
    private LocalDate admissionDate;

    /**
     * 毕业日期
     */
    private LocalDate graduationDate;

    /**
     * 入学层次(如:初中/高中,非数据库字段)
     */
    @TableField(exist = false)
    private String entryLevel;

    /**
     * 学制(如:3年/4年,非数据库字段)
     */
    @TableField(exist = false)
    private String educationSystem;

    /**
     * 学生状态: 0在读 1休学 2退学 3毕业 4转学
     */
    private Integer studentStatus;

    /**
     * 宿舍ID
     */
    private Long dormitoryId;

    /**
     * 床位号
     */
    @TableField("bed_number")
    private String bedNumber;

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
     * 用户ID(关联users表)
     */
    private Long userId;

    // ========== 关联字段（非数据库字段） ==========

    /**
     * 班级信息(关联查询)
     */
    @TableField(exist = false)
    private Class classInfo;

    /**
     * 班级名称(关联查询)
     */
    @TableField(exist = false)
    private String className;

    /**
     * 专业名称(关联查询)
     */
    @TableField(exist = false)
    private String majorName;

    /**
     * 年级名称(关联查询)
     */
    @TableField(exist = false)
    private String gradeName;

    /**
     * 宿舍信息(关联查询)
     */
    @TableField(exist = false)
    private Dormitory dormitory;

    /**
     * 宿舍楼号(关联查询)
     */
    @TableField(exist = false)
    private String buildingNo;

    /**
     * 宿舍房间号(关联查询)
     */
    @TableField(exist = false)
    private String roomNo;
}