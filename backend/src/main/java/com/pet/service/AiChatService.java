package com.pet.service;

import com.pet.dto.AiChatRequest;
import com.pet.dto.AiChatResponse;
import com.pet.entity.Pet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class AiChatService {

    private final PetService petService;
    private final PetKnowledgeService knowledgeService;
    private final RestClient restClient;

    @Value("${deepseek.api-key:}")
    private String apiKey;

    @Value("${deepseek.model:deepseek-chat}")
    private String model;

    public AiChatService(
            PetService petService,
            PetKnowledgeService knowledgeService,
            RestClient.Builder restClientBuilder,
            @Value("${deepseek.base-url:https://api.deepseek.com}") String baseUrl) {
        this.petService = petService;
        this.knowledgeService = knowledgeService;
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
    }

    public AiChatResponse chat(AiChatRequest request) {
        if (request == null || !StringUtils.hasText(request.getMessage())) {
            return new AiChatResponse("请先输入想咨询的问题。", List.of(), true);
        }

        Pet pet = request.getPetId() == null ? null : petService.getPetById(request.getPetId());
        List<PetKnowledgeService.KnowledgeItem> items = knowledgeService.findRelevant(pet, request.getMessage(), 4);
        List<String> sources = buildSources(items);
        String prompt = buildPrompt(pet, request.getMessage(), items);

        if (!StringUtils.hasText(apiKey)) {
            return new AiChatResponse(localAnswer(pet, items), sources, true);
        }

        try {
            String answer = callDeepSeek(prompt);
            return new AiChatResponse(answer, sources, false);
        } catch (Exception ex) {
            return new AiChatResponse(localAnswer(pet, items), sources, true);
        }
    }

    private String callDeepSeek(String prompt) {
        Map<String, Object> body = Map.of(
                "model", model,
                "temperature", 0.3,
                "messages", List.of(
                        Map.of("role", "system", "content", "你是宠物领养及健康管理系统中的AI顾问。回答要基于给定知识库和当前宠物档案，避免编造医疗诊断；涉及疾病时提醒用户去正规宠物医院。"),
                        Map.of("role", "user", "content", prompt)
                )
        );

        Map<?, ?> response = restClient.post()
                .uri("/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .body(body)
                .retrieve()
                .body(Map.class);

        if (response == null || !(response.get("choices") instanceof List<?> choices) || choices.isEmpty()) {
            return "AI服务暂时没有返回内容，请稍后再试。";
        }
        Object firstChoice = choices.get(0);
        if (!(firstChoice instanceof Map<?, ?> choice) || !(choice.get("message") instanceof Map<?, ?> message)) {
            return "AI服务返回格式异常，请稍后再试。";
        }
        Object content = message.get("content");
        return content == null ? "AI服务暂时没有返回内容，请稍后再试。" : content.toString();
    }

    private String buildPrompt(Pet pet, String question, List<PetKnowledgeService.KnowledgeItem> items) {
        StringBuilder builder = new StringBuilder();
        builder.append("用户问题：").append(question).append("\n\n");
        if (pet != null) {
            builder.append("当前宠物档案：")
                    .append("名称=").append(pet.getName())
                    .append("，类型=").append(pet.getType())
                    .append("，品种=").append(pet.getBreed())
                    .append("，年龄=").append(pet.getAge()).append("个月")
                    .append("，体重=").append(pet.getWeight()).append("kg")
                    .append("，健康状态=").append(pet.getHealthStatus())
                    .append("，描述=").append(pet.getDescription())
                    .append("\n\n");
        }
        builder.append("已爬取知识库片段：\n");
        for (PetKnowledgeService.KnowledgeItem item : items) {
            builder.append("- ")
                    .append(item.getType()).append(" / ").append(item.getBreed())
                    .append("：体重范围").append(item.getWeightRange())
                    .append("，寿命").append(item.getLifeSpan())
                    .append("，性格").append(item.getTemperament())
                    .append("。资料摘要：").append(item.getDescription())
                    .append(" 养护建议：").append(item.getCareAdvice())
                    .append("\n");
        }
        builder.append("\n请用中文回答，结构简洁，给出可执行建议，并说明依据来自系统知识库。");
        return builder.toString();
    }

    private String localAnswer(Pet pet, List<PetKnowledgeService.KnowledgeItem> items) {
        StringBuilder builder = new StringBuilder();
        if (pet != null) {
            builder.append("根据当前宠物档案，").append(pet.getName())
                    .append("是").append(pet.getBreed()).append("，")
                    .append("当前体重约").append(pet.getWeight()).append("kg。");
        }
        if (!items.isEmpty()) {
            PetKnowledgeService.KnowledgeItem item = items.get(0);
            builder.append("知识库中相近品种资料显示：")
                    .append(item.getBreed()).append("常见体重范围为").append(item.getWeightRange())
                    .append("，寿命约").append(item.getLifeSpan())
                    .append("，性格特征为").append(item.getTemperament()).append("。")
                    .append(item.getCareAdvice());
        }
        builder.append("如出现持续呕吐、精神沉郁、发热、食欲明显下降等情况，应及时到正规宠物医院检查。");
        return builder.toString();
    }

    private List<String> buildSources(List<PetKnowledgeService.KnowledgeItem> items) {
        Set<String> sources = new LinkedHashSet<>();
        for (PetKnowledgeService.KnowledgeItem item : items) {
            if (StringUtils.hasText(item.getFactSource()) && StringUtils.hasText(item.getSourceUrl())) {
                sources.add(item.getFactSource() + "：" + item.getSourceUrl());
            }
            if (StringUtils.hasText(item.getImageSource()) && StringUtils.hasText(item.getImageSourceUrl())) {
                sources.add(item.getImageSource() + "：" + item.getImageSourceUrl());
            }
        }
        return new ArrayList<>(sources);
    }
}
