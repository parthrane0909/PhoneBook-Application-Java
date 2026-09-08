package com.phonebook.dto;

import java.util.List;

public record ContactImportResponse(
        int imported,
        int skipped,
        List<ContactImportError> errors
) {}
