package com.school.management.infrastructure.persistence.asset;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;

/**
 * 资产折旧Mapper
 */
@Mapper
public interface AssetDepreciationMapper extends BaseMapper<AssetDepreciationPO> {

    /**
     * 查询资产最近一条折旧记录
     */
    @Select("SELECT * FROM asset_depreciation WHERE asset_id = #{assetId} " +
            "ORDER BY depreciation_period DESC LIMIT 1")
    AssetDepreciationPO selectLatestByAssetId(@Param("assetId") Long assetId);

    /**
     * 统计资产累计折旧总额
     */
    @Select("SELECT COALESCE(SUM(depreciation_amount), 0) FROM asset_depreciation " +
            "WHERE asset_id = #{assetId}")
    BigDecimal sumDepreciationByAssetId(@Param("assetId") Long assetId);

    /**
     * 检查某期间是否已计提
     */
    @Select("SELECT COUNT(*) FROM asset_depreciation " +
            "WHERE asset_id = #{assetId} AND depreciation_period = #{period}")
    int countByAssetIdAndPeriod(@Param("assetId") Long assetId, @Param("period") String period);

    /**
     * 分页查询资产折旧历史
     */
    @Select("SELECT * FROM asset_depreciation WHERE asset_id = #{assetId} " +
            "ORDER BY depreciation_period DESC LIMIT #{offset}, #{limit}")
    List<AssetDepreciationPO> selectByAssetIdWithPagination(
            @Param("assetId") Long assetId,
            @Param("offset") int offset,
            @Param("limit") int limit
    );
}
