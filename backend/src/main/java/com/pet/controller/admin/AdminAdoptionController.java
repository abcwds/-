
package com.pet.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pet.common.Result;
import com.pet.entity.AdoptionApplication;
import com.pet.service.AdoptionApplicationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/adoptions")
public class AdminAdoptionController {

    @Autowired
    private AdoptionApplicationService adoptionApplicationService;

    @GetMapping
    public Result<Page<AdoptionApplication>> listApplications(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword) {
        Page<AdoptionApplication> applications = adoptionApplicationService.listApplications(page, size, status, keyword);
        return Result.success(applications);
    }

    @GetMapping("/{id}")
    public Result<AdoptionApplication> getApplication(@PathVariable Long id) {
        AdoptionApplication application = adoptionApplicationService.getById(id);
        return Result.success(application);
    }

    @PutMapping("/{id}/review")
    public Result<?> reviewApplication(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        Object rawStatus = params.get("status");
        Integer status = rawStatus instanceof Number ? ((Number) rawStatus).intValue() : null;
        String rejectReason = (String) params.get("rejectReason");
        adoptionApplicationService.reviewApplication(id, status, rejectReason);
        return Result.success(status == 1 ? "审核通过" : "审核拒绝");
    }
}
