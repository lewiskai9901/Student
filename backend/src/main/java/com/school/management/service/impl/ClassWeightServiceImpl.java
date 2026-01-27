package com.school.management.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.management.dto.ClassWeightResultDTO;
import com.school.management.entity.*;
import com.school.management.entity.record.CheckRecordNew;
import com.school.management.entity.record.CheckRecordDeductionNew;
import com.school.management.mapper.*;
import com.school.management.mapper.record.CheckRecordMapper;
import com.school.management.mapper.record.CheckRecordDeductionMapper;
import com.school.management.service.ClassWeightService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.school.management.entity.DailyCheck;
import com.school.management.entity.DailyCheckTarget;
import com.school.management.mapper.DailyCheckMapper;
import com.school.management.mapper.DailyCheckTargetMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 班级加权服务实现类
 * 核心业务:班级人数加权计算引擎
 * 使用新版表结构：check_records_new, check_record_deductions_new
 *
 * @author system
 * @version 3.0.0
 * @since 2024-12-29
 */
@Slf4j
@Service
public class ClassWeightServiceImpl implements ClassWeightService {

    private final ClassWeightConfigMapper weightConfigMapper;
    private final ClassSizeStandardMapper standardMapper;
    private final ClassSizeSnapshotMapper snapshotMapper;
    private final ClassMapper classMapper;
    private final GradeMapper gradeMapper;
    private final CategoryWeightRuleMapper categoryWeightRuleMapper;
    private final CheckRecordMapper checkRecordMapper;
    private final CheckRecordDeductionMapper deductionMapper;
    private final DailyCheckMapper dailyCheckMapper;
    private final DailyCheckTargetMapper dailyCheckTargetMapper;

    public ClassWeightServiceImpl(
            ClassWeightConfigMapper weightConfigMapper,
            ClassSizeStandardMapper standardMapper,
            ClassSizeSnapshotMapper snapshotMapper,
            ClassMapper classMapper,
            GradeMapper gradeMapper,
            CategoryWeightRuleMapper categoryWeightRuleMapper,
            @Qualifier("newCheckRecordMapper") CheckRecordMapper checkRecordMapper,
            @Qualifier("newCheckRecordDeductionMapper") CheckRecordDeductionMapper deductionMapper,
            DailyCheckMapper dailyCheckMapper,
            DailyCheckTargetMapper dailyCheckTargetMapper) {
        this.weightConfigMapper = weightConfigMapper;
        this.standardMapper = standardMapper;
        this.snapshotMapper = snapshotMapper;
        this.classMapper = classMapper;
        this.gradeMapper = gradeMapper;
        this.categoryWeightRuleMapper = categoryWeightRuleMapper;
        this.checkRecordMapper = checkRecordMapper;
        this.deductionMapper = deductionMapper;
        this.dailyCheckMapper = dailyCheckMapper;
        this.dailyCheckTargetMapper = dailyCheckTargetMapper;
    }

    @Override
    public ClassWeightResultDTO calculateWeightedScore(Long classId, BigDecimal originalScore,
                                                       LocalDate checkDate, Long recordId) {
        log.info("开始计算加权分数: classId={}, originalScore={}, checkDate={}, recordId={}",
                classId, originalScore, checkDate, recordId);

        // 1. 获取加权配置
        ClassWeightConfig config = getWeightConfigForClass(classId, checkDate);
        if (config == null || config.getEnableWeight() == 0) {
            log.info("未找到有效的加权配置或加权未启用,返回原始分数");
            return buildNoWeightResult(classId, originalScore);
        }

        // 2. 获取实际班级人数
        Integer actualSize = getActualClassSize(classId, checkDate, recordId);
        if (actualSize == null || actualSize == 0) {
            log.warn("无法获取班级人数,返回原始分数");
            return buildNoWeightResult(classId, originalScore);
        }

        // 3. 获取标准人数
        com.school.management.entity.Class clazz = classMapper.selectById(classId);
        Long semesterId = getCurrentSemesterId(); // 需要从配置或上下文获取
        Integer standardSize = getStandardSize(classId, semesterId, checkDate);
        if (standardSize == null || standardSize == 0) {
            log.warn("无法获取标准人数,返回原始分数");
            return buildNoWeightResult(classId, originalScore);
        }

        // 4. 计算权重系数
        BigDecimal weightFactor = calculateWeightFactor(actualSize, standardSize, config);

        // 5. 计算加权后分数
        BigDecimal weightedScore = originalScore.multiply(weightFactor)
                .setScale(2, RoundingMode.HALF_UP);

        // 6. 构建返回结果
        ClassWeightResultDTO result = buildWeightResult(
                classId, originalScore, weightFactor, config, actualSize, standardSize
        );
        result.setWeightedScore(weightedScore);
        result.setScoreDifference(weightedScore.subtract(originalScore));
        result.setWeightApplied(true);

        log.info("加权计算完成: 原始分={}, 加权分={}, 权重系数={}, 实际人数={}, 标准人数={}",
                originalScore, weightedScore, weightFactor, actualSize, standardSize);

        return result;
    }

