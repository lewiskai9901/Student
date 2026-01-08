package com.school.management.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.management.entity.DailyCheckWeightConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 日常检查加权配置关联Mapper接口
 *
 * @author system
 * @since 1.0.0
 */
@Mapper
public interface DailyCheckWeightConfigMapper extends BaseMapper<DailyCheckWeightConfig> {

    /**
     * 根据日常检查ID查询所有关联的加权配置
     *
     * @param dailyCheckId 日常检查ID
     * @return 加权配置列表
     */
    @Select("SELECT dcwc.*, cwc.config_name as weight_config_name, cwc.weight_mode, cwc.standard_size_mode, cwc.standard_size, " +
            "cwc.min_weight, cwc.max_weight, cwc.enable_weight_limit " +
            "FROM daily_check_weight_configs dcwc " +
            "LEFT JOIN class_weight_configs cwc ON dcwc.weight_config_id = cwc.id " +
            "WHERE dcwc.daily_check_id = #{dailyCheckId} AND dcwc.deleted = 0 " +
            "ORDER BY dcwc.priority DESC, dcwc.sort_order ASC")
    List<DailyCheckWeightConfig> selectByDailyCheckId(@Param("dailyCheckId") Long dailyCheckId);

    /**
     * 查询指定日常检查的默认加权配置
     *
     * @param dailyCheckId 日常检查ID
     * @return 默认加权配置
     */
    @Select("SELECT dcwc.*, cwc.config_name as weight_config_name, cwc.weight_mode, cwc.standard_size_mode " +
            "FROM daily_check_weight_configs dcwc " +
            "LEFT JOIN class_weight_configs cwc ON dcwc.weight_config_id = cwc.id " +
            "WHERE dcwc.daily_check_id = #{dailyCheckId} AND dcwc.is_default = 1 AND dcwc.deleted = 0 " +
            "LIMIT 1")
    DailyCheckWeightConfig selectDefaultConfig(@Param("dailyCheckId") Long dailyCheckId);

    /**
     * 根据分类ID查询适用的加权配置
     * 优先匹配CATEGORY级别的配置
     *
     * @param dailyCheckId 日常检查ID
     * @param categoryId 分类ID
     * @return 加权配置
     */
    @Select("SELECT dcwc.*, cwc.config_name as weight_config_name, cwc.weight_mode, cwc.standard_size_mode " +
            "FROM daily_check_weight_configs dcwc " +
            "LEFT JOIN class_weight_configs cwc ON dcwc.weight_config_id = cwc.id " +
            "WHERE dcwc.daily_check_id = #{dailyCheckId} " +
            "AND dcwc.apply_scope = 'CATEGORY' " +
            "AND dcwc.deleted = 0 " +
            "AND JSON_CONTAINS(dcwc.apply_category_ids, CAST(#{categoryId} AS CHAR)) " +
            "ORDER BY dcwc.priority DESC " +
            "LIMIT 1")
    DailyCheckWeightConfig selectByCategoryId(@Param("dailyCheckId") Long dailyCheckId, @Param("categoryId") Long categoryId);

    /**
     * 根据扣分项ID查询适用的加权配置
     * 优先匹配ITEM级别的配置
     *
     * @param dailyCheckId 日常检查ID
     * @param itemId 扣分项ID
     * @return 加权配置
     */
    @Select("SELECT dcwc.*, cwc.config_name as weight_config_name, cwc.weight_mode, cwc.standard_size_mode " +
            "FROM daily_check_weight_configs dcwc " +
            "LEFT JOIN class_weight_configs cwc ON dcwc.weight_config_id = cwc.id " +
            "WHERE dcwc.daily_check_id = #{dailyCheckId} " +
            "AND dcwc.apply_scope = 'ITEM' " +
            "AND dcwc.deleted = 0 " +
            "AND JSON_CONTAINS(dcwc.apply_item_ids, CAST(#{itemId} AS CHAR)) " +
            "ORDER BY dcwc.priority DESC " +
            "LIMIT 1")
    DailyCheckWeightConfig selectByItemId(@Param("dailyCheckId") Long dailyCheckId, @Param("itemId") Long itemId);

    /**
     * 删除指定日常检查的所有加权配置关联（软删除）
     *
     * @param dailyCheckId 日常检查ID
     * @return 影响行数
     */
    @Select("UPDATE daily_check_weight_configs SET deleted = 1 WHERE daily_check_id = #{dailyCheckId}")
    int softDeleteByDailyCheckId(@Param("dailyCheckId") Long dailyCheckId);
}
