package com.school.management.service.evaluation.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.school.management.entity.evaluation.EvaluationDimension;
import com.school.management.exception.BusinessException;
import com.school.management.mapper.evaluation.EvaluationDimensionMapper;
import com.school.management.service.evaluation.EvaluationDimensionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 综测维度配置服务实现类
 *
 * @author Claude
 * @since 2025-11-28
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EvaluationDimensionServiceImpl extends ServiceImpl<EvaluationDimensionMapper, EvaluationDimension>
        implements EvaluationDimensionService {

    private final EvaluationDimensionMapper dimensionMapper;

    @Override
    public List<EvaluationDimension> getAllEnabled() {
        return dimensionMapper.selectAllEnabled();
    }

    @Override
    public EvaluationDimension getByCode(String code) {
        return dimensionMapper.selectByCode(code);
    }

    @Override
    public Map<String, EvaluationDimension> getDimensionMap() {
        List<EvaluationDimension> dimensions = getAllEnabled();
        Map<String, EvaluationDimension> map = new HashMap<>();
        for (EvaluationDimension dimension : dimensions) {
            map.put(dimension.getDimensionCode(), dimension);
        }
        return map;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDimension(EvaluationDimension dimension) {
        EvaluationDimension existing = dimensionMapper.selectById(dimension.getId());
        if (existing == null) {
            throw new BusinessException("维度配置不存在");
        }

        // 验证权重范围
        if (dimension.getWeight() != null) {
            if (dimension.getWeight().compareTo(BigDecimal.ZERO) < 0 ||
                    dimension.getWeight().compareTo(new BigDecimal("100")) > 0) {
                throw new BusinessException("权重必须在0-100之间");
            }
        }

        dimensionMapper.updateById(dimension);
        log.info("更新综测维度配置: id={}, code={}", dimension.getId(), existing.getDimensionCode());
    }

    @Override
    public Map<String, BigDecimal> getWeightMap() {
        List<EvaluationDimension> dimensions = getAllEnabled();
        Map<String, BigDecimal> map = new HashMap<>();
        for (EvaluationDimension dimension : dimensions) {
            map.put(dimension.getDimensionCode(), dimension.getWeight());
        }
        return map;
    }
}
