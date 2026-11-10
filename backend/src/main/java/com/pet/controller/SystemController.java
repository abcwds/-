package com.pet.controller;

import com.pet.common.Result;
import com.pet.service.PetKnowledgeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class SystemController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PetKnowledgeService petKnowledgeService;

    @GetMapping({"/", "/api/system/health"})
    public Result<Map<String, Object>> health() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("service", "pet-adoption");
        data.put("timestamp", LocalDateTime.now());

        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            data.put("status", "UP");
            data.put("database", "UP");
            return Result.success("服务运行正常", data);
        } catch (Exception ex) {
            data.put("status", "DEGRADED");
            data.put("database", "DOWN");
            data.put("detail", "数据库连接不可用");
            return Result.error(503, "服务可访问，但数据库不可用", data);
        }
    }

    @GetMapping("/api/system/data-sources")
    public Result<?> dataSources() {
        return Result.success(petKnowledgeService.getSources());
    }
}