    @Override
    public List<ClassWeightResultDTO> batchCalculateWeightedScore(List<Long> classIds,
                                                                   BigDecimal originalScore,
                                                                   LocalDate checkDate,
                                                                   Long recordId) {
        log.info("批量计算加权分数: classIds.size={}, originalScore={}", classIds.size(), originalScore);

        return classIds.stream()
                .map(classId -> calculateWeightedScore(classId, originalScore, checkDate, recordId))
                .collect(Collectors.toList());
    }

    @Override
    public BigDecimal getWeightFactor(Long classId, LocalDate checkDate) {
        ClassWeightConfig config = getWeightConfigForClass(classId, checkDate);
        if (config == null || config.getEnableWeight() == 0) {
            return BigDecimal.ONE;
        }

        Integer actualSize = getActualClassSize(classId, checkDate, null);
        Long semesterId = getCurrentSemesterId();
        Integer standardSize = getStandardSize(classId, semesterId, checkDate);

        if (actualSize == null || standardSize == null || standardSize == 0) {
            return BigDecimal.ONE;
        }

        return calculateWeightFactor(actualSize, standardSize, config);
    }

    @Override
    public Integer getStandardSize(Long classId, Long semesterId, LocalDate checkDate) {
        com.school.management.entity.Class clazz = classMapper.selectById(classId);
        if (clazz == null) {
            log.error("班级不存在: classId={}", classId);
            return null;
        }

        // 获取加权配置,检查标准人数模式
        ClassWeightConfig config = getWeightConfigForClass(classId, checkDate != null ? checkDate : LocalDate.now());
        if (config != null && config.getStandardSizeMode() != null) {
            String sizeMode = config.getStandardSizeMode();
            log.info("检测到标准人数模式: {}", sizeMode);

            // FIXED 模式: 使用配置的固定标准人数
            if ("FIXED".equals(sizeMode)) {
                Integer fixedSize = config.getStandardSize();
                if (fixedSize != null && fixedSize > 0) {
                    log.info("使用固定模式的标准人数: {}", fixedSize);
                    return fixedSize;
                }
            }

            // TARGET_AVERAGE 模式: 根据检查目标班级计算平均人数
            if ("TARGET_AVERAGE".equals(sizeMode)) {
                log.info("TARGET_AVERAGE模式: 尝试根据检查目标班级计算平均人数");

                // 1. 尝试通过检查日期找到对应的日常检查
                if (checkDate != null) {
                    DailyCheck dailyCheck = dailyCheckMapper.selectOne(
                            new LambdaQueryWrapper<DailyCheck>()
                                    .eq(DailyCheck::getCheckDate, checkDate)
                                    .eq(DailyCheck::getDeleted, 0)
                                    .last("LIMIT 1")
                    );

                    if (dailyCheck != null) {
                        Integer targetAvgSize = calculateTargetAverageSize(dailyCheck.getId());
                        if (targetAvgSize != null && targetAvgSize > 0) {
                            log.info("TARGET_AVERAGE模式: 从检查目标计算得到标准人数={}", targetAvgSize);
                            return targetAvgSize;
                        }
                    }
                }

                // 2. 如果无法计算，使用配置的standard_size作为回退值
                Integer fixedSize = config.getStandardSize();
                if (fixedSize != null && fixedSize > 0) {
                    log.warn("TARGET_AVERAGE模式: 无法计算目标平均人数，使用回退值: {}", fixedSize);
                    return fixedSize;
                }
                log.warn("TARGET_AVERAGE模式: 无法计算目标平均人数，且未配置回退的standard_size");
                return null;
            }

            // RANGE_AVERAGE 模式: 根据配置的范围计算平均人数
            if ("RANGE_AVERAGE".equals(sizeMode)) {
                Integer rangeAvgSize = calculateRangeAverageSize(config);
                if (rangeAvgSize != null) {
                    log.info("使用范围平均模式的标准人数: {}", rangeAvgSize);
                    return rangeAvgSize;
                }
            }
        }

        // 三层优先级系统: 全局配置 > 实时计算
        // 注：新版schema不支持检查记录级别的自定义标准人数和快照功能

        // 层级3: 查询全局标准人数配置(按优先级: 学期+部门+年级 > 学期+部门 > 学期)
        ClassSizeStandard standard = standardMapper.selectStandardSize(
                semesterId,
                clazz.getOrgUnitId(),
                clazz.getGradeLevel()
        );

        if (standard != null) {
            com.school.management.enums.StandardSizeMode mode = standard.getStandardModeEnum();

            // 固定模式: 使用配置的固定标准人数
            if (mode.isFixed()) {
                log.info("使用固定模式的标准人数: {}", standard.getStandardSize());
                return standard.getStandardSize();
            }

            // 动态模式: 实时计算平均人数(fall through到层级4)
            log.info("配置为动态模式,将实时计算平均人数");
        }

        // 层级4: 实时计算同年级同组织单元的平均人数
        Integer avgSize = calculateAverageClassSize(clazz.getOrgUnitId(), clazz.getGradeLevel());
        log.info("使用实时计算的平均人数: {}", avgSize);
        return avgSize;
    }

