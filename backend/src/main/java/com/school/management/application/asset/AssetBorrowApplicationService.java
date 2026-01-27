package com.school.management.application.asset;

import com.school.management.application.asset.command.CreateBorrowCommand;
import com.school.management.application.asset.command.ReturnBorrowCommand;
import com.school.management.application.asset.query.AssetBorrowDTO;
import com.school.management.common.PageResult;
import com.school.management.domain.asset.model.aggregate.Asset;
import com.school.management.domain.asset.model.aggregate.AssetBorrow;
import com.school.management.domain.asset.model.valueobject.*;
import com.school.management.domain.asset.repository.AssetBorrowRepository;
import com.school.management.domain.asset.repository.AssetRepository;
import com.school.management.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 资产借用应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssetBorrowApplicationService {

    private final AssetBorrowRepository borrowRepository;
    private final AssetRepository assetRepository;

    /**
     * 创建借用记录
     */
    @Transactional
    public Long createBorrow(CreateBorrowCommand command) {
        // 1. 查找资产
        Asset asset = assetRepository.findById(command.getAssetId())
                .orElseThrow(() -> new BusinessException("资产不存在"));

        // 2. 检查资产状态
        if (asset.getStatus() == AssetStatus.SCRAPPED) {
            throw new BusinessException("已报废资产不能借用");
        }
        if (asset.getStatus() == AssetStatus.REPAIRING) {
            throw new BusinessException("维修中的资产不能借用");
        }

        int borrowQuantity = command.getQuantity() != null ? command.getQuantity() : 1;
        BorrowType borrowType = BorrowType.fromCode(command.getBorrowType());

        // 3. 根据管理模式检查借用条件
        if (asset.isBatchManagement()) {
            // 批量管理：检查库存是否足够
            if (!asset.canBorrow(borrowQuantity)) {
                throw new BusinessException(
                    String.format("库存不足，当前库存: %d, 需要: %d", asset.getAvailableQuantity(), borrowQuantity)
                );
            }
        } else {
            // 单品管理：检查是否有未归还的借用
            if (borrowRepository.hasUnreturnedBorrow(command.getAssetId())) {
                throw new BusinessException("该资产已被借出，请等待归还后再借用");
            }
            // 单品管理下数量必须为1
            borrowQuantity = 1;
        }

        // 4. 生成借用单号
        String borrowNo = borrowRepository.generateBorrowNo();

        // 5. 创建借用记录
        AssetBorrow borrow = AssetBorrow.create(
                borrowNo,
                borrowType,
                asset,
                borrowQuantity,
                command.getBorrowerId(),
                command.getBorrowerName(),
                command.getBorrowerDept(),
                command.getBorrowerPhone(),
                command.getExpectedReturnDate(),
                command.getPurpose(),
                command.getOperatorId(),
                command.getOperatorName()
        );

        // 6. 更新资产状态/库存
        if (asset.isBatchManagement()) {
            // 批量管理：领用时扣减库存
            if (borrowType == BorrowType.USE) {
                asset.deductQuantity(borrowQuantity);
                assetRepository.save(asset);
                log.info("批量资产领用，扣减库存: {}, 数量: {}, 剩余库存: {}",
                        asset.getAssetName(), borrowQuantity, asset.getAvailableQuantity());
            }
            // 批量管理的借用不扣减库存，因为需要归还
        } else {
            // 单品管理：更新资产状态为在用
            asset.setStatus(AssetStatus.IN_USE);
            assetRepository.save(asset);
        }

        borrowRepository.save(borrow);
        log.info("创建借用记录: {}, 借用人: {}, 资产: {}, 数量: {}",
                borrowNo, command.getBorrowerName(), asset.getAssetName(), borrowQuantity);

        return borrow.getId();
    }

    /**
     * 归还资产
     */
    @Transactional
    public void returnBorrow(ReturnBorrowCommand command) {
        // 1. 查找借用记录
        AssetBorrow borrow = borrowRepository.findById(command.getBorrowId())
                .orElseThrow(() -> new BusinessException("借用记录不存在"));

        // 2. 查找资产
        Asset asset = assetRepository.findById(borrow.getAssetId())
                .orElseThrow(() -> new BusinessException("资产不存在"));

        // 3. 执行归还
        ReturnCondition condition = command.getReturnCondition() != null
                ? ReturnCondition.fromCode(command.getReturnCondition())
                : ReturnCondition.GOOD;

        borrow.returnAsset(
                condition,
                command.getReturnRemark(),
                command.getReturnerId(),
                command.getReturnerName()
        );

        // 4. 更新资产状态/库存
        if (asset.isBatchManagement()) {
            // 批量管理：仅借用(BORROW)类型需要归还，不扣减库存的不需要恢复
            // 注意：领用(USE)类型已扣减库存且不需要归还
            if (borrow.getBorrowType() == BorrowType.BORROW && condition != ReturnCondition.LOST) {
                // 借用归还时恢复库存（丢失除外）
                asset.restoreQuantity(borrow.getQuantity());
                assetRepository.save(asset);
                log.info("批量资产归还，恢复库存: {}, 数量: {}, 当前库存: {}",
                        asset.getAssetName(), borrow.getQuantity(), asset.getAvailableQuantity());
            }
        } else {
            // 单品管理：更新资产状态
            if (condition == ReturnCondition.LOST) {
                // 丢失：标记为报废
                asset.scrap();
            } else if (condition == ReturnCondition.DAMAGED) {
                // 损坏：标记为维修中
                asset.markAsRepairing();
            } else {
                // 完好：标记为闲置
                asset.markAsIdle();
            }
            assetRepository.save(asset);
        }

        borrowRepository.save(borrow);
        log.info("资产归还: {}, 状况: {}", borrow.getBorrowNo(), condition.getDescription());
    }

    /**
     * 取消借用
     */
    @Transactional
    public void cancelBorrow(Long borrowId) {
        AssetBorrow borrow = borrowRepository.findById(borrowId)
                .orElseThrow(() -> new BusinessException("借用记录不存在"));

        // 查找资产
        Asset asset = assetRepository.findById(borrow.getAssetId())
                .orElseThrow(() -> new BusinessException("资产不存在"));

        borrow.cancel();

        // 恢复资产状态/库存
        if (asset.isBatchManagement()) {
            // 批量管理：如果是领用(USE)且已扣减库存，需要恢复
            if (borrow.getBorrowType() == BorrowType.USE) {
                asset.restoreQuantity(borrow.getQuantity());
                assetRepository.save(asset);
                log.info("取消领用，恢复库存: {}, 数量: {}, 当前库存: {}",
                        asset.getAssetName(), borrow.getQuantity(), asset.getAvailableQuantity());
            }
        } else {
            // 单品管理：恢复资产状态为闲置
            asset.markAsIdle();
            assetRepository.save(asset);
        }

        borrowRepository.save(borrow);
        log.info("取消借用: {}", borrow.getBorrowNo());
    }

    /**
     * 获取借用详情
     */
    public AssetBorrowDTO getBorrow(Long id) {
        return borrowRepository.findById(id)
                .map(this::toDTO)
                .orElseThrow(() -> new BusinessException("借用记录不存在"));
    }

    /**
     * 分页查询借用记录
     */
    public PageResult<AssetBorrowDTO> listBorrows(
            Integer borrowType,
            Integer status,
            Long borrowerId,
            String keyword,
            int pageNum,
            int pageSize
    ) {
        BorrowType type = borrowType != null ? BorrowType.fromCode(borrowType) : null;
        BorrowStatus borrowStatus = status != null ? BorrowStatus.fromCode(status) : null;

        List<AssetBorrowDTO> list = borrowRepository.findAll(type, borrowStatus, borrowerId, keyword, pageNum, pageSize)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

        long total = borrowRepository.count(type, borrowStatus, borrowerId, keyword);

        return PageResult.of(list, total, pageNum, pageSize);
    }

    /**
     * 获取用户的借用记录
     */
    public List<AssetBorrowDTO> getMyBorrows(Long userId) {
        return borrowRepository.findByBorrowerId(userId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取资产的借用历史
     */
    public List<AssetBorrowDTO> getAssetBorrowHistory(Long assetId) {
        return borrowRepository.findByAssetIdAndStatus(assetId, null).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取已逾期的借用记录
     */
    public List<AssetBorrowDTO> getOverdueBorrows() {
        return borrowRepository.findOverdue().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 更新逾期状态（定时任务调用）
     */
    @Transactional
    public void updateOverdueStatus() {
        List<AssetBorrow> overdueList = borrowRepository.findOverdue();
        for (AssetBorrow borrow : overdueList) {
            if (borrow.getStatus() == BorrowStatus.BORROWED) {
                borrow.markAsOverdue();
                borrowRepository.save(borrow);
                log.info("借用记录 {} 已标记为逾期", borrow.getBorrowNo());
            }
        }
    }

    /**
     * 统计借用数据
     */
    public BorrowStatistics getStatistics() {
        long borrowedCount = borrowRepository.count(BorrowType.BORROW, BorrowStatus.BORROWED, null, null);
        long overdueCount = borrowRepository.count(null, BorrowStatus.OVERDUE, null, null);
        long usedCount = borrowRepository.count(BorrowType.USE, null, null, null);

        return BorrowStatistics.builder()
                .borrowedCount(borrowedCount)
                .overdueCount(overdueCount)
                .usedCount(usedCount)
                .build();
    }

    private AssetBorrowDTO toDTO(AssetBorrow borrow) {
        LocalDate today = LocalDate.now();
        boolean overdue = borrow.isOverdue();
        long overdueDays = borrow.getOverdueDays();

        Long remainingDays = null;
        if (borrow.getBorrowType() == BorrowType.BORROW
                && borrow.getExpectedReturnDate() != null
                && borrow.getStatus() == BorrowStatus.BORROWED) {
            remainingDays = ChronoUnit.DAYS.between(today, borrow.getExpectedReturnDate());
        }

        return AssetBorrowDTO.builder()
                .id(borrow.getId())
                .borrowNo(borrow.getBorrowNo())
                .borrowType(borrow.getBorrowType() != null ? borrow.getBorrowType().getCode() : null)
                .borrowTypeDesc(borrow.getBorrowType() != null ? borrow.getBorrowType().getDescription() : null)
                .assetId(borrow.getAssetId())
                .assetCode(borrow.getAssetCode())
                .assetName(borrow.getAssetName())
                .quantity(borrow.getQuantity())
                .borrowerId(borrow.getBorrowerId())
                .borrowerName(borrow.getBorrowerName())
                .borrowerDept(borrow.getBorrowerDept())
                .borrowerPhone(borrow.getBorrowerPhone())
                .borrowDate(borrow.getBorrowDate())
                .expectedReturnDate(borrow.getExpectedReturnDate())
                .actualReturnDate(borrow.getActualReturnDate())
                .returnCondition(borrow.getReturnCondition() != null ? borrow.getReturnCondition().getCode() : null)
                .returnConditionDesc(borrow.getReturnCondition() != null ? borrow.getReturnCondition().getDescription() : null)
                .returnRemark(borrow.getReturnRemark())
                .returnerId(borrow.getReturnerId())
                .returnerName(borrow.getReturnerName())
                .purpose(borrow.getPurpose())
                .status(borrow.getStatus() != null ? borrow.getStatus().getCode() : null)
                .statusDesc(borrow.getStatus() != null ? borrow.getStatus().getDescription() : null)
                .operatorId(borrow.getOperatorId())
                .operatorName(borrow.getOperatorName())
                .overdue(overdue)
                .overdueDays(overdueDays)
                .remainingDays(remainingDays)
                .createdAt(borrow.getCreatedAt())
                .build();
    }

    /**
     * 借用统计
     */
    @lombok.Data
    @lombok.Builder
    public static class BorrowStatistics {
        private Long borrowedCount;  // 借出中数量
        private Long overdueCount;   // 已逾期数量
        private Long usedCount;      // 已领用数量
    }
}
