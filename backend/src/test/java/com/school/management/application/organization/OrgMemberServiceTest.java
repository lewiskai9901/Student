package com.school.management.application.organization;

import com.school.management.domain.access.repository.AccessRelationRepository;
import com.school.management.domain.organization.repository.OrgUnitRepository;
import com.school.management.infrastructure.extension.PolicyContext;
import com.school.management.infrastructure.extension.PolicyRegistry;
import com.school.management.infrastructure.persistence.user.UserDomainMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

/**
 * Regression test for {@link OrgMemberService} after the membership-unified-relation
 * refactor (Task 2.2): 成员增删改只走 {@link MembershipResolver}, 不再写
 * {@code users.primary_org_unit_id} 外键, 也不手工建/删 access_relation 行。
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

    @Test
    void endAllByOrgUnitId_shouldUseDeleteByResource_thisIsCorrectForDissolution() {
        // 组织解散 — 删除该 org 全部 member 关系是预期行为。
        service.endAllByOrgUnitId(100L, "解散");
        verify(accessRelationRepository).deleteByResource("org_unit", 100L);
        // 不再清 primary_org_unit_id 外键。
        verify(userDomainMapper, never()).clearPrimaryOrgUnitId(any());
    }
}
