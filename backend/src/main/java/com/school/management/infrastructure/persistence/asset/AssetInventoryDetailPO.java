package com.school.management.infrastructure.persistence.asset;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 资产盘点明细持久化对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("asset_inventory_detail")
public class AssetInventoryDetailPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long inventoryId;

    private Long assetId;

    private Integer expectedQuantity;

    private Integer actualQuantity;

    private Integer difference;

    private Integer resultType;

    private LocalDateTime checkTime;

    private Long checkerId;

    private String checkerName;

    private String remark;

    // 非持久化字段,用于关联查询
    @TableField(exist = false)
    private String assetCode;

    @TableField(exist = false)
    private String assetName;

    @TableField(exist = false)
    private String locationName;
}
