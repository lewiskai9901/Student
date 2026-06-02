package com.school.management.infrastructure.persistence.system;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 公告/通知持久化对象 — 对应 announcements 表。
 *
 * <p>2026-06-02 补全: 该表 + 前端页面/菜单/权限早已存在, 但后端 API 从未实现 (一直 404)。
 * 本 PO + Mapper + AnnouncementController 补齐 CRUD。
 */
@Data
@TableName("announcements")
public class AnnouncementPO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String title;
    private String content;
    /** notice / announcement / warning */
    private String announcementType;
    /** 1 普通 / 2 重要 / 3 紧急 */
    private Integer priority;
    private Long publisherId;
    private String publisherName;
    private LocalDateTime publishTime;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    /** ALL / ORG / ROLE / USER 等 */
    private String targetType;
    /** 目标 ID 列表 (JSON 原文) */
    private String targetIds;
    private String attachmentUrl;
    private Integer isPublished;
    private Integer isPinned;
    private Integer viewCount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    private Long createdBy;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    private Long updatedBy;

    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;

    private Long tenantId;
}
