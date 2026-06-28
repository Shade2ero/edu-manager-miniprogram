package com.edumanager.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 签到结果 DTO — 综合返回签到状态、风控结果、课消结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckInResult {

    // ==================== 签到状态 ====================

    /** 签到是否成功 */
    private Boolean checkInSuccess;

    /** 签到时间 */
    private LocalDateTime checkInTime;

    /** 签到方式（QR_SCAN / LBS / TEACHER_MANUAL） */
    private String checkInMethod;

    // ==================== 风控信息 ====================

    /** 风控评分（0-100，分数越高风险越大） */
    private Integer riskScore;

    /** 风控结果等级 */
    private RiskLevel riskLevel;

    /** 风控明细说明 */
    private String riskDescription;

    /** 距签到中心点距离（米） */
    private BigDecimal distanceFromCenter;

    // ==================== 课消信息 ====================

    /** 是否触发课消 */
    private Boolean consumptionTriggered;

    /** 课消是否成功 */
    private Boolean consumptionSuccess;

    /** 扣减课时数 */
    private BigDecimal consumedHours;

    /** 课程剩余课时（课消失败时为 null） */
    private BigDecimal remainingHours;

    /** 课消失败原因（如有） */
    private String consumptionFailReason;

    /** 是否触发余额不足预警 */
    private Boolean lowBalanceWarning;

    // ==================== 提示信息 ====================

    /** 前端展示的提示信息 */
    private String displayMessage;

    // ==================== 风控等级枚举 ====================

    public enum RiskLevel {
        /** 安全：正常签到，无异常 */
        SAFE,
        /** 可疑：正常签到，后台标记，人工抽查 */
        SUSPICIOUS,
        /** 警告：签到成功，教师端推送预警通知 */
        WARNING,
        /** 高危：需教师二次确认后方可生效 */
        HIGH_RISK
    }

    // ==================== 静态工厂方法 ====================

    /**
     * 构建正常签到结果
     */
    public static CheckInResult success(LocalDateTime checkInTime, String method,
                                        Integer riskScore, RiskLevel riskLevel,
                                        BigDecimal distance, ConsumeResult consumeResult) {
        CheckInResultBuilder builder = CheckInResult.builder()
                .checkInSuccess(true)
                .checkInTime(checkInTime)
                .checkInMethod(method)
                .riskScore(riskScore)
                .riskLevel(riskLevel)
                .distanceFromCenter(distance)
                .consumptionTriggered(true);

        if (consumeResult != null && consumeResult.getSuccess()) {
            builder.consumptionSuccess(true)
                    .consumedHours(consumeResult.getConsumedAmount())
                    .remainingHours(consumeResult.getRemaining())
                    .lowBalanceWarning(consumeResult.getLowBalanceWarning())
                    .displayMessage(buildSuccessMessage(consumeResult));
        } else if (consumeResult != null) {
            builder.consumptionSuccess(false)
                    .consumptionFailReason(consumeResult.getFailReason())
                    .displayMessage("签到成功，但课时扣减失败：" + consumeResult.getFailReason());
        }

        return builder.build();
    }

    /**
     * 构建高危签到结果（需教师二次确认）
     */
    public static CheckInResult highRisk(LocalDateTime checkInTime, String method,
                                          Integer riskScore, String riskDescription,
                                          BigDecimal distance) {
        return CheckInResult.builder()
                .checkInSuccess(false)   // 标记为未成功，等待教师确认
                .checkInTime(checkInTime)
                .checkInMethod(method)
                .riskScore(riskScore)
                .riskLevel(RiskLevel.HIGH_RISK)
                .riskDescription(riskDescription)
                .distanceFromCenter(distance)
                .consumptionTriggered(false)
                .displayMessage("签到存在异常，请等待教师确认")
                .build();
    }

    /**
     * 构建失败结果
     */
    public static CheckInResult fail(String message) {
        return CheckInResult.builder()
                .checkInSuccess(false)
                .displayMessage(message)
                .build();
    }

    private static String buildSuccessMessage(ConsumeResult cr) {
        StringBuilder sb = new StringBuilder("签到成功！");
        if (cr.getSuccess()) {
            sb.append("本次消耗 ").append(cr.getConsumedAmount()).append(" 课时");
            sb.append("，剩余 ").append(cr.getRemaining()).append(" 课时");
            if (cr.getLowBalanceWarning()) {
                sb.append("（课时即将用完，请及时续费）");
            }
        }
        return sb.toString();
    }
}
