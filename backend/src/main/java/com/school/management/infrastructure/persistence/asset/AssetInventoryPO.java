package com.school.management.infrastructure.persistence.asset;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 资产盘点持久化对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("asset_inventory")
public class AssetInventoryPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String inventoryCode;

    private String inventoryName;

    private String scopeType;

    private String scopeValue;

    private LocalDate startDate;

    private LocalDate endDate;

    private Integer status;

    private Integer totalCount;

    private Integer checkedCount;

    private Integer profitCount;

    private Integer lossCount;

    private Long createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
