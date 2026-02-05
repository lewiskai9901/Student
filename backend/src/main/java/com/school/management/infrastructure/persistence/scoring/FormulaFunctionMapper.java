package com.school.management.infrastructure.persistence.scoring;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 内置函数Mapper
 */
@Mapper
public interface FormulaFunctionMapper extends BaseMapper<FormulaFunctionPO> {

    @Select("SELECT * FROM formula_functions WHERE name = #{name} AND deleted = 0")
    FormulaFunctionPO selectByName(@Param("name") String name);

    @Select("SELECT * FROM formula_functions WHERE is_enabled = 1 AND deleted = 0")
    List<FormulaFunctionPO> selectAllEnabled();

    @Select("SELECT * FROM formula_functions WHERE category = #{category} AND deleted = 0")
    List<FormulaFunctionPO> selectByCategory(@Param("category") String category);
}
