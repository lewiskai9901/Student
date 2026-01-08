package com.school.management.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 宿舍查询请求DTO
 *
 * @author system
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DormitoryQueryRequest extends BaseQueryRequest {

    /**
     * 房间号
     */
    private String dormitoryNo;

    /**
     * 楼宇ID
     */
    private Long buildingId;

    /**
     * 楼栋名称 (用于模糊搜索)
     */
    private String buildingName;

    /**
     * 楼层
     */
    private Integer floorNumber;

    /**
     * 房间类型: 1四人间 2六人间 3八人间
     */
    private Integer roomType;

    /**
     * 性别类型: 1男 2女 3混合
     */
    private Integer genderType;

    /**
     * 宿管员ID
     */
    private Long supervisorId;

    /**
     * 状态: 1正常 2维修 3停用
     */
    private Integer status;

    // ========== 数据权限过滤字段 ==========

    /**
     * 楼栋ID列表(数据权限过滤用)
     */
    private List<Long> buildingIds;

    /**
     * 班级ID列表(数据权限过滤用 - 通过学生分配过滤宿舍)
     */
    private List<Long> classIds;

    /**
     * 排序字段
     */
    private String sortBy = "buildingName";

    /**
     * 排序方向: asc/desc
     */
    private String sortOrder = "asc";

    /**
     * 自定义setter防止SQL注入
     */
    public void setSortOrder(String sortOrder) {
        if (sortOrder == null || sortOrder.trim().isEmpty()) {
            this.sortOrder = "asc";
        } else {
            String normalized = sortOrder.trim().toLowerCase();
            if ("asc".equals(normalized) || "desc".equals(normalized)) {
                this.sortOrder = normalized.toUpperCase();
            } else {
                this.sortOrder = "ASC"; // 默认值,防止注入
            }
        }
    }

    /**
     * 自定义setter防止SQL注入 - 白名单验证sortBy字段
     */
    public void setSortBy(String sortBy) {
        if (sortBy == null || sortBy.trim().isEmpty()) {
            this.sortBy = "buildingName";
        } else {
            // 白名单验证
            switch (sortBy) {
                case "buildingName":
                case "dormitoryNo":
                case "createdAt":
                    this.sortBy = sortBy;
                    break;
                default:
                    this.sortBy = "buildingName"; // 默认值
            }
        }
    }
}