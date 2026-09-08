package com.phonebook.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ContactImportRequest(
        @NotNull
        @Size(max = 5000)
        List<@Valid ContactCreateRequest> rows
) {}
