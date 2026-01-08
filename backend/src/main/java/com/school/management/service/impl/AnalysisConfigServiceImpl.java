package com.school.management.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.school.management.dto.analysis.AnalysisConfigDTO;
import com.school.management.dto.analysis.AnalysisConfigQueryDTO;
import com.school.management.dto.analysis.AnalysisReportDTO;
import com.school.management.dto.analysis.AnalysisReportDTO.*;
import com.school.management.entity.AnalysisConfig;
import com.school.management.entity.Semester;
import com.school.management.exception.BusinessException;
import com.school.management.mapper.AnalysisConfigMapper;
import com.school.management.mapper.SemesterMapper;
import com.school.management.service.AnalysisConfigService;
import com.school.management.service.AnalysisExportService;
import com.school.management.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 统计分析配置Service实现类
 *
 * @author Claude
 * @since 2025-12-05
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisConfigServiceImpl extends ServiceImpl<AnalysisConfigMapper, AnalysisConfig>
        implements AnalysisConfigService {

    private final SemesterMapper semesterMapper;
    private final AnalysisExportService analysisExportService;

    @Override
    public IPage<AnalysisConfigDTO> getConfigPage(AnalysisConfigQueryDTO query) {
        Page<AnalysisConfigDTO> page = new Page<>(query.getPageNum(), query.getPageSize());
        return baseMapper.selectConfigPage(page, query);
    }

    @Override
    public AnalysisConfigDTO getConfigById(Long id) {
        AnalysisConfigDTO dto = baseMapper.selectConfigById(id);
        if (dto == null) {
            throw new BusinessException("配置不存在");
        }
        return dto;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createConfig(AnalysisConfigDTO dto) {
        validateConfig(dto);

        AnalysisConfig entity = new AnalysisConfig();
        BeanUtils.copyProperties(dto, entity);
        entity.setCreatedBy(SecurityUtils.getCurrentUserId());
        entity.setStatus(1);

        save(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateConfig(AnalysisConfigDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException("配置ID不能为空");
        }

        AnalysisConfig existing = getById(dto.getId());
        if (existing == null) {
            throw new BusinessException("配置不存在");
        }

        validateConfig(dto);

        AnalysisConfig entity = new AnalysisConfig();
        BeanUtils.copyProperties(dto, entity);

        return updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteConfig(Long id) {
        AnalysisConfig existing = getById(id);
        if (existing == null) {
            throw new BusinessException("配置不存在");
        }
        return removeById(id);
    }

    @Override
    public AnalysisReportDTO generateReport(Long configId) {
        AnalysisConfigDTO config = getConfigById(configId);

        // 计算时间范围
        LocalDate startDate = calculateStartDate(config);
        LocalDate endDate = calculateEndDate(config);

        // 获取目标ID
        Long targetId = getTargetId(config);

        // 构建报告
        AnalysisReportDTO report = new AnalysisReportDTO();
        report.setConfigId(configId);
        report.setConfigName(config.getName());
        report.setTargetType(config.getTargetType());
        report.setTargetName(config.getTargetName());
        report.setStartDate(startDate);
        report.setEndDate(endDate);
        report.setGeneratedAt(LocalDateTime.now());

        // 生成报告数据
        ReportData data = new ReportData();
        List<String> sections = config.getReportSections();

        if (sections == null || sections.isEmpty()) {
            sections = Arrays.asList("FREQUENCY_STATS", "SCORE_STATS", "TREND_ANALYSIS",
                    "DISTRIBUTION", "CLASS_RANKING", "STUDENT_RANKING");
        }

        for (String section : sections) {
            switch (section) {
                case "FREQUENCY_STATS":
                    data.setFrequencyStats(generateFrequencyStats(config, targetId, startDate, endDate));
                    break;
                case "SCORE_STATS":
                    data.setScoreStats(generateScoreStats(config, targetId, startDate, endDate));
                    break;
                case "TREND_ANALYSIS":
                    data.setTrendAnalysis(generateTrendAnalysis(config, targetId, startDate, endDate));
                    break;
                case "DISTRIBUTION":
                    data.setDistribution(generateDistribution(config, targetId, startDate, endDate));
                    break;
                case "CLASS_RANKING":
                    data.setClassRanking(generateClassRanking(config, targetId, startDate, endDate));
                    break;
                case "STUDENT_RANKING":
                    data.setStudentRanking(generateStudentRanking(config, targetId, startDate, endDate));
                    break;
                case "DEDUCTION_HEATMAP":
                    data.setDeductionHeatmap(generateHeatmap(config, targetId, startDate, endDate));
                    break;
                case "CATEGORY_RADAR":
                    data.setCategoryRadar(generateRadar(config, startDate, endDate));
                    break;
                case "COMPARE_ANALYSIS":
                    data.setCompareAnalysis(generateCompareAnalysis(config, targetId, startDate, endDate));
                    break;
            }
        }

        report.setData(data);
        return report;
    }

    @Override
    public byte[] exportToExcel(Long configId) {
        AnalysisReportDTO report = generateReport(configId);
        return analysisExportService.exportToExcel(report);
    }

    @Override
    public byte[] exportToPdf(Long configId) {
        AnalysisReportDTO report = generateReport(configId);
        return analysisExportService.exportToPdf(report);
    }

    // ==================== 私有方法 ====================

    private void validateConfig(AnalysisConfigDTO dto) {
        if (dto.getTargetType() == null) {
            throw new BusinessException("分析目标类型不能为空");
        }

        switch (dto.getTargetType()) {
            case "PLAN":
                if (dto.getPlanId() == null) {
                    throw new BusinessException("请选择检查计划");
                }
                break;
            case "TEMPLATE":
                if (dto.getTemplateId() == null) {
                    throw new BusinessException("请选择检查模板");
                }
                break;
            case "CATEGORY":
                if (dto.getCategoryId() == null) {
                    throw new BusinessException("请选择检查类别");
                }
                break;
            case "DEDUCTION_ITEM":
                if (dto.getDeductionItemId() == null) {
                    throw new BusinessException("请选择扣分项");
                }
                break;
            case "SINGLE_CHECK":
                if (dto.getCheckRecordId() == null) {
                    throw new BusinessException("请选择检查记录");
                }
                break;
            case "ORGANIZATION":
                if (dto.getDepartmentId() == null && dto.getClassId() == null) {
                    throw new BusinessException("请选择院系或班级");
                }
                break;
        }

        if ("FIXED".equals(dto.getTimeRangeType())) {
            if (dto.getFixedStartDate() == null || dto.getFixedEndDate() == null) {
                throw new BusinessException("固定时间范围需要指定开始和结束日期");
            }
            if (dto.getFixedStartDate().isAfter(dto.getFixedEndDate())) {
                throw new BusinessException("开始日期不能晚于结束日期");
            }
        } else if ("DYNAMIC".equals(dto.getTimeRangeType())) {
            if (dto.getDynamicRange() == null) {
                throw new BusinessException("动态时间范围需要选择范围类型");
            }
        }
    }

    private LocalDate calculateStartDate(AnalysisConfigDTO config) {
        if ("FIXED".equals(config.getTimeRangeType())) {
            return config.getFixedStartDate();
        }

        LocalDate today = LocalDate.now();
        String dynamicRange = config.getDynamicRange();

        switch (dynamicRange) {
            case "LAST_7_DAYS":
                return today.minusDays(7);
            case "LAST_30_DAYS":
                return today.minusDays(30);
            case "THIS_MONTH":
                return today.with(TemporalAdjusters.firstDayOfMonth());
            case "THIS_SEMESTER":
                Semester currentSemester = semesterMapper.selectCurrentSemester();
                return currentSemester != null ? currentSemester.getStartDate() : today.minusMonths(4);
            case "THIS_YEAR":
                return today.with(TemporalAdjusters.firstDayOfYear());
            default:
                return today.minusDays(30);
        }
    }

    private LocalDate calculateEndDate(AnalysisConfigDTO config) {
        if ("FIXED".equals(config.getTimeRangeType())) {
            return config.getFixedEndDate();
        }

        LocalDate today = LocalDate.now();
        String dynamicRange = config.getDynamicRange();

        if ("THIS_SEMESTER".equals(dynamicRange)) {
            Semester currentSemester = semesterMapper.selectCurrentSemester();
            if (currentSemester != null && currentSemester.getEndDate().isBefore(today)) {
                return currentSemester.getEndDate();
            }
        }

        return today;
    }

    private Long getTargetId(AnalysisConfigDTO config) {
        switch (config.getTargetType()) {
            case "PLAN":
                return config.getPlanId();
            case "TEMPLATE":
                return config.getTemplateId();
            case "CATEGORY":
                return config.getCategoryId();
            case "DEDUCTION_ITEM":
                return config.getDeductionItemId();
            case "SINGLE_CHECK":
                return config.getCheckRecordId();
            case "ORGANIZATION":
                return config.getDepartmentId() != null ? config.getDepartmentId() : config.getClassId();
            default:
                return null;
        }
    }

    private FrequencyStats generateFrequencyStats(AnalysisConfigDTO config, Long targetId,
            LocalDate startDate, LocalDate endDate) {
        FrequencyStats stats = new FrequencyStats();

        // 获取总体统计
        Map<String, Object> overview = baseMapper.statOverview(
                config.getTargetType(), targetId, startDate, endDate,
                config.getOrgScopeType(), config.getOrgScopeIds());

        if (overview != null) {
            stats.setTotalChecks(getIntValue(overview, "totalChecks"));
            stats.setTotalDeductions(getIntValue(overview, "totalDeductions"));
            stats.setClassCount(getIntValue(overview, "classCount"));
        }

        // 获取各扣分项频次
        List<Map<String, Object>> itemStats;
        if ("CATEGORY".equals(config.getTargetType())) {
            itemStats = baseMapper.statDeductionByCategory(
                    targetId, startDate, endDate,
                    config.getOrgScopeType(), config.getOrgScopeIds());
        } else if ("TEMPLATE".equals(config.getTargetType())) {
            itemStats = baseMapper.statByTemplate(
                    targetId, startDate, endDate,
                    config.getOrgScopeType(), config.getOrgScopeIds());
        } else {
            itemStats = Collections.emptyList();
        }

        int totalCount = itemStats.stream()
                .mapToInt(m -> getIntValue(m, "count"))
                .sum();

        List<FrequencyItem> frequencies = itemStats.stream()
                .map(m -> {
                    FrequencyItem item = new FrequencyItem();
                    item.setName(getStringValue(m, "itemName", "categoryName"));
                    item.setCount(getIntValue(m, "count"));
                    if (totalCount > 0) {
                        item.setPercentage(BigDecimal.valueOf(item.getCount() * 100.0 / totalCount)
                                .setScale(2, RoundingMode.HALF_UP));
                    }
                    return item;
                })
                .collect(Collectors.toList());

        stats.setItemFrequencies(frequencies);
        return stats;
    }

    private ScoreStats generateScoreStats(AnalysisConfigDTO config, Long targetId,
            LocalDate startDate, LocalDate endDate) {
        ScoreStats stats = new ScoreStats();

        Map<String, Object> overview = baseMapper.statOverview(
                config.getTargetType(), targetId, startDate, endDate,
                config.getOrgScopeType(), config.getOrgScopeIds());

        if (overview != null) {
            stats.setTotalDeduction(getBigDecimalValue(overview, "totalDeduction"));
            stats.setAvgDeduction(getBigDecimalValue(overview, "avgDeduction"));
            stats.setMaxDeduction(getBigDecimalValue(overview, "maxDeduction"));
            stats.setMinDeduction(getBigDecimalValue(overview, "minDeduction"));
        }

        return stats;
    }

    private TrendAnalysis generateTrendAnalysis(AnalysisConfigDTO config, Long targetId,
            LocalDate startDate, LocalDate endDate) {
        TrendAnalysis trend = new TrendAnalysis();

        List<Map<String, Object>> trendData = baseMapper.statTrendByDate(
                config.getTargetType(), targetId, startDate, endDate,
                config.getOrgScopeType(), config.getOrgScopeIds());

        List<String> dates = new ArrayList<>();
        List<BigDecimal> deductionTrend = new ArrayList<>();
        List<Integer> checkCountTrend = new ArrayList<>();

        for (Map<String, Object> row : trendData) {
            Object dateObj = row.get("checkDate");
            if (dateObj != null) {
                dates.add(dateObj.toString());
            }
            deductionTrend.add(getBigDecimalValue(row, "totalDeduction"));
            checkCountTrend.add(getIntValue(row, "checkCount"));
        }

        trend.setDates(dates);
        trend.setDeductionTrend(deductionTrend);
        trend.setCheckCountTrend(checkCountTrend);

        return trend;
    }

    private List<DistributionItem> generateDistribution(AnalysisConfigDTO config, Long targetId,
            LocalDate startDate, LocalDate endDate) {
        List<Map<String, Object>> distData;

        if ("CATEGORY".equals(config.getTargetType())) {
            distData = baseMapper.statDeductionByCategory(
                    targetId, startDate, endDate,
                    config.getOrgScopeType(), config.getOrgScopeIds());
        } else if ("TEMPLATE".equals(config.getTargetType())) {
            distData = baseMapper.statByTemplate(
                    targetId, startDate, endDate,
                    config.getOrgScopeType(), config.getOrgScopeIds());
        } else {
            distData = baseMapper.statByAllCategories(startDate, endDate,
                    config.getOrgScopeType(), config.getOrgScopeIds());
        }

        BigDecimal total = distData.stream()
                .map(m -> getBigDecimalValue(m, "totalDeduction"))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return distData.stream()
                .map(m -> {
                    DistributionItem item = new DistributionItem();
                    item.setName(getStringValue(m, "itemName", "categoryName"));
                    item.setValue(getBigDecimalValue(m, "totalDeduction"));
                    if (total.compareTo(BigDecimal.ZERO) > 0) {
                        item.setPercentage(item.getValue()
                                .multiply(BigDecimal.valueOf(100))
                                .divide(total, 2, RoundingMode.HALF_UP));
                    }
                    return item;
                })
                .collect(Collectors.toList());
    }

    private List<RankingItem> generateClassRanking(AnalysisConfigDTO config, Long targetId,
            LocalDate startDate, LocalDate endDate) {
        List<Map<String, Object>> rankData = baseMapper.statClassRanking(
                config.getTargetType(), targetId, startDate, endDate,
                config.getOrgScopeType(), config.getOrgScopeIds(), 20);

        List<RankingItem> ranking = new ArrayList<>();
        int rank = 1;
        for (Map<String, Object> row : rankData) {
            RankingItem item = new RankingItem();
            item.setRank(rank++);
            item.setId(getLongValue(row, "classId"));
            item.setName(getStringValue(row, "className"));
            item.setDeductionCount(getIntValue(row, "deductionCount"));
            item.setTotalDeduction(getBigDecimalValue(row, "totalDeduction"));
            item.setAvgScore(getBigDecimalValue(row, "avgDeduction"));
            ranking.add(item);
        }

        return ranking;
    }

    private List<StudentRankingItem> generateStudentRanking(AnalysisConfigDTO config, Long targetId,
            LocalDate startDate, LocalDate endDate) {
        List<Map<String, Object>> rankData = baseMapper.statStudentRanking(
                config.getTargetType(), targetId, startDate, endDate,
                config.getOrgScopeType(), config.getOrgScopeIds(), 20);

        List<StudentRankingItem> ranking = new ArrayList<>();
        int rank = 1;
        for (Map<String, Object> row : rankData) {
            StudentRankingItem item = new StudentRankingItem();
            item.setRank(rank++);
            item.setStudentId(getLongValue(row, "studentId"));
            item.setStudentNo(getStringValue(row, "studentNo"));
            item.setStudentName(getStringValue(row, "studentName"));
            item.setClassName(getStringValue(row, "className"));
            item.setDeductionCount(getIntValue(row, "deductionCount"));
            item.setTotalDeduction(getBigDecimalValue(row, "totalDeduction"));
            ranking.add(item);
        }

        return ranking;
    }

    private HeatmapData generateHeatmap(AnalysisConfigDTO config, Long targetId,
            LocalDate startDate, LocalDate endDate) {
        List<Map<String, Object>> heatmapData = baseMapper.statHeatmapData(
                config.getTargetType(), targetId, startDate, endDate,
                config.getOrgScopeType(), config.getOrgScopeIds());

        HeatmapData result = new HeatmapData();

        // 收集所有日期和项目名称
        Set<String> dateSet = new TreeSet<>();
        Set<String> itemSet = new LinkedHashSet<>();
        Map<String, Map<String, BigDecimal>> dataMap = new HashMap<>();
        BigDecimal maxValue = BigDecimal.ZERO;

        for (Map<String, Object> row : heatmapData) {
            String dateStr = getStringValue(row, "dateStr");
            String itemName = getStringValue(row, "itemName");
            BigDecimal value = getBigDecimalValue(row, "count");

            dateSet.add(dateStr);
            itemSet.add(itemName);
            dataMap.computeIfAbsent(dateStr, k -> new HashMap<>()).put(itemName, value);

            if (value.compareTo(maxValue) > 0) {
                maxValue = value;
            }
        }

        List<String> xLabels = new ArrayList<>(dateSet);
        List<String> yLabels = new ArrayList<>(itemSet);
        List<List<Object>> data = new ArrayList<>();

        for (int x = 0; x < xLabels.size(); x++) {
            Map<String, BigDecimal> dayData = dataMap.get(xLabels.get(x));
            for (int y = 0; y < yLabels.size(); y++) {
                BigDecimal value = dayData != null ? dayData.getOrDefault(yLabels.get(y), BigDecimal.ZERO) : BigDecimal.ZERO;
                data.add(Arrays.asList(x, y, value));
            }
        }

        result.setXLabels(xLabels);
        result.setYLabels(yLabels);
        result.setData(data);
        result.setMaxValue(maxValue);

        return result;
    }

    private RadarData generateRadar(AnalysisConfigDTO config, LocalDate startDate, LocalDate endDate) {
        List<Map<String, Object>> categoryData = baseMapper.statByAllCategories(
                startDate, endDate, config.getOrgScopeType(), config.getOrgScopeIds());

        RadarData result = new RadarData();

        List<RadarIndicator> indicators = new ArrayList<>();
        List<BigDecimal> values = new ArrayList<>();

        BigDecimal maxDeduction = categoryData.stream()
                .map(m -> getBigDecimalValue(m, "totalDeduction"))
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.valueOf(100));

        // 确保最大值至少为100
        if (maxDeduction.compareTo(BigDecimal.valueOf(100)) < 0) {
            maxDeduction = BigDecimal.valueOf(100);
        }

        for (Map<String, Object> row : categoryData) {
            RadarIndicator indicator = new RadarIndicator();
            indicator.setName(getStringValue(row, "categoryName"));
            indicator.setMax(maxDeduction);
            indicators.add(indicator);

            values.add(getBigDecimalValue(row, "totalDeduction"));
        }

        result.setIndicators(indicators);

        RadarSeries series = new RadarSeries();
        series.setName("扣分分布");
        series.setValues(values);
        result.setSeries(Collections.singletonList(series));

        return result;
    }

    private CompareAnalysis generateCompareAnalysis(AnalysisConfigDTO config, Long targetId,
            LocalDate startDate, LocalDate endDate) {
        // 获取班级排名数据用于对比
        List<Map<String, Object>> classData = baseMapper.statClassRanking(
                config.getTargetType(), targetId, startDate, endDate,
                config.getOrgScopeType(), config.getOrgScopeIds(), 10);

        CompareAnalysis result = new CompareAnalysis();

        List<String> groups = classData.stream()
                .map(m -> getStringValue(m, "className"))
                .collect(Collectors.toList());

        List<String> indicators = Arrays.asList("扣分次数", "总扣分");

        Map<String, List<BigDecimal>> data = new HashMap<>();
        data.put("扣分次数", classData.stream()
                .map(m -> BigDecimal.valueOf(getIntValue(m, "deductionCount")))
                .collect(Collectors.toList()));
        data.put("总扣分", classData.stream()
                .map(m -> getBigDecimalValue(m, "totalDeduction"))
                .collect(Collectors.toList()));

        result.setGroups(groups);
        result.setIndicators(indicators);
        result.setData(data);

        return result;
    }

    // ==================== 工具方法 ====================

    private int getIntValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return 0;
        if (value instanceof Number) return ((Number) value).intValue();
        return Integer.parseInt(value.toString());
    }

    private Long getLongValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).longValue();
        return Long.parseLong(value.toString());
    }

    private BigDecimal getBigDecimalValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal) return (BigDecimal) value;
        if (value instanceof Number) return BigDecimal.valueOf(((Number) value).doubleValue());
        return new BigDecimal(value.toString());
    }

    private String getStringValue(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object value = map.get(key);
            if (value != null) return value.toString();
        }
        return "";
    }

    // ==================== 新增方法：按计划查询和复制配置 ====================

    @Override
    public List<AnalysisConfigDTO> getConfigsByPlanId(Long planId) {
        if (planId == null) {
            throw new BusinessException("检查计划ID不能为空");
        }
        return baseMapper.selectConfigsByPlanId(planId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long copyConfigToPlan(Long sourceConfigId, Long targetPlanId) {
        if (sourceConfigId == null) {
            throw new BusinessException("源配置ID不能为空");
        }
        if (targetPlanId == null) {
            throw new BusinessException("目标计划ID不能为空");
        }

        // 获取源配置
        AnalysisConfig source = getById(sourceConfigId);
        if (source == null) {
            throw new BusinessException("源配置不存在");
        }

        // 创建新配置
        AnalysisConfig newConfig = new AnalysisConfig();
        BeanUtils.copyProperties(source, newConfig);
        newConfig.setId(null);  // 清空ID，让数据库生成新ID
        newConfig.setPlanId(targetPlanId);  // 设置目标计划ID
        newConfig.setName(source.getName() + "_副本");  // 修改名称
        newConfig.setCreatedBy(SecurityUtils.getCurrentUserId());

        save(newConfig);

        log.info("复制配置成功: sourceId={}, newId={}, targetPlanId={}",
                sourceConfigId, newConfig.getId(), targetPlanId);

        return newConfig.getId();
    }
}
