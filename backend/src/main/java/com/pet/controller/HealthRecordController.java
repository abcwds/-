
package com.pet.controller;

import com.pet.common.Result;
import com.pet.entity.HealthRecord;
import com.pet.service.HealthRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/health")
public class HealthRecordController {

    @Autowired
    private HealthRecordService healthRecordService;

    @GetMapping("/pet/{petId}")
    public Result<List<HealthRecord>> getHealthRecordsByPet(@PathVariable Long petId) {
        List<HealthRecord> records = healthRecordService.getRecordsByPetId(petId);
        return Result.success(records);
    }
}
