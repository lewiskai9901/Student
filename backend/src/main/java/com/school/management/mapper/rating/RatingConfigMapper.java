package com.school.management.mapper.rating;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.management.entity.rating.RatingConfig;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

/**
 * 评级配置 Mapper
 *
 * @author System
 * @since 4.4.0
 */
@Mapper
@Repository("ratingConfigMapperV1")
public interface RatingConfigMapper extends BaseMapper<RatingConfig> {
}
