package com.pet.controller;

import com.pet.common.Result;
import com.pet.dto.LoginRequest;
import com.pet.dto.RegisterRequest;
import com.pet.entity.User;
import com.pet.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @PostMapping("/login")
    public Result<User> login(@Valid @RequestBody LoginRequest request) {
        User user = userService.login(request.getUsername(), request.getPassword());
        return Result.success("登录成功", user);
    }

    @PostMapping("/register")
    public Result<User> register(@Valid @RequestBody RegisterRequest request) {
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(request.getPassword());
        user.setNickname(request.getNickname());
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        User registered = userService.register(user);
        return Result.success("注册成功", registered);
    }

    @GetMapping("/me")
    public ResponseEntity<Result<User>> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || "anonymousUser".equals(auth.getPrincipal())) {
            return ResponseEntity.status(401).body(Result.unauthorized());
        }
        Long userId = (Long) auth.getPrincipal();
        User user = userService.getById(userId);
        if (user != null) {
            user.setPassword(null);
        }
        return ResponseEntity.ok(Result.success(user));
    }

    @PutMapping("/profile")
    public Result<User> updateProfile(@RequestBody User user) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long userId = (Long) auth.getPrincipal();
        User existing = userService.getById(userId);
        if (user.getNickname() != null) existing.setNickname(user.getNickname());
        if (user.getPhone() != null) existing.setPhone(user.getPhone());
        if (user.getEmail() != null) existing.setEmail(user.getEmail());
        if (user.getAvatar() != null) existing.setAvatar(user.getAvatar());
        userService.updateById(existing);
        existing.setPassword(null);
        return Result.success("更新成功", existing);
    }
}
