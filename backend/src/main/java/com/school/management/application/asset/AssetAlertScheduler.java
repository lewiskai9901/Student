package com.school.management.application.asset;

import com.school.management.domain.asset.model.aggregate.Asset;
import com.school.management.domain.asset.model.aggregate.AssetBorrow;
import com.school.management.domain.asset.model.valueobject.AlertLevel;
import com.school.management.domain.asset.model.valueobject.AlertType;
import com.school.management.domain.asset.model.valueobject.BorrowStatus;
import com.school.management.domain.asset.model.valueobject.ManagementMode;
import com.school.management.domain.asset.repository.AssetBorrowRepository;
import com.school.management.domain.asset.repository.AssetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 资产预警定时任务
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AssetAlertScheduler {

    private final AssetRepository assetRepository;
    private final AssetBorrowRepository borrowRepository;
    private final AssetAlertApplicationService alertService;
    private final AssetApprovalApplicationService approvalService;

    /**
     * 每天凌晨2点检查资产预警
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void checkAssetAlerts() {
        log.info("Starting asset alert check...");
        try {
            checkOverdueBorrows();
            checkNearOverdueBorrows();
            checkWarrantyExpiry();
            checkLowStock();
            processExpiredApprovals();
            log.info("Asset alert check completed.");
        } catch (Exception e) {
            log.error("Asset alert check failed", e);
        }
    }

    /**
     * 检查逾期借用
     */
    private void checkOverdueBorrows() {
        log.info("Checking overdue borrows...");
        List<AssetBorrow> overdueBorrows = borrowRepository.findOverdue();
        int count = 0;
        for (AssetBorrow borrow : overdueBorrows) {
            if (borrow.getExpectedReturnDate() == null) continue;

            long overdueDays = ChronoUnit.DAYS.between(borrow.getExpectedReturnDate(), LocalDate.now());
            String content = String.format("资产【%s】已逾期 %d 天未归还，借用人：%s",
                    borrow.getAssetName(), overdueDays, borrow.getBorrowerName());

            AlertLevel level = overdueDays > 7 ? AlertLevel.URGENT :
                              (overdueDays > 3 ? AlertLevel.IMPORTANT : AlertLevel.NORMAL);

            Long alertId = alertService.createBorrowAlert(
                    AlertType.OVERDUE,
                    borrow.getAssetId(),
                    borrow.getAssetCode(),
                    borrow.getAssetName(),
                    borrow.getId(),
                    content,
                    level,
                    borrow.getBorrowerId(),
                    borrow.getBorrowerName()
            );

            if (alertId != null) count++;
        }
        log.info("Created {} overdue alerts", count);
    }

    /**
     * 检查即将逾期借用（3天内）
     */
    private void checkNearOverdueBorrows() {
        log.info("Checking near overdue borrows...");
        List<AssetBorrow> activeBorrows = borrowRepository.findByStatus(BorrowStatus.BORROWED);
        int count = 0;
        LocalDate today = LocalDate.now();
        LocalDate warningDate = today.plusDays(3);

        for (AssetBorrow borrow : activeBorrows) {
            if (borrow.getExpectedReturnDate() == null) continue;

            LocalDate expectedReturn = borrow.getExpectedReturnDate();
            if (expectedReturn.isAfter(today) && !expectedReturn.isAfter(warningDate)) {
                long daysLeft = ChronoUnit.DAYS.between(today, expectedReturn);
                String content = String.format("资产【%s】将在 %d 天后到期，请及时归还",
                        borrow.getAssetName(), daysLeft);

                Long alertId = alertService.createBorrowAlert(
                        AlertType.NEAR_OVERDUE,
                        borrow.getAssetId(),
                        borrow.getAssetCode(),
                        borrow.getAssetName(),
                        borrow.getId(),
                        content,
                        AlertLevel.NORMAL,
                        borrow.getBorrowerId(),
                        borrow.getBorrowerName()
                );

                if (alertId != null) count++;
            }
        }
        log.info("Created {} near overdue alerts", count);
    }

    /**
     * 检查保修即将到期（30天内）
     */
    private void checkWarrantyExpiry() {
        log.info("Checking warranty expiry...");
        List<Asset> assets = assetRepository.findWarrantyExpiringWithin(30);
        int count = 0;
        LocalDate today = LocalDate.now();

        for (Asset asset : assets) {
            if (asset.getWarrantyDate() == null) continue;

            long daysLeft = ChronoUnit.DAYS.between(today, asset.getWarrantyDate());
            String content;
            AlertLevel level;

            if (daysLeft <= 0) {
                content = String.format("资产【%s】保修已过期", asset.getAssetName());
                level = AlertLevel.IMPORTANT;
            } else {
                content = String.format("资产【%s】保修将在 %d 天后到期", asset.getAssetName(), daysLeft);
                level = daysLeft <= 7 ? AlertLevel.IMPORTANT : AlertLevel.NORMAL;
            }

            Long alertId = alertService.createAlert(
                    AlertType.WARRANTY_EXPIRE,
                    asset.getId(),
                    asset.getAssetCode(),
                    asset.getAssetName(),
                    content,
                    level
            );

            if (alertId != null) count++;
        }
        log.info("Created {} warranty expiry alerts", count);
    }

    /**
     * 检查库存不足（批量管理资产）
     */
    private void checkLowStock() {
        log.info("Checking low stock...");
        List<Asset> batchAssets = assetRepository.findByManagementMode(ManagementMode.BATCH);
        int count = 0;

        for (Asset asset : batchAssets) {
            int available = asset.getAvailableQuantity();
            int threshold = asset.getStockWarningThreshold() != null ?
                    asset.getStockWarningThreshold() : 10;

            if (available <= threshold) {
                String content = String.format("资产【%s】库存不足，当前库存：%d，预警阈值：%d",
                        asset.getAssetName(), available, threshold);

                AlertLevel level = available == 0 ? AlertLevel.URGENT :
                                  (available <= threshold / 2 ? AlertLevel.IMPORTANT : AlertLevel.NORMAL);

                Long alertId = alertService.createAlert(
                        AlertType.LOW_STOCK,
                        asset.getId(),
                        asset.getAssetCode(),
                        asset.getAssetName(),
                        content,
                        level
                );

                if (alertId != null) count++;
            }
        }
        log.info("Created {} low stock alerts", count);
    }

    /**
     * 处理过期审批
     */
    private void processExpiredApprovals() {
        log.info("Processing expired approvals...");
        approvalService.processExpiredApprovals();
    }
}
