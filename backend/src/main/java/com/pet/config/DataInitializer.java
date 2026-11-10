package com.pet.config;

import com.pet.entity.User;
import com.pet.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        User admin = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, "admin"));
        if (admin != null) {
            admin.setPassword(passwordEncoder.encode("123456"));
            userMapper.updateById(admin);
            System.out.println("Admin password updated successfully");
        }

        User user1 = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, "user1"));
        if (user1 != null) {
            user1.setPassword(passwordEncoder.encode("123456"));
            userMapper.updateById(user1);
            System.out.println("User1 password updated successfully");
        }
    }
}
