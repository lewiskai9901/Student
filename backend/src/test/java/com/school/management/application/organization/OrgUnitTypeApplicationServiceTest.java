package com.school.management.application.organization;

import com.school.management.application.shared.TypeTreeBuilder.TypeTreeNode;
import com.school.management.domain.organization.model.entity.OrgType;
import com.school.management.domain.organization.repository.OrgUnitTypeRepository;
import com.school.management.domain.shared.repository.EntityTypeConfigRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * OrgUnitTypeApplicationService 应用服务单测.
 *
 * 用 Mockito 隔离 OrgUnitTypeRepository / EntityTypeConfigRepository,
 * 验证组织类型创建/更新/删除/启停 + metadataSchema 校验 + 跨类型引用校验
 * + allowedChildTypeCodes → 子类型 parentTypeCode 同步的编排逻辑.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OrgUnitTypeApplicationService 应用服务")
class OrgUnitTypeApplicationServiceTest {

    @Mock OrgUnitTypeRepository orgUnitTypeRepository;
    @Mock EntityTypeConfigRepository entityTypeConfigRepository;

    @InjectMocks OrgUnitTypeApplicationService service;

    // ============================================================
    // helpers
    // ============================================================

    private OrgUnitTypeApplicationService.CreateOrgUnitTypeCommand createCmd(String code, String name) {
        OrgUnitTypeApplicationService.CreateOrgUnitTypeCommand cmd =
                new OrgUnitTypeApplicationService.CreateOrgUnitTypeCommand();
        cmd.setTypeCode(code);
        cmd.setTypeName(name);
        cmd.setCategory("FUNCTIONAL");
        return cmd;
    }

    private OrgType type(Long id, String code) {
        return OrgType.builder().id(id).typeCode(code).typeName(code + "名")
                .category("FUNCTIONAL").isEnabled(true).build();
    }

    // ============================================================
    @Nested
    @DisplayName("createOrgUnitType")
    class CreateTests {

