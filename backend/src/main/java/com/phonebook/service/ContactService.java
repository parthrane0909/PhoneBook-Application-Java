package com.phonebook.service;

import com.phonebook.dto.*;
import com.phonebook.model.Contact;
import com.phonebook.model.Tag;
import com.phonebook.repository.ContactRepository;
import com.phonebook.repository.TagRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ContactService {

    private final ContactRepository contactRepository;
    private final TagRepository tagRepository;
    private final TransactionTemplate transactionTemplate;

    public ContactService(
            ContactRepository contactRepository,
            TagRepository tagRepository,
            PlatformTransactionManager transactionManager
    ) {
        this.contactRepository = contactRepository;
        this.tagRepository = tagRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Transactional
    public ContactResponse createContact(ContactCreateRequest request) {
        validatePhone(request.getPhoneNumber());

        String phone = request.getPhoneNumber().trim();
        String email = normalizeNullable(request.getEmail());
        assertUnique(phone, email, null);

        Contact contact = new Contact();
        contact.setName(request.getName());
        contact.setPhoneNumber(phone);
        contact.setEmail(email);
        contact.setAddress(normalizeNullable(request.getAddress()));
        contact.setTags(new HashSet<>(getOrCreateTags(request.getTags())));

        return toResponse(contactRepository.saveAndFlush(contact));
    }

    @Transactional(readOnly = true)
    public ContactListResponse getContacts(
            String search,
            Boolean favorite,
            String tag,
            boolean unlabeled,
            boolean recent,
            String sort,
            int page,
            int limit
    ) {
        validateSort(sort);

        Specification<Contact> specification = buildSpecification(
                search, favorite, tag, unlabeled, recent
        );

        Pageable pageable = PageRequest.of(
                page - 1,
                limit,
                buildSort(sort)
        );

        Page<Contact> result = contactRepository.findAll(specification, pageable);

        List<ContactResponse> contacts = result.getContent()
                .stream()
                .map(this::toResponse)
                .toList();

        return new ContactListResponse(
                contacts,
                page,
                limit,
                result.getTotalElements()
        );
    }

    @Transactional(readOnly = true)
    public ContactResponse getContact(Long id) {
        return toResponse(findContact(id));
    }

    @Transactional
    public ContactResponse updateContact(Long id, ContactUpdateRequest request) {
        Contact contact = findContact(id);

        if (request.getName() != null) {
            contact.setName(request.getName());
        }

        if (request.getPhoneNumber() != null) {
            validatePhone(request.getPhoneNumber());
            contact.setPhoneNumber(request.getPhoneNumber().trim());
        }

        if (request.getEmail() != null) {
            contact.setEmail(normalizeNullable(request.getEmail()));
        }

        if (request.getAddress() != null) {
            contact.setAddress(normalizeNullable(request.getAddress()));
        }

        if (request.getTags() != null) {
            contact.setTags(new HashSet<>(getOrCreateTags(request.getTags())));
        }

        assertUnique(contact.getPhoneNumber(), contact.getEmail(), contact.getId());

        return toResponse(contactRepository.saveAndFlush(contact));
    }

    @Transactional
    public void deleteContact(Long id) {
        Contact contact = findContact(id);
        contactRepository.delete(contact);
    }

    @Transactional
    public ContactResponse updateFavorite(Long id, boolean favorite) {
        Contact contact = findContact(id);
        contact.setFavorite(favorite);
        return toResponse(contactRepository.saveAndFlush(contact));
    }

    @Transactional
    public ContactResponse markViewed(Long id) {
        Contact contact = findContact(id);
        contact.setLastViewedAt(LocalDateTime.now(ZoneOffset.UTC));
        return toResponse(contactRepository.saveAndFlush(contact));
    }

    @Transactional(readOnly = true)
    public List<TagResponse> getTags() {
        return tagRepository.findAllByOrderByNameAsc()
                .stream()
                .map(tag -> new TagResponse(tag.getId(), tag.getName()))
                .toList();
    }

    @Transactional(readOnly = true)
    public ContactMetricsResponse getMetrics() {
        long total = contactRepository.count();
        long favorites = contactRepository.count(
                (root, query, cb) -> cb.isTrue(root.get("favorite"))
        );
        long unlabeled = contactRepository.count(
                (root, query, cb) -> cb.isEmpty(root.get("tags"))
        );

        LocalDateTime weekAgo =
                LocalDateTime.now(ZoneOffset.UTC).minusDays(7);

        long recentlyAdded = contactRepository.count(
                (root, query, cb) ->
                        cb.greaterThanOrEqualTo(root.get("createdAt"), weekAgo)
        );

        return new ContactMetricsResponse(
                total,
                favorites,
                recentlyAdded,
                unlabeled
        );
    }

    public ContactImportResponse importContacts(ContactImportRequest request) {
        int imported = 0;
        int skipped = 0;
        List<ContactImportError> errors = new ArrayList<>();

        int rowNumber = 2;

        for (ContactCreateRequest row : request.rows()) {
            try {
                transactionTemplate.executeWithoutResult(status -> importRow(row));
                imported++;
            } catch (DataIntegrityViolationException | IllegalArgumentException ex) {
                skipped++;
                String reason = ex instanceof IllegalArgumentException
                        ? ex.getMessage()
                        : "Duplicate phone number or email";

                errors.add(new ContactImportError(
                        rowNumber,
                        reason == null ? "Invalid contact" : reason
                ));
            }

            rowNumber++;
        }

        return new ContactImportResponse(imported, skipped, errors);
    }

    private void importRow(ContactCreateRequest row) {
        validatePhone(row.getPhoneNumber());

        String phone = row.getPhoneNumber().trim();
        String email = normalizeNullable(row.getEmail());

        assertUnique(phone, email, null);

        Contact contact = new Contact();
        contact.setName(row.getName());
        contact.setPhoneNumber(phone);
        contact.setEmail(email);
        contact.setAddress(normalizeNullable(row.getAddress()));
        contact.setTags(new HashSet<>(getOrCreateTags(row.getTags())));

        contactRepository.saveAndFlush(contact);
    }

    private Specification<Contact> buildSpecification(
            String search,
            Boolean favorite,
            String tag,
            boolean unlabeled,
            boolean recent
    ) {
        return (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates =
                    new ArrayList<>();

            if (search != null && !search.trim().isEmpty()) {
                String pattern = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";

                Subquery<Long> tagged = query.subquery(Long.class);
                Root<Contact> taggedRoot = tagged.from(Contact.class);
                Join<Contact, Tag> searchTags = taggedRoot.join("tags");
                tagged.select(taggedRoot.get("id"));
                tagged.where(
                        cb.equal(taggedRoot.get("id"), root.get("id")),
                        cb.like(cb.lower(searchTags.get("name")), pattern)
                );

                predicates.add(
                        cb.or(
                                cb.like(cb.lower(root.get("name")), pattern),
                                cb.like(cb.lower(root.get("phoneNumber")), pattern),
                                cb.exists(tagged)
                        )
                );
            }

            if (favorite != null) {
                predicates.add(cb.equal(root.get("favorite"), favorite));
            }

            if (tag != null && !tag.trim().isEmpty()) {
                Subquery<Long> tagged = query.subquery(Long.class);
                Root<Contact> taggedRoot = tagged.from(Contact.class);
                Join<Contact, Tag> tagJoin = taggedRoot.join("tags");
                tagged.select(taggedRoot.get("id"));
                tagged.where(
                        cb.equal(taggedRoot.get("id"), root.get("id")),
                        cb.equal(
                                cb.lower(tagJoin.get("name")),
                                tag.trim().toLowerCase(Locale.ROOT)
                        )
                );
                predicates.add(cb.exists(tagged));
            }

            if (unlabeled) {
                predicates.add(cb.isEmpty(root.get("tags")));
            }

            if (recent) {
                predicates.add(cb.isNotNull(root.get("lastViewedAt")));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    private Sort buildSort(String sort) {
        return switch (sort) {
            case "name_desc" ->
                    Sort.by(Sort.Order.desc("name"));
            case "recently_viewed" ->
                    Sort.by(
                            new Sort.Order(
                                    Sort.Direction.DESC,
                                    "lastViewedAt"
                            ).nullsLast(),
                            Sort.Order.asc("name")
                    );
            case "recently_added" ->
                    Sort.by(Sort.Order.desc("createdAt"));
            case "recently_updated" ->
                    Sort.by(Sort.Order.desc("updatedAt"));
            default ->
                    Sort.by(Sort.Order.asc("name"));
        };
    }

    private void validateSort(String sort) {
        Set<String> valid = Set.of(
                "name_asc",
                "name_desc",
                "recently_viewed",
                "recently_added",
                "recently_updated"
        );

        if (!valid.contains(sort)) {
            throw new IllegalArgumentException("Invalid sort value");
        }
    }

    private void validatePhone(String value) {
        if (value == null) {
            throw new IllegalArgumentException(
                    "Enter a valid phone number with 7–15 digits."
            );
        }

        String phone = value.trim();
        long digits = phone.chars()
                .filter(Character::isDigit)
                .count();

        if (phone.isEmpty()
                || digits < 7
                || digits > 15
                || !phone.matches("^\\+?[0-9\\s()\\-]+$")) {
            throw new IllegalArgumentException(
                    "Enter a valid phone number with 7–15 digits."
            );
        }
    }

    private void assertUnique(String phone, String email, Long excludeId) {
        boolean duplicatePhone = excludeId == null
                ? contactRepository.existsByPhoneNumber(phone)
                : contactRepository.existsByPhoneNumberAndIdNot(phone, excludeId);

        boolean duplicateEmail = email != null && (excludeId == null
                ? contactRepository.existsByEmail(email)
                : contactRepository.existsByEmailAndIdNot(email, excludeId));

        if (duplicatePhone || duplicateEmail) {
            throw new IllegalArgumentException("Phone number or email already exists");
        }
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private List<Tag> getOrCreateTags(List<String> names) {
        if (names == null || names.isEmpty()) {
            return List.of();
        }

        Map<String, String> cleaned = new LinkedHashMap<>();

        for (String raw : names) {
            if (raw == null) {
                continue;
            }

            String name = raw.trim();

            if (name.isEmpty()) {
                continue;
            }

            if (name.length() > 80) {
                throw new IllegalArgumentException(
                        "Each tag must be 80 characters or fewer."
                );
            }

            cleaned.putIfAbsent(name.toLowerCase(Locale.ROOT), name);
        }

        if (cleaned.size() > 10) {
            throw new IllegalArgumentException(
                    "A contact can have at most 10 tags."
            );
        }

        List<Tag> tags = new ArrayList<>();

        for (String name : cleaned.values()) {
            Tag tag = tagRepository
                    .findByNameIgnoreCase(name)
                    .orElseGet(() -> {
                        Tag created = new Tag();
                        created.setName(name);
                        return tagRepository.saveAndFlush(created);
                    });

            tags.add(tag);
        }

        return tags;
    }

    private Contact findContact(Long id) {
        return contactRepository.findById(id)
                .orElseThrow(() ->
                        new NoSuchElementException("Contact not found")
                );
    }

    private ContactResponse toResponse(Contact contact) {
        List<TagResponse> tags = contact.getTags()
                .stream()
                .sorted(Comparator.comparing(Tag::getName,
                        String.CASE_INSENSITIVE_ORDER))
                .map(tag -> new TagResponse(tag.getId(), tag.getName()))
                .collect(Collectors.toList());

        return new ContactResponse(
                contact.getId(),
                contact.getName(),
                contact.getPhoneNumber(),
                contact.getEmail(),
                contact.getAddress(),
                contact.isFavorite(),
                contact.getLastViewedAt(),
                contact.getCreatedAt(),
                contact.getUpdatedAt(),
                tags
        );
    }
}
