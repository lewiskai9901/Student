package com.school.management.domain.place.model.entity;

import com.school.management.domain.place.model.valueobject.OccupantType;
import com.school.management.exception.BusinessException;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 场所占用实体
 * 用于记录学生入住宿舍、员工使用工位等
 */
@Getter
@NoArgsConstructor
public class PlaceOccupant {

    private Long id;
    private Long placeId;
    private OccupantType occupantType;
    private Long occupantId;
    private Integer positionNo;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private Integer status;  // 0=已退出, 1=在住
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 关联信息（非持久化）
    private String occupantName;
    private String occupantNo;

    /**
     * 创建占用记录
     */
    public static PlaceOccupant create(Long placeId, OccupantType occupantType,
                                       Long occupantId, Integer positionNo) {
        PlaceOccupant occupant = new PlaceOccupant();
        occupant.placeId = placeId;
        occupant.occupantType = occupantType;
        occupant.occupantId = occupantId;
        occupant.positionNo = positionNo;
        occupant.checkInDate = LocalDate.now();
        occupant.status = 1;  // 在住
        occupant.createdAt = LocalDateTime.now();
        return occupant;
    }

    /**
     * 从持久化数据重建
     */
    public static PlaceOccupant reconstitute(Long id, Long placeId, OccupantType occupantType,
                                             Long occupantId, Integer positionNo,
                                             LocalDate checkInDate, LocalDate checkOutDate,
                                             Integer status, String remark,
                                             LocalDateTime createdAt, LocalDateTime updatedAt) {
        PlaceOccupant occupant = new PlaceOccupant();
        occupant.id = id;
        occupant.placeId = placeId;
        occupant.occupantType = occupantType;
        occupant.occupantId = occupantId;
        occupant.positionNo = positionNo;
        occupant.checkInDate = checkInDate;
        occupant.checkOutDate = checkOutDate;
        occupant.status = status;
        occupant.remark = remark;
        occupant.createdAt = createdAt;
        occupant.updatedAt = updatedAt;
        return occupant;
    }

    /**
     * 设置ID
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 退出
     */
    public void checkOut() {
        if (this.status == 0) {
            throw new BusinessException("该记录已退出");
        }
        this.status = 0;
        this.checkOutDate = LocalDate.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 更换位置
     */
    public void changePosition(Integer newPositionNo) {
        this.positionNo = newPositionNo;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 设置备注
     */
    public void setRemark(String remark) {
        this.remark = remark;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 设置关联信息（用于查询展示）
     */
    public void setOccupantInfo(String name, String no) {
        this.occupantName = name;
        this.occupantNo = no;
    }

    /**
     * 是否在住
     */
    public boolean isActive() {
        return this.status != null && this.status == 1;
    }

    /**
     * 是否是学生
     */
    public boolean isStudent() {
        return this.occupantType == OccupantType.STUDENT;
    }
}
