package com.school.management.infrastructure.persistence.inspection.v7.analytics;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AlertMapper extends BaseMapper<AlertPO> {

    @Select("SELECT * FROM insp_alerts WHERE status = #{status} AND deleted = 0 ORDER BY triggered_at DESC")
    List<AlertPO> findByStatus(@Param("status") String status);

    @Select("SELECT * FROM insp_alerts WHERE deleted = 0 ORDER BY triggered_at DESC LIMIT #{limit}")
    List<AlertPO> findRecent(@Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM insp_alerts WHERE status = #{status} AND deleted = 0")
    long countByStatus(@Param("status") String status);
}
