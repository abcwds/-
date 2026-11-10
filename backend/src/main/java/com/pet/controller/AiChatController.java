package com.pet.controller;

import com.pet.common.Result;
import com.pet.dto.AiChatRequest;
import com.pet.dto.AiChatResponse;
import com.pet.service.AiChatService;
import com.pet.service.PetKnowledgeService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ai")
public class AiChatController {

    private final AiChatService aiChatService;
    private final PetKnowledgeService knowledgeService;

    public AiChatController(AiChatService aiChatService, PetKnowledgeService knowledgeService) {
        this.aiChatService = aiChatService;
        this.knowledgeService = knowledgeService;
    }

    @PostMapping("/chat")
    public Result<AiChatResponse> chat(@RequestBody AiChatRequest request) {
        return Result.success(aiChatService.chat(request));
    }

    @GetMapping("/sources")
    public Result<List<PetKnowledgeService.SourceItem>> sources() {
        return Result.success(knowledgeService.getSources());
    }
}
