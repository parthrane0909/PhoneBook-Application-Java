package com.phonebook.controller;

import com.phonebook.dto.*;
import com.phonebook.service.ContactService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/contacts")
@Validated
public class ContactController {

    private final ContactService contactService;

    public ContactController(ContactService contactService) {
        this.contactService = contactService;
    }

    @PostMapping({"", "/"})
    public ContactResponse createContact(
            @Valid @RequestBody ContactCreateRequest request) {
        return contactService.createContact(request);
    }

    @GetMapping({"", "/"})
    public ContactListResponse getContacts(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean favorite,
            @RequestParam(required = false) String tag,
            @RequestParam(defaultValue = "false") boolean unlabeled,
            @RequestParam(defaultValue = "false") boolean recent,
            @RequestParam(defaultValue = "name_asc") String sort,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit
    ) {
        return contactService.getContacts(
                search,
                favorite,
                tag,
                unlabeled,
                recent,
                sort,
                page,
                limit
        );
    }

    @GetMapping("/metrics")
    public ContactMetricsResponse getMetrics() {
        return contactService.getMetrics();
    }

    @GetMapping("/tags")
    public List<TagResponse> getTags() {
        return contactService.getTags();
    }

    @PostMapping("/import")
    public ContactImportResponse importContacts(
            @Valid @RequestBody ContactImportRequest request) {
        return contactService.importContacts(request);
    }

    @GetMapping("/{contactId:\\d+}")
    public ContactResponse getContact(@PathVariable Long contactId) {
        return contactService.getContact(contactId);
    }

    @PutMapping("/{contactId:\\d+}")
    public ContactResponse updateContact(
            @PathVariable Long contactId,
            @Valid @RequestBody ContactUpdateRequest request) {
        return contactService.updateContact(contactId, request);
    }

    @DeleteMapping("/{contactId:\\d+}")
    public java.util.Map<String, String> deleteContact(
            @PathVariable Long contactId) {
        contactService.deleteContact(contactId);
        return java.util.Map.of("message", "Contact deleted successfully");
    }

    @PatchMapping("/{contactId:\\d+}/favorite")
    public ContactResponse updateFavorite(
            @PathVariable Long contactId,
            @Valid @RequestBody FavoriteUpdate request) {
        return contactService.updateFavorite(
                contactId,
                request.isFavorite()
        );
    }

    @PatchMapping("/{contactId:\\d+}/viewed")
    public ContactResponse markViewed(@PathVariable Long contactId) {
        return contactService.markViewed(contactId);
    }
}
