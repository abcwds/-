
package com.pet.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("health_record")
public class HealthRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long petId;

    private LocalDate recordDate;

    private BigDecimal weight;

    private BigDecimal temperature;

    private String symptom;

    private String diagnosis;

    private String treatment;

    private String veterinarian;

    private String hospital;

    private BigDecimal cost;

    private String notes;

    private String sourceName;

    private String sourceUrl;

    private String sourceNote;

    private LocalDateTime createdAt;

    @TableField(exist = false)
    private String petName;
}
