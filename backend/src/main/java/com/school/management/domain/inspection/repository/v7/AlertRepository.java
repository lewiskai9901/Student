package com.school.management.domain.inspection.repository.v7;

import com.school.management.domain.inspection.model.v7.analytics.Alert;

import java.util.List;
import java.util.Optional;

public interface AlertRepository {

    Alert save(Alert alert);

    Optional<Alert> findById(Long id);

    List<Alert> findByStatus(String status);

    List<Alert> findRecent(int limit);

    long countByStatus(String status);

    void deleteById(Long id);
}
