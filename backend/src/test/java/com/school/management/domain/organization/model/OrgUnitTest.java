package com.school.management.domain.organization.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 组织单元聚合根单元测试
 * 测试组织单元的创建、更新、启用/禁用、层级关系等功能
 */
@DisplayName("组织单元聚合根测试")
class OrgUnitTest {

    private static final Long CREATOR_ID = 1L;
    private static final Long LEADER_ID = 100L;

    @Nested
    @DisplayName("创建组织单元测试")
    class CreateOrgUnitTest {

        @Test
        @DisplayName("成功创建组织单元")
        void shouldCreateOrgUnitSuccessfully() {
            OrgUnit orgUnit = OrgUnit.create(
                "DEPT001",
                "信息技术系",
                OrgUnitType.DEPARTMENT,
                null,
                CREATOR_ID
            );

            assertNotNull(orgUnit);
            assertEquals("DEPT001", orgUnit.getUnitCode());
            assertEquals("信息技术系", orgUnit.getUnitName());
            assertEquals(OrgUnitType.DEPARTMENT, orgUnit.getUnitType());
            assertNull(orgUnit.getParentId());
            assertTrue(orgUnit.isEnabled());
            assertEquals(CREATOR_ID, orgUnit.getCreatedBy());
            assertNotNull(orgUnit.getCreatedAt());
            assertFalse(orgUnit.getDomainEvents().isEmpty());
        }

        @Test
        @DisplayName("创建带父节点的组织单元")
        void shouldCreateOrgUnitWithParent() {
            OrgUnit orgUnit = OrgUnit.create(
                "GROUP001",
                "软件教研组",
                OrgUnitType.TEACHING_GROUP,
                1L,
                CREATOR_ID
            );

            assertEquals(1L, orgUnit.getParentId());
        }

        @Test
        @DisplayName("创建组织单元时缺少编码应抛出异常")
        void shouldFailWhenUnitCodeIsNull() {
            assertThrows(NullPointerException.class, () ->
                OrgUnit.create(null, "信息技术系", OrgUnitType.DEPARTMENT, null, CREATOR_ID)
            );
        }

        @Test
        @DisplayName("创建组织单元时缺少名称应抛出异常")
        void shouldFailWhenUnitNameIsNull() {
            assertThrows(NullPointerException.class, () ->
                OrgUnit.create("DEPT001", null, OrgUnitType.DEPARTMENT, null, CREATOR_ID)
            );
        }

        @Test
        @DisplayName("创建组织单元时缺少类型应抛出异常")
        void shouldFailWhenUnitTypeIsNull() {
            assertThrows(NullPointerException.class, () ->
                OrgUnit.create("DEPT001", "信息技术系", null, null, CREATOR_ID)
            );
        }

        @Test
        @DisplayName("创建组织单元时编码为空字符串应抛出异常")
        void shouldFailWhenUnitCodeIsBlank() {
            assertThrows(IllegalArgumentException.class, () ->
                OrgUnit.create("  ", "信息技术系", OrgUnitType.DEPARTMENT, null, CREATOR_ID)
            );
        }

        @Test
        @DisplayName("创建组织单元时编码过长应抛出异常")
        void shouldFailWhenUnitCodeTooLong() {
            String longCode = "A".repeat(51);
            assertThrows(IllegalArgumentException.class, () ->
                OrgUnit.create(longCode, "信息技术系", OrgUnitType.DEPARTMENT, null, CREATOR_ID)
            );
        }
    }

    @Nested
    @DisplayName("更新组织单元测试")
    class UpdateOrgUnitTest {

        private OrgUnit orgUnit;

        @BeforeEach
        void setUp() {
            orgUnit = OrgUnit.create(
                "DEPT001",
                "信息技术系",
                OrgUnitType.DEPARTMENT,
                null,
                CREATOR_ID
            );
            orgUnit.clearDomainEvents();
        }

