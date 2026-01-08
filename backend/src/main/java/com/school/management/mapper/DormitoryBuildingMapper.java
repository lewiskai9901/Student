package com.school.management.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.management.dto.DormitoryBuildingResponse;
import com.school.management.entity.DormitoryBuilding;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 宿舍楼Mapper接口
 *
 * @author system
 * @since 1.0.0
 */
@Mapper
public interface DormitoryBuildingMapper extends BaseMapper<DormitoryBuilding> {

    /**
     * 分页查询宿舍楼
     *
     * @param page 分页参数
     * @param request 查询条件
     * @return 宿舍楼分页列表
     */
    IPage<DormitoryBuildingResponse> selectBuildingPage(
            Page<DormitoryBuildingResponse> page,
            @Param("request") Object request
    );

    /**
     * 根据楼号查询
     *
     * @param buildingNo 楼号
     * @return 宿舍楼
     */
    DormitoryBuilding selectByBuildingNo(@Param("buildingNo") String buildingNo);

    /**
     * 检查楼号是否存在
     *
     * @param buildingNo 楼号
     * @param excludeId 排除的ID(用于更新时检查)
     * @return 数量
     */
    int countByBuildingNo(@Param("buildingNo") String buildingNo, @Param("excludeId") Long excludeId);

    /**
     * 根据宿舍类型查询
     *
     * @param buildingType 宿舍类型
     * @return 宿舍楼列表
     */
    List<DormitoryBuildingResponse> selectByBuildingType(@Param("buildingType") Integer buildingType);

    /**
     * 获取所有正常状态的宿舍楼
     *
     * @return 宿舍楼列表
     */
    List<DormitoryBuildingResponse> selectAllNormal();
}
