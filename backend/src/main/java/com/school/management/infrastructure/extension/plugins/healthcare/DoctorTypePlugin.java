package com.school.management.infrastructure.extension.plugins.healthcare;

import com.school.management.infrastructure.extension.EntityTypePlugin;
import com.school.management.infrastructure.extension.FieldDefinition;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 医师 (USER 子类型) — 医疗行业医护角色, 可开具医嘱.
 */
@Component
@SuppressWarnings("deprecation")
public class DoctorTypePlugin implements EntityTypePlugin {

    @Override public String getEntityType() { return "USER"; }
    @Override public String getTypeCode() { return "DOCTOR"; }
    @Override public String getTypeName() { return "医师"; }
    @Override public String getCategory() { return "STAFF"; }

    @Override
    public List<FieldDefinition> getSystemFields() {
        return List.of(
            FieldDefinition.of("doctorNo", "工号", "text", "基本信息", true, Map.of()),
            FieldDefinition.of("licenseNo", "执业证号", "text", "资质", true, Map.of()),
            FieldDefinition.of("specialty", "专科", "text", "资质", false, Map.of()),
            FieldDefinition.of("title", "职称", "text", "资质", false, Map.of())
        );
    }

    @Override
    public Map<String, Boolean> getFeatures() {
        return Map.of(
            "canLogin", true,
            "isStaff", true,
            "canPrescribe", true
        );
    }

    @Override
    public Map<String, Object> getUiConfig() {
        return Map.of("icon", "user-md", "color", "#7c3aed");
    }
}
