package com.edumanager.util;

import cn.hutool.crypto.digest.DigestUtil;

/**
 * 设备指纹工具类
 *
 * <p>根据前端传来的设备信息生成指纹，用于识别同一设备的多账号签到等异常行为。</p>
 *
 * <p><b>注意：</b>小程序环境下的设备指纹精度有限，
 * 此工具仅作为风控辅助手段，不应作为唯一的防作弊依据。</p>
 */
public final class DeviceFingerprintUtil {

    private DeviceFingerprintUtil() {}

    /**
     * 根据设备属性组合生成指纹哈希。
     * <p>组合 brand + model + systemVersion + wxVersion，
     * 同一台手机这些信息基本不变。</p>
     */
    public static String generate(String brand, String model,
                                   String systemVersion, String wxVersion) {
        String raw = String.format("%s|%s|%s|%s",
                nullToEmpty(brand),
                nullToEmpty(model),
                nullToEmpty(systemVersion),
                nullToEmpty(wxVersion));
        return DigestUtil.md5Hex(raw);
    }

    /**
     * 检测是否可能是模拟器/虚拟机环境。
     * <p>简单规则：模拟器的设备信息通常包含特定关键字。</p>
     *
     * @return true 表示疑似模拟器
     */
    public static boolean isSuspectedEmulator(String brand, String model,
                                               String systemVersion) {
        String combined = (brand + model + systemVersion).toLowerCase();

        // 常见的模拟器/云手机标识
        String[] emulatorKeywords = {
                "emulator", "simulator", "virtual", "vbox", "vmware",
                "android sdk built for x86", "generic", "google_sdk",
                "bluestacks", "nox", "mumu", "ldplayer", "雷电", "逍遥",
                "tencent vir", "cloudphone", "云手机"
        };

        for (String keyword : emulatorKeywords) {
            if (combined.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
