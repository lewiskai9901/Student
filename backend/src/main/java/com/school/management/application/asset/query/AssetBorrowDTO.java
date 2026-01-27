package com.school.management.application.asset.query;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 资产借用DTO
 */
@Data
@Builder
public class AssetBorrowDTO {

    private Long id;
    private String borrowNo;
    private Integer borrowType;
    private String borrowTypeDesc;

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
    private String returnCondition;
    private String returnConditionDesc;
    private String returnRemark;
    private Long returnerId;
    private String returnerName;

    // 申请信息
    private String purpose;
    private Integer status;
    private String statusDesc;

    // 操作信息
    private Long operatorId;
    private String operatorName;

    // 计算字段
    private Boolean overdue;
    private Long overdueDays;
    private Long remainingDays;

    private LocalDateTime createdAt;
}
