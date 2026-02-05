package com.school.management.infrastructure.persistence.scoring;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 内置变量Mapper
 */
@Mapper
public interface FormulaVariableMapper extends BaseMapper<FormulaVariablePO> {

    @Select("SELECT * FROM formula_variables WHERE name = #{name} AND deleted = 0")
    FormulaVariablePO selectByName(@Param("name") String name);

    @Select("SELECT * FROM formula_variables WHERE is_enabled = 1 AND deleted = 0")
    List<FormulaVariablePO> selectAllEnabled();

    @Select("SELECT * FROM formula_variables WHERE category = #{category} AND deleted = 0")
    List<FormulaVariablePO> selectByCategory(@Param("category") String category);
}
