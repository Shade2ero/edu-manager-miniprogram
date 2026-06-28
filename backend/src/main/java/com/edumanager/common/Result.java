package com.edumanager.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一响应体 — 所有 API 返回此结构
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {

    /** 状态码（0=成功，非0=失败） */
    private int code;

    /** 提示消息 */
    private String message;

    /** 响应数据 */
    private T data;

    // ==================== 静态工厂方法 ====================

    public static <T> Result<T> ok(T data) {
        return new Result<>(0, "ok", data);
    }

    public static <T> Result<T> ok() {
        return new Result<>(0, "ok", null);
    }

    public static <T> Result<T> fail(int code, String message) {
        return new Result<>(code, message, null);
    }

    public static <T> Result<T> fail(String message) {
        return new Result<>(500, message, null);
    }
}
