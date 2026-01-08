package com.school.management.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.management.entity.RatingTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 评级模板Mapper接口 (V3.0)
 *
 * @author Claude
 * @since 2025-11-24
 */
@Mapper
public interface RatingTemplateMapper extends BaseMapper<RatingTemplate> {

    /**
     * 分页查询评级模板
     *
     * @param page 分页参数
     * @param query 查询条件
     * @return 评级模板列表
     */
    IPage<Map<String, Object>> selectTemplatePage(Page<Map<String, Object>> page, @Param("query") Map<String, Object> query);

    /**
     * 获取评级模板详细信息（包含规则和等级）
     *
     * @param id 模板ID
     * @return 详细信息
     */
    Map<String, Object> selectTemplateDetail(@Param("id") Long id);

    /**
     * 根据检查模板ID查询评级模板列表
     *
     * @param checkTemplateId 检查模板ID
     * @return 评级模板列表
     */
    List<RatingTemplate> selectByCheckTemplateId(@Param("checkTemplateId") Long checkTemplateId);

    /**
     * 根据模板编码查询
     *
     * @param templateCode 模板编码
     * @return 评级模板
     */
    RatingTemplate selectByTemplateCode(@Param("templateCode") String templateCode);

    /**
     * 获取默认评级模板
     *
     * @return 默认模板
     */
    RatingTemplate selectDefaultTemplate();

    /**
     * 设置默认模板（先清空所有默认标记，再设置新的）
     *
     * @param id 模板ID
     * @return 更新数量
     */
    int setDefaultTemplate(@Param("id") Long id);

    /**
     * 清空所有默认标记
     *
     * @return 更新数量
     */
    int clearAllDefaultFlags();
}
