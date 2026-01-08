package com.school.management.mapper.rating;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.management.entity.rating.RatingChangeLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 评级变更日志 Mapper
 *
 * @author System
 * @since 4.4.0
 */
@Mapper
public interface RatingChangeLogMapper extends BaseMapper<RatingChangeLog> {
}
