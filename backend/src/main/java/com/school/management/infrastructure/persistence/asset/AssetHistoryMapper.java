package com.school.management.infrastructure.persistence.asset;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 资产变更记录Mapper接口
 */
@Mapper
public interface AssetHistoryMapper extends BaseMapper<AssetHistoryPO> {

    /**
     * 根据资产ID查询变更记录(按时间倒序)
     */
    @Select("SELECT * FROM asset_history WHERE asset_id = #{assetId} ORDER BY operate_time DESC")
    List<AssetHistoryPO> selectByAssetId(@Param("assetId") Long assetId);

    /**
     * 根据资产ID和变更类型查询
     */
    @Select("SELECT * FROM asset_history WHERE asset_id = #{assetId} AND change_type = #{changeType} ORDER BY operate_time DESC")
    List<AssetHistoryPO> selectByAssetIdAndChangeType(@Param("assetId") Long assetId,
                                                       @Param("changeType") String changeType);

    /**
     * 查询最近N条变更记录
     */
    @Select("SELECT * FROM asset_history WHERE asset_id = #{assetId} ORDER BY operate_time DESC LIMIT #{limit}")
    List<AssetHistoryPO> selectRecentByAssetId(@Param("assetId") Long assetId, @Param("limit") int limit);
}
