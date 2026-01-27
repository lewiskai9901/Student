package com.school.management.interfaces.rest.asset.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * 创建借用请求
 */
@Data
public class CreateBorrowRequest {

    /**
     * 借用类型: 1-领用(无需归还) 2-借用(需归还)
     */
    @NotNull(message = "借用类型不能为空")
    private Integer borrowType;

    /**
     * 资产ID
     */
    @NotNull(message = "资产ID不能为空")
    private Long assetId;

    /**
     * 借用数量
     */
    @Min(value = 1, message = "借用数量至少为1")
    private Integer quantity = 1;

    /**
     * 借用人ID
     */
    @NotNull(message = "借用人不能为空")
    private Long borrowerId;

    /**
     * 借用人姓名
     */
    @NotBlank(message = "借用人姓名不能为空")
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
     * 预计归还日期（借用类型必填）
     */
    private LocalDate expectedReturnDate;

    /**
     * 借用原因/用途
     */
    private String purpose;
}
