package com.school.management.mapper.evaluation;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.management.entity.evaluation.BehaviorType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 行为类型Mapper接口
 *
 * @author Claude
 * @since 2025-11-28
 */
@Mapper
public interface BehaviorTypeMapper extends BaseMapper<BehaviorType> {

    /**
     * 分页查询行为类型列表
     */
    Page<Map<String, Object>> selectBehaviorTypePage(Page<?> page, @Param("query") Map<String, Object> query);

    /**
     * 根据类别查询行为类型
     */
    List<BehaviorType> selectByCategory(@Param("category") String category);

    /**
     * 根据行为性质查询
     */
    List<BehaviorType> selectByNature(@Param("nature") Integer nature);

    /**
     * 查询行为类型详情(含映射规则)
     */
    Map<String, Object> selectDetailById(@Param("id") Long id);

    /**
     * 根据编码查询
     */
    BehaviorType selectByCode(@Param("code") String code);
}
