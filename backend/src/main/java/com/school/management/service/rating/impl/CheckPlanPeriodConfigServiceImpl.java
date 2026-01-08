package com.school.management.service.rating.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.school.management.dto.rating.PeriodConfigDTO;
import com.school.management.entity.rating.CheckPlanPeriodConfig;
import com.school.management.mapper.rating.CheckPlanPeriodConfigMapper;
import com.school.management.service.rating.CheckPlanPeriodConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 检查计划周期配置服务实现
 *
 * @author System
 * @since 4.4.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CheckPlanPeriodConfigServiceImpl
        extends ServiceImpl<CheckPlanPeriodConfigMapper, CheckPlanPeriodConfig>
        implements CheckPlanPeriodConfigService {

    @Override
    public CheckPlanPeriodConfig getOrCreateConfig(Long checkPlanId) {
        LambdaQueryWrapper<CheckPlanPeriodConfig> query = new LambdaQueryWrapper<>();
        query.eq(CheckPlanPeriodConfig::getCheckPlanId, checkPlanId);

        CheckPlanPeriodConfig config = this.getOne(query);
        if (config == null) {
            // 创建默认配置（周一为起始日）
            config = new CheckPlanPeriodConfig();
            config.setCheckPlanId(checkPlanId);
            config.setWeekStartDay(1); // 默认周一
            this.save(config);
            log.info("创建检查计划周期配置: checkPlanId={}", checkPlanId);
        }

        return config;
    }

    @Override
    @Transactional
    public void updateConfig(PeriodConfigDTO dto) {
        CheckPlanPeriodConfig config = getOrCreateConfig(dto.getCheckPlanId());
        config.setWeekStartDay(dto.getWeekStartDay());
        this.updateById(config);
        log.info("更新检查计划周期配置: checkPlanId={}, weekStartDay={}",
                dto.getCheckPlanId(), dto.getWeekStartDay());
    }

    @Override
    public int getWeekStartDay(Long checkPlanId) {
        CheckPlanPeriodConfig config = getOrCreateConfig(checkPlanId);
        return config.getWeekStartDay();
    }
}
