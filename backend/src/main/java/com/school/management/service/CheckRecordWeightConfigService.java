package com.school.management.service;

import java.util.List;
import java.util.Map;

/**
 * 检查记录加权配置服务接口 (V3.0)
 *
 * @author Claude
 * @since 2025-11-24
 */
public interface CheckRecordWeightConfigService {

    /**
     * 获取检查记录的配置树（3级结构）
     *
     * @param recordId 记录ID
     * @return 配置树
     */
    List<Map<String, Object>> getConfigTree(Long recordId);

    /**
     * 获取生效的加权配置（处理3级继承逻辑）
     *
     * @param recordId 记录ID
     * @param categoryId 类别ID（可选）
     * @param itemId 检查项ID（可选）
     * @return 生效的配置
     */
    Map<String, Object> getEffectiveConfig(Long recordId, Long categoryId, Long itemId);

    /**
     * 设置记录级别加权配置
     *
     * @param recordId 记录ID
     * @param weightConfigId 加权配置ID
     * @param weightEnabled 是否启用加权
     */
    void setRecordLevelConfig(Long recordId, Long weightConfigId, Integer weightEnabled);

    /**
     * 设置类别级别加权配置
     *
     * @param recordId 记录ID
     * @param categoryId 类别ID
     * @param weightConfigId 加权配置ID（null表示继承上级）
     * @param isInherited 是否继承（1=继承, 0=覆盖）
     * @param weightEnabled 是否启用加权
     */
    void setCategoryLevelConfig(Long recordId, Long categoryId, Long weightConfigId,
                                Integer isInherited, Integer weightEnabled);

    /**
     * 设置检查项级别加权配置
     *
     * @param recordId 记录ID
     * @param categoryId 类别ID
     * @param itemId 检查项ID
     * @param weightConfigId 加权配置ID（null表示继承上级）
     * @param isInherited 是否继承（1=继承, 0=覆盖）
     * @param weightEnabled 是否启用加权
     */
    void setItemLevelConfig(Long recordId, Long categoryId, Long itemId, Long weightConfigId,
                            Integer isInherited, Integer weightEnabled);

    /**
     * 批量设置类别级别配置
     *
     * @param recordId 记录ID
     * @param configs 配置列表
     */
    void batchSetCategoryConfigs(Long recordId, List<Map<String, Object>> configs);

    /**
     * 批量设置检查项级别配置
     *
     * @param recordId 记录ID
     * @param configs 配置列表
     */
    void batchSetItemConfigs(Long recordId, List<Map<String, Object>> configs);

    /**
     * 重置为记录级别配置（删除所有类别和检查项级别的覆盖配置）
     *
     * @param recordId 记录ID
     */
    void resetToRecordLevel(Long recordId);

    /**
     * 初始化检查记录的加权配置（从模板复制）
     *
     * @param recordId 记录ID
     * @param templateId 模板ID
     */
    void initializeConfigsFromTemplate(Long recordId, Long templateId);

    /**
     * 生成加权配置快照（用于历史记录）
     *
     * @param recordId 记录ID
     * @return 配置快照
     */
    Map<String, Object> generateConfigSnapshot(Long recordId);
}
