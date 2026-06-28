package com.edumanager.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 配置
 * <p>
 * 包括：
 * <ul>
 *   <li>乐观锁插件（虽然核心扣减用手写 CAS，但其他表仍可用 @Version）</li>
 *   <li>分页插件</li>
 *   <li>自动填充（created_at, updated_at）</li>
 * </ul>
 * </p>
 */
@Configuration
public class MybatisPlusConfig {

    /**
     * MyBatis-Plus 拦截器链
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 乐观锁拦截器 — 处理 @Version 注解字段
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());

        // 分页拦截器
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));

        return interceptor;
    }

    /**
     * 自动填充处理器 — 在 INSERT/UPDATE 时自动设置时间戳
     */
    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new MetaObjectHandler() {
            @Override
            public void insertFill(MetaObject metaObject) {
                this.strictInsertFill(metaObject, "createdAt", LocalDateTime.class, LocalDateTime.now());
                this.strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
            }

            @Override
            public void updateFill(MetaObject metaObject) {
                this.strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
            }
        };
    }
}
