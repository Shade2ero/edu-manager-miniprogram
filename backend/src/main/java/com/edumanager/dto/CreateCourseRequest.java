package com.edumanager.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 管理员创建课程请求
 */
@Data
public class CreateCourseRequest {

    @NotBlank(message = "课程名称不能为空")
    private String name;

    private String description;
    private String category;
    private Integer defaultDuration = 60;

    /** 课时包列表（可选，创建课程时同时创建课时包） */
    private List<PackageItem> packages;

    @Data
    public static class PackageItem {
        @NotBlank private String name;
        @NotNull private Integer totalHours;
        @NotNull private Integer price;
        private Integer originalPrice;
        private Integer validDays = 365;
    }
}
