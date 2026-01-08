package com.school.management.dto.request;

import lombok.Data;

import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 申诉创建请求DTO
 *
 * @author system
 * @version 3.0.0
 * @since 2024-12-29
 */
@Data
public class AppealCreateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 检查记录ID
     */
    @NotNull(message = "检查记录ID不能为空")
    private Long recordId;

    /**
     * 扣分明细ID
     */
    @NotNull(message = "扣分明细ID不能为空")
    private Long itemId;

    /**
     * 申诉原因
     */
    @NotBlank(message = "申诉原因不能为空")
    @Size(max = 2000, message = "申诉原因不能超过2000字符")
    private String reason;

    /**
     * 期望调整后的分数
     */
    @DecimalMin(value = "0.0", message = "期望分数不能为负数")
    private BigDecimal expectedScore;

    /**
     * 证明材料(附件ID列表)
     */
    @Size(max = 10, message = "证明材料最多10个")
    private List<Long> evidenceIds;

    /**
     * 证明材料描述
     */
    @Size(max = 1000, message = "证明材料描述不能超过1000字符")
    private String evidenceDescription;

    /**
     * 联系电话
     */
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String contactPhone;

    /**
     * 申诉类型(1=分数有误,2=情节特殊,3=流程不当,4=其他)
     */
    private Integer appealType;

    /**
     * 是否紧急
     */
    private Boolean isUrgent;

    /**
     * 备注
     */
    @Size(max = 500, message = "备注不能超过500字符")
    private String remark;
}
