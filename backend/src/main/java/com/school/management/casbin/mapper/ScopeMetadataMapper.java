package com.school.management.casbin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.management.casbin.entity.ScopeMetadata;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 范围元数据Mapper接口
 *
 * @author system
 * @version 1.0.0
 */
@Mapper
public interface ScopeMetadataMapper extends BaseMapper<ScopeMetadata> {

    /**
     * 根据范围表达式查询
     *
     * @param scopeExpression 范围表达式
     * @return 范围元数据
     */
    @Select("SELECT * FROM scope_metadata WHERE scope_expression = #{scopeExpression}")
    ScopeMetadata selectByExpression(@Param("scopeExpression") String scopeExpression);

    /**
     * 根据关联实体查询
     *
     * @param refType 关联类型
     * @param refId   关联ID
     * @return 范围元数据
     */
    @Select("SELECT * FROM scope_metadata WHERE ref_type = #{refType} AND ref_id = #{refId}")
    ScopeMetadata selectByRef(@Param("refType") String refType, @Param("refId") Long refId);

    /**
     * 根据范围类型查询
     *
     * @param scopeType 范围类型
     * @return 范围元数据列表
     */
    @Select("SELECT * FROM scope_metadata WHERE scope_type = #{scopeType} ORDER BY sort_order")
    List<ScopeMetadata> selectByScopeType(@Param("scopeType") String scopeType);

    /**
     * 查询子范围
     *
     * @param parentScope 父范围表达式
     * @return 子范围列表
     */
    @Select("SELECT * FROM scope_metadata WHERE parent_scope = #{parentScope} ORDER BY sort_order")
    List<ScopeMetadata> selectChildren(@Param("parentScope") String parentScope);
}
