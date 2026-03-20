package com.school.management.infrastructure.persistence.place;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.management.domain.place.model.entity.PlaceOccupant;
import com.school.management.domain.place.model.valueobject.OccupantType;
import com.school.management.domain.place.repository.PlaceOccupantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 场所占用仓储实现
 */
@Repository
@RequiredArgsConstructor
public class PlaceOccupantRepositoryImpl implements PlaceOccupantRepository {

    private final PlaceOccupantMapper occupantMapper;

    @Override
    public PlaceOccupant save(PlaceOccupant occupant) {
        PlaceOccupantPO po = toPO(occupant);
        if (occupant.getId() == null) {
            occupantMapper.insert(po);
            occupant.setId(po.getId());
        } else {
            occupantMapper.updateById(po);
        }
        return occupant;
    }

    @Override
    public Optional<PlaceOccupant> findById(Long id) {
        PlaceOccupantPO po = occupantMapper.selectById(id);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public List<PlaceOccupant> findActiveByPlaceId(Long placeId) {
        return occupantMapper.selectActiveByPlaceId(placeId).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<PlaceOccupant> findAllByPlaceId(Long placeId) {
        return occupantMapper.selectAllByPlaceId(placeId).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public Optional<PlaceOccupant> findActiveByOccupant(OccupantType occupantType, Long occupantId) {
        PlaceOccupantPO po = occupantMapper.selectActiveByOccupant(occupantType.name(), occupantId);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public Optional<PlaceOccupant> findActiveByPosition(Long placeId, Integer positionNo) {
        PlaceOccupantPO po = occupantMapper.selectActiveByPosition(placeId, positionNo);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public boolean isPositionOccupied(Long placeId, Integer positionNo) {
        return occupantMapper.selectActiveByPosition(placeId, positionNo) != null;
    }

    @Override
    public boolean hasActiveOccupancy(OccupantType occupantType, Long occupantId) {
        return occupantMapper.selectActiveByOccupant(occupantType.name(), occupantId) != null;
    }

    @Override
    public int countActiveByPlaceId(Long placeId) {
        return occupantMapper.countActiveByPlaceId(placeId);
    }

    @Override
    public List<Integer> findOccupiedPositions(Long placeId) {
        return occupantMapper.selectOccupiedPositions(placeId);
    }

    @Override
    public void batchCheckOutByPlaceId(Long placeId) {
        occupantMapper.batchCheckOut(placeId);
    }

    @Override
    public void delete(Long id) {
        occupantMapper.deleteById(id);
    }

    // ========== 转换方法 ==========

    private PlaceOccupantPO toPO(PlaceOccupant occupant) {
        PlaceOccupantPO po = new PlaceOccupantPO();
        po.setId(occupant.getId());
        po.setPlaceId(occupant.getPlaceId());
        po.setOccupantType(occupant.getOccupantType().name());
        po.setOccupantId(occupant.getOccupantId());
        po.setPositionNo(occupant.getPositionNo());
        po.setCheckInDate(occupant.getCheckInDate());
        po.setCheckOutDate(occupant.getCheckOutDate());
        po.setStatus(occupant.getStatus());
        po.setRemark(occupant.getRemark());
        po.setCreatedAt(occupant.getCreatedAt());
        po.setUpdatedAt(occupant.getUpdatedAt());
        return po;
    }

    private PlaceOccupant toDomain(PlaceOccupantPO po) {
        PlaceOccupant occupant = PlaceOccupant.reconstitute(
            po.getId(),
            po.getPlaceId(),
            OccupantType.valueOf(po.getOccupantType()),
            po.getOccupantId(),
            po.getPositionNo(),
            po.getCheckInDate(),
            po.getCheckOutDate(),
            po.getStatus(),
            po.getRemark(),
            po.getCreatedAt(),
            po.getUpdatedAt()
        );
        // 设置关联信息
        occupant.setOccupantInfo(po.getOccupantName(), po.getOccupantNo());
        return occupant;
    }
}
