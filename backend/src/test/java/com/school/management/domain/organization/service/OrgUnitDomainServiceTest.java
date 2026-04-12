package com.school.management.domain.organization.service;

import com.school.management.domain.organization.model.OrgUnit;
import com.school.management.domain.organization.repository.OrgUnitRepository;
import com.school.management.domain.organization.repository.OrgUnitTypeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * OrgUnitDomainService 回归测试 —
 * 主要覆盖 splitOrgUnit 的越权拆分修复 (commit 5b3f3d59)，
 * 以及 mergeOrgUnits 的基础校验。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("组织单元领域服务测试")
class OrgUnitDomainServiceTest {

    @Mock
    OrgUnitRepository orgUnitRepository;

    @Mock
    OrgUnitTypeRepository orgUnitTypeRepository;

    @InjectMocks
    OrgUnitDomainService service;

    // ==================== splitOrgUnit ====================

    @Test
    @DisplayName("拆分: 指定子组织不属于源组织时应抛异常 (回归: 5b3f3d59)")
    void splitOrgUnit_whenChildBelongsToOtherParent_shouldThrow() {
        // Source is dept A (id=1, root). The "child" supplied actually belongs to a different parent (id=2).
        OrgUnit source = OrgUnit.create("A", "部门A", "DEPARTMENT", null, 1L);
        ReflectionTestUtils.setField(source, "id", 1L);

        OrgUnit unrelatedChild = OrgUnit.create("X", "其他组", "TEAM", 2L, 1L);
        ReflectionTestUtils.setField(unrelatedChild, "id", 99L);

        when(orgUnitRepository.findById(1L)).thenReturn(Optional.of(source));
        when(orgUnitRepository.existsByUnitCode("NEW_A")).thenReturn(false);
        when(orgUnitRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(orgUnitRepository.findById(99L)).thenReturn(Optional.of(unrelatedChild));

        OrgUnitDomainService.SplitSpec spec = new OrgUnitDomainService.SplitSpec(
            "NEW_A", "新部门A", List.of(99L));

        assertThatThrownBy(() -> service.splitOrgUnit(1L, List.of(spec), "拆分", 1L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("不属于源组织");
    }

    @Test
    @DisplayName("拆分: 源组织不存在时应抛异常")
    void splitOrgUnit_whenSourceNotFound_shouldThrow() {
        when(orgUnitRepository.findById(999L)).thenReturn(Optional.empty());

        OrgUnitDomainService.SplitSpec spec = new OrgUnitDomainService.SplitSpec(
            "NEW", "新单元", List.of());

        assertThatThrownBy(() -> service.splitOrgUnit(999L, List.of(spec), "拆分", 1L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Source org unit not found");
    }

    @Test
    @DisplayName("拆分: 源组织已撤销时应抛异常")
    void splitOrgUnit_whenSourceDissolved_shouldThrow() {
        OrgUnit source = OrgUnit.create("A", "部门A", "DEPARTMENT", null, 1L);
        ReflectionTestUtils.setField(source, "id", 1L);
        source.dissolve("已撤销", 1L);

        when(orgUnitRepository.findById(1L)).thenReturn(Optional.of(source));

        OrgUnitDomainService.SplitSpec spec = new OrgUnitDomainService.SplitSpec(
            "NEW_A", "新部门A", List.of());

        assertThatThrownBy(() -> service.splitOrgUnit(1L, List.of(spec), "拆分", 1L))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("dissolved");
    }

    @Test
    @DisplayName("拆分: 新单元编码已存在时应抛异常")
    void splitOrgUnit_whenNewCodeAlreadyExists_shouldThrow() {
        OrgUnit source = OrgUnit.create("A", "部门A", "DEPARTMENT", null, 1L);
        ReflectionTestUtils.setField(source, "id", 1L);

        when(orgUnitRepository.findById(1L)).thenReturn(Optional.of(source));
        when(orgUnitRepository.existsByUnitCode("DUP")).thenReturn(true);

        OrgUnitDomainService.SplitSpec spec = new OrgUnitDomainService.SplitSpec(
            "DUP", "新单元", List.of());

        assertThatThrownBy(() -> service.splitOrgUnit(1L, List.of(spec), "拆分", 1L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("already exists");
    }

    // ==================== mergeOrgUnits ====================

    @Test
    @DisplayName("合并: 源与目标相同时应抛异常")
    void mergeOrgUnits_sourceEqualsTarget_shouldThrow() {
        assertThatThrownBy(() -> service.mergeOrgUnits(1L, 1L, "合并", 1L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Cannot merge an org unit into itself");
    }

    @Test
    @DisplayName("合并: 源组织不存在时应抛异常")
    void mergeOrgUnits_whenSourceNotFound_shouldThrow() {
        when(orgUnitRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.mergeOrgUnits(1L, 2L, "合并", 1L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Source org unit not found");
    }

    @Test
    @DisplayName("合并: 源组织已撤销时应抛异常")
    void mergeOrgUnits_whenSourceAlreadyDissolved_shouldThrow() {
        OrgUnit source = OrgUnit.create("A", "部门A", "DEPARTMENT", null, 1L);
        ReflectionTestUtils.setField(source, "id", 1L);
        source.dissolve("已撤销", 1L);

        OrgUnit target = OrgUnit.create("B", "部门B", "DEPARTMENT", null, 1L);
        ReflectionTestUtils.setField(target, "id", 2L);

        when(orgUnitRepository.findById(1L)).thenReturn(Optional.of(source));
        when(orgUnitRepository.findById(2L)).thenReturn(Optional.of(target));

        assertThatThrownBy(() -> service.mergeOrgUnits(1L, 2L, "合并", 1L))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("already dissolved");
    }

    @Test
    @DisplayName("合并: 源是目标祖先时应抛异常 (环依赖)")
    void mergeOrgUnits_whenSourceIsAncestorOfTarget_shouldThrow() {
        OrgUnit source = OrgUnit.builder()
            .id(1L)
            .unitCode("A")
            .unitName("部门A")
            .unitType("DEPARTMENT")
            .treePath("/1/")
            .treeLevel(1)
            .build();

        OrgUnit target = OrgUnit.builder()
            .id(2L)
            .unitCode("B")
            .unitName("部门B")
            .unitType("DEPARTMENT")
            .parentId(1L)
            .treePath("/1/2/")
            .treeLevel(2)
            .build();

        when(orgUnitRepository.findById(1L)).thenReturn(Optional.of(source));
        when(orgUnitRepository.findById(2L)).thenReturn(Optional.of(target));

        assertThatThrownBy(() -> service.mergeOrgUnits(1L, 2L, "合并", 1L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("ancestor");
    }

    // ==================== moveOrgUnit ====================

    @Test
    @DisplayName("移动: 将组织移到自身下应抛异常")
    void moveOrgUnit_whenNewParentIsSelf_shouldThrow() {
        OrgUnit orgUnit = OrgUnit.create("A", "部门A", "DEPARTMENT", null, 1L);
        ReflectionTestUtils.setField(orgUnit, "id", 1L);

        assertThatThrownBy(() -> service.moveOrgUnit(orgUnit, 1L, 1L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("under itself");
    }

    // ==================== canDelete ====================

    @Test
    @DisplayName("可删除判定: 无子组织返回 true")
    void canDelete_whenNoChildren_shouldReturnTrue() {
        OrgUnit orgUnit = OrgUnit.create("A", "部门A", "DEPARTMENT", null, 1L);
        ReflectionTestUtils.setField(orgUnit, "id", 1L);

        when(orgUnitRepository.countByParentId(1L)).thenReturn(0L);

        assertThat(service.canDelete(orgUnit)).isTrue();
    }

    @Test
    @DisplayName("可删除判定: 有子组织返回 false")
    void canDelete_whenHasChildren_shouldReturnFalse() {
        OrgUnit orgUnit = OrgUnit.create("A", "部门A", "DEPARTMENT", null, 1L);
        ReflectionTestUtils.setField(orgUnit, "id", 1L);

        when(orgUnitRepository.countByParentId(1L)).thenReturn(3L);

        assertThat(service.canDelete(orgUnit)).isFalse();
    }
}
