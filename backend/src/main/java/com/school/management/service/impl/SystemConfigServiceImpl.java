package com.school.management.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.school.management.entity.SystemConfig;
import com.school.management.exception.BusinessException;
import com.school.management.mapper.SystemConfigMapper;
import com.school.management.service.SystemConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * 系统配置服务实现类
 *
 * @author Claude Code
 * @date 2025-11-18
 */
@Slf4j
@Service
public class SystemConfigServiceImpl extends ServiceImpl<SystemConfigMapper, SystemConfig> implements SystemConfigService {

    private static final String CACHE_NAME = "systemConfig";

    @Override
    public IPage<SystemConfig> queryPage(String configKey, String configGroup, Integer status, int pageNum, int pageSize) {
        Page<SystemConfig> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SystemConfig> wrapper = new LambdaQueryWrapper<>();

        // 构建查询条件
        wrapper.like(StringUtils.hasText(configKey), SystemConfig::getConfigKey, configKey)
                .eq(StringUtils.hasText(configGroup), SystemConfig::getConfigGroup, configGroup)
                .eq(status != null, SystemConfig::getStatus, status)
                .eq(SystemConfig::getDeleted, 0)
                .orderByAsc(SystemConfig::getConfigGroup, SystemConfig::getSortOrder);

        return this.page(page, wrapper);
    }

    @Override
    @Cacheable(value = CACHE_NAME, key = "'group:' + #configGroup")
    public List<SystemConfig> getByGroup(String configGroup) {
        LambdaQueryWrapper<SystemConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemConfig::getConfigGroup, configGroup)
                .eq(SystemConfig::getStatus, 1)
                .eq(SystemConfig::getDeleted, 0)
                .orderByAsc(SystemConfig::getSortOrder);
        return this.list(wrapper);
    }

    @Override
    @Cacheable(value = CACHE_NAME, key = "'obj:' + #configKey")
    public SystemConfig getByKey(String configKey) {
        LambdaQueryWrapper<SystemConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemConfig::getConfigKey, configKey)
                .eq(SystemConfig::getStatus, 1)
                .eq(SystemConfig::getDeleted, 0)
                .last("LIMIT 1");

        return this.getOne(wrapper);
    }

    @Override
    @Cacheable(value = CACHE_NAME, key = "'key:' + #configKey")
    public String getConfigValue(String configKey) {
        SystemConfig config = getByKey(configKey);
        return config != null ? config.getConfigValue() : null;
    }

    @Override
    public String getConfigValue(String configKey, String defaultValue) {
        String value = getConfigValue(configKey);
        return value != null ? value : defaultValue;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public boolean batchUpdate(Map<String, String> configs) {
        try {
            for (Map.Entry<String, String> entry : configs.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();

                LambdaQueryWrapper<SystemConfig> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(SystemConfig::getConfigKey, key);

                SystemConfig config = this.getOne(wrapper);
                if (config != null) {
                    // 更新现有配置
                    config.setConfigValue(value);
                    this.updateById(config);
                } else {
                    // 配置不存在，创建新配置
                    log.info("配置键不存在，创建新配置: {}", key);
                    SystemConfig newConfig = new SystemConfig();
                    newConfig.setConfigKey(key);
                    newConfig.setConfigValue(value);
                    // 根据 key 前缀自动设置 configGroup
                    String group = inferConfigGroup(key);
                    newConfig.setConfigGroup(group);
                    newConfig.setConfigType("string");
                    newConfig.setConfigLabel(key);
                    newConfig.setIsSystem(0);
                    newConfig.setStatus(1);
                    newConfig.setDeleted(0);
                    this.save(newConfig);
                }
            }
            return true;
        } catch (Exception e) {
            log.error("批量更新配置失败", e);
            throw new BusinessException("批量更新配置失败");
        }
    }

    /**
     * 根据配置键推断配置组
     */
    private String inferConfigGroup(String configKey) {
        if (configKey.startsWith("system.")) {
            return "system";
        } else if (configKey.startsWith("business.")) {
            return "business";
        } else if (configKey.startsWith("ui.")) {
            return "ui";
        } else {
            return "other";
        }
    }

    @Override
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public void refreshCache() {
        log.info("刷新系统配置缓存");
    }
}
