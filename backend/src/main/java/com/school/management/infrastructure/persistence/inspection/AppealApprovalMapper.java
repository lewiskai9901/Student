package com.school.management.infrastructure.persistence.inspection;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * MyBatis mapper for appeal approval records.
 */
@Mapper
public interface AppealApprovalMapper extends BaseMapper<AppealApprovalPO> {

    @Select("SELECT * FROM appeal_approvals WHERE appeal_id = #{appealId} ORDER BY reviewed_at ASC")
    List<AppealApprovalPO> findByAppealId(@Param("appealId") Long appealId);

    @Select("SELECT * FROM appeal_approvals WHERE reviewer_id = #{reviewerId} ORDER BY reviewed_at DESC")
    List<AppealApprovalPO> findByReviewerId(@Param("reviewerId") Long reviewerId);
}
