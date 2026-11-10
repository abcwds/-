
package com.pet.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pet.entity.Announcement;
import com.pet.mapper.AnnouncementMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class AnnouncementService extends ServiceImpl<AnnouncementMapper, Announcement> {

    public List<Announcement> listActive(String type) {
        LambdaQueryWrapper<Announcement> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Announcement::getStatus, 1);
        if (StringUtils.hasText(type)) {
            wrapper.eq(Announcement::getType, type);
        }
        wrapper.orderByDesc(Announcement::getSortOrder)
               .orderByDesc(Announcement::getCreatedAt);
        return this.list(wrapper);
    }

    public Page<Announcement> list(int page, int size, String type, String keyword) {
        LambdaQueryWrapper<Announcement> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(type)) {
            wrapper.eq(Announcement::getType, type);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Announcement::getTitle, keyword);
        }
        wrapper.orderByDesc(Announcement::getSortOrder)
               .orderByDesc(Announcement::getCreatedAt);
        return this.page(new Page<>(page, size), wrapper);
    }
}
