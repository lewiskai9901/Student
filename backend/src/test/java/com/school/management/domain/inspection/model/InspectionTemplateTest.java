package com.school.management.domain.inspection.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 检查模板聚合根单元测试
 * 测试检查模板的创建、发布、归档、分类管理等功能
 */
@DisplayName("检查模板聚合根测试")
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
            "日常检查模板",
            "用于日常班级量化检查",
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
    @DisplayName("创建模板测试")
    class CreateTemplateTest {

        @Test
        @DisplayName("成功创建模板")
        void shouldCreateTemplateSuccessfully() {
            assertNotNull(template);
            assertEquals("TPL001", template.getTemplateCode());
            assertEquals("日常检查模板", template.getTemplateName());
            assertEquals("用于日常班级量化检查", template.getDescription());
            assertEquals(TemplateScope.GLOBAL, template.getScope());
            assertNull(template.getApplicableOrgUnitId());
            assertEquals(TemplateStatus.DRAFT, template.getStatus());
            assertFalse(template.isDefault());
            assertEquals(1, template.getCurrentVersion());
            assertEquals(CREATOR_ID, template.getCreatedBy());
            assertNotNull(template.getCreatedAt());
            assertFalse(template.getDomainEvents().isEmpty());
        }

        @Test
        @DisplayName("创建部门范围的模板")
        void shouldCreateDepartmentScopeTemplate() {
            InspectionTemplate deptTemplate = InspectionTemplate.create(
                "TPL002",
                "系部检查模板",
                "信息技术系专用模板",
                TemplateScope.DEPARTMENT,
                ORG_UNIT_ID,
                CREATOR_ID
            );

            assertEquals(TemplateScope.DEPARTMENT, deptTemplate.getScope());
            assertEquals(ORG_UNIT_ID, deptTemplate.getApplicableOrgUnitId());
        }

        @Test
        @DisplayName("创建模板时缺少编码应抛出异常")
        void shouldFailWhenTemplateCodeIsNull() {
            assertThrows(NullPointerException.class, () ->
                InspectionTemplate.create(null, "日常检查模板", null, TemplateScope.GLOBAL, null, CREATOR_ID)
            );
        }

        @Test
        @DisplayName("创建模板时缺少名称应抛出异常")
        void shouldFailWhenTemplateNameIsNull() {
            assertThrows(NullPointerException.class, () ->
                InspectionTemplate.create("TPL001", null, null, TemplateScope.GLOBAL, null, CREATOR_ID)
            );
        }

        @Test
        @DisplayName("创建模板时编码为空字符串应抛出异常")
        void shouldFailWhenTemplateCodeIsBlank() {
            assertThrows(IllegalArgumentException.class, () ->
                InspectionTemplate.create("  ", "日常检查模板", null, TemplateScope.GLOBAL, null, CREATOR_ID)
            );
        }

        @Test
        @DisplayName("创建模板时编码过长应抛出异常")
        void shouldFailWhenTemplateCodeTooLong() {
            String longCode = "A".repeat(51);
            assertThrows(IllegalArgumentException.class, () ->
                InspectionTemplate.create(longCode, "日常检查模板", null, TemplateScope.GLOBAL, null, CREATOR_ID)
            );
        }

        @Test
        @DisplayName("创建模板时名称过长应抛出异常")
        void shouldFailWhenTemplateNameTooLong() {
            String longName = "A".repeat(101);
            assertThrows(IllegalArgumentException.class, () ->
                InspectionTemplate.create("TPL001", longName, null, TemplateScope.GLOBAL, null, CREATOR_ID)
            );
        }
    }

    @Nested
    @DisplayName("分类管理测试")
    class CategoryManagementTest {

        @Test
        @DisplayName("添加分类到草稿模板")
        void shouldAddCategoryToDraftTemplate() {
            InspectionCategory category = createTestCategory(1L, "纪律", 20);

            template.addCategory(category);

            assertEquals(1, template.getCategories().size());
            assertEquals("纪律", template.getCategories().get(0).getCategoryName());
        }

        @Test
        @DisplayName("不能向已发布模板添加分类")
        void shouldFailToAddCategoryToPublishedTemplate() {
            InspectionCategory category = createTestCategory(1L, "纪律", 20);
            template.addCategory(category);
            template.publish();

            InspectionCategory newCategory = createTestCategory(2L, "卫生", 30);
            assertThrows(IllegalStateException.class, () ->
                template.addCategory(newCategory)
            );
        }

        @Test
        @DisplayName("从草稿模板移除分类")
        void shouldRemoveCategoryFromDraftTemplate() {
            InspectionCategory category = createTestCategory(1L, "纪律", 20);
            template.addCategory(category);

            template.removeCategory(1L);

            assertTrue(template.getCategories().isEmpty());
        }

        @Test
        @DisplayName("不能从已发布模板移除分类")
        void shouldFailToRemoveCategoryFromPublishedTemplate() {
            InspectionCategory category = createTestCategory(1L, "纪律", 20);
            template.addCategory(category);
            template.publish();

            assertThrows(IllegalStateException.class, () ->
                template.removeCategory(1L)
            );
        }

        @Test
        @DisplayName("计算模板总基础分")
        void shouldCalculateTotalBaseScore() {
            template.addCategory(createTestCategory(1L, "纪律", 20));
            template.addCategory(createTestCategory(2L, "卫生", 30));
            template.addCategory(createTestCategory(3L, "学习", 50));

            assertEquals(100, template.calculateTotalBaseScore());
        }
    }

    @Nested
    @DisplayName("发布模板测试")
    class PublishTemplateTest {

        @Test
        @DisplayName("成功发布有分类的模板")
        void shouldPublishTemplateWithCategories() {
            template.addCategory(createTestCategory(1L, "纪律", 20));

            template.publish();

            assertEquals(TemplateStatus.PUBLISHED, template.getStatus());
            assertEquals(2, template.getCurrentVersion());
        }

        @Test
        @DisplayName("不能发布没有分类的模板")
        void shouldFailToPublishTemplateWithoutCategories() {
            assertThrows(IllegalStateException.class, () ->
                template.publish()
            );
        }
    }

    @Nested
    @DisplayName("归档模板测试")
    class ArchiveTemplateTest {

        @Test
        @DisplayName("归档模板")
        void shouldArchiveTemplate() {
            template.addCategory(createTestCategory(1L, "纪律", 20));
            template.publish();

            template.archive();

            assertEquals(TemplateStatus.ARCHIVED, template.getStatus());
        }

        @Test
        @DisplayName("可以归档草稿模板")
        void shouldArchiveDraftTemplate() {
            template.archive();

            assertEquals(TemplateStatus.ARCHIVED, template.getStatus());
        }
    }

    @Nested
    @DisplayName("设置默认模板测试")
    class SetDefaultTemplateTest {

        @Test
        @DisplayName("设置已发布模板为默认")
        void shouldSetPublishedTemplateAsDefault() {
            template.addCategory(createTestCategory(1L, "纪律", 20));
            template.publish();

            template.setAsDefault();

            assertTrue(template.isDefault());
        }

        @Test
        @DisplayName("不能将未发布模板设为默认")
        void shouldFailToSetDraftTemplateAsDefault() {
            assertThrows(IllegalStateException.class, () ->
                template.setAsDefault()
            );
        }
    }

    @Nested
    @DisplayName("更新模板信息测试")
    class UpdateTemplateInfoTest {

        @Test
        @DisplayName("更新模板信息")
        void shouldUpdateTemplateInfo() {
            template.updateInfo("新的检查模板名称", "新的描述");

            assertEquals("新的检查模板名称", template.getTemplateName());
            assertEquals("新的描述", template.getDescription());
        }

        @Test
        @DisplayName("更新时空名称保持原值")
        void shouldKeepOriginalNameWhenUpdateWithBlank() {
            template.updateInfo("", "新的描述");

            assertEquals("日常检查模板", template.getTemplateName());
            assertEquals("新的描述", template.getDescription());
        }

        @Test
        @DisplayName("更新时null名称保持原值")
        void shouldKeepOriginalNameWhenUpdateWithNull() {
            template.updateInfo(null, "新的描述");

            assertEquals("日常检查模板", template.getTemplateName());
        }
    }

    @Nested
    @DisplayName("Builder测试")
    class BuilderTest {

        @Test
        @DisplayName("使用Builder构建完整对象")
        void shouldBuildTemplateWithAllFields() {
            InspectionTemplate built = InspectionTemplate.builder()
                .id(1L)
                .templateCode("TPL001")
                .templateName("日常检查模板")
                .description("描述")
                .scope(TemplateScope.GLOBAL)
                .applicableOrgUnitId(null)
                .isDefault(false)
                .currentVersion(1)
                .status(TemplateStatus.DRAFT)
                .createdBy(CREATOR_ID)
                .build();

            assertEquals(1L, built.getId());
            assertEquals("TPL001", built.getTemplateCode());
            assertEquals(TemplateStatus.DRAFT, built.getStatus());
        }
    }
}
