package com.school.management.application.place;

import com.school.management.domain.access.repository.AccessRelationRepository;
import com.school.management.domain.organization.repository.OrgUnitRepository;
import com.school.management.domain.place.model.aggregate.UniversalPlace;
import com.school.management.domain.place.model.entity.UniversalPlaceOccupant;
import com.school.management.domain.place.model.valueobject.PlaceStatus;
import com.school.management.domain.place.repository.UniversalPlaceOccupantRepository;
import com.school.management.domain.place.repository.UniversalPlaceRepository;
import com.school.management.domain.place.service.PlaceInheritanceService;
import com.school.management.domain.shared.repository.EntityTypeConfigRepository;
import com.school.management.domain.user.model.aggregate.User;
import com.school.management.domain.user.model.valueobject.UserStatus;
import com.school.management.domain.user.repository.UserRepository;
import com.school.management.infrastructure.activity.ActivityEventBuilder;
import com.school.management.infrastructure.activity.ActivityEventPublisher;
import com.school.management.infrastructure.extension.PolicyContext;
import com.school.management.infrastructure.extension.PolicyRegistry;
import com.school.management.infrastructure.extension.PolicyViolationException;
import com.school.management.infrastructure.extension.Violation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * W1.4: 验证 UniversalPlaceApplicationService.checkIn 在变更边界调用 PolicyRegistry.
 *
 * <p>契约:
 * <ul>
 *   <li>BEFORE_CHECKIN: enforce() — BLOCK 违规抛 PolicyViolationException, 后续业务逻辑不执行</li>
 *   <li>AFTER_CHECKIN: check() — WARN 违规仅记录, 不抛异常, 不回滚</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class PlaceCheckInPolicyHookTest {

    @Mock UniversalPlaceRepository placeRepository;
    @Mock EntityTypeConfigRepository entityTypeConfigRepository;
    @Mock UniversalPlaceOccupantRepository occupantRepository;
    @Mock OrgUnitRepository orgUnitRepository;
    @Mock UserRepository userRepository;
    @Mock AccessRelationRepository accessRelationRepository;
    @Mock PlaceInheritanceService inheritanceService;
    @Mock ActivityEventPublisher activityEventPublisher;
    @Mock PolicyRegistry policyRegistry;

    @InjectMocks UniversalPlaceApplicationService service;

    private static final Long PLACE_ID = 200L;
    private static final Long USER_ID = 888L;

    private UniversalPlace place;
    private User user;

    @BeforeEach
    void setUp() {
        place = UniversalPlace.builder()
                .id(PLACE_ID)
                .placeCode("R202")
                .placeName("202宿舍")
                .typeCode("DORM_ROOM")
                .capacity(4)
                .currentOccupancy(1)
                .status(PlaceStatus.NORMAL)
                .build();

        user = User.reconstruct(
                USER_ID,
                "bob",
                "encodedpwd",
                "Bob",
                null, null, null,
                1,
                null, null,
                null,
                "STUDENT",
                UserStatus.ENABLED,
                null, null, null, null,
                false,
                Collections.emptyList(),
                null, null
        );
    }

    private UniversalPlaceApplicationService.CheckInCommand cmd() {
        UniversalPlaceApplicationService.CheckInCommand c =
                new UniversalPlaceApplicationService.CheckInCommand();
        c.setOccupantType("user");
        c.setOccupantId(USER_ID);
        return c;
    }

    @Test
    void checkIn_whenBeforePolicyBlocks_throwsAndDoesNotTouchBusinessLogic() {
        // Mock enforce() 抛 PolicyViolationException — 模拟 BLOCK 违规.
        PolicyViolationException blocked = new PolicyViolationException(
                List.of(Violation.block("CAP_FULL", "capacity exceeded")));
        when(policyRegistry.enforce(any(PolicyContext.class))).thenThrow(blocked);

        assertThatThrownBy(() -> service.checkIn(PLACE_ID, cmd()))
                .isInstanceOf(PolicyViolationException.class)
                .hasMessageContaining("CAP_FULL");

        // BLOCK 后 BEFORE 已抛出, 任何业务副作用都不应发生.
        verify(placeRepository, never()).findById(any());
        verify(userRepository, never()).findById(any());
        verify(occupantRepository, never()).save(any());
        verify(placeRepository, never()).atomicIncrementOccupancy(any());
        // AFTER 也不应被调用 (同一个 enforce Mock — 但 after 是 check, 这里验证 check 从未被调用).
        verify(policyRegistry, never()).check(any(PolicyContext.class));
    }

    @Test
    void checkIn_whenAfterPolicyWarns_completesSuccessfullyAndLogsWarnings() {
        // BEFORE: enforce 放行 (空列表).
        when(policyRegistry.enforce(any(PolicyContext.class))).thenReturn(Collections.emptyList());
        // AFTER: check 返回一条 WARN.
        when(policyRegistry.check(any(PolicyContext.class)))
                .thenReturn(List.of(Violation.warn("MIN_OCCUPANTS",
                        "place will be under-utilized below policy threshold")));

        // 业务依赖 stub:
        when(placeRepository.findById(PLACE_ID)).thenReturn(Optional.of(place));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(accessRelationRepository.findBySubjectAndResourceType("user", USER_ID, "org_unit"))
                .thenReturn(Collections.emptyList());
        when(occupantRepository.save(any(UniversalPlaceOccupant.class)))
                .thenAnswer(inv -> {
                    UniversalPlaceOccupant o = inv.getArgument(0);
                    o.setId(42L);
                    return o;
                });
        when(placeRepository.atomicIncrementOccupancy(PLACE_ID)).thenReturn(true);
        // activityEventPublisher.newEvent(...) 链式 builder, stub 让每一步返回 self, publish() 是 void.
        ActivityEventBuilder builder = org.mockito.Mockito.mock(ActivityEventBuilder.class,
                org.mockito.Answers.RETURNS_SELF);
        when(activityEventPublisher.newEvent(any(), any(), any(), any())).thenReturn(builder);

        // Act — 不应抛异常.
        service.checkIn(PLACE_ID, cmd());

        // 断言两阶段都被调用, 且 phase 参数正确.
        ArgumentCaptor<PolicyContext> beforeCtx = ArgumentCaptor.forClass(PolicyContext.class);
        verify(policyRegistry).enforce(beforeCtx.capture());
        assertThat(beforeCtx.getValue().entityType()).isEqualTo("place");
        assertThat(beforeCtx.getValue().phase()).isEqualTo("BEFORE_CHECKIN");

        ArgumentCaptor<PolicyContext> afterCtx = ArgumentCaptor.forClass(PolicyContext.class);
        verify(policyRegistry).check(afterCtx.capture());
        assertThat(afterCtx.getValue().entityType()).isEqualTo("place");
        assertThat(afterCtx.getValue().phase()).isEqualTo("AFTER_CHECKIN");

        // 业务逻辑完成: occupant 持久化 + 容量递增.
        verify(occupantRepository).save(any(UniversalPlaceOccupant.class));
        verify(placeRepository).atomicIncrementOccupancy(PLACE_ID);
    }
}
