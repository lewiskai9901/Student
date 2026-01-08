package com.school.management.infrastructure.persistence.asset;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.management.domain.asset.model.aggregate.Building;
import com.school.management.domain.asset.model.valueobject.BuildingType;
import com.school.management.domain.asset.repository.BuildingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 楼宇仓储实现
 */
@Repository
@RequiredArgsConstructor
public class BuildingRepositoryImpl implements BuildingRepository {

    private final AssetBuildingMapper buildingMapper;

    @Override
    public Building save(Building building) {
        BuildingPO po = toPO(building);
        if (po.getId() == null) {
            buildingMapper.insert(po);
        } else {
            buildingMapper.updateById(po);
        }
        building.setId(po.getId());
        return building;
    }

    @Override
    public Optional<Building> findById(Long id) {
        BuildingPO po = buildingMapper.selectById(id);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public void delete(Building building) {
        buildingMapper.deleteById(building.getId());
    }

    @Override
    public Optional<Building> findByBuildingNo(String buildingNo) {
        BuildingPO po = buildingMapper.selectByBuildingNo(buildingNo);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public List<Building> findByType(BuildingType type) {
        return buildingMapper.selectByType(type.getCode()).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Building> findAllActive() {
        return buildingMapper.selectAllActive().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Building> findDormitoryBuildings() {
        return buildingMapper.selectDormitoryBuildings().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByBuildingNo(String buildingNo) {
        return buildingMapper.countByBuildingNo(buildingNo) > 0;
    }

    @Override
    public List<Building> findByPage(BuildingQueryCriteria criteria, int pageNum, int pageSize) {
        Page<BuildingPO> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<BuildingPO> wrapper = buildQueryWrapper(criteria);
        wrapper.orderByAsc(BuildingPO::getBuildingNo);

        Page<BuildingPO> result = buildingMapper.selectPage(page, wrapper);
        return result.getRecords().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public long countByCriteria(BuildingQueryCriteria criteria) {
        LambdaQueryWrapper<BuildingPO> wrapper = buildQueryWrapper(criteria);
        return buildingMapper.selectCount(wrapper);
    }

    private LambdaQueryWrapper<BuildingPO> buildQueryWrapper(BuildingQueryCriteria criteria) {
        LambdaQueryWrapper<BuildingPO> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(criteria.getKeyword())) {
            wrapper.and(w -> w
                    .like(BuildingPO::getBuildingNo, criteria.getKeyword())
                    .or()
                    .like(BuildingPO::getBuildingName, criteria.getKeyword())
            );
        }

        if (criteria.getBuildingType() != null) {
            wrapper.eq(BuildingPO::getBuildingType, criteria.getBuildingType().getCode());
        }

        if (criteria.getStatus() != null) {
            wrapper.eq(BuildingPO::getStatus, criteria.getStatus());
        }

        return wrapper;
    }

    private BuildingPO toPO(Building building) {
        BuildingPO po = new BuildingPO();
        po.setId(building.getId());
        po.setBuildingNo(building.getBuildingNo());
        po.setBuildingName(building.getBuildingName());
        po.setBuildingType(building.getBuildingType() != null ? building.getBuildingType().getCode() : null);
        po.setTotalFloors(building.getTotalFloors());
        po.setLocation(building.getLocation());
        po.setConstructionYear(building.getConstructionYear());
        po.setDescription(building.getDescription());
        po.setStatus(building.getStatus());
        po.setCreatedAt(building.getCreatedAt());
        po.setUpdatedAt(building.getUpdatedAt());
        return po;
    }

    private Building toDomain(BuildingPO po) {
        return Building.reconstruct(
                po.getId(),
                po.getBuildingNo(),
                po.getBuildingName(),
                po.getBuildingType() != null ? BuildingType.fromCode(po.getBuildingType()) : null,
                po.getTotalFloors(),
                po.getLocation(),
                po.getConstructionYear(),
                po.getDescription(),
                po.getStatus(),
                po.getCreatedAt(),
                po.getUpdatedAt()
        );
    }
}
