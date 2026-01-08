package com.school.management.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.school.management.entity.CheckRatingConfig;

/**
 * 检查评级配置服务接口
 *
 * @author system
 * @since 3.1.0
 */
public interface CheckRatingConfigService extends IService<CheckRatingConfig> {

    /**
     * 分页查询评级配置列表
     *
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @param configName 配置名称（可选）
     * @param status 状态（可选）
     * @return 分页结果
     */
    IPage<CheckRatingConfig> listConfigs(Integer pageNum, Integer pageSize, String configName, Integer status);

    /**
     * 创建评级配置
     *
     * @param config 配置对象
     * @return 配置ID
     */
    Long createConfig(CheckRatingConfig config);

    /**
     * 更新评级配置
     *
     * @param id 配置ID
     * @param config 配置对象
     * @return 是否成功
     */
    boolean updateConfig(Long id, CheckRatingConfig config);

    /**
     * 删除评级配置
     *
     * @param id 配置ID
     * @return 是否成功
     */
    boolean deleteConfig(Long id);

    /**
     * 设置默认配置
     *
     * @param id 配置ID
     * @return 是否成功
     */
    boolean setDefaultConfig(Long id);

    /**
     * 获取默认配置
     *
     * @return 默认配置
     */
    CheckRatingConfig getDefaultConfig();
}
