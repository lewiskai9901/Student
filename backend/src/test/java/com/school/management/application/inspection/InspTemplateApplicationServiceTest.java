package com.school.management.application.inspection;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.school.management.common.PageResult;
import com.school.management.domain.inspection.model.execution.TargetType;
import com.school.management.domain.inspection.model.template.ItemType;
import com.school.management.domain.inspection.model.template.TemplateItem;
import com.school.management.domain.inspection.model.template.TemplateSection;
import com.school.management.domain.inspection.model.template.TemplateStatus;
import com.school.management.domain.inspection.model.template.TemplateVersion;
import com.school.management.domain.inspection.repository.CalculationRuleRepository;
import com.school.management.domain.inspection.repository.GradeBandRepository;
import com.school.management.domain.inspection.repository.InspProjectRepository;
import com.school.management.domain.inspection.repository.ScoringProfileRepository;
import com.school.management.domain.inspection.repository.TemplateItemRepository;
import com.school.management.domain.inspection.repository.TemplateSectionRepository;
import com.school.management.domain.inspection.repository.TemplateVersionRepository;
import com.school.management.infrastructure.event.SpringDomainEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * InspTemplateApplicationService 应用服务单测.
 *
 * 用 Mockito 隔离 repository / 事件发布, 验证根分区 CRUD / 生命周期 /
 * 发布快照 / 深度复制 / 版本管理 / 导出 的编排逻辑.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("InspTemplateApplicationService 根分区应用服务")
class InspTemplateApplicationServiceTest {

    @Mock TemplateSectionRepository sectionRepository;
    @Mock TemplateVersionRepository versionRepository;
    @Mock TemplateItemRepository itemRepository;
    @Mock SpringDomainEventPublisher eventPublisher;
    @Mock ScoringProfileRepository scoringProfileRepository;
    @Mock GradeBandRepository gradeBandRepository;
    @Mock CalculationRuleRepository calculationRuleRepository;
    @Mock InspProjectRepository inspProjectRepository;

    ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    InspTemplateApplicationService service;

    @BeforeEach
    void setUp() {
        service = new InspTemplateApplicationService(
                sectionRepository, versionRepository, itemRepository,
                eventPublisher, objectMapper, scoringProfileRepository,
                gradeBandRepository, calculationRuleRepository, inspProjectRepository);
    }

    // ---- helpers ----

    private TemplateSection root(Long id, TemplateStatus status) {
        TemplateSection s = TemplateSection.reconstruct(TemplateSection.builder()
                .sectionCode("SEC-ROOT").sectionName("根分区")
                .parentSectionId(null).templateId(null)
                .status(status != null ? status : TemplateStatus.DRAFT)
                .createdBy(1L));
        s.setId(id);
        return s;
    }

    private TemplateSection child(Long id, Long parentId) {
        TemplateSection s = TemplateSection.reconstruct(TemplateSection.builder()
                .sectionCode("SEC-CHILD-" + id).sectionName("子分区" + id)
                .parentSectionId(parentId).templateId(null)
                .createdBy(1L));
        s.setId(id);
        return s;
    }

    private TemplateItem item(Long id, Long sectionId) {
        TemplateItem it = TemplateItem.create(sectionId, "IT-" + id, "检查项" + id,
                ItemType.TEXT, 1L);
        it.setId(id);
        return it;
    }

