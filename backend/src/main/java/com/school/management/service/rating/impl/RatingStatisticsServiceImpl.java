package com.school.management.service.rating.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.school.management.dto.rating.RatingStatisticsVO;
import com.school.management.entity.Class;
import com.school.management.entity.rating.RatingConfig;
import com.school.management.entity.rating.RatingResult;
import com.school.management.entity.rating.RatingStatistics;
import com.school.management.mapper.ClassMapper;
import com.school.management.mapper.rating.RatingConfigMapper;
import com.school.management.mapper.rating.RatingResultMapper;
import com.school.management.mapper.rating.RatingStatisticsMapper;
import com.school.management.service.rating.RatingStatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 评级统计服务实现
 *
 * @author System
 * @since 4.4.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RatingStatisticsServiceImpl extends ServiceImpl<RatingStatisticsMapper, RatingStatistics>
        implements RatingStatisticsService {

    private final RatingResultMapper resultMapper;
    private final RatingConfigMapper configMapper;
    private final ClassMapper classMapper;

    @Override
    @Transactional
    public void updateStatistics(Long ratingConfigId, int year, Integer month) {
        log.info("更新评级统计: ratingConfigId={}, year={}, month={}", ratingConfigId, year, month);

        // 获取评级配置
        RatingConfig config = configMapper.selectById(ratingConfigId);
        if (config == null) {
            log.warn("评级配置不存在: ratingConfigId={}", ratingConfigId);
            return;
        }

        // 查询该配置下已发布的评级结果
        LambdaQueryWrapper<RatingResult> resultQuery = new LambdaQueryWrapper<>();
        resultQuery.eq(RatingResult::getRatingConfigId, ratingConfigId)
                .eq(RatingResult::getStatus, "PUBLISHED")
                .eq(RatingResult::getAwarded, 1); // 只统计获奖的

        // 根据周期类型添加时间过滤
        if (month != null) {
            // 月度统计
            resultQuery.apply("YEAR(period_start) = {0} AND MONTH(period_start) = {1}", year, month);
        } else {
            // 年度统计
            resultQuery.apply("YEAR(period_start) = {0}", year);
        }

        List<RatingResult> results = resultMapper.selectList(resultQuery);

        // 按班级分组统计
        Map<Long, Long> classAwardedCount = results.stream()
                .collect(Collectors.groupingBy(
                        RatingResult::getClassId,
                        Collectors.counting()
                ));

        // 更新统计表
        for (Map.Entry<Long, Long> entry : classAwardedCount.entrySet()) {
            Long classId = entry.getKey();
            int count = entry.getValue().intValue();

            // 查找已有统计
            LambdaQueryWrapper<RatingStatistics> statsQuery = new LambdaQueryWrapper<>();
            statsQuery.eq(RatingStatistics::getRatingConfigId, ratingConfigId)
                    .eq(RatingStatistics::getClassId, classId)
                    .eq(RatingStatistics::getPeriodType, config.getRatingType())
                    .eq(RatingStatistics::getYear, year);

            if (month != null) {
                statsQuery.eq(RatingStatistics::getMonth, month);
            }

            RatingStatistics stats = this.getOne(statsQuery);

            if (stats == null) {
                // 创建新统计
                stats = new RatingStatistics();
                stats.setCheckPlanId(config.getCheckPlanId());
                stats.setRatingConfigId(ratingConfigId);
                stats.setClassId(classId);
                stats.setPeriodType(config.getRatingType());
                stats.setYear(year);
                stats.setMonth(month);
                stats.setAwardedCount(count);
                this.save(stats);
            } else {
                // 更新统计
                stats.setAwardedCount(count);
                this.updateById(stats);
            }
        }

        log.info("评级统计更新完成: ratingConfigId={}, classCount={}", ratingConfigId, classAwardedCount.size());
    }

    @Override
    public List<RatingStatisticsVO> getClassStatistics(Long classId, Long ratingConfigId,
                                                       Integer year, Integer month) {
        LambdaQueryWrapper<RatingStatistics> query = new LambdaQueryWrapper<>();
        query.eq(RatingStatistics::getClassId, classId)
                .eq(ratingConfigId != null, RatingStatistics::getRatingConfigId, ratingConfigId)
                .eq(year != null, RatingStatistics::getYear, year)
                .eq(month != null, RatingStatistics::getMonth, month)
                .orderByDesc(RatingStatistics::getYear)
                .orderByDesc(RatingStatistics::getMonth);

        List<RatingStatistics> statistics = this.list(query);
        return statistics.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<RatingStatisticsVO> getPlanStatistics(Long checkPlanId, Integer year, Integer month) {
        LambdaQueryWrapper<RatingStatistics> query = new LambdaQueryWrapper<>();
        query.eq(RatingStatistics::getCheckPlanId, checkPlanId)
                .eq(year != null, RatingStatistics::getYear, year)
                .eq(month != null, RatingStatistics::getMonth, month)
                .orderByDesc(RatingStatistics::getAwardedCount);

        List<RatingStatistics> statistics = this.list(query);
        return statistics.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    /**
     * 转换为VO
     */
    private RatingStatisticsVO convertToVO(RatingStatistics stats) {
        RatingStatisticsVO vo = new RatingStatisticsVO();
        BeanUtils.copyProperties(stats, vo);

        // 设置文本
        vo.setPeriodTypeText(getPeriodTypeText(stats.getPeriodType()));
        vo.setPeriodText(formatPeriodText(stats));

        // 加载评级配置信息
        RatingConfig config = configMapper.selectById(stats.getRatingConfigId());
        if (config != null) {
            vo.setRatingName(config.getRatingName());
            vo.setIcon(config.getIcon());
            vo.setColor(config.getColor());
        }

        // 加载班级名称
        if (stats.getClassId() != null) {
            Class classEntity = classMapper.selectById(stats.getClassId());
            if (classEntity != null) {
                vo.setClassName(classEntity.getClassName());
            }
        }

        return vo;
    }

    /**
     * 格式化周期文本
     */
    private String formatPeriodText(RatingStatistics stats) {
        if (stats.getMonth() != null) {
            return stats.getYear() + "年" + stats.getMonth() + "月";
        } else {
            return stats.getYear() + "年";
        }
    }

    /**
     * 获取周期类型文本
     */
    private String getPeriodTypeText(String type) {
        switch (type) {
            case "DAILY": return "日评级";
            case "WEEKLY": return "周评级";
            case "MONTHLY": return "月评级";
            default: return type;
        }
    }
}
