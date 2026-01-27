package com.school.management.domain.teaching.model.valueobject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 评分制值对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GradeScale {

    /**
     * 等级
     */
    private String level;

    /**
     * 绩点
     */
    private BigDecimal gradePoint;

    /**
     * 是否通过
     */
    private boolean passed;

    /**
     * 根据百分制成绩计算等级和绩点
     */
    public static GradeScale fromScore(BigDecimal score, Integer scaleType) {
        if (score == null) {
            return GradeScale.builder()
                    .level(null)
                    .gradePoint(null)
                    .passed(false)
                    .build();
        }

        double s = score.doubleValue();

        return switch (scaleType) {
            case 1 -> fromHundredScale(s);
            case 2 -> fromFiveLevel(s);
            case 3 -> fromPassFail(s);
            default -> fromHundredScale(s);
        };
    }

    /**
     * 百分制转换
     */
    private static GradeScale fromHundredScale(double score) {
        String level;
        BigDecimal gp;
        boolean passed;

        if (score >= 90) {
            level = "A";
            gp = BigDecimal.valueOf(4.0);
            passed = true;
        } else if (score >= 85) {
            level = "A-";
            gp = BigDecimal.valueOf(3.7);
            passed = true;
        } else if (score >= 82) {
            level = "B+";
            gp = BigDecimal.valueOf(3.3);
            passed = true;
        } else if (score >= 78) {
            level = "B";
            gp = BigDecimal.valueOf(3.0);
            passed = true;
        } else if (score >= 75) {
            level = "B-";
            gp = BigDecimal.valueOf(2.7);
            passed = true;
        } else if (score >= 72) {
            level = "C+";
            gp = BigDecimal.valueOf(2.3);
            passed = true;
        } else if (score >= 68) {
            level = "C";
            gp = BigDecimal.valueOf(2.0);
            passed = true;
        } else if (score >= 64) {
            level = "C-";
            gp = BigDecimal.valueOf(1.7);
            passed = true;
        } else if (score >= 60) {
            level = "D";
            gp = BigDecimal.valueOf(1.0);
            passed = true;
        } else {
            level = "F";
            gp = BigDecimal.valueOf(0.0);
            passed = false;
        }

        return GradeScale.builder()
                .level(level)
                .gradePoint(gp)
                .passed(passed)
                .build();
    }

    /**
     * 五级制转换（优秀/良好/中等/及格/不及格）
     */
    private static GradeScale fromFiveLevel(double score) {
        String level;
        BigDecimal gp;
        boolean passed;

        if (score >= 90) {
            level = "优秀";
            gp = BigDecimal.valueOf(4.0);
            passed = true;
        } else if (score >= 80) {
            level = "良好";
            gp = BigDecimal.valueOf(3.0);
            passed = true;
        } else if (score >= 70) {
            level = "中等";
            gp = BigDecimal.valueOf(2.0);
            passed = true;
        } else if (score >= 60) {
            level = "及格";
            gp = BigDecimal.valueOf(1.0);
            passed = true;
        } else {
            level = "不及格";
            gp = BigDecimal.valueOf(0.0);
            passed = false;
        }

        return GradeScale.builder()
                .level(level)
                .gradePoint(gp)
                .passed(passed)
                .build();
    }

    /**
     * 二级制（通过/不通过）
     */
    private static GradeScale fromPassFail(double score) {
        boolean passed = score >= 60;
        return GradeScale.builder()
                .level(passed ? "通过" : "不通过")
                .gradePoint(passed ? BigDecimal.ONE : BigDecimal.ZERO)
                .passed(passed)
                .build();
    }

    /**
     * 获取评分制名称
     */
    public static String getScaleTypeName(Integer scaleType) {
        return switch (scaleType) {
            case 1 -> "百分制";
            case 2 -> "五级制";
            case 3 -> "二级制";
            default -> "未知";
        };
    }
}
