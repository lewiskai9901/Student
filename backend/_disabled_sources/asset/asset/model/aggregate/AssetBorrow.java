package com.school.management.domain.asset.model.aggregate;

import com.school.management.domain.asset.model.valueobject.BorrowStatus;
import com.school.management.domain.asset.model.valueobject.BorrowType;
import com.school.management.domain.asset.model.valueobject.ReturnCondition;
import com.school.management.domain.shared.AggregateRoot;
import com.school.management.exception.BusinessException;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 资产借用聚合根
 */
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

    public AssetBorrow() {}

    // Getters
    public String getBorrowNo() { return borrowNo; }
    public BorrowType getBorrowType() { return borrowType; }
    public Long getAssetId() { return assetId; }
    public String getAssetCode() { return assetCode; }
    public String getAssetName() { return assetName; }
    public Integer getQuantity() { return quantity; }
    public Long getBorrowerId() { return borrowerId; }
    public String getBorrowerName() { return borrowerName; }
    public String getBorrowerDept() { return borrowerDept; }
    public String getBorrowerPhone() { return borrowerPhone; }
    public LocalDateTime getBorrowDate() { return borrowDate; }
    public LocalDate getExpectedReturnDate() { return expectedReturnDate; }
    public LocalDateTime getActualReturnDate() { return actualReturnDate; }
    public ReturnCondition getReturnCondition() { return returnCondition; }
    public String getReturnRemark() { return returnRemark; }
    public Long getReturnerId() { return returnerId; }
    public String getReturnerName() { return returnerName; }
    public String getPurpose() { return purpose; }
    public BorrowStatus getStatus() { return status; }
    public Long getOperatorId() { return operatorId; }
    public String getOperatorName() { return operatorName; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    // Setters
    public void setBorrowNo(String borrowNo) { this.borrowNo = borrowNo; }
    public void setBorrowType(BorrowType borrowType) { this.borrowType = borrowType; }
    public void setAssetId(Long assetId) { this.assetId = assetId; }
    public void setAssetCode(String assetCode) { this.assetCode = assetCode; }
    public void setAssetName(String assetName) { this.assetName = assetName; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public void setBorrowerId(Long borrowerId) { this.borrowerId = borrowerId; }
    public void setBorrowerName(String borrowerName) { this.borrowerName = borrowerName; }
    public void setBorrowerDept(String borrowerDept) { this.borrowerDept = borrowerDept; }
    public void setBorrowerPhone(String borrowerPhone) { this.borrowerPhone = borrowerPhone; }
    public void setBorrowDate(LocalDateTime borrowDate) { this.borrowDate = borrowDate; }
    public void setExpectedReturnDate(LocalDate expectedReturnDate) { this.expectedReturnDate = expectedReturnDate; }
    public void setActualReturnDate(LocalDateTime actualReturnDate) { this.actualReturnDate = actualReturnDate; }
    public void setReturnCondition(ReturnCondition returnCondition) { this.returnCondition = returnCondition; }
    public void setReturnRemark(String returnRemark) { this.returnRemark = returnRemark; }
    public void setReturnerId(Long returnerId) { this.returnerId = returnerId; }
    public void setReturnerName(String returnerName) { this.returnerName = returnerName; }
    public void setPurpose(String purpose) { this.purpose = purpose; }
    public void setStatus(BorrowStatus status) { this.status = status; }
    public void setOperatorId(Long operatorId) { this.operatorId = operatorId; }
    public void setOperatorName(String operatorName) { this.operatorName = operatorName; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

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

    // Builder pattern
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final AssetBorrow instance = new AssetBorrow();

        public Builder borrowNo(String borrowNo) { instance.borrowNo = borrowNo; return this; }
        public Builder borrowType(BorrowType borrowType) { instance.borrowType = borrowType; return this; }
        public Builder assetId(Long assetId) { instance.assetId = assetId; return this; }
        public Builder assetCode(String assetCode) { instance.assetCode = assetCode; return this; }
        public Builder assetName(String assetName) { instance.assetName = assetName; return this; }
        public Builder quantity(Integer quantity) { instance.quantity = quantity; return this; }
        public Builder borrowerId(Long borrowerId) { instance.borrowerId = borrowerId; return this; }
        public Builder borrowerName(String borrowerName) { instance.borrowerName = borrowerName; return this; }
        public Builder borrowerDept(String borrowerDept) { instance.borrowerDept = borrowerDept; return this; }
        public Builder borrowerPhone(String borrowerPhone) { instance.borrowerPhone = borrowerPhone; return this; }
        public Builder borrowDate(LocalDateTime borrowDate) { instance.borrowDate = borrowDate; return this; }
        public Builder expectedReturnDate(LocalDate expectedReturnDate) { instance.expectedReturnDate = expectedReturnDate; return this; }
        public Builder actualReturnDate(LocalDateTime actualReturnDate) { instance.actualReturnDate = actualReturnDate; return this; }
        public Builder returnCondition(ReturnCondition returnCondition) { instance.returnCondition = returnCondition; return this; }
        public Builder returnRemark(String returnRemark) { instance.returnRemark = returnRemark; return this; }
        public Builder returnerId(Long returnerId) { instance.returnerId = returnerId; return this; }
        public Builder returnerName(String returnerName) { instance.returnerName = returnerName; return this; }
        public Builder purpose(String purpose) { instance.purpose = purpose; return this; }
        public Builder status(BorrowStatus status) { instance.status = status; return this; }
        public Builder operatorId(Long operatorId) { instance.operatorId = operatorId; return this; }
        public Builder operatorName(String operatorName) { instance.operatorName = operatorName; return this; }
        public Builder createdAt(LocalDateTime createdAt) { instance.createdAt = createdAt; return this; }
        public Builder updatedAt(LocalDateTime updatedAt) { instance.updatedAt = updatedAt; return this; }

        public AssetBorrow build() { return instance; }
    }
}
