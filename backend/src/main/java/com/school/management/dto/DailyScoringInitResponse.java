package com.school.management.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 日常检查打分初始化数据响应DTO
 *
 * @author system
 * @since 1.0.0
 */
@Data
public class DailyScoringInitResponse {

    private Long checkId;
    private String checkName;
    private LocalDate checkDate;
    private Integer checkType;
    private Long checkerId;
    private String checkerName;

    /**
     * 总轮次数
     */
    private Integer totalRounds;

    /**
     * 轮次名称列表
     */
    private List<String> roundNames;

    /**
     * 检查目标班级列表
     */
    private List<TargetClassInfo> targetClasses;

    /**
     * 检查类别列表
     */
    private List<CategoryInfo> categories;

    /**
     * 关联资源（宿舍/教室）
     * key: categoryId
     */
    private Map<Long, LinkResourceInfo> linkResources;

    /**
     * 已有的打分明细
     */
    private List<ScoringDetailInfo> existingDetails;

    /**
     * 目标班级信息
     */
    @Data
    public static class TargetClassInfo {
        private Long classId;
        private String className;
        private Long gradeId;
        private String gradeName;
        private Long departmentId;
        private String departmentName;
        private Integer studentCount;
    }

    /**
     * 检查类别信息
     */
    @Data
    public static class CategoryInfo {
        private Long id;
        private Long categoryId;
        private String categoryName;
        private String categoryCode;
        private Integer linkType;  // 0=无关联, 1=宿舍, 2=教室
        private Integer checkRounds;  // 本次检查轮次数（已废弃）
        private String participatedRounds;  // 参与的轮次，如"1,3"
        private List<Integer> participatedRoundsList;  // 参与的轮次列表，方便前端使用
        private Integer sortOrder;
        private Integer enableWeight;  // 是否启用加权
        private List<DeductionItemInfo> deductionItems;
    }

    /**
     * 扣分项信息
     */
    @Data
    public static class DeductionItemInfo {
        private Long id;
        private String itemName;
        private Integer deductMode;  // 1=固定分值, 2=按人数, 3=范围配置
        private BigDecimal fixedScore;
        private BigDecimal baseScore;
        private BigDecimal perPersonScore;
        private String rangeConfig;
        private String description;
        private Integer allowPhoto;
        private Integer allowRemark;
        private Integer allowStudents;
        private Integer sortOrder;
    }

    /**
     * 关联资源信息
     */
    @Data
    public static class LinkResourceInfo {
        private Integer linkType;
        private List<ClassLinkResource> classResources;
    }

    /**
     * 班级关联资源
     */
    @Data
    public static class ClassLinkResource {
        private Long classId;
        private String className;
        private List<DormitoryInfo> dormitories;
        private List<ClassroomInfo> classrooms;
    }

    /**
     * 宿舍信息
     */
    @Data
    public static class DormitoryInfo {
        private Long id;
        private String dormitoryNo;
        private String buildingName;
        private Integer floor;
        private Integer currentCount;
    }

    /**
     * 教室信息
     */
    @Data
    public static class ClassroomInfo {
        private Long id;
        private String classroomNo;
        private String buildingName;
        private Integer floor;
    }

    /**
     * 打分明细信息
     */
    @Data
    public static class ScoringDetailInfo {
        private Long id;
        private Integer checkRound;  // 检查轮次
        private Long categoryId;
        private Long classId;
        private String className;
        private Long deductionItemId;
        private String deductionItemName;
        private Integer deductMode;
        private Integer linkType;
        private Long linkId;
        private String linkNo;
        private BigDecimal deductScore;
        private Integer personCount;
        private String studentIds;
        private String studentNames;
        private String description;
        private String remark;
        private String photoUrls;
        private Integer appealStatus;
    }
}
