package com.school.management.domain.space.model.aggregate;

import com.school.management.domain.shared.AggregateRoot;
import com.school.management.domain.space.model.valueobject.SpaceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.EqualsAndHashCode;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 通用空间聚合根
 * 支持任意类型的空间实例，类型由SpaceType配置决定
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class UniversalSpace extends AggregateRoot<Long> {

    private Long id;

    // ==================== 基础信息 ====================

    /**
     * 空间编码（唯一）
     */
    private String spaceCode;

    /**
     * 空间名称
     */
    private String spaceName;

    /**
     * 空间类型编码（关联SpaceType）
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

    // ==================== 状态 ====================

    /**
     * 状态
     */
    @Builder.Default
    private SpaceStatus status = SpaceStatus.NORMAL;

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
     * 增加占用数
     */
    public void incrementOccupancy() {
        if (currentOccupancy == null) {
            currentOccupancy = 0;
        }
        currentOccupancy++;
    }

    /**
     * 减少占用数
     */
    public void decrementOccupancy() {
        if (currentOccupancy != null && currentOccupancy > 0) {
            currentOccupancy--;
        }
    }

    /**
     * 设置占用数
     */
    public void setOccupancyCount(int count) {
        this.currentOccupancy = Math.max(0, count);
    }

    /**
     * 分配给组织单元
     */
    public void assignToOrgUnit(Long orgUnitId) {
        this.orgUnitId = orgUnitId;
    }

    /**
     * 取消组织单元分配
     */
    public void unassignFromOrgUnit() {
        this.orgUnitId = null;
    }

    /**
     * 设置负责人
     */
    public void setResponsible(Long userId) {
        this.responsibleUserId = userId;
    }

    /**
     * 启用
     */
    public void enable() {
        this.status = SpaceStatus.NORMAL;
    }

    /**
     * 禁用
     */
    public void disable() {
        if (currentOccupancy != null && currentOccupancy > 0) {
            throw new IllegalStateException("空间有占用者，不能禁用");
        }
        this.status = SpaceStatus.DISABLED;
    }

    /**
     * 开始维护
     */
    public void startMaintenance() {
        this.status = SpaceStatus.MAINTENANCE;
    }

    /**
     * 完成维护
     */
    public void completeMaintenance() {
        this.status = SpaceStatus.NORMAL;
    }

    /**
     * 是否可以入住/占用
     */
    public boolean canCheckIn() {
        return status == SpaceStatus.NORMAL && hasAvailableCapacity();
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
    public boolean isAncestorOf(UniversalSpace other) {
        if (other == null || other.getPath() == null || this.path == null) {
            return false;
        }
        return other.getPath().startsWith(this.path) && !other.getPath().equals(this.path);
    }

    /**
     * 判断是否为指定空间的后代
     */
    public boolean isDescendantOf(UniversalSpace other) {
        if (other == null) {
            return false;
        }
        return other.isAncestorOf(this);
    }

    // ==================== 工厂方法 ====================

    /**
     * 创建空间
     */
    public static UniversalSpace create(String spaceName, String typeCode, Long parentId) {
        String spaceCode = generateSpaceCode();
        return UniversalSpace.builder()
                .spaceCode(spaceCode)
                .spaceName(spaceName)
                .typeCode(typeCode)
                .parentId(parentId)
                .status(SpaceStatus.NORMAL)
                .currentOccupancy(0)
                .attributes(new HashMap<>())
                .build();
    }

    /**
     * 创建根空间
     */
    public static UniversalSpace createRoot(String spaceName, String typeCode) {
        return create(spaceName, typeCode, null);
    }

    /**
     * 生成空间编码
     */
    private static String generateSpaceCode() {
        return "SP_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
