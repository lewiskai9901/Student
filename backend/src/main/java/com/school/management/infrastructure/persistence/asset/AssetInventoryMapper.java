package com.school.management.infrastructure.persistence.asset;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 资产盘点Mapper接口
 */
@Mapper
public interface AssetInventoryMapper extends BaseMapper<AssetInventoryPO> {

    /**
     * 根据状态查询盘点任务
     */
    @Select("SELECT * FROM asset_inventory WHERE status = #{status} ORDER BY created_at DESC")
    List<AssetInventoryPO> selectByStatus(@Param("status") Integer status);

    /**
     * 分页查询盘点任务
     */
    IPage<AssetInventoryPO> selectPage(Page<AssetInventoryPO> page, @Param("status") Integer status);

    /**
     * 获取今日最大盘点单号
     */
    @Select("SELECT MAX(inventory_code) FROM asset_inventory WHERE inventory_code LIKE CONCAT(#{prefix}, '%')")
    String getMaxInventoryCode(@Param("prefix") String prefix);
}
