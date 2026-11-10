package com.pet.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pet.entity.Pet;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class PetKnowledgeService {

    private final ObjectMapper objectMapper;
    private PetKnowledge knowledge = new PetKnowledge();

    public PetKnowledgeService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void loadKnowledge() {
        try {
            ClassPathResource resource = new ClassPathResource("pet-knowledge.json");
            try (InputStream inputStream = resource.getInputStream()) {
                knowledge = objectMapper.readValue(inputStream, PetKnowledge.class);
            }
        } catch (Exception ex) {
            knowledge = new PetKnowledge();
        }
    }

    public List<KnowledgeItem> getItems() {
        return knowledge.getItems() == null ? List.of() : knowledge.getItems();
    }

    public List<SourceItem> getSources() {
        List<SourceItem> sources = new ArrayList<>(knowledge.getSources() == null ? List.of() : knowledge.getSources());
        sources.add(source(
                "MSD/Merck Veterinary Manual",
                "https://www.msdvetmanual.com/multimedia/table/normal-rectal-temperature-ranges",
                "详情页体温图正常范围参考"));
        sources.add(source(
                "AAHA 2022 Canine Vaccination Guidelines",
                "https://www.aaha.org/resources/2022-aaha-canine-vaccination-guidelines/",
                "犬只疫苗接种和复种提醒参考"));
        sources.add(source(
                "2020 AAHA/AAFP Feline Vaccination Guidelines",
                "https://www.aaha.org/resources/2020-aahaaafp-feline-vaccination-guidelines/",
                "猫只疫苗接种和复种提醒参考"));
        return sources;
    }

    private SourceItem source(String name, String url, String usage) {
        SourceItem source = new SourceItem();
        source.setName(name);
        source.setUrl(url);
        source.setUsage(usage);
        return source;
    }

    public List<KnowledgeItem> findRelevant(Pet pet, String question, int limit) {
        String text = ((question == null ? "" : question) + " "
                + (pet == null ? "" : pet.getType() + " " + pet.getBreed())).toLowerCase(Locale.ROOT);
        List<ScoredItem> scored = new ArrayList<>();

        for (KnowledgeItem item : getItems()) {
            int score = 0;
            if (StringUtils.hasText(item.getType()) && text.contains(item.getType().toLowerCase(Locale.ROOT))) {
                score += 3;
            }
            if (pet != null && StringUtils.hasText(pet.getBreed()) && StringUtils.hasText(item.getBreed())
                    && sameBreed(pet.getBreed(), item.getBreed())) {
                score += 8;
            }
            if (StringUtils.hasText(item.getBreed()) && text.contains(item.getBreed().toLowerCase(Locale.ROOT))) {
                score += 5;
            }
            if (StringUtils.hasText(item.getTemperament())) {
                for (String word : item.getTemperament().split(",")) {
                    if (StringUtils.hasText(word) && text.contains(word.trim().toLowerCase(Locale.ROOT))) {
                        score += 1;
                    }
                }
            }
            if (score > 0) {
                scored.add(new ScoredItem(item, score));
            }
        }

        if (scored.isEmpty()) {
            return getItems().stream().limit(limit).collect(Collectors.toList());
        }

        return scored.stream()
                .sorted(Comparator.comparingInt(ScoredItem::score).reversed())
                .map(ScoredItem::item)
                .limit(limit)
                .collect(Collectors.toList());
    }

    private boolean sameBreed(String left, String right) {
        String normalizedLeft = normalizeBreed(left);
        String normalizedRight = normalizeBreed(right);
        return normalizedLeft.contains(normalizedRight) || normalizedRight.contains(normalizedLeft);
    }

    private String normalizeBreed(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private record ScoredItem(KnowledgeItem item, int score) {
    }

    @Data
    public static class PetKnowledge {
        private String generatedAt;
        private List<SourceItem> sources = new ArrayList<>();
        private List<KnowledgeItem> items = new ArrayList<>();
    }

    @Data
    public static class SourceItem {
        private String name;
        private String url;
        private String usage;
    }

    @Data
    public static class KnowledgeItem {
        private String type;
        private String breed;
        private String nameZh;
        private String image;
        private String imageSource;
        private String factSource;
        private String weightRange;
        private String lifeSpan;
        private String temperament;
        private String description;
        private String careAdvice;
        private String sourceUrl;
        private String imageSourceUrl;
    }
}
