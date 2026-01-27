package com.school.management.interfaces.rest.asset.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Excel导入结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportResult {

    /**
     * 总行数
     */
    private Integer totalCount;

    /**
     * 成功数
     */
    private Integer successCount;

    /**
     * 失败数
     */
    private Integer failCount;

    /**
     * 错误明细
     */
    @Builder.Default
    private List<ImportError> errors = new ArrayList<>();

    /**
     * 导入错误详情
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImportError {
        /**
         * 行号
         */
        private Integer rowNum;

        /**
         * 资产名称
         */
        private String assetName;

        /**
         * 错误原因
         */
        private String errorMessage;
    }

    /**
     * 添加错误
     */
    public void addError(int rowNum, String assetName, String errorMessage) {
        if (errors == null) {
            errors = new ArrayList<>();
        }
        errors.add(ImportError.builder()
                .rowNum(rowNum)
                .assetName(assetName)
                .errorMessage(errorMessage)
                .build());
    }
}
