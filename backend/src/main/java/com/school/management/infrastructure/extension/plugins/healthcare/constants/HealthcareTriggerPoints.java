package com.school.management.infrastructure.extension.plugins.healthcare.constants;

/**
 * Phase 8.6: HEALTH 触发点常量 (避免裸字符串, NoMagicTriggerStringTest 会抓).
 */
public final class HealthcareTriggerPoints {
    private HealthcareTriggerPoints() {}

    /** 病人入院 — context: {patientId, wardId, attendingDoctorId} */
    public static final String PATIENT_ADMITTED = "PATIENT_ADMITTED";

    /** 病人出院 — context: {patientId, dischargeType} */
    public static final String PATIENT_DISCHARGED = "PATIENT_DISCHARGED";
}
