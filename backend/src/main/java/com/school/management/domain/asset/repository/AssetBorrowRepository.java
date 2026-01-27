package com.school.management.domain.asset.repository;

import com.school.management.domain.asset.model.aggregate.AssetBorrow;
import com.school.management.domain.asset.model.valueobject.BorrowStatus;
import com.school.management.domain.asset.model.valueobject.BorrowType;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 资产借用仓储接口
 */
public interface AssetBorrowRepository {

    /**
     * 保存借用记录
     */
    void save(AssetBorrow borrow);

    /**
     * 根据ID查找
     */
    Optional<AssetBorrow> findById(Long id);

    /**
     * 根据借用单号查找
     */
    Optional<AssetBorrow> findByBorrowNo(String borrowNo);

    /**
     * 生成借用单号
     */
    String generateBorrowNo();

    /**
     * 根据资产ID和状态查找
     */
    List<AssetBorrow> findByAssetIdAndStatus(Long assetId, BorrowStatus status);

    /**
     * 检查资产是否有未归还的借用记录
     */
    boolean hasUnreturnedBorrow(Long assetId);

    /**
     * 根据借用人ID查找
     */
    List<AssetBorrow> findByBorrowerId(Long borrowerId);

    /**
     * 查找已逾期的借用记录
     */
    List<AssetBorrow> findOverdue();

    /**
     * 查找即将到期的借用记录（N天内）
     */
    List<AssetBorrow> findNearExpiry(int days);

    /**
     * 分页查询
     */
    List<AssetBorrow> findAll(BorrowType borrowType, BorrowStatus status, Long borrowerId,
                              String keyword, int pageNum, int pageSize);

    /**
     * 统计总数
     */
    long count(BorrowType borrowType, BorrowStatus status, Long borrowerId, String keyword);

    /**
     * 根据状态查找
     */
    List<AssetBorrow> findByStatus(BorrowStatus status);
}
