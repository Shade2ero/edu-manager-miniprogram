package com.edumanager.exception;

import lombok.Getter;

/**
 * 业务异常 — 统一的业务层异常类
 * <p>
 * 包含错误码和消息，由全局异常处理器捕获后转换为统一响应格式返回前端。
 * </p>
 */
@Getter
public class BusinessException extends RuntimeException {

    /** 业务错误码 */
    private final int code;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(String message) {
        super(message);
        this.code = 500;
    }

    // ==================== 预定义错误码 ====================

    /** 课时余额不足 */
    public static BusinessException insufficientBalance(Long studentId, Long courseId) {
        return new BusinessException(10001,
                String.format("学员(%d)课程(%d)课时余额不足，无法扣减", studentId, courseId));
    }

    /** 课时账户不存在 */
    public static BusinessException balanceNotFound(Long studentId, Long courseId) {
        return new BusinessException(10002,
                String.format("学员(%d)课程(%d)课时账户不存在，请先购买课程", studentId, courseId));
    }

    /** 乐观锁并发冲突（提示重试） */
    public static BusinessException concurrentConflict() {
        return new BusinessException(10003, "系统繁忙，请稍后重试");
    }

    /** 扣减金额非法 */
    public static BusinessException illegalAmount() {
        return new BusinessException(10004, "扣减课时数必须大于0");
    }
}
