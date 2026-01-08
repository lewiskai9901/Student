package com.school.management.aspect;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 性能监控切面测试
 */
@DisplayName("性能监控切面测试")
class PerformanceMonitorAspectTest {

    private PerformanceMonitorAspect aspect;

    @BeforeEach
    void setUp() {
        aspect = new PerformanceMonitorAspect();
    }

    @Nested
    @DisplayName("MethodStats测试")
    class MethodStatsTest {

        @Test
        @DisplayName("记录单次调用")
        void shouldRecordSingleInvocation() {
            var stats = new PerformanceMonitorAspect.MethodStats();

            stats.record(100L, false);

            assertEquals(1, stats.getInvocationCount());
            assertEquals(100L, stats.getTotalTime());
            assertEquals(100L, stats.getMinTime());
            assertEquals(100L, stats.getMaxTime());
            assertEquals(100.0, stats.getAverageTime());
            assertEquals(0, stats.getErrorCount());
        }

        @Test
        @DisplayName("记录多次调用")
        void shouldRecordMultipleInvocations() {
            var stats = new PerformanceMonitorAspect.MethodStats();

            stats.record(50L, false);
            stats.record(100L, false);
            stats.record(150L, false);

            assertEquals(3, stats.getInvocationCount());
            assertEquals(300L, stats.getTotalTime());
            assertEquals(50L, stats.getMinTime());
            assertEquals(150L, stats.getMaxTime());
            assertEquals(100.0, stats.getAverageTime());
        }

        @Test
        @DisplayName("记录错误调用")
        void shouldRecordErrors() {
            var stats = new PerformanceMonitorAspect.MethodStats();

            stats.record(100L, false);
            stats.record(200L, true);
            stats.record(150L, false);

            assertEquals(3, stats.getInvocationCount());
            assertEquals(1, stats.getErrorCount());
        }

        @Test
        @DisplayName("更新最大时间")
        void shouldUpdateMaxTime() {
            var stats = new PerformanceMonitorAspect.MethodStats();

            stats.record(100L, false);
            stats.record(50L, false);
            stats.record(200L, false);
            stats.record(150L, false);

            assertEquals(200L, stats.getMaxTime());
        }

        @Test
        @DisplayName("更新最小时间")
        void shouldUpdateMinTime() {
            var stats = new PerformanceMonitorAspect.MethodStats();

            stats.record(100L, false);
            stats.record(200L, false);
            stats.record(50L, false);
            stats.record(150L, false);

            assertEquals(50L, stats.getMinTime());
        }

        @Test
        @DisplayName("无调用时最小时间为0")
        void shouldReturnZeroMinTimeWhenNoInvocations() {
            var stats = new PerformanceMonitorAspect.MethodStats();

            assertEquals(0, stats.getMinTime());
        }

        @Test
        @DisplayName("无调用时平均时间为0")
        void shouldReturnZeroAverageWhenNoInvocations() {
            var stats = new PerformanceMonitorAspect.MethodStats();

            assertEquals(0.0, stats.getAverageTime());
        }

        @Test
        @DisplayName("toString格式正确")
        void shouldFormatToString() {
            var stats = new PerformanceMonitorAspect.MethodStats();
            stats.record(100L, false);
            stats.record(200L, true);

            String str = stats.toString();

            assertTrue(str.contains("count=2"));
            assertTrue(str.contains("avg=150.00ms"));
            assertTrue(str.contains("min=100ms"));
            assertTrue(str.contains("max=200ms"));
            assertTrue(str.contains("errors=1"));
        }
    }

    @Nested
    @DisplayName("统计管理测试")
    class StatsManagementTest {

        @Test
        @DisplayName("获取空统计")
        void shouldReturnEmptyStats() {
            var stats = aspect.getMethodStats();

            assertTrue(stats.isEmpty());
        }

        @Test
        @DisplayName("重置统计")
        void shouldResetStats() {
            // 先获取引用，修改后重置
            var stats = aspect.getMethodStats();
            stats.put("test", new PerformanceMonitorAspect.MethodStats());
            assertEquals(1, aspect.getMethodStats().size());

            aspect.resetStats();

            assertTrue(aspect.getMethodStats().isEmpty());
        }
    }

    @Nested
    @DisplayName("并发安全测试")
    class ConcurrencyTest {

        @Test
        @DisplayName("并发记录统计")
        void shouldHandleConcurrentRecords() throws InterruptedException {
            var stats = new PerformanceMonitorAspect.MethodStats();
            int threadCount = 10;
            int recordsPerThread = 100;

            Thread[] threads = new Thread[threadCount];
            for (int i = 0; i < threadCount; i++) {
                threads[i] = new Thread(() -> {
                    for (int j = 0; j < recordsPerThread; j++) {
                        stats.record((long) (Math.random() * 100), Math.random() < 0.1);
                    }
                });
            }

            for (Thread thread : threads) {
                thread.start();
            }

            for (Thread thread : threads) {
                thread.join();
            }

            assertEquals(threadCount * recordsPerThread, stats.getInvocationCount());
        }
    }
}
