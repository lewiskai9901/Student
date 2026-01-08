package com.school.management.service;

import com.school.management.dto.DailyCheckWeightConfigDTO;
import com.school.management.entity.DailyCheckWeightConfig;

import java.util.List;

/**
 * 日常检查加权配置服务接口
 *
 * 支持一次检查关联多个加权配置，每个配置可应用于特定分类或扣分项
 *
 * @author system
 * @since 1.0.0
 */
public interface DailyCheckWeightConfigService {

    /**
     * 获取日常检查的所有加权配置
     *
     * @param dailyCheckId 日常检查ID
     * @return 加权配置列表
     */
    List<DailyCheckWeightConfigDTO> getConfigsByDailyCheckId(Long dailyCheckId);

    /**
     * 根据分类ID获取适用的加权配置
     * 优先级：ITEM级别配置 > CATEGORY级别配置 > 默认配置
     *
     * @param dailyCheckId 日常检查ID
     * @param categoryId 分类ID
     * @param itemId 扣分项ID（可选）
     * @return 加权配置DTO
     */
    DailyCheckWeightConfigDTO getApplicableConfig(Long dailyCheckId, Long categoryId, Long itemId);

    /**
     * 保存日常检查的加权配置列表
     * 会先删除原有配置，再保存新配置
     *
     * @param dailyCheckId 日常检查ID
     * @param configs 加权配置列表
     * @return 保存后的配置列表
     */
    List<DailyCheckWeightConfigDTO> saveConfigs(Long dailyCheckId, List<DailyCheckWeightConfigDTO> configs);

    /**
     * 添加单个加权配置
     *
     * @param config 加权配置DTO
     * @return 保存后的配置
     */
    DailyCheckWeightConfigDTO addConfig(DailyCheckWeightConfigDTO config);

    /**
     * 更新加权配置
     *
     * @param config 加权配置DTO
     * @return 更新后的配置
     */
    DailyCheckWeightConfigDTO updateConfig(DailyCheckWeightConfigDTO config);

    /**
     * 删除加权配置
     *
     * @param configId 配置ID
     * @return 是否删除成功
     */
    boolean deleteConfig(Long configId);

    /**
     * 删除日常检查的所有加权配置
     *
     * @param dailyCheckId 日常检查ID
     * @return 删除的配置数量
     */
    int deleteByDailyCheckId(Long dailyCheckId);

    /**
     * 计算并更新配置的标准人数
     * 根据标准人数模式（TARGET_AVERAGE等）计算实际标准人数
     *
     * @param configId 配置ID
     * @return 计算后的标准人数
     */
    Integer calculateAndUpdateStandardSize(Long configId);

    /**
     * 获取预定义颜色方案列表
     *
     * @return 颜色方案列表
     */
    List<DailyCheckWeightConfigDTO.ColorScheme> getPredefinedColors();

    /**
     * 将实体转换为DTO
     *
     * @param entity 实体
     * @return DTO
     */
    DailyCheckWeightConfigDTO toDTO(DailyCheckWeightConfig entity);

    /**
     * 将DTO转换为实体
     *
     * @param dto DTO
     * @return 实体
     */
    DailyCheckWeightConfig toEntity(DailyCheckWeightConfigDTO dto);
}
