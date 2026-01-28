package com.school.management.application.inspection.saga;

import com.school.management.domain.inspection.model.ClassInspectionRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RatingCalculationStep 测试")
class RatingCalculationStepTest {

    private RatingCalculationStep step;
    private static final LocalDate TEST_DATE = LocalDate.of(2026, 1, 15);
    private static final Long SESSION_ID = 1L;

    @BeforeEach
    void setUp() {
        step = new RatingCalculationStep();
    }

    private ClassInspectionRecord createRecord(Long classId, BigDecimal finalScore) {
        return ClassInspectionRecord.builder()
                .id(classId)
                .sessionId(SESSION_ID)
                .classId(classId)
                .className("班级" + classId)
                .orgUnitId(100L)
                .orgUnitName("测试部门")
                .baseScore(100)
                .totalDeduction(BigDecimal.ZERO)
                .bonusScore(BigDecimal.ZERO)
                .finalScore(finalScore)
                .build();
    }

    private ClassInspectionRecord createRecordWithDeduction(Long classId, int baseScore,
                                                             BigDecimal deduction, BigDecimal bonus) {
        return ClassInspectionRecord.builder()
                .id(classId)
                .sessionId(SESSION_ID)
                .classId(classId)
                .className("班级" + classId)
                .orgUnitId(100L)
                .orgUnitName("测试部门")
                .baseScore(baseScore)
                .totalDeduction(deduction)
                .bonusScore(bonus)
                .build();
    }

    @Nested
    @DisplayName("评级分类")
    class RatingClassification {

        @Test
        @DisplayName("90分及以上应该评为优秀")
        void scoreAbove90ShouldBeExcellent() {
            List<ClassInspectionRecord> records = List.of(
                    createRecord(1L, new BigDecimal("95")),
                    createRecord(2L, new BigDecimal("90"))
            );

            RatingCalculationResult result = step.execute(SESSION_ID, TEST_DATE, records);

            assertEquals(2, result.getRatingDistribution().get("EXCELLENT").size());
            assertTrue(result.getRatingDistribution().get("EXCELLENT").contains(1L));
            assertTrue(result.getRatingDistribution().get("EXCELLENT").contains(2L));
        }

        @Test
        @DisplayName("80-89分应该评为良好")
        void score80To89ShouldBeGood() {
            List<ClassInspectionRecord> records = List.of(
                    createRecord(1L, new BigDecimal("85")),
                    createRecord(2L, new BigDecimal("80"))
            );

            RatingCalculationResult result = step.execute(SESSION_ID, TEST_DATE, records);

            assertEquals(2, result.getRatingDistribution().get("GOOD").size());
        }

        @Test
        @DisplayName("60-79分应该评为合格")
        void score60To79ShouldBePass() {
            List<ClassInspectionRecord> records = List.of(
                    createRecord(1L, new BigDecimal("75")),
                    createRecord(2L, new BigDecimal("60"))
            );

            RatingCalculationResult result = step.execute(SESSION_ID, TEST_DATE, records);

            assertEquals(2, result.getRatingDistribution().get("PASS").size());
        }

        @Test
        @DisplayName("60分以下应该评为不合格")
        void scoreBelow60ShouldBeFail() {
            List<ClassInspectionRecord> records = List.of(
                    createRecord(1L, new BigDecimal("59")),
                    createRecord(2L, new BigDecimal("30"))
            );

            RatingCalculationResult result = step.execute(SESSION_ID, TEST_DATE, records);

            assertEquals(2, result.getRatingDistribution().get("FAIL").size());
        }

        @Test
        @DisplayName("混合分数应该正确分类")
        void mixedScoresShouldBeCorrectlyClassified() {
            List<ClassInspectionRecord> records = List.of(
                    createRecord(1L, new BigDecimal("95")),
                    createRecord(2L, new BigDecimal("85")),
                    createRecord(3L, new BigDecimal("70")),
                    createRecord(4L, new BigDecimal("50"))
            );

            RatingCalculationResult result = step.execute(SESSION_ID, TEST_DATE, records);

            Map<String, List<Long>> dist = result.getRatingDistribution();
            assertEquals(1, dist.get("EXCELLENT").size());
            assertEquals(1, dist.get("GOOD").size());
            assertEquals(1, dist.get("PASS").size());
            assertEquals(1, dist.get("FAIL").size());
        }
    }

    @Nested
    @DisplayName("平均分计算")
    class AverageScoreCalculation {

        @Test
        @DisplayName("应该正确计算平均分")
        void shouldCalculateAverageCorrectly() {
            List<ClassInspectionRecord> records = List.of(
                    createRecord(1L, new BigDecimal("90")),
                    createRecord(2L, new BigDecimal("80")),
                    createRecord(3L, new BigDecimal("70"))
            );

            RatingCalculationResult result = step.execute(SESSION_ID, TEST_DATE, records);

            assertEquals(new BigDecimal("80.00"), result.getAverageScore());
        }

        @Test
        @DisplayName("空记录应该返回零平均分")
        void emptyRecordsShouldReturnZeroAverage() {
            RatingCalculationResult result = step.execute(SESSION_ID, TEST_DATE, Collections.emptyList());

            assertEquals(BigDecimal.ZERO, result.getAverageScore());
            assertEquals(0, result.getRatedClassCount());
        }

        @Test
        @DisplayName("单条记录的平均分等于自身")
        void singleRecordAverageEqualsSelf() {
            List<ClassInspectionRecord> records = List.of(
                    createRecord(1L, new BigDecimal("88.50"))
            );

            RatingCalculationResult result = step.execute(SESSION_ID, TEST_DATE, records);

            assertEquals(new BigDecimal("88.50"), result.getAverageScore());
        }
    }

