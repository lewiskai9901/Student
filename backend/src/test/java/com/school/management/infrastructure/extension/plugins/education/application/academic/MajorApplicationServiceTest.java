package com.school.management.infrastructure.extension.plugins.education.application.academic;

import com.school.management.infrastructure.extension.plugins.education.application.academic.command.CreateMajorCommand;
import com.school.management.infrastructure.extension.plugins.education.application.academic.command.UpdateMajorCommand;
import com.school.management.infrastructure.extension.plugins.education.application.academic.query.MajorDTO;
import com.school.management.infrastructure.extension.plugins.education.domain.academic.model.Major;
import com.school.management.infrastructure.extension.plugins.education.domain.academic.model.MajorStatus;
import com.school.management.infrastructure.extension.plugins.education.domain.academic.repository.MajorRepository;
import com.school.management.infrastructure.extension.plugins.education.infrastructure.persistence.academic.MajorDirectionPersistenceMapper;
import com.school.management.infrastructure.extension.plugins.education.infrastructure.persistence.academic.MajorPersistenceMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * MajorApplicationService 测试 — 验证创建/更新/删除/查询专业的业务编排
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MajorApplicationService 测试")
class MajorApplicationServiceTest {

    @Mock
    private MajorRepository majorRepository;

    @Mock
    private MajorPersistenceMapper majorMapper;

    @Mock
    private MajorDirectionPersistenceMapper directionMapper;

    @InjectMocks
    private MajorApplicationService service;

    private Major newMajor() {
        return Major.create("CS001", "计算机应用", 100L, "desc", 1L);
    }

    @Nested
    @DisplayName("创建专业")
    class CreateMajorTests {

        @Test
        @DisplayName("应成功创建专业")
        void shouldCreateMajor() {
            CreateMajorCommand cmd = new CreateMajorCommand();
            cmd.setMajorCode("CS001");
            cmd.setMajorName("计算机应用");
            cmd.setOrgUnitId(100L);
            cmd.setDescription("desc");
            cmd.setCreatedBy(1L);
            cmd.setMajorStatus("ENROLLING");

            when(majorRepository.existsByMajorCode("CS001")).thenReturn(false);
            when(majorRepository.save(any(Major.class))).thenAnswer(inv -> inv.getArgument(0));

            MajorDTO result = service.createMajor(cmd);

            assertThat(result).isNotNull();
            assertThat(result.getMajorCode()).isEqualTo("CS001");
            assertThat(result.getMajorName()).isEqualTo("计算机应用");
            verify(majorRepository).save(any(Major.class));
        }

        @Test
        @DisplayName("专业编码已存在抛异常")
        void shouldFailWhenCodeExists() {
            CreateMajorCommand cmd = new CreateMajorCommand();
            cmd.setMajorCode("CS001");
            cmd.setMajorName("计算机应用");
            cmd.setOrgUnitId(100L);

            when(majorRepository.existsByMajorCode("CS001")).thenReturn(true);

            assertThatThrownBy(() -> service.createMajor(cmd))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("已存在");
            verify(majorRepository, never()).save(any());
        }

        @Test
        @DisplayName("非法 majorStatus 不抛错, 保持默认")
        void shouldIgnoreInvalidMajorStatusEnum() {
            CreateMajorCommand cmd = new CreateMajorCommand();
            cmd.setMajorCode("CS001");
            cmd.setMajorName("计算机应用");
            cmd.setOrgUnitId(100L);
            cmd.setCreatedBy(1L);
            cmd.setMajorStatus("INVALID_VALUE"); // 不抛 — 保留默认 ENROLLING

            when(majorRepository.existsByMajorCode("CS001")).thenReturn(false);
            when(majorRepository.save(any(Major.class))).thenAnswer(inv -> inv.getArgument(0));

            MajorDTO result = service.createMajor(cmd);

            assertThat(result).isNotNull();
            assertThat(result.getMajorStatus()).isEqualTo("ENROLLING");
        }
    }

    @Nested
    @DisplayName("更新专业")
    class UpdateMajorTests {

