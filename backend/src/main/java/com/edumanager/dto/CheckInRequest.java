package com.edumanager.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 学生签到请求 DTO
 *
 * <p>由小程序前端在扫码或 LBS 定位后提交。</p>
 */
@Data
public class CheckInRequest {

    // ==================== 必填：业务标识 ====================

    /** 排课ID（标识哪一节课） */
    @NotNull(message = "排课ID不能为空")
    private Long scheduleId;

    /** 学员ID */
    @NotNull(message = "学员ID不能为空")
    private Long studentId;

    // ==================== 必填：签到凭据 ====================

    /**
     * 二维码 Token（扫码签到时必传）
     * <p>教师端生成的动态二维码包含此 token，有时效性</p>
     */
    private String qrToken;

    // ==================== 必填：位置信息（LBS 围栏校验用） ====================

    /** 签到位置纬度 */
    @NotNull(message = "签到纬度不能为空")
    private BigDecimal latitude;

    /** 签到位置经度 */
    @NotNull(message = "签到经度不能为空")
    private BigDecimal longitude;

    // ==================== 必填：设备信息（防作弊用） ====================

    /** 设备品牌（如 "Apple", "HUAWEI"） */
    @NotBlank(message = "设备品牌不能为空")
    private String deviceBrand;

    /** 设备型号（如 "iPhone 15 Pro", "Mate 60 Pro"） */
    @NotBlank(message = "设备型号不能为空")
    private String deviceModel;

    /** 操作系统版本（如 "iOS 17.4", "Android 14"） */
    @NotBlank(message = "系统版本不能为空")
    private String systemVersion;

    /** 微信版本号（如 "8.0.48"） */
    @NotBlank(message = "微信版本不能为空")
    private String wxVersion;

    /** 设备唯一标识（由小程序端通过 wx.getDeviceInfo 等接口组合生成） */
    private String deviceFingerprint;

    // ==================== 可选：签到备注 ====================

    /** 备注（如"迟到5分钟"） */
    private String remark;
}
