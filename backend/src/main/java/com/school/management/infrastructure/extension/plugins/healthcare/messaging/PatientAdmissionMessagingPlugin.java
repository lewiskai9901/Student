package com.school.management.infrastructure.extension.plugins.healthcare.messaging;

import com.school.management.infrastructure.extension.Contribution;
import com.school.management.infrastructure.extension.MessagingDomainPlugin;
import com.school.management.infrastructure.extension.PluginPackage;
import com.school.management.infrastructure.extension.plugins.healthcare.constants.HealthcareTriggerPoints;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * HEALTH 入院/出院/转科/手术/用药/紧急告警 消息插件.
 *
 * 从旧 {@code MessagingDomainPlugin} 三合一 SPI 迁移到 {@link PluginPackage#contribute()}
 * 返回细粒度 {@link Contribution.TriggerPointContribution} / {@link Contribution.EventTypeContribution}.
 *
 * <h3>本插件声明</h3>
 * <ul>
 *   <li>6 个触发点: PATIENT_ADMITTED / PATIENT_DISCHARGED / PATIENT_TRANSFERRED /
 *                 SURGERY_SCHEDULED / MEDICATION_DUE / EMERGENCY_ALERT</li>
 *   <li>6 个事件类型: 对应的 *_EVT</li>
 * </ul>
 *
 * <h3>industry metadata</h3>
 * 本类亦实现 {@link PluginPackage}, 沿用 HEALTH 行业码, 与 HealthcareManifest
 * 同 industry_code, UPSERT 到 plugin_packages 时同行幂等.
 */
@Component
public class PatientAdmissionMessagingPlugin implements PluginPackage {

    private static final String DOMAIN_CODE = "patient-admission";
    private static final String DOMAIN_NAME = "病人入院";

    // ═════════ PluginManifest 元数据 (复用 HEALTH industry) ═════════

    @Override public String getIndustryCode() { return "HEALTH"; }
    @Override public String getIndustryName() { return "医疗行业"; }
    @Override public List<String> getDependsOn() { return List.of("CORE"); }

    /** 由包路径归属本插件类到 HEALTH (与 HealthcareManifest.owns 行为一致) */
    @Override
    public boolean owns(Class<?> pluginClass) {
        return pluginClass.getPackageName().contains(".plugins.healthcare");
    }

    // ═════════ Track M3: 细粒度 Contribution ═════════

    @Override
    public Stream<Contribution> contribute() {
        return Stream.of(
            // ─── 6 个触发点 ───
            new Contribution.TriggerPointContribution(DOMAIN_CODE, DOMAIN_NAME,
                new MessagingDomainPlugin.TriggerPointDef(
                    HealthcareTriggerPoints.PATIENT_ADMITTED,
                    "病人入院",
                    Map.of("patientId", "Long", "wardId", "Long", "attendingDoctorId", "Long"),
                    "病人办理入院登记时触发, 通知家属")),
            new Contribution.TriggerPointContribution(DOMAIN_CODE, DOMAIN_NAME,
                new MessagingDomainPlugin.TriggerPointDef(
                    HealthcareTriggerPoints.PATIENT_DISCHARGED,
                    "病人出院",
                    Map.of("patientId", "Long", "dischargeType", "String"),
                    "病人办理出院登记时触发, 通知家属")),
            new Contribution.TriggerPointContribution(DOMAIN_CODE, DOMAIN_NAME,
                new MessagingDomainPlugin.TriggerPointDef(
                    HealthcareTriggerPoints.PATIENT_TRANSFERRED,
                    "病人转科",
                    Map.of("patientId", "Long", "fromWardId", "Long", "toWardId", "Long"),
                    "病人转科/转病区时触发, 通知双方病区医护")),
            new Contribution.TriggerPointContribution(DOMAIN_CODE, DOMAIN_NAME,
                new MessagingDomainPlugin.TriggerPointDef(
                    HealthcareTriggerPoints.SURGERY_SCHEDULED,
                    "手术安排",
                    Map.of("patientId", "Long", "surgeryType", "String", "scheduledAt", "Instant"),
                    "主刀医师确定手术时间后触发, 通知病人/家属/手术室")),
            new Contribution.TriggerPointContribution(DOMAIN_CODE, DOMAIN_NAME,
                new MessagingDomainPlugin.TriggerPointDef(
                    HealthcareTriggerPoints.MEDICATION_DUE,
                    "用药提醒",
                    Map.of("patientId", "Long", "medicationName", "String", "dueAt", "Instant"),
                    "按医嘱时间到点时触发, 提醒责任护士用药")),
            new Contribution.TriggerPointContribution(DOMAIN_CODE, DOMAIN_NAME,
                new MessagingDomainPlugin.TriggerPointDef(
                    HealthcareTriggerPoints.EMERGENCY_ALERT,
                    "紧急告警",
                    Map.of("patientId", "Long", "wardId", "Long", "alertLevel", "String", "reason", "String"),
                    "监护仪/设备异常或人员呼叫紧急告警时触发, 扇出到病区当班医护")),

            // ─── 6 个事件类型 ───
            new Contribution.EventTypeContribution(DOMAIN_CODE, DOMAIN_NAME,
                new MessagingDomainPlugin.EventTypeDef(
                    "PATIENT_ADMITTED_EVT", "入院登记",
                    "HEALTHCARE", "医疗事件", "NEUTRAL",
                    "HeartPulse", "#dc2626",
                    List.of("USER"),
                    "病人入院")),
            new Contribution.EventTypeContribution(DOMAIN_CODE, DOMAIN_NAME,
                new MessagingDomainPlugin.EventTypeDef(
                    "PATIENT_DISCHARGED_EVT", "出院登记",
                    "HEALTHCARE", "医疗事件", "NEUTRAL",
                    "LogOut", "#10b981",
                    List.of("USER"),
                    "病人出院")),
            new Contribution.EventTypeContribution(DOMAIN_CODE, DOMAIN_NAME,
                new MessagingDomainPlugin.EventTypeDef(
                    "PATIENT_TRANSFERRED_EVT", "转科通知",
                    "HEALTHCARE", "医疗事件", "NEUTRAL",
                    "ArrowLeftRight", "#0ea5e9",
                    List.of("USER"),
                    "病人转科/转病区")),
            new Contribution.EventTypeContribution(DOMAIN_CODE, DOMAIN_NAME,
                new MessagingDomainPlugin.EventTypeDef(
                    "SURGERY_SCHEDULED_EVT", "手术排期",
                    "HEALTHCARE", "医疗事件", "NEUTRAL",
                    "Scissors", "#7c3aed",
                    List.of("USER"),
                    "手术已排期")),
            new Contribution.EventTypeContribution(DOMAIN_CODE, DOMAIN_NAME,
                new MessagingDomainPlugin.EventTypeDef(
                    "MEDICATION_DUE_EVT", "用药提醒",
                    "HEALTHCARE", "医疗事件", "NEUTRAL",
                    "Pill", "#f59e0b",
                    List.of("USER"),
                    "按医嘱时间到点提醒")),
            new Contribution.EventTypeContribution(DOMAIN_CODE, DOMAIN_NAME,
                new MessagingDomainPlugin.EventTypeDef(
                    "EMERGENCY_ALERT_EVT", "紧急告警",
                    "HEALTHCARE", "医疗事件", "NEGATIVE",
                    "AlertTriangle", "#dc2626",
                    List.of("USER"),
                    "病人/设备紧急告警"))
        );
    }
}
