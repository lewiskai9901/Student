package com.school.management.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.dto.DailyCheckWeightConfigDTO;
import com.school.management.entity.*;
import com.school.management.mapper.*;
import com.school.management.service.DailyCheckWeightConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 日常检查加权配置服务实现类
 *
 * @author system
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DailyCheckWeightConfigServiceImpl implements DailyCheckWeightConfigService {

    private final DailyCheckWeightConfigMapper configMapper;
    private final ClassWeightConfigMapper weightConfigMapper;
    private final DailyCheckMapper dailyCheckMapper;
    private final DailyCheckTargetMapper dailyCheckTargetMapper;
    private final ClassMapper classMapper;
    private final CheckCategoryMapper checkCategoryMapper;
    private final ObjectMapper objectMapper;

    @Override
    public List<DailyCheckWeightConfigDTO> getConfigsByDailyCheckId(Long dailyCheckId) {
        log.info("获取日常检查的加权配置列表: dailyCheckId={}", dailyCheckId);
        List<DailyCheckWeightConfig> configs = configMapper.selectByDailyCheckId(dailyCheckId);
        return configs.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public DailyCheckWeightConfigDTO getApplicableConfig(Long dailyCheckId, Long categoryId, Long itemId) {
        log.info("获取适用的加权配置: dailyCheckId={}, categoryId={}, itemId={}", dailyCheckId, categoryId, itemId);

        // 1. 优先查找ITEM级别配置
        if (itemId != null) {
            DailyCheckWeightConfig itemConfig = configMapper.selectByItemId(dailyCheckId, itemId);
            if (itemConfig != null) {
                log.info("找到ITEM级别加权配置: configId={}", itemConfig.getId());
                return toDTO(itemConfig);
            }
        }

        // 2. 查找CATEGORY级别配置
        if (categoryId != null) {
            DailyCheckWeightConfig categoryConfig = configMapper.selectByCategoryId(dailyCheckId, categoryId);
            if (categoryConfig != null) {
                log.info("找到CATEGORY级别加权配置: configId={}", categoryConfig.getId());
                return toDTO(categoryConfig);
            }
        }

        // 3. 使用默认配置
        DailyCheckWeightConfig defaultConfig = configMapper.selectDefaultConfig(dailyCheckId);
        if (defaultConfig != null) {
            log.info("使用默认加权配置: configId={}", defaultConfig.getId());
            return toDTO(defaultConfig);
        }

        log.warn("未找到适用的加权配置: dailyCheckId={}, categoryId={}, itemId={}", dailyCheckId, categoryId, itemId);
        return null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<DailyCheckWeightConfigDTO> saveConfigs(Long dailyCheckId, List<DailyCheckWeightConfigDTO> configs) {
        log.info("保存日常检查的加权配置: dailyCheckId={}, configCount={}", dailyCheckId, configs.size());

        // 1. 软删除原有配置
        configMapper.softDeleteByDailyCheckId(dailyCheckId);

        // 2. 保存新配置
        List<DailyCheckWeightConfigDTO> savedConfigs = new ArrayList<>();
        for (int i = 0; i < configs.size(); i++) {
            DailyCheckWeightConfigDTO config = configs.get(i);
            config.setDailyCheckId(dailyCheckId);
            config.setSortOrder(i);

            DailyCheckWeightConfig entity = toEntity(config);

            // 创建配置快照
            createConfigSnapshot(entity);

            // 计算标准人数
            Integer standardSize = calculateStandardSize(dailyCheckId, entity.getWeightConfigId());
            entity.setCalculatedStandardSize(standardSize);

            configMapper.insert(entity);
            savedConfigs.add(toDTO(entity));
        }

        log.info("保存完成: 共{}个配置", savedConfigs.size());
        return savedConfigs;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DailyCheckWeightConfigDTO addConfig(DailyCheckWeightConfigDTO config) {
        log.info("添加加权配置: dailyCheckId={}, weightConfigId={}", config.getDailyCheckId(), config.getWeightConfigId());

        DailyCheckWeightConfig entity = toEntity(config);

        // 创建配置快照
        createConfigSnapshot(entity);

        // 计算标准人数
        Integer standardSize = calculateStandardSize(config.getDailyCheckId(), config.getWeightConfigId());
        entity.setCalculatedStandardSize(standardSize);

        configMapper.insert(entity);
        return toDTO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DailyCheckWeightConfigDTO updateConfig(DailyCheckWeightConfigDTO config) {
        log.info("更新加权配置: configId={}", config.getId());

        DailyCheckWeightConfig entity = toEntity(config);

        // 重新创建配置快照
        createConfigSnapshot(entity);

        // 重新计算标准人数
        Integer standardSize = calculateStandardSize(config.getDailyCheckId(), config.getWeightConfigId());
        entity.setCalculatedStandardSize(standardSize);

        configMapper.updateById(entity);
        return toDTO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteConfig(Long configId) {
        log.info("删除加权配置: configId={}", configId);
        DailyCheckWeightConfig entity = new DailyCheckWeightConfig();
        entity.setId(configId);
        entity.setDeleted(1);
        return configMapper.updateById(entity) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteByDailyCheckId(Long dailyCheckId) {
        log.info("删除日常检查的所有加权配置: dailyCheckId={}", dailyCheckId);
        return configMapper.softDeleteByDailyCheckId(dailyCheckId);
    }

    @Override
    public Integer calculateAndUpdateStandardSize(Long configId) {
        DailyCheckWeightConfig config = configMapper.selectById(configId);
        if (config == null) {
            log.warn("配置不存在: configId={}", configId);
            return null;
        }

        Integer standardSize = calculateStandardSize(config.getDailyCheckId(), config.getWeightConfigId());
        if (standardSize != null) {
            config.setCalculatedStandardSize(standardSize);
            configMapper.updateById(config);
        }
        return standardSize;
    }

    @Override
    public List<DailyCheckWeightConfigDTO.ColorScheme> getPredefinedColors() {
        return DailyCheckWeightConfigDTO.PREDEFINED_COLORS;
    }

    @Override
    public DailyCheckWeightConfigDTO toDTO(DailyCheckWeightConfig entity) {
        if (entity == null) return null;

        DailyCheckWeightConfigDTO dto = new DailyCheckWeightConfigDTO();
        dto.setId(entity.getId());
        dto.setDailyCheckId(entity.getDailyCheckId());
        dto.setWeightConfigId(entity.getWeightConfigId());
        dto.setWeightConfigName(entity.getWeightConfigName());
        dto.setColorCode(entity.getColorCode());
        dto.setColorName(entity.getColorName());
        dto.setApplyScope(entity.getApplyScope());
        dto.setIsDefault(entity.getIsDefault() != null && entity.getIsDefault() == 1);
        dto.setPriority(entity.getPriority());
        // 优先使用calculated_standard_size，如果为空则使用class_weight_configs表的standard_size
        Integer standardSize = entity.getCalculatedStandardSize();
        if (standardSize == null && entity.getStandardSize() != null) {
            standardSize = entity.getStandardSize();
        }
        dto.setCalculatedStandardSize(standardSize);
        dto.setSortOrder(entity.getSortOrder());
        dto.setRemarks(entity.getRemarks());
        // 优先从JOIN字段获取weightMode和standardSizeMode
        if (entity.getWeightMode() != null) {
            dto.setWeightMode(entity.getWeightMode());
            dto.setWeightModeDesc(getWeightModeDesc(entity.getWeightMode()));
        }
        if (entity.getStandardSizeMode() != null) {
            dto.setStandardSizeMode(entity.getStandardSizeMode());
            dto.setStandardSizeModeDesc(getStandardSizeModeDesc(entity.getStandardSizeMode()));
        }
        // 从JOIN字段获取minWeight和maxWeight
        if (entity.getMinWeight() != null) {
            dto.setMinWeight(entity.getMinWeight());
        }
        if (entity.getMaxWeight() != null) {
            dto.setMaxWeight(entity.getMaxWeight());
        }

        // 解析分类ID列表
        if (entity.getApplyCategoryIds() != null && !entity.getApplyCategoryIds().isEmpty()) {
            try {
                List<Long> categoryIds = objectMapper.readValue(entity.getApplyCategoryIds(),
                        new TypeReference<List<Long>>() {});
                dto.setApplyCategoryIds(categoryIds);

                // 获取分类名称
                List<String> categoryNames = new ArrayList<>();
                for (Long categoryId : categoryIds) {
                    CheckCategory category = checkCategoryMapper.selectById(categoryId);
                    if (category != null) {
                        categoryNames.add(category.getCategoryName());
                    }
                }
                dto.setApplyCategoryNames(categoryNames);
            } catch (JsonProcessingException e) {
                log.error("解析分类ID列表失败", e);
            }
        }

        // 解析扣分项ID列表
        if (entity.getApplyItemIds() != null && !entity.getApplyItemIds().isEmpty()) {
            try {
                List<Long> itemIds = objectMapper.readValue(entity.getApplyItemIds(),
                        new TypeReference<List<Long>>() {});
                dto.setApplyItemIds(itemIds);
            } catch (JsonProcessingException e) {
                log.error("解析扣分项ID列表失败", e);
            }
        }

        // 解析配置快照获取额外信息
        if (entity.getWeightConfigSnapshot() != null && !entity.getWeightConfigSnapshot().isEmpty()) {
            try {
                var snapshot = objectMapper.readTree(entity.getWeightConfigSnapshot());
                if (snapshot.has("weightMode")) {
                    dto.setWeightMode(snapshot.get("weightMode").asText());
                    dto.setWeightModeDesc(getWeightModeDesc(dto.getWeightMode()));
                }
                if (snapshot.has("standardSizeMode")) {
                    dto.setStandardSizeMode(snapshot.get("standardSizeMode").asText());
                    dto.setStandardSizeModeDesc(getStandardSizeModeDesc(dto.getStandardSizeMode()));
                }
                if (snapshot.has("minWeight")) {
                    dto.setMinWeight(new BigDecimal(snapshot.get("minWeight").asText()));
                }
                if (snapshot.has("maxWeight")) {
                    dto.setMaxWeight(new BigDecimal(snapshot.get("maxWeight").asText()));
                }
            } catch (JsonProcessingException e) {
                log.error("解析配置快照失败", e);
            }
        }

        return dto;
    }

    @Override
    public DailyCheckWeightConfig toEntity(DailyCheckWeightConfigDTO dto) {
        if (dto == null) return null;

        DailyCheckWeightConfig entity = new DailyCheckWeightConfig();
        entity.setId(dto.getId());
        entity.setDailyCheckId(dto.getDailyCheckId());
        entity.setWeightConfigId(dto.getWeightConfigId());
        entity.setColorCode(dto.getColorCode());
        entity.setColorName(dto.getColorName());
        entity.setApplyScope(dto.getApplyScope());
        entity.setIsDefault(dto.getIsDefault() != null && dto.getIsDefault() ? 1 : 0);
        entity.setPriority(dto.getPriority());
        entity.setCalculatedStandardSize(dto.getCalculatedStandardSize());
        entity.setSortOrder(dto.getSortOrder());
        entity.setRemarks(dto.getRemarks());

        // 序列化分类ID列表
        if (dto.getApplyCategoryIds() != null && !dto.getApplyCategoryIds().isEmpty()) {
            try {
                entity.setApplyCategoryIds(objectMapper.writeValueAsString(dto.getApplyCategoryIds()));
            } catch (JsonProcessingException e) {
                log.error("序列化分类ID列表失败", e);
            }
        }

        // 序列化扣分项ID列表
        if (dto.getApplyItemIds() != null && !dto.getApplyItemIds().isEmpty()) {
            try {
                entity.setApplyItemIds(objectMapper.writeValueAsString(dto.getApplyItemIds()));
            } catch (JsonProcessingException e) {
                log.error("序列化扣分项ID列表失败", e);
            }
        }

        return entity;
    }

    // ========== 私有辅助方法 ==========

    /**
     * 创建配置快照
     */
    private void createConfigSnapshot(DailyCheckWeightConfig entity) {
        if (entity.getWeightConfigId() == null) return;

        ClassWeightConfig weightConfig = weightConfigMapper.selectById(entity.getWeightConfigId());
        if (weightConfig == null) return;

        try {
            var snapshot = objectMapper.createObjectNode();
            snapshot.put("id", weightConfig.getId());
            snapshot.put("configName", weightConfig.getConfigName());
            snapshot.put("configCode", weightConfig.getConfigCode());
            snapshot.put("weightMode", weightConfig.getWeightMode());
            snapshot.put("standardSizeMode", weightConfig.getStandardSizeMode());
            snapshot.put("standardSize", weightConfig.getStandardSize());
            snapshot.put("minWeight", weightConfig.getMinWeight() != null ? weightConfig.getMinWeight().toString() : null);
            snapshot.put("maxWeight", weightConfig.getMaxWeight() != null ? weightConfig.getMaxWeight().toString() : null);
            snapshot.put("enableWeightLimit", weightConfig.getEnableWeightLimit());

            entity.setWeightConfigSnapshot(objectMapper.writeValueAsString(snapshot));
        } catch (JsonProcessingException e) {
            log.error("创建配置快照失败", e);
        }
    }

    /**
     * 计算标准人数
     */
    private Integer calculateStandardSize(Long dailyCheckId, Long weightConfigId) {
        if (weightConfigId == null) return null;

        ClassWeightConfig weightConfig = weightConfigMapper.selectById(weightConfigId);
        if (weightConfig == null) return null;

        String sizeMode = weightConfig.getStandardSizeMode();
        log.info("计算标准人数: dailyCheckId={}, sizeMode={}", dailyCheckId, sizeMode);

        // FIXED模式：使用配置的固定值
        if ("FIXED".equals(sizeMode)) {
            return weightConfig.getStandardSize();
        }

        // TARGET_AVERAGE模式：根据目标班级计算
        if ("TARGET_AVERAGE".equals(sizeMode)) {
            return calculateTargetAverageSize(dailyCheckId);
        }

        // 默认返回配置值
        return weightConfig.getStandardSize();
    }

    /**
     * 计算目标班级平均人数
     */
    private Integer calculateTargetAverageSize(Long dailyCheckId) {
        try {
            // 获取检查目标班级列表
            LambdaQueryWrapper<DailyCheckTarget> targetWrapper = new LambdaQueryWrapper<>();
            targetWrapper.eq(DailyCheckTarget::getCheckId, dailyCheckId)
                    .eq(DailyCheckTarget::getTargetType, 1);  // 1=班级
            List<DailyCheckTarget> targets = dailyCheckTargetMapper.selectList(targetWrapper);

            if (targets == null || targets.isEmpty()) {
                log.warn("日常检查ID={}没有班级类型的检查目标", dailyCheckId);
                return null;
            }

            int totalStudents = 0;
            int validClassCount = 0;

            for (DailyCheckTarget target : targets) {
                com.school.management.entity.Class classEntity = classMapper.selectById(target.getTargetId());
                if (classEntity != null && classEntity.getStudentCount() != null && classEntity.getStudentCount() > 0) {
                    totalStudents += classEntity.getStudentCount();
                    validClassCount++;
                }
            }

            if (validClassCount == 0) {
                log.warn("日常检查ID={}的目标班级都没有有效的学生数量", dailyCheckId);
                return null;
            }

            int averageSize = Math.round((float) totalStudents / validClassCount);
            log.info("TARGET_AVERAGE计算: dailyCheckId={}, 班级数={}, 总学生数={}, 平均人数={}",
                    dailyCheckId, validClassCount, totalStudents, averageSize);

            return averageSize;
        } catch (Exception e) {
            log.error("计算TARGET_AVERAGE标准人数失败", e);
            return null;
        }
    }

    private String getWeightModeDesc(String weightMode) {
        return switch (weightMode) {
            case "STANDARD" -> "标准人数折算";
            case "PER_CAPITA" -> "人均扣分";
            case "SEGMENT" -> "分段加权";
            case "NONE" -> "不加权";
            default -> "未知模式";
        };
    }

    private String getStandardSizeModeDesc(String mode) {
        return switch (mode) {
            case "FIXED" -> "固定标准人数";
            case "TARGET_AVERAGE" -> "目标班级平均人数";
            case "RANGE_AVERAGE" -> "范围内平均人数";
            case "CUSTOM" -> "自定义规则";
            default -> "未知模式";
        };
    }
}
