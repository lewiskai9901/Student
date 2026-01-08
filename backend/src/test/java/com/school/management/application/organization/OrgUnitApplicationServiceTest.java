package com.school.management.application.organization;

import com.school.management.application.organization.command.UpdateOrgUnitCommand;
import com.school.management.application.organization.query.OrgUnitDTO;
import com.school.management.domain.organization.model.OrgUnit;
import com.school.management.domain.organization.model.OrgUnitType;
import com.school.management.domain.organization.repository.OrgUnitRepository;
import com.school.management.domain.shared.event.DomainEventPublisher;
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
    private DomainEventPublisher eventPublisher;

    private OrgUnitApplicationService applicationService;

    private static final Long CREATOR_ID = 1L;
    private static final Long ORG_UNIT_ID = 100L;

    @BeforeEach
    void setUp() {
        applicationService = new OrgUnitApplicationService(orgUnitRepository, eventPublisher);
    }

    @Nested
    @DisplayName("Get OrgUnit tests")
    class GetOrgUnitTest {

        @Test
        @DisplayName("Successfully get OrgUnit by ID")
        void shouldGetOrgUnitById() {
            OrgUnit orgUnit = createOrgUnit(ORG_UNIT_ID, "DEPT001", "IT Department", OrgUnitType.DEPARTMENT, null);
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
                    createOrgUnit(1L, "DEPT001", "Dept 1", OrgUnitType.DEPARTMENT, null),
                    createOrgUnit(2L, "DEPT002", "Dept 2", OrgUnitType.DEPARTMENT, null)
            );
            when(orgUnitRepository.findByUnitType(OrgUnitType.DEPARTMENT)).thenReturn(departments);

            List<OrgUnitDTO> result = applicationService.getOrgUnitsByType(OrgUnitType.DEPARTMENT);

            assertEquals(2, result.size());
            verify(orgUnitRepository).findByUnitType(OrgUnitType.DEPARTMENT);
        }

        @Test
        @DisplayName("Get children of OrgUnit")
        void shouldGetChildren() {
            Long parentId = 10L;
            List<OrgUnit> children = Arrays.asList(
                    createOrgUnit(1L, "CHILD001", "Child 1", OrgUnitType.DEPARTMENT, parentId),
                    createOrgUnit(2L, "CHILD002", "Child 2", OrgUnitType.DEPARTMENT, parentId)
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
                    .leaderId(200L)
                    .sortOrder(10)
                    .updatedBy(CREATOR_ID)
                    .build();

            OrgUnit existingOrgUnit = createOrgUnit(ORG_UNIT_ID, "DEPT001", "Old Name", OrgUnitType.DEPARTMENT, null);
            when(orgUnitRepository.findById(ORG_UNIT_ID)).thenReturn(Optional.of(existingOrgUnit));
            when(orgUnitRepository.save(any(OrgUnit.class))).thenAnswer(invocation -> invocation.getArgument(0));

            OrgUnitDTO result = applicationService.updateOrgUnit(ORG_UNIT_ID, command);

            assertNotNull(result);
            assertEquals("Updated Name", result.getUnitName());
            assertEquals(200L, result.getLeaderId());

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
            OrgUnit orgUnit = createOrgUnit(ORG_UNIT_ID, "DEPT001", "IT Department", OrgUnitType.DEPARTMENT, null);
            when(orgUnitRepository.findById(ORG_UNIT_ID)).thenReturn(Optional.of(orgUnit));
            when(orgUnitRepository.countByParentId(ORG_UNIT_ID)).thenReturn(0L);
            doNothing().when(orgUnitRepository).deleteById(ORG_UNIT_ID);

            assertDoesNotThrow(() -> applicationService.deleteOrgUnit(ORG_UNIT_ID));

            verify(orgUnitRepository).deleteById(ORG_UNIT_ID);
        }

        @Test
        @DisplayName("Throw exception when deleting OrgUnit with children")
        void shouldThrowExceptionWhenDeletingWithChildren() {
            OrgUnit orgUnit = createOrgUnit(ORG_UNIT_ID, "DEPT001", "IT Department", OrgUnitType.DEPARTMENT, null);
            when(orgUnitRepository.findById(ORG_UNIT_ID)).thenReturn(Optional.of(orgUnit));
            when(orgUnitRepository.countByParentId(ORG_UNIT_ID)).thenReturn(5L);

            assertThrows(IllegalStateException.class, () ->
                    applicationService.deleteOrgUnit(ORG_UNIT_ID)
            );

            verify(orgUnitRepository, never()).deleteById(any());
        }
    }

    @Nested
    @DisplayName("Enable/Disable OrgUnit tests")
    class EnableDisableOrgUnitTest {

        @Test
        @DisplayName("Successfully enable OrgUnit")
        void shouldEnableOrgUnit() {
            OrgUnit orgUnit = createOrgUnit(ORG_UNIT_ID, "DEPT001", "IT Department", OrgUnitType.DEPARTMENT, null);
            orgUnit.disable();
            when(orgUnitRepository.findById(ORG_UNIT_ID)).thenReturn(Optional.of(orgUnit));
            when(orgUnitRepository.save(any(OrgUnit.class))).thenAnswer(invocation -> invocation.getArgument(0));

            applicationService.enableOrgUnit(ORG_UNIT_ID);

            verify(orgUnitRepository).save(argThat(unit -> unit.isEnabled()));
        }

        @Test
        @DisplayName("Successfully disable OrgUnit")
        void shouldDisableOrgUnit() {
            OrgUnit orgUnit = createOrgUnit(ORG_UNIT_ID, "DEPT001", "IT Department", OrgUnitType.DEPARTMENT, null);
            when(orgUnitRepository.findById(ORG_UNIT_ID)).thenReturn(Optional.of(orgUnit));
            when(orgUnitRepository.save(any(OrgUnit.class))).thenAnswer(invocation -> invocation.getArgument(0));

            applicationService.disableOrgUnit(ORG_UNIT_ID);

            verify(orgUnitRepository).save(argThat(unit -> !unit.isEnabled()));
        }

        @Test
        @DisplayName("Throw exception when enabling non-existent OrgUnit")
        void shouldThrowExceptionWhenEnablingNonExistent() {
            when(orgUnitRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class, () ->
                    applicationService.enableOrgUnit(999L)
            );
        }
    }

    @Nested
    @DisplayName("Get OrgUnit tree tests")
    class GetOrgUnitTreeTest {

        @Test
        @DisplayName("Get empty tree when no roots")
        void shouldReturnEmptyTreeWhenNoRoots() {
            when(orgUnitRepository.findRoots()).thenReturn(Collections.emptyList());

            var result = applicationService.getOrgUnitTree();

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Get tree with single root")
        void shouldReturnTreeWithSingleRoot() {
            OrgUnit root = createOrgUnit(1L, "SCHOOL001", "School", OrgUnitType.SCHOOL, null);
            when(orgUnitRepository.findRoots()).thenReturn(Arrays.asList(root));
            when(orgUnitRepository.findByParentId(1L)).thenReturn(Collections.emptyList());

            var result = applicationService.getOrgUnitTree();

            assertEquals(1, result.size());
            assertEquals("SCHOOL001", result.get(0).getUnitCode());
        }
    }

    private OrgUnit createOrgUnit(Long id, String code, String name, OrgUnitType type, Long parentId) {
        return OrgUnit.builder()
                .id(id)
                .unitCode(code)
                .unitName(name)
                .unitType(type)
                .parentId(parentId)
                .treePath(parentId == null ? "/" + id + "/" : "/" + parentId + "/" + id + "/")
                .treeLevel(parentId == null ? 0 : 1)
                .sortOrder(0)
                .enabled(true)
                .createdBy(CREATOR_ID)
                .build();
    }
}
