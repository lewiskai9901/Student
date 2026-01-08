package com.school.management.dto.wechat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * 微信模板消息 DTO
 *
 * @author system
 * @since 4.3.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateMessageDTO {

    /**
     * 接收者openid
     */
    private String touser;

    /**
     * 模板ID
     */
    private String template_id;

    /**
     * 跳转链接（可选）
     */
    private String url;

    /**
     * 模板数据
     */
    private Map<String, TemplateData> data;

    /**
     * 模板数据项
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TemplateData {
        /**
         * 数据值
         */
        private String value;

        /**
         * 颜色（可选）
         */
        private String color;

        public TemplateData(String value) {
            this.value = value;
        }
    }

    /**
     * 构建公告推送消息
     */
    public static TemplateMessageDTO buildAnnouncementMessage(
            String openid,
            String templateId,
            String title,
            String content,
            String publishTime,
            String url) {

        Map<String, TemplateData> data = new HashMap<>();
        data.put("first", new TemplateData("您有一条新公告"));
        data.put("keyword1", new TemplateData(title));
        data.put("keyword2", new TemplateData(content.length() > 50 ? content.substring(0, 50) + "..." : content));
        data.put("keyword3", new TemplateData(publishTime));
        data.put("remark", new TemplateData("点击查看详情"));

        return TemplateMessageDTO.builder()
                .touser(openid)
                .template_id(templateId)
                .url(url)
                .data(data)
                .build();
    }

    /**
     * 构建通报推送消息
     */
    public static TemplateMessageDTO buildNotificationMessage(
            String openid,
            String templateId,
            String title,
            String className,
            String checkDate,
            String url) {

        Map<String, TemplateData> data = new HashMap<>();
        data.put("first", new TemplateData("您有一条新的检查通报"));
        data.put("keyword1", new TemplateData(title));
        data.put("keyword2", new TemplateData(className));
        data.put("keyword3", new TemplateData(checkDate));
        data.put("remark", new TemplateData("点击查看详情"));

        return TemplateMessageDTO.builder()
                .touser(openid)
                .template_id(templateId)
                .url(url)
                .data(data)
                .build();
    }
}
