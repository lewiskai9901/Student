package com.school.management.domain.organization.service;

import com.school.management.domain.organization.model.OrgUnit;
import com.school.management.domain.organization.model.entity.OrgType;
import com.school.management.domain.organization.repository.OrgUnitRepository;
import com.school.management.domain.organization.repository.OrgUnitTypeRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Domain service for organization unit operations.
 * Uses OrgUnitTypeRepository for database-driven type validation.
 */
@RequiredArgsConstructor
@Service
public class OrgUnitDomainService {

    private final OrgUnitRepository orgUnitRepository;
    private final OrgUnitTypeRepository orgUnitTypeRepository;

    /**
     * Creates a new organization unit with proper tree path calculation.
     * Validates type against org_unit_types database configuration.
     */
    public OrgUnit createOrgUnit(String unitCode, String unitName, String unitType,
                                  Long parentId, Long createdBy) {
        // Validate unique code
        if (orgUnitRepository.existsByUnitCode(unitCode)) {
            throw new IllegalArgumentException("Unit code already exists: " + unitCode);
        }

        // Validate unitType exists and is enabled (reads from entity_type_configs)
        OrgType typeEntity = orgUnitTypeRepository.findByTypeCode(unitType)
                .orElseThrow(() -> new IllegalArgumentException("Invalid unit type: " + unitType));
        if (!typeEntity.isEnabled()) {
            throw new IllegalArgumentException("Unit type is disabled: " + unitType);
        }

        // Validate parent relationship
        OrgUnit parent = null;
        if (parentId != null) {
            parent = orgUnitRepository.findById(parentId)
                .orElseThrow(() -> new IllegalArgumentException("Parent not found: " + parentId));

            // Validate type hierarchy using database configuration
            validateParentChildType(parent.getUnitType(), unitType);
        } else {
            // Root units must be top-level types
            if (!typeEntity.isTopLevel()) {
                throw new IllegalArgumentException("Only top-level types can be root organization units");
            }
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
     * Validates that the child type can be placed under the parent type,
     * using the database-driven allowed_child_type_codes configuration.
     */
    private void validateParentChildType(String parentTypeCode, String childTypeCode) {
        OrgType parentType = orgUnitTypeRepository.findByTypeCode(parentTypeCode)
            .orElse(null);

        if (parentType == null) {
            return;
        }

        // If parent has allowedChildTypeCodes, validate against it
        if (parentType.getAllowedChildTypeCodes() != null && !parentType.getAllowedChildTypeCodes().isEmpty()) {
            if (!parentType.allowsChildType(childTypeCode)) {
                throw new IllegalArgumentException(
                    String.format("Type %s does not allow child type %s", parentTypeCode, childTypeCode));
            }
        }
    }

    /**
     * Gets the allowed child type codes for a given parent type.
     */
    public List<String> getAllowedChildTypes(String parentTypeCode) {
        OrgType parentType = orgUnitTypeRepository.findByTypeCode(parentTypeCode)
            .orElse(null);

        if (parentType == null) {
            return List.of();
        }

        // If parent has explicit allowed child type codes, return those
        if (parentType.getAllowedChildTypeCodes() != null && !parentType.getAllowedChildTypeCodes().isEmpty()) {
            return parentType.getAllowedChildTypeCodes();
        }

        // Otherwise return all enabled types
        return orgUnitTypeRepository.findAllEnabled().stream()
            .map(OrgType::getTypeCode)
            .collect(Collectors.toList());
    }

    /**
     * Moves an organization unit to a new parent.
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
            validateParentChildType(newParent.getUnitType(), orgUnit.getUnitType());
        }

        // Update tree position
        if (newParent != null) {
            orgUnit.setTreePosition(newParent.getTreePath(), newParent.getTreeLevel());
        } else {
            orgUnit.setTreePosition(null, 0);
        }
    }

    /**
     * Checks if an organization unit can be safely deleted.
     */
    public boolean canDelete(OrgUnit orgUnit) {
        long childCount = orgUnitRepository.countByParentId(orgUnit.getId());
        return childCount == 0;
    }

    /**
     * Merges source org unit into target.
     * - Sets source status to DISSOLVED, marks mergedIntoId
     * - Moves source's children under target
     * Returns list of moved children for change logging.
     */
    public List<OrgUnit> mergeOrgUnits(Long sourceId, Long targetId, String reason, Long updatedBy) {
        if (sourceId.equals(targetId)) {
            throw new IllegalArgumentException("Cannot merge an org unit into itself");
        }

        OrgUnit source = orgUnitRepository.findById(sourceId)
            .orElseThrow(() -> new IllegalArgumentException("Source org unit not found: " + sourceId));
        OrgUnit target = orgUnitRepository.findById(targetId)
            .orElseThrow(() -> new IllegalArgumentException("Target org unit not found: " + targetId));

        if (source.isDissolved()) {
            throw new IllegalStateException("Cannot merge an already dissolved org unit");
        }
        if (target.isDissolved()) {
            throw new IllegalStateException("Cannot merge into a dissolved org unit");
        }

        // Validate no parent-child relationship (circular reference prevention)
        if (source.isAncestorOf(target)) {
            throw new IllegalArgumentException("Cannot merge: source is an ancestor of target");
        }
        if (source.isDescendantOf(target)) {
            throw new IllegalArgumentException("Cannot merge: source is a descendant of target");
        }

        // Move children from source to target
        List<OrgUnit> children = orgUnitRepository.findByParentId(sourceId);
        List<OrgUnit> movedChildren = new ArrayList<>();
        for (OrgUnit child : children) {
            child.moveToParent(targetId, updatedBy);
            child.setTreePosition(target.getTreePath(), target.getTreeLevel());
            orgUnitRepository.save(child);
            movedChildren.add(child);
        }

        // Mark source as merged
        source.markMergedInto(targetId, reason, updatedBy);
        orgUnitRepository.save(source);

        return movedChildren;
    }

    /**
     * Splits an org unit by creating new org units and moving specified children.
     * Each split spec has: newUnitCode, newUnitName, childIds (children to move).
     * All new units inherit the same unitType and parent as the source.
     * Returns the list of newly created org units.
     */
    public List<OrgUnit> splitOrgUnit(Long sourceId, List<SplitSpec> splits, String reason, Long createdBy) {
        OrgUnit source = orgUnitRepository.findById(sourceId)
            .orElseThrow(() -> new IllegalArgumentException("Source org unit not found: " + sourceId));

        if (source.isDissolved()) {
            throw new IllegalStateException("Cannot split a dissolved org unit");
        }

        List<OrgUnit> newUnits = new ArrayList<>();

        for (SplitSpec spec : splits) {
            if (orgUnitRepository.existsByUnitCode(spec.newUnitCode)) {
                throw new IllegalArgumentException("Unit code already exists: " + spec.newUnitCode);
            }

            OrgUnit newUnit = OrgUnit.create(
                spec.newUnitCode, spec.newUnitName,
                source.getUnitType(), source.getParentId(), createdBy
            );
            newUnit.markSplitFrom(sourceId);

            // Calculate tree position based on source's parent
            if (source.getParentId() != null) {
                OrgUnit parent = orgUnitRepository.findById(source.getParentId()).orElse(null);
                if (parent != null) {
                    newUnit = orgUnitRepository.save(newUnit);
                    newUnit.setTreePosition(parent.getTreePath(), parent.getTreeLevel());
                    orgUnitRepository.save(newUnit);
                } else {
                    newUnit = orgUnitRepository.save(newUnit);
                    newUnit.setTreePosition(null, 0);
                    orgUnitRepository.save(newUnit);
                }
            } else {
                newUnit = orgUnitRepository.save(newUnit);
                newUnit.setTreePosition(null, 0);
                orgUnitRepository.save(newUnit);
            }

            // Move specified children
            final OrgUnit savedUnit = newUnit;
            if (spec.childIds != null) {
                for (Long childId : spec.childIds) {
                    orgUnitRepository.findById(childId).ifPresent(child -> {
                        child.moveToParent(savedUnit.getId(), createdBy);
                        child.setTreePosition(savedUnit.getTreePath(), savedUnit.getTreeLevel());
                        orgUnitRepository.save(child);
                    });
                }
            }

            newUnits.add(savedUnit);
        }

        return newUnits;
    }

    /**
     * Specification for a single split target.
     */
    public static class SplitSpec {
        public final String newUnitCode;
        public final String newUnitName;
        public final List<Long> childIds;

        public SplitSpec(String newUnitCode, String newUnitName, List<Long> childIds) {
            this.newUnitCode = newUnitCode;
            this.newUnitName = newUnitName;
            this.childIds = childIds;
        }
    }

    // ==================== Tree Path Repair ====================

    /**
     * Repair all tree_path and tree_level values by recursing from roots.
     */
    public int repairAllTreePaths() {
        List<OrgUnit> roots = orgUnitRepository.findRoots();
        int count = 0;
        for (OrgUnit root : roots) {
            root.setTreePosition(null, 0); // sets path="/<id>/", level=1
            orgUnitRepository.save(root);
            count++;
            count += repairChildren(root);
        }
        return count;
    }

    private int repairChildren(OrgUnit parent) {
        List<OrgUnit> children = orgUnitRepository.findByParentId(parent.getId());
        int count = 0;
        for (OrgUnit child : children) {
            child.setTreePosition(parent.getTreePath(), parent.getTreeLevel());
            orgUnitRepository.save(child);
            count++;
            count += repairChildren(child);
        }
        return count;
    }
}