    // ============================================================
    @Nested
    @DisplayName("createRootSection — 创建根分区")
    class CreateRootTests {
        @Test
        @DisplayName("仅基本信息: 只保存一次, code 以 SEC- 开头")
        void shouldCreateWithBasicInfoOnly() {
            when(sectionRepository.save(any(TemplateSection.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            TemplateSection result = service.createRootSection(
                    "模板A", null, null, null, null, 99L);

            assertThat(result.getSectionName()).isEqualTo("模板A");
            assertThat(result.getSectionCode()).startsWith("SEC-");
            assertThat(result.getStatus()).isEqualTo(TemplateStatus.DRAFT);
            // 没有可选字段 / targetType => 只保存一次
            verify(sectionRepository, times(1)).save(any(TemplateSection.class));
        }

        @Test
        @DisplayName("带 description/catalog/tags: 触发二次保存并写入字段")
        void shouldCreateWithOptionalInfo() {
            when(sectionRepository.save(any(TemplateSection.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            TemplateSection result = service.createRootSection(
                    "模板B", "描述", 7L, "[\"安全\"]", null, 99L);

            assertThat(result.getDescription()).isEqualTo("描述");
            assertThat(result.getCatalogId()).isEqualTo(7L);
            assertThat(result.getTags()).isEqualTo("[\"安全\"]");
            verify(sectionRepository, times(2)).save(any(TemplateSection.class));
        }

        @Test
        @DisplayName("合法 targetType: 解析为枚举并设置")
        void shouldSetValidTargetType() {
            when(sectionRepository.save(any(TemplateSection.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            TemplateSection result = service.createRootSection(
                    "模板C", null, null, null, "ORG", 99L);

            assertThat(result.getTargetType()).isEqualTo(TargetType.ORG);
            verify(sectionRepository, times(2)).save(any(TemplateSection.class));
        }

        @Test
        @DisplayName("非法 targetType: 忽略, 不抛异常, targetType 保持 null")
        void shouldIgnoreInvalidTargetType() {
            when(sectionRepository.save(any(TemplateSection.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            TemplateSection result = service.createRootSection(
                    "模板D", null, null, null, "NOT_A_TYPE", 99L);

            assertThat(result.getTargetType()).isNull();
        }

        @Test
        @DisplayName("空白 targetType: 跳过, 不触发额外保存")
        void shouldSkipBlankTargetType() {
            when(sectionRepository.save(any(TemplateSection.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            service.createRootSection("模板E", null, null, null, "   ", 99L);

            verify(sectionRepository, times(1)).save(any(TemplateSection.class));
        }

        @Test
        @DisplayName("4 参兼容重载: 委托到 5 参版本, targetType=null")
        void shouldDelegate4ArgOverload() {
            when(sectionRepository.save(any(TemplateSection.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            TemplateSection result = service.createRootSection(
                    "模板F", "d", 1L, "t", 99L);

            assertThat(result.getTargetType()).isNull();
            assertThat(result.getDescription()).isEqualTo("d");
        }
    }

    // ============================================================
    @Nested
    @DisplayName("getRootSection — 查询根分区")
    class GetRootTests {
        @Test
        @DisplayName("是根分区: 返回 present")
        void shouldReturnRoot() {
            when(sectionRepository.findById(1L)).thenReturn(Optional.of(root(1L, null)));
            assertThat(service.getRootSection(1L)).isPresent();
        }

        @Test
        @DisplayName("ID 是子分区: filter 过滤掉, 返回 empty")
        void shouldFilterOutNonRoot() {
            when(sectionRepository.findById(2L)).thenReturn(Optional.of(child(2L, 1L)));
            assertThat(service.getRootSection(2L)).isEmpty();
        }

        @Test
        @DisplayName("不存在: 返回 empty")
        void shouldReturnEmptyWhenMissing() {
            when(sectionRepository.findById(9L)).thenReturn(Optional.empty());
            assertThat(service.getRootSection(9L)).isEmpty();
        }
    }

    // ============================================================
    @Nested
    @DisplayName("listRootSections — 分页查询")
    class ListRootTests {
        @Test
        @DisplayName("正确计算 offset 并组装 PageResult")
        void shouldComputeOffsetAndPage() {
            when(sectionRepository.findRootSectionsPaged(20, 10, "DRAFT", 3L, "kw"))
                    .thenReturn(List.of(root(1L, TemplateStatus.DRAFT)));
            when(sectionRepository.countRootSections("DRAFT", 3L, "kw")).thenReturn(45);

            PageResult<TemplateSection> result = service.listRootSections(
                    3, 10, TemplateStatus.DRAFT, 3L, "kw");

            assertThat(result.getRecords()).hasSize(1);
            assertThat(result.getTotal()).isEqualTo(45L);
            assertThat(result.getCurrent()).isEqualTo(3L);
            assertThat(result.getSize()).isEqualTo(10L);
        }

        @Test
        @DisplayName("status 为 null: 传给 repository 的状态串也为 null")
        void shouldPassNullStatus() {
            when(sectionRepository.findRootSectionsPaged(0, 10, null, null, null))
                    .thenReturn(List.of());
            when(sectionRepository.countRootSections(null, null, null)).thenReturn(0);

            PageResult<TemplateSection> result = service.listRootSections(
                    1, 10, null, null, null);

            assertThat(result.getRecords()).isEmpty();
            assertThat(result.getTotal()).isZero();
        }
    }

    // ============================================================
    @Nested
    @DisplayName("getRootSectionUsage — 使用计数")
    class UsageTests {
        @Test
        @DisplayName("透传到 inspProjectRepository.countByRootSectionIds")
        void shouldDelegateUsage() {
            when(inspProjectRepository.countByRootSectionIds(List.of(1L, 2L)))
                    .thenReturn(Map.of(1L, 3, 2L, 0));

            Map<Long, Integer> result = service.getRootSectionUsage(List.of(1L, 2L));

            assertThat(result).containsEntry(1L, 3).containsEntry(2L, 0);
        }
    }

    // ============================================================
    @Nested
    @DisplayName("updateRootSection — 更新基本信息")
    class UpdateRootTests {
        @Test
        @DisplayName("更新名称/描述并保存")
        void shouldUpdate() {
            TemplateSection r = root(1L, TemplateStatus.DRAFT);
            when(sectionRepository.findById(1L)).thenReturn(Optional.of(r));
            when(sectionRepository.save(any(TemplateSection.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            TemplateSection result = service.updateRootSection(
                    1L, "新名", "新描述", 5L, "[\"卫生\"]", 88L);

            assertThat(result.getSectionName()).isEqualTo("新名");
            assertThat(result.getDescription()).isEqualTo("新描述");
            assertThat(result.getUpdatedBy()).isEqualTo(88L);
        }

        @Test
        @DisplayName("根分区不存在抛 IllegalArgumentException")
        void shouldRejectMissing() {
            when(sectionRepository.findById(9L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.updateRootSection(9L, "x", null, null, null, 1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("根分区不存在");
        }

        @Test
        @DisplayName("传入子分区 ID 抛 IllegalArgumentException")
        void shouldRejectNonRoot() {
            when(sectionRepository.findById(2L)).thenReturn(Optional.of(child(2L, 1L)));
            assertThatThrownBy(() -> service.updateRootSection(2L, "x", null, null, null, 1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("不是根分区");
        }
    }

    // ============================================================
    @Nested
    @DisplayName("deleteRootSection — 级联删除")
    class DeleteRootTests {
        @Test
        @DisplayName("DRAFT 根分区: 逆序删子孙 items+section, 最后删根")
        void shouldCascadeDelete() {
            TemplateSection r = root(1L, TemplateStatus.DRAFT);
            TemplateSection c1 = child(2L, 1L);
            TemplateSection c2 = child(3L, 2L);
            when(sectionRepository.findById(1L)).thenReturn(Optional.of(r));
            when(sectionRepository.findDescendants(1L)).thenReturn(List.of(c1, c2));

            service.deleteRootSection(1L);

            // 子孙逆序: c2 先于 c1
            ArgumentCaptor<Long> sectDel = ArgumentCaptor.forClass(Long.class);
            verify(sectionRepository, times(3)).deleteById(sectDel.capture());
            assertThat(sectDel.getAllValues()).containsExactly(3L, 2L, 1L);
            // items 也按 3,2,1 删
            verify(itemRepository).deleteBySectionId(3L);
            verify(itemRepository).deleteBySectionId(2L);
            verify(itemRepository).deleteBySectionId(1L);
        }

        @Test
        @DisplayName("PUBLISHED 根分区: 拒绝删除")
        void shouldRejectPublished() {
            when(sectionRepository.findById(1L))
                    .thenReturn(Optional.of(root(1L, TemplateStatus.PUBLISHED)));
            assertThatThrownBy(() -> service.deleteRootSection(1L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("需先弃用");
        }

        @Test
        @DisplayName("无子孙: 仍删根本身的 items 和根")
        void shouldDeleteRootWithNoDescendants() {
            when(sectionRepository.findById(1L))
                    .thenReturn(Optional.of(root(1L, TemplateStatus.DRAFT)));
            when(sectionRepository.findDescendants(1L)).thenReturn(List.of());

            service.deleteRootSection(1L);

            verify(itemRepository).deleteBySectionId(1L);
            verify(sectionRepository).deleteById(1L);
        }
    }

    // ============================================================
    @Nested
    @DisplayName("publishRootSection — 发布快照")
    class PublishTests {
        @Test
        @DisplayName("无子分区: 拒绝发布")
        void shouldRejectWhenNoChildSections() {
            when(sectionRepository.findById(1L))
                    .thenReturn(Optional.of(root(1L, TemplateStatus.DRAFT)));
            when(sectionRepository.findDescendants(1L)).thenReturn(List.of());

            assertThatThrownBy(() -> service.publishRootSection(1L, 9L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("至少需要一个子分区");
        }

        @Test
        @DisplayName("有子分区但无任何检查项: 拒绝发布")
        void shouldRejectWhenNoItems() {
            when(sectionRepository.findById(1L))
                    .thenReturn(Optional.of(root(1L, TemplateStatus.DRAFT)));
            when(sectionRepository.findDescendants(1L)).thenReturn(List.of(child(2L, 1L)));
            when(itemRepository.findBySectionId(anyLong())).thenReturn(List.of());

            assertThatThrownBy(() -> service.publishRootSection(1L, 9L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("至少需要一个检查项");
        }

        @Test
        @DisplayName("成功发布: 保存版本 + 发布领域事件 + 状态变 PUBLISHED")
        void shouldPublishSuccessfully() {
            TemplateSection r = root(1L, TemplateStatus.DRAFT);
            TemplateSection c = child(2L, 1L);
            when(sectionRepository.findById(1L)).thenReturn(Optional.of(r));
            when(sectionRepository.findDescendants(1L)).thenReturn(List.of(c));
            when(itemRepository.findBySectionId(1L)).thenReturn(List.of(item(10L, 1L)));
            when(itemRepository.findBySectionId(2L)).thenReturn(List.of(item(11L, 2L)));
            when(scoringProfileRepository.findBySectionId(anyLong()))
                    .thenReturn(Optional.empty());
            when(sectionRepository.save(any(TemplateSection.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            TemplateVersion version = service.publishRootSection(1L, 9L);

            assertThat(version).isNotNull();
            assertThat(version.getVersion()).isEqualTo(1);
            assertThat(r.getStatus()).isEqualTo(TemplateStatus.PUBLISHED);
            verify(versionRepository).save(any(TemplateVersion.class));
            verify(eventPublisher).publish(any());
        }

        @Test
        @DisplayName("发布快照包含 scoring 配置 (有 profile 时取 gradeBands/rules)")
        void shouldIncludeScoringSnapshot() {
            TemplateSection r = root(1L, TemplateStatus.DRAFT);
            TemplateSection c = child(2L, 1L);
            when(sectionRepository.findById(1L)).thenReturn(Optional.of(r));
            when(sectionRepository.findDescendants(1L)).thenReturn(List.of(c));
            when(itemRepository.findBySectionId(1L)).thenReturn(List.of(item(10L, 1L)));
            when(itemRepository.findBySectionId(2L)).thenReturn(List.of());
            when(scoringProfileRepository.findBySectionId(1L))
                    .thenReturn(Optional.of(com.school.management.domain.inspection.model.scoring
                            .ScoringProfile.reconstruct(
                            com.school.management.domain.inspection.model.scoring.ScoringProfile
                                    .builder().id(500L).sectionId(1L).createdBy(1L))));
            when(scoringProfileRepository.findBySectionId(2L)).thenReturn(Optional.empty());
            when(gradeBandRepository.findByScoringProfileId(500L)).thenReturn(List.of());
            when(calculationRuleRepository.findByScoringProfileIdOrderByPriority(500L))
                    .thenReturn(List.of());
            when(sectionRepository.save(any(TemplateSection.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            TemplateVersion version = service.publishRootSection(1L, 9L);

            assertThat(version).isNotNull();
            verify(gradeBandRepository).findByScoringProfileId(500L);
            verify(calculationRuleRepository).findByScoringProfileIdOrderByPriority(500L);
        }

        @Test
        @DisplayName("根分区不存在: 拒绝")
        void shouldRejectMissing() {
            when(sectionRepository.findById(9L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.publishRootSection(9L, 1L))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ============================================================
    @Nested
    @DisplayName("deprecate / archive — 生命周期")
    class LifecycleTests {
        @Test
        @DisplayName("deprecate: PUBLISHED → DEPRECATED")
        void shouldDeprecate() {
            TemplateSection r = root(1L, TemplateStatus.PUBLISHED);
            when(sectionRepository.findById(1L)).thenReturn(Optional.of(r));
            when(sectionRepository.save(any(TemplateSection.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            service.deprecateRootSection(1L);

            assertThat(r.getStatus()).isEqualTo(TemplateStatus.DEPRECATED);
            verify(sectionRepository).save(r);
        }

        @Test
        @DisplayName("deprecate 非 PUBLISHED: 抛 IllegalStateException")
        void shouldRejectDeprecateDraft() {
            when(sectionRepository.findById(1L))
                    .thenReturn(Optional.of(root(1L, TemplateStatus.DRAFT)));
            assertThatThrownBy(() -> service.deprecateRootSection(1L))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("archive: DEPRECATED → ARCHIVED")
        void shouldArchive() {
            TemplateSection r = root(1L, TemplateStatus.DEPRECATED);
            when(sectionRepository.findById(1L)).thenReturn(Optional.of(r));
            when(sectionRepository.save(any(TemplateSection.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            service.archiveRootSection(1L);

            assertThat(r.getStatus()).isEqualTo(TemplateStatus.ARCHIVED);
        }

        @Test
        @DisplayName("archive 草稿: 抛 IllegalStateException")
        void shouldRejectArchiveDraft() {
            when(sectionRepository.findById(1L))
                    .thenReturn(Optional.of(root(1L, TemplateStatus.DRAFT)));
            assertThatThrownBy(() -> service.archiveRootSection(1L))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    // ============================================================
    @Nested
    @DisplayName("duplicateRootSection — 深度复制")
    class DuplicateTests {
        @Test
        @DisplayName("复制根 + 子孙树 + 检查项, 名称带(副本)")
        void shouldDuplicateTree() {
            TemplateSection src = root(1L, TemplateStatus.PUBLISHED);
            TemplateSection c1 = child(2L, 1L);
            TemplateSection c2 = child(3L, 2L);
            when(sectionRepository.findById(1L)).thenReturn(Optional.of(src));
            when(sectionRepository.findDescendants(1L)).thenReturn(List.of(c1, c2));
            // save 给新分区分配自增 ID
            final long[] seq = {1000L};
            when(sectionRepository.save(any(TemplateSection.class))).thenAnswer(inv -> {
                TemplateSection s = inv.getArgument(0);
                if (s.getId() == null) s.setId(seq[0]++);
                return s;
            });
            when(itemRepository.findBySectionId(anyLong())).thenReturn(List.of());

            TemplateSection result = service.duplicateRootSection(1L, 77L);

            assertThat(result.getSectionName()).contains("(副本)");
            // 根保存 2 次 (创建 + updateInfo), 2 个子孙各 1 次
            verify(sectionRepository, times(4)).save(any(TemplateSection.class));
        }

        @Test
        @DisplayName("复制检查项: 对每个分区调用 copyItemsForSection")
        void shouldCopyItems() {
            TemplateSection src = root(1L, TemplateStatus.DRAFT);
            TemplateSection c1 = child(2L, 1L);
            when(sectionRepository.findById(1L)).thenReturn(Optional.of(src));
            when(sectionRepository.findDescendants(1L)).thenReturn(List.of(c1));
            final long[] seq = {1000L};
            when(sectionRepository.save(any(TemplateSection.class))).thenAnswer(inv -> {
                TemplateSection s = inv.getArgument(0);
                if (s.getId() == null) s.setId(seq[0]++);
                return s;
            });
            when(itemRepository.findBySectionId(1L)).thenReturn(List.of(item(10L, 1L)));
            when(itemRepository.findBySectionId(2L)).thenReturn(List.of());
            when(itemRepository.save(any(TemplateItem.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            service.duplicateRootSection(1L, 77L);

            // 根分区的 1 个 item 被复制保存
            verify(itemRepository, times(1)).save(any(TemplateItem.class));
        }

        @Test
        @DisplayName("源不是根分区: 抛 IllegalArgumentException")
        void shouldRejectNonRootSource() {
            when(sectionRepository.findById(2L)).thenReturn(Optional.of(child(2L, 1L)));
            assertThatThrownBy(() -> service.duplicateRootSection(2L, 1L))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ============================================================
    @Nested
    @DisplayName("listVersions / getVersion — 版本管理")
    class VersionTests {
        @Test
        @DisplayName("listVersions: 确认是根分区后透传 repository")
        void shouldListVersions() {
            when(sectionRepository.findById(1L))
                    .thenReturn(Optional.of(root(1L, TemplateStatus.PUBLISHED)));
            TemplateVersion v = TemplateVersion.reconstruct(
                    500L, 1L, 1, "{}", null, 1L, LocalDateTime.now());
            when(versionRepository.findByTemplateId(1L)).thenReturn(List.of(v));

            List<TemplateVersion> result = service.listVersions(1L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getVersion()).isEqualTo(1);
        }

        @Test
        @DisplayName("listVersions: 非根分区抛异常")
        void shouldRejectListVersionsNonRoot() {
            when(sectionRepository.findById(2L)).thenReturn(Optional.of(child(2L, 1L)));
            assertThatThrownBy(() -> service.listVersions(2L))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("getVersion: 透传 templateId + version")
        void shouldGetVersion() {
            when(sectionRepository.findById(1L))
                    .thenReturn(Optional.of(root(1L, TemplateStatus.PUBLISHED)));
            TemplateVersion v = TemplateVersion.reconstruct(
                    500L, 1L, 2, "{}", null, 1L, LocalDateTime.now());
            when(versionRepository.findByTemplateIdAndVersion(1L, 2)).thenReturn(Optional.of(v));

            Optional<TemplateVersion> result = service.getVersion(1L, 2);

            assertThat(result).isPresent();
            assertThat(result.get().getVersion()).isEqualTo(2);
        }

        @Test
        @DisplayName("getVersion: 版本不存在返回 empty")
        void shouldReturnEmptyVersion() {
            when(sectionRepository.findById(1L))
                    .thenReturn(Optional.of(root(1L, TemplateStatus.PUBLISHED)));
            when(versionRepository.findByTemplateIdAndVersion(1L, 9))
                    .thenReturn(Optional.empty());

            assertThat(service.getVersion(1L, 9)).isEmpty();
        }
    }

    // ============================================================
    @Nested
    @DisplayName("exportRootSection — 导出 JSON")
    class ExportTests {
        @Test
        @DisplayName("导出含 rootSection/sections/items/exportedAt 字段")
        void shouldExportJson() throws Exception {
            TemplateSection r = root(1L, TemplateStatus.PUBLISHED);
            TemplateSection c = child(2L, 1L);
            when(sectionRepository.findById(1L)).thenReturn(Optional.of(r));
            when(sectionRepository.findDescendants(1L)).thenReturn(List.of(c));
            when(itemRepository.findBySectionId(1L)).thenReturn(List.of(item(10L, 1L)));
            when(itemRepository.findBySectionId(2L)).thenReturn(List.of());

            String json = service.exportRootSection(1L);

            @SuppressWarnings("unchecked")
            Map<String, Object> parsed = objectMapper.readValue(json, Map.class);
            assertThat(parsed).containsKeys("rootSection", "sections", "items", "exportedAt");
            assertThat((List<?>) parsed.get("items")).hasSize(1);
        }

        @Test
        @DisplayName("非根分区: 抛 IllegalArgumentException")
        void shouldRejectExportNonRoot() {
            when(sectionRepository.findById(2L)).thenReturn(Optional.of(child(2L, 1L)));
            assertThatThrownBy(() -> service.exportRootSection(2L))
                    .isInstanceOf(IllegalArgumentException.class);
            verify(sectionRepository, never()).findDescendants(eq(2L));
        }
    }
}
