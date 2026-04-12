package com.school.management.domain.teaching.model.offering;

import com.school.management.domain.shared.AggregateRoot;
import lombok.Getter;

@Getter
public class SemesterOffering extends AggregateRoot<Long> {
    private Long semesterId;
    private Long planId;
    private Long planCourseId;
    private Long courseId;
    private String applicableGrade;
    private Integer weeklyHours;
    private Integer totalWeeks;
    private Integer startWeek;
    private Integer endWeek;
    private Integer weekType; // 0=每周 1=单周 2=双周
    private Integer courseCategory;
    private Integer courseType;
    private Boolean allowCombined;
    private Integer maxCombinedClasses;
    private Boolean allowWalking;
    private Integer status; // 0draft 1confirmed 2allocated
    private String remark;
    private Long createdBy;

    protected SemesterOffering() {}

    public static SemesterOffering create(Long semesterId, Long courseId, String applicableGrade,
            Integer weeklyHours, Integer startWeek, Integer endWeek, Long createdBy) {
        SemesterOffering o = new SemesterOffering();
        o.semesterId = semesterId;
        o.courseId = courseId;
        o.applicableGrade = applicableGrade;
        o.weeklyHours = weeklyHours;
        o.startWeek = startWeek != null ? startWeek : 1;
        o.endWeek = endWeek;
        o.weekType = 0;
        o.allowCombined = false;
        o.maxCombinedClasses = 2;
        o.allowWalking = false;
        o.status = 0;
        o.createdBy = createdBy;
        return o;
    }

    public static SemesterOffering reconstruct(Long id, Long semesterId, Long planId,
            Long planCourseId, Long courseId,
            String applicableGrade, Integer weeklyHours, Integer totalWeeks,
            Integer startWeek, Integer endWeek, Integer weekType, Integer courseCategory, Integer courseType,
            Boolean allowCombined, Integer maxCombinedClasses, Boolean allowWalking,
            Integer status, String remark, Long createdBy) {
        SemesterOffering o = new SemesterOffering();
        o.id = id;
        o.semesterId = semesterId;
        o.planId = planId;
        o.planCourseId = planCourseId;
        o.courseId = courseId;
        o.applicableGrade = applicableGrade;
        o.weeklyHours = weeklyHours;
        o.totalWeeks = totalWeeks;
        o.startWeek = startWeek;
        o.endWeek = endWeek;
        o.weekType = weekType != null ? weekType : 0;
        o.courseCategory = courseCategory;
        o.courseType = courseType;
        o.allowCombined = allowCombined;
        o.maxCombinedClasses = maxCombinedClasses;
        o.allowWalking = allowWalking;
        o.status = status;
        o.remark = remark;
        o.createdBy = createdBy;
        return o;
    }

    public void confirm() {
        if (this.status != 0) throw new IllegalStateException("只能确认草稿状态的开课计划");
        this.status = 1;
    }

    public void update(Integer weeklyHours, Integer startWeek, Integer endWeek, Integer weekType,
            Integer courseCategory, Integer courseType, Boolean allowCombined,
            Integer maxCombinedClasses, Boolean allowWalking, String remark) {
        this.weeklyHours = weeklyHours;
        this.startWeek = startWeek;
        this.endWeek = endWeek;
        this.weekType = weekType != null ? weekType : 0;
        this.courseCategory = courseCategory;
        this.courseType = courseType;
        this.allowCombined = allowCombined;
        this.maxCombinedClasses = maxCombinedClasses;
        this.allowWalking = allowWalking;
        this.remark = remark;
    }
}
