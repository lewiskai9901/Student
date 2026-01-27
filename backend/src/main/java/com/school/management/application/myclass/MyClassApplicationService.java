package com.school.management.application.myclass;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.school.management.application.myclass.query.DormitoryDistributionDTO;
import com.school.management.application.myclass.query.MyClassDTO;
import com.school.management.application.myclass.query.MyClassOverviewDTO;
import com.school.management.application.myclass.query.MyClassStudentDTO;
import com.school.management.domain.organization.model.SchoolClass;
import com.school.management.domain.organization.model.TeacherAssignment;
import com.school.management.domain.organization.repository.SchoolClassRepository;
import com.school.management.domain.student.model.aggregate.Student;
import com.school.management.domain.student.repository.StudentRepository;
import com.school.management.dto.StudentResponse;
import com.school.management.entity.Building;
import com.school.management.entity.ClassDormitory;
import com.school.management.entity.Dormitory;
import com.school.management.mapper.BuildingMapper;
import com.school.management.mapper.ClassDormitoryMapper;
import com.school.management.mapper.DormitoryMapper;
import com.school.management.mapper.StudentMapper;
import com.school.management.service.StudentDormitoryService;
import com.school.management.dto.StudentDormitoryResponse;
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
    private final StudentMapper studentMapper;
    private final ClassDormitoryMapper classDormitoryMapper;
    private final DormitoryMapper dormitoryMapper;
    private final BuildingMapper buildingMapper;
    private final StudentDormitoryService studentDormitoryService;

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
            .orElseThrow(() -> new RuntimeException("班级不存在"));

        // 验证用户有权限访问该班级
        validateAccess(classId, userId);

        // 获取学生统计
        long studentCount = studentRepository.countByClassId(classId);
        
        // 使用mapper查询性别统计
        Long maleCount = studentMapper.countByClassIdAndGender(classId, "男");
        Long femaleCount = studentMapper.countByClassIdAndGender(classId, "女");

        return MyClassOverviewDTO.builder()
            .classId(classId)
            .className(schoolClass.getClassName())
            .studentCount((int) studentCount)
            .maleCount(maleCount != null ? maleCount.intValue() : 0)
            .femaleCount(femaleCount != null ? femaleCount.intValue() : 0)
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

        // 使用V1 StudentMapper获取完整数据（包含宿舍信息）
        List<StudentResponse> students = studentMapper.selectByClassId(classId);

        return students.stream()
            .filter(s -> {
                if (keyword != null && !keyword.isEmpty()) {
                    String name = s.getRealName() != null ? s.getRealName() : "";
                    String studentNo = s.getStudentNo() != null ? s.getStudentNo() : "";
                    return name.contains(keyword) || studentNo.contains(keyword);
                }
                return true;
            })
            .filter(s -> {
                if (status != null && !status.isEmpty()) {
                    Integer studentStatus = s.getStudentStatus();
                    // status: 1在读 2休学 3退学 4毕业 5转学
                    return studentStatus != null && studentStatus.toString().equals(status);
                }
                return true;
            })
            .map(this::toStudentDTOFromResponse)
            .collect(Collectors.toList());
    }

    /**
     * 获取班级宿舍分布
     * 改进: 从班级-宿舍绑定关系查询，显示所有分配给班级的宿舍（包括空宿舍）
     */
    public List<DormitoryDistributionDTO> getDormitoryDistribution(Long classId, Long userId) {
        log.info("获取班级宿舍分布 - classId: {}, userId: {}", classId, userId);
        validateAccess(classId, userId);

        // 1. 查询班级-宿舍绑定关系
        QueryWrapper<ClassDormitory> bindingQuery = new QueryWrapper<>();
        bindingQuery.eq("class_id", classId);
        List<ClassDormitory> bindings = classDormitoryMapper.selectList(bindingQuery);

        log.info("班级 {} 的宿舍绑定记录数: {}", classId, bindings.size());
        if (!bindings.isEmpty()) {
            log.info("绑定的宿舍ID列表: {}", bindings.stream()
                .map(ClassDormitory::getDormitoryId)
                .collect(Collectors.toList()));
        }

        if (bindings.isEmpty()) {
            log.warn("班级 {} 没有绑定任何宿舍", classId);
            return new ArrayList<>();
        }

        // 2. 获取所有宿舍ID
        List<Long> dormitoryIds = bindings.stream()
            .map(ClassDormitory::getDormitoryId)
            .collect(Collectors.toList());

        // 3. 查询宿舍详情
        List<Dormitory> dormitories = dormitoryMapper.selectBatchIds(dormitoryIds);
        log.info("查询到宿舍数量: {}", dormitories.size());
        for (Dormitory d : dormitories) {
            log.info("宿舍详情: id={}, dormitoryNo={}, buildingId={}", d.getId(), d.getDormitoryNo(), d.getBuildingId());
        }
        Map<Long, Dormitory> dormitoryMap = dormitories.stream()
            .collect(Collectors.toMap(Dormitory::getId, d -> d));

        // 4. 查询所有相关楼栋
        Set<Long> buildingIds = dormitories.stream()
            .map(Dormitory::getBuildingId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        Map<Long, Building> buildingMap = new HashMap<>();
        if (!buildingIds.isEmpty()) {
            List<Building> buildings = buildingMapper.selectBatchIds(buildingIds);
            buildingMap = buildings.stream()
                .collect(Collectors.toMap(Building::getId, b -> b));
        }

        // 5. 查询每个宿舍的入住学生
        Map<Long, List<StudentDormitoryResponse>> studentsByDormitory = new HashMap<>();
        for (Long dormitoryId : dormitoryIds) {
            try {
                List<StudentDormitoryResponse> students = studentDormitoryService.getCurrentByDormitoryId(dormitoryId);
                studentsByDormitory.put(dormitoryId, students);
            } catch (Exception e) {
                log.warn("Failed to load students for dormitory {}: {}", dormitoryId, e.getMessage());
                studentsByDormitory.put(dormitoryId, new ArrayList<>());
            }
        }

        // 6. 按楼栋分组组装结果
        Map<Long, List<Dormitory>> dormitoriesByBuilding = dormitories.stream()
            .filter(d -> d.getBuildingId() != null)
            .collect(Collectors.groupingBy(Dormitory::getBuildingId));

        log.info("按楼栋分组后的楼栋数量: {}", dormitoriesByBuilding.size());

        // 处理没有buildingId的宿舍 - 归入"未知楼栋"
        List<Dormitory> dormitoriesWithoutBuilding = dormitories.stream()
            .filter(d -> d.getBuildingId() == null)
            .collect(Collectors.toList());

        if (!dormitoriesWithoutBuilding.isEmpty()) {
            log.warn("有 {} 个宿舍没有buildingId，将归入'未知楼栋'", dormitoriesWithoutBuilding.size());
            dormitoriesByBuilding.put(0L, dormitoriesWithoutBuilding);
        }

        if (dormitoriesByBuilding.isEmpty()) {
            log.warn("没有任何宿舍数据可供显示");
        }

        List<DormitoryDistributionDTO> result = new ArrayList<>();
        Map<Long, Building> finalBuildingMap = buildingMap;

        for (Map.Entry<Long, List<Dormitory>> entry : dormitoriesByBuilding.entrySet()) {
            Long buildingId = entry.getKey();
            List<Dormitory> buildingDormitories = entry.getValue();
            Building building = finalBuildingMap.get(buildingId);

            String buildingName = building != null ? building.getBuildingName() : "未知楼栋";

            // 推断楼栋类型（从宿舍性别类型）
            String buildingType = "MIXED";
            Set<Integer> genderTypes = buildingDormitories.stream()
                .map(Dormitory::getGenderType)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
            if (genderTypes.size() == 1) {
                Integer gt = genderTypes.iterator().next();
                if (gt == 1) buildingType = "MALE";
                else if (gt == 2) buildingType = "FEMALE";
            }

            // 组装房间列表
            List<DormitoryDistributionDTO.DormitoryRoomDTO> rooms = new ArrayList<>();
            int totalStudents = 0;

            for (Dormitory dorm : buildingDormitories) {
                List<StudentDormitoryResponse> dormStudents = studentsByDormitory.getOrDefault(dorm.getId(), new ArrayList<>());
                totalStudents += dormStudents.size();

                List<DormitoryDistributionDTO.StudentBedDTO> studentBeds = dormStudents.stream()
                    .map(s -> DormitoryDistributionDTO.StudentBedDTO.builder()
                        .id(s.getStudentId())
                        .name(s.getStudentName())
                        .bedNo(s.getBedNumber())
                        .build())
                    .collect(Collectors.toList());

                rooms.add(DormitoryDistributionDTO.DormitoryRoomDTO.builder()
                    .dormitoryId(dorm.getId())
                    .roomNo(dorm.getDormitoryNo())
                    .floor(dorm.getFloorNumber())
                    .studentCount(dormStudents.size())
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

    private MyClassStudentDTO toStudentDTOFromResponse(StudentResponse s) {
        // 构建宿舍显示名称
        String dormitoryName = null;
        if (s.getBuildingName() != null && s.getRoomNo() != null) {
            dormitoryName = s.getBuildingName() + " " + s.getRoomNo();
        } else if (s.getRoomNo() != null) {
            dormitoryName = s.getRoomNo();
        }

        // 状态映射: 1在读 2休学 3退学 4毕业 5转学
        String status = "ENROLLED";
        if (s.getStudentStatus() != null) {
            switch (s.getStudentStatus()) {
                case 1: status = "ENROLLED"; break;
                case 2: status = "SUSPENDED"; break;
                case 3: status = "DROPPED_OUT"; break;
                case 4: status = "GRADUATED"; break;
                case 5: status = "TRANSFERRED"; break;
                default: status = "ENROLLED";
            }
        }

        // 性别转换: 1男 2女 -> 男/女
        String gender = null;
        if (s.getGender() != null) {
            gender = s.getGender() == 1 ? "男" : (s.getGender() == 2 ? "女" : null);
        }

        return MyClassStudentDTO.builder()
            .id(s.getId())
            .studentNo(s.getStudentNo())
            .name(s.getRealName())
            .gender(gender)
            .phone(s.getPhone())
            .dormitoryName(dormitoryName)
            .bedNo(s.getBedNumber())
            .status(status)
            .build();
    }

    private void validateAccess(Long classId, Long userId) {
        List<SchoolClass> userClasses = schoolClassRepository.findByTeacherId(userId);
        boolean hasAccess = userClasses.stream()
            .anyMatch(c -> c.getId().equals(classId));
        if (!hasAccess) {
            throw new RuntimeException("无权访问该班级");
        }
    }
}
