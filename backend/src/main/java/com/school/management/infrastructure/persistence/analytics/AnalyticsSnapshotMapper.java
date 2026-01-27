package com.school.management.infrastructure.persistence.analytics;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.time.LocalDate;
import java.util.List;

@Mapper
public interface AnalyticsSnapshotMapper extends BaseMapper<AnalyticsSnapshotPO> {
    @Select("SELECT * FROM analytics_snapshots WHERE snapshot_type = #{type} AND snapshot_date = #{date} ORDER BY generated_at DESC")
    List<AnalyticsSnapshotPO> findByTypeAndDate(@Param("type") String type, @Param("date") LocalDate date);

    @Select("SELECT * FROM analytics_snapshots WHERE snapshot_type = #{type} AND snapshot_scope = #{scope} AND scope_id = #{scopeId} ORDER BY generated_at DESC LIMIT 1")
    AnalyticsSnapshotPO findLatestByTypeAndScope(@Param("type") String type, @Param("scope") String scope, @Param("scopeId") Long scopeId);

    @Select("SELECT * FROM analytics_snapshots WHERE snapshot_type = #{type} AND snapshot_date BETWEEN #{startDate} AND #{endDate} ORDER BY snapshot_date DESC")
    List<AnalyticsSnapshotPO> findByTypeAndDateRange(@Param("type") String type, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}
