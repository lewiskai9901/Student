package com.school.management.interfaces.rest.asset;

import lombok.Data;

/**
 * 标签生成请求
 */
@Data
public class LabelRequest {
    private Long assetId;
    private String assetCode;
    private String assetName;
    private String location;
}
