package com.school.management.infrastructure.persistence.asset;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

/**
 * 资产借用记录Mapper
 */
@Mapper
public interface AssetBorrowMapper extends BaseMapper<AssetBorrowPO> {

    /**
     * 查找已逾期的借用记录
     */
    @Select("SELECT * FROM asset_borrow WHERE status = 1 AND borrow_type = 2 " +
            "AND expected_return_date < #{today} AND deleted = 0")
    List<AssetBorrowPO> selectOverdue(@Param("today") LocalDate today);

    /**
     * 查找即将到期的借用记录
     */
    @Select("SELECT * FROM asset_borrow WHERE status = 1 AND borrow_type = 2 " +
            "AND expected_return_date BETWEEN #{today} AND #{targetDate} AND deleted = 0")
    List<AssetBorrowPO> selectNearExpiry(@Param("today") LocalDate today,
                                         @Param("targetDate") LocalDate targetDate);

    /**
     * 获取今日最大借用单号
     */
    @Select("SELECT MAX(borrow_no) FROM asset_borrow WHERE borrow_no LIKE CONCAT(#{prefix}, '%')")
    String selectMaxBorrowNo(@Param("prefix") String prefix);

    /**
     * 检查资产是否有未归还的借用记录
     */
    @Select("SELECT COUNT(*) FROM asset_borrow WHERE asset_id = #{assetId} " +
            "AND status IN (1, 3) AND deleted = 0")
    int countUnreturnedByAssetId(@Param("assetId") Long assetId);
}
