package com.school.management.domain.organization.repository;

import com.school.management.domain.organization.model.OrgUnit;
import com.school.management.domain.shared.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Repository interface for OrgUnit aggregate.
 * Defines the contract for persisting and querying organization units.
 */
public interface OrgUnitRepository extends Repository<OrgUnit, Long> {

    /**
     * Finds an organization unit by its code.
     *
     * @param unitCode the unit code
     * @return an Optional containing the org unit if found
     */
    Optional<OrgUnit> findByUnitCode(String unitCode);

    /**
     * Finds all organization units by type.
     *
     * @param unitType the type to filter by
     * @return list of matching org units
     */
    List<OrgUnit> findByUnitType(String unitType);

    /**
     * Finds all children of a parent organization unit.
     *
     * @param parentId the parent ID
     * @return list of child org units
     */
    List<OrgUnit> findByParentId(Long parentId);

    /**
     * Finds all root organization units (no parent).
     *
     * @return list of root org units
     */
    List<OrgUnit> findRoots();

    /**
     * Finds all descendants of an organization unit.
     *
     * @param treePath the tree path to search under
     * @return list of descendant org units
     */
    List<OrgUnit> findDescendants(String treePath);

    /**
     * Finds all ancestors of an organization unit.
     *
     * @param orgUnitId the org unit ID
     * @return list of ancestor org units (from root to parent)
     */
    List<OrgUnit> findAncestors(Long orgUnitId);

    /**
     * Checks if a unit code already exists.
     *
     * @param unitCode the unit code to check
     * @return true if exists, false otherwise
     */
    boolean existsByUnitCode(String unitCode);

    /**
     * Counts children of a parent organization unit.
     *
     * @param parentId the parent ID
     * @return count of children
     */
    long countByParentId(Long parentId);

    /**
     * Finds all organization units.
     *
     * @return list of all org units
     */
    List<OrgUnit> findAll();

    /**
     * Batch finds all children of multiple parent organization units.
     *
     * @param parentIds the parent IDs
     * @return list of child org units for all specified parents
     */
    List<OrgUnit> findByParentIds(Collection<Long> parentIds);

    /**
     * Batch checks which parent IDs actually have children.
     * Returns the subset of parentIds that have at least one child.
     *
     * @param parentIds the parent IDs to check
     * @return set of parent IDs that have children
     */
    Set<Long> findParentIdsWithChildren(Collection<Long> parentIds);
}
