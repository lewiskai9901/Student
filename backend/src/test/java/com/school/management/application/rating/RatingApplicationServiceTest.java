package com.school.management.application.rating;

import com.school.management.domain.rating.model.DivisionMethod;
import com.school.management.domain.rating.model.RatingConfig;
import com.school.management.domain.rating.model.RatingPeriodType;
import com.school.management.domain.rating.model.RatingResult;
import com.school.management.domain.rating.model.RatingResultStatus;
import com.school.management.domain.rating.repository.RatingConfigRepository;
import com.school.management.domain.rating.repository.RatingResultRepository;
import com.school.management.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Rating应用服务测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("评比应用服务测试")
class RatingApplicationServiceTest {

    @Mock
    private RatingConfigRepository configRepository;

    @Mock
    private RatingResultRepository resultRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private RatingApplicationService service;

    private RatingConfig testConfig;
    private RatingResult testResult;

    @BeforeEach
    void setUp() {
        testConfig = RatingConfig.create(
            1L, "周优秀班级", RatingPeriodType.WEEKLY,
            DivisionMethod.TOP_N, BigDecimal.TEN, 100L
        );

        testResult = RatingResult.create(
            1L, 1L, 100L, "高一(1)班",
            RatingPeriodType.WEEKLY,
            LocalDate.now().minusDays(7), LocalDate.now()
        );
    }

    @Nested
    @DisplayName("配置管理测试")
    class ConfigManagementTest {

        @Test
        @DisplayName("获取所有配置")
        void shouldGetAllConfigs() {
            when(configRepository.findAll()).thenReturn(List.of(testConfig));

            List<RatingConfig> configs = service.getAllConfigs();

            assertEquals(1, configs.size());
            verify(configRepository).findAll();
        }

        @Test
        @DisplayName("根据ID获取配置")
        void shouldGetConfigById() {
            when(configRepository.findById(1L)).thenReturn(Optional.of(testConfig));

            Optional<RatingConfig> result = service.getConfigById(1L);

            assertTrue(result.isPresent());
            assertEquals("周优秀班级", result.get().getRatingName());
        }

        @Test
        @DisplayName("创建配置")
        void shouldCreateConfig() {
            CreateRatingConfigCommand command = new CreateRatingConfigCommand();
            command.setCheckPlanId(1L);
            command.setRatingName("新配置");
            command.setPeriodType(RatingPeriodType.MONTHLY);
            command.setDivisionMethod(DivisionMethod.TOP_PERCENT);
            command.setDivisionValue(BigDecimal.valueOf(20));
            command.setCreatedBy(100L);

            when(configRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            RatingConfig result = service.createConfig(command);

            assertNotNull(result);
            assertEquals("新配置", result.getRatingName());
            verify(configRepository).save(any());
            verify(eventPublisher, atLeastOnce()).publishEvent(any());
        }

        @Test
        @DisplayName("更新配置")
        void shouldUpdateConfig() {
            when(configRepository.findById(1L)).thenReturn(Optional.of(testConfig));
            when(configRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            UpdateRatingConfigCommand command = new UpdateRatingConfigCommand();
            command.setRatingName("更新后名称");
            command.setIcon("new-icon");

            RatingConfig result = service.updateConfig(1L, command);

            assertEquals("更新后名称", result.getRatingName());
            verify(configRepository).save(any());
        }

        @Test
        @DisplayName("删除配置")
        void shouldDeleteConfig() {
            when(configRepository.findById(1L)).thenReturn(Optional.of(testConfig));
            doNothing().when(configRepository).delete(any());

            service.deleteConfig(1L);

            verify(configRepository).delete(testConfig);
        }

        @Test
        @DisplayName("删除不存在的配置抛出异常")
        void shouldThrowWhenDeletingNonExistentConfig() {
            when(configRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(BusinessException.class, () -> service.deleteConfig(999L));
        }
    }

    @Nested
    @DisplayName("结果管理测试")
    class ResultManagementTest {

        @Test
        @DisplayName("提交审核")
        void shouldSubmitForApproval() {
            when(resultRepository.findById(1L)).thenReturn(Optional.of(testResult));
            when(resultRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.submitForApproval(1L, 100L);

            assertEquals(RatingResultStatus.PENDING_APPROVAL, testResult.getStatus());
            verify(resultRepository).save(testResult);
        }

        @Test
        @DisplayName("审核通过")
        void shouldApproveResult() {
            testResult.submitForApproval(100L);
            when(resultRepository.findById(1L)).thenReturn(Optional.of(testResult));
            when(resultRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.approveResult(1L, 200L);

            assertEquals(RatingResultStatus.APPROVED, testResult.getStatus());
        }

        @Test
        @DisplayName("审核驳回")
        void shouldRejectResult() {
            testResult.submitForApproval(100L);
            when(resultRepository.findById(1L)).thenReturn(Optional.of(testResult));
            when(resultRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.rejectResult(1L, 200L, "数据有误");

            assertEquals(RatingResultStatus.REJECTED, testResult.getStatus());
        }

        @Test
        @DisplayName("发布结果")
        void shouldPublishResult() {
            testResult.submitForApproval(100L);
            testResult.approve(200L);
            when(resultRepository.findById(1L)).thenReturn(Optional.of(testResult));
            when(resultRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.publishResult(1L, 200L);

            assertEquals(RatingResultStatus.PUBLISHED, testResult.getStatus());
        }

        @Test
        @DisplayName("撤销发布")
        void shouldRevokeResult() {
            testResult.submitForApproval(100L);
            testResult.approve(200L);
            testResult.publish(200L);
            when(resultRepository.findById(1L)).thenReturn(Optional.of(testResult));
            when(resultRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.revokeResult(1L, 200L, "发现错误");

            assertEquals(RatingResultStatus.REVOKED, testResult.getStatus());
        }
    }

    @Nested
    @DisplayName("按检查计划查询测试")
    class QueryByCheckPlanTest {

        @Test
        @DisplayName("获取检查计划的所有配置")
        void shouldGetConfigsByCheckPlan() {
            when(configRepository.findByCheckPlanId(1L)).thenReturn(List.of(testConfig));

            List<RatingConfig> configs = service.getConfigsByCheckPlanId(1L);

            assertEquals(1, configs.size());
            verify(configRepository).findByCheckPlanId(1L);
        }

        @Test
        @DisplayName("获取检查计划的所有结果")
        void shouldGetResultsByCheckPlan() {
            when(resultRepository.findByCheckPlanId(1L)).thenReturn(List.of(testResult));

            List<RatingResult> results = service.getResultsByCheckPlanId(1L);

            assertEquals(1, results.size());
            verify(resultRepository).findByCheckPlanId(1L);
        }
    }
}
