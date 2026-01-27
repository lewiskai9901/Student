package com.school.management.domain.teaching.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 考试安排实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamArrangement {

    private Long id;

    /**
     * 考试批次ID
     */
    private Long batchId;

    /**
     * 课程ID
     */
    private Long courseId;

    /**
     * 考试日期
     */
    private LocalDate examDate;

    /**
     * 开始时间
     */
    private LocalTime startTime;

    /**
     * 结束时间
     */
    private LocalTime endTime;

    /**
     * 考试时长（分钟）
     */
    private Integer duration;

    /**
     * 考试形式：1闭卷 2开卷 3机考 4口试 5实操
     */
    @Builder.Default
    private Integer examForm = 1;

    /**
     * 应考人数
     */
    @Builder.Default
    private Integer totalStudents = 0;

    /**
     * 备注
     */
    private String remark;

    /**
     * 状态：1正常 0取消
     */
    @Builder.Default
    private Integer status = 1;

    /**
     * 考场列表
     */
    @Builder.Default
    private List<ExamRoom> rooms = new ArrayList<>();

    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;

    /**
     * 获取考试形式名称
     */
    public String getExamFormName() {
        return switch (examForm) {
            case 1 -> "闭卷";
            case 2 -> "开卷";
            case 3 -> "机考";
            case 4 -> "口试";
            case 5 -> "实操";
            default -> "未知";
        };
    }

    /**
     * 添加考场
     */
    public void addRoom(ExamRoom room) {
        if (rooms == null) {
            rooms = new ArrayList<>();
        }
        room.setArrangementId(this.id);
        rooms.add(room);
    }

    /**
     * 移除考场
     */
    public void removeRoom(Long roomId) {
        if (rooms != null) {
            rooms.removeIf(r -> r.getId().equals(roomId));
        }
    }

    /**
     * 计算已安排座位数
     */
    public int getArrangedSeats() {
        return rooms.stream()
                .mapToInt(r -> r.getStudentCount() != null ? r.getStudentCount() : 0)
                .sum();
    }

    /**
     * 检查座位是否足够
     */
    public boolean hasEnoughSeats() {
        return getArrangedSeats() >= totalStudents;
    }

    /**
     * 取消考试安排
     */
    public void cancel() {
        this.status = 0;
    }

    /**
     * 恢复考试安排
     */
    public void restore() {
        this.status = 1;
    }

    /**
     * 是否已取消
     */
    public boolean isCancelled() {
        return status == 0;
    }
}
