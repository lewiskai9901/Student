package com.school.management.mapper.rating;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.management.entity.rating.DailyClassSummary;
import org.apache.ibatis.annotations.Mapper;

/**
 * 每日班级汇总 Mapper
 *
 * @author System
 * @since 4.4.0
 */
@Mapper
public interface DailyClassSummaryMapper extends BaseMapper<DailyClassSummary> {
}
