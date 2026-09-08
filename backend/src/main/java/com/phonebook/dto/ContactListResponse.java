package com.phonebook.dto;

import java.util.List;

public record ContactListResponse(
        List<ContactResponse> contacts,
        int page,
        int limit,
        long total
) {}
