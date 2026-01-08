package com.school.management.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 任务操作类型枚举
 */
@Getter
@AllArgsConstructor
public enum TaskAction {

    CREATE("CREATE", "创建任务"),
    ASSIGN("ASSIGN", "分配任务"),
    ACCEPT("ACCEPT", "接收任务"),
    SUBMIT("SUBMIT", "提交任务"),
    APPROVE("APPROVE", "审批通过"),
    REJECT("REJECT", "审批打回"),
    CANCEL("CANCEL", "取消任务"),
    TRANSFER("TRANSFER", "转交任务"),
    RESUBMIT("RESUBMIT", "重新提交");

    private final String code;
    private final String desc;

    public static TaskAction fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (TaskAction action : values()) {
            if (action.getCode().equals(code)) {
                return action;
            }
        }
        return null;
    }
}
