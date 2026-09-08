package com.phonebook.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ContactMetricsResponse(
        long total,
        long favorites,
        @JsonProperty("recently_added") long recentlyAdded,
        long unlabeled
) {}
