package com.school.management.application.organization;

import com.school.management.application.organization.query.OrgMemberDTO;
import com.school.management.application.organization.query.OrgStatisticsDTO;
import com.school.management.domain.access.model.entity.AccessRelation;
import com.school.management.domain.access.repository.AccessRelationRepository;
import com.school.management.domain.organization.model.OrgUnit;
import com.school.management.domain.organization.repository.OrgUnitRepository;
import com.school.management.infrastructure.persistence.user.UserDomainMapper;
import com.school.management.infrastructure.persistence.user.UserPO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 组织成员管理服务
 * 基于 user.primary_org_unit_id 归属关系 + access_relations 访问关系
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class OrgMemberService {

    private final UserDomainMapper userDomainMapper;
    private final OrgUnitRepository orgUnitRepository;
    private final AccessRelationRepository accessRelationRepository;

    /**
     * 获取归属成员列表（primary_org_unit_id = orgUnitId）
     */
    @Transactional(readOnly = true)
    public List<OrgMemberDTO> getBelongingMembers(Long orgUnitId) {
        OrgUnit orgUnit = orgUnitRepository.findById(orgUnitId)
                .orElseThrow(() -> new IllegalArgumentException("OrgUnit not found: " + orgUnitId));

        List<UserPO> users = userDomainMapper.findByOrgUnitId(orgUnitId);
        return users.stream()
                .map(u -> toMemberDTO(u, orgUnitId, orgUnit.getUnitName()))
                .collect(Collectors.toList());
    }

    /**
     * 递归获取成员列表（本组织 + 所有下级组织）
     */
    @Transactional(readOnly = true)
    public List<OrgMemberDTO> getMembersRecursive(Long orgUnitId) {
        OrgUnit orgUnit = orgUnitRepository.findById(orgUnitId)
                .orElseThrow(() -> new IllegalArgumentException("OrgUnit not found: " + orgUnitId));

        // Collect this org + all descendant IDs
        List<Long> allOrgIds = new ArrayList<>();
        allOrgIds.add(orgUnitId);
        collectDescendantIds(orgUnitId, allOrgIds);

        if (allOrgIds.isEmpty()) {
            return Collections.emptyList();
        }

        // Build orgId -> orgName map (batch query instead of N+1)
        List<OrgUnit> allOrgs = orgUnitRepository.findByIds(allOrgIds);
        Map<Long, String> orgNameMap = allOrgs.stream()
                .collect(Collectors.toMap(OrgUnit::getId, OrgUnit::getUnitName, (a, b) -> a));

        List<UserPO> users = userDomainMapper.findByOrgUnitIdIn(allOrgIds);
        return users.stream()
                .map(u -> toMemberDTO(u, u.getPrimaryOrgUnitId(),
                        orgNameMap.getOrDefault(u.getPrimaryOrgUnitId(), "")))
                .collect(Collectors.toList());
    }

    /**
     * 获取组织统计信息
     */
    @Transactional(readOnly = true)
    public OrgStatisticsDTO getOrgStatistics(Long orgUnitId) {
        orgUnitRepository.findById(orgUnitId)
                .orElseThrow(() -> new IllegalArgumentException("OrgUnit not found: " + orgUnitId));

        OrgStatisticsDTO dto = new OrgStatisticsDTO();
        dto.setOrgUnitId(orgUnitId);
        dto.setBelongingCount(userDomainMapper.countByPrimaryOrgUnitId(orgUnitId));

        // Count by user type
        List<Map<String, Object>> typeCounts = userDomainMapper.countByPrimaryOrgUnitIdGroupByType(orgUnitId);
        Map<String, Long> countByType = new LinkedHashMap<>();
        if (typeCounts != null) {
            for (Map<String, Object> row : typeCounts) {
                String typeCode = row.get("user_type_code") != null ? row.get("user_type_code").toString() : "UNKNOWN";
                Long cnt = row.get("cnt") != null ? ((Number) row.get("cnt")).longValue() : 0L;
                countByType.put(typeCode, cnt);
            }
        }
        dto.setCountByUserType(countByType);

        return dto;
    }

    /**
     * 添加成员：设置 user.primary_org_unit_id，创建 access_relation
     */
    @Transactional
    public void addMember(Long orgUnitId, Long userId) {
        OrgUnit orgUnit = orgUnitRepository.findById(orgUnitId)
                .orElseThrow(() -> new IllegalArgumentException("OrgUnit not found: " + orgUnitId));

        // Set user's primary org unit
        userDomainMapper.setPrimaryOrgUnitId(userId, orgUnitId);

        // Create access relation (user -> org_unit, member)
        if (!accessRelationRepository.exists("org_unit", orgUnitId, "member", "user", userId)) {
            AccessRelation relation = AccessRelation.builder()
                    .resourceType("org_unit")
                    .resourceId(orgUnitId)
                    .relation("member")
                    .subjectType("user")
                    .subjectId(userId)
                    .build();
            accessRelationRepository.save(relation);
        }

        log.info("Added user {} as member of org {}", userId, orgUnitId);
    }

    /**
     * 移除成员：清除 user.primary_org_unit_id，删除 access_relation
     */
    @Transactional
    public void removeMember(Long orgUnitId, Long userId) {
        // Clear user's primary org unit (only if it matches)
        userDomainMapper.clearPrimaryOrgUnitIdForUser(userId, orgUnitId);

        // Remove access relation
        accessRelationRepository.deleteByResourceAndSubject("org_unit", orgUnitId, "user", userId);

        log.info("Removed user {} from org {}", userId, orgUnitId);
    }

    /**
     * 终止组织的所有成员归属（组织删除/解散时使用）
     */
    @Transactional
    public void endAllByOrgUnitId(Long orgUnitId, String reason) {
        int count = userDomainMapper.clearPrimaryOrgUnitId(orgUnitId);
        accessRelationRepository.deleteByResource("org_unit", orgUnitId);
        log.info("Ended all memberships for org {} (count={}, reason={})", orgUnitId, count, reason);
    }

    // ==================== Helper methods ====================

    private void collectDescendantIds(Long parentId, List<Long> result) {
        List<OrgUnit> children = orgUnitRepository.findByParentId(parentId);
        for (OrgUnit child : children) {
            result.add(child.getId());
            collectDescendantIds(child.getId(), result);
        }
    }

    private OrgMemberDTO toMemberDTO(UserPO user, Long orgUnitId, String orgUnitName) {
        OrgMemberDTO dto = new OrgMemberDTO();
        dto.setUserId(user.getId());
        dto.setUserName(user.getRealName() != null ? user.getRealName() : user.getUsername());
        dto.setUserTypeCode(user.getUserTypeCode());
        dto.setMembershipType("belonging");
        dto.setPrimaryOrgUnitId(user.getPrimaryOrgUnitId());
        dto.setPrimaryOrgUnitName(orgUnitName);
        return dto;
    }
}
