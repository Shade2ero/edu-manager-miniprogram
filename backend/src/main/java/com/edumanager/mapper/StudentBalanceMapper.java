package com.edumanager.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.edumanager.entity.StudentBalance;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

/**
 * 学员课时账户 Mapper
 * <p>
 * 核心 SQL：CAS（Compare And Swap）乐观锁扣减，彻底杜绝超扣问题。
 * </p>
 */
@Mapper
public interface StudentBalanceMapper extends BaseMapper<StudentBalance> {

    /**
     * 【核心方法】使用乐观锁 + 余额校验 安全扣减课时。
     * <p>
     * SQL 级联三个防御条件：
     * <ol>
     *   <li>remaining >= amount — 余额充足才扣（业务防线）</li>
     *   <li>version = expectedVersion — 乐观锁，防止并发覆盖（并发防线）</li>
     *   <li>id = balanceId — 精确锁定一行</li>
     * </ol>
     * 返回 affected rows = 0 时，调用方需判断是余额不足还是并发冲突，然后重试或抛异常。
     * </p>
     *
     * @param balanceId       账户ID
     * @param amount          扣减课时数（正数）
     * @param expectedVersion 期望的版本号
     * @return 受影响行数（1=成功, 0=失败）
     */
    @Update("UPDATE student_balance " +
            "SET remaining = remaining - #{amount}, " +
            "    total_consumed = total_consumed + #{amount}, " +
            "    version = version + 1, " +
            "    updated_at = NOW() " +
            "WHERE id = #{balanceId} " +
            "  AND remaining >= #{amount} " +
            "  AND version = #{expectedVersion}")
    int consumeWithVersion(@Param("balanceId") Long balanceId,
                           @Param("amount") BigDecimal amount,
                           @Param("expectedVersion") Integer expectedVersion);

    /**
     * 增加课时（购买或人工调整时调用）。
     * <p>使用乐观锁保证与扣减操作不冲突。</p>
     *
     * @param balanceId       账户ID
     * @param amount          增加课时数（正数）
     * @param expectedVersion 期望的版本号
     * @return 受影响行数
     */
    @Update("UPDATE student_balance " +
            "SET remaining = remaining + #{amount}, " +
            "    total_purchased = total_purchased + #{amount}, " +
            "    version = version + 1, " +
            "    updated_at = NOW() " +
            "WHERE id = #{balanceId} " +
            "  AND version = #{expectedVersion}")
    int addWithVersion(@Param("balanceId") Long balanceId,
                       @Param("amount") BigDecimal amount,
                       @Param("expectedVersion") Integer expectedVersion);
}
