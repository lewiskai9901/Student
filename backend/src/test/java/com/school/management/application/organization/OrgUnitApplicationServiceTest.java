package com.school.management.application.organization;

import com.school.management.application.organization.command.UpdateOrgUnitCommand;
import com.school.management.application.organization.query.OrgUnitDTO;
import com.school.management.domain.organization.model.OrgUnit;
import com.school.management.domain.organization.model.valueobject.OrgUnitStatus;
import com.school.management.domain.organization.repository.OrgUnitRepository;
import com.school.management.domain.organization.repository.OrgUnitTypeRepository;
import com.school.management.domain.organization.repository.PositionRepository;
import com.school.management.domain.organization.repository.UserPositionRepository;
import com.school.management.domain.organization.repository.SchoolClassRepository;
import com.school.management.domain.organization.service.OrgUnitDomainService;
import com.school.management.domain.access.repository.AccessRelationRepository;
import com.school.management.domain.shared.event.DomainEventPublisher;
import com.school.management.infrastructure.activity.ActivityEventBuilder;
import com.school.management.infrastructure.activity.ActivityEventPublisher;
import com.school.management.infrastructure.persistence.organization.SchoolClassMapper;
import com.school.management.infrastructure.persistence.place.UniversalPlaceOccupantMapper;
import com.school.management.infrastructure.persistence.user.UserDomainMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * OrgUnitApplicationService unit tests.
 * Tests query and mutation operations.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OrgUnitApplicationService tests")
class OrgUnitApplicationServiceTest {

    @Mock
    private OrgUnitRepository orgUnitRepository;

    @Mock
    private OrgUnitTypeRepository orgUnitTypeRepository;

    @Mock
    private OrgUnitDomainService orgUnitDomainService;

    @Mock
    private DomainEventPublisher eventPublisher;

    @Mock
    private AccessRelationRepository accessRelationRepository;

    @Mock
    private PositionRepository positionRepository;

    @Mock
    private UserPositionRepository userPositionRepository;

    @Mock
    private SchoolClassRepository schoolClassRepository;

    @Mock
    private SchoolClassMapper schoolClassMapper;

    @Mock
    private UserDomainMapper userDomainMapper;

    @Mock
    private ActivityEventPublisher activityEventPublisher;

    @Mock
    private UniversalPlaceOccupantMapper placeOccupantMapper;

    private OrgUnitApplicationService applicationService;

    private static final Long CREATOR_ID = 1L;
    private static final Long ORG_UNIT_ID = 100L;

    @BeforeEach
    void setUp() {
        applicationService = new OrgUnitApplicationService(orgUnitRepository, orgUnitTypeRepository, orgUnitDomainService, eventPublisher, activityEventPublisher, accessRelationRepository, positionRepository, userPositionRepository, schoolClassRepository, schoolClassMapper, userDomainMapper, placeOccupantMapper);

        // Mock ActivityEventPublisher to return a chainable builder
        ActivityEventBuilder mockBuilder = mock(ActivityEventBuilder.class);
        lenient().when(activityEventPublisher.newEvent(anyString(), anyString(), anyString())).thenReturn(mockBuilder);
        lenient().when(activityEventPublisher.newEvent(anyString(), anyString(), anyString(), anyString())).thenReturn(mockBuilder);
        lenient().when(mockBuilder.resourceId(any(Long.class))).thenReturn(mockBuilder);
        lenient().when(mockBuilder.resourceId(any(String.class))).thenReturn(mockBuilder);
        lenient().when(mockBuilder.resourceName(anyString())).thenReturn(mockBuilder);
        lenient().when(mockBuilder.changedFields(any())).thenReturn(mockBuilder);
        lenient().when(mockBuilder.reason(any())).thenReturn(mockBuilder);
    }

    @Nested
    @DisplayName("Get OrgUnit tests")
    class GetOrgUnitTest {

        @Test
        @DisplayName("Successfully get OrgUnit by ID")
        void shouldGetOrgUnitById() {
            OrgUnit orgUnit = createOrgUnit(ORG_UNIT_ID, "DEPT001", "IT Department", "DEPARTMENT", null);
            when(orgUnitRepository.findById(ORG_UNIT_ID)).thenReturn(Optional.of(orgUnit));

            OrgUnitDTO result = applicationService.getOrgUnit(ORG_UNIT_ID);

            assertNotNull(result);
            assertEquals(ORG_UNIT_ID, result.getId());
            assertEquals("DEPT001", result.getUnitCode());

            verify(orgUnitRepository).findById(ORG_UNIT_ID);
        }

