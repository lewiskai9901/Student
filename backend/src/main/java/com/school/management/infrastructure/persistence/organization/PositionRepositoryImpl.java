package com.school.management.infrastructure.persistence.organization;

import com.school.management.domain.organization.model.Position;
import com.school.management.domain.organization.repository.PositionRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class PositionRepositoryImpl implements PositionRepository {
    private final PositionMapper mapper;

    public PositionRepositoryImpl(PositionMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<Position> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public Optional<Position> findByPositionCode(String code) {
        return Optional.ofNullable(mapper.findByPositionCode(code)).map(this::toDomain);
    }

    @Override
    public List<Position> findByOrgUnitId(Long orgUnitId) {
        return mapper.findByOrgUnitId(orgUnitId).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<Position> findByReportsToId(Long reportsToId) {
        return mapper.findByReportsToId(reportsToId).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public boolean existsByPositionCode(String code) {
        return mapper.findByPositionCode(code) != null;
    }

    @Override
    public Position save(Position position) {
        PositionPO po = toPO(position);
        if (position.getId() == null) {
            mapper.insert(po);
            position.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        return position;
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    @Override
    public int softDeleteByOrgUnitId(Long orgUnitId) {
        return mapper.softDeleteByOrgUnitId(orgUnitId);
    }

    private Position toDomain(PositionPO po) {
        return Position.reconstruct(
            po.getId(), po.getPositionCode(), po.getPositionName(),
            po.getOrgUnitId(), po.getJobLevel(),
            po.getHeadcount() != null ? po.getHeadcount() : 1,
            po.getReportsToId(), po.getResponsibilities(), po.getRequirements(),
            po.getSortOrder() != null ? po.getSortOrder() : 0,
            Boolean.TRUE.equals(po.getKeyPosition()),
            po.getEnabled() == null || po.getEnabled(),
            po.getVersion() != null ? po.getVersion().longValue() : 0L,
            po.getTenantId(), po.getCreatedAt(), po.getCreatedBy(),
            po.getUpdatedAt(), po.getUpdatedBy()
        );
    }

    private PositionPO toPO(Position d) {
        PositionPO po = new PositionPO();
        po.setId(d.getId());
        po.setPositionCode(d.getPositionCode());
        po.setPositionName(d.getPositionName());
        po.setOrgUnitId(d.getOrgUnitId());
        po.setJobLevel(d.getJobLevel());
        po.setHeadcount(d.getHeadcount());
        po.setReportsToId(d.getReportsToId());
        po.setResponsibilities(d.getResponsibilities());
        po.setRequirements(d.getRequirements());
        po.setSortOrder(d.getSortOrder());
        po.setKeyPosition(d.isKeyPosition());
        po.setEnabled(d.isEnabled());
        po.setTenantId(d.getTenantId());
        po.setCreatedAt(d.getCreatedAt());
        po.setCreatedBy(d.getCreatedBy());
        po.setUpdatedAt(d.getUpdatedAt());
        po.setUpdatedBy(d.getUpdatedBy());
        po.setVersion(d.getVersion() != null ? d.getVersion().intValue() : 0);
        return po;
    }
}