        @Test
        @DisplayName("成功更新组织单元信息")
        void shouldUpdateOrgUnitSuccessfully() {
            List<Long> deputyLeaderIds = Arrays.asList(200L, 300L);

            orgUnit.update("信息工程系", null, LEADER_ID, deputyLeaderIds, 10, 2L);

            assertEquals("信息工程系", orgUnit.getUnitName());
            assertEquals(LEADER_ID, orgUnit.getLeaderId());
            assertEquals(2, orgUnit.getDeputyLeaderIds().size());
            assertEquals(10, orgUnit.getSortOrder());
            assertEquals(2L, orgUnit.getUpdatedBy());
            assertFalse(orgUnit.getDomainEvents().isEmpty());
        }

        @Test
        @DisplayName("更新时空名称保持原值")
        void shouldKeepOriginalNameWhenUpdateWithBlank() {
            orgUnit.update("", null, LEADER_ID, null, null, 2L);

            assertEquals("信息技术系", orgUnit.getUnitName());
        }

        @Test
        @DisplayName("更新时null名称保持原值")
        void shouldKeepOriginalNameWhenUpdateWithNull() {
            orgUnit.update(null, null, LEADER_ID, null, null, 2L);

            assertEquals("信息技术系", orgUnit.getUnitName());
        }
    }

    @Nested
    @DisplayName("启用/禁用测试")
    class EnableDisableTest {

        private OrgUnit orgUnit;

        @BeforeEach
        void setUp() {
            orgUnit = OrgUnit.create(
                "DEPT001",
                "信息技术系",
                OrgUnitType.DEPARTMENT,
                null,
                CREATOR_ID
            );
        }

        @Test
        @DisplayName("禁用组织单元")
        void shouldDisableOrgUnit() {
            orgUnit.disable();

            assertFalse(orgUnit.isEnabled());
        }

        @Test
        @DisplayName("启用组织单元")
        void shouldEnableOrgUnit() {
            orgUnit.disable();
            orgUnit.enable();

            assertTrue(orgUnit.isEnabled());
        }
    }

    @Nested
    @DisplayName("领导分配测试")
    class AssignLeaderTest {

        private OrgUnit orgUnit;

        @BeforeEach
        void setUp() {
            orgUnit = OrgUnit.create(
                "DEPT001",
                "信息技术系",
                OrgUnitType.DEPARTMENT,
                null,
                CREATOR_ID
            );
        }

        @Test
        @DisplayName("分配领导")
        void shouldAssignLeader() {
            orgUnit.assignLeader(LEADER_ID, 2L);

            assertEquals(LEADER_ID, orgUnit.getLeaderId());
            assertEquals(2L, orgUnit.getUpdatedBy());
        }
    }

    @Nested
    @DisplayName("树路径测试")
    class TreePathTest {

        private OrgUnit orgUnit;

        @BeforeEach
        void setUp() {
            orgUnit = OrgUnit.create(
                "DEPT001",
                "信息技术系",
                OrgUnitType.DEPARTMENT,
                null,
                CREATOR_ID
            );
            orgUnit.setId(1L);
        }

        @Test
        @DisplayName("设置根节点路径")
        void shouldSetRootTreePath() {
            orgUnit.setTreePosition(null, 0);

            assertEquals("/1/", orgUnit.getTreePath());
            assertEquals(1, orgUnit.getTreeLevel());
        }

        @Test
        @DisplayName("设置空字符串父路径时作为根节点")
        void shouldSetRootTreePathWhenParentPathEmpty() {
            orgUnit.setTreePosition("", 0);

            assertEquals("/1/", orgUnit.getTreePath());
            assertEquals(1, orgUnit.getTreeLevel());
        }

        @Test
        @DisplayName("设置子节点路径")
        void shouldSetChildTreePath() {
            orgUnit.setTreePosition("/100/", 1);

            assertEquals("/100/1/", orgUnit.getTreePath());
            assertEquals(2, orgUnit.getTreeLevel());
        }
    }

    @Nested
    @DisplayName("层级关系测试")
    class HierarchyTest {