    @Override
    public Integer getStandardSize(Long classId, Long semesterId, LocalDate checkDate, Long recordId) {
        // 委托给三参数版本，recordId暂不使用（TARGET_AVERAGE模式通过checkDate查找）
        return getStandardSize(classId, semesterId, checkDate);
    }

    @Override
    public Integer getActualClassSize(Long classId, LocalDate checkDate, Long recordId) {
        // 1. 优先使用快照人数(如果有检查记录ID)
        if (recordId != null) {
            ClassSizeSnapshot snapshot = snapshotMapper.selectOne(
                    new LambdaQueryWrapper<ClassSizeSnapshot>()
                            .eq(ClassSizeSnapshot::getRecordId, recordId)
                            .eq(ClassSizeSnapshot::getClassId, classId)
                            .last("LIMIT 1")
            );
            if (snapshot != null) {
                log.info("使用快照人数: recordId={}, classId={}, studentCount={}",
                        recordId, classId, snapshot.getStudentCount());
                // 增加快照使用次数
                snapshotMapper.increaseUsageCount(snapshot.getId());
                return snapshot.getStudentCount();
            }
        }

        // 2. 查询指定日期的快照
        if (checkDate != null) {
            ClassSizeSnapshot snapshot = snapshotMapper.selectByClassAndDate(classId, checkDate);
            if (snapshot != null) {
                log.info("使用日期快照人数: checkDate={}, classId={}, studentCount={}",
                        checkDate, classId, snapshot.getStudentCount());
                return snapshot.getStudentCount();
            }
        }

        // 3. 使用实时班级人数
        com.school.management.entity.Class clazz = classMapper.selectById(classId);
        if (clazz != null) {
            log.info("使用实时班级人数: classId={}, studentCount={}", classId, clazz.getStudentCount());
            return clazz.getStudentCount();
        }

        log.error("无法获取班级人数: classId={}", classId);
        return null;
    }

    @Override
    public ClassWeightConfig getWeightConfigForClass(Long classId, LocalDate currentDate) {
        com.school.management.entity.Class clazz = classMapper.selectById(classId);
        if (clazz == null) {
            log.error("班级不存在: classId={}", classId);
            return null;
        }

        // 按优先级查询: 班级 > 年级 > 部门 > 全局
        ClassWeightConfig config = weightConfigMapper.selectConfigForClass(
                classId,
                clazz.getGradeLevel() != null ? clazz.getGradeLevel().longValue() : null,
                clazz.getOrgUnitId(),
                currentDate
        );

        if (config != null) {
            log.info("找到加权配置: configCode={}, weightMode={}, applyScope={}",
                    config.getConfigCode(), config.getWeightMode(), config.getApplyScope());
        } else {
            log.warn("未找到有效的加权配置: classId={}", classId);
        }

        return config;
    }

