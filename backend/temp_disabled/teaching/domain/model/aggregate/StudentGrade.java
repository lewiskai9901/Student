package com.school.management.domain.teaching.model.aggregate;

import com.school.management.domain.shared.AggregateRoot;
import com.school.management.domain.teaching.model.entity.GradeItem;
import com.school.management.domain.teaching.model.valueobject.GradeScale;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 学生成绩聚合根
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentGrade extends AggregateRoot<Long> {

    private Long id;

    /**
     * 录入批次ID
     */
    private Long batchId;

    /**
     * 学期ID
     */
    private Long semesterId;

    /**
     * 教学任务ID
     */
    private Long taskId;

    /**
     * 课程ID
     */
    private Long courseId;

    /**
     * 学生ID
     */
    private Long studentId;

    /**
     * 班级ID
     */
    private Long classId;

    /**
     * 评分制：1百分制 2五级制 3二级制
     */
    @Builder.Default
    private Integer gradeScaleType = 1;

    /**
     * 平时成绩
     */
    private BigDecimal regularScore;

    /**
     * 期中成绩
     */
    private BigDecimal midtermScore;

    /**
     * 期末成绩
     */
    private BigDecimal finalScore;

    /**
     * 实验成绩
     */
    private BigDecimal experimentScore;

    /**
     * 总评成绩
     */
    private BigDecimal totalScore;

    /**
     * 等级（五级制：A/B/C/D/F）
     */
    private String gradeLevel;

    /**
     * 绩点
     */
    private BigDecimal gradePoint;

    /**
     * 是否通过
     */
    private Boolean passed;

    /**
     * 获得学分
     */
    private BigDecimal creditsEarned;

    /**
     * 状态：0未录入 1已录入 2已确认 3已发布 4有异议
     */
    @Builder.Default
    private Integer gradeStatus = 0;

    /**
     * 是否补考成绩
     */
    @Builder.Default
    private Boolean isMakeup = false;

    /**
     * 是否重修成绩
     */
    @Builder.Default
    private Boolean isRetake = false;

    /**
     * 补考次数
     */
    @Builder.Default
    private Integer makeupCount = 0;

    /**
     * 录入教师ID
     */
    private Long inputTeacherId;

    /**
     * 录入时间
     */
    private LocalDateTime inputTime;

    /**
     * 确认时间
     */
    private LocalDateTime confirmTime;

    /**
     * 发布时间
     */
    private LocalDateTime publishTime;

    /**
     * 备注
     */
    private String remark;

    /**
     * 成绩明细列表
     */
    @Builder.Default
    private List<GradeItem> items = new ArrayList<>();

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Override
    public Long getId() {
        return id;
    }

    /**
     * 录入成绩
     */
    public void recordGrade(BigDecimal regularScore, BigDecimal midtermScore,
                            BigDecimal finalScore, BigDecimal experimentScore,
                            Long teacherId) {
        this.regularScore = regularScore;
        this.midtermScore = midtermScore;
        this.finalScore = finalScore;
        this.experimentScore = experimentScore;
        this.inputTeacherId = teacherId;
        this.inputTime = LocalDateTime.now();
        this.gradeStatus = 1;
    }

    /**
     * 计算总评成绩（默认比例：平时30%，期末70%）
     */
    public void calculateTotalScore() {
        calculateTotalScore(new BigDecimal("0.30"), new BigDecimal("0.70"));
    }

    /**
     * 计算总评成绩（自定义比例）
     */
    public void calculateTotalScore(BigDecimal regularWeight, BigDecimal finalWeight) {
        BigDecimal total = BigDecimal.ZERO;

        if (regularScore != null) {
            total = total.add(regularScore.multiply(regularWeight));
        }
        if (finalScore != null) {
            total = total.add(finalScore.multiply(finalWeight));
        }

        this.totalScore = total.setScale(1, RoundingMode.HALF_UP);
        updatePassedAndGradePoint();
    }

    /**
     * 根据成绩项计算总评
     */
    public void calculateFromItems() {
        BigDecimal total = BigDecimal.ZERO;
        for (GradeItem item : items) {
            if (item.getWeightedScore() != null) {
                total = total.add(item.getWeightedScore());
            }
        }
        this.totalScore = total.setScale(1, RoundingMode.HALF_UP);
        updatePassedAndGradePoint();
    }

    /**
     * 更新通过状态和绩点
     */
    private void updatePassedAndGradePoint() {
        if (totalScore == null) return;

        // 更新等级和绩点
        GradeScale scale = GradeScale.fromScore(totalScore, gradeScaleType);
        this.gradeLevel = scale.getLevel();
        this.gradePoint = scale.getGradePoint();
        this.passed = scale.isPassed();
    }

    /**
     * 确认成绩
     */
    public void confirm() {
        if (gradeStatus < 1) {
            throw new IllegalStateException("成绩尚未录入");
        }
        this.gradeStatus = 2;
        this.confirmTime = LocalDateTime.now();
    }

    /**
     * 发布成绩
     */
    public void publish() {
        if (gradeStatus < 2) {
            throw new IllegalStateException("成绩尚未确认");
        }
        this.gradeStatus = 3;
        this.publishTime = LocalDateTime.now();
    }

    /**
     * 标记为有异议
     */
    public void markDisputed() {
        this.gradeStatus = 4;
    }

    /**
     * 添加成绩明细项
     */
    public void addItem(GradeItem item) {
        if (items == null) {
            items = new ArrayList<>();
        }
        item.setGradeId(this.id);
        items.add(item);
    }

    /**
     * 是否可编辑
     */
    public boolean isEditable() {
        return gradeStatus < 2;
    }

    /**
     * 是否已发布
     */
    public boolean isPublished() {
        return gradeStatus >= 3;
    }

    /**
     * 获取成绩状态名称
     */
    public String getGradeStatusName() {
        return switch (gradeStatus) {
            case 0 -> "未录入";
            case 1 -> "已录入";
            case 2 -> "已确认";
            case 3 -> "已发布";
            case 4 -> "有异议";
            default -> "未知";
        };
    }
}
