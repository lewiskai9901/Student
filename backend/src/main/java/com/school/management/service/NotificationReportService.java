package com.school.management.service;

import com.school.management.entity.NotificationRecord;
import java.util.List;

/**
 * 通报管理服务接口
 *
 * @author system
 * @since 4.2.0
 */
public interface NotificationReportService {

    /**
     * 生成通报草稿
     * 创建可编辑的草稿记录，不立即生成文件
     *
     * @param planId 计划ID
     * @param templateId 模板ID
     * @param dailyCheckIds 日常检查ID列表
     * @param checkRounds 选择的轮次列表
     * @param deductionItemIds 扣分项筛选，为空则包含全部
     * @param variableValues 变量值（JSON格式）
     * @return 通报记录（草稿状态）
     */
    NotificationRecord generateNotification(
            Long planId,
            Long templateId,
            List<Long> dailyCheckIds,
            List<Integer> checkRounds,
            List<Long> deductionItemIds,
            String variableValues
    );

    /**
     * 更新通报内容
     *
     * @param id 通报记录ID
     * @param title 标题
     * @param contentHtml 内容HTML
     * @return 更新后的通报记录
     */
    NotificationRecord updateNotificationContent(Long id, String title, String contentHtml);

    /**
     * 发布通报
     *
     * @param id 通报记录ID
     * @return 发布后的通报记录
     */
    NotificationRecord publishNotification(Long id);

    /**
     * 下载通报文件（按需生成）
     *
     * @param id 通报记录ID
     * @param format 输出格式 (PDF/WORD)
     * @return 文件字节数组
     */
    byte[] downloadNotification(Long id, String format);

    /**
     * 获取通报历史记录
     *
     * @param planId 计划ID
     * @return 通报记录列表
     */
    List<NotificationRecord> getNotificationHistory(Long planId);

    /**
     * 获取通报详情
     *
     * @param id 通报记录ID
     * @return 通报记录
     */
    NotificationRecord getNotificationById(Long id);

    /**
     * 重新生成通报
     *
     * @param recordId 原通报记录ID
     * @return 新的通报记录
     */
    NotificationRecord regenerateNotification(Long recordId);

    /**
     * 删除通报记录
     *
     * @param id 通报记录ID
     */
    void deleteNotification(Long id);

    /**
     * 获取通报文件
     *
     * @param id 通报记录ID
     * @return 文件字节数组
     */
    byte[] getNotificationFile(Long id);
}
