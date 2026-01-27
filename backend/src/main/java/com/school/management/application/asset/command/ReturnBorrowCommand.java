package com.school.management.application.asset.command;

import lombok.Builder;
import lombok.Data;

/**
 * 归还借用命令
 */
@Data
@Builder
public class ReturnBorrowCommand {
    /**
     * 借用记录ID
     */
    private Long borrowId;

    /**
     * 归还状况: good-完好 damaged-损坏 lost-丢失
     */
    private String returnCondition;

    /**
     * 归还备注
     */
    private String returnRemark;

    /**
     * 归还经办人ID
     */
    private Long returnerId;

    /**
     * 归还经办人姓名
     */
    private String returnerName;
}
