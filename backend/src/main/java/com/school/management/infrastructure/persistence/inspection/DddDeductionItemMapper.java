package com.school.management.infrastructure.persistence.inspection;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * MyBatis mapper for deduction items (DDD infrastructure).
 */
@Mapper
public interface DddDeductionItemMapper extends BaseMapper<DeductionItemPO> {

    @Select("SELECT * FROM deduction_items WHERE category_id = #{categoryId} AND deleted = 0 ORDER BY sort_order")
    List<DeductionItemPO> findByCategoryId(@Param("categoryId") Long categoryId);

    @Select("SELECT * FROM deduction_items WHERE category_id = #{categoryId} AND is_enabled = 1 AND deleted = 0 ORDER BY sort_order")
    List<DeductionItemPO> findEnabledByCategoryId(@Param("categoryId") Long categoryId);

    @Select("SELECT * FROM deduction_items WHERE item_code = #{itemCode} AND category_id = #{categoryId} AND deleted = 0")
    DeductionItemPO findByItemCodeAndCategoryId(
        @Param("itemCode") String itemCode,
        @Param("categoryId") Long categoryId);
}
