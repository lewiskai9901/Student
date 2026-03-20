package com.school.management.infrastructure.persistence.organization;

import com.school.management.domain.organization.model.entity.UserPosition;
import com.school.management.domain.organization.model.valueobject.AppointmentType;
import com.school.management.domain.organization.repository.UserPositionRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class UserPositionRepositoryImpl implements UserPositionRepository {
    private final UserPositionMapper mapper;

    public UserPositionRepositoryImpl(UserPositionMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<UserPosition> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public List<UserPosition> findByUserId(Long userId) {
        return mapper.findByUserId(userId).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<UserPosition> findCurrentByUserId(Long userId) {
        return mapper.findCurrentByUserId(userId).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<UserPosition> findByPositionId(Long positionId) {
        return mapper.findByPositionId(positionId).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<UserPosition> findCurrentByPositionId(Long positionId) {
        return mapper.findCurrentByPositionId(positionId).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public Optional<UserPosition> findPrimaryByUserId(Long userId) {
        return Optional.ofNullable(mapper.findPrimaryByUserId(userId)).map(this::toDomain);
    }

    @Override
    public int countCurrentByPositionId(Long positionId) {
        return mapper.countCurrentByPositionId(positionId);
    }

    @Override
    public List<UserPosition> findCurrentByOrgUnitId(Long orgUnitId) {
        return mapper.findCurrentByOrgUnitId(orgUnitId).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public int endAllByOrgUnitId(Long orgUnitId, String reason) {
        return mapper.endAllByOrgUnitId(orgUnitId, reason);
    }

    @Override
    public UserPosition save(UserPosition up) {
        UserPositionPO po = toPO(up);
        if (up.getId() == null) {
            mapper.insert(po);
            up.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        return up;
    }

    private UserPosition toDomain(UserPositionPO po) {
        AppointmentType type = null;
        try { type = AppointmentType.valueOf(po.getAppointmentType()); } catch (Exception ignored) {}
        return UserPosition.reconstruct(
            po.getId(), po.getUserId(), po.getPositionId(),
            Boolean.TRUE.equals(po.getIsPrimary()),
            type != null ? type : AppointmentType.FORMAL,
            po.getStartDate(), po.getEndDate(),
            po.getAppointmentReason(), po.getDepartureReason(),
            po.getTenantId(), po.getCreatedAt(), po.getCreatedBy()
        );
    }

    private UserPositionPO toPO(UserPosition d) {
        UserPositionPO po = new UserPositionPO();
        po.setId(d.getId());
        po.setUserId(d.getUserId());
        po.setPositionId(d.getPositionId());
        po.setIsPrimary(d.isPrimary());
        po.setAppointmentType(d.getAppointmentType() != null ? d.getAppointmentType().name() : "FORMAL");
        po.setStartDate(d.getStartDate());
        po.setEndDate(d.getEndDate());
        po.setAppointmentReason(d.getAppointmentReason());
        po.setDepartureReason(d.getDepartureReason());
        po.setTenantId(d.getTenantId());
        po.setCreatedAt(d.getCreatedAt());
        po.setCreatedBy(d.getCreatedBy());
        return po;
    }
}