    @Override
    public boolean shouldApplyWeight(Long classId, Long recordId, Long categoryId) {
        // 1. 检查全局配置级开关
        ClassWeightConfig config = getWeightConfigForClass(classId, LocalDate.now());
        if (config == null || config.getEnableWeight() == 0) {
            return false;
        }

        // 2. 检查检查记录级开关（新版schema不支持记录级加权开关，跳过此检查）

        // 3. 检查类别级开关(如果有类别ID)
        if (categoryId != null && config.getId() != null) {
            CategoryWeightRule categoryRule = categoryWeightRuleMapper.selectByConfigAndCategory(
                    config.getId(), categoryId
            );
            if (categoryRule != null && categoryRule.getEnableWeight() == 0) {
                return false;
            }
        }

        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean recalculateClassStats(Long recordId, Long classId) {
        log.info("重新计算班级统计: recordId={}, classId={}", recordId, classId);

        try {
            // 1. 查询班级的所有扣分明细
            List<CheckRecordDeductionNew> deductions = deductionMapper.selectList(
                    new LambdaQueryWrapper<CheckRecordDeductionNew>()
                            .eq(CheckRecordDeductionNew::getRecordId, recordId)
                            .eq(CheckRecordDeductionNew::getClassId, classId)
            );
            if (deductions.isEmpty()) {
                log.warn("没有找到扣分明细: recordId={}, classId={}", recordId, classId);
                return false;
            }

            // 2. 获取检查记录
            CheckRecordNew record = checkRecordMapper.selectById(recordId);
            if (record == null) {
                log.error("检查记录不存在");
                return false;
            }

            // 3. 分类别统计原始分数
            Map<Long, BigDecimal> categoryScores = deductions.stream()
                    .filter(d -> d.getCategoryId() != null)
                    .collect(Collectors.groupingBy(
                            CheckRecordDeductionNew::getCategoryId,
                            Collectors.reducing(
                                    BigDecimal.ZERO,
                                    d -> d.getActualScore() != null ? d.getActualScore() : BigDecimal.ZERO,
                                    BigDecimal::add
                            )
                    ));

            // 4. 对每个类别应用加权
            BigDecimal totalWeightedScore = BigDecimal.ZERO;
            for (Map.Entry<Long, BigDecimal> entry : categoryScores.entrySet()) {
                Long categoryId = entry.getKey();
                BigDecimal categoryScore = entry.getValue();

                // 检查是否应用加权
                if (shouldApplyWeight(classId, recordId, categoryId)) {
                    ClassWeightResultDTO result = calculateWeightedScore(
                            classId, categoryScore, record.getCheckDate(), recordId
                    );
                    totalWeightedScore = totalWeightedScore.add(result.getWeightedScore());
                } else {
                    totalWeightedScore = totalWeightedScore.add(categoryScore);
                }
            }

            // 5. 更新检查记录的班级总分
            // 注意: 这里需要根据实际的表结构更新相应字段
            log.info("重算完成: 加权后总分={}", totalWeightedScore);
            return true;

        } catch (Exception e) {
            log.error("重新计算班级统计失败", e);
            throw e;
        }
    }

    @Override
    public BigDecimal calculateSegmentWeight(Integer actualSize, String segmentRules) {
        if (segmentRules == null || segmentRules.trim().isEmpty()) {
            log.warn("分段规则为空");
            return BigDecimal.ONE;
        }

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode segments = objectMapper.readTree(segmentRules);
            for (JsonNode segment : segments) {
                Integer minSize = segment.has("minSize") ? segment.get("minSize").asInt() : null;
                Integer maxSize = segment.has("maxSize") ? segment.get("maxSize").asInt() : null;
                BigDecimal weight = new BigDecimal(segment.get("weight").asText());

                if ((minSize == null || actualSize >= minSize) &&
                        (maxSize == null || actualSize <= maxSize)) {
                    log.info("匹配到分段规则: [{}-{}], weight={}", minSize, maxSize, weight);
                    return weight;
                }
            }

            log.warn("未匹配到任何分段规则,返回默认权重1.0");
            return BigDecimal.ONE;

        } catch (Exception e) {
            log.error("解析分段规则失败: {}", segmentRules, e);
            return BigDecimal.ONE;
        }
    }

