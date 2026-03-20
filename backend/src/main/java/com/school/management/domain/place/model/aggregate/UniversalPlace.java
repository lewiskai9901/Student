package com.school.management.domain.place.model.aggregate;

import com.school.management.domain.shared.AggregateRoot;
import com.school.management.domain.place.model.valueobject.PlaceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.EqualsAndHashCode;

import java.util.HashMap;
import java.util.Map;

/**
 * 通用空间聚合根
 * 支持任意类型的空间实例，类型由PlaceType配置决定
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class UniversalPlace extends AggregateRoot<Long> {

    private Long id;

    // ==================== 基础信息 ====================

    /**
     * 空间编码（唯一）
     */
    private String placeCode;

    /**
     * 空间名称
     */
    private String placeName;

    /**
     * 空间类型编码（关联PlaceType）
     */
    private String typeCode;

    /**
     * 描述
     */
    private String description;

    // ==================== 层级关系 ====================

    /**
     * 父级ID
     */
    private Long parentId;

    /**
     * 物化路径（如 /1/2/3/）
     */
    private String path;

    /**
     * 层级深度（0表示根节点）
     */
    @Builder.Default
    private Integer level = 0;

    // ==================== 容量 ====================

    /**
     * 容量
     */
    private Integer capacity;

    /**
     * 当前占用数
     */
    @Builder.Default
    private Integer currentOccupancy = 0;

    // ==================== 归属 ====================

    /**
     * 所属组织单元ID
     */
    private Long orgUnitId;

    /**
     * 负责人ID
     */
    private Long responsibleUserId;

    // ==================== 性别限制 ====================

    /**
     * 性别限制: MALE / FEMALE / MIXED / null（null表示继承父节点）
     */
    private String gender;

    // ==================== 状态 ====================

    /**
     * 状态
     */
    @Builder.Default
    private PlaceStatus status = PlaceStatus.NORMAL;

    // ==================== 扩展属性 ====================

    /**
     * 扩展属性值
     */
    @Builder.Default
    private Map<String, Object> attributes = new HashMap<>();

    // ==================== 业务方法 ====================

    /**
     * 是否有可用容量
     */
    public boolean hasAvailableCapacity() {
        if (capacity == null) {
            return true; // 无容量限制
        }
        return currentOccupancy < capacity;
    }

    /**
     * 获取可用容量
     */
    public int getAvailableCapacity() {
        if (capacity == null) {
            return Integer.MAX_VALUE;
        }
        return Math.max(0, capacity - currentOccupancy);
    }

    /**
     * 获取占用率
     */
    public double getOccupancyRate() {
        if (capacity == null || capacity == 0) {
            return 0.0;
        }
        return (double) currentOccupancy / capacity;
    }

    /**
     * 更新占用人数（领域方法，发布事件）
     *
     * @param newOccupancy 新占用人数
     * @param operationType 操作类型（CHECK_IN/CHECK_OUT/MANUAL）
     */
    public void updateOccupancy(int newOccupancy, String operationType) {
        Integer oldOccupancy = this.currentOccupancy != null ? this.currentOccupancy : 0;

        // 值未变化，无需操作
        if (oldOccupancy.equals(newOccupancy)) {
            return;
        }

        // 业务规则校验：不能为负数
        if (newOccupancy < 0) {
            throw new IllegalArgumentException("占用人数不能为负数");
        }

        // 业务规则校验：不能超过容量（除非无容量限制）
        if (capacity != null && newOccupancy > capacity) {
            throw new IllegalStateException(
                    String.format("占用人数(%d)不能超过容量(%d)", newOccupancy, capacity)
            );
        }

        // 更新值
        this.currentOccupancy = newOccupancy;

        // 发布领域事件
        registerEvent(new com.school.management.domain.place.event.PlaceCapacityUpdatedEvent(
                this.id, this.placeName, this.typeCode,
                oldOccupancy, newOccupancy, this.capacity,
                operationType
        ));
    }

    /**
     * 入住（占用数+1）
     */
    public void checkIn() {
        int currentCount = (currentOccupancy != null) ? currentOccupancy : 0;
        updateOccupancy(currentCount + 1, "CHECK_IN");
    }

    /**
     * 退住（占用数-1）
     */
    public void checkOut() {
        int currentCount = (currentOccupancy != null) ? currentOccupancy : 0;
        if (currentCount <= 0) {
            throw new IllegalStateException("当前无占用者，无法退住");
        }
        updateOccupancy(currentCount - 1, "CHECK_OUT");
    }

    /**
     * 手动调整占用数
     */
    public void adjustOccupancy(int newCount) {
        updateOccupancy(newCount, "MANUAL");
    }

    /**
     * 分配给组织单元（领域方法，发布事件）
     *
     * @param orgUnitId 组织单元ID（传入null表示恢复继承父级）
     * @param reason 变更原因
     */
    public void assignOrganization(Long orgUnitId, String reason) {
        Long oldOrgUnitId = this.orgUnitId;

        // 值未变化，无需操作
        if (java.util.Objects.equals(oldOrgUnitId, orgUnitId)) {
            return;
        }

        // 更新值
        this.orgUnitId = orgUnitId;

        // 发布领域事件
        registerEvent(new com.school.management.domain.place.event.PlaceOrgAssignedEvent(
                this.id, this.placeName, oldOrgUnitId, orgUnitId, reason
        ));
    }

    /**
     * 取消组织单元分配（恢复继承）
     *
     * @param reason 变更原因
     */
    public void clearOrganizationOverride(String reason) {
        assignOrganization(null, reason);
    }

    /**
     * 分配负责人（领域方法，发布事件）
     *
     * @param userId 负责人用户ID
     * @param reason 变更原因
     */
    public void assignResponsible(Long userId, String reason) {
        Long oldResponsibleUserId = this.responsibleUserId;

        // 值未变化，无需操作
        if (java.util.Objects.equals(oldResponsibleUserId, userId)) {
            return;
        }

        // 更新值
        this.responsibleUserId = userId;

        // 发布领域事件
        registerEvent(new com.school.management.domain.place.event.PlaceResponsibleAssignedEvent(
                this.id, this.placeName, oldResponsibleUserId, userId, reason
        ));
    }

    /**
     * 变更状态（领域方法，发布事件）
     *
     * @param newStatus 新状态
     * @param reason 变更原因
     */
    public void changeStatus(PlaceStatus newStatus, String reason) {
        PlaceStatus oldStatus = this.status;

        // 状态未变化，无需操作
        if (oldStatus == newStatus) {
            return;
        }

        // 业务规则校验
        if (newStatus == PlaceStatus.DISABLED) {
            if (currentOccupancy != null && currentOccupancy > 0) {
                throw new IllegalStateException("场所有占用者，不能禁用");
            }
        }

        // 更新状态
        this.status = newStatus;

        // 发布领域事件
        registerEvent(new com.school.management.domain.place.event.PlaceStatusChangedEvent(
                this.id, this.placeName, oldStatus, newStatus, reason
        ));
    }

    /**
     * 启用（领域方法）
     */
    public void enable(String reason) {
        changeStatus(PlaceStatus.NORMAL, reason);
    }

    /**
     * 禁用（领域方法）
     */
    public void disable(String reason) {
        changeStatus(PlaceStatus.DISABLED, reason);
    }

    /**
     * 开始维护（领域方法）
     */
    public void startMaintenance(String reason) {
        changeStatus(PlaceStatus.MAINTENANCE, reason);
    }

    /**
     * 完成维护（领域方法）
     */
    public void completeMaintenance(String reason) {
        changeStatus(PlaceStatus.NORMAL, reason);
    }

    /**
     * 是否可以入住/占用
     */
    public boolean canCheckIn() {
        return status == PlaceStatus.NORMAL && hasAvailableCapacity();
    }

    /**
     * 设置扩展属性
     */
    public void setAttribute(String key, Object value) {
        if (attributes == null) {
            attributes = new HashMap<>();
        }
        attributes.put(key, value);
    }

    /**
     * 获取扩展属性
     */
    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key) {
        if (attributes == null) {
            return null;
        }
        return (T) attributes.get(key);
    }

    /**
     * 移除扩展属性
     */
    public void removeAttribute(String key) {
        if (attributes != null) {
            attributes.remove(key);
        }
    }

    /**
     * 验证性别限制与父节点有效性别的兼容性
     */
    public void validateGender(String parentEffectiveGender) {
        if (gender == null) return;
        if (parentEffectiveGender == null || "MIXED".equals(parentEffectiveGender)) return;
        if (!parentEffectiveGender.equals(gender)) {
            throw new IllegalArgumentException("性别限制与父节点冲突：父节点为" + parentEffectiveGender + "，不允许设置为" + gender);
        }
    }

    /**
     * 更新层级信息
     */
    public void updateHierarchy(Long parentId, String parentPath, int parentLevel) {
        this.parentId = parentId;
        if (parentPath == null || parentPath.isEmpty()) {
            this.path = "/" + this.id + "/";
            this.level = 0;
        } else {
            this.path = parentPath + this.id + "/";
            this.level = parentLevel + 1;
        }
    }

    /**
     * 判断是否为指定空间的祖先
     */
    public boolean isAncestorOf(UniversalPlace other) {
        if (other == null || other.getPath() == null || this.path == null) {
            return false;
        }
        return other.getPath().startsWith(this.path) && !other.getPath().equals(this.path);
    }

    /**
     * 判断是否为指定空间的后代
     */
    public boolean isDescendantOf(UniversalPlace other) {
        if (other == null) {
            return false;
        }
        return other.isAncestorOf(this);
    }

    // ==================== 工厂方法 ====================

    /**
     * 创建空间（placeCode 由用户输入）
     */
    public static UniversalPlace create(String placeCode, String placeName, String typeCode, Long parentId) {
        if (placeCode == null || placeCode.isBlank()) {
            throw new IllegalArgumentException("场所编号不能为空");
        }
        return UniversalPlace.builder()
                .placeCode(placeCode.trim())
                .placeName(placeName)
                .typeCode(typeCode)
                .parentId(parentId)
                .status(PlaceStatus.NORMAL)
                .currentOccupancy(0)
                .attributes(new HashMap<>())
                .build();
    }

    /**
     * 创建根空间
     */
    public static UniversalPlace createRoot(String placeCode, String placeName, String typeCode) {
        return create(placeCode, placeName, typeCode, null);
    }
}
