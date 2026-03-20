package com.school.management.application.place.query;

import lombok.Data;

import java.util.List;

/**
 * 场所统计DTO
 */
@Data
public class PlaceStatisticsDTO {

    private int totalBuildings;
    private int totalRooms;
    private int totalCapacity;
    private int totalOccupancy;
    private double occupancyRate;

    private List<RoomTypeStats> byRoomType;
    private List<BuildingTypeStats> byBuildingType;

    @Data
    public static class RoomTypeStats {
        private String roomType;
        private String roomTypeName;
        private int count;
        private int capacity;
        private int occupancy;
        private double occupancyRate;
    }

    @Data
    public static class BuildingTypeStats {
        private String buildingType;
        private String buildingTypeName;
        private int count;
        private int roomCount;
    }
}
