package com.edumanager.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 数据看板统计 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardVO {

    // ========== 概览数据 ==========
    /** 总学员数 */
    private int totalStudents;
    /** 本月新增学员 */
    private int newStudentsThisMonth;
    /** 活跃课程数 */
    private int activeCourses;

    // ========== 考勤统计 ==========
    /** 本月总排课数 */
    private int totalSchedules;
    /** 本月总应签到人次 */
    private int totalShouldCheckIn;
    /** 本月实际签到人次 */
    private int totalCheckedIn;
    /** 出勤率（百分比，如 94.5） */
    private BigDecimal attendanceRate;
    /** 请假人次 */
    private int leaveCount;
    /** 缺勤人次 */
    private int absentCount;

    // ========== 课消统计 ==========
    /** 本月总消耗课时 */
    private BigDecimal totalConsumedHours;
    /** 本月总购买课时 */
    private BigDecimal totalPurchasedHours;
    /** 课消率（消耗/购买） */
    private BigDecimal consumptionRate;
    /** 本月课消金额（按课时单价估算） */
    private BigDecimal estimatedRevenue;

    // ========== 预警数据 ==========
    /** 课时不足学员数（剩余 <= 2） */
    private int lowBalanceStudentCount;
    /** 即将到期学员数（30天内） */
    private int expiringSoonStudentCount;

    // ========== 趋势数据（近7天） ==========
    /** 近7天每日签到人次 [3,5,8,12,10,7,9] */
    private int[] dailyCheckInCounts;
    /** 近7天每日课消课时 [3,5,8,12,10,7,9] */
    private BigDecimal[] dailyConsumedHours;
    /** 近7天日期标签 ["06-21","06-22",...] */
    private String[] dailyLabels;
}
