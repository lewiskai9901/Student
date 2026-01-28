package com.school.management.domain.inspection.model;

import com.school.management.domain.inspection.event.InspectionTemplateCreatedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for the InspectionTemplate aggregate root.
 *
 * Covers: creation with valid/invalid data, category management,
 * lifecycle transitions (DRAFT -> PUBLISHED -> ARCHIVED),
 * default template assignment, total base score calculation,
 * domain event registration, and builder construction.
 */
@DisplayName("InspectionTemplate Aggregate Root")
class InspectionTemplateTest {

    private static final Long CREATOR_ID = 1L;
    private static final Long ORG_UNIT_ID = 10L;

    private InspectionTemplate template;

    @BeforeEach
    void setUp() {
        template = createValidTemplate();
    }

    private InspectionTemplate createValidTemplate() {
        return InspectionTemplate.create(
            "TPL001",
            "Daily Inspection Template",
            "Used for daily class quantification checks",
            TemplateScope.GLOBAL,
            null,
            CREATOR_ID
        );
    }

    private InspectionCategory createTestCategory(Long id, String name, int baseScore) {
        return InspectionCategory.builder()
            .id(id)
            .categoryCode("CAT" + id)
            .categoryName(name)
            .baseScore(baseScore)
            .sortOrder(id.intValue())
            .build();
    }

    @Nested
    @DisplayName("Creation")
    class CreateTemplateTest {

        @Test
        @DisplayName("should create template with valid data and DRAFT status")
        void shouldCreateTemplateSuccessfully() {
            // then
            assertNotNull(template);
            assertEquals("TPL001", template.getTemplateCode());
            assertEquals("Daily Inspection Template", template.getTemplateName());
            assertEquals("Used for daily class quantification checks", template.getDescription());
            assertEquals(TemplateScope.GLOBAL, template.getScope());
            assertNull(template.getApplicableOrgUnitId());
            assertEquals(TemplateStatus.DRAFT, template.getStatus());
            assertFalse(template.isDefault());
            assertEquals(1, template.getCurrentVersion());
            assertEquals(CREATOR_ID, template.getCreatedBy());
            assertNotNull(template.getCreatedAt());
            assertTrue(template.getCategories().isEmpty());
        }

        @Test
        @DisplayName("should register InspectionTemplateCreatedEvent on creation")
        void shouldRegisterCreatedEvent() {
            // then
            assertFalse(template.getDomainEvents().isEmpty());
            assertEquals(1, template.getDomainEvents().size());
            assertInstanceOf(InspectionTemplateCreatedEvent.class, template.getDomainEvents().get(0));
        }

        @Test
        @DisplayName("should create department-scoped template with org unit ID")
        void shouldCreateDepartmentScopeTemplate() {
            // when
            InspectionTemplate deptTemplate = InspectionTemplate.create(
                "TPL002",
                "Department Template",
                "IT department specific template",
                TemplateScope.DEPARTMENT,
                ORG_UNIT_ID,
                CREATOR_ID
            );

            // then
            assertEquals(TemplateScope.DEPARTMENT, deptTemplate.getScope());
            assertEquals(ORG_UNIT_ID, deptTemplate.getApplicableOrgUnitId());
        }

        @Test
        @DisplayName("should create grade-scoped template")
        void shouldCreateGradeScopeTemplate() {
            // when
            InspectionTemplate gradeTemplate = InspectionTemplate.create(
                "TPL003",
                "Grade Template",
                null,
                TemplateScope.GRADE,
                ORG_UNIT_ID,
                CREATOR_ID
            );

            // then
            assertEquals(TemplateScope.GRADE, gradeTemplate.getScope());
        }

        @Test
        @DisplayName("should throw NullPointerException when template code is null")
        void shouldFailWhenTemplateCodeIsNull() {
            assertThrows(NullPointerException.class, () ->
                InspectionTemplate.create(null, "Template", null, TemplateScope.GLOBAL, null, CREATOR_ID)
            );
        }

