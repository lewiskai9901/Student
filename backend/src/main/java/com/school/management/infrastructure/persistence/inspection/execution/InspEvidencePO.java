package com.school.management.infrastructure.persistence.inspection.execution;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("insp_evidences")
public class InspEvidencePO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    private Long orgUnitId;               // 数据权限边界 (从 submission 继承, MetaObjectHandler 自动填充)
    private Long submissionId;
    private Long detailId;
    private String evidenceType;
    private String fileName;
    private String filePath;
    private String fileUrl;
    private Long fileSize;
    private String mimeType;
    private String thumbnailUrl;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private LocalDateTime capturedAt;
    private String metadata;
    private String aiAnalysis;
    private BigDecimal aiConfidence;
    private LocalDateTime createdAt;

    @TableLogic
    private Integer deleted;
}
