package com.edumanager.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 课程课时余额展示对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseBalanceVO {

    private Long courseId;
    private String courseName;
    private String coverUrl;
    private String category;
    /** 剩余课时 */
    private BigDecimal remaining;
    /** 累计购买 */
    private BigDecimal totalPurchased;
    /** 累计消耗 */
    private BigDecimal totalConsumed;
    /** 到期日 */
    private String expiresAt;
    /** 距到期天数 */
    private Integer daysUntilExpiry;
}
