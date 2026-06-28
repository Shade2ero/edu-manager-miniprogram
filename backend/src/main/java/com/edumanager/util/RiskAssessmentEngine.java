package com.edumanager.util;

import com.edumanager.dto.CheckInResult;
import com.edumanager.entity.ClassSchedule;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 签到风控评估器
 *
 * <p><b>设计理念：打分制而非二元判定</b></p>
 * <p>不同维度的风险因素累积评分，根据总分决定风控等级，
 * 避免因单一因素（如 GPS 漂移）直接拒绝合法签到。</p>
 *
 * <p><b>风险维度与分值：</b></p>
 * <table>
 *   <tr><th>维度</th><th>条件</th><th>分值</th></tr>
 *   <tr><td>签到时间窗口</td><td>超出 ±30min 窗口</td><td>+20</td></tr>
 *   <tr><td>LBS 围栏</td><td>距离 &gt; 围栏半径 + 容错阈值</td><td>+30</td></tr>
 *   <tr><td>LBS 围栏</td><td>距离 &gt; 围栏半径 × 3（严重越界）</td><td>+60</td></tr>
 *   <tr><td>设备指纹</td><td>疑似模拟器/虚拟环境</td><td>+40</td></tr>
 *   <tr><td>设备指纹</td><td>短时间多账号签到</td><td>+30</td></tr>
 *   <tr><td>设备指纹</td><td>学员频繁换设备</td><td>+15</td></tr>
 * </table>
 *
 * <p><b>风控等级映射：</b></p>
 * <table>
 *   <tr><th>总分</th><th>等级</th><th>处理策略</th></tr>
 *   <tr><td>0-30</td><td>SAFE</td><td>正常签到</td></tr>
 *   <tr><td>31-60</td><td>SUSPICIOUS</td><td>正常签到 + 后台标记人工抽查</td></tr>
 *   <tr><td>61-80</td><td>WARNING</td><td>签到成功 + 教师端推送预警通知</td></tr>
 *   <tr><td>81-100</td><td>HIGH_RISK</td><td>需教师二次确认后方可签到</td></tr>
 * </table>
 */
public final class RiskAssessmentEngine {

    // ==================== 风险分值常量 ====================

    /** LBS 距离超过围栏半径 + 容错阈值 */
    private static final int SCORE_LBS_OUT_OF_RANGE = 30;
    /** LBS 距离超过围栏半径 × 3（严重越界，直接高危） */
    private static final int SCORE_LBS_FAR_OUT = 60;
    /** 签到时间超出 ±30min 窗口 */
    private static final int SCORE_TIME_OUT_OF_WINDOW = 20;
    /** 疑似模拟器/虚拟机 */
    private static final int SCORE_EMULATOR_SUSPECTED = 40;
    /** 同一设备短时间内多账号签到 */
    private static final int SCORE_MULTI_ACCOUNT_SAME_DEVICE = 30;
    /** 学员近期频繁更换设备 */
    private static final int SCORE_FREQUENT_DEVICE_CHANGE = 15;

    /** 签到有效时间窗口（分钟） */
    private static final int CHECK_IN_WINDOW_MINUTES = 30;

    /** LBS 容错距离（米），GPS 漂移容差 */
    private static final double LBS_TOLERANCE_METERS = 30.0;

    private RiskAssessmentEngine() {}

