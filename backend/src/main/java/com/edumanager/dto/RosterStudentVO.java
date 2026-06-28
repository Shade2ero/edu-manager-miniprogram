package com.edumanager.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 点名册学员展示 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RosterStudentVO {

    private Long classStudentId;
    private Long studentId;
    private String studentName;
    private String avatarUrl;
    private String status;          // PENDING/CHECKED_IN/ABSENT/LEAVE
    private LocalDateTime checkInTime;
    private String checkInMethod;
    private BigDecimal remainingHours;  // 该课程剩余课时
    private Integer riskFlag;
    private String riskReason;
}
