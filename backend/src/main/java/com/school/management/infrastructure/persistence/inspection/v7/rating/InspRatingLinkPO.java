package com.school.management.infrastructure.persistence.inspection.v7.rating;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("insp_rating_links")
public class InspRatingLinkPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    private Long projectId;
    private Long ratingConfigId;
    private String periodType;
    private Boolean autoCalculate;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
