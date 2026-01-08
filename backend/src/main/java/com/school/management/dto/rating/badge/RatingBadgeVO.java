package com.school.management.dto.rating.badge;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 荣誉徽章配置视图对象
 *
 * @author Claude Code
 * @since 2025-12-22
 */
@Data
public class RatingBadgeVO {

    private Long id;
    private String badgeName;
    private String badgeIcon;
    private String badgeLevel;          // GOLD/SILVER/BRONZE

    private Long ruleId;
    private String ruleName;

    // 授予条件
    private GrantConditionVO grantCondition;

    private String description;
    private Boolean enabled;
    private Boolean autoGrant;

    // 当前符合条件的班级数
    private Integer currentQualifiedCount;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 授予条件VO
     */
    @Data
    public static class GrantConditionVO {
        private String conditionType;      // FREQUENCY/CONSECUTIVE/MULTI_RULE
        private Integer frequencyThreshold; // 频次阈值
        private Integer consecutiveThreshold; // 连续阈值
        private String periodType;         // WEEK/MONTH/SEMESTER
        private String levelId;            // 要求的等级ID
        private String levelName;          // 要求的等级名称
        private MultiRuleCondition multiRuleCondition; // 多规则条件
    }

    /**
     * 多规则条件
     */
    @Data
    public static class MultiRuleCondition {
        private String operator;           // AND/OR
        private java.util.List<RuleRequirement> requirements;
    }

    /**
     * 规则要求
     */
    @Data
    public static class RuleRequirement {
        private Long ruleId;
        private String ruleName;
        private Long levelId;
        private String levelName;
        private Integer frequencyThreshold;
    }
}
