package com.edumanager.service.impl;

import cn.hutool.core.date.LocalDateTimeUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edumanager.dto.DashboardVO;
import com.edumanager.entity.*;
import com.edumanager.mapper.*;
import com.edumanager.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 数据看板服务 — 聚合统计查询
 *
 * <p>生产环境建议使用定时任务预计算 + Redis 缓存，
 * 避免每次请求都做全表聚合。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final AttendanceLogMapper attendanceLogMapper;
    private final BalanceTransactionMapper transactionMapper;
    private final ClassScheduleMapper scheduleMapper;
    private final ClassStudentMapper classStudentMapper;
    private final StudentBalanceMapper balanceMapper;

    @Override
    public DashboardVO getMonthlyDashboard(Long institutionId) {
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate monthEnd = today.withDayOfMonth(today.lengthOfMonth());
        LocalDateTime monthStartTime = monthStart.atStartOfDay();
        LocalDateTime monthEndTime = monthEnd.atTime(23, 59, 59);

        // ===== 考勤统计 =====
        // 本月排课
        Long totalSchedules = scheduleMapper.selectCount(
                new LambdaQueryWrapper<ClassSchedule>()
                        .eq(ClassSchedule::getInstitutionId, institutionId)
                        .between(ClassSchedule::getScheduleDate, monthStart, monthEnd)
        );

        // 本月签到人次
        Long checkedInCount = attendanceLogMapper.selectCount(
                new LambdaQueryWrapper<AttendanceLog>()
                        .eq(AttendanceLog::getInstitutionId, institutionId)
                        .between(AttendanceLog::getCheckInTime, monthStartTime, monthEndTime)
        );

        // 本月考勤记录（含缺席/请假）
        Long totalAttendance = classStudentMapper.selectCount(
                new LambdaQueryWrapper<ClassStudent>()
                        .inSql(ClassStudent::getScheduleId,
                                "SELECT id FROM class_schedule WHERE institution_id = " + institutionId
                                        + " AND schedule_date BETWEEN '" + monthStart + "' AND '" + monthEnd + "'")
        );

        Long absentCount = classStudentMapper.selectCount(
                new LambdaQueryWrapper<ClassStudent>()
                        .eq(ClassStudent::getStatus, "ABSENT")
                        .inSql(ClassStudent::getScheduleId,
                                "SELECT id FROM class_schedule WHERE institution_id = " + institutionId
                                        + " AND schedule_date BETWEEN '" + monthStart + "' AND '" + monthEnd + "'")
        );

        Long leaveCount = classStudentMapper.selectCount(
                new LambdaQueryWrapper<ClassStudent>()
                        .eq(ClassStudent::getStatus, "LEAVE")
                        .inSql(ClassStudent::getScheduleId,
                                "SELECT id FROM class_schedule WHERE institution_id = " + institutionId
                                        + " AND schedule_date BETWEEN '" + monthStart + "' AND '" + monthEnd + "'")
        );

        BigDecimal attendanceRate = BigDecimal.ZERO;
        if (totalAttendance > 0) {
            attendanceRate = BigDecimal.valueOf(checkedInCount)
                    .divide(BigDecimal.valueOf(totalAttendance), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(1, RoundingMode.HALF_UP);
        }

        // ===== 课消统计 =====
        // 本月消耗流水
        List<BalanceTransaction> monthlyTxns = transactionMapper.selectList(
                new LambdaQueryWrapper<BalanceTransaction>()
                        .eq(BalanceTransaction::getInstitutionId, institutionId)
                        .eq(BalanceTransaction::getChangeType, "CONSUME")
                        .between(BalanceTransaction::getCreatedAt, monthStartTime, monthEndTime)
        );
        BigDecimal totalConsumed = monthlyTxns.stream()
                .map(tx -> tx.getChangeAmount().abs())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 本月购买流水
        List<BalanceTransaction> purchaseTxns = transactionMapper.selectList(
                new LambdaQueryWrapper<BalanceTransaction>()
                        .eq(BalanceTransaction::getInstitutionId, institutionId)
                        .eq(BalanceTransaction::getChangeType, "PURCHASE")
                        .between(BalanceTransaction::getCreatedAt, monthStartTime, monthEndTime)
        );
        BigDecimal totalPurchased = purchaseTxns.stream()
                .map(BalanceTransaction::getChangeAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal consumptionRate = BigDecimal.ZERO;
        if (totalPurchased.compareTo(BigDecimal.ZERO) > 0) {
            consumptionRate = totalConsumed
                    .divide(totalPurchased, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(1, RoundingMode.HALF_UP);
        }

        // ===== 预警统计 =====
        Long lowBalanceCount = balanceMapper.selectCount(
                new LambdaQueryWrapper<StudentBalance>()
                        .le(StudentBalance::getRemaining, 2)
                        .gt(StudentBalance::getRemaining, 0)
        );
        Long expiringCount = balanceMapper.selectCount(
                new LambdaQueryWrapper<StudentBalance>()
                        .le(StudentBalance::getExpiresAt, today.plusDays(30))
                        .ge(StudentBalance::getExpiresAt, today)
        );

        // ===== 近7天趋势 =====
        int[] dailyCheckIn = new int[7];
        String[] dailyLabels = new String[7];
        BigDecimal[] dailyConsumedArr = new BigDecimal[7];
        for (int i = 6; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            dailyLabels[6 - i] = d.toString().substring(5); // "MM-dd"
            LocalDateTime dayStart = d.atStartOfDay();
            LocalDateTime dayEnd = d.atTime(23, 59, 59);

            Long dayCheckIn = attendanceLogMapper.selectCount(
                    new LambdaQueryWrapper<AttendanceLog>()
                            .eq(AttendanceLog::getInstitutionId, institutionId)
                            .between(AttendanceLog::getCheckInTime, dayStart, dayEnd)
            );
            dailyCheckIn[6 - i] = dayCheckIn.intValue();

            List<BalanceTransaction> dayTxns = transactionMapper.selectList(
                    new LambdaQueryWrapper<BalanceTransaction>()
                            .eq(BalanceTransaction::getInstitutionId, institutionId)
                            .eq(BalanceTransaction::getChangeType, "CONSUME")
                            .between(BalanceTransaction::getCreatedAt, dayStart, dayEnd)
            );
            dailyConsumedArr[6 - i] = dayTxns.stream()
                    .map(tx -> tx.getChangeAmount().abs())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        return DashboardVO.builder()
                .totalStudents(0) // TODO: 关联 student 表
                .newStudentsThisMonth(0)
                .activeCourses(0)
                .totalSchedules(totalSchedules.intValue())
                .totalShouldCheckIn(totalAttendance.intValue())
                .totalCheckedIn(checkedInCount.intValue())
                .attendanceRate(attendanceRate)
                .leaveCount(leaveCount.intValue())
                .absentCount(absentCount.intValue())
                .totalConsumedHours(totalConsumed)
                .totalPurchasedHours(totalPurchased)
                .consumptionRate(consumptionRate)
                .estimatedRevenue(BigDecimal.ZERO)
                .lowBalanceStudentCount(lowBalanceCount.intValue())
                .expiringSoonStudentCount(expiringCount.intValue())
                .dailyCheckInCounts(dailyCheckIn)
                .dailyConsumedHours(dailyConsumedArr)
                .dailyLabels(dailyLabels)
                .build();
    }
}
