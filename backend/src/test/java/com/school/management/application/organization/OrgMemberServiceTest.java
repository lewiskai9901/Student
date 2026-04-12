package com.school.management.application.organization;

import com.school.management.domain.access.repository.AccessRelationRepository;
import com.school.management.domain.organization.repository.OrgUnitRepository;
import com.school.management.infrastructure.persistence.user.UserDomainMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Regression test for {@link OrgMemberService#removeMember(Long, Long)}.
 *
 * <p>Guards against the data corruption bug fixed in commit 1168eec3:
 * the pre-fix implementation called
 * {@code accessRelationRepository.deleteByResource("org_unit", orgUnitId)},
 * which wiped out ALL members of the target org instead of removing just
 * the one user. The post-fix implementation correctly uses
 * {@code deleteByResourceAndSubject(resourceType, resourceId, subjectType, subjectId)}.
 */
@ExtendWith(MockitoExtension.class)
class OrgMemberServiceTest {

    @Mock
    UserDomainMapper userDomainMapper;

    @Mock
    OrgUnitRepository orgUnitRepository;

    @Mock
    AccessRelationRepository accessRelationRepository;

    @InjectMocks
    OrgMemberService service;

    @Test
    void removeMember_shouldOnlyDeleteSpecificUserRelation_notAllOrgRelations() {
        // This is the regression test for the data corruption bug.
        // Pre-fix: deleteByResource("org_unit", orgUnitId) deleted ALL users.
        // Post-fix: deleteByResourceAndSubject(resourceType, resourceId, subjectType, subjectId).
        service.removeMember(100L, 999L);

        // The correct method with 4 args must be called.
        verify(accessRelationRepository).deleteByResourceAndSubject(
                eq("org_unit"), eq(100L), eq("user"), eq(999L));

        // The broken method must NOT be called (it would delete all members).
        verify(accessRelationRepository, never()).deleteByResource("org_unit", 100L);
    }

    @Test
    void removeMember_shouldAlsoClearUserPrimaryOrgUnit() {
        service.removeMember(100L, 999L);
        // Verify the user's primary org unit assignment is cleared (user-scoped clear,
        // not the org-wide clear used during dissolution).
        verify(userDomainMapper).clearPrimaryOrgUnitIdForUser(999L, 100L);
    }

    @Test
    void endAllByOrgUnitId_shouldUseDeleteByResource_thisIsCorrectForDissolution() {
        // This method is called during org dissolution — deleting ALL relations is intentional.
        service.endAllByOrgUnitId(100L, "解散");
        verify(accessRelationRepository).deleteByResource("org_unit", 100L);
    }
}
