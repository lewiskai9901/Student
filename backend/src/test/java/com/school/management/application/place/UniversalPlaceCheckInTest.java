package com.school.management.application.place;

import com.school.management.domain.access.repository.AccessRelationRepository;
import com.school.management.domain.organization.repository.OrgUnitRepository;
import com.school.management.domain.place.model.aggregate.UniversalPlace;
import com.school.management.domain.place.model.entity.UniversalPlaceOccupant;
import com.school.management.domain.place.model.valueobject.PlaceStatus;
import com.school.management.domain.place.repository.UniversalPlaceOccupantRepository;
import com.school.management.domain.place.repository.UniversalPlaceRepository;
import com.school.management.domain.place.repository.UniversalPlaceTypeRepository;
import com.school.management.domain.place.service.PlaceInheritanceService;
import com.school.management.domain.user.model.aggregate.User;
import com.school.management.domain.user.model.valueobject.UserStatus;
import com.school.management.domain.user.repository.UserRepository;
import com.school.management.domain.user.repository.UserTypeRepository;
import com.school.management.infrastructure.activity.ActivityEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Regression test for
 * {@link UniversalPlaceApplicationService#checkIn(Long, UniversalPlaceApplicationService.CheckInCommand)}.
 *
 * <p>Guards against the overbooking race condition fixed in commit b08f793d.
 * Pre-fix: the service did {@code read → increment in memory → save}, allowing two
 * concurrent check-ins to both see {@code currentOccupancy < capacity} and push the
 * place past capacity.
 * Post-fix: the service relies on {@link UniversalPlaceRepository#atomicIncrementOccupancy(Long)},
 * which performs a single conditional UPDATE at the database level. When that atomic
 * call returns {@code false} (the place was filled by a concurrent winner between our
 * {@code findById} and our increment attempt), the occupant record we just persisted
 * MUST be rolled back and the caller must see {@code IllegalStateException("场所已满...")}.
 *
 * <p>Without the rollback, a losing concurrent caller would leave an orphaned
 * occupant row behind — silently corrupting occupancy data.
 */
@ExtendWith(MockitoExtension.class)
class UniversalPlaceCheckInTest {

    @Mock UniversalPlaceRepository placeRepository;
    @Mock UniversalPlaceTypeRepository placeTypeRepository;
    @Mock UniversalPlaceOccupantRepository occupantRepository;
    @Mock OrgUnitRepository orgUnitRepository;
    @Mock UserRepository userRepository;
    @Mock UserTypeRepository userTypeRepository;
    @Mock AccessRelationRepository accessRelationRepository;
    @Mock PlaceInheritanceService inheritanceService;
    @Mock ActivityEventPublisher activityEventPublisher;

    @InjectMocks UniversalPlaceApplicationService service;

    private static final Long PLACE_ID = 100L;
    private static final Long USER_ID = 999L;
    private static final Long SAVED_OCCUPANT_ID = 77L;

    private UniversalPlace fullPlace;
    private User user;

    @BeforeEach
    void setUp() {
        // A place that's about to hit capacity. currentOccupancy equals capacity here
        // to represent the "concurrent winner already filled it" state — the atomic UPDATE
        // in the repo will refuse our increment because current_occupancy >= capacity.
        fullPlace = UniversalPlace.builder()
                .id(PLACE_ID)
                .placeCode("R101")
                .placeName("101宿舍")
                .typeCode("DORM_ROOM")
                .capacity(4)
                .currentOccupancy(4)
                .status(PlaceStatus.NORMAL)
                .build();

        // Use the domain reconstruct() factory so User has a real id and gender.
        user = User.reconstruct(
                USER_ID,
                "alice",
                "encodedpwd",
                "Alice",
                null, null, null, null,
                1,                   // gender
                null, null,
                null,                // primaryOrgUnitId
                "STUDENT",
                UserStatus.ENABLED,
                null, null, null, null,
                false,
                Collections.emptyList(),
                null, null
        );
    }

    @Test
    void checkIn_whenAtomicIncrementReturnsFalse_throwsAndRollsBackOccupant() {
        when(placeRepository.findById(PLACE_ID)).thenReturn(Optional.of(fullPlace));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        // No access relations — skip the orgUnitRepository branch entirely.
        when(accessRelationRepository.findBySubjectAndResourceType("user", USER_ID, "org_unit"))
                .thenReturn(Collections.emptyList());

        // save() must return a persisted occupant with a non-null id so that
        // the rollback can call deleteById(occupant.getId()).
        when(occupantRepository.save(any(UniversalPlaceOccupant.class)))
                .thenAnswer(inv -> {
                    UniversalPlaceOccupant o = inv.getArgument(0);
                    o.setId(SAVED_OCCUPANT_ID);
                    return o;
                });

        // The critical stub: atomic increment fails (place was filled concurrently).
        when(placeRepository.atomicIncrementOccupancy(PLACE_ID)).thenReturn(false);

        UniversalPlaceApplicationService.CheckInCommand cmd =
                new UniversalPlaceApplicationService.CheckInCommand();
        cmd.setOccupantType("user");
        cmd.setOccupantId(USER_ID);
        // Leave positionNo null to skip the isPositionOccupied branch.

        // Act + assert: caller sees the "full" error.
        assertThatThrownBy(() -> service.checkIn(PLACE_ID, cmd))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("场所已满");

        // Core regression guard: the just-created occupant must be deleted
        // so it doesn't linger as an orphan after the atomic increment refused us.
        verify(occupantRepository).deleteById(eq(SAVED_OCCUPANT_ID));

        // And the audit event must NOT be published for a failed check-in.
        verify(activityEventPublisher, never()).newEvent(any(), any(), any());
        verify(activityEventPublisher, never()).newEvent(any(), any(), any(), any());
    }

    @Test
    void checkIn_whenAtomicIncrementReturnsFalse_doesNotAttemptDecrement() {
        // Paranoia check: we must not "undo" the increment we never made.
        // The fix only rolls back the occupant INSERT; it must not also call
        // atomicDecrementOccupancy (which would corrupt the winner's count).
        when(placeRepository.findById(PLACE_ID)).thenReturn(Optional.of(fullPlace));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(accessRelationRepository.findBySubjectAndResourceType("user", USER_ID, "org_unit"))
                .thenReturn(Collections.emptyList());
        when(occupantRepository.save(any(UniversalPlaceOccupant.class)))
                .thenAnswer(inv -> {
                    UniversalPlaceOccupant o = inv.getArgument(0);
                    o.setId(SAVED_OCCUPANT_ID);
                    return o;
                });
        when(placeRepository.atomicIncrementOccupancy(PLACE_ID)).thenReturn(false);

        UniversalPlaceApplicationService.CheckInCommand cmd =
                new UniversalPlaceApplicationService.CheckInCommand();
        cmd.setOccupantType("user");
        cmd.setOccupantId(USER_ID);

        assertThatThrownBy(() -> service.checkIn(PLACE_ID, cmd))
                .isInstanceOf(IllegalStateException.class);

        verify(placeRepository, never()).atomicDecrementOccupancy(anyLong());
    }
}
