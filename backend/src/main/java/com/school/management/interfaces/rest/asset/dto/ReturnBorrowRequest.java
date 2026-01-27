package com.school.management.interfaces.rest.asset.dto;

import lombok.Data;

/**
 * 归还借用请求
 */
@Data
public class ReturnBorrowRequest {

    /**
     * 归还状况: good-完好 damaged-损坏 lost-丢失
     */
    private String returnCondition;

    /**
     * 归还备注
     */
    private String returnRemark;
}
