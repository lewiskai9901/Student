package com.school.management.entity.evaluation;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 学生关系快照实体类
 * 记录学生在某一时刻的组织关系
 *
 * @author Claude
 * @since 2025-11-28
 */
@Data
@TableName("student_relationship_snapshots")
public class StudentRelationshipSnapshot implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 快照日期
     */
    private LocalDate snapshotDate;

    /**
     * 快照类型: DAILY/SEMESTER_START/SEMESTER_END
     */
    private String snapshotType;

    /**
     * 学生ID
     */
    private Long studentId;

    /**
     * 学号
     */
    private String studentNo;

    /**
     * 姓名
     */
    private String studentName;

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
     * 组织单元ID
     */
    private Long orgUnitId;

    /**
     * 组织单元名称
     */
    private String orgUnitName;

    /**
     * 宿舍ID
     */
    private Long dormitoryId;

    /**
     * 楼栋ID
     */
    private Long buildingId;

    /**
     * 楼栋名称
     */
    private String buildingName;

    /**
     * 宿舍号
     */
    private String dormitoryNo;

    /**
     * 床位号
     */
    private String bedNo;

    /**
     * 是否宿舍长
     */
    private Integer isDormLeader;

    /**
     * 学生状态: 1在读,2休学,3退学,4毕业
     */
    private Integer studentStatus;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    // ==================== 快照类型常量 ====================

    public static final String TYPE_DAILY = "DAILY";                    // 日常快照
    public static final String TYPE_SEMESTER_START = "SEMESTER_START";  // 学期初快照
    public static final String TYPE_SEMESTER_END = "SEMESTER_END";      // 学期末快照

    // ==================== 学生状态常量 ====================

    public static final int STUDENT_STATUS_ACTIVE = 1;        // 在读
    public static final int STUDENT_STATUS_SUSPENDED = 2;     // 休学
    public static final int STUDENT_STATUS_WITHDRAWN = 3;     // 退学
    public static final int STUDENT_STATUS_GRADUATED = 4;     // 毕业
}
