package com.school.management.dto.rating.badge;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 徽章授予结果VO
 *
 * @author Claude Code
 * @since 2025-12-22
 */
@Data
public class BadgeGrantResultVO {

    private Long badgeId;
    private String badgeName;
    private String badgeLevel;

    private Long classId;
    private String className;

    private LocalDateTime grantedAt;
    private Boolean success;
    private String message;
}
