package com.school.management.domain.inspection.model;

import com.school.management.domain.inspection.event.SessionPublishedEvent;
import com.school.management.domain.inspection.event.SessionSubmittedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for the InspectionSession aggregate root.
 *
 * Covers: creation with valid/invalid data, lifecycle state transitions
 * (CREATED -> IN_PROGRESS -> SUBMITTED -> PUBLISHED),
 * invalid transition guards, domain event registration on submit/publish,
 * and builder construction with defaults.
 */
@DisplayName("InspectionSession Aggregate Root")
class InspectionSessionTest {

    private static final Long TEMPLATE_ID = 10L;
    private static final Integer TEMPLATE_VERSION = 2;
    private static final Long INSPECTOR_ID = 100L;
    private static final String INSPECTOR_NAME = "Inspector Zhang";
    private static final Long CREATOR_ID = 1L;
    private static final LocalDate INSPECTION_DATE = LocalDate.of(2026, 1, 15);

    private InspectionSession session;

    @BeforeEach
    void setUp() {
        session = createValidSession();
    }

    private InspectionSession createValidSession() {
        return InspectionSession.create(
            "SES-20260115-001",
            TEMPLATE_ID,
            TEMPLATE_VERSION,
            INSPECTION_DATE,
            "morning",
            InputMode.SPACE_FIRST,
            ScoringMode.DEDUCTION_ONLY,
            100,
            InspectionLevel.CLASS,
            INSPECTOR_ID,
            INSPECTOR_NAME,
            CREATOR_ID
        );
    }

    @Nested
    @DisplayName("Creation")
    class CreateSessionTest {

