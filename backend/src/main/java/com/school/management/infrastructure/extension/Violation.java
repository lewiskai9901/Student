package com.school.management.infrastructure.extension;

/**
 * 策略检查违规 — Policy.check() 返回的条目.
 *
 * @param severity BLOCK/WARN/INFO, 决定后续流程
 * @param code     全局违规码, 用于前端 i18n / 告警路由
 * @param message  人类可读消息
 * @param detail   可选: 具体违规对象 (如越限的 entity), 方便追责
 */
public record Violation(Severity severity, String code, String message, Object detail) {

    public enum Severity { BLOCK, WARN, INFO }

    public static Violation block(String code, String msg) {
        return new Violation(Severity.BLOCK, code, msg, null);
    }

    public static Violation warn(String code, String msg) {
        return new Violation(Severity.WARN, code, msg, null);
    }

    public static Violation info(String code, String msg) {
        return new Violation(Severity.INFO, code, msg, null);
    }
}
