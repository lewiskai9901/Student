package com.school.management.dto.rating.notification;

import lombok.Data;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

/**
 * 证书批量生成请求DTO
 *
 * @author Claude Code
 * @since 2025-12-22
 */
@Data
public class CertificateGenerateRequest {

    @NotNull(message = "徽章ID不能为空")
    private Long badgeId;

    @NotEmpty(message = "班级ID列表不能为空")
    private List<Long> classIds;

    private LocalDate periodStart;
    private LocalDate periodEnd;
    private String periodLabel;         // 如"2025年12月"

    // 证书类型
    @NotNull(message = "证书类型不能为空")
    private String certificateType;     // GOLD金质/SILVER银质/BRONZE铜质

    // 荣誉称号
    private String honorTitle;          // 如"12月卫生标兵班级"

    // 表彰内容（支持变量占位符）
    private String honorContent;

    // 落款单位
    private String issuerName;          // 如"学生处"

    // 是否发送给班主任
    private Boolean sendToTeacher;

    // 输出格式
    private String outputFormat;        // PDF/IMAGE
}
