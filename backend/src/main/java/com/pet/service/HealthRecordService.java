
package com.pet.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pet.common.exception.BusinessException;
import com.pet.entity.HealthRecord;
import com.pet.entity.Pet;
import com.pet.mapper.HealthRecordMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;

@Service
public class HealthRecordService extends ServiceImpl<HealthRecordMapper, HealthRecord> {

    @Autowired
    private PetService petService;

    public HealthRecord createRecord(HealthRecord record) {
        validateRecordDate(record);
        Pet pet = petService.getById(record.getPetId());
        if (pet == null) {
            throw new BusinessException(400, "宠物不存在");
        }
        this.save(record);
        return record;
    }

    public HealthRecord updateRecord(Long id, HealthRecord record) {
        record.setId(id);
        validateRecordDate(record);
        if (petService.getById(record.getPetId()) == null) {
            throw new BusinessException(400, "宠物不存在");
        }
        this.updateById(record);
        return record;
    }

    public Page<HealthRecord> listRecords(int page, int size, Long petId, String keyword) {
        LambdaQueryWrapper<HealthRecord> wrapper = new LambdaQueryWrapper<>();
        if (petId != null) {
            wrapper.eq(HealthRecord::getPetId, petId);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(query -> query.like(HealthRecord::getDiagnosis, keyword)
                    .or()
                    .like(HealthRecord::getSymptom, keyword));
        }
        wrapper.orderByDesc(HealthRecord::getRecordDate);
        Page<HealthRecord> result = this.page(new Page<>(page, size), wrapper);
        enrichRecords(result.getRecords());
        return result;
    }

    public List<HealthRecord> getRecordsByPetId(Long petId) {
        LambdaQueryWrapper<HealthRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HealthRecord::getPetId, petId);
        wrapper.orderByDesc(HealthRecord::getRecordDate);
        List<HealthRecord> records = this.list(wrapper);
        enrichRecords(records);
        return records;
    }

    private void enrichRecords(List<HealthRecord> records) {
        for (HealthRecord record : records) {
            Pet pet = petService.getById(record.getPetId());
            if (pet != null) {
                record.setPetName(pet.getName());
            }
        }
    }

    private void validateRecordDate(HealthRecord record) {
        if (record.getRecordDate() != null && record.getRecordDate().isAfter(LocalDate.now())) {
            throw new BusinessException(400, "健康记录日期不能晚于今天");
        }
    }
}
