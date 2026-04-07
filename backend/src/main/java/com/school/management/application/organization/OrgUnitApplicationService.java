package com.school.management.application.organization;

import com.school.management.application.organization.command.CreateOrgUnitCommand;
import com.school.management.application.organization.command.UpdateOrgUnitCommand;
import com.school.management.application.organization.query.OrgUnitDTO;
import com.school.management.application.organization.query.OrgUnitTreeDTO;
import com.school.management.domain.organization.model.OrgUnit;
import com.school.management.domain.organization.model.Position;
import com.school.management.domain.organization.model.entity.OrgType;
import com.school.management.domain.organization.model.entity.OrgCategory;
import com.school.management.domain.shared.model.valueobject.FieldChange;
import com.school.management.domain.organization.repository.OrgUnitRepository;
import com.school.management.infrastructure.activity.ActivityEventPublisher;
import com.school.management.domain.organization.repository.OrgUnitTypeRepository;
import com.school.management.domain.organization.repository.PositionRepository;
import com.school.management.domain.student.repository.SchoolClassRepository;
import com.school.management.domain.organization.repository.UserPositionRepository;
import com.school.management.domain.organization.service.OrgUnitDomainService;
import com.school.management.domain.access.repository.AccessRelationRepository;
import com.school.management.domain.shared.event.DomainEventPublisher;
import com.school.management.infrastructure.extension.ExtensionContext;
import com.school.management.infrastructure.extension.ExtensionDispatcher;
import com.school.management.infrastructure.persistence.student.SchoolClassMapper;
import com.school.management.infrastructure.persistence.student.SchoolClassPO;
import com.school.management.infrastructure.persistence.place.UniversalPlaceOccupantMapper;
import com.school.management.infrastructure.persistence.user.UserDomainMapper;
import com.school.management.infrastructure.persistence.user.UserPO;
import com.school.management.application.event.TriggerService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Application service for organization unit operations.
 * Orchestrates domain services, repositories, and event publishing.
 */
@RequiredArgsConstructor
@Service
public class OrgUnitApplicationService {

    private final OrgUnitRepository orgUnitRepository;
    private final OrgUnitTypeRepository orgUnitTypeRepository;
    private final OrgUnitDomainService orgUnitDomainService;
    private final DomainEventPublisher eventPublisher;
    private final ActivityEventPublisher activityEventPublisher;
    private final AccessRelationRepository accessRelationRepository;
    private final PositionRepository positionRepository;
    private final UserPositionRepository userPositionRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final SchoolClassMapper schoolClassMapper;
    private final UserDomainMapper userDomainMapper;
    private final UniversalPlaceOccupantMapper placeOccupantMapper;

    @Autowired(required = false)
    private TriggerService triggerService;

    @Autowired(required = false)
    private ExtensionDispatcher extensionDispatcher;

    @Transactional
    public OrgUnitDTO createOrgUnit(CreateOrgUnitCommand command) {
        OrgUnit orgUnit = orgUnitDomainService.createOrgUnit(
            command.getUnitCode(),
            command.getUnitName(),
            command.getUnitType(),
            command.getParentId(),
            command.getCreatedBy()
        );

        // 设置扩展属性（来自 DynamicForm）
        if (command.getAttributes() != null && !command.getAttributes().isEmpty()) {
            orgUnit.setAttributes(command.getAttributes());
        }

        orgUnit = orgUnitRepository.save(orgUnit);

        orgUnit.getDomainEvents().forEach(eventPublisher::publish);
        orgUnit.clearDomainEvents();

        // Create user-selected positions (or none if not provided)
        createSelectedPositions(orgUnit, command.getSelectedPositions());

        // SPI: 触发插件生命周期钩子
        if (extensionDispatcher != null && orgUnit.getUnitType() != null) {
            try {
                ExtensionContext ctx = ExtensionContext.builder()
                    .entityType("ORG_UNIT")
                    .typeCode(orgUnit.getUnitType())
                    .entityId(orgUnit.getId())
                    .entityName(orgUnit.getUnitName())
                    .parentId(command.getParentId())
                    .attributes(orgUnit.getAttributes())
                    .operatorId(command.getCreatedBy())
                    .build();
                extensionDispatcher.fireAfterCreate("ORG_UNIT", orgUnit.getUnitType(), ctx);
            } catch (Exception e) {
                log.warn("SPI afterCreate 执行异常: {}", e.getMessage());
            }
        }

        // 触发事件: 组织单元创建
        if (triggerService != null) {
            try {
                triggerService.fire("ORG_UNIT_CREATED", Map.of(
                    "orgUnitId", orgUnit.getId(),
                    "orgUnitName", orgUnit.getUnitName() != null ? orgUnit.getUnitName() : "",
                    "orgUnitType", orgUnit.getUnitType() != null ? orgUnit.getUnitType() : "",
                    "parentId", command.getParentId() != null ? command.getParentId() : 0L
                ));
            } catch (Exception ignored) {}
        }

        return toDTO(orgUnit);
    }

