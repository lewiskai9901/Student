package com.school.management.infrastructure.extension.plugins.education;

import com.school.management.infrastructure.extension.EntityTypePlugin;
import com.school.management.infrastructure.extension.FieldDefinition;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 教育行业专有场所类型 — 宿舍楼/宿舍层/教学楼/教学层/实训楼/实训层/实验室等.
 *
 * CLASSROOM / DORM_ROOM 另有独立插件 (含业务字段).
 */
public class EducationPlaceTypesPlugin {
    // marker
}

@Component
class EducationDormBuildingPlugin implements EntityTypePlugin {
    public String getEntityType() { return "PLACE"; }
    public String getTypeCode() { return "DORM_BUILDING"; }
    public String getTypeName() { return "宿舍楼"; }
    public String getCategory() { return "BUILDING"; }
    public List<FieldDefinition> getSystemFields() { return List.of(); }
    public Map<String, Object> getUiConfig() { return Map.of("icon", "building-2", "color", "#0891b2"); }
}

@Component
class EducationDormFloorPlugin implements EntityTypePlugin {
    public String getEntityType() { return "PLACE"; }
    public String getTypeCode() { return "DORM_FLOOR"; }
    public String getTypeName() { return "宿舍楼层"; }
    public String getCategory() { return "FLOOR"; }
    public List<FieldDefinition> getSystemFields() { return List.of(); }
    public Map<String, Object> getUiConfig() { return Map.of("icon", "layers", "color", "#0891b2"); }
}

@Component
class EducationTeachBuildingPlugin implements EntityTypePlugin {
    public String getEntityType() { return "PLACE"; }
    public String getTypeCode() { return "TYPE_TEACH_BLDG"; }
    public String getTypeName() { return "教学楼"; }
    public String getCategory() { return "BUILDING"; }
    public List<FieldDefinition> getSystemFields() { return List.of(); }
    public Map<String, Object> getUiConfig() { return Map.of("icon", "school", "color", "#d97706"); }
}

@Component
class EducationTeachFloorPlugin implements EntityTypePlugin {
    public String getEntityType() { return "PLACE"; }
    public String getTypeCode() { return "TYPE_TEACH_FLOOR"; }
    public String getTypeName() { return "教学楼层"; }
    public String getCategory() { return "FLOOR"; }
    public List<FieldDefinition> getSystemFields() { return List.of(); }
    public Map<String, Object> getUiConfig() { return Map.of("icon", "layers", "color", "#d97706"); }
}

@Component
class EducationTrainBuildingPlugin implements EntityTypePlugin {
    public String getEntityType() { return "PLACE"; }
    public String getTypeCode() { return "TYPE_TRAIN_BLDG"; }
    public String getTypeName() { return "实训楼"; }
    public String getCategory() { return "BUILDING"; }
    public List<FieldDefinition> getSystemFields() { return List.of(); }
    public Map<String, Object> getUiConfig() { return Map.of("icon", "factory", "color", "#b45309"); }
}

@Component
class EducationTrainFloorPlugin implements EntityTypePlugin {
    public String getEntityType() { return "PLACE"; }
    public String getTypeCode() { return "TYPE_TRAIN_FLOOR"; }
    public String getTypeName() { return "实训楼层"; }
    public String getCategory() { return "FLOOR"; }
    public List<FieldDefinition> getSystemFields() { return List.of(); }
    public Map<String, Object> getUiConfig() { return Map.of("icon", "layers", "color", "#b45309"); }
}

@Component
class EducationLabPlugin implements EntityTypePlugin {
    public String getEntityType() { return "PLACE"; }
    public String getTypeCode() { return "TYPE_LAB"; }
    public String getTypeName() { return "实验室"; }
    public String getCategory() { return "ROOM"; }
    public List<FieldDefinition> getSystemFields() { return List.of(); }
    public Map<String, Object> getUiConfig() { return Map.of("icon", "flask-conical", "color", "#7c3aed"); }
}

@Component
class EducationComputerLabPlugin implements EntityTypePlugin {
    public String getEntityType() { return "PLACE"; }
    public String getTypeCode() { return "TYPE_COMPUTER_LAB"; }
    public String getTypeName() { return "机房"; }
    public String getCategory() { return "ROOM"; }
    public List<FieldDefinition> getSystemFields() { return List.of(); }
    public Map<String, Object> getUiConfig() { return Map.of("icon", "monitor", "color", "#2563eb"); }
}

@Component
class EducationTrainingRoomPlugin implements EntityTypePlugin {
    public String getEntityType() { return "PLACE"; }
    public String getTypeCode() { return "TYPE_TRAINING"; }
    public String getTypeName() { return "实训室"; }
    public String getCategory() { return "ROOM"; }
    public List<FieldDefinition> getSystemFields() { return List.of(); }
    public Map<String, Object> getUiConfig() { return Map.of("icon", "wrench", "color", "#b45309"); }
}
