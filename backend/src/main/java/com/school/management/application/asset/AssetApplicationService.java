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
import com.school.management.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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

        if (command.getDepartmentId() != null) {
            dormitory.assignToDepartment(command.getDepartmentId());
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

        if (command.getDepartmentId() != null) {
            dormitory.assignToDepartment(command.getDepartmentId());
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
                                                           Long departmentId, Integer genderType,
                                                           Integer status, Boolean hasAvailableBeds,
                                                           int pageNum, int pageSize) {
        DormitoryRepository.DormitoryQueryCriteria criteria = new DormitoryRepository.DormitoryQueryCriteria();
        criteria.setKeyword(keyword);
        criteria.setBuildingId(buildingId);
        criteria.setDepartmentId(departmentId);
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
        return DormitoryDTO.builder()
                .id(dormitory.getId())
                .buildingId(dormitory.getBuildingId())
                .departmentId(dormitory.getDepartmentId())
                .dormitoryNo(dormitory.getDormitoryNo())
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
