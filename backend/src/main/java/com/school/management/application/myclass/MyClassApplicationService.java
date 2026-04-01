package com.school.management.application.myclass;

import com.school.management.application.myclass.query.DormitoryDistributionDTO;
import com.school.management.application.myclass.query.MyClassDTO;
import com.school.management.application.myclass.query.MyClassOverviewDTO;
import com.school.management.application.myclass.query.MyClassStudentDTO;
import com.school.management.domain.organization.model.SchoolClass;
import com.school.management.domain.organization.repository.SchoolClassRepository;
import com.school.management.domain.place.model.aggregate.Place;
import com.school.management.domain.place.model.entity.PlaceClassAssignment;
import com.school.management.domain.place.model.entity.UniversalPlaceOccupant;
import com.school.management.domain.place.model.valueobject.GenderType;
import com.school.management.domain.place.repository.PlaceClassAssignmentRepository;
import com.school.management.domain.place.repository.UniversalPlaceOccupantRepository;
import com.school.management.domain.place.repository.PlaceRepository;
import com.school.management.domain.student.model.aggregate.Student;
import com.school.management.domain.student.model.valueobject.Gender;
import com.school.management.domain.student.model.valueobject.StudentStatus;
import com.school.management.domain.student.repository.StudentRepository;
import com.school.management.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 我的班级应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MyClassApplicationService {

    private final SchoolClassRepository schoolClassRepository;
    private final StudentRepository studentRepository;
    private final PlaceClassAssignmentRepository placeClassAssignmentRepository;
    private final PlaceRepository placeRepository;
    private final UniversalPlaceOccupantRepository placeOccupantRepository;

    /**
     * 获取当前用户管理的班级列表
     */
    public List<MyClassDTO> getMyClasses(Long userId) {
        List<SchoolClass> classes = schoolClassRepository.findByTeacherId(userId);

        return classes.stream()
            .map(c -> toMyClassDTO(c, userId))
            .collect(Collectors.toList());
    }

    /**
     * 获取班级概览数据
     */
    public MyClassOverviewDTO getClassOverview(Long classId, Long userId) {
        SchoolClass schoolClass = schoolClassRepository.findById(classId)
            .orElseThrow(() -> new BusinessException("班级不存在"));

        // 验证用户有权限访问该班级
        validateAccess(classId, userId);

        // 获取学生统计
        long studentCount = studentRepository.countByClassId(classId);

        // 使用DDD仓储按性别统计
        long maleCount = studentRepository.countByClassIdAndGender(classId, Gender.MALE);
        long femaleCount = studentRepository.countByClassIdAndGender(classId, Gender.FEMALE);

        return MyClassOverviewDTO.builder()
            .classId(classId)
            .className(schoolClass.getClassName())
            .studentCount((int) studentCount)
            .maleCount((int) maleCount)
            .femaleCount((int) femaleCount)
            .classRank(0) // TODO: 从检查记录服务获取
            .totalClasses(0)
            .averageScore(0.0)
            .scoreTrend(0.0)
            .pendingAppeals(0)
            .scoreTrendList(new ArrayList<>())
            .recentRecords(new ArrayList<>())
            .build();
    }

    /**
     * 获取班级学生列表
     */
    public List<MyClassStudentDTO> getClassStudents(Long classId, Long userId, String keyword, String status) {
        validateAccess(classId, userId);

        // 使用DDD StudentRepository获取学生
        List<Student> students = studentRepository.findByClassId(classId);

        return students.stream()
            .filter(s -> {
                if (keyword != null && !keyword.isEmpty()) {
                    String name = s.getName() != null ? s.getName() : "";
                    String studentNo = s.getStudentNo() != null ? s.getStudentNo() : "";
                    return name.contains(keyword) || studentNo.contains(keyword);
                }
                return true;
            })
            .filter(s -> {
                if (status != null && !status.isEmpty()) {
                    StudentStatus studentStatus = s.getStatus();
                    // status code: 1在读 2休学 3退学 4毕业 5开除
                    return studentStatus != null && String.valueOf(studentStatus.getCode()).equals(status);
                }
                return true;
            })
            .map(this::toStudentDTOFromDomain)
            .collect(Collectors.toList());
    }

    /**
     * 获取班级宿舍分布
     * 使用DDD Place领域: PlaceClassAssignment查询班级-宿舍绑定, Place获取房间/楼栋信息, PlaceOccupant获取入住学生
     */
    public List<DormitoryDistributionDTO> getDormitoryDistribution(Long classId, Long userId) {
        log.info("获取班级宿舍分布 - classId: {}, userId: {}", classId, userId);
        validateAccess(classId, userId);

        // 1. 查询班级的场所分配关系 (PlaceClassAssignment)
        List<PlaceClassAssignment> assignments = placeClassAssignmentRepository.findByClassId(classId);

        log.info("班级 {} 的场所分配记录数: {}", classId, assignments.size());
        if (!assignments.isEmpty()) {
            log.info("分配的场所ID列表: {}", assignments.stream()
                .map(PlaceClassAssignment::getPlaceId)
                .collect(Collectors.toList()));
        }

        if (assignments.isEmpty()) {
            log.warn("班级 {} 没有分配任何宿舍场所", classId);
            return new ArrayList<>();
        }

        // 2. 获取所有分配的场所(房间)
        List<Long> placeIds = assignments.stream()
            .map(PlaceClassAssignment::getPlaceId)
            .collect(Collectors.toList());

        Map<Long, Place> placeMap = new HashMap<>();
        for (Long placeId : placeIds) {
            placeRepository.findById(placeId).ifPresent(place -> placeMap.put(placeId, place));
        }
        log.info("查询到宿舍场所数量: {}", placeMap.size());

        if (placeMap.isEmpty()) {
            log.warn("未找到有效的宿舍场所");
            return new ArrayList<>();
        }

        // 3. 收集所有楼栋ID，查询楼栋(building)信息
        Set<Long> buildingIds = placeMap.values().stream()
            .map(Place::getBuildingId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        Map<Long, Place> buildingMap = new HashMap<>();
        for (Long buildingId : buildingIds) {
            placeRepository.findById(buildingId).ifPresent(building -> buildingMap.put(buildingId, building));
        }

        // 4. 查询每个宿舍(place)的在住学生
        Map<Long, List<UniversalPlaceOccupant>> occupantsByPlace = new HashMap<>();
        for (Long placeId : placeIds) {
            try {
                List<UniversalPlaceOccupant> occupants = placeOccupantRepository.findActiveByPlaceId(placeId);
                occupantsByPlace.put(placeId, occupants);
            } catch (Exception e) {
                log.warn("Failed to load occupants for place {}: {}", placeId, e.getMessage());
                occupantsByPlace.put(placeId, new ArrayList<>());
            }
        }

        // 5. 按楼栋分组
        Map<Long, List<Place>> roomsByBuilding = placeMap.values().stream()
            .filter(s -> s.getBuildingId() != null)
            .collect(Collectors.groupingBy(Place::getBuildingId));

        log.info("按楼栋分组后的楼栋数量: {}", roomsByBuilding.size());

        // 处理没有buildingId的场所 - 归入"未知楼栋"
        List<Place> roomsWithoutBuilding = placeMap.values().stream()
            .filter(s -> s.getBuildingId() == null)
            .collect(Collectors.toList());

        if (!roomsWithoutBuilding.isEmpty()) {
            log.warn("有 {} 个宿舍场所没有buildingId，将归入'未知楼栋'", roomsWithoutBuilding.size());
            roomsByBuilding.put(0L, roomsWithoutBuilding);
        }

        if (roomsByBuilding.isEmpty()) {
            log.warn("没有任何宿舍数据可供显示");
        }

        // 6. 组装结果
        List<DormitoryDistributionDTO> result = new ArrayList<>();

        for (Map.Entry<Long, List<Place>> entry : roomsByBuilding.entrySet()) {
            Long buildingId = entry.getKey();
            List<Place> buildingRooms = entry.getValue();
            Place building = buildingMap.get(buildingId);

            String buildingName = building != null ? building.getPlaceName() : "未知楼栋";

            // 推断楼栋类型（从房间性别类型）
            String buildingType = "MIXED";
            Set<GenderType> genderTypes = buildingRooms.stream()
                .map(Place::getGenderType)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
            if (genderTypes.size() == 1) {
                GenderType gt = genderTypes.iterator().next();
                if (gt == GenderType.MALE) buildingType = "MALE";
                else if (gt == GenderType.FEMALE) buildingType = "FEMALE";
            }

            // 组装房间列表
            List<DormitoryDistributionDTO.DormitoryRoomDTO> rooms = new ArrayList<>();
            int totalStudents = 0;

            for (Place room : buildingRooms) {
                List<UniversalPlaceOccupant> roomOccupants = occupantsByPlace.getOrDefault(room.getId(), new ArrayList<>());
                totalStudents += roomOccupants.size();

                List<DormitoryDistributionDTO.StudentBedDTO> studentBeds = roomOccupants.stream()
                    .map(o -> DormitoryDistributionDTO.StudentBedDTO.builder()
                        .id(o.getOccupantId())
                        .name(o.getOccupantName())
                        .bedNo(o.getPositionNo())
                        .build())
                    .collect(Collectors.toList());

                rooms.add(DormitoryDistributionDTO.DormitoryRoomDTO.builder()
                    .dormitoryId(room.getId())
                    .roomNo(room.getRoomNo() != null ? String.valueOf(room.getRoomNo()) : null)
                    .floor(room.getFloorNumber())
                    .studentCount(roomOccupants.size())
                    .students(studentBeds)
                    .build());
            }

            // 按楼层和房间号排序
            rooms.sort(Comparator
                .comparing((DormitoryDistributionDTO.DormitoryRoomDTO r) -> r.getFloor() != null ? r.getFloor() : 0)
                .thenComparing(DormitoryDistributionDTO.DormitoryRoomDTO::getRoomNo));

            result.add(DormitoryDistributionDTO.builder()
                .buildingId(buildingId)
                .buildingName(buildingName)
                .buildingType(buildingType)
                .rooms(rooms)
                .studentCount(totalStudents)
                .build());
        }

        // 按楼栋名称排序
        result.sort(Comparator.comparing(DormitoryDistributionDTO::getBuildingName));

        return result;
    }

    private MyClassDTO toMyClassDTO(SchoolClass c, Long userId) {
        // 从 classes 表的 teacher_id 字段判断角色
        // 目前简化处理，如果查出来就是班主任
        String myRole = "HEAD_TEACHER";

        return MyClassDTO.builder()
            .id(c.getId())
            .classCode(c.getClassCode())
            .className(c.getClassName())
            .shortName(c.getShortName())
            .currentSize(c.getCurrentSize())
            .standardSize(c.getStandardSize())
            .status(c.getStatus() != null ? c.getStatus().name() : "ACTIVE")
            .enrollmentYear(c.getEnrollmentYear())
            .myRole(myRole)
            .weeklyRank(0)
            .totalClasses(0)
            .weeklyScore(0.0)
            .scoreTrend(new ArrayList<>())
            .build();
    }

    /**
     * 从DDD Student domain对象转换为MyClassStudentDTO
     * 宿舍信息通过 place_occupants 查询（统一场所管理体系）
     */
    private MyClassStudentDTO toStudentDTOFromDomain(Student s) {
        // 通过 place_occupants 查询学生当前入住的宿舍
        String dormitoryName = null;
        String bedNo = null;
        try {
            Optional<UniversalPlaceOccupant> activeOccupancy =
                placeOccupantRepository.findActiveByOccupant("STUDENT", s.getId());
            if (activeOccupancy.isPresent()) {
                UniversalPlaceOccupant occ = activeOccupancy.get();
                bedNo = occ.getPositionNo();
                // 查询场所名称
                Optional<Place> dormPlace = placeRepository.findById(occ.getPlaceId());
                if (dormPlace.isPresent()) {
                    Place place = dormPlace.get();
                    if (place.getBuildingId() != null) {
                        Optional<Place> buildingPlace = placeRepository.findById(place.getBuildingId());
                        if (buildingPlace.isPresent()) {
                            dormitoryName = buildingPlace.get().getPlaceName() + " " + place.getRoomNo();
                        } else {
                            dormitoryName = String.valueOf(place.getRoomNo());
                        }
                    } else {
                        dormitoryName = place.getRoomNo() != null ? String.valueOf(place.getRoomNo()) : place.getPlaceName();
                    }
                }
            }
        } catch (Exception e) {
            log.debug("查询学生宿舍信息失败: studentId={}, error={}", s.getId(), e.getMessage());
        }

        // 状态映射
        String status = "ENROLLED";
        if (s.getStatus() != null) {
            switch (s.getStatus()) {
                case STUDYING: status = "ENROLLED"; break;
                case SUSPENDED: status = "SUSPENDED"; break;
                case WITHDRAWN: status = "DROPPED_OUT"; break;
                case GRADUATED: status = "GRADUATED"; break;
                case EXPELLED: status = "EXPELLED"; break;
                default: status = "ENROLLED";
            }
        }

        // 性别转换
        String gender = null;
        if (s.getGender() != null) {
            gender = s.getGender().getDescription();
        }

        return MyClassStudentDTO.builder()
            .id(s.getId())
            .studentNo(s.getStudentNo())
            .name(s.getName())
            .gender(gender)
            .phone(s.getPhone())
            .dormitoryName(dormitoryName)
            .bedNo(bedNo)
            .status(status)
            .build();
    }

    private void validateAccess(Long classId, Long userId) {
        List<SchoolClass> userClasses = schoolClassRepository.findByTeacherId(userId);
        boolean hasAccess = userClasses.stream()
            .anyMatch(c -> c.getId().equals(classId));
        if (!hasAccess) {
            throw new BusinessException("无权访问该班级");
        }
    }
}
