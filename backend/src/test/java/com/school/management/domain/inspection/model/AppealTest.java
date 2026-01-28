package com.school.management.domain.inspection.model;

import com.school.management.domain.inspection.event.AppealStatusChangedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for the Appeal aggregate root.
 *
 * Covers: creation with valid/invalid data, full approval flow
 * (PENDING -> LEVEL1_REVIEWING -> LEVEL1_APPROVED -> LEVEL2_REVIEWING -> APPROVED -> EFFECTIVE),
 * rejection flow, withdrawal, invalid state transitions,
 * score difference calculation, and domain event registration.
 */
@DisplayName("Appeal Aggregate Root")
class AppealTest {

    private Appeal appeal;
    private static final Long INSPECTION_RECORD_ID = 1L;
    private static final Long DEDUCTION_DETAIL_ID = 2L;
    private static final Long CLASS_ID = 3L;
    private static final Long APPLICANT_ID = 100L;
    private static final Long REVIEWER_1_ID = 200L;
    private static final Long REVIEWER_2_ID = 300L;

    @BeforeEach
    void setUp() {
        appeal = createValidAppeal();
    }

    private Appeal createValidAppeal() {
        return Appeal.create(
                INSPECTION_RECORD_ID,
                DEDUCTION_DETAIL_ID,
                CLASS_ID,
                "APP-20260103-001",
                "Deduction is incorrect, actual situation differs from record",
                Arrays.asList("evidence1.jpg", "evidence2.jpg"),
                new BigDecimal("10.00"),
                new BigDecimal("5.00"),
                APPLICANT_ID
        );
    }

    @Nested
    @DisplayName("Creation")
    class CreateAppealTest {

        @Test
        @DisplayName("should create appeal with valid data and PENDING status")
        void shouldCreateAppealSuccessfully() {
            // then
            assertNotNull(appeal);
            assertEquals(INSPECTION_RECORD_ID, appeal.getInspectionRecordId());
            assertEquals(DEDUCTION_DETAIL_ID, appeal.getDeductionDetailId());
            assertEquals(CLASS_ID, appeal.getClassId());
            assertEquals("APP-20260103-001", appeal.getAppealCode());
            assertEquals(APPLICANT_ID, appeal.getApplicantId());
            assertEquals(AppealStatus.PENDING, appeal.getStatus());
            assertNotNull(appeal.getAppliedAt());
            assertEquals(new BigDecimal("10.00"), appeal.getOriginalDeduction());
            assertEquals(new BigDecimal("5.00"), appeal.getRequestedDeduction());
            assertNull(appeal.getApprovedDeduction());
        }

        @Test
        @DisplayName("should preserve attachments list")
        void shouldPreserveAttachments() {
            // then
            assertEquals(2, appeal.getAttachments().size());
            assertTrue(appeal.getAttachments().contains("evidence1.jpg"));
            assertTrue(appeal.getAttachments().contains("evidence2.jpg"));
        }

        @Test
        @DisplayName("should return unmodifiable attachments list")
        void shouldReturnUnmodifiableAttachments() {
            assertThrows(UnsupportedOperationException.class, () ->
                appeal.getAttachments().add("extra.jpg")
            );
        }

        @Test
        @DisplayName("should create appeal with null attachments as empty list")
        void shouldHandleNullAttachments() {
            // when
            Appeal a = Appeal.create(
                INSPECTION_RECORD_ID, DEDUCTION_DETAIL_ID, CLASS_ID,
                "APP-002", "Reason", null,
                new BigDecimal("10"), new BigDecimal("5"), APPLICANT_ID
            );

            // then
            assertNotNull(a.getAttachments());
            assertTrue(a.getAttachments().isEmpty());
        }

        @Test
        @DisplayName("should create appeal with empty attachments list")
        void shouldHandleEmptyAttachments() {
            // when
            Appeal a = Appeal.create(
                INSPECTION_RECORD_ID, DEDUCTION_DETAIL_ID, CLASS_ID,
                "APP-002", "Reason", Collections.emptyList(),
                new BigDecimal("10"), new BigDecimal("5"), APPLICANT_ID
            );

            // then
            assertTrue(a.getAttachments().isEmpty());
        }