        @Test
        @DisplayName("应成功更新专业")
        void shouldUpdate() {
            Major major = newMajor();
            UpdateMajorCommand cmd = new UpdateMajorCommand();
            cmd.setMajorName("新名称");
            cmd.setDescription("新描述");
            cmd.setStatus(1);
            cmd.setUpdatedBy(99L);

            when(majorRepository.findById(1L)).thenReturn(Optional.of(major));
            when(majorRepository.save(any(Major.class))).thenAnswer(inv -> inv.getArgument(0));

            MajorDTO result = service.updateMajor(1L, cmd);

            assertThat(result.getMajorName()).isEqualTo("新名称");
            verify(majorRepository).save(major);
        }

        @Test
        @DisplayName("专业不存在抛异常")
        void shouldFailWhenNotFound() {
            UpdateMajorCommand cmd = new UpdateMajorCommand();
            when(majorRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateMajor(999L, cmd))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("专业不存在");
        }

        @Test
        @DisplayName("status=0 时禁用专业")
        void shouldDisableWhenStatusZero() {
            Major major = newMajor();
            UpdateMajorCommand cmd = new UpdateMajorCommand();
            cmd.setStatus(0);

            when(majorRepository.findById(1L)).thenReturn(Optional.of(major));
            when(majorRepository.save(any(Major.class))).thenAnswer(inv -> inv.getArgument(0));

            service.updateMajor(1L, cmd);

            assertThat(major.isEnabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("删除专业")
    class DeleteMajorTests {

        @Test
        @DisplayName("应成功删除")
        void shouldDeleteMajor() {
            Major major = newMajor();
            when(majorRepository.findById(1L)).thenReturn(Optional.of(major));

            service.deleteMajor(1L);

            verify(majorRepository).delete(major);
        }

        @Test
        @DisplayName("专业不存在时抛异常")
        void shouldFailWhenDeleteNotFound() {
            when(majorRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deleteMajor(999L))
                    .isInstanceOf(IllegalArgumentException.class);
            verify(majorRepository, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("查询专业")
    class QueryTests {

        @Test
        @DisplayName("应根据 ID 查询")
        void shouldGetById() {
            Major major = newMajor();
            when(majorRepository.findById(1L)).thenReturn(Optional.of(major));

            MajorDTO dto = service.getMajor(1L);
            assertThat(dto.getMajorCode()).isEqualTo("CS001");
            assertThat(dto.getMajorName()).isEqualTo("计算机应用");
        }

        @Test
        @DisplayName("ID 不存在抛异常")
        void shouldFailGetByIdNotFound() {
            when(majorRepository.findById(999L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.getMajor(999L))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("getMajorsByOrgUnit 应转换 DTO 列表")
        void shouldGetByOrgUnit() {
            Major m1 = newMajor();
            when(majorRepository.findByOrgUnitId(100L)).thenReturn(java.util.List.of(m1));

            assertThat(service.getMajorsByOrgUnit(100L)).hasSize(1);
        }

        @Test
        @DisplayName("getAllEnabledMajors 应返回启用专业")
        void shouldGetAllEnabled() {
            when(majorRepository.findAllEnabled()).thenReturn(java.util.List.of(newMajor()));
            assertThat(service.getAllEnabledMajors()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("DTO 映射")
    class DtoMappingTests {

        @Test
        @DisplayName("启用专业的 DTO statusName 应为 启用")
        void shouldMapEnabledStatusName() {
            Major major = newMajor();
            when(majorRepository.findById(1L)).thenReturn(Optional.of(major));
            MajorDTO dto = service.getMajor(1L);
            assertThat(dto.getStatus()).isEqualTo(1);
            assertThat(dto.getStatusName()).isEqualTo("启用");
        }

        @Test
        @DisplayName("禁用专业的 DTO statusName 应为 禁用")
        void shouldMapDisabledStatusName() {
            Major major = newMajor();
            major.disable();
            when(majorRepository.findById(1L)).thenReturn(Optional.of(major));
            MajorDTO dto = service.getMajor(1L);
            assertThat(dto.getStatus()).isEqualTo(0);
            assertThat(dto.getStatusName()).isEqualTo("禁用");
        }

        @Test
        @DisplayName("majorStatus 应序列化为 name")
        void shouldMapMajorStatusName() {
            Major major = newMajor();
            major.updateVocationalInfo(null, null, null, null, null, null, MajorStatus.SUSPENDED);
            when(majorRepository.findById(1L)).thenReturn(Optional.of(major));
            MajorDTO dto = service.getMajor(1L);
            assertThat(dto.getMajorStatus()).isEqualTo("SUSPENDED");
        }
    }
}
