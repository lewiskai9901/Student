package com.school.management.infrastructure.persistence.space;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 场所类型Mapper
 */
@Mapper
public interface SpaceTypeMapper extends BaseMapper<SpaceTypePO> {

    /**
     * 根据类型编码查询
     */
    @Select("SELECT * FROM space_types WHERE type_code = #{typeCode} AND deleted = 0")
    SpaceTypePO findByTypeCode(@Param("typeCode") String typeCode);

    /**
     * 根据父类型编码查询子类型
     */
    @Select("SELECT * FROM space_types WHERE parent_type_code = #{parentTypeCode} AND deleted = 0 ORDER BY sort_order")
    List<SpaceTypePO> findByParentTypeCode(@Param("parentTypeCode") String parentTypeCode);

    /**
     * 查询顶级类型
     */
    @Select("SELECT * FROM space_types WHERE (parent_type_code IS NULL OR parent_type_code = '') AND deleted = 0 ORDER BY sort_order")
    List<SpaceTypePO> findTopLevelTypes();

    /**
     * 查询所有启用的类型
     */
    @Select("SELECT * FROM space_types WHERE is_enabled = 1 AND deleted = 0 ORDER BY sort_order")
    List<SpaceTypePO> findAllEnabled();

    /**
     * 查询可检查的类型
     */
    @Select("SELECT * FROM space_types WHERE can_be_inspected = 1 AND is_enabled = 1 AND deleted = 0 ORDER BY sort_order")
    List<SpaceTypePO> findInspectableTypes();

    /**
     * 查询可借用的类型
     */
    @Select("SELECT * FROM space_types WHERE can_be_borrowed = 1 AND is_enabled = 1 AND deleted = 0 ORDER BY sort_order")
    List<SpaceTypePO> findBorrowableTypes();

    /**
     * 查询宿舍类型（有床位的）
     */
    @Select("SELECT * FROM space_types WHERE can_have_beds = 1 AND is_enabled = 1 AND deleted = 0 ORDER BY sort_order")
    List<SpaceTypePO> findDormitoryTypes();

    /**
     * 查询教学类型（有座位的）
     */
    @Select("SELECT * FROM space_types WHERE can_have_seats = 1 AND is_enabled = 1 AND deleted = 0 ORDER BY sort_order")
    List<SpaceTypePO> findTeachingTypes();

    /**
     * 检查类型编码是否存在
     */
    @Select("SELECT COUNT(*) > 0 FROM space_types WHERE type_code = #{typeCode} AND deleted = 0")
    boolean existsByTypeCode(@Param("typeCode") String typeCode);

    /**
     * 统计使用该类型的场所数量
     * 注意：需要根据实际的spaces表结构调整
     */
    @Select("SELECT COUNT(*) FROM spaces WHERE space_type_code = #{typeCode} AND deleted = 0")
    int countSpacesByTypeCode(@Param("typeCode") String typeCode);
}
