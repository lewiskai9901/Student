package com.school.management.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.management.entity.CategoryWeightRule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 类别加权规则Mapper接口
 *
 * @author system
 * @version 3.0.0
 * @since 2024-12-29
 */
@Mapper
public interface CategoryWeightRuleMapper extends BaseMapper<CategoryWeightRule> {

    /**
     * 查询配置的所有类别规则
     *
     * @param weightConfigId 加权配置ID
     * @return 类别规则列表
     */
    List<CategoryWeightRule> selectByConfigId(@Param("weightConfigId") Long weightConfigId);

    /**
     * 查询类别的加权规则
     *
     * @param weightConfigId 加权配置ID
     * @param categoryId 类别ID
     * @return 类别规则
     */
    CategoryWeightRule selectByConfigAndCategory(
            @Param("weightConfigId") Long weightConfigId,
            @Param("categoryId") Long categoryId
    );

    /**
     * 批量插入类别规则
     *
     * @param rules 类别规则列表
     * @return 插入行数
     */
    int batchInsert(@Param("rules") List<CategoryWeightRule> rules);

    /**
     * 删除配置的所有类别规则
     *
     * @param weightConfigId 加权配置ID
     * @return 删除行数
     */
    int deleteByConfigId(@Param("weightConfigId") Long weightConfigId);
}
