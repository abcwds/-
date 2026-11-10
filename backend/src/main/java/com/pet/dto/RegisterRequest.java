package com.pet.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "请输入用户名")
    @Pattern(regexp = "^[A-Za-z0-9_]{4,20}$", message = "用户名需为4-20位字母、数字或下划线")
    private String username;

    @NotBlank(message = "请输入密码")
    @Size(min = 6, max = 30, message = "密码长度需为6-30位")
    private String password;

    @NotBlank(message = "请输入昵称")
    @Size(max = 30, message = "昵称不能超过30个字符")
    private String nickname;

    @NotBlank(message = "请输入手机号")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "请输入正确的手机号")
    private String phone;

    @NotBlank(message = "请输入邮箱")
    @Email(message = "请输入正确的邮箱")
    private String email;
}
