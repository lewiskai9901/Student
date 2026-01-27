package com.school.management.domain.teaching.model.aggregate;

import com.school.management.domain.shared.AggregateRoot;
import com.school.management.domain.teaching.model.entity.PlanCourse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 培养方案聚合根
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class CurriculumPlan extends AggregateRoot<Long> {

    private Long id;

    /**
     * 方案代码
     */
    private String planCode;

    /**
     * 方案名称
     */
    private String planName;

    /**
     * 专业ID
     */
    private Long majorId;

    /**
     * 入学年份
     */
    private Integer enrollYear;

    /**
     * 学制（年）
     */
    private Integer duration;

    /**
     * 培养层次：1中专 2大专 3本科
     */
    private Integer educationLevel;

    /**
     * 毕业总学分要求
     */
    private BigDecimal totalCredits;

    /**
     * 必修学分要求
     */
    private BigDecimal requiredCredits;

    /**
     * 选修学分要求
     */
    private BigDecimal electiveCredits;

    /**
     * 实践学分要求
     */
    private BigDecimal practiceCredits;

    /**
     * 授予学位类型
     */
    private String degreeType;

    /**
     * 培养目标
     */
    private String objectives;

    /**
     * 毕业要求
     */
    private String requirements;

    /**
     * 备注
     */
    private String remark;

    /**
     * 版本号 - 使用Long以匹配AggregateRoot基类
     */
    @Builder.Default
    private Long version = 1L;

    @Override
    public Long getVersion() {
        return version;
    }

    /**
     * 状态：0草稿 1已发布 2已归档
     */
    private Integer status;

    /**
     * 发布时间
     */
    private LocalDateTime publishedAt;

    /**
     * 发布人
     */
    private Long publishedBy;

    /**
     * 方案课程列表
     */
    @Builder.Default
    private List<PlanCourse> courses = new ArrayList<>();

    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;

    @Override
    public Long getId() {
        return id;
    }

    // === Alias methods for backward compatibility ===

    public Integer getGradeYear() {
        return enrollYear;
    }

    public void setGradeYear(Integer gradeYear) {
        this.enrollYear = gradeYear;
    }

    public Integer getEducationLength() {
        return duration;
    }

    public void setEducationLength(Integer educationLength) {
        this.duration = educationLength;
    }

    public String getTrainingObjective() {
        return objectives;
    }

    public void setTrainingObjective(String trainingObjective) {
        this.objectives = trainingObjective;
    }

    public String getGraduationRequirement() {
        return requirements;
    }

    public void setGraduationRequirement(String graduationRequirement) {
        this.requirements = graduationRequirement;
    }

    /**
     * 添加课程到方案
     */
    public void addCourse(PlanCourse course) {
        if (courses == null) {
            courses = new ArrayList<>();
        }
        course.setPlanId(this.id);
        courses.add(course);
    }

    /**
     * 移除课程
     */
    public void removeCourse(Long courseId) {
        if (courses != null) {
            courses.removeIf(c -> c.getCourseId().equals(courseId));
        }
    }

    /**
     * 获取指定学期的课程
     */
    public List<PlanCourse> getCoursesBySemester(int semesterNumber) {
        return courses.stream()
                .filter(c -> c.getSemesterNumber() == semesterNumber)
                .collect(Collectors.toList());
    }

    /**
     * 按学期分组课程
     */
    public Map<Integer, List<PlanCourse>> getCoursesBySemesterGrouped() {
        return courses.stream()
                .collect(Collectors.groupingBy(PlanCourse::getSemesterNumber));
    }

    /**
     * 计算总学分
     */
    public BigDecimal calculateTotalCredits() {
        return courses.stream()
                .map(PlanCourse::getCredits)
                .filter(c -> c != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 发布方案
     */
    public void publish(Long publisherId) {
        if (this.status == 1) {
            throw new IllegalStateException("方案已发布");
        }
        this.status = 1;
        this.publishedAt = LocalDateTime.now();
        this.publishedBy = publisherId;
    }

    /**
     * 归档方案
     */
    public void archive() {
        this.status = 2;
    }

    /**
     * 创建新版本
     */
    public CurriculumPlan createNewVersion() {
        CurriculumPlan newPlan = CurriculumPlan.builder()
                .planCode(this.planCode + "-v" + (this.version + 1))
                .planName(this.planName)
                .majorId(this.majorId)
                .enrollYear(this.enrollYear)
                .educationLevel(this.educationLevel)
                .duration(this.duration)
                .totalCredits(this.totalCredits)
                .requiredCredits(this.requiredCredits)
                .electiveCredits(this.electiveCredits)
                .practiceCredits(this.practiceCredits)
                .degreeType(this.degreeType)
                .objectives(this.objectives)
                .requirements(this.requirements)
                .version(this.version + 1L)
                .status(0) // 草稿状态
                .build();

        // 复制课程
        for (PlanCourse course : this.courses) {
            newPlan.addCourse(course.copy());
        }

        return newPlan;
    }

    /**
     * 是否草稿状态
     */
    public boolean isDraft() {
        return status == 0;
    }

    /**
     * 是否已发布
     */
    public boolean isPublished() {
        return status == 1;
    }

    /**
     * 是否已归档
     */
    public boolean isArchived() {
        return status == 2;
    }
}
