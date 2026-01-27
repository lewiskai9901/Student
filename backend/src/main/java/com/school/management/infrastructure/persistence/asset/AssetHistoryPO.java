package com.school.management.infrastructure.persistence.asset;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 资产变更记录持久化对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("asset_history")
public class AssetHistoryPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long assetId;

    private String changeType;

    private String changeContent;

    private String oldLocationType;

    private Long oldLocationId;

    private String oldLocationName;

    private String newLocationType;

    private Long newLocationId;

    private String newLocationName;

    private Long operatorId;

    private String operatorName;

    private LocalDateTime operateTime;

    private String remark;
}
