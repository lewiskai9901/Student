package com.school.management.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.school.management.entity.Announcement;

import java.util.List;

/**
 * 公告服务接口
 *
 * @author Claude Code
 * @date 2025-11-18
 */
public interface AnnouncementService extends IService<Announcement> {

    /**
     * 分页查询公告
     */
    IPage<Announcement> queryPage(String title, String announcementType, Integer isPublished,
                                   int pageNum, int pageSize);

    /**
     * 发布公告
     */
    boolean publish(Long id);

    /**
     * 撤回公告
     */
    boolean revoke(Long id);

    /**
     * 置顶/取消置顶
     */
    boolean pin(Long id, boolean pinned);

    /**
     * 标记已读
     */
    boolean markAsRead(Long announcementId, Long userId);

    /**
     * 获取未读公告数量
     */
    long getUnreadCount(Long userId);

    /**
     * 获取用户的公告列表(已发布且在有效期内)
     */
    List<Announcement> getUserAnnouncements(Long userId);
}
