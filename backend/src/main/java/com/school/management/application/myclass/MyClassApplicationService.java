package com.school.management.application.myclass;

import com.school.management.application.myclass.query.DormitoryDistributionDTO;
import com.school.management.application.myclass.query.MyClassDTO;
import com.school.management.application.myclass.query.MyClassOverviewDTO;
import com.school.management.application.myclass.query.MyClassStudentDTO;
import com.school.management.domain.student.model.SchoolClass;
import com.school.management.domain.student.repository.SchoolClassRepository;
import com.school.management.domain.place.model.aggregate.UniversalPlace;
import com.school.management.domain.place.model.entity.UniversalPlaceOccupant;
import com.school.management.domain.place.repository.UniversalPlaceOccupantRepository;
import com.school.management.domain.place.repository.UniversalPlaceRepository;
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
    private final UniversalPlaceRepository universalPlaceRepository;
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
    public MyClassOverviewDTO getClassOverview(Long orgUnitId, Long userId) {
        SchoolClass schoolClass = schoolClassRepository.findById(orgUnitId)
            .orElseThrow(() -> new BusinessException("班级不存在"));

        // 验证用户有权限访问该班级
        validateAccess(orgUnitId, userId);

        // 获取学生统计
        long studentCount = studentRepository.countByClassId(orgUnitId);

        // 使用DDD仓储按性别统计
        long maleCount = studentRepository.countByClassIdAndGender(orgUnitId, Gender.MALE);
        long femaleCount = studentRepository.countByClassIdAndGender(orgUnitId, Gender.FEMALE);

        return MyClassOverviewDTO.builder()
            .orgUnitId(orgUnitId)
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
    public List<MyClassStudentDTO> getClassStudents(Long orgUnitId, Long userId, String keyword, String status) {
        validateAccess(orgUnitId, userId);

        // 使用DDD StudentRepository获取学生
        List<Student> students = studentRepository.findByClassId(orgUnitId);

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
     * 使用 UniversalPlace 领域: 通过 orgUnitId 查询分配给班级的场所, PlaceOccupant 获取入住学生
     */
    public List<DormitoryDistributionDTO> getDormitoryDistribution(Long orgUnitId, Long userId) {
        log.info("获取班级宿舍分布 - classId: {}, userId: {}", orgUnitId, userId);
        validateAccess(orgUnitId, userId);

        // 1. 查询分配给该班级组织单元的场所（即 orgUnitId = classId 的宿舍）
        List<UniversalPlace> assignedPlaces = universalPlaceRepository.findByOrgUnitId(orgUnitId);

        log.info("班级 {} 分配的场所数量: {}", orgUnitId, assignedPlaces.size());

        if (assignedPlaces.isEmpty()) {
            log.warn("班级 {} 没有分配任何宿舍场所", orgUnitId);
            return new ArrayList<>();
        }

        // 2. 构建场所Map，收集父节点ID（作为楼栋）
        Map<Long, UniversalPlace> placeMap = new HashMap<>();
        Set<Long> parentIds = new HashSet<>();
        for (UniversalPlace place : assignedPlaces) {
            placeMap.put(place.getId(), place);
            if (place.getParentId() != null) {
                parentIds.add(place.getParentId());
            }
        }

        // 3. 查询父节点信息（楼栋/楼层），并继续上溯找到楼栋
        Map<Long, UniversalPlace> ancestorMap = new HashMap<>();
        for (Long parentId : parentIds) {
            universalPlaceRepository.findById(parentId).ifPresent(parent -> {
                ancestorMap.put(parentId, parent);
                // 如果父级还有上级（即房间->楼层->楼栋），继续查一层
                if (parent.getParentId() != null) {
                    universalPlaceRepository.findById(parent.getParentId())
                        .ifPresent(grandParent -> ancestorMap.put(grandParent.getId(), grandParent));
                }
            });
        }

        // 4. 查询每个宿舍的在住学生
        Map<Long, List<UniversalPlaceOccupant>> occupantsByPlace = new HashMap<>();
        for (UniversalPlace place : assignedPlaces) {
            try {
                List<UniversalPlaceOccupant> occupants = placeOccupantRepository.findActiveByPlaceId(place.getId());
                occupantsByPlace.put(place.getId(), occupants);
            } catch (Exception e) {
                log.warn("Failed to load occupants for place {}: {}", place.getId(), e.getMessage());
                occupantsByPlace.put(place.getId(), new ArrayList<>());
            }
        }

        // 5. 找到每个房间的楼栋（向上遍历层级），按楼栋分组
        Map<Long, List<UniversalPlace>> roomsByBuilding = new HashMap<>();
        Map<Long, UniversalPlace> buildingMap = new HashMap<>();

        for (UniversalPlace room : assignedPlaces) {
            // 向上找到楼栋（level较低的祖先）
            Long buildingId = findBuildingId(room, ancestorMap);
            if (buildingId != null) {
                UniversalPlace building = ancestorMap.get(buildingId);
                if (building != null) {
                    buildingMap.put(buildingId, building);
                }
            } else {
                buildingId = 0L; // 未知楼栋
            }
            roomsByBuilding.computeIfAbsent(buildingId, k -> new ArrayList<>()).add(room);
        }

        log.info("按楼栋分组后的楼栋数量: {}", roomsByBuilding.size());

        // 6. 组装结果
        List<DormitoryDistributionDTO> result = new ArrayList<>();

        for (Map.Entry<Long, List<UniversalPlace>> entry : roomsByBuilding.entrySet()) {
            Long buildingId = entry.getKey();
            List<UniversalPlace> buildingRooms = entry.getValue();
            UniversalPlace building = buildingMap.get(buildingId);

            String buildingName = building != null ? building.getPlaceName() : "未知楼栋";

            // 推断楼栋类型（从房间性别限制）
            String buildingType = "MIXED";
            Set<String> genderTypes = buildingRooms.stream()
                .map(UniversalPlace::getGender)
                .filter(g -> g != null && !"MIXED".equals(g))
                .collect(Collectors.toSet());
            if (genderTypes.size() == 1) {
                buildingType = genderTypes.iterator().next();
            }

            // 组装房间列表
            List<DormitoryDistributionDTO.DormitoryRoomDTO> rooms = new ArrayList<>();
            int totalStudents = 0;

            for (UniversalPlace room : buildingRooms) {
                List<UniversalPlaceOccupant> roomOccupants = occupantsByPlace.getOrDefault(room.getId(), new ArrayList<>());
                totalStudents += roomOccupants.size();

                List<DormitoryDistributionDTO.StudentBedDTO> studentBeds = roomOccupants.stream()
                    .map(o -> DormitoryDistributionDTO.StudentBedDTO.builder()
                        .id(o.getOccupantId())
                        .name(o.getOccupantName())
                        .bedNo(o.getPositionNo())
                        .build())
                    .collect(Collectors.toList());

                // 从 attributes 中获取 roomNo 和 floorNumber
                Object roomNoAttr = room.getAttribute("roomNo");
                String roomNoStr = roomNoAttr != null ? String.valueOf(roomNoAttr) : room.getPlaceCode();
                Object floorAttr = room.getAttribute("floorNumber");
                Integer floorNumber = floorAttr instanceof Number ? ((Number) floorAttr).intValue() : null;

                rooms.add(DormitoryDistributionDTO.DormitoryRoomDTO.builder()
                    .dormitoryId(room.getId())
                    .roomNo(roomNoStr)
                    .floor(floorNumber)
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

    /**
     * 从房间向上遍历祖先，找到楼栋节点的ID
     * 策略：向上找 level 最小的非根祖先（通常 level=1 是楼栋）
     */
    private Long findBuildingId(UniversalPlace room, Map<Long, UniversalPlace> ancestorMap) {
        Long parentId = room.getParentId();
        if (parentId == null) return null;

        UniversalPlace parent = ancestorMap.get(parentId);
        if (parent == null) return parentId; // 无法解析，返回直接父级

        // 如果父级还有父级，则父级可能是楼层，祖父级是楼栋
        if (parent.getParentId() != null) {
            UniversalPlace grandParent = ancestorMap.get(parent.getParentId());
            if (grandParent != null) {
                return grandParent.getId(); // 祖父级 = 楼栋
            }
        }

        return parentId; // 父级 = 楼栋
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
                Optional<UniversalPlace> dormPlace = universalPlaceRepository.findById(occ.getPlaceId());
                if (dormPlace.isPresent()) {
                    UniversalPlace place = dormPlace.get();
                    // 尝试查询父节点（楼栋）获取完整名称
                    if (place.getParentId() != null) {
                        Optional<UniversalPlace> parentPlace = universalPlaceRepository.findById(place.getParentId());
                        if (parentPlace.isPresent()) {
                            // 如果父级还有父级，则父级是楼层，再上一层是楼栋
                            UniversalPlace parent = parentPlace.get();
                            if (parent.getParentId() != null) {
                                Optional<UniversalPlace> grandParent = universalPlaceRepository.findById(parent.getParentId());
                                dormitoryName = grandParent.map(gp -> gp.getPlaceName() + " " + place.getPlaceName())
                                    .orElse(place.getPlaceName());
                            } else {
                                dormitoryName = parent.getPlaceName() + " " + place.getPlaceName();
                            }
                        } else {
                            dormitoryName = place.getPlaceName();
                        }
                    } else {
                        dormitoryName = place.getPlaceName();
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

    private void validateAccess(Long orgUnitId, Long userId) {
        List<SchoolClass> userClasses = schoolClassRepository.findByTeacherId(userId);
        boolean hasAccess = userClasses.stream()
            .anyMatch(c -> c.getId().equals(orgUnitId));
        if (!hasAccess) {
            throw new BusinessException("无权访问该班级");
        }
    }
}
