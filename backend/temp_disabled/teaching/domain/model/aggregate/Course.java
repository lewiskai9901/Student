package com.school.management.domain.teaching.model.aggregate;

import com.school.management.domain.shared.AggregateRoot;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 课程聚合根
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class Course extends AggregateRoot<Long> {

    private Long id;

    /**
     * 课程代码
     */
    private String courseCode;

    /**
     * 课程名称
     */
    private String courseName;

    /**
     * 课程英文名
     */
    private String courseNameEn;

    /**
     * englishName的别名getter
     */
    private String englishName;

    public String getEnglishName() {
        return englishName != null ? englishName : courseNameEn;
    }

    public void setEnglishName(String englishName) {
        this.englishName = englishName;
        this.courseNameEn = englishName;
    }

    /**
     * 课程类别：1公共基础课 2专业基础课 3专业核心课 4专业选修课 5通识选修课 6实践课
     */
    private Integer courseCategory;

    /**
     * 课程性质：1必修 2限选 3任选
     */
    private Integer courseType;

    /**
     * 课程属性：1理论课 2实验课 3理论+实验 4实践课
     */
    private Integer courseNature;

    /**
     * 学分
     */
    private BigDecimal credits;

    /**
     * 总学时
     */
    private Integer totalHours;

    /**
     * 理论学时
     */
    private Integer theoryHours;

    /**
     * 实践/实验学时
     */
    private Integer practiceHours;

    /**
     * 周学时
     */
    private Integer weeklyHours;

    /**
     * 考核方式：1考试 2考查
     */
    private Integer examType;

    /**
     * 评分制：1百分制 2五级制 3二级制
     */
    private Integer gradeScaleType;

    /**
     * 开课部门ID
     */
    private Long orgUnitId;

    /**
     * 先修课程ID数组
     */
    private List<Long> prerequisiteIds;

    /**
     * 课程简介
     */
    private String description;

    /**
     * 教学大纲URL
     */
    private String syllabusUrl;

    /**
     * 状态：1启用 0停用
     */
    private Integer status;

    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;

    @Override
    public Long getId() {
        return id;
    }

    /**
     * 获取课程类别名称
     */
    public String getCourseCategoryName() {
        return switch (courseCategory) {
            case 1 -> "公共基础课";
            case 2 -> "专业基础课";
            case 3 -> "专业核心课";
            case 4 -> "专业选修课";
            case 5 -> "通识选修课";
            case 6 -> "实践课";
            default -> "未知";
        };
    }

    /**
     * 获取课程性质名称
     */
    public String getCourseTypeName() {
        return switch (courseType) {
            case 1 -> "必修";
            case 2 -> "限选";
            case 3 -> "任选";
            default -> "未知";
        };
    }

    /**
     * 获取课程属性名称
     */
    public String getCourseNatureName() {
        return switch (courseNature) {
            case 1 -> "理论课";
            case 2 -> "实验课";
            case 3 -> "理论+实验";
            case 4 -> "实践课";
            default -> "未知";
        };
    }

    /**
     * 是否需要实验室
     */
    public boolean needsLab() {
        return courseNature != null && (courseNature == 2 || courseNature == 3);
    }

    /**
     * 是否实践类课程
     */
    public boolean isPractice() {
        return courseNature != null && courseNature == 4;
    }

    /**
     * 启用课程
     */
    public void enable() {
        this.status = 1;
    }

    /**
     * 停用课程
     */
    public void disable() {
        this.status = 0;
    }

    /**
     * 验证学时是否正确
     */
    public boolean validateHours() {
        if (totalHours == null) return true;
        int sum = (theoryHours != null ? theoryHours : 0) + (practiceHours != null ? practiceHours : 0);
        return sum <= totalHours;
    }
}