        @Test
        @DisplayName("Throw exception when OrgUnit not found")
        void shouldThrowExceptionWhenNotFound() {
            when(orgUnitRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class, () ->
                    applicationService.getOrgUnit(999L)
            );
        }

        @Test
        @DisplayName("Get OrgUnits by type")
        void shouldGetOrgUnitsByType() {
            List<OrgUnit> departments = Arrays.asList(
                    createOrgUnit(1L, "DEPT001", "Dept 1", "DEPARTMENT", null),
                    createOrgUnit(2L, "DEPT002", "Dept 2", "DEPARTMENT", null)
            );
            when(orgUnitRepository.findByUnitType("DEPARTMENT")).thenReturn(departments);

            List<OrgUnitDTO> result = applicationService.getOrgUnitsByType("DEPARTMENT");

            assertEquals(2, result.size());
            verify(orgUnitRepository).findByUnitType("DEPARTMENT");
        }

        @Test
        @DisplayName("Get children of OrgUnit")
        void shouldGetChildren() {
            Long parentId = 10L;
            List<OrgUnit> children = Arrays.asList(
                    createOrgUnit(1L, "CHILD001", "Child 1", "DEPARTMENT", parentId),
                    createOrgUnit(2L, "CHILD002", "Child 2", "DEPARTMENT", parentId)
            );
            when(orgUnitRepository.findByParentId(parentId)).thenReturn(children);

            List<OrgUnitDTO> result = applicationService.getChildren(parentId);

            assertEquals(2, result.size());
            verify(orgUnitRepository).findByParentId(parentId);
        }
    }

    @Nested
    @DisplayName("Update OrgUnit tests")
    class UpdateOrgUnitTest {

        @Test
        @DisplayName("Successfully update OrgUnit")
        void shouldUpdateOrgUnit() {
            UpdateOrgUnitCommand command = UpdateOrgUnitCommand.builder()
                    .unitName("Updated Name")
                    .sortOrder(10)
                    .headcount(50)
                    .updatedBy(CREATOR_ID)
                    .build();

            OrgUnit existingOrgUnit = createOrgUnit(ORG_UNIT_ID, "DEPT001", "Old Name", "DEPARTMENT", null);
            when(orgUnitRepository.findById(ORG_UNIT_ID)).thenReturn(Optional.of(existingOrgUnit));
            when(orgUnitRepository.save(any(OrgUnit.class))).thenAnswer(invocation -> invocation.getArgument(0));

            OrgUnitDTO result = applicationService.updateOrgUnit(ORG_UNIT_ID, command);

            assertNotNull(result);
            assertEquals("Updated Name", result.getUnitName());

            verify(orgUnitRepository).findById(ORG_UNIT_ID);
            verify(orgUnitRepository).save(any(OrgUnit.class));
        }

