package com.edumanager.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 学员课时账户 — 核心表
 * <p>
 * 每种课程维护一条记录，记录该学员该课程的剩余课时。
 * 通过乐观锁 version 字段保证并发扣减安全。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("student_balance")
public class StudentBalance {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属机构ID */
    private Long institutionId;

    /** 学员ID */
    private Long studentId;

    /** 课程ID */
    private Long courseId;

    /** 累计购买课时数 */
    private BigDecimal totalPurchased;

    /** 累计已消耗课时数 */
    private BigDecimal totalConsumed;

    /**
     * 剩余课时数（核心字段）
     * <p>计算公式：remaining = totalPurchased - totalConsumed</p>
     */
    private BigDecimal remaining;

    /** 最早到期日（多笔购买取最早未过期的） */
    private LocalDate expiresAt;

    /**
     * 乐观锁版本号
     * <p>每次扣减 +1，UPDATE 时 WHERE version = 期望值，防止并发超扣</p>
     */
    @Version
    private Integer version;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
