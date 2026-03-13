package com.school.management.infrastructure.persistence.inspection.v7.platform;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AuditTrailMapper extends BaseMapper<AuditTrailPO> {

    @Select("SELECT * FROM insp_audit_trail WHERE resource_type = #{resourceType} AND resource_id = #{resourceId} AND deleted = 0 ORDER BY occurred_at DESC")
    List<AuditTrailPO> findByResourceTypeAndId(@Param("resourceType") String resourceType,
                                                @Param("resourceId") Long resourceId);

    @Select("SELECT * FROM insp_audit_trail WHERE user_id = #{userId} AND deleted = 0 ORDER BY occurred_at DESC")
    List<AuditTrailPO> findByUserId(@Param("userId") Long userId);

    @Select("SELECT * FROM insp_audit_trail WHERE deleted = 0 ORDER BY occurred_at DESC LIMIT #{limit}")
    List<AuditTrailPO> findRecent(@Param("limit") int limit);

    @Select("SELECT * FROM insp_audit_trail WHERE occurred_at >= #{start} AND occurred_at <= #{end} AND deleted = 0 ORDER BY occurred_at DESC")
    List<AuditTrailPO> findByDateRange(@Param("start") LocalDateTime start,
                                        @Param("end") LocalDateTime end);
}
