package com.school.management.infrastructure.persistence.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 量化类型字典 Mapper
 */
@Mapper
public interface QuantificationDictCategoryMapper extends BaseMapper<QuantificationDictCategoryPO> {

    @Select("SELECT * FROM quantification_dict_categories WHERE status = 1 AND deleted = 0 ORDER BY sort_order, id")
    List<QuantificationDictCategoryPO> findAllEnabled();

    @Select("SELECT * FROM quantification_dict_categories WHERE category_code = #{categoryCode} AND deleted = 0")
    QuantificationDictCategoryPO findByCategoryCode(String categoryCode);
}
