package com.school.management.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 特性开关配置测试
 */
@DisplayName("特性开关配置测试")
class FeatureToggleConfigTest {

    private FeatureToggleConfig config;

    @BeforeEach
    void setUp() {
        config = new FeatureToggleConfig();
        config.init();
    }

    @Nested
    @DisplayName("默认配置测试")
    class DefaultConfigTest {

        @Test
        @DisplayName("默认特性开关已初始化")
        void shouldInitializeDefaultToggles() {
            assertNotNull(config.getToggles());
            assertTrue(config.getToggles().containsKey(FeatureToggleConfig.V2_RATING_API));
            assertTrue(config.getToggles().containsKey(FeatureToggleConfig.V2_TASK_API));
            assertTrue(config.getToggles().containsKey(FeatureToggleConfig.V2_INSPECTION_API));
        }

        @Test
        @DisplayName("V2 API默认启用")
        void shouldEnableV2ApisByDefault() {
            assertTrue(config.isEnabled(FeatureToggleConfig.V2_RATING_API));
            assertTrue(config.isEnabled(FeatureToggleConfig.V2_TASK_API));
            assertTrue(config.isEnabled(FeatureToggleConfig.V2_INSPECTION_API));
        }

        @Test
        @DisplayName("事件溯源默认禁用")
        void shouldDisableEventSourcingByDefault() {
            assertFalse(config.isEnabled(FeatureToggleConfig.EVENT_SOURCING));
        }
    }

    @Nested
    @DisplayName("启用/禁用测试")
    class EnableDisableTest {

        @Test
        @DisplayName("禁用特性")
        void shouldDisableFeature() {
            assertTrue(config.isEnabled(FeatureToggleConfig.V2_RATING_API));

            config.disableFeature(FeatureToggleConfig.V2_RATING_API);

            assertFalse(config.isEnabled(FeatureToggleConfig.V2_RATING_API));
        }

        @Test
        @DisplayName("启用特性")
        void shouldEnableFeature() {
            config.disableFeature(FeatureToggleConfig.V2_RATING_API);
            assertFalse(config.isEnabled(FeatureToggleConfig.V2_RATING_API));

            config.enableFeature(FeatureToggleConfig.V2_RATING_API);

            assertTrue(config.isEnabled(FeatureToggleConfig.V2_RATING_API));
        }
    }

    @Nested
    @DisplayName("灰度发布测试")
    class RolloutTest {

        @Test
        @DisplayName("设置灰度比例")
        void shouldSetRolloutPercentage() {
            config.setRolloutPercentage(FeatureToggleConfig.V2_RATING_API, 50);

            assertEquals(50, config.getToggles().get(FeatureToggleConfig.V2_RATING_API).getRolloutPercentage());
        }

        @Test
        @DisplayName("灰度比例不超过100")
        void shouldClampRolloutPercentageToMax() {
            config.setRolloutPercentage(FeatureToggleConfig.V2_RATING_API, 150);

            assertEquals(100, config.getToggles().get(FeatureToggleConfig.V2_RATING_API).getRolloutPercentage());
        }

        @Test
        @DisplayName("灰度比例不低于0")
        void shouldClampRolloutPercentageToMin() {
            config.setRolloutPercentage(FeatureToggleConfig.V2_RATING_API, -10);

            assertEquals(0, config.getToggles().get(FeatureToggleConfig.V2_RATING_API).getRolloutPercentage());
        }

        @Test
        @DisplayName("100%灰度对所有用户启用")
        void shouldEnableForAllUsersAt100Percent() {
            config.setRolloutPercentage(FeatureToggleConfig.V2_RATING_API, 100);

            // 测试多个用户ID都能访问
            for (long userId = 1; userId <= 100; userId++) {
                assertTrue(config.isEnabled(FeatureToggleConfig.V2_RATING_API, userId));
            }
        }

        @Test
        @DisplayName("0%灰度对所有用户禁用")
        void shouldDisableForAllUsersAt0Percent() {
            config.setRolloutPercentage(FeatureToggleConfig.V2_RATING_API, 0);

            for (long userId = 1; userId <= 100; userId++) {
                assertFalse(config.isEnabled(FeatureToggleConfig.V2_RATING_API, userId));
            }
        }

        @Test
        @DisplayName("用户ID决定灰度分组")
        void shouldAssignUserToConsistentBucket() {
            config.setRolloutPercentage(FeatureToggleConfig.V2_RATING_API, 50);

            // 同一用户ID应该总是在同一分组
            Long userId = 42L;
            boolean firstCheck = config.isEnabled(FeatureToggleConfig.V2_RATING_API, userId);
            boolean secondCheck = config.isEnabled(FeatureToggleConfig.V2_RATING_API, userId);

            assertEquals(firstCheck, secondCheck);
        }
    }

    @Nested
    @DisplayName("白名单/黑名单测试")
    class WhitelistBlacklistTest {

        @Test
        @DisplayName("白名单用户始终启用")
        void shouldEnableForWhitelistedUsers() {
            config.setRolloutPercentage(FeatureToggleConfig.V2_RATING_API, 0);
            config.getToggles().get(FeatureToggleConfig.V2_RATING_API).setWhitelistUsers(Set.of(1L, 2L, 3L));

            assertTrue(config.isEnabled(FeatureToggleConfig.V2_RATING_API, 1L));
            assertTrue(config.isEnabled(FeatureToggleConfig.V2_RATING_API, 2L));
            assertTrue(config.isEnabled(FeatureToggleConfig.V2_RATING_API, 3L));
        }

        @Test
        @DisplayName("黑名单用户始终禁用")
        void shouldDisableForBlacklistedUsers() {
            config.setRolloutPercentage(FeatureToggleConfig.V2_RATING_API, 100);
            config.getToggles().get(FeatureToggleConfig.V2_RATING_API).setBlacklistUsers(Set.of(999L));

            assertFalse(config.isEnabled(FeatureToggleConfig.V2_RATING_API, 999L));
        }

        @Test
        @DisplayName("非白名单用户受灰度控制")
        void shouldApplyRolloutForNonWhitelistedUsers() {
            config.setRolloutPercentage(FeatureToggleConfig.V2_RATING_API, 0);
            config.getToggles().get(FeatureToggleConfig.V2_RATING_API).setWhitelistUsers(Set.of(1L));

            assertTrue(config.isEnabled(FeatureToggleConfig.V2_RATING_API, 1L));
            assertFalse(config.isEnabled(FeatureToggleConfig.V2_RATING_API, 2L));
        }
    }

    @Nested
    @DisplayName("未知特性测试")
    class UnknownFeatureTest {

        @Test
        @DisplayName("未知特性返回false")
        void shouldReturnFalseForUnknownFeature() {
            assertFalse(config.isEnabled("unknown-feature"));
            assertFalse(config.isEnabled("unknown-feature", 1L));
        }
    }
}
