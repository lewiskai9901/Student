package com.school.management.domain.place.model.aggregate;

import com.school.management.domain.shared.AggregateRoot;
import com.school.management.domain.place.model.valueobject.PlaceStatus;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.EqualsAndHashCode;

import java.util.HashMap;
import java.util.Map;

/**
 * 通用空间聚合根
 * 支持任意类型的空间实例，类型由PlaceType配置决定
 *
 * <p>N1 (2026-05-20): 删除 shadow {@code id} 字段 — id 由 {@link AggregateRoot}
 * 提供. {@code @Builder} 移到含 {@code id} 参数的构造器上, 经 {@code setId()}
 * 写入继承字段, 保持 {@code UniversalPlace.builder().id(..)} API 不变.
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class UniversalPlace extends AggregateRoot<Long> {

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
    private Integer level = 0;

    // ==================== 容量 ====================

    /**
     * 容量
     */
    private Integer capacity;

    /**
     * 当前占用数
     */
    private Integer currentOccupancy = 0;

    // ==================== 归属 ====================

    /**
     * 有效组织ID — <b>投影列</b> (解析后含继承)。
     * 归属真相在 access_relations 的 {@code belongs_to|place|org_unit} 覆盖点关系
     * (写入走 {@code PlaceOrgResolver}), 本字段由 {@code PlaceOrgProjector} 维护,
     * 业务代码只读, save 路径不回写 (PO 层 FieldStrategy.NEVER 兜底)。
     * 负责人无投影字段, 读时经 {@code responsible_for} 关系 + 场所树继承解析。
     */
    private Long effectiveOrgUnitId;

    // ==================== 性别限制 ====================

    /**
     * 性别限制: MALE / FEMALE / MIXED / null（null表示继承父节点）
     */
    private String gender;

    // ==================== 状态 ====================

    /**
     * 状态
     */
    private PlaceStatus status = PlaceStatus.NORMAL;

    // ==================== 扩展属性 ====================

    /**
     * 扩展属性值
     */
    private Map<String, Object> attributes = new HashMap<>();

    // ==================== 构造 ====================

    /**
     * 全参构造器 — {@code @Builder} 挂在此处, 使 builder 含 {@code id}
     * (走 {@link AggregateRoot#setId}, 非 shadow 字段).
     */
    @Builder
    public UniversalPlace(Long id, String placeCode, String placeName, String typeCode,
                          String description, Long parentId, String path, Integer level,
                          Integer capacity, Integer currentOccupancy, Long effectiveOrgUnitId,
                          String gender, PlaceStatus status,
                          Map<String, Object> attributes) {
        setId(id);
        this.placeCode = placeCode;
        this.placeName = placeName;
        this.typeCode = typeCode;
        this.description = description;
        this.parentId = parentId;
        this.path = path;
        this.level = level != null ? level : 0;
        this.capacity = capacity;
        this.currentOccupancy = currentOccupancy != null ? currentOccupancy : 0;
        this.effectiveOrgUnitId = effectiveOrgUnitId;
        this.gender = gender;
        this.status = status != null ? status : PlaceStatus.NORMAL;
        this.attributes = attributes != null ? attributes : new HashMap<>();
    }

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

    // 归属/负责人分配已迁出聚合: 真相在 access_relations 关系 (PlaceOrgResolver 写入),
    // PlaceOrgAssignedEvent/PlaceResponsibleAssignedEvent 由 ApplicationService 在转译关系操作时发布。

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

        // 业务规则校验：有占用者时不允许禁用或维护
        if (newStatus == PlaceStatus.DISABLED || newStatus == PlaceStatus.MAINTENANCE) {
            if (currentOccupancy != null && currentOccupancy > 0) {
                String action = newStatus == PlaceStatus.DISABLED ? "禁用" : "设为维护中";
                throw new IllegalStateException("场所有占用者，不能" + action);
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
