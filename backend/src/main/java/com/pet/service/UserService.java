package com.pet.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pet.common.exception.BusinessException;
import com.pet.entity.User;
import com.pet.mapper.UserMapper;
import com.pet.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class UserService extends ServiceImpl<UserMapper, User> {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    public User login(String username, String password) {
        String normalizedUsername = username == null ? "" : username.trim();
        if (!StringUtils.hasText(normalizedUsername) || !StringUtils.hasText(password)) {
            throw new BusinessException(400, "请输入用户名和密码");
        }

        User user = this.getOne(new LambdaQueryWrapper<User>().eq(User::getUsername, normalizedUsername));
        if (user == null) {
            throw new BusinessException(400, "用户名不存在");
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BusinessException(400, "密码错误");
        }
        if (user.getStatus() == 0) {
            throw new BusinessException(400, "用户已被禁用");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        user.setToken(token);
        user.setPassword(null);
        return user;
    }

    public User register(User user) {
        user.setUsername(user.getUsername().trim());
        user.setPassword(user.getPassword().trim());
        user.setNickname(user.getNickname() == null ? user.getUsername() : user.getNickname().trim());
        user.setPhone(user.getPhone() == null ? null : user.getPhone().trim());
        user.setEmail(user.getEmail() == null ? null : user.getEmail().trim());

        if (this.count(new LambdaQueryWrapper<User>().eq(User::getUsername, user.getUsername())) > 0) {
            throw new BusinessException(400, "用户名已存在");
        }
        if (StringUtils.hasText(user.getPhone())
                && this.count(new LambdaQueryWrapper<User>().eq(User::getPhone, user.getPhone())) > 0) {
            throw new BusinessException(400, "手机号已被注册");
        }
        if (StringUtils.hasText(user.getEmail())
                && this.count(new LambdaQueryWrapper<User>().eq(User::getEmail, user.getEmail())) > 0) {
            throw new BusinessException(400, "邮箱已被注册");
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole(0);
        user.setStatus(1);
        this.save(user);

        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        user.setToken(token);
        user.setPassword(null);
        return user;
    }

    public Page<User> listUsers(int page, int size, String keyword) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(query -> query.like(User::getUsername, keyword)
                    .or()
                    .like(User::getNickname, keyword)
                    .or()
                    .like(User::getPhone, keyword));
        }
        wrapper.orderByDesc(User::getCreatedAt);
        return this.page(new Page<>(page, size), wrapper);
    }

    public void updateUserStatus(Long id, Integer status) {
        User user = this.getById(id);
        if (user == null) {
            throw new BusinessException(400, "用户不存在");
        }
        user.setStatus(status);
        this.updateById(user);
    }
}
