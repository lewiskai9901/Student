package com.school.management.application.organization;

import com.school.management.application.organization.query.OrgMemberDTO;
import com.school.management.application.organization.query.OrgStatisticsDTO;
import com.school.management.domain.access.repository.AccessRelationRepository;
import com.school.management.domain.organization.model.OrgUnit;
import com.school.management.domain.organization.repository.OrgUnitRepository;
import com.school.management.infrastructure.extension.PolicyContext;
import com.school.management.infrastructure.extension.PolicyRegistry;
import com.school.management.infrastructure.persistence.user.UserDomainMapper;
import com.school.management.infrastructure.persistence.user.UserPO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

/**
 * Regression test for {@link OrgMemberService} after the membership-unified-relation
 * refactor: 成员增删改 (Task 2.2) 与读路径 (Task 3.1) 均走 {@link MembershipResolver},
 * 不再读写 {@code users.primary_org_unit_id} 外键, 也不手工建/删 access_relation 行。
 *
 * <p>历史背景: commit 1168eec3 曾修过 removeMember 误删整个 org 全部成员的 bug
 * (deleteByResource vs deleteByResourceAndSubject)。现在删除归属统一交给
 * MembershipResolver.clearMembership(userId) — 归属唯一, 不会跨 org 误删。
 */
@ExtendWith(MockitoExtension.class)
class OrgMemberServiceTest {

    @Mock
    UserDomainMapper userDomainMapper;

    @Mock
    OrgUnitRepository orgUnitRepository;

    @Mock
    AccessRelationRepository accessRelationRepository;

    @Mock
    PolicyRegistry policyRegistry;

    @Mock
    MembershipResolver membershipResolver;

    @InjectMocks
    OrgMemberService service;

    @org.junit.jupiter.api.BeforeEach
    void stubPolicyRegistryDefaults() {
        // Default: no policy violations; tests that care should override per-case.
        lenient().when(policyRegistry.enforce(any(PolicyContext.class)))
                .thenReturn(Collections.emptyList());
        lenient().when(policyRegistry.check(any(PolicyContext.class)))
                .thenReturn(Collections.emptyList());
    }

    @Test
    void addMember_shouldDelegateToMembershipResolver_andNotWriteFk() {
        when(orgUnitRepository.findById(100L))
                .thenReturn(Optional.of(mock(com.school.management.domain.organization.model.OrgUnit.class)));

        service.addMember(100L, 999L);

        // 统一归属写入走 MembershipResolver (grant-or-replace)。
        verify(membershipResolver).setMembership(999L, 100L);

        // 不再写 primary_org_unit_id 外键, 也不手工建 access_relation。
        verify(userDomainMapper, never()).setPrimaryOrgUnitId(any(), any());
        verify(accessRelationRepository, never()).save(any());
    }

    @Test
    void removeMember_shouldClearMembership_whenUserBelongsToThatOrg() {
        when(membershipResolver.orgOf(999L)).thenReturn(Optional.of(100L));

        service.removeMember(100L, 999L);

        verify(membershipResolver).clearMembership(999L);
        // 不再写 primary_org_unit_id 外键, 也不手工删 access_relation。
        verify(userDomainMapper, never()).clearPrimaryOrgUnitIdForUser(any(), any());
        verify(accessRelationRepository, never())
                .deleteByResourceAndSubject(any(), any(), any(), any());
    }

    @Test
    void removeMember_shouldNotClear_whenUserBelongsToDifferentOrg() {
        // 用户归属在另一个 org — 不得误删其归属。
        when(membershipResolver.orgOf(999L)).thenReturn(Optional.of(200L));

        service.removeMember(100L, 999L);

        verify(membershipResolver, never()).clearMembership(any());
    }

    // ==================== 读路径: 归属改走 MembershipResolver ====================

