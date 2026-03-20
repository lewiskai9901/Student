package com.school.management.infrastructure.persistence.user;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用户类型Mapper（统一类型系统 Phase 2）
 */
@Mapper
public interface UserTypeMapper extends BaseMapper<UserTypePO> {

    @Select("SELECT * FROM user_types WHERE type_code = #{typeCode} AND deleted = 0")
    UserTypePO findByTypeCode(@Param("typeCode") String typeCode);

    @Select("SELECT * FROM user_types WHERE parent_type_code = #{parentTypeCode} AND deleted = 0 ORDER BY sort_order")
    List<UserTypePO> findByParentTypeCode(@Param("parentTypeCode") String parentTypeCode);

    @Select("SELECT * FROM user_types WHERE (parent_type_code IS NULL OR parent_type_code = '') AND deleted = 0 ORDER BY sort_order")
    List<UserTypePO> findTopLevelTypes();

    @Select("SELECT * FROM user_types WHERE is_enabled = 1 AND deleted = 0 ORDER BY sort_order")
    List<UserTypePO> findAllEnabled();

    @Select("SELECT * FROM user_types WHERE category = #{category} AND deleted = 0 ORDER BY sort_order")
    List<UserTypePO> findByCategory(@Param("category") String category);

    @Select("SELECT * FROM user_types WHERE JSON_EXTRACT(features, CONCAT('$.', #{featureKey})) = true AND deleted = 0 ORDER BY sort_order")
    List<UserTypePO> findByFeature(@Param("featureKey") String featureKey);

    @Select("SELECT COUNT(*) > 0 FROM user_types WHERE type_code = #{typeCode} AND deleted = 0")
    boolean existsByTypeCode(@Param("typeCode") String typeCode);

    @Select("SELECT COUNT(*) FROM users WHERE user_type_code = #{typeCode} AND deleted = 0")
    int countUsersByTypeCode(@Param("typeCode") String typeCode);
}
