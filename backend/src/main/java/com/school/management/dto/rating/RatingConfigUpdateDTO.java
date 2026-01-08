package com.school.management.dto.rating;

import lombok.Data;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

/**
 * 评级配置更新 DTO
 *
 * @author System
 * @since 4.4.0
 */
@Data
public class RatingConfigUpdateDTO {

    /**
     * 配置ID
     */
    @NotNull(message = "配置ID不能为空")
    private Long id;

    /**
     * 评级名称（如：优秀班级、卫生班级）
     */
    @NotBlank(message = "评级名称不能为空")
    @Size(max = 50, message = "评级名称长度不能超过50个字符")
    private String ratingName;

    /**
     * 图标
     */
    @Size(max = 100, message = "图标长度不能超过100个字符")
    private String icon;

    /**
     * 颜色（十六进制）
     */
    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "颜色格式不正确，应为十六进制格式如#FF5733")
    private String color;

    /**
     * 显示优先级（数字越小越靠前）
     */
    @Min(value = 0, message = "显示优先级不能小于0")
    @Max(value = 9999, message = "显示优先级不能大于9999")
    private Integer priority;

    /**
     * 划分方式：TOP_N/TOP_PERCENT/BOTTOM_N/BOTTOM_PERCENT/OTHER
     */
    @NotBlank(message = "划分方式不能为空")
    @Pattern(regexp = "^(TOP_N|TOP_PERCENT|BOTTOM_N|BOTTOM_PERCENT|OTHER)$", message = "划分方式必须是TOP_N、TOP_PERCENT、BOTTOM_N、BOTTOM_PERCENT或OTHER")
    private String divisionMethod;

    /**
     * 划分值（3名或10%）
     * 注意：当divisionMethod为OTHER时，此值可以为null
     */
    @DecimalMin(value = "0.01", message = "划分值必须大于0")
    private BigDecimal divisionValue;

    /**
     * 是否需要审核：0否 1是
     */
    private Integer requireApproval;

    /**
     * 审核通过后自动发布：0否 1是
     */
    private Integer autoPublish;

    /**
     * 是否启用：0否 1是
     */
    private Integer enabled;

    /**
     * 排序序号
     */
    private Integer sortOrder;

    /**
     * 规则说明
     */
    @Size(max = 500, message = "规则说明长度不能超过500个字符")
    private String description;

    /**
     * 排名数据源列表
     */
    @NotEmpty(message = "排名数据源不能为空")
    @Size(max = 5, message = "最多支持5个数据源组合")
    private List<RatingRankingSourceDTO> rankingSources;

    /**
     * 变更说明
     */
    @Size(max = 200, message = "变更说明长度不能超过200个字符")
    private String changeDescription;
}
