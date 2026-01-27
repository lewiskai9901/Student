package com.school.management.application.myclass.query;

import lombok.Data;
import lombok.Builder;
import java.util.List;

/**
 * 宿舍分布信息 DTO
 */
@Data
@Builder
public class DormitoryDistributionDTO {
    private Long buildingId;
    private String buildingName;
    private String buildingType; // MALE, FEMALE, MIXED
    private List<DormitoryRoomDTO> rooms;
    private Integer studentCount;

    @Data
    @Builder
    public static class DormitoryRoomDTO {
        private Long dormitoryId;
        private String roomNo;
        private Integer floor;
        private Integer studentCount;
        private List<StudentBedDTO> students;
    }

    @Data
    @Builder
    public static class StudentBedDTO {
        private Long id;
        private String name;
        private String bedNo;
    }
}
