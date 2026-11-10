
package com.pet.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("pet")
public class Pet {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String type;

    private String breed;

    private Integer age;

    private Integer gender;

    private BigDecimal weight;

    private String color;

    private String description;

    private String image;

    private Integer status;

    private String healthStatus;

    private BigDecimal adoptFee;

    private String shelter;

    private String sourceName;

    private String sourceUrl;

    private LocalDate sourceCheckedAt;

    private String imageSourceName;

    private String imageSourceUrl;

    private String sourceNote;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
