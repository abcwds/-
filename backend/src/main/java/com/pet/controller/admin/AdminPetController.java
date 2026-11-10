
package com.pet.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pet.common.Result;
import com.pet.entity.Pet;
import com.pet.service.PetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/pets")
public class AdminPetController {

    @Autowired
    private PetService petService;

    @GetMapping
    public Result<Page<Pet>> listPets(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        Page<Pet> pets = petService.adminListPets(page, size, type, keyword, status);
        return Result.success(pets);
    }

    @GetMapping("/{id}")
    public Result<Pet> getPet(@PathVariable Long id) {
        Pet pet = petService.getById(id);
        return Result.success(pet);
    }

    @PostMapping
    public Result<Pet> createPet(@RequestBody Pet pet) {
        petService.save(pet);
        return Result.success("创建成功", pet);
    }

    @PutMapping("/{id}")
    public Result<Pet> updatePet(@PathVariable Long id, @RequestBody Pet pet) {
        pet.setId(id);
        petService.updateById(pet);
        return Result.success("更新成功", pet);
    }

    @DeleteMapping("/{id}")
    public Result<?> deletePet(@PathVariable Long id) {
        petService.removeById(id);
        return Result.success("删除成功");
    }
}
