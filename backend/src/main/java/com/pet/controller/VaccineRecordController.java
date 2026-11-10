
package com.pet.controller;

import com.pet.common.Result;
import com.pet.entity.VaccineRecord;
import com.pet.service.VaccineRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vaccine")
public class VaccineRecordController {

    @Autowired
    private VaccineRecordService vaccineRecordService;

    @GetMapping("/pet/{petId}")
    public Result<List<VaccineRecord>> getVaccineRecordsByPet(@PathVariable Long petId) {
        List<VaccineRecord> records = vaccineRecordService.getRecordsByPetId(petId);
        return Result.success(records);
    }
}
