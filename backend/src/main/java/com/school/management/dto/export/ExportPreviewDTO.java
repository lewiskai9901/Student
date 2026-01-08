package com.school.management.dto.export;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 导出预览DTO
 */
@Data
public class ExportPreviewDTO {

    /**
     * 检查日期
     */
    private String checkDate;

    /**
     * 检查名称
     */
    private String checkName;

    /**
     * 违纪总人次
     */
    private Integer totalCount;

    /**
     * 涉及班级数
     */
    private Integer classCount;

    /**
     * 涉及部门数
     */
    private Integer departmentCount;

    /**
     * 按班级统计（用于导出时选择班级）
     */
    private List<ClassStatDTO> classStats;

    /**
     * 分组后的数据（用于预览）
     */
    private List<DepartmentGroupDTO> groupedData;

    /**
     * 渲染后的HTML（用于预览）
     */
    private String renderedHtml;

    /**
     * 班级统计
     */
    @Data
    public static class ClassStatDTO {
        private Long classId;
        private String className;
        private Long departmentId;
        private String departmentName;
        private Long gradeId;
        private String gradeName;
        private Integer studentCount;
    }

    /**
     * 部门分组
     */
    @Data
    public static class DepartmentGroupDTO {
        private Long departmentId;
        private String departmentName;
        private Integer totalCount;
        private List<GradeGroupDTO> grades;
    }

    /**
     * 年级分组
     */
    @Data
    public static class GradeGroupDTO {
        private Long gradeId;
        private String gradeName;
        private Integer totalCount;
        private List<ClassGroupDTO> classes;
    }

    /**
     * 班级分组
     */
    @Data
    public static class ClassGroupDTO {
        private Long classId;
        private String className;
        private Integer totalCount;
        private List<StudentRecordDTO> students;
    }

    /**
     * 学生违纪记录
     */
    @Data
    public static class StudentRecordDTO {
        private Long studentId;
        private String studentNo;
        private String studentName;
        private String gender;
        private Long classId;
        private String className;
        private Long gradeId;
        private String gradeName;
        private Long departmentId;
        private String departmentName;
        private String headTeacher;

        // 宿舍/教室共用字段（根据 relationType 区分）
        private String buildingName;  // 楼号（宿舍楼或教学楼）
        private String roomNo;        // 房间号（宿舍号或教室号）

        // 检查相关字段
        private String categoryName;
        private String relationType;  // 关联类型：无、宿舍、教室
        private Long deductionItemId;
        private String deductionItemName;
        private String deductMode;
        private Double deductScore;
        private String remark;
        private String checkerName;
        private Integer checkRound;

        /**
         * 扣分次数（每条扣分记录至少为1）
         */
        private Integer personCount;

        /**
         * 扣分项原总扣分 = deductScore * personCount
         */
        private Double originalTotalScore;

        /**
         * 扣分项加权扣分分值
         */
        private Double weightedDeductScore;

        /**
         * 扣分项加权总扣分 = weightedDeductScore * personCount
         */
        private Double totalWeightedDeductScore;

        /**
         * 检查类别原总扣分（同一班级同一检查类别的所有记录此值相同）
         */
        private Double categoryOriginalScore;

        /**
         * 检查类别加权总扣分（同一班级同一检查类别的所有记录此值相同）
         */
        private Double categoryWeightedScore;

        /**
         * 班级原总扣分（同一班级的所有记录此值相同）
         */
        private Double classOriginalScore;

        /**
         * 班级加权总扣分（同一班级的所有记录此值相同）
         */
        private Double classWeightedScore;
    }
}
