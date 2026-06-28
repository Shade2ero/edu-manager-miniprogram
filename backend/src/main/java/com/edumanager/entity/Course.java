package com.edumanager.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 课程定义
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("course")
public class Course {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属机构ID */
    private Long institutionId;

    /** 课程名称 */
    private String name;

    /** 封面图 */
    private String coverUrl;

    /** 课程介绍 */
    private String description;

    /** 分类标签 */
    private String category;

    /** 默认每节课时长（分钟） */
    private Integer defaultDuration;

    /** 状态：0-下架 1-上架 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
