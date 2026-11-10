
package com.pet.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pet.common.Result;
import com.pet.entity.HealthRecord;
import com.pet.service.HealthRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/health")
public class AdminHealthController {

    @Autowired
    private HealthRecordService healthRecordService;

    @GetMapping
    public Result<Page<HealthRecord>> listRecords(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long petId,
            @RequestParam(required = false) String keyword) {
        Page<HealthRecord> records = healthRecordService.listRecords(page, size, petId, keyword);
        return Result.success(records);
    }

    @GetMapping("/{id}")
    public Result<HealthRecord> getRecord(@PathVariable Long id) {
        HealthRecord record = healthRecordService.getById(id);
        return Result.success(record);
    }

    @PostMapping
    public Result<HealthRecord> createRecord(@RequestBody HealthRecord record) {
        healthRecordService.createRecord(record);
        return Result.success("创建成功", record);
    }

    @PutMapping("/{id}")
    public Result<HealthRecord> updateRecord(@PathVariable Long id, @RequestBody HealthRecord record) {
        healthRecordService.updateRecord(id, record);
        return Result.success("更新成功", record);
    }

    @DeleteMapping("/{id}")
    public Result<?> deleteRecord(@PathVariable Long id) {
        healthRecordService.removeById(id);
        return Result.success("删除成功");
    }
}
