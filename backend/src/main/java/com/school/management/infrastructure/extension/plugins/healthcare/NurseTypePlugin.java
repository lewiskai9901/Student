package com.school.management.infrastructure.extension.plugins.healthcare;

import com.school.management.infrastructure.extension.EntityTypePlugin;
import com.school.management.infrastructure.extension.FieldDefinition;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 护士 (USER 子类型) — 医疗行业医护角色.
 */
@Component
@SuppressWarnings("deprecation")
public class NurseTypePlugin implements EntityTypePlugin {

    @Override public String getEntityType() { return "USER"; }
    @Override public String getTypeCode() { return "NURSE"; }
    @Override public String getTypeName() { return "护士"; }
    @Override public String getCategory() { return "STAFF"; }

    @Override
    public List<FieldDefinition> getSystemFields() {
        return List.of(
            FieldDefinition.of("nurseNo", "工号", "text", "基本信息", true, Map.of()),
            FieldDefinition.of("licenseNo", "执业证号", "text", "资质", false, Map.of()),
            FieldDefinition.of("wardId", "所属病区", "relation", "执业信息", false,
                Map.of("target", "org_units", "labelField", "unitName"))
        );
    }

    @Override
    public Map<String, Boolean> getFeatures() {
        return Map.of(
            "canLogin", true,
            "isStaff", true,
            "canCare", true
        );
    }

    @Override
    public Map<String, Object> getUiConfig() {
        return Map.of("icon", "stethoscope", "color", "#0ea5e9");
    }
}
