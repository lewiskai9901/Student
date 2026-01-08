package com.school.management.application.organization;

import com.school.management.application.organization.command.CreateOrgUnitCommand;
import com.school.management.application.organization.command.UpdateOrgUnitCommand;
import com.school.management.application.organization.query.OrgUnitDTO;
import com.school.management.application.organization.query.OrgUnitTreeDTO;
import com.school.management.domain.organization.model.OrgUnit;
import com.school.management.domain.organization.model.OrgUnitType;
import com.school.management.domain.organization.repository.OrgUnitRepository;
import com.school.management.domain.organization.service.OrgUnitDomainService;
import com.school.management.domain.shared.event.DomainEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Application service for organization unit operations.
 * Orchestrates domain services, repositories, and event publishing.
 */
@Service
public class OrgUnitApplicationService {

    private final OrgUnitRepository orgUnitRepository;
    private final OrgUnitDomainService orgUnitDomainService;
    private final DomainEventPublisher eventPublisher;

    public OrgUnitApplicationService(OrgUnitRepository orgUnitRepository,
                                     DomainEventPublisher eventPublisher) {
        this.orgUnitRepository = orgUnitRepository;
        this.orgUnitDomainService = new OrgUnitDomainService(orgUnitRepository);
        this.eventPublisher = eventPublisher;
    }

    /**
     * Creates a new organization unit.
     */
    @Transactional
    public OrgUnitDTO createOrgUnit(CreateOrgUnitCommand command) {
        OrgUnit orgUnit = orgUnitDomainService.createOrgUnit(
            command.getUnitCode(),
            command.getUnitName(),
            command.getUnitType(),
            command.getParentId(),
            command.getCreatedBy()
        );

        orgUnit = orgUnitRepository.save(orgUnit);

        // Publish domain events
        orgUnit.getDomainEvents().forEach(eventPublisher::publish);
        orgUnit.clearDomainEvents();

        return toDTO(orgUnit);
    }

    /**
     * Updates an existing organization unit.
     */
    @Transactional
    public OrgUnitDTO updateOrgUnit(Long id, UpdateOrgUnitCommand command) {
        OrgUnit orgUnit = orgUnitRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("OrgUnit not found: " + id));

        orgUnit.update(
            command.getUnitName(),
            command.getLeaderId(),
            command.getDeputyLeaderIds(),
            command.getSortOrder(),
            command.getUpdatedBy()
        );

        orgUnit = orgUnitRepository.save(orgUnit);

        // Publish domain events
        orgUnit.getDomainEvents().forEach(eventPublisher::publish);
        orgUnit.clearDomainEvents();

        return toDTO(orgUnit);
    }

    /**
     * Gets an organization unit by ID.
     */
    @Transactional(readOnly = true)
    public OrgUnitDTO getOrgUnit(Long id) {
        return orgUnitRepository.findById(id)
            .map(this::toDTO)
            .orElseThrow(() -> new IllegalArgumentException("OrgUnit not found: " + id));
    }

    /**
     * Gets organization units by type.
     */
    @Transactional(readOnly = true)
    public List<OrgUnitDTO> getOrgUnitsByType(OrgUnitType unitType) {
        return orgUnitRepository.findByUnitType(unitType).stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    /**
     * Gets children of an organization unit.
     */
    @Transactional(readOnly = true)
    public List<OrgUnitDTO> getChildren(Long parentId) {
        return orgUnitRepository.findByParentId(parentId).stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    /**
     * Gets the complete organization tree.
     */
    @Transactional(readOnly = true)
    public List<OrgUnitTreeDTO> getOrgUnitTree() {
        List<OrgUnit> roots = orgUnitRepository.findRoots();
        return roots.stream()
            .map(this::buildTree)
            .collect(Collectors.toList());
    }

    /**
     * Deletes an organization unit.
     */
    @Transactional
    public void deleteOrgUnit(Long id) {
        OrgUnit orgUnit = orgUnitRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("OrgUnit not found: " + id));

        if (!orgUnitDomainService.canDelete(orgUnit)) {
            throw new IllegalStateException("Cannot delete OrgUnit with children");
        }

        orgUnitRepository.deleteById(id);
    }

    /**
     * Enables an organization unit.
     */
    @Transactional
    public void enableOrgUnit(Long id) {
        OrgUnit orgUnit = orgUnitRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("OrgUnit not found: " + id));
        orgUnit.enable();
        orgUnitRepository.save(orgUnit);
    }

    /**
     * Disables an organization unit.
     */
    @Transactional
    public void disableOrgUnit(Long id) {
        OrgUnit orgUnit = orgUnitRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("OrgUnit not found: " + id));
        orgUnit.disable();
        orgUnitRepository.save(orgUnit);
    }

    // Helper methods
    private OrgUnitTreeDTO buildTree(OrgUnit orgUnit) {
        OrgUnitTreeDTO dto = toTreeDTO(orgUnit);
        List<OrgUnit> children = orgUnitRepository.findByParentId(orgUnit.getId());
        dto.setChildren(children.stream()
            .map(this::buildTree)
            .collect(Collectors.toList()));
        return dto;
    }

    private OrgUnitDTO toDTO(OrgUnit orgUnit) {
        OrgUnitDTO dto = new OrgUnitDTO();
        dto.setId(orgUnit.getId());
        dto.setUnitCode(orgUnit.getUnitCode());
        dto.setUnitName(orgUnit.getUnitName());
        dto.setUnitType(orgUnit.getUnitType());
        dto.setParentId(orgUnit.getParentId());
        dto.setTreePath(orgUnit.getTreePath());
        dto.setTreeLevel(orgUnit.getTreeLevel());
        dto.setLeaderId(orgUnit.getLeaderId());
        dto.setDeputyLeaderIds(orgUnit.getDeputyLeaderIds());
        dto.setSortOrder(orgUnit.getSortOrder());
        dto.setEnabled(orgUnit.isEnabled());
        return dto;
    }

    private OrgUnitTreeDTO toTreeDTO(OrgUnit orgUnit) {
        OrgUnitTreeDTO dto = new OrgUnitTreeDTO();
        dto.setId(orgUnit.getId());
        dto.setUnitCode(orgUnit.getUnitCode());
        dto.setUnitName(orgUnit.getUnitName());
        dto.setUnitType(orgUnit.getUnitType());
        dto.setLeaderId(orgUnit.getLeaderId());
        dto.setEnabled(orgUnit.isEnabled());
        dto.setChildren(new ArrayList<>());
        return dto;
    }
}