        @Test
        @DisplayName("should throw NullPointerException when template name is null")
        void shouldFailWhenTemplateNameIsNull() {
            assertThrows(NullPointerException.class, () ->
                InspectionTemplate.create("TPL001", null, null, TemplateScope.GLOBAL, null, CREATOR_ID)
            );
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when template code is blank")
        void shouldFailWhenTemplateCodeIsBlank() {
            assertThrows(IllegalArgumentException.class, () ->
                InspectionTemplate.create("  ", "Template", null, TemplateScope.GLOBAL, null, CREATOR_ID)
            );
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when template code exceeds 50 characters")
        void shouldFailWhenTemplateCodeTooLong() {
            String longCode = "A".repeat(51);
            assertThrows(IllegalArgumentException.class, () ->
                InspectionTemplate.create(longCode, "Template", null, TemplateScope.GLOBAL, null, CREATOR_ID)
            );
        }

        @Test
        @DisplayName("should accept template code of exactly 50 characters")
        void shouldAcceptTemplateCodeAtMaxLength() {
            String maxCode = "A".repeat(50);
            InspectionTemplate t = InspectionTemplate.create(
                maxCode, "Template", null, TemplateScope.GLOBAL, null, CREATOR_ID
            );
            assertEquals(maxCode, t.getTemplateCode());
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when template name exceeds 100 characters")
        void shouldFailWhenTemplateNameTooLong() {
            String longName = "A".repeat(101);
            assertThrows(IllegalArgumentException.class, () ->
                InspectionTemplate.create("TPL001", longName, null, TemplateScope.GLOBAL, null, CREATOR_ID)
            );
        }

        @Test
        @DisplayName("should accept template name of exactly 100 characters")
        void shouldAcceptTemplateNameAtMaxLength() {
            String maxName = "A".repeat(100);
            InspectionTemplate t = InspectionTemplate.create(
                "TPL001", maxName, null, TemplateScope.GLOBAL, null, CREATOR_ID
            );
            assertEquals(maxName, t.getTemplateName());
        }

        @Test
        @DisplayName("should allow null description")
        void shouldAllowNullDescription() {
            InspectionTemplate t = InspectionTemplate.create(
                "TPL001", "Template", null, TemplateScope.GLOBAL, null, CREATOR_ID
            );
            assertNull(t.getDescription());
        }
    }

    @Nested
    @DisplayName("Category Management")
    class CategoryManagementTest {

        @Test
        @DisplayName("should add category to DRAFT template")
        void shouldAddCategoryToDraftTemplate() {
            // given
            InspectionCategory category = createTestCategory(1L, "Discipline", 20);

            // when
            template.addCategory(category);

            // then
            assertEquals(1, template.getCategories().size());
            assertEquals("Discipline", template.getCategories().get(0).getCategoryName());
        }

        @Test
        @DisplayName("should add multiple categories to DRAFT template")
        void shouldAddMultipleCategoriesToDraftTemplate() {
            // when
            template.addCategory(createTestCategory(1L, "Discipline", 20));
            template.addCategory(createTestCategory(2L, "Hygiene", 30));
            template.addCategory(createTestCategory(3L, "Study", 50));

            // then
            assertEquals(3, template.getCategories().size());
        }

        @Test
        @DisplayName("should throw IllegalStateException when adding category to PUBLISHED template")
        void shouldFailToAddCategoryToPublishedTemplate() {
            // given
            template.addCategory(createTestCategory(1L, "Discipline", 20));
            template.publish();

            // when/then
            InspectionCategory newCategory = createTestCategory(2L, "Hygiene", 30);
            IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                template.addCategory(newCategory)
            );
            assertTrue(ex.getMessage().contains("non-draft"));
        }

        @Test
        @DisplayName("should throw IllegalStateException when adding category to ARCHIVED template")
        void shouldFailToAddCategoryToArchivedTemplate() {
            // given
            template.archive();

            // when/then
            InspectionCategory category = createTestCategory(1L, "Discipline", 20);
            assertThrows(IllegalStateException.class, () ->
                template.addCategory(category)
            );
        }

        @Test
        @DisplayName("should remove category from DRAFT template")
        void shouldRemoveCategoryFromDraftTemplate() {
            // given
            template.addCategory(createTestCategory(1L, "Discipline", 20));

            // when
            template.removeCategory(1L);

            // then
            assertTrue(template.getCategories().isEmpty());
        }

        @Test
        @DisplayName("should only remove matching category ID")
        void shouldOnlyRemoveMatchingCategoryId() {
            // given
            template.addCategory(createTestCategory(1L, "Discipline", 20));
            template.addCategory(createTestCategory(2L, "Hygiene", 30));

            // when
            template.removeCategory(1L);

            // then
            assertEquals(1, template.getCategories().size());
            assertEquals("Hygiene", template.getCategories().get(0).getCategoryName());
        }

        @Test
        @DisplayName("should throw IllegalStateException when removing category from PUBLISHED template")
        void shouldFailToRemoveCategoryFromPublishedTemplate() {
            // given
            template.addCategory(createTestCategory(1L, "Discipline", 20));
            template.publish();

            // when/then
            assertThrows(IllegalStateException.class, () ->
                template.removeCategory(1L)
            );
        }

        @Test
        @DisplayName("should throw IllegalStateException when removing category from ARCHIVED template")
        void shouldFailToRemoveCategoryFromArchivedTemplate() {
            // given
            template.addCategory(createTestCategory(1L, "Discipline", 20));
            template.archive();

            // when/then
            assertThrows(IllegalStateException.class, () ->
                template.removeCategory(1L)
            );
        }

        @Test
        @DisplayName("should return unmodifiable categories list")
        void shouldReturnUnmodifiableCategoriesList() {
            // given
            template.addCategory(createTestCategory(1L, "Discipline", 20));

            // when/then
            assertThrows(UnsupportedOperationException.class, () ->
                template.getCategories().add(createTestCategory(2L, "Extra", 10))
            );
        }
    }

    @Nested
    @DisplayName("calculateTotalBaseScore")
    class CalculateTotalBaseScoreTest {

        @Test
        @DisplayName("should return sum of all category base scores")
        void shouldCalculateTotalBaseScore() {
            // given
            template.addCategory(createTestCategory(1L, "Discipline", 20));
            template.addCategory(createTestCategory(2L, "Hygiene", 30));
            template.addCategory(createTestCategory(3L, "Study", 50));

            // when
            int total = template.calculateTotalBaseScore();

            // then
            assertEquals(100, total);
        }

        @Test
        @DisplayName("should return zero when no categories exist")
        void shouldReturnZeroWhenNoCategories() {
            // when
            int total = template.calculateTotalBaseScore();

            // then
            assertEquals(0, total);
        }

        @Test
        @DisplayName("should return single category score when only one category exists")
        void shouldReturnSingleCategoryScore() {
            // given
            template.addCategory(createTestCategory(1L, "Discipline", 45));

            // when
            int total = template.calculateTotalBaseScore();

            // then
            assertEquals(45, total);
        }
    }

    @Nested
    @DisplayName("Publishing")
    class PublishTemplateTest {

        @Test
        @DisplayName("should transition from DRAFT to PUBLISHED and increment version")
        void shouldPublishTemplateWithCategories() {
            // given
            template.addCategory(createTestCategory(1L, "Discipline", 20));

            // when
            template.publish();

            // then
            assertEquals(TemplateStatus.PUBLISHED, template.getStatus());
            assertEquals(2, template.getCurrentVersion());
        }

        @Test
        @DisplayName("should throw IllegalStateException when publishing without categories")
        void shouldFailToPublishTemplateWithoutCategories() {
            // when/then
            IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                template.publish()
            );
            assertTrue(ex.getMessage().contains("without categories"));
        }

        @Test
        @DisplayName("should increment version on each publish")
        void shouldIncrementVersionOnPublish() {
            // given
            template.addCategory(createTestCategory(1L, "Discipline", 20));
            assertEquals(1, template.getCurrentVersion());

            // when
            template.publish();

            // then
            assertEquals(2, template.getCurrentVersion());
        }

        @Test
        @DisplayName("should update the updatedAt timestamp on publish")
        void shouldUpdateTimestampOnPublish() {
            // given
            template.addCategory(createTestCategory(1L, "Discipline", 20));
            var beforePublish = template.getUpdatedAt();

            // when
            template.publish();

            // then
            assertNotNull(template.getUpdatedAt());
            assertTrue(template.getUpdatedAt().compareTo(beforePublish) >= 0);
        }
    }

