package com.edumanager.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 课时流水展示对象（返回前端用）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionVO {

    private Long id;
    /** 变动类型：PURCHASE / CONSUME / REFUND / ADJUST */
    private String changeType;
    /** 变动课时数（正=增加，负=减少） */
    private BigDecimal changeAmount;
    /** 变动前余额 */
    private BigDecimal balanceBefore;
    /** 变动后余额 */
    private BigDecimal balanceAfter;
    /** 业务描述（如"签到自动扣除 — 钢琴启蒙课"） */
    private String bizDescription;
    /** 课程名称 */
    private String courseName;
    /** 备注 */
    private String remark;
    /** 创建时间 */
    private String createdAt;
}
