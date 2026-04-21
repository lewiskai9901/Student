package com.school.management.infrastructure.extension.plugins.healthcare.constants;

/**
 * HEALTH 触发点常量 (避免裸字符串, NoMagicTriggerStringTest 会抓).
 */
public final class HealthcareTriggerPoints {
    private HealthcareTriggerPoints() {}

    /** 病人入院 — context: {patientId, wardId, attendingDoctorId} */
    public static final String PATIENT_ADMITTED = "PATIENT_ADMITTED";

    /** 病人出院 — context: {patientId, dischargeType} */
    public static final String PATIENT_DISCHARGED = "PATIENT_DISCHARGED";

    /** 病人转科/转病区 — context: {patientId, fromWardId, toWardId} */
    public static final String PATIENT_TRANSFERRED = "PATIENT_TRANSFERRED";

    /** 手术安排 — context: {patientId, surgeryType, scheduledAt} */
    public static final String SURGERY_SCHEDULED = "SURGERY_SCHEDULED";

    /** 用药提醒 — context: {patientId, medicationName, dueAt} */
    public static final String MEDICATION_DUE = "MEDICATION_DUE";

    /** 紧急告警 — context: {patientId, wardId, alertLevel, reason} */
    public static final String EMERGENCY_ALERT = "EMERGENCY_ALERT";
}
