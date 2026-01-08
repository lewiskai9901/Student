package com.school.management.service.evaluation;

import com.baomidou.mybatisplus.extension.service.IService;
import com.school.management.entity.evaluation.EvaluationDimension;

import java.util.List;
import java.util.Map;

/**
 * 综测维度配置服务接口
 *
 * @author Claude
 * @since 2025-11-28
 */
public interface EvaluationDimensionService extends IService<EvaluationDimension> {

    /**
     * 获取所有启用的维度配置
     */
    List<EvaluationDimension> getAllEnabled();

    /**
     * 根据维度编码获取配置
     */
    EvaluationDimension getByCode(String code);

    /**
     * 获取维度配置Map (code -> dimension)
     */
    Map<String, EvaluationDimension> getDimensionMap();

    /**
     * 更新维度配置
     */
    void updateDimension(EvaluationDimension dimension);

    /**
     * 获取维度权重Map (code -> weight)
     */
    Map<String, java.math.BigDecimal> getWeightMap();
}