    /**
     * 执行完整的风控评估。
     *
     * @param requestContext 签到请求上下文
     * @return 评估结果
     */
    public static RiskAssessmentResult assess(RiskAssessmentContext requestContext) {
        int totalScore = 0;
        List<String> riskDetails = new ArrayList<>();

        // ===== 维度1：签到时间窗口校验 =====
        ClassSchedule schedule = requestContext.getSchedule();
        LocalDateTime now = requestContext.getCheckInTime();
        LocalDateTime windowStart = LocalDateTime.of(
                schedule.getScheduleDate(), schedule.getStartTime()).minusMinutes(CHECK_IN_WINDOW_MINUTES);
        LocalDateTime windowEnd = LocalDateTime.of(
                schedule.getScheduleDate(), schedule.getEndTime()).plusMinutes(CHECK_IN_WINDOW_MINUTES);

        if (now.isBefore(windowStart) || now.isAfter(windowEnd)) {
            totalScore += SCORE_TIME_OUT_OF_WINDOW;
            riskDetails.add(String.format(
                    "签到时间(%s)超出课程时间窗口[%s ~ %s]",
                    now, windowStart, windowEnd));
        }

        // ===== 维度2：LBS 围栏校验 =====
        BigDecimal distance = requestContext.getDistanceFromCenter();
        double fenceRadius = requestContext.getLbsRadius() != null
                ? requestContext.getLbsRadius().doubleValue() : 300.0;

        if (distance != null) {
            double dist = distance.doubleValue();
            if (dist > fenceRadius * 3) {
                // 严重越界（超过围栏半径3倍）：直接加高分
                totalScore += SCORE_LBS_FAR_OUT;
                riskDetails.add(String.format(
                        "签到位置距中心%.1f米，严重超出围栏范围(%.0f米×3)",
                        dist, fenceRadius));
            } else if (dist > fenceRadius + LBS_TOLERANCE_METERS) {
                // 略超围栏（可能在隔壁教室/楼下）
                totalScore += SCORE_LBS_OUT_OF_RANGE;
                riskDetails.add(String.format(
                        "签到位置距中心%.1f米，超出围栏范围(%.0f米+%.0f米容错)",
                        dist, fenceRadius, LBS_TOLERANCE_METERS));
            }
        }

        // ===== 维度3：设备指纹风控 =====
        if (requestContext.isEmulatorSuspected()) {
            totalScore += SCORE_EMULATOR_SUSPECTED;
            riskDetails.add("疑似模拟器或虚拟环境签到");
        }
        if (requestContext.isMultiAccountSameDevice()) {
            totalScore += SCORE_MULTI_ACCOUNT_SAME_DEVICE;
            riskDetails.add("该设备短时间内存在多账号签到");
        }
        if (requestContext.isFrequentDeviceChange()) {
            totalScore += SCORE_FREQUENT_DEVICE_CHANGE;
            riskDetails.add("该学员近期频繁更换签到设备");
        }

        // ===== 汇总结果 =====
        totalScore = Math.min(totalScore, 100); // 上限 100

        CheckInResult.RiskLevel level;
        if (totalScore <= 30) {
            level = CheckInResult.RiskLevel.SAFE;
        } else if (totalScore <= 60) {
            level = CheckInResult.RiskLevel.SUSPICIOUS;
        } else if (totalScore <= 80) {
            level = CheckInResult.RiskLevel.WARNING;
        } else {
            level = CheckInResult.RiskLevel.HIGH_RISK;
        }

        return RiskAssessmentResult.builder()
                .riskScore(totalScore)
                .riskLevel(level)
                .riskDetails(riskDetails)
                .distanceFromCenter(distance)
                .build();
    }

    // ==================== 上下文与结果 Bean ====================

    /**
     * 风控评估输入上下文
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RiskAssessmentContext {
        /** 当前签到时间 */
        private LocalDateTime checkInTime;
        /** 排课信息 */
        private ClassSchedule schedule;
        /** 距机构中心的距离（米），调用方先算好 */
        private BigDecimal distanceFromCenter;
        /** 机构围栏半径（米） */
        private Integer lbsRadius;
        /** 是否疑似模拟器 */
        private boolean emulatorSuspected;
        /** 同设备短时间多账号签到 */
        private boolean multiAccountSameDevice;
        /** 学员频繁换设备 */
        private boolean frequentDeviceChange;
    }

    /**
     * 风控评估结果
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RiskAssessmentResult {
        /** 风险总分（0-100） */
        private Integer riskScore;
        /** 风险等级 */
        private CheckInResult.RiskLevel riskLevel;
        /** 风险明细列表 */
        private List<String> riskDetails;
        /** 距中心点距离（米） */
        private BigDecimal distanceFromCenter;

        public String getRiskDescription() {
            return riskDetails == null || riskDetails.isEmpty()
                    ? "无异常"
                    : String.join("；", riskDetails);
        }
    }
}
