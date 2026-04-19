package com.school.management.infrastructure.extension.plugins.healthcare;

import com.school.management.infrastructure.extension.EntityTypePlugin;
import com.school.management.infrastructure.extension.FieldDefinition;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 病人 (USER 子类型) — 医疗行业最基础的身份.
 */
@Component
@SuppressWarnings("deprecation")
public class PatientPlugin implements EntityTypePlugin {

    @Override public String getEntityType() { return "USER"; }
    @Override public String getTypeCode() { return "PATIENT"; }
    @Override public String getTypeName() { return "病人"; }
    @Override public String getCategory() { return "MEMBER"; }

    @Override
    public List<FieldDefinition> getSystemFields() {
        return List.of(
            FieldDefinition.of("medicalRecordNo", "病历号", "text", "基本信息", true, Map.of()),
            FieldDefinition.of("admittedDate", "入院日期", "date", "就诊信息", false, Map.of()),
            FieldDefinition.of("wardId", "病区", "relation", "就诊信息", false,
                Map.of("target", "org_units", "labelField", "unitName")),
            FieldDefinition.of("emergencyContact", "紧急联系人", "text", "家庭信息", false, Map.of())
        );
    }

    @Override
    public Map<String, Boolean> getFeatures() {
        return Map.of(
            "isExternal", true,
            "hasGuardian", true,
            "canLogin", false
        );
    }

    @Override
    public Map<String, Object> getUiConfig() {
        return Map.of("icon", "heart-pulse", "color", "#dc2626");
    }
}
