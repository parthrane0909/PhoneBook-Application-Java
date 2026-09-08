package com.phonebook.dto;

public record ContactImportError(
        int row,
        String reason
) {}
