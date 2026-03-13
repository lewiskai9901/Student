package com.school.management.infrastructure.persistence.inspection.v7.knowledge;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("insp_knowledge_articles")
public class KnowledgeArticlePO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    private String title;
    private String content;
    private String category;
    private String tags;
    private String relatedItemCodes;
    private Long sourceCaseId;
    private Integer viewCount;
    private Integer helpfulCount;
    private Boolean isPinned;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
