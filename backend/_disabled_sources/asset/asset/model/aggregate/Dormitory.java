package com.school.management.domain.asset.model.aggregate;

import com.school.management.domain.asset.event.DormitoryCreatedEvent;
import com.school.management.domain.asset.event.StudentCheckedInEvent;
import com.school.management.domain.asset.event.StudentCheckedOutEvent;
import com.school.management.domain.asset.model.valueobject.DormitoryStatus;
import com.school.management.domain.asset.model.valueobject.GenderType;
import com.school.management.domain.asset.model.valueobject.RoomUsageType;
import com.school.management.domain.shared.AggregateRoot;
import com.school.management.exception.BusinessException;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 宿舍聚合根
 */
@Getter
@Setter
public class Dormitory extends AggregateRoot<Long> {

    private Long buildingId;
    private Long orgUnitId;
    private String dormitoryNo;
    private Integer floorNumber;
    private RoomUsageType roomUsageType;
    private Integer bedCapacity;
    private Integer bedCount;
    private Integer occupiedBeds;
    private GenderType genderType;
    private String facilities;
    private String notes;
    private DormitoryStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    protected Dormitory() {}

    /**
     * 创建宿舍
     */
    public static Dormitory create(Long buildingId, String dormitoryNo, Integer floorNumber,
                                    RoomUsageType roomUsageType, Integer bedCapacity,
                                    GenderType genderType) {
        Dormitory dormitory = new Dormitory();
        dormitory.buildingId = buildingId;
        dormitory.dormitoryNo = dormitoryNo;
        dormitory.floorNumber = floorNumber;
        dormitory.roomUsageType = roomUsageType;
        dormitory.bedCapacity = bedCapacity;
        dormitory.bedCount = roomUsageType.canHaveBeds() ? bedCapacity : 0;
        dormitory.occupiedBeds = 0;
        dormitory.genderType = genderType;
        dormitory.status = DormitoryStatus.ACTIVE;
        dormitory.createdAt = LocalDateTime.now();
        dormitory.updatedAt = LocalDateTime.now();

        dormitory.registerEvent(new DormitoryCreatedEvent(
                null,
                dormitoryNo,
                buildingId,
                bedCapacity
        ));

        return dormitory;
    }

    /**
     * 从持久化重建
     */
    public static Dormitory reconstruct(Long id, Long buildingId, Long orgUnitId,
                                         String dormitoryNo, Integer floorNumber,
                                         RoomUsageType roomUsageType, Integer bedCapacity,
                                         Integer bedCount, Integer occupiedBeds,
                                         GenderType genderType, String facilities,
                                         String notes, DormitoryStatus status,
                                         LocalDateTime createdAt, LocalDateTime updatedAt) {
        Dormitory dormitory = new Dormitory();
        dormitory.setId(id);
        dormitory.buildingId = buildingId;
        dormitory.orgUnitId = orgUnitId;
        dormitory.dormitoryNo = dormitoryNo;
        dormitory.floorNumber = floorNumber;
        dormitory.roomUsageType = roomUsageType;
        dormitory.bedCapacity = bedCapacity;
        dormitory.bedCount = bedCount;
        dormitory.occupiedBeds = occupiedBeds;
        dormitory.genderType = genderType;
        dormitory.facilities = facilities;
        dormitory.notes = notes;
        dormitory.status = status;
        dormitory.createdAt = createdAt;
        dormitory.updatedAt = updatedAt;
        return dormitory;
    }

    /**
     * 学生入住
     */
    public void checkIn(Long studentId, Integer bedNumber, String studentName) {
        validateCanCheckIn();

        if (bedNumber != null && (bedNumber < 1 || bedNumber > bedCount)) {
            throw new BusinessException("床位号无效，应在1到" + bedCount + "之间");
        }

        this.occupiedBeds = (this.occupiedBeds == null ? 0 : this.occupiedBeds) + 1;
        this.updatedAt = LocalDateTime.now();

        registerEvent(new StudentCheckedInEvent(
                String.valueOf(getId()),
                studentId,
                studentName,
                dormitoryNo,
                bedNumber
        ));
    }

    /**
     * 学生退宿
     */
    public void checkOut(Long studentId, String studentName, Integer bedNumber) {
        if (this.occupiedBeds == null || this.occupiedBeds <= 0) {
            throw new BusinessException("宿舍没有入住学生");
        }

        this.occupiedBeds = this.occupiedBeds - 1;
        this.updatedAt = LocalDateTime.now();

        registerEvent(new StudentCheckedOutEvent(
                String.valueOf(getId()),
                studentId,
                studentName,
                dormitoryNo,
                bedNumber
        ));
    }

    /**
     * 更新宿舍信息
     */
    public void updateInfo(Integer floorNumber, RoomUsageType roomUsageType,
                           Integer bedCapacity, GenderType genderType,
                           String facilities, String notes) {
        // 如果减少床位，需要检查是否有足够空床位
        if (bedCapacity != null && bedCapacity < this.bedCount) {
            int currentOccupied = this.occupiedBeds != null ? this.occupiedBeds : 0;
            if (bedCapacity < currentOccupied) {
                throw new BusinessException("床位容量不能小于当前入住人数: " + currentOccupied);
            }
        }

        this.floorNumber = floorNumber;
        this.roomUsageType = roomUsageType;
        this.bedCapacity = bedCapacity;
        this.bedCount = roomUsageType.canHaveBeds() ? bedCapacity : 0;
        this.genderType = genderType;
        this.facilities = facilities;
        this.notes = notes;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 分配给组织单元
     */
    public void assignToOrgUnit(Long orgUnitId) {
        this.orgUnitId = orgUnitId;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 取消组织单元分配
     */
    public void removeFromOrgUnit() {
        this.orgUnitId = null;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 启用宿舍
     */
    public void enable() {
        this.status = DormitoryStatus.ACTIVE;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 停用宿舍
     */
    public void disable() {
        if (this.occupiedBeds != null && this.occupiedBeds > 0) {
            throw new BusinessException("宿舍还有" + this.occupiedBeds + "人入住，无法停用");
        }
        this.status = DormitoryStatus.INACTIVE;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 获取可用床位数
     */
    public int getAvailableBeds() {
        int total = bedCount != null ? bedCount : 0;
        int occupied = occupiedBeds != null ? occupiedBeds : 0;
        return total - occupied;
    }

    /**
     * 是否已满
     */
    public boolean isFull() {
        return getAvailableBeds() <= 0;
    }

    /**
     * 是否为学生宿舍
     */
    public boolean isStudentDormitory() {
        return roomUsageType != null && roomUsageType.isStudentDormitory();
    }

    /**
     * 是否可以入住
     */
    public boolean canCheckIn() {
        return status != null && status.isActive()
                && isStudentDormitory()
                && !isFull();
    }

    private void validateCanCheckIn() {
        if (status == null || !status.isActive()) {
            throw new BusinessException("宿舍已停用");
        }
        if (!isStudentDormitory()) {
            throw new BusinessException("该房间不是学生宿舍");
        }
        if (isFull()) {
            throw new BusinessException("宿舍已满");
        }
    }
}
