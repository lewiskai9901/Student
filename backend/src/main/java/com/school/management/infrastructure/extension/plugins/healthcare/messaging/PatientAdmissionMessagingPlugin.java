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
 * Track M3 reference: HEALTH 入院/出院消息插件 — 已从 {@code MessagingDomainPlugin}
 * (旧 @Deprecated 三合一 SPI) 迁移至 {@link PluginPackage#contribute()} + 细粒度
 * {@link Contribution.TriggerPointContribution} / {@link Contribution.EventTypeContribution}.
 *
 * <h3>本插件声明</h3>
 * <ul>
 *   <li>2 个触发点: PATIENT_ADMITTED / PATIENT_DISCHARGED (业务代码 fire 用)</li>
 *   <li>2 个事件类型: PATIENT_ADMITTED_EVT / PATIENT_DISCHARGED_EVT (UI 消息分类)</li>
 * </ul>
 *
 * <h3>为何不声明默认触发器 (DefaultTriggerDef)</h3>
 * 触发点→事件类型的绑定属于 admin 运行时配置语义 (多个事件类型可绑同触发点,
 * 管理员随时可改), 不适合编码强绑. 若要开箱即用, 放到 DB seed 或 admin UI
 * "一键启用" 按钮, 不走 SPI.
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
            // 2 个触发点
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

            // 2 个事件类型
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
                    "病人出院"))
        );
    }
}
