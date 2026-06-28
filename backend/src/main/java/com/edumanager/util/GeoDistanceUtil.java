package com.edumanager.util;

import java.math.BigDecimal;

/**
 * 地理距离计算工具类
 *
 * <p>使用 Haversine（半正矢）公式计算两个经纬度坐标之间的球面距离。</p>
 * <p>适用于短距离（< 100km）的签到围栏判断场景，精度足够且计算高效。</p>
 */
public final class GeoDistanceUtil {

    /** 地球平均半径（米） */
    private static final double EARTH_RADIUS_METERS = 6_371_000.0;

    private GeoDistanceUtil() {}

    /**
     * 计算两个坐标点之间的球面距离（单位：米）
     *
     * <p><b>Haversine 公式</b></p>
     * <pre>
     * a = sin²(Δlat/2) + cos(lat1)·cos(lat2)·sin²(Δlng/2)
     * c = 2 · atan2(√a, √(1−a))
     * d = R · c
     * </pre>
     *
     * @param lat1 点1 纬度（十进制）
     * @param lng1 点1 经度（十进制）
     * @param lat2 点2 纬度（十进制）
     * @param lng2 点2 经度（十进制）
     * @return 两点间的距离（米），保留 2 位小数
     */
    public static BigDecimal calculate(double lat1, double lng1,
                                        double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = EARTH_RADIUS_METERS * c;

        return BigDecimal.valueOf(distance).setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    /**
     * 计算两个坐标点之间的距离（BigDecimal 版本）
     */
    public static BigDecimal calculate(BigDecimal lat1, BigDecimal lng1,
                                        BigDecimal lat2, BigDecimal lng2) {
        if (lat1 == null || lng1 == null || lat2 == null || lng2 == null) {
            return BigDecimal.ZERO;
        }
        return calculate(lat1.doubleValue(), lng1.doubleValue(),
                lat2.doubleValue(), lng2.doubleValue());
    }
}
