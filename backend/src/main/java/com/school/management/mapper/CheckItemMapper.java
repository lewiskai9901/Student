package com.school.management.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.management.entity.CheckItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 检查项字典Mapper接口 (V3.0)
 *
 * @author Claude
 * @since 2025-11-24
 */
@Mapper
public interface CheckItemMapper extends BaseMapper<CheckItem> {

    /**
     * 分页查询检查项
     *
     * @param page 分页参数
     * @param query 查询条件
     * @return 检查项列表
     */
    IPage<Map<String, Object>> selectItemPage(Page<Map<String, Object>> page, @Param("query") Map<String, Object> query);

    /**
     * 根据类别ID查询检查项
     *
     * @param categoryId 类别ID
     * @return 检查项列表
     */
    List<CheckItem> selectByCategoryId(@Param("categoryId") Long categoryId);

    /**
     * 根据检查项编码查询
     *
     * @param itemCode 检查项编码
     * @return 检查项
     */
    CheckItem selectByItemCode(@Param("itemCode") String itemCode);

    /**
     * 查询所有启用的检查项
     *
     * @return 检查项列表
     */
    List<CheckItem> selectAllEnabled();

    /**
     * 根据类别ID查询启用的检查项
     *
     * @param categoryId 类别ID
     * @return 检查项列表
     */
    List<CheckItem> selectEnabledByCategoryId(@Param("categoryId") Long categoryId);

    /**
     * 批量插入检查项
     *
     * @param items 检查项列表
     * @return 插入数量
     */
    int batchInsert(@Param("items") List<CheckItem> items);

    /**
     * 根据类别ID删除检查项（逻辑删除）
     *
     * @param categoryId 类别ID
     * @return 删除数量
     */
    int deleteByCategoryId(@Param("categoryId") Long categoryId);
}
