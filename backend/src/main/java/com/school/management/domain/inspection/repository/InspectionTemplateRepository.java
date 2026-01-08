package com.school.management.domain.inspection.repository;

import com.school.management.domain.inspection.model.InspectionTemplate;
import com.school.management.domain.inspection.model.TemplateScope;
import com.school.management.domain.inspection.model.TemplateStatus;
import com.school.management.domain.shared.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for InspectionTemplate aggregate.
 */
public interface InspectionTemplateRepository extends Repository<InspectionTemplate, Long> {

    /**
     * Finds a template by its code.
     */
    Optional<InspectionTemplate> findByTemplateCode(String templateCode);

    /**
     * Finds all templates by scope.
     */
    List<InspectionTemplate> findByScope(TemplateScope scope);

    /**
     * Finds all templates by status.
     */
    List<InspectionTemplate> findByStatus(TemplateStatus status);

    /**
     * Finds the default template for a scope.
     */
    Optional<InspectionTemplate> findDefaultByScope(TemplateScope scope);

    /**
     * Finds templates applicable to an organization unit.
     */
    List<InspectionTemplate> findByApplicableOrgUnitId(Long orgUnitId);

    /**
     * Checks if a template code already exists.
     */
    boolean existsByTemplateCode(String templateCode);

    /**
     * Finds all published templates.
     */
    List<InspectionTemplate> findAllPublished();
}
