package com.school.management.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 公告通知实体类
 *
 * @author Claude Code
 * @date 2025-11-18
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("announcements")
public class Announcement extends BaseEntity {

    /**
     * 公告标题
     */
    private String title;

    /**
     * 公告内容
     */
    private String content;

    /**
     * 公告类型(notice-通知,announcement-公告,warning-警告)
     */
    private String announcementType;

    /**
     * 优先级(1-普通,2-重要,3-紧急)
     */
    private Integer priority;

    /**
     * 发布人ID
     */
    private Long publisherId;

    /**
     * 发布人姓名
     */
    private String publisherName;

    /**
     * 发布时间
     */
    private LocalDateTime publishTime;

    /**
     * 生效开始时间
     */
    private LocalDateTime startTime;

    /**
     * 生效结束时间
     */
    private LocalDateTime endTime;

    /**
     * 目标类型(all-全体,role-角色,user-指定用户)
     */
    private String targetType;

    /**
     * 目标ID列表(JSON数组)
     */
    private String targetIds;

    /**
     * 附件URL
     */
    private String attachmentUrl;

    /**
     * 是否已发布(0-草稿,1-已发布)
     */
    private Integer isPublished;

    /**
     * 是否置顶(0-否,1-是)
     */
    private Integer isPinned;

    /**
     * 浏览次数
     */
    private Integer viewCount;
}
