package com.school.management.infrastructure.extension.plugins.education;

import com.school.management.infrastructure.extension.EntityTypePlugin;
import com.school.management.infrastructure.extension.FieldDefinition;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class SchoolPlugin implements EntityTypePlugin {
    public String getEntityType() { return "ORG_UNIT"; }
    public String getTypeCode() { return "SCHOOL"; }
    public String getTypeName() { return "学校"; }
    public String getCategory() { return "ROOT"; }
    public List<String> getAllowedChildTypeCodes() { return List.of("DEPARTMENT", "ADMIN_OFFICE"); }

    public List<FieldDefinition> getSystemFields() {
        return List.of(
            FieldDefinition.of("schoolLevel", "办学层次", "select", "基本信息", false,
                Map.of("options", List.of(
                    Map.of("value", "PRIMARY", "label", "小学"),
                    Map.of("value", "JUNIOR", "label", "初中"),
                    Map.of("value", "SENIOR", "label", "高中"),
                    Map.of("value", "VOCATIONAL", "label", "中职"),
                    Map.of("value", "COLLEGE", "label", "高职/大专"),
                    Map.of("value", "UNIVERSITY", "label", "本科及以上")))),
            FieldDefinition.of("principalName", "校长", "user", "人员配置", false, Map.of()),
            FieldDefinition.of("address", "学校地址", "text", "基本信息", false, Map.of()),
            FieldDefinition.of("phone", "联系电话", "text", "基本信息", false, Map.of())
        );
    }

    public Map<String, Object> getUiConfig() {
        return Map.of("icon", "building-2", "color", "#111827");
    }
}
