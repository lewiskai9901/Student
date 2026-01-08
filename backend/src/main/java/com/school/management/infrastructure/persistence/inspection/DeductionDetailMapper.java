package com.school.management.infrastructure.persistence.inspection;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * MyBatis mapper for deduction details.
 */
@Mapper
public interface DeductionDetailMapper extends BaseMapper<DeductionDetailPO> {

    @Select("SELECT * FROM deduction_details WHERE class_score_id = #{classScoreId} AND deleted = 0")
    List<DeductionDetailPO> findByClassScoreId(@Param("classScoreId") Long classScoreId);

    @Select("SELECT * FROM deduction_details WHERE deduction_item_id = #{deductionItemId} AND deleted = 0")
    List<DeductionDetailPO> findByDeductionItemId(@Param("deductionItemId") Long deductionItemId);
}
