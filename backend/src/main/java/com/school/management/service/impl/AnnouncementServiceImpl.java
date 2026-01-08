package com.school.management.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.school.management.entity.Announcement;
import com.school.management.entity.AnnouncementRead;
import com.school.management.exception.BusinessException;
import com.school.management.mapper.AnnouncementMapper;
import com.school.management.mapper.AnnouncementReadMapper;
import com.school.management.service.AnnouncementService;
import com.school.management.service.WechatPushService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 公告服务实现类
 *
 * @author Claude Code
 * @date 2025-11-18
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnnouncementServiceImpl extends ServiceImpl<AnnouncementMapper, Announcement> implements AnnouncementService {

    private final AnnouncementReadMapper announcementReadMapper;
    private final WechatPushService wechatPushService;

    @Override
    public IPage<Announcement> queryPage(String title, String announcementType, Integer isPublished,
                                         int pageNum, int pageSize) {
        Page<Announcement> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Announcement> wrapper = new LambdaQueryWrapper<>();

        wrapper.like(StringUtils.hasText(title), Announcement::getTitle, title)
                .eq(StringUtils.hasText(announcementType), Announcement::getAnnouncementType, announcementType)
                .eq(isPublished != null, Announcement::getIsPublished, isPublished)
                .eq(Announcement::getDeleted, 0)
                .orderByDesc(Announcement::getIsPinned, Announcement::getPublishTime);

        return this.page(page, wrapper);
    }

    @Override
    public boolean publish(Long id) {
        Announcement announcement = this.getById(id);
        if (announcement == null) {
            throw new BusinessException("公告不存在");
        }

        announcement.setIsPublished(1);
        announcement.setPublishTime(LocalDateTime.now());
        boolean result = this.updateById(announcement);

        // 发布成功后触发微信推送
        if (result && wechatPushService.isPushEnabled()) {
            try {
                Map<String, Integer> pushResult = wechatPushService.pushAnnouncement(announcement);
                log.info("公告微信推送完成: id={}, total={}, success={}, failed={}",
                        id, pushResult.get("total"), pushResult.get("success"), pushResult.get("failed"));
            } catch (Exception e) {
                log.error("公告微信推送失败: id={}", id, e);
                // 推送失败不影响发布结果
            }
        }

        return result;
    }

    @Override
    public boolean revoke(Long id) {
        Announcement announcement = this.getById(id);
        if (announcement == null) {
            throw new BusinessException("公告不存在");
        }

        announcement.setIsPublished(0);
        return this.updateById(announcement);
    }

    @Override
    public boolean pin(Long id, boolean pinned) {
        Announcement announcement = this.getById(id);
        if (announcement == null) {
            throw new BusinessException("公告不存在");
        }

        announcement.setIsPinned(pinned ? 1 : 0);
        return this.updateById(announcement);
    }

    @Override
    public boolean markAsRead(Long announcementId, Long userId) {
        try {
            // 检查是否已读
            LambdaQueryWrapper<AnnouncementRead> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(AnnouncementRead::getAnnouncementId, announcementId)
                    .eq(AnnouncementRead::getUserId, userId);

            if (announcementReadMapper.selectCount(wrapper) > 0) {
                return true; // 已读过
            }

            // 创建阅读记录
            AnnouncementRead read = new AnnouncementRead();
            read.setAnnouncementId(announcementId);
            read.setUserId(userId);
            read.setReadTime(LocalDateTime.now());
            read.setCreatedAt(LocalDateTime.now());

            announcementReadMapper.insert(read);

            // 增加浏览次数
            Announcement announcement = this.getById(announcementId);
            if (announcement != null) {
                announcement.setViewCount(announcement.getViewCount() + 1);
                this.updateById(announcement);
            }

            return true;
        } catch (Exception e) {
            log.error("标记公告已读失败", e);
            return false;
        }
    }

    @Override
    public long getUnreadCount(Long userId) {
        // 获取所有已发布且在有效期内的公告
        List<Announcement> announcements = getUserAnnouncements(userId);

        // 统计未读数量
        long unreadCount = 0;
        for (Announcement announcement : announcements) {
            LambdaQueryWrapper<AnnouncementRead> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(AnnouncementRead::getAnnouncementId, announcement.getId())
                    .eq(AnnouncementRead::getUserId, userId);

            if (announcementReadMapper.selectCount(wrapper) == 0) {
                unreadCount++;
            }
        }

        return unreadCount;
    }

    @Override
    public List<Announcement> getUserAnnouncements(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        LambdaQueryWrapper<Announcement> wrapper = new LambdaQueryWrapper<>();

        wrapper.eq(Announcement::getIsPublished, 1)
                .eq(Announcement::getDeleted, 0)
                .and(w -> w.isNull(Announcement::getStartTime)
                        .or()
                        .le(Announcement::getStartTime, now))
                .and(w -> w.isNull(Announcement::getEndTime)
                        .or()
                        .ge(Announcement::getEndTime, now))
                .orderByDesc(Announcement::getIsPinned, Announcement::getPublishTime);

        return this.list(wrapper);
    }
}
