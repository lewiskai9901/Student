package com.school.management.infrastructure.extension.plugins.healthcare.messaging;

import com.school.management.infrastructure.extension.MessagingDomainPlugin;
import com.school.management.infrastructure.extension.plugins.healthcare.constants.HealthcareTriggerPoints;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Phase 8.6: HEALTH 入院/出院消息域 reference impl.
 *
 * 3 件事一并声明:
 *  1. triggerPoints: 登记业务代码可 fire 的触发点常量 (PATIENT_ADMITTED / PATIENT_DISCHARGED)
 *  2. eventTypes: UI 消息类型分组 (病人入院 → 医疗场景, polarity=NEUTRAL)
 *  3. defaultTriggers: 默认触发器 — 入院事件把 patientId 作为 subject 扇出
 */
@Component
@SuppressWarnings("deprecation")
public class PatientAdmissionMessagingPlugin implements MessagingDomainPlugin {

    @Override public String getDomainCode() { return "patient-admission"; }
    @Override public String getDomainName() { return "病人入院"; }

    @Override
    public List<TriggerPointDef> triggerPoints() {
        return List.of(
            new TriggerPointDef(
                HealthcareTriggerPoints.PATIENT_ADMITTED,
                "病人入院",
                Map.of("patientId", "Long", "wardId", "Long", "attendingDoctorId", "Long"),
                "病人办理入院登记时触发, 通知家属"),
            new TriggerPointDef(
                HealthcareTriggerPoints.PATIENT_DISCHARGED,
                "病人出院",
                Map.of("patientId", "Long", "dischargeType", "String"),
                "病人办理出院登记时触发, 通知家属")
        );
    }

    @Override
    public List<EventTypeDef> eventTypes() {
        return List.of(
            new EventTypeDef(
                "PATIENT_ADMITTED_EVT", "入院登记",
                "HEALTHCARE", "医疗事件", "NEUTRAL",
                "HeartPulse", "#dc2626",
                List.of("USER"),
                "病人入院"),
            new EventTypeDef(
                "PATIENT_DISCHARGED_EVT", "出院登记",
                "HEALTHCARE", "医疗事件", "NEUTRAL",
                "LogOut", "#10b981",
                List.of("USER"),
                "病人出院")
        );
    }

    @Override
    public List<DefaultTriggerDef> defaultTriggers() {
        return List.of(
            new DefaultTriggerDef(
                HealthcareTriggerPoints.PATIENT_ADMITTED,
                "PATIENT_ADMITTED_EVT",
                List.of(Map.of("type", "USER", "id", "{{patientId}}"))),
            new DefaultTriggerDef(
                HealthcareTriggerPoints.PATIENT_DISCHARGED,
                "PATIENT_DISCHARGED_EVT",
                List.of(Map.of("type", "USER", "id", "{{patientId}}")))
        );
    }
}
