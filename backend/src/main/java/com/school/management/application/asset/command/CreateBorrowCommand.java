package com.school.management.application.asset.command;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

/**
 * 创建借用命令
 */
@Data
@Builder
public class CreateBorrowCommand {
    /**
     * 借用类型: 1-领用 2-借用
     */
    private Integer borrowType;

    /**
     * 资产ID
     */
    private Long assetId;

    /**
     * 借用数量
     */
    private Integer quantity;

    /**
     * 借用人ID
     */
    private Long borrowerId;

    /**
     * 借用人姓名
     */
    private String borrowerName;

    /**
     * 借用人部门
     */
    private String borrowerDept;

    /**
     * 借用人电话
     */
    private String borrowerPhone;

    /**
     * 预计归还日期（借用必填）
     */
    private LocalDate expectedReturnDate;

    /**
     * 借用原因/用途
     */
    private String purpose;

    /**
     * 操作人ID
     */
    private Long operatorId;

    /**
     * 操作人姓名
     */
    private String operatorName;
}
