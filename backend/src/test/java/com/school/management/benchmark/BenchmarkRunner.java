package com.school.management.benchmark;

import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * 性能基准测试运行器
 *
 * 运行所有基准测试并生成报告
 *
 * 使用方式:
 * 1. 直接运行此类的 main 方法
 * 2. 或使用 Maven: mvn exec:java -Dexec.mainClass="com.school.management.benchmark.BenchmarkRunner"
 *
 * 测试结果会输出到 target/benchmark-results.json
 */
public class BenchmarkRunner {

    public static void main(String[] args) throws RunnerException {
        // 选择要运行的基准测试
        String benchmarkPattern = args.length > 0 ? args[0] : ".*Benchmark.*";

        Options opt = new OptionsBuilder()
            // 包含所有基准测试类
            .include(benchmarkPattern)
            // Fork 数量 (启动新的 JVM 进程)
            .forks(1)
            // 预热迭代
            .warmupIterations(3)
            // 测量迭代
            .measurementIterations(5)
            // 结果输出格式
            .resultFormat(ResultFormatType.JSON)
            // 结果输出文件
            .result("target/benchmark-results.json")
            // JVM 参数
            .jvmArgs(
                "-Xms512m",
                "-Xmx512m",
                "-XX:+UseG1GC"
            )
            .build();

        System.out.println("========================================");
        System.out.println("     性能基准测试开始");
        System.out.println("========================================");
        System.out.println("测试模式: " + benchmarkPattern);
        System.out.println("结果输出: target/benchmark-results.json");
        System.out.println("----------------------------------------");

        new Runner(opt).run();

        System.out.println("========================================");
        System.out.println("     性能基准测试完成");
        System.out.println("========================================");
    }

    /**
     * 运行单个基准测试类
     */
    public static void runSingle(Class<?> benchmarkClass) throws RunnerException {
        Options opt = new OptionsBuilder()
            .include(benchmarkClass.getSimpleName())
            .forks(1)
            .warmupIterations(2)
            .measurementIterations(3)
            .build();

        new Runner(opt).run();
    }

    /**
     * 快速运行模式 (用于开发调试)
     */
    public static void quickRun(String pattern) throws RunnerException {
        Options opt = new OptionsBuilder()
            .include(pattern)
            .forks(0) // 不启动新的 JVM (更快但结果不够准确)
            .warmupIterations(1)
            .measurementIterations(2)
            .build();

        new Runner(opt).run();
    }
}
