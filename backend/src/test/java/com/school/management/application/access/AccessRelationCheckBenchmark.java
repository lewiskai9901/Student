package com.school.management.application.access;

import com.school.management.domain.access.model.valueobject.AccessLevel;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.TimeUnit;

/**
 * Phase 7 W7.5 — JMH 微基准 access_level enum.parse() 与基础对象构造.
 *
 * <p>真实 check() / expand() 性能基准需要启动 Spring + Redis + DB,
 * 不适合 JMH(JMH 期望纯 Java 函数). 此处只做"AccessLevel.parse() 不慢"等
 * 单元级别基准, 留生产 SRE 用 JMeter / Gatling 做端到端压测.
 *
 * <p>运行方式 (本测试默认 @Disabled, 不入 mvn test 流程):
 * <pre>
 * mvn test -Dtest=AccessRelationCheckBenchmark#runBenchmark -DargLine="-Xmx512m"
 * </pre>
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 2, time = 1)
@Measurement(iterations = 3, time = 1)
@Fork(1)
@State(Scope.Benchmark)
public class AccessRelationCheckBenchmark {

    private String[] testInputs;

    @Setup
    public void setup() {
        testInputs = new String[]{"FULL", "READ_ONLY", "OWNER", "full", "owner", "read_only"};
    }

    @Benchmark
    public AccessLevel parseValid() {
        return AccessLevel.parse(testInputs[0]);
    }

    @Benchmark
    public AccessLevel parseLowerCase() {
        return AccessLevel.parse(testInputs[3]);
    }

    @Benchmark
    public AccessLevel parseInvalid() {
        return AccessLevel.parse("WHATEVER");
    }

    @Benchmark
    public boolean isReadWrite() {
        return AccessLevel.FULL.isReadWrite();
    }

    /**
     * 实际跑基准的入口 — JUnit 启动 JMH Runner.
     * 默认 @Disabled, 显式 -Dtest=AccessRelationCheckBenchmark 或注释掉 disable 才跑.
     */
    @Test
    @Disabled("JMH benchmark — 显式触发,不进 mvn test 默认流程. 命令: mvn test -Dtest=AccessRelationCheckBenchmark")
    public void runBenchmark() throws Exception {
        Options opt = new OptionsBuilder()
            .include(this.getClass().getName())
            .forks(1)
            .build();
        new Runner(opt).run();
    }
}
