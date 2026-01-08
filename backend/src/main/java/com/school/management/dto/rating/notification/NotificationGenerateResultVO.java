package com.school.management.dto.rating.notification;

import lombok.Data;

/**
 * 通报生成结果VO
 *
 * @author Claude Code
 * @since 2025-12-22
 */
@Data
public class NotificationGenerateResultVO {

    private Boolean success;
    private String message;

    // 生成的文件信息
    private String fileUrl;             // 文件访问URL
    private String fileName;            // 文件名
    private Long fileSize;              // 文件大小（字节）
    private String fileFormat;          // 文件格式

    // 通报记录ID（如果保存到记录）
    private Long notificationRecordId;

    // 包含的班级数量
    private Integer includedClassCount;

    // 生成时间
    private String generatedAt;
}
