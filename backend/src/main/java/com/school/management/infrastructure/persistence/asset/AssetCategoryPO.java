package com.school.management.infrastructure.persistence.asset;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 资产分类持久化对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("asset_category")
public class AssetCategoryPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long parentId;

    private String categoryCode;

    private String categoryName;

    private Integer categoryType;

    private Integer defaultManagementMode;

    private Integer depreciationYears;

    private String unit;

    private Integer sortOrder;

    private String remark;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;

    // 非持久化字段,用于统计
    @TableField(exist = false)
    private Integer assetCount;
}
