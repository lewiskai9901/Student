package com.school.management.domain.teaching.service;

import com.school.management.domain.teaching.model.entity.ScheduleEntry;
import com.school.management.domain.teaching.model.valueobject.ScheduleConflict;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 排课领域服务接口
 */
public interface SchedulingDomainService {

    /**
     * 自动排课
     *
     * @param request 排课请求
     * @return 排课结果
     */
    SchedulingResult autoGenerate(SchedulingRequest request);

    /**
     * 使用遗传算法生成课表
     *
     * @param tasks          教学任务列表
     * @param scheduleId     课表ID
     * @param maxIterations  最大迭代次数
     * @param populationSize 种群大小
     * @param mutationRate   变异率
     * @return 生成的排课条目列表
     */
    List<ScheduleEntry> generateSchedule(List<com.school.management.domain.teaching.model.aggregate.TeachingTask> tasks,
                                          Long scheduleId,
                                          int maxIterations,
                                          int populationSize,
                                          double mutationRate);

    /**
     * 冲突检测
     *
     * @param entry 待检测的排课条目
     * @return 冲突列表
     */
    List<ScheduleConflict> detectConflicts(ScheduleEntry entry);

    /**
     * 验证排课条目是否有效
     *
     * @param entry 排课条目
     * @return 验证结果
     */
    ValidationResult validate(ScheduleEntry entry);

    /**
     * 排课请求
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class SchedulingRequest {
        /**
         * 学期ID
         */
        private Long semesterId;

        /**
         * 待排教学任务ID列表
         */
        private List<Long> taskIds;

        /**
         * 硬约束条件
         */
        private SchedulingConstraints constraints;

        /**
         * 软约束偏好
         */
        private SchedulingPreferences preferences;

        /**
         * 最大迭代次数
         */
        @Builder.Default
        private Integer maxIterations = 500;
    }

    /**
     * 排课约束（硬约束）
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class SchedulingConstraints {
        /**
         * 教师不冲突
         */
        @Builder.Default
        private boolean noTeacherConflict = true;

        /**
         * 教室不冲突
         */
        @Builder.Default
        private boolean noClassroomConflict = true;

        /**
         * 班级不冲突
         */
        @Builder.Default
        private boolean noClassConflict = true;

        /**
         * 教室容量匹配
         */
        @Builder.Default
        private boolean matchClassroomCapacity = true;

        /**
         * 教室类型匹配
         */
        @Builder.Default
        private boolean matchClassroomType = true;

        /**
         * 可用星期（如[1,2,3,4,5]表示周一到周五）
         */
        private List<Integer> availableWeekdays;

        /**
         * 可用节次
         */
        private List<Integer> availableSlots;

        /**
         * 每天最多课时
         */
        private Integer maxDailyHours;

        /**
         * 最多连续课时
         */
        private Integer maxContinuousHours;
    }

    /**
     * 排课偏好（软约束）
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class SchedulingPreferences {
        /**
         * 尊重教师时间偏好
         */
        @Builder.Default
        private boolean respectTeacherPreference = true;

        /**
         * 教师偏好权重
         */
        @Builder.Default
        private Integer teacherPreferenceWeight = 10;

        /**
         * 专业课尽量上午
         */
        @Builder.Default
        private boolean morningForMajorCourses = true;

        /**
         * 实验课尽量连堂
         */
        @Builder.Default
        private boolean continuousForLabCourses = true;

        /**
         * 课程均匀分布
         */
        @Builder.Default
        private boolean evenDistribution = true;

        /**
         * 避免孤立课
         */
        @Builder.Default
        private boolean avoidSingleSlot = true;
    }

    /**
     * 排课结果
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class SchedulingResult {
        /**
         * 是否成功
         */
        private boolean success;

        /**
         * 生成的排课条目
         */
        private List<ScheduleEntry> entries;

        /**
         * 冲突列表
         */
        private List<ScheduleConflict> conflicts;

        /**
         * 适应度分数（0-1）
         */
        private Double fitnessScore;

        /**
         * 迭代次数
         */
        private Integer iterations;

        /**
         * 消息
         */
        private String message;

        public static SchedulingResult success(List<ScheduleEntry> entries, double fitness, int iterations) {
            return SchedulingResult.builder()
                    .success(true)
                    .entries(entries)
                    .fitnessScore(fitness)
                    .iterations(iterations)
                    .message("排课成功")
                    .build();
        }

        public static SchedulingResult partial(List<ScheduleEntry> entries, List<ScheduleConflict> conflicts,
                                               double fitness, int iterations) {
            return SchedulingResult.builder()
                    .success(false)
                    .entries(entries)
                    .conflicts(conflicts)
                    .fitnessScore(fitness)
                    .iterations(iterations)
                    .message("存在" + conflicts.size() + "个冲突")
                    .build();
        }

        public static SchedulingResult failure(String message) {
            return SchedulingResult.builder()
                    .success(false)
                    .message(message)
                    .build();
        }
    }

    /**
     * 验证结果
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class ValidationResult {
        private boolean valid;
        private List<String> errors;

        public static ValidationResult success() {
            return ValidationResult.builder().valid(true).build();
        }

        public static ValidationResult failure(List<String> errors) {
            return ValidationResult.builder().valid(false).errors(errors).build();
        }
    }
}
