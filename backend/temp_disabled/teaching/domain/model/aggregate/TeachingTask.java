package com.school.management.domain.teaching.model.aggregate;

import com.school.management.domain.shared.AggregateRoot;
import com.school.management.domain.teaching.model.entity.TaskTeacher;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 教学任务聚合根
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeachingTask extends AggregateRoot<Long> {

    private Long id;

    /**
     * 任务编号
     */
    private String taskCode;

    /**
     * 学期ID
     */
    private Long semesterId;

    /**
     * 课程ID
     */
    private Long courseId;

    /**
     * 教学班级ID
     */
    private Long classId;

    /**
     * 开课部门ID
     */
    private Long orgUnitId;

    /**
     * 学生人数
     */
    private Integer studentCount;

    /**
     * 周学时
     */
    private Integer weeklyHours;

    /**
     * 总学时
     */
    private Integer totalHours;

    /**
     * 起始周
     */
    @Builder.Default
    private Integer startWeek = 1;

    /**
     * 结束周
     */
    @Builder.Default
    private Integer endWeek = 16;

    /**
     * 排课状态：0未排 1部分排 2已排完
     */
    @Builder.Default
    private Integer schedulingStatus = 0;

    /**
     * 任务状态：0草稿 1已确认 2已取消
     */
    private Integer taskStatus;

    /**
     * 备注
     */
    private String remark;

    /**
     * 任务教师列表
     */
    @Builder.Default
    private List<TaskTeacher> teachers = new ArrayList<>();

    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;

    @Override
    public Long getId() {
        return id;
    }

    /**
     * 分配教师
     */
    public void assignTeacher(TaskTeacher teacher) {
        if (teachers == null) {
            teachers = new ArrayList<>();
        }
        // 检查是否已分配
        boolean exists = teachers.stream()
                .anyMatch(t -> t.getTeacherId().equals(teacher.getTeacherId()));
        if (exists) {
            throw new IllegalStateException("该教师已分配到此任务");
        }
        teacher.setTaskId(this.id);
        teachers.add(teacher);
    }

    /**
     * 移除教师
     */
    public void removeTeacher(Long teacherId) {
        if (teachers != null) {
            teachers.removeIf(t -> t.getTeacherId().equals(teacherId));
        }
    }

    /**
     * 获取主讲教师
     */
    public Optional<TaskTeacher> getMainTeacher() {
        return teachers.stream()
                .filter(t -> t.getTeacherRole() == 1)
                .findFirst();
    }

    /**
     * 获取主讲教师ID
     */
    public Long getMainTeacherId() {
        return getMainTeacher().map(TaskTeacher::getTeacherId).orElse(null);
    }

    /**
     * 确认任务
     */
    public void confirm() {
        if (teachers == null || teachers.isEmpty()) {
            throw new IllegalStateException("请先分配教师");
        }
        if (getMainTeacher().isEmpty()) {
            throw new IllegalStateException("请先分配主讲教师");
        }
        this.taskStatus = 1;
    }

    /**
     * 取消任务
     */
    public void cancel() {
        if (schedulingStatus > 0) {
            throw new IllegalStateException("已排课的任务不能取消");
        }
        this.taskStatus = 2;
    }

    /**
     * 更新排课状态
     */
    public void updateSchedulingStatus(int scheduledHours) {
        if (scheduledHours == 0) {
            this.schedulingStatus = 0;
        } else if (scheduledHours < totalHours) {
            this.schedulingStatus = 1;
        } else {
            this.schedulingStatus = 2;
        }
    }

    /**
     * 计算教学周数
     */
    public int getTeachingWeeks() {
        return endWeek - startWeek + 1;
    }

    /**
     * 计算每周需要排几次课（连堂算一次）
     */
    public int getSessionsPerWeek(int hoursPerSession) {
        return (int) Math.ceil((double) weeklyHours / hoursPerSession);
    }

    /**
     * 是否已确认
     */
    public boolean isConfirmed() {
        return taskStatus != null && taskStatus == 1;
    }

    /**
     * 是否已取消
     */
    public boolean isCancelled() {
        return taskStatus != null && taskStatus == 2;
    }

    /**
     * 是否已排课完成
     */
    public boolean isFullyScheduled() {
        return schedulingStatus != null && schedulingStatus == 2;
    }
}
