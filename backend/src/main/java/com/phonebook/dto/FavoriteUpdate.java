package com.phonebook.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

public record FavoriteUpdate(
        @NotNull
        @JsonProperty("is_favorite")
        Boolean isFavorite
) {}