    @Nested
    @DisplayName("Archiving")
    class ArchiveTemplateTest {

        @Test
        @DisplayName("should archive a PUBLISHED template")
        void shouldArchivePublishedTemplate() {
            // given
            template.addCategory(createTestCategory(1L, "Discipline", 20));
            template.publish();

            // when
            template.archive();

            // then
            assertEquals(TemplateStatus.ARCHIVED, template.getStatus());
        }

        @Test
        @DisplayName("should archive a DRAFT template")
        void shouldArchiveDraftTemplate() {
            // when
            template.archive();

            // then
            assertEquals(TemplateStatus.ARCHIVED, template.getStatus());
        }

        @Test
        @DisplayName("should update updatedAt timestamp on archive")
        void shouldUpdateTimestampOnArchive() {
            // given
            var before = template.getUpdatedAt();

            // when
            template.archive();

            // then
            assertTrue(template.getUpdatedAt().compareTo(before) >= 0);
        }
    }

    @Nested
    @DisplayName("Set As Default")
    class SetDefaultTemplateTest {

        @Test
        @DisplayName("should set PUBLISHED template as default")
        void shouldSetPublishedTemplateAsDefault() {
            // given
            template.addCategory(createTestCategory(1L, "Discipline", 20));
            template.publish();

            // when
            template.setAsDefault();

            // then
            assertTrue(template.isDefault());
        }

