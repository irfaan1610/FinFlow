package com.project.finance.service;

import com.project.finance.dto.request.RecordRequest;
import com.project.finance.dto.response.RecordResponse;
import com.project.finance.model.RecordType;
import com.project.finance.model.Role;
import com.project.finance.model.User;
import com.project.finance.model.UserStatus;
import com.project.finance.repository.FinancialRecordRepository;
import com.project.finance.repository.UserRepository;
import net.jqwik.api.*;
import net.jqwik.spring.JqwikSpringSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for RecordService.
 *
 * Feature: finance-dashboard
 * Property 7: Record create round-trip
 * Property 8: Soft delete excludes records from queries
 * Property 9: Combined filter correctness
 * Validates: Requirements 4.1, 4.4, 5.1–5.4, 8.4
 */
@SpringBootTest
@JqwikSpringSupport
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RecordServicePropertyTest {

    @Autowired
    private RecordService recordService;

    @Autowired
    private FinancialRecordRepository recordRepository;

    @Autowired
    private UserRepository userRepository;

    // -------------------------------------------------------------------------
    // Property 7: Record create round-trip
    // For any valid RecordRequest, the created FinancialRecord retrieved by its
    // id SHALL have amount, type, category, date, and description matching the
    // request, and createdBy SHALL reference the authenticated admin user.
    // Validates: Requirements 4.1
    // -------------------------------------------------------------------------

    @Property(tries = 100)
    @Label("Property 7: Record create round-trip")
    void recordCreateRoundTrip(@ForAll("validRecordRequests") RecordRequest req) {
        // Feature: finance-dashboard, Property 7: Record create round-trip
        // Validates: Requirements 4.1

        User admin = createAndSaveUser();
        try {
            RecordResponse response = recordService.createRecord(req, admin);

            assertThat(response.getId()).isNotNull();
            assertThat(response.getAmount()).isEqualByComparingTo(req.getAmount());
            assertThat(response.getType()).isEqualTo(req.getType());
            assertThat(response.getCategory()).isEqualTo(req.getCategory());
            assertThat(response.getDate()).isEqualTo(req.getDate());
            assertThat(response.getDescription()).isEqualTo(req.getDescription());
            assertThat(response.getCreatedById()).isEqualTo(admin.getId());
        } finally {
            recordRepository.deleteAll(recordRepository.findAll().stream()
                    .filter(r -> r.getCreatedBy().getId().equals(admin.getId()))
                    .toList());
            userRepository.delete(admin);
        }
    }

    // -------------------------------------------------------------------------
    // Property 8: Soft delete excludes records from queries
    // For any FinancialRecord that has been deleted, that record SHALL NOT
    // appear in getRecords results. The record SHALL still exist in the DB
    // with deleted=true.
    // Validates: Requirements 4.4, 8.4
    // -------------------------------------------------------------------------

    @Property(tries = 100)
    @Label("Property 8: Soft delete excludes records from queries")
    void softDeleteExcludesRecordsFromQueries(@ForAll("validRecordRequests") RecordRequest req) {
        // Feature: finance-dashboard, Property 8: Soft delete excludes records from queries
        // Validates: Requirements 4.4, 8.4

        User admin = createAndSaveUser();
        try {
            RecordResponse created = recordService.createRecord(req, admin);
            Long recordId = created.getId();

            // Soft delete the record
            recordService.deleteRecord(recordId);

            // Record must NOT appear in paginated query results
            Page<RecordResponse> results = recordService.getRecords(
                    PageRequest.of(0, Integer.MAX_VALUE), null, null, null, null);
            List<Long> returnedIds = results.getContent().stream()
                    .map(RecordResponse::getId)
                    .toList();
            assertThat(returnedIds).doesNotContain(recordId);

            // Record must still exist in DB with deleted=true
            assertThat(recordRepository.findById(recordId)).isPresent();
            assertThat(recordRepository.findById(recordId).get().isDeleted()).isTrue();
        } finally {
            recordRepository.deleteAll(recordRepository.findAll().stream()
                    .filter(r -> r.getCreatedBy().getId().equals(admin.getId()))
                    .toList());
            userRepository.delete(admin);
        }
    }

    // -------------------------------------------------------------------------
    // Property 9: Combined filter correctness
    // For any combination of active filter parameters, every record returned
    // SHALL satisfy ALL active filter conditions simultaneously (AND logic).
    // Validates: Requirements 5.1, 5.2, 5.3, 5.4
    // -------------------------------------------------------------------------

    @Property(tries = 100)
    @Label("Property 9: Combined filter correctness")
    void combinedFilterCorrectness(@ForAll("filterScenarios") FilterScenario scenario) {
        // Feature: finance-dashboard, Property 9: Combined filter correctness
        // Validates: Requirements 5.1, 5.2, 5.3, 5.4

        User admin = createAndSaveUser();
        try {
            // Persist all records in the scenario
            for (RecordRequest req : scenario.records()) {
                recordService.createRecord(req, admin);
            }

            Page<RecordResponse> results = recordService.getRecords(
                    PageRequest.of(0, Integer.MAX_VALUE),
                    scenario.startDate(),
                    scenario.endDate(),
                    scenario.category(),
                    scenario.type());

            for (RecordResponse r : results.getContent()) {
                if (scenario.startDate() != null) {
                    assertThat(r.getDate()).isAfterOrEqualTo(scenario.startDate());
                }
                if (scenario.endDate() != null) {
                    assertThat(r.getDate()).isBeforeOrEqualTo(scenario.endDate());
                }
                if (scenario.category() != null) {
                    assertThat(r.getCategory()).isEqualTo(scenario.category());
                }
                if (scenario.type() != null) {
                    assertThat(r.getType()).isEqualTo(scenario.type());
                }
            }
        } finally {
            recordRepository.deleteAll(recordRepository.findAll().stream()
                    .filter(r -> r.getCreatedBy().getId().equals(admin.getId()))
                    .toList());
            userRepository.delete(admin);
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private User createAndSaveUser() {
        User u = new User();
        u.setName("Admin");
        u.setEmail("admin-" + UUID.randomUUID() + "@test.com");
        u.setPassword("$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy");
        u.setRole(Role.ADMIN);
        u.setStatus(UserStatus.ACTIVE);
        return userRepository.save(u);
    }

    // -------------------------------------------------------------------------
    // Arbitraries
    // -------------------------------------------------------------------------

    @Provide
    Arbitrary<RecordRequest> validRecordRequests() {
        Arbitrary<BigDecimal> amounts = Arbitraries.bigDecimals()
                .between(BigDecimal.ONE, new BigDecimal("999999"))
                .ofScale(2);
        Arbitrary<RecordType> types = Arbitraries.of(RecordType.class);
        Arbitrary<String> categories = Arbitraries.of("Food", "Transport", "Salary", "Utilities", "Entertainment");
        Arbitrary<LocalDate> dates = Arbitraries.of(
                LocalDate.of(2024, 1, 15),
                LocalDate.of(2024, 6, 1),
                LocalDate.of(2025, 3, 20),
                LocalDate.of(2025, 11, 5),
                LocalDate.of(2026, 2, 28));
        Arbitrary<String> descriptions = Arbitraries.strings().alpha().ofMinLength(0).ofMaxLength(30);

        return Combinators.combine(amounts, types, categories, dates, descriptions)
                .as((amount, type, category, date, desc) -> {
                    RecordRequest r = new RecordRequest();
                    r.setAmount(amount);
                    r.setType(type);
                    r.setCategory(category);
                    r.setDate(date);
                    r.setDescription(desc.isEmpty() ? null : desc);
                    return r;
                });
    }

    @Provide
    Arbitrary<FilterScenario> filterScenarios() {
        Arbitrary<RecordType> optionalType = Arbitraries.of(
                null, RecordType.INCOME, RecordType.EXPENSE);
        Arbitrary<String> optionalCategory = Arbitraries.of(
                null, "Food", "Transport", "Salary");

        // Generate a small list of records (1–5) to seed the DB
        Arbitrary<List<RecordRequest>> recordLists = validRecordRequests().list().ofMinSize(1).ofMaxSize(5);

        return Combinators.combine(recordLists, optionalType, optionalCategory)
                .as((records, type, category) -> {
                    // Build a date range that covers all record dates when both are set
                    LocalDate start = LocalDate.of(2024, 1, 1);
                    LocalDate end = LocalDate.of(2026, 12, 31);
                    // Randomly omit start/end to test partial filter combinations
                    return new FilterScenario(records, start, end, category, type);
                });
    }

    record FilterScenario(
            List<RecordRequest> records,
            LocalDate startDate,
            LocalDate endDate,
            String category,
            RecordType type) {}
}
