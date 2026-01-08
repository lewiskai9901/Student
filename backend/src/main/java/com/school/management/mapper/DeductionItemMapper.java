package com.school.management.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.management.entity.DeductionItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 扣分项Mapper
 *
 * @author system
 * @since 1.0.0
 */
@Mapper
public interface DeductionItemMapper extends BaseMapper<DeductionItem> {

    /**
     * 根据类型ID获取扣分项列表
     */
    List<DeductionItem> selectByTypeId(@Param("typeId") Long typeId);

    /**
     * 根据类型ID获取启用的扣分项列表
     */
    List<DeductionItem> selectEnabledByTypeId(@Param("typeId") Long typeId);

    /**
     * 分页查询扣分项
     */
    IPage<DeductionItem> selectDeductionItemPage(Page<DeductionItem> page, @Param("typeId") Long typeId);
}
