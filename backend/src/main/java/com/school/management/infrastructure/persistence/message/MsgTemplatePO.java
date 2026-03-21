package com.school.management.infrastructure.persistence.message;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 消息模板持久化对象
 */
@Data
@TableName("msg_templates")
public class MsgTemplatePO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    private String templateCode;
    private String templateName;
    private String titleTemplate;
    private String contentTemplate;
    private Integer isSystem;
    private Integer isEnabled;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
