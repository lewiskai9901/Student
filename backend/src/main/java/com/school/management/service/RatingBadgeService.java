package com.school.management.service;

import com.school.management.dto.rating.badge.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 荣誉徽章服务接口
 *
 * 提供徽章配置、自动授予、证书生成等功能
 *
 * @author Claude Code
 * @since 2025-12-22
 */
public interface RatingBadgeService {

    /**
     * 创建徽章
     *
     * @param dto 徽章创建DTO
     * @return 徽章ID
     */
    Long createBadge(RatingBadgeCreateDTO dto);

    /**
     * 更新徽章
     *
     * @param badgeId 徽章ID
     * @param dto     更新DTO
     */
    void updateBadge(Long badgeId, RatingBadgeCreateDTO dto);

    /**
     * 删除徽章
     *
     * @param badgeId 徽章ID
     */
    void deleteBadge(Long badgeId);

    /**
     * 获取徽章详情
     *
     * @param badgeId 徽章ID
     * @return 徽章VO
     */
    RatingBadgeVO getBadgeDetail(Long badgeId);

    /**
     * 获取检查计划的徽章列表
     *
     * @param checkPlanId 检查计划ID
     * @return 徽章列表
     */
    List<RatingBadgeVO> getBadgesByPlan(Long checkPlanId);

    /**
     * 切换徽章启用状态
     *
     * @param badgeId 徽章ID
     * @param enabled 是否启用
     */
    void toggleBadgeEnabled(Long badgeId, Boolean enabled);

    /**
     * 检查并自动授予徽章（核心方法）
     *
     * 遍历所有启用的自动授予徽章，检查符合条件的班级并授予
     *
     * @param checkPlanId 检查计划ID
     * @param periodStart 统计周期开始
     * @param periodEnd   统计周期结束
     * @return 授予结果列表
     */
    List<BadgeGrantResultVO> checkAndGrantBadges(
            Long checkPlanId,
            LocalDate periodStart,
            LocalDate periodEnd
    );

    /**
     * 手动授予徽章
     *
     * @param request 授予请求
     */
    void grantBadge(BadgeGrantRequest request);

    /**
     * 撤销徽章
     *
     * @param recordId 徽章记录ID
     * @param reason   撤销原因
     */
    void revokeBadge(Long recordId, String reason);

    /**
     * 获取班级的徽章记录
     *
     * @param classId 班级ID
     * @return 徽章记录列表
     */
    List<ClassBadgeRecordVO> getClassBadgeRecords(Long classId);

    /**
     * 获取当前符合条件的班级数量
     *
     * 用于徽章配置页面展示"当前符合X个班级"
     *
     * @param badgeId     徽章ID
     * @param periodStart 统计周期开始
     * @param periodEnd   统计周期结束
     * @return 符合条件的班级数量
     */
    Integer getQualifiedClassCount(Long badgeId, LocalDate periodStart, LocalDate periodEnd);

    /**
     * 获取符合条件的班级列表
     *
     * @param badgeId     徽章ID
     * @param periodStart 统计周期开始
     * @param periodEnd   统计周期结束
     * @return 班级ID列表
     */
    List<Long> getQualifiedClassIds(Long badgeId, LocalDate periodStart, LocalDate periodEnd);
}
