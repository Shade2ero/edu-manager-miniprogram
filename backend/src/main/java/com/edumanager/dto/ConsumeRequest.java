package com.edumanager.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.math.BigDecimal;

/**
 * 课时扣减请求 DTO
 */
@Data
public class ConsumeRequest {

    /** 学员ID */
    @NotNull(message = "学员ID不能为空")
    private Long studentId;

    /** 课程ID */
    @NotNull(message = "课程ID不能为空")
    private Long courseId;

    /** 扣减课时数（默认1课时），支持0.5课时等小数 */
    @NotNull(message = "扣减课时数不能为空")
    @Positive(message = "扣减课时数必须大于0")
    private BigDecimal amount = BigDecimal.ONE;

    /** 业务类型：ATTENDANCE（签到扣课）/ MANUAL（人工扣减） */
    @NotNull(message = "业务类型不能为空")
    private String bizType;

    /** 关联业务ID（如 attendance_log.id） */
    @NotNull(message = "业务ID不能为空")
    private Long bizId;

    /** 备注 */
    private String remark;
}
