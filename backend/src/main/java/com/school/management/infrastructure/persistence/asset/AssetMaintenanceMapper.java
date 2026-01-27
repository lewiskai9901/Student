package com.school.management.infrastructure.persistence.asset;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 资产维修保养记录Mapper接口
 */
@Mapper
public interface AssetMaintenanceMapper extends BaseMapper<AssetMaintenancePO> {

    /**
     * 根据资产ID查询维修记录
     */
    @Select("SELECT * FROM asset_maintenance WHERE asset_id = #{assetId} ORDER BY created_at DESC")
    List<AssetMaintenancePO> selectByAssetId(@Param("assetId") Long assetId);

    /**
     * 根据状态查询
     */
    @Select("SELECT * FROM asset_maintenance WHERE status = #{status} ORDER BY created_at DESC")
    List<AssetMaintenancePO> selectByStatus(@Param("status") Integer status);

    /**
     * 查询资产的进行中维修记录
     */
    @Select("SELECT * FROM asset_maintenance WHERE asset_id = #{assetId} AND status = 1 LIMIT 1")
    AssetMaintenancePO selectInProgressByAssetId(@Param("assetId") Long assetId);

    /**
     * 分页查询维修记录(带资产信息)
     */
    IPage<AssetMaintenancePO> selectPageWithAsset(Page<AssetMaintenancePO> page,
                                                   @Param("assetId") Long assetId,
                                                   @Param("maintenanceType") Integer maintenanceType,
                                                   @Param("status") Integer status);
}
