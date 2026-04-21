package com.school.management.infrastructure.extension.plugins.healthcare;

import com.school.management.infrastructure.extension.PluginConfigSchema;
import com.school.management.infrastructure.extension.PluginPackage;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 医疗行业插件包 (示例骨架) — 展示如何新增行业扩展.
 *
 * 当前规模: 1 个 EntityTypePlugin (Patient) + 1 个 MenuContributionPlugin,
 * 足以证明 CORE + EDU + HEALTH 三方共存时启动/治理 API/前端加载都正常.
 *
 * 真实医疗场景应继续扩展: 病历/药品/床位/排班/医嘱 等.
 */
@Component
public class HealthcareManifest implements PluginPackage {

    @Override
    public String getIndustryCode() { return "HEALTH"; }

    @Override
    public String getIndustryName() { return "医疗行业"; }

    @Override
    public List<String> getDependsOn() { return List.of("CORE"); }

    @Override
    public Map<String, String> getDependsOnWithVersion() {
        return Map.of("CORE", ">=1.0.0 <2.0.0");
    }

    @Override
    public boolean owns(Class<?> pluginClass) {
        return pluginClass.getPackageName().contains(".plugins.healthcare");
    }

    /**
     * Phase 7.5: 声明 HEALTH 插件可被管理员配置的字段.
     * 管理员 UI 会据此渲染表单, 值存到 tenant_plugin_enablement.config_json,
     * 业务代码 via TenantPluginService.getConfig(tenantId, "HEALTH", key) 读取.
     */
    @Override
    public PluginConfigSchema configSchema() {
        return new PluginConfigSchema(List.of(
            PluginConfigSchema.Field.stringField(
                "admissionWardPrefix", "入院病区前缀", "A-", false)
                .withDescription("新病人入院时自动生成的病区编号前缀. 如 A- 会产生 A-001, A-002..."),
            PluginConfigSchema.Field.booleanField(
                "autoAssignBed", "自动床位分配", true)
                .withDescription("入院时是否自动分配同病区空床位. 关闭则需要人工指定."),
            PluginConfigSchema.Field.enumField(
                "dischargeNotifyChannel", "出院通知渠道",
                List.of("NONE", "SMS", "WECHAT"), "SMS")
                .withDescription("病人出院时通过何种渠道通知家属."),
            PluginConfigSchema.Field.stringField(
                "hospitalName", "医院名称", "示例医院", false)
                .withDescription("模板/表头/通知签名等使用的医院名称."),
            PluginConfigSchema.Field.numberField(
                "wardMaxBedsDefault", "默认床位数", 20, false)
                .withDescription("新建病区时默认的床位数上限, 超过 MaxBedCapacityPolicy 会 BLOCK."),
            PluginConfigSchema.Field.stringField(
                "admissionAutonumberPrefix", "入院号前缀", "ADM", false)
                .withDescription("入院号自动编号前缀, 如 ADM20260421-0001."),
            PluginConfigSchema.Field.enumField(
                "genderPolicy", "性别分区策略",
                List.of("STRICT", "WARN", "NONE"), "WARN")
                .withDescription("STRICT=性别不符 BLOCK, WARN=仅警告, NONE=不检查.")
        ));
    }

    /** Phase 7.4 skeleton: HEALTH 目前 schema v1 (仅引入 user_patient 扩展表) */
    @Override
    public int schemaVersion() {
        return 1;
    }

    @Override
    public String schemaMigrationLocation() {
        return "classpath:db/migration/plugins/healthcare/";
    }
}
