package com.school.management.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.management.entity.DailyCheckDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 检查扣分明细Mapper
 *
 * @author system
 * @since 1.0.6
 */
@Mapper
public interface DailyCheckDetailMapper extends BaseMapper<DailyCheckDetail> {

    /**
     * 根据检查ID查询所有明细
     *
     * @param checkId 检查ID
     * @return 明细列表
     */
    List<DailyCheckDetail> selectByCheckId(@Param("checkId") Long checkId);

    /**
     * 根据检查ID和班级ID查询明细
     *
     * @param checkId 检查ID
     * @param classId 班级ID
     * @return 明细列表
     */
    List<DailyCheckDetail> selectByCheckIdAndClassId(
            @Param("checkId") Long checkId,
            @Param("classId") Long classId
    );

    /**
     * 根据检查ID、类别ID和班级ID查询明细
     *
     * @param checkId    检查ID
     * @param categoryId 类别ID
     * @param classId    班级ID
     * @return 明细列表
     */
    List<DailyCheckDetail> selectByCheckIdAndCategoryIdAndClassId(
            @Param("checkId") Long checkId,
            @Param("categoryId") Long categoryId,
            @Param("classId") Long classId
    );

    /**
     * 批量插入明细
     *
     * @param details 明细列表
     * @return 插入数量
     */
    int batchInsert(@Param("details") List<DailyCheckDetail> details);

    /**
     * 批量逻辑删除(软删除)
     *
     * @param ids 明细ID列表
     * @return 删除数量
     */
    int batchDeleteByIds(@Param("ids") List<Long> ids);
}
