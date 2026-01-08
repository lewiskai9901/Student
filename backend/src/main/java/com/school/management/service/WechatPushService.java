package com.school.management.service;

import com.school.management.entity.Announcement;

import java.util.List;
import java.util.Map;

/**
 * 微信推送服务接口
 *
 * @author system
 * @since 4.5.0
 */
public interface WechatPushService {

    /**
     * 推送公告
     *
     * @param announcement 公告
     * @return 推送结果统计 (success: 成功数, failed: 失败数, total: 总数)
     */
    Map<String, Integer> pushAnnouncement(Announcement announcement);

    /**
     * 推送通报
     *
     * @param notificationId 通报ID
     * @param title          通报标题
     * @param className      班级名称
     * @param checkDate      检查日期
     * @param targetRoleIds  目标角色ID列表
     * @return 推送结果统计
     */
    Map<String, Integer> pushNotification(Long notificationId, String title, String className,
                                          String checkDate, List<Long> targetRoleIds);

    /**
     * 推送给指定用户
     *
     * @param businessType 业务类型 (ANNOUNCEMENT/NOTIFICATION)
     * @param businessId   业务ID
     * @param userIds      用户ID列表
     * @param title        标题
     * @param content      内容
     * @param url          跳转链接
     * @return 推送结果统计
     */
    Map<String, Integer> pushToUsers(String businessType, Long businessId,
                                     List<Long> userIds, String title, String content, String url);

    /**
     * 推送给指定角色的用户
     *
     * @param businessType 业务类型
     * @param businessId   业务ID
     * @param roleIds      角色ID列表
     * @param title        标题
     * @param content      内容
     * @param url          跳转链接
     * @return 推送结果统计
     */
    Map<String, Integer> pushToRoles(String businessType, Long businessId,
                                     List<Long> roleIds, String title, String content, String url);

    /**
     * 推送给所有用户
     *
     * @param businessType 业务类型
     * @param businessId   业务ID
     * @param title        标题
     * @param content      内容
     * @param url          跳转链接
     * @return 推送结果统计
     */
    Map<String, Integer> pushToAll(String businessType, Long businessId,
                                   String title, String content, String url);

    /**
     * 检查推送功能是否可用
     *
     * @return true-可用, false-不可用
     */
    boolean isPushEnabled();

    /**
     * 获取推送统计
     *
     * @param businessType 业务类型
     * @param businessId   业务ID
     * @return 推送统计 (total, success, failed, pending)
     */
    Map<String, Integer> getPushStatistics(String businessType, Long businessId);
}
