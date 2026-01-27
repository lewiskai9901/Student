package com.school.management.domain.teaching.model.aggregate;

import com.school.management.domain.shared.AggregateRoot;
import com.school.management.domain.teaching.model.entity.ExamArrangement;
import com.school.management.domain.teaching.model.entity.ExamRoom;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 考试批次聚合根
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Examination extends AggregateRoot<Long> {

    private Long id;

    /**
     * 批次代码
     */
    private String batchCode;

    /**
     * 批次名称
     */
    private String batchName;

    /**
     * 学期ID
     */
    private Long semesterId;

    /**
     * 考试类型：1期中 2期末 3补考 4重修
     */
    private Integer examType;

    /**
     * 考试开始日期
     */
    private LocalDate startDate;

    /**
     * 考试结束日期
     */
    private LocalDate endDate;

    /**
     * 报名截止时间（补考/重修用）
     */
    private LocalDateTime registrationDeadline;

    /**
     * 状态：0筹备中 1报名中 2已安排 3进行中 4已结束
     */
    @Builder.Default
    private Integer status = 0;

    /**
     * 备注
     */
    private String remark;

    /**
     * 考试安排列表
     */
    @Builder.Default
    private List<ExamArrangement> arrangements = new ArrayList<>();

    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;

    @Override
    public Long getId() {
        return id;
    }

    /**
     * 获取考试类型名称
     */
    public String getExamTypeName() {
        return switch (examType) {
            case 1 -> "期中考试";
            case 2 -> "期末考试";
            case 3 -> "补考";
            case 4 -> "重修考试";
            default -> "未知";
        };
    }

    /**
     * 添加考试安排
     */
    public void addArrangement(ExamArrangement arrangement) {
        if (arrangements == null) {
            arrangements = new ArrayList<>();
        }
        arrangement.setBatchId(this.id);
        arrangements.add(arrangement);
    }

    /**
     * 移除考试安排
     */
    public void removeArrangement(Long arrangementId) {
        if (arrangements != null) {
            arrangements.removeIf(a -> a.getId().equals(arrangementId));
        }
    }

    /**
     * 开始报名（补考/重修）
     */
    public void startRegistration() {
        if (examType < 3) {
            throw new IllegalStateException("普通考试无需报名");
        }
        this.status = 1;
    }

    /**
     * 完成安排
     */
    public void completeArrangement() {
        if (arrangements == null || arrangements.isEmpty()) {
            throw new IllegalStateException("尚未添加考试安排");
        }
        this.status = 2;
    }

    /**
     * 开始考试
     */
    public void start() {
        if (status < 2) {
            throw new IllegalStateException("考试安排尚未完成");
        }
        this.status = 3;
    }

    /**
     * 结束考试
     */
    public void finish() {
        this.status = 4;
    }

    /**
     * 获取考试天数
     */
    public long getExamDays() {
        if (startDate != null && endDate != null) {
            return java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;
        }
        return 0;
    }

    /**
     * 获取总应考人数
     */
    public int getTotalStudents() {
        return arrangements.stream()
                .mapToInt(a -> a.getTotalStudents() != null ? a.getTotalStudents() : 0)
                .sum();
    }

    /**
     * 是否可以编辑
     */
    public boolean isEditable() {
        return status <= 1;
    }

    /**
     * 是否正在进行
     */
    public boolean isOngoing() {
        return status == 3;
    }

    /**
     * 是否已结束
     */
    public boolean isFinished() {
        return status == 4;
    }
}
