package com.school.management.infrastructure.persistence.asset;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 资产审批Mapper
 */
@Mapper
public interface AssetApprovalMapper extends BaseMapper<AssetApprovalPO> {

    @Select("SELECT * FROM asset_approval WHERE expire_time < NOW() AND status = 0")
    List<AssetApprovalPO> selectExpiredPending();

    @Select("SELECT COUNT(*) FROM asset_approval WHERE status = #{status}")
    int countByStatus(@Param("status") Integer status);

    @Select("SELECT COUNT(*) FROM asset_approval WHERE applicant_id = #{applicantId} AND status = #{status}")
    int countByApplicantIdAndStatus(@Param("applicantId") Long applicantId, @Param("status") Integer status);
}
