
package com.pet.controller;

import com.pet.common.Result;
import com.pet.entity.Announcement;
import com.pet.service.AnnouncementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/announcements")
public class AnnouncementController {

    @Autowired
    private AnnouncementService announcementService;

    @GetMapping
    public Result<List<Announcement>> listAnnouncements(@RequestParam(required = false) String type) {
        List<Announcement> announcements = announcementService.listActive(type);
        return Result.success(announcements);
    }

    @GetMapping("/{id}")
    public Result<Announcement> getAnnouncement(@PathVariable Long id) {
        Announcement announcement = announcementService.getById(id);
        return Result.success(announcement);
    }
}
