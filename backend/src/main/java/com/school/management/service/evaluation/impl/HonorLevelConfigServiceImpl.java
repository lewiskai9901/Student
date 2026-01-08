package com.school.management.service.evaluation.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.school.management.entity.evaluation.HonorLevelConfig;
import com.school.management.exception.BusinessException;
import com.school.management.mapper.evaluation.HonorLevelConfigMapper;
import com.school.management.service.evaluation.HonorLevelConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 荣誉等级配置服务实现类
 *
 * @author Claude
 * @since 2025-11-28
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HonorLevelConfigServiceImpl extends ServiceImpl<HonorLevelConfigMapper, HonorLevelConfig>
        implements HonorLevelConfigService {

    private final HonorLevelConfigMapper configMapper;

    @Override
    public List<HonorLevelConfig> getByHonorTypeId(Long honorTypeId) {
        return configMapper.selectByHonorTypeId(honorTypeId);
    }

    @Override
    public List<HonorLevelConfig> getEnabledByHonorTypeId(Long honorTypeId) {
        return configMapper.selectEnabledByHonorTypeId(honorTypeId);
    }

    @Override
    public HonorLevelConfig getByLevelAndRank(Long honorTypeId, String levelCode, String rankCode) {
        return configMapper.selectByLevelAndRank(honorTypeId, levelCode, rankCode);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createConfig(HonorLevelConfig config) {
        // 检查是否已存在相同级别和名次的配置
        HonorLevelConfig existing = getByLevelAndRank(config.getHonorTypeId(),
                config.getLevelCode(), config.getRankCode());
        if (existing != null) {
            throw new BusinessException("该级别和名次的配置已存在");
        }

        // 设置默认值
        if (config.getStatus() == null) {
            config.setStatus(1);
        }
        if (config.getPriority() == null) {
            config.setPriority(0);
        }
        if (config.getSortOrder() == null) {
            config.setSortOrder(0);
        }

        configMapper.insert(config);
        log.info("创建荣誉等级配置: id={}, honorTypeId={}, level={}, rank={}",
                config.getId(), config.getHonorTypeId(), config.getLevelCode(), config.getRankCode());
        return config.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateConfig(HonorLevelConfig config) {
        HonorLevelConfig existing = configMapper.selectById(config.getId());
        if (existing == null) {
            throw new BusinessException("等级配置不存在");
        }

        // 如果修改了级别或名次，检查是否冲突
        if ((config.getLevelCode() != null && !config.getLevelCode().equals(existing.getLevelCode())) ||
                (config.getRankCode() != null && !config.getRankCode().equals(existing.getRankCode()))) {
            String levelCode = config.getLevelCode() != null ? config.getLevelCode() : existing.getLevelCode();
            String rankCode = config.getRankCode() != null ? config.getRankCode() : existing.getRankCode();
            HonorLevelConfig duplicate = getByLevelAndRank(existing.getHonorTypeId(), levelCode, rankCode);
            if (duplicate != null && !duplicate.getId().equals(config.getId())) {
                throw new BusinessException("该级别和名次的配置已存在");
            }
        }

        configMapper.updateById(config);
        log.info("更新荣誉等级配置: id={}", config.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteConfig(Long id) {
        HonorLevelConfig existing = configMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("等级配置不存在");
        }

        configMapper.deleteById(id);
        log.info("删除荣誉等级配置: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveConfigs(Long honorTypeId, List<HonorLevelConfig> configs) {
        // 删除原有配置
        configMapper.deleteByHonorTypeId(honorTypeId);

        // 保存新配置
        if (configs != null && !configs.isEmpty()) {
            for (HonorLevelConfig config : configs) {
                config.setId(null);
                config.setHonorTypeId(honorTypeId);
                if (config.getStatus() == null) {
                    config.setStatus(1);
                }
                if (config.getPriority() == null) {
                    config.setPriority(0);
                }
                if (config.getSortOrder() == null) {
                    config.setSortOrder(0);
                }
                configMapper.insert(config);
            }
        }

        log.info("批量保存荣誉等级配置: honorTypeId={}, count={}", honorTypeId,
                configs != null ? configs.size() : 0);
    }
}