        @Test
        @DisplayName("should initialize with empty approval records")
        void shouldHaveEmptyApprovalRecords() {
            assertTrue(appeal.getApprovalRecords().isEmpty());
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when inspection record ID is null")
        void shouldFailWhenInspectionRecordIdIsNull() {
            assertThrows(IllegalArgumentException.class, () ->
                Appeal.create(null, DEDUCTION_DETAIL_ID, CLASS_ID,
                    "APP-001", "Reason", null,
                    new BigDecimal("10"), new BigDecimal("5"), APPLICANT_ID)
            );
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when reason is empty")
        void shouldFailWhenReasonIsEmpty() {
            assertThrows(IllegalArgumentException.class, () ->
                Appeal.create(INSPECTION_RECORD_ID, DEDUCTION_DETAIL_ID, CLASS_ID,
                    "APP-001", "", null,
                    new BigDecimal("10"), new BigDecimal("5"), APPLICANT_ID)
            );
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when reason is blank")
        void shouldFailWhenReasonIsBlank() {
            assertThrows(IllegalArgumentException.class, () ->
                Appeal.create(INSPECTION_RECORD_ID, DEDUCTION_DETAIL_ID, CLASS_ID,
                    "APP-001", "   ", null,
                    new BigDecimal("10"), new BigDecimal("5"), APPLICANT_ID)
            );
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when reason is null")
        void shouldFailWhenReasonIsNull() {
            assertThrows(IllegalArgumentException.class, () ->
                Appeal.create(INSPECTION_RECORD_ID, DEDUCTION_DETAIL_ID, CLASS_ID,
                    "APP-001", null, null,
                    new BigDecimal("10"), new BigDecimal("5"), APPLICANT_ID)
            );
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when original deduction is null")
        void shouldFailWhenOriginalDeductionIsNull() {
            assertThrows(IllegalArgumentException.class, () ->
                Appeal.create(INSPECTION_RECORD_ID, DEDUCTION_DETAIL_ID, CLASS_ID,
                    "APP-001", "Reason", null,
                    null, new BigDecimal("5"), APPLICANT_ID)
            );
        }
    }

    @Nested
    @DisplayName("Level 1 Review")
    class Level1ReviewTest {

        @Test
        @DisplayName("should transition from PENDING to LEVEL1_REVIEWING")
        void shouldStartLevel1Review() {
            // when
            appeal.startLevel1Review(REVIEWER_1_ID);

            // then
            assertEquals(AppealStatus.LEVEL1_REVIEWING, appeal.getStatus());
            assertEquals(REVIEWER_1_ID, appeal.getLevel1ReviewerId());
            assertEquals(1, appeal.getApprovalRecords().size());
        }

        @Test
        @DisplayName("should register domain event on level 1 review start")
        void shouldRegisterEventOnLevel1Start() {
            // when
            appeal.startLevel1Review(REVIEWER_1_ID);

            // then
            assertFalse(appeal.getDomainEvents().isEmpty());
            assertInstanceOf(AppealStatusChangedEvent.class, appeal.getDomainEvents().get(0));
        }

        @Test
        @DisplayName("should transition from LEVEL1_REVIEWING to LEVEL1_APPROVED")
        void shouldLevel1Approve() {
            // given
            appeal.startLevel1Review(REVIEWER_1_ID);

            // when
            appeal.level1Approve(REVIEWER_1_ID, "Situation confirmed, approved");

            // then
            assertEquals(AppealStatus.LEVEL1_APPROVED, appeal.getStatus());
            assertEquals("Situation confirmed, approved", appeal.getLevel1Comment());
            assertNotNull(appeal.getLevel1ReviewedAt());
            assertEquals(2, appeal.getApprovalRecords().size());
        }

        @Test
        @DisplayName("should transition from LEVEL1_REVIEWING to LEVEL1_REJECTED")
        void shouldLevel1Reject() {
            // given
            appeal.startLevel1Review(REVIEWER_1_ID);

            // when
            appeal.level1Reject(REVIEWER_1_ID, "Insufficient evidence");

            // then
            assertEquals(AppealStatus.LEVEL1_REJECTED, appeal.getStatus());
            assertEquals("Insufficient evidence", appeal.getLevel1Comment());
            assertNotNull(appeal.getLevel1ReviewedAt());
        }

        @Test
        @DisplayName("should throw when attempting LEVEL1_APPROVED directly from PENDING")
        void shouldFailDirectTransitionToLevel1Approved() {
            // when/then
            IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                appeal.level1Approve(REVIEWER_1_ID, "comment")
            );
            assertTrue(ex.getMessage().contains("Cannot transition"));
        }

        @Test
        @DisplayName("should throw when attempting LEVEL1_REJECTED directly from PENDING")
        void shouldFailDirectTransitionToLevel1Rejected() {
            assertThrows(IllegalStateException.class, () ->
                appeal.level1Reject(REVIEWER_1_ID, "comment")
            );
        }
    }

    @Nested
    @DisplayName("Level 2 Review")
    class Level2ReviewTest {

        @BeforeEach
        void passLevel1() {
            appeal.startLevel1Review(REVIEWER_1_ID);
            appeal.level1Approve(REVIEWER_1_ID, "Approved");
        }

        @Test
        @DisplayName("should transition from LEVEL1_APPROVED to LEVEL2_REVIEWING")
        void shouldStartLevel2Review() {
            // when
            appeal.startLevel2Review(REVIEWER_2_ID);

            // then
            assertEquals(AppealStatus.LEVEL2_REVIEWING, appeal.getStatus());
            assertEquals(REVIEWER_2_ID, appeal.getLevel2ReviewerId());
        }

        @Test
        @DisplayName("should transition from LEVEL2_REVIEWING to APPROVED with approved deduction")
        void shouldFinalApprove() {
            // given
            appeal.startLevel2Review(REVIEWER_2_ID);
            BigDecimal approvedDeduction = new BigDecimal("3.00");

            // when
            appeal.approve(REVIEWER_2_ID, "Final approval", approvedDeduction);

            // then
            assertEquals(AppealStatus.APPROVED, appeal.getStatus());
            assertEquals(approvedDeduction, appeal.getApprovedDeduction());
            assertEquals("Final approval", appeal.getLevel2Comment());
            assertNotNull(appeal.getLevel2ReviewedAt());
        }

        @Test
        @DisplayName("should transition from LEVEL2_REVIEWING to REJECTED")
        void shouldFinalReject() {
            // given
            appeal.startLevel2Review(REVIEWER_2_ID);

            // when
            appeal.reject(REVIEWER_2_ID, "Original check result correct");

            // then
            assertEquals(AppealStatus.REJECTED, appeal.getStatus());
            assertEquals("Original check result correct", appeal.getLevel2Comment());
        }

        @Test
        @DisplayName("should throw when attempting final approval directly from LEVEL1_APPROVED")
        void shouldFailDirectApprovalFromLevel1Approved() {
            // Level 1 approved but level 2 review not started
            assertThrows(IllegalStateException.class, () ->
                appeal.approve(REVIEWER_2_ID, "comment", new BigDecimal("3.00"))
            );
        }

        @Test
        @DisplayName("should throw when attempting final rejection directly from LEVEL1_APPROVED")
        void shouldFailDirectRejectionFromLevel1Approved() {
            assertThrows(IllegalStateException.class, () ->
                appeal.reject(REVIEWER_2_ID, "comment")
            );
        }
    }

    @Nested
    @DisplayName("Withdrawal")
    class WithdrawTest {

        @Test
        @DisplayName("should allow applicant to withdraw a PENDING appeal")
        void shouldWithdrawPendingAppeal() {
            // when
            appeal.withdraw(APPLICANT_ID);

            // then
            assertEquals(AppealStatus.WITHDRAWN, appeal.getStatus());
            assertEquals(1, appeal.getApprovalRecords().size());
        }

        @Test
        @DisplayName("should register domain event on withdrawal")
        void shouldRegisterEventOnWithdraw() {
            // when
            appeal.withdraw(APPLICANT_ID);

            // then
            assertFalse(appeal.getDomainEvents().isEmpty());
            assertInstanceOf(AppealStatusChangedEvent.class, appeal.getDomainEvents().get(0));
        }

        @Test
        @DisplayName("should throw when non-applicant attempts withdrawal")
        void shouldFailWhenNonApplicantWithdraws() {
            // when/then
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                appeal.withdraw(999L)
            );
            assertTrue(ex.getMessage().contains("applicant"));
        }

        @Test
        @DisplayName("should throw when withdrawing from LEVEL1_REVIEWING state")
        void shouldFailWhenWithdrawingFromLevel1Reviewing() {
            // given
            appeal.startLevel1Review(REVIEWER_1_ID);

            // when/then
            assertThrows(IllegalStateException.class, () ->
                appeal.withdraw(APPLICANT_ID)
            );
        }

        @Test
        @DisplayName("should throw when withdrawing from APPROVED state")
        void shouldFailWhenWithdrawingFromApproved() {
            // given
            appeal.startLevel1Review(REVIEWER_1_ID);
            appeal.level1Approve(REVIEWER_1_ID, "ok");
            appeal.startLevel2Review(REVIEWER_2_ID);
            appeal.approve(REVIEWER_2_ID, "ok", new BigDecimal("3.00"));

            // when/then
            assertThrows(IllegalStateException.class, () ->
                appeal.withdraw(APPLICANT_ID)
            );
        }

        @Test
        @DisplayName("should throw when withdrawing from WITHDRAWN state")
        void shouldFailWhenAlreadyWithdrawn() {
            // given
            appeal.withdraw(APPLICANT_ID);

            // when/then
            assertThrows(IllegalStateException.class, () ->
                appeal.withdraw(APPLICANT_ID)
            );
        }
    }

    @Nested
    @DisplayName("Make Effective")
    class MakeEffectiveTest {

        @BeforeEach
        void approveAppeal() {
            appeal.startLevel1Review(REVIEWER_1_ID);
            appeal.level1Approve(REVIEWER_1_ID, "Approved");
            appeal.startLevel2Review(REVIEWER_2_ID);
            appeal.approve(REVIEWER_2_ID, "Final approval", new BigDecimal("3.00"));
        }

        @Test
        @DisplayName("should transition from APPROVED to EFFECTIVE")
        void shouldMakeEffective() {
            // when
            appeal.makeEffective();

            // then
            assertEquals(AppealStatus.EFFECTIVE, appeal.getStatus());
            assertNotNull(appeal.getEffectiveAt());
        }

        @Test
        @DisplayName("should add approval record for effective transition")
        void shouldAddApprovalRecordForEffective() {
            // given
            int recordsBefore = appeal.getApprovalRecords().size();

            // when
            appeal.makeEffective();

            // then
            assertEquals(recordsBefore + 1, appeal.getApprovalRecords().size());
        }

        @Test
        @DisplayName("should throw when making PENDING appeal effective")
        void shouldFailWhenMakingPendingEffective() {
            // given
            Appeal freshAppeal = createValidAppeal();

            // when/then
            assertThrows(IllegalStateException.class, freshAppeal::makeEffective);
        }

        @Test
        @DisplayName("should throw when making REJECTED appeal effective")
        void shouldFailWhenMakingRejectedEffective() {
            // given
            Appeal rejectedAppeal = createValidAppeal();
            rejectedAppeal.startLevel1Review(REVIEWER_1_ID);
            rejectedAppeal.level1Approve(REVIEWER_1_ID, "ok");
            rejectedAppeal.startLevel2Review(REVIEWER_2_ID);
            rejectedAppeal.reject(REVIEWER_2_ID, "rejected");

            // when/then
            assertThrows(IllegalStateException.class, rejectedAppeal::makeEffective);
        }
    }

    @Nested
    @DisplayName("Score Difference Calculation")
    class ScoreDifferenceTest {

        @Test
        @DisplayName("should calculate difference using approved deduction")
        void shouldCalculateScoreDifferenceWithApprovedDeduction() {
            // given
            appeal.startLevel1Review(REVIEWER_1_ID);
            appeal.level1Approve(REVIEWER_1_ID, "ok");
            appeal.startLevel2Review(REVIEWER_2_ID);
            appeal.approve(REVIEWER_2_ID, "ok", new BigDecimal("3.00"));

            // when - originalDeduction=10.00, approvedDeduction=3.00
            BigDecimal difference = appeal.calculateScoreDifference();

            // then
            assertEquals(new BigDecimal("7.00"), difference);
        }

        @Test
        @DisplayName("should fallback to requested deduction when approved deduction is null")
        void shouldUseRequestedDeductionWhenApprovedIsNull() {
            // No approval, so approvedDeduction is null.
            // Should use requestedDeduction=5.00 instead.

            // when - originalDeduction=10.00, requestedDeduction=5.00
            BigDecimal difference = appeal.calculateScoreDifference();

            // then
            assertEquals(new BigDecimal("5.00"), difference);
        }

        @Test
        @DisplayName("should return zero difference when approved deduction equals original")
        void shouldReturnZeroDifference() {
            // given
            appeal.startLevel1Review(REVIEWER_1_ID);
            appeal.level1Approve(REVIEWER_1_ID, "ok");
            appeal.startLevel2Review(REVIEWER_2_ID);
            appeal.approve(REVIEWER_2_ID, "ok", new BigDecimal("10.00"));

            // when - original=10, approved=10
            BigDecimal difference = appeal.calculateScoreDifference();

            // then
            assertEquals(new BigDecimal("0.00"), difference);
        }
    }

    @Nested
    @DisplayName("Invalid State Transitions")
    class InvalidTransitionTest {

        @Test
        @DisplayName("cannot transition from PENDING directly to APPROVED")
        void shouldFailPendingToApproved() {
            assertThrows(IllegalStateException.class, () ->
                appeal.approve(REVIEWER_2_ID, "comment", new BigDecimal("3.00"))
            );
        }

        @Test
        @DisplayName("cannot transition from PENDING directly to REJECTED")
        void shouldFailPendingToRejected() {
            assertThrows(IllegalStateException.class, () ->
                appeal.reject(REVIEWER_2_ID, "comment")
            );
        }

        @Test
        @DisplayName("cannot transition from PENDING directly to EFFECTIVE")
        void shouldFailPendingToEffective() {
            assertThrows(IllegalStateException.class, appeal::makeEffective);
        }

        @Test
        @DisplayName("cannot transition from PENDING directly to LEVEL2_REVIEWING")
        void shouldFailPendingToLevel2Reviewing() {
            assertThrows(IllegalStateException.class, () ->
                appeal.startLevel2Review(REVIEWER_2_ID)
            );
        }

        @Test
        @DisplayName("cannot start level 1 review from LEVEL1_REVIEWING")
        void shouldFailLevel1ReviewingToLevel1Reviewing() {
            // given
            appeal.startLevel1Review(REVIEWER_1_ID);

            // when/then
            assertThrows(IllegalStateException.class, () ->
                appeal.startLevel1Review(REVIEWER_1_ID)
            );
        }

        @Test
        @DisplayName("cannot proceed from LEVEL1_REJECTED (terminal state)")
        void shouldFailFromLevel1RejectedTerminalState() {
            // given
            appeal.startLevel1Review(REVIEWER_1_ID);
            appeal.level1Reject(REVIEWER_1_ID, "rejected");

            // when/then
            assertThrows(IllegalStateException.class, () ->
                appeal.startLevel2Review(REVIEWER_2_ID)
            );
            assertThrows(IllegalStateException.class, () ->
                appeal.approve(REVIEWER_2_ID, "comment", new BigDecimal("3"))
            );
            assertThrows(IllegalStateException.class, appeal::makeEffective);
        }

        @Test
        @DisplayName("cannot proceed from WITHDRAWN (terminal state)")
        void shouldFailFromWithdrawnTerminalState() {
            // given
            appeal.withdraw(APPLICANT_ID);

            // when/then
            assertThrows(IllegalStateException.class, () ->
                appeal.startLevel1Review(REVIEWER_1_ID)
            );
        }

        @Test
        @DisplayName("cannot proceed from EFFECTIVE (terminal state)")
        void shouldFailFromEffectiveTerminalState() {
            // given
            appeal.startLevel1Review(REVIEWER_1_ID);
            appeal.level1Approve(REVIEWER_1_ID, "ok");
            appeal.startLevel2Review(REVIEWER_2_ID);
            appeal.approve(REVIEWER_2_ID, "ok", new BigDecimal("3"));
            appeal.makeEffective();

            // when/then
            assertThrows(IllegalStateException.class, () ->
                appeal.startLevel1Review(REVIEWER_1_ID)
            );
        }

        @Test
        @DisplayName("cannot proceed from REJECTED (terminal state)")
        void shouldFailFromRejectedTerminalState() {
            // given
            appeal.startLevel1Review(REVIEWER_1_ID);
            appeal.level1Approve(REVIEWER_1_ID, "ok");
            appeal.startLevel2Review(REVIEWER_2_ID);
            appeal.reject(REVIEWER_2_ID, "rejected");

            // when/then
            assertThrows(IllegalStateException.class, appeal::makeEffective);
            assertThrows(IllegalStateException.class, () ->
                appeal.approve(REVIEWER_2_ID, "x", new BigDecimal("1"))
            );
        }
    }

    @Nested
    @DisplayName("Full Approval Workflow")
    class FullWorkflowTest {

        @Test
        @DisplayName("should complete full approval flow: PENDING -> ... -> EFFECTIVE")
        void shouldCompleteFullApprovalWorkflow() {
            // given - initial PENDING state
            assertEquals(AppealStatus.PENDING, appeal.getStatus());

            // when - level 1 review
            appeal.startLevel1Review(REVIEWER_1_ID);
            assertEquals(AppealStatus.LEVEL1_REVIEWING, appeal.getStatus());

            appeal.level1Approve(REVIEWER_1_ID, "Initial review passed");
            assertEquals(AppealStatus.LEVEL1_APPROVED, appeal.getStatus());

            // when - level 2 review
            appeal.startLevel2Review(REVIEWER_2_ID);
            assertEquals(AppealStatus.LEVEL2_REVIEWING, appeal.getStatus());

            appeal.approve(REVIEWER_2_ID, "Final approval", new BigDecimal("2.00"));
            assertEquals(AppealStatus.APPROVED, appeal.getStatus());

            // when - make effective
            appeal.makeEffective();
            assertEquals(AppealStatus.EFFECTIVE, appeal.getStatus());

            // then - verify all approval records accumulated
            assertEquals(5, appeal.getApprovalRecords().size());
            assertNotNull(appeal.getEffectiveAt());
        }

        @Test
        @DisplayName("should terminate flow on level 1 rejection")
        void shouldTerminateOnLevel1Rejection() {
            // when
            appeal.startLevel1Review(REVIEWER_1_ID);
            appeal.level1Reject(REVIEWER_1_ID, "Rejected");

            // then
            assertEquals(AppealStatus.LEVEL1_REJECTED, appeal.getStatus());
            assertTrue(appeal.getStatus().isTerminal());

            // cannot continue
            assertThrows(IllegalStateException.class, () ->
                appeal.startLevel2Review(REVIEWER_2_ID)
            );
        }

        @Test
        @DisplayName("should terminate flow on level 2 rejection")
        void shouldTerminateOnLevel2Rejection() {
            // given
            appeal.startLevel1Review(REVIEWER_1_ID);
            appeal.level1Approve(REVIEWER_1_ID, "ok");
            appeal.startLevel2Review(REVIEWER_2_ID);

            // when
            appeal.reject(REVIEWER_2_ID, "Rejected on final review");

            // then
            assertEquals(AppealStatus.REJECTED, appeal.getStatus());
            assertTrue(appeal.getStatus().isTerminal());

            // cannot make effective
            assertThrows(IllegalStateException.class, appeal::makeEffective);
        }
    }

    @Nested
    @DisplayName("AppealStatus State Machine")
    class AppealStatusTest {

        @Test
        @DisplayName("PENDING should allow transitions to LEVEL1_REVIEWING and WITHDRAWN")
        void pendingAllowedTransitions() {
            assertTrue(AppealStatus.PENDING.canTransitionTo(AppealStatus.LEVEL1_REVIEWING));
            assertTrue(AppealStatus.PENDING.canTransitionTo(AppealStatus.WITHDRAWN));
            assertFalse(AppealStatus.PENDING.canTransitionTo(AppealStatus.APPROVED));
            assertFalse(AppealStatus.PENDING.canTransitionTo(AppealStatus.EFFECTIVE));
        }

        @Test
        @DisplayName("LEVEL1_REVIEWING should allow transitions to LEVEL1_APPROVED and LEVEL1_REJECTED")
        void level1ReviewingAllowedTransitions() {
            assertTrue(AppealStatus.LEVEL1_REVIEWING.canTransitionTo(AppealStatus.LEVEL1_APPROVED));
            assertTrue(AppealStatus.LEVEL1_REVIEWING.canTransitionTo(AppealStatus.LEVEL1_REJECTED));
            assertFalse(AppealStatus.LEVEL1_REVIEWING.canTransitionTo(AppealStatus.APPROVED));
        }

        @Test
        @DisplayName("LEVEL1_APPROVED should only allow transition to LEVEL2_REVIEWING")
        void level1ApprovedAllowedTransitions() {
            assertTrue(AppealStatus.LEVEL1_APPROVED.canTransitionTo(AppealStatus.LEVEL2_REVIEWING));
            assertFalse(AppealStatus.LEVEL1_APPROVED.canTransitionTo(AppealStatus.APPROVED));
        }

        @Test
        @DisplayName("terminal states should have no allowed transitions")
        void terminalStatesHaveNoTransitions() {
            assertTrue(AppealStatus.LEVEL1_REJECTED.isTerminal());
            assertTrue(AppealStatus.REJECTED.isTerminal());
            assertTrue(AppealStatus.WITHDRAWN.isTerminal());
            assertTrue(AppealStatus.EFFECTIVE.isTerminal());

            assertTrue(AppealStatus.LEVEL1_REJECTED.getAllowedTransitions().isEmpty());
            assertTrue(AppealStatus.REJECTED.getAllowedTransitions().isEmpty());
            assertTrue(AppealStatus.WITHDRAWN.getAllowedTransitions().isEmpty());
            assertTrue(AppealStatus.EFFECTIVE.getAllowedTransitions().isEmpty());
        }

        @Test
        @DisplayName("reviewing states should require reviewer action")
        void reviewingStatesRequireAction() {
            assertTrue(AppealStatus.LEVEL1_REVIEWING.requiresReviewerAction());
            assertTrue(AppealStatus.LEVEL2_REVIEWING.requiresReviewerAction());
            assertFalse(AppealStatus.PENDING.requiresReviewerAction());
            assertFalse(AppealStatus.APPROVED.requiresReviewerAction());
        }

        @Test
        @DisplayName("isSuccessful should return true for APPROVED and EFFECTIVE")
        void successfulStates() {
            assertTrue(AppealStatus.APPROVED.isSuccessful());
            assertTrue(AppealStatus.EFFECTIVE.isSuccessful());
            assertFalse(AppealStatus.PENDING.isSuccessful());
            assertFalse(AppealStatus.REJECTED.isSuccessful());
            assertFalse(AppealStatus.LEVEL1_REJECTED.isSuccessful());
        }
    }

    @Nested
    @DisplayName("Domain Events")
    class DomainEventTest {

        @Test
        @DisplayName("should register AppealStatusChangedEvent on each state change")
        void shouldRegisterEventOnStatusChange() {
            // when
            appeal.startLevel1Review(REVIEWER_1_ID);

            // then
            assertFalse(appeal.getDomainEvents().isEmpty());
            assertInstanceOf(AppealStatusChangedEvent.class, appeal.getDomainEvents().get(0));
        }

        @Test
        @DisplayName("should accumulate events for multiple transitions")
        void shouldAccumulateEvents() {
            // when
            appeal.startLevel1Review(REVIEWER_1_ID);
            appeal.level1Approve(REVIEWER_1_ID, "ok");

            // then
            assertEquals(2, appeal.getDomainEvents().size());
        }

        @Test
        @DisplayName("should clear events when clearDomainEvents is called")
        void shouldClearEvents() {
            // given
            appeal.startLevel1Review(REVIEWER_1_ID);
            assertFalse(appeal.getDomainEvents().isEmpty());

            // when
            appeal.clearDomainEvents();

            // then
            assertTrue(appeal.getDomainEvents().isEmpty());
        }

        @Test
        @DisplayName("should return unmodifiable events list")
        void shouldReturnUnmodifiableEventsList() {
            assertThrows(UnsupportedOperationException.class, () ->
                appeal.getDomainEvents().clear()
            );
        }
    }
}
