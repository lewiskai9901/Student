package com.school.management.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.school.management.entity.SystemConfig;

import java.util.List;
import java.util.Map;

/**
 * 系统配置服务接口
 *
 * @author Claude Code
 * @date 2025-11-18
 */
public interface SystemConfigService extends IService<SystemConfig> {

    /**
     * 分页查询系统配置
     *
     * @param configKey 配置键
     * @param configGroup 配置分组
     * @param status 状态
     * @param pageNum 页码
     * @param pageSize 页大小
     * @return 分页结果
     */
    IPage<SystemConfig> queryPage(String configKey, String configGroup, Integer status, int pageNum, int pageSize);

    /**
     * 根据配置组查询配置列表
     *
     * @param configGroup 配置分组
     * @return 配置列表
     */
    List<SystemConfig> getByGroup(String configGroup);

    /**
     * 根据配置键获取配置对象
     *
     * @param configKey 配置键
     * @return 配置对象
     */
    SystemConfig getByKey(String configKey);

    /**
     * 根据配置键获取配置值
     *
     * @param configKey 配置键
     * @return 配置值
     */
    String getConfigValue(String configKey);

    /**
     * 根据配置键获取配置值(带默认值)
     *
     * @param configKey 配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    String getConfigValue(String configKey, String defaultValue);

    /**
     * 批量更新配置
     *
     * @param configs 配置映射(key->value)
     * @return 是否成功
     */
    boolean batchUpdate(Map<String, String> configs);

    /**
     * 刷新配置缓存
     */
    void refreshCache();
}