        @Test
        @DisplayName("判断祖先关系")
        void shouldIdentifyAncestor() {
            OrgUnit parent = OrgUnit.builder()
                .id(1L)
                .unitCode("DEPT001")
                .unitName("信息技术系")
                .unitType(OrgUnitType.DEPARTMENT)
                .treePath("/1/")
                .treeLevel(1)
                .build();

            OrgUnit child = OrgUnit.builder()
                .id(2L)
                .unitCode("GROUP001")
                .unitName("软件教研组")
                .unitType(OrgUnitType.TEACHING_GROUP)
                .parentId(1L)
                .treePath("/1/2/")
                .treeLevel(2)
                .build();

            assertTrue(parent.isAncestorOf(child));
            assertFalse(child.isAncestorOf(parent));
        }

        @Test
        @DisplayName("判断后代关系")
        void shouldIdentifyDescendant() {
            OrgUnit parent = OrgUnit.builder()
                .id(1L)
                .unitCode("DEPT001")
                .unitName("信息技术系")
                .unitType(OrgUnitType.DEPARTMENT)
                .treePath("/1/")
                .treeLevel(1)
                .build();

            OrgUnit child = OrgUnit.builder()
                .id(2L)
                .unitCode("GROUP001")
                .unitName("软件教研组")
                .unitType(OrgUnitType.TEACHING_GROUP)
                .parentId(1L)
                .treePath("/1/2/")
                .treeLevel(2)
                .build();

            assertTrue(child.isDescendantOf(parent));
            assertFalse(parent.isDescendantOf(child));
        }

        @Test
        @DisplayName("节点不是自己的祖先")
        void shouldNotBeAncestorOfItself() {
            OrgUnit orgUnit = OrgUnit.builder()
                .id(1L)
                .unitCode("DEPT001")
                .unitName("信息技术系")
                .unitType(OrgUnitType.DEPARTMENT)
                .treePath("/1/")
                .treeLevel(1)
                .build();

            assertFalse(orgUnit.isAncestorOf(orgUnit));
        }

        @Test
        @DisplayName("节点不是自己的后代")
        void shouldNotBeDescendantOfItself() {
            OrgUnit orgUnit = OrgUnit.builder()
                .id(1L)
                .unitCode("DEPT001")
                .unitName("信息技术系")
                .unitType(OrgUnitType.DEPARTMENT)
                .treePath("/1/")
                .treeLevel(1)
                .build();

            assertFalse(orgUnit.isDescendantOf(orgUnit));
        }

        @Test
        @DisplayName("null节点的祖先判断")
        void shouldHandleNullForAncestorCheck() {
            OrgUnit orgUnit = OrgUnit.builder()
                .id(1L)
                .unitCode("DEPT001")
                .unitName("信息技术系")
                .unitType(OrgUnitType.DEPARTMENT)
                .treePath("/1/")
                .treeLevel(1)
                .build();

            assertFalse(orgUnit.isAncestorOf(null));
        }
    }

    @Nested
    @DisplayName("Builder测试")
    class BuilderTest {

        @Test
        @DisplayName("使用Builder构建完整对象")
        void shouldBuildOrgUnitWithAllFields() {
            List<Long> deputyLeaderIds = Arrays.asList(200L, 300L);

            OrgUnit orgUnit = OrgUnit.builder()
                .id(1L)
                .unitCode("DEPT001")
                .unitName("信息技术系")
                .unitType(OrgUnitType.DEPARTMENT)
                .parentId(null)
                .treePath("/1/")
                .treeLevel(1)
                .leaderId(100L)
                .deputyLeaderIds(deputyLeaderIds)
                .sortOrder(1)
                .enabled(true)
                .createdBy(1L)
                .build();

            assertEquals(1L, orgUnit.getId());
            assertEquals("DEPT001", orgUnit.getUnitCode());
            assertEquals(100L, orgUnit.getLeaderId());
            assertEquals(2, orgUnit.getDeputyLeaderIds().size());
        }
    }
}
