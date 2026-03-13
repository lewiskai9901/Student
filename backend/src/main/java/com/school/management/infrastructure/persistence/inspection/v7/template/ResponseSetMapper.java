package com.school.management.infrastructure.persistence.inspection.v7.template;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ResponseSetMapper extends BaseMapper<ResponseSetPO> {

    @Select("SELECT * FROM insp_response_sets WHERE set_code = #{setCode} AND deleted = 0")
    ResponseSetPO findBySetCode(@Param("setCode") String setCode);

    @Select("SELECT * FROM insp_response_sets WHERE is_global = #{isGlobal} AND deleted = 0 ORDER BY set_name")
    List<ResponseSetPO> findByIsGlobal(@Param("isGlobal") Boolean isGlobal);

    @Select("<script>"
            + "SELECT * FROM insp_response_sets WHERE deleted = 0"
            + "<if test='keyword != null and keyword != \"\"'>"
            + "  AND (set_name LIKE CONCAT('%',#{keyword},'%') OR set_code LIKE CONCAT('%',#{keyword},'%'))"
            + "</if>"
            + " ORDER BY updated_at DESC"
            + " LIMIT #{size} OFFSET #{offset}"
            + "</script>")
    List<ResponseSetPO> findPagedWithConditions(@Param("offset") int offset,
                                                 @Param("size") int size,
                                                 @Param("keyword") String keyword);

    @Select("<script>"
            + "SELECT COUNT(*) FROM insp_response_sets WHERE deleted = 0"
            + "<if test='keyword != null and keyword != \"\"'>"
            + "  AND (set_name LIKE CONCAT('%',#{keyword},'%') OR set_code LIKE CONCAT('%',#{keyword},'%'))"
            + "</if>"
            + "</script>")
    long countWithConditions(@Param("keyword") String keyword);
}