    @Override
    public BigDecimal applyWeightLimit(BigDecimal weightFactor, ClassWeightConfig config) {
        if (config.getEnableWeightLimit() == 0) {
            return weightFactor;
        }

        BigDecimal minWeight = config.getMinWeight();
        BigDecimal maxWeight = config.getMaxWeight();
        BigDecimal limited = weightFactor;

        if (minWeight != null && weightFactor.compareTo(minWeight) < 0) {
            limited = minWeight;
            log.info("权重系数低于下限: {} < {}, 调整为{}", weightFactor, minWeight, limited);
        }

        if (maxWeight != null && weightFactor.compareTo(maxWeight) > 0) {
            limited = maxWeight;
            log.info("权重系数超过上限: {} > {}, 调整为{}", weightFactor, maxWeight, limited);
        }

        return limited;
    }

    @Override
    public ClassWeightResultDTO buildWeightResult(Long classId, BigDecimal originalScore,
                                                   BigDecimal weightFactor, ClassWeightConfig config,
                                                   Integer actualSize, Integer standardSize) {
        ClassWeightResultDTO result = new ClassWeightResultDTO();
        result.setClassId(classId);
        result.setOriginalScore(originalScore);
        result.setWeightFactor(weightFactor);
        result.setActualClassSize(actualSize);
        result.setStandardClassSize(standardSize);
        result.setWeightMode(config.getWeightMode());
        result.setMinWeight(config.getMinWeight());
        result.setMaxWeight(config.getMaxWeight());

        // 判断是否受限
        boolean isLimited = false;
        String limitType = "NONE";
        if (config.getEnableWeightLimit() == 1) {
            if (config.getMinWeight() != null && weightFactor.compareTo(config.getMinWeight()) <= 0) {
                isLimited = true;
                limitType = "MIN";
            } else if (config.getMaxWeight() != null && weightFactor.compareTo(config.getMaxWeight()) >= 0) {
                isLimited = true;
                limitType = "MAX";
            }
        }
        result.setLimited(isLimited);
        result.setLimitType(limitType);

        return result;
    }

    // ========== 私有辅助方法 ==========

    /**
     * 计算权重系数
     */
    private BigDecimal calculateWeightFactor(Integer actualSize, Integer standardSize,
                                              ClassWeightConfig config) {
        BigDecimal weightFactor;

        switch (config.getWeightMode()) {
            case "STANDARD":
                // 标准人数模式: 权重 = 标准人数 / 实际人数
                weightFactor = BigDecimal.valueOf(standardSize)
                        .divide(BigDecimal.valueOf(actualSize), 4, RoundingMode.HALF_UP);
                break;

            case "PER_CAPITA":
                // 人均模式: 权重 = 实际人数 / 标准人数
                weightFactor = BigDecimal.valueOf(actualSize)
                        .divide(BigDecimal.valueOf(standardSize), 4, RoundingMode.HALF_UP);
                break;

            case "SEGMENT":
                // 分段模式: 根据人数区间返回固定权重
                weightFactor = calculateSegmentWeight(actualSize, config.getSegmentRules());
                break;

            case "NONE":
            default:
                weightFactor = BigDecimal.ONE;
                break;
        }

        // 应用上下限
        return applyWeightLimit(weightFactor, config);
    }

    /**
     * 计算平均班级人数
     */
    private Integer calculateAverageClassSize(Long orgUnitId, Integer gradeLevel) {
        // 查询同组织单元同年级的所有班级
        List<com.school.management.entity.Class> classes = classMapper.selectList(
                new LambdaQueryWrapper<com.school.management.entity.Class>()
                        .eq(com.school.management.entity.Class::getOrgUnitId, orgUnitId)
                        .eq(com.school.management.entity.Class::getGradeLevel, gradeLevel)
                        .eq(com.school.management.entity.Class::getDeleted, 0)
        );

        if (classes.isEmpty()) {
            log.warn("未找到同组织单元同年级的班级,使用默认值40");
            return 40; // 默认值
        }

        // 检查班级数量是否足够(至少3个班级才有统计意义)
        if (classes.size() < 3) {
            log.warn("班级数量不足3个(实际{}个),样本过少,使用默认值40。建议使用FIXED固定标准模式!", classes.size());
            return 40; // 样本过少,使用默认值
        }

        double average = classes.stream()
                .mapToInt(c -> c.getStudentCount() != null ? c.getStudentCount() : 0)
                .average()
                .orElse(40.0);

        int result = (int) Math.round(average);
        log.info("计算平均班级人数: 组织单元={}, 年级={}, 班级数={}, 平均人数={}",
                orgUnitId, gradeLevel, classes.size(), result);

        return result;
    }

