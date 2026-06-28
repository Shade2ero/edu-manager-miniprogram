package com.edumanager.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 课时流水 — 记录每一笔课时变动（充值/消费/退费/补偿）
 * <p>只追加写入，不修改不删除，保证审计完整性</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("balance_transaction")
public class BalanceTransaction {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属机构ID */
    private Long institutionId;

    /** 学员ID */
    private Long studentId;

    /** 课程ID */
    private Long courseId;

    /** 关联 student_balance.id */
    private Long balanceId;

    /**
     * 变动类型
     * <p>PURCHASE - 购买充值 / CONSUME - 签到消耗 / REFUND - 退费退还 / ADJUST - 人工调整</p>
     */
    private String changeType;

    /** 变动课时数（正=增加，负=减少） */
    private BigDecimal changeAmount;

    /** 变动前余额 */
    private BigDecimal balanceBefore;

    /** 变动后余额 */
    private BigDecimal balanceAfter;

    /**
     * 业务类型
     * <p>ATTENDANCE - 签到扣课 / ORDER - 购买充值 / MANUAL - 人工操作 / REFUND - 退费</p>
     */
    private String bizType;

    /** 关联业务ID（出勤记录ID / 订单ID） */
    private Long bizId;

    /** 备注 */
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