        @Test
        @DisplayName("Throw exception when OrgUnit not found for update")
        void shouldThrowExceptionWhenNotFound() {
            UpdateOrgUnitCommand command = UpdateOrgUnitCommand.builder()
                    .unitName("Updated Name")
                    .build();

            when(orgUnitRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class, () ->
                    applicationService.updateOrgUnit(999L, command)
            );

            verify(orgUnitRepository).findById(999L);
            verify(orgUnitRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Delete OrgUnit tests")
    class DeleteOrgUnitTest {

        @Test
        @DisplayName("Successfully delete OrgUnit without children")
        void shouldDeleteOrgUnitWithoutChildren() {
            OrgUnit orgUnit = createOrgUnit(ORG_UNIT_ID, "DEPT001", "IT Department", "DEPARTMENT", null);
            when(orgUnitRepository.findById(ORG_UNIT_ID)).thenReturn(Optional.of(orgUnit));
            when(orgUnitRepository.findByParentId(ORG_UNIT_ID)).thenReturn(Collections.emptyList());
            doNothing().when(orgUnitRepository).deleteById(ORG_UNIT_ID);

            assertDoesNotThrow(() -> applicationService.deleteOrgUnit(ORG_UNIT_ID));

            verify(orgUnitRepository).deleteById(ORG_UNIT_ID);
        }
    }

    @Nested
    @DisplayName("Freeze/Unfreeze/Dissolve OrgUnit tests")
    class LifecycleOrgUnitTest {

        @Test
        @DisplayName("Successfully freeze OrgUnit")
        void shouldFreezeOrgUnit() {
            OrgUnit orgUnit = createOrgUnit(ORG_UNIT_ID, "DEPT001", "IT Department", "DEPARTMENT", null);
            when(orgUnitRepository.findById(ORG_UNIT_ID)).thenReturn(Optional.of(orgUnit));
            when(orgUnitRepository.save(any(OrgUnit.class))).thenAnswer(invocation -> invocation.getArgument(0));

            applicationService.freezeOrgUnit(ORG_UNIT_ID, "测试冻结", CREATOR_ID);

            verify(orgUnitRepository).save(argThat(unit -> unit.getStatus() == OrgUnitStatus.FROZEN));
        }

        @Test
        @DisplayName("Successfully unfreeze OrgUnit")
        void shouldUnfreezeOrgUnit() {
            OrgUnit orgUnit = createOrgUnit(ORG_UNIT_ID, "DEPT001", "IT Department", "DEPARTMENT", null);
            orgUnit.freeze("冻结", CREATOR_ID);
            when(orgUnitRepository.findById(ORG_UNIT_ID)).thenReturn(Optional.of(orgUnit));
            when(orgUnitRepository.save(any(OrgUnit.class))).thenAnswer(invocation -> invocation.getArgument(0));

            applicationService.unfreezeOrgUnit(ORG_UNIT_ID, CREATOR_ID);

            verify(orgUnitRepository).save(argThat(unit -> unit.getStatus() == OrgUnitStatus.ACTIVE));
        }

        @Test
        @DisplayName("Successfully dissolve OrgUnit")
        void shouldDissolveOrgUnit() {
            OrgUnit orgUnit = createOrgUnit(ORG_UNIT_ID, "DEPT001", "IT Department", "DEPARTMENT", null);
            when(orgUnitRepository.findById(ORG_UNIT_ID)).thenReturn(Optional.of(orgUnit));
            when(orgUnitRepository.save(any(OrgUnit.class))).thenAnswer(invocation -> invocation.getArgument(0));

            applicationService.dissolveOrgUnit(ORG_UNIT_ID, "业务调整", CREATOR_ID);

            verify(orgUnitRepository).save(argThat(unit -> unit.getStatus() == OrgUnitStatus.DISSOLVED));
        }

        @Test
        @DisplayName("Throw exception when freezing non-existent OrgUnit")
        void shouldThrowExceptionWhenFreezingNonExistent() {
            when(orgUnitRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class, () ->
                    applicationService.freezeOrgUnit(999L, "test", CREATOR_ID)
            );
        }
    }

    @Nested
    @DisplayName("Get OrgUnit tree tests")
    class GetOrgUnitTreeTest {

        @Test
        @DisplayName("Get empty tree when no org units")
        void shouldReturnEmptyTreeWhenNoOrgUnits() {
            when(orgUnitRepository.findAll()).thenReturn(Collections.emptyList());
            when(orgUnitTypeRepository.findAll()).thenReturn(Collections.emptyList());

            var result = applicationService.getOrgUnitTree();

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Get tree with single root")
        void shouldReturnTreeWithSingleRoot() {
            OrgUnit root = createOrgUnit(1L, "SCHOOL001", "School", "SCHOOL", null);
            when(orgUnitRepository.findAll()).thenReturn(List.of(root));
            when(orgUnitTypeRepository.findAll()).thenReturn(Collections.emptyList());

            var result = applicationService.getOrgUnitTree();

            assertEquals(1, result.size());
            assertEquals("SCHOOL001", result.get(0).getUnitCode());
        }
    }

    private OrgUnit createOrgUnit(Long id, String code, String name, String type, Long parentId) {
        return OrgUnit.builder()
                .id(id)
                .unitCode(code)
                .unitName(name)
                .unitType(type)
                .parentId(parentId)
                .treePath(parentId == null ? "/" + id + "/" : "/" + parentId + "/" + id + "/")
                .treeLevel(parentId == null ? 0 : 1)
                .sortOrder(0)
                .status(OrgUnitStatus.ACTIVE)
                .createdBy(CREATOR_ID)
                .build();
    }
}
