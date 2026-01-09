package com.school.management.application.inspection;

import com.school.management.domain.inspection.model.*;
import com.school.management.domain.inspection.repository.AppealRepository;
import com.school.management.domain.inspection.repository.InspectionRecordRepository;
import com.school.management.domain.inspection.repository.InspectionTemplateRepository;
import com.school.management.domain.shared.event.DomainEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for InspectionApplicationService.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("InspectionApplicationService 测试")
class InspectionApplicationServiceTest {

    @Mock
    private InspectionTemplateRepository templateRepository;

    @Mock
    private InspectionRecordRepository recordRepository;

    @Mock
    private AppealRepository appealRepository;

    @Mock
    private DomainEventPublisher eventPublisher;

    @InjectMocks
    private InspectionApplicationService service;

    @Nested
    @DisplayName("创建检查模板")
    class CreateTemplateTests {

        @Test
        @DisplayName("应成功创建新模板")
        void shouldCreateTemplateSuccessfully() {
            // Given
            CreateTemplateCommand command = CreateTemplateCommand.builder()
                    .templateCode("TPL-001")
                    .templateName("宿舍卫生检查模板")
                    .description("用于每日宿舍卫生检查")
                    .scope(TemplateScope.GLOBAL)
                    .createdBy(1L)
                    .build();

            when(templateRepository.existsByTemplateCode("TPL-001")).thenReturn(false);
            when(templateRepository.save(any(InspectionTemplate.class)))
                    .thenAnswer(invocation -> {
                        InspectionTemplate template = invocation.getArgument(0);
                        // 模拟设置ID
                        return template;
                    });

            // When
            InspectionTemplate result = service.createTemplate(command);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTemplateCode()).isEqualTo("TPL-001");
            assertThat(result.getTemplateName()).isEqualTo("宿舍卫生检查模板");
            assertThat(result.getScope()).isEqualTo(TemplateScope.GLOBAL);
            assertThat(result.getStatus()).isEqualTo(TemplateStatus.DRAFT);

            verify(templateRepository).existsByTemplateCode("TPL-001");
            verify(templateRepository).save(any(InspectionTemplate.class));
        }

        @Test
        @DisplayName("模板编码重复时应抛出异常")
        void shouldThrowExceptionWhenTemplateCodeExists() {
            // Given
            CreateTemplateCommand command = CreateTemplateCommand.builder()
                    .templateCode("TPL-001")
                    .templateName("重复模板")
                    .scope(TemplateScope.GLOBAL)
                    .createdBy(1L)
                    .build();

            when(templateRepository.existsByTemplateCode("TPL-001")).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> service.createTemplate(command))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Template code already exists");

            verify(templateRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("添加检查类别")
    class AddCategoryTests {

        private InspectionTemplate existingTemplate;

        @BeforeEach
        void setUp() {
            existingTemplate = InspectionTemplate.create(
                    "TPL-001",
                    "测试模板",
                    "描述",
                    TemplateScope.GLOBAL,
                    null,
                    1L
            );
        }

        @Test
        @DisplayName("应成功添加类别到模板")
        void shouldAddCategoryToTemplate() {
            // Given
            Long templateId = 1L;
            AddCategoryCommand command = AddCategoryCommand.builder()
                    .categoryCode("CAT-001")
                    .categoryName("地面卫生")
                    .baseScore(20)
                    .sortOrder(1)
                    .build();

            when(templateRepository.findById(templateId)).thenReturn(Optional.of(existingTemplate));
            when(templateRepository.save(any(InspectionTemplate.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            InspectionTemplate result = service.addCategoryToTemplate(templateId, command);

            // Then
            assertThat(result.getCategories()).hasSize(1);
            assertThat(result.getCategories().get(0).getCategoryCode()).isEqualTo("CAT-001");
            assertThat(result.getCategories().get(0).getCategoryName()).isEqualTo("地面卫生");

            verify(templateRepository).save(any(InspectionTemplate.class));
        }

        @Test
        @DisplayName("模板不存在时应抛出异常")
        void shouldThrowExceptionWhenTemplateNotFound() {
            // Given
            Long templateId = 999L;
            AddCategoryCommand command = AddCategoryCommand.builder()
                    .categoryCode("CAT-001")
                    .categoryName("类别")
                    .baseScore(10)
                    .sortOrder(1)
                    .build();

            when(templateRepository.findById(templateId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> service.addCategoryToTemplate(templateId, command))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Template not found");
        }
    }

    @Nested
    @DisplayName("发布模板")
    class PublishTemplateTests {

        @Test
        @DisplayName("应成功发布草稿模板")
        void shouldPublishDraftTemplate() {
            // Given
            Long templateId = 1L;
            InspectionTemplate template = InspectionTemplate.create(
                    "TPL-001",
                    "测试模板",
                    "描述",
                    TemplateScope.GLOBAL,
                    null,
                    1L
            );
            // 添加类别以满足发布要求
            InspectionCategory category = InspectionCategory.create(
                    templateId,
                    "CAT-001",
                    "测试类别",
                    100,
                    1
            );
            template.addCategory(category);

            when(templateRepository.findById(templateId)).thenReturn(Optional.of(template));
            when(templateRepository.save(any(InspectionTemplate.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            InspectionTemplate result = service.publishTemplate(templateId);

            // Then
            assertThat(result.getStatus()).isEqualTo(TemplateStatus.PUBLISHED);
            verify(templateRepository).save(any(InspectionTemplate.class));
        }
    }

    @Nested
    @DisplayName("获取模板")
    class GetTemplateTests {

        @Test
        @DisplayName("应返回存在的模板")
        void shouldReturnExistingTemplate() {
            // Given
            Long templateId = 1L;
            InspectionTemplate template = InspectionTemplate.create(
                    "TPL-001",
                    "测试模板",
                    "描述",
                    TemplateScope.GLOBAL,
                    null,
                    1L
            );

            when(templateRepository.findById(templateId)).thenReturn(Optional.of(template));

            // When
            Optional<InspectionTemplate> result = service.getTemplate(templateId);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getTemplateCode()).isEqualTo("TPL-001");
        }

        @Test
        @DisplayName("模板不存在时应返回空")
        void shouldReturnEmptyWhenTemplateNotExists() {
            // Given
            Long templateId = 999L;
            when(templateRepository.findById(templateId)).thenReturn(Optional.empty());

            // When
            Optional<InspectionTemplate> result = service.getTemplate(templateId);

            // Then
            assertThat(result).isEmpty();
        }
    }
}
