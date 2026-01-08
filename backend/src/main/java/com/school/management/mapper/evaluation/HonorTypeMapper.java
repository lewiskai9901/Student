package com.school.management.mapper.evaluation;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.management.entity.evaluation.HonorType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 荣誉类型Mapper接口
 *
 * @author Claude
 * @since 2025-11-28
 */
@Mapper
public interface HonorTypeMapper extends BaseMapper<HonorType> {

    /**
     * 查询所有启用的荣誉类型
     */
    List<HonorType> selectAllEnabled();

    /**
     * 根据类别查询
     */
    List<HonorType> selectByCategory(@Param("category") String category);

    /**
     * 根据类型编码查询
     */
    HonorType selectByCode(@Param("code") String code);

    /**
     * 查询荣誉类型详情(含等级配置)
     */
    Map<String, Object> selectDetailById(@Param("id") Long id);

    /**
     * 查询可用的荣誉类型（用于申报选择）
     */
    List<Map<String, Object>> selectAvailableTypes();
}
