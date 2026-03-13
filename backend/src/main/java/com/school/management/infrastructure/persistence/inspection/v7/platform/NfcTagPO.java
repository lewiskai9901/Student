package com.school.management.infrastructure.persistence.inspection.v7.platform;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("insp_nfc_tags")
public class NfcTagPO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private String tagUid;
    private String locationName;
    private Long placeId;
    private Long orgUnitId;
    private Boolean isActive;
    private LocalDateTime createdAt;

    @TableLogic
    private Integer deleted;
}
