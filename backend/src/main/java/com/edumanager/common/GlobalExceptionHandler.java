package com.edumanager.common;

import com.edumanager.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 * <p>捕获所有未处理异常，统一封装为 Result 返回前端</p>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务异常 — 返回业务错误码和消息 */
    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusinessException(BusinessException e) {
        log.warn("业务异常 code={}, message={}", e.getCode(), e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    /** 未知异常 — 记录完整堆栈，返回通用错误（开发阶段透出异常详情） */
    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        log.error("系统异常", e);
        // 开发阶段把真实异常信息返回方便调试
        String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
        Throwable cause = e.getCause();
        while (cause != null) {
            msg += " | Caused by: " + (cause.getMessage() != null ? cause.getMessage() : cause.getClass().getSimpleName());
            cause = cause.getCause();
        }
        return Result.fail(msg);
    }
}
