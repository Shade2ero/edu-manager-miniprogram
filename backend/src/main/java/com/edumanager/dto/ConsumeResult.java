package com.edumanager.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 课时扣减返回结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsumeResult {

    /** 是否扣减成功 */
    private Boolean success;

    /** 扣减后剩余课时 */
    private BigDecimal remaining;

    /** 扣减前课时 */
    private BigDecimal balanceBefore;

    /** 实际扣减课时 */
    private BigDecimal consumedAmount;

    /** 流水ID */
    private Long transactionId;

    /** 是否触发了余额预警（剩余 <= 2 课时） */
    private Boolean lowBalanceWarning;

    /** 失败原因（success=false 时填充） */
    private String failReason;

    // ==================== 静态工厂 ====================

    public static ConsumeResult success(BigDecimal before, BigDecimal after,
                                        BigDecimal amount, Long txId, boolean lowBalance) {
        return ConsumeResult.builder()
                .success(true)
                .balanceBefore(before)
                .remaining(after)
                .consumedAmount(amount)
                .transactionId(txId)
                .lowBalanceWarning(lowBalance)
                .build();
    }

    public static ConsumeResult insufficient(BigDecimal remaining) {
        return ConsumeResult.builder()
                .success(false)
                .remaining(remaining)
                .consumedAmount(BigDecimal.ZERO)
                .failReason("课时余额不足")
                .build();
    }

    public static ConsumeResult accountNotFound() {
        return ConsumeResult.builder()
                .success(false)
                .failReason("课时账户不存在，请先购买课程")
                .build();
    }
}
