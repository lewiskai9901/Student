package com.school.management.service.task.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.school.management.dto.task.SystemMessageDTO;
import com.school.management.entity.task.SystemMessage;
import com.school.management.entity.task.Task;
import com.school.management.enums.MessageType;
import com.school.management.mapper.task.SystemMessageMapper;
import com.school.management.service.task.SystemMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 站内消息服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemMessageServiceImpl extends ServiceImpl<SystemMessageMapper, SystemMessage>
        implements SystemMessageService {

    private final SystemMessageMapper systemMessageMapper;

    @Override
    public IPage<SystemMessageDTO> pageQuery(Integer pageNum, Integer pageSize, Long receiverId, String messageType, Integer isRead) {
        Page<SystemMessage> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<SystemMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemMessage::getReceiverId, receiverId)
                .eq(StringUtils.hasText(messageType), SystemMessage::getMessageType, messageType)
                .eq(isRead != null, SystemMessage::getIsRead, isRead)
                .orderByDesc(SystemMessage::getCreatedAt);

        IPage<SystemMessage> result = page(page, wrapper);
        return result.convert(this::convertToDTO);
    }

    @Override
    public Long countUnread(Long receiverId) {
        return systemMessageMapper.countUnread(receiverId);
    }

    @Override
    public boolean markAsRead(Long messageId, Long userId) {
        SystemMessage message = getById(messageId);
        if (message == null || !message.getReceiverId().equals(userId)) {
            return false;
        }

        message.setIsRead(1);
        message.setReadTime(LocalDateTime.now());
        return updateById(message);
    }

    @Override
    public int markAllAsRead(Long receiverId) {
        return systemMessageMapper.markAllAsRead(receiverId);
    }

    @Override
    public boolean deleteMessage(Long messageId, Long userId) {
        SystemMessage message = getById(messageId);
        if (message == null || !message.getReceiverId().equals(userId)) {
            return false;
        }
        return removeById(messageId);
    }

    @Override
    @Async
    public void sendTaskAssignMessage(Task task, Long receiverId) {
        try {
            SystemMessage message = new SystemMessage();
            message.setMessageType(MessageType.TASK_ASSIGN.getCode());
            message.setTitle("您有新的任务");
            message.setContent(String.format("【%s】分配给您一个新任务：%s", task.getAssignerName(), task.getTitle()));
            message.setSenderId(task.getAssignerId());
            message.setSenderName(task.getAssignerName());
            message.setReceiverId(receiverId);
            message.setIsRead(0);
            message.setBusinessType("TASK");
            message.setBusinessId(task.getId());

            Map<String, Object> extraData = new HashMap<>();
            extraData.put("taskCode", task.getTaskCode());
            extraData.put("priority", task.getPriority());
            extraData.put("dueDate", task.getDueDate());
            message.setExtraData(extraData);

            save(message);
            log.info("发送任务分配通知成功: taskId={}, receiverId={}", task.getId(), receiverId);
        } catch (Exception e) {
            log.error("发送任务分配通知失败: taskId={}, receiverId={}", task.getId(), receiverId, e);
        }
    }

    @Override
    @Async
    public void sendApprovalNotification(Task task, Long receiverId) {
        try {
            SystemMessage message = new SystemMessage();
            message.setMessageType(MessageType.TASK_APPROVE.getCode());
            message.setTitle("您有待审批的任务");
            // 使用任务创建人信息，因为Task实体没有assignee相关字段
            message.setContent(String.format("任务【%s】等待您审批", task.getTitle()));
            message.setSenderId(task.getAssignerId());
            message.setSenderName(task.getAssignerName());
            message.setReceiverId(receiverId);
            message.setIsRead(0);
            message.setBusinessType("TASK");
            message.setBusinessId(task.getId());

            save(message);
            log.info("发送审批通知成功: taskId={}, receiverId={}", task.getId(), receiverId);
        } catch (Exception e) {
            log.error("发送审批通知失败: taskId={}, receiverId={}", task.getId(), receiverId, e);
        }
    }

    @Override
    @Async
    public void sendTaskCompleteMessage(Task task, Long receiverId) {
        try {
            SystemMessage message = new SystemMessage();
            message.setMessageType(MessageType.TASK_COMPLETE.getCode());
            message.setTitle("任务已完成");
            message.setContent(String.format("您提交的任务【%s】已审批通过", task.getTitle()));
            message.setReceiverId(receiverId);
            message.setIsRead(0);
            message.setBusinessType("TASK");
            message.setBusinessId(task.getId());

            save(message);
            log.info("发送任务完成通知成功: taskId={}, receiverId={}", task.getId(), receiverId);
        } catch (Exception e) {
            log.error("发送任务完成通知失败: taskId={}, receiverId={}", task.getId(), receiverId, e);
        }
    }

    @Override
    @Async
    public void sendTaskRejectMessage(Task task, Long receiverId, String reason) {
        try {
            SystemMessage message = new SystemMessage();
            message.setMessageType(MessageType.TASK_REJECT.getCode());
            message.setTitle("任务被打回");
            message.setContent(String.format("您提交的任务【%s】被打回，原因：%s", task.getTitle(), reason));
            message.setReceiverId(receiverId);
            message.setIsRead(0);
            message.setBusinessType("TASK");
            message.setBusinessId(task.getId());

            Map<String, Object> extraData = new HashMap<>();
            extraData.put("rejectReason", reason);
            message.setExtraData(extraData);

            save(message);
            log.info("发送任务打回通知成功: taskId={}, receiverId={}", task.getId(), receiverId);
        } catch (Exception e) {
            log.error("发送任务打回通知失败: taskId={}, receiverId={}", task.getId(), receiverId, e);
        }
    }

    @Override
    @Async
    public void sendTaskRemindMessage(Task task, Long receiverId, String content) {
        try {
            SystemMessage message = new SystemMessage();
            message.setMessageType(MessageType.TASK_REMIND.getCode());
            message.setTitle("任务提醒");
            message.setContent(content);
            message.setReceiverId(receiverId);
            message.setIsRead(0);
            message.setBusinessType("TASK");
            message.setBusinessId(task.getId());

            save(message);
            log.info("发送任务提醒通知成功: taskId={}, receiverId={}", task.getId(), receiverId);
        } catch (Exception e) {
            log.error("发送任务提醒通知失败: taskId={}, receiverId={}", task.getId(), receiverId, e);
        }
    }

    @Override
    public SystemMessageDTO convertToDTO(SystemMessage message) {
        if (message == null) {
            return null;
        }
        SystemMessageDTO dto = new SystemMessageDTO();
        BeanUtils.copyProperties(message, dto);

        MessageType type = MessageType.fromCode(message.getMessageType());
        if (type != null) {
            dto.setMessageTypeText(type.getDesc());
        }

        return dto;
    }
}
