package com.school.management.infrastructure.persistence.place;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 场所占用 Mapper
 */
@Mapper
public interface PlaceOccupantMapper extends BaseMapper<PlaceOccupantPO> {

    /**
     * 查询场所的在住占用者（带关联信息）
     */
    List<PlaceOccupantPO> selectActiveByPlaceId(@Param("placeId") Long placeId);

    /**
     * 查询场所的所有占用记录
     */
    List<PlaceOccupantPO> selectAllByPlaceId(@Param("placeId") Long placeId);

    /**
     * 根据占用者查找在住记录
     */
    PlaceOccupantPO selectActiveByOccupant(@Param("occupantType") String occupantType,
                                            @Param("occupantId") Long occupantId);

    /**
     * 根据位置查找在住占用者
     */
    PlaceOccupantPO selectActiveByPosition(@Param("placeId") Long placeId,
                                            @Param("positionNo") Integer positionNo);

    /**
     * 统计场所在住人数
     */
    int countActiveByPlaceId(@Param("placeId") Long placeId);

    /**
     * 获取已占用的位置列表
     */
    List<Integer> selectOccupiedPositions(@Param("placeId") Long placeId);

    /**
     * 批量退出
     */
    void batchCheckOut(@Param("placeId") Long placeId);
}
