package com.school.management.service.analysis;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.school.management.dto.analysis.AnalysisConfigDTO;
import com.school.management.dto.analysis.AnalysisMetricDTO;
import com.school.management.entity.analysis.StatAnalysisConfig;
import com.school.management.entity.analysis.AnalysisMetric;
import com.school.management.entity.analysis.CategoryMapping;
import com.school.management.exception.BusinessException;
import com.school.management.mapper.analysis.StatAnalysisConfigMapper;
import com.school.management.mapper.analysis.AnalysisMetricMapper;
import com.school.management.mapper.analysis.CategoryMappingMapper;
import com.school.management.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 统计分析配置服务
 *
 * 注意：这个服务操作 stat_analysis_configs 表（StatAnalysisConfig 实体）
 * 与 service/AnalysisConfigService 接口（操作 analysis_configs 表）是不同的功能模块
 *
 * @see com.school.management.service.AnalysisConfigService 用于 CheckPlan 的内置分析配置
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StatAnalysisConfigService extends ServiceImpl<StatAnalysisConfigMapper, StatAnalysisConfig> {

    private final StatAnalysisConfigMapper configMapper;
    private final AnalysisMetricMapper metricMapper;
    private final CategoryMappingMapper categoryMappingMapper;

    /**
     * 分页查询配置
     */
    public IPage<AnalysisConfigDTO> getConfigPage(Long planId, String configName, Boolean isEnabled, int pageNum, int pageSize) {
        Page<StatAnalysisConfig> page = new Page<>(pageNum, pageSize);
        IPage<StatAnalysisConfig> configPage = configMapper.selectPageByCondition(page, planId, configName, null, isEnabled);

        return configPage.convert(this::toDTO);
    }

    /**
     * 获取计划下的配置列表
     */
    public List<AnalysisConfigDTO> getConfigsByPlanId(Long planId) {
        List<StatAnalysisConfig> configs = configMapper.selectByPlanId(planId);
        return configs.stream().map(this::toDTO).collect(Collectors.toList());
    }

    /**
     * 获取配置详情
     */
    public AnalysisConfigDTO getConfigDetail(Long id) {
        StatAnalysisConfig config = configMapper.selectById(id);
        if (config == null || Boolean.TRUE.equals(config.getDeleted())) {
            throw new BusinessException("配置不存在");
        }

        AnalysisConfigDTO dto = toDTO(config);

        // 加载指标配置
        List<AnalysisMetric> metrics = metricMapper.selectByConfigId(id);
        dto.setMetrics(metrics.stream().map(this::metricToDTO).collect(Collectors.toList()));

        // 加载类别映射
        List<CategoryMapping> mappings = categoryMappingMapper.selectByConfigId(id);
        dto.setCategoryMappings(mappings.stream().map(this::mappingToDTO).collect(Collectors.toList()));

        return dto;
    }

    /**
     * 创建配置
     */
    @Transactional(rollbackFor = Exception.class)
    public AnalysisConfigDTO createConfig(AnalysisConfigDTO dto) {
        // 验证配置名称唯一
        validateConfigName(dto.getPlanId(), dto.getConfigName(), null);

        StatAnalysisConfig config = toEntity(dto);
        config.setCreatorId(SecurityUtils.getCurrentUserId());
        config.setCreatedAt(LocalDateTime.now());
        config.setUpdatedAt(LocalDateTime.now());
        config.setDeleted(false);

        configMapper.insert(config);

        // 保存指标配置
        if (dto.getMetrics() != null && !dto.getMetrics().isEmpty()) {
            saveMetrics(config.getId(), dto.getMetrics());
        }

        // 保存类别映射
        if (dto.getCategoryMappings() != null && !dto.getCategoryMappings().isEmpty()) {
            saveCategoryMappings(config.getId(), dto.getCategoryMappings());
        }

        return getConfigDetail(config.getId());
    }

    /**
     * 更新配置
     */
    @Transactional(rollbackFor = Exception.class)
    public AnalysisConfigDTO updateConfig(Long id, AnalysisConfigDTO dto) {
        StatAnalysisConfig existing = configMapper.selectById(id);
        if (existing == null || Boolean.TRUE.equals(existing.getDeleted())) {
            throw new BusinessException("配置不存在");
        }

        // 验证配置名称唯一
        if (StringUtils.hasText(dto.getConfigName()) && !dto.getConfigName().equals(existing.getConfigName())) {
            validateConfigName(existing.getPlanId(), dto.getConfigName(), id);
        }

        // 更新基本信息
        if (StringUtils.hasText(dto.getConfigName())) {
            existing.setConfigName(dto.getConfigName());
        }
        if (StringUtils.hasText(dto.getConfigDesc())) {
            existing.setConfigDesc(dto.getConfigDesc());
        }
        if (dto.getScopeType() != null) {
            existing.setScopeType(dto.getScopeType());
        }
        if (dto.getScopeConfig() != null) {
            existing.setScopeConfig(dto.getScopeConfig());
        }
        if (dto.getTargetType() != null) {
            existing.setTargetType(dto.getTargetType());
        }
        if (dto.getTargetConfig() != null) {
            existing.setTargetConfig(dto.getTargetConfig());
        }
        if (dto.getUpdateMode() != null) {
            existing.setUpdateMode(dto.getUpdateMode());
        }
        if (dto.getMissingStrategy() != null) {
            existing.setMissingStrategy(dto.getMissingStrategy());
        }
        if (dto.getMissingStrategyConfig() != null) {
            existing.setMissingStrategyConfig(dto.getMissingStrategyConfig());
        }
        if (dto.getIsPublic() != null) {
            existing.setIsPublic(dto.getIsPublic());
        }
        if (dto.getIsEnabled() != null) {
            existing.setIsEnabled(dto.getIsEnabled());
        }
        if (dto.getSortOrder() != null) {
            existing.setSortOrder(dto.getSortOrder());
        }

        existing.setUpdatedAt(LocalDateTime.now());
        configMapper.updateById(existing);

        // 更新指标配置
        if (dto.getMetrics() != null) {
            metricMapper.deleteByConfigId(id);
            saveMetrics(id, dto.getMetrics());
        }

        // 更新类别映射
        if (dto.getCategoryMappings() != null) {
            categoryMappingMapper.deleteByConfigId(id);
            saveCategoryMappings(id, dto.getCategoryMappings());
        }

        return getConfigDetail(id);
    }

    /**
     * 删除配置
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteConfig(Long id) {
        StatAnalysisConfig config = configMapper.selectById(id);
        if (config == null) {
            throw new BusinessException("配置不存在");
        }

        // 使用 MyBatis Plus 逻辑删除
        configMapper.deleteById(id);
    }

    /**
     * 启用/禁用配置
     */
    @Transactional(rollbackFor = Exception.class)
    public void toggleEnabled(Long id, boolean enabled) {
        StatAnalysisConfig config = configMapper.selectById(id);
        if (config == null || Boolean.TRUE.equals(config.getDeleted())) {
            throw new BusinessException("配置不存在");
        }

        config.setIsEnabled(enabled);
        config.setUpdatedAt(LocalDateTime.now());
        configMapper.updateById(config);
    }

    /**
     * 设为默认配置
     */
    @Transactional(rollbackFor = Exception.class)
    public void setDefault(Long id) {
        StatAnalysisConfig config = configMapper.selectById(id);
        if (config == null || Boolean.TRUE.equals(config.getDeleted())) {
            throw new BusinessException("配置不存在");
        }

        // 清除原默认配置
        StatAnalysisConfig currentDefault = configMapper.selectDefaultByPlanId(config.getPlanId());
        if (currentDefault != null && !currentDefault.getId().equals(id)) {
            currentDefault.setIsDefault(false);
            currentDefault.setUpdatedAt(LocalDateTime.now());
            configMapper.updateById(currentDefault);
        }

        // 设置新默认配置
        config.setIsDefault(true);
        config.setUpdatedAt(LocalDateTime.now());
        configMapper.updateById(config);
    }

    /**
     * 复制配置
     */
    @Transactional(rollbackFor = Exception.class)
    public AnalysisConfigDTO copyConfig(Long id, String newName) {
        AnalysisConfigDTO original = getConfigDetail(id);

        // 生成新名称
        if (!StringUtils.hasText(newName)) {
            newName = original.getConfigName() + " - 副本";
        }
        validateConfigName(original.getPlanId(), newName, null);

        // 复制配置
        original.setId(null);
        original.setConfigName(newName);
        original.setIsDefault(false);
        original.setCreatedAt(null);
        original.setUpdatedAt(null);

        // 清除指标ID
        if (original.getMetrics() != null) {
            original.getMetrics().forEach(m -> {
                m.setId(null);
                m.setConfigId(null);
            });
        }

        // 清除映射ID
        if (original.getCategoryMappings() != null) {
            original.getCategoryMappings().forEach(m -> m.setId(null));
        }

        return createConfig(original);
    }

    // ==================== 私有方法 ====================

    private void validateConfigName(Long planId, String configName, Long excludeId) {
        LambdaQueryWrapper<StatAnalysisConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StatAnalysisConfig::getPlanId, planId)
                .eq(StatAnalysisConfig::getConfigName, configName)
                .eq(StatAnalysisConfig::getDeleted, false);
        if (excludeId != null) {
            wrapper.ne(StatAnalysisConfig::getId, excludeId);
        }

        if (configMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("配置名称已存在");
        }
    }

    private void saveMetrics(Long configId, List<AnalysisMetricDTO> metrics) {
        int order = 0;
        for (AnalysisMetricDTO metricDTO : metrics) {
            AnalysisMetric metric = metricToEntity(metricDTO);
            metric.setConfigId(configId);
            metric.setDisplayOrder(order++);
            metric.setCreatedAt(LocalDateTime.now());
            metric.setUpdatedAt(LocalDateTime.now());
            metricMapper.insert(metric);
        }
    }

    private void saveCategoryMappings(Long configId, List<AnalysisConfigDTO.CategoryMappingDTO> mappings) {
        for (AnalysisConfigDTO.CategoryMappingDTO mappingDTO : mappings) {
            CategoryMapping mapping = new CategoryMapping();
            mapping.setConfigId(configId);
            mapping.setTemplateCategoryId(mappingDTO.getTemplateCategoryId());
            mapping.setTemplateCategoryName(mappingDTO.getTemplateCategoryName());
            mapping.setUnifiedCategoryId(mappingDTO.getUnifiedCategoryId());
            mapping.setUnifiedCategoryName(mappingDTO.getUnifiedCategoryName());
            mapping.setCreatedAt(LocalDateTime.now());
            categoryMappingMapper.insert(mapping);
        }
    }

    private AnalysisConfigDTO toDTO(StatAnalysisConfig entity) {
        AnalysisConfigDTO dto = new AnalysisConfigDTO();
        BeanUtils.copyProperties(entity, dto);

        // 转换嵌套对象
        if (entity.getScopeConfig() != null) {
            dto.setScopeConfig(entity.getScopeConfig());
        }
        if (entity.getTargetConfig() != null) {
            dto.setTargetConfig(entity.getTargetConfig());
        }
        if (entity.getMissingStrategyConfig() != null) {
            dto.setMissingStrategyConfig(entity.getMissingStrategyConfig());
        }

        return dto;
    }

    private StatAnalysisConfig toEntity(AnalysisConfigDTO dto) {
        StatAnalysisConfig entity = new StatAnalysisConfig();
        BeanUtils.copyProperties(dto, entity);
        return entity;
    }

    private AnalysisMetricDTO metricToDTO(AnalysisMetric entity) {
        AnalysisMetricDTO dto = new AnalysisMetricDTO();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }

    private AnalysisMetric metricToEntity(AnalysisMetricDTO dto) {
        AnalysisMetric entity = new AnalysisMetric();
        BeanUtils.copyProperties(dto, entity);
        return entity;
    }

    private AnalysisConfigDTO.CategoryMappingDTO mappingToDTO(CategoryMapping entity) {
        AnalysisConfigDTO.CategoryMappingDTO dto = new AnalysisConfigDTO.CategoryMappingDTO();
        dto.setId(entity.getId());
        dto.setTemplateCategoryId(entity.getTemplateCategoryId());
        dto.setTemplateCategoryName(entity.getTemplateCategoryName());
        dto.setUnifiedCategoryId(entity.getUnifiedCategoryId());
        dto.setUnifiedCategoryName(entity.getUnifiedCategoryName());
        return dto;
    }
}
