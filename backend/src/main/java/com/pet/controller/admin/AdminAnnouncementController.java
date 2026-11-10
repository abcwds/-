
package com.pet.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pet.common.Result;
import com.pet.entity.Announcement;
import com.pet.service.AnnouncementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/announcements")
public class AdminAnnouncementController {

    @Autowired
    private AnnouncementService announcementService;

    @GetMapping
    public Result<Page<Announcement>> listAnnouncements(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String keyword) {
        Page<Announcement> announcements = announcementService.list(page, size, type, keyword);
        return Result.success(announcements);
    }

    @GetMapping("/{id}")
    public Result<Announcement> getAnnouncement(@PathVariable Long id) {
        Announcement announcement = announcementService.getById(id);
        return Result.success(announcement);
    }

    @PostMapping
    public Result<Announcement> createAnnouncement(@RequestBody Announcement announcement) {
        announcementService.save(announcement);
        return Result.success("创建成功", announcement);
    }

    @PutMapping("/{id}")
    public Result<Announcement> updateAnnouncement(@PathVariable Long id, @RequestBody Announcement announcement) {
        announcement.setId(id);
        announcementService.updateById(announcement);
        return Result.success("更新成功", announcement);
    }

    @PutMapping("/{id}/status")
    public Result<?> updateStatus(@PathVariable Long id, @RequestBody java.util.Map<String, Integer> params) {
        Announcement announcement = announcementService.getById(id);
        if (announcement == null) {
            return Result.badRequest("公告不存在");
        }
        announcement.setStatus(params.get("status"));
        announcementService.updateById(announcement);
        return Result.success("状态更新成功");
    }

    @DeleteMapping("/{id}")
    public Result<?> deleteAnnouncement(@PathVariable Long id) {
        announcementService.removeById(id);
        return Result.success("删除成功");
    }
}
