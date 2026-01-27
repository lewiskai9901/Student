package com.school.management.infrastructure.persistence.asset;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 资产Mapper接口
 */
@Mapper
public interface AssetMapper extends BaseMapper<AssetPO> {

    /**
     * 分页查询资产列表(带分类信息)
     */
    IPage<AssetPO> selectPageWithCategory(Page<AssetPO> page,
                                          @Param("categoryId") Long categoryId,
                                          @Param("status") Integer status,
                                          @Param("locationType") String locationType,
                                          @Param("locationId") Long locationId,
                                          @Param("keyword") String keyword);

    /**
     * 查询资产详情(带分类信息)
     */
    AssetPO selectByIdWithCategory(@Param("id") Long id);

    /**
     * 根据位置查询资产列表
     */
    List<AssetPO> selectByLocation(@Param("locationType") String locationType,
                                   @Param("locationId") Long locationId);

    /**
     * 统计指定分类下的资产数量
     */
    @Select("SELECT COUNT(*) FROM asset WHERE category_id = #{categoryId} AND deleted = 0")
    int countByCategoryId(@Param("categoryId") Long categoryId);

    /**
     * 按状态统计资产数量
     */
    @Select("SELECT COUNT(*) FROM asset WHERE status = #{status} AND deleted = 0")
    int countByStatus(@Param("status") Integer status);

    /**
     * 统计全部资产数量
     */
    @Select("SELECT COUNT(*) FROM asset WHERE deleted = 0")
    int countAll();

    /**
     * 获取今日最大资产编号
     */
    @Select("SELECT MAX(asset_code) FROM asset WHERE asset_code LIKE CONCAT(#{prefix}, '%')")
    String getMaxAssetCode(@Param("prefix") String prefix);

    /**
     * 查询非指定状态的资产
     */
    @Select("SELECT * FROM asset WHERE status != #{status} AND deleted = 0")
    List<AssetPO> selectByStatusNot(@Param("status") Integer status);

    /**
     * 按分类查询非指定状态的资产
     */
    @Select("SELECT * FROM asset WHERE category_id = #{categoryId} AND status != #{status} AND deleted = 0")
    List<AssetPO> selectByCategoryIdAndStatusNot(@Param("categoryId") Long categoryId, @Param("status") Integer status);

    /**
     * 按位置查询非指定状态的资产
     */
    @Select("SELECT * FROM asset WHERE location_type = #{locationType} AND location_id = #{locationId} AND status != #{status} AND deleted = 0")
    List<AssetPO> selectByLocationAndStatusNot(@Param("locationType") String locationType,
                                                @Param("locationId") Long locationId,
                                                @Param("status") Integer status);
}
