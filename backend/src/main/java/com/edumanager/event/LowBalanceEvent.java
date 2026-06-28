package com.edumanager.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 余额不足事件 — 扣减后剩余课时 <= 阈值时发布
 * <p>监听器负责发送微信订阅消息通知家长续费</p>
 */
@Getter
public class LowBalanceEvent extends ApplicationEvent {

    private final Long studentId;
    private final Long courseId;
    private final Long institutionId;
    private final java.math.BigDecimal remaining;

    public LowBalanceEvent(Object source, Long studentId, Long courseId,
                           Long institutionId, java.math.BigDecimal remaining) {
        super(source);
        this.studentId = studentId;
        this.courseId = courseId;
        this.institutionId = institutionId;
        this.remaining = remaining;
    }
}