        @Test
        @DisplayName("正常创建: 保存为非系统/启用, 并同步子类型 parentTypeCode")
        void shouldCreateAndSave() {
            OrgUnitTypeApplicationService.CreateOrgUnitTypeCommand cmd = createCmd("DEPT", "部门");
            when(orgUnitTypeRepository.existsByTypeCode("DEPT")).thenReturn(false);
            when(orgUnitTypeRepository.save(any(OrgType.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            OrgType result = service.createOrgUnitType(cmd);

            ArgumentCaptor<OrgType> captor = ArgumentCaptor.forClass(OrgType.class);
            verify(orgUnitTypeRepository).save(captor.capture());
            OrgType saved = captor.getValue();
            assertThat(saved.getTypeCode()).isEqualTo("DEPT");
            assertThat(saved.getTypeName()).isEqualTo("部门");
            assertThat(saved.isSystem()).isFalse();
            assertThat(saved.isEnabled()).isTrue();
            assertThat(result).isSameAs(saved);
        }

        @Test
        @DisplayName("typeCode 已存在: 抛 IllegalArgumentException")
        void shouldRejectDuplicateTypeCode() {
            OrgUnitTypeApplicationService.CreateOrgUnitTypeCommand cmd = createCmd("DEPT", "部门");
            when(orgUnitTypeRepository.existsByTypeCode("DEPT")).thenReturn(true);

            assertThatThrownBy(() -> service.createOrgUnitType(cmd))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("类型编码已存在");
            verify(orgUnitTypeRepository, never()).save(any());
        }

        @Test
        @DisplayName("父类型不存在: 抛 IllegalArgumentException")
        void shouldRejectMissingParentType() {
            OrgUnitTypeApplicationService.CreateOrgUnitTypeCommand cmd = createCmd("DEPT", "部门");
            cmd.setParentTypeCode("ROOT");
            when(orgUnitTypeRepository.existsByTypeCode("DEPT")).thenReturn(false);
            when(orgUnitTypeRepository.findByTypeCode("ROOT")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.createOrgUnitType(cmd))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("父类型不存在");
        }

        @Test
        @DisplayName("category 提供但 features 为空: 用 category 默认 features")
        void shouldUseCategoryDefaultFeaturesWhenNull() {
            OrgUnitTypeApplicationService.CreateOrgUnitTypeCommand cmd = createCmd("GRP", "成员组");
            cmd.setCategory("GROUP");
            cmd.setFeatures(null);
            when(orgUnitTypeRepository.existsByTypeCode("GRP")).thenReturn(false);
            when(orgUnitTypeRepository.save(any(OrgType.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            OrgType result = service.createOrgUnitType(cmd);

            // GROUP 默认 features 含 memberManagement=true
            assertThat(result.getFeatures()).isNotNull();
            assertThat(result.getFeatures().get("memberManagement")).isTrue();
            assertThat(result.getFeatures().get("attendance")).isTrue();
        }

        @Test
        @DisplayName("非法 category 字符串: 忽略, features 保持 null")
        void shouldIgnoreInvalidCategoryForDefaultFeatures() {
            OrgUnitTypeApplicationService.CreateOrgUnitTypeCommand cmd = createCmd("X", "X");
            cmd.setCategory("NOT_A_CATEGORY");
            cmd.setFeatures(null);
            when(orgUnitTypeRepository.existsByTypeCode("X")).thenReturn(false);
            when(orgUnitTypeRepository.save(any(OrgType.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            OrgType result = service.createOrgUnitType(cmd);

            assertThat(result.getFeatures()).isNull();
        }

        @Test
        @DisplayName("提供 allowedChildTypeCodes: 同步子类型 parentTypeCode 并保存")
        void shouldSyncChildParentTypeCodes() {
            OrgUnitTypeApplicationService.CreateOrgUnitTypeCommand cmd = createCmd("DEPT", "部门");
            cmd.setAllowedChildTypeCodes(List.of("TEAM"));
            when(orgUnitTypeRepository.existsByTypeCode("DEPT")).thenReturn(false);
            when(orgUnitTypeRepository.save(any(OrgType.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            OrgType child = type(2L, "TEAM"); // parentTypeCode 为 null
            when(orgUnitTypeRepository.findByTypeCode("TEAM")).thenReturn(Optional.of(child));

            service.createOrgUnitType(cmd);

            // child 的 parentTypeCode 被设置后再次 save
            assertThat(child.getParentTypeCode()).isEqualTo("DEPT");
            verify(orgUnitTypeRepository).save(child);
        }

        @Test
        @DisplayName("子类型 parentTypeCode 已正确: 不重复保存")
        void shouldNotResaveChildWithCorrectParent() {
            OrgUnitTypeApplicationService.CreateOrgUnitTypeCommand cmd = createCmd("DEPT", "部门");
            cmd.setAllowedChildTypeCodes(List.of("TEAM"));
            when(orgUnitTypeRepository.existsByTypeCode("DEPT")).thenReturn(false);
            when(orgUnitTypeRepository.save(any(OrgType.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            OrgType child = OrgType.builder().id(2L).typeCode("TEAM").typeName("组")
                    .parentTypeCode("DEPT").build();
            when(orgUnitTypeRepository.findByTypeCode("TEAM")).thenReturn(Optional.of(child));

            service.createOrgUnitType(cmd);

            // 只保存了 DEPT 自身, 不保存 child
            verify(orgUnitTypeRepository, times(1)).save(any(OrgType.class));
        }

        @Test
        @DisplayName("metadataSchema 缺 fields 数组: 抛 IllegalArgumentException")
        void shouldRejectSchemaWithoutFields() {
            OrgUnitTypeApplicationService.CreateOrgUnitTypeCommand cmd = createCmd("DEPT", "部门");
            cmd.setMetadataSchema("{\"name\":\"x\"}");
            when(orgUnitTypeRepository.existsByTypeCode("DEPT")).thenReturn(false);

            assertThatThrownBy(() -> service.createOrgUnitType(cmd))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("fields 数组");
        }

        @Test
        @DisplayName("metadataSchema fields 不是数组: 抛 IllegalArgumentException")
        void shouldRejectSchemaFieldsNotArray() {
            OrgUnitTypeApplicationService.CreateOrgUnitTypeCommand cmd = createCmd("DEPT", "部门");
            cmd.setMetadataSchema("{\"fields\":\"x\"}");
            when(orgUnitTypeRepository.existsByTypeCode("DEPT")).thenReturn(false);

            assertThatThrownBy(() -> service.createOrgUnitType(cmd))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("fields 必须是数组");
        }

        @Test
        @DisplayName("metadataSchema 字段缺 key/label/type: 抛 IllegalArgumentException")
        void shouldRejectSchemaFieldMissingProps() {
            OrgUnitTypeApplicationService.CreateOrgUnitTypeCommand cmd = createCmd("DEPT", "部门");
            cmd.setMetadataSchema("{\"fields\":[{\"key\":\"a\"}]}");
            when(orgUnitTypeRepository.existsByTypeCode("DEPT")).thenReturn(false);

            assertThatThrownBy(() -> service.createOrgUnitType(cmd))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("key、label、type");
        }

        @Test
        @DisplayName("metadataSchema 字段 key 非法 (数字开头): 抛 IllegalArgumentException")
        void shouldRejectSchemaInvalidKey() {
            OrgUnitTypeApplicationService.CreateOrgUnitTypeCommand cmd = createCmd("DEPT", "部门");
            cmd.setMetadataSchema("{\"fields\":[{\"key\":\"1bad\",\"label\":\"L\",\"type\":\"text\"}]}");
            when(orgUnitTypeRepository.existsByTypeCode("DEPT")).thenReturn(false);

            assertThatThrownBy(() -> service.createOrgUnitType(cmd))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("不能以数字开头");
        }

        @Test
        @DisplayName("metadataSchema 不支持的字段类型: 抛 IllegalArgumentException")
        void shouldRejectSchemaUnsupportedType() {
            OrgUnitTypeApplicationService.CreateOrgUnitTypeCommand cmd = createCmd("DEPT", "部门");
            cmd.setMetadataSchema("{\"fields\":[{\"key\":\"a\",\"label\":\"L\",\"type\":\"json\"}]}");
            when(orgUnitTypeRepository.existsByTypeCode("DEPT")).thenReturn(false);

            assertThatThrownBy(() -> service.createOrgUnitType(cmd))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("不支持的字段类型");
        }

        @Test
        @DisplayName("metadataSchema JSON 格式错误: 抛 IllegalArgumentException")
        void shouldRejectMalformedSchemaJson() {
            OrgUnitTypeApplicationService.CreateOrgUnitTypeCommand cmd = createCmd("DEPT", "部门");
            cmd.setMetadataSchema("{not valid json");
            when(orgUnitTypeRepository.existsByTypeCode("DEPT")).thenReturn(false);

            assertThatThrownBy(() -> service.createOrgUnitType(cmd))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("JSON 格式错误");
        }

        @Test
        @DisplayName("合法 metadataSchema: 通过校验正常创建")
        void shouldAcceptValidSchema() {
            OrgUnitTypeApplicationService.CreateOrgUnitTypeCommand cmd = createCmd("DEPT", "部门");
            cmd.setMetadataSchema("{\"fields\":[{\"key\":\"phone\",\"label\":\"电话\",\"type\":\"text\"}]}");
            when(orgUnitTypeRepository.existsByTypeCode("DEPT")).thenReturn(false);
            when(orgUnitTypeRepository.save(any(OrgType.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            OrgType result = service.createOrgUnitType(cmd);

            assertThat(result.getMetadataSchema()).contains("phone");
        }

        @Test
        @DisplayName("引用的用户类型不存在: 抛 IllegalArgumentException")
        void shouldRejectMissingUserTypeReference() {
            OrgUnitTypeApplicationService.CreateOrgUnitTypeCommand cmd = createCmd("DEPT", "部门");
            cmd.setDefaultUserTypeCodes(List.of("STUDENT"));
            when(orgUnitTypeRepository.existsByTypeCode("DEPT")).thenReturn(false);
            when(entityTypeConfigRepository.existsByTypeCode("USER", "STUDENT")).thenReturn(false);

            assertThatThrownBy(() -> service.createOrgUnitType(cmd))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("引用的用户类型不存在");
        }

        @Test
        @DisplayName("引用的场所类型不存在: 抛 IllegalArgumentException")
        void shouldRejectMissingPlaceTypeReference() {
            OrgUnitTypeApplicationService.CreateOrgUnitTypeCommand cmd = createCmd("DEPT", "部门");
            cmd.setDefaultPlaceTypeCodes(List.of("CLASSROOM"));
            when(orgUnitTypeRepository.existsByTypeCode("DEPT")).thenReturn(false);
            when(entityTypeConfigRepository.existsByTypeCode("PLACE", "CLASSROOM")).thenReturn(false);

            assertThatThrownBy(() -> service.createOrgUnitType(cmd))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("引用的场所类型不存在");
        }

        @Test
        @DisplayName("跨类型引用全部存在: 通过校验")
        void shouldAcceptValidCrossReferences() {
            OrgUnitTypeApplicationService.CreateOrgUnitTypeCommand cmd = createCmd("DEPT", "部门");
            cmd.setDefaultUserTypeCodes(List.of("STUDENT"));
            cmd.setDefaultPlaceTypeCodes(List.of("CLASSROOM"));
            when(orgUnitTypeRepository.existsByTypeCode("DEPT")).thenReturn(false);
            when(entityTypeConfigRepository.existsByTypeCode("USER", "STUDENT")).thenReturn(true);
            when(entityTypeConfigRepository.existsByTypeCode("PLACE", "CLASSROOM")).thenReturn(true);
            when(orgUnitTypeRepository.save(any(OrgType.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            OrgType result = service.createOrgUnitType(cmd);

            assertThat(result.getDefaultUserTypeCodes()).containsExactly("STUDENT");
            verify(entityTypeConfigRepository).existsByTypeCode("USER", "STUDENT");
            verify(entityTypeConfigRepository).existsByTypeCode("PLACE", "CLASSROOM");
        }

        @Test
        @DisplayName("引用列表含空白字符串: 跳过不校验")
        void shouldSkipBlankReferenceCodes() {
            OrgUnitTypeApplicationService.CreateOrgUnitTypeCommand cmd = createCmd("DEPT", "部门");
            cmd.setDefaultUserTypeCodes(java.util.Arrays.asList("", "  "));
            when(orgUnitTypeRepository.existsByTypeCode("DEPT")).thenReturn(false);
            when(orgUnitTypeRepository.save(any(OrgType.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            service.createOrgUnitType(cmd);

            verify(entityTypeConfigRepository, never()).existsByTypeCode(anyString(), anyString());
        }
    }

    // ============================================================
    @Nested
    @DisplayName("updateOrgUnitType")
    class UpdateTests {

        @Test
        @DisplayName("更新基本信息: typeName/description/icon 被改, 保存返回")
        void shouldUpdateBasicInfo() {
            OrgType existing = type(1L, "DEPT");
            when(orgUnitTypeRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(orgUnitTypeRepository.save(any(OrgType.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            OrgUnitTypeApplicationService.UpdateOrgUnitTypeCommand cmd =
                    new OrgUnitTypeApplicationService.UpdateOrgUnitTypeCommand();
            cmd.setTypeName("新名称");
            cmd.setDescription("新描述");
            cmd.setIcon("icon-new");

            OrgType result = service.updateOrgUnitType(1L, cmd);

            assertThat(result.getTypeName()).isEqualTo("新名称");
            assertThat(result.getDescription()).isEqualTo("新描述");
            assertThat(result.getIcon()).isEqualTo("icon-new");
        }

        @Test
        @DisplayName("组织类型不存在: 抛 IllegalArgumentException")
        void shouldRejectUpdateNotFound() {
            when(orgUnitTypeRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateOrgUnitType(99L,
                    new OrgUnitTypeApplicationService.UpdateOrgUnitTypeCommand()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("组织类型不存在");
        }

        @Test
        @DisplayName("null 字段保留原值")
        void shouldKeepOriginalWhenFieldNull() {
            OrgType existing = OrgType.builder().id(1L).typeCode("DEPT").typeName("原名")
                    .description("原描述").icon("原图标").category("FUNCTIONAL").build();
            when(orgUnitTypeRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(orgUnitTypeRepository.save(any(OrgType.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            OrgType result = service.updateOrgUnitType(1L,
                    new OrgUnitTypeApplicationService.UpdateOrgUnitTypeCommand());

            assertThat(result.getTypeName()).isEqualTo("原名");
            assertThat(result.getDescription()).isEqualTo("原描述");
            assertThat(result.getIcon()).isEqualTo("原图标");
        }

        @Test
        @DisplayName("更新 category 和 features")
        void shouldUpdateCategoryAndFeatures() {
            OrgType existing = type(1L, "DEPT");
            when(orgUnitTypeRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(orgUnitTypeRepository.save(any(OrgType.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            OrgUnitTypeApplicationService.UpdateOrgUnitTypeCommand cmd =
                    new OrgUnitTypeApplicationService.UpdateOrgUnitTypeCommand();
            cmd.setCategory("GROUP");
            cmd.setFeatures(Map.of("memberManagement", true));

            OrgType result = service.updateOrgUnitType(1L, cmd);

            assertThat(result.getCategory()).isEqualTo("GROUP");
            assertThat(result.getFeatures().get("memberManagement")).isTrue();
        }

        @Test
        @DisplayName("更新 metadataSchema: 校验通过后写入")
        void shouldUpdateMetadataSchema() {
            OrgType existing = type(1L, "DEPT");
            when(orgUnitTypeRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(orgUnitTypeRepository.save(any(OrgType.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            OrgUnitTypeApplicationService.UpdateOrgUnitTypeCommand cmd =
                    new OrgUnitTypeApplicationService.UpdateOrgUnitTypeCommand();
            cmd.setMetadataSchema("{\"fields\":[{\"key\":\"k\",\"label\":\"L\",\"type\":\"number\"}]}");

            OrgType result = service.updateOrgUnitType(1L, cmd);

            assertThat(result.getMetadataSchema()).contains("number");
        }

        @Test
        @DisplayName("更新 allowedChildTypeCodes: 同步子类型 parentTypeCode")
        void shouldSyncChildrenOnHierarchyUpdate() {
            OrgType existing = type(1L, "DEPT");
            when(orgUnitTypeRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(orgUnitTypeRepository.save(any(OrgType.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            OrgType child = type(2L, "TEAM");
            when(orgUnitTypeRepository.findByTypeCode("TEAM")).thenReturn(Optional.of(child));

            OrgUnitTypeApplicationService.UpdateOrgUnitTypeCommand cmd =
                    new OrgUnitTypeApplicationService.UpdateOrgUnitTypeCommand();
            cmd.setAllowedChildTypeCodes(List.of("TEAM"));

            service.updateOrgUnitType(1L, cmd);

            assertThat(child.getParentTypeCode()).isEqualTo("DEPT");
            verify(orgUnitTypeRepository).save(child);
        }

        @Test
        @DisplayName("未提供 allowedChildTypeCodes: 不触发子类型同步")
        void shouldNotSyncChildrenWhenChildCodesNull() {
            OrgType existing = type(1L, "DEPT");
            when(orgUnitTypeRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(orgUnitTypeRepository.save(any(OrgType.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            service.updateOrgUnitType(1L,
                    new OrgUnitTypeApplicationService.UpdateOrgUnitTypeCommand());

            verify(orgUnitTypeRepository, never()).findByTypeCode(anyString());
        }

        @Test
        @DisplayName("更新 sortOrder")
        void shouldUpdateSortOrder() {
            OrgType existing = type(1L, "DEPT");
            when(orgUnitTypeRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(orgUnitTypeRepository.save(any(OrgType.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            OrgUnitTypeApplicationService.UpdateOrgUnitTypeCommand cmd =
                    new OrgUnitTypeApplicationService.UpdateOrgUnitTypeCommand();
            cmd.setSortOrder(42);

            OrgType result = service.updateOrgUnitType(1L, cmd);

            assertThat(result.getSortOrder()).isEqualTo(42);
        }
    }

    // ============================================================
    @Nested
    @DisplayName("deleteOrgUnitType")
    class DeleteTests {

        @Test
        @DisplayName("无引用无子类型: 正常删除")
        void shouldDelete() {
            OrgType existing = type(1L, "DEPT");
            when(orgUnitTypeRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(orgUnitTypeRepository.isTypeInUse("DEPT")).thenReturn(false);
            when(orgUnitTypeRepository.findByParentTypeCode("DEPT")).thenReturn(List.of());

            service.deleteOrgUnitType(1L);

            verify(orgUnitTypeRepository).deleteById(1L);
        }

        @Test
        @DisplayName("组织类型不存在: 抛 IllegalArgumentException")
        void shouldRejectDeleteNotFound() {
            when(orgUnitTypeRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deleteOrgUnitType(99L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("组织类型不存在");
            verify(orgUnitTypeRepository, never()).deleteById(any());
        }

        @Test
        @DisplayName("类型已被组织单元使用: 拒绝删除")
        void shouldRejectDeleteWhenInUse() {
            OrgType existing = type(1L, "DEPT");
            when(orgUnitTypeRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(orgUnitTypeRepository.isTypeInUse("DEPT")).thenReturn(true);

            assertThatThrownBy(() -> service.deleteOrgUnitType(1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("已被组织单元使用");
            verify(orgUnitTypeRepository, never()).deleteById(any());
        }

        @Test
        @DisplayName("类型下存在子类型: 拒绝删除")
        void shouldRejectDeleteWhenHasChildren() {
            OrgType existing = type(1L, "DEPT");
            when(orgUnitTypeRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(orgUnitTypeRepository.isTypeInUse("DEPT")).thenReturn(false);
            when(orgUnitTypeRepository.findByParentTypeCode("DEPT"))
                    .thenReturn(List.of(type(2L, "TEAM")));

            assertThatThrownBy(() -> service.deleteOrgUnitType(1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("存在子类型");
            verify(orgUnitTypeRepository, never()).deleteById(any());
        }
    }

    // ============================================================
    @Nested
    @DisplayName("toggleStatus")
    class ToggleStatusTests {

        @Test
        @DisplayName("enabled=true: 调 enable() 并保存")
        void shouldEnable() {
            OrgType existing = OrgType.builder().id(1L).typeCode("DEPT").isEnabled(false).build();
            when(orgUnitTypeRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(orgUnitTypeRepository.save(any(OrgType.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            OrgType result = service.toggleStatus(1L, true);

            assertThat(result.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("enabled=false: 调 disable() 并保存")
        void shouldDisable() {
            OrgType existing = OrgType.builder().id(1L).typeCode("DEPT").isEnabled(true).build();
            when(orgUnitTypeRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(orgUnitTypeRepository.save(any(OrgType.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            OrgType result = service.toggleStatus(1L, false);

            assertThat(result.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("组织类型不存在: 抛 IllegalArgumentException")
        void shouldRejectToggleNotFound() {
            when(orgUnitTypeRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.toggleStatus(99L, true))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("组织类型不存在");
        }
    }

    // ============================================================
    @Nested
    @DisplayName("查询方法")
    class QueryTests {

        @Test
        @DisplayName("getAllOrgUnitTypes 透传 repository.findAll")
        void shouldGetAll() {
            List<OrgType> all = List.of(type(1L, "A"), type(2L, "B"));
            when(orgUnitTypeRepository.findAll()).thenReturn(all);

            assertThat(service.getAllOrgUnitTypes()).isEqualTo(all);
        }

        @Test
        @DisplayName("getEnabledOrgUnitTypes 透传 repository.findAllEnabled")
        void shouldGetEnabled() {
            List<OrgType> enabled = List.of(type(1L, "A"));
            when(orgUnitTypeRepository.findAllEnabled()).thenReturn(enabled);

            assertThat(service.getEnabledOrgUnitTypes()).isEqualTo(enabled);
        }

        @Test
        @DisplayName("getInspectableTypes 用 inspectionTarget feature 查询")
        void shouldGetInspectableTypes() {
            List<OrgType> result = List.of(type(1L, "BRANCH"));
            when(orgUnitTypeRepository.findByFeature("inspectionTarget")).thenReturn(result);

            assertThat(service.getInspectableTypes()).isEqualTo(result);
            verify(orgUnitTypeRepository).findByFeature("inspectionTarget");
        }

        @Test
        @DisplayName("getOrgUnitTypeTree 按 parentTypeCode 构建树")
        void shouldBuildTree() {
            OrgType root = OrgType.builder().id(1L).typeCode("ROOT").parentTypeCode(null).build();
            OrgType child = OrgType.builder().id(2L).typeCode("DEPT").parentTypeCode("ROOT").build();
            when(orgUnitTypeRepository.findAll()).thenReturn(List.of(root, child));

            List<TypeTreeNode<OrgType>> tree = service.getOrgUnitTypeTree();

            assertThat(tree).hasSize(1);
            assertThat(tree.get(0).getData().getTypeCode()).isEqualTo("ROOT");
            assertThat(tree.get(0).getChildren()).hasSize(1);
            assertThat(tree.get(0).getChildren().get(0).getData().getTypeCode()).isEqualTo("DEPT");
        }

        @Test
        @DisplayName("getCategories 返回全部 OrgCategory 枚举值含默认 features")
        void shouldGetCategories() {
            List<OrgUnitTypeApplicationService.OrgCategoryDTO> result = service.getCategories();

            assertThat(result).hasSize(5);
            assertThat(result).extracting(OrgUnitTypeApplicationService.OrgCategoryDTO::getCode)
                    .contains("ROOT", "BRANCH", "FUNCTIONAL", "GROUP", "CONTAINER");
            OrgUnitTypeApplicationService.OrgCategoryDTO group = result.stream()
                    .filter(c -> "GROUP".equals(c.getCode())).findFirst().orElseThrow();
            assertThat(group.getLabel()).isEqualTo("成员组");
            assertThat(group.getDefaultFeatures().get("memberManagement")).isTrue();
        }

        @Test
        @DisplayName("getOrgUnitTypeById 存在: 返回实体")
        void shouldGetById() {
            OrgType t = type(1L, "DEPT");
            when(orgUnitTypeRepository.findById(1L)).thenReturn(Optional.of(t));

            assertThat(service.getOrgUnitTypeById(1L)).isSameAs(t);
        }

        @Test
        @DisplayName("getOrgUnitTypeById 不存在: 抛 IllegalArgumentException")
        void shouldRejectGetByIdNotFound() {
            when(orgUnitTypeRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getOrgUnitTypeById(99L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("组织类型不存在");
        }

        @Test
        @DisplayName("getOrgUnitTypeByCode 存在: 返回实体")
        void shouldGetByCode() {
            OrgType t = type(1L, "DEPT");
            when(orgUnitTypeRepository.findByTypeCode("DEPT")).thenReturn(Optional.of(t));

            assertThat(service.getOrgUnitTypeByCode("DEPT")).isSameAs(t);
        }

        @Test
        @DisplayName("getOrgUnitTypeByCode 不存在: 抛 IllegalArgumentException")
        void shouldRejectGetByCodeNotFound() {
            when(orgUnitTypeRepository.findByTypeCode("X")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getOrgUnitTypeByCode("X"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("组织类型不存在");
        }
    }
}