    /**
     * 获取当前学期ID
     * 优先级: 1. 从请求上下文获取 2. 从系统配置表获取 3. 使用默认值
     */
    private Long getCurrentSemesterId() {
        // 方案1: 从ThreadLocal或Request Context获取当前学期(如果已实现)
        // Long semesterId = RequestContextHolder.getCurrentSemesterId();
        // if (semesterId != null) {
        //     return semesterId;
        // }

        // 方案2: 从系统配置表获取当前学期(推荐)
        // SystemConfig config = systemConfigMapper.selectByConfigKey("current_semester_id");
        // if (config != null && config.getConfigValue() != null) {
        //     return Long.parseLong(config.getConfigValue());
        // }

        // 方案3: 根据当前时间自动计算学期
        // return calculateCurrentSemesterByDate(LocalDate.now());

        // 使用默认值(开发环境)
        log.debug("使用默认学期ID: 1");
        return 1L;
    }

    /**
     * 构建无加权结果
     */
    private ClassWeightResultDTO buildNoWeightResult(Long classId, BigDecimal originalScore) {
        ClassWeightResultDTO result = new ClassWeightResultDTO();
        result.setClassId(classId);
        result.setOriginalScore(originalScore);
        result.setWeightedScore(originalScore);
        result.setWeightFactor(BigDecimal.ONE);
        result.setScoreDifference(BigDecimal.ZERO);
        result.setWeightApplied(false);
        result.setWeightMode("NONE");
        return result;
    }

    // 注：新版schema不支持自定义标准人数和快照功能，已移除parseCustomStandardSize和getSnapshotStandardSize方法

    /**
     * 计算范围平均人数(RANGE_AVERAGE模式)
     * 支持多范围组合,并处理交集去重
     *
     * @param config 加权配置,包含rangeDepartments, rangeGrades, rangeClasses
     * @return 平均人数,如果没有符合条件的班级则返回null
     */
    private Integer calculateRangeAverageSize(ClassWeightConfig config) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            java.util.Set<Long> classIdSet = new java.util.HashSet<>(); // 使用Set自动去重

            // 1. 解析并收集组织单元范围内的班级
            if (config.getRangeDepartments() != null && !config.getRangeDepartments().trim().isEmpty()) {
                JsonNode orgUnitIds = objectMapper.readTree(config.getRangeDepartments());
                for (JsonNode orgUnitIdNode : orgUnitIds) {
                    Long orgUnitId = orgUnitIdNode.asLong();
                    List<com.school.management.entity.Class> orgUnitClasses = classMapper.selectList(
                            new LambdaQueryWrapper<com.school.management.entity.Class>()
                                    .eq(com.school.management.entity.Class::getOrgUnitId, orgUnitId)
                                    .eq(com.school.management.entity.Class::getDeleted, 0)
                    );
                    orgUnitClasses.forEach(c -> classIdSet.add(c.getId()));
                    log.info("组织单元 {} 收集到 {} 个班级", orgUnitId, orgUnitClasses.size());
                }
            }

            // 2. 解析并收集年级范围内的班级
            if (config.getRangeGrades() != null && !config.getRangeGrades().trim().isEmpty()) {
                JsonNode gradeNos = objectMapper.readTree(config.getRangeGrades());
                for (JsonNode gradeNode : gradeNos) {
                    String gradeNo = gradeNode.asText();
                    // 年级编号可能是字符串,需要查询对应的gradeLevel
                    List<com.school.management.entity.Class> gradeClasses = classMapper.selectList(
                            new LambdaQueryWrapper<com.school.management.entity.Class>()
                                    .eq(com.school.management.entity.Class::getGradeLevel, Integer.parseInt(gradeNo))
                                    .eq(com.school.management.entity.Class::getDeleted, 0)
                    );
                    gradeClasses.forEach(c -> classIdSet.add(c.getId()));
                    log.info("年级 {} 收集到 {} 个班级", gradeNo, gradeClasses.size());
                }
            }

