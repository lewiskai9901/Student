package com.school.management.application.organization;

import com.school.management.application.organization.command.CreatePositionCommand;
import com.school.management.application.organization.command.UpdatePositionCommand;
import com.school.management.application.organization.query.PositionDTO;
import com.school.management.application.organization.query.PositionStaffingDTO;
import com.school.management.domain.organization.model.Position;
import com.school.management.infrastructure.activity.ActivityEventPublisher;
import com.school.management.domain.organization.model.entity.UserPosition;
import com.school.management.domain.organization.model.OrgUnit;
import com.school.management.domain.shared.model.valueobject.FieldChange;
import com.school.management.domain.organization.repository.OrgUnitRepository;
import com.school.management.domain.organization.repository.PositionRepository;
import com.school.management.domain.organization.repository.UserPositionRepository;
import com.school.management.domain.shared.event.DomainEventPublisher;
import com.school.management.infrastructure.persistence.user.UserDomainMapper;
import com.school.management.infrastructure.persistence.user.UserPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class PositionApplicationService {

    private final PositionRepository positionRepository;
    private final UserPositionRepository userPositionRepository;
    private final OrgUnitRepository orgUnitRepository;
    private final DomainEventPublisher eventPublisher;
    private final ActivityEventPublisher activityEventPublisher;
    private final UserDomainMapper userDomainMapper;

    @Transactional
    public PositionDTO createPosition(CreatePositionCommand cmd) {
        if (positionRepository.existsByPositionCode(cmd.getPositionCode())) {
            throw new IllegalArgumentException("岗位编码已存在: " + cmd.getPositionCode());
        }
        Position position = Position.create(cmd.getPositionCode(), cmd.getPositionName(),
            cmd.getOrgUnitId(), cmd.getHeadcount() != null ? cmd.getHeadcount() : 1, cmd.getCreatedBy());
        position.setJobLevel(cmd.getJobLevel());
        position.setReportsToId(cmd.getReportsToId());
        position.setResponsibilities(cmd.getResponsibilities());
        position.setRequirements(cmd.getRequirements());

        position = positionRepository.save(position);
        position.getDomainEvents().forEach(eventPublisher::publish);
        position.clearDomainEvents();

        // Publish unified activity event
        activityEventPublisher.newEvent("organization", "POSITION", "CREATE", "创建岗位")
            .resourceId(position.getId())
            .resourceName(position.getPositionName())
            .changedFields(List.of(new FieldChange("positionName", null, position.getPositionName())))
            .publish();

        return toDTO(position);
    }

    @Transactional
    public PositionDTO updatePosition(Long id, UpdatePositionCommand cmd) {
        Position position = positionRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("岗位不存在: " + id));

        List<FieldChange> changes = position.update(
            cmd.getPositionName(), cmd.getJobLevel(),
            cmd.getHeadcount() != null ? cmd.getHeadcount() : position.getHeadcount(),
            cmd.getReportsToId(), cmd.getResponsibilities(), cmd.getRequirements(),
            cmd.isKeyPosition(),
            cmd.getSortOrder() != null ? cmd.getSortOrder() : position.getSortOrder(),
            cmd.getUpdatedBy()
        );

        position = positionRepository.save(position);
        position.getDomainEvents().forEach(eventPublisher::publish);
        position.clearDomainEvents();

        if (!changes.isEmpty()) {
            activityEventPublisher.newEvent("organization", "POSITION", "UPDATE", "更新岗位")
                .resourceId(position.getId())
                .resourceName(position.getPositionName())
                .changedFields(new ArrayList<>(changes))
                .publish();
        }

        return toDTO(position);
    }

    @Transactional
    public void deletePosition(Long id) {
        positionRepository.deleteById(id);
    }

    @Transactional
    public void enablePosition(Long id) {
        Position p = positionRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("岗位不存在: " + id));
        p.enable();
        positionRepository.save(p);
    }

    @Transactional
    public void disablePosition(Long id) {
        Position p = positionRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("岗位不存在: " + id));
        p.disable();
        positionRepository.save(p);
    }

    @Transactional(readOnly = true)
    public PositionDTO getPosition(Long id) {
        Position p = positionRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("岗位不存在: " + id));
        return toDTO(p);
    }

    @Transactional(readOnly = true)
    public List<PositionDTO> getPositionsByOrgUnit(Long orgUnitId) {
        List<Position> positions = positionRepository.findByOrgUnitId(orgUnitId);
        return positions.stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PositionStaffingDTO getStaffing(Long orgUnitId) {
        OrgUnit orgUnit = orgUnitRepository.findById(orgUnitId)
            .orElseThrow(() -> new IllegalArgumentException("组织不存在: " + orgUnitId));

        List<Position> positions = positionRepository.findByOrgUnitId(orgUnitId);
        PositionStaffingDTO dto = new PositionStaffingDTO();
        dto.setOrgUnitId(orgUnitId);
        dto.setOrgUnitName(orgUnit.getUnitName());

        int totalHeadcount = 0;
        int actualCount = 0;
        int vacantCount = 0;
        int overstaffedCount = 0;
        List<PositionStaffingDTO.PositionStaffingItem> items = new ArrayList<>();

        for (Position p : positions) {
            int current = userPositionRepository.countCurrentByPositionId(p.getId());
            totalHeadcount += p.getHeadcount();
            actualCount += current;
            if (p.getHeadcount() > current) vacantCount += (p.getHeadcount() - current);
            if (current > p.getHeadcount()) overstaffedCount++;

            PositionStaffingDTO.PositionStaffingItem item = new PositionStaffingDTO.PositionStaffingItem();
            item.setPositionName(p.getPositionName());
            item.setHeadcount(p.getHeadcount());
            item.setActualCount(current);
            items.add(item);
        }

        dto.setTotalHeadcount(totalHeadcount);
        dto.setActualCount(actualCount);
        dto.setVacantCount(vacantCount);
        dto.setOverstaffedCount(overstaffedCount);
        dto.setPositions(items);
        return dto;
    }

    private PositionDTO toDTO(Position p) {
        PositionDTO dto = new PositionDTO();
        dto.setId(p.getId());
        dto.setPositionCode(p.getPositionCode());
        dto.setPositionName(p.getPositionName());
        dto.setOrgUnitId(p.getOrgUnitId());
        dto.setJobLevel(p.getJobLevel());
        dto.setHeadcount(p.getHeadcount());
        dto.setKeyPosition(p.isKeyPosition());
        dto.setEnabled(p.isEnabled());
        dto.setReportsToId(p.getReportsToId());
        dto.setResponsibilities(p.getResponsibilities());
        dto.setRequirements(p.getRequirements());

        // org unit name
        orgUnitRepository.findById(p.getOrgUnitId()).ifPresent(ou -> dto.setOrgUnitName(ou.getUnitName()));

        // reports-to name
        if (p.getReportsToId() != null) {
            positionRepository.findById(p.getReportsToId())
                .ifPresent(rt -> dto.setReportsToName(rt.getPositionName()));
        }

        // holders
        List<UserPosition> currentHolders = userPositionRepository.findCurrentByPositionId(p.getId());
        int currentCount = currentHolders.size();
        dto.setCurrentCount(currentCount);
        dto.setVacantCount(Math.max(0, p.getHeadcount() - currentCount));

        if (!currentHolders.isEmpty()) {
            Set<Long> userIds = currentHolders.stream().map(UserPosition::getUserId).collect(Collectors.toSet());
            Map<Long, String> nameMap = Map.of();
            if (!userIds.isEmpty()) {
                List<UserPO> users = userDomainMapper.selectBatchIds(new ArrayList<>(userIds));
                nameMap = users.stream().collect(Collectors.toMap(UserPO::getId, UserPO::getRealName, (a, b) -> a));
            }
            Map<Long, String> finalNameMap = nameMap;
            dto.setHolders(currentHolders.stream().map(h -> {
                PositionDTO.UserPositionSummaryDTO s = new PositionDTO.UserPositionSummaryDTO();
                s.setUserId(h.getUserId());
                s.setUserName(finalNameMap.getOrDefault(h.getUserId(), "用户#" + h.getUserId()));
                s.setPrimary(h.isPrimary());
                s.setAppointmentType(h.getAppointmentType() != null ? h.getAppointmentType().name() : "FORMAL");
                return s;
            }).collect(Collectors.toList()));
        } else {
            dto.setHolders(List.of());
        }

        return dto;
    }
}
