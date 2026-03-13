package com.school.management.infrastructure.persistence.space;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.management.domain.space.model.entity.SpaceOccupant;
import com.school.management.domain.space.model.valueobject.OccupantType;
import com.school.management.domain.space.repository.SpaceOccupantRepository;
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
public class SpaceOccupantRepositoryImpl implements SpaceOccupantRepository {

    private final SpaceOccupantMapper occupantMapper;

    @Override
    public SpaceOccupant save(SpaceOccupant occupant) {
        SpaceOccupantPO po = toPO(occupant);
        if (occupant.getId() == null) {
            occupantMapper.insert(po);
            occupant.setId(po.getId());
        } else {
            occupantMapper.updateById(po);
        }
        return occupant;
    }

    @Override
    public Optional<SpaceOccupant> findById(Long id) {
        SpaceOccupantPO po = occupantMapper.selectById(id);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public List<SpaceOccupant> findActiveBySpaceId(Long spaceId) {
        return occupantMapper.selectActiveBySpaceId(spaceId).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<SpaceOccupant> findAllBySpaceId(Long spaceId) {
        return occupantMapper.selectAllBySpaceId(spaceId).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public Optional<SpaceOccupant> findActiveByOccupant(OccupantType occupantType, Long occupantId) {
        SpaceOccupantPO po = occupantMapper.selectActiveByOccupant(occupantType.name(), occupantId);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public Optional<SpaceOccupant> findActiveByPosition(Long spaceId, Integer positionNo) {
        SpaceOccupantPO po = occupantMapper.selectActiveByPosition(spaceId, positionNo);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public boolean isPositionOccupied(Long spaceId, Integer positionNo) {
        return occupantMapper.selectActiveByPosition(spaceId, positionNo) != null;
    }

    @Override
    public boolean hasActiveOccupancy(OccupantType occupantType, Long occupantId) {
        return occupantMapper.selectActiveByOccupant(occupantType.name(), occupantId) != null;
    }

    @Override
    public int countActiveBySpaceId(Long spaceId) {
        return occupantMapper.countActiveBySpaceId(spaceId);
    }

    @Override
    public List<Integer> findOccupiedPositions(Long spaceId) {
        return occupantMapper.selectOccupiedPositions(spaceId);
    }

    @Override
    public void batchCheckOutBySpaceId(Long spaceId) {
        occupantMapper.batchCheckOut(spaceId);
    }

    @Override
    public void delete(Long id) {
        occupantMapper.deleteById(id);
    }

    // ========== 转换方法 ==========

    private SpaceOccupantPO toPO(SpaceOccupant occupant) {
        SpaceOccupantPO po = new SpaceOccupantPO();
        po.setId(occupant.getId());
        po.setSpaceId(occupant.getSpaceId());
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

    private SpaceOccupant toDomain(SpaceOccupantPO po) {
        SpaceOccupant occupant = SpaceOccupant.reconstitute(
            po.getId(),
            po.getSpaceId(),
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
