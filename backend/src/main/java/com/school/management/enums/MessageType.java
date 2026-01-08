package com.school.management.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 消息类型枚举
 */
@Getter
@AllArgsConstructor
public enum MessageType {

    TASK_ASSIGN("TASK_ASSIGN", "任务分配"),
    TASK_REMIND("TASK_REMIND", "任务提醒"),
    TASK_APPROVE("TASK_APPROVE", "审批通知"),
    TASK_REJECT("TASK_REJECT", "打回通知"),
    TASK_COMPLETE("TASK_COMPLETE", "完成通知"),
    SYSTEM("SYSTEM", "系统通知");

    private final String code;
    private final String desc;

    public static MessageType fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (MessageType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }
}
