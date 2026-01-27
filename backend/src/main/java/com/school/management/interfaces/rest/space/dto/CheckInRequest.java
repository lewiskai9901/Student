package com.school.management.interfaces.rest.space.dto;

import com.school.management.domain.space.model.valueobject.OccupantType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 入住请求
 */
@Data
public class CheckInRequest {

    @NotNull(message = "占用者类型不能为空")
    private OccupantType occupantType;

    @NotNull(message = "占用者ID不能为空")
    private Long occupantId;

    /**
     * 位置编号（如床位号）
     */
    private Integer positionNo;

    /**
     * 备注
     */
    private String remark;
}
