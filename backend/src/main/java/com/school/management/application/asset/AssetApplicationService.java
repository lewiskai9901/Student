package com.school.management.application.asset;

import com.school.management.application.asset.command.*;
import com.school.management.application.asset.query.BuildingDTO;
import com.school.management.application.asset.query.DormitoryDTO;
import com.school.management.common.PageResult;
import com.school.management.domain.asset.model.aggregate.Building;
import com.school.management.domain.asset.model.aggregate.Dormitory;
import com.school.management.domain.asset.model.valueobject.*;
import com.school.management.domain.asset.repository.BuildingRepository;
import com.school.management.domain.asset.repository.DormitoryRepository;
import com.school.management.domain.shared.event.DomainEventPublisher;
import com.school.management.entity.ClassDormitoryBinding;
import com.school.management.entity.User;
import com.school.management.exception.BusinessException;
import com.school.management.infrastructure.persistence.organization.OrgUnitMapper;
import com.school.management.infrastructure.persistence.organization.OrgUnitPO;
import com.school.management.mapper.ClassDormitoryBindingMapper;
import com.school.management.mapper.ClassMapper;
import com.school.management.mapper.UserMapper;
import com.school.management.dto.ClassResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 资产应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssetApplicationService {

    private final BuildingRepository buildingRepository;
    private final DormitoryRepository dormitoryRepository;
    private final DomainEventPublisher eventPublisher;
    private final ClassDormitoryBindingMapper classDormitoryBindingMapper;
    private final ClassMapper classMapper;
    private final UserMapper userMapper;
    private final OrgUnitMapper orgUnitMapper;

    // ============ 楼宇管理 ============

    @Transactional
    public Long createBuilding(CreateBuildingCommand command) {
        if (buildingRepository.existsByBuildingNo(command.getBuildingNo())) {
            throw new BusinessException("楼号已存在: " + command.getBuildingNo());
        }

        Building building = Building.create(
                command.getBuildingNo(),
                command.getBuildingName(),
                BuildingType.fromCode(command.getBuildingType()),
                command.getTotalFloors(),
                command.getLocation()
        );

        if (command.getConstructionYear() != null || command.getDescription() != null) {
            building.updateInfo(
                    command.getBuildingName(),
                    BuildingType.fromCode(command.getBuildingType()),
                    command.getTotalFloors(),
                    command.getLocation(),
                    command.getConstructionYear(),
                    command.getDescription()
            );
        }

        Building saved = buildingRepository.save(building);
        eventPublisher.publishAll(building.getDomainEvents());
        building.clearDomainEvents();

        log.info("楼宇创建成功: {}", saved.getId());
        return saved.getId();
    }

    @Transactional
    public void updateBuilding(Long id, CreateBuildingCommand command) {
        Building building = buildingRepository.findById(id)
                .orElseThrow(() -> new BusinessException("楼宇不存在: " + id));

        building.updateInfo(
                command.getBuildingName(),
                BuildingType.fromCode(command.getBuildingType()),
                command.getTotalFloors(),
                command.getLocation(),
                command.getConstructionYear(),
                command.getDescription()
        );

        buildingRepository.save(building);
        eventPublisher.publishAll(building.getDomainEvents());
        building.clearDomainEvents();

        log.info("楼宇更新成功: {}", id);
    }

    @Transactional
    public void deleteBuilding(Long id) {
        Building building = buildingRepository.findById(id)
                .orElseThrow(() -> new BusinessException("楼宇不存在: " + id));

        long dormitoryCount = dormitoryRepository.countByBuildingId(id);
        if (dormitoryCount > 0) {
            throw new BusinessException("楼宇下存在宿舍，无法删除");
        }

        buildingRepository.delete(building);
        log.info("楼宇删除成功: {}", id);
    }

    public BuildingDTO getBuildingById(Long id) {
        return buildingRepository.findById(id)
                .map(this::toBuildingDTO)
                .orElseThrow(() -> new BusinessException("楼宇不存在: " + id));
    }

    public List<BuildingDTO> getDormitoryBuildings() {
        return buildingRepository.findDormitoryBuildings().stream()
                .map(this::toBuildingDTO)
                .collect(Collectors.toList());
    }

    public PageResult<BuildingDTO> findBuildingsByPage(String keyword, Integer buildingType,
                                                        Integer status, int pageNum, int pageSize) {
        BuildingRepository.BuildingQueryCriteria criteria = new BuildingRepository.BuildingQueryCriteria();
        criteria.setKeyword(keyword);
        if (buildingType != null) {
            criteria.setBuildingType(BuildingType.fromCode(buildingType));
        }
        criteria.setStatus(status);

        List<Building> buildings = buildingRepository.findByPage(criteria, pageNum, pageSize);
        long total = buildingRepository.countByCriteria(criteria);

        List<BuildingDTO> dtos = buildings.stream()
                .map(this::toBuildingDTO)
                .collect(Collectors.toList());

        return PageResult.of(dtos, total, pageNum, pageSize);
    }

    // ============ 宿舍管理 ============

    @Transactional
    public Long createDormitory(CreateDormitoryCommand command) {
        if (dormitoryRepository.existsByDormitoryNo(command.getDormitoryNo())) {
            throw new BusinessException("宿舍号已存在: " + command.getDormitoryNo());
        }

        buildingRepository.findById(command.getBuildingId())
                .orElseThrow(() -> new BusinessException("楼宇不存在: " + command.getBuildingId()));

        Dormitory dormitory = Dormitory.create(
                command.getBuildingId(),
                command.getDormitoryNo(),
                command.getFloorNumber(),
                RoomUsageType.fromCode(command.getRoomUsageType()),
                command.getBedCapacity(),
                command.getGenderType() != null ? GenderType.fromCode(command.getGenderType()) : null
        );

        if (command.getOrgUnitId() != null) {
            dormitory.assignToOrgUnit(command.getOrgUnitId());
        }
        if (command.getFacilities() != null || command.getNotes() != null) {
            dormitory.updateInfo(
                    command.getFloorNumber(),
                    RoomUsageType.fromCode(command.getRoomUsageType()),
                    command.getBedCapacity(),
                    command.getGenderType() != null ? GenderType.fromCode(command.getGenderType()) : null,
                    command.getFacilities(),
                    command.getNotes()
            );
        }

        Dormitory saved = dormitoryRepository.save(dormitory);
        eventPublisher.publishAll(dormitory.getDomainEvents());
        dormitory.clearDomainEvents();

        log.info("宿舍创建成功: {}", saved.getId());
        return saved.getId();
    }

    @Transactional
    public void updateDormitory(Long id, CreateDormitoryCommand command) {
        Dormitory dormitory = dormitoryRepository.findById(id)
                .orElseThrow(() -> new BusinessException("宿舍不存在: " + id));

        dormitory.updateInfo(
                command.getFloorNumber(),
                RoomUsageType.fromCode(command.getRoomUsageType()),
                command.getBedCapacity(),
                command.getGenderType() != null ? GenderType.fromCode(command.getGenderType()) : null,
                command.getFacilities(),
                command.getNotes()
        );

        if (command.getOrgUnitId() != null) {
            dormitory.assignToOrgUnit(command.getOrgUnitId());
        }

        dormitoryRepository.save(dormitory);
        log.info("宿舍更新成功: {}", id);
    }

    @Transactional
    public void deleteDormitory(Long id) {
        Dormitory dormitory = dormitoryRepository.findById(id)
                .orElseThrow(() -> new BusinessException("宿舍不存在: " + id));

        if (dormitory.getOccupiedBeds() != null && dormitory.getOccupiedBeds() > 0) {
            throw new BusinessException("宿舍还有学生入住，无法删除");
        }

        dormitoryRepository.delete(dormitory);
        log.info("宿舍删除成功: {}", id);
    }

    public DormitoryDTO getDormitoryById(Long id) {
        return dormitoryRepository.findById(id)
                .map(this::toDormitoryDTO)
                .orElseThrow(() -> new BusinessException("宿舍不存在: " + id));
    }

    public List<DormitoryDTO> getDormitoriesByBuildingId(Long buildingId) {
        return dormitoryRepository.findByBuildingId(buildingId).stream()
                .map(this::toDormitoryDTO)
                .collect(Collectors.toList());
    }

    public PageResult<DormitoryDTO> findDormitoriesByPage(String keyword, Long buildingId,
                                                           Long orgUnitId, Integer genderType,
                                                           Integer status, Boolean hasAvailableBeds,
                                                           int pageNum, int pageSize) {
        DormitoryRepository.DormitoryQueryCriteria criteria = new DormitoryRepository.DormitoryQueryCriteria();
        criteria.setKeyword(keyword);
        criteria.setBuildingId(buildingId);
        criteria.setOrgUnitId(orgUnitId);
        if (genderType != null) {
            criteria.setGenderType(GenderType.fromCode(genderType));
        }
        if (status != null) {
            criteria.setStatus(DormitoryStatus.fromCode(status));
        }
        criteria.setHasAvailableBeds(hasAvailableBeds);

        List<Dormitory> dormitories = dormitoryRepository.findByPage(criteria, pageNum, pageSize);
        long total = dormitoryRepository.countByCriteria(criteria);

        List<DormitoryDTO> dtos = dormitories.stream()
                .map(this::toDormitoryDTO)
                .collect(Collectors.toList());

        return PageResult.of(dtos, total, pageNum, pageSize);
    }

    // ============ 入住/退宿管理 ============

    @Transactional
    public void checkIn(CheckInCommand command) {
        Dormitory dormitory = dormitoryRepository.findById(command.getDormitoryId())
                .orElseThrow(() -> new BusinessException("宿舍不存在: " + command.getDormitoryId()));

        dormitory.checkIn(command.getStudentId(), command.getBedNumber(), command.getStudentName());
        dormitoryRepository.save(dormitory);

        eventPublisher.publishAll(dormitory.getDomainEvents());
        dormitory.clearDomainEvents();

        log.info("学生入住成功: {} -> {} 床位{}",
                command.getStudentId(), command.getDormitoryId(), command.getBedNumber());
    }

    @Transactional
    public void checkOut(CheckOutCommand command) {
        Dormitory dormitory = dormitoryRepository.findById(command.getDormitoryId())
                .orElseThrow(() -> new BusinessException("宿舍不存在: " + command.getDormitoryId()));

        dormitory.checkOut(command.getStudentId(), command.getStudentName(), command.getBedNumber());
        dormitoryRepository.save(dormitory);

        eventPublisher.publishAll(dormitory.getDomainEvents());
        dormitory.clearDomainEvents();

        log.info("学生退宿成功: {} <- {}", command.getStudentId(), command.getDormitoryId());
    }

    // ============ 批量操作 ============

    @Transactional
    public int batchUpdateDepartment(List<Long> dormitoryIds, Long orgUnitId) {
        if (dormitoryIds == null || dormitoryIds.isEmpty()) {
            return 0;
        }

        int count = 0;
        for (Long dormitoryId : dormitoryIds) {
            try {
                Dormitory dormitory = dormitoryRepository.findById(dormitoryId).orElse(null);
                if (dormitory != null) {
                    if (orgUnitId != null) {
                        dormitory.assignToOrgUnit(orgUnitId);
                    } else {
                        dormitory.removeFromOrgUnit();
                    }
                    dormitoryRepository.save(dormitory);
                    count++;
                }
            } catch (Exception e) {
                log.error("更新宿舍部门失败: dormitoryId={}, orgUnitId={}", dormitoryId, orgUnitId, e);
            }
        }

        log.info("批量更新宿舍部门完成: {} 间", count);
        return count;
    }

    // ============ 转换方法 ============

    private BuildingDTO toBuildingDTO(Building building) {
        return BuildingDTO.builder()
                .id(building.getId())
                .buildingNo(building.getBuildingNo())
                .buildingName(building.getBuildingName())
                .buildingType(building.getBuildingType() != null ? building.getBuildingType().getCode() : null)
                .buildingTypeName(building.getBuildingType() != null ? building.getBuildingType().getName() : null)
                .totalFloors(building.getTotalFloors())
                .location(building.getLocation())
                .constructionYear(building.getConstructionYear())
                .description(building.getDescription())
                .status(building.getStatus())
                .statusName(building.isActive() ? "启用" : "停用")
                .createdAt(building.getCreatedAt())
                .updatedAt(building.getUpdatedAt())
                .build();
    }

    private DormitoryDTO toDormitoryDTO(Dormitory dormitory) {
        // 获取楼栋信息
        String buildingNo = null;
        String buildingName = null;
        if (dormitory.getBuildingId() != null) {
            Building building = buildingRepository.findById(dormitory.getBuildingId()).orElse(null);
            if (building != null) {
                buildingNo = building.getBuildingNo();
                buildingName = building.getBuildingName();
            }
        }

        // 获取组织单元名称
        String orgUnitName = null;
        if (dormitory.getOrgUnitId() != null) {
            OrgUnitPO orgUnit = orgUnitMapper.selectById(dormitory.getOrgUnitId());
            if (orgUnit != null) {
                orgUnitName = orgUnit.getDeptName();
            }
        }

        // 获取班级绑定信息及班主任信息
        String assignedClassIds = null;
        String assignedClassNames = null;
        String classTeacherNames = null;
        String classTeacherPhones = null;

        if (dormitory.getId() != null) {
            List<ClassDormitoryBinding> bindings = classDormitoryBindingMapper.selectByDormitoryId(dormitory.getId());
            if (bindings != null && !bindings.isEmpty()) {
                List<Long> classIds = bindings.stream()
                        .map(ClassDormitoryBinding::getClassId)
                        .distinct()
                        .collect(Collectors.toList());

                // 获取班级详情（包含班主任信息）
                List<ClassResponse> classes = classMapper.selectClassResponseByIds(classIds);
                if (classes != null && !classes.isEmpty()) {
                    assignedClassIds = classes.stream()
                            .map(c -> String.valueOf(c.getId()))
                            .collect(Collectors.joining(","));
                    assignedClassNames = classes.stream()
                            .map(ClassResponse::getClassName)
                            .collect(Collectors.joining(", "));

                    // 获取班主任信息
                    Set<Long> teacherIds = classes.stream()
                            .map(ClassResponse::getTeacherId)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toSet());

                    if (!teacherIds.isEmpty()) {
                        List<User> teachers = new ArrayList<>();
                        for (Long teacherId : teacherIds) {
                            User teacher = userMapper.selectById(teacherId);
                            if (teacher != null) {
                                teachers.add(teacher);
                            }
                        }

                        if (!teachers.isEmpty()) {
                            classTeacherNames = teachers.stream()
                                    .map(User::getRealName)
                                    .filter(Objects::nonNull)
                                    .distinct()
                                    .collect(Collectors.joining(", "));
                            classTeacherPhones = teachers.stream()
                                    .map(User::getPhone)
                                    .filter(Objects::nonNull)
                                    .distinct()
                                    .collect(Collectors.joining(", "));
                        }
                    }
                }
            }
        }

        return DormitoryDTO.builder()
                .id(dormitory.getId())
                .buildingId(dormitory.getBuildingId())
                .buildingNo(buildingNo)
                .buildingName(buildingName)
                .orgUnitId(dormitory.getOrgUnitId())
                .orgUnitName(orgUnitName)
                .dormitoryNo(dormitory.getDormitoryNo())
                .assignedClassIds(assignedClassIds)
                .assignedClassNames(assignedClassNames)
                .classTeacherNames(classTeacherNames)
                .classTeacherPhones(classTeacherPhones)
                .floorNumber(dormitory.getFloorNumber())
                .roomUsageType(dormitory.getRoomUsageType() != null ? dormitory.getRoomUsageType().getCode() : null)
                .roomUsageTypeName(dormitory.getRoomUsageType() != null ? dormitory.getRoomUsageType().getName() : null)
                .bedCapacity(dormitory.getBedCapacity())
                .bedCount(dormitory.getBedCount())
                .occupiedBeds(dormitory.getOccupiedBeds())
                .availableBeds(dormitory.getAvailableBeds())
                .genderType(dormitory.getGenderType() != null ? dormitory.getGenderType().getCode() : null)
                .genderTypeName(dormitory.getGenderType() != null ? dormitory.getGenderType().getName() : null)
                .facilities(dormitory.getFacilities())
                .notes(dormitory.getNotes())
                .status(dormitory.getStatus() != null ? dormitory.getStatus().getCode() : null)
                .statusName(dormitory.getStatus() != null ? dormitory.getStatus().getName() : null)
                .createdAt(dormitory.getCreatedAt())
                .updatedAt(dormitory.getUpdatedAt())
                .build();
    }
}
