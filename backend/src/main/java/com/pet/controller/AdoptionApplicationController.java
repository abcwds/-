
package com.pet.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pet.common.Result;
import com.pet.entity.AdoptionApplication;
import com.pet.service.AdoptionApplicationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/adoptions")
public class AdoptionApplicationController {

    @Autowired
    private AdoptionApplicationService adoptionApplicationService;

    @PostMapping
    public Result<AdoptionApplication> submitApplication(@RequestBody AdoptionApplication application) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long userId = (Long) auth.getPrincipal();
        application.setUserId(userId);
        AdoptionApplication result = adoptionApplicationService.submitApplication(application);
        return Result.success("申请提交成功", result);
    }

    @GetMapping("/my")
    public Result<Page<AdoptionApplication>> listMyApplications(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long userId = (Long) auth.getPrincipal();
        Page<AdoptionApplication> applications = adoptionApplicationService.listApplicationsByUser(userId, page, size, status);
        return Result.success(applications);
    }
}
