package com.school.management.infrastructure.persistence.inspection.v7.template;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface LibraryItemMapper extends BaseMapper<LibraryItemPO> {

    @Select("SELECT * FROM insp_library_items WHERE item_code = #{itemCode} AND deleted = 0 LIMIT 1")
    LibraryItemPO findByItemCode(@Param("itemCode") String itemCode);

    @Select("SELECT DISTINCT category FROM insp_library_items WHERE category IS NOT NULL AND deleted = 0 ORDER BY category")
    List<String> findDistinctCategories();
}
