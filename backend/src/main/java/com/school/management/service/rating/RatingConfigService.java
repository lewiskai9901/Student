package com.school.management.service.rating;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.school.management.dto.rating.*;
import com.school.management.entity.rating.RatingConfig;

import java.util.List;

/**
 * 评级配置服务
 *
 * @author System
 * @since 4.4.0
 */
public interface RatingConfigService extends IService<RatingConfig> {

    /**
     * 创建评级配置
     *
     * @param dto 创建DTO
     * @param userId 创建人ID
     * @return 配置ID
     */
    Long createConfig(RatingConfigCreateDTO dto, Long userId);

    /**
     * 更新评级配置
     *
     * @param dto 更新DTO
     * @param userId 更新人ID
     */
    void updateConfig(RatingConfigUpdateDTO dto, Long userId);

    /**
     * 删除评级配置
     *
     * @param configId 配置ID
     */
    void deleteConfig(Long configId);

    /**
     * 启用/禁用评级配置
     *
     * @param configId 配置ID
     * @param enabled 是否启用
     */
    void toggleEnabled(Long configId, boolean enabled);

    /**
     * 获取评级配置详情
     *
     * @param configId 配置ID
     * @return 配置VO
     */
    RatingConfigVO getConfigDetail(Long configId);

    /**
     * 分页查询评级配置
     *
     * @param query 查询条件
     * @return 分页结果
     */
    Page<RatingConfigVO> getConfigPage(RatingConfigQueryDTO query);

    /**
     * 获取检查计划的所有评级配置
     *
     * @param checkPlanId 检查计划ID
     * @return 配置列表
     */
    List<RatingConfigVO> getConfigsByPlan(Long checkPlanId);
}
