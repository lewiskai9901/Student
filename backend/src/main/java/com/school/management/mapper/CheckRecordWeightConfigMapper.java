package com.school.management.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.management.entity.CheckRecordWeightConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 检查记录加权配置Mapper接口 (V3.0)
 *
 * @author Claude
 * @since 2025-11-24
 */
@Mapper
public interface CheckRecordWeightConfigMapper extends BaseMapper<CheckRecordWeightConfig> {

    /**
     * 根据记录ID查询所有加权配置（树形结构）
     *
     * @param recordId 记录ID
     * @return 配置列表
     */
    List<Map<String, Object>> selectConfigTreeByRecordId(@Param("recordId") Long recordId);

    /**
     * 获取生效的加权配置（处理继承逻辑）
     *
     * 优先级: ITEM > CATEGORY > RECORD
     *
     * @param recordId 记录ID
     * @param categoryId 类别ID（可选）
     * @param itemId 检查项ID（可选）
     * @return 生效的配置
     */
    Map<String, Object> selectEffectiveConfig(@Param("recordId") Long recordId,
                                               @Param("categoryId") Long categoryId,
                                               @Param("itemId") Long itemId);

    /**
     * 根据记录ID和层级查询配置
     *
     * @param recordId 记录ID
     * @param configLevel 配置层级 RECORD/CATEGORY/ITEM
     * @return 配置列表
     */
    List<CheckRecordWeightConfig> selectByRecordIdAndLevel(@Param("recordId") Long recordId,
                                                             @Param("configLevel") String configLevel);

    /**
     * 批量插入加权配置
     *
     * @param configs 配置列表
     * @return 插入数量
     */
    int batchInsert(@Param("configs") List<CheckRecordWeightConfig> configs);

    /**
     * 根据记录ID删除所有配置
     *
     * @param recordId 记录ID
     * @return 删除数量
     */
    int deleteByRecordId(@Param("recordId") Long recordId);

    /**
     * 查询记录级别配置
     *
     * @param recordId 记录ID
     * @return 记录级别配置
     */
    CheckRecordWeightConfig selectRecordLevelConfig(@Param("recordId") Long recordId);

    /**
     * 查询类别级别配置列表
     *
     * @param recordId 记录ID
     * @return 类别级别配置列表
     */
    List<CheckRecordWeightConfig> selectCategoryLevelConfigs(@Param("recordId") Long recordId);

    /**
     * 查询检查项级别配置列表
     *
     * @param recordId 记录ID
     * @param categoryId 类别ID（可选）
     * @return 检查项级别配置列表
     */
    List<CheckRecordWeightConfig> selectItemLevelConfigs(@Param("recordId") Long recordId,
                                                          @Param("categoryId") Long categoryId);
}
