package com.school.management.domain.rating.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Rating配置聚合根单元测试
 */
@DisplayName("评比配置聚合根测试")
class RatingConfigTest {

    private static final Long CHECK_PLAN_ID = 1L;
    private static final String RATING_NAME = "周优秀班级";
    private static final Long CREATED_BY = 100L;

    private RatingConfig config;

    @BeforeEach
    void setUp() {
        config = RatingConfig.create(
            CHECK_PLAN_ID,
            RATING_NAME,
            RatingPeriodType.WEEKLY,
            DivisionMethod.TOP_N,
            BigDecimal.TEN,
            CREATED_BY
        );
    }

    @Nested
    @DisplayName("创建配置测试")
    class CreateConfigTest {

        @Test
        @DisplayName("成功创建评比配置")
        void shouldCreateConfigSuccessfully() {
            assertNotNull(config);
            assertEquals(CHECK_PLAN_ID, config.getCheckPlanId());
            assertEquals(RATING_NAME, config.getRatingName());
            assertEquals(RatingPeriodType.WEEKLY, config.getPeriodType());
            assertEquals(DivisionMethod.TOP_N, config.getDivisionMethod());
            assertEquals(BigDecimal.TEN, config.getDivisionValue());
            assertTrue(config.isEnabled());
            assertFalse(config.getDomainEvents().isEmpty());
        }

        @Test
        @DisplayName("创建百分比划分配置")
        void shouldCreatePercentDivisionConfig() {
            RatingConfig percentConfig = RatingConfig.create(
                CHECK_PLAN_ID,
                "月度前20%",
                RatingPeriodType.MONTHLY,
                DivisionMethod.TOP_PERCENT,
                BigDecimal.valueOf(20),
                CREATED_BY
            );

            assertEquals(DivisionMethod.TOP_PERCENT, percentConfig.getDivisionMethod());
            assertEquals(BigDecimal.valueOf(20), percentConfig.getDivisionValue());
        }

        @Test
        @DisplayName("创建日评比配置")
        void shouldCreateDailyConfig() {
            RatingConfig dailyConfig = RatingConfig.create(
                CHECK_PLAN_ID,
                "今日之星",
                RatingPeriodType.DAILY,
                DivisionMethod.TOP_N,
                BigDecimal.valueOf(3),
                CREATED_BY
            );

            assertEquals(RatingPeriodType.DAILY, dailyConfig.getPeriodType());
        }
    }

    @Nested
    @DisplayName("更新配置测试")
    class UpdateConfigTest {

        @Test
        @DisplayName("更新评比名称")
        void shouldUpdateRatingName() {
            config.clearDomainEvents();

            config.updateBasicInfo("新名称", "新图标", "#FF0000", 1, "新描述");

            assertEquals("新名称", config.getRatingName());
            assertEquals("新图标", config.getIcon());
            assertEquals("#FF0000", config.getColor());
            assertEquals(1, config.getPriority());
            assertEquals("新描述", config.getDescription());
        }

        @Test
        @DisplayName("更新划分规则")
        void shouldUpdateDivisionRule() {
            config.updateDivisionRule(
                RatingPeriodType.MONTHLY,
                DivisionMethod.TOP_PERCENT,
                BigDecimal.valueOf(15)
            );

            assertEquals(RatingPeriodType.MONTHLY, config.getPeriodType());
            assertEquals(DivisionMethod.TOP_PERCENT, config.getDivisionMethod());
            assertEquals(BigDecimal.valueOf(15), config.getDivisionValue());
        }

        @Test
        @DisplayName("更新审批设置")
        void shouldUpdateApprovalSettings() {
            config.updateApprovalSettings(true, false);

            assertTrue(config.isRequireApproval());
            assertFalse(config.isAutoPublish());
        }
    }

    @Nested
    @DisplayName("启用/禁用测试")
    class EnableDisableTest {

        @Test
        @DisplayName("禁用配置")
        void shouldDisableConfig() {
            assertTrue(config.isEnabled());

            config.disable();

            assertFalse(config.isEnabled());
        }

        @Test
        @DisplayName("启用配置")
        void shouldEnableConfig() {
            config.disable();
            assertFalse(config.isEnabled());

            config.enable();

            assertTrue(config.isEnabled());
        }
    }

    @Nested
    @DisplayName("划分方法测试")
    class DivisionMethodTest {

        @Test
        @DisplayName("TOP_N正向划分")
        void shouldTestTopN() {
            assertEquals(DivisionMethod.TOP_N, config.getDivisionMethod());
            assertTrue(config.getDivisionMethod().isTopOriented());
            assertFalse(config.getDivisionMethod().isBottomOriented());
            assertFalse(config.getDivisionMethod().isPercentBased());
        }

        @Test
        @DisplayName("TOP_PERCENT百分比划分")
        void shouldTestTopPercent() {
            RatingConfig percentConfig = RatingConfig.create(
                CHECK_PLAN_ID,
                "前10%",
                RatingPeriodType.WEEKLY,
                DivisionMethod.TOP_PERCENT,
                BigDecimal.TEN,
                CREATED_BY
            );

            assertTrue(percentConfig.getDivisionMethod().isPercentBased());
            assertTrue(percentConfig.getDivisionMethod().isTopOriented());
        }

        @Test
        @DisplayName("BOTTOM_N后向划分")
        void shouldTestBottomN() {
            RatingConfig bottomConfig = RatingConfig.create(
                CHECK_PLAN_ID,
                "后5名",
                RatingPeriodType.WEEKLY,
                DivisionMethod.BOTTOM_N,
                BigDecimal.valueOf(5),
                CREATED_BY
            );

            assertTrue(bottomConfig.getDivisionMethod().isBottomOriented());
            assertFalse(bottomConfig.getDivisionMethod().isTopOriented());
        }
    }
}
