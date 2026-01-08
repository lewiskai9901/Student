package com.school.management.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.school.management.common.result.ResultCode;
import com.school.management.entity.CheckRecordWeightConfig;
import com.school.management.exception.BusinessException;
import com.school.management.mapper.CheckRecordWeightConfigMapper;
import com.school.management.mapper.ClassWeightConfigMapper;
import com.school.management.service.CheckRecordWeightConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 检查记录加权配置服务实现类 (V3.0)
 *
 * @author Claude
 * @since 2025-11-24
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CheckRecordWeightConfigServiceImpl extends ServiceImpl<CheckRecordWeightConfigMapper, CheckRecordWeightConfig>
        implements CheckRecordWeightConfigService {

    private final CheckRecordWeightConfigMapper weightConfigMapper;
    private final ClassWeightConfigMapper classWeightConfigMapper;

    @Override
    public List<Map<String, Object>> getConfigTree(Long recordId) {
        log.info("获取配置树: recordId={}", recordId);

        return weightConfigMapper.selectConfigTreeByRecordId(recordId);
    }

    @Override
    public Map<String, Object> getEffectiveConfig(Long recordId, Long categoryId, Long itemId) {
        log.info("获取生效的加权配置: recordId={}, categoryId={}, itemId={}", recordId, categoryId, itemId);

        Map<String, Object> effectiveConfig = weightConfigMapper.selectEffectiveConfig(recordId, categoryId, itemId);

        if (effectiveConfig == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "未找到生效的加权配置");
        }

        return effectiveConfig;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setRecordLevelConfig(Long recordId, Long weightConfigId, Integer weightEnabled) {
        log.info("设置记录级别加权配置: recordId={}, weightConfigId={}, weightEnabled={}",
                recordId, weightConfigId, weightEnabled);

        // 查询是否已存在记录级别配置
        CheckRecordWeightConfig existingConfig = weightConfigMapper.selectRecordLevelConfig(recordId);

        if (existingConfig != null) {
            // 更新现有配置
            existingConfig.setWeightConfigId(weightConfigId);
            existingConfig.setWeightEnabled(weightEnabled);
            weightConfigMapper.updateById(existingConfig);
        } else {
            // 创建新配置
            CheckRecordWeightConfig config = new CheckRecordWeightConfig();
            config.setRecordId(recordId);
            config.setConfigLevel("RECORD");
            config.setTargetName("检查记录");
            config.setWeightConfigId(weightConfigId);
            config.setIsInherited(0); // 记录级别不继承
            config.setWeightEnabled(weightEnabled);
            weightConfigMapper.insert(config);
        }

        log.info("记录级别加权配置设置成功");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setCategoryLevelConfig(Long recordId, Long categoryId, Long weightConfigId,
                                       Integer isInherited, Integer weightEnabled) {
        log.info("设置类别级别加权配置: recordId={}, categoryId={}, weightConfigId={}, isInherited={}, weightEnabled={}",
                recordId, categoryId, weightConfigId, isInherited, weightEnabled);

        // 查询记录级别配置（作为parent）
        CheckRecordWeightConfig recordConfig = weightConfigMapper.selectRecordLevelConfig(recordId);
        if (recordConfig == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "请先设置记录级别配置");
        }

        // 创建类别级别配置
        CheckRecordWeightConfig config = new CheckRecordWeightConfig();
        config.setRecordId(recordId);
        config.setConfigLevel("CATEGORY");
        config.setParentId(recordConfig.getId());
        config.setCategoryId(categoryId);
        config.setTargetName("类别-" + categoryId);
        config.setWeightConfigId(weightConfigId);
        config.setIsInherited(isInherited);
        config.setWeightEnabled(weightEnabled);

        weightConfigMapper.insert(config);

        log.info("类别级别加权配置设置成功");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setItemLevelConfig(Long recordId, Long categoryId, Long itemId, Long weightConfigId,
                                    Integer isInherited, Integer weightEnabled) {
        log.info("设置检查项级别加权配置: recordId={}, categoryId={}, itemId={}, weightConfigId={}, isInherited={}, weightEnabled={}",
                recordId, categoryId, itemId, weightConfigId, isInherited, weightEnabled);

        // 查询类别级别配置（作为parent）
        List<CheckRecordWeightConfig> categoryConfigs = weightConfigMapper.selectByRecordIdAndLevel(recordId, "CATEGORY");
        CheckRecordWeightConfig categoryConfig = categoryConfigs.stream()
                .filter(c -> c.getCategoryId().equals(categoryId))
                .findFirst()
                .orElse(null);

        if (categoryConfig == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "请先设置类别级别配置");
        }

        // 创建检查项级别配置
        CheckRecordWeightConfig config = new CheckRecordWeightConfig();
        config.setRecordId(recordId);
        config.setConfigLevel("ITEM");
        config.setParentId(categoryConfig.getId());
        config.setCategoryId(categoryId);
        config.setItemId(itemId);
        config.setTargetName("检查项-" + itemId);
        config.setWeightConfigId(weightConfigId);
        config.setIsInherited(isInherited);
        config.setWeightEnabled(weightEnabled);

        weightConfigMapper.insert(config);

        log.info("检查项级别加权配置设置成功");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchSetCategoryConfigs(Long recordId, List<Map<String, Object>> configs) {
        log.info("批量设置类别级别配置: recordId={}, count={}", recordId, configs.size());

        for (Map<String, Object> configMap : configs) {
            Long categoryId = ((Number) configMap.get("categoryId")).longValue();
            Long weightConfigId = configMap.get("weightConfigId") != null ?
                    ((Number) configMap.get("weightConfigId")).longValue() : null;
            Integer isInherited = (Integer) configMap.get("isInherited");
            Integer weightEnabled = (Integer) configMap.get("weightEnabled");

            setCategoryLevelConfig(recordId, categoryId, weightConfigId, isInherited, weightEnabled);
        }

        log.info("批量设置类别级别配置成功");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchSetItemConfigs(Long recordId, List<Map<String, Object>> configs) {
        log.info("批量设置检查项级别配置: recordId={}, count={}", recordId, configs.size());

        for (Map<String, Object> configMap : configs) {
            Long categoryId = ((Number) configMap.get("categoryId")).longValue();
            Long itemId = ((Number) configMap.get("itemId")).longValue();
            Long weightConfigId = configMap.get("weightConfigId") != null ?
                    ((Number) configMap.get("weightConfigId")).longValue() : null;
            Integer isInherited = (Integer) configMap.get("isInherited");
            Integer weightEnabled = (Integer) configMap.get("weightEnabled");

            setItemLevelConfig(recordId, categoryId, itemId, weightConfigId, isInherited, weightEnabled);
        }

        log.info("批量设置检查项级别配置成功");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetToRecordLevel(Long recordId) {
        log.info("重置为记录级别配置: recordId={}", recordId);

        // 删除所有类别和检查项级别的配置
        weightConfigMapper.deleteByRecordId(recordId);

        log.info("重置为记录级别配置成功");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void initializeConfigsFromTemplate(Long recordId, Long templateId) {
        log.info("从模板初始化加权配置: recordId={}, templateId={}", recordId, templateId);

        // 1. 先删除目标记录的现有配置
        weightConfigMapper.deleteByRecordId(recordId);

        // 2. 查询模板(源记录)的所有加权配置
        List<CheckRecordWeightConfig> templateConfigs = new ArrayList<>();

        // 获取记录级别配置
        CheckRecordWeightConfig recordLevelConfig = weightConfigMapper.selectRecordLevelConfig(templateId);
        if (recordLevelConfig != null) {
            templateConfigs.add(recordLevelConfig);
        }

        // 获取类别级别配置
        List<CheckRecordWeightConfig> categoryConfigs = weightConfigMapper.selectCategoryLevelConfigs(templateId);
        if (categoryConfigs != null) {
            templateConfigs.addAll(categoryConfigs);
        }

        // 获取检查项级别配置
        List<CheckRecordWeightConfig> itemConfigs = weightConfigMapper.selectItemLevelConfigs(templateId, null);
        if (itemConfigs != null) {
            templateConfigs.addAll(itemConfigs);
        }

        if (templateConfigs.isEmpty()) {
            log.warn("模板记录没有加权配置: templateId={}", templateId);
            return;
        }

        // 3. 复制配置到新记录，需要处理ID映射
        Map<Long, Long> oldToNewIdMap = new HashMap<>();
        List<CheckRecordWeightConfig> newConfigs = new ArrayList<>();

        // 按层级顺序处理: RECORD -> CATEGORY -> ITEM
        for (CheckRecordWeightConfig oldConfig : templateConfigs) {
            if ("RECORD".equals(oldConfig.getConfigLevel())) {
                CheckRecordWeightConfig newConfig = copyConfig(oldConfig, recordId);
                newConfig.setParentId(null); // 记录级别没有父级
                weightConfigMapper.insert(newConfig);
                oldToNewIdMap.put(oldConfig.getId(), newConfig.getId());
            }
        }

        for (CheckRecordWeightConfig oldConfig : templateConfigs) {
            if ("CATEGORY".equals(oldConfig.getConfigLevel())) {
                CheckRecordWeightConfig newConfig = copyConfig(oldConfig, recordId);
                // 设置新的父级ID
                if (oldConfig.getParentId() != null && oldToNewIdMap.containsKey(oldConfig.getParentId())) {
                    newConfig.setParentId(oldToNewIdMap.get(oldConfig.getParentId()));
                }
                weightConfigMapper.insert(newConfig);
                oldToNewIdMap.put(oldConfig.getId(), newConfig.getId());
            }
        }

        for (CheckRecordWeightConfig oldConfig : templateConfigs) {
            if ("ITEM".equals(oldConfig.getConfigLevel())) {
                CheckRecordWeightConfig newConfig = copyConfig(oldConfig, recordId);
                // 设置新的父级ID
                if (oldConfig.getParentId() != null && oldToNewIdMap.containsKey(oldConfig.getParentId())) {
                    newConfig.setParentId(oldToNewIdMap.get(oldConfig.getParentId()));
                }
                weightConfigMapper.insert(newConfig);
                oldToNewIdMap.put(oldConfig.getId(), newConfig.getId());
            }
        }

        log.info("从模板初始化加权配置成功: 复制了{}条配置", templateConfigs.size());
    }

    /**
     * 复制配置对象
     */
    private CheckRecordWeightConfig copyConfig(CheckRecordWeightConfig source, Long newRecordId) {
        CheckRecordWeightConfig target = new CheckRecordWeightConfig();
        target.setRecordId(newRecordId);
        target.setConfigLevel(source.getConfigLevel());
        target.setCategoryId(source.getCategoryId());
        target.setItemId(source.getItemId());
        target.setTargetName(source.getTargetName());
        target.setWeightConfigId(source.getWeightConfigId());
        target.setIsInherited(source.getIsInherited());
        target.setWeightEnabled(source.getWeightEnabled());
        return target;
    }

    @Override
    public Map<String, Object> generateConfigSnapshot(Long recordId) {
        log.info("生成加权配置快照: recordId={}", recordId);

        List<Map<String, Object>> configTree = getConfigTree(recordId);

        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("recordId", recordId);
        snapshot.put("configTree", configTree);
        snapshot.put("snapshotTime", System.currentTimeMillis());

        return snapshot;
    }
}