    @Transactional
    public OrgUnitDTO updateOrgUnit(Long id, UpdateOrgUnitCommand command) {
        OrgUnit orgUnit = orgUnitRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("OrgUnit not found: " + id));

        List<FieldChange> changes = orgUnit.update(
            command.getUnitName(),
            command.getSortOrder(),
            command.getHeadcount(),
            command.getAttributes(),
            command.getUpdatedBy()
        );

        orgUnit = orgUnitRepository.save(orgUnit);

        // Publish domain events
        orgUnit.getDomainEvents().forEach(eventPublisher::publish);
        orgUnit.clearDomainEvents();

        // Publish unified activity event if there were actual changes
        if (!changes.isEmpty()) {
            activityEventPublisher.newEvent("organization", "ORG_UNIT", "UPDATE", "更新组织单元")
                .resourceId(id)
                .resourceName(orgUnit.getUnitName())
                .changedFields(new ArrayList<>(changes))
                .reason(command.getReason())
                .publish();
        }

        return toDTO(orgUnit);
    }

    @Transactional(readOnly = true)
    public OrgUnitDTO getOrgUnit(Long id) {
        return orgUnitRepository.findById(id)
            .map(this::toDTO)
            .orElseThrow(() -> new IllegalArgumentException("OrgUnit not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<OrgUnitDTO> getOrgUnitsByType(String unitType) {
        return orgUnitRepository.findByUnitType(unitType).stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OrgUnitDTO> getChildren(Long parentId) {
        return orgUnitRepository.findByParentId(parentId).stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OrgUnitTreeDTO> getOrgUnitTree() {
        List<OrgUnit> allUnits = orgUnitRepository.findAll();

        Map<String, OrgType> typeMap = loadTypeMap();

        Map<Long, List<OrgUnit>> childrenMap = allUnits.stream()
            .filter(u -> u.getParentId() != null)
            .collect(Collectors.groupingBy(OrgUnit::getParentId));

        List<OrgUnit> roots = allUnits.stream()
            .filter(u -> u.getParentId() == null)
            .collect(Collectors.toList());

        List<OrgUnitTreeDTO> tree = roots.stream()
            .map(root -> buildTreeFromMap(root, childrenMap, typeMap))
            .collect(Collectors.toList());

        // Batch-fill class extension data for all CLASS type nodes
        enrichClassNodes(tree, allUnits);

        return tree;
    }

    @Transactional(readOnly = true)
    public List<OrgType> getAllowedChildTypes(String parentTypeCode) {
        List<String> allowedCodes = orgUnitDomainService.getAllowedChildTypes(parentTypeCode);
        return orgUnitTypeRepository.findAllEnabled().stream()
            .filter(t -> allowedCodes.contains(t.getTypeCode()))
            .collect(Collectors.toList());
    }

    @Transactional
    public void deleteOrgUnit(Long id) {
        OrgUnit orgUnit = orgUnitRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("OrgUnit not found: " + id));

        // Collect all descendants using parent_id recursion (tree_path may be corrupted)
        List<OrgUnit> allDescendants = new ArrayList<>();
        collectDescendantsByParentId(id, allDescendants);

        // Delete deepest first (reverse order)
        for (int i = allDescendants.size() - 1; i >= 0; i--) {
            OrgUnit desc = allDescendants.get(i);
            cleanupOrgUnitData(desc.getId(), desc.getUnitName(), "组织删除");
            if ("CLASS".equals(desc.getUnitType())) {
                schoolClassRepository.deleteById(desc.getId());
            } else {
                orgUnitRepository.deleteById(desc.getId());
            }
        }

        // Clean up and delete the target unit itself
        cleanupOrgUnitData(id, orgUnit.getUnitName(), "组织删除");
        if ("CLASS".equals(orgUnit.getUnitType())) {
            schoolClassRepository.deleteById(id);
        } else {
            orgUnitRepository.deleteById(id);
        }
    }

    /**
     * 递归收集所有后代节点（基于 parent_id，不依赖 tree_path）
     */
    private void collectDescendantsByParentId(Long parentId, List<OrgUnit> result) {
        List<OrgUnit> children = orgUnitRepository.findByParentId(parentId);
        for (OrgUnit child : children) {
            result.add(child);
            collectDescendantsByParentId(child.getId(), result);
        }
    }

    /**
     * 清除组织关联的所有数据：成员归属、岗位任命、岗位定义、访问关系、场所入住快照
     */
    private void cleanupOrgUnitData(Long orgUnitId, String orgUnitName, String reason) {
        // 1. 结束该组织下所有在任岗位任命
        userPositionRepository.endAllByOrgUnitId(orgUnitId, reason);
        // 2. 清除归属到该组织的用户（primary_org_unit_id → null）
        userDomainMapper.clearPrimaryOrgUnitId(orgUnitId);
        // 3. 逻辑删除该组织下所有岗位定义
        positionRepository.softDeleteByOrgUnitId(orgUnitId);
        // 4. 删除访问关系
        accessRelationRepository.deleteByResource("org_unit", orgUnitId);
        // 5. 清空场所入住记录中的组织名称快照
        if (orgUnitName != null) {
            placeOccupantMapper.clearOrgUnitName(orgUnitName);
        }
    }

    @Transactional
    public OrgUnitDTO freezeOrgUnit(Long id, String reason, Long updatedBy) {
        OrgUnit orgUnit = orgUnitRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("OrgUnit not found: " + id));

        List<FieldChange> changes = orgUnit.freeze(reason, updatedBy);
        orgUnit = orgUnitRepository.save(orgUnit);

        if (!changes.isEmpty()) {
            activityEventPublisher.newEvent("organization", "ORG_UNIT", "FREEZE", "冻结组织单元")
                .resourceId(id)
                .resourceName(orgUnit.getUnitName())
                .changedFields(new ArrayList<>(changes))
                .reason(reason)
                .publish();
        }
        return toDTO(orgUnit);
    }

    @Transactional
    public OrgUnitDTO unfreezeOrgUnit(Long id, Long updatedBy) {
        OrgUnit orgUnit = orgUnitRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("OrgUnit not found: " + id));

        List<FieldChange> changes = orgUnit.unfreeze(updatedBy);
        orgUnit = orgUnitRepository.save(orgUnit);

        if (!changes.isEmpty()) {
            activityEventPublisher.newEvent("organization", "ORG_UNIT", "UNFREEZE", "解冻组织单元")
                .resourceId(id)
                .resourceName(orgUnit.getUnitName())
                .changedFields(new ArrayList<>(changes))
                .publish();
        }
        return toDTO(orgUnit);
    }

    @Transactional
    public OrgUnitDTO dissolveOrgUnit(Long id, String reason, Long updatedBy) {
        OrgUnit orgUnit = orgUnitRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("OrgUnit not found: " + id));

        List<FieldChange> changes = orgUnit.dissolve(reason, updatedBy);
        orgUnit = orgUnitRepository.save(orgUnit);

        // 解散时清除成员归属、岗位任命、岗位定义、访问关系、场所入住快照
        userPositionRepository.endAllByOrgUnitId(id, "组织解散: " + (reason != null ? reason : ""));
        userDomainMapper.clearPrimaryOrgUnitId(id);
        positionRepository.softDeleteByOrgUnitId(id);
        accessRelationRepository.deleteByResource("org_unit", id);
        if (orgUnit.getUnitName() != null) {
            placeOccupantMapper.clearOrgUnitName(orgUnit.getUnitName());
        }

        if (!changes.isEmpty()) {
            activityEventPublisher.newEvent("organization", "ORG_UNIT", "DISSOLVE", "解散组织单元")
                .resourceId(id)
                .resourceName(orgUnit.getUnitName())
                .changedFields(new ArrayList<>(changes))
                .reason(reason)
                .publish();
        }
        return toDTO(orgUnit);
    }

    @Transactional
    public OrgUnitDTO mergeOrgUnit(Long sourceId, Long targetId, String reason, Long updatedBy) {
        List<OrgUnit> movedChildren = orgUnitDomainService.mergeOrgUnits(sourceId, targetId, reason, updatedBy);

        activityEventPublisher.newEvent("organization", "ORG_UNIT", "MERGE", "合并组织单元")
            .resourceId(sourceId)
            .changedFields(List.of(new FieldChange("mergedIntoId", null, targetId.toString())))
            .reason(reason)
            .publish();

        OrgUnit target = orgUnitRepository.findById(targetId)
            .orElseThrow(() -> new IllegalArgumentException("Target not found: " + targetId));
        return toDTO(target);
    }

    @Transactional
    public List<OrgUnitDTO> splitOrgUnit(Long sourceId, List<SplitRequest> splits, String reason, Long createdBy) {
        List<OrgUnitDomainService.SplitSpec> specs = splits.stream()
            .map(s -> new OrgUnitDomainService.SplitSpec(s.getUnitCode(), s.getUnitName(), s.getChildIds()))
            .collect(Collectors.toList());

        List<OrgUnit> newUnits = orgUnitDomainService.splitOrgUnit(sourceId, specs, reason, createdBy);

        activityEventPublisher.newEvent("organization", "ORG_UNIT", "SPLIT", "拆分组织单元")
            .resourceId(sourceId)
            .changedFields(List.of(new FieldChange("splitInto", null,
                newUnits.stream().map(u -> u.getId().toString()).collect(Collectors.joining(",")))))
            .reason(reason)
            .publish();

        return newUnits.stream().map(this::toDTO).collect(Collectors.toList());
    }

    @lombok.Data
    public static class SplitRequest {
        private String unitCode;
        private String unitName;
        private List<Long> childIds;
    }

    // ==================== Position creation ====================

    /**
     * 根据用户选择的岗位列表创建岗位实例
     */
    private void createSelectedPositions(OrgUnit orgUnit, List<CreateOrgUnitCommand.SelectedPosition> selectedPositions) {
        if (selectedPositions == null || selectedPositions.isEmpty()) return;

        int sortOrder = 0;
        for (CreateOrgUnitCommand.SelectedPosition sp : selectedPositions) {
            if (sp.getPositionName() == null || sp.getPositionName().trim().isEmpty()) continue;

            String positionCode = orgUnit.getId() + "_POS_" + System.currentTimeMillis() + "_" + sortOrder;

            Position position = Position.create(
                positionCode,
                sp.getPositionName().trim(),
                orgUnit.getId(),
                sp.getHeadcount() > 0 ? sp.getHeadcount() : 1,
                orgUnit.getCreatedBy()
            );
            position.update(
                sp.getPositionName().trim(), null, sp.getHeadcount() > 0 ? sp.getHeadcount() : 1,
                null, null, null, false, sortOrder,
                orgUnit.getCreatedBy()
            );
            positionRepository.save(position);
            sortOrder++;
        }
    }

    // ==================== Helper methods ====================

    /**
     * Batch-enrich CLASS type nodes with class-specific data.
     */
    private void enrichClassNodes(List<OrgUnitTreeDTO> tree, List<OrgUnit> allUnits) {
        List<Long> classIds = allUnits.stream()
            .filter(u -> "CLASS".equals(u.getUnitType()))
            .map(OrgUnit::getId)
            .collect(Collectors.toList());

        if (classIds.isEmpty()) return;

        List<SchoolClassPO> classPOs = schoolClassMapper.selectBatchIds(classIds);
        Map<Long, SchoolClassPO> classMap = classPOs.stream()
            .collect(Collectors.toMap(SchoolClassPO::getId, Function.identity(), (a, b) -> a));

        Set<Long> teacherIds = classPOs.stream()
            .filter(c -> c.getTeacherId() != null)
            .map(SchoolClassPO::getTeacherId)
            .collect(Collectors.toSet());

        Map<Long, String> teacherNameMap = Map.of();
        if (!teacherIds.isEmpty()) {
            List<UserPO> teachers = userDomainMapper.selectBatchIds(new ArrayList<>(teacherIds));
            teacherNameMap = teachers.stream()
                .collect(Collectors.toMap(UserPO::getId, UserPO::getRealName, (a, b) -> a));
        }

        fillClassData(tree, classMap, teacherNameMap);
    }

    private void fillClassData(List<OrgUnitTreeDTO> nodes, Map<Long, SchoolClassPO> classMap,
                               Map<Long, String> teacherNameMap) {
        for (OrgUnitTreeDTO node : nodes) {
            if ("CLASS".equals(node.getUnitType())) {
                SchoolClassPO classPO = classMap.get(node.getId());
                if (classPO != null) {
                    node.setStudentCount(classPO.getStudentCount());
                    node.setStandardSize(classPO.getStandardSize() != null ? classPO.getStandardSize() : 50);
                    node.setEnrollmentYear(classPO.getEnrollmentYear());
                    node.setClassStatus(classPO.getStatus() != null && classPO.getStatus() == 1
                        ? "ACTIVE" : "PREPARING");
                    if (classPO.getTeacherId() != null) {
                        node.setHeadTeacherName(teacherNameMap.get(classPO.getTeacherId()));
                    }
                }
            }
            if (node.getChildren() != null && !node.getChildren().isEmpty()) {
                fillClassData(node.getChildren(), classMap, teacherNameMap);
            }
        }
    }

    private Map<String, OrgType> loadTypeMap() {
        return orgUnitTypeRepository.findAll().stream()
            .collect(Collectors.toMap(
                OrgType::getTypeCode,
                t -> t,
                (a, b) -> a
            ));
    }

    private OrgUnitTreeDTO buildTreeFromMap(OrgUnit orgUnit, Map<Long, List<OrgUnit>> childrenMap,
                                             Map<String, OrgType> typeMap) {
        OrgUnitTreeDTO dto = toTreeDTO(orgUnit, typeMap);
        List<OrgUnit> children = childrenMap.getOrDefault(orgUnit.getId(), List.of());
        dto.setChildren(children.stream()
            .map(child -> buildTreeFromMap(child, childrenMap, typeMap))
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
        dto.setSortOrder(orgUnit.getSortOrder());
        dto.setStatus(orgUnit.getStatus() != null ? orgUnit.getStatus().name() : "ACTIVE");
        dto.setStatusLabel(orgUnit.getStatus() != null ? orgUnit.getStatus().getLabel() : "正常");
        dto.setHeadcount(orgUnit.getHeadcount());
        dto.setAttributes(orgUnit.getAttributes());
        dto.setMergedIntoId(orgUnit.getMergedIntoId());
        dto.setDissolvedAt(orgUnit.getDissolvedAt());
        dto.setDissolvedReason(orgUnit.getDissolvedReason());

        enrichWithTypeInfo(dto, orgUnit.getUnitType());

        return dto;
    }

    private void enrichWithTypeInfo(OrgUnitDTO dto, String typeCode) {
        if (typeCode == null) return;
        orgUnitTypeRepository.findByTypeCode(typeCode).ifPresent(typeEntity -> {
            dto.setTypeName(typeEntity.getTypeName());
            dto.setTypeIcon(typeEntity.getIcon());
            dto.setTypeColor(null);
        });
    }

    private OrgUnitTreeDTO toTreeDTO(OrgUnit orgUnit, Map<String, OrgType> typeMap) {
        OrgUnitTreeDTO dto = new OrgUnitTreeDTO();
        dto.setId(orgUnit.getId());
        dto.setParentId(orgUnit.getParentId());
        dto.setUnitCode(orgUnit.getUnitCode());
        dto.setUnitName(orgUnit.getUnitName());
        dto.setUnitType(orgUnit.getUnitType());
        dto.setStatus(orgUnit.getStatus() != null ? orgUnit.getStatus().name() : "ACTIVE");
        dto.setStatusLabel(orgUnit.getStatus() != null ? orgUnit.getStatus().getLabel() : "正常");
        dto.setHeadcount(orgUnit.getHeadcount());
        dto.setChildren(new ArrayList<>());

        if (orgUnit.getUnitType() != null) {
            OrgType typeEntity = typeMap.get(orgUnit.getUnitType());
            if (typeEntity != null) {
                dto.setTypeName(typeEntity.getTypeName());
                dto.setTypeIcon(typeEntity.getIcon());
                dto.setCategory(typeEntity.getCategory());
                dto.setTypeColor(null);
            }
        }

        return dto;
    }

    // autoCreateClassBinding removed — replaced by SPI plugin (ClassPlugin.afterCreate)
}
