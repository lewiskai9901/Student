package com.school.management.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.management.dto.CheckTemplateResponse;
import com.school.management.entity.CheckTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 检查模板Mapper接口
 *
 * @author system
 * @since 1.0.0
 */
@Mapper
public interface CheckTemplateMapper extends BaseMapper<CheckTemplate> {

    /**
     * 分页查询检查模板
     *
     * @param page 分页参数
     * @param templateName 模板名称
     * @param status 状态
     * @return 检查模板列表
     */
    IPage<CheckTemplate> selectTemplatePage(Page<CheckTemplate> page,
                                           @Param("templateName") String templateName,
                                           @Param("status") Integer status);

    /**
     * 根据模板编码查询
     *
     * @param templateCode 模板编码
     * @return 检查模板
     */
    CheckTemplate selectByTemplateCode(@Param("templateCode") String templateCode);

    /**
     * 检查模板编码是否存在
     *
     * @param templateCode 模板编码
     * @param excludeId 排除的ID
     * @return 数量
     */
    Integer countByTemplateCode(@Param("templateCode") String templateCode, @Param("excludeId") Long excludeId);

    /**
     * 获取模板详情(含类别列表)
     *
     * @param id 模板ID
     * @return 模板详情
     */
    CheckTemplateResponse selectTemplateDetailById(@Param("id") Long id);
}
