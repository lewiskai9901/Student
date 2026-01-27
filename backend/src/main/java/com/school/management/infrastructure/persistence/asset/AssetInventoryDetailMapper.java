package com.school.management.infrastructure.persistence.asset;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 资产盘点明细Mapper接口
 */
@Mapper
public interface AssetInventoryDetailMapper extends BaseMapper<AssetInventoryDetailPO> {

    /**
     * 根据盘点ID查询明细列表(带资产信息)
     */
    List<AssetInventoryDetailPO> selectByInventoryIdWithAsset(@Param("inventoryId") Long inventoryId);

    /**
     * 批量插入盘点明细
     */
    int batchInsert(@Param("list") List<AssetInventoryDetailPO> list);
}
