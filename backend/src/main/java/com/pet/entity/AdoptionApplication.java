
package com.pet.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("adoption_application")
public class AdoptionApplication {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long petId;

    private String realName;

    private String phone;

    private String address;

    private String workStatus;

    private String housingType;

    private Integer hasOtherPet;

    private String experience;

    private String reason;

    private Integer status;

    private String rejectReason;

    private LocalDateTime adoptedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableField(exist = false)
    private String petName;

    @TableField(exist = false)
    private String petImage;

    @TableField(exist = false)
    private String username;
}
