package com.school.management.domain.teaching.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 考场实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamRoom {

    private Long id;

    /**
     * 考试安排ID
     */
    private Long arrangementId;

    /**
     * 教室ID
     */
    private Long classroomId;

    /**
     * 考场编号
     */
    private String roomCode;

    /**
     * 座位数
     */
    private Integer seatCount;

    /**
     * 安排考生数
     */
    @Builder.Default
    private Integer studentCount = 0;

    /**
     * 座位安排（学生ID与座位号映射）
     */
    private Map<Long, String> seatLayout;

    /**
     * 备注
     */
    private String remark;

    /**
     * 监考教师列表
     */
    @Builder.Default
    private List<ExamInvigilator> invigilators = new ArrayList<>();

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 添加监考教师
     */
    public void addInvigilator(ExamInvigilator invigilator) {
        if (invigilators == null) {
            invigilators = new ArrayList<>();
        }
        invigilator.setRoomId(this.id);
        invigilators.add(invigilator);
    }

    /**
     * 移除监考教师
     */
    public void removeInvigilator(Long teacherId) {
        if (invigilators != null) {
            invigilators.removeIf(i -> i.getTeacherId().equals(teacherId));
        }
    }

    /**
     * 安排学生座位
     */
    public void assignSeat(Long studentId, String seatNumber) {
        if (seatLayout == null) {
            seatLayout = new java.util.HashMap<>();
        }
        seatLayout.put(studentId, seatNumber);
        this.studentCount = seatLayout.size();
    }

    /**
     * 取消学生座位
     */
    public void unassignSeat(Long studentId) {
        if (seatLayout != null) {
            seatLayout.remove(studentId);
            this.studentCount = seatLayout.size();
        }
    }

    /**
     * 获取剩余座位数
     */
    public int getRemainingSeats() {
        return seatCount - (studentCount != null ? studentCount : 0);
    }

    /**
     * 是否已满
     */
    public boolean isFull() {
        return getRemainingSeats() <= 0;
    }

    /**
     * 获取主监考
     */
    public ExamInvigilator getMainInvigilator() {
        return invigilators.stream()
                .filter(i -> i.getInvigilatorRole() == 1)
                .findFirst()
                .orElse(null);
    }

    /**
     * 检查监考配置是否完整
     */
    public boolean hasCompleteInvigilators() {
        return invigilators != null && !invigilators.isEmpty()
                && invigilators.stream().anyMatch(i -> i.getInvigilatorRole() == 1);
    }
}
