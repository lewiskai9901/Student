package com.school.management.benchmark;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * JSON 序列化性能基准测试
 *
 * 测试不同数据结构和配置下的序列化/反序列化性能
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, warmups = 1)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
public class SerializationBenchmark {

    private ObjectMapper defaultMapper;
    private ObjectMapper optimizedMapper;

    private SimpleDTO simpleDto;
    private ComplexDTO complexDto;
    private List<SimpleDTO> dtoList;

    private String simpleJson;
    private String complexJson;
    private String listJson;

    @Setup(Level.Trial)
    public void setup() throws JsonProcessingException {
        // 默认配置的 ObjectMapper
        defaultMapper = new ObjectMapper();
        defaultMapper.registerModule(new JavaTimeModule());
        defaultMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // 优化配置的 ObjectMapper
        optimizedMapper = new ObjectMapper();
        optimizedMapper.registerModule(new JavaTimeModule());
        optimizedMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        optimizedMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

        // 简单 DTO
        simpleDto = new SimpleDTO(1L, "测试用户", "test@example.com", LocalDateTime.now());

        // 复杂 DTO (包含嵌套对象和列表)
        List<String> tags = List.of("标签1", "标签2", "标签3");
        List<AddressDTO> addresses = List.of(
            new AddressDTO("北京市", "朝阳区", "某某街道"),
            new AddressDTO("上海市", "浦东新区", "另一条街道")
        );
        complexDto = new ComplexDTO(
            1L, "复杂用户", "complex@example.com",
            LocalDateTime.now(), tags, addresses
        );

        // DTO 列表
        dtoList = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            dtoList.add(new SimpleDTO(
                (long) i,
                "用户" + i,
                "user" + i + "@example.com",
                LocalDateTime.now()
            ));
        }

        // 预序列化 JSON 字符串
        simpleJson = defaultMapper.writeValueAsString(simpleDto);
        complexJson = defaultMapper.writeValueAsString(complexDto);
        listJson = defaultMapper.writeValueAsString(dtoList);
    }

    // ==================== 简单对象序列化 ====================

    @Benchmark
    public void serializeSimpleDefault(Blackhole bh) throws JsonProcessingException {
        bh.consume(defaultMapper.writeValueAsString(simpleDto));
    }

    @Benchmark
    public void serializeSimpleOptimized(Blackhole bh) throws JsonProcessingException {
        bh.consume(optimizedMapper.writeValueAsString(simpleDto));
    }

    // ==================== 复杂对象序列化 ====================

    @Benchmark
    public void serializeComplexDefault(Blackhole bh) throws JsonProcessingException {
        bh.consume(defaultMapper.writeValueAsString(complexDto));
    }

    @Benchmark
    public void serializeComplexOptimized(Blackhole bh) throws JsonProcessingException {
        bh.consume(optimizedMapper.writeValueAsString(complexDto));
    }

    // ==================== 列表序列化 ====================

    @Benchmark
    public void serializeListDefault(Blackhole bh) throws JsonProcessingException {
        bh.consume(defaultMapper.writeValueAsString(dtoList));
    }

    @Benchmark
    public void serializeListOptimized(Blackhole bh) throws JsonProcessingException {
        bh.consume(optimizedMapper.writeValueAsString(dtoList));
    }

    // ==================== 反序列化 ====================

    @Benchmark
    public void deserializeSimple(Blackhole bh) throws JsonProcessingException {
        bh.consume(defaultMapper.readValue(simpleJson, SimpleDTO.class));
    }

    @Benchmark
    public void deserializeComplex(Blackhole bh) throws JsonProcessingException {
        bh.consume(defaultMapper.readValue(complexJson, ComplexDTO.class));
    }

    // ==================== 数据类 ====================

    public record SimpleDTO(
        Long id,
        String name,
        String email,
        LocalDateTime createdAt
    ) {}

    public record AddressDTO(
        String city,
        String district,
        String street
    ) {}

    public record ComplexDTO(
        Long id,
        String name,
        String email,
        LocalDateTime createdAt,
        List<String> tags,
        List<AddressDTO> addresses
    ) {}

    /**
     * 运行基准测试
     */
    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
            .include(SerializationBenchmark.class.getSimpleName())
            .forks(1)
            .build();

        new Runner(opt).run();
    }
}