        @Test
        @DisplayName("should create session with valid data and CREATED status")
        void shouldCreateSessionSuccessfully() {
            // then
            assertNotNull(session);
            assertEquals("SES-20260115-001", session.getSessionCode());
            assertEquals(TEMPLATE_ID, session.getTemplateId());
            assertEquals(TEMPLATE_VERSION, session.getTemplateVersion());
            assertEquals(INSPECTION_DATE, session.getInspectionDate());
            assertEquals("morning", session.getInspectionPeriod());
            assertEquals(InputMode.SPACE_FIRST, session.getInputMode());
            assertEquals(ScoringMode.DEDUCTION_ONLY, session.getScoringMode());
            assertEquals(100, session.getBaseScore());
            assertEquals(InspectionLevel.CLASS, session.getInspectionLevel());
            assertEquals(SessionStatus.CREATED, session.getStatus());
            assertEquals(INSPECTOR_ID, session.getInspectorId());
            assertEquals(INSPECTOR_NAME, session.getInspectorName());
            assertEquals(CREATOR_ID, session.getCreatedBy());
            assertNotNull(session.getCreatedAt());
            assertNull(session.getSubmittedAt());
            assertNull(session.getPublishedAt());
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when session code is null")
        void shouldFailWhenSessionCodeIsNull() {
            assertThrows(IllegalArgumentException.class, () ->
                InspectionSession.create(
                    null, TEMPLATE_ID, TEMPLATE_VERSION, INSPECTION_DATE,
                    "morning", InputMode.SPACE_FIRST, ScoringMode.DEDUCTION_ONLY,
                    100, InspectionLevel.CLASS, INSPECTOR_ID, INSPECTOR_NAME, CREATOR_ID
                )
            );
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when session code is blank")
        void shouldFailWhenSessionCodeIsBlank() {
            assertThrows(IllegalArgumentException.class, () ->
                InspectionSession.create(
                    "   ", TEMPLATE_ID, TEMPLATE_VERSION, INSPECTION_DATE,
                    "morning", InputMode.SPACE_FIRST, ScoringMode.DEDUCTION_ONLY,
                    100, InspectionLevel.CLASS, INSPECTOR_ID, INSPECTOR_NAME, CREATOR_ID
                )
            );
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when template ID is null")
        void shouldFailWhenTemplateIdIsNull() {
            assertThrows(IllegalArgumentException.class, () ->
                InspectionSession.create(
                    "SES-001", null, TEMPLATE_VERSION, INSPECTION_DATE,
                    "morning", InputMode.SPACE_FIRST, ScoringMode.DEDUCTION_ONLY,
                    100, InspectionLevel.CLASS, INSPECTOR_ID, INSPECTOR_NAME, CREATOR_ID
                )
            );
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when inspection date is null")
        void shouldFailWhenInspectionDateIsNull() {
            assertThrows(IllegalArgumentException.class, () ->
                InspectionSession.create(
                    "SES-001", TEMPLATE_ID, TEMPLATE_VERSION, null,
                    "morning", InputMode.SPACE_FIRST, ScoringMode.DEDUCTION_ONLY,
                    100, InspectionLevel.CLASS, INSPECTOR_ID, INSPECTOR_NAME, CREATOR_ID
                )
            );
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when inspector ID is null")
        void shouldFailWhenInspectorIdIsNull() {
            assertThrows(IllegalArgumentException.class, () ->
                InspectionSession.create(
                    "SES-001", TEMPLATE_ID, TEMPLATE_VERSION, INSPECTION_DATE,
                    "morning", InputMode.SPACE_FIRST, ScoringMode.DEDUCTION_ONLY,
                    100, InspectionLevel.CLASS, null, INSPECTOR_NAME, CREATOR_ID
                )
            );
        }
    }

    @Nested
    @DisplayName("Start Inspection (CREATED -> IN_PROGRESS)")
    class StartInspectionTest {

        @Test
        @DisplayName("should transition from CREATED to IN_PROGRESS")
        void shouldStartInspection() {
            // when
            session.startInspection();

            // then
            assertEquals(SessionStatus.IN_PROGRESS, session.getStatus());
        }

        @Test
        @DisplayName("should throw when starting from IN_PROGRESS state")
        void shouldFailWhenStartingFromInProgress() {
            // given
            session.startInspection();

            // when/then
            IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                session.startInspection()
            );
            assertTrue(ex.getMessage().contains("created"));
        }

        @Test
        @DisplayName("should throw when starting from SUBMITTED state")
        void shouldFailWhenStartingFromSubmitted() {
            // given
            session.submit(3);

            // when/then
            assertThrows(IllegalStateException.class, () ->
                session.startInspection()
            );
        }

        @Test
        @DisplayName("should throw when starting from PUBLISHED state")
        void shouldFailWhenStartingFromPublished() {
            // given
            session.submit(3);
            session.publish(3);

            // when/then
            assertThrows(IllegalStateException.class, () ->
                session.startInspection()
            );
        }
    }

    @Nested
    @DisplayName("Submit Session")
    class SubmitSessionTest {

        @Test
        @DisplayName("should transition from CREATED to SUBMITTED with class records")
        void shouldSubmitFromCreated() {
            // when
            session.submit(5);

            // then
            assertEquals(SessionStatus.SUBMITTED, session.getStatus());
            assertNotNull(session.getSubmittedAt());
        }

        @Test
        @DisplayName("should transition from IN_PROGRESS to SUBMITTED")
        void shouldSubmitFromInProgress() {
            // given
            session.startInspection();

            // when
            session.submit(3);

            // then
            assertEquals(SessionStatus.SUBMITTED, session.getStatus());
            assertNotNull(session.getSubmittedAt());
        }

        @Test
        @DisplayName("should register SessionSubmittedEvent on submit")
        void shouldRegisterSubmittedEvent() {
            // when
            session.setId(42L);
            session.submit(5);

            // then
            assertFalse(session.getDomainEvents().isEmpty());
            boolean hasEvent = session.getDomainEvents().stream()
                .anyMatch(e -> e instanceof SessionSubmittedEvent);
            assertTrue(hasEvent);
        }

        @Test
        @DisplayName("should throw when submitting without class records")
        void shouldFailWhenNoClassRecords() {
            // when/then
            IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                session.submit(0)
            );
            assertTrue(ex.getMessage().contains("without any class records"));
        }

        @Test
        @DisplayName("should throw when submitting from SUBMITTED state")
        void shouldFailWhenSubmittingFromSubmitted() {
            // given
            session.submit(3);

            // when/then
            assertThrows(IllegalStateException.class, () ->
                session.submit(3)
            );
        }

        @Test
        @DisplayName("should throw when submitting from PUBLISHED state")
        void shouldFailWhenSubmittingFromPublished() {
            // given
            session.submit(3);
            session.publish(3);

            // when/then
            assertThrows(IllegalStateException.class, () ->
                session.submit(3)
            );
        }
    }

    @Nested
    @DisplayName("Publish Session")
    class PublishSessionTest {

        @BeforeEach
        void submitSession() {
            session.setId(42L);
            session.submit(5);
            session.clearDomainEvents(); // clear submit events
        }

        @Test
        @DisplayName("should transition from SUBMITTED to PUBLISHED")
        void shouldPublish() {
            // when
            session.publish(5);

            // then
            assertEquals(SessionStatus.PUBLISHED, session.getStatus());
            assertNotNull(session.getPublishedAt());
        }

        @Test
        @DisplayName("should register SessionPublishedEvent on publish")
        void shouldRegisterPublishedEvent() {
            // when
            session.publish(5);

            // then
            assertFalse(session.getDomainEvents().isEmpty());
            boolean hasEvent = session.getDomainEvents().stream()
                .anyMatch(e -> e instanceof SessionPublishedEvent);
            assertTrue(hasEvent);
        }

        @Test
        @DisplayName("should throw when publishing from CREATED state")
        void shouldFailWhenPublishingFromCreated() {
            // given
            InspectionSession freshSession = createValidSession();

            // when/then
            assertThrows(IllegalStateException.class, () ->
                freshSession.publish(3)
            );
        }

        @Test
        @DisplayName("should throw when publishing from IN_PROGRESS state")
        void shouldFailWhenPublishingFromInProgress() {
            // given
            InspectionSession inProgressSession = createValidSession();
            inProgressSession.startInspection();

            // when/then
            assertThrows(IllegalStateException.class, () ->
                inProgressSession.publish(3)
            );
        }

        @Test
        @DisplayName("should throw when publishing from PUBLISHED state (double publish)")
        void shouldFailWhenAlreadyPublished() {
            // given
            session.publish(5);

            // when/then
            assertThrows(IllegalStateException.class, () ->
                session.publish(5)
            );
        }
    }

    @Nested
    @DisplayName("Full Lifecycle")
    class FullLifecycleTest {

        @Test
        @DisplayName("should complete full lifecycle: CREATED -> IN_PROGRESS -> SUBMITTED -> PUBLISHED")
        void shouldCompleteFullLifecycle() {
            // given
            session.setId(42L);
            assertEquals(SessionStatus.CREATED, session.getStatus());

            // when - start
            session.startInspection();
            assertEquals(SessionStatus.IN_PROGRESS, session.getStatus());

            // when - submit
            session.submit(10);
            assertEquals(SessionStatus.SUBMITTED, session.getStatus());
            assertNotNull(session.getSubmittedAt());

            // when - publish
            session.publish(10);
            assertEquals(SessionStatus.PUBLISHED, session.getStatus());
            assertNotNull(session.getPublishedAt());

            // then - domain events registered for submit and publish
            long submittedEvents = session.getDomainEvents().stream()
                .filter(e -> e instanceof SessionSubmittedEvent).count();
            long publishedEvents = session.getDomainEvents().stream()
                .filter(e -> e instanceof SessionPublishedEvent).count();
            assertEquals(1, submittedEvents);
            assertEquals(1, publishedEvents);
        }

        @Test
        @DisplayName("should allow direct submit from CREATED (skipping IN_PROGRESS)")
        void shouldAllowDirectSubmitFromCreated() {
            // given
            session.setId(42L);

            // when
            session.submit(5);

            // then
            assertEquals(SessionStatus.SUBMITTED, session.getStatus());
        }
    }

    @Nested
    @DisplayName("Builder")
    class BuilderTest {

        @Test
        @DisplayName("should build session with all fields via Builder")
        void shouldBuildSessionWithAllFields() {
            // when
            InspectionSession built = InspectionSession.builder()
                .id(1L)
                .sessionCode("SES-001")
                .templateId(TEMPLATE_ID)
                .templateVersion(TEMPLATE_VERSION)
                .inspectionDate(INSPECTION_DATE)
                .inspectionPeriod("afternoon")
                .inputMode(InputMode.PERSON_FIRST)
                .scoringMode(ScoringMode.DUAL_TRACK)
                .baseScore(150)
                .inspectionLevel(InspectionLevel.DEPARTMENT)
                .status(SessionStatus.SUBMITTED)
                .inspectorId(INSPECTOR_ID)
                .inspectorName(INSPECTOR_NAME)
                .createdBy(CREATOR_ID)
                .build();

            // then
            assertEquals(1L, built.getId());
            assertEquals("SES-001", built.getSessionCode());
            assertEquals(InputMode.PERSON_FIRST, built.getInputMode());
            assertEquals(ScoringMode.DUAL_TRACK, built.getScoringMode());
            assertEquals(150, built.getBaseScore());
            assertEquals(InspectionLevel.DEPARTMENT, built.getInspectionLevel());
            assertEquals(SessionStatus.SUBMITTED, built.getStatus());
        }

        @Test
        @DisplayName("should use defaults when optional builder fields are omitted")
        void shouldUseDefaultsForOptionalFields() {
            // when - builder with id set skips validation
            InspectionSession built = InspectionSession.builder()
                .id(1L)
                .sessionCode("SES-001")
                .templateId(TEMPLATE_ID)
                .inspectionDate(INSPECTION_DATE)
                .inspectorId(INSPECTOR_ID)
                .build();

            // then
            assertEquals(InputMode.SPACE_FIRST, built.getInputMode());
            assertEquals(ScoringMode.DEDUCTION_ONLY, built.getScoringMode());
            assertEquals(100, built.getBaseScore());
            assertEquals(InspectionLevel.CLASS, built.getInspectionLevel());
            assertEquals(SessionStatus.CREATED, built.getStatus());
        }

        @Test
        @DisplayName("builder with existing ID should skip validation (for reconstruction)")
        void shouldSkipValidationForExistingId() {
            // when - null sessionCode with id set should not throw
            InspectionSession built = InspectionSession.builder()
                .id(1L)
                .build();

            // then
            assertEquals(1L, built.getId());
            assertEquals(SessionStatus.CREATED, built.getStatus());
        }
    }

    @Nested
    @DisplayName("Domain Events")
    class DomainEventTest {

        @Test
        @DisplayName("should not have events immediately after creation via factory")
        void shouldNotHaveEventsAfterCreation() {
            // Factory method does not register events (unlike InspectionTemplate)
            assertTrue(session.getDomainEvents().isEmpty());
        }

        @Test
        @DisplayName("should clear domain events")
        void shouldClearEvents() {
            // given
            session.setId(1L);
            session.submit(5);
            assertFalse(session.getDomainEvents().isEmpty());

            // when
            session.clearDomainEvents();

            // then
            assertTrue(session.getDomainEvents().isEmpty());
        }
    }
}
