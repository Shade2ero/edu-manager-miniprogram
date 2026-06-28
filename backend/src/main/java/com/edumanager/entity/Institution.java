package com.edumanager.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 机构/租户
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("institution")
public class Institution {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 机构名称 */
    private String name;

    /** Logo 地址 */
    private String logoUrl;

    /** 联系电话 */
    private String contactPhone;

    /** 机构地址 */
    private String address;

    /** 签到中心点纬度 */
    private BigDecimal lbsLatitude;

    /** 签到中心点经度 */
    private BigDecimal lbsLongitude;

    /** 签到围栏半径（米） */
    private Integer lbsRadius;

    /** 状态：0-停用 1-正常 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
