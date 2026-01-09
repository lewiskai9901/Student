package com.school.management.mapper.rating;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.management.entity.rating.RatingResult;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

/**
 * 评级结果 Mapper
 *
 * @author System
 * @since 4.4.0
 */
@Mapper
@Repository("ratingResultMapperV1")
public interface RatingResultMapper extends BaseMapper<RatingResult> {
}
