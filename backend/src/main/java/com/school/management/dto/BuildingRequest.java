package com.school.management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.List;

/**
 * 楼宇请求DTO
 *
 * @author system
 * @since 1.0.0
 */
@Data
public class BuildingRequest {

    /**
     * 楼宇ID(更新时使用)
     */
    private Long id;

    /**
     * 楼号(纯数字)
     */
    @NotBlank(message = "楼号不能为空")
    @Pattern(regexp = "^[0-9]+$", message = "楼号必须为纯数字")
    private String buildingNo;

    /**
     * 楼宇名称
     */
    @NotBlank(message = "楼宇名称不能为空")
    private String buildingName;

    /**
     * 楼宇类型: 1-教学楼, 2-宿舍楼, 3-办公楼
     */
    @NotNull(message = "楼宇类型不能为空")
    private Integer buildingType;

    /**
     * 总楼层数
     */
    @NotNull(message = "总楼层数不能为空")
    private Integer totalFloors;

    /**
     * 地理位置
     */
    private String location;

    /**
     * 建造年份
     */
    private Integer constructionYear;

    /**
     * 楼管理员ID
     */
    private Long managerId;

    /**
     * 楼宇描述
     */
    private String description;

    /**
     * 状态: 0-停用, 1-启用
     */
    @NotNull(message = "状态不能为空")
    private Integer status;

    /**
     * 关联部门ID列表
     */
    private List<Long> departmentIds;
}
