
package com.pet.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("vaccine_record")
public class VaccineRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long petId;

    private String vaccineName;

    private String vaccineType;

    private String dose;

    private LocalDate vaccinateDate;

    private LocalDate nextDate;

    private String hospital;

    private String doctor;

    private String notes;

    private String sourceName;

    private String sourceUrl;

    private String sourceNote;

    private LocalDateTime createdAt;

    @TableField(exist = false)
    private String petName;
}
