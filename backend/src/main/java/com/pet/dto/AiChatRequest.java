package com.pet.dto;

import lombok.Data;

@Data
public class AiChatRequest {
    private Long petId;
    private String message;
}
