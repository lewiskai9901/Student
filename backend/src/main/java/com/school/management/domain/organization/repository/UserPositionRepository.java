package com.school.management.domain.organization.repository;

import com.school.management.domain.organization.model.entity.UserPosition;

import java.util.List;
import java.util.Optional;

public interface UserPositionRepository {

    Optional<UserPosition> findById(Long id);

    List<UserPosition> findByUserId(Long userId);

    List<UserPosition> findCurrentByUserId(Long userId);

    List<UserPosition> findByPositionId(Long positionId);

    List<UserPosition> findCurrentByPositionId(Long positionId);

    Optional<UserPosition> findPrimaryByUserId(Long userId);

    int countCurrentByPositionId(Long positionId);

    List<UserPosition> findCurrentByOrgUnitId(Long orgUnitId);

    UserPosition save(UserPosition up);

    int endAllByOrgUnitId(Long orgUnitId, String reason);
}
