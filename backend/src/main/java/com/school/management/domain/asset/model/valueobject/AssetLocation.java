package com.school.management.domain.asset.model.valueobject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 资产位置值对象
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetLocation {

    private LocationType locationType;
    private Long locationId;
    private String locationName;

    public boolean isEmpty() {
        return locationType == null && locationId == null;
    }

    public static AssetLocation empty() {
        return new AssetLocation(null, null, null);
    }

    public static AssetLocation of(LocationType type, Long id, String name) {
        return new AssetLocation(type, id, name);
    }
}
