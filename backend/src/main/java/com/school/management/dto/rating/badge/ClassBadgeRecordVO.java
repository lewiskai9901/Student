package com.school.management.dto.rating.badge;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 班级徽章获得记录VO
 *
 * @author Claude Code
 * @since 2025-12-22
 */
@Data
public class ClassBadgeRecordVO {

    private Long id;
    private Long badgeId;
    private String badgeName;
    private String badgeIcon;
    private String badgeLevel;

    private Long classId;
    private String className;
    private String departmentName;
    private String gradeName;

    private LocalDateTime grantedAt;
    private String grantedBy;           // 授予人姓名

    private LocalDate periodStart;
    private LocalDate periodEnd;
    private String periodLabel;         // 如"2025年12月"

    // 成就数据
    private Map<String, Object> achievementData;

    // 证书相关
    private String certificateUrl;
    private Boolean certificateGenerated;

    // 撤销信息
    private Boolean revoked;
    private LocalDateTime revokedAt;
    private String revokedReason;
}
