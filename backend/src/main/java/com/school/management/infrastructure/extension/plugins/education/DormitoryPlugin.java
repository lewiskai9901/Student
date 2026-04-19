package com.school.management.infrastructure.extension.plugins.education;

import com.school.management.infrastructure.extension.EntityTypePlugin;
import com.school.management.infrastructure.extension.ExtensionContext;
import com.school.management.infrastructure.extension.FieldDefinition;
import com.school.management.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class DormitoryPlugin implements EntityTypePlugin {

    private final JdbcTemplate jdbc;

    public String getEntityType() { return "PLACE"; }
    public String getTypeCode() { return "DORM_ROOM"; }
    public String getTypeName() { return "宿舍"; }
    public String getCategory() { return "ROOM"; }
    public String getParentTypeCode() { return "DORM_FLOOR"; }

    public List<FieldDefinition> getSystemFields() {
        // 注: 床位数/性别限制 复用系统字段 places.capacity / places.gender,不在此声明
        return List.of(
            FieldDefinition.of("floorNumber", "楼层", "number", "位置信息", false, Map.of()),
            FieldDefinition.of("roomManager", "舍长", "user", "人员配置", false,
                Map.of("role", "STUDENT"))
        );
    }

    public Map<String, Boolean> getFeatures() {
        return Map.of("hasCapacity", true, "hasOccupancy", true, "hasGender", true);
    }

    public Map<String, Object> getUiConfig() {
        return Map.of("icon", "bed-double", "color", "#0d9488");
    }

    @Override
    public void beforeDelete(ExtensionContext ctx) {
        try {
            Long occupants = jdbc.queryForObject(
                "SELECT COUNT(*) FROM place_occupant_records WHERE place_id = ? AND status = 'CHECKED_IN' AND deleted = 0",
                Long.class, ctx.getEntityId());
            if (occupants != null && occupants > 0) {
                throw new BusinessException("宿舍内有 " + occupants + " 名在住人员，无法删除");
            }
        } catch (BusinessException e) { throw e; }
        catch (Exception ignored) {}
    }

    // capacity 由系统层保证非空,无需插件再校验
}
