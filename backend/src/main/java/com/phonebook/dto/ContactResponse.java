package com.phonebook.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;

public record ContactResponse(
        Long id,
        String name,
        @JsonProperty("phone_number") String phoneNumber,
        String email,
        String address,
        @JsonProperty("is_favorite") boolean isFavorite,
        @JsonProperty("last_viewed_at") LocalDateTime lastViewedAt,
        @JsonProperty("created_at") LocalDateTime createdAt,
        @JsonProperty("updated_at") LocalDateTime updatedAt,
        List<TagResponse> tags
) {}
