
package com.pet.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pet.common.Result;
import com.pet.entity.Pet;
import com.pet.service.PetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pets")
public class PetController {

    @Autowired
    private PetService petService;

    @GetMapping
    public Result<Page<Pet>> listPets(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String keyword) {
        Page<Pet> pets = petService.listPets(page, size, type, keyword);
        return Result.success(pets);
    }

    @GetMapping("/types")
    public Result<List<Map<String, String>>> listPetTypes() {
        List<Pet> pets = petService.listPetTypes();
        List<Map<String, String>> types = pets.stream()
                .map(p -> Map.of("type", p.getType()))
                .distinct()
                .toList();
        return Result.success(types);
    }

    @GetMapping("/{id}")
    public Result<Pet> getPet(@PathVariable Long id) {
        Pet pet = petService.getPetById(id);
        return Result.success(pet);
    }
}
