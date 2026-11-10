
package com.pet.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pet.common.exception.BusinessException;
import com.pet.entity.Pet;
import com.pet.mapper.PetMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class PetService extends ServiceImpl<PetMapper, Pet> {

    public Page<Pet> listPets(int page, int size, String type, String keyword) {
        LambdaQueryWrapper<Pet> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Pet::getStatus, 0);
        if (StringUtils.hasText(type)) {
            wrapper.eq(Pet::getType, type);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(query -> query.like(Pet::getName, keyword)
                    .or()
                    .like(Pet::getBreed, keyword));
        }
        wrapper.orderByDesc(Pet::getCreatedAt);
        return this.page(new Page<>(page, size), wrapper);
    }

    public List<Pet> listPetTypes() {
        return this.list(new LambdaQueryWrapper<Pet>().select(Pet::getType).groupBy(Pet::getType));
    }

    public Pet getPetById(Long id) {
        Pet pet = this.getById(id);
        if (pet == null) {
            throw new BusinessException(400, "宠物不存在");
        }
        return pet;
    }

    public void updatePetStatus(Long id, Integer status) {
        Pet pet = this.getById(id);
        if (pet == null) {
            throw new BusinessException(400, "宠物不存在");
        }
        pet.setStatus(status);
        this.updateById(pet);
    }

    public Page<Pet> adminListPets(int page, int size, String type, String keyword, Integer status) {
        LambdaQueryWrapper<Pet> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(type)) {
            wrapper.eq(Pet::getType, type);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(query -> query.like(Pet::getName, keyword)
                    .or()
                    .like(Pet::getBreed, keyword));
        }
        if (status != null) {
            wrapper.eq(Pet::getStatus, status);
        }
        wrapper.orderByDesc(Pet::getCreatedAt);
        return this.page(new Page<>(page, size), wrapper);
    }
}
