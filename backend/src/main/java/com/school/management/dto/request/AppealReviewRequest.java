package com.school.management.dto.request;

import com.school.management.exception.BusinessException;
import lombok.Data;

import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 申诉审核请求DTO
 *
 * @author system
 * @version 3.0.0
 * @since 2024-12-29
 */
@Data
public class AppealReviewRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 申诉ID
     */
    @NotNull(message = "申诉ID不能为空")
    private Long appealId;

    /**
     * 审批结果(2=同意,3=驳回,4=转交)
     */
    @NotNull(message = "审批结果不能为空")
    @Min(value = 2, message = "审批结果值不合法")
    @Max(value = 4, message = "审批结果值不合法")
    private Integer approvalStatus;

    /**
     * 审批意见
     */
    @NotBlank(message = "审批意见不能为空")
    @Size(max = 2000, message = "审批意见不能超过2000字符")
    private String approvalOpinion;

    /**
     * 调整后的分数(当同意申诉时)
     */
    @DecimalMin(value = "0.0", message = "调整后分数不能为负数")
    private BigDecimal adjustedScore;

    /**
     * 转交给谁(当approvalStatus=4时必填)
     */
    private Long transferredTo;

    /**
     * 转交原因(当approvalStatus=4时必填)
     */
    @Size(max = 500, message = "转交原因不能超过500字符")
    private String transferReason;

    /**
     * 附件ID列表
     */
    private java.util.List<Long> attachmentIds;

    /**
     * 是否通知申诉人
     */
    private Boolean notifyApplicant;

    /**
     * 是否通知相关人员
     */
    private Boolean notifyRelated;

    /**
     * 备注
     */
    @Size(max = 500, message = "备注不能超过500字符")
    private String remark;

    /**
     * 验证转交相关字段
     */
    public void validateTransfer() {
        if (approvalStatus != null && approvalStatus == 4) {
            if (transferredTo == null) {
                throw new BusinessException("转交时必须指定转交对象");
            }
            if (transferReason == null || transferReason.trim().isEmpty()) {
                throw new BusinessException("转交时必须填写转交原因");
            }
        }
    }

    /**
     * 验证同意申诉时的分数
     * 注意: adjustedScore 是可选的，如果不填写则保持原扣分
     */
    public void validateAdjustedScore() {
        // adjustedScore 是可选的，不再强制要求
        // 如果不填写，则保持原扣分不变
    }
}
