package com.school.management.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.management.entity.CheckCategory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 检查类别字典Mapper接口 (V3.0)
 *
 * @author Claude
 * @since 2025-11-24
 */
@Mapper
public interface CheckCategoryMapper extends BaseMapper<CheckCategory> {

    /**
     * 分页查询检查类别
     *
     * @param page 分页参数
     * @param query 查询条件
     * @return 类别列表
     */
    IPage<Map<String, Object>> selectCategoryPage(Page<Map<String, Object>> page, @Param("query") Map<String, Object> query);

    /**
     * 查询所有启用的类别
     *
     * @return 类别列表
     */
    List<CheckCategory> selectAllEnabled();

    /**
     * 根据类别编码查询
     *
     * @param categoryCode 类别编码
     * @return 检查类别
     */
    CheckCategory selectByCategoryCode(@Param("categoryCode") String categoryCode);

    /**
     * 根据类别类型查询
     *
     * @param categoryType 类别类型
     * @return 类别列表
     */
    List<CheckCategory> selectByCategoryType(@Param("categoryType") String categoryType);

    /**
     * 统计检查类别下的检查项数量
     *
     * @param categoryId 类别ID
     * @return 检查项数量
     */
    int countItemsByCategoryId(@Param("categoryId") Long categoryId);
}
