package com.edumanager.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.edumanager.entity.BalanceTransaction;
import org.apache.ibatis.annotations.Mapper;

/**
 * 课时流水 Mapper — 只追加，不修改
 */
@Mapper
public interface BalanceTransactionMapper extends BaseMapper<BalanceTransaction> {
}
