package com.school.management.infrastructure.persistence.user;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用户类型Mapper
 */
@Mapper
public interface UserTypeMapper extends BaseMapper<UserTypePO> {

    /**
     * 根据类型编码查询
     */
    @Select("SELECT * FROM user_types WHERE type_code = #{typeCode} AND deleted = 0")
    UserTypePO findByTypeCode(@Param("typeCode") String typeCode);

    /**
     * 根据父类型编码查询子类型
     */
    @Select("SELECT * FROM user_types WHERE parent_type_code = #{parentTypeCode} AND deleted = 0 ORDER BY sort_order")
    List<UserTypePO> findByParentTypeCode(@Param("parentTypeCode") String parentTypeCode);

    /**
     * 查询顶级类型
     */
    @Select("SELECT * FROM user_types WHERE (parent_type_code IS NULL OR parent_type_code = '') AND deleted = 0 ORDER BY sort_order")
    List<UserTypePO> findTopLevelTypes();

    /**
     * 查询所有启用的类型
     */
    @Select("SELECT * FROM user_types WHERE is_enabled = 1 AND deleted = 0 ORDER BY sort_order")
    List<UserTypePO> findAllEnabled();

    /**
     * 查询可登录的类型
     */
    @Select("SELECT * FROM user_types WHERE can_login = 1 AND is_enabled = 1 AND deleted = 0 ORDER BY sort_order")
    List<UserTypePO> findLoginableTypes();

    /**
     * 查询可作为检查员的类型
     */
    @Select("SELECT * FROM user_types WHERE can_be_inspector = 1 AND is_enabled = 1 AND deleted = 0 ORDER BY sort_order")
    List<UserTypePO> findInspectorTypes();

    /**
     * 查询可被检查的类型
     */
    @Select("SELECT * FROM user_types WHERE can_be_inspected = 1 AND is_enabled = 1 AND deleted = 0 ORDER BY sort_order")
    List<UserTypePO> findInspectableTypes();

    /**
     * 查询需要班级归属的类型
     */
    @Select("SELECT * FROM user_types WHERE requires_class = 1 AND is_enabled = 1 AND deleted = 0 ORDER BY sort_order")
    List<UserTypePO> findClassRequiredTypes();

    /**
     * 检查类型编码是否存在
     */
    @Select("SELECT COUNT(*) > 0 FROM user_types WHERE type_code = #{typeCode} AND deleted = 0")
    boolean existsByTypeCode(@Param("typeCode") String typeCode);

    /**
     * 统计使用该类型的用户数量
     */
    @Select("SELECT COUNT(*) FROM sys_user WHERE user_type = #{typeCode} AND deleted = 0")
    int countUsersByTypeCode(@Param("typeCode") String typeCode);
}
