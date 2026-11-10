
package com.pet.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pet.common.exception.BusinessException;
import com.pet.entity.AdoptionApplication;
import com.pet.entity.Pet;
import com.pet.entity.User;
import com.pet.mapper.AdoptionApplicationMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AdoptionApplicationService extends ServiceImpl<AdoptionApplicationMapper, AdoptionApplication> {

    @Autowired
    private PetService petService;

    @Autowired
    private UserService userService;

    @Transactional
    public AdoptionApplication submitApplication(AdoptionApplication application) {
        Pet pet = petService.getById(application.getPetId());
        if (pet == null) {
            throw new BusinessException(400, "宠物不存在");
        }
        if (pet.getStatus() != 0) {
            throw new BusinessException(400, "该宠物已被领养或已下架");
        }
        LambdaQueryWrapper<AdoptionApplication> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AdoptionApplication::getUserId, application.getUserId())
               .eq(AdoptionApplication::getPetId, application.getPetId())
               .in(AdoptionApplication::getStatus, 0, 1);
        if (this.count(wrapper) > 0) {
            throw new BusinessException(400, "您已经申请过该宠物");
        }
        application.setStatus(0);
        this.save(application);
        return application;
    }

    public Page<AdoptionApplication> listApplicationsByUser(Long userId, int page, int size) {
        return listApplicationsByUser(userId, page, size, null);
    }

    public Page<AdoptionApplication> listApplicationsByUser(Long userId, int page, int size, Integer status) {
        if (status != null && (status < 0 || status > 2)) {
            throw new BusinessException(400, "申请状态无效");
        }
        LambdaQueryWrapper<AdoptionApplication> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AdoptionApplication::getUserId, userId);
        if (status != null) {
            wrapper.eq(AdoptionApplication::getStatus, status);
        }
        wrapper.orderByDesc(AdoptionApplication::getCreatedAt);
        Page<AdoptionApplication> result = this.page(new Page<>(page, size), wrapper);
        enrichApplications(result.getRecords());
        return result;
    }

    public Page<AdoptionApplication> listApplications(int page, int size, Integer status, String keyword) {
        LambdaQueryWrapper<AdoptionApplication> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(AdoptionApplication::getStatus, status);
        }
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(query -> query.like(AdoptionApplication::getRealName, keyword)
                    .or()
                    .like(AdoptionApplication::getPhone, keyword));
        }
        wrapper.orderByDesc(AdoptionApplication::getCreatedAt);
        Page<AdoptionApplication> result = this.page(new Page<>(page, size), wrapper);
        enrichApplications(result.getRecords());
        return result;
    }

    private void enrichApplications(java.util.List<AdoptionApplication> applications) {
        for (AdoptionApplication app : applications) {
            Pet pet = petService.getById(app.getPetId());
            if (pet != null) {
                app.setPetName(pet.getName());
                app.setPetImage(pet.getImage());
            }
            User user = userService.getById(app.getUserId());
            if (user != null) {
                app.setUsername(user.getUsername());
            }
        }
    }

    @Transactional
    public void reviewApplication(Long id, Integer status, String rejectReason) {
        AdoptionApplication application = this.getById(id);
        if (application == null) {
            throw new BusinessException(400, "申请不存在");
        }
        if (application.getStatus() != 0) {
            throw new BusinessException(400, "该申请已处理");
        }
        if (status == null || (status != 1 && status != 2)) {
            throw new BusinessException(400, "审核状态只能是通过或拒绝");
        }
        if (status == 2 && (rejectReason == null || rejectReason.trim().isEmpty())) {
            throw new BusinessException(400, "拒绝申请必须填写理由");
        }
        application.setStatus(status);
        application.setRejectReason(rejectReason == null ? null : rejectReason.trim());
        if (status == 1) {
            Pet pet = petService.getById(application.getPetId());
            if (pet == null || pet.getStatus() != 0) {
                throw new BusinessException(409, "该宠物已被其他申请领养或已下架");
            }
            application.setAdoptedAt(LocalDateTime.now());
            petService.updatePetStatus(application.getPetId(), 1);
            LambdaQueryWrapper<AdoptionApplication> pending = new LambdaQueryWrapper<>();
            pending.eq(AdoptionApplication::getPetId, application.getPetId())
                    .eq(AdoptionApplication::getStatus, 0)
                    .ne(AdoptionApplication::getId, application.getId());
            List<AdoptionApplication> otherApplications = this.list(pending);
            for (AdoptionApplication other : otherApplications) {
                other.setStatus(2);
                other.setRejectReason("该宠物已被其他申请领养");
                this.updateById(other);
            }
        }
        this.updateById(application);
    }
}
