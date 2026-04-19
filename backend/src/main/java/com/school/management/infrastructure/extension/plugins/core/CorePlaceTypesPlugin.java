package com.school.management.infrastructure.extension.plugins.core;

import com.school.management.infrastructure.extension.EntityTypePlugin;
import com.school.management.infrastructure.extension.FieldDefinition;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 通用场所类型 — 任何行业部署都会用到的场所骨架.
 *
 * 多个类型打包在一个文件里 (Java 允许多个 package-private 顶级类).
 * 每个类型一个 @Component,让 PluginRegistrar 的 List<EntityTypePlugin> 正常扫描,
 * 且 getClass().getName() 能写出可读的 plugin_class.
 *
 * 行业扩展(教育/医疗)在各自的 *PlacesPlugin 里声明垂直场所.
 */
public class CorePlaceTypesPlugin {
    // marker file — 实际插件定义见同目录下的各 Core*Plugin 类
}

@Component
class CoreBuildingPlugin implements EntityTypePlugin {
    public String getEntityType() { return "PLACE"; }
    public String getTypeCode() { return "BUILDING"; }
    public String getTypeName() { return "建筑"; }
    public String getCategory() { return "BUILDING"; }
    public List<FieldDefinition> getSystemFields() { return List.of(); }
    public Map<String, Object> getUiConfig() { return Map.of("icon", "building", "color", "#6b7280"); }
}

@Component
class CoreFloorPlugin implements EntityTypePlugin {
    public String getEntityType() { return "PLACE"; }
    public String getTypeCode() { return "FLOOR"; }
    public String getTypeName() { return "楼层"; }
    public String getCategory() { return "FLOOR"; }
    public List<FieldDefinition> getSystemFields() { return List.of(); }
    public Map<String, Object> getUiConfig() { return Map.of("icon", "layers", "color", "#6b7280"); }
}

@Component
class CoreRoomPlugin implements EntityTypePlugin {
    public String getEntityType() { return "PLACE"; }
    public String getTypeCode() { return "ROOM"; }
    public String getTypeName() { return "房间"; }
    public String getCategory() { return "ROOM"; }
    public List<FieldDefinition> getSystemFields() { return List.of(); }
    public Map<String, Object> getUiConfig() { return Map.of("icon", "door-open", "color", "#6b7280"); }
}

@Component
class CoreAreaPlugin implements EntityTypePlugin {
    public String getEntityType() { return "PLACE"; }
    public String getTypeCode() { return "AREA"; }
    public String getTypeName() { return "区域"; }
    public String getCategory() { return "AREA"; }
    public List<FieldDefinition> getSystemFields() { return List.of(); }
    public Map<String, Object> getUiConfig() { return Map.of("icon", "map", "color", "#6b7280"); }
}

@Component
class CoreSitePlugin implements EntityTypePlugin {
    public String getEntityType() { return "PLACE"; }
    public String getTypeCode() { return "SITE"; }
    public String getTypeName() { return "场地"; }
    public String getCategory() { return "AREA"; }
    public List<FieldDefinition> getSystemFields() { return List.of(); }
    public Map<String, Object> getUiConfig() { return Map.of("icon", "map-pin", "color", "#6b7280"); }
}

@Component
class CorePointPlugin implements EntityTypePlugin {
    public String getEntityType() { return "PLACE"; }
    public String getTypeCode() { return "POINT"; }
    public String getTypeName() { return "点位"; }
    public String getCategory() { return "POINT"; }
    public List<FieldDefinition> getSystemFields() { return List.of(); }
    public Map<String, Object> getUiConfig() { return Map.of("icon", "locate", "color", "#6b7280"); }
}
