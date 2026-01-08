package com.school.management.service.task;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.school.management.dto.task.SystemMessageDTO;
import com.school.management.entity.task.SystemMessage;
import com.school.management.entity.task.Task;

/**
 * 站内消息服务接口
 */
public interface SystemMessageService extends IService<SystemMessage> {

    /**
     * 分页查询消息
     */
    IPage<SystemMessageDTO> pageQuery(Integer pageNum, Integer pageSize, Long receiverId, String messageType, Integer isRead);

    /**
     * 获取未读消息数量
     */
    Long countUnread(Long receiverId);

    /**
     * 标记消息为已读
     */
    boolean markAsRead(Long messageId, Long userId);

    /**
     * 标记所有消息为已读
     */
    int markAllAsRead(Long receiverId);

    /**
     * 删除消息
     */
    boolean deleteMessage(Long messageId, Long userId);

    /**
     * 发送任务分配通知
     */
    void sendTaskAssignMessage(Task task, Long receiverId);

    /**
     * 发送审批通知
     */
    void sendApprovalNotification(Task task, Long receiverId);

    /**
     * 发送任务完成通知
     */
    void sendTaskCompleteMessage(Task task, Long receiverId);

    /**
     * 发送任务打回通知
     */
    void sendTaskRejectMessage(Task task, Long receiverId, String reason);

    /**
     * 发送任务提醒通知
     */
    void sendTaskRemindMessage(Task task, Long receiverId, String message);

    /**
     * DTO转换
     */
    SystemMessageDTO convertToDTO(SystemMessage message);
}
