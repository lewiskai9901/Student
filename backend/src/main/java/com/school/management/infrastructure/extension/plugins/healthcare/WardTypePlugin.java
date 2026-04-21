package com.school.management.infrastructure.extension.plugins.healthcare;

import com.school.management.infrastructure.extension.EntityTypePlugin;
import com.school.management.infrastructure.extension.FieldDefinition;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 病区 (ORG_UNIT 子类型) — 医疗行业基础组织单元.
 */
@Component
@SuppressWarnings("deprecation")
public class WardTypePlugin implements EntityTypePlugin {

    @Override public String getEntityType() { return "ORG_UNIT"; }
    @Override public String getTypeCode() { return "WARD"; }
    @Override public String getTypeName() { return "病区"; }
    @Override public String getCategory() { return "STRUCTURE"; }

    @Override
    public List<FieldDefinition> getSystemFields() {
        return List.of(
            FieldDefinition.of("bedCount", "床位数", "number", "基本信息", true, Map.of()),
            FieldDefinition.of("specialty", "专科方向", "text", "基本信息", false, Map.of()),
            FieldDefinition.of("floor", "楼层", "text", "位置", false, Map.of())
        );
    }

    @Override
    public Map<String, Object> getUiConfig() {
        return Map.of("icon", "hospital", "color", "#0ea5e9");
    }
}
