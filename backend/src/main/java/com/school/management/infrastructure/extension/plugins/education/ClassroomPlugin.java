package com.school.management.infrastructure.extension.plugins.education;

import com.school.management.infrastructure.extension.EntityTypePlugin;
import com.school.management.infrastructure.extension.FieldDefinition;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class ClassroomPlugin implements EntityTypePlugin {

    public String getEntityType() { return "PLACE"; }
    public String getTypeCode() { return "CLASSROOM"; }
    public String getTypeName() { return "教室"; }
    public String getCategory() { return "ROOM"; }

    public List<FieldDefinition> getSystemFields() {
        return List.of(
            FieldDefinition.of("roomFunction", "教室功能", "select", "基本信息", false,
                Map.of("options", List.of(
                    Map.of("value", "NORMAL", "label", "普通教室"),
                    Map.of("value", "MULTIMEDIA", "label", "多媒体教室"),
                    Map.of("value", "LAB", "label", "实验室"),
                    Map.of("value", "COMPUTER", "label", "机房")))),
            FieldDefinition.of("hasProjector", "有投影仪", "boolean", "设施配置", false, Map.of()),
            FieldDefinition.of("hasAC", "有空调", "boolean", "设施配置", false, Map.of()),
            FieldDefinition.of("examCapacity", "考试容量", "number", "考试配置", false,
                Map.of("min", 0, "max", 500))
        );
    }

    public Map<String, Object> getUiConfig() {
        return Map.of("icon", "building", "color", "#d97706");
    }
}
