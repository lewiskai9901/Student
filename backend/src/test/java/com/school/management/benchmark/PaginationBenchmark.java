package com.school.management.benchmark;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 分页性能基准测试
 *
 * 比较不同分页实现方式的性能：
 * - 传统 subList 分页
 * - Stream API 分页
 * - 手动索引分页
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, warmups = 1)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
public class PaginationBenchmark {

    @Param({"1000", "10000", "100000"})
    private int listSize;

    @Param({"10", "50", "100"})
    private int pageSize;

    private List<Integer> testList;

    @Setup(Level.Trial)
    public void setup() {
        testList = IntStream.range(0, listSize)
            .boxed()
            .collect(Collectors.toList());
    }

    // ==================== 第一页分页测试 ====================

    @Benchmark
    public void subListFirstPage(Blackhole bh) {
        int fromIndex = 0;
        int toIndex = Math.min(pageSize, testList.size());
        bh.consume(testList.subList(fromIndex, toIndex));
    }

    @Benchmark
    public void streamFirstPage(Blackhole bh) {
        bh.consume(testList.stream()
            .skip(0)
            .limit(pageSize)
            .collect(Collectors.toList()));
    }

    @Benchmark
    public void manualFirstPage(Blackhole bh) {
        List<Integer> result = new ArrayList<>(pageSize);
        int end = Math.min(pageSize, testList.size());
        for (int i = 0; i < end; i++) {
            result.add(testList.get(i));
        }
        bh.consume(result);
    }

    // ==================== 中间页分页测试 ====================

    @Benchmark
    public void subListMiddlePage(Blackhole bh) {
        int pageNum = listSize / pageSize / 2; // 中间页
        int fromIndex = pageNum * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, testList.size());
        bh.consume(testList.subList(fromIndex, toIndex));
    }

    @Benchmark
    public void streamMiddlePage(Blackhole bh) {
        int pageNum = listSize / pageSize / 2;
        bh.consume(testList.stream()
            .skip((long) pageNum * pageSize)
            .limit(pageSize)
            .collect(Collectors.toList()));
    }

    @Benchmark
    public void manualMiddlePage(Blackhole bh) {
        int pageNum = listSize / pageSize / 2;
        List<Integer> result = new ArrayList<>(pageSize);
        int start = pageNum * pageSize;
        int end = Math.min(start + pageSize, testList.size());
        for (int i = start; i < end; i++) {
            result.add(testList.get(i));
        }
        bh.consume(result);
    }

    // ==================== 最后一页分页测试 ====================

    @Benchmark
    public void subListLastPage(Blackhole bh) {
        int totalPages = (testList.size() + pageSize - 1) / pageSize;
        int pageNum = totalPages - 1;
        int fromIndex = pageNum * pageSize;
        int toIndex = testList.size();
        bh.consume(testList.subList(fromIndex, toIndex));
    }

    @Benchmark
    public void streamLastPage(Blackhole bh) {
        int totalPages = (testList.size() + pageSize - 1) / pageSize;
        int pageNum = totalPages - 1;
        bh.consume(testList.stream()
            .skip((long) pageNum * pageSize)
            .limit(pageSize)
            .collect(Collectors.toList()));
    }

    @Benchmark
    public void manualLastPage(Blackhole bh) {
        int totalPages = (testList.size() + pageSize - 1) / pageSize;
        int pageNum = totalPages - 1;
        List<Integer> result = new ArrayList<>();
        int start = pageNum * pageSize;
        for (int i = start; i < testList.size(); i++) {
            result.add(testList.get(i));
        }
        bh.consume(result);
    }

    /**
     * 运行基准测试
     */
    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
            .include(PaginationBenchmark.class.getSimpleName())
            .forks(1)
            .build();

        new Runner(opt).run();
    }
}
