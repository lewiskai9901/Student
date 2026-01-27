package com.school.management.infrastructure.persistence.asset;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 资产分类Mapper接口
 */
@Mapper
public interface AssetCategoryMapper extends BaseMapper<AssetCategoryPO> {

    /**
     * 查询分类列表(带资产数量统计)
     */
    List<AssetCategoryPO> selectAllWithAssetCount();

    /**
     * 查询顶级分类
     */
    @Select("SELECT * FROM asset_category WHERE parent_id IS NULL AND deleted = 0 ORDER BY sort_order")
    List<AssetCategoryPO> selectRootCategories();

    /**
     * 查询子分类
     */
    @Select("SELECT * FROM asset_category WHERE parent_id = #{parentId} AND deleted = 0 ORDER BY sort_order")
    List<AssetCategoryPO> selectByParentId(Long parentId);
}
