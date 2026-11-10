
package com.pet.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pet.common.Result;
import com.pet.entity.VaccineRecord;
import com.pet.service.VaccineRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/vaccine")
public class AdminVaccineController {

    @Autowired
    private VaccineRecordService vaccineRecordService;

    @GetMapping
    public Result<Page<VaccineRecord>> listRecords(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long petId,
            @RequestParam(required = false) String keyword) {
        Page<VaccineRecord> records = vaccineRecordService.listRecords(page, size, petId, keyword);
        return Result.success(records);
    }

    @GetMapping("/{id}")
    public Result<VaccineRecord> getRecord(@PathVariable Long id) {
        VaccineRecord record = vaccineRecordService.getById(id);
        return Result.success(record);
    }

    @PostMapping
    public Result<VaccineRecord> createRecord(@RequestBody VaccineRecord record) {
        vaccineRecordService.createRecord(record);
        return Result.success("创建成功", record);
    }

    @PutMapping("/{id}")
    public Result<VaccineRecord> updateRecord(@PathVariable Long id, @RequestBody VaccineRecord record) {
        vaccineRecordService.updateRecord(id, record);
        return Result.success("更新成功", record);
    }

    @DeleteMapping("/{id}")
    public Result<?> deleteRecord(@PathVariable Long id) {
        vaccineRecordService.removeById(id);
        return Result.success("删除成功");
    }
}
