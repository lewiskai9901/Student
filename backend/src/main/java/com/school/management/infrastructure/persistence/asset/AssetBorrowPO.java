package com.school.management.infrastructure.persistence.asset;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 资产借用记录持久化对象
 */
@Data
@TableName("asset_borrow")
public class AssetBorrowPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String borrowNo;
    private Integer borrowType;

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
    private String returnRemark;
    private Long returnerId;
    private String returnerName;

    // 申请信息
    private String purpose;
    private Integer status;

    // 操作信息
    private Long operatorId;
    private String operatorName;

    // 通用字段
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
