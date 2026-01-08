package com.school.management.service.rating;

import com.baomidou.mybatisplus.extension.service.IService;
import com.school.management.dto.rating.PeriodConfigDTO;
import com.school.management.entity.rating.CheckPlanPeriodConfig;

/**
 * 检查计划周期配置服务
 *
 * @author System
 * @since 4.4.0
 */
public interface CheckPlanPeriodConfigService extends IService<CheckPlanPeriodConfig> {

    /**
     * 获取或创建周期配置
     *
     * @param checkPlanId 检查计划ID
     * @return 周期配置
     */
    CheckPlanPeriodConfig getOrCreateConfig(Long checkPlanId);

    /**
     * 更新周期配置
     *
     * @param dto 配置DTO
     */
    void updateConfig(PeriodConfigDTO dto);

    /**
     * 获取周起始日
     *
     * @param checkPlanId 检查计划ID
     * @return 周起始日（1-7）
     */
    int getWeekStartDay(Long checkPlanId);
}
