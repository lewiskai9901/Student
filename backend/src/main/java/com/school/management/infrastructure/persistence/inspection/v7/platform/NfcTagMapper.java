package com.school.management.infrastructure.persistence.inspection.v7.platform;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface NfcTagMapper extends BaseMapper<NfcTagPO> {

    @Select("SELECT * FROM insp_nfc_tags WHERE tag_uid = #{tagUid} AND deleted = 0 LIMIT 1")
    NfcTagPO findByTagUid(@Param("tagUid") String tagUid);

    @Select("SELECT * FROM insp_nfc_tags WHERE is_active = 1 AND deleted = 0 ORDER BY created_at DESC")
    List<NfcTagPO> findActive();
}
