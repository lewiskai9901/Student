package com.school.management.infrastructure.persistence.inspection.evaluation;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.management.infrastructure.access.DataPermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
@DataPermission(module = "indicator_result")
public interface IndicatorResultMapper extends BaseMapper<IndicatorResultPO> {

    @Select("SELECT * FROM indicator_results WHERE indicator_id = #{indicatorId} AND deleted = 0 " +
            "ORDER BY computed_at DESC")
    List<IndicatorResultPO> findByIndicatorId(@Param("indicatorId") Long indicatorId);

    @Select("SELECT * FROM indicator_results WHERE indicator_id = #{indicatorId} " +
            "AND status = #{status} AND deleted = 0 ORDER BY computed_at DESC")
    List<IndicatorResultPO> findByIndicatorIdAndStatus(@Param("indicatorId") Long indicatorId,
                                                       @Param("status") String status);

    @Select("SELECT * FROM indicator_results WHERE indicator_id = #{indicatorId} " +
            "AND target_id = #{targetId} AND period_key = #{periodKey} " +
            "AND status = 'PUBLISHED' AND deleted = 0 " +
            "ORDER BY computed_at DESC LIMIT 1")
    IndicatorResultPO findCurrentPublished(@Param("indicatorId") Long indicatorId,
                                            @Param("targetId") Long targetId,
                                            @Param("periodKey") String periodKey);

    @Select("SELECT * FROM indicator_results WHERE indicator_id = #{indicatorId} " +
            "AND target_id = #{targetId} AND period_key = #{periodKey} AND deleted = 0 " +
            "ORDER BY computed_at ASC")
    List<IndicatorResultPO> findRevisionHistory(@Param("indicatorId") Long indicatorId,
                                                 @Param("targetId") Long targetId,
                                                 @Param("periodKey") String periodKey);
}
