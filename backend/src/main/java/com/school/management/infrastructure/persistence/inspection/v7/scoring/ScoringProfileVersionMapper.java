package com.school.management.infrastructure.persistence.inspection.v7.scoring;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ScoringProfileVersionMapper extends BaseMapper<ScoringProfileVersionPO> {

    @Select("SELECT * FROM insp_scoring_profile_versions WHERE profile_id = #{profileId} AND deleted = 0 ORDER BY version DESC")
    List<ScoringProfileVersionPO> findByProfileId(Long profileId);

    @Select("SELECT * FROM insp_scoring_profile_versions WHERE profile_id = #{profileId} AND version = #{version} AND deleted = 0")
    ScoringProfileVersionPO findByProfileIdAndVersion(Long profileId, Integer version);
}
