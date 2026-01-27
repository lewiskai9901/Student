package com.school.management.application.asset.query;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 资产变更记录DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetHistoryDTO {

    private Long id;

    private Long assetId;

    private String changeType;

    private String changeTypeDesc;

    private String changeContent;

    private String oldLocationType;

    private Long oldLocationId;

    private String oldLocationName;

    private String newLocationType;

    private Long newLocationId;

    private String newLocationName;

    private Long operatorId;

    private String operatorName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime operateTime;

    private String remark;
}
