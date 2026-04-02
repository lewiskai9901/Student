package com.school.management.infrastructure.persistence.place;

import com.school.management.domain.place.model.entity.UniversalPlaceOccupant;
import com.school.management.domain.place.repository.UniversalPlaceOccupantRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 空间占用记录仓储实现
 */
@Repository
public class UniversalPlaceOccupantRepositoryImpl implements UniversalPlaceOccupantRepository {

    private final UniversalPlaceOccupantMapper mapper;

    public UniversalPlaceOccupantRepositoryImpl(UniversalPlaceOccupantMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public UniversalPlaceOccupant save(UniversalPlaceOccupant occupant) {
        UniversalPlaceOccupantPO po = toPO(occupant);
        if (po.getId() == null) {
            mapper.insert(po);
            occupant.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        return toDomain(po);
    }

    @Override
    public Optional<UniversalPlaceOccupant> findById(Long id) {
        UniversalPlaceOccupantPO po = mapper.selectById(id);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public List<UniversalPlaceOccupant> findActiveByPlaceId(Long placeId) {
        return mapper.findActiveByPlaceId(placeId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<UniversalPlaceOccupant> findAllByPlaceId(Long placeId) {
        return mapper.findAllByPlaceId(placeId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<UniversalPlaceOccupant> findActiveByOccupant(String occupantType, Long occupantId) {
        UniversalPlaceOccupantPO po = mapper.findActiveByOccupant(occupantType, occupantId);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public List<UniversalPlaceOccupant> findAllByOccupant(String occupantType, Long occupantId) {
        return mapper.findAllByOccupant(occupantType, occupantId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean isPositionOccupied(Long placeId, String positionNo) {
        return mapper.isPositionOccupied(placeId, positionNo);
    }

    @Override
    public boolean hasActiveOccupancy(String occupantType, Long occupantId) {
        return mapper.hasActiveOccupancy(occupantType, occupantId);
    }

    @Override
    public int countActiveByPlaceId(Long placeId) {
        return mapper.countActiveByPlaceId(placeId);
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    @Override
    public void batchCheckOut(List<Long> ids) {
        if (ids != null && !ids.isEmpty()) {
            mapper.batchCheckOut(ids);
        }
    }

    @Override
    public List<UniversalPlaceOccupant> findActiveByOccupantId(Long occupantId) {
        return mapper.findActiveByOccupantId(occupantId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<UniversalPlaceOccupant> findActiveByPlaceIds(List<Long> placeIds, String occupantType) {
        return mapper.findActiveByPlaceIds(placeIds, occupantType).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    // ==================== 转换方法 ====================

    private UniversalPlaceOccupantPO toPO(UniversalPlaceOccupant entity) {
        UniversalPlaceOccupantPO po = new UniversalPlaceOccupantPO();
        po.setId(entity.getId());
        po.setPlaceId(entity.getPlaceId());
        po.setOccupantType(entity.getOccupantType());
        po.setOccupantId(entity.getOccupantId());
        po.setOccupantName(entity.getOccupantName());
        po.setUsername(entity.getUsername());
        po.setOrgUnitName(entity.getOrgUnitName());
        po.setGender(entity.getGender());
        po.setPositionNo(entity.getPositionNo());
        po.setCheckInTime(entity.getCheckInTime());
        po.setCheckOutTime(entity.getCheckOutTime());
        po.setStatus(entity.getStatus());
        po.setRemark(entity.getRemark());
        return po;
    }

    private UniversalPlaceOccupant toDomain(UniversalPlaceOccupantPO po) {
        return UniversalPlaceOccupant.builder()
                .id(po.getId())
                .placeId(po.getPlaceId())
                .occupantType(po.getOccupantType())
                .occupantId(po.getOccupantId())
                .occupantName(po.getOccupantName())
                .username(po.getUsername())
                .orgUnitName(po.getOrgUnitName())
                .gender(po.getGender())
                .positionNo(po.getPositionNo())
                .checkInTime(po.getCheckInTime())
                .checkOutTime(po.getCheckOutTime())
                .status(po.getStatus())
                .remark(po.getRemark())
                .build();
    }
}
