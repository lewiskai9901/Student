package com.school.management.dto.rating.badge;

import lombok.Data;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

/**
 * 徽章授予请求DTO
 *
 * @author Claude Code
 * @since 2025-12-22
 */
@Data
public class BadgeGrantRequest {

    @NotNull(message = "徽章ID不能为空")
    private Long badgeId;

    @NotEmpty(message = "班级ID列表不能为空")
    private List<Long> classIds;

    private LocalDate periodStart;
    private LocalDate periodEnd;

    // 是否生成证书
    private Boolean generateCertificate;

    // 备注
    private String remark;
}