        @Test
        @DisplayName("should throw IllegalStateException when setting DRAFT template as default")
        void shouldFailToSetDraftTemplateAsDefault() {
            // when/then
            IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                template.setAsDefault()
            );
            assertTrue(ex.getMessage().contains("published"));
        }

        @Test
        @DisplayName("should throw IllegalStateException when setting ARCHIVED template as default")
        void shouldFailToSetArchivedTemplateAsDefault() {
            // given
            template.archive();

            // when/then
            assertThrows(IllegalStateException.class, () ->
                template.setAsDefault()
            );
        }
    }

    @Nested
    @DisplayName("Update Info")
    class UpdateTemplateInfoTest {

        @Test
        @DisplayName("should update name and description")
        void shouldUpdateTemplateInfo() {
            // when
            template.updateInfo("New Template Name", "New description");

            // then
            assertEquals("New Template Name", template.getTemplateName());
            assertEquals("New description", template.getDescription());
        }

        @Test
        @DisplayName("should keep original name when updated with blank string")
        void shouldKeepOriginalNameWhenUpdateWithBlank() {
            // when
            template.updateInfo("", "New description");

            // then
            assertEquals("Daily Inspection Template", template.getTemplateName());
            assertEquals("New description", template.getDescription());
        }

        @Test
        @DisplayName("should keep original name when updated with null")
        void shouldKeepOriginalNameWhenUpdateWithNull() {
            // when
            template.updateInfo(null, "New description");

            // then
            assertEquals("Daily Inspection Template", template.getTemplateName());
        }

        @Test
        @DisplayName("should allow setting description to null")
        void shouldAllowNullDescription() {
            // when
            template.updateInfo("Name", null);

            // then
            assertNull(template.getDescription());
        }
    }

    @Nested
    @DisplayName("Builder")
    class BuilderTest {

        @Test
        @DisplayName("should build template with all fields via Builder")
        void shouldBuildTemplateWithAllFields() {
            // when
            InspectionTemplate built = InspectionTemplate.builder()
                .id(1L)
                .templateCode("TPL001")
                .templateName("Daily Inspection")
                .description("Description")
                .scope(TemplateScope.GLOBAL)
                .applicableOrgUnitId(null)
                .isDefault(false)
                .currentVersion(1)
                .status(TemplateStatus.DRAFT)
                .createdBy(CREATOR_ID)
                .build();

            // then
            assertEquals(1L, built.getId());
            assertEquals("TPL001", built.getTemplateCode());
            assertEquals("Daily Inspection", built.getTemplateName());
            assertEquals(TemplateStatus.DRAFT, built.getStatus());
            assertFalse(built.isDefault());
            assertEquals(1, built.getCurrentVersion());
        }

        @Test
        @DisplayName("should use defaults when optional builder fields are omitted")
        void shouldUseDefaultsForOptionalFields() {
            // when
            InspectionTemplate built = InspectionTemplate.builder()
                .templateCode("TPL001")
                .templateName("Template")
                .build();

            // then
            assertEquals(TemplateScope.GLOBAL, built.getScope());
            assertFalse(built.isDefault());
            assertEquals(1, built.getCurrentVersion());
            assertEquals(TemplateStatus.DRAFT, built.getStatus());
            assertTrue(built.getCategories().isEmpty());
        }
    }

    @Nested
    @DisplayName("Domain Events")
    class DomainEventTest {

        @Test
        @DisplayName("should register event on creation via factory method")
        void shouldRegisterEventOnCreate() {
            // then (template created in setUp)
            assertEquals(1, template.getDomainEvents().size());
            assertInstanceOf(InspectionTemplateCreatedEvent.class, template.getDomainEvents().get(0));
        }

        @Test
        @DisplayName("should clear domain events")
        void shouldClearDomainEvents() {
            // given
            assertFalse(template.getDomainEvents().isEmpty());

            // when
            template.clearDomainEvents();

            // then
            assertTrue(template.getDomainEvents().isEmpty());
        }

        @Test
        @DisplayName("builder-constructed template should not have domain events")
        void builderShouldNotRegisterEvents() {
            // when
            InspectionTemplate built = InspectionTemplate.builder()
                .templateCode("TPL001")
                .templateName("Template")
                .build();

            // then
            assertTrue(built.getDomainEvents().isEmpty());
        }
    }
}
