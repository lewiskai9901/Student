package com.school.management.infrastructure.persistence.inspection.v6;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * V6检查证据持久化对象
 */
@Data
@TableName("inspection_evidences")
public class InspectionEvidencePO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long detailId;
    private Long targetId;

    // 文件信息
    private String fileName;
    private String filePath;
    private String fileUrl;
    private Long fileSize;
    private String fileType;

    // GPS元数据
    private BigDecimal latitude;
    private BigDecimal longitude;

    // 审计
    private Long uploadBy;
    private LocalDateTime uploadTime;
    private LocalDateTime createdAt;
}
