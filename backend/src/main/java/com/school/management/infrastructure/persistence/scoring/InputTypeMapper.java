package com.school.management.infrastructure.persistence.scoring;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 打分方式Mapper
 */
@Mapper
public interface InputTypeMapper extends BaseMapper<InputTypePO> {

    @Select("SELECT * FROM input_types WHERE code = #{code} AND deleted = 0")
    InputTypePO selectByCode(@Param("code") String code);

    @Select("SELECT * FROM input_types WHERE is_enabled = 1 AND deleted = 0 ORDER BY sort_order")
    List<InputTypePO> selectAllEnabled();

    @Select("SELECT * FROM input_types WHERE category = #{category} AND deleted = 0 ORDER BY sort_order")
    List<InputTypePO> selectByCategory(@Param("category") String category);

    @Select("SELECT COUNT(*) FROM input_types WHERE code = #{code} AND deleted = 0")
    int countByCode(@Param("code") String code);
}
