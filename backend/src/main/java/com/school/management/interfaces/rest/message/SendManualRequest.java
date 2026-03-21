package com.school.management.interfaces.rest.message;

import lombok.Data;

import java.util.List;

/**
 * 手动发送消息请求体
 */
@Data
public class SendManualRequest {

    private List<Long> userIds;
    private String title;
    private String content;
}
