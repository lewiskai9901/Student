package com.school.management.infrastructure.persistence.asset;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.management.domain.asset.model.aggregate.Dormitory;
import com.school.management.domain.asset.model.valueobject.DormitoryStatus;
import com.school.management.domain.asset.model.valueobject.GenderType;
import com.school.management.domain.asset.model.valueobject.RoomUsageType;
import com.school.management.domain.asset.repository.DormitoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 宿舍仓储实现
 */
@Repository
@RequiredArgsConstructor
public class DormitoryRepositoryImpl implements DormitoryRepository {

    private final AssetDormitoryMapper dormitoryMapper;

    @Override
    public Dormitory save(Dormitory dormitory) {
        DormitoryPO po = toPO(dormitory);
        if (po.getId() == null) {
            dormitoryMapper.insert(po);
        } else {
            dormitoryMapper.updateById(po);
        }
        dormitory.setId(po.getId());
        return dormitory;
    }

    @Override
    public Optional<Dormitory> findById(Long id) {
        DormitoryPO po = dormitoryMapper.selectById(id);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public void delete(Dormitory dormitory) {
        dormitoryMapper.deleteById(dormitory.getId());
    }

    @Override
    public Optional<Dormitory> findByDormitoryNo(String dormitoryNo) {
        DormitoryPO po = dormitoryMapper.selectByDormitoryNo(dormitoryNo);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public List<Dormitory> findByBuildingId(Long buildingId) {
        return dormitoryMapper.selectByBuildingId(buildingId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Dormitory> findByBuildingIdAndFloor(Long buildingId, Integer floorNumber) {
        return dormitoryMapper.selectByBuildingIdAndFloor(buildingId, floorNumber).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Dormitory> findByOrgUnitId(Long orgUnitId) {
        return dormitoryMapper.selectByOrgUnitId(orgUnitId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Dormitory> findAvailable(Long buildingId, GenderType genderType) {
        Integer genderCode = genderType != null ? genderType.getCode() : null;
        return dormitoryMapper.selectAvailable(buildingId, genderCode).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByDormitoryNo(String dormitoryNo) {
        return dormitoryMapper.countByDormitoryNo(dormitoryNo) > 0;
    }

    @Override
    public long countByBuildingId(Long buildingId) {
        return dormitoryMapper.countByBuildingId(buildingId);
    }

    @Override
    public long countByOrgUnitId(Long orgUnitId) {
        return dormitoryMapper.countByOrgUnitId(orgUnitId);
    }

    @Override
    public List<Dormitory> findByPage(DormitoryQueryCriteria criteria, int pageNum, int pageSize) {
        Page<DormitoryPO> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<DormitoryPO> wrapper = buildQueryWrapper(criteria);
        wrapper.orderByAsc(DormitoryPO::getDormitoryNo);

        Page<DormitoryPO> result = dormitoryMapper.selectPage(page, wrapper);
        return result.getRecords().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public long countByCriteria(DormitoryQueryCriteria criteria) {
        LambdaQueryWrapper<DormitoryPO> wrapper = buildQueryWrapper(criteria);
        return dormitoryMapper.selectCount(wrapper);
    }

    private LambdaQueryWrapper<DormitoryPO> buildQueryWrapper(DormitoryQueryCriteria criteria) {
        LambdaQueryWrapper<DormitoryPO> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(criteria.getKeyword())) {
            wrapper.like(DormitoryPO::getDormitoryNo, criteria.getKeyword());
        }

        if (criteria.getBuildingId() != null) {
            wrapper.eq(DormitoryPO::getBuildingId, criteria.getBuildingId());
        }

        if (criteria.getOrgUnitId() != null) {
            wrapper.eq(DormitoryPO::getOrgUnitId, criteria.getOrgUnitId());
        }

        if (criteria.getFloorNumber() != null) {
            wrapper.eq(DormitoryPO::getFloorNumber, criteria.getFloorNumber());
        }

        if (criteria.getGenderType() != null) {
            wrapper.eq(DormitoryPO::getGenderType, criteria.getGenderType().getCode());
        }

        if (criteria.getStatus() != null) {
            wrapper.eq(DormitoryPO::getStatus, criteria.getStatus().getCode());
        }

        if (Boolean.TRUE.equals(criteria.getHasAvailableBeds())) {
            wrapper.apply("bed_count > occupied_beds");
        }

        return wrapper;
    }

    private DormitoryPO toPO(Dormitory dormitory) {
        DormitoryPO po = new DormitoryPO();
        po.setId(dormitory.getId());
        po.setBuildingId(dormitory.getBuildingId());
        po.setOrgUnitId(dormitory.getOrgUnitId());
        po.setDormitoryNo(dormitory.getDormitoryNo());
        po.setFloorNumber(dormitory.getFloorNumber());
        po.setRoomUsageType(dormitory.getRoomUsageType() != null ? dormitory.getRoomUsageType().getCode() : null);
        po.setBedCapacity(dormitory.getBedCapacity());
        po.setBedCount(dormitory.getBedCount());
        po.setOccupiedBeds(dormitory.getOccupiedBeds());
        po.setGenderType(dormitory.getGenderType() != null ? dormitory.getGenderType().getCode() : null);
        po.setFacilities(dormitory.getFacilities());
        po.setNotes(dormitory.getNotes());
        po.setStatus(dormitory.getStatus() != null ? dormitory.getStatus().getCode() : null);
        po.setCreatedAt(dormitory.getCreatedAt());
        po.setUpdatedAt(dormitory.getUpdatedAt());
        return po;
    }

    private Dormitory toDomain(DormitoryPO po) {
        return Dormitory.reconstruct(
                po.getId(),
                po.getBuildingId(),
                po.getOrgUnitId(),
                po.getDormitoryNo(),
                po.getFloorNumber(),
                po.getRoomUsageType() != null ? RoomUsageType.fromCode(po.getRoomUsageType()) : null,
                po.getBedCapacity(),
                po.getBedCount(),
                po.getOccupiedBeds(),
                po.getGenderType() != null ? GenderType.fromCode(po.getGenderType()) : null,
                po.getFacilities(),
                po.getNotes(),
                po.getStatus() != null ? DormitoryStatus.fromCode(po.getStatus()) : null,
                po.getCreatedAt(),
                po.getUpdatedAt()
        );
    }
}