    @Test
    void getBelongingMembers_shouldReadFromMembershipResolver_notFk() {
        OrgUnit org = mock(OrgUnit.class);
        when(org.getUnitName()).thenReturn("一班");
        when(orgUnitRepository.findById(100L)).thenReturn(Optional.of(org));

        // 归属成员来自 member 关系, 而非 primary_org_unit_id 外键
        when(membershipResolver.membersOf(100L)).thenReturn(List.of(1L, 2L));

        UserPO u1 = new UserPO();
        u1.setId(1L);
        u1.setRealName("张三");
        u1.setUserTypeCode("STUDENT");
        UserPO u2 = new UserPO();
        u2.setId(2L);
        u2.setUsername("lisi");
        u2.setUserTypeCode("STUDENT");
        when(userDomainMapper.selectBatchIds(List.of(1L, 2L))).thenReturn(List.of(u1, u2));

        List<OrgMemberDTO> result = service.getBelongingMembers(100L);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(OrgMemberDTO::getUserId).containsExactly(1L, 2L);
        assertThat(result).allSatisfy(dto -> assertThat(dto.getPrimaryOrgUnitId()).isEqualTo(100L));

        // 不再查 FK 口径
        verify(userDomainMapper, never()).findByOrgUnitId(any());
    }

    @Test
    void getMembersRecursive_shouldUseSubtree_notReExpand() {
        when(orgUnitRepository.findById(100L))
                .thenReturn(Optional.of(mock(OrgUnit.class)));

        // membersOfSubtree 已含自身 + 子树, 不得再叠加一次展开
        when(membershipResolver.membersOfSubtree(100L)).thenReturn(List.of(1L, 2L));
        when(membershipResolver.orgOf(1L)).thenReturn(Optional.of(100L));
        when(membershipResolver.orgOf(2L)).thenReturn(Optional.of(101L));

        OrgUnit o100 = mock(OrgUnit.class);
        when(o100.getId()).thenReturn(100L);
        when(o100.getUnitName()).thenReturn("一班");
        OrgUnit o101 = mock(OrgUnit.class);
        when(o101.getId()).thenReturn(101L);
        when(o101.getUnitName()).thenReturn("二班");
        when(orgUnitRepository.findByIds(any())).thenReturn(List.of(o100, o101));

        UserPO u1 = new UserPO();
        u1.setId(1L);
        u1.setRealName("张三");
        UserPO u2 = new UserPO();
        u2.setId(2L);
        u2.setRealName("李四");
        when(userDomainMapper.selectBatchIds(List.of(1L, 2L))).thenReturn(List.of(u1, u2));

        List<OrgMemberDTO> result = service.getMembersRecursive(100L);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(OrgMemberDTO::getPrimaryOrgUnitId).containsExactly(100L, 101L);
        assertThat(result).extracting(OrgMemberDTO::getPrimaryOrgUnitName).containsExactly("一班", "二班");

        // 不再查 FK 口径
        verify(userDomainMapper, never()).findByOrgUnitIdIn(any());
    }

    @Test
    void getOrgStatistics_shouldCountFromMembershipResolver_notFk() {
        when(orgUnitRepository.findById(100L))
                .thenReturn(Optional.of(mock(OrgUnit.class)));

        when(membershipResolver.countMembers(100L)).thenReturn(5L);
        when(membershipResolver.countMembersByType(100L))
                .thenReturn(Map.of("STUDENT", 4L, "TEACHER", 1L));

        OrgStatisticsDTO dto = service.getOrgStatistics(100L);

        assertThat(dto.getBelongingCount()).isEqualTo(5L);
        assertThat(dto.getCountByUserType()).containsEntry("STUDENT", 4L).containsEntry("TEACHER", 1L);

        // 不再查 FK 口径
        verify(userDomainMapper, never()).countByPrimaryOrgUnitId(any());
        verify(userDomainMapper, never()).countByPrimaryOrgUnitIdGroupByType(any());
    }

    @Test
    void endAllByOrgUnitId_shouldUseDeleteByResource_thisIsCorrectForDissolution() {
        // 组织解散 — 删除该 org 全部 member 关系是预期行为。
        service.endAllByOrgUnitId(100L, "解散");
        verify(accessRelationRepository).deleteByResource("org_unit", 100L);
        // 不再清 primary_org_unit_id 外键。
        verify(userDomainMapper, never()).clearPrimaryOrgUnitId(any());
    }
}
