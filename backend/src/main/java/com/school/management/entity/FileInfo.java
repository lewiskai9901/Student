package com.school.management.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文件信息实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("file_info")
public class FileInfo extends BaseEntity {

    /**
     * 原始文件名
     */
    private String originalName;

    /**
     * 存储文件名
     */
    private String storedName;

    /**
     * 文件路径
     */
    private String filePath;

    /**
     * 文件大小(字节)
     */
    private Long fileSize;

    /**
     * 文件类型
     */
    private String fileType;

    /**
     * MIME类型
     */
    private String mimeType;

    /**
     * 业务类型 (student_photo, document, attachment等)
     */
    private String businessType;

    /**
     * 业务ID
     */
    private Long businessId;

    /**
     * 上传者ID
     */
    private Long uploaderId;

    /**
     * 文件访问URL
     */
    private String fileUrl;

    /**
     * 备注
     */
    private String remark;
}