            // 3. 解析并收集指定的班级
            if (config.getRangeClasses() != null && !config.getRangeClasses().trim().isEmpty()) {
                JsonNode classIds = objectMapper.readTree(config.getRangeClasses());
                for (JsonNode classIdNode : classIds) {
                    Long classId = classIdNode.asLong();
                    classIdSet.add(classId);
                }
                log.info("直接选择的班级数: {}", classIds.size());
            }

            // 4. 如果没有选择任何范围,返回null
            if (classIdSet.isEmpty()) {
                log.warn("RANGE_AVERAGE模式未配置任何范围,返回null");
                return null;
            }

            // 5. 查询所有去重后的班级,计算平均人数
            List<com.school.management.entity.Class> allClasses = classMapper.selectList(
                    new LambdaQueryWrapper<com.school.management.entity.Class>()
                            .in(com.school.management.entity.Class::getId, classIdSet)
                            .eq(com.school.management.entity.Class::getDeleted, 0)
            );

            if (allClasses.isEmpty()) {
                log.warn("RANGE_AVERAGE模式未找到有效的班级,返回null");
                return null;
            }

            // 6. 计算平均人数
            double avgSize = allClasses.stream()
                    .mapToInt(c -> c.getStudentCount() != null ? c.getStudentCount() : 0)
                    .average()
                    .orElse(0.0);

            int result = (int) Math.round(avgSize);

            // 7. 计算总人数用于日志
            int totalStudents = allClasses.stream()
                    .mapToInt(c -> c.getStudentCount() != null ? c.getStudentCount() : 0)
                    .sum();

            log.info("RANGE_AVERAGE计算完成: 去重后班级数={}, 总人数={}, 平均人数={}",
                    allClasses.size(), totalStudents, result);

            return result;

        } catch (Exception e) {
            log.error("计算范围平均人数失败", e);
            return null;
        }
    }

    /**
     * 计算检查目标班级的平均人数
     * 用于 TARGET_AVERAGE 标准人数模式
     *
     * @param dailyCheckId 日常检查ID
     * @return 目标班级的平均人数，如果无法计算则返回 null
     */
    private Integer calculateTargetAverageSize(Long dailyCheckId) {
        try {
            // 1. 获取检查目标班级列表 (target_type=1 表示班级)
            LambdaQueryWrapper<DailyCheckTarget> targetWrapper = new LambdaQueryWrapper<>();
            targetWrapper.eq(DailyCheckTarget::getCheckId, dailyCheckId)
                    .eq(DailyCheckTarget::getTargetType, 1);  // 1=班级
            List<DailyCheckTarget> targets = dailyCheckTargetMapper.selectList(targetWrapper);

            if (targets == null || targets.isEmpty()) {
                log.warn("日常检查ID={}没有班级类型的检查目标，无法计算TARGET_AVERAGE", dailyCheckId);
                return null;
            }

            // 2. 获取所有目标班级的人数
            List<Long> classIds = targets.stream()
                    .map(DailyCheckTarget::getTargetId)
                    .collect(Collectors.toList());

            int totalStudents = 0;
            int validClassCount = 0;

            for (Long classId : classIds) {
                com.school.management.entity.Class classEntity = classMapper.selectById(classId);
                if (classEntity != null && classEntity.getStudentCount() != null && classEntity.getStudentCount() > 0) {
                    totalStudents += classEntity.getStudentCount();
                    validClassCount++;
                }
            }

            if (validClassCount == 0) {
                log.warn("日常检查ID={}的目标班级都没有有效的学生数量", dailyCheckId);
                return null;
            }

            // 3. 计算平均值（四舍五入）
            int averageSize = Math.round((float) totalStudents / validClassCount);
            log.info("TARGET_AVERAGE计算: 日常检查ID={}, 班级数={}, 总学生数={}, 平均人数={}",
                    dailyCheckId, validClassCount, totalStudents, averageSize);

            return averageSize;
        } catch (Exception e) {
            log.error("计算TARGET_AVERAGE标准人数失败: dailyCheckId={}", dailyCheckId, e);
            return null;
        }
    }
}
