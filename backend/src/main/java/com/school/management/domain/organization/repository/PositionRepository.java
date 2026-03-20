package com.school.management.domain.organization.repository;

import com.school.management.domain.organization.model.Position;

import java.util.List;
import java.util.Optional;

public interface PositionRepository {

    Optional<Position> findById(Long id);

    Optional<Position> findByPositionCode(String code);

    List<Position> findByOrgUnitId(Long orgUnitId);

    List<Position> findByReportsToId(Long reportsToId);

    boolean existsByPositionCode(String code);

    Position save(Position position);

    void deleteById(Long id);

    int softDeleteByOrgUnitId(Long orgUnitId);
}
