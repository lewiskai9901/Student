package com.school.management.domain.teaching.model.valueobject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 排课冲突值对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleConflict {

    private Long id;

    /**
     * 学期ID
     */
    private Long semesterId;

    /**
     * 排课条目ID
     */
    private Long entryId;

    /**
     * 冲突的另一条目ID
     */
    private Long conflictEntryId;

    /**
     * 冲突类型：1教师冲突 2教室冲突 3班级冲突
     */
    private Integer conflictType;

    /**
     * 冲突详情描述
     */
    private String conflictDetail;

    /**
     * 是否已解决
     */
    @Builder.Default
    private Boolean resolved = false;

    /**
     * 解决人
     */
    private Long resolvedBy;

    /**
     * 解决时间
     */
    private LocalDateTime resolvedAt;

    /**
     * 解决说明
     */
    private String resolutionNote;

    private LocalDateTime createdAt;

    /**
     * 创建教师冲突
     */
    public static ScheduleConflict teacherConflict(Long entryId, Long conflictEntryId, Long teacherId) {
        return ScheduleConflict.builder()
                .entryId(entryId)
                .conflictEntryId(conflictEntryId)
                .conflictType(1)
                .conflictDetail("教师ID=" + teacherId + " 时间冲突")
                .build();
    }

    /**
     * 创建教室冲突
     */
    public static ScheduleConflict classroomConflict(Long entryId, Long conflictEntryId, Long classroomId) {
        return ScheduleConflict.builder()
                .entryId(entryId)
                .conflictEntryId(conflictEntryId)
                .conflictType(2)
                .conflictDetail("教室ID=" + classroomId + " 时间冲突")
                .build();
    }

    /**
     * 创建班级冲突
     */
    public static ScheduleConflict classConflict(Long entryId, Long conflictEntryId, Long classId) {
        return ScheduleConflict.builder()
                .entryId(entryId)
                .conflictEntryId(conflictEntryId)
                .conflictType(3)
                .conflictDetail("班级ID=" + classId + " 时间冲突")
                .build();
    }

    /**
     * 获取冲突类型名称
     */
    public String getConflictTypeName() {
        return switch (conflictType) {
            case 1 -> "教师冲突";
            case 2 -> "教室冲突";
            case 3 -> "班级冲突";
            default -> "未知冲突";
        };
    }

    /**
     * 标记为已解决
     */
    public void resolve(Long userId, String note) {
        this.resolved = true;
        this.resolvedBy = userId;
        this.resolvedAt = LocalDateTime.now();
        this.resolutionNote = note;
    }

    /**
     * 是否已解决
     */
    public boolean isResolved() {
        return resolved != null && resolved;
    }

    /**
     * 获取资源ID（教师ID/教室ID/班级ID）
     */
    public Long getResourceId() {
        // 从conflictDetail中解析资源ID
        if (conflictDetail != null && conflictDetail.contains("ID=")) {
            try {
                String idStr = conflictDetail.substring(conflictDetail.indexOf("ID=") + 3);
                idStr = idStr.split(" ")[0];
                return Long.parseLong(idStr);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    /**
     * 获取资源名称
     */
    public String getResourceName() {
        return getConflictTypeName() + "资源";
    }

    /**
     * 获取星期
     */
    public Integer getWeekday() {
        // 冲突检测时应该记录weekday，这里返回null表示未记录
        return null;
    }

    /**
     * 获取节次
     */
    public Integer getSlot() {
        return null;
    }

    /**
     * 获取起始周
     */
    public Integer getStartWeek() {
        return null;
    }

    /**
     * 获取结束周
     */
    public Integer getEndWeek() {
        return null;
    }

    /**
     * 获取冲突的条目ID
     */
    public Long getConflictingEntryId() {
        return conflictEntryId;
    }

    /**
     * 获取描述
     */
    public String getDescription() {
        return conflictDetail != null ? conflictDetail : getConflictTypeName();
    }
}
