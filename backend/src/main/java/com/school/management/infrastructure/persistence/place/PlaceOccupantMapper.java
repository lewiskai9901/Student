package com.school.management.infrastructure.persistence.space;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 场所占用 Mapper
 */
@Mapper
public interface SpaceOccupantMapper extends BaseMapper<SpaceOccupantPO> {

    /**
     * 查询场所的在住占用者（带关联信息）
     */
    List<SpaceOccupantPO> selectActiveBySpaceId(@Param("spaceId") Long spaceId);

    /**
     * 查询场所的所有占用记录
     */
    List<SpaceOccupantPO> selectAllBySpaceId(@Param("spaceId") Long spaceId);

    /**
     * 根据占用者查找在住记录
     */
    SpaceOccupantPO selectActiveByOccupant(@Param("occupantType") String occupantType,
                                            @Param("occupantId") Long occupantId);

    /**
     * 根据位置查找在住占用者
     */
    SpaceOccupantPO selectActiveByPosition(@Param("spaceId") Long spaceId,
                                            @Param("positionNo") Integer positionNo);

    /**
     * 统计场所在住人数
     */
    int countActiveBySpaceId(@Param("spaceId") Long spaceId);

    /**
     * 获取已占用的位置列表
     */
    List<Integer> selectOccupiedPositions(@Param("spaceId") Long spaceId);

    /**
     * 批量退出
     */
    void batchCheckOut(@Param("spaceId") Long spaceId);
}
