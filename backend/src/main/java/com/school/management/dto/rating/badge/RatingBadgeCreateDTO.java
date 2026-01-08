package com.school.management.dto.rating.badge;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 创建荣誉徽章DTO
 *
 * @author Claude Code
 * @since 2025-12-22
 */
@Data
public class RatingBadgeCreateDTO {

    @NotBlank(message = "徽章名称不能为空")
    private String badgeName;

    private String badgeIcon;

    @NotBlank(message = "徽章等级不能为空")
    private String badgeLevel;      // GOLD/SILVER/BRONZE

    @NotNull(message = "评级规则ID不能为空")
    private Long ruleId;

    @NotNull(message = "授予条件不能为空")
    private RatingBadgeVO.GrantConditionVO grantCondition;

    private String description;
    private Boolean autoGrant;
}
