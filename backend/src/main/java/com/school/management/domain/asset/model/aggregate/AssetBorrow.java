package com.school.management.domain.asset.model.aggregate;

import com.school.management.domain.asset.model.valueobject.BorrowStatus;
import com.school.management.domain.asset.model.valueobject.BorrowType;
import com.school.management.domain.asset.model.valueobject.ReturnCondition;
import com.school.management.domain.shared.AggregateRoot;
import com.school.management.exception.BusinessException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 资产借用聚合根
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetBorrow extends AggregateRoot<Long> {

    private String borrowNo;
    private BorrowType borrowType;

    // 资产信息
    private Long assetId;
    private String assetCode;
    private String assetName;
    private Integer quantity;

    // 借用人信息
    private Long borrowerId;
    private String borrowerName;
    private String borrowerDept;
    private String borrowerPhone;

    // 时间信息
    private LocalDateTime borrowDate;
    private LocalDate expectedReturnDate;
    private LocalDateTime actualReturnDate;

    // 归还信息
    private ReturnCondition returnCondition;
    private String returnRemark;
    private Long returnerId;
    private String returnerName;

    // 申请信息
    private String purpose;
    private BorrowStatus status;

    // 操作信息
    private Long operatorId;
    private String operatorName;

    // 通用字段
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 创建借用记录
     */
    public static AssetBorrow create(
            String borrowNo,
            BorrowType borrowType,
            Asset asset,
            Integer quantity,
            Long borrowerId,
            String borrowerName,
            String borrowerDept,
            String borrowerPhone,
            LocalDate expectedReturnDate,
            String purpose,
            Long operatorId,
            String operatorName
    ) {
        // 验证
        if (borrowType == BorrowType.BORROW && expectedReturnDate == null) {
            throw new BusinessException("借用类型必须填写预计归还日期");
        }

        AssetBorrow borrow = new AssetBorrow();
        borrow.setBorrowNo(borrowNo);
        borrow.setBorrowType(borrowType);

        // 资产信息快照
        borrow.setAssetId(asset.getId());
        borrow.setAssetCode(asset.getAssetCode());
        borrow.setAssetName(asset.getAssetName());
        borrow.setQuantity(quantity != null ? quantity : 1);

        // 借用人
        borrow.setBorrowerId(borrowerId);
        borrow.setBorrowerName(borrowerName);
        borrow.setBorrowerDept(borrowerDept);
        borrow.setBorrowerPhone(borrowerPhone);

        // 时间
        borrow.setBorrowDate(LocalDateTime.now());
        borrow.setExpectedReturnDate(expectedReturnDate);

        // 申请
        borrow.setPurpose(purpose);
        borrow.setStatus(BorrowStatus.BORROWED);

        // 操作
        borrow.setOperatorId(operatorId);
        borrow.setOperatorName(operatorName);

        borrow.setCreatedAt(LocalDateTime.now());
        borrow.setUpdatedAt(LocalDateTime.now());

        return borrow;
    }

    /**
     * 归还资产
     */
    public void returnAsset(
            ReturnCondition condition,
            String remark,
            Long returnerId,
            String returnerName
    ) {
        if (this.status != BorrowStatus.BORROWED && this.status != BorrowStatus.OVERDUE) {
            throw new BusinessException("当前状态不允许归还");
        }

        this.returnCondition = condition;
        this.returnRemark = remark;
        this.returnerId = returnerId;
        this.returnerName = returnerName;
        this.actualReturnDate = LocalDateTime.now();
        this.status = BorrowStatus.RETURNED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 标记为逾期
     */
    public void markAsOverdue() {
        if (this.status != BorrowStatus.BORROWED) {
            return;
        }
        this.status = BorrowStatus.OVERDUE;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 取消借用
     */
    public void cancel() {
        if (this.status != BorrowStatus.BORROWED) {
            throw new BusinessException("只有借出中的记录才能取消");
        }
        this.status = BorrowStatus.CANCELLED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 检查是否已逾期
     */
    public boolean isOverdue() {
        if (this.borrowType != BorrowType.BORROW || this.expectedReturnDate == null) {
            return false;
        }
        if (this.status == BorrowStatus.RETURNED || this.status == BorrowStatus.CANCELLED) {
            return false;
        }
        return LocalDate.now().isAfter(this.expectedReturnDate);
    }

    /**
     * 获取逾期天数
     */
    public long getOverdueDays() {
        if (!isOverdue()) {
            return 0;
        }
        return java.time.temporal.ChronoUnit.DAYS.between(this.expectedReturnDate, LocalDate.now());
    }

    /**
     * 是否需要归还
     */
    public boolean needReturn() {
        return this.borrowType == BorrowType.BORROW;
    }
}
