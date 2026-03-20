package com.school.management.application.organization;

import com.school.management.application.organization.command.AppointUserCommand;
import com.school.management.application.organization.command.EndAppointmentCommand;
import com.school.management.application.organization.command.TransferUserCommand;
import com.school.management.application.organization.query.OrgMemberDTO;
import com.school.management.application.organization.query.OrgStatisticsDTO;
import com.school.management.application.organization.query.UserPositionDTO;
import com.school.management.domain.organization.model.Position;
import com.school.management.infrastructure.activity.ActivityEventPublisher;
import com.school.management.domain.organization.model.OrgUnit;
import com.school.management.domain.organization.model.entity.UserPosition;
import com.school.management.domain.organization.model.valueobject.AppointmentType;
import com.school.management.domain.shared.model.valueobject.FieldChange;
import com.school.management.domain.access.model.entity.AccessRelation;
import com.school.management.domain.access.repository.AccessRelationRepository;
import com.school.management.domain.organization.repository.OrgUnitRepository;
import com.school.management.domain.organization.repository.PositionRepository;
import com.school.management.domain.organization.repository.UserPositionRepository;
import com.school.management.domain.user.repository.UserRepository;
import com.school.management.common.util.SecurityUtils;
import com.school.management.domain.shared.event.DomainEventPublisher;
import com.school.management.infrastructure.persistence.user.UserDomainMapper;
import com.school.management.infrastructure.persistence.user.UserPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class UserPositionApplicationService {

    private final UserPositionRepository userPositionRepository;
    private final PositionRepository positionRepository;
    private final OrgUnitRepository orgUnitRepository;
    private final DomainEventPublisher eventPublisher;
    private final ActivityEventPublisher activityEventPublisher;
    private final UserDomainMapper userDomainMapper;
    private final PositionAccessSyncService syncService;
    private final UserRepository userRepository;
    private final AccessRelationRepository accessRelationRepository;

    @Transactional
    public UserPositionDTO appointUser(AppointUserCommand cmd) {
        AppointmentType type = AppointmentType.FORMAL;
        if (cmd.getAppointmentType() != null) {
            try { type = AppointmentType.valueOf(cmd.getAppointmentType()); } catch (Exception ignored) {}
        }

        UserPosition up = UserPosition.appoint(
            cmd.getUserId(), cmd.getPositionId(), cmd.isPrimary(),
            type, cmd.getStartDate(), cmd.getReason(), cmd.getCreatedBy()
        );
        up = userPositionRepository.save(up);

        String appointeeName = userRepository.findById(cmd.getUserId())
            .map(u -> u.getRealName() != null ? u.getRealName() : u.getUsername())
            .orElse(cmd.getUserId().toString());
        String posName = positionRepository.findById(cmd.getPositionId())
            .map(Position::getPositionName).orElse(cmd.getPositionId().toString());
        activityEventPublisher.newEvent("organization", "USER_POSITION", "CREATE", "任命人员")
            .resourceId(up.getId())
            .changedFields(List.of(new FieldChange("userId", null, appointeeName),
                     new FieldChange("positionId", null, posName)))
            .reason(cmd.getReason())
            .publish();

        syncService.onAppoint(up);

        return toDTO(up);
    }

    @Transactional
    public void endAppointment(Long userPositionId, EndAppointmentCommand cmd) {
        UserPosition up = userPositionRepository.findById(userPositionId)
            .orElseThrow(() -> new IllegalArgumentException("任命记录不存在: " + userPositionId));
        up.endAppointment(cmd.getEndDate(), cmd.getReason());
        userPositionRepository.save(up);

        activityEventPublisher.newEvent("organization", "USER_POSITION", "UPDATE", "结束任命")
            .resourceId(userPositionId)
            .changedFields(List.of(new FieldChange("endDate", null, up.getEndDate().toString())))
            .reason(cmd.getReason())
            .publish();

        syncService.onEndAppointment(up);
    }

    @Transactional
    public void transferUser(TransferUserCommand cmd) {
        // End old appointment
        List<UserPosition> current = userPositionRepository.findCurrentByPositionId(cmd.getFromPositionId());
        UserPosition oldUp = current.stream()
            .filter(u -> u.getUserId().equals(cmd.getUserId()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("用户不在原岗位上"));
        oldUp.endAppointment(cmd.getTransferDate(), "调岗: " + cmd.getReason());
        userPositionRepository.save(oldUp);

        // Create new appointment
        UserPosition newUp = UserPosition.appoint(
            cmd.getUserId(), cmd.getToPositionId(), oldUp.isPrimary(),
            oldUp.getAppointmentType(), cmd.getTransferDate(),
            "调岗: " + cmd.getReason(), cmd.getOperatedBy()
        );
        userPositionRepository.save(newUp);

        String fromPosName = positionRepository.findById(cmd.getFromPositionId())
            .map(Position::getPositionName).orElse(cmd.getFromPositionId().toString());
        String toPosName = positionRepository.findById(cmd.getToPositionId())
            .map(Position::getPositionName).orElse(cmd.getToPositionId().toString());
        activityEventPublisher.newEvent("organization", "USER_POSITION", "TRANSFER", "人员调岗")
            .resourceId(newUp.getId())
            .changedFields(List.of(new FieldChange("positionId", fromPosName, toPosName)))
            .reason(cmd.getReason())
            .publish();

        syncService.onTransfer(oldUp, newUp);
    }

    @Transactional(readOnly = true)
    public List<UserPositionDTO> getUserPositions(Long userId) {
        return userPositionRepository.findByUserId(userId).stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<UserPositionDTO> getCurrentByPosition(Long positionId) {
        return userPositionRepository.findCurrentByPositionId(positionId).stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UserPositionDTO getPrimaryPosition(Long userId) {
        return userPositionRepository.findPrimaryByUserId(userId).map(this::toDTO).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<OrgMemberDTO> getMembersByOrgUnit(Long orgUnitId) {
        List<UserPosition> ups = userPositionRepository.findCurrentByOrgUnitId(orgUnitId);
        if (ups.isEmpty()) return List.of();
        return buildStaffDTOs(ups, orgUnitId);
    }

    /**
     * 递归获取当前组织及所有子组织的岗位人员
     */
    @Transactional(readOnly = true)
    public List<OrgMemberDTO> getMembersRecursive(Long orgUnitId) {
        // 收集当前组织 + 所有后代组织 ID
        Set<Long> allOrgIds = new LinkedHashSet<>();
        collectDescendantIds(orgUnitId, allOrgIds);

        // 批量查询所有组织的活跃岗位
        List<UserPosition> allUps = new ArrayList<>();
        Map<Long, Long> positionOrgMap = new HashMap<>(); // positionId → orgUnitId
        for (Long oid : allOrgIds) {
            List<UserPosition> ups = userPositionRepository.findCurrentByOrgUnitId(oid);
            allUps.addAll(ups);
            for (UserPosition up : ups) {
                positionOrgMap.put(up.getPositionId(), oid);
            }
        }
        if (allUps.isEmpty()) return List.of();

        // 批量获取用户信息
        Set<Long> userIds = allUps.stream().map(UserPosition::getUserId).collect(Collectors.toSet());
        Map<Long, UserPO> userMap = userDomainMapper.selectBatchIds(new ArrayList<>(userIds))
            .stream().collect(Collectors.toMap(UserPO::getId, u -> u, (a, b) -> a));

        // 批量获取岗位信息
        Set<Long> posIds = allUps.stream().map(UserPosition::getPositionId).collect(Collectors.toSet());
        Map<Long, Position> posMap = posIds.stream()
            .map(id -> positionRepository.findById(id).orElse(null))
            .filter(Objects::nonNull)
            .collect(Collectors.toMap(Position::getId, p -> p));

        // 批量获取组织名称
        Map<Long, String> orgNameMap = new HashMap<>();
        for (Long oid : allOrgIds) {
            orgUnitRepository.findById(oid).ifPresent(ou -> orgNameMap.put(oid, ou.getUnitName()));
        }

        // 归属组织 ID → 名称映射
        Set<Long> primaryOrgIds = userMap.values().stream()
            .map(UserPO::getPrimaryOrgUnitId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        for (Long oid : primaryOrgIds) {
            if (!orgNameMap.containsKey(oid)) {
                orgUnitRepository.findById(oid).ifPresent(ou -> orgNameMap.put(oid, ou.getUnitName()));
            }
        }

        return allUps.stream().map(up -> {
            OrgMemberDTO dto = new OrgMemberDTO();
            dto.setUserPositionId(up.getId());
            dto.setUserId(up.getUserId());
            dto.setPositionId(up.getPositionId());
            dto.setPrimary(up.isPrimary());
            dto.setAppointmentType(up.getAppointmentType() != null ? up.getAppointmentType().name() : "FORMAL");
            dto.setStartDate(up.getStartDate() != null ? up.getStartDate().toString() : null);
            dto.setMembershipType("staff");

            UserPO userPO = userMap.get(up.getUserId());
            if (userPO != null) {
                dto.setUserName(userPO.getRealName() != null ? userPO.getRealName() : "用户#" + up.getUserId());
                dto.setUserTypeCode(userPO.getUserTypeCode());
                dto.setPrimaryOrgUnitId(userPO.getPrimaryOrgUnitId());
                dto.setPrimaryOrgUnitName(orgNameMap.get(userPO.getPrimaryOrgUnitId()));
            } else {
                dto.setUserName("用户#" + up.getUserId());
            }

            Position pos = posMap.get(up.getPositionId());
            if (pos != null) {
                dto.setPositionName(pos.getPositionName());
                dto.setJobLevel(pos.getJobLevel());
                dto.setKeyPosition(pos.isKeyPosition());
            }

            Long posOrgId = positionOrgMap.get(up.getPositionId());
            dto.setOrgUnitId(posOrgId);
            dto.setOrgUnitName(orgNameMap.get(posOrgId));

            return dto;
        }).collect(Collectors.toList());
    }

    private void collectDescendantIds(Long orgUnitId, Set<Long> result) {
        result.add(orgUnitId);
        List<OrgUnit> children = orgUnitRepository.findByParentId(orgUnitId);
        for (OrgUnit child : children) {
            collectDescendantIds(child.getId(), result);
        }
    }

    @Transactional(readOnly = true)
    public List<OrgMemberDTO> getBelongingMembers(Long orgUnitId) {
        List<UserPO> users = userDomainMapper.findByOrgUnitId(orgUnitId);
        if (users.isEmpty()) return List.of();

        // 批量查用户的主岗信息
        Map<Long, UserPosition> primaryPositions = new HashMap<>();
        for (UserPO u : users) {
            userPositionRepository.findPrimaryByUserId(u.getId()).ifPresent(up -> primaryPositions.put(u.getId(), up));
        }

        // 批量查岗位信息
        Set<Long> posIds = primaryPositions.values().stream().map(UserPosition::getPositionId).collect(Collectors.toSet());
        Map<Long, Position> posMap = posIds.stream()
            .map(id -> positionRepository.findById(id).orElse(null))
            .filter(Objects::nonNull)
            .collect(Collectors.toMap(Position::getId, p -> p));

        return users.stream().map(u -> {
            OrgMemberDTO dto = new OrgMemberDTO();
            dto.setUserId(u.getId());
            dto.setUserName(u.getRealName());
            dto.setUserTypeCode(u.getUserTypeCode());
            dto.setMembershipType("belonging");

            UserPosition up = primaryPositions.get(u.getId());
            if (up != null) {
                dto.setUserPositionId(up.getId());
                dto.setPositionId(up.getPositionId());
                dto.setPrimary(up.isPrimary());
                dto.setAppointmentType(up.getAppointmentType() != null ? up.getAppointmentType().name() : "FORMAL");
                dto.setStartDate(up.getStartDate() != null ? up.getStartDate().toString() : null);
                Position pos = posMap.get(up.getPositionId());
                if (pos != null) {
                    dto.setPositionName(pos.getPositionName());
                    dto.setJobLevel(pos.getJobLevel());
                    dto.setKeyPosition(pos.isKeyPosition());
                }
            }
            return dto;
        }).collect(Collectors.toList());
    }

    /**
     * 直接添加成员到组织（设置 primaryOrgUnitId，不需要岗位）
     */
    @Transactional
    public void addMemberToOrg(Long orgUnitId, Long userId, Long operatedBy) {
        // 验证组织存在
        orgUnitRepository.findById(orgUnitId)
            .orElseThrow(() -> new IllegalArgumentException("组织不存在: " + orgUnitId));

        // 设置用户的 primaryOrgUnitId
        var user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("用户不存在: " + userId));
        user.setPrimaryOrgUnitId(orgUnitId);
        userRepository.save(user);

        // 确保有 staff access_relation（数据权限）
        boolean exists = accessRelationRepository.exists(
            "org_unit", orgUnitId, "staff", "user", userId);
        if (!exists) {
            AccessRelation ar = AccessRelation.builder()
                .resourceType("org_unit")
                .resourceId(orgUnitId)
                .relation("staff")
                .subjectType("user")
                .subjectId(userId)
                .includeChildren(false)
                .accessLevel(1)
                .createdBy(operatedBy)
                .createdAt(java.time.LocalDateTime.now())
                .updatedAt(java.time.LocalDateTime.now())
                .build();
            accessRelationRepository.save(ar);
        }

        String memberName = userRepository.findById(userId)
            .map(u -> u.getRealName() != null ? u.getRealName() : u.getUsername())
            .orElse(userId.toString());
        activityEventPublisher.newEvent("organization", "ORG_UNIT", "UPDATE", "添加成员")
            .resourceId(orgUnitId)
            .changedFields(List.of(new FieldChange("addMember", null, memberName)))
            .reason("添加成员: " + memberName)
            .publish();
    }

    /**
     * 从组织移除成员（清除 primaryOrgUnitId，结束所有岗位）
     */
    @Transactional
    public void removeMemberFromOrg(Long orgUnitId, Long userId, Long operatedBy) {
        // 1. 结束该用户在此组织的所有活跃岗位
        List<UserPosition> activePositions = userPositionRepository.findCurrentByOrgUnitId(orgUnitId);
        for (UserPosition up : activePositions) {
            if (up.getUserId().equals(userId)) {
                up.endAppointment(LocalDate.now(), "从组织移除");
                userPositionRepository.save(up);
            }
        }

        // 2. 清除 primaryOrgUnitId
        userRepository.findById(userId).ifPresent(user -> {
            if (orgUnitId.equals(user.getPrimaryOrgUnitId())) {
                user.setPrimaryOrgUnitId(null);
                userRepository.save(user);
            }
        });

        // 3. 清除 staff access_relation
        List<AccessRelation> rels = accessRelationRepository.findByResource("org_unit", orgUnitId);
        rels.stream()
            .filter(r -> "user".equals(r.getSubjectType())
                && r.getSubjectId().equals(userId)
                && "staff".equals(r.getRelation()))
            .forEach(r -> accessRelationRepository.deleteById(r.getId()));

        String removedName = userRepository.findById(userId)
            .map(u -> u.getRealName() != null ? u.getRealName() : u.getUsername())
            .orElse(userId.toString());
        activityEventPublisher.newEvent("organization", "ORG_UNIT", "UPDATE", "移除成员")
            .resourceId(orgUnitId)
            .changedFields(List.of(new FieldChange("removeMember", removedName, null)))
            .reason("移除成员: " + removedName)
            .publish();
    }

    @Transactional(readOnly = true)
    public OrgStatisticsDTO getOrgStatistics(Long orgUnitId) {
        OrgStatisticsDTO stats = new OrgStatisticsDTO();
        stats.setOrgUnitId(orgUnitId);

        // 归属成员数 (primary_org_unit_id)
        stats.setBelongingCount(userDomainMapper.countByPrimaryOrgUnitId(orgUnitId));

        // 岗位人员数 (通过 user_positions)
        List<UserPosition> staffPositions = userPositionRepository.findCurrentByOrgUnitId(orgUnitId);
        long uniqueStaff = staffPositions.stream().map(UserPosition::getUserId).distinct().count();
        stats.setStaffCount(uniqueStaff);

        // 按用户类型细分
        List<java.util.Map<String, Object>> typeCounts = userDomainMapper.countByPrimaryOrgUnitIdGroupByType(orgUnitId);
        Map<String, Long> countByType = new LinkedHashMap<>();
        for (java.util.Map<String, Object> row : typeCounts) {
            String typeCode = row.get("user_type_code") != null ? row.get("user_type_code").toString() : "UNKNOWN";
            Long cnt = ((Number) row.get("cnt")).longValue();
            countByType.put(typeCode, cnt);
        }
        stats.setCountByUserType(countByType);

        return stats;
    }

    /**
     * 批量构建 staff OrgMemberDTO，填充用户归属组织信息
     */
    private List<OrgMemberDTO> buildStaffDTOs(List<UserPosition> ups, Long currentOrgUnitId) {
        Set<Long> userIds = ups.stream().map(UserPosition::getUserId).collect(Collectors.toSet());
        Map<Long, UserPO> userMap = userDomainMapper.selectBatchIds(new ArrayList<>(userIds))
            .stream().collect(Collectors.toMap(UserPO::getId, u -> u, (a, b) -> a));

        Set<Long> posIds = ups.stream().map(UserPosition::getPositionId).collect(Collectors.toSet());
        Map<Long, Position> posMap = posIds.stream()
            .map(id -> positionRepository.findById(id).orElse(null))
            .filter(Objects::nonNull)
            .collect(Collectors.toMap(Position::getId, p -> p));

        // 归属组织名称
        Set<Long> primaryOrgIds = userMap.values().stream()
            .map(UserPO::getPrimaryOrgUnitId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        Map<Long, String> orgNameMap = new HashMap<>();
        for (Long oid : primaryOrgIds) {
            orgUnitRepository.findById(oid).ifPresent(ou -> orgNameMap.put(oid, ou.getUnitName()));
        }

        return ups.stream().map(up -> {
            OrgMemberDTO dto = new OrgMemberDTO();
            dto.setUserPositionId(up.getId());
            dto.setUserId(up.getUserId());
            dto.setPositionId(up.getPositionId());
            dto.setPrimary(up.isPrimary());
            dto.setAppointmentType(up.getAppointmentType() != null ? up.getAppointmentType().name() : "FORMAL");
            dto.setStartDate(up.getStartDate() != null ? up.getStartDate().toString() : null);
            dto.setMembershipType("staff");
            dto.setOrgUnitId(currentOrgUnitId);

            UserPO userPO = userMap.get(up.getUserId());
            if (userPO != null) {
                dto.setUserName(userPO.getRealName() != null ? userPO.getRealName() : "用户#" + up.getUserId());
                dto.setUserTypeCode(userPO.getUserTypeCode());
                dto.setPrimaryOrgUnitId(userPO.getPrimaryOrgUnitId());
                dto.setPrimaryOrgUnitName(orgNameMap.get(userPO.getPrimaryOrgUnitId()));
            } else {
                dto.setUserName("用户#" + up.getUserId());
            }

            Position pos = posMap.get(up.getPositionId());
            if (pos != null) {
                dto.setPositionName(pos.getPositionName());
                dto.setJobLevel(pos.getJobLevel());
                dto.setKeyPosition(pos.isKeyPosition());
            }
            return dto;
        }).collect(Collectors.toList());
    }

    private UserPositionDTO toDTO(UserPosition up) {
        UserPositionDTO dto = new UserPositionDTO();
        dto.setId(up.getId());
        dto.setUserId(up.getUserId());
        dto.setPositionId(up.getPositionId());
        dto.setPrimary(up.isPrimary());
        dto.setAppointmentType(up.getAppointmentType() != null ? up.getAppointmentType().name() : "FORMAL");
        dto.setStartDate(up.getStartDate() != null ? up.getStartDate().toString() : null);
        dto.setEndDate(up.getEndDate() != null ? up.getEndDate().toString() : null);
        dto.setAppointmentReason(up.getAppointmentReason());
        dto.setDepartureReason(up.getDepartureReason());
        dto.setCurrent(up.isCurrent());

        // Enrich names
        UserPO userPO = userDomainMapper.selectById(up.getUserId());
        if (userPO != null) dto.setUserName(userPO.getRealName());

        positionRepository.findById(up.getPositionId()).ifPresent(p -> {
            dto.setPositionName(p.getPositionName());
            dto.setOrgUnitId(p.getOrgUnitId());
            orgUnitRepository.findById(p.getOrgUnitId()).ifPresent(ou -> dto.setOrgUnitName(ou.getUnitName()));
        });

        return dto;
    }
}
