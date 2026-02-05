package com.school.management.domain.space.model.entity;

import com.school.management.domain.shared.Entity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 空间占用记录实体
 * 记录谁在什么时间占用了某个空间
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UniversalSpaceOccupant implements Entity<Long> {

    private Long id;

    /**
     * 空间ID
     */
    private Long spaceId;

    /**
     * 占用者类型（如：STUDENT, EMPLOYEE, VISITOR 等，可自定义）
     */
    private String occupantType;

    /**
     * 占用者ID
     */
    private Long occupantId;

    /**
     * 占用者名称（冗余字段，方便显示）
     */
    private String occupantName;

    /**
     * 位置号（床位号/工位号等）
     */
    private String positionNo;

    /**
     * 入住时间
     */
    private LocalDateTime checkInTime;

    /**
     * 退出时间
     */
    private LocalDateTime checkOutTime;

    /**
     * 状态：0-已退出 1-在住
     */
    @Builder.Default
    private Integer status = 1;

    /**
     * 备注
     */
    private String remark;

    // ==================== 业务方法 ====================

    /**
     * 是否在住
     */
    public boolean isActive() {
        return status != null && status == 1;
    }

    /**
     * 退出
     */
    public void checkOut() {
        this.checkOutTime = LocalDateTime.now();
        this.status = 0;
    }

    /**
     * 退出并记录备注
     */
    public void checkOut(String remark) {
        checkOut();
        this.remark = remark;
    }

    /**
     * 获取在住时长（小时）
     */
    public long getStayDurationHours() {
        LocalDateTime endTime = checkOutTime != null ? checkOutTime : LocalDateTime.now();
        return java.time.Duration.between(checkInTime, endTime).toHours();
    }

    /**
     * 获取在住时长（天）
     */
    public long getStayDurationDays() {
        LocalDateTime endTime = checkOutTime != null ? checkOutTime : LocalDateTime.now();
        return java.time.Duration.between(checkInTime, endTime).toDays();
    }

    // ==================== 工厂方法 ====================

    /**
     * 创建占用记录
     */
    public static UniversalSpaceOccupant create(Long spaceId, String occupantType, Long occupantId,
                                                 String occupantName, String positionNo) {
        return UniversalSpaceOccupant.builder()
                .spaceId(spaceId)
                .occupantType(occupantType)
                .occupantId(occupantId)
                .occupantName(occupantName)
                .positionNo(positionNo)
                .checkInTime(LocalDateTime.now())
                .status(1)
                .build();
    }
}
