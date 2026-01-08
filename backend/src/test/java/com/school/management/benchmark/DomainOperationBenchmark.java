package com.school.management.benchmark;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 领域操作性能基准测试
 *
 * 测试常见领域业务操作的性能
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, warmups = 1)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
public class DomainOperationBenchmark {

    private List<ScoreRecord> scoreRecords;
    private List<StudentEntity> students;
    private Map<Long, List<ScoreRecord>> scoresByStudent;

    private static final int STUDENT_COUNT = 1000;
    private static final int RECORDS_PER_STUDENT = 50;

    @Setup(Level.Trial)
    public void setup() {
        Random random = new Random(42);

        // 创建学生数据
        students = new ArrayList<>();
        for (int i = 0; i < STUDENT_COUNT; i++) {
            students.add(new StudentEntity(
                (long) i,
                "学生" + i,
                "STU" + String.format("%06d", i),
                (long) (i % 10), // 10个班级
                (long) (i % 3)  // 3个年级
            ));
        }

        // 创建成绩记录
        scoreRecords = new ArrayList<>();
        for (int i = 0; i < STUDENT_COUNT; i++) {
            for (int j = 0; j < RECORDS_PER_STUDENT; j++) {
                scoreRecords.add(new ScoreRecord(
                    (long) (i * RECORDS_PER_STUDENT + j),
                    (long) i,
                    LocalDate.now().minusDays(j),
                    BigDecimal.valueOf(60 + random.nextInt(40)),
                    "检查项目" + (j % 10)
                ));
            }
        }

        // 按学生分组
        scoresByStudent = scoreRecords.stream()
            .collect(Collectors.groupingBy(ScoreRecord::studentId));
    }

    // ==================== 成绩统计测试 ====================

    @Benchmark
    public void calculateAverageScoreLoop(Blackhole bh) {
        BigDecimal sum = BigDecimal.ZERO;
        int count = 0;
        for (ScoreRecord record : scoreRecords) {
            sum = sum.add(record.score());
            count++;
        }
        BigDecimal avg = count > 0 ? sum.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        bh.consume(avg);
    }

    @Benchmark
    public void calculateAverageScoreStream(Blackhole bh) {
        OptionalDouble avg = scoreRecords.stream()
            .mapToDouble(r -> r.score().doubleValue())
            .average();
        bh.consume(avg.orElse(0.0));
    }

    @Benchmark
    public void calculateAverageScoreParallelStream(Blackhole bh) {
        OptionalDouble avg = scoreRecords.parallelStream()
            .mapToDouble(r -> r.score().doubleValue())
            .average();
        bh.consume(avg.orElse(0.0));
    }

    // ==================== 按班级分组统计 ====================

    @Benchmark
    public void groupByClassLoop(Blackhole bh) {
        Map<Long, List<StudentEntity>> result = new HashMap<>();
        for (StudentEntity student : students) {
            result.computeIfAbsent(student.classId(), k -> new ArrayList<>()).add(student);
        }
        bh.consume(result);
    }

    @Benchmark
    public void groupByClassStream(Blackhole bh) {
        Map<Long, List<StudentEntity>> result = students.stream()
            .collect(Collectors.groupingBy(StudentEntity::classId));
        bh.consume(result);
    }

    // ==================== 学生成绩排名 ====================

    @Benchmark
    public void calculateStudentRankingsLoop(Blackhole bh) {
        // 计算每个学生的平均分
        Map<Long, BigDecimal> avgScores = new HashMap<>();
        for (var entry : scoresByStudent.entrySet()) {
            BigDecimal sum = BigDecimal.ZERO;
            for (ScoreRecord record : entry.getValue()) {
                sum = sum.add(record.score());
            }
            int count = entry.getValue().size();
            avgScores.put(entry.getKey(), count > 0 ?
                sum.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP) :
                BigDecimal.ZERO);
        }

        // 排序
        List<Map.Entry<Long, BigDecimal>> sorted = new ArrayList<>(avgScores.entrySet());
        sorted.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        // 生成排名
        Map<Long, Integer> rankings = new HashMap<>();
        for (int i = 0; i < sorted.size(); i++) {
            rankings.put(sorted.get(i).getKey(), i + 1);
        }
        bh.consume(rankings);
    }

    @Benchmark
    public void calculateStudentRankingsStream(Blackhole bh) {
        // 计算每个学生的平均分
        Map<Long, Double> avgScores = scoresByStudent.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> e.getValue().stream()
                    .mapToDouble(r -> r.score().doubleValue())
                    .average()
                    .orElse(0.0)
            ));

        // 排序并生成排名
        List<Long> sortedStudents = avgScores.entrySet().stream()
            .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
            .map(Map.Entry::getKey)
            .toList();

        Map<Long, Integer> rankings = new HashMap<>();
        for (int i = 0; i < sortedStudents.size(); i++) {
            rankings.put(sortedStudents.get(i), i + 1);
        }
        bh.consume(rankings);
    }

    // ==================== 日期范围过滤 ====================

    @Benchmark
    public void filterByDateRangeLoop(Blackhole bh) {
        LocalDate start = LocalDate.now().minusDays(30);
        LocalDate end = LocalDate.now();

        List<ScoreRecord> result = new ArrayList<>();
        for (ScoreRecord record : scoreRecords) {
            if (!record.checkDate().isBefore(start) && !record.checkDate().isAfter(end)) {
                result.add(record);
            }
        }
        bh.consume(result);
    }

    @Benchmark
    public void filterByDateRangeStream(Blackhole bh) {
        LocalDate start = LocalDate.now().minusDays(30);
        LocalDate end = LocalDate.now();

        List<ScoreRecord> result = scoreRecords.stream()
            .filter(r -> !r.checkDate().isBefore(start) && !r.checkDate().isAfter(end))
            .toList();
        bh.consume(result);
    }

    @Benchmark
    public void filterByDateRangeParallelStream(Blackhole bh) {
        LocalDate start = LocalDate.now().minusDays(30);
        LocalDate end = LocalDate.now();

        List<ScoreRecord> result = scoreRecords.parallelStream()
            .filter(r -> !r.checkDate().isBefore(start) && !r.checkDate().isAfter(end))
            .toList();
        bh.consume(result);
    }

    // ==================== 数据类 ====================

    public record StudentEntity(
        Long id,
        String name,
        String studentNo,
        Long classId,
        Long gradeId
    ) {}

    public record ScoreRecord(
        Long id,
        Long studentId,
        LocalDate checkDate,
        BigDecimal score,
        String checkItem
    ) {}

    /**
     * 运行基准测试
     */
    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
            .include(DomainOperationBenchmark.class.getSimpleName())
            .forks(1)
            .build();

        new Runner(opt).run();
    }
}
