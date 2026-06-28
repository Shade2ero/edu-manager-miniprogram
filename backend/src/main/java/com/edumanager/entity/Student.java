package com.edumanager.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("student")
public class Student {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long institutionId;
    private String openid;
    private String unionid;
    private String realName;
    private String gender;
    private String nickname;
    private String avatarUrl;
    private String phone;
    private String parentPhone;

    /** STUDENT（默认） */
    private String role;

    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
