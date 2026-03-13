package com.school.management.infrastructure.persistence.inspection.v7.template;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ResponseSetOptionMapper extends BaseMapper<ResponseSetOptionPO> {

    @Select("SELECT * FROM insp_response_set_options WHERE response_set_id = #{responseSetId} AND deleted = 0 ORDER BY sort_order")
    List<ResponseSetOptionPO> findByResponseSetId(@Param("responseSetId") Long responseSetId);

    @Update("UPDATE insp_response_set_options SET deleted = 1 WHERE response_set_id = #{responseSetId} AND deleted = 0")
    void softDeleteByResponseSetId(@Param("responseSetId") Long responseSetId);
}
