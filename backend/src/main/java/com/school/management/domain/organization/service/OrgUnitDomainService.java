package com.school.management.domain.organization.service;

import com.school.management.domain.organization.model.OrgUnit;
import com.school.management.domain.organization.model.OrgUnitType;
import com.school.management.domain.organization.repository.OrgUnitRepository;

import java.util.Optional;

/**
 * Domain service for organization unit operations.
 * Contains domain logic that spans multiple aggregates or doesn't naturally fit in an entity.
 */
public class OrgUnitDomainService {

    private final OrgUnitRepository orgUnitRepository;

    public OrgUnitDomainService(OrgUnitRepository orgUnitRepository) {
        this.orgUnitRepository = orgUnitRepository;
    }

    /**
     * Creates a new organization unit with proper tree path calculation.
     *
     * @param unitCode  the unique code
     * @param unitName  the display name
     * @param unitType  the organization type
     * @param parentId  the parent ID (can be null for root)
     * @param createdBy the user creating this unit
     * @return the created organization unit
     */
    public OrgUnit createOrgUnit(String unitCode, String unitName, OrgUnitType unitType,
                                  Long parentId, Long createdBy) {
        // Validate unique code
        if (orgUnitRepository.existsByUnitCode(unitCode)) {
            throw new IllegalArgumentException("Unit code already exists: " + unitCode);
        }

        // Validate parent relationship
        OrgUnit parent = null;
        if (parentId != null) {
            parent = orgUnitRepository.findById(parentId)
                .orElseThrow(() -> new IllegalArgumentException("Parent not found: " + parentId));

            // Validate type hierarchy
            if (!unitType.canBeChildOf(parent.getUnitType())) {
                throw new IllegalArgumentException(
                    String.format("Unit type %s cannot be a child of %s",
                        unitType, parent.getUnitType()));
            }
        } else if (unitType != OrgUnitType.SCHOOL) {
            throw new IllegalArgumentException("Only SCHOOL type can be a root organization unit");
        }

        // Create the org unit
        OrgUnit orgUnit = OrgUnit.create(unitCode, unitName, unitType, parentId, createdBy);

        // Set tree position
        if (parent != null) {
            orgUnit.setTreePosition(parent.getTreePath(), parent.getTreeLevel());
        } else {
            orgUnit.setTreePosition(null, 0);
        }

        return orgUnit;
    }

    /**
     * Moves an organization unit to a new parent.
     *
     * @param orgUnit     the organization unit to move
     * @param newParentId the new parent ID
     * @param updatedBy   the user performing the move
     */
    public void moveOrgUnit(OrgUnit orgUnit, Long newParentId, Long updatedBy) {
        if (orgUnit.getId().equals(newParentId)) {
            throw new IllegalArgumentException("Cannot move an org unit under itself");
        }

        OrgUnit newParent = null;
        if (newParentId != null) {
            newParent = orgUnitRepository.findById(newParentId)
                .orElseThrow(() -> new IllegalArgumentException("New parent not found: " + newParentId));

            // Check for circular reference
            if (newParent.getTreePath().contains("/" + orgUnit.getId() + "/")) {
                throw new IllegalArgumentException("Cannot create circular reference in organization tree");
            }

            // Validate type hierarchy
            if (!orgUnit.getUnitType().canBeChildOf(newParent.getUnitType())) {
                throw new IllegalArgumentException(
                    String.format("Unit type %s cannot be a child of %s",
                        orgUnit.getUnitType(), newParent.getUnitType()));
            }
        }

        // Update tree position for this unit and all descendants
        String oldTreePath = orgUnit.getTreePath();
        if (newParent != null) {
            orgUnit.setTreePosition(newParent.getTreePath(), newParent.getTreeLevel());
        } else {
            orgUnit.setTreePosition(null, 0);
        }

        // Note: Descendants will need to be updated by the application service
        // using the old and new tree paths
    }

    /**
     * Checks if an organization unit can be safely deleted.
     *
     * @param orgUnit the organization unit to check
     * @return true if can be deleted, false otherwise
     */
    public boolean canDelete(OrgUnit orgUnit) {
        // Check for children
        long childCount = orgUnitRepository.countByParentId(orgUnit.getId());
        return childCount == 0;
    }
}
