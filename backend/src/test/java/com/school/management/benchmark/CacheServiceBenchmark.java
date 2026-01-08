package com.school.management.benchmark;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 缓存服务性能基准测试
 *
 * 比较不同缓存实现的性能差异：
 * - HashMap (非线程安全)
 * - ConcurrentHashMap (线程安全)
 * - 带过期时间的缓存实现
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, warmups = 1)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
public class CacheServiceBenchmark {

    private Map<String, Object> hashMap;
    private Map<String, Object> concurrentHashMap;
    private SimpleCache simpleCache;

    private static final int CACHE_SIZE = 10000;
    private static final String KEY_PREFIX = "key_";

    @Setup(Level.Trial)
    public void setup() {
        hashMap = new HashMap<>();
        concurrentHashMap = new ConcurrentHashMap<>();
        simpleCache = new SimpleCache();

        // 预填充缓存
        for (int i = 0; i < CACHE_SIZE; i++) {
            String key = KEY_PREFIX + i;
            String value = "value_" + i;
            hashMap.put(key, value);
            concurrentHashMap.put(key, value);
            simpleCache.put(key, value, Duration.ofHours(1));
        }
    }

    // ==================== 读取性能测试 ====================

    @Benchmark
    public void hashMapGet(Blackhole bh) {
        bh.consume(hashMap.get(KEY_PREFIX + (System.nanoTime() % CACHE_SIZE)));
    }

    @Benchmark
    public void concurrentHashMapGet(Blackhole bh) {
        bh.consume(concurrentHashMap.get(KEY_PREFIX + (System.nanoTime() % CACHE_SIZE)));
    }

    @Benchmark
    public void simpleCacheGet(Blackhole bh) {
        bh.consume(simpleCache.get(KEY_PREFIX + (System.nanoTime() % CACHE_SIZE)));
    }

    // ==================== 写入性能测试 ====================

    @Benchmark
    public void hashMapPut(Blackhole bh) {
        String key = "new_key_" + System.nanoTime();
        hashMap.put(key, "value");
        bh.consume(key);
    }

    @Benchmark
    public void concurrentHashMapPut(Blackhole bh) {
        String key = "new_key_" + System.nanoTime();
        concurrentHashMap.put(key, "value");
        bh.consume(key);
    }

    @Benchmark
    public void simpleCachePut(Blackhole bh) {
        String key = "new_key_" + System.nanoTime();
        simpleCache.put(key, "value", Duration.ofMinutes(30));
        bh.consume(key);
    }

    // ==================== 缓存未命中测试 ====================

    @Benchmark
    public void hashMapMiss(Blackhole bh) {
        bh.consume(hashMap.get("nonexistent_" + System.nanoTime()));
    }

    @Benchmark
    public void concurrentHashMapMiss(Blackhole bh) {
        bh.consume(concurrentHashMap.get("nonexistent_" + System.nanoTime()));
    }

    @Benchmark
    public void simpleCacheMiss(Blackhole bh) {
        bh.consume(simpleCache.get("nonexistent_" + System.nanoTime()));
    }

    /**
     * 简单的带过期时间的缓存实现
     */
    private static class SimpleCache {
        private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

        public void put(String key, Object value, Duration ttl) {
            cache.put(key, new CacheEntry(value, System.currentTimeMillis() + ttl.toMillis()));
        }

        public Object get(String key) {
            CacheEntry entry = cache.get(key);
            if (entry == null) {
                return null;
            }
            if (System.currentTimeMillis() > entry.expireTime) {
                cache.remove(key);
                return null;
            }
            return entry.value;
        }

        public void evict(String key) {
            cache.remove(key);
        }

        private record CacheEntry(Object value, long expireTime) {}
    }

    /**
     * 运行基准测试
     */
    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
            .include(CacheServiceBenchmark.class.getSimpleName())
            .forks(1)
            .build();

        new Runner(opt).run();
    }
}
