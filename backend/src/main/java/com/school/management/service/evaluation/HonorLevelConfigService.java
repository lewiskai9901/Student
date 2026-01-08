package com.school.management.service.evaluation;

import com.baomidou.mybatisplus.extension.service.IService;
import com.school.management.entity.evaluation.HonorLevelConfig;

import java.util.List;

/**
 * 荣誉等级配置服务接口
 *
 * @author Claude
 * @since 2025-11-28
 */
public interface HonorLevelConfigService extends IService<HonorLevelConfig> {

    /**
     * 根据荣誉类型ID获取等级配置列表
     */
    List<HonorLevelConfig> getByHonorTypeId(Long honorTypeId);

    /**
     * 根据荣誉类型ID获取启用的等级配置列表
     */
    List<HonorLevelConfig> getEnabledByHonorTypeId(Long honorTypeId);

    /**
     * 根据级别和名次获取配置
     */
    HonorLevelConfig getByLevelAndRank(Long honorTypeId, String levelCode, String rankCode);

    /**
     * 创建等级配置
     */
    Long createConfig(HonorLevelConfig config);

    /**
     * 更新等级配置
     */
    void updateConfig(HonorLevelConfig config);

    /**
     * 删除等级配置
     */
    void deleteConfig(Long id);

    /**
     * 批量保存荣誉类型的等级配置
     */
    void saveConfigs(Long honorTypeId, List<HonorLevelConfig> configs);
}
