
package com.pet.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pet.common.exception.BusinessException;
import com.pet.entity.Pet;
import com.pet.entity.VaccineRecord;
import com.pet.mapper.VaccineRecordMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;

@Service
public class VaccineRecordService extends ServiceImpl<VaccineRecordMapper, VaccineRecord> {

    @Autowired
    private PetService petService;

    public VaccineRecord createRecord(VaccineRecord record) {
        validateDates(record);
        Pet pet = petService.getById(record.getPetId());
        if (pet == null) {
            throw new BusinessException(400, "宠物不存在");
        }
        this.save(record);
        return record;
    }

    public VaccineRecord updateRecord(Long id, VaccineRecord record) {
        record.setId(id);
        validateDates(record);
        if (petService.getById(record.getPetId()) == null) {
            throw new BusinessException(400, "宠物不存在");
        }
        this.updateById(record);
        return record;
    }

    public Page<VaccineRecord> listRecords(int page, int size, Long petId, String keyword) {
        LambdaQueryWrapper<VaccineRecord> wrapper = new LambdaQueryWrapper<>();
        if (petId != null) {
            wrapper.eq(VaccineRecord::getPetId, petId);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(query -> query.like(VaccineRecord::getVaccineName, keyword)
                    .or()
                    .like(VaccineRecord::getHospital, keyword));
        }
        wrapper.orderByDesc(VaccineRecord::getVaccinateDate);
        Page<VaccineRecord> result = this.page(new Page<>(page, size), wrapper);
        enrichRecords(result.getRecords());
        return result;
    }

    public List<VaccineRecord> getRecordsByPetId(Long petId) {
        LambdaQueryWrapper<VaccineRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VaccineRecord::getPetId, petId);
        wrapper.orderByDesc(VaccineRecord::getVaccinateDate);
        List<VaccineRecord> records = this.list(wrapper);
        enrichRecords(records);
        return records;
    }

    private void enrichRecords(List<VaccineRecord> records) {
        for (VaccineRecord record : records) {
            Pet pet = petService.getById(record.getPetId());
            if (pet != null) {
                record.setPetName(pet.getName());
            }
        }
    }

    private void validateDates(VaccineRecord record) {
        if (record.getVaccinateDate() != null && record.getVaccinateDate().isAfter(LocalDate.now())) {
            throw new BusinessException(400, "接种日期不能晚于今天");
        }
        if (record.getNextDate() != null
                && record.getVaccinateDate() != null
                && record.getNextDate().isBefore(record.getVaccinateDate())) {
            throw new BusinessException(400, "下次接种日期不能早于本次接种日期");
        }
    }
}
