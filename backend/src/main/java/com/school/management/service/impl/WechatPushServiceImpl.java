package com.school.management.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.dto.wechat.TemplateMessageDTO;
import com.school.management.entity.Announcement;
import com.school.management.entity.User;
import com.school.management.entity.WechatPushRecord;
import com.school.management.mapper.UserMapper;
import com.school.management.mapper.WechatPushRecordMapper;
import com.school.management.service.WechatConfigService;
import com.school.management.service.WechatPushService;
import com.school.management.service.WechatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 微信推送服务实现
 *
 * @author system
 * @since 4.5.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WechatPushServiceImpl implements WechatPushService {

    private final WechatConfigService wechatConfigService;
    private final WechatService wechatService;
    private final WechatPushRecordMapper pushRecordMapper;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Override
    @Transactional
    public Map<String, Integer> pushAnnouncement(Announcement announcement) {
        if (!isPushEnabled()) {
            log.warn("微信推送功能未启用");
            return Map.of("total", 0, "success", 0, "failed", 0, "skipped", 0);
        }

        String templateId = wechatConfigService.getAnnouncementTemplateId();
        if (!StringUtils.hasText(templateId)) {
            log.warn("公告模板ID未配置");
            return Map.of("total", 0, "success", 0, "failed", 0, "skipped", 0);
        }

        // 获取目标用户
        List<User> targetUsers = getTargetUsers(announcement.getTargetType(), announcement.getTargetIds());

        if (targetUsers.isEmpty()) {
            log.info("没有需要推送的目标用户: announcementId={}", announcement.getId());
            return Map.of("total", 0, "success", 0, "failed", 0, "skipped", 0);
        }

        // 构建跳转链接
        String url = wechatConfigService.getShareBaseUrl() + "/announcement/" + announcement.getId();

        // 发送时间
        String publishTime = announcement.getPublishTime() != null
                ? announcement.getPublishTime().format(DATE_FORMATTER)
                : "即时发布";

        // 执行推送
        return doPush(
                WechatPushRecord.BusinessType.ANNOUNCEMENT.name(),
                announcement.getId(),
                targetUsers,
                templateId,
                announcement.getTitle(),
                announcement.getContent(),
                publishTime,
                url
        );
    }

    @Override
    @Transactional
    public Map<String, Integer> pushNotification(Long notificationId, String title, String className,
                                                  String checkDate, List<Long> targetRoleIds) {
        if (!isPushEnabled()) {
            log.warn("微信推送功能未启用");
            return Map.of("total", 0, "success", 0, "failed", 0, "skipped", 0);
        }

        String templateId = wechatConfigService.getNotificationTemplateId();
        if (!StringUtils.hasText(templateId)) {
            log.warn("通报模板ID未配置");
            return Map.of("total", 0, "success", 0, "failed", 0, "skipped", 0);
        }

        // 获取目标用户
        List<User> targetUsers;
        if (targetRoleIds == null || targetRoleIds.isEmpty()) {
            targetUsers = findUsersWithOpenid();
        } else {
            targetUsers = findUsersByRoleIds(targetRoleIds);
        }

        if (targetUsers.isEmpty()) {
            log.info("没有需要推送的目标用户: notificationId={}", notificationId);
            return Map.of("total", 0, "success", 0, "failed", 0, "skipped", 0);
        }

        // 构建跳转链接
        String url = wechatConfigService.getShareBaseUrl() + "/notification/" + notificationId;

        // 执行推送
        return doPushNotification(
                notificationId,
                targetUsers,
                templateId,
                title,
                className,
                checkDate,
                url
        );
    }

    @Override
    @Transactional
    public Map<String, Integer> pushToUsers(String businessType, Long businessId,
                                            List<Long> userIds, String title, String content, String url) {
        if (!isPushEnabled()) {
            return Map.of("total", 0, "success", 0, "failed", 0, "skipped", 0);
        }

        List<User> targetUsers = findUsersByIds(userIds);
        String templateId = getTemplateId(businessType);

        if (!StringUtils.hasText(templateId) || targetUsers.isEmpty()) {
            return Map.of("total", 0, "success", 0, "failed", 0, "skipped", 0);
        }

        return doPush(businessType, businessId, targetUsers, templateId, title, content, "", url);
    }

    @Override
    @Transactional
    public Map<String, Integer> pushToRoles(String businessType, Long businessId,
                                            List<Long> roleIds, String title, String content, String url) {
        if (!isPushEnabled()) {
            return Map.of("total", 0, "success", 0, "failed", 0, "skipped", 0);
        }

        List<User> targetUsers = findUsersByRoleIds(roleIds);
        String templateId = getTemplateId(businessType);

        if (!StringUtils.hasText(templateId) || targetUsers.isEmpty()) {
            return Map.of("total", 0, "success", 0, "failed", 0, "skipped", 0);
        }

        return doPush(businessType, businessId, targetUsers, templateId, title, content, "", url);
    }

    @Override
    @Transactional
    public Map<String, Integer> pushToAll(String businessType, Long businessId,
                                          String title, String content, String url) {
        if (!isPushEnabled()) {
            return Map.of("total", 0, "success", 0, "failed", 0, "skipped", 0);
        }

        List<User> targetUsers = findUsersWithOpenid();
        String templateId = getTemplateId(businessType);

        if (!StringUtils.hasText(templateId) || targetUsers.isEmpty()) {
            return Map.of("total", 0, "success", 0, "failed", 0, "skipped", 0);
        }

        return doPush(businessType, businessId, targetUsers, templateId, title, content, "", url);
    }

    @Override
    public boolean isPushEnabled() {
        return wechatConfigService.isTemplateEnabled() && wechatConfigService.isConfigured();
    }

    @Override
    public Map<String, Integer> getPushStatistics(String businessType, Long businessId) {
        int success = pushRecordMapper.countSuccessByBusiness(businessType, businessId);
        int failed = pushRecordMapper.countFailedByBusiness(businessType, businessId);
        List<WechatPushRecord> records = pushRecordMapper.selectByBusiness(businessType, businessId);
        int total = records.size();
        int pending = total - success - failed;

        return Map.of("total", total, "success", success, "failed", failed, "pending", pending);
    }

    // ================== 私有方法 ==================

    /**
     * 获取目标用户
     */
    private List<User> getTargetUsers(String targetType, String targetIds) {
        if ("all".equals(targetType)) {
            return findUsersWithOpenid();
        }

        if (!StringUtils.hasText(targetIds)) {
            return Collections.emptyList();
        }

        try {
            List<Long> ids = objectMapper.readValue(targetIds, new TypeReference<List<Long>>() {});

            if ("role".equals(targetType)) {
                return findUsersByRoleIds(ids);
            } else if ("user".equals(targetType)) {
                return findUsersByIds(ids);
            }
        } catch (Exception e) {
            log.error("解析targetIds失败: {}", targetIds, e);
        }

        return Collections.emptyList();
    }

    /**
     * 查询有openid的所有用户
     */
    private List<User> findUsersWithOpenid() {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNotNull(User::getWechatOpenid)
               .ne(User::getWechatOpenid, "")
               .eq(User::getStatus, 1)
               .eq(User::getDeleted, 0);
        return userMapper.selectList(wrapper);
    }

    /**
     * 根据角色ID查询有openid的用户
     */
    private List<User> findUsersByRoleIds(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 使用原生SQL查询
        String roleIdsStr = roleIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        return userMapper.findUsersWithOpenidByRoleIds(roleIdsStr);
    }

    /**
     * 根据用户ID查询有openid的用户
     */
    private List<User> findUsersByIds(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyList();
        }

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(User::getId, userIds)
               .isNotNull(User::getWechatOpenid)
               .ne(User::getWechatOpenid, "")
               .eq(User::getStatus, 1)
               .eq(User::getDeleted, 0);
        return userMapper.selectList(wrapper);
    }

    /**
     * 获取模板ID
     */
    private String getTemplateId(String businessType) {
        if (WechatPushRecord.BusinessType.ANNOUNCEMENT.name().equals(businessType)) {
            return wechatConfigService.getAnnouncementTemplateId();
        } else if (WechatPushRecord.BusinessType.NOTIFICATION.name().equals(businessType)) {
            return wechatConfigService.getNotificationTemplateId();
        }
        return null;
    }

    /**
     * 执行公告推送
     */
    private Map<String, Integer> doPush(String businessType, Long businessId, List<User> targetUsers,
                                        String templateId, String title, String content,
                                        String publishTime, String url) {
        int total = targetUsers.size();
        int success = 0;
        int failed = 0;
        int skipped = 0;

        log.info("开始推送: businessType={}, businessId={}, targetCount={}", businessType, businessId, total);

        for (User user : targetUsers) {
            try {
                // 构建模板消息
                TemplateMessageDTO message = TemplateMessageDTO.buildAnnouncementMessage(
                        user.getWechatOpenid(),
                        templateId,
                        title,
                        content,
                        publishTime,
                        url
                );

                // 创建推送记录
                WechatPushRecord record = WechatPushRecord.create(
                        WechatPushRecord.BusinessType.valueOf(businessType),
                        businessId,
                        user.getId(),
                        user.getWechatOpenid(),
                        templateId,
                        toJson(message)
                );

                // 发送消息
                Map<String, Object> result = wechatService.sendTemplateMessage(message);
                Integer errcode = (Integer) result.get("errcode");

                if (errcode != null && errcode == 0) {
                    record.markSuccess(String.valueOf(result.get("msgid")));
                    success++;
                } else {
                    record.markFailed(String.valueOf(errcode), String.valueOf(result.get("errmsg")));
                    failed++;
                }

                // 保存记录
                pushRecordMapper.insert(record);

            } catch (Exception e) {
                log.error("推送失败: userId={}, openid={}", user.getId(), user.getWechatOpenid(), e);
                failed++;
            }
        }

        log.info("推送完成: businessType={}, businessId={}, total={}, success={}, failed={}",
                businessType, businessId, total, success, failed);

        return Map.of("total", total, "success", success, "failed", failed, "skipped", skipped);
    }

    /**
     * 执行通报推送
     */
    private Map<String, Integer> doPushNotification(Long businessId, List<User> targetUsers,
                                                     String templateId, String title, String className,
                                                     String checkDate, String url) {
        int total = targetUsers.size();
        int success = 0;
        int failed = 0;

        String businessType = WechatPushRecord.BusinessType.NOTIFICATION.name();

        log.info("开始推送通报: businessId={}, targetCount={}", businessId, total);

        for (User user : targetUsers) {
            try {
                // 构建模板消息
                TemplateMessageDTO message = TemplateMessageDTO.buildNotificationMessage(
                        user.getWechatOpenid(),
                        templateId,
                        title,
                        className,
                        checkDate,
                        url
                );

                // 创建推送记录
                WechatPushRecord record = WechatPushRecord.create(
                        WechatPushRecord.BusinessType.NOTIFICATION,
                        businessId,
                        user.getId(),
                        user.getWechatOpenid(),
                        templateId,
                        toJson(message)
                );

                // 发送消息
                Map<String, Object> result = wechatService.sendTemplateMessage(message);
                Integer errcode = (Integer) result.get("errcode");

                if (errcode != null && errcode == 0) {
                    record.markSuccess(String.valueOf(result.get("msgid")));
                    success++;
                } else {
                    record.markFailed(String.valueOf(errcode), String.valueOf(result.get("errmsg")));
                    failed++;
                }

                // 保存记录
                pushRecordMapper.insert(record);

            } catch (Exception e) {
                log.error("推送失败: userId={}, openid={}", user.getId(), user.getWechatOpenid(), e);
                failed++;
            }
        }

        log.info("推送完成: businessType={}, businessId={}, total={}, success={}, failed={}",
                businessType, businessId, total, success, failed);

        return Map.of("total", total, "success", success, "failed", failed, "skipped", 0);
    }

    /**
     * 对象转JSON
     */
    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return null;
        }
    }
}
