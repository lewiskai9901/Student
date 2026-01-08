package com.school.management.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.school.management.entity.CheckRatingConfig;
import com.school.management.exception.BusinessException;
import com.school.management.mapper.CheckRatingConfigMapper;
import com.school.management.service.CheckRatingConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 检查评级配置服务实现
 *
 * @author system
 * @since 3.1.0
 */
@Slf4j
@Service
public class CheckRatingConfigServiceImpl extends ServiceImpl<CheckRatingConfigMapper, CheckRatingConfig>
        implements CheckRatingConfigService {

    @Override
    public IPage<CheckRatingConfig> listConfigs(Integer pageNum, Integer pageSize, String configName, Integer status) {
        log.info("分页查询评级配置列表: pageNum={}, pageSize={}, configName={}, status={}",
                pageNum, pageSize, configName, status);

        Page<CheckRatingConfig> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<CheckRatingConfig> wrapper = new LambdaQueryWrapper<>();

        // 条件查询
        if (configName != null && !configName.trim().isEmpty()) {
            wrapper.like(CheckRatingConfig::getConfigName, configName);
        }
        if (status != null) {
            wrapper.eq(CheckRatingConfig::getStatus, status);
        }

        // 排序：默认配置优先，然后按创建时间倒序
        wrapper.orderByDesc(CheckRatingConfig::getIsDefault)
                .orderByDesc(CheckRatingConfig::getCreatedAt);

        return this.page(page, wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createConfig(CheckRatingConfig config) {
        log.info("创建评级配置: {}", config.getConfigName());

        // 校验必填字段
        if (config.getConfigName() == null || config.getConfigName().trim().isEmpty()) {
            throw new BusinessException("配置名称不能为空");
        }

        // 检查配置名称是否重复
        LambdaQueryWrapper<CheckRatingConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CheckRatingConfig::getConfigName, config.getConfigName());
        Long count = this.count(wrapper);
        if (count > 0) {
            log.warn("配置名称已存在: {}", config.getConfigName());
            throw new BusinessException("配置名称已存在，请使用其他名称");
        }

        // 设置默认值
        if (config.getRatingMethod() == null) {
            config.setRatingMethod(1); // 默认按比例
        }
        if (config.getScoreBasis() == null) {
            config.setScoreBasis(1); // 默认总扣分
        }
        if (config.getStatus() == null) {
            config.setStatus(1); // 默认启用
        }
        if (config.getIsDefault() == null) {
            config.setIsDefault(0); // 默认非默认配置
        }

        // 如果设置为默认配置，需要先取消其他配置的默认状态
        if (config.getIsDefault() != null && config.getIsDefault() == 1) {
            clearDefaultConfig();
        }

        config.setCreatedAt(LocalDateTime.now());
        config.setUpdatedAt(LocalDateTime.now());

        this.save(config);
        log.info("成功创建评级配置: id={}", config.getId());
        return config.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateConfig(Long id, CheckRatingConfig config) {
        log.info("更新评级配置: id={}", id);

        // 检查配置是否存在
        CheckRatingConfig existing = this.getById(id);
        if (existing == null) {
            log.warn("评级配置不存在: id={}", id);
            throw new BusinessException("评级配置不存在");
        }

        // 检查配置名称是否与其他记录重复（排除自身）
        if (config.getConfigName() != null && !config.getConfigName().trim().isEmpty()) {
            LambdaQueryWrapper<CheckRatingConfig> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(CheckRatingConfig::getConfigName, config.getConfigName())
                    .ne(CheckRatingConfig::getId, id);
            Long count = this.count(wrapper);
            if (count > 0) {
                log.warn("配置名称已存在: {}", config.getConfigName());
                throw new BusinessException("配置名称已存在，请使用其他名称");
            }
        }

        // 如果设置为默认配置，需要先取消其他配置的默认状态
        if (config.getIsDefault() != null && config.getIsDefault() == 1) {
            clearDefaultConfig();
        }

        config.setId(id);
        config.setUpdatedAt(LocalDateTime.now());

        boolean result = this.updateById(config);
        log.info("成功更新评级配置: id={}", id);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteConfig(Long id) {
        log.info("删除评级配置: id={}", id);

        // 检查配置是否存在
        CheckRatingConfig config = this.getById(id);
        if (config == null) {
            log.warn("评级配置不存在: id={}", id);
            throw new BusinessException("评级配置不存在");
        }

        // 不允许删除默认配置
        if (config.getIsDefault() != null && config.getIsDefault() == 1) {
            log.warn("不允许删除默认配置: id={}", id);
            throw new BusinessException("不允许删除默认配置，请先设置其他配置为默认");
        }

        boolean result = this.removeById(id);
        log.info("成功删除评级配置: id={}", id);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean setDefaultConfig(Long id) {
        log.info("设置默认配置: id={}", id);

        // 检查配置是否存在
        CheckRatingConfig config = this.getById(id);
        if (config == null) {
            log.warn("评级配置不存在: id={}", id);
            throw new BusinessException("评级配置不存在");
        }

        // 检查配置是否已启用
        if (config.getStatus() != null && config.getStatus() == 0) {
            log.warn("禁用状态的配置不能设为默认: id={}", id);
            throw new BusinessException("禁用状态的配置不能设为默认，请先启用该配置");
        }

        // 先取消所有配置的默认状态
        clearDefaultConfig();

        // 设置指定配置为默认
        config.setIsDefault(1);
        config.setUpdatedAt(LocalDateTime.now());
        boolean result = this.updateById(config);

        log.info("成功设置默认配置: id={}", id);
        return result;
    }

    @Override
    public CheckRatingConfig getDefaultConfig() {
        log.info("获取默认评级配置");

        LambdaQueryWrapper<CheckRatingConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CheckRatingConfig::getIsDefault, 1)
                .eq(CheckRatingConfig::getStatus, 1)
                .last("LIMIT 1");

        CheckRatingConfig config = this.getOne(wrapper);

        if (config == null) {
            log.warn("未找到默认评级配置");
        } else {
            log.info("找到默认评级配置: id={}, name={}", config.getId(), config.getConfigName());
        }

        return config;
    }

    /**
     * 清除所有配置的默认状态
     */
    private void clearDefaultConfig() {
        log.info("清除所有配置的默认状态");

        CheckRatingConfig update = new CheckRatingConfig();
        update.setIsDefault(0);
        update.setUpdatedAt(LocalDateTime.now());

        LambdaQueryWrapper<CheckRatingConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CheckRatingConfig::getIsDefault, 1);

        this.update(update, wrapper);
    }
}
