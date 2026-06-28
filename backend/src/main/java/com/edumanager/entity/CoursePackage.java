package com.edumanager.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 课时包（SKU）
 * <p>售卖的最小单元，一个课程可以有多个课时包（如16课时包、48课时包）</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("course_package")
public class CoursePackage {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属机构ID */
    private Long institutionId;

    /** 关联课程ID */
    private Long courseId;

    /** 课时包名称（如"春季16课时包"） */
    private String name;

    /** 包含课时数 */
    private Integer totalHours;

    /** 售价（分） */
    private Integer price;

    /** 原价（分） */
    private Integer originalPrice;

    /** 购买后有效期（天） */
    private Integer validDays;

    /** 排序权重 */
    private Integer sortOrder;

    /** 状态：0-下架 1-上架 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
