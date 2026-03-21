package com.school.management.infrastructure.persistence.message;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 消息模板 MyBatis Mapper
 */
@Mapper
public interface MsgTemplateMapper extends BaseMapper<MsgTemplatePO> {

    @Select("SELECT * FROM msg_templates WHERE deleted = 0 ORDER BY id")
    List<MsgTemplatePO> findAll();

    @Select("SELECT * FROM msg_templates WHERE template_code = #{code} AND deleted = 0 LIMIT 1")
    MsgTemplatePO findByCode(@Param("code") String code);
}
