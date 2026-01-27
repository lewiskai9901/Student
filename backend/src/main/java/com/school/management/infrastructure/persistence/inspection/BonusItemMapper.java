package com.school.management.infrastructure.persistence.inspection;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * MyBatis mapper for bonus items.
 */
@Mapper
public interface BonusItemMapper extends BaseMapper<BonusItemPO> {

    @Select("SELECT * FROM bonus_items WHERE category_id = #{categoryId} AND deleted = 0 ORDER BY sort_order ASC")
    List<BonusItemPO> findByCategoryId(@Param("categoryId") Long categoryId);

    @Select("SELECT * FROM bonus_items WHERE status = 1 AND deleted = 0 ORDER BY sort_order ASC")
    List<BonusItemPO> findAllEnabled();
}