    @Nested
    @DisplayName("分数解析 (finalScore 为空时使用 base - deduction + bonus)")
    class ScoreResolution {

        @Test
        @DisplayName("finalScore为空时应该使用计算得分")
        void shouldFallbackToComputedScore() {
            List<ClassInspectionRecord> records = List.of(
                    createRecordWithDeduction(1L, 100, new BigDecimal("15"), new BigDecimal("3"))
            );

            RatingCalculationResult result = step.execute(SESSION_ID, TEST_DATE, records);

            // 100 - 15 + 3 = 88 → GOOD
            assertEquals(1, result.getRatingDistribution().get("GOOD").size());
            assertEquals(new BigDecimal("88.00"), result.getAverageScore());
        }

        @Test
        @DisplayName("有finalScore时应该优先使用")
        void shouldPreferFinalScore() {
            List<ClassInspectionRecord> records = List.of(
                    createRecord(1L, new BigDecimal("92"))
            );

            RatingCalculationResult result = step.execute(SESSION_ID, TEST_DATE, records);

            assertEquals(1, result.getRatingDistribution().get("EXCELLENT").size());
        }
    }

    @Nested
    @DisplayName("结果元数据")
    class ResultMetadata {

        @Test
        @DisplayName("应该设置正确的会话ID和日期")
        void shouldSetCorrectMetadata() {
            List<ClassInspectionRecord> records = List.of(
                    createRecord(1L, new BigDecimal("85"))
            );

            RatingCalculationResult result = step.execute(SESSION_ID, TEST_DATE, records);

            assertEquals(SESSION_ID, result.getSessionId());
            assertEquals(TEST_DATE, result.getInspectionDate());
            assertEquals(1, result.getRatedClassCount());
        }

        @Test
        @DisplayName("所有评级类别应该存在于分布中")
        void allRatingCategoriesShouldExist() {
            RatingCalculationResult result = step.execute(SESSION_ID, TEST_DATE, Collections.emptyList());

            assertNotNull(result.getRatingDistribution().get("EXCELLENT"));
            assertNotNull(result.getRatingDistribution().get("GOOD"));
            assertNotNull(result.getRatingDistribution().get("PASS"));
            assertNotNull(result.getRatingDistribution().get("FAIL"));
        }
    }

    @Nested
    @DisplayName("边界值")
    class BoundaryValues {

        @Test
        @DisplayName("恰好90分应该是优秀")
        void exactly90ShouldBeExcellent() {
            List<ClassInspectionRecord> records = List.of(
                    createRecord(1L, new BigDecimal("90"))
            );
            RatingCalculationResult result = step.execute(SESSION_ID, TEST_DATE, records);
            assertEquals(1, result.getRatingDistribution().get("EXCELLENT").size());
        }

        @Test
        @DisplayName("89.99分应该是良好")
        void just89Point99ShouldBeGood() {
            List<ClassInspectionRecord> records = List.of(
                    createRecord(1L, new BigDecimal("89.99"))
            );
            RatingCalculationResult result = step.execute(SESSION_ID, TEST_DATE, records);
            assertEquals(1, result.getRatingDistribution().get("GOOD").size());
        }

        @Test
        @DisplayName("恰好80分应该是良好")
        void exactly80ShouldBeGood() {
            List<ClassInspectionRecord> records = List.of(
                    createRecord(1L, new BigDecimal("80"))
            );
            RatingCalculationResult result = step.execute(SESSION_ID, TEST_DATE, records);
            assertEquals(1, result.getRatingDistribution().get("GOOD").size());
        }

        @Test
        @DisplayName("恰好60分应该是合格")
        void exactly60ShouldBePass() {
            List<ClassInspectionRecord> records = List.of(
                    createRecord(1L, new BigDecimal("60"))
            );
            RatingCalculationResult result = step.execute(SESSION_ID, TEST_DATE, records);
            assertEquals(1, result.getRatingDistribution().get("PASS").size());
        }

        @Test
        @DisplayName("59.99分应该是不合格")
        void just59Point99ShouldBeFail() {
            List<ClassInspectionRecord> records = List.of(
                    createRecord(1L, new BigDecimal("59.99"))
            );
            RatingCalculationResult result = step.execute(SESSION_ID, TEST_DATE, records);
            assertEquals(1, result.getRatingDistribution().get("FAIL").size());
        }

        @Test
        @DisplayName("finalScore为零时应该使用fallback计算(base=100)")
        void zeroFinalScoreShouldFallbackToBaseScore() {
            // finalScore == 0 triggers fallback: base(100) - deduction(0) + bonus(0) = 100 → EXCELLENT
            List<ClassInspectionRecord> records = List.of(
                    createRecord(1L, BigDecimal.ZERO)
            );
            RatingCalculationResult result = step.execute(SESSION_ID, TEST_DATE, records);
            assertEquals(1, result.getRatingDistribution().get("EXCELLENT").size());
        }

        @Test
        @DisplayName("通过扣分使得分低于60应该是不合格")
        void highDeductionShouldBeFail() {
            List<ClassInspectionRecord> records = List.of(
                    createRecordWithDeduction(1L, 100, new BigDecimal("50"), BigDecimal.ZERO)
            );
            RatingCalculationResult result = step.execute(SESSION_ID, TEST_DATE, records);
            // 100 - 50 + 0 = 50 → FAIL
            assertEquals(1, result.getRatingDistribution().get("FAIL").size());
        }

        @Test
        @DisplayName("100分应该是优秀")
        void hundredShouldBeExcellent() {
            List<ClassInspectionRecord> records = List.of(
                    createRecord(1L, new BigDecimal("100"))
            );
            RatingCalculationResult result = step.execute(SESSION_ID, TEST_DATE, records);
            assertEquals(1, result.getRatingDistribution().get("EXCELLENT").size());
        }
    }
}
