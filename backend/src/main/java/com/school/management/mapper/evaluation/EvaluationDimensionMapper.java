package com.school.management.mapper.evaluation;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.management.entity.evaluation.EvaluationDimension;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 综测维度配置Mapper接口
 *
 * @author Claude
 * @since 2025-11-28
 */
@Mapper
public interface EvaluationDimensionMapper extends BaseMapper<EvaluationDimension> {

    /**
     * 查询所有启用的维度配置(按排序)
     */
    List<EvaluationDimension> selectAllEnabled();

    /**
     * 根据维度编码查询
     */
    EvaluationDimension selectByCode(@Param("code") String code);
}
